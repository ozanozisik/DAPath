package org.dapath.internal.pathway;

/*
 * Ozan Ozisik
 */
public enum EntryType {
    ORTHOLOG(1), ENZYME(2), REACTION(3), GENE(4), GROUP(5), COMPOUND(6), MAP(7), MULTIGENE(8), OUTCOME(9), OTHER(10);
    //Multigene is made up by me for gene entries in graphics that is composed of many genes
    //like hsa:10482 hsa:55998 hsa:56000 hsa:56001 hsa:728343
    //Outcome is made up for entries related to pathway outcomes like apoptosis, proliferation, differentiation

    private int type;

    private EntryType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public String getTypeStr() {
        switch (type) {
            case 1:
                return "ortholog";
            case 2:
                return "enzyme";
            case 3:
                return "reaction";
            case 4:
                return "gene";
            case 5:
                return "group";
            case 6:
                return "compound";
            case 7:
                return "map";
            case 8:
                return "multigene";
            case 9:
                return "outcome";
            case 10:
                return "other";
            default:
                return "unknownType";
        }
    }
}
