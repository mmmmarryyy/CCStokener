package fi.arcusys.acj.util.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Servlet for providing classloader resources.
 * @author Mikko Taivainen
 *
 */
public class ClasspathResourceProviderServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final Logger log = LoggerFactory.getLogger(ClasspathResourceProviderServlet.class);

    public static final String ALLOWED_PATTERNS_PROPERTY = "allowedPatterns";

    public static final String ALLOWED_RESOURCES_PROPERTY = "allowedResources";

    public static final String ALLOWED_MASKS_PROPERTY = "allowedMasks";

    public static final String ALLOWED_PATTERNS_SEPARATOR = "[\n\r\t ,;]";

    public static final String ALLOWED_RESOURCES_SEPARATOR = ALLOWED_PATTERNS_SEPARATOR;

    public static final String ALLOWED_MASKS_SEPARATOR = ALLOWED_PATTERNS_SEPARATOR;

    protected String[] getAllowedPatterns(HttpServletRequest req) {
        return doGetAllowedX(req, ALLOWED_PATTERNS_PROPERTY, ALLOWED_PATTERNS_SEPARATOR);
    }

    protected String[] getAllowedResources(HttpServletRequest req) {
        return doGetAllowedX(req, ALLOWED_RESOURCES_PROPERTY, ALLOWED_RESOURCES_SEPARATOR);
    }

    protected String[] getAllowedMasks(HttpServletRequest req) {
        return doGetAllowedX(req, ALLOWED_MASKS_PROPERTY, ALLOWED_MASKS_SEPARATOR);
    }

    protected String[] doGetAllowedX(HttpServletRequest req, String paramName, String separator) {
        String val = this.getInitParameter(paramName);
        if (null == val || 0 == val.trim().length()) {
            return new String[0];
        }
        return val.split(separator, 0);
    }

    protected boolean isAllowed(HttpServletRequest req, String resPath) {
        boolean allow = false;
        String[] allowedResources = getAllowedResources(req);
        if (allowedResources.length > 0) {
            for (String res : allowedResources) {
                if (resPath.equals(res)) {
                    log.debug("Access to resource explicitly allowed: {}", res);
                    allow = true;
                    break;
                }
            }
        }
        if (allow) {
            return true;
        }
        String[] allowedMasks = getAllowedMasks(req);
        if (allowedMasks.length > 0) {
            for (String mask : allowedMasks) {
                int lastPos = mask.length() - 1;
                if (lastPos < 0) {
                    log.warn("Skipping empty mask");
                    continue;
                }
                int wildCardPos = mask.indexOf('*');
                if (-1 == wildCardPos) {
                    allow = resPath.equals(mask);
                } else if (0 == wildCardPos) {
                    if (mask.indexOf('*', 1) >= 0) {
                        log.warn("Invalid allowedMask mask (wildcard '*' in the middle): {}", mask);
                    } else {
                        allow = resPath.endsWith(mask);
                    }
                } else if (lastPos == wildCardPos) {
                    allow = resPath.startsWith(mask);
                } else {
                    log.warn("Invalid allowedMask mask (wildcard '*' in the middle): {}", mask);
                }
                if (allow) {
                    if (log.isDebugEnabled()) {
                        log.debug("Access to resource '" + resPath + "' allowed by mask: " + mask);
                    }
                    break;
                }
            }
        }
        if (allow) {
            return true;
        }
        String[] allowedPatterns = getAllowedPatterns(req);
        if (allowedPatterns.length > 0) {
            for (String pat : allowedPatterns) {
                if (resPath.matches(pat)) {
                    allow = true;
                    log.debug("Access to resource '{}' allowed by pattern '{}'", resPath, pat);
                }
            }
        }
        return allow;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String reqUri = req.getRequestURI();
        log.debug("doGet; RequestURI = {}", reqUri);
        String contextPath = req.getContextPath();
        if (!reqUri.startsWith(contextPath)) {
            throw new IllegalArgumentException("RequestURI '" + reqUri + "' doesn't start with context path '" + contextPath + "' ");
        }
        reqUri = reqUri.substring(contextPath.length());
        log.debug("Requested resource: '{}'", reqUri);
        boolean allow = isAllowed(req, reqUri);
        if (!allow) {
            log.error("Resource '{}' is not included in allowedResources, allowedMasks or allowedPatterns", reqUri);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        URL resUrl = ClasspathResourceProviderServlet.class.getResource(reqUri);
        if (null == resUrl) {
            resp.sendError(404, "No such resource found: " + reqUri);
        } else {
            byte[] data = readResource(resUrl);
            log.debug("Read {} bytes of data", data.length);
            resp.setContentLength(data.length);
            OutputStream out = new BufferedOutputStream(resp.getOutputStream());
            out.write(data);
            out.flush();
            log.debug("Wrote {} bytes of data", data.length);
        }
    }

    private static class Buf {

        int length;

        byte[] data;

        Buf(byte[] data, int length) {
            this.data = data;
            this.length = length;
        }
    }

    byte[] readResource(URL url) throws IOException {
        InputStream in = new BufferedInputStream(url.openStream());
        try {
            int totalSize = 0;
            List<Buf> buffers = new LinkedList<Buf>();
            final int BUF_SIZE = 1024;
            byte[] buf = new byte[BUF_SIZE];
            int read;
            do {
                read = in.read(buf);
                if (read > 0) {
                    totalSize += read;
                    buffers.add(new Buf(buf, read));
                    buf = new byte[BUF_SIZE];
                }
            } while (read > 0);
            buf = new byte[totalSize];
            int pos = 0;
            for (Buf buf2 : buffers) {
                System.arraycopy(buf2.data, 0, buf, pos, buf2.length);
                pos += buf2.length;
            }
            return buf;
        } finally {
            try {
                in.close();
            } catch (Exception ex) {
                log.error("An exception while closing InputStream", ex);
            }
        }
    }
}
