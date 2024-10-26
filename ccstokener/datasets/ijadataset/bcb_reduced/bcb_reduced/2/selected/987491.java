package net.sf.sail.webstart;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import javax.jnlp.DownloadService;
import javax.jnlp.DownloadServiceListener;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.JOptionPane;

public class WebstartUtils {

    static Proxy webstartProxy = null;

    static URLStreamHandlerFactory streamHandler = null;

    public static void setProxy(String address, int port) {
        System.out.println("Setting proxy address: " + address + " port: " + port);
        if (streamHandler == null) {
            streamHandler = new URLStreamHandlerFactory() {

                public URLStreamHandler createURLStreamHandler(String protocol) {
                    if (protocol.equals("http")) {
                        System.err.println("returning protocol handler");
                        return new sun.net.www.protocol.http.Handler() {

                            protected URLConnection openConnection(URL arg0) throws IOException {
                                System.err.println("opening: " + arg0);
                                if (webstartProxy == null) {
                                    return super.openConnection(arg0);
                                } else {
                                    return super.openConnection(arg0, webstartProxy);
                                }
                            }
                        };
                    }
                    Exception e = new Exception("proxy interceptor can't handle protocol: " + protocol);
                    return null;
                }
            };
            URL.setURLStreamHandlerFactory(streamHandler);
        }
        if (address == null) {
            webstartProxy = null;
        } else {
            webstartProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(address, port));
        }
    }

    public static void loadPart(String partName) {
        try {
            DownloadService downloadService = (DownloadService) ServiceManager.lookup("javax.jnlp.DownloadService");
            downloadService.loadPart(partName, new DownloadServiceListener() {

                public void downloadFailed(URL arg0, String arg1) {
                }

                public void progress(URL arg0, String arg1, long arg2, long arg3, int arg4) {
                }

                public void upgradingArchive(URL arg0, String arg1, int arg2, int arg3) {
                }

                public void validating(URL arg0, String arg1, long arg2, long arg3, int arg4) {
                }
            });
        } catch (UnavailableServiceException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.err.println("Started 2...");
        setProxy("127.0.0.1", 8888);
        try {
            URL url = new URL("http://www.concord.org/");
            URLConnection openConnection = url.openConnection();
            InputStream inputStream = openConnection.getInputStream();
            inputStream.read();
            inputStream.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            System.setProperty("http.proxyHost", "127.0.0.1");
            System.setProperty("http.proxyPort", "8888");
            URL url = new URL("http://www.concord.org/");
            URLConnection openConnection = url.openConnection();
            InputStream inputStream = openConnection.getInputStream();
            inputStream.read();
            inputStream.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JOptionPane.showConfirmDialog(null, "First Part Complete");
        loadPart("second");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        JOptionPane.showConfirmDialog(null, "Second Part Complete");
    }
}
