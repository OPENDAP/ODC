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
import opendap.clients.odc.data.Model_Dataset;

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
				if( ! cs.setStandardColors(ColorSpecification.COLOR_STYLE_Default, sbError) ){
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

	static boolean zPlot_PseudoColor( PlotEnvironment environment, PlotLayout layout, Model_Dataset model, String sCaption, PlottingVariable pv, Visualizer.OutputTarget eOutputOption, int iFrame, int ctFrames, StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}

	public boolean draw( StringBuffer sbError ){
		try {
			if( model == null ){
				sbError.append( "internal error, no data model" );
				return false;
			}
			final PlotEnvironment.PLOT_TYPE ePlotType = environment.getPlotType();
			ColorSpecification       cs = environment.getColorSpecification();
			final PlotScale          ps = environment.getScale();
			final PlotOptions        po = environment.getOptions();
			if( data == null ){
				sbError.insert(0, "Failed to get dataset: ");
				return false;
			}
			if( ps == null ){
				sbError.insert(0, "No plot scale");
				return false;
			}

			// this is a hack to add extra space in the margin for a legend/colorbar
			// if the defaults are being used
			// if in a future version a more generalized layout system is installed which
			// is capable of automatically adjusting the margins exactly then this should
			// be removed todo
			if( po.option_legend_zShow ){
				PlotLayout.ORIENTATION eORIENT = po.option_legend_Layout.getOrientation();
				if( ps.getMarginRight_px() == PlotScale.PX_DEFAULT_MARGIN
				    && (eORIENT == PlotLayout.ORIENTATION.TopRight ) ){
					ps.setMarginRight(130f);
				}
			}

			int iMultisliceDelay = po.getValue_int(PlotOptions.OPTION_MultisliceDelay);

			if( cs != null && data != null ){
				if( cs.getDataType() != data.getDataType() ){
					ApplicationController.vShowWarning("color specification data type does not match variable 1 data type; cs " + cs.getName() + " ignored");
					cs = null;
				}
			}

			String sCaption = model.getTitle();

//			final VariableInfo varAxisX = data.getAxis_X();
//			final VariableInfo varAxisY = data.getAxis_Y();

			final int ctVariable1 = data.getDataElementCount();
			final int ctVariable2 = ctVariable1;
			final int ctVariable  = (ctVariable1 == 0 ? ctVariable2 : ctVariable1);
			if( ctVariable < 1 ){
				sbError.append("no variables defined for plotting output");
				return false;
			}

			boolean zResult;
//			if( ePlotType == PlotEnvironment.PLOT_TYPE.XY ){ // all variables on same plot
//				return zPlot_XY( environment, layout, model, sCaption, pdat, varAxisX, varAxisY, eOutputOption, sbError);
//			} else { // each variable goes on a different plot
//				for( int xVariable = 1; xVariable <= ctVariable; xVariable++ ){
//					PlottingVariable pv1 = pdat.getVariable1(xVariable);
//					PlottingVariable pv2 = pdat.getVariable2(xVariable);
//					if( ctVariable > 1 && eOutputOption == OutputTarget.Thumbnail ) sCaption = pv1.getSliceCaption();
//					switch(ePlotType){
//						case Histogram:
//							zResult = zPlot_Histogram( environment, layout, model, sCaption, pv1, eOutputOption, xVariable, ctVariable, sbError);
//							break;
//						case Pseudocolor:
//							zResult = zPlot_PseudoColor( environment, layout, model, sCaption, pv1, varAxisX, varAxisY, eOutputOption, xVariable, ctVariable, sbError);
//							break;
//						case Vector:
//							zResult = zPlot_Vector( environment, layout, model, sCaption, pv1, pv2, varAxisX, varAxisY, eOutputOption, xVariable, ctVariable, sbError);
//							break;
//						case XY:
//							sbError.append( "not applicable" );
//							return false;
//						case Surface:
//							sbError.append( "plot type not implemented " + ePlotType );
//							return false;
//						default:
//							sbError.append("unknown plot type " + ePlotType);
//							return false;
//					}
//					if( !zResult ){
//						sbError.insert(0, "Error plotting variable " + xVariable + ": ");
//						return false;
//					}
//				}
//			}
			return true;
		} catch(Exception ex) {
			sbError.append("error creating data sets: ");
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



