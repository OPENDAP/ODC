package opendap.clients.odc;

/**
 * <p>Title: OPeNDAP Data Connector</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: OPeNDAP.org</p>
 * @author John Chamberlain
 * @version 2.42
 */

import javax.swing.JPanel;

abstract public class Panel_URLList extends JPanel {
	protected Model_URL_control mControlModel;
    public Panel_URLList( Model_URL_control control ){ mControlModel = control; }
	abstract public int getSelectedIndex();
	abstract public int[] getSelectedIndices();
	abstract public void vSelectIndex( int iIndex );
}
