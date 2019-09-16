package org.dapath.internal.dapath;

public class Parameters {

    public static String propertiesFile="dapathParameters";
    
    public static String keggFolder = "";
    public static String[] expFilePath = {""};
    public static String outputFolder = "";
    public static boolean expFile_SkipFirstLine = true;//Skips the first line of the file (make this true if first line contains column names)
    public static int topPathwayNumberToBeWritten = 20;
    /**
     * 0: Do nothing. 
     * 1: Assign Parameters.baseNodeProb to genes that are part of more than crossTalkLimit pathways.
     * 2: Multiply p-values of genes by 10^(n-1).
     * n is the number of pathways the gene is involved. 
     * 3: Multiply p-values of genes by ...
     */
    public static int crossTalkHandlingMethod = 3;
    public static int crossTalkLimit = 3;
    
    
    public static boolean downloadKeggIfAbsent=false;
    
    
    public static boolean kgmlParserAssignRelationsToParent=true;
    
    public static String allrsidsFile = "./FilesInput/data/behcet_jp_gene_allRSids.csv";
    
    /**
     * 0: Multiply subentries. 1: Add subentries. 2: Mean of subentries 3: Get
     * the max / min (1-p used / p used) effect probability. 4: Get the
     * probability of activation of at least one.
     */
    public static int multigeneHandlingMethod = 3;//There are alternative gene products for the signal.

    /**
     * 0: Multiply subentries. 1: Add subentries. 2: Mean of subentries 3: Get
     * the max / min (1-p used / p used) effect probability.
     */
    public static int groupHandlingMethod = 3;//Gene products form a complex.

    public static int pathQueueSize = 30000;
    public static int distinctPathwaysInPath = 300;

    public static double baseNodeProb = 0.05;

//    public static double pathScoreThreshold=Math.log(baseNodeProb) * (baseNodeProb/10) * (baseNodeProb/10)/Math.log(baseNodeProb * baseNodeProb * baseNodeProb);
    public static double pathScoreThreshold = 0;
    
    public static int pathSearchDepthLimit=10;
}
