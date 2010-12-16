package opendap.clients.odc.data;

import java.util.ArrayList;
import javax.swing.MutableComboBoxModel;
import javax.swing.AbstractListModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;

import opendap.clients.odc.ApplicationController;

/** The application controller maintains this object */

public class Model_LoadedDatasets extends AbstractListModel implements MutableComboBoxModel {
	private static int mctSessionDatasets = 0; // keeps track of total number of datasets created in this session
	private int miCapacity = 1000;
	private int mctDatasets = 0;
	private int miSelectedItem = 0;
	private Model_Dataset[] maDatasets = new Model_Dataset[ 1000 ];
	private ArrayList<ListDataListener> listListeners = new ArrayList<ListDataListener>(); 

	private Model_LoadedDatasets(){}
	
	public static Model_LoadedDatasets create(){
		Model_LoadedDatasets new_model = new Model_LoadedDatasets();
		return new_model;
	}
	
	public static int getSessionDatasetCount(){ return mctSessionDatasets; }
	
	boolean _contains( Model_Dataset model ){
		for( int xModel = 1; xModel <= mctDatasets; xModel++ ) if( maDatasets[xModel].equals( model ) ) return true;
		return false;
	}
	
	boolean _setName( String sNewName, StringBuffer sbError ){
		return _setName( miSelectedItem, sNewName, sbError );
	}
	
	boolean _setName( int xItem0, String sNewName, StringBuffer sbError ){
		if( xItem0 < 0 || xItem0 >= mctDatasets ){
			sbError.append( "internal error, invalid item number (" + xItem0 + ")" );
			return false;
		}
		maDatasets[xItem0].setTitle( sNewName );
		fireContentsChanged( this, xItem0, xItem0 );
		return true;
	}

	void addDataset( Model_Dataset url ){
		if( url == null ){
			ApplicationController.vShowError("internal error, attempt to add null dataset");
			return;
		}
		for( int xDataset = 0; xDataset < mctDatasets; xDataset++ ){
			if( url.equals( maDatasets[xDataset] ) ){
				ApplicationController.vShowError("internal error, attempt to add duplicate dataset");
				return;
			}
		}
		if( mctDatasets == miCapacity ){
			int iNewCapacity = 2 * miCapacity;
			Model_Dataset[] aEnlargedDatasetBuffer = new Model_Dataset[ iNewCapacity ];
			System.arraycopy( maDatasets, 0, aEnlargedDatasetBuffer, 0, mctDatasets );
			maDatasets = aEnlargedDatasetBuffer;
			miCapacity = iNewCapacity;
		}
		maDatasets[mctDatasets] = url;
		mctDatasets++;
		mctSessionDatasets--;
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
		} else {
			return null;
		}
	}

    public int getIndexOf( Object o ){
		for( int xList = 0; xList < mctDatasets; xList++ ){
			if( maDatasets[xList] == o ){
				return xList;
			}
		}
        return -1;
    }
	
	// used to programmatically change the selection of the item in the combo box
	public void setSelectedItem( Object o ){
		if( o == null ){
			ApplicationController.vShowError( "internal error, attempt to set dataset list to null" );
			return;
		}
		Model_Dataset modelSelected = maDatasets[miSelectedItem];
		if( o instanceof Model_Dataset ){
			if( ( 	modelSelected != null && !modelSelected.equals( o ) ) ||
					modelSelected == null && o != null ){
				for( int xList = 0; xList < mctDatasets; xList++ ){
					if( maDatasets[xList] == o ){
						miSelectedItem = xList;
						fireContentsChanged( this, -1, -1 );
						return;
					}
				}
				ApplicationController.vShowError( "internal error, attempt to set dataset list to unknown model" );
				return;
	        }
		} else if( o instanceof String ){ // this will happen if there is a name change, the editor tries to set the model to the new name
			modelSelected.setTitle( (String)o ); 
		} else {
			ApplicationController.vShowError( "internal error, attempt to set dataset list to unrecognized object: " + o.getClass().getName() );
			return;
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
	
    // javax.swing.MutableComboBoxModel interface
    public void addElement( Object anObject ){
    	System.out.println( "addElement" );
    }

    public void insertElementAt(Object anObject,int index) {
    	System.out.println( "insertElementAt" );
    }

    // implements javax.swing.MutableComboBoxModel
    public void removeElementAt(int index) {
    	System.out.println( "removeElementAt" );
    }

    // implements javax.swing.MutableComboBoxModel
    public void removeElement(Object anObject) {
    	System.out.println( "removeElement" );
    }
	
	
}

