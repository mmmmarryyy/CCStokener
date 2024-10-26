package com.mediasol.commons.log4j;

import java.net.URL;
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author david
 *
 */
public class Activator extends Plugin {

    private static final String CFG_EXT_POINT = "com.mediasol.commons.log4j.configuration";

    private static Activator plugin;

    /**
	 * 
	 */
    public Activator() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        IStringVariableManager mgr = VariablesPlugin.getDefault().getStringVariableManager();
        Properties properties = new Properties();
        for (IConfigurationElement e : Platform.getExtensionRegistry().getConfigurationElementsFor(CFG_EXT_POINT)) {
            Properties local = new Properties();
            String url = e.getAttribute("url");
            if (url != null) {
                local.load(new URL(url).openStream());
            }
            String res = e.getAttribute("resource");
            if (res != null) {
                Bundle b = Platform.getBundle(e.getDeclaringExtension().getNamespaceIdentifier());
                local.load(b.getEntry(res).openStream());
            }
            for (Object k : local.keySet()) {
                String key = (String) k;
                String val = local.getProperty(key);
                if (val == null) continue;
                val = mgr.performStringSubstitution(val);
                properties.setProperty(key, val);
            }
        }
        PropertyConfigurator.configure(properties);
    }

    public static Activator getDefault() {
        return plugin;
    }
}
