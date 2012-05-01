package opendap.clients.odc;

public class Utility_Numeric {
	final public static java.util.Random random = new java.util.Random(); 
	
	final public static String floatToString_Rounded( Float f ){
		if( Math.abs( f - Math.round( f ) ) < 0.000001 ) return Integer.toString( Math.round( f ) );
		return Float.toString( f );
	}
	final public static String doubleToString_Rounded( Double f ){
		if( Math.abs( f - Math.round( f ) ) < 0.000000000000001 ) return Long.toString( Math.round( f ) );
		return Double.toString( f );
	}
	final public static int max( int ... numbers ){
		int iMax = numbers[0];
		for( int xArg = 1; xArg < numbers.length; xArg++ )
			if( numbers[xArg] > iMax ) iMax = numbers[xArg];
		return iMax;
	}	
	final public static int min( int ... numbers ){
		int iMin = numbers[0];
		for( int xArg = 1; xArg < numbers.length; xArg++ )
			if( numbers[xArg] < iMin ) iMin = numbers[xArg];
		return iMin;
	}
	final public static int power10( int iValue, int iExponent ){
		for( int xExponent = 1; xExponent <= iExponent; xExponent++ ) iValue *= 10;
		return iValue;
	}
	final public static long power10_toLong( int iValue, int iExponent ){
		long lValue = iValue;
		for( int xExponent = 1; xExponent <= iExponent; xExponent++ ) lValue *= 10;
		return lValue;
	}

	/** generation of a Gaussian deviates (variants) by the ratio of uniform method */
	final public static double generateNormalDeviate( final double mean, final double std_deviation ){
		double u, v, x, y, q;
		do {
			u = random.nextDouble();
			v = 1.7156 * ( random.nextDouble() - 0.5 );
			x = u - 0.449871;
			y = (v < 0 ? v * -1 : v) + 0.386595;
			q = x*x + y * (0.19600 * y - 0.25472 * x);
		} while( q > 0.27597 &&
				(q > 0.27846 || v*v > -4d * Math.log(u) * u*u));
        return mean + std_deviation * v / u;
	}
	
}
