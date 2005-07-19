package opendap.clients.odc;

import java.awt.Font;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

public class Button_Select extends JButton implements ListSelectionListener {
	public Button_Select(final SearchInterface searchable){
		super();
		Styles.vApply( Styles.STYLE_NetworkAction, this );
		setText("To Retrieve");
		setEnabled(false);
		addActionListener(
			new ActionListener(){
	    		public void actionPerformed(ActionEvent event) {
		    		searchable.vAddSelected();
			    }
			}
		);
		searchable.addListSelectionListener( this );
	}
    public void valueChanged(ListSelectionEvent e) {
		Object oSource = e.getSource();
		if( oSource instanceof ListSelectionModel ){
			if( ((ListSelectionModel)e.getSource()).isSelectionEmpty() ){
				setEnabled(false);
			} else {
				setEnabled(true);
			}
		} else if( oSource instanceof JTree ) {
			JTree jtree = (JTree)oSource;
			TreeModel tm = jtree.getModel();
			TreeSelectionModel tsm = jtree.getSelectionModel();
			if( tsm.isSelectionEmpty() ){
				setEnabled(false);
			} else {
				TreePath[] tp = tsm.getSelectionPaths();
				if (tp == null) {
					setEnabled(false);
				} else {
					for (int i=0; i < tp.length; i++) {
						Object oNode = tp[i].getLastPathComponent();
						if( tm.isLeaf(oNode) ){
							setEnabled(true);
							return;
						}
					}
					setEnabled(false);
				}
			}
		}
	}
}



