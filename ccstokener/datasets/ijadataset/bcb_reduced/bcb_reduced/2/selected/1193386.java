package uk.ac.shef.wit.simmetrics.metrichandlers;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.BlockDistance;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

/**
 * uk.ac.shef.wit.simmetrics.metrichandlers.MetricHandler defines a class able
 * to detail information about the metrics available.
 * 
 * @author Sam Chapman <a href="mailto:s.chapman@dcs.shef.ac.uk>Email</a>
 * @version 1.0 Date: 21-Jul-2005 Time: 15:47:20
 *          <p/>
 *          Copyright Sam Chapman 21-Jul-2005 <a
 *          href="http://www.dcs.shef.ac.uk/~sam/">website</a>
 */
public class MetricHandler {

    /**
	 * private string metric used to get the details of the resource.
	 */
    private static AbstractStringMetric aMetric = new BlockDistance();

    /**
	 * gets the metrics available in the jar or filepath.
	 * 
	 * @return an ArrayList of Strings containing metric names
	 */
    public static ArrayList<String> GetMetricsAvailable() {
        ArrayList<String> outputVect = new ArrayList<String>();
        Class tosubclass = null;
        try {
            tosubclass = Class.forName("uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        String pckgname = "uk.ac.shef.wit.simmetrics.similaritymetrics";
        String name = pckgname;
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        name = name.replace('.', '/');
        URL url = aMetric.getClass().getResource(name);
        if (url == null) return null;
        File directory = new File(url.getFile());
        if (directory.exists()) {
            String[] files = directory.list();
            for (String file : files) {
                if (file.endsWith(".class")) {
                    String classname = file.substring(0, file.length() - 6);
                    try {
                        Object o = Class.forName(pckgname + "." + classname).newInstance();
                        assert tosubclass != null;
                        if (tosubclass.isInstance(o)) {
                            outputVect.add(classname);
                        }
                    } catch (ClassNotFoundException cnfex) {
                        System.err.println(cnfex);
                    } catch (InstantiationException iex) {
                    } catch (IllegalAccessException iaex) {
                    }
                }
            }
        } else {
            try {
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                String starts = conn.getEntryName();
                JarFile jfile = conn.getJarFile();
                Enumeration<JarEntry> e = jfile.entries();
                while (e.hasMoreElements()) {
                    ZipEntry entry = e.nextElement();
                    String entryname = entry.getName();
                    if (entryname.startsWith(starts) && (entryname.lastIndexOf('/') <= starts.length()) && entryname.endsWith(".class")) {
                        String classname = entryname.substring(0, entryname.length() - 6);
                        if (classname.startsWith("/")) classname = classname.substring(1);
                        classname = classname.replace('/', '.');
                        try {
                            Object o = Class.forName(classname).newInstance();
                            assert tosubclass != null;
                            if (tosubclass.isInstance(o)) {
                                outputVect.add(classname.substring(classname.lastIndexOf('.') + 1));
                            }
                        } catch (ClassNotFoundException cnfex) {
                            System.err.println(cnfex);
                        } catch (InstantiationException iex) {
                        } catch (IllegalAccessException iaex) {
                        }
                    }
                }
            } catch (IOException ioex) {
                System.err.println(ioex);
            }
        }
        return outputVect;
    }

    /**
	 * creates a metric with a given name using reflection.
	 * 
	 * @param metricName
	 *            the <code>String</code> name of the metric to create
	 * @return if a valid name the metric otherwise null
	 */
    public static AbstractStringMetric createMetric(String metricName) {
        AbstractStringMetric aplugin = null;
        try {
            Class<?> metricDefinition = Class.forName("uk.ac.shef.wit.simmetrics.similaritymetrics." + metricName);
            Constructor<?> constructor = metricDefinition.getConstructor();
            aplugin = (AbstractStringMetric) constructor.newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return aplugin;
    }
}
