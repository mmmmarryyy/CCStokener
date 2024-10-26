package wtkx.in;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Load application reference from <code>"/META-INF/wtkx"</code>.  
 *
 * @author jdp
 */
public final class MetaService extends Object {

    public static final String Resource = "/META-INF/wtkx";

    public final URL location;

    public final String value;

    public MetaService() {
        super();
        URL url = this.getClass().getResource(Resource);
        if (null != url) {
            this.location = url;
            String value = null;
            try {
                InputStream in = url.openStream();
                try {
                    value = (new java.io.DataInputStream(in).readLine());
                    if (null != value) {
                        value = value.trim();
                        if (1 > value.length()) value = null;
                    }
                } finally {
                    in.close();
                }
            } catch (IOException exc) {
                exc.printStackTrace();
            }
            this.value = value;
        } else {
            this.location = null;
            this.value = null;
        }
    }

    public boolean hasLocation() {
        return (null != this.location);
    }

    public boolean hasNotLocation() {
        return (null == this.location);
    }

    public boolean hasValue() {
        return (null != this.value);
    }

    public boolean hasNotValue() {
        return (null == this.value);
    }
}
