/**
 * Title:        Utility_String
 * Description:  Contains shared string utility routines for all the classes in this package
 * Copyright:    Copyright (c) 2002-2009
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.06
 */

package opendap.clients.odc;

public class Utility_String {

	public static final int ALIGNMENT_LEFT = 1;
	public static final int ALIGNMENT_RIGHT = 2;
	
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
			    sb.append("\\u" + sFixedWidth(Integer.toHexString((int)c), 4, '0', ALIGNMENT_RIGHT));
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

	public static int find( String s, String[] as, boolean zCaseSensitive ){
		if( as == null ) return -1;
		for( int x = 0; x < as.length; x++ ){
			if( as[x] == null ) continue;
			if( zCaseSensitive ? as[x].equals( s ) : as[x].equalsIgnoreCase( s ) ) return x;
		}
		return -1;
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
		int ctChar = getOccurrenceCount(s, c);
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
		return sFixedWidth(Integer.toString(intSz), 3, '0', ALIGNMENT_RIGHT) + cOrder;
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
			Long.parseLong(s);
			return true; // its a long or an int of some kind
		} catch(Exception ex) {}
		try {
			Double.parseDouble(s); // its a double or float
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

	public static String sFixedWidth(String sText, int iWidth, char cPadding){
		return sFixedWidth(sText, iWidth, cPadding, ALIGNMENT_LEFT);
	}

	public static String sFixedWidth(String s, int iWidth, char cPadding, int constAlignment){
		StringBuffer sb;
		if(s.length()<iWidth){
			sb = new StringBuffer(iWidth);
			if( constAlignment== ALIGNMENT_LEFT ) sb.append(s);
			for( int xChar=1; xChar<=iWidth-s.length(); xChar++) sb.append(cPadding);
			if( constAlignment== ALIGNMENT_RIGHT )
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
	
	public static String sFormatFixedDecimal( float f, int iWidth, int iDecimalPosition, char cPadding ){
		return sFormatFixedDecimal( (double)f, iWidth, iDecimalPosition, cPadding );
	}

	/** iDecimalPosition must be > 1 and < iWidth and iWidth must be > 2 */
	public static String sFormatFixedDecimal( double d, int iWidth, int iDecimalPosition, char cPadding ){
		if( iDecimalPosition < 2 || iDecimalPosition > iWidth - 1 || iWidth < 3 ) return sRepeatChar( '*', iWidth );
		StringBuffer sb = new StringBuffer( iWidth );
		int iIntegerPlaces = iDecimalPosition - 1;
		int iDecimalPlaces = iWidth - iDecimalPosition;
		if( d >= Math.pow( 10, iIntegerPlaces ) ){
			return sRepeatChar( '+', iWidth );
		}
		if( d <= -1 * Math.pow( 10, iIntegerPlaces ) ){
			return sRepeatChar( '-', iWidth );
		}
		double dRoundedValue = Utility.round( d, iDecimalPlaces * -1 );
		String s = Double.toString( dRoundedValue );
		return s;
		// TODO NOT WORKING
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

	/** number of decimal places, 0 means integer pecision */
	public static String sDoubleToPrecisionString( double d, int iPrecision ){
		if( Double.isNaN(d) ) return "NaN";
		if( Double.isInfinite(d) ) return "\u221E";
		return Float.toString( (float)d ); // temporary solution
		// int iOrder = (int)(Math.log(d) / Math.log(10)) + 1;
		// return "";
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

	final public static String sLineBreak( String sInput, int ctChars, String sBreakChars ){
		int lenInput = sInput.length();
		StringBuffer sb = new StringBuffer( lenInput + lenInput/ctChars + 1);
		int xChar = 0;
		int ctCharsSinceLastBreak = 0;
		while( true ){
			if( xChar == lenInput ) break;
			ctCharsSinceLastBreak++;
			char cCurrent = sInput.charAt(xChar);
			sb.append( cCurrent );
			if( sInput.substring( xChar ).startsWith( sBreakChars ) ) ctCharsSinceLastBreak = 0;
			if( ctCharsSinceLastBreak == ctChars ){
				sb.append( sBreakChars );
				ctCharsSinceLastBreak = 0;
			}
			xChar++;
		}
		return sb.toString();
	}

	final public static String getRValue( String sExpression ){
			int posEquals = sExpression.indexOf( '=' );
			if( posEquals == -1 ) return null;  // no equals sign
			return sExpression.substring( posEquals + 1 ).trim();
	}

	final public static String getLValue( String sExpression ){
			int posEquals = sExpression.indexOf( '=' );
			if( posEquals == -1 ) return null;  // no equals sign
			return sExpression.substring( 0, posEquals ).trim();
	}
	
}
