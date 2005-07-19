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





