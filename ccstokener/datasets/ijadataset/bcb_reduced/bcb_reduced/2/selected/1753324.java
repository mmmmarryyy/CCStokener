package org.akrogen.tkui.usecases.platform.swt.html;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import org.akrogen.tkui.core.dom.bindings.IDOMDocumentBindable;
import org.akrogen.tkui.core.platform.IPlatform;
import org.akrogen.tkui.dom.xhtml.XHTMLConstants;
import org.akrogen.tkui.dom.xul.XULConstants;
import org.akrogen.tkui.dom.xul.dom.simples.Textbox;
import org.akrogen.tkui.platform.swt.SWTPlatform;
import org.cyberneko.html.parsers.DOMParser;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.ufacekit.core.databinding.dom.DOMObservables;
import org.ufacekit.core.databinding.dom.conversion.xerces.StringToXercesDocumentConverter;
import org.ufacekit.core.databinding.dom.conversion.xerces.XercesDocumentToStringConverter;
import org.ufacekit.core.dom.events.DOMEventConstants;
import org.ufacekit.ui.jface.databinding.jface.SWTObservables;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.xml.sax.InputSource;

public class WebBrowserTest {

    public static void main(String[] args) {
        try {
            Display display = new Display();
            Shell shell = new Shell(display, SWT.SHELL_TRIM);
            FillLayout layout = new FillLayout();
            shell.setLayout(layout);
            Composite topPanel = new Composite(shell, SWT.NONE);
            FillLayout layoutToPanel = new FillLayout();
            topPanel.setLayout(layoutToPanel);
            InputStream source = new FileInputStream(new File("resources/html/browser.xul"));
            IPlatform platform = SWTPlatform.getDefaultPlatform();
            final IDOMDocumentBindable document = platform.createDocument(topPanel, null, null);
            document.setDefaultNamespaceURI(XHTMLConstants.XHTML_NAMESPACE_URI);
            document.addErrorHandler(new DOMErrorHandler() {

                public boolean handleError(DOMError error) {
                    Exception e = (Exception) error.getRelatedException();
                    e.printStackTrace();
                    return false;
                }
            });
            platform.loadDocument(source, document);
            Element searchElement = document.getElementById("search");
            ((EventTarget) searchElement).addEventListener(DOMEventConstants.click, new EventListener() {

                public void handleEvent(org.w3c.dom.events.Event evt) {
                    load(document);
                }
            }, false);
            Text textarea = new Text(topPanel, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
            DataBindingContext context = document.getDataBindingContext();
            Realm realm = context.getValidationRealm();
            context.bindValue(SWTObservables.observeText(textarea, SWT.Modify), DOMObservables.observeDocumentContent(realm, document), new UpdateValueStrategy().setConverter(new StringToNekoDocumentConverter()), new UpdateValueStrategy().setConverter(new XercesDocumentToStringConverter()));
            shell.pack();
            shell.open();
            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) display.sleep();
            }
            shell.dispose();
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
