package opendap.clients.odc;

/**
 * Title:        Interpreter
 * Description:  Python interpreter capability
 * Copyright:    Copyright (c) 2007-11
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.07
 */

/////////////////////////////////////////////////////////////////////////////
// This file is part of the OPeNDAP Data Connector project.
//
// Copyright (c) 2007-9 OPeNDAP, Inc.
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

import opendap.clients.odc.data.Model_Dataset;
import opendap.clients.odc.data.Model_PlottableExpression;
import opendap.clients.odc.data.Script;
import opendap.dap.DArray;
import opendap.dap.DGrid;
import opendap.dap.DStructure;

import org.python.util.PythonInterpreter;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyCode;

import java.util.ArrayList;
import java.util.HashMap;

public class Interpreter {
	private PythonInterpreter mInterpreter = null;
	private String msPrompt = ">>> ";
	private java.io.OutputStream streamTrace = null;

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
			mInterpreter.exec("import site");
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
			vWriteLine( os, "!python syntax error: " + parse_error, msPrompt );
		} catch( org.python.core.PyException python_error ) {
			vWriteLine( os, "!python error: " + python_error, msPrompt );
		} catch( Throwable t ) {
			System.out.println("error: " + t.getClass().getName());
			ApplicationController.vUnexpectedError( t, sbError );
			return false;
		}
		return true;
	}

	public boolean zExecute( String sCommand, StringBuffer sbError ){
		if( sCommand == null ){
			sbError.append( "null sExpression to exec" );
			return false;
		}
		if( mInterpreter == null ){
			sbError.append("no Python interpreter exists");
			return false;
		}
		try {
			mInterpreter.exec( sCommand );
		} catch( org.python.core.PySyntaxError parse_error ) {
			sbError.append( "!python syntax error: " + parse_error );
			return false;
		} catch( org.python.core.PyException python_error ) {
			sbError.append( "!python error: " + python_error );
			return false;
		} catch( Throwable t ) {
			sbError.append( "interpreter error: " + t.getClass().getName() );
			ApplicationController.vUnexpectedError( t, sbError );
			return false;
		}
		return true;
	}

	public PyObject zEval( PyCode code, StringBuffer sbError ){
		if( code == null ){
			sbError.append( "null code object to eval" );
			return null;
		}
		if( mInterpreter == null ){
			sbError.append("no Python interpreter exists for evaluation");
			return null;
		}
		try {
			PyObject pyobject = mInterpreter.eval( code );
			return pyobject;
		} catch( org.python.core.PySyntaxError parse_error ) {
			sbError.append( "!python syntax error: " + parse_error );
			return null;
		} catch( org.python.core.PyException python_error ) {
			sbError.append( "!python error: " + python_error );
			return null;
		} catch( Throwable t ) {
			sbError.append( "interpreter error: " + t.getClass().getName() );
			ApplicationController.vUnexpectedError( t, sbError );
			return null;
		}
	}
	
	public PyObject zEval( String sExpression, StringBuffer sbError ){
		if( sExpression == null ){
			sbError.append( "null sExpression to eval" );
			return null;
		}
		if( mInterpreter == null ){
			sbError.append("no Python interpreter exists for evaluation");
			return null;
		}
		try {
			PyObject pyobject = mInterpreter.eval( sExpression );
			return pyobject;
		} catch( org.python.core.PySyntaxError parse_error ) {
			sbError.append( "!python syntax error: " + parse_error );
			return null;
		} catch( org.python.core.PyException python_error ) {
			sbError.append( "!python error: " + python_error );
			return null;
		} catch( Throwable t ) {
			sbError.append( "interpreter error: " + t.getClass().getName() );
			ApplicationController.vUnexpectedError( t, sbError );
			return null;
		}
	}
	
	public boolean zSet( String sVariableName, Object oRValue, StringBuffer sbError ){
		if( sVariableName == null ){
			ApplicationController.vShowWarning( "null variable name to exec" );
		}
		if( mInterpreter == null ){
			sbError.append("no Python interpreter exists for set");
			return false;
		}
		try {
			mInterpreter.set( sVariableName, oRValue );
			return true;
		} catch( org.python.core.PySyntaxError parse_error ) {
			sbError.append( "!python syntax error: " + parse_error );
			return false;
		} catch( org.python.core.PyException python_error ) {
			sbError.append( "!python error: " + python_error );
			return false;
		} catch( Throwable t ) {
			sbError.append( "interpreter error: " + t.getClass().getName() );
			ApplicationController.vUnexpectedError( t, sbError );
			return false;
		}
	}

	public double get( String sVariableName, StringBuffer sbError ){
		if( sVariableName == null ){
			sbError.append( "null variable name" );
			return Double.NEGATIVE_INFINITY;
		}
		if( mInterpreter == null ){
			sbError.append("no Python interpreter exists for set");
			return Double.NEGATIVE_INFINITY;
		}
		try {
			PyObject object = mInterpreter.get( sVariableName );
			return object.asDouble();
		} catch( org.python.core.PySyntaxError parse_error ) {
			sbError.append( "!python syntax error: " + parse_error );
			return Double.NEGATIVE_INFINITY;
		} catch( org.python.core.PyException python_error ) {
			sbError.append( "!python error: " + python_error );
			return Double.NEGATIVE_INFINITY;
		} catch( Throwable t ) {
			sbError.append( "interpreter error: " + t.getClass().getName() );
			ApplicationController.vUnexpectedError( t, sbError );
			return Double.NEGATIVE_INFINITY;
		}
	}

	public PyObject getInteger( String sVariableName, StringBuffer sbError ){
		if( sVariableName == null ){
			sbError.append( "null variable name" );
			return null;
		}
		if( mInterpreter == null ){
			sbError.append("no Python interpreter exists for set");
			return null;
		}
		try {
			PyObject object = mInterpreter.get( sVariableName );
			return object;
		} catch( org.python.core.PySyntaxError parse_error ) {
			sbError.append( "!python syntax error: " + parse_error );
			return null;
		} catch( org.python.core.PyException python_error ) {
			sbError.append( "!python error: " + python_error );
			return null;
		} catch( Throwable t ) {
			sbError.append( "interpreter error: " + t.getClass().getName() );
			ApplicationController.vUnexpectedError( t, sbError );
			return null;
		}
	}
	
	public void vEnterCommand( String s ){
		vWriteLine( ApplicationController.getInstance().getTextViewerOS(), s, msPrompt );
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
	
	public Script generateScriptFromText( String sScriptText, StringBuffer sbError ){
		if( sScriptText == null ){
			sbError.append( "script text missing" );
			return null;
		}
		streamTrace = ApplicationController.getInstance().getTextViewerOS();
		Script script = Script.create( sScriptText, streamTrace, sbError );
		if( script == null ){
			sbError.insert( 0, "failed to parse script: " );
			return null;
		}
		return script;
	}
	
}
	
