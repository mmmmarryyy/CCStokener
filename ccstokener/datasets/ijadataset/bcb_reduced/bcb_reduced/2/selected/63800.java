package net.benojt.context;

import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import net.benojt.FractalWindow;
import net.benojt.coloring.Coloring;
import net.benojt.display.Display;
import net.benojt.iterator.Iterator;
import net.benojt.renderer.Renderer;

/**
 * an application context.
 * handler for all kinda things 
 * at the moment only programm properties are kept/loaded/saved 
 * @author frank
 *
 */
public final class Context extends Hashtable<String, Object> implements Keys, EventListener {

    static final long serialVersionUID = 493518487136L;

    private static final String cfgFileString = "benojt.cfg";

    private static final String cfgDirString = ".benojt";

    private static Context ctx;

    private static String path;

    private static final Hashtable<String, Collection<EventListener>> listeners = new Hashtable<String, Collection<EventListener>>();

    /** currently open fractal windows */
    private static final HashSet<Window> windowList = new HashSet<Window>();

    /** a menu containing an item for each open fractal window */
    private static JMenu winMenu;

    /** the shut down hook that was added at program start*/
    private static Thread shutdownHook;

    /** he font to be used in dialogs */
    private static Font dlgFont = new Font("Arial", 0, 10);

    public static Vector<Class<? extends Display>> displays;

    public static Vector<Class<? extends Renderer>> renderers;

    public static Vector<Class<? extends Coloring>> colorings;

    public static Vector<Class<? extends Iterator>> iterators;

    static {
        displays = new Vector<Class<? extends Display>>(6);
        displays.add(net.benojt.display.Anaglyph3DHitCount.class);
        displays.add(net.benojt.display.ComplexPlane.class);
        displays.add(net.benojt.display.ComplexPlaneHitCount.class);
        displays.add(net.benojt.display.ComplexPlaneBuffered.class);
        displays.add(net.benojt.display.Space3DHitCount.class);
        displays.add(net.benojt.display.Space4DHitCount.class);
        renderers = new Vector<Class<? extends Renderer>>(5);
        renderers.add(net.benojt.renderer.MultiPassRenderer.class);
        renderers.add(net.benojt.renderer.MultiThreadRenderer.class);
        renderers.add(net.benojt.renderer.MultiThreadHighPrecisionRenderer.class);
        renderers.add(net.benojt.renderer.ParameterMapRenderer.class);
        renderers.add(net.benojt.renderer.RandomPointRenderer.class);
        colorings = new Vector<Class<? extends Coloring>>(8);
        colorings.add(net.benojt.coloring.Anaglyph.class);
        colorings.add(net.benojt.coloring.BlackAndWhite.class);
        colorings.add(net.benojt.coloring.FixedPoint.class);
        colorings.add(net.benojt.coloring.GradientByHits.class);
        colorings.add(net.benojt.coloring.GradientByIterations.class);
        colorings.add(net.benojt.coloring.GradientByMagnitude.class);
        colorings.add(net.benojt.coloring.Hits.class);
        colorings.add(net.benojt.coloring.SimpleGradient.class);
        iterators = new Vector<Class<? extends Iterator>>(12);
        iterators.add(net.benojt.iterator.Buddhabrot.class);
        iterators.add(net.benojt.iterator.BurningShip.class);
        iterators.add(net.benojt.iterator.ConfigurableFormula.class);
        iterators.add(net.benojt.iterator.Henon.class);
        iterators.add(net.benojt.iterator.Julia.class);
        iterators.add(net.benojt.iterator.Lyapunov.class);
        iterators.add(net.benojt.iterator.Mandelbrot.class);
        iterators.add(net.benojt.iterator.Newton.class);
        iterators.add(net.benojt.iterator.PickoverStalks.class);
        iterators.add(net.benojt.iterator.Spider.class);
        iterators.add(net.benojt.iterator.StrangeAttractors.class);
        iterators.add(net.benojt.iterator.SymmetryInChaos.class);
    }

    private Context() {
    }

    /**
	 * returns the context for this application
	 * if neccessary a new context is created and initialized/read from the config file
	 * @return
	 */
    public static Context getContext() {
        if (ctx == null) {
            try {
                path = Context.getBenoitDir();
                File f = new File(path);
                if (!f.exists()) f.mkdirs();
                FileInputStream istream = new FileInputStream(path + System.getProperty("file.separator") + cfgFileString);
                ObjectInputStream p = new ObjectInputStream(istream);
                Object o = p.readObject();
                istream.close();
                ctx = (Context) o;
            } catch (Exception ex) {
                System.out.println("could not load context create new one");
                ctx = new Context();
            }
            Context.addEventListener(Keys.PROXY_STRING, ctx);
        }
        return ctx;
    }

    /**
	 * install a shutdown hook that is called when the program exits abnormally
	 */
    public static void installShutdownHook() {
        shutdownHook = new Thread() {

            @Override
            public void run() {
                System.out.println("Shutdown hook called --");
                long time = System.currentTimeMillis();
                int num = 1;
                for (Window fw : Context.windowList) {
                    if (fw.getClass() != FractalWindow.class) continue;
                    String fileName = Context.getBenoitDir() + System.getProperty("file.separator") + FractalWindow.SaveDir + System.getProperty("file.separator") + "shutdown_" + time + "#" + num++ + ".bjf";
                    ((FractalWindow) fw).saveXML(new File(fileName));
                    System.out.println("saved - " + fileName);
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
	 * save property-hashtble to disk
	 *
	 */
    public void savePersistProp() {
        try {
            FileOutputStream ostream = new FileOutputStream(path + System.getProperty("file.separator") + cfgFileString);
            ObjectOutputStream p = new ObjectOutputStream(ostream);
            p.writeObject(ctx);
            p.flush();
            ostream.close();
        } catch (Exception ex) {
            System.out.println("could not save config");
        }
    }

    /**
	 * puts the new property into the hashtable.
	 * @param key
	 * @param value
	 */
    public synchronized void setProperty(String key, Object value) {
        if (value == null) this.remove(key); else if (key == null) System.out.println("key null not allowed"); else {
            this.put(key, value);
            this.savePersistProp();
            fireEvent(key);
        }
    }

    /**
	 * remove a property from the context
	 * @param key
	 */
    public synchronized void removeProperty(String key) {
        if (key == null) System.out.println("key null not allowed");
        this.remove(key);
        this.savePersistProp();
        fireEvent(key);
    }

    /**
	 * get property from hashtable
	 * if property does not exist then a default value for known properties is returned
	 * @param property property key
	 * @return
	 */
    public Object getProperty(String key) {
        if (key == null) return null;
        Object res = null;
        Object o = this.get(key);
        if (o != null) res = o;
        return res;
    }

    /**
	 * get a property or return default. 
	 * if value is null or class is not equal to default class 
	 * then return some default value
	 * @param key
	 * @param def
	 * @return
	 */
    public Object getProperty(String key, Object def) {
        Object res = def;
        Object o = getProperty(key);
        if (def != null) {
            if (o != null && def.getClass().isInstance(o)) res = o;
            if (o == null) {
                setProperty(key, def);
            }
        }
        return res;
    }

    /**
	 * tests if property with key key has value value<BR>
	 * defaults for not defined properties to false
	 * @param key
	 * @param value
	 * @return null if key is null or value!=property of key; else true
	 */
    public boolean getPropertyEquals(String key, Object value) {
        boolean res;
        if (key != null) {
            if (value == null) res = this.getProperty(key) == null; else {
                res = value.equals(this.getProperty(key));
            }
        } else res = false;
        return res;
    }

    /**
	 * create a reader for file in jar or file system
	 * @param rPath
	 * @return
	 */
    public static Reader getReader(String rPath) {
        try {
            URL url = getResource(rPath);
            if (url != null) return new InputStreamReader(url.openStream());
            File file = new File(rPath);
            if (file.canRead()) return new FileReader(file);
        } catch (Exception ex) {
            System.out.println("could not create reader for " + rPath);
        }
        return null;
    }

    /**
	 * get url of file in jar or file system
	 * @param resPath
	 * @return
	 */
    public static URL getResource(String resPath) {
        try {
            URL url = Context.class.getResource("/" + resPath);
            if (url != null) return url;
            File f = new File(resPath);
            if (f.canRead()) return f.toURI().toURL();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void addWindowToList(Window fw) {
        if (!windowList.contains(fw)) {
            windowList.add(fw);
            winMenu = null;
        } else {
        }
    }

    public static void removeWindowFromList(Window fw) {
        if (windowList.remove(fw)) {
            winMenu = null;
            fw.dispose();
            if (windowList.isEmpty() && shutdownHook != null) Context.exit(0);
        } else {
            System.out.println("could no find window in list " + fw);
        }
    }

    /**
	 * create and return the menu containing the open fractal windows
	 * @return
	 */
    public static JMenu getWindowsMenu() {
        if (winMenu == null) {
            winMenu = new JMenu("Windows");
            for (final Window fw : windowList) {
                String title = "unknown";
                if (fw instanceof java.awt.Dialog) title = ((Dialog) fw).getTitle(); else if (fw instanceof Frame) title = ((Frame) fw).getTitle();
                JMenuItem wi = new JMenuItem(title);
                wi.addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        if (fw instanceof Frame) ((Frame) fw).setExtendedState(Frame.NORMAL);
                        fw.toFront();
                        fw.setVisible(true);
                    }
                });
                winMenu.add(wi);
            }
        }
        return winMenu;
    }

    public static void clearWinMenu() {
        winMenu = null;
    }

    /**
	 * the benojt config dir containing configurations, 
	 * iterator classes and iterator source files
	 * @return
	 */
    public static String getBenoitDir() {
        return System.getProperties().getProperty("user.home") + System.getProperty("file.separator") + cfgDirString;
    }

    public static synchronized void addEventListener(String key, EventListener listener) {
        Collection<EventListener> l = listeners.get(key);
        if (l == null) {
            l = new Vector<EventListener>();
            listeners.put(key, l);
        }
        l.add(listener);
    }

    public static synchronized void removeEventListener(String key, EventListener listener) {
        Collection<EventListener> l = listeners.get(key);
        if (l != null) l.remove(listener);
    }

    public static synchronized void fireEvent(String key) {
        fireEvent(key, null, null);
    }

    public static synchronized void fireEvent(String key, Object sender, Object data) {
        Collection<EventListener> l = listeners.get(key);
        if (l != null) for (EventListener listener : l) {
            listener.eventFired(new Event(key, sender, data));
        }
    }

    public static Font getDlgFont() {
        return dlgFont;
    }

    /**
	 * executes javac -version and returns first output line containing version.
	 * @return null if execution failed 
	 */
    public static String checkCompiler() {
        String javac = "javac";
        try {
            Object o = getContext().getProperty(Keys.JAVA_COMPILER);
            if (o != null) javac = (String) o;
            String command = javac + " -version";
            Process pr = Runtime.getRuntime().exec(command);
            BufferedReader br = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
            String firstLine = br.readLine();
            do {
            } while (br.readLine() != null);
            return firstLine;
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
        return null;
    }

    /**
	 * exit benojt savely.
	 * @param arg exit arg
	 */
    public static void exit(int arg) {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
        System.out.println("shut down Benojt");
        System.exit(arg);
    }

    /**
	 * tests if a class can be loaded.
	 * @param cls
	 * @return
	 */
    public static boolean isLoadable(String clsName) {
        try {
            Class.forName(clsName);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    public static void readProxyConfig() {
        String proxyString = (String) Context.getContext().getProperty(Keys.PROXY_STRING);
        if (proxyString == null) {
            proxyString = "";
            Context.getContext().setProperty(Keys.PROXY_STRING, proxyString);
        }
        String proxyHost = "";
        String proxyPort = "";
        int colPos = proxyString.indexOf(":");
        if (proxyString.length() > 2 && colPos > 1 && colPos < proxyString.length() - 1) {
            proxyHost = proxyString.substring(0, colPos);
            proxyPort = proxyString.substring(colPos + 1);
        }
        System.getProperties().put("proxySet", (proxyHost.length() == 0 || proxyPort.length() == 9) ? "false" : "true");
        System.getProperties().put("proxyHost", proxyHost);
        System.getProperties().put("proxyPort", proxyPort);
    }

    public void eventFired(Event event) {
        if (event.getKey().equals(Keys.PROXY_STRING)) {
            readProxyConfig();
        }
    }
}
