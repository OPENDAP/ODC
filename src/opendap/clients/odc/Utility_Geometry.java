package opendap.clients.odc;

import java.awt.geom.Point2D;

public class Utility_Geometry {
	
	public static java.awt.geom.Point2D.Double intersectTwoLines( double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4  ){
		double denominator = (y4 - y3)*(x2 - x1) - (x4 - x3)*(y2 - y1);
		double numerator_1 = (x4 - x3)*(y1 - y3) - (y4 - y3)*(x1 - x3);
		double numerator_2 = (x2 - x1)*(y1 - y3) - (y2 - y1)*(x1 - x3);
		if( denominator == 0 ){
			if( numerator_1 == 0 ){ // line segments are coincident
				return new java.awt.geom.Point2D.Double( (x3+x4)/2, (y3+y4)/2 );
			} else {
				return null; // line segments are parallel
			}
		}
		double slope_1 = numerator_1 / denominator;
		double slope_2 = numerator_2 / denominator;
		double intersection_x = x1 + slope_1 * (x2 - x1);
		double intersection_y = y1 + slope_1 * (y2 - y1);
		if( slope_1 > 0 && slope_1 < 1  && slope_2 > 0 && slope_2 < 1 ){ // line segments intersect
			return new java.awt.geom.Point2D.Double( intersection_x, intersection_y );
		} else {
			return null; // line segments do not intersect
		}
	}
	
	/** determines the cartesian distance between two sets of coordinates */
	public static double distanceTwoPoints( int x1, int y1, int x2, int y2 ){
		return Math.sqrt( (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2) );
	}

	public static double distanceTwoPoints( double x1, double y1, double x2, double y2 ){
		return Math.sqrt( (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2) );
	}

	public static double distanceTwoPoints( double x1, double y1, Point2D.Double p ){
		return Math.sqrt( (x1-p.x)*(x1-p.x) + (y1-p.y)*(y1-p.y) );
	}
	
	/** finds the closest point on a line segment to a point x, y */
	public static Point2D.Double intersectPointLineSegment( double x, double y, double x1_line, double y1_line, double x2_line, double y2_line ){
		double length_line_segment = distanceTwoPoints( x1_line, y1_line, x2_line, y2_line );
		if( length_line_segment == 0 ) return new Point2D.Double( x1_line, y1_line );
		double numerator = (((x - x1_line) * (x2_line - x1_line)) + ((y - y1_line) * (y2_line - y1_line)));
		double slope = numerator / (length_line_segment * length_line_segment);
		if( slope > 0 && slope < 1 ){ // closest point to line is on segment
			double x_intersection = x1_line + slope * (x2_line - x1_line);
			double y_intersection = y1_line + slope * (y2_line - y1_line);
			return new Point2D.Double( x_intersection, y_intersection ); 
		} else { // closest point is one of the end points
			double distance_to_1 = distanceTwoPoints( x, y, x1_line, y1_line );
			double distance_to_2 = distanceTwoPoints( x, y, x2_line, y2_line );
			return distance_to_1 < distance_to_2 ? 
					new Point2D.Double( x1_line, y1_line ) : 
					new Point2D.Double( x2_line, y2_line );  
		}
	}

	/** distance between a point and a line segment */
	public static double distancePointLineSegment( double x, double y, double x1_line, double y1_line, double x2_line, double y2_line ){
		double length_line_segment = distanceTwoPoints( x1_line, y1_line, x2_line, y2_line );
		if( length_line_segment == 0 ) return distanceTwoPoints( x, y, x1_line, y1_line );
		double numerator = (((x - x1_line) * (x2_line - x1_line)) + ((y - y1_line) * (y2_line - y1_line)));
		double slope = numerator / (length_line_segment * length_line_segment);
		if( slope > 0 && slope < 1 ){ // closest point to line is on segment
			double x_intersection = x1_line + slope * (x2_line - x1_line);
			double y_intersection = y1_line + slope * (y2_line - y1_line);
			return distanceTwoPoints( x, y, x_intersection, y_intersection );
		} else { // closest point is one of the end points
			double distance_to_1 = distanceTwoPoints( x, y, x1_line, y1_line );
			double distance_to_2 = distanceTwoPoints( x, y, x2_line, y2_line );
			return distance_to_1 < distance_to_2 ? distance_to_1 : distance_to_2;  
		}
	}
	
	/** returns acute angle in radians between lines */
	public static double angleTwoLines( double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4 ){
		double tangent;
		if( x1 - x2 == 0 ){
			if( x3 - x4 == 0 ) return 0; 
			tangent = (y3 - y4)/(x3 - x4 ); // slope 34
		} else if( x3 - x4 == 0 ){
			if( x1 - x2 == 0 ) return 0; 
			tangent = (y1 - y2)/(x1 - x2); // slope 12
		} else {
			double slope_12 = (y1 - y2)/(x1 - x2);
			double slope_34 = (y3 - y4)/(x3 - x4);
			if( slope_12 * slope_34 == -1 ){ // lines are orthogonal
				return Math.PI / 2;
			} else {
				tangent = (slope_12 - slope_34)/(1 + slope_12 * slope_34);
			}
		}
		if( tangent < 0 ) tangent = tangent * -1;
		return Math.atan( tangent );
	}

	public static void drawPolylineToRaster( int[] raster, int pxWidth, int pxHeight, int[] aiXcoordinates, int[] aiYcoordinates, int iRGBA  ){
		for( int xSegment = 1; xSegment < aiXcoordinates.length; xSegment++ ){
			int x1 = aiXcoordinates[xSegment - 1];
			int y1 = aiYcoordinates[xSegment - 1];
			int x2 = aiXcoordinates[xSegment];
			int y2 = aiYcoordinates[xSegment];
			drawLineToRaster( raster, pxWidth, pxHeight, x1, x2, y1, y2, iRGBA );
		}
	}
	
	public static void drawLineToRaster( int[] raster, int pxWidth, int pxHeight, int x1, int y1, int x2, int y2, int iRGBA  ){
		int iRasterLength = raster.length;
		int xLittleStep = 0;
		int xBigStep = 0;
		int x = x1;
		int y = y1;
		int iBigStep, iLittleStep;
		int diffX, diffY;
		int stepX, stepY;
		
		// parametize
		if( x1 <= x2 ){
			diffX = x2 - x1;
			stepX = 1;
			if( y1 <= y2 ){
				diffY = y2 - y1;
				stepY = 1;
			} else {
				diffY = y1 - y2;
				stepY = -1;
			}
		} else {
			diffX = x1 - x2;
			stepX = -1;
			if( y1 <= y2 ){
				diffY = y2 - y1;
				stepY = 1;
			} else {
				diffY = y1 - y2;
				stepY = -1;
			}
		}
		int iXY = diffX * diffY;
		if( diffX > diffY ){
			iBigStep = diffX;
			iLittleStep = diffY;
		} else {
			iBigStep = diffY;
			iLittleStep = diffX;
		}

		// draw line
		xBigStep = iBigStep;
		while( true ){
			if( xLittleStep > iXY ) break;
			int iRasterCoordinate = x + y * pxWidth;
			if( x >= 0 && x < pxWidth && y >= 0 && y < pxHeight && iRasterCoordinate < iRasterLength ) // must do these checks because arrow bounds could exceed plot area
				raster[iRasterCoordinate] = iRGBA;
			xLittleStep += iLittleStep;
			if( diffX > diffY ) x += stepX; else y += stepY;
			if( xLittleStep >= xBigStep ){
				if( diffX > diffY ) y += stepY; else x += stepX;
				xBigStep += iBigStep;
			}
		}
	
	}
	
	public static void drawOrthogonalLineX( int[] raster, int pxWidth, int pxHeight, int x, int y1, int y2, int iRGBA  ){
		int maxRasterValue = raster.length - 1;
		if( y1 < y2 ){
			for( int y = y1; y <= y2; y++ ){
				int iRasterCoordinate = x + y * pxWidth;
				if( iRasterCoordinate < 0 || iRasterCoordinate > maxRasterValue ) continue;
				raster[iRasterCoordinate] = iRGBA;
			}
		} else {
			for( int y = y2; y <= y1; y++ ){
				int iRasterCoordinate = x + y * pxWidth;
				if( iRasterCoordinate < 0 || iRasterCoordinate > maxRasterValue ) continue;
				raster[iRasterCoordinate] = iRGBA;
			}
		}
	}

	public static void drawOrthogonalLineY( int[] raster, int pxWidth, int pxHeight, int x1, int x2, int y, int iRGBA  ){
		int maxRasterValue = raster.length - 1;
		if( x1 < x2 ){
			for( int x = x1; x <= x2; x++ ){
				int iRasterCoordinate = x + y * pxWidth;
				if( iRasterCoordinate < 0 || iRasterCoordinate > maxRasterValue ) continue;
				raster[iRasterCoordinate] = iRGBA;
			}
		} else {
			for( int x = x2; x <= x1; x++ ){
				int iRasterCoordinate = x + y * pxWidth;
				if( iRasterCoordinate < 0 || iRasterCoordinate > maxRasterValue ) continue;
				raster[iRasterCoordinate] = iRGBA;
			}
		}
	}

	// from corner to corner
	public static void drawRectangle( int[] raster, int pxWidth, int pxHeight, int x1, int y1, int x2, int y2, int iRGBA  ){
		drawOrthogonalLineX( raster, pxWidth, pxHeight, x1, y1, y2, iRGBA  );
		drawOrthogonalLineX( raster, pxWidth, pxHeight, x2, y1, y2, iRGBA  );
		drawOrthogonalLineY( raster, pxWidth, pxHeight, x1, x2, y1, iRGBA  );
		drawOrthogonalLineY( raster, pxWidth, pxHeight, x1, x2, y2, iRGBA  );
	}
	
}
