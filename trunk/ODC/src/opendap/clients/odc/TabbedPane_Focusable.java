package opendap.clients.odc;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

public class TabbedPane_Focusable extends JTabbedPane implements IControlPanel {
	public void vSetFocus(){
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				IControlPanel panel = (IControlPanel)getSelectedComponent();
				panel.vSetFocus();
			}
		});
	}
}
