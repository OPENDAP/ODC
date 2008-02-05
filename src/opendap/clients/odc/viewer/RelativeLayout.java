package opendap.clients.odc.viewer;

/** Layout tool which allows user to specify the orientation of an object
 *  relative to a different object and be able to determine the draw 
 *  coordinates of the resulting subimage */

public class RelativeLayout {

	public RelativeLayout( RelativeLayoutInterface object, Orientation orientation, Orientation alignment, int iHorizontalOffset, int iVerticalOffset, int iRotation ){
		mObject = object;
		meOrientation = orientation;
		meAlignment = alignment;
		mpxOffsetHorizontal = iHorizontalOffset;
		mpxOffsetVertical = iVerticalOffset;
		miRotation = iRotation;
	}

	public enum Orientation {
		TopLeft,
		TopMiddle,
		TopRight,
		BottomLeft,
		BottomMiddle,
		BottomRight,
		LeftMiddle,
		RightMiddle,
		Center
	}

	private RelativeLayoutInterface mObject;
	private Orientation meOrientation = Orientation.TopLeft;
	private Orientation meAlignment = Orientation.TopLeft;
	private int mpxOffsetHorizontal = 0;
	private int mpxOffsetVertical = 0;
	private int miRotation = 0;

	public Orientation getOrientation(){ return meOrientation; }
	public Orientation getAlignment(){ return meAlignment; }
	public int getOffsetHorizontal(){ return mpxOffsetHorizontal; }
	public int getOffsetVertical(){ return mpxOffsetVertical; }
	public int getRotation(){ return miRotation; }

	public void setOrientation( Orientation e ){ meOrientation = e; }
	public void setAlignment( Orientation e ){ meAlignment = e; }
	public void setOffsetHorizontal( int i ){ mpxOffsetHorizontal = i; }
	public void setOffsetVertical( int i ){ mpxOffsetVertical = i; }
	public void setRotation( int i ){ miRotation = i; }

	java.awt.Point getLocation( int width, int height ){
		java.awt.Rectangle rectReferenceObject = mObject.getLayoutRect();
		int hLocation = (int)rectReferenceObject.getX();
		int vLocation = (int)rectReferenceObject.getY();
		int widthReference = (int)rectReferenceObject.getWidth();
		int heightReference = (int)rectReferenceObject.getHeight();
		switch( meOrientation ){
			case TopLeft: break;
			case TopMiddle:
				hLocation += widthReference / 2 + 1; break;
			case TopRight:
				hLocation += widthReference + 1; break;
			case BottomLeft:
				vLocation += heightReference + 1; break;
			case BottomMiddle:
				hLocation += widthReference / 2 + 1;
				vLocation += heightReference + 1; break;
			case BottomRight:
				hLocation += widthReference + 1;
				vLocation += heightReference + 1; break;
			case LeftMiddle:
				vLocation += heightReference / 2 + 1; break;
			case RightMiddle:
				hLocation += widthReference + 1;
				vLocation += heightReference / 2 + 1; break;
			case Center:
				hLocation += widthReference / 2 + 1;
				vLocation += heightReference / 2 + 1; break;
		}
		hLocation += mpxOffsetHorizontal;
		vLocation += mpxOffsetVertical;
		switch( meAlignment ){
			case TopLeft: break;
			case TopMiddle:
				hLocation -= width / 2; break;
			case TopRight:
				hLocation -= width + 1; break;
			case BottomLeft:
				vLocation -= height; break;
			case BottomMiddle:
				hLocation -= height / 2;
				vLocation -= width; break;
			case BottomRight:
				hLocation -= width + 1;
				vLocation -= height; break;
			case LeftMiddle:
				vLocation -= height / 2; break;
			case RightMiddle:
				hLocation -= width;
				vLocation -= height / 2; break;
			case Center:
				hLocation -= width / 2;
				vLocation -= height / 2; break;
		}
		return new java.awt.Point( hLocation, vLocation );
	}

	public String toString(){
		StringBuffer sb = new StringBuffer(250);
		sb.append("PlotLayout {\n");
//		sb.append("\tObject: " + getObject_String(meObject) + "\n");
//		sb.append("\tOrientation: " + getOrientation_String(meOrientation) + "\n");
//		sb.append("\tAlignment: " + getOrientation_String(meAlignment) + "\n");
		sb.append("\tOffset Horizontal: " + mpxOffsetHorizontal + "\n");
		sb.append("\tOffset Vertical: " + mpxOffsetVertical + "\n");
		sb.append("\tRotation: " + miRotation + "\n");
		sb.append("}\n");
		return sb.toString();
	}
	
}

