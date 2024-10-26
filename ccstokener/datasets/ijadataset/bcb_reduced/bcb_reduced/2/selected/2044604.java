package ch.unibe.im2.inkanno.exporter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * Factory for export methods. The exporters can be specified by a 
 * string. The factory class will then search for this class. If it can not
 * be found a Factory Excpetion is thrown. Other wise an object
 * of this class is returned. 
 * The advantage of this factory class is that no compile-time dependency
 * to the actual exporter class is given. So new exporter can be added without
 * recompiling the application.
 * On the otherhand exporter depending on external libraries can be droped
 * for easy installation.
 * @author emanuel
 *
 */
public class ExporterFactory {

    @SuppressWarnings("unchecked")
    public Exporter createExporter(String name) throws FactoryException {
        Class c = null;
        try {
            c = Class.forName(name);
        } catch (ClassNotFoundException e1) {
            throw new FactoryException("Exporter '" + name + "' could not be found.");
        }
        if (c != null) {
            Exporter x = null;
            try {
                x = (Exporter) c.newInstance();
            } catch (InstantiationException e) {
                throw new FactoryException("Exporter '" + name + "' is not valid.");
            } catch (IllegalAccessException e) {
                throw new FactoryException("Exporter '" + name + "' is not valid.");
            }
            return x;
        } else {
            throw new FactoryException("Exporter '" + name + "' could not be found.");
        }
    }

    public List<Exporter> loadAvailableExporters() {
        List<Exporter> l = new ArrayList<Exporter>();
        try {
            Enumeration<URL> en = ClassLoader.getSystemClassLoader().getResources("ch/unibe/im2/inkanno/plugins/exporter_implementation.properties");
            while (en.hasMoreElements()) {
                Properties p = new Properties();
                URL url = en.nextElement();
                p.load(url.openStream());
                for (Object objstr : p.keySet()) {
                    String str = (String) objstr;
                    if (str.equals("exporter") || str.startsWith("exporter.")) {
                        if (p.getProperty(str) != null) {
                            l.add(createExporter(p.getProperty(str)));
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        return l;
    }
}
