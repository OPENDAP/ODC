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

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

public class Button_Select extends JButton implements ListSelectionListener {
	public Button_Select(final SearchInterface searchable){
		super();
		Styles.vApply( Styles.STYLE_NetworkAction, this );
		setText("To Retrieve");
		setEnabled(false);
		addActionListener(
			new ActionListener(){
	    		public void actionPerformed(ActionEvent event) {
		    		searchable.vAddSelected();
			    }
			}
		);
		searchable.addListSelectionListener( this );
	}
    public void valueChanged(ListSelectionEvent e) {
		Object oSource = e.getSource();
		if( oSource instanceof ListSelectionModel ){
			if( ((ListSelectionModel)e.getSource()).isSelectionEmpty() ){
				setEnabled(false);
			} else {
				setEnabled(true);
			}
		} else if( oSource instanceof JTree ) {
			JTree jtree = (JTree)oSource;
			TreeModel tm = jtree.getModel();
			TreeSelectionModel tsm = jtree.getSelectionModel();
			if( tsm.isSelectionEmpty() ){
				setEnabled(false);
			} else {
				TreePath[] tp = tsm.getSelectionPaths();
				if (tp == null) {
					setEnabled(false);
				} else {
					for (int i=0; i < tp.length; i++) {
						Object oNode = tp[i].getLastPathComponent();
						if( tm.isLeaf(oNode) ){
							setEnabled(true);
							return;
						}
					}
					setEnabled(false);
				}
			}
		}
	}
}



