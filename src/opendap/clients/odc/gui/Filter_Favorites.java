package opendap.clients.odc.gui;

import java.io.File;
import java.io.FilenameFilter;

public class Filter_Favorites implements FilenameFilter {
	public boolean accept( File fileDirectory, String sName ){
		return sName.startsWith("favorite-");
	}
}

