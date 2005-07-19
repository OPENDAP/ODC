package opendap.clients.odc.plot;

// everything is in micro degrees
// longitude is 0 to 360
// latitude is -90 to 90
public class GeoReference {
	public static final int TYPE_IntegerG0 = 1; // greenwich is at 0
	public static final int TYPE_FloatG0 = 2; // greenwich is at 0
	public static final int TYPE_MicroG0 = 3; // greenwich is at 0
	private boolean mzLinear;
	private boolean mzReversed_longitude;
	private boolean mzReversed_latitude;
	private int miLongitude_West, miLongitude_East;
	private int miLatitude_South, miLatitude_North;
	private int[] maiLongitudes_Micro1;
	private int[] maiLatitudes_Micro1;
	private int mBound_North;
	private int mBound_East;
	private int mBound_South;
	private int mBound_West;
	private int mctLongitudes;
	private int mctLatitudes;

	public GeoReference(){}

	public boolean zInitialize( double[] adLongitudes1, double[] adLatitudes1, int eTYPE, StringBuffer sbError ){
		mctLongitudes = adLongitudes1.length - 1;
		mctLatitudes = adLatitudes1.length - 1;
		maiLongitudes_Micro1 = new int[mctLongitudes + 1];
		maiLatitudes_Micro1 = new int[mctLatitudes + 1];
		if( mctLongitudes < 3 || mctLatitudes < 3 ){
			sbError.append("less than three longitude/latitudes");
			return false;
		}
		if( adLongitudes1[1] == adLongitudes1[mctLongitudes] || adLatitudes1[1] == adLatitudes1[mctLatitudes] ){
			sbError.append("longitude/latitude ranges are same at beginning and end");
			return false;
		}

		// convert to standard microdegrees
		double dLongitude_Minimum = 1000;
		double dLongitude_Maximum = -1000;
		for( int xLongitude = 1; xLongitude <= mctLongitudes; xLongitude++ ){
			double dblLongitude = adLongitudes1[xLongitude];
			if( dblLongitude < dLongitude_Minimum ) dLongitude_Minimum = dblLongitude;
			if( dblLongitude > dLongitude_Maximum ) dLongitude_Maximum = dblLongitude;
			if( dblLongitude < -180d || dblLongitude > 360d ){
				sbError.append("invalid longitude: " + dblLongitude);
				return false;
			}
			if( dblLongitude < 0 ) dblLongitude += 360d;
			maiLongitudes_Micro1[xLongitude] = (int)(dblLongitude * 1000000);
		}
		if( dLongitude_Maximum - dLongitude_Minimum > 360d ){
			sbError.append("longitude range is greater than 360");
			return false;
		}

		double dLatitude_Minimum = 1000;
		double dLatitude_Maximum = -1000;
		for( int xLatitude = 1; xLatitude <= mctLatitudes; xLatitude++ ){
			double dblLatitude = adLatitudes1[xLatitude];
			if( dblLatitude < dLatitude_Minimum ) dLatitude_Minimum = dblLatitude;
			if( dblLatitude > dLatitude_Maximum ) dLatitude_Maximum = dblLatitude;
			if( dblLatitude < -90d || dblLatitude > 90d ){
				sbError.append("invalid Latitude: " + dblLatitude);
				return false;
			}
			maiLatitudes_Micro1[xLatitude] = (int)(dblLatitude * 1000000);
		}

		// validate continuity
		boolean zAscending_Current = maiLongitudes_Micro1[2] > maiLongitudes_Micro1[1];
		boolean zAscending_Previous = zAscending_Current;
		mzReversed_longitude = !zAscending_Current;
		int xWrap_longitude = 0;
		double dIncrement = maiLongitudes_Micro1[2] - maiLongitudes_Micro1[1];
		if( dIncrement == 0 ){
			sbError.append("first two longitudes are duplicates");
			return false;
		}
		for( int xLongitude = 3; xLongitude <= mctLongitudes; xLongitude++ ){
			if( maiLongitudes_Micro1[xLongitude] == maiLongitudes_Micro1[xLongitude - 1] ){
				sbError.append("longitudes " + (xLongitude - 1) + " and " + xLongitude + " are duplicates");
				return false;
			}
			zAscending_Current = maiLongitudes_Micro1[xLongitude] > maiLongitudes_Micro1[xLongitude - 1];
			if( zAscending_Current != zAscending_Previous ){
				if( xWrap_longitude > 0 ){
					sbError.append("longitudes are discontinuous at " + xLongitude);
					return false;
				} else xWrap_longitude = xLongitude;
			}
		}

		zAscending_Current = maiLatitudes_Micro1[2] > maiLatitudes_Micro1[1];
		zAscending_Previous = zAscending_Current;
		mzReversed_latitude = !zAscending_Current;
		int xWrap_Latitude = 0;
		dIncrement = maiLatitudes_Micro1[2] - maiLatitudes_Micro1[1];
		if( dIncrement == 0 ){
			sbError.append("first two Latitudes are duplicates");
			return false;
		}
		for( int xLatitude = 3; xLatitude <= mctLatitudes; xLatitude++ ){
			if( maiLatitudes_Micro1[xLatitude] == maiLatitudes_Micro1[xLatitude - 1] ){
				sbError.append("Latitudes " + (xLatitude - 1) + " and " + xLatitude + " are duplicates");
				return false;
			}
			zAscending_Current = maiLatitudes_Micro1[xLatitude] > maiLatitudes_Micro1[xLatitude - 1];
			if( zAscending_Current != zAscending_Previous ){
				if( xWrap_Latitude > 0 ){
					sbError.append("Latitudes are discontinuous at " + xLatitude);
					return false;
				} else xWrap_Latitude = xLatitude;
			}
		}

		// find bounds
		if( mzReversed_longitude ){
			mBound_West = maiLongitudes_Micro1[mctLongitudes];
			mBound_East = maiLongitudes_Micro1[1];
		} else {
			mBound_West = maiLongitudes_Micro1[1];
			mBound_East = maiLongitudes_Micro1[mctLongitudes];
		}
		if( mzReversed_latitude ){
			mBound_South = maiLatitudes_Micro1[mctLatitudes];
			mBound_North = maiLatitudes_Micro1[1];
		} else {
			mBound_South = maiLatitudes_Micro1[1];
			mBound_North = maiLatitudes_Micro1[mctLatitudes];
		}

		return true;
	}
//	public boolean zInitialize( int[] aiLongitudes1, int[] aiLatitudes1, int eTYPE, StringBuffer sbError ){
//		maiLongitudes_Micro1 = new int[aiLongitudes1.length];
//		maiLatitudes_Micro1 = new int[aiLatitudes1.length];
//		System.arraycopy(aiLongitudes1, 0, maiLongitudes_Micro1, 0, aiLongitudes1.length);
//		System.arraycopy(aiLatitudes1, 0, maiLatitudes_Micro1, 0, aiLatitudes1.length);
// assume integer todo
//		if( eTYPE == TYPE_IntegerG0 ){
//			for( int xLongitude = 1; xLongitude < aiLongitudes1.length; xLongitude++ ){
//				maiLongitudes_Micro1[xLongitude] *= 1000000;
//			}
//			for( int xLatitude = 1; xLatitude < aiLatitudes1.length; xLatitude++ ){
//				maiLatitudes_Micro1[xLatitude] *= 1000000;
//			}
//		}
//		return true;
//	}
	public int getBound_North(){ return mBound_North; }
	public int getBound_East(){ return mBound_East; }
	public int getBound_South(){ return mBound_South; }
	public int getBound_West(){ return mBound_West; }
	public int getLongitudeCount(){ return mctLongitudes; }
	public int getLatitudeCount(){ return mctLatitudes; }
	public boolean getReversed_Longitude(){ return mzReversed_longitude; }
	public boolean getReversed_Latitude(){ return mzReversed_latitude; }

	public int[] getLongitudeMapping1_Micro(int ctPixels){
		if( maiLongitudes_Micro1 == null ) return null;
		int ctLongitudes = maiLongitudes_Micro1.length - 1;
		if( ctPixels == ctLongitudes ) return maiLongitudes_Micro1;
		int[] aiLongitudeMapping_Micro1 = new int[ctPixels + 1];
		aiLongitudeMapping_Micro1[1] = maiLongitudes_Micro1[1]; // the first pixel always maps to first longitude
		if( ctLongitudes >= ctPixels ){
			for( int xPixel = 2; xPixel < ctPixels; xPixel++ ){
				int xLongitude = 1 + xPixel * (ctLongitudes - 1) / ctPixels;
				aiLongitudeMapping_Micro1[xPixel] = maiLongitudes_Micro1[xLongitude];
			}
		} else {
			int iLongitudeRange = ( maiLongitudes_Micro1[1] < maiLongitudes_Micro1[ctLongitudes] ) ? maiLongitudes_Micro1[ctLongitudes] - maiLongitudes_Micro1[1] : maiLongitudes_Micro1[ctLongitudes] + 360 - maiLongitudes_Micro1[1];
			int iMicrodegreesPerPixel = iLongitudeRange / ctPixels;
			for( int xPixel = 2; xPixel < ctPixels; xPixel++ ){
				aiLongitudeMapping_Micro1[xPixel] = maiLongitudes_Micro1[1] + iMicrodegreesPerPixel * (xPixel - 1);
				if( aiLongitudeMapping_Micro1[xPixel] > 360 ) aiLongitudeMapping_Micro1[xPixel] -= 360;
			}
		}
		aiLongitudeMapping_Micro1[ctPixels] = maiLongitudes_Micro1[ctLongitudes]; // the last pixel always maps to last longitude
		return aiLongitudeMapping_Micro1;
	}
	public int[] getLatitudeMapping1_Micro(int ctPixels){
		if( maiLatitudes_Micro1 == null ) return null;
		int ctLatitudes = maiLatitudes_Micro1.length - 1;
		if( ctPixels == ctLatitudes ) return maiLatitudes_Micro1;
		int[] aiLatitudeMapping_Micro1 = new int[ctPixels + 1];
		aiLatitudeMapping_Micro1[1] = maiLatitudes_Micro1[1]; // the first pixel always maps to first Latitude
		if( ctLatitudes >= ctPixels ){
			for( int xPixel = 2; xPixel < ctPixels; xPixel++ ){
				int xLatitude = 1 + xPixel * (ctLatitudes - 1) / ctPixels;
				aiLatitudeMapping_Micro1[xPixel] = maiLatitudes_Micro1[xLatitude];
			}
		} else {
			int iLatitudeRange = ( maiLatitudes_Micro1[1] < maiLatitudes_Micro1[ctLatitudes] ) ? maiLatitudes_Micro1[ctLatitudes] - maiLatitudes_Micro1[1] : maiLatitudes_Micro1[ctLatitudes] + 360 - maiLatitudes_Micro1[1];
			int iMicrodegreesPerPixel = iLatitudeRange / ctPixels;
			for( int xPixel = 2; xPixel < ctPixels; xPixel++ ){
				aiLatitudeMapping_Micro1[xPixel] = maiLatitudes_Micro1[1] + iMicrodegreesPerPixel * (xPixel - 1);
				if( aiLatitudeMapping_Micro1[xPixel] > 360 ) aiLatitudeMapping_Micro1[xPixel] -= 360;
			}
		}
		aiLatitudeMapping_Micro1[ctPixels] = maiLatitudes_Micro1[ctLatitudes]; // the last pixel always maps to last Latitude
		return aiLatitudeMapping_Micro1;
	}
}