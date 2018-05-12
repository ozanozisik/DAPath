package org.dapath.internal.keggoperations;

/*
 * Ozan Ozisik
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class GeneListParser {

    /**
     * Fills geneIdToSymbolMap and geneSymbolToIdMap HashMap objects using
     * hsaGenes.txt file
     *
     * @param directoryPath
     * @param geneIdToSymbolMap
     * @param geneSymbolToIdMap
     */
    public static void parseGeneList(String filePath, HashMap<String, String> geneIdToSymbolMap, HashMap<String, String> geneSymbolToIdMap) throws IOException {

        BufferedReader bufReader = new BufferedReader(new FileReader(filePath));
        bufReader.mark(1000);
        String line = bufReader.readLine();

        if (line.contains("(RefSeq)")) {
            while ((line = bufReader.readLine()) != null) {

                int indexOfRefSeq = line.indexOf("(RefSeq)");
                int indexOfSemicolon = line.indexOf(';');
                int indexOfFirstTab = line.indexOf('\t');

                String geneId = line.substring(0, indexOfFirstTab);

                if (indexOfRefSeq >= 0 && indexOfSemicolon > indexOfRefSeq) {
                    String namesStr = line.substring(indexOfRefSeq + 9, indexOfSemicolon);
                    String[] namesStrArr = namesStr.split("[ ,]+");
                    String name = namesStrArr[0].trim();
                    if (name != null && !name.equals("")) {
                        geneIdToSymbolMap.put(geneId, name);
                    }
                }
            }
        } else {
            while ((line = bufReader.readLine()) != null) {
                int indexOfFirstTab = line.indexOf('\t');
                int indexOfSemicolon = line.indexOf(';');
                String geneId = line.substring(0, indexOfFirstTab);
                if (indexOfSemicolon > 0) {
                    String namesStr = line.substring(indexOfFirstTab + 1, indexOfSemicolon);
                    String[] namesStrArr = namesStr.split("[ ,]+");
                    String name = namesStrArr[0].trim();
                    if (name != null && !name.equals("")) {
                        geneIdToSymbolMap.put(geneId, name);
                    }
                }
            }
        }

        bufReader.close();

        for (String geneId : geneIdToSymbolMap.keySet()) {
            geneSymbolToIdMap.put(geneIdToSymbolMap.get(geneId), geneId);
        }

    }

    /**
     * Fills geneIdToSymbolsMap and geneSymbolToIdMap HashMap objects.
     * geneIdToSymbolsMap contains all alternative names given in hsaGenes.txt
     *
     * @param directoryPath
     * @param geneIdToSymbolsMap
     * @param geneSymbolToIdMap
     */
    public static void parseGeneListWithAlternativeNames(String filePath, HashMap<String, ArrayList<String>> geneIdToSymbolsMap, HashMap<String, String> geneSymbolToIdMap) {

        try {

            BufferedReader bufReader = new BufferedReader(new FileReader(filePath));

            String line;
            while ((line = bufReader.readLine()) != null) {

                int indexOfRefSeq = line.indexOf("(RefSeq)");
                int indexOfSemicolon = line.indexOf(';');
                int indexOfFirstTab = line.indexOf('\t');

                String geneId = line.substring(0, indexOfFirstTab);

                if (indexOfRefSeq >= 0 && indexOfSemicolon > indexOfRefSeq) {
                    String namesStr = line.substring(indexOfRefSeq + 9, indexOfSemicolon);
                    String[] namesStrArr = namesStr.split("[ ,]+");

                    ArrayList<String> geneNames = new ArrayList<String>();

                    for (int i = 0; i < namesStrArr.length; i++) {
                        geneNames.add(namesStrArr[i].trim());
                    }
                    geneIdToSymbolsMap.put(geneId, geneNames);
                }

            }
            bufReader.close();

            //Filling geneNamesToIdMap
            //Gene names are mapped to hsa codes.
            //At first, the first names in the names list are assigned to codes.
            //Then other names are assigned.
            //The reason for this is as follows: "HGF" is written next to three codes but it actually has its own code,
            //the code in which it is in the first order. A name which is not first in any code, will be assigned to 
            //the first code it is seen with.
            for (String geneId : geneIdToSymbolsMap.keySet()) {
                ArrayList<String> geneNames = geneIdToSymbolsMap.get(geneId);
                geneSymbolToIdMap.put(geneNames.get(0), geneId);
            }
            for (String geneId : geneIdToSymbolsMap.keySet()) {
                ArrayList<String> geneNames = geneIdToSymbolsMap.get(geneId);
                for (String geneName : geneNames) {
                    if (!geneSymbolToIdMap.containsKey(geneName)) {
                        geneSymbolToIdMap.put(geneName, geneId);
                    }
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    /**
     * @param filePath
     * @param geneIdsSet
     */
    public static void getGenesInPathwaysSet(String filePath, HashSet<String> geneIdsSet) throws IOException{

        BufferedReader bufReader = new BufferedReader(new FileReader(filePath));

        String line;
        while ((line = bufReader.readLine()) != null) {
            geneIdsSet.add(line.trim());
        }
        bufReader.close();

    }

}
