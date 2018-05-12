package org.dapath.internal.keggoperations;

/*
 * Ozan Ozisik
 */
import org.dapath.internal.pathway.RelationDirection;
import org.dapath.internal.pathway.EntryType;
import org.dapath.internal.pathway.RelationType;
import org.dapath.internal.pathway.Relation;
import org.dapath.internal.pathway.Pathway;
import org.dapath.internal.pathway.Entry;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.*;
import org.dapath.internal.dapath.Parameters;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 *
 * @author Ozan Ozisik
 * @see
 * <a href=http://www.kegg.jp/kegg/xml/docs/>http://www.kegg.jp/kegg/xml/docs/</a>
 */
public class KGMLParser {

    private HashMap<String, String> geneIdToSymbolMap;

    /**
     * Reads xml file, parses it, and creates Pathway object.
     *
     * @param fInStream
     * @return pathway
     * @throws ExcNoSuchEntryWithId
     */
    public Pathway read(FileInputStream fInStream, HashMap<String, String> geneIdToSymbolMap) {

        this.geneIdToSymbolMap = geneIdToSymbolMap;

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        Document document;
        Pathway pathway = null;

        try {

            //To prevent parser trying to reach "http://www.kegg.jp/kegg/xml/KGML_v0.7.1_.dtd"
            //for validation of xml.
            builderFactory.setValidating(false);
            builderFactory.setNamespaceAware(true);
            builderFactory.setFeature("http://xml.org/sax/features/namespaces", false);
            builderFactory.setFeature("http://xml.org/sax/features/validation", false);
            builderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            builderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            builder = builderFactory.newDocumentBuilder();

            document = builder.parse(fInStream);
            Element rootElement = document.getDocumentElement();

            pathway = new Pathway(rootElement.getAttribute("name"), rootElement.getAttribute("org"), rootElement.getAttribute("number"), rootElement.getAttribute("title"), rootElement.getAttribute("image"), rootElement.getAttribute("link"));
            System.out.println(rootElement.getAttribute("name"));

            NodeList nodes1 = rootElement.getChildNodes();
            for (int i = 0; i < nodes1.getLength(); i++) {
                if (nodes1.item(i) instanceof Element) {

                    Element element = (Element) nodes1.item(i);

                    if (element.getTagName().compareTo("entry") == 0) {
                        parseEntry(element, pathway);
                    } else if (element.getTagName().compareTo("reaction") == 0) {
                        //TODO Currently unnecessary
                    } else if (element.getTagName().compareTo("relation") == 0) {
                        parseRelation(element, pathway);
                    } else {
                        System.out.println("Warning: KGMLParser. Unknown tag in 1st level nodes in pathway " + pathway.getName() + " " + pathway.getTitle());
                    }
                }
            }

            //System.out.println(pathway.pathwayToString());
        } catch (ParserConfigurationException e) {
            //TODO
            e.printStackTrace();
        } catch (SAXException e) {
            //	TODO
            e.printStackTrace();
        } catch (IOException e) {
            //TODO
            e.printStackTrace();
        } catch (Exception e) {
            //TODO			
            e.printStackTrace();
        }

        return pathway;

    }

    /**
     * Parses "entry" in the xml file. The entry element contains information
     * about a node of the pathway. Attributes of this element are id, name,
     * type, link, reaction. Entry type may be ortholog, enzyme, reaction, gene,
     * group, compound, map. An entry may have a graphics sub-element that
     * contains graphics information. An entry may have component sub-elements
     * if the entry's type is group.
     *
     * @param element
     * @param pathway
     * @throws ExcNoSuchEntryWithId
     *
     */
    /**
     * @param element
     * @param pathway
     * @throws Exception
     */
    private void parseEntry(Element element, Pathway pathway) throws Exception {

        boolean ignoreEntry = false;//Some entries may be ignored. For example, TITLE entry.
        //It only contains the title of the map just to show on graphics, it does not interact.		

        EntryType eType = null;
        String type = element.getAttribute("type");
        if (type.compareTo("ortholog") == 0) {
            eType = EntryType.ORTHOLOG;
        } else if (type.compareTo("enzyme") == 0) {
            eType = EntryType.ENZYME;
        } else if (type.compareTo("reaction") == 0) {
            eType = EntryType.REACTION;
        } else if (type.compareTo("gene") == 0) {
            eType = EntryType.GENE;
        } else if (type.compareTo("group") == 0) {
            eType = EntryType.GROUP;
        } else if (type.compareTo("compound") == 0) {
            eType = EntryType.COMPOUND;
        } else if (type.compareTo("map") == 0) {
            eType = EntryType.MAP;
        } else if (type.compareTo("outcome") == 0) {
            eType = EntryType.OUTCOME;
        } else if (type.compareTo("other") == 0) {
            eType = EntryType.OTHER;
        } else {
            System.out.println("Unknown entry type." + type);
        }

        String symbol = null;
        String entryId;
        ArrayList<Integer> components = new ArrayList<>();
        int graphicX = -1, graphicY = -1;

        String entryIdsTogether = element.getAttribute("name");//There may be a single entry id or multiple entry ids.		
        String[] entryIdsArr = entryIdsTogether.split(" ");

        if ((eType == EntryType.GENE) && (entryIdsArr.length > 1)) {
            eType = EntryType.MULTIGENE;
        }

        entryId = entryIdsTogether;

        NodeList nodes = element.getChildNodes();
        for (int j = 0; j < nodes.getLength(); j++) {
            if (nodes.item(j) instanceof Element) {

                Element element2 = (Element) nodes.item(j);

                if (element2.getTagName().compareTo("graphics") == 0) {
                    try {
                        graphicX = Integer.parseInt(element2.getAttribute("x"));
                        graphicY = Integer.parseInt(element2.getAttribute("y"));
                    } catch (NumberFormatException ex) {
                        String coords = element2.getAttribute("coords");
                        String coord[] = coords.split("[, ]");
                        graphicX = Integer.parseInt(coord[0]);
                        graphicY = Integer.parseInt(coord[1]);
                    }

                    String nameFull2 = element2.getAttribute("name");

                    String nameArr2;

                    if (eType == EntryType.MAP) {
                        if (nameFull2.contains("TITLE")) {
                            ignoreEntry = true;
                        }
                        symbol = nameFull2;

                    } else if (eType == EntryType.GENE) {
                        symbol = geneIdToSymbolMap.get(entryIdsArr[0]);//If type is gene, there is a single element in entryIdsArr 
                        if (symbol == null) {
                            symbol = nameFull2;
                            //symbol=nameFull2.split(", |,| |\\.\\.\\.")[0];
                            System.out.println("Name could not be found in geneNamesMap: " + entryIdsArr[0]);
                            System.out.println(entryId);
                        }
                    } else {
                        symbol = nameFull2;
                        //nameArr2=nameFull2.split(", |,| |\\.\\.\\.");
                        //symbol=new ArrayList<String>(Arrays.asList(nameArr2));
                    }

                } else if (element2.getTagName().compareTo("component") == 0) {//groups have this property
                    components.add(Integer.parseInt(element2.getAttribute("id")));
                }
            }
        }

        if (!ignoreEntry) {
            Entry entry = new Entry(Integer.parseInt(element.getAttribute("id")), eType, element.getAttribute("link"), entryId, symbol, element.getAttribute("reaction"), graphicX, graphicY);

            if (eType == EntryType.GROUP) {
                ArrayList<Entry> componentEntries = new ArrayList<Entry>();
                for (int c = 0; c < components.size(); c++) {
                    Entry compEntry = pathway.getEntryWithPathwaySpecificId(components.get(c));
                    compEntry.setParent(entry);
                    componentEntries.add(compEntry);
                }
                entry.setComponentEntries(componentEntries);
            }
            pathway.addEntry(entry);

            if (eType == EntryType.MULTIGENE) {
                ArrayList<Entry> componentEntries = new ArrayList<Entry>();
                int pathwaySpecificId = Integer.parseInt(element.getAttribute("id"));
                for (int i = 0; i < entryIdsArr.length; i++) {
                    String subEntrySymbol = geneIdToSymbolMap.get(entryIdsArr[i]);
                    if (subEntrySymbol != null) {
                        //System.out.println(""+subEntryNameList.get(0));
                        Entry subEntry = new Entry(pathwaySpecificId * 1000 + i, EntryType.GENE, "", entryIdsArr[i], subEntrySymbol, element.getAttribute("reaction"), graphicX, graphicY);
                        subEntry.setParent(entry);
                        pathway.addEntry(subEntry);
                        componentEntries.add(subEntry);
                    }
                }
                entry.setComponentEntries(componentEntries);
            }

        }

    }

    /**
     * Parses "relation" in the xml file. The relation element specifies
     * relationship between two proteins (gene products) or two KOs (ortholog
     * groups) or protein and compound, which is indicated by an arrow or a line
     * connecting two nodes in the KEGG pathways. The relation element has a
     * subelement named the subtype element. When the name attribute value of
     * the subtype element is a value with directionality like "activation", the
     * direction of the interaction is from entry1 to entry2.
     *
     * Subtype name and values:
     *
     * * compound, Entry element id attribute value for compound, ECRel and
     * PPRel, shared with two successive reactions (ECrel) or intermediate of
     * two interacting proteins (PPrel) * hidden compound, Entry element id
     * attribute value for hidden compound, ECRel, shared with two successive
     * reactions but not displayed in the pathway map * activation, -->, PPRel,
     * positive and negative effects which may be associated with molecular
     * information below. * inhibition, --|, PPRel * expression, -->, GERel,
     * interactions via DNA binding * repression, --|, GERel * indirect effect,
     * ..>, PPRel and GERel, indirect effect without molecular details * state
     * change, ..., PPRel, state transition * binding/association, ---, PPRel,
     * association and dissociation * dissociation, -+-, PPRel * missing
     * interaction, -/-, PPRel and GERel, missing interaction due to mutation,
     * etc. * phosphorylation, +p, , PPRel, molecular events *
     * dephosphorylation, -p, PPRel * glycosylation +g, PPRel * ubiquitination
     * +u, PPRel * methylation +m, PPRel
     *
     * @param element
     * @param pathway
     * @throws ExcNoSuchEntryWithId
     *
     */
    private void parseRelation(Element element, Pathway pathway) throws Exception {

        int entry1Id = Integer.parseInt(element.getAttribute("entry1"));
        int entry2Id = Integer.parseInt(element.getAttribute("entry2"));
        String type = element.getAttribute("type");

        Entry readEntry1 = pathway.getEntryWithPathwaySpecificId(entry1Id);
        Entry readEntry2 = pathway.getEntryWithPathwaySpecificId(entry2Id);

        //TODO Be cautious, this part is important, if an entry has a parent (group for example), relations are assigned to group.
        if(Parameters.kgmlParserAssignRelationsToParent){
            if (readEntry1.getParent() != null) {
                readEntry1 = readEntry1.getParent();
            }
            if (readEntry2.getParent() != null) {
                readEntry2 = readEntry2.getParent();
            }
        }

        NodeList nodes = element.getChildNodes();
        boolean importantRelationFound = false;

        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element) {

                Element element2 = (Element) nodes.item(i);

                String subtypeName = element2.getAttribute("name");
                String subtypeValue = element2.getAttribute("value");

                Relation relation1 = null;
                Relation relation2 = null;

                switch (subtypeName) {
                    case "activation":
                        pathway.setIncludesUsableRelation(true);
                        relation1 = new Relation(readEntry1, RelationType.ACTIVATION, subtypeValue, RelationDirection.INCOMING);
                        relation2 = new Relation(readEntry2, RelationType.ACTIVATION, subtypeValue, RelationDirection.OUTGOING);
                        readEntry1.addRelation(relation2);
                        readEntry2.addRelation(relation1);
                        importantRelationFound = true;
                        break;
                    case "inhibition":
                        pathway.setIncludesUsableRelation(true);
                        relation1 = new Relation(readEntry1, RelationType.INHIBITION, subtypeValue, RelationDirection.INCOMING);
                        relation2 = new Relation(readEntry2, RelationType.INHIBITION, subtypeValue, RelationDirection.OUTGOING);
                        readEntry1.addRelation(relation2);
                        readEntry2.addRelation(relation1);
                        importantRelationFound = true;
                        break;
                    case "expression":
                        pathway.setIncludesUsableRelation(true);
                        relation1 = new Relation(readEntry1, RelationType.EXPRESSION, subtypeValue, RelationDirection.INCOMING);
                        relation2 = new Relation(readEntry2, RelationType.EXPRESSION, subtypeValue, RelationDirection.OUTGOING);
                        readEntry1.addRelation(relation2);
                        readEntry2.addRelation(relation1);
                        importantRelationFound = true;
                        break;
                    case "repression":
                        pathway.setIncludesUsableRelation(true);
                        relation1 = new Relation(readEntry1, RelationType.REPRESSION, subtypeValue, RelationDirection.INCOMING);
                        relation2 = new Relation(readEntry2, RelationType.REPRESSION, subtypeValue, RelationDirection.OUTGOING);
                        readEntry1.addRelation(relation2);
                        readEntry2.addRelation(relation1);
                        importantRelationFound = true;
                        break;
                }
            }
        }

        if (!importantRelationFound) {
            for (int i = 0; i < nodes.getLength(); i++) {
                if (nodes.item(i) instanceof Element) {

                    Element element2 = (Element) nodes.item(i);

                    String subtypeName = element2.getAttribute("name");
                    String subtypeValue = element2.getAttribute("value");

                    Relation relation1;
                    Relation relation2;

                    switch (subtypeName) {
                        case "compound":
                            pathway.setIncludesUsableRelation(true);
                            //Presence of shared compound is assumed to be activation, and it is directional.
                            relation1 = new Relation(readEntry1, RelationType.ACTIVATION, subtypeValue, RelationDirection.INCOMING);
                            relation2 = new Relation(readEntry2, RelationType.ACTIVATION, subtypeValue, RelationDirection.OUTGOING);
                            readEntry1.addRelation(relation2);
                            readEntry2.addRelation(relation1);

                            break;
                        case "hidden compound":
                            //Presence of shared hidden compound is assumed to be activation, and it is directional.
                            relation1 = new Relation(readEntry1, RelationType.ACTIVATION, subtypeValue, RelationDirection.INCOMING);
                            relation2 = new Relation(readEntry2, RelationType.ACTIVATION, subtypeValue, RelationDirection.OUTGOING);
                            readEntry1.addRelation(relation2);
                            readEntry2.addRelation(relation1);
                            break;
//					case "activation":
//						pathway.setIncludesUsableRelation(true);
//						relation1=new Relation(readEntry1, RelationType.ACTIVATION, subtypeValue, RelationDirection.INCOMING);
//						relation2=new Relation(readEntry2, RelationType.ACTIVATION, subtypeValue, RelationDirection.OUTGOING);
//						
//						readEntry1.addRelation(relation2);
//						readEntry2.addRelation(relation1);
//						break;
//					case "inhibition":
//						pathway.setIncludesUsableRelation(true);
//						relation1=new Relation(readEntry1, RelationType.INHIBITION, subtypeValue, RelationDirection.INCOMING);
//						relation2=new Relation(readEntry2, RelationType.INHIBITION, subtypeValue, RelationDirection.OUTGOING);
//						
//						readEntry1.addRelation(relation2);
//						readEntry2.addRelation(relation1);
//						break;
//					case "expression":
//						pathway.setIncludesUsableRelation(true);
//						relation1=new Relation(readEntry1, RelationType.EXPRESSION, subtypeValue, RelationDirection.INCOMING);
//						relation2=new Relation(readEntry2, RelationType.EXPRESSION, subtypeValue, RelationDirection.OUTGOING);
//						
//						readEntry1.addRelation(relation2);
//						readEntry2.addRelation(relation1);
//						break;
//					case "repression":
//						pathway.setIncludesUsableRelation(true);
//						relation1=new Relation(readEntry1, RelationType.REPRESSION, subtypeValue, RelationDirection.INCOMING);
//						relation2=new Relation(readEntry2, RelationType.REPRESSION, subtypeValue, RelationDirection.OUTGOING);
//						
//						readEntry1.addRelation(relation2);
//						readEntry2.addRelation(relation1);
//						break;
                        case "indirect effect":
                            pathway.setIncludesUsableRelation(true);
//						relation1=new Relation(readEntry1, RelationType.INDIRECT_EFFECT, subtypeValue, RelationDirection.INCOMING);
//						relation2=new Relation(readEntry2, RelationType.INDIRECT_EFFECT, subtypeValue, RelationDirection.OUTGOING);

                            //Indirect effect is assumed to be activation.
                            relation1 = new Relation(readEntry1, RelationType.ACTIVATION, subtypeValue, RelationDirection.INCOMING);
                            relation2 = new Relation(readEntry2, RelationType.ACTIVATION, subtypeValue, RelationDirection.OUTGOING);
                            readEntry1.addRelation(relation2);
                            readEntry2.addRelation(relation1);
                            break;
                        case "indirect":
                            pathway.setIncludesUsableRelation(true);
//						relation1=new Relation(readEntry1, RelationType.INDIRECT_EFFECT, subtypeValue, RelationDirection.INCOMING);
//						relation2=new Relation(readEntry2, RelationType.INDIRECT_EFFECT, subtypeValue, RelationDirection.OUTGOING);

                            //Indirect is assumed to be activation.
                            relation1 = new Relation(readEntry1, RelationType.ACTIVATION, subtypeValue, RelationDirection.INCOMING);
                            relation2 = new Relation(readEntry2, RelationType.ACTIVATION, subtypeValue, RelationDirection.OUTGOING);
                            readEntry1.addRelation(relation2);
                            readEntry2.addRelation(relation1);
                            break;    
                        case "state change":
//						relation1=new Relation(readEntry1, RelationType.STATE_CHANGE, subtypeValue,RelationDirection.UNDIRECTED);
//						relation2=new Relation(readEntry2, RelationType.STATE_CHANGE, subtypeValue, RelationDirection.UNDIRECTED);

                            pathway.setIncludesUsableRelation(true);
                            //State change is assumed to be activation.						
                            relation1 = new Relation(readEntry1, RelationType.STATE_CHANGE, subtypeValue, RelationDirection.INCOMING);
                            relation2 = new Relation(readEntry2, RelationType.STATE_CHANGE, subtypeValue, RelationDirection.OUTGOING);
                            readEntry1.addRelation(relation2);
                            readEntry2.addRelation(relation1);
                            break;
                        case "binding/association":
//						relation1=new Relation(readEntry1, RelationType.BINDING_ASSOCIATION, subtypeValue, RelationDirection.UNDIRECTED);
//						relation2=new Relation(readEntry2, RelationType.BINDING_ASSOCIATION, subtypeValue, RelationDirection.UNDIRECTED);
//						
//						readEntry1.addRelation(relation2);
//						readEntry2.addRelation(relation1);

                            //Binding/Association is assumed to be activation.
                            relation1 = new Relation(readEntry1, RelationType.ACTIVATION, subtypeValue, RelationDirection.INCOMING);
                            relation2 = new Relation(readEntry2, RelationType.ACTIVATION, subtypeValue, RelationDirection.OUTGOING);

                            readEntry1.addRelation(relation2);
                            readEntry2.addRelation(relation1);

                            relation1 = new Relation(readEntry1, RelationType.ACTIVATION, subtypeValue, RelationDirection.OUTGOING);
                            relation2 = new Relation(readEntry2, RelationType.ACTIVATION, subtypeValue, RelationDirection.INCOMING);

                            readEntry1.addRelation(relation2);
                            readEntry2.addRelation(relation1);
                            break;
                        case "dissociation":
//						relation1=new Relation(readEntry1, RelationType.DISSOCIATION, subtypeValue, RelationDirection.UNDIRECTED);
//						relation2=new Relation(readEntry2, RelationType.DISSOCIATION, subtypeValue, RelationDirection.UNDIRECTED);
//						
//						readEntry1.addRelation(relation2);
//						readEntry2.addRelation(relation1);

                            //Dissociation is assumed to be activation.
                            relation1 = new Relation(readEntry1, RelationType.ACTIVATION, subtypeValue, RelationDirection.INCOMING);
                            relation2 = new Relation(readEntry2, RelationType.ACTIVATION, subtypeValue, RelationDirection.OUTGOING);

                            readEntry1.addRelation(relation2);
                            readEntry2.addRelation(relation1);

                            relation1 = new Relation(readEntry1, RelationType.ACTIVATION, subtypeValue, RelationDirection.OUTGOING);
                            relation2 = new Relation(readEntry2, RelationType.ACTIVATION, subtypeValue, RelationDirection.INCOMING);

                            readEntry1.addRelation(relation2);
                            readEntry2.addRelation(relation1);
                            break;
                        case "missing interaction":
//						relation1=new Relation(readEntry1, RelationType.MISSING_INTERACTION, subtypeValue, RelationDirection.UNDIRECTED);
//						relation2=new Relation(readEntry2, RelationType.MISSING_INTERACTION, subtypeValue, RelationDirection.UNDIRECTED);
//						
//						readEntry1.addRelation(relation2);
//						readEntry2.addRelation(relation1);

                            //missing interaction is assumed to be activation.
                            relation1 = new Relation(readEntry1, RelationType.ACTIVATION, subtypeValue, RelationDirection.INCOMING);
                            relation2 = new Relation(readEntry2, RelationType.ACTIVATION, subtypeValue, RelationDirection.OUTGOING);

                            readEntry1.addRelation(relation2);
                            readEntry2.addRelation(relation1);
                            break;
                        case "phosphorylation":
                            pathway.setIncludesUsableRelation(true);
//						relation1=new Relation(readEntry1, RelationType.PHOSPHORYLATION, subtypeValue, RelationDirection.INCOMING);
//						relation2=new Relation(readEntry2, RelationType.PHOSPHORYLATION, subtypeValue, RelationDirection.OUTGOING);

                            //If activation or inhibition is not mentioned phosphorylation is assumed to be activation.
                            relation1 = new Relation(readEntry1, RelationType.ACTIVATION, subtypeValue, RelationDirection.INCOMING);
                            relation2 = new Relation(readEntry2, RelationType.ACTIVATION, subtypeValue, RelationDirection.OUTGOING);

                            readEntry1.addRelation(relation2);
                            readEntry2.addRelation(relation1);
                            break;
                        case "dephosphorylation":
                            pathway.setIncludesUsableRelation(true);
//						relation1=new Relation(readEntry1, RelationType.DEPHOSPHORYLATION, subtypeValue, RelationDirection.INCOMING);
//						relation2=new Relation(readEntry2, RelationType.DEPHOSPHORYLATION, subtypeValue, RelationDirection.OUTGOING);

                            //If activation or inhibition is not mentioned dephosphorylation is assumed to be activation.
                            relation1 = new Relation(readEntry1, RelationType.ACTIVATION, subtypeValue, RelationDirection.INCOMING);
                            relation2 = new Relation(readEntry2, RelationType.ACTIVATION, subtypeValue, RelationDirection.OUTGOING);

                            readEntry1.addRelation(relation2);
                            readEntry2.addRelation(relation1);
                            break;
                        case "glycosylation":
//						relation1=new Relation(readEntry1, RelationType.GLYCOSYLATION, subtypeValue, RelationDirection.INCOMING);
//						relation2=new Relation(readEntry2, RelationType.GLYCOSYLATION, subtypeValue, RelationDirection.OUTGOING);
//						
//						readEntry1.addRelation(relation2);
//						readEntry2.addRelation(relation1);
                            break;
                        case "ubiquitination":
                            pathway.setIncludesUsableRelation(true);
//						relation1=new Relation(readEntry1, RelationType.UBIQUITINATION, subtypeValue, RelationDirection.INCOMING);
//						relation2=new Relation(readEntry2, RelationType.UBIQUITINATION, subtypeValue, RelationDirection.OUTGOING);

                            //If activation or inhibition is not mentioned ubiquitination is assumed to be inhibition.
                            relation1 = new Relation(readEntry1, RelationType.INHIBITION, subtypeValue, RelationDirection.INCOMING);
                            relation2 = new Relation(readEntry2, RelationType.INHIBITION, subtypeValue, RelationDirection.OUTGOING);

                            readEntry1.addRelation(relation2);
                            readEntry2.addRelation(relation1);
                            break;
                        case "methylation":
//						pathway.setIncludesUsableRelation(true);
//						relation1=new Relation(readEntry1, RelationType.METHYLATION, subtypeValue, RelationDirection.INCOMING);
//						relation2=new Relation(readEntry2, RelationType.METHYLATION, subtypeValue, RelationDirection.OUTGOING);
//						
//						readEntry1.addRelation(relation2);
//						readEntry2.addRelation(relation1);
                            break;
                        default:
                            System.out.println("Unknown subtype name in parsing relation: "+subtypeName);
                    }

                }
            }
        }

    }

}
