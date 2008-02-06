package opendap.clients.odc;

/**
 * Title:        Interpreter
 * Description:  Python interpreter capability
 * Copyright:    Copyright (c) 2007-8
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.00
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

import org.python.util.PythonInterpreter;
import org.python.core.*;

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
			String sScriptDirectory = ConfigurationManager.getInstance().getProperty_DIR_Scripts();
			String sScriptDirectory_quoted = '"' + sScriptDirectory + '"';
			sScriptDirectory_quoted = Utility.sReplaceString( sScriptDirectory_quoted, "\\", "\\\\");
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

}
