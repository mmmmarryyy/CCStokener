package me.w70.bot.gui.rs;

import java.applet.Applet;
import java.applet.AppletContext;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import me.w70.bot.Bot;
import me.w70.bot.gui.MainWindow;

public class RSPanel extends Applet implements java.applet.AppletStub {

    private HashMap<String, String> params = new HashMap<String, String>();

    private int world = 33;

    private boolean members = false;

    public JPanel theGame;

    public RSLoader rsLoader;

    public Applet loader;

    public Mouse mouseManager;

    public Keyboard keyboardManager;

    @Override
    public final void update(Graphics graphics) {
        if (loader != null) {
            loader.update(graphics);
        }
    }

    public Applet getClient() {
        return loader;
    }

    @Override
    public final void paint(Graphics graphics) {
        if (loader != null) {
            loader.paint(graphics);
        }
    }

    private String readPage(URL url) throws IOException, InterruptedException {
        String referer = url.toExternalForm();
        String s = readPage(url, referer);
        return s;
    }

    private String readPage(URL url, String referer) throws IOException, InterruptedException {
        URLConnection uc = url.openConnection();
        uc.addRequestProperty("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        uc.addRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        uc.addRequestProperty("Accept-Encoding", "gzip,deflate");
        uc.addRequestProperty("Accept-Language", "en-gb,en;q=0.5");
        uc.addRequestProperty("Connection", "keep-alive");
        uc.addRequestProperty("Host", "www.runescape.com");
        uc.addRequestProperty("Keep-Alive", "300");
        if (referer != null) uc.addRequestProperty("Referer", referer);
        uc.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.8.0.6) Gecko/20060728 Firefox/1.5.0.6");
        DataInputStream di = new DataInputStream(uc.getInputStream());
        byte[] buffer = new byte[uc.getContentLength()];
        di.readFully(buffer);
        di.close();
        Thread.sleep(250 + (int) Math.random() * 500);
        return new String(buffer);
    }

    private void getParams() {
        String ret = "";
        try {
            String m = "m0";
            if (members) {
                m = "m1";
            }
            ret = readPage(new URL("http://world" + world + ".runescape.com/plugin.js?param=o0,a0," + m + ",s0"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(world);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String regex = "<param name=(.*) value=(.*)>";
        Matcher m;
        m = Pattern.compile(regex).matcher(ret);
        while (m.find()) {
            String name = m.group(1);
            String value = m.group(2);
            params.put(name, value);
        }
    }

    public String loadSignLink() throws Exception {
        BufferedReader in = new BufferedReader(new FileReader(Bot.CONFIG_DIR + "signlink"));
        String blah = null;
        while ((blah = in.readLine()) != null) {
            return blah;
        }
        return "";
    }

    public RSPanel(int world, boolean members) {
        this.world = world;
        this.members = members;
        getParams();
        try {
            setVisible(true);
            rsLoader = new RSLoader();
            String slC = loadSignLink();
            Class<?> signLink = rsLoader.loadClass(slC);
            Constructor<?> sl = signLink.getConstructors()[0];
            int i = 32;
            try {
                i += Integer.parseInt(getParameter("modewhat"));
            } catch (Exception e) {
            }
            Object slInitialized = sl.newInstance(this, i, "runescape", 29);
            Class<?> clientClass = rsLoader.loadClass("client");
            loader = (Applet) clientClass.newInstance();
            clientClass.getMethod("providesignlink", new Class[] { signLink }).invoke(null, new Object[] { slInitialized });
            loader.init();
            loader.start();
            loader.setPreferredSize(new Dimension(MainWindow.RS_WIDTH, MainWindow.RS_HEIGHT));
            setPreferredSize(new Dimension(MainWindow.RS_WIDTH, MainWindow.RS_HEIGHT));
            loader.setSize(new Dimension(MainWindow.RS_WIDTH, MainWindow.RS_HEIGHT));
            Bot.addMessage(this.getClass(), "Runescape started");
            JPopupMenu.setDefaultLightWeightPopupEnabled(false);
            loader.setStub(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Long lastHackTime = System.currentTimeMillis();

    boolean initHack = false;

    public void hackInput() {
        if (!initHack) {
            initHack = true;
            try {
                Thread.sleep(6000);
            } catch (Exception e) {
            }
        } else {
        }
        Component comp = Bot.methods.getClient().getCanvas();
        mouseManager = new Mouse(this, comp.getMouseListeners()[0], comp.getMouseMotionListeners()[0]);
        keyboardManager = new Keyboard(this, comp.getKeyListeners()[0], 70, 80);
        removeInputs();
        new Thread(mouseManager).start();
        lastHackTime = System.currentTimeMillis();
    }

    public void removeInputs() {
        Component comp = null;
        comp = Bot.methods.getClient().getCanvas();
        try {
            for (MouseListener listener : comp.getMouseListeners()) {
                comp.removeMouseListener(listener);
            }
            for (MouseMotionListener listener : comp.getMouseMotionListeners()) comp.removeMouseMotionListener(listener);
            for (KeyListener listener : comp.getKeyListeners()) comp.removeKeyListener(listener);
            for (FocusListener listener : comp.getFocusListeners()) comp.removeFocusListener(listener);
        } catch (Exception e) {
        }
        comp.addMouseListener(mouseManager);
        comp.addMouseMotionListener(mouseManager);
        comp.addKeyListener(keyboardManager);
    }

    public void appletResize(int width, int height) {
    }

    public final URL getCodeBase() {
        try {
            return new URL("http://world" + world + ".runescape.com/");
        } catch (Exception e) {
            return null;
        }
    }

    public final URL getDocumentBase() {
        try {
            return new URL("http://world" + world + ".runescape.com/");
        } catch (Exception e) {
            return null;
        }
    }

    public final String getParameter(String name) {
        return params.get(name);
    }

    public final AppletContext getAppletContext() {
        return null;
    }

    public boolean isActive() {
        return false;
    }
}
