package opendap.clients.odc.gui;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

// returns folder icon instead of page icon in the case of a leaf
public class LeaflessTreeCellRenderer extends DefaultTreeCellRenderer {
    public Component getTreeCellRendererComponent(JTree tree, Object value,
						  boolean sel,
						  boolean expanded,
						  boolean leaf, int row,
						  boolean hasFocus) {
		Component componentDefault = super.getTreeCellRendererComponent( tree, value, sel, expanded, leaf, row, hasFocus );
		if( tree.isEnabled() ){
			if( leaf ){
				setIcon(getClosedIcon());
			}
		} else {
			if( leaf ){
				setDisabledIcon( getClosedIcon() );
			}
		}
		return componentDefault;
    }
}
