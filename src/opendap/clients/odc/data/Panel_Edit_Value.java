package opendap.clients.odc.data;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Enumeration;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.DefaultCellEditor;
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
import opendap.clients.odc.FormLayout;
import opendap.clients.odc.Utility_String;
import opendap.clients.odc.gui.Resources;
import opendap.dap.BaseType;
import opendap.dap.DArray;
import opendap.dap.DArrayDimension;
import opendap.dap.DGrid;
import opendap.dap.DSequence;
import opendap.dap.DStructure;
import opendap.dap.test.dap_test;

// see Panel_View_Data for layout
// parent: Panel_Define_Dataset

public class Panel_Edit_Value extends JPanel {
	private Panel_Define_Dataset mParent;
	private Value valueActive = null;
	private Panel_Edit_ValueEditor mActiveEditor;
	private Panel_Edit_Value_blank edit_blank;
	private Panel_Edit_Value_Byte editByte;
	private Panel_Edit_Value_Short editShort;
	private Panel_Edit_Value_Integer editInteger;
	private Panel_Edit_Value_Float editFloat;
	private Panel_Edit_Value_Double editDouble;
	private Panel_Edit_Value_String editString;
	private Panel_Edit_Value(){};
	static Panel_Edit_Value _create( Panel_Define_Dataset parent, Panel_Edit_ViewStructure view, StringBuffer sbError ){
		Panel_Edit_Value panel = new Panel_Edit_Value();
		try {
			panel.mParent = parent;
			panel.setLayout( new java.awt.BorderLayout() );
			panel.edit_blank = new Panel_Edit_Value_blank();
			panel.editByte = new Panel_Edit_Value_Byte();
			if( ! panel.editByte._zInitialize( view, sbError ) ){
				sbError.insert( 0, "error initializing byte editor: " );
				return null;
			}
			panel.editShort = new Panel_Edit_Value_Short();
			if( ! panel.editShort._zInitialize( view, sbError ) ){
				sbError.insert( 0, "error initializing short editor: " );
				return null;
			}
			panel.editInteger = new Panel_Edit_Value_Integer();
			if( ! panel.editInteger._zInitialize( view, sbError ) ){
				sbError.insert( 0, "error initializing integer editor: " );
				return null;
			}
			panel.editFloat = new Panel_Edit_Value_Float();
			if( ! panel.editFloat._zInitialize( view, sbError ) ){
				sbError.insert( 0, "error initializing float editor: " );
				return null;
			}
			panel.editDouble = new Panel_Edit_Value_Double();
			if( ! panel.editDouble._zInitialize( view, sbError ) ){
				sbError.insert( 0, "error initializing double editor: " );
				return null;
			}
			panel.editString = new Panel_Edit_Value_String();
			if( ! panel.editString._zInitialize( view, sbError ) ){
				sbError.insert( 0, "error initializing string editor: " );
				return null;
			}
			panel.add( panel.edit_blank, BorderLayout.CENTER );
			return panel;
		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError( ex, sbError );
			return null;
		}
	}
	public void _showValue( Value value ){
		if( mActiveEditor != null || value == null ){
			mActiveEditor._clear();
			remove( mActiveEditor );
			add( edit_blank );
		}
		valueActive = value;
		switch( value.getType() ){
			case Byte: mActiveEditor = editByte; break;
			case Int16: mActiveEditor = editShort; break;
			case UInt16: mActiveEditor = editShort; break;
			case Int32: mActiveEditor = editInteger; break;
			case UInt32: mActiveEditor = editInteger; break;
			case Float32: mActiveEditor = editFloat; break;
			case Float64: mActiveEditor = editDouble; break;
			case String: mActiveEditor = editString; break;
			default:
				mActiveEditor = null; // should not happen
				ApplicationController.vShowError_NoModal( "internal error, unknown/unsupported value type to edit: " + value.getType() );
				return;
		}
		add( mActiveEditor, BorderLayout.CENTER );
		mActiveEditor._show( value );
	}
}

abstract class Panel_Edit_ValueEditor extends JPanel {
	private Value valueActive = null;
	protected Panel_Edit_ViewStructure view;
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
			
			layout = new FormLayout( this );
			setLayout( layout );
			layout.setMargin( 4, 4 );
			layout.add( labelName, jtfName );
			layout.add( labelName_Long, displayName_Long );
			layout.add( labelName_Clear, labelName_Encoded );
			
			jtfName.addFocusListener(
				new java.awt.event.FocusAdapter(){
					public void focusLost(java.awt.event.FocusEvent evt) {
						String sNewName = jtfName.getText();
//						if( ! sNewName.equals( jtfName.getName() ) ) _setName( sNewName, view );
					}
				}
			);
			jtfName.addActionListener(       // occurs if user clicks enter key
				new java.awt.event.ActionListener(){
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						String sNewName = jtfName.getText();
//						if( ! sNewName.equals( jtfName.getName() ) ) _setName( sNewName, view );
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
		valueActive = null;
	}
	void _show( Value value ){ // activate the node for editing
		if( value == null ){ // blank screen
			jtfName.setText( "" );
			displayName_Long.setText( "" );
			labelName_Encoded.setText( "" );
		} else {
			DAP.DAP_TYPE type = value.getType();
//			jtfName.setText( bt.getClearName() );
//			displayName_Long.setText( bt.getLongName() );
//			labelName_Encoded.setText( bt.getName() );
		}
		valueActive = value;
	}
}

class Panel_Edit_Value_blank extends JPanel {
	Panel_Edit_Value_blank(){
		setLayout( new java.awt.BorderLayout() );
		JPanel panelLabel = new JPanel();
		panelLabel.setLayout( new javax.swing.BoxLayout( panelLabel, BoxLayout.LINE_AXIS ) );
		JLabel label = new JLabel( "No Value Selected" );
		panelLabel.add( javax.swing.Box.createHorizontalStrut( 10 ) );
		panelLabel.add( label );
		panelLabel.add( javax.swing.Box.createHorizontalStrut( 10 ) );
		add( panelLabel, BorderLayout.CENTER );
	}
}

class Panel_Edit_Value_Byte extends Panel_Edit_ValueEditor {
	void _show( Value value ){
		super._show( value );
	}
	boolean _zInitialize( Panel_Edit_ViewStructure structure_view, StringBuffer sbError ){
		super._zInitialize( structure_view, sbError );
		return true;
	}
}

class Panel_Edit_Value_Short extends Panel_Edit_ValueEditor {
	private Node_Array valueActive = null;
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
	void _show( Value value ){
		super._show( value );
		labelValueCount_value.setText( Integer.toString( valueActive._getValueCount() ) );
		jcbType.setSelectedItem( DAP.getTypeEnumByName( valueActive._getValueTypeString() ) );
		int xDimension1 = 1;
		int ctDimension = valueActive._getDimensionCount();
		for( ; xDimension1 <= ctDimension; xDimension1++ ){
			listDimensionNameJTF.get( xDimension1 - 1 ).setText( valueActive._getDimensionName( xDimension1 ) );
			listDimensionSizeJTF.get( xDimension1 - 1 ).setText( Integer.toString( valueActive._getDimensionLength( xDimension1 ) ) );
			listDimensionDelete.get( xDimension1 - 1 ).setVisible( ctDimension > 1 );
			listDimensionUp.get( xDimension1 - 1 ).setVisible( xDimension1 > 1 );
			listDimensionDown.get( xDimension1 - 1 ).setVisible( xDimension1 < ctDimension );
			listDimensionPanel.get( xDimension1 - 1 ).setVisible( true );
		}
		for( ; xDimension1 <= MAX_DIMENSIONS; xDimension1++ ){
			listDimensionPanel.get( xDimension1 - 1).setVisible( false );
		}
//		view._getVariableView()._show( value );
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
					}
				}
			);
			buttonDelete.addActionListener( 
				new java.awt.event.ActionListener(){
					public void actionPerformed( java.awt.event.ActionEvent evt ){
						JOptionPane.showMessageDialog( Panel_Edit_Value_Short.this, "not implemented" );
					}
				}
			);
		}
		buttonAddDimension.addActionListener( 
			new java.awt.event.ActionListener(){
				public void actionPerformed( java.awt.event.ActionEvent evt ){
				}
			}
		);
		return true;
	}
}
class Panel_Edit_Value_Integer extends Panel_Edit_ValueEditor {
	void _show( Value value ){
		super._show( value );
	}
	boolean _zInitialize( Panel_Edit_ViewStructure structure_view, StringBuffer sbError ){
		super._zInitialize( structure_view, sbError );
		return true;
	}
}
class Panel_Edit_Value_Float extends Panel_Edit_ValueEditor {
	void _show( Value value ){
		super._show( value );
	}
	boolean _zInitialize( Panel_Edit_ViewStructure structure_view, StringBuffer sbError ){
		super._zInitialize( structure_view, sbError );
		return true;
	}
}
class Panel_Edit_Value_Double extends Panel_Edit_ValueEditor {
	void _show( Value value ){
		super._show( value );
	}
	boolean _zInitialize( Panel_Edit_ViewStructure structure_view, StringBuffer sbError ){
		super._zInitialize( structure_view, sbError );
		return true;
	}
}
class Panel_Edit_Value_String extends Panel_Edit_ValueEditor {
	void _show( Value value ){
		super._show( value );
	}
	boolean _zInitialize( Panel_Edit_ViewStructure structure_view, StringBuffer sbError ){
		super._zInitialize( structure_view, sbError );
		return true;
	}
}
