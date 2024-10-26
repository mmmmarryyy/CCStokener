package net.taylor.seam;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.dom4j.Element;
import org.jboss.seam.Component;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.init.Initialization;
import org.jboss.seam.util.Resources;
import org.jboss.seam.util.XML;

/**
 * Utility for accessing component metadata.
 * 
 * @author jgilbert01
 */
public class ComponentUtil {

    public static List<Component> getComponentsByBeanClass(Class<?> beanClass) {
        List<Component> list = new ArrayList<Component>();
        Context context = Contexts.getApplicationContext();
        String[] names = context.getNames();
        for (String name : names) {
            if (name.endsWith(Initialization.COMPONENT_SUFFIX)) {
                Component component = (Component) context.get(name);
                if (component.getBeanClass().equals(beanClass)) {
                    list.add(component);
                }
            }
        }
        return list;
    }

    public static List<Component> getComponentsByAnnotaion(Class<?> annotationType) {
        List<Component> list = new ArrayList<Component>();
        Context context = Contexts.getApplicationContext();
        String[] names = context.getNames();
        for (String name : names) {
            if (name.endsWith(Initialization.COMPONENT_SUFFIX)) {
                Component component = (Component) context.get(name);
                if (component.beanClassHasAnnotation(annotationType)) {
                    list.add(component);
                }
            }
        }
        return list;
    }

    public static List<Component> getComponentsByBusinessInterface(Class<?> beanClass) {
        List<Component> list = new ArrayList<Component>();
        Context context = Contexts.getApplicationContext();
        String[] names = context.getNames();
        for (String name : names) {
            if (name.endsWith(Initialization.COMPONENT_SUFFIX)) {
                Component component = (Component) context.get(name);
                if (component.getBusinessInterfaces().contains(beanClass)) {
                    list.add(component);
                }
            }
        }
        return list;
    }

    public static List<URL> getComponentXmlFiles() throws Exception {
        List<URL> list = new ArrayList<URL>();
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("META-INF/components.xml");
        while (resources.hasMoreElements()) {
            list.add(resources.nextElement());
        }
        resources = Thread.currentThread().getContextClassLoader().getResources("WEB-INF/components.xml");
        while (resources.hasMoreElements()) {
            list.add(resources.nextElement());
        }
        return list;
    }

    public static URL getComponentXmlFileWith(String name) throws Exception {
        List<URL> all = getComponentXmlFiles();
        for (URL url : all) {
            InputStream stream = null;
            try {
                stream = url.openStream();
                Element root = XML.getRootElement(stream);
                for (Element elem : (List<Element>) root.elements()) {
                    String ns = elem.getNamespace().getURI();
                    if (name.equals(elem.attributeValue("name"))) {
                        return url;
                    }
                }
            } finally {
                Resources.closeStream(stream);
            }
        }
        return null;
    }
}
