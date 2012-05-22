package opendap.clients.odc.plot;

abstract public class Plot {
	protected Plot( PlotEnvironment environment, PlotLayout layout ){
		this.environment = environment;
		this.layout = layout;
	}
	protected IPlottable data = null; // needed for data microscope
	protected String msCaption = null; // needed for data microscope
	public PlotEnvironment environment = null;
	public PlotLayout layout = null;
	public String getCaption(){ return msCaption; }
	abstract public String getDescriptor();
	abstract public int[] render( int pxPlotWidth, int pxPlotHeight, StringBuffer sbError ); // returns the raster
	protected int[] raster = null;
	public int[] getRaster( int iRasterLength ){
		if( raster == null || raster.length != iRasterLength ){
			raster = new int[ iRasterLength ];
		}
		return raster;
	}
}
