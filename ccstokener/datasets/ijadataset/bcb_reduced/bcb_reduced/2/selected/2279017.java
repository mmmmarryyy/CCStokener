package jade.imtp.leap.http;

import jade.mtp.TransportAddress;
import jade.imtp.leap.JICP.Connection;
import jade.imtp.leap.JICP.JICPPacket;
import jade.imtp.leap.JICP.JICPProtocol;
import java.io.*;
import java.net.*;
import javax.swing.*;

/**
 * Class declaration
 * @author Giovanni Caire - TILAB
 */
class HTTPClientConnection extends Connection {

    private static final int READY = 0;

    private static final int WRITTEN = 1;

    private static final int CLOSED = -1;

    private HttpURLConnection hc;

    private String url;

    private InputStream is;

    private OutputStream os;

    private int state;

    /**
	 * Constructor declaration
	 */
    public HTTPClientConnection(TransportAddress ta) {
        url = getProtocol() + ta.getHost() + ":" + ta.getPort() + "/jade";
        state = READY;
        Authenticator.setDefault(new Authenticator() {

            private String username = null;

            private String password = null;

            protected PasswordAuthentication getPasswordAuthentication() {
                if (username == null) {
                    username = System.getProperty("http.username");
                    password = System.getProperty("http.password");
                    if (username == null) {
                        JTextField usrTF = new JTextField();
                        JPasswordField pwdTF = new JPasswordField();
                        Object[] message = new Object[] { "Insert username and password", usrTF, pwdTF };
                        int ret = JOptionPane.showConfirmDialog(null, message, null, JOptionPane.OK_CANCEL_OPTION);
                        if (ret == 0) {
                            username = usrTF.getText();
                            password = pwdTF.getText();
                        }
                    }
                }
                return new PasswordAuthentication(username, password.toCharArray());
            }
        });
    }

    protected String getProtocol() {
        return "http://";
    }

    protected HttpURLConnection open(String url) throws MalformedURLException, IOException {
        return (HttpURLConnection) (new URL(url)).openConnection();
    }

    public int writePacket(JICPPacket pkt) throws IOException {
        if (state == READY) {
            int ret = 0;
            hc = open(url);
            hc.setDoOutput(true);
            hc.setRequestMethod("POST");
            hc.connect();
            os = hc.getOutputStream();
            ret = pkt.writeTo(os);
            state = WRITTEN;
            return ret;
        } else {
            throw new IOException("Write not available");
        }
    }

    public JICPPacket readPacket() throws IOException {
        if (state == WRITTEN) {
            try {
                is = hc.getInputStream();
                return JICPPacket.readFrom(is);
            } finally {
                try {
                    close();
                } catch (Exception e) {
                }
            }
        } else {
            throw new IOException("Wrong connection state " + state);
        }
    }

    /**
	 */
    public void close() throws IOException {
        state = CLOSED;
        try {
            is.close();
        } catch (Exception e) {
        }
        is = null;
        try {
            os.close();
        } catch (Exception e) {
        }
        os = null;
        try {
            hc.disconnect();
        } catch (Exception e) {
        }
        hc = null;
    }

    /**
	 */
    public String getRemoteHost() throws Exception {
        throw new Exception("Unsupported operation");
    }
}
