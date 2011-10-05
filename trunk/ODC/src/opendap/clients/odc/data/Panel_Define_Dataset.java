package opendap.clients.odc.data;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import opendap.clients.odc.ApplicationController;

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
	private Dimension dimMinimum = new Dimension( 200, 200 );
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
			
			setLayout( new BorderLayout() );
			setBorder( null );

			Border borderEtched = BorderFactory.createEtchedBorder();

			// set up variable panel
			JPanel panelVariable = new JPanel();
			BorderLayout layoutVariable = new BorderLayout();
			panelVariable.setMinimumSize( dimMinimum );
			panelVariable.setBorder( BorderFactory.createTitledBorder( borderEtched, "Edit Variable", TitledBorder.RIGHT, TitledBorder.TOP) );
			panelVariable.setLayout( layoutVariable );
			panelVariable.add( mEditingControls, BorderLayout.NORTH );
			panelVariable.add( mVariableEditingPanel, BorderLayout.CENTER );
			
			// set up value panel
			JPanel panelValue = new JPanel();
			panelValue.setMinimumSize( dimMinimum );
			panelValue.setBorder( BorderFactory.createTitledBorder( borderEtched, "Edit Value", TitledBorder.RIGHT, TitledBorder.TOP) );
			panelValue.setLayout( new BorderLayout() );
			panelValue.add( mValueEditingPanel, BorderLayout.CENTER );

			// configure main panel
			add( panelVariable, BorderLayout.CENTER );
			add( panelValue, BorderLayout.EAST );
			
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


