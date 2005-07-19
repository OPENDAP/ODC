package opendap.clients.odc;

/**
 * Title:        Panel_Retrieve_SelectedDatasets
 * Description:  Displays currently selected datasets
 * Copyright:    Copyright (c) 2002-4
 * Company:      OPeNDAP
 * @author       John Chamberlain
 * @version      2.48
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.border.*;

public class Panel_Retrieve_SelectedDatasets extends JPanel {

	Panel_Retrieve_SelectedDatasets() {}

	Model_Retrieve model;

	JSplitPane msplitpane_ListDir;

	private JPanel                   mPanel_List;
	private Panel_Retrieve_Directory mPanel_Directory;

	Panel_Retrieve_Directory getPanelDirectory(){ return mPanel_Directory; }

    boolean zInitialize(StringBuffer sbError){

        try {

			model = ApplicationController.getInstance().getRetrieveModel();
			if( model == null ){
				sbError.append("no model");
				return false;
			}

			// establish model
//			Model_URLTable theListModel = new Model_URLTable(mjtableSelected, false, true, true);
			final Model_URLList theListModel = new Model_URLList( true );
			final Panel_URLList theListPanel = new Panel_URLList_JList( theListModel );
			theListModel.setControl( theListPanel );
			model.setURLList( theListModel );

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
						DodsURL[] aSelectedURLs = theListModel.getSelectedURLs( sbError_favorite );
						if( aSelectedURLs == null ){
							if( sbError_favorite.length() > 0 ){
								ApplicationController.getInstance().vShowError( sbError_favorite.toString() );
							}
							return;
						}
		    			theListModel.vAddToFavorites(aSelectedURLs);
					}
				}
			);

			javax.swing.ImageIcon imageInternet = Utility.imageiconLoadResource("icons/internet-connection-icon.gif");

			// layout list panel
			mPanel_List = new JPanel();
			mPanel_List.setLayout(new java.awt.GridBagLayout());
			java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
			gbc.fill = java.awt.GridBagConstraints.BOTH;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = 4;
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
			if( ConfigurationManager.getInstance().getProperty_MODE_ReadOnly() ){
				// do not allow saving to favorites
			} else {
				gbc.gridx = 2;
				mPanel_List.add(jbuttonFavorite, gbc);
			}

			// Directory Panel
			mPanel_Directory = new Panel_Retrieve_Directory();
			if( !mPanel_Directory.zInitialize( this, sbError ) ){
				sbError.insert(0, "Failed to initialize directory panel");
				return false;
			}
			Border borderEmpty = new EmptyBorder(0, 0, 0, 0);
			mPanel_Directory.setBorder(borderEmpty);

			msplitpane_ListDir = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
			msplitpane_ListDir.setTopComponent(mPanel_List);
			msplitpane_ListDir.setBottomComponent(mPanel_Directory);
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
			Utility.vUnexpectedError(ex, sbError);
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



