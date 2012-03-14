package opendap.clients.odc.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.BoxLayout;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DAP;
import opendap.clients.odc.data.Node;
import opendap.clients.odc.data.Node_Array;
import opendap.dap.BaseType;

// see Panel_View_Data for layout
// parent: Panel_Define_Dataset

public class Panel_Edit_Variable extends JPanel {
	private Panel_Define_Dataset mParent;
	private Node nodeActive = null;
	private Panel_Edit_VariableEditor mActiveEditor;
	private Panel_Edit_Variable_root editRoot;
	private Panel_Edit_Variable_Array editArray;
	private Panel_Edit_Variable_Grid editGrid;
	private Panel_Edit_Variable_Sequence editSequence;
	private Panel_Edit_Variable_Structure editStructure;
	private Panel_Edit_Variable_Primitive editPrimitive;
	private Panel_Edit_Variable(){};
	static Panel_Edit_Variable _create( Panel_Define_Dataset parent, Panel_Edit_ViewStructure view, StringBuffer sbError ){
		Panel_Edit_Variable panel = new Panel_Edit_Variable();
		try {
			panel.mParent = parent;
			panel.setLayout( new java.awt.BorderLayout() );
			panel.editRoot = new Panel_Edit_Variable_root();
			if( ! panel.editRoot._zInitialize( view, sbError ) ){
				sbError.insert( 0, "error initializing root editor: " );
				return null;
			}
			panel.editArray = new Panel_Edit_Variable_Array();
			if( ! panel.editArray._zInitialize( view, sbError ) ){
				sbError.insert( 0, "error initializing array editor: " );
				return null;
			}
			panel.editGrid = new Panel_Edit_Variable_Grid();
			if( ! panel.editGrid._zInitialize( view, sbError ) ){
				sbError.insert( 0, "error initializing grid editor: " );
				return null;
			}
			panel.editSequence = new Panel_Edit_Variable_Sequence();
			if( ! panel.editSequence._zInitialize( view, sbError ) ){
				sbError.insert( 0, "error initializing structure editor: " );
				return null;
			}
			panel.editStructure = new Panel_Edit_Variable_Structure();
			if( ! panel.editStructure._zInitialize( view, sbError ) ){
				sbError.insert( 0, "error initializing structure editor: " );
				return null;
			}
			panel.editPrimitive = new Panel_Edit_Variable_Primitive();
			if( ! panel.editPrimitive._zInitialize( view, sbError ) ){
				sbError.insert( 0, "error initializing primitive editor: " );
				return null;
			}
			return panel;
		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError( ex, sbError );
			return null;
		}
	}
	public void _showVariable( Node node ){
		if( mActiveEditor != null || node == null ){
			mActiveEditor._clear();
			remove( mActiveEditor );
		}
		nodeActive = node;
		if( nodeActive.isRoot() ){
			mActiveEditor = editRoot;
		} else {
			BaseType bt = nodeActive.getBaseType();
			switch( DAP.getType( bt ) ){
				case Array:
					mActiveEditor = editArray;
					break;
				case Grid:
					mActiveEditor = editGrid;
					break;
				case Sequence:
					mActiveEditor = editSequence;
					break;
				case Structure:
					mActiveEditor = editStructure;
					break;
				case Byte:
				case Int16:
				case UInt16:
				case Int32:
				case UInt32:
				case Float32:
				case Float64:
				case String:
					mActiveEditor = editPrimitive;
					System.out.println( "showing primitive editor" );
					break;
				default:
					mActiveEditor = null; // should not happen
					ApplicationController.vShowError_NoModal( "internal error, unknown/unsupported data type to edit: " + bt.getTypeName() );
					return;
			}
		}
		add( mActiveEditor, BorderLayout.CENTER );
		mActiveEditor._show( node );
	}
}

abstract class Panel_Edit_VariableEditor extends JPanel {
	private Node node_active = null;
	protected Panel_Edit_ViewStructure view;
	protected JLabel labelType = new JLabel( "Type:" );
	protected JLabel labelType_value = new JLabel( "" );
	protected JLabel labelName = new JLabel( "Name:" );
	protected JLabel labelName_Long = new JLabel( "Long Name:" );
	protected JLabel displayName_Long = new JLabel();  // long name is not editable (automatically determined by dataset structure)
	protected JLabel labelName_Clear = new JLabel( "Encoded Name:" );
	protected JTextField jtfName = new JTextField();
	protected JLabel labelName_Encoded = new JLabel();
	protected FormLayout layout;
	boolean _zInitialize( Panel_Edit_ViewStructure structure_view, StringBuffer sbError ){
		try {
			this.view = structure_view;
			jtfName.setPreferredSize( new Dimension( 200, 28 ) );
			labelName_Encoded.setPreferredSize( new Dimension( 200, 20 ) );
			labelType_value.setFont( Styles.fontFixed12 );
			
			layout = new FormLayout( this );
			setLayout( layout );
			layout.setMargin( 4, 4 );
			layout.add( labelName, jtfName );
			layout.add( labelType, labelType_value );
			layout.add( labelName_Long, displayName_Long );
			layout.add( labelName_Clear, labelName_Encoded );
			
			jtfName.addFocusListener(
				new java.awt.event.FocusAdapter(){
					public void focusLost(java.awt.event.FocusEvent evt) {
						String sNewName = jtfName.getText();
						if( ! sNewName.equals( jtfName.getName() ) ) _setName( sNewName, view );
					}
				}
			);
			jtfName.addActionListener(       // occurs if user clicks enter key
				new java.awt.event.ActionListener(){
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						String sNewName = jtfName.getText();
						if( ! sNewName.equals( jtfName.getName() ) ) _setName( sNewName, view );
					}
				}
			);

			return true;
		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}
	void _clear(){ // release all references
		node_active = null;
	}
	void _show( Node node ){ // activate the node for editing
		if( node == null ){ // blank screen
			jtfName.setText( "" );
			displayName_Long.setText( "" );
			labelName_Encoded.setText( "" );
			labelType_value.setText( "" );
		} else if( node.isRoot() ) {
			opendap.dap.DataDDS ddds = view._getModel().getSource().getData();
			if( ddds == null ){
				jtfName.setText( "" );
				displayName_Long.setText( "" );
				labelName_Encoded.setText( "" );
			} else {
				jtfName.setText( ddds.getClearName() );
				displayName_Long.setText( ddds.getLongName() );
				labelName_Encoded.setText( ddds.getName() );
			}
			labelType_value.setText( "Structure (root node)" );
		} else {
			BaseType bt = node.getBaseType();
			jtfName.setText( bt.getClearName() );
			displayName_Long.setText( bt.getLongName() );
			labelName_Encoded.setText( bt.getName() );
			labelType_value.setText( DAP.getType_String( bt ) );
		}
		node_active = node;
	}
	void _setName( String sNewName, Panel_Edit_ViewStructure view ){
		try {
			if( node_active == null ){ // blank screen
				ApplicationController.vShowWarning( "unable to set name, no model is active" );
			} else {
				StringBuffer sbError = new StringBuffer();
				if( ! node_active._setName( sNewName, sbError ) ){
					ApplicationController.vShowError( "Failed to set name to [" + sNewName + "]: " + sbError.toString() );
				}
			}
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "Error changing dimension name: " + t );
		}
	}
}

class Panel_Edit_Variable_root extends Panel_Edit_VariableEditor {
	void _show( Node node ){ // will be null
		super._show( node );
	}
	boolean _zInitialize( Panel_Edit_ViewStructure structure_view, StringBuffer sbError ){
		super._zInitialize( structure_view, sbError );
		return true;
	}
}

class Panel_Edit_Variable_Array extends Panel_Edit_VariableEditor {
	private Node_Array nodeActive = null;
	public static final int MAX_DIMENSIONS = 10;
	public static final int INDENT = 10;
	public static final int LABEL_MARGIN = 4;
	public static final int BUTTON_MARGIN = 7;
	public static final int BUTTON_SPACING = 5;
	public static final int COLUMN_SPACING = 6;
	private JLabel labelValueCount = new JLabel( "Value Count:" );
	private JLabel labelValueCount_value = new JLabel( "0" );
	private JLabel labelValueType = new JLabel( "Value Type:" );
	private JComboBox jcbType;
	private JLabel labelDimensions = new JLabel( "Dimensions:" );
	private JButton buttonAddDimension = new JButton( "New" );
	private ArrayList<JPanel> listDimensionPanel = new ArrayList<JPanel>(); 
	private ArrayList<JTextField> listDimensionNameJTF = new ArrayList<JTextField>(); 
	private ArrayList<JTextField> listDimensionSizeJTF = new ArrayList<JTextField>();
	private ArrayList<JButton> listDimensionDelete = new ArrayList<JButton>(); 
	private ArrayList<JButton> listDimensionUp = new ArrayList<JButton>(); 
	private ArrayList<JButton> listDimensionDown = new ArrayList<JButton>(); 
	void _show( Node node ){
		super._show( node );
		nodeActive = (Node_Array)node;
		labelValueCount_value.setText( Integer.toString( nodeActive._getValueCount() ) );
		jcbType.setSelectedItem( DAP.getTypeEnumByName( nodeActive._getValueTypeString() ) );
		int xDimension1 = 1;
		int ctDimension = nodeActive._getDimensionCount();
		for( ; xDimension1 <= ctDimension; xDimension1++ ){
			listDimensionNameJTF.get( xDimension1 - 1 ).setText( nodeActive._getDimensionName( xDimension1 ) );
			listDimensionSizeJTF.get( xDimension1 - 1 ).setText( Integer.toString( nodeActive._getDimensionLength( xDimension1 ) ) );
			listDimensionDelete.get( xDimension1 - 1 ).setVisible( ctDimension > 1 );
			listDimensionUp.get( xDimension1 - 1 ).setVisible( xDimension1 > 1 );
			listDimensionDown.get( xDimension1 - 1 ).setVisible( xDimension1 < ctDimension );
			listDimensionPanel.get( xDimension1 - 1 ).setVisible( true );
		}
		for( ; xDimension1 <= MAX_DIMENSIONS; xDimension1++ ){
			listDimensionPanel.get( xDimension1 - 1).setVisible( false );
		}
		view._getVariableView()._show( node );
	}
	boolean _zInitialize( Panel_Edit_ViewStructure structure_view, StringBuffer sbError ){
		super._zInitialize( structure_view, sbError );
		Class<?> class_DAP_TYPE = DAP.DAP_TYPE.class;
		jcbType = new JComboBox( class_DAP_TYPE.getEnumConstants() ) ;
		layout.add( labelValueCount, labelValueCount_value );  
		layout.add( labelValueType, jcbType );  
		layout.add( labelDimensions, buttonAddDimension );
		listDimensionPanel.clear();
		listDimensionNameJTF.clear();
		listDimensionSizeJTF.clear();
		listDimensionDelete.clear();
		listDimensionUp.clear();
		listDimensionDown.clear();
		Dimension dimName_preferred = new Dimension( 120, 25 );
		Dimension dimSize_preferred = new Dimension( 50, 25 );
		labelValueCount_value.setFont( Styles.fontFixed12 );
		for( int xDimension1 = 1; xDimension1 <= MAX_DIMENSIONS; xDimension1++ ){
			final JPanel panelDimensionEditor = new JPanel();
			final JTextField jtfDimensionName = new JTextField();
			final JTextField jtfDimensionSize = new JTextField();
			final JButton buttonDelete = new JButton( new ImageIcon( Resources.imageNavigateMinus ) );
			final JButton buttonUp = new JButton( new ImageIcon( Resources.imageArrowUp ) );
			final JButton buttonDown = new JButton( new ImageIcon( Resources.imageArrowDown ) );
			jtfDimensionName.setPreferredSize( new Dimension( dimName_preferred ) );
			jtfDimensionSize.setPreferredSize( new Dimension( dimSize_preferred ) );
			listDimensionNameJTF.add( jtfDimensionName );
			listDimensionSizeJTF.add( jtfDimensionSize );
			listDimensionDelete.add( buttonDelete );
			listDimensionUp.add( buttonUp );
			listDimensionDown.add( buttonDown );
			panelDimensionEditor.setLayout( new BoxLayout( panelDimensionEditor, BoxLayout.X_AXIS ) );
			panelDimensionEditor.add( new JLabel( "Size:" ) );
			panelDimensionEditor.add( Box.createHorizontalStrut( LABEL_MARGIN ) );
			panelDimensionEditor.add( jtfDimensionSize ); 
			panelDimensionEditor.add( Box.createHorizontalStrut( COLUMN_SPACING ) );
			panelDimensionEditor.add( new JLabel( "Name:" ) );
			panelDimensionEditor.add( Box.createHorizontalStrut( LABEL_MARGIN ) );
			panelDimensionEditor.add( jtfDimensionName );
			panelDimensionEditor.add( Box.createHorizontalStrut( BUTTON_MARGIN ) );
			panelDimensionEditor.add( buttonDelete );
			panelDimensionEditor.add( Box.createHorizontalStrut( BUTTON_SPACING ) );
			panelDimensionEditor.add( buttonUp );
			panelDimensionEditor.add( Box.createHorizontalStrut( BUTTON_SPACING ) );
			panelDimensionEditor.add( buttonDown );
			listDimensionPanel.add( panelDimensionEditor );
			layout.add( Box.createHorizontalStrut( INDENT ), panelDimensionEditor );  
			
			final int xDimension_final1 = xDimension1;
			jtfDimensionName.addFocusListener(
				new java.awt.event.FocusAdapter(){
					public void focusLost(java.awt.event.FocusEvent evt) {
						// do nothing
					}
				}
			);
			jtfDimensionName.addActionListener(       // occurs if user clicks enter key
				new java.awt.event.ActionListener(){
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						String sNewName = jtfDimensionName.getText();
						if( ! sNewName.equals( nodeActive.getName() ) ) setDimensionName( xDimension_final1, sNewName );
					}
				}
			);
			jtfDimensionSize.addFocusListener(
				new java.awt.event.FocusAdapter(){
					public void focusLost(java.awt.event.FocusEvent evt) {
						// do nothing
					}
				}
			);
			jtfDimensionSize.addActionListener(       // occurs if user clicks enter key
				new java.awt.event.ActionListener(){
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						String sNewSize = jtfDimensionSize.getText();
						if( ! sNewSize.equals( nodeActive._getDimensionLength( xDimension_final1 ) ) ) setDimensionSize( xDimension_final1, sNewSize );
					}
				}
			);
			buttonDelete.addActionListener( 
				new java.awt.event.ActionListener(){
					public void actionPerformed( java.awt.event.ActionEvent evt ){
						JOptionPane.showMessageDialog( Panel_Edit_Variable_Array.this, "not implemented" );
					}
				}
			);
		}
		buttonAddDimension.addActionListener( 
			new java.awt.event.ActionListener(){
				public void actionPerformed( java.awt.event.ActionEvent evt ){
					addDimension();
				}
			}
		);
		return true;
	}
	void addDimension(){
		StringBuffer sbError = new StringBuffer( 256 );
		if( nodeActive._addDimension(sbError) ){
			_show( nodeActive );
		} else {
			ApplicationController.vShowError( "Error adding new dimension: " + sbError );
		}
	}
	void setDimensionName( int xDimension1, String sNewName ){
		StringBuffer sbError = new StringBuffer( 256 );
		if( nodeActive._setDimensionName( xDimension1, sNewName, sbError ) ){
			ApplicationController.vShowStatus( "Changed dimension " + xDimension1 + " name to " + sNewName );
		} else {
			ApplicationController.vShowError( "Error changing dimension name: " + sbError );
		}
		_show( nodeActive );
		System.out.println( "showed active" );
	}
	void setDimensionSize( int xDimension1, String sNewSize ){
		try {
			int iNewSize = Integer.parseInt( sNewSize );
			StringBuffer sbError = new StringBuffer( 256 );
			if( nodeActive._setDimensionSize( xDimension1, iNewSize, sbError ) ){
				ApplicationController.vShowStatus( "Changed dimension " + xDimension1 + " size to " + iNewSize );
			} else {
				ApplicationController.vShowError( "Error changing size of dimension " + xDimension1 + ": " + sbError );
			}
			_show( nodeActive ); 
		} catch( NumberFormatException e ) {
			ApplicationController.vShowError( "New size: " + sNewSize + " could not be interpreted as an integer." );			
		} catch( Throwable t ) {
			ApplicationController.vShowError( "Error changing size of dimension " + xDimension1 + ": " + t );
		}
		_show( nodeActive );
	}
}
class Panel_Edit_Variable_Grid extends Panel_Edit_VariableEditor {
	void _show( Node node ){
		super._show( node );
	}
	boolean _zInitialize( Panel_Edit_ViewStructure structure_view, StringBuffer sbError ){
		super._zInitialize( structure_view, sbError );
		return true;
	}
}
class Panel_Edit_Variable_Sequence extends Panel_Edit_VariableEditor {
	void _show( Node node ){
		super._show( node );
	}
	boolean _zInitialize( Panel_Edit_ViewStructure structure_view, StringBuffer sbError ){
		super._zInitialize( structure_view, sbError );
		return true;
	}
}
class Panel_Edit_Variable_Structure extends Panel_Edit_VariableEditor {
	void _show( Node node ){
		super._show( node );
	}
	boolean _zInitialize( Panel_Edit_ViewStructure structure_view, StringBuffer sbError ){
		super._zInitialize( structure_view, sbError );
		return true;
	}
}
class Panel_Edit_Variable_Primitive extends Panel_Edit_VariableEditor {
	private JLabel labelType = new JLabel( "Type:" );
	private JComboBox jcbType;
	void _show( Node node ){
		super._show( node );
		jcbType.setSelectedItem( DAP.getTypeEnumByName( node.getBaseType().getTypeName() ) );
	}
	boolean _zInitialize( Panel_Edit_ViewStructure structure_view, StringBuffer sbError ){
		super._zInitialize( structure_view, sbError );
		Class<?> class_DAP_TYPE = DAP.DAP_TYPE.class;
		jcbType = new JComboBox( class_DAP_TYPE.getEnumConstants() ) ;
		layout.add( labelType, jcbType );  
		return true;
	}
}
