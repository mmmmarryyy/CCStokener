package org.skyfree.ghyll.tcard.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.skyfree.ghyll.tcard.core.internal.TCard;
import org.skyfree.ghyll.tcard.core.internal.TRepository;
import org.skyfree.ghyll.tcard.core.internal.TWorkpiece;
import org.skyfree.ghyll.tcard.core.internal.TWorkspace;
import com.tools.logging.PluginLogManager;

/**
 * This class ...
 */
public class TCardCore implements BundleActivator {

    private ITWorkspace WP;

    private ITRepository REP;

    boolean initilized = false;

    private Hashtable<String, ArrayList<String>> choiceMap;

    private File meta;

    private static TCardCore plugin;

    public static final String PLUGIN_ID = "org.skyfree.ghyll.tcard.core";

    private static final String LOG_PROPERTIES_FILE = "logger.properties";

    private PluginLogManager logManager = null;

    public TCardCore() {
        plugin = this;
    }

    /**
	 * Returns the WorkSpace object. 
	 * @return
	 */
    public ITWorkspace getWorkSpace() {
        return this.WP;
    }

    /**
	 * Return the Repository Object.
	 * @return
	 */
    public ITRepository getRepository() {
        return this.REP;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        this.configure();
        initilize();
    }

    public void initilize() throws CoreException {
        this.logManager.getLogger("TCardCore").debug("Initilize Start");
        WP = new TWorkspace(StoragePath.WS);
        WP.load();
        REP = new TRepository(StoragePath.RES);
        REP.load();
        initilizeChoice();
        this.initilized = true;
        this.logManager.getLogger("TCardCore").info("Initilize Sucessfully");
    }

    private void initilizeChoice() {
        File eclipse = new File(Platform.getInstallLocation().getURL().getPath());
        File root = new File(eclipse, "data");
        this.meta = new File(root, "choice.dat");
        if (!this.meta.exists()) {
            this.choiceMap = new Hashtable<String, ArrayList<String>>();
            ArrayList<String> material = new ArrayList<String>();
            material.add("Q235");
            material.add("45#");
            material.add("H62");
            material.add("Ly12");
            material.add("1Cr18Ni9Ti");
            material.add("1Cr13");
            material.add("LN66");
            material.add("40Cr");
            material.add("42CrMo");
            choiceMap.put(ITCard.MATERIAL, material);
            ArrayList<String> operate = new ArrayList<String>();
            operate.add("��");
            operate.add("ϳ");
            operate.add("��");
            operate.add("ƽĥ");
            operate.add("��ĥ");
            operate.add("���и�");
            operate.add("ǯ");
            operate.add("��");
            operate.add("װ��");
            choiceMap.put(ITCard.OPERATE, operate);
            ArrayList<String> designer = new ArrayList<String>();
            choiceMap.put(ITCard.DESIGNER, designer);
            save2ChoiceFile();
            return;
        } else {
            loadChoiceFromFile();
        }
    }

    public void save2ChoiceFile() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(this.meta));
            oos.writeObject(this.choiceMap);
            oos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadChoiceFromFile() {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(this.meta));
            this.choiceMap = (Hashtable<String, ArrayList<String>>) ois.readObject();
            ois.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> queryChoice(String key) {
        return this.choiceMap.get(key);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }

    public static TCardCore getDefault() {
        if (plugin == null) {
            try {
                Bundle thisBundle = Platform.getBundle(TCardCore.PLUGIN_ID);
                thisBundle.start();
            } catch (BundleException e) {
                e.printStackTrace();
            }
        }
        if (!plugin.initilized) try {
            plugin.initilize();
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return plugin;
    }

    @SuppressWarnings("unchecked")
    public static Object create(Class adapter, IStoragePath storagePath) {
        if (adapter == ITWorkpiece.class) {
            return new TWorkpiece(storagePath, null);
        }
        if (adapter == ITCard.class) {
            return new TCard(storagePath, null, null, null);
        }
        return null;
    }

    ArrayList<IResourceChangedListener> listeners = new ArrayList<IResourceChangedListener>();

    public void addResourceChangedListener(IResourceChangedListener listener) {
        this.listeners.add(listener);
    }

    public void fireResourceChangedEvent(IResourceChangeEvent event) {
        Iterator<IResourceChangedListener> it = this.listeners.iterator();
        while (it.hasNext()) {
            IResourceChangedListener listener = it.next();
            if (listener != null) listener.onResourceChanged(event);
        }
    }

    private void configure() {
        Bundle thisBundle = Platform.getBundle(TCardCore.PLUGIN_ID);
        try {
            URL url = thisBundle.getEntry("/" + LOG_PROPERTIES_FILE);
            InputStream propertiesInputStream = url.openStream();
            if (propertiesInputStream != null) {
                Properties props = new Properties();
                props.load(propertiesInputStream);
                propertiesInputStream.close();
                this.logManager = new PluginLogManager(thisBundle, props);
            }
        } catch (Exception e) {
            String message = "Error while initializing log properties." + e.getMessage();
            IStatus status = new Status(IStatus.ERROR, thisBundle.getSymbolicName(), IStatus.ERROR, message, e);
            Platform.getLog(thisBundle).log(status);
            throw new RuntimeException("Error while initializing log properties.", e);
        }
    }

    public static PluginLogManager getLogManager() {
        return plugin.logManager;
    }
}
