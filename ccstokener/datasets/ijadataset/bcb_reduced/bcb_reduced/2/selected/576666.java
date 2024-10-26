package org.epoline.jsf.utils;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class JarExtractor {

    /**
	 * Extract entries from locally stored jar file
	 * @param jarFileName
	 * @return List of file- and directory names
	 * @throws java.io.IOException
	 */
    public static String[] getEntries(String jarFileName) throws IOException {
        ArrayList l = new ArrayList();
        ZipFile zf = new ZipFile(jarFileName);
        Enumeration e = zf.entries();
        while (e.hasMoreElements()) {
            ZipEntry ze = (ZipEntry) e.nextElement();
            l.add(ze.toString());
        }
        zf.close();
        return (String[]) l.toArray(new String[l.size()]);
    }

    /**
	 * Read a jar or zip file from local storage
	 * @param jarFileName
	 * @return Map
	 * @throws java.lang.Exception
	 */
    public static Map getResources(String jarFileName) throws Exception {
        Map htSizes = new Hashtable();
        Map htJarContents = new HashMap();
        ZipFile zf = new ZipFile(jarFileName);
        Enumeration e = zf.entries();
        while (e.hasMoreElements()) {
            ZipEntry ze = (ZipEntry) e.nextElement();
            Console.log(ze.toString());
            htSizes.put(ze.getName(), new Integer((int) ze.getSize()));
        }
        zf.close();
        FileInputStream fis = new FileInputStream(jarFileName);
        BufferedInputStream bis = new BufferedInputStream(fis);
        ZipInputStream zis = new ZipInputStream(bis);
        ZipEntry ze = null;
        while ((ze = zis.getNextEntry()) != null) {
            if (ze.isDirectory()) {
                continue;
            }
            int size = (int) ze.getSize();
            if (size == -1) {
                size = ((Integer) htSizes.get(ze.getName())).intValue();
            }
            byte[] b = new byte[size];
            int rb = 0;
            int chunk = 0;
            while ((size - rb) > 0) {
                chunk = zis.read(b, rb, size - rb);
                if (chunk == -1) {
                    break;
                }
                rb += chunk;
            }
            htJarContents.put(ze.getName(), b);
        }
        return htJarContents;
    }

    /**
	 * Read a jar or zip file using HTTP and extract the <code>resource</code>
	 * @param jarFileName filename of the jar
	 * @param resource The entry from the jar file (e.g. images/home.gif)
	 * @param port port number
     * @return Map with byte[] as value(s)
	 */
    public static Map getResources(String jarFileName, String resource, int port) throws Exception {
        return getResources(jarFileName, resource, port, "http");
    }

    /**
	 * Read a jar or zip file from a URL and extract the <code>resource</code>
	 * @param jarFileName filename of the jar
	 * @param resource The entry from the jar file (e.g. images/home.gif)
	 * @param port port number
	 * @param protocol Protocol to use (file | http)
	 * @return Map with byte[] as value(s)
	 */
    public static Map getResources(String jarFileName, String resource, int port, String protocol) throws Exception {
        Hashtable content = new Hashtable();
        if (!(protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("file"))) throw new IllegalArgumentException("Unsupported protocol; supported is: file or http");
        URL url = new URL(protocol, InetAddress.getLocalHost().getHostName(), port, jarFileName);
        BufferedInputStream bis = new BufferedInputStream(url.openStream());
        JarInputStream zipIs = new JarInputStream(bis);
        ZipEntry entry;
        int size = 0;
        Vector v = new Vector();
        try {
            while ((entry = zipIs.getNextEntry()) != null) {
                Console.log(entry.getName() + ", " + entry.getSize() + "..." + entry.toString());
                content.put(entry.getName(), new ZipEntry(entry));
                v.add(entry);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ZipEntry ze = null;
        for (int i = 0; i < v.size(); i++) {
            ZipEntry zipEntry = (ZipEntry) v.elementAt(i);
            if (zipEntry.getName().equals(resource)) {
                ze = zipEntry;
                break;
            }
        }
        size = (int) ze.getSize();
        Console.log("resource size=" + size);
        byte[] buf = new byte[size];
        int rb = 0;
        int chunk = 0;
        while ((size - rb) > 0) {
            chunk = zipIs.read(buf);
            Console.log("chunk = " + chunk + ", rb=" + rb);
            if (chunk == -1) {
                break;
            }
            rb += chunk;
        }
        try {
            zipIs.close();
            bis.close();
            url = null;
        } catch (IOException e) {
            Console.log("error closing jar " + e.getMessage());
            e.printStackTrace();
        }
        if (size != buf.length) throw new Exception("Resource '" + resource + "' has not been read correctly.");
        System.out.println("buf=" + buf);
        content.put(resource, buf);
        return content;
    }

    static void saveToDisk(byte[] b) {
        try {
            FileOutputStream fos = new FileOutputStream("d:\\service\\" + System.currentTimeMillis() + ".gif");
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(b);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
	 * Read a jar or zip file from a URL and extract the <code>resource</code>
	 * <p>
	 * NOTE
	 * The hostname will be extracted from the <code>java.rmi.server.codebase</code>
	 * property. If the codebase contains a port number, that port will override the
	 * PORT parameter in this method call. If not, the specified port will be used.
	 * </p>
	 * @param jarName Name of the jar file
	 * @param entry The entry you want to extract
	 * @param port The port
	 * @return The specified entry
	 */
    public static byte[] getJarEntry(String jarName, String entry, int port) {
        byte[] b = null;
        try {
            String codebase = System.getProperty("java.rmi.server.codebase", InetAddress.getLocalHost().getHostName());
            String protocol = "http://";
            int x = codebase.indexOf(protocol) + protocol.length();
            String s2 = codebase.substring(x);
            int x2 = s2.indexOf('/');
            String downloadHost = s2.substring(0, x2);
            if (downloadHost.indexOf(':') == -1) {
                downloadHost += ":" + port;
            }
            URL url = new URL("jar:http://" + downloadHost + "/" + jarName + "!/" + entry);
            JarURLConnection jurl = (JarURLConnection) url.openConnection();
            JarEntry je = jurl.getJarEntry();
            InputStream is = jurl.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            int size = (int) je.getSize();
            b = new byte[size];
            int rb = 0;
            int chunk = 0;
            while ((size - rb) > 0) {
                chunk = bis.read(b, rb, size - rb);
                if (chunk == -1) {
                    break;
                }
                rb += chunk;
            }
            bis.close();
            is.close();
            bis = null;
            is = null;
            url = null;
            jurl = null;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return b;
    }

    public static Image getImage(Class clazz, String resource) {
        URL u = clazz.getClassLoader().getResource(resource);
        return Toolkit.getDefaultToolkit().createImage(u);
    }
}
