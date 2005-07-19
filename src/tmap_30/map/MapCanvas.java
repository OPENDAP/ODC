package tmap_30.map;

import java.awt.AWTEvent;
import java.awt.Event;
import java.awt.Container;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Frame;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import java.awt.image.ImageProducer;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.RGBImageFilter;
import java.awt.MediaTracker;

import java.awt.image.BufferedImage;

import javax.swing.*;
import java.awt.Insets;

import tmap_30.map.MapConstants;
import tmap_30.map.MaxZoomException;
import tmap_30.map.MinZoomException;

/**
 * An extensible Canvas class for obtaining/displaying 2D coordinates.
 * <p>
 * This canvas has a base image and uses a tool to get/set coordinates
 * on the base image.
 * <p>
 * This is the second official release (version 2) of the MapCanvas.
 * It includes zoom/pan/scroll and snap-to-grid capabilities.
 *
 * @version     2.2, 17 June 1997
 * @author      Jonathan Callahan
 *
 * Note: Using a tool to select 360 degrees of longitude will result
 * in <code>user_x[HI] = user_x[LO]+360</code> where user_x[LO] is
 * correctly interpolated into the domain of the x axis.  Thus,
 * user_x[HI] will be outside the domain of the x axis specified
 * in MapGrid.
 */

public class MapCanvas extends JPanel implements MapConstants, MouseListener, MouseMotionListener {

	private MapScroller mScroller;
	Image base_image, gray_image;

	private Dimension offDimension;
	private Image offImage;
	private Graphics offGraphics;

	private int iWidth_Preferred;
	private int iHeight_Preferred;
	private int clip_width;
	private int clip_height;

	private double image_scaling = 1.0;
	private Rectangle imageRect = new Rectangle(0, 0, 0, 0);

	// scrolling rate in panning()
	private int slow_delta=1;
	private int fast_delta=5;

	/**
	 * Flag to indicate movement of the base image is desired.
	 */
	public boolean pan_down = false;
	/**
	 * Flag to indicate movement of the base image is desired.
	 */
	public boolean pan_down_fast = false;
	/**
	 * Flag to indicate movement of the base image is desired.
	 */
	public boolean pan_left = false;
	/**
	 * Flag to indicate movement of the base image is desired.
	 */
	public boolean pan_left_fast = false;
	/**
	 * Flag to indicate movement of the base image is desired.
	 */
	public boolean pan_right = false;
	/**
	 * Flag to indicate movement of the base image is desired.
	 */
	public boolean pan_right_fast = false;
	/**
	 * Flag to indicate movement of the base image is desired.
	 */
	public boolean pan_up = false;
	/**
	 * Flag to indicate movement of the base image is desired.
	 */
	public boolean pan_up_fast = false;

	/**
	 * Flag determining whether scrolling is controlled by the active MapTool
	 * or externally by the application programmer.
	 */
	public boolean tool_driven = true;

	/**
	 * The zoom factor to be applied when using the methods zoom_in() and zoom_out().
	 */
	public double zoom_factor = 1.4;

	/**
	 * The maximum image scaling to be allowed.
	 * It doesn't make sense to scale more than 2X the original image
	 */
	public double max_img_scaling = 4.0;

	/**
	 * The minimum image scaling to be allowed.
	 * This will be set automatically when the MapCanvas is created.
	 * The initial min_img_scaling will be such that the map cannot be made
	 * smaller than the MapCanvas area in both the X and Y dimensions.
	 */
	public double min_img_scaling = 0.25;

	private MapTool [] mToolArray;
	private MapRegion [] mRegionArray;

	private int selected_tool = 0;

	/**
	 * The current grid being used by the map.
	 */
	public MapGrid grid;

	public void vScrollRight(){ this.mScroller.setState(MapScroller.STATE_PanRight); }
	public void vScrollLeft(){ this.mScroller.setState(MapScroller.STATE_PanLeft); }
	public void vScrollStop(){ this.mScroller.setState(MapScroller.STATE_Inactive); }

	class MapScroller extends Thread {

		public static final int STATE_Inactive = 1;
		public static final int STATE_MousePan = 2; // does not work
		public static final int STATE_PanRight = 3;
		public static final int STATE_PanLeft = 4;
		private int sleep_milliseconds=10000;
		private int eState = 1;

		public MapScroller(){
			this.setDaemon(true);
		}

		public int getState(){ return eState; }

		public void setState( int STATE ){
			if( STATE > 0 && STATE < 5 ){
				eState = STATE;
				this.interrupt();
			}
		}

		public void set_sleep_milliseconds(int milliseconds) {
			sleep_milliseconds = milliseconds;
		}

		public void run() {
			while(true){
				switch( eState ){
					case STATE_Inactive:
						try {
							sleep(sleep_milliseconds);
						} catch (InterruptedException e) {}
						break;
					case STATE_MousePan: // does not work
						if( panning() ) {
							repaint();
						}
						break;
					case STATE_PanRight:
						scroll_X(-fast_delta);
						break;
					case STATE_PanLeft:
						scroll_X(fast_delta);
						break;
				}
				yield();
				try { sleep(50); } catch(Exception e) {}
			}
		}
	}

	public boolean panning( )
	{
		boolean refresh = false;

		if ( tool_driven ) {

			if ( getTool().pan_left) {
				if ( getTool().pan_left_fast )
		scroll_X(fast_delta);
	else
	 scroll_X(slow_delta);
 refresh = true;
			} else if ( getTool().pan_right) {
				if ( getTool().pan_right_fast)
		scroll_X(-fast_delta);
	else
	 scroll_X(-slow_delta);
 refresh = true;
			}

			if ( getTool().pan_down) {
				if ( getTool().pan_down_fast)
		scroll_Y(-fast_delta);
	else
	 scroll_Y(-slow_delta);
 refresh = true;
			} else if ( getTool().pan_up) {
				if ( getTool().pan_up_fast)
		scroll_Y(fast_delta);
	else
	 scroll_Y(slow_delta);
 refresh = true;
			}

		} else {

			if ( pan_left ) {
				if ( pan_left_fast )
		scroll_X(fast_delta);
	else
	 scroll_X(slow_delta);
 refresh = true;
			} else if ( pan_right ) {
				if ( pan_right_fast )
		scroll_X(-fast_delta);
	else
	 scroll_X(-slow_delta);
 refresh = true;
			}

			if ( pan_down ) {
				if ( pan_down_fast ) {
					scroll_Y(-fast_delta);
				} else
		scroll_Y(-slow_delta);
	refresh = true;
			} else if ( pan_up ) {
				if ( pan_up_fast )
		scroll_Y(fast_delta);
	else
	 scroll_Y(slow_delta);
 refresh = true;
			}
			getTool().setUser_XY();
		} // tool_driven
		return refresh;
	}

	/**
	 * Constructs and initializes a MapCanvas with the specified parameters.
	 * @param base_image the image over which the tool will be drawn
	 * @param width the width in pixels of the MapCanvas
	 * @param height the height in pixels of the MapCanvas
	 * @param tool the tool for user interaction
	 * @param grid the grid associated with the underlying basemap.
	 */
	public MapCanvas(Image base_image, int width, int height, MapTool [] mToolArray, MapGrid grid){
		MediaTracker tracker;

		this.base_image = base_image;
		this.iWidth_Preferred = width;
		this.iHeight_Preferred = height;
		this.mToolArray = mToolArray;
		this.grid = grid;
		this.grid.setCanvasWidth(iWidth_Preferred);

		ImageFilter f = new GrayFilter();
		ImageProducer producer = new FilteredImageSource(base_image.getSource(),f);
		gray_image = this.createImage(producer);

		tracker = new MediaTracker(this);

		tracker.addImage(gray_image, 1);
		try {
			tracker.waitForID(1);
		} catch (InterruptedException e) {
			System.out.println("MapCanvas: " + e);
		}
		if (tracker.isErrorID(1)) {
			System.out.println("MapCanvas: Error creating gray image.");
		}

		tracker.addImage(this.base_image, 2);
		try {
			tracker.waitForID(2);
		} catch (InterruptedException e) {
			System.out.println("MapCanvas: " + e);
		}
		if (tracker.isErrorID(2)) {
			System.out.println("MapCanvas: Error creating image.");
		}

		// mouse listener
		addMouseListener( this );
		addMouseMotionListener( this );
		enableEvents(AWTEvent.MOUSE_EVENT_MASK|AWTEvent.MOUSE_MOTION_EVENT_MASK);

		scale_image_to_fit();

		mScroller = new MapScroller();
		mScroller.setPriority(Thread.MIN_PRIORITY);
		mScroller.start();
	}

	/**
	 * Constructs and initializes a MapCanvas with the specified parameters.
	 * @param base_image the image over which the tool will be drawn
	 * @param width the width in pixels of the MapCanvas
	 * @param height the height in pixels of the MapCanvas
	 * @param tool the tool for user interaction
	 * @param grid the grid associated with the underlying basemap.
	 */
	public MapCanvas(Image base_image, int width, int height,
					 MapTool [] mToolArray, MapGrid grid, int x, int y, double scaling)
	{
		MediaTracker tracker;

		this.base_image = base_image;
		this.iWidth_Preferred = width;
		this.iHeight_Preferred = height;
		this.mToolArray = mToolArray;
		this.grid = grid;
		this.grid.setCanvasWidth(iWidth_Preferred);

		ImageFilter f = new GrayFilter();
		ImageProducer producer = new FilteredImageSource(base_image.getSource(),f);
		gray_image = this.createImage(producer);

		tracker = new MediaTracker(this);
		tracker.addImage(gray_image, 1);

		try {
			tracker.waitForID(1);
		} catch (InterruptedException e) {
			System.out.println("MapCanvas: " + e);
		}
		if (tracker.isErrorID(1)) {
			System.out.println("MapCanvas: Error creating gray image.");
		}

		// mouse listener
		addMouseListener( this );
		addMouseMotionListener( this );
		enableEvents(AWTEvent.MOUSE_EVENT_MASK|AWTEvent.MOUSE_MOTION_EVENT_MASK);

		position_and_scale_image(x, y, scaling);

		mScroller = new MapScroller();
		mScroller.setPriority(Thread.MIN_PRIORITY);
		mScroller.start();
	}

	public void vClear(){
		//
	}

	public int getHeight_Clip(){ return clip_height; }

	/**
	 * Paints the canvas with the base image and the current tool.
	 * @param g the specified Graphics window
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Insets insets = getInsets();
		Dimension d = getSize();
		if ( (offGraphics == null) || (d.width != offDimension.width) || (d.height != offDimension.height) ) {
			offDimension = d;
			offImage = createImage(d.width, d.height);
			offGraphics = offImage.getGraphics();
		}

		int top = insets.top;
		int left = insets.left;
		offGraphics.setColor(Color.gray);
		offGraphics.fillRect(0, 0, clip_width, clip_height);

		offGraphics.setClip(0, 0, clip_width, clip_height);
		offGraphics.drawImage(gray_image, imageRect.x, imageRect.y, imageRect.width, imageRect.height, this);
		offGraphics.drawImage(gray_image, imageRect.x+imageRect.width, imageRect.y, imageRect.width, imageRect.height, this);

		// This next section deals with calculating a clipping rectangle for the valid range of the data.
		int xlo, xhi, xwidth, ylo, yhi, yheight;
		double domain = grid.domain_X[HI]-grid.domain_X[LO];
		double range = getTool().range_X[HI]-getTool().range_X[LO];
		if ( Math.abs(domain) == Math.abs(range) ) {
			xlo = 0;
			xhi = clip_width;
		} else {
			xlo = grid.userToPixel_X(getTool().range_X[LO]);
			xhi = grid.userToPixel_X(getTool().range_X[HI]);
		}

		// Now for some logic to deal with ranges that span
		// the domain edges.  (e.g. domain=-180:180, range=140E:90W)
		// (xhi < xlo) means that xhi or xlo is outside of the
		// MapCanvas viewing area and we need modify xlo or xhi
		// to match the MapCanvas edge.
		if ( xhi < xlo ) {
			if ( xlo > clip_width ) xlo = 0;
		    else xhi = clip_width;
		}
		xwidth = xhi - xlo;

		ylo = grid.userToPixel_Y(getTool().range_Y[HI]);
		yhi = grid.userToPixel_Y(getTool().range_Y[LO]);
		yheight = yhi - ylo;

		// Now we apply a clipping rectangle to match the data range
		offGraphics.setClip(xlo, ylo, xwidth, yheight);
		offGraphics.drawImage(base_image, imageRect.x, imageRect.y, imageRect.width, imageRect.height, this);
		offGraphics.drawImage(base_image, imageRect.x+imageRect.width, imageRect.y, imageRect.width, imageRect.height, this);

		offGraphics.setClip(0, 0, clip_width, clip_height);

		for (int i=0; i<mToolArray.length; i++) {
			mToolArray[i].draw(offGraphics);
		}

		for (int i=0; i<mRegionArray.length; i++) {
			mRegionArray[i].draw(offGraphics);
		}

		g.setClip(0, 0, clip_width, clip_height);
		g.drawImage(offImage, 0, 0, this);
	}

	/**
	 * Causes the map to scroll an amount in the X direction.
	 * @param delta the number of pixels to scroll.
	 */
	public synchronized void scroll_X(int delta)
	{
		int tool_delta = delta;
		imageRect.x += delta;

	   /*
		* The starting coordinates of the base image are always:
		*
		*   -imageRect.width < imageRect.x <= 0  (for modulo_X)
		*   iWidth_Preferred - imageRect.width < imageRect.x <= 0 (for non-modulo_X)
		*   iHeight_Preferred - imageRect.height < imageRect.y <= 0
		*/

		if ( grid.modulo_X ) {
			if ( imageRect.x <= -imageRect.width ) {
				imageRect.x = imageRect.x + imageRect.width;
			}
			if ( imageRect.x > 0 ) {
				imageRect.x = imageRect.x - imageRect.width;
			}
		} else {
			if ( (imageRect.x+imageRect.width) < iWidth_Preferred ) {
				imageRect.x = iWidth_Preferred - imageRect.width;
				tool_delta = 0;
			}
			if ( imageRect.x > 0 ) {
				imageRect.x = 0;
				tool_delta = 0;
			}
		}


		/*
		* If the right edge of the tool is less than 0 or the
		* left edge of the tool is greater than the width of the canvas
		*/
		for (int i=0; i<selected_tool; i++) {
			if ( grid.modulo_X ) {
				if ( (mToolArray[i].x+tool_delta+mToolArray[i].width) < 0 ) tool_delta += imageRect.width;
				if ( (mToolArray[i].x+tool_delta) > iWidth_Preferred ) tool_delta -= imageRect.width;
			}
			mToolArray[i].setLocation(mToolArray[i].x+tool_delta,mToolArray[i].y);
		}

		for (int i=selected_tool+1; i<mToolArray.length; i++) {
			if ( grid.modulo_X ) {
				if ( (mToolArray[i].x+tool_delta+mToolArray[i].width) < 0 ) tool_delta += imageRect.width;
				if ( (mToolArray[i].x+tool_delta) > this.iWidth_Preferred ) tool_delta -= imageRect.width;
			}
			mToolArray[i].setLocation(mToolArray[i].x+tool_delta,mToolArray[i].y);
		}

		/*
		* If the right edge of the region is less than 0 or the
		* left edge of the region is greater than the width of the canvas
		*/
		for (int i=0; i<mRegionArray.length; i++) {
			if ( grid.modulo_X ) {
				if ( (mRegionArray[i].x+tool_delta+mRegionArray[i].width) < 0 ) tool_delta += imageRect.width;
				if ( (mRegionArray[i].x+tool_delta) > iWidth_Preferred ) tool_delta -= imageRect.width;
			}
			mRegionArray[i].setLocation(mRegionArray[i].x+tool_delta,mRegionArray[i].y);
		}
		repaint();
	}

	/**
	 * Causes the map to scroll an amount in the Y direction.
	 * @param delta the number of pixels to scroll.
	 */
	public synchronized void scroll_Y(int delta)
	{
		int tool_delta = delta;
		imageRect.y += delta;

		if ( (imageRect.y+imageRect.height) < iHeight_Preferred ) {
			imageRect.y = iHeight_Preferred - imageRect.height;
			tool_delta = 0;
		}
		if ( imageRect.y > 0 ) {
			imageRect.y = 0;
			tool_delta = 0;
		}

		for (int i=0; i<selected_tool; i++)
		    mToolArray[i].setLocation(mToolArray[i].x,mToolArray[i].y+tool_delta);

		for (int i=selected_tool+1; i<mToolArray.length; i++)
		    mToolArray[i].setLocation(mToolArray[i].x,mToolArray[i].y+tool_delta);

		for (int i=0; i<mRegionArray.length; i++)
		    mRegionArray[i].setLocation(mRegionArray[i].x,mRegionArray[i].y+tool_delta);

	}

	/*
	 * We need this in order to get the frame so we can change the cursor.
	 */
	private Frame getFrame(){
		Container parent = this.getParent();
		while ( (parent != null) && !(parent instanceof Frame)) parent = parent.getParent();
		return ((Frame) parent);
	}


	public void mouseMoved(MouseEvent evt)
	{
		int type = getTool().mouseMove( evt.getX(), evt.getY() );
		Frame frame = this.getFrame();
		if( frame != null) frame.setCursor( new Cursor(type) );
	}

	public void mousePressed(MouseEvent evt)
	{
		getTool().mouseDown( evt.getX(), evt.getY() );
		if ( getTool().is_active() ) {
			repaint();
		}
	}

	public void mouseDragged(MouseEvent evt)
	{
		getTool().mouseDrag( evt.getX(), evt.getY() );
		if ( getTool().is_active() ) {
			repaint();
		}
	}

	public void mouseReleased(MouseEvent evt)
	{
		getTool().mouseUp( evt.getX(), evt.getY() );
		repaint();
	}

	public void mouseEntered(MouseEvent evt) { }
	public void mouseExited(MouseEvent evt) { }
	public void mouseClicked(MouseEvent evt)
	{
		mousePressed( evt );
	}

	/**
	 * Increases the base image size the internally maintained zoom factor.
	 *
	 * @exception MaxZoomException already at max zoom.
	 * @exception MinZoomException already at min zoom.
	 */
	public synchronized void zoom_in()
			throws MaxZoomException, MinZoomException
	{
		this.zoom(zoom_factor);
	}


	/**
	 * Decreases the base image size the internally maintained zoom factor.
	 *
	 * @exception MaxZoomException already at max zoom.
	 * @exception MinZoomException already at min zoom.
	 */
	public synchronized void zoom_out()
			throws MaxZoomException, MinZoomException
	{
		this.zoom(1.0/zoom_factor);
	}

	/**
	 * Increases/decreases the base image size by the specified zoom factor.
	 *
	 * @param zoom_factor.
	 * @exception MaxZoomException already at max zoom.
	 * @exception MinZoomException already at min zoom.
	 */
	public synchronized void zoom(double zoom_factor) throws MaxZoomException, MinZoomException {
		Graphics g = getGraphics();
		double initial_scaling = image_scaling;
		if ( (image_scaling * zoom_factor) > max_img_scaling ) {
			this.zoom(max_img_scaling/image_scaling);
			throw new MaxZoomException();
		} else if ( (image_scaling * zoom_factor) < min_img_scaling - 0.01) {
			this.zoom(min_img_scaling/image_scaling);
			throw new MinZoomException();
		} else {
			image_scaling = image_scaling * zoom_factor;
		}
		zoom_factor = image_scaling / initial_scaling;
		imageRect.width = (int)(base_image.getWidth(this)*image_scaling);
		imageRect.height = (int)(base_image.getHeight(this)*image_scaling);
		clip_width = (imageRect.width < iWidth_Preferred) ? imageRect.width : iWidth_Preferred;
		clip_height = (imageRect.height < iHeight_Preferred) ? imageRect.height : iHeight_Preferred;
		for (int i=0; i<mToolArray.length; i++) {
			mToolArray[i].applyClipRect(0, 0, clip_width, clip_height);
		}
		center_tool(zoom_factor);
		repaint();
	}


	public void center_tool(double zoom_factor) {

		double [] mRegionArrayUser_X = new double[mRegionArray.length];
		double [] mRegionArrayUser_Y = new double[mRegionArray.length];
		for (int i=0; i<mRegionArray.length; i++) {
			mRegionArrayUser_X[i] = mRegionArray[i].user_X;
			mRegionArrayUser_Y[i] = mRegionArray[i].user_Y;
		}

		double [] mToolArrayUser_X = new double[mToolArray.length];
		double [] mToolArrayUser_Y = new double[mToolArray.length];
		for (int i=0; i<mToolArray.length; i++) {
			mToolArrayUser_X[i] = mToolArray[i].user_X[LO];
			mToolArrayUser_Y[i] = mToolArray[i].user_Y[HI];
		}

		// Move the image so that the selected tool will be centered
		if ( imageRect.width >= iWidth_Preferred ) {
			imageRect.x = (int) (iWidth_Preferred/2 - (getTool().x + getTool().width/2 - imageRect.x)*zoom_factor);
			imageRect.y = (int) (iHeight_Preferred/2 - (getTool().y + getTool().height/2 - imageRect.y)*zoom_factor);
		} else {
			imageRect.x = (int) (imageRect.width/2 - (getTool().x + getTool().width/2 - imageRect.x)*zoom_factor);
			imageRect.y = (int) (imageRect.height/2 - (getTool().y + getTool().height/2 - imageRect.y)*zoom_factor);
		}

		// Resize the tool
		for (int i=0; i<mToolArray.length; i++) {
			mToolArray[i].width *= zoom_factor;
			mToolArray[i].height *= zoom_factor;
		}
		for (int i=0; i<mRegionArray.length; i++) {
			mRegionArray[i].width *= zoom_factor;
			mRegionArray[i].height *= zoom_factor;
		}

		// Check the image width/height and change image_rect.x/.y if appropriate.
		if ( !grid.modulo_X ) {
			if ( imageRect.width >= iWidth_Preferred ) {
				if ( (imageRect.width + imageRect.x) < iWidth_Preferred ) imageRect.x = iWidth_Preferred - imageRect.width;
				imageRect.x = (imageRect.x > 0) ? 0 : imageRect.x;
			} else {
				imageRect.x = 0;
			}
		}

		if ( imageRect.height >= iHeight_Preferred ) {
			if ( (imageRect.height + imageRect.y) < iHeight_Preferred ) imageRect.y = iHeight_Preferred - imageRect.height;
			imageRect.y = (imageRect.y > 0) ? 0 : imageRect.y;
		} else {
			imageRect.y = 0;
		}

		// This takes care of some checks associated with changing imageRect.x and imageRect.y
		this.scroll_X(0);
		this.scroll_Y(0);

		clip_width = (imageRect.width < iWidth_Preferred) ? imageRect.width : iWidth_Preferred;
		clip_height = (imageRect.height < iHeight_Preferred) ? imageRect.height : iHeight_Preferred;
		for (int i=0; i<mToolArray.length; i++) {
			mToolArray[i].applyClipRect(0, 0, clip_width, clip_height);
		}

		for (int i=0; i<mToolArray.length; i++) {
			mToolArray[i].setUserLocation(mToolArrayUser_X[i],mToolArrayUser_Y[i]);
		}

		for (int i=0; i<mRegionArray.length; i++) {
			mRegionArray[i].setUserLocation(mRegionArrayUser_X[i],mRegionArrayUser_Y[i]);
		}

	}


	public int getSelected()
	{
		return selected_tool;
	}

	public MapTool getTool()
	{
		return mToolArray[selected_tool];
	}

	public MapTool getTool(int i)
	{
		return mToolArray[i];
	}

	public void newToolFromOld(int i, MapTool new_tool, MapTool old_tool)
	{
		int alteration=0;

		new_tool.setGrid(grid);
		new_tool.setRange_X(old_tool.range_X[LO],old_tool.range_X[HI]);
		new_tool.setRange_Y(old_tool.range_Y[LO],old_tool.range_Y[HI]);
		new_tool.setUser_X(old_tool.user_X[LO],old_tool.user_X[HI]);
		new_tool.setUser_Y(old_tool.user_Y[LO],old_tool.user_Y[HI]);
		new_tool.setSnapping(old_tool.getSnap_X(),old_tool.getSnap_Y());
		new_tool.drawHandles = old_tool.drawHandles;

		mToolArray[i] = new_tool;
		mToolArray[i].applyClipRect(0, 0, clip_width, clip_height);

		// The check_for_zero_range() function expands the
		// tool when necessary and may force us to update the
		// user_X/Y values.
		alteration = getTool().check_for_zero_range();
		if (alteration == 1 || alteration == 3) mToolArray[i].setUser_X();
		if (alteration == 2 || alteration == 3) mToolArray[i].setUser_Y();
		mToolArray[i].saveHandles();
	}

	public void setTool(int i, MapTool tool){
		int alteration=0;
		mToolArray[i] = tool;
		mToolArray[i].setGrid(grid);
		mToolArray[i].applyClipRect(0, 0, clip_width, clip_height);
		mToolArray[i].setUser_XY();
		mToolArray[i].check_for_zero_range();
		mToolArray[i].setUser_XY();
		if (i == selected_tool) mToolArray[i].drawHandles = true;
		mToolArray[i].saveHandles();
	}

	public void setToolArray(MapTool [] mToolArray){
		this.mToolArray = mToolArray;
		selected_tool = 0;
		mToolArray[selected_tool].drawHandles = true;
		for (int i=0; i<mToolArray.length; i++) {
			mToolArray[i].setGrid(grid);
			mToolArray[i].applyClipRect(0, 0, clip_width, clip_height);
			mToolArray[i].setUser_XY();
		}
	}

	public void selectTool(int id){
		for (int i=0; i<id; i++) mToolArray[i].drawHandles = false;
		mToolArray[id].drawHandles = true;
		for (int i=id+1; i<mToolArray.length; i++) mToolArray[i].drawHandles = false;
		if ( mToolArray[id].getDelta_X() != 0 ) grid.setDelta_X(mToolArray[id].getDelta_X());
		if ( mToolArray[id].getDelta_Y() != 0 ) grid.setDelta_Y(mToolArray[id].getDelta_Y());
		selected_tool = id;
		repaint();
	}

	public void setRegionArray(MapRegion [] mRegionArray){
		this.mRegionArray = mRegionArray;
		for (int i=0; i<mRegionArray.length; i++) {
			mRegionArray[i].setGrid(grid);
			mRegionArray[i].setUserLocation();
		}
	}

	public void setGrid(MapGrid grid){
		this.grid = grid;
		this.grid.imageRect = this.imageRect;
		for (int i=0; i<mToolArray.length; i++) {
			mToolArray[i].setGrid(this.grid);
		}
		for (int i=0; i<mRegionArray.length; i++) {
			mRegionArray[i].setGrid(grid);
		}
	}

	public MapGrid getGrid() {
		return grid;
	}

	public void setImage(Image image) throws Exception {
		if ( image != null ){
			this.base_image = image;
	    } else {
			throw new Exception("null image passed to MapCanvas.  Reusing previous image.");
		}
		scale_image_to_fit();
		Graphics g = getGraphics();
		repaint();
	}

	public Image get_image() { return base_image; }


	/**
	 * Returns a string with information for initial positioning and
	 * and sizing of the base map.  This information can be used to
	 * initialize a new MapCanvas with the constructor which includes
	 * the x, y and scaling parameters.
	 */
	public String get_internals()
	{
		StringBuffer sbuf = new StringBuffer(imageRect.x +" " +imageRect.y +
				" " + image_scaling +
			    " " + min_img_scaling +
				" " + max_img_scaling);
		return sbuf.toString();
	}

	/*
	 * Some intelligence to do initial sizing when a new image is received.
	 */
	void scale_image_to_fit() {
		double vert_scaling = 1.0;
		double hor_scaling = 1.0;

		vert_scaling = (double)this.iHeight_Preferred / (double)base_image.getHeight(this);
		hor_scaling = (double)this.iWidth_Preferred / (double)base_image.getWidth(this);
		image_scaling = (vert_scaling < hor_scaling) ? vert_scaling : hor_scaling;

		if ( image_scaling < 0.1 ) {
			System.out.println("image scaling = " + image_scaling + ", being reset to 1.");
			image_scaling = 1;
		}
		min_img_scaling = image_scaling;

		imageRect.x = 0;
		imageRect.y = 0;
		imageRect.width = (int)(base_image.getWidth(this)*image_scaling);
		imageRect.height = (int)(base_image.getHeight(this)*image_scaling);
		grid.imageRect = this.imageRect;

		clip_width = (imageRect.width < iWidth_Preferred) ? imageRect.width : iWidth_Preferred;
		clip_height = (imageRect.height < iHeight_Preferred) ? imageRect.height : iHeight_Preferred;
	}


  /*
   * Some intelligence to do image initializing.
   */
  void position_and_scale_image(int x, int y, double scaling)
  {

    image_scaling = scaling;

    imageRect.x = x;
    imageRect.y = y;
    imageRect.width = (int)(base_image.getWidth(this)*image_scaling);
    imageRect.height = (int)(base_image.getHeight(this)*image_scaling);

    //grid.imageRect = this.imageRect;
    for (int i=0; i<mToolArray.length; i++) {
      mToolArray[i].grid.imageRect = imageRect;
    }

    clip_width = (imageRect.width < iWidth_Preferred) ? imageRect.width : iWidth_Preferred;
    clip_height = (imageRect.height < iHeight_Preferred) ? imageRect.height : iHeight_Preferred;

  }

	class GrayFilter extends RGBImageFilter {
		public GrayFilter() {canFilterIndexColorModel = true;}
		public int filterRGB(int x, int y, int rgb) {
			int a = rgb & 0xff000000;

			int r = (rgb & 0xff0000) >> 16;
			int g = (rgb & 0x00ff00) >> 8;
			int b = (rgb & 0x0000ff);
//      int gray = (int)(.3 * r + .59 * g + .11 * b);
			int gray = 128 + (int)(.075 * r + .145 * g + .027 * b);
			return a | (gray << 16) | (gray << 8) | gray;
		}
	}

}
