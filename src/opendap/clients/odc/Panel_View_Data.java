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
import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JButton;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

public class Panel_View_Data extends JPanel implements IControlPanel {
	Panel_LoadedDatasets panelLoadedDatasets;
	Panel_EditContainer panelEditContainer;
	Panel_VarView panelVarView;	
    public Panel_View_Data() {}
	public boolean _zInitialize( Model_LoadedDatasets data_list, StringBuffer sbError ){
		try {
			panelLoadedDatasets = new Panel_LoadedDatasets();
			panelEditContainer = new Panel_EditContainer();
			panelVarView = new Panel_VarView();	
			Model_DataView model = new Model_DataView();
			if( ! model.zInitialize( data_list, sbError ) ){
				sbError.insert(0, "failed to initialize model: ");
				return false;
			}
			if( ! panelLoadedDatasets.zInitialize( model, sbError ) ){
				sbError.insert(0, "failed to initialize loaded datasets panel: ");
				return false;
			}
			
			if( ! panelEditContainer._zInitialize( sbError) ){
				sbError.insert( 0, "failed to initialize structure view: " );
				return false;
			}
			
			if( ! panelVarView._zInitialize( sbError ) ){
				sbError.insert( 0, "failed to initialize var view: " );
				return false;
			}
			
			JPanel panelTop = new JPanel();
			panelTop.setLayout( new BorderLayout() );
			panelTop.add( panelLoadedDatasets, BorderLayout.NORTH );
			panelTop.add( panelEditContainer, BorderLayout.CENTER );
			JSplitPane jsplitTreeVar = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
			jsplitTreeVar.setTopComponent( panelTop );
			jsplitTreeVar.setBottomComponent( panelVarView );
			this.setLayout( new BorderLayout() );
			this.add( jsplitTreeVar, BorderLayout.CENTER );
		} catch( Exception ex ) {
			return false;
		}
		return true;
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
	Model_Dataset modelActive = null;
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
		try {
			SavableImplementation savable = new SavableImplementation( opendap.clients.odc.Model_Dataset.class, null, null );
			Model_Dataset model = (Model_Dataset)savable._open();
			if( model == null ) return; // user cancelled
			mDatasetList.addDataset( model );
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "while trying to load dataset: " );
		}
	}
	void action_Unload(){
		mDatasetList.removeDataset( modelActive );
	}
	void action_Save(){
		if( modelActive == null ){
			ApplicationController.vShowStatus_NoCache( "no dataset selected" );
		} else {
			modelActive.getSavable()._save( modelActive );
		}
	}
	void action_SaveAs(){
		if( modelActive == null ){
			ApplicationController.vShowStatus_NoCache( "no dataset selected for SaveAs" );
		} else {
			modelActive.getSavable()._saveAs( modelActive );
		}
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
		JButton buttonLoad = new JButton( "Load..." );
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

// contains the editing panels:
// Data -        left: Panel_Edit_StructureView      right: Panel_Define_Dataset
// Expression -  left: Panel_Edit_Expression         right: Panel_Define_Expression
// Stream -      left: Panel_Edit_Stream             right: Panel_Define_Stream 
class Panel_EditContainer extends JPanel {
	Panel_Edit_StructureView mEditStructure;
	Panel_Edit_Expression mEditExpression;
	Panel_Edit_Stream mEditStream;
	Panel_Define_Dataset mDefineData;
	Panel_Define_Expression mDefineExpression;
	Panel_Define_Stream mDefineStream;
	boolean _zInitialize( StringBuffer sbError ){
		mEditStructure = new Panel_Edit_StructureView();
		mEditExpression = new Panel_Edit_Expression();
		mEditStream = new Panel_Edit_Stream();
		mDefineData = new Panel_Define_Dataset();
		mDefineExpression = new Panel_Define_Expression();
		mDefineStream = new Panel_Define_Stream();
		setLayout( new BorderLayout() );
		return true;
	}
	boolean zSetModel( Model_Dataset model, StringBuffer sbError ){
		switch( model.getType() ){
			case Model_Dataset.TYPE_Data:
				if( mEditStructure._zInitialize( mDefineData, sbError ) ){
					this.removeAll();
					this.add( mEditStructure, BorderLayout.CENTER );
					this.add( mDefineData, BorderLayout.EAST );
				} else {
					sbError.insert( 0, "failed to initialize data structure panel" );
					return false;
				}
				break;
			case Model_Dataset.TYPE_Expression:
				if( mEditExpression._zInitialize( model, mDefineExpression, sbError ) ){
					this.removeAll();
					this.add( mEditExpression, BorderLayout.CENTER );
					this.add( mDefineExpression, BorderLayout.EAST );
				} else {
					sbError.insert( 0, "failed to initialize expression editing panel" );
					return false;
				}
				break;
			case Model_Dataset.TYPE_Stream:
				if( mEditStream._zInitialize( model, mDefineStream, sbError ) ){
					this.removeAll();
					this.add( mEditStream, BorderLayout.CENTER );
					this.add( mDefineStream, BorderLayout.EAST );
				} else {
					sbError.insert( 0, "failed to initialize stream editing panel" );
					return false;
				}
				break;
			default:
				sbError.append( "unsupported model type (" + model.getTypeString() + ")" );
				return false;
		}
		return true;
	}
}

class Panel_Define_Stream extends JPanel {
	private Panel_View_Data mParent;
	private Dimension dimMinimum = new Dimension(100, 80);
	public Dimension getMinimumSize(){
		return dimMinimum;
	}
	boolean _zInitialize( Panel_View_Data parent, StringBuffer sbError ){
		try {
			mParent = parent;

			Border borderEtched = BorderFactory.createEtchedBorder();

			// set up panel
			setLayout( new BorderLayout() );
			setBorder( BorderFactory.createTitledBorder(borderEtched, "Define Stream", TitledBorder.RIGHT, TitledBorder.TOP) );
			removeAll();

			return true;

		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}
}

class Panel_Define_Expression extends JPanel {
	private Panel_View_Data mParent;
	private Dimension dimMinimum = new Dimension(100, 80);
	public Dimension getMinimumSize(){
		return dimMinimum;
	}
	boolean _zInitialize( Panel_View_Data parent, StringBuffer sbError ){
		try {
			mParent = parent;

			Border borderEtched = BorderFactory.createEtchedBorder();

			// set up panel
			setLayout( new BorderLayout() );
			setBorder( BorderFactory.createTitledBorder(borderEtched, "Define Expression", TitledBorder.RIGHT, TitledBorder.TOP) );
			removeAll();

			return true;

		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}
}

class Panel_Define_Dataset extends JPanel {
	private Panel_View_Data mParent;
	private Dimension dimMinimum = new Dimension(100, 80);
	public Dimension getMinimumSize(){
		return dimMinimum;
	}
	boolean _zInitialize( Panel_View_Data parent, StringBuffer sbError ){
		try {
			mParent = parent;

			Border borderEtched = BorderFactory.createEtchedBorder();

			// set up panel
			setLayout( new BorderLayout() );
			setBorder( BorderFactory.createTitledBorder(borderEtched, "Define Dataset", TitledBorder.RIGHT, TitledBorder.TOP) );
			removeAll();

			return true;

		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}
	public void _showVariable( BaseType bt ){
		// TODO
	}	
}

class Panel_Edit_StructureView extends JPanel {
	private Model_DataTree mTreeModel = null;
	private JScrollPane mscrollpane_DataTree;
	private Panel_Define_Dataset mParent;
	private Dimension dimMinimum = new Dimension(100, 80);
	private JTree mtreeData = null;
	public Dimension getMinimumSize(){
		return dimMinimum;
	}
	void setModel( Model_DataTree model ){
		mTreeModel = model;
		if( mtreeData != null ) mtreeData.setModel( mTreeModel );
	}
	boolean _zInitialize( Panel_Define_Dataset parent, StringBuffer sbError ){
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
			setBorder( BorderFactory.createTitledBorder(borderEtched, "Dataset Structure", TitledBorder.RIGHT, TitledBorder.TOP) );
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

class Panel_Edit_Expression extends JPanel {
	private Model_Dataset mModel;
	private Panel_Define_Expression mParent;
	boolean _zInitialize( Model_Dataset model, Panel_Define_Expression parent, StringBuffer sbError ){
		if( model.getType() != Model_Dataset.TYPE_Expression ){
			sbError.append( "supplied model is not an expression" );
			return false;
		}
		mModel = model;
		mParent = parent;
		setLayout( new BorderLayout() );
		Border borderEtched = BorderFactory.createEtchedBorder();
		setBorder( BorderFactory.createTitledBorder(borderEtched, "Expression Editor", TitledBorder.RIGHT, TitledBorder.TOP) );
		return true;
	}
}

class Panel_Edit_Stream extends JPanel {
	private Model_Dataset mModel;
	private Panel_Define_Stream mParent;
	boolean _zInitialize( Model_Dataset model, Panel_Define_Stream parent, StringBuffer sbError ){
		if( model.getType() != Model_Dataset.TYPE_Stream ){
			sbError.append( "supplied model is not a stream" );
			return false;
		}
		setLayout( new BorderLayout() );
		Border borderEtched = BorderFactory.createEtchedBorder();
		setBorder( BorderFactory.createTitledBorder(borderEtched, "Stream Editor", TitledBorder.RIGHT, TitledBorder.TOP) );
		mModel = model;
		mParent = parent;
		return true;
	}
}

class Panel_VarView extends JPanel {
	public Panel_VarView(){}
	public boolean _zInitialize( StringBuffer sbError ){
		this.add( new JLabel("var view") );
		return true;
	}
	// depending on mode/selected variable type
	// draw on canvas
	// on click/double click events
	// right click to resize stuff
	// be able to record and store presentation info in class below
}

// stores information about how the dataset should be viewed (column widths etc)
class Model_DatasetView {
	// TODO
}

class Model_VariableView {
	// TODO
}

