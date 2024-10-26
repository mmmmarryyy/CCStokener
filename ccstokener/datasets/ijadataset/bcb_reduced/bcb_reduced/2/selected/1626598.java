package org.salamandra.web.core.request;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ResourceUtils;
import org.springframework.web.util.UrlPathHelper;

public class JSEntityRequest extends AbstractInspectRequest {

    /** Logger that is available to subclasses */
    protected static final Log LOG = LogFactory.getLog(JSEntityRequest.class);

    private UrlPathHelper urlPathHelper;

    public JSEntityRequest(HttpServletRequest request) {
        super(request);
        this.urlPathHelper = new UrlPathHelper();
    }

    /**
	 * Look up a handler for the URL path of the given request.
	 * @param request current HTTP request
	 * @return the looked up handler instance, or <code>null</code>
	 */
    protected String getPageName() {
        String lookupPath = this.urlPathHelper.getLookupPathForRequest(getRequest());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Looking up handler for [" + lookupPath + "]");
        }
        return lookupPath;
    }

    @Override
    public boolean isValidate() {
        if (true) return false;
        String name = getPageName();
        if (!name.equals("/home.do")) return true;
        return false;
    }

    public Resource getResource() {
        return new Resource(getPageName());
    }

    public class Resource {

        private String location;

        protected Resource(String location) {
            this.location = location;
        }

        public void write(OutputStream out) throws IOException {
            StringBuffer sb = new StringBuffer();
            sb.append(ResourceUtils.CLASSPATH_URL_PREFIX);
            sb.append(location);
            URL url = ResourceUtils.getURL(sb.toString());
            copy(url.openStream(), out, 8096);
        }

        private void copy(final InputStream input, final OutputStream output, final int bufferSize) throws IOException {
            try {
                final byte[] buffer = new byte[bufferSize];
                int n;
                while (-1 != (n = input.read(buffer))) {
                    output.write(buffer, 0, n);
                }
            } finally {
                input.close();
            }
        }
    }
}
