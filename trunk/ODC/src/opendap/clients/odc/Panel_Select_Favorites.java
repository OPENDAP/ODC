package opendap.clients.odc;

/**
 * Title:        Panel_Select_Favorites
 * Description:  Selection panel showing saved user favorite URL selections
 * Copyright:    Copyright (c) 2002
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.57
 */

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class Panel_Select_Favorites extends SearchInterface {

    public Panel_Select_Favorites() {}

	Panel_URLList_JList mListPanel;
	JScrollPane jscrollpaneList;
	Model_URLList mListModel;
	JTextArea mjtaInfo;
	JScrollPane mjscrollInfo;

	int[] maiSelected;

    public boolean zInitialize(StringBuffer sbError){

        try {

			javax.swing.border.Border borderStandard = BorderFactory.createEtchedBorder();
			setBorder(borderStandard);

			setLayout(new java.awt.BorderLayout());

			// Create and intialize the favorites list
			jscrollpaneList = new JScrollPane();
			mListModel = new Model_URLList( false ); // do not do type checking
			mListPanel = new Panel_URLList_JList( mListModel );
			mListModel.setControl( mListPanel );
// TODO
//			DatasetListRenderer renderer = new DatasetListRenderer(jlistFavorites, false, false);
//			jlistFavorites.setCellRenderer(renderer);
//			mListModel.addListDataListener(renderer);
			jscrollpaneList.setViewportView(mListPanel);
			vRefreshFavoritesList();
			jscrollpaneList.setPreferredSize(new java.awt.Dimension(200, 200));

			// Set up info listener
/* TODO
			mListPanel.addMouseListener(
				new MouseAdapter(){
				    public void mousePressed( MouseEvent me ){
						maiSelected = mListPanel.getSelectedIndices();
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
							Panel_Select_Favorites.this.vAddSelected();
						}
					}
				}
			);
*/

			// Create and intialize the command panel
			JPanel jpanelCommand = new JPanel();
			jpanelCommand.setLayout(new BorderLayout());

			// Create the info panel
			mjtaInfo = new JTextArea();
			mjtaInfo.setMargin(new Insets(6, 10, 10, 10));
			mjscrollInfo = new JScrollPane();
			mjscrollInfo.setViewportView(mjtaInfo);
			mjscrollInfo.setPreferredSize(new Dimension(600, 100));

			JPanel panelButtons = new JPanel();
			panelButtons.setBorder(new javax.swing.border.EmptyBorder(3, 3, 3, 3));

			// Select
			Button_Select jbuttonSelect = new Button_Select(this);
			panelButtons.add( jbuttonSelect );

			// Remove all selected data sets
			JButton jbuttonRemoveSelected = new JButton("Remove Selected");
			jbuttonRemoveSelected.addActionListener(
				new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					Panel_Select_Favorites.this.vRemoveSelected(false);
					}
				}
			);
			panelButtons.add( jbuttonRemoveSelected );

			// Remove all data sets
			JButton jbuttonRemoveAll = new JButton("Remove All");
			jbuttonRemoveAll.addActionListener(
				new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					Panel_Select_Favorites.this.vRemoveAll();
					}
				}
			);
			panelButtons.add( jbuttonRemoveAll );
			jpanelCommand.add( panelButtons, java.awt.BorderLayout.NORTH );

			this.add(jpanelCommand, BorderLayout.NORTH);
			this.add(jscrollpaneList, java.awt.BorderLayout.CENTER);
			this.add(mjscrollInfo, BorderLayout.SOUTH);

            return true;

        } catch(Exception ex){
			Utility.vUnexpectedError(ex, sbError);
            return false;
        }
	}

	public void addListSelectionListener( ListSelectionListener listener ){
		// TODO
	}

	void vRefreshFavoritesList(){
		DodsURL[] maURLs = DodsURL.getPreferenceURLs_Favorites();
		mListModel.vDatasets_DeleteAll();
		if( maURLs != null && maURLs.length > 0 ) mListModel.vDatasets_Add(maURLs, false);
		mListPanel.repaint();
		return;
	}

	public DodsURL[] getURLs( StringBuffer sbError ){
		if( mListModel == null ){
		    sbError.append("unable to add selected, internal error: no model");
			return null;
		}
		return mListModel.getSelectedURLs(sbError);
	}

	void vRemoveSelected( boolean zRemoveAll ){
		StringBuffer sbError = new StringBuffer(250);
		DodsURL[] aurlSelected = mListModel.getSelectedURLs( sbError );
		if( aurlSelected == null ) return; // forget about error
		int ctRemovals = aurlSelected.length;
		for( int xSelected = 0; xSelected < ctRemovals; xSelected++ ){
			DodsURL urlCurrent = aurlSelected[xSelected];
			if( urlCurrent == null ){
				ApplicationController.vShowWarning("element at " + xSelected + " unexpectedly null");
				continue;
			}
			int iID = urlCurrent.getID();
			if( iID == 0 ){
				ApplicationController.vShowWarning("element at " + xSelected + " had no ID");
				continue;
			}
			if( zDeleteFavorite(iID, sbError) ){
				ApplicationController.vShowStatus("Deleted favorite ID number " + iID);
			} else {
				ApplicationController.vShowError("Error deleting favorite ID number " + iID + ": " + sbError);
			}
		}
		try { Thread.sleep(500); }  catch(Exception ex) {}
		vRefreshFavoritesList();
	}

	void vRemoveAll(){
		vRemoveSelected(true);
	}

	void vShowInfo( String sInfo ){
		mjtaInfo.setText(sInfo);
		mjscrollInfo.scrollRectToVisible(new Rectangle(0,0, 1,1));
	}

	static void vAddFavorite( DodsURL url ){
		FavoritesFilter filter = new FavoritesFilter();
		File[] afile = ConfigurationManager.getPreferencesFiles(filter);
		String sFilename;
		if( afile == null ){
			sFilename = "favorite-0000001.ser";
		} else {
			if( afile.length < 1 ) {
				sFilename = "favorite-0000001.ser";
			} else {
				int iMaxFavoriteNumber = 0;
				for( int xFile = 0; xFile < afile.length; xFile++ ){
					File fileFavorite = afile[xFile];
					String sNumber = fileFavorite.getName().substring(9, 16); // make more robust
					try {
						int iCurrentNumber = Integer.parseInt(sNumber);
						if( iCurrentNumber > iMaxFavoriteNumber ) iMaxFavoriteNumber = iCurrentNumber;
					} catch(Exception ex) {
						// do nothing
					}
				}
				String sNumberNew = Utility.sFixedWidth(Integer.toString(iMaxFavoriteNumber+1), 7, '0', Utility.ALIGNMENT_RIGHT);
				sFilename = "favorite-" + sNumberNew + ".ser";
			}
		}
		StringBuffer sbError = new StringBuffer();
		if( !Utility.zStorePreferenceObject(url, sFilename, sbError) ){
			ApplicationController.vShowError("Failed to store favorite: " + sbError);
		}
	}

	static boolean zDeleteFavorite( int iID, StringBuffer sbError ){
		try {
			String sNumber = Utility.sFixedWidth(Integer.toString(iID), 7, '0', Utility.ALIGNMENT_RIGHT);
			String sFilename = "favorite-" + sNumber + ".ser";
			return Utility.zDeletePreferenceObject(sFilename, sbError);
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

}

