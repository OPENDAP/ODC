package opendap.clients.odc.viewer;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.Point;
import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLJPanel;

import opendap.clients.odc.Utility;

import com.sun.opengl.util.Animator;

public class ViewPanel extends GLJPanel implements MouseWheelListener, MouseListener, ComponentListener {
	private static GLCapabilities caps;
	private HUD hud = null;
	private ViewManager view_manager;
	static {
		caps = new GLCapabilities();
		caps.setAlphaBits(8);
	}

	Animator animator = null;
	Model2D_Raster raster = null;

	public ViewPanel(){
		super(caps, null, null);
//		this.setBorder( javax.swing.BorderFactory.createLineBorder(java.awt.Color.WHITE) );
	}

	public boolean _zInitialize( ViewManager vm, StringBuffer sbError ){
		try {
			view_manager = vm;
			this.hud = view_manager.hud;
			animator = new Animator( this );
			this.addMouseListener( this );
			this.addComponentListener( this );
			return true;
		} catch( Exception t ) {
			Utility.vUnexpectedError( t, sbError );
			return false;
		}
	}

	boolean _setRaster( Model2D_Raster raster, StringBuffer sbError ){
		this.raster = raster;
		repaint();
		return true;
	}
	
	String _getGraphicsInfo(){
		StringBuffer sb = new StringBuffer();
		GL gl = this.getGL();
		sb.append( "OpenGL version: " + gl.glGetString(GL.GL_VERSION) );
		sb.append( "package: " + gl.getClass().getName() );
		sb.append( "settings: " + this.getChosenGLCapabilities() );
		sb.append( "vendor: " + gl.glGetString(GL.GL_VENDOR) );
		sb.append( "renderer: " + gl.glGetString(GL.GL_RENDERER) );
		return sb.toString();
	}

	int _getRasterWidth(){ return raster == null ? 0 : raster.iWidth; } 
	int _getRasterHeight(){ return raster == null ? 0 : raster.iHeight; } 
	
	boolean _zActivateAnimation( StringBuffer sbError ) {  // do not execute on swing thread
		try {
			animator.start();
//			animator.setRunAsFastAsPossible( true );
			return true;
		} catch( Exception t ) {
			Utility.vUnexpectedError( t, sbError );
			return false;
		}
	}
	
	boolean _zStopAnimation( StringBuffer sbError ) { // do not execute on swing thread
		try {
			animator.stop();
			return true;
		} catch( Exception t ) {
			Utility.vUnexpectedError( t, sbError );
			return false;
		}
	}

	private long startTime;
	private int frameCount;
	public void paintComponent( Graphics g ){
		super.paintComponent(g);

		// frame rate determination
		if( animator.isAnimating() ){
			if( startTime == 0 ) startTime = System.currentTimeMillis();
			if( ++frameCount == 30 ){
				long endTime = System.currentTimeMillis();
				view_manager.iFrameRate = (int)(30000 / (endTime - startTime));
				frameCount = 0;
				startTime = System.currentTimeMillis();
			}
		}
		if( raster != null ){
			int iZoomLevel = view_manager.iZoomLevel;
			int iSubImage_x = view_manager.iVP_x;
			int iSubImage_y = view_manager.iVP_y;
			raster.render( g, 0, 0, getWidth(), getHeight(), iZoomLevel, iSubImage_x, iSubImage_y );
		}
		hud.render(g);
	}
	
	public void mouseWheelMoved(MouseWheelEvent e) {
		view_manager.setZoom( e.getWheelRotation() ); // up is negative, down is positive
	}

	// Mouse listener interface
	public void mousePressed(MouseEvent evt){ }
	public void mouseDragged(MouseEvent evt){ }
	public void mouseReleased(MouseEvent evt){ }
	public void mouseEntered(MouseEvent evt) { }
	public void mouseExited(MouseEvent evt) { }
	public void mouseClicked( MouseEvent e ){
		hud.click( e.getX(), e.getY() );
	}

	// Component listener interface
	public void componentHidden( ComponentEvent e ){}
	public void componentMoved( ComponentEvent e ){}
	public void componentShown( ComponentEvent e ){}
	public void componentResized( ComponentEvent e ){
		view_manager.iVP_h = this.getHeight();
		view_manager.iVP_w = this.getWidth();
		repaint();
	}
}
