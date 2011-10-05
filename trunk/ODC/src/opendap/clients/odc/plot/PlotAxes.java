package opendap.clients.odc.plot;

import java.util.ArrayList;

import opendap.clients.odc.Utility;

public class PlotAxes extends javax.swing.AbstractListModel {
	private PlotAxes(){}
	public static PlotAxes create(){ return new PlotAxes(); }
	boolean mzAutomatic  = true; // automatically generate all axes
	boolean mzAutomaticX = false; // automatically generate an X axis
	boolean mzAutomaticY = false; // automatically generate a Y axis

	// list model interface
	private ArrayList<PlotAxis> mlistAxis = new ArrayList<PlotAxis>(); // contains PlotAxis
	public int getSize(){ return mlistAxis.size(); }
	public Object getElementAt(int index){ return mlistAxis.get(index); }

	public ArrayList<PlotAxis> _getList(){ return mlistAxis; } 
	
	void removeAll(){
		if( getSize() == 0 ) return; // nothing to remove
		int xLastItem = getSize() - 1;
		mlistAxis.clear();
		fireIntervalRemoved(this, 0, xLastItem);
	}
	void remove( int xItemToRemove0 ){
		mlistAxis.remove(xItemToRemove0);
		fireIntervalRemoved(this, xItemToRemove0, xItemToRemove0);
	}
	void remove( String sID_ToRemove ){
		for( int xItem = 0; xItem < mlistAxis.size(); xItem++ ){
			PlotAxis item = (PlotAxis)mlistAxis.get(xItem);
			if( Utility.equalsUPPER( item.getID(), sID_ToRemove ) ){
				mlistAxis.remove(xItem);
				fireIntervalRemoved(this, xItem, xItem);
			}
		}
	}
	PlotAxis get(int index0){
		return (PlotAxis)mlistAxis.get(index0);
	}
	PlotAxis getNew( String sID ){
		for( int xItem = 0; xItem < mlistAxis.size(); xItem++ ){
			PlotAxis item = (PlotAxis)mlistAxis.get(xItem);
			if( Utility.equalsUPPER( item.getID(), sID ) ) return item;
		}
		return newItem( sID );
	}
	private PlotAxis newItem( String sID ){
		PlotAxis item = PlotAxis.create( sID );
		mlistAxis.add(item);
		int xNewItem = getSize()-1;
		fireIntervalAdded(this, xNewItem, xNewItem);
		return item;
	}
	void vFireItemChanged(int index0){
		this.fireContentsChanged(this, index0, index0);
	}
}

