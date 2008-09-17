package opendap.clients.odc;

/**
 * Title:        Configuration Manager
 * Description:  Maintains preferential settings for the application
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

import java.io.File;
import java.util.Properties;
import java.util.ArrayList;
import java.io.FilenameFilter;

public class ConfigurationManager {

	final static int DEFAULT_timeout_InternetConnect = 20;
	final static int DEFAULT_timeout_InternetRead = 60;
	final static int DEFAULT_editing_TabSize = 4;
	final static int DEFAULT_editing_ColumnCount = 80;

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
	private static final String URL_Default_GCMD = "http://gcmd.nasa.gov/OpenAPI/";
	private static final String URL_Default_GCMD_old = "http://gcmd.nasa.gov/servlets/md/";

	private static final String DIR_Default_ImageCache = "ImageCache";
	private static final String DIR_Default_DataCache = "DataCache";
	private static final String DIR_Default_Plots = "plots";
	private static final String DIR_Default_Scripts = "scripts";
	private static final String DIR_Default_Coastline = "coastline";

	private static final String FEEDBACK_Default_EmailRelayURL = "/cgi-bin/odc_mail_relay.pl"; // http://test.opendap.org/cgi-bin/odc_mail_relay.pl
	private static final String FEEDBACK_Default_MailHost = "test.opendap.org";
	private static final String FEEDBACK_Default_MailPort = "80";
	private static final String FEEDBACK_Default_EmailAddress = "feedback@opendap.org";
	private static final String FEEDBACK_Default_BugHost = "scm.opendap.org";
	private static final String FEEDBACK_Default_BugPort = "8090";
	private static final String FEEDBACK_Default_BugRoot = "/trac";

	private static final String EDITING_Default_TabSize = "4";
	private static final String EDITING_Default_ColumnCount = "80";

	ArrayList<String> mlistProperties;

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
	public static final String PROPERTY_DIR_Scripts = "dir.Scripts";
	public static final String PROPERTY_PROXY_Use = "proxy.Use";
	public static final String PROPERTY_PROXY_Host = "proxy.Host";
	public static final String PROPERTY_PROXY_Port = "proxy.Port";
	public static final String PROPERTY_PROXY_UseBasicAuthentication = "proxy.UseBasicAuthentication";
	public static final String PROPERTY_PROXY_Username = "proxy.Username";
	public static final String PROPERTY_PROXY_Password = "proxy.Password";
	public static final String PROPERTY_MAIL_Host = "mail.Host";
	public static final String PROPERTY_MAIL_Port = "mail.Port";
	public static final String PROPERTY_FEEDBACK_EmailRelayURL = "feedback.EmailRelayURL";
	public static final String PROPERTY_FEEDBACK_EmailAddress = "feedback.EmailAddress";
	public static final String PROPERTY_FEEDBACK_EmailUserAddress = "feedback.EmailUserAddress";
	public static final String PROPERTY_FEEDBACK_BugHost = "feedback.BugHost";
	public static final String PROPERTY_FEEDBACK_BugPort = "feedback.BugPort";
	public static final String PROPERTY_FEEDBACK_BugRoot = "feedback.BugRoot";
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
	public static final String PROPERTY_DISPLAY_StartupLocation_X = "display.StartupLocation_X";
	public static final String PROPERTY_DISPLAY_StartupLocation_Y = "display.StartupLocation_Y";
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
	public static final String PROPERTY_EDITING_TabSize = "editing.TabSize";
	public static final String PROPERTY_EDITING_ColumnCount = "editing.ColumnCount";
	public static final String PROPERTY_EDITING_LineWrap = "editing.LineWrap";
	public static final String PROPERTY_EDITING_WrapByWords = "editing.WrapByWords";

	private String msBaseDirectoryPath = null; // sacred object

	private boolean mzReadOnly = true;

	private Properties mProperties;

	private boolean mzMacEnvironment = false;

    public static ConfigurationManager getInstance(){return thisSingleton;}

	private ConfigurationManager(){}

	private boolean mzInitialized = false;

    public static boolean zInitializeConfigurationManager( String sBaseDirectoryPath, StringBuffer sbError ){

		thisSingleton = new ConfigurationManager();

		File fileCurrentDirectory = new File(".");
		if( fileCurrentDirectory == null ){
			ApplicationController.vShowStatus( "Warning: unable to determine current directory" );
		} else {
			ApplicationController.vShowStatus( "Current Directory: " + fileCurrentDirectory.getAbsolutePath() );
		}

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

		// default to working directory if no base directory is specified by the user
		if( sBaseDirectoryPath == null || sBaseDirectoryPath.length() == 0){ // use the working directory
			sBaseDirectoryPath = System.getProperty("user.dir");
			if( sBaseDirectoryPath == null ){
				sbError.append("no base directory path supplied or working directory available");
				return false;
			}
		} else { // there is a directory specified by the user

			// validate base directory specified by the user
			if( sBaseDirectoryPath != null && sBaseDirectoryPath.length() > 0 ){
				StringBuffer sbValidationError = new StringBuffer(250);
				if( ! Utility.zDirectoryValidate( sBaseDirectoryPath, sbValidationError ) ){
					ApplicationController.vShowWarning("Invalid command line-supplied base directory: " + sbValidationError);
				}
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
			String s = getInstance().getOption(PROPERTY_MODE_ReadOnly);
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
		mlistProperties = new ArrayList<String>();
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
		mlistProperties.add( PROPERTY_PROXY_UseBasicAuthentication );
		mlistProperties.add( PROPERTY_PROXY_Username );
		mlistProperties.add( PROPERTY_PROXY_Password );
		mlistProperties.add( PROPERTY_MAIL_Host );
		mlistProperties.add( PROPERTY_MAIL_Port );
		mlistProperties.add( PROPERTY_FEEDBACK_EmailRelayURL );
		mlistProperties.add( PROPERTY_FEEDBACK_EmailAddress );
		mlistProperties.add( PROPERTY_FEEDBACK_EmailUserAddress );
		mlistProperties.add( PROPERTY_FEEDBACK_BugHost );
		mlistProperties.add( PROPERTY_FEEDBACK_BugPort );
		mlistProperties.add( PROPERTY_FEEDBACK_BugRoot );
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
		mlistProperties.add( PROPERTY_DISPLAY_StartupLocation_X );
		mlistProperties.add( PROPERTY_DISPLAY_StartupLocation_Y );
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
		mlistProperties.add( PROPERTY_EDITING_TabSize );
	}


	public String getBaseDirectory(){
		return msBaseDirectoryPath;
	}
	public boolean getIsMacEnvironment(){ return mzMacEnvironment; }
	public String getProperty_URL_GCMD(){
		String sGCMD_URL = getInstance().getOption(PROPERTY_URL_GCMD, getDefault_URL_GCMD());
		if( sGCMD_URL.equalsIgnoreCase( ConfigurationManager.URL_Default_GCMD_old ) ){
			getInstance().setOption( PROPERTY_URL_GCMD, ConfigurationManager.URL_Default_GCMD );
			return ConfigurationManager.URL_Default_GCMD; // default URL changed
		} else {
			return sGCMD_URL;
		}
	}
	public String getProperty_URL_ECHO(){ return getInstance().getOption(PROPERTY_URL_ECHO, getDefault_URL_ECHO()); }
	public String getProperty_URL_XML(){ return getInstance().getOption(PROPERTY_URL_XML, getDefault_URL_DatasetList()); }
	public String getProperty_PATH_XML_Cache(){ return getInstance().getOption(PROPERTY_PATH_XML_Cache, this.getDefault_PATH_XML()); }
	public String getProperty_PATH_XML_ECHO_Valids(){ return getInstance().getOption(PROPERTY_PATH_ECHO_Valids, this.getDefault_PATH_ECHO_Valids()); }
	public String getProperty_PATH_Gazetteer(){ return getInstance().getOption(PROPERTY_PATH_Gazetteer, this.getDefault_PATH_Gazetteer()); }
	public String getProperty_PATH_Coastline(){ return getInstance().getOption(PROPERTY_PATH_Coastline, this.getDefault_PATH_Coastline()); }
	public String getProperty_DIR_ImageCache(){ return getInstance().getOption(PROPERTY_DIR_ImageCache, this.getDefault_DIR_ImageCache()); }
	public String getProperty_DIR_DataCache(){ return getInstance().getOption(PROPERTY_DIR_DataCache, this.getDefault_DIR_DataCache()); }
	public String getProperty_DIR_Plots(){ return getInstance().getOption(PROPERTY_DIR_Plots, this.getDefault_DIR_Plots()); }
	public String getProperty_DIR_Scripts(){ return getInstance().getOption(PROPERTY_DIR_Scripts, this.getDefault_DIR_Scripts()); }
	public String getProperty_DISPLAY_IconSize(){ return getInstance().getOption(PROPERTY_DISPLAY_IconSize, "16"); }
	public String getProperty_EDITING_TabSize(){ return getInstance().getOption(PROPERTY_EDITING_TabSize, "4"); }
	public boolean getProperty_MODE_ReadOnly(){
		return mzReadOnly;
	}
	public boolean getProperty_DISPLAY_ShowStandardOut(){
		String s = getInstance().getOption(PROPERTY_DISPLAY_ShowStandardOut);
		if( s == null ){
			return getDefault_DISPLAY_ShowStandardOut();
		}
		if( s.toUpperCase().startsWith("Y") || s.toUpperCase().equals("TRUE")) return true;
		return false;
	}
	public boolean getProperty_DISPLAY_ShowViewTab(){
		String s = getInstance().getOption(PROPERTY_DISPLAY_ShowViewTab);
		if( s == null ){
			return true; // default
		}
		if( s.toUpperCase().startsWith("Y") || s.toUpperCase().equals("TRUE") ) return true;
		return false;
	}
	public boolean getProperty_DISPLAY_AllowPlotterFiles(){
		String s = getInstance().getOption(PROPERTY_DISPLAY_AllowPlotterFiles);
		if( s == null ){
			return false; // default
		}
		if( s.toUpperCase().startsWith("Y") || s.toUpperCase().equals("TRUE") ) return true;
		return false;
	}
	public boolean getProperty_DISPLAY_ShowErrorPopups(){
		String s = getInstance().getOption(PROPERTY_DISPLAY_ShowErrorPopups);
		if( s == null ){
			return true; // default
		}
		if( s.toUpperCase().startsWith("Y") || s.toUpperCase().equals("TRUE") ) return true;
		return false;
	}
	public boolean getProperty_LOGGING_ShowHeaders(){
		String s = getInstance().getOption(PROPERTY_LOGGING_ShowHeaders);
		if( s == null ){
			return false; // default
		}
		if( s.toUpperCase().startsWith("Y") || s.toUpperCase().equals("TRUE") ) return true;
		return false;
	}
	public boolean getProperty_LOGGING_ReportMetrics(){
		String s = getInstance().getOption(PROPERTY_LOGGING_ReportMetrics);
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
		String s = getInstance().getOption(PROPERTY_OUTPUT_DodsFormat);
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
		String sMarginRight = getInstance().getOption(PROPERTY_DISPLAY_MarginRight, "0");
		int iMarginRight = 0;
		try {
			iMarginRight = Integer.parseInt(sMarginRight);
			if( iMarginRight < 0 ) iMarginRight *= -1;
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid right margin setting [" + sMarginRight + "]. Must be an integer.");
		}
		return iMarginRight;
	}

	public int getProperty_MarginBottom(){
		String sMarginBottom = getInstance().getOption(PROPERTY_DISPLAY_MarginBottom, "32");
		int iMarginBottom = 32;
		try {
			iMarginBottom = Integer.parseInt(sMarginBottom);
			if( iMarginBottom < 0 ) iMarginBottom *= -1;
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid bottom margin setting [" + sMarginBottom + "]. Must be an integer.");
		}
		return iMarginBottom;
	}

	public int getProperty_StartupSize_Width(){
		String sStartupSize_Width = getInstance().getOption(PROPERTY_DISPLAY_StartupSize_Width, "0");
		int iStartupSize_Width = 0;
		try {
			iStartupSize_Width = Integer.parseInt(sStartupSize_Width);
			if( iStartupSize_Width < 0 ) iStartupSize_Width *= -1;
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid startup size width setting [" + sStartupSize_Width + "]. Must be an integer.");
		}
		return iStartupSize_Width;
	}

	public int getProperty_StartupSize_Height(){
		String sStartupSize_Height = getInstance().getOption(PROPERTY_DISPLAY_StartupSize_Height, "0");
		int iStartupSize_Height = 0;
		try {
			iStartupSize_Height = Integer.parseInt(sStartupSize_Height);
			if( iStartupSize_Height < 0 ) iStartupSize_Height *= -1;
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid startup size Height setting [" + sStartupSize_Height + "]. Must be an integer.");
		}
		return iStartupSize_Height;
	}

	public int getProperty_StartupLocation_X(){
		String sStartupLocation_X = getInstance().getOption(PROPERTY_DISPLAY_StartupLocation_X, "0");
		int iStartupLocation_X = 0;
		try {
			iStartupLocation_X = Integer.parseInt(sStartupLocation_X);
			if( iStartupLocation_X < 0 ) iStartupLocation_X *= -1;
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid startup location X setting [" + sStartupLocation_X + "]. Must be an integer.");
		}
		return iStartupLocation_X;
	}

	public int getProperty_StartupLocation_Y(){
		String sStartupLocation_Y = getInstance().getOption(PROPERTY_DISPLAY_StartupLocation_Y, "0");
		int iStartupLocation_Y = 0;
		try {
			iStartupLocation_Y = Integer.parseInt(sStartupLocation_Y);
			if( iStartupLocation_Y < 0 ) iStartupLocation_Y *= -1;
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid startup location X setting [" + sStartupLocation_Y + "]. Must be an integer.");
		}
		return iStartupLocation_Y;
	}

	public int getProperty_RecentCount(){
		String sRecentCount = getInstance().getOption(PROPERTY_MAX_RecentCount, "50");
		int iRecentCount = 50;
		try {
			iRecentCount = Integer.parseInt(sRecentCount);
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid recent count setting [" + sRecentCount + "]. Must be an integer.");
		}
		return iRecentCount;
	}
	public int getProperty_MaxTableRows(){
		String sMax = getInstance().getOption(PROPERTY_MAX_TableRows, "10000");
		int iMax = 10000;
		try {
			iMax = Integer.parseInt(sMax);
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid max table rows setting [" + sMax + "]. Must be an integer.");
		}
		return iMax;
	}
	public int getProperty_MaxViewCharacters(){
		String sMaxViewCharacters = getInstance().getOption(PROPERTY_MAX_ViewCharacters, "100000");
		int iMaxViewCharacters = 100000;
		try {
			iMaxViewCharacters = Integer.parseInt(sMaxViewCharacters);
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid max view characters setting [" + sMaxViewCharacters + "]. Must be an integer.");
		}
		return iMaxViewCharacters;
	}
	public int getProperty_DirectoryCount(){
		int iMaxDirectoryCount = 100; // default
		String sMaxDirectoryCount = getInstance().getOption(PROPERTY_MAX_DirectoryCount, Integer.toString(iMaxDirectoryCount));
		try {
			iMaxDirectoryCount = Integer.parseInt(sMaxDirectoryCount);
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid max directory count setting [" + sMaxDirectoryCount + "]. Must be an integer.");
		}
		return iMaxDirectoryCount;
	}
	public int getProperty_DirectoryFiles(){
		int iMaxDirectoryFiles = 10000; // default
		String sMaxDirectoryFiles = getInstance().getOption(PROPERTY_MAX_DirectoryFiles, Integer.toString(iMaxDirectoryFiles));
		try {
			iMaxDirectoryFiles = Integer.parseInt(sMaxDirectoryFiles);
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid max directory files setting [" + sMaxDirectoryFiles + "]. Must be an integer.");
		}
		return iMaxDirectoryFiles;
	}
	public int getProperty_DirectoryDepth(){
		int iMaxDirectoryDepth = 10; // default
		String sMaxDirectoryDepth = getInstance().getOption(PROPERTY_MAX_DirectoryDepth, Integer.toString(iMaxDirectoryDepth));
		try {
			iMaxDirectoryDepth = Integer.parseInt(sMaxDirectoryDepth);
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid max directory depth setting [" + sMaxDirectoryDepth + "]. Must be an integer.");
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
	public boolean getProperty_ProxyUseBasicAuthentication(){
		String s = getInstance().getOption(PROPERTY_PROXY_UseBasicAuthentication);
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
	public String getProperty_ProxyUsername(){
		return getInstance().getOption(PROPERTY_PROXY_Username, null);
	}
	public String getProperty_ProxyPassword(){
		return getInstance().getOption(PROPERTY_PROXY_Password, null);
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
			ApplicationController.vShowWarning("Invalid mail server port setting [" + sPort + "]. Must be an integer.");
		}
		return iMailServerPort;
	}
	public String getProperty_FEEDBACK_EmailRelayURL(){
		return getInstance().getOption(PROPERTY_FEEDBACK_EmailRelayURL, FEEDBACK_Default_EmailRelayURL);
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
	public int getProperty_FEEDBACK_BugPort(){
		String sPort = getInstance().getOption(PROPERTY_FEEDBACK_BugPort, FEEDBACK_Default_BugPort);
		int iBugPort = 8090;
		try {
			iBugPort = Integer.parseInt(sPort);
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid bug server port setting [" + sPort + "]. Must be an integer.");
		}
		return iBugPort;
	}
	public String getProperty_FEEDBACK_BugRoot(){
		return getInstance().getOption(PROPERTY_FEEDBACK_BugRoot, FEEDBACK_Default_BugRoot);
	}
	public int getProperty_InterprocessServerPort(){
		String sInterprocessServerPort = getInstance().getOption(PROPERTY_INTERPROCESS_SERVER_Port, "31870");
		int iInterprocessServerPort = 31870;
		try {
			iInterprocessServerPort = Integer.parseInt(sInterprocessServerPort);
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid interprocess server port setting [" + sInterprocessServerPort + "]. Must be an integer.");
		}
		return iInterprocessServerPort;
	}
	public boolean getProperty_InterprocessServerOn(){
		String sInterprocessServerOn = getInstance().getOption(PROPERTY_INTERPROCESS_SERVER_On, "Yes");
		if( sInterprocessServerOn == null ) return true;
		if( sInterprocessServerOn.startsWith("y") || sInterprocessServerOn.startsWith("Y") ) return true;
		if( sInterprocessServerOn.equalsIgnoreCase("true") ) return true;
		return false;
	}
	public boolean getProperty_DISPLAY_ShowSplashScreen(){
		String sValue = getInstance().getOption(PROPERTY_DISPLAY_ShowSplashScreen, "Yes");
		if( sValue == null ) return true;
		if( sValue.length() == 0 ) return true;
		if( sValue.toUpperCase().startsWith("Y") ) return true;
		return false;
	}
	public boolean getProperty_DISPLAY_ShowPopupCancel(){
		String sValue = getInstance().getOption(PROPERTY_DISPLAY_ShowPopupCancel, "Yes");
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
		String sStatusTimeout = getInstance().getOption(PROPERTY_TIMEOUT_StatusMessage, "20000");
		int iStatusTimeout = 20000;
		try {
			iStatusTimeout = Integer.parseInt(sStatusTimeout);
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid status message timeout setting [" + sStatusTimeout + "]. Must be an integer.");
		}
		return iStatusTimeout;
	}
	public int getProperty_Timeout_InternetConnect(){
		String sTimeout = getInstance().getOption(PROPERTY_TIMEOUT_InternetConnect, Integer.toString(DEFAULT_timeout_InternetConnect));
		int iTimeout = DEFAULT_timeout_InternetConnect;
		try {
			iTimeout = Integer.parseInt(sTimeout);
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid status message timeout setting [" + sTimeout + "]. Must be an integer in seconds.");
		}
		return iTimeout;
	}
	public int getProperty_Timeout_InternetRead(){
		String sTimeout = getInstance().getOption(PROPERTY_TIMEOUT_InternetRead, Integer.toString(DEFAULT_timeout_InternetRead));
		int iTimeout = DEFAULT_timeout_InternetRead;
		try {
			iTimeout = Integer.parseInt(sTimeout);
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid status message timeout setting [" + sTimeout + "]. Must be an integer in seconds.");
		}
		return iTimeout;
	}
	public int getProperty_Editing_TabSize(){
		String sTimeout = getInstance().getOption(PROPERTY_EDITING_TabSize, EDITING_Default_TabSize );
		int iTabSize = DEFAULT_editing_TabSize;
		try {
			iTabSize = Integer.parseInt(sTimeout);
			if( iTabSize < 0 || iTabSize > 120 ) iTabSize = DEFAULT_editing_TabSize;
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid tab setting [" + iTabSize + "]. Must be an integer between 0 and 120.");
		}
		return iTabSize;
	}
	public int getProperty_Editing_ColumnCount(){
		String sTimeout = getInstance().getOption(PROPERTY_EDITING_ColumnCount, EDITING_Default_ColumnCount );
		int iColumnCount = DEFAULT_editing_ColumnCount;
		try {
			iColumnCount = Integer.parseInt(sTimeout);
			if( iColumnCount < 0 || iColumnCount > 10000 ) iColumnCount = DEFAULT_editing_ColumnCount;
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Invalid column count setting [" + iColumnCount + "]. Must be an integer between 0 and 10000.");
		}
		return iColumnCount;
	}
	public boolean getProperty_Editing_LineWrap(){
		String s = getInstance().getOption(PROPERTY_EDITING_LineWrap);
		if( s == null ){
			return true; // default
		}
		if( s.toUpperCase().startsWith("Y") || s.toUpperCase().equals("TRUE") ) return true;
		return false;
	}
	public boolean getProperty_Editing_WrapByWords(){
		String s = getInstance().getOption(PROPERTY_EDITING_WrapByWords);
		if( s == null ){
			return true; // default
		}
		if( s.toUpperCase().startsWith("Y") || s.toUpperCase().equals("TRUE") ) return true;
		return false;
	}
	public int getProperty_PlotCount(){
		String sStatusTimeout = getInstance().getOption(PROPERTY_COUNT_Plots, "0");
		try {
			int iPlotCount = Integer.parseInt(sStatusTimeout);
			if( iPlotCount >= 0 ) return iPlotCount;
		} catch(Exception ex) {}
		ApplicationController.vShowWarning("Invalid plot count setting [" + sStatusTimeout + "]. Must be a non-negative integer.");
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

	String getDefault_DIR_Scripts(){
		String sWorkingDirectory = this.getBaseDirectory();
		if( sWorkingDirectory == null ) sWorkingDirectory = "";
		String sSeparator = System.getProperty(SYSTEM_PROPERTY_FileSeparator);
		String sPlotsDirectory = Utility.sConnectPaths(sWorkingDirectory, sSeparator, DIR_Default_Scripts) + sSeparator;
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
			if( zStore ){
				vStoreProperties();
			}
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
		sb.append(PROPERTY_DIR_Scripts + " = " + this.getProperty_DIR_Scripts() + "\n" );
		sb.append(PROPERTY_PROXY_Use + " = " + (this.getProperty_ProxyUse() ? "Yes" : "No") + "\n" );
		sb.append(PROPERTY_PROXY_Host + " = " + (this.getProperty_ProxyHost()==null ? "[none]" : this.getProperty_ProxyHost()) + "\n" );
		sb.append(PROPERTY_PROXY_Port + " = " + (this.getProperty_ProxyPort()==null ? "[none]" : this.getProperty_ProxyPort()) + "\n" );
		sb.append(PROPERTY_PROXY_UseBasicAuthentication + " = " + (this.getProperty_ProxyUseBasicAuthentication() ? "Yes" : "No") + "\n" );
		sb.append(PROPERTY_PROXY_Username + " = " + (this.getProperty_ProxyUsername()==null ? "[none]" : this.getProperty_ProxyUsername()) + "\n" );
		sb.append(PROPERTY_PROXY_Password + " = " + (this.getProperty_ProxyPassword()==null ? "[none]" : this.getProperty_ProxyPassword()) + "\n" );
		sb.append(PROPERTY_MAIL_Host + " = " + (getProperty_MailHost()==null ? "[none]" : getProperty_MailHost()) + "\n" );
		sb.append(PROPERTY_MAIL_Port + " = " + (getProperty_MailPort() < 0 ? "[none]" : Integer.toString(getProperty_MailPort())) + "\n" );
		sb.append(PROPERTY_FEEDBACK_EmailRelayURL + " = " + (this.getProperty_FEEDBACK_EmailRelayURL()==null ? "[none (default is " + FEEDBACK_Default_EmailRelayURL + ")]" : this.getProperty_FEEDBACK_EmailRelayURL()) + "\n" );
		sb.append(PROPERTY_FEEDBACK_EmailAddress + " = " + (this.getProperty_FEEDBACK_EmailAddress()==null ? "[none (default is " + FEEDBACK_Default_EmailAddress + ")]" : this.getProperty_FEEDBACK_EmailAddress()) + "\n" );
		sb.append(PROPERTY_FEEDBACK_EmailUserAddress + " = " + (this.getProperty_FEEDBACK_EmailUserAddress()==null ? "[none]" : this.getProperty_FEEDBACK_EmailAddress()) + "\n" );
		sb.append(PROPERTY_FEEDBACK_BugHost + " = " + (this.getProperty_FEEDBACK_BugHost()==null ? "[none (default is " + FEEDBACK_Default_BugHost + ")]" : this.getProperty_FEEDBACK_BugHost()) + "\n" );
		sb.append(PROPERTY_FEEDBACK_BugPort + " = " + (this.getProperty_FEEDBACK_BugPort() < 0 ? "[none (default is " + FEEDBACK_Default_BugPort + ")]" : "" + this.getProperty_FEEDBACK_BugPort()) + "\n" );
		sb.append(PROPERTY_FEEDBACK_BugRoot + " = " + (this.getProperty_FEEDBACK_BugRoot()==null ? "[none (default is " + FEEDBACK_Default_BugRoot + ")]" : this.getProperty_FEEDBACK_BugRoot()) + "\n" );
		sb.append(PROPERTY_INTERPROCESS_SERVER_Port + " = " + this.getProperty_InterprocessServerPort() + "\n" );
		sb.append(PROPERTY_INTERPROCESS_SERVER_On + " = " + (this.getProperty_InterprocessServerOn() ? "Yes" : "No") + "\n" );
		sb.append(PROPERTY_DISPLAY_IconSize + " = " + this.getProperty_DISPLAY_IconSize() + "\n" );
		sb.append(PROPERTY_DISPLAY_MarginBottom + " = " + this.getProperty_MarginBottom() + "\n" );
		sb.append(PROPERTY_DISPLAY_MarginRight + " = " + this.getProperty_MarginRight() + "\n" );
		sb.append(PROPERTY_DISPLAY_StartupSize_Width + " = " + this.getProperty_StartupSize_Width() + "\n" );
		sb.append(PROPERTY_DISPLAY_StartupSize_Height + " = " + this.getProperty_StartupSize_Height() + "\n" );
		sb.append(PROPERTY_DISPLAY_StartupLocation_X + " = " + this.getProperty_StartupLocation_X() + "\n" );
		sb.append(PROPERTY_DISPLAY_StartupLocation_Y + " = " + this.getProperty_StartupLocation_Y() + "\n" );
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
		sb.append(PROPERTY_EDITING_TabSize + " = " + this.getProperty_Editing_TabSize() + "\n" );
		sb.append(PROPERTY_EDITING_ColumnCount + " = " + this.getProperty_Editing_ColumnCount() + "\n" );
		sb.append(PROPERTY_EDITING_LineWrap + " = " + this.getProperty_Editing_LineWrap() + "\n" );
		sb.append(PROPERTY_EDITING_WrapByWords + " = " + this.getProperty_Editing_WrapByWords() + "\n" );
		return sb.toString();
	}

	void vRefresh( boolean zStore ){
		mProperties = new Properties();
		java.io.FileInputStream fisProperties = null;
		String sPath = getPath_Properties();
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
			mProperties.setProperty( PROPERTY_DIR_Scripts, this.getDefault_DIR_Scripts());
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
		    mProperties.setProperty( PROPERTY_FEEDBACK_BugPort, FEEDBACK_Default_BugPort );
			mProperties.setProperty( PROPERTY_FEEDBACK_BugRoot, FEEDBACK_Default_BugRoot );
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
			mProperties.setProperty( PROPERTY_EDITING_TabSize, "4");
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

	static File[] getPreferencesFiles(FilenameFilter filter){
		String sPreferencesDirectory = ConfigurationManager.getInstance().getProperty_PreferencesDirectory();
		File filePreferencesDirectory = new File( sPreferencesDirectory );
		if( filePreferencesDirectory == null ){
			ApplicationController.vShowError("attempt to resolve preferences directory (" + sPreferencesDirectory + ") unexpectedly had a null result" );
			return null;
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

}


