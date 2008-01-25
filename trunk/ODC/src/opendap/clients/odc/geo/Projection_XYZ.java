package opendap.clients.odc.geo;

import geotrans.Geocentric;
import geotrans.GeotransError;
import geotrans.JNIException;
import geotrans.LocalCartesian;
import geotrans.JNIEngine;

public class Projection_XYZ {
//	private Geocentric geocentric;
//	private LocalCartesian local_cartesian;
//	public double getX(){
//		switch( eProjection ){
//			case Geocentric: return geocentric.getX();
//			case LocalCartesian: local_cartesian.getX();
//            default: return Double.NaN;
//		}
//	}
//	public double getY(){
//		switch( eProjection ){
//			case Geocentric: return geocentric.getY();
//			case LocalCartesian: local_cartesian.getY();
//            default: return Double.NaN;
//		}
//	}
//	public double getZ(){
//		switch( eProjection ){
//			case Geocentric: return geocentric.getZ();
//			case LocalCartesian: local_cartesian.getZ();
//            default: return Double.NaN;
//		}
//	}
//	public void setCoordinates( int state, int direction, double x, double y, double z )
//	{
//		JNIEngine geotrans_engine = opendap.clients.odc.ApplicationController.getInstance().getGeodesy().getGeoTransEngine();
//		try {
//			switch( eProjection ){
//				case Geocentric:
//					geocentric = new Geocentric( x, y, z );
//					geotrans_engine.JNISetGeocentricCoordinates( state, direction, geocentric );
//					break;
//				case LocalCartesian:
//					local_cartesian = new LocalCartesian( x, y, z );
//					geotrans_engine.JNISetLocalCartesianCoordinates( state, direction, local_cartesian );
//					break;
//                default:
//                	System.err.println("invalid method for coordinate type XYZ");
//			}
//		} catch(GeotransError e) {
//            //
//		} catch(JNIException e) {
//			// 
//        }
//	}
}
