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
//			Utility.vUnexpectedError( t, "XXX" );
			ApplicationController.vShowError("Failed to write interpreter line: " + t);
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
		String sRValue_allow_errors = null;
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
		String sRValue_index_dimensions = null;

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
		String name_1 = null;
		String name_2 = null;
		String name_3 = null;
		String name_4 = null;
		int index_dimensions = 1;
		boolean allow_errors = false;
		
		// process lines in the expression set
		for( int xLine = 1; xLine <= listLines.size(); xLine++ ){
			String sLine = listLines.get( xLine - 1 );
			if( sLine == null || sLine.trim().length() == 0 ) continue;
			sLine = sLine.trim();
			if( sLine.charAt( 0 ) == '#' ) continue; // comment
			int posEquals = sLine.indexOf( '=' );
			if( posEquals == -1 ) continue; // no equals sign
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
			} else if( sLValue.equals( "index_dimensions" ) ){
				sRValue_index_dimensions = sRValue;
			} else {
				listPreValueVariables.add( sLine );
			}
		}
		
		// (1) All unrecognized variables beginning with "__" (globals) are evaluated.
		for( int xGlobal = 1; xGlobal <= listGlobals.size(); xGlobal++ ){
			String sGlobal = listGlobals.get( xGlobal - 1 );
			try {
				mInterpreter.eval( sGlobal );
			} catch( org.python.core.PySyntaxError parse_error ) {
				sbError.append( "syntax error evaluating global (" + sGlobal + ": " + parse_error );
				return null;
			} catch( org.python.core.PyException python_error ) {
				sbError.append( "python error evaluating global (" + sGlobal + "): " + python_error );
			} catch( Throwable t ) {
				sbError.append( "while evaluating global " + sGlobal );
				ApplicationController.vUnexpectedError( t, sbError );
				return null;
			}
		}
		

		// (2) The configuration values (allow_errors, type, name, and index_dimensions) are evaluated first in that order.

		// determine whether to allow errors
		if( sLine_allow_errors == null ){
			// do nothing, use default value
		} else if( sRValue_allow_errors.equalsIgnoreCase( "true" ) ){
			allow_errors = true;
		} else if( sRValue_allow_errors.equalsIgnoreCase( "false" ) ){
			allow_errors = false;
		} else {
			try {
				mInterpreter.eval( sLine_allow_errors );
				PyObject po_allow_errors = mInterpreter.get( "allow_errors" );
				allow_errors = Py.py2boolean( po_allow_errors );
			} catch( org.python.core.PySyntaxError parse_error ) {
				sbError.append( "syntax error processing allow_errors setting: " + parse_error );
				return null;
			} catch( org.python.core.PyException python_error ) {
				sbError.append( "python error processing allow_errors setting: " + python_error );
				return null;
			} catch( Throwable t ) {
				sbError.append( "while processing allow_errors setting" );
				ApplicationController.vUnexpectedError( t, sbError );
				return null;
			}
		}
		
		// determine the type of data (already done above)
		
		// evaluate the dimension size of the index values
		if( sRValue_index_dimensions == null ){
			// then default will be used
		} else {
			try {
				PyObject po_IndexDimensions = mInterpreter.eval( sRValue_index_dimensions );
				int iIndexDimensions_conversion = Py.py2int( po_IndexDimensions );
				if( iIndexDimensions_conversion < 0 || iIndexDimensions_conversion > 4 ){
					sbError.append( "index dimension expression (" + sRValue_index_dimensions + ") did not evaluate to a valid integer between 1 and 4" );
					return null;
				} else {
					index_dimensions =iIndexDimensions_conversion;
				}
			} catch( org.python.core.PySyntaxError parse_error ) {
				sbError.append( "syntax error processing index_dimensions setting: " + parse_error );
				return null;
			} catch( org.python.core.PyException python_error ) {
				sbError.append( "python error processing index_dimensions setting: " + python_error );
				return null;
			} catch( Throwable t ) {
				sbError.append( "while processing index_dimensions setting" );
				ApplicationController.vUnexpectedError( t, sbError );
				return null;
			}
		}
		
		// evaluate the dimensional names
		try {
			if( sRValue_name_1 != null ){
				PyObject po_sName1 = mInterpreter.eval( sRValue_name_1 );
				name_1 = po_sName1.toString();
			}
			if( sRValue_name_2 != null ){
				PyObject po_sName2 = mInterpreter.eval( sRValue_name_2 );
				name_2 = po_sName2.toString();
			}
			if( sRValue_name_3 != null ){
				PyObject po_sName3 = mInterpreter.eval( sRValue_name_3 );
				name_3 = po_sName3.toString();
			}
			if( sRValue_name_4 != null ){
				PyObject po_sName4 = mInterpreter.eval( sRValue_name_4 );
				name_4 = po_sName4.toString();
			}
		} catch( org.python.core.PySyntaxError parse_error ) {
			sbError.append( "syntax error processing allow_errors setting: " + parse_error );
			return null;
		} catch( org.python.core.PyException python_error ) {
			sbError.append( "python error processing allow_errors setting: " + python_error );
			return null;
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, sbError );
			return null;
		}
		
		// (3) The size variables are evaluated to determine the dimensions of the value array.
		try {
			if( sRValue_size_1 == null ){
				// then default will be used
			} else {
				PyObject po_IndexDimensions = mInterpreter.eval( sRValue_size_1 );
				int iIndexSize_1 = Py.py2int( po_IndexDimensions );
				if( iIndexSize_1 < 1 ){
					sbError.append( "index dimension 1 expression (" + sRValue_index_dimensions + ") did not evaluate to a positive integer" );
					return null;
				} else {
					size_1 =iIndexSize_1;
				}
			}
			if( sRValue_size_2 == null ){
				// then default will be used
			} else {
				PyObject po_IndexDimensions = mInterpreter.eval( sRValue_size_2 );
				int iIndexSize_2 = Py.py2int( po_IndexDimensions );
				if( iIndexSize_2 < 0 ){
					sbError.append( "index dimension 2 expression (" + sRValue_index_dimensions + ") did not evaluate to a non-negative integer" );
					return null;
				} else {
					size_2 =iIndexSize_2;
				}
			}
			if( sRValue_size_3 == null ){
				// then default will be used
			} else {
				PyObject po_IndexDimensions = mInterpreter.eval( sRValue_size_3 );
				int iIndexSize_3 = Py.py2int( po_IndexDimensions );
				if( iIndexSize_3 < 0 ){
					sbError.append( "index dimension 3 expression (" + sRValue_index_dimensions + ") did not evaluate to a non-negative integer" );
					return null;
				} else {
					size_3 =iIndexSize_3;
				}
			}
			if( sRValue_size_4 == null ){
				// then default will be used
			} else {
				PyObject po_IndexDimensions = mInterpreter.eval( sRValue_size_4 );
				int iIndexSize_4 = Py.py2int( po_IndexDimensions );
				if( iIndexSize_4 < 0 ){
					sbError.append( "index dimension 4 expression (" + sRValue_index_dimensions + ") did not evaluate to a non-negative integer" );
					return null;
				} else {
					size_4 =iIndexSize_4;
				}
			}
		} catch( org.python.core.PySyntaxError parse_error ) {
			sbError.append( "syntax error processing index size settings: " + parse_error );
			return null;
		} catch( org.python.core.PyException python_error ) {
			sbError.append( "python error processing index size settings: " + python_error );
			return null;
		} catch( Throwable t ) {
			sbError.append( "while processing index size settings" );
			ApplicationController.vUnexpectedError( t, sbError );
			return null;
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
		try {
			if( sRValue_index_1 == null ){
				// then default will be used
			} else {
				PyObject po_IndexDimensions = mInterpreter.eval( sRValue_size_1 );
				int iIndexSize_1 = Py.py2int( po_IndexDimensions );
				if( iIndexSize_1 < 1 ){
					sbError.append( "index dimension 1 expression (" + sRValue_index_dimensions + ") did not evaluate to a positive integer" );
					return null;
				} else {
					size_1 =iIndexSize_1;
				}
			}
			if( sRValue_size_2 == null ){
				// then default will be used
			} else {
				PyObject po_IndexDimensions = mInterpreter.eval( sRValue_size_2 );
				int iIndexSize_2 = Py.py2int( po_IndexDimensions );
				if( iIndexSize_2 < 0 ){
					sbError.append( "index dimension 2 expression (" + sRValue_index_dimensions + ") did not evaluate to a non-negative integer" );
					return null;
				} else {
					size_2 =iIndexSize_2;
				}
			}
			if( sRValue_size_3 == null ){
				// then default will be used
			} else {
				PyObject po_IndexDimensions = mInterpreter.eval( sRValue_size_3 );
				int iIndexSize_3 = Py.py2int( po_IndexDimensions );
				if( iIndexSize_3 < 0 ){
					sbError.append( "index dimension 3 expression (" + sRValue_index_dimensions + ") did not evaluate to a non-negative integer" );
					return null;
				} else {
					size_3 =iIndexSize_3;
				}
			}
			if( sRValue_size_4 == null ){
				// then default will be used
			} else {
				PyObject po_IndexDimensions = mInterpreter.eval( sRValue_size_4 );
				int iIndexSize_4 = Py.py2int( po_IndexDimensions );
				if( iIndexSize_4 < 0 ){
					sbError.append( "index dimension 4 expression (" + sRValue_index_dimensions + ") did not evaluate to a non-negative integer" );
					return null;
				} else {
					size_4 =iIndexSize_4;
				}
			}
		} catch( org.python.core.PySyntaxError parse_error ) {
			sbError.append( "syntax error processing index size settings: " + parse_error );
			return null;
		} catch( org.python.core.PyException python_error ) {
			sbError.append( "python error processing index size settings: " + python_error );
			return null;
		} catch( Throwable t ) {
			sbError.append( "while processing index size settings" );
			ApplicationController.vUnexpectedError( t, sbError );
			return null;
		}

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
	
}
