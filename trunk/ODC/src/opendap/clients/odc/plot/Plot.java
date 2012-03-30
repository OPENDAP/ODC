package opendap.clients.odc.plot;

abstract public class Plot {
	protected IPlottable data = null; // needed for data microscope
	protected String msCaption = null; // needed for data microscope
	public String getCaption(){ return msCaption; }
	abstract public String getDescriptor();
	abstract public boolean render( int[] raster, int pxPlotWidth, int pxPlotHeight, StringBuffer sbError );
}
