package opendap.clients.odc;

import java.util.ArrayList;

public class API {
	public String help(){
		StringBuffer sb = new StringBuffer(10000);
		sb.append("dumps()   ArrayList<String> list of dumps (used in debugging)");
		return sb.toString();
	}
	public String help( String sFunction ){
		if( sFunction.equalsIgnoreCase( "dumps" ) ){
			return "Returns an ArrayList<String> containing dump text; some errors when they occur will add a body of text to the dump list and quote the dump index number in the error message. This is done when the dump text might be too large for insertion into the error message. The developer or user can later retrieve the dump text from the array.";
		} else {
			return "unknown function name (" + sFunction + ")";
		}
	}
	public ArrayList<String> dumps(){ return ApplicationController.getInstance().listDumps; }
}
