package opendap.clients.odc.geo;

import geotrans.*;

public class Geodesy {
	private final static Geodesy singleton = new Geodesy();
	public native java.lang.String JNIErrorPrefix ( int direction,  int system);
    public final static int INPUT = 0;
    public final static int OUTPUT = 1;	
	private JNIEngine geotrans_engine;
    private JNIStrtoval jniStrtoval;
    private int mctDatums = 0;
    private int mctEllipsoids = 0;
	private String masDatumName[];
	private String masDatumCode[];
	private String masEllipsoidName[];
	private String masEllipsoidCode[];
	public final JNIEngine getGeoTransEngine(){ return geotrans_engine; }
	private Geodesy(){} // private constructor to enforce singleton -- needed for default datum initialization
	public static Geodesy getInstance(){ return singleton; }
	
	/** example of using this package */
	public static void main(String[] args){
		StringBuffer sbError = new StringBuffer();
		try {
			Geodesy geodesy = Geodesy.getInstance();
			if( ! geodesy.zInitialize( "C:\\dev\\workspace\\ODC\\base", sbError ) ){
				System.err.println( "error initializing geodesy package: " + sbError );
				System.exit(0);
			}
			if( ! geodesy.setDatums( geodesy.getWGS84index(), sbError ) ){
				System.err.println( "error setting datums to WGS84: " + sbError );
				System.exit(0);
			}
			Geodata_Geocentric dataGeocentric = new Geodata_Geocentric( 3848899.322, 3691426.002, 3486838.194 );
			Geodata_Geodetic dataGeodetic_out = new Geodata_Geodetic( 0, 0, 0 );
			Geodata_Geodetic dataGeodetic = (Geodata_Geodetic)geodesy.convert( dataGeocentric, dataGeodetic_out, sbError );
//			Geodata_Geodetic dataGeodetic = (Geodata_Geodetic)geodesy.convert( dataGeocentric, Geodata.Projection.Geodetic, sbError );
			if( dataGeodetic == null){
				System.err.println( "error converting: " + sbError );
				System.exit(0);
			} else {
				System.out.println( dataGeocentric.sDump() + " converts to " + dataGeodetic.sDump() );
			}
		} catch( Throwable t ) {
			Utility.vUnexpectedError(t, sbError);
			System.err.println("Unexpected error: " + sbError.toString() );
			System.exit(0);
		}
	}
//X: 3848899.322 Y: 3691426.002 Z: 3486838.194 converts to lat: 0.582140580222199 lon: 0.7645170441702235 height: 1.939870516769588
//X: 3848899.322 Y: 3691426.002 Z: 3486838.194 converts to lat: 0.582140580222199 lon: 0.7645170441702235 height: 0.0	
	public boolean zInitialize( String sBaseDirectory, StringBuffer sbError ){
		String sPath_GeoTrans_dir = null;
		String sPath_GeoTrans_libs = null;
		try {
			// String sGeoTrans_EnvironmentVar = "GEOTRANS_DATA"; // this environment variable shows where data directory is (ellips.dat, etc) 
			String sGeoTrans2_libname = System.mapLibraryName( "geotrans2" );
			String sJNIGeoTrans_libname = System.mapLibraryName( "jnigeotrans" );
			sPath_GeoTrans_dir = Utility.sConnectPaths( sBaseDirectory, "GeoTrans" );
			sPath_GeoTrans_libs = Utility.sConnectPaths( sPath_GeoTrans_dir, "lib" );
			String sPath_GeoTrans2 = Utility.sConnectPaths( sPath_GeoTrans_libs, sGeoTrans2_libname );
			String sPath_JNIGeoTrans = Utility.sConnectPaths( sPath_GeoTrans_libs, sJNIGeoTrans_libname );
			System.load(sPath_GeoTrans2);
			System.load(sPath_JNIGeoTrans);
			geotrans_engine = new JNIEngine();
			geotrans_engine.JNIInitializeEngine();
			jniStrtoval = new JNIStrtoval();
			if( zLoadDatums( sbError ) ){
//				System.out.println( "Datums:\n" + sDumpDatums() );
//				System.out.println( "\nEllipsoids:\n" + sDumpEllipsoids() );
			} else {
				sbError.insert( 0, "error loading datums: " );
				return false;
			}
		} catch( Throwable t ) {
			sbError.append("error loading GeoTrans libraries (" + sPath_GeoTrans_libs + "): " + t);
			return false;
		}
		return true;
	}
    
	public int getDatumCount(){ return mctDatums; }
	public int getEllipsoidCount(){ return mctEllipsoids; }
	public String[] getDatumNames(){ return masDatumName; }
	public String[] getDatumCodes(){ return masDatumCode; }
	public String[] getEllipsoidNames(){ return masEllipsoidName; }
	public String[] getEllipsoidCodes(){ return masEllipsoidCode; }
	public int getWGS84index(){ try { return (int)geotrans_engine.JNIGetDatumIndex ("WGE"); } catch( Throwable t ) { return -1; } }
//    ellipsoidList.setSelectedIndex((int)jniEngine.JNIGetEllipsoidIndex ("WE") - 1);
	
	public boolean zLoadDatums( StringBuffer sbError ){
		try {
			int ctDatums = (int)geotrans_engine.JNIGetDatumCount();
			masDatumName = new String[ ctDatums + 1]; // one based
			masDatumCode = new String[ ctDatums + 1]; // one based
			for( int xDatum = 1; xDatum <= ctDatums; xDatum++ ){
				masDatumName[xDatum] = geotrans_engine.JNIGetDatumName( xDatum );
				masDatumCode[xDatum] = geotrans_engine.JNIGetDatumCode( xDatum );
			}
			mctDatums = ctDatums;
			int ctEllipsoids = (int)geotrans_engine.JNIGetEllipsoidCount();
			masEllipsoidName = new String[ ctEllipsoids + 1]; // one based
			masEllipsoidCode = new String[ ctEllipsoids + 1]; // one based
			for( int xEllipsoid = 1; xEllipsoid <= ctEllipsoids; xEllipsoid++ ){
				masEllipsoidName[xEllipsoid] = geotrans_engine.JNIGetEllipsoidName( xEllipsoid );
				masEllipsoidCode[xEllipsoid] = geotrans_engine.JNIGetEllipsoidCode( xEllipsoid );
			}
			mctEllipsoids = ctEllipsoids;
			return true;
		} catch( GeotransError e ) {
			sbError.append( "GeoTrans error: " + e );
			return false;
		} catch( JNIException e ) {
			sbError.append( "JNI error: " + e );
			return false;
		} catch( Throwable t ) {
			Utility.vUnexpectedError( t, sbError );
			return false;
		}    
	}
    
	public String sDumpDatums(){
		if( mctDatums == 0 ) return "[no datums loaded]";
		StringBuffer sbDatums = new StringBuffer( 10000 );
		for( int xDatum = 1; xDatum <= mctDatums; xDatum++ ){
			sbDatums.append( "" + xDatum + ' ' + masDatumCode[xDatum] + ' ' + masDatumName[xDatum] + '\n' );
		}
		return sbDatums.toString();
	}
	public String sDumpEllipsoids(){
		if( mctEllipsoids == 0 ) return "[no Ellipsoids loaded]";
		StringBuffer sbEllipsoids = new StringBuffer( 10000 );
		for( int xEllipsoid = 1; xEllipsoid <= mctEllipsoids; xEllipsoid++ ){
			sbEllipsoids.append( "" + xEllipsoid + ' ' + masEllipsoidCode[xEllipsoid] + ' ' + masEllipsoidName[xEllipsoid] + '\n' );
		}
		return sbEllipsoids.toString();
	}
	
//	public <T extends Geodata> T convert(Geodata geoData, Class<T> clazz, int error)
//	public <T extends Geodata> T convert( Geodata in, Geodata<T> out, StringBuffer sbError ){

	/** returns a new Geodata object of the type given as 'out' with the converted values */ 
	public Geodata convert( Geodata in, Geodata out, StringBuffer sbError ){
		try {
//			setAccuracy( ce90, le90, se90 );
			geotrans_engine.JNISetCoordinateSystem( ConversionState.INTERACTIVE, INPUT, Geodata.getGeotransProjectionOrdinal( in.getProjection() ) );
            if( ! in.setAsInput( geotrans_engine, sbError ) ){ sbError.insert( 0, "input: " ); return null; }
            if( ! out.setAsOutput( geotrans_engine, sbError ) ){ sbError.insert( 0, "output: " ); return null; }
			geotrans_engine.JNIConvert( ConversionState.INTERACTIVE );
			return Geodata.getOutput( geotrans_engine, out.getProjection(), sbError );
		} catch( GeotransError e ) {
			sbError.append( e.getMessage() );
			return null;
		} catch( GeotransWarning e ) {
			sbError.append( e.getMessage() );
			return null;
		} catch( JNIException e ){
			sbError.append( e.getMessage() );
			return null;
		}
	}

	public boolean setDatums( int iDatumIndex, StringBuffer sbError ){
		try {
			geotrans_engine.JNISetDatum( ConversionState.INTERACTIVE, INPUT, iDatumIndex );
			geotrans_engine.JNISetDatum( ConversionState.INTERACTIVE, OUTPUT, iDatumIndex );
		} catch( GeotransError e ) {
			sbError.append( e.getMessage() );
			return false;
		} catch( Throwable t ) {
			Utility.vUnexpectedError( t, sbError );
			return false;
		}
		return true;
	}
	
	/** assumes that the output parameters have already been set */
	public Geodata convert( Geodata in, Geodata.Projection projection, StringBuffer sbError ){
		try {
//			setAccuracy( ce90, le90, se90 );
			geotrans_engine.JNISetCoordinateSystem( ConversionState.INTERACTIVE, INPUT, Geodata.getGeotransProjectionOrdinal( in.getProjection() ) );
            if( ! in.setAsInput( geotrans_engine, sbError ) ){ sbError.insert( 0, "input: " ); return null; }
			geotrans_engine.JNIConvert( ConversionState.INTERACTIVE );
			return Geodata.getOutput( geotrans_engine, projection, sbError );
		} catch( GeotransError e ) {
			sbError.append( e.getMessage() );
			return null;
		} catch( GeotransWarning e ) {
			sbError.append( e.getMessage() );
			return null;
		} catch( JNIException e ){
			sbError.append( e.getMessage() );
			return null;
		}
	}
	
	void setInput(){}
	void setOutput(){}

	public void determineAccuracy() {
		int eSTATE = 1; // file 0 , INTERACTIVE 1, header 2
		boolean _3dConversion = true;
		
        Accuracy accuracy = geotrans_engine.JNIGetConversionErrors( eSTATE );
        double ce90 = accuracy.getCE90(); // -1.0 means unknown
        double le90 = accuracy.getLE90();
        double se90 = accuracy.getSE90();
	}

	// -1 is unknown value
	public void setAccuracy( double ce90, double le90, double se90 ){
		boolean zChangeAccuracy = true;
        if ( zChangeAccuracy ){
            try
            {
                geotrans_engine.JNISetConversionErrors( ConversionState.INTERACTIVE, ce90, le90, se90 );
            }
            catch(GeotransError e)
            {
                //
            }
        }
	}
		
	public void setEngineParams_ALBERS( int iMode, int iDirection,  int iDatum, int iProjectionType,
			double latOrigin,
			double lonCentralMeridien,
			double lat1stParallel,
			double lat2ndParallel,
			double dFalseEasting,
			double dFalseNorthing
		)
	{
		try {
			geotrans_engine.JNISetDatum( iMode, iDirection, iDatum );
			if( iProjectionType == CoordinateTypes.F16GRS )
				geotrans_engine.JNISetCoordinateSystem(iMode, iDirection, CoordinateTypes.MGRS);
			else
				geotrans_engine.JNISetCoordinateSystem(iMode, iDirection, iProjectionType);
            
			AlbersEqualAreaConic albersParams = new AlbersEqualAreaConic( latOrigin,
																		  lonCentralMeridien,
                                                                          lat1stParallel,
                                                                          lat2ndParallel,
                                                                          dFalseEasting,
                                                                          dFalseNorthing);
			geotrans_engine.JNISetAlbersEqualAreaConicParams( iMode, iDirection, albersParams);
		} catch( GeotransError e ) {
            // jniStrtoval.setEngineError(true, e.getMessage());
        } catch(JNIException e) {
            // jniStrtoval.setJNIError(true, e.getMessage());
        }
	}

	public void setEngineParams_AZIMUTHAL( int iMode, int iDirection,  int iDatum, int iProjectionType,
    		double latOrigin,
    		double lonCentralMeridien,
    		double dFalseEasting,
    		double dFalseNorthing
	){
		try {
		switch(iProjectionType)
		{
			case CoordinateTypes.AZIMUTHAL:
                {
                    AzimuthalEquidistant azeqParams = new AzimuthalEquidistant( latOrigin,
                                                                                lonCentralMeridien,
                                                                                dFalseEasting,
                                                                                dFalseNorthing );
                    geotrans_engine.JNISetAzimuthalEquidistantParams(iMode, iDirection, azeqParams);
                    break;
                }
                case CoordinateTypes.BONNE:
                {
                    Bonne bonneParams = new Bonne( latOrigin,
                                                   lonCentralMeridien,
                                                   dFalseEasting,
                                                   dFalseNorthing );
                    geotrans_engine.JNISetBonneParams(iMode, iDirection, bonneParams);
                    break;
                }
                case CoordinateTypes.CASSINI:
                {
                    Cassini cassiniParams = new Cassini( latOrigin,
                                                   lonCentralMeridien,
                                                   dFalseEasting,
                                                   dFalseNorthing );
                   geotrans_engine.JNISetCassiniParams(iMode, iDirection, cassiniParams);

                    break;
                }
                case CoordinateTypes.CYLEQA:
                {
                    CylindricalEqualArea params = new CylindricalEqualArea( latOrigin,
                                                   lonCentralMeridien,
                                                   dFalseEasting,
                                                   dFalseNorthing );
                    geotrans_engine.JNISetCylindricalEqualAreaParams( iMode, iDirection, params );
                    break;
                }
                case CoordinateTypes.ECKERT4:
                {
                    Eckert4 params = new Eckert4(  lonCentralMeridien,
                                                   dFalseEasting,
                                                   dFalseNorthing );
                    geotrans_engine.JNISetEckert4Params(iMode, iDirection, params);
                    break;
                }
                case CoordinateTypes.ECKERT6:
                {
                    Eckert6 params = new Eckert6(  lonCentralMeridien,
                                                   dFalseEasting,
                                                   dFalseNorthing );
                    geotrans_engine.JNISetEckert6Params(iMode, iDirection, params);
                    break;
                }
                case CoordinateTypes.EQDCYL:
                {
                    EquidistantCylindrical params = new EquidistantCylindrical( latOrigin, // standard parallel
                                                   lonCentralMeridien,
                                                   dFalseEasting,
                                                   dFalseNorthing );
                    geotrans_engine.JNISetEquidistantCylindricalParams(iMode, iDirection, params);
                    break;
                }
                case CoordinateTypes.GNOMONIC:
                {
                    Gnomonic params = new Gnomonic( latOrigin,
                                                    lonCentralMeridien,
                                                    dFalseEasting,
                                                    dFalseNorthing );
                    geotrans_engine.JNISetGnomonicParams( iMode, iDirection, params );
                    break;
                }
                case CoordinateTypes.MILLER:
                {
                    MillerCylindrical params = new MillerCylindrical( 
                                                    lonCentralMeridien,
                                                    dFalseEasting,
                                                    dFalseNorthing );
                    geotrans_engine.JNISetMillerCylindricalParams( iMode, iDirection, params );
                    break;
                }
                case CoordinateTypes.MOLLWEIDE:
                {
                    Mollweide params = new Mollweide( 
                                                    lonCentralMeridien,
                                                    dFalseEasting,
                                                    dFalseNorthing );
                    geotrans_engine.JNISetMollweideParams( iMode, iDirection, params );
                    break;
                }
                case CoordinateTypes.ORTHOGRAPHIC:
                {
                    Orthographic params = new Orthographic( latOrigin,
                                                    lonCentralMeridien,
                                                    dFalseEasting,
                                                    dFalseNorthing );
                    geotrans_engine.JNISetOrthographicParams( iMode, iDirection, params );
                    break;
                }
                case CoordinateTypes.POLARSTEREO:
                {
                    PolarStereographic params = new PolarStereographic( latOrigin, // latitude of true scale
                                                    lonCentralMeridien, // longitude down from pole
                                                    dFalseEasting,
                                                    dFalseNorthing );
                    geotrans_engine.JNISetPolarStereographicParams( iMode, iDirection, params );
                    break;
                }
                case CoordinateTypes.POLYCONIC:
                {
                    Polyconic params = new Polyconic( latOrigin, // latitude of true scale
                                                    lonCentralMeridien, // longitude down from pole
                                                    dFalseEasting,
                                                    dFalseNorthing );
                    geotrans_engine.JNISetPolyconicParams( iMode, iDirection, params );
                    break;
                }
                case CoordinateTypes.SINUSOIDAL:
                {
                    Sinusoidal params = new Sinusoidal(  lonCentralMeridien,
                                                   dFalseEasting,
                                                   dFalseNorthing );
                    geotrans_engine.JNISetSinusoidalParams(iMode, iDirection, params);
                    break;
                }
                case CoordinateTypes.STEREOGRAPHIC:
                {
                    Stereographic params = new Stereographic( latOrigin, // latitude of true scale
                                                    lonCentralMeridien, // longitude down from pole
                                                    dFalseEasting,
                                                    dFalseNorthing );
                    geotrans_engine.JNISetStereographicParams( iMode, iDirection, params );
                    break;
                }
                case CoordinateTypes.GRINTEN:
                {
                    VanDerGrinten params = new VanDerGrinten( latOrigin,
                                                    dFalseEasting,
                                                    dFalseNorthing );
                    geotrans_engine.JNISetVanDerGrintenParams( iMode, iDirection, params );
                    break;
                }
            }
		} catch( GeotransError e ) {
            // jniStrtoval.setEngineError(true, e.getMessage());
        } catch(JNIException e) {
            // jniStrtoval.setJNIError(true, e.getMessage());
        }
	}

	public void setEngineParams_LAMBERT_conformal( int iMode, int iDirection,  int iDatum, int iProjectionType,
    		double latOrigin,
    		double lonCentralMeridien,
    		double dScaleFactor,
    		double dFalseEasting,
    		double dFalseNorthing
	){
		try {
		switch(iProjectionType) {
			case CoordinateTypes.LAMBERT_1:
			{
				LambertConformalConic1 params = new LambertConformalConic1( latOrigin,
                                                    lonCentralMeridien,
                                                    dScaleFactor,
                                                    dFalseEasting,
                                                    dFalseNorthing );
				geotrans_engine.JNISetLambertConformalConic1Params( iMode, iDirection, params );
				break;
			}
			case CoordinateTypes.MERCATOR:
			{
				Mercator params = new Mercator( latOrigin,
                                                    lonCentralMeridien,
                                                    dScaleFactor,
                                                    dFalseEasting,
                                                    dFalseNorthing );
				geotrans_engine.JNISetMercatorParams( iMode, iDirection, params );
				break;
			}
			case CoordinateTypes.TRCYLEQA:
			{
				TransverseCylindricalEqualArea params = new TransverseCylindricalEqualArea( latOrigin,
                                                    lonCentralMeridien,
                                                    dScaleFactor,
                                                    dFalseEasting,
                                                    dFalseNorthing );
				geotrans_engine.JNISetTransverseCylindricalEqualAreaParams( iMode, iDirection, params );
				break;
			}
			case CoordinateTypes.TRANMERC:
			{
				TransverseMercator params = new TransverseMercator( latOrigin,
                                                    lonCentralMeridien,
                                                    dScaleFactor,
                                                    dFalseEasting,
                                                    dFalseNorthing );
				geotrans_engine.JNISetTransverseMercatorParams( iMode, iDirection, params );
				break;
			}
		}
		} catch( GeotransError e ) {
            // jniStrtoval.setEngineError(true, e.getMessage());
        } catch(JNIException e) {
            // jniStrtoval.setJNIError(true, e.getMessage());
        }
	}

	public void setEngineParams_LAMBERT_parallel( int iMode, int iDirection,  int iDatum, int iProjectionType,
    		double latOrigin,
    		double lonCentralMeridien,
    		double d1stParallel,
    		double d2ndParallel,
    		double dFalseEasting,
    		double dFalseNorthing
	){
		try {
		switch(iProjectionType){
			case CoordinateTypes.LAMBERT_2:
			{
				LambertConformalConic2 params = new LambertConformalConic2( latOrigin,
                                                    lonCentralMeridien,
                                                    d1stParallel,
                                                    d2ndParallel,
                                                    dFalseEasting,
                                                    dFalseNorthing );
				geotrans_engine.JNISetLambertConformalConic2Params( iMode, iDirection, params );
				break;
			}
		}
		} catch( GeotransError e ) {
            // jniStrtoval.setEngineError(true, e.getMessage());
        } catch(JNIException e) {
            // jniStrtoval.setJNIError(true, e.getMessage());
        }
	}

	public void setEngineParams_Cartesian( int iMode, int iDirection,  int iDatum, int iProjectionType,
    		double latOrigin,
    		double lonOrigin,
    		double dHeight,
    		double dOrientation
	){
		try {
		switch(iProjectionType){
			case CoordinateTypes.LOCCART:
			{
				LocalCartesian params = new LocalCartesian( latOrigin,
                                                    lonOrigin,
                                                    dHeight,
                                                    dOrientation );
				geotrans_engine.JNISetLocalCartesianParams( iMode, iDirection, params );
				break;
			}
		}
		} catch( GeotransError e ) {
            // jniStrtoval.setEngineError(true, e.getMessage());
        } catch(JNIException e) {
            // jniStrtoval.setJNIError(true, e.getMessage());
        }

	}

//	public void setEngineParams_Neys( int iMode, int iDirection,  int iDatum, int iProjectionType,
//    		double dOriginLatitude,
//    		double dCentralMeridien,
//    		double dFalseEasting,
//    		double dFalseNorthing
//	){
//		try {
//			int iStandardParallel = 71; // can also be 74, user has to choose
//			Neys params = new Neys( dOriginLatitude,
//                                    dCentralMeridien,
//                                    iStandardParallel * PI / 180,
//                                    dFalseEasting,
//                                    dFalseNorthing );
//			geotrans_engine.JNISetNeysParams( iMode, iDirection, params );
//		} catch( GeotransError e ) {
//            // jniStrtoval.setEngineError(true, e.getMessage());
//        } catch(JNIException e) {
//            // jniStrtoval.setJNIError(true, e.getMessage());
//        }
//
//	}
	
	public void setEngine_ObliqueMercator( int iMode, int iDirection,  int iDatum, int iProjectionType,
    		double latOrigin,
    		double dLatitude1,
    		double dLongitude1,
    		double dLatitude2,
    		double dLongitude2,
    		double dScaleFactor,
    		double dFalseEasting,
    		double dFalseNorthing
	){
		try {
		switch(iProjectionType){
			case CoordinateTypes.LOCCART:
			{
				ObliqueMercator params = new ObliqueMercator(
						                            latOrigin,
                                                    dLatitude1,
                                                    dLongitude1,
                                                    dLatitude2,
                                                    dLongitude2,
                                                    dScaleFactor,
                                                    dFalseEasting,
                                                    dFalseNorthing );
				geotrans_engine.JNISetObliqueMercatorParams( iMode, iDirection, params );
				break;
			}
		}
		} catch( GeotransError e ) {
            // jniStrtoval.setEngineError(true, e.getMessage());
        } catch(JNIException e) {
            // jniStrtoval.setJNIError(true, e.getMessage());
        }
	}

	public void setEngine_UTM( int iMode, int iDirection,  int iDatum, int iProjectionType,
    		long lZone,
    		long lOverride
	){
		try {
			UTM UTMParams = new UTM( lZone, lOverride );
			geotrans_engine.JNISetUTMParams( iMode, iDirection, UTMParams );
		} catch( GeotransError e ) {
            // jniStrtoval.setEngineError(true, e.getMessage());
        } catch(JNIException e) {
            // jniStrtoval.setJNIError(true, e.getMessage());
        }
	}

	public void setInputCoords_MGRS( int state, int direction, String sCoordinates_MGRS )
	{
		try {                    
			MGRS coordinates;
			int length = sCoordinates_MGRS.length();
			if ((sCoordinates_MGRS.charAt(length - 1) == ('0')) && (sCoordinates_MGRS.charAt(length - 2) == ('0')))
                        coordinates = new MGRS(sCoordinates_MGRS.substring(0, length - 2));
                    else
                        coordinates = new MGRS(sCoordinates_MGRS);
			geotrans_engine.JNISetMGRSCoordinates(state, direction, coordinates);
		} catch(GeotransError e) {
            //
		} catch(JNIException e) {
			// 
        }
	}

	public void setInputCoords_Geodetic( int state, int direction, double dLongitude, double dLatitude, double dHeight )
	{
		try {
	        Geodetic coordinates = new Geodetic( dLongitude, dLatitude, dHeight );
	        geotrans_engine.JNISetGeodeticCoordinates( state, direction, coordinates );
		} catch(GeotransError e) {
            //
		} catch(JNIException e) {
			// 
        }
	}

	public void setInputCoords_String( int state, int direction, int iProjectionType, String sCoordinates )
	{
		sCoordinates = sCoordinates.trim();
		try {
			switch( iProjectionType ){
				case CoordinateTypes.BNG:
				{
					BNG coordinates = new BNG( sCoordinates );
					geotrans_engine.JNISetBNGCoordinates(state, direction, coordinates);
					break;
				}
                case CoordinateTypes.GARS:
                {
                    GARS GARSCoords = new GARS( sCoordinates );
                    geotrans_engine.JNISetGARSCoordinates(state, direction, GARSCoords);
                    break;
                }
                case CoordinateTypes.GEOREF:
                {
                    GEOREF GEOREFCoords = new GEOREF( sCoordinates );
                    geotrans_engine.JNISetGEOREFCoordinates(state, direction, GEOREFCoords);

                    break;
                }
                case CoordinateTypes.USNG:
                {
                    USNG USNGCoords = new USNG( sCoordinates );
                    geotrans_engine.JNISetUSNGCoordinates( state, direction, USNGCoords );
                    break;
                }
                default:
                	System.err.println("invalid method for coordinate type String");
			}
		} catch(GeotransError e) {
            //
		} catch(JNIException e) {
			// 
        }
	}

	public void setInputCoords_Zoned( int state, int direction, int iProjectionType, double dEasting, double dNorthing, int iZone, char cHemisphere )
	{
		try {
			switch( iProjectionType ){
                case CoordinateTypes.UPS:
                {
                    UPS UPSCoords = new UPS( dEasting,
                                             dNorthing,
                                             cHemisphere );
                    geotrans_engine.JNISetUPSCoordinates(state, direction, UPSCoords);

                    break;
                }
                case CoordinateTypes.UTM:
                {
                    UTM UTMCoords = new UTM( dEasting,
                                             dNorthing , iZone, cHemisphere);
                    geotrans_engine.JNISetUTMCoordinates(state, direction, UTMCoords);
                    break;
                }
                default:
                	System.err.println("invalid method for coordinate type zoned");
			}
		} catch(GeotransError e) {
            //
		} catch(JNIException e) {
			// 
        }
	}
	
	public void setInputCoords( int state, int direction, int projType, double dEasting, double dNorthing )
	{
        try
        {
            switch(projType)
            {
                case CoordinateTypes.ALBERS:
                {
                    AlbersEqualAreaConic albersCoords = new AlbersEqualAreaConic( dEasting,
                                                                                  dNorthing );
                    geotrans_engine.JNISetAlbersEqualAreaConicCoordinates(state, direction, albersCoords);

                    break;
                }
                case CoordinateTypes.AZIMUTHAL:
                {
                    AzimuthalEquidistant azeqCoords = new AzimuthalEquidistant( dEasting,
                                                                                dNorthing );
                    geotrans_engine.JNISetAzimuthalEquidistantCoordinates(state, direction, azeqCoords);

                    break;
                }
                case CoordinateTypes.BNG:
                {
                	System.err.println("invalid method for setting BNG coordinates");
                    break;
                }
                case CoordinateTypes.BONNE:
                {
                    Bonne bonneCoords = new Bonne( dEasting,
                                                   dNorthing );
                    geotrans_engine.JNISetBonneCoordinates(state, direction, bonneCoords);

                    break;
                }
                case CoordinateTypes.CASSINI:
                {
                    Cassini cassiniCoords = new Cassini( dEasting,
                                                         dNorthing );
                   geotrans_engine.JNISetCassiniCoordinates(state, direction, cassiniCoords);

                    break;
                }
                case CoordinateTypes.CYLEQA:
                {
                    CylindricalEqualArea cyleqaCoords = new CylindricalEqualArea( dEasting,
                                                                                  dNorthing );
                    geotrans_engine.JNISetCylindricalEqualAreaCoordinates(state, direction, cyleqaCoords);

                    break;
                }
                case CoordinateTypes.ECKERT4:
                {
                    Eckert4 eckert4Coords = new Eckert4( dEasting,
                                                         dNorthing );
                    geotrans_engine.JNISetEckert4Coordinates(state, direction, eckert4Coords);

                    break;
                }
                case CoordinateTypes.ECKERT6:
                {
                    Eckert6 eckert6Coords = new Eckert6( dEasting,
                                                         dNorthing );
                    geotrans_engine.JNISetEckert6Coordinates(state, direction, eckert6Coords);

                    break;
                }
                case CoordinateTypes.EQDCYL:
                {
                    EquidistantCylindrical eqdcylCoords = new EquidistantCylindrical( dEasting,
                                                                                      dNorthing );
                    geotrans_engine.JNISetEquidistantCylindricalCoordinates(state, direction, eqdcylCoords);

                    break;
                }
                case CoordinateTypes.F16GRS:
                {
                	System.err.println("invalid method for setting MGRS coordinates");
                    break;
                }
                case CoordinateTypes.GARS:
                {
                	System.err.println("invalid method for setting MGRS coordinates");
                    break;
                }
                case CoordinateTypes.GEOCENTRIC:
                {
                	System.err.println("invalid method for setting geocentric coordinates");
                    break;
                }
                case CoordinateTypes.GEODETIC:
                {
                	System.err.println("invalid method for setting geocentric coordinates");
                    break;
                }
                case CoordinateTypes.GEOREF:
                {
                	System.err.println("invalid method for setting geocentric coordinates");
                    break;
                }
                case CoordinateTypes.GNOMONIC:
                {
                    Gnomonic gnomonicCoords = new Gnomonic( dEasting,
                                                            dNorthing );
                    geotrans_engine.JNISetGnomonicCoordinates(state, direction, gnomonicCoords);

                    break;
                }
                case CoordinateTypes.LAMBERT_1:
                {
                    LambertConformalConic1 lambert1Coords = new LambertConformalConic1( dEasting,
                                                                                        dNorthing );
                    geotrans_engine.JNISetLambertConformalConic1Coordinates(state, direction, lambert1Coords);

                    break;
                }
                case CoordinateTypes.LAMBERT_2:
                {
                    LambertConformalConic2 lambert2Coords = new LambertConformalConic2( dEasting,
                                                                                        dNorthing );
                     geotrans_engine.JNISetLambertConformalConic2Coordinates(state, direction, lambert2Coords);

                    break;
                }
                case CoordinateTypes.LOCCART:
                {
                	System.err.println("invalid method for setting local cartesian coordinates");
                    break;
                }
                case CoordinateTypes.MERCATOR:
                {
                    Mercator mercatorCoords = new Mercator( dEasting,
                                                            dNorthing );
                    geotrans_engine.JNISetMercatorCoordinates(state, direction, mercatorCoords);

                    break;
                }
                case CoordinateTypes.MGRS:
                {
                	System.err.println("invalid method for setting MGRS coordinates");
                    break;
                }
                case CoordinateTypes.MILLER:
                {
                    MillerCylindrical millerCoords = new MillerCylindrical( dEasting,
                                                                            dNorthing );
                    geotrans_engine.JNISetMillerCylindricalCoordinates(state, direction, millerCoords);

                    break;
                }
                case CoordinateTypes.MOLLWEIDE:
                {
                    Mollweide mollweidCoords = new Mollweide( dEasting,
                                                              dNorthing );
                    geotrans_engine.JNISetMollweideCoordinates(state, direction, mollweidCoords);

                    break;
                }
                case CoordinateTypes.NEYS:
                {
                    Neys neysCoords = new Neys( dEasting,
                                               dNorthing );
                    geotrans_engine.JNISetNeysCoordinates(state, direction, neysCoords);

                    break;
                }
                case CoordinateTypes.NZMG:
                {
                    NZMG NZMGCoords = new NZMG( dEasting,
                                                dNorthing );
                    geotrans_engine.JNISetNZMGCoordinates(state, direction, NZMGCoords);

                    break;
                }
                case CoordinateTypes.OMERC:
                {
                    ObliqueMercator omercCoords = new ObliqueMercator( dEasting,
                                                                       dNorthing );
                    geotrans_engine.JNISetObliqueMercatorCoordinates(state, direction, omercCoords);

                    break;
                }
                case CoordinateTypes.ORTHOGRAPHIC:
                {
                    Orthographic orthogrCoords = new Orthographic( dEasting,
                                                                   dNorthing );
                    geotrans_engine.JNISetOrthographicCoordinates(state, direction, orthogrCoords);

                    break;
                }
                case CoordinateTypes.POLARSTEREO:
                {
                    PolarStereographic polarstCoords = new PolarStereographic( dEasting,
                                                                               dNorthing );
                    geotrans_engine.JNISetPolarStereographicCoordinates(state, direction, polarstCoords);

                    break;
                }
                case CoordinateTypes.POLYCONIC:
                {
                    Polyconic polyconCoords = new Polyconic( dEasting,
                                                             dNorthing );
                    geotrans_engine.JNISetPolyconicCoordinates(state, direction, polyconCoords);

                    break;
                }
                case CoordinateTypes.SINUSOIDAL:
                {
                    Sinusoidal sinusoidCoords = new Sinusoidal( dEasting,
                                                                dNorthing );
                    geotrans_engine.JNISetSinusoidalCoordinates(state, direction, sinusoidCoords);

                    break;
                }
                case CoordinateTypes.STEREOGRAPHIC:
                {
                    Stereographic stereogrCoords = new Stereographic( dEasting,
                                                                      dNorthing );
                    geotrans_engine.JNISetStereographicCoordinates(state, direction, stereogrCoords);

                    break;
                }
                case CoordinateTypes.TRCYLEQA:
                {
                    TransverseCylindricalEqualArea trcyleqaCoords = new TransverseCylindricalEqualArea( dEasting,
                                                                                                        dNorthing );
                    geotrans_engine.JNISetTransverseCylindricalEqualAreaCoordinates(state, direction, trcyleqaCoords);

                    break;
                }
                case CoordinateTypes.TRANMERC:
                {
                    TransverseMercator tranmercCoords = new TransverseMercator( dEasting,
                                                                                dNorthing );
                    geotrans_engine.JNISetTransverseMercatorCoordinates(state, direction, tranmercCoords);

                    break;
                }
                case CoordinateTypes.GRINTEN:
                {
                    VanDerGrinten grintenCoords = new VanDerGrinten( dEasting,
                                                                     dNorthing /*, hemisphere*/);
                    geotrans_engine.JNISetVanDerGrintenCoordinates(state, direction, grintenCoords);

                    break;
                }
                case CoordinateTypes.UPS:
                {
                	System.err.println("invalid method for setting UPS coordinates");
                    break;
                }
                case CoordinateTypes.USNG:
                {
                	System.err.println("invalid method for setting USNG coordinates");
                    break;
                }
                case CoordinateTypes.UTM:
                {
                	System.err.println("invalid method for setting UTM coordinates");
                    break;
                }
                default:
                    break;
            }
        }
        catch(GeotransError e)
        {
            jniStrtoval.setEngineError(true, e.getMessage());
        }
        catch(JNIException e)
        {
            jniStrtoval.setJNIError(true, e.getMessage());
        }
    }

	
	
}

//class CoordinateTypes extends Object {
//
//    // Variable declaration for projection list.
//    // The list index is based on order. 
//    // These variables must be syncronized with any changes in the list in engine.h.
//    public final static int GEODETIC  = 0;        
//    public final static int GEOREF    = 1;        
//    public final static int GARS      = 2;        
//    public final static int GEOCENTRIC = 3;       
//    public final static int LOCCART   = 4;        
//    public final static int MGRS      = 5;        
//    public final static int USNG      = 6;        
//    public final static int UTM       = 7;        
//    public final static int UPS       = 8;        
//    public final static int ALBERS    = 9;        
//    public final static int AZIMUTHAL = 10;        
//    public final static int BNG       = 11;        
//    public final static int BONNE     = 12;       
//    public final static int CASSINI   = 13;       
//    public final static int CYLEQA    = 14;       
//    public final static int ECKERT4   = 15;       
//    public final static int ECKERT6   = 16;       
//    public final static int EQDCYL    = 17;       
//    public final static int GNOMONIC  = 18;       
//    public final static int LAMBERT_1 = 19;       
//    public final static int LAMBERT_2 = 20;       
//    public final static int MERCATOR  = 21;       
//    public final static int MILLER    = 22;       
//    public final static int MOLLWEIDE = 23;       
//    public final static int NEYS      = 24;       
//    public final static int NZMG      = 25;       
//    public final static int OMERC     = 26;       
//    public final static int ORTHOGRAPHIC = 27;    
//    public final static int POLARSTEREO = 28;     
//    public final static int POLYCONIC = 29;       
//    public final static int SINUSOIDAL = 30;      
//    public final static int STEREOGRAPHIC = 31;    
//    public final static int TRCYLEQA  = 32;      
//    public final static int TRANMERC  = 33;       
//    public final static int GRINTEN   = 34;       
//    public final static int F16GRS   = 35;       
//        
//}


class PerspectiveView {

//point in space:  x, y, z
//camera plane position: Cx, Cy, Cz
//camera plane rotation: Rx, Ry, Rz
//eye position: Ex, Ey, d               - relative to camera
//
//distance from camera plane to viewer = Cd
//
//point translated to camera: Tx, Ty, Tz
//
//Tx = cos Ry * (sin Rz * (x-Cx) + cos Rz * (x-Cx)) - sin Ry * (z-Cz)
//Ty = sin Rx * (cos Ry * (z-Cz) + sin Ry * (sin Rz * (y-Cy) + cos Rz * (x-Cx))) + cos Rx * (cos Rz * (y-Cy) - sin Rz * (x-Cx))
//Tz = cos Rx * (cos Ry * (z-Cz) + sin Ry * (sin Rz * (y-Cy) + cos rz * (x-Cx))) - sin Rx * (cos Rz * (y-Cy) - sin Rz * (x-Cx))
//
//u = (Tx - Ex)(Cd/Tz)
//v = (Ty - Ey)(Cd/Tz)
//
//field of view = 2 * arctan( 1/d )
//
//distance from eye to screen: s
//distance from eye/camera to point: d
//
//display x = u * s / d
//display y = v * s / d

}

	