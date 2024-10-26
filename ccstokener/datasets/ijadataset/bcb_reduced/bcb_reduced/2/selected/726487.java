package dsr.comms;

import java.applet.Applet;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Properties;
import ssmith.io.IOFunctions;
import ssmith.io.TextFile;
import dsr.AppletMain;
import dsr.IDisplayMessages;
import dsr.start.StartupNew;
import dsrwebserver.HTTPHeaders;

public final class WGet4Client extends Authenticator {

    private static final String PROXY_FILENAME = "proxy.txt";

    public static int T_STD = 0;

    public static int T_SPECIFIED = 1;

    public static int T_PLAYBACK = 2;

    private static final int MAX_TRIES = 6;

    private static final int TIMEOUT = 1000 * 60 * 2;

    private String s_url;

    private String text_response = "";

    private byte[] data_response;

    private int response_code;

    private String redirect_to = "";

    private int tries_remaining = MAX_TRIES;

    private IDisplayMessages show_msg;

    private static boolean checked_for_proxy = false;

    private static String login, pwd, proxy, port;

    /**
	 * Call this for playback data
	 */
    public WGet4Client(Applet app, String _post_data, boolean _encode) throws UnknownHostException, IOException {
        this(null, T_PLAYBACK, app.getDocumentBase().getHost().length() > 0 ? "http://" + app.getDocumentBase().getHost() + "/appletcomm/PlaybackComms.cls" : "http://127.0.0.1/appletcomm/PlaybackComms.cls", _post_data, _encode);
    }

    /**
	 * Call this for std comms data
	 */
    public WGet4Client(IDisplayMessages _owner, String _post_data, boolean _encode) throws UnknownHostException, IOException {
        this(_owner, T_STD, null, _post_data, _encode);
    }

    /**
	 * Call this for anything else data
	 */
    public WGet4Client(IDisplayMessages _owner, int type, String full_url, String _post_data, boolean _encode) throws UnknownHostException, IOException {
        super();
        show_msg = _owner;
        if (checked_for_proxy == false && type != T_PLAYBACK) {
            checked_for_proxy = true;
            try {
                if (new File(PROXY_FILENAME).canRead()) {
                    TextFile tf = new TextFile();
                    tf.openFile(PROXY_FILENAME, TextFile.READ);
                    proxy = tf.readLine();
                    port = tf.readLine();
                    login = tf.readLine();
                    pwd = tf.readLine();
                    tf.close();
                    if (show_msg != null) {
                        show_msg.displayMessage("Using Proxy " + proxy + ":" + port);
                    }
                } else {
                    if (show_msg != null) {
                    }
                }
            } catch (Exception ex) {
                AppletMain.HandleError(null, ex);
            }
        }
        String post_data = "";
        if (_post_data.length() > 0) {
            if (_encode) {
                String b64 = CommFuncs.Encode(_post_data);
                post_data = "post=" + HTTPHeaders.URLEncodeString(b64);
            } else {
                post_data = "post=" + HTTPHeaders.URLEncodeString(_post_data);
            }
        }
        if (type == T_STD) {
            s_url = AppletMain.URL_FOR_CLIENT + "/appletcomm/MiscCommsPage.cls";
        } else if (type == T_SPECIFIED || type == T_PLAYBACK) {
            s_url = full_url;
        }
        while (tries_remaining > 0) {
            tries_remaining--;
            try {
                while (true) {
                    this.post(s_url, post_data);
                    if (this.response_code == 302 && this.redirect_to.length() > 0) {
                        this.s_url = redirect_to;
                    } else if (response_code == 200) {
                        if (tries_remaining + 1 < MAX_TRIES) {
                            displayMessage("* " + StartupNew.strings.getTranslation("Connected to server"));
                        }
                        tries_remaining = 0;
                        break;
                    } else if (response_code == 500) {
                        throw new RuntimeException("Server error");
                    } else {
                        throw new RuntimeException("" + this.response_code);
                    }
                }
            } catch (java.net.ConnectException ex) {
                displayMessage(" *" + StartupNew.strings.getTranslation("Failed to connect to server.") + " *");
            } catch (java.io.FileNotFoundException ex) {
                AppletMain.HandleError(null, ex, false);
                displayMessage(StartupNew.strings.getTranslation("Comms error: " + ex.toString()));
                break;
            } catch (IOException ex) {
                AppletMain.HandleError(null, ex, false);
                displayMessage(StartupNew.strings.getTranslation("Comms error: " + ex.toString()));
            }
            if (tries_remaining > 0) {
                displayMessage("* " + StartupNew.strings.getTranslation("Retrying") + "...");
            }
        }
        if (getResponseCode() != 200) {
            if (getResponseCode() > 0) {
                throw new IOException("Got response " + getResponseCode() + " from server.");
            } else {
                throw new IOException("Could not connect to server.");
            }
        }
    }

    private void displayMessage(String s) {
        if (this.show_msg != null) {
            this.show_msg.displayMessage(s);
        }
    }

    public int getResponseCode() {
        return this.response_code;
    }

    public String getResponse() {
        return text_response.toString();
    }

    public byte[] getDataResponse() {
        if (data_response == null && this.text_response != null) {
            data_response = this.text_response.getBytes();
        }
        return data_response;
    }

    private void post(String server, String post_data) throws IOException {
        URL url = new URL(server.replaceAll("\\\\", "/"));
        if (proxy != null) {
            Authenticator.setDefault(this);
            Properties systemProperties = System.getProperties();
            systemProperties.setProperty("http.proxySet", "true");
            systemProperties.setProperty("http.proxyHost", proxy);
            systemProperties.setProperty("http.proxyPort", port);
        }
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        if (post_data != null) {
            if (post_data.length() > 0) {
                urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConn.setRequestProperty("Content-Length", "" + post_data.length());
                DataOutputStream dos = new DataOutputStream(urlConn.getOutputStream());
                dos.writeBytes(post_data);
                dos.writeBytes("\r\n");
                dos.flush();
                dos.close();
            }
        }
        response_code = urlConn.getResponseCode();
        DataInputStream dis = new DataInputStream(urlConn.getInputStream());
        IOFunctions.WaitForData(dis, TIMEOUT);
        if (urlConn.getContentType().startsWith("text")) {
            String s = "";
            StringBuffer str = new StringBuffer();
            while ((s = dis.readLine()) != null) {
                str.append(s + "\n");
            }
            if (str.length() > 0) {
                str.delete(str.length() - 1, str.length());
            }
            text_response = str.toString();
        } else {
            int bytes_remaining = urlConn.getContentLength();
            int bytes_read = 0;
            data_response = new byte[urlConn.getContentLength()];
            while (bytes_remaining > 0) {
                try {
                    int len = dis.read(data_response, bytes_read, bytes_remaining);
                    if (len < 0) {
                        break;
                    }
                    bytes_read += len;
                    bytes_remaining -= len;
                } catch (java.lang.IndexOutOfBoundsException ex) {
                    ex.printStackTrace();
                }
            }
        }
        dis.close();
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(login, pwd.toCharArray());
    }
}
