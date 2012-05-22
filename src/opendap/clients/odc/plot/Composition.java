package opendap.clients.odc.plot;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.Utility;
import opendap.clients.odc.Utility_String;

public class Composition {
	private Composition(){}
	
	private ArrayList<Plot> listPlots = new ArrayList<Plot>();

	public ArrayList<Plot> getPlotList(){ return listPlots; }
	
	private BufferedImage mbi = null;
	BufferedImage getBuffer(){ return mbi; }
	BufferedImage getBuffer( int pxCanvas_width, int pxCanvas_height ){
		if( mbi == null || mbi.getWidth() != pxCanvas_width || mbi.getHeight() != pxCanvas_height ){
			mbi = new BufferedImage( pxCanvas_width, pxCanvas_height, BufferedImage.TYPE_INT_ARGB );
		}
		return mbi;
	}
	
	// legend
	protected int mpxLegend_X = 0;
	protected int mpxLegend_Y = 0;
	protected int mpxLegend_Width = 0;
	protected int mpxLegend_Height = 0;

	// scale
	protected int mpxScale_X = 0;
	protected int mpxScale_Y = 0;
	protected int mpxScale_Width = 0;
	protected int mpxScale_Height = 0;
	
	// layout
	protected PlotLayout layout = PlotLayout.create( PlotLayout.LayoutStyle.PlotArea );
	protected int mpxMargin_Right = 0;
	protected int mpxMargin_Left = 0;
	protected int mpxMargin_Top = 0;
	protected int mpxMargin_Bottom = 0;

	protected ColorSpecification mColors = null;
	protected GeoReference mGeoReference = null;   // these two items should be combined
	protected Model_Projection mProjection = null;
	
	private String msID;
	private String msCaption = null;
	
	public final String _getID(){ return msID; }
	public final String _getCaption(){ return msCaption; }

	private static int miSessionCount = 0;
	final private static String FORMAT_ID_date = "yyyyMMdd";

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
		return composition;
	}

}
