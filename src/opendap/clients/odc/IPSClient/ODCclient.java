package opendap.clients.odc.IPSClient;

/**
 * Title:        OPeNDAP Data Connector Example Client
 * Description:  Talks to the ODC interprocess server on the local machine and retrieves URLs
 * Organization: University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      1.0
 */

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.io.*;
import java.awt.Container;
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.util.ArrayList;

import javax.swing.JFrame;

/** This class demonstrates a generalized way to contact and issue commands to the
 *  ODC's Interprocess Server, or indeed any telnet-compatible listener.
 *
 *  Note that it extends Component so that it can receive asynchronous message events
 *  when information is received from the IPS. In this way it does not block while waiting
 *  for a response.
 */

public class ODCclient implements IIPSReceiver {

	private final int DEFAULT_PORT = 31870;
	private int miPort = DEFAULT_PORT;
	private StringBuffer msbResponseBuffer = new StringBuffer(80);
	private final String FORMATTING_ResponseTermination = "/~/\n";

	private final static ODCclient singleton = new ODCclient();

	ConsoleConnection mConnection;

	public static void main(String[] args){
		StringBuffer sbError = new StringBuffer(80);
		String[] asURLs = ODCclient.getInstance().getSelectedURLs_blocking(sbError);

		// use the blocking call to get the URLs

		if( asURLs == null ){
			System.err.println("Failed to get selected URLs: " + sbError);
		} else {
			System.out.println("ODC returned " + (asURLs.length-3) + " URLs:");
			for(int xURL = 1; xURL < asURLs.length-2; xURL++){
				System.out.println(asURLs[xURL]);
			}
		}
	}

	public static ODCclient getInstance(){ return singleton; }

	public int getPort(){ return miPort; }
	public void setPort( int iNewPort ){ miPort = iNewPort; }

	public void vClearResponseBuffer(){ msbResponseBuffer.setLength(0); }
	StringBuffer getResponseBuffer(){ return msbResponseBuffer; }


	/** This is a function showing how to make a synchronous retrieval of the URLs.
	 *  In general polling like this is not a good way to contact a server but since
	 *  the communication is on the local machine there is no serious exposure.
	 *  A better way to get the URLs in a more integrated way is shown in next function.
	 *
	 *  If you receive null in return, an error occurred.
	 *
	 *  The approach to this polling mechanism is that a 3-second period where there
	 *  is no data at all sent indicates no connection, whereas if data has been
	 *  received it will wait up to 10 seconds for more data before timing out.
	 *
	 *  Note that the zeroeth element of the array will contain the command and the
	 *  last element will contain the response terminator.
	 *
	 */
	public String[] getSelectedURLs_blocking(StringBuffer sbError){
		try {
			if( !zConnect("127.0.0.1", miPort, sbError) ){
				sbError.insert(0, "failed to connect on port " + miPort + ": ");
				return null;
			}
			mConnection.vWriteLine("getSelectedURLs");
			long lngTimeBegin = System.currentTimeMillis();
			int lenLast = -1;
			while(true){
				try{ wait(200); } catch(Exception ex) {}
				int lenBuffer = msbResponseBuffer.length();
				long lngTimeCurrent = System.currentTimeMillis();
				if( lenBuffer == 0 ){
					if( (lngTimeCurrent - lngTimeBegin) > 3000 ){ // 3 second timeout
						sbError.append("no response from ODC");
						return null;
					}
				} else {
					if( lenBuffer == lenLast ){
						if( msbResponseBuffer.toString().endsWith(FORMATTING_ResponseTermination) ){
							break; // got a complete answer
						} else if( (lngTimeCurrent - lngTimeBegin) > 10000 ){ // 10 second timeout once data is received
							sbError.append("incomplete response from ODC after " + lenBuffer + " bytes");
							return null;
						}
					} else {
						lenLast = lenBuffer;
						lngTimeBegin = lngTimeCurrent;
					}
				}
			}
			String[] asURLs = split(msbResponseBuffer.toString(),'\n');
			this.vClearResponseBuffer();
			return asURLs;
		} catch(Exception ex) {
			sbError.insert(0, "unexpected error");
			return null;
		} finally {
			if( !zClose(sbError) ){
				System.err.println("Failed to close ODC connection: " + sbError);
			}
		}
	}

        public static String[] split( String s, char c ){
                if( s == null ) return null;
                int len = s.length();
                int ctChar = 0;
                for(int pos = 0; pos < len; pos++){
                        if( s.charAt(pos) == c ) ctChar++;
                }
                int ctSegments = ctChar + 1;
                String[] asSegments = new String[ctSegments];
                if( ctChar == 0 ){ // minimal case
                        asSegments[0] = s;
                        return asSegments;
                }
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

	boolean getSelectedURLs(StringBuffer sbError){
		sbError.append("not implemented yet");
		return false;
	}

	boolean zConnect(String sHostAddr, int iPort, StringBuffer sbError) {
		InetAddress theAddress;
		try {
			theAddress = InetAddress.getByName(sHostAddr);
		} catch(Exception ex) {
			sbError.insert(0, "Failed to determine address of " + "host " + sHostAddr + ": ");
			return false;
		}
		return zConnect(theAddress, iPort, sbError);
	}

	boolean zConnect(InetAddress addressServer, int iPort, StringBuffer sbError) {
		Socket socketClient;
		if (mConnection != null) { // terminate existing con
			if (!mConnection.zTerminate(sbError)) {
				sbError.insert(0, "Unable to terminate existing connection: ");
				return false;
			}
		} try {
			socketClient = new Socket(addressServer, iPort);
		} catch(Exception ex) {
			sbError.insert(0, "Failed to create client socket for address " + addressServer + ':' + iPort + ": " + ex);
			return false;
		}
		mConnection = new ConsoleConnection(socketClient, this);
		return true;
	}

	/** If the error buffer is populated and the function returns true, treat
	 *  the contents of the error buffer as a status message.
	 */

	boolean zClose(StringBuffer sbError){
		if (mConnection == null) {
			sbError.append("no connection currently exists");
			return true;
		} else {
			if (mConnection.zTerminate(sbError)) {
				sbError.append("no longer connected to " + mConnection.getName());
				return true;
			} else {
				sbError.insert(0, "Unable to properly terminate existing connection: ");
				return false;
			}
		}
	}

	public void postMessage (String sMessage) {
		msbResponseBuffer.append(sMessage);
		return;
	}

    String msMatlabError;
    String[] masURLs;
    public void matlab_vLoadURLs() {
	msMatlabError = null;
	StringBuffer sbError = new StringBuffer(80);

	// Use ODCclient blocking call to get URLs
	masURLs = ODCclient.getInstance().getSelectedURLs_blocking(sbError);

	if ( masURLs == null ) {
	    msMatlabError = "Failed to get selected URLs: " + sbError;
	}
    }

    public boolean matlab_hasError() {
	return ( msMatlabError != null );
    }
    public String matlab_getError() {
	return ( msMatlabError == null ? "[no error]" : msMatlabError );
    }
    public int matlab_getURLCount() {
	if ( masURLs == null ) return 0;
	else {
	    for (int pos = 0; pos < masURLs.length; pos++) {
		if ( masURLs[pos].endsWith("/~/")) return pos-1;
	    }
	    return masURLs.length;
	}
    }
    public String matlab_getURL( int idx ) {
	return masURLs[idx];
    }
}

interface IIPSReceiver {
	void postMessage( String sMessage );
}

class ConsoleConnection extends Thread {

	private Socket msocketClient;
	private BufferedReader breader = null;
	private BufferedWriter bwriter = null;
	private IIPSReceiver moEventSink;
	private boolean mbBreakConnection = false; // term flag
	private int mLineCount = 0;
	private final int THREAD_RELIEF_INTERVAL = 100;
	public ConsoleConnection(Socket theClientSocket, IIPSReceiver oEventSink) {
		super();
		this.setDaemon(true);
		this.msocketClient = theClientSocket;
		this.moEventSink = oEventSink;
		try {
			breader = new BufferedReader(new InputStreamReader(theClientSocket.getInputStream()));
			bwriter = new BufferedWriter(new OutputStreamWriter(theClientSocket.getOutputStream()));
			this.start();
		} catch(Exception ex) {
			System.err.println("Unable to set up streams: " + ex);
		}
	}
	public void run() {
		int iNextThreadRelief = THREAD_RELIEF_INTERVAL;
		while(true) {
			try {
				if (mbBreakConnection) break;
				String sIncomingMessage = breader.readLine();

				// the following line is protection against
				// keepalive connections which send a steady stream
				// of nulls
				if (sIncomingMessage == null) continue;

				mLineCount++;
				this.moEventSink.postMessage(sIncomingMessage + "\n");
				this.yield();
				if (mLineCount == iNextThreadRelief) {
					this.sleep(500);
					iNextThreadRelief += THREAD_RELIEF_INTERVAL;
				}
			} catch(SocketException ex) {
				break;
			} catch(Exception ex) {
				System.err.println("Error: " + ex);
			}
		}
	}
	void vWriteLine(String sLine) {
		if (bwriter != null) {
			try {
				bwriter.write(sLine);
				bwriter.newLine();
				bwriter.flush();
			} catch(Exception ex) {
				System.err.println("Failed to write line: " + ex);
			}
		}
	}
	String getStatus() {
		if (msocketClient == null) {
			return "no connection";
		} else {
			return "listening to: " + msocketClient.getInetAddress();
		}
	}
	boolean zTerminate(StringBuffer sbError) {
		try {
			msocketClient.close();
			mbBreakConnection = true;
			return true;
		} catch(Exception ex) {
			sbError.append("Error closing socket: " + ex);
			return false;
		}
	}
}





