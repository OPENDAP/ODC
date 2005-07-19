package opendap.clients.odc;

/**
 * Title:        Panel Retrieve Directory
 * Description:  The directory retrieval panel
 * Copyright:    Copyright (c) 2003-4
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.49
 */

import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import opendap.dap.*;

public class Panel_Retrieve_Directory extends JPanel {

	private Model_Retrieve modelRetrieve;

	private DodsURL murlDirectory = null;
	private DodsURL murlFile = null;
	private JPanel mpanelButtons = null;
	private JPanel mpanelRegex = null;
	private JTree mtreeDirectory = null;
	private JList mlistDirectory = null;
	private JTextArea mjtaMessage = null;
	private JTextField mjtfDirectoryRegex = null;
	private JLabel mjlabelDirectorySample = null;
	private JScrollPane mscrollpane_DirectoryTree;
	private JScrollPane mscrollpane_DirectoryList;
	private JSplitPane msplitDirectory;
	private DirectoryTreeNode mActiveNode;

	private Panel_Retrieve_SelectedDatasets mParent;

	private boolean mzTreeNodeSelectionOccurring = false;

    public Panel_Retrieve_Directory() {}

	Dimension dimMinimum = new Dimension(100, 80);
	public Dimension getMinimumSize(){
		return dimMinimum;
	}

	boolean zInitialize( Panel_Retrieve_SelectedDatasets parent, StringBuffer sbError ){

		try {

			modelRetrieve = ApplicationController.getInstance().getRetrieveModel();
			if( modelRetrieve == null ){
				sbError.append("no retrieve model");
				return false;
			}

			mParent = parent;

			mjtaMessage = new JTextArea();

			// Directory Header setup
			Border borderEtched = BorderFactory.createEtchedBorder();
			mlistDirectory = new JList();
			mlistDirectory.setMinimumSize(new Dimension(50, 50));
			mlistDirectory.setBorder(new EmptyBorder(3, 10, 10, 10));
			mpanelRegex = new JPanel();
			mpanelRegex.setLayout(new BoxLayout(mpanelRegex, BoxLayout.X_AXIS));
			mjtfDirectoryRegex = new JTextField();
			mjtfDirectoryRegex.setMinimumSize(new Dimension(150, 15));
			mpanelRegex.add(new JLabel("Directory Regex: "));
			mpanelRegex.add(mjtfDirectoryRegex);

			// tree
			mtreeDirectory = new JTree();
			TreeCellRenderer rendererLeaflessTreeCell = new LeaflessTreeCellRenderer();
			mtreeDirectory.setCellRenderer(rendererLeaflessTreeCell);
			mtreeDirectory.setMinimumSize(new Dimension(60, 60));

			// scroll panes
			mscrollpane_DirectoryTree = new JScrollPane();
			mscrollpane_DirectoryTree.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			mscrollpane_DirectoryTree.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			mscrollpane_DirectoryList = new JScrollPane(mlistDirectory);
			mscrollpane_DirectoryList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			mscrollpane_DirectoryList.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			// split pane
			msplitDirectory = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, mscrollpane_DirectoryTree, mscrollpane_DirectoryList );

			// set up panel
			setLayout(new BorderLayout());
			setBorder( BorderFactory.createTitledBorder(borderEtched, "Directory", TitledBorder.RIGHT, TitledBorder.TOP) );
			removeAll();
    		add(msplitDirectory, BorderLayout.CENTER);
			// add(mpanelRegex, BorderLayout.SOUTH); leave out the directory regex for now

			mtreeDirectory.addMouseListener(
				new MouseAdapter(){
				    public void mousePressed( MouseEvent me ){
						if( me.getClickCount() == 2 ){
							final JTree tree = (JTree)me.getSource();
							final int row = tree.getRowForLocation(me.getX(),me.getY());
							if( row != -1 ){
								TreePath path = tree.getPathForRow(row);
								final DirectoryTreeNode node = (DirectoryTreeNode)path.getLastPathComponent();
								if( node == null ) return;
								if( node.isTerminal() ) return; // the node is a leaf
								if( murlDirectory == null ){
									ApplicationController.getInstance().vShowError("Internal Error; directory has no defined URL");
									return;
								}
								final String sPageURL = Utility.sConnectPaths( murlDirectory.getBaseURL(), "/", node.getPathString() );
								final Activity activityDirectoryNodeFetch = new Activity();
								Continuation_DoCancel con =
									new Continuation_DoCancel(){
										public void Do(){
											ApplicationController.getInstance().getRetrieveModel().vFetchDirectoryTree_LoadNode(node, sPageURL, 0, false, activityDirectoryNodeFetch);
											try {
												javax.swing.SwingUtilities.invokeAndWait(
													new Runnable() {
														public void run() {
															mtreeDirectory.updateUI(); // refresh structure
														}
													}
												);
											} catch(Exception ex) {
												Utility.vUnexpectedError(ex, "Error updating tree");
											}
											vUpdateFileList(node);
											try {
												javax.swing.SwingUtilities.invokeAndWait(
													new Runnable() {
														public void run() {
															tree.expandRow(row);
														}
													}
												);
											} catch( Exception ex ) {
												Utility.vUnexpectedError(ex, "Error expanding row");
											}
										}
										public void Cancel(){
											// no cleanup
										}
									};
								activityDirectoryNodeFetch.vDoActivity(null, null, con, "Fetching directory " + sPageURL);
							}
						}
					}
				}
			);

			// selection listener for directory tree
			mtreeDirectory.addTreeSelectionListener(
				new TreeSelectionListener(){
				    public void valueChanged(TreeSelectionEvent e){
						TreePath tp = mtreeDirectory.getSelectionPath();
						if( tp == null ) return;
						Object oSelectedNode = tp.getLastPathComponent();
						DirectoryTreeNode node = (DirectoryTreeNode)oSelectedNode;
						vUpdateNodeSelections();
						vUpdateFileList( node );
					}
				}
			);

			// selection listener for directory listing
// no longer used because it conflicts with the ctrl-click functionality
//			mlistDirectory.addListSelectionListener(
//				new ListSelectionListener(){
//				    public void valueChanged( ListSelectionEvent lse ){
//						int xItem = lse.getFirstIndex();
//						vShowListItem( xItem );
//					}
//				}
//			);

			mlistDirectory.addMouseListener(
				new MouseAdapter(){
				    public void mousePressed( MouseEvent me ){
						final MouseEvent me_final = me;
						JList list = (JList)me.getSource();
						int xItem = list.locationToIndex(me.getPoint());
						if( me_final.getClickCount() == 2 ){
							if( xItem > 0 ){
								ListModel model = list.getModel();
								Object oItem = model.getElementAt(xItem);
								if( murlDirectory == null ){
									ApplicationController.getInstance().vShowError("Internal error, directory URL missing");
									return;
								}

								// the idea here is to use the stored URL (the URL actually in the
								// the directory page) preferentially to support the THREDDS directories
								// which do not have a true directory structure; if an implementation
								// has a relative URL unexpectedly and the stored URL does not start
								// with a valid http protocol indicator then the backup method is to
								// use the old way of constructing the URL from scratch based on the
								// the directory path
								String sStoredURL = mActiveNode.getHREFList()[xItem];
								if( sStoredURL == null ) sStoredURL = "";
								final String sBaseURL = sStoredURL.toUpperCase().startsWith("HTTP://") ? sStoredURL : Utility.sConnectPaths( murlDirectory.getBaseURL(), "/", mActiveNode.getPathString() ) + oItem;
								if( me_final.isControlDown() ){
									if( me_final.isShiftDown() ){ // get DAS
										modelRetrieve.vShowDAS(sBaseURL, null);
									} else { // get DDS
										modelRetrieve.vShowDDS(sBaseURL, null);
									}
								} else {
									int eTYPE;
									if( Utility.isImage(sBaseURL) ){
										eTYPE = DodsURL.TYPE_Image;
									} else {
										eTYPE = DodsURL.TYPE_Data; // todo see if we can make more discrimination
									}
									murlFile = new DodsURL(sBaseURL, eTYPE);
									murlFile.setTitle(mActiveNode.getFileName(xItem));
									mActiveNode.setFileURL(xItem, murlFile);
									DodsURL[] aURL = new DodsURL[1];
									aURL[0]	= murlFile;
									modelRetrieve.getURLList().vDatasets_Add(aURL, true);
									modelRetrieve.vShowURL(murlFile, null);
								}
							}
						} else { // only one click
							vShowListItem( xItem );
					    }
					}
				}
			);

			return true;

		} catch( Exception ex ) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	final void vShowListItem( int xIndex ){
//		int xIndex = Panel_Retrieve_Directory.this.mlistDirectory.getSelectedIndex();
		if (xIndex == 0) {
			// header selected, ignore
		} else if (murlDirectory == null) {
			// no dir tree active, ignore
		} else if (mzTreeNodeSelectionOccurring) {
			// ignore change to selection because it is being done programmatically
			// as a side effect of a tree node selection, not because user is selecting
			// in list
		}
		else { // the user is selecting something in the list, update the url info
			if (mtreeDirectory == null) {
				ApplicationController.vShowError(
					"directory tree unexpectedly does not exist");
				return;
			}
			Model_DirectoryTree model = (Model_DirectoryTree) mtreeDirectory.
				getModel();
			if (model == null) {
				ApplicationController.vShowWarning(
					"directory tree returned no model");
				return;
			}
			DirectoryTreeNode nodeSelected = model.getSelectedNode();
			if (nodeSelected == null)
				return; // do nothing
			nodeSelected.setSelectedFileIndices(mlistDirectory.
												getSelectedIndices());
			int xSelection0 = mlistDirectory.getSelectedIndex();
			if (xSelection0 < 0) {
				// no selection
			}
			else if (xSelection0 == 0) {
				// file count entry (list header)
			}
			else { // an item is selected (the index is effectively one-based because of the header)

				// if item has DDS use it
				DodsURL url = null;
				if (mActiveNode.getFileURL(xIndex) != null) {
					url = mActiveNode.getFileURL(xIndex);
				}
				else if (mActiveNode.getFirstFileURL(xIndex) != null) {
					url = mActiveNode.getFileURL(xIndex);
				}
				else {
					// no DDS retrieved yet for this directory
				}
				if (url != null)
					Panel_Retrieve_Directory.this.modelRetrieve.vShowURL(url, null);

					// update location bar

					// update status bar with selection description if it exists
				String sDescription = nodeSelected.getFileDescription(
					xSelection0);
				if (sDescription != null)
					ApplicationController.vShowStatus_NoCache(sDescription);
			}
		}
	}

	void vShowMessage( String sMessage ){
		if( sMessage == null ){
			mjtaMessage.setText( "" );
		} else {
			mjtaMessage.setText( sMessage );
		}
		mscrollpane_DirectoryTree.setViewportView( mjtaMessage );
	}

	DodsURL getURL_directory(){
		return murlDirectory;
	}

	DodsURL getURL_file(){
		return murlFile;
	}

	boolean setURL( final DodsURL url, StringBuffer sbError ){
		try {
			if( url == null ){
				sbError.append("no url supplied");
				return false;
			}
			mpxDividerLocationCurrent = 1000; // reset the divider location with every new url
			murlDirectory = url;
			murlFile = null; // todo panel should remember old selections and file URLs if possible
			final Model_DirectoryTree tm = murlDirectory.getDirectoryTree();
			mtreeDirectory.setModel(tm);
			if( url.getDirectoryRegex() == null ){
				mjtfDirectoryRegex.setText("");
			} else {
				mjtfDirectoryRegex.setText(url.getDirectoryRegex());
			}
			Object[] aoEmpty = {"[empty]"};
			mlistDirectory.setListData(aoEmpty);
			mscrollpane_DirectoryTree.setViewportView( mtreeDirectory );
			if( mtreeDirectory.getRowCount() > 0 ) mtreeDirectory.setSelectionRow(0); // select the root
			ApplicationController.getInstance().getRetrieveModel().setLocationString(murlDirectory.getBaseURL());
			mParent.model.retrieve_panel.getPanelDDX().vClear();
			return true;
		} catch( Throwable t ) {
			Utility.vUnexpectedError(t, sbError);
			return false;
		}
	}

	private int mpxDividerLocationCurrent = 1000; // this keeps some constancy to the divider location
	private void vUpdateDividerLocation(){
		if( mActiveNode == null ){
			msplitDirectory.setDividerLocation(0.5d);
		} else {
			try {
				String[] asFiles = mActiveNode.getFileList(); // one-based
				FontMetrics fm = mlistDirectory.getFontMetrics(mlistDirectory.getFont());
				String sLeadText = mlistDirectory.getModel().getElementAt(0).toString();
				int pxLeadWidth = fm.stringWidth(sLeadText);
				int pxDesiredWidth = pxLeadWidth;
				for( int xFile = 1; xFile <= asFiles.length - 1; xFile++ ){
					if( asFiles[xFile] == null ) continue;
					int pxWidth = fm.stringWidth( asFiles[xFile] );
					if( pxWidth > pxDesiredWidth ) pxDesiredWidth = pxWidth;
				}
				int pxPaneWidth = mParent.getWidth();
				if( pxDesiredWidth > pxPaneWidth * 0.80 ){
					msplitDirectory.setDividerLocation(0.2d);
				} else {
					int pxLocation = pxPaneWidth - pxDesiredWidth - 50;
					if( pxLocation < mpxDividerLocationCurrent ){
						mpxDividerLocationCurrent = pxLocation;
						msplitDirectory.setDividerLocation(mpxDividerLocationCurrent);
					}
				}
			} catch(Exception ex) {
				msplitDirectory.setDividerLocation(0.5d);
			}
		}
	}

	private void vUpdateNodeSelections(){
		TreeSelectionModel tsm = mtreeDirectory.getSelectionModel();
		Model_DirectoryTree model = (Model_DirectoryTree)mtreeDirectory.getModel();
		TreePath[] atp = tsm.getSelectionPaths();
		model.vClearNodeSelection();
		if( atp == null ) return;
		for( int xPath = 0; xPath < atp.length; xPath++ ){
			DirectoryTreeNode node = (DirectoryTreeNode)atp[xPath].getLastPathComponent();
			node.setSelected(true);
		}
	}

	private void vUpdateFileList( DirectoryTreeNode node ){
		Model_DirectoryTree model = (Model_DirectoryTree)mtreeDirectory.getModel();
		if( node == null ){
			model.setSelectedNode(null);
			ApplicationController.getInstance().vShowWarning("internal error, nothing to update file list with");
			return;
		}
		mActiveNode = node;
		model.setSelectedNode(mActiveNode);

		// show error if any
		if( node.zHasError() ){
			modelRetrieve.retrieve_panel.vShowMessage("Directory Error: \n" + node.getError());
		}

		String[] asFiles = mActiveNode.getFileList(); // one-based
		if( asFiles == null ){
			ApplicationController.getInstance().vShowWarning("internal error, file list unexpectedly null");
			return;
		}
		if( mActiveNode.isDiscovered() ){
			if( asFiles.length == 1 ){
				asFiles[0] = "[empty]";
			} else {
				asFiles[0] = (asFiles.length-1) + " files: (dbl-click to view)";
			}
			mzTreeNodeSelectionOccurring = true;
		} else {
			asFiles[0] = "[<-dbl-click to fetch]";
		}
		mlistDirectory.setListData(asFiles);
		int[] aiSelectedIndices = mActiveNode.getFileList_SelectedIndices();
		if( aiSelectedIndices != null ) mlistDirectory.setSelectedIndices(aiSelectedIndices);
		mzTreeNodeSelectionOccurring = false;
		mlistDirectory.invalidate();
		validate();
		vUpdateDividerLocation();
		final String sLocationURL = Utility.sConnectPaths( murlDirectory.getBaseURL(), "/", node.getPathString() );
		ApplicationController.getInstance().getRetrieveModel().setLocationString( sLocationURL );
	}

	public Model_DirectoryTree getDirectoryTreeModel(){
		if( mtreeDirectory == null ) return null;
		return (Model_DirectoryTree)this.mtreeDirectory.getModel();
	}

}

// returns folder icon instead of page icon in the case of a leaf
class LeaflessTreeCellRenderer extends DefaultTreeCellRenderer {
    public Component getTreeCellRendererComponent(JTree tree, Object value,
						  boolean sel,
						  boolean expanded,
						  boolean leaf, int row,
						  boolean hasFocus) {
		Component componentDefault = super.getTreeCellRendererComponent( tree, value, sel, expanded, leaf, row, hasFocus );
		if (tree.isEnabled()) {
			if (leaf) {
				setIcon(getClosedIcon());
			}
		} else {
			if (leaf) {
				setDisabledIcon(getClosedIcon());
			}
		}
		return this;
    }
}

