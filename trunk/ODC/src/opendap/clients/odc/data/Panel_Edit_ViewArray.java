package opendap.clients.odc.data;

import javax.swing.border.Border;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.KeyListener;
import java.awt.event.FocusListener;
import java.awt.geom.Rectangle2D;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.plot.PlotOptions;
import opendap.clients.odc.gui.Styles;

public class Panel_Edit_ViewArray extends JPanel implements ComponentListener, MouseListener, MouseMotionListener, KeyListener, FocusListener {
	private static final long serialVersionUID = 0L;
	private BufferedImage mbi = null;
	private int miBufferWidth;
	private int miBufferHeight;
	private static boolean mzFatalError = false;
	private Font fontHeader = Styles.fontSansSerifBold12;
	private Font fontValue = Styles.fontSansSerif12;
	private Color colorBackground = Color.WHITE;
	private Color colorHeaderBackground = new Color( 0xFF9FE5FF ); // muted blue
	private Color colorHeaderText = new Color( 0xFF505050 ); // dark gray
	private Color colorGridlines = new Color( 0xFFB0B0B0 ); // light gray
	private Color colorCursorCellBorder = new Color( 0xFF606060 ); // dark gray
	private Stroke strokeCursor = new BasicStroke( 2f );
	private Border borderFocused = BorderFactory.createLineBorder( Color.blue );
	private Node_Array nodeActive;
	private Panel_VarView parent;
	
	private Panel_Edit_ViewArray(){}
	
	final static Panel_Edit_ViewArray _create( Panel_VarView parent, StringBuffer sbError ){
		Panel_Edit_ViewArray panel = new Panel_Edit_ViewArray();
		panel.parent = parent;
		panel.setFocusable( true );
		panel.setBackground( panel.colorBackground );
		panel.addComponentListener( panel );
		panel.addMouseListener( panel );
		panel.addMouseMotionListener( panel );
		panel.addKeyListener( panel );
		panel.addFocusListener( panel );
		return panel;
	}
	
	public void paintComponent( Graphics g ){
		super.paintComponent(g);
		try {
			// if( mbi != null ) g.drawString( "Xmj", 0, 0);
			if( mbi != null ) ((Graphics2D)g).drawImage(mbi, null, 0, 0); // flip image to canvas
		} catch(Exception ex) {
			if( mzFatalError ) return; // got the training wheels on here todo
			mzFatalError = true;
			ApplicationController.vUnexpectedError(ex, "Error rendering plot image");
		}
	}
	
	public void componentResized( java.awt.event.ComponentEvent e ){
		if( nodeActive == null ){
			// clear TODO
		} else {
			_vDrawImage( nodeActive );
		}
	}
	public void componentHidden( ComponentEvent e ){}
	public void componentMoved( ComponentEvent e ){}
	public void componentShown( ComponentEvent e ){}
	
	public int iPageSize_row = 0;     // the number of full rows in a page
	public int iPageSize_column = 0;
	public int xLastFullRow = 0;
	public int xLastFullColumn = 0;
	
	int pxCell_width = 80;
	int pxCell_height = 20;
	int pxRowHeader_width = 60;
	int pxColumnHeader_height = pxCell_height;  
	public void _vDrawImage( Node_Array node ){
		nodeActive = node;
		Model_VariableView view = node._getView();
		int xOrigin_row = view.origin_row;
		int xOrigin_column = view.origin_column;

		// standard scaled area
		int pxCanvasWidth = this.getWidth();
		int pxCanvasHeight = this.getHeight();

		if( mbi == null ){
			Dimension dimScreen = Toolkit.getDefaultToolkit().getScreenSize();
			miBufferWidth = dimScreen.width;
			miBufferHeight = dimScreen.height;
			mbi = new BufferedImage( miBufferWidth, miBufferHeight, BufferedImage.TYPE_INT_ARGB );
		}
		Graphics2D g2 = (Graphics2D)mbi.getGraphics();
		g2.setColor( colorBackground );
		g2.fillRect( 0, 0, miBufferWidth, miBufferHeight ); // clear buffer
		
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF );
		g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
		
		int pxRowHeader_bottom_inset = 2;
		int pxColumnHeader_height = pxCell_height;  
		
		int ctRows = node._getRowCount();
		int ctRowsRemaining = ctRows - xOrigin_row;
		int pxRowHeightRemaining = pxColumnHeader_height + ctRowsRemaining * pxCell_height;
		int ctColumns = node._getColumnCount();
		if( ctColumns == 0 ) ctColumns = 1;
		int ctColumnsRemaining = ctColumns - xOrigin_column;
		int pxColumnWidthRemaining = pxRowHeader_width + ctColumnsRemaining * pxCell_width;

		// draw header backgrounds
		int posColumnHeader_x = pxRowHeader_width; 
		int posColumnHeader_y = 0;
		int pxColumnHeader_width = pxColumnWidthRemaining > pxCanvasWidth ? pxCanvasWidth - posColumnHeader_x : ctColumnsRemaining * pxCell_width;
		int posRowHeader_x = 0;
		int posRowHeader_y = pxColumnHeader_height;
		int pxRowHeader_height = pxRowHeightRemaining > pxCanvasHeight ? pxCanvasHeight - posRowHeader_y : ctRowsRemaining * pxCell_height;
		g2.setColor( colorHeaderBackground );
		g2.fillRect( posColumnHeader_x, posColumnHeader_y, pxColumnHeader_width, pxColumnHeader_height );
		g2.fillRect( posRowHeader_x, posRowHeader_y, pxRowHeader_width, pxRowHeader_height );
		
		// draw column headers centered
		g2.setFont( fontHeader );
		g2.setColor( colorHeaderText );
		FontMetrics fmHeader = g2.getFontMetrics( fontHeader );
		int pxHeaderFontHeight = fmHeader.getAscent() + fmHeader.getLeading(); // digits do not have descents
		int offsetY = pxCell_height - (pxCell_height - pxHeaderFontHeight) / 2; 
		int posCell_x = 0;
		int posCell_y = 0;
		posCell_x = pxRowHeader_width; // advance cursor past row header
		int xColumn = xOrigin_column;
		for( ; xColumn < ctColumns; xColumn++ ){
			String sHeaderText = Integer.toString( xColumn );
			int iStringWidth = fmHeader.stringWidth( sHeaderText );
			int offsetX = (pxCell_width - iStringWidth ) / 2;
			g2.drawString( sHeaderText, posCell_x + offsetX, posCell_y + offsetY );
			posCell_x += pxCell_width; // advance to next cell
			if( posCell_x > pxCanvasWidth ) break; // not enough canvas to draw all the columns
		}
		xLastFullColumn = xColumn - 1;

		// draw row headers centered
		posCell_x = 0; // start at lefthand edge of screen
		posCell_y += pxCell_height; // advance cursor past column header
		offsetY = pxCell_height - pxRowHeader_bottom_inset;
		int xRow = xOrigin_row;
		for( ; xRow < ctRows; xRow++ ){
			String sHeaderText = Integer.toString( xRow );
			int iStringWidth = fmHeader.stringWidth( sHeaderText );
			int offsetX = (pxCell_width - iStringWidth ) / 2;
			g2.drawString( sHeaderText, posCell_x + offsetX, posCell_y + offsetY );
			posCell_y += pxCell_height; // advance to next cell
			if( posCell_y > pxCanvasHeight ) break; // not enough canvas to draw all the columns
		}
		xLastFullRow = xRow - 1;
		iPageSize_row = xLastFullRow - view.origin_row + 1;
		iPageSize_column = xLastFullColumn - view.origin_column + 1;

		// draw grid lines
		g2.setColor( colorGridlines );
		int posLine_x = posColumnHeader_x;
		int posLine_y = pxColumnHeader_height;
		int posLine_width = pxCanvasWidth;
		int posLine_height = pxCanvasHeight;
		if( posLine_width > pxColumnWidthRemaining ) posLine_width = posColumnHeader_x + ctColumnsRemaining * pxCell_width;  
		if( posLine_height > pxRowHeightRemaining ) posLine_height = pxColumnHeader_height + ctRowsRemaining * pxCell_height;
		int xGridline = 0;
		while( true ){ // column gridlines
			if( posLine_x > pxCanvasWidth ) break;
			if( xGridline > xLastFullColumn - view.origin_column + 1 ) break;
			g2.drawLine( posLine_x, 0, posLine_x, posLine_height );
			posLine_x += pxCell_width;
			xGridline++;
		}
		xGridline = 0;
		while( true ){ // row gridlines
			if( posLine_y > pxCanvasHeight ) break;
			if( xGridline > xLastFullRow - view.origin_row + 1 ) break;
			g2.drawLine( 0, posLine_y, posLine_width, posLine_y );
			posLine_y += pxCell_height; 
			xGridline++;
		}		

		// draw cursor
		int posCursor_row = pxColumnHeader_height + pxCell_height * (view.cursor_row - view.origin_row) - 1;
		int posCursor_column = pxRowHeader_width + pxCell_width * (view.cursor_column - view.origin_column) - 1;
		int pxCursor_width = pxCell_width + 3;
		int pxCursor_height = pxCell_height + 3;
		g2.setColor( colorCursorCellBorder );
		g2.setStroke( strokeCursor );
		g2.drawRect( posCursor_column, posCursor_row, pxCursor_width, pxCursor_height );

		// draw selection
		int ctColumnsInSelection = view.selectionLR_column - view.selectionUL_column;
		int ctRowsInSelection = view.selectionUL_row - view.selectionLR_row;
		int posSelection_x = pxRowHeader_width + pxCell_width * (view.selectionUL_column - view.origin_column) - 1;
		int posSelection_y = pxColumnHeader_height + pxCell_height * (view.selectionUL_row - view.origin_row) - 1;
		int pxSelection_width = pxCell_width * ctColumnsInSelection + 3;
		int pxSelection_height = pxCell_height * ctRowsInSelection + 3;
		g2.setColor( colorCursorCellBorder );
		g2.setStroke( strokeCursor );
		g2.drawRect( posSelection_x, posSelection_y, pxSelection_width, pxSelection_height );
		
		_vUpdateCellValues( g2, node, xOrigin_row, xOrigin_column, pxRowHeader_width, pxColumnHeader_height, pxCell_width, pxCell_height );
						
		repaint();
	}

	private void _vUpdateCellValues( Graphics2D g2, Node_Array node, int xD1, int xD2, int posX_origin, int posY_origin, int pxCell_width, int pxCell_height ){

		int pxCanvasWidth = this.getWidth();
		int pxCanvasHeight = this.getHeight();
		int posCell_x = posX_origin;
		int posCell_y = posY_origin;
		
		g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
		opendap.dap.PrimitiveVector pv = node.getPrimitiveVector();
		Object oValues = pv.getInternalStorage();
		int[] aiValues = (int[])oValues;
		
		// draw cell values
		g2.setColor( Color.BLACK );
		g2.setFont( fontValue );
		FontMetrics fmValue = g2.getFontMetrics( fontValue );
		int pxDescent = fmValue.getDescent();
		int pxRightInset = 2;
		int ctRows = node._getRowCount();
		int ctColumns = node._getColumnCount();
		int pxValueFontHeight = fmValue.getAscent() + fmValue.getLeading(); // digits do not have descents
		int offsetY = pxCell_height - pxDescent;
		int ctColumns_shown = ctColumns == 0 ? 1 : ctColumns;
		for( int xRow = xD1; xRow < ctRows; xRow++ ){
			for( int xColumn = xD2; xColumn < ctColumns_shown; xColumn++ ){
				int iValueIndex = node._getValueIndex( xRow, xColumn );
				String sValueText = Integer.toString( aiValues[iValueIndex] );
				int iStringWidth = fmValue.stringWidth( sValueText );
				int offsetX = pxCell_width - iStringWidth - pxRightInset;
				g2.drawString( sValueText, posCell_x + offsetX, posCell_y + offsetY );
				posCell_x += pxCell_width; // advance to next cell
				if( posCell_x > pxCanvasWidth ) break; // not enough canvas to draw all the columns
			}
			posCell_y += pxCell_height; // advance to next row
			if( posCell_y > pxCanvasHeight ) break; // not enough canvas to draw all the rows
			posCell_x = posX_origin;   // reset x-position to first column
		}
				
	}

	/**
	arrow keys                     move the selection one cell at a time
	PageUp/PageDown                moves the view up/down by a screen of rows
	Ctrl+PageUp/PageDown           moves the view left/right by a screen of columns
	Ctrl+Shift+PageUp/PageDown     moves the view diagonally by a screen
	Ctrl+Home                      moves the selection to [0][0]
	Ctrl+End                       moves the selection to the end of the slice
	Atl+PageUp/PageDown            moves the view to the next/previous page (ie slice)
	*/	
	public void keyPressed( KeyEvent ke ){
//		System.out.println("key pressed: "  +  ke.getKeyCode());
		switch( ke.getKeyCode() ){
			case KeyEvent.VK_UP:
				parent._setCursorUp(); break;
			case KeyEvent.VK_DOWN:
				parent._setCursorDown(); break;
			case KeyEvent.VK_LEFT:
				parent._setCursorLeft(); break;
			case KeyEvent.VK_RIGHT:
				parent._setCursorRight(); break;
			case KeyEvent.VK_HOME:
				parent._setCursorHome(); break;
			case KeyEvent.VK_END:
				parent._setCursorEnd(); break;
			case KeyEvent.VK_PAGE_UP:
				if( (ke.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) == 0 ){
					if( (ke.getModifiers() & KeyEvent.ALT_DOWN_MASK) == 0 ){
						parent._setCursorPageUp();
					} else {
						parent._setCursorSliceUp();
					}
				} else {
					if( (ke.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) == 0 ){
						parent._setCursorPageLeft();
					} else {
						parent._setCursorPageDiagonalUp();
					}
				}
				break;
			case KeyEvent.VK_PAGE_DOWN:
				if( (ke.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) == 0 ){
					if( (ke.getModifiers() & KeyEvent.ALT_DOWN_MASK) == 0 ){
						parent._setCursorPageDown();
					} else {
						parent._setCursorSliceDown();
					}
				} else {
					if( (ke.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) == 0 ){
						parent._setCursorPageRight();
					} else {
						parent._setCursorPageDiagonalDown();
					}
				}
				break;
		}
		ke.consume(); // consume the event (don't let other components be triggered by these events)
	}

	public void keyReleased(KeyEvent ke){
	}

	public void keyTyped(KeyEvent ke){
		// consumed
	}

	private boolean mzDraggingRow = false;
	private boolean mzDraggingColumn = false;
	public void mousePressed( java.awt.event.MouseEvent e ){
		requestFocusInWindow();
		java.awt.Point point = e.getPoint();
		int x = point.x;
		int y = point.y;
		int xCell_row = (y - pxColumnHeader_height)/pxCell_height;
		int xCell_column = (x - pxRowHeader_width)/pxCell_width;
		if( x < pxRowHeader_width ){
			if( y < pxColumnHeader_height ){
				parent._setCursor( nodeActive._view.origin_row, nodeActive._view.origin_column );
				parent._selectAll();
			} else {
				parent._setCursor( xCell_row, nodeActive._view.origin_column );
				parent._selectRow( xCell_row );
				mzDraggingRow = true;
			}
		} else if( y < pxColumnHeader_height ){
				parent._setCursor( nodeActive._view.origin_row, xCell_column );
				parent._selectColumn( xCell_column );
				mzDraggingColumn = true;
		} else {
			parent._setCursor( xCell_row, xCell_column );
		}
	}
	
	public void mouseReleased( java.awt.event.MouseEvent e ){
		mzDraggingRow = false;
		mzDraggingColumn = false;
	}

	public void mouseEntered( java.awt.event.MouseEvent e ){
	}

	public void mouseExited( java.awt.event.MouseEvent e ){
	}

	public void mouseClicked( java.awt.event.MouseEvent e ){
	}

	public void mouseDragged( java.awt.event.MouseEvent e ){
		java.awt.Point point = e.getPoint();
		int x = point.x;
		int y = point.y;
		int xCell_row = (y - pxColumnHeader_height)/pxCell_height;
		int xCell_column = (x - pxRowHeader_width)/pxCell_width;
		if( mzDraggingRow ){
			parent._selectRows( xCell_row, nodeActive._view.selectionUL_row );
		} else if( mzDraggingColumn ){
			parent._selectColumns( xCell_column, nodeActive._view.selectionUL_column );
		} else {
			parent._selectRange( xCell_row, xCell_column, nodeActive._view.cursor_row, nodeActive._view.cursor_column );
		}
    }

	public void mouseMoved( java.awt.event.MouseEvent e ){
    }
	public void focusGained( java.awt.event.FocusEvent e ){
		setBorder( borderFocused );
	}
	public void focusLost( java.awt.event.FocusEvent e ){
		setBorder( null );
	}
}

//g2D.setComposite( AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f) );
//Rectangle2D.Double rect = new Rectangle2D.Double(0,0,width,height); 
//g2D.fill(rect);
