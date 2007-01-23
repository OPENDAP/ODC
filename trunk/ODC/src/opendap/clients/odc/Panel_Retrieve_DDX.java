package opendap.clients.odc;

/**
 * Title:        Panel_Retrieve_DDX
 * Description:  Creates subsetting display in retrieval panel
 * Copyright:    Copyright (c) 2002-4
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.48
 */

import java.lang.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import opendap.dap.*;

public class Panel_Retrieve_DDX extends JPanel {

	private Panel_Retrieve_AdditionalCriteria myParent;

	private Model_Retrieve model;

	private DodsURL mURL = null;
	private DDSSelector mDDSS = null;
	private JPanel mpanelControl;
	private JScrollPane mscrollpane_Criteria;
	private final JCheckBox jcheckShowDescriptions = new JCheckBox(" details");
	private boolean mzShowingDescriptions = false;
	private final static JLabel jlabelStride = new JLabel("Step:");
	private final static String[] masStrideValues = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "15", "20", "25", "30", "40", "50", "75", "100" };
	private final JComboBox jcbStride = new JComboBox( masStrideValues );

    public Panel_Retrieve_DDX(){}

	Dimension dimMinimum = new Dimension(10, 50);
	public Dimension getMinimumSize(){
		return dimMinimum;
	}

	boolean zInitialize( Panel_Retrieve_AdditionalCriteria theParent, StringBuffer sbError ){

		try {

			if( theParent == null ){
				sbError.append("no parent supplied");
				return false;
			}
			myParent = theParent;

			model = ApplicationController.getInstance().getRetrieveModel();
			if( model == null ){
				sbError.append("no model");
				return false;
			}

			setLayout(new BorderLayout());
			setBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0));

			// scroll panes
			mscrollpane_Criteria = new JScrollPane();
			mscrollpane_Criteria.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			mscrollpane_Criteria.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			// button setup
			mpanelControl = new JPanel();
			mpanelControl.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
			mpanelControl.setLayout(new FlowLayout(FlowLayout.LEFT));

			// select all button
			JButton jbuttonSelectAll = new JButton("All");
			jbuttonSelectAll.addActionListener(
				new ActionListener(){
					public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
						mDDSS.vSelectAll( true );
					}
				}
			);
			mpanelControl.add(jbuttonSelectAll);

			// select none button
			JButton jbuttonSelectNone = new JButton("None");
			jbuttonSelectNone.addActionListener(
				new ActionListener(){
					public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
						mDDSS.vSelectAll( false );
					}
				}
			);
			mpanelControl.add(jbuttonSelectNone);

			jcbStride.setSelectedIndex(0); // 1
			jcbStride.addActionListener(
				new ActionListener(){
					public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
						if( mURL != null ){
							try {
								int iStep = Integer.parseInt(jcbStride.getSelectedItem().toString());
								vUpdateStep( iStep );
							} catch(Exception ex) {
								Utility.vUnexpectedError(ex, "Error changing stride");
							}
						}
					}
				}
			);
			jcheckShowDescriptions.setVisible(false);
			jcheckShowDescriptions.addActionListener(
				new ActionListener(){
					public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
						if( mURL != null ){
							mzShowingDescriptions = jcheckShowDescriptions.isSelected();
							if( mDDSS == null ){
								// should not happen
							} else {
								mDDSS.vUpdateInfo( mzShowingDescriptions );
							}
						}
					}
				}
			);

			mpanelControl.add(Box.createHorizontalStrut(3));
			mpanelControl.add(jlabelStride);
			mpanelControl.add(Box.createHorizontalStrut(2));
			mpanelControl.add(jcbStride);
			mpanelControl.add(Box.createHorizontalStrut(3));
			mpanelControl.add(jcheckShowDescriptions);
			mpanelControl.add(Box.createHorizontalStrut(3));
			mpanelControl.add(Box.createHorizontalGlue());

			add( mscrollpane_Criteria, BorderLayout.CENTER );
			add( mpanelControl, BorderLayout.SOUTH );

			return true;

		} catch( Exception ex ) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	boolean zShowDescriptions(){ return mzShowingDescriptions; }

	Component glue = Box.createGlue();
	void vClear(){
		SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					jcbStride.setSelectedIndex(0);
					mscrollpane_Criteria.setViewportView(glue);
					mscrollpane_Criteria.invalidate();
					ApplicationController.getInstance().getAppFrame().getPanel_Retrieve().getOutputPanel().vUpdateOutput_Blank();
				}
			}
		);
	}

	JTextArea jtaMessage = new JTextArea();
	void setRetrieveMessage( final String sMessage ){
		SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					jtaMessage.setText( sMessage );
					mscrollpane_Criteria.setViewportView(jtaMessage);
					ApplicationController.getInstance().getAppFrame().getPanel_Retrieve().getOutputPanel().vUpdateOutput_Text();
				}
			}
		);
	}

	String getRetrieveMessage(){ return jtaMessage.getText(); }

	boolean setStructure( DodsURL url, StringBuffer sbError ){
		try {
			if( url == null ){
				vClear();
				return true;
			}
			jcbStride.setSelectedIndex(0);
			mURL = url;
			final DDS dds = url.getDDS_Full();
			final DAS das = url.getDAS();
			ApplicationController.getInstance().setConstraintChanging(true);
			if( dds == null ){
				vClear();
				return true;
			}
			mDDSS = new DDSSelector(this);
			if( mDDSS == null ){
				sbError.append("Failed to create DDS selector");
				return false;
			}
			if( !mDDSS.zSetDDS( dds, das, sbError ) ){
				sbError.append("Failed to set structure for DDS selector");
				return false;
			}
			SwingUtilities.invokeLater(
				new Runnable(){
					public void run(){
						jcheckShowDescriptions.setVisible(das != null);
						mDDSS.vSelectAll(false);
						mscrollpane_Criteria.setViewportView(mDDSS);
						mscrollpane_Criteria.invalidate();
					}
				}
			);
			return true;
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		} finally {
			mscrollpane_Criteria.invalidate();
			ApplicationController.getInstance().setConstraintChanging(false);
		}
	}

	private void vUpdateStep( int iStep ){
		if( mDDSS != null ) mDDSS.vUpdateStep( iStep );
	}

	public DodsURL getURL(){ return mURL; }

	/* when a CE is changed this becomes the master CE for the active top-level URL, thus
	   for a directory, all files in the directory will share the same CE which is the master
	   CE set here
	*/
	static boolean mzApplyingSubset = false;
	public void vApplySubset(){
		if( mzApplyingSubset ){
			System.out.println("recursive error");
			Thread.dumpStack();
			return;
		}
		try {
			mzApplyingSubset = true;
			if( mDDSS == null ){
				ApplicationController.getInstance().vShowError("Internal Error, attempt to apply subset to null DDS");
			} else {
				if( mURL == null ){
					ApplicationController.getInstance().vShowWarning("Internal error, attempt to apply subset to null url");
					return;
				}
				String sCE = mDDSS.generateCE("");
				mURL.setConstraintExpression(sCE);
				DodsURL urlDirectory = model.retrieve_panel.getPanelDirectory().getURL_directory();
				if( urlDirectory == null ){
					ApplicationController.getInstance().vShowWarning("unable to set constraint, no directory URL defined");
				} else {
					urlDirectory.setConstraintExpression(sCE);
					model.retrieve_panel.validate();
				}
				ApplicationController.getInstance().getRetrieveModel().vValidateRetrieval();
				if( mURL != null ) model.setLocationString(mURL.getFullURL());
			}
		} catch(Exception ex) {
			StringBuffer sbError = new StringBuffer("applying subset");
			Utility.vUnexpectedError(ex, sbError);
		} finally {
			mzApplyingSubset = false;
		}
	}

	public void vApplySelections(){
		try {
			if( mDDSS == null ){
				ApplicationController.getInstance().vShowError("Internal Error, attempt to apply selection to null DDS");
			} else {
				mDDSS.vUpdateSelections();
			}
		} catch(Exception ex) {
			StringBuffer sbError = new StringBuffer("applying subset");
			Utility.vUnexpectedError(ex, sbError);
		}
	}

}



