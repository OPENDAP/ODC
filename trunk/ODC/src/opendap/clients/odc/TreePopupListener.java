/////////////////////////////////////////////////////////////////////////////
// This file is part of the OPeNDAP Data Connector project.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

package opendap.clients.odc;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.event.*;

class TreePopupListener extends MouseAdapter {
    private JTree tree;
    private JPopupMenu leafPopup;
    private JPopupMenu nodePopup;

    public TreePopupListener(JTree listenTo, JPopupMenu leafMenu, JPopupMenu nodeMenu) {
        tree = listenTo;
	leafPopup = leafMenu;
        nodePopup = nodeMenu;
    }

    public void mousePressed(MouseEvent e) {
	maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
	maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
	if (e.isPopupTrigger()) {
            TreePath t = tree.getPathForLocation(e.getX(), e.getY());
	    if(!tree.isPathSelected(t))
                tree.setSelectionPath(t);
            if(t != null) {
                DefaultMutableTreeNode n = (DefaultMutableTreeNode)t.getLastPathComponent();
                if(n.isLeaf())
                    leafPopup.show(e.getComponent(), e.getX(), e.getY());
                else if(n.getUserObject() instanceof DodsURL)
                    nodePopup.show(e.getComponent(), e.getX(), e.getY());
            }
	}
    }

}


