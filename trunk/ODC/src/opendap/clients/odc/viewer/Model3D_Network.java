package opendap.clients.odc.viewer;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** models a network: nodes connected by curves or line segments
 *  each node can have an integer "value" and a string label
 *  each segment can have an integer "weight" and a string label
 *  the model is in an xyz reference frame 
 */
public class Model3D_Network {
	int ctNodes = 0;
	int[] ax; // one-based arrays
	int[] ay;
	int[] az;
	int[] avalue;
	String[] alabel_node;
	int ctSegments = 0;
	int[] afrom;
	int[] ato;
	int[] aweight;
	String[] alabel_segment;
	
	boolean mzShowNodes = true;
	boolean mzShowNodeLabels = true;
	boolean mzShowSegments = true;

	private final BasicStroke strokeRoad = new BasicStroke(
      3f 
	);
	
	public Model3D_Network(){
		ctNodes = 0;
		ax = new int[1001]; // one-based arrays
		ay = new int[1001];
		az = new int[1001];
		avalue = new int[1001];
		alabel_node = new String[1001];
		ctSegments = 0;
		afrom = new int[1001];
		ato = new int[1001];
		aweight = new int[1001];
		alabel_segment = new String[1001];
	}

	public final void nodeAdd( int x, int y, int z, int value, String label ){
		ctNodes++;
		ax[ctNodes] = x;
		ay[ctNodes] = y;
		az[ctNodes] = z;
		avalue[ctNodes] = value;
		alabel_node[ctNodes] = label;
	}

	public final void segmentAdd( int xNodeFrom, int xNodeTo, int weight, String label ){
		ctSegments++;
		afrom[ctSegments] = xNodeFrom;
		ato[ctSegments] = xNodeTo;
		aweight[ctSegments] = weight;
		alabel_segment[ctSegments] = label;
	}
	
	public final double getDistance( int xNodeFrom, int xNodeTo )
	{
		return Math.sqrt( 
				(ax[xNodeFrom] - ax[xNodeTo]) * (ax[xNodeFrom] - ax[xNodeTo]) + 
				(ay[xNodeFrom] - ay[xNodeTo]) * (ay[xNodeFrom] - ay[xNodeTo]) + 
				(az[xNodeFrom] - az[xNodeTo]) * (az[xNodeFrom] - az[xNodeTo]) 
				);
	}
	
	public final double getDistance( int xNode, int x, int y, int z )
	{
		return Math.sqrt( 
				(ax[xNode] - x) * (ax[xNode] - x) + 
				(ay[xNode] - y) * (ay[xNode] - y) + 
				(az[xNode] - z) * (az[xNode] - z) 
				);
	}
	
	/** returns 1-based array of nodes that are connected to only one other node */  
	public final int[] getTerminals(){
		boolean zWriting = false;
		int[] aiTerminals = null;
		while( true ){
			int ctTerminals = 0;
			for( int xNode = 1; xNode <= ctNodes; xNode++ ){
				int ctSegmentsWithNode = 0;
				for( int xSegment = 1; xSegment <= ctSegments; xSegment++ ){
					if( afrom[xSegment] == xNode || ato[xSegment] == xNode ) ctSegmentsWithNode++;
				}
				if( ctSegmentsWithNode == 1 ){
					ctTerminals++;
					if( zWriting ) aiTerminals[ctTerminals] = xNode;
				}
			}
			if( zWriting ) return aiTerminals;
			aiTerminals = new int[ctTerminals + 1];
			zWriting = true;
		}
	}

	void render(  Graphics g, int x_VP, int y_VP, int w_VP, int h_VP, float fScale ){
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(
				RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY ); // emphasize image quality
		g2.setRenderingHint(
				RenderingHints.KEY_ANTIALIASING, 
				RenderingHints.VALUE_ANTIALIAS_ON);
		int iCircleWidth = 10;
		int iCircleOffset = iCircleWidth / 2;
		g2.setPaint( java.awt.Color.CYAN );
		if( mzShowNodes ){
			for( int xNode = 1; xNode <= ctNodes; xNode++ ){
				int x = (int)((ax[xNode] - x_VP)*fScale) - iCircleOffset;
				int y = (int)((ay[xNode] - y_VP)*fScale) - iCircleOffset;
				if( x >= 0 && y >= 0 && x < w_VP && y < h_VP ) g.fillOval( x, y, iCircleWidth, iCircleWidth);
			}
		}
		if( mzShowSegments ){
			java.awt.Stroke strokeDefault = ((Graphics2D)g).getStroke(); 
			((Graphics2D)g).setStroke( strokeRoad );
			for( int xSegment = 1; xSegment <= ctSegments; xSegment++ ){
				int x_from = (int)((ax[afrom[xSegment]] - x_VP) * fScale);
				int y_from = (int)((ay[afrom[xSegment]] - y_VP) * fScale);
				int x_to   = (int)((ax[ato[xSegment]] - x_VP) * fScale);
				int y_to   = (int)((ay[ato[xSegment]] - y_VP) * fScale);
				if( x_from >= 0 && y_from >= 0 && x_from < w_VP && y_from < h_VP ||   // todo case where line is in square but both nodes are not
					x_to >= 0 && y_to >= 0 && x_to < w_VP && y_to < h_VP )
						g.drawLine(x_from, y_from, x_to, y_to);
			}
			((Graphics2D)g).setStroke( strokeDefault ); // restore default stroke
		}
		if( mzShowNodeLabels ){
			for( int xNode = 1; xNode <= ctNodes; xNode++ ){
				int x = (int)((ax[xNode] - x_VP) * fScale) + iCircleWidth + 2;
				int y = (int)((ay[xNode] - y_VP) * fScale);
				if( x >= 0 && y >= 0 && x < w_VP && y < h_VP ) g2.drawString( Integer.toString(xNode), x, y);;
			}
		}
	}

}
