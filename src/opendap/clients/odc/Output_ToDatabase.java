package opendap.clients.odc;

/**
 * Title:        Output_ToDatabase
 * Description:  Methods to generate output to a database
 * Copyright:    Copyright (c) 2002
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.17
 */

import opendap.dap.DataDDS;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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


