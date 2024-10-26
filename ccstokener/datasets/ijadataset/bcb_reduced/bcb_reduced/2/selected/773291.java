package org.wam.browser;

/** A browser that renders WAM documents */
public class WamBrowser extends javax.swing.JPanel {

    javax.swing.JTextField theAddressBar;

    private WamContentPane theContentPane;

    /** Creates a WAM browser */
    public WamBrowser() {
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS));
        theAddressBar = new javax.swing.JTextField(250);
        add(theAddressBar);
        theContentPane = new WamContentPane();
        add(theContentPane);
        theContentPane.setPreferredSize(new java.awt.Dimension(800, 600));
        theAddressBar.addKeyListener(new java.awt.event.KeyAdapter() {

            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() != java.awt.event.KeyEvent.VK_ENTER) return;
                goToAddress(theAddressBar.getText());
            }
        });
    }

    /** @param address The address of the document to get */
    public void goToAddress(String address) {
        java.net.URL url;
        try {
            url = new java.net.URL(address);
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException(address + " is not a valid URL", e);
        }
        org.wam.parser.WamParser wamParser = new org.wam.parser.WamDomParser();
        org.wam.core.WamDocument wamDoc;
        try {
            wamDoc = wamParser.parseDocument(new java.io.InputStreamReader(url.openStream()), new org.wam.core.WamDocument.GraphicsGetter() {

                @Override
                public java.awt.Graphics2D getGraphics() {
                    return (java.awt.Graphics2D) WamBrowser.this.getGraphics();
                }
            });
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Could not access address " + address, e);
        } catch (org.wam.parser.WamParseException e) {
            throw new IllegalArgumentException("Could not parse XML document at " + address, e);
        }
        wamDoc.postCreate();
        theContentPane.setContent(wamDoc);
        repaint();
    }
}
