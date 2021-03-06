package opendap.clients.odc.data;

import java.util.ArrayList;
import java.util.HashMap;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DAP;
import opendap.clients.odc.Utility;
import opendap.clients.odc.Utility_Array;
import opendap.clients.odc.Utility_String;
import opendap.dap.DArray;
import opendap.dap.DGrid;
import opendap.dap.DStructure;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

public class Script {
	final public static boolean DEFAULT_allow_errors = false;
	final public static boolean DEFAULT_trace = false;
	private boolean zAllowErrors = DEFAULT_allow_errors;
	private boolean zTrace = DEFAULT_trace;
	private String msScriptText;
	private String msLine_trace;
	private String msRValue_trace;
	private java.io.OutputStream streamTrace;
	int[] size1 = new int[5]; // 1-based
	private HashMap<String,String> hmExp = new HashMap<String,String>();
	ArrayList<String> listGlobals = new ArrayList<String>();
	ArrayList<String> listPreIndexVariables = new ArrayList<String>();
	ArrayList<String> listPreValueVariables = new ArrayList<String>();
	int ctDimensions = 0;
	int trace_1_begin = 0;
	int trace_1_end = 0;
	int trace_2_begin = 0;
	int trace_2_end = 0;
	int trace_3_begin = 0;
	int trace_3_end = 0;
	int trace_4_begin = 0;
	int trace_4_end = 0;
	private Script(){
		String[] asKeyword = {
			"value", "type", "name", "allow_errors",
			"size_1", "size_2", "size_3", "size_4",
			"name_1", "name_2", "name_3", "name_4",
			"index_1", "index_2", "index_3", "index_4",
			"index_1_1_name", "index_1_2_name", "index_1_3_name",  
			"index_2_1_name", "index_2_2_name", "index_2_3_name",  
			"index_3_1_name", "index_3_2_name", "index_3_3_name",  
			"index_4_1_name", "index_4_2_name", "index_4_3_name", 
			"index_1_1_type", "index_1_2_type", "index_1_3_type",  
			"index_2_1_type", "index_2_2_type", "index_2_3_type",  
			"index_3_1_type", "index_3_2_type", "index_3_3_type",  
			"index_4_1_type", "index_4_2_type", "index_4_3_type", 
			"index_1_1", "index_1_2", "index_1_3",  
			"index_2_1", "index_2_2", "index_2_3",  
			"index_3_1", "index_3_2", "index_3_3",  
			"index_4_1", "index_4_2", "index_4_3"
		};
		for( int xKeyword = 0; xKeyword < asKeyword.length; xKeyword++ ){
			hmExp.put( asKeyword[xKeyword], null );
		}		
	}
	
	public String get( String sKey ){ return hmExp.get( sKey ); }
	
	public boolean getAllowErrors(){ return zAllowErrors; }
	public boolean getTrace(){ return zTrace; }
	public static Script create( String sScriptText, java.io.OutputStream streamTrace, StringBuffer sbError ){
		Script script = new Script();
		script.msScriptText = sScriptText;
		streamTrace = streamTrace;
		if( ! script.initialize( sbError ) ){
			sbError.insert( 0, "failed to initialize script: " );
			return null;
		}
		return script;
	}
	
	public String getText(){ return msScriptText; }
	
	private boolean initialize( StringBuffer sbError ){
		PythonInterpreter interpreter = ApplicationController.getInstance().getInterpreter().getInterpeter();
		if( msScriptText == null || msScriptText.trim().length() == 0 ){
			sbError.append( "expression text is blank" );
			return false;
		}
		ArrayList<String> listLines = Utility.zLoadLines( msScriptText, sbError );
		if( listLines == null ){
			sbError.insert( 0, "failed to parse script into lines of text: " );
			return false;
		}
		
		String sLine_allow_errors = null;
		String sLine_trace = null;
		String sRValue_trace = null; // this RValue is treated specially because of a non-standard syntax
		
		// process lines in the expression set
		for( int xLine = 1; xLine <= listLines.size(); xLine++ ){
			String sLine = listLines.get( xLine - 1 );
			if( sLine == null || sLine.trim().length() == 0 ) continue;
			sLine = sLine.trim();
			if( sLine.charAt( 0 ) == '#' ) continue; // comment
			int posEquals = sLine.indexOf( '=' );
			if( posEquals == -1 ){  // no equals sign
				if( sLine.startsWith( "trace" ) ){
					zTrace = true;
					sLine_trace = sLine;
					sRValue_trace = sLine.substring( "trace".length() ).trim();
				}
				continue;
			}
			String sLValue = sLine.substring( 0, posEquals ).trim();
			String sRValue = sLine.substring( posEquals + 1 ).trim();
			if( zTrace ){
				trace( "" ); // get off of command prompt
				try {
					String sMessage = String.format( "line %d L-value: [%s] R-value: [%s]\n", xLine, sLValue, sRValue );
					trace( sMessage );
				} catch( Throwable t ){}
			}
			if( ! zIsValidPythonIdentifier( sLValue, sbError ) ){
				sbError.insert( 0, "Identifier \"" + sLValue + "\" used as an L-value in line " + xLine + " is invalid: " );
				return false;
			}
			if( sLValue.startsWith( "__" ) ){
				listGlobals.add( sLine );
			} else if( sLValue.startsWith( "_" ) ){
				listPreIndexVariables.add( sLine );
			} else if( hmExp.containsKey( sLValue ) ){
				hmExp.put( sLValue, sRValue );
			} else {
				listPreValueVariables.add( sLine );
			}
		}
		
		// determine whether to trace
		if( sLine_trace == null || !zTrace  ){
			// do not trace
		} else {
			zTrace = true;
		}

		// The size variables are evaluated to determine the dimensions of the value array.
		for( int xDim = 1; xDim <= 4; xDim++ ){
			String sSizeIdentifier = "size_" + xDim;
			String sSizeIdentifier_value = hmExp.get( sSizeIdentifier ); 
			if( sSizeIdentifier_value == null ){
				// then default will be used
			} else {
				int iEvaluatedSize = evaluatePythonRValue_DimSize( sSizeIdentifier_value, xDim, streamTrace, zTrace, sbError );
				if( iEvaluatedSize == -1 ) return false;
				if( iEvaluatedSize == 0 ) { 
					if( xDim == 1 ){
						sbError.append( "dimension 1 evaluated to 0; must be 1 or greater" );
						return false;
					}
					break; // rest of sizes will be zero
				}
				ctDimensions++;
				size1[xDim] = iEvaluatedSize;
			}
		}

		// evaluate the trace intervals
		try {
			if( zTrace ){
				if( sRValue_trace != null && sRValue_trace.length() > 0 ){
					String[] asTrace = Utility_String.splitCommaWhiteSpace( sRValue_trace );
					int ctTrace = asTrace.length; 
					if( ctTrace != ctDimensions ){
						trace( "trace parameter (" + sRValue_trace + ") parsed to have " + ctTrace + " entries, but there are " + ctDimensions + ". If trace parameters are used they must match the number of dimensions. Tracing will now be turned off." );
						zTrace = false;
					} else {
						for( int xTraceParameter = 1; xTraceParameter <= ctTrace; xTraceParameter++ ){
							String sParameter = asTrace[xTraceParameter - 1];
							String[] asTraceRange = Utility_String.split( sParameter, ':' );
							if( asTraceRange.length > 2 ){
								trace( "trace parameter " + xTraceParameter + " (" + sParameter + ") parsed to have " + asTraceRange.length + " entries, but only 1 or 2 were expected. Trace parameters must be either 0-based numbers or ranges of index values, e.g., \"23:45\". Tracing will now be turned off." );
								zTrace = false;
								break;
							}
							int iParameter_begin = Utility_String.parseInteger_nonnegative( asTraceRange[0] );
							if( iParameter_begin == -1 ){
								trace( "trace parameter " + xTraceParameter + " (" + asTraceRange[0] + ") did not parse to a non-negative integer. Tracing will now be turned off." );
								zTrace = false;
								break;
							}
							int iParameter_end;
							if( asTraceRange.length == 2 ){
								iParameter_end = Utility_String.parseInteger_nonnegative( asTraceRange[1] );
								if( iParameter_begin == -1 ){
									trace( "trace parameter " + xTraceParameter + ", end part (" + asTraceRange[1] + ") did not parse to a non-negative integer. Tracing will now be turned off." );
									zTrace = false;
									break;
								}
							} else {
								iParameter_end = iParameter_begin; 
							}
							if( iParameter_end < iParameter_begin ){
								trace( "trace parameter " + xTraceParameter + " (" + sParameter+ ") is invalid because the begin value ( " + iParameter_begin + " ) is greater than the end value ( " + iParameter_end + " ). Tracing will now be turned off." );
								zTrace = false;
								break;
							}
							switch( xTraceParameter ){
								case 1:
									trace_1_begin = iParameter_begin;
									trace_1_end = iParameter_end;
									break;
								case 2:
									trace_2_begin = iParameter_begin;
									trace_2_end = iParameter_end;
									break;
								case 3:
									trace_3_begin = iParameter_begin;
									trace_3_end = iParameter_end;
									break;
								case 4:
									trace_4_begin = iParameter_begin;
									trace_4_end = iParameter_end;
									break;
							}
						}
					}
				} else { // trace every value
					trace_1_begin = 0;
					trace_1_end = size1[1] - 1; if( trace_1_end < 0 ) trace_1_end = 0;
					trace_2_begin = 0;
					trace_2_end = size1[2] - 1; if( trace_2_end < 0 ) trace_2_end = 0;
					trace_3_begin = 0;
					trace_3_end = size1[3] - 1; if( trace_3_end < 0 ) trace_3_end = 0;
					trace_4_begin = 0;
					trace_4_end = size1[4] - 1; if( trace_4_end < 0 ) trace_4_end = 0;
				}
			}
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, sbError );
			return false;
		}
		
		// determine whether to allow errors
		try {
			String sRValue_allow_errors = hmExp.get( "allow_errors" );
			if( sRValue_allow_errors == null ){
				// do nothing, use default value
			} else if( sRValue_allow_errors.equalsIgnoreCase( "true" ) ){
				zAllowErrors = true;
				if( zTrace )	trace( "allow_errors set to true" );
			} else if( sRValue_allow_errors.equalsIgnoreCase( "false" ) ){
				zAllowErrors = false;
				if( zTrace )	trace( "allow_errors set to false" );
			} else {
				try {
					interpreter.exec( sLine_allow_errors );
					PyObject po_allow_errors = interpreter.get( "allow_errors" );
					zAllowErrors = Py.py2boolean( po_allow_errors );
					if( zTrace )	trace( "allow_errors ( " + sLine_allow_errors + " ) evaluated to " + zAllowErrors );
				} catch( org.python.core.PySyntaxError parse_error ) {
					sbError.append( "Python syntax error processing allow_errors setting: " + parse_error );
					return false;
				} catch( org.python.core.PyException python_error ) {
					String sMessage = "Python error processing allow_errors setting: " + python_error;
					if( zTrace )	trace( sMessage );
					sbError.append( sMessage );
					return false;
				} catch( Throwable t ) {
					String sMessage = "unexpected error while evaluating allow_errors parameter (" + sLine_allow_errors + "): " + Utility.errorExtractLine( t );
					if( zTrace ){
						trace( sMessage );
					}
					sbError.append( sMessage );
					ApplicationController.vUnexpectedError( t, sbError );
					return false;
				}
			}
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "while determining whether to allow errors" );
			return false;
		}
		
		return true;
	}
	final private void trace( String sMessage ){
		try {
			streamTrace.write( sMessage.getBytes() );
			streamTrace.write( '\n' );
		} catch( Throwable t ) {}
	}
	
	final private static String[] aPythonReservedWords = {
		"and", "assert", "break", "class", "continue",
		"def", "del", "elif", "else", "except",
		"exec", "finally", "for", "from", "global",
		"if", "import", "in", "is", "lambda",
		"not", "or", "pass", "print", "raise",
		"return", "try", "while", "float", "int",
		"string"
	};
		
	final private boolean zIsValidPythonIdentifier( String sIdentifier, StringBuffer sbError ){
		if( sIdentifier == null || sIdentifier.trim().length() == 0 ){
			sbError.append( "identifier is blank" );
			return false;
		}
		if( Character.isDigit( sIdentifier.charAt(0) ) ){
			sbError.append( "begins with a digit" );
			return false;
		}
		if( ! Character.isLetter( sIdentifier.charAt(0) ) ){
			sbError.append( "does not begin with a letter" );
			return false;
		}
		for( int xChar = 0; xChar < sIdentifier.length(); xChar++ ){
			char c = sIdentifier.charAt( xChar );
			if( Character.isLetterOrDigit( c ) || c == '_' || c == '.' ) continue;
			sbError.append( "invalid character at position " + (xChar + 1) + ": " + c );
			return false;
		}
		if( Utility_Array.arrayHasMember( aPythonReservedWords, sIdentifier ) ){
			sbError.append( "is a reserved word in Python" );
			return false;
		}
		return true;
	}

	private int evaluatePythonRValue_DimSize( String sRValue, int iDimNumber, java.io.OutputStream streamTrace, boolean zTrace, StringBuffer sbError ){
		try {
			PythonInterpreter interpreter = ApplicationController.getInstance().getInterpreter().getInterpeter();
			PyObject po_DimensionSize = interpreter.eval( sRValue );
			int iDimensionSize_conversion = Py.py2int( po_DimensionSize );
			if( iDimensionSize_conversion < 0 ){
				String sMessage = "dimension size expression (" + sRValue + ") for dimension " + iDimNumber + " did not evaluate to a non-negative integer (" + iDimensionSize_conversion + ")";
				if( zTrace ){
					trace( sMessage );
				}
				sbError.append( sMessage );
				return -1;
			} else {
				trace( "Size for dimension " + iDimNumber + " ( size_" + iDimNumber + " ) evaluated to " + iDimensionSize_conversion );
				return iDimensionSize_conversion;
			}
		} catch( org.python.core.PySyntaxError parse_error ) {
			String sMessage = "Python syntax error while evaluating size_" + iDimNumber + " setting (" + sRValue + "): " + parse_error;
			if( zTrace ){
				trace( sMessage );
			}
			sbError.append( sMessage );
			return -1;
		} catch( org.python.core.PyException python_error ) {
			String sMessage = "Python error evaluating size_" + iDimNumber + " setting (" + sRValue + "): " + python_error;
			if( zTrace ){
				trace( sMessage );
			}
			sbError.append( sMessage );
			return -1;
		} catch( Throwable t ) {
			String sMessage = "error while processing size_" + iDimNumber + " setting (" + sRValue + "): " + Utility.errorExtractLine( t );
			if( zTrace ){
				trace( sMessage );
			}
			sbError.append( sMessage );
			return -1;
		}
	}

//		int[] actFilters1 = new int[10]; // the number of filters for this particular dimension
//		int[][] aiFilterBegin = new int[10][100]; // the begin of the filtered range, -1 means no contraint 
//		int[][] aiFilterEnd = new int[10][100]; // the begin of the filtered range, -1 means no contraint 

	public Model_Dataset generateDataset( StringBuffer sbError ){
		
		PythonInterpreter interpreter = ApplicationController.getInstance().getInterpreter().getInterpeter();
		
		//***** DEFAULTS *********************************************************
		Object DEFAULT_value = null;
		String DEFAULT_type = "Float64";
		int DEFAULT_size_1 = 100;
		int DEFAULT_size_2 = 0;
		int DEFAULT_size_3 = 0;
		int DEFAULT_size_4 = 0;
		Object DEFAULT_index_1 = null;
		Object DEFAULT_index_2 = null;
		Object DEFAULT_index_3 = null;
		Object DEFAULT_index_4 = null;
		String DEFAULT_name_1 = null;
		String DEFAULT_name_2 = null;
		String DEFAULT_name_3 = null;
		String DEFAULT_name_4 = null;
		int DEFAULT_index_dimensions = 1;

		//***********************************************************
		// Simple Array/Grid Generation
		//***********************************************************
		boolean allow_errors = getAllowErrors();
		boolean trace = getTrace();
//		int trace_1_begin = trace_1_begin;
//		int trace_1_end = trace_1_end;
//		int trace_2_begin = trace_2_begin;
//		int trace_2_end = trace_2_end;
//		int trace_3_begin = trace_3_begin;
//		int trace_3_end = trace_3_end;
//		int trace_4_begin = trace_4_begin;
//		int trace_4_end = trace_4_end;

		String value_RValue = null;
		DAP.DAP_TYPE type = DAP.DAP_TYPE.Float64;
		DAP.DAP_TYPE[][] index_type = new DAP.DAP_TYPE [5][4];  // variable dimension (1-based), index dimension (0 = primary)
		String sValueName = "value";
		String[][] name = new String[5][4];  // variable dimension (1-based), index dimension (0 = primary)

		//***********************************************************
		// Initialization
		//***********************************************************
		size1[1] = 100;
		for( int xVariable = 1; xVariable <= 4; xVariable++ )  // 0 not used
			for( int xIndex = 0; xIndex <= 3; xIndex++ )
				index_type[xVariable][xIndex] = DAP.DAP_TYPE.Float64;
		for( int xVariable = 1; xVariable <= 4; xVariable++ )  // 0 not used
			for( int xIndex = 0; xIndex <= 3; xIndex++ )
				name[xVariable][xIndex] = xIndex == 0 ? "dimension" + '_' + xVariable : "dim_" + xVariable + "index_" + xIndex;  
		
		//***********************************************************
		// Multi-vector Grid Support
		//***********************************************************
		int[] index_vectors = new int[5];  // 1-based, number of vectors used to represent the dimensional index
		String[][] index_RValue = new String[5][4]; // variable dimension (1-based), index dimension (0 = primary) 
		String[][] index_LValue = new String[5][4]; // variable dimension (1-based), index dimension (0 = primary)

		//***********************************************************
		// RValues
		//***********************************************************

		// obtain the RValue of the value
		value_RValue = get( "value" );
		if( value_RValue == null ){
			sbError.append( "no \"value\" parameter supplied; this parameter is required to define the values of the data array" );
			return null;
		}
		
		// (1) All unrecognized variables beginning with "__" (globals) are evaluated.
		try {
			for( int xGlobal = 1; xGlobal <= listGlobals.size(); xGlobal++ ){
				String sGlobal = listGlobals.get( xGlobal - 1 );
				try {
					interpreter.exec( sGlobal );
					if( trace ){
						String sGlobalLValue = Utility_String.getLValue( sGlobal );
						if( sGlobalLValue == null ){
							String sMessage = "global " + sGlobal + " evaluated";
							trace( sMessage );
						} else {
							PyObject po_global = interpreter.get( sGlobalLValue );
							String sMessage = "global " + sGlobal + " evaluated to: " + po_global.toString();
							trace( sMessage );
						}
					}
				} catch( org.python.core.PySyntaxError parse_error ) {
					String sMessage = "Python syntax error executing global ( " + sGlobal + " ): " + parse_error;
					if( trace ){
						trace( sMessage );
					}
					sbError.append( sMessage );
					if( !allow_errors ) return null;
				} catch( org.python.core.PyException python_error ) {
					String sMessage = "Python error executing global (" + sGlobal + "): " + python_error;
					if( trace ){
						trace( sMessage );
					}
					if( !allow_errors ){
						sbError.append( sMessage );
						return null;
					}
				} catch( Throwable t ) {
					String sMessage = "unexpected error while executing global (" + sGlobal + "): " + Utility.errorExtractLine( t );
					if( trace ){
						trace( sMessage );
					}
					if( !allow_errors ){
						sbError.append( sMessage );
						ApplicationController.vUnexpectedError( t, sbError );
						return null;
					}
				}
			}
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "while evaluating globals" );
			return null;
		}
		
		// (2) The configuration values (type, name, and index_dimensions) are evaluated first in that order.

		// determine the type of data
		String sExp_ValueType = get( "type" );
		if( sExp_ValueType == null ){
			// default will be used 
		} else {
			String sEvaluatedType = evaluatePythonRValue_String( "type", sExp_ValueType, streamTrace, trace, sbError );
			if( sEvaluatedType == null ){
				if( allow_errors ){
					// default will be used
				} else {
					sbError.insert( 0, "error evaluating value type: " );
					return null;
				}
			} else {
				DAP.DAP_TYPE eType =  DAP.getTypeEnumByName( sEvaluatedType );
				if( eType == null ){
					sbError.insert( 0, "unrecognized value type: " + sEvaluatedType );
					return null;
				} else {
					type = eType;
				}
			}
		}		

		// determine the name of the value array
		String sExp_ValueName = get( "name" ); 
		if( sExp_ValueName == null ){
			// default will be used 
		} else {
			String sEvaluatedName = evaluatePythonRValue_String( "name", sExp_ValueName, streamTrace, trace, sbError );
			if( sEvaluatedName == null ){
				if( allow_errors ){
					// default will be used
				} else {
					sbError.insert( 0, "error evaluating value name: " );
					return null;
				}
			} else {
				sValueName = sEvaluatedName;
			}
		}		
		
		// determine and validate the dimension count of the index vectors
		boolean[] azMultidimensionalIndex1 = new boolean[4]; // 1-based
		for( int xDim = 1; xDim <= 4; xDim++ ){
			if( get( "index_" + xDim + "_3" ) != null ){
				index_vectors[xDim] = 3;
				azMultidimensionalIndex1[xDim] = true;
			} else if( get( "index_" + xDim + "_2" ) != null ){
				index_vectors[xDim] = 2;
				azMultidimensionalIndex1[xDim] = true;
			} else if( get( "index_" + xDim + "_1" ) != null ){
				index_vectors[xDim] = 1;
				azMultidimensionalIndex1[xDim] = true;
			}
			if( get( "index_" + xDim ) != null ){
				if( azMultidimensionalIndex1[xDim] ){
					sbError.append( "indexing conflict, both multidimensional and unidimensional indices are defined for dimension " + xDim );
					return null;
				}
				index_vectors[xDim] = 1;
			}
		}
		
		// evaluate the dimensional names
		for( int xDim = 1; xDim <= 4; xDim++ ){
			String sVarName = "name_" + xDim;
			String sExp_VarName = get( sVarName ); 
			if( sExp_VarName == null ){
				// default will be used 
			} else {
				String sEvaluatedName = evaluatePythonRValue_DimName( sExp_VarName, xDim, streamTrace, trace, sbError );
				if( sEvaluatedName == null ){
					if( allow_errors ){
						// default will be used
					} else {
						sbError.insert( 0, "failed to evaluate dimension " + xDim + " name: " );
						return null;
					}
				} else {
					name[xDim][0] = sEvaluatedName;
				}
			}
		}

		// evaluate the index vector names
		for( int xDim = 1; xDim <= 4; xDim++ ){
			for( int xVector = 1; xVector <= 3; xVector++ ){
				String sVarName = "index_" + xDim + "_" + xVector + "_name";
				String sExp_VarName = get( sVarName ); 
				if( sExp_VarName == null ){
					// default will be used 
				} else {
					String sEvaluatedName = evaluatePythonRValue_DimName( sExp_VarName, xDim, streamTrace, trace, sbError );
					if( sEvaluatedName == null ){
						if( allow_errors ){
							// default will be used
						} else {
							sbError.insert( 0, "failed to evaluate dimension " + xDim + " vector name: " );
							return null;
						}
					} else {
						name[xDim][xVector] = sEvaluatedName;
					}
				}
			}
		}

		// isolate the index LValues and RValues
		for( int xDim = 1; xDim <= 4; xDim++ ){
			for( int xVector = 0; xVector <= 3; xVector++ ){
				index_LValue[xDim][xVector] = xVector == 0 ? "index_" + xDim : "index_" + xDim + "_" + xVector;
				index_RValue[xDim][xVector] = get( index_LValue[xDim][xVector] ); 
			}
		}		

		// make sure we have enough memory
		int iTotalSize = size1[1];
		if( size1[2] > 0 ){
			if( Integer.MAX_VALUE / size1[2] > iTotalSize ){
				sbError.append( "dimensional sizes (" + size1[1] + " and " + size1[2] + ") result in an oversized array" );
				return null;
			} else {
				iTotalSize *= size1[2];
				if( size1[3] > 0 ){
					if( Integer.MAX_VALUE / size1[3] > iTotalSize ){
						sbError.append( "dimensional sizes (" + size1[1] + " and " + size1[2] + " and " + size1[3] + ") result in an oversized array" );
						return null;
					} else {
						iTotalSize *= size1[3];
						if( size1[4] > 0 ){
							if( Integer.MAX_VALUE / size1[4] > iTotalSize ){
								sbError.append( "dimensional sizes (" + size1[1] + " and " + size1[2] + " and " + size1[3] + " and " + size1[4] + ") result in an oversized array" );
								return null;
							}
						} else {
							iTotalSize *= size1[4];
						}
					}
				}
			}
		}
		if( ! Utility.zMemoryCheck( iTotalSize, DAP.getDataSize( type ), sbError ) ){
			sbError.append( "projected array of size " + iTotalSize + " and type " + DAP.getType_String( type ) + " is too large to be contained in available memory. See help for information on increasing memory allocation." );
			return null;
		}
		
		// allocate the data array
		PyPrimitiveVector pvValue = new PyPrimitiveVector( type, size1[1], size1[2], size1[3], size1[4] ); // dimension (1-based) x vector (0-based) 
		
		// allocate the primary dimensional indices
		PyPrimitiveVector[][] pvIndex = new PyPrimitiveVector[5][4]; // dimension (1-based) x vector (0-based) 
		for( int xDim = 1; xDim <= ctDimensions; xDim++ ){
			int ctVectors = azMultidimensionalIndex1[xDim] ? index_vectors[xDim] : 1;
			for( int xVector = 1; xVector <= ctVectors; xVector++ ){
				DAP.DAP_TYPE eTYPE = azMultidimensionalIndex1[xDim] ? index_type[xDim][xVector] : index_type[xDim][0];
				int xVectorIndex = azMultidimensionalIndex1[xDim] ? xVector : 0;
				pvIndex[xDim][xVectorIndex] = new PyPrimitiveVector( eTYPE, size1[xDim], 0, 0, 0 );
			}
		}	

		// (4) The internal index values are looped.
//		ArrayList<String> listPreValueVariables = new ArrayList<String>();
		int x1 = 0;
		int x2 = 0;
		int x3 = 0;
		int x4 = 0;
		while( true ){
			if( x1 > size1[1] || x2 > size1[2] || x3 > size1[3] || x4 > size1[4] ) break; // done
			boolean zTraceLoop =
				trace 	&& x1 >= trace_1_begin && x1 <= trace_1_end  
						&& x2 >= trace_2_begin && x2 <= trace_2_end 
						&& x3 >= trace_3_begin && x3 <= trace_3_end 
						&& x4 >= trace_4_begin && x4 <= trace_4_end;

			// (5) All unrecognized variables beginning with "_" are evaluated after macro substitution.
			for( int xPreIndexExp = 1; xPreIndexExp <= listPreIndexVariables.size(); xPreIndexExp++ ){
				String sPreIndexExp = listPreIndexVariables.get( xPreIndexExp - 1 );
				if( sPreIndexExp == null ){
					sbError.append( String.format( "internal error on loop %d %d %d %d, pre-index expression %d was unexpectedly null", x1, x2, x3, x4, xPreIndexExp) );
					return null;
				}
				String sPreIndexExp_after_macro = sMacroSubstitution( sPreIndexExp, x1, x2, x3, x4 ); 
				if( zTraceLoop && ! sPreIndexExp.equals( sPreIndexExp_after_macro ) ){ // then there was a change
					trace( "pre-index expression " + xPreIndexExp + " (" + sPreIndexExp + ") after macro substitution: " + sPreIndexExp_after_macro );
				}
				try {
					interpreter.exec( sPreIndexExp_after_macro );
				} catch( org.python.core.PySyntaxError parse_error ) {
					sbError.append( "Python syntax error executing pre-index statement (" + sPreIndexExp_after_macro + ": " + parse_error );
					return null;
				} catch( org.python.core.PyException python_error ) {
					sbError.append( "Python error executing pre-index statement (" + sPreIndexExp_after_macro + "): " + python_error );
				} catch( Throwable t ) {
					sbError.append( "while executing pre-index statement " + sPreIndexExp_after_macro );
					ApplicationController.vUnexpectedError( t, sbError );
					return null;
				}
			}

			// (6) The index variables are evaluated.
			for( int xDim = 1; xDim <= ctDimensions; xDim++ ){
				int ctVectors = index_vectors[xDim]; // if there are no indices for this dim then this will be 0 and looping will continue
				for( int xVector = 1; xVector <= ctVectors; xVector++ ){
					int xIndex = azMultidimensionalIndex1[xDim] ? xVector : 0;
					String sRValue = index_RValue[xDim][xIndex];
					if( sRValue == null ){
						sbError.append( String.format( "internal error on loop %d %d %d %d, index expression %d %d was unexpectedly null", x1, x2, x3, x4, xDim, azMultidimensionalIndex1[xDim] ? xVector: 0) );
						return null;
					}
					String sRValue_after_macro = sMacroSubstitution( sRValue, x1, x2, x3, x4 );
					if( zTraceLoop && ! sRValue.equals( sRValue_after_macro ) ){ // then there was a change
						trace( "index expression " + index_LValue[xDim][xIndex] + " (" + sRValue + ") after macro substitution: " + sRValue_after_macro );
					}
					try {
						PyObject po_IndexValue = interpreter.eval( sRValue_after_macro );
						int xMember;
						switch( xDim ){
							case 1: xMember = x1; break;
							case 2: xMember = x2; break;
							case 3: xMember = x3; break;
							case 4: xMember = x4; break;
							default:
								xMember = 0;
						}
						if( pvIndex[xDim][xIndex].set( xMember, 0, 0, 0, po_IndexValue, sbError ) ){
							if( zTraceLoop ){
								String sMessage = String.format( "value %d written for index, dim: %d, vector: %d, member: %d" , pvIndex[xDim][xIndex].getValue( xMember, 0, 0, 0 ), xDim, xIndex, xMember );
								trace( sMessage );
							}
						} else {
							sbError.insert( 0, String.format( "error setting value %s for index, dim: %d, vector: %d, member: %d: " , po_IndexValue.toString(), xDim, xIndex, xMember ) ); 
							if( zTraceLoop ){
								trace( sbError.toString() );
							}
							if( ! allow_errors ) return null;
						}
						
					} catch( org.python.core.PySyntaxError parse_error ) {
						String sMessage = "Python syntax error while evaluating " + index_LValue[xDim][xIndex] + " setting (" + sRValue_after_macro + "): " + parse_error;
						if( zTraceLoop ){
							trace( sMessage );
						}
						sbError.append( sMessage );
						return null;
					} catch( org.python.core.PyException python_error ) {
						String sMessage = "Python error evaluating " + index_LValue[xDim][xIndex] + " setting (" + sRValue_after_macro + "): " + python_error;
						if( zTraceLoop ){
							trace( sMessage);
						}
						sbError.append( sMessage );
						return null;
					} catch( Throwable t ) {
						String sMessage = "error while processing " + index_LValue[xDim][xIndex] + " setting (" + sRValue_after_macro + "): " + Utility.errorExtractLine( t );
						if( zTraceLoop ){
							trace( sMessage );
						}
						sbError.append( sMessage );
						return null;
					}
				}
			}

			// (7) All unrecognized variables not beginning with "_" are evaluated after macro substitution.
			for( int xPreValueExp = 1; xPreValueExp <= listPreValueVariables.size(); xPreValueExp++ ){
				String sPreValueExp = listPreValueVariables.get( xPreValueExp - 1 );
				if( sPreValueExp == null ){
					sbError.append( String.format( "internal error on loop %d %d %d %d, pre-value expression %d was unexpectedly null", x1, x2, x3, x4, xPreValueExp ) );
					return null;
				}
				String sPreValueExp_after_macro = sMacroSubstitution( sPreValueExp, x1, x2, x3, x4 ); 
				if( zTraceLoop && ! sPreValueExp.equals( sPreValueExp_after_macro ) ){ // then there was a change
					trace( "pre-value expression " + xPreValueExp + " (" + sPreValueExp + ") after macro substitution: " + sPreValueExp_after_macro );
				}
				try {
					interpreter.exec( sPreValueExp_after_macro + '\n' );
				} catch( org.python.core.PySyntaxError parse_error ) {
					sbError.append( "Python syntax error executing pre-value statement (" + sPreValueExp_after_macro + "): " + parse_error );
					return null;
				} catch( org.python.core.PyException python_error ) {
					sbError.append( "Python error executing pre-value statement (" + sPreValueExp_after_macro + "): " + python_error );
				} catch( Throwable t ) {
					sbError.append( "while executing pre-value statement " + sPreValueExp_after_macro );
					ApplicationController.vUnexpectedError( t, sbError );
					return null;
				}
			}

			// (8) The value variable is evaluated.
			if( value_RValue == null ){
				sbError.append( String.format( "internal error on loop %d %d %d %d, value expression was unexpectedly null", x1, x2, x3, x4 ) );
				return null;
			}
			String value_RValue_after_macro = sMacroSubstitution( value_RValue, x1, x2, x3, x4 );
			if( zTraceLoop && ! value_RValue.equals( value_RValue_after_macro ) ){ // then there was a change
				trace( "value expression (" + value_RValue + ") after macro substitution: " + value_RValue_after_macro );
			}
			try {
				PyObject po_IndexValue = interpreter.eval( value_RValue_after_macro );
				if( pvValue.set( x1, x2, x3, x4, po_IndexValue, sbError ) ){
					if( zTraceLoop ){
						String sMessage = String.format( "value %s written for member [%d][%d][%d][%d]" , pvValue.getValue( x1, x2, x3, x4 ), x1, x2, x3, x4 );
						trace( sMessage );
					}
				} else {
					sbError.insert( 0, String.format( "error setting value %s for member [%d][%d][%d][%d]: " , po_IndexValue.toString(), x1, x2, x3, x4 ) );
					if( zTraceLoop ){
						trace( sbError.toString() );
					}
					if( ! allow_errors ) return null;
				}
				
			} catch( org.python.core.PySyntaxError parse_error ) {
				String sMessage = "Python syntax error while evaluating " + value_RValue + " setting (" + value_RValue_after_macro + "): " + parse_error;
				if( zTraceLoop ){
					trace( sMessage);
				}
				sbError.append( sMessage );
				return null;
			} catch( org.python.core.PyException python_error ) {
				String sMessage = "Python error evaluating " + value_RValue + " setting (" + value_RValue_after_macro + "): " + python_error;
				if( zTraceLoop ){
					trace( sMessage );
				}
				sbError.append( sMessage );
				return null;
			} catch( Throwable t ) {
				String sMessage = "error while processing " + value_RValue + " setting (" + value_RValue_after_macro + "): " + Utility.errorExtractLine( t );
				if( zTraceLoop ){
					trace( sMessage );
				}
				sbError.append( sMessage );
				return null;
			}

			// (9) The process increments counters and loops to step (3), and continuing until the value matrix is full.
			x1++;
			if( x1 == size1[1] ){
				x1 = 0;
				x2++;
				if( x2 == size1[2] ){
					x2 = 0;
					x3++;
					if( x3 == size1[3] ){
						x3 = 0;
						x4++;
					}}}
		}

		// create the Data DDS
		opendap.dap.DataDDS datadds = null;
		try {
			String sServerVersion = "2.1.5";
			int iHeaderType = opendap.dap.ServerVersion.XDAP; // this is the simple version (eg "2.1.5"), server version strings are like "XDODS/2.1.5", these are only used in HTTP, not in files
			opendap.dap.ServerVersion server_version = new opendap.dap.ServerVersion( sServerVersion, iHeaderType );
			datadds = new opendap.dap.DataDDS( server_version );
			opendap.dap.DefaultFactory factory = new opendap.dap.DefaultFactory();
			StringBuffer sbDDS = new StringBuffer(1000);
			
			// create the value array
			DArray darray = createDArray( factory, sValueName, type, pvValue.getInternalStorage(), sbError );
			if( darray == null ){
				sbError.insert( 0, String.format( "error creating DArray for value \"%s\" of type %s: ", sValueName, DAP.getType_String( type ) ) );
				return null;
			}
			
			// add dimensions to array
			boolean zIndexPresent = false;
			int iSizeOfValueArray = 1;
			for( int xDim = 1; xDim <= 4; xDim++ ){
				if( size1[xDim] == 0 ) break;
				String sDimensionName = name[xDim][0];
				if( sDimensionName == null ){
					darray.appendDim( size1[xDim] );
				} else {
					darray.appendDim( size1[xDim], sDimensionName, false );
				}
				iSizeOfValueArray *= size1[xDim]; 
				if( index_vectors[xDim] > 0 ) zIndexPresent = true; 
			}			
			
			// validate dimensions
			if( pvValue.getTotalSize() != iSizeOfValueArray ){
				sbError.append( "internal error, data vector size " + pvValue.getTotalSize() + " does not match dim size " + iSizeOfValueArray );
				return null;
			}
			
			// check to see if we are creating a grid
			DGrid dgrid = null;
			if( zIndexPresent ){  // then we are creating a grid, otherwise it will be a plain array
				dgrid = factory.newDGrid();
				dgrid.addVariable( darray, DGrid.ARRAY );
				for( int xDim = 1; xDim <= 4; xDim++ ){
					if( size1[xDim] == 0 ) break;
					String sDimensionName = name[xDim][0];
					if( index_vectors[xDim] == 1 ){ // simple index vector
						DAP.DAP_TYPE daptypeDim = index_type[xDim][0];
						DArray darrayDimIndex = createDArray( factory, sDimensionName, daptypeDim, pvIndex[xDim][0].getInternalStorage(), sbError );
						if( darray == null ){
							sbError.insert( 0, String.format( "error creating DArray for dimension %s of type %s: ", sDimensionName, DAP.getType_String( daptypeDim ) ) );
							return null;
						}
						dgrid.addVariable( darrayDimIndex, DGrid.MAPS );
					} else if( index_vectors[xDim] > 1 ){ // multi-vector index
						DStructure dstructure = factory.newDStructure( sDimensionName );
						for( int xVector = 1; xVector <= index_vectors[xDim]; xVector++ ){
							DAP.DAP_TYPE daptypeDim = index_type[xDim][xVector];
							String sVectorName = name[xDim][xVector];
							Object oInternalStorage = pvIndex[xDim][xVector].getInternalStorage();
							DArray darrayDimIndex = createDArray( factory, sVectorName, daptypeDim, oInternalStorage, sbError );
							if( darray == null ){
								sbError.insert( 0, String.format( "error creating DArray for grid vector %s of type %s: ", sVectorName, DAP.getType_String( daptypeDim ) ) );
								return null;
							}
							dstructure.addVariable( darrayDimIndex );
						}
						dgrid.addVariable( dstructure, DGrid.MAPS );
					}
				}
			} else {
				datadds.addVariable( darray ); // creating a plain array
			}
			
			// add the DDS to the dataset
			System.out.println( "dds text: " + datadds.getDDSText() );
			
		} catch( Throwable t ) {
			sbError.append( "while creating Data DDS" );
			ApplicationController.vUnexpectedError( t, sbError );
			return null;
		}

		// create the model
		Model_Dataset model = Model_Dataset.createData( datadds, sbError );
		if( model == null ){
			sbError.insert( 0, "error creating model for Data DDS: " );
			return null;
		}
		
		return model;
	}

	private DArray createDArray( opendap.dap.BaseTypeFactory factory, String sName, DAP.DAP_TYPE type, Object oInternalStorage, StringBuffer sbError ){
		try {
			DArray darray = factory.newDArray();
			switch( type ){
				case Byte:
					darray.addVariable( new opendap.dap.DByte() );
					break;
				case Int16:
					darray.addVariable( new opendap.dap.DInt16() );
					break;
				case Int32:
					darray.addVariable( new opendap.dap.DInt32() );
					break;
				case UInt16:
					darray.addVariable( new opendap.dap.DUInt16() );
					break;
				case UInt32:
					darray.addVariable( new opendap.dap.DUInt32() );
					break;
				case Float32:
					darray.addVariable( new opendap.dap.DFloat32() );
					break;
				case Float64:
					darray.addVariable( new opendap.dap.DFloat64() );
					break;
				case String:
					darray.addVariable( new opendap.dap.DString() );
					break;
				default:
					sbError.append( "unknown variable type: " + type );
					return null;
			}
			darray.setName( sName );
			darray.getPrimitiveVector().setInternalStorage( oInternalStorage );
			return darray;
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, sbError );
			return null;
		}
	}
	
	// this method uses $$ to escape a dollar sign
	StringBuffer sbLiteral$ = new StringBuffer();
	private String sMacroSubstitution( String sExp, int x1, int x2, int x3, int x4 ){
		String sExp_after_macro = sExp;
		boolean zUsingEscape = false;
		if( sExp.contains( "$$" ) ){
			sbLiteral$.setLength(0);
			while( true ){ // determine a unique string to substitute for a literal dollar sign in case of a double escape
				sbLiteral$.append('~');
				if( sExp.contains(sbLiteral$) ) break;
			}
			Utility_String.sReplaceString( sExp_after_macro, "$$", sbLiteral$.toString() );
			zUsingEscape = true;
		}
		sExp_after_macro = Utility_String.sReplaceString( sExp, "$1", Integer.toString( x1 ) );
		sExp_after_macro = Utility_String.sReplaceString( sExp_after_macro, "$2", Integer.toString( x2 ) );
		sExp_after_macro = Utility_String.sReplaceString( sExp_after_macro, "$3", Integer.toString( x3 ) );
		sExp_after_macro = Utility_String.sReplaceString( sExp_after_macro, "$4", Integer.toString( x4 ) );
		if( zUsingEscape )	sExp_after_macro = Utility_String.sReplaceString( sExp_after_macro, sbLiteral$.toString(), "$" );
		return sExp_after_macro;
	}
	
	private int evaluatePythonRValue_DimCount( String sRValue, int iDimNumber, java.io.OutputStream streamTrace, boolean zTrace, StringBuffer sbError ){
		try {
			PythonInterpreter interpreter = ApplicationController.getInstance().getInterpreter().getInterpeter();
			PyObject po_IndexDimensionCount = interpreter.eval( sRValue );
			int iIndexDimensionCount_conversion = Py.py2int( po_IndexDimensionCount );
			if( iIndexDimensionCount_conversion < 0 || iIndexDimensionCount_conversion > 4 ){
				String sMessage = "index dimension count expression (" + sRValue + ") for dimension " + iDimNumber + " did not evaluate to a valid integer between 1 and 4";
				if( zTrace ){
					trace( sMessage );
				}
				sbError.append( sMessage );
				return -1;
			} else {
				trace( "Dimension count for index variable " + iDimNumber + " ( index_" + iDimNumber + "_dimension ) evaluated to have " + iIndexDimensionCount_conversion + " dimensions." );
				return iIndexDimensionCount_conversion;
			}
		} catch( org.python.core.PySyntaxError parse_error ) {
			String sMessage = "Python syntax error while evaluating index_" + iDimNumber + "_dimensions setting (" + sRValue + "): " + parse_error;
			if( zTrace ){
				trace( sMessage );
			}
			sbError.append( sMessage );
			return -1;
		} catch( org.python.core.PyException python_error ) {
			String sMessage = "Python error evaluating index_" + iDimNumber + "_dimensions setting (" + sRValue + "): " + python_error;
			if( zTrace ){
				trace( sMessage );
			}
			sbError.append( sMessage );
			return -1;
		} catch( Throwable t ) {
			String sMessage = "error while processing index_" + iDimNumber + "_dimensions setting (" + sRValue + "): " + Utility.errorExtractLine( t );
			if( zTrace ){
				trace( sMessage );
			}
			sbError.append( sMessage );
			return -1;
		}
	}

	private int evaluatePythonRValue_TraceParameter( String sRValue, String sParameterName, java.io.OutputStream streamTrace, boolean zTrace, StringBuffer sbError ){
		try {
			PythonInterpreter interpreter = ApplicationController.getInstance().getInterpreter().getInterpeter();
			PyObject po_Parameter = interpreter.eval( sRValue );
			int iParameter = Py.py2int( po_Parameter );
			if( iParameter < 0 ){
				String sMessage = sParameterName + " expression (" + sRValue + ") did not evaluate to a non-negative integer (" + iParameter + ")";
				if( zTrace ){
					trace( sMessage );
				}
				sbError.append( sMessage );
				return -1;
			} else {
				trace( "Parameter " + sParameterName + " evaluated to " + iParameter );
				return iParameter;
			}
		} catch( org.python.core.PySyntaxError parse_error ) {
			String sMessage = "Python syntax error while evaluating " + sParameterName + " setting (" + sRValue + "): " + parse_error;
			if( zTrace ){
				try { trace( sMessage ); } catch( Throwable t ) {}
			}
			sbError.append( sMessage );
			return -1;
		} catch( org.python.core.PyException python_error ) {
			String sMessage = "Python error evaluating " + sParameterName + " setting (" + sRValue + "): " + python_error;
			if( zTrace ){
				try { trace( sMessage ); } catch( Throwable t ) {}
			}
			sbError.append( sMessage );
			return -1;
		} catch( Throwable t ) {
			String sMessage = "error while processing " + sParameterName + " setting (" + sRValue + "): " + Utility.errorExtractLine( t );
			if( zTrace ){
				try { trace( sMessage ); } catch( Throwable trace_t ) {}
			}
			sbError.append( sMessage );
			return -1;
		}
	}

	private String evaluatePythonRValue_String( String sLValue, String sRValue, java.io.OutputStream streamTrace, boolean zTrace, StringBuffer sbError ){
		try {
			PythonInterpreter interpreter = ApplicationController.getInstance().getInterpreter().getInterpeter();
			PyObject po_String = interpreter.eval( sRValue );
			String sString = po_String.toString();
			if( DAP.isValidIdentifier( sString, sbError) ){
				trace( "String variable \"" + sLValue + "\" evaluated to \"" + sString + "\"" );
				return sString;
			} else {
				sbError.insert( 0, "String expression (" + sLValue + ") evaluated to \"" + sString + "\" which not a valid identifier: " );
				if( zTrace ){
					trace( sbError.toString() );
				}
				return null;
			}
		} catch( org.python.core.PySyntaxError parse_error ) {
			String sMessage = "Python syntax error while evaluating setting (" + sRValue + "): " + parse_error;
			if( zTrace ){
				try { trace( sMessage ); } catch( Throwable t ) {}
			}
			sbError.append( sMessage );
			return null;
		} catch( org.python.core.PyException python_error ) {
			String sMessage = "Python error evaluating setting (" + sRValue + "): " + python_error;
			if( zTrace ){
				try { trace( sMessage ); } catch( Throwable t ) {}
			}
			sbError.append( sMessage );
			return null;
		} catch( Throwable t ) {
			String sMessage = "error while processing setting (" + sRValue + "): " + Utility.errorExtractLine( t );
			if( zTrace ){
				try { trace( sMessage ); } catch( Throwable trace_t ) {}
			}
			sbError.append( sMessage );
			return null;
		}
	}
	
	private String evaluatePythonRValue_DimName( String sRValue, int iDimNumber, java.io.OutputStream streamTrace, boolean zTrace, StringBuffer sbError ){
		try {
			PythonInterpreter interpreter = ApplicationController.getInstance().getInterpreter().getInterpeter();
			PyObject po_DimensionName = interpreter.eval( sRValue );
			String sDimensionsName = po_DimensionName.toString();
			if( DAP.isValidIdentifier( sDimensionsName, sbError) ){
				trace( "Dimension " + iDimNumber + " name variable ( name_" + iDimNumber + " ) evaluated \"" + sDimensionsName + "\"" );
				return sDimensionsName;
			} else {
				sbError.insert( 0, "dimension name expression (" + sRValue + ") for dimension " + iDimNumber + " did not evaluate to a valid identifier: " );
				if( zTrace ){
					trace( sbError.toString() );
				}
				return null;
			}
		} catch( org.python.core.PySyntaxError parse_error ) {
			String sMessage = "Python syntax error while evaluating dimension name_" + iDimNumber + " setting (" + sRValue + "): " + parse_error;
			if( zTrace ){
				try { trace( sMessage ); } catch( Throwable t ) {}
			}
			sbError.append( sMessage );
			return null;
		} catch( org.python.core.PyException python_error ) {
			String sMessage = "Python error evaluating dimension name_" + iDimNumber + " setting (" + sRValue + "): " + python_error;
			if( zTrace ){
				try { trace( sMessage ); } catch( Throwable t ) {}
			}
			sbError.append( sMessage );
			return null;
		} catch( Throwable t ) {
			String sMessage = "error while processing dimension name_" + iDimNumber + " setting (" + sRValue + "): " + Utility.errorExtractLine( t );
			if( zTrace ){
				try { trace( sMessage ); } catch( Throwable trace_t ) {}
			}
			sbError.append( sMessage );
			return null;
		}
	}
	
}

class PyPrimitiveVector {
	
	final public static int ERROR_VALUE_Byte = 0xFF;
	final public static int ERROR_VALUE_Int16 = -32768;
	final public static int ERROR_VALUE_UInt16 = 0xFFFF; // 65535
	final public static int ERROR_VALUE_UInt32 = 0xFFFFFFFF; // 65535
	
	int[] aiVector = null;
	long[] anVector = null;
	float[] afVector = null;
	double[] adVector = null;
	String[] asVector = null;
	int iDim1, iDim2, iDim3, iDim4; // dimension lengths
	int iTotalSize = 0;

	private DAP.DAP_TYPE eTYPE;

	public PyPrimitiveVector( DAP.DAP_TYPE type, int lenDim1, int lenDim2, int lenDim3, int lenDim4 ){
		eTYPE = type;
		iDim1 = lenDim1;
		iDim2 = lenDim2;
		iDim3 = lenDim3;
		iDim4 = lenDim4;
		iTotalSize = iDim1 * ( iDim2 > 0 ? iDim2 * ( iDim3 > 0 ? iDim3 * ( iDim4 > 0 ? iDim4 : 1 ) : 1 ): 1 );
		switch( eTYPE ){
			case Byte:
			case Int16:
			case Int32:
			case UInt16:
				aiVector = new int[iTotalSize];
				break;
			case UInt32:
				anVector = new long[iTotalSize];
				break;
			case Float32:
				afVector = new float[iTotalSize];
				break;
			case Float64:
				adVector = new double[iTotalSize];
				break;
			case String:
				asVector = new String[iTotalSize];
				break;
			default:
				return ;
		}
	}
	
	public int getTotalSize(){ return iTotalSize; }

	// generates an object suitable for internal storage in OPeNDAP
	public Object getInternalStorage(){
		switch( eTYPE ){
			case Byte:
			case Int16:
			case Int32:
				return aiVector;
			case UInt16:
			case UInt32:
			case Float32:
				return afVector;
			case Float64:
				return adVector;
			case String:
				return asVector;
			default:
				return null;
		}
	}

	public boolean set( int xDim1, int xDim2, int xDim3, int xDim4, PyObject pyValue, StringBuffer sbError ){
		int xArray = xDim1 + iDim1 * xDim2 + iDim1 * iDim2 * xDim3 + iDim1 * iDim2 * iDim3 * xDim4;
		if( xArray >= iTotalSize ){
			sbError.append( String.format( "array size %d exceeded by index coordinates %d %d %d %d", iTotalSize, xDim1, xDim2, xDim3, xDim4 ) );
			return false;
		}
		try {
			switch( eTYPE ){
				case Byte:
				case Int16:
				case Int32:
				case UInt16:
					int iValue = Py.py2int( pyValue );
					return set( xArray, iValue, sbError );
				case UInt32:
					long nValue = Py.py2long( pyValue );
					return set( xArray, nValue, sbError );
				case Float32:
					float fValue = Py.py2float( pyValue );
					return set( xArray, fValue, sbError );
				case Float64:
					double dValue = Py.py2double( pyValue );
					return set( xArray, dValue, sbError );
				case String:
					String sValue = pyValue.toString();
					return set( xArray, sValue, sbError );
				default:
					sbError.append( "unknown data type" );
					return false;
			}
		} catch( Throwable t ) {
			sbError.append( "error coercing Jython object to Java value" );
			return false;
		}
	}
	
	public boolean set( int xArray, int iValue, StringBuffer sbError ){
		switch( eTYPE ){
			case Byte:
				if( iValue < 0 ){ 
					sbError.append( String.format( "value %d is out of range for byte type which is unsigned", iValue ) ); 
					aiVector[xArray] = ERROR_VALUE_Byte;
					return false; } 
				if( iValue > 255 ){ 
					sbError.append( String.format( "value %d is out of range for byte type which has maximum value of 255", iValue ) ); 
					aiVector[xArray] = ERROR_VALUE_Byte;
					return false; } 
				aiVector[xArray] = iValue;
				return true;
			case Int16:
				if( iValue < -32768 || iValue > 32767 ){ 
					sbError.append( String.format( "value %d is out of range for Int16 type (-32768 to 32767)", iValue ) ); 
					aiVector[xArray] = ERROR_VALUE_Int16;
					return false; } 
				aiVector[xArray] = iValue;
				return true;
			case Int32:
				aiVector[xArray] = iValue;
				return true;
			case UInt16:
				if( iValue < 0 || iValue > 65535 ){ 
					sbError.append( String.format( "value %d is out of range for UInt16 type (0 to 65535)", iValue ) ); 
					aiVector[xArray] = ERROR_VALUE_UInt16;
					return false; }
				aiVector[xArray] = iValue;
				return true;
			case UInt32:
				sbError.append( "internal error, attempt to cast an int to a long" );
				anVector[xArray] = Long.MIN_VALUE;
				return false;
			case Float32:
				sbError.append( "internal error, attempt to cast an int to a Float32" );
				afVector[xArray] = Float.NaN;
				return false;
			case Float64:
				sbError.append( "internal error, attempt to cast an int to a Float64" );
				adVector[xArray] = Double.NaN;
				return false;
			case String:
				asVector[xArray] = Integer.toString( iValue );
				return true;
			default:
				sbError.append( "unknown data type" );
				return false;
		}
	}
	public boolean set( int xArray, long nValue, StringBuffer sbError ){
		switch( eTYPE ){
			case Byte:
				sbError.append( "internal error, attempt to cast a long to a Byte" );
				aiVector[xArray] = ERROR_VALUE_Byte;
				return false;
			case Int16:
				sbError.append( "internal error, attempt to cast a long to an Int16" );
				aiVector[xArray] = ERROR_VALUE_Int16;
				return false;
			case UInt16:
				sbError.append( "internal error, attempt to cast a long to a UInt16" );
				aiVector[xArray] = ERROR_VALUE_UInt16;
				return false;
			case Int32:
				sbError.append( "internal error, attempt to cast a long to an Int32" );
				aiVector[xArray] = Integer.MIN_VALUE;
				return false;
			case UInt32:
				if( nValue < 0 || nValue > 0xFFFFFFFF ){ 
					sbError.append( String.format( "value %d is out of range for UInt16 type (0 to 4294967295)", nValue ) ); 
					aiVector[xArray] = ERROR_VALUE_UInt32;
					return false; }
				anVector[xArray] = nValue;
				return true;
			case Float32:
				sbError.append( "internal error, attempt to cast a long to a Float32" );
				afVector[xArray] = Float.NaN;
				return false;
			case Float64:
				sbError.append( "internal error, attempt to cast a long to a Float64" );
				adVector[xArray] = Double.NaN;
				return false;
			case String:
				asVector[xArray] = Long.toString( nValue );
				return true;
			default:
				sbError.append( "unknown data type" );
				return false;
		}
	}
	public boolean set( int xArray, float fValue, StringBuffer sbError ){
		switch( eTYPE ){
			case Byte:
				sbError.append( "internal error, attempt to cast a float to a Byte" );
				aiVector[xArray] = ERROR_VALUE_Byte;
				return false;
			case Int16:
				sbError.append( "internal error, attempt to cast a float to an Int16" );
				aiVector[xArray] = ERROR_VALUE_Int16;
				return false;
			case UInt16:
				sbError.append( "internal error, attempt to cast a float to a UInt16" );
				aiVector[xArray] = ERROR_VALUE_UInt16;
				return false;
			case Int32:
				sbError.append( "internal error, attempt to cast a float to an Int32" );
				aiVector[xArray] = Integer.MIN_VALUE;
				return false;
			case UInt32:
				sbError.append( "internal error, attempt to cast a float to a UInt32" );
				anVector[xArray] = ERROR_VALUE_UInt32;
				return false;
			case Float32:
				afVector[xArray] = fValue;
				return true;
			case Float64:
				sbError.append( "internal error, attempt to cast a 32-bit floating point number to a Float64" );
				adVector[xArray] = Double.NaN;
				return false;
			case String:
				asVector[xArray] = Float.toString( fValue );
				return true;
			default:
				sbError.append( "unknown data type" );
				return false;
		}
	}
	public boolean set( int xArray, double dValue, StringBuffer sbError ){
		switch( eTYPE ){
			case Byte:
				sbError.append( "internal error, attempt to cast a double to a Byte" );
				aiVector[xArray] = ERROR_VALUE_Byte;
				return false;
			case Int16:
				sbError.append( "internal error, attempt to cast a double to an Int16" );
				aiVector[xArray] = ERROR_VALUE_Int16;
				return false;
			case UInt16:
				sbError.append( "internal error, attempt to cast a double to a UInt16" );
				aiVector[xArray] = ERROR_VALUE_UInt16;
				return false;
			case Int32:
				sbError.append( "internal error, attempt to cast a double to an Int32" );
				aiVector[xArray] = Integer.MIN_VALUE;
				return false;
			case UInt32:
				sbError.append( "internal error, attempt to cast a double to a UInt32" );
				anVector[xArray] = ERROR_VALUE_UInt32;
				return false;
			case Float32:
				sbError.append( "internal error, attempt to cast a double to a Float32" );
				afVector[xArray] = Float.NaN;
				return false;
			case Float64:
				adVector[xArray] = dValue;
				return true;
			case String:
				asVector[xArray] = Double.toString( dValue );
				return true;
			default:
				sbError.append( "unknown data type" );
				return false;
		}
	}
	public boolean set( int xArray, String sValue, StringBuffer sbError ){
		switch( eTYPE ){
			case Byte:
				sbError.append( "internal error, attempt to cast a String to a Byte" );
				aiVector[xArray] = ERROR_VALUE_Byte;
				return false;
			case Int16:
				sbError.append( "internal error, attempt to cast a String to an Int16" );
				aiVector[xArray] = ERROR_VALUE_Int16;
				return false;
			case UInt16:
				sbError.append( "internal error, attempt to cast a String to a UInt16" );
				aiVector[xArray] = ERROR_VALUE_UInt16;
				return false;
			case Int32:
				sbError.append( "internal error, attempt to cast a String to an Int32" );
				aiVector[xArray] = Integer.MIN_VALUE;
				return false;
			case UInt32:
				sbError.append( "internal error, attempt to cast a String to a UInt32" );
				anVector[xArray] = ERROR_VALUE_UInt32;
				return false;
			case Float32:
				sbError.append( "internal error, attempt to cast a String to a Float32" );
				afVector[xArray] = Float.NaN;
				return false;
			case Float64:
				sbError.append( "internal error, attempt to cast a String to a Float64" );
				adVector[xArray] = Double.NaN;
				return false;
			case String:
				asVector[xArray] = sValue;
				return true;
			default:
				sbError.append( "unknown data type" );
				return false;
		}
	}
	public String getValue( int xDim1, int xDim2, int xDim3, int xDim4 ){
		int xArray = xDim1 + iDim1 * xDim2 + iDim1 * iDim2 * xDim3 + iDim1 * iDim2 * iDim3 * xDim4;
		if( xArray >= iTotalSize ){
			// sbError.append( String.format( "array size %d exceed by index coordinates %d %d %d %d", iTotalSize, xDim1, xDim2, xDim3, xDim4 ) );
			return "#OUT_OF_BOUNDS#";
		}
		switch( eTYPE ){
			case Byte:
			case Int16:
			case UInt16:
			case Int32:
				return Integer.toString( aiVector[xArray] );
			case UInt32:
				return Long.toString( anVector[xArray] );
			case Float32:
				return Float.toString( afVector[xArray] );
			case Float64:
				return Double.toString( adVector[xArray] );
			case String:
				return asVector[xArray];
			default:
				return "#UNKNOWN_TYPE#";
		}
	}
}

