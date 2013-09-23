package opendap.clients.odc.geo;

import geotrans.JNIEngine;

abstract public class Projection<T extends Projection> {
	protected final static double PI = 3.14159265358979323e0;  
    protected final static int INPUT = 0;
    protected final static int OUTPUT = 1;
	public int iDatumIndex;
	public Projection(){
		iDatumIndex = Geodesy.getInstance().getWGS84index();
	}
	public boolean setDatum( int iDatumIndex, StringBuffer sbError ){
		int iDatumCount = Geodesy.getInstance().getDatumCount();
		if( iDatumIndex < 1 || iDatumIndex > iDatumCount ){
			sbError.append( "invalid datum index " + iDatumIndex + " (1-" + iDatumCount + ")" );
			return false;
		}
		return true;
	}
	abstract public ProjectionType getProjectionType();

	public double[] getMapping_Latitude(){ return null; } // TODO
	public double[] getMapping_Longitude(){ return null; }

	static public Projection getOutput( JNIEngine geotrans_engine, Projection.ProjectionType projection, StringBuffer sbError ){
		switch( projection ){
			case Geocentric:               // x: y: z
				return Projection_Geocentric.getOutput( geotrans_engine, sbError );
			case Geodetic:                 // latitude: longitude: height (optional)
				return Projection_Geodetic.getOutput( geotrans_engine, sbError );
			case LocalCartesian:
				return Projection_LocalCartesian.getOutput( geotrans_engine, sbError );
			case Perspective:
			case Georef:                   // World Geographic Reference System
			case GARS:
			case MGRS:                     // Military Grid Reference System
			case USNG:                     // United States National Grid
			case BNG:                      // British National Grid
			case UTM:
			case UPS:
			case Albers:
			case Azimuthal:
			case Bonne:
			case Cassini:
			case CylindricalEqualArea:
			case CylindricalEquidistant:
			case CylindricalTransverseEA:
			case Eckert4:
			case Eckert6:
			case Gnomic:
			case Lambert1:
			case Lambert2:
			case Mercator:
			case MercatorOblique:
			case MercatorTransverse:
			case Miller:
			case Mollweide:
			case Neys:
			case NZMG:
			case Orthographic:
			case PolarStereo:
			case Sinusoidal:
			case Stereographic:
			case VanDerGrinten:
		}
		sbError.append("unsupported output projection");
		return null;
	}
	
	public boolean setAsInput_Parameters( JNIEngine geotrans_engine, StringBuffer sbError ){
		if( ! setParameters( geotrans_engine, INPUT, sbError ) ){
			sbError.insert( 0, "error setting input parameters: " );
			return false;
		}
		return true;
	}
	public boolean setAsInput_Coordinates( JNIEngine geotrans_engine, StringBuffer sbError ){
		if( ! setCoordinates( geotrans_engine, INPUT, sbError ) ){
			sbError.insert( 0, "error setting input coordinates: " );
			return false;
		}
		return true;
	}
	public boolean setAsOutput_Parameters( JNIEngine geotrans_engine, StringBuffer sbError ){
		if( ! setParameters( geotrans_engine, OUTPUT, sbError ) ){
			sbError.insert( 0, "error setting output parameters: " );
			return false;
		}
		return true;
	}
	public boolean setAsOutput_Coordinates( JNIEngine geotrans_engine, StringBuffer sbError ){
		if( ! setCoordinates( geotrans_engine, OUTPUT, sbError ) ){
			sbError.insert( 0, "error setting output coordinates: " );
			Thread.dumpStack();
			return false;
		}
		return true;
	}

	abstract protected boolean setCoordinates( JNIEngine geotrans_engine, int direction, StringBuffer sbError );
	abstract protected boolean setParameters( JNIEngine geotrans_engine, int direction, StringBuffer sbError );
	abstract protected String sDump();
	
	public enum ProjectionType {
		Geocentric,               // x, y, z
		Geodetic,                 // latitude, longitude, height (optional)
		Perspective,
		LocalCartesian,
		Georef,                   // World Geographic Reference System
		GARS,
		MGRS,                     // Military Grid Reference System
		USNG,                     // United States National Grid
		BNG,                      // British National Grid
		UTM,
		UPS,
		Albers,
		Azimuthal,
		Bonne,
		Cassini,
		CylindricalEqualArea,
		CylindricalEquidistant,
		CylindricalTransverseEA,
		Eckert4,
		Eckert6,
		Gnomic,
		Lambert1,
		Lambert2,
		Mercator,
		MercatorOblique,
		MercatorTransverse,
		Miller,
		Mollweide,
		Neys,
		NZMG,
		Orthographic,
		PolarStereo,
		Sinusoidal,
		Stereographic,
		VanDerGrinten,
	};

	/** returns -1 if projection not supported by Geotrans */
	static int getGeotransProjectionOrdinal( ProjectionType e ){
		switch( e ){
			case Geocentric: return 3;               // x: return ; y: return ; z
			case Geodetic: return 0;                 // latitude, longitude, height (optional)
			case Perspective: return -1;
			case LocalCartesian: return 4;
			case Georef: return 1;                   // World Geographic Reference System
			case GARS: return 2;
			case MGRS: return 5;                     // Military Grid Reference System
			case USNG: return 6;                     // United States National Grid
			case BNG: return 11;                      // British National Grid
			case UTM: return 7;
			case UPS: return 8;
			case Albers: return 9;
			case Azimuthal: return 10;
			case Bonne: return 12;
			case Cassini: return 13;
			case CylindricalEqualArea: return 14;
			case CylindricalEquidistant: return 17;
			case CylindricalTransverseEA: return 32;
			case Eckert4: return 15;
			case Eckert6: return 16;
			case Gnomic: return 18;
			case Lambert1: return 19;
			case Lambert2: return 20;
			case Mercator: return 21;
			case MercatorOblique: return 26;
			case MercatorTransverse: return 33;
			case Miller: return 22;
			case Mollweide: return 23;
			case Neys: return 24;
			case NZMG: return 25;
			case Orthographic: return 27;
			case PolarStereo: return 28;
			case Sinusoidal: return 30;
			case Stereographic: return 31;
			case VanDerGrinten: return 34;
			default: return -1;
		}
	}
	
	
}
