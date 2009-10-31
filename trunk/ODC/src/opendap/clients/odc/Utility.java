package opendap.clients.odc;

/**
 * Title:        Utility
 * Description:  Contains shared utility routines for all the classes in this package
 * Copyright:    Copyright (c) 2002-2008
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

import java.io.*;
import java.net.*;
import java.util.*;

public class Utility {

	private static final String SYSTEM_PROPERTY_FileSeparator = "file.separator";
	private static String msFileSeparator = System.getProperty( SYSTEM_PROPERTY_FileSeparator );

    public Utility() {}

	public static long getMemory_Max(){ return Runtime.getRuntime().maxMemory(); }
	public static long getMemory_Total(){ return Runtime.getRuntime().totalMemory(); }
	public static long getMemory_Free(){ return Runtime.getRuntime().freeMemory(); }
	public static long getMemory_Available(){
		long nMax = getMemory_Max();
		long nTotal = getMemory_Total();
		long nFree = getMemory_Free();
		return nMax - nTotal + nFree;
	}

	public static boolean zMemoryCheck( long nBytes ){
		if( getMemory_Available() > nBytes + 5000000 ){
			return true;
		} else {
			return false;
		}
	}

	public static boolean zMemoryCheck( int iBytes ){
		if( getMemory_Available() > iBytes + 5000000 ){
			return true;
		} else {
			return false;
		}
	}

	// requires a 5 megabyte buffer which is wise for a Swing application
	public static boolean zMemoryCheck( int iCount, int iWidth, StringBuffer sbError ){
		if( getMemory_Available() > iCount * iWidth + 5000000 ){
			return true;
		} else {
			sbError.append("insufficient memory (see help for how to increase memory)");
			return false;
		}
	}

	public static String  getFileSeparator(){
		return msFileSeparator;
	}

	/* returns the protocol in upper case */
	public static String url_getPROTOCOL( String sURL ){
		sURL = sURL.trim();
		int posSeparator = sURL.indexOf(':');
		if( posSeparator < 0 ) return null;
		return sURL.substring(0, posSeparator).trim().toUpperCase();
	}

	public final static int SORT_none = 0;
	public final static int SORT_ascending = 1;
	public final static int SORT_descending = 2;

	static void sortFiles( File[] afiles, int eSORT ){
		vQuickSortInternal_files( 0, afiles.length - 1, afiles, eSORT );
	}

	private static void vQuickSortInternal_files( int xLower, int xUpper, File[] buffer, int eSORT){

		int xPivot;

		int iComparisonDirection = eSORT == SORT_ascending ? 1 : -1;

	    //sort array buffer[xLower..xUpper]
		while( xLower < xUpper ){

			// quickly sort short lists by insert sort
			if( xUpper - xLower <= 12 ){
				vInsertSort( xLower, xUpper, buffer, iComparisonDirection );
				return;
			}

			// partition into two segments
			int i, j;

			// select valPivot and exchange with 1st element
			int idxPivot = xLower + ((xUpper - xLower) / 2);
			File valPivot = buffer[idxPivot];
			buffer[idxPivot] = buffer[xLower];

			// sort lngLower+1..lngUpper based on valPivot
			i = xLower + 1;
			j = xUpper;
			while( true ){
				while (i < j && valPivot.compareTo(buffer[i]) * iComparisonDirection > 0 ) i++;  // positive for ascending
				while (j >= i && buffer[j].compareTo(valPivot) * iComparisonDirection > 0 ) j--; // positive for ascending
				if( i >= j ) break;
				File valSwap = buffer[i];
				buffer[i] = buffer[j];
				buffer[j] = valSwap;
				j--;
				i++;
			}

			// valPivot belongs in buffer[j]
			buffer[xLower] = buffer[j];
			buffer[j] = valPivot;

			xPivot = j;

			// sort the smallest partition to minimize stack requirements
			if( xPivot - xLower <= xUpper - xPivot ){
                vQuickSortInternal_files( xLower, xPivot - 1, buffer, eSORT );
                xLower = xPivot + 1;
			} else {
                vQuickSortInternal_files( xPivot + 1, xUpper, buffer, eSORT );
				xUpper = xPivot - 1;
			}
		}
	}

	// Use InsertSort for short lists. Sort array Buffer(LBound to UBound)
	private static void vInsertSort( int xLBound, int xUBound, File[] buffer, int iComparisonDirection ){
		int idxHigh, idxLow;
	    File val;
		for( idxHigh = (xLBound + 1); idxHigh <= xUBound; idxHigh++ ){
		    val = buffer[idxHigh];

		    // Shift elements down until insertion point found.
            for( idxLow = (idxHigh - 1); idxLow >= xLBound; idxLow-- ){
				if( buffer[idxLow].compareTo(val) * iComparisonDirection <= 0 ) break;  // less than or equal to ascending
                buffer[idxLow + 1] = buffer[idxLow];
		    }
            buffer[idxLow + 1] = val; // do insertion
		}
	}

	static public Object oLoadObject(String sPath, StringBuffer sbError){
		try {
			File fileToBeDeserialized = new File(sPath);
			Object o = oLoadObject(fileToBeDeserialized, sbError);
			if( o == null ){
				sbError.insert(0, "Unable to load object at " + sPath + ":");
				return null;
			} else {
				return o;
			}
		} catch(Exception ex) {
			sbError.append("failed to define file for " + sPath + ": " + ex);
			return null;
		}
	}

	static public Object oLoadObject(File fileToBeDeserialized, StringBuffer sbError){
		try {
			if(fileToBeDeserialized.exists()){
				FileInputStream fis = new FileInputStream(fileToBeDeserialized);
				ObjectInputStream ois = new ObjectInputStream(fis);
				try {
					return ois.readObject();
				} catch(Exception ex) {
					sbError.append("Failed to read object from file: " + ex);
					return null;
				} finally {
					try {
						if(fis!=null) fis.close();
					} catch(Exception ex) {}
				}
			} else {
				sbError.append("File does not exist.");
				return null;
			}
		} catch(Exception ex) {
			sbError.append("unexpected error: " + ex);
			return null;
		}
	}

	static boolean zStorePreferenceObject(Object theObjectReference, String sFileName, StringBuffer sbError){
		try {
			String sPreferencesDirectory = ConfigurationManager.getInstance().getProperty_PreferencesDirectory();
			String sPath = sPreferencesDirectory + sFileName;
			File fileSerialization = new File(sPath);
			FileOutputStream fos = new FileOutputStream(fileSerialization);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			try {
				oos.writeObject(theObjectReference);
				return true;
			} catch(Exception ex) {
				sbError.append("Failed to serialize object to file [" + sPath + "]: " + ex);
				return false;
			} finally {
				try {
					if(fos!=null) fos.close();
				} catch(Exception ex) {}
			}
		} catch(Exception ex) {
			sbError.append("unexpected error: " + ex);
			return false;
		}
	}

	/** loads a CSV file into an array list of 0-based string arrays
	 *  quotation marks are double escaped
	 *  @param  sAbsolutePath       absolute path name to file
	 *  @param  iEstimatedSize      the estimated sie of the CSV file in bytes
	 *  @param  zRecognizeComments  unquoted # signs will be treated as comments
	 *  @return                     ArrayList of 0-based string arrays
	 **/
	public static ArrayList<String[]> zLoadCSV( String sAbsolutePath, int iEstimatedSize, boolean zRecognizeComments, StringBuffer sbError ){
		ArrayList<String> listLines = zLoadLines( sAbsolutePath, iEstimatedSize, sbError );
		if( listLines == null ){ sbError.insert( 0, "failed to load file: " ); return null; }
		ArrayList<String[]> listRecords = new ArrayList<String[]>( listLines.size() );
		int ctLines = listLines.size();
		for( int xLine = 1; xLine <= ctLines; xLine++ ){
			if( xLine == 130 ){
				System.out.println("130");
			}
			String sLine = (String) listLines.get( xLine - 1 );
			String[] asLine = zParseCSVLine( sLine, zRecognizeComments );
			if( asLine == null ) continue; // ignore blank lines
			listRecords.add( asLine );
		}
		return listRecords;
	}

	public static String[] zParseCSVLine( String sLine, boolean zRecognizeComments ){
		if( sLine == null )return null;
		sLine = sLine.trim();
		if( sLine.trim().length() == 0 ) return null;
		if( sLine.startsWith( "#" ) ) return null; // comment
		String[] asLine = null;
		boolean zWriting = false;
		int ctFields = 0;
		int lenLine = sLine.length();
		StringBuffer sbField = new StringBuffer(256);
		while( true ){
			int pos = 0;
			int state = 0; // beginning of field
			while( true ){
				if( pos == lenLine ){
					if( zWriting ){
						asLine[ctFields] = sbField.toString().trim();
						return asLine;
					}
					ctFields++;
					asLine = new String[ctFields];
					ctFields = 0;
					zWriting = true;
					pos = 0;
					state = 0;
					sbField.setLength(0);
				}
				char c = sLine.charAt( pos );
				switch( state ){
					case 0: // beginning of field
						if( Character.isWhitespace( c ) ) break; // ignore leading whitespace
						if( c == '"' ){ state = 1; break; } // in quoted string
						if( c == ',' ){ // end of field
							if( zWriting ){
								asLine[ctFields] = sbField.toString().trim();
								sbField.setLength(0);
							}
							ctFields++;
							state = 0;
							break;
						}
						state = 2; // in unquoted string or number
						sbField.append( c );
						break;
					case 1: // in quoted string
						if( c == '"' ){ state = 3; break; } // after quotation in quoted string
						sbField.append( c );
						break;
					case 2: // in unquoted string or number
						if( c == ',' ){
							if( zWriting ){
								asLine[ctFields] = sbField.toString().trim();
								sbField.setLength(0);
							}
							ctFields++;
							state = 0;
							break;
						}
						if(  zRecognizeComments && c == '#' ){ // comment
							pos = lenLine - 1; // skip to end of line
							break;
						}
						sbField.append( c );
						break;
					case 3: // after quotation in quoted string
						if( c == ',' ){ // end of field
							if( zWriting ){
								asLine[ctFields] = sbField.toString().trim();
								sbField.setLength(0);
							}
							ctFields++;
							state = 0;
							break;
						}
						if( c == '"' ){ sbField.append( c ); break; } // double escaped quotation
						sbField.append( '"' ); // treat the quotation as a literal even though it is violation of CSV format
						sbField.append( c );
						state = 1;
						break;
				}
				pos++;
			}
		}
	}

	// loads lines in a file into an array list of lines
	// content can include newlines if in quotation marks
	// End of Line:
	//   PC - carriage return plus line feed
	//   Macintosh - carriage return
	//   UNIX - line feed (usually called "new line" in UNIX parlance)
	// ASCII code in decimal: LF = 10; CR = 13
	public static ArrayList<String> zLoadLines( String sAbsolutePath, int iEstimatedSize, StringBuffer sbError){
		if( sAbsolutePath == null ){ sbError.append("path missing"); return null; }
		StringBuffer sbContent = new StringBuffer(iEstimatedSize);
		if( !fileLoadIntoBuffer( sAbsolutePath, sbContent, sbError ) ){
			sbError.insert(0, "error loading file (" + sAbsolutePath + "): ");
			return null;
		}
		return zLoadLines( sbContent, sbError );
	}
	
	public static ArrayList<String> zLoadLines( StringBuffer sbContent, StringBuffer sbError ){
		return zLoadLines( sbContent.toString(), sbError );
	}
		
	public static ArrayList<String> zLoadLines( String sContent, StringBuffer sbError ){ 
		int lenContent = sContent.length();
		ArrayList<String> listLines = new ArrayList<String>();
		StringBuffer sbLine = new StringBuffer(120);
		int pos = 0;
		int eState = 1;
		while( pos < lenContent ){
			char c = sContent.charAt(pos);
			switch( eState ){
				case( 1 ): // in line
					if( c == 13 ){
						eState = 2; // after CR
					} else if( c == 10 ){ // end of UNIX line
						listLines.add(sbLine.toString());
						sbLine.setLength(0);
						pos++;
					} else {
						sbLine.append(c);
					}
					pos++;
					break;
				case( 2 ): // after CR
					if( c == 10 ){ // reached end of PC line
						listLines.add(sbLine.toString());
						sbLine.setLength(0);
						pos++;
						eState = 1;
					} else if( c == 13 ){ // two mac lines in a row (stay in CR state)
						listLines.add(sbLine.toString());
						sbLine.setLength(0);
						pos++;
					} else { // single mac line
						listLines.add(sbLine.toString());
						sbLine.setLength(0);
						eState = 1;
					}
			}
		}
		if( sbLine.length() > 0 ) listLines.add(sbLine.toString()); // add any unterminated lines at end
		return listLines;
	}

	static String fileLoadIntoString( File file, StringBuffer sbError){
		StringBuffer sb = new StringBuffer(1000);
		Utility.fileLoadIntoBuffer( file.getAbsolutePath(), sb, sbError);
		return sb.toString();
	}

	static Object fileLoadIntoObject( File file, StringBuffer sbError){
		if( !file.exists() ){
			sbError.append("file not found");
			return false;
		}
		java.io.ObjectInputStream ois = null;
		try {
			java.io.FileInputStream inputstreamResource = new java.io.FileInputStream( file );
			ois = new java.io.ObjectInputStream( inputstreamResource );
		} catch(Exception ex) {
			sbError.append("unable to open file: " + ex);
			return false;
		}
		Object object = null;
		try {
			ois.readObject();
		} catch( Throwable t ) {
			sbError.append("unable to read object from file (" + file + "): " + t );
			return false;
		}
		return object;
	}
	
// does not work reliably because if a file is mapped then it is not released until
// the memory is garbage collected
//	static String fileLoadIntoString( String sAbsolutePath, StringBuffer sbError){
//		File file = new File(sAbsolutePath);
//		if( !file.exists() ){
//			sbError.append("file not found");
//			return null;
//		}
//		FileInputStream fileInputStream = null;
//		java.nio.channels.FileChannel fileChannel = null;
//		long nFileSize;
//		java.nio.MappedByteBuffer mbb;
//		try {
//			fileInputStream = new FileInputStream( file );
//			fileChannel = fileInputStream.getChannel();
//			nFileSize = fileChannel.size();
//			if( ! Utility.zMemoryCheck( nFileSize ) ){
//				sbError.append("insufficient memory to load file with " + nFileSize + " bytes");
//				return null;
//			}
//			mbb = fileChannel.map( java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, nFileSize );
//			StringBuffer sb = new StringBuffer( (int)nFileSize );
//			for( int x = 0; x < nFileSize; x++ ) sb.append( (char)mbb.get() );
//			fileChannel.close();
//			fileInputStream.close();
//			return sb.toString();
//		} catch( Throwable t ) {
//			sbError.append( "Unexpected error loading file: " + errorExtractStackTrace( t ) );
//			try {
//				if( fileChannel != null ) fileChannel.close();
//				if( fileInputStream != null ) fileInputStream.close();
//			} catch( Throwable ex ) {}
//			return null;
//		}
//	}

	static boolean fileLoadIntoBuffer( String sAbsolutePath, StringBuffer sbResource, StringBuffer sbError){
		File file = new File(sAbsolutePath);
		if( !file.exists() ){
			sbError.append("file not found");
			return false;
		}
		java.io.InputStream inputstreamResource = null;
		try {
			inputstreamResource = new FileInputStream(file);
		} catch(Exception ex) {
			sbError.append("unable to open file: " + ex);
			return false;
		}
		return zLoadString( inputstreamResource, sbResource, sbError );
	}

	/** Provide resource path (eg "resources/help.txt") */
	static boolean zLoadStringResource( String sRelativeResourcePath, StringBuffer sbResource, StringBuffer sbError){
		java.io.InputStream inputstreamResource = null;
		try {
			inputstreamResource = ApplicationController.getInstance().getClass().getResourceAsStream(sRelativeResourcePath);
		} catch(Exception ex) {
			sbError.append("failed to open resource: " + ex);
			return false;
		}
		return zLoadString( inputstreamResource, sbResource, sbError );
	}

	static boolean zLoadString( java.io.InputStream inputstreamResource, StringBuffer sbResource, StringBuffer sbError){
		if( inputstreamResource == null ){
			sbError.append("resource input stream does not exist");
			return false;
		}
		BufferedReader brFileToBeLoaded = null;
		try {
			int iFileCharacter;
			brFileToBeLoaded = new BufferedReader(new InputStreamReader(inputstreamResource));
			while(true) {
				iFileCharacter = brFileToBeLoaded.read();
				if(iFileCharacter==-1) break;
				sbResource.append((char)iFileCharacter);
			}
		} catch(Exception ex) {
			sbError.append("Failed to read resource: " + ex);
			return false;
		} finally {
			try {
				if(brFileToBeLoaded!=null) brFileToBeLoaded.close();
			} catch(Exception ex) {
				sbError.append("Failed to close resource: " + ex);
				return false;
			}
		}
		return true;
	}

	/** Extracts the stack trace as a string from an exception */
	public static String errorExtractStackTrace( Throwable theException ) {
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
	public static String errorExtractLine( Throwable theException ) {
		try {
			if( theException == null ){ return "[no exception supplied]"; }
			StringWriter sw = new StringWriter();
			theException.printStackTrace(new PrintWriter(sw));
			String sStackTrace = sw.toString();
			String sErrorLine = Utility_String.getEnclosedSubstring(sStackTrace, "(", ")");
			return theException.toString() + " (" + sErrorLine + ")";
		} catch(Exception ex) {
			return "stack trace unavailable (" + ex.toString() + ")";
		}
	}

	static String getFilenameFromURL( String sFullURL ){
	    if( sFullURL == null || sFullURL.length() == 0 ) return null;
		String sReverseURL = Utility_String.sReverse(sFullURL);
		int pos = 0;
		int len = sReverseURL.length();
		String sFileName = sFullURL;
		while(true){
			if( pos >= len ) break;
			char c = sReverseURL.charAt(pos);
			if( c == '/' || c == '\\' || c == ':' ){
				sFileName = sFullURL.substring(len-pos);
				break;
			}
			pos++;
		}
		return sFileName;
	}

	static String sFetchHttpString( String sURL, StringBuffer sbError ){
		try {
			ApplicationController.vShowStatus("resolving url... " + sURL);
			java.net.URL url = null;
			try {
				url = new URL(sURL);
			} catch( Exception ex ) {
				sbError.append("failed to resolve url [" + sURL + "]: " + ex);
				return null;
			}
			if( url == null ){
				sbError.append("url was unexpectedly null");
				return null;
			}
			ApplicationController.vShowStatus("Connecting to " + sURL + "...");
			java.net.HttpURLConnection connection = (java.net.HttpURLConnection)url.openConnection();
			connection.setAllowUserInteraction(false);
			connection.setUseCaches(false);
			connection.setDoInput(true);
			try {
				connection.connect();
				int lenContent = connection.getContentLength();
				InputStream is = connection.getInputStream();
				if( is == null ){
					sbError.append("input stream was unavailable");
					return null;
				}
				String sTotalBytes = (lenContent==-1) ? " bytes" : " of " + lenContent + " total bytes";
				byte[] abData = new byte[10000];
				StringBuffer sbData = new StringBuffer(10000);
				do{
					int iPacketCount = is.read(abData);
					if( iPacketCount == -1 ) break; // reached end of stream
					if( iPacketCount > 0 ){
						String sPacket = new String(abData, 0, iPacketCount);
						sbData.append(sPacket);
						ApplicationController.vShowStatus_NoCache("Received " + iPacketCount + sTotalBytes);
					}
				} while(true);
				ApplicationController.vShowStatus("Download complete from " + sURL);
				return sbData.toString();
			} catch( Exception ex ) {
				System.err.println("download exception: " + ex);
				sbError.append("download terminated: " + ex);
				return null;
			} finally {
				if( connection != null ){
					connection.disconnect();
					if( connection.getInputStream() !=  null ) connection.getInputStream().close();
				}
			}
		} catch(Exception ex) {
			sbError.append("Unexpected error making http download: " + ex);
			return null;
		}
	}

	final static String[] aExtensionToMIME = {
		"gif", "IMAGE/GIF",
		"jpeg", "IMAGE/JPEG",
		"jpe", "IMAGE/JPEG",
		"jpg", "IMAGE/JPEG",
		"png", "IMAGE/PNG",
		"tiff", "IMAGE/TIFF",
		"tif", "IMAGE/TIFF"
	};

	static boolean isImage( String sURL ){
		String sExtension = Utility.getExtension(sURL);
		if( sExtension == null ) return false;
		for( int xExtension = 0; xExtension < aExtensionToMIME.length/2; xExtension++ ){
			if( aExtensionToMIME[xExtension*2].equalsIgnoreCase(sExtension) )
			    return true;
		}
		return false;
	}

	static boolean isDodsURL( String sURL ){
		if( sURL == null ) return false;
		if( sURL.toUpperCase().indexOf("NPH-") < 0 ) return false;
		return true;
	}

	static String getMIMEtype( String sURL ){
		String sExtension = Utility.getExtension(sURL);
		if( sExtension == null ) return null;
		for( int xExtension = 0; xExtension < aExtensionToMIME.length/2; xExtension++ ){
			if( aExtensionToMIME[xExtension*2].equalsIgnoreCase(sExtension) )
			    return aExtensionToMIME[xExtension*2 + 1];
		}
		return null;
	}

	static String getExtension( String sFileName ){
		if( sFileName == null ) return null;
		String sReverseFileName = Utility_String.sReverse(sFileName);
		int posDot = sReverseFileName.indexOf('.');
		if( posDot == -1 ) return null;
		String sPossibleExtension = sFileName.substring(sFileName.length() - posDot);
		if( sPossibleExtension.indexOf(msFileSeparator) == -1 ){
			return sPossibleExtension;
		} else {
			return null; // if a slash is in there it means the dot was upstream from the file name
		}
	}

	public static String sListDoubles1( double[] ad1, String sSeperator ){
		StringBuffer sb = new StringBuffer(100);
		for( int x = 1; x < ad1.length; x++ ){
			if( x > 1 ) sb.append(sSeperator);
			sb.append( ad1[x] );
		}
		return sb.toString();
	}

	public static String sFriendlyFileName( String sName ){
		return sValidIdentifier( sName, true );
	}

	public static String sValidIdentifier( String sName, boolean zAllowInitialNumeral ){
		try {
			if( sName == null ) return "unknown";
			if( sName.length() == 0 ) return "unknown";
			if( sName.length() > 32 ) sName = sName.substring(0, 32);
			StringBuffer sb = new StringBuffer(sName.length());
			char c = sName.charAt(0);
			if( (c >= '0' && c<= '9' && zAllowInitialNumeral) || (c >= 'A' && c<= 'Z') || (c >= 'a' && c <= 'z') || c == '~' || c == '-' ){
				sb.append(c);
			} else {
				sb.append('_');
			}
			for( int xChar = 1; xChar < sName.length(); xChar++ ){
				c = sName.charAt(xChar);
				if( (c >= '0' && c<= '9') || (c >= 'A' && c<= 'Z') || (c >= 'a' && c <= 'z') || c == '~' || c == '-' ){
					sb.append(c);
				} else {
					sb.append('_');
				}
			}
			return sb.toString();
		} catch(Exception ex) { // should not happen
			return "unknown_err";
		}
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

	static File fileDefine( String sDirectory, String sFileName, StringBuffer sbError ){
		try {
			String sPath = Utility.sConnectPaths( sDirectory, sFileName );
			File file = new File( sPath );
			return file;
		} catch( Throwable t ) {
			sbError.append("error: " +  t);
			return null;
		}
	}

	static javax.swing.JFileChooser jfc = null;
	/** opens a dialog to save a file
	 *  if the error buffer is blank and file is null then the user cancelled the operation
	 *  @param sSuggestedFileName  example: "processing_script.py", null is ok
	 *  @param sDirectory  default directory to save to, terminator optional
	 *  @return File  the file to which the user saved */
	static final File fileSaveAs( java.awt.Component componentParent, String sTitle, String sDirectory, String sSuggestedFileName, String sContent, StringBuffer sbError ){
		try {

			// ask user for desired location
			File fileDirectory = Utility.fileEstablishDirectory( sDirectory, sbError );
			if (jfc == null) jfc = new javax.swing.JFileChooser();
			if( fileDirectory == null ){
				// no default directory
			} else {
				jfc.setCurrentDirectory( fileDirectory );
			}
			if( sSuggestedFileName != null && sSuggestedFileName.length() != 0 ){
				jfc.setSelectedFile(new File(sSuggestedFileName));
			}
			int iState = jfc.showDialog( componentParent, sTitle );
			File file = jfc.getSelectedFile();
			if (file == null || iState != javax.swing.JFileChooser.APPROVE_OPTION) return null; // user cancel
			if( ! fileSave( file, sContent, sbError ) ){
				sbError.insert( 0, "failed to save [" + file + "]: " );
				return null;
			}
			return file;
		} catch(Exception ex) {
			sbError.append( "unexpected error: " + ex );
			return null;
		}
	}

	static final File fileSaveAs( java.awt.Component componentParent, String sTitle, String sDirectory, String sSuggestedFileName, java.io.Serializable content, StringBuffer sbError ){
		try {

			// ask user for desired location
			File fileDirectory = Utility.fileEstablishDirectory( sDirectory, sbError );
			if (jfc == null) jfc = new javax.swing.JFileChooser();
			if( fileDirectory == null ){
				// no default directory
			} else {
				jfc.setCurrentDirectory( fileDirectory );
			}
			if( sSuggestedFileName != null && sSuggestedFileName.length() != 0 ){
				jfc.setSelectedFile(new File(sSuggestedFileName));
			}
			int iState = jfc.showDialog( componentParent, sTitle );
			File file = jfc.getSelectedFile();
			if (file == null || iState != javax.swing.JFileChooser.APPROVE_OPTION) return null; // user cancel
			if( ! fileSave( file, content, sbError ) ){
				sbError.insert( 0, "failed to save [" + file + "]: " );
				return null;
			}
			return file;
		} catch(Exception ex) {
			sbError.append( "unexpected error: " + ex );
			return null;
		}
	}
	
	public static boolean fileSave( String sAbsolutePath, String sContent, StringBuffer sbError){
		File file = new File(sAbsolutePath);
		return fileSave( file, sContent, sbError );
	}

	public static boolean fileSave(  File file, java.io.Serializable sContent, StringBuffer sbError ){

		// if file does not exist, create it
		try {
			if( ! file.exists() ){
				file.createNewFile();
			}
		} catch( Exception ex ) {
			sbError.append("failed to create file " + file + ": " + ex );
			return false;
		}


		// open file
		java.io.FileOutputStream fos = null;
		try {
		    fos = new java.io.FileOutputStream(file);
			if( fos == null ){
				sbError.append("failed to open file, empty stream");
				return false;
			}
		} catch(Exception ex) {
			sbError.append("failed to open file for writing: " + ex);
			return false;
		}

		// save to file
		try {
			ObjectOutputStream oos = new ObjectOutputStream( fos );
			oos.writeObject( sContent );
		} catch(Exception ex) {
			ApplicationController.vShowError("object write failure: " + ex);
			return false;
		} finally {
			try {
				if( fos!=null ) fos.close();
			} catch(Exception ex) {}
		}
		return true;
	}
	
	public static boolean fileSave(  File file, String sContent, StringBuffer sbError ){

		// if file does not exist, create it
		try {
			if( ! file.exists() ){
				file.createNewFile();
			}
		} catch( Exception ex ) {
			sbError.append("failed to create file " + file + ": " + ex );
			return false;
		}


		// open file
		java.io.FileOutputStream fos = null;
		try {
		    fos = new java.io.FileOutputStream(file);
			if( fos == null ){
				sbError.append("failed to open file, empty stream");
				return false;
			}
		} catch(Exception ex) {
			sbError.append("failed to open file for writing: " + ex);
			return false;
		}

		// save to file
		try {
			fos.write( sContent.getBytes() );
		} catch(Exception ex) {
			ApplicationController.vShowError("write failure (" + sContent.length() + " bytes): " + ex);
			return false;
		} finally {
			try {
				if( fos!=null ) fos.close();
			} catch(Exception ex) {}
		}
		return true;
	}

	/* 1.4.1 only
	public static boolean zChannelCopy( ReadableByteChannel rbcSource, WritableByteChannel wbcDestination, StringBuffer sbError ){
		try {
			ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
			while( rbcSource.read(buffer) != -1 ){
				buffer.flip();
				wbcDestination.write(buffer);
				buffer.compact();
			}
			buffer.flip();
			while( buffer.hasRemaining() ){
				wbcDestination.write(buffer);
			}
			return true;
		} catch(Exception ex) {
			sbError.append("i/o error: " + ex);
			return false;
		}
	}
	*/

	/** rounds a double to the nearest integer */
	public static int round( double d ){
		return (int)round( d, 0 );
	}

	/** the order is the power of ten, for example order 0 is rounding to the
	 *  nearest integer; order 2 rounds to the nearest 100; order -3 rounds to
	 *  the nearest 0.001 etc
	 */
	public static double round( double d, int iOrder ){
		double dDivisor = Math.pow(10, iOrder);
		double dRemainder = d % dDivisor;
		if( dRemainder == 0 ) return d;
		if( d > 0 ){
			if( dRemainder >= dDivisor / 2 ){ // round up
				return d - dRemainder + dDivisor;
			} else { // round down
				return d - dRemainder;
			}
		} else {
			if( dRemainder*-1 >= dDivisor / 2 ){ // round down
				return d - dRemainder - dDivisor;
			} else {
				return d - dRemainder;
			}
		}
	}

	/** rounds absolutely down (not towards zero) */
	public static double round_down( double d, int iOrder ){
		double dDivisor = Math.pow(10, iOrder);
		double dRemainder = d % dDivisor;
		if( dRemainder == 0 ) return d;
		if( d < 0 ){
			return d - dRemainder - dDivisor;
		} else {
			return d - dRemainder;
		}
	}

	/** rounds absolutely up (not towards zero) */
	public static double round_up( double d, int iOrder ){
		double dDivisor = Math.pow(10, iOrder);
		double dRemainder = d % dDivisor;
		if( dRemainder == 0 ) return d;
		if( d < 0 ){
			return d - dRemainder;
		} else {
			return d - dRemainder + dDivisor;
		}
	}

	// number of decimal places, 0 means integer
	public static String sDoubleToRoundedString( double d, int iDecimalPlaces ){
		if( Double.isNaN(d) ) return "NaN";
		if( Double.isInfinite(d) ) return "\u221E";
		if( iDecimalPlaces > 0 ){
			long nIntegerPortion = (long)d;
			double dDecimalPortion = d - nIntegerPortion;
			double dMultiplier = (long)Math.pow(10, iDecimalPlaces);
			double dRoundedPortion = Math.round(dDecimalPortion * dMultiplier)/dMultiplier;
			double dRounded = (double)nIntegerPortion + dRoundedPortion;
			return Double.toString(dRounded);
		} else {
			long n = Math.round(d);
			return Long.toString(n);
		}
	}

	static double[] aiInvert( double[] ai, int x_len, int y_len ){
		double[] adTransformedValues0 = new double[ai.length];
		int xValue = -1;
		for( int xY = 1; xY <= y_len; xY++ ){
			for( int xX = 1; xX <= x_len; xX++ ){
				xValue++;
				adTransformedValues0[y_len*(xX-1) + xY - 1] = ai[xValue];
				//adTransformedValues0[(lenX_real-xX_real)*lenY_real + xY_real - 1] = adValues0[xValue];
			}
		}
		return adTransformedValues0;
	}

	public static boolean equalsUPPER( String s1, String s2 ){
		if( s1 == null || s2 == null ) return false;
		return s1.equalsIgnoreCase(s2);
	}
	public final static boolean zNullOrBlank( String s ){
		if( s == null ) return true;
		if( s.trim().length() == 0 ) return true;
		return false;
	}

	public static void soundPlay(File filePath){
		try {
			if( filePath == null ) return;
			javax.sound.sampled.AudioInputStream isAudio = javax.sound.sampled.AudioSystem.getAudioInputStream(filePath);
			if (isAudio != null){
				int iLoopCount = 1;
				javax.sound.sampled.AudioFormat format = isAudio.getFormat();
				javax.sound.sampled.DataLine.Info info = new javax.sound.sampled.DataLine.Info(javax.sound.sampled.Clip.class, format);
				javax.sound.sampled.Clip clip = (javax.sound.sampled.Clip) javax.sound.sampled.AudioSystem.getLine(info);
				clip.open(isAudio);
				clip.loop(iLoopCount);
				clip.close();
			}
		} catch(Exception ex) {
			// do nothing
		}
	}

	public static boolean zDirectoryValidate( String sPath, StringBuffer sbError ){
		File fileDirectory;
		String sCanonicalPath;
		try {
		    fileDirectory = new File( sPath );
		} catch(Exception ex) {
			sbError.append("directory string could not be interpreted as a path (" + Utility_String.sSafeSubstring(sPath, 0, 80) + ")");
			return false;
		}
		try {
		    sCanonicalPath = fileDirectory.getCanonicalPath();
			if( sCanonicalPath == null || sCanonicalPath.length() == 0 ){
			   sbError.append("could not obtain canonical path for base directory (" + Utility_String.sSafeSubstring(sPath, 0, 80) + ")");
			   return false;
			}
		} catch(Exception ex) {
			sbError.append("error obtaining canonical path for directory (" + Utility_String.sSafeSubstring(sPath, 0, 80) + "): " + ex);
			return false;
		}
		if( !fileDirectory.isDirectory() ){
			sbError.append("path does not resolve to a directory: " + sCanonicalPath);
			return false;
		}
		if( !fileDirectory.exists() ){
			sbError.append("directory does not exist: " + sCanonicalPath);
			return false;
		}
		return true;
	}

	/** this is located here because it can be a time-consuming action to determine the fonts */
	static String[] AS_System_Fonts = null;
	public static String[] getFontFamilies(){
		if( AS_System_Fonts != null ) return AS_System_Fonts;
		java.awt.GraphicsEnvironment gEnv = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
		AS_System_Fonts = gEnv.getAvailableFontFamilyNames();
		if( AS_System_Fonts == null ){ // should not happen
			AS_System_Fonts = new String[8];
			AS_System_Fonts[0] = "Serif";
			AS_System_Fonts[1] = "Sans-Serif";
		    AS_System_Fonts[2] = "Monospaced";
		    AS_System_Fonts[3] = "Dialog";
		    AS_System_Fonts[4] = "Dialog Input";
		    AS_System_Fonts[5] = "Lucida Bright";
		    AS_System_Fonts[6] = "Lucida Sans";
		    AS_System_Fonts[7] = "Lucida Sans Typewriter";
		}
        return AS_System_Fonts;
	}

	public static String getCurrentDate(String sFormat){
		try {
			Calendar c = Calendar.getInstance();
			java.text.SimpleDateFormat df = new java.text.SimpleDateFormat( sFormat );
			return df.format(c.getTime());
		} catch(Exception ex) {
			return "[error]";
		}
	}

	public static void vCenterComponent( java.awt.Component component, int widthComponent, int heightComponent, int widthContainer, int heightContainer ){
		component.setBounds(widthContainer/2 - (widthComponent/2), heightContainer/2 - (heightComponent/2), widthComponent, heightComponent);
	}

	public static File fileEstablishDirectory( String sPath, StringBuffer sbError ){
		try {
			File fileDirectory = null;
			if (sPath == null) {
				return null;
			} else {
				fileDirectory = new File(sPath);
				if (!fileDirectory.exists()) { // try to create it
					boolean zDirectoryCreated = fileDirectory.mkdirs();
					if (!zDirectoryCreated) {
						return null;
					}
					if (!fileDirectory.isDirectory()) {
						return null;
					}
				}
				return fileDirectory;
			}
		} catch(Exception ex) {
			sbError.append( Utility.errorExtractStackTrace( ex ) );
			return null;
		}
	}

	final public static String sGetDODSError( String sPageText ){
		int posError = sPageText.indexOf( "Error {" );
		int posMessage = sPageText.indexOf( "message = \"" );
	 	int posTermination = sPageText.indexOf( "\";\n};" );
		if( posError == -1 || posMessage == -1 || posTermination == -1 ) return null;
		if( ! ( posError < posMessage && posMessage < posTermination ) ) return null;
		posMessage = posMessage + 11; /* move start past initial quote */
		int iMessageLength = posTermination - posMessage;
		String sMessage = sPageText.substring( posMessage, posMessage + iMessageLength );
		return sMessage;
	}

	static Java2Clipboard mClipboardOwner;
	final public static void vCopyToClipboard( String sText ){
		if( mClipboardOwner == null ) mClipboardOwner = new Java2Clipboard();
		mClipboardOwner.vCopy( sText );
	}

	public static void vCenterWindow( java.awt.Window w ) {

		java.awt.Rectangle rectWindow = w.getBounds();
        java.awt.Toolkit toolkit = w.getToolkit();
        java.awt.Dimension dimensionScreen = toolkit.getScreenSize();
        w.setLocation(
            (dimensionScreen.width - rectWindow.width)/2,
            (dimensionScreen.height - rectWindow.height)/2 );

    }

	public static double difference( double v1, double v2 ){ return v1 - v2 > 0 ? v1 - v2 : v2 - v1; }
	public static float difference( float v1, float v2 ){ return v1 - v2 > 0 ? v1 - v2 : v2 - v1; }
	public static int difference( int v1, int v2 ){ return v1 - v2 > 0 ? v1 - v2 : v2 - v1; }
	public static long difference( long v1, long v2 ){ return v1 - v2 > 0 ? v1 - v2 : v2 - v1; }

}

class Java2Clipboard implements java.awt.datatransfer.ClipboardOwner {

    public void vCopy( String sText ) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			try {
				sm.checkSystemClipboardAccess();
			} catch ( Exception e ) {
				ApplicationController.vShowError( "unable to access clipboard (may be in browser or in an operating system with no clipboard or a protected clipboard" );
				return;
			}
        }
		java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
		java.awt.datatransfer.Clipboard clipboard = toolkit.getSystemClipboard();
		java.awt.datatransfer.StringSelection string_selection = new java.awt.datatransfer.StringSelection( sText );
		clipboard.setContents( string_selection, this );
    }

    public void lostOwnership( java.awt.datatransfer.Clipboard clip, java.awt.datatransfer.Transferable tr ){
		// do nothing
    }
}

class FavoritesFilter implements FilenameFilter {
	public boolean accept( File fileDirectory, String sName ){
		return sName.startsWith("favorite-");
	}
}

class RecentFilter implements FilenameFilter {
	public boolean accept( File fileDirectory, String sName ){
		return sName.startsWith("recent-");
	}
}



