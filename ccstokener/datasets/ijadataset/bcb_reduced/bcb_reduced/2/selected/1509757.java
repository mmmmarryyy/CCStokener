package it.freax.fpm.core.download;

import it.freax.fpm.util.ErrorHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

/**
 * This class extends AbstractDownload class for downloading file
 * from HTTP protocol.
 * 
 * @author kLeZ-hAcK
 * @version 0.1
 */
public final class HttpDownload extends AbstractDownload {

    /**
	 * Costructor for HttpDownload.
	 * 
	 * @param url
	 * @param path
	 */
    public HttpDownload(URL url, String path) {
        super(url, path);
        download();
    }

    /**
	 * Costructor for HttpDownload.
	 * 
	 * @param url
	 * @param path
	 * @param proxyUrl
	 * @param port
	 */
    public HttpDownload(URL url, String path, String proxyUrl, int port) {
        super(url, path, proxyUrl, port);
        download();
    }

    /**
	 * Costructor for HttpDownload.
	 * 
	 * @param url
	 * @param path
	 * @param proxyUrl
	 * @param port
	 * @param userName
	 * @param password
	 */
    public HttpDownload(URL url, String path, String proxyUrl, int port, String userName, String password) {
        super(url, path, proxyUrl, port, userName, password);
        download();
    }

    /**
	 * This method permits the download of a file from a url through HTTP
	 * protocol.
	 */
    @Override
    public void run() {
        RandomAccessFile rafile = null;
        InputStream stream = null;
        HttpURLConnection connection = null;
        try {
            if (useProxy) {
                Properties systemProperties = System.getProperties();
                systemProperties.setProperty("http.proxyHost", proxyUrl);
                systemProperties.setProperty("http.proxyPort", String.valueOf(port));
                if (useAuthentication) {
                    Authenticator.setDefault(new SimpleAuthenticator(userName, password));
                }
            }
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            StringBuilder sb = new StringBuilder();
            sb.append(path);
            if (!path.endsWith(System.getProperty("file.separator"))) {
                sb.append(System.getProperty("file.separator"));
            }
            sb.append(getFileName(url));
            if (downloaded > 0) {
                connection.setRequestProperty("Range", "bytes=" + downloaded + "-");
            }
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                setDebugMessage("Response code is not 200: ", true);
                setDebugMessage(String.valueOf(connection.getResponseCode()), true);
                setDebugMessage(String.valueOf(connection.getResponseMessage()), true);
                if (responseCode >= 300) {
                    error();
                    return;
                }
            }
            long contentLength = Long.parseLong(connection.getHeaderField("content-length"));
            if (contentLength < 1) {
                setDebugMessage("Content length is not valid.", false);
                error();
                return;
            }
            if (size == -1) {
                size = contentLength;
                stateChanged();
            }
            rafile = new RandomAccessFile(sb.toString(), "rw");
            rafile.seek(downloaded);
            stream = connection.getInputStream();
            while (status == DOWNLOADING) {
                byte buffer[];
                if (size - downloaded > MAX_BUFFER_SIZE) {
                    buffer = new byte[MAX_BUFFER_SIZE];
                } else if ((size - downloaded < MAX_BUFFER_SIZE) && (size - downloaded > 0)) {
                    buffer = new byte[(int) (size - downloaded)];
                } else {
                    break;
                }
                int read = stream.read(buffer);
                if (read == -1) {
                    break;
                }
                rafile.write(buffer, 0, read);
                downloaded += read;
                stateChanged();
            }
            if (status == DOWNLOADING) {
                status = COMPLETE;
                stateChanged();
            }
        } catch (Exception e) {
            ErrorHandler.getOne(this.getClass(), debug).handle(e);
            setDebugMessage(e.toString(), true);
            error();
        } finally {
            if (rafile != null) {
                try {
                    rafile.close();
                } catch (Exception e) {
                    ErrorHandler.getOne(this.getClass(), debug).handle(e);
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    ErrorHandler.getOne(this.getClass(), debug).handle(e);
                }
            }
        }
    }
}
