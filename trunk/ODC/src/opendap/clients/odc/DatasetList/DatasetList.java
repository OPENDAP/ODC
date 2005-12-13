package opendap.clients.odc.DatasetList;

import opendap.clients.odc.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

import javax.swing.border.EmptyBorder;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;

import java.net.*;

import java.nio.ByteBuffer;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.io.RandomAccessFile;

import opendap.clients.odc.*;

/**
 * This class provides the base structure for the DatasetList application.
 *
 * @version     2.00 Jul 2002
 * @author      Kashan A. Shaikh
 * @modified by Sheila Jiang 12 Mar 2002
 * @modified by John Chamberlain July 2002, December 2003, June 2004, July 2004
 *
 * 1.26 July 2001
 * 2.14 August 2002
 * 2.30 December 2003
 * 2.49 June 2004
 * 2.53 July 2004
 *
 */
public class DatasetList extends SearchInterface {
    private DOMTree xmlDOMTree;

    // Dimensions
    static final int windowHeight = 400;
    static final int leftWidth = 400;
    static final int rightWidth = 400;
    static final int windowWidth = leftWidth + rightWidth;

    private JPanel treePanel;
    private JSplitPane windowSplitPane;
    private JPanel treeSelectionInfoPanel;
	private JScrollPane scrollTree;

    public static String EVENT_RETRIEVE="Retrieve";
    public static String EVENT_REFRESH="Refresh XML";
    public static String EVENT_REFRESH_CANCEL="Cancel Refresh";
    public static String EVENT_SEARCH="Search";
    public static String EVENT_CLEAR="Clear";
    public static String EVENT_SHOW_ALL="Show All";

	private JButton mjbuttonRefresh;
	private final JButton searchButton = new JButton("Search: ");
	private final JButton jbuttonShowAll = new JButton("Show All");
	javax.swing.JTextField jtfSearch;

    public DatasetList() {

        xmlDOMTree = new DOMTree(this);

        // Selection window
        JPanel outerInfoPanel = new JPanel();
        outerInfoPanel.setLayout(new BoxLayout(outerInfoPanel,BoxLayout.Y_AXIS));
        outerInfoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        outerInfoPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        outerInfoPanel.setBorder(BorderFactory.createCompoundBorder(
                                     BorderFactory.createTitledBorder("Current Datasets"),
                                     BorderFactory.createEmptyBorder(2,2,2,2)));
        treeSelectionInfoPanel = new JPanel();
        treeSelectionInfoPanel.setLayout(new BoxLayout(treeSelectionInfoPanel,BoxLayout.Y_AXIS));
        addTreeSelectionInterface();
        JScrollPane treeSelectionInfoScrollPane = new JScrollPane(treeSelectionInfoPanel);
        treeSelectionInfoScrollPane.setPreferredSize(new Dimension( rightWidth, windowHeight ));
        outerInfoPanel.add(treeSelectionInfoScrollPane);

        // Build xml Tree view
		scrollTree = new JScrollPane(xmlDOMTree);
        scrollTree.setPreferredSize(new Dimension( leftWidth, windowHeight ));
        treePanel = new JPanel();
        treePanel.setLayout(new BoxLayout(treePanel,BoxLayout.Y_AXIS));
        treePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        treePanel.setAlignmentY(Component.TOP_ALIGNMENT);
        EmptyBorder eb = new EmptyBorder(5,5,5,5);
        BevelBorder bb = new BevelBorder(BevelBorder.LOWERED);
        CompoundBorder cb = new CompoundBorder(eb,bb);
        treePanel.setBorder(new CompoundBorder(cb,eb));
        treePanel.setLayout(new BorderLayout());

		// Populate tree panel
		StringBuffer sbError = new StringBuffer(256);
		if( !zRefreshDisplayFromCache(sbError) ){
			ApplicationController.vShowError("Error refreshing dataset list from cache: " + sbError.toString());
		}

        // Put tree view and selection view into a splitPane
        windowSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,treePanel,outerInfoPanel);
        windowSplitPane.setContinuousLayout( true );
        windowSplitPane.setDividerLocation( leftWidth );
        windowSplitPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        windowSplitPane.setAlignmentY(Component.TOP_ALIGNMENT);
        windowSplitPane.setPreferredSize(new Dimension( windowWidth+10, windowHeight+10 ));
        windowSplitPane.setOneTouchExpandable(true);

        JPanel jpanelCommandBar = new JPanel();
		jpanelCommandBar.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		jpanelCommandBar.setLayout(new BoxLayout(jpanelCommandBar, BoxLayout.X_AXIS));
		vSetupCommandBar(jpanelCommandBar);

        // Add components
        this.setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        this.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.setAlignmentY(Component.TOP_ALIGNMENT);
        this.add(jpanelCommandBar);
        this.add(windowSplitPane);
    }

    public boolean zInitialize(StringBuffer sbError){ return true; }

	private void vSetupCommandBar(JPanel jpanelContainer){

        Button_Select jbuttonSelect = new Button_Select(this);
        jbuttonSelect.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jpanelContainer.add(jbuttonSelect);

		jpanelContainer.add(Box.createHorizontalStrut(2));

        mjbuttonRefresh = new JButton("Refresh Dataset List");
		mjbuttonRefresh.setMinimumSize(new Dimension(40, 30));
		if( isURLRemote() ){
			javax.swing.ImageIcon iiInternetConnectionIcon = Utility.imageiconLoadResource("icons/internet-connection-icon.gif");
			mjbuttonRefresh.setIcon(iiInternetConnectionIcon);
		}
        mjbuttonRefresh.addActionListener(new ActionRefresh());
        mjbuttonRefresh.setAlignmentX(JComponent.LEFT_ALIGNMENT);

		searchButton.setMinimumSize(new Dimension(40, 30));
        searchButton.addActionListener(new actionSearch());
        searchButton.setAlignmentX(JComponent.LEFT_ALIGNMENT);

		jbuttonShowAll.setMinimumSize(new Dimension(40, 30));
        jbuttonShowAll.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jbuttonShowAll.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					xmlDOMTree.removeConstraints();
					jbuttonShowAll.setEnabled(false);
					return;
				}
			}
		);
		jbuttonShowAll.setEnabled(false);

		jtfSearch = new JTextField();
		jtfSearch.setMinimumSize(new Dimension(40, 30));
		jtfSearch.setMaximumSize(new Dimension(120, 30));
		jtfSearch.setPreferredSize(new Dimension(400, 20));
        jtfSearch.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        jpanelContainer.add(mjbuttonRefresh);
		jpanelContainer.add(Box.createHorizontalStrut(2));
        jpanelContainer.add(searchButton);
		jpanelContainer.add(Box.createHorizontalStrut(2));
        jpanelContainer.add(jtfSearch);
		jpanelContainer.add(Box.createHorizontalStrut(2));
        jpanelContainer.add(jbuttonShowAll);

		jpanelContainer.add(Box.createHorizontalGlue());

	}

    //
    // Update Selection Pane
    //
    private void addTreeSelectionInterface() {

		xmlDOMTree.addMouseListener(
			new MouseAdapter(){
				public void mousePressed( MouseEvent me ){
					if( me.getClickCount() == 2 ){
						StringBuffer sbError = new StringBuffer(80);
						DodsURL[] urls = DatasetList.this.getURLs(sbError);
	    				if( urls == null ) return;
						Model_Retrieve retrieve_model = ApplicationController.getInstance().getRetrieveModel();
		    			retrieve_model.getURLList().vDatasets_Add( urls );
				    	ApplicationController.getInstance().getAppFrame().vActivateRetrievalPanel();

						// select the first URL in the added ones, and activate it
						DodsURL urlFirst = urls[0];
						Model_URLList modelURLList = retrieve_model.getURLList();
						Panel_URLList panelList = modelURLList.getControl();
						for( int iListIndex = 0; iListIndex < modelURLList.getSize(); iListIndex++ ){
							if( modelURLList.get( iListIndex ) == urlFirst ){
								panelList.vSelectIndex(iListIndex);
								ApplicationController.getInstance().getRetrieveModel().vShowURL( urlFirst, null );
							}
						}


					}
				}
			}
		);

        xmlDOMTree.addTreeSelectionListener(new TreeSelectionListener() {
		public void valueChanged(TreeSelectionEvent e) {
		    Object[] nodes = xmlDOMTree.getSelection();
		    if (nodes != null) {
				treeSelectionInfoPanel.removeAll();
				for (int i=0; i < nodes.length; i++) {
					DOMTree.AdapterNode thisnode = (DOMTree.AdapterNode) nodes[i];
					JLabel label = new JLabel();
					String text = "";
					if (thisnode.isLeaf()) {
						if (xmlDOMTree.numDesiredURLAttributes(thisnode) > 0) {
							text = thisnode.getAttributes().getNamedItem(DOMTree.ATTR_NAME).getNodeValue();
							for (int u=0; u < xmlDOMTree.desiredURLAttributes.length; u++) {
								if (thisnode.getAttributes().getNamedItem(DOMTree.desiredURLAttributes[u]) != null) {
									text += "\n  ";
									text += xmlDOMTree.desiredURLAttributes[u] + ": ";
									text += thisnode.getAttributes().getNamedItem(DOMTree.desiredURLAttributes[u]).getNodeValue();
								}
							}
						}
					} else {		// aggregated dataset item
						text += "\n";
						text += thisnode.getAttributes().getNamedItem(DOMTree.ATTR_NAME).getNodeValue();
					}
					label.setText(text);
					treeSelectionInfoPanel.add(label);
				}
				updateUI();
		    }
		}
	    });
    }

    // XML source management

	String getDatasetXML_URL(){
		String sURL = ConfigurationManager.getInstance().getProperty_URL_XML();
		if( sURL == null ) return null;
		java.net.URL urlDatasetListXML = null;
		try {
			urlDatasetListXML = new java.net.URL(sURL);
		} catch(Exception ex) {
			ApplicationController.vShowError("unable to interpret dataset URL [" + sURL + "]: " + ex);
		}
		return sURL;
	}

	void vDeleteCache(){
		String sCachePath = ConfigurationManager.getInstance().getProperty_PATH_XML_Cache();
		if( sCachePath == null ) return;
		try {
			java.io.File fileCache = new java.io.File(sCachePath);
			if( fileCache == null ) return;
			if( fileCache.exists()) fileCache.delete();
		} catch(Exception ex) {
			ApplicationController.vShowError("unable to delete cache [" + sCachePath + "]: " + ex);
			return;
		}
	}

	boolean isURLRemote(){
		String surlDatasetListXML = this.getDatasetXML_URL();
		if( surlDatasetListXML == null ) return false;
        String sProtocol = Utility.url_getPROTOCOL( surlDatasetListXML );
        if( sProtocol == null ) return false;
        if( sProtocol.equalsIgnoreCase("file") ){
			return false;
        } else {
			return true;
		}
	}

	java.io.FileInputStream getXMLInputStream(){
		String sCachePath = ConfigurationManager.getInstance().getProperty_PATH_XML_Cache();
		if( sCachePath == null ) return null;
		try {
			java.io.File fileCache = new java.io.File(sCachePath);
			if( fileCache == null ) return null;
			if( fileCache.exists()){
				java.io.FileInputStream fisCache = new java.io.FileInputStream( fileCache );
				return fisCache;
			} else {
				return null;
			}
		} catch(Exception ex) {
			ApplicationController.vShowError("unable to load file [" + sCachePath + "]: " + ex);
			return null;
		}
	}

	boolean zLoadXMLCache_NIO(StringBuffer sbError){
		java.nio.channels.FileChannel fcCache;
		java.nio.channels.ReadableByteChannel rbcSource;
		int iDatasetSourceSize = 0;
		String sCachePath = ConfigurationManager.getInstance().getProperty_PATH_XML_Cache();
		String sURI = ConfigurationManager.getInstance().getProperty_URL_XML();
		if( sCachePath == null ){
			sbError.append("no cache path defined");
			return false;
		}
		if( sURI == null ) return true; // in this case the cache is the source so there is no load
		try {
			java.io.File fileCache = new java.io.File(sCachePath);
			if( fileCache == null ){
				sbError.append("cache path is invalid [" + sCachePath + "]");
				return false;
			}
			java.io.FileOutputStream fosCache = new java.io.FileOutputStream( fileCache );
			fcCache = fosCache.getChannel();
		} catch(Exception ex) {
			sbError.append("error opening cache [" + sCachePath + "]: " + ex);
			return false;
		}
		try {
			String surlDatasetXML = this.getDatasetXML_URL();
			URI uriDatasetXML = new URI(surlDatasetXML);
			String sScheme = uriDatasetXML.getScheme();
			if( surlDatasetXML == null ){
				sbError.append("URL to dataset XML not supplied");
				try { if( fcCache != null ) fcCache.close(); } catch(Exception exclose) {}
				return false;
			} else if( sScheme.equalsIgnoreCase("file") ){
				String sProtocol = Utility.url_getPROTOCOL( surlDatasetXML );
				java.io.File fileSource = new java.io.File(surlDatasetXML);
				if( fileSource == null ){
					sbError.append("dataset source path is invalid [" + surlDatasetXML + "]");
					return false;
				}
				java.io.FileOutputStream fosSource = new java.io.FileOutputStream( fileSource );
				rbcSource = fosSource.getChannel();
			} else {
				java.io.InputStream isDatasetSource;
				java.net.URLConnection ucDatasetSource;
				try {
					isDatasetSource = uriDatasetXML.toURL().openStream();
					ucDatasetSource = uriDatasetXML.toURL().openConnection();
				} catch(Exception ex) {
					sbError.append("error opening dataset source [" + uriDatasetXML + "]: " + ex);
					try { if( fcCache != null ) fcCache.close(); } catch(Exception exclose) {}
					return false;
				}
				iDatasetSourceSize = ucDatasetSource.getContentLength();
				if( iDatasetSourceSize == -1 ){ // hopefully this won't happen
					ApplicationController.getInstance().vShowWarning("Dataset source provider server returned no content length.");
					iDatasetSourceSize = 1000000;
				}
				rbcSource = java.nio.channels.Channels.newChannel(ucDatasetSource.getInputStream());
			}
		} catch(Exception ex) {
			sbError.append("error opening dataset xml source [" + sURI + "]: " + ex);
			try { if( fcCache != null ) fcCache.close(); } catch(Exception exclose) {}
			return false;
		}
		try {
			fcCache.transferFrom(rbcSource, 0L, iDatasetSourceSize);
		} catch(Exception ex) {
			sbError.append("error transferring dataset source xml to cache: " + ex);
			return false;
		} finally {
			try { if( fcCache != null ) fcCache.close(); } catch(Exception exclose) {}
			try { if( rbcSource != null ) rbcSource.close(); } catch(Exception exclose) {}
		}
		return true;
	}

	boolean zUpdateCacheFromMasterList(StringBuffer sbError){

		// get relevant paths
		int iDatasetSourceSize = 0;
		final String sCachePath = ConfigurationManager.getInstance().getProperty_PATH_XML_Cache();
		final String sURI = ConfigurationManager.getInstance().getProperty_URL_XML();
		if( sCachePath == null ){
			sbError.append("no cache path defined");
			return false;
		}
		if( sURI == null ) return true; // in this case the cache is the source so there is no load

		// download the XML page to a string
		ByteCounter byte_counter = new ByteCounter(){
			public void vReportByteCount_EverySecond( long nByteCount ){
				ApplicationController.getInstance().vShowStatus_NoCache("Received " + Utility.getByteCountString(nByteCount) + " (" + nByteCount + ")");
			}
			public void vReportByteCount_Total( long nByteCount ){
				ApplicationController.getInstance().vShowStatus("Received " + Utility.getByteCountString(nByteCount) + " (" + nByteCount + ") for " + sURI);
			}
		};
		String sDatasetsXML = IO.getStaticContent(sURI, byte_counter, null, sbError );
		if( sDatasetsXML == null ){
			sbError.insert(0, "Error downloading datasets XML (" + sURI + "): ");
			return false;
		}

		// see if the content parses correctly before overwriting old file
		ByteArrayInputStream bais = new ByteArrayInputStream(sDatasetsXML.getBytes());
		org.w3c.dom.Document domParsedDocument = DOMTree.documentBuild( bais, sbError );
		if( domParsedDocument == null ){
			sbError.insert(0, "Error building DOM document for (" + sURI + "): ");
			return false;
		}

		// write document to cache file (this will overwrite the old cache)
		FileChannel fcCache = null;
		try {
			ByteBuffer bbXML = ByteBuffer.wrap(sDatasetsXML.getBytes());
			RandomAccessFile rafCache = new RandomAccessFile(sCachePath, "rw");
			fcCache = rafCache.getChannel();
			fcCache.truncate(bbXML.limit());
			fcCache.position(0);
			fcCache.write(bbXML);
		} catch( Throwable t ) {
			Utility.vUnexpectedError(t, sbError);
			return false;
		} finally {
			try { if( fcCache != null ) fcCache.close(); } catch(Exception ex){}
		}

		ApplicationController.vShowStatus("Updated " + sCachePath + " from " + sURI);
		return true;
	}

	boolean zLoadXMLCache_FromFile( FileOutputStream fosSource, FileOutputStream fosCache, StringBuffer sbError){
		if( fosSource == null ){
			sbError.append("no source file stream");
			return false;
		}
		if( fosCache == null ){
			sbError.append("no cache file stream");
			return false;
		}
		int iDatasetSourceSize = 0; // todo do we need to set this ?
		java.nio.channels.ReadableByteChannel rbcSource = null;
		java.nio.channels.FileChannel fcCache = null;
		try {
			rbcSource = fosSource.getChannel();
			fcCache = fosCache.getChannel();
			fcCache.transferFrom(rbcSource, 0L, iDatasetSourceSize);
			return true;
		} catch(Exception ex) {
			sbError.append("error transferring dataset source xml to cache: " + ex);
			return false;
		} finally {
			try { if( fcCache != null ) fcCache.close(); } catch(Exception exclose) {}
			try { if( rbcSource != null ) rbcSource.close(); } catch(Exception exclose) {}
		}
	}

	// 1.4.1 not used yet
	boolean zLoadXMLCache_FromStream( InputStream isSource, FileChannel fcCache, int iDatasetSourceSize, StringBuffer sbError){
		java.nio.channels.ReadableByteChannel rbcSource = java.nio.channels.Channels.newChannel(isSource);
		try {
			fcCache.transferFrom(rbcSource, 0L, iDatasetSourceSize);
			return true;
		} catch(Exception ex) {
			sbError.append("error doing file transfer: " + ex);
			return false;
		}
	}

	// translate tree selection event into a list selection event (kind of a kludge)
	public void addListSelectionListener(final ListSelectionListener listener){
        xmlDOMTree.addTreeSelectionListener(
			new TreeSelectionListener() {
			    public void valueChanged(TreeSelectionEvent e) {
					try {
						ListSelectionEvent lse = new ListSelectionEvent(xmlDOMTree, -1, -1, false);
						listener.valueChanged( lse );
						return;
					} catch(Exception ex) {}
		    	}
			}
		);
	}

    // Returns DodsUrl object with selected URLs
	public DodsURL[] getURLs( StringBuffer sbError ) {
        Hashtable urlshash = new Hashtable();
        Vector urlsvect = new Vector();
        Object[] nodes = xmlDOMTree.getSelection();
		if (nodes != null) {
            for (int i=0; i < nodes.length; i++) {
                DOMTree.AdapterNode thisnode = (DOMTree.AdapterNode) nodes[i];
                if (thisnode.isLeaf()) {
					String key = null;
                    if (thisnode.getAttributes().getNamedItem(DOMTree.ATTR_BASE_URL) != null) {
                        key = thisnode.getAttributes().getNamedItem(DOMTree.ATTR_BASE_URL).getNodeValue();
                    } else if( thisnode.getAttributes().getNamedItem(DOMTree.ATTR_DIR) != null) {
						key = thisnode.getAttributes().getNamedItem(DOMTree.ATTR_DIR).getNodeValue();
					} else {
						org.w3c.dom.Node nodeUnusable = thisnode.getAttributes().getNamedItem(DOMTree.ATTR_NAME);
						String sAttributeName = nodeUnusable == null ? "[unknown]" : nodeUnusable.getNodeValue();
						ApplicationController.getInstance().vShowWarning("Entry is not a data URL or directory URL: " + sAttributeName);
				    }
					if( key != null ){
						if (! urlshash.containsKey(key)) {
							urlshash.put(key,key);
							urlsvect.addElement(thisnode);
						}
					}
                } else {	// aggregated entry
                    for (int s=0; s < thisnode.childCount(); s++) {
						String key = null;
						if (thisnode.getChild(s).getAttributes().getNamedItem(DOMTree.ATTR_BASE_URL) != null) {
							key = thisnode.getChild(s).getAttributes().getNamedItem(DOMTree.ATTR_BASE_URL).getNodeValue();
						} else if( thisnode.getChild(s).getAttributes().getNamedItem(DOMTree.ATTR_DIR) != null) {
							key = thisnode.getChild(s).getAttributes().getNamedItem(DOMTree.ATTR_DIR).getNodeValue();
						} else {
							org.w3c.dom.Node nodeUnusable = thisnode.getAttributes().getNamedItem(DOMTree.ATTR_NAME);
							String sAttributeName = nodeUnusable == null ? "[unknown]" : nodeUnusable.getNodeValue();
							ApplicationController.getInstance().vShowStatus_NoCache("Entry is not a data URL or directory URL: " + sAttributeName);
						}
						if( key != null ){
							if (! urlshash.containsKey(key)) {
								urlshash.put(key,key);
								urlsvect.addElement(thisnode.getChild(s));
							}
						}
                    }
                }
            }
        }

        if (urlsvect.size() > 0) {
            DodsURL[] urls = new DodsURL[urlsvect.size()];
            for (int i=0; i < urlsvect.size(); i++) {
                urls[i] = new DodsURL();
                DOMTree.AdapterNode thisnode = (DOMTree.AdapterNode) urlsvect.elementAt(i);
                if (thisnode.getAttributes().getNamedItem(DOMTree.ATTR_CATALOG) != null ||
					thisnode.getAttributes().getNamedItem(DOMTree.ATTR_DIR) != null) {
                    urls[i].setType(DodsURL.TYPE_Directory);
					urls[i].setURL(thisnode.getAttributes().getNamedItem(DOMTree.ATTR_DIR).getNodeValue());
                } else {
                    urls[i].setType(DodsURL.TYPE_Data);
					urls[i].setURL(thisnode.getAttributes().getNamedItem(DOMTree.ATTR_BASE_URL).getNodeValue());
                }
                urls[i].setTitle(thisnode.getAttributes().getNamedItem(DOMTree.ATTR_NAME).getNodeValue());
            }
            return urls;
        } else {
            return null;
        }
    }

	private boolean zRefreshDisplayFromCache(StringBuffer sbError){
		if( xmlDOMTree.zRefreshTreeFromCacheFile(sbError) ){
			treePanel.removeAll();
			treePanel.add(scrollTree,BorderLayout.CENTER);
			return true;
		} else {

			// post message to tree panel
			String sCachePath = ConfigurationManager.getInstance().getProperty_PATH_XML_Cache();
			JTextArea jtaNoTree = new JTextArea();
			jtaNoTree.setText(sbError.toString());
			jtaNoTree.setPreferredSize( new Dimension( leftWidth, windowHeight ));
			treePanel.add(jtaNoTree, BorderLayout.CENTER);

			// and return error
			sbError.insert(0, "Failed to refresh tree from cache file: " + sbError);
			return false;
		}
	}

	class ActionRefresh implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			Activity activityRefreshCache = new Activity();
			Continuation_DoCancel conLoadXMLCache =
				new Continuation_DoCancel(){
					public void Do(){
						StringBuffer sbError = new StringBuffer(256);
						if( DatasetList.this.zUpdateCacheFromMasterList(sbError) ){
							if( zRefreshDisplayFromCache(sbError) ){
								String sCachePath = ConfigurationManager.getInstance().getProperty_PATH_XML_Cache();
								ApplicationController.vShowStatus("Updated dataset list tree from: " + sCachePath);
							} else {
								ApplicationController.vShowError(sbError.toString());
							}
						} else {
							ApplicationController.vShowError("Failed to get master dataset list: " + sbError);
						}
					}
					public void Cancel(){
					}
				};
			activityRefreshCache.vDoActivity(mjbuttonRefresh, this, conLoadXMLCache, "Refreshing dataset xml cache ...");
		}
	}

	class actionSearch implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			String sSearchText = jtfSearch.getText();
			if( sSearchText == null || sSearchText.length() == 0 ){
				xmlDOMTree.removeConstraints();
				jbuttonShowAll.setEnabled(false);
				return;
			} else {
				jbuttonShowAll.setEnabled(true);
			}
			String[] as = Utility.split(sSearchText, ' ');
			int ctAnd = 0, ctOr = 0;
			for( int xSearchTerm = 0; xSearchTerm < as.length; xSearchTerm++ ){
				if( as[xSearchTerm].startsWith("+") ) ctAnd++; else ctOr++;
			}
			Object[] aoAnd, aoOr;
			if( ctOr == 0 ){
				aoAnd = as;
				aoOr = new String[1];
				aoOr[0] = "";
				for( int xSearchTerm = 0; xSearchTerm < as.length; xSearchTerm++ ){
					as[xSearchTerm] = as[xSearchTerm].substring(1);
				}
			} else if( ctAnd == 0 ){
				aoOr = as;
				aoAnd = new String[1];
				aoAnd[0] = "";
			} else {
				aoAnd = new Object[ctAnd];
				aoOr = new Object[ctOr];
				int xAnd = 0, xOr = 0;
				for( int xSearchTerm = 0; xSearchTerm < as.length; xSearchTerm++ ){
					if( as[xSearchTerm].startsWith("+") ){
						aoAnd[xAnd] = as[xSearchTerm].substring(1);
						xAnd++;
					} else {
						aoOr[xOr] = as[xSearchTerm];
						xOr++;
					}
				}
			}
			StringBuffer sbError = new StringBuffer(80);
            int searchResult = xmlDOMTree.constrainTree(
                                   aoAnd, "AND",
                                   "AND",
                                   aoOr, "OR", sbError );
            switch(searchResult){
				case DOMTree.SEARCH_NULL:
					ApplicationController.vShowStatus("Invalid Search");
					break;
				case DOMTree.SEARCH_NO_MATCH:
					ApplicationController.vShowStatus("The search returned no matches.");
					break;
				case DOMTree.SEARCH_ERROR:
					ApplicationController.vShowStatus("An error was encountered during the search.");
					break;
				case DOMTree.SEARCH_MATCH_FOUND:
					// do nothing
					break;
            }
		}
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            DatasetList datalist = new DatasetList();

            if (datalist != null) {
                // Create a frame and container for the panels.
                JFrame datasetListFrame = new JFrame("DatasetList");

                // Set the look and feel.
                try {
                    UIManager.setLookAndFeel(
                        UIManager.getCrossPlatformLookAndFeelClassName());
                } catch(Exception e) {}

                datasetListFrame.setContentPane(datalist);

                // Exit when the window is closed.
                // This constant doesn't exist in JDK1.1.  rph 08/15/01
                // datasetListFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                datasetListFrame.pack();
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int w = windowWidth + 10;
                int h = windowHeight + 10;
                datasetListFrame.setLocation(screenSize.width/3 - w/2, screenSize.height/2 - h/2);
                datasetListFrame.setSize(w, h);
                datasetListFrame.setVisible(true);
            }
        } else {
            System.out.println("\nUsage: java DatasetList xmlfile");
        }
    }

}



