import jdvi.*;
import java.applet.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.applet.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.net.*;

/**
 * The main class <code>JDvi</code> of the jDvi TeX dvi file viewer.
 *
 * This is free software (see the <a href="GPL">GPL</a>).
 * @author <a href="mailto:timh@sfb288.math.tu-berlin.de">Tim Hoffmann</a>
 * @version $Revision: 1.1.1.1 $
 */
public class JDvi extends Panel implements JDviContext {

    JDviViewerPanel dvi;

    URL scribbleURL;

    Properties properties;

    FileDialog dialog;

    long modificationTime = 0;

    File dviFile;

    public JDvi() throws java.io.FileNotFoundException, java.io.IOException {
        properties = new Properties();
        File propFile = new File(System.getProperty("jdvi.propertiesFile", System.getProperty("user.home") + System.getProperty("file.separator") + "jdviProp"));
        if (propFile.exists()) {
            System.out.println("loading properties from " + propFile.getAbsolutePath());
            InputStream stream = new FileInputStream(propFile);
            properties.load(stream);
            stream.close();
            System.out.println("properties are " + properties);
        }
        setBackground(Color.white);
        setLayout(new BorderLayout());
    }

    public Image getImage(URL url) {
        Image img = null;
        try {
            URLConnection connect = url.openConnection();
            Object oo = connect.getContent();
            if (oo instanceof ImageProducer) {
                System.out.println("image producer " + oo);
                img = createImage((ImageProducer) oo);
            } else if (oo instanceof Image) {
                img = (Image) oo;
            }
        } catch (java.net.MalformedURLException ex) {
            img = null;
            System.err.println("Image loading: " + ex);
        } catch (java.io.IOException ex2) {
            img = null;
            System.err.println("Image loading: " + ex2);
        } finally {
            return img;
        }
    }

    public AudioClip getAudioClip(URL url) {
        return null;
    }

    public void showDocument(URL url) {
        String cmd[] = new String[2];
        cmd[0] = System.getProperty("jdvi.browser", getProperties().getProperty("jdvi.browser", "netscape"));
        cmd[1] = url.toExternalForm();
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (java.io.IOException e) {
            System.out.println(e);
        }
    }

    public void showDocument(URL url, String s) {
        try {
            showDocument(new URL(url, s));
        } catch (java.net.MalformedURLException e) {
        }
    }

    public URL getCodeBase() {
        try {
            return new URL("file:" + System.getProperty("user.dir") + System.getProperty("file.separator"));
        } catch (java.net.MalformedURLException e) {
            return null;
        }
    }

    public URL getDocumentBase() {
        return getCodeBase();
    }

    public void inform(String s) {
        System.out.println("Inform: " + s);
    }

    /**
     * This method is no longer part of the JDviContext interface.
     */
    public Properties getProperties() {
        return properties;
    }

    public String getProperty(String name) {
        return System.getProperty(name, properties.getProperty(name));
    }

    public String getProperty(String name, String defRes) {
        return System.getProperty(name, properties.getProperty(name, defRes));
    }

    /**
     * If the property "jdvi.scribbleFile.save" is non null,
     * writeScribbleFile() opens a file dialog to ask where to save
     * the scribble file.
     */
    public void writeScribbleFile() {
        if (getProperty("jdvi.scribbleFile.save") == null) return;
        String file = null;
        if (scribbleURL.getProtocol().equals("file")) {
            file = scribbleURL.getFile();
            dialog.setFile(file);
        }
        dialog.show();
        if (dialog.getFile() != null) {
            file = dialog.getDirectory() + dialog.getFile();
            System.out.println("Choosen:" + dialog.getDirectory() + dialog.getFile());
            dialog.dispose();
            try {
                FileOutputStream ostream = new FileOutputStream(file);
                ObjectOutputStream q = new ObjectOutputStream(ostream);
                System.out.print("Writing scribble file " + file);
                for (int i = 0; i < dvi.getNumberOfPages(); i++) {
                    q.writeObject(dvi.page.scribble[i]);
                    System.out.print(".");
                }
                System.out.println(" ");
                q.flush();
                ostream.close();
            } catch (java.io.FileNotFoundException e) {
                System.err.println(e);
            } catch (java.io.IOException e2) {
                System.err.println(e2);
            }
        } else {
            System.out.println("Nothing saved!");
        }
    }

    /**
     * if the property "jdvi.font.cache" is non null, writeFontCache()
     * writes a serialized zipped version of the font array of the JDviPanel
     * dvi.page.document.font into a file with name equal to the value
     * of above property. This can be read by JDvi ot the JDviApplet
     * to speed up font loading.
     */
    public void writeFontCache() {
        String fontCache = getProperty("jdvi.font.cache");
        if (fontCache != null) try {
            FileOutputStream ostream = new FileOutputStream(fontCache);
            DeflaterOutputStream dstream = new DeflaterOutputStream(ostream);
            ObjectOutputStream q = new ObjectOutputStream(dstream);
            System.out.print("Writing font cache file " + fontCache);
            q.writeObject(dvi.page.document.font);
            q.flush();
            dstream.close();
        } catch (java.io.FileNotFoundException e) {
            System.err.println(e);
        } catch (java.io.IOException e2) {
            System.err.println(e2);
        }
    }

    /**
     * 	Read a scribble file written by writeScribbleFile()
     */
    public void readScribbleFile() {
        try {
            InputStream istream = scribbleURL.openStream();
            ObjectInputStream q = new ObjectInputStream(istream);
            System.out.println("Reading scribble file " + scribbleURL);
            for (int i = 0; i < dvi.getNumberOfPages(); i++) {
                dvi.page.scribble[i] = (JDviScribble) q.readObject();
            }
            istream.close();
        } catch (java.io.FileNotFoundException e) {
            System.err.println(e);
        } catch (java.io.IOException e2) {
            System.err.println(e2);
        } catch (java.lang.ClassNotFoundException e3) {
            System.err.println(e3);
        }
    }

    public static void main(String args[]) throws java.io.FileNotFoundException, java.io.IOException {
        System.out.println("home:" + System.getProperty("user.home"));
        final JDvi p = new JDvi();
        if (args.length > 2 | args.length < 1) {
            System.err.println("usage: java JDvi dvi_file");
            System.exit(0);
        }
        p.dvi = new JDviViewerPanel(p);
        Button quit = new Button("Quit");
        quit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                p.writeScribbleFile();
                p.writeFontCache();
                System.exit(0);
            }
        });
        p.dvi.addCustomComponent(quit);
        p.add("Center", p.dvi);
        WindowListener l = new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                p.writeScribbleFile();
                p.writeFontCache();
                System.exit(0);
            }

            public void windowActivated(WindowEvent e) {
                if (p.dviFile != null) {
                    if (p.modificationTime < p.dviFile.lastModified()) {
                        try {
                            p.modificationTime = p.dviFile.lastModified();
                            p.dvi.page.fullsize.flush();
                            p.dvi.page.fullsize = null;
                            p.dvi.loadDocument(p.dvi.page.document.document, p.dvi.page.document.document.toString());
                            p.dvi.page.repaint();
                        } catch (java.io.FileNotFoundException ex1) {
                        } catch (java.io.IOException ex2) {
                        }
                    }
                }
            }
        };
        Frame fr = new Frame("jDvi");
        fr.addWindowListener(l);
        fr.add(p);
        int x = 600;
        int y = 500;
        try {
            x = Integer.parseInt(p.getProperty("jdvi.frame.width"));
            y = Integer.parseInt(p.getProperty("jdvi.frame.height"));
        } catch (java.lang.NumberFormatException e) {
            System.err.println("Format error while parsing frame resolution properties. Using default insead.System.properties:" + System.getProperties());
        }
        fr.setSize(x, y);
        fr.show();
        p.dialog = new FileDialog(fr, "Write ScribbleFile", FileDialog.SAVE);
        URL baseURL = new URL("file:" + System.getProperty("user.dir") + System.getProperty("file.separator"));
        URL fileURL = new URL(baseURL, args[0]);
        String scribbleFile = fileURL.getFile();
        if (scribbleFile.endsWith(".dvi")) scribbleFile = scribbleFile.substring(0, scribbleFile.length() - 3) + "sbl"; else scribbleFile = scribbleFile + ".sbl";
        scribbleFile = p.getProperty("jdvi.scribbleFile.name", scribbleFile);
        if (fileURL.getProtocol().startsWith("file")) {
            p.dviFile = new File(fileURL.getFile());
            p.modificationTime = p.dviFile.lastModified();
        }
        fr.setTitle(fileURL.toString());
        if (args.length < 2) {
            p.dvi.loadDocument(fileURL, fileURL.toString());
        } else p.dvi.loadDocument(fileURL, "file:" + System.getProperty("user.dir") + System.getProperty("file.separator") + args[1]);
        p.repaint();
        p.dvi.gotoPage(0);
    }
}
