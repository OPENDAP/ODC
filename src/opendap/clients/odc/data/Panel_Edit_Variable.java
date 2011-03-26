package opendap.clients.odc.data;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Enumeration;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.BoxLayout;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DAP;
import opendap.clients.odc.FormLayout;
import opendap.clients.odc.Utility_String;
import opendap.dap.BaseType;
import opendap.dap.DArray;
import opendap.dap.DArrayDimension;
import opendap.dap.DGrid;
import opendap.dap.DSequence;
import opendap.dap.DStructure;
import opendap.dap.test.dap_test;

public class Panel_Edit_Variable extends JPanel {
	private Panel_Define_Dataset mParent;
	private BaseType bt_active = null;
	private Panel_Edit_VariableEditor mActiveEditor;
	private Panel_Edit_Variable_Array editArray;
	private Panel_Edit_Variable_Grid editGrid;
	private Panel_Edit_Variable_Sequence editSequence;
	private Panel_Edit_Variable_Structure editStructure;
	private Panel_Edit_Variable_Primitive editPrimitive;
	private Panel_Edit_Variable(){};
	static Panel_Edit_Variable _create( Panel_Define_Dataset parent, Panel_Edit_StructureView view, StringBuffer sbError ){
		Panel_Edit_Variable panel = new Panel_Edit_Variable();
		try {
			panel.mParent = parent;
			panel.setLayout( new java.awt.BorderLayout() );
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
	public void _showVariable( BaseType bt ){
		if( mActiveEditor != null ){
			mActiveEditor._clear();
			remove( mActiveEditor );
		}
		bt_active = bt;
		switch( DAP.getType( bt ) ){
			case Array:
				mActiveEditor = editArray;
				System.out.println( "showing array editor" );
				break;
			case Grid:
				mActiveEditor = editGrid;
				System.out.println( "showing grid editor" );
				break;
			case Sequence:
				mActiveEditor = editSequence;
				System.out.println( "showing sequence editor" );
				break;
			case Structure:
				mActiveEditor = editStructure;
				System.out.println( "showing structure editor, layout " + editStructure.layout + " with " + editStructure.layout.listDefinedElements.size() + " elements" );
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
		add( mActiveEditor, BorderLayout.CENTER );
		mActiveEditor._show( bt );
	}
}

abstract class Panel_Edit_VariableEditor extends JPanel {
	private BaseType bt_active = null;
	private Panel_Edit_StructureView view;
	protected JLabel labelName = new JLabel( "Name:" );
	protected JLabel labelName_Long = new JLabel( "Long Name:" );
	protected JLabel displayName_Long = new JLabel();  // long name is not editable (automatically determined by dataset structure)
	protected JLabel labelName_Clear = new JLabel( "Clear Name:" );
	protected JTextField jtfName = new JTextField();
	protected JTextField jtfName_Clear = new JTextField();
	protected FormLayout layout;
	boolean _zInitialize( Panel_Edit_StructureView structure_view, StringBuffer sbError ){
		try {
			this.view = structure_view;
			jtfName.setPreferredSize( new Dimension( 200, 20 ) );
			jtfName_Clear.setPreferredSize( new Dimension( 200, 20 ) );
			
			layout = new FormLayout( this );
			setLayout( layout );
			layout.setMargin( 4, 4 );
			layout.add( labelName, jtfName );
			layout.add( labelName_Long, displayName_Long );
			layout.add( labelName_Clear, jtfName_Clear );
			
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
		bt_active = null;
	}
	void _show( BaseType bt ){ // activate the base type for editing
		jtfName.setText( bt.getName() );
		displayName_Long.setText( bt.getLongName() );
		jtfName_Clear.setText( bt.getClearName() );
		bt_active = bt;
	}
	void _setName( String sNewName, Panel_Edit_StructureView view ){
		try {
			bt_active.setName( sNewName );
			view._update( bt_active );
		} catch( Throwable t ) {
			ApplicationController.vShowError( "Error changing dimension name: " + t );
		}
	}
}

class Panel_Edit_Variable_Array extends Panel_Edit_VariableEditor {
	private DArray btArray_active = null;
	public static final int MAX_DIMENSIONS = 10;
	public static final int INDENT = 10;
	public static final int LABEL_MARGIN = 4;
	public static final int COLUMN_SPACING = 6;
	private JLabel labelType = new JLabel( "Value Type:" );
	private JComboBox jcbType;
	private JLabel labelDimensions = new JLabel( "Dimensions:" );
	private JButton buttonAddDimension = new JButton( "New" );
	private ArrayList<JPanel> listDimensionPanel = new ArrayList<JPanel>(); 
	private ArrayList<JTextField> listDimensionNameJTF = new ArrayList<JTextField>(); 
	private ArrayList<JTextField> listDimensionSizeJTF = new ArrayList<JTextField>(); 
	void _show( BaseType bt ){
		super._show( bt );
		btArray_active = (DArray)bt;
		jcbType.setSelectedItem( DAP.getTypeEnumByName( bt.getTypeName() ) );
		int xDimension = 1;
		for( ; xDimension <= btArray_active.getLength(); xDimension++ ){
			opendap.dap.DArrayDimension dimension;
			try {
				dimension = btArray_active.getDimension( xDimension - 1 );
			} catch( Throwable t ) {
				continue; // TODO issue error
			}
			listDimensionPanel.get( xDimension - 1).setVisible( true );
			listDimensionNameJTF.get( xDimension - 1 ).setVisible( true );
			listDimensionNameJTF.get( xDimension - 1 ).setText( dimension.getName() );
			listDimensionSizeJTF.get( xDimension - 1 ).setVisible( true );
			listDimensionSizeJTF.get( xDimension - 1 ).setText( Integer.toString( dimension.getSize() ) );
		}
		for( ; xDimension <= MAX_DIMENSIONS; xDimension++ ){
			listDimensionPanel.get( xDimension - 1).setVisible( false );
		}
	}
	boolean _zInitialize( Panel_Edit_StructureView structure_view, StringBuffer sbError ){
		super._zInitialize( structure_view, sbError );
		Class<?> class_DAP_TYPE = DAP.DAP_TYPE.class;
		jcbType = new JComboBox( class_DAP_TYPE.getEnumConstants() ) ;
		layout.add( labelType, jcbType );  
		layout.add( labelDimensions, buttonAddDimension );
		listDimensionPanel.clear();
		listDimensionNameJTF.clear();
		listDimensionSizeJTF.clear();
		for( int xDimension = 1; xDimension <= MAX_DIMENSIONS; xDimension++ ){
			final JPanel panelDimensionEditor = new JPanel();
			final JTextField jtfDimensionName = new JTextField();
			final JTextField jtfDimensionSize = new JTextField(); 
			listDimensionNameJTF.add( jtfDimensionName );
			listDimensionSizeJTF.add( jtfDimensionSize );
			panelDimensionEditor.setLayout( new BoxLayout( panelDimensionEditor, BoxLayout.X_AXIS ) );
			panelDimensionEditor.add( new JLabel( "Name:" ) );
			panelDimensionEditor.add( Box.createHorizontalStrut( LABEL_MARGIN ) );
			panelDimensionEditor.add( jtfDimensionName );
			panelDimensionEditor.add( Box.createHorizontalStrut( COLUMN_SPACING ) );
			panelDimensionEditor.add( new JLabel( "Size:" ) );
			panelDimensionEditor.add( Box.createHorizontalStrut( LABEL_MARGIN ) );
			panelDimensionEditor.add( jtfDimensionSize ); 
			listDimensionPanel.add( panelDimensionEditor );
			layout.add( Box.createHorizontalStrut( INDENT ), panelDimensionEditor );  
			
			final int xDimension_final = xDimension;
			jtfDimensionName.addFocusListener(
				new java.awt.event.FocusAdapter(){
					public void focusLost(java.awt.event.FocusEvent evt) {
						String sNewName = jtfDimensionName.getText();
						if( ! sNewName.equals( btArray_active.getName() ) ) setDimensionName( xDimension_final, sNewName );
					}
				}
			);
			jtfDimensionName.addActionListener(       // occurs if user clicks enter key
				new java.awt.event.ActionListener(){
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						String sNewName = jtfDimensionName.getText();
						if( ! sNewName.equals( btArray_active.getName() ) ) setDimensionName( xDimension_final, sNewName );
					}
				}
			);
			jtfDimensionSize.addFocusListener(
				new java.awt.event.FocusAdapter(){
					public void focusLost(java.awt.event.FocusEvent evt) {
						String sNewSize = jtfDimensionSize.getText();
						if( ! sNewSize.equals( btArray_active.getName() ) ) setDimensionSize( xDimension_final, sNewSize );
					}
				}
			);
			jtfDimensionSize.addActionListener(       // occurs if user clicks enter key
				new java.awt.event.ActionListener(){
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						String sNewSize = jtfDimensionSize.getText();
						if( ! sNewSize.equals( btArray_active.getName() ) ) setDimensionSize( xDimension_final, sNewSize );
					}
				}
			);
		}
		return true;
	}
	void setDimensionName( int xDimension0, String sNewName ){
		try {
			opendap.dap.DArrayDimension dimension = btArray_active.getDimension( xDimension0 );
			dimension.setName( sNewName );
		} catch( Throwable t ) {
			ApplicationController.vShowError( "Error changing dimension name: " + t );
		}
	}
	void setDimensionSize( int xDimension0, String sNewSize ){
		try {
			int iNewSize = Integer.parseInt( sNewSize );
			if( iNewSize < 1 || iNewSize > 9 ){
				ApplicationController.vShowError( "New size: " + sNewSize + " is invalid. It must be a positive integer greater than 0 and less than 10." );			
			}
			opendap.dap.DArrayDimension dimension = btArray_active.getDimension( xDimension0 );
			dimension.setSize( iNewSize );
		} catch( NumberFormatException e ) {
			ApplicationController.vShowError( "New size: " + sNewSize + " could not be interpreted as an integer." );			
		} catch( Throwable t ) {
			ApplicationController.vShowError( "Error changing dimension size: " + t );
		}
	}
}
class Panel_Edit_Variable_Grid extends Panel_Edit_VariableEditor {
	void _show( BaseType bt ){
		super._show( bt );
	}
	boolean _zInitialize( Panel_Edit_StructureView structure_view, StringBuffer sbError ){
		super._zInitialize( structure_view, sbError );
		return true;
	}
}
class Panel_Edit_Variable_Sequence extends Panel_Edit_VariableEditor {
	void _show( BaseType bt ){
		super._show( bt );
	}
	boolean _zInitialize( Panel_Edit_StructureView structure_view, StringBuffer sbError ){
		super._zInitialize( structure_view, sbError );
		return true;
	}
}
class Panel_Edit_Variable_Structure extends Panel_Edit_VariableEditor {
	void _show( BaseType bt ){
		super._show( bt );
	}
	boolean _zInitialize( Panel_Edit_StructureView structure_view, StringBuffer sbError ){
		super._zInitialize( structure_view, sbError );
		return true;
	}
}
class Panel_Edit_Variable_Primitive extends Panel_Edit_VariableEditor {
	private JLabel labelType = new JLabel( "Type:" );
	private JComboBox jcbType;
	void _show( BaseType bt ){
		super._show( bt );
		jcbType.setSelectedItem( DAP.getTypeEnumByName( bt.getTypeName() ) );
	}
	boolean _zInitialize( Panel_Edit_StructureView structure_view, StringBuffer sbError ){
		super._zInitialize( structure_view, sbError );
		Class<?> class_DAP_TYPE = DAP.DAP_TYPE.class;
		jcbType = new JComboBox( class_DAP_TYPE.getEnumConstants() ) ;
		layout.add( labelType, jcbType );  
		return true;
	}
}
