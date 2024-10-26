package org.extwind.osgi.ebr.internal.impl;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.extwind.osgi.ebr.Ebr;
import org.extwind.osgi.ebr.EbrBundle;
import org.extwind.osgi.ebr.internal.EbrLoader;
import org.osgi.framework.Version;

/**
 * @author donf.yang
 * 
 */
public class EbrImpl implements Ebr {

    private Map<String, EbrBundle> bundles = new HashMap<String, EbrBundle>();

    private static Log logger = LogFactory.getLog(EbrImpl.class);

    private String location;

    private boolean load;

    protected long lastModify = -1;

    private String name;

    private String description;

    public EbrImpl(String location) {
        this.location = location;
    }

    @Override
    public EbrBundle findBundle(String name, Version version) {
        return bundles.get(name + "_" + version.toString());
    }

    @Override
    public EbrBundle[] findBundles(String filter) {
        return bundles.values().toArray(new EbrBundle[0]);
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public long getLastModify() {
        return lastModify;
    }

    @Override
    public boolean isLoaded() {
        return load;
    }

    @Override
    public boolean isOutOfSync() {
        try {
            URL url = new URL(location);
            return lastModify != url.openConnection().getLastModified();
        } catch (Exception e) {
            logger.warn(e);
            return false;
        }
    }

    @Override
    public synchronized void load() throws Exception {
        EbrLoader loader = createEbrLoader();
        lastModify = loader.load();
        this.name = loader.getEbrName();
        this.description = loader.getEbrDescription();
        this.bundles = loader.getBundles();
        load = true;
    }

    @Override
    public synchronized boolean update() throws Exception {
        load();
        return true;
    }

    protected EbrLoader createEbrLoader() {
        EbrLoader loader = new EbrLoaderImpl(this, location);
        return loader;
    }
}
