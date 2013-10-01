package opendap.clients.odc.graphics;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;

public class BoxArray {
	private int[] aiRaster = null;

	// input values
	private int pxOffset_x = 0;
	private int pxOffset_y = 0;
	private int pxAlley = 0;
	private int pxMargin_top = 0;
	private int pxMargin_bottom = 0;
	private int pxMargin_left = 0;
	private int pxMargin_right = 0;
	private int pxBoxHeight = 0;
	private int pxBoxWidth = 0;
	private int ctRows = 0;
	private int ctColumns = 0;
	private int argbBackground = 0;
	private int argbBoxColor[][] = null;

	// derived values
	private int pxBufferWidth = 0;
	private int pxBufferHeight = 0;
	private int pxArrayWidth = 0;
	private int pxArrayHeight = 0;
	private int[] apxRowBaseline_x = null; // 1-based coordinates for box baselines, 0 = left/top edge of box array, [ctColumns/Rows + 1] = right/bottom edge of array
	private int[] apxRowBaseline_y = null;
	private int[] apxRow_x = null; // 1-based coordinates for box upper left corner, 0 = left/top edge of box array, [ctColumns/Rows + 1] = right/bottom edge of array
	private int[] apxRow_y = null;
	private int[] apxColumn_x = null;
	private int[] apxColumn_y = null;

	private BoxArray(){}

	public static BoxArray create( BufferedImage bi, int pxOffset_x, int pxOffset_y, int ctRows, int ctColumns, int pxBoxWidth, int pxBoxHeight, int argbBoxColor[][], int pxMargin_top, int pxMargin_bottom, int pxMargin_left, int pxMargin_right, StringBuffer sbError ){
		BoxArray instance = new BoxArray();
		return instance;
	}

	public static BoxArray create( BufferedImage bi, int pxOffset_x, int pxOffset_y, int ctRows, int ctColumns, int pxBoxWidth, int pxBoxHeight, int argbBoxColor[][], int pxMargin, StringBuffer sbError ){
		return create( bi, pxOffset_x, pxOffset_y, ctRows, ctColumns, pxBoxWidth, pxBoxHeight, argbBoxColor, pxMargin, pxMargin, pxMargin, pxMargin, sbError );
	}

	public int[] getRowBaseline_x(){ return apxRowBaseline_x; }
	public int[] getRowBaseline_y( int xRow1 ){ return apxRowBaseline_y; }
	public int[] getRow_x(){ return apxRow_x; }
	public int[] getRow_y( int xRow1 ){ return apxRow_y; }
	public int[] getColumn_x(){ return apxColumn_x; }
	public int[] getColumn_y( int xRow1 ){ return apxColumn_y; }

	public void draw(){

		// draw background
		for( int pxScanline = pxOffset_y; pxScanline < pxArrayHeight; pxScanline++ )
			for( int px_x = pxOffset_x; px_x < pxOffset_x + pxArrayWidth; px_x++ )
				aiRaster[ pxScanline * pxBufferWidth + px_x ] = argbBackground;

		// draw boxes
		for( int xBox_row = 1; xBox_row <= ctRows; xBox_row++ ){
			for( int xBox_col = 1; xBox_col <= ctColumns; xBox_col++ ){
				for( int pxScanline = apxRow_y[xBox_row]; pxScanline < pxBoxHeight; pxScanline++ )
					for( int px_x = apxColumn_x[xBox_col]; px_x < pxBoxWidth; px_x++ )
						aiRaster[ pxScanline * pxBufferWidth + px_x ] = argbBoxColor[ xBox_row ][ xBox_col ];
			}
		}
	}

	public boolean zResize( StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}

	public boolean setBuffer( BufferedImage bi, StringBuffer sbError ){
		if( bi.getType() != BufferedImage.TYPE_INT_ARGB ){
			sbError.append( "buffered image is of the wrong type ( " + bi.getType() + " ), must be an integer ARGB." );
			return false;
		}
		Raster raster = bi.getRaster();
		if( raster == null ){
			sbError.append( "raster for buffer image is missing." );
			return false;
		}
		aiRaster = ((DataBufferInt)raster.getDataBuffer()).getData();
		return true;
	}

}

class BoxArray_TestPanel extends JPanel {
	final public static boolean DEBUG = false;
	JPanel panelControls = new JPanel();
	JPanel panelDisplay = new JPanel();
	public static void main( String[] args )
	{
		if( DEBUG) System.out.println("FormTestPanel main, tests FormLayout");
		JFrame frame = new JFrame();
		java.awt.event.WindowListener listenerCloser = new java.awt.event.WindowAdapter(){
			@Override
			public void windowClosing( java.awt.event.WindowEvent e ){
				if( DEBUG) System.out.println("closed");
				System.exit(0);
			}
		};
		frame.addWindowListener( listenerCloser );
		BoxArray_TestPanel test_panel = new BoxArray_TestPanel();
		frame.add(test_panel);
		frame.pack();
		final int iFrameWidth = frame.getWidth();
		final int iFrameHeight = frame.getHeight();
		java.awt.Dimension dimScreenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation( dimScreenSize.width/2 - (iFrameWidth/2), dimScreenSize.height/2 - (iFrameHeight/2) );
		frame.setVisible( true );
	}

	BoxArray_TestPanel(){

//	for( int a = 1; a < 50; a++ ){
//		for( int b = a + 1; b < 51; b++ ){
//			for( int c = 1; c < 700; c++ ){
//				int a = 1;
//				int b = 13;
//				int c = 9;
//FindAnswer:
//				int ctSolutions = 0;
//				for( int x = -200; x < 200; x++ ){
//					for( int y = -200; y < 200; y++ ){
//						if( a*x + b*y == c ){
//							if( x + y < 80 ) break FindAnswer;
//							if( x > 0 && y > 0 ){
//								if( DEBUG) System.out.println("answer, a = " + a + " b = " + b + " c = " + c + " x = " + x + " y = " + y + " x+y = " + (x+y) );
//								ctSolutions++;
//							}
//							break FindAnswer;
//						}
//					}
//				}
//				if( DEBUG) System.out.println( "c: " + c + " number of solutions: " +  ctSolutions );
//			}
//		}
//	}

//		long nStart = System.currentTimeMillis();
//		double dAnswer = 0, dQuotient;
//		dQuotient = 4.123d;
//		double d1 = 5.6;
//		double d2 = 6.5;
//		for( int x = 1; x < 100000000; x++ ){
//			dAnswer = Math.sqrt(dQuotient);
//			dAnswer = d1 * d2;
//		}
//		if( DEBUG) System.out.println("total time: " + (System.currentTimeMillis() - nStart) );
//		if( true ) System.exit(0);

		this.setBorder( BorderFactory.createLineBorder( Color.YELLOW));
		panelControls.add( new JLabel("control panel") );

//		panelDisplay.setLayout( new FormLayout(panelDisplay) );
//
//		panelDisplay = this;
//		JLabel label1 = new JLabel("label 1");
//if( DEBUG) System.out.println("label 1 preferred size: " + label1.getPreferredSize().getWidth() );
//
//		label1.setBorder( BorderFactory.createLineBorder(Color.BLUE ));
//		FormLayout layout = new FormLayout( panelDisplay );
//		panelDisplay.setLayout( layout );
//		panelDisplay.add( label1 );
//		panelDisplay.add( new JTextField("text field 1") );
//		panelDisplay.add( new JLabel("label 2") );
//		panelDisplay.add( new JTextField("text field 2") );
//		panelDisplay.add( new JLabel("label 3") );
//		panelDisplay.add( new JTextField("text field 3") );
//		panelDisplay.add( new JLabel("label 4") );
//		panelDisplay.add( new JTextField("text field 4xyzt") );
//		panelDisplay.add( new JLabel("label 5") );
//		panelDisplay.add( new JTextField("text field 5") );
//		panelDisplay.add( new JLabel("label 6") );
//		panelDisplay.add( new JTextField("text field 6") );
//
//		layout.setMargin( 4, 4 );

//		layout.setSpacing_Default( 0, 5, 0, 0, 10 );

	}

	void vDumpComponentSizing( Component c ){
		if( DEBUG) System.out.println( "minimum: " + c.getMinimumSize() );
		if( DEBUG) System.out.println( "maximum: " + c.getMaximumSize() );
		if( DEBUG) System.out.println( "preferred: " + c.getPreferredSize() );
	}

}

/**
 * This class can read chunks of RGB image data out of a file and return a BufferedImage.
 * It may use an optimized technique for loading images that relies on assumptions about the
 * default image format on Windows.
 * OFF OF SO
		mbi.
   BufferedImage currentImage = new BufferedImage(width,height,BufferedImage.TYPE_3BYTE_BGR);
   byte[] imgData = ((DataBufferByte)currentImage.getRaster().getDataBuffer()).getData();
   System.arraycopy(frame,0,imgData,0,frame.length);

 *
public class RGBImageLoader
{
    private byte[] tempBuffer_;
    private boolean fastLoading_;

    public RGBImageLoader()
    {
        fastLoading_ = canUseFastLoadingTechnique();
    }

    private boolean canUseFastLoadingTechnique()
    {
        // Create an image that's compatible with the screen
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        BufferedImage image = gc.createCompatibleImage(100, 100, Transparency.TRANSLUCENT);

        // On windows this should be an ARGB integer packed raster. If it is then we can
        // use our optimization technique

        if(image.getType() != BufferedImage.TYPE_INT_ARGB)
            return false;

        WritableRaster raster = image.getRaster();

        if(!(raster instanceof IntegerInterleavedRaster))
            return false;

        if(!(raster.getDataBuffer() instanceof DataBufferInt))
            return false;

        if(!(image.getColorModel() instanceof DirectColorModel))
            return false;

        DirectColorModel colorModel = (DirectColorModel) image.getColorModel();

        if(!(colorModel.getColorSpace() instanceof ICC_ColorSpace) ||
             colorModel.getNumComponents() != 4 ||
             colorModel.getAlphaMask() != 0xff000000 ||
             colorModel.getRedMask() != 0xff0000 ||
             colorModel.getGreenMask() != 0xff00 ||
             colorModel.getBlueMask() != 0xff)
            return false;

        if(raster.getNumBands() != 4 ||
           raster.getNumDataElements() != 1 ||
           !(raster.getSampleModel() instanceof SinglePixelPackedSampleModel))
            return false;

        return true;
    }

    public BufferedImage loadImage(File file, int width, int height, long imageOffset) throws IOException
    {
        if(fastLoading_)
            return loadImageUsingFastTechnique(file, width, height, imageOffset);
        else
            return loadImageUsingCompatibleTechnique(file, width, height, imageOffset);
    }

    private BufferedImage loadImageUsingFastTechnique(File file, int width, int height, long imageOffset) throws IOException
    {
        int sizeBytes = width * height * 3;

        // Make sure buffer is big enough
        if(tempBuffer_ == null || tempBuffer_.length < sizeBytes)
            tempBuffer_ = new byte[sizeBytes];

        RandomAccessFile raf = null;
        try
        {
            raf = new RandomAccessFile(file, "r");

            raf.seek(imageOffset);

            int bytesRead = raf.read(tempBuffer_, 0, sizeBytes);
            if (bytesRead != sizeBytes)
                throw new IOException("Invalid byte count. Should be " + sizeBytes + " not " + bytesRead);

            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
            BufferedImage image = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
            WritableRaster raster = image.getRaster();
            DataBufferInt dataBuffer = (DataBufferInt) raster.getDataBuffer();

            addAlphaChannel(tempBuffer_, sizeBytes, dataBuffer.getData());

            return image;
        }
        finally
        {
            try
            {
                if(raf != null)
                raf.close();
            }
            catch(Exception ex)
            {
            }
        }
    }

    private BufferedImage loadImageUsingCompatibleTechnique(File file, int width, int height, long imageOffset) throws IOException
    {
        int sizeBytes = width * height * 3;

        RandomAccessFile raf = null;
        try
        {
            raf = new RandomAccessFile(file, "r");

            // Lets navigate to the offset
            raf.seek(imageOffset);

            DataBufferByte dataBuffer = new DataBufferByte(sizeBytes);
            byte[] bytes = dataBuffer.getData();

            int bytesRead = raf.read(bytes, 0, sizeBytes);
            if (bytesRead != sizeBytes)
                throw new IOException("Invalid byte count. Should be " + sizeBytes + " not " + bytesRead);

            WritableRaster raster = Raster.createInterleavedRaster(dataBuffer, // dataBuffer
                            width, // width
                            height, // height
                            width * 3, // scanlineStride
                            3, // pixelStride
                            new int[]{0, 1, 2}, // bandOffsets
                            null); // location

            ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), // ColorSpace
                            new int[]{8, 8, 8}, // bits
                            false, // hasAlpha
                            false, // isPreMultiplied
                            ComponentColorModel.OPAQUE, DataBuffer.TYPE_BYTE);

            BufferedImage loadImage = new BufferedImage(colorModel, raster, false, null);

            // Convert it into a buffered image that's compatible with the current screen.
            // Not ideal creating this image twice....
            BufferedImage image = createCompatibleImage(loadImage);

            return image;
        }
        finally
        {
            try
            {
                if(raf != null)
                raf.close();
            }
            catch(Exception ex)
            {
            }
        }
    }

    private BufferedImage createCompatibleImage(BufferedImage image)
    {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

        BufferedImage newImage = gc.createCompatibleImage(image.getWidth(), image.getHeight(), Transparency.TRANSLUCENT);

        Graphics2D g = newImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return newImage;
    }


    private void addAlphaChannel(byte[] rgbBytes, int bytesLen, int[] argbInts)
    {
        for(int i=0, j=0; i<bytesLen; i+=3, j++)
        {
            argbInts[j] = ((byte) 0xff) << 24 |                 // Alpha
                        (rgbBytes[i] << 16) & (0xff0000) |      // Red
                        (rgbBytes[i+1] << 8) & (0xff00) |       // Green
                        (rgbBytes[i+2]) & (0xff);               // Blue
        }
    }

}
 */
