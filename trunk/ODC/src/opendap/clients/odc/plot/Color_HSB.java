package opendap.clients.odc.plot;

/** functions which manage Hue-Saturation-Brightness (HSB) color system */

public class Color_HSB {
    public static int iHSBtoRGBA( int iHSB ) {
		int iAlpha      = (int)((iHSB & 0xFF000000L) >> 24);
		int iHue        = (iHSB & 0x00FF0000) >> 16;
		int iSaturation = (iHSB & 0x0000FF00) >> 8;
		int iBrightness =  iHSB & 0x000000FF;
		return iHSBtoRGBA( iAlpha, iHue, iSaturation, iBrightness );
	}

    public static int iHSBtoRGB( int iHSB ) {
		int iHue        = (iHSB & 0x00FF0000) >> 16;
		int iSaturation = (iHSB & 0x0000FF00) >> 8;
		int iBrightness =  iHSB & 0x000000FF;
		return iHSBtoRGBA( iHSB, iHue, iSaturation, iBrightness );
	}

    public static int iHSBtoRGBA(int iAlpha, int ihue, int isaturation, int ibrightness) {
		float hue = (float)ihue/255;
		float saturation = (float)isaturation/255;
		float brightness = (float)ibrightness/255;
		int r = 0, g = 0, b = 0;
    	if (saturation == 0) {
			r = g = b = (int) (brightness * 255.0f + 0.5f);
		} else {
			float h = (hue - (float)Math.floor(hue)) * 6.0f;
			float f = h - (float)java.lang.Math.floor(h);
			float p = brightness * (1.0f - saturation);
			float q = brightness * (1.0f - saturation * f);
			float t = brightness * (1.0f - (saturation * (1.0f - f)));
			switch ((int) h) {
				case 0:
					r = (int) (brightness * 255.0f + 0.5f);
					g = (int) (t * 255.0f + 0.5f);
					b = (int) (p * 255.0f + 0.5f);
					break;
				case 1:
					r = (int) (q * 255.0f + 0.5f);
					g = (int) (brightness * 255.0f + 0.5f);
					b = (int) (p * 255.0f + 0.5f);
					break;
				case 2:
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (brightness * 255.0f + 0.5f);
					b = (int) (t * 255.0f + 0.5f);
					break;
				case 3:
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (q * 255.0f + 0.5f);
					b = (int) (brightness * 255.0f + 0.5f);
					break;
				case 4:
					r = (int) (t * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					b = (int) (brightness * 255.0f + 0.5f);
					break;
				case 5:
					r = (int) (brightness * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					b = (int) (q * 255.0f + 0.5f);
					break;
			}
		}
		return (iAlpha << 24) | (r << 16) | (g << 8) | (b << 0);
    }

	public static int iRGBtoHSB( int rgb ){
		int iAlpha      = (int)((rgb & 0xFF000000L) >> 24);
		int iRed        = (rgb & 0x00FF0000) >> 16;
		int iGreen      = (rgb & 0x0000FF00) >> 8;
		int iBlue       =  rgb & 0x000000FF;
		return iRGBtoHSB(iAlpha, iRed, iGreen, iBlue);
	}

	// could be wrong check carefully by doing inversions
	public static int iRGBtoHSB(int alpha, int r, int g, int b) {
		int hue, saturation, brightness;
		int iLargestComponent = (r > g) ? r : g;
		if (b > iLargestComponent) iLargestComponent = b;
		int iSmallestComponent = (r < g) ? r : g;
		if (b < iSmallestComponent) iSmallestComponent = b;
		brightness = iLargestComponent;
		if (iLargestComponent == 0){
	        saturation = 0;
		    hue = 0;
		} else {
			float huec;
			float fSpread = (float) (iLargestComponent - iSmallestComponent);
			saturation = (int)(255 * fSpread / iLargestComponent);
			float redc = ((float) (iLargestComponent - r)) / fSpread;
			float greenc = ((float) (iLargestComponent - g)) / fSpread;
			float bluec = ((float) (iLargestComponent - b)) / fSpread;
			if (r == iLargestComponent)
			    huec = bluec - greenc;
		    else if (g == iLargestComponent)
			    huec = 2.0f + redc - bluec;
		    else
			    huec = 4.0f + greenc - redc;
		    huec = huec / 6.0f;
		    if (huec < 0)
			huec = huec + 1.0f;
			hue = (int)(huec * 255f);
		}
		return (alpha << 24) & ( hue << 16 ) & ( saturation << 8 ) & brightness;
    }
}
