package com.sun.spot.spotworld;

import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;
import javax.microedition.io.DatagramConnection;
import com.sun.spot.client.SPOTWorldCommands;
import com.sun.spot.client.command.HelloCommand;
import com.sun.spot.client.command.HelloResult;
import com.sun.spot.client.command.HelloResultList;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.ConfigPage;
import com.sun.spot.peripheral.radio.ILowPan;
import com.sun.spot.spotworld.common.LocaleUtil;
import com.sun.spot.spotworld.common.SPOTWorldLoader;
import com.sun.spot.spotworld.gui.IRadioIndicator;
import com.sun.spot.spotworld.gui.ISpotWorldViewer;
import com.sun.spot.spotworld.gui.SpotWorldPortal;
import com.sun.spot.spotworld.participants.Application;
import com.sun.spot.spotworld.participants.ESpotBasestation;
import com.sun.spot.spotworld.participants.Group;
import com.sun.spot.spotworld.participants.SquawkHost;
import com.sun.spot.spotworld.participants.SunSPOT;
import com.sun.spot.spotworld.virtualobjects.IVirtualObject;
import com.sun.spot.spotworld.virtualobjects.VirtualObject;
import com.sun.spot.suiteconverter.Suite;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import com.sun.squawk.security.signing.SigningService;

/**
 * a collection of Proxies to your Sun SPOTs.
 * Note SPOTWorld isn't a GUI, that is independent. See SPOTWorldPortal
 *
 * major aspects of a world are
 * 
 *   - broadcasts and collects replies from SPOTs. Once received tries to find the appropriate proxy class to instantiate
 *      and add the resulting instance to its collection.
 *   - looks for jar files defining proxies and dynamically loads them from the virtualObjects/ directory.
 *      This would be a way to make your own kind of device apper in SPOTWorld.
 *
 * @author randy, arshan, robert, vipul
 */
public class SpotWorld extends VirtualObject {

    Vector<ISpotWorldViewer> viewers = new Vector<ISpotWorldViewer>();

    protected IRadioIndicator basestation;

    RadiogramConnection SPOTCastsRxConn;

    protected static int desktopLibraryHash = 0;

    protected static String desktopSDKversion = null;

    protected static String spotfinderPath = null;

    protected static String buildXMLPath = null;

    protected DatagramConnection SPOTCastTxConn;

    boolean SPOTCastTxing = false;

    boolean SPOTHelloing = false;

    protected int discoveryTimeout = 3000;

    protected int discoveryHopCount = ILowPan.DEFAULT_HOPS;

    protected int discoveryPanId = 3;

    protected int discoveryChannel = 26;

    protected String lastAddress = "";

    protected boolean readyToAddVirtualObjects = false;

    protected static byte SPOTWORLD_BEACON_PACKET = 0x0;

    protected static byte SPOTWORLD_REPLY_PACKET = 0x1;

    RadiogramConnection spotCastReplyConn = null;

    protected static boolean isSocketServerRunning = false;

    protected long spotWorldID = Calendar.getInstance().getTimeInMillis();

    protected Vector<Vector<String>> voClassesWithViews = null;

    protected Vector<String> knownMACAddresses = new Vector<String>();

    protected SPOTWorldLoader loader = new SPOTWorldLoader();

    public SpotWorld() {
    }

    public static String getBuildXMLPath() {
        if (buildXMLPath != null) return buildXMLPath;
        Properties p = getSunspotProperties();
        try {
            String SDKHome = p.getProperty("sunspot.home");
            File sf = new File(SDKHome + File.separator + "build.xml");
            buildXMLPath = SDKHome + File.separator + "build.xml";
        } catch (Exception e) {
            System.err.println("Encountered exception in " + "getBuildXMLPath: " + e);
            e.printStackTrace();
        }
        return buildXMLPath;
    }

    public static String getSpotfinderPath() {
        if (spotfinderPath != null) return spotfinderPath;
        Properties p = getSunspotProperties();
        try {
            String SDKHome = p.getProperty("sunspot.home");
            File sf = new File(SDKHome + File.separator + "bin" + File.separator + "spotfinder");
            spotfinderPath = SDKHome + File.separator + "bin" + File.separator + "spotfinder";
        } catch (Exception e) {
            System.err.println("Encountered exception in " + "getSpotfinderPath: " + e);
            e.printStackTrace();
        }
        return spotfinderPath;
    }

    public static String getDesktopSDKVersion() {
        if (desktopSDKversion != null) return desktopSDKversion;
        Properties p = getSunspotProperties();
        try {
            String SDKHome = p.getProperty("sunspot.home");
            p.load(new FileInputStream(new File(SDKHome, "version.properties")));
            desktopSDKversion = p.getProperty("version.datestamp");
        } catch (Exception e) {
            System.err.println("Encountered exception in " + "getDesktopSDKVersion: " + e);
            e.printStackTrace();
        }
        return desktopSDKversion;
    }

    public static int getDesktopLibraryHash() {
        if (desktopLibraryHash != 0) return desktopLibraryHash;
        int[] memoryAddrs = new int[] { ConfigPage.LIBRARY_VIRTUAL_ADDRESS, ConfigPage.BOOTSTRAP_ADDRESS };
        Suite suite = new Suite();
        Properties p = getSunspotProperties();
        try {
            String SDKHome = p.getProperty("sunspot.home");
            String libName = p.getProperty("spot.library.name");
            String squawkSuite = SDKHome + File.separator + "arm" + File.separator + "squawk.suite";
            String libSuite = SDKHome + File.separator + "arm" + File.separator + libName + ".suite";
            suite.loadFromFile(libSuite, squawkSuite, memoryAddrs);
            return suite.getHash();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static byte[] getDesktopOwnerId() {
        byte[] keyBytes = null;
        try {
            keyBytes = SigningService.getInstance().getPublicKeyBytes();
        } catch (Exception e) {
            System.err.println("Encountered " + e + " while retrieving owner Id.");
        }
        return keyBytes;
    }

    public static Properties getSunspotProperties() {
        Properties p = new Properties();
        String userHome = System.getProperty("user.home");
        try {
            p.load(new FileInputStream(new File(userHome, ".sunspot.properties")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return p;
    }

    public static Properties getDefaultProperties() {
        Properties ssp = getSunspotProperties();
        Properties p = new Properties();
        try {
            String SDKHome = ssp.getProperty("sunspot.home");
            p.load(new FileInputStream(new File(SDKHome, "default.properties")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return p;
    }

    public void addMACAddress(String address) {
        knownMACAddresses.addElement(address);
    }

    public boolean deleteMACAddress(String address) {
        boolean res = knownMACAddresses.removeElement(address);
        msg("Removal of " + address + " returned " + res);
        return res;
    }

    public boolean isKnownMACAddress(String address) {
        return knownMACAddresses.contains(address);
    }

    public void init() {
        super.init();
        Runnable rnnbl3 = new Runnable() {

            public void run() {
                boolean shouldWait = !isReadyToAddVirtualObjects();
                while (shouldWait) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    shouldWait = !isReadyToAddVirtualObjects();
                }
                Group group = new Group("All");
                addVirtualObject(group);
                listenForParticipants();
                msg("SPOTWorld started");
            }
        };
        (new Thread(rnnbl3)).start();
    }

    public String getUniqueID() {
        return "" + spotWorldID;
    }

    protected void listenForParticipants() {
        startHelloThread();
    }

    public void dummy() {
        Runnable r = new Runnable() {

            public void run() {
            }
        };
        (new Thread(r)).start();
    }

    public Vector<ESpotBasestation> getBasestations() {
        Vector<ESpotBasestation> result = new Vector<ESpotBasestation>();
        for (IVirtualObject vo : getVirtualObjectsCopy()) {
            if (vo instanceof ESpotBasestation) {
                result.add((ESpotBasestation) vo);
            }
        }
        return result;
    }

    public Vector<Group> getGroups() {
        Vector<Group> r = new Vector<Group>();
        for (IVirtualObject vo : getVirtualObjectsCopy()) {
            if (vo instanceof Group) {
                r.add((Group) vo);
            }
        }
        return r;
    }

    public Group getGroupNamed(String nm) {
        for (Group g : getGroups()) {
            if (g.getName().equals(nm)) {
                return g;
            }
        }
        return null;
    }

    public void removeVirtualObject(IVirtualObject obj) {
        super.removeVirtualObject(obj);
    }

    public void addVirtualObject(IVirtualObject obj) {
        msg(LocaleUtil.getString("adding new virtual object") + " : " + obj + " and notifying all viewers.");
        boolean shouldWait = !isReadyToAddVirtualObjects();
        int waitCount = 0;
        while (shouldWait && waitCount++ < 10) {
            try {
                Thread.sleep(1000);
                Thread.yield();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            if (waitCount == 5) msg("[SpotWorld > addVirtualObject()] Warning: trying to add " + obj + " but not ready after " + waitCount + " tries.");
            shouldWait = !isReadyToAddVirtualObjects();
        }
        if (waitCount >= 10) {
            msg("Giving up after " + waitCount + " tries, " + obj + " is not added.");
            return;
        }
        super.addVirtualObject(obj);
        Iterator<ISpotWorldViewer> iterator = viewers.iterator();
        ISpotWorldViewer viewer;
        while (iterator.hasNext()) {
            viewer = (ISpotWorldViewer) iterator.next();
            viewer.addVirtualObject(obj);
        }
        if (obj instanceof SquawkHost) {
            Group gp = getGroupNamed("All");
            if (gp != null) gp.addElement(obj);
            ((SquawkHost) obj).getRunningApps();
        }
    }

    public boolean isReadyToAddVirtualObjects() {
        return readyToAddVirtualObjects;
    }

    public void setReadyToAddVirtualObjects(boolean readyToAddVirtualObjects) {
        this.readyToAddVirtualObjects = readyToAddVirtualObjects;
    }

    protected Map<String, String> views = null;

    /**
     *  Looks in SPOTWorld/views directory for available views to use
     *  loads the jars into SPOTWorld (but not run them) and adds their
     *  names into a data structure.
     */
    protected void findAvailableViews() {
        File dir = new File(System.getProperty("user.dir") + "/views");
        File[] files = dir.listFiles();
        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File file, String name) {
                return name.endsWith("jar");
            }
        };
        files = dir.listFiles(filter);
        views = new HashMap<String, String>();
        URL[] urls = new URL[files.length];
        for (int i = 0; i < urls.length; i++) {
            try {
                urls[i] = new URL("jar:file:" + files[i] + "!/");
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            }
        }
        loader.addURLs(urls);
        for (URL url : urls) {
            try {
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                String name = conn.getManifest().getMainAttributes().getValue("View-Name");
                String shortName = conn.getManifest().getMainAttributes().getValue("SPOTWorld-Name");
                views.put(shortName, name);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public Vector<Application> getApplications() {
        Vector<Application> reply = new Vector<Application>();
        for (IVirtualObject vo : getVirtualObjectsCopy()) {
            if (vo instanceof Application) {
                reply.add((Application) vo);
            }
        }
        ;
        return reply;
    }

    /**
     *  Returns the available views. The available views are the jar files in
     *  the SPOTWOrld/view dir and are loaded at run-time.
     */
    public Vector<String> getAvailableViews() {
        if (views == null) findAvailableViews();
        return new Vector(views.keySet());
    }

    protected HashMap<String, Component> viewCache = new HashMap<String, Component>();

    /**
     *  Returns requested view as a component.
     *
     *  @param viewName name of the view.
     */
    public Component getView(String viewName) {
        if (views == null) findAvailableViews();
        try {
            if (viewCache.get(viewName) == null) {
                msg("About to load the following class: " + views.get(viewName) + " associated with the name " + viewName);
                Class c = loader.loadClass(views.get(viewName));
                msg("Loaded class was " + c);
                Constructor constructor = c.getConstructor(SpotWorld.class);
                msg("Constructor was " + constructor);
                msg(" new instance gives " + constructor.newInstance(this));
                ISpotWorldViewer view = (ISpotWorldViewer) constructor.newInstance(this);
                for (Vector<String> classNViews : getVOClassesWithViews()) {
                    for (String voItem : classNViews) {
                        view.addVirtualObjectMapping(loader.loadClass(classNViews.firstElement()), loader.loadClass(voItem));
                    }
                }
                viewers.add(view);
                for (IVirtualObject vo : getVirtualObjectsCopy()) {
                    view.addVirtualObject(vo);
                }
                Component holder = view.getViewHolder();
                holder.setName(viewName);
                viewCache.put(viewName, holder);
                return holder;
            } else {
                return viewCache.get(viewName);
            }
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        } catch (SecurityException ex) {
            ex.printStackTrace();
        } catch (InvocationTargetException ex) {
            System.err.println("InvocationTargetException Cause = " + ex.getCause() + " target exception = " + ex.getTargetException());
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     *  Finds all available virtual object jars
     *
     */
    protected void initVOClassesWithViews() {
        File dir = new File(System.getProperty("user.dir") + "/virtualObjects");
        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File file, String name) {
                return name.endsWith("jar");
            }
        };
        File[] files = dir.listFiles(filter);
        voClassesWithViews = new Vector<Vector<String>>();
        URL[] urls = new URL[files.length];
        for (int i = 0; i < urls.length; i++) {
            try {
                urls[i] = new URL("jar:file:" + files[i] + "!/");
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            }
        }
        loader.addURLs(urls);
        for (URL url : urls) {
            try {
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                Vector<String> vo = new Vector<String>();
                vo.addElement(conn.getManifest().getMainAttributes().getValue("VO-Name"));
                String supportedViews = conn.getManifest().getMainAttributes().getValue("Supported-Views");
                String[] viewNames = supportedViews.split(",");
                for (String viewName : viewNames) vo.addElement(viewName.trim());
                voClassesWithViews.addElement(vo);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public Vector<Vector<String>> getVOClassesWithViews() {
        if (voClassesWithViews == null) initVOClassesWithViews();
        return voClassesWithViews;
    }

    public void startHelloThread() {
        String dos = SpotWorldPortal.getSPOTWorldProperty("discover.on.startup");
        String audis = SpotWorldPortal.getSPOTWorldProperty("auto.discovery.enabled");
        String period = SpotWorldPortal.getSPOTWorldProperty("auto.discovery.period");
        if ((dos != null) && dos.equalsIgnoreCase("false")) {
            System.err.println(" *** WARNING *** : auto-discovery on " + "startup is turned off.");
            return;
        }
        SPOTHelloing = true;
        Runnable r = new Runnable() {

            public void run() {
                Random rand = new Random();
                while (SPOTHelloing) {
                    SPOTHelloing = false;
                    discover();
                    int extraTime = rand.nextInt(2000);
                    Utils.sleep(10000 + extraTime);
                }
            }
        };
        (new Thread(r)).start();
    }

    public HelloResultList discover() {
        return discover("broadcast");
    }

    public HelloResultList discover(String destination) {
        Object ret = null;
        String address = null;
        String whitelist = SpotWorldPortal.getSPOTWorldProperty("spots.whitelist");
        if (whitelist != null) whitelist = whitelist.toUpperCase();
        try {
            if (!SPOTWorldCommands.baseStationAvailable()) {
                System.err.println("No basestation available!");
                return null;
            }
            msg("Discovering ... " + destination);
            getBasestation().showAsBusy();
            ret = SPOTWorldCommands.doCommand(null, HelloCommand.NAME, "" + getDiscoveryTimeout() + " " + getDiscoveryHopCount() + " " + destination);
            if (ret != null) {
                msg("Response\n" + ret);
                HelloResultList hrl = (HelloResultList) ret;
                for (int i = 0; i < hrl.size(); i++) {
                    HelloResult hr = hrl.getResult(i);
                    address = IEEEAddress.toDottedHex(hr.remoteAddress).toUpperCase();
                    if ((whitelist == null) || (whitelist.indexOf(address) != -1)) {
                        addVirtualObject(address, hr);
                    } else {
                        System.err.println("Ignoring SPOT <" + address + ">.");
                    }
                }
            } else {
                msg("Null response");
                return null;
            }
        } catch (Exception ex) {
            return null;
        } finally {
            getBasestation().showAsNoLongerBusy();
        }
        return (HelloResultList) ret;
    }

    public void addVirtualObject(String address, HelloResult hr) {
        String deviceType = hr.hardwareMajorRevision;
        if (isKnownMACAddress(address)) {
            msg("In hello reply, SPOTWorld already contained: " + deviceType + ", " + address);
            for (IVirtualObject sh : getVirtualObjectsCopy()) {
                if (sh.getUniqueID().equals(address)) {
                    try {
                        if (sh instanceof SunSPOT) {
                            ((SunSPOT) sh).initFromHelloResult(hr);
                            ((SunSPOT) sh).refresh();
                            ((SunSPOT) sh).notifyUIObjects();
                        } else {
                            msg("Ignoring object in addVirtualObject1");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } else {
            msg("In hello reply, SPOTWorld deems a new object: " + deviceType + ", " + address + " ...");
            addMACAddress(address);
            for (Vector<String> vec : getVOClassesWithViews()) {
                for (String participantName : vec) {
                    String[] shortName = participantName.split("\\.");
                    if (shortName[shortName.length - 1].toLowerCase().equals(deviceType.toLowerCase())) {
                        msg(" ..... identifed " + address + " as a " + participantName);
                        try {
                            Class vo = Class.forName(participantName);
                            Constructor constructor = vo.getConstructor(new Class[] { String.class });
                            IVirtualObject device = (IVirtualObject) constructor.newInstance(address);
                            if (device instanceof SunSPOT) {
                                ((SunSPOT) device).initFromHelloResult(hr);
                            }
                            addVirtualObject(device);
                            try {
                                device.refresh();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        } catch (ClassNotFoundException ex) {
                            ex.printStackTrace();
                        } catch (SecurityException ex) {
                            ex.printStackTrace();
                        } catch (NoSuchMethodException ex) {
                            ex.printStackTrace();
                        } catch (IllegalArgumentException ex) {
                            ex.printStackTrace();
                        } catch (InvocationTargetException ex) {
                            ex.printStackTrace();
                        } catch (InstantiationException ex) {
                            ex.printStackTrace();
                        } catch (IllegalAccessException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public void startSPOTSocket() {
        if (!isSocketServerRunning) {
            isSocketServerRunning = true;
            (new Thread(new Runnable() {

                public void run() {
                    while (true) {
                        try {
                            ServerSocket serverSocket = new ServerSocket(3000);
                            (new SPOTSocketHandler(serverSocket.accept())).start();
                            serverSocket.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            })).start();
            msg("Server socket has started");
        } else msg("Server socket is already running");
    }

    class SPOTSocketHandler extends Thread {

        private Socket socket = null;

        public SPOTSocketHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            BufferedReader in = null;
            PrintWriter out = null;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream());
                String msg = in.readLine();
                msg("Read in : " + msg);
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    out.close();
                    in.close();
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public int getDiscoveryTimeout() {
        return discoveryTimeout;
    }

    public void setDiscoveryTimeout(int discoveryTimeout) {
        this.discoveryTimeout = discoveryTimeout;
    }

    public int getDiscoveryHopCount() {
        return discoveryHopCount;
    }

    public void setDiscoveryHopCount(int hc) {
        discoveryHopCount = hc;
    }

    public int getDiscoveryChannel() {
        return discoveryChannel;
    }

    public void setDiscoveryChannel(int channel) {
        discoveryChannel = channel;
    }

    public int getDiscoveryPanId() {
        return discoveryPanId;
    }

    public void setDiscoveryPanId(int panId) {
        discoveryPanId = panId;
    }

    public String getSPOTAddress() {
        if (lastAddress.equals("")) return "0014.4F01.0000.0000";
        return lastAddress;
    }

    public void setSPOTAddress(String addr) {
        lastAddress = addr;
    }

    public void showBasestationAsBusy() {
        if (basestation != null) basestation.showAsBusy();
    }

    public void showBasestationAsNoLongerBusy() {
        if (basestation != null) basestation.showAsNoLongerBusy();
    }

    public IRadioIndicator getBasestation() {
        return basestation;
    }

    public void setBasestation(IRadioIndicator basestation) {
        this.basestation = basestation;
    }
}
