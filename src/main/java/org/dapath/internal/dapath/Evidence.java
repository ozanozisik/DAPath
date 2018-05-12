package org.dapath.internal.dapath;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class Evidence {

    private double pathwayProbability;
    private double basePathwayProbability;
    private double pathwayFoldChange;
    private double pathwayLogChange;
    private double pathwayProportionTest;
    private double avgInputOutputFoldChange;

    private LinkedHashMap<InputOutputPair, Double> inputOutput2LogChange;
    private LinkedHashMap<InputOutputPair, Double> inputOutput2FoldChange;
    private HashMap<InputOutputPair, String> inputOutput2MostAffectedPath;
    private String pathwayName;
    private double bestPathLogChange;
    private String bestLogChangePath;

    public Evidence(double pathwayFoldChange, double pathwayLogChange, double pathwayProportionTest, double pathwayProbability, double basePathwayProbability, double avgInputOutputFoldChange, double bestPathLogChange, String bestLogChangePath, LinkedHashMap<InputOutputPair, Double> inputOutput2LogChange, LinkedHashMap<InputOutputPair, Double> inputOutput2FoldChange, HashMap<InputOutputPair, String> inputOutput2MostAffectedPath, String pathwayName) {
        super();
        this.pathwayFoldChange = pathwayFoldChange;
        this.pathwayLogChange = pathwayLogChange;
        this.pathwayProportionTest = pathwayProportionTest;
        this.pathwayProbability = pathwayProbability;
        this.basePathwayProbability = basePathwayProbability;
        this.avgInputOutputFoldChange = avgInputOutputFoldChange;
        this.bestPathLogChange = bestPathLogChange;
        this.bestLogChangePath = bestLogChangePath;
        this.inputOutput2FoldChange = inputOutput2FoldChange;
        this.inputOutput2LogChange = inputOutput2LogChange;
        this.inputOutput2MostAffectedPath = inputOutput2MostAffectedPath;
        this.pathwayName = pathwayName;
    }

    public Double getPathwayProbability() {
        return pathwayProbability;
    }

    public String toString() {
        String str = pathwayName + "\t";
        str = str + pathwayFoldChange + "\t";
        str = str + pathwayLogChange + "\t";
        str = str + pathwayProportionTest + "\t";
        str = str + pathwayProbability + "\t";
        str = str + basePathwayProbability + "\t";
        str = str + avgInputOutputFoldChange + "\t";
        str = str + bestPathLogChange + "\t";
        str = str + bestLogChangePath + "\t";
//		for(String outputStr:output2FoldChange.keySet()){
//			str=str+outputStr+"\t"+output2FoldChange.get(outputStr)+"\t";
        for (InputOutputPair inputOutputPair : inputOutput2LogChange.keySet()) {
//		for(InputOutputPair inputOutputPair:inputOutput2FoldChange.keySet()){
            str = str + inputOutputPair.getOutput().getSymbol() + " " + inputOutputPair.getOutput().getEntryId() + " " + inputOutputPair.getOutput().getPathwaySpecificId() + " --- ";
            str = str + inputOutputPair.getInput().getSymbol() + " " + inputOutputPair.getInput().getEntryId() + " " + inputOutputPair.getInput().getPathwaySpecificId() + "\t";
//			str=str+inputOutput2MostAffectedPath.get(inputOutputPair)+"\t";
            str = str + inputOutput2LogChange.get(inputOutputPair) + "\t";
            str = str + inputOutput2FoldChange.get(inputOutputPair) + "\t";
        }

        return str;
    }

    public static String getHeaderLine() {
        return "Pathway\tPathwayFoldChange\tPathwayLogChange\tPathwayPropTest"
                + "\tPathwayProbability\tBasePathwayProbability"
                + "\tAvgInputOutputFoldChange\tBestPathLogChangeInAPath\tBestLogChangePath"
                + "\tBestPathIn";
    }

    public Double getPathwayFoldChange() {
        return pathwayFoldChange;
    }

    public void setPathwayFoldChange(Double pathwayFoldChange) {
        this.pathwayFoldChange = pathwayFoldChange;
    }

    //Descending
    static Comparator<Evidence> pathwayProbComparator = new Comparator<Evidence>() {
        public int compare(Evidence e1, Evidence e2) {
            double v1 = e1.getPathwayProbability();
            double v2 = e2.getPathwayProbability();

            if (Double.isNaN(v1)) {
                return 1;
            }

            if (Double.isNaN(v2)) {
                return -1;
            }

            if (v1 > v2) {
                return -1;
            } else if (v1 < v2) {
                return 1;
            } else {
                return 0;
            }
        }
    };
}
