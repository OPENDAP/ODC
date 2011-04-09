package opendap.clients.odc.data;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import opendap.clients.odc.FormLayout;

public class Panel_Edit_Cell extends JPanel {
	JTextField jtfValue;
	private Panel_Edit_Cell(){}
	final static Panel_Edit_Cell _create( StringBuffer sbError ){
		Panel_Edit_Cell panel = new Panel_Edit_Cell();
		FormLayout layout = new FormLayout( panel );
		panel.setLayout( layout );
		panel.jtfValue = new JTextField();
		layout.add( new JLabel( "Value:" ), panel.jtfValue );
		return panel;
	}
}
