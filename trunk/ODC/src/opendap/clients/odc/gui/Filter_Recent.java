package opendap.clients.odc.gui;

import java.io.File;
import java.io.FilenameFilter;

public class Filter_Recent implements FilenameFilter {
	public boolean accept( File fileDirectory, String sName ){
		return sName.startsWith("recent-");
	}
}
