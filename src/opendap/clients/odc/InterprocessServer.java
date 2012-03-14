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

package opendap.clients.odc;

import java.util.ArrayList;
import java.util.Iterator;
import java.net.*;
import java.io.*;
import java.util.regex.*;

import opendap.clients.odc.data.Model_Dataset;
import opendap.clients.odc.data.Model_Retrieve;
import opendap.clients.odc.data.Model_URLList;
import opendap.clients.odc.data.OutputProfile;

public class InterprocessServer extends Thread {

    public InterprocessServer() {
		this.setDaemon(true);
	}

	private ServerSocket msocketServer = null;
	private int miPort;
	private ArrayList<CommandListener> mlistClients = new ArrayList<CommandListener>();

	boolean zInitialize( StringBuffer sbError ){
		miPort = ConfigurationManager.getInstance().getProperty_InterprocessServerPort();
		return true;
	}

	public void run() {
		Socket socketClient = null;
		try {
			msocketServer = new ServerSocket(miPort);
			if(msocketServer == null) {
				ApplicationController.vShowError("Failed to create server socket on port " + miPort + ". This port may be in use (on PCs use netstat -a to find out).");
				return;
			}
		} catch(Exception ex) {
			ApplicationController.vShowErrorDialog("Warning! You may have another instance of the Data Connector running on your machine. (Failed to open server socket, see errors).");
			ApplicationController.vShowError("While creating server socket on port " + miPort + ": " + ex);
			return;
		}
		ApplicationController.vShowStatus("Interprocess server listening on port " + miPort);
		while(true){
			try {
				socketClient = msocketServer.accept();
//				InetAddress inetClientAddress = socketClient.getInetAddress();
			} catch(Exception ex) {
				ApplicationController.vShowError("Failed on attempt to accept command server connection: " + ex);
				return;
			}
			try {
				StringBuffer sbError = new StringBuffer(250);
				final CommandListener theNewListener = new CommandListener();
				if( !theNewListener.zInitialize_Remote(this, socketClient, sbError) ){
					ApplicationController.vShowError("Error while initializing a command listener: " + sbError);
					socketClient.close();
					return;
				}
				theNewListener.start();
				ApplicationController.vShowStatus("Accepted connection from " +  socketClient.getInetAddress().getHostAddress());
			} catch(Exception ex) {
				ApplicationController.vUnexpectedError(ex, new StringBuffer("Unexpected error while creating command listener"));
			}
		}
	}

	protected void vTerminateClient( CommandListener theClient ){
		try { mlistClients.remove( theClient ); } catch(Exception ex) {}
	}

	// release external resources (the listener socket)
	protected void finalize() {
		if(msocketServer != null){
			try {
				msocketServer.close();
			} catch(Exception ex) {
				ApplicationController.vShowError("Failed to close command server socket.");
			}
			msocketServer = null;
		}
	}

	void writeLineToClients( String sLineText ){
		Iterator<CommandListener> iteratorClients = mlistClients.iterator();
		if( iteratorClients != null ){
			while( iteratorClients.hasNext() ){
				CommandListener oClient = iteratorClients.next();
				if( oClient != null ){
					try {
						oClient.writeLine(sLineText);
					} catch(Exception ex) {}
				}
			}
		}
	}

	public static String getResponseTermination(){ return CommandListener.FORMATTING_ResponseTermination; } 
	
}

// todo kill connection after timeout has elapsed
// this class can be used in two modes: remote and local
class CommandListener extends Thread {
	final static String FORMATTING_ResponseTermination = "/~/\n";
	private InterprocessServer mServer;
	private Socket mClientSocket;
	private OutputStream mos;
	private OutputStreamWriter mwriter;
	private BufferedWriter mbwriter;
	private String msLastCommand = null;
	public CommandListener() {
		super();
	}
	boolean zInitialize_Remote(InterprocessServer theServer, Socket theClientSocket, StringBuffer sbError){
		try {
			if( theServer == null ){
				sbError.append("internal error, server reference missing");
				return false;
			}
			if( theClientSocket == null ){
				sbError.append("internal error, client socket reference missing");
				return false;
			}
			this.mServer = theServer;
			this.mClientSocket = theClientSocket;
			if( !zInitialize_Local( mClientSocket.getOutputStream(), sbError ) ){
				return false;
			}
			return true;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}
	boolean zInitialize_Local(OutputStream os, StringBuffer sbError){
		try {
			vSetOutputStream( os );
			return true;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}
	public void run() {
		try {
			InputStreamReader reader = new InputStreamReader(mClientSocket.getInputStream());
			BufferedReader breader = new BufferedReader(reader);
			while(true){
				String sCommand = breader.readLine();
				if( sCommand.trim().length()==0 ) continue;
				vExecute(sCommand, null, FORMATTING_ResponseTermination);
			}
		} catch(java.net.SocketException exSocket) {  // socket closed
			try { mClientSocket.close(); } catch(Exception ex) {}
			mServer.vTerminateClient(this);
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, new StringBuffer("Failed to respond to command (connection may have been broken)"));
		}
	}

	void vSetOutputStream( OutputStream os ){
		this.mos = os;
		mwriter = new OutputStreamWriter(mos);
		mbwriter = new BufferedWriter(mwriter);
	}

	/** Executes an ODC-specific command. Regular commands go to the Python
	 *  interpreter. To execute an ODC command prefix it with an
	 *  exclamation point (!).
	 */
	void vExecute( String sCommand, OutputStream os, String sTerminator ) {
		try {
			if( sCommand == null ){
				ApplicationController.vShowWarning("internal error, null command supplied to execute");
				return;
			}
			ApplicationController.vLogActivity( "command: " + sCommand );
			if( os == null ){
				// use existing os
			} else {
				if( os == mos ){
					// already pointing to this os
				} else {
					vSetOutputStream( os );
				}
			}
			writeLine("\n   !command: " + sCommand);
			sCommand = sCommand.trim();
			String sCommandUpper = sCommand.toUpperCase();
			StringBuffer sbError = new StringBuffer(250);
			Model_Retrieve retrieve_model = ApplicationController.getInstance().getRetrieveModel();
			Model_URLList urllist = retrieve_model.getURLList();
			if( urllist == null ){
				String sError = "[Error: no url list]";
				writeLine(sError);
				ApplicationController.vShowError(sError);
			}
			if( sCommandUpper == null ){
				String sError = "[Error: command was null]";
				writeLine(sError);
				ApplicationController.vShowError(sError);
			} else if( sCommandUpper.length() == 0 ) {
				String sError = "[Error: command was blank]";
				writeLine(sError);
				ApplicationController.vShowError(sError);
			} else if( sCommandUpper.equals("?") || sCommandUpper.equals("HELP") ){
				writeLine(ApplicationController.getInstance().getAppName() + " version " + ApplicationController.getInstance().getAppVersion());
				writeLine("--- ODC Commands (case-insensitive, require leading \"!\") ---");
				writeLine("? or Help          - this information");
				writeLine("exit or quit       - end the application");
				writeLine("about              - about this application");
				writeLine("again              - repeat the previous command (up arrow also works)");
				writeLine("clear              - clears the command window");
				writeLine("reset              - creates a new Python interpreter");
				writeLine("show config        - displays current configuration settings");
				writeLine("show splash        - displays the splash screen (click it to close)");
				writeLine("show activities    - displays any active processes");
				writeLine("show memory        - prints memory information for system");
				writeLine("list scripts       - lists the files in the scripts directory");
				writeLine("gc                 - run garbage collector (compacts memory)");
				writeLine("set [option] [val] - sets the value of a configuration option");
				writeLine("dump tree          - prints tree if selected URL is a directory");
				writeLine("AddURL [url]       - adds an URL to the selection list");
				writeLine("GetIP              - returns the external IP address of the current machine");
				writeLine("GetSelectionCount  - returns the number of currently selected data sets");
				writeLine("GetAllURLs         - returns URLs for the selected datasets (gives directory urls)");
				writeLine("GetSelectedURLs    - returns selected URLs (directories are recursed)");
				writeLine("GetSelectedThumbs  - returns selected thumbnail URLs from plotter");
				writeLine("GetSelectedURLInfo - returns selected URLs (directories are recursed) plus URL type");
				writeLine("GetSelectedData    - returns ASCII data for selected URLs");
				writeLine("GetDataRecords     - returns ASCII data in records for selected URLs");
				writeLine("GetURL?Index=1     - gets the URL for the first dataset");
				//writeLine("GetURLInfo?Index=1 - gets the URL for the first dataset with full details");
				writeLine("GetData?Index=1    - gets the data for the first dataset");
				writeLine("GetInfo?Index=1    - gets the info for the first dataset");
				writeLine("setv [var] [val]   - sets the value of an environment variable");
				writeLine("eval [exp]         - evaluates an environment expression");
				writeLine("getall             - gets the values of all environment variables");
				writeLine("reload exps        - reloads the one-liner expression history");
			} else if( sCommandUpper.equals("ABOUT") ){
				writeLine(ApplicationController.getInstance().getAppName() + " version " + ApplicationController.getInstance().getAppVersion() + " " + ApplicationController.getInstance().getAppReleaseDate());
				writeLine("  OPeNDAP.org");
				writeLine("  Principal: Peter Cornillon");
				writeLine("  Developers: Daniel Holloway, John Chamberlain");
				writeLine("  Also includes components by Nathan Potter (Oregon State), Jake Hamby (NASA/JPL), and Jonathan Callahan (NOAA)");
				writeLine("");
				writeLine("  Description: " + ApplicationController.getInstance().getAppName() + " is a standalone client application that allows users to ");
				writeLine("  locate, query and retrieve datasets and other files served by OPeNDAP/DODS servers.");
				writeLine("  \nFor more information visit: http://OPeNDAP.org");
				writeLine("  \nFor software updates and tutorials: http://dodsdev.gso.uri.edu/ODC");
			} else if( sCommandUpper.equals("EXIT") || sCommandUpper.equals("QUIT") ){
				ApplicationController.vForceExit();
			} else if( sCommandUpper.startsWith("AGAIN") ){
				if( msLastCommand == null ){
					writeLine("[no existing command]");
				} else {
					sCommand = msLastCommand;
					vExecute( sCommand, os, sTerminator );
				}
			} else if( sCommandUpper.startsWith("CLEAR") ){
				ApplicationController.getInstance().getAppFrame().getPanel_Command().vClearDisplay();
			} else if( sCommandUpper.startsWith("RESET") ){
				if( ApplicationController.getInstance().getInterpreter().zCreateInterpreter( os, sbError ) ){
					writeLine("new interpreter created");
				} else {
					writeLine("failed to create new interpreter: " + sbError);
				}
			} else if( sCommandUpper.startsWith("SET") ){
				boolean zOption = !sCommandUpper.startsWith("SETV");
				int posOptionBegin = sCommand.indexOf(' ');
				if( posOptionBegin < 0 ){
					writeLine("no option specified (use 'show config' to see list)");
					return;
				}
				int posOptionEnd = sCommand.indexOf(' ', posOptionBegin + 1);
				String sOption;
				if( posOptionEnd < 0 ){
					sOption = sCommand.substring(posOptionBegin);
				} else {
					sOption = sCommand.substring(posOptionBegin, posOptionEnd);
				}
				sOption = sOption.trim();
				if( sOption.length() == 0 ){
					writeLine("no option/variable specified, must be space delimited (use 'show config' to see list)");
					return;
				}
				if( zOption ){
					if( !ConfigurationManager.getInstance().isOption( sOption ) ){
						writeLine("unknown option '" + sOption + "' (option name is case sensitive, use 'show config' to see list)");
						return;
					}
				}
				String sValue = sCommand.substring(posOptionEnd).trim();
				String sNewValue;
				if( zOption ){
					ConfigurationManager.getInstance().setOption( sOption, sValue);
	    			sNewValue = ConfigurationManager.getInstance().getOption( sOption );
				} else {
					ApplicationController.getInstance().set(sOption, sValue);
					sNewValue = ApplicationController.getInstance().eval(sOption);
				}
				writeLine(sOption + " = " + sNewValue);
				return;
			} else if( sCommandUpper.startsWith("EVAL ") ){
				String sExp = sCommand.substring(4).trim();
				writeLine(ApplicationController.getInstance().eval(sExp));
				return;
			} else if( sCommandUpper.startsWith("MATCH ") ){
				int xsp1 = sCommand.indexOf(" ");
				int xsp2 = sCommand.indexOf(" ", xsp1 + 1);
				if( xsp2 <= xsp1 ){
					writeLine("xsp invalid: " + xsp1 + " " + xsp2);
				}
				String sExp = sCommand.substring(xsp1, xsp2).trim();
				String sRegex = sCommand.substring(xsp2).trim();
				writeLine("Expression: " + sExp);
				writeLine("Regex: " + sRegex);
				String sText = ApplicationController.getInstance().eval(sExp);
				Pattern pattern = Pattern.compile(sRegex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
				Matcher matcher = pattern.matcher(sText);
				int ctMatches = 0;
				String sFirstMatch = "";
				if( matcher.find() ){
					ctMatches++;
					sFirstMatch = matcher.group();
				}
				while( matcher.find() ) ctMatches++;
				writeLine("Matches: " + ctMatches);
				if( ctMatches > 0 ){
					writeLine("First match: " + sFirstMatch);
				}
				return;
			} else if( sCommandUpper.startsWith("GETALL ") ){
				writeLine(ApplicationController.getInstance().getall());
				return;
			} else if( sCommandUpper.startsWith("RELOAD EXPS") ){
				ApplicationController.getInstance().getAppFrame().getVariableViewer().getExpressionHistory()._reload();
				return;
			} else if( sCommandUpper.equals("DUMP TREE") ){
				writeLine( retrieve_model.getDirectoryTreePrintout() );
			} else if( sCommandUpper.equals("SHOW CONFIG") ){
				writeLine(ConfigurationManager.getInstance().sDump());
			} else if( sCommandUpper.equals("SHOW SPLASH") ){
				ApplicationController.getInstance().vShowSplashScreen();
			} else if( sCommandUpper.equals("SHOW MEMORY") ){
				writeLine(ApplicationController.getInstance().sMemoryStatus());
			} else if( sCommandUpper.equals("LIST SCRIPTS") ){
				String sDirectoryPath = ConfigurationManager.getInstance().getDefault_DIR_Scripts();
				ArrayList<String> listFiles = Utility.listDirectoryContents( sDirectoryPath, sbError );
				if( listFiles == null ){
					writeLine( "Error identifying files in " + sDirectoryPath + ": " + sbError.toString() );
				} else {
					for( String s : listFiles ){
						writeLine( s );
					}
				}
			} else if( sCommandUpper.equals("GC") ){
				Runtime.getRuntime().gc();
				Thread.yield();
				writeLine("made system request to run garbage collector");
			} else if( sCommandUpper.equals("SHOW ACTIVITIES") ){
				ArrayList<Activity> listActivities = ApplicationController.getInstance().getActivities();
				if( listActivities.size() == 0 ){
					writeLine("[no activities running]");
				} else {
					Iterator<Activity> iter = listActivities.iterator();
					while(iter.hasNext()){
						Activity activityCurrent = (Activity)iter.next();
						String sMessage = activityCurrent.getMessage();
						writeLine(sMessage);
					}
				}
			} else if( sCommandUpper.startsWith("DUMPDAS")){
				Model_Dataset[] aURLs = urllist.getSelectedURLs(sbError);
				if( aURLs == null ){
					String sError = "[Error: failed to serve selected URLs: " + sbError + "]";
					writeLine(sError);
					ApplicationController.vShowError(sError);
				} else {
					for( int xURL = 0; xURL < aURLs.length; xURL++ ){
						Model_Dataset url = aURLs[xURL];
						opendap.dap.DAS das = url.getDAS();
						if( das == null ){
							writeLine("no DAS for: " + url.toString());
						} else {
							writeLine("DAS for: " + url.toString());
							writeLine(DAP.dumpDAS(das));
						}
					}
					ApplicationController.vShowStatus("" + aURLs.length + " selected URLs sent");
				}
			} else if( sCommandUpper.startsWith("ADDURL") ){
				String sURL = sCommandUpper.substring(6).trim();
				if( sURL.length() == 0 ){
					writeLine("no URL specified");
				} else {
					Model_Dataset model = null;
					String sType;
					if( sURL.endsWith("/") ){
						model = Model_Dataset.createDirectoryFromURL( sURL, sbError);
						if( model == null ){
							writeLine( "error creating directory URL model: " + sbError );
							return;
						}
						sType = "Directory";
					} else {
						model = Model_Dataset.createDataFromURL( sURL, sbError);
						if( model == null ){
							writeLine( "error creating data URL model: " + sbError );
							return;
						}
						sType = "Data";
					}
					Model_Dataset[] aURL = new Model_Dataset[1];
					aURL[0] = model;
					aURL[0].setTitle("[untitled]");
					ApplicationController.getInstance().getRetrieveModel().getURLList().vDatasets_Add(aURL);
					writeLine( sType + " URL added: " + aURL[0].toString() );
				}
			} else if( sCommandUpper.startsWith("GETSELECTIONCOUNT")){
				int iCount = urllist.getSelectedURLsCount();
				writeLine(Integer.toString(iCount));
			} else if( sCommandUpper.startsWith("GETSELECTEDURLS")){
				Model_Dataset[] aURLs = urllist.getSelectedURLs(sbError);
				if( aURLs == null ){
					String sError = "[Error: failed to serve selected URLs: " + sbError + "]";
					writeLine(sError);
					ApplicationController.vShowError(sError);
				} else {
					for( int xURL = 0; xURL < aURLs.length; xURL++ ){
						writeLine(aURLs[xURL].getFullURL());
					}
					ApplicationController.vShowStatus("" + aURLs.length + " selected URLs sent");
				}
				writeLine(FORMATTING_ResponseTermination);
			} else if( sCommandUpper.startsWith("GETSELECTEDTHUMBS")){
				Model_Dataset[] aURLs = ApplicationController.getInstance().getSelectedThumbs();
				if( aURLs == null ){
					String sError = "[Error: no thumbnail urls available]";
					writeLine(sError);
					ApplicationController.vShowError(sError);
				} else {
					for( int xURL = 0; xURL < aURLs.length; xURL++ ){
						writeLine(aURLs[xURL].getFullURL());
					}
					ApplicationController.vShowStatus("" + aURLs.length + " selected URLs sent");
				}
				writeLine(FORMATTING_ResponseTermination);
			} else if( sCommandUpper.startsWith("GETSELECTEDURLINFO")){
				Model_Dataset[] aURLs = urllist.getSelectedURLs(sbError);
				if( aURLs == null ){
					String sError = "[Error: failed to serve selected URLs: " + sbError + "]";
					writeLine(sError);
					ApplicationController.vShowError(sError);
				} else {
					for( int xURL = 0; xURL < aURLs.length; xURL++ ){
						writeLine(aURLs[xURL].getFullURL() + "  " + aURLs[xURL].getTypeString());
					}
					ApplicationController.vShowStatus("" + aURLs.length + " selected URLs sent");
				}
			} else if( sCommandUpper.startsWith("GETALLURLS")){
				Model_Dataset[] aURLs = urllist.getAllURLs();
				if( aURLs == null ){
					String sError = "[Error: failed to get all URLs: " + sbError + "]";
					writeLine(sError);
					ApplicationController.vShowError(sError);
				} else {
					for( int xURL = 0; xURL < aURLs.length; xURL++ ){
						writeLine(aURLs[xURL].getFullURL());
					}
					ApplicationController.vShowStatus("All " + aURLs.length + " top-level URLs sent");
				}
				writeLine(FORMATTING_ResponseTermination);
			} else if( sCommandUpper.startsWith("GETSELECTEDDATA")){
				Model_Dataset[] aURLs = urllist.getSelectedURLs(sbError);
				if( aURLs == null ){
					String sError = "[Error: failed to get selected data: " + sbError + "]";
					writeLine(sError);
					ApplicationController.vShowError(sError);
				} else {
					OutputProfile op = new OutputProfile( aURLs, mos, OutputProfile.FORMAT_Data_ASCII_text, null);
					if( !ApplicationController.getInstance().getOutputEngine().zOutputProfile(null, null, op, sbError) ){
						String sError = "[Error: failed to output selected data: " + sbError + "]";
						writeLine(sError);
						ApplicationController.vShowError(sError);
					} else {
						ApplicationController.vShowStatus("" + aURLs.length + " data set" + (aURLs.length==1?"":"s") + " sent");
					}
				}
			} else if( sCommandUpper.startsWith("GETDATARECORDS")){
				Model_Dataset[] aURLs = urllist.getSelectedURLs(sbError);
				if( aURLs == null ){
					String sError = "[Error: failed to get selected data: " + sbError + "]";
					writeLine(sError);
					ApplicationController.vShowError(sError);
				} else {
					OutputProfile op = new OutputProfile( aURLs, mos, OutputProfile.FORMAT_Data_ASCII_records, FORMATTING_ResponseTermination);
					if( !ApplicationController.getInstance().getOutputEngine().zOutputProfile(null, null, op, sbError) ){
						String sError = "[Error: failed to output selected data: " + sbError + "]";
						writeLine(sError);
						ApplicationController.vShowError(sError);
					} else {
						ApplicationController.vShowStatus("" + aURLs.length + " data records" + (aURLs.length==1?"":"s") + " sent");
					}
				}
			} else if( sCommandUpper.startsWith("GETALLDATA")){
				Model_Dataset[] aURLs = urllist.getAllURLs();
				if( aURLs == null ){
					String sError = "[Error: failed to get all data: " + sbError + "]";
					writeLine(sError);
					ApplicationController.vShowError(sError);
				} else {
					OutputProfile op = new OutputProfile( aURLs, mos, OutputProfile.FORMAT_Data_ASCII_text, null);
					if( !ApplicationController.getInstance().getOutputEngine().zOutputProfile(null, null, op, sbError) ){
						String sError = "[Error: failed to output all selected data: " + sbError + "]";
						writeLine(sError);
						ApplicationController.vShowError(sError);
					} else {
						ApplicationController.vShowStatus("" + aURLs.length + " all data sets" + (aURLs.length==1?"":"s") + " sent");
					}
				}
			} else if( sCommandUpper.startsWith("GETURL") || sCommandUpper.startsWith("GETDATA") || sCommandUpper.startsWith("GETINFO") ){
				Model_Dataset[] aURLs = urllist.getAllURLs();
				Model_Dataset[] aURLsToOutput;
				if( aURLs == null ){
					String sError = "[Error: failed to get URLs for " + sCommand + ": " + sbError + "]";
					writeLine(sError);
					ApplicationController.vShowError(sError);
				} else {
					int posParamDelimiter = sCommand.indexOf("?");
					int iURLIndex = -1;
					Model_Dataset url = null;
					if( posParamDelimiter == -1 ){
						url = urllist.getSelectedURL();
					} else {
						String sParam = sCommand.substring(posParamDelimiter);
						String[] asParams = Utility_String.split(sParam, '=');
						if( asParams.length != 2 ){
							String sError = "[Error: url selection parameter has invalid format: " + sParam + "]";
							writeLine(sError);
							return;
						} else {
							String sURLIndex = asParams[1];
							try {
								iURLIndex = Integer.parseInt(sURLIndex);
							} catch(Exception ex) {
								String sError = "[Error: url index not an integer: " + sURLIndex + "]";
								writeLine(sError);
								return;
							}
							if( iURLIndex < 1 || iURLIndex > aURLs.length ){
								String sError = "[Error: url selection index (" + iURLIndex + ") is out of range (1 - " + (aURLs.length) + ")]";
								writeLine(sError);
								return;
							}
							if( sCommandUpper.startsWith("GETDATA") ){
								aURLsToOutput = urllist.getSelectedURLs( sbError );
								if( aURLsToOutput == null ){
									String sError = "[Error: failed to get sub selected URLs: " + sbError + "]";
									writeLine(sError);
									ApplicationController.vShowError(sError);
									return;
								}
							} else {
								aURLsToOutput = new Model_Dataset[1];
								aURLsToOutput[0] = aURLs[iURLIndex-1];
							}
						}
						if( aURLsToOutput == null ){
							writeLine("internal error; no url resulted");
						} else {
							if( sCommandUpper.startsWith("GETURL") ){
								writeLine(aURLsToOutput[0].getFullURL());
								ApplicationController.vShowStatus("URL " + iURLIndex + " sent");
							} else if( sCommandUpper.startsWith("GETINFO") ){
								writeLine(aURLsToOutput[0].getInfo());
								ApplicationController.vShowStatus("Info " + iURLIndex + " sent");
							} else if( sCommandUpper.startsWith("GETDATA") ){
								OutputProfile op = new OutputProfile( aURLsToOutput, mos, OutputProfile.FORMAT_Data_ASCII_text, null );
								if( !ApplicationController.getInstance().getOutputEngine().zOutputProfile(null, null, op, sbError) ){
									String sError = "[Error: failed to get data set #" + iURLIndex + " of selection: " + sbError + "]";
									writeLine(sError);
									ApplicationController.vShowError(sError);
								} else {
									ApplicationController.vShowStatus("data set #" + iURLIndex + " of selection sent");
								}
							} else {
								ApplicationController.vShowStatus("unknown command: " + sCommand);
							}
						}
					}
				}
 			} else if( sCommandUpper.startsWith("MESSAGE ") ){
				 String sCount = sCommand.substring(8).trim();
				 try {
					 int ct = Integer.parseInt(sCount);
					 StringBuffer sb = new StringBuffer(1000);
					 String sPhrase = "" + sCount + "words ";
					 for(int xphrase = 0; xphrase < ct; xphrase++ ){
						 sb.append(sPhrase);
					 }
					 opendap.clients.odc.gui.Message.show( sb.toString() );
				 } catch(Exception ex) {}
 			} else if( sCommandUpper.startsWith("MATCH ") ){
 				int xsp1 = sCommand.indexOf(" ");
 				int xsp2 = sCommand.indexOf(" ", xsp1 + 1);
 				if( xsp2 <= xsp1 ){
 					writeLine("xsp invalid: " + xsp1 + " " + xsp2);
 				}
 				String sExp = sCommand.substring(xsp1, xsp2).trim();
 				String sRegex = sCommand.substring(xsp2).trim();
 				writeLine("Expression: " + sExp);
 				writeLine("Regex: " + sRegex);
 				String sText = ApplicationController.getInstance().eval(sExp);
 				Pattern pattern = Pattern.compile(sRegex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
 				Matcher matcher = pattern.matcher(sText);
 				int ctMatches = 0;
 				String sFirstMatch = "";
 				if( matcher.find() ){
 					ctMatches++;
 					sFirstMatch = matcher.group();
 				}
 				while( matcher.find() ) ctMatches++;
 				writeLine("Matches: " + ctMatches);
 				if( ctMatches > 0 ){
 					writeLine("First match: " + sFirstMatch);
 				}
 				return;
			} else if( sCommandUpper.startsWith("TABLESET") ){ // test routine
				String[] args = Utility_String.splitCommaWhiteSpace(sCommand);
				int iRows = Integer.parseInt(args[1]);
				int iCols = Integer.parseInt(args[2]);
				int[][] data = new int[iRows][iCols];
				int iValue = 0;
				for( int xRow = 0; xRow < iRows; xRow++ ){
				    for( int xCol = 0; xCol < iCols; xCol++ ){
						data[xRow][xCol] = ++iValue;
					}
				}
				ApplicationController.getInstance().getAppFrame().getTableViewer().vSetData(data);
			} else if( sCommandUpper.startsWith("TESTLAYOUT") ){ // test routine
				opendap.clients.odc.plot.Panel_View_Plot.vTestLayout();
				writeLine("layout tester launched");
			} else if( sCommandUpper.startsWith( "GETIP" ) ){
				String sURL = "http://ttools.kattare.com/IPLookup/servlet/iplookup.IPLookupServlet3";
				String sIP = IO.getStaticContent( sURL, null, null, sbError );
				if( sbError.length() != 0 ) {
					writeLine(sbError.toString());
				} else {
					writeLine(sIP);
				}
			} else {
				String sError = "[Error: unknown command: " + sCommand + "]";
				writeLine(sError);
				ApplicationController.vShowError(sError);
			}
			if( sTerminator != null ) mbwriter.write(sTerminator); // command reponse terminator
			mbwriter.flush();
			msLastCommand = sCommand;
		} catch(Exception ex) {
			StringBuffer sbUnexpectedError = new StringBuffer("Failed to execute command: ");
			ApplicationController.vUnexpectedError(ex, sbUnexpectedError);
			try {
				writeLine(sbUnexpectedError.toString());
			} catch(Exception e) {}
		}
	}

	void writeLine(String sLineText) throws java.io.IOException {
		mbwriter.write(sLineText);
		mbwriter.newLine();
		mbwriter.flush();
	}
}




