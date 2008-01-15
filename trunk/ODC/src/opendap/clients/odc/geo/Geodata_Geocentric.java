package opendap.clients.odc.geo;

import geotrans.ConversionState;
import geotrans.Geocentric;
import geotrans.GeotransError;
import geotrans.JNIEngine;
import geotrans.JNIException;

public class Geodata_Geocentric extends Geodata {
	private Geocentric geocentric;
	public Geodata_Geocentric( double x, double y, double z ){
		geocentric = new Geocentric( x, y, z );
	}
	public Geodata_Geocentric( Geocentric geocentric ){
		this.geocentric = geocentric;
	}
	public Projection getProjection(){ return Geodata.Projection.Geocentric; }
	public double getX(){ return geocentric.getX(); }
	public double getY(){ return geocentric.getY(); }
	public double getZ(){ return geocentric.getZ(); }
	public static Geodata getOutput( JNIEngine geotrans_engine, StringBuffer sbError ){
		try {
			return new Geodata_Geocentric( geotrans_engine.JNIGetGeocentricCoordinates( ConversionState.INTERACTIVE, OUTPUT ) );
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
			geotrans_engine.JNISetGeocentricCoordinates( ConversionState.INTERACTIVE, direction, geocentric );
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
		return "X: " + geocentric.getX()+ " Y: " + geocentric.getY() + " Z: " + geocentric.getZ();
	}
}
