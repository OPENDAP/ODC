package opendap.clients.odc.data;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

// contains the editing panels:
// Data -        left: Panel_Edit_StructureView      right: Panel_Define_Dataset
// Expression -  left: Panel_Edit_Expression         right: Panel_Define_Expression
// Stream -      left: Panel_Edit_Stream             right: Panel_Define_Stream

public class Panel_EditContainer extends JPanel {
	Panel_View_Data mParent;
	Panel_Edit_blank mEditBlank;
	Panel_Edit_StructureView mEditStructure;
	Panel_Edit_Expression mEditExpression;
	Panel_Edit_Stream mEditStream;
	Panel_Define_Dataset mDefineData;
	Panel_Define_Expression mDefineExpression;
	Panel_Define_Stream mDefineStream;
	private Panel_EditContainer(){}
	static Panel_EditContainer _zCreate( Panel_View_Data parent, StringBuffer sbError ){
		if( parent == null ){
			sbError.append( "no parent supplied" );
			return null;
		}
		Panel_EditContainer panelEditContainer = new Panel_EditContainer();
		panelEditContainer.mParent = parent; 
		panelEditContainer.mDefineExpression = Panel_Define_Expression._zCreate( parent, sbError );
		panelEditContainer.mEditBlank = new Panel_Edit_blank();
		panelEditContainer.mEditStructure = new Panel_Edit_StructureView();
		panelEditContainer.mEditExpression = new Panel_Edit_Expression();
		panelEditContainer.mEditStream = new Panel_Edit_Stream();
		panelEditContainer.mDefineData = new Panel_Define_Dataset();
		panelEditContainer.mDefineStream = new Panel_Define_Stream();
		if( ! panelEditContainer.mDefineData._zInitialize( parent, sbError ) ){
			sbError.insert( 0, "initializing data define panel: " );
			return null;
		}
		if( ! panelEditContainer.mEditStructure._zInitialize( panelEditContainer.mDefineData, sbError ) ){
			sbError.insert( 0, "initializing structure panel: " );
			return null;
		}
		if( ! panelEditContainer.mEditExpression._zInitialize( panelEditContainer.mDefineExpression, null, null, null, sbError ) ){
			sbError.insert( 0, "initializing expression editor: " );
			return null;
		}
		
		panelEditContainer.setLayout( new BorderLayout() );
		Border borderEtched = BorderFactory.createEtchedBorder();
		panelEditContainer.setBorder( BorderFactory.createTitledBorder( borderEtched, "Structure Editing", TitledBorder.RIGHT, TitledBorder.TOP ) );
		panelEditContainer._vClear();
		return panelEditContainer;
	}
	void _vClear(){
		removeAll();
		add( mEditBlank, BorderLayout.CENTER );
	}
	boolean _zSetModel( Model_Dataset model, StringBuffer sbError ){
		if( model == null ){
			sbError.insert( 0, "supplied model was null" );
			return false;
		}
		switch( model.getType() ){
			case Model_Dataset.TYPE_Data:
				Model_DataTree tree = model.getDataTree( sbError );
				if( tree == null ){
					sbError.insert( 0, "failed to get data tree for model" );
					return false;
				}
				removeAll();
				add( mEditStructure, BorderLayout.CENTER );
				add( mDefineData, BorderLayout.EAST );
				Model_DataTree data_tree = model.getDataTree( sbError );
				if( data_tree == null ){
					sbError.insert( 0,"failed to get data tree for model " + model.getDDS_Name() );
					return false;
				}
				mEditStructure._setModel( data_tree );
				break;
			case Model_Dataset.TYPE_Expression:
				if( mEditExpression._setModel( model, sbError ) ){
					removeAll();
					add( mEditExpression, BorderLayout.CENTER );
					add( mDefineExpression, BorderLayout.EAST );
				} else {
					sbError.insert( 0, "failed to initialize expression editing panel: " );
					return false;
				}
				break;
			case Model_Dataset.TYPE_Stream:
				if( mEditStream._zInitialize(  mDefineStream, sbError ) ){
					removeAll();
					add( mEditStream, BorderLayout.CENTER );
					add( mDefineStream, BorderLayout.EAST );
				} else {
					sbError.insert( 0, "failed to initialize stream editing panel: " );
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

