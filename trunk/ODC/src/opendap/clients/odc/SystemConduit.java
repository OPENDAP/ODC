package opendap.clients.odc;

import java.io.*;

public class SystemConduit {

    public static void main(String[] args){
	}

	static void vExec(final String sCommand, final String sArg1, final String sArg2){
		try {
//			long nStart = System.currentTimeMillis();
			String[] asCommand;
			String sCommandComplete;
			if( sArg1 == null ){
				asCommand = new String[1];
				asCommand[0] = sCommand;
				sCommandComplete = sCommand;
			} else if( sArg2 == null ) {
				asCommand = new String[2];
				asCommand[0] = sCommand;
				asCommand[1] = sArg1;
				sCommandComplete = sCommand + " " + sArg1;
			} else {
				asCommand = new String[3];
				asCommand[0] = sCommand;
				asCommand[1] = sArg1;
				asCommand[2] = sArg2;
				sCommandComplete = sCommand + " " + sArg1 + " " + sArg2;
			}
			final String[] asCommand_final = asCommand;
			final String sCommandComplete_final = sCommandComplete;
			ApplicationController.vShowStatus("Executing OS command: " + sCommandComplete_final);
			Thread threadProcess = new Thread(){
				public void run(){
					try {
						Process process = Runtime.getRuntime().exec(asCommand_final);
					} catch(Exception ex) {
						Utility.vUnexpectedError(ex, "Failed to execute command " + sCommandComplete_final);
					}
				}
			};
			threadProcess.setDaemon(true);
			threadProcess.start();
			Thread.sleep(1000);
			// process.waitFor();
//			System.out.println("execution took ms: " + (System.currentTimeMillis() - nStart) );
//			InputStream is = process.getErrorStream();
//			OutputStream osCrafty = processCrafty.getOutputStream();
//			BufferedWriter bwCrafty = new BufferedWriter(new OutputStreamWriter(osCrafty));
//			BufferedReader br = new BufferedReader(new InputStreamReader(is));
//			System.out.println("reading...");
//			long start = System.currentTimeMillis();
//			while(true){
//				String sLine = br.readLine();
//				if( sLine == null ){
//				} else {
//					System.out.println(sLine);
//				}
//				if( System.currentTimeMillis() - start >  1000 ) break;
//			}
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			return;
		}
	}
	static void setDirectoryReadWrite(String sDirectory ){
		int eOperatingSystem = getOS_TYPE();
		String sCommand;
		String sArg1 = null;
		String sArg2 = null;
		switch( eOperatingSystem ){
			case OS_TYPE_Windows:
				if( sDirectory.endsWith("\\") ) sDirectory = sDirectory.substring(0, sDirectory.length() - 1);
				sCommand = "attrib";
				sArg1 = "-R";
				sArg2 = "\"" + sDirectory + "\"";
				break;
			case OS_TYPE_Mac:
				sCommand = "chmod";
				sArg1 = "a+w";
				sArg2 = sDirectory;
				break;
			case OS_TYPE_Unix:
			case OS_TYPE_unknown:
			default:
				sCommand = "chmod";
				sArg1 = "a+w";
				sArg2 = sDirectory;
				break;
		}
		vExec( sCommand, sArg1, sArg2 );
	}
	static void setDirectoryFilesReadWrite(String sDirectory ){
		int eOperatingSystem = getOS_TYPE();
		String sCommand;
		String sArg1 = null;
		String sArg2 = null;
		String sDirectoryFiles;
		switch( eOperatingSystem ){
			case OS_TYPE_Windows:
				if( sDirectory.endsWith("\\") ) sDirectory = sDirectory.substring(0, sDirectory.length() - 1);
				sCommand = "attrib";
				sArg1 = "-R";
				sArg2 = "\"" + sDirectory + "\\*.*\" /S";
				break;
			case OS_TYPE_Mac:
				sDirectoryFiles = Utility.sConnectPaths(sDirectory, "datasets.xml");
				sCommand = "chmod";
				sArg1 = "a+w";
				sArg2 = sDirectoryFiles;
				break;
			case OS_TYPE_Unix:
			case OS_TYPE_unknown:
			default:
				sDirectoryFiles = Utility.sConnectPaths(sDirectory, "datasets.xml");
				sCommand = "chmod";
				sArg1 = "a+w";
			    sArg2 =	sDirectoryFiles;
				break;
		}
		vExec( sCommand, sArg1, sArg2 );
	}
	public final static int OS_TYPE_unknown = 0;
	public final static int OS_TYPE_Windows = 1;
	public final static int OS_TYPE_Unix = 2;
	public final static int OS_TYPE_Mac = 3;
	static int getOS_TYPE(){
		if( System.getProperty("mrj.version") != null) return OS_TYPE_Mac;
		String sOS = System.getProperty(ConfigurationManager.SYSTEM_PROPERTY_OSName);
		if( sOS == null ) return OS_TYPE_unknown;
		sOS = sOS.toUpperCase();
		if( sOS.startsWith("WINDOWS") ) return OS_TYPE_Windows;
		if(	sOS.startsWith("UNIX") ||
			sOS.startsWith("LINUX") ||
			sOS.startsWith("SOLARIS") ||
			sOS.startsWith("SUNOS") ||
			sOS.startsWith("HP-UX") ||
			sOS.startsWith("MPE/IX") ||
			sOS.startsWith("FREEBSD") ||
			sOS.startsWith("IRIX") ||
			sOS.startsWith("DIGITAL UNIX") ||
			sOS.startsWith("MINIX") ){
				return OS_TYPE_Unix;
		}
		return OS_TYPE_unknown;
	}
}