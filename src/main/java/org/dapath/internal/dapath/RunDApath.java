/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dapath.internal.dapath;

import org.dapath.internal.cyvisualization.GraphVisualizationCy;
import org.dapath.internal.pathway.EntryType;
import org.dapath.internal.pathway.Pathway;
import org.dapath.internal.pathway.Entry;
import org.dapath.internal.keggoperations.KGMLParser;
import org.dapath.internal.keggoperations.GeneListParser;
import org.dapath.internal.keggoperations.KEGGDownloader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Properties;

/**
 *
 * @author Ozan Ozisik
 */
public class RunDApath {

    
    public static void main(String args[]) throws IOException{
        //TODO: Property reading can be added here for runs from console
        
        Properties props = new Properties();
        FileReader fr = new FileReader(Parameters.propertiesFile);
        props.load(fr);

        Parameters.keggFolder=props.getProperty("keggFolder");
        Parameters.expFilePath=new String[1];
        Parameters.expFilePath[0] = props.getProperty("expFilePath");
        Parameters.outputFolder=props.getProperty("outputFolder");
        Parameters.expFile_SkipFirstLine=Boolean.parseBoolean(props.getProperty("expFile_SkipFirstLine"));
        Parameters.topPathwayNumberToBeWritten=Integer.parseInt(props.getProperty("topPathwayNumberToBeWritten"));
        if(Boolean.parseBoolean(props.getProperty("useCrossTalkHandling"))){
            Parameters.crossTalkHandlingMethod=3;
        }else{
            Parameters.crossTalkHandlingMethod=0;
        }
        Parameters.crossTalkLimit=Integer.parseInt(props.getProperty("crossTalkLimit"));
        
            
        
        runDASPA();
    }
    
    
    public static void runDASPA() {

        try {

            ArrayList<Pathway> pathwayList = new ArrayList<Pathway>();
            HashMap<String, String> geneSymbolToIdMap = new HashMap<String, String>();
            HashMap<String, String> geneIdToSymbolMap = new HashMap<String, String>();
            HashSet<String> geneIdsInPathwaysSet = new HashSet<String>();

            readPathways(pathwayList, geneIdToSymbolMap, geneSymbolToIdMap, geneIdsInPathwaysSet);

//            ArrayList<FixedSizePriorityQueue<PathScorePair>> pathPriorityList = new ArrayList<FixedSizePriorityQueue<PathScorePair>>();
//            for (int i = 0; i < Parameters.expFilePath.length; i++) {
//                pathPriorityList.add(new FixedSizePriorityQueue<PathScorePair>(Parameters.pathQueueSize));
//            }

//            ArrayList<PriorityQueue<PathScorePair>> pathPriorityList = new ArrayList<PriorityQueue<PathScorePair>>();
            ArrayList<ArrayList<PathScorePair>> pathsList=new ArrayList<>();
            ArrayList<HashMap<String, Double>> idToValueMapList=new ArrayList<>();

            for (int expFileNo = 0; expFileNo < Parameters.expFilePath.length; expFileNo++) {

                System.out.println("\n\n" + Parameters.expFilePath[expFileNo] + "\n\n");

                HashMap<String, Double> idToValueMap = ExperimentFileReaderPDirect.readExperimentFile(Parameters.expFilePath[expFileNo], geneSymbolToIdMap, geneIdsInPathwaysSet);
                HashMap<String, Double> idToValueMapBeforeCrosstalkHandling = new HashMap<>(idToValueMap);
                updateValuesAccordingToCrosstalkHandlingMethodAndBaseNodeProbability(idToValueMap, pathwayList);

                idToValueMapList.add(idToValueMap);
                
                EntryRelationTypePair.setIdToValueMap(idToValueMap);
                EntryRelationTypePair.setIdToValueMapBeforeCrosstalkHandling(idToValueMapBeforeCrosstalkHandling);

//                FixedSizePriorityQueue<PathScorePair> pathPriority = pathPriorityList.get(expFileNo);
                
                //This is used to store paths. In the FixedSizePriorityQueue paths are ordered by their scores,
                //path with least score is the head. When the queue is filled the head is polled (removed)
                FixedSizePriorityQueue<PathScorePair> pathPriority=new FixedSizePriorityQueue<PathScorePair>(Parameters.pathQueueSize);

                for (Pathway pathway : pathwayList) {
                    AnalyzerProbabilistic analyzer = new AnalyzerProbabilistic();
                    analyzer.analyze(pathway, geneIdsInPathwaysSet, idToValueMap, pathPriority);
                }
                
//                PriorityQueue<PathScorePair> pathPriorityDecreasingOrder = new PriorityQueue<PathScorePair>(pathPriority.size(), Collections.reverseOrder());
//                pathPriorityDecreasingOrder.addAll(pathPriority);
                
                ArrayList<PathScorePair> paths=new ArrayList<>();
                while(!pathPriority.isEmpty()){
                    paths.add(pathPriority.poll());
                }
                Collections.reverse(paths);
                
                pathsList.add(paths);
//                pathPriorityList.add(pathPriorityDecreasingOrder);

                String fileNamePostFix = Parameters.expFilePath[expFileNo].substring(Parameters.expFilePath[expFileNo].lastIndexOf(File.separatorChar) + 1)+".csv";
                //writePaths(fileNamePostFix, pathPriority);
//                writePaths(fileNamePostFix, pathPriorityDecreasingOrder);
                writePaths(fileNamePostFix, paths);


                /**
                 * Creating a list of merged paths that consist of similar paths
                 * of different pathways
                 */
//                ArrayList<MergedPathDifferentPathways> mergedPathDifferentPathwaysList=new ArrayList<>();
//                mergePathsDifferentPathways(paths, idToValueMap, idToValueMapBeforeCrosstalkHandling,mergedPathDifferentPathwaysList);
//                writeMergedPathsDifferentPathways(fileNamePostFix, mergedPathDifferentPathwaysList);

                
                
                /**
                 * Creating a list of merged paths that consist of similar paths
                 * of the same pathway
                 */
                ArrayList<MergedPath> mergedPathList=new ArrayList<>();
                mergePaths(paths, idToValueMap, idToValueMapBeforeCrosstalkHandling,mergedPathList);
                writeMergedPaths(fileNamePostFix, mergedPathList);
                
                
                if(Parameters.expFilePath.length==1){
                    try{
                        for (MergedPath mergedPath:mergedPathList) {
                            GraphVisualizationCy.drawMergedImpactPathway(mergedPath.getPathway(), idToValueMap, idToValueMapBeforeCrosstalkHandling, mergedPath.getEntry2ImpactColorMap(), mergedPath.getEntry2FrequencyColorMap(), mergedPath.getPathwayRank(), mergedPath.getPathRank());
                        }
                    }catch(Exception ex){
                        System.out.println(ex.getMessage());
                    }
                }


//                writeRSIdsOfGenesInTopPaths(fileNamePostFix, pathPriority);
            }
            
            if(Parameters.expFilePath.length>1){
                ArrayList<MergedPath> commonPathList=new ArrayList<>();
                findCommonPaths(pathsList.get(0), pathsList.get(1), idToValueMapList.get(0), idToValueMapList.get(1), commonPathList);
                
                String fileNamePostFix = Parameters.expFilePath[0].substring(Parameters.expFilePath[0].lastIndexOf(File.separatorChar) + 1) +
                                        Parameters.expFilePath[1].substring(Parameters.expFilePath[1].lastIndexOf(File.separatorChar) + 1);
                writeMergedPaths(fileNamePostFix, commonPathList);
                
                try{
                    for (MergedPath commonPath:commonPathList) {
                        //TODO: idToValueMap olarak birincininkini verdim.
                        GraphVisualizationCy.drawMergedImpactPathway(commonPath.getPathway(), idToValueMapList.get(0), idToValueMapList.get(1), commonPath.getEntry2ImpactColorMap(), commonPath.getEntry2FrequencyColorMap(), commonPath.getPathwayRank(), commonPath.getPathRank());
                    }
                }catch(Exception ex){
                    System.out.println(ex.getMessage());
                }
            }

        } catch (FileNotFoundException e) {
            System.out.println("File cannot be found.");
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void readPathways(ArrayList<Pathway> pathwayList, HashMap<String, String> geneIdToSymbolMap, HashMap<String, String> geneSymbolToIdMap, HashSet<String> geneIdsInPathwaysSet) throws IOException {
        File dir = new File(Parameters.keggFolder);
        File[] directoryListing = dir.listFiles();

        if ((directoryListing == null) || directoryListing.length == 0) {
            if(Parameters.downloadKeggIfAbsent){
                KEGGDownloader.downloadAll(Parameters.keggFolder);
                directoryListing = dir.listFiles();
            }else{
                throw(new IOException("Problem with KEGG folder"));
            }
        }

        GeneListParser.parseGeneList(Parameters.keggFolder + File.separatorChar + "hsaGenes.txt", geneIdToSymbolMap, geneSymbolToIdMap);
        GeneListParser.getGenesInPathwaysSet(Parameters.keggFolder + File.separatorChar + "hsaGeneIdsInPathways.txt", geneIdsInPathwaysSet);

        for (File file : directoryListing) {
            if (file.getName().endsWith("xml")) {
                KGMLParser kp = new KGMLParser();
                Pathway pathway = kp.read(new FileInputStream(file), geneIdToSymbolMap);

                if (pathway.getIncludesUsableRelation()) {
                    pathwayList.add(pathway);
                }
            }
        }
    }

    /**
     * This method increases the p-value (decreases the significance) of genes
     * that are part of many pathways to highlight more specific pathways. The
     * p-values are increased to at most Parameters.baseNodeProb-epsilon
     *
     * @param idToValueMap
     * @param pathwayList
     */
    public static void updateValuesAccordingToCrosstalkHandlingMethodAndBaseNodeProbability(HashMap<String, Double> idToValueMap, ArrayList<Pathway> pathwayList) {

        if (Parameters.crossTalkHandlingMethod != 0) {

            double epsilon = 0.0;

            HashMap<String, Integer> crossTalkHM = new HashMap<String, Integer>();
            for (String id : idToValueMap.keySet()) {
                crossTalkHM.put(id, 0);
            }

            for (Pathway pathway : pathwayList) {
                for (Entry e : pathway.getEntryList()) {
                    String id = e.getEntryId();
                    if (crossTalkHM.containsKey(id)) {
                        crossTalkHM.put(id, crossTalkHM.get(id) + 1);
                    }
                }
            }

            for (String id : crossTalkHM.keySet()) {
                if (crossTalkHM.get(id) > 1) {
                    if (Parameters.crossTalkHandlingMethod == 1) {
                        int limit = Parameters.crossTalkLimit;
                        if (crossTalkHM.get(id) > limit) {
                            idToValueMap.put(id, Parameters.baseNodeProb - epsilon);
                        }
                    } else if (Parameters.crossTalkHandlingMethod == 2) {
                        Double p = idToValueMap.get(id);
                        for (int i = 0; i < crossTalkHM.get(id)-1; i++) {
                            p = p * 10;
                        }
                        if (p >= Parameters.baseNodeProb) {
                            p = Parameters.baseNodeProb - epsilon;
                        }
                        idToValueMap.put(id, p);
                    } else if (Parameters.crossTalkHandlingMethod == 3) {
                        Double p = idToValueMap.get(id);
                        int limit = Parameters.crossTalkLimit;
                        if (crossTalkHM.get(id) > limit) {
                            int mult;
                            
                            //10,100, +5
                            mult = (crossTalkHM.get(id) - limit) * 5;
                            mult += 5;
                            if (mult > 100) {
                                mult = 100;
                            }
                            
                            //10,100, +10
//                            mult = (crossTalkHM.get(id) - limit) * 10;
//                            mult += 0;
//                            if (mult > 100) {
//                                mult = 100;
//                            }                            

                            //10,1000, +10
//                            mult = (crossTalkHM.get(id) - limit) * 10;
//                            mult += 0;
//                            if (mult > 1000) {
//                                mult = 1000;
//                            }

                            p = p * mult;
                            if (p >= Parameters.baseNodeProb) {
                                p = Parameters.baseNodeProb - epsilon;
                            }
                            idToValueMap.put(id, p);
                        }
                    }
                }
            }
        }

    }
    
    public static void writePaths(String fileNamePostFix, ArrayList<PathScorePair> paths) throws IOException {
//    public static void writePaths(String fileNamePostFix, PriorityQueue<PathScorePair> pathPriorityDecreasingOrder) throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Calendar cal = Calendar.getInstance();
        BufferedWriter bw = new BufferedWriter(new FileWriter(Parameters.outputFolder+File.separatorChar+dateFormat.format(cal.getTime()) + "_ResultPaths_" + fileNamePostFix));

        HashSet<String> writtenPathSet = new HashSet<String>();
        
//        //Creating a copy
//        PriorityQueue<PathScorePair> reversed = new PriorityQueue<PathScorePair>(pathPriorityDecreasingOrder.size(), Collections.reverseOrder());
//        reversed.addAll(pathPriorityDecreasingOrder);
        
        int curSetSize = 0;
        for(PathScorePair psp:paths){
//        while (!reversed.isEmpty() && writtenPathSet.size() < Parameters.distinctPathwaysInPath) {
//            PathScorePair psp = reversed.poll();

            writtenPathSet.add(psp.getPathwayTitle());
            bw.write(psp.toString());

            String link = psp.getPathway().getLink();
            if (link != null) {
                ArrayList<EntryRelationTypePair> path = psp.getPath();
                for (EntryRelationTypePair er : path) {
                    Entry entry1 = er.getEntry();
                    if (entry1.getType() == EntryType.GROUP || entry1.getType() == EntryType.MULTIGENE) {
                        for (Entry entry2 : entry1.getComponents()) {
                            if (entry2.getType() == EntryType.MULTIGENE) {
                                for (Entry entry3 : entry2.getComponents()) {
                                    link = link + "+" + entry3.getEntryId();
                                }
                            } else {
                                link = link + "+" + entry2.getEntryId();
                            }
                        }
                    } else {
                        link = link + "+" + entry1.getEntryId();
                    }
                }
                bw.write("\t" + link);
            }

            bw.newLine();
            if (writtenPathSet.size() % 10 == 0 && writtenPathSet.size() > curSetSize) {
                bw.write("First " + writtenPathSet.size() + " ended.");
                bw.newLine();
                curSetSize = writtenPathSet.size() + 1;
            }
        }
        bw.close();

    }


//Old implementation
    
//    public static void writePathsCommonRemoved(String fileNamePostFix, PriorityQueue<PathScorePair> pathPriorityDecreasingOrder, HashMap<String, Double> idToValueMap, HashMap<String, Double> idToValueMapBeforeCrosstalkHandling, ArrayList<HashSet<Entry>> entriesOfCommonPathsList, ArrayList<Pathway> pathwaysThePathsBelongTo, ArrayList<HashMap<Entry, Integer>> entry2ImpactColorMapList, ArrayList<HashMap<Entry, Integer>> entry2FrequencyColorMapList, HashMap<Pathway, Integer> pathway2RankMap) throws IOException {
//        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
//        Calendar cal = Calendar.getInstance();
//        BufferedWriter bw = new BufferedWriter(new FileWriter(Parameters.outputFolder+File.separatorChar+dateFormat.format(cal.getTime()) + "_ResultPaths_" + fileNamePostFix));
//
//        HashSet<String> writtenPathSet = new HashSet<String>();
//
//        //Creating a copy
//        PriorityQueue<PathScorePair> reversed = new PriorityQueue<PathScorePair>(pathPriorityDecreasingOrder.size(), Collections.reverseOrder());
//        reversed.addAll(pathPriorityDecreasingOrder);
//
//        ArrayList<PathScorePair> reversedAL = new ArrayList<PathScorePair>();
//        while (!reversed.isEmpty()) {
//            reversedAL.add(reversed.poll());
//        }
//
//        Integer nextPathwayRank = 1;
//
//        String thresholdPathway = findThresholdPathway(reversedAL);
//
//        int curSetSize = 0;
//        PathScorePair psp;
//        int i = 0;
//        int pathNo = 1;
//        while (!(psp = reversedAL.get(i)).getPathwayTitle().equals(thresholdPathway)) {
//
//            if (pathway2RankMap.get(psp.getPathway()) == null) {
//                pathway2RankMap.put(psp.getPathway(), nextPathwayRank);
//                nextPathwayRank++;
//            }
//
//            HashMap<Entry, Integer> entry2ImpactColorMap = new HashMap<Entry, Integer>();
//            HashMap<Entry, Integer> entry2FrequencyColorMap = new HashMap<Entry, Integer>();
//
//            HashSet<Entry> entriesOfCommonPaths = new HashSet<Entry>();
//            for (EntryRelationTypePair er : psp.getPath()) {
//                entriesOfCommonPaths.add(er.getEntry());
//                entry2FrequencyColorMap.put(er.getEntry(), 1);
//            }
//
//            double scorePSP = psp.getScore();
//            ArrayList<EntryRelationTypePair> pathOfPsp = psp.getPath();
////			ArrayList<ArrayList<EntryRelationTypePair>> commonPathList=new ArrayList<ArrayList<EntryRelationTypePair>>();
//            ArrayList<PathScorePair> removedPathList = new ArrayList<PathScorePair>();
//
//            int j = i + 1;
//            PathScorePair pspCur;
//            while (j < reversedAL.size() && !reversedAL.get(j).getPathwayTitle().equals(thresholdPathway)) {
////			while(j<reversedAL.size() && !reversedAL.get(j).getPathwayName().equals(thresholdPathway) && (pspCur=reversedAL.get(j)).getScore()>=scorePSP/2){
//                pspCur = reversedAL.get(j);
//                if (pspCur.getPathwayTitle().equals(psp.getPathwayTitle())) {
//                    ArrayList<EntryRelationTypePair> pathOfPspCur = pspCur.getPath();
//                    HashSet<Entry> entriesOfPSPCur = new HashSet<Entry>();
//                    for (EntryRelationTypePair er : pathOfPspCur) {
//                        entriesOfPSPCur.add(er.getEntry());
//                    }
//
//                    int common = 0;
//                    for (EntryRelationTypePair er : pathOfPsp) {
//                        if (entriesOfPSPCur.contains(er.getEntry())) {
//                            common++;
//                        }
//                    }
//
//                    int commonExt = 0;//Number of common entries with the collection of merged paths
//
//                    for (Entry entry : entriesOfPSPCur) {
//                        if (entriesOfCommonPaths.contains(entry)) {
//                            commonExt++;
//                        }
//                    }
//
//                    double jaccardInd = ((double) common) / (pathOfPsp.size() + pathOfPspCur.size() - common);
//
//                    if (jaccardInd >= 0.5 || commonExt == entriesOfPSPCur.size()) {
//                        removedPathList.add(reversedAL.get(j));
//
//                        for (EntryRelationTypePair er : reversedAL.get(j).getPath()) {
//                            entriesOfCommonPaths.add(er.getEntry());
//                            Integer freq;
//                            if ((freq = entry2FrequencyColorMap.get(er.getEntry())) == null) {
//                                entry2FrequencyColorMap.put(er.getEntry(), 1);
//                            } else {
//                                entry2FrequencyColorMap.put(er.getEntry(), freq + 1);
//                            }
//                        }
//
//                        reversedAL.remove(j);
//
//                        j = i;//Start again for commonExt control
//                    }
//                }
//                j++;
//            }
//
//            entriesOfCommonPathsList.add(entriesOfCommonPaths);
//            pathwaysThePathsBelongTo.add(psp.getPathway());
//            entry2FrequencyColorMapList.add(entry2FrequencyColorMap);
//
//            writtenPathSet.add(psp.getPathwayTitle());
//            
//            //Burada psp'nin skoru ve pathi yazılıyor, ortak node'lar sadece link'te görülebiliyor.
//            bw.write(psp.toString());
//
//            String link = psp.getPathway().getLink();
//            if (link != null) {
//                for (Entry entry1 : entriesOfCommonPaths) {
//                    if (entry1.getType() == EntryType.GROUP || entry1.getType() == EntryType.MULTIGENE) {
//                        for (Entry entry2 : entry1.getComponents()) {
//                            if (entry2.getType() == EntryType.MULTIGENE) {
//                                for (Entry entry3 : entry2.getComponents()) {
//                                    link = link + "+" + entry3.getEntryId();
//                                }
//                            } else {
//                                link = link + "+" + entry2.getEntryId();
//                            }
//                        }
//                    } else {
//                        link = link + "+" + entry1.getEntryId();
//                    }
//                }
//                bw.write("\t" + link);
////				bw.write("\t"+);
//            }
//
//            bw.newLine();
//            if (writtenPathSet.size() % 10 == 0 && writtenPathSet.size() > curSetSize) {
//                bw.write("First " + writtenPathSet.size() + " ended.");
//                bw.newLine();
//                curSetSize = writtenPathSet.size() + 1;
//            }
//
//            for (Entry entry : entriesOfCommonPaths) {
//                entry2ImpactColorMap.put(entry, 0);
//                if (entry.getType() == EntryType.GROUP || entry.getType() == EntryType.MULTIGENE) {
//                    for (Entry entry2 : entry.getComponents()) {
//                        if (entry2.getType() == EntryType.MULTIGENE) {
//                            for (Entry entry3 : entry2.getComponents()) {
//                                if (entry2ImpactColorMap.get(entry) < getColor(entry3, idToValueMap, idToValueMapBeforeCrosstalkHandling)) {
//                                    entry2ImpactColorMap.put(entry, getColor(entry3, idToValueMap, idToValueMapBeforeCrosstalkHandling));
//                                }
//                            }
//                        } else if (entry2ImpactColorMap.get(entry) < getColor(entry2, idToValueMap, idToValueMapBeforeCrosstalkHandling)) {
//                            entry2ImpactColorMap.put(entry, getColor(entry2, idToValueMap, idToValueMapBeforeCrosstalkHandling));
//                        }
//                    }
//                } else {
//                    entry2ImpactColorMap.put(entry, getColor(entry, idToValueMap, idToValueMapBeforeCrosstalkHandling));
//                }
//            }
//            entry2ImpactColorMapList.add(entry2ImpactColorMap);
//
//            pathNo++;
//            i++;
//        }
//
//        bw.close();
//
//    }
    
    
    public static void writeMergedPaths(String fileNamePostFix, ArrayList<MergedPath> mergedPathList) throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Calendar cal = Calendar.getInstance();
        BufferedWriter bw = new BufferedWriter(new FileWriter(Parameters.outputFolder+File.separatorChar+dateFormat.format(cal.getTime()) + "_ResultPathsClustered_" + fileNamePostFix));
        HashSet<String> writtenPathwaySet = new HashSet<String>();
        int curWrittenPathwaySetSize = 0;
        
        for(MergedPath mergedPath:mergedPathList){
            bw.write(mergedPath.getPathway().getOrg()+mergedPath.getPathway().getNumber()+"\t");
            bw.write(mergedPath.getPathway().getTitle()+"\t");
            bw.write(mergedPath.getPathwayRank()+"\t");
            bw.write(mergedPath.getPathRank()+"\t");
            bw.write(mergedPath.getScoreOfFrontierPath()+"\t");
            
            writtenPathwaySet.add(mergedPath.getPathway().getTitle());

            HashSet<Entry> entriesOfCommonPaths=mergedPath.getEntriesOfCommonPaths();
            String link = mergedPath.getPathway().getLink();
            if (link != null) {
                for (Entry entry1 : entriesOfCommonPaths) {
                    if (entry1.getType() == EntryType.GROUP || entry1.getType() == EntryType.MULTIGENE) {
                        for (Entry entry2 : entry1.getComponents()) {
                            if (entry2.getType() == EntryType.MULTIGENE) {
                                for (Entry entry3 : entry2.getComponents()) {
                                    link = link + "+" + entry3.getEntryId();
                                }
                            } else {
                                link = link + "+" + entry2.getEntryId();
                            }
                        }
                    } else {
                        link = link + "+" + entry1.getEntryId();
                    }
                }
                bw.write(link);
            }

            bw.newLine();
            if (writtenPathwaySet.size() % 10 == 0 && writtenPathwaySet.size() > curWrittenPathwaySetSize) {
                bw.write("First " + writtenPathwaySet.size() + " ended.");
                bw.newLine();
                curWrittenPathwaySetSize = writtenPathwaySet.size() + 1;
            }
        }

        bw.close();

    }
    
    public static void mergePaths(ArrayList<PathScorePair> paths, HashMap<String, Double> idToValueMap, HashMap<String, Double> idToValueMapBeforeCrosstalkHandling, ArrayList<MergedPath> mergedPathList){
        
        ArrayList<PathScorePair> pathsCopy = new ArrayList<>(paths);
        HashMap<Pathway, Integer> pathway2RankMap=new HashMap<>();
        Integer nextPathwayRank = 1;

        String thresholdPathway = findThresholdPathway(pathsCopy);

        PathScorePair psp;
        int i = 0;
        int pathNo = 1;
        while (!(psp = pathsCopy.get(i)).getPathwayTitle().equals(thresholdPathway)) {

            if (pathway2RankMap.get(psp.getPathway()) == null) {
                pathway2RankMap.put(psp.getPathway(), nextPathwayRank);
                nextPathwayRank++;
            }

            HashMap<Entry, Integer> entry2ImpactColorMap = new HashMap<>();
            HashMap<Entry, Integer> entry2FrequencyColorMap = new HashMap<>();

            HashSet<Entry> entriesOfCommonPaths = new HashSet<>();
            for (EntryRelationTypePair er : psp.getPath()) {
                entriesOfCommonPaths.add(er.getEntry());
                entry2FrequencyColorMap.put(er.getEntry(), 1);
            }

            double scorePSP = psp.getScore();
            ArrayList<EntryRelationTypePair> pathOfPsp = psp.getPath();

            int j = i + 1;
            PathScorePair pspCur;
            while (j < pathsCopy.size() && !pathsCopy.get(j).getPathwayTitle().equals(thresholdPathway)) {
//            while(j<reversedAL.size() && !reversedAL.get(j).getPathwayName().equals(thresholdPathway) && (pspCur=reversedAL.get(j)).getScore()>=scorePSP/2){
                pspCur = pathsCopy.get(j);

                if (pspCur.getPathwayTitle().equals(psp.getPathwayTitle())) {
                    ArrayList<EntryRelationTypePair> pathOfPspCur = pspCur.getPath();
                    HashSet<Entry> entriesOfPSPCur = new HashSet<>();
                    for (EntryRelationTypePair er : pathOfPspCur) {
                        entriesOfPSPCur.add(er.getEntry());
                    }

                    int common = 0;
                    for (EntryRelationTypePair er : pathOfPsp) {
                        if (entriesOfPSPCur.contains(er.getEntry())) {
                            common++;
                        }
                    }

                    int commonExt = 0;//Number of common entries with the collection of merged paths
                    //Path will be deleted if it is contained by the the readily merged paths.
                    for (Entry entry : entriesOfPSPCur) {
                        if (entriesOfCommonPaths.contains(entry)) {
                            commonExt++;
                        }
                    }

//                    //Overlap index defined in http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0099030
//                    double minOfSizes= pathOfPsp.size() < pathOfPspCur.size() ? pathOfPsp.size() : pathOfPspCur.size();
//                    double overlapInd = (double) common / minOfSizes;
//                    if (overlapInd >= 0.6 || commonExt == entriesOfPSPCur.size()) {
                        
                    double jaccardInd = ((double) common) / (pathOfPsp.size() + pathOfPspCur.size() - common);
                    if (jaccardInd >= 0.5 || commonExt == entriesOfPSPCur.size()) {
                        
                        for (EntryRelationTypePair er : pathsCopy.get(j).getPath()) {
                            entriesOfCommonPaths.add(er.getEntry());
                            Integer freq;
                            if ((freq = entry2FrequencyColorMap.get(er.getEntry())) == null) {
                                entry2FrequencyColorMap.put(er.getEntry(), 1);
                            } else {
                                entry2FrequencyColorMap.put(er.getEntry(), freq + 1);
                            }
                        }

                        pathsCopy.remove(j);

                        j = i;//Start again for commonExt control
                    }
                }
                j++;
            }

            for (Entry entry : entriesOfCommonPaths) {
                if(!entry2ImpactColorMap.containsKey(entry)){
                    entry2ImpactColorMap.put(entry, 0);
                }
                if (entry.getType() == EntryType.GROUP || entry.getType() == EntryType.MULTIGENE) {
                    for (Entry entry2 : entry.getComponents()) {
                        if (entry2.getType() == EntryType.MULTIGENE) {
                            for (Entry entry3 : entry2.getComponents()) {
                                if (entry2ImpactColorMap.get(entry) < getColor(entry3, idToValueMap, idToValueMapBeforeCrosstalkHandling)) {
                                    entry2ImpactColorMap.put(entry, getColor(entry3, idToValueMap, idToValueMapBeforeCrosstalkHandling));
                                }
                            }
                        } else if (entry2ImpactColorMap.get(entry) < getColor(entry2, idToValueMap, idToValueMapBeforeCrosstalkHandling)) {
                            entry2ImpactColorMap.put(entry, getColor(entry2, idToValueMap, idToValueMapBeforeCrosstalkHandling));
                        }
                    }
                } else {
                    entry2ImpactColorMap.put(entry, getColor(entry, idToValueMap, idToValueMapBeforeCrosstalkHandling));
                }
            }
            
//            entriesOfCommonPathsList.add(entriesOfCommonPaths);
//            pathwaysThePathsBelongTo.add(psp.getPathway());
//            entry2FrequencyColorMapList.add(entry2FrequencyColorMap);
//            entry2ImpactColorMapList.add(entry2ImpactColorMap);
            
            MergedPath mergedPath=new MergedPath(entriesOfCommonPaths, psp.getScore(), psp.getPathway(), pathway2RankMap.get(psp.getPathway()), pathNo, entry2ImpactColorMap, entry2FrequencyColorMap);
            mergedPathList.add(mergedPath);
//            System.out.println(mergedPath);

            pathNo++;
            i++;
        }
    }


    public static void writeMergedPathsDifferentPathways(String fileNamePostFix, ArrayList<MergedPathDifferentPathways> mergedPathList) throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Calendar cal = Calendar.getInstance();
        BufferedWriter bw = new BufferedWriter(new FileWriter(Parameters.outputFolder+File.separatorChar+dateFormat.format(cal.getTime()) + "_ResultPathsMergeInterPathway_" + fileNamePostFix));
        HashSet<String> writtenPathwaySet = new HashSet<String>();
        int curWrittenPathwaySetSize = 0;
        
        for(MergedPathDifferentPathways mergedPath:mergedPathList){
            for(Pathway pathway:mergedPath.getPathwayList()){
                bw.write(pathway.getTitle()+",");
            }
            bw.write("\t");
            bw.write(mergedPath.getPathwayRank()+"\t");
            bw.write(mergedPath.getPathRank()+"\t");
            bw.write(mergedPath.getScoreOfFrontierPath()+"\t");
            
            writtenPathwaySet.add(mergedPath.getPathwayList().get(0).getTitle());

            HashSet<Entry> entriesOfCommonPaths=mergedPath.getEntriesOfCommonPaths();
            String link = mergedPath.getPathwayList().get(0).getLink();
            if (link != null) {
                for (Entry entry1 : entriesOfCommonPaths) {
                    if (entry1.getType() == EntryType.GROUP || entry1.getType() == EntryType.MULTIGENE) {
                        for (Entry entry2 : entry1.getComponents()) {
                            if (entry2.getType() == EntryType.MULTIGENE) {
                                for (Entry entry3 : entry2.getComponents()) {
                                    link = link + "+" + entry3.getEntryId();
                                }
                            } else {
                                link = link + "+" + entry2.getEntryId();
                            }
                        }
                    } else {
                        link = link + "+" + entry1.getEntryId();
                    }
                }
                bw.write(link);
            }

            bw.newLine();
            if (writtenPathwaySet.size() % 10 == 0 && writtenPathwaySet.size() > curWrittenPathwaySetSize) {
                bw.write("First " + writtenPathwaySet.size() + " ended.");
                bw.newLine();
                curWrittenPathwaySetSize = writtenPathwaySet.size() + 1;
            }
        }

        bw.close();

    }
    
    public static void mergePathsDifferentPathways(ArrayList<PathScorePair> paths, HashMap<String, Double> idToValueMap, HashMap<String, Double> idToValueMapBeforeCrosstalkHandling, ArrayList<MergedPathDifferentPathways> mergedPathList){
        
        ArrayList<PathScorePair> pathsCopy = new ArrayList<>(paths);
        HashMap<Pathway, Integer> pathway2RankMap=new HashMap<>();
        Integer nextPathwayRank = 1;

        String thresholdPathway = findThresholdPathway(pathsCopy);

        PathScorePair psp;
        int i = 0;
        int pathNo = 1;
        while (!(psp = pathsCopy.get(i)).getPathwayTitle().equals(thresholdPathway)) {
            
            ArrayList<Pathway> pathwayList=new ArrayList<>();
            pathwayList.add(psp.getPathway());

            if (pathway2RankMap.get(psp.getPathway()) == null) {
                pathway2RankMap.put(psp.getPathway(), nextPathwayRank);
                nextPathwayRank++;
            }

            HashMap<Entry, Integer> entry2ImpactColorMap = new HashMap<>();
            HashMap<Entry, Integer> entry2FrequencyColorMap = new HashMap<>();

            HashSet<Entry> entriesOfCommonPaths = new HashSet<>();
            for (EntryRelationTypePair er : psp.getPath()) {
                entriesOfCommonPaths.add(er.getEntry());
                entry2FrequencyColorMap.put(er.getEntry(), 1);
            }

            double scorePSP = psp.getScore();
            ArrayList<EntryRelationTypePair> pathOfPsp = psp.getPath();

            int j = i + 1;
            PathScorePair pspCur;
            while (j < pathsCopy.size() && !pathsCopy.get(j).getPathwayTitle().equals(thresholdPathway)) {
//            while(j<reversedAL.size() && !reversedAL.get(j).getPathwayName().equals(thresholdPathway) && (pspCur=reversedAL.get(j)).getScore()>=scorePSP/2){
                pspCur = pathsCopy.get(j);

                ArrayList<EntryRelationTypePair> pathOfPspCur = pspCur.getPath();
                HashSet<Entry> entriesOfPSPCur = new HashSet<>();
                for (EntryRelationTypePair er : pathOfPspCur) {
                    entriesOfPSPCur.add(er.getEntry());
                }

                int common = 0;
                for (EntryRelationTypePair er : pathOfPsp) {
                    if (entriesOfPSPCur.contains(er.getEntry())) {
                        common++;
                    }
                }

                int commonExt = 0;//Number of common entries with the collection of merged paths
                //Path will be deleted if it is contained by the the readily merged paths.
                for (Entry entry : entriesOfPSPCur) {
                    if (entriesOfCommonPaths.contains(entry)) {
                        commonExt++;
                    }
                }

//                    //Overlap index defined in http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0099030
//                    double minOfSizes= pathOfPsp.size() < pathOfPspCur.size() ? pathOfPsp.size() : pathOfPspCur.size();
//                    double overlapInd = (double) common / minOfSizes;
//                    if (overlapInd >= 0.6 || commonExt == entriesOfPSPCur.size()) {
                double jaccardInd = ((double) common) / (pathOfPsp.size() + pathOfPspCur.size() - common);
                if (jaccardInd >= 0.5 || commonExt == entriesOfPSPCur.size()) {

                    for (EntryRelationTypePair er : pathsCopy.get(j).getPath()) {
                        entriesOfCommonPaths.add(er.getEntry());
                        Integer freq;
                        if ((freq = entry2FrequencyColorMap.get(er.getEntry())) == null) {
                            entry2FrequencyColorMap.put(er.getEntry(), 1);
                        } else {
                            entry2FrequencyColorMap.put(er.getEntry(), freq + 1);
                        }
                    }
                    
                    if(!pathwayList.contains(pspCur.getPathway())){
                        pathwayList.add(pspCur.getPathway());
                    }

                    pathsCopy.remove(j);

                    j = i;//Start again for commonExt control
                }
                
                j++;
            }

            for (Entry entry : entriesOfCommonPaths) {
                if(!entry2ImpactColorMap.containsKey(entry)){
                    entry2ImpactColorMap.put(entry, 0);
                }
                if (entry.getType() == EntryType.GROUP || entry.getType() == EntryType.MULTIGENE) {
                    for (Entry entry2 : entry.getComponents()) {
                        if (entry2.getType() == EntryType.MULTIGENE) {
                            for (Entry entry3 : entry2.getComponents()) {
                                if (entry2ImpactColorMap.get(entry) < getColor(entry3, idToValueMap, idToValueMapBeforeCrosstalkHandling)) {
                                    entry2ImpactColorMap.put(entry, getColor(entry3, idToValueMap, idToValueMapBeforeCrosstalkHandling));
                                }
                            }
                        } else if (entry2ImpactColorMap.get(entry) < getColor(entry2, idToValueMap, idToValueMapBeforeCrosstalkHandling)) {
                            entry2ImpactColorMap.put(entry, getColor(entry2, idToValueMap, idToValueMapBeforeCrosstalkHandling));
                        }
                    }
                } else {
                    entry2ImpactColorMap.put(entry, getColor(entry, idToValueMap, idToValueMapBeforeCrosstalkHandling));
                }
            }
            
//            entriesOfCommonPathsList.add(entriesOfCommonPaths);
//            pathwaysThePathsBelongTo.add(psp.getPathway());
//            entry2FrequencyColorMapList.add(entry2FrequencyColorMap);
//            entry2ImpactColorMapList.add(entry2ImpactColorMap);
            
            MergedPathDifferentPathways mergedPath=new MergedPathDifferentPathways(entriesOfCommonPaths, psp.getScore(), pathwayList, pathway2RankMap.get(psp.getPathway()), pathNo, entry2ImpactColorMap, entry2FrequencyColorMap);
            mergedPathList.add(mergedPath);
//            System.out.println(mergedPath);

            pathNo++;
            i++;
        }
    }    
    
    
    
    

    public static void findCommonPaths(ArrayList<PathScorePair> paths1, ArrayList<PathScorePair> paths2, HashMap<String, Double> idToValueMap1, HashMap<String, Double> idToValueMap2, ArrayList<MergedPath> commonPathList){
        
        ArrayList<PathScorePair> paths1Copy = new ArrayList<>(paths1);
        ArrayList<PathScorePair> paths2Copy = new ArrayList<>(paths2);
        HashMap<Pathway, Integer> pathway2RankMap=new HashMap<>();
        Integer nextPathwayRank = 1;

        String thresholdPathway1 = findThresholdPathway(paths1Copy);
        String thresholdPathway2 = findThresholdPathway(paths2Copy);

        boolean commonnessExists;
        
        PathScorePair psp1;
        int i = 0;
        int pathNo = 1;
        while (!(psp1 = paths1Copy.get(i)).getPathwayTitle().equals(thresholdPathway1)) {
            commonnessExists=false;
            if (pathway2RankMap.get(psp1.getPathway()) == null) {
                pathway2RankMap.put(psp1.getPathway(), nextPathwayRank);
                nextPathwayRank++;
            }

            HashMap<Entry, Integer> entry2ImpactColorMap = new HashMap<Entry, Integer>();
            HashMap<Entry, Integer> entry2FrequencyColorMap = new HashMap<Entry, Integer>();

            HashSet<Entry> entriesOfCommonPaths = new HashSet<Entry>();
            for (EntryRelationTypePair er : psp1.getPath()) {
                entriesOfCommonPaths.add(er.getEntry());
                entry2FrequencyColorMap.put(er.getEntry(), 1);
            }

            double scorePSP = psp1.getScore();
            ArrayList<EntryRelationTypePair> pathOfPsp1 = psp1.getPath();

            int j = 0;
            PathScorePair psp2;
            while (j < paths2Copy.size() && !paths2Copy.get(j).getPathwayTitle().equals(thresholdPathway2)) {
//            while(j<reversedAL.size() && !reversedAL.get(j).getPathwayName().equals(thresholdPathway) && (pspCur=reversedAL.get(j)).getScore()>=scorePSP/2){
                psp2 = paths2Copy.get(j);
                if (psp2.getPathwayTitle().equals(psp1.getPathwayTitle())) {
                    ArrayList<EntryRelationTypePair> pathOfPsp2 = psp2.getPath();
                    HashSet<Entry> entriesOfPSP2 = new HashSet<Entry>();
                    for (EntryRelationTypePair er : pathOfPsp2) {
                        entriesOfPSP2.add(er.getEntry());
                    }

                    int common = 0;
                    for (EntryRelationTypePair er : pathOfPsp1) {
                        if (entriesOfPSP2.contains(er.getEntry())) {
                            common++;
                        }
                    }

//                    int commonExt = 0;//Number of common entries with the collection of merged paths
//                    //Path will be deleted if it is contained by the the readily merged paths.
//                    for (Entry entry : entriesOfPSPCur) {
//                        if (entriesOfCommonPaths.contains(entry)) {
//                            commonExt++;
//                        }
//                    }

                    double jaccardInd = ((double) common) / (pathOfPsp1.size() + pathOfPsp2.size() - common);

//                    if (jaccardInd >= 0.5 || commonExt == entriesOfPSPCur.size()) {
                    if (jaccardInd >= 0.5) {
                        commonnessExists=true;
                        for (EntryRelationTypePair er : paths2Copy.get(j).getPath()) {
                            entriesOfCommonPaths.add(er.getEntry());
                            Integer freq;
                            if ((freq = entry2FrequencyColorMap.get(er.getEntry())) == null) {
                                entry2FrequencyColorMap.put(er.getEntry(), 1);
                            } else {
                                entry2FrequencyColorMap.put(er.getEntry(), freq + 1);
                            }
                        }

//                        pathsCopy.remove(j);

//                        j = i;//Start again for commonExt control
                    }
                }
                j++;
            }

            for (Entry entry : entriesOfCommonPaths) {
                if(!entry2ImpactColorMap.containsKey(entry)){
                    entry2ImpactColorMap.put(entry, 0);
                }
                if (entry.getType() == EntryType.GROUP || entry.getType() == EntryType.MULTIGENE) {
                    for (Entry entry2 : entry.getComponents()) {
                        if (entry2.getType() == EntryType.MULTIGENE) {
                            for (Entry entry3 : entry2.getComponents()) {
                                if (entry2ImpactColorMap.get(entry) < getColor2(entry3, idToValueMap1, idToValueMap2)) {
                                    entry2ImpactColorMap.put(entry, getColor2(entry3, idToValueMap1, idToValueMap2));
                                }
                            }
                        } else if (entry2ImpactColorMap.get(entry) < getColor2(entry2, idToValueMap1, idToValueMap2)) {
                            entry2ImpactColorMap.put(entry, getColor2(entry2, idToValueMap1, idToValueMap2));
                        }
                    }
                } else {
                    entry2ImpactColorMap.put(entry, getColor2(entry, idToValueMap1, idToValueMap2));
                }
            }
            
//            entriesOfCommonPathsList.add(entriesOfCommonPaths);
//            pathwaysThePathsBelongTo.add(psp.getPathway());
//            entry2FrequencyColorMapList.add(entry2FrequencyColorMap);
//            entry2ImpactColorMapList.add(entry2ImpactColorMap);
            
            if(commonnessExists){
                MergedPath commonPath=new MergedPath(entriesOfCommonPaths, psp1.getScore(), psp1.getPathway(), pathway2RankMap.get(psp1.getPathway()), pathNo, entry2ImpactColorMap, entry2FrequencyColorMap);
                commonPathList.add(commonPath);
            }

            pathNo++;
            i++;
        }
        
        
        for(i=0;i<commonPathList.size()-1;i++){
            HashSet<Entry> entriesOfCommonPaths1=commonPathList.get(i).getEntriesOfCommonPaths();
            for(int j=i+1;j<commonPathList.size();j++){
                HashSet<Entry> entriesOfCommonPaths2=commonPathList.get(j).getEntriesOfCommonPaths();
                if(entriesOfCommonPaths1.equals(entriesOfCommonPaths2)){
                    commonPathList.remove(j);
                    j--;
                }
            }
            commonPathList.set(i, new MergedPath(commonPathList.get(i).getEntriesOfCommonPaths(), commonPathList.get(i).getScoreOfFrontierPath(), commonPathList.get(i).getPathway(), commonPathList.get(i).getPathwayRank(), i+1, commonPathList.get(i).getEntry2ImpactColorMap(), commonPathList.get(i).getEntry2FrequencyColorMap()));
        }
        
        
    }
    
    

    public static int getColor(Entry entry, HashMap<String, Double> idToValueMap, HashMap<String, Double> idToValueMapBeforeCrosstalkHandling) {
        int color = 0;
        if (idToValueMap.containsKey(entry.getEntryId())) {
            double p = idToValueMap.get(entry.getEntryId());
            double pBeforeCrossTalkModification = idToValueMapBeforeCrosstalkHandling.get(entry.getEntryId());

            color = (int) Math.abs(Math.round(Math.log10(p)));
        }
        return color;
    }
    
    public static int getColor2(Entry entry, HashMap<String, Double> idToValueMap1, HashMap<String, Double> idToValueMap2) {
        int color = 0;
        
        if(idToValueMap1.containsKey(entry.getEntryId()))
            color=color+4;
        if(idToValueMap2.containsKey(entry.getEntryId()))
            color=color+4;
        
        return color;
    }

    //Find the pathway that comes after the last pathway to be displayed, e.g. 21st pathway if 20 pathways will be displayed.
    public static String findThresholdPathway(ArrayList<PathScorePair> paths) {
        HashSet<String> pathwaysBeforeThresholdSet = new HashSet<String>();
        int i = 0;
        String lastAddedPathway = "";

        while ((pathwaysBeforeThresholdSet.size() < Parameters.topPathwayNumberToBeWritten + 1) && i < (paths.size())) {
            if (pathwaysBeforeThresholdSet.add(paths.get(i).getPathwayTitle())) {
                lastAddedPathway = paths.get(i).getPathwayTitle();
            }
            i++;
        }
        String thresholdPathway = lastAddedPathway;
        return thresholdPathway;
    }

    public static void writeRSIdsOfGenesInTopPaths(String fileNamePostFix, FixedSizePriorityQueue<PathScorePair> pathPriority) throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Calendar cal = Calendar.getInstance();
        BufferedWriter bw = new BufferedWriter(new FileWriter(Parameters.outputFolder+File.separatorChar+dateFormat.format(cal.getTime()) + "_RSIds_" + fileNamePostFix));

        HashMap<String, ArrayList<String>> geneToRsIdsMap = readRsIdsFile();
        HashMap<String, HashSet<Entry>> pathwayToGenesMap = new HashMap<String, HashSet<Entry>>();
        ArrayList<String> pathwayNamesList = new ArrayList<String>();//This list is used to write pathways in order of score.

        PriorityQueue<PathScorePair> reversed = new PriorityQueue<PathScorePair>(pathPriority.size(), Collections.reverseOrder());
        reversed.addAll(pathPriority);

        boolean contAdding = true;
        while (!reversed.isEmpty() && contAdding) {

            PathScorePair psp = reversed.poll();
            String pathwayName = psp.getPathwayTitle();

            if (pathwayToGenesMap.containsKey(pathwayName) || pathwayToGenesMap.size() < Parameters.topPathwayNumberToBeWritten) {

                if (!pathwayToGenesMap.containsKey(pathwayName)) {
                    pathwayToGenesMap.put(pathwayName, new HashSet<Entry>());
                    pathwayNamesList.add(pathwayName);
                }

                HashSet<Entry> genesSet = pathwayToGenesMap.get(pathwayName);

                ArrayList<EntryRelationTypePair> path = psp.getPath();
                for (EntryRelationTypePair er : path) {
                    Entry entry1 = er.getEntry();
                    if (entry1.getType() == EntryType.GROUP || entry1.getType() == EntryType.MULTIGENE) {
                        for (Entry entry2 : entry1.getComponents()) {
                            if (entry2.getType() == EntryType.MULTIGENE) {
                                genesSet.addAll(entry2.getComponents());
                            } else {
                                genesSet.add(entry2);
                            }
                        }
                    } else {
                        genesSet.add(entry1);
                    }
                }
            } else {
                contAdding = false;
            }
        }

        for (String pathwayName : pathwayNamesList) {
            bw.write(pathwayName);
            bw.write("\t");
            for (Entry gene : pathwayToGenesMap.get(pathwayName)) {
                String geneSymbol = gene.getSymbol();
                if (geneToRsIdsMap.get(geneSymbol) != null) {
                    bw.write(geneSymbol);
                    bw.write("\t");
                    String rsIdsStr = "";
                    for (String rsId : geneToRsIdsMap.get(geneSymbol)) {
                        rsIdsStr = rsIdsStr + rsId + ",";
                    }
                    rsIdsStr = rsIdsStr.substring(0, rsIdsStr.length() - 1);//removes last comma
                    bw.write(rsIdsStr);
                    bw.write("\t");
                }
            }
            bw.newLine();
        }

        bw.close();
    }

    public static HashMap<String, ArrayList<String>> readRsIdsFile() throws IOException {
        HashMap<String, ArrayList<String>> geneToRsIdsMap = new HashMap<String, ArrayList<String>>();

        BufferedReader br = new BufferedReader(new FileReader(Parameters.allrsidsFile));
        String readLine;
        String strArr[];
        while ((readLine = br.readLine()) != null) {
            strArr = readLine.split("\\t");
            strArr[1] = strArr[1].substring(1, strArr[1].length() - 1);

            String strArr2[];
            strArr2 = strArr[1].split(", ");

            ArrayList<String> rsList = new ArrayList<String>();

            for (int i = 0; i < strArr2.length; i++) {
                rsList.add(strArr2[i]);
            }

            geneToRsIdsMap.put(strArr[0], rsList);
        }
        br.close();
        return geneToRsIdsMap;
    }

}
