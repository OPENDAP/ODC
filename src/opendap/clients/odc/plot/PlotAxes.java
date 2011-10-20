package opendap.clients.odc.plot;

import java.util.ArrayList;

import opendap.clients.odc.Utility;

public class PlotAxes extends javax.swing.AbstractListModel {
	private PlotAxis axisDefaultX;
	private PlotAxis axisDefaultY;
	private PlotAxes(){}
	public static PlotAxes create(){
		PlotAxes axes = new PlotAxes();
		axes.axisDefaultX = PlotAxis.createLinear_X( "X", 0, 1 );
		axes.axisDefaultY = PlotAxis.createLinear_Y( "Y", 0, 1 );
		axes._newItem( axes.axisDefaultX );
		axes._newItem( axes.axisDefaultY );
		return axes;
	}
	boolean mzAutomatic  = true; // automatically generate all axes
	boolean mzAutomaticX = false; // automatically generate an X axis
	boolean mzAutomaticY = false; // automatically generate a Y axis
	
	PlotAxis _getDefaultX(){ return axisDefaultX; } 
	PlotAxis _getDefaultY(){ return axisDefaultY; }
	ArrayList<PlotAxis> _getActive(){
		ArrayList<PlotAxis> listActive = new ArrayList();
		for( PlotAxis axis : mlistAxis ){
			if( axis.getActive() ) listActive.add( axis );
		}
		return listActive;
	}

	PlotAxis _getNew( String sID ){
		for( int xItem = 0; xItem < mlistAxis.size(); xItem++ ){
			PlotAxis item = (PlotAxis)mlistAxis.get(xItem);
			if( Utility.equalsUPPER( item.getID(), sID ) ) return item;
		}
		return _newItem( sID );
	}
	private PlotAxis _newItem( String sID ){
		PlotAxis item = PlotAxis.create( sID );
		return _newItem( item );
	}
	private PlotAxis _newItem( PlotAxis item ){
		mlistAxis.add(item);
		int xNewItem = getSize()-1;
		fireIntervalAdded(this, xNewItem, xNewItem);
		return item;
	}
	
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
	void vFireItemChanged(int index0){
		this.fireContentsChanged(this, index0, index0);
	}
}

