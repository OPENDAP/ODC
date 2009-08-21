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

import java.util.HashMap;

import java.beans.PropertyChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
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
	Model_DataView modelDataView = null;
	Panel_LoadedDatasets panelLoadedDatasets;
	Panel_EditContainer panelEditContainer;
	Panel_VarView panelVarView;
	private JSplitPane msplitViewData;
	private HashMap<Model_Dataset,Integer> hashmapVerticalSplit; // used to store divider location
    public Panel_View_Data() {}
	public boolean _zInitialize( Model_LoadedDatasets data_list, StringBuffer sbError ){
		try {
			panelLoadedDatasets = new Panel_LoadedDatasets();
			panelEditContainer = new Panel_EditContainer();
			panelVarView = new Panel_VarView();	
			modelDataView = new Model_DataView();
			if( ! modelDataView.zInitialize( this, data_list, sbError ) ){
				sbError.insert(0, "failed to initialize model: ");
				return false;
			}
			if( ! panelLoadedDatasets.zInitialize( this, sbError ) ){
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
			msplitViewData = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
			msplitViewData.setContinuousLayout( true );
			msplitViewData.setTopComponent( panelTop );
			msplitViewData.setBottomComponent( panelVarView );
			msplitViewData.setDividerLocation( 0.5d );
			setLayout( new BorderLayout() );
			
			// setup split pane
			add( msplitViewData, BorderLayout.CENTER );
			hashmapVerticalSplit = new HashMap<Model_Dataset,Integer>();
			msplitViewData.addPropertyChangeListener(
				new PropertyChangeListener(){
					public void propertyChange( java.beans.PropertyChangeEvent e ){
						if( e.getPropertyName().equals( JSplitPane.LAST_DIVIDER_LOCATION_PROPERTY ) ){
							JSplitPane jsp = (JSplitPane)e.getSource();
							int iDividerLocation = jsp.getDividerLocation();
							Model_Dataset active_model = Panel_View_Data.this.modelDataView.modelActive;
							if( active_model != null )
								Panel_View_Data.this.hashmapVerticalSplit.put( active_model, new Integer(iDividerLocation) );
						}}});
			
		} catch( Exception ex ) {
			return false;
		}
		return true;
	}
	
	public void _vActivate( Model_Dataset modelDataset ){
		if( modelDataset == null ){
			ApplicationController.vShowError( "internal error, no existing dataset for data view selection" );
			return;
		}
		StringBuffer sbError = new StringBuffer( 250 );
		switch( modelDataset.getType() ){
			case Model_Dataset.TYPE_Data:
			case Model_Dataset.TYPE_Expression:
				if( _zSetModel( modelDataset, sbError ) ){
					// ApplicationController.vShowStatus( "activated: " + modelDataset.getTitle() );
				} else {
					ApplicationController.vShowError( "error setting model for expression: " + sbError );
					return;
				}
				modelDataView.modelActive = modelDataset;
				break;
			default:
				ApplicationController.vShowError( "internal error, unsupported type for data view: " + modelDataset.getTypeString() );
				return;
		}
		Integer integerDividerLocation = hashmapVerticalSplit.get( modelDataset );
		if( integerDividerLocation == null ){
			msplitViewData.setDividerLocation( 0.5d );
		} else {
			msplitViewData.setDividerLocation( integerDividerLocation.intValue() );
		}
		
	}
	
	public void vSetFocus(){    	
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				panelLoadedDatasets.requestFocus();
			}
		});
	}
	
	public boolean _zSetModel( Model_Dataset model, StringBuffer sbError ){
		if( ! panelEditContainer._zSetModel( model, sbError ) ) return false;
		return true; 
	}

}

class Model_DataView {
	Panel_View_Data mParent;
	int ctNewDatasets = 0;
	Model_LoadedDatasets mDatasetList = null;
	Model_Dataset modelActive = null;
	boolean zInitialize( Panel_View_Data parent, Model_LoadedDatasets data_list, StringBuffer sbError ){
		if( data_list == null ){
			sbError.append("dataset list not supplied");
			return false;
		}
		mDatasetList = data_list;
		mParent = parent;
		return true;
	}
	void action_New_Dataset(){
		try {
			String sServerVersion = "2.1.5";
			int iHeaderType = opendap.dap.ServerVersion.XDAP; // this is the simple version (eg "2.1.5"), server version strings are like "XDODS/2.1.5", these are only used in HTTP, not in files
			opendap.dap.ServerVersion server_version = new opendap.dap.ServerVersion( sServerVersion, iHeaderType );
			opendap.dap.DataDDS datadds = new opendap.dap.DataDDS( server_version );
			ctNewDatasets++;
			String sName = "data" + ctNewDatasets;
			datadds.setName( sName );
			Model_Dataset model = new Model_Dataset( Model_Dataset.TYPE_Data );
			model.setTitle( sName );
			if( mDatasetList._contains( model ) ){
				ApplicationController.vShowError( "" );
			}
			mDatasetList.addDataset( model );
			mParent._vActivate( model );
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "while trying to create new dataset: " );
		}
	}
	void action_New_Expression(){
		try {
			ctNewDatasets++;
			String sName = "expression" + ctNewDatasets;
			Model_Dataset model = new Model_Dataset( Model_Dataset.TYPE_Expression );
			model.setTitle( sName );
			String sFileDirectory = ConfigurationManager.getInstance().getDefault_DIR_Scripts();
			String sFileName = model.getTitle() + ".txt";
			model.setFileDirectory( sFileDirectory );
			model.setFileName( sFileName );
			mDatasetList.addDataset( model );
			mParent._vActivate( model );
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "while trying to create new dataset: " );
		}
	}
	void action_Load(){
		try {
			SavableImplementation savable = new SavableImplementation( opendap.clients.odc.Model_Dataset.class, null, null );
			Model_Dataset model = (Model_Dataset)savable._open();
			if( model == null ) return; // user cancelled
			if( mDatasetList._contains( model ) ){
				ApplicationController.vShowWarning( "model " + model.getTitle() + " is already open" );
			} else {
				mDatasetList.addDataset( model );
			}
			mParent._vActivate( model );
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
	Panel_View_Data mParent;
	Model_DataView modelDataView;
	boolean zInitialize( final Panel_View_Data parent, final StringBuffer sbError ){

		if( parent == null ){
			sbError.append("no parent supplied");
			return false;
		}
		mParent = parent;
		modelDataView = mParent.modelDataView;

		// create controls
		JComboBox jcbLoadedVariables = new JComboBox( modelDataView.mDatasetList );
		JButton buttonNewDataset = new JButton( "New" );
		JButton buttonNewExpression = new JButton( "New Exp" );
		JButton buttonLoad = new JButton( "Load..." );
		JButton buttonUnload = new JButton( "Unload" );
		JButton buttonSave = new JButton( "Save" );
		JButton buttonSaveAs = new JButton( "Save as..." );

		modelDataView.mDatasetList.addListDataListener( jcbLoadedVariables );

		// layout controls
		this.setLayout( new BoxLayout(this, BoxLayout.X_AXIS) );
		this.add( jcbLoadedVariables );
		this.add( buttonNewDataset );
		this.add( buttonNewExpression );
		this.add( buttonLoad );
		this.add( buttonUnload );
		this.add( buttonSave );
		this.add( buttonSaveAs );

		jcbLoadedVariables.addItemListener(
			new java.awt.event.ItemListener(){
				public void itemStateChanged( ItemEvent event ){
					if( event.getStateChange() == ItemEvent.SELECTED ){
						Model_Dataset modelSelected = (Model_Dataset)event.getItem();
						mParent._vActivate( modelSelected );
					}
				}
			}
		);
		jcbLoadedVariables.setEditable( true );
		jcbLoadedVariables.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed( ActionEvent e ){
					if( "comboBoxEdited".equals( e.getActionCommand() ) ){
						JComboBox jcb = (JComboBox)e.getSource();
						Object oEditBox = jcb.getEditor().getEditorComponent();
						if( oEditBox == null ) return;
						if( ! (oEditBox instanceof JTextField) ){
							ApplicationController.vShowError( "internal error, combo box editor was not a JTextField" );
							return;
						}
						JTextField jtfEditor = (JTextField)oEditBox;
						String sText = jtfEditor.getText();
						if( sText == null || sText.length() == 0 ){
							ApplicationController.vShowWarning( "dataset names cannot be blank" );
							return;
						}
						Model_LoadedDatasets model = (Model_LoadedDatasets)jcb.getModel();
						StringBuffer sbError = new StringBuffer();
						if( ! model._setName( sText, sbError ) ){
							ApplicationController.vShowError( "error setting dataset name: " + sbError );
						}
					}
            }});
		buttonNewDataset.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed( ActionEvent event) {
					modelDataView.action_New_Dataset();
				}
			}
		);
		buttonNewExpression.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed( ActionEvent event) {
					modelDataView.action_New_Expression();
				}
			}
		);
		buttonLoad.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed(ActionEvent event) {
					modelDataView.action_Load();
				}
			}
		);
		buttonUnload.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed(ActionEvent event) {
					modelDataView.action_Unload();
				}
			}
		);
		buttonSave.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed(ActionEvent event) {
					modelDataView.action_Save();
				}
			}
		);
		buttonSaveAs.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed(ActionEvent event) {
					modelDataView.action_SaveAs();
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
		if( ! mEditExpression._zInitialize( mDefineExpression, null, null, null, sbError ) ){
			sbError.insert( 0, "initializing expression editor: " );
			return false;
		}
		mEditStream = new Panel_Edit_Stream();
		mDefineData = new Panel_Define_Dataset();
		mDefineExpression = new Panel_Define_Expression();
		mDefineStream = new Panel_Define_Stream();
		setLayout( new BorderLayout() );
		Border borderEtched = BorderFactory.createEtchedBorder();
		setBorder( BorderFactory.createTitledBorder( borderEtched, "Edit Container", TitledBorder.RIGHT, TitledBorder.TOP ) );
		return true;
	}
	boolean _zSetModel( Model_Dataset model, StringBuffer sbError ){
		switch( model.getType() ){
			case Model_Dataset.TYPE_Data:
				if( mEditStructure._zInitialize( mDefineData, sbError ) ){
					removeAll();
					add( mEditStructure, BorderLayout.CENTER );
					add( mDefineData, BorderLayout.EAST );
				} else {
					sbError.insert( 0, "failed to initialize data structure panel" );
					return false;
				}
				break;
			case Model_Dataset.TYPE_Expression:
				if( mEditExpression._setModel( model, sbError ) ){
					removeAll();
					add( mEditExpression, BorderLayout.CENTER );
					add( mDefineExpression, BorderLayout.EAST );
				} else {
					sbError.insert( 0, "failed to initialize expression editing panel" );
					return false;
				}
				break;
			case Model_Dataset.TYPE_Stream:
				if( mEditStream._zInitialize(  mDefineStream, sbError ) ){
					removeAll();
					add( mEditStream, BorderLayout.CENTER );
					add( mDefineStream, BorderLayout.EAST );
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
	Panel_View_Data _getParent(){ return mParent; }
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
	Panel_View_Data _getParent(){ return mParent; }
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
	Panel_View_Data _getParent(){ return mParent; }
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
	void _setModel( Model_DataTree model ){
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
	private Panel_View_Text_Editor mEditor;
	private Panel_DDSView mDDSView;
	boolean _zInitialize( Panel_Define_Expression parent, String sDirectory, String sName, String sContent, StringBuffer sbError ){
		mModel = null;
		mParent = parent;
		Border borderEtched = BorderFactory.createEtchedBorder();
		setBorder( BorderFactory.createTitledBorder( borderEtched, "Expression Editor", TitledBorder.RIGHT, TitledBorder.TOP ) );
		setLayout( new BorderLayout() );
		
		// set up editor
		mEditor = new Panel_View_Text_Editor();
		mEditor._zInitialize( null, sDirectory, sName, sContent, sbError );
		this.add( mEditor, BorderLayout.CENTER );
		
		// set up structure display
		mDDSView = new Panel_DDSView();
		this.add( mDDSView, BorderLayout.EAST );
		
		return true;
	}
	Model_Dataset _getModel(){ return mModel; }
	boolean _setModel( Model_Dataset model, StringBuffer sbError ){
		if( model.getType() != Model_Dataset.TYPE_Expression ){
			sbError.append( "supplied model is not an expression" );
			return false;
		}
		mModel = model;
		mEditor._setModel( model );
		if( ! mDDSView.zSetExpressionDDS( model, sbError ) ){
			sbError.insert( 0, "setting DDS view for expression model" );
			return false;
		}
		return true;
	}
	Panel_Define_Expression _getParent(){ return mParent; }
}

class Panel_Edit_Stream extends JPanel {
	private Model_Dataset mModel;
	private Panel_Define_Stream mParent;
	boolean _zInitialize( Panel_Define_Stream parent, StringBuffer sbError ){
		setLayout( new BorderLayout() );
		Border borderEtched = BorderFactory.createEtchedBorder();
		setBorder( BorderFactory.createTitledBorder(borderEtched, "Stream Editor", TitledBorder.RIGHT, TitledBorder.TOP) );
		mParent = parent;
		return true;
	}
	Model_Dataset _getModel(){ return mModel; }
	boolean _setModel( Model_Dataset model, StringBuffer sbError ){
		if( model.getType() != Model_Dataset.TYPE_Stream ){
			sbError.append( "supplied model is not a stream" );
			return false;
		}
		mModel = model;
		return true;
	}
	Panel_Define_Stream _getParent(){ return mParent; }
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

