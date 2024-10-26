package geisler.projekt.game.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;

/**
 * Diese Klasse enthaelt Hilfsdienste fuer den Zugriff auf Resourcen (to be continued).
 * 
 * @author Mario Beresheim
 * @version 1.00
 * @release 12.04.2011
 */
public class ResourceHelper {

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");

    /** Der ClassLoader fuer den Zugriff auf Ressourcen */
    public static ClassLoader RESOURCE_LOADER;

    /**
	 * Liefert den ClassLoader zum Laden von Ressourcen. Ein ClassLoader
	 * kann in die statische Variable {@link #RESOURCE_LOADER} gesetzt werden.
	 * Ist keine ClassLoader gesetzt, so wird der ClassLoader fuer diese
	 * Klasse verwendet.
	 * 
	 * @return Der ClassLoader.
	 */
    public static ClassLoader getResourceLoader() {
        if (RESOURCE_LOADER == null) {
            RESOURCE_LOADER = ResourceHelper.class.getClassLoader();
        }
        return RESOURCE_LOADER;
    }

    /**
	 * Kopiert die angegebene Ressource in ein Verzeichnis.
	 * 
	 * @param resource Die zu kopierende Ressource.
	 * @param directory Das Verzeichnis.
	 * @param alwaysOverwrite {@code true} Ueberschreibt eine existierende Ressource in jedem Fall,
	 * auch wenn diese aktueller ist. 
	 * <br>{@code false} Eine existierende Ressource wird nur dann ueberschrieben, wenn sie aelter ist.
	 * @return {@code true} Die Ressource wurde in das angegebene Verzeichnis kopiert.
	 * <br>{@code false} Die Ressource wurde nicht kopiert.
	 * @throws IllegalArgumentException fehlende oder bloedsinnige Parameter
	 * @throws IOException
	 */
    public static boolean copyResourceInDirectory(String resource, File directory, boolean alwaysOverwrite) throws IOException {
        if (resource == null || "".equals(resource)) {
            throw new IllegalArgumentException("Parameter \"resource\" wurde nicht gesetzt.");
        }
        if (directory == null) {
            throw new IllegalArgumentException("Parameter \"directory\" wurde nicht gesetzt.");
        }
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IllegalArgumentException("Das Verzeichnis '" + directory + "' konnte nicht erstellt werden.");
            }
        }
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("'" + directory + "' ist kein Verzeichnis.");
        }
        URL resourceUrl = getResourceLoader().getResource(resource);
        if (resourceUrl == null) {
            resourceUrl = ResourceHelper.class.getResource(resource);
        }
        if (resourceUrl == null) {
            throw new IOException("Die Ressource '" + resource + "' wurde nicht gefunden.");
        }
        long lastModified = getLastModified(resourceUrl);
        File resourceFile = new File(resourceUrl.toString());
        File outputFile = new File(directory.getAbsolutePath() + FILE_SEPARATOR + resourceFile.getName());
        if (outputFile.exists() && outputFile.lastModified() >= lastModified && !alwaysOverwrite) {
            return false;
        }
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        BufferedOutputStream bufferedStream = null;
        int byteCount = 4 * 1024;
        byte data[] = new byte[byteCount];
        try {
            inputStream = resourceUrl.openConnection().getInputStream();
            outputStream = new FileOutputStream(outputFile);
            bufferedStream = new BufferedOutputStream(outputStream);
            while ((byteCount = inputStream.read(data, 0, byteCount)) > 0) {
                bufferedStream.write(data, 0, byteCount);
            }
            bufferedStream.flush();
        } finally {
            try {
                bufferedStream.close();
            } catch (Exception exc) {
            }
            try {
                outputStream.close();
            } catch (Exception exc) {
            }
            try {
                inputStream.close();
            } catch (Exception exc) {
            }
        }
        outputFile.setLastModified(lastModified);
        return true;
    }

    /**
	 * Liefert das Aenderungsdatum fuer die angegebene Url. Die Url muss auf eine
	 * Datei zeigen. Die Datei darf dabei auch in einem Jar oder Zip liegen.
	 *
	 * @param url Die Url.
	 * @return Das Aenderungsdatum.
	 * @throws IllegalArgumentException Die Url zeigt nicht auf eine Datei.
	 * @throws IOException
	 */
    public static long getLastModified(URL url) throws IOException {
        if ("file".equals(url.getProtocol())) {
            File file = new File(url.getFile());
            return file.lastModified();
        } else if ("jar".equals(url.getProtocol())) {
            String filenName = url.getFile();
            if (filenName != null && filenName.contains("!/")) {
                String[] tokens = filenName.split("!/");
                String entryName = tokens[tokens.length - 1];
                JarURLConnection jarUrl = (JarURLConnection) url.openConnection();
                ZipEntry entry = jarUrl.getJarFile().getEntry(entryName);
                if (entry != null) {
                    return entry.getTime();
                }
            }
        } else {
            throw new IllegalArgumentException("Die Url '" + url.toString() + "' zeigt nicht auf eine Datei");
        }
        return 0L;
    }
}
