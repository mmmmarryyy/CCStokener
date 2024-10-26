package it.freax.fpm.core.download;

import it.freax.fpm.util.ErrorHandler;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

/**
 * This class extends AbstractDownload class for downloading file
 * from FTP protocol.
 * 
 * @author kLeZ-hAcK
 * @version 0.1
 */
public class FtpDownload extends AbstractDownload {

    /**
	 * Costructor for FtpDownload.
	 * 
	 * @param url
	 * @param path
	 */
    public FtpDownload(URL url, String path) {
        super(url, path);
        download();
    }

    /**
	 * Costructor for FtpDownload.
	 * 
	 * @param url
	 * @param path
	 * @param proxyUrl
	 * @param port
	 */
    public FtpDownload(URL url, String path, String proxyUrl, int port) {
        super(url, path, proxyUrl, port);
        download();
    }

    /**
	 * Costructor for FtpDownload.
	 * 
	 * @param url
	 * @param path
	 * @param proxyUrl
	 * @param port
	 * @param userName
	 * @param password
	 */
    public FtpDownload(URL url, String path, String proxyUrl, int port, String userName, String password) {
        super(url, path, proxyUrl, port, userName, password);
        download();
    }

    /**
	 * This method permits the download of a file from a url through FTP
	 * protocol.
	 */
    @Override
    public void run() {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            if (useProxy) {
                Properties systemProperties = System.getProperties();
                systemProperties.setProperty("ftp.proxyHost", proxyUrl);
                systemProperties.setProperty("ftp.proxyPort", String.valueOf(port));
                if (useAuthentication) {
                    Authenticator.setDefault(new SimpleAuthenticator(userName, password));
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append(path);
            if (!path.endsWith(System.getProperty("file.separator"))) {
                sb.append(System.getProperty("file.separator"));
            }
            sb.append(getFileName(url));
            status = DOWNLOADING;
            URLConnection urlc = url.openConnection();
            bis = new BufferedInputStream(urlc.getInputStream());
            bos = new BufferedOutputStream(new FileOutputStream(sb.toString()));
            while (status == DOWNLOADING) {
                byte[] buffer = new byte[MAX_BUFFER_SIZE];
                int read = bis.read(buffer);
                if (read == -1) {
                    break;
                }
                bos.write(buffer, 0, read);
                downloaded += read;
            }
            status = COMPLETE;
            stateChanged();
        } catch (MalformedURLException e) {
            ErrorHandler.getOne(this.getClass(), debug).handle(e);
            setDebugMessage(e.toString(), true);
            error();
        } catch (IOException e) {
            ErrorHandler.getOne(this.getClass(), debug).handle(e);
            setDebugMessage(e.toString(), true);
            error();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException ioe) {
                    ErrorHandler.getOne(this.getClass(), debug).handle(ioe);
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException ioe) {
                    ErrorHandler.getOne(this.getClass(), debug).handle(ioe);
                }
            }
        }
    }
}
