package opendap.clients.odc;

public class Utility_Numeric {
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
}
