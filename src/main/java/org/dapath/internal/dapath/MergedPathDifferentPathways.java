package org.dapath.internal.dapath;

import java.util.ArrayList;
import org.dapath.internal.pathway.Pathway;

import java.util.HashMap;
import java.util.HashSet;
import org.dapath.internal.pathway.Entry;

public class MergedPathDifferentPathways{

    private final HashSet<Entry> entriesOfCommonPaths;
    private final Double scoreOfFrontierPath;
    private final ArrayList<Pathway> pathwayList;
    
    private final int pathwayRank;
    private final int pathRank;
    private final HashMap<Entry, Integer> entry2ImpactColorMap;
    private final HashMap<Entry, Integer> entry2FrequencyColorMap;

    public MergedPathDifferentPathways(HashSet<Entry> entriesOfCommonPaths, Double scoreOfFrontierPath, ArrayList<Pathway> pathwayList, int pathwayRank, int pathRank, HashMap<Entry, Integer> entry2ImpactColorMap, HashMap<Entry, Integer> entry2FrequencyColorMap) {
        super();
        this.entriesOfCommonPaths=entriesOfCommonPaths;
        this.scoreOfFrontierPath=scoreOfFrontierPath;
        this.pathwayList=pathwayList;
        this.pathwayRank=pathwayRank;
        this.pathRank=pathRank;
        this.entry2ImpactColorMap=entry2ImpactColorMap;
        this.entry2FrequencyColorMap=entry2FrequencyColorMap;
    }

    public HashSet<Entry> getEntriesOfCommonPaths() {
        return entriesOfCommonPaths;
    }

    public Double getScoreOfFrontierPath() {
        return scoreOfFrontierPath;
    }

    public ArrayList<Pathway> getPathwayList() {
        return pathwayList;
    }

    public int getPathwayRank() {
        return pathwayRank;
    }

    public int getPathRank() {
        return pathRank;
    }
    
    public HashMap<Entry, Integer> getEntry2ImpactColorMap() {
        return entry2ImpactColorMap;
    }

    public HashMap<Entry, Integer> getEntry2FrequencyColorMap() {
        return entry2FrequencyColorMap;
    }
    
    @Override
    public String toString(){
        String str="";
        for(Pathway pathway:pathwayList){
            str=str+pathway.getTitle()+",";
        }
        str=str+"\t"+pathwayRank+"\t"+scoreOfFrontierPath+"\t"+entriesOfCommonPaths;
        return str;
    }

}
