package org.dapath.internal;
import org.cytoscape.work.TaskFactory;


import java.util.Properties;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.task.edit.MapTableToNetworkTablesTaskFactory;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {
    public static BundleContext context;
    private static CyAppAdapter appAdapter;
    public CyApplicationManager cyApplicationManager;
    public static CySwingApplication cyDesktopService;
    public CyServiceRegistrar cyServiceRegistrar;
    public ProjectMenuAction menuaction;
    public static CyNetworkFactory networkFactory;
    public static CyNetworkManager networkManager;
    public static CyNetworkViewFactory networkViewFactory;
    public static CyNetworkViewManager networkViewManager;
    public static CyNetworkNaming networkNaming;
    public static VisualStyleFactory visualStyleFactory;
    public static VisualMappingFunctionFactory visualMappingFunctionFactoryPassthrough;
    public static VisualMappingManager visualMappingManager;
    public static VisualMappingFunctionFactory visualMappingFunctionFactoryContinuous;
    public static VisualMappingFunctionFactory visualMappingFunctionFactoryDiscrete;
    public static CySwingAppAdapter adapter;
    
    @Override
    public void start(BundleContext context) throws Exception {
        String version = new String("1.0");
        CyActivator.context=context;
        appAdapter = getService(context, CyAppAdapter.class);
        networkViewManager = getService(context, CyNetworkViewManager.class);
        networkViewFactory = getService(context, CyNetworkViewFactory.class);
        networkFactory = getService(context, CyNetworkFactory.class);
        networkManager = getService(context, CyNetworkManager.class);
        networkNaming = getService(context, CyNetworkNaming.class);
        this.cyApplicationManager = getService(context, CyApplicationManager.class);
        this.cyDesktopService = getService(context, CySwingApplication.class);
        this.cyServiceRegistrar = getService(context, CyServiceRegistrar.class);
        visualStyleFactory = getService(context,VisualStyleFactory.class);
        visualMappingFunctionFactoryPassthrough = getService(context,VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
        visualMappingManager = getService(context,VisualMappingManager.class);
        visualMappingFunctionFactoryContinuous = getService(context,VisualMappingFunctionFactory.class, "(mapping.type=continuous)");
        visualMappingFunctionFactoryDiscrete = getService(context,VisualMappingFunctionFactory.class, "(mapping.type=discrete)");
        adapter = getService(context,CySwingAppAdapter.class);
  
        menuaction = new ProjectMenuAction(cyApplicationManager, "DAPath " + version, this);
        //SpanningTreeStartMenu panel = new SpanningTreeStartMenu(this);
        //registerService(context, panel, CytoPanelComponent.class, new Properties());
        CyTableFactory tableFactory = getService(context,CyTableFactory.class);
	MapTableToNetworkTablesTaskFactory mapTableToNetworkTablesTaskFactory = getService(context,MapTableToNetworkTablesTaskFactory.class);
	CreateTableTaskFactory createTableTaskFactory = new CreateTableTaskFactory(tableFactory,mapTableToNetworkTablesTaskFactory);
        Properties createTableTaskFactoryProps = new Properties();
	createTableTaskFactoryProps.setProperty("preferredMenu","Apps");
	createTableTaskFactoryProps.setProperty("title","Create Table");
	registerService(context,createTableTaskFactory,TaskFactory.class, createTableTaskFactoryProps);
        registerAllServices(context, menuaction, new Properties());
    }

    public CyServiceRegistrar getcyServiceRegistrar() {
        return cyServiceRegistrar;
    }

    public CyApplicationManager getcyApplicationManager() {
        return cyApplicationManager;
    }
    public CyNetworkManager getcyNetworkManager() {
        return networkManager;
    }
    public CyNetworkFactory getcyNetworkFactory() {
        return networkFactory;
    }
    public CySwingApplication getcytoscapeDesktopService() {
        return cyDesktopService;
    }

    public ProjectMenuAction getmenuaction() {
        return menuaction;
    }
    
    public static CyAppAdapter getCyAppAdapter(){
        return appAdapter;
    }

    CyNetworkManager getCyNetworkManager() {
        return networkManager;
    }

    CyNetworkFactory getCyNetworkFactory() {
        return networkFactory;
    }
    
}
