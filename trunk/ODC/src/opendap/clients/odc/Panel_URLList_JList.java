package opendap.clients.odc;

/**
 * <p>Title: OPeNDAP Data Connector</p>
 * <p>Description: JList implementation for an URL List</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: OPeNDAP.org</p>
 * @author John Chamberlain
 * @version 2.59
 */

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

import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Panel_URLList_JList extends Panel_URLList {
	private JList mjlist;
    public Panel_URLList_JList( final Model_URL_control control_model ){
		super( control_model );

		this.setLayout( new BorderLayout() );

		// Setup List
		mjlist = new JList();

		ListModel list_model = (ListModel)control_model;

		mjlist.setModel( list_model );

		// set up the cell renderer (adds the folder/granule icons etc)
		DatasetListRenderer renderer = new DatasetListRenderer( mjlist, false, false );
		mjlist.setCellRenderer(renderer);
		list_model.addListDataListener(renderer);

		mjlist.addKeyListener(
			new java.awt.event.KeyListener(){
				public void keyPressed( java.awt.event.KeyEvent ke ){
					if( ke.getKeyCode() == ke.VK_DELETE ){
						int[] aiSelectedToDelete = mjlist.getSelectedIndices();
						control_model.vDatasets_Delete( aiSelectedToDelete );
					}
				}
				public void keyReleased( java.awt.event.KeyEvent ke ){
					// consumed
				}
				public void keyTyped( java.awt.event.KeyEvent ke ){
					// consumed
				}
			}
		);

		mjlist.addMouseListener(
			new MouseAdapter(){
				public void mousePressed( MouseEvent me ){
					if( me.getClickCount() == 1 ){
						Object oSelectedItem = mjlist.getSelectedValue();
						DodsURL urlSelected = null;
						if( oSelectedItem instanceof DodsURL ){
							urlSelected = (DodsURL)oSelectedItem;
						} else {
							return; // there can be entries in the list that are not URLs -- ignore them
						}
						Model_Retrieve retrieve_model = ApplicationController.getInstance().getRetrieveModel();
						if( retrieve_model == null ){
							ApplicationController.getInstance().vShowError( "internal error: retrieval model does not exist for dataset activation" );
							return;
						}
						if( urlSelected == null ){ // can happen because of a bug in the list / table component
							retrieve_model.vClearSelection();
							return;
						}
						if( me.isControlDown() ){
							if( me.isShiftDown() ){
								retrieve_model.vShowDAS( urlSelected, null );
							} else {
								retrieve_model.vShowDDS( urlSelected, null );
							}
						} else {
							if( urlSelected.getType() == DodsURL.TYPE_Data ){
								retrieve_model.getRetrievePanel().vShowDirectory( false );
							}
							retrieve_model.vShowURL( urlSelected, null );
						}
					}
				}
			}
		);

		JScrollPane jscrollpaneList = new JScrollPane();
		jscrollpaneList.setViewportView(mjlist);

		this.add( jscrollpaneList, BorderLayout.CENTER );

	}
	public int getSelectedIndex(){
		if( mjlist == null ) return -1;
		return mjlist.getSelectedIndex();
	}
	public int[] getSelectedIndices(){
		if( mjlist == null ) return null;
		return mjlist.getSelectedIndices();
	}
	public void vSelectIndex( int iIndex ){
		int iListSize = mjlist.getModel().getSize();
		if( iIndex == -1 ){
			mjlist.clearSelection();
		} else if( iIndex < 0 || iIndex >= iListSize ){
			ApplicationController.getInstance().vShowWarning( "attempt to set index (" + iIndex + ") out of range on url list of size " + iListSize);
		} else {
			mjlist.setSelectedIndex( iIndex );
		}
	}
}
