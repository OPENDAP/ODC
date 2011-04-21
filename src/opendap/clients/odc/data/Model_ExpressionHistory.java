package opendap.clients.odc.data;

import java.util.ArrayList;
import javax.swing.MutableComboBoxModel;
import javax.swing.AbstractListModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.Utility;

/** The application controller maintains this object.
 *  This models the one-liner history used in the array viewers combo box only, not scripts in general.
 *  This information is persisted by the file "exp.txt" in the base directory.
 */
public class Model_ExpressionHistory extends AbstractListModel implements MutableComboBoxModel {
	private static int mctSessionExps = 0; // keeps track of total number of one-liners created in this session
	private int miCapacity = 1000;
	private int mctExpressions = 0;
	private int miSelectedItem0 = -1; // nothing selected
	private String[] msExpressions0 = new String[ 1000 ];
	private ArrayList<ListDataListener> listListeners = new ArrayList<ListDataListener>(); 

	private Model_ExpressionHistory(){}
	
	public static Model_ExpressionHistory _create(){
		Model_ExpressionHistory new_model = new Model_ExpressionHistory();
		StringBuffer sbError = new StringBuffer();
		if( ! new_model._zLoadHistory( sbError ) ){
			ApplicationController.vShowError_NoModal( "Expression history failed to load: " );
		}
		return new_model;
	}
	
	public final boolean _contains( String sExpression ){
		for( int xModel = 1; xModel <= mctExpressions; xModel++ ) if( msExpressions0[xModel - 1].equals( sExpression ) ) return true;
		return false;
	}
	
	public final void _reload(){ // called by user to reload the model
		StringBuffer sbError = new StringBuffer( 256 );
		if( _zLoadHistory( sbError ) ){
			ApplicationController.vShowStatus( "Expression history with " + mctExpressions + " lines reloaded." );
		} else {
			ApplicationController.vShowError( "Expression history failed to reload: " );
		}
	}
	
	final boolean _zLoadHistory( StringBuffer sbError ){
		String sPath = ConfigurationManager.getInstance().getProperty_PATH_ExpHistory();
		if( ! Utility.zFileExists( sPath, sbError ) ){
			return true; // it is ok for there to be no file, in that case no history is loaded
		}
		ArrayList<String> listLines = Utility.zLoadLines( sPath, 8000, sbError );
		if( listLines == null ){
			sbError.insert( 0, "error loading lines from " + sPath + ": " );
			return false;
		}
		if( listLines.size() > miCapacity ){
			ApplicationController.vShowWarning( "warning, one-liner history has more than the maximum of " + miCapacity + " lines in it; list will be limited to that size" );
		}
		int xLine = 0;
		for( ; xLine < listLines.size() && xLine < miCapacity; xLine++ ){
			msExpressions0[xLine] = listLines.get( xLine );
		}
		mctExpressions = xLine;
		return true;
	}
	
	public final void _addExpression( String sExpression ){
		if( sExpression == null ){
			ApplicationController.vShowError("(Model_ExpressionHistory) internal error, attempt to add null string");
			return;
		}
		for( int xExpression = 0; xExpression < mctExpressions; xExpression++ ){
			if( sExpression.equals( msExpressions0[xExpression] ) ){
				ApplicationController.vShowWarning("no one-liner added, expression already exists in list at item index " + xExpression );
				return;
			}
		}
		if( mctExpressions == miCapacity ){
			int iNewCapacity = 2 * miCapacity;
			String[] aEnlargedDatasetBuffer = new String[ iNewCapacity ];
			System.arraycopy( msExpressions0, 0, aEnlargedDatasetBuffer, 0, mctExpressions );
			msExpressions0 = aEnlargedDatasetBuffer;
			miCapacity = iNewCapacity;
		}
		msExpressions0[mctExpressions] = sExpression;
		mctExpressions++;
		int interval_index = mctExpressions - 1;
		miSelectedItem0 = interval_index;
		for( ListDataListener listener : listListeners ){
			listener.intervalAdded( new ListDataEvent( this, interval_index, interval_index, ListDataEvent.INTERVAL_ADDED ) );
		}
	}

	public final void _removeExpression( int iIndex0 ){
		if( iIndex0 < 0 || iIndex0 >= mctExpressions ){
			ApplicationController.vShowWarning("attempt to unload non-existent dataset index " + iIndex0);
			return;
		}
		for( int xList = iIndex0; xList < mctExpressions - 1; xList++ ){
			msExpressions0[xList] = msExpressions0[xList + 1];
		}
		mctExpressions--;
		if( iIndex0 <= miSelectedItem0 ){
			miSelectedItem0--; 
		}
		for( ListDataListener listener : listListeners ){
			listener.intervalAdded( new ListDataEvent( this, iIndex0, iIndex0, ListDataEvent.INTERVAL_REMOVED ) );
		}
	}

	public final void _removeExpression( String sExpression ){
		for( int xList = 0; xList < mctExpressions; xList++ ){
			if( msExpressions0[xList] == sExpression ){
				for( ; xList < mctExpressions - 1; xList++ ) msExpressions0[xList] = msExpressions0[xList + 1];
				mctExpressions--;
				if( xList <= miSelectedItem0 ){
					miSelectedItem0--; 
				}
				for( ListDataListener listener : listListeners ){
					listener.intervalAdded( new ListDataEvent( this, xList, xList, ListDataEvent.INTERVAL_REMOVED ) );
				}
				return;
			}
		}
		ApplicationController.vShowWarning( "attempt to remove non-existent expression \"" + sExpression + "\"" );
		return;
	}

	// AbstractListModel
	public Object getElementAt( int iIndex0 ){
		if( iIndex0 < 0 || iIndex0 >= mctExpressions ){
			ApplicationController.vShowWarning("attempt to get non-existent dataset element " + iIndex0);
			return null;
		}
		return msExpressions0[iIndex0];
	}

	// ComboBoxModel Interface
	public Object getSelectedItem(){
		if( miSelectedItem0 >= 0 && miSelectedItem0 < mctExpressions ){
			return msExpressions0[miSelectedItem0];
		} else {
			return null;
		}
	}

	/** returns -1 if nothing is selected */
	public int getSelectedIndex0(){
		return miSelectedItem0;
	}
	
    public int getIndexOf( Object o ){
		for( int xList = 0; xList < mctExpressions; xList++ ){
			if( msExpressions0[xList] == o ){
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
		String sExpressionSelected = msExpressions0[miSelectedItem0];
		if( o instanceof String ){
			if( ( 	sExpressionSelected != null && !sExpressionSelected.equals( o ) ) ||
					sExpressionSelected == null && o != null ){
				for( int xList = 0; xList < mctExpressions; xList++ ){
					if( msExpressions0[xList] == o ){
						miSelectedItem0 = xList;
						fireContentsChanged( this, -1, -1 );
						return;
					}
				}
				ApplicationController.vShowError( "internal error, attempt to set one-liner history selection to unknown expression" );
				return;
	        }
		} else {
			ApplicationController.vShowError( "internal error, attempt to set expression history item to unrecognized object: " + o.getClass().getName() );
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
		return mctExpressions;
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





