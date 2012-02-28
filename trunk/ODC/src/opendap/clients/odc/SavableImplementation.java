package opendap.clients.odc;

import opendap.clients.odc.data.Model_Dataset;
import java.io.File;
import java.util.ArrayList;

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
		StringBuffer sbError = new StringBuffer(); 
		try {
			
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
			Object o = _open( file, sbError );
			if( o == null && sbError.length() > 0 ){
				ApplicationController.vShowError( "Error opening file (" + file.getAbsolutePath() + "): " + sbError );
			}
			return o;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError( ex, sbError );
			return null;
		}
	}
			
	public Object _open( File file, StringBuffer sbError ){
		try {
			
			// characterize file
			Object serializableContent = null;
			if( Utility.zFileIsBinary( file ) ){
			
				// open the selected file as object
				serializableContent = Utility.fileLoadIntoObject( file, sbError);
				if( serializableContent == null ){
					sbError.append( "Error opening file " + file + ": " + sbError );
					return null;
				}
				if( serializableContent.getClass() != mExpectedClass ){
					sbError.append( "Content wrong type, received " + serializableContent.getClass() + ", expected " + mExpectedClass );
					return null;
				}
			} else { // open as script
				String s = Utility.fileLoadIntoString( file, sbError );
				if( s == null ){
					sbError.insert( 0, "failed to load into string: " );
					return null;
				}
				ArrayList<String> listLines = Utility.zLoadLines( s, sbError );
				if( listLines == null ){
					sbError.insert( 0, "failed to load lines: " );
					return null;
				}
				boolean zIsODCExpression = false;
				boolean zIsDataScript = false;
				boolean zEncounteredNonComment = false;
				for( int xLine = 0; xLine < listLines.size(); xLine++ ){
					String sLine = listLines.get( xLine );
					String s_no_spaces = Utility_String.sReplaceString( sLine, " ", "" );
					String s_no_tabs = Utility_String.sReplaceString( s_no_spaces, "\t", "" );
					String sLine_upper = s_no_tabs.toUpperCase();
					if( sLine_upper.trim().length() > 0 &&  ! sLine_upper.startsWith( "#" ) ) zEncounteredNonComment = true;
					if( ! zEncounteredNonComment && sLine_upper.startsWith( "#!ODC" ) ) zIsODCExpression = true; 
					if( sLine_upper.startsWith( "VALUE=" ) || sLine_upper.startsWith( "0$=" ) ){
						zIsDataScript = true;
						break;
					}
				}
				Model_Dataset model;
				if( zIsODCExpression ){
					if( zIsDataScript ){
						model = Model_Dataset.createDataScript( sbError );
					} else {
						model = Model_Dataset.createExpression( sbError );
					}
					if( model == null ){
						sbError.insert( 0, "failed to create model: " );
						return null;
					}
					model.setTextContent( s );
					serializableContent = model;
				} else { // treat as plain text
					model = Model_Dataset.createPlainText( sbError );
					model.setTextContent( s );
					serializableContent = model;
				}
			}
			msFileDirectory = file.getParent();
			msFileName = file.getName();
			return serializableContent;
			
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError( ex, sbError );
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
					if( serializableObjectToBeSaved instanceof Model_Dataset ){
						Model_Dataset model = (Model_Dataset)serializableObjectToBeSaved;
						if( model.getType() == Model_Dataset.DATASET_TYPE.DataScript ||
							model.getType() == Model_Dataset.DATASET_TYPE.PlottableExpression || 
							model.getType() == Model_Dataset.DATASET_TYPE.Text ){
							String sFileContent = model.getTextContent();
							Utility.fileSave( file, sFileContent, sbError );
							ApplicationController.vShowStatus( "Saved script " + file );
							String sPath_LastLoadedData = file.getAbsolutePath();
							ConfigurationManager.getInstance().setOption( ConfigurationManager.PROPERTY_PATH_LastLoadedData, sPath_LastLoadedData );
							_makeClean();
							System.out.println( "Saved: " + sFileContent );
							return true;
						}
					}
					if( Utility.fileSave( file, serializableObjectToBeSaved, sbError ) ){
						ApplicationController.vShowStatus( "Saved " + file );
						String sPath_LastLoadedData = file.getAbsolutePath();
						ConfigurationManager.getInstance().setOption( ConfigurationManager.PROPERTY_PATH_LastLoadedData, sPath_LastLoadedData );
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

		boolean zSaveScript = false;
		String sScriptText = null;
		if( serializableObjectToBeSaved instanceof Model_Dataset ){
			Model_Dataset model = (Model_Dataset)serializableObjectToBeSaved;
			if( model.getType() == Model_Dataset.DATASET_TYPE.DataScript ||
				model.getType() == Model_Dataset.DATASET_TYPE.PlottableExpression ||
				model.getType() == Model_Dataset.DATASET_TYPE.Text ){
				zSaveScript = true;
				sScriptText = model.getTextContent();
			}
		}
		File fileSaved;
		if( zSaveScript ){
			fileSaved = Utility.fileSaveAs( ApplicationController.getInstance().getAppFrame(), "Save Script As...", sSuggestedDirectory, _getFileName(), sScriptText, sbError );
		} else {
			fileSaved = Utility.fileSaveAs( ApplicationController.getInstance().getAppFrame(), "Save As...", sSuggestedDirectory, _getFileName(), serializableObjectToBeSaved, sbError );
		}
		if( fileSaved == null ){
			if( sbError.length() == 0 ){
				ApplicationController.vShowStatus_NoCache( "save cancelled" );
			} else { // an error occurred
				ApplicationController.vShowError( "Error saving file: " +  sbError );
			}
			return false;
		}
		String sPath_LastLoadedData = fileSaved.getAbsolutePath();
		ConfigurationManager.getInstance().setOption( ConfigurationManager.PROPERTY_PATH_LastLoadedData, sPath_LastLoadedData );

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

		_makeClean();
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

