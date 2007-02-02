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

package opendap.clients.odc.GCMD;

import javax.swing.table.*;
import java.lang.*;

public class GCMDTableModel extends AbstractTableModel {
    final String[] columnNames = { "Title" };

    private Object[][] data;

    public GCMDTableModel() {}

	public void setData(Object[] id){
		if( id == null ){
			data = null;
		} else {
			data = new Object[id.length][1];
			for(int i=0;i<id.length;i++) {
				data[i][0] = id[i];
			}
		}
		this.fireTableStructureChanged();
	}

    public int getColumnCount() {
		if( data == null ) return 0;
		return columnNames.length;
    }

    public int getRowCount() {
		if( data == null ) return 0;
		return data.length;
    }

    public String getColumnName(int col) {
		if( col < 0 || col > columnNames.length-1 ) return "";
		return columnNames[col];
    }

    public Object getValueAt(int row, int col) {
		if( data == null ) return null;
		if( row < 0 || row >= data.length || col < 0 || col > 0 ) return null;
		return data[row][col];
    }

    /*
     * JTable uses this method to determine the default renderer/
     * editor for each cell.  If we didn't implement this method,
     * then the last column would contain text ("true"/"false"),
     * rather than a check box.
     */
    public Class getColumnClass(int c) {
		return getValueAt(0, c).getClass();
    }

    public boolean isCellEditable(int row, int col) {
		//Note that the data/cell address is constant,
		//no matter where the cell appears onscreen.
		return false;
    }

}


