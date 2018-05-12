package org.dapath.internal;

import java.util.Properties;
import java.util.Random;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;

/**
 *
 * @author smd.faizan@gmail.com
 */
public class ProjectCore {

    public static CyNetwork network;
    public CyNetworkView view;
    public CyApplicationManager cyApplicationManager;
    public CySwingApplication cyDesktopService;
    public CyServiceRegistrar cyServiceRegistrar;
    public CyActivator cyactivator;
    public CyNetworkFactory cyNetworkFactory;
    public CyNetworkManager cyNetworkManager;
    // random to be used throughout the app, so to avoid seed repetition
    public Random random;
    public static ProjectStartMenu startmenu;

    public ProjectCore(CyActivator cyactivator) {
        this.cyactivator = cyactivator;
        this.cyApplicationManager = cyactivator.cyApplicationManager;
        this.cyDesktopService = cyactivator.cyDesktopService;
        this.cyServiceRegistrar = cyactivator.cyServiceRegistrar;
        network = cyApplicationManager.getCurrentNetwork();
        view = cyApplicationManager.getCurrentNetworkView();
        cyNetworkFactory = cyactivator.getCyNetworkFactory();
        cyNetworkManager = cyactivator.getCyNetworkManager();
        startmenu = createStartMenu();
        random = new Random();
        registerServices();
        updatecurrentnetwork();
    }

    public void updatecurrentnetwork() {
        //get the network view object
        if (view == null) {
            view = null;
            network = null;
        } else {
            view = cyApplicationManager.getCurrentNetworkView();
            //get the network object; this contains the graph  
            network = view.getModel();
        }
    }

    public void closecore() {
        network = null;
        view = null;
    }

    public ProjectStartMenu createStartMenu() {
        ProjectStartMenu pstartmenu = new ProjectStartMenu(cyactivator);
        cyServiceRegistrar.registerService(pstartmenu, CytoPanelComponent.class, new Properties());
        CytoPanel cytopanelwest = cyDesktopService.getCytoPanel(CytoPanelName.WEST);
        int index = cytopanelwest.indexOfComponent(pstartmenu);
        cytopanelwest.setSelectedIndex(index);
        return pstartmenu;
    }

    public void closeStartMenu() {
        cyServiceRegistrar.unregisterService(startmenu, CytoPanelComponent.class);
    }

    public CyApplicationManager getCyApplicationManager() {
        return this.cyApplicationManager;
    }

    public CySwingApplication getCyDesktopService() {
        return this.cyDesktopService;
    }
    
    public static ProjectStartMenu getStartMenu(){
        return startmenu;
    }
    public CyNetwork getCurrentnetwork() {
        return network;
    }
    public void updateCurrentNetworks(){
        network = cyApplicationManager.getCurrentNetwork();
        view = cyApplicationManager.getCurrentNetworkView();
    }
    
    public CyNetworkView getCurrentnetworkView() {
        return view;
    }
    
    void registerServices(){
        // changeEdgeAttributeListener = new ChangeEdgeAttributeListener();
        //cyactivator.cyServiceRegistrar.registerService(changeEdgeAttributeListener, SetCurrentNetworkListener.class, new Properties());
    }
    
}
