package cl.coretech.openbravo.translator.gui;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JDialog;
import javax.swing.JFrame;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.gui.HtmlPanel;
import org.lobobrowser.html.parser.DocumentBuilderImpl;
import org.lobobrowser.html.parser.InputSourceImpl;
import org.lobobrowser.html.test.SimpleHtmlRendererContext;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class ConversorLibrosHelp extends JDialog {

    /**
	 * 
	 */
    private static final long serialVersionUID = -7414423005280448016L;

    private String helpUri = "";

    public ConversorLibrosHelp(JFrame parent) {
        super(parent);
        helpUri = "http://www.coretech.cl/obtranslate";
        initComponents();
    }

    private void initComponents() {
        try {
            {
                this.setTitle("Ayuda");
                this.setModal(true);
            }
            HtmlPanel htmlPanel = initBrowser();
            this.add(htmlPanel, BorderLayout.CENTER);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        this.setSize(640, 480);
    }

    private HtmlPanel initBrowser() throws IOException, SAXException {
        String uri = helpUri;
        URL url = new URL(uri);
        URLConnection connection = url.openConnection();
        InputStream in = connection.getInputStream();
        Reader reader = new InputStreamReader(in);
        InputSource is = new InputSourceImpl(reader, uri);
        HtmlPanel htmlPanel = new HtmlPanel();
        HtmlRendererContext rendererContext = new LocalHtmlRendererContext(htmlPanel);
        htmlPanel.setPreferredWidth(800);
        DocumentBuilderImpl builder = new DocumentBuilderImpl(rendererContext.getUserAgentContext(), rendererContext);
        Document document = builder.parse(is);
        in.close();
        htmlPanel.setDocument(document, rendererContext);
        return htmlPanel;
    }

    private static class LocalHtmlRendererContext extends SimpleHtmlRendererContext {

        @SuppressWarnings("deprecation")
        public LocalHtmlRendererContext(HtmlPanel contextComponent) {
            super(contextComponent);
        }
    }
}
