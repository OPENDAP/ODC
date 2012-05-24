package opendap.clients.odc.plot;

import java.awt.image.BufferedImage;
import java.awt.Rectangle;

abstract public class CompositionElement {
	abstract public boolean zRender( BufferedImage bi, Rectangle location, StringBuffer sbError );
	abstract public String getDescriptor();
}
