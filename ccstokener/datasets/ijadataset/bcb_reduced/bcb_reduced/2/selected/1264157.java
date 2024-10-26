package app;

import java.io.BufferedReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.gui.HtmlPanel;
import org.lobobrowser.html.parser.DocumentBuilderImpl;
import org.lobobrowser.html.parser.InputSourceImpl;
import org.w3c.dom.Document;
import org.w3c.dom.html2.HTMLElement;
import org.xml.sax.InputSource;
import org.lobobrowser.html.test.*;
import org.xml.sax.SAXException;

/**
 *
 * @author samir
 */
public class BrowserSilencioso {

    private String uri;

    private String body;

    private String login;

    private String senha;

    private String ip;

    public BrowserSilencioso(String ip, String login, String senha) {
        uri = "http://" + ip + "/login";
        this.login = login;
        this.senha = senha;
        this.ip = ip;
    }

    public String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line + "\n");
        }
        is.close();
        return sb.toString();
    }

    public void open() throws MalformedURLException, IOException, SAXException {
        URL url = new URL(uri);
        URLConnection connection = url.openConnection();
        InputStream in = connection.getInputStream();
        body = convertStreamToString(in);
        body = body.replace("<head>", "<head><base href=\"http://" + ip + "\">");
        body = body.replace("document.login.username.focus();", "document.login.username.value = \"" + login + "\";" + "document.login.password.value = \"" + senha + "\";" + "doLogin();");
        in = new ByteArrayInputStream(body.getBytes("UTF-8"));
        Reader reader = new InputStreamReader(in);
        InputSource is = new InputSourceImpl(reader, uri);
        HtmlPanel htmlPanel = new HtmlPanel();
        UserAgentContext ucontext = new LocalUserAgentContext();
        HtmlRendererContext rendererContext = new LocalHtmlRendererContext(htmlPanel, ucontext);
        htmlPanel.setPreferredWidth(100);
        DocumentBuilderImpl builder = new DocumentBuilderImpl(rendererContext.getUserAgentContext(), rendererContext);
        Document document = builder.parse(is);
        in.close();
        htmlPanel.setDocument(document, rendererContext);
    }

    private static class LocalUserAgentContext extends SimpleUserAgentContext {

        public LocalUserAgentContext() {
        }

        @Override
        public String getAppMinorVersion() {
            return "0";
        }

        @Override
        public String getAppName() {
            return "MikrotkHotspotLogin";
        }

        @Override
        public String getAppVersion() {
            return "1";
        }

        @Override
        public String getUserAgent() {
            return "Mozilla/4.0 (compatible;) CobraTest/1.0";
        }
    }

    private static class LocalHtmlRendererContext extends SimpleHtmlRendererContext {

        public LocalHtmlRendererContext(HtmlPanel contextComponent, UserAgentContext ucontext) {
            super(contextComponent, ucontext);
        }

        @Override
        public void linkClicked(HTMLElement linkNode, URL url, String target) {
            super.linkClicked(linkNode, url, target);
        }

        @Override
        public HtmlRendererContext open(URL url, String windowName, String windowFeatures, boolean replace) {
            HtmlPanel newPanel = new HtmlPanel();
            HtmlRendererContext newCtx = new LocalHtmlRendererContext(newPanel, this.getUserAgentContext());
            newCtx.navigate(url, "_this");
            return newCtx;
        }
    }
}
