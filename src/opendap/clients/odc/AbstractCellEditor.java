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

import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.event.MouseEvent;
import java.util.EventObject;

abstract public class AbstractCellEditor implements TableCellEditor {
	protected EventListenerList listenerList = new EventListenerList();
	protected Object value;
	protected ChangeEvent changeEvent = null;
	protected int clickCountToStart = 1;

	public Object getCellEditorValue() {
		return value;
	}
	public void setCellEditorValue(Object value) {
		this.value = value;
	}
	public void setClickCountToStart(int count) {
		clickCountToStart = count;
	}
	public int getClickCountToStart() {
		return clickCountToStart;
	}
	public boolean isCellEditable(EventObject anEvent) {
		if (anEvent instanceof MouseEvent) {
			if (((MouseEvent)anEvent).getClickCount() <
												clickCountToStart)
				return false;
		}
		return true;
	}
	public boolean shouldSelectCell(EventObject anEvent) {
		return true;
	}
	public boolean stopCellEditing() {
		fireEditingStopped();
		return true;
	}
	public void cancelCellEditing() {
		fireEditingCanceled();
	}
	public void addCellEditorListener(CellEditorListener l) {
		listenerList.add(CellEditorListener.class, l);
	}
	public void removeCellEditorListener(CellEditorListener l) {
		listenerList.remove(CellEditorListener.class, l);
	}
	protected void fireEditingStopped() {
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i] == CellEditorListener.class) {
				if (changeEvent == null)
					changeEvent = new ChangeEvent(this);
				((CellEditorListener)
				listeners[i+1]).editingStopped(changeEvent);
			}
		}
	}
	protected void fireEditingCanceled() {
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==CellEditorListener.class) {
				if (changeEvent == null)
					changeEvent = new ChangeEvent(this);
				((CellEditorListener)
				listeners[i+1]).editingCanceled(changeEvent);
			}
		}
	}
}


