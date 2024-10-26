package de.spieleck.app.jacson.filter;

import de.spieleck.app.jacson.JacsonConfigException;
import de.spieleck.app.jacson.JacsonException;
import de.spieleck.app.jacson.JacsonRegistry;
import de.spieleck.config.ConfigNode;
import java.io.IOException;
import java.net.URL;
import java.io.InputStream;

/**
 * This filter checks if a given chunk, treated as URL works, i.e. the given
 * ressource is reachable. If the param inverse of ConstFilter is set to true
 * non-working URLs will be forwarded. If it is set to false working URLs
 * will be forwarded.
 * @author pcs
 * @since 0.89
 * @version $Id: BrokenLinkFilter.java 50 2007-03-23 21:50:25Z pcs_org $
 * @jacson:plugin subtype="reject"
 */
public class BrokenLinkFilter extends ConstFilter {

    public void putChunk(String chunk) throws JacsonException {
        try {
            URL url = new URL(chunk);
            InputStream is = url.openStream();
            if (inverse) drain.putChunk(chunk);
            is.close();
        } catch (IOException broken) {
            if (!inverse) drain.putChunk(chunk);
        }
    }
}
