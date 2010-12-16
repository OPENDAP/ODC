package opendap.clients.odc.gui;

/**
 * Title:        Resources
 * Description:  Standard borders and fonts
 * Copyright:    Copyright (c) 2005-2010
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.06
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

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.Utility;

public class Resources {

	public final static String dirImages = "/opendap/clients/odc/images";
    public final static String pathHelpText = "/opendap/clients/odc/doc/odc-help.txt";
    public final static String pathICONS_InternetConnection = "/opendap/clients/odc/icons/internet-connection-icon.gif";

	public static ImageIcon imageiconSplash = null;
	public static ImageIcon imageiconInternet = null;
	public static ImageIcon imageiconCalculator = null;
	private static ImageIcon imageiconScreen = null;

	public static Image imageIndicator_Granule = null;
	public static Image imageIndicator_Directory = null;
	public static Image imageIndicator_Catalog = null;
	public static Image imageIndicator_Binary = null;
	public static Image imageIndicator_Image = null;
	public static Image imageConstrained = null;
	public static Image imageArrowUp = null;
	public static Image imageArrowDown = null;
	public static Image imageNavigateMinus = null;
	public static Image imageNavigatePlus = null;

	public static enum Icons {
		SplashScreen,
		Internet,
		Calculator,
		DisplayScreen,
		Granule,
		Directory,
		Catalog,
		Binary,
		Image,
		Constrained,
		ArrowUp,
		ArrowDown,
		Minus,
		Plus
	}
	
    public Resources(){}

	public static javax.swing.ImageIcon imageiconLoadResource( String sResourcePath, StringBuffer sbError ){
		java.awt.Image image = imageLoadResource( sResourcePath, sbError );
		if( image == null ) return null;
		return new javax.swing.ImageIcon(image);
	}

	public static java.awt.image.BufferedImage loadBufferedImage( String sResourcePath, StringBuffer sbError ){
		java.net.URL url = ApplicationController.getInstance().getClass().getResource(sResourcePath);
		if( url == null ){
			sbError.append("image resource (" + sResourcePath + ") not found (was missing from the class path or jar file)");
			return null;
		}
		try {
			return javax.imageio.ImageIO.read( url );
		} catch( Throwable t ) {
			sbError.append("Error reading [" + sResourcePath + "]: " + t );
			return null;
		}
	}
	
	// icon delivery is set up this way so that icon fetches can occur in an ad hoc manner without
	// all icons having to be loaded; this is useful when running an isolated panel in testing, for example
	// in such a case the regular app may not have been started so no general load of all icons will have occurred
	public static ImageIcon getIcon( Icons eIcon ){
		String sBasePath = "/opendap/clients/odc/icons/";
		String sIconFileName = null;
		switch( eIcon ){
			case SplashScreen:
				break;
			case Internet:
				break;
			case Calculator:
				break;
			case DisplayScreen:
				if( imageiconScreen == null ){
					sIconFileName = "computer_monitor.png";
				} else return imageiconScreen;
				break;
			case Granule:
				break;
			case Directory:
				break;
			case Catalog:
				break;
			case Binary:
				break;
			case Image:
				break;
			case Constrained:
				break;
			case ArrowUp:
				break;
			case ArrowDown:
				break;
			case Minus:
				break;
			case Plus:
				break;
			default:
				// question mark?
		}
		StringBuffer sbError = new StringBuffer();
		imageiconScreen = Resources.imageiconLoadResource( sBasePath + sIconFileName, sbError );
		if( imageiconScreen == null ){
			ApplicationController.vShowError_NoModal( sIconFileName + " icon not loaded: " + sbError );
			sbError.setLength( 0 );
		}
		return imageiconScreen;
	}

	
	public static java.awt.Image imageLoadResource( String sResourcePath, StringBuffer sbError ){
		try {
			java.net.URL url = ApplicationController.getInstance().getClass().getResource( sResourcePath );
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
				ApplicationController.vShowWarning("Unknown icon size [" + sIconSize + "]. Supported sizes are \"16\", \"24\" and \"32\".");
				return;
			}
			sIconPath = Utility.sConnectPaths("/opendap/clients/odc", "/", sIconPath);
			StringBuffer sbError = new StringBuffer();
			javax.swing.ImageIcon icon = imageiconLoadResource( sIconPath, sbError );
			if( icon == null ){
				ApplicationController.vShowWarning("failed to load icon " + sIconPath + ": " +sbError);
				return;
			}
			theFrame.setIconImage(icon.getImage());
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Unexpected error setting up icon. Default icon will appear.");
		}
	}

	public static boolean zLoadIcons( StringBuffer sbError ){
		String sPath = "/opendap/clients/odc/icons/";
		try {
			String pathSplashScreen = Utility.sConnectPaths( dirImages, "splash-1.png" );
			// System.out.println( "ss: " + pathSplashScreen );
			imageiconSplash = imageiconLoadResource(pathSplashScreen, sbError);
			if( imageiconSplash == null ){
				ApplicationController.vShowError_NoModal("Splash screen [" + pathSplashScreen + "] not loaded: " + sbError);
				sbError.setLength( 0 );
			}

			imageiconInternet = imageiconLoadResource(pathICONS_InternetConnection, sbError );
			if( imageiconInternet == null ){
				ApplicationController.vShowError_NoModal("Internet icon [" + pathICONS_InternetConnection + "] not loaded: " + sbError);
				sbError.setLength( 0 );
			}

			imageiconCalculator = Resources.imageiconLoadResource( sPath + "calculator.gif", sbError );
			if( imageiconCalculator == null ){
				ApplicationController.vShowError_NoModal("Calculator icon not loaded: " + sbError);
				sbError.setLength( 0 );
			}

			imageiconScreen = Resources.imageiconLoadResource( sPath + "computer_monitor.png", sbError );
			if( imageiconScreen == null ){
				ApplicationController.vShowError_NoModal("Display screen icon not loaded: " + sbError);
				sbError.setLength( 0 );
			}
			
			javax.swing.ImageIcon image;

			image = imageiconLoadResource(sPath + "dataset-granule.gif", sbError);
			if( image == null ){
				sbError.insert( 0, "icon " + sPath + "dataset-granule.gif" + " not found: " );
				return false;
			}
			imageIndicator_Granule = image.getImage();

			image = imageiconLoadResource(sPath + "dataset-directory.gif", sbError);
			if( image == null ){
				sbError.insert( 0, "icon " + sPath + "dataset-directory.gif" + " not found: " );
				return false;
			}
			imageIndicator_Directory = image.getImage();

			image = imageiconLoadResource(sPath + "dataset-catalog.gif", sbError);
			if( image == null ){
				sbError.insert( 0, "icon " + sPath + "dataset-catalog.gif" + " not found: " );
				return false;
			}
			imageIndicator_Catalog = image.getImage();

			image = imageiconLoadResource(sPath + "dataset-binary.gif", sbError);
			if( image == null ){
				sbError.insert( 0, "icon " + sPath + "dataset-binary.gif" + " not found: " );
				return false;
			}
			imageIndicator_Binary = image.getImage();

			image = imageiconLoadResource(sPath + "dataset-image.gif", sbError);
			if( image == null ){
				sbError.insert( 0, "icon " + sPath + "dataset-image.gif" + " not found: " );
				return false;
			}
			imageIndicator_Image = image.getImage();

			image = imageiconLoadResource(sPath + "constrained.gif", sbError);
			if( image == null ){
				sbError.insert( 0, "icon " + sPath + "constrained.gif" + " not found: " );
				return false;
			}
			imageConstrained = image.getImage();

			image = imageiconLoadResource(sPath + "arrow_up_blue.png", sbError);
			if( image == null ){
				sbError.insert( 0, "icon " + sPath + "arrow_up_blue.png" + " not found: " );
				return false;
			}
			imageArrowUp = image.getImage();

			image = imageiconLoadResource(sPath + "arrow_down_blue.png", sbError);
			if( image == null ){
				sbError.insert( 0, "icon " + sPath + "arrow_down_blue.png" + " not found: " );
				return false;
			}
			imageArrowDown = image.getImage();

			image = imageiconLoadResource(sPath + "navigate_plus.png", sbError);
			if( image == null ){
				sbError.insert( 0, "icon " + sPath + "navigate_plus.png" + " not found: " );
				return false;
			}
			imageNavigatePlus = image.getImage();

			image = imageiconLoadResource(sPath + "navigate_minus.png", sbError);
			if( image == null ){
				sbError.insert( 0, "icon " + sPath + "navigate_minus.png" + " not found: " );
				return false;
			}
			imageNavigateMinus = image.getImage();
			
			return true;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError( ex, sbError );
			return false;
		}
	}

}



