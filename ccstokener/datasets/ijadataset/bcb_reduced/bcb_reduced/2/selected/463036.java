package ac.hiu.j314.vesma;

import ac.hiu.j314.elmve.*;
import ac.hiu.j314.elmve.clients.*;
import java.applet.*;
import java.net.*;
import java.io.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.sound.midi.*;
import javax.sound.sampled.*;

public class VAppletCustomizer extends ElmCustomizer implements AppletContext, ActionListener {

    private static final long serialVersionUID = 1L;

    Hashtable<String, AppletData> applets = new Hashtable<String, AppletData>();

    static URL documentBase;

    static AppletContext appletContext;

    JFrame frame;

    String source;

    JTextField sourceTextField;

    JButton goButton;

    public void startProcessing(MyOrder o) {
        super.startProcessing(o);
        appletContext = this;
        String htmlFile = source = o.getString(0);
        URL url = makeURL(htmlFile);
        String dbs = url.toString();
        dbs = dbs.substring(0, dbs.lastIndexOf("/") + 1);
        documentBase = makeURL(dbs);
        loadHtmlFile(url);
        start();
    }

    protected void loadHtmlFile(URL htmlFile) {
        try {
            MyParserCallback callback = new MyParserCallback();
            Reader reader = new InputStreamReader(htmlFile.openStream());
            new ParserDelegator().parse(reader, callback, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Applet getApplet(String name) {
        AppletData ad = (AppletData) applets.get(name);
        return ad.applet;
    }

    public Enumeration<Applet> getApplets() {
        Vector<Applet> v = new Vector<Applet>();
        Enumeration e = applets.elements();
        while (e.hasMoreElements()) {
            AppletData ad = (AppletData) e.nextElement();
            v.addElement(ad.applet);
        }
        return v.elements();
    }

    public AudioClip getAudioClip(URL url) {
        return new MyAudioClip(url);
    }

    public Image getImage(URL url) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        return toolkit.getImage(url);
    }

    public void showDocument(URL url) {
        ;
    }

    public void showDocument(URL url, String target) {
        ;
    }

    public void showStatus(String status) {
        ;
    }

    public void start() {
        Box box = Box.createVerticalBox();
        frame = new JFrame("VAppletCustomizer");
        frame.getContentPane().add(box);
        Enumeration e = applets.elements();
        while (e.hasMoreElements()) {
            AppletData ad = (AppletData) e.nextElement();
            box.add(ad.applet);
            ad.applet.setSize(ad.width, ad.height);
            ad.applet.init();
            ad.applet.start();
            System.out.println(ad.toString());
        }
        Box b = Box.createHorizontalBox();
        sourceTextField = new JTextField(source);
        sourceTextField.addActionListener(this);
        b.add(sourceTextField);
        goButton = new JButton("Go");
        goButton.addActionListener(this);
        b.add(goButton);
        frame.getContentPane().add(b, "North");
        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent we) {
                frame.dispose();
                dispose();
            }
        });
        frame.pack();
        frame.setVisible(true);
    }

    protected void reload() {
        frame.dispose();
        URL url = makeURL(source);
        String dbs = url.toString();
        dbs = dbs.substring(0, dbs.lastIndexOf("/") + 1);
        documentBase = makeURL(dbs);
        loadHtmlFile(url);
        start();
    }

    public void actionPerformed(ActionEvent ae) {
        if ((ae.getSource() == sourceTextField) || (ae.getSource() == goButton)) {
            source = sourceTextField.getText();
            send(makeOrder(elm, "setSource", source));
            reload();
        }
    }

    Hashtable<String, String> currentAppletParams;

    int appletCounter = 0;

    public class MyParserCallback extends HTMLEditorKit.ParserCallback {

        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            if ("applet".equals(t.toString())) {
                AppletData appletData = new AppletData();
                currentAppletParams = new Hashtable<String, String>();
                appletData.params = currentAppletParams;
                String name = (String) a.getAttribute(HTML.Attribute.NAME);
                if (name == null) name = "aPplet" + appletCounter++;
                appletData.name = name;
                URL url;
                appletData.codebase = documentBase;
                appletData.width = Integer.parseInt((String) a.getAttribute(HTML.Attribute.WIDTH));
                appletData.height = Integer.parseInt((String) a.getAttribute(HTML.Attribute.HEIGHT));
                appletData.code = (String) a.getAttribute(HTML.Attribute.CODE);
                appletData.appletStub = new MyAppletStub(appletContext, appletData);
                appletData.applet = loadApplet(appletData.codebase, appletData.code);
                appletData.applet.setStub(appletData.appletStub);
                applets.put(name, appletData);
                System.out.println(a.toString());
            }
        }

        public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            if ("param".equals(t.toString())) {
                String name = (String) a.getAttribute(HTML.Attribute.NAME);
                String value = (String) a.getAttribute(HTML.Attribute.VALUE);
                currentAppletParams.put(name, value);
                System.out.println(a.toString());
            }
        }
    }

    protected Applet loadApplet(URL codebase, String code) {
        Applet a = null;
        try {
            MyClassLoader loader = new MyClassLoader(codebase);
            String name = code.substring(0, code.length() - 6);
            a = (Applet) loader.loadClass(name).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return a;
    }

    protected class MyAppletStub implements AppletStub {

        protected AppletContext ac;

        protected AppletData ad;

        public MyAppletStub(AppletContext ac, AppletData ad) {
            this.ac = ac;
            this.ad = ad;
        }

        public void appletResize(int width, int height) {
            ;
        }

        public AppletContext getAppletContext() {
            return ac;
        }

        public URL getCodeBase() {
            return ad.codebase;
        }

        public URL getDocumentBase() {
            return documentBase;
        }

        public String getParameter(String name) {
            return (String) ad.params.get(name);
        }

        public boolean isActive() {
            return false;
        }
    }

    protected class AppletData {

        protected Hashtable params;

        protected String code;

        protected Applet applet;

        protected String name;

        protected URL codebase;

        protected int width;

        protected int height;

        protected AppletContext appletContext;

        protected AppletStub appletStub;

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(name + "\n");
            sb.append("  code: " + code.toString() + "\n");
            sb.append("  codebase: " + codebase.toString() + "\n");
            sb.append("  width: " + width + "\n");
            sb.append("  height: " + height + "\n");
            sb.append("  params: " + params.toString() + "\n");
            return sb.toString();
        }
    }

    protected class MyClassLoader extends ClassLoader {

        URL codeBase;

        public MyClassLoader(URL codeBase) {
            this.codeBase = codeBase;
        }

        public Class<?> findClass(String name) {
            byte b[] = loadClassData(name);
            return defineClass(name, b, 0, b.length);
        }

        public byte[] loadClassData(String name) {
            URL url;
            byte b[] = null;
            try {
                String s = codeBase.toString();
                if (s.startsWith("jar")) {
                    s = s.substring(s.indexOf("!") + 1, s.length());
                    url = W.getResource(s + name + ".class");
                } else {
                    url = new URL(s + name + ".class");
                }
                URLConnection c = url.openConnection();
                b = new byte[c.getContentLength()];
                InputStream is = c.getInputStream();
                is.read(b);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return b;
        }
    }

    public class MyAudioClip implements AudioClip {

        boolean isMidi;

        Sequence sequence;

        Sequencer sequencer;

        Clip clip;

        public MyAudioClip(URL url) {
            try {
                sequence = MidiSystem.getSequence(url);
                sequencer = MidiSystem.getSequencer();
                sequencer.open();
                sequencer.setSequence(sequence);
                isMidi = true;
            } catch (Exception e) {
                try {
                    AudioInputStream stream = AudioSystem.getAudioInputStream(url);
                    AudioFormat format = stream.getFormat();
                    DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
                    clip = (Clip) AudioSystem.getLine(info);
                    clip.open();
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        }

        public void play() {
            if (isMidi) sequencer.start(); else clip.start();
        }

        public void stop() {
            if (isMidi) sequencer.stop(); else clip.stop();
        }

        public void loop() {
            if (isMidi) sequencer.start(); else clip.start();
        }
    }

    public static URL makeURL(String s) {
        URL url = null;
        try {
            if (s.startsWith("http:") || s.startsWith("file:")) url = new URL(s); else if (s.startsWith("jar")) {
                s = s.substring(s.indexOf('!') + 1, s.length());
                url = W.getResource(s);
            } else url = W.getResource(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    public void setStream(String key, InputStream stream) throws IOException {
    }

    public InputStream getStream(String key) {
        return null;
    }

    public Iterator<String> getStreamKeys() {
        return null;
    }
}
