package ext.egantt.util.hive;

import com.egantt.util.Trace;
import java.io.InputStream;
import java.net.URL;

public abstract class AbstractHive implements HiveImpl {

    public AbstractHive() {
    }

    public URL getURL(String fragment) {
        URL url = null;
        try {
            url = createURL(fragment);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (url == null) return null;
        try {
            InputStream is = url.openStream();
            if (is != null) {
                is.close();
                return url;
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace(Trace.out);
        }
        return null;
    }

    protected abstract URL createURL(String s) throws Throwable;
}
