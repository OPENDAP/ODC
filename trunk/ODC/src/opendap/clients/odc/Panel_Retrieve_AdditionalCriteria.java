package opendap.clients.odc;

/**
 * Title:        Panel_Retrieve_AdditionalCriteria
 * Description:  Features dynamic criteria controls for sub-selecting datasets
 * Copyright:    Copyright (c) 2002-4
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.48
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import opendap.dap.*;

public class Panel_Retrieve_AdditionalCriteria extends JPanel {

	private Model_Retrieve model;

	private JTextArea mjtaInfo;
	private JScrollPane mjscrollInfo;
	private Panel_Retrieve_DDX mPanel_DDX;

	Panel_Retrieve_AdditionalCriteria() {}

    boolean zInitialize( StringBuffer sbError ){

        try {

			model = ApplicationController.getInstance().getRetrieveModel();
			if( model == null ){
				sbError.append("no model");
				return false;
			}

			setLayout(new BorderLayout());
			Border borderEmpty = new EmptyBorder(0, 0, 0, 0);

			// DDX Panel
			mPanel_DDX = new Panel_Retrieve_DDX();
			if( !mPanel_DDX.zInitialize(this, sbError) ){
				sbError.insert(0, "Failed to initialize DDX panel");
				return false;
			}
			mPanel_DDX.setBorder(borderEmpty);

			// Info Panel
			mjscrollInfo = new JScrollPane();
			mjtaInfo = new JTextArea();
			mjtaInfo.setBorder(borderEmpty);
			mjscrollInfo.setViewportView(mjtaInfo);

            return true;

        } catch(Exception ex){
            sbError.insert(0, "Unexpected error: " + ex);
            return false;
        }
	}

	Panel_Retrieve_DDX getPanelDDX(){
	    return mPanel_DDX;
	}

//	boolean zShowURL( DodsURL urlToConstrain, StringBuffer sbError ){
//		try {
//			if( ApplicationController.getInstance().isAutoUpdating() ){
//				SwingUtilities.invokeLater(
//					new Runnable(){
//						public void run(){
//		    				JOptionPane.showMessageDialog(null, "Still retrieving structure information (check progress bar in bottom right of screen).");
//						}
//					}
//				);
//				return true;
//			}
//		    if( urlToConstrain == null ){
//				vUpdate("[no URL selected]");
//			} else {
//				if( zUpdate( urlToConstrain, sbError) ){
//					// success
//				} else {
//					sbError.insert(0, "failed to update constraint: ");
//					return false;
//				}
//			}
//			return true;
//		} catch(Exception ex) {
//			sbError.append("Unexpected error: " + ex);
//			return false;
//		}
//	}



	/** In the case that the url is a directory then the dds should be the DDS of a typical file in the directory */
    public boolean zShowStructure( DodsURL url, StringBuffer sbError) {
		try {
			vClear();
			if( url == null ){
			   sbError.append("No URL supplied");
			   model.setLocationString("");
			   return false;
			}
			if( url.getDDS_Full() == null ){
			   sbError.append("internal error, no structure supplied");
			   model.setLocationString("");
			   return false;
			}
			final int iURLType = url.getType();
			if( !mPanel_DDX.setStructure(url, sbError) ) return false;
			model.setLocationString(url.getFullURL());
			add( mPanel_DDX );
			return true;
		} catch(Exception e) {
			vShowText("Constraint expression modification unavailable: " + e);
			Utility.vUnexpectedError( e, sbError );
			return false;
		} finally {
			ApplicationController.getInstance().getRetrieveModel().vValidateRetrieval();
		}
    }

	public void vShowText( String sInfo ){
		try {
			removeAll();
			mjtaInfo.setText(sInfo);
			add(mjscrollInfo);
			invalidate();
			ApplicationController.getInstance().getRetrieveModel().vValidateRetrieval();
		} catch(Exception ex) {
			StringBuffer sbError = new StringBuffer("showing retrieve text");
			Utility.vUnexpectedError( ex, sbError );
			ApplicationController.getInstance().vShowError(sbError.toString());
		}
	}

	public void vClear(){
		removeAll();
		model.setLocationString("");
	}

}

