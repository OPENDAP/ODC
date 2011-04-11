package opendap.clients.odc.data;

import javax.swing.JPanel;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.plot.PlotOptions;

public class Panel_Edit_ViewArray extends JPanel implements ComponentListener {
	private static final long serialVersionUID = 0L;
	private BufferedImage mbi = null;
	private static boolean mzFatalError = false;
	private Font fontHeader = new Font( "LucidaBrightDemiBold", Font.PLAIN, 12 );
	private Font fontValue = new Font( "LucidaBrightDemiBold", Font.PLAIN, 12 );
	private Color colorHeaderBackground = new Color( 0xFF9FE5FF ); // muted blue
	private Color colorHeaderText = new Color( 0xFF505050 ); // dark gray
	private Color colorGridlines = new Color( 0xFFB0B0B0 ); // light gray
	private Node_Array nodeActive;
	
	private Panel_Edit_ViewArray(){}
	
	final static Panel_Edit_ViewArray _create( StringBuffer sbError ){
		Panel_Edit_ViewArray panel = new Panel_Edit_ViewArray();
		panel.setBackground( Color.WHITE );
		panel.addComponentListener( panel );
		return panel;
	}
	
	public void paintComponent( Graphics g ){
		super.paintComponent(g);
		try {
			if( mbi != null ) g.drawString( "Xmj", 0, 0);
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
	
	public void _vDrawImage( Node_Array node ){
		Model_VariableView view = node._getView();
		int xD1 = view.array_origin_x;
		int xD2 = view.array_origin_y;

		// standard scaled area
		int pxCanvasWidth = this.getWidth();
		int pxCanvasHeight = this.getHeight();

		if( mbi == null ){
			mbi = new BufferedImage( pxCanvasWidth, pxCanvasHeight, BufferedImage.TYPE_INT_ARGB );
		}
		Graphics2D g2 = (Graphics2D)mbi.getGraphics();
		
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF );
		
		int pxCell_width = 80;
		int pxCell_height = 20;
		int pxRowHeader_width = 60;
		int pxRowHeader_bottom_inset = 2;
		int pxColumnHeader_height = pxCell_height;  
		
		int ctRows = node._getRowCount();
		int ctColumns = node._getColumnCount();

		// draw header backgrounds
		int posColumnHeader_x = pxRowHeader_width; 
		int posColumnHeader_y = 0;
		int pxColumnHeader_width = pxCanvasWidth - posColumnHeader_x;
		if( pxColumnHeader_width > ctColumns * pxCell_width ) pxColumnHeader_width = ctColumns * pxCell_width;
		int posRowHeader_x = 0;
		int posRowHeader_y = pxColumnHeader_height;
		int pxRowHeader_height = pxCanvasHeight - posRowHeader_y;
		if( pxRowHeader_height > ctRows * pxCell_height ) pxRowHeader_height = ctRows * pxCell_height;   
		g2.setColor( colorHeaderBackground );
		g2.fillRect( posColumnHeader_x, posColumnHeader_y, pxColumnHeader_width, pxColumnHeader_height );
		g2.fillRect( posRowHeader_x, posRowHeader_y, pxRowHeader_width, pxRowHeader_height );
		
		// draw column headers centered
		g2.setFont( fontHeader );
		g2.setColor( colorHeaderText );
		FontMetrics fmHeader = g2.getFontMetrics( fontHeader );
		int pxHeaderFontHeight = fmHeader.getAscent() + fmHeader.getLeading(); // digits do not have descents
		int offsetY = (pxCell_height - pxHeaderFontHeight) / 2; 
		int posCell_x = 0;
		int posCell_y = 0;
		posCell_x = pxRowHeader_width; // advance cursor past row header
		for( int xColumn = xD2; xColumn < ctColumns; xColumn++ ){
			String sHeaderText = Integer.toString( xColumn );
			int iStringWidth = fmHeader.stringWidth( sHeaderText );
			int offsetX = (pxCell_width - iStringWidth ) / 2;
			g2.drawString( sHeaderText, posCell_x + offsetX, posCell_y + offsetY );
			posCell_x += pxCell_width; // advance to next cell
			if( posCell_x > pxCanvasWidth ) break; // not enough canvas to draw all the columns
		}

		// draw row headers centered
		posCell_x = 0; // start at lefthand edge of screen
		posCell_y += pxCell_height; // advance cursor past column header
		offsetY = pxCell_height - pxRowHeader_bottom_inset;
		for( int xRow = xD1; xRow < ctRows; xRow++ ){
			String sHeaderText = Integer.toString( xRow );
			int iStringWidth = fmHeader.stringWidth( sHeaderText );
			int offsetX = (pxCell_width - iStringWidth ) / 2;
			g2.drawString( sHeaderText, posCell_x + offsetX, posCell_y + offsetY );
			posCell_y += pxCell_height; // advance to next cell
			if( posCell_y > pxCanvasHeight ) break; // not enough canvas to draw all the columns
		}

		// draw grid lines
		g2.setColor( colorGridlines );
		int posLine_x = posColumnHeader_x;
		int posLine_y = pxColumnHeader_height;
		int posLine_width = pxCanvasWidth;
		int posLine_height = pxCanvasHeight;
		if( posLine_width > posColumnHeader_x + ctColumns * pxCell_width ) posLine_width = posColumnHeader_x + ctColumns * pxCell_width;  
		if( posLine_height > pxColumnHeader_height + ctRows * pxCell_height ) posLine_height = pxColumnHeader_height + ctRows * pxCell_height;  
		while( true ){
			if( posLine_x > pxCanvasWidth ) break;
			g2.drawLine( posLine_x, 0, posLine_x, posLine_height );
			posLine_x += pxCell_width; 
		}
		while( true ){
			if( posLine_y > pxCanvasHeight ) break;
			g2.drawLine( 0, posLine_y, posLine_width, posLine_y );
			posLine_y += pxCell_height; 
		}		

		_vUpdateCellValues( g2, node, xD1, xD2, pxRowHeader_width, pxColumnHeader_height, pxCell_width, pxCell_height );
				
		g2.setFont( new Font( "Lucida Bright Demibold", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Bright Demibold", 50, 50 ); 
		g2.setFont( new Font( "Lucida Bright Italic", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Bright Italic", 50, 75 ); 
		g2.setFont( new Font( "Lucida Bright Regular", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Bright Regular", 50, 100 ); 
		g2.setFont( new Font( "Lucida Console", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Console", 50, 125 ); 
		g2.setFont( new Font( "Lucida Sans Demibold", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Demibold", 50, 150 ); 
		g2.setFont( new Font( "Lucida Sans Demibold Italic", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Demibold Italic", 50, 175 ); 
		g2.setFont( new Font( "Lucida Sans Demibold Roman", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Demibold Roman", 50, 200 ); 
		g2.setFont( new Font( "Lucida Sans Italic", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Italic", 50, 225 ); 
		g2.setFont( new Font( "Lucida Sans Regular", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Regular", 50, 250 ); 		
		g2.setFont( new Font( "Lucida Sans Typewriter Bold", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Typewriter Bold", 50, 275 ); 
		g2.setFont( new Font( "Lucida Sans Typewriter Bold Oblique", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Typewriter Bold Oblique", 50, 300 ); 
		g2.setFont( new Font( "Lucida Sans Typewriter Oblique", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Typewriter Oblique", 50, 325 ); 
		g2.setFont( new Font( "Lucida Sans Typewriter Regular", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Typewriter Regular", 50, 350 ); 
		g2.setFont( new Font( "Lucida Sans Unicode", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Unicode", 50, 375 ); 
		
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
		int pxRightInset = 2;
		int ctRows = node._getRowCount();
		int ctColumns = node._getColumnCount();
		int pxValueFontHeight = fmValue.getAscent() + fmValue.getLeading(); // digits do not have descents
		int offsetY = pxCell_height - pxValueFontHeight;
		for( int xRow = xD1; xRow <= ctRows; xRow++ ){
			for( int xColumn = xD2; xColumn <= ctColumns; xColumn++ ){
				int iValueIndex = node._getValueIndex( xRow, xColumn );
				String sValueText = Integer.toString( aiValues[iValueIndex] );
				int iStringWidth = fmValue.stringWidth( sValueText );
				int offsetX = pxCell_width - iStringWidth + pxRightInset;
				g2.drawString( sValueText, posCell_x + offsetX, posCell_y + offsetY );
				posCell_x += pxCell_width; // advance to next cell
				if( posCell_x > pxCanvasWidth ) break; // not enough canvas to draw all the columns
			}
			posCell_y += pxCell_width; // advance to next row
			if( posCell_y > pxCanvasHeight ) break; // not enough canvas to draw all the rows
			posCell_x = posX_origin;   // reset x-position to first column
		}
				
	}
}
