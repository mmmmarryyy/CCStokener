package com.gorillalogic.util;

import java.util.*;
import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.*;
import org.w3c.dom.*;
import org.apache.log4j.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import com.gorillalogic.config.ConfigurationException;
import com.gorillalogic.core.GorillaException;
import com.gorillalogic.core.GorillaRuntimeException;

public class IOUtil {

    private static Logger logger = Logger.getLogger(IOUtil.class);

    public static Document readDocument(InputStream in) {
        Document doc = null;
        try {
            DocumentBuilder builder = null;
            if (true) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                builder = factory.newDocumentBuilder();
            } else {
                builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
            }
            doc = builder.parse(in);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return doc;
    }

    public static InputStream getFileAsInputStream(String file) {
        InputStream in = null;
        String msg = null;
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            msg = "-- requested input file " + file + " cannot be opened for input.";
        }
        return in;
    }

    public static byte[] readBytes(InputStream is) throws IOException {
        ByteArrayOutputStream into = new ByteArrayOutputStream();
        int asInt = is.read();
        while (asInt != -1) {
            into.write(asInt);
            asInt = is.read();
        }
        return into.toByteArray();
    }

    public static InputStream extractFromZipFile(File f, String endsWith) throws IOException {
        ZipFile zip = null;
        InputStream rc = null;
        try {
            zip = new ZipFile(f);
            rc = extractFromZipFile(zip, endsWith);
        } catch (Exception e) {
            if (rc != null) {
                rc.close();
            }
            throw new IOException("Error extracting XMI from zipped file " + f.getPath() + ": " + e.getMessage());
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return rc;
    }

    public static InputStream extractFromZipFile(ZipFile zip, String endsWith) throws IOException {
        InputStream in = null;
        InputStream rc = null;
        try {
            int nextIndex;
            ZipEntry next;
            String nextName;
            Enumeration enums = zip.entries();
            while (enums.hasMoreElements()) {
                nextName = (next = (ZipEntry) enums.nextElement()).getName();
                if (nextName.endsWith(endsWith)) {
                    in = zip.getInputStream(next);
                    byte[] xmi = IOUtil.inputStreamToByteArray(in);
                    rc = new ByteArrayInputStream(xmi);
                    break;
                }
            }
        } catch (Exception e) {
            throw new IOException("Error extracting XMI from zip stream inside model file " + zip.getName() + ": " + e.getMessage());
        } finally {
            try {
                zip.close();
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return rc;
    }

    /** replaces a zipfile entry with the supplied file, creating a new zip file
	 *  with the replaced entry included
	 *  @param zipFilePath - the source zip file 
	 *  @param replacementEntryName - the name of the entry to replace 
	 *  @param replacementEntryFilePath - the file read the replacement entry from 
	 *  @param outputFilePath - where to write the new  
	 */
    public static void replaceEntryInZipFile(String zipFilePath, String replacementEntryName, String replacementEntryFilePath, String outputFilePath) throws IOException {
        replaceEntryInZipFile(zipFilePath, replacementEntryName, new FileInputStream(replacementEntryFilePath), outputFilePath);
    }

    /** replaces a zipfile entry with the supplied stream, creating a new zip file
	 *  with the replaced entry included
	 *  @param zipFilePath - the source zip file 
	 *  @param replacementEntryName - the name of the entry to replace 
	 *  @param replacementEntryStream - an InputStream to read the replacement entry from 
	 *  @param outputFilePath - where to write the new  
	 */
    public static void replaceEntryInZipFile(String zipFilePath, String replacementEntryName, InputStream replacementEntryStream, String outputFilePath) throws IOException {
        ZipOutputStream tempZipStream = null;
        File tempZipFile = null;
        File zipFile = null;
        ZipFile zip = null;
        FileInputStream file = null;
        InputStream entryStream = null;
        try {
            zipFile = new File(zipFilePath);
            zip = new ZipFile(zipFile);
            tempZipFile = new File(outputFilePath);
            tempZipStream = new ZipOutputStream(new FileOutputStream(tempZipFile));
            byte[] buffer = new byte[1024];
            int bytesRead;
            Enumeration entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String entryName = entry.getName();
                if (entryName.equals(replacementEntryName)) {
                    entry = new ZipEntry(entryName);
                    entryStream = replacementEntryStream;
                } else {
                    entryStream = zip.getInputStream(entry);
                    entry.setCompressedSize(-1);
                }
                tempZipStream.putNextEntry(entry);
                while ((bytesRead = entryStream.read(buffer)) != -1) {
                    tempZipStream.write(buffer, 0, bytesRead);
                }
                entryStream.close();
            }
        } catch (IOException e) {
            throw e;
        } finally {
            IOException ex = null;
            if (tempZipStream != null) {
                try {
                    tempZipStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    ex = e;
                }
            }
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    ex = e;
                }
            }
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    ex = e;
                }
            }
            if (entryStream != null) {
                try {
                    entryStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    ex = e;
                }
            }
            if (ex != null) {
                throw new IOException("error(s) caught closing files: " + ex.getMessage());
            }
        }
    }

    public static void copy(File inputFile, File outputFile) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            outputFile.createNewFile();
            in = new FileInputStream(inputFile);
            out = new FileOutputStream(outputFile);
            copy(in, out);
        } finally {
            try {
                in.close();
                out.close();
            } catch (Exception e) {
            }
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        while (true) {
            int c = in.read();
            if (c == -1) {
                out.close();
                return;
            }
            out.write(c);
        }
    }

    public static String fileToString(String fileName) throws IOException {
        byte[] b = inputStreamToByteArray(new FileInputStream(fileName));
        return new String(b);
    }

    public static byte[] fileToByteArray(String fileName) throws IOException {
        return inputStreamToByteArray(new FileInputStream(fileName));
    }

    public static byte[] fileToByteArray(File in) throws IOException {
        return inputStreamToByteArray(new FileInputStream(in));
    }

    public static String inputStreamToString(InputStream in) throws IOException {
        byte[] b = inputStreamToByteArray(in);
        return new String(b);
    }

    public static String readerToString(Reader in) throws IOException {
        int c;
        StringBuffer buf = new StringBuffer();
        while ((c = in.read()) != -1) {
            buf.append((char) c);
        }
        return buf.toString();
    }

    public static byte[] inputStreamToByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(in.available());
        try {
            int c;
            while ((c = in.read()) != -1) {
                baos.write(c);
            }
        } finally {
            in.close();
        }
        return baos.toByteArray();
    }

    class IOUtilException extends IOException {

        IOUtilException() {
            super();
        }

        IOUtilException(String s) {
            super(s);
        }
    }

    /** @return the extension part of the passed fileName or an empty string if none is found
     */
    public static String getExtension(String fileName) {
        String name = new File(fileName).getName();
        int dot = name.lastIndexOf('.');
        if (dot == -1) {
            return "";
        }
        return name.substring(dot + 1);
    }

    public static String getNameWithoutExtension(String fileName) {
        String ext = getExtension(fileName);
        return fileName.substring(0, fileName.lastIndexOf("." + ext));
    }

    /**
     * JarResources: JarResources maps all resources included in a
     * Zip or Jar file. Additionaly, it provides a method to extract one
     * as a blob.
     */
    public static final class JarResources {

        private Hashtable htSizes = new Hashtable();

        private Hashtable htJarContents = new Hashtable();

        private String jarFileName;

        /**
        * creates a JarResources. It extracts all resources from a Jar
        * into an internal hashtable, keyed by resource names.
        * @param jarFileName a jar or zip file
        */
        public JarResources(String jarFileName) {
            this.jarFileName = jarFileName;
            init();
        }

        /**
        * Extracts a jar resource as a blob.
        * @param name a resource name.
        */
        public byte[] getResource(String name) {
            return (byte[]) htJarContents.get(name);
        }

        /**
        * initializes internal hash tables with Jar file resources.
        */
        private void init() {
            try {
                ZipFile zf = new ZipFile(jarFileName);
                Enumeration e = zf.entries();
                while (e.hasMoreElements()) {
                    ZipEntry ze = (ZipEntry) e.nextElement();
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
                    byte[] b = new byte[(int) size];
                    int rb = 0;
                    int chunk = 0;
                    while (((int) size - rb) > 0) {
                        chunk = zis.read(b, rb, (int) size - rb);
                        if (chunk == -1) {
                            break;
                        }
                        rb += chunk;
                    }
                    htJarContents.put(ze.getName(), b);
                }
            } catch (NullPointerException e) {
                return;
            } catch (FileNotFoundException e) {
                return;
            } catch (IOException e) {
                return;
            }
        }
    }

    public static File getClasspathDir(Class clazz) throws GorillaException {
        String className = clazz.getName();
        String resourceName = "/" + className.replaceAll("\\.", "/") + ".class";
        URL url = clazz.getResource(resourceName);
        if (url == null) {
            throw new GorillaException("could not bootstrap root of classpath" + " for class" + className);
        }
        String urlString = url.toString();
        urlString = urlString.substring(0, urlString.lastIndexOf(resourceName) + 1);
        try {
            url = new URL(urlString);
        } catch (java.net.MalformedURLException e) {
            throw new GorillaException(e.getMessage());
        }
        URLConnection conn;
        try {
            conn = url.openConnection();
        } catch (IOException e) {
            throw new GorillaException("IOException attempting to open classpath" + " \"" + url.toString() + "\":" + e.getMessage());
        }
        String urlFileName = null;
        if (conn instanceof JarURLConnection) {
            url = ((JarURLConnection) conn).getJarFileURL();
            urlFileName = url.getFile();
            urlFileName = urlFileName.substring(0, urlFileName.lastIndexOf("/"));
        } else {
            urlFileName = url.getFile();
        }
        String bootFullPath = "could not parse url for file: " + urlFileName;
        try {
            bootFullPath = URLDecoder.decode(urlFileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new GorillaException(bootFullPath + ": " + e.getMessage());
        }
        File file = new File(bootFullPath);
        return file;
    }

    public static void expandJar(JarURLConnection conn, File dest) throws GorillaException {
        String dir;
        try {
            dir = conn.getJarEntry().getName();
        } catch (IOException e) {
            throw new GorillaException("Error reading " + conn.getURL().toString());
        }
        JarFile jar = null;
        try {
            jar = conn.getJarFile();
        } catch (Exception e) {
            throw new GorillaException("Error reading jar file " + conn.getJarFileURL().toString(), e);
        }
        Enumeration<JarEntry> enumeration = jar.entries();
        while (enumeration.hasMoreElements()) {
            JarEntry entry = enumeration.nextElement();
            String name = entry.getName();
            if (!name.startsWith(dir)) {
                continue;
            }
            File destFile = new File(dest, name.substring((dir).length()));
            if (entry.isDirectory()) {
                destFile.mkdirs();
                continue;
            }
            BufferedInputStream in = null;
            BufferedOutputStream out = null;
            try {
                logger.debug("Copying " + name + " to " + destFile.getPath());
                destFile.createNewFile();
                in = new BufferedInputStream(jar.getInputStream(entry));
                out = new BufferedOutputStream(new FileOutputStream(destFile));
                copy(in, out);
            } catch (Exception e) {
                throw new GorillaException("Exception copying " + name + " to " + destFile.getPath());
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    throw new GorillaException("Error closing config files", e);
                }
            }
        }
    }

    public static void copyDir(File src, File dest) throws ConfigurationException {
        try {
            if (src.isFile()) {
                BufferedInputStream srcFile = new BufferedInputStream(new FileInputStream(src));
                dest.createNewFile();
                BufferedOutputStream destFile = new BufferedOutputStream(new FileOutputStream(dest));
                while (true) {
                    int c = srcFile.read();
                    if (c == -1) {
                        destFile.close();
                        return;
                    }
                    destFile.write(c);
                }
            }
            File[] files = src.listFiles();
            dest.mkdirs();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    copyDir(files[i], new File(dest, files[i].getName()));
                    return;
                }
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(files[i]));
                File destFile = new File(dest, files[i].getName());
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(destFile));
                logger.debug("Copying " + files[i].getPath() + " to " + destFile.getPath());
                copy(in, out);
            }
        } catch (Exception e) {
            throw new ConfigurationException("Error copying " + src.getPath() + " to " + dest.getPath(), e);
        }
    }
}
