package opendap.clients.odc.DatasetList;

import opendap.clients.odc.*;
import opendap.clients.odc.data.Model_Dataset;
import opendap.clients.odc.data.Model_Dataset.DATASET_TYPE;
import opendap.clients.odc.data.Model_Retrieve;
import opendap.clients.odc.data.Model_URLList;
import opendap.clients.odc.data.Panel_URLList;
import opendap.clients.odc.gui.Resources;

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

/**
 * This class provides the base structure for the DatasetList application.
 *
 * @version     2.00 Jul 2002
 * @author      Kashan A. Shaikh
 * @modified by Sheila Jiang 12 Mar 2002
 * @modified by John Chamberlain July 2002, December 2003, June 2004, July 2004, March 2012
 *
 * 1.26 July 2001
 * 2.14 August 2002
 * 2.30 December 2003
 * 2.49 June 2004
 * 2.53 July 2004
 * 3.05 October 2009
 * 3.08 March 2012      THREDDS mode added
 *
 */
public class DatasetList extends SearchInterface {

	public enum MODE {
		DatasetList,
		THREDDS
	}
	
	private MODE mode;
	
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

	private DatasetList() {}
    
	public MODE getMode(){ return mode; }

	public static DatasetList create( MODE mode, StringBuffer sbError ){
		DatasetList panel = new DatasetList();
		panel.mode = mode;
		panel.xmlDOMTree = new DOMTree( panel );

        // Selection window
		JPanel outerInfoPanel = new JPanel();
		outerInfoPanel.setLayout(new BoxLayout(outerInfoPanel,BoxLayout.Y_AXIS));
		outerInfoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		outerInfoPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		outerInfoPanel.setBorder(BorderFactory.createCompoundBorder(
                                     BorderFactory.createTitledBorder("Current Datasets"),
                                     BorderFactory.createEmptyBorder(2,2,2,2)));
		panel.treeSelectionInfoPanel = new JPanel();
		panel.treeSelectionInfoPanel.setLayout( new BoxLayout( panel.treeSelectionInfoPanel,BoxLayout.Y_AXIS ));
		panel.addTreeSelectionInterface();
		JScrollPane treeSelectionInfoScrollPane = new JScrollPane( panel.treeSelectionInfoPanel );
		treeSelectionInfoScrollPane.setPreferredSize(new Dimension( rightWidth, windowHeight ));
		outerInfoPanel.add(treeSelectionInfoScrollPane);

        // Build xml Tree view
		panel.scrollTree = new JScrollPane( panel.xmlDOMTree );
		panel.scrollTree.setPreferredSize(new Dimension( leftWidth, windowHeight ));
		panel.treePanel = new JPanel();
		panel.treePanel.setLayout( new BoxLayout( panel.treePanel,BoxLayout.Y_AXIS ));
		panel.treePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.treePanel.setAlignmentY(Component.TOP_ALIGNMENT);
		EmptyBorder eb = new EmptyBorder(5,5,5,5);
		BevelBorder bb = new BevelBorder(BevelBorder.LOWERED);
		CompoundBorder cb = new CompoundBorder(eb,bb);
		panel.treePanel.setBorder( new CompoundBorder(cb,eb) );
		panel.treePanel.setLayout( new BorderLayout() );

		// Populate tree panel
		String sCachePath = panel.getCachePath();
		if( panel.zRefreshDisplayFromCache( sCachePath, sbError )){
			// refresh method updates panel content
		} else {
			ApplicationController.vShowWarning( "Error refreshing dataset list from cache: " + sbError.toString() );
			sbError.setLength( 0 );
		}

        // Put tree view and selection view into a splitPane
		panel.windowSplitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, panel.treePanel, outerInfoPanel );
		panel.windowSplitPane.setContinuousLayout( true );
		panel.windowSplitPane.setDividerLocation( leftWidth );
		panel.windowSplitPane.setAlignmentX( Component.CENTER_ALIGNMENT );
		panel.windowSplitPane.setAlignmentY( Component.TOP_ALIGNMENT );
		panel.windowSplitPane.setPreferredSize( new Dimension( windowWidth+10, windowHeight+10 ) );
		panel.windowSplitPane.setOneTouchExpandable( true );

		JPanel jpanelCommandBar = new JPanel();
		jpanelCommandBar.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		jpanelCommandBar.setLayout(new BoxLayout(jpanelCommandBar, BoxLayout.X_AXIS));
		panel.vSetupCommandBar( jpanelCommandBar );

        // Add components
        panel.setLayout( new BoxLayout( panel,BoxLayout.Y_AXIS ));
        panel.setAlignmentX( Component.CENTER_ALIGNMENT );
        panel.setAlignmentY( Component.TOP_ALIGNMENT );
        panel.add( jpanelCommandBar );
        panel.add( panel.windowSplitPane );
        
        return panel;
    }

	private void vSetupCommandBar(JPanel jpanelContainer){

        Button_Select jbuttonSelect = new Button_Select(this);
        jbuttonSelect.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jpanelContainer.add(jbuttonSelect);

		jpanelContainer.add(Box.createHorizontalStrut(2));

        mjbuttonRefresh = new JButton("Refresh Dataset List");
		mjbuttonRefresh.setMinimumSize(new Dimension(40, 30));
		if( isURLRemote() ){
			if( Resources.imageiconInternet != null ) mjbuttonRefresh.setIcon(Resources.imageiconInternet);
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
						SwingUtilities.invokeLater(
							new Runnable(){
								public void run(){
									vAddSelectedURLs();
								}
							}
						);
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
							for (int u=0; u < DOMTree.desiredURLAttributes.length; u++) {
								if (thisnode.getAttributes().getNamedItem(DOMTree.desiredURLAttributes[u]) != null) {
									text += "\n  ";
									text += DOMTree.desiredURLAttributes[u] + ": ";
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

	private final void vAddSelectedURLs(){
		try {
			StringBuffer sbError = new StringBuffer(80);
			Model_Dataset[] urls = DatasetList.this.getURLs(sbError);
			if( urls == null ) return;
			Model_Retrieve retrieve_model = ApplicationController.getInstance().getRetrieveModel();
			retrieve_model.getURLList().vDatasets_Add( urls );
			ApplicationController.getInstance().getAppFrame().vActivateRetrievalPanel();

			// select the first URL in the added ones, and activate it
			Model_Dataset urlFirst = urls[0];
			Model_URLList modelURLList = retrieve_model.getURLList();
			Panel_URLList panelList = modelURLList.getControl();
			for( int iListIndex = 0; iListIndex < modelURLList.getSize(); iListIndex++ ){
				if( modelURLList.get( iListIndex ) == urlFirst ){
					panelList.vSelectIndex(iListIndex);
					if( urlFirst.getType() == DATASET_TYPE.Data )
						ApplicationController.getInstance().getRetrieveModel().getRetrievePanel().vShowDirectory( false ); // data URLs do not have directories
					ApplicationController.getInstance().getRetrieveModel().vShowURL( urlFirst, null );
				}
			}
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "adding selected URLs from dataset list" );
		}
	}

    // XML source management

	String getURL_CatalogXML( StringBuffer sbError ){
		String sURL;
		String sTarget;
		if( mode == MODE.DatasetList ){
			sURL = ConfigurationManager.getInstance().getProperty_URL_DatasetList();
			sTarget = "Dataset List";
		} else if( mode == MODE.DatasetList ){
			sURL = ConfigurationManager.getInstance().getProperty_URL_THREDDS();
			sTarget = "THREDDS master catalog";
		} else {
			sbError.append( "Internal error, unknown mode" );
			return null;
		}
		if( sURL == null ){
			sbError.append( "The URL for " + mode + " is set to null in your configuration. This setting must point to the " + sTarget + "." );
			return null;
		}
		try {
			new java.net.URL( sURL ); // see if we can make a valid URL
		} catch(Exception ex) {
			ApplicationController.vShowError("unable to interpret URL [" + sURL + "]: " + ex);
		}
		return sURL;
	}

	private String getCachePath(){
		String sCachePath;
		if( mode == MODE.DatasetList ){
			sCachePath = ConfigurationManager.getInstance().getProperty_PATH_XML_DatasetList_Cache();
		} else if( mode == MODE.THREDDS ){
			sCachePath = ConfigurationManager.getInstance().getProperty_PATH_XML_THREDDS_Cache();
		} else return null;
		return sCachePath;
	}

	private String getCatalogURL(){
		String sURL;
		if( mode == MODE.DatasetList ){
			sURL = ConfigurationManager.getInstance().getProperty_URL_DatasetList();
		} else if( mode == MODE.THREDDS ){
			sURL = ConfigurationManager.getInstance().getProperty_URL_THREDDS();
		} else return null;
		return sURL;
	}
	
	void vDeleteCache(){
		String sCachePath = getCachePath();
		try {
			java.io.File fileCache = new java.io.File(sCachePath);
			if( fileCache.exists()) fileCache.delete();
		} catch(Exception ex) {
			ApplicationController.vShowError("unable to delete cache [" + sCachePath + "]: " + ex);
			return;
		}
	}

	boolean isURLRemote(){
		StringBuffer sbError = new StringBuffer();
		String surlDatasetListXML = this.getURL_CatalogXML( sbError );
		if( surlDatasetListXML == null ){
			ApplicationController.vShowWarning( "Unable to get URL for data catalog: " + sbError );
			return false;
		}
        String sProtocol = Utility.url_getPROTOCOL( surlDatasetListXML );
        if( sProtocol == null ) return false;
        if( sProtocol.equalsIgnoreCase("file") ){
			return false;
        } else {
			return true;
		}
	}

	boolean zLoadXMLCache_NIO( StringBuffer sbError ){
		java.nio.channels.FileChannel fcCache;
		java.nio.channels.ReadableByteChannel rbcSource;
		int iDatasetSourceSize = 0;
		String sCachePath = getCachePath();
		if( sCachePath == null ){
			sbError.append( "no cache path defined" );
			return false;
		}
		String sURI;
		if( mode == MODE.DatasetList ){
			sURI = ConfigurationManager.getInstance().getProperty_URL_DatasetList();
		} else if( mode == MODE.THREDDS ){
			sURI = ConfigurationManager.getInstance().getProperty_URL_THREDDS();
		} else return true;
		if( sURI == null ) return true; // in this case the cache is the source so there is no load
		try {
			java.io.File fileCache = new java.io.File( sCachePath );
			java.io.FileOutputStream fosCache = new java.io.FileOutputStream( fileCache );
			fcCache = fosCache.getChannel();
		} catch(Exception ex) {
			sbError.append("error opening cache [" + sCachePath + "]: " + ex);
			return false;
		}
		try {
			String surlDatasetXML = getURL_CatalogXML( sbError );
			if( surlDatasetXML == null ){
				sbError.insert( 0, "error getting catalog XML URL: " );
				return false;
			}
			URI uriDatasetXML = new URI( surlDatasetXML );
			String sScheme = uriDatasetXML.getScheme();
			if( sScheme.equalsIgnoreCase("file") ){
				String sProtocol = Utility.url_getPROTOCOL( surlDatasetXML );
				if( ! sProtocol.equalsIgnoreCase( "FILE" ) ){
					sbError.append( "URI validation failed, scheme was file, but protocol for [" + surlDatasetXML + "] does not appear to be file-based" );
					return false;
				}
				java.io.File fileSource = new java.io.File( surlDatasetXML );
				java.io.FileOutputStream fosSource = new java.io.FileOutputStream( fileSource );
				rbcSource = fosSource.getChannel();
			} else {
				java.net.URLConnection ucDatasetSource;
				try {
					ucDatasetSource = uriDatasetXML.toURL().openConnection();
				} catch(Exception ex) {
					sbError.append("error opening dataset source [" + uriDatasetXML + "]: " + ex);
					try { if( fcCache != null ) fcCache.close(); } catch( Throwable t ){}
					return false;
				}
				iDatasetSourceSize = ucDatasetSource.getContentLength();
				if( iDatasetSourceSize == -1 ){ // hopefully this won't happen
					ApplicationController.vShowWarning( "Dataset source provider server returned no content length." );
					iDatasetSourceSize = 1000000;
				}
				rbcSource = java.nio.channels.Channels.newChannel( ucDatasetSource.getInputStream() );
			}
		} catch(Exception ex) {
			sbError.append("error opening dataset xml source [" + sURI + "]: " + ex);
			try { if( fcCache != null ) fcCache.close(); } catch(Exception exclose) {}
			return false;
		}
		try {
			fcCache.transferFrom( rbcSource, 0L, iDatasetSourceSize );
		} catch(Exception ex) {
			sbError.append("error transferring dataset source xml to cache: " + ex);
			return false;
		} finally {
			try { if( fcCache != null ) fcCache.close(); } catch(Exception exclose) {}
			try { if( rbcSource != null ) rbcSource.close(); } catch(Exception exclose) {}
		}
		return true;
	}

	boolean zUpdateCacheFromMasterCatalog( StringBuffer sbError ){

		// get relevant paths
		final String sCachePath = getCachePath();
		final String sURI = getCatalogURL();
		if( sCachePath == null ){
			sbError.append("no cache path defined");
			return false;
		}
		if( sURI == null ) return true; // in this case the cache is the source so there is no load

		// download the XML page to a string
		ByteCounter byte_counter = new ByteCounter(){
			public void vReportByteCount_EverySecond( long nByteCount ){
				ApplicationController.vShowStatus_NoCache("Received " + Utility_String.getByteCountString(nByteCount) + " (" + nByteCount + ")");
			}
			public void vReportByteCount_Total( long nByteCount ){
				ApplicationController.vShowStatus("Received " + Utility_String.getByteCountString(nByteCount) + " (" + nByteCount + ") for " + sURI);
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
		} catch( java.io.FileNotFoundException fnf ) {
			sbError.append("error creating/opening file; this problem may be caused by a bug in MacOS 10.3, if you are using a Macintosh make sure it is 10.4 or later");
			return false;
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError(t, sbError);
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
			sbError.append( "error transferring dataset source xml to cache: " + ex );
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
	public Model_Dataset[] getURLs( StringBuffer sbError ) {
        Hashtable<String,String> urlshash = new Hashtable<String,String>();
        Vector<DOMTree.AdapterNode> urlsvect = new Vector<DOMTree.AdapterNode>();
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
						ApplicationController.vShowWarning("Entry is not a data URL or directory URL: " + sAttributeName);
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
						DOMTree.AdapterNode nodeChild = thisnode.getChild( s, sbError );
						if( nodeChild == null ){
							ApplicationController.vShowWarning("Error getting child " + s + ": " + sbError );
							return null;
						}
						if( nodeChild.getAttributes().getNamedItem(DOMTree.ATTR_BASE_URL) != null) {
							key = nodeChild.getAttributes().getNamedItem(DOMTree.ATTR_BASE_URL).getNodeValue();
						} else if( nodeChild.getAttributes().getNamedItem(DOMTree.ATTR_DIR) != null) {
							key = nodeChild.getAttributes().getNamedItem(DOMTree.ATTR_DIR).getNodeValue();
						} else {
							org.w3c.dom.Node nodeUnusable = thisnode.getAttributes().getNamedItem(DOMTree.ATTR_NAME);
							String sAttributeName = nodeUnusable == null ? "[unknown]" : nodeUnusable.getNodeValue();
							ApplicationController.vShowStatus_NoCache("Entry is not a data URL or directory URL: " + sAttributeName);
						}
						if( key != null ){
							if (! urlshash.containsKey(key)) {
								urlshash.put(key,key);
								urlsvect.addElement( nodeChild );
							}
						}
                    }
                }
            }
        }

        if( urlsvect.size() > 0 ){
            Model_Dataset[] urls = new Model_Dataset[urlsvect.size()];
            for (int i=0; i < urlsvect.size(); i++) {
                DOMTree.AdapterNode thisnode = (DOMTree.AdapterNode) urlsvect.elementAt(i);
                String sURL = thisnode.getAttributes().getNamedItem(DOMTree.ATTR_DIR).getNodeValue();
                if( thisnode.getAttributes().getNamedItem(DOMTree.ATTR_CATALOG) != null ||
					thisnode.getAttributes().getNamedItem(DOMTree.ATTR_DIR) != null) {
                	Model_Dataset model = Model_Dataset.createDirectoryFromURL( sURL, sbError );
                	if( model == null ){
                		sbError.insert( 0, "unable to create internal model for directory: " );
                		return null;
                	}
                	urls[i] = model;
                } else {
                	Model_Dataset model = Model_Dataset.createDirectoryFromURL( sURL, sbError );
                	if( model == null ){
                		sbError.insert( 0, "unable to create internal model for directory: " );
                		return null;
                	}
                	urls[i] = model;
                }
                urls[i].setTitle(thisnode.getAttributes().getNamedItem(DOMTree.ATTR_NAME).getNodeValue());
            }
            return urls;
        } else {
            return null;
        }
    }

	private boolean zRefreshDisplayFromCache( String sCacheFilePath, StringBuffer sbError ){
		if( xmlDOMTree.zRefreshTreeFromCacheFile( sCacheFilePath, sbError ) ){
			treePanel.removeAll();
			treePanel.add(scrollTree,BorderLayout.CENTER);
			return true;
		} else {

			// post message to tree panel
			JTextArea jtaNoTree = new JTextArea();
			jtaNoTree.setText( sbError.toString() );
			jtaNoTree.setPreferredSize( new Dimension( leftWidth, windowHeight ));
			treePanel.add( jtaNoTree, BorderLayout.CENTER );

			// and return error
			sbError.insert(0, "Failed to refresh tree from cache file: ");

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
						if( DatasetList.this.zUpdateCacheFromMasterCatalog(sbError) ){
							String sCachePath = getCachePath();
							if( zRefreshDisplayFromCache( sCachePath, sbError ) ){
								ApplicationController.vShowStatus( "Updated " + mode + " catalog from: " + sCachePath );
							} else {
								ApplicationController.vShowError( "Failed to update catalog from " + sCachePath + " for " + mode + ": " + sbError.toString() );
							}
						} else {
							ApplicationController.vShowError( "Failed to get master catalog for " + mode + ": " + sbError);
						}
					}
					public void Cancel(){
					}
				};
			activityRefreshCache.vDoActivity( mjbuttonRefresh, this, conLoadXMLCache, "Refreshing " + mode + " catalog xml cache ..." );
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
			String[] as = Utility_String.split( sSearchText, ' ' );
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
			String sCachePath = DatasetList.this.getCachePath();
			if( sCachePath == null ){
				ApplicationController.vShowError( "Unable to constrain search, no cache path. A catalog must be loaded and cached before a constraint can be applied." );
				return;
			}
            int searchResult = xmlDOMTree.constrainTree( sCachePath,
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
					ApplicationController.vShowStatus( "An error was encountered during the search." );
					break;
				case DOMTree.SEARCH_MATCH_FOUND:
					// do nothing
					break;
				case DOMTree.SEARCH_BAD_DOCUMENT:
					ApplicationController.vShowStatus( "The catalog cache file [" + sCachePath + "] did not load." );
					break;
            }
		}
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            DatasetList datalist = new DatasetList();

            if (datalist != null) {
                // Create a frame and container for the panels.
                JFrame datasetListFrame = new JFrame( "" + datalist.mode + " Catalog");

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



