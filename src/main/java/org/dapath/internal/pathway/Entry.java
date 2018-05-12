package org.dapath.internal.pathway;

/*
 * Ozan Ozisik
 */
import java.util.ArrayList;
import java.util.HashSet;

public class Entry {

    /**
     * the ID of this entry in the pathway map, "id" in KEGG xml
     */
    private int pathwaySpecificId;
    /**
     * the type of this entry (ortholog, enzyme, reaction, gene, group,
     * compound, map, multigene, outcome, other)
     */
    private EntryType type;
    /**
     * the resource location of the information about this entry
     */
    private String link;
    /**
     * the id/entryId of this entry, like hsa:6885, "name" in KEGG xml
     */
    private String entryId;
    /**
     * Symbol of the entry, like MAP3K7
     */
    private String symbol;

    private String reaction;
    private int x, y;

    private ArrayList<Entry> components;
    private Entry parent = null;//if entry is part of a multigene, this won't be null

    private ArrayList<Relation> relationsIncoming;
    private ArrayList<Relation> relationsOutgoing;
    private ArrayList<Relation> relationsUndirected;

    private HashSet<Entry> relatedEntriesIncoming;
    private HashSet<Entry> relatedEntriesOutgoing;
    private HashSet<Entry> relatedEntriesUndirected;

    public Entry(int pathwaySpecificId, EntryType type, String link, String entryId, String symbol, String reaction, int x, int y) {
        this.pathwaySpecificId = pathwaySpecificId;
        this.type = type;
        this.link = link;
        this.entryId = entryId;
        this.symbol = symbol;
        this.reaction = reaction;
        this.x = x;
        this.y = y;

        relationsIncoming = new ArrayList<Relation>();
        relationsOutgoing = new ArrayList<Relation>();
        relationsUndirected = new ArrayList<Relation>();

        relatedEntriesIncoming = new HashSet<Entry>();
        relatedEntriesOutgoing = new HashSet<Entry>();
        relatedEntriesUndirected = new HashSet<Entry>();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + pathwaySpecificId;
        result = prime * result + ((link == null) ? 0 : link.hashCode());
        result = prime * result + ((entryId == null) ? 0 : entryId.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Entry other = (Entry) obj;
        if (pathwaySpecificId != other.pathwaySpecificId) {
            return false;
        }
        if (link == null) {
            if (other.link != null) {
                return false;
            }
        } else if (!link.equals(other.link)) {
            return false;
        }
        if (entryId == null) {
            if (other.entryId != null) {
                return false;
            }
        } else if (!entryId.equals(other.entryId)) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        return true;
    }

    /**
     * Sets components of an entry with type "group"
     *
     * @param entries
     */
    public void setComponentEntries(ArrayList<Entry> entries) {
        components = new ArrayList<Entry>(entries);
    }

    public void setParent(Entry parent) {
        this.parent = parent;
    }

    public Entry getParent() {
        return parent;
    }

    /**
     * @return id of entry specific to pathway
     */
    public int getPathwaySpecificId() {
        return pathwaySpecificId;
    }

    public EntryType getType() {
        return type;
    }

    public String getLink() {
        return link;
    }

    public String getEntryId() {
        return entryId;
    }

    /**
     * @return entry symbol, like MAP3K7
     */
    public String getSymbol() {
        return symbol;
    }

    public String getReaction() {
        return reaction;
    }

    public ArrayList<Entry> getComponents() {
        return components;
    }

    public ArrayList<Relation> getRelationsIncoming() {
        return relationsIncoming;
    }

    public ArrayList<Relation> getRelationsOutgoing() {
        return relationsOutgoing;
    }

    public ArrayList<Relation> getRelationsUndirected() {
        return relationsUndirected;
    }

    public HashSet<Entry> getRelatedEntriesIncoming() {
        return relatedEntriesIncoming;
    }

    public HashSet<Entry> getRelatedEntriesOutgoing() {
        return relatedEntriesOutgoing;
    }

    public HashSet<Entry> getRelatedEntriesUndirected() {
        return relatedEntriesUndirected;
    }

    public int getOutgoingEntryNumber() {
        return relatedEntriesOutgoing.size();
    }

    public int getIncomingEntryNumber() {
        return relatedEntriesIncoming.size();
    }

    public void addRelation(Relation relation) {
        //System.out.println(""+ relation.getType().toString()+" relation added to "+this.id+" "+direction.toString()+" "+ relation.getEntry().getId());
        RelationDirection direction = relation.getDirection();
        if (direction == RelationDirection.INCOMING) {
            relationsIncoming.add(relation);
            relatedEntriesIncoming.add(relation.getEntry());
        } else if (direction == RelationDirection.OUTGOING) {
            relationsOutgoing.add(relation);
            relatedEntriesOutgoing.add(relation.getEntry());
        } else {
            relationsUndirected.add(relation);
            relatedEntriesUndirected.add(relation.getEntry());
        }
    }

    public void addRelatedEntry(Entry entry, RelationDirection direction) {
        if (direction == RelationDirection.INCOMING) {
            relatedEntriesIncoming.add(entry);
        } else if (direction == RelationDirection.OUTGOING) {
            relatedEntriesOutgoing.add(entry);
        } else {
            relatedEntriesUndirected.add(entry);
        }
    }

    public void removeRelatedEntry(Entry entry, RelationDirection direction) {
        if (direction == RelationDirection.INCOMING) {
            relatedEntriesIncoming.remove(entry);
        } else if (direction == RelationDirection.OUTGOING) {
            relatedEntriesOutgoing.remove(entry);
        } else {
            relatedEntriesUndirected.remove(entry);
        }
    }

    public void removeRelation(Relation relation) {
        RelationDirection direction = relation.getDirection();
        if (direction == RelationDirection.INCOMING) {
            relationsIncoming.remove(relation);
            relatedEntriesIncoming.remove(relation.getEntry());
        } else if (direction == RelationDirection.OUTGOING) {
            relationsOutgoing.remove(relation);
            relatedEntriesOutgoing.remove(relation.getEntry());
        } else {
            relationsUndirected.remove(relation);
            relatedEntriesUndirected.remove(relation.getEntry());
        }
    }

    @Override
    public String toString() {
        return "" + this.getPathwaySpecificId() + " " + this.getEntryId()+ " " + this.getSymbol();
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
