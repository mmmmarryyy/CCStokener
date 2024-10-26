package KFramework30.Base;

import java.net.*;
import java.util.jar.*;
import java.awt.*;
import java.lang.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
	General purpose class that loads JAR files from URLS.
	It can return images from the jar.
*/
public final class KJarResourcesClass {

    private KConfigurationClass configuration;

    private KLogClass log;

    public boolean debugOn = false;

    private Hashtable htSizes = new Hashtable();

    private Hashtable htJarContents = new Hashtable();

    private String jarFileName;

    /** Constructor */
    public KJarResourcesClass(KConfigurationClass configurationParam, KLogClass logParam) {
        configuration = configurationParam;
        log = logParam;
        log.log(this, "Created successfully");
    }

    /** Get a blob from jar */
    public byte[] getBytes(String name) throws Exception {
        byte[] result = (byte[]) htJarContents.get(name);
        if (result == null) {
            String message = "[";
            message += name;
            message += "] not found in table.";
            throw new Exception(message);
        }
        ;
        return result;
    }

    /** Gets an image from a jar */
    public Image getImage(String imageName) throws KExceptionClass {
        {
            String message = "Reading image [";
            message += imageName;
            message += "] from JAR file [";
            message += jarFileName;
            message += "]...";
            log.log(this, message);
        }
        Image image;
        try {
            image = Toolkit.getDefaultToolkit().createImage(getBytes(imageName));
        } catch (Exception error) {
            try {
                image = Toolkit.getDefaultToolkit().createImage(getBytes("error.gif"));
            } catch (Exception againError) {
                String message = "Error creating image [";
                message += imageName;
                message += "]";
                message += " from resource file [";
                message += jarFileName;
                message += "]";
                throw new KExceptionClass(message, new KExceptionClass(againError.toString(), null));
            }
        }
        return (image);
    }

    public void loadJarFile(String jarFileNameParam) throws KExceptionClass {
        jarFileName = jarFileNameParam;
        {
            String message = "Loading resource file [";
            message += jarFileName;
            message += "]...";
            log.log(this, message);
        }
        try {
            URL url = new URL(jarFileName);
            JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
            jarConnection.setUseCaches(false);
            JarFile jarFile = jarConnection.getJarFile();
            Enumeration jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                ZipEntry zipEntrie = (ZipEntry) jarEntries.nextElement();
                {
                    String message = "Scanning [";
                    message += jarFileName;
                    message += "] found [";
                    message += describeEntry(zipEntrie);
                    message += "]";
                    log.log(this, message);
                }
                htSizes.put(zipEntrie.getName(), new Integer((int) zipEntrie.getSize()));
            }
            ;
            jarFile.close();
            BufferedInputStream inputBuffer = new BufferedInputStream(jarConnection.getJarFileURL().openStream());
            ZipInputStream input = new ZipInputStream(inputBuffer);
            ZipEntry zipEntrie = null;
            while ((zipEntrie = input.getNextEntry()) != null) {
                if (zipEntrie.isDirectory()) continue;
                {
                    String message = "Scanning [";
                    message += jarFileName;
                    message += "] loading [";
                    message += zipEntrie.getName();
                    message += "] for [";
                    message += zipEntrie.getSize();
                    message += "] bytes.";
                    log.log(this, message);
                }
                int size = (int) zipEntrie.getSize();
                if (size == -1) {
                    size = ((Integer) htSizes.get(zipEntrie.getName())).intValue();
                }
                ;
                byte[] entrieData = new byte[(int) size];
                int offset = 0;
                int dataRead = 0;
                while (((int) size - offset) > 0) {
                    dataRead = input.read(entrieData, offset, (int) size - offset);
                    if (dataRead == -1) break;
                    offset += dataRead;
                }
                htJarContents.put(zipEntrie.getName(), entrieData);
                if (debugOn) {
                    System.out.println(zipEntrie.getName() + "  offset=" + offset + ",size=" + size + ",csize=" + zipEntrie.getCompressedSize());
                }
                ;
            }
            ;
        } catch (Exception error) {
            String message = "Error loading data from JAR file [";
            message += error.toString();
            message += "]";
            throw new KExceptionClass(message, new KExceptionClass(error.toString(), null));
        }
        ;
    }

    private String describeEntry(ZipEntry entry) {
        StringBuffer message = new StringBuffer();
        if (entry.isDirectory()) {
            message.append("d ");
        } else {
            message.append("f ");
        }
        ;
        if (entry.getMethod() == ZipEntry.STORED) {
            message.append("stored   ");
        } else {
            message.append("defalted ");
        }
        ;
        message.append(entry.getName());
        message.append("\t");
        message.append("" + entry.getSize());
        if (entry.getMethod() == ZipEntry.DEFLATED) {
            message.append("/" + entry.getCompressedSize());
        }
        return (message.toString());
    }
}
