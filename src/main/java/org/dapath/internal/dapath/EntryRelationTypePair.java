/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dapath.internal.dapath;

import java.util.ArrayList;
import java.util.HashMap;
import org.dapath.internal.pathway.Entry;
import org.dapath.internal.pathway.EntryType;
import org.dapath.internal.pathway.RelationType;

/**
 *
 * @author Ozan Ozisik
 */
public class EntryRelationTypePair {
    private Entry entry;
    private RelationType relationType;
    private static HashMap<String, Double> idToValueMap;
    private static HashMap<String, Double> idToValueMapBeforeCrosstalkHandling;

    public EntryRelationTypePair(Entry entry, RelationType relationType) {
        super();
        this.entry = entry;
        this.relationType = relationType;
    }

    public static void setIdToValueMap(HashMap<String, Double> idToValueMapp) {
        idToValueMap = idToValueMapp;
    }

    public static void setIdToValueMapBeforeCrosstalkHandling(HashMap<String, Double> idToValueMapBeforeCrosstalkHandlingg) {
        idToValueMapBeforeCrosstalkHandling = idToValueMapBeforeCrosstalkHandlingg;
    }

    public Entry getEntry() {
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public void setRelationType(RelationType relationType) {
        this.relationType = relationType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entry == null) ? 0 : entry.hashCode());
        result = prime * result + ((relationType == null) ? 0 : relationType.hashCode());
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
        EntryRelationTypePair other = (EntryRelationTypePair) obj;
        if (entry == null) {
            if (other.entry != null) {
                return false;
            }
        } else if (!entry.equals(other.entry)) {
            return false;
        }
        if (relationType != other.relationType) {
            return false;
        }
        return true;
    }

    public String toString() {
        String str;
        str = "";
        if (entry.getType() == EntryType.GROUP) {
            str += "GROUP{";
            ArrayList<Entry> components = entry.getComponents();
            for (int i = 0; i < components.size(); i++) {
                Entry entry2 = components.get(i);
                if (entry2.getType() == EntryType.MULTIGENE) {
                    str += "MULTIGENE{";
                    ArrayList<Entry> components2 = entry2.getComponents();
                    for (int j = 0; j < components2.size(); j++) {
                        Entry entry3 = components2.get(j);
                        str += entry3.getSymbol();
                        if (idToValueMap.containsKey(entry3.getEntryId())) {
                            str += getPValueText(entry3);
                        }
                        if (j < components2.size() - 1) {
                            str += ", ";
                        }
                    }
                    str += "}";

                } else {
                    str += entry2.getSymbol();
//					if(idToValueMap.containsKey(entry2.getEntryId()) && idToValueMap.get(entry2.getEntryId())<0.05)
                    if (idToValueMap.containsKey(entry2.getEntryId())) {
                        str += getPValueText(entry2);
                    }
//						str+="***("+idToValueMap.get(entry2.getEntryId())+"/"+idToValueMapBeforeCrosstalkHandling.get(entry2.getEntryId())+")";
                }
                if (i < components.size() - 1) {
                    str += ", ";
                }
            }
            str += "}" + " " + relationType + " -- ";
        } else if (entry.getType() == EntryType.MULTIGENE) {
            str += "MULTIGENE{";

            ArrayList<Entry> components = entry.getComponents();
            for (int i = 0; i < components.size(); i++) {
                Entry entry2 = components.get(i);
                str += entry2.getSymbol();
//				if(idToValueMap.containsKey(entry2.getEntryId()) && idToValueMap.get(entry2.getEntryId())<0.05)//Does not show some rsIDs because their p increased because of crosstalk
                if (idToValueMap.containsKey(entry2.getEntryId())) {
                    str += getPValueText(entry2);
                }
//					str+="***("+idToValueMap.get(entry2.getEntryId())+"/"+idToValueMapBeforeCrosstalkHandling.get(entry2.getEntryId())+")";
                if (i < components.size() - 1) {
                    str += ", ";
                }
            }
            str += "}" + " " + relationType + " -- ";

        } else {
            str += entry.getSymbol();
//			if(idToValueMap.containsKey(entry.getEntryId()) && idToValueMap.get(entry.getEntryId())<0.05)
            if (idToValueMap.containsKey(entry.getEntryId())) {
                str += getPValueText(entry);
            }
//				str+="***("+idToValueMap.get(entry.getEntryId())+"/"+idToValueMapBeforeCrosstalkHandling.get(entry.getEntryId())+")";
            str += " " + relationType + " -- ";
        }
        return str;
    }

    public String getPValueText(Entry entry) {
        double p = idToValueMap.get(entry.getEntryId());
        double pBeforeCrossTalkModification = idToValueMapBeforeCrosstalkHandling.get(entry.getEntryId());
        String str = "";
        if (p >= 0.01) {
            str += "*";
        } else if (p < 0.01 && p > 0.00000005) {
            str += "**";
        } else {
            str += "***";
        }
        str += "(" + p + "/" + pBeforeCrossTalkModification + ")";
        return str;
    }
}
