package opendap.clients.odc.geo;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Utility {
	public static boolean DEBUG = true;
	private static final String SYSTEM_PROPERTY_FileSeparator = "file.separator";
	static String msFileSeparator = System.getProperty( SYSTEM_PROPERTY_FileSeparator );
	public static void vUnexpectedError( Throwable t, StringBuffer sbError ){
		String sErrorMessage = DEBUG ? extractStackTrace(t) : Utility.extractErrorLine(t);
		if( sbError == null ){
			System.err.println("(no buffer supplied for the following error:)");
			System.err.println(sErrorMessage);
		} else {
			sbError.append(": ").append(sErrorMessage);
		}
	}
	/** Extracts the stack trace as a string from an exception */
	public static String extractStackTrace( Throwable theException ){
		try {
			if( theException == null ){ return "[no exception supplied]"; }
			StringWriter sw = new StringWriter();
			theException.printStackTrace(new PrintWriter(sw));
			return sw.toString();
		} catch(Exception ex) {
			return "stack trace unavailable (" + ex.toString() + ")";
		}
	}

	/** Extracts the stack trace as a string from an exception */
	public static String extractErrorLine( Throwable theException ){
		try {
			if( theException == null ){ return "[no exception supplied]"; }
			StringWriter sw = new StringWriter();
			theException.printStackTrace(new PrintWriter(sw));
			String sStackTrace = sw.toString();
			String sErrorLine = Utility.getEnclosedSubstring(sStackTrace, "(", ")");
			return theException.toString() + " (" + sErrorLine + ")";
		} catch(Exception ex) {
			return "stack trace unavailable (" + ex.toString() + ")";
		}
	}
	/** Returns the text in between two strings within a string.
	 *     - if left frame is null, returns everything to left of right frame
	 *     - if right frame is null, returns everything to right of left frame
	 *     - only the first valid enclosure is returned
	 *     - if either side of the enclosure is missing, null is returned
	 *  Example: f("my dear (sic) sir", "(", ")" ) returns "sic"
	 */
	public static String getEnclosedSubstring( String s, String sLeft, String sRight ){
		if( s == null ) return null;
		if( (sLeft == null || sLeft.length() == 0) && (sRight == null || sRight.length() == 0) ) return s;
		if( (sLeft == null || sLeft.length() == 0) ){
			int pos = s.indexOf(sRight);
			if( pos == -1 ) return null;
			return s.substring(0, pos);
		}
		if( (sRight == null || sRight.length() == 0) ){
			int pos = s.indexOf(sLeft);
			if( pos == -1 ) return null;
			return s.substring(pos + sLeft.length());
		}
		int posLeft = s.indexOf(sLeft);
		if( posLeft == -1 ) return null;
		int posRight = s.indexOf(sRight, posLeft + sLeft.length());
		if( posRight == -1 ) return null;
		return s.substring(posLeft + sLeft.length(), posRight);
	}
	public static String sConnectPaths(String sPrePath, String sPostPath){
		return sConnectPaths(sPrePath, msFileSeparator, sPostPath);
	}
	/** Takes two path fragments and puts them together making sure only a single slash separates them. Examples:<br>
	 *    null, null returns "/"
	 *    "A", null  returns "A"
	 *    "A", "B"   returns "A/B"
	 *    null, "B"  returns "/B"
	 *    "A/", "B"  returns "A/B"
	 *    "A/", "/B" returns "A/B"  */
	public static String sConnectPaths(String sPrePath, String sPathSeparator, String sPostPath){
		if( sPathSeparator == null ) sPathSeparator = msFileSeparator;
		if(sPrePath == null){
			if(sPostPath == null){
				return sPathSeparator;
			} else {
				if(sPostPath.startsWith(sPathSeparator)){
					return sPostPath;
				} else {
					return sPathSeparator + sPostPath;
				}
			}
		} else {
			if(sPostPath == null){
				return sPrePath;
			} else {
				if(sPostPath.startsWith(sPathSeparator)){
					if(sPrePath.endsWith(sPathSeparator)){
						return sPrePath + sPostPath.substring(1);
					} else {
						return sPrePath + sPostPath;
					}
				} else {
					if(sPrePath.endsWith(sPathSeparator)){
						return sPrePath + sPostPath;
					} else {
						return sPrePath + sPathSeparator + sPostPath;
					}
				}
			}
		}
	}


}
