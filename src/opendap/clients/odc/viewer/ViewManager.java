package opendap.clients.odc.viewer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import opendap.clients.odc.*;

public class ViewManager {
	final HUD hud = new HUD();
	int iFrameRate = 0;
	ViewPanel panelViewPort = null;
	int ZOOM_max = 10;
	int ZOOM_min = -10;
	int iZoomLevel = 0;
	int iVP_x = 0;
	int iVP_y = 0;
	int iVP_w = 0;
	int iVP_h = 0;
	int iVPB_w = 0; // viewport bounds
	int iVPB_h = 0;
	int iVPC_x = 0; // viewport center
	int iVPC_y = 0;

	public ViewManager() {}

	ViewPanel getViewport(){ return panelViewPort; }
	
	public boolean zInitialize( StringBuffer sbError ){
		try {
			if( ! hud.zInitialize( this, sbError ) ){
				sbError.insert( 0, "error initializing HUD: " );
				return false;
			}
			return true;
		} catch( Exception t ) {
			Utility.vUnexpectedError( t, sbError );
			return false;
		}
	}

	void setCenter( int x, int y ){
		if( (x + 1) * 2 < iVP_w ) x = (iVP_w + 1)/2 - 1;  // -1 is necessary because 0-based
		else if( (iVPB_w - x + 1) * 2 < iVP_w ) x = iVPB_w - (iVP_w + 1)/2 - 1;
		if( (y + 1) * 2 < iVP_h ) y = (iVP_h + 1)/2 - 1;  // -1 is necessary because 0-based
		else if( (iVPB_h - y + 1) * 2 < iVP_h ) y = iVPB_h - (iVP_h + 1)/2 - 1;
		iVPC_x = x;
		iVPC_y = y;
		if( iZoomLevel == 0 ){
			iVP_x = iVPC_x - iVP_w / 2; 
			iVP_y = iVPC_y - iVP_h / 2;
		} else if( iZoomLevel > 0 ) {
		} else {
		}
		panelViewPort.repaint();
	}
	
	void setZoom( int iZoomAdjustment ){
		if( iZoomAdjustment > 0 && iZoomLevel < ZOOM_max ) iZoomLevel++;
		else if( iZoomLevel < ZOOM_min ) iZoomLevel--;
		panelViewPort.repaint();
	}
	
	public static void main( String[] args ) {
		vRunFallujahTest();
//		vRunGearDemo();
	}

	public static void vRunFallujahTest(){
		JFrame frame = new JFrame("Fallujah Scenario");
		final ViewManager manager = new ViewManager();
		manager.panelViewPort = new ViewPanel();
		manager.panelViewPort.setOpaque(false);
		frame.getContentPane().add( manager.panelViewPort, BorderLayout.CENTER );
	    frame.addWindowListener(new WindowAdapter() {
	        public void windowClosing(WindowEvent e) {
	          // Run this on another thread than the AWT event queue to
	          // make sure the call to Animator.stop() completes before
	          // exiting
	          new Thread(new Runnable() {
	              public void run() {
	                manager.panelViewPort._zStopAnimation( new StringBuffer() );
	                System.exit(0);
	              }
	            }).start();
	        }
	      });
		StringBuffer sbError = new StringBuffer();
		manager.zInitialize( sbError );
		manager.panelViewPort._zInitialize( manager, sbError );
		
		Model2D_Raster raster = new Model2D_Raster();
		if( ! raster.zLoadImageFromFile( "C:/dev/workspace/ADACK/imagery/fallujah_ge_7m.png", sbError) ){
			System.err.println( "error loading file: " + sbError ); 
		}
		if( ! manager.panelViewPort._setRaster( raster, sbError) ){
			System.err.println( "error setting raster: " + sbError ); 
		}
		frame.setSize(500, 500);
		frame.setVisible( true );
		manager.iVP_w = manager.panelViewPort.getWidth();
		manager.iVP_h = manager.panelViewPort.getHeight();
		manager.iVPB_w = raster.iWidth;
		manager.iVPB_h = raster.iHeight;
		manager.iVPC_x = manager.iVPB_w / 2;
		manager.iVPC_y = manager.iVPB_h / 2;
	}
	
	public static void vRunGearDemo(){
		JFrame frame = new JFrame("Gear Demo");
		frame.getContentPane().setLayout(new BorderLayout());
		final ViewManager manager = new ViewManager();
		manager.panelViewPort = new ViewPanel();
		manager.panelViewPort.setOpaque(false);

		// create gradient panel for background
		JPanel gradientPanel = new JPanel() {
			public void paintComponent(Graphics g) {
				((Graphics2D) g).setPaint(new GradientPaint(0, 0, Color.WHITE,
						getWidth(), getHeight(), Color.DARK_GRAY));
				g.fillRect(0, 0, getWidth(), getHeight());
			}
		};
		gradientPanel.setLayout( new BorderLayout() );
		gradientPanel.add( manager.panelViewPort, BorderLayout.CENTER );
		frame.getContentPane().add( gradientPanel, BorderLayout.CENTER );

		final JCheckBox checkBox = new JCheckBox("Transparent", true);
		checkBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manager.panelViewPort.setOpaque(!checkBox.isSelected());
			}
		});
		frame.getContentPane().add(checkBox, BorderLayout.SOUTH);
		frame.setSize(300, 300);
		frame.setVisible( true );
	    frame.addWindowListener(new WindowAdapter() {
	        public void windowClosing(WindowEvent e) {
	          // Run this on another thread than the AWT event queue to
	          // make sure the call to Animator.stop() completes before
	          // exiting
	          new Thread(new Runnable() {
	              public void run() {
	                manager.panelViewPort._zStopAnimation( new StringBuffer() );
	                System.exit(0);
	              }
	            }).start();
	        }
	      });
		
		StringBuffer sbError = new StringBuffer();
		manager.zInitialize( sbError );
		manager.panelViewPort._zInitialize( manager, sbError );
		manager.panelViewPort.addGLEventListener( new Model3D_Gears() );
		manager.panelViewPort._zActivateAnimation( sbError );
	}

	

}


