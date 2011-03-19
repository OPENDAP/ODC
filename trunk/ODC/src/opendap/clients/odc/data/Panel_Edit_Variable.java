package opendap.clients.odc.data;

import java.awt.BorderLayout;
import java.util.Enumeration;

import javax.swing.DefaultCellEditor;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JComboBox;

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

public class Panel_Edit_Variable extends JPanel {
	private Panel_Define_Dataset mParent;
	private BaseType bt_active = null;
	private Panel_Edit_VariableEditor mActiveEditor;
	private Panel_Edit_Variable_Array editArray;
	private Panel_Edit_Variable_Grid editGrid;
	private Panel_Edit_Variable_Sequence editSequence;
	private Panel_Edit_Variable_Structure editStructure;
	private Panel_Edit_Variable_Primitive editPrimitive;
	boolean _zInitialize( Panel_Define_Dataset parent, StringBuffer sbError ){
		try {
			mParent = parent;
			setLayout( new java.awt.BorderLayout() );
			editStructure = new Panel_Edit_Variable_Structure();
			if( ! editStructure._zInitialize( sbError ) ){
				sbError.insert( 0, "error initializing structure editor: " );
				return false;
			}
			return true;
		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}
	public void _showVariable( BaseType bt ){
		mActiveEditor._clear();
		remove( mActiveEditor );
		bt_active = bt;
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
			default:
				mActiveEditor = null; // should not happen
				break;
		}
		add( mActiveEditor, BorderLayout.CENTER );
	}
}

abstract class Panel_Edit_VariableEditor extends JPanel {
	private BaseType bt_active = null;
	protected JLabel labelName = new JLabel( "Name:" );
	protected JTextField jtfName = new JTextField();
	protected JLabel labelName_Long = new JLabel( "Long Name:" );
	protected JTextField jtfName_Long = new JTextField();
	protected JLabel labelName_Clear = new JLabel( "Clear Name:" );
	protected JTextField jtfName_Clear = new JTextField();
	boolean _zInitialize( StringBuffer sbError ){
		try {
			FormLayout layout = new FormLayout( this );
			setLayout( layout );
			layout.setMargin( 4, 4 );
			add( labelName );
			add( jtfName );
			add( labelName_Long );
			add( jtfName_Long );
			add( labelName_Clear );
			add( jtfName_Clear );
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
		jtfName_Long.setText( bt.getLongName() );
		jtfName_Clear.setText( bt.getClearName() );
		bt_active = bt;
	}
}

class Panel_Edit_Variable_Array extends Panel_Edit_VariableEditor {
	void _show( BaseType bt ){
		super._show( bt );
	}
	boolean _zInitialize( StringBuffer sbError ){
		super._zInitialize( sbError );
		return true;
	}
}
class Panel_Edit_Variable_Grid extends Panel_Edit_VariableEditor {
	void _show( BaseType bt ){
		super._show( bt );
	}
	boolean _zInitialize( StringBuffer sbError ){
		super._zInitialize( sbError );
		return true;
	}
}
class Panel_Edit_Variable_Sequence extends Panel_Edit_VariableEditor {
	void _show( BaseType bt ){
		super._show( bt );
	}
	boolean _zInitialize( StringBuffer sbError ){
		super._zInitialize( sbError );
		return true;
	}
}
class Panel_Edit_Variable_Structure extends Panel_Edit_VariableEditor {
	void _show( BaseType bt ){
		super._show( bt );
	}
	boolean _zInitialize( StringBuffer sbError ){
		super._zInitialize( sbError );
		return true;
	}
}
class Panel_Edit_Variable_Primitive extends Panel_Edit_VariableEditor {
	protected JLabel labelType = new JLabel( "Type:" );
	protected JComboBox jcbType;
	void _show( BaseType bt ){
		super._show( bt );
		jcbType.setSelectedItem( DAP.getTypeEnumByName( bt.getTypeName() ) );
	}
	boolean _zInitialize( StringBuffer sbError ){
		super._zInitialize( sbError );
		add( labelType );
		Class<?> class_DAP_TYPE = DAP.DAP_TYPE.class;
		jcbType = new JComboBox( class_DAP_TYPE.getEnumConstants() ) ;
		add( jcbType );
		return true;
	}
}
