package ru.adv.web.app.control;

import java.util.Enumeration;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import ru.adv.http.Query;
import ru.adv.logger.TLogger;
import ru.adv.mozart.Defaults;

public class InlineProxyController implements Controller {

    public static final String INLINE_URI_SUBSTR_NO_SLASH = "/inline";

    private static final String INLINE_URI_SUBSTR = INLINE_URI_SUBSTR_NO_SLASH + "/";

    private TLogger logger = new TLogger(InlineProxyController.class);

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        TLogger.setRequestInfoInThread(request);
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("request: " + request.getRequestURI());
                logger.debug("requestUrl: " + request.getRequestURL());
                logger.debug("contextPath: " + request.getContextPath());
            }
            String url = calculateOriginalUrl(request);
            if (url == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return null;
            }
            logger.info("url: " + url);
            try {
                proxyRequest(url, request, response);
            } catch (HttpException e) {
                logger.logErrorStackTrace("Error on request " + request.getRequestURI(), e);
                response.getOutputStream().write(e.toString().getBytes());
                throw e;
            }
            return null;
        } finally {
            TLogger.clearRequestInfoInThread();
        }
    }

    /**
	 * Looking /inline/ substring in URI to split it
	 * 
	 * @param request
	 * @return null if /inline/ is not found
	 */
    public static String calculateOriginalUrl(HttpServletRequest request) {
        String url = null;
        String requestURI = request.getRequestURI();
        if (requestURI.endsWith(INLINE_URI_SUBSTR_NO_SLASH)) {
            requestURI += "/";
        }
        String[] tokenz = StringUtils.split(requestURI, INLINE_URI_SUBSTR);
        if (tokenz != null) {
            Query query = new Query(request.getQueryString(), Defaults.ENCODING);
            query.remove(Defaults.INLINE_PARAM_NAME);
            query.add(Defaults.INLINE_PARAM_NAME, tokenz[0] + INLINE_URI_SUBSTR_NO_SLASH);
            String queryString = query.toString();
            if (queryString.startsWith("&")) {
                queryString = queryString.substring(1);
            }
            url = "http://" + request.getServerName() + (request.getLocalPort() == 80 ? "" : ":" + request.getLocalPort()) + "/" + tokenz[1] + "?" + queryString;
        }
        return url;
    }

    private void proxyRequest(String url, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ServletOutputStream out = response.getOutputStream();
        HttpClient httpClient = new DefaultHttpClient();
        HttpRequestBase method = null;
        if (request.getMethod().equals("GET")) {
            method = new HttpGet(url);
        } else if (request.getMethod().equals("POST")) {
            method = new HttpPost(url);
            Enumeration<?> paramNames = request.getParameterNames();
            HttpParams params = new BasicHttpParams();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement().toString();
                params.setParameter(paramName, request.getParameter(paramName));
            }
            method.setParams(params);
        } else {
            throw new Exception("Supports GET and POST methods only.");
        }
        try {
            HttpResponse proxyResponse = httpClient.execute(method);
            response.setStatus(proxyResponse.getStatusLine().getStatusCode());
            HeaderIterator headers = proxyResponse.headerIterator();
            while (headers.hasNext()) {
                Header header = headers.nextHeader();
                if ("Server".equalsIgnoreCase(header.getName())) {
                    continue;
                }
                if ("Transfer-Encoding".equalsIgnoreCase(header.getName()) && "chunked".equalsIgnoreCase(header.getValue())) {
                    continue;
                }
                response.setHeader(header.getName(), header.getValue());
            }
            proxyResponse.getEntity().writeTo(out);
            out.flush();
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
}
