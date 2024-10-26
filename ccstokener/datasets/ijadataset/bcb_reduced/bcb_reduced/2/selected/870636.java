package org.t2framework.t2.testing.it;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;
import org.t2framework.commons.ut.BaseTestCase;
import org.t2framework.commons.util.Assertion;
import org.t2framework.commons.util.CloseableUtil;
import org.t2framework.commons.util.SystemPropertyUtil;
import org.t2framework.t2.contexts.HttpMethod;
import sdloader.SDLoader;
import sdloader.javaee.WebAppContext;

/**
 * T2 page integration base testcase.
 * 
 * This class provides basic integration test feature using the web container,
 * SDLoader.Once the test case invokes, SDLoader runs as web container and
 * everything works like as you access from browser.
 * 
 * @author shot
 * 
 */
public abstract class AbstractPageIntegrationTestCase extends BaseTestCase {

    protected SDLoader loader;

    protected WebAppContext context;

    protected IntegrationTestCaseConfig config;

    protected abstract IntegrationTestCaseConfig createIntegrationTestCaseConfig();

    @Override
    public void runBare() throws Throwable {
        this.loader = new SDLoader();
        this.loader.setAutoPortDetect(true);
        this.loader.setUseNoCacheMode(true);
        final String contextPath = "/" + this.getClass().getName();
        this.config = createIntegrationTestCaseConfig();
        this.context = new WebAppContext(contextPath, config.getBaseDir());
        for (String classPath : config.getClassPaths()) {
            this.context.addClassPath(classPath);
        }
        for (String libpath : config.getLibraryDirectoryPaths()) {
            this.context.addLibDirPath(libpath);
        }
        this.loader.addWebAppContext(this.context);
        try {
            this.loader.start();
            super.runBare();
        } finally {
            this.loader.stop();
        }
    }

    protected String getRequestContent(String partOfUrl, HttpMethod method, Map<String, String> paramMap, Map<String, String> headerMap) throws Exception {
        Assertion.notNull(partOfUrl);
        String s = encodeUrl(partOfUrl);
        String urlText = this.config.getProcotol() + "://localhost:" + loader.getPort() + context.getContextPath() + s;
        String params = "";
        if (paramMap != null) {
            StringBuilder builder = new StringBuilder();
            for (Entry<String, String> paramEntry : paramMap.entrySet()) {
                String key = URLEncoder.encode(paramEntry.getKey(), "UTF-8");
                String value = URLEncoder.encode(paramEntry.getValue(), "UTF-8");
                builder.append(key).append("=").append(value).append("&");
            }
            if (0 < builder.length()) {
                builder.setLength(builder.length() - 1);
            }
            params = builder.toString();
        }
        if (method == HttpMethod.GET && "".equals(params) == false) {
            urlText = (urlText.indexOf("?") == -1) ? "?" + params : "&" + params;
        }
        URL url = null;
        if (this.config.getProxyHost() == null && this.config.getProxyPort() == -1) {
            url = new URL(urlText);
        } else {
            new URL(this.config.getProcotol(), this.config.getProxyHost(), this.config.getProxyPort(), urlText);
        }
        HttpURLConnection con = null;
        BufferedReader reader = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(method.name());
            con.setUseCaches(false);
            con.setDoInput(true);
            con.setDoOutput(true);
            if (headerMap != null) {
                for (Entry<String, String> headerEntry : headerMap.entrySet()) {
                    con.addRequestProperty(headerEntry.getKey(), headerEntry.getValue());
                }
            }
            if (method == HttpMethod.POST) {
                PrintStream ps = new PrintStream(con.getOutputStream());
                ps.print(params);
                ps.close();
            }
            con.connect();
            reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line = null;
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(SystemPropertyUtil.LINE_SEP);
            }
            if (0 < builder.length()) {
                builder.setLength(builder.length() - SystemPropertyUtil.LINE_SEP.length());
            }
            return new String(builder);
        } finally {
            if (reader != null) {
                CloseableUtil.close(reader);
            }
            if (con != null) {
                con.disconnect();
            }
        }
    }

    protected String encodeUrl(String partOfUrl) {
        StringBuilder builder = new StringBuilder();
        for (char c : partOfUrl.toCharArray()) {
            if (c == ' ') {
                builder.append("%20");
            } else {
                builder.append(c);
            }
        }
        return new String(builder);
    }

    protected String getRequestContent(String urlText, HttpMethod method) throws Exception {
        return getRequestContent(urlText, method, null, null);
    }
}
