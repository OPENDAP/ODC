package opendap.clients.odc.data;

/**
 * A <code>DodsURL</code> stores information about a Dods URL.  More
 * specifially, it stores the base url, the constraint expression, the
 * type of URL, and what class is need to get further information from
 * the URL.  It should be used over <code>String</code> to represent
 * Dods URLs whenever possible.
 *
 * @author rhonhart (original version, many moons ago)
 * @author John S. Chamberlain
 */

/////////////////////////////////////////////////////////////////////////////
// This file is part of the OPeNDAP Data Connector project.
//
// Copyright (c) 2007-2010 OPeNDAP, Inc.
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

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Catalog;
import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.DAP;
import opendap.clients.odc.SavableImplementation;
import opendap.clients.odc.plot.Plot_Definition;
import opendap.clients.odc.Utility;
import opendap.clients.odc.Utility_String;
import opendap.dap.*;
import java.util.Enumeration;
import java.io.FilenameFilter;
import java.io.File;

import opendap.dap.BaseType;

public class Model_Dataset implements java.io.Serializable {

	// todo delete fav/recent files that have bad serialization
	private static final long serialVersionUID = 2L;

//	public final static int TYPE_Data = 0;
//	public final static int TYPE_Definition = 1;
//	public final static int TYPE_Directory = 2;
//	public final static int TYPE_Catalog = 3;
//	public final static int TYPE_Image = 4;
//	public final static int TYPE_HTML = 5;
//	public final static int TYPE_Text = 6;
//	public final static int TYPE_Binary = 7;
//	public final static int TYPE_Expression = 8;
//	public final static int TYPE_Stream = 9;
//    
	public enum DATASET_TYPE {
		Data,
		Definition,
		Directory,
		Catalog,
		Image,
		HTML,
		Text,
		Binary,
		PlottableExpression,
		DataScript,
		Stream
    };

    private DATASET_TYPE meType;
    private String msURL;
    private String msCE;
	private String msSubDirectory; // with no leading slash
	private String msDirectoryRegex;
    private String msTitle;
	private int miDigest = 0;
	private String msMIMEType;

	private String msDDS_Text;
	private String msDAS_Text;
	private String msDDX_Text;
	private String msInfo_Text;
	private String msScript_Text;

	transient private boolean mzUnreachable = false;
	transient private String msError; // if the site is unreachable

	private DDS mDDS_Subset;
	private DDS mDDS_Full;
	private DAS mDAS; // this is used in case of a catalog
	transient private Catalog mCatalog;
	transient private Model_DirectoryTree mDirectoryTree; // this is used in the case of a directory
	transient private DataDDS mDataDDS;
	transient private SavableImplementation mSavable;
	transient private Plot_Definition mPlotDefinition = new Plot_Definition();

	transient int miID; // used to tag an URL with an arbitrary id such as for favorites and recent

	private Model_Dataset(){} // prevent external creation of class, users must employ factory constructors
	
    /**
     * Create a new <code>Model_Dataset</code> of the data type.
     */
	public final static Model_Dataset createData( final DataDDS data, final StringBuffer sbError ){
		String sTextDDS = data.getDDSText();
    	DDS dds = DAP.getDDSforText( sTextDDS, sbError );
    	if( dds == null ){
    		sbError.insert( 0, "unable to generate DDS from text: " );
    		return null;
    	}
    	Model_Dataset model = new Model_Dataset();
		model.msURL = "";
		model.msCE = "";
		model.meType = DATASET_TYPE.Data;
		model.msTitle = null;
		model.msMIMEType = null;
		model.mSavable = new SavableImplementation( Model_Dataset.class, null, null );
		model.mDataDDS = data;
		model.mDDS_Full = dds;
		model.msDDS_Text = dds.getDDSText();
		return model;
    }

    /**
     * Create a <code>DodsURL</code> with a specific base URL of type
     * <code>type</code>.  The constraint expression is set to an empty string
     * and the urlProcessor is set to the default for the give type.
     * @param dodsURL The base url.
     * @param type The type of URL.
     */
    public final static Model_Dataset createDataFromURL( final String sURL, StringBuffer sbError ){
    	if( sURL == null ){
    		sbError.append( "no URL supplied" );
    		return null;
    	}
    	Model_Dataset model = new Model_Dataset();
		model.msURL = sURL;
		model.msCE = "";
		model.meType = DATASET_TYPE.Data;
		model.msTitle = null;
		model.msMIMEType = null;
		model.mSavable = new SavableImplementation( Model_Dataset.class, null, null );
		return model;
    }

    /**
     * Create a <code>DodsURL model</code> of the Directory type.
     * The constraint expression is set to an empty string
     * and the urlProcessor is set to the default for the give type.
     * @param dodsURL The base url.
     * @param type The type of URL.
     */
    public final static Model_Dataset createDirectoryFromURL( final String sURL, StringBuffer sbError ){
    	if( sURL == null ){
    		sbError.append( "no URL supplied" );
    		return null;
    	}
    	Model_Dataset model = new Model_Dataset();
		model.msURL = sURL;
		model.msCE = "";
		model.meType = DATASET_TYPE.Directory;
		model.msTitle = null;
		model.msMIMEType = null;
		model.mSavable = new SavableImplementation( Model_Dataset.class, null, null );
		return model;
    }
    
    /**
     * Create a new <code>Model_Dataset</code> of the data type.
     */
    public final static Model_Dataset createExpression( final StringBuffer sbError ){
    	Model_Dataset model = new Model_Dataset();
		model.meType = DATASET_TYPE.PlottableExpression;
		model.msScript_Text = "";
		model.msURL = "";
		model.msCE = "";
		model.mDDS_Full = null;
		model.msDDS_Text = null;
		model.msTitle = null;
		model.msMIMEType = null;
		model.mSavable = new SavableImplementation( Model_Dataset.class, null, null );
		return model;
    }

    public final static Model_Dataset createDataScript( final StringBuffer sbError ){
    	Model_Dataset model = new Model_Dataset();
		model.meType = DATASET_TYPE.DataScript;
		model.msScript_Text = "";
		model.msURL = "";
		model.msCE = "";
		model.mDDS_Full = null;
		model.msDDS_Text = null;
		model.msTitle = null;
		model.msMIMEType = null;
		model.mSavable = new SavableImplementation( Model_Dataset.class, null, null );
		return model;
    }
    
    public final static Model_Dataset createDataDefinition( final DDS dds, final StringBuffer sbError ){
    	if( dds == null ){
    		sbError.append( "no DDS" );
    		return null;
    	}
    	Model_Dataset model = new Model_Dataset();
		model.meType = DATASET_TYPE.Definition;
		model.msScript_Text = "";
		model.msURL = "";
		model.msCE = "";
		model.mDDS_Full = dds;
		model.msDDS_Text = dds.getDDSText();
		model.msTitle = null;
		model.msMIMEType = null;
		model.mSavable = new SavableImplementation( Model_Dataset.class, null, null );
		return model;
    }
    
    /**
     * Create a new <code>Model_Dataset</code> of the image type.
     */
    public final static Model_Dataset createImageFromURL( final String sURL, final StringBuffer sbError ){
    	Model_Dataset model = new Model_Dataset();
		model.meType = DATASET_TYPE.Image;
		model.msURL = "";
		model.msCE = "";
		model.msTitle = null;
		model.msMIMEType = Utility.getMIMEtype( sURL );
		model.mSavable = new SavableImplementation( Model_Dataset.class, null, null );
		return model;
    }
    
    /**
     * Create a <code>Model_Dataset</code> by copying an existing <code>Model_Dataset</code>
     * @param model The model to copy.
     */
    public final static Model_Dataset createClone( Model_Dataset existing_model ){
    	if( existing_model == null ) return null;
    	Model_Dataset model = new Model_Dataset();
		model.msURL = existing_model.msURL;
		model.msCE = existing_model.msCE;
		model.msSubDirectory = existing_model.msSubDirectory; // with no leading slash
		model.msDirectoryRegex = existing_model.msDirectoryRegex;
		model.meType = existing_model.meType;
		model.msTitle = existing_model.msTitle;
		model.miDigest = existing_model.miDigest;
		model.msMIMEType = existing_model.msMIMEType;
		model.msDDS_Text= existing_model.msDDS_Text;
		model.msDAS_Text= existing_model.msDAS_Text;
		model.msDDX_Text= existing_model.msDDX_Text;
		model.msInfo_Text= existing_model.msInfo_Text;
		model.msScript_Text= existing_model.msScript_Text;
		model.mDDS_Subset = existing_model.mDDS_Subset;
		model.mDDS_Full = existing_model.mDDS_Full;
		model.mDAS = existing_model.mDAS;
		model.mSavable = new SavableImplementation( Model_Dataset.class, existing_model.msTitle, null );
		return model;
    }

    /** terminate
     * Create a <code>DodsURL</code> with a specific base URL and constraint
     * expression.  This url is assumed to be a TYPE_Data, and it uses the
     * default TYPE_Data processor.
     * @param dodsURL The base url.
     * @param dodsCE The constraint expression.
    public Model_Dataset( String dodsURL, String dodsCE ){
		msURL = dodsURL;
		msCE = dodsCE;

		// It only makes sense to supply a constraint expression for a
		// data URL, so we can assume this is a data URL.
		miURLType = TYPE_Data;
		msTitle = null;
		mSavable = new SavableImplementation( this.getClass(), null, null );
    }
     */

	public boolean equals( Object o ){
		if( this == o ) return true;
	    if( o == null ) return false;
	    if( !( o instanceof Model_Dataset ) ) return false;
		Model_Dataset dataset = (Model_Dataset)o;
		if( msURL == null || dataset.msURL.length() == 0 ){
			return false;
		} else {
			switch( meType ){
				case Data:
				case Definition:
				case Directory:
				case Catalog:
				case HTML:
				case Text:
				case Binary:
				case Stream:
					if( !msURL.equalsIgnoreCase( dataset.msURL ) ) return false;
					if( !msCE.equalsIgnoreCase( dataset.msCE ) ) return false;
					return true;
				case PlottableExpression:
				case DataScript:
					 // consider two expressions to be distinct unless they are the same object
				default:
					return false;
			}
		}
	}

	public String getInfo(){
		StringBuffer sbInfo = new StringBuffer(1000);
		sbInfo.append("Title: " + this.getTitle() + '\n');
		sbInfo.append("Full URL: " + this.getFullURL() + '\n');
		sbInfo.append("URL type: " + this.getTypeString() + '\n' );
		if( this.isUnreachable() ) sbInfo.append( "URL is unreachable: " + this.getError() + "\n" );
		DATASET_TYPE eType = getType();
		if( eType == DATASET_TYPE.Data ){
			if( this.getDDS_Text() != null ) sbInfo.append(this.getDDS_Text());
			if( this.getDAS_Text() != null ) sbInfo.append(this.getDAS_Text());
			if( this.getInfo_Text() != null ) sbInfo.append(this.getInfo_Text());
		}
		if( eType == DATASET_TYPE.Directory ){
			if( this.getDirectoryRegex() != null ) sbInfo.append( this.getDirectoryRegex() + '\n' );
			if( this.getDirectoryTree() == null ){
				sbInfo.append("Directory tree not available");
			} else {
				sbInfo.append( this.getDirectoryTree().getPrintout() );
			}
		}
		if( eType == DATASET_TYPE.Catalog ){
			sbInfo.append("[Additional catalog info not supported]");
		}
		if( eType == DATASET_TYPE.Image ){
			sbInfo.append("[Additional image info not available]");
		}
		return sbInfo.toString();
	}

	public boolean isUnreachable(){ return mzUnreachable; }
	
	public SavableImplementation getSavable(){ return mSavable; }

	public Plot_Definition getPlotDefinition(){ return mPlotDefinition; }
	
	public String getError(){ return msError; }

	public void setUnreachable( boolean zUnreachable, String sError ){
		mzUnreachable = zUnreachable;
		msError = sError;
	}

	public String getSubDirectory(){ return msSubDirectory; }

	public void setSubDirectory(String sSubDirectory){ msSubDirectory = sSubDirectory; }

	public int getDigest(){ return miDigest; }

	public void setDigest(int iNewDigest){ miDigest = iNewDigest; }

	boolean mzErrorDDS = false;

	/** was there an error getting the DDS for this URL? */
	public boolean getDDS_Error(){ return mzErrorDDS; }
	public boolean setDDS_Error(boolean z){ return mzErrorDDS = z; }

	public DDS getDDS_Full(){ return mDDS_Full; }

	public DDS getDDS_Subset(){ return mDDS_Subset; }

	public void setDDS_Full(DDS dds){
		mDDS_Full = dds;
		if( dds != null ) setUnreachable( false, null );
	}

	public void setDDS_Subset(DDS dds){
		mDDS_Subset = dds;
		if( dds != null ) setUnreachable( false, null );
	}

	public DataDDS getData(){ return mDataDDS; }

	public void setData( DataDDS ddds ){ mDataDDS = ddds; }

	public DAS getDAS(){ return mDAS; }

	public void setDAS(DAS das){ mDAS = das; }

	public String getDDS_Text(){ return msDDS_Text; }

	public String getDDX_Text(){ return msDDX_Text; }
	
	public String getServerVersion(){ if( mDataDDS == null ) return null; else return mDataDDS.getServerVersion().getVersionString(); }
	
	public void setDDS_Text(String dds_text){
		msDDS_Text = dds_text;
	}

	public void setDDX_Text(String ddx_text){
		msDDS_Text = ddx_text;
	}
	
	public String getDDS_Name(){ if( mDataDDS == null ) return "[no DataDDS]"; else return mDataDDS.getName(); } 
	
	public String getDAS_Text(){ return msDAS_Text; }

	public void setDAS_Text(String das_text){ msDAS_Text = das_text; }

	public String getInfo_Text(){ return msInfo_Text; }

	public String getExpression_Text(){ return msScript_Text; }
	
	public void setExpression_Text(String expression_text){
		msScript_Text = expression_text;
	}
	
	public String getFileName(){
		String sFullURL = this.getBaseURL();
		if( sFullURL == null ) return "";
		if( Utility.isDodsURL(sFullURL) && sFullURL.toUpperCase().endsWith(".HTML") ){
			sFullURL.substring(0, sFullURL.length()-5); // strip .HTML
		}
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

	public void setInfo_Text(String info_text){
		msInfo_Text = info_text;
	}

	/** one-based array of files */
	public Model_DirectoryTree getDirectoryTree(){ return mDirectoryTree; }

	public String[] getDirectoryFiles(StringBuffer sbError){
		if( mDirectoryTree == null ){
			sbError.append("no directory tree defined");
			return null;
		}
		String sPath = this.getSubDirectory();
		return mDirectoryTree.getDirectoryFiles( sPath, sbError );
	}

	/** one-based array of files */
	public void setDirectoryTree(Model_DirectoryTree dt ){
		mDirectoryTree = dt;
		if( mDirectoryTree != null ) setUnreachable( false, null );
	}

    /**
     * Returns the CE of the DodsURL.
     * @return the CE of the DodsURL.
     */
	public String getConstraintExpression() {
		return msCE;
	}

    /**
     * Returns the encoded CE of the DodsURL.
     * @return the CE of the DodsURL.
     */
	public String getConstraintExpression_Encoded() {
		try {
			// return java.net.URLEncoder.encode( msCE, "UTF-8" ); // standard Java escape
			String sEscapedCE = Utility_String.sEscapeURL(msCE);
			return sEscapedCE;
		} catch(Exception ex) {
			StringBuffer sbError = new StringBuffer("error escaping CE: ");
			ApplicationController.vUnexpectedError(ex, sbError);
			ApplicationController.vShowError(sbError.toString());
			return msCE;
		}
	}

    /**
     * Returns the base URL of the DodsURL.
     * @return the base URL of the DodsURL.
     */
	public String getBaseURL() {
		return msURL;
    }

	/** this form is used when making URLs from Directories */
	public String getFullURL(String sBaseURL){
		if(msCE.length() > 0){
			return sBaseURL + "?" + this.getConstraintExpression_Encoded();
		} else {
			return sBaseURL;
		}
	}

    /**
     * Concatenates the baseURL and the constraint expression to get
     * the full Dods URL.
     * @return a complete Dods URL.
     */
    public String getFullURL() {
		if( this.meType == DATASET_TYPE.Directory && msSubDirectory != null){
			return getFullURL(Utility.sConnectPaths(getBaseURL(), "/", getSubDirectory()));
		} else {
			return getFullURL(getBaseURL());
		}
	}

    /**
     * Returns the title, if any, of the URL.
     * @return the title, if any, of the URL.
     */

    public String getTitle() {
		return msTitle;
    }

    /**
     * Returns the type of the URL.
     * @return the type of the URL.
     */
    public DATASET_TYPE getType() {
		return meType;
    }

    /**
     * Returns the MIME type of the URL.
     * @return the MIME type of the URL.
     */
    public String getMIMEType() {
		return msMIMEType;
    }

    /**
     * Returns the type of the URL as a string
     * @return the type of the URL.
     */
    public String getTypeString() {
    	return meType.toString();
    }

    /**
     * Create a string representation of the URL.
     * @return The full URL.
     */
    public String toString() {
    	String sFileName = getFileName();
    	if( sFileName == null || sFileName.length() == 0 ){
    		if( msTitle == null ){
    			return "[unnamed]";
    		} else return msTitle;
    	} else {
			if( msTitle == null ){
	            return getFileName();
			} else {
				return msTitle + " (" + getFileName() + ")";
			}
    	}
    }

    /**
     * Set the constraint expression for the URL.
     * @param dodsCE The constraint expression.
     */
    public void setConstraintExpression(String dodsCE) {
		msCE = dodsCE;
		if(msCE.startsWith("?")) msCE = msCE.substring(1);
    }

	public boolean isConstrained(){
		if( msCE == null ) return false;
		return (msCE.length() != 0 );
	}

	/** The catalog constraint expression only applies to the top level of the catalog */
	public void setCatalog( Catalog theCatalog ){
		mCatalog = theCatalog;
	}

	public Catalog getCatalog(){ return mCatalog; }

	public String getExampleFile(){
		DATASET_TYPE eType = getType();
		if( eType == DATASET_TYPE.Data || eType == DATASET_TYPE.Image ){
			return this.getFullURL();
		} else if( eType == DATASET_TYPE.Directory ){
			StringBuffer sbError = new StringBuffer(80);
			String[] asFiles = this.getDirectoryFiles(sbError);
			if( asFiles == null ) return null;
			if( asFiles.length < 2 ) return null;
			return asFiles[1];
		} else if( eType == DATASET_TYPE.Catalog ){
			if( this.mCatalog == null ) return null;
			return this.mCatalog.getExampleFileFullURL();
		}
		return null;
	}

	public void setDirectoryRegex( String sRegex ){
		msDirectoryRegex = sRegex;
	}

	public String getDirectoryRegex(){ return msDirectoryRegex; }

    /**
     * Set the title of the URL.
     * @param urlTitle The title of the URL.
     */
    public void setTitle(String sNewTitle) {
		msTitle = sNewTitle;
    }

    /**
     * Set the directory location of the file this dataset is currently being saved to.
     * @param urlTitle The title of the URL.
     */
    public void setFileName( String sNewFileName ){
		mSavable._setFileName( sNewFileName );
    }    

    /**
     * Set the directory location of the file this dataset is currently being saved to.
     * @param urlTitle The title of the URL.
     */
    public void setFileDirectory( String sNewDirectory ){
		mSavable._setFileDirectory( sNewDirectory );
    }    
    
    /**
     * Set the base URL
     * @param dodsURL the base URL.
     */
    public void setURL(String dodsURL) {
		msURL = dodsURL;
    }

	public int getID(){ return miID; }
	public void setID( int iNewID ){ miID = iNewID; }

	// 123M  999K 999G 850B
	public String getSizeEstimateS(){
		long nSize = getSizeEstimate();
		if( nSize == 0 ) return "----";
		return Utility_String.getByteCountString( nSize );
	}

	public long getSizeEstimate(){
		DDS dds = getDDS_Subset();
		DataDDS ddds = getData();
		if( dds == null ) dds = getDDS_Full();
		if( dds == null && ddds == null ) return 0;
		if( getData() == null ){
			return nSize_recursion( dds.getVariables() ); // get variables returns BaseType
		} else {
			return nSize_recursion( ddds.getVariables() );
		}
	}
	private long nSize_recursion( Enumeration<BaseType> enumVariables ){
		try {
			long nCount = 0;
			while( enumVariables.hasMoreElements() ){
				BaseType bt = enumVariables.nextElement();
				if( bt instanceof DSequence ){
					DSequence ds = (DSequence)bt;
					int ctRow = ds.getRowCount();
					if( ctRow < 1 ) continue; // cannot estimate the size of sequences
					for( int xRow = 0; xRow < ctRow; xRow++ ){
						java.util.Vector<BaseType> vectorBaseTypes = ds.getRow(xRow);
						if( vectorBaseTypes == null ) continue;
						nCount += nSize_recursion( vectorBaseTypes.elements() );
					}
				} else if(bt instanceof DStructure || bt instanceof DGrid ) {
					enumVariables = ((DConstructor)bt).getVariables();
				} else if(bt instanceof DArray) {
					DArray darray = (DArray)bt;
					Enumeration<DArrayDimension> enumDims = darray.getDimensions();
					long nArraySize = 1;
					while(enumDims.hasMoreElements()){
					    DArrayDimension dim = (DArrayDimension)enumDims.nextElement();
						nArraySize *= dim.getSize();
					}
					nCount += nArraySize;
				} else if( bt instanceof DByte) {
					nCount += 1;
				} else if( bt instanceof DInt16 || bt instanceof DUInt16) {
					nCount += 2;
				} else if( bt instanceof DFloat32 || bt instanceof DInt32 || bt instanceof DUInt32 ) {
					nCount += 4;
				} else if( bt instanceof DFloat64) {
					nCount += 8;
				} else if( bt instanceof DString) {
					DString dString = (DString)bt;
					String s = dString.getValue();
					if( s == null ) continue; // unable to estimate
					nCount += s.length();
				} else {
					// other types are not estimated
				}
				nCount += nSize_recursion( enumVariables );
			}
			return nCount;
		} catch( Exception ex ) {
			return 0;
		}
	}

	public static Model_Dataset[] getPreferenceURLs_Favorites(){
		FilenameFilter filter = new opendap.clients.odc.gui.Filter_Favorites();
		return getPreferenceURLs( filter, Utility.SORT_descending );
	}

	public static Model_Dataset[] getPreferenceURLs_Recent(){
		FilenameFilter filter = new opendap.clients.odc.gui.Filter_Recent();
		return getPreferenceURLs( filter, Utility.SORT_descending );
	}

	public static Model_Dataset[] getPreferenceURLs( FilenameFilter filter, int eSORT ){
		String sFileName = null, sPath = null;
		try {
			File[] afile = ConfigurationManager.getPreferencesFiles(filter);
			if( afile == null ) return null;
			Utility.sortFiles( afile, eSORT );
			Model_Dataset[] aURLs = new Model_Dataset[afile.length];
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
					ApplicationController.vShowWarning( "Failed to load preference " + iNumber + " [" + sPath + "]: " + sbError + "/n Delete this file to eliminate this warning." );
				} else {
					xDodsURL++;
					aURLs[xDodsURL-1] = (Model_Dataset)oURL;
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
			ApplicationController.vUnexpectedError(ex, sbError);
			ApplicationController.vShowError(sbError.toString());
			return null;
		}
	}
	
	Model_Dataset_Local getDataTree( StringBuffer sbError ){
		BaseType bt_root;
		if( getType() == DATASET_TYPE.Data ){
			DataDDS data = this.getData();
			if( data == null ){
				sbError.append( "internal error, model is defined as a DataDDS but there is no data structure present." );
				return null;
			}
			bt_root = data;
		} else if( getType() == DATASET_TYPE.Definition ){
			bt_root = this.getDDS_Full();
			if( bt_root == null ){
				sbError.append( "internal error, model is defined as a DDS but there is no structure present." );
				return null;
			}
		} else {
			sbError.append( "dataset type is " + this.getTypeString() + " which cannot be interpreted as a tree" );
			return null;
		}
		Model_Dataset_Local model = Model_Dataset_Local.create( this, sbError );
		if( model == null ){
			sbError.insert( 0, "failed to create tree model: " );
		}
		return model;
	}
	
}


