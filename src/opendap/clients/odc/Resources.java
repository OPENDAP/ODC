package opendap.clients.odc;

/**
 * Title:        Resources
 * Description:  Standard borders and fonts
 * Copyright:    Copyright (c) 2005
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.59
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

import java.awt.Image;
import javax.swing.ImageIcon;

public class Resources {

    public final static String pathSplashScreen = "/opendap/clients/odc/images/splash-1.png"; // png doesn't seem to work when loaded as a resource
    public final static String pathHelpText = "/opendap/clients/odc/doc/odc-help.txt";
    public final static String pathICONS_InternetConnection = "/opendap/clients/odc/icons/internet-connection-icon.gif";

	public static ImageIcon imageiconSplash = null;
	public static ImageIcon imageiconInternet = null;
	public static ImageIcon imageiconCalculator = null;

	static Image imageIndicator_Granule = null;
	static Image imageIndicator_Directory = null;
	static Image imageIndicator_Catalog = null;
	static Image imageIndicator_Binary = null;
	static Image imageIndicator_Image = null;
	static Image imageConstrained = null;

    public Resources(){}

	public static javax.swing.ImageIcon imageiconLoadResource( String sResourcePath, StringBuffer sbError ){
		java.awt.Image image = imageLoadResource( sResourcePath, sbError );
		if( image == null ) return null;
		return new javax.swing.ImageIcon(image);
	}

	public static java.awt.Image imageLoadResource( String sResourcePath, StringBuffer sbError ){
		try {
			java.net.URL url = ApplicationController.getInstance().getClass().getResource(sResourcePath);
			if( url == null ){
				sbError.append("image resource not found (was missing from the class path or jar file)");
				return null;
			}
			Object oContent = url.getContent();
			if( oContent == null ){
				sbError.append("resource content not available: " + sResourcePath);
				return null;
			}
			java.awt.image.ImageProducer image_producer = (java.awt.image.ImageProducer)oContent;
			if( image_producer == null ){
				if( oContent instanceof java.awt.Image ){
					return (java.awt.Image)oContent;
				} else {
					sbError.append("failed to load image: " + sResourcePath + " unknown resource type: " + oContent.getClass().getName());
					return null;
				}
			} else {
				java.awt.Toolkit tk = java.awt.Toolkit.getDefaultToolkit();
				java.awt.Image image = tk.createImage(image_producer);
				return image;
			}
/* does not work for zip files for some reason:
			java.io.InputStream inputstreamImage = url.openStream();
			int iChunkSize = inputstreamImage.available();
			int iPreviousChunkSize = 0;
			byte[] abImage = new byte[0];
			byte[] abImage_buffer;
			int nChunk = 0;
			while( iChunkSize > 0 ){
				nChunk++;
				byte[] abImage_chunk = new byte[iChunkSize];
				inputstreamImage.read(abImage_chunk, 0, iChunkSize);
				abImage_buffer = new byte[abImage.length + iChunkSize];
				System.arraycopy(abImage, 0, abImage_buffer, 0, abImage.length);
				System.arraycopy(abImage_chunk, 0, abImage_buffer, abImage.length, abImage_chunk.length);
				abImage = abImage_buffer;
				iPreviousChunkSize = iChunkSize;
				iChunkSize = inputstreamImage.available();

// there appears to be a bug in the zip loader for windows that causes
// the available count not to go to 0 when it is done--it sets starts at
// the full size of the file and stays there
				if ( nChunk > 10 || (iChunkSize == iPreviousChunkSize) ) break; // emergency protection
			}
			java.awt.Toolkit tk = java.awt.Toolkit.getDefaultToolkit();
			java.awt.Image image = tk.createImage(abImage);
*/
		} catch( Exception ex ) {
			sbError.append("failed to load image: " + sResourcePath + " error: " + ex);
			return null;
		}
	}

	public static void iconAdd( javax.swing.JFrame theFrame ){
		try {
			String sIconPath;
			String sIconSize = ConfigurationManager.getInstance().getProperty_DISPLAY_IconSize();
			if( sIconSize.equals("16") ){
				sIconPath = "/icons/icon-16.gif";
			} else if( sIconSize.equals("24") ){
				sIconPath = "/icons/icon-24.gif";
			} else if( sIconSize.equals("32") ){
				sIconPath = "/icons/icon-32.gif";
			} else {
				sIconPath = "/icons/icon-32.gif";
				ApplicationController.getInstance().vShowWarning("Unknown icon size [" + sIconSize + "]. Supported sizes are \"16\", \"24\" and \"32\".");
				return;
			}
			sIconPath = Utility.sConnectPaths("/opendap/clients/odc", "/", sIconPath);
			StringBuffer sbError = new StringBuffer();
			javax.swing.ImageIcon icon = imageiconLoadResource( sIconPath, sbError );
			if( icon == null ){
				ApplicationController.getInstance().vShowWarning("failed to load icon " + sIconPath + ": " +sbError);
				return;
			}
			theFrame.setIconImage(icon.getImage());
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowWarning("Unexpected error setting up icon. Default icon will appear.");
		}
	}

	public static boolean zLoadIcons(StringBuffer sbError){
		String sPath = "/opendap/clients/odc/icons/";
		try {

			imageiconSplash = imageiconLoadResource(pathSplashScreen, sbError);
			if( imageIndicator_Granule == null ){
				ApplicationController.vShowError_NoModal("Splash screen [" + pathSplashScreen + "] not loaded: " + sbError);
			}

			imageiconInternet = imageiconLoadResource(pathICONS_InternetConnection, sbError );
			if( imageiconInternet == null ){
				ApplicationController.vShowError_NoModal("Internet icon [" + pathICONS_InternetConnection + "] not loaded: " + sbError);
			}

			imageiconCalculator = Resources.imageiconLoadResource( sPath + "calculator.gif", sbError );
			if( imageiconCalculator == null ){
				ApplicationController.vShowError_NoModal("Calculator icon not loaded: " + sbError);
			}

			javax.swing.ImageIcon image = imageiconLoadResource(sPath + "dataset-granule.gif", sbError);
			imageIndicator_Granule = image.getImage();
			if( imageIndicator_Granule == null ){
				sbError.insert( 0, "icon " + sPath + "dataset-granule.gif" + " not found: " );
				return false;
			}
			image = imageiconLoadResource(sPath + "dataset-directory.gif", sbError);
			imageIndicator_Directory = image.getImage();
			if( imageIndicator_Granule == null ){
				sbError.insert( 0, "icon " + sPath + "dataset-directory.gif" + " not found: " );
				return false;
			}
			image = imageiconLoadResource(sPath + "dataset-catalog.gif", sbError);
			imageIndicator_Catalog = image.getImage();
			if( imageIndicator_Granule == null ){
				sbError.insert( 0, "icon " + sPath + "dataset-catalog.gif" + " not found: " );
				return false;
			}
			image = imageiconLoadResource(sPath + "dataset-binary.gif", sbError);
			imageIndicator_Binary = image.getImage();
			if( imageIndicator_Granule == null ){
				sbError.insert( 0, "icon " + sPath + "dataset-binary.gif" + " not found: " );
				return false;
			}
			image = imageiconLoadResource(sPath + "dataset-image.gif", sbError);
			imageIndicator_Image = image.getImage();
			if( imageIndicator_Granule == null ){
				sbError.insert( 0, "icon " + sPath + "dataset-image.gif" + " not found: " );
				return false;
			}
			image = imageiconLoadResource(sPath + "constrained.gif", sbError);
			imageConstrained = image.getImage();
			if( imageIndicator_Granule == null ){
				sbError.insert( 0, "icon " + sPath + "constrained.gif" + " not found: " );
				return false;
			}
			return true;
		} catch(Exception ex) {
			sbError.append("Icons not found in path " + sPath);
			return false;
		}
	}

}



