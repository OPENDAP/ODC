package opendap.clients.odc.viewer;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.Point;
import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLJPanel;

import opendap.clients.odc.Utility;

import com.sun.opengl.util.Animator;

public class ViewPanel extends GLJPanel implements MouseWheelListener, MouseListener {
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
	}

	public boolean zInitialize( ViewManager vm, StringBuffer sbError ){
		try {
			view_manager = vm;
			this.hud = view_manager.hud;
			animator = new Animator( this );
			return true;
		} catch( Exception t ) {
			Utility.vUnexpectedError( t, sbError );
			return false;
		}
	}

	String getGraphicsInfo(){
		StringBuffer sb = new StringBuffer();
		GL gl = this.getGL();
		sb.append( "OpenGL version: " + gl.glGetString(GL.GL_VERSION) );
		sb.append( "package: " + gl.getClass().getName() );
		sb.append( "settings: " + this.getChosenGLCapabilities() );
		sb.append( "vendor: " + gl.glGetString(GL.GL_VENDOR) );
		sb.append( "renderer: " + gl.glGetString(GL.GL_RENDERER) );
		return sb.toString();
	}

	boolean zActivateAnimation( StringBuffer sbError ) {  // do not execute on swing thread
		try {
			animator.start();
//			animator.setRunAsFastAsPossible( true );
			return true;
		} catch( Exception t ) {
			Utility.vUnexpectedError( t, sbError );
			return false;
		}
	}
	
	boolean zStopAnimation( StringBuffer sbError ) { // do not execute on swing thread
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
		if( raster != null ) raster.render( g, 0, 0, this.getWidth(), this.getHeight(), view_manager.iZoomLevel, )
		hud.render(g);
	}
	
	public void mouseWheelMoved(MouseWheelEvent e) {
		int iWheelRotation = e.getWheelRotation(); // up is negative, down is positive
	}

	// Mouse listener interface
	public void mousePressed(MouseEvent evt){ }
	public void mouseDragged(MouseEvent evt){ }
	public void mouseReleased(MouseEvent evt){ }
	public void mouseEntered(MouseEvent evt) { }
	public void mouseExited(MouseEvent evt) { }
	public void mouseClicked( MouseEvent e ){
		Point p = e.getLocationOnScreen();
		hud.click( p.x, p.y );
	}
	
}
