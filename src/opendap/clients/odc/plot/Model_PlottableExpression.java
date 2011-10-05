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
					listIdentifiers.add( new Identifier( sbBuffer.toString(), posIdentifier ) );
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
						listIdentifiers.add( new Identifier( sbBuffer.toString(), posIdentifier ) );
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
	
//	"x_range_begin", "x_range_end", "x_increment",
//	"y_range_begin", "y_range_end", "y_increment",
//	"r_range_begin", "r_range_end", "r_increment",
//	"t_range_begin", "t_range_end", "t_increment",
	// TODO use an integer mode if inputs are all integers
	public PlotCoordinates getPlotCoordinates( int iPlotWidth, int iPlotHeight, StringBuffer sbError ){
		PlotCoordinates coordinates = new PlotCoordinates();
		opendap.clients.odc.Interpreter odc_interpreter = ApplicationController.getInstance().getInterpreter();
		double dBeginX = odc_interpreter.get( "_x_range_begin", sbError );
		if( Double.isInfinite( dBeginX ) ){
			sbError.insert( 0, "error getting _x_range_begin: " );
			return null;
		}
		if( Double.isNaN( dBeginX ) ){ // not defined
			dBeginX = 0;
		}
		double dEndX = odc_interpreter.get( "_x_range_end", sbError );
		if( Double.isInfinite( dEndX ) ){
			sbError.insert( 0, "error getting _x_range_end: " );
			return null;
		}
		if( Double.isNaN( dEndX ) ){ // not defined
			dEndX = iPlotWidth;
		}
		double dRangeX = dEndX - dBeginX;
		double dIntervalX = dRangeX / (double)iPlotWidth; // numeric range associated with a pixel
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
		double dIntervalY = odc_interpreter.get( "_y_interval", sbError );
		if( Double.isInfinite( dIntervalY ) ){
			sbError.insert( 0, "error getting _y_interval: " );
			return null;
		}
		int eState = 0;
		boolean zBeginX_Defined = ! Double.isNaN( dBeginX ); 
		boolean zEndX_Defined = ! Double.isNaN( dEndX ); 
		boolean zBeginY_Defined = ! Double.isNaN( dBeginY ); 
		boolean zEndY_Defined = ! Double.isNaN( dEndY ); 
		switch( type ){
			case Cartesian:
				double dRangeY = 0;
				if( Double.isNaN( dBeginY ) ){ // '==' does not work for NaN values
					if( Double.isNaN( dEndY ) ){
						if( Double.isNaN( dIntervalY ) ){
							eState = 1; // "Determining Y Range, no constraints";
						} else {
							eState = 2; // "Determining Y Range, interval is known";
						}
						dBeginY = Double.MAX_VALUE;
						dEndY = Double.MIN_VALUE;
					} else {
						if( Double.isNaN( dIntervalY ) ){
							dBeginY = Double.MAX_VALUE;
							eState = 3; // "Determining Y Range, end is known";
						} else {
							dBeginY = dEndY - (iPlotHeight - 1) * dIntervalY;
							dRangeY = dEndY - dBeginY;
							eState = 5; // counting;
						}
					}
				} else {
					if( Double.isNaN( dEndY ) ){
						if( Double.isNaN( dIntervalY ) ){
							eState = 4; // "Determining Y Range, beginning is known";
							dEndY = Double.MIN_VALUE;
						} else {
							dEndY = dBeginY + (iPlotHeight - 1) * dIntervalY;
							dRangeY = dEndY - dBeginY;
							eState = 5; // counting;
						}
					} else {
						if( Double.isNaN( dIntervalY ) ){
							dIntervalY = (dEndY - dBeginY + 1.0d) / (double)iPlotHeight;
						} else {
							// all three parameters defined
						}
						dRangeY = dEndY - dBeginY;
						eState = 5; // counting
					}
				}
				while( true ){
					int ctPoints = 0;
					int y = 0;
					org.python.core.PyCode codeY = compiled_y.compiled_code;
					for( int x = 1; x <= iPlotWidth; x++ ){ // there will be at least one point for every x pixel
						double dX = dBeginX + x * dRangeX / iPlotWidth;
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
							case 2: // "Determining Y Range, interval is known"
								if( dY < dBeginY ) dBeginY = dY;
								if( dY > dEndY ) dEndY = dY;
								break;
							case 3: // "Determining Y Range, end is known"
								if( dY < dBeginY ) dBeginY = dY;
								break;
							case 4: // "Determining Y Range, beginning is known"
								if( dY > dEndY ) dEndY = dY;
								break;
							case 5: // counting
							case 6: // writing
								double dY_coordinate = iPlotHeight * (dY - dBeginY) / dRangeY;
								int y_previous = y;
								y = (int)dY_coordinate;
								if( dY_coordinate >= iPlotHeight || dY_coordinate < 0 ){
									// outside plottable view, do not plot
								} else {
									if( dY_coordinate - y >= 0.5d ) y++; // round
									if( eState == 6 ){ // write base point
										coordinates.ax0[ctPoints] = x;
										coordinates.ay0[ctPoints] = y;
									}
									ctPoints++;
									if( ctPoints > 1 ){
										if( y - y_previous > 1 ){
											if( eState == 6 ){
												int y_halfway = coordinates.ay0[ctPoints - 1] + (y - coordinates.ay0[ctPoints - 1]) / 2;
												for( int xY = coordinates.ay0[ctPoints - 1]; xY < y_halfway; xY++ ){
													coordinates.ax0[ctPoints] = x - 1;
													coordinates.ay0[ctPoints] = xY;
													ctPoints++;
												}
												for( int xY = y_halfway; xY < y; xY++ ){
													coordinates.ax0[ctPoints] = x;
													coordinates.ay0[ctPoints] = xY;
													ctPoints++;
												}
											} else {
												ctPoints += y - y_previous; 
											}
										} else if( y_previous - y > 1 ){ // need to interpolate points to make curve continuous
											if( eState == 6 ){
												int y_halfway = coordinates.ay0[ctPoints - 1] + (y - coordinates.ay0[ctPoints - 1]) / 2;
												for( int xY = coordinates.ay0[ctPoints - 1]; xY > y_halfway; xY-- ){
													coordinates.ax0[ctPoints] = x - 1;
													coordinates.ay0[ctPoints] = xY;
													ctPoints++;
												}
												for( int xY = y_halfway; xY > y; xY-- ){
													coordinates.ax0[ctPoints] = x;
													coordinates.ay0[ctPoints] = xY;
													ctPoints++;
												}
											} else {
												ctPoints += y_previous - y; 
											}
										}
									}
								}
								break;								
						}
					}
					if( eState == 1 || eState == 2 || eState == 3 || eState == 4 ){
						if( eState == 1 ){
							dIntervalY = (dEndY - dBeginY + 1.0d) / (double)iPlotHeight;
						} else if( eState == 2 ){
							dEndY = dBeginY + (iPlotHeight - 1) * dIntervalY;
						} else if( eState == 3 ){
							dBeginY = dEndY - (iPlotHeight - 1) * dIntervalY;
						} else { // eState == 4
							dIntervalY = (dEndY - dBeginY + 1.0d) / (double)iPlotHeight;
						}
						dRangeY = dEndY - dBeginY;
						eState = 5;
					} else if( eState == 5 ){
						coordinates.dBeginX = dBeginX; 
						coordinates.dEndX = dEndX; 
						coordinates.dBeginY = dBeginY; 
						coordinates.dEndY = dEndY; 
						coordinates.ctPoints = ctPoints;
						coordinates.ax0 = new int[ctPoints];
						coordinates.ay0 = new int[ctPoints];
						coordinates.aAlpha = new int[ctPoints];
						eState = 6;
					} else if( eState == 6 ) break; // done
				}
				break;
			case Polar:
			case Parametric:
		}
		return coordinates;
	}
}

class PlotCoordinates {
	boolean zHasAlpha = false;
	boolean zHasColor = false;
	int argbBaseColor = 0xFF000000; // solid black
	int ctPoints;
	int[] ax0;
	int[] ay0;
	int[] aAlpha;
	double dBeginX;
	double dEndX;
	double dBeginY;
	double dEndY;
	String sDump(){
		StringBuffer sb = new StringBuffer();
		sb.append( "zHasAlpha: " + zHasAlpha ).append( '\n' ); 
		sb.append( "zHasColor: " + zHasColor ).append( '\n' ); 
		sb.append( "argbBaseColor: " + Integer.toHexString( argbBaseColor ) ).append( '\n' ); 
		sb.append( "dBeginX: " + dBeginX ).append( '\n' ); 
		sb.append( "dEndX: " + dEndX ).append( '\n' ); 
		sb.append( "dBeginY: " + dBeginY ).append( '\n' ); 
		sb.append( "dEndY: " + dEndY ).append( '\n' );
		sb.append( "ctPoints: " + ctPoints ).append( '\n' );
		return sb.toString();
	}
	String sDump( int ctPointsToDump ){
		if( ctPointsToDump > ctPoints ) ctPointsToDump = ctPoints; 
		StringBuffer sb = new StringBuffer();
		sb.append( "zHasAlpha: " + zHasAlpha ).append( '\n' ); 
		sb.append( "zHasColor: " + zHasColor ).append( '\n' ); 
		sb.append( "argbBaseColor: " + Integer.toHexString( argbBaseColor ) ).append( '\n' ); 
		sb.append( "dBeginX: " + dBeginX ).append( '\n' ); 
		sb.append( "dEndX: " + dEndX ).append( '\n' ); 
		sb.append( "dBeginY: " + dBeginY ).append( '\n' ); 
		sb.append( "dEndY: " + dEndY ).append( '\n' );
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
	Identifier( String s, int pos ){
		this.s = s;
		this.pos = pos;
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

