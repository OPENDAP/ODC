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
}
