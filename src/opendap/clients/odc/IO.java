package opendap.clients.odc;

import java.io.*;

import java.net.URL;
import java.net.InetSocketAddress;
import java.nio.*;
import java.nio.channels.*;
import java.util.ArrayList;

public class IO {

	public final static int TIMEOUT_CHECK_INTERVAL_ms = 100;
	public final static int TIMEOUT_DEFAULT_CONNECT   = 20;
	public final static int TIMEOUT_DEFAULT_READ      = 10;
	public final static int MAX_HEADER_BYTES          = 10000; // headers larger than this unacceptable
	public final static int MAX_ERROR_BYTES           = 10000; // the largest acceptable error
	public final static int BUFFER_SIZE               = 1024 * 10;

    public IO(){}

	public static String getStaticContent(String sURL, ByteCounter bc, Activity activity, StringBuffer sbError) {
		URL url;
		try {
			 url = new URL(sURL);
		} catch(Exception ex) {
			sbError.append("Failed to interpret " + sURL + " as an URL: " + ex);
			return null;
		}
		return getStaticContent( url, bc, activity, sbError );
	}

    /**
     * Open a connection to the DODS server.
     * @param url the URL to open.
     * @param sbError Buffer to write any error message to.
     * @return the opened <code>InputStream</code> or null in the case of an error.
     */
	public static String getStaticContent(URL url, ByteCounter bc, Activity activity, StringBuffer sbError) {

		if( url == null ){
			sbError.append("URL missing");
			return null;
		}
		String sHost  = url.getHost();
		int    iPort  = url.getPort();
		String sPath  = url.getPath();
		String sQuery = url.getQuery();
		if( iPort == -1 ) iPort = 80;
		return getStaticContent( sHost, iPort, sPath, sQuery, bc, activity, sbError );
	}

	public static String getStaticContent(String sHost, int iPort, String sPath, String sQuery, ByteCounter bc, Activity activity, StringBuffer sbError) {
		String sCommand = "GET";
		String sProtocol = "HTTP/1.1";
		String sReferer = null;
		String sContentType = null;
		String sContent = null;
		ArrayList listClientCookies = null;
		ArrayList listServerCookies = null;
		return getStaticContent(sCommand, sHost, iPort, sPath, sQuery, sProtocol, sReferer, sContentType, sContent, listClientCookies, listServerCookies, bc, activity, sbError);
	}

    /**
     * Open a connection to a server.
     * @param sHost (like gso.uri.edu or 127.0.0.1)
     * @param iPort (80 is the normal port for http)
     * @param sPath (like /index.html or /ftp/pub/)
     * @param sQuery (like q=%2Bjava+%2Bnio, no question mark)
	 * @param sProtocol (like HTTP/1.1)
     * @param sReferer
     * @param sContentType supply content MIME type if desired
     * @param sContent supply additional content if desired (for example if this is a post) [content length will automatically be calculated]
	 * @param listClientCookies supply an ArrayList of cookies you want to send to server
	 * @param listServerCookies supply an ArrayList if you want the cookie strings back from server
     * @param bc Optional ByteCounter object to get progress feedback, supply null if unwanted
     * @param sbError Buffer to write any error message to.
     * @return the content of the return as a String or null in the case of an error.
     */
	public static String getStaticContent(String sCommand, String sHost, int iPort, String sPath, String sQuery, String sProtocol, String sReferer, String sContentType, String sContent, ArrayList listClientCookies, ArrayList listServerCookies, ByteCounter bc, Activity activity, StringBuffer sbError) {

		if( sHost == null ){
			sbError.append("host missing");
			return null;
		}

		boolean zLogHeaders = ConfigurationManager.getInstance().getProperty_LOGGING_ShowHeaders();

		int iTimeoutConnect_seconds = ConfigurationManager.getInstance().getProperty_Timeout_InternetConnect();
		int iTimeoutRead_seconds = ConfigurationManager.getInstance().getProperty_Timeout_InternetRead();
		String sUserAgent = ApplicationController.getInstance().getVersionString();

		SocketChannel socket_channel = IO.oAcquireSocketChannel(sHost, iPort, iTimeoutConnect_seconds, activity, sbError);

		// render request
		String sRequestLocator = sPath + (sQuery == null ? "" : '?' + sQuery);
		String sRequest = sCommand + " " + sRequestLocator + " " + sProtocol + "\r\n";
		sRequest += "Host: " + sHost + ":" + iPort + "\r\n";
		sRequest += "User-Agent: " + sUserAgent + "\r\n";
		sRequest += "Accept: text/*\r\n";
		if( sReferer != null ){
			sRequest += "Referer: " + sReferer + "\r\n";
		}
		if( listClientCookies != null ){
			StringBuffer sbCookieText = new StringBuffer(1000);
			for( int xCookie = 1; xCookie <= listClientCookies.size(); xCookie++ ){
				if( xCookie > 1 ) sbCookieText.append("; ");
				sbCookieText.append((String)listClientCookies.get(xCookie - 1));
			}
			if( sbCookieText.length() > 0 ) sRequest += "Cookie: " + sbCookieText  + "\r\n";
		}
		if( sContent != null ){
			if( sContentType != null ) sRequest += "Content-Type: " + sContentType  + "\r\n";
			sRequest += "Content-Length: " + sContent.length()  + "\r\n";
			sRequest += "\r\n"; // end of header
			sRequest += sContent;
		} else {
			sRequest += "\r\n"; // end of header
		}

		if( zLogHeaders ) ApplicationController.getInstance().vShowStatus("Request header:\n" + Utility.sShowUnprintable(sRequest));

		try {
			byte[] ab = sRequest.getBytes();
			if( activity != null ) activity.vUpdateStatus("sending request " + sRequestLocator);
			socket_channel.write(ByteBuffer.wrap(ab));
		} catch( Throwable t ) {
			try { socket_channel.close(); } catch( Throwable t_is ){}
			return null;
		}

		if( activity != null ) activity.vUpdateStatus("connected, waiting for request response");
		String sHeader = sScanHeaderHTTP( socket_channel, iTimeoutRead_seconds, activity, sbError );
		if( sHeader == null ){
			sbError.insert(0, "scanning header: ");
			return null;
		} else if( sHeader.length() == 0) {
			sbError.append("server returned nothing");
			return null;
		}
		boolean zHeaderExists = sHeader.toUpperCase().startsWith("HTTP/");

		int iContentLength = 0;
		boolean zHasContentLength = false;
		boolean zChunked = false;
		if( zHeaderExists ){

			if( zLogHeaders ) ApplicationController.getInstance().vShowStatus("Response header http://" + sHost + ":" + iPort + sRequestLocator + ":\n" + Utility.sShowUnprintable(sHeader));

			// validate HTTP protocol, load and check headers
			String sHTTP_Response = getHeaderField( sHeader, 1 );
			if( sHTTP_Response == null ){
				sbError.append("server response was empty");
				try { socket_channel.close(); } catch( Throwable t_is ){}
				return null;
			}
			if( !sHTTP_Response.startsWith("HTTP/") ){
				sbError.append("server response was not HTTP: " + Utility.sSafeSubstring(sHTTP_Response, 0, 120));
				try { socket_channel.close(); } catch( Throwable t_is ){}
				return null;
			}
			String[] asHTTPResponse = Utility.split(sHTTP_Response, ' ');
			if( asHTTPResponse.length == 1 ){
				sbError.append("server response had no error/success code: " + Utility.sSafeSubstring(sHTTP_Response, 0, 120));
				try { socket_channel.close(); } catch( Throwable t_is ){}
				return null;
			}
			String sResponseCode = null;
			for( int xResponseField = 1; xResponseField <= asHTTPResponse.length - 1; xResponseField++ ){
				if( asHTTPResponse[xResponseField] != null && asHTTPResponse[xResponseField].length() > 0 ){
					sResponseCode = asHTTPResponse[xResponseField];
					break;
				}
			}
			if( sResponseCode == null ){
				sbError.append("server response had no error/success code2: " + Utility.sSafeSubstring(sHTTP_Response, 0, 120));
				try { socket_channel.close(); } catch( Throwable t_is ){}
				return null;
			}
			if( !sResponseCode.equals("200") ){
				sbError.append("server returned HTTP error code: " + Utility.sSafeSubstring(sHTTP_Response, 0, 500));
				try { socket_channel.close(); } catch( Throwable t_is ){}
				return null;
			}
			String sContentDescription = getHeaderField(sHeader, "content-description");
			String sContentLength = getHeaderField(sHeader, "content-length");
			if (sContentLength != null){
				try {
					iContentLength = Integer.parseInt(sContentLength);
				} catch(Exception ex) {
					ApplicationController.vShowWarning("failed to interpret content length (" + sContentLength + ") as integer");
				}
			}
			String sTransferEncoding = getHeaderField(sHeader, "transfer-encoding");
			if( sTransferEncoding != null && sTransferEncoding.equalsIgnoreCase("chunked") ) zChunked = true;
			if( listServerCookies != null ){ // scan for any cookies
				int xCookie = 0;
				while( true ){
					xCookie++;
					if( xCookie > 1000 ) break; // overflow protection
					String sCookieText = getHeaderField(sHeader, "Set-Cookie", xCookie);
					if( sCookieText == null ) break; else listServerCookies.add(sCookieText);
				}
			}
		} else {
			if( zLogHeaders ) ApplicationController.getInstance().vShowWarning("No header http://" + sHost + ":" + iPort + sRequestLocator);
		}

		String sDocument;
		if( zChunked ){
			if( activity != null ) activity.vUpdateStatus("received header, reading chunked document");
			sDocument = getChunkedDocument(socket_channel, iTimeoutRead_seconds, bc, activity, sbError);
		} else {
			if( zHasContentLength ){
				if( activity != null ) activity.vUpdateStatus("received header, reading document length " + iContentLength);
				sDocument = getRemainder(socket_channel, iContentLength, iTimeoutRead_seconds, bc, activity, sbError);
			} else {
				if( activity != null ) activity.vUpdateStatus("received header, reading document of unknown length");
				sDocument = getRemainder(socket_channel, iTimeoutRead_seconds, bc, activity, sbError);
			}
		}

		if( sDocument == null ) return null;

		if( !zHeaderExists ) sDocument = sHeader + sDocument; // the header fragment will actually be part of the document

        return sDocument;
    }

	// scan header ([CR]LF [CR]LF signals end of header)
	// if header does not start with HTTP/ then treat return as content
	static String sScanHeaderHTTP(SocketChannel socket_channel, int iTimeoutRead_seconds, Activity activity, StringBuffer sbError){
		StringBuffer sbHeader = new StringBuffer(MAX_HEADER_BYTES);
		try {
			ByteBuffer buffer = ByteBuffer.allocate(1); // read one byte at a time
			int ctTotalBytesRead = 0;
			int ctLineBytesRead = 0;
			int buffer_offset = 0;
			long nReadStarted = System.currentTimeMillis();
			long nLastDot = System.currentTimeMillis();
			int ctDots = 0;
ReadHeader:
			while( true ){
				switch( socket_channel.read(buffer) ){
					case -1: // end of stream
						String sHeader1000 = Utility.sSafeSubstring(sbHeader.toString(), 1, 1000);
						// sHeader1000 = Utility.sReplaceString(sHeader1000, "\r", "\\r");
						// sHeader1000 = Utility.sReplaceString(sHeader1000, "\n", "\\n");
						try { socket_channel.close(); } catch( Throwable t_is ){}
						if( ctTotalBytesRead == 0 ){
							return ""; // document is empty
						} else {
							sbError.append("premature end of stream in header after " + ctTotalBytesRead + " bytes read, header: [" + sbHeader + "]");
							return null;
						}
					case 0: // nothing read
						Thread.sleep(TIMEOUT_CHECK_INTERVAL_ms);
						if( (System.currentTimeMillis() - nLastDot) > 1000 ){ // make a dot every second
							ctDots++;
							nLastDot = System.currentTimeMillis();
							String sStatus = "waiting for header " + Utility.sRepeatChar('.', ctDots);
							ApplicationController.vShowStatus_NoCache(sStatus);
						}
						if( (System.currentTimeMillis() - nReadStarted) > iTimeoutRead_seconds * 1000 ){
							sbError.append( "timeout waiting to read (see help topic Timeouts) after " + ctTotalBytesRead + " bytes read, header: [" + Utility.sSafeSubstring(sbHeader.toString(), 1, 120) + "]" );
							try { socket_channel.close(); } catch( Throwable t_is ){}
							return null;
						}
						break;
					case 1: // read a byte
						buffer.flip(); // prepare to read byte
						byte b = buffer.get();
						buffer.clear(); // reset buffer for next read
						sbHeader.append((char)b);
						ctTotalBytesRead++;
						ctLineBytesRead++;
						if( ctTotalBytesRead > MAX_HEADER_BYTES ){
							sbError.append("Maximum number of header bytes (" + MAX_HEADER_BYTES + ") exceeded by header: [" + Utility.sSafeSubstring(sbHeader.toString(), 1, 120) + "...]" );
							try { socket_channel.close(); } catch( Throwable t_is ){}
							return null;
						} else if( ctTotalBytesRead == 5 ){
							if( ! sbHeader.toString().equalsIgnoreCase("HTTP/") ){

								// in this case the server has returned the content directly
								// with no header; return just the content we have so far
								break ReadHeader;
							}
						}

						// the below logic treats two newlines in a row as the end of the
						// header not including carriage returns. in theory header lines
						// must be ended by \r\n by the specification but some server
						// writers implement the protocol incorrectly and use only new lines
						if( b == 13 ) ctLineBytesRead--; // do not include carriage returns
						if( b == 10 ){ // found end of line
							if( ctLineBytesRead == 1 ) break ReadHeader; // found end of header
							ctLineBytesRead = 0; // found end of line
						}
						nReadStarted = System.currentTimeMillis(); // reset the read timer
						ctDots = 0; // reset the dot counter
				}
				if( activity != null && activity.isCancelled() ){
					sbError.append("operation cancelled by user");
					return null;
				}
			}
			return sbHeader.toString();
		} catch( Throwable t ) {
			sbError.append("error scanning header: ");
			Utility.vUnexpectedError(t, sbError);
			try { socket_channel.close(); } catch( Throwable t_is ){}
			return null;
		}
	}

	private static int getHeaderFieldCount( String sHeader ){
		int posCursor = 0;
		int ctHeaderFields = 0;
		do {
			posCursor = sHeader.indexOf("\n", posCursor) + 1;
			ctHeaderFields++;
		} while( posCursor > 0 );
		return ctHeaderFields - 1; // the last line is blank so subtract one
	}

	private static String getHeaderField( String sHeader, int xField1 ){
		if( xField1 < 1 || xField1 > getHeaderFieldCount( sHeader ) ) return null;
		int posFieldBegin = 0;
		int posFieldEnd = 0;
		int xHeaderField = 0;
		do {
			posFieldEnd = sHeader.indexOf("\n", posFieldBegin); // some servers use only \n as the term instead of the protocol specification
			xHeaderField++;
			if( xHeaderField == xField1 ) return sHeader.substring(posFieldBegin, posFieldEnd).trim();
			posFieldBegin += 2;
		} while( posFieldEnd >= 0 );
		return null; // should not happen
	}

	private static String getHeaderField( String sHeader, String sFieldName ){
		return getHeaderField( sHeader, sFieldName, 1 );
	}

	private static String getHeaderField( String sHeader, String sFieldName, int iIncidence ){
		int xIncidence = 0;
		int posStart = 0;
		while( true ){
			int posField = sHeader.toUpperCase().indexOf("\n" + sFieldName.toUpperCase(), posStart);
			if( posField == -1 ) return null;
			xIncidence++;
			int posEnd = sHeader.indexOf("\n", posField + 1);
    		if( posEnd == -1 ) posEnd = sHeader.length(); // should not happen
			if( xIncidence == iIncidence ){
		    	return sHeader.substring(posField + 1 + sFieldName.length() + 1, posEnd).trim(); // the plus one is for the colon
			} else {
				posStart = posEnd;
			}
		}
	}

	private static String getRemainder( SocketChannel socket_channel, int iReadTimeout_seconds, ByteCounter byte_counter, Activity activity, StringBuffer sbError ){
		return getRemainder( socket_channel, 0, iReadTimeout_seconds, byte_counter, activity, sbError );
	}

	/** if the iContentLength is zero then there will be no limit */
	private static String getRemainder( SocketChannel socket_channel, int iContentLength, int iReadTimeout_seconds, ByteCounter byte_counter, Activity activity, StringBuffer sbError ){
		StringBuffer sbContent = new StringBuffer(1024);
		long ctTotalBytesRead = 0;
		try {
			ByteBuffer bb = ByteBuffer.allocateDirect(1024);
			long nReadStarted = System.currentTimeMillis();
			long mnLastByteCountReport = nReadStarted;
			while( true ){
				if( activity != null && activity.isCancelled() ){
					sbError.append("operation cancelled by user during document read");
					return null;
				}
				int iBytesRead = socket_channel.read(bb);
				switch( iBytesRead ){
					case -1: // end of stream
						return sbContent.toString();
					case 0: // nothing read
						Thread.sleep(TIMEOUT_CHECK_INTERVAL_ms);
						if( (System.currentTimeMillis() - nReadStarted) > iReadTimeout_seconds * 1000 ){
							sbError.append( "timeout waiting to read (see help topic Timeouts) remainder after " + ctTotalBytesRead + " bytes read, content: [" + Utility.sSafeSubstring(sbContent, 1, 120) + "]" );
							if( byte_counter != null ) byte_counter.vReportByteCount_Total( ctTotalBytesRead );
							return null;
						}
					default: // read bytes
						bb.flip(); // prepare to read byte buffer
					    while( bb.remaining() > 0 ){
							sbContent.append((char)bb.get());
						}
						ctTotalBytesRead += iBytesRead;
						if( iContentLength > 0 && ctTotalBytesRead > iContentLength ){
							return sbContent.toString();
						}
						bb.clear();
		    			nReadStarted = System.currentTimeMillis(); // reset the read timer
						if( byte_counter != null )
							if( System.currentTimeMillis() - mnLastByteCountReport > 1000 ){
								byte_counter.vReportByteCount_EverySecond(ctTotalBytesRead);
								mnLastByteCountReport = System.currentTimeMillis();
							}
				}
			}
		} catch( Throwable t ) {
			Utility.vUnexpectedError(t, sbError);
			return null;
		}
	}

	private static String getChunkedDocument( SocketChannel socket_channel, int iReadTimeout_seconds, ByteCounter byte_counter, Activity activity, StringBuffer sbError ){
		StringBuffer sbContent = new StringBuffer(1024);
		StringBuffer sbChunkHeader = new StringBuffer(80);
		long ctTotalBytesRead = 0;
		try {
			ByteBuffer bb = ByteBuffer.allocateDirect(1024);
			long nReadStarted = System.currentTimeMillis();
			long mnLastByteCountReport = nReadStarted;
			int eState = 1; // in chunk line
			int iChunkSize = 0;
			int posChunk = 0;
			while( true ){
				if( activity != null && activity.isCancelled() ){
					sbError.append("operation cancelled by user during chunked read");
					return null;
				}
				int iBytesRead = socket_channel.read(bb);
				switch( iBytesRead ){
					case -1: // end of stream
						return sbContent.toString();
					case 0: // nothing read
						Thread.sleep(TIMEOUT_CHECK_INTERVAL_ms);
						if( (System.currentTimeMillis() - nReadStarted) > iReadTimeout_seconds * 1000 ){
							sbError.append( "timeout waiting to read (see help topic Timeouts) remainder after " + ctTotalBytesRead + " bytes read, content: [" + Utility.sSafeSubstring(sbContent, 1, 120) + "]" );
							if( byte_counter != null ) byte_counter.vReportByteCount_Total( ctTotalBytesRead );
							return null;
						}
					default: // read bytes
						bb.flip(); // prepare to read byte buffer
						while( bb.remaining() > 0 ){
							char cInput = (char)bb.get();
							ctTotalBytesRead += iBytesRead;
							if( eState == 1 ){
								sbChunkHeader.append(cInput);
								int lenHeader = sbChunkHeader.length();
								if( lenHeader > 2 && sbChunkHeader.charAt(lenHeader - 1) == '\n' && sbChunkHeader.charAt(lenHeader - 2) == '\r' ){
									int ctSizeBytes = 0;
									for( ; ctSizeBytes < lenHeader - 2; ctSizeBytes++ ) if( sbChunkHeader.charAt(ctSizeBytes) == ';' ) break;
									String sChunkSize = sbChunkHeader.substring(0, ctSizeBytes);
									try {
										iChunkSize = Integer.parseInt(sChunkSize, 16);
										sbChunkHeader.setLength(0); // reset buffer
									} catch(Exception ex) {
										sbError.append("cannot interpret (" + Utility.sShowUnprintable(sChunkSize) + ") as a chunk size");
										return null;
									}
									if( iChunkSize == 0 ){
										eState = 3; // terminating
								    } else {
										eState = 2; // in content
									}
									posChunk = 0;
								}
							} else if( eState == 2 ){
								posChunk++;
								if( posChunk <= iChunkSize ){
								    sbContent.append(cInput);
								} else {
									// discard input
									if( posChunk == iChunkSize + 2 ) eState = 1; // the extra two bytes are an extra CRLF at the end of chunk
								}
							} else { // terminating by absorbing final CRLF after empty chunk
								posChunk++;
								if( posChunk == 2 ) return sbContent.toString();
							}
						}
						bb.clear();
		    			nReadStarted = System.currentTimeMillis(); // reset the read timer
						if( byte_counter != null )
							if( System.currentTimeMillis() - mnLastByteCountReport > 1000 ){
								byte_counter.vReportByteCount_EverySecond(ctTotalBytesRead);
								mnLastByteCountReport = System.currentTimeMillis();
							}
				}
			}
		} catch( Throwable t ) {
			Utility.vUnexpectedError(t, sbError);
			return null;
		}
	}

	public static SocketChannel oAcquireSocketChannel( String sRequestHost, int iRequestPort, int iConnectTimeout_seconds, Activity activity, StringBuffer sbError ){

		// determine if method is via proxy
		// when using a proxy the full URI is sent in the get part of the request
		// see RFC 2616 section 5.1.2 Request-URI
		String sConnection_Host;
		int iConnection_Port;
		boolean zUseProxy = ConfigurationManager.getInstance().getProperty_ProxyUse();
		if( zUseProxy ){
			sConnection_Host = ConfigurationManager.getInstance().getProperty_ProxyHost();
			String sConnection_Port = ConfigurationManager.getInstance().getProperty_ProxyPort();
			iConnection_Port = Utility.parseInteger_nonnegative( sConnection_Port );
			if( iConnection_Port < 0 || iConnection_Port > 65535 ){
				sbError.append("The specified proxy port [" + sConnection_Port + "] is not a non-negative integer less than 65536. Currently the ODC is set to use a proxy (config setting proxy.Use) but the proxy port [" + sConnection_Port + "] is invalid.");
				return null;
			}
		} else {
			sConnection_Host = sRequestHost;
			iConnection_Port = iRequestPort;
			if( iConnection_Port == -1 ) iConnection_Port = 80;
		}

		// connect to remote host
		SocketChannel socket_channel = null;
		try {
			InetSocketAddress address = new InetSocketAddress(sConnection_Host, iConnection_Port);
			socket_channel = SocketChannel.open();
			if( socket_channel == null ){
				sbError.append("SocketChannel.open() unexpectedly returned null");
				return null;
			}
			socket_channel.configureBlocking(false);
			long nConnectionStarted = System.currentTimeMillis();
			if( activity != null ) activity.vUpdateStatus("connecting...");
			ApplicationController.vShowStatus_NoCache("connecting...");
			socket_channel.connect(address);
			long nLastDot = nConnectionStarted;
			while( !socket_channel.finishConnect() ){
				if( activity != null && activity.isCancelled() ){
					sbError.append("operation cancelled by user during connect");
					return null;
				}
				Thread.sleep(TIMEOUT_CHECK_INTERVAL_ms);
				if( (System.currentTimeMillis() - nConnectionStarted) > iConnectTimeout_seconds * 1000 ){
					sbError.append( "timeout waiting to connect (see help topic Timeouts)" );
					return null;
				}
				if( System.currentTimeMillis() - nLastDot > 1000 ){
					ApplicationController.vShowStatus_NoCache_Append(".");
					nLastDot = System.currentTimeMillis();
				}
			}
			ApplicationController.vClearStatus();
		} catch( java.lang.InterruptedException ex ) { // the user has cancelled the action
			return null;
		} catch( java.net.ConnectException ex ) {
			sbError.append("error connecting to " + (zUseProxy ? "proxy" : "remote host") + " (" + sConnection_Host + ":" + iConnection_Port + "): " + ex);
			return null;
		} catch( Throwable t ) {
			sbError.append("error during open of " + (zUseProxy ? "proxy" : "remote host") + " (" + sConnection_Host + ":" + iConnection_Port + "): " + t);
			Utility.vUnexpectedError(t, sbError);
			return null;
		}

		return socket_channel;
	}

}

/**
 * A <code>ChunkedInputStream</code> provides a stream for reading a body of
 * a http message that can be sent as a series of chunks, each with its own
 * size indicator. Optionally the last chunk can be followed by trailers
 * containing entity-header fields.
 */
class InputStream_Chunked extends InputStream {

    private InputStream in;
    private byte[] abRawBuffer = new byte[32];
    private int posRawBuffer;
    private int iRawBufferLength; // number of valid bytes in the raw buffer

    private int iChunkSize;
    private int ctChunkBytesRead; // from 0 to iChunkSize -- number of bytes read so far
    private int posChunkBuffer; // from 0 to iChunkSize-1 -- next byte to be read
    private byte[] abChunkBuffer = new byte[4096];
    private int iChunkBufferLength; // number of valid bytes in the chunked buffer

    static final int STATE_AWAITING_CHUNK_HEADER  = 1; // expecting "chunk-size [ chunk-extension ] CRLF"
    static final int STATE_READING_CHUNK	      = 2;
    static final int STATE_AWAITING_CHUNK_EOL	  = 3;
    static final int STATE_AWAITING_TRAILERS	  = 4;
    static final int STATE_DONE			          = 5;

    private int eState;
	private boolean mzError = false;
	private boolean mzAllowBlocking = false;
    private boolean mzStreamClosed;

    public InputStream_Chunked( InputStream in, boolean zAllowBlocking ) throws IOException {
        this.in = in;
		this.mzAllowBlocking = zAllowBlocking;
		eState = STATE_AWAITING_CHUNK_HEADER;
    }

	public static final boolean test( StringBuffer sbError ){
		String sChunkingServerURL = "";
		int iTimeoutConnect_seconds = 30;
		int iTimeoutRead_seconds = 30;
		URL url;
		try {
			url = new URL(sChunkingServerURL);
		} catch(Exception ex) {
			sbError.append("Error forming URL " + sChunkingServerURL + ": " + ex);
			return false;
		}
		OpendapConnection oc = new OpendapConnection();
		ByteTracker bt =  oc.openConnection(url, iTimeoutConnect_seconds, iTimeoutRead_seconds, null, null, sbError );
		if( bt == null ){
			if( sbError.length() > 0 )
				sbError.insert(0, "error making connection to " + url + ": ");
			return false;
		}
		byte[] abContent_chunked = getStreamContent( bt, sbError );

        return true;
	}

	/** returns 1-based array of the data in bytes; length of data is length of array - 1 */
	private static byte[] getStreamContent( InputStream in, StringBuffer sbError ){
		byte[] abContent = new byte[10000];
		int posDDS = 0;
		try {
			while( true ){
				int iByte = in.read();
				if( iByte == -1 ) break;
				posDDS++;
				abContent[posDDS] = (byte)iByte;
				if( posDDS == abContent.length ){ // need to expand array
					byte[] abContent_doubled = new byte[abContent.length * 2];
					System.arraycopy(abContent, 0, abContent_doubled, 0, abContent.length);
					abContent = abContent_doubled;
				}
			}
			byte[] abContent_complete = new byte[ posDDS + 1 ];
			System.arraycopy(abContent, 0, abContent_complete, 0, posDDS + 1);
			return abContent_complete;
        } catch (Throwable t) {
			sbError.append("error reading data: " + t);
			return null;
        }
	}

    private void ensureOpen() throws IOException {
		if( mzStreamClosed ){
			throw new IOException("stream is closed");
		}
    }

    /**
     * Ensures there is <code>size</code> bytes available in
     * <code>rawData<code>. This requires that we either
     * shift the bytes in use to the begining of the buffer
     * or allocate a large buffer with sufficient space available.
     */
	private void ensureRawAvailable(int size) {
		if( iRawBufferLength + size > abRawBuffer.length ){
			int used = iRawBufferLength - posRawBuffer;
			if (used + size > abRawBuffer.length) {
				byte tmp[] = new byte[used + size];
				if (used > 0) {
					System.arraycopy(abRawBuffer, posRawBuffer, tmp, 0, used);
				}
				abRawBuffer = tmp;
			}
			else {
				if (used > 0) {
					System.arraycopy(abRawBuffer, posRawBuffer, abRawBuffer, 0, used);
				}
			}
			iRawBufferLength = used;
			posRawBuffer = 0;
		}
	}

	// read transfer while inside chunk
	private int fastRead(byte[] b, int off, int len) throws IOException {
		int remaining = iChunkBufferLength - ctChunkBytesRead;
		int ctReadableBytes = (remaining < len) ? remaining : len;
		if( ctReadableBytes > 0 ){
			int nread;
			try {
				nread = in.read(b, off, ctReadableBytes);
			} catch (IOException ex) {
				mzError = true;
				throw ex;
			}
			if( nread > 0 ){
				ctChunkBytesRead += nread;
				if( ctChunkBytesRead >= iChunkBufferLength ){
					eState = STATE_AWAITING_CHUNK_EOL;
				}
				return nread;
			}
			mzError = true;
			throw new IOException("Premature EOF in fast read");
		} else {
			return 0;
		}
	}

    /**
     * Process any outstanding bytes that have already been read into
     * <code>rawData</code>.
     * <p>
     * The parsing of the chunked stream is performed as a state machine with
     * <code>state</code> representing the current state of the processing.
     * <p>
     * Returns when either all the outstanding bytes in rawData have been
     * processed or there is insufficient bytes available to continue
     * processing. When the latter occurs <code>rawPos</code> will not have
     * been updated and thus the processing can be restarted once further
     * bytes have been read into <code>rawData</code>.
     */
	private void processRaw() throws IOException {
		int pos;
		int i;
		while( eState != STATE_DONE ){
			switch( eState ){
				case STATE_AWAITING_CHUNK_HEADER: // newline terminates chunk header
					pos = posRawBuffer;
					while( pos < iRawBufferLength ){
						if( abRawBuffer[pos] == '\n' ) break;
						pos++;
					}
					if( pos >= iRawBufferLength ) return;

					// extract the chunk size from the header (ignoring extensions).
					String sHeader = new String( abRawBuffer, posRawBuffer, pos - posRawBuffer + 1);
					int posHeader = 0;
					for( ; posHeader < sHeader.length(); posHeader++ ){
						if( Character.digit(sHeader.charAt(posHeader), 16) == -1 ) break;
					}
					try {
						iChunkSize = Integer.parseInt(sHeader.substring(0, posHeader), 16);
					} catch( Exception ex ) {
						mzError = true;
						throw new IOException("Unable to interpret [" + sHeader + "] as a chunk size header");
					}
					posRawBuffer = pos + 1; // beginning of chunk content
					ctChunkBytesRead = 0;
					if( iChunkSize > 0 ){
						eState = STATE_READING_CHUNK;
					} else {
						eState = STATE_AWAITING_TRAILERS;
					}
					break;

				case STATE_READING_CHUNK:
					if( posRawBuffer >= iRawBufferLength ) return; // no data available
					int ctChunkRemaining = iChunkSize - ctChunkBytesRead;
					int ctRawRemaining   = iRawBufferLength - posRawBuffer;
					int ctReadableBytes = ctChunkRemaining < ctRawRemaining ? ctChunkRemaining : ctRawRemaining;
					if( abChunkBuffer.length < iChunkBufferLength + ctReadableBytes ){
						int ctBytesToMoves = iChunkSize - posChunkBuffer;
						if( abChunkBuffer.length < ctBytesToMoves + ctReadableBytes ){ // expand chunk buffer
							byte[] abNewChunkBuffer = new byte[ctBytesToMoves + ctReadableBytes];
							System.arraycopy(abChunkBuffer, posChunkBuffer, abNewChunkBuffer, 0, ctBytesToMoves);
							abChunkBuffer = abNewChunkBuffer;
						} else {
							System.arraycopy(abChunkBuffer, posChunkBuffer, abChunkBuffer, 0, ctBytesToMoves);
						}
						posChunkBuffer = 0;
						ctChunkBytesRead = ctBytesToMoves;
					}

					System.arraycopy(abRawBuffer, posRawBuffer, abChunkBuffer, ctChunkBytesRead, ctReadableBytes);
					posRawBuffer       += ctReadableBytes;
					ctChunkBytesRead   += ctReadableBytes;
					iChunkBufferLength += ctReadableBytes;

					if( iChunkSize - ctChunkBytesRead <= 0 ){ // chunk has been read
						eState = STATE_AWAITING_CHUNK_EOL;
					} else {
						return;
					}
					break;

				case STATE_AWAITING_CHUNK_EOL:
					if( posRawBuffer + 1 >= iRawBufferLength ) return;
					if( abRawBuffer[posRawBuffer] != '\r') {
						mzError = true;
						throw new IOException("missing CR");
					}
					if( abRawBuffer[posRawBuffer + 1] != '\n') {
						mzError = true;
						throw new IOException("missing LF");
					}
					posRawBuffer += 2;
					eState = STATE_AWAITING_CHUNK_HEADER;
					break;
				case STATE_AWAITING_TRAILERS:
					pos = posRawBuffer;
					while( pos < iRawBufferLength ){
						if( abRawBuffer[pos] == '\n') break;
						pos++;
					}
					if( pos >= iRawBufferLength ) return;
					if( pos == posRawBuffer ){
						mzError = true;
						throw new IOException("LF not preceded by CR");
					}
					if( abRawBuffer[pos - 1] != '\r' ){
						mzError = true;
						throw new IOException("LF not preceded by CR");
					}
					if( pos == (posRawBuffer + 1) ){
						eState = STATE_DONE;
						closeUnderlying();
						return;
					}

					// extract trailer
					String sTrailer = new String(abRawBuffer, posRawBuffer, pos - posRawBuffer);
					int posKeySeparator = sTrailer.indexOf(':');
					if( posKeySeparator == -1 ){
						throw new IOException("Malformed tailer - format should be key:value");
					}
					String key = (sTrailer.substring(0, posKeySeparator)).trim();
					String value = (sTrailer.substring(posKeySeparator + 1, sTrailer.length())).trim();
					// todo handle trailers -- for now trailers are ignored

					posRawBuffer = pos + 1; // next trailer
					break;

			}
		}
	}


    /**
     * Reads any available bytes from the underlying stream into
     * <code>rawData</code> and returns the number of bytes of
     * chunk data available in <code>chunkData</code> that the
     * application can read.
     */
	private int readAheadNonBlocking() throws IOException {

		/*
		 * If there's anything available on the underlying stream then we read
		 * it into the raw buffer and process it. Processing ensures that any
		 * available chunk data is made available in chunkData.
		 */
		int avail = in.available();
		if( avail > 0 ){
			ensureRawAvailable(avail);
			int nread;
			try {
				nread = in.read( abRawBuffer, iRawBufferLength, avail);
			} catch (IOException ex) {
				mzError = true;
				throw ex;
			}
			if( nread < 0 ){
				mzError = true; /* premature EOF ? */
				return -1;
			}
			iRawBufferLength += nread;
			processRaw();
		}

		return iChunkBufferLength - posChunkBuffer; // readable byte count
	}

	private int readAheadBlocking() throws IOException {
		do { // read raw bytes
			if( eState == STATE_DONE ) return -1;
			ensureRawAvailable(32);
			int nread;
			try {
				nread = in.read(abRawBuffer, iRawBufferLength, abRawBuffer.length - iRawBufferLength);
			} catch( IOException exRead ){
				mzError = true;
				throw exRead;
			}
			if( nread < 0 ){
				mzError = true;
				throw new IOException("premature EOF encountered in chunked stream");
			}
			iChunkBufferLength += nread;
			processRaw();
		} while( iChunkBufferLength <= 0 );
		return iChunkBufferLength - posChunkBuffer;
	}

	private int readAhead(boolean allowBlocking) throws IOException {
		if (eState == STATE_DONE) {
			return -1;
		}
		if( posChunkBuffer >= iChunkBufferLength ){ // start over at beginning of buffer
			iChunkBufferLength = 0;
			posChunkBuffer = 0;
		}
		if( mzAllowBlocking ){
			return readAheadBlocking();
		} else {
			return readAheadNonBlocking();
		}
	}

	// super class methods

	public synchronized int read() throws IOException {
		ensureOpen();
		if( posChunkBuffer >= iChunkBufferLength){
			if( readAhead(true) <= 0 ){
				return -1;
			}
		}
		return abChunkBuffer[posChunkBuffer++] & 0xff;
	}

	public synchronized int read(byte b[], int off, int len) throws IOException {
		ensureOpen();
		if ( (off < 0) || (off > b.length) || (len < 0) ||
			( (off + len) > b.length) || ( (off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		} else if( len == 0 ){
			return 0;
		}

		int avail = iChunkBufferLength - posChunkBuffer;
		if( avail <= 0 ){
			if( eState == STATE_READING_CHUNK) {  // if inside chunk move data directly into stream
				return fastRead(b, off, len);
			}

			// ... else keep reading until inside a chunk
			avail = readAhead(true);
			if (avail < 0) {
				return -1; /* EOF */
			}
		}
		int ctReadableBytes = (avail < len) ? avail : len;
		System.arraycopy(abChunkBuffer, posChunkBuffer, b, off, ctReadableBytes);
		posChunkBuffer += ctReadableBytes;

		return ctReadableBytes;
	}

	public synchronized int available() throws IOException {
		ensureOpen();
		int avail = readAhead(false);
		if (avail < 0) {
			return 0;
		} else {
			return avail;
		}
	}

	public synchronized void close() throws IOException {
		if( mzStreamClosed ) return;
		closeUnderlying();
		mzStreamClosed = true;
	}


	/**
	* Close the underlying input stream by either returning it to the
	* keep alive cache or closing the stream.
	* <p>
	* As a chunked stream is inheritly persistent (see HTTP 1.1 RFC) the
	* underlying stream can be returned to the keep alive cache if the
	* stream can be completely read without error.
	*/
	private void closeUnderlying() throws IOException {
		if (in == null) return;

		if (!mzError && eState == STATE_DONE) {
		    // finished
		} else {
			if (!hurry()) {
				// close client
			}
		}
		in = null;
	}

	/**
	 * Hurry the input stream by reading everything from the underlying
	 * stream. If the last chunk (and optional trailers) can be read without
	 * blocking then the stream is considered hurried.
	 * <p>
	 * Note that if an error has occured or we can't get to last chunk
	 * without blocking then this stream can't be hurried and should be
	 * closed.
	 */
	public synchronized boolean hurry() {
		if( in == null || mzError ){
			return false;
		}

		try {
			readAhead(false);
		} catch (Exception e) {
			return false;
		}

		if( mzError ){
			return false;
		}

		return (eState == STATE_DONE);
	}

}
