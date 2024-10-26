package org.jsserv.resource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import org.apache.log4j.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;

/**
 * Holder for a Script resource. To be cached by {@link URLResourceLoader}.
 * @author shelmberger
 */
public class ScriptHolder extends URLResourceHolder<Script> {

    protected static Logger log = Logger.getLogger(ScriptHolder.class);

    public ScriptHolder(URL url) {
        super(url);
        log.debug("Construction script holder for " + url);
    }

    @Override
    public Script createContent() throws IOException {
        Context cx = Context.enter();
        Reader reader = null;
        try {
            URL url = getURL();
            reader = new InputStreamReader(url.openStream());
            return cx.compileReader(reader, url.toExternalForm(), 1, null);
        } finally {
            if (reader != null) {
                reader.close();
            }
            Context.exit();
        }
    }
}
