package opendap.clients.odc;

/**
 * Title:        Configuration Manager
 * Description:  Maintains preferential settings for the application
 * Copyright:    Copyright (c) 2002-2004
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.57
 */

import java.io.File;
import java.util.Properties;
import java.util.ArrayList;

public class ConfigurationManager {

	final static int DEFAULT_timeout_InternetConnect = 20;
	final static int DEFAULT_timeout_InternetRead = 60;

	public static final String EXTENSION_ColorSpecification = ".cs";
	public static final String EXTENSION_Data = ".odc";

    private static ConfigurationManager thisSingleton;
	public static final String SYSTEM_PROPERTY_FileSeparator = "file.separator";
	public static final String SYSTEM_PROPERTY_ClassPath = "java.class.path";
	public static final String SYSTEM_PROPERTY_JavaVersion = "java.version";
	public static final String SYSTEM_PROPERTY_OSName = "os.name";
	private static final String PROPERTIES_HEADER = "Import Preferences version 2";
	private static final String FILE_NAME_PROPERTIES = "config.txt";
	private static final String FILE_NAME_XML = "datasets.xml";
	private static final String FILE_NAME_ECHO_Valids = "ECHO_static_valids.xml";
	private static final String FILE_NAME_Gazetteer = "gazetteer.txt";

	private static final String URL_Default_DatasetList = "http://xml.opendap.org/datasets/datasets.xml";
	private static final String URL_Default_GCMD = "http://gcmd.nasa.gov/servlets/md/";

	private static final String DIR_Default_ImageCache = "ImageCache";
	private static final String DIR_Default_DataCache = "DataCache";
	private static final String DIR_Default_Plots = "plots";
	private static final String DIR_Default_Coastline = "coastline";

	private static final String FEEDBACK_Default_MailHost = "dcz.opendap.org";
	private static final String FEEDBACK_Default_MailPort = "20041";
	private static final String FEEDBACK_Default_EmailAddress = "feedback@opendap.org";
	private static final String FEEDBACK_Default_BugHost = "dodsdev.gso.uri.edu";
	private static final String FEEDBACK_Default_BugzillaRoot = "/bugzilla";

	ArrayList mlistProperties;

	public static final String PROPERTY_MODE_ReadOnly = "mode.ReadOnly";
	public static final String PROPERTY_URL_GCMD = "url.GCMD";
	public static final String PROPERTY_URL_ECHO = "url.ECHO";
	public static final String PROPERTY_URL_XML = "url.XML";
	public static final String PROPERTY_PATH_XML_Cache = "path.XML_Cache";
	public static final String PROPERTY_PATH_ECHO_Valids = "path.XML_ECHO_Valids";
	public static final String PROPERTY_PATH_Gazetteer = "path.Gazetteer"; // can be file or directory
	public static final String PROPERTY_PATH_Coastline = "path.Coastline"; // can be file or directory
	public static final String PROPERTY_DIR_ImageCache = "dir.ImageCache";
	public static final String PROPERTY_DIR_DataCache = "dir.DataCache";
	public static final String PROPERTY_DIR_Plots = "dir.Plots";
	public static final String PROPERTY_PROXY_Use = "proxy.Use";
	public static final String PROPERTY_PROXY_Host = "proxy.Host";
	public static final String PROPERTY_PROXY_Port = "proxy.Port";
	public static final String PROPERTY_MAIL_Host = "mail.Host";
	public static final String PROPERTY_MAIL_Port = "mail.Port";
	public static final String PROPERTY_FEEDBACK_EmailAddress = "feedback.EmailAddress";
	public static final String PROPERTY_FEEDBACK_EmailUserAddress = "feedback.EmailUserAddress";
	public static final String PROPERTY_FEEDBACK_BugHost = "feedback.BugHost";
	public static final String PROPERTY_FEEDBACK_BugzillaRoot = "feedback.BugzillaRoot";
	public static final String PROPERTY_INTERPROCESS_SERVER_Port = "InterprocessServer.port";
	public static final String PROPERTY_INTERPROCESS_SERVER_On = "InterprocessServer.on";
	public static final String PROPERTY_DISPLAY_IconSize = "display.IconSize";
	public static final String PROPERTY_DISPLAY_ShowSplashScreen = "display.ShowSplashScreen";
	public static final String PROPERTY_DISPLAY_ShowStandardOut = "display.ShowStandardOut";
	public static final String PROPERTY_DISPLAY_ShowPopupCancel = "display.ShowPopupCancel";
	public static final String PROPERTY_DISPLAY_ShowViewTab = "display.ShowViewTab";
	public static final String PROPERTY_DISPLAY_AllowPlotterFiles = "display.AllowPlotterFiles";
	public static final String PROPERTY_DISPLAY_ShowErrorPopups = "display.ShowErrorPopups";
	public static final String PROPERTY_DISPLAY_MarginBottom = "display.MarginBottom";
	public static final String PROPERTY_DISPLAY_MarginRight = "display.MarginRight";
	public static final String PROPERTY_DISPLAY_StartupSize_Width = "display.StartupSize_Width";
	public static final String PROPERTY_DISPLAY_StartupSize_Height = "display.StartupSize_Height";
	public static final String PROPERTY_LOGGING_ShowHeaders = "logging.ShowHeaders";
	public static final String PROPERTY_LOGGING_ReportMetrics = "logging.ReportMetrics";
	public static final String PROPERTY_OUTPUT_DodsFormat = "output.DodsFormat";
	public static final String PROPERTY_MAX_RecentCount = "max.RecentCount";
	public static final String PROPERTY_MAX_ViewCharacters = "max.ViewCharacters";
	public static final String PROPERTY_MAX_TableRows = "max.TableRows";
	public static final String PROPERTY_MAX_DirectoryCount = "max.DirectoryCount";
	public static final String PROPERTY_MAX_DirectoryFiles = "max.DirectoryFiles";
	public static final String PROPERTY_MAX_DirectoryDepth = "max.DirectoryDepth";
	public static final String PROPERTY_TIMEOUT_StatusMessage = "timeout.StatusMessage";
	public static final String PROPERTY_TIMEOUT_InternetConnect = "timeout.InternetConnect";
	public static final String PROPERTY_TIMEOUT_InternetRead = "timeout.InternetRead";
	public static final String PROPERTY_COUNT_Plots = "count.Plots";

	private String msBaseDirectoryPath = null; // sacred object

	private boolean mzReadOnly = true;

	private Properties mProperties;

	private boolean mzMacEnvironment = false;

    public static ConfigurationManager getInstance(){return thisSingleton;}

	private ConfigurationManager(){}

	private boolean mzInitialized = false;

    public static boolean zInitializeConfigurationManager( String sBaseDirectoryPath, StringBuffer sbError ){

		thisSingleton = new ConfigurationManager();

		// determine runtime paths
		if( ! thisSingleton.zResolveBaseDirectory(sBaseDirectoryPath, sbError) ){
			sbError.insert(0, "failed to resolve base directory: ");
			return false;
		}
		ApplicationController.vShowStatus( "Base Directory: " + thisSingleton.getBaseDirectory() );

		// determine read-only status
		if( ! thisSingleton.zResolveReadOnly(sbError) ){
			sbError.insert(0, "failed to determine read-only status: ");
			return false;
		}

		// is this a mac?
		if (System.getProperty("mrj.version") == null) {
			thisSingleton.mzMacEnvironment = false;
        } else {
			thisSingleton.mzMacEnvironment = true;
		}

		thisSingleton.vSetupPropertiesList();

		ApplicationController.vShowStatus( "Configuration file: " + thisSingleton.getPath_Properties() );

		thisSingleton.mzInitialized = true;

		thisSingleton.vRefresh( true ); // must be done after initialization

		return true;
	}

	boolean zResolveBaseDirectory( String sBaseDirectoryPath, StringBuffer sbError ){

		// default to working directory if necessary
		if( sBaseDirectoryPath == null || sBaseDirectoryPath.length() == 0){ // use the working directory
			sBaseDirectoryPath = System.getProperty("user.dir");
			if( sBaseDirectoryPath == null ){
				sbError.append("no base directory path supplied or working directory available");
				return false;
			}
		}

		// prevent directory path overrun
		if( sBaseDirectoryPath != null && sBaseDirectoryPath.length() > 2500 ){
			String sFragment = sBaseDirectoryPath.substring(0, 40);
			sbError.append("base directory cannot be longer than 2500 characters (" + sFragment + " ...)");
			return false;
		}

		sBaseDirectoryPath = sBaseDirectoryPath.trim();
		if( sBaseDirectoryPath.length() > 1 ){
			if( sBaseDirectoryPath.endsWith(System.getProperty(SYSTEM_PROPERTY_FileSeparator)) ){
				sBaseDirectoryPath = sBaseDirectoryPath.substring(0, sBaseDirectoryPath.length() - 1);
			}
		}

		// create and validate base directory file object
		File fileBaseDirectory;
		String sCanonicalPath;
		try {
		    fileBaseDirectory = new File( sBaseDirectoryPath );
		} catch(Exception ex) {
			sbError.append("base directory string could not be interpreted as a path (" + Utility.sSafeSubstring(sBaseDirectoryPath, 0, 80) + ")");
			return false;
		}
		try {
		    sCanonicalPath = fileBaseDirectory.getCanonicalPath();
			if( sCanonicalPath == null || sCanonicalPath.length() == 0 ){
			   sbError.append("could not obtain canonical path for base directory (" + Utility.sSafeSubstring(sBaseDirectoryPath, 0, 80) + ")");
			   return false;
			}
		} catch(Exception ex) {
			sbError.append("error obtaining canonical path for base directory (" + Utility.sSafeSubstring(sBaseDirectoryPath, 0, 80) + "): " + ex);
			return false;
		}
		if( !fileBaseDirectory.isDirectory() ){
			sbError.append("base directory path does not resolve to a directory: " + sCanonicalPath);
			return false;
		}
		if( !fileBaseDirectory.exists() ){
			sbError.append("base directory does not exist: " + sCanonicalPath);
			return false;
		}

		msBaseDirectoryPath = sCanonicalPath; // so far so good

		// make sure the base directory contains datasets.xml
		// the user must be running the ODC from its directory or have the base directory set in the config.txt file
		boolean zDatasetListValidated = false;
		String sDatasetListPath;
		try {
			sDatasetListPath = getProperty_PATH_XML_Cache();
			File fileDatasetList = new File( sDatasetListPath );
			zDatasetListValidated = fileDatasetList.exists();
		} catch(Exception ex) {}
		if( !zDatasetListValidated ){
			sbError.append("incorrect working directory (dataset list not present); you must either run the ODC from its directory or specify a base directory as an application argument (see installation document, section \"Base Directory\"); current directory is " + sCanonicalPath);
			return false;
		}

		return true;
	}

	private boolean zResolveReadOnly(StringBuffer sbError){
		mzReadOnly = false; // default condition
		File fileBaseDirectory;
		File fileDatasetsXML;
		File fileConfiguration;
		File fileGazetteer;
		String sPath_Configuration = getPath_Properties();
		String sPath_DatasetsXML = getProperty_PATH_XML_Cache();
		String sPath_Gazetteer = getProperty_PATH_Gazetteer();
		try {
			fileBaseDirectory = new File( msBaseDirectoryPath );
			fileConfiguration = new File( sPath_Configuration );
			fileDatasetsXML = new File( sPath_DatasetsXML );
			fileGazetteer = new File( sPath_Gazetteer );
		} catch(Exception ex) {
			sbError.append("unable to determine paths for core files/directories");
			return false;
		}
		if( !fileBaseDirectory.canRead() ){
			sbError.append("cannot read base directory; user permissions/access error");
			return false;
		}
		if( fileConfiguration.exists() && !fileConfiguration.canRead() ){
			sbError.append("cannot read configuration file (" + sPath_Configuration + "); user permissions/access error");
			return false;
		}
		if( !fileDatasetsXML.canRead() ){
			sbError.append("cannot read datasets file (" + sPath_DatasetsXML + "); user permissions/access error");
			return false;
		}
		if( !fileGazetteer.canRead() ){
			sbError.append("cannot read gazetteer file (" + sPath_Gazetteer + "); user permissions/access error");
			return false;
		}
		if( !fileBaseDirectory.canWrite() ){
			SystemConduit.setDirectoryReadWrite(msBaseDirectoryPath); // try to make writable
			SystemConduit.setDirectoryFilesReadWrite(msBaseDirectoryPath);
			try { Thread.sleep(1000); } catch(Exception ex) {}
			if( !fileBaseDirectory.canWrite() ){ // try again, if still failing to write go to read-only mode
				mzReadOnly = true;
				ApplicationController.vShowStatus("Starting in read-only mode (unable to write to base directory)");
			}
		}
		if( !fileBaseDirectory.canWrite() ){
			ApplicationController.vShowStatus("Starting in read-only mode (base directory not writable: " + msBaseDirectoryPath + ")");
			mzReadOnly = true;
		} else if( fileConfiguration.exists() && !fileConfiguration.canWrite() ){
			ApplicationController.vShowStatus("Starting in read-only mode (configuration file not writable: " + sPath_Configuration + ")");
			mzReadOnly = true;
		} else if( !fileDatasetsXML.canWrite() ){
			ApplicationController.vShowStatus("Starting in read-only mode (datasets.xml not writable: " + sPath_DatasetsXML + ")");
			mzReadOnly = true;
		} else {
			String s = this.getInstance().getOption(PROPERTY_MODE_ReadOnly);
			if( s != null ){
				if( s.toUpperCase().startsWith("Y") || s.toUpperCase().equals("TRUE")){
					mzReadOnly = true;
				    ApplicationController.vShowStatus("Starting in read-only mode (config file setting)");
				}
			}
		}
		return true;
	}

	private void vSetupPropertiesList(){
		mlistProperties = new ArrayList();
		mlistProperties.add( PROPERTY_MODE_ReadOnly);
		mlistProperties.add( PROPERTY_URL_GCMD);
		mlistProperties.add( PROPERTY_URL_ECHO );
		mlistProperties.add( PROPERTY_URL_XML );
		mlistProperties.add( PROPERTY_PATH_XML_Cache );
		mlistProperties.add( PROPERTY_PATH_ECHO_Valids );
		mlistProperties.add( PROPERTY_PATH_Gazetteer );
		mlistProperties.add( PROPERTY_PATH_Coastline );
		mlistProperties.add( PROPERTY_DIR_ImageCache );
		mlistProperties.add( PROPERTY_DIR_DataCache );
		mlistProperties.add( PROPERTY_DIR_Plots );
		mlistProperties.add( PROPERTY_PROXY_Use );
		mlistProperties.add( PROPERTY_PROXY_Host );
		mlistProperties.add( PROPERTY_PROXY_Port );
		mlistProperties.add( PROPERTY_MAIL_Host );
		mlistProperties.add( PROPERTY_MAIL_Port );
		mlistProperties.add( PROPERTY_FEEDBACK_EmailAddress );
		mlistProperties.add( PROPERTY_FEEDBACK_EmailUserAddress );
		mlistProperties.add( PROPERTY_FEEDBACK_BugHost );
		mlistProperties.add( PROPERTY_FEEDBACK_BugzillaRoot );
		mlistProperties.add( PROPERTY_INTERPROCESS_SERVER_Port );
		mlistProperties.add( PROPERTY_INTERPROCESS_SERVER_On );
		mlistProperties.add( PROPERTY_DISPLAY_IconSize );
		mlistProperties.add( PROPERTY_DISPLAY_ShowSplashScreen );
		mlistProperties.add( PROPERTY_DISPLAY_ShowStandardOut );
		mlistProperties.add( PROPERTY_DISPLAY_ShowPopupCancel );
		mlistProperties.add( PROPERTY_DISPLAY_ShowViewTab );
		mlistProperties.add( PROPERTY_DISPLAY_AllowPlotterFiles );
		mlistProperties.add( PROPERTY_DISPLAY_ShowErrorPopups );
		mlistProperties.add( PROPERTY_DISPLAY_MarginBottom );
		mlistProperties.add( PROPERTY_DISPLAY_MarginRight );
		mlistProperties.add( PROPERTY_DISPLAY_StartupSize_Width );
		mlistProperties.add( PROPERTY_DISPLAY_StartupSize_Height );
		mlistProperties.add( PROPERTY_LOGGING_ShowHeaders );
		mlistProperties.add( PROPERTY_LOGGING_ReportMetrics );
		mlistProperties.add( PROPERTY_OUTPUT_DodsFormat );
		mlistProperties.add( PROPERTY_MAX_RecentCount );
		mlistProperties.add( PROPERTY_MAX_ViewCharacters );
		mlistProperties.add( PROPERTY_MAX_TableRows );
		mlistProperties.add( PROPERTY_MAX_DirectoryCount );
		mlistProperties.add( PROPERTY_MAX_DirectoryFiles );
		mlistProperties.add( PROPERTY_MAX_DirectoryDepth );
		mlistProperties.add( PROPERTY_TIMEOUT_StatusMessage );
		mlistProperties.add( PROPERTY_TIMEOUT_InternetConnect );
		mlistProperties.add( PROPERTY_TIMEOUT_InternetRead );
		mlistProperties.add( PROPERTY_COUNT_Plots );
	}


	public String getBaseDirectory(){
		return msBaseDirectoryPath;
	}
	public boolean getIsMacEnvironment(){ return mzMacEnvironment; }
	public String getProperty_URL_GCMD(){ return this.getInstance().getOption(PROPERTY_URL_GCMD, getDefault_URL_GCMD()); }
	public String getProperty_URL_ECHO(){ return this.getInstance().getOption(PROPERTY_URL_ECHO, getDefault_URL_ECHO()); }
	public String getProperty_URL_XML(){ return this.getInstance().getOption(PROPERTY_URL_XML, getDefault_URL_DatasetList()); }
	public String getProperty_PATH_XML_Cache(){ return this.getInstance().getOption(PROPERTY_PATH_XML_Cache, this.getDefault_PATH_XML()); }
	public String getProperty_PATH_XML_ECHO_Valids(){ return this.getInstance().getOption(PROPERTY_PATH_ECHO_Valids, this.getDefault_PATH_ECHO_Valids()); }
	public String getProperty_PATH_Gazetteer(){ return this.getInstance().getOption(PROPERTY_PATH_Gazetteer, this.getDefault_PATH_Gazetteer()); }
	public String getProperty_PATH_Coastline(){ return this.getInstance().getOption(PROPERTY_PATH_Coastline, this.getDefault_PATH_Coastline()); }
	public String getProperty_DIR_ImageCache(){ return this.getInstance().getOption(PROPERTY_DIR_ImageCache, this.getDefault_DIR_ImageCache()); }
	public String getProperty_DIR_DataCache(){ return this.getInstance().getOption(PROPERTY_DIR_DataCache, this.getDefault_DIR_DataCache()); }
	public String getProperty_DIR_Plots(){ return this.getInstance().getOption(PROPERTY_DIR_Plots, this.getDefault_DIR_Plots()); }
	public String getProperty_DISPLAY_IconSize(){ return this.getInstance().getOption(PROPERTY_DISPLAY_IconSize, "16"); }
	public boolean getProperty_MODE_ReadOnly(){
		return mzReadOnly;
	}
	public boolean getProperty_DISPLAY_ShowStandardOut(){
		String s = this.getInstance().getOption(PROPERTY_DISPLAY_ShowStandardOut);
		if( s == null ){
			return getDefault_DISPLAY_ShowStandardOut();
		}
		if( s.toUpperCase().startsWith("Y") || s.toUpperCase().equals("TRUE")) return true;
		return false;
	}
	public boolean getProperty_DISPLAY_ShowViewTab(){
		String s = this.getInstance().getOption(PROPERTY_DISPLAY_ShowViewTab);
		if( s == null ){
			return true; // default
		}
		if( s.toUpperCase().startsWith("Y") || s.toUpperCase().equals("TRUE") ) return true;
		return false;
	}
	public boolean getProperty_DISPLAY_AllowPlotterFiles(){
		String s = this.getInstance().getOption(PROPERTY_DISPLAY_AllowPlotterFiles);
		if( s == null ){
			return false; // default
		}
		if( s.toUpperCase().startsWith("Y") || s.toUpperCase().equals("TRUE") ) return true;
		return false;
	}
	public boolean getProperty_DISPLAY_ShowErrorPopups(){
		String s = this.getInstance().getOption(PROPERTY_DISPLAY_ShowErrorPopups);
		if( s == null ){
			return true; // default
		}
		if( s.toUpperCase().startsWith("Y") || s.toUpperCase().equals("TRUE") ) return true;
		return false;
	}
	public boolean getProperty_LOGGING_ShowHeaders(){
		String s = this.getInstance().getOption(PROPERTY_LOGGING_ShowHeaders);
		if( s == null ){
			return false; // default
		}
		if( s.toUpperCase().startsWith("Y") || s.toUpperCase().equals("TRUE") ) return true;
		return false;
	}
	public boolean getProperty_LOGGING_ReportMetrics(){
		String s = this.getInstance().getOption(PROPERTY_LOGGING_ReportMetrics);
		if( s == null ){
			return false; // default
		}
		if( s.toUpperCase().startsWith("Y") || s.toUpperCase().equals("TRUE") ) return true;
		return false;
	}
	public boolean getDefault_DISPLAY_ShowStandardOut(){
		return isUNIX();  // show standard out if it is a UNIX system
	}

	public boolean isUNIX(){
		String sOS = System.getProperty(SYSTEM_PROPERTY_OSName);
		if( sOS == null ) return false;
		sOS = sOS.toUpperCase();
		if( sOS.startsWith("UNIX") ) return true;
		if( sOS.startsWith("LINUX") ) return true;
		if( sOS.startsWith("SOLARIS") ) return true;
		if( sOS.startsWith("SUNOS") ) return true;
		if( sOS.startsWith("HP-UX") ) return true;
		if( sOS.startsWith("MPE/IX") ) return true;
		if( sOS.startsWith("FREEBSD") ) return true;
		if( sOS.startsWith("IRIX") ) return true;
		if( sOS.startsWith("DIGITAL UNIX") ) return true;
		if( sOS.startsWith("MINIX") ) return true;
		return false;
	}

	/** Returns an output profile format constant (see class OutputProfile) */
	public int getProperty_OUTPUT_DodsFormat(){
		String s = this.getInstance().getOption(PROPERTY_OUTPUT_DodsFormat);
		if( s == null ){
			return OutputProfile.FORMAT_Data_ASCII_text;
		} else if( s.toUpperCase().equals("RAW") ){
			return OutputProfile.FORMAT_Data_Raw;
		} else if( s.toUpperCase().equals("ASCII") ){
			return OutputProfile.FORMAT_Data_ASCII_text;
		} else if( s.toUpperCase().equals("FORMATTED") ){
			return OutputProfile.FORMAT_Data_ASCII_text;
		} else if( s.toUpperCase().equals("DODS") ){
			return OutputProfile.FORMAT_Data_DODS;
		} else {
			return OutputProfile.FORMAT_Data_ASCII_text;
		}
	}

	public int getProperty_MarginRight(){
		String sMarginRight = this.getInstance().getOption(PROPERTY_DISPLAY_MarginRight, "0");
		int iMarginRight = 0;
		try {
			iMarginRight = Integer.parseInt(sMarginRight);
			if( iMarginRight < 0 ) iMarginRight *= -1;
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowWarning("Invalid right margin setting [" + sMarginRight + "]. Must be an integer.");
		}
		return iMarginRight;
	}

	public int getProperty_MarginBottom(){
		String sMarginBottom = this.getInstance().getOption(PROPERTY_DISPLAY_MarginBottom, "32");
		int iMarginBottom = 32;
		try {
			iMarginBottom = Integer.parseInt(sMarginBottom);
			if( iMarginBottom < 0 ) iMarginBottom *= -1;
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowWarning("Invalid bottom margin setting [" + sMarginBottom + "]. Must be an integer.");
		}
		return iMarginBottom;
	}

	public int getProperty_StartupSize_Width(){
		String sStartupSize_Width = this.getInstance().getOption(PROPERTY_DISPLAY_StartupSize_Width, "0");
		int iStartupSize_Width = 0;
		try {
			iStartupSize_Width = Integer.parseInt(sStartupSize_Width);
			if( iStartupSize_Width < 0 ) iStartupSize_Width *= -1;
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowWarning("Invalid startup size width setting [" + sStartupSize_Width + "]. Must be an integer.");
		}
		return iStartupSize_Width;
	}

	public int getProperty_StartupSize_Height(){
		String sStartupSize_Height = this.getInstance().getOption(PROPERTY_DISPLAY_StartupSize_Height, "0");
		int iStartupSize_Height = 0;
		try {
			iStartupSize_Height = Integer.parseInt(sStartupSize_Height);
			if( iStartupSize_Height < 0 ) iStartupSize_Height *= -1;
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowWarning("Invalid startup size Height setting [" + sStartupSize_Height + "]. Must be an integer.");
		}
		return iStartupSize_Height;
	}

	public int getProperty_RecentCount(){
		String sRecentCount = this.getInstance().getOption(PROPERTY_MAX_RecentCount, "50");
		int iRecentCount = 50;
		try {
			iRecentCount = Integer.parseInt(sRecentCount);
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowWarning("Invalid recent count setting [" + sRecentCount + "]. Must be an integer.");
		}
		return iRecentCount;
	}
	public int getProperty_MaxTableRows(){
		String sMax = this.getInstance().getOption(PROPERTY_MAX_TableRows, "10000");
		int iMax = 10000;
		try {
			iMax = Integer.parseInt(sMax);
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowWarning("Invalid max table rows setting [" + sMax + "]. Must be an integer.");
		}
		return iMax;
	}
	public int getProperty_MaxViewCharacters(){
		String sMaxViewCharacters = this.getInstance().getOption(PROPERTY_MAX_ViewCharacters, "100000");
		int iMaxViewCharacters = 100000;
		try {
			iMaxViewCharacters = Integer.parseInt(sMaxViewCharacters);
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowWarning("Invalid max view characters setting [" + sMaxViewCharacters + "]. Must be an integer.");
		}
		return iMaxViewCharacters;
	}
	public int getProperty_DirectoryCount(){
		int iMaxDirectoryCount = 100; // default
		String sMaxDirectoryCount = this.getInstance().getOption(PROPERTY_MAX_DirectoryCount, Integer.toString(iMaxDirectoryCount));
		try {
			iMaxDirectoryCount = Integer.parseInt(sMaxDirectoryCount);
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowWarning("Invalid max directory count setting [" + sMaxDirectoryCount + "]. Must be an integer.");
		}
		return iMaxDirectoryCount;
	}
	public int getProperty_DirectoryFiles(){
		int iMaxDirectoryFiles = 10000; // default
		String sMaxDirectoryFiles = this.getInstance().getOption(PROPERTY_MAX_DirectoryFiles, Integer.toString(iMaxDirectoryFiles));
		try {
			iMaxDirectoryFiles = Integer.parseInt(sMaxDirectoryFiles);
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowWarning("Invalid max directory files setting [" + sMaxDirectoryFiles + "]. Must be an integer.");
		}
		return iMaxDirectoryFiles;
	}
	public int getProperty_DirectoryDepth(){
		int iMaxDirectoryDepth = 10; // default
		String sMaxDirectoryDepth = this.getInstance().getOption(PROPERTY_MAX_DirectoryDepth, Integer.toString(iMaxDirectoryDepth));
		try {
			iMaxDirectoryDepth = Integer.parseInt(sMaxDirectoryDepth);
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowWarning("Invalid max directory depth setting [" + sMaxDirectoryDepth + "]. Must be an integer.");
		}
		return iMaxDirectoryDepth;
	}
	public boolean getProperty_ProxyUse(){
		String s = getInstance().getOption(PROPERTY_PROXY_Use);
		if( s == null ){
			return false; // default
		}
		if( s.toUpperCase().startsWith("Y") || s.toUpperCase().equals("TRUE") ) return true;
		return false;
	}
	public String getProperty_ProxyHost(){
		return getInstance().getOption(PROPERTY_PROXY_Host, null);
	}
	public String getProperty_ProxyPort(){
		return getInstance().getOption(PROPERTY_PROXY_Port, null);
	}
	public String getProperty_MailHost(){
		return getInstance().getOption(PROPERTY_MAIL_Host, FEEDBACK_Default_MailHost);
	}
	public int getProperty_MailPort(){
		String sPort = getInstance().getOption(PROPERTY_MAIL_Port, FEEDBACK_Default_MailPort);
		int iMailServerPort = 20041;
		try {
			iMailServerPort = Integer.parseInt(sPort);
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowWarning("Invalid mail server port setting [" + sPort + "]. Must be an integer.");
		}
		return iMailServerPort;
	}
	public String getProperty_FEEDBACK_EmailAddress(){
		return getInstance().getOption(PROPERTY_FEEDBACK_EmailAddress, FEEDBACK_Default_EmailAddress);
	}
	public String getProperty_FEEDBACK_EmailUserAddress(){
		return getInstance().getOption(PROPERTY_FEEDBACK_EmailUserAddress, "");
	}
	public String getProperty_FEEDBACK_BugHost(){
		return getInstance().getOption(PROPERTY_FEEDBACK_BugHost, FEEDBACK_Default_BugHost);
	}
	public String getProperty_FEEDBACK_BugzillaRoot(){
		return getInstance().getOption(PROPERTY_FEEDBACK_BugzillaRoot, FEEDBACK_Default_BugzillaRoot);
	}
	public int getProperty_InterprocessServerPort(){
		String sInterprocessServerPort = this.getInstance().getOption(PROPERTY_INTERPROCESS_SERVER_Port, "31870");
		int iInterprocessServerPort = 31870;
		try {
			iInterprocessServerPort = Integer.parseInt(sInterprocessServerPort);
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowWarning("Invalid interprocess server port setting [" + sInterprocessServerPort + "]. Must be an integer.");
		}
		return iInterprocessServerPort;
	}
	public boolean getProperty_InterprocessServerOn(){
		String sInterprocessServerOn = this.getInstance().getOption(PROPERTY_INTERPROCESS_SERVER_On, "Yes");
		if( sInterprocessServerOn == null ) return true;
		if( sInterprocessServerOn.startsWith("y") || sInterprocessServerOn.startsWith("Y") ) return true;
		if( sInterprocessServerOn.equalsIgnoreCase("true") ) return true;
		return false;
	}
	public boolean getProperty_DISPLAY_ShowSplashScreen(){
		String sValue = this.getInstance().getOption(PROPERTY_DISPLAY_ShowSplashScreen, "Yes");
		if( sValue == null ) return true;
		if( sValue.length() == 0 ) return true;
		if( sValue.toUpperCase().startsWith("Y") ) return true;
		return false;
	}
	public boolean getProperty_DISPLAY_ShowPopupCancel(){
		String sValue = this.getInstance().getOption(PROPERTY_DISPLAY_ShowPopupCancel, "Yes");
		if( sValue == null ) return true;
		if( sValue.length() == 0 ) return true;
		if( sValue.toUpperCase().startsWith("Y") ) return true;
		return false;
	}
	public String getProperty_PreferencesDirectory(){
		String sWorkingDirectory = this.getBaseDirectory();
		String sSeparator = System.getProperty(SYSTEM_PROPERTY_FileSeparator);
		if( sWorkingDirectory == null ) return null;
		String sPreferencesDirectory = Utility.sConnectPaths(sWorkingDirectory, sSeparator, "preferences") + sSeparator;
		return sPreferencesDirectory;
	}
	public int getProperty_StatusTimeout(){
		String sStatusTimeout = this.getInstance().getOption(PROPERTY_TIMEOUT_StatusMessage, "20000");
		int iStatusTimeout = 20000;
		try {
			iStatusTimeout = Integer.parseInt(sStatusTimeout);
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowWarning("Invalid status message timeout setting [" + sStatusTimeout + "]. Must be an integer.");
		}
		return iStatusTimeout;
	}
	public int getProperty_Timeout_InternetConnect(){
		String sTimeout = this.getInstance().getOption(PROPERTY_TIMEOUT_InternetConnect, Integer.toString(DEFAULT_timeout_InternetConnect));
		int iTimeout = DEFAULT_timeout_InternetConnect;
		try {
			iTimeout = Integer.parseInt(sTimeout);
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowWarning("Invalid status message timeout setting [" + sTimeout + "]. Must be an integer in seconds.");
		}
		return iTimeout;
	}
	public int getProperty_Timeout_InternetRead(){
		String sTimeout = this.getInstance().getOption(PROPERTY_TIMEOUT_InternetRead, Integer.toString(DEFAULT_timeout_InternetRead));
		int iTimeout = DEFAULT_timeout_InternetRead;
		try {
			iTimeout = Integer.parseInt(sTimeout);
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowWarning("Invalid status message timeout setting [" + sTimeout + "]. Must be an integer in seconds.");
		}
		return iTimeout;
	}
	public int getProperty_PlotCount(){
		String sStatusTimeout = this.getInstance().getOption(PROPERTY_COUNT_Plots, "0");
		try {
			int iPlotCount = Integer.parseInt(sStatusTimeout);
			if( iPlotCount >= 0 ) return iPlotCount;
		} catch(Exception ex) {}
		ApplicationController.getInstance().vShowWarning("Invalid plot count setting [" + sStatusTimeout + "]. Must be a non-negative integer.");
		return 0;
	}

	public String getDefault_DISPLAY_IconSize(){ return "16"; }

	String getDefault_DIR_ImageCache(){
		String sWorkingDirectory = this.getBaseDirectory();
		if( sWorkingDirectory == null ) sWorkingDirectory = "";
		String sSeparator = System.getProperty(SYSTEM_PROPERTY_FileSeparator);
		String sCacheDirectory = Utility.sConnectPaths(sWorkingDirectory, sSeparator, DIR_Default_ImageCache) + sSeparator;
		return sCacheDirectory;
	}

	String getDefault_DIR_DataCache(){
		String sWorkingDirectory = this.getBaseDirectory();
		if( sWorkingDirectory == null ) sWorkingDirectory = "";
		String sSeparator = System.getProperty(SYSTEM_PROPERTY_FileSeparator);
		String sCacheDirectory = Utility.sConnectPaths(sWorkingDirectory, sSeparator, DIR_Default_DataCache) + sSeparator;
		return sCacheDirectory;
	}

	String getDefault_DIR_Plots(){
		String sWorkingDirectory = this.getBaseDirectory();
		if( sWorkingDirectory == null ) sWorkingDirectory = "";
		String sSeparator = System.getProperty(SYSTEM_PROPERTY_FileSeparator);
		String sPlotsDirectory = Utility.sConnectPaths(sWorkingDirectory, sSeparator, DIR_Default_Plots) + sSeparator;
		return sPlotsDirectory;
	}

	String getDefault_URL_GCMD(){ return URL_Default_GCMD; }

	String getDefault_URL_ECHO(){ return "http://fosters.gsfc.nasa.gov:4500/"; }

	String getDefault_URL_DatasetList(){ return URL_Default_DatasetList; }

	String getDefault_PATH_XML(){
		return Utility.sConnectPaths(getBaseDirectory(), FILE_NAME_XML);
	}

	String getDefault_PATH_ECHO_Valids(){
		return Utility.sConnectPaths(getBaseDirectory(), FILE_NAME_ECHO_Valids);
	}

	String getDefault_PATH_Gazetteer(){
		return Utility.sConnectPaths(getBaseDirectory(), FILE_NAME_Gazetteer);
	}

	String getDefault_PATH_Coastline(){
		return Utility.sConnectPaths(getBaseDirectory(), DIR_Default_Coastline);
	}

	String getPath_Properties(){
		return Utility.sConnectPaths(getBaseDirectory(), FILE_NAME_PROPERTIES);
	}

	public String getOption( String sKey ){
		return getOption( sKey, null );
	}

	public String getOption( String sKey, String sDefault ){
		if( mProperties == null ) vRefresh( false );
		return mProperties.getProperty(sKey, sDefault);
	}

	public void setOption( String sKey, String sValue ){
		if( mzReadOnly ){
			ApplicationController.vShowWarning("internal error, attempt to set option in read-only mode: " + sKey);
			return;
		}
		try {
			boolean zStore = mzInitialized;
	    	if( mProperties == null ) vRefresh( zStore );
		    mProperties.setProperty(sKey, sValue);
			if( zStore ) vStoreProperties();
	    	ApplicationController.vSetOptions();
		} catch( Exception ex ) {
			ApplicationController.vShowWarning("Failed to set option: '" + sKey + "' with error: " + ex);
			return;
		}
	}

	public boolean isOption( String sKey ){
		if( mProperties == null ) vRefresh( false );
		for( int xList = 0; xList < mlistProperties.size(); xList++ ){
			if( mlistProperties.get(xList).equals(sKey) ) return true;
		}
		return false;
	}

	public String sDump(){
		StringBuffer sb = new StringBuffer(1000);
		sb.append("base directory is " + this.getBaseDirectory() + "\n" );
		sb.append("is mac? " + (this.mzMacEnvironment ? "yes" : "no") + "\n" );
		sb.append(PROPERTY_MODE_ReadOnly + " = " + this.getProperty_MODE_ReadOnly() + "\n" );
		sb.append(PROPERTY_URL_GCMD + " = " + this.getProperty_URL_GCMD() + "\n" );
		sb.append(PROPERTY_URL_ECHO + " = " + this.getProperty_URL_ECHO() + "\n" );
		sb.append(PROPERTY_URL_XML + " = " + this.getProperty_URL_XML() + "\n" );
		sb.append(PROPERTY_PATH_XML_Cache + " = " + this.getProperty_PATH_XML_Cache() + "\n" );
		sb.append(PROPERTY_PATH_ECHO_Valids + " = " + this.getProperty_PATH_XML_ECHO_Valids() + "\n" );
		sb.append(PROPERTY_PATH_Gazetteer + " = " + this.getProperty_PATH_Gazetteer() + "\n" );
		sb.append(PROPERTY_PATH_Coastline + " = " + this.getProperty_PATH_Coastline() + "\n" );
		sb.append(PROPERTY_DIR_ImageCache + " = " + this.getProperty_DIR_ImageCache() + "\n" );
		sb.append(PROPERTY_DIR_DataCache + " = " + this.getProperty_DIR_DataCache() + "\n" );
		sb.append(PROPERTY_DIR_Plots + " = " + this.getProperty_DIR_Plots() + "\n" );
		sb.append(PROPERTY_PROXY_Use + " = " + (this.getProperty_ProxyUse() ? "Yes" : "No") + "\n" );
		sb.append(PROPERTY_PROXY_Host + " = " + (this.getProperty_ProxyHost()==null ? "[none]" : this.getProperty_ProxyHost()) + "\n" );
		sb.append(PROPERTY_PROXY_Port + " = " + (this.getProperty_ProxyPort()==null ? "[none]" : this.getProperty_ProxyPort()) + "\n" );
		sb.append(PROPERTY_MAIL_Host + " = " + (getProperty_MailHost()==null ? "[none]" : getProperty_MailHost()) + "\n" );
		sb.append(PROPERTY_MAIL_Port + " = " + (getProperty_MailPort() < 0 ? "[none]" : Integer.toString(getProperty_MailPort())) + "\n" );
		sb.append(PROPERTY_FEEDBACK_EmailAddress + " = " + (this.getProperty_FEEDBACK_EmailAddress()==null ? "[none (default is " + FEEDBACK_Default_EmailAddress + ")]" : this.getProperty_FEEDBACK_EmailAddress()) + "\n" );
		sb.append(PROPERTY_FEEDBACK_EmailUserAddress + " = " + (this.getProperty_FEEDBACK_EmailUserAddress()==null ? "[none]" : this.getProperty_FEEDBACK_EmailAddress()) + "\n" );
		sb.append(PROPERTY_FEEDBACK_BugHost + " = " + (this.getProperty_FEEDBACK_BugHost()==null ? "[none (default is " + FEEDBACK_Default_BugHost + ")]" : this.getProperty_FEEDBACK_BugHost()) + "\n" );
		sb.append(PROPERTY_FEEDBACK_BugzillaRoot + " = " + (this.getProperty_FEEDBACK_BugzillaRoot()==null ? "[none (default is " + FEEDBACK_Default_BugzillaRoot + ")]" : this.getProperty_FEEDBACK_BugzillaRoot()) + "\n" );
		sb.append(PROPERTY_INTERPROCESS_SERVER_Port + " = " + this.getProperty_InterprocessServerPort() + "\n" );
		sb.append(PROPERTY_INTERPROCESS_SERVER_On + " = " + (this.getProperty_InterprocessServerOn() ? "Yes" : "No") + "\n" );
		sb.append(PROPERTY_DISPLAY_IconSize + " = " + this.getProperty_DISPLAY_IconSize() + "\n" );
		sb.append(PROPERTY_DISPLAY_MarginBottom + " = " + this.getProperty_MarginBottom() + "\n" );
		sb.append(PROPERTY_DISPLAY_MarginRight + " = " + this.getProperty_MarginRight() + "\n" );
		sb.append(PROPERTY_DISPLAY_StartupSize_Width + " = " + this.getProperty_StartupSize_Width() + "\n" );
		sb.append(PROPERTY_DISPLAY_StartupSize_Height + " = " + this.getProperty_StartupSize_Height() + "\n" );
		sb.append(PROPERTY_DISPLAY_ShowSplashScreen + " = " + (this.getProperty_DISPLAY_ShowSplashScreen() ? "Yes" : "No") + "\n" );
		sb.append(PROPERTY_DISPLAY_ShowStandardOut + " = " + (this.getProperty_DISPLAY_ShowStandardOut() ? "Yes" : "No") + "\n" );
		sb.append(PROPERTY_DISPLAY_ShowPopupCancel + " = " + (this.getProperty_DISPLAY_ShowPopupCancel() ? "Yes" : "No") + "\n" );
		sb.append(PROPERTY_DISPLAY_ShowViewTab + " = " + (this.getProperty_DISPLAY_ShowViewTab() ? "Yes" : "No") + "\n" );
		sb.append(PROPERTY_DISPLAY_ShowErrorPopups + " = " + (this.getProperty_DISPLAY_ShowErrorPopups() ? "Yes" : "No") + "\n" );
		sb.append(PROPERTY_DISPLAY_AllowPlotterFiles + " = " + (this.getProperty_DISPLAY_AllowPlotterFiles() ? "Yes" : "No") + "\n" );
		sb.append(PROPERTY_LOGGING_ShowHeaders + " = " + (this.getProperty_LOGGING_ShowHeaders() ? "Yes" : "No") + "\n" );
		sb.append(PROPERTY_LOGGING_ReportMetrics + " = " + (this.getProperty_LOGGING_ReportMetrics() ? "Yes" : "No") + "\n" );
		sb.append(PROPERTY_OUTPUT_DodsFormat + " = " + OutputProfile.sFormatDescription(this.getProperty_OUTPUT_DodsFormat()) + " [ASCII, Formatted, Raw, DODS]"+ "\n");
		sb.append(PROPERTY_MAX_RecentCount + " = " + this.getProperty_RecentCount() + "\n" );
		sb.append(PROPERTY_MAX_ViewCharacters + " = " + this.getProperty_MaxViewCharacters() + "\n" );
		sb.append(PROPERTY_MAX_TableRows + " = " + this.getProperty_MaxTableRows() + "\n" );
		sb.append(PROPERTY_MAX_DirectoryCount + " = " + this.getProperty_DirectoryCount() + "\n" );
		sb.append(PROPERTY_MAX_DirectoryFiles + " = " + this.getProperty_DirectoryFiles() + "\n" );
		sb.append(PROPERTY_MAX_DirectoryDepth + " = " + this.getProperty_DirectoryDepth() + "\n" );
		sb.append(PROPERTY_TIMEOUT_StatusMessage + " = " + this.getProperty_StatusTimeout() + "\n" );
		sb.append(PROPERTY_TIMEOUT_InternetConnect + " = " + this.getProperty_Timeout_InternetConnect() + "\n" );
		sb.append(PROPERTY_TIMEOUT_InternetRead + " = " + this.getProperty_Timeout_InternetRead() + "\n" );
		sb.append(PROPERTY_COUNT_Plots + " = " + this.getProperty_PlotCount() + "\n" );
		return sb.toString();
	}

	void vRefresh( boolean zStore ){
		mProperties = new Properties();
		java.io.FileInputStream fisProperties = null;
		String sPath = getPath_Properties();
		boolean zReadOnly;
		if( zStore ){
			try {
				java.io.File fileProperties = new java.io.File(sPath);
				if( fileProperties.exists() ){
					fisProperties = new java.io.FileInputStream(fileProperties);
				}
			} catch(Exception ex) {
				// does not exist -- fisProperties will be null
			}
		}
		if( fisProperties == null ){ // create a new file
			mProperties.setProperty( PROPERTY_URL_GCMD, getDefault_URL_GCMD());
			mProperties.setProperty( PROPERTY_URL_ECHO, getDefault_URL_ECHO());
			mProperties.setProperty( PROPERTY_URL_XML, getDefault_URL_DatasetList());
			mProperties.setProperty( PROPERTY_PATH_XML_Cache, this.getDefault_PATH_XML());
			mProperties.setProperty( PROPERTY_PATH_ECHO_Valids, this.getDefault_PATH_ECHO_Valids());
			mProperties.setProperty( PROPERTY_PATH_Gazetteer, this.getDefault_PATH_Gazetteer());
			mProperties.setProperty( PROPERTY_PATH_Coastline, this.getDefault_PATH_Coastline());
			mProperties.setProperty( PROPERTY_DIR_ImageCache, this.getDefault_DIR_ImageCache());
			mProperties.setProperty( PROPERTY_DIR_DataCache, this.getDefault_DIR_DataCache());
			mProperties.setProperty( PROPERTY_DIR_Plots, this.getDefault_DIR_Plots());
			mProperties.setProperty( PROPERTY_DISPLAY_IconSize, this.getDefault_DISPLAY_IconSize());
			mProperties.setProperty( PROPERTY_DISPLAY_MarginBottom, "32");
			mProperties.setProperty( PROPERTY_DISPLAY_ShowSplashScreen, "Yes");
			mProperties.setProperty( PROPERTY_MAX_RecentCount, "50");
			mProperties.setProperty( PROPERTY_MAX_ViewCharacters, "100000");
			mProperties.setProperty( PROPERTY_MAX_TableRows, "10000");
		    mProperties.setProperty( PROPERTY_MAIL_Host, FEEDBACK_Default_MailHost );
		    mProperties.setProperty( PROPERTY_MAIL_Port, FEEDBACK_Default_MailPort );
		    mProperties.setProperty( PROPERTY_FEEDBACK_EmailAddress, FEEDBACK_Default_EmailAddress );
		    mProperties.setProperty( PROPERTY_FEEDBACK_BugHost, FEEDBACK_Default_BugHost );
			mProperties.setProperty( PROPERTY_FEEDBACK_BugzillaRoot, FEEDBACK_Default_BugzillaRoot );
			mProperties.setProperty( PROPERTY_INTERPROCESS_SERVER_Port, "31870");
			mProperties.setProperty( PROPERTY_INTERPROCESS_SERVER_On, "Yes");
			mProperties.setProperty( PROPERTY_DISPLAY_ShowStandardOut, this.getDefault_DISPLAY_ShowStandardOut() ? "Yes" : "No" );
			mProperties.setProperty( PROPERTY_DISPLAY_ShowPopupCancel, "Yes");
			mProperties.setProperty( PROPERTY_DISPLAY_ShowViewTab, "Yes");
			mProperties.setProperty( PROPERTY_DISPLAY_ShowErrorPopups, "Yes");
			mProperties.setProperty( PROPERTY_DISPLAY_AllowPlotterFiles, "No");
			mProperties.setProperty( PROPERTY_OUTPUT_DodsFormat, "ASCII");
			mProperties.setProperty( PROPERTY_MAX_DirectoryCount, "100");
			mProperties.setProperty( PROPERTY_MAX_DirectoryFiles, "10000");
			mProperties.setProperty( PROPERTY_MAX_DirectoryDepth, "10");
			mProperties.setProperty( PROPERTY_TIMEOUT_StatusMessage, "20000");
			mProperties.setProperty( PROPERTY_TIMEOUT_InternetConnect, "20");
			mProperties.setProperty( PROPERTY_TIMEOUT_InternetRead, "10");
			mProperties.setProperty( PROPERTY_COUNT_Plots, "0");
		    if( zStore ) vStoreProperties();
		} else { // load existing file
			try {
				mProperties.load(fisProperties);
			} catch(Exception ex) {
				ApplicationController.vShowError("Failed to refresh configuration: " + ex);
			}
		}
		try { if( fisProperties != null ) fisProperties.close(); } catch( Exception ex ){}
	}

	private void vStoreProperties(){
		java.io.FileOutputStream fos = null;
		String sPath = this.getPath_Properties();
		try {
			fos = new java.io.FileOutputStream(sPath);
		} catch( Exception ex ){
			ApplicationController.vShowWarning("Failed to store properties file [" + sPath + "]: " + ex);
			return;
		}
		try {
			mProperties.store(fos, PROPERTIES_HEADER);
		} catch( Exception ex ){
			ApplicationController.vShowError("Failed to store file [" + sPath + "]: " + ex);
		}
		try { if( fos != null ) fos.close(); } catch( Exception ex ){}
	}
}


