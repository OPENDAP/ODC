package opendap.clients.odc.geo;

import geotrans.ConversionState;
import geotrans.Geodetic;
import geotrans.GeotransError;
import geotrans.JNIEngine;
import geotrans.JNIException;

public class Projection_Geodetic extends Projection {
	private Geodetic geodetic;
	/** set flag to indicate whether inputs are in degrees or radians */
	public Projection_Geodetic( double latitude, double longitude, double height_meters, boolean zInDegrees ){
		if( zInDegrees ){
			geodetic = new Geodetic( longitude * PI / 180, latitude * PI / 180, height_meters );
		} else {
			geodetic = new Geodetic( longitude, latitude, height_meters );
		}
	}
	public Projection_Geodetic( Geodetic geodetic ){
		this.geodetic = geodetic;
	}
	public ProjectionType getProjectionType(){ return Projection.ProjectionType.Geodetic; }
	public double getLongitude_radians(){ return geodetic.getLongitude(); }
	public double getLatitude_radians(){ return geodetic.getLatitude(); }
	public double getLongitude_degrees(){ return geodetic.getLongitude() * 180/PI; }
	public double getLatitude_degrees(){ return geodetic.getLatitude() * 180/PI; }
	public double getHeight_meters(){ return geodetic.getHeight(); }
	public static Projection getOutput( JNIEngine geotrans_engine, StringBuffer sbError ){
		try {
			return new Projection_Geodetic( geotrans_engine.JNIGetGeodeticCoordinates( ConversionState.INTERACTIVE, OUTPUT ) );
		} catch(GeotransError e) {
			sbError.insert( 0, "geotrans error: " + e );
			return null;
		} catch( JNIException e ) {
			sbError.insert( 0, "JNI error: " + e );
			return null;
        } catch( Throwable t ) {
        	Utility.vUnexpectedError( t, sbError );
        	return null;
        }
	}
	protected boolean setCoordinates( JNIEngine geotrans_engine, int direction, StringBuffer sbError ) {
		try {
			geotrans_engine.JNISetGeodeticCoordinates( ConversionState.INTERACTIVE, direction, geodetic );
		} catch(GeotransError e) {
			sbError.insert( 0, "geotrans error: " + e );
			return false;
		} catch( JNIException e ) {
			sbError.insert( 0, "JNI error: " + e );
			return false;
        } catch( Throwable t ) {
        	Utility.vUnexpectedError( t, sbError );
        	return false;
        }
        return true;
	}
	protected boolean setParameters( JNIEngine geotrans_engine, int direction, StringBuffer sbError ){
		try {
			geotrans_engine.JNISetGeodeticParams( ConversionState.INTERACTIVE, direction, geodetic );
		} catch(GeotransError e) {
			sbError.insert( 0, "geotrans error: " + e );
			return false;
		} catch( JNIException e ) {
			sbError.insert( 0, "JNI error: " + e );
			return false;
        } catch( Throwable t ) {
        	Utility.vUnexpectedError( t, sbError );
        	return false;
        }
        return true;
	}
	public String sDump(){
		return "lat: " + getLatitude_degrees() + " lon: " + getLongitude_degrees() + " height: " + getHeight_meters();
	}
}
