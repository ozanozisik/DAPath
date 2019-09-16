package org.dapath.internal.keggoperations;

/*
 * Ozan Ozisik
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class KEGGDownloader {

    static ArrayList<String> pathwayIds = new ArrayList<String>();
    static ArrayList<String> pathwayNames = new ArrayList<String>();

    /**
     * Downloads pathways, gene list and gene sets of pathways
     *
     * @throws IOException
     */
    public static void downloadAll(String keggFolderPath) throws IOException {
        downloadGeneSets(keggFolderPath);
        downloadPathways(keggFolderPath);
    }

    /**
     * Creates two file pairs consisting of genes in each pathway, one pair
     * using gene ids, one pair using gene symbols. In the file pairs one file
     * has all the genes in new lines without duplicates. Other file keeps genes
     * of each pathway in different lines. This method also downloads
     * homosapiens gene list for id symbol mapping.
     *
     * @throws IOException
     */
    public static void downloadGeneSets(String keggFolderPath) throws IOException {

        createKeggFolderIfMissing(keggFolderPath);

        downloadGeneList(keggFolderPath);

        if (pathwayIds.isEmpty()) {
            getPathwayIdsAndSymbols(pathwayIds, pathwayNames);
        }

        HashMap<String, String> geneIdToSymbolMap = new HashMap<String, String>();
        HashMap<String, String> geneSymbolToIdMap = new HashMap<String, String>();

        GeneListParser.parseGeneList(keggFolderPath + File.separator + "hsaGenes.txt", geneIdToSymbolMap, geneSymbolToIdMap);

        HashSet<String> geneSetWIds = new HashSet<String>();
        HashSet<String> geneSetWSymbols = new HashSet<String>();

        BufferedWriter bufWriterWIds = new BufferedWriter(new FileWriter(keggFolderPath + File.separatorChar + "hsaGeneIdsInPathways.txt"));
        BufferedWriter bufWriterGroupedWIds = new BufferedWriter(new FileWriter(keggFolderPath + File.separatorChar + "hsaGeneIdsInEachPathwayGrouped.txt"));

        BufferedWriter bufWriterWSymbols = new BufferedWriter(new FileWriter(keggFolderPath + File.separatorChar + "hsaGeneSymbolsInPathways.txt"));
        BufferedWriter bufWriterGroupedWSymbols = new BufferedWriter(new FileWriter(keggFolderPath + File.separatorChar + "hsaGeneSymbolsInEachPathwayGrouped.txt"));

        //Gets genes in each pathway and saves it.
        for (int i = 0; i < pathwayIds.size(); i++) {

//			URL urlPath = new URL("http://rest.kegg.jp/link/genes/"+pathwayIds.get(i));
            URL urlPath = new URL("http://rest.kegg.jp/link/hsa/" + pathwayIds.get(i));
            HttpURLConnection connPath = (HttpURLConnection) urlPath.openConnection();

            if (connPath.getResponseCode() != 200) {
                bufWriterWIds.close();
                bufWriterGroupedWIds.close();
                bufWriterWSymbols.close();
                bufWriterGroupedWSymbols.close();
                throw new IOException(connPath.getResponseMessage());
            }
            InputStream streamPath = connPath.getInputStream();

            BufferedReader bufReader = new BufferedReader(new InputStreamReader(streamPath));

            String line;
            while ((line = bufReader.readLine()) != null) {
                String strArr[] = line.split("[\\t]");
                if (strArr.length > 1) {
                    String geneId = strArr[1];
//                    System.out.println(geneId);
                    String geneSymbol = geneIdToSymbolMap.get(geneId);
//                    System.out.println(geneSymbol);
                    if (geneSymbol != null) {
                        geneSetWIds.add(geneId);
                        geneSetWSymbols.add(geneSymbol);
                        bufWriterGroupedWIds.write(geneId + "\t");
                        bufWriterGroupedWSymbols.write(geneSymbol + "\t");
                    } else {
                        //System.out.println("Symbol null for "+geneId+", did not add id or symbol");
                    }
                }
            }
            streamPath.close();
            bufReader.close();

            bufWriterGroupedWIds.newLine();
            bufWriterGroupedWSymbols.newLine();

        }

        for (String str : geneSetWIds) {
            bufWriterWIds.write(str);
            bufWriterWIds.newLine();
        }
        for (String str : geneSetWSymbols) {
            bufWriterWSymbols.write(str);
            bufWriterWSymbols.newLine();
        }

        bufWriterWIds.close();
        bufWriterGroupedWIds.close();
        bufWriterWSymbols.close();
        bufWriterGroupedWSymbols.close();

    }

    /**
     * Saves each pathway's xml
     *
     * @throws IOException
     */
    public static void downloadPathways(String keggFolderPath) throws IOException {

        createKeggFolderIfMissing(keggFolderPath);

        if (pathwayIds.isEmpty()) {
            getPathwayIdsAndSymbols(pathwayIds, pathwayNames);
        }

        BufferedWriter bw = new BufferedWriter(new FileWriter(keggFolderPath + File.separatorChar + "pathwayNames.txt"));
        for (String str : pathwayNames) {
            bw.write(str);
            bw.newLine();
        }
        bw.close();

        for (int i = 0; i < pathwayIds.size(); i++) {

            URL urlPath = new URL("http://rest.kegg.jp/get/" + pathwayIds.get(i) + "/kgml");
            HttpURLConnection connPath = (HttpURLConnection) urlPath.openConnection();

            if (connPath.getResponseCode() != 200) {
                throw new IOException(connPath.getResponseMessage());
            }
            InputStream streamPath = connPath.getInputStream();

            BufferedReader bufReaderPath = new BufferedReader(new InputStreamReader(streamPath));
            BufferedWriter bufWriterPath = new BufferedWriter(new FileWriter(keggFolderPath + File.separatorChar + pathwayIds.get(i) + ".xml"));

            int ch;
            while ((ch = bufReaderPath.read()) != -1) {
                bufWriterPath.write(ch);
            }

            streamPath.close();
            bufReaderPath.close();
            bufWriterPath.close();

        }

    }

    /**
     * Creates a file consisting of the list of homosapiens genes with ids (e.g.
     * hsa:222029), definitions and symbols.
     *
     * @throws IOException
     */
    public static void downloadGeneList(String keggFolderPath) throws IOException {

//        createKeggFolderIfMissing(keggFolderPath);

        URL url = new URL("http://rest.kegg.jp/list/hsa");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        if (conn.getResponseCode() != 200) {
            throw new IOException(conn.getResponseMessage());
        }
        InputStream stream = conn.getInputStream();

        BufferedReader bufReader = new BufferedReader(new InputStreamReader(stream));
        BufferedWriter bufWriter = new BufferedWriter(new FileWriter(keggFolderPath + File.separatorChar + "hsaGenes.txt"));
        String line;
        while ((line = bufReader.readLine()) != null) {
            bufWriter.write(line);
            bufWriter.newLine();
        }

        stream.close();
        bufReader.close();
        bufWriter.close();

    }

    public static void createKeggFolderIfMissing(String keggFolderPath) {
        File keggFolderDir = new File(keggFolderPath);
        
        System.out.println(keggFolderDir.getAbsolutePath());
        
        
        // if the directory does not exist, create it
        if (!keggFolderDir.exists()) {
            try {
                keggFolderDir.mkdir();
            } catch (SecurityException se) {
                System.out.println("Security settings do not allow creating keggPathways directory.");
                throw new SecurityException(se.getMessage());
            }
        }
    }

    /**
     * Gets the list of homosapiens pathways and fills pathwayIds and
     * pathwayNames arraylists given as parameters.
     *
     * @param pathwayIds
     * @param pathwayNames
     * @throws IOException
     */
    public static void getPathwayIdsAndSymbols(ArrayList<String> pathwayIds,
            ArrayList<String> pathwayNames) throws IOException {

        URL url = new URL("http://rest.kegg.jp/list/pathway/hsa");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        if (conn.getResponseCode() != 200) {
            throw new IOException(conn.getResponseMessage());
        }
        InputStream stream = conn.getInputStream();

        BufferedReader bufReader = new BufferedReader(new InputStreamReader(
                stream));
        String line;
        while ((line = bufReader.readLine()) != null) {
            String[] tmp = line.split("\t");
            pathwayIds.add(tmp[0].substring(5));// Excludes "path:" in id, e.g."path:hsa00010"
            pathwayNames.add(tmp[1].substring(0, tmp[1].lastIndexOf(" -")));
        }

        stream.close();
        bufReader.close();
    }

}
