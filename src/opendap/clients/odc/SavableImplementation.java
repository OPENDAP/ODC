package opendap.clients.odc;

import java.io.File;

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

}
