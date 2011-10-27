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
	public CompiledExpression compiled_x;
	public CompiledExpression compiled_y;
	public CompiledExpression compiled_z;
	public CompiledExpression compiled_r;
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
		String[] asDollarTokens = { "$x", "$y", "$z", "$r", "$t", "$theta" };
		String[] asKnownParameters = {
				"$x", "$y", "$z", "$r", "$t", "$theta",
				"x", "y", "z", "r", "t", "theta",
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
			if( Identifier.contains( listIdentifiers, "$x" ) && Identifier.contains( listIdentifiers, "$y" ) ){
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
			if( Identifier.contains( listIdentifiers, "x" ) && Identifier.contains( listIdentifiers, "y" ) ){
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
				if( sVariable.equals( "_x") ){
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
		PlotRange range = new PlotRange();
		range.dBeginX = dBeginX;
		range.dEndX = dEndX;
		switch( type ){
			case Cartesian:
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
				// TODO
			case Parametric:
				// TODO
		}
		return range;
	}
	
	public PlotCoordinates getPlotCoordinates( int iPlotWidth, int iPlotHeight, StringBuffer sbError ){
		if( range == null ){
			range = getPlotRange( sbError );
			if( range == null ) return null;
		}
		PlotCoordinates coordinates = new PlotCoordinates();
		opendap.clients.odc.Interpreter odc_interpreter = ApplicationController.getInstance().getInterpreter();
		double dRangeX = range.dEndX - range.dBeginX;
		double dRangeY = range.dEndY - range.dBeginY;
		switch( type ){
			case Cartesian:
				org.python.core.PyCode codeY = compiled_y.compiled_code;
				boolean zWriting = false;
				while( true ){  // two passes, one to count the number of points, the second to write the points
					int y = 0;
					double dX = 0;
					double dY = 0;
					int ctPoints = 0;
					for( int x = 0; x < iPlotWidth; x++ ){ // there will be at least one point for every x pixel

						// determine y coordinate for x value
						double dX_previous = dX;
						dX = range.dBeginX + x * dRangeX / iPlotWidth;
						if( ! odc_interpreter.zSet( "_x", dX, sbError ) ){
							sbError.insert( 0, "failed to set _x to " + dX );
							return null;
						}
						PyObject pyobjectY = odc_interpreter.zEval( codeY, sbError );
						if( pyobjectY == null ){
							sbError.insert( 0, "failed to eval _y expression " +  compiled_y + ": " );
							return null;
						}
						double dY_previous = x == 1 ? pyobjectY.asDouble() : dY;
						dY = pyobjectY.asDouble();
						double dY_coordinate = iPlotHeight * (dY - range.dBeginY) / dRangeY;
						int y_previous = y;
						y = (int)dY_coordinate;
						
						// plot the point
						if( y >= iPlotHeight || y < 0 ){
							// outside plottable view, do not plot
						} else {
							if( dY_coordinate - y >= 0.5d ) y++; // round
							if( zWriting ){
								coordinates.ax0[x] = x;
								coordinates.ay0[x] = y;
								coordinates.ax0_value[x] = dX;
								coordinates.ay0_value[x] = dY;
							}
							ctPoints++;
						}
						
						// interpolate points if y-coordinates are not continuous
						if( ctPoints > 1 && ( y - y_previous > 1 || y_previous - y > 1 ) ){
							double dY_delta = dRangeY / iPlotHeight / 100;
							double dX_begin = dX_previous;
							double dX_end = dX;
							double dY_begin = dY_previous;
							double dY_end = dY;
							boolean zAscend = (y - y_previous > 1);
							int iIncrement = zAscend ? 1 : -1; 
							for( int y_interpolation = y_previous + iIncrement; y_interpolation != y; y_interpolation += iIncrement ){
								double dY_target = y_interpolation * dRangeY / iPlotHeight + range.dBeginY;
								double dX_interpolation = dFindInterpolatedX( dY_target, dY_delta, dX_begin, dX_end, dY_begin, dY_end, odc_interpreter, codeY, sbError );
								if( zWriting ){
									int x_coordinate = (int)(iPlotWidth * (dX - range.dBeginX) / dRangeX);
									coordinates.ax0[x] = x_coordinate;
									coordinates.ay0[x] = y_interpolation;
									coordinates.ax0_value[x] = dX_interpolation;
									coordinates.ay0_value[x] = dY_target;
								}
								ctPoints++;
							}
						}
					}
					if( zWriting ) break;
					coordinates.setCapacity( ctPoints );
					zWriting = true;
				}

				// generate rendered points (antialiased or jagged)
				zWriting = false;
				boolean zAntialiased = true;
				double dThicknessSquared = 8;
				int ctRenderedPoints = 0;
				while( true ){  // two passes, one to count the number of points, the second to write the points
					ctRenderedPoints = 0;
					for( int xPoint = 0; xPoint < coordinates.ctPoints; xPoint++ ){ // cycle through the dimensionless points and define points which are actually rendered
						int x = coordinates.ax0[xPoint];
						int y = coordinates.ay0[xPoint];
						ctRenderedPoints += makePoint( coordinates, xPoint, x, y, ctRenderedPoints, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 0 ); 
					}
					if( zWriting ) break;
					coordinates.setCapacity_Rendered( ctRenderedPoints, zAntialiased );
					zWriting = true;
					System.out.println( "rendered points found: " + ctRenderedPoints );
				}

				// consolidate rendered points so there are no duplicates
				// if two points coincide, then the alpha value which is more opaque is used
				int[] ax0_unique = null;
				int[] ay0_unique = null;
				int[] alpha_unique = null;
				zWriting = false;
				while( true ){
					int ctRenderedPoints_unique = 0;
					for( int xRenderedPoint = 0; xRenderedPoint < ctRenderedPoints - 1; xRenderedPoint++ ){
						int xRenderedPoint2 = xRenderedPoint + 1;
						while( true ){
							if( xRenderedPoint2 == ctRenderedPoints ){
								if( zWriting ){
									int alpha_max = coordinates.aAlpha[xRenderedPoint];
									for( int xBacktrack = xRenderedPoint - 1; xBacktrack >= 0; xBacktrack-- ){
										if( coordinates.ax0_antialiased[xRenderedPoint] == coordinates.ax0_antialiased[xBacktrack] &&
											coordinates.ay0_antialiased[xRenderedPoint] == coordinates.ay0_antialiased[xBacktrack] &&
											coordinates.aAlpha[xBacktrack] > alpha_max ){
											alpha_max = coordinates.aAlpha[xBacktrack];
										}
									}
									ax0_unique[ctRenderedPoints_unique] = coordinates.ax0_antialiased[xRenderedPoint]; 
									ay0_unique[ctRenderedPoints_unique] = coordinates.ay0_antialiased[xRenderedPoint];
									coordinates.aAlpha[ctRenderedPoints_unique] = alpha_max;
								}
								ctRenderedPoints_unique++;
								break;
							}
							if( coordinates.ax0_antialiased[xRenderedPoint] == coordinates.ax0_antialiased[xRenderedPoint2] &&
								coordinates.ay0_antialiased[xRenderedPoint] == coordinates.ay0_antialiased[xRenderedPoint2] ){
								break;
							}
							xRenderedPoint2++;
						}
					}
					if( zWriting ){
						coordinates.ax0_antialiased = ax0_unique;
						coordinates.ay0_antialiased = ay0_unique;
						coordinates.aAlpha = alpha_unique;
						break;
					}
					coordinates.ctPoints_antialiased = ctRenderedPoints_unique;
					ax0_unique = new int[ctRenderedPoints_unique];
					ay0_unique = new int[ctRenderedPoints_unique];
					alpha_unique = new int[ctRenderedPoints_unique];
					zWriting = true;
					System.out.println( "unique renderings: " + ctRenderedPoints_unique );
				}
				coordinates.zHasAlpha = true;
				
				break;
			case Polar:
			case Parametric:
		}
		return coordinates;
	}

	// the idea of the mode is to make the recursive search move outwards, the mode pattern is:
	//     1 2 2 2 3
	//     8 1 2 3 4
	//     8 8 0 4 4
	//     8 7 6 5 4
	//     7 6 6 6 5
	private int makePoint( PlotCoordinates coordinates, int xPoint, int x, int y, int xRenderedPoint, double dRangeX, double dRangeY, int iPlotWidth, int iPlotHeight, double dThicknessSquared, boolean zWriting, int mode ){ 
		double dX = (double)x;
		double dY = (double)y;
		int xCurrentPoint = xPoint;
		int xPreviousPoint = xPoint == 0 ? xPoint : xPoint - 1;
		int xNextPoint = xPoint == coordinates.ctPoints - 1 ? xPoint : xPoint + 1;
		double dX_Current = ( coordinates.ax0_value[xCurrentPoint] - range.dBeginX ) * iPlotWidth / dRangeX; // this is the pixel-related coordinate of the value
		double dY_Current = ( coordinates.ay0_value[xCurrentPoint] - range.dBeginY ) * iPlotHeight / dRangeY; // this is the pixel-related coordinate of the value
		double dX_Previous = ( coordinates.ax0_value[xPreviousPoint] - range.dBeginX ) * iPlotWidth / dRangeX; // this is the pixel-related coordinate of the value
		double dY_Previous = ( coordinates.ay0_value[xPreviousPoint] - range.dBeginY ) * iPlotHeight / dRangeY; // this is the pixel-related coordinate of the value
		double dX_Next = ( coordinates.ax0_value[xNextPoint] - range.dBeginX ) * iPlotWidth / dRangeX; // this is the pixel-related coordinate of the value
		double dY_Next = ( coordinates.ay0_value[xNextPoint] - range.dBeginY ) * iPlotHeight / dRangeY; // this is the pixel-related coordinate of the value
		double distance2CurrentPoint = (dX - dX_Current) * (dX - dX_Current) + (dY - dY_Current) * (dY - dY_Current);
		double distance2PreviousPoint = (dX - dX_Previous) * (dX - dX_Previous) + (dY - dY_Previous) * (dY - dY_Previous);
		double distance2NextPoint = (dX - dX_Next) * (dX - dX_Next) + (dY - dY_Next) * (dY - dY_Next);
		double distanceSquaredAverage = ( distance2CurrentPoint + distance2PreviousPoint + distance2NextPoint )/3;
		if( distanceSquaredAverage > dThicknessSquared ) return 0; // do not make a point if point is beyond thickness of line
		if( zWriting ){
			int alpha = (int)((dThicknessSquared - distanceSquaredAverage) * 0xFF / dThicknessSquared);
			coordinates.ax0_antialiased[xRenderedPoint] = coordinates.ax0[xPoint];
			coordinates.ay0_antialiased[xRenderedPoint] = coordinates.ay0[xPoint];
			coordinates.aAlpha[xRenderedPoint] = alpha;
		}
		int ctPointsMade = 1;
		switch( mode ){
			case 0:
				ctPointsMade += makePoint( coordinates, xPoint, x + 1, y, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 4 );
				ctPointsMade += makePoint( coordinates, xPoint, x + 1, y + 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 3 );
				ctPointsMade += makePoint( coordinates, xPoint, x, y + 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 2 );
				ctPointsMade += makePoint( coordinates, xPoint, x - 1, y, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 8 );
				ctPointsMade += makePoint( coordinates, xPoint, x - 1, y - 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 7 );
				ctPointsMade += makePoint( coordinates, xPoint, x, y - 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 6 );
				ctPointsMade += makePoint( coordinates, xPoint, x + 1, y - 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 5 );
				ctPointsMade += makePoint( coordinates, xPoint, x - 1, y + 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 1 );
				break;
			case 1:
				ctPointsMade += makePoint( coordinates, xPoint, x - 1, y + 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 1 );
				ctPointsMade += makePoint( coordinates, xPoint, x - 1, y, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 8 );
				ctPointsMade += makePoint( coordinates, xPoint, x, y + 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 2 );
				break;
			case 2:
				ctPointsMade += makePoint( coordinates, xPoint, x, y + 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 2 );
				break;
			case 3:
				ctPointsMade += makePoint( coordinates, xPoint, x + 1, y + 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 3 );
				ctPointsMade += makePoint( coordinates, xPoint, x, y + 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 2 );
				ctPointsMade += makePoint( coordinates, xPoint, x + 1, y, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 4 );
				break;
			case 4:
				ctPointsMade += makePoint( coordinates, xPoint, x + 1, y, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 4 );
				break;
			case 5:
				ctPointsMade += makePoint( coordinates, xPoint, x + 1, y - 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 5 );
				ctPointsMade += makePoint( coordinates, xPoint, x + 1, y, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 4 );
				ctPointsMade += makePoint( coordinates, xPoint, x, y - 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 6 );
				break;
			case 6:
				ctPointsMade += makePoint( coordinates, xPoint, x, y - 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 6 );
			case 7:
				ctPointsMade += makePoint( coordinates, xPoint, x - 1, y - 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 7 );
				ctPointsMade += makePoint( coordinates, xPoint, x, y - 1, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 6 );
				ctPointsMade += makePoint( coordinates, xPoint, x - 1, y, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 8 );
				break;
			case 8:
				ctPointsMade += makePoint( coordinates, xPoint, x - 1, y, xRenderedPoint + ctPointsMade, dRangeX, dRangeY, iPlotWidth, iPlotHeight, dThicknessSquared, zWriting, 8 );
				break;
		}
		return ctPointsMade;
	}
	
	private double dFindInterpolatedX( double dY_target, double dY_delta, double dX_begin, double dX_end, double dY_begin, double dY_end, opendap.clients.odc.Interpreter odc_interpreter, org.python.core.PyCode codeY, StringBuffer sbError ){
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
		double dY_difference = dY > dY_target ? dY - dY_target : dY_target - dY;
		if( dY_difference < dY_delta ){
			return dX;
		} else {
			if( ( dY_begin < dY_end && dY < dY_target ) || (dY_begin > dY_end && dY > dY_target ) ){
				return dFindInterpolatedX( dY_target, dY_delta, dX_begin, dX, dY_begin, dY_end, odc_interpreter, codeY, sbError );
			} else {
				return dFindInterpolatedX( dY_target, dY_delta, dX, dX_end, dY_begin, dY_end, odc_interpreter, codeY, sbError );
			}
		}
	}
	
	private void makePoint( PlotCoordinates coordinates, int xPoint, int x, int y, double dX, double dY, double dY_previous ){
		coordinates.ax0[xPoint] = x;
		coordinates.ay0[xPoint] = y;
		double distanceCurrentPointSquared = (y - dY) * (y - dY);
		double distancePreviousPointSquared = (y - dY_previous) * (y - dY_previous);
		double distanceAveraged = ( distanceCurrentPointSquared + distancePreviousPointSquared ) / 2;
		coordinates.aAlpha[xPoint] = (int)(0xFF * (1 - distanceAveraged));
	}
}

class PlotRange {
	double dBeginX;
	double dEndX;
	double dBeginY;
	double dEndY;
}

class PlotCoordinates {
	boolean zHasAlpha = false;
	boolean zHasColor = false;
	int argbBaseColor = 0xFF000000; // solid black
	
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
	
	PlotRange range;
	String sDump(){
		StringBuffer sb = new StringBuffer();
		sb.append( "zHasAlpha: " + zHasAlpha ).append( '\n' ); 
		sb.append( "zHasColor: " + zHasColor ).append( '\n' ); 
		sb.append( "argbBaseColor: " + Integer.toHexString( argbBaseColor ) ).append( '\n' ); 
		sb.append( "dBeginX: " + range.dBeginX ).append( '\n' ); 
		sb.append( "dEndX: " + range.dEndX ).append( '\n' ); 
		sb.append( "dBeginY: " + range.dBeginY ).append( '\n' ); 
		sb.append( "dEndY: " + range.dEndY ).append( '\n' );
		sb.append( "ctPoints: " + ctPoints ).append( '\n' );
		return sb.toString();
	}
	String sDump( int ctPointsToDump ){
		if( ctPointsToDump > ctPoints ) ctPointsToDump = ctPoints; 
		StringBuffer sb = new StringBuffer();
		sb.append( "zHasAlpha: " + zHasAlpha ).append( '\n' ); 
		sb.append( "zHasColor: " + zHasColor ).append( '\n' ); 
		sb.append( "argbBaseColor: " + Integer.toHexString( argbBaseColor ) ).append( '\n' ); 
		sb.append( "dBeginX: " + range.dBeginX ).append( '\n' ); 
		sb.append( "dEndX: " + range.dEndX ).append( '\n' ); 
		sb.append( "dBeginY: " + range.dBeginY ).append( '\n' ); 
		sb.append( "dEndY: " + range.dEndY ).append( '\n' );
		sb.append( "ctPoints: " + ctPoints ).append( '\n' );
		for( int xPoint = 0; xPoint < ctPointsToDump; xPoint++ ){
			sb.append( String.format( "%d %d %d\n", ax0[xPoint], ay0[xPoint], aAlpha[xPoint] ) );
		}
		return sb.toString();
	}
}

enum PlottableExpression_TYPE {
	Cartesian,
	Polar,
	Parametric
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

