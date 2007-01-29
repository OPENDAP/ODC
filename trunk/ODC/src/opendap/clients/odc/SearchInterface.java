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

/////////////////////////////////////////////////////////////////////////////
// This file is part of the OPeNDAP Data Connector project.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

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



