package opendap.clients.odc.plot;

import java.awt.image.BufferedImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;

import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.Utility;
import opendap.clients.odc.Utility_String;

public class Composition {
	private Composition(){}

	public enum LayoutStyle {
		SinglePlot { @Override public String toString() { return "Single Plot"; } },
		Row2 { @Override public String toString() { return "2 in a Row"; } },
		Row3 { @Override public String toString() { return "3 in a Row"; } },
		Row4 { @Override public String toString() { return "4 in a Row"; } },
		Column2 { @Override public String toString() { return "2 in a Column"; } },
		Column3 { @Override public String toString() { return "3 in a Column"; } },
		Column4 { @Override public String toString() { return "4 in a Column"; } },
		Array2x2 { @Override public String toString() { return "2x2 Array"; } },
		Array3x3 { @Override public String toString() { return "3x3 Array"; } },
		Array4x4 { @Override public String toString() { return "4x4 Array"; } },
		CustomArray { @Override public String toString() { return "Custom Array"; } },
		MixedArray { @Override public String toString() { return "Mixed Array"; } },
		FreeForm { @Override public String toString() { return "Free Form"; } }
	}
	
	private CompositionLayout layout = null;
	private ArrayList<Plot> listPlots = new ArrayList<Plot>();

	public CompositionLayout getLayout(){ return layout; }
	public ArrayList<Plot> getPlotList(){ return listPlots; }

	private BufferedImage mbi = null;
	BufferedImage getBuffer(){
		Dimension dim = layout.getCompositionDimensions();
		if( mbi == null || mbi.getWidth() != dim.getWidth() || mbi.getHeight() != dim.getHeight() ){
			mbi = new BufferedImage( dim.width, dim.height, BufferedImage.TYPE_INT_ARGB );
		}
		return mbi;
	}
	
	private String msID;
	private String msCaption = null;
	
	public final String _getID(){ return msID; }
	public final String _getCaption(){ return msCaption; }
	public final int getWidth_pixels(){
		return 0; // todo
	}
	public final int getHeight_pixels(){
		return 0; // todo
	}

	private static int miSessionCount = 0;
	final private static String FORMAT_ID_date = "yyyyMMdd";

	public static Composition create(){ return create( (ArrayList<Plot>)null ); }

	public static Composition create( Plot plot ){
		ArrayList<Plot> listPlots = new ArrayList<Plot>();
		if( plot != null ) listPlots.add( plot );
		return create( listPlots );
	}

	public static Composition create( ArrayList<Plot> listPlots ){

		// increment composition serial number
		if( miSessionCount == 0 ){ // update from properties
			if( ConfigurationManager.getInstance() == null ){
				miSessionCount = 0;
			} else {
				miSessionCount = ConfigurationManager.getInstance().getProperty_PlotCount();
			}
		}
		miSessionCount++;
		if( ConfigurationManager.getInstance() != null ) ConfigurationManager.getInstance().setOption(ConfigurationManager.PROPERTY_COUNT_Plots, Integer.toString(miSessionCount));
		int iCountWidth;
		if( miSessionCount < 1000 ) iCountWidth = 3;
		else if( miSessionCount < 100000 ) iCountWidth = 5;
		else iCountWidth = 10;
		
		Composition composition = new Composition();

		if( listPlots != null ) for( Plot plot : listPlots ) composition.listPlots.add( plot ); // copy the plot references to the local list so the source list can be garbage collected
		
		// create composition descriptive identifier
		String sID_descriptive;
		if( listPlots.size() == 0 ){
			sID_descriptive = "plotless_" + Utility.getCurrentDate( FORMAT_ID_date );
		} else if( listPlots.size() == 1 ){
			sID_descriptive = listPlots.get(0).getDescriptor() + Utility.getCurrentDate( FORMAT_ID_date );
		} else {
			sID_descriptive = "multiplot_" + listPlots.get(0).getDescriptor() + Utility.getCurrentDate( FORMAT_ID_date );
		}
		composition.msID = sID_descriptive + "-" + Utility_String.sFormatFixedRight( miSessionCount, iCountWidth, '0' ); // append plot count to ID descriptive string
		
		composition.layout = CompositionLayout.create();
		
		return composition;
	}
	
	// draws a full rendering of annotations and one or more plots
	public boolean _zRender( StringBuffer sbError ){
		BufferedImage bi = getBuffer();
		for( Plot plot : getPlotList() ){
			Rectangle rectRenderingLocation = layout.getElementLayout( plot );
			if( ! plot.zRender( bi, rectRenderingLocation, sbError ) ){
				sbError.append( "error rendering composition element " +  plot.getDescriptor() );
				return false;
			}
		}
		return true;
	}
	
}


