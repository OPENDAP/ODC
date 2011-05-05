package opendap.clients.odc.data;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DAP;
import opendap.clients.odc.FormLayout;
import opendap.dap.BaseType;

// Panel_View_Data
// see Panel_Edit_Container for parent
// TOP MIDDLE:    Panel_Edit_Dataset   (editing controls, like move up/down)
// MIDDLE: Panel_Edit_Variable  (editing of an individual variable)
// RIGHT:  Panel_Edit_Value  (editing of an individual value)

public class Panel_Define_Dataset extends JPanel {
	private Panel_View_Data mParent;
	private Panel_Edit_Dataset mEditingControls;
	private Panel_Edit_Variable mVariableEditingPanel;
	private Panel_Edit_Value mValueEditingPanel;
	private Dimension dimMinimum = new Dimension( 100, 80);
//	private Dimension dimPreferred = new Dimension( 200, 300 );
	public Dimension getMinimumSize(){
		return dimMinimum;
	}
	boolean _zInitialize( Panel_View_Data parent, Panel_Edit_Container container, StringBuffer sbError ){
		try {
			mParent = parent;
			Panel_Edit_ViewStructure structure_view = container.mEditStructure;
			mEditingControls = new Panel_Edit_Dataset( mParent, container );
			mVariableEditingPanel = Panel_Edit_Variable._create( this, structure_view, sbError );
			if( mVariableEditingPanel == null  ){
				sbError.insert( 0, "failed to create variable editing panel: " );
				return false;
			};					
			mValueEditingPanel = Panel_Edit_Value._create( this, structure_view, sbError );
			if( mValueEditingPanel == null  ){
				sbError.insert( 0, "failed to create value editing panel: " );
				return false;
			};					

			Border borderEtched = BorderFactory.createEtchedBorder();

			// set up variable panel
			JPanel panelVariable = new JPanel();
			panelVariable.getInsets().top = 0;
			panelVariable.getInsets().bottom = 0;
			panelVariable.getInsets().left = 0;
			panelVariable.getInsets().right = 0;
			panelVariable.setMinimumSize( dimMinimum );
//			setPreferredSize( dimPreferred );
			panelVariable.setBorder( BorderFactory.createTitledBorder(borderEtched, "Edit Variable", TitledBorder.RIGHT, TitledBorder.TOP) );
			panelVariable.setLayout( new BorderLayout() );
			panelVariable.add( mEditingControls, BorderLayout.NORTH );
			panelVariable.add( mVariableEditingPanel, BorderLayout.CENTER );
			
			// set up value panel
			JPanel panelValue = new JPanel();
			panelValue.setMinimumSize( dimMinimum );
			panelValue.getInsets().top = 0;
			panelValue.getInsets().bottom = 0;
			panelValue.getInsets().left = 0;
			panelValue.getInsets().right = 0;
			panelValue.setBorder( BorderFactory.createTitledBorder(borderEtched, "Edit Value", TitledBorder.RIGHT, TitledBorder.TOP) );
			panelValue.add( mValueEditingPanel, BorderLayout.CENTER );

			// configure main panel
			add( panelVariable, BorderLayout.CENTER );
			add( panelValue, BorderLayout.EAST );
			
			getInsets().top = 0;
			getInsets().bottom = 0;
			getInsets().left = 0;
			getInsets().right = 0;
			
//			JLabel jlabelVariableName = new JLabel( "Name:" );
//			jlabelVariableName.setAlignmentX( JLabel.RIGHT_ALIGNMENT );
//			jlabelVariableName.setBounds( 10, 10, 40, 15 );
//			JLabel jlabelVariableType = new JLabel( "Type:" );
//			jlabelVariableName.setAlignmentX( JLabel.RIGHT_ALIGNMENT );
//			jlabelVariableType.setBounds( 10, 25, 40, 15 );
//			add( jlabelVariableName );
//			add( jlabelVariableType );

			
			return true;

		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}
	Panel_View_Data _getParent(){ return mParent; }
	public void _showVariable( Node node ){
		mVariableEditingPanel._showVariable( node );
	}	
}


