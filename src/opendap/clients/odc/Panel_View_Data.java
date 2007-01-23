package opendap.clients.odc;

import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JSplitPane;
import javax.swing.JButton;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

public class Panel_View_Data extends JPanel {
    public Panel_View_Data() {}
	public boolean zInitialize( StringBuffer sbError ){
		try {

			Model_DataView model = new Model_DataView();
			if( ! model.zInitialize(sbError) ){
				sbError.insert(0, "failed to initialize model: ");
				return false;
			}

			Panel_LoadedDatasets panelLoadedDatasets = new Panel_LoadedDatasets();
			Panel_StructureView panelStructureView = new Panel_StructureView();
			Panel_VarView panelVarView = new Panel_VarView();
			JPanel panelTop = new JPanel();
			panelTop.setLayout( new BorderLayout() );
			panelTop.add( panelLoadedDatasets, BorderLayout.NORTH );
			panelTop.add( panelStructureView, BorderLayout.CENTER );
			JSplitPane jsplitTreeVar = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
			jsplitTreeVar.setTopComponent( panelTop );
			jsplitTreeVar.setBottomComponent( panelVarView );
		} catch( Exception ex ) {
			return false;
		}
		return true;
	}
}

class Model_LoadedDatasets implements ListModel {
	private int miCapacity = 1000;
	private int mctDatasets = 0;
	private DodsURL[] maDatasets = new DodsURL[ 1000 ];

	void addDataset( DodsURL url ){
		if( url == null ){
			ApplicationController.getInstance().vShowWarning("attempt to null dataset");
			return;
		}
		if( mctDatasets == miCapacity ){
			int iNewCapacity = 2 * miCapacity;
			DodsURL[] aEnlargedDatasetBuffer = new DodsURL[ iNewCapacity ];
			System.arraycopy(maDatasets, 0, aEnlargedDatasetBuffer, 0, miCapacity);
			maDatasets = aEnlargedDatasetBuffer;
		}
		mctDatasets++;
		maDatasets[mctDatasets] = url;
	}

	void removeDataset( int iIndex0 ){
		if( iIndex0 < 0 || iIndex0 >= mctDatasets ){
			ApplicationController.getInstance().vShowWarning("attempt to unload non-existent dataset " + iIndex0);
			return;
		}
		for( int xList = iIndex0; xList < mctDatasets - 1; xList++ ){
			maDatasets[xList] = maDatasets[xList + 1];
		}
		mctDatasets--;
		// TODO xxx make event
	}

	void removeDataset( DodsURL url ){
		for( int xList = 0; xList < mctDatasets; xList++ ){
			if( maDatasets[xList] == url ){
				for( ; xList < mctDatasets - 1; xList++ ) maDatasets[xList] = maDatasets[xList + 1];
				mctDatasets--;
				// TODO xxx make event
			}
		}
		ApplicationController.getInstance().vShowWarning("attempt to unload non-existent dataset " + url);
		return;
	}

	// ListModel Interface
	public Object getElementAt( int iIndex0 ){
		if( iIndex0 < 0 || iIndex0 >= mctDatasets ){
			ApplicationController.getInstance().vShowWarning("attempt to get non-existent dataset element " + iIndex0);
			return null;
		}
		return maDatasets[iIndex0];
	}
	public void addListDataListener( ListDataListener ldl){
	}
	public void removeListDataListener( ListDataListener ldl ){
	}
	public int getSize(){ return mctDatasets; }
}

class Model_DataView {
	Model_LoadedDatasets mDatasetList = null;
	boolean zInitialize( StringBuffer sbError ){
		// Model_LoadedDatasets data_list -- get this from application controller
/* NOT FUNCTIONAL
		if( data_list == null ){
			sbError.append("dataset list not supplied");
			return false;
		}
		mDatasetList = data_list;
*/
	  sbError.append("not implemented");
		return false;
	}
	void action_New(){
	}
	void action_Load(){
	}
	void action_Unload(){
	}
	void action_Save(){
	}
	void action_SaveAs(){
	}
}

class Panel_LoadedDatasets extends JPanel {
	boolean zInitialize( final Model_DataView model, StringBuffer sbError ){

		if( model == null ){
			sbError.append("no model supplied");
			return false;
		}

		// create controls
		JComboBox jcbLoadedVariables = new JComboBox();
		JButton buttonNew = new JButton( "New" );
		JButton buttonLoad = new JButton( "Load" );
		JButton buttonUnload = new JButton( "Unload" );
		JButton buttonSave = new JButton( "Save" );
		JButton buttonSaveAs = new JButton( "Save as..." );

		// layout controls
		this.setLayout( new BoxLayout(this, BoxLayout.X_AXIS) );
		this.add( jcbLoadedVariables );
		this.add( buttonNew );
		this.add( buttonLoad );
		this.add( buttonUnload );
		this.add( buttonSave );
		this.add( buttonSaveAs );

		buttonNew.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed( ActionEvent event) {
					model.action_New();
				}
			}
		);
		buttonLoad.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed(ActionEvent event) {
					model.action_Load();
				}
			}
		);
		buttonUnload.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed(ActionEvent event) {
					model.action_Unload();
				}
			}
		);
		buttonSave.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed(ActionEvent event) {
					model.action_Save();
				}
			}
		);
		buttonSaveAs.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed(ActionEvent event) {
					model.action_SaveAs();
				}
			}
		);

		return true;
	}
}

class Panel_StructureView extends JPanel {
	// create tree
	// create scroll pane
	// add tree to scroll pane
	// hook up events to model
}

class Panel_VarView extends JPanel {
	// depending on mode/selected variable type
	// draw on canvas
	// on click/double click events
	// right click to resize stuff
	// be able to record and store presentation info in class below
}

// stores information about how the dataset should be viewed (column widths etc)
class Model_DatasetView {
}

class Model_VariableView {
}

