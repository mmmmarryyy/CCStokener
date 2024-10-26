package edu.upmc.opi.caBIG.caTIES.map.lobo;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.awt.*;
import java.util.logging.*;
import javax.swing.*;
import org.w3c.dom.Document;
import org.w3c.dom.html2.HTMLElement;
import org.xml.sax.InputSource;
import org.lobobrowser.html.*;
import org.lobobrowser.html.gui.*;
import org.lobobrowser.html.parser.*;
import org.lobobrowser.html.test.*;

public class BarebonesTest {

    public static void main(String[] args) throws Exception {
        Logger.getLogger("org.lobobrowser").setLevel(Level.WARNING);
        String uri = "http://lobobrowser.org/browser/home.jsp";
        uri = "http://www.google.com/fusiontables/embedviz?viz=MAP&q=select+col4%2C+col5%2C+col6%2C+col7%2C+col11%2C+col8%2C+col9%2C+col10%2C+col13+from+1159987+&h=false&lat=46.55886030311719&lng=-95.80078125&z=4&t=2&l=col13";
        URL url = new URL(uri);
        URLConnection connection = url.openConnection();
        InputStream in = connection.getInputStream();
        Reader reader = new InputStreamReader(in);
        InputSource is = new InputSourceImpl(reader, uri);
        HtmlPanel htmlPanel = new HtmlPanel();
        UserAgentContext ucontext = new LocalUserAgentContext();
        HtmlRendererContext rendererContext = new LocalHtmlRendererContext(htmlPanel, ucontext);
        htmlPanel.setPreferredWidth(800);
        DocumentBuilderImpl builder = new DocumentBuilderImpl(rendererContext.getUserAgentContext(), rendererContext);
        Document document = builder.parse(is);
        in.close();
        htmlPanel.setDocument(document, rendererContext);
        final JFrame frame = new JFrame();
        frame.getContentPane().add(htmlPanel);
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                frame.pack();
                frame.setVisible(true);
            }
        });
    }

    private static class LocalUserAgentContext extends SimpleUserAgentContext {

        public LocalUserAgentContext() {
        }

        public String getAppMinorVersion() {
            return "0";
        }

        public String getAppName() {
            return "BarebonesTest";
        }

        public String getAppVersion() {
            return "1";
        }

        public String getUserAgent() {
            return "Mozilla/4.0 (compatible;) CobraTest/1.0";
        }
    }

    private static class LocalHtmlRendererContext extends SimpleHtmlRendererContext {

        public LocalHtmlRendererContext(HtmlPanel contextComponent, UserAgentContext ucontext) {
            super(contextComponent, ucontext);
        }

        public void linkClicked(HTMLElement linkNode, URL url, String target) {
            super.linkClicked(linkNode, url, target);
            System.out.println("## Link clicked: " + linkNode);
        }

        public HtmlRendererContext open(URL url, String windowName, String windowFeatures, boolean replace) {
            HtmlPanel newPanel = new HtmlPanel();
            JFrame frame = new JFrame();
            frame.setSize(600, 400);
            frame.getContentPane().add(newPanel);
            HtmlRendererContext newCtx = new LocalHtmlRendererContext(newPanel, this.getUserAgentContext());
            newCtx.navigate(url, "_this");
            return newCtx;
        }
    }
}
