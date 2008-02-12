package opendap.clients.odc.geo;

import geotrans.ConversionState;
import geotrans.LocalCartesian;
import geotrans.GeotransError;
import geotrans.JNIEngine;
import geotrans.JNIException;

public class Projection_LocalCartesian extends Projection {
	private LocalCartesian local_cartesian;
	
	// parameter constructor
	public Projection_LocalCartesian( 
			double origin_lat,            // in decimal degrees or radians (see boolean switch)
			double origin_lon,
			double origin_height,
			double orientation_degreesN,  // degrees clockwise from North
			boolean zInDegrees            // whether the lat/lon are in decimal degrees, radians if false
		){ 
		if( zInDegrees ){ // in decimal degrees
			this.local_cartesian = new LocalCartesian( origin_lat * PI / 180, origin_lon * PI / 180, origin_height, orientation_degreesN * PI / 180 );
		} else { // in radians
			this.local_cartesian = new LocalCartesian( origin_lat, origin_lon, origin_height, orientation_degreesN * PI / 180 );
//			System.out.println("set lat to: " + (origin_lat * 180)/PI + "  lon: " + (origin_lon * 180)/PI );
		}
	}
	
	// output constructor
	public Projection_LocalCartesian( double x, double y, double z ){
		this.local_cartesian = new LocalCartesian( x, y, z );
	}
	public Projection_LocalCartesian( LocalCartesian local_cartesian ){
		this.local_cartesian = local_cartesian;
	}
	public ProjectionType getProjectionType(){ return Projection.ProjectionType.LocalCartesian; }
	public double getX(){ return local_cartesian.getX(); }
	public double getY(){ return local_cartesian.getY(); }
	public double getZ(){ return local_cartesian.getZ(); }
	public static Projection getOutput( JNIEngine geotrans_engine, StringBuffer sbError ){
		try {
			return new Projection_LocalCartesian( geotrans_engine.JNIGetLocalCartesianCoordinates( ConversionState.INTERACTIVE, OUTPUT ) );
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
			geotrans_engine.JNISetLocalCartesianCoordinates( ConversionState.INTERACTIVE, direction, local_cartesian );
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
	protected boolean setParameters( JNIEngine geotrans_engine, int direction, StringBuffer sbError ) {
		try {
			geotrans_engine.JNISetLocalCartesianParams( ConversionState.INTERACTIVE, direction, local_cartesian );
		} catch(GeotransError e) {
			sbError.insert( 0, "geotrans error: " + e);
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
		if( local_cartesian.getOriginLatitude() != 0 ){
			return "origin lat: " + local_cartesian.getOriginLatitude() + " origin lon: " + local_cartesian.getOriginLongitude() + " origin height: " + local_cartesian.getOriginHeight() + " origin orientation: " + local_cartesian.getOrientation(); 
		}
		return "X: " + local_cartesian.getX()+ " Y: " + local_cartesian.getY() + " Z: " + local_cartesian.getZ();
	}
}
