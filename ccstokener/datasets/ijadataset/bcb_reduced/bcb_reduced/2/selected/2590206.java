package medieveniti.util.update;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Der ZipUpdater ist ein einfacher Updater,
 * der es ermöglicht, einen Ordner (den Ziel-Ordner)
 * mit Hilfe einer Zip-Datei zu updaten. Dabei werden alle Dateien in
 * der Zip-Datei dazu benutzt, die entsprechenden Dateien im Ziel-Ordner
 * zu überschreiben. Dateien, die sich im Ziel-Ordner befinden, nicht aber
 * in der Zip-Datei, werden nicht gel�scht oder anderweitig verändert.
 * @author Hans Kirchner
 */
public class ZipUpdater {

    /**
	 * Versteckt den öffentlichen Standard-Konstruktor.
	 */
    private ZipUpdater() {
    }

    /**
	 * Startet den Updater mit einem ZipInpuStream.
	 * @param targetFolder Ziel-Ordner
	 * @param in ZipInputStream der Zip-Datei
	 */
    public static void run(File targetFolder, ZipInputStream in) throws UpdateException {
        try {
            if (targetFolder == null) throw new NullPointerException("TargetFolder must not be null.");
            if (in == null) throw new NullPointerException("ZipInputStream must not be null.");
            if (!targetFolder.exists()) targetFolder.mkdirs();
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                System.out.println(entry.getName());
                File file = new File(targetFolder.getAbsolutePath() + File.separator + correctSeparators(entry.getName()));
                System.out.println(file.getPath());
                if (!entry.isDirectory()) {
                    file.getParentFile().mkdirs();
                    FileOutputStream out = new FileOutputStream(file);
                    copy(in, out);
                    out.close();
                } else {
                    file.mkdirs();
                }
                in.closeEntry();
            }
            in.close();
        } catch (Exception e) {
            throw new UpdateException(e);
        }
    }

    /**
	 * Startet den Updater mit einem InputStream.
	 * @param targetFolder Ziel-Ordner
	 * @param in InputStream der Zip-Datei
	 */
    public static void run(File targetFolder, InputStream in) throws UpdateException {
        run(targetFolder, new ZipInputStream(in));
    }

    /**
	 * Startet den Updater mit einer URL.
	 * @param targetFolder Ziel-Ordner
	 * @param url URL der Zip-Datei
	 */
    public static void run(File targetFolder, URL url) throws UpdateException {
        try {
            run(targetFolder, new ZipInputStream(url.openStream()));
        } catch (Exception e) {
            if (e instanceof UpdateException) throw (UpdateException) e; else throw new UpdateException(e);
        }
    }

    /**
	 * Startet den Updater mit einem File.
	 * @param targetFolder Ziel-Ordner
	 * @param file lokale Zip-Datei
	 */
    public static void run(File targetFolder, File file) throws UpdateException {
        try {
            run(targetFolder, new ZipInputStream(new FileInputStream(file)));
        } catch (Exception e) {
            if (e instanceof UpdateException) throw (UpdateException) e; else throw new UpdateException(e);
        }
    }

    /**
	 * Kleine Hilfsmethode, die die Slashes des Pfades in die des
	 * nativen Dateisystems umwandelt.
	 * @param path Pfad
	 * @return nativer Pfad
	 */
    private static String correctSeparators(String path) {
        return path.replace("/", File.separator);
    }

    /**
	 * Größse des Byte-Array-Buffers für die copy-Methode.
	 * Es wird empfohlen, ein Vielfaches von 1024 zu verwenden.
	 */
    private static final int BUF_SIZE = 4096;

    /**
	 * Schreibt alle gelesenen Bytes des InputStreams in den
	 * des OuputStreams.
	 * @param in Eingabe
	 * @param out Ausgabe
	 * @throws IOException bei Ein-/Ausgabe-Fehlern
	 */
    private static void copy(InputStream in, OutputStream out) throws IOException {
        BufferedInputStream bufIn = new BufferedInputStream(in);
        BufferedOutputStream bufOut = new BufferedOutputStream(out);
        byte[] buffer = new byte[BUF_SIZE];
        int len;
        while ((len = bufIn.read(buffer)) != -1) {
            bufOut.write(buffer, 0, len);
        }
        bufOut.flush();
    }
}
