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

package opendap.clients.odc.data;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.DAP.DAP_VARIABLE;
import opendap.clients.odc.IControlPanel;
import opendap.clients.odc.Interpreter;
import opendap.clients.odc.geo.Utility;
import opendap.clients.odc.gui.LeaflessTreeCellRenderer;
import opendap.clients.odc.gui.Styles;
import opendap.clients.odc.Panel_View_Text_Editor;
import opendap.clients.odc.SavableImplementation;
import opendap.dap.BaseType;
import opendap.dap.DataDDS;

import java.util.HashMap;

import java.beans.PropertyChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ComponentEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import java.util.ArrayList;

/** Panel_LoadedDatasets    dataset combo box and action buttons
 *  Model_DataView          key actions (new dataset, delete dataset, etc)
 *
 * contains the editing panels:
 * Data -        left: Panel_Edit_StructureView      right: Panel_Define_Dataset
 *                                                              Panel_Edit_Dataset
 *                                                              Panel_Edit_Variable
 * Expression -  left: Panel_Edit_Expression         right: Panel_Define_Expression
 * Stream -      left: Panel_Edit_Stream             right: Panel_Define_Stream
 * 
 * Panel below (South):
 * 
 *   Panel_VarView (North)
 *   Panel_Edit_ViewArray  (South)
 */

public class Panel_View_Data extends JPanel implements IControlPanel {
	Model_DataView modelDataView = null;
	Panel_LoadedDatasets panelLoadedDatasets;
	Panel_Edit_Container panelEditContainer;
	Panel_VarView panelVarView;
	boolean mzAddingItemToList = false; // this is flag used to prevent the add from triggering an item activation
	private JSplitPane msplitViewData;
	private HashMap<Model_Dataset,Integer> hashmapVerticalSplit; // used to store divider location
    public Panel_View_Data() {}
    
	public boolean _zInitialize( Model_LoadedDatasets data_list, StringBuffer sbError ){
		try {
			msplitViewData = new JSplitPane( JSplitPane.VERTICAL_SPLIT );

			panelLoadedDatasets = new Panel_LoadedDatasets();
			panelVarView = Panel_VarView._create( msplitViewData, sbError );	
			if( panelVarView == null ){
				sbError.insert( 0, "failed to create var view: " );
				return false;
			}			
			modelDataView = new Model_DataView();
			if( ! modelDataView.zInitialize( this, data_list, sbError ) ){
				sbError.insert(0, "failed to initialize model: ");
				return false;
			}
			if( ! panelLoadedDatasets.zInitialize( this, sbError ) ){
				sbError.insert(0, "failed to initialize loaded datasets panel: ");
				return false;
			}
			panelEditContainer = Panel_Edit_Container._zCreate( this, sbError );
			if( panelEditContainer == null ){
				sbError.insert( 0, "failed to initialize structure view: " );
				return false;
			}
			
			JPanel panelTop = new JPanel();
			panelTop.setLayout( new BorderLayout() );
			panelTop.add( panelLoadedDatasets, BorderLayout.NORTH );
			panelTop.add( panelEditContainer, BorderLayout.CENTER );
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
			ApplicationController.vUnexpectedError( ex, sbError );
			return false;
		}
		return true;
	}
	
	public void _vActivate( Model_Dataset modelDataset ){
		if( modelDataset == null ){
			ApplicationController.vShowError( "(Panel_View_Data) internal error, no existing dataset for data view selection" );
			return;
		}
		StringBuffer sbError = new StringBuffer( 250 );
		switch( modelDataset.getType() ){
			case Model_Dataset.TYPE_Data:
				if( _zSetModel( modelDataset, sbError ) ){
					// ApplicationController.vShowStatus( "activated dataset model: " + modelDataset.getTitle() );
				} else {
					ApplicationController.vShowError( "(Panel_View_Data) internal error while setting model for dataset: " + sbError );
					return;
				}
				modelDataView.modelActive = modelDataset;
				break;
			case Model_Dataset.TYPE_Expression:
				if( _zSetModel( modelDataset, sbError ) ){
					// ApplicationController.vShowStatus( "activated expression model: " + modelDataset.getTitle() );
				} else {
					ApplicationController.vShowError( "(Panel_View_Data) internal error while setting model for expression: " + sbError );
					return;
				}
				modelDataView.modelActive = modelDataset;
				break;
			default:
				ApplicationController.vShowError( "(Panel_View_Data) internal error, unsupported type for data view: " + modelDataset.getTypeString() );
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
		if( model == null ){
			sbError.append( "no model supplied" );
			return false;
		}
		if( ! panelEditContainer._zSetModel( model, sbError ) ) return false;
		return true; 
	}

	public Model_DataView _getModelDataView(){ return modelDataView; }
	
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
			datadds.setName( "data" );
			StringBuffer sbError = new StringBuffer(256);
			Model_Dataset model = Model_Dataset.createData( datadds, sbError );
			if( model == null ){
				ApplicationController.vShowError( "(Panel_View_Data) error creating data model: " + sbError.toString() );
				return;
			}
			String sTitle = "Dataset";
			addDataset_DataDDS( model, sTitle );
			mDatasetList.setSelectedItem( model );
			mParent._vActivate( model );
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "(Panel_View_Data) while trying to create new dataset: " );
		}
	}
	void addDataset_DataDDS( Model_Dataset model, String sTitle ){
		try {
			mParent.mzAddingItemToList = true;
			if( model == null ){
				ApplicationController.vShowError( "(Panel_View_Data) internal error, attempted to add non-existent model" );
				return;
			}
			if( mDatasetList._contains( model ) ){
				ApplicationController.vShowError( "(Panel_View_Data) internal error, duplicate model" );
				return;
			}
			DataDDS data = model.getData();
			if( data == null ){
				ApplicationController.vShowError( "(Panel_View_Data) internal error, attempted to add model with no data" );
				return;
			}
			model.setTitle( sTitle );
			ctNewDatasets++;
			mDatasetList.addDataset( model );
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t,"While adding model " + model + " as DataDDS to list" );
		} finally {
			mParent.mzAddingItemToList = false;
		}
	}
	void action_New_Expression(){
		try {
			ctNewDatasets++;
			String sName = "expression" + ctNewDatasets;
			opendap.dap.DDS dds = new opendap.dap.DDS( sName );
			StringBuffer sbError = new StringBuffer( 256 );
			Model_Dataset model = Model_Dataset.createExpression( dds, sbError );
			if( model == null ){
				ApplicationController.vShowError( "(Panel_View_Data) failed to create expression model from blank DDS: " + sbError.toString() );
				return;
			}
			model.setTitle( sName );
			String sFileDirectory = ConfigurationManager.getInstance().getDefault_DIR_Scripts();
			String sFileName = model.getTitle() + ".txt";
			model.setFileDirectory( sFileDirectory );
			model.setFileName( sFileName );
			mParent.mzAddingItemToList = true;
			mDatasetList.addDataset( model );
			mParent.mzAddingItemToList = false;
			mParent._vActivate( model );
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "while trying to create new dataset: " );
			mParent.mzAddingItemToList = false;
		}
	}
	void action_Load(){
		try {
			SavableImplementation savable = new SavableImplementation( opendap.clients.odc.data.Model_Dataset.class, null, null );
			Model_Dataset model = (Model_Dataset)savable._open();
			if( model == null ) return; // user cancelled
			if( mDatasetList._contains( model ) ){
				ApplicationController.vShowWarning( "(Panel_View_Data) model " + model.getTitle() + " is already open" );
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
	void action_Retitle(){
		if( modelActive == null ){
			ApplicationController.vShowStatus_NoCache( "no dataset selected for retitling operation" );
		} else {
			StringBuffer sbError = new StringBuffer();
			try {
				String sPromptStandard = "Enter new title for " + modelActive.getTitle() + ":";
				String sPromptVerbose = "Enter new title for " + modelActive.getTitle() + " (cannot be blank or over 255 characters):";
				String sPrompt = sPromptStandard;
				String sOldName = modelActive.getTitle();
				while( true ){
					String sNewName = JOptionPane.showInputDialog( sPrompt );
					if( sNewName == null ) return; // cancel
					if( sNewName.length()  == 0 ){
						ApplicationController.vShowStatus_NoCache( "new title for dataset cannot be blank" );
						sPrompt = sPromptVerbose;
						continue;
					}
					if( sNewName.length() > 255 ){
						ApplicationController.vShowStatus_NoCache( "new title for dataset cannot be longer than 255 characters" );
						sPrompt = sPromptVerbose;
						continue;
					}
					if( mDatasetList._setName( sNewName, sbError ) ){
						ApplicationController.vShowStatus( "Dataset retitled from " + sOldName + " to " + sNewName );
					} else {
						ApplicationController.vShowError( "Error attempting to retitle dataset to [" + sNewName + "]: " );
					}
					return;
				}
			} catch( Throwable t ) {
				Utility.vUnexpectedError( t, sbError );
			}
		}
	}
	void action_GenerateDatasetFromExpression(){
		if( modelActive == null ){
			ApplicationController.vShowStatus_NoCache( "no expression selected for generation" );
		} else if( modelActive.getType() != Model_Dataset.TYPE_Expression ){
			ApplicationController.vShowStatus_NoCache( "selected dataset is not an expression" );
		} else {
			StringBuffer sbError = new StringBuffer( 256 );
			String sExpressionText = this.modelActive.getExpression_Text();
			Interpreter interpreter = ApplicationController.getInstance().getInterpreter();
			Model_Dataset generated_dataset = interpreter.generateDatasetFromExpression( sExpressionText, sbError );
			if( generated_dataset == null ){
				ApplicationController.vShowError( "Failed to generate dataset from expression " + this.modelActive.getTitle() + ": " + sbError );
				return;
			}
			addDataset_DataDDS( generated_dataset, this.modelActive.getTitle() + "_" );
		}
	}
}

class Panel_LoadedDatasets extends JPanel {
	Panel_View_Data mParent;
	Model_DataView modelDataView;
	JComboBox jcbLoadedDatasets;
	boolean zInitialize( final Panel_View_Data parent, final StringBuffer sbError ){

		if( parent == null ){
			sbError.append("no parent supplied");
			return false;
		}
		mParent = parent;
		modelDataView = mParent.modelDataView;

		// create controls
		jcbLoadedDatasets = new JComboBox( modelDataView.mDatasetList );
		JButton buttonNewDataset = new JButton( "New" );
		JButton buttonNewExpression = new JButton( "New Exp" );
		JButton buttonLoad = new JButton( "Load..." );
		JButton buttonUnload = new JButton( "Unload" );
		JButton buttonSave = new JButton( "Save" );
		JButton buttonSaveAs = new JButton( "Save as..." );
		JButton buttonRetitle = new JButton( "Retitle" );

		jcbLoadedDatasets.setRenderer( new CellRenderer_UniqueLabel() );
		modelDataView.mDatasetList.addListDataListener( jcbLoadedDatasets );

		// layout controls
		this.setLayout( new BoxLayout(this, BoxLayout.X_AXIS) );
		this.add( jcbLoadedDatasets );
		this.add( buttonNewDataset );
		this.add( buttonNewExpression );
		this.add( buttonLoad );
		this.add( buttonUnload );
		this.add( buttonSave );
		this.add( buttonSaveAs );
		this.add( buttonRetitle );

		jcbLoadedDatasets.addItemListener(
			new java.awt.event.ItemListener(){
				public void itemStateChanged( ItemEvent event ){
					if( mParent.mzAddingItemToList ) return; // item is being programmatically added
					if( event.getStateChange() == ItemEvent.SELECTED ){
						Model_Dataset modelSelected = (Model_Dataset)event.getItem();
						mParent._vActivate( modelSelected );
					}
				}
			}
		);
		jcbLoadedDatasets.setEditable( false );
		jcbLoadedDatasets.addActionListener(
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
		buttonRetitle.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed(ActionEvent event) {
					modelDataView.action_Retitle();
				}
			}
		);

		return true;
	}
}

// this changes the label to append a duplicate count after the string
// for example if there are 5 items in the list named "apple" and
// this is the 3rd such item then its label will be rendered as "apple (3)"
class CellRenderer_UniqueLabel extends javax.swing.DefaultListCellRenderer {
	public java.awt.Component getListCellRendererComponent(
			javax.swing.JList list,
			Object value,   // value to display
			int index,      // cell index
			boolean iss,    // is the cell selected
			boolean chf)    // the list and the cell have the focus
	{
		super.getListCellRendererComponent(list, value, index, iss, chf);
		if( value == null ){
			setText( "" );
		} else {
			Model_LoadedDatasets model = (Model_LoadedDatasets)list.getModel();
			Object oSelected = model.getSelectedItem();
			int ctListItems = model.getSize();
			if( index == -1 && oSelected == null ){
				setText( "" );
			} else {
				if( index == -1 ) index = model.getSelectedIndex0();
				String sValue = value.toString();
				int ctDuplicateItems = 0;
				int xDuplicateItem = 0;
				for( int xItem = 0; xItem < ctListItems; xItem++ ){
					if( index == xItem ){
						xDuplicateItem = ctDuplicateItems + 1; 
						continue;
					}
					Object o = list.getModel().getElementAt( xItem );
					if( o.toString().equals( sValue ) ) ctDuplicateItems++;
				}
				if( ctDuplicateItems > 0 ){
					String sText = sValue + ' ' + '(' + xDuplicateItem + ')';
					setText( sText );
				} else {
					setText( sValue );
				}
			}
		}
		// setIcon(...); TODO
		return this;
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
	private Panel_Define_Expression(){}
	public Dimension getMinimumSize(){
		return dimMinimum;
	}
	static Panel_Define_Expression _zCreate( Panel_View_Data parent, StringBuffer sbError ){
		try {
			Panel_Define_Expression panelDefineExpression = new Panel_Define_Expression(); 
			if( parent == null ){
				sbError.append( "no parent supplied" );
				return null;
			}
			panelDefineExpression.mParent = parent;
			Border borderEtched = BorderFactory.createEtchedBorder();

			// set up panel
			panelDefineExpression.setLayout( new BorderLayout() );
			panelDefineExpression.setBorder( BorderFactory.createTitledBorder(borderEtched, "Define Expression", TitledBorder.RIGHT, TitledBorder.TOP) );
			panelDefineExpression.removeAll();

			return panelDefineExpression;

		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return null;
		}
	}
	Panel_View_Data _getParent(){ return mParent; }
}

class Panel_Edit_Expression extends JPanel {
	private Model_Dataset mModel;
	private Panel_Define_Expression mParent;
	private Panel_View_Text_Editor mEditor;
	private Panel_DDSView mDDSView;
	boolean _zInitialize( Panel_Define_Expression parent, String sDirectory, String sName, String sContent, StringBuffer sbError ){
		mModel = null;
		mParent = parent;
		if( parent == null ){
			sbError.append( "no parent supplied" );
			return false;
		}
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
		
		// set up control buttons
		JPanel panelControlButtons = new JPanel();
		JButton buttonGenerateDataset = new JButton( "Generate Dataset" );
		panelControlButtons.setLayout( new BoxLayout(panelControlButtons, BoxLayout.X_AXIS) );
		panelControlButtons.add( buttonGenerateDataset );
		this.add( panelControlButtons, BorderLayout.NORTH );
		
		// define actions
		buttonGenerateDataset.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed( ActionEvent event ){
					if( mParent == null ){
						ApplicationController.vShowError( "internal error, Panel_Edit_Expression has no parent" );
						return;
					}
					Panel_View_Data panelViewData = mParent._getParent();
					if( panelViewData == null ){
						ApplicationController.vShowError( "internal error, no panel exists for generate dataset" );
						return;
					}
					Model_DataView modelDataView = panelViewData._getModelDataView();
					if( panelViewData == null ){
						ApplicationController.vShowError( "internal error, no data view model exists for generate dataset" );
						return;
					}
					modelDataView.action_GenerateDatasetFromExpression();
				}
			}
		);
		
		return true;
	}
	Model_Dataset _getModel(){ return mModel; }
	boolean _setModel( Model_Dataset model, StringBuffer sbError ){
		if( model == null ){
			sbError.append( "no model supplied" );
			return false;
		}
		if( model.getDDS_Full() == null ){
			sbError.append( "model has no DDS" );
			return false;
		}
		if( model.getType() != Model_Dataset.TYPE_Expression ){
			sbError.append( "supplied model is not an expression" );
			return false;
		}
		mModel = model;
		mEditor._setModel( model );
		if( ! mDDSView.zSetExpressionDDS( model, sbError ) ){
			sbError.insert( 0, "setting DDS view for expression model: " );
			return false;
		}
		return true;
	}
	Panel_Define_Expression _getParent(){ return mParent; }
}

/** Blank panel with help-type info on it. Used when no model is selected. */
class Panel_Edit_blank extends JPanel {
	ArrayList<JLabel> listLabels = new ArrayList<JLabel>(); 
	public Panel_Edit_blank(){
		this.setLayout( null );
		this.setPreferredSize( new Dimension( 200, 200 ) );
		listLabels.add( new JLabel( "    New: creates an empty data structure with no variables defined." ) );
		listLabels.add( new JLabel( "New Exp: opens an editing window for writing an expression." ) );
		listLabels.add( new JLabel( "   Load: loads an existing data file or expression. Supported formats are:" ) );
		listLabels.add( new JLabel( "            .odc ODC native binary (contains data definition and possibly data)" ) );
		listLabels.add( new JLabel( "            .cdf NetCDF binary (contains data definition and possibly data)" ) );
		listLabels.add( new JLabel( "            .csv Comma-separated values" ) );
		listLabels.add( new JLabel( "            .py  Python script file used to generate data." ) );
		int posX = 10;
		int posY = 0;
		for( JLabel label : listLabels ){
			posY += 20;
			label.setFont( Styles.fontFixed12 );
			label.setSize( label.getPreferredSize() );
			label.setLocation( posX, posY );
			this.add( label );
		}
		this.invalidate();
	}
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

// depending on mode/selected variable type
// draw on canvas
// on click/double click events
// right click to resize stuff
// be able to record and store presentation info in class below
class Panel_VarView extends JPanel implements java.awt.event.ComponentListener, java.awt.event.FocusListener {
	JPanel panelArray_Command;
	Panel_Edit_Cell panelArray_CellEditor;
	Panel_Edit_ViewArray panelArray_View;
	JTextField jtfArray_x;
	JTextField jtfArray_y;
	JTextField jtfArray_value;
	JTextField jtfArray_exp;
	JComboBox jcbArray_exp_range;
	Node_Array nodeActive;
	JSplitPane mSplitPane;     // this panel is in the lower half of this split pane
	final JLabel labelY = new JLabel( "y:" );
	final JLabel labelValue = new JLabel( "value:" );
	final JLabel labelExp = new JLabel( "exp:" );
	final ClickableButton buttonX = new ClickableButton( "x:" );
	private boolean mzShowingSelectionCoordinates = true;
	private StringBuffer sbError_local = new StringBuffer( 256 );
	private Panel_VarView(){}
	public static Panel_VarView _create( JSplitPane jsp, StringBuffer sbError ){
		final Panel_VarView panel = new Panel_VarView();
		panel.mSplitPane = jsp;
				
		// set up command panel for array viewer
		panel.jtfArray_x = new JTextField();
		panel.jtfArray_y = new JTextField();
		panel.jtfArray_value = new JTextField();
		panel.jtfArray_exp = new JTextField();
		String[] asExpressionRangeModes = { "All", "Value", "Selection", "View" };
		panel.jcbArray_exp_range = new JComboBox( asExpressionRangeModes );
		panel.labelY.setHorizontalAlignment( JLabel.RIGHT );
//		panel.labelY.setBorder( BorderFactory.createLineBorder( Color.BLUE ) );
		panel.labelValue.setHorizontalAlignment( JLabel.RIGHT );
//		panel.labelValue.setBorder( BorderFactory.createLineBorder( Color.BLUE ) );
		panel.buttonX.setHorizontalAlignment( JLabel.CENTER );
		panel.buttonX.addMouseListener( panel.buttonX );
		panel.panelArray_Command = new JPanel();
		panel.panelArray_Command.setPreferredSize( new Dimension( 400, 30 ) );
		panel.panelArray_Command.setLayout( null );
		panel.panelArray_Command.add( panel.buttonX );
		panel.panelArray_Command.add( panel.jtfArray_x );
		panel.panelArray_Command.add( panel.labelY );
		panel.panelArray_Command.add( panel.jtfArray_y );
		panel.panelArray_Command.add( panel.labelValue );
		panel.panelArray_Command.add( panel.jtfArray_value );
		panel.panelArray_Command.add( panel.labelExp );
		panel.panelArray_Command.add( panel.jtfArray_exp );
		panel.panelArray_Command.add( panel.jcbArray_exp_range );
		
		// set up array viewer
		panel.panelArray_View = Panel_Edit_ViewArray._create( panel, sbError );
		if( panel.panelArray_View ==  null ){
			sbError.insert( 0, "failed to create array viewer" );
			return null;
		}
//		javax.swing.plaf.basic.BasicSplitPaneUI jspUI = (javax.swing.plaf.basic.BasicSplitPaneUI)panel.mSplitPane.getUI(); 
//		jspUI.getDivider().addComponentListener( panel.panelArray_View ); // listens for movements of the split pane

		panel.setLayout( new BorderLayout() );
		panel.add( panel.panelArray_Command, BorderLayout.NORTH );
		panel.add( panel.panelArray_View, BorderLayout.CENTER );
		panel.addComponentListener( panel );
		panel.addFocusListener( panel );
		
		panel.jtfArray_x.addActionListener(			
			new java.awt.event.ActionListener(){
				public void actionPerformed( ActionEvent event) {
					try {
						int xNewRow = Integer.parseInt( panel.jtfArray_x.getText() );
						if( xNewRow < 0 ) xNewRow = 0;
						if( xNewRow >= panel.nodeActive._getRowCount() ) xNewRow = panel.nodeActive._getRowCount() - 1;
						panel._setSelection( xNewRow, panel.nodeActive.iSelection_y );
					} catch( NumberFormatException ex ) {
						ApplicationController.vShowError_NoModal( "Unable to intepret new row number [" + panel.jtfArray_x.getText() + "] as an integer" );
						panel.jtfArray_x.setText( String.valueOf( panel.nodeActive.iSelection_x ) );
					} catch( Throwable t ){
						ApplicationController.vUnexpectedError( t, "While setting selection coordinate for x" );
					}
				}
			}
		);

		panel.jtfArray_y.addActionListener(			
			new java.awt.event.ActionListener(){
				public void actionPerformed( ActionEvent event) {
					try {
							int xNewColumn = Integer.parseInt( panel.jtfArray_y.getText() );
						if( panel.nodeActive._getColumnCount() == 0 ){ // if this is a vector there are no columns
							xNewColumn = 0; // for a vector the column index is always zero
						} else {
							if( xNewColumn < 0 ) xNewColumn = 0;
							if( xNewColumn >= panel.nodeActive._getColumnCount() ) xNewColumn = panel.nodeActive._getColumnCount() - 1;
						}
						panel._setSelection( panel.nodeActive.iSelection_x, xNewColumn );
					} catch( NumberFormatException ex ) {
						ApplicationController.vShowError_NoModal( "Unable to intepret new Column number [" + panel.jtfArray_y.getText() + "] as an integer" );
						panel.jtfArray_x.setText( String.valueOf( panel.nodeActive.iSelection_y ) );
					} catch( Throwable t ){
						ApplicationController.vUnexpectedError( t, "While setting selection coordinate for y" );
					}
				}
			}
		);

		panel.jtfArray_value.addActionListener(			
			new java.awt.event.ActionListener(){
				public void actionPerformed( ActionEvent event) {
					panel.sbError_local.setLength( 0 );
					if( panel._setSelectedValue( panel.jtfArray_value.getText(), panel.sbError_local ) ){
						panel.panelArray_View._vDrawImage( panel.nodeActive );
					} else {
						panel.jtfArray_value.setText( panel.nodeActive._getValueString_selected() );
					}
				}
			}
		);
		
		return panel;
	}
	
	private class ClickableButton extends JLabel implements java.awt.event.MouseListener {
		Border borderX_up = new SoftBevelBorder( SoftBevelBorder.RAISED );
		Border borderX_down = new SoftBevelBorder( SoftBevelBorder.LOWERED );
		public ClickableButton( String sLabel ){
			super( sLabel );
			setBorder( borderX_up );
		}
		public void mouseReleased( java.awt.event.MouseEvent e ){
			setBorder( borderX_up );
		}
		public void mouseEntered( java.awt.event.MouseEvent e ){}
		public void mouseExited( java.awt.event.MouseEvent e ){}
		public void mousePressed( java.awt.event.MouseEvent e ){
			setBorder( borderX_down );
		}
		public void mouseClicked( java.awt.event.MouseEvent e ){
			if( mzShowingSelectionCoordinates ){
				labelY.setVisible( false );
				jtfArray_x.setVisible( false );
				jtfArray_y.setVisible( false );
				mzShowingSelectionCoordinates = false;
			} else {
				labelY.setVisible( true );
				jtfArray_x.setVisible( true );
				jtfArray_y.setVisible( true );
				mzShowingSelectionCoordinates = true;
			}
			_resize();
		}
	}
	
	private void _resize(){
		int posSelection_x = 6;
		int posSelection_y = 4;
		int pxLabelMargin = 3;
		int pxButtonX_width = 20;
		int pxSelectionJTF_width = 80;
		int pxLabelY_width = 15;
		int pxLabelValue_width = 38;
		int pxExpValue_width = 25;
		buttonX.setBounds( posSelection_x, posSelection_y + 2, pxButtonX_width, 20 );
		jtfArray_x.setBounds( posSelection_x + pxButtonX_width + pxLabelMargin, posSelection_y, pxSelectionJTF_width, 25 );
		int posYLabel_x = posSelection_x + pxButtonX_width + pxLabelMargin + pxSelectionJTF_width + 2;
		labelY.setBounds( posYLabel_x, posSelection_y, pxLabelY_width, 22 );
		jtfArray_y.setBounds( posYLabel_x + pxLabelY_width + pxLabelMargin, posSelection_y, pxSelectionJTF_width, 25 );
		int pxSelection_width;   
		if( mzShowingSelectionCoordinates ){
			pxSelection_width = pxButtonX_width + pxLabelMargin + pxSelectionJTF_width *2 + 2 + pxLabelY_width + 6;   
		} else {
			pxSelection_width = pxButtonX_width + 2;
		}
		
		// value editor
		int pxValueEditor_x = posSelection_x + pxSelection_width + 2;
		labelValue.setBounds( pxValueEditor_x, posSelection_y, pxLabelValue_width, 22 );
		int posJTFArray_x = pxValueEditor_x + pxLabelValue_width + pxLabelMargin;
		int pxJTFArray_width = 160; 
		jtfArray_value.setBounds( posJTFArray_x, posSelection_y, pxJTFArray_width, 25 );
		int pxValueEditor_width = pxLabelValue_width + pxLabelMargin + pxJTFArray_width; 
		
		// expression one-liner and mode combo
		int pxExpressionEditor_x = pxValueEditor_x + pxValueEditor_width + 2;
		labelExp.setBounds( pxExpressionEditor_x, posSelection_y, pxExpValue_width, 22 );
		int posJTFExp_x = pxExpressionEditor_x + pxExpValue_width;
		int pxRangeModeCombo_width = 75;
		int pxRightHandMargin = 6;
		int pxJTFexp_width = this.getWidth() - posJTFExp_x - pxRangeModeCombo_width - pxRightHandMargin;
		jtfArray_exp.setBounds( posJTFExp_x, posSelection_y, pxJTFexp_width, 25 );
		jcbArray_exp_range.setBounds( posJTFExp_x + pxJTFexp_width + 3, posSelection_y, pxRangeModeCombo_width, 25 );
	}
	
	public void componentHidden( ComponentEvent e ){ _resize(); }
	public void componentMoved( ComponentEvent e ){ _resize(); }
	public void componentResized( ComponentEvent e ){ _resize(); }
	public void componentShown( ComponentEvent e ){ _resize(); }

	public static final Border borderBlue = BorderFactory.createLineBorder( Color.BLUE ); 
	public void focusGained( java.awt.event.FocusEvent e ){
		setBorder( borderBlue );
	}

	public void focusLost( java.awt.event.FocusEvent e ){
		setBorder( null );
	}
	
	public void _show( Node node ){
		if( node == null ){
			setVisible( false );
			return;
		}
		if( node.getType() == DAP_VARIABLE.Array ){
			setVisible( true );
			nodeActive = (Node_Array)node;
			_setSelection( nodeActive.iSelection_x, nodeActive.iSelection_y );
			return;
		} else {
			setVisible( false );
			return;
		}
	}
	void _setSelection( int row, int column ){
		if( nodeActive == null ) return;
		nodeActive.iSelection_x = row;
		nodeActive.iSelection_y = column;
		jtfArray_x.setText( Integer.toString( row ) );
		jtfArray_y.setText( Integer.toString( column ) );
		jtfArray_value.setText( nodeActive._getValueString( row, column ) );
		panelArray_View._vDrawImage( nodeActive );
	}
	
	boolean _setSelectedValue( String sNewValue, StringBuffer sbError ){
		return nodeActive._setSelectedValue( sNewValue, sbError );
	}
	
	public boolean _isShowingSelectionCoordinates(){ return mzShowingSelectionCoordinates; } 
}

// stores information about how the dataset should be viewed (column widths etc)
class Model_DatasetView {
}

class Model_VariableView {
	int array_origin_x = 0;
	int array_origin_y = 0;
	int array_cursor_x = 0;
	int array_cursor_y = 0;
	int array_dim_x = 1;
	int array_dim_y = 2;
	int[] array_page = new int[10];
}

