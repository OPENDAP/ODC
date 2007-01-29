package opendap.clients.odc;

/**
 * Title:        Panel_Retrieve_Location
 * Description:  Location bar at the top of the retrieve tab
 * Copyright:    Copyright (c) 2002
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.57
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Panel_Retrieve_Location extends JPanel {

	Panel_Retrieve_Location() {}

	private Model_Retrieve model;
    private javax.swing.JTextField jtfLocation;
	private JButton jbuttonAddToList;

    boolean zInitialize(StringBuffer sbError){

        try {

			model = ApplicationController.getInstance().getRetrieveModel();
			if( model == null ){
				sbError.append("no model");
				return false;
			}

			javax.swing.border.Border borderStandard = BorderFactory.createEtchedBorder();
			this.setBorder(borderStandard);
			this.setLayout(new GridBagLayout());
			JLabel jlabelLocation = new JLabel("Location:");
			jtfLocation = new javax.swing.JTextField();

			final JButton jbuttonAddToList = new JButton();
			// jbuttonAddToList_ViewPanel.setForeground(Color.RED);
			jbuttonAddToList.setText("Add To List");
			final ActionListener actionGo =
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						String sLocationString = Panel_Retrieve_Location.this.getLocationString();
						if( sLocationString == null ){
							JOptionPane.showMessageDialog(null, "no location string");
							return;
						}
						sLocationString = sLocationString.trim();
						if( sLocationString.length() == 0 ){
							JOptionPane.showMessageDialog(null, "location string is blank");
							return;
						}
						model.vEnterURLByHand(sLocationString);
					}
				};
			jbuttonAddToList.addActionListener(actionGo);

			java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();

			gbc.insets = new Insets(2, 4, 2, 3);
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 0;
			gbc.fill = gbc.NONE;
			this.add(jlabelLocation, gbc);

			gbc.insets = new Insets(2, 0, 2, 5);
			gbc.fill = gbc.HORIZONTAL;
			gbc.gridx = 1;
			gbc.weightx = 1;
			this.add(jtfLocation, gbc);

			gbc.insets = new Insets(2, 0, 2, 2);
			gbc.gridx = 2;
			gbc.weightx = 0;
			this.add(jbuttonAddToList, gbc);

            return true;

        } catch(Exception ex){
            Utility.vUnexpectedError(ex, sbError);
            return false;
        }

    }

	void setLocationString( final String sLocation ){
		SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					if( jtfLocation == null ) return;
					String s = ( sLocation == null ) ? "" : sLocation;
					jtfLocation.setText(s);
				}
			}
		);
	}

	String getLocationString(){
		if( jtfLocation == null ) return "";
		return jtfLocation.getText();
	}

}



