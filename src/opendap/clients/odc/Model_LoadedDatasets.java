package opendap.clients.odc;

import java.util.ArrayList;
import javax.swing.ComboBoxModel;
import javax.swing.AbstractListModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;

class Model_LoadedDatasets extends AbstractListModel implements ComboBoxModel {
	private int miCapacity = 1000;
	private int mctDatasets = 0;
	private int miSelectedItem = 0;
	private Model_Dataset[] maDatasets = new Model_Dataset[ 1000 ];
	private ArrayList<ListDataListener> listListeners = new ArrayList<ListDataListener>(); 

	boolean _contains( Model_Dataset model ){
		for( int xModel = 1; xModel <= mctDatasets; xModel++ ) if( maDatasets[xModel] == model ) return true;
		return false;
	}
	
	void addDataset( Model_Dataset url ){
		if( url == null ){
			ApplicationController.vShowWarning("attempt to add null dataset");
			return;
		}
		if( mctDatasets == miCapacity ){
			int iNewCapacity = 2 * miCapacity;
			Model_Dataset[] aEnlargedDatasetBuffer = new Model_Dataset[ iNewCapacity ];
			System.arraycopy(maDatasets, 0, aEnlargedDatasetBuffer, 0, miCapacity);
			maDatasets = aEnlargedDatasetBuffer;
		}
		maDatasets[mctDatasets] = url;
		mctDatasets++;
		int interval_index = mctDatasets - 1;
		miSelectedItem = interval_index;
		for( ListDataListener listener : listListeners ){
			listener.intervalAdded( new ListDataEvent( this, interval_index, interval_index, ListDataEvent.INTERVAL_ADDED ) );
		}
	}

	void removeDataset( int iIndex0 ){
		if( iIndex0 < 0 || iIndex0 >= mctDatasets ){
			ApplicationController.vShowWarning("attempt to unload non-existent dataset index " + iIndex0);
			return;
		}
		for( int xList = iIndex0; xList < mctDatasets - 1; xList++ ){
			maDatasets[xList] = maDatasets[xList + 1];
		}
		mctDatasets--;
		if( iIndex0 <= miSelectedItem ){
			miSelectedItem--; 
		}
		for( ListDataListener listener : listListeners ){
			listener.intervalAdded( new ListDataEvent( this, iIndex0, iIndex0, ListDataEvent.INTERVAL_REMOVED ) );
		}
	}

	void removeDataset( Model_Dataset url ){
		for( int xList = 0; xList < mctDatasets; xList++ ){
			if( maDatasets[xList] == url ){
				for( ; xList < mctDatasets - 1; xList++ ) maDatasets[xList] = maDatasets[xList + 1];
				mctDatasets--;
				if( xList <= miSelectedItem ){
					miSelectedItem--; 
				}
				for( ListDataListener listener : listListeners ){
					listener.intervalAdded( new ListDataEvent( this, xList, xList, ListDataEvent.INTERVAL_REMOVED ) );
				}
				return;
			}
		}
		ApplicationController.vShowWarning("attempt to unload non-existent dataset " + url);
		return;
	}

	// AbstractListModel
	public Object getElementAt( int iIndex0 ){
		if( iIndex0 < 0 || iIndex0 >= mctDatasets ){
			ApplicationController.vShowWarning("attempt to get non-existent dataset element " + iIndex0);
			return null;
		}
		return maDatasets[iIndex0];
	}

	// ComboBoxModel Interface
	public Object getSelectedItem(){
		if( miSelectedItem >= 0 && miSelectedItem < mctDatasets ){
			return maDatasets[miSelectedItem];
		} else return null;
	}
	public void setSelectedItem( Object o ){
		for( int xList = 0; xList < mctDatasets; xList++ ){
			if( maDatasets[xList] == o ){
				miSelectedItem = xList;
				return;
			}
		}
	}
	public void addListDataListener( ListDataListener ldl){
		listListeners.add( ldl );
	}
	public void removeListDataListener( ListDataListener ldl ){
		listListeners.remove( ldl );
	}
	public int getSize(){ 
		return mctDatasets;
	}
}

