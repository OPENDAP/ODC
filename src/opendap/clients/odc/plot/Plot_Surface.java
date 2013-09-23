package opendap.clients.odc.plot;

/**
 * Title:        Panel_Plot_Surface
 * Description:  Plots surfaces
 * Copyright:    Copyright (c) 2010-2012
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.08
 */

import opendap.clients.odc.Utility_Geometry;

import java.util.ArrayList;

class Plot_Surface extends Plot {

	public enum SURFACE_TYPE {
		Line,
		Mesh,
		Shaded
	};

	public boolean draw( StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}

	private Plot_Surface( PlotEnvironment environment, PlotLayout layout ){
		super( environment, layout );		
	}

	public static Plot_Surface create( PlotEnvironment environment, PlotLayout layout ){
		Plot_Surface plot = new Plot_Surface( environment, layout );
		return plot;
	}
	
	public String getDescriptor(){ return "S"; }

	public boolean render( int pxPlotWidth, int pxPlotHeight, StringBuffer sbError ){

		int iRangeDivisions = 100;
		int iAngularDivisions = 50;
		float fFieldOfView_degrees = 40f;
		float fFieldOfView_radians = fFieldOfView_degrees * (float)Math.PI / 180f;

		// define objects
		int iCountObjects = 3;
		int[][] aiObjectLocations = new int[iCountObjects + 1][4];
		int[] aiObjectLocation_1 = { 0, 50, 100, 40 }; // range, angle, value
		int[] aiObjectLocation_2 = { 0, 175, 150, 60 }; // range, angle, value
		int[] aiObjectLocation_3 = { 0, 150, 300, 50 }; // range, angle, value
		aiObjectLocations[1] = aiObjectLocation_1;
		aiObjectLocations[2] = aiObjectLocation_2;
		aiObjectLocations[3] = aiObjectLocation_3;

		// generate data in the scan space
		java.util.Random random = new java.util.Random();
		float[][] afCellValue = new float[iAngularDivisions + 1][iRangeDivisions + 1]; 
		float[][] afCell_x = new float[iAngularDivisions + 1][iRangeDivisions + 1]; 
		float[][] afCell_y = new float[iAngularDivisions + 1][iRangeDivisions + 1]; 
		for( int xAngle = 1; xAngle <= iAngularDivisions; xAngle++ ){
			double dAngle_base = (float)(xAngle - 1)/iAngularDivisions * fFieldOfView_radians;
			double dAngle = dAngle_base - Math.PI / 2;
			for( int xRange = 1; xRange <= iRangeDivisions; xRange++ ){
				if( xRange == 1 || xRange == iRangeDivisions ){
					afCellValue[xAngle][xRange] = 0;
				}
				afCell_x[xAngle][xRange] = (float) (xRange * Math.cos( dAngle ));
				afCell_y[xAngle][xRange] = (float) (xRange * Math.sin( dAngle ));
				afCellValue[xAngle][xRange] = random.nextFloat() * 10.0f;
				for( int xObject = 1; xObject <= iCountObjects; xObject++ ){
					int x1 = aiObjectLocations[xObject][1];
					int y1 = aiObjectLocations[xObject][2];
					int x2 = (int)afCell_x[xAngle][xRange];
					int y2 = (int)afCell_y[xAngle][xRange];
//					System.out.format( "%d %d %d %d\n", x1, y1, x2, y2 );
					if( Math.abs( x1 - x2 ) < 3  && Math.abs( y1 - y2 ) < 100 ){
						double distance = Utility_Geometry.distanceTwoPoints(x1, y1, x2, y2);
						afCellValue[xAngle][xRange] = (float)aiObjectLocations[xObject][3] * (float)((1.0d - distance/20.0d) * (1.0d - distance/20.0d)); 
					}
				}
//if( xRange < 10 && dAngle < 10 ) System.out.format( "%f %f %f\n", afCell_x[xRange][xAngle], afCell_y[xRange][xAngle], afCellValue[xRange][xAngle] ); 
			}
		}

		// translate to screen coordinates
//		int iXAxisOffset = 600;
//		int iYAxisSize = 1000;
//		float fYAxisOffset = iYAxisSize / 2;
//		float fScale = 5;
		
		// make lines
		int iRGBA = 0; // black ? TODO
		ArrayList<Line> listLines = new ArrayList<Line>();
		listLines.add( Line.create( "x", 0, 0, 0, 500, 0, 0, iRGBA ) );  // x-axis 
		listLines.add( Line.create( "y", 0, 0, 0, 0, 500, 0, iRGBA ) );  // y-axis 
		listLines.add( Line.create( "z", 0, 0, 0, 0, 0, 500, iRGBA ) );  // z-axis 
		
		// projection parameters
		double dScaleX = 1;
		double dScaleY = 1;
		double dScaleZ = 1;
		double dAngleDegrees = 30;
		double alpha = dAngleDegrees * Math.PI / 180.0d;
		double beta = dAngleDegrees * Math.PI / 180.0d;
		double calpha = Math.cos( alpha ) * dScaleX;
		double salpha = Math.sin( alpha ) * dScaleX;
		double cbeta  = Math.cos( beta ) * dScaleY;
		double sbeta  = Math.sin( beta ) * dScaleY;

		// project points onto 2D surface
		int[][] aiX = new int[iAngularDivisions][iRangeDivisions];
		int[][] aiY = new int[iAngularDivisions][iRangeDivisions];
		int[][] aiY_fill = new int[iAngularDivisions][iRangeDivisions];
		for( int xAngle = 1; xAngle <= iAngularDivisions; xAngle++ ){
			for( int xRange = 1; xRange <= iRangeDivisions; xRange++ ){
				aiX[xAngle - 1][xRange - 1] = (int)(calpha * afCell_x[xAngle][xRange] - cbeta * afCell_y[xAngle][xRange]);
				aiY[xAngle - 1][xRange - 1] = 1000 - (int)(salpha * afCell_x[xAngle][xRange] + sbeta * afCell_y[xAngle][xRange] + dScaleZ * afCellValue[xAngle][xRange]);
				aiY_fill[xAngle - 1][xRange - 1] = aiY[xAngle - 1][xRange - 1] + 5;
			}
		}

		System.out.format( "value %d\n", (int)(0.5d * 20) ); 		
		
		// project lines onto 2D surface
		ArrayList<Line> listProjectedLines = new ArrayList<Line>();
		for( Line line : listLines ){
			int x1 = (int)( calpha * line.x1 - cbeta * line.y1 );
			int y1 = (int)( salpha * line.x1 + sbeta * line.y1 + dScaleZ * line.z1 );
			int x2 = (int)( calpha * line.x2 - cbeta * line.y2 );
			int y2 = (int)( salpha * line.x2 + sbeta * line.y2 + dScaleZ * line.z2 );
			listProjectedLines.add( Line.create( line.sLabel, x1, y1, 0, x2, y2, 0, line.iRGBA ) ); 
		}
		
		// draw lines
		for( Line line : listProjectedLines ){
			Utility_Geometry.drawLineToRaster( raster, pxPlotWidth, pxPlotHeight, line.x1, line.y1, line.x2, line.y2, line.iRGBA );
			// System.out.format( "drew line %s %d %d %d %d\n", line.sLabel, line.x1, line.y1, line.x2, line.y2 ); 
		}
		
		// draw points
//		for( int xAngle = 1; xAngle <= iAngularDivisions; xAngle++ ){
		/*
		for( int xAngle = iAngularDivisions; xAngle >= 1; xAngle-- ){
			g2.setColor( Color.white );
			g2.fillPolygon( aiX[xAngle - 1], aiY_fill[xAngle - 1], iRangeDivisions );
			g2.setColor( Color.black );
			g2.drawPolyline( aiX[xAngle - 1], aiY[xAngle - 1], iRangeDivisions );
		}
		*/
		
		return true;
	}

	public boolean zCreateRGBArray(int pxWidth, int pxHeight, boolean zAveraged, StringBuffer sbError){
		sbError.append("internal error, rgb array creation not applicable to plot line");
		return false;
	}

}

class Line {
	String sLabel;
	int x1;
	int x2;
	int y1;
	int y2;
	int z1;
	int z2;
	int iRGBA;
	public static Line create( String sLabel, int x1, int y1, int z1, int x2, int y2, int z2, int iRGBA ){
		Line new_line = new Line();
		new_line.sLabel = sLabel;
		new_line.x1 = x1;
		new_line.y1 = y1;
		new_line.z1 = z1;
		new_line.y2 = y2;
		new_line.x2 = x2;
		new_line.z2 = z2;
		return new_line;
	}
}

