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

package opendap.clients.odc.gui;

import java.awt.*;
import javax.swing.*;

import opendap.clients.odc.IControlPanel;
import opendap.clients.odc.data.Model_Dataset;

public class Panel_Retrieve extends JPanel implements IControlPanel {

	private Panel_Retrieve_Location panelLocation;
	private Panel_Retrieve_SelectedDatasets panelSelectedDatasets;
	private Panel_Retrieve_AdditionalCriteria panelAdditionalCriteria;
	private Panel_Retrieve_Output panelOutput_below;
	private javax.swing.JSplitPane splitpaneRetrieve;
	private javax.swing.border.Border borderOutput;
	private JPanel panelRight;

	final public boolean zInitialize( StringBuffer sbError ){
		panelLocation = new Panel_Retrieve_Location();
		panelSelectedDatasets = new Panel_Retrieve_SelectedDatasets();
		panelAdditionalCriteria = new Panel_Retrieve_AdditionalCriteria();
		panelOutput_below = new Panel_Retrieve_Output();

		if( !panelLocation.zInitialize(sbError) ){
			sbError.insert(0, "Failed to initialize location panel: ");
			return false;
		}

		if( !panelSelectedDatasets.zInitialize(sbError) ){
			sbError.insert(0, "Failed to initialize selected datasets panel: ");
			return false;
		}

		if( !panelAdditionalCriteria.zInitialize( sbError) ){
			sbError.insert(0, "Failed to initialize additional criteria panel: ");
			return false;
		}

		if( !panelOutput_below.zInitialize(sbError) ){
			sbError.insert(0, "Failed to initialize output panel below: ");
			return false;
		}

		// set up borders
		javax.swing.border.Border borderEtched = BorderFactory.createEtchedBorder();
		javax.swing.border.Border borderSelected = BorderFactory.createTitledBorder(borderEtched, "Datasets");
		panelSelectedDatasets.setBorder(borderSelected);
		javax.swing.border.Border borderCriteria = BorderFactory.createTitledBorder(borderEtched, "Additional Criteria");
		borderOutput = BorderFactory.createTitledBorder(borderEtched, "Output Settings");
		panelAdditionalCriteria.setBorder(borderCriteria);
		panelOutput_below.setBorder(borderOutput);

		JPanel panelLeft = new JPanel();
		panelLeft.setBorder(BorderFactory.createEmptyBorder());
		panelLeft.setLayout(new BorderLayout());
		panelLeft.add(panelSelectedDatasets, BorderLayout.CENTER);

		panelRight = new JPanel();
		panelRight.setBorder(BorderFactory.createEmptyBorder());
		panelRight.setLayout(new BorderLayout());
		panelRight.add( panelAdditionalCriteria, BorderLayout.CENTER );
		panelRight.add( panelOutput_below, BorderLayout.SOUTH );

        splitpaneRetrieve = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelLeft, panelRight );

		setLayout(new java.awt.BorderLayout());
		add(panelLocation, java.awt.BorderLayout.NORTH);
		add(splitpaneRetrieve, java.awt.BorderLayout.CENTER);

		panelSelectedDatasets.vClear();

//		ComponentAdapter ca = new ComponentAdapter(){
//			public void componentResized(ComponentEvent e) {
//				vUpdateOutputPanelLocation();
//			}
//		};
//		addComponentListener(ca);

		return true;
	}

    public void _vSetFocus(){    	
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				panelSelectedDatasets.requestFocus();
			}
		});
    }

    public Panel_Retrieve_Output getOutputPanel(){ return panelOutput_below; }

// just not working
//	boolean zUsingBelow = true;
//	void vUpdateOutputPanelLocation(){
//		int pxCriteriaPanelWidth = panelAdditionalCriteria.getWidth();
//		int pxNominalOutputPanelWidth = 400;
//		if(  pxCriteriaPanelWidth > pxNominalOutputPanelWidth && zUsingBelow ){
//			panelOutput_below.setVisible( false );
//			if( panelAdditionalCriteria.getPanelDDX() != null &&
//				    panelAdditionalCriteria.getPanelDDX().panelOutput_right != null ){
//					    panelAdditionalCriteria.getPanelDDX().panelOutput_right.setVisible( true );
//						panelAdditionalCriteria.getPanelDDX().panelOutput_right.invalidate();
//			}
//		} else if( !zUsingBelow ) {
//			panelOutput_below.setVisible( true );
//			if( panelAdditionalCriteria.getPanelDDX() != null &&
//				    panelAdditionalCriteria.getPanelDDX().panelOutput_right != null ){
//					    panelAdditionalCriteria.getPanelDDX().panelOutput_right.setVisible( false );
//						panelAdditionalCriteria.getPanelDDX().panelOutput_right.invalidate();
//			}
//		}
//		panelAdditionalCriteria.invalidate();
//		panelSelectedDatasets.invalidate();
//		panelOutput_below.invalidate();
//		revalidate();
//	}

	final public Panel_Retrieve_DDX getPanelDDX(){
		if( panelAdditionalCriteria == null ) return null;
		return panelAdditionalCriteria.getPanelDDX();
	}

	final public Panel_Retrieve_Directory getPanelDirectory(){
		if( panelAdditionalCriteria == null ) return null;
		return panelSelectedDatasets.getPanelDirectory();
	}

	final public Panel_Retrieve_Location getPanel_Location(){ return this.panelLocation; }

	final public void vAdditionalCriteria_Clear(){
		SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					panelAdditionalCriteria.vClear();
				}
			}
		);
	}

	final public void vClearSelection(){
		SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					getPanelDDX().vClear();
					panelSelectedDatasets.vClear();
				}
			}
		);
	}

	final public boolean zShowStructure( Model_Dataset url, StringBuffer sbError ){
		return panelAdditionalCriteria.zShowStructure( url, sbError );
	}

	final public void vAdditionalCriteria_ShowText( final String sMessage ){
		SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					panelAdditionalCriteria.vShowText( sMessage );
				}
			}
		);
	}

	final public void vShowDirectory( final boolean zShow ){
		SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					panelSelectedDatasets.vShowDirectory(zShow);
				}
			}
		);
	}

	final public void setLocationString( final String sLocation ){
		SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					panelLocation.setLocationString(sLocation);
				}
			}
		);
	}

	final public void setEstimatedDownloadSize( final String sSize ){
		SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					panelOutput_below.setEstimatedDownloadSize( sSize );
				}
			}
		);
	}
}
