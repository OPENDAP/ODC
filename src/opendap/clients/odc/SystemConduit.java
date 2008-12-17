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

package opendap.clients.odc;

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
						Process process = Runtime.getRuntime().exec( asCommand_final );
						java.io.BufferedReader buffer = new java.io.BufferedReader( new java.io.InputStreamReader(process.getInputStream()));
						String sOutput = null;
						try {
							java.io.OutputStream osTextViewer = ApplicationController.getInstance().getTextViewerOS();
							while(( sOutput = buffer.readLine() ) != null ) {
								osTextViewer.write( (sOutput + '\n').getBytes() );
							}
							buffer.close();
							String sOutputResult = "Command completed with value: " + process.exitValue();
							osTextViewer.write( sOutputResult.getBytes() );
						} catch (Exception e) {
							// Ignore read errors; they mean the process is done.
						}
					} catch(Exception ex) {
						ApplicationController.vUnexpectedError(ex, "Failed to execute command " + sCommandComplete_final);
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
