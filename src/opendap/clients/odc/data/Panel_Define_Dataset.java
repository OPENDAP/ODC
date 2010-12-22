package opendap.clients.odc.data;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import opendap.clients.odc.ApplicationController;
import opendap.dap.BaseType;

// TODO use Panel_EditDataset to add controls

public class Panel_Define_Dataset extends JPanel {
	private Panel_View_Data mParent;
	private Dimension dimMinimum = new Dimension( 100, 80);
	private Dimension dimPreferred = new Dimension( 200, 300 );
	public Dimension getMinimumSize(){
		return dimMinimum;
	}
	boolean _zInitialize( Panel_View_Data parent, StringBuffer sbError ){
		try {
			mParent = parent;

			Border borderEtched = BorderFactory.createEtchedBorder();

			// set up panel
			setMinimumSize( dimMinimum );
			setPreferredSize( dimPreferred );
			setBorder( BorderFactory.createTitledBorder(borderEtched, "Define Dataset", TitledBorder.RIGHT, TitledBorder.TOP) );
			setLayout( null );
			JLabel jlabelVariableName = new JLabel( "Name:" );
			jlabelVariableName.setAlignmentX( JLabel.RIGHT_ALIGNMENT );
			jlabelVariableName.setBounds( 10, 10, 40, 15 );
			JLabel jlabelVariableType = new JLabel( "Type:" );
			jlabelVariableName.setAlignmentX( JLabel.RIGHT_ALIGNMENT );
			jlabelVariableType.setBounds( 10, 25, 40, 15 );

			add( jlabelVariableName );
			add( jlabelVariableType );
			
			return true;

		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}
	public void _showVariable( BaseType bt ){
		System.out.println( "variable " + bt + " picked" );
	}	
	Panel_View_Data _getParent(){ return mParent; }
}

