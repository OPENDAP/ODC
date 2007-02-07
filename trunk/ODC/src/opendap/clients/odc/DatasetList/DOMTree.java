/**
 * DOMTree.java
 *
 * 1.00 2001/7/26
 *
 */

package opendap.clients.odc.DatasetList;

import opendap.clients.odc.*;

import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.*;

import javax.swing.tree.*;

import java.util.regex.*;

/**
 * This class echos an uriDatasetListXML
 *
 * @version     1.00 26 Jul 2001
 * @author      Kashan A. Shaikh
 */
public class DOMTree extends JTree {
    final static public int SEARCH_NULL = 0;
    final static public int SEARCH_NO_MATCH = 1;
    final static public int SEARCH_ERROR = 2;
    final static public int SEARCH_MATCH_FOUND = 3;

    static private String listTitle = "DODS Dataset List";

    // DOM object
    private Document document;
    private Document currentDoc;

    // List of elements
    static private String[] treeElementNames = {
        "datasets",
        "provider",
        "dataset",
        "subdataset",
    };

    static private String rootTitle = "Data Providers";
    static public String ELEMENT_ROOT = "datasets";
    static public String ELEMENT_DATASET = "dataset";

    // List of attributes
    static private String[] desiredTreeAttributes = {
		"baseURL",
        "name",
        "url",
        "catalog",
        "dir",
        "gcmd",
    };

    static public String ATTR_NAME = "name";
    static public String ATTR_CATALOG = "catalog";
    static public String ATTR_DIR = "dir";

    // List of desired URL attributes
    static public String[] desiredURLAttributes = {
        "baseURL", "dir"
    };

    static public String ATTR_BASE_URL = "baseURL";

	DatasetList mParent = null;
    public DOMTree( opendap.clients.odc.DatasetList.DatasetList parent){
		this.mParent = parent;
	}

	boolean zRefreshTreeFromCacheFile( StringBuffer sbError ){
		java.io.FileInputStream fisXML = mParent.getXMLInputStream( sbError );
		if( fisXML == null ){
			sbError.insert(0, "dataset list xml unavailable: ");
			return false;
		}

		Document domParsedDocument = documentBuild( fisXML, sbError );
		if( domParsedDocument == null ) return false;
		document   = domParsedDocument;
        currentDoc = domParsedDocument;

        // Set up the tree
		DomToTreeModelAdapter theTreeModelAdapter = new DomToTreeModelAdapter(document);
        setModel(theTreeModelAdapter);

		// Remove unusuable nodes
		this.vRemoveUnusableNodes(theTreeModelAdapter);

		// display the top level nodes
        expandRow(1);

		return true;
	}

	static org.w3c.dom.Document documentBuild( java.io.InputStream is, StringBuffer sbError ){
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document parsed_document;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
	        parsed_document = builder.parse(is);
        } catch (SAXException sxe) {
            // Error generated during parsing
            Exception  x = sxe;
            if (sxe.getException() != null) x = sxe.getException();
            x.printStackTrace();
			sbError.append("dataset list xml parse error: " + sxe);
			return null;
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            pce.printStackTrace();
			sbError.append("dataset list xml parser configuration error: " + pce);
			return null;
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
			sbError.append("dataset list xml load I/O error: " + ioe);
			return null;
        } catch( Throwable t ) {
			Utility.vUnexpectedError(t, sbError);
			return null;
		}
		return parsed_document;
	}

	private void vRemoveUnusableNodes(DomToTreeModelAdapter tree){
		Object oRoot = tree.getRoot();
		if( oRoot == null ) return;
		DOMTree.AdapterNode nodeRoot = (DOMTree.AdapterNode)oRoot;
		vRemoveUnusableNodes( nodeRoot );
	}

	private void vRemoveUnusableNodes( DOMTree.AdapterNode node ){
		if( node == null ) return;
		for( int xChild = node.childCount() - 1; xChild >= 0 ; xChild-- ){
			DOMTree.AdapterNode nodeChild = node.getChild(xChild);
			if (nodeChild.isLeaf()) {
				if( nodeChild.getAttributes().getNamedItem(DOMTree.ATTR_BASE_URL) != null ) continue; // data url
				if( nodeChild.getAttributes().getNamedItem(DOMTree.ATTR_DIR) != null ) continue; // directory url
				node.removeChild(xChild); // not a valid node
			} else {
				vRemoveUnusableNodes( nodeChild ) ;
				if( nodeChild.childCount() < 1 ){ // all children have been removed
					node.removeChild(xChild); // directory node no longer has children
				}
			}
		}
	}

    //
    // Returns an array of the selected nodes
    //
    public Object[] getSelection() {
        Hashtable hashNodes = new Hashtable();
        Vector vnodes = new Vector();
        TreePath[] selectionPath = getSelectionPaths();
        DomToTreeModelAdapter model = new DomToTreeModelAdapter(currentDoc);
        for (int s=0; s < getSelectionCount(); s++) {
            AdapterNode temp = null;
            boolean leafFound = false;
            for (int i=0; i < selectionPath[s].getPathCount(); i++) {
                AdapterNode node = (AdapterNode) selectionPath[s].getPathComponent(i);
                if (node != null) {
                    if (node.getAttributes() != null) {
                        if (model.isLeaf(node)) {
                            if (! hashNodes.containsKey(node.toString())) {
                                hashNodes.put(node.toString(),node.toString());
                                vnodes.addElement(node);
                                leafFound = true;
                            }
                        } else if (node.getNodeName().equals(ELEMENT_DATASET)) {
                            if (! hashNodes.containsKey(node.toString())) {
                                temp = node;
                            }
                        }
                    }
                }
            }
            if ((! leafFound) && (temp != null)) {
                hashNodes.put(temp.toString(),temp.toString());
                vnodes.addElement(temp);
            }
            temp = null;
        }

        Object[] vnodeArray = vnodes.toArray();
        return vnodeArray;
    }


    //
    // Returns the number of desired URL attributes found in the node
    //
    public int numDesiredURLAttributes(AdapterNode node) {
        if ((node == null) || (node.getAttributes() == null)) return 0;
        int count = 0;
        for (int i=0; i < desiredURLAttributes.length; i++) {
            if (node.getAttributes().getNamedItem(desiredURLAttributes[i]) != null) {
                count++;
            }
        }
        return count;
    }


    //
    // Removes all Constraints
    //
    public void removeConstraints() {
        setModel(new DomToTreeModelAdapter(document));
        expandRow(1);
    }


    public int constrainTree(Object[] keywords_G1, String logic_G1,
                             String logic_between,
                             Object[] keywords_G2, String logic_G2,
							 StringBuffer sbError) {

		if( keywords_G1 == null || logic_G1 == null || logic_between == null || keywords_G2 == null || logic_G2 == null ){
			sbError.append("Internal Error, one or more null inputs");
			return this.SEARCH_ERROR;
		}

        // setup regular expressions
        java.util.regex.Pattern[] search_G1 = new java.util.regex.Pattern[keywords_G1.length];
        java.util.regex.Pattern[] search_G2 = new java.util.regex.Pattern[keywords_G2.length];
        try {
            for (int i=0; i < keywords_G1.length; i++) {
				String sPattern =  keywords_G1[i].toString();
                search_G1[i] = Pattern.compile(sPattern, Pattern.CASE_INSENSITIVE);
            }
            for (int i=0; i < keywords_G2.length; i++) {
				String sPattern = keywords_G2[i].toString();
                search_G2[i] = Pattern.compile(sPattern, Pattern.CASE_INSENSITIVE);
            }
        } catch(Exception e) {}

        // setup logic
        boolean init_remove_G1 = true;
        if (logic_G1.equalsIgnoreCase("AND")) {
            init_remove_G1 = false;
        } else if (logic_G1.equalsIgnoreCase("OR")) {
            init_remove_G1 = true;
        }
        boolean init_remove_G2 = true;
        if (logic_G2.equalsIgnoreCase("AND")) {
            init_remove_G2 = false;
        } else if (logic_G2.equalsIgnoreCase("OR")) {
            init_remove_G2 = true;
        }

        // Check for a null search
        boolean nosearch = false;
        boolean g1null = false;
        boolean g2null = false;
        if ( ((keywords_G1.length == 0) || ((keywords_G1.length == 1) && (keywords_G1[0].toString().equals("")) )) ) {
            logic_between = "AND";	// AND it
            g1null = true;
        }
        if ( ((keywords_G2.length == 0) || ((keywords_G2.length == 1) && (keywords_G2[0].toString().equals("")) )) ) {
            logic_between = "AND";	// AND it
            g2null = true;
        }
        if (g1null && g2null) {
            nosearch = true;
        }

        if (! nosearch) {
            // search tree
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Document nDoc;
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
				java.io.InputStream fisXML = mParent.getXMLInputStream( sbError );
				if( fisXML == null ){
					ApplicationController.vShowError( "Failed to access datasets xml file: " + sbError.toString() );
					return SEARCH_ERROR;
				}
                nDoc = builder.parse( fisXML );
            } catch(Exception e) {
				Utility.vUnexpectedError(e, new StringBuffer("creating document builder: "));
				return SEARCH_ERROR;
			}

            AdapterNode newDoc = new AdapterNode(nDoc);
            AdapterNode root = newDoc.getChild(0);

            int provcount = root.childCount();
            int provpos = 0;
            for (int p=0; p < provcount; p++) { 	// provider level
                AdapterNode prov = root.getChild(provpos);
                int dsetcount = prov.childCount();
                int dsetpos = 0;
                for (int d=0; d < dsetcount; d++) { 	// dataset level
                    AdapterNode dset = prov.getChild(dsetpos);
                    if ( dset.isLeaf() ) {
                        boolean remove_G1 = init_remove_G1;
                        for (int k=0; k < keywords_G1.length; k++) {
							String sSearchText = dset.getAttributes().getNamedItem(ATTR_NAME).getNodeValue();
							Matcher matcher = search_G1[k].matcher(sSearchText);
                            if ( matcher.find() ) {
                                if (logic_G1.equalsIgnoreCase("OR")) {
                                    remove_G1=false;
                                }
                            } else {
                                if (logic_G1.equalsIgnoreCase("AND")) {
                                    remove_G1=true;
                                }
                            }
                        }
                        boolean remove_G2 = init_remove_G2;
                        for (int k=0; k < keywords_G2.length; k++) {
							String sSearchText = dset.getAttributes().getNamedItem(ATTR_NAME).getNodeValue();
							Matcher matcher = search_G2[k].matcher(sSearchText);
                            if ( matcher.find() ) {
                                if (logic_G2.equalsIgnoreCase("OR")) {
                                    remove_G2=false;
                                }
                            } else {
                                if (logic_G2.equalsIgnoreCase("AND")) {
                                    remove_G2=true;
                                }
                            }
                        }
                        boolean remove = false;
                        if (logic_between.equalsIgnoreCase("AND")) {
                            remove = remove_G1 || remove_G2;
                        } else if (logic_between.equalsIgnoreCase("OR")) {
                            remove = remove_G1 && remove_G2;
                        }
                        if (remove == true) {
                            prov.removeChild(dsetpos);
                        } else {
                            dsetpos++;
                        }
                    } else {
                        int subdsetcount = dset.childCount();
                        int subdsetpos = 0;
                        for (int s=0; s < subdsetcount; s++) {	// subdataset level
                            AdapterNode subdset = dset.getChild(subdsetpos);
                            boolean remove_G1 = init_remove_G1;
                            for (int k=0; k < keywords_G1.length; k++) {
								String sSearchText = subdset.getAttributes().getNamedItem(ATTR_NAME).getNodeValue();
								Matcher matcher = search_G1[k].matcher(sSearchText);
								if ( matcher.find() ) {
                                    if (logic_G1.equalsIgnoreCase("OR")) {
                                        remove_G1=false;
                                    }
                                } else {
                                    if (logic_G1.equalsIgnoreCase("AND")) {
                                        remove_G1=true;
                                    }
                                }
                            }
                            boolean remove_G2 = init_remove_G2;
                            for (int k=0; k < keywords_G2.length; k++) {
								String sSearchText = subdset.getAttributes().getNamedItem(ATTR_NAME).getNodeValue();
								Matcher matcher = search_G2[k].matcher(sSearchText);
								if ( matcher.find() ) {
                                    if (logic_G2.equalsIgnoreCase("OR")) {
                                        remove_G2=false;
                                    }
                                } else {
                                    if (logic_G2.equalsIgnoreCase("AND")) {
                                        remove_G2=true;
                                    }
                                }
                            }
                            boolean remove = false;
                            if (logic_between.equalsIgnoreCase("AND")) {
                                remove = remove_G1 || remove_G2;
                            } else if (logic_between.equalsIgnoreCase("OR")) {
                                remove = remove_G1 && remove_G2;
                            }
                            if (remove == true) {
                                dset.removeChild(subdsetpos);
                            } else {
                                subdsetpos++;
                            }
                        }
                        if (dset.childCount() == 0) { 	// no subdatasets left under this dataset?
                            prov.removeChild(dsetpos);
                        } else {
                            dsetpos++;
                        }
                    }
                }
                if (prov.childCount() == 0) { 	// no datasets left under this provider?
                    root.removeChild(provpos);
                } else {
                    provpos++;
                }
            }
            if (root.childCount() == 0) {
                return SEARCH_NO_MATCH;
            } else {
                currentDoc = (Document) newDoc.getFirstChild().getParentNode();
                DomToTreeModelAdapter newTree = new DomToTreeModelAdapter(currentDoc);
                setModel(newTree);
                expandRow(1);
                return SEARCH_MATCH_FOUND;
            }
        } else {
            return SEARCH_NULL;
        }
    }

    // An array of names for DOM node-types
    // (Array indexes = nodeType() values.)
    static final String[] typeName = {
        "none",
        "Element",
        "Attr",
        "Text",
        "CDATA",
        "EntityRef",
        "Entity",
        "ProcInstr",
        "Comment",
        "Document",
        "DocType",
        "DocFragment",
        "Notation",
    };

    static final int ELEMENT_TYPE =   1;	// Element
    static final int ATTR_TYPE =      2;
    static final int TEXT_TYPE =      3;
    static final int CDATA_TYPE =     4;
    static final int ENTITYREF_TYPE = 5;
    static final int ENTITY_TYPE =    6;
    static final int PROCINSTR_TYPE = 7;
    static final int COMMENT_TYPE =   8;
    static final int DOCUMENT_TYPE =  9;
    static final int DOCTYPE_TYPE =  10;
    static final int DOCFRAG_TYPE =  11;
    static final int NOTATION_TYPE = 12;



    //
    // Checks to see if we want to display this element
    //
    public boolean treeElement(String elementName) {
        for (int i=0; i<treeElementNames.length; i++) {
            if ( elementName.equals(treeElementNames[i]) ) {
                return true;
            }
        }
        return false;
    }


    //
    // DOM node Adapter
    //
    public class AdapterNode
    {
        private org.w3c.dom.Node domNode;

        // Construct an Adapter node from a DOM node
        public AdapterNode(org.w3c.dom.Node node) {
            domNode = node;
        }

        // Return a string that identifies this node in the tree
        public String toString() {
            String s = "";
            String nodeName = domNode.getNodeName();
            if (! nodeName.startsWith("#")) {
                if (nodeName.equals(ELEMENT_ROOT)) {
                    s += rootTitle;
                } else {
                    // Trim the value to get rid of NL's at the front
                    String t = content().trim();
                    int x = t.indexOf("\n");
                    if (x >= 0) t = t.substring(0, x);
                    s += " " + t;
                }
            } else {
                if (nodeName.startsWith("#document")) {
                    s += listTitle;
                }
            }

            return s;
        }

        public String content() {
            String s = "";
            if (domNode.getAttributes() != null) {
                if (domNode.getAttributes().getLength() > 0) {
                    for (int i=0; i < desiredTreeAttributes.length; i++) {
                        if (domNode.getAttributes().getNamedItem(desiredTreeAttributes[i]) != null) {
                            if (desiredTreeAttributes[i] == ATTR_NAME) {
                                s += domNode.getAttributes().getNamedItem(desiredTreeAttributes[i]).getNodeValue();
                            }
                        }
                    }
                }
            }
            return s;
        }

        public NamedNodeMap getAttributes() {
            return domNode.getAttributes();
        }

        public String getNodeName() {
            return domNode.getNodeName();
        }

        public String getNodeValue() {
            return domNode.getNodeValue();
        }

        public Node getParentNode() {
            return domNode.getParentNode();
        }

        public NodeList getChildNodes() {
            return domNode.getChildNodes();
        }

        public Node getFirstChild() {
            return domNode.getFirstChild();
        }

        public Node removeChild(int searchIndex) {
            //Note: JTree index is zero-based.
            org.w3c.dom.Node node = domNode.getChildNodes().item(searchIndex);
            // Return Nth displayable node
            int elementNodeIndex = 0;
            for (int i=0; i<domNode.getChildNodes().getLength(); i++) {
                node = domNode.getChildNodes().item(i);
                if (node.getNodeType() == ELEMENT_TYPE
                        && treeElement( node.getNodeName() )
                        && elementNodeIndex++ == searchIndex)
                {
                    break;
                }
            }
            return domNode.removeChild(node);
        }


        public boolean isLeaf() {
            if (childCount() > 0) return false;
            return true;
        }

        public int index(AdapterNode child) {
            //System.err.println("Looking for index of " + child);
            int count = childCount();
            for (int i=0; i<count; i++) {
                AdapterNode n = (AdapterNode) this.getChild(i);
                if (child == n) return i;
            }
            return -1; // Should never get here.
        }

        public AdapterNode getChild(int searchIndex) {
            //Note: JTree index is zero-based.
            org.w3c.dom.Node node = domNode.getChildNodes().item(searchIndex);
            // Return Nth displayable node
            int elementNodeIndex = 0;
            for (int i=0; i<domNode.getChildNodes().getLength(); i++) {
                node = domNode.getChildNodes().item(i);
                if (node.getNodeType() == ELEMENT_TYPE
                        && treeElement( node.getNodeName() )
                        && elementNodeIndex++ == searchIndex)
                {
                    break;
                }
            }
            return new AdapterNode(node);
        }

        public int childCount() {
            int count = 0;
            for (int i=0; i<domNode.getChildNodes().getLength(); i++) {
                org.w3c.dom.Node node = domNode.getChildNodes().item(i);
                if (node.getNodeType() == ELEMENT_TYPE
                        && treeElement( node.getNodeName() ))
                {
                    ++count;		// count only the nodes we want
                }
            }
            return count;
        }

    } // AdapterNode



    //
    // This adapter converts the current Document (a DOM) into
    // a JTree model.
    //
    public class DomToTreeModelAdapter implements javax.swing.tree.TreeModel
    {
        private Document domDoc;

        // Constructor
        public DomToTreeModelAdapter(Document doc) {
            domDoc = doc;
        }

        // Basic TreeModel operations
        public Object getRoot() {
            return new AdapterNode(domDoc);
        }
        public boolean isLeaf(Object aNode) {
            // Return true for any node with no children
            AdapterNode node = (AdapterNode) aNode;
            return node.isLeaf();
        }
        public int getChildCount(Object parent) {
            AdapterNode node = (AdapterNode) parent;
            return node.childCount();
        }
        public Object getChild(Object parent, int index) {
            AdapterNode node = (AdapterNode) parent;
            return node.getChild(index);
        }
        public int getIndexOfChild(Object parent, Object child) {
            AdapterNode node = (AdapterNode) parent;
            return node.index((AdapterNode) child);
        }
        public void valueForPathChanged(TreePath path, Object newValue) {
            // Null. We won't be making changes in the GUI
            // If we did, we would ensure the new value was really new
            // and then fire a TreeNodesChanged event.
        }


        private Vector listenerList = new Vector();
        public void addTreeModelListener( TreeModelListener listener ) {
            if ( listener != null && ! listenerList.contains( listener ) ) {
                listenerList.addElement( listener );
            }
        }
        public void removeTreeModelListener( TreeModelListener listener ) {
            if ( listener != null ) {
                listenerList.removeElement( listener );
            }
        }
    } // DomToTreeModelAdapter

}


