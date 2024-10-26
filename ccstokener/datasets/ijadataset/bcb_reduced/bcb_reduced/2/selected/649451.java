package org.ji18n.core.container;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.ji18n.core.log.LogLevel;
import org.ji18n.core.log.LogService;
import org.ji18n.core.log.LogServiceFactory;
import org.ji18n.core.util.Classes;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @version $Id: MultiContainerService.java 159 2008-07-03 01:28:51Z david_ward2 $
 * @author david at ji18n.org
 */
public class MultiContainerService implements ContainerService {

    private static final String NAME = "multi";

    private static final Map<String, Class<?>> CLASS_MAP = new LinkedHashMap<String, Class<?>>();

    static {
        Class<?>[] clazzes = Classes.getServiceClasses(ContainerService.class);
        for (Class<?> clazz : clazzes) {
            if (!clazz.getName().equals(MultiContainerService.class.getName())) {
                try {
                    ContainerService throwaway = (ContainerService) clazz.newInstance();
                    CLASS_MAP.put(throwaway.getConfigName(), clazz);
                } catch (Throwable ignore) {
                }
            }
        }
    }

    private static LogService logService = LogServiceFactory.createLogService(MultiContainerService.class);

    private Map<String, ContainerService> cfgnm_cntr_map = new LinkedHashMap<String, ContainerService>();

    private Map<String, ContainerService> objid_cntr_map = new LinkedHashMap<String, ContainerService>();

    public MultiContainerService() {
    }

    public String getName() {
        return NAME;
    }

    public String[] getContainerNames() {
        Set<String> containerNames = new LinkedHashSet<String>();
        for (ContainerService c : cfgnm_cntr_map.values()) containerNames.add(c.getName());
        return containerNames.toArray(new String[containerNames.size()]);
    }

    public String getConfigName() {
        return null;
    }

    public void start() {
        stop();
        for (String configName : CLASS_MAP.keySet()) {
            Class<?> clazz = CLASS_MAP.get(configName);
            try {
                ContainerService c = (ContainerService) clazz.newInstance();
                assert c.getConfigName().equals(configName);
                c.start();
                cfgnm_cntr_map.put(configName, c);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void deploy(URL... urls) {
        Map<String, Set<URL>> mini_map = new LinkedHashMap<String, Set<URL>>();
        for (URL url : urls) {
            ContainerService container = getContainer(url);
            String configName = container.getConfigName();
            Set<URL> set = mini_map.get(configName);
            if (set == null) {
                set = new LinkedHashSet<URL>();
                mini_map.put(configName, set);
            }
            set.add(url);
        }
        for (String configName : mini_map.keySet()) {
            ContainerService container = cfgnm_cntr_map.get(configName);
            Set<URL> set = mini_map.get(configName);
            if (logService.isEnabledFor(LogLevel.DEBUG)) {
                String s = NAME + " -> " + container.getName() + "-" + container.getConfigName() + " (" + set.size() + ")";
                logService.log(LogLevel.DEBUG, s);
            }
            container.deploy(set.toArray(new URL[set.size()]));
            String[] ids = container.getObjectIds();
            for (String id : ids) {
                if (!objid_cntr_map.containsKey(id)) objid_cntr_map.put(id, container);
            }
        }
    }

    public String[] getObjectIds() {
        Set<String> ids = new LinkedHashSet<String>();
        for (ContainerService c : cfgnm_cntr_map.values()) ids.addAll(Arrays.asList(c.getObjectIds()));
        return ids.toArray(new String[ids.size()]);
    }

    public Object getObject(String id) {
        ContainerService container = objid_cntr_map.get(id);
        return (container != null ? container.getObject(id) : null);
    }

    public void stop() {
        for (ContainerService c : cfgnm_cntr_map.values()) c.stop();
        cfgnm_cntr_map.clear();
        objid_cntr_map.clear();
    }

    private ContainerService getContainer(URL url) {
        ConfigHandler ch = new ConfigHandler();
        InputStream is = null;
        try {
            is = url.openStream();
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(ch);
            xr.parse(new InputSource(is));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (is != null) is.close();
            } catch (Throwable t) {
            }
        }
        return cfgnm_cntr_map.get(ch.configName);
    }

    private class ConfigHandler extends DefaultHandler {

        public String configName = null;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) {
            if (configName == null) configName = localName;
        }
    }
}
