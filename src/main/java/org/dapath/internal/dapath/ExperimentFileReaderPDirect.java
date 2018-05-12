package org.dapath.internal.dapath;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author Ozan Basic experiment file. File consists of gene name and p-value
 * columns. There may be more than one p-value columns like SPOTPvalue and
 * FSScorePvalue.
 */
public class ExperimentFileReaderPDirect {

    //Genes with value 0 are discarded
    public static HashMap<String, Double> readExperimentFile(String path, HashMap<String, String> geneSymbolToIdMap, HashSet<String> genesInPathwaysSet) throws Exception {

        HashMap<String, Double> valuesHM;

        try {

            valuesHM = new HashMap<String, Double>();

            File file = new File(path);
            FileReader fReader = new FileReader(file);
            BufferedReader bufReader = new BufferedReader(fReader);

            boolean skipFirstLine = Parameters.expFile_SkipFirstLine;

            String line;
            String[] strArr;

            int recurring = 0;
            int notInKegg = 0;
            int notInAnyPathway = 0;

            int ttt = 0;

            int lineNo = 0;
            while ((line = bufReader.readLine()) != null) {

                lineNo++;

                if (skipFirstLine) {//If skipFirstLine==true, skips the line once.
                    //System.out.println("Skipped this line: "+line);
                    skipFirstLine = false;
                } else {
                    strArr = line.split("[, \\t]");

                    try {

                        String geneCode = geneSymbolToIdMap.get(strArr[0]);
                        if (geneCode == null) {
                            notInKegg++;
                        } else if (!genesInPathwaysSet.contains(geneCode)) {
                            notInAnyPathway++;
                        } else if (strArr[1].compareTo("NA") != 0) {

                            Double val = Double.parseDouble(strArr[1]);

                            if (val != 0) {
                                //In case the same genes have multiple p-values assigned in the file:
                                Double existingVal = valuesHM.get(geneCode);
                                if (existingVal == null) {
                                    valuesHM.put(geneCode, val);
                                    ttt++;
                                } else if (existingVal > val) {//TODO existingValue>value implementation specific, should be taken care of when copying code.
                                    valuesHM.put(geneCode, val);
                                    ttt++;
                                }
                            }

                        }

                    } catch (NumberFormatException ex) {
                        System.out.println("Warning: Value " + strArr[1] + " in line " + lineNo + " cannot be resolved to a double (in ExperimentFileReader)");
                    }
                }
            }

            System.out.println("Recurring:" + recurring + " notInKegg:" + notInKegg + " notInAnyPathway:" + notInAnyPathway);
            System.out.println("Total genes in experiment file:" + ttt);
            bufReader.close();

        } catch (FileNotFoundException ex) {
            throw (new Exception("FileNotFoundException in ExperimentFileReader " + ex.toString()));
        } catch (IOException ex) {
            throw (new Exception("IOException in ExperimentFileReader " + ex.toString()));
        } catch (NullPointerException ex) {
            throw (new Exception("NullPointerException in ExperimentFileReader " + ex.toString()));
        } catch (Exception ex) {
            throw (new Exception("Exception in ExperimentFileReader " + ex.toString()));
        }

        return valuesHM;

    }
}
