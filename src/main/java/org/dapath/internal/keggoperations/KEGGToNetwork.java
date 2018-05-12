package org.dapath.internal.keggoperations;

/*
 * Ozan Ozisik
 */
import org.dapath.internal.pathway.EntryType;
import org.dapath.internal.pathway.Pathway;
import org.dapath.internal.pathway.Entry;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class KEGGToNetwork {

    public void keggToNetwork(String keggFolderPath, String outputFilePath) {
        try {

            File dir = new File(keggFolderPath);
            File[] directoryListing = dir.listFiles();

            if ((directoryListing == null) || directoryListing.length == 0) {
                KEGGDownloader.downloadAll(keggFolderPath);
                directoryListing = dir.listFiles();
//                throw new Exception("Kegg folder does not exist or it is empty.");

            }

            FileWriter fw = new FileWriter(outputFilePath);
            BufferedWriter bw = new BufferedWriter(fw);

            HashMap<String, String> geneSymbolToIdMap = new HashMap<String, String>();
            HashMap<String, String> geneIdToSymbolMap = new HashMap<String, String>();
            HashSet<String> genesInPathwaysSet = new HashSet<String>();
            GeneListParser.parseGeneList(keggFolderPath + File.separator + "hsaGenes.txt", geneIdToSymbolMap, geneSymbolToIdMap);
            GeneListParser.getGenesInPathwaysSet(keggFolderPath + File.separatorChar + "hsaGenesInPathways.txt", genesInPathwaysSet);

            HashSet<String> writtenStrings = new HashSet<String>();

            for (File file : directoryListing) {

                if (file.getName().endsWith("xml")) {

                    KGMLParser kr = new KGMLParser();
                    Pathway pathway = kr.read(new FileInputStream(file), geneIdToSymbolMap);

                    System.out.println(pathway.getName());
//                    pathway.removeDuplicateEntries();

                    ArrayList<Entry> entryList = pathway.getEntryList();
                    for (Entry currentEntry : entryList) {
                        
                        ArrayList<Entry> currentDestinationEntries = new ArrayList<>(currentEntry.getRelatedEntriesOutgoing());
                        for(Entry destinationEntry:currentDestinationEntries){
                            addPair(currentEntry, destinationEntry, bw, writtenStrings);
                        }
                        
                        if(currentEntry.getType()==EntryType.GROUP){
                            ArrayList<Entry> components=currentEntry.getComponents();
                            for(int i=0;i<components.size()-1;i++){
                                for(int j=i+1;j<components.size();j++){
                                    addPair(components.get(i), components.get(j), bw, writtenStrings);
                                }
                            }
                        }
                        
                    }

                }
            }
            bw.close();

        } catch (FileNotFoundException e) {
            System.out.println("File cannot be found.");
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    
    public void addPair(Entry sourceEntry, Entry destinationEntry, BufferedWriter bw, HashSet<String> writtenStrings) throws IOException{
        if(sourceEntry.getType()==EntryType.GROUP || sourceEntry.getType()==EntryType.MULTIGENE){
            for(Entry subSourceEntry:sourceEntry.getComponents()){
                addPair(subSourceEntry, destinationEntry, bw, writtenStrings);
            }
        }
        if(destinationEntry.getType()==EntryType.GROUP || destinationEntry.getType()==EntryType.MULTIGENE){
            for(Entry subDestinationEntry:destinationEntry.getComponents()){
                addPair(sourceEntry, subDestinationEntry, bw, writtenStrings);
            }
        }
        if(sourceEntry.getType()==EntryType.GENE && destinationEntry.getType()==EntryType.GENE){
            if (!sourceEntry.getSymbol().equals(destinationEntry.getSymbol())) {
                String str = sourceEntry.getSymbol() + "\tpp\t" + destinationEntry.getSymbol();
                if (!writtenStrings.contains(str)) {
                    bw.write(str);bw.newLine();
                    writtenStrings.add(str);
                    String str2 = destinationEntry.getSymbol() + "\tpp\t" + sourceEntry.getSymbol();
                    writtenStrings.add(str2);
                    
                }
            }
        }
        
    }
}