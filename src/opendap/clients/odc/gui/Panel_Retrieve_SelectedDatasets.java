package opendap.clients.odc.gui;

/**
 * Title:        Panel_Retrieve_SelectedDatasets
 * Description:  Displays currently selected datasets
 * Copyright:    Copyright (c) 2002-4
 * Company:      OPeNDAP
 * @author       John Chamberlain
 * @version      2.48
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.Continuation_DoCancel;
import opendap.clients.odc.Model;
import opendap.clients.odc.data.Model_Dataset;
import opendap.clients.odc.data.Model_Retrieve;
import opendap.clients.odc.data.Model_URLList;

public class Panel_Retrieve_SelectedDatasets extends JPanel {

	Panel_Retrieve_SelectedDatasets() {}

	Model_Retrieve model;

	JSplitPane msplitpane_ListDir;

	private JPanel                   mPanel_List;
	private Panel_Retrieve_Directory mPanel_Directory;

	Panel_Retrieve_Directory getPanelDirectory(){ return mPanel_Directory; }

    boolean zInitialize( StringBuffer sbError ){

        try {
			setToolTipText( "Retrieve Selected Datasets Panel" );

			model = Model.get().getRetrieveModel();
			if( model == null ){
				sbError.append("no model");
				return false;
			}

			// establish model
//			Model_URLTable theListModel = new Model_URLTable(mjtableSelected, false, true, true); // old methodology
			final Model_URLList theListModel = Model.get().getRetrieveModel().getURLList();
			if( theListModel == null ){ sbError.append("internal error: retrieve model does not exist"); return false; }

			// determine mouse behavior of the URL list in the retrieve panel
			MouseAdapter mouse_behavior = new MouseAdapter(){
				public void mousePressed( MouseEvent me ){
					if( me.getClickCount() == 1 ){
						Object oSelectedItem = theListModel.getSelectedURL();
						Model_Dataset urlSelected = null;
						if( oSelectedItem instanceof Model_Dataset ){
							urlSelected = (Model_Dataset)oSelectedItem;
						} else {
							return; // there can be entries in the list that are not URLs -- ignore them
						}
						Model_Retrieve retrieve_model = Model.get().getRetrieveModel();
						if( retrieve_model == null ){
							ApplicationController.vShowError( "internal error: retrieval model does not exist for dataset activation" );
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
							switch( me.getModifiers() ){
								case InputEvent.BUTTON1_MASK:{  // left button
									if( urlSelected.getType() == opendap.clients.odc.data.Model_Dataset.DATASET_TYPE.Data ){
										retrieve_model.getRetrievePanel().vShowDirectory( false );
									}
									retrieve_model.vShowURL( urlSelected, null );
									break;
								}
								case InputEvent.BUTTON2_MASK: { break; } // middle button
								case InputEvent.BUTTON3_MASK: {          // right button
									String s = (String)JOptionPane.showInputDialog(
											ApplicationController.getInstance().getAppFrame(),
											"Press Ok to change URL. This is the base URL. It should not have a type extension (.dds .das .info etc) and should have no constraint.",
											"Dataset Base URL",
											JOptionPane.PLAIN_MESSAGE,
											null,                         // no icon
											null,                         // no choice list
											urlSelected.getBaseURL() );
									if( (s != null) && (s.length() > 0) ){
										urlSelected.setURL( s );
										if( urlSelected.getType() == opendap.clients.odc.data.Model_Dataset.DATASET_TYPE.Data ){
											retrieve_model.getRetrievePanel().vShowDirectory( false );
										}
										retrieve_model.vShowURL( urlSelected, null );
										break;
									}
								}
							}
						}
					}
				}
			};

			final Panel_URLList theListPanel = new Panel_URLList_JList( theListModel, mouse_behavior );
			theListModel.setControl( theListPanel );

			// Remove all selected data sets
			JButton jbuttonRemoveSelected = new JButton("Remove Selected");
			jbuttonRemoveSelected.addActionListener(
				new ActionListener(){
				    public void actionPerformed(ActionEvent event) {
						theListModel.vDatasets_DeleteSelected();
					}
				}
			);

			// Remove all data sets
			JButton jbuttonRemoveAll = new JButton("Remove All");
			jbuttonRemoveAll.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						theListModel.vDatasets_DeleteAll();
					}
				}
			);

			// Add to the favorites list
			JButton jbuttonFavorite = new JButton("Favorite");
			jbuttonFavorite.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						StringBuffer sbError_favorite = new StringBuffer(80);
						Model_Dataset[] aSelectedURLs = theListModel.getSelectedURLs( sbError_favorite );
						if( aSelectedURLs == null ){
							if( sbError_favorite.length() > 0 ){
								ApplicationController.vShowError( sbError_favorite.toString() );
							}
							return;
						}
		    			theListModel.vAddToFavorites(aSelectedURLs);
					}
				}
			);

			boolean zReadOnly = ConfigurationManager.getInstance().getProperty_MODE_ReadOnly();

			// layout button panel
			JPanel panelRetrieveButtons = new JPanel();
			panelRetrieveButtons.setLayout(new javax.swing.BoxLayout(panelRetrieveButtons, BoxLayout.X_AXIS));
			panelRetrieveButtons.add(jbuttonRemoveSelected);
			panelRetrieveButtons.add(jbuttonRemoveAll);
			if( zReadOnly ){
				// do not allow saving to favorites
			} else {
				panelRetrieveButtons.add(jbuttonFavorite);
			}

			// layout list panel
			mPanel_List = new JPanel();
			mPanel_List.setLayout(new BorderLayout());
			mPanel_List.add(theListPanel, BorderLayout.CENTER);
			mPanel_List.add(panelRetrieveButtons, BorderLayout.SOUTH);

			// layout list panel - grid method not working for some reason
/*			mPanel_List = new JPanel();
			mPanel_List.setLayout(new java.awt.GridBagLayout());
			mPanel_List.setBorder(Styles.BORDER_Blue);
			java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
			gbc.fill = java.awt.GridBagConstraints.BOTH;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = zReadOnly ? 2 : 3;
			gbc.weightx = 1;
			gbc.weighty = .99;
			mPanel_List.add(theListPanel, gbc);
			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.gridwidth = 1;
			gbc.weightx = .25;
			gbc.weighty = .01;
			mPanel_List.add(jbuttonRemoveSelected, gbc);
			gbc.gridx = 1;
			mPanel_List.add(jbuttonRemoveAll, gbc);
			if( zReadOnly ){
				// do not allow saving to favorites
			} else {
				gbc.gridx = 2;
				mPanel_List.add(jbuttonFavorite, gbc);
			}
*/

			// Directory Panel
			mPanel_Directory = new Panel_Retrieve_Directory();
			if( !mPanel_Directory.zInitialize( this, sbError ) ){
				sbError.insert(0, "Failed to initialize directory panel");
				return false;
			}
			Border borderEmpty = new EmptyBorder(0, 0, 0, 0);
			mPanel_Directory.setBorder(borderEmpty);

			msplitpane_ListDir = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
			msplitpane_ListDir.setTopComponent( mPanel_List );
			msplitpane_ListDir.setBottomComponent( mPanel_Directory );
			msplitpane_ListDir.setContinuousLayout(true);

			setLayout(new BorderLayout());
			add(msplitpane_ListDir, BorderLayout.CENTER);

			SwingUtilities.invokeLater(
				new Runnable(){
					public void run(){
						msplitpane_ListDir.setDividerLocation(0.20);
					}
				}
			);

            return true;

        } catch(Exception ex){
			ApplicationController.vUnexpectedError(ex, sbError);
            return false;
        }
	}

	void vShowDirectory( boolean zShow ){
		if( zShow ){
			mPanel_Directory.setVisible(true);
			msplitpane_ListDir.setDividerLocation(0.20d);
		} else {
			mPanel_Directory.setVisible(false);
		}
	}

	void vClear(){
		mPanel_Directory.setVisible(false);
	}

}

class ButtonEditor extends AbstractCellEditor {
	ButtonRenderer mrenderer;
	public ButtonEditor() {}
	void vInitialize( String sButtonText, final Continuation_DoCancel con ){
		mrenderer = new ButtonRenderer();
		mrenderer.vInitialize(sButtonText, con);
		mrenderer.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					try {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								fireEditingStopped();
								con.Do();
							}
						});
					} catch(Exception ex) {}
				}
			});
	}
	public Component getTableCellEditorComponent(
									JTable table, Object value,
									boolean isSelected,
									int row, int col) {
		return mrenderer.getTableCellRendererComponent(
							  table, value, true, true,
							  row, col);
	}
	TableCellRenderer getRenderer(){
		return mrenderer;
	}
}

class ButtonRenderer extends JLabel implements TableCellRenderer {
	public ButtonRenderer(){}
	public Component getTableCellRendererComponent(
		JTable table, Object value,
		boolean isSelected,
		boolean hasFocus,
		int row, int col) {
		return this;
	}
	void vInitialize( String sText, final Continuation_DoCancel con ){
		this.setText(sText);
		this.setBorder(new javax.swing.border.LineBorder(Color.red, 1, true));
		this.setBackground(Color.white);
		this.setForeground(Color.red);
		this.setHorizontalAlignment(JLabel.CENTER);
	}
}



