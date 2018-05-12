package org.dapath.internal.pathway;

/*
 * Ozan Ozisik
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

//TODO:Pathway name is called name in xml but it is actually kegg id of pathway, it might be changed.
public class Pathway {

    private String name;
    private String org;
    private String number;
    private String title;
    private String image;
    private String link;

    private HashMap<Integer, Entry> entryHash;
    private ArrayList<Entry> entryList;
    
    private static HashMap<String, Pathway> pathwayTitleToPathwayMap=new HashMap<String, Pathway>();

    private boolean includesUsableRelation;

    public boolean getIncludesUsableRelation() {
        return includesUsableRelation;
    }

    public void setIncludesUsableRelation(boolean bool) {
        includesUsableRelation = bool;
    }

    public Pathway(String name, String org, String number, String title, String image, String link) {
        super();
        this.name = name;
        this.org = org;
        this.number = number;
        this.title = title;
        this.image = image;
        this.link = link;
        
        pathwayTitleToPathwayMap.put(title, this);

        entryHash = new HashMap<Integer, Entry>();
        entryList = new ArrayList<Entry>();
    }

    public void addEntry(Entry entry) {
        entryHash.put(entry.getPathwaySpecificId(), entry);
        entryList.add(entry);
    }

//	@SuppressWarnings("unchecked")
//	public HashMap<Integer, Entry> getEntryHashClone() {
//		return (HashMap<Integer, Entry>) entryHash.clone();
//	}
    @SuppressWarnings("unchecked")
    public ArrayList<Entry> getEntryListClone() {
        return (ArrayList<Entry>) entryList.clone();
    }

//	public HashMap<Integer, Entry> getEntryHash() {
//		return entryHash;
//	}
    public ArrayList<Entry> getEntryList() {
        return entryList;
    }

    /**
     * @param pathwaySpecificId
     * @return entry Entries are stored in an HashMap. This method gets Entry
     * using its id.
     * @throws Exception
     */
    public Entry getEntryWithPathwaySpecificId(int pathwaySpecificId) throws Exception {
        Entry entry = entryHash.get(pathwaySpecificId);
        if (entry == null) {
            System.out.println("pathwaySpecificId=" + pathwaySpecificId);
            throw new Exception("No such entry with pathwaySpecificId " + pathwaySpecificId);
        }
        return entry;
    }

    public String getName() {
        return name;
    }

    public String getOrg() {
        return org;
    }

    public String getNumber() {
        return number;
    }

    public String getTitle() {
        return title;
    }

    public String getImage() {
        return image;
    }

    public String getLink() {
        return link;
    }

    public int getNumberOfEntries() {
        return entryList.size();
    }
    
    public static Pathway getPathwayWithTitle(String title){
        return pathwayTitleToPathwayMap.get(title);
    }

    public String pathwayToString() {
        String str = "";
        try {

            //Another way for iterating over hashmap
            //Map<Integer, Entry> entryHash = new HashMap<Integer, Entry>();
            //for (Map.Entry<Integer, Entry> entry : entryHash.entrySet()) {
            for (Object entryId : entryHash.keySet()) {

                Entry entry = entryHash.get(entryId);
                str = str + entry.getPathwaySpecificId() + " ";

                if (entry.getType() == EntryType.GROUP) {
                    for (Entry comp : entry.getComponents()) {
                        str = str + comp.getPathwaySpecificId() + " " + comp.getSymbol() + " ";
                    }
                } else {
                    str = str + entry.getSymbol() + "\n";

                    str = str + "RelationsIncoming:\n";
                    for (Relation relation : entry.getRelationsIncoming()) {
                        Entry relatedEntry = relation.getEntry();
                        str = str + relatedEntry.getPathwaySpecificId() + " " + relatedEntry.getSymbol() + " " + relation.getSymbol();
                    }

                    str = str + "\nRelationsOutgoing:\n";
                    for (Relation relation : entry.getRelationsOutgoing()) {
                        Entry relatedEntry = relation.getEntry();
                        str = str + relatedEntry.getPathwaySpecificId() + " " + relatedEntry.getSymbol() + " " + relation.getSymbol();
                    }

                    str = str + "\nRelationsUndirected:\n";
                    for (Relation relation : entry.getRelationsUndirected()) {
                        Entry relatedEntry = relation.getEntry();
                        str = str + relatedEntry.getPathwaySpecificId() + " " + relatedEntry.getSymbol() + " " + relation.getSymbol();
                    }
                }
                str = str + "\n";
            }

        } catch (Exception ex) {
            System.out.println(str);
        }

        return str;

    }

    public int removeDuplicateEntries() {

        ArrayList<Entry> addedDuplicates = new ArrayList<Entry>();
        HashMap<Entry, Entry> duplicatesDup2OrgHM = new HashMap<Entry, Entry>();

        for (int i = 0; i < entryList.size() - 1; i++) {

            Entry originalEntry = entryList.get(i);

            if (!addedDuplicates.contains(originalEntry)) {
                for (int j = i + 1; j < entryList.size(); j++) {
                    if (originalEntry.getEntryId().compareTo(entryList.get(j).getEntryId()) == 0) {

                        if (originalEntry.getType() == entryList.get(j).getType()) {
                            Entry duplicateEntry = entryList.get(j);
                            addedDuplicates.add(duplicateEntry);
                            duplicatesDup2OrgHM.put(duplicateEntry, originalEntry);
                        }

                    }

                }
                //duplicatesDup2OrgHM.put(entryList.get(i), duplicates);
            }
        }

        for (Entry entry : entryList) {

            ArrayList<Relation> relationsIncoming = entry.getRelationsIncoming();
            for (Relation relation : relationsIncoming) {
                Entry dup = relation.getEntry();
                Entry org = duplicatesDup2OrgHM.get(dup);
                if (org != null) {
                    relation.updateEntry(org);
                    entry.removeRelatedEntry(dup, relation.getDirection());
                    entry.addRelatedEntry(org, relation.getDirection());
                }
            }

            ArrayList<Relation> relationsOutgoing = entry.getRelationsOutgoing();
            for (Relation relation : relationsOutgoing) {
                Entry dup = relation.getEntry();
                Entry org = duplicatesDup2OrgHM.get(dup);
                if (org != null) {
                    relation.updateEntry(org);
                    entry.removeRelatedEntry(dup, relation.getDirection());
                    entry.addRelatedEntry(org, relation.getDirection());
                }
            }

            ArrayList<Relation> relationsUndirected = entry.getRelationsUndirected();
            for (Relation relation : relationsUndirected) {
                Entry dup = relation.getEntry();
                Entry org = duplicatesDup2OrgHM.get(dup);
                if (org != null) {
                    relation.updateEntry(org);
                    entry.removeRelatedEntry(dup, relation.getDirection());
                    entry.addRelatedEntry(org, relation.getDirection());
                }
            }

        }

        for (Entry duplicateEntry : duplicatesDup2OrgHM.keySet()) {

            Entry originalEntry = duplicatesDup2OrgHM.get(duplicateEntry);

            ArrayList<Relation> originalIncoming = originalEntry.getRelationsIncoming();
            ArrayList<Relation> originalOutgoing = originalEntry.getRelationsOutgoing();
            ArrayList<Relation> originalUndirected = originalEntry.getRelationsUndirected();

            ArrayList<Relation> relationsIncoming = duplicateEntry.getRelationsIncoming();
            for (Relation relation : relationsIncoming) {
                if (!originalIncoming.contains(relation)) {
                    originalEntry.addRelation(relation);
                }
            }

            ArrayList<Relation> relationsOutgoing = duplicateEntry.getRelationsOutgoing();
            for (Relation relation : relationsOutgoing) {
                if (!originalOutgoing.contains(relation)) {
                    originalEntry.addRelation(relation);
                }
            }

            ArrayList<Relation> relationsUndirected = duplicateEntry.getRelationsUndirected();
            for (Relation relation : relationsUndirected) {
                if (!originalUndirected.contains(relation)) {
                    originalEntry.addRelation(relation);
                }
            }
        }

        for (Entry entry : entryList) {
            HashSet<Relation> relationToBeRemoved = new HashSet<Relation>();

            ArrayList<Relation> relationsIncoming = entry.getRelationsIncoming();
            ArrayList<Relation> relationsOutgoing = entry.getRelationsOutgoing();
            ArrayList<Relation> relationsUndirected = entry.getRelationsUndirected();

            for (int i = 0; i < relationsIncoming.size(); i++) {
                for (int j = i + 1; j < relationsIncoming.size(); j++) {
                    if (relationsIncoming.get(i).equals(relationsIncoming.get(j))) {
                        relationToBeRemoved.add(relationsIncoming.get(j));
                    }
                }
            }
            relationsIncoming.removeAll(relationToBeRemoved);//Removes all including the one to be kept
            relationsIncoming.addAll(relationToBeRemoved);//The one to be kept is added.
            relationToBeRemoved.clear();

            for (int i = 0; i < relationsOutgoing.size(); i++) {
                for (int j = i + 1; j < relationsOutgoing.size(); j++) {
                    if (relationsOutgoing.get(i).equals(relationsOutgoing.get(j))) {
                        relationToBeRemoved.add(relationsOutgoing.get(j));
                    }
                }
            }
            relationsOutgoing.removeAll(relationToBeRemoved);//Removes all including the one to be kept
            relationsOutgoing.addAll(relationToBeRemoved);//The one to be kept is added.
            relationToBeRemoved.clear();

            for (int i = 0; i < relationsUndirected.size(); i++) {
                for (int j = i + 1; j < relationsUndirected.size(); j++) {
                    if (relationsUndirected.get(i).equals(relationsUndirected.get(j))) {
                        relationToBeRemoved.add(relationsUndirected.get(j));
                    }
                }
            }
            relationsUndirected.removeAll(relationToBeRemoved);//Removes all including the one to be kept
            relationsUndirected.addAll(relationToBeRemoved);//The one to be kept is added.
        }

        entryList.removeAll(addedDuplicates);
        entryHash.keySet().removeAll(addedDuplicates);

//		for(int i=0;i<entryList.size();i++){
//			Entry entry=entryList.get(i);
//			entry.removeDuplicateRelations(duplicatesDup2OrgHM);
//		}
        return 0;
    }

}
