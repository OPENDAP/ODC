package opendap.clients.odc;

/**
 * <p>Title: OPeNDAP Data Connector</p>
 * <p>Description: table implementation of an url list</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: OPeNDAP.org</p>
 * @author John Chamberlain
 * @version 2.59
 */

import javax.swing.*;
import javax.swing.table.*;

// **** CLASS NOT CURRENTLY IN USE ****

public class Panel_URLList_JTable extends JPanel {
	private JTable mjtable;
	private JTable mjtableSelected;
    public Panel_URLList_JTable( Model_URL_control control_model ){
		super();

		// Setup List
		mjtable = new JTable();
		mjtable.setTableHeader(null);
		mjtable.setRowSelectionAllowed(true);
		mjtable.setShowGrid(true);

		// format columns (table model must exist before column model can be accessed)
		TableColumnModel cmDatasets = mjtableSelected.getColumnModel();
		cmDatasets.getColumn(0).setPreferredWidth(10);
		cmDatasets.getColumn(0).setMaxWidth(10);
		cmDatasets.getColumn(1).setPreferredWidth(250);

		// setup info column
		TableColumn tcInfo = mjtableSelected.getColumn("info");
		ButtonEditor buttonEditorInfo = new ButtonEditor();
		Continuation_DoCancel conInfo = new Continuation_DoCancel(){
			public void Do(){
				int iClickedRow = mjtableSelected.getSelectedRow();
				// model.vShowURLInfo(iClickedRow); TODO NO LONGER WORKING SINCE REWORK
			}
			public void Cancel(){}
		};
		buttonEditorInfo.vInitialize("?", conInfo);
		tcInfo.setCellEditor(buttonEditorInfo);
		tcInfo.setCellRenderer(buttonEditorInfo.getRenderer());

		final ListSelectionModel selectionmodel = mjtableSelected.getSelectionModel();

		// setup title column
		TableColumn tcTitle = mjtableSelected.getColumn("title");
		tcTitle.setCellRenderer(new DatasetListRenderer(mjtableSelected, false, true));

		mjtableSelected.addKeyListener(
			new java.awt.event.KeyListener(){
				public void keyPressed( java.awt.event.KeyEvent ke ){
					if( ke.getKeyCode() == ke.VK_DELETE ){
						int[] aiSelectedToDelete = mjtableSelected.getSelectedRows();
						// NO LONGER FUNCTIONAL -- ApplicationController.getInstance().getRetrieveModel().getURLList().vSelectedDatasets_Remove(aiSelectedToDelete);
					} else if( ke.getKeyCode() == ke.VK_F3 ){
						ApplicationController.getInstance().getRetrieveModel().vEnterURLByHand(null);
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

		this.add( this.mjtable );

	}
}
