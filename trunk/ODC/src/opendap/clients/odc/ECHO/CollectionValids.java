package opendap.clients.odc.ECHO;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;

/**
 * Manages the valids for discovery search
 *
 * @author Sheila Jiang <jiangz@po.gso.uri.edu>
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

public class CollectionValids extends JList implements ListSelectionListener {

	private boolean selected;
	private String name;
	private String[] valids;
	private boolean[] selection;

	/**
	 * Constructs <code>CollectionValids</code>
	 *
	 * @param theName the category name of the valids
	 * @param theValids the vulues of the valids
	 */

	public CollectionValids(String theName, String[] theValids){
		super(theValids);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		addListSelectionListener(this);
		selected = false;
		name = theName;
		valids = theValids;
		selection = new boolean[valids.length];
		for (int i=0; i<valids.length; i++){
			selection[i] = false;
		}
	}

	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) return;
		if (!isSelectionEmpty()) {
			for(int i=0; i<valids.length;i++){
				if (isSelectedIndex(i)) setSelected(i);
				else deSelect(i);
			}
		}
	}

	public String getName(){
		return name;
	}

	public boolean isSelected(){
		return selected;
	}

	public void setSelected(){
		selected = true;
	}

	public void deSelect(){
		selected = false;
	}

	public String[] getValids(){
		return valids;
	}

	public void setValids(String[] theValids){
		valids = theValids;
		setListData(valids);
	}

	public boolean getSelection(int index){
		return selection[index];
	}

	public void setSelected(int index){
		selection[index] = true;
	}

	public void deSelect(int index){
		selection[index] = false;
	}

}





