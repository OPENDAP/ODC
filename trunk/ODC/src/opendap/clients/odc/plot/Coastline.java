package opendap.clients.odc.plot;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.Utility;

import java.nio.ByteBuffer;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.File;
import java.nio.channels.FileChannel;
import java.io.RandomAccessFile;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.RenderingHints;

// Global Self-consistant Hierarchical High-resolution Shorelines
// Version 1.2 May 18, 1999
//
// Paul Wessel, G&G, SOEST, U of Hawaii (wessel@soest.hawaii.edu)
// Walter H. F. Smith, NOAA Geosciences Lab (walter@amos.grdl.noaa.gov)
//
// Ref: Wessel, P., and W. H. F. Smith, 1996, A global self-consistent,
//        hierarchical, high-resolution shoreline database, J. Geophys.
//        Res., 101, 8741-8743.
//
// The files contain several successive logical blocks of the form
//
// <polygon header>
// <polygon points>
//
// Each header consist of the following variables:
//
// int id;	                      /* Unique polygon id number, starting at 0 */
// int n;                         /* Number of points in this polygon */
// int level;                     /* 1 land, 2 lake, 3 island_in_lake, 4 pond_in_island_in_lake */
// int west, east, south, north;  /* min/max extent in micro-degrees */
// int area;                      /* Area of polygon in 1/10 km^2 */
// short int greenwich;           /* Greenwich is 1 if Greenwich is crossed */
// short int source;              /* 0 = CIA WDBII, 1 = WVS */
//
// Here, int is 4-byte integers and short means 2-byte integers.
//
// The polygon points are stored as n successive records of the form
//
// int	x;	/* longitude of a point in micro-degrees */
// int	y;	/* latitude of a point in micro-degrees */

// File		   Description       Resolution   File
// -------------------------------------------------
// gshhs_f.b   Full               0.04 km    87M
// gshhs_h.b   High               0.2  km    20M
// gshhs_i.b   Intermediate       1.0  km     5M
// gshhs_l.b   Low                5.0  km     1M
// gshhs_c.b   Crude             25    km   167K

// first line of c is:
// P      0    1240 1 W 79793839.900 -17.53378 190.32600 -34.83044  77.71625

//Paul Wessel and Walter. H. F. Smith
//GMT URL:   http://www.soest.hawaii.edu/soest/gmt.html

public class Coastline {

	final public static String RELATIVE_PATH_Crude = "/coastline/gshhs_c.b";

	final public static int MINIMUM_POLYGON_POINT_COUNT = 10;
	final public static int MINIMUM_POLYGON_AREA = 0;

	final static int SIDE_NORTH = 1;
	final static int SIDE_EAST = 2;
	final static int SIDE_SOUTH = 3;
	final static int SIDE_WEST = 4;

	static Polygons mPolygonsCrude = null;
	static Polygons mPolygonsLow = null;
	static Polygons mPolygonsIntermediate = null;
	static Polygons mPolygonsHigh = null;
	static Polygons mPolygonsFull = null;
	private PlotLinestyle mLinestyle = new PlotLinestyle(PlotLinestyle.DEFAULT_Coastline);
	final public static double iCircumferenceOfEarth_km = 40075d;

	static final Polygons getPolygonsInRegion( int[] aiLongitude_micro, int[] aiLatitude_micro, StringBuffer sbError ){
		int ctLongitudes = aiLongitude_micro.length - 1;
		int ctLatitudes  = aiLatitude_micro.length - 1;
		int iLongitudeWest = aiLongitude_micro[1];
		int iLongitudeEast = aiLongitude_micro[ctLongitudes];
		int iLatitudeSouth  = aiLatitude_micro[1];
		int iLatitudeNorth  = aiLatitude_micro[ctLatitudes];
		return getPolygonsInRegion( ctLongitudes, iLongitudeWest, iLongitudeEast, ctLatitudes, iLatitudeSouth, iLatitudeNorth, sbError );
	}

	// get the one-based line segments for this region
	// D1 = segment index, D2 = dimension (1 = LON, 2 = LAT), D3 = value (either LON/LAT)
	static final Polygons getPolygonsInRegion( int ctPixels_width, int iLon_W, int iLon_E, int ctPixels_height, int iLat_S, int iLat_N, StringBuffer sbError ){

		// todo transformation in structured way
		if( iLon_W < 0 ) iLon_W += 360000000;
		if( iLon_E < 0 ) iLon_E += 360000000;

		boolean zWrapped_Lon = iLon_W > iLon_E;
		boolean zWrapped_Lat = iLat_S > iLat_N;

//System.out.println("getting polygons in region, N: " + iLat_N + " E: " + iLon_E + " S: " + iLat_S + " W: " + iLon_W);

		double dRange_lon = (iLon_E > iLon_W) ? (double)(iLon_E - iLon_W) : (double)(360000000 - iLon_W + iLon_E);
		double dRange_lat = (iLat_N > iLat_S) ? (double)(iLat_N - iLat_S) : (double)(180000000 - iLat_S + iLat_N);
		double lon_resolution_km = dRange_lon * iCircumferenceOfEarth_km / 360000000d / (double)ctPixels_width;
		double lat_resolution_km = dRange_lat * 2d * iCircumferenceOfEarth_km / 360000000d / (double)ctPixels_height;
		double resolution_km     = lon_resolution_km > lat_resolution_km ? lat_resolution_km : lon_resolution_km;
//System.out.println("dRange_lon: " + dRange_lon + " lat: " + dRange_lat + " pixel width: " + ctPixels_width + " height: " + ctPixels_height);
//System.out.println("resolution km: " + resolution_km);

		// todo be able to get from file instead of load entire file
		// use extents
		Polygons polygons = getAllPolygons( resolution_km, sbError );
		if( polygons == null ) return null;
//		Polygons polygons = getAllPolygons_Test();

//int ctAccepted = 0;
//int ctExcluded = 0;
		// create the line segment array
		long nStart = System.currentTimeMillis();
		Polygons region_polygons = new Polygons();
		int[] segmentCurrent1_X = null;
		int[] segmentCurrent1_Y = null;
		final int STATE_CountingSubPolygons = 1;
		final int STATE_CountingPoints = 2;
		final int STATE_Filling = 3;
		int eSTATE = STATE_CountingSubPolygons;
		int ctPolygon = polygons.getPolygonCount();
		int ctRegionPolygons = 0;
		int xRegionPolygons = 0;
		int iTotalPoints = polygons.getTotalPointCount();
		int iPointsProcessed = 0;

		int ctIntersections;
		int[] aiIntersectionSide = new int[1000];
		int[] aiIntersectionLonLat = new int[1000]; // if the side is North/South it will be a longitude and vice versa
		int[] aiIntersectionIndex_in = new int[1000]; // the point before which the intersection occurs
		int[] aiIntersectionIndex_out = new int[1000]; // the point before which the intersection occurs
		int[] aiIntersectionIndex_twin = new int[1000]; // the twin intersection if there is one
		int[] aiIntersectionSubPolygon = new int[1000]; // the index number of the sub-polygon to which this intersection belongs

		while( true ){
			xRegionPolygons = 0;
			for( int xPolygon = 1; xPolygon <= ctPolygon; xPolygon++ ){

				if( zRegionsExclusive(  polygons.aiExtent_west[xPolygon],
										polygons.aiExtent_east[xPolygon],
										polygons.aiExtent_south[xPolygon],
										polygons.aiExtent_north[xPolygon],
										iLon_W, iLon_E, iLat_S, iLat_N )     ) continue;

				// used to determine whether each corner of region is in polygon or not
				int iCrossCount_NE = 0;
				int iCrossCount_SE = 0;
				int iCrossCount_NW = 0;
				int iCrossCount_SW = 0;

				int[] polygon1_LON = polygons.getPoints1_X(xPolygon);
				int[] polygon1_LAT = polygons.getPoints1_Y(xPolygon);
				int ctPolygonPoints = polygon1_LON.length - 1;
				ctIntersections = 0;
				boolean zLastPointInRegion = zPointInRegion( polygon1_LON[ctPolygonPoints], polygon1_LAT[ctPolygonPoints], iLon_W, iLon_E, iLat_S, iLat_N );
				int lon_previous = polygon1_LON[ctPolygonPoints];
				int lat_previous = polygon1_LAT[ctPolygonPoints];

				int iLon_E_360 = (iLon_W > iLon_E) ? iLon_E + 360000000 : iLon_E; // normalize Eastern bound if longitude wraps for intersection calculations

//if( eSTATE == 1 ) System.out.println("state " + eSTATE + " polygon " + xPolygon + " points: " + ctPolygonPoints);
				for( int xPolygonPoint = 1; xPolygonPoint <= ctPolygonPoints; xPolygonPoint++ ){
					int lon_point = polygon1_LON[xPolygonPoint];
					int lat_point =  polygon1_LAT[xPolygonPoint];
					boolean zCurrentPointInRegion = zPointInRegion( lon_point, lat_point, iLon_W, iLon_E, iLat_S, iLat_N );
//ctAccepted++;
//if( eSTATE == STATE_Filling ){
//	if( ctExcluded % 1000 == 0 ){
//		System.out.println("excluding point " + polygon1_LON[xPolygonPoint] + " : " + polygon1_LAT[xPolygonPoint] );
//	}
//	ctExcluded++;
//}

					float slope;
					int lonIntercept_North;
					int lonIntercept_South;
					int latIntercept_West = 999999999; // initialized to value outside possible intercepts
					int latIntercept_East = 999999999;
					int lon_least;
					int lon_most;
					int lat_least;
					int lat_most;

					boolean zSegmentCrosses0Lon = (lon_point - lon_previous > 60000000) || (lon_point - lon_previous < -60000000);

					// TODO the wrapped latitude problem
					// TODO adjust straight intersections for wrapping
					// determine corner relationships
					// note that as a simplification cases in which the polygon cuts across a corner
					// between two non-included points is ignored
					if( zCurrentPointInRegion && zLastPointInRegion ){ // need to determine if segment crosses longitude gap (lat gap TODO)

						// is the distance between the points greater than distance of the points from the sides of the region?
						// if so, then the point crosses the gap
						int lon_point_360 = lon_point < iLon_W ? lon_point + 360000000 : lon_point;
						int lon_previous_360 = lon_previous < iLon_W ? lon_previous + 360000000 : lon_previous;
						int diff_points = lon_point_360 > lon_previous_360 ? lon_point_360 - lon_previous_360 : lon_previous_360 - lon_point_360;
						int diff_sides = lon_point_360 > lon_previous_360 ? iLon_E_360 - lon_point_360 + lon_previous_360 - iLon_W : iLon_E_360 - lon_previous_360 + lon_point_360 - iLon_W;
						if( diff_points > diff_sides ){

//if( eSTATE == 1 && (xPolygon == 1) ){
//System.out.println("intersection over gap: " + (ctIntersections + 1));
//}

							// need to create two intersections with an undefined out point
							int iIntersectionSide = 0;
							int iIntersectionLonLat = 0;
							int insertion_index = xPolygonPoint;
							int index_previous_point = xPolygonPoint == 1 ? ctPolygonPoints : xPolygonPoint - 1;

							int lon_point_W_in = (lon_point_360 < lon_previous_360) ? lon_point_360 : lon_previous_360;
							int lon_point_E_in = (lon_point_360 < lon_previous_360) ? lon_previous_360 : lon_point_360;
							int lon_point_W_out = iLon_W - (iLon_E_360 - lon_point_E_in);
							int lon_point_E_out = iLon_E_360 + (lon_point_W_in - iLon_W);
							int lat_point_W_in = (lon_point_360 < lon_previous_360) ? lat_point : lat_previous;
							int lat_point_E_out = lat_point_W_in;
							int lat_point_E_in = (lon_point_360 < lon_previous_360) ? lat_previous : lat_point;
							int lat_point_W_out = lat_point_E_in;
							float slope_W = (float)(lat_point_W_in - lat_point_W_out)/(float)(lon_point_W_in - lon_point_W_out); // slope of western intersection
							float slope_E = (float)(lat_point_E_in - lat_point_E_out)/(float)(lon_point_E_in - lon_point_E_out); // slope of eastern intersection
							latIntercept_West = (int)((float)(iLon_W - lon_point_W_in)*slope_W) + lat_point_W_in;
							latIntercept_East = (int)((float)(iLon_E_360 - lon_point_E_in)*slope_E) + lat_point_E_in;

							// add the first intersection (the western intersection)
							ctIntersections++;
							aiIntersectionSide[ctIntersections] = SIDE_WEST;
							aiIntersectionLonLat[ctIntersections] = latIntercept_West;
							aiIntersectionIndex_in[ctIntersections] = (lon_point_360 < lon_previous_360) ? xPolygonPoint : index_previous_point;
							aiIntersectionIndex_out[ctIntersections] = (lon_point_360 < lon_previous_360) ? index_previous_point : xPolygonPoint;
							aiIntersectionIndex_twin[ctIntersections] = 0; // value assigned after sorting
//if( eSTATE == 1 ){
//System.out.println("\tpoint current: (" + lon_point + ", " + lat_point + ")");
//System.out.println("\tpoint previous: (" + lon_previous + ", " + lat_previous + ")");
//System.out.println("\tintersection side: " + aiIntersectionSide[ctIntersections]);
//System.out.println("\tintersection latlon: " + aiIntersectionLonLat[ctIntersections]);
//System.out.println("\tindex in: " + aiIntersectionIndex_in[ctIntersections]);
//System.out.println("\tindex out: " + aiIntersectionIndex_out[ctIntersections]);
//System.out.println("------------------------");
//}

							// add the second intersection (the eastern intersection)
							ctIntersections++;
							aiIntersectionSide[ctIntersections] = SIDE_EAST;
							aiIntersectionLonLat[ctIntersections] = latIntercept_East;
							aiIntersectionIndex_in[ctIntersections] = (lon_point_360 < lon_previous_360) ? index_previous_point : xPolygonPoint;
							aiIntersectionIndex_out[ctIntersections] = (lon_point_360 < lon_previous_360) ? xPolygonPoint : index_previous_point;
							aiIntersectionIndex_twin[ctIntersections] = 0; // value assigned after sorting
//if( eSTATE == 1 ){
//System.out.println("\tpoint current: (" + lon_point + ", " + lat_point + ")");
//System.out.println("\tpoint previous: (" + lon_previous + ", " + lat_previous + ")");
//System.out.println("\tintersection side: " + aiIntersectionSide[ctIntersections]);
//System.out.println("\tintersection latlon: " + aiIntersectionLonLat[ctIntersections]);
//System.out.println("\tindex in: " + aiIntersectionIndex_in[ctIntersections]);
//System.out.println("\tindex out: " + aiIntersectionIndex_out[ctIntersections]);
//}
						}
					} else
					if( zCurrentPointInRegion != zLastPointInRegion ){ // then region boundary was crossed (an intersection has occurred)
//if( eSTATE == 1 && (xPolygon == 1) ){
//System.out.println("intersection: " + (ctIntersections + 1));
//}
						int iIntersectionSide = 0;
						int iIntersectionLonLat = 0;
						int insertion_index = xPolygonPoint;
						int index_previous_point = xPolygonPoint == 1 ? ctPolygonPoints : xPolygonPoint - 1;

						if( lon_point == lon_previous ){ // vertical segment
							if( lon_point == iLon_W ){
								iIntersectionSide = SIDE_WEST;
								iIntersectionLonLat = lat_point;
							} else if( lon_point == iLon_E ){
								iIntersectionSide = SIDE_EAST;
								iIntersectionLonLat = lat_point;
							} else if( lat_previous >= iLat_N ){
								iIntersectionSide = SIDE_NORTH;
								iIntersectionLonLat = lon_point;
							} else { // lat previous is <= S
								iIntersectionSide = SIDE_SOUTH;
								iIntersectionLonLat = lon_point;
							}
						} else if( lat_point == lat_previous ){ // horizontal segment
							if( lat_point == iLat_N ){
								iIntersectionSide = SIDE_NORTH;
								iIntersectionLonLat = lon_point;
							} else if( lat_point == iLat_S ){
								iIntersectionSide = SIDE_SOUTH;
								iIntersectionLonLat = lon_point;
							} else if( lon_previous <= iLon_W ){
								iIntersectionSide = SIDE_WEST;
								iIntersectionLonLat = lat_point;
							} else { // lat previous is >= E
								iIntersectionSide = SIDE_EAST;
								iIntersectionLonLat = lat_point;
							}
						} else { // angled segment

						    if( zSegmentCrosses0Lon ){
								if( iLon_W <= lon_point && iLon_W <= lon_previous  ){
									if( lon_point < lon_previous ){
										slope = (float)(lat_point - lat_previous)/(float)(lon_point - (lon_previous - 360000000));
										latIntercept_West = (int)((float)(iLon_W - lon_point)*slope) + lat_point;
									} else {
										slope = (float)(lat_point - lat_previous)/(float)((lon_point - 360000000) - lon_previous);
										latIntercept_West = (int)((float)(iLon_W - lon_previous)*slope) + lat_previous;
									}
								} else if ( iLon_W >= lon_point && iLon_W >= lon_previous ){
									if( lon_point > lon_previous ){
										slope = (float)(lat_point - lat_previous)/(float)(lon_point - (lon_previous + 360000000));
										latIntercept_West = (int)((float)(iLon_W - lon_point)*slope) + lat_point;
									} else {
										slope = (float)(lat_point - lat_previous)/(float)((lon_point + 360000000) - lon_previous);
										latIntercept_West = (int)((float)(iLon_W - lon_previous)*slope) + lat_previous;
									}
								} else if( iLon_E <= lon_point && iLon_E <= lon_previous  ){
									if( lon_point < lon_previous ){
										slope = (float)(lat_point - lat_previous)/(float)(lon_point - (lon_previous - 360000000));
										latIntercept_East = (int)((float)(iLon_E - lon_point)*slope) + lat_point;
									} else {
										slope = (float)(lat_point - lat_previous)/(float)((lon_point - 360000000) - lon_previous);
										latIntercept_East = (int)((float)(iLon_E - lon_previous)*slope) + lat_previous;
									}
								} else if ( iLon_E >= lon_point && iLon_E >= lon_previous ){
									if( lon_point > lon_previous ){
										slope = (float)(lat_point - lat_previous)/(float)(lon_point - (lon_previous + 360000000));
										latIntercept_East = (int)((float)(iLon_E - lon_point)*slope) + lat_point;
									} else {
										slope = (float)(lat_point - lat_previous)/(float)((lon_point + 360000000) - lon_previous);
										latIntercept_East = (int)((float)(iLon_E - lon_previous)*slope) + lat_previous;
									}
								}
							} else {
								slope = (float)(lat_point - lat_previous)/(float)(lon_point - lon_previous);
								if( (iLon_W <= lon_point && iLon_W >= lon_previous) || (iLon_W >= lon_point && iLon_W <= lon_previous) ){
									latIntercept_West = (int)((float)(iLon_W - lon_point)*slope) + lat_point;
								} else if( (iLon_E <= lon_point && iLon_E >= lon_previous) || (iLon_E >= lon_point && iLon_E <= lon_previous) ){
									latIntercept_East = (int)((float)(iLon_E - lon_point)*slope) + lat_point;
								}
							}

							// todo lat wrap adjustments (analogous to lon adjustments)
							// todo consider using intercepts as above
							// note this still probably isn't right because the western bound could be
							// either small or large and vice versa for the east, it tends to work only because
							// usually the western bound is the small one
							float slope_longitudes = (float)(lat_point - lat_previous)/(float)(lon_point - lon_previous);
							lonIntercept_North = (int)((float)(iLat_N - lat_point)/slope_longitudes) + lon_point;
							lonIntercept_South = (int)((float)(iLat_S - lat_point)/slope_longitudes) + lon_point;

							lon_least = lon_point < lon_previous ? lon_point : lon_previous; // todo may have a wrappin problem here
							lon_most  = lon_point < lon_previous ? lon_previous : lon_point;
							lat_least = lat_point < lat_previous ? lat_point : lat_previous;
							lat_most  = lat_point < lat_previous ? lat_previous : lat_point;
//if( eSTATE == 1 && (xPolygon == 1) ){
//System.out.println("\tzSegmentCrosses0Lon: " + zSegmentCrosses0Lon );
//System.out.println("\tlat least: " + lat_least );
//System.out.println("\tlat most: " + lat_most );
//System.out.println("\tlonIntercept_North: " + lonIntercept_North );
//System.out.println("\tlatIntercept_East: " + latIntercept_East );
//System.out.println("\tlonIntercept_South: " + lonIntercept_South );
//System.out.println("\tlatIntercept_West: " + latIntercept_West );
//}
						    if( zSegmentCrosses0Lon ){
								if( (lonIntercept_North >= lon_most && lonIntercept_North <= 360000000) || (lonIntercept_North <= lon_least && lonIntercept_North >= 0) ){
									iIntersectionSide = SIDE_NORTH;
									iIntersectionLonLat = lonIntercept_North;
								}
								if( (lonIntercept_South >= lon_most && lonIntercept_South <= 360000000) || (lonIntercept_South <= lon_least && lonIntercept_South >= 0) ){
									iIntersectionSide = SIDE_SOUTH;
									iIntersectionLonLat = lonIntercept_South;
								}
							} else {
								if( (lonIntercept_North >= lon_least && lonIntercept_North <= lon_most) ){ // crosses North
									iIntersectionSide = SIDE_NORTH;
									iIntersectionLonLat = lonIntercept_North;
								}
								if( (lonIntercept_South >= lon_least && lonIntercept_South <= lon_most) ){ // crosses south
									iIntersectionSide = SIDE_SOUTH;
									iIntersectionLonLat = lonIntercept_South;
								}
							}
							// latitude cannot wrap todo check
							if( (latIntercept_West >= lat_least && latIntercept_West <= lat_most) ){ // crosses West
								iIntersectionSide = SIDE_WEST;
								iIntersectionLonLat = latIntercept_West;
							} else
							if( (latIntercept_East >= lat_least && latIntercept_East <= lat_most) ){ // crosses East
								iIntersectionSide = SIDE_EAST;
								iIntersectionLonLat = latIntercept_East;
							}
						}

						if( ( (iIntersectionSide == SIDE_NORTH || iIntersectionSide == SIDE_SOUTH) && (iIntersectionLonLat < 0 || iIntersectionLonLat > 360000000))
						    ||
						    ( (iIntersectionSide == SIDE_WEST  || iIntersectionSide == SIDE_EAST) && (iIntersectionLonLat < -90000000 || iIntersectionLonLat > 90000000))
						){
							sbError.append("internal error, invalid intersection in polygon " + xPolygon);
							return null;
						}

						// add the intersection
						ctIntersections++;
						aiIntersectionSide[ctIntersections] = iIntersectionSide;
						aiIntersectionLonLat[ctIntersections] = iIntersectionLonLat;
						aiIntersectionIndex_in[ctIntersections] = zCurrentPointInRegion ? xPolygonPoint : index_previous_point;
						aiIntersectionIndex_out[ctIntersections] = zCurrentPointInRegion ? index_previous_point : xPolygonPoint;
						aiIntersectionIndex_twin[ctIntersections] = 0;

//if( eSTATE == 1 && (xPolygon == 1) ){
//System.out.println("intersection: " + ctIntersections);
//System.out.println("\tpoint current: (" + lon_point + ", " + lat_point + ")");
//System.out.println("\tpoint previous: (" + lon_previous + ", " + lat_previous + ")");
//System.out.println("\tintersection side: " + iIntersectionSide);
//System.out.println("\tintersection latlon: " + iIntersectionLonLat);
//System.out.println("\tcurrent point in: " + (zCurrentPointInRegion ? "Yes" : "No"));
//}

					} // end intersection

					zLastPointInRegion = zCurrentPointInRegion;

					// determine whether corners are in polygon or not
					// a segment is assumed between each point and the previous point
					// if a ray from a corner crosses this segment then the count for the corner
					// is incremented; if the total count for all segments is odd then the
					// corner is in the polygon, if it is even then it is outside the polygon

				    // the algorithm for doing this is as follows:
					// checking if crosses ray:
					// example: vertical upward ray:
					// (1) x's must be on either side of ray (or in contact)
					// (2) if y's both above bottom y then it crosses
					// (3) if y's both below then it does not cross
					// (4) else must calculate intercept, if intercept below bottom of ray then does not cross else it does

					// Northeast/Southeast crossings
					if( (zSegmentCrosses0Lon &&
						((iLon_E >= lon_point && iLon_E >= lon_previous) ||
						 (iLon_E <= lon_point && iLon_E <= lon_previous)) )
						||
						((iLon_E >= lon_point && iLon_E <= lon_previous) ||
						 (iLon_E <= lon_point && iLon_E >= lon_previous))      ){ // NE/SE x's cross (1)
						if( lat_point >= iLat_N && lat_previous >= iLat_N ){
							iCrossCount_NE++; // (2)
						} else if( lat_point <= iLat_S && lat_previous <= iLat_S ){
							iCrossCount_SE++; // (2)
						} else if( lat_point >= iLat_N || lat_previous >= iLat_N ) { // must calculate intercept (4)
							int lon_point_t = zSegmentCrosses0Lon && lon_point > lon_previous ? lon_point - 360000000 : lon_point; // transform longitudes
							int lon_previous_t = zSegmentCrosses0Lon && lon_previous > lon_point ? lon_previous - 360000000 : lon_previous;
							float fSegmentIntercept = (iLon_E - lon_point_t) * ((float)(lat_point - lat_previous)/(float)(lon_point_t - lon_previous_t)) + lat_point;
							if( fSegmentIntercept >= (float)iLat_N ){
								iCrossCount_NE++;
							} else if( fSegmentIntercept <= (float)iLat_S ){
								iCrossCount_SE++;
							}
						}

					// Northwest/Southwest crossings
					} else
					if( (zSegmentCrosses0Lon &&
						((iLon_W >= lon_point && iLon_W >= lon_previous) ||
						 (iLon_W <= lon_point && iLon_W <= lon_previous)) )
						||
						((iLon_W >= lon_point && iLon_W <= lon_previous) ||
						 (iLon_W <= lon_point && iLon_W >= lon_previous))      ){ // NW/SW x's cross (1)
						if( lat_point >= iLat_N && lat_previous >= iLat_N ){
							iCrossCount_NW++; // (2)
						} else if( lat_point <= iLat_S && lat_previous <= iLat_S ){
							iCrossCount_SW++; // (2)
						} else if( lat_point >= iLat_N || lat_previous >= iLat_N ) { // must calculate intercept (4)
							int lon_point_t = zSegmentCrosses0Lon && lon_point > lon_previous ? lon_point - 360000000 : lon_previous; // transform longitudes
							int lon_previous_t = zSegmentCrosses0Lon && lon_previous > lon_point ? lon_previous - 360000000 : lon_previous;
							float fSegmentIntercept = (iLon_E - lon_point_t) * ((float)(lat_point - lat_previous)/(float)(lon_point_t - lon_previous_t)) + lat_point;
							if( fSegmentIntercept >= (float)iLat_N ){
								iCrossCount_NW++;
							} else if( fSegmentIntercept <= (float)iLat_S ){
								iCrossCount_SW++;
							}
						}
					}

					// record previous point before continuing
					lon_previous = lon_point;
					lat_previous = lat_point;

				} // end loop points in this polygon

				// if there are no intersections then add entire polygon
				// otherwise determine the number of sub-polygons,
				// the number of intersections in each polygon,
			    // and assign each intersection to a polygon
				// set the point capacity of each sub-polygon
				int iTotalPointsAdded = 0;
				if( ctIntersections == 0 ){
					xRegionPolygons++;
					if( eSTATE == STATE_CountingPoints ){
						region_polygons.setPointCapacity(xRegionPolygons, ctPolygonPoints);
//System.out.println("setting point capacity for #" + xRegionPolygons + " to " + ctPolygonPoints + " - complete");
					}
					else if( eSTATE == STATE_Filling ){
						segmentCurrent1_X = region_polygons.getPoints1_X( xRegionPolygons );
						segmentCurrent1_Y = region_polygons.getPoints1_Y( xRegionPolygons );
	    				for( int xPolygonPoint = 1; xPolygonPoint <= ctPolygonPoints; xPolygonPoint++ ){
							segmentCurrent1_X[xPolygonPoint] = polygon1_LON[xPolygonPoint];
							segmentCurrent1_Y[xPolygonPoint] = polygon1_LAT[xPolygonPoint];
						}
					}
					iTotalPointsAdded = ctPolygonPoints;
				} else {
//if( eSTATE == 1 && (xPolygon == 1) ){
//System.out.println("before sort intersection side: " + Utility.dumpArray(aiIntersectionSide, 0, ctIntersections));
//System.out.println("before sort intersection lon-lat: " + Utility.dumpArray(aiIntersectionLonLat, 0, ctIntersections));
//System.out.println("before sort intersection index in: " + Utility.dumpArray(aiIntersectionIndex_in, 0, ctIntersections));
//System.out.println("before sort intersection index out: " + Utility.dumpArray(aiIntersectionIndex_out, 0, ctIntersections));
//}

					// insert sort the intersections
					for( int xIntersection = 2; xIntersection <= ctIntersections; xIntersection++ ){
						int insert_Side = aiIntersectionSide[xIntersection];
						int insert_LonLat = aiIntersectionLonLat[xIntersection];
						int insert_Index_in = aiIntersectionIndex_in[xIntersection];
						int insert_Index_out = aiIntersectionIndex_out[xIntersection];
						int xInsertion = xIntersection-1;
						for( ; xInsertion > 0; xInsertion-- ){
							boolean zLessThan =( (insert_Side < aiIntersectionSide[xInsertion]) ||
							    ((insert_Side == aiIntersectionSide[xInsertion]) &&
								 zIntersectionLessThan( insert_LonLat, aiIntersectionLonLat[xInsertion], insert_Side, iLat_N, iLon_E, iLat_S, iLon_W ))
							);
//String clause_code = "";
//if( xPolygon == 2 ) System.out.println("comparing side " + insert_Side + ":" + insert_LonLat + " to " + aiIntersectionSide[xInsertion] + ":" + aiIntersectionLonLat[xInsertion] + " lt: " + zLessThan + " " + clause_code);
//							if( (insert_Side < aiIntersectionSide[xInsertion]) ||
//							    ((insert_Side == aiIntersectionSide[xInsertion]) &&
//								 zIntersectionLessThan( insert_LonLat, aiIntersectionLonLat[xInsertion], insert_Side, iLat_N, iLon_E, iLat_S, iLon_W ))
//							){
						    if( zLessThan ){
								aiIntersectionSide[xInsertion+1] = aiIntersectionSide[xInsertion];
								aiIntersectionLonLat[xInsertion+1] = aiIntersectionLonLat[xInsertion];
								aiIntersectionIndex_in[xInsertion+1] = aiIntersectionIndex_in[xInsertion];
								aiIntersectionIndex_out[xInsertion+1] = aiIntersectionIndex_out[xInsertion];
							} else
								break;
						}
						aiIntersectionSide[xInsertion+1] = insert_Side;
						aiIntersectionLonLat[xInsertion+1] = insert_LonLat;
						aiIntersectionIndex_in[xInsertion+1] = insert_Index_in;
						aiIntersectionIndex_out[xInsertion+1] = insert_Index_out;

						// determine if any of the intersections are twins
						for( int xIntersection1 = 1; xIntersection1 <= ctIntersections; xIntersection1++ ){
							for( int xIntersection2 = xIntersection1+1; xIntersection2 <= ctIntersections; xIntersection2++ ){
								if( aiIntersectionIndex_in[xIntersection1] == aiIntersectionIndex_out[xIntersection2] && aiIntersectionIndex_out[xIntersection1] == aiIntersectionIndex_in[xIntersection2]){
									aiIntersectionIndex_twin[xIntersection1] = xIntersection2;
									aiIntersectionIndex_twin[xIntersection2] = xIntersection1;
									xIntersection1 = xIntersection2;
								}
							}
						}

					}
//if( eSTATE == 1 && (xPolygon == 1) ){
//System.out.println("#intersections: " + ctIntersections);
//System.out.println("intersection side: " + Utility.dumpArray(aiIntersectionSide, 0, ctIntersections));
//System.out.println("intersection lon-lat: " + Utility.dumpArray(aiIntersectionLonLat, 0, ctIntersections));
//System.out.println("intersection index in: " + Utility.dumpArray(aiIntersectionIndex_in, 0, ctIntersections));
//System.out.println("intersection index out: " + Utility.dumpArray(aiIntersectionIndex_out, 0, ctIntersections));
//System.out.println("intersection index twin: " + Utility.dumpArray(aiIntersectionIndex_twin, 0, ctIntersections));
//System.out.println("cross count NW: " + iCrossCount_NW);
//System.out.println("cross count NE: " + iCrossCount_NE);
//System.out.println("cross count SE: " + iCrossCount_SE);
//System.out.println("cross count SW: " + iCrossCount_SW);
//}

					boolean[] azIntersectionUsed = new boolean[ctIntersections + 1]; // as intersections are added as new points they are ticked off here as having been used
					int ctIntersectionsLeft = ctIntersections;
					boolean zNorthwestCornerIncluded = (iCrossCount_NW % 2 == 1); // is the NW corner in the polygon?
					boolean zDirectionIn = zNorthwestCornerIncluded;
					while( ctIntersectionsLeft > 0 ){
						xRegionPolygons++;
						if( eSTATE == STATE_Filling ){
						    segmentCurrent1_X = region_polygons.getPoints1_X( xRegionPolygons );
						    segmentCurrent1_Y = region_polygons.getPoints1_Y( xRegionPolygons );
						}
						int xIntersection = 1;
						int xStartingPoint = 0;
						int xPolygonPoint = 0;
						int ctRegionPolygonPoints = 0; // the total number of points in the current sub-polygon
						boolean zGoingForwards = true;
						for( xIntersection = 1; xIntersection <= ctIntersections; xIntersection++ ){
							if( !azIntersectionUsed[xIntersection] ){ // found the starting point
								ctRegionPolygonPoints++;
								xStartingPoint = aiIntersectionIndex_in[xIntersection]; // this is the index of the point the path starts
								xPolygonPoint = xStartingPoint;
								zGoingForwards = xPolygonPoint > aiIntersectionIndex_out[xIntersection];
								azIntersectionUsed[xIntersection] = true;
								ctIntersectionsLeft--;
								if( eSTATE == STATE_Filling ) vAddIntersection(segmentCurrent1_X, segmentCurrent1_Y, ctRegionPolygonPoints, aiIntersectionSide[xIntersection], aiIntersectionLonLat[xIntersection], iLat_N, iLon_E, iLat_S, iLon_W);
								break;
							}
						}
						if( xIntersection > ctIntersections ){ // did not find starting point
							ApplicationController.vShowError("internal error, no starting point in coastline");
							break;
						}
						int iCurrentSide = aiIntersectionSide[xIntersection]; // xIntersection is the intersection at the starting point
						while( true ){
							if( zDirectionIn ){
//								if( !zGoingForwards ) xStartingPoint = (xStartingPoint == 1) ? ctPolygonPoints : xStartingPoint - 1;
								int increment = zGoingForwards ? 1 : -1;
								while( zDirectionIn ){
									xIntersection = 0;
									while( true ){ // see if this point is an unused intersection
										xIntersection++;
										if( xIntersection > ctIntersections ){ // not an intersection, add point and go on to next point
		    								ctRegionPolygonPoints++;
//System.out.println("adding internal point " + ctRegionPolygonPoints + " " + polygon1_LON[xPolygonPoint] + " " + polygon1_LAT[xPolygonPoint]);
											if( eSTATE == STATE_Filling ){
												segmentCurrent1_X[ctRegionPolygonPoints] = polygon1_LON[xPolygonPoint];
												segmentCurrent1_Y[ctRegionPolygonPoints] = polygon1_LAT[xPolygonPoint];
											}
											xPolygonPoint += increment;
											if( zGoingForwards ){ if( xPolygonPoint > ctPolygonPoints ) xPolygonPoint = 1; } else if( xPolygonPoint < 1 ) xPolygonPoint = ctPolygonPoints;
											if( xPolygonPoint == xStartingPoint ) break; // done
											xIntersection = 0; // reset intersection index
										} else {

											// make sure that this is not the other side of a double intersection
											if( xPolygonPoint == aiIntersectionIndex_out[xIntersection] && !azIntersectionUsed[xIntersection] && xPolygonPoint != (aiIntersectionIndex_in[aiIntersectionIndex_twin[xIntersection]]) ){
												azIntersectionUsed[xIntersection] = true;
												ctIntersectionsLeft--;
												ctRegionPolygonPoints++;
												if( eSTATE == STATE_Filling ){ // add the intersection
													vAddIntersection(segmentCurrent1_X, segmentCurrent1_Y, ctRegionPolygonPoints, aiIntersectionSide[xIntersection], aiIntersectionLonLat[xIntersection], iLat_N, iLon_E, iLat_S, iLon_W);
												}
												iCurrentSide = aiIntersectionSide[xIntersection];
												zDirectionIn = false; // reverse direction, now we are dealing with segment outside of region
												break;
											}
										}
									}
									if( xPolygonPoint == xStartingPoint ) break; // done
								}
							} else { // direction is out
								ctRegionPolygonPoints++;
								int iNextIntersection = xIntersection == ctIntersections ? 1 : xIntersection + 1;
								if( aiIntersectionSide[iNextIntersection] == iCurrentSide ){  // next intersection is next point
									xIntersection = iNextIntersection;
									azIntersectionUsed[xIntersection] = true;
									ctIntersectionsLeft--;
									if( eSTATE == STATE_Filling ){
										vAddIntersection(segmentCurrent1_X, segmentCurrent1_Y, ctRegionPolygonPoints, iCurrentSide, aiIntersectionLonLat[xIntersection], iLat_N, iLon_E, iLat_S, iLon_W);
									}
									xPolygonPoint = aiIntersectionIndex_in[xIntersection];
									zGoingForwards = xPolygonPoint > aiIntersectionIndex_out[xIntersection];
									zDirectionIn = true; // reverse direction
								} else { // intervening corner is next point
									if( eSTATE == STATE_Filling ){
										int iLatLon = 0;
										switch( iCurrentSide ){
											case SIDE_NORTH:
												iLatLon = iLon_E;
												break;
											case SIDE_EAST:
												iLatLon = iLat_S;
												break;
											case SIDE_SOUTH:
												iLatLon = iLon_W;
												break;
											case SIDE_WEST:
												iLatLon = iLat_N;
												break;
										}
										vAddIntersection(segmentCurrent1_X, segmentCurrent1_Y, ctRegionPolygonPoints, iCurrentSide, iLatLon, iLat_N, iLon_E, iLat_S, iLon_W);
									}
									xPolygonPoint = 0; // after a corner a new intersection must be found
									iCurrentSide++; // if we add the NW corner we are done and this will be == 5
								}
							}
							if( xPolygonPoint == xStartingPoint || iCurrentSide == 5 ) break;
						} // end loop sub-polygon
						if( eSTATE == STATE_CountingPoints ){
							region_polygons.setPointCapacity(xRegionPolygons, ctRegionPolygonPoints);
						}
						iTotalPointsAdded += ctRegionPolygonPoints;
						zDirectionIn = false; // the remainder of the sub-polygons always start out
					} // end loop region polygons
				}

				iPointsProcessed += iTotalPointsAdded;
				if( System.currentTimeMillis() - nStart > 1000 ){
		    		ApplicationController.vShowStatus_NoCache("Resolving coastline " + (iPointsProcessed * 100 / (iTotalPoints * 3)) + "%"); // x3 because all the points in polygons are examined three times
					nStart = System.currentTimeMillis();
				}
			} // end loop polygons
			if( eSTATE == STATE_CountingSubPolygons ){ // allocate the memory for the sub-polygons
				ctRegionPolygons = xRegionPolygons;
				region_polygons.setCapacity(ctRegionPolygons);
				eSTATE = STATE_CountingPoints;
			} else if( eSTATE == STATE_CountingPoints ){
				eSTATE = STATE_Filling;
			} else {
				if( xRegionPolygons == ctRegionPolygons ) break; // done, otherwise fill last one
			}
		}
		if( ctRegionPolygons == 0 ){
			// no coastline segments found
		}
		return region_polygons;
	}

	// determines whether the first point of intersection is less than the second going in a clockwise
	// direction around the bounding box
	private static final boolean zIntersectionLessThan( int iLonLat1, int iLonLat2, int iSIDE, int iLat_N, int iLon_E, int iLat_S, int iLon_W ){
		switch( iSIDE ){
			default:
			case SIDE_NORTH:
				if( iLon_E > iLon_W ){ // normal case
					return iLonLat1 < iLonLat2;
				} else { // wrapped
					if( (iLonLat1 >= iLon_W && iLonLat2 >= iLon_W ) ||
					    (iLonLat1 <= iLon_E && iLonLat2 <= iLon_E ) ) return iLonLat1 < iLonLat2;
					else return iLonLat1 >= iLon_W; // if #1 is in left of split return true
				}
			case SIDE_EAST:
				if( iLat_N > iLat_S ){ // normal case
					return iLonLat1 > iLonLat2; // #1 is less than #2 if it is higher on the east side
				} else { // wrapped
					if( (iLonLat1 >= iLat_S && iLonLat2 >= iLat_S ) ||
					    (iLonLat1 <= iLat_N && iLonLat2 <= iLat_N ) ) return iLonLat1 > iLonLat2;
					else return iLonLat1 <= iLat_N; // if #1 is in the south half of East is it less than
				}
			case SIDE_SOUTH:
					if( (iLonLat1 >= iLon_W && iLonLat2 >= iLon_W ) ||
					    (iLonLat1 >= iLon_E && iLonLat2 >= iLon_E ) ) return iLonLat1 > iLonLat2;
					else return iLonLat1 <= iLon_E; // if #1 is in right of split return true
			case SIDE_WEST:
				if( iLat_N > iLat_S ){ // normal case
					return iLonLat1 < iLonLat2; // #1 is less than #2 if it is higher on the east side
				} else { // wrapped
					if( (iLonLat1 >= iLat_S && iLonLat2 >= iLat_S ) ||
					    (iLonLat1 <= iLat_N && iLonLat2 <= iLat_N ) ) return iLonLat1 < iLonLat2;
					else return iLonLat1 >= iLat_S; // if #1 is in the south half of West is it less than
				}
		}
	}

	private static final void vAddIntersection( int[] aiLon, int[] aiLat, int xAddLocation, int iIntersectionSide, int iIntersectionLonLat, int iLat_N, int iLon_E, int iLat_S, int iLon_W ){
		switch( iIntersectionSide ){
			case SIDE_NORTH:
				aiLon[xAddLocation] = iIntersectionLonLat;
				aiLat[xAddLocation] = iLat_N;
				break;
			case SIDE_EAST:
				aiLon[xAddLocation] = iLon_E;
				aiLat[xAddLocation] = iIntersectionLonLat;
				break;
			case SIDE_SOUTH:
				aiLon[xAddLocation] = iIntersectionLonLat;
				aiLat[xAddLocation] = iLat_S;
				break;
			case SIDE_WEST:
				aiLon[xAddLocation] = iLon_W;
				aiLat[xAddLocation] = iIntersectionLonLat;
				break;
		}
	}

//	private final int iFindNextIntersection( int[] aiIntersectionSide, int[] aiIntersectionLonLat

	private static final boolean zPointInRegion( int iPointLongitude, int iPointLatitude, int iLonW, int iLonE, int iLatS, int iLatN ){
		return ( iLonE > iLonW ?
				iPointLongitude >= iLonW &&
				iPointLongitude <= iLonE
			    :
				iPointLongitude >= iLonW ||
				iPointLongitude <= iLonE
				)
			&&
			   ( iLatN > iLatS  ?
				iPointLatitude >= iLatS &&
				iPointLatitude <= iLatN
				:
				iPointLatitude >= iLatS ||
				iPointLatitude <= iLatN
				);
	}

	// if the regions are mutually exclusive then true is returned
	// if they share any common points then false is returned
	// coordinates must use same frame of reference
	// TODO latitudes may not wrap this way; examine antartica bounding carefully
	private static final boolean zRegionsExclusive( int iLonW_1, int iLonE_1, int iLatS_1, int iLatN_1, int iLonW_2, int iLonE_2, int iLatS_2, int iLatN_2){
		if( iLonW_1 > iLonE_1 ){ // wrapped longitude 1
			if( iLatS_1 > iLatN_1 ){ // wrapped latitude 1
				if( iLonW_2 > iLonE_2 ){ // wrapped longitude 2
					if( iLatS_2 > iLatN_2 ){ // wrapped latitude 2
						return false; // must both include corners
					} else {
						return iLatS_1 > iLatN_2 && iLatN_1 < iLatS_2;
					}
				} else {
					if( iLatS_2 > iLatN_2 ){ // wrapped latitude 2
						return  iLonW_1 > iLonE_2 && iLonE_1 < iLonW_2;
					} else {
						return  ( iLonW_1 > iLonE_2 && iLonE_1 < iLonW_2) ||
	    					    ( iLatS_1 > iLatN_2 && iLatN_1 < iLatS_2) ;
					}
				}
			} else {
				if( iLonW_2 > iLonE_2 ){ // wrapped longitude 2
					if( iLatS_2 > iLatN_2 ){ // wrapped latitude 2
						return iLatS_2 > iLatN_1 && iLatN_2 < iLatS_1;
					} else {
						return iLatS_1 > iLatN_2 || iLatN_1 < iLatS_2;
					}
				} else {
					if( iLatS_2 > iLatN_2 ){ // wrapped latitude 2
						return  ( iLonW_1 > iLonE_2 && iLonE_1 < iLonW_2) ||
	    					    ( iLatS_1 > iLatN_2 && iLatN_1 < iLatS_2) ;
					} else {
						return  ( iLonW_1 > iLonE_2 && iLonE_1 < iLonW_2) ||
	    					    ( iLatS_1 > iLatN_2 || iLatN_1 < iLatS_2) ;
					}
				}
			}
		} else {
			if( iLatS_1 > iLatN_1 ){ // wrapped latitude 1
				if( iLonW_2 > iLonE_2 ){ // wrapped longitude 2
					if( iLatS_2 > iLatN_2 ){ // wrapped latitude 2
						return  iLonW_2 > iLonE_1 && iLonE_2 < iLonW_1;
					} else {
						return  ( iLonW_2 > iLonE_1 && iLonE_2 < iLonW_1) ||
	    					    ( iLatS_2 > iLatN_1 && iLatN_2 < iLatS_1) ;
					}
				} else {
					if( iLatS_2 > iLatN_2 ){ // wrapped latitude 2
						return iLonW_1 > iLonE_2 || iLonE_1 < iLonW_2;
					} else {
						return  ( iLonW_2 > iLonE_1 || iLonE_2 < iLonW_1) ||
							    ( iLatS_2 > iLatN_1 && iLatN_2 < iLatS_1) ;
					}
				}
			} else {
				if( iLonW_2 > iLonE_2 ){ // wrapped longitude 2
					if( iLatS_2 > iLatN_2 ){ // wrapped latitude 2
						return  ( iLonW_2 > iLonE_1 && iLonE_2 < iLonW_1) ||
	    					    ( iLatS_2 > iLatN_1 && iLatN_2 < iLatS_1) ;
					} else {
						return  ( iLonW_2 > iLonE_1 && iLonE_2 < iLonW_1) ||
	    					    ( iLatS_2 > iLatN_1 || iLatN_2 < iLatS_1) ;
					}
				} else {
					if( iLatS_2 > iLatN_2 ){ // wrapped latitude 2
						return  ( iLonW_2 > iLonE_1 || iLonE_2 < iLonW_1) ||
	    					    ( iLatS_2 > iLatN_1 && iLatN_2 < iLatS_1) ;
					} else {
						return  ( iLonW_1 > iLonE_2 || iLonE_1 < iLonW_2) ||
	    					    ( iLatS_1 > iLatN_2 || iLatN_1 < iLatS_2) ;
					}
				}
			}
		}
	}

	public static void vTestRegions(){
		int[] a = { 100, 160, 50, 20 }; // W E N S
		int[] b = { 140, 200, 0 , -40 };
		int[] c = { 150, 180, 40, -10 };
		int[] d = { 260, 300, -60, 40 };
		int[] e = { 340, 120, -70, 45 };
		int[] f = { 340, 30, 40, 20 };
		int[] g = { 350, 145, -10, -30 };
		int[] h = { 350, 20, -60, 70 };
		int[] i = { 290, 350, 80, 30 };
		int[] j = { 220, 275, 25, -20 };
		int[] k = { 130, 140, -80, 48 };
		vTestCompare( "A", a, "B", b, true );
		vTestCompare( "A", a, "C", c, false );
		vTestCompare( "A", a, "E", e, false );
		vTestCompare( "A", a, "F", f, true );
		vTestCompare( "A", a, "G", g, true );
		vTestCompare( "A", a, "J", j, true );
		vTestCompare( "A", a, "H", h, true );
		vTestCompare( "A", a, "K", k, false );
		vTestCompare( "B", b, "C", c, false );
		vTestCompare( "B", b, "E", e, true );
		vTestCompare( "B", b, "G", g, false );
		vTestCompare( "B", b, "J", j, true );
		vTestCompare( "B", b, "K", k, true );
		vTestCompare( "C", c, "J", j, true );
		vTestCompare( "C", c, "K", k, true );
		vTestCompare( "D", d, "E", e, true );
		vTestCompare( "D", d, "F", f, true );
		vTestCompare( "D", d, "I", i, false );
		vTestCompare( "D", d, "J", j, true );
		vTestCompare( "E", e, "F", f, true );
		vTestCompare( "E", e, "I", i, false );
		vTestCompare( "E", e, "K", k, true );
		vTestCompare( "F", f, "J", j, true );
		vTestCompare( "F", f, "I", i, false );
		vTestCompare( "F", f, "H", h, true );
		vTestCompare( "G", g, "H", h, true );
		vTestCompare( "G", g, "J", j, true );
		vTestCompare( "H", h, "I", i, false );
	}

	private static void vTestCompare( String sRegionOne, int[] one, String sRegionTwo, int[] two, boolean zExpected ){
		boolean zValue1 = zRegionsExclusive( one[0], one[1], one[3], one[2], two[0], two[1], two[3], two[2] );
		boolean zValue2 = zRegionsExclusive( two[0], two[1], two[3], two[2], one[0], one[1], one[3], one[2] );
		System.out.println("compare " + sRegionOne + " to " + sRegionTwo + ": " + zValue1 + " " + zValue2 + " - " + zExpected);
	}

	static Polygons getAllPolygons( double resolution_km, StringBuffer sbError ){
		String sCoastline = ConfigurationManager.getInstance().getProperty_PATH_Coastline();
		File fileCoastline = null;
		if( sCoastline != null && sCoastline.length() != 0 ){
			try {
				fileCoastline = new File(sCoastline);
			} catch(Exception ex) {
				sbError.append("Unable to interpret coastline path (" + sCoastline + ") as file or directory: " + ex);
				return null;
			}
		}

//gshhs_f.c   Full             0.04 km     87M       < 0.05 km
//gshhs_h.c   High             0.2  km     20M       < 0.4 km
//gshhs_i.c   Intermediate     1.0  km      5M       <  2 km
//gshhs_l.c   Low              5.0  km      1M       < 10 km
//gshhs_c.c   Crude           25    km    167K

		boolean zLoad;
		DataInputStream dis;
		Polygons polygons;
// for now the built in crude will not be used
//		if( sCoastline == null || sCoastline.length() == 0 || (fileCoastline.isDirectory() && resolution_km >= 10) ){ // use built-in crude
//			if( mPolygonsCrude == null ){
//				Object[] eggPolygons = new Object[1];
//				if( zReadCoastlineData("built-in 25km", false, RELATIVE_PATH_Crude, eggPolygons, sbError ) ){
//					mPolygonsCrude = (Polygons)eggPolygons[0];
//				} else {
//					return null;
//				}
//			}
//			polygons = mPolygonsCrude;
//		} else {
			if( fileCoastline.isFile() ){ // use only this file
				dis = getCoastlineData_file(fileCoastline, sbError);
				Object[] eggPolygons = new Object[1];
				if( zReadCoastlineData("custom coastline file", false, fileCoastline.getAbsolutePath(), eggPolygons, sbError ) ){
					polygons = (Polygons)eggPolygons[0];
				} else {
					return null;
				}
			} else if( fileCoastline.isDirectory() ){
				final int eCOASTLINE_Intermediate = 3;
				final int eCOASTLINE_Low = 4;
				final int eCOASTLINE_Crude = 5;
				String sFile;
				int eFile = 0;
// full and high not supported in current version
//				if( resolution_km < 0.05d ){ // use full
//					sFile = "gshhs_f.b";
//					eFile = 1;
//				} else if( resolution_km < 0.4d ){ // use high
//					sFile = "gshhs_h.b";
//					eFile = 2;
//				} else
				if( resolution_km < 2d ){ // use intermediate
					sFile = "gshhs_i.b";
					eFile = eCOASTLINE_Intermediate;
				} else if( resolution_km < 10d ){  // use low
					sFile = "gshhs_l.b";
					eFile = eCOASTLINE_Low;
				} else { // use crude
					sFile = "gshhs_c.b";
					eFile = eCOASTLINE_Crude;
				}
				String sFilePath = Utility.sConnectPaths(fileCoastline.getAbsolutePath(), sFile);
				try {
					fileCoastline = new File(sFilePath);
				} catch(Exception ex) {
					sbError.append("Unable to access coastline file " + sFilePath);
					return null;
				}
				if( !fileCoastline.exists() ){
					sbError.append("No coastline file with required resolution (" + resolution_km + ")");
					return null;
			    }
				if( eFile == eCOASTLINE_Low && mPolygonsLow != null ){
					polygons = mPolygonsLow;
				} else if( eFile == eCOASTLINE_Intermediate && mPolygonsIntermediate != null ){
					polygons = mPolygonsIntermediate;
				} else if( eFile == eCOASTLINE_Crude && mPolygonsCrude != null ){
					polygons = mPolygonsCrude;
				} else {
					Object[] eggPolygons = new Object[1];
					if( zReadCoastlineData(sFile, false, sFilePath, eggPolygons, sbError ) ){
						switch( eFile ){
							default:
							case eCOASTLINE_Crude:
								mPolygonsCrude = (Polygons)eggPolygons[0];
								polygons = mPolygonsCrude;
								break;
							case eCOASTLINE_Low:
								mPolygonsLow = (Polygons)eggPolygons[0];
								polygons = mPolygonsLow;
								break;
							case eCOASTLINE_Intermediate:
								mPolygonsIntermediate = (Polygons)eggPolygons[0];
								polygons = mPolygonsIntermediate;
								break;
						}
					} else {
						return null;
					}
				}
			} else {
				sbError.append("unknown mode for coastline path (" + fileCoastline + "); not file or directory");
				return null;
			}
//		}

//			if( mPolygonsLow == null ){
//			}
//			polygons = mPolygonsLow;
//		} else { // use crude
//
//			sFilePath = "I:\\Geo\\GSHHS\\gshhs_c.b";
//			if( mPolygonsCrude == null ){
//				Object[] eggPolygons = new Object[1];
//				if( zReadCoastlineFile( sFilePath, eggPolygons, sbError ) ){
//					mPolygonsCrude = (Polygons)eggPolygons[0];
//				} else {
//					return null;
//				}
//			}
//			polygons = mPolygonsCrude;
//		}
		return polygons;
	}

	static DataInputStream getCoastlineData_resource( String sPath, StringBuffer sbError ){
		java.io.InputStream inputstreamResource = null;
		try {
			inputstreamResource = ApplicationController.getInstance().getClass().getResourceAsStream(sPath);
			DataInputStream dis = new java.io.DataInputStream( inputstreamResource );
			return dis;
		} catch(Exception ex) {
			sbError.append("failed to open resource (" + sPath + "): " + ex);
			return null;
		}
	}

	static DataInputStream getCoastlineData_file( File fileCoastLine, StringBuffer sbError ){
		try {
			if( fileCoastLine == null ){
				sbError.append("no file supplied");
				return null;
			}
			java.io.FileInputStream fis = new java.io.FileInputStream( fileCoastLine );
			DataInputStream dis = new java.io.DataInputStream( fis );
			if( dis == null ){
				sbError.append("unable to create data input stream");
				return null;
			}
			return dis;
		} catch(Exception ex) {
			sbError.append("error opening coastline file [" + fileCoastLine + "]: " + ex);
			return null;
		}
	}

	static boolean zReadCoastlineData( String sFilePathInfo, boolean zResource, String sPath, Object[] eggPolygons, StringBuffer sbError ){
		ApplicationController.vShowStatus_NoCache("Loading coastline file " + sFilePathInfo);
		long nProcedureStart = System.currentTimeMillis();
		long nStart = nProcedureStart;
		int iDatasetSourceSize = 0;

		java.io.DataInputStream dis;
		if( zResource ){
			dis = getCoastlineData_resource( sPath, sbError );
		} else {
			File file = new File( sPath );
			dis = getCoastlineData_file( file, sbError );
		}
		if( dis == null ){
			sbError.insert( 0, "Failed to open coastline " + (zResource ? "resource" : "file") + " " + sPath + ": ");
			return false;
		}

		// Determine the number of polygons
		int ctPolygons = 0;
		try {
			while( true ){
				if( dis.available() < 1 ) break; // done?

				// read header
				int id            = dis.readInt();   /* Unique polygon id number, starting at 0 */
				int ctPoints      = dis.readInt();   /* Number of points in this polygon */
				int eLevel        = dis.readInt();   /* 1 land, 2 lake, 3 island_in_lake, 4 pond_in_island_in_lake */
				int iExtent_west  = dis.readInt();   /* min/max extent in micro-degrees : west */
				int iExtent_east  = dis.readInt();   /* min/max extent in micro-degrees : east */
				int iExtent_south = dis.readInt();   /* min/max extent in micro-degrees : south */
				int iExtent_north = dis.readInt();   /* min/max extent in micro-degrees : north */
				int iArea         = dis.readInt();   /* Area of polygon in 1/10 km^2 */
				short eGreenwich  = dis.readShort(); /* Greenwich is 1 if Greenwich is crossed */
				short eSource     = dis.readShort(); /* 0 = CIA WDBII, 1 = WVS */

				if( ctPoints >= MINIMUM_POLYGON_POINT_COUNT && iArea >= MINIMUM_POLYGON_AREA ){
		    		ctPolygons++;
				}

				dis.skipBytes( 8 * ctPoints );

				if( System.currentTimeMillis() - nStart > 1000 ){
		    		ApplicationController.vShowStatus_NoCache("Loading coastline file, counting polygon " + ctPolygons);
					nStart = System.currentTimeMillis();
				}

			}

		} catch(Exception ex) {
			opendap.clients.odc.Utility.vUnexpectedError(ex, sbError);
			sbError.insert(0, "error reading polygon " + ctPolygons + " from " + sFilePathInfo + " " + sPath + ": ");
			return false;
		} finally {
			try { dis.close(); } catch(Exception ex) {}
		}

		if( ctPolygons < 1 ){
			sbError.append("no usable data in coastline file");
			return false;
		}

		if( zResource ){
			dis = getCoastlineData_resource( sPath, sbError );
		} else {
			File file = new File( sPath );
			dis = getCoastlineData_file( file, sbError );
		}
		if( dis == null ){
			sbError.insert( 0, "Failed to re-open coastline " + (zResource ? "resource" : "file") + " " + sPath + ": ");
			return false;
		}

		// establish data structure
		Polygons polygons = new Polygons();
		polygons.setCapacity(ctPolygons);

		int xPolygon = 0;
		int ctPoints = 0;
		int xPoint = 0;
		try {
			while( true ){

				if( dis.available() < 1 ) break; // done?

				// read header
				int id            = dis.readInt();   /* Unique polygon id number, starting at 0 */
				    ctPoints      = dis.readInt();   /* Number of points in this polygon */
				int eLevel        = dis.readInt();   /* 1 land, 2 lake, 3 island_in_lake, 4 pond_in_island_in_lake */
				int iExtent_west  = dis.readInt();   /* min/max extent in micro-degrees : west */
				int iExtent_east  = dis.readInt();   /* min/max extent in micro-degrees : east */
				int iExtent_south = dis.readInt();   /* min/max extent in micro-degrees : south */
				int iExtent_north = dis.readInt();   /* min/max extent in micro-degrees : north */
				int iArea         = dis.readInt();   /* Area of polygon in 1/10 km^2 */
				short eGreenwich  = dis.readShort(); /* Greenwich is 1 if Greenwich is crossed */
				short eSource     = dis.readShort(); /* 0 = CIA WDBII, 1 = WVS */

//System.out.println("id: " + id);
//System.out.println("ctPoints: " + ctPoints);
//System.out.println("eLevel: " + eLevel);
//System.out.println("iExtent_west: " + iExtent_west);
//System.out.println("iExtent_east: " + iExtent_east);
//System.out.println("iExtent_south: " + iExtent_north);
//System.out.println("iArea: " + iArea);
//System.out.println("eGreenwich: " + eGreenwich);
//System.out.println("eSource: " + eSource);

				if( System.currentTimeMillis() - nStart > 1000 ){
		    		ApplicationController.vShowStatus_NoCache("Loading coastline file, reading polygon " + xPolygon + " of " + ctPolygons);
					nStart = System.currentTimeMillis();
				}

				if( ctPoints >= MINIMUM_POLYGON_POINT_COUNT && iArea >= MINIMUM_POLYGON_AREA ){
					xPolygon++;
					polygons.aeLevel[xPolygon] = eLevel;
					polygons.aiExtent_west[xPolygon] = iExtent_west;
					polygons.aiExtent_east[xPolygon] = iExtent_east;
					polygons.aiExtent_south[xPolygon] = iExtent_south;
					polygons.aiExtent_north[xPolygon] = iExtent_north;
					polygons.aiArea[xPolygon] = iArea;
					polygons.setPointCapacity(xPolygon, ctPoints);

					// read points
					int[] aiPoints_X = polygons.getPoints1_X(xPolygon);
					int[] aiPoints_Y = polygons.getPoints1_Y(xPolygon);
					for( xPoint = 1; xPoint <= ctPoints; xPoint++ ){
						int iLongitude  = dis.readInt(); /* longitude of a point in micro-degrees */
						int iLatitude   = dis.readInt(); /* latitude of a point in micro-degrees */
						aiPoints_X[xPoint] = iLongitude;
						aiPoints_Y[xPoint] = iLatitude;
					}
				} else {
					dis.skipBytes( 8 * ctPoints );
				}
			}

		} catch(Exception ex) {
			opendap.clients.odc.Utility.vUnexpectedError(ex, sbError);
			sbError.insert(0, "error reading polygon " + ctPolygons + " point " + xPoint + " of " + ctPoints + ": ");
			return false;
		} finally {
			try { dis.close(); } catch(Exception ex) {}
		}
		eggPolygons[0] = polygons;
   		ApplicationController.vShowStatus("Loaded coastline file " + (System.currentTimeMillis() - nProcedureStart)/1000 + "s " + sPath);
		return true;
	}

	public final static Color COLOR_CoastlineDefault = new Color(0, 0, 0, 0xB0);
	static public void vDrawCoastline( Graphics2D g2, int xPlotArea_LowerLeft, int yPlotArea_LowerLeft, int pxPlotWidth, int pxPlotHeight, GeoReference theGeoReference ){
		PlotLinestyle style = null;
		Color colorSea = null;
		Color colorLand = null;
		Color colorLake = null;
		style = new PlotLinestyle();
		g2.setColor(COLOR_CoastlineDefault);
		g2.setStroke(style.getStroke());
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int[] aiLongitude_micro = theGeoReference.getLongitudeMapping1_Micro(pxPlotWidth);
		int[] aiLatitude_micro = theGeoReference.getLatitudeMapping1_Micro(pxPlotHeight);
		if( aiLongitude_micro == null || aiLongitude_micro.length < 10 ){
			ApplicationController.vShowWarning("unable to draw coastline, longitude mapping too small");
			return;
		}
		if( aiLatitude_micro == null || aiLatitude_micro.length < 10 ){
			ApplicationController.vShowWarning("unable to draw coastline, latitude mapping too small");
			return;
		}
//System.out.println("longitudes: " + Utility.dumpArray(aiLongitude_micro, 0, 0));
//System.out.println("latitudes: " + Utility.dumpArray(aiLatitude_micro, 0, 0));
		// todo for projections the mapping will be 2D
//int ctOut = 0;

		int iLatitudeNorth = theGeoReference.getBound_North();
		int iLongitudeEast = theGeoReference.getBound_East();
		int iLatitudeSouth = theGeoReference.getBound_South();
		int iLongitudeWest = theGeoReference.getBound_West();

		StringBuffer sbError = new StringBuffer(80);
		Polygons polygonsCoastline = Coastline.getPolygonsInRegion( pxPlotWidth, iLongitudeWest, iLongitudeEast, pxPlotHeight, iLatitudeSouth, iLatitudeNorth, sbError);
		if( polygonsCoastline == null ){
			ApplicationController.vShowError_NoModal("coastline unavailable: " + sbError);
			return;
		}

		int ctPolygons = polygonsCoastline.getPolygonCount();
//System.out.println("draw polygon count: " + ctPolygons);
		if( ctPolygons == 0 ){
			return; // no visible coast lines on plot
		}
		try {

			int iRange_longitude = (iLongitudeEast > iLongitudeWest) ? iLongitudeEast - iLongitudeWest : 360000000 - iLongitudeWest + iLongitudeEast;
			int iRange_latitude = (iLatitudeNorth > iLatitudeSouth) ? iLatitudeNorth - iLatitudeSouth : 180000000 - iLatitudeSouth + iLatitudeNorth;
			int iMicrodegreesPerPixel_X = iRange_longitude / pxPlotWidth;
			int iMicrodegreesPerPixel_Y = iRange_latitude / pxPlotHeight;
//System.out.println("microdegrees per pixel x: " + iMicrodegreesPerPixel_X);
//System.out.println("microdegrees per pixel y: " + iMicrodegreesPerPixel_Y);

			// create the point buffer
			int ctPointsInLargestPolygon = polygonsCoastline.getLongestSegmentLength();
			if( !Utility.zMemoryCheck(ctPointsInLargestPolygon * 2, 4, sbError) ){
				ApplicationController.vShowWarning("Unable to draw coastline, insufficient memory");
				return;
			}
			int[] points_X = new int[ctPointsInLargestPolygon];
			int[] points_Y = new int[ctPointsInLargestPolygon];

			int xPlotArea_UpperLeft = xPlotArea_LowerLeft;
		    int yPlotArea_UpperLeft = yPlotArea_LowerLeft - pxPlotHeight;
			int rim_North = yPlotArea_UpperLeft;
			int rim_East  = xPlotArea_UpperLeft + pxPlotWidth - 1;
			int rim_South = yPlotArea_UpperLeft + pxPlotHeight - 1;
			int rim_West  = xPlotArea_UpperLeft;
			g2.setClip(xPlotArea_UpperLeft, yPlotArea_UpperLeft, pxPlotWidth, pxPlotHeight);

			for( int xPolygon = 1; xPolygon <= ctPolygons; xPolygon++ ){
//System.out.println("longitudes poly " + xPolygon + ": " + Utility.dumpArray(polygonsCoastline.getPoints1_X(xPolygon), 0, 0));
//System.out.println("latitudes poly " + xPolygon + ": " + Utility.dumpArray(polygonsCoastline.getPoints1_Y(xPolygon), 0, 0));
				int[] aiPolygonPoints1_X = polygonsCoastline.getPoints1_X(xPolygon);
				int[] aiPolygonPoints1_Y = polygonsCoastline.getPoints1_Y(xPolygon);
				int ctPoints = aiPolygonPoints1_X.length - 1;
//System.out.println("polygon: " + xPolygon + " with " + ctPoints + " points");

				int xLongitude = 1; // adjust to the nearest longitude to the coastline longitude
				int xLatitude = 1; // adjust to the nearest longitude to the coastline longitude
				int xExtent_Longitude = 0;
				int xExtent_Latitude = 0;
				boolean zReverseLongitude = theGeoReference.getReversed_Longitude();
				boolean zReverseLatitude = theGeoReference.getReversed_Latitude();
				long nStart = System.currentTimeMillis();
				for( int xPoint = 1; xPoint <= ctPoints; xPoint++ ){

					xLongitude = aiPolygonPoints1_X[xPoint];
					xLatitude = aiPolygonPoints1_Y[xPoint];
					int lon_width = xLongitude >= iLongitudeWest ? xLongitude - iLongitudeWest : xLongitude + 360000000 - iLongitudeWest;
					int lat_height = xLatitude >= iLatitudeSouth ? xLatitude - iLatitudeSouth : xLatitude + 180000000 - iLatitudeSouth;
					if( zReverseLongitude ){
						points_X[xPoint - 1] = xPlotArea_UpperLeft + pxPlotWidth - (lon_width / iMicrodegreesPerPixel_X);
					} else {
						points_X[xPoint - 1] = xPlotArea_UpperLeft + lon_width / iMicrodegreesPerPixel_X;
					}
					if( zReverseLatitude ){
					    points_Y[xPoint - 1] = yPlotArea_UpperLeft + lat_height / iMicrodegreesPerPixel_Y;
					} else {
					    points_Y[xPoint - 1] = yPlotArea_UpperLeft + pxPlotHeight - lat_height / iMicrodegreesPerPixel_Y;
					}

					// this clause puts any edges of the polygon that abut the edges of the
					// plot area just outside of the plot area.
					if( xPoint > 1 ){
						if( points_X[xPoint-1] == rim_West && points_X[xPoint - 2] == rim_West ){
							points_X[xPoint-1]--;
							points_X[xPoint-2]--;
						} else if( points_X[xPoint-1] == rim_East && points_X[xPoint - 2] == rim_East ){
							points_X[xPoint-1]++;
							points_X[xPoint-2]++;
						}
						if( points_Y[xPoint-1] == rim_South && points_Y[xPoint - 2] == rim_South ){
							points_Y[xPoint-1]++;
							points_Y[xPoint-2]++;
						} else if( points_Y[xPoint-1] == rim_North && points_Y[xPoint - 2] == rim_North ){
							points_Y[xPoint-1]--;
							points_Y[xPoint-2]--;
						}
					}
					if( xPoint == ctPoints ){
						if( points_X[0] == rim_West && points_X[ctPoints - 1] == rim_West ){
							points_X[0]--;
							points_X[ctPoints-1]--;
						} else if( points_X[0] == rim_East && points_X[xPoint - 1] == rim_East ){
							points_X[0]++;
							points_X[ctPoints-1]++;
						}
						if( points_Y[0] == rim_South && points_Y[ctPoints - 1] == rim_South ){
							points_Y[0]++;
							points_Y[ctPoints-1]++;
						} else if( points_Y[0] == rim_North && points_Y[ctPoints - 1] == rim_North ){
							points_Y[0]--;
							points_Y[ctPoints-1]--;
						}
					}
//if( xPoint > 1 && ((points_X[xPoint-1] - points_X[xPoint-2] > pxPlotHeight/2) || points_X[xPoint-2] - points_X[xPoint-1] > pxPlotHeight/2) ){
//	System.out.println("bad draw segment in polygon " + xPolygon + " : point #" + (xPoint - 1) + " (" + aiPolygonPoints1_X[xPoint-1] + ", " + aiPolygonPoints1_Y[xPoint-1] + ") - #" + xPoint + " (" + xLongitude + ", " + xLatitude + ")");
//} else
//if( xPoint > 1 && ((points_Y[xPoint-1] - points_Y[xPoint-2] > pxPlotHeight/2) || points_Y[xPoint-2] - points_Y[xPoint-1] > pxPlotHeight/2) ){
//	System.out.println("bad draw segment in polygon " + xPolygon + " : point #" + (xPoint - 1) + " (" + aiPolygonPoints1_X[xPoint-1] + ", " + aiPolygonPoints1_Y[xPoint-1] + ") - #" + xPoint + " (" + xLongitude + ", " + xLatitude + ")");
//}

//					if( System.currentTimeMillis() - nStart > 10000 ){
//						ApplicationController.vShowStatus_NoCache("Resolving coastline longitude " + xPoint + " of " + ctPoints);
//						nStart = System.currentTimeMillis();
//					}

// debugging
//					if( aiLatitude_micro[xLatitude] == -88975218 ){
//						xLatitude = xLatitude;// step debug from here
//					}
//if( ctOut++ < 100 ){
//System.out.println("coastline " + xSegment + " point " + xPoint + " (" + points_X[xPoint] + ", " + points_Y[xPoint] + ") = " +
//"segment " + xSegment + " point " + xPoint + " seg-lon,lat(" + aiSegmentPoints1_X[xPoint + 1] + ", " + aiSegmentPoints1_Y[xPoint + 1] + ") resolved to lon/lat (" + aiLongitude_micro[xLongitude] + ", " + aiLatitude_micro[xLatitude] + ")");
//}
				}
//System.out.println("points x " + xPolygon + " ctpoints: " + ctPoints + ": " + Utility.dumpArray(points_X, 0, 0));
//System.out.println("points y " + xPolygon + ": " + Utility.dumpArray(points_Y, 0, 0));
				g2.drawPolygon(points_X, points_Y, ctPoints);

			}
		} catch(Exception ex) {
			StringBuffer sbUError = new StringBuffer("coastline unexpected error: ");
			Utility.vUnexpectedError(ex, sbUError);
			ApplicationController.vShowError_NoModal(sbUError.toString());
			return;
		} finally {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g2.setClip(null);
		}
	}

	static Polygons getAllPolygons_Test(){
		int[] aiPoints1_X = new int[18];
		int[] aiPoints1_Y = new int[18];
		aiPoints1_X[1] = 270; aiPoints1_Y[1] = 24;
		aiPoints1_X[2] = 275; aiPoints1_Y[2] = 27;
		aiPoints1_X[3] = 278; aiPoints1_Y[3] = 25;
		aiPoints1_X[4] = 278; aiPoints1_Y[4] = 22;
		aiPoints1_X[5] = 285; aiPoints1_Y[5] = 11;
		aiPoints1_X[6] = 289; aiPoints1_Y[6] = 6;
		aiPoints1_X[7] = 300; aiPoints1_Y[7] = 4;
		aiPoints1_X[8] = 302; aiPoints1_Y[8] = 9;
		aiPoints1_X[9] = 297; aiPoints1_Y[9] = 10;
		aiPoints1_X[10] = 295; aiPoints1_Y[10] = 12;
		aiPoints1_X[11] = 304; aiPoints1_Y[11] = 14;
		aiPoints1_X[12] = 308; aiPoints1_Y[12] = 9;
		aiPoints1_X[13] = 301; aiPoints1_Y[13] = 1;
		aiPoints1_X[14] = 283; aiPoints1_Y[14] = 5;
		aiPoints1_X[15] = 280; aiPoints1_Y[15] = 11;
		aiPoints1_X[16] = 277; aiPoints1_Y[16] = 16;
		aiPoints1_X[17] = 272; aiPoints1_Y[17] = 19;
		for( int xPoint = 1; xPoint < aiPoints1_X.length; xPoint++ ){
			aiPoints1_X[xPoint] *= 1000000; // micro degrees
			aiPoints1_Y[xPoint] *= 1000000;
		}
		Polygons polygons = new Polygons();
		polygons.setCapacity(1);
		polygons.setPointCapacity(1, aiPoints1_X.length);
		polygons.setPoints1(1, aiPoints1_X, aiPoints1_Y);
		polygons.aiExtent_west[1] = 270000000;
		polygons.aiExtent_north[1] = 27000000;
		polygons.aiExtent_east[1] = 308000000;
		polygons.aiExtent_south[1] = 1000000;
		return polygons;
	}

}

class LineSegments {
	private int mctSegments = 0;
	private int mctTotalPoints = 0;
	private int[][] maiLineSegments_X = null;
	private int[][] maiLineSegments_Y = null;
	void setSegmentCapacity( int ctSegments ){
		if( mctSegments > 0 ){
			int[][] aiiBuffer_X = new int[ctSegments + 1][];
			int[][] aiiBuffer_Y = new int[ctSegments + 1][];
			for( int xSegment = 1; xSegment <= mctSegments; xSegment++ ){
				int len = maiLineSegments_X[xSegment].length;
				int[] aiBuffer_X = new int[len];
				int[] aiBuffer_Y = new int[len];
				System.arraycopy( maiLineSegments_X[xSegment], 0, aiBuffer_X, 0, len);
				System.arraycopy( maiLineSegments_Y[xSegment], 0, aiBuffer_Y, 0, len);
				aiiBuffer_X[xSegment] = aiBuffer_X;
				aiiBuffer_Y[xSegment] = aiBuffer_Y;
			}
			maiLineSegments_X = aiiBuffer_X;
			maiLineSegments_Y = aiiBuffer_Y;
		} else {
			maiLineSegments_X = new int[ctSegments + 1][];
			maiLineSegments_Y = new int[ctSegments + 1][];
		}
		mctSegments = ctSegments;
	}
	void setPointCapacity( int xSegment1, int ctPoints ){
		maiLineSegments_X[xSegment1] = new int[ctPoints + 1];
		maiLineSegments_Y[xSegment1] = new int[ctPoints + 1];
		mctTotalPoints += ctPoints;
	}
	int getSegmentCount(){
		return mctSegments;
	}
	int getTotalPointCount(){ return mctTotalPoints; }
	int getLongestSegmentLength(){
		int iMaxLength = 0;
		for( int xSegment = 1; xSegment <= mctSegments; xSegment++ ){
			int ctPoints = (maiLineSegments_X[xSegment].length - 1);
			if( ctPoints > iMaxLength ) iMaxLength = ctPoints;
		}
		return iMaxLength;
	}
	int[] getPoints1_X( int xSegment1 ){ return maiLineSegments_X[xSegment1]; }
	int[] getPoints1_Y( int xSegment1 ){ return maiLineSegments_Y[xSegment1]; }
	void setPoints1( int xSegment1, int[] aiPoints1_X, int[] aiPoints1_Y ){
		maiLineSegments_X[xSegment1] = aiPoints1_X;
		maiLineSegments_Y[xSegment1] = aiPoints1_Y;
		mctTotalPoints += aiPoints1_X.length - 1;
	}
	public String toString(){
		StringBuffer sb = new StringBuffer(500);
		sb.append("LineSegments object with " + mctSegments + " segments");
		int iGrandTotalPoints = 0;
		for( int xSegment = 1; xSegment <= mctSegments; xSegment++ ){
			int ctPoints = (maiLineSegments_X[xSegment].length - 1);
//			sb.append("  segment " + xSegment + " has " + ctPoints + " points\n");
			iGrandTotalPoints += ctPoints;
		}
		sb.append(" and " + iGrandTotalPoints + " points");
		return sb.toString();
	}
}

class Polygons extends LineSegments {
	int[] aeLevel;   /* 1 land, 2 lake, 3 island_in_lake, 4 pond_in_island_in_lake */
	int[] aiExtent_west;   /* min/max extent in micro-degrees : west */
	int[] aiExtent_east;   /* min/max extent in micro-degrees : east */
	int[] aiExtent_south;   /* min/max extent in micro-degrees : south */
	int[] aiExtent_north;   /* min/max extent in micro-degrees : north */
	int[] aiArea;   /* Area of polygon in 1/10 km^2 */
	void setCapacity( int ctPolygons ){
		super.setSegmentCapacity(ctPolygons);
		aeLevel         = new int[ctPolygons + 1];
		aiExtent_west   = new int[ctPolygons + 1];
		aiExtent_east   = new int[ctPolygons + 1];
		aiExtent_south  = new int[ctPolygons + 1];
		aiExtent_north  = new int[ctPolygons + 1];
		aiArea          = new int[ctPolygons + 1];
	}
	int getPolygonCount(){ return getSegmentCount(); }
}

//old intersection code
//					// determine corner relationships
//					// note that as a simplification cases in which the polygon cuts across a corner
//					// between two non-included points is ignored
//					if( xPolygonPoint > 1 && zCurrentPointInRegion != zLastPointInRegion ){ // then region boundary was crossed
//						if( lon_point == lon_previous ){ // vertical segment
//						} else if( lat_point == lat_previous ){ // horizontal segment
//						} else { // angled segment
//							float slope = (float)(lat_point - lat_previous)/(float)(lon_point - lon_previous);
//							int lonIntercept_North = (int)((float)(iLat_N - lat_point)/slope) + lon_point;
//							int lonIntercept_South = (int)((float)(iLat_S - lat_point)/slope) + lon_point;
//							int latIntercept_West = (int)((float)(iLon_W - lon_point)*slope) + lat_point;
//							int latIntercept_East = (int)((float)(iLon_E - lon_point)*slope) + lat_point;
//						    if( zWrapped_Lon ){
//								if( (lonIntercept_North >= iLon_W && lonIntercept_North <= 360) || (lonIntercept_North <= iLon_E && lonIntercept_North >= 0) ){
//									int intercept_transformed = lonIntercept_North - iLon_W + (lonIntercept_North < iLon_W ? 360 : 0);
//									int right_transformed = iN_Right_lon - iLon_W + (iN_Right_lon < iLon_W ? 360 : 0);
//									if( intercept_transformed > right_transformed ){
//										insertion_index_N_Right = xPolygonPoint;
//										iN_Right_lon = lonIntercept_North;
//									}
//									int left_transformed = iN_Left_lon - iLon_W + (iN_Left_lon < iLon_W ? 360 : 0);
//									if( intercept_transformed < left_transformed ){
//										insertion_index_N_Left = xPolygonPoint;
//										iN_Left_lon = lonIntercept_North;
//									}
//								}
//								if( (lonIntercept_South >= iLon_W && lonIntercept_South <= 360) || (lonIntercept_South <= iLon_E && lonIntercept_South >= 0) ){
//									int intercept_transformed = lonIntercept_South - iLon_W + (lonIntercept_South < iLon_W ? 360 : 0);
//									int right_transformed = iS_Right_lon - iLon_W + (iS_Right_lon < iLon_W ? 360 : 0);
//									if( intercept_transformed > right_transformed ){
//										insertion_index_S_Right = xPolygonPoint;
//										iS_Right_lon = lonIntercept_South;
//									}
//									int left_transformed = iS_Left_lon - iLon_W + (iS_Left_lon < iLon_W ? 360 : 0);
//									if( intercept_transformed < left_transformed ){
//										insertion_index_S_Left = xPolygonPoint;
//										iS_Left_lon = lonIntercept_South;
//									}
//								}
//							} else {
//								if( (lonIntercept_North >= iLon_W && lonIntercept_North <= iLon_E) ){ // crosses North
//									if( lonIntercept_North > iN_Right_lon ){
//										insertion_index_N_Right = xPolygonPoint;
//										iN_Right_lon = lonIntercept_North;
//									}
//									if( lonIntercept_North < iN_Left_lon ){
//										insertion_index_N_Left = xPolygonPoint;
//										iN_Left_lon = lonIntercept_North;
//									}
//								}
//								if( (lonIntercept_South >= iLon_W && lonIntercept_South <= iLon_E) ){ // crosses south
//									if( lonIntercept_South > iS_Right_lon ){
//										insertion_index_S_Right = xPolygonPoint;
//										iS_Right_lon = lonIntercept_South;
//									}
//									if( lonIntercept_South < iS_Left_lon ){
//										insertion_index_S_Left = xPolygonPoint;
//										iS_Left_lon = lonIntercept_South;
//									}
//								}
//							}
//							if( zWrapped_Lat ){
//								if( (latIntercept_West >= iLat_S && latIntercept_West <= 90) || (latIntercept_West <= iLat_N && latIntercept_West >= -90) ){
//									int intercept_transformed = latIntercept_West > iLat_N ? latIntercept_West - iLat_S : 180 + latIntercept_West - iLat_S;
//									int top_transformed = iW_Top_lat > iLat_N ? iW_Top_lat - iLat_S : 180 + iW_Top_lat - iLat_S;
//									if( intercept_transformed > top_transformed ){
//										insertion_index_W_Top = xPolygonPoint;
//										iW_Top_lat = latIntercept_West;
//									}
//									int bottom_transformed = iW_Bottom_lat > iLat_N ? iW_Bottom_lat - iLat_S : 180 + iW_Bottom_lat - iLat_S;
//									if( intercept_transformed < bottom_transformed ){
//										insertion_index_W_Bottom = xPolygonPoint;
//										iW_Bottom_lat = latIntercept_West;
//									}
//								}
//								if( (latIntercept_East >= iLat_S && latIntercept_East <= 90) || (latIntercept_East <= iLat_N && latIntercept_East >= -90) ){
//									int intercept_transformed = latIntercept_East > iLat_N ? latIntercept_East - iLat_S : 180 + latIntercept_East - iLat_S;
//									int top_transformed = iE_Top_lat > iLat_N ? iE_Top_lat - iLat_S : 180 + iE_Top_lat - iLat_S;
//									if( intercept_transformed > top_transformed ){
//										insertion_index_E_Top = xPolygonPoint;
//										iE_Top_lat = latIntercept_East;
//									}
//									int bottom_transformed = iE_Bottom_lat > iLat_N ? iE_Bottom_lat - iLat_S : 180 + iE_Bottom_lat - iLat_S;
//									if( intercept_transformed < bottom_transformed ){
//										insertion_index_E_Bottom = xPolygonPoint;
//										iE_Bottom_lat = latIntercept_East;
//									}
//								}
//							} else {
//								if( (latIntercept_West >= iLat_S && latIntercept_West <= iLat_N) ){ // crosses West
//									if( latIntercept_West > iW_Top_lat ){
//										insertion_index_W_Top = xPolygonPoint;
//										iW_Top_lat = latIntercept_West;
//									}
//									if( latIntercept_West < iW_Bottom_lat ){
//										insertion_index_W_Bottom = xPolygonPoint;
//										iW_Bottom_lat = latIntercept_West;
//									}
//								}
//								if( (latIntercept_East >= iLat_S && latIntercept_East <= iLat_N) ){ // crosses East
//									if( latIntercept_East > iE_Top_lat ){
//										insertion_index_E_Top = xPolygonPoint;
//										iE_Top_lat = latIntercept_East;
//									}
//									if( latIntercept_East < iE_Bottom_lat ){
//										insertion_index_E_Bottom = xPolygonPoint;
//										iE_Bottom_lat = latIntercept_East;
//									}
//								}
//							}
//						}
//					//	iLon_W, iLon_E, iLat_S, iLat_N
//
//						zLastPointInRegion = zCurrentPointInRegion;
//					}

// this is the old scaling algorithm that did not really work very well
//		boolean zLonAscending = (aiLongitude_micro[1] < aiLongitude_micro[2]);
//		boolean zLatAscending = (aiLatitude_micro[1] < aiLatitude_micro[2]);
//		int ctLongitudes = aiLongitude_micro.length - 1;
//		int ctLatitudes  = aiLatitude_micro.length - 1;
//    				if( zCurrentPointInRegion ){
//						ctSegmentPoints++;
//						if( ctSegmentPoints == 1 ){
//							ctRegionPolygons++;
//							if( eSTATE == STATE_Filling ){
//								segmentCurrent1_X = region_polygons.getPoints1_X( ctRegionPolygons );
//								segmentCurrent1_Y = region_polygons.getPoints1_Y( ctRegionPolygons );
//							}
//						}
//						if( eSTATE == STATE_Filling ){
//							segmentCurrent1_X[ctSegmentPoints] = polygon1_LON[xPolygonPoint];
//							segmentCurrent1_Y[ctSegmentPoints] = polygon1_LAT[xPolygonPoint];
//							if( segmentCurrent1_X[ctSegmentPoints] > 180000000 ) segmentCurrent1_X[ctSegmentPoints] -= 360000000;
//						}
//					} else {
//						if( ctSegmentPoints > 0 ){ // reached end of segment
//							if( eSTATE == STATE_CountingPoints ){
//								region_polygons.setPointCapacity(ctRegionPolygons, ctSegmentPoints);
//							}
//							ctSegmentPoints = 0;
//						}
//					}

//					// Find closest longitude
//					int iCoastline_lon = aiPolygonPoints1_X[xPoint + 1]; // in micro degrees
//					if( zLonAscending ){
//						if( aiLongitude_micro[xLongitude] <= iCoastline_lon ){
//							while( true ){
//								xLongitude++;
//								if( xLongitude > ctLongitudes ){ // out of bounds - should not happen
//									xLongitude = ctLongitudes;
//									break;
//								}
//								if( aiLongitude_micro[xLongitude] >= iCoastline_lon ){
//									xExtent_Longitude = xLongitude - 1;
//									break;
//								}
//							}
//						} else {
//							while( true ){
//								xLongitude--;
//								if( xLongitude < 1 ){ // out of bounds - should not happen
//									xLongitude = 1;
//									break;
//								}
//								if( aiLongitude_micro[xLongitude] <= iCoastline_lon ){
//									xExtent_Longitude = xLongitude + 1;
//									break;
//								}
//							}
//						}
//					} else { // longitude is descending
//						if( aiLongitude_micro[xLongitude] >= iCoastline_lon ){
//							while( true ){
//								xLongitude++;
//								if( xLongitude > ctLongitudes ){ // out of bounds, should not happen
//									xLongitude = ctLongitudes;
//									break;
//								}
//								if( aiLongitude_micro[xLongitude] <= iCoastline_lon ){
//									xExtent_Longitude = xLongitude - 1;
//									break;
//								}
//							}
//						} else {
//							while( true ){
//								xLongitude--;
//								if( xLongitude < 1 ){ // out of bounds - should not happen
//									xLongitude = 1;
//									break;
//								}
//								if( aiLongitude_micro[xLongitude] >= iCoastline_lon ){
//									xExtent_Longitude = xLongitude + 1;
//									break;
//								}
//							}
//						}
//					}
//					int iDiffBase = aiLongitude_micro[xLongitude] - iCoastline_lon; if( iDiffBase < 0 ) iDiffBase *= -1;
//					int iDiffExtent = aiLongitude_micro[xExtent_Longitude] - iCoastline_lon; if( iDiffExtent < 0 ) iDiffExtent *= -1;
//					if( iDiffExtent < iDiffBase ) xLongitude = xExtent_Longitude;
//					points_X[xPoint] = xPlotArea_LowerLeft + xLongitude;
//
//					// Find closest latitude
//					int iCoastline_lat = aiPolygonPoints1_Y[xPoint + 1]; // in micro degrees
//					if( zLatAscending ){
//						if( xLatitude > ctLatitudes ){
//							xLatitude = xLatitude;
//						}
//						if( aiLatitude_micro[xLatitude] <= iCoastline_lat ){
//							while( true ){
//								xLatitude++;
//								if( xLatitude > ctLatitudes ){ // out of bounds - should not happen
//									xLongitude = ctLatitudes;
//									break;
//								}
//								if( aiLatitude_micro[xLatitude] >= iCoastline_lat ){
//									xExtent_Latitude = xLatitude - 1;
//									break;
//								}
//							}
//						} else {
//							while( true ){
//								xLatitude--;
//								if( xLatitude < 1 ){ // out of bounds - should not happen
//									xLongitude = 1;
//									break;
//								}
//								if( aiLatitude_micro[xLatitude] <= iCoastline_lat ){
//									xExtent_Latitude = xLatitude + 1;
//									break;
//								}
//							}
//						}
//					} else { // latitude is descending
//						if( aiLatitude_micro[xLatitude] >= iCoastline_lat ){
//							while( true ){
//								xLatitude++;
//								if( xLatitude > ctLatitudes ){ // out of bounds - should not happen
//									xLongitude = ctLatitudes;
//									break;
//								}
//								if( aiLatitude_micro[xLatitude] <= iCoastline_lat ){
//									xExtent_Latitude = xLatitude - 1;
//									break;
//								}
//							}
//						} else {
//							while( true ){
//								xLatitude--;
//								if( xLatitude < 1 ){ // out of bounds should not happen
//									xLongitude = 1;
//									break;
//								}
//								if( aiLatitude_micro[xLatitude] >= iCoastline_lat ){
//									xExtent_Latitude = xLatitude + 1;
//									break;
//								}
//							}
//						}
//					}
//					iDiffBase = aiLatitude_micro[xLatitude] - iCoastline_lat; if( iDiffBase < 0 ) iDiffBase *= -1;
//					iDiffExtent = aiLatitude_micro[xExtent_Latitude] - iCoastline_lat; if( iDiffExtent < 0 ) iDiffExtent *= -1;
//					if( iDiffExtent < iDiffBase ) xLatitude = xExtent_Latitude;
//					points_Y[xPoint] = yPlotArea_LowerLeft - xLatitude;
