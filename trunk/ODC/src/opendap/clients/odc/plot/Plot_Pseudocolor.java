package opendap.clients.odc.plot;

/**
 * Title:        Plot_Pseudocolor
 * Description:  Generates plots of data into pseudocolor images
 * Copyright:    Copyright (c) 2002-12
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.08
 */

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DAP;

class Plot_Pseudocolor extends Plot {

	private ColorSpecification cs = null;
	
	private Plot_Pseudocolor( PlotEnvironment environment, PlotLayout layout ){
		super( environment, layout );
	}

	public String getDescriptor(){ return "P"; }

	public static Plot_Pseudocolor create( PlotEnvironment environment, PlotLayout layout, String sCaption, StringBuffer sbError ){
		Plot_Pseudocolor plot = new Plot_Pseudocolor( environment, layout );
		plot.cs = environment.getColorSpecification();
		plot.msCaption = sCaption;
		return plot;
	}

	public boolean setData( PlottingData pdat, StringBuffer sbError ){
		try {
			PlottingVariable pv = pdat.getVariable_Primary();
			int eDATA_TYPE   = pv.getDataType();
			Object[] eggData = pv.getDataEgg();
			int iWidth       = pv.getDimLength(1);
			int iHeight      = pv.getDimLength(2);
			if( iWidth <= 1 || iHeight <=1 ){
				sbError.append("cannot plot pseudocolor with width " + iWidth + " and height " + iHeight + "; data must be two-dimensional");
				return false;
			}
			PlottableData plottableData = PlottableData.create( eDATA_TYPE, eggData, null, null, null, iWidth, iHeight, sbError );
			if( plottableData == null ){
				sbError.insert(0, "Failed to set pseudocolor data (type " + DAP.getType_String(eDATA_TYPE) + ") with width " + iWidth + " and height " + iHeight + ": ");
				return false;
			}
			data = plottableData;
			
			
			// setup generated cs if necessary
			ColorSpecification cs = environment.getColorSpecification();
			if( cs == null ){
				cs = new ColorSpecification("[system generated]", eDATA_TYPE);
				int iDefaultMissingColor = 0xFF0000FF;
				// cs.setMissing( Panel_View_Plot.getDataParameters().getMissingEgg(), eDATA_TYPE, iDefaultMissingColor );
				cs.setMissing( pv.getMissingEgg(), eDATA_TYPE, iDefaultMissingColor );
				if( !cs.setStandardColors(ColorSpecification.COLOR_STYLE_Default, sbError) ){
					sbError.insert(0, "Failed to create default standard colors: ");
					return false;
				}
			} else {
				if( cs.getDataType() != eDATA_TYPE ){
					sbError.append("Color specification type (" + DAP.getType_String(eDATA_TYPE) + ") does not match data type (" +  DAP.getType_String(eDATA_TYPE) + "); see help topic color specification for more information");
					return false;
				}
			}
			return true;
		} catch(Exception ex) {
			sbError.append("While building pseudocolor plot: ");
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}
	
	public boolean render( int pxPlotWidth, int pxPlotHeight, StringBuffer sbError ){
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



