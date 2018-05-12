/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dapath.internal.cyvisualization;

import org.dapath.internal.pathway.EntryType;
import org.dapath.internal.pathway.Relation;
import org.dapath.internal.pathway.Entry;
import org.dapath.internal.pathway.Pathway;
import java.awt.Paint;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.cytoscape.model.CyNetwork;
import org.dapath.internal.CyActivator;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyColumn;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.view.vizmap.VisualStyle;

/**
 *
 * @author Ozan Ozisik
 */
public class GraphVisualizationCy {
    
    //public static void drawPathway(Pathway pathway) {
    public static void drawMergedImpactPathway(Pathway pathway, HashMap<String, Double> idToValueMap, HashMap<String, Double> idToValueMapBeforeCrosstalkHandling, HashMap<Entry, Integer> entry2ImpactColor, HashMap<Entry, Integer> entry2FrequencyColor, int rankAmongPathways, int rankAmongMergedPaths) {
        CyNetwork cyNetwork=CyActivator.networkFactory.createNetwork();
        cyNetwork.getRow(cyNetwork).set(CyNetwork.NAME, pathway.getTitle());
        Long cyNetworkID = cyNetwork.getSUID();

        HashSet<Entry> drawnEntriesSet = new HashSet<Entry>();
        HashSet<Entry> entriesWithoutRelationSet = new HashSet<Entry>();
        HashMap<Entry,CyNode> entryToCyNodeMap = new HashMap<Entry,CyNode>();

        
        for (Entry entry : pathway.getEntryList()) {
            if (entry.getIncomingEntryNumber() + entry.getOutgoingEntryNumber() > 0) {
                CyNode node = cyNetwork.addNode();
                entryToCyNodeMap.put(entry, node);
                cyNetwork.getRow(node).set(CyNetwork.NAME, entry.getEntryId()+" "+entry.getSymbol());
                drawnEntriesSet.add(entry);
            }else {
                entriesWithoutRelationSet.add(entry);
            }
        }
        
        for (Entry entry : drawnEntriesSet) {
            for (Relation relation : entry.getRelationsOutgoing()) {
                Entry entry2 = relation.getEntry();
                if (relation.getType().getStimulation() > 0) {//activation
                    CyEdge edge = cyNetwork.addEdge(entryToCyNodeMap.get(entry),entryToCyNodeMap.get(entry2), true);
                } else if (relation.getType().getStimulation() == 0) {//neutral
                    CyEdge edge = cyNetwork.addEdge(entryToCyNodeMap.get(entry),entryToCyNodeMap.get(entry2), true);
                } else {//deactivation
                    CyEdge edge = cyNetwork.addEdge(entryToCyNodeMap.get(entry),entryToCyNodeMap.get(entry2), true);
                }
                
            }
        }
        
        CyActivator.networkManager.addNetwork(cyNetwork);
        CyNetworkView networkView;
        Collection<CyNetworkView> networkViews = CyActivator.networkViewManager.getNetworkViews(cyNetwork);
        if (networkViews.isEmpty()) {
            networkView = CyActivator.networkViewFactory.createNetworkView(cyNetwork);
            CyActivator.networkViewManager.addNetworkView(networkView);
        } else {
            networkView = networkViews.iterator().next();
        }
                
        for(Entry entry:entryToCyNodeMap.keySet()){
            CyNode cyNode=entryToCyNodeMap.get(entry);
            View<CyNode> nodeView = networkView.getNodeView(cyNode);
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, ((double)entry.getX()));
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, ((double)entry.getY()));
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR, Color.BLUE);
            
        }
        
        
        CyTable cyNetworkTable = cyNetwork.getDefaultNetworkTable();
        cyNetworkTable.setTitle(pathway.getTitle());
        CyRow cyNetworkRow = cyNetworkTable.getRow(cyNetworkID);
        createColumn(cyNetworkTable,"name",String.class);
        createColumn(cyNetworkTable,"org",String.class);
        createColumn(cyNetworkTable,"number",String.class);
        createColumn(cyNetworkTable,"title",String.class);
        createColumn(cyNetworkTable,"link",String.class);
        createColumn(cyNetworkTable,"rankAmongPathways",String.class);
        createColumn(cyNetworkTable,"rankAmongMergedPaths",Integer.class);
        cyNetworkRow.set("name", pathway.getName());
        cyNetworkRow.set("org", pathway.getOrg());
        cyNetworkRow.set("number", pathway.getNumber());
        cyNetworkRow.set("title", pathway.getTitle());
        cyNetworkRow.set("link", pathway.getLink());
        cyNetworkRow.set("rankAmongPathways", pathway.getLink());
        cyNetworkRow.set("rankAmongMergedPaths", rankAmongMergedPaths);
        
        CyTable cyNodeTable = cyNetwork.getDefaultNodeTable();
        cyNodeTable.createColumn("label", String.class, true);
        cyNodeTable.createColumn("genesymbol", String.class, true);
        cyNodeTable.createColumn("pvalues", String.class, true);
        cyNodeTable.createColumn("bgcolor", String.class, true);
        cyNodeTable.createColumn("shape", String.class, true);
        cyNodeTable.createColumn("width", String.class, true);
        cyNodeTable.createColumn("height", String.class, true);
        
        for(Entry entry:drawnEntriesSet){
            CyNode cyNode = entryToCyNodeMap.get(entry);
            CyRow cyNodeRow = cyNodeTable.getRow(cyNode.getSUID());
            
            String label = "";
            String genesymbol = "";
            String pvalues = "";

            if (entry.getType() == EntryType.GROUP) {
                label += "GROUP:" + entry.getComponents().get(0).getSymbol().split(",")[0].replace("...", "");
                genesymbol += "GROUP{\n";
                ArrayList<Entry> components = entry.getComponents();
                for (int i = 0; i < components.size(); i++) {
                    Entry entry2 = components.get(i);
                    if (entry2.getType() == EntryType.MULTIGENE) {
                        genesymbol += "MULTIGENE{\n";
                        ArrayList<Entry> components2 = entry2.getComponents();
                        for (int j = 0; j < components2.size(); j++) {
                            Entry entry3 = components2.get(j);
                            genesymbol+=entry3.getSymbol();
                            if (idToValueMap.containsKey(entry3.getEntryId())||idToValueMapBeforeCrosstalkHandling.containsKey(entry3.getEntryId())) {
                                pvalues += entry3.getSymbol();
                                pvalues += getPValueText(entry3, idToValueMap, idToValueMapBeforeCrosstalkHandling)+"\n";
                                if(j<components2.size()-1){
                                    pvalues += ", ";
                                }
                            }
                            if(j<components2.size()-1){
                                genesymbol += ", ";
                            }
                        }
                        genesymbol += "}";
                    } else {
                        genesymbol+=entry2.getSymbol();
                        if (idToValueMap.containsKey(entry2.getEntryId())||idToValueMapBeforeCrosstalkHandling.containsKey(entry2.getEntryId())) {
                            pvalues += entry2.getSymbol();
                            pvalues += getPValueText(entry2, idToValueMap, idToValueMapBeforeCrosstalkHandling) + "\n";
                            if (i < components.size() - 1) {
                                pvalues += ", ";
                            }
                        }
                    }
                    if (i < components.size() - 1) {
                        genesymbol += ", ";
                    }
                }
                genesymbol += "}";
            } else if (entry.getType() == EntryType.MULTIGENE) {
                label += "MULTIGENE:" + entry.getSymbol().split(",")[0].replace("...", "");
                genesymbol += "MULTIGENE{\n";

                ArrayList<Entry> components = entry.getComponents();
                for (int i = 0; i < components.size(); i++) {
                    Entry entry2 = components.get(i);
                    genesymbol+=entry2.getSymbol();
                    if (idToValueMap.containsKey(entry2.getEntryId())||idToValueMapBeforeCrosstalkHandling.containsKey(entry2.getEntryId())) {
                        pvalues += entry2.getSymbol();
                        pvalues += getPValueText(entry2, idToValueMap, idToValueMapBeforeCrosstalkHandling) + "\n";
                        if (i < components.size() - 1) {
                            pvalues += ", ";
                        }
                    }
                    if(i<components.size()-1){
                        genesymbol += ", ";
                    }
                }
                genesymbol += "}";

            } else {
                label += entry.getSymbol();
                genesymbol += entry.getSymbol();
                if (idToValueMap.containsKey(entry.getEntryId())||idToValueMapBeforeCrosstalkHandling.containsKey(entry.getEntryId())) {
                    pvalues += entry.getSymbol();
                    pvalues += getPValueText(entry, idToValueMap, idToValueMapBeforeCrosstalkHandling);
                }
            }

            
            String colorCode = "#FFFFFF";
            if (entry2ImpactColor.get(entry) != null) {
                switch (entry2ImpactColor.get(entry)) {
                    case 0:
                        colorCode = "#FFFFBB";
                        break;
                    case 1:
                        colorCode = "#FFFF00";
                        break;
                    case 2:
                        colorCode = "#FFE000";
                        break;
                    case 3:
                        colorCode = "#FFC000";
                        break;
                    case 4:
                        colorCode = "#FFA000";
                        break;
                    case 5:
                        colorCode = "#FF8000";
                        break;
                    case 6:
                        colorCode = "#FF6000";
                        break;
                    case 7:
                        colorCode = "#FF4000";
                        break;
                    case 8:
                        colorCode = "#FF2000";
                        break;
                    default:
                        colorCode = "#FF0000";
                }
            }
            
            View<CyNode> nodeView = networkView.getNodeView(cyNode);
            //nodeView.setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR, Color.BLUE);
            //nodeView.setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR, new Color(Integer.parseInt(colorCode, 16)));
            //nodeView.setVisualProperty(BasicVisualLexicon.NODE_SHAPE, "rectangle");
            
            
            String strokeWidth = "1";
            String strokeColor = "grey";
            if (entry2FrequencyColor.get(entry) != null) {
                strokeWidth = "3";
//			strokeWidth=""+(entry2FrequencyColor.get(entry)*2);
//			strokeColor="purple";
                switch (entry2FrequencyColor.get(entry)) {
                    case 1:
                        strokeColor = "FF00FF";
                        break;
                    case 2:
                        strokeColor = "EE00EE";
                        break;
                    case 3:
                        strokeColor = "DD00DD";
                        break;
                    case 4:
                        strokeColor = "CC00CC";
                        break;
                    case 5:
                        strokeColor = "BB00BB";
                        break;
                    case 6:
                        strokeColor = "AA00AA";
                        break;
                    case 7:
                        strokeColor = "990099";
                        break;
                    case 8:
                        strokeColor = "880088";
                        break;
                    case 9:
                        strokeColor = "770077";
                        break;
                    case 10:
                        strokeColor = "660066";
                        break;
                    default:
                        strokeColor = "550055";
                        break;
                }
            }

            cyNodeRow.set("label", label);
            cyNodeRow.set("genesymbol", genesymbol);
            cyNodeRow.set("pvalues", pvalues);
            cyNodeRow.set("bgcolor", colorCode);
            cyNodeRow.set("shape", "rectangle");
            cyNodeRow.set("width", "46");
            cyNodeRow.set("height", "17");
            
            //nodeView.setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR, new Color(Integer.parseInt(colorCode,16)));
            
            
        }
        
        VisualStyle visualStyle=CyActivator.visualStyleFactory.createVisualStyle("dapath");
        Set<VisualStyle> visualStyles = CyActivator.visualMappingManager.getAllVisualStyles();

        
        boolean isVisualStylePresent = false;
        
        for (VisualStyle vs : visualStyles) {
            if (vs.getTitle().equals(visualStyle.getTitle())) {
                isVisualStylePresent = true;
                visualStyle = vs;
            }
        }
        
        
        VisualMappingFunction<String, Paint> nodeColorMapping = CyActivator.visualMappingFunctionFactoryPassthrough.createVisualMappingFunction
            ("bgcolor", String.class, BasicVisualLexicon.NODE_FILL_COLOR);
        VisualMappingFunction<String, NodeShape> nodeShapeMapping = CyActivator.visualMappingFunctionFactoryPassthrough.createVisualMappingFunction
            ("shape", String.class, BasicVisualLexicon.NODE_SHAPE);
        VisualMappingFunction<String, String> nodeLabelMapping = CyActivator.visualMappingFunctionFactoryPassthrough.createVisualMappingFunction
            ("label", String.class, BasicVisualLexicon.NODE_LABEL);
        VisualMappingFunction<String, Double> nodeWidthMapping = CyActivator.visualMappingFunctionFactoryPassthrough.createVisualMappingFunction
            ("width", String.class, BasicVisualLexicon.NODE_WIDTH);
        VisualMappingFunction<String, Double> nodeHeightMapping = CyActivator.visualMappingFunctionFactoryPassthrough.createVisualMappingFunction
            ("height", String.class, BasicVisualLexicon.NODE_HEIGHT);
        
        visualStyle.addVisualMappingFunction(nodeColorMapping);
        visualStyle.addVisualMappingFunction(nodeShapeMapping);
        visualStyle.addVisualMappingFunction(nodeLabelMapping);
        visualStyle.addVisualMappingFunction(nodeWidthMapping);
        visualStyle.addVisualMappingFunction(nodeHeightMapping);
        
        if (!isVisualStylePresent)
            CyActivator.visualMappingManager.addVisualStyle(visualStyle);

        VisualPropertyDependency dependency = null;
        for (VisualPropertyDependency<?> dep : visualStyle.getAllVisualPropertyDependencies()) {
                if (dep.getDisplayName().equalsIgnoreCase("Lock node width and height") ||
                    dep.getDisplayName().equalsIgnoreCase("Lock \nnode width and height"))
                    dependency = dep;
            }
            if (dependency != null && dependency.isDependencyEnabled())
                dependency.setDependency(false);
            visualStyle.addVisualPropertyDependency(dependency);
        
        visualStyle.apply(networkView);
        //CyActivator.visualMappingManager.setVisualStyle(visualStyle, networkView);
        //networkView.updateView();
        //CyActivator.cyDesktopService.getJFrame().repaint();

    }
    
    
    public static void createColumn(CyTable destTable, String columnName, Class classType) {
        boolean containsColumn = false;

        Iterator<CyColumn> destIterator = destTable.getColumns().iterator();
        while (destIterator.hasNext() && containsColumn == false) {
            CyColumn destColumn = destIterator.next();
            if (destColumn.getName().toLowerCase().equals(columnName.toLowerCase())) {
                containsColumn = true;
            }
        }
        if (!containsColumn) {
            destTable.createColumn(columnName, classType, true);
        }
    }
    
//    public static String getPValueText(Entry entry, HashMap<String, Double> idToValueMap, HashMap<String, Double> idToValueMapBeforeCrosstalkHandling) {
//        double p = idToValueMap.get(entry.getEntryId());
//        double pBeforeCrossTalkModification = idToValueMapBeforeCrosstalkHandling.get(entry.getEntryId());
//        String str = "";
//        str += "(" + p + "/" + pBeforeCrossTalkModification + ")";
//        return str;
//    }
    
    public static String getPValueText(Entry entry, HashMap<String, Double> idToValueMap, HashMap<String, Double> idToValueMap2) {
        Double p = idToValueMap.get(entry.getEntryId());
        Double p2 = idToValueMap2.get(entry.getEntryId());
        String str = "";
        if(p!=null || p2!=null){
            str += "(" + p + "/" + p2 + ")";
        }
        return str;
    }
    
}
