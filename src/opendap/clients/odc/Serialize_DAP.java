package opendap.clients.odc;

import java.io.File;

import opendap.dap.*;

/**
 * Serialization of OPeNDAP objects primarily for saving to file
 * DataDDS Format:
 * 
 * Magic Word (null terminated): OPeNDAP\0 
 * Object Type (null terminated): DataDDS\0
 * Server Version String (null terminated):
 * Server Version Major: int
 * Server Version Minor: int 
 * DDS length: int
 * DDS text (null terminated)
 * DAS length: int (0 if no DAS is present)
 * DAS text (null terminated)
 * 
 */

public class Serialize_DAP {
	
	public final static String MAGIC_WORD = "OPeNDAP";
	public final static String OBJECT_TYPE_DataDDS = "DataDDS";
	public final static int MAX_DDS_LENGTH = 100000;
	public final static int MAX_DAS_LENGTH = 250000;
	
	/** @return true on success */
	final public static boolean write( opendap.dap.DataDDS ddds, String sFilePath, StringBuffer sbError ){
		try {
			// if file does not exist, create it
			File file = null;
			try {
				file = new File( sFilePath );
				if( ! file.exists() ){
					file.createNewFile();
				}
			} catch( Exception ex ) {
				sbError.append("failed to create file " + sFilePath + ": " + ex );
				return false;
			}

			// open file
			java.io.FileOutputStream fos = null;
			try {
			    fos = new java.io.FileOutputStream(file);
				if( fos == null ){
					sbError.append("failed to open file, empty stream");
					return false;
				}
			} catch(Exception ex) {
				sbError.append("failed to open file for writing: " + ex);
				return false;
			}
			java.io.DataOutputStream dos = new java.io.DataOutputStream( fos ); 

			fos.write( MAGIC_WORD.getBytes() ); fos.write('\0');
			fos.write( OBJECT_TYPE_DataDDS.getBytes() ); fos.write('\0');
			String sServerVersion = ddds.getServerVersion().getVersionString();
			fos.write( sServerVersion.getBytes() ); fos.write('\0');
			dos.writeInt( ddds.getServerVersion().getMajor() );
			dos.writeInt( ddds.getServerVersion().getMinor() );
			String sDDS = ddds.getDDSText();
			dos.writeInt( sDDS.length() );
			fos.write( sDDS.getBytes() ); fos.write('\0');
			DAS das = ddds.getDAS();
			if( das == null ){
				dos.writeInt( 0 );
			} else {
				String sDAS = das.toString();
				dos.writeInt( sDAS.length() );
				fos.write( sDAS.getBytes() );
				fos.write('\0');
			}
			ddds.externalize( dos );
			return true;
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, sbError );
			return false;
		}
	}

	/** @return DataDDS on success, null on failure */
	final public static DataDDS read( String sFilePath, StringBuffer sbError ){
		try {
			File file = null;
			try {
				file = new File( sFilePath );
				if( ! file.exists() ){
					sbError.append( "file does not exist: " + sFilePath );
					return null;
				}
			} catch( Exception ex ) {
				sbError.append("failed to create file " + sFilePath + ": " + ex );
				return null;
			}

			// open file
			java.io.FileInputStream fis = null;
			try {
			    fis = new java.io.FileInputStream(file);
				if( fis == null ){
					sbError.append("failed to open file, empty stream");
					return null;
				}
			} catch(Exception ex) {
				sbError.append("failed to open file for reading: " + ex);
				return null;
			}
			java.io.DataInputStream dis = new java.io.DataInputStream( fis ); 

			String sMagicWord = readString( fis, MAGIC_WORD.length(), sbError );
			if( sMagicWord == null || ! sMagicWord.equals( MAGIC_WORD ) ){
				sbError.append("file is not an ODC file");
				return null;
			}
			String sObjectType = readString( fis, OBJECT_TYPE_DataDDS.length(), sbError );
			if( sObjectType == null || ! sObjectType.equals( OBJECT_TYPE_DataDDS ) ){
				sbError.append("file is not a DataDDS");
				return null;
			}
			String sServerVersion = readString( fis, 256, sbError );
			if( sServerVersion == null ){
				sbError.append( "DataDDS file invalid, no server version: " + sbError );
				return null;
			}
			int iServerVersion_Major = dis.readInt();
			int iServerVersion_Minor = dis.readInt();
			int iDDSlength = dis.readInt();
			if( iDDSlength < 10 || iDDSlength > MAX_DDS_LENGTH ){
				sbError.append( "invalid DDS length: " + iDDSlength );
				return null;
			}
			String sDDS = readChars( fis, iDDSlength, sbError );
			if( sDDS == null ){
				sbError.insert( 0, "error reading DDS of length " + iDDSlength + ": " );
				return null;
			}
			fis.read(); // int iDDS_null_terminator = 
			int iDASlength = dis.readInt();
			if( iDASlength < 10 || iDASlength > MAX_DAS_LENGTH ){
				sbError.append( "invalid DAS length: " + iDASlength );
				return null;
			}
			String sDAS = readChars( fis, iDASlength, sbError );
			if( sDAS == null ){
				sbError.insert( 0, "error reading DAS of length " + iDASlength + ": " );
				return null;
			}
			fis.read(); // int iDAS_null_terminator = 
			ServerVersion sv = new ServerVersion( iServerVersion_Major, iServerVersion_Minor );
			DataDDS ddds = new DataDDS( sv );
			try {
				ddds.deserialize( dis, sv, null );
			} catch( Throwable t ) {
				ApplicationController.vUnexpectedError( t, sbError );
				sbError.insert( 0, "error deserializing data DDS: " );
				return null;
			}
			return ddds;
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, sbError );
			return null;
		}
	}

	final public static String readString( java.io.FileInputStream fis, int iMaxLength, StringBuffer sbError ){
		try {
			StringBuffer sb = new StringBuffer(250);
			int ctChars = 0;
			while( true ){
				int iByte = fis.read();
				if( iByte == -1 ){
					if( sb.length() == 0 ){
						sbError.append("file is empty");
						return null;
					} else return sb.toString();
				}
				if( iByte == 0 ) return sb.toString();
				ctChars++;
				if( ctChars > iMaxLength ){
					sbError.append("end of string not found");
					return null;
				}
				sb.append( iByte );
			}
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, sbError );
			return null;
		}
	}

	final public static String readChars( java.io.FileInputStream fis, int ctCharsToRead, StringBuffer sbError ){
		try {
			StringBuffer sb = new StringBuffer(ctCharsToRead);
			int ctChars = 0;
			while( true ){
				if( ctChars == ctCharsToRead ) return sb.toString();
				int iByte = fis.read();
				if( iByte == -1 ){
					if( sb.length() == 0 ){
						sbError.append("file ended after " + ctChars + " characters");
						return null;
					} else return sb.toString();
				}
				ctChars++;
				sb.append( iByte );
			}
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, sbError );
			return null;
		}
	}
	
}
