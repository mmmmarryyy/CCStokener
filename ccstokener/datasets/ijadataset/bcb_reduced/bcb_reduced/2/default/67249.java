import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

/**
 * Self extracting jar class. 
 */
public class SelfExtractor {

    /**
         * Extract jar file
         */
    public void extract() throws IOException {
        ZipFile zipFile = getZipFile();
        System.out.println("Extracting " + zipFile.getName() + "....");
        Enumeration entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            extractEntry(zipFile, entry);
        }
    }

    /**
         * Extract entry
         */
    private void extractEntry(ZipFile zf, ZipEntry ze) throws IOException {
        File fl = new File(ze.toString());
        if (ze.isDirectory()) {
            fl.mkdirs();
            return;
        }
        String parentName = fl.getParent();
        if (parentName != null) {
            File par = new File(parentName);
            if (!par.exists()) {
                par.mkdirs();
            }
        }
        System.out.println(ze);
        FileOutputStream fos = new FileOutputStream(fl);
        InputStream is = zf.getInputStream(ze);
        byte buff[] = new byte[1024];
        int cnt = -1;
        while ((cnt = is.read(buff)) != -1) {
            fos.write(buff, 0, cnt);
        }
        is.close();
        fos.close();
    }

    /**
         * Get the jar file
         */
    private ZipFile getZipFile() throws IOException {
        String className = getClass().getName() + ".class";
        String jarURLName = getClass().getClassLoader().getSystemResource(className).toString();
        int toIndex = jarURLName.lastIndexOf("!/");
        URL url = new URL(jarURLName.substring(0, toIndex) + "!/");
        JarURLConnection urlCon = (JarURLConnection) url.openConnection();
        return urlCon.getJarFile();
    }

    /**
         * Program starting point
         */
    public static void main(String args[]) {
        try {
            new SelfExtractor().extract();
        } catch (Exception ex) {
            System.err.println("**** Extraction failed ****");
            ex.printStackTrace();
        }
    }
}
