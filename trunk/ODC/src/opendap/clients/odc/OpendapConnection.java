package opendap.clients.odc;

import java.net.*;
import java.io.*;
import opendap.dap.parser.ParseException;
import java.util.zip.InflaterInputStream;

import java.nio.*;
import java.nio.channels.*;

import opendap.dap.ServerVersion;
import opendap.dap.DODSException;
import opendap.dap.DDSException;
import opendap.dap.DASException;
import opendap.dap.DAS;
import opendap.dap.DDS;
import opendap.dap.DataDDS;
import opendap.dap.BaseTypeFactory;
import opendap.dap.StatusUI;
import opendap.dap.DefaultFactory;

public class OpendapConnection {

	public final static String DEFAULT_Protocol       = "HTTP/1.1";
	public final static int TIMEOUT_CHECK_INTERVAL_ms = 100;
	public final static int TIMEOUT_DEFAULT_CONNECT   = 20;
	public final static int TIMEOUT_DEFAULT_READ      = 10;
	public final static int MAX_HEADER_BYTES          = 10000; // headers larger than this unacceptable
	public final static int MAX_ERROR_BYTES           = 10000; // the largest acceptable error
	public final static int BUFFER_SIZE               = 1024 * 10;
    private ServerVersion ver;

	private int miTimeoutConnect_seconds = TIMEOUT_DEFAULT_CONNECT;
	private int miTimeoutRead_seconds = TIMEOUT_DEFAULT_READ;
	private String msUserAgent = "OPeNDAP client";

	private ByteTracker mByteTracker;

    public OpendapConnection(){
		miTimeoutConnect_seconds = ConfigurationManager.getInstance().getProperty_Timeout_InternetConnect();
		miTimeoutRead_seconds = ConfigurationManager.getInstance().getProperty_Timeout_InternetRead();
	}

	public void setTimeoutsInSeconds( int iTimeoutConnect, int iTimeoutRead ){
		if( iTimeoutConnect >= 0 ) miTimeoutConnect_seconds = iTimeoutConnect;
		if( iTimeoutRead >= 0 ) miTimeoutRead_seconds = iTimeoutRead;
	}

	public void setUserAgent( String sUserAgent ){
		msUserAgent = sUserAgent;
	}

    /**
     * Open a connection to the DODS server.
     * @param url the URL to open.
     * @param sbError Buffer to write any error message to.
     * @return the opened <code>InputStream</code> or null in the case of an error.
     */
    ByteTracker openConnection(URL url, int iConnectTimeout_seconds, int iReadTimeout_seconds, ByteCounter bc, Activity activity, StringBuffer sbError) {
		return openConnection( url, iConnectTimeout_seconds, iReadTimeout_seconds, bc, null, activity, sbError );
	}

    /**
     * Open a connection to the DODS server.
     * @param url the URL to open.
     * @param sbError Buffer to write any error message to.
     * @return the opened <code>InputStream</code> or null in the case of an error.
     */
    ByteTracker openConnection(URL url, int iConnectTimeout_seconds, int iReadTimeout_seconds, ByteCounter bc, String sProtocol, Activity activity, StringBuffer sbError) {
		if( url == null ){
			sbError.append("URL missing");
			return null;
		}

		boolean zLogHeaders = ConfigurationManager.getInstance().getProperty_LOGGING_ShowHeaders();

		// determine request parameters
		String sRequest_Host           = url.getHost();
		int    iRequest_Port           = url.getPort();
		String sRequest_Query          = url.getQuery();
		boolean zUseProxy = ConfigurationManager.getInstance().getProperty_ProxyUse();
		String sRequestLocator;
		if( zUseProxy ){
			sRequestLocator = url.toString();
		} else {
			sRequestLocator = url.getPath() + (sRequest_Query == null ? "" : '?' + sRequest_Query);
		}

		SocketChannel socket_channel = IO.oAcquireSocketChannel(sRequest_Host, iRequest_Port, iConnectTimeout_seconds, activity, sbError);
		if( socket_channel == null ){
			sbError.insert(0, "failed to acquire socket channel: ");
			return null;
		}

		// build request string
		String sRequest;
		if( sProtocol == null || sProtocol.trim().length() == 0 ) sProtocol = DEFAULT_Protocol;
		sRequest  = "GET " + sRequestLocator + " " + sProtocol + "\r\n";
		sRequest += "Host: " + sRequest_Host + "\r\n";
		sRequest += "User-Agent: " + msUserAgent + "\r\n";
		sRequest += "Accept: text/*\r\n";
		sRequest += "\r\n"; // blank line signals end of request

		String sRequest_Print;
	    if( zUseProxy ){
			sRequest_Print = sRequestLocator;
		} else {
			sRequest_Print = "http://" + sRequest_Host + ":" + iRequest_Port + sRequestLocator;
		}

		if( zLogHeaders ) ApplicationController.getInstance().vShowStatus("Request header:\n" + Utility.sShowUnprintable(sRequest));

		try {
			if( activity != null ) activity.vUpdateStatus("sending request");
			socket_channel.write(ByteBuffer.wrap(sRequest.getBytes()));
		} catch( Throwable t ) {
			Utility.vUnexpectedError(t, "rendering request for " + sRequestLocator + " to remote host (" + sRequest_Host + ":" + iRequest_Port + ")");
			try { socket_channel.close(); } catch( Throwable t_is ){}
			return null;
		}

		String sHeader = IO.sScanHeaderHTTP( socket_channel, iReadTimeout_seconds, activity, sbError );
		if( sHeader == null ){
			sbError.insert(0, "scanning header: ");
			return null;
		} else if( sHeader.length() == 0) {
			sbError.append("server returned nothing (" + sRequest_Print + ")");
			return null;
		}
		boolean zHeaderExists = sHeader.toUpperCase().startsWith("HTTP/");

		if( zHeaderExists ){
			if( zLogHeaders )
				ApplicationController.getInstance().vShowStatus("Response header for (" + sRequest_Print + "):\n" + Utility.sShowUnprintable(sHeader));
			String sHTTP_Response = getHeaderField( sHeader, 1 );
			if( sHTTP_Response == null ){
				sbError.append("server response was empty");
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
				sbError.append("server returned HTTP error code: " + Utility.sSafeSubstring(sHTTP_Response, 0, 250));
//				System.out.println( getRemainder(socket_channel, 10000, iReadTimeout_seconds) );
				try { socket_channel.close(); } catch( Throwable t_is ){}
				return null;
			}
			String type = getHeaderField(sHeader, "content-description");
			if (type != null && (type.equalsIgnoreCase("dods_error") || type.equalsIgnoreCase("dods-error") )) {
				sbError.append("server error: " + getRemainder(socket_channel, 1000, iReadTimeout_seconds, activity));
				try { socket_channel.close(); } catch( Throwable t_is ){}
				return null;
			}
			String sServerVersion = getHeaderField(sHeader, "xdods-server");
			if (sServerVersion == null){
				sbError.append("not a valid OPeNDAP server, missing MIME Header field \"xdods-server.\" (" + sRequest_Print + ")");
				try { socket_channel.close(); } catch( Throwable t_is ){}
				return null;
			}
			ver = new ServerVersion(sServerVersion);
			String sTransferEncoding = getHeaderField(sHeader, "transfer-encoding");
			if( sTransferEncoding != null ){
				if( sProtocol.equals("HTTP/1.0") ){
					sbError.append("no byte tracker support for transfer encoding: " + sTransferEncoding);
	    			try { socket_channel.close(); } catch( Throwable t_is ){}
		    		return null;
				} else { // trying using 1.0 protocol
	    			try { socket_channel.close(); } catch( Throwable t_is ){}
					ApplicationController.vShowWarning("trying HTTP/1.0 because server is chunking (" + sRequest_Print + ")");
					mByteTracker = openConnection(url, iConnectTimeout_seconds, iReadTimeout_seconds, bc, "HTTP/1.0", activity, sbError);
					if( mByteTracker == null ){
						sbError.insert(0, "(" + sRequest_Print + ") failed using HTTP/1.0: ");
						return null;
					} else {
						return mByteTracker;
					}
				}
			}
		} else {
			if( zLogHeaders )
				ApplicationController.getInstance().vShowWarning("No header returned for " + sRequest_Print);
		}

		// create and return the ByteTracker
		mByteTracker = new ByteTracker( activity, socket_channel, BUFFER_SIZE, iReadTimeout_seconds, bc );
        return mByteTracker;
    }

	private int getHeaderFieldCount( String sHeader ){
		int posCursor = 0;
		int ctHeaderFields = 0;
		do {
			posCursor = sHeader.indexOf("\n", posCursor) + 1;
			ctHeaderFields++;
		} while( posCursor > 0 );
		return ctHeaderFields - 1; // the last line is blank so subtract one
	}

	private String getHeaderField( String sHeader, int xField1 ){
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

	private String getHeaderField( String sHeader, String sFieldName ){
		int posField = sHeader.toUpperCase().indexOf("\n" + sFieldName.toUpperCase());
		if( posField == -1 ) return null;
		int posEnd = sHeader.indexOf("\n", posField + 1);
		if( posEnd == -1 ) posEnd = sHeader.length(); // should not happen
		return sHeader.substring(posField + 1 + sFieldName.length() + 1, posEnd).trim(); // the plus one is for the colon
	}

	/** if the iByteCountLimit is zero then there will be no limit */
	private String getRemainder( SocketChannel socket_channel, int iApproximateByteCountLimit, int iReadTimeout_seconds, Activity activity ){
		StringBuffer sbContent = new StringBuffer(1024);
		int ctTotalBytesRead = 0;
		try {
			ByteBuffer bb = ByteBuffer.allocateDirect(1024);
			long nReadStarted = System.currentTimeMillis();
			while( true ){
				if( activity != null && activity.isCancelled() ){
					return "[operation cancelled during remainder fetch]";
				}
				int iBytesRead = socket_channel.read(bb);
				switch( iBytesRead ){
					case -1: // end of stream
						return sbContent.toString();
					case 0: // nothing read
						Thread.sleep(TIMEOUT_CHECK_INTERVAL_ms);
						if( (System.currentTimeMillis() - nReadStarted) > iReadTimeout_seconds * 1000 ){
							sbContent.append( "timeout waiting to read (see help topic Timeouts), remainder after " + ctTotalBytesRead + " bytes read, content: [" + Utility.sSafeSubstring(sbContent, 1, 120) + "]" );
							return sbContent.toString();
						}
					default: // read bytes
						bb.flip(); // prepare to read byte buffer
					    while( bb.remaining() > 0 ){
							sbContent.append((char)bb.get());
						}
						ctTotalBytesRead += iBytesRead;
						if( iApproximateByteCountLimit > 0 && ctTotalBytesRead > iApproximateByteCountLimit ){
							return sbContent.toString();
						}
						bb.clear();
		    			nReadStarted = System.currentTimeMillis(); // reset the read timer

				}
			}
		} catch( java.lang.InterruptedException ex ) { // the user has cancelled the action
			return null;
		} catch( Throwable t ) {
			Utility.vUnexpectedError(t, sbContent);
			return sbContent.toString();
		}
	}

    public String getDDS_Text( String sBaseURL, String sCE, Activity activity, StringBuffer sbError ){
		String sURL = sBaseURL + ".dds" + (sCE == null ? "" : '?' + sCE);
		ByteCounter bc = null; // not necessary for brief transmissions
		return IO.getStaticContent( sURL, bc, activity, sbError );
	}

    public DDS getDDS( String sBaseURL, String sCE, Activity activity, StringBuffer sbError ){
		InputStream is = getDDS_InputStream( sBaseURL, sCE, activity, sbError );
		if( is == null ){
			if( sbError.length() > 0 )
				sbError.insert(0, "error opening connection: ");
			return null;
		}
        DDS dds = new DDS();
        try {
            dds.parse(is);
		} catch(Throwable t) {
			sbError.append("error parsing DDS: " + t);
			return null;
        } finally {
			try{ is.close(); } catch(Throwable t) {}
        }
        return dds;
	}

	public InputStream getDDS_InputStream( String sBaseURL, String sCE, Activity activity, StringBuffer sbError ){
		URL url = null;
		String sURL = sBaseURL + ".dds" + (sCE == null ? "" : '?' + sCE);
		try {
			url = new URL(sURL);
		} catch(Exception ex) {
			sbError.append("Error forming URL " + sURL + ": " + ex);
			return null;
		}
		return openConnection(url, miTimeoutConnect_seconds, miTimeoutRead_seconds, null, activity, sbError );
	}

    public DAS getDAS( String sBaseURL, String sCE, Activity activity, StringBuffer sbError ){
		InputStream is = getDAS_InputStream( sBaseURL, sCE, activity, sbError );
		if( is == null ){
			if( sbError.length() > 0 )
				sbError.insert(0, "error making connection: ");
			return null;
		}
        DAS das = new DAS();
        try {
            das.parse(is);
		} catch(Throwable t) {
			sbError.append("error parsing DDS: " + t);
			return null;
        } finally {
			try{ is.close(); } catch(Throwable t) {}
        }
        return das;
    }

    public String getDAS_Text( String sBaseURL, String sCE, Activity activity, StringBuffer sbError ){
		String sURL = sBaseURL + ".das" + (sCE == null ? "" : '?' + sCE);
		ByteCounter bc = null; // not necessary for brief transmissions
		return IO.getStaticContent( sURL, bc, activity, sbError );
	}

	public InputStream getDAS_InputStream( String sBaseURL, String sCE, Activity activity, StringBuffer sbError ){
		URL url = null;
		String sURL = sBaseURL + ".das" + (sCE == null ? "" : '?' + sCE);
		try {
			url = new URL(sURL);
		} catch(Throwable t) {
			sbError.append("Error forming URL " + sURL + ": " + t);
			return null;
		}
	    return openConnection(url, miTimeoutConnect_seconds, miTimeoutRead_seconds, null, activity, sbError );
	}

    public DDS getDDX( String sBaseURL, String sCE, Activity activity, StringBuffer sbError ){
		URL url = null;
		String sURL = sBaseURL + ".ddx" + (sCE == null ? "" : '?' + sCE);
		try {
			url = new URL(sURL);
		} catch(Throwable t) {
			sbError.append("Error forming URL " + sURL + ": " + t);
			return null;
		}
		InputStream is = openConnection(url, miTimeoutConnect_seconds, miTimeoutRead_seconds, null, activity, sbError );
		if( is == null ){
			if( sbError.length() > 0 )
				sbError.insert(0, "error making connection to " + url + ": ");
			return null;
		}
        DDS dds = new DDS();
        try {
            dds.parseXML(is, false);
		} catch(Throwable t) {
			sbError.append("error parsing XML: " + t);
			return null;
        } finally {
			try{ is.close(); } catch(Exception t) {}
        }
        return dds;
    }

    /**
     * Returns the `Data object' from the dataset referenced by this object's
     * URL given the constraint expression CE. Note that the Data object is
     * really just a DDS object with data bound to the variables. The DDS will
     * probably contain fewer variables (and those might have different
     * types) than in the DDS returned by getDDS() because that method returns
     * the entire DDS (but without any data) while this method returns
     * only those variables listed in the projection part of the constraint
     * expression.
     * <p>
     *
     * This method uses the 2 step method for aquiring data from a server using
     * a DDX and a BLOB. First, a DDX (an XML representation of a DDS) is requested.
     * The DDX is parsed and a DataDDS is created.
     * The DDX contains a URL that points to the servers BLOB service. The BLOB
     * service returns only the serialized binary content of the DataDDS. The DataDDS
     * then deserializes the BLOB and fills itself with data.
     *
     */
    public DataDDS getDataDDX( String sBaseURL, String sCE, Activity activity, StringBuffer sbError ){
		URL url = null;
		String sURL = sBaseURL + ".ddx" + (sCE == null ? "" : '?' + sCE);
		try {
			url = new URL(sURL);
		} catch(Exception ex) {
			sbError.append("Error forming URL " + sURL + ": " + ex);
			return null;
		}
		InputStream is = openConnection(url, miTimeoutConnect_seconds, miTimeoutRead_seconds, null, activity, sbError );
		if( is == null ){
			if( sbError.length() > 0 )
				sbError.insert(0, "error making connection to " + url + ": ");
			return null;
		}
		BaseTypeFactory btf = new DefaultFactory();
        DataDDS dds = new DataDDS(ver, btf);
        try {
            dds.parseXML(is, false);
		} catch(Throwable t) {
			sbError.append("error parsing DDX XML: " + t);
			return null;
        } finally {
			try{ is.close(); } catch(Throwable t) {}
        }
		// todo load blob?
        return dds;
    }

    public DataDDS getDataDDS(String sBaseURL, String sCE, ByteCounter bc, Activity activity, StringBuffer sbError){
		URL url = null;
		String sURL = sBaseURL + ".dods" + (sCE == null ? "" : '?' + sCE);
		try {
			url = new URL(sURL);
		} catch(Exception ex) {
			sbError.append("Error forming URL " + sURL + ": " + ex);
			return null;
		}
		ByteTracker bt = openConnection(url, miTimeoutConnect_seconds, miTimeoutRead_seconds, bc, activity, sbError );
		if( bt == null ){
			if( sbError.length() > 0 )
				sbError.insert(0, "error making connection to " + url + ": ");
			return null;
		}
		BaseTypeFactory btf = new DefaultFactory();
        DataDDS data_dds = new DataDDS(ver, btf);

		// get DDS
		// the DDS portion of the data stream is after the header and is separated from the
		// blob by the string "Data:\n"
		byte[] abDDS = new byte[1000];
		byte[] abTerminator = { '\n', 'D', 'a', 't', 'a', ':', '\n' };
		int lenTerminator = abTerminator.length;
		int posTerminator = 0;
		int posDDS = 0;
//System.out.println("reading DDS header:\n");
        try {
			ApplicationController.vShowStatus_NoCache("reading DDS header...");
			if( activity != null ) activity.vUpdateStatus("reading DDS header...");
SearchForTerminator:
			while( true ){
				if( activity != null && activity.isCancelled() ){
					sbError.append("operation cancelled by user during DDS fetch");
					return null;
				}
				int iByte = bt.read();
				if( iByte == -1 ){
					sbError.append("premature end of stream while reading DDS at " + posDDS);
					return null;
				}
				abDDS[posDDS] = (byte)iByte;
				if( posDDS + 1 >= lenTerminator ){
					int xTerminator = 0;
					while( true ){
						xTerminator++;
						if( xTerminator > lenTerminator ) break SearchForTerminator;
						if( abDDS[posDDS - lenTerminator + xTerminator] != abTerminator[xTerminator - 1] ) break;
					}
				}
				posDDS++;
				if( posDDS == abDDS.length ){ // need to expand array
					byte[] abDDS_doubled = new byte[abDDS.length * 2];
					System.arraycopy(abDDS, 0, abDDS_doubled, 0, abDDS.length);
					abDDS = abDDS_doubled;
				}
			}
			int iTerminatorSize = abTerminator.length - 1; // the leading newline is not part of the terminator
			byte[] abDDS_trim = new byte[posDDS - iTerminatorSize];
			System.arraycopy(abDDS, 0, abDDS_trim, 0, abDDS_trim.length);
			abDDS = abDDS_trim;
			ApplicationController.vClearStatus();
        } catch (Throwable t) {
			sbError.append("error reading DDS portion of data: " + t);
			if( ConfigurationManager.getInstance().getProperty_LOGGING_ShowHeaders() ){
				byte[] abDDS_trim = new byte[posDDS];
				System.arraycopy(abDDS, 0, abDDS_trim, 0, abDDS_trim.length);
				sbError.append("\ncontent: \n").append(new String(abDDS_trim));
			}
			return null;
        }
//System.out.println("DataDDS structure:\n" + Utility.sShowUnprintable(new String(abDDS)));
		// parse the DDS
        try {
			ByteArrayInputStream bais =	new java.io.ByteArrayInputStream(abDDS);
			data_dds.parse(bais);
        } catch (Throwable t) {
			sbError.append("error parsing data's DDS: " + t);
			return null;
		}

		// read the blob
		ApplicationController.vShowStatus_NoCache("reading data...");
		if( activity != null ) activity.vUpdateStatus("reading data...");
        try {
            data_dds.readData(bt, null);
        } catch (Throwable t) {
			sbError.append("error reading data: " + t);
			return null;
        } finally {
			try{ bt.close(); } catch(Throwable t) {}
        }

		// validate that there is at least one variable
		java.util.Enumeration enumVariables = data_dds.getVariables();
		if( enumVariables == null || !enumVariables.hasMoreElements() ){
			sbError.append("there were no variables in the returned dataset");
			return null;
		}

        return data_dds;
    }

}

class ByteTracker extends InputStream {
	public final static int TIMEOUT_CHECK_INTERVAL_ms = 100;
	SocketChannel m_socket_channel;
	long nBytesRead = 0;
	ByteBuffer mBuffer;
	boolean mzEndOfStream = false;
	int miReadTimeout_seconds = 10;
	private ByteCounter mByteCounter = null;
	private Activity mActivity;
	ByteTracker( Activity activity, SocketChannel socket_channel, int buffer_size, int iReadTimeout_seconds, ByteCounter bc ){
		mActivity = activity;
		m_socket_channel = socket_channel;
		mByteCounter = bc;
		mBuffer = ByteBuffer.allocateDirect(buffer_size);
		mBuffer.limit(0); // set mBuffer to empty in drain state
		miReadTimeout_seconds = iReadTimeout_seconds;
	}
	public long getBytesRead(){ return nBytesRead; }
	public int available() throws IOException {
//		System.out.print("sent available: " + mBuffer.remaining());
		return mBuffer.remaining();
	}
	public void close() throws IOException {
		if( m_socket_channel != null ) m_socket_channel.close();
	}
	public int read() throws IOException {
		if( mActivity != null && mActivity.isCancelled() ){
			throw new IOException("operation cancelled");
		}
		if( mBuffer.remaining() > 0 ){
			nBytesRead++;
			byte bValue = mBuffer.get();
			int iValue = (int)bValue & 0xFF;
//			if( iValue < 20 ){
//				System.out.print("." + iValue);
//			} else {
//				System.out.print("." + (char)iValue);
//			}
			return iValue;
		}
		if( mzEndOfStream ) return -1;
		read_channel();
		if( mBuffer.remaining() > 0 ){
		    nBytesRead++;
			byte bValue = mBuffer.get();
			int iValue = (int)bValue & 0xFF;
//			if( iValue < 20 ){
//				System.out.print("," + iValue);
//			} else {
//				System.out.print("," + (char)iValue);
//			}
		    return iValue;
		}
		if( mzEndOfStream ) return -1;
		throw new IOException("channel read returned no data");
	}
/*
	public int read(byte[] buffer) throws IOException {
		System.out.print("*r2*");
		int iCapacity = buffer.length;
		int iRemaining = mBuffer.remaining();
		if( iRemaining > 0 ){
			nBytesRead++;
			int iByteValue = (int)mBuffer.get();
			System.out.print("." + iByteValue);
			return iByteValue;
		}
		if( mzEndOfStream ) return -1;
		read_channel();
		if( mBuffer.remaining() > 0 ){
		    nBytesRead++;
			int iByteValue = (int)mBuffer.get();
			System.out.print("," + iByteValue);
		    return iByteValue;
		}
		if( mzEndOfStream ) return -1;
		throw new IOException("channel read returned no data");
	}
	public int read(byte[] buffer, int offset, int count) throws IOException {
		System.out.print("*r3*");
		return -1;
	}
*/
	public long skip( long count ) throws IOException {
		if( mActivity != null && mActivity.isCancelled() ){
			throw new IOException("operation cancelled");
		}
		System.out.print("skip " + count);
		if( mBuffer.remaining() >= count ){
			mBuffer.position(mBuffer.position() + (int)count);
			return count;
		}
		while( true ){
			int ctBytesSkipped = mBuffer.remaining();
			count -= ctBytesSkipped;
			read_channel();
			if( mzEndOfStream ) return ctBytesSkipped;
			if( mBuffer.remaining() >= count ){
				mBuffer.position(mBuffer.position() + (int)count);
				return count;
			}
		}
	}

	private long mnLastByteCountReport = System.currentTimeMillis();
	private void read_channel() throws IOException {
		mBuffer.clear(); // put the mBuffer in fill state
		long nReadStarted = System.currentTimeMillis();
ReadLoop:
		while( true ){
			if( mActivity != null && mActivity.isCancelled() ){
				throw new IOException("operation cancelled");
			}
			int ctBytesRead = m_socket_channel.read(mBuffer);
			switch( ctBytesRead ){
				case -1: // end of stream
					try { close(); } catch( Throwable t_is ){}
					mBuffer.limit(0); // zero mBuffer
					mzEndOfStream = true;
					if( mByteCounter != null ) mByteCounter.vReportByteCount_Total( nBytesRead );
					break ReadLoop;
				case 0: // nothing read
					try {
						Thread.sleep(TIMEOUT_CHECK_INTERVAL_ms);
					} catch( java.lang.InterruptedException ex ) {}
					if( (System.currentTimeMillis() - nReadStarted) > (miReadTimeout_seconds * 1000) ){
						try { close(); } catch( Throwable t_is ){}
						mBuffer.limit(0); // zero mBuffer
						if( mByteCounter != null ) mByteCounter.vReportByteCount_Total( nBytesRead );
						throw new IOException("timeout (" + miReadTimeout_seconds + " seconds) waiting to read after " + nBytesRead + " bytes read (see help topic Timeouts)");
					}
					break;
				default: // read some bytes
					if( mByteCounter != null )
						if( System.currentTimeMillis() - mnLastByteCountReport > 1000 ){
						    mByteCounter.vReportByteCount_EverySecond(nBytesRead);
							mnLastByteCountReport = System.currentTimeMillis();
						}
					mBuffer.flip(); // put mBuffer in drain state
					break ReadLoop;
			}
		}
	}
}

// why are we not extending ByteTracker here?
//class ByteTracker_Chunked extends InputStream {
class ByteTracker_Chunked extends ByteTracker {
	private final static boolean zALLOW_BLOCKING = false; // no blocking
	InputStream_Chunked mIS_chunked;
	ByteTracker_Chunked( Activity activity, SocketChannel socket_channel, int buffer_size, int iReadTimeout_seconds, ByteCounter bc ){
		super( activity, socket_channel, buffer_size, iReadTimeout_seconds, bc );
		try {
			mIS_chunked = new InputStream_Chunked( this, zALLOW_BLOCKING );
		} catch(Exception ex) {}
	}
	public int available() throws IOException { return mIS_chunked.available(); }
	public void close() throws IOException { mIS_chunked.close(); }
	public void mark( int readlimit ){ mIS_chunked.mark( readlimit ); }
	public boolean markSupported(){ return mIS_chunked.markSupported(); }
	public int read() throws IOException { return mIS_chunked.read(); }
	public int read( byte[] abBuffer ) throws IOException { return mIS_chunked.read( abBuffer ); }
	public int ready( byte[] abBuffer, int offset, int length ) throws IOException { return mIS_chunked.read( abBuffer, offset, length ); }
	public long skip( long count ) throws IOException { return mIS_chunked.skip( count ); }
	public void reset() throws java.io.IOException { mIS_chunked.reset(); }
}

