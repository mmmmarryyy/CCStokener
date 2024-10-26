package net.woodstock.rockapi.extjs.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.woodstock.rockapi.jsp.filter.HttpFilter;
import net.woodstock.rockapi.utils.ClassLoaderUtils;
import net.woodstock.rockapi.utils.IOUtils;
import net.woodstock.rockapi.utils.StringUtils;

public class ExtJSResourcesFilter extends HttpFilter {

    public static final String RESOURCES_PATH = "/extjs-resources";

    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException {
        String context = request.getContextPath();
        String resource = request.getRequestURI().replace(context, "");
        resource = resource.replaceAll(RESOURCES_PATH + "/", "");
        if ((StringUtils.isEmpty(resource)) || (resource.endsWith("/"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        this.getLogger().info("Getting resource: " + resource);
        URL url = ClassLoaderUtils.getResource(resource);
        if (url == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        InputStream input = url.openStream();
        OutputStream output = response.getOutputStream();
        URLConnection connection = url.openConnection();
        String contentEncoding = connection.getContentEncoding();
        int contentLength = connection.getContentLength();
        String contentType = connection.getContentType();
        if (contentEncoding != null) {
            response.setCharacterEncoding(contentEncoding);
        }
        response.setContentLength(contentLength);
        response.setContentType(contentType);
        IOUtils.copy(input, output, true);
    }
}
