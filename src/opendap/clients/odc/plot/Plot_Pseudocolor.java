package opendap.clients.odc.plot;

/**
 * Title:        Plot_Pseudocolor
 * Description:  Generates plots of data into pseudocolor images
 * Copyright:    Copyright (c) 2002-12
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.08
 */

import opendap.clients.odc.DAP;

class Plot_Pseudocolor extends Plot {

	private ColorSpecification cs = null;
	
	private Plot_Pseudocolor(){}

	public String getDescriptor(){ return "P"; }

	public static Plot_Pseudocolor create( PlotEnvironment environment, IPlottable data, String sCaption, StringBuffer sbError ){
		Plot_Pseudocolor plot = new Plot_Pseudocolor();
		plot.data = data;
		plot.cs = environment.getColorSpecification();
		plot.msCaption = sCaption;
		return plot;
	}

	public boolean render( int[] raster, int pxPlotWidth, int pxPlotHeight, StringBuffer sbError ){
		int iDataDim_Width = data.getDimension_x();
		int iDataDim_Height = data.getDimension_y();
		boolean zAveraged = false; // TODO
		switch( data.getDataType() ){
			case DAP.DATA_TYPE_Float32:
				cs.render( raster, data.getFloatArray(), iDataDim_Width, iDataDim_Height, pxPlotWidth, pxPlotHeight, zAveraged );
				break;
			case DAP.DATA_TYPE_Float64:
				cs.render( raster, data.getDoubleArray(), iDataDim_Width, iDataDim_Height, pxPlotWidth, pxPlotHeight, zAveraged );
				break;
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				cs.render( raster, data.getShortArray(), iDataDim_Width, iDataDim_Height, pxPlotWidth, pxPlotHeight, zAveraged );
				break;
			case DAP.DATA_TYPE_UInt16:
			case DAP.DATA_TYPE_Int32:
				cs.render( raster, data.getIntArray(), iDataDim_Width, iDataDim_Height, pxPlotWidth, pxPlotHeight, zAveraged );
				break;
			case DAP.DATA_TYPE_UInt32:
				cs.render( raster, data.getLongArray(), iDataDim_Width, iDataDim_Height, pxPlotWidth, pxPlotHeight, zAveraged );
				break;
		}
		return true;
	}
	
}



