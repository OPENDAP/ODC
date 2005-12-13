package opendap.clients.odc;

/**
 * Title:        DatasetListRenderer
 * Description:  Renders JList cells displaying dataset URLs
 * Copyright:    Copyright (c) 2002
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.0
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.border.*;

// todo support deletes

class DatasetListRenderer extends JLabel implements ListCellRenderer, TableCellRenderer, ListDataListener {
	protected Border mBorder_FocusCell = BorderFactory.createLineBorder(Color.cyan,1);
	protected Border mBorder_RegularCell = BorderFactory.createEmptyBorder(1,1,1,1);
	protected Insets mInsets = new Insets(1, 1, 1, 1);
	private int mCategoryX, mTextX, mTextY;
	protected boolean isURL;
	protected DodsURL mURL;  // these two values are either/or: either the cell is an URL or its treated as a string
	protected int mIndex;
	protected String sTextValue = "";
	private boolean mzDiscovered = false;
	protected boolean mzShowTypeIndicators = false;
	protected boolean mzShowUndiscoveredAsRed = false;
	private int miIndicatorWidth;
	private static Image imageIndicator_Granule;
	private static Image imageIndicator_Directory;
	private static Image imageIndicator_Catalog;
	private static Image imageIndicator_Binary;
	private static Image imageIndicator_Image;
	private static Image imageConstrained;
	private static int miColorCount = 12;
	private Color[] macolorContent_Type;
	private int[] maiDigestList = new int[13]; // the zeroeth element is the pointer
	private static Color[] macolorScheme = new Color[13];
	static {
		macolorScheme[1] = Color.black;
		macolorScheme[2] = Color.blue;
		macolorScheme[3] = Color.yellow;
		macolorScheme[4] = Color.orange;
		macolorScheme[5] = Color.cyan;
		macolorScheme[6] = Color.darkGray;
		macolorScheme[7] = Color.green;
		macolorScheme[8] = Color.magenta;
		macolorScheme[9] = Color.red;
		macolorScheme[10] = Color.pink;
		macolorScheme[11] = Color.white;
		macolorScheme[12] = Color.lightGray;
	}
	public void vInitialize(int iSize, boolean zShowUndiscoveredAsRed){
		imageIndicator_Granule = Utility.imageIndicator_Granule;
		imageIndicator_Directory = Utility.imageIndicator_Directory;
		imageIndicator_Catalog = Utility.imageIndicator_Catalog;
		imageIndicator_Binary = Utility.imageIndicator_Binary;
		imageIndicator_Image = Utility.imageIndicator_Image;
		imageConstrained = Utility.imageConstrained;
		setOpaque(true);
		this.setMinimumSize(new Dimension(80, 20));
		this.setPreferredSize(new Dimension(400, 20));
		int iClassIndicatorWidth = (imageIndicator_Granule==null) ? 0 : imageIndicator_Granule.getWidth(null);
		if( mzShowTypeIndicators ){
			miIndicatorWidth = 14 + iClassIndicatorWidth; // 12 is the width of the type indicator
			mCategoryX = mInsets.left + 14;
		} else {
			miIndicatorWidth = 3 + iClassIndicatorWidth; // 12 is the width of the type indicator
			mCategoryX = 3 + mInsets.left;
		}
		mTextX = mInsets.left + miIndicatorWidth + 2;
		macolorContent_Type = new Color[iSize];
		maiDigestList[0] = 0; // in the beginning none of the colors are used
	}
	public DatasetListRenderer(JTable table, boolean zShowTypeIndicators, boolean zShowUndiscoveredAsRed){
		super();
		setFont(table.getFont());
		mzShowTypeIndicators = zShowTypeIndicators;
		this.vInitialize(table.getModel().getRowCount(), zShowUndiscoveredAsRed);
	}
	public DatasetListRenderer(JList list, boolean zShowTypeIndicators, boolean zShowUndiscoveredAsRed){
		super();
		setFont(list.getFont());
		mzShowTypeIndicators = zShowTypeIndicators;
		this.vInitialize(list.getModel().getSize(), zShowUndiscoveredAsRed);
	}
	public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean zCellHasFocus ){
		if( isSelected ){
			setForeground(list.getSelectionForeground());
			setBackground(list.getSelectionBackground());
		} else {
			setForeground(list.getForeground());
			setBackground(list.getBackground());
		}
		if( zCellHasFocus ){
			setBorder(mBorder_FocusCell);
		} else {
			setBorder(mBorder_RegularCell);
		}
		if( list.getModel().getSize() != macolorContent_Type.length )
			macolorContent_Type = new Color[list.getModel().getSize()];
		mIndex = index;
		if( value == null ){
			isURL = false;
			sTextValue = "[internal error]";
		} else if( value instanceof DodsURL ){
			isURL = true;
			mURL = (DodsURL)value;
			sTextValue = mURL.getTitle() + ( mURL.isUnreachable() ? " [unreachable]" : "") ;
			mzDiscovered = (mURL.getDDS_Full() != null);
		} else {
			isURL = false;
			sTextValue = value.toString();
			mzDiscovered = true;
		}
		return this;
	}

	public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected, boolean cellHasFocus, int row, int col ){
		if( isSelected ){
			setForeground(table.getSelectionForeground());
			setBackground(table.getSelectionBackground());
		} else {
			setForeground(table.getForeground());
			setBackground(table.getBackground());
		}
		if( cellHasFocus ){
			setBorder(mBorder_FocusCell);
		} else {
			setBorder(mBorder_RegularCell);
		}
		if( table.getModel().getRowCount() != macolorContent_Type.length )
			macolorContent_Type = new Color[table.getModel().getRowCount()];
		mIndex = row;
		if( value == null ){
			isURL = false;
			sTextValue = "[internal error]";
		} else if( value instanceof DodsURL ){
			isURL = true;
			mURL = (DodsURL)value;
			sTextValue = mURL.getTitle();
		} else {
			isURL = false;
			sTextValue = value.toString();
		}
		return this;
	}

	public void paint(Graphics g) {
		super.paint(g);
		int iCellWidth = getWidth();
		int iCellHeight = getHeight();
		g.setColor(getBackground());
		g.fillRect(0, 0, iCellWidth, iCellHeight);
		getBorder().paintBorder(this, g, 0, 0, iCellWidth, iCellHeight);
		if( this.isURL ){
			if( mzShowTypeIndicators ){
				int iDigest = this.mURL.getDigest();
				if( iDigest == 0 ){
					g.drawString("?", mInsets.left+2, mInsets.top+2);
				} else {
					if( macolorContent_Type[mIndex] == null ){ // need to determine the color
						int xDigest = 1;
						while(true){
							if( xDigest > miColorCount ){ // did not find this digest and the color scheme is exhausted
								macolorContent_Type[mIndex] = macolorScheme[miColorCount]; // color will be the last one
								break;
							}
							if( xDigest > maiDigestList[0] ){ // new color is needed
								maiDigestList[0] = xDigest;
								maiDigestList[xDigest] = iDigest;
								macolorContent_Type[mIndex] = macolorScheme[xDigest];
							}
							if( iDigest == maiDigestList[xDigest] ){
								macolorContent_Type[mIndex] = macolorScheme[xDigest];
								break;
							}
							xDigest++;
						}
					}
					g.setColor(Color.black);
					g.drawRect(mInsets.left+2, mInsets.top+2, 10, 10); // type indicator
					g.setColor(macolorContent_Type[mIndex]);
					g.fillRect(mInsets.left+2, mInsets.top+2, 10, 10); // type indicator
				}
			}
			g.setColor(getForeground());
			if( this.mURL.getType() == DodsURL.TYPE_Directory ){
				g.drawImage(imageIndicator_Directory, mCategoryX, mInsets.top, Color.white, null);
			} else if( this.mURL.getType() == DodsURL.TYPE_Catalog ) {
				g.drawImage(imageIndicator_Catalog, mCategoryX, mInsets.top, Color.white, null);
			} else if( this.mURL.getType() == DodsURL.TYPE_Binary ) {
				g.drawImage(imageIndicator_Binary, mCategoryX, mInsets.top, Color.white, null);
			} else if( this.mURL.getType() == DodsURL.TYPE_Image ) {
				g.drawImage(imageIndicator_Image, mCategoryX, mInsets.top, Color.white, null);
			} else {
				g.drawImage(imageIndicator_Granule, mCategoryX, mInsets.top, Color.white, null);
			}
		}
		FontMetrics fm = g.getFontMetrics();
		mTextY = mInsets.top + fm.getAscent();
		if( sTextValue == null ) sTextValue = "[error]";
		// g.setFont(g.getFont());
		if( !mzDiscovered && mzShowUndiscoveredAsRed ){
			g.setColor(Color.red);
		} else {
			g.setColor(getForeground());
		}
		g.drawString(sTextValue, mTextX, mTextY);
		if( this.isURL ){
			if( this.mURL.isConstrained() ){
				int iConstraintX = mTextX + fm.stringWidth(sTextValue) + 2;
				if( iConstraintX > (this.getWidth()-16) ) iConstraintX = this.getWidth()-16;
				g.drawImage(imageConstrained, iConstraintX, mInsets.top, Color.white, null);
			}
		}
	}

	public void intervalAdded(ListDataEvent e){ this.vInitialize(1, mzShowUndiscoveredAsRed); }
	public void intervalRemoved(ListDataEvent e){ this.vInitialize(1, mzShowUndiscoveredAsRed); }
	public void contentsChanged(ListDataEvent e){ this.vInitialize(1, mzShowUndiscoveredAsRed); }

}


