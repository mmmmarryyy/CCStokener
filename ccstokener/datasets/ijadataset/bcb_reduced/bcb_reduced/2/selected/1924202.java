package webuilder.webx;

import java.awt.Color;
import java.awt.LayoutManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;
import org.w3c.dom.Document;
import org.w3c.dom.html2.HTMLElement;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.lobobrowser.html.*;
import org.lobobrowser.html.gui.*;
import org.lobobrowser.html.parser.*;
import org.lobobrowser.html.test.*;
import javax.swing.JPanel;
import org.lobobrowser.html.test.*;
import webuilder.webx.util.CobraConfig;

public class DesignPane extends HtmlPanel {

    Document document;

    HtmlPanel htmlPanel;

    UserAgentContext ucontext;

    HtmlRendererContext rendererContext;

    public DesignPane() throws SAXException, IOException {
        super();
        JFrame testFrame;
        String t;
        String s;
        String uri = "http://www.w3schools.com";
        URL url = new URL(uri);
        URLConnection connection = url.openConnection();
        InputStream in = connection.getInputStream();
        Reader reader = new InputStreamReader(in);
        InputSource is = new InputSourceImpl(reader, uri);
        ucontext = new CobraConfig.LocalUserAgentContext();
        rendererContext = new CobraConfig.LocalHtmlRendererContext(this, ucontext);
        setPreferredWidth(800);
        DocumentBuilderImpl builder = new DocumentBuilderImpl(rendererContext.getUserAgentContext(), rendererContext);
        in.close();
    }

    public void displayHTML(Document document) {
        this.document = document;
        setDocument(document, rendererContext);
    }
}
