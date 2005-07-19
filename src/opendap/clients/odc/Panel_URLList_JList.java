package opendap.clients.odc;

/**
 * <p>Title: OPeNDAP Data Connector</p>
 * <p>Description: JList implementation for an URL List</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: OPeNDAP.org</p>
 * @author John Chamberlain
 * @version 2.59
 */

import javax.swing.*;

public class Panel_URLList_JList extends Panel_URLList {
	private JList mjlist;
    public Panel_URLList_JList( final Model_URL_control control_model ){
		super( control_model );

		// Setup List
		mjlist = new JList();

		mjlist.addKeyListener(
			new java.awt.event.KeyListener(){
				public void keyPressed( java.awt.event.KeyEvent ke ){
					if( ke.getKeyCode() == ke.VK_DELETE ){
						int[] aiSelectedToDelete = mjlist.getSelectedIndices();
						control_model.vDatasets_Delete( aiSelectedToDelete );
					}
				}
				public void keyReleased( java.awt.event.KeyEvent ke ){
					// consumed
				}
				public void keyTyped( java.awt.event.KeyEvent ke ){
					// consumed
				}
			}
		);

		JScrollPane jscrollpaneList = new JScrollPane();
		jscrollpaneList.setViewportView(mjlist);

		this.add( jscrollpaneList );

	}
	public int getSelectedIndex(){
		if( mjlist == null ) return -1;
		return mjlist.getSelectedIndex();
	}
	public int[] getSelectedIndices(){
		if( mjlist == null ) return null;
		return mjlist.getSelectedIndices();
	}
}