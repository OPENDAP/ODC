package opendap.clients.odc.plot;

import java.util.ArrayList;

import org.python.core.PyObject;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Utility;
import opendap.clients.odc.Utility_Array;
import opendap.clients.odc.Utility_String;
import opendap.clients.odc.data.Script;


/** Models a plottable expression such as "y=x^2"
 *  Created from a plottable script. See "Plotting Expressions" in the help text.
 *  See opendap.clients.odc.Interpreter.generatePlottableExpression() for generation code.
 *  
 *  When a plottable expression is executed, the "processed expression", the whole blob is exec'ed once at the beginning
 *  Then, depending on the plot type, only the dependent variable(s), is/are evaluated with each pass through the 
 *  independent variables.
 */

public class Model_PlottableExpression {
	public final static double DEFAULT_Theta_begin = 0;
	public final static double DEFAULT_Theta_end = 2 * Math.PI;
	public CompiledExpression compiled_x;
	public CompiledExpression compiled_y;
	public CompiledExpression compiled_z;
	public CompiledExpression compiled_r;
	public CompiledExpression compiled_v;
	private PlottableExpression_TYPE type;
	private Model_PlottableExpression(){}
	private String msProcessedExpression = null;
	String getProcessedExpression(){ return msProcessedExpression; }
	PlotRange range = null;
	PlottableExpression_TYPE getType(){ return type; } 
	public final static Model_PlottableExpression create( Script script, StringBuffer sbError ){
		Model_PlottableExpression model = new Model_PlottableExpression();
		String sScriptText = script.getText();
		String sScriptText_escaped = Utility_String.sReplaceString( sScriptText, "$$", "$" );
		ArrayList<Identifier> listIdentifiers = gatherIdentifiers( sScriptText_escaped, sbError );
		String[] asDollarTokens = { "$x", "$y", "$z", "$r", "$t", "$theta", "$value" };
		String[] asKnownParameters = {
				"$x", "$y", "$z", "$r", "$t", "$theta", "$value",
				"x", "y", "z", "r", "t", "theta", "value",
				"x_range_begin", "x_range_end", "x_increment",
				"y_range_begin", "y_range_end", "y_increment",
				"r_range_begin", "r_range_end", "r_increment",
				"t_range_begin", "t_range_end", "t_increment",
				"theta_range_begin", "theta_range_end", "theta_increment"
				}; 
		
		// determine if $ tokens are being used
		boolean zUsing$Tokens = false;
		DollarCheck:
		for( Identifier identifier : listIdentifiers ){
			for( String sToken : asDollarTokens ){
				if( identifier.s.equalsIgnoreCase( sToken ) ){
					zUsing$Tokens = true;
					break DollarCheck;
				}
			}
		}

//System.out.println( "Identifier count: " + listIdentifiers.size() );
//for( int x = 0; x < listIdentifiers.size(); x++ ){
//	Identifier i = listIdentifiers.get( x );
//	System.out.println( i.s );
//}
//System.out.println( "dollar tokens: " + zUsing$Tokens );

		// determine type of expression
		if( zUsing$Tokens ){
			if( Identifier.contains( listIdentifiers, "$value" ) ){
				model.type = PlottableExpression_TYPE.Pseudocolor;
			} else if( Identifier.contains( listIdentifiers, "$x" ) && Identifier.contains( listIdentifiers, "$y" ) ){
				model.type = PlottableExpression_TYPE.Cartesian;
			} else if( Identifier.contains( listIdentifiers, "$r" ) && Identifier.contains( listIdentifiers, "$theta" ) ){
				model.type = PlottableExpression_TYPE.Polar;
			} else if( Identifier.contains( listIdentifiers, "$x" ) && Identifier.contains( listIdentifiers, "$t" ) ){
				model.type = PlottableExpression_TYPE.Parametric;
			} else {
				sbError.append( "insufficient variables to make plottable expression (must have x/y or r/theta or x/t)" );
				return null;
			}
		} else {
			if( Identifier.contains( listIdentifiers, "value" ) ){
				model.type = PlottableExpression_TYPE.Pseudocolor;
			} else if( Identifier.contains( listIdentifiers, "x" ) && Identifier.contains( listIdentifiers, "y" ) ){
				model.type = PlottableExpression_TYPE.Cartesian;
			} else if( Identifier.contains( listIdentifiers, "r" ) && Identifier.contains( listIdentifiers, "theta" ) ){
				model.type = PlottableExpression_TYPE.Polar;
			} else if( Identifier.contains( listIdentifiers, "x" ) && Identifier.contains( listIdentifiers, "t" ) ){
				model.type = PlottableExpression_TYPE.Parametric;
			} else {
				sbError.append( "insufficient variables to make plottable expression (must have x/y or r/theta or x/t)" );
				return null;
			}
		}
		
		// convert known identifiers
		StringBuffer sbScript = new StringBuffer( sScriptText_escaped );
		for( int xIdentifier = listIdentifiers.size() - 1; xIdentifier >= 0; xIdentifier-- ){
			Identifier identifier = listIdentifiers.get( xIdentifier );
			if( zUsing$Tokens && ! identifier.s.startsWith( "$" ) ) continue; // ignore identifiers that do not start with $ if using $'s
			if( Utility_Array.arrayHasMember( asKnownParameters, identifier.s ) ){
				if( identifier.s.startsWith( "$" ) ){
					sbScript.replace( identifier.pos, identifier.pos, "_" );
				} else {
					sbScript.insert( identifier.pos, "_" );
				}
			}
		}

		// gather and exec lines, set compiled expressions
		opendap.clients.odc.Interpreter odc_interpreter = ApplicationController.getInstance().getInterpreter();
		ArrayList<String> listLines = Utility.zLoadLines( sbScript, sbError);
		for( int xLine = 0; xLine < listLines.size(); xLine++ ){
			String sLine = listLines.get( xLine );
			sLine = sLine.trim();
			listLines.set( xLine, sLine );
			int posEqualsSign = sLine.indexOf( '=' );
			if( posEqualsSign > 0 ){
				String sVariable = sLine.substring( 0, posEqualsSign ).trim();
				String sRValue = sLine.substring( posEqualsSign + 1 ).trim();
				if( sVariable.equals( "_value") ){
					org.python.core.PyCode code = odc_interpreter.zCompile( sRValue, sbError );
					if( code == null  ){
						sbError.insert( 0, "failed to compile _value expression [" + sRValue + "]: ");
						return null;
					}
					model.compiled_v = CompiledExpression.create( sVariable, sLine, code );
				} else if( sVariable.equals( "_x") ){
					org.python.core.PyCode code = odc_interpreter.zCompile( sRValue, sbError );
					if( code == null  ){
						sbError.insert( 0, "failed to compile _x expression [" + sRValue + "]: ");
						return null;
					}
					model.compiled_x = CompiledExpression.create( sVariable, sLine, code );
				} else if( sVariable.equals( "_y") ){
					org.python.core.PyCode code = odc_interpreter.zCompile( sRValue, sbError );
					if( code == null  ){
						sbError.insert( 0, "failed to compile _y expression [" + sRValue + "]: " );
						return null;
					}
					model.compiled_y = CompiledExpression.create( sVariable, sLine, code );
				} else if( sVariable.equals( "_z") ){
					org.python.core.PyCode code = odc_interpreter.zCompile( sRValue, sbError );
					if( code == null  ){
						sbError.insert( 0, "failed to compile _z expression [" + sRValue + "]: " );
						return null;
					}
					model.compiled_z = CompiledExpression.create( sVariable, sLine, code );
				} else if( sVariable.equals( "_r") ){
					org.python.core.PyCode code = odc_interpreter.zCompile( sRValue, sbError );
					if( code == null  ){
						sbError.insert( 0, "failed to compile _r expression [" + sRValue + "]: ");
						return null;
					}
					model.compiled_r = CompiledExpression.create( sVariable, sLine, code );
				} else { // execute other assignments once
					if( ! odc_interpreter.zExecute( sLine, sbError ) ){
						sbError.insert( 0, "error executing assignment on line " + (xLine+1) + " (" + sLine + "): " );
						return null;
					}
				}
			} else { // execute non-assignments
				if( ! odc_interpreter.zExecute( sLine, sbError ) ){
					sbError.insert( 0, "error executing line " + (xLine+1) + " (" + sLine + "): " );
					return null;
				}
			}
		}
		
		return model;
	}
	final private static ArrayList<Identifier> gatherIdentifiers( String sScriptText, StringBuffer sbError ){
		ArrayList<Identifier> listIdentifiers = new ArrayList<Identifier>();
		int pos = 0;
		int ctChars = sScriptText.length();
		int state = 0;
		int posIdentifier = -1;
		StringBuffer sbBuffer = new StringBuffer();
		while( true ){
			if( pos == ctChars ){
				if( state == 1 ){
					String sIdentifier = sbBuffer.toString();
					listIdentifiers.add( Identifier.create( sIdentifier, posIdentifier ) );
				}
				break;
			}
			char c = sScriptText.charAt( pos );
			switch( state ){
				case 0: // start
					if( Character.isWhitespace( c ) ){
						// ignore
					} else if( c == '_' || Character.isLetter( c ) ||  c == '$' ){
						state = 1; // in identifier
						posIdentifier = pos;
						sbBuffer.append( c );
					} else if( Character.isDigit( c ) ){
						state = 2; // number
					} else {
						state = 3; // punctuation
					}
					break;
				case 1: // in identifier
					if( c == '_' || Character.isLetter( c ) ||  Character.isDigit( c ) ){
						sbBuffer.append( c );
					} else {
						String sIdentifier = sbBuffer.toString();
						listIdentifiers.add( Identifier.create( sIdentifier, posIdentifier ) );
						sbBuffer.setLength( 0 );
						posIdentifier = -1;
						state = 0;
					}
					break;
				case 2: // in number
					if( Character.isDigit( c ) || c == '_' || Character.isLetter( c ) ){
						// treat as part of the number
					} else {
						state = 0;
					}
					break;
			}
			pos++;
		}
		return listIdentifiers;
	}
	
//	"x_range_begin", "x_range_end"
//	"y_range_begin", "y_range_end"
//	"r_range_begin", "r_range_end"
//	"t_range_begin", "t_range_end"
	// TODO use an integer mode if inputs are all integers
	
	public PlotRange getPlotRange( StringBuffer sbError ){
		if( range != null ) return range;
		opendap.clients.odc.Interpreter odc_interpreter = ApplicationController.getInstance().getInterpreter();
		PlotRange range = new PlotRange();
		switch( type ){
			case Cartesian:
				double dBeginX = odc_interpreter.get( "_x_range_begin", sbError );
				if( Double.isInfinite( dBeginX ) ){
					sbError.insert( 0, "error getting _x_range_begin: " );
					return null;
				}
				if( Double.isNaN( dBeginX ) ){ // not defined, use default
					dBeginX = 0;
				}
				double dEndX = odc_interpreter.get( "_x_range_end", sbError );
				if( Double.isInfinite( dEndX ) ){
					sbError.insert( 0, "error getting _x_range_end: " );
					return null;
				}
				if( Double.isNaN( dEndX ) ){ // not defined, use default
					dEndX = 10;
				}
				double dRangeX = dEndX - dBeginX;
				double dBeginY = odc_interpreter.get( "_y_range_begin", sbError );
				if( Double.isInfinite( dBeginY ) ){
					sbError.insert( 0, "error getting _y_range_begin: " );
					return null;
				}
				double dEndY = odc_interpreter.get( "_y_range_end", sbError );
				if( Double.isInfinite( dEndY ) ){
					sbError.insert( 0, "error getting _y_range_end: " );
					return null;
				}
				int eState = 0;
				boolean zBeginY_Defined = ! Double.isNaN( dBeginY ); 
				boolean zEndY_Defined = ! Double.isNaN( dEndY );
				range.dBeginX = dBeginX;
				range.dEndX = dEndX;
				double dRangeY = 0;
				if( zBeginY_Defined ){
					if( zEndY_Defined ){ // Y range, beginning and end are defined
						range.dBeginY = dBeginY;
						range.dEndY = dEndY;
						return range;
					} else { // "Determining Y Range, beginning is known, end is unknown";
						eState = 3; // "Determining Y Range, beginning is known, end is unknown";
						dEndY = Double.MIN_VALUE;
					}
				} else {
					if( Double.isNaN( dEndY ) ){
						eState = 1; // "Determining Y Range, no constraints";
						dBeginY = Double.MAX_VALUE;
						dEndY = Double.MIN_VALUE;
					} else {
						dBeginY = Double.MAX_VALUE;
						eState = 2; // "Determining Y Range, beginning is unknown, end is known";
					}
				}
				int iNominalPlotWidth = 1000;
				org.python.core.PyCode codeY = compiled_y.compiled_code;
				for( int x = 1; x <= iNominalPlotWidth; x++ ){ // there will be at least one point for every x pixel
					double dX = dBeginX + x * dRangeX / iNominalPlotWidth;
					if( ! odc_interpreter.zSet( "_x", dX, sbError ) ){
						sbError.insert( 0, "failed to set _x to " + dX );
						return null;
					}
					PyObject pyobjectY = odc_interpreter.zEval( codeY, sbError );
					if( pyobjectY == null ){
						sbError.insert( 0, "failed to eval _y expression " +  compiled_y + ": " );
						return null;
					}
					double dY = pyobjectY.asDouble();
					switch( eState ){
						case 1: // "Determining Y Range, no constraints"
							if( dY < dBeginY ) dBeginY = dY;
							if( dY > dEndY ) dEndY = dY;
							break;
						case 2: // "Determining Y Range, beginning is unknown"
							if( dY < dBeginY ) dBeginY = dY;
							break;
						case 3: // "Determining Y Range, end is unknown"
							if( dY > dEndY ) dEndY = dY;
							break;
					}
				}
				range.dBeginY = dBeginY;
				range.dEndY = dEndY;
				break;
			case Polar:
				double dBeginTheta = odc_interpreter.get( "_theta_range_begin", sbError );
				if( Double.isInfinite( dBeginTheta ) ){
					dBeginTheta = DEFAULT_Theta_begin;
				}
				double dEndTheta = odc_interpreter.get( "_theta_range_end", sbError );
				if( Double.isInfinite( dEndTheta ) ){
					dEndTheta = DEFAULT_Theta_end;
				}
				range.dBeginTheta = dBeginTheta;
				range.dEndTheta = dEndTheta;
				double dRangeTheta = dEndTheta - dBeginTheta;  
				int iNominalAngularResolution = 1000;
				org.python.core.PyCode codeR = compiled_r.compiled_code;
				for( int x = 1; x <= iNominalAngularResolution; x++ ){ // there will be at least one point for every x pixel
					double dTheta = dBeginTheta + x * dRangeTheta / iNominalAngularResolution;
					if( ! odc_interpreter.zSet( "_theta", dTheta, sbError ) ){
						sbError.insert( 0, "failed to set _theta to " + dTheta );
						return null;
					}
					PyObject pyobjectR = odc_interpreter.zEval( codeR, sbError );
					if( pyobjectR == null ){
						sbError.insert( 0, "failed to eval _r expression " +  compiled_r + ": " );
						return null;
					}
					double dR = pyobjectR.asDouble();
					if( dR > range.dEndR ) range.dEndR = dR; 
				}
				break;
			case Parametric:
				// TODO
		}
		return range;
	}
	
	public PlotCoordinates getPlotCoordinates( int pxPlotWidth, int pxPlotHeight, StringBuffer sbError ){
		if( range == null ){
			range = getPlotRange( sbError );
			if( range == null ) return null;
		}
		PlotCoordinates coordinates = new PlotCoordinates();
		coordinates.maiPlotBuffer = new int[pxPlotWidth][pxPlotHeight];
		opendap.clients.odc.Interpreter interpreter = ApplicationController.getInstance().getInterpreter();
		double dRangeX = range.dEndX - range.dBeginX;
		double dRangeY = range.dEndY - range.dBeginY;
		double dMax_x = pxPlotWidth - 1;  // if the plot is 100 pixels wide then x goes from 0 to 99;
		double dMax_y = pxPlotHeight - 1;
		boolean zWriting = false;
		switch( type ){
			case Pseudocolor: { // encapsulating braces needed to protect like variables dX, dY, etc.
				org.python.core.PyCode codeV = compiled_v.compiled_code;
				zWriting = false;
				int ctPoints = pxPlotWidth * pxPlotHeight;
				long nMemoryRequired = ctPoints * 8;
				coordinates.zIsFillPlot = true;
				if( Utility.zMemoryCheck( nMemoryRequired ) ){
					coordinates.madPlotBuffer = new double[ctPoints];
				} else {
					sbError.append( "unable to make pseudocolor/fill plot, insufficient memory; requires " + nMemoryRequired + " bytes; check help for increasing memory" );
					return null;
				}
				// coordinates.setCapacity( ctPoints ); // capacity is only set when plotting x-y pairs, not fill plots
				double dX = 0;
				double dY = 0;
				double dV = 0;
				int xPlotBufferIndex = 0;
				double dMissing = Double.NaN;
				for( int x = 0; x < pxPlotWidth; x++ ){
					for( int y = 0; y < pxPlotHeight; y++ ){
						dX = range.dBeginX + x * dRangeX / dMax_x;
						if( ! interpreter.zSet( "_x", dX, sbError ) ){
							sbError.insert( 0, "failed to set _x to " + dX );
							return null;
						}
						dY = range.dBeginY + y * dRangeY / dMax_y;
						if( ! interpreter.zSet( "_y", dY, sbError ) ){
							sbError.insert( 0, "failed to set _y to " + dY );
							return null;
						}
						
						PyObject pyobjectV = interpreter.zEval( codeV, sbError );
						if( pyobjectV == null ){ // probably division by zero
							dV = dMissing;
						} else {
							dV = pyobjectV.asDouble();
						}
						xPlotBufferIndex = y * pxPlotWidth + x;
						coordinates.madPlotBuffer[xPlotBufferIndex] = dV;
						xPlotBufferIndex++;
					}
				}
				break; }
			case Cartesian:
				org.python.core.PyCode codeY = compiled_y.compiled_code;
				zWriting = false;
				while( true ){  // two passes, one to count the number of points, the second to write the points
					int y = 0;
					double dX = 0;
					double dY = 0;
					int ctPoints = 0;
					for( int x = 0; x < pxPlotWidth; x++ ){ // there will be at least one point for every x pixel

						// determine y coordinate for x value
						double dX_previous = dX;
						dX = range.dBeginX + x * dRangeX / dMax_x;
						if( ! interpreter.zSet( "_x", dX, sbError ) ){
							sbError.insert( 0, "failed to set _x to " + dX );
							return null;
						}
						PyObject pyobjectY = interpreter.zEval( codeY, sbError );
						if( pyobjectY == null ){ // probably division by zero
							continue;
//							sbError.insert( 0, "failed to eval _y expression " +  compiled_y + ": " ); // TODO division by zero
//							return null;
						}
						double dY_previous = x == 0 ? pyobjectY.asDouble() : dY;
						dY = pyobjectY.asDouble();
						double dY_coordinate = dMax_y * (dY - range.dBeginY) / dRangeY;
						int y_previous = y;
						y = (int)dY_coordinate;
						if( dY_coordinate > 0 ){
							if( dY_coordinate - y >= 0.5d ) y++; // round up
						} else {
							if( y - dY_coordinate >= 0.5d ) y--; // round down
						}

						// interpolate points if y-coordinates are not continuous
						if( ctPoints > 0 && ( y - y_previous > 1 || y_previous - y > 1 ) ){
							double dY_delta = dRangeY / dMax_y / 100;
							double dX_begin = dX_previous;
							double dX_end = dX;
							double dY_begin = dY_previous;
							double dY_end = dY;
							boolean zAscend = (y - y_previous > 1);
							int iIncrement = zAscend ? 1 : -1; 
							for( int y_interpolation = y_previous + iIncrement; y_interpolation != y; y_interpolation += iIncrement ){
								if( zWriting ){
									double dY_target = y_interpolation * dRangeY / dMax_y + range.dBeginY;
									double dX_interpolation = dFindInterpolatedX( dY_target, dY_delta, dX_begin, dX_end, dY_begin, dY_end, interpreter, codeY, sbError );
									double dX_coordinate = dMax_x * (dX_interpolation - range.dBeginX) / dRangeX;
									int x_coordinate = (int)dX_coordinate;
									if( dX_interpolation > 0 ){
										if( dX_interpolation - x_coordinate >= 0.5d ) x_coordinate++; // round up
									} else {
										if( x_coordinate - dX_interpolation >= 0.5d ) x_coordinate--; // round down
									}
									coordinates.ax0[ctPoints] = x_coordinate;
									coordinates.ay0[ctPoints] = y_interpolation;
									coordinates.ax0_value[ctPoints] = dX_interpolation;
									coordinates.ay0_value[ctPoints] = dY_target;
								}
								ctPoints++;
							}
						}

						// plot the point
						if( y >= pxPlotHeight || y < 0 ){
							// outside plottable view, do not plot
						} else {
							if( zWriting ){
								coordinates.ax0[ctPoints] = x;
								coordinates.ay0[ctPoints] = y;
								coordinates.ax0_value[ctPoints] = dX;
								coordinates.ay0_value[ctPoints] = dY;
							}
							ctPoints++;
						}
						
					}
					if( zWriting ) break;
					coordinates.setCapacity( ctPoints );
					zWriting = true;
				}

				// generate rendered points (antialiased or jagged)
				zWriting = false;
				boolean zAntialiased = true;
				double dThickness_pixels = 1;
				int ctRenderedPoints = 0;
				while( true ){  // two passes, one to count the number of points, the second to write the points
					ctRenderedPoints = 0;
					Utility_Array.clear( coordinates.maiPlotBuffer );
					for( int xPoint = 0; xPoint < coordinates.ctPoints; xPoint++ ){ // cycle through the dimensionless points and define points which are actually rendered
						int x = coordinates.ax0[xPoint];
						int y = coordinates.ay0[xPoint];
						ctRenderedPoints += makePoint( coordinates, xPoint, x, y, ctRenderedPoints, dRangeX, dRangeY, pxPlotWidth, pxPlotHeight, dThickness_pixels, zWriting, 0 ); 
					}
					if( zWriting ) break;
					coordinates.setCapacity_Rendered( ctRenderedPoints, zAntialiased );
					zWriting = true;
				}

				break;
			case Polar:
				org.python.core.PyCode codeR = compiled_r.compiled_code;
				int pxPlotCenter_x = pxPlotWidth / 2; 
				int pxPlotCenter_y = pxPlotHeight / 2; 

				// determine the angular resolution in radians based on the plot area
				double dCornerDistance = Math.sqrt( pxPlotWidth * pxPlotWidth + pxPlotHeight * pxPlotHeight );
				double angular_resolution_radians = Math.asin( 1 / dCornerDistance );

				// size the plotting arrays according to the angular resolution
				double dRangeTheta = range.dEndTheta - range.dBeginTheta;
				if( dRangeTheta < 0 ) dRangeTheta *= -1;
				if( dRangeTheta > 2 * Math.PI ) dRangeTheta = 2 * Math.PI; 
				int iRadialCount = (int)( dRangeTheta / angular_resolution_radians ); 
				double[] y_value_calculated = new double[iRadialCount];
				int[] y_value_plotted = new int[iRadialCount];
				double[] x_value_calculated = new double[iRadialCount];
				int[] x_value_plotted = new int[iRadialCount];
				double y_scale = 1d;
				double y_offset = 0d;
				double x_scale = 1d;
				double x_offset = 0d;

				// calculate the direct xy-values and determine xy-range for a full circle
				double y_max = Double.MIN_VALUE;
				double y_min = Double.MAX_VALUE;
				double x_max = Double.MIN_VALUE;
				double x_min = Double.MAX_VALUE;
				double dR = 0;
				double dX = 0;
				double dY = 0;
				int y = 0;
				int x = 0;
				int ctPoints = 0;
				for( int xRadial = 0; xRadial < iRadialCount; xRadial++ ){
					
					// calculate real values
					double theta = range.dBeginTheta + angular_resolution_radians * xRadial;
					Object oCurrentThetaValue = new Double( theta );
					if( ! interpreter.zSet( "_theta", oCurrentThetaValue, sbError ) ){
						ApplicationController.vShowError( "failed to set theta-value to " + oCurrentThetaValue + " for radial " + xRadial + ": " + sbError );
						return null;
					}
					PyObject pyobject_range = interpreter.zEval( codeR, sbError );
					if( pyobject_range == null ){
						ApplicationController.vShowError( "failed to evaluate polar expression '" + msProcessedExpression + "' with theta value of " + oCurrentThetaValue + ": " + sbError );
						return null;
					}
					double dR_previous = dR; 
					double dX_previous = dX; 
					double dY_previous = dY; 
					dR = pyobject_range.asDouble();
					dX = dR * Math.cos( theta );
					dY = dR * Math.sin( theta );
					
					// determine plot coordinates
					double dX_coordinate = dMax_x * (dX - range.dBeginX) / dRangeX;
					double dY_coordinate = dMax_y * (dY - range.dBeginY) / dRangeY;
					int x_previous = x;
					int y_previous = y;
					x = (int)dX_coordinate;
					if( dX_coordinate > 0 ){
						if( dX_coordinate - x >= 0.5d ) x++; // round up
					} else {
						if( x - dX_coordinate >= 0.5d ) x--; // round down
					}
					y = (int)dY_coordinate;
					if( dY_coordinate > 0 ){
						if( dY_coordinate - y >= 0.5d ) y++; // round up
					} else {
						if( y - dY_coordinate >= 0.5d ) y--; // round down
					}
					
					// interpolate points in a line if coordinates are not continuous TODO hard to do curves because could be an infinite number of points between radials
					if( ctPoints > 0 && ( y - y_previous > 1 || y_previous - y > 1 || x - x_previous > 1 || x_previous - x > 1 ) ){
						int x_range = x >= x_previous ? x - x_previous : x_previous - x;
						int y_range = y >= y_previous ? y - y_previous : y_previous - y;
						int x_begin = x_previous;
						int x_end = x;
						int y_begin = y_previous;
						int y_end = y;
						boolean zAscend_y = (y - y_previous > 1);
						boolean zAscend_x = (x - x_previous > 1);
						int iIncrement_y = zAscend_y ? 1 : -1; 
						int iIncrement_x = zAscend_x ? 1 : -1; 
						if( x_range > y_range ){
							int interpolation_index = 0;
							for( int x_interpolation = x_begin + iIncrement_x; x_interpolation != x_end; x_interpolation += iIncrement_x ){
								interpolation_index++;
								int y_interpolation = y_begin + iIncrement_y * interpolation_index * y_range / x_range;    
								if( zWriting ){
									coordinates.ax0[ctPoints] = x_interpolation;
									coordinates.ay0[ctPoints] = y_interpolation;
									coordinates.ax0_value[ctPoints] = range.dBeginX + x_interpolation * dRangeX / dMax_x;
									coordinates.ay0_value[ctPoints] = range.dBeginY + y_interpolation * dRangeY / dMax_y;
								}
								ctPoints++;
							}
						} else {
							int interpolation_index = 0;
							for( int y_interpolation = y_begin + iIncrement_y; y_interpolation != y_end; y_interpolation += iIncrement_y ){
								interpolation_index++;
								int x_interpolation = x_begin + iIncrement_x * interpolation_index * x_range / y_range;    
								if( zWriting ){
									coordinates.ax0[ctPoints] = x_interpolation;
									coordinates.ay0[ctPoints] = y_interpolation;
									coordinates.ax0_value[ctPoints] = range.dBeginX + x_interpolation * dRangeX / dMax_x;
									coordinates.ay0_value[ctPoints] = range.dBeginY + y_interpolation * dRangeY / dMax_y;
								}
								ctPoints++;
							}
						}

						// plot the point
						if( y >= pxPlotHeight || y < 0 || x >= pxPlotWidth || x < 0 ){
							// outside plottable view, do not plot
						} else {
							if( zWriting ){
								coordinates.ax0[ctPoints] = x;
								coordinates.ay0[ctPoints] = y;
								coordinates.ax0_value[ctPoints] = dX;
								coordinates.ay0_value[ctPoints] = dY;
							}
							ctPoints++;
						}
// ???				
//				y_offset = y_min + pxPlotCenter_y;
//				y_scale = pxPlotHeight / (y_max - y_min);
//				x_offset = x_min + pxPlotCenter_x;
//				x_scale = pxPlotWidth / (x_max - x_min);
						
					}
					if( zWriting ) break;
					coordinates.setCapacity( ctPoints );
					zWriting = true;
				}
				break;
			case Parametric:
		}
		coordinates.maiPlotBuffer = null;
		coordinates.zHasAlpha = true;
		return coordinates;
	}

	// the idea of the mode is to make the recursive search move outwards, the mode pattern is:
	//     1 2 2 2 3
	//     8 1 2 3 4
	//     8 8 0 4 4
	//     8 7 6 5 4
	//     7 6 6 6 5
	private int makePoint( PlotCoordinates coordinates, int xPoint, int x, int y, int xRenderedPoint, double dRangeX, double dRangeY, int iPlotWidth, int iPlotHeight, double dThickness_pixels, boolean zWriting, int mode ){
		if( x < 0 || x >= iPlotWidth || y < 0 || y >= iPlotHeight ) return 0;
		double dMax_x = iPlotWidth - 1;  // if the plot is 100 pixels wide, x goes from 0 to 99
		double dMax_y = iPlotHeight - 1;
		double dThicknessThreshold = dThickness_pixels * 0.5;
		double dAntialiasingThreshold = dThicknessThreshold + 1;
		double dX = (double)x;
		double dY = (double)y;
		int xCurrentPoint = xPoint;
		int xPreviousPoint = xPoint == 0 ? xPoint : xPoint - 1;
		int xNextPoint = xPoint == coordinates.ctPoints - 1 ? xPoint : xPoint + 1;
		double dX_Current = ( coordinates.ax0_value[xCurrentPoint] - range.dBeginX ) * dMax_x / dRangeX; // this is the pixel-related coordinate of the value
		double dY_Current = ( coordinates.ay0_value[xCurrentPoint] - range.dBeginY ) * dMax_y / dRangeY; // this is the pixel-related coordinate of the value
		double dX_Previous = ( coordinates.ax0_value[xPreviousPoint] - range.dBeginX ) * dMax_x / dRangeX; // this is the pixel-related coordinate of the value
		double dY_Previous = ( coordinates.ay0_value[xPreviousPoint] - range.dBeginY ) * dMax_y / dRangeY; // this is the pixel-related coordinate of the value
		double dX_PreviousMidpoint = ( dX_Current + dX_Previous )/2;  
		double dY_PreviousMidpoint = ( dY_Current + dY_Previous )/2;
		double dX_Next = ( coordinates.ax0_value[xNextPoint] - range.dBeginX ) * dMax_x / dRangeX; // this is the pixel-related coordinate of the value
		double dY_Next = ( coordinates.ay0_value[xNextPoint] - range.dBeginY ) * dMax_y / dRangeY; // this is the pixel-related coordinate of the value
		double dX_NextMidpoint = ( dX_Current + dX_Next )/2;  
		double dY_NextMidpoint = ( dY_Current + dY_Next )/2;
		double distanceCurrentPoint = Math.sqrt( (dX - dX_Current) * (dX - dX_Current) + (dY - dY_Current) * (dY - dY_Current) );
		double distancePreviousPoint = Math.sqrt( (dX - dX_Previous) * (dX - dX_Previous) + (dY - dY_Previous) * (dY - dY_Previous) );
		double distanceNextPoint = Math.sqrt( (dX - dX_Next) * (dX - dX_Next) + (dY - dY_Next) * (dY - dY_Next) );
		double distancePreviousMidpoint = Math.sqrt( (dX - dX_PreviousMidpoint) * (dX - dX_PreviousMidpoint) + (dY - dY_PreviousMidpoint) * (dY - dY_PreviousMidpoint) );
		double distanceNextMidpoint = Math.sqrt( (dX - dX_NextMidpoint) * (dX - dX_NextMidpoint) + (dY - dY_NextMidpoint) * (dY - dY_NextMidpoint) );
		double distance = distanceCurrentPoint;
		if( distancePreviousPoint < distance ) distance = distancePreviousPoint; 
		if( distanceNextPoint < distance ) distance = distanceNextPoint; 
		if( distancePreviousMidpoint < distance ) distance = distancePreviousMidpoint; 
		if( distanceNextMidpoint < distance ) distance = distanceNextMidpoint;  
//		if( xPoint < 30 ) System.out.format( "xPoint %d  distance: %f  dX_Current: %f  dY_Current: %f  dX: %f  dY: %f\n", xPoint, distance, dX_Current, dY_Current, dX, dY ); 		
		if( distance > dAntialiasingThreshold ) return 0; // do not make a point if point is beyond thickness of line
		int alpha = distance > dThicknessThreshold ?
				(int)( 0xFF + 0xAA * Math.log( (dAntialiasingThreshold - distance)/dAntialiasingThreshold ) ) :
				0xFF;
		if( alpha <= 0 ) return 0; // do not plot totally transparent points
		if( coordinates.maiPlotBuffer[x][y] > 0 ){ // point has already been plotted
			if( alpha > coordinates.maiPlotBuffer[x][y] ){
				if( zWriting ){  // go back and update the coordinate list with the higher alpha
					for( int xPriorPlottedPoint = xRenderedPoint - 1; xPriorPlottedPoint >= 0; xPriorPlottedPoint-- ){
						if( 	coordinates.ax0_antialiased[xPriorPlottedPoint] == x &&
								coordinates.ay0_antialiased[xPriorPlottedPoint] == y ){
								coordinates.aAlpha[xPriorPlottedPoint] = alpha;
								return 0;
						}
					}
				}
				coordinates.maiPlotBuffer[x][y] = alpha; // record the higher alpha
			}
			return 0;
		}
		coordinates.maiPlotBuffer[x][y] = alpha; // new point
		if( zWriting ){
			coordinates.ax0_antialiased[xRenderedPoint] = x;
			coordinates.ay0_antialiased[xRenderedPoint] = y;
			coordinates.aAlpha[xRenderedPoint] = alpha;
		}
		int ctPointsMade = 1;
		switch( mode ){
			case 0:
				ctPointsMade += makePoint( coordinates, xPoint, x + 1, y, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 4 );
				ctPointsMade += makePoint( coordinates, xPoint, x + 1, y + 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 3 );
				ctPointsMade += makePoint( coordinates, xPoint, x, y + 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 2 );
				ctPointsMade += makePoint( coordinates, xPoint, x - 1, y, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 8 );
				ctPointsMade += makePoint( coordinates, xPoint, x - 1, y - 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 7 );
				ctPointsMade += makePoint( coordinates, xPoint, x, y - 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 6 );
				ctPointsMade += makePoint( coordinates, xPoint, x + 1, y - 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 5 );
				ctPointsMade += makePoint( coordinates, xPoint, x - 1, y + 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 1 );
				break;
			case 1:
				ctPointsMade += makePoint( coordinates, xPoint, x - 1, y + 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 1 );
				ctPointsMade += makePoint( coordinates, xPoint, x - 1, y, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 8 );
				ctPointsMade += makePoint( coordinates, xPoint, x, y + 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 2 );
				break;
			case 2:
				ctPointsMade += makePoint( coordinates, xPoint, x, y + 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 2 );
				break;
			case 3:
				ctPointsMade += makePoint( coordinates, xPoint, x + 1, y + 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 3 );
				ctPointsMade += makePoint( coordinates, xPoint, x, y + 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 2 );
				ctPointsMade += makePoint( coordinates, xPoint, x + 1, y, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 4 );
				break;
			case 4:
				ctPointsMade += makePoint( coordinates, xPoint, x + 1, y, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 4 );
				break;
			case 5:
				ctPointsMade += makePoint( coordinates, xPoint, x + 1, y - 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 5 );
				ctPointsMade += makePoint( coordinates, xPoint, x + 1, y, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 4 );
				ctPointsMade += makePoint( coordinates, xPoint, x, y - 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 6 );
				break;
			case 6:
				ctPointsMade += makePoint( coordinates, xPoint, x, y - 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 6 );
			case 7:
				ctPointsMade += makePoint( coordinates, xPoint, x - 1, y - 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 7 );
				ctPointsMade += makePoint( coordinates, xPoint, x, y - 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 6 );
				ctPointsMade += makePoint( coordinates, xPoint, x - 1, y, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 8 );
				break;
			case 8:
				ctPointsMade += makePoint( coordinates, xPoint, x - 1, y, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThickness_pixels, zWriting, 8 );
				break;
		}
		return ctPointsMade;
	}
	
	private double dFindInterpolatedX( double dY_target, double dY_delta, double dX_begin, double dX_end, double dY_begin, double dY_end, opendap.clients.odc.Interpreter odc_interpreter, org.python.core.PyCode codeY, StringBuffer sbError ){
//		if( dX_begin > dX_end && dX_begin - dX_end < dX_delta ) return Double.NaN; // interpolation failure
//		if( dX_begin < dX_end && dX_end - dX_begin < dX_delta ) return Double.NaN; // interpolation failure
		double dX = (dX_end + dX_begin) / 2;
		if( ! odc_interpreter.zSet( "_x", dX, sbError ) ){
			sbError.insert( 0, "failed to set _x to " + dX );
			return Double.NaN;
		}
		PyObject pyobjectY = odc_interpreter.zEval( codeY, sbError );
		if( pyobjectY == null ){
			sbError.insert( 0, "failed to eval _y expression " +  compiled_y + ": " );
			return Double.NaN;
		}
		double dY = pyobjectY.asDouble();
//System.out.format( "dY %f, dX %f, dY_target %f, dY_delta %f, dX_begin %f, dX_end %f, dY_begin %f, dY_end %f \n", dY, dX, dY_target, dY_delta, dX_begin, dX_end, dY_begin, dY_end );
		double dY_difference = dY > dY_target ? dY - dY_target : dY_target - dY;
		if( dY_difference < dY_delta ){
			return dX;
		} else {
			if( dY < dY_target ){
				if( dY_begin < dY_end ){
					return dFindInterpolatedX( dY_target, dY_delta, dX, dX_end, dY, dY_end, odc_interpreter, codeY, sbError );
				} else {
					return dFindInterpolatedX( dY_target, dY_delta, dX_begin, dX, dY_begin, dY, odc_interpreter, codeY, sbError );
				}	
			} else {
				if( dY_begin < dY_end ){
					return dFindInterpolatedX( dY_target, dY_delta, dX_begin, dX, dY_begin, dY, odc_interpreter, codeY, sbError );
				} else {
					return dFindInterpolatedX( dY_target, dY_delta, dX, dX_end, dY, dY_end, odc_interpreter, codeY, sbError );
				}	
			}
		}
	}
	
}

class PlotRange {
	double dBeginX;
	double dEndX;
	double dBeginY;
	double dEndY;
	double dBeginTheta;
	double dEndTheta;
	double dEndR;
}

class PlotCoordinates {
	boolean zHasAlpha = false;
	boolean zHasColor = false;
	int argbBaseColor = 0xFF000000; // solid black
	boolean zIsFillPlot = false; // true if it is a pseudocolor--fills the whole plot area
	
	// in the case of fill plots the plot buffer will have the data value for each cell
	double[] madPlotBuffer; 
	
	// used for an XY plot
	int[][] maiPlotBuffer;  // this is used to optimize the generation of the plot coordinates
	                        // by providing a pre-plot of the curve so that the point discovery
	                        // algorithm can determine if the point has already been plotted
	                        // By doing this the step of eliminating redundant points can be avoided
	                        // This results in a faster algorithm at the expense of allocating a big buffer.	
	
	// actual (dimensionless points)
	int ctPoints;
	int[] ax0;
	int[] ay0;
	double[] ax0_value; // values on which the coordinate is based
	double[] ay0_value;

	int ctPoints_jagged;
	int[] ax0_jagged;
	int[] ay0_jagged;

	int ctPoints_antialiased;
	int[] ax0_antialiased;
	int[] ay0_antialiased;
	int[] aAlpha;
	
	void setCapacity( int ctPoints ){
		this.ctPoints = ctPoints;
		ax0 = new int[ctPoints];
		ay0 = new int[ctPoints];
		ax0_value = new double[ctPoints]; // values on which the coordinate is based
		ay0_value = new double[ctPoints];	
	}
	
	void setCapacity_Rendered( int ctPoints, boolean zAntialiased ){
		if( zAntialiased ){
			ctPoints_antialiased = ctPoints;
			ax0_antialiased = new int[ctPoints];
			ay0_antialiased = new int[ctPoints];
			aAlpha = new int[ctPoints];
		} else {
			int ctPoints_jagged;
			ax0_jagged = new int[ctPoints];
			ay0_jagged = new int[ctPoints];
		}
	}
	
	String sDump(){
		StringBuffer sb = new StringBuffer();
		sb.append( "zHasAlpha: " + zHasAlpha ).append( '\n' ); 
		sb.append( "zHasColor: " + zHasColor ).append( '\n' ); 
		sb.append( "argbBaseColor: " + Integer.toHexString( argbBaseColor ) ).append( '\n' ); 
		sb.append( "ctPoints: " + ctPoints ).append( '\n' );
		return sb.toString();
	}
	String sDump( int ctPointsToDump ){
		if( ctPointsToDump > ctPoints ) ctPointsToDump = ctPoints; 
		return sDump( 0, ctPointsToDump );
	}
	String sDump( int xFrom, int xTo ){
		StringBuffer sb = new StringBuffer();
		sb.append( "zHasAlpha: " + zHasAlpha ).append( '\n' ); 
		sb.append( "zHasColor: " + zHasColor ).append( '\n' ); 
		sb.append( "argbBaseColor: " + Integer.toHexString( argbBaseColor ) ).append( '\n' ); 
		sb.append( "dimensionless ctPoints: " + ctPoints ).append( '\n' );
		for( int xPoint = xFrom; xPoint <= xTo; xPoint++ ){
			sb.append( String.format( "%d %d %f %f\n", ax0[xPoint], ay0[xPoint], ax0_value[xPoint], ay0_value[xPoint] ) );
		}
		if( ctPoints_jagged > 0 ){
			sb.append( "jagged ctPoints: " + ctPoints_jagged ).append( '\n' );
			for( int xPoint = xFrom; xPoint <= xTo && xPoint < ctPoints_jagged; xPoint++ ){
				sb.append( String.format( "%d %d %d\n", ax0_jagged[xPoint], ay0_jagged[xPoint] ) );
			}
		}
		if( ctPoints_antialiased > 0 ){
			sb.append( "antialiased ctPoints: " + ctPoints_antialiased ).append( '\n' );
			for( int xPoint = xFrom; xPoint <= xTo && xPoint < ctPoints_antialiased; xPoint++ ){
				sb.append( String.format( "%d: %d %d %d\n", xPoint, ax0_antialiased[xPoint], ay0_antialiased[xPoint], aAlpha[xPoint] ) );
			}
		}
		return sb.toString();
	}
}

enum PlottableExpression_TYPE {
	Cartesian,
	Polar,
	Parametric, 
	Pseudocolor
}

class Identifier {
	private Identifier(){}
	final static Identifier create( String s, int pos ){
		Identifier identifier = new Identifier();
		identifier.s = s;
		identifier.pos = pos;
		return identifier;
	}
	String s;
	int pos;
	public static boolean contains( ArrayList<Identifier> list, String sIdentifier ){
		for( Identifier i : list ){
			if( i.s.equalsIgnoreCase( sIdentifier ) ) return true;
		}
		return false;
	}
}

class CompiledExpression {
	String sLineExpression;
	String sVariableIdentifier;
	org.python.core.PyCode compiled_code;
	final static CompiledExpression create( String sSymbol, String sLine, org.python.core.PyCode code ){
		CompiledExpression exp = new CompiledExpression();
		exp.sLineExpression = sLine;
		exp.sVariableIdentifier = sSymbol;
		exp.compiled_code = code;
		return exp;
	}
}

