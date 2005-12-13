package opendap.clients.odc;

/**
 * Title:        Panel_Select_Recent
 * Description:  Selection panel showing saved user Recent URL selections
 * Copyright:    Copyright (c) 2002
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.45
 */

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;

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
		maURLs = Utility.getPreferenceURLs_Recent();
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

}

