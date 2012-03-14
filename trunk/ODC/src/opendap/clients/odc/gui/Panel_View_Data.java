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

package opendap.clients.odc.gui;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.data.Model_Dataset;
import opendap.clients.odc.data.Model_LoadedDatasets;
import opendap.clients.odc.data.Script;
import opendap.clients.odc.data.Model_Dataset.DATASET_TYPE;
import opendap.clients.odc.IControlPanel;
import opendap.clients.odc.Interpreter;
import opendap.clients.odc.SavableImplementation;
import opendap.dap.DataDDS;

import java.util.HashMap;

import java.beans.PropertyChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JSplitPane;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import java.util.ArrayList;

/** Panel_LoadedDatasets    dataset combo box and action buttons
 *  Model_DataView          key actions (new dataset, delete dataset, etc)
 *
 *  NORTH: Panel_LoadedDatasets  (contains command buttons for file loading etc, in this file)
 *
 * contains the editing panels (in Panel_Edit_Container):
 * 
 * Data -        left: Panel_Edit_StructureView      right: Panel_Define_Dataset
 *                                                              Panel_Edit_Dataset
 *                                                              Panel_Edit_Variable
 * Expression -  left: Panel_Edit_Expression         right: Panel_Define_Expression
 * Stream -      left: Panel_Edit_Stream             right: Panel_Define_Stream
 * 
 * Panel below (South):
 * 
 *   Panel_VarView (North)
 *   Panel_View_Variable (South) (contains Panel_Edit_ViewArray)
 *     or panelPreviewPane
 *     
 * variable activation:
 * 
 *   Panel_Edit_ViewStructure._getVariableView()._show( node )  [see Panel_Edit_Variable_Array]
 *   
 * expression activation:
 * 
 */

public class Panel_View_Data extends JPanel implements IControlPanel {
	Model_DataView modelDataView = null;
	Panel_LoadedDatasets panelLoadedDatasets;
	Panel_Edit_Container panelEditContainer;
	opendap.clients.odc.plot.PreviewPane panelPreviewPane;
	public Panel_View_Variable panelVarView;
	boolean mzAddingItemToList = false; // this is flag used to prevent the add from triggering an item activation
	private JSplitPane msplitViewData;
	private HashMap<Model_Dataset,Integer> hashmapVerticalSplit; // used to store divider location
    public Panel_View_Data() {}
    
	public boolean _zInitialize( Model_LoadedDatasets data_list, StringBuffer sbError ){
		try {
			msplitViewData = new JSplitPane( JSplitPane.VERTICAL_SPLIT );

			panelLoadedDatasets = new Panel_LoadedDatasets();
			panelVarView = Panel_View_Variable._create( sbError );	
			if( panelVarView == null ){
				sbError.insert( 0, "failed to create var view: " );
				return false;
			}
			panelPreviewPane = new opendap.clients.odc.plot.PreviewPane(); 
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
			msplitViewData.setTopComponent( panelTop );
			msplitViewData.setContinuousLayout( true );
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
			case Data:
				if( _zSetModel( modelDataset, sbError ) ){
					// ApplicationController.vShowStatus( "activated dataset model: " + modelDataset.getTitle() );
					modelDataView.modelActive = modelDataset;
					msplitViewData.setBottomComponent( panelVarView );
					msplitViewData.setDividerLocation( 0.5d );
				} else {
					ApplicationController.vShowError( "(Panel_View_Data) internal error while setting model for dataset: " + sbError );
					return;
				}
				break;
			case PlottableExpression:
			case Text:
				if( _zSetModel( modelDataset, sbError ) ){
					// ApplicationController.vShowStatus( "activated expression model: " + modelDataset.getTitle() );
					modelDataView.modelActive = modelDataset;
					msplitViewData.setBottomComponent( panelPreviewPane );
					msplitViewData.setDividerLocation( 0.5d );
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

	void _vClear(){
		panelEditContainer._vClear();
	}
	
	public opendap.clients.odc.plot.PreviewPane _getPreviewPane(){
		return panelPreviewPane;
	}
	
	public void _vSetFocus(){    	
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
			StringBuffer sbError = new StringBuffer( 256 );
			Model_Dataset model = Model_Dataset.createExpression( sbError );
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
	void action_LoadLast(){
		try {
			StringBuffer sbError = new StringBuffer();
			String sPath_LastLoadedData = ConfigurationManager.getInstance().getOption( ConfigurationManager.PROPERTY_PATH_LastLoadedData );
			if( sPath_LastLoadedData == null || sPath_LastLoadedData.trim().length() == 0 ){
				ApplicationController.vShowError( "no known last loaded data (running read only?)" );
				return;
			}
			java.io.File file = new java.io.File( sPath_LastLoadedData );
			if( ! file.exists() ){
				ApplicationController.vShowError( "the last loaded data file (" + sPath_LastLoadedData + ") no longer exists at that location." );
				return;
			}
			SavableImplementation savable = new SavableImplementation( opendap.clients.odc.data.Model_Dataset.class, null, null );
			Model_Dataset model = (Model_Dataset)savable._open( file, sbError );
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
	void action_Load(){
		try {
			SavableImplementation savable = new SavableImplementation( opendap.clients.odc.data.Model_Dataset.class, null, null );
			Model_Dataset model = (Model_Dataset)savable._open();
			if( model == null ) return; // user cancelled (or there was an error which has already been reported)
			switch( model.getType() ){
				case Data:
				case PlottableExpression:
				case Text:
					break; // these are all supported types
				default:
					ApplicationController.vShowError( "The data type of this model " + model.getType() + " cannot be opened from this view." );
					return;
			}
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
		if( mDatasetList.getSize() == 0 ){
			mParent._vClear();
		} else {
			Model_Dataset model = (Model_Dataset)mDatasetList.getSelectedItem();
			mParent._vActivate( model );
		}
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
			SavableImplementation savable = modelActive.getSavable();
			if( savable == null ){
				ApplicationController.vShowError( "internal error, no savable implementation defined for " + modelActive );
				return;
			}
			savable._saveAs( modelActive );
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
				ApplicationController.vUnexpectedError( t, sbError );
			}
		}
	}
	void action_GenerateDatasetFromExpression(){
		if( modelActive == null ){
			ApplicationController.vShowStatus_NoCache( "no expression selected for generation" );
		} else if( modelActive.getType() != DATASET_TYPE.DataScript ){
			ApplicationController.vShowStatus_NoCache( "selected dataset is not a data-generating expression" );
		} else {
			StringBuffer sbError = new StringBuffer( 256 );
			String sExpressionText = this.modelActive.getTextContent();
			Interpreter interpreter = ApplicationController.getInstance().getInterpreter();
			Script script = interpreter.generateScriptFromText( sExpressionText, sbError );
			if( script == null ){
				ApplicationController.vShowError( "Failed to analyze script text " + this.modelActive.getTitle() + ": " + sbError );
				return;
			}
			Model_Dataset generated_dataset = script.generateDataset( sbError );
			if( generated_dataset == null ){
				ApplicationController.vShowError( "Failed to generate dataset from expression " + this.modelActive.getTitle() + ": " + sbError );
				return;
			}
			addDataset_DataDDS( generated_dataset, this.modelActive.getTitle() + "_" );
		}
	}
	void action_PlotFromExpression(){
		if( modelActive == null ){
			ApplicationController.vShowError( "No expression selected for plotting." );
		} else if( modelActive.getType() != DATASET_TYPE.PlottableExpression && modelActive.getType() != DATASET_TYPE.Text ){
			ApplicationController.vShowError( "Selected dataset is not a plottable expression (or text)." );
		} else {
			StringBuffer sbError = new StringBuffer( 256 );
			opendap.clients.odc.plot.Panel_View_Plot plotter = ApplicationController.getInstance().getAppFrame().getPlotter();
			if( ! plotter.zPlotExpressionToPreview( modelActive, sbError ) ){
				ApplicationController.vShowError( "Failed to plot expression " + this.modelActive.getTitle() + ": " + sbError );
				return;
			}
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
		JButton buttonLoadLast = new JButton( "Load Last" ); buttonLoadLast.setToolTipText( "Loads last loaded data file" ); 
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
		this.add( buttonLoadLast );
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
		buttonLoadLast.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed(ActionEvent event) {
					modelDataView.action_LoadLast();
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
		if( model.getType() != DATASET_TYPE.Stream ){
			sbError.append( "supplied model is not a stream" );
			return false;
		}
		mModel = model;
		return true;
	}
	Panel_Define_Stream _getParent(){ return mParent; }
}


