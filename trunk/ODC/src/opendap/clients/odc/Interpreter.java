package opendap.clients.odc;

/**
 * Title:        Interpreter
 * Description:  Python interpreter capability
 * Copyright:    Copyright (c) 2007-9
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.06
 */

/////////////////////////////////////////////////////////////////////////////
// This file is part of the OPeNDAP Data Connector project.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

import opendap.dap.DArray;

import org.python.util.PythonInterpreter;
import org.python.core.Py;
import org.python.core.PyObject;

import java.util.ArrayList;
import java.util.HashMap;

public class Interpreter {
	private PythonInterpreter mInterpreter = null;
	private String msPrompt = ">>> ";

	public Interpreter() {}

	public PythonInterpreter getInterpeter(){ return mInterpreter; }
	public int getPromptLength(){ return msPrompt.length(); }

	public boolean zInitialize( java.io.OutputStream os, StringBuffer sbError ){
		if( zCreateInterpreter( os, sbError ) ){
//			System.out.println("interpreter exists");
		} else {
			sbError.insert( 0, "Error creating interpreter: " );
			return false;
		}
		return true;
	}

	/** creates a new interpreter (the old one, if any, is discarded) */
	public boolean zCreateInterpreter( java.io.OutputStream os, StringBuffer sbError ){
		try {
			mInterpreter = new PythonInterpreter();
			mInterpreter.setOut(os);
			mInterpreter.setErr(os);
			mInterpreter.exec("import sys");
			mInterpreter.exec("import opendap.clients.odc.API");
			mInterpreter.exec("odcapi = opendap.clients.odc.API");
			String sScriptDirectory = ConfigurationManager.getInstance().getProperty_DIR_Scripts();
			String sScriptDirectory_quoted = '"' + sScriptDirectory + '"';
			sScriptDirectory_quoted = Utility_String.sReplaceString( sScriptDirectory_quoted, "\\", "\\\\");
			mInterpreter.exec("sys.path.append(" + sScriptDirectory_quoted + ")");
			os.write( msPrompt.getBytes() );
			os.flush();
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, sbError );
			return false;
		}
		return true;
	}

	public boolean zExecute( String sCommand, java.io.OutputStream os, StringBuffer sbError ){
		if( sCommand == null ){
			ApplicationController.vShowWarning( "null command issued to interpreter" );
		}
		if( mInterpreter == null ){
			sbError.append("no Python interpreter exists");
			return false;
		}
		try {
			mInterpreter.setOut(os);
			mInterpreter.setErr(os);
			mInterpreter.exec( sCommand );
		} catch( org.python.core.PySyntaxError parse_error ) {
			vWriteLine( os, "!syntax error: " + parse_error, msPrompt );
		} catch( org.python.core.PyException python_error ) {
			vWriteLine( os, "!python error: " + python_error, msPrompt );
		} catch( Throwable t ) {
			System.out.println("error: " + t.getClass().getName());
			ApplicationController.vUnexpectedError( t, sbError );
			return false;
		}
		return true;
	}

	public void vWritePrompt( java.io.OutputStream os ){
		try {
			os.write( msPrompt.getBytes() );
			os.flush();
		} catch( Throwable t ){}
	}

	private void vWriteLine( java.io.OutputStream os, String sLine, String sPrompt ){
		try {
			if( sPrompt == null ) sPrompt = "";
			os.write( sLine.getBytes() );
			os.write( "\n".getBytes() );
			os.flush();
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "Failed to write interpreter line: " );
		}
	}
	
	/** see help text for details on the rules for generation of the data set
	 *  @return null on error */
	public Model_Dataset generateDatasetFromExpression( String sExpressionText, StringBuffer sbError ){
		
		if( sExpressionText == null || sExpressionText.trim().length() == 0 ){
			sbError.append( "expression text is blank" );
			return null;
		}
		ArrayList<String> listLines = Utility.zLoadLines( sExpressionText, sbError );
		if( listLines == null ){
			sbError.insert( 0, "failed to parse expression into lines of text: " );
			return null;
		}

		HashMap<String,String> hmExp = new HashMap<String,String>();
		
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
		boolean DEFAULT_allow_errors = false;
		boolean DEFAULT_trace = false;

		//***********************************************************
		// Simple Array/Grid Generation
		//***********************************************************
		boolean allow_errors = DEFAULT_allow_errors;
		boolean trace = DEFAULT_trace;
		int trace_1_begin = 0;
		int trace_1_end = 0;
		int trace_2_begin = 0;
		int trace_2_end = 0;
		int trace_3_begin = 0;
		int trace_3_end = 0;
		int trace_4_begin = 0;
		int trace_4_end = 0;

		String value_RValue = null;
		DAP.DAP_TYPE type = DAP.DAP_TYPE.Float64;
		int[] size1 = new int[5]; // 1-based
		DAP.DAP_TYPE[][] index_type = new DAP.DAP_TYPE [5][4];  // variable dimension (1-based), index dimension (0 = primary)
		String[][] name = new String[5][4];  // variable dimension (1-based), index dimension (0 = primary)

		ArrayList<String> listGlobals = new ArrayList<String>();
		ArrayList<String> listPreIndexVariables = new ArrayList<String>();
		ArrayList<String> listPreValueVariables = new ArrayList<String>();
		String sLine_allow_errors = null;
		String sLine_trace = null;

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
		String sRValue_trace = null; // this RValue is treated specially because of a non-standard syntax
		String[] asKeyword = {
			"value", "type", "allow_errors",
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

		// process lines in the expression set
		for( int xLine = 1; xLine <= listLines.size(); xLine++ ){
			String sLine = listLines.get( xLine - 1 );
			if( sLine == null || sLine.trim().length() == 0 ) continue;
			sLine = sLine.trim();
			if( sLine.charAt( 0 ) == '#' ) continue; // comment
			int posEquals = sLine.indexOf( '=' );
			if( posEquals == -1 ){  // no equals sign
				if( sLine.startsWith( "trace" ) ){
					trace = true;
					sLine_trace = sLine;
					sRValue_trace = sLine.substring( "trace".length() ).trim();
				}
				continue;
			}
			String sLValue = sLine.substring( 0, posEquals ).trim();
			String sRValue = sLine.substring( posEquals + 1 ).trim();
			if( ! zIsValidPythonIdentifier( sLValue, sbError ) ){
				sbError.insert( 0, "Identifier \"" + sLValue + "\" used as an L-value in line " + xLine + " is invalid: " );
				return null;
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
		final java.io.OutputStream streamTrace = ApplicationController.getInstance().getTextViewerOS();
		
		if( sLine_trace == null && !trace ){
			// do not trace
		} else {
			trace = true;
		}
		
		// determine whether to allow errors
		try {
			String sRValue_allow_errors = hmExp.get( "allow_errors" );
			if( sRValue_allow_errors == null ){
				// do nothing, use default value
			} else if( sRValue_allow_errors.equalsIgnoreCase( "true" ) ){
				allow_errors = true;
				if( trace )	streamTrace.write( "allow_errors set to true".getBytes() );
			} else if( sRValue_allow_errors.equalsIgnoreCase( "false" ) ){
				allow_errors = false;
				if( trace )	streamTrace.write( "allow_errors set to false".getBytes() );
			} else {
				try {
					mInterpreter.eval( sLine_allow_errors );
					PyObject po_allow_errors = mInterpreter.get( "allow_errors" );
					allow_errors = Py.py2boolean( po_allow_errors );
					if( trace )	streamTrace.write( ("allow_errors ( " + sLine_allow_errors + " ) evaluated to " + allow_errors).getBytes() );
				} catch( org.python.core.PySyntaxError parse_error ) {
					sbError.append( "syntax error processing allow_errors setting: " + parse_error );
					return null;
				} catch( org.python.core.PyException python_error ) {
					String sMessage = "python error processing allow_errors setting: " + python_error;
					if( trace )	streamTrace.write( sMessage.getBytes() );
					sbError.append( sMessage );
					return null;
				} catch( Throwable t ) {
					String sMessage = "unexpected error while evaluating allow_errors parameter (" + sLine_allow_errors + "): " + Utility.errorExtractLine( t );
					if( trace ){
						streamTrace.write( sMessage.getBytes() );
					}
					sbError.append( sMessage );
					ApplicationController.vUnexpectedError( t, sbError );
					return null;
				}
			}
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "while determining whether to allow errors" );
			return null;
		}
		
		// obtain the RValue of the value
		value_RValue = hmExp.get( "value" );
		if( value_RValue == null ){
			sbError.append( "no \"value\" parameter supplied; this parameter is required to define the values of the data array" );
			return null;
		}
		
		// (1) All unrecognized variables beginning with "__" (globals) are evaluated.
		try {
			for( int xGlobal = 1; xGlobal <= listGlobals.size(); xGlobal++ ){
				String sGlobal = listGlobals.get( xGlobal - 1 );
				try {
					mInterpreter.eval( sGlobal );
					if( trace ){
						String sGlobalLValue = Utility_String.getLValue( sGlobal );
						if( sGlobalLValue == null ){
							String sMessage = "global " + sGlobal + " evaluated";
							streamTrace.write( sMessage.getBytes() );
						} else {
							PyObject po_global = mInterpreter.get( sGlobalLValue );
							String sMessage = "global " + sGlobal + " evaluated to: " + po_global.toString();
							streamTrace.write( sMessage.getBytes() );
						}
					}
				} catch( org.python.core.PySyntaxError parse_error ) {
					String sMessage = "syntax error evaluating global ( " + sGlobal + " ): " + parse_error;
					if( trace ){
						streamTrace.write( sMessage.getBytes() );
					}
					sbError.append( sMessage );
					if( !allow_errors ) return null;
				} catch( org.python.core.PyException python_error ) {
					String sMessage = "python error evaluating global (" + sGlobal + "): " + python_error;
					if( trace ){
						streamTrace.write( sMessage.getBytes() );
					}
					if( !allow_errors ){
						sbError.append( sMessage );
						return null;
					}
				} catch( Throwable t ) {
					String sMessage = "unexpected error while evaluating global (" + sGlobal + "): " + Utility.errorExtractLine( t );
					if( trace ){
						streamTrace.write( sMessage.getBytes() );
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

		// determine the type of data (already done above)
		
		// determine and validate the dimension count of the index vectors
		boolean[] azMultidimensionalIndex1 = new boolean[4]; // 1-based
		for( int xDim = 1; xDim <= 4; xDim++ ){
			if( hmExp.get( "index_" + xDim + "_3" ) != null ){
				index_vectors[xDim] = 3;
				azMultidimensionalIndex1[xDim] = true;
			} else if( hmExp.get( "index_" + xDim + "_2" ) != null ){
				index_vectors[xDim] = 2;
				azMultidimensionalIndex1[xDim] = true;
			} else if( hmExp.get( "index_" + xDim + "_1" ) != null ){
				index_vectors[xDim] = 1;
				azMultidimensionalIndex1[xDim] = true;
			}
			if( hmExp.get( "index_" + xDim ) != null ){
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
			String sExp_VarName = hmExp.get( sVarName ); 
			if( sExp_VarName == null ){
				// default will be used 
			} else {
				String sEvaluatedName = evaluatePythonRValue_DimName( sExp_VarName, xDim, streamTrace, trace, sbError );
				if( sEvaluatedName == null ){
					if( allow_errors ){
						// default will be used
					} else {
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
				String sExp_VarName = hmExp.get( sVarName ); 
				if( sExp_VarName == null ){
					// default will be used 
				} else {
					String sEvaluatedName = evaluatePythonRValue_DimName( sExp_VarName, xDim, streamTrace, trace, sbError );
					if( sEvaluatedName == null ){
						if( allow_errors ){
							// default will be used
						} else {
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
				index_RValue[xDim][xVector] = hmExp.get( index_LValue[xDim][xVector] ); 
			}
		}		

		// (3) The size variables are evaluated to determine the dimensions of the value array.
		int ctDimensions = 0;
		for( int xDim = 1; xDim <= 4; xDim++ ){
			String sSizeIdentifier = "size_" + xDim;
			String sSizeIdentifier_value = hmExp.get( sSizeIdentifier ); 
			if( sSizeIdentifier_value == null ){
				// then default will be used
			} else {
				int iEvaluatedSize = evaluatePythonRValue_DimSize( sSizeIdentifier_value, xDim, streamTrace, trace, sbError );
				if( iEvaluatedSize == -1 ) return null;
				if( iEvaluatedSize == 0 ) { 
					if( xDim == 1 ){
						sbError.append( "dimension 1 evaluated to 0; must be 1 or greater" );
						return null;
					}
					break; // rest of sizes will be zero
				}
				ctDimensions++;
				size1[xDim] = iEvaluatedSize;
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

		// evaluate the trace intervals
		try {
			if( trace ){
				if( sRValue_trace != null && sRValue_trace.length() > 0 ){
					String[] asTrace = Utility_String.splitCommaWhiteSpace( sRValue_trace );
					int ctTrace = asTrace.length; 
					if( ctTrace != ctDimensions ){
						streamTrace.write( ("trace parameter (" + sRValue_trace + ") parsed to have " + ctTrace + " entries, but there are " + ctDimensions + ". If trace parameters are used they must match the number of dimensions. Tracing will now be turned off.").getBytes() );
						trace = false;
					} else {
						for( int xTraceParameter = 1; xTraceParameter <= ctTrace; xTraceParameter++ ){
							String sParameter = asTrace[xTraceParameter - 1];
							String[] asTraceRange = Utility_String.split( sParameter, ':' );
							if( asTraceRange.length > 2 ){
								streamTrace.write( ("trace parameter " + xTraceParameter + " (" + sParameter + ") parsed to have " + asTraceRange.length + " entries, but only 1 or 2 were expected. Trace parameters must be either 0-based numbers or ranges of index values, e.g., \"23:45\". Tracing will now be turned off.").getBytes() );
								trace = false;
								break;
							}
							int iParameter_begin = Utility_String.parseInteger_nonnegative( asTraceRange[0] );
							if( iParameter_begin == -1 ){
								streamTrace.write( ("trace parameter " + xTraceParameter + " (" + asTraceRange[0] + ") did not parse to a non-negative integer. Tracing will now be turned off.").getBytes() );
								trace = false;
								break;
							}
							int iParameter_end;
							if( asTraceRange.length == 2 ){
								iParameter_end = Utility_String.parseInteger_nonnegative( asTraceRange[1] );
								if( iParameter_begin == -1 ){
									streamTrace.write( ("trace parameter " + xTraceParameter + ", end part (" + asTraceRange[1] + ") did not parse to a non-negative integer. Tracing will now be turned off.").getBytes() );
									trace = false;
									break;
								}
							} else {
								iParameter_end = iParameter_begin; 
							}
							if( iParameter_end < iParameter_begin ){
								streamTrace.write( ("trace parameter " + xTraceParameter + " (" + sParameter+ ") is invalid because the begin value ( " + iParameter_begin + " ) is greater than the end value ( " + iParameter_end + " ). Tracing will now be turned off.").getBytes() );
								trace = false;
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
			ApplicationController.vUnexpectedError( t, "while evaluating trace parameters" );
			return null;
		}
		
		// (4) The internal index values are looped.
//		ArrayList<String> listPreValueVariables = new ArrayList<String>();
		int x1 = 0;
		int x2 = 0;
		int x3 = 0;
		int x4 = 0;
		while( true ){
			if( x1 == size1[1] && x2 == size1[2] && x3 == size1[3] && x4 == size1[4] ) break; // done
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
					try { streamTrace.write( ("pre-index expression " + xPreIndexExp + " (" + sPreIndexExp + ") after macro substitution: " + sPreIndexExp_after_macro ).getBytes() ); } catch( Throwable t ){};
				}
				try {
					mInterpreter.eval( sPreIndexExp_after_macro );
				} catch( org.python.core.PySyntaxError parse_error ) {
					sbError.append( "syntax error evaluating pre-index expression (" + sPreIndexExp_after_macro + ": " + parse_error );
					return null;
				} catch( org.python.core.PyException python_error ) {
					sbError.append( "python error evaluating pre-index expression (" + sPreIndexExp_after_macro + "): " + python_error );
				} catch( Throwable t ) {
					sbError.append( "while evaluating pre-index expression " + sPreIndexExp_after_macro );
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
						try { streamTrace.write( ("index expression " + index_LValue[xDim][xIndex] + " (" + sRValue + ") after macro substitution: " + sRValue_after_macro ).getBytes() ); } catch( Throwable t ){};
					}
					try {
						PyObject po_IndexValue = mInterpreter.eval( sRValue_after_macro );
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
								streamTrace.write( sMessage.getBytes() );
							}
						} else {
							sbError.insert( 0, String.format( "error setting value %s for index, dim: %d, vector: %d, member: %d: " , po_IndexValue.toString(), xDim, xIndex, xMember ) ); 
							if( zTraceLoop ){
								streamTrace.write( sbError.toString().getBytes() );
							}
							if( ! allow_errors ) return null;
						}
						
					} catch( org.python.core.PySyntaxError parse_error ) {
						String sMessage = "syntax error while evaluating " + index_LValue[xDim][xIndex] + " setting (" + sRValue_after_macro + "): " + parse_error;
						if( zTraceLoop ){
							try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable t ) {}
						}
						sbError.append( sMessage );
						return null;
					} catch( org.python.core.PyException python_error ) {
						String sMessage = "python error evaluating " + index_LValue[xDim][xIndex] + " setting (" + sRValue_after_macro + "): " + python_error;
						if( zTraceLoop ){
							try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable t ) {}
						}
						sbError.append( sMessage );
						return null;
					} catch( Throwable t ) {
						String sMessage = "error while processing " + index_LValue[xDim][xIndex] + " setting (" + sRValue_after_macro + "): " + Utility.errorExtractLine( t );
						if( zTraceLoop ){
							try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable trace_t ) {}
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
					try { streamTrace.write( ("pre-value expression " + xPreValueExp + " (" + sPreValueExp + ") after macro substitution: " + sPreValueExp_after_macro ).getBytes() ); } catch( Throwable t ){};
				}
				try {
					mInterpreter.eval( sPreValueExp_after_macro );
				} catch( org.python.core.PySyntaxError parse_error ) {
					sbError.append( "syntax error evaluating pre-value expression (" + sPreValueExp_after_macro + ": " + parse_error );
					return null;
				} catch( org.python.core.PyException python_error ) {
					sbError.append( "python error evaluating pre-value expression (" + sPreValueExp_after_macro + "): " + python_error );
				} catch( Throwable t ) {
					sbError.append( "while evaluating pre-value expression " + sPreValueExp_after_macro );
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
				try { streamTrace.write( ("value expression (" + value_RValue + ") after macro substitution: " + value_RValue_after_macro ).getBytes() ); } catch( Throwable t ){};
			}
			try {
				PyObject po_IndexValue = mInterpreter.eval( value_RValue_after_macro );
				if( pvValue.set( x1, x2, x3, x4, po_IndexValue, sbError ) ){
					if( zTraceLoop ){
						String sMessage = String.format( "value %s written for member [%d][%d][%d][%d]" , pvValue.getValue( x1, x2, x3, x4 ), x1, x2, x3, x4 );
						streamTrace.write( sMessage.getBytes() );
					}
				} else {
					sbError.insert( 0, String.format( "error setting value %s for member [%d][%d][%d][%d]" , po_IndexValue.toString(), x1, x2, x3, x4 ) );
					if( zTraceLoop ){
						streamTrace.write( sbError.toString().getBytes() );
					}
					if( ! allow_errors ) return null;
				}
				
			} catch( org.python.core.PySyntaxError parse_error ) {
				String sMessage = "syntax error while evaluating " + value_RValue + " setting (" + value_RValue_after_macro + "): " + parse_error;
				if( zTraceLoop ){
					try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable t ) {}
				}
				sbError.append( sMessage );
				return null;
			} catch( org.python.core.PyException python_error ) {
				String sMessage = "python error evaluating " + value_RValue + " setting (" + value_RValue_after_macro + "): " + python_error;
				if( zTraceLoop ){
					try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable t ) {}
				}
				sbError.append( sMessage );
				return null;
			} catch( Throwable t ) {
				String sMessage = "error while processing " + value_RValue + " setting (" + value_RValue_after_macro + "): " + Utility.errorExtractLine( t );
				if( zTraceLoop ){
					try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable trace_t ) {}
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
			boolean zIndexPresent = false;
			for( int xDim = 1; xDim <= 4; xDim++ ){
				if( index_vectors[xDim] > 0 ) zIndexPresent = true; 
			}
			 if( zIndexPresent ){  // then we are creating a grid, otherwise it will be a plain array
			 } else { // creating a plain array
				opendap.dap.DefaultFactory factory = new opendap.dap.DefaultFactory();
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
						darray.addVariable( new opendap.dap.DFloat32() );
						break;
					case String:
						darray.addVariable( new opendap.dap.DString() );
						break;
				}
				for( int xDim = 1; xDim <= 4; xDim++ ){
					if( size1[xDim] == 0 ) break;
					darray.appendDim( size1[xDim] );
				}
				// set name
				darray.getPrimitiveVector().setInternalStorage( pvValue.getInternalStorage() );
				datadds.addVariable( darray );
			}
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

	final private static String[] aPythonReservedWords = {
		"and", "assert", "break", "class", "continue",
		"def", "del", "elif", "else", "except",
		"exec", "finally", "for", "from", "global",
		"if", "import", "in", "is", "lambda",
		"not", "or", "pass", "print", "raise",
		"return", "try", "while", "float", "int",
		"string"
	};
	
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
	
	private boolean zIsValidPythonIdentifier( String sIdentifier, StringBuffer sbError ){
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

	private int evaluatePythonRValue_DimCount( String sRValue, int iDimNumber, java.io.OutputStream streamTrace, boolean zTrace, StringBuffer sbError ){
		try {
			PyObject po_IndexDimensionCount = mInterpreter.eval( sRValue );
			int iIndexDimensionCount_conversion = Py.py2int( po_IndexDimensionCount );
			if( iIndexDimensionCount_conversion < 0 || iIndexDimensionCount_conversion > 4 ){
				String sMessage = "index dimension count expression (" + sRValue + ") for dimension " + iDimNumber + " did not evaluate to a valid integer between 1 and 4";
				if( zTrace ){
					streamTrace.write( sMessage.getBytes() );
				}
				sbError.append( sMessage );
				return -1;
			} else {
				streamTrace.write( ("Dimension count for index variable " + iDimNumber + " ( index_" + iDimNumber + "_dimension ) evaluated to have " + iIndexDimensionCount_conversion + " dimensions." ).getBytes() );
				return iIndexDimensionCount_conversion;
			}
		} catch( org.python.core.PySyntaxError parse_error ) {
			String sMessage = "syntax error while evaluating index_" + iDimNumber + "_dimensions setting (" + sRValue + "): " + parse_error;
			if( zTrace ){
				try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable t ) {}
			}
			sbError.append( sMessage );
			return -1;
		} catch( org.python.core.PyException python_error ) {
			String sMessage = "python error evaluating index_" + iDimNumber + "_dimensions setting (" + sRValue + "): " + python_error;
			if( zTrace ){
				try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable t ) {}
			}
			sbError.append( sMessage );
			return -1;
		} catch( Throwable t ) {
			String sMessage = "error while processing index_" + iDimNumber + "_dimensions setting (" + sRValue + "): " + Utility.errorExtractLine( t );
			if( zTrace ){
				try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable trace_t ) {}
			}
			sbError.append( sMessage );
			return -1;
		}
	}

	private int evaluatePythonRValue_DimSize( String sRValue, int iDimNumber, java.io.OutputStream streamTrace, boolean zTrace, StringBuffer sbError ){
		try {
			PyObject po_DimensionSize = mInterpreter.eval( sRValue );
			int iDimensionSize_conversion = Py.py2int( po_DimensionSize );
			if( iDimensionSize_conversion < 0 ){
				String sMessage = "dimension size expression (" + sRValue + ") for dimension " + iDimNumber + " did not evaluate to a non-negative integer (" + iDimensionSize_conversion + ")";
				if( zTrace ){
					streamTrace.write( sMessage.getBytes() );
				}
				sbError.append( sMessage );
				return -1;
			} else {
				streamTrace.write( ("Size for dimension " + iDimNumber + " ( size_" + iDimNumber + " ) evaluated to " + iDimensionSize_conversion ).getBytes() );
				return iDimensionSize_conversion;
			}
		} catch( org.python.core.PySyntaxError parse_error ) {
			String sMessage = "syntax error while evaluating size_" + iDimNumber + " setting (" + sRValue + "): " + parse_error;
			if( zTrace ){
				try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable t ) {}
			}
			sbError.append( sMessage );
			return -1;
		} catch( org.python.core.PyException python_error ) {
			String sMessage = "python error evaluating size_" + iDimNumber + " setting (" + sRValue + "): " + python_error;
			if( zTrace ){
				try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable t ) {}
			}
			sbError.append( sMessage );
			return -1;
		} catch( Throwable t ) {
			String sMessage = "error while processing size_" + iDimNumber + " setting (" + sRValue + "): " + Utility.errorExtractLine( t );
			if( zTrace ){
				try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable trace_t ) {}
			}
			sbError.append( sMessage );
			return -1;
		}
	}

	private int evaluatePythonRValue_TraceParameter( String sRValue, String sParameterName, java.io.OutputStream streamTrace, boolean zTrace, StringBuffer sbError ){
		try {
			PyObject po_Parameter = mInterpreter.eval( sRValue );
			int iParameter = Py.py2int( po_Parameter );
			if( iParameter < 0 ){
				String sMessage = sParameterName + " expression (" + sRValue + ") did not evaluate to a non-negative integer (" + iParameter + ")";
				if( zTrace ){
					streamTrace.write( sMessage.getBytes() );
				}
				sbError.append( sMessage );
				return -1;
			} else {
				streamTrace.write( ("Parameter " + sParameterName + " evaluated to " + iParameter ).getBytes() );
				return iParameter;
			}
		} catch( org.python.core.PySyntaxError parse_error ) {
			String sMessage = "syntax error while evaluating " + sParameterName + " setting (" + sRValue + "): " + parse_error;
			if( zTrace ){
				try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable t ) {}
			}
			sbError.append( sMessage );
			return -1;
		} catch( org.python.core.PyException python_error ) {
			String sMessage = "python error evaluating " + sParameterName + " setting (" + sRValue + "): " + python_error;
			if( zTrace ){
				try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable t ) {}
			}
			sbError.append( sMessage );
			return -1;
		} catch( Throwable t ) {
			String sMessage = "error while processing " + sParameterName + " setting (" + sRValue + "): " + Utility.errorExtractLine( t );
			if( zTrace ){
				try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable trace_t ) {}
			}
			sbError.append( sMessage );
			return -1;
		}
	}
	
	private String evaluatePythonRValue_DimName( String sRValue, int iDimNumber, java.io.OutputStream streamTrace, boolean zTrace, StringBuffer sbError ){
		try {
			PyObject po_DimensionName = mInterpreter.eval( sRValue );
			String sDimensionsName = po_DimensionName.toString();
			if( DAP.isValidIdentifier( sDimensionsName, sbError) ){
				streamTrace.write( ("Dimension " + iDimNumber + " name variable ( name_" + iDimNumber + " ) evaluated \"" + sDimensionsName + "\"" ).getBytes() );
				return sDimensionsName;
			} else {
				sbError.insert( 0, "dimension name expression (" + sRValue + ") for dimension " + iDimNumber + " did not evaluate to a valid identifier: " );
				if( zTrace ){
					streamTrace.write( sbError.toString().getBytes() );
				}
				return null;
			}
		} catch( org.python.core.PySyntaxError parse_error ) {
			String sMessage = "syntax error while evaluating dimension name_" + iDimNumber + " setting (" + sRValue + "): " + parse_error;
			if( zTrace ){
				try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable t ) {}
			}
			sbError.append( sMessage );
			return null;
		} catch( org.python.core.PyException python_error ) {
			String sMessage = "python error evaluating dimension name_" + iDimNumber + " setting (" + sRValue + "): " + python_error;
			if( zTrace ){
				try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable t ) {}
			}
			sbError.append( sMessage );
			return null;
		} catch( Throwable t ) {
			String sMessage = "error while processing dimension name_" + iDimNumber + " setting (" + sRValue + "): " + Utility.errorExtractLine( t );
			if( zTrace ){
				try { streamTrace.write( sMessage.getBytes() ); } catch( Throwable trace_t ) {}
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
			sbError.append( String.format( "array size %d exceed by index coordinates %d %d %d %d", iTotalSize, xDim1, xDim2, xDim3, xDim4 ) );
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

