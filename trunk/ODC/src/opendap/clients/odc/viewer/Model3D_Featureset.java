package opendap.clients.odc.viewer;

import java.awt.Graphics;

public class Model3D_Featureset {
	protected int mctTimeslice = 0;
	public int getTimesliceCount(){
		return mctTimeslice;
	}
	public boolean zInitialize( StringBuffer sbError ){
		return true;
	}
	public void render(  Graphics g, int x_VP, int y_VP, int w_VP, int h_VP, float fScale, int iTimeslice ){
	}
}
