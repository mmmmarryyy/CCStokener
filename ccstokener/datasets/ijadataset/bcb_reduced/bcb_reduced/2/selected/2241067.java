package com.safi.workshop.sqlexplorer.dbproduct;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import com.safi.db.DBConnection;
import com.safi.db.DBDriver;
import com.safi.db.DbFactory;
import com.safi.db.SafiDriverManager;
import com.safi.server.plugin.SafiServerPlugin;
import com.safi.workshop.part.SafiWorkshopEditorUtil;
import com.safi.workshop.sqlexplorer.ExplorerException;
import com.safi.workshop.sqlexplorer.plugin.SQLExplorerPlugin;
import com.safi.workshop.sqlexplorer.util.URLUtil;

/**
 * Provides access to the list of drivers, persisting their configuration in the Eclipse
 * workspace; restoring to defaults is also supported.
 * 
 * This is part of the rewrite of SQLAlias, which was originally taken from SquirrelSQL;
 * the old DriverModel (via DataCache) used parts of Squirrel which no longer exist (even
 * in the SquirrelSQL CVS on Sourceforge) and are effectively undocumented. Changes needed
 * to fix bugs relating to transactions and multiple logons per alias meant that keeping
 * the old code became unmaintainable, hence the sweeping rewrite.
 * 
 * @author John Spackman
 */
public class DriverManager {

    public static final String DRIVER = "driver";

    public static final String DRIVER_CLASS = "driver-class";

    public static final String DRIVERS = "drivers";

    public static final String ID = "id";

    public static final String JARS = "jars";

    public static final String JAR = "jar";

    public static final String NAME = "name";

    public static final String URL = "url";

    private HashMap<String, ManagedDriver> drivers = new HashMap<String, ManagedDriver>();

    private SafiDriverManager safiDriverManager;

    public DriverManager() {
    }

    public DriverManager(SafiDriverManager safiDriverManager) {
        this();
        this.safiDriverManager = safiDriverManager;
    }

    /**
   * Restores drivers to their default location
   * 
   * @throws ExplorerException
   */
    public void restoreDrivers() throws ExplorerException {
        try {
            drivers.clear();
            URL url = URLUtil.getResourceURL("default_drivers.xml");
            loadDefaultDrivers(url.openStream());
        } catch (IOException e) {
            throw new ExplorerException(e);
        }
    }

    /**
   * Loads drivers from the users preferences
   * 
   * @throws ExplorerException
   */
    public void loadDrivers() throws ExplorerException {
        loadDrivers(false);
    }

    public void loadDrivers(boolean addAliases) throws ExplorerException {
        drivers.clear();
        if (safiDriverManager != null) try {
            for (DBDriver d : safiDriverManager.getDrivers()) {
                ManagedDriver md = new ManagedDriver(d);
                addDriver(md, false);
                if (addAliases) {
                    Date now = new Date();
                    for (DBConnection conn : d.getConnections()) {
                        d.setLastModified(now);
                        d.setLastUpdated(now);
                        SQLExplorerPlugin.getDefault().getAliasManager().addAlias(new Alias(conn), false);
                    }
                }
            }
        } catch (Exception e) {
            throw new ExplorerException("Cannot load user drivers: " + e.getMessage(), e);
        }
    }

    /**
   * Loads driver definition from a given location
   * 
   * @param input
   * @throws ExplorerException
   */
    protected void loadDefaultDrivers(InputStream input) throws ExplorerException {
        try {
            SAXReader reader = new SAXReader();
            Document doc = reader.read(input);
            Element root = doc.getRootElement();
            if (root.getName().equals("Beans")) root = convertFromV3(root);
            for (Object o : root.elements(DRIVER)) {
                Element driverElem = (Element) o;
                ManagedDriver driver = new ManagedDriver(driverElem);
                addDriver(driver);
            }
        } catch (Exception e) {
            throw new ExplorerException(e);
        }
    }

    /**
   * Saves the drivers back to disk
   * 
   * @throws ExplorerException
   */
    public void saveDrivers() throws ExplorerException {
        try {
            SQLExplorerPlugin.getDefault().saveDBResources(false);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExplorerException(e);
        }
    }

    public void addDriver(ManagedDriver driver) {
        addDriver(driver, true);
    }

    /**
   * Adds a new Driver
   * 
   * @param driver
   */
    public void addDriver(ManagedDriver driver, boolean addToSafi) {
        if (driver.getId() == null || driver.getId().trim().length() == 0) throw new IllegalArgumentException("Driver has an invalid ID");
        if (drivers.get(driver.getId()) != null) throw new IllegalArgumentException("Driver with id of " + driver.getId() + " already exists");
        drivers.put(driver.getId(), driver);
        if (addToSafi) {
            DBDriver sd = DbFactory.eINSTANCE.createDBDriver();
            sd.setLastModified(new Date());
            sd.setDriverClassName(driver.getDriverClassName());
            sd.setExampleUrl(driver.getUrl());
            sd.setName(driver.getId());
            sd.setDefaultPort(driver.getDefaultPort());
            for (String jar : driver.getJars()) {
                sd.getJars().add(jar);
            }
            safiDriverManager.getDrivers().add(sd);
            driver.setDriver(sd);
            try {
                saveDrivers();
            } catch (ExplorerException e) {
                e.printStackTrace();
                MessageDialog.openError(Display.getDefault().getActiveShell(), "Save Error", "Couldn't save Safi DB Resources: " + e.getLocalizedMessage());
            }
        }
        if (SafiWorkshopEditorUtil.getSafiNavigator(false) != null) SafiWorkshopEditorUtil.getSafiNavigator(false).modelChanged(SafiServerPlugin.getDefault().isConnected());
    }

    /**
   * Removes a driver
   * 
   * @param driver
   */
    public void removeDriver(ManagedDriver driver) {
        ManagedDriver d = drivers.remove(driver.getId());
        if (d != null) {
            safiDriverManager.getDrivers().remove(d.getDriver());
            try {
                saveDrivers();
            } catch (ExplorerException e) {
                MessageDialog.openError(Display.getDefault().getActiveShell(), "Save Error", "Couldn't save Safi DB Resources: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    /**
   * Returns a driver with a given ID
   * 
   * @param id
   * @return
   */
    public ManagedDriver getDriver(String id) {
        return drivers.get(id);
    }

    public ManagedDriver getDriver(int id) {
        for (ManagedDriver d : drivers.values()) {
            if (d.getDriver().getId() == id) return d;
        }
        return null;
    }

    /**
   * Returns all the drivers
   * 
   * @return
   */
    public Collection<ManagedDriver> getDrivers() {
        return drivers.values();
    }

    /**
   * Allocates a new Unique ID for creating drivers with
   * 
   * @return
   */
    public String createUniqueId(String prefix) {
        int count = 0;
        String start = prefix;
        String id = prefix;
        while (drivers.containsKey(id = start)) {
            start = prefix + ++count;
        }
        return id;
    }

    /**
   * Converts from the old v3 format (which is a JavaBean encoding)
   * 
   * @param root
   * @return
   */
    protected Element convertFromV3(Element root) {
        Element result = new DefaultElement(DRIVERS);
        for (Object o : root.elements("Bean")) {
            Element elem = (Element) o;
            String str;
            Element driver = result.addElement(DRIVER);
            try {
                str = elem.element("identifier").elementText("string");
                driver.addAttribute(ID, str);
                str = elem.elementText("driverClass");
                if (str != null) driver.addElement(DRIVER_CLASS).setText(str);
                str = elem.elementText("name");
                driver.addElement(NAME).setText(str);
                str = elem.elementText("url");
                driver.addElement(URL).setText(str);
                Element jars = driver.addElement(JARS);
                Element jarFileNames = elem.element("jarFileNames");
                for (Object o2 : jarFileNames.elements("Bean")) {
                    Element jarBeanElem = (Element) o2;
                    str = jarBeanElem.elementText("string");
                    if (str != null && str.trim().length() > 0) jars.addElement(JAR).setText(str);
                }
            } catch (IllegalArgumentException e) {
                SQLExplorerPlugin.error("Error loading v3 driver " + driver.attributeValue(ID), e);
                throw e;
            }
        }
        return result;
    }
}
