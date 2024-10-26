package org.pachyderm.apollo.data;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import org.apache.log4j.Logger;
import org.pachyderm.apollo.core.UTType;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSSet;
import com.webobjects.foundation.NSTimestamp;

public class CXURLObject extends CXManagedObject {

    private static Logger LOG = Logger.getLogger(CXURLObject.class);

    private static final NSSet<String> _IntrinsicURLAttributes = new NSSet<String>(new String[] { MD.FSContentChangeDate, MD.FSExists, MD.FSSize, "NetworkFailure" });

    private NSDictionary<String, Object> _intrinsicValuesByAttribute = null;

    private URL _url;

    protected CXURLObject(URL url) {
        super();
        _url = url;
    }

    /**
   * Returns the URL referenced by this managed object.
   */
    @Override
    public URL url() {
        return _url;
    }

    public static CXManagedObject objectWithURL(URL url) {
        return (url == null) ? null : ("file".equals(url.getProtocol())) ? CXFileObject.objectWithFilePath(url.getPath()) : new CXURLObject(url);
    }

    public static CXManagedObject objectWithURL(String urlString) {
        try {
            return CXURLObject.objectWithURL(new URL(urlString));
        } catch (Exception x) {
            return null;
        }
    }

    @Override
    public String identifier() {
        return _url.toExternalForm();
    }

    /**
	 * Returns <code>public.url</code> as the type of this managed object.
	 */
    @Override
    public String typeIdentifier() {
        return UTType.URL;
    }

    @Override
    protected Object getStoredValueForAttribute(String attribute) {
        return (_IntrinsicURLAttributes.containsObject(attribute)) ? _intrinsicValueForKey(attribute) : super.getStoredValueForAttribute(attribute);
    }

    @Override
    protected void setStoredValueForAttribute(Object value, String attribute) {
        super.setStoredValueForAttribute(value, attribute);
    }

    protected Object _intrinsicValueForKey(String attribute) {
        _readValuesFromNetwork();
        return _intrinsicValuesByAttribute.objectForKey(attribute);
    }

    public NSTimestamp FSContentChangeDate() {
        _readValuesFromNetwork();
        return (NSTimestamp) _intrinsicValuesByAttribute.objectForKey(MD.FSContentChangeDate);
    }

    public Boolean FSExists() {
        _readValuesFromNetwork();
        return (Boolean) _intrinsicValuesByAttribute.objectForKey(MD.FSExists);
    }

    public Number FSSize() {
        _readValuesFromNetwork();
        return (Integer) _intrinsicValuesByAttribute.objectForKey(MD.FSSize);
    }

    private void _readValuesFromNetwork() {
        if (_intrinsicValuesByAttribute == null) {
            NSMutableDictionary<String, Object> values = new NSMutableDictionary<String, Object>(3);
            values.setObjectForKey(Boolean.FALSE, "NetworkFailure");
            try {
                URLConnection connection = url().openConnection();
                if (connection instanceof HttpURLConnection) {
                    HttpURLConnection httpconnect = (HttpURLConnection) connection;
                    httpconnect.setRequestMethod("HEAD");
                    switch(httpconnect.getResponseCode()) {
                        case HttpURLConnection.HTTP_OK:
                        case HttpURLConnection.HTTP_MOVED_PERM:
                        case HttpURLConnection.HTTP_MOVED_TEMP:
                        case HttpURLConnection.HTTP_NOT_MODIFIED:
                            values.setObjectForKey(Boolean.TRUE, MD.FSExists);
                            break;
                        default:
                            values.setObjectForKey(Boolean.FALSE, MD.FSExists);
                    }
                    LOG.info("_readValuesFromNetwork: " + httpconnect.toString());
                    values.setObjectForKey(new NSTimestamp(httpconnect.getLastModified()), MD.FSContentChangeDate);
                    values.setObjectForKey(new Integer(httpconnect.getContentLength()), MD.FSSize);
                } else {
                    values.setObjectForKey(Boolean.FALSE, MD.FSExists);
                }
            } catch (Exception x) {
                values.setObjectForKey(Boolean.FALSE, MD.FSExists);
                values.setObjectForKey(Boolean.TRUE, "NetworkFailure");
            }
            _intrinsicValuesByAttribute = values;
        }
    }

    public String toString() {
        return "<CXURLObject: id=" + identifier() + ">";
    }
}
