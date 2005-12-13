package opendap.clients.odc;

/**
 * Title:        Application Controller
 * Description:  Top-level controller for starting and managing the application
 * Copyright:    Copyright (c) 2002-2004
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.59
 */

import java.io.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class ApplicationController {

	public final static boolean DEBUG = true;

    private static final ApplicationController thisSingleton = new ApplicationController();

    private static final String msAppName = "OPeNDAP Data Connector";
    private static final String msAppVersion = "2.59";
    private static final String msAppReleaseDate = "12 December 2005"; // todo create ANT substitution
	private static final long SPLASH_SCREEN_DELAY_MS = 0; // 1800; // 1.8 seconds

	String getAppName(){ return msAppName; }
	String getAppVersion(){ return msAppVersion; }
	String getAppReleaseDate(){ return msAppReleaseDate; }
	String getVersionString(){ return "ODC " + msAppVersion; }

	private ApplicationFrame appframe;
	private InterprocessServer server;
	private CommandListener command;

	private OutputEngine mOutputEngine;
	public OutputEngine getOutputEngine(){ return mOutputEngine; }

	private Model_Retrieve mRetrieve;
	public Model_Retrieve getRetrieveModel(){ return mRetrieve; }

	private ArrayList listStatusMessages = new ArrayList();
	private ArrayList listWarnings = new ArrayList();
	private ArrayList listErrors = new ArrayList();
	private ArrayList mlistActivities = new ArrayList();

	private Environment mEnvironment = new Environment();

    public static void main(String[] args){
        try {

			ApplicationController thisInstance = ApplicationController.getInstance();

			StringBuffer sbError = new StringBuffer(256);

			// get configuration manager going
			String sBaseDirectory = null;
			if( args != null ){
				if( args.length > 0 ){
					sBaseDirectory = args[0];
				}
			}
			if( !ConfigurationManager.zInitializeConfigurationManager(sBaseDirectory, sbError) ){
				ApplicationController.vShowStartupDialog("Configuration failure: " + sbError);
				System.out.println("Configuration failure: " + sbError);
				System.exit(1); // todo not really a good idea but don't want to leave process hanging and not easily endable by user
			}

			boolean zShowSplashScreen = ConfigurationManager.getInstance().getProperty_DISPLAY_ShowSplashScreen();
			if( zShowSplashScreen ) thisInstance.vShowSplashScreen();
			thisInstance.vShowStartupMessage("preloading classes");
			thisInstance.vClassPreLoad();
			thisInstance.vShowStartupMessage("determining available fonts");
			Utility.getFontFamilies();
			thisInstance.vShowStartupMessage("loading icons");
			if( !Utility.zLoadIcons(sbError) ){
				ApplicationController.vShowStartupDialog("Failed to start: " + sbError);
				System.out.println("Failed to start: " + sbError);
				System.exit(1); // todo not really a good idea but don't want to leave process hanging and not easily endable by user
			}
			thisInstance.vShowStartupMessage("creating main frame");
			thisInstance.appframe = new ApplicationFrame();
			if( ConfigurationManager.getInstance().getProperty_InterprocessServerOn() )
				thisInstance.vInterprocessServer_Start();
			vSetLookAndFeel();
			vSetSounds();
			thisInstance.vShowStartupMessage("creating models");
			thisInstance.mOutputEngine = new OutputEngine();
			thisInstance.mRetrieve     = new Model_Retrieve();
			if( !thisInstance.appframe.zInitialize( msAppName, thisSingleton, sbError ) ){
				ApplicationController.vShowStartupDialog("Failed to initialize main window: " + sbError);
				System.out.println("Failed to initialize main window: " + sbError);
				System.exit(1); // todo not really a good idea but don't want to leave process hanging and not easily endable by user
			}
			ApplicationController.getInstance().vShowStartupMessage("setting options");
			vSetOptions();
			if( zShowSplashScreen ){
				java.util.Timer timerStartup = new java.util.Timer();
				StartupMainFrame startup = new StartupMainFrame();
				timerStartup.schedule(startup, SPLASH_SCREEN_DELAY_MS);
			} else {
				thisInstance.vActivate();
			}
        } catch(Exception ex){
			String sStackTrace = ApplicationController.extractStackTrace(ex);
            System.out.println("Unexpected error starting application: " + sStackTrace);
			ApplicationController.vShowStartupDialog("Failed to initialize: " + sStackTrace);
            System.exit(1);
        }
    }

	void vActivate(){
		thisSingleton.appframe.vActivate();
		vSetFrameBounds();
	}

	/* loads heavy packages like xerxes up front */
	void vClassPreLoad(){
		try {
			javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
		} catch(Exception ex) {}

	}

	static class StartupMainFrame extends java.util.TimerTask {
		public final void run(){
			if( windowSplash != null ) windowSplash.dispose();
			thisSingleton.vActivate();
		}
	}

	static void vSetLookAndFeel(){
		try {
			String sLF = javax.swing.UIManager.getSystemLookAndFeelClassName();
			javax.swing.UIManager.setLookAndFeel(sLF);
			vShowStatus("Set look and feel to platform: " + sLF);
		} catch(Exception ex) {
			vShowWarning("Failed to set look and feel: " + ex);
		}
	}

	static void vSetOptions(){

		// establish proxy
		String sProxyHost = ConfigurationManager.getInstance().getProperty_ProxyHost();
		String sProxyPort = ConfigurationManager.getInstance().getProperty_ProxyPort();
		if( sProxyHost == null ) sProxyHost = "";
		if( sProxyPort == null ) sProxyPort = "";
		Properties prop = System.getProperties();
        if( sProxyHost.length() == 0 ) prop.remove("http.proxyHost"); else prop.put("http.proxyHost", sProxyHost);
        if( sProxyPort.length() == 0 ) prop.remove("http.proxyPort"); else prop.put("http.proxyPort", sProxyPort);

	}

	private static File fileBeep = null;
	private static void vSetSounds(){
		try {
			String sBeepPath = "sounds/BEEPDROP.WAV";
			fileBeep = new File(sBeepPath);
		} catch(Exception ex) {
		}
	}

	private static int mFrame_ExtraWidth = 0;
	private static int mFrame_ExtraHeight = 0;
	private static void vSetFrameBounds(){
		Dimension dimContent = getInstance().getAppFrame().getContentPane().getSize();
		Dimension dimFrame   = getInstance().getAppFrame().getSize();
		mFrame_ExtraWidth = (int)(dimFrame.getWidth() - dimContent.getWidth());
		mFrame_ExtraHeight = (int)(dimFrame.getHeight() - dimContent.getHeight());
	}
	public int getFrame_ExtraWidth(){ return mFrame_ExtraWidth; }
	public int getFrame_ExtraHeight(){ return mFrame_ExtraHeight; }

	private static javax.swing.JWindow windowSplash = null;
	private static javax.swing.JLabel labelSplash = null;
	private static ImageIcon imageiconSplash;
	void vShowSplashScreen(){
		try {
			StringBuffer sbError = new StringBuffer();
			imageiconSplash = Utility.imageiconLoadResource(Resources.SplashScreen, sbError);
			if( imageiconSplash == null ){
				System.err.println("splash screen [" + Resources.SplashScreen + "] missing from classpath (" + sbError + ")");
				javax.swing.JOptionPane.showMessageDialog(null, "startup screen unavailable");
				return;
			}
			labelSplash = new javax.swing.JLabel(imageiconSplash);
			final javax.swing.JWindow windowSplash_final = new javax.swing.JWindow(); // todo try assigning an owner to avoid hidden window phenomenon
			windowSplash = windowSplash_final;
			windowSplash_final.getContentPane().add(labelSplash, java.awt.BorderLayout.CENTER);
			java.awt.Dimension dimScreenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
			final int iImageWidth = imageiconSplash.getIconWidth();
			final int iImageHeight = imageiconSplash.getIconHeight();
			windowSplash_final.pack();
			windowSplash_final.setLocation(dimScreenSize.width/2 - (iImageWidth/2), dimScreenSize.height/2 - (iImageHeight/2));
			windowSplash_final.addMouseListener(
				new MouseAdapter(){
					public void mousePressed( MouseEvent me ){
						windowSplash = null;
						if( windowSplash_final != null ) windowSplash_final.dispose();
					}
				}
			);
			windowSplash.setVisible(true);
		} catch(Exception ex) {
			windowSplash = null;
			if( windowSplash != null ) windowSplash.dispose();
			Utility.vUnexpectedError(ex, new StringBuffer("showing splash screen: "));
		}
	}

	final public static ApplicationController getInstance(){return thisSingleton;}

	final static Font fontSplashScreen = new Font(null, Font.PLAIN, 10);
	final public void vShowStartupMessage( final String sMessage ){
		if( windowSplash != null ){
			try {
				javax.swing.SwingUtilities.invokeAndWait(
					new Runnable() {
						public void run() {
							labelSplash.repaint();
						}
					}
				);
				javax.swing.SwingUtilities.invokeAndWait(
					new Runnable() {
						public void run() {
							if (sMessage != null) {
								java.awt.Graphics g = labelSplash.getGraphics();
								g.setColor(java.awt.Color.white);
								g.setFont(fontSplashScreen);
								g.setClip(0, 0, labelSplash.getWidth(), 30);
								g.drawString("Version " + msAppVersion, 10, 14);
								g.drawString(sMessage, 11, 28);
							}
						}
					}
				);
				Thread.yield();
			} catch( Exception ex ) {}
		}
	}

	public void vInterprocessServer_Start(){
		if( server == null ){
			server = new InterprocessServer();
			StringBuffer sbError = new StringBuffer(80);
			if( !server.zInitialize(sbError) ){
				ApplicationController.getInstance().vShowError("Failed to start interprocess server: " + sbError);
				return;
			}
		}
		server.start();
	}

	public void vInterprocessServer_Stop(){
		if( server == null ) return;
		server.interrupt();
	}

	public String eval(String s){
		return mEnvironment.eval(s);
	}

	public String getall(){
		return mEnvironment.getall();
	}

	public void set(String symbol, String value){
		mEnvironment.set(symbol, value);
	}

	public ApplicationFrame getAppFrame(){ return appframe; } // todo review this usage

	public ArrayList getActivities(){ return this.mlistActivities; }

	public void vCommand( String sCommand, OutputStream os ){
		if( command == null ){
			command = new CommandListener();
			if( os == null ){ os = this.getAppFrame().getTextViewerOS(); }
			if( os == null ){
				this.vShowError("internal error, text viewer's output stream unavailable");
				return;
			}
			StringBuffer sbError = new StringBuffer(80);
			if( !command.zInitialize_Local( os, sbError ) ){
				this.vShowError("Failed to initialize command listener: " + sbError);
				return ;
			}
		}
		command.vExecute( sCommand, os, null );
	}

	public int getStatusCount(){ return listStatusMessages.size(); }
	public int getWarningCount(){ return listWarnings.size(); }
	public int getErrorCount(){ return listErrors.size(); }

	public DodsURL[] getSelectedThumbs(){
		if( getAppFrame() == null ) return null;
		if( getAppFrame().getPlotter() == null ) return null;
		return getAppFrame().getPlotter().getPanel_Thumbnails().getSelectedURLs0();
	}

	public java.io.OutputStream getTextViewerOS(){
		return ApplicationController.getInstance().getAppFrame().getTextViewerOS();
	}

	public java.io.OutputStream getImageViewerOS( String sImageName, StringBuffer sbError ){
		return ApplicationController.getInstance().getAppFrame().getImageViewerOS(sImageName, sbError);
	}

	// do not use this / application should exit by quitting all non-daemon threads
	public static void vForceExit(){
		if( ConfigurationManager.getInstance().getProperty_LOGGING_ReportMetrics() ){
			vPersistLog();
		}
		System.exit(0);
	}

	public static void vShowHelp(){
		vShowErrorDialog("not implemented");
	}

	public static void vClearStatus(){
		ApplicationController.getInstance().getAppFrame().vClearStatus();
	}

	public void vClearHistory(){
		this.listErrors.clear();
		this.listWarnings.clear();
		this.listStatusMessages.clear();
	}

	/** outputs status without putting it in the messages collection
	 *  this should be used when many repetitive statuses are being generated
	 */
	public static void vShowStatus_NoCache( String sStatusMessage ){
		ApplicationController.getInstance().getAppFrame().vShowStatus(sStatusMessage);
	}

	public static void vShowStatus_NoCache_Append( String sStatusMessage ){
		ApplicationController.getInstance().getAppFrame().vAppendStatus(sStatusMessage);
	}

	public static void vShowStatus( String sStatusMessage ){
		Date dateNow = new Date();
		ApplicationController.getInstance().listStatusMessages.add( dateNow.toString() + " " + sStatusMessage );
		ApplicationFrame app_frame = ApplicationController.getInstance().getAppFrame();
		if( app_frame != null ) app_frame.vShowStatus(sStatusMessage);
		System.out.println(sStatusMessage);
	}

	public static void vLogActivity( String sStatusMessage ){
		Date dateNow = new Date();
		ApplicationController.getInstance().listStatusMessages.add( dateNow.toString() + " " + sStatusMessage);
	}

	public static void vPersistLog(){
		ApplicationController ac = ApplicationController.getInstance();
		StringBuffer sbLog = new StringBuffer(1000);
		Iterator iterator = ac.listErrors.iterator();
		while(iterator.hasNext()){
			String sMessage = (String)iterator.next() + "\n";
			sbLog.append(sMessage);
		}
		iterator = ac.listWarnings.iterator();
		while(iterator.hasNext()){
			String sMessage = (String)iterator.next() + "\n";
			sbLog.append(sMessage);
		}
		iterator = ac.listStatusMessages.iterator();
		while(iterator.hasNext()){
			String sMessage = (String)iterator.next() + "\n";
			sbLog.append(sMessage);
		}
	}

	public static boolean zErrorPopupActive = false;
	public static void vShowError( String sErrorMessage ){
		Date dateNow = new Date();
		ApplicationController.getInstance().listErrors.add( dateNow.toString() + " " + sErrorMessage );
		ApplicationFrame app_frame = ApplicationController.getInstance().getAppFrame();
		if( app_frame != null ) app_frame.vShowStatus(sErrorMessage);
		System.out.println(sErrorMessage);
		Utility.soundPlay(fileBeep);
		if( app_frame != null ){
			if( !zErrorPopupActive && ConfigurationManager.getInstance().getProperty_DISPLAY_ShowErrorPopups() ){
				try {
					zErrorPopupActive = true;
					app_frame.vShowAlert_Error(sErrorMessage);
				} finally {
					zErrorPopupActive = false;
				}
			}
		}
	}

	public static void vShowError_NoModal( String sErrorMessage ){
		Date dateNow = new Date();
		ApplicationController.getInstance().listErrors.add( dateNow.toString() + " " + sErrorMessage );
		ApplicationFrame app_frame = ApplicationController.getInstance().getAppFrame();
		if( app_frame != null ) app_frame.vShowStatus(sErrorMessage);
		System.out.println(sErrorMessage);
		Utility.soundPlay(fileBeep);
	}

	public static void vShowWarning( String sWarningMessage ){
		Date dateNow = new Date();
		ApplicationController.getInstance().listWarnings.add( dateNow.toString() + " " + sWarningMessage );
		ApplicationFrame app_frame = ApplicationController.getInstance().getAppFrame();
		if( app_frame != null ) app_frame.vShowStatus(sWarningMessage);
		System.out.println(sWarningMessage);
	}

	public static void vShowStartupDialog( String sErrorMessage ){
		String sWrappedMessage = Utility.sWrap(sErrorMessage, 60, false, "\n");
		javax.swing.JOptionPane.showMessageDialog(ApplicationController.getInstance().windowSplash, sWrappedMessage);
	}

	public static void vShowErrorDialog( String sErrorMessage ){
		ApplicationFrame frame = ApplicationController.getInstance().getAppFrame();
		if( frame == null ){
			javax.swing.JOptionPane.showMessageDialog(null, sErrorMessage);
		} else {
			frame.vShowAlert_Error(sErrorMessage);
		}
	}

	public String getLog(){
		StringBuffer sbLog = new StringBuffer(10000);
		sbLog.append("Errors:");
		Iterator iterator = listErrors.iterator();
		if( iterator.hasNext() ){
			sbLog.append("\n");
		} else {
			sbLog.append(" [none]\n");
		}
		while(iterator.hasNext()){
			String sMessage = (String)iterator.next() + "\n";
			sbLog.append(sMessage);
		}
		sbLog.append("\nWarnings:");
		iterator = listWarnings.iterator();
		if( iterator.hasNext() ){
			sbLog.append("\n");
		} else {
			sbLog.append(" [none]\n");
		}
		while(iterator.hasNext()){
			String sMessage = (String)iterator.next() + "\n";
			sbLog.append(sMessage);
		}
		sbLog.append("\nStatus:");
		iterator = listStatusMessages.iterator();
		if( iterator.hasNext() ){
			sbLog.append("\n");
		} else {
			sbLog.append(" [none]\n");
		}
		while(iterator.hasNext()){
			String sMessage = (String)iterator.next() + "\n";
			sbLog.append(sMessage);
		}
		return sbLog.toString();
	}

	public void vDumpMessages(){
		OutputStream osTextView = this.getInstance().getAppFrame().getTextViewerOS();
		String sHeader = "Errors:";
		Iterator iterator = listErrors.iterator();
		if( iterator.hasNext() ){
			sHeader += "\n";
		} else {
			sHeader += " [none]\n";
		}
		System.out.print(sHeader);
		try { if( osTextView != null ) osTextView.write(sHeader.getBytes()); } catch(Exception ex) {}
		while(iterator.hasNext()){
			String sMessage = (String)iterator.next() + "\n";
			System.out.print(sMessage);
			try { if( osTextView != null ) osTextView.write(sMessage.getBytes()); } catch(Exception ex) {}
		}
		sHeader = "Warnings:";
		iterator = listWarnings.iterator();
		if( iterator.hasNext() ){
			sHeader += "\n";
		} else {
			sHeader += " [none]\n";
		}
		System.out.print(sHeader);
		try { if( osTextView != null ) osTextView.write(sHeader.getBytes()); } catch(Exception ex) {}
		while(iterator.hasNext()){
			String sMessage = (String)iterator.next() + "\n";
			System.out.print(sMessage);
			try { if( osTextView != null ) osTextView.write(sMessage.getBytes()); } catch(Exception ex) {}
		}
		sHeader = "Status:";
		iterator = listStatusMessages.iterator();
		if( iterator.hasNext() ){
			sHeader += "\n";
		} else {
			sHeader += " [none]\n";
		}
		System.out.print(sHeader);
		try { if( osTextView != null ) osTextView.write(sHeader.getBytes()); } catch(Exception ex) {}
		while(iterator.hasNext()){
			String sMessage = (String)iterator.next() + "\n";
			System.out.print(sMessage);
			try { if( osTextView != null ) osTextView.write(sMessage.getBytes()); } catch(Exception ex) {}
		}
	}

	public void vDumpLog(){
		OutputStream osTextView = this.getInstance().getAppFrame().getTextViewerOS();
		String sHeader = "Status Log:";
		Iterator iterator = listStatusMessages.iterator();
		if( iterator.hasNext() ){
			sHeader += "\n";
		} else {
			sHeader += " [none]\n";
		}
		System.out.print(sHeader);
		try { if( osTextView != null ) osTextView.write(sHeader.getBytes()); } catch(Exception ex) {}
		while(iterator.hasNext()){
			String sMessage = (String)iterator.next() + "\n";
			System.out.print(sMessage);
			try { if( osTextView != null ) osTextView.write(sMessage.getBytes()); } catch(Exception ex) {}
		}
		vDumpErrors(); // dump errors also
	}

	public void vDumpErrors(){
		OutputStream osTextView = this.getInstance().getAppFrame().getTextViewerOS();
		String sHeader = "Errors:";
		Iterator iterator = listErrors.iterator();
		if( iterator.hasNext() ){
			sHeader += "\n";
		} else {
			sHeader += " [none]\n";
		}
		System.out.print(sHeader);
		try { if( osTextView != null ) osTextView.write(sHeader.getBytes()); } catch(Exception ex) {}
		while(iterator.hasNext()){
			String sMessage = (String)iterator.next() + "\n";
			System.out.print(sMessage);
			try { if( osTextView != null ) osTextView.write(sMessage.getBytes()); } catch(Exception ex) {}
		}
		sHeader = "Warnings:";
		iterator = listWarnings.iterator();
		if( iterator.hasNext() ){
			sHeader += "\n";
		} else {
			sHeader += " [none]\n";
		}
		System.out.print(sHeader);
		try { if( osTextView != null ) osTextView.write(sHeader.getBytes()); } catch(Exception ex) {}
		while(iterator.hasNext()){
			String sMessage = (String)iterator.next() + "\n";
			System.out.print(sMessage);
			try { if( osTextView != null ) osTextView.write(sMessage.getBytes()); } catch(Exception ex) {}
		}
	}

	static long lLastProfile = 0;
	public static void vProfile( String sTag ){
		long lNow = System.currentTimeMillis();
		System.out.println( sTag + ": " + (lNow - lLastProfile));
		lLastProfile = lNow;
	}

	/** Extracts the stack trace as a string from an exception */
	static String extractStackTrace(Throwable theException) {
		try {
			java.io.StringWriter sw = new java.io.StringWriter();
			theException.printStackTrace(new java.io.PrintWriter(sw));
			return sw.toString();
		} catch(Exception ex) {
			return "stack trace unavailable (" + ex.toString() + ")";
		}
	}

	opendap.clients.odc.GCMD.GCMDSearch mSearch_GCMD = null;
	opendap.clients.odc.ECHO.ECHOSearchWindow mSearch_ECHO = null;
	opendap.clients.odc.DatasetList.DatasetList mSearch_XML = null;
	opendap.clients.odc.THREDDS.THREDDSSearch mSearch_THREDDS = null;

	opendap.clients.odc.GCMD.GCMDSearch getGCMDSearch(){
		if( mSearch_GCMD == null ){
			String urlGCMD = ConfigurationManager.getInstance().getOption("url.GCMD");
			if( urlGCMD == null ){
				this.vShowError("Failed to initialize retrieve GCMD url from configuration file");
				return null;
			} else {
				mSearch_GCMD = new opendap.clients.odc.GCMD.GCMDSearch(urlGCMD);
			}
		}
		return mSearch_GCMD;
	}

	opendap.clients.odc.ECHO.ECHOSearchWindow getECHOSearch(){
		return null; // temporarily disabling
//		if( mSearch_ECHO == null ){
//			mSearch_ECHO = new opendap.clients.odc.ECHO.ECHOSearchWindow();
//		}
//		return mSearch_ECHO;
	}

	opendap.clients.odc.THREDDS.THREDDSSearch getTHREDDSSearch(){
		return null;
// implementation postponed
//		if( mSearch_THREDDS == null ){
//			String sTHREDDS_URL = "";
//			if( sTHREDDS_URL == null ){
//				this.vShowError("Failed to initialize retrieve THREDDS url from configuration file");
//				return null;
//			} else {
//				mSearch_THREDDS = new opendap.clients.odc.THREDDS.THREDDSSearch();
//				StringBuffer sbError = new StringBuffer( 80 );
//				if( !mSearch_THREDDS.zInitialize( sbError ) ){
//					vShowError("Failed to initialize THREDDS interface: " + sbError);
//					return null;
//				}
//			}
//		}
//		return mSearch_THREDDS;
	}

	opendap.clients.odc.DatasetList.DatasetList getDatasetListSearch(){
		if( mSearch_XML == null ){
			String urlXML = ConfigurationManager.getInstance().getOption("url.XML");
			if( urlXML == null ){
				this.vShowError("Failed to initialize retrieve XML url from configuration file");
				return null;
			} else {
				mSearch_XML = new opendap.clients.odc.DatasetList.DatasetList();
			}
		}
		return mSearch_XML;
	}

	boolean mzConstraintChanging = false;
	boolean isConstraintChanging(){ return mzConstraintChanging; }
	void setConstraintChanging( boolean z ){ mzConstraintChanging = z; }

	public void vActivity_Add( Activity activity ){
		mlistActivities.add(activity);
	}

	public void vActivity_Remove( Activity activity ){
		mlistActivities.remove(activity);
	}

	public void vCancelActivities(){
		ApplicationController.getInstance().vShowStatus("cancelling all activities");
		Iterator iteratorActivities = mlistActivities.iterator();
		while( iteratorActivities.hasNext() ){
			Activity activityCurrent = (Activity)iteratorActivities.next();
			activityCurrent.vCancelActivity();
		}
	}

	public boolean isActive(){ return (mlistActivities.size() > 0); }

	boolean mzIsAutoUpdating = false;
	public boolean isAutoUpdating(){ return mzIsAutoUpdating; }
	public void setAutoUpdating(boolean z){ mzIsAutoUpdating = z; }

	public static long getMemory_Max(){ return Runtime.getRuntime().maxMemory(); }
	public static long getMemory_Total(){ return Runtime.getRuntime().totalMemory(); }
	public static long getMemory_Free(){ return Runtime.getRuntime().freeMemory(); }
	public static long getMemory_Available(){
		long nMax = getMemory_Max();
		long nTotal = getMemory_Total();
		long nFree = getMemory_Free();
		return nMax - nTotal + nFree;
	}

	public String sMemoryStatus(){
		StringBuffer sb = new StringBuffer(200);
		long nMax = getMemory_Max();
		long nTotal = getMemory_Total();
		long nFree = getMemory_Free();
		sb.append("--- Memory Status ------------------------------------------------\n");
		sb.append("       max: " + Utility.sFormatFixedRight(nMax, 12, '.') + "  max available to the ODC\n");
		sb.append("     total: " + Utility.sFormatFixedRight(nTotal, 12, '.') + "  total amount currently allocated\n");
		sb.append("      free: " + Utility.sFormatFixedRight(nFree, 12, '.') + "  unused memory in the allocation\n");
		sb.append("      used: " + Utility.sFormatFixedRight((nTotal - nFree) , 12, '.') + "  " + (int)((nTotal - nFree)/nMax) + "%  used memory (total-free)\n");
		sb.append(" available: " + (int)((nMax - nTotal + nFree)/1048576) + " M  amount left (max - total + free)\n");
		return sb.toString();
	}

}

class Message extends JPanel {
	private static boolean zActive;
	private static Message instance = null;
	private static JDialog jd;
	private static JTextArea jtaMessage;
	private static JOptionPane jop;
	public static void show( String sMessage ){

		if( zActive ) return; // do not show two messages simultaneously

		try {

			zActive = true;

			// setup picker dialog if it does not exist yet
			if( instance == null ){
				instance = new Message();
				instance.setLayout(new BorderLayout());
				jtaMessage = new JTextArea();
				jtaMessage.setLineWrap(true);
				JScrollPane jsp = new JScrollPane(jtaMessage);
				instance.add(jsp, BorderLayout.CENTER);
			}

			// activate message box
			jtaMessage.setText(sMessage);
			jop = new JOptionPane(instance, JOptionPane.PLAIN_MESSAGE);
			jd = jop.createDialog(ApplicationController.getInstance().getAppFrame(), "ODC Message");

			int len = sMessage.length();
			int iWidth, iHeight;
			Dimension dimScreen = Toolkit.getDefaultToolkit().getScreenSize();
			int screen_width = (int)dimScreen.getWidth();
			int screen_height = (int)dimScreen.getHeight();
			if( len < 120 ){
				iWidth = 400;
				iHeight = 160;
			} else if( len < 1000 ) {
				iWidth = 600;
				iHeight = 400;
			} else {
				iWidth = (int)(dimScreen.getWidth() * .80f);
				iHeight = (int)(dimScreen.getHeight() * .80f);
			}
			jd.setSize(new Dimension(iWidth, iHeight));
			jd.setLocation((screen_width - iWidth)/2, (screen_height - iHeight)/2);

			jd.show();
		} catch( Exception ex ) {
			// ignore errors
		} finally {
			zActive = false;
		}

		return;
	}
}

// this is simple evaluator that can be replaced if the program ever gets a full-fledged interpreter
class Environment {
	public Environment(){
		vMakeEnvironment();
	}
	String[] asEnvironment_Symbol, asEnvironment_Value;
	public String eval( String sExp ){
		return get( sExp );
//		int len = sExp.length();
//		StringBuffer sbExp = new StringBuffer( len * 2 );
//		sbExp.append(sExp);
//		int pos = -1;
//		int ctSymbols = asEnvironment_Symbol.length - 1;
//		while( ++pos < sbExp.length() ){
//			for( int xSymbol = 1; xSymbol <= ctSymbols; xSymbol++ ){
//				String sSymbol = asEnvironment_Symbol[xSymbol];
//				int posSymbolEnd = pos + sSymbol.length();
//				if( posSymbolEnd <= sbExp.length() ){
//					if( sbExp.substring( pos, posSymbolEnd).equalsIgnoreCase(sSymbol) ){
//						sbExp.delete(pos, posSymbolEnd);
//						sbExp.insert(pos + 1, asEnvironment_Value[xSymbol]);
//					}
//				}
//			}
//		}
//		return sbExp.toString();
	}
	public void set( String sSymbol, String sValue ){
		int xSymbol = 0;
		while( true ){
			xSymbol++;
			if( xSymbol >  - 1 ){ // make new symbol
				vIncrementEnvironment();
				asEnvironment_Symbol[xSymbol] = sSymbol;
				break;
			}
			if( asEnvironment_Symbol[xSymbol].equalsIgnoreCase(sSymbol) ) break;
		}
		asEnvironment_Value[xSymbol] = sValue;
	}
	public String get( String sSymbol ){
		int xSymbol = 0;
		while( true ){
			xSymbol++;
			if( xSymbol > mlenEnvironment ) return null;
			if( asEnvironment_Symbol[xSymbol] == null ) return null;
			if( asEnvironment_Symbol[xSymbol].equalsIgnoreCase(sSymbol) ) return asEnvironment_Value[xSymbol];
		}
	}
	/** gets all environment variables */
	public String getall( ){
		int xSymbol = 0;
		StringBuffer sb = new StringBuffer(120);
		while( true ){
			xSymbol++;
			if( xSymbol > mlenEnvironment ) return sb.toString();
			sb.append(asEnvironment_Symbol[xSymbol]).append(" = ").append(asEnvironment_Value[xSymbol]);
		}
	}
	private int mlenEnvironment = 0;
	private void vIncrementEnvironment(){
		String[] asEnvironment_Symbol_new = new String[mlenEnvironment + 2];
		String[] asEnvironment_Value_new = new String[mlenEnvironment + 2];
		System.arraycopy(asEnvironment_Symbol, 0, asEnvironment_Symbol_new, 0, asEnvironment_Symbol.length);
		System.arraycopy(asEnvironment_Value, 0, asEnvironment_Value_new, 0, asEnvironment_Value.length);
		asEnvironment_Symbol = asEnvironment_Symbol_new;
		asEnvironment_Value = asEnvironment_Value_new;
		mlenEnvironment = asEnvironment_Symbol.length - 1;
	}
	private void vMakeEnvironment(){
		int ctSymbols = 1;
		mlenEnvironment = ctSymbols;
		asEnvironment_Symbol = new String[ctSymbols + 1]; // one-based
		asEnvironment_Value = new String[ctSymbols + 1];
		java.util.Date dateNow = new java.util.Date();
		asEnvironment_Symbol[1] = "now"; asEnvironment_Value[1] = dateNow.toString();
	}

}

