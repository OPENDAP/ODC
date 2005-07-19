package opendap.clients.odc;

/**
 * A <code>DodsURL</code> stores information about a Dods URL.  More
 * specifially, it stores the base url, the constraint expression, the
 * type of URL, and what class is need to get further information from
 * the URL.  It should be used over <code>String</code> to represent
 * Dods URLs whenever possible.
 *
 * @author rhonhart
 */

import opendap.dap.*;
import java.util.Enumeration;

public class DodsURL implements java.io.Serializable {

	// todo delete fav/recent files that have bad serialization
	private static final long serialVersionUID = 2L;

    public final static int TYPE_Data = 0;
    public final static int TYPE_Directory = 1;
    public final static int TYPE_Catalog = 2;
    public final static int TYPE_Image = 3;
    public final static int TYPE_HTML = 4;
    public final static int TYPE_Text = 5;
    public final static int TYPE_Binary = 6;

    private String msURL;
    private String msCE;
	private String msSubDirectory; // with no leading slash
	private String msDirectoryRegex;
    private int miURLType;
    private String msTitle;
	private int miDigest = 0;
	private String msMIMEType;

	private String msDDS_Text;
	private String msDAS_Text;
	private String msInfo_Text;

	transient private boolean mzUnreachable = false;
	transient private String msError; // if the site is unreachable

	private DDS mDDS_Subset;
	private DDS mDDS_Full;
	private DAS mDAS; // this is used in case of a catalog
	transient private Catalog mCatalog;
	transient private Model_DirectoryTree mDirectoryTree; // this is used in the case of a directory
	transient private DataDDS mDataDDS;

	transient int miID; // used to tag an URL with an arbitrary id such as for favorites and recent

    /**
     * Create an empty <code>DodsURL</code>.
     */
    public DodsURL() {
		msURL = "";
		msCE = "";
		miURLType = 0;
		msTitle = null;
		msMIMEType = null;
    }

    /**
     * Create a <code>DodsURL</code> by copying an existing <code>DodsURL</code>
     * @param dodsURL The url to copy.
     */
    public DodsURL(DodsURL dodsURL) {
		msURL = dodsURL.msURL;
		msCE = dodsURL.msCE;
		miURLType = dodsURL.miURLType;
		msTitle = dodsURL.msTitle;
		msSubDirectory = dodsURL.msSubDirectory; // with no leading slash
		msDirectoryRegex = dodsURL.msDirectoryRegex;
		msMIMEType = dodsURL.msMIMEType;
    }

    /**
     * Create a <code>DodsURL</code> with a specific base URL and constraint
     * expression.  This url is assumed to be a TYPE_Data, and it uses the
     * default TYPE_Data processor.
     * @param dodsURL The base url.
     * @param dodsCE The constraint expression.
     */
    public DodsURL(String dodsURL, String dodsCE) {
		msURL = dodsURL;
		msCE = dodsCE;

		// It only makes sense to supply a constraint expression for a
		// data URL, so we can assume this is a data URL.
		miURLType = TYPE_Data;
		msTitle = null;
    }

    /**
     * Create a <code>DodsURL</code> with a specific base URL of type
     * <code>type</code>.  The constraint expression is set to an empty string
     * and the urlProcessor is set to the default for the give type.
     * @param dodsURL The base url.
     * @param type The type of URL.
     */
    public DodsURL(String dodsURL, int type) {
		msURL = dodsURL;
		msCE = "";
		miURLType = type;
		msTitle = null;
		if( type == DodsURL.TYPE_Image ) msMIMEType = Utility.getMIMEtype(dodsURL);
    }

	public boolean equals(Object o){
	    if( o == null ) return false;
	    if( !( o instanceof DodsURL ) ) return false;
		DodsURL urlComparison = (DodsURL)o;
		if( !msURL.equalsIgnoreCase(urlComparison.msURL) ) return false;
		if( !msCE.equalsIgnoreCase(urlComparison.msCE) ) return false;
		return true;
	}

	public String getInfo(){
		StringBuffer sbInfo = new StringBuffer(1000);
		sbInfo.append(this.getTitle() + '\n');
		sbInfo.append(this.getFullURL() + '\n');
		sbInfo.append("URL type: " + this.getTypeString() + '\n' );
		if( this.isUnreachable() ) sbInfo.append("URL is unreachable: " + this.getError() + "\n");
		String sBaseURL = this.getBaseURL();
		StringBuffer sbError = new StringBuffer(80);
		int iType = getType();
		if( iType == DodsURL.TYPE_Data ){
			if( this.getDDS_Text() != null ) sbInfo.append(this.getDDS_Text());
			if( this.getDAS_Text() != null ) sbInfo.append(this.getDAS_Text());
			if( this.getInfo_Text() != null ) sbInfo.append(this.getInfo_Text());
		}
		if( iType == DodsURL.TYPE_Directory ){
			if( this.getDirectoryRegex() != null ) sbInfo.append(this.getDirectoryRegex() + '\n');
			if( this.getDirectoryTree() == null ){
				sbInfo.append("Directory tree not available");
			} else {
				sbInfo.append(this.getDirectoryTree().getPrintout());
			}
		}
		if( iType == DodsURL.TYPE_Catalog ){
			sbInfo.append("[Additional catalog info not supported]");
		}
		if( iType == DodsURL.TYPE_Image ){
			sbInfo.append("[Additional image info not available]");
		}
		return sbInfo.toString();
	}

	public boolean isUnreachable(){ return mzUnreachable; }

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

	public void setDDS_Text(String dds_text){
		msDDS_Text = dds_text;
	}

	public String getDAS_Text(){ return msDAS_Text; }

	public void setDAS_Text(String das_text){ msDAS_Text = das_text; }

	public String getInfo_Text(){ return msInfo_Text; }

	public String getFileName(){
		String sFullURL = this.getBaseURL();
		if( sFullURL == null ) return "";
		if( Utility.isDodsURL(sFullURL) && sFullURL.toUpperCase().endsWith(".HTML") ){
			sFullURL.substring(0, sFullURL.length()-5); // strip .HTML
		}
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
		return mDirectoryTree.getDirectoryFiles(sPath, sbError);
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
			String sEscapedCE = Utility.sEscapeURL(msCE);
			return sEscapedCE;
		} catch(Exception ex) {
			StringBuffer sbError = new StringBuffer("error escaping CE: ");
			Utility.vUnexpectedError(ex, sbError);
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
		if( this.miURLType == this.TYPE_Directory && msSubDirectory != null){
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
    public int getType() {
		return miURLType;
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
		switch(miURLType){
			case DodsURL.TYPE_Data: return "Data";
			case DodsURL.TYPE_Directory: return "Directory";
			case DodsURL.TYPE_Catalog: return "Catalog";
			case DodsURL.TYPE_Image: return "Image";
			default: return "[unknown]";
		}
    }

    /**
     * Create a string representation of the URL.
     * @return The full URL.
     */
    public String toString() {
		if(msTitle != null) {
			return msTitle + "(" + getFullURL() + ")";
		} else {
            return getFullURL();
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
		int iType = this.getType();
		if( iType == this.TYPE_Data || iType == this.TYPE_Image ){
			return this.getFullURL();
		} else if( iType == this.TYPE_Directory ){
			StringBuffer sbError = new StringBuffer(80);
			String[] asFiles = this.getDirectoryFiles(sbError);
			if( asFiles == null ) return null;
			if( asFiles.length < 2 ) return null;
			return asFiles[1];
		} else if( iType == this.TYPE_Catalog ){
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
     * Set the type of the URL.  Additionally, if no processor has been set,
     * this function will set it to the default processor for type
     * <code>type</type>.
     * @param type The type of URL.
     */
    public void setType(int type) {
		miURLType = type;
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
		return Utility.getByteCountString( nSize );
	}

	public long getSizeEstimate(){
		DDS dds = getDDS_Subset();
		DataDDS ddds = getData();
		if( dds == null ) dds = getDDS_Full();
		if( dds == null && ddds == null ) return 0;
		if( getData() == null ){
			return nSize_recursion( dds.getVariables() );
		} else {
			return nSize_recursion( ddds.getVariables() );
		}
	}
	private long nSize_recursion( Enumeration enumVariables ){
		try {
			long nCount = 0;
			while( enumVariables.hasMoreElements() ){
				BaseType bt = (BaseType)enumVariables.nextElement();
				if( bt instanceof DSequence ){
					DSequence ds = (DSequence)bt;
					int ctRow = ds.getRowCount();
					if( ctRow < 1 ) continue; // cannot estimate the size of sequences
					for( int xRow = 0; xRow < ctRow; xRow++ ){
						java.util.Vector vectorBaseTypes = ds.getRow(xRow);
						if( vectorBaseTypes == null ) continue;
						nCount += nSize_recursion( vectorBaseTypes.elements() );
					}
				} else if(bt instanceof DStructure || bt instanceof DGrid ) {
					enumVariables = ((DConstructor)bt).getVariables();
				} else if(bt instanceof DArray) {
					DArray darray = (DArray)bt;
					Enumeration enumDims = darray.getDimensions();
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

}


