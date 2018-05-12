package org.dapath.internal;

import java.awt.event.ActionEvent;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;

/**
 * Creates a new menu item under Apps menu section.
 *
 */
public class ProjectMenuAction extends AbstractCyAction {
    
    private CyActivator cyactivator;

    public ProjectMenuAction(CyApplicationManager cyApplicationManager, final String menuTitle, CyActivator cyactivator) {
        super(menuTitle, cyApplicationManager, null, null);
        setPreferredMenu("Apps");
        this.cyactivator = cyactivator;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Starting menu in control panel");
        new ProjectCore(cyactivator);
    }
}
