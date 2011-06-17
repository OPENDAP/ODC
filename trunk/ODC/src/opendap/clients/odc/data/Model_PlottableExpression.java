package opendap.clients.odc.data;

import java.util.ArrayList;

import opendap.clients.odc.Utility_Array;
import opendap.clients.odc.Utility_String;


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
	final static Model_PlottableExpression create( Script script, StringBuffer sbError ){
		Model_PlottableExpression model = new Model_PlottableExpression();
		String sScriptText = script.getText();
		String sScriptText_escaped = Utility_String.sReplaceString( sScriptText, "$$", "$" );
		ArrayList<Identifier> listIdentifiers = gatherIdentifiers( sScriptText_escaped, sbError );
		String[] asTokens = { "$x", "$y", "$z", "$r", "$t", "$theta" };
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
			for( String sToken : asTokens ){
				if( identifier.s.equalsIgnoreCase( sToken ) ){
					zUsing$Tokens = true;
					break DollarCheck;
				}
			}
		}

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
		
		// make compiled objects
//		pycodeExpression = interpreter.getInterpeter().compile( sExpression_macroed ); // the expression must be precompiled for performance reasons
		
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
			if( pos == ctChars ) break;
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
	
	// [0][0] count of plot values
	//     
	int[][] getPlotValues(){
		return null;
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
}

