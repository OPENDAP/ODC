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
import opendap.clients.odc.FormLayout;
import opendap.dap.BaseType;

// TODO use Panel_EditDataset to add controls
// see Panel_Edit_Container for developer UI guide

public class Panel_Define_Dataset extends JPanel {
	private Panel_View_Data mParent;
	private Panel_Edit_Dataset mEditingControls;
	private Panel_Edit_Variable mVariableEditingPanel;
	private Dimension dimMinimum = new Dimension( 100, 80);
	private Dimension dimPreferred = new Dimension( 200, 300 );
	public Dimension getMinimumSize(){
		return dimMinimum;
	}
	boolean _zInitialize( Panel_View_Data parent, StringBuffer sbError ){
		try {
			mParent = parent;
			mEditingControls = new Panel_Edit_Dataset();
			mVariableEditingPanel = new Panel_Edit_Variable();
			if( ! mVariableEditingPanel._zInitialize( this, sbError ) ){
				sbError.insert( 0, "failed to initialize variable editing panel: " );
				return false;
			};
					

			Border borderEtched = BorderFactory.createEtchedBorder();

			// set up panel
			setMinimumSize( dimMinimum );
			setPreferredSize( dimPreferred );
			setBorder( BorderFactory.createTitledBorder(borderEtched, "Define Dataset", TitledBorder.RIGHT, TitledBorder.TOP) );
			setLayout( new BorderLayout() );
			add( mEditingControls, BorderLayout.NORTH );
			add( mVariableEditingPanel, BorderLayout.CENTER );

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
	public void _showVariable( BaseType bt ){
		mVariableEditingPanel._showVariable( bt );
	}	
}

class Panel_Edit_Variable extends JPanel {
	private Panel_Define_Dataset mParent;
	private FormLayout layout = null;
	private JTextField jtfName = new JTextField();
	private JTextField jtfType = new JTextField();
	boolean _zInitialize( Panel_Define_Dataset parent, StringBuffer sbError ){
		try {
			mParent = parent;
			FormLayout layout = new FormLayout( this );
			setLayout( layout );
			layout.setMargin( 4, 4 );
			return true;
		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}
	public void _showVariable( BaseType bt ){
		System.out.println( "variable " + bt + " picked" );
	}	
}

