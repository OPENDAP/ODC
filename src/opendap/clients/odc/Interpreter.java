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
		
		ArrayList<String> listGlobals = new ArrayList<String>();
		ArrayList<String> listPreIndexVariables = new ArrayList<String>();
		ArrayList<String> listPreValueVariables = new ArrayList<String>();
		String sLine_allow_errors = null;
		String sLine_trace = null;
		String sRValue_allow_errors = null;
		String sRValue_trace = null;
		String sRValue_value = null;
		String sRValue_type = null;
		String sRValue_size_1 = null;
		String sRValue_size_2 = null;
		String sRValue_size_3 = null;
		String sRValue_size_4 = null;
		String sRValue_index_1 = null;
		String sRValue_index_2 = null;
		String sRValue_index_3 = null;
		String sRValue_index_4 = null;
		String sRValue_name_1 = null;
		String sRValue_name_2 = null;
		String sRValue_name_3 = null;
		String sRValue_name_4 = null;
		String sRValue_index_1_dimensions = null;
		String sRValue_index_2_dimensions = null;
		String sRValue_index_3_dimensions = null;
		String sRValue_index_4_dimensions = null;

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

		Object value = DEFAULT_value;
		int type = DAP.DATA_TYPE_Float64;
		int size_1 = 100;
		int size_2 = 0;
		int size_3 = 0;
		int size_4 = 0;
		Object index_1 = null;
		Object index_2 = null;
		Object index_3 = null;
		Object index_4 = null;
		String name_1 = "dimension_1";
		String name_2 = "dimension_2";
		String name_3 = "dimension_3";
		String name_4 = "dimension_4";
		int index_1_dimensions = 1;
		int index_2_dimensions = 1;
		int index_3_dimensions = 1;
		int index_4_dimensions = 1;
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
			} else if( sLValue.equals( "allow_errors" ) ){
				sRValue_allow_errors = sRValue;
				sLine_allow_errors = sLine;
			} else if( sLValue.equals( "value" ) ){
				sRValue_value = sRValue;
			} else if( sLValue.equals( "type" ) ){
				sRValue_type = sRValue;
				int iDAP_type = DAP.getTypeByName( sRValue_type );
				if( iDAP_type == 0 ){
					sbError.append( "unknown DAP type: " + sRValue_type );
					return null;
				}
				type = iDAP_type;
			} else if( sLValue.equals( "size_1" ) ){
				sRValue_size_1 = sRValue;
			} else if( sLValue.equals( "size_2" ) ){
				sRValue_size_2 = sRValue;
			} else if( sLValue.equals( "size_3" ) ){
				sRValue_size_3 = sRValue;
			} else if( sLValue.equals( "size_4" ) ){
				sRValue_size_4 = sRValue;
			} else if( sLValue.equals( "index_1" ) ){
				sRValue_index_1 = sRValue;
			} else if( sLValue.equals( "index_2" ) ){
				sRValue_index_2 = sRValue;
			} else if( sLValue.equals( "index_3" ) ){
				sRValue_index_3 = sRValue;
			} else if( sLValue.equals( "index_4" ) ){
				sRValue_index_4 = sRValue;
			} else if( sLValue.equals( "name_1" ) ){
				sRValue_name_1 = sRValue;
			} else if( sLValue.equals( "name_2" ) ){
				sRValue_name_2 = sRValue;
			} else if( sLValue.equals( "name_3" ) ){
				sRValue_name_3 = sRValue;
			} else if( sLValue.equals( "name_4" ) ){
				sRValue_name_4 = sRValue;
			} else if( sLValue.equals( "index_1_dimensions" ) ){
				sRValue_index_1_dimensions = sRValue;
			} else if( sLValue.equals( "index_2_dimensions" ) ){
				sRValue_index_1_dimensions = sRValue;
			} else if( sLValue.equals( "index_3_dimensions" ) ){
				sRValue_index_1_dimensions = sRValue;
			} else if( sLValue.equals( "index_4_dimensions" ) ){
				sRValue_index_1_dimensions = sRValue;
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
			if( sLine_allow_errors == null ){
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
		
		// evaluate the dimension count of the index values
		if( sRValue_index_1 == null ){
			// then default will be used
		} else {
			index_1_dimensions = evaluatePythonRValue_DimCount( sRValue_index_1, 1, streamTrace, trace, sbError );
			if( index_1_dimensions == -1 ) return null;
		}
		if( sRValue_index_2 == null ){
			// then default will be used
		} else {
			index_2_dimensions = evaluatePythonRValue_DimCount( sRValue_index_2, 2, streamTrace, trace, sbError );
			if( index_2_dimensions == -1 ) return null;
		}
		if( sRValue_index_3 == null ){
			// then default will be used
		} else {
			index_3_dimensions = evaluatePythonRValue_DimCount( sRValue_index_3, 3, streamTrace, trace, sbError );
			if( index_1_dimensions == -1 ) return null;
		}
		if( sRValue_index_4 == null ){
			// then default will be used
		} else {
			index_4_dimensions = evaluatePythonRValue_DimCount( sRValue_index_4, 4, streamTrace, trace, sbError );
			if( index_4_dimensions == -1 ) return null;
		}
		
		// evaluate the dimensional names
		if( sRValue_name_1 == null ){
			// then default will be used
		} else {
			String sEvaluatedName = evaluatePythonRValue_DimName( sRValue_name_1, 1, streamTrace, trace, sbError );
			if( sEvaluatedName == null ){
				if( allow_errors ){
					// default will be used
				} else {
					return null;
				}
			} else {
				name_1 = sEvaluatedName;
			}
		}
		if( sRValue_name_2 == null ){
			// then default will be used
		} else {
			String sEvaluatedName = evaluatePythonRValue_DimName( sRValue_name_2, 2, streamTrace, trace, sbError );
			if( sEvaluatedName == null ){
				if( allow_errors ){
					// default will be used
				} else {
					return null;
				}
			} else {
				name_2 = sEvaluatedName;
			}
		}
		if( sRValue_name_3 == null ){
			// then default will be used
		} else {
			String sEvaluatedName = evaluatePythonRValue_DimName( sRValue_name_3, 3, streamTrace, trace, sbError );
			if( sEvaluatedName == null ){
				if( allow_errors ){
					// default will be used
				} else {
					return null;
				}
			} else {
				name_3 = sEvaluatedName;
			}
		}
		if( sRValue_name_4 == null ){
			// then default will be used
		} else {
			String sEvaluatedName = evaluatePythonRValue_DimName( sRValue_name_4, 4, streamTrace, trace, sbError );
			if( sEvaluatedName == null ){
				if( allow_errors ){
					// default will be used
				} else {
					return null;
				}
			} else {
				name_4 = sEvaluatedName;
			}
		}
		
		// (3) The size variables are evaluated to determine the dimensions of the value array.
		int ctDimensions = 0;
		if( sRValue_size_1 == null ){
			// then default will be used
		} else {
			size_1 = evaluatePythonRValue_DimSize( sRValue_size_1, 1, streamTrace, trace, sbError );
			if( size_1 == -1 ) return null;
			if( size_1 == 0 ) { sbError.append( "dimension 1 evaluated to 0; must be 1 or greater" ); return null; }
			ctDimensions++;
		}
		if( sRValue_index_2 == null ){
			// then default will be used
		} else {
			size_2 = evaluatePythonRValue_DimCount( sRValue_index_2, 2, streamTrace, trace, sbError );
			if( size_2 == -1 ) return null; // invalid sizes cannot be ignored
			ctDimensions++;
		}
		if( sRValue_index_3 == null ){
			// then default will be used
		} else {
			size_3 = evaluatePythonRValue_DimCount( sRValue_index_3, 3, streamTrace, trace, sbError );
			if( size_3 == -1 ) return null; // invalid sizes cannot be ignored
			if( size_3 > 0 && size_2 == 0 ){
				sbError.append( "sizes given for dimensions 1 and 3, but not 2; dimension 2 must have at least 1 member if dimension 3 is used" );
				return null;
			}
			ctDimensions++;
		}
		if( sRValue_index_4 == null ){
			// then default will be used
		} else {
			size_4 = evaluatePythonRValue_DimCount( sRValue_index_4, 4, streamTrace, trace, sbError );
			if( size_4 == -1 ) return null; // invalid sizes cannot be ignored
			if( size_4 > 0 && (size_2 == 0 || size_3 == 0) ){
				sbError.append( "sizes given for dimensions 1 and 4, but not 2 and/or 3; both dimension 2 and 3 must have at least 1 member if dimension 4 is used" );
				return null;
			}
			ctDimensions++;
		}

		// make sure we have enough memory
		int iTotalSize = size_1;
		if( size_2 > 0 ){
			if( Integer.MAX_VALUE / size_2 > iTotalSize ){
				sbError.append( "dimensional sizes (" + size_1 + " and " + size_2 + ") result in an oversized array" );
				return null;
			} else {
				iTotalSize *= size_2;
				if( size_3 > 0 ){
					if( Integer.MAX_VALUE / size_3 > iTotalSize ){
						sbError.append( "dimensional sizes (" + size_1 + " and " + size_2 + " and " + size_3 + ") result in an oversized array" );
						return null;
					} else {
						iTotalSize *= size_3;
						if( size_4 > 0 ){
							if( Integer.MAX_VALUE / size_4 > iTotalSize ){
								sbError.append( "dimensional sizes (" + size_1 + " and " + size_2 + " and " + size_3 + " and " + size_3 + ") result in an oversized array" );
								return null;
							}
						} else {
							iTotalSize *= size_4;
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
		final int eTYPE = type; // this is done to optimize compilation
		byte[] abData;
		short[] ashData;
		int[] aiData;
		long[] anData;
		float[] afData;
		double[] adData;
		String[] asData;
		switch( eTYPE ){
			case DAP.DATA_TYPE_Byte:
				abData = new byte[iTotalSize];
				break;
			case DAP.DATA_TYPE_Int16:
			case DAP.DATA_TYPE_UInt16:
				ashData = new short[iTotalSize];
				break;
			case DAP.DATA_TYPE_Int32:
			case DAP.DATA_TYPE_UInt32:
				aiData = new int[iTotalSize];
				break;
			case DAP.DATA_TYPE_Float32:
				afData = new float[iTotalSize];
				break;
			case DAP.DATA_TYPE_Float64:
				adData = new double[iTotalSize];
				break;
			case DAP.DATA_TYPE_String:
				asData = new String[iTotalSize];
				break;
			default:
				sbError.append( "unrecognized data type: " + eTYPE );
				return null;
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
			if( x1 == size_1 && x2 == size_2 && x3 == size_3 && x4 == size_4 ) break; // done

		// (5) All unrecognized variables beginning with "_" are evaluated after macro substitution.
		for( int xPreIndexExp = 1; xPreIndexExp <= listPreIndexVariables.size(); xPreIndexExp++ ){
			String sPreIndexExp = listPreIndexVariables.get( xPreIndexExp - 1 );
			String sPreIndexExp_after_macro = null;
			try {
				sPreIndexExp_after_macro = Utility_String.sReplaceString( sPreIndexExp, "$1", Integer.toString( x1 ) );
				sPreIndexExp_after_macro = Utility_String.sReplaceString( sPreIndexExp_after_macro, "$2", Integer.toString( x1 ) );
				sPreIndexExp_after_macro = Utility_String.sReplaceString( sPreIndexExp_after_macro, "$3", Integer.toString( x1 ) );
				sPreIndexExp_after_macro = Utility_String.sReplaceString( sPreIndexExp_after_macro, "$4", Integer.toString( x1 ) );
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

		// (7) All unrecognized variables not beginning with "_" are evaluated after macro substitution.

		// (8) The value variable is evaluated.

		// (9) The process increments counters and loops to step (3), and continuing until the value matrix is full.
			x1++;
			if( x1 == size_1 ){
				x1 = 0;
				x2++;
				if( x2 == size_2 ){
					x2 = 0;
					x3++;
					if( x3 == size_3 ){
						x3 = 0;
						x4++;
					}}}
		}

		// and create the Data DDS
		opendap.dap.DataDDS datadds = null;
		try {
			String sServerVersion = "2.1.5";
			int iHeaderType = opendap.dap.ServerVersion.XDAP; // this is the simple version (eg "2.1.5"), server version strings are like "XDODS/2.1.5", these are only used in HTTP, not in files
			opendap.dap.ServerVersion server_version = new opendap.dap.ServerVersion( sServerVersion, iHeaderType );
			datadds = new opendap.dap.DataDDS( server_version );
			if( sRValue_index_1 != null ){  // then we are creating a grid, otherwise it will be a plain array
			} else { // creating a plain array
				opendap.dap.DefaultFactory factory = new opendap.dap.DefaultFactory();
//				DArray darray = new factory.newDArray();
//				darray.addVariable(v)
	//			datadds.addVariable(v)
			}
		} catch( Throwable t ) {
			sbError.append( "while creating Data DDS" );
			ApplicationController.vUnexpectedError( t, sbError );
			return null;
		}

		// create the model
		// Model_Dataset model = Model_Dataset.createData( datadds, sbError );
		// if( model == null ){
//			sbError.insert( 0, "error creating model for Data DDS: " );
//			return null;
//		}
		
		
	
		return null;
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
