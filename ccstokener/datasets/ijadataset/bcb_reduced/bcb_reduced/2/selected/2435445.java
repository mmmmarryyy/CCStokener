package edu.mit.osidimpl.manager;

/**
 *  <p>
 *  Implements the OsidManager. OSID managers sublass this to create their own
 *  implementations. This is simply a convenient way to stash common methods.
 *
 *  </p><p>
 *  CVS $Id: OsidManagerWithCascadingProperties.java,v 1.5 2008/02/25 15:11:14 jeffkahn Exp $
 *  </p>
 *  
 *  @author  Tom Coppeto
 *  @version $OSID: 2.0$ $Revision: 1.5 $
 *  @see     org.osid.OsidManager
 */
public class OsidManagerWithCascadingProperties extends OsidManager {

    private java.util.Properties configuration = null;

    /**
     *  The manager will fetch its own properties
     *  but we'll do it here and ignore the passed in configuration. This is
     *  invoked by the OsidLoader after the manager is created and is as good
     *  as place as any. I prefer it to creating a constructor that may
     *  throw an exception the OsidLoader isn't prepared to deal with.
     *
     *  @param configuration the configuration that is being ignored
     */
    public void assignConfiguration(java.util.Properties configuration) throws org.osid.OsidException {
        this.configuration = configuration;
        loadConfiguration();
        initialize();
        return;
    }

    protected void loadConfiguration() throws org.osid.OsidException {
        String className = getClass().getName();
        java.util.Vector elements = new java.util.Vector();
        int index = 0;
        String app = (String) getOsidContext().getContext("context");
        if (app != null) {
            className += "." + app;
        }
        while (index != -1) {
            index = className.indexOf(".", index == 0 ? 0 : index + 1);
            if (index != -1) {
                String token = className.substring(0, index);
                elements.add(token);
            }
        }
        elements.add(className);
        java.net.URL url_op = null;
        try {
            String configurationFile = this.configuration.getProperty("configurationFile");
            if (configurationFile != null) {
                System.out.println("location of osidProperties.xml? " + configurationFile);
                storeConfigurationXMLFile(configurationFile, "osid");
            } else {
                url_op = getClass().getClassLoader().getResource("osidProperties.xml");
                storeConfigurationXMLFile(url_op, "osid");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        int size = elements.size();
        for (int i = 0; i < size; i++) {
            try {
                java.net.URL url = this.getClass().getResource(elements.elementAt(i) + ".properties");
                storeConfigurationPropertiesFile(url, (String) elements.elementAt(i));
            } catch (Exception e) {
            }
        }
        return;
    }

    private void storeConfigurationPropertiesFile(java.net.URL url, String comp) {
        java.util.Properties p;
        try {
            p = new java.util.Properties();
            p.load(url.openStream());
        } catch (java.io.IOException ie) {
            System.err.println("error opening: " + url.getPath() + ": " + ie.getMessage());
            return;
        }
        storeConfiguration(p, comp);
        return;
    }

    private void storeConfigurationXMLFile(String filePath, String comp) {
        java.util.Properties p;
        try {
            p = new java.util.Properties();
            p.loadFromXML(new java.io.FileInputStream(filePath));
        } catch (java.io.IOException ie) {
            System.err.println("error opening: " + filePath + ": " + ie.getMessage());
            return;
        }
        storeConfiguration(p, comp);
        return;
    }

    private void storeConfigurationXMLFile(java.net.URL url, String comp) {
        java.util.Properties p;
        try {
            p = new java.util.Properties();
            p.loadFromXML(url.openStream());
        } catch (java.io.IOException ie) {
            System.err.println("error opening: " + url.getPath() + ": " + ie.getMessage());
            return;
        }
        storeConfiguration(p, comp);
        return;
    }

    private void storeConfiguration(java.util.Properties p, String comp) {
        java.util.Enumeration keys = p.propertyNames();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String value = p.getProperty(key);
            String prefix = "";
            int i = key.lastIndexOf('.');
            if (i != -1) {
                prefix = key.substring(0, i);
            }
            if (comp.equals("osid")) {
            } else if (prefix.length() == 0) {
                key = comp + "." + key;
            } else if (comp.startsWith(prefix) && (comp.equals(prefix) == false)) {
                System.err.println("error in properties file: " + comp);
                System.err.println("property " + key + " is out of scope");
                continue;
            } else if (comp.equals(prefix) || prefix.startsWith(comp)) {
            } else {
                System.err.println("error in properties file: " + comp);
                System.err.println("property " + key + " is broken");
                continue;
            }
            this.configuration.setProperty(key, value);
        }
        return;
    }

    protected String getConfiguration(String key) {
        if (key == null) {
            return (null);
        }
        org.osid.OsidContext context = null;
        String className = getClass().getName();
        try {
            context = getOsidContext();
        } catch (org.osid.OsidException oe) {
        }
        if (context != null) {
            try {
                String app = (String) context.getContext("context");
                if (app != null) {
                    className += "." + app;
                }
            } catch (org.osid.OsidException oe) {
            }
        }
        if (this.configuration != null) {
            String value = this.configuration.getProperty(className + "." + key);
            if (value != null) {
                return (value);
            }
            String path = className;
            int index = 0;
            while (index != -1) {
                index = path.lastIndexOf(".");
                if (index != -1) {
                    path = path.substring(0, index);
                    value = this.configuration.getProperty(path + "." + key);
                    if (value != null) {
                        return (value);
                    }
                } else {
                    value = this.configuration.getProperty(key);
                    if (value != null) {
                        return (value);
                    }
                }
            }
        }
        return (null);
    }
}
