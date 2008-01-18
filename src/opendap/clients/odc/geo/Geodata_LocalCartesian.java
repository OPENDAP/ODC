package opendap.clients.odc.geo;

import geotrans.ConversionState;
import geotrans.LocalCartesian;
import geotrans.GeotransError;
import geotrans.JNIEngine;
import geotrans.JNIException;

public class Geodata_LocalCartesian extends Geodata {
	private LocalCartesian local_cartesian;
	
	// parameter constructor
	public Geodata_LocalCartesian( 
			double origin_lat_degrees,
			double origin_lon_degrees,
			double origin_height,
			double orientation_degreesN ){ // degrees clockwise from North
		this.local_cartesian = new LocalCartesian( origin_lat_degrees * PI / 180, origin_lon_degrees * PI / 180, origin_height, orientation_degreesN * PI / 180 );   
	}
	
	// output constructor
	public Geodata_LocalCartesian( double x, double y, double z ){
		this.local_cartesian = new LocalCartesian( x, y, z );
	}
	public Geodata_LocalCartesian( LocalCartesian local_cartesian ){
		this.local_cartesian = local_cartesian;
	}
	public Projection getProjection(){ return Geodata.Projection.Geocentric; }
	public double getX(){ return local_cartesian.getX(); }
	public double getY(){ return local_cartesian.getY(); }
	public double getZ(){ return local_cartesian.getZ(); }
	public static Geodata getOutput( JNIEngine geotrans_engine, StringBuffer sbError ){
		try {
			return new Geodata_LocalCartesian( geotrans_engine.JNIGetLocalCartesianCoordinates( ConversionState.INTERACTIVE, OUTPUT ) );
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
		return true; // geocentric has no parameters
	}
	public String sDump(){
		return "X: " + local_cartesian.getX()+ " Y: " + local_cartesian.getY() + " Z: " + local_cartesian.getZ();
	}
}
