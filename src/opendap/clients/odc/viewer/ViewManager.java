package opendap.clients.odc.viewer;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import opendap.clients.odc.*;

public class ViewManager {
	final HUD hud = new HUD();
	int iFrameRate = 0;
	JFrame frame = null;
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

	Model2D_Raster mRaster;
	Model3D_Network mNetwork;
	
	public ViewManager() {}

	public final JFrame getFrame(){ return frame; }
	public final ViewPanel getViewport(){ return panelViewPort; }
	
	public final boolean zInitialize( StringBuffer sbError ){
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

	public final static ViewManager createFrame( String sTitle, StringBuffer sbError ){
		final ViewManager manager = new ViewManager();
		manager.frame = new JFrame(sTitle);
		manager.panelViewPort = new ViewPanel();
		manager.panelViewPort.setOpaque(false);
		manager.frame.getContentPane().add( manager.panelViewPort, BorderLayout.CENTER );
	    manager.frame.addWindowListener(new WindowAdapter() {
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
		manager.zInitialize( sbError );
		manager.panelViewPort._zInitialize( manager, sbError );
		return manager;
	}
	
	public final void setOrigin( int x, int y ){
		iVPB_w = mRaster.iWidth;
		iVPB_h = mRaster.iHeight;
		iVP_x = x;
		iVP_y = y;
		iVP_w = panelViewPort.getWidth();
		iVP_h = panelViewPort.getHeight();
		iVPC_x = x + iVP_w / 2;
		iVPC_y = y + iVP_h / 2;
	}

	public final void setCenter( int x, int y ){ // TODO update mouse position
		if( (x + 1) * 2 < iVP_w ) x = (iVP_w + 1)/2;
		else if( (iVPB_w - x + 1) * 2 < iVP_w ) x = iVPB_w - (iVP_w + 1)/2;
		if( (y + 1) * 2 < iVP_h ) y = (iVP_h + 1)/2;
		else if( (iVPB_h - y + 1) * 2 < iVP_h ) y = iVPB_h - (iVP_h + 1)/2;
		iVPC_x = x;
		iVPC_y = y;
		if( iZoomLevel == 0 ){
			iVP_x = iVPC_x - iVP_w / 2; 
			iVP_y = iVPC_y - iVP_h / 2;
		} else if( iZoomLevel > 0 ) {
			iVP_x = iVPC_x - iVP_w / 2; 
			iVP_y = iVPC_y - iVP_h / 2;
		} else {
			iVP_x = iVPC_x - iVP_w / 2; 
			iVP_y = iVPC_y - iVP_h / 2;
		}
		panelViewPort.repaint();
	}
	
	public final void setZoom( int iZoomAdjustment ){
		if( iZoomAdjustment > 0 && iZoomLevel < ZOOM_max ) iZoomLevel++;
		else if( iZoomLevel > ZOOM_min ) iZoomLevel--;
		setCenter( iVPC_x, iVPC_y );
	}

	public final void movePanVertical( int px ){
		setCenter( iVPC_x, iVPC_y + px ); 
		panelViewPort.repaint();
	}

	public final void movePanHorizontal( int px ){
		setCenter( iVPC_x + px, iVPC_y ); 
		panelViewPort.repaint();
	}
	
	public final  boolean zAddRaster( Model2D_Raster raster, StringBuffer sbError ){
		if( raster == null ){
			sbError.append( "raster missing" );
			return false;
		}
		if( ! panelViewPort._setRaster( raster, sbError) ){
			sbError.insert( 0, "failed to set raster: " );
			return false;
		}
		mRaster = raster;
		return true;
	}

	public final boolean zAddNetwork( Model3D_Network network, StringBuffer sbError ){
		if( network == null ){
			sbError.append( "network missing" );
			return false;
		}
		if( ! panelViewPort._setNetwork( network, sbError) ){
			sbError.insert( 0, "failed to set raster: " );
			return false;
		}
		mNetwork = network;
		return true;
	}
	
	public static void main( String[] args ) {
		vRunGearDemo();
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

	


