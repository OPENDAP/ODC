package opendap.clients.odc;

/**
 * <p>Title: OPeNDAP Data Connector</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: OPeNDAP.org</p>
 * @author John Chamberlain
 * @version 2.59
 */

public interface Model_URL_control {

	// gets the currently user-selected URL
	public DodsURL[] getSelectedURLs( StringBuffer sbError ); // zero-based

	// gets the URL from the list active in the control
	public DodsURL getDisplayURL( int xURL_0); // zero-based

	// adds datasets to the control
	public void vDatasets_Add( DodsURL[] aURL0, boolean z);

	public void vDatasets_Delete( int[] aiIndicesToRemove );

	public void vDatasets_DeleteAll();

}
