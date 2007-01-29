package opendap.clients.odc;

/**
 * Title:        Panel_Select_Recent
 * Description:  Selection panel showing saved user Recent URL selections
 * Copyright:    Copyright (c) 2002
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.45
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

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class Panel_Select_Recent extends SearchInterface {

    public Panel_Select_Recent() {}

	Panel_URLList_JList mListPanel;
	JScrollPane jscrollpaneList;
	JScrollPane mjscrollInfo;
	Model_URLList mListModel;
	DodsURL[] maURLs = null;
	JTextArea mjtaInfo;

    public boolean zInitialize(StringBuffer sbError){

        try {

			javax.swing.border.Border borderStandard = BorderFactory.createEtchedBorder();
			this.setBorder(borderStandard);

			this.setLayout(new java.awt.BorderLayout());

			// Create and intialize the Recents list
			mListModel = new Model_URLList( false ); // do not do type checking
			mListPanel = new Panel_URLList_JList( mListModel );
			mListModel.setControl( mListPanel );
			// TODO
			// DatasetListRenderer renderer = new DatasetListRenderer(jlistFavorites, false, false);
			// jlistFavorites.setCellRenderer(renderer);
			// mListModel.addListDataListener(renderer);
			jscrollpaneList = new JScrollPane();
			jscrollpaneList.setViewportView(mListPanel);
			vRefreshRecentList();
			jscrollpaneList.setPreferredSize(new java.awt.Dimension(200, 200));

			// Set up info listener
/* TODO
			mjlistRecent.addMouseListener(
				new MouseAdapter(){
				    public void mousePressed( MouseEvent me ){
						if( me.getClickCount() == 1 ){
							JList list = (JList)me.getSource();
							int xItem = list.locationToIndex(me.getPoint());
							if( xItem != -1 ){
								ListModel model = list.getModel();
								Object oItem = model.getElementAt(xItem);
								if( oItem instanceof DodsURL ){
									vShowInfo(((DodsURL)oItem).getInfo());
								}
							}
						} else if( me.getClickCount() == 2 ){
							Panel_Select_Recent.this.vAddSelected();
						}
					}
				}
			);
*/

			// Create and intialize the command panel
			JPanel panelButtons = new JPanel();
			panelButtons.setBorder(new javax.swing.border.EmptyBorder(3, 3, 3, 3));

			// Create the info panel
			mjtaInfo = new JTextArea();
			mjtaInfo.setMargin(new Insets(6, 10, 10, 10));
			mjscrollInfo = new JScrollPane();
			mjscrollInfo.setViewportView(mjtaInfo);
			mjscrollInfo.setPreferredSize(new Dimension(600, 100));

			// Select
			Button_Select jbuttonSelect = new Button_Select(this);
			panelButtons.add( jbuttonSelect );

			this.add(panelButtons, BorderLayout.NORTH);
		    this.add(jscrollpaneList, BorderLayout.CENTER);
			this.add(mjscrollInfo, java.awt.BorderLayout.SOUTH);

            return true;

        } catch(Exception ex){
			Utility.vUnexpectedError(ex, sbError);
            return false;
        }
	}

	void vShowInfo( String sInfo ){
		mjtaInfo.setText(sInfo);
		mjscrollInfo.scrollRectToVisible(new Rectangle(0,0, 1,1));
	}

	void vRefreshRecentList(){
		maURLs = DodsURL.getPreferenceURLs_Recent();
		mListModel.vDatasets_DeleteAll();
		mListModel.vDatasets_Add(maURLs, false);
		return;
	}

	public void addListSelectionListener(ListSelectionListener listener){
// TODO		mjlistRecent.getSelectionModel().addListSelectionListener(listener);
	}

	public DodsURL[] getURLs( StringBuffer sbError ){
		if( mListModel == null ){
		    sbError.append("unable to add selected recent, internal error: no model");
			return null;
		}
	    return mListModel.getSelectedURLs(sbError);
	}

	/** Returns -1 if not found. Returns zero-based index if is found. */
	int xIndexOfMatchingBaseURL( DodsURL url ){
		if( url == null ){
			ApplicationController.getInstance().vShowError("internal error: null supplied to zInList");
			return -1;
		}
		if( maURLs == null ) return -1;
		for( int xURL = 0; xURL < maURLs.length; xURL++ ){
			if( maURLs[xURL] == null ) continue;
			if( url.getBaseURL().equals(maURLs[xURL].getBaseURL()) ) return xURL;
		}
		return -1;
	}

	static void vAddRecent( DodsURL url ){
		StringBuffer sbError = new StringBuffer();
		int iRecentCount = ConfigurationManager.getInstance().getProperty_RecentCount();
		if( iRecentCount < 1 ) return; // no recents at all
		RecentFilter filter = new RecentFilter();
		File[] afile = ConfigurationManager.getPreferencesFiles(filter);
		if( afile == null ){
			ApplicationController.getInstance().vShowError("error adding recent: directory list unexpectedly null");
			return;
		}

		// see if the URL is already present
		Panel_Select_Recent panelRecent = ApplicationController.getInstance().getAppFrame().getPanel_Recent();
		if( panelRecent == null ){
			ApplicationController.getInstance().vShowWarning("error adding recent: recent panel unavailable");
			return;
		}
		int xFileToDelete = panelRecent.xIndexOfMatchingBaseURL(url);
		if( xFileToDelete != - 1 ){ // already in there (replace existing object)
			String sFilename = afile[xFileToDelete].getName();
			if( !Utility.zDeletePreferenceObject(sFilename, sbError) ){
				ApplicationController.getInstance().vShowWarning("failed to delete matching recent: " + sbError);
				return;
			}
		}

		// delete any additional recents necessary to bring list down to recent count
		if( afile.length > iRecentCount-1 ){
			java.util.Arrays.sort(afile);
			int ctToDelete = afile.length - iRecentCount + 1;
			for( xFileToDelete = 0; xFileToDelete < ctToDelete; xFileToDelete++ ){
				String sFilename = afile[xFileToDelete].getName();
				if( !Utility.zDeletePreferenceObject(sFilename, sbError) ){
					ApplicationController.getInstance().vShowWarning("failed to delete extra recent: " + sbError);
				}
			}
		}

		String sFilename;
		if( afile == null ){
			sFilename = "recent-0000001.ser";
		} else {
			if( afile.length < 1 ) {
				sFilename = "recent-0000001.ser";
			} else {
				int iMaxFavoriteNumber = 0;
				for( int xFile = 0; xFile < afile.length; xFile++ ){
					File fileRecent = afile[xFile];
					String sNumber = fileRecent.getName().substring(7, 14); // make more robust
					try {
						int iCurrentNumber = Integer.parseInt(sNumber);
						if( iCurrentNumber > iMaxFavoriteNumber ) iMaxFavoriteNumber = iCurrentNumber;
					} catch(Exception ex) {
						// do nothing
					}
				}
				String sNumberNew = Utility.sFixedWidth(Integer.toString(iMaxFavoriteNumber+1), 7, '0', Utility.ALIGNMENT_RIGHT);
				sFilename = "recent-" + sNumberNew + ".ser";
			}
		}
		if( !Utility.zStorePreferenceObject(url, sFilename, sbError) ){
			ApplicationController.vShowError("Failed to store recent: " + sbError);
		}
	}

}

