package opendap.clients.odc;

/**
 * Title:        Panel_View_Image
 * Description:  Displays GIFs, PNG etc
 * Copyright:    Copyright (c) 2002-2004
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.48
 */

import java.awt.event.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;

public class Panel_View_Image extends JPanel {

    public Panel_View_Image() {}

	private JList mjlistImages = null;
	private File mfileCacheDirectory;
	private boolean mzEnabled = false;
	private String[] masImageCacheFiles = null;
	private ImagePanel mDisplay = null;

	public boolean isEnabled(){ return mzEnabled; }

    public static void main(String[] args){
		JFrame frame = new JFrame();
		Panel_View_Image pvi = new Panel_View_Image();
		StringBuffer sbError = new StringBuffer(80);
		if( pvi.zInitialize(sbError) ){
			frame.setContentPane(pvi);
			WindowListener listenerCloser = new WindowAdapter(){
				public void windowClosing(WindowEvent e){
					ApplicationController.vForceExit();
				}
			};
			frame.addWindowListener(listenerCloser);
			Dimension dimScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
			frame.setSize(dimScreenSize);
			frame.setVisible(true);
		}
	}

    boolean zInitialize(StringBuffer sbError){

        try {

			javax.swing.border.Border borderStandard = BorderFactory.createEtchedBorder();
			this.setBorder(borderStandard);

			this.setLayout(new java.awt.BorderLayout());

			JSplitPane jsplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		    this.add(jsplit, java.awt.BorderLayout.CENTER);

			// Create list area
			JPanel jpanelImageList = this.jpanelMakeFileList(sbError);
			if( jpanelImageList == null ){
				if( sbError.length() > 0 ){
					ApplicationController.vShowWarning("Unable to show file list: " + sbError);
					sbError.setLength(0);
				}
				jsplit.setLeftComponent(new JLabel("[unavailable]"));
			} else {
				jsplit.setLeftComponent(jpanelImageList);
			}
			vRefreshImageList();

			// Create and intialize the image area
			JScrollPane jspDisplay = new JScrollPane();
			mDisplay = new ImagePanel();
			mDisplay.initialize(jspDisplay.getViewport());
			jspDisplay.setViewportView(mDisplay);
			jspDisplay.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			jspDisplay.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			jsplit.setRightComponent(jspDisplay);

			JPanel jpanelControls = new JPanel();

			// Open
			JButton jbuttonOpen = new JButton("Open");
			jbuttonOpen.addActionListener(
				new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					Panel_View_Image.this.vAction_Open();
				}
			}
			);
			jpanelControls.add(jbuttonOpen);

			// Ruler
			ImageRuler ruler = new ImageRuler(mDisplay);
			Border borderRuler = new TitledBorder("Ruler");
			ruler.setBorder(borderRuler);
			jpanelControls.add(ruler);
			mDisplay.setRuler(ruler);

			JPanel jpanelCommand = new JPanel();
			jpanelCommand.setLayout(new BorderLayout());
			jpanelCommand.add(jpanelControls, BorderLayout.SOUTH);

			this.add(jpanelControls, BorderLayout.SOUTH);

            return true;

        } catch(Exception ex){
            sbError.insert(0, "Unexpected error: " + ex);
            return false;
        }
	}

	JPanel jpanelMakeFileList( StringBuffer sbError ){

		mjlistImages = new JList();

		// create file list
		mjlistImages.addMouseListener(
			new MouseAdapter(){
				public void mousePressed( MouseEvent me ){
					if( me.getClickCount() == 1 ){
						JList list = (JList)me.getSource();
						int x0Item = list.locationToIndex(me.getPoint());
						if( x0Item > -1 ){
							Panel_View_Image.this.vAction_LoadCacheItem( x0Item );
						}
					}
				}
			}
		);

		String sCacheDirectory = ConfigurationManager.getInstance().getDefault_DIR_ImageCache();
		if( sCacheDirectory == null ){
			sbError.append("no cache directory exists");
			return null;
		}
		mfileCacheDirectory = new File(sCacheDirectory);
		if( !mfileCacheDirectory.exists() ){
			if( ConfigurationManager.getInstance().getProperty_MODE_ReadOnly() ){
				return null;
			} else {
				boolean zDirectoryCreated = mfileCacheDirectory.mkdirs();
				if( !zDirectoryCreated ){
					sbError.append("failed to create image cache directory [" + sCacheDirectory + "]");
					return null;
				}
			}
		}
		if( !mfileCacheDirectory.isDirectory() ){
			sbError.append("image cache is not a directory [" + sCacheDirectory + "]");
			return null;
		}
		this.mzEnabled = true;

		JPanel jpanelImageList = new JPanel();
		JScrollPane jscrollList = new JScrollPane(mjlistImages);

		// Clear Cache
		JButton jbuttonClearCache = new JButton("Clear Cache");
		jbuttonClearCache.addActionListener(
			new ActionListener(){
			public void actionPerformed(ActionEvent event) {
				Panel_View_Image.this.vClearCache();
				}
			}
		);

		// Refresh Cache
		JButton jbuttonRefresh = new JButton("Refresh List");
		jbuttonRefresh.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					Panel_View_Image.this.vRefreshImageList();
				}
			}
		);

		// layout list panel
		jpanelImageList.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.BOTH; gbc.gridwidth = 2; gbc.gridheight = 1; gbc.weightx = 1.0; gbc.weighty = 1.0;
		jpanelImageList.add( mjlistImages, gbc );
		gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.gridheight = 1; gbc.weightx = 0.0; gbc.weighty = 0.0;
		jpanelImageList.add( jbuttonClearCache, gbc );
		gbc.gridx = 1; gbc.gridy = 1;
		jpanelImageList.add( jbuttonRefresh, gbc );

		return jpanelImageList;
	}

	void vAction_LoadCacheItem( int x0Item ){
		try {
			String sFullPath = Utility.sConnectPaths( Panel_View_Image.this.mfileCacheDirectory.getPath(), this.masImageCacheFiles[x0Item] );
			Panel_View_Image.this.vAction_LoadFile( sFullPath );
		} catch(Exception ex) {
			StringBuffer sbError = new StringBuffer("internal error, no object for item in list");
			Utility.vUnexpectedError(ex, sbError);
			ApplicationController.vShowError(sbError.toString());
		}
	}

	void vAction_LoadFile( String sPath ){
		File file = new File(sPath);
		vAction_LoadFile( file );
	}

	void vAction_LoadFile( File file ){
		try {
			if( file == null ){
				vAction_ShowError( "internal error, no file" );
				return;
			}
			if( !file.exists() ){
				vAction_ShowError( "File does not exist: " + file.getCanonicalPath() );
				return;
			}
			Image image = Toolkit.getDefaultToolkit().createImage(file.getCanonicalPath());
			if( image == null ){
				vAction_ShowError("Failed to create image (no further information)");
			}
			ImageIcon ii = new javax.swing.ImageIcon(image); // force image load
			long nTimeout = System.currentTimeMillis() + 3000; // 3 seconds
			while( ii.getImageLoadStatus() == MediaTracker.LOADING ){
				this.wait(100);
				if( System.currentTimeMillis() > nTimeout ){
					int iResponse = JOptionPane.showConfirmDialog(this, "Still loading, continue to wait?", "Still Loading", javax.swing.JOptionPane.YES_NO_OPTION);
					if( iResponse == javax.swing.JOptionPane.YES_OPTION ){
						nTimeout += 6000;
					} else {
						break;
					}
				}
			}
			switch( ii.getImageLoadStatus() ){
				case MediaTracker.ABORTED:
				case MediaTracker.LOADING:
					vAction_ShowStatus("Image load aborted");
					break;
				case MediaTracker.ERRORED:
					vAction_ShowError("Unknown error loading image");
					break;
				case MediaTracker.COMPLETE:
					vAction_Image_Set( ii.getImage() );
			}
		} catch(Exception ex) {
			vAction_ShowError("Failed to create image: " + ex);
		} catch(Error err) {
			vAction_ShowError("System failure creating image: " + err);
			mDisplay = null;
		}
	}

	void vAction_Image_Set(final Image i){
		javax.swing.SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					mDisplay.setImage(i);
				}
			}
		);
	}

	void vAction_Ruler_Show( final boolean z ){
		javax.swing.SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					mDisplay.getRuler().setShowRuler( z );
				}
			}
		);
	}

	void vAction_Ruler_ColorAdvance(){
	}

	JFileChooser jfc = null;
	void vAction_Open(){

		// determine file path
		if( jfc == null ) jfc = new JFileChooser();
		int iState = jfc.showDialog(null, "Select File Location");
		File file = jfc.getSelectedFile();
		if( iState != JFileChooser.APPROVE_OPTION ) return;
		if( file == null ) return;

		vAction_LoadFile( file );
	}

	void vAction_ShowError( String sError ){
		ApplicationController.vShowError(sError);
		if( mDisplay.isFullScreen() ) mDisplay.setMessage( sError );
	}

	void vAction_ShowStatus( String sMessage ){
		ApplicationController.vShowStatus(sMessage);
		if( mDisplay.isFullScreen() ) mDisplay.setMessage( sMessage );
	}

	void vAction_ShowStatus_NoCache( String sMessage ){
		ApplicationController.vShowStatus_NoCache(sMessage);
		if( mDisplay.isFullScreen() ) mDisplay.setMessage( sMessage );
	}

	File getCacheDirectory(){ return mfileCacheDirectory; }

	OutputStream getCacheFileOutputStream( String sImageName, StringBuffer sbError ) {
		String sCacheDirectory = null;
		try {
			File fileCacheDirectory = this.getCacheDirectory();
			if( fileCacheDirectory == null ){
				sbError.append("Image cache directory undefined");
				return null;
			}
			sCacheDirectory = fileCacheDirectory.getCanonicalPath();
		} catch(Exception ex) {
			sbError.append("Unable to get image cache directory: " + sCacheDirectory);
			return null;
		}
		String sPath = Utility.sConnectPaths( sCacheDirectory, sImageName );
		File fileTarget = new File(sPath);
		try {
			FileOutputStream fos = new FileOutputStream(fileTarget);
			if( fos == null ){
				sbError.append("no output stream for " + fileTarget);
			}
			return fos;
		} catch(Exception ex) {
			sbError.append("Unable to create cache file [" + sPath + "]: " + ex);
			return null;
		}
	}

	void vClearCache(){
		if( this.isEnabled() ){
			if( mfileCacheDirectory != null ){
				if( masImageCacheFiles != null ){
					String sCacheDirectory = null;
					try {
						sCacheDirectory = mfileCacheDirectory.getCanonicalPath();
					} catch(Exception ex) {
						this.vAction_ShowError("failed to identify cache directory: " + mfileCacheDirectory);
						return;
					}
					for( int xFile = 0; xFile < masImageCacheFiles.length; xFile++ ){
						String sPathToDelete = Utility.sConnectPaths( sCacheDirectory, masImageCacheFiles[xFile]);
						File fileToDelete = new File(sPathToDelete);
						if( fileToDelete.exists() ){
							fileToDelete.delete();
						} else {
							this.vAction_ShowError("could not locate file: " + sPathToDelete);
							return;
						}
					}
					vRefreshImageList();
				}
			}
		}
	}

	public void vRefreshImageList(){
		vRefreshImageList( null );
	}

	public void vRefreshImageList( String sFileToActivate ){
		mjlistImages.removeAll();
		int x0itemToActivate = -1;
		if( this.isEnabled() && (mfileCacheDirectory != null) ){
			File[] mafileImages = mfileCacheDirectory.listFiles();
			int ctFiles = mafileImages.length;
			masImageCacheFiles = new String[ctFiles];
			for( int xFile = 0; xFile < ctFiles; xFile++ ){
				masImageCacheFiles[xFile] = mafileImages[xFile].getName();
				if( sFileToActivate != null ) if( sFileToActivate.equalsIgnoreCase(masImageCacheFiles[xFile]) ) x0itemToActivate = xFile;
			}
			mjlistImages.setListData(masImageCacheFiles);
		}
		if( x0itemToActivate >= 0 ){
			this.vAction_LoadCacheItem(x0itemToActivate);
		}
	}

}

class ImagePanel extends JPanel implements MouseListener, MouseMotionListener {

	private final static int iMIN_SIZE_WIDTH = 400;
	private final static int iMIN_SIZE_HEIGHT = 200;
	private final static Dimension MIN_DIMENSION = new Dimension(200, 40);
	private BufferedImage mbi = null;
	private Dimension mImageSize = null;
	private Dimension mCanvasSize = null;
	private int miImage_Height = 0;
	private int miImage_Width = 0;
	private int miCanvas_Height = 0;
	private int miCanvas_Width = 0;
	private ImageRuler mRuler;
	private boolean mzMode_FullScreen = false;
	private String mDisplay_sMessage = null;
	private JViewport mvp = null;
	ImagePanel(){}
	void initialize( JViewport vp ){
		mvp = vp;
		this.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
	}
	void setRuler(ImageRuler controlRuler){ mRuler = controlRuler; }
	ImageRuler getRuler(){ return mRuler; }
	void setMessage( String sMessage ){
		mDisplay_sMessage = sMessage;
		this.repaint();
	}
	boolean isFullScreen(){ return mzMode_FullScreen; }
	void setFullScreen( boolean z ){ mzMode_FullScreen = z; }

	void setImage( Image i ){
		miImage_Height = i.getHeight(null);
		miImage_Width = i.getWidth(null);
		miCanvas_Height = miImage_Height + 20;
		miCanvas_Width = miImage_Width + 20;
		mbi = new BufferedImage(miImage_Width,miImage_Height,BufferedImage.TYPE_INT_RGB);
		mbi.getGraphics().drawImage(i,0,0,null);
		this.mImageSize = new Dimension(miImage_Width, miImage_Height);
		this.mCanvasSize = new Dimension(miCanvas_Width, miCanvas_Height);
		this.setPreferredSize(mCanvasSize);
		this.repaint();
	}
	Dimension getCanvasSize(){ return mCanvasSize; }
	Dimension getImageSize(){ return mImageSize; }
	int getImageHeight(){ return miImage_Height; }
	int getImageWidth(){ return miImage_Width; }

// color hsb draws to the mbi:
//		g2 = (Graphics2D)mbi.getGraphics();
//		((Graphics2D)g).drawImage(mbi, null, 0, 0);

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
		if( mbi == null ){
			g2.drawString("[no image open]", 10, 25);
		} else {
			int offset_x = 10;
			int offset_y = 10;
			g2.drawImage(mbi, null, offset_x, offset_y);
			g2.setColor(Color.red);
			g2.drawRect(1, 1, this.miCanvas_Width - 1, this.miCanvas_Height - 1);
			g2.setColor(Color.gray);
			g2.setFont(Styles.fontSansSerif10);
			FontMetrics fontmetrics = g2.getFontMetrics(Styles.fontSansSerif10);
			int iFontHeight = fontmetrics.getHeight();
			if( mRuler.getShowRuler() ){
				g2.setColor(mRuler.getColor());
				if( miImage_Width > 10 ){
					for(int x = 10; x < this.miImage_Width; x += 10){
						g2.drawLine(offset_x + x, offset_y + 3, offset_x + x, offset_y + 5);
					}
					for(int x = 50; x < this.miImage_Width; x += 50){
						g2.drawLine(offset_x + x, offset_y + 3, offset_x + x, offset_y + 8);
						String s = Integer.toString(x);
						int iStringWidth = fontmetrics.stringWidth(s);
						g2.drawString(s, offset_x + x - (iStringWidth/2), offset_y + 9 + iFontHeight);
					}
				}
				if( miImage_Height > 10 ){
					for(int y = 10; y < this.miImage_Height; y += 10){
						g2.drawLine(offset_x + 3, offset_y + y, offset_x + 5, offset_y + y);
					}
					for(int y = 50; y < this.miImage_Height; y += 50){
						g2.drawLine(offset_x + 3, offset_y + y, offset_x + 8, offset_y + y);
						g2.drawString(Integer.toString(y), offset_x + 11, offset_y + y + 4);
					}
				}
			}
			this.setSize(this.mImageSize);
			// mvp.setViewPosition();
		}
	}

	// Mouse motion interface
	public void mouseMoved(MouseEvent evt){
	}

	// Mouse listener interface
	public void mousePressed(MouseEvent evt){
	}

	public void mouseDragged(MouseEvent evt){
	}

	public void mouseReleased(MouseEvent evt){
	}

	public void mouseEntered(MouseEvent evt) { }
	public void mouseExited(MouseEvent evt) { }
	public void mouseClicked(MouseEvent evt){
		mousePressed( evt );
	}

    public Dimension getPreferredSize(JComponent c) {
		return this.getCanvasSize();
    }

    public Dimension getMinimumSize(JComponent c) {
		return MIN_DIMENSION;
    }

    public Dimension getMaximumSize(JComponent c) {
		return this.getImageSize();
    }

	// Scrollable interface
//    public Dimension getPreferredScrollableViewportSize() {
//		if( miImage_Height < iMIN_SIZE_HEIGHT && miImage_Width < iMIN_SIZE_WIDTH ) return MIN_DIMENSION;
//		if( miImage_Height < iMIN_SIZE_HEIGHT ) return new Dimension( miImage_Width, iMIN_SIZE_HEIGHT );
//		if( miImage_Width < iMIN_SIZE_WIDTH ) return new Dimension( iMIN_SIZE_WIDTH, miImage_Height );
//		return this.mImageSize;
//    }
//
//    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction){
//		return 1;
//    }
//
//    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
//        return (orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width;
//    }
//
//
//    /**
//     * If panel is displayed in a JViewport, don't change its width
//     * when the viewports width changes.  This allows horizontal
//     * scrolling if the JViewport is itself embedded in a JScrollPane.
//     *
//     * @return False - don't track the viewports width.
//     * @see Scrollable#getScrollableTracksViewportWidth
//     */
//    public boolean getScrollableTracksViewportWidth() {
//		if (getParent() instanceof JViewport) {
//			return (((JViewport)getParent()).getWidth() > getPreferredSize().width);
//		}
//		return false;
//    }
//
//    /**
//     * If panel is displayed in a JViewport, don't change its height
//     * when the viewports height changes.  This allows vertical
//     * scrolling if the JViewport is itself embedded in a JScrollPane.
//     *
//     * @return False - don't track the viewports width.
//     * @see Scrollable#getScrollableTracksViewportWidth
//     */
//    public boolean getScrollableTracksViewportHeight() {
//		if (getParent() instanceof JViewport) {
//			return (((JViewport)getParent()).getHeight() > getPreferredSize().height);
//		}
//		return false;
//    }

}

class ImageRuler extends JPanel {
	private boolean mzShowRuler = false;
	private boolean mzShowGrid = false;
	private Color colorRuler = Color.lightGray;
	private int miFixedColor = 0;
	ImagePanel mParentPanel;
	ImageRuler( ImagePanel parent ){
		mParentPanel = parent;
		acolorFixedList = new Color[17];
		acolorFixedList[1] = Color.black;
		acolorFixedList[2] = Color.blue;
		acolorFixedList[3] = Color.cyan;
		acolorFixedList[4] = Color.darkGray;
		acolorFixedList[5] = Color.gray;
		acolorFixedList[6] = Color.green;
		acolorFixedList[7] = Color.lightGray;
		acolorFixedList[8] = Color.magenta;
		acolorFixedList[9] = Color.orange;
		acolorFixedList[10] = Color.pink;
		acolorFixedList[11] = Color.red;
		acolorFixedList[12] = Color.white;
		acolorFixedList[13] = Color.yellow;
		final JCheckBox jcheckRuler = new JCheckBox("Show");
		jcheckRuler.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
				    ImageRuler.this.setShowRuler( jcheckRuler.isSelected() );
				}
			}
		);
		this.add(jcheckRuler);
		final JCheckBox jcheckGrid = new JCheckBox("Grid");
		jcheckRuler.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
				    ImageRuler.this.setShowGrid( jcheckGrid.isSelected() );
				}
			}
		);
		this.add(jcheckGrid);
		final JButton jbuttonRulerColorAdvance = new JButton("Color");
		jbuttonRulerColorAdvance.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
				    ImageRuler.this.setColorAdvance();
				}
			}
		);
		this.add(jbuttonRulerColorAdvance);
	}
	Color getColor(){ return this.colorRuler; }
	boolean getShowRuler(){ return mzShowRuler; }
	boolean getShowGrid(){ return mzShowGrid; }
	void setShowRuler(boolean z){
		if( z == mzShowRuler ) return;
		mzShowRuler = z;
		mParentPanel.repaint();
	}
	void setShowGrid(boolean z){
		if( z == mzShowGrid ) return;
		mzShowGrid = z;
		mParentPanel.repaint();
	}
	Color[] acolorFixedList;
	void setColorAdvance(){
		miFixedColor++;
		if( miFixedColor >= acolorFixedList.length ) miFixedColor = 1;
		colorRuler = acolorFixedList[miFixedColor];
		if( mzShowRuler || mzShowGrid ) mParentPanel.repaint();
	}
	void setColor(Color c){ colorRuler = c; }
}


