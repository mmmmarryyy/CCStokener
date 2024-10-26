package org.cloudlet.web.boot.server;

import com.google.gwt.inject.client.GinModule;
import com.google.gwt.inject.rebind.adapter.GinModuleAdapter;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.logging.Logger;

public final class BootModule extends AbstractModule {

    private final Logger logger = Logger.getLogger(getClass().getName());

    public static final String INT_PROPERTIES = "/ini.properties";

    public static Properties props = new Properties();

    @Override
    protected void configure() {
        try {
            URL url = BootModule.class.getResource(INT_PROPERTIES);
            InputStream in = url.openStream();
            props.load(in);
            logger.config("load init properties " + url);
            Names.bindProperties(binder(), props);
        } catch (Exception e) {
            logger.finest("classpath:/init.properties not exist, use out-of-box values");
        }
        loadFromClasspath();
    }

    private void loadFromClasspath() {
        ServiceLoader<Module> modules = ServiceLoader.load(Module.class);
        Iterator<Module> moduleIt = modules.iterator();
        while (moduleIt.hasNext()) {
            Module module = moduleIt.next();
            logger.finer("Install " + module.getClass().getName());
            install(module);
        }
        ServiceLoader<GinModule> ginModules = ServiceLoader.load(GinModule.class);
        Iterator<GinModule> ginModuleItr = ginModules.iterator();
        while (ginModuleItr.hasNext()) {
            GinModule ginModule = ginModuleItr.next();
            GinModuleAdapter module = new GinModuleAdapter(ginModule);
            logger.finer("Install " + ginModule.getClass().getName());
            install(module);
        }
    }
}
