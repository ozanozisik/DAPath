package org.dapath.internal.pathway;

/*
 * Ozan Ozisik
 */
public class Relation {

    private Entry entry;
    private RelationType type;
    private String symbol;
    private RelationDirection direction;

    public Relation(Entry entry, RelationType type, String symbol, RelationDirection direction) {
        this.entry = entry;
        this.type = type;
        this.symbol = symbol;
        this.direction = direction;
    }

    public Relation getCopy() {
        return new Relation(this.entry, this.type, this.symbol, this.direction);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((direction == null) ? 0 : direction.hashCode());
        result = prime * result + ((entry == null) ? 0 : entry.hashCode());
        result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
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
        Relation other = (Relation) obj;
        if (direction != other.direction) {
            return false;
        }
        if (entry == null) {
            if (other.entry != null) {
                return false;
            }
//		} else if (!entry.equals(other.entry))
        } else if (!entry.getEntryId().equals((other.entry.getEntryId()))) {
            return false;
        }
        if (symbol == null) {
            if (other.symbol != null) {
                return false;
            }
        } else if (!symbol.equals(other.symbol)) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        return true;
    }

    public Entry getEntry() {
        return entry;
    }

    public RelationType getType() {
        return type;
    }

    public String getSymbol() {
        return symbol;
    }

    public RelationDirection getDirection() {
        return direction;
    }

    public void updateEntry(Entry entry) {
        this.entry = entry;
    }

    public String toString() {
        return "EntryId " + entry.getPathwaySpecificId() + " relType " + type + " symbol " + symbol + " dir " + direction;
    }
}
