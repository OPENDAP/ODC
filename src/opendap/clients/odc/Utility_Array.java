package opendap.clients.odc;

public class Utility_Array {
	public static boolean arrayHasMember( int[] ai, int value ){
		for( int x = 0; x < ai.length; x++ ) if( ai[x] == value ) return true;
		return false;
	}
	public static boolean arrayHasMember( String[] as, String s ){
		for( int x = 0; x < as.length; x++ ) if( as[x] == null ? s == null : as[x].equalsIgnoreCase( s ) ) return true;
		return false;
	}
	public static boolean arrayStartsWithMember( String[] as, String s ){
		for( int x = 0; x < as.length; x++ ) if( as[x] == null ? s == null : as[x].startsWith( s ) ) return true;
		return false;
	}
	public static String dumpArray( double[] ad, int from, int to ){
		if( ad == null ) return "null";
		if( to == 0 ) to = ad.length - 1;
		StringBuffer sb = new StringBuffer(80);
		for( int x = from; x <= to; x++ ){
			sb.append("[" + x + "] = " + ad[x] + "\n");
		}
		return sb.toString();
	}
	public static String dumpArray( String[] as, int from, int to ){
		if( as == null ) return "[null]";
		if( from < 0 ) from = 0;
		if( to == 0 || to > as.length - 1 ) to = as.length - 1;
		StringBuffer sb = new StringBuffer(80);
		for( int x = from; x <= to; x++ ){
			sb.append("[" + x + "] = " + as[x] + "\n");
		}
		return sb.toString();
	}
	public static String dumpArray( float[] af, int from, int to ){
		if( af == null ) return "null";
		if( to == 0 ) to = af.length - 1;
		StringBuffer sb = new StringBuffer(80);
		for( int x = from; x <= to; x++ ){
			sb.append("[" + x + "] = " + af[x] + "\n");
		}
		return sb.toString();
	}
	public static String sDumpMatrix( double[] ai, int x_len, int y_len, int x_to, int y_to ){
		int xValue = -1;
		StringBuffer sb = new StringBuffer(250);
		for( int xY = 1; xY <= y_len; xY++ ){
			if( xY > y_to ) break;
			for( int xX = 1; xX <= x_len; xX++ ){
				xValue++;
				if( xX > x_to ) continue;
				sb.append(' ');
				if( ai[xValue] < 10 ) sb.append(' ');
				sb.append(ai[xValue]);
			}
		    sb.append("\n");
		}
		return sb.toString();
	}

	public static String sDumpMatrix( int[] ai ){
		StringBuffer sb = new StringBuffer(250);
		int lenArray = ai.length;
		sb.append("array[" + lenArray + "]: ");
		for( int x = 0; x < lenArray; x++ ){
			sb.append(' ');
			sb.append(ai[x]);
		}
		return sb.toString();
	}
	public static String dumpArray( int[] ai, int from, int to ){
		if( ai == null ) return "[null]";
		if( to == 0 ) to = ai.length - 1;
		StringBuffer sb = new StringBuffer(80);
		for( int x = from; x <= to; x++ ){
			sb.append("[" + x + "] = " + ai[x] + "\n");
		}
		return sb.toString();
	}
	public static String dumpArray( int[] ai ){
		if( ai == null ) return "[null]";
		int to = ai.length - 1;
		int from = 0;
		return dumpArray( ai, from, to );
	}
	public static String dumpArray( int[][] ai, int from, int to ){
		if( ai == null ) return "[null]";
		if( to == 0 ) to = ai.length - 1;
		StringBuffer sb = new StringBuffer(80);
		for( int x = from; x <= to; x++ ){
			sb.append("[" + x + "] = \n" + dumpArray( ai[x], 0, 0, 1 ) + "\n");
		}
		return sb.toString();
	}
	public static String dumpArray( int[] ai, int from, int to, int indent ){
		String sIndent = Utility.sRepeatChar( '\t', indent );
		if( ai == null ) return sIndent + "[null]";
		if( to == 0 ) to = ai.length - 1;
		StringBuffer sb = new StringBuffer(80);
		for( int x = from; x <= to; x++ ){
			sb.append( sIndent ).append("[" + x + "] = " + ai[x] + "\n");
		}
		return sb.toString();
	}
	public static String dumpArray( float[][] af, int from, int to ){
		if( af == null ) return "[null]";
		if( to == 0 ) to = af.length - 1;
		StringBuffer sb = new StringBuffer(80);
		for( int x = from; x <= to; x++ ){
			sb.append("[" + x + "] = \n" + dumpArray( af[x], 0, 0, 1 ) + "\n");
		}
		return sb.toString();
	}
	public static String dumpArray( float[] af, int from, int to, int indent ){
		String sIndent = Utility.sRepeatChar( '\t', indent );
		if( af == null ) return sIndent + "[null]";
		if( to == 0 ) to = af.length - 1;
		StringBuffer sb = new StringBuffer(80);
		for( int x = from; x <= to; x++ ){
			sb.append( sIndent ).append("[" + x + "] = " + af[x] + "\n");
		}
		return sb.toString();
	}
	public static String dumpArray( double[][] ad){
		return dumpArray( ad, 0, 0 );
	}
	public static String dumpArray( double[][] ad, int from, int to ){
		if( ad == null ) return "[null]";
		if( to == 0 ) to = ad.length - 1;
		StringBuffer sb = new StringBuffer(80);
		for( int x = from; x <= to; x++ ){
			sb.append("[" + x + "] = \n" + dumpArray( ad[x], 0, 0, 1 ) + "\n");
		}
		return sb.toString();
	}
	public static String dumpArray( double[] ad, int from, int to, int indent ){
		String sIndent = Utility.sRepeatChar( '\t', indent );
		if( ad == null ) return sIndent + "[null]";
		if( to == 0 ) to = ad.length - 1;
		StringBuffer sb = new StringBuffer(80);
		for( int x = from; x <= to; x++ ){
			sb.append( sIndent ).append("[" + x + "] = " + ad[x] + "\n");
		}
		return sb.toString();
	}

}
