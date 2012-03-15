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

package opendap.clients.odc.search;

import opendap.clients.odc.*;
import opendap.clients.odc.data.Model_Dataset;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
//import thredds.catalog.*;
//import thredds.ui.*;

/** THREDDS is disabled for now **/

// http://motherlode.ucar.edu:8080/thredds/topcatalog.xml

public class THREDDSSearch extends SearchInterface {

	private final JTabbedPane tabbedPane = new JTabbedPane();
	private String msCatalogURL;
	Model_Dataset[] mSelectedURLs = null;

    /**
     * Create a new <code>THREDDSSearch</code>
     */
    public THREDDSSearch() {}

	public boolean zInitialize( StringBuffer sbError ){
		try {

			final String sCatalogURL = "http://motherlode.ucar.edu:8080/thredds/topcatalog.xml";

			msCatalogURL = sCatalogURL;

			//add title info
			tabbedPane.setBorder(BorderFactory.createTitledBorder("THREDDS Search"));
			// add tabbed panel and button panel
			setLayout( new BorderLayout() );
			

			sbError.append("not implemented");
			return false;

		} catch( Throwable ex ) {
			JPanel panelError = new JPanel();
			panelError.add(new JTextArea("Error instantiating THREDDS:\n" + Utility.errorExtractStackTrace(ex)));
			this.add(panelError, BorderLayout.CENTER);
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	public Model_Dataset[] getURLs( StringBuffer sbError ){
		return mSelectedURLs;
	}
	public void addListSelectionListener( javax.swing.event.ListSelectionListener listener ){
		// does not do anything
	}

	//  [quote]
	//	When you get an event, you should examine the dataset and decide what to do with it.
	//	You can use a thredds.datamodel.Dataset object to help with this.
	//	Currently this only handles GRID data types with DODS or NETCDF service types.
	//	See the NetCDF-Java User Manual  to see how to manipulate GridDataset objects.

//	private void setDataset(thredds.catalog.InvDataset invDS) {
//		java.util.List listAccess = invDS.getAccess();
//		if( listAccess == null ){
//			// todo error
//			return;
//		}
//		if( listAccess.size() == 0 ){
//			System.err.println("no access"); // todo make regular error
//			return;
//		}
//		String sURL = null;
//		try {
//			for( int xAccess = 0; xAccess < listAccess.size(); xAccess++ ){
//				InvAccess access = (InvAccess)listAccess.get(xAccess);
//				sURL = access.getUrlPath();
//				break;
//			}
//		} catch( Exception ex ) {
//			System.err.println("access error " + ex); // todo make regular error
//			return;
//		}
//		if( sURL == null || sURL.trim().length() == 0 ){
//			System.err.println("no URL for data set"); // todo make regular error
//			return;
//		}
//		DodsURL url = new DodsURL();
//		url.setTitle( invDS.getName() );
//		url.setURL(sURL);
//		mSelectedURLs = new DodsURL[1];
//		mSelectedURLs[0] = url;
//		vAddSelected(); // will trigger fetch of getURLs above
//	}

    public static void main(String args[]) {
		JFrame frame = new JFrame("THREDDS");
		frame.getContentPane().add(new THREDDSSearch());
		frame.pack();
		frame.setVisible(true);
    }
}

// old code using NetCDF library
//			final thredds.catalog.ui.ThreddsDatasetChooser thredds_chooser = new thredds.catalog.ui.ThreddsDatasetChooser( null, tabbedPane);
//			thredds_chooser.addPropertyChangeListener( new java.beans.PropertyChangeListener() {
//				public void propertyChange(java.beans.PropertyChangeEvent e) {
//					if (e.getPropertyName().equals("Dataset")) {
//						thredds.catalog.InvDataset ds = (thredds.catalog.InvDataset)e.getNewValue();
//						setDataset(ds);
//					}
//				}
//			});

			// only allow OPeNDAP data sets
//			thredds_chooser.setDatasetFilter( new DatasetFilter.ByServiceType( ServiceType.OPENDAP));

			// Create and intialize the command panel
//			final JPanel jpanelAccessTHREDDS = new JPanel();
//			final JButton jbuttonAccessTHREDDS = new JButton("Click to \nAccess THREDDS");
//			Styles.vApply( Styles.STYLE_NetworkAction, jbuttonAccessTHREDDS);
//			jbuttonAccessTHREDDS.setMinimumSize(new Dimension(250, 100));
//			jbuttonAccessTHREDDS.setMaximumSize(new Dimension(250, 100));
//			jbuttonAccessTHREDDS.setPreferredSize(new Dimension(250, 100));
//			javax.swing.ImageIcon iiInternetConnectionIcon = Utility.imageiconLoadResource(Resources.ICON_InternetConnection);
//			jbuttonAccessTHREDDS.setIcon(iiInternetConnectionIcon);
//			final ActionListener action =
//				new ActionListener(){
//					public void actionPerformed(ActionEvent event) {
//						Activity activityAccessTHREDDS = new Activity();
//						Continuation_DoCancel con =
//							new Continuation_DoCancel(){
//								public void Do(){
//									THREDDSSearch.this.remove(jpanelAccessTHREDDS);
//									try {
//										add(thredds_chooser, BorderLayout.CENTER);
//									} catch(Exception ex){
//										StringBuffer sb = new StringBuffer(80);
//										Utility.vUnexpectedError(ex, sb);
//										ApplicationController.getInstance().vShowError("Error creating free text search panel: " + sb);
//									}
//									jbuttonAccessTHREDDS.setVisible(false);
//								}
//								public void Cancel(){
//									tabbedPane.removeAll();
//								}
//							};
//						activityAccessTHREDDS.vDoActivity(	jbuttonAccessTHREDDS, this, con, "Accessing THREDDS" );
//					}
//				};
//			jbuttonAccessTHREDDS.addActionListener(action);
//
//			// add button to center of display
//			jpanelAccessTHREDDS.setBorder(javax.swing.BorderFactory.createEmptyBorder());
//			jpanelAccessTHREDDS.setLayout(new java.awt.GridBagLayout());
//			jpanelAccessTHREDDS.add( jbuttonAccessTHREDDS );
//			this.add(jpanelAccessTHREDDS, BorderLayout.CENTER);



