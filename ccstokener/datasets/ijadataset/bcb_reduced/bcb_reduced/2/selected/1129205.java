package net.sourceforge.thinfeeder.command.action;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import net.sourceforge.thinfeeder.ThinFeeder;
import net.sourceforge.thinfeeder.model.dao.DAOSystem;
import net.sourceforge.thinfeeder.util.Utils;

/**
 * @author fabianofranz@users.sourceforge.net
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CheckNewVersionAction extends Action {

    private static final String VERSION_FILE = "http://thinfeeder.sourceforge.net/version.txt";

    private boolean hasNewVersion = false;

    /**
	 * @param main
	 */
    public CheckNewVersionAction(ThinFeeder main) {
        super(main);
    }

    public void doAction() throws MalformedURLException, IOException, Exception {
        URL url = new URL(CheckNewVersionAction.VERSION_FILE);
        InputStream is = url.openStream();
        byte[] buffer = Utils.loadBytes(is);
        is.close();
        String version = new String(buffer);
        if (version != null) {
            version = version.substring(0, version.lastIndexOf("\n") == -1 ? version.length() : version.lastIndexOf("\n"));
        }
        hasNewVersion = !DAOSystem.getSystem().getVersion().equals(version);
    }

    /**
	 * @return Returns the hasNewVersion.
	 */
    public boolean hasNewVersion() {
        return hasNewVersion;
    }
}
