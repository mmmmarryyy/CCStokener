package org.fit.cssbox.demo;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JFrame;
import javax.swing.tree.DefaultTreeModel;
import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.exceptions.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.fit.cssbox.layout.BrowserCanvas;
import org.fit.cssbox.layout.Viewport;
import org.fit.cssbox.pdf.PdfBrowserCanvas;
import org.w3c.dom.Document;

/**
 * This demo shows a simple browser of PDF files based on transforming the files to DOM and rendering by CSSBox.
 * It is based on the {@link BoxBrowser} demo from the CSSBox packages, only the document pre-processing part is changed.
 * 
 * @author burgetr
 */
public class PdfBoxBrowser extends org.fit.cssbox.demo.BoxBrowser {

    @Override
    public void displayURL(String urlstring) {
        try {
            if (!urlstring.startsWith("http:") && !urlstring.startsWith("ftp:") && !urlstring.startsWith("file:")) urlstring = "http://" + urlstring;
            URL url = new URL(urlstring);
            urlText.setText(url.toString());
            URLConnection con = url.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; BoxBrowserTest/2.x; Linux) CSSBox/2.x (like Gecko)");
            InputStream is = con.getInputStream();
            System.out.println("Parsing PDF: " + url);
            PDDocument doc = loadPdf(is);
            is.close();
            contentCanvas = new PdfBrowserCanvas(doc, null, contentScroll.getSize(), url);
            contentCanvas.addMouseListener(new MouseListener() {

                public void mouseClicked(MouseEvent e) {
                    System.out.println("Click: " + e.getX() + ":" + e.getY());
                    canvasClick(e.getX(), e.getY());
                }

                public void mousePressed(MouseEvent e) {
                }

                public void mouseReleased(MouseEvent e) {
                }

                public void mouseEntered(MouseEvent e) {
                }

                public void mouseExited(MouseEvent e) {
                }
            });
            contentScroll.setViewportView(contentCanvas);
            Viewport viewport = ((BrowserCanvas) contentCanvas).getViewport();
            root = createBoxTree(viewport);
            boxTree.setModel(new DefaultTreeModel(root));
            Document dom = ((PdfBrowserCanvas) contentCanvas).getBoxTree().getDocument();
            domTree.setModel(new DefaultTreeModel(createDomTree(dom)));
        } catch (Exception e) {
            System.err.println("*** Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected PDDocument loadPdf(InputStream is) throws IOException {
        PDDocument document = null;
        document = PDDocument.load(is);
        if (document.isEncrypted()) {
            try {
                document.decrypt("");
            } catch (InvalidPasswordException e) {
                System.err.println("Error: Document is encrypted with a password.");
                System.exit(1);
            } catch (CryptographyException e) {
                System.err.println("Error: Document is encrypted with a password.");
                System.exit(1);
            }
        }
        return document;
    }

    public static void main(String[] args) {
        browser = new PdfBoxBrowser();
        JFrame main = browser.getMainWindow();
        main.setSize(1200, 600);
        main.setVisible(true);
    }
}
