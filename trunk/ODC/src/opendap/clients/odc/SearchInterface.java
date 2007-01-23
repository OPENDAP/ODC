/*
 * Any Interface added by the SearchWindow class must extend this class
 *
 * In addition to extending this class, it must
 *   - Have it's first constructor take either a String, or no value at all
 *
 * For further information, see the comments at the top of SearchWindow.java .
 *
 * @author John Chamberlain
 */
package opendap.clients.odc;

import javax.swing.JPanel;
import javax.swing.event.ListSelectionListener;

public abstract class SearchInterface extends JPanel {
	public abstract DodsURL[] getURLs( StringBuffer sbError );
	public abstract void addListSelectionListener( ListSelectionListener listener );
	public abstract boolean zInitialize( StringBuffer sbError );
	public void vAddSelected(){
		StringBuffer sbError = new StringBuffer(80);
		DodsURL[] urls = getURLs(sbError);
		if( urls == null ){
			ApplicationController.getInstance().vShowError("failed to get selected urls for retrieval: " + sbError);
		} else {
			Model_Retrieve retrieve_model = ApplicationController.getInstance().getRetrieveModel();
			Model_URLList urllist = retrieve_model.getURLList();
			urllist.vDatasets_Add(urls);
			ApplicationController.getInstance().getAppFrame().vActivateRetrievalPanel();
		}
	}

}



