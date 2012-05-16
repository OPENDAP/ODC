package opendap.clients.odc.plot;

abstract public class Plot {
	protected Plot( PlotEnvironment environment ){ this.environment = environment; }
	protected IPlottable data = null; // needed for data microscope
	protected String msCaption = null; // needed for data microscope
	public PlotEnvironment environment = null;
	public String getCaption(){ return msCaption; }
	abstract public String getDescriptor();
	abstract public boolean render( int[] raster, int pxPlotWidth, int pxPlotHeight, StringBuffer sbError );
}
