package net.sourceforge.juploader.filedownload;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Klasa służąca do pobierania plików z internetu i zapisywania ich do
 * plików lokalnych.
 *
 * @author Adam Pawelec
 */
public class FileDownload {

    public static final int BUFFER_SIZE = 1024;

    /**
     * Pobiera plik do wskazanej lokacji.
     *
     * @param address url pliku
     * @param fileName miejsce do którego ma zostać zapisany plik
     */
    public static void download(String address, String fileName, ProgressIndicator progress) throws Exception {
        URL url = new URL(address);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));
        URLConnection conn = url.openConnection();
        InputStream in = conn.getInputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int numRead;
        int total = conn.getContentLength();
        int completed = 0;
        if (progress != null) {
            progress.started(total);
        }
        while ((numRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, numRead);
            completed += numRead;
            if (progress != null) {
                progress.progress(completed);
            }
        }
        if (in != null) {
            in.close();
        }
        if (out != null) {
            out.close();
        }
        if (progress != null) {
            progress.finished();
        }
    }

    /**
     * Pobiera plik i zapisuje do katalogu tymczasowego.
     * Uwaga - należy pamiętać o usunięci tego pliku po jego wykorzystaniu.
     * @param address adres pliku do pobrania.
     * @return obiekt <code>File</code> do której zapisano plik.
     */
    public static File downloadToTemp(String address, ProgressIndicator progress) throws Exception {
        File tempFile = null;
        try {
            tempFile = File.createTempFile(readName(readFileName(address)), readExtension(readFileName(address)));
            tempFile.deleteOnExit();
            download(address, tempFile.getPath(), progress);
        } catch (Exception e) {
            if (tempFile != null) {
                tempFile.delete();
            }
            throw new Exception();
        }
        return tempFile;
    }

    public static File downloadToTemp(String address) throws Exception {
        return downloadToTemp(address, null);
    }

    /**
     * Odczytuje nazwę pliku z podanego adresu tj. ostatnią część po znaku /
     * @param address URL
     * @return String zawierający nazwę pliku
     */
    private static String readFileName(String address) {
        return address.substring(address.lastIndexOf('/') + 1, address.length());
    }

    /**
     * Odczytuje rozszerzenie z nazwy pliku.
     * @param fileName nazwa pliku
     * @return String zawierający rozszerzenie
     */
    private static String readExtension(String fileName) {
        return fileName.substring(fileName.indexOf('.'));
    }

    /**
     * Odczytuje nazwę pliku, tj. część przed rozszerzeniem.
     * @param fileName nazwa pliku
     * @return String zawierający nazwę bez rozszerzenia.
     */
    private static String readName(String fileName) {
        return fileName.substring(0, fileName.indexOf('.'));
    }
}
