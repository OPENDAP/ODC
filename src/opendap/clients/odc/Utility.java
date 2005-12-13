package opendap.clients.odc;

/**
 * Title:        Utility
 * Description:  Contains shared utility routines for all the classes in this package
 * Copyright:    Copyright (c) 2002-2004
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.51
 */

import java.io.*;
import java.net.*;
import java.awt.Image;
import java.util.*;

public class Utility {

	private static final String SYSTEM_PROPERTY_FileSeparator = "file.separator";
	private static String msFileSeparator = System.getProperty(SYSTEM_PROPERTY_FileSeparator);

    public Utility() {}

	// requires a 5 megabyte buffer which is wise for a Swing application
	public static boolean zMemoryCheck( int iCount, int iWidth, StringBuffer sbError ){
		if( ApplicationController.getMemory_Available() > iCount * iWidth + 5000000 ){
			return true;
		} else {
			sbError.append("insufficient memory (see help for how to increase memory)");
			return false;
		}
	}

	public static boolean activateURL( String sURL, StringBuffer sbError ){
		try {
			Runtime.getRuntime().exec("start " + sURL);
			return true;
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	public static String getByteCountString( long nBytes ){
		char cOrder;
		int intSz;
		if( nBytes < 1000 ){
			intSz = (int)nBytes;
			cOrder = 'B';
		} else
		if( nBytes < 0x100000 ){ // kilobytes
			intSz = (int)(nBytes / 1024);
			cOrder = 'K';
		} else
		if( nBytes < 0x40000000 ){ // megabytes
			intSz = (int)(nBytes / 0x100000);
			cOrder = 'M';
		} else {
			intSz = (int)(nBytes / 0x40000000);
			cOrder = 'G';
		}
		return Utility.sFixedWidth(Integer.toString(intSz), 3, '0', Utility.ALIGNMENT_RIGHT) + cOrder;
	}

	public static void iconAdd( javax.swing.JFrame theFrame ){
		try {
			String sIconPath;
			String sIconSize = ConfigurationManager.getInstance().getProperty_DISPLAY_IconSize();
			if( sIconSize.equals("16") ){
				sIconPath = "icons/icon-16.gif";
			} else if( sIconSize.equals("24") ){
				sIconPath = "icons/icon-24.gif";
			} else if( sIconSize.equals("32") ){
				sIconPath = "icons/icon-32.gif";
			} else {
				sIconPath = "icons/icon-32.gif";
				ApplicationController.getInstance().vShowError("Unknown icon size [" + sIconSize + "]. Supported sizes are \"16\", \"24\" and \"32\".");
			}
			theFrame.setIconImage(Utility.imageiconLoadResource(sIconPath).getImage());
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowError("Unexpected error setting up icon. Default icon will appear.");
		}
	}

	static Image imageIndicator_Granule;
	static Image imageIndicator_Directory;
	static Image imageIndicator_Catalog;
	static Image imageIndicator_Binary;
	static Image imageIndicator_Image;
	static Image imageConstrained;
	public static boolean zLoadIcons(StringBuffer sbError){
		String sPath = "icons/";
		try {
			imageIndicator_Granule = Utility.imageiconLoadResource(sPath + "dataset-granule.gif").getImage();
			imageIndicator_Directory = Utility.imageiconLoadResource(sPath + "dataset-directory.gif").getImage();
			imageIndicator_Catalog = Utility.imageiconLoadResource(sPath + "dataset-catalog.gif").getImage();
			imageIndicator_Binary = Utility.imageiconLoadResource(sPath + "dataset-binary.gif").getImage();
			imageIndicator_Image = Utility.imageiconLoadResource(sPath + "dataset-image.gif").getImage();
			imageConstrained = Utility.imageiconLoadResource(sPath + "constrained.gif").getImage();
			return true;
		} catch(Exception ex) {
			sbError.append("Icons not found in path " + sPath);
			return false;
		}
	}

	public static String  getFileSeparator(){
		return msFileSeparator;
	}

	public static void vUnexpectedError( Throwable ex, String sMessage ){
		if( ApplicationController.DEBUG ){
			ApplicationController.vShowError(sMessage + ":\n" + Utility.extractStackTrace(ex));
		} else {
			ApplicationController.vShowError(sMessage + ": " + Utility.extractErrorLine(ex));
		}
	}

	public static void vUnexpectedError( Throwable ex, StringBuffer sbError ){
		String sErrorMessage = ApplicationController.DEBUG ? Utility.extractStackTrace(ex) : Utility.extractErrorLine(ex);
		if( sbError == null ){
			System.err.println("(no buffer supplied for the following error:)");
			System.err.println(sErrorMessage);
		} else {
			sbError.append(": ").append(sErrorMessage);
		}
	}

	public static String sEscapeURL( String sRawURL ){
		String sReplacePlus = sReplaceString( sRawURL, "+", "%2B" );
		String sReplaceSpace = sReplaceString( sReplacePlus, " ", "%20" );
		return sReplaceSpace;
	}

	public static String sShowUnprintable( String sRawText ){
		if( sRawText == null ) return "[null]";
		int len = sRawText.length();
		StringBuffer sb = new StringBuffer( len * 2 );
		for( int xChar = 0; xChar < len; xChar++ ){
			char c = sRawText.charAt(xChar);
			if( c < 32 || c > 255 )
			    sb.append("\\u" + Utility.sFixedWidth(Integer.toHexString((int)c), 4, '0', ALIGNMENT_RIGHT));
		    else
				sb.append(c);
			if( c == '\n' ) sb.append('\n');
		}
		return sb.toString();
	}

	public static int getOccurrenceCount(String s, char c){
		if( s == null ) return 0;
		int len = s.length();
		if( len == 0 ) return 0;
		int iCount = 0;
		for(int pos = 0; pos < len; pos++){
			if( s.charAt(pos) == c ) iCount++;
		}
		return iCount;
	}

	public static int find( StringBuffer sb, String sTarget ){ // zero based
		return find( sb, sTarget, false, 0 );
	}

	public static int find( StringBuffer sb, String sTarget, boolean zIgnoreCase ){ // zero based
		return find( sb, sTarget, zIgnoreCase, 0 );
	}

	public static int find( StringBuffer sb, String sTarget, int posStart ){ // zero based
		return find( sb, sTarget, false, posStart );
	}

	public static int find( StringBuffer sb, String sTarget, boolean zIgnoreCase, int posStart ){ // zero based
		if( sb == null || sTarget == null ) return -2; // invalid input
		if( posStart < 0 ) posStart = 0;
		int posBuffer = posStart;
		int posTarget = 0;
		int lenBuffer = sb.length();
		int lenTarget = sTarget.length();
		if( lenTarget == 0 ) return -3; // invalid target
		if( zIgnoreCase ) sTarget = sTarget.toUpperCase();
		while( true ){
			if( posBuffer >= lenBuffer ) return -1; // not found
			char cBuffer = zIgnoreCase ? Character.toUpperCase(sb.charAt(posBuffer)) : sb.charAt(posBuffer);
			if( cBuffer == sTarget.charAt(posTarget) ){
				if( posTarget + 1 == lenTarget ){ // found
					return posBuffer - lenTarget + 1;
				} else {
					posTarget++;
				}
			} else {
				if( posTarget > 0 ){
					posBuffer -= posTarget;
					posTarget = 0;
				}
			}
			posBuffer++;
		}
	}

	public static String[] split( String s, char c ){
		if( s == null ) return null;
		int ctChar = Utility.getOccurrenceCount(s, c);
		int ctSegments = ctChar + 1;
		String[] asSegments = new String[ctSegments];
		if( ctChar == 0 ){ // minimal case
			asSegments[0] = s;
			return asSegments;
		}
		int len = s.length();
		int xSegment = 0;
		int posStartOfSegment = 0;
		for(int pos = 0; pos < len; pos++){
			if( s.charAt(pos) == c ){
				asSegments[xSegment] = s.substring(posStartOfSegment, pos);
				posStartOfSegment = pos + 1; // skip delimiter
				xSegment++;
			}
		}
		asSegments[xSegment] = s.substring(posStartOfSegment, len);
		return asSegments;
	}

	/** splits a string into comma OR white space-separated values
	 *  in other words if two words are separated by 3 spaces than
	 *  only the two words are included; blanks (two commas in a row)
	 *  are treated as the empty string ""; a terminal comma will
	 *  result in an extra empty string word at end but terminal
	 *  white space is ignored; if the string is null, null is returned
	 *  if the string is the empty string an array containing only one
	 *  element, the empty string, is returned
	 */
	public static String[] splitCommaWhiteSpace( String s ){
		if( s == null ) return null;
		String[] as = null;
		if( s.length() == 0 ){
			as = new String[1];
			as[0] = "";
			return as;
		}
		int len = s.length();
		int ctElements = 0;
		boolean zFilling = false; // first size the content array then in second pass fill it
		int eMode = 1; // before word
		StringBuffer sbBuffer = new StringBuffer(80);
		while( true ){
			for( int pos = 0; pos < len; pos++ ){
				char c = s.charAt(pos);
				switch( eMode ){
					case 1: // before word
						if( c == ',' ){ // word is empty string
							ctElements++;
		    				if( zFilling ) as[ctElements-1] = "";
						} else if( Character.isWhitespace(c) ) {
							// ignore
						} else {
							ctElements++;
							if( zFilling ) sbBuffer.append( c );
							eMode = 2; // in word
						}
						break;
					case 2: // in word
						if( c == ',' ){ // found end of word and beginning of new word
		    				if( zFilling ){
								as[ctElements-1] = sbBuffer.toString();
								sbBuffer.setLength(0);
							}
							eMode = 4; // after comma
						} else if( Character.isWhitespace(c) ) {  // found end of word
		    				if( zFilling ){
								as[ctElements-1] = sbBuffer.toString();
								sbBuffer.setLength(0);
							}
							eMode = 3; // after word
						} else {
							if( zFilling ) sbBuffer.append( c ); // still in word
						}
						break;
					case 3: // after word
						if( c == ',' ){ // found end of word
							eMode = 4; // after comma
						} else if( Character.isWhitespace(c) ) {
							// still after word
						} else {
							ctElements++;
							if( zFilling ) sbBuffer.append( c );
							eMode = 2;
						}
						break;
					case 4: // after comma
						if( c == ',' ){ // word is empty string
							ctElements++;
		    				if( zFilling ) as[ctElements-1] = "";
						} else if( Character.isWhitespace(c) ) {
							// ignore
						} else {
							ctElements++;
							if( zFilling ) sbBuffer.append( c );
							eMode = 2; // in word
						}
						break;
				}
			}
			if( eMode == 4 ){
				ctElements++; // terminal comma
				if( zFilling ) as[ctElements-1] = "";
			}
			if( zFilling ){
				if( eMode == 2 ){
					as[ctElements-1] = sbBuffer.toString();
				}
				return as;
			} else {
				as = new String[ctElements];
				zFilling = true;
				ctElements = 0;
				eMode = 1;
			}
		}
	}

	public static String sReplaceString(String sTarget, String sReplace, String sWith){
		if( sTarget==null || sReplace==null ) return sTarget;
		if( sReplace.length()==0 ) return sTarget;
		StringBuffer sbTarget = new StringBuffer(sTarget);
		vReplace(sbTarget, sReplace, sWith);
		return sbTarget.toString();
	}

	public static void vReplace(StringBuffer sbTarget, String sReplace, String sWith){
		if( sbTarget == null ) return;
		int lenTarget = sbTarget.length();
		if( lenTarget == 0 ) return;
		if( sReplace == null ) return;
		int lenReplace = sReplace.length();
		if( sReplace.length() == 0 ) return;
		if( lenReplace > lenTarget ) return; // if the replacement string is larger than the buffer obviously it won't be in the buffer
		char charReplaceBegin = sReplace.charAt(0);
		int posTarget=0;
		while(true){
			if( posTarget > lenTarget-lenReplace ) return;
			if( sbTarget.charAt(posTarget) == charReplaceBegin ){
				if( sbTarget.substring(posTarget, posTarget+lenReplace).equals(sReplace) ) break;
			}
			posTarget++;
		}
		// replaces: if( sbTarget.indexOf(sReplace) < 0 ) return; 1.4.1 only
		if( sWith == null ) sWith="";
		StringBuffer sb = new StringBuffer(lenTarget*2);
		int pos = 0, posScan = 0;
		int posAppend = 0;
		char cStart = sReplace.charAt(0);
ScanForStartOfMatch:
		while(pos<lenTarget){
			char cCurrent = sbTarget.charAt(pos);
			if(cCurrent==cStart){
				posScan=1;
				while(true){
					if(posScan==lenReplace){ // match found
						sb.append(sWith); // append replacement string
						posAppend += lenReplace;
						pos += lenReplace;
						continue ScanForStartOfMatch;
					}
					if(pos+posScan==lenTarget) break; // end of string - no match
					if(sbTarget.charAt(pos+posScan)!=sReplace.charAt(posScan)) break; // not a match
					posScan++;
				}
			}
			sb.append(cCurrent);
			pos++;
		}
		sbTarget.setLength(0);
		sbTarget.append(sb);
		return;
	}

	/* returns the protocol in upper case */
	public static String url_getPROTOCOL( String sURL ){
		sURL = sURL.trim();
		int posSeparator = sURL.indexOf(':');
		if( posSeparator < 0 ) return null;
		return sURL.substring(0, posSeparator).trim().toUpperCase();
	}

	public static javax.swing.ImageIcon imageiconLoadResource( String sResourcePath ){
		java.awt.Image image = imageLoadResource(sResourcePath);
		if( image == null ) return null;
		return new javax.swing.ImageIcon(image);
	}

	public static java.awt.Image imageLoadResource( String sResourcePath ){
		try {
			java.net.URL url = ApplicationController.getInstance().getClass().getResource(sResourcePath);
			if( url == null ){
				ApplicationController.vShowError("resource not found: " + sResourcePath + " (this resource was missing from the class path or jar file)");
				return null;
			}
			Object oContent = url.getContent();
			if( oContent == null ){
				ApplicationController.vShowError("resource content not available: " + sResourcePath);
				return null;
			}
			java.awt.image.ImageProducer image_producer = (java.awt.image.ImageProducer)oContent;
			if( image_producer == null ){
				if( oContent instanceof java.awt.Image ){
					return (java.awt.Image)oContent;
				} else {
					ApplicationController.vShowError("failed to load image: " + sResourcePath + " unknown resource type: " + oContent.getClass().getName());
					return null;
				}
			} else {
				java.awt.Toolkit tk = java.awt.Toolkit.getDefaultToolkit();
				java.awt.Image image = tk.createImage(image_producer);
				return image;
			}
/* does not work for zip files for some reason:
			java.io.InputStream inputstreamImage = url.openStream();
			int iChunkSize = inputstreamImage.available();
			int iPreviousChunkSize = 0;
			byte[] abImage = new byte[0];
			byte[] abImage_buffer;
			int nChunk = 0;
			while( iChunkSize > 0 ){
				nChunk++;
				byte[] abImage_chunk = new byte[iChunkSize];
				inputstreamImage.read(abImage_chunk, 0, iChunkSize);
				abImage_buffer = new byte[abImage.length + iChunkSize];
				System.arraycopy(abImage, 0, abImage_buffer, 0, abImage.length);
				System.arraycopy(abImage_chunk, 0, abImage_buffer, abImage.length, abImage_chunk.length);
				abImage = abImage_buffer;
				iPreviousChunkSize = iChunkSize;
				iChunkSize = inputstreamImage.available();

// there appears to be a bug in the zip loader for windows that causes
// the available count not to go to 0 when it is done--it sets starts at
// the full size of the file and stays there
				if ( nChunk > 10 || (iChunkSize == iPreviousChunkSize) ) break; // emergency protection
			}
			java.awt.Toolkit tk = java.awt.Toolkit.getDefaultToolkit();
			java.awt.Image image = tk.createImage(abImage);
*/
		} catch( Exception ex ) {
			ApplicationController.vShowError("failed to load image: " + sResourcePath + " error: " + ex);
			return null;
		}
	}

	static void vAddFavorite( DodsURL url ){
		FavoritesFilter filter = new FavoritesFilter();
		File[] afile = getPreferencesFiles(filter);
		String sFilename;
		if( afile == null ){
			sFilename = "favorite-0000001.ser";
		} else {
			if( afile.length < 1 ) {
				sFilename = "favorite-0000001.ser";
			} else {
				int iMaxFavoriteNumber = 0;
				for( int xFile = 0; xFile < afile.length; xFile++ ){
					File fileFavorite = afile[xFile];
					String sNumber = fileFavorite.getName().substring(9, 16); // make more robust
					try {
						int iCurrentNumber = Integer.parseInt(sNumber);
						if( iCurrentNumber > iMaxFavoriteNumber ) iMaxFavoriteNumber = iCurrentNumber;
					} catch(Exception ex) {
						// do nothing
					}
				}
				String sNumberNew = sFixedWidth(Integer.toString(iMaxFavoriteNumber+1), 7, '0', Utility.ALIGNMENT_RIGHT);
				sFilename = "favorite-" + sNumberNew + ".ser";
			}
		}
		StringBuffer sbError = new StringBuffer();
		if( !Utility.zStorePreferenceObject(url, sFilename, sbError) ){
			ApplicationController.vShowError("Failed to store favorite: " + sbError);
		}
	}

	static void vAddRecent( DodsURL url ){
		StringBuffer sbError = new StringBuffer();
		int iRecentCount = ConfigurationManager.getInstance().getProperty_RecentCount();
		if( iRecentCount < 1 ) return; // no recents at all
		RecentFilter filter = new RecentFilter();
		File[] afile = getPreferencesFiles(filter);
		if( afile == null ){
			ApplicationController.getInstance().vShowError("error adding recent: directory list unexpectedly null");
			return;
		}

		// see if the URL is already present
		Panel_Select_Recent panelRecent = ApplicationController.getInstance().getAppFrame().getPanel_Recent();
		if( panelRecent == null ){
			ApplicationController.getInstance().vShowWarning("error adding recent: recent panel unavailable");
			return;
		}
		int xFileToDelete = panelRecent.xIndexOfMatchingBaseURL(url);
		if( xFileToDelete != - 1 ){ // already in there (replace existing object)
			String sFilename = afile[xFileToDelete].getName();
			if( !Utility.zDeletePreferenceObject(sFilename, sbError) ){
				ApplicationController.getInstance().vShowWarning("failed to delete matching recent: " + sbError);
				return;
			}
		}

		// delete any additional recents necessary to bring list down to recent count
		if( afile.length > iRecentCount-1 ){
			java.util.Arrays.sort(afile);
			int ctToDelete = afile.length - iRecentCount + 1;
			for( xFileToDelete = 0; xFileToDelete < ctToDelete; xFileToDelete++ ){
				String sFilename = afile[xFileToDelete].getName();
				if( !Utility.zDeletePreferenceObject(sFilename, sbError) ){
					ApplicationController.getInstance().vShowWarning("failed to delete extra recent: " + sbError);
				}
			}
		}

		String sFilename;
		if( afile == null ){
			sFilename = "recent-0000001.ser";
		} else {
			if( afile.length < 1 ) {
				sFilename = "recent-0000001.ser";
			} else {
				int iMaxFavoriteNumber = 0;
				for( int xFile = 0; xFile < afile.length; xFile++ ){
					File fileRecent = afile[xFile];
					String sNumber = fileRecent.getName().substring(7, 14); // make more robust
					try {
						int iCurrentNumber = Integer.parseInt(sNumber);
						if( iCurrentNumber > iMaxFavoriteNumber ) iMaxFavoriteNumber = iCurrentNumber;
					} catch(Exception ex) {
						// do nothing
					}
				}
				String sNumberNew = sFixedWidth(Integer.toString(iMaxFavoriteNumber+1), 7, '0', Utility.ALIGNMENT_RIGHT);
				sFilename = "recent-" + sNumberNew + ".ser";
			}
		}
		if( !Utility.zStorePreferenceObject(url, sFilename, sbError) ){
			ApplicationController.vShowError("Failed to store recent: " + sbError);
		}
	}

	static boolean zDeleteFavorite( int iID, StringBuffer sbError ){
		try {
			String sNumber = sFixedWidth(Integer.toString(iID), 7, '0', Utility.ALIGNMENT_RIGHT);
			String sFilename = "favorite-" + sNumber + ".ser";
			return Utility.zDeletePreferenceObject(sFilename, sbError);
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	public final static int SORT_none = 0;
	public final static int SORT_ascending = 1;
	public final static int SORT_descending = 2;

	static DodsURL[] getPreferenceURLs_Favorites(){
		FilenameFilter filter = new FavoritesFilter();
		return getPreferenceURLs(filter, SORT_descending);
	}

	static DodsURL[] getPreferenceURLs_Recent(){
		FilenameFilter filter = new RecentFilter();
		return getPreferenceURLs(filter, SORT_descending);
	}

	static DodsURL[] getPreferenceURLs(FilenameFilter filter, int eSORT){
		String sFileName = null, sPath = null;
		try {
			File[] afile = getPreferencesFiles(filter);
			if( afile == null ) return null;
			sortFiles( afile, eSORT );
			DodsURL[] aURLs = new DodsURL[afile.length];
			StringBuffer sbError = new StringBuffer(80);
			int xDodsURL = 0;
			for( int xURL = 0; xURL < afile.length; xURL++ ){
				sPath = afile[xURL].getAbsolutePath();
				sFileName = afile[xURL].getName();
				String sNumber = sFileName.substring(9, 16); // make more robust
				int iNumber;
				try {
					iNumber = Integer.parseInt(sNumber);
				} catch(Exception ex) {
					iNumber = 0;
				}
				Object oURL = Utility.oLoadObject(sPath, sbError);
				if( oURL == null ){
					ApplicationController.vShowError("Failed to load preference " + iNumber + " [" + sPath + "]: " + sbError);
				} else {
					xDodsURL++;
					aURLs[xDodsURL-1] = (DodsURL)oURL;
					aURLs[xDodsURL-1].setID(iNumber);
				}
			}
			if( xDodsURL > 0 ){
				return aURLs;
			} else {
				return null;
			}
		} catch(Exception ex) {
			StringBuffer sbError = new StringBuffer("Unexpected error loading preferences (filename: " + sFileName + " path: " + sPath + "): ");
			vUnexpectedError(ex, sbError);
			ApplicationController.vShowError(sbError.toString());
			return null;
		}
	}

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

	static File[] getPreferencesFiles(FilenameFilter filter){
		String sPreferencesDirectory = ConfigurationManager.getInstance().getProperty_PreferencesDirectory();
		File filePreferencesDirectory = new File(sPreferencesDirectory);
		if( filePreferencesDirectory == null ){
			filePreferencesDirectory.mkdirs();
		}
		if( !filePreferencesDirectory.exists() ){
			filePreferencesDirectory.mkdirs();
		}
        if (filePreferencesDirectory.isDirectory()) {
			return filePreferencesDirectory.listFiles(filter);
		} else {
			return null;
		}
	}

	File[] getPreferences_Recent(){
		return null;
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

	// loads lines in a file into an array list of lines
	// content can include newlines if in quotation marks
	// End of Line:
	//   PC - carriage return plus line feed
	//   Macintosh - carriage return
	//   UNIX - line feed (usually called "new line" in UNIX parlance)
	// ASCII code in decimal: LF = 10; CR = 13
	public static ArrayList zLoadLines( String sAbsolutePath, int iEstimatedSize, StringBuffer sbError){
		if( sAbsolutePath == null ){ sbError.append("path missing"); return null; }
		StringBuffer sbContent = new StringBuffer(iEstimatedSize);
		if( !zLoadStringFile( sAbsolutePath, sbContent, sbError ) ){
			sbError.insert(0, "error loading file (" + sAbsolutePath + "): ");
			return null;
		}
		int lenContent = sbContent.length();
		ArrayList listLines = new ArrayList();
		StringBuffer sbLine = new StringBuffer(120);
		int pos = 0;
		int eState = 1;
		while( pos < lenContent ){
			char c = sbContent.charAt(pos);
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

	static boolean zLoadStringFile( String sAbsolutePath, StringBuffer sbResource, StringBuffer sbError){
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

	static boolean zSaveStringFile( String sAbsolutePath, StringBuffer sbBuffer, StringBuffer sbError){
		File file = new File(sAbsolutePath);
		OutputStream os = null;
		try {
			os = new FileOutputStream(file);
		} catch(Exception ex) {
			sbError.append("Failed to open file for writing: " + ex);
			return false;
		}
		try {
			os.write(sbBuffer.toString().getBytes());
		} catch(Exception ex) {
			sbError.append("Failed to write " + sbBuffer.length() + " byte buffer: " + ex);
			return false;
		} finally {
			try {
				if(os != null) os.close();
			} catch(Exception ex) {
				sbError.append("Failed to close output stream: " + ex);
				return false;
			}
		}
		return true;
	}

	static boolean zDeletePreferenceObject( String sFileName, StringBuffer sbError ){
		try {
			String sPreferencesDirectory = ConfigurationManager.getInstance().getProperty_PreferencesDirectory();
			String sPath = sPreferencesDirectory + sFileName;
			File fileToDelete = new File(sPath);
			if( !fileToDelete.exists() ){
				sbError.append("file does not exist: " + sPath);
				return false;
			}
			fileToDelete.delete();
			return true;
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	static int parseInteger_positive(String s){
		try {
			int i = Integer.parseInt(s);
			if( i < 1 ) return -1;
			return i;
		} catch(Exception ex) {
			return -1;
		}
	}

	static int parseInteger_nonnegative(String s){
		try {
			int i = Integer.parseInt(s);
			if( i < 0 ) return -1;
			return i;
		} catch(Exception ex) {
			return -1;
		}
	}

	static boolean isNumeric(String s){
		try {
			long n = Long.parseLong(s);
			return true; // its a long or an int of some kind
		} catch(Exception ex) {}
		try {
			double d = Double.parseDouble(s); // its a double or float
			return true;
		} catch(Exception ex) {}
		return false; // not a number
	}

	static boolean isInteger(String s){
		try {
			Integer.parseInt(s);
			return true;
		} catch(Exception ex) {
			return false;
		}
	}

	static boolean isDouble(String s){
		try {
			Double.parseDouble(s);
			return true;
		} catch(Exception ex) {
			return false;
		}
	}

	public static String sToHex( int i, int iWidth ){
		String s;
		char cPadding;
		try {
			s = Integer.toHexString(i).toUpperCase();
			cPadding = '0';
		} catch(Exception ex) {
			s = "?";
			cPadding = '?';
		}
		return sFixedWidth(s, iWidth, cPadding, ALIGNMENT_RIGHT);
	}

	public static String sToHex( long n, int iWidth ){
		String s;
		char cPadding;
		try {
			s = Long.toHexString(n).toUpperCase();
			cPadding = '0';
		} catch(Exception ex) {
			s = "?";
			cPadding = '?';
		}
		return sFixedWidth(s, iWidth, cPadding, ALIGNMENT_RIGHT);
	}

	public static final int ALIGNMENT_LEFT = 1;
	public static final int ALIGNMENT_RIGHT = 2;

	public static String sFixedWidth(String sText, int iWidth, char cPadding){
		return sFixedWidth(sText, iWidth, cPadding, Utility.ALIGNMENT_LEFT);
	}

	public static String sFixedWidth(String s, int iWidth, char cPadding, int constAlignment){
		StringBuffer sb;
		if(s.length()<iWidth){
			sb = new StringBuffer(iWidth);
			if(constAlignment==Utility.ALIGNMENT_LEFT) sb.append(s);
			for(int xChar=1; xChar<=iWidth-s.length(); xChar++) sb.append(cPadding);
			if(constAlignment==Utility.ALIGNMENT_RIGHT)
				return sb.append(s).toString();
			else
				return sb.toString();
		} else {
			if(s.length()>iWidth)
			    return s.substring(0, iWidth);
		    else
			    return s;
		}
	}

	/** Treats null as "". Returns "" on width < 1. The text is left-justified (use sRight function to right-justify). */
	public static String sFormatFixedRight(long n, int iWidth, char cPadding){
		String sNumber = Long.toString(n);
		return sFormatFixedRight( sNumber, iWidth, cPadding );
	}

	/** Treats null as "". Returns "" on width < 1. The text is left-justified (use sRight function to right-justify). */
	public static String sFormatFixedRight(int i, int iWidth, char cPadding){
		String sNumber = Integer.toString(i);
		return sFormatFixedRight( sNumber, iWidth, cPadding );
	}

	/** Extracts the stack trace as a string from an exception */
	public static String extractStackTrace(Throwable theException) {
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
	public static String extractErrorLine(Throwable theException) {
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

	static String getFilenameFromURL( String sFullURL ){
	    if( sFullURL == null || sFullURL.length() == 0 ) return null;
		String sReverseURL = Utility.sReverse(sFullURL);
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

	public static String sReverse( String s ){
		if( s == null ) return s;
		if( s.length() < 2 ) return s;
		int posLeft = -1;
		int len = s.length();
		StringBuffer sb = new StringBuffer(s);
		while(true){
			posLeft++;
			int posRight = len - posLeft - 1;
			if( posLeft >= posRight ) break;
			sb.setCharAt(posLeft, s.charAt(posRight));
			sb.setCharAt(posRight, s.charAt(posLeft));
		}
		return sb.toString();
	}

	public static String sRepeatChar( char c, int ctRepetition ){
		if( ctRepetition == 0 ) return "";
		StringBuffer sb = new StringBuffer( ctRepetition );
		for(int xRep = 1; xRep <= ctRepetition; xRep++ ){
			sb.append(c);
		}
		return sb.toString();
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
						ApplicationController.getInstance().vShowStatus_NoCache("Received " + iPacketCount + sTotalBytes);
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
		String sReverseFileName = Utility.sReverse(sFileName);
		int posDot = sReverseFileName.indexOf('.');
		if( posDot == -1 ) return null;
		String sPossibleExtension = sFileName.substring(sFileName.length() - posDot);
		if( sPossibleExtension.indexOf(msFileSeparator) == -1 ){
			return sPossibleExtension;
		} else {
			return null; // if a slash is in there it means the dot was upstream from the file name
		}
	}

	public static String sDetemineDodsRawPath( String sDodsURL, StringBuffer sbError ){
		try {
			if( sDodsURL == null ){
				sbError.append("input URL was null");
				return null;
			}

			// find method
			int posMethodBegin = sDodsURL.indexOf("://");
			if( posMethodBegin < 0 ){
				sbError.append("no method found in URL");
				return null;
			}
			int posDomainBegin = posMethodBegin + 3;

			// find end of domain
			int posDomainEnd = sDodsURL.indexOf("/", posDomainBegin);
			if( posDomainEnd < 0 ){
				sbError.append("no domain-terminating slash found after method");
				return null;
			}

			// find nph
			int posNPHbegin = sDodsURL.toUpperCase().indexOf("NPH-");
			if( posNPHbegin < 0 ){
				sbError.append("hint 'nph-' not found in URL");
				return null;
			}

			// find end of nph
			int posNPHend = sDodsURL.indexOf("/", posNPHbegin);
			if( posNPHend < 0 ){
				sbError.append("no slash found after 'nph-' hint");
				return null;
			}

			// construct raw URL
			StringBuffer sbRawURL = new StringBuffer(sDodsURL.length());
			sbRawURL.append(sDodsURL.substring(0, posDomainEnd));
			sbRawURL.append(sDodsURL.substring(posNPHend));
			return sbRawURL.toString();

		} catch( Exception ex ) {
			vUnexpectedError(ex, sbError);
			return null;
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
		try {
			if( sName == null ) return "unknown";
			if( sName.length() == 0 ) return "unknown";
			if( sName.length() > 32 ) sName = sName.substring(0, 32);
			StringBuffer sb = new StringBuffer(sName.length());
			for( int xChar = 0; xChar < sName.length(); xChar++ ){
				char c = sName.charAt(xChar);
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

	/** Gets a substring from a buffer of a particular length. There is no danger of causing an error by giving bad arguments.
	 *  For example, if the snip length plus the location exceeds the length of the buffer it will return the contents of the buffer
	 *  up to the end safely. This function is useful when doing error reporting on a particular area of a large buffer when you
	 *  want to excerpt the offending area of the buffer.
	 *  (c) John Chamberlain
	 * */
	public static String sSafeSubstring(StringBuffer sb, int posOneBasedSnipLocation, int lenSnip){
		if(sb==null) return "[no buffer]";
		if(sb.length()==0) return "[buffer is empty]";
		if(lenSnip<0) return "";
		if(posOneBasedSnipLocation<1) posOneBasedSnipLocation = 1;
		if(posOneBasedSnipLocation>sb.length()) posOneBasedSnipLocation = sb.length();
		if(posOneBasedSnipLocation + lenSnip - 1 > sb.length()){
			return sb.substring(posOneBasedSnipLocation-1);
		} else {
			return sb.substring(posOneBasedSnipLocation-1, posOneBasedSnipLocation + lenSnip - 1);
		}
	}

	public static String sSafeSubstring(String sBuffer, int posOneBasedSnipLocation, int lenSnip){
		if(sBuffer==null) return "[no buffer]";
		if(sBuffer.length()==0) return "[buffer is empty]";
		if(lenSnip<0) return "";
		if(posOneBasedSnipLocation<1) posOneBasedSnipLocation = 1;
		if(posOneBasedSnipLocation>sBuffer.length()) posOneBasedSnipLocation = sBuffer.length();
		if(posOneBasedSnipLocation + lenSnip - 1 > sBuffer.length()){
			return sBuffer.substring(posOneBasedSnipLocation-1);
		} else {
			return sBuffer.substring(posOneBasedSnipLocation-1, posOneBasedSnipLocation + lenSnip - 1);
		}
	}

	public static String sSafeSubstring(byte[] ab, int posOneBasedSnipLocation, int lenSnip){
		if(ab==null) return "[no buffer]";
		if(ab.length==0) return "[buffer is empty]";
		if(posOneBasedSnipLocation<1) posOneBasedSnipLocation = 1;
		if(lenSnip<0) return "";
		if(posOneBasedSnipLocation>ab.length) posOneBasedSnipLocation = ab.length;
		if(posOneBasedSnipLocation + lenSnip - 1 > ab.length){
			return new String(ab, posOneBasedSnipLocation-1, ab.length - posOneBasedSnipLocation + 1);
		} else {
			return new String(ab, posOneBasedSnipLocation-1, lenSnip);
		}
	}

	/** Removes the HTML tages from an HTML file. Replaces <P>, <BR>, </TR> with newlines. Replaces <TD> with tab */
	public static String sStripTags( String sHTML ){
		StringBuffer sb = new StringBuffer(sHTML);
		vReplace(sb, "<P>", "");
		vReplace(sb, "<p>", "");
		vReplace(sb, "<BR>", "");
		vReplace(sb, "<br>", "");
		vReplace(sb, "<HR>", "---------------------------------------------------------");
		vReplace(sb, "<hr>", "---------------------------------------------------------");
		vReplace(sb, "<TABLE>", "");
		vReplace(sb, "<table>", "");
		vReplace(sb, "<TR>", "");
		vReplace(sb, "<tr>", "");
		vReplace(sb, "<TD>", "\t");
		vReplace(sb, "<td>", "\t");
		int pos = 0;
		int posTag_begin = 0;
		int posTag_end = 0;
		int iTagDepth = 0;
		while(pos < sb.length()){
			if(sb.charAt(pos) == '<'){
				if( iTagDepth == 0 ) posTag_begin = pos;
				iTagDepth++;
				pos++;
				continue;
			} else {
				if(sb.charAt(pos) == '>'){
					iTagDepth--;
					if( iTagDepth < 0 ){ // unmatched tag
						iTagDepth = 0;
						pos++;
						continue;
					}
					if( iTagDepth == 0){
						sb.delete(posTag_begin, pos+1);
						pos = posTag_begin;
						continue;
					}
				}
				pos++;
			}
		}
		return sb.toString();
	}

	/** Returns "" on null or width < 1. */
	public static String sRight(String sText, int iWidth){
		if(sText==null) return "";
		if(sText.length()<=iWidth) return sText;
		return sText.substring(sText.length()-iWidth);
	}

	// number of decimal places, 0 means integer pecision
	public static String sDoubleToPrecisionString( double d, int iPrecision ){
		if( Double.isNaN(d) ) return "NaN";
		if( Double.isInfinite(d) ) return "\u221E";
		return Float.toString( (float)d ); // temporary solution
		// int iOrder = (int)(Math.log(d) / Math.log(10)) + 1;
		// return "";
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

	/** Treats null as "". Returns "" on width < 1. The text is left-justified (use sRight function to right-justify). */
	static String sFormatFixedRight(String sText, int iWidth, char cPadding){
		StringBuffer sb;
		if(sText == null) sText="";
		if(iWidth < 1) return "";
		if(sText.length()<iWidth){
			sb = new StringBuffer(iWidth);
			for(int xChar=1; xChar<=iWidth-sText.length(); xChar++) sb.append(cPadding);
			return sb.append(sText).toString();
		} else if(sText.length()>iWidth) {
			return sText.substring(0, iWidth);
		} else { return sText; }
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

	public static String dumpArray( int[] ai, int from, int to ){
		if( ai == null ) return "[null]";
		if( to == 0 ) to = ai.length - 1;
		StringBuffer sb = new StringBuffer(80);
		for( int x = from; x <= to; x++ ){
			sb.append("[" + x + "] = " + ai[x] + "\n");
		}
		return sb.toString();
	}
	public static String dumpArray( String[] as, int from, int to ){
		if( as == null ) return "[null]";
		if( to == 0 ) to = as.length - 1;
		StringBuffer sb = new StringBuffer(80);
		for( int x = from; x <= to; x++ ){
			sb.append("[" + x + "] = " + as[x] + "\n");
		}
		return sb.toString();
	}
	public static boolean equalsUPPER( String s1, String s2 ){
		if( s1 == null || s2 == null ) return false;
		return s1.equalsIgnoreCase(s2);
	}
	public static String dumpArray( double[] ad, int from, int to ){
		if( ad == null ) return "null";
		if( to == 0 ) to = ad.length - 1;
		StringBuffer sb = new StringBuffer(80);
		for( int x = from; x <= to; x++ ){
			sb.append("[" + x + "] = " + ad[x] + "\n");
		}
		return sb.toString();
	}
	public static String dumpArray( float[] af, int from, int to ){
		if( af == null ) return "null";
		if( to == 0 ) to = af.length - 1;
		StringBuffer sb = new StringBuffer(80);
		for( int x = from; x <= to; x++ ){
			sb.append("[" + x + "] = " + af[x] + "\n");
		}
		return sb.toString();
	}
	public static String sDumpMatrix( double[] ai, int x_len, int y_len, int x_to, int y_to ){
		int xValue = -1;
		StringBuffer sb = new StringBuffer(250);
		for( int xY = 1; xY <= y_len; xY++ ){
			if( xY > y_to ) break;
			for( int xX = 1; xX <= x_len; xX++ ){
				xValue++;
				if( xX > x_to ) continue;
				sb.append(' ');
				if( ai[xValue] < 10 ) sb.append(' ');
				sb.append(ai[xValue]);
			}
		    sb.append("\n");
		}
		return sb.toString();
	}

	static String sDumpMatrix( int[] ai ){
		StringBuffer sb = new StringBuffer(250);
		int lenArray = ai.length;
		sb.append("array[" + lenArray + "]: ");
		for( int x = 0; x < lenArray; x++ ){
			sb.append(' ');
			sb.append(ai[x]);
		}
		return sb.toString();
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

	public static String sWrap(String sText, int iColumns, boolean zHardMargin, String sEOL ) {
		if( iColumns < 1 ) return sText;
		if( sText.length() < 2 ) return sText;
		int lenText = sText.length();
		StringBuffer sb = new StringBuffer( (int) (lenText * 1.10));
		int iLineSize = 0;
		if (zHardMargin) {
			for (int pos = 0; pos < lenText; pos++) {
				sb.append(sText.charAt(pos));
				iLineSize++;
				if( iLineSize == iColumns ){
					sb.append(sEOL);
					iLineSize = 0;
				}
			}
		} else {
			char cCurrent = sText.charAt(0);
			char cNext = ' ';
			for (int pos = 1; pos < lenText; pos++) {
				cNext = sText.charAt(pos);
				sb.append(cCurrent);
				iLineSize++;
				if( sText.regionMatches(false, pos, sEOL, 0, sEOL.length()) ){
					iLineSize = 0;
					pos += sEOL.length() - 1;
				} else {
					if (iLineSize >= iColumns && Character.isWhitespace(cCurrent) && !Character.isWhitespace(cNext)) {
						sb.append(sEOL);
						iLineSize = 0;
					}
				}
				cCurrent = cNext;
			}
			sb.append(cNext);
		}
		return sb.toString();
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

	public static File fileEstablishDirectory( String sPath ){
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
			StringBuffer sbError = new StringBuffer();
			Utility.vUnexpectedError(ex, sbError);
			ApplicationController.vShowWarning("error establishing directory: " + sbError);
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



