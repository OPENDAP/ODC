package opendap.clients.odc;

/**
 * Title:        Output_ToDatabase
 * Description:  Methods to generate output to a database
 * Copyright:    Copyright (c) 2002
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.17
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

import opendap.dap.DataDDS;
import java.sql.Connection;
import java.sql.DriverManager;

public class Output_ToDatabase {
	Output_ToDatabase(){}
	public boolean zWriteToDatabase( DataDDS ddds, String sDriverClass, String sURL, String sUID, String sPassword, StringBuffer sbError ){
		if( ddds == null ){ sbError.append("no data supplied"); return false; }
		if( sDriverClass == null ){ sbError.append("no driver class specified"); return false; }
		if( sURL == null ){ sbError.append("no URL specified"); return false; }
		try {
			Class.forName(sDriverClass); // load driver
		} catch(Exception ex) {
			sbError.append("driver class not found [" + sDriverClass + "]");
			return false;
		}
		Connection con = null;
		try {
			if( sUID == null || sPassword == null ){
				con = DriverManager.getConnection(sURL);
			} else {
				con = DriverManager.getConnection(sURL, sUID, sPassword);
			}
		} catch(Exception ex) {
			sbError.append("error writing to database: " + ex);
			return false;
		} finally {
			try {
				if( con != null ) con.close();
			} catch(Exception ex) {
				ApplicationController.getInstance().vShowWarning("error closing connection: " + ex);
			}
		}
		return true;
	}

}


