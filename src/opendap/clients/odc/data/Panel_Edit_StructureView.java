package opendap.clients.odc.data;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.gui.LeaflessTreeCellRenderer;
import opendap.dap.BaseType;

// see Panel_Edit_Container for guide
// this class is used directly by the container to show the dataset structure tree
//
//    Panel_Define_Dataset
//       mscrollpane_DataTree contains
//          mtreeData exposes mTreeModel
public class Panel_Edit_StructureView extends JPanel {
	private Panel_View_Data parent;
	private Model_Dataset_Local mTreeModel = null;
	private JScrollPane mscrollpane_DataTree;
	private Panel_Define_Dataset mDefinitionPanel;
	private Dimension dimMinimum = new Dimension(100, 80);
	private JTree mtreeData = null;
	Panel_Edit_StructureView(){} // can only be created with the method below
	public Dimension getMinimumSize(){
		return dimMinimum;
	}
	void _update( BaseType bt ){   // if this base type has changed, update the tree appropriately
		mTreeModel._update( bt );
	}
	void _setModel( Model_Dataset_Local model ){
		mTreeModel = model;
		if( mtreeData != null ) mtreeData.setModel( mTreeModel );
	}
	void _select( Node node ){
		mtreeData.setSelectionPath( node.getTreePath() );
	}
	Node _getSelectedNode(){
		TreePath path = mtreeData.getSelectionPath();
		if( path == null ) return null;
		Object oSelected = path.getLastPathComponent();
		if( oSelected == null ) return null;
		return (Node)oSelected;
	}
	Model_Dataset_Local _getModel(){
		return mTreeModel;
	}
	boolean _zInitialize( Panel_View_Data view, Panel_Define_Dataset definition_panel, StringBuffer sbError ){
		try {
			parent = view;
			mDefinitionPanel = definition_panel;

			Border borderEtched = BorderFactory.createEtchedBorder();

			// tree
			mtreeData = new JTree();
			TreeCellRenderer rendererLeaflessTreeCell = new LeaflessTreeCellRenderer();
			mtreeData.setCellRenderer( rendererLeaflessTreeCell );
			mtreeData.setMinimumSize( new Dimension(60, 60) );
    		mtreeData.setBorder( BorderFactory.createLineBorder( Color.GREEN ) );

			// scroll panes
			mscrollpane_DataTree = new JScrollPane();
			mscrollpane_DataTree.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			mscrollpane_DataTree.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			mscrollpane_DataTree.getViewport().add( mtreeData );

			// set up panel
			setLayout( new BorderLayout() );
			setBorder( BorderFactory.createTitledBorder(borderEtched, "Dataset Structure", TitledBorder.RIGHT, TitledBorder.TOP) );
			removeAll();
    		add( mscrollpane_DataTree, BorderLayout.CENTER );
    		
			// selection listener for directory tree
			mtreeData.addTreeSelectionListener(
				new TreeSelectionListener(){
				    public void valueChanged( TreeSelectionEvent e ){
						TreePath tp = mtreeData.getSelectionPath();
						if( tp == null ) return;
						Object oSelectedNode = tp.getLastPathComponent();
						Node node = (Node)oSelectedNode;
						mDefinitionPanel._showVariable( node );
					}
				}
			);

			return true;

		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}
}
