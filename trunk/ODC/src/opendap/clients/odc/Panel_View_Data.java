/////////////////////////////////////////////////////////////////////////////
// This file is part of the OPeNDAP Data Connector project.
//
// Copyright (c) 2007-8 OPeNDAP, Inc.
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

import opendap.dap.BaseType;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

public class Panel_View_Data extends JPanel implements IControlPanel {	
	Panel_LoadedDatasets panelLoadedDatasets = new Panel_LoadedDatasets();
	Panel_StructureView panelStructureView = new Panel_StructureView();
	Panel_VarView panelVarView = new Panel_VarView();	
    public Panel_View_Data() {}
	public boolean _zInitialize( Model_LoadedDatasets data_list, StringBuffer sbError ){
		try {

			Model_DataView model = new Model_DataView();
			if( ! model.zInitialize( data_list, sbError ) ){
				sbError.insert(0, "failed to initialize model: ");
				return false;
			}
			if( ! panelLoadedDatasets.zInitialize( model, sbError ) ){
				sbError.insert(0, "failed to initialize loaded datasets panel: ");
				return false;
			}
			JPanel panelTop = new JPanel();
			panelTop.setLayout( new BorderLayout() );
			panelTop.add( panelLoadedDatasets, BorderLayout.NORTH );
			panelTop.add( panelStructureView, BorderLayout.CENTER );
			JSplitPane jsplitTreeVar = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
			jsplitTreeVar.setTopComponent( panelTop );
			jsplitTreeVar.setBottomComponent( panelVarView );
			this.add( jsplitTreeVar );
		} catch( Exception ex ) {
			return false;
		}
		return true;
	}
	
	public void _showVariable( BaseType bt ){
	}
	
	public void vSetFocus(){    	
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				panelLoadedDatasets.requestFocus();
			}
		});
	}
	
}

class Model_DataView {
	int ctNewDatasets = 0;
	Model_LoadedDatasets mDatasetList = null;
	boolean zInitialize( Model_LoadedDatasets data_list, StringBuffer sbError ){
		if( data_list == null ){
			sbError.append("dataset list not supplied");
			return false;
		}
		mDatasetList = data_list;
		return true;
	}
	void action_New(){
		try {
			String sServerVersion = "2.1.5";
			int iHeaderType = opendap.dap.ServerVersion.XDAP; // this is the simple version (eg "2.1.5"), server version strings are like "XDODS/2.1.5", these are only used in HTTP, not in files
			opendap.dap.ServerVersion server_version = new opendap.dap.ServerVersion( sServerVersion, iHeaderType );
			opendap.dap.DataDDS datadds = new opendap.dap.DataDDS( server_version );
			ctNewDatasets++;
			String sName = "data" + ctNewDatasets;
			datadds.setName( sName );
			Model_Dataset model = new Model_Dataset();
			model.setTitle( sName );
			mDatasetList.addDataset( model );
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "while trying to create new dataset: " );
		}
	}
	void action_Load(){
	}
	void action_Unload(){
	}
	void action_Save(){
	}
	void action_SaveAs(){
	}
}

class Panel_LoadedDatasets extends JPanel {
	boolean zInitialize( final Model_DataView model, final StringBuffer sbError ){

		if( model == null ){
			sbError.append("no model supplied");
			return false;
		}

		// create controls
		JComboBox jcbLoadedVariables = new JComboBox( model.mDatasetList );
		JButton buttonNew = new JButton( "New" );
		JButton buttonLoad = new JButton( "Load" );
		JButton buttonUnload = new JButton( "Unload" );
		JButton buttonSave = new JButton( "Save" );
		JButton buttonSaveAs = new JButton( "Save as..." );

		model.mDatasetList.addListDataListener( jcbLoadedVariables );
		
		// layout controls
		this.setLayout( new BoxLayout(this, BoxLayout.X_AXIS) );
		this.add( jcbLoadedVariables );
		this.add( buttonNew );
		this.add( buttonLoad );
		this.add( buttonUnload );
		this.add( buttonSave );
		this.add( buttonSaveAs );

		buttonNew.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed( ActionEvent event) {
					model.action_New();
				}
			}
		);
		buttonLoad.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed(ActionEvent event) {
					model.action_Load();
				}
			}
		);
		buttonUnload.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed(ActionEvent event) {
					model.action_Unload();
				}
			}
		);
		buttonSave.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed(ActionEvent event) {
					model.action_Save();
				}
			}
		);
		buttonSaveAs.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed(ActionEvent event) {
					model.action_SaveAs();
				}
			}
		);

		return true;
	}
}

class Panel_StructureView extends JPanel {
	private Model_DataTree mTreeModel = null;
	private JScrollPane mscrollpane_DataTree;
	private Panel_View_Data mParent;
	private Dimension dimMinimum = new Dimension(100, 80);
	private JTree mtreeData = null;
	public Dimension getMinimumSize(){
		return dimMinimum;
	}
	void setModel( Model_DataTree model ){
		mTreeModel = model;
		if( mtreeData != null ) mtreeData.setModel( mTreeModel );
	}
	boolean zInitialize( Panel_View_Data parent, StringBuffer sbError ){
		try {
			mParent = parent;

			Border borderEtched = BorderFactory.createEtchedBorder();

			// tree
			mtreeData = new JTree();
			TreeCellRenderer rendererLeaflessTreeCell = new LeaflessTreeCellRenderer();
			mtreeData.setCellRenderer(rendererLeaflessTreeCell);
			mtreeData.setMinimumSize(new Dimension(60, 60));

			// scroll panes
			mscrollpane_DataTree = new JScrollPane();
			mscrollpane_DataTree.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			mscrollpane_DataTree.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			// set up panel
			setLayout( new BorderLayout() );
			setBorder( BorderFactory.createTitledBorder(borderEtched, "Dataset", TitledBorder.RIGHT, TitledBorder.TOP) );
			removeAll();
    		add( mscrollpane_DataTree, BorderLayout.CENTER );

			// selection listener for directory tree
			mtreeData.addTreeSelectionListener(
				new TreeSelectionListener(){
				    public void valueChanged(TreeSelectionEvent e){
						TreePath tp = mtreeData.getSelectionPath();
						if( tp == null ) return;
						Object oSelectedNode = tp.getLastPathComponent();
						DataTreeNode node = (DataTreeNode)oSelectedNode;
						mParent._showVariable( node.getBaseType() );
					}
				}
			);

			return true;

		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}
}

class Panel_VarView extends JPanel {
	// depending on mode/selected variable type
	// draw on canvas
	// on click/double click events
	// right click to resize stuff
	// be able to record and store presentation info in class below
}

// stores information about how the dataset should be viewed (column widths etc)
class Model_DatasetView {
}

class Model_VariableView {
}

