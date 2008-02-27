package opendap.clients.odc.viewer;

import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import opendap.clients.odc.*;

public class ViewManager implements KeyListener, RelativeLayoutInterface {
	final HUD hud = new HUD();
	int iFrameRate = 0;
	JFrame frame = null;
	ViewPanel panelViewPort = null;
	int ZOOM_max = 10;
	int ZOOM_min = -10;
	int TIMESLICE_max = 0;
	int iZoomLevel = 0;
	int iTimeslice_begin = -1;
	int iTimeslice_end = -1;
	int iTimeslice_depth = 0;
	int iVP_x = 0;
	int iVP_y = 0;
	int iVP_w = 0;
	int iVP_h = 0;
	int iVPB_w = 0; // viewport bounds
	int iVPB_h = 0;
	int iVPC_x = 0; // viewport center
	int iVPC_y = 0;
	int iVPM_t = 0; // viewport margins (ie decoration, ie title bar and borders)
	int iVPM_b = 0;
	int iVPM_l = 0;
	int iVPM_r = 0;

	Model2D_Raster mRaster;
	Model3D_Network mNetwork;
	Model3D_Featureset mFeatureset;
	
	public ViewManager() {}

	public final java.awt.Rectangle getLayoutRect(){ return new java.awt.Rectangle( 0, 0, iVP_w, iVP_h ); }  
	public final JFrame getFrame(){ return frame; }
	public final ViewPanel getViewport(){ return panelViewPort; }
	public final void redraw(){ panelViewPort.repaint(); }
	
	public final boolean zInitialize( StringBuffer sbError ){
		try {
			if( ! hud.zInitialize( this, sbError ) ){
				sbError.insert( 0, "error initializing HUD: " );
				return false;
			}
			return true;
		} catch( Exception t ) {
			ApplicationController.vUnexpectedError( t, sbError );
			return false;
		}
	}

	/** creates a view as a new, independent window */
	public final static ViewManager createAsExternalFrame( String sTitle, StringBuffer sbError ){
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
		manager.frame.addKeyListener( manager );
		manager.zInitialize( sbError );
		manager.panelViewPort._zInitialize( manager, sbError );

		return manager;
	}
	
	public Graphics2D getGraphics(){ return (Graphics2D)this.panelViewPort.getGraphics(); }
	
	public final HUD getHUD(){ return hud; }
	
	ArrayList<CommandInterface> listCommandObjects = new ArrayList<CommandInterface>();
	public final void command(){
		String sCommand = JOptionPane.showInputDialog( frame, "Enter command:", "Enter command", JOptionPane.OK_CANCEL_OPTION );
		if( sCommand == null ) return;
		String[] asCommand = Utility.splitCommaWhiteSpace( sCommand );
		for( CommandInterface layer : listCommandObjects ){
			String[] asLayerCommands = layer.getCommands();
			if( Utility.arrayStartsWithMember( asLayerCommands, asCommand[0] ) ){
				int[] aiArg = new int[asCommand.length - 1];
				for( int xArg = 0; xArg < aiArg.length; xArg++ ){
					try {
						int iValue = Integer.parseInt( asCommand[xArg + 1] );
						aiArg[xArg] = iValue;
					} catch( Throwable t ) {
					}
				}
				layer.command( asCommand[0], aiArg );
			}
		}
	}
	
	public final void frameSetVisible( boolean z ){
		frame.setVisible( z );
	}
	
	public final void frameSetViewportSize( int iWidth, int iHeight ){
		iVP_w = iWidth;
		iVP_h = iHeight;
		frame.getContentPane().setSize( 500, 500 );
		Insets insets = frame.getInsets();
		iVPM_t = insets.top;
		iVPM_b = insets.bottom;
		iVPM_l = insets.left;
		iVPM_r = insets.right;
		frame.setSize( 500 + iVPM_l + iVPM_r, 500 + iVPM_t + iVPM_b);
	}
	
	public final float getScale(){
		if( iZoomLevel == 0 ){
			return 1;
		} else if( iZoomLevel > 0 ) {
			int iPixelRatio = iZoomLevel + 1;
			return iPixelRatio; 
		} else {
			int iPixelRatio = iZoomLevel * -1 + 1;
			return 1f / iPixelRatio;
		}
	}
	
	public final void frameCenterOnScreen(){
		java.awt.Dimension dimScreenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		getFrame().setLocation(dimScreenSize.width/2 - (iVP_w/2), dimScreenSize.height/2 - (iVP_h/2));
	}
	
	public final void setOrigin( int x, int y ){
		iVPB_w = mRaster.iWidth;
		iVPB_h = mRaster.iHeight;
		iVP_x = x;
		iVP_y = y;
		iVP_w = panelViewPort.getWidth();
		iVP_h = panelViewPort.getHeight();
		if( iZoomLevel == 0 ){
			iVPC_x = iVP_x + iVP_w / 2; 
			iVPC_y = iVP_y + iVP_h / 2;
		} else if( iZoomLevel > 0 ) {
			int iPixelRatio = iZoomLevel + 1;
			iVPC_x = (iVP_x + iVP_w / 2) / iPixelRatio; 
			iVPC_y = (iVP_y + iVP_h / 2) / iPixelRatio;
		} else {
			int iPixelRatio = iZoomLevel * -1 + 1;
			iVPC_x = (iVP_x + iVP_w / 2) * iPixelRatio; 
			iVPC_y = (iVP_y + iVP_h / 2) * iPixelRatio;
		}
		panelViewPort.repaint();
	}

	public final void setCenter( int x, int y ){ // TODO update mouse position
//		if( (x + 1) * 2 < iVP_w ) x = (iVP_w + 1)/2;
//		else if( (iVPB_w - x + 1) * 2 < iVP_w ) x = iVPB_w - (iVP_w + 1)/2;
//		if( (y + 1) * 2 < iVP_h ) y = (iVP_h + 1)/2;
//		else if( (iVPB_h - y + 1) * 2 < iVP_h ) y = iVPB_h - (iVP_h + 1)/2;
		iVPB_w = mRaster.iWidth;
		iVPB_h = mRaster.iHeight;
		iVPC_x = x;
		iVPC_y = y;
		iVP_w = panelViewPort.getWidth();
		iVP_h = panelViewPort.getHeight();
		if( iZoomLevel == 0 ){
			iVP_x = iVPC_x - iVP_w / 2; 
			iVP_y = iVPC_y - iVP_h / 2;
		} else if( iZoomLevel > 0 ) {
			int iPixelRatio = iZoomLevel + 1;
			iVP_x = iVPC_x - iVP_w / (2 * iPixelRatio); 
			iVP_y = iVPC_y - iVP_h / (2 * iPixelRatio);
		} else {
			int iPixelRatio = iZoomLevel * -1 + 1;
			iVP_x = iVPC_x - iVP_w * iPixelRatio / 2; 
			iVP_y = iVPC_y - iVP_h * iPixelRatio / 2;
		}
		panelViewPort.repaint();
	}
	
	public final void setZoom( int iZoomAdjustment ){
		if( iZoomAdjustment < 0 && iZoomLevel < ZOOM_max ) iZoomLevel++;
		else if( iZoomLevel > ZOOM_min ) iZoomLevel--;
		setCenter( iVPC_x, iVPC_y );
	}

	public final void moveCenter(){
		setCenter( iVPB_w / 2, iVPB_h / 2 ); 
	}
	
	public final void movePanVertical( int px ){
		if( iZoomLevel < 0 ){
			setCenter( iVPC_x, iVPC_y + px * (iZoomLevel * -1) );
		} else {
			setCenter( iVPC_x, iVPC_y + px );
		}
		panelViewPort.repaint();
	}

	public final void movePanHorizontal( int px ){
		if( iZoomLevel < 0 ){
			setCenter( iVPC_x + px * (iZoomLevel * -1), iVPC_y );
		} else {
			setCenter( iVPC_x + px, iVPC_y ); 
		}
		panelViewPort.repaint();
	}

	public final void animateSetTimeslice( int px ){
		if( iTimeslice_begin == -1 ) return; // in this case timeslice is not application, ie, not an animated rendering
		iTimeslice_begin += px > 0 ? -1 : 1;
		if( iTimeslice_begin < 0 ) iTimeslice_begin = 0;
		if( iTimeslice_begin > TIMESLICE_max ) iTimeslice_begin = TIMESLICE_max;
		if( iTimeslice_depth == 0 ){ // show all time slices
			iTimeslice_end = TIMESLICE_max;
		} else {
			iTimeslice_end = iTimeslice_begin + iTimeslice_depth - 1; 
		}
		panelViewPort.repaint();
	}

	public final void animateSetFrameCount( int iFrameCount ){
		iTimeslice_depth = iFrameCount;
		iTimeslice_end   = iTimeslice_begin + iTimeslice_depth - 1;
		if( iTimeslice_end > TIMESLICE_max ) iTimeslice_end = TIMESLICE_max; 
		panelViewPort.repaint();
	}

	public final void animateShowAllTimeslices(){
		iTimeslice_depth = 0;
		panelViewPort.repaint();
	}
	
	public final  boolean zAddRaster( Model2D_Raster raster, StringBuffer sbError ){
		if( raster == null ){
			sbError.append( "raster missing" );
			return false;
		}
		if( panelViewPort._setRaster( raster, sbError) ){
			mRaster = raster;
			setOrigin(0, 0);
		} else {
			sbError.insert( 0, "failed to set raster: " );
			return false;
		}
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

	public final boolean zAddFeatureset( Model3D_Featureset featureset, StringBuffer sbError ){
		if( featureset == null ){
			sbError.append( "featureset missing" );
			return false;
		}
		if( ! panelViewPort._setFeatureset( featureset, sbError) ){
			sbError.insert( 0, "failed to set raster: " );
			return false;
		}
		if( featureset.getTimesliceCount() > 0 ){ // then featureset is animated
			iTimeslice_begin = 1;
			iTimeslice_end = 1;
			iTimeslice_depth = 1;
			TIMESLICE_max = featureset.getTimesliceCount(); 
		}
		mFeatureset = featureset;
		listCommandObjects.add( mFeatureset );
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
		java.awt.image.BufferedImage biOpenGL_Logo = Resources.loadBufferedImage( "/opendap/clients/odc/images/opengl_logo.png", sbError );
		if( biOpenGL_Logo != null ) manager.getHUD().elementAddImage( biOpenGL_Logo );
		manager.panelViewPort._zInitialize( manager, sbError );
		manager.panelViewPort.addGLEventListener( new Model3D_Gears() );
		manager.panelViewPort._zActivateAnimation( sbError );
	}

	public void keyTyped( java.awt.event.KeyEvent e) {
    }

    public void keyPressed( java.awt.event.KeyEvent e) {
    	switch( e.getKeyCode() ){
    		case KeyEvent.VK_RIGHT:
    			movePanHorizontal( 1 );
    			break;
    		case KeyEvent.VK_LEFT:
    			movePanHorizontal( -1 );
    			break;
    		case KeyEvent.VK_UP:
    			movePanVertical( -1 );
    			break;
    		case KeyEvent.VK_DOWN:
    			movePanVertical( 1 );
    			break;
    		case KeyEvent.VK_F10:
    			command();
    			break;
    		case KeyEvent.VK_F5:
    			moveCenter();
    			break;
    	}
    }

    /** Handle the key-released event from the text field. */
    public void keyReleased( java.awt.event.KeyEvent e) {
    }
	
}



//	private BufferedImage scaleImage(BufferedImage img, float xScale, float yScale) {
//		BufferedImage scaled = new BufferedImage((int) (img.getWidth() * xScale), (int) (img.getHeight() * yScale), BufferedImage.TYPE_INT_ARGB);
//		Graphics2D g = scaled.createGraphics();
//		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//		g.drawRenderedImage(img, AffineTransform.getScaleInstance(xScale, yScale));
//		return scaled;
//	}

