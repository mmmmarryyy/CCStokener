package org.akrogen.tkui.usecases.platform.swing.html;

import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.akrogen.tkui.core.dom.bindings.IDOMDocumentBindable;
import org.akrogen.tkui.core.platform.IPlatform;
import org.akrogen.tkui.dom.xhtml.XHTMLConstants;
import org.akrogen.tkui.dom.xul.XULConstants;
import org.akrogen.tkui.dom.xul.dom.simples.Textbox;
import org.akrogen.tkui.platform.swing.SwingPlatform;
import org.apache.xerces.parsers.DOMParser;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.Realm;
import org.ufacekit.core.databinding.dom.DOMObservables;
import org.ufacekit.core.databinding.dom.conversion.xerces.StringToXercesDocumentConverter;
import org.ufacekit.core.databinding.dom.conversion.xerces.XercesDocumentToStringConverter;
import org.ufacekit.core.dom.events.DOMEventConstants;
import org.ufacekit.ui.swing.databinding.swing.SwingEventConstants;
import org.ufacekit.ui.swing.databinding.swing.SwingObservables;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.xml.sax.InputSource;

public class WEBBrowserTest {

    public static void main(String[] args) {
        try {
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JPanel panel = new JPanel();
            frame.getContentPane().add(panel);
            panel.setLayout(new GridLayout());
            InputStream source = new FileInputStream(new File("resources/html/browser.xul"));
            IPlatform platform = SwingPlatform.getDefaultPlatform();
            final IDOMDocumentBindable document = platform.createDocument(panel, null, null);
            document.setDefaultNamespaceURI(XHTMLConstants.XHTML_NAMESPACE_URI);
            platform.loadDocument(source, document);
            Element searchElement = document.getElementById("search");
            ((EventTarget) searchElement).addEventListener(DOMEventConstants.click, new EventListener() {

                public void handleEvent(org.w3c.dom.events.Event evt) {
                    load(document);
                }
            }, false);
            JTextArea textarea = new JTextArea();
            JScrollPane scrollPane = new JScrollPane(textarea);
            panel.add(scrollPane);
            DataBindingContext context = document.getDataBindingContext();
            Realm realm = context.getValidationRealm();
            context.bindValue(SwingObservables.observeText(textarea, SwingEventConstants.Modify), DOMObservables.observeDocumentContent(realm, document), new UpdateValueStrategy().setConverter(new StringToXercesDocumentConverter()), new UpdateValueStrategy().setConverter(new XercesDocumentToStringConverter()));
            frame.pack();
            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void load(final IDOMDocumentBindable browser) {
        try {
            String urlText = ((Textbox) browser.getElementById("url")).getValue();
            URL url = new java.net.URL((new File("./")).toURL(), urlText);
            final DOMParser parser = new org.cyberneko.html.parsers.DOMParser();
            parser.parse(new InputSource(url.openStream()));
            Document domHTMLSelect = parser.getDocument();
            Element element = domHTMLSelect.getDocumentElement();
            Element content = browser.getElementById("content");
            Element contentG = browser.getElementById("contentG");
            if (contentG != null) {
                content.removeChild(contentG);
            }
            contentG = browser.createElementNS(XULConstants.XUL_NAMESPACE_URI, XULConstants.HBOX_ELEMENT);
            contentG.setAttribute("id", "contentG");
            content.appendChild(contentG);
            browser.setBaseURI(urlText);
            Node n = browser.importNode(element, true);
            contentG.appendChild(n);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
