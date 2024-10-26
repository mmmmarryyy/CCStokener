package various;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * TODO Description
 *
 * @author <a href="mailto:j.kahovec@imperial.ac.uk">Jakub Kahovec</a>
 *
 */
public class TestJAR {

    /**
     * @param args
     * @throws IOException 
     * @throws URISyntaxException 
     */
    public static void main(String[] args) throws IOException, URISyntaxException {
        getURLListFromResource("org/servingMathematics/mqat/examples", ".*[.mqat]", false);
        ClassLoader thisCL = TestJAR.class.getClassLoader();
        URLClassLoader thisURLCL = (URLClassLoader) thisCL;
        Enumeration e = thisURLCL.findResources("org/servingMathematics/mqat/examples");
        JarFile jarFile;
        File file;
        File[] files;
        for (; e.hasMoreElements(); ) {
            URL url = (URL) e.nextElement();
            System.out.println(url);
            URI uri = new URI(new URL("jar:file:/C:/Projects/imperial/workspace/mqat/trunk/lib/mqat-examples.jar!/org/servingMathematics/mqat/examples/Digging_hole.xml").toString());
            url = uri.toURL();
            System.out.println(url);
            JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
            jarFile = jarConnection.getJarFile();
            Enumeration ee = jarFile.entries();
            for (; ee.hasMoreElements(); ) {
                JarEntry jarEntry = (JarEntry) ee.nextElement();
                if (!jarEntry.isDirectory()) {
                    System.out.println(jarEntry);
                    String fileURI = url.toString().substring(0, url.toString().lastIndexOf('!') + 1);
                    fileURI += "/" + jarEntry;
                    System.out.println(fileURI);
                }
            }
        }
        URL[] urls = thisURLCL.getURLs();
        for (int i = 0; i < urls.length; i++) {
            System.out.println(urls[i]);
        }
    }

    public static String[] getURLListFromResource(String resourceName, String regExFilter, boolean onlyFromFirstMatched) throws IOException {
        String[] urlArray;
        Vector urlVector = new Vector();
        Enumeration e;
        ClassLoader classLoader = TestJAR.class.getClassLoader();
        URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
        e = urlClassLoader.findResources(resourceName);
        for (; e.hasMoreElements(); ) {
            URL url = (URL) e.nextElement();
            if ("file".equals(url.getProtocol())) {
                File file = new File(url.getPath());
                File[] fileList = file.listFiles();
                if (fileList != null) {
                    for (int i = 0; i < fileList.length; i++) {
                        String urlStr = fileList[i].toURL().toString();
                        if (urlStr.matches(".*[.mqat]")) {
                            urlVector.add(urlStr);
                        }
                    }
                }
            } else if ("jar".equals(url.getProtocol())) {
                JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
                JarFile jarFile = jarConnection.getJarFile();
                e = jarFile.entries();
                for (; e.hasMoreElements(); ) {
                    JarEntry jarEntry = (JarEntry) e.nextElement();
                    if (!jarEntry.isDirectory()) {
                        String urlStr = url.toString().substring(0, url.toString().lastIndexOf('!') + 1);
                        urlStr += "/" + jarEntry;
                        if (urlStr.matches(".*[.mqat]")) {
                            urlVector.add(urlStr);
                        }
                    }
                }
            }
            if (onlyFromFirstMatched) {
                break;
            }
        }
        urlArray = (String[]) urlVector.toArray(new String[urlVector.size()]);
        return urlArray;
    }
}
