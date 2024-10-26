package juploader.httpclient;

import juploader.httpclient.exceptions.RequestCancelledException;
import org.apache.http.client.methods.HttpUriRequest;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URLConnection;

/**
 * Żądanie HTTP pobierające plik metodą GET do wybranej ścieżki.
 *
 * @author Adam Pawelec
 */
public class FileDownloadRequest extends AbstractHttpRequest {

    private static final int BUFFRE_SIZE = 4096;

    private File destFile;

    public FileDownloadRequest() {
        super(null, null);
    }

    /**
     * Wykonuje żądanie pobrania pliku.
     *
     * @return ponieważ celem klienta jest tylko pobranie pliku zawsze zwraca
     *         null a plik jest zapisany do ścieżki określonej właściwością
     *         <b>destFile</b>
     * @throws RequestCancelledException w przypadku anulowania pobierania
     *                                   pliku
     * @throws IllegalStateException     w przypadku gdy wywołano metodę na
     *                                   nieprzygotowanym obiekcie np. nie
     *                                   podano URL do pliku lub nie określono
     *                                   ścieżki docelowej
     * @throws IOException               w przypadku nieoczekiwanego błędu IO
     */
    @Override
    public HttpResponse makeRequest() throws RequestCancelledException, IllegalStateException, IOException {
        checkState();
        OutputStream out = null;
        InputStream in = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(destFile));
            URLConnection conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[BUFFRE_SIZE];
            int numRead;
            long totalSize = conn.getContentLength();
            long transferred = 0;
            started(totalSize);
            while (!checkAbortFlag() && (numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                out.flush();
                transferred += numRead;
                progress(transferred);
            }
            if (checkAbortFlag()) {
                cancelled();
            } else {
                finished();
            }
            if (checkAbortFlag()) {
                throw new RequestCancelledException();
            }
        } finally {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        }
        return null;
    }

    /**
     * Ustawia plik, do którego zapisany będzie wynik pobierania.
     *
     * @param destFile plik
     * @throws IOException w przypadku, gdy aplikacja nie ma uprawnień zapisu do
     *                     wskazanego pliku lub wystąpił inny błąd IO
     */
    public void setDestFile(File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        if (!destFile.canWrite()) {
            throw new IOException("Cannot write to file.");
        }
        this.destFile = destFile;
    }

    @Override
    protected void checkState() throws IllegalStateException {
        super.checkState();
        if (destFile == null) {
            throw new IllegalStateException("Destination file not set");
        }
    }

    @Override
    protected HttpUriRequest createRequest() throws URISyntaxException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
