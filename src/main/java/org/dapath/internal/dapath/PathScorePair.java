package org.dapath.internal.dapath;

import org.dapath.internal.pathway.Pathway;
import java.util.ArrayList;
import java.util.Collections;

public class PathScorePair implements Comparable<PathScorePair> {

    private ArrayList<EntryRelationTypePair> path;
    private Double score;
    private double baseProb;
    private double pathProb;
    private double pathEnrichmentP;
    private final Pathway pathway;

//	public PathScorePair(ArrayList<EntryRelationTypePair> path, Double score, String pathwayName) {
    public PathScorePair(ArrayList<EntryRelationTypePair> path, Double score, Pathway pathway) {
        super();
        this.path = path;
        this.score = score;
        this.pathway = pathway;
    }

    public ArrayList<EntryRelationTypePair> getPath() {
        return path;
    }

    public void setPath(ArrayList<EntryRelationTypePair> path) {
        this.path = path;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Pathway getPathway() {
        return pathway;
    }

    public String getPathwayTitle() {
        return pathway.getTitle();
    }

    public String getPathwayId() {
        return pathway.getOrg() + pathway.getNumber();
    }

    @Override
    public String toString() {
        ArrayList<EntryRelationTypePair> reversePath = new ArrayList<EntryRelationTypePair>(this.path);
        Collections.reverse(reversePath);
        String str = "";
        str += getPathwayId() + "\t" + getPathwayTitle() + "\t" + this.score + "\t" + this.pathEnrichmentP + "\t" + this.path.size() + "\t";

        for (int i = path.size() - 1; i >= 0; i--) {
            str += path.get(i);
        }
        return str;
    }

    //ascending, because in fixed size priority queue poll removes first element so element with small score should be removed.
    @Override
    public int compareTo(PathScorePair arg0) {
        return this.score.compareTo(arg0.score);
    }

    public double getPathProb() {
        return pathProb;
    }

    public void setPathProb(double pathProb) {
        this.pathProb = pathProb;
    }

    public double getBaseProb() {
        return baseProb;
    }

    public void setBaseProb(double baseProb) {
        this.baseProb = baseProb;
    }

    public double getPathEnrichmentP() {
        return pathEnrichmentP;
    }

    public void setPathEnrichmentP(double pathEnrichmentP) {
        this.pathEnrichmentP = pathEnrichmentP;
    }

}
