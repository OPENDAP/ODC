package opendap.clients.odc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import javax.swing.JFileChooser;

import opendap.clients.odc.data.Model_Dataset;
import opendap.clients.odc.plot.Panel_View_Plot;

public class SavableImplementation implements ISavable {
	
	private Class<?> mExpectedClass = null;
	private String msFileDirectory;
	private String msFileName;
	private boolean mzDirty = false;
	
	public SavableImplementation( java.lang.Class<?> expected_class, String sFileDirectory, String sFileName ){
		mExpectedClass = expected_class;
		msFileDirectory = sFileDirectory;
		msFileName = sFileName;
	}
	
    /** returns the directory for the file with the terminating separator usually
     *  example: "c:\odc\scripts\" */
	public String _getFileDirectory(){ return msFileDirectory; }
	
    /** file name only, example: "image_processing.py" */
	public String _getFileName(){ return msFileName; }

    /** set file name only, example: "image_processing.py" */
	public void _setFileName( String sNewName ){ msFileName = sNewName; }

    /** sets the directory for the file (including terminating separator is optional)
     *  example: "c:\odc\scripts\" */
	public void _setFileDirectory( String sNewDirectory ){ msFileDirectory = sNewDirectory; }
    
	static javax.swing.JFileChooser mjfcOpen = null;
	public Object _open(){
		try {
			StringBuffer sbError = new StringBuffer( 250 );

			// ask user for desired location
			if( mjfcOpen == null ){
				mjfcOpen = new javax.swing.JFileChooser();
				String sDirectory = ConfigurationManager.getInstance().getDefault_DIR_Scripts();
				File fileDirectory = Utility.fileEstablishDirectory( sDirectory, sbError );
				if( fileDirectory == null ){
					// no default directory
				} else {
					mjfcOpen.setCurrentDirectory( fileDirectory );
				}
			}
			int iState = mjfcOpen.showDialog( ApplicationController.getInstance().getAppFrame(), "Open Text File" );
			File file = mjfcOpen.getSelectedFile();
			if( file == null || iState != javax.swing.JFileChooser.APPROVE_OPTION || ! file.isFile() ){
				ApplicationController.vShowStatus_NoCache( "file open cancelled" );
				return null;
			}
			
			// open the selected file
			Object serializableContent = Utility.fileLoadIntoObject( file, sbError);
			if( serializableContent == null ){
				ApplicationController.vShowError( "Error opening file " + file + ": " + sbError );
				return null;
			}
			if( serializableContent.getClass() != mExpectedClass ){
				ApplicationController.vShowError( "Content wrong type, received " + serializableContent.getClass() + ", expected " + mExpectedClass );
				return null;
			}
			msFileDirectory = file.getParent();
			msFileName = file.getName();
			return serializableContent;
			
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError( ex, "while opening file" );
			return null;
		}
	}
	public boolean _save( java.io.Serializable serializableObjectToBeSaved ){
		String sDirectory = _getFileDirectory();
		String sFileName  = _getFileName();
		if( sDirectory == null || sFileName == null ){
			return _saveAs( serializableObjectToBeSaved );
		} else {
			try {
				StringBuffer sbError = new StringBuffer();
				File file = Utility.fileDefine( sDirectory, sFileName, sbError);
				if( file == null ){					
					ApplicationController.vShowWarning( "error defining file (dir: " + sDirectory + " name: " + sFileName + "): " );
					return _saveAs( serializableObjectToBeSaved );
				} else {
					if( Utility.fileSave( file, serializableObjectToBeSaved, sbError ) ){
						ApplicationController.vShowStatus( "Saved " + file );
						_makeClean();
						return true;
					} else {
						ApplicationController.vShowError( "Error saving file [" + file + "]: " + sbError );
						return false;
					}
				}
			} catch( Throwable t ) {
				ApplicationController.vUnexpectedError( t, "while saving file" );
				return false;
			}
		}
	}
	public boolean _saveAs( java.io.Serializable serializableObjectToBeSaved ){
		StringBuffer sbError = new StringBuffer();
		String sSuggestedDirectory = _getFileDirectory();
		if( sSuggestedDirectory == null ) sSuggestedDirectory = ConfigurationManager.getInstance().getProperty_DIR_Scripts();
		File fileSaved = Utility.fileSaveAs( ApplicationController.getInstance().getAppFrame(), "Save As...", sSuggestedDirectory, _getFileName(), serializableObjectToBeSaved, sbError );
		if( fileSaved == null ){
			if( sbError.length() == 0 ){
				ApplicationController.vShowStatus_NoCache( "save cancelled" );
			} else { // an error occurred
				ApplicationController.vShowError( "Error saving file: " +  sbError );
			}
			return false;
		}

		// remember this directory as the new text file (scripts) directory
		File fileNewScriptsDirectory = fileSaved.getParentFile();
		if( fileNewScriptsDirectory != null ){
			try {
				String sNewScriptsDirectory = fileNewScriptsDirectory.getCanonicalPath();
				ConfigurationManager.getInstance().setOption( ConfigurationManager.PROPERTY_DIR_Scripts, sNewScriptsDirectory );
			} catch( Throwable t ) {
				ApplicationController.vShowWarning( "failed to determine canonical path for [" + fileNewScriptsDirectory + "]: " + t );
			}
		}

		this.mzDirty = false;
		ApplicationController.vShowStatus( "Saved file as " + fileSaved );
		_makeClean();
		return true;

	}
	public boolean _isDirty(){
		return mzDirty;
	}
	public void _makeDirty(){ mzDirty = true; }
	public void _makeClean(){ mzDirty = false; }

// old file loading routine
//	void vFileLoad(){
//		try {
//
//			// determine file path
//			String sCacheDirectory = ConfigurationManager.getInstance().getProperty_DIR_DataCache();
//			StringBuffer sbError = new StringBuffer();
//			File fileCacheDirectory = Utility.fileEstablishDirectory( sCacheDirectory, sbError );
//			if (jfc == null) jfc = new JFileChooser();
//			if( fileCacheDirectory != null ) jfc.setCurrentDirectory(fileCacheDirectory);
//			int iState = jfc.showDialog(Panel_View_Plot.this, "Load");
//			File file = jfc.getSelectedFile();
//			if (iState != JFileChooser.APPROVE_OPTION)	return;
//			if (file == null) return;
//
//			// load serialized object
//			Object o = Utility.oLoadObject(file, sbError);
//			if( o == null ){
//				ApplicationController.vShowError("Error loading Data DDS from file: " + sbError);
//				return;
//			}
//			if( o instanceof opendap.clients.odc.plot.DataConnectorFile ){
//				DataConnectorFile dcf = (DataConnectorFile)o;
//				Model_Dataset url = dcf.getURL();
//				url.setData(dcf.getData());
//				if( Panel_View_Plot.this.source_Add(url, sbError) ){
//					ApplicationController.vShowStatus("File loaded to plotter: " + url);
//				} else {
//					ApplicationController.vShowError("Error adding file as plotting source: " + sbError);
//				}
//			} else {
//				ApplicationController.vShowError("Object in file " + file.getPath() + " is of type " +  o.getClass().getName() + " not a loadable type");
//				return;
//			}
//
//		} catch(Exception ex) {
//			StringBuffer sbError = new StringBuffer(80);
//			ApplicationController.vUnexpectedError(ex, sbError);
//			ApplicationController.vShowError("Unexpected error loading item: " + sbError);
//		}
//	}

// old file save routine
//	void vSaveSelectedListing(){
//		int xURL_0;
//		try {
//			xURL_0 = jlistSelectedURLs.getSelectedIndex();
//			if( xURL_0 < 0 ){
//				ApplicationController.vShowWarning("nothing selected");
//				return;
//			}
//			final Model_Dataset url = (Model_Dataset)Panel_View_Plot.this.listPlotterData.get(xURL_0);
//			String sTitle = url.getTitle();
//			DataConnectorFile dcf = new DataConnectorFile();
//			dcf.setTitle(url.getTitle());
//			dcf.setURL(url);
//			dcf.setData(url.getData());
//
//			// ask user for desired location
//			String sCacheDirectory = ConfigurationManager.getInstance().getProperty_DIR_DataCache();
//			StringBuffer sbError = new StringBuffer();
//			File fileCacheDirectory = Utility.fileEstablishDirectory( sCacheDirectory, sbError );
//			if (jfc == null) jfc = new JFileChooser();
//			if( fileCacheDirectory == null ){
//				// not established
//			} else {
//				jfc.setCurrentDirectory(fileCacheDirectory);
//			}
//			String sSuggestedFileName = Utility.sFriendlyFileName(sTitle) + ConfigurationManager.EXTENSION_Data;
//			jfc.setSelectedFile(new File(sSuggestedFileName));
//			int iState = jfc.showDialog(Panel_View_Plot.this, "Select Save Location");
//			File file = jfc.getSelectedFile();
//			if (file == null || iState != JFileChooser.APPROVE_OPTION) return; // user cancel
//
//			// try to save this directory as the new data cache directory
//			File fileNewCacheDirectory = file.getParentFile();
//			if( fileCacheDirectory != null ) if( !fileCacheDirectory.equals(fileNewCacheDirectory) ){
//				String sNewCacheDirectory = fileNewCacheDirectory.getCanonicalPath();
//				ConfigurationManager.getInstance().setOption(ConfigurationManager.PROPERTY_DIR_DataCache, sNewCacheDirectory );
//			}
//
//			// save DCF
//			String sPath = file.getPath();
//			FileOutputStream fos = new FileOutputStream(file);
//			ObjectOutputStream oos = new ObjectOutputStream(fos);
//			try {
//				oos.writeObject(dcf);
//			} catch(Exception ex) {
//				ApplicationController.vShowError("Failed to serialize object to file [" + sPath + "]: " + ex);
//			} finally {
//				try {
//					if(fos!=null) fos.close();
//				} catch(Exception ex) {}
//			}
//		} catch(Exception ex) {
//			StringBuffer sbError = new StringBuffer(80);
//			ApplicationController.vUnexpectedError(ex, sbError);
//			ApplicationController.vShowError("Unexpected error unloading item: " + sbError);
//		}
//	}

	
}
