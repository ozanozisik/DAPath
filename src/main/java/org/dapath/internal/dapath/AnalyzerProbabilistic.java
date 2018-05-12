package org.dapath.internal.dapath;

/*
 * Ozan Ozisik
 */
//TODO eHM'de hsa:.. id ile tutuluyor, ne gerek var? Adıyla tutulsun işte...
import org.dapath.internal.statistics.HyperGeometricTest;
import org.dapath.internal.pathway.EntryType;
import org.dapath.internal.pathway.RelationType;
import org.dapath.internal.pathway.Relation;
import org.dapath.internal.pathway.Pathway;
import org.dapath.internal.pathway.Entry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

//TODO: Groups may have multigene components, check if I'm handling these (and not assuming that all are single genes)
public class AnalyzerProbabilistic {

    Pathway pathway;
    HashMap<String, Double> idToValueMap;
    ArrayList<ArrayList<EntryRelationTypePair>> paths;

    Integer geneNumberInAllPathways = null;
    Integer affectedGeneNumberInAllPathways = null;

    /**
     * Takes results arrayList to be returned,
     *
     * @param pathwayy
     * @param idToValueMap
     * @throws ExcNoSuchEntryWithId
     * @throws IOException
     */
    public void analyze(Pathway pathwayy, HashSet<String> geneIdsInPathwaysSet, HashMap<String, Double> idToValueMap, FixedSizePriorityQueue<PathScorePair> pathPriority) throws IOException {

        this.pathway = pathwayy;
        this.idToValueMap = idToValueMap;

        System.out.println(pathway.getName() + " " + pathway.getTitle());

        if (geneNumberInAllPathways == null) {
            ArrayList<Integer> genomeGeneNumbersList = new ArrayList<Integer>();
            countNumberOfAffectedGenesByGeneIds(genomeGeneNumbersList, new ArrayList<String>(geneIdsInPathwaysSet));
            geneNumberInAllPathways = genomeGeneNumbersList.get(0);
            affectedGeneNumberInAllPathways = genomeGeneNumbersList.get(1);

//			System.out.println("geneNumberInAllPathways "+geneNumberInAllPathways+" affectedGeneNumberInAllPathways"+affectedGeneNumberInAllPathways);
        }

        ArrayList<Integer> pathwayGeneNumbersList = new ArrayList<Integer>();
        countNumberOfAffectedGenes(pathwayGeneNumbersList, pathway.getEntryList());
        int geneNumberInPathway = pathwayGeneNumbersList.get(0);
        int affectedGeneNumberInPathway = pathwayGeneNumbersList.get(1);
//		System.out.println(geneNumberInPathway+" "+affectedGeneNumberInPathway);

        paths = new ArrayList<ArrayList<EntryRelationTypePair>>();

        //In multigene and group, if a component has outgoing entry, it will not be used as outEntry.
        //In entry, if entry has a parent multigene or group and the parent has outgoing entry, it will not be used as outEntry.
        for (Entry entry : pathway.getEntryList()) {
            boolean isOutEntry = false;

            if (entry.getOutgoingEntryNumber() == 0) {

                if ((entry.getType() == EntryType.GROUP) || (entry.getType() == EntryType.MULTIGENE)) {
                    isOutEntry = true;
                    for (Entry subEntry : entry.getComponents()) {
                        if (subEntry.getOutgoingEntryNumber() != 0) {
                            isOutEntry = false;
                        }
                    }
                } else if ((entry.getParent() == null) || (entry.getParent().getOutgoingEntryNumber() == 0)) {
                    isOutEntry = true;
                }

                if (isOutEntry) {

                    ArrayList<EntryRelationTypePair> path = new ArrayList<EntryRelationTypePair>();
                    path.add(new EntryRelationTypePair(entry, RelationType.OUTNODE));
                    dft(entry, path);
                }
            }
        }

        System.out.println("Number of paths: " + paths.size());

        //Prints gene id, name and p-value
//		if(pathway.getName().equals("path:hsa04612")){
//			for(Entry entry:pathway.getEntryList()){ 
//				if(idToValueMap.get(entry.getEntryId())!=null){
//					System.out.print(entry.getEntryId()+"_"+entry.getSymbol()+" "+idToValueMap.get(entry.getEntryId())+"\n");
//				}
//			}
//		}
        //Maps input output pairs to paths leading to them.
        HashMap<InputOutputPair, ArrayList<ArrayList<EntryRelationTypePair>>> inputOutput2paths = new HashMap<InputOutputPair, ArrayList<ArrayList<EntryRelationTypePair>>>();
        //Maps input output pairs to the list of EntryRelationTypePairs on the paths leading to them. 
        HashMap<InputOutputPair, HashSet<EntryRelationTypePair>> inputOutput2ERsOnPaths = new HashMap<InputOutputPair, HashSet<EntryRelationTypePair>>();
        //Fills the hashmaps declared above.
        for (ArrayList<EntryRelationTypePair> path : paths) {
            int geneEtcNumber=0;
            for(EntryRelationTypePair er:path){
                if((er.getEntry().getType()==EntryType.GENE)||(er.getEntry().getType()==EntryType.MULTIGENE)||(er.getEntry().getType()==EntryType.GROUP)){
                    geneEtcNumber++;
                }
            }
            if(geneEtcNumber>2){
//            if (path.size() > 2) {//Paths consisting of 1 or 2 nodes are discarded.
                Entry outEntry = path.get(0).getEntry();
                Entry inEntry = path.get(path.size() - 1).getEntry();
                InputOutputPair inputOutputPair = new InputOutputPair(inEntry, outEntry);

                ArrayList<ArrayList<EntryRelationTypePair>> pathsOfInOutEntry = inputOutput2paths.get(inputOutputPair);
                HashSet<EntryRelationTypePair> ersOnPaths = inputOutput2ERsOnPaths.get(inputOutputPair);
                if (pathsOfInOutEntry == null) {
                    pathsOfInOutEntry = new ArrayList<ArrayList<EntryRelationTypePair>>();
                    inputOutput2paths.put(inputOutputPair, pathsOfInOutEntry);
                    ersOnPaths = new HashSet<EntryRelationTypePair>();
                    inputOutput2ERsOnPaths.put(inputOutputPair, ersOnPaths);
                }
                pathsOfInOutEntry.add(path);
                for (EntryRelationTypePair er : path) {
                    ersOnPaths.add(er);
                }
            }
        }

        int maxPathNumber = 0;
        for (InputOutputPair inputOutputPair : inputOutput2paths.keySet()) {
            if (inputOutput2paths.get(inputOutputPair).size() > maxPathNumber) {
                maxPathNumber = inputOutput2paths.get(inputOutputPair).size();
            }
        }

        //For pathway score calculation. Pathways with too many paths are discarded because of high computation needs.
//		if(maxPathNumber>Parameters.maxPathNumberAllowed){
//			System.out.println(pathway.getName()+" "+pathway.getTitle()+" too many paths: "+maxPathNumber);
//			return;
//		}
        for (InputOutputPair inputOutputPair : inputOutput2paths.keySet()) {

            //Sum of path probabilities are calculated.
            //Nodes in each path are multiplied.
            for (ArrayList<EntryRelationTypePair> path : inputOutput2paths.get(inputOutputPair)) {

                ArrayList<Entry> pathEntryList = new ArrayList<Entry>();
                for (EntryRelationTypePair er : path) {
                    pathEntryList.add(er.getEntry());
                }
                ArrayList<Integer> geneNumbers = new ArrayList<Integer>();
                countNumberOfAffectedGenes(geneNumbers, pathEntryList);
                int geneNumberInPath = geneNumbers.get(0);
                int affectedGeneNumberInPath = geneNumbers.get(1);

                

                //TODO:The line below is commented because it takes a little bit more time and we are not using it right now.
//                HyperGeometricTest hgt = new HyperGeometricTest();
//		  double pathEnrichmentP=hgt.calculateHypergDistr(affectedGeneNumberInPath, affectedGeneNumberInAllPathways, geneNumberInPath, geneNumberInAllPathways);
//                if (pathEnrichmentP > 1) {
//                    pathEnrichmentP = 1;
 //               }
                double pathEnrichmentP = 0;
                

                //System.out.println(affectedGeneNumberInPath+" "+affectedGeneNumberInAllPathways+" "+geneNumberInPath+" "+geneNumberInAllPathways+" "+pathEnrichmentP);
//				if(pathEnrichmentP<0.0009){
//					System.out.println(path.toString());
//					System.out.println(affectedGeneNumberInPath+" "+affectedGeneNumberInAllPathways+" "+geneNumberInPath+" "+geneNumberInAllPathways);
//					System.out.println(pathEnrichmentP);
//				}
                double basePathProbability = 1.0;
                double pathProbability = 1.0;

                HashSet<Entry> addedEntries = new HashSet<Entry>();//This is used to prevent double addition of a gene (G1) which
                //expresses a gene (G2) and also has binding/association relation with another node (G3). 
                //Binding/association is two way. If we do not check this hashset, paths like this G2 <--G1 --G3 --G1 can occur
                //which results the multiplication of G1's score twice.

                //TODO:State change olmasa da pespese ayni gen geliyorsa dahil edilmemesi lazim.
                for (EntryRelationTypePair er : path) {
                    if (er.getRelationType() != RelationType.STATE_CHANGE && !addedEntries.contains(er.getEntry())) {
                        //TODO:Same gene but different entry is counted again, as it affects the path twice, it may be correct.	

                        if (er.getEntry().getType() != EntryType.COMPOUND) {//TODO:Other types also should be excluded although they are not usually in the paths
//							pathProbability=pathProbability*getProbability(er.getEntry(),er.getRelationType());
//							basePathProbability=basePathProbability*(1-Parameters.baseNodeProb);

                            ArrayList<Double> probAL = getProbabilityArray(er.getEntry(), er.getRelationType());
                            //First is base probability, second is impact probability
                            basePathProbability = basePathProbability * probAL.get(0);
                            pathProbability = pathProbability * probAL.get(1);

                        }
                        addedEntries.add(er.getEntry());
                    }
                }

                if (pathProbability == 1) {
                    pathProbability = 0.999999999999999;
                }

                //TODO: Caution, here significance is directly used as probability, instead of 1-significance
//				double pathLogChange=Math.log(basePathProbability)/Math.log(pathProbability);
                double pathLogChange = Math.log(pathProbability) / Math.log(basePathProbability);

//				double propTest=proportionTest(basePathProbability, pathProbability);
                //BehcetJP'de Platelet'te proportionTest methodu tak�l�yor.
                int numOfNonCompoundsInPath = path.size();
                for (EntryRelationTypePair er : path) {
                    if (er.getEntry().getType() == EntryType.COMPOUND) {
                        numOfNonCompoundsInPath--;
                    }
                }

                if (numOfNonCompoundsInPath > 1 && pathLogChange > Parameters.pathScoreThreshold) {
//				if(path.size()>2 && pathLogChange>Parameters.pathScoreThreshold){
//					PathScorePair psp=new PathScorePair(path, propTest, pathway.getTitle()));
                    PathScorePair psp = new PathScorePair(path, pathLogChange, pathway);
                    psp.setBaseProb(basePathProbability);
                    psp.setPathProb(pathProbability);
                    psp.setPathEnrichmentP(pathEnrichmentP);
                    pathPriority.add(psp);
                }

            }

        }//endOf for(InputOutputPair inputOutputPair:inputOutput2paths.keySet()){

    }

    //////////////////////////////////////////
    public ArrayList<Double> getProbabilityArray(Entry entry, RelationType relType) {
        ArrayList<Double> probAL;//First is base probability, second is impact probability

        if ((entry.getType() == EntryType.MULTIGENE) || (entry.getType() == EntryType.GROUP)) {

            int handlingParameter = -1;
            if (entry.getType() == EntryType.MULTIGENE) {
                handlingParameter = Parameters.multigeneHandlingMethod;
            } else if (entry.getType() == EntryType.GROUP) {
                handlingParameter = Parameters.groupHandlingMethod;
            }

            if (handlingParameter == 0) {
                probAL = multiplyProbabilitiesOfSubEntriesArray(entry, relType);
            } else if (handlingParameter == 3) {
                //TODO: Caution, here significance is directly used as probability, instead of 1-significance				
//				probAL=maxOfProbabilitiesOfSubEntriesArray(entry, relType);
                probAL = minOfProbabilitiesOfSubEntriesArray(entry, relType);
//			}else if(handlingParameter==4){
//				probAL=atLeastOneActiveInSubEntriesArray(entry, relType); 
            } else {
                System.out.println("Unknown value for multigene/group handling Method.");
                probAL = new ArrayList<Double>();
                probAL.add(1.0);
                probAL.add(1.0);
            }

        } else {
            probAL = getProbability2Array(entry, relType);

        }
        return probAL;
    }

    public ArrayList<Double> getProbability2Array(Entry entry, RelationType relType) {
        ArrayList<Double> probAL = new ArrayList<Double>();//First is base probability, second is impact probability

        //TODO: Caution, here significance is directly used as probability, instead of 1-significance
//		probAL.add(1-Parameters.baseNodeProb);
//		probAL.add(1-(idToValueMap.get(entry.getEntryId())!=null ? idToValueMap.get(entry.getEntryId()) : Parameters.baseNodeProb ));
        probAL.add(Parameters.baseNodeProb);
        probAL.add((idToValueMap.get(entry.getEntryId()) != null ? idToValueMap.get(entry.getEntryId()) : Parameters.baseNodeProb));
        return probAL;
    }

    private ArrayList<Double> multiplyProbabilitiesOfSubEntriesArray(Entry entry, RelationType relType) {
        ArrayList<Double> probAL = new ArrayList<Double>();//First is base probability, second is impact probability
        probAL.add(1.0);
        probAL.add(1.0);
        for (Entry subEntry : entry.getComponents()) {
            ArrayList<Double> probALtmp = getProbabilityArray(subEntry, relType);
            probAL.set(0, probAL.get(0) * probALtmp.get(0));
            probAL.set(1, probAL.get(1) * probALtmp.get(1));
        }
        return probAL;
    }

    private ArrayList<Double> maxOfProbabilitiesOfSubEntriesArray(Entry entry, RelationType relType) {
        ArrayList<Double> probAL = new ArrayList<Double>();//First is base probability, second is impact probability
        double maxProb = -1;
        for (Entry subEntry : entry.getComponents()) {
            ArrayList<Double> probALtmp;
            probALtmp = getProbabilityArray(subEntry, relType);
            if (probALtmp.get(1) > maxProb) {
                maxProb = probALtmp.get(1);
            }
        }
        probAL.add(1 - Parameters.baseNodeProb);
        probAL.add(maxProb);
        return probAL;
    }
    //TODO: Caution, here significance is directly used as probability, instead of 1-significance

    private ArrayList<Double> minOfProbabilitiesOfSubEntriesArray(Entry entry, RelationType relType) {
        ArrayList<Double> probAL = new ArrayList<Double>();//First is base probability, second is impact probability
        double minProb = 1;
        for (Entry subEntry : entry.getComponents()) {
            ArrayList<Double> probALtmp;
            probALtmp = getProbabilityArray(subEntry, relType);
            if (probALtmp.get(1) < minProb) {
                minProb = probALtmp.get(1);
            }
        }
        probAL.add(Parameters.baseNodeProb);
        probAL.add(minProb);
        return probAL;
    }

//	private ArrayList<Double> atLeastOneActiveInSubEntriesArray(Entry entry, RelationType relType){
//		ArrayList<Double> probAL=new ArrayList<Double>();
//		double prob=1;
//		for(Entry subEntry:entry.getComponents()){
//			ArrayList<Double> probALtmp=getProbabilityArray(subEntry, relType);
//			prob=prob*(1-probALtmp.get(1));
//		}
//		probAL.add(1-Parameters.baseNodeProb);
//		probAL.add(1-prob);
//		return probAL;
//	}
    //////////////////////////////////////////
    /*
	public double getProbability(Entry entry, RelationType relType){
		if((entry.getType()==EntryType.MULTIGENE)||(entry.getType()==EntryType.GROUP)){
			
			int handlingParameter=-1;
			if(entry.getType()==EntryType.MULTIGENE){
				handlingParameter=Parameters.multigeneHandlingMethod;
			}else if(entry.getType()==EntryType.GROUP){
				handlingParameter=Parameters.groupHandlingMethod;
			}
			
			if(handlingParameter==0){
				return multiplyProbabilitiesOfSubEntries(entry, relType);
			}else if(handlingParameter==1){
				return addProbabilitiesOfSubEntries(entry, relType);
			}else if(handlingParameter==2){
				return meanOfProbabilitiesOfSubEntries(entry, relType);
			}else if(handlingParameter==3){
				return maxOfProbabilitiesOfSubEntries(entry, relType);
			}else if(handlingParameter==4){
				return atLeastOneActiveInSubEntries(entry, relType); 
			}else{
				System.out.println("Unknown value for multigene/group handling Method.");
				return -1;
			}
			
		}else{
			return getProbability2(entry, relType);
		}
	}
	
	private double multiplyProbabilitiesOfSubEntries(Entry entry, RelationType relType){
		double prob=1;
		for(Entry subEntry:entry.getComponents()){
			prob*=getProbability(subEntry, relType);
		}
		return prob;
	}
	private double addProbabilitiesOfSubEntries(Entry entry, RelationType relType){
		double prob=0;
		for(Entry subEntry:entry.getComponents()){
			prob+=getProbability(subEntry, relType);
		}
		return prob;
	}
	private double meanOfProbabilitiesOfSubEntries(Entry entry, RelationType relType){
		double prob=0;
		for(Entry subEntry:entry.getComponents()){
			prob+=getProbability(subEntry, relType);
		}
		return prob/entry.getComponents().size();
	}
	private double maxOfProbabilitiesOfSubEntries(Entry entry, RelationType relType){
		double prob=-1;
		for(Entry subEntry:entry.getComponents()){
			double p=getProbability(subEntry, relType);
			if(p>prob){
				prob=p;
			}
		}
		return prob;
	}
	private double atLeastOneActiveInSubEntries(Entry entry, RelationType relType){
		double prob=1;
		for(Entry subEntry:entry.getComponents()){
			prob=prob*(1-getProbability(subEntry, relType));
		}
		return 1-prob;
	}
	
	
	
	public double getProbability2(Entry entry, RelationType relType){
		double prob=1-(idToValueMap.get(entry.getEntryId())!=null ? idToValueMap.get(entry.getEntryId()) : Parameters.baseNodeProb );
		return prob;
	}	
     */
    /**
     * Fills the global paths object.
     *
     * @param seed
     * @param path
     */
    public void dft(Entry seed, ArrayList<EntryRelationTypePair> path) {
        if (seed.getIncomingEntryNumber() == 0) {
//			System.out.println(path.get(0).getEntry().getNameGr()+" "+path.get(0).getEntry().getEntryId());
//			System.out.println(seed.getId()+" "+seed.getEntryId()+" "+seed.getNameGr());
            paths.add(path);

        } else if (path.size() < this.pathway.getNumberOfEntries()) {
            for (Relation relation : seed.getRelationsIncoming()) {
                Entry neigh = relation.getEntry();
                if (!path.contains(new EntryRelationTypePair(neigh, relation.getType()))) {
//						for(EntryRelationTypePair er:path){
//							System.out.print(er.getEntry().getEntryId()+" ");
//						}
//						System.out.println();
                    ArrayList<EntryRelationTypePair> pathNew = new ArrayList<EntryRelationTypePair>(path);
                    pathNew.add(new EntryRelationTypePair(neigh, relation.getType()));
                    dft(neigh, pathNew);
                }
            }
        }
    }

    /**
     * Counts the number of genes and number of affected genes in the pathway.
     *
     * @param affectedGeneNumber
     * @param geneNumber
     */
    public void countNumberOfAffectedGenes(ArrayList<Integer> geneNumbers, ArrayList<Entry> entryList) {
        LinkedList<Entry> entryLinkedList = new LinkedList<Entry>();
        HashSet<String> geneIds = new HashSet<String>();
        entryLinkedList.addAll(entryList);

        while (!entryLinkedList.isEmpty()) {
            Entry entry = entryLinkedList.poll();
            if (entry.getType() == EntryType.GENE) {
                geneIds.add(entry.getEntryId());
            } else if (entry.getType() == EntryType.GROUP || entry.getType() == EntryType.MULTIGENE) {
                entryLinkedList.addAll(entry.getComponents());
            }
        }

        int geneNumber = geneIds.size();
        int affectedGeneNumber = 0;

        for (String id : geneIds) {
            if (idToValueMap.containsKey(id)) {
                affectedGeneNumber++;
            }
        }
        geneNumbers.add(geneNumber);
        geneNumbers.add(affectedGeneNumber);
    }

    /**
     * Counts the number of genes and number of affected genes in the pathway.
     * Input is geneIds. In this method all ids are assumed to be gene ids, no
     * further checking is performed.
     *
     * @param affectedGeneNumber
     * @param geneNumber
     */
    public void countNumberOfAffectedGenesByGeneIds(ArrayList<Integer> geneNumbers, ArrayList<String> entryIdList) {
        int geneNumber = entryIdList.size();
        int affectedGeneNumber = 0;
        for (String id : entryIdList) {
            if (idToValueMap.containsKey(id)) {
                affectedGeneNumber++;
            }
        }
        geneNumbers.add(geneNumber);
        geneNumbers.add(affectedGeneNumber);
    }
}
