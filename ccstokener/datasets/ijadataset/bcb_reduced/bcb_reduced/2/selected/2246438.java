package org.openconcerto.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class NetUtils {

    /**
     * Whether the passed address refers to this computer.
     * 
     * @param addr an ip or dns address, eg "192.168.28.52".
     * @return <code>true</code> if <code>addr</code> is bound to an interface of this computer.
     */
    public static final boolean isSelfAddr(String addr) {
        if (addr == null) return false;
        if (addr.startsWith("127.") || addr.startsWith("localhost")) return true;
        try {
            final Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                final NetworkInterface ni = en.nextElement();
                final Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    final InetAddress inetAddress = addresses.nextElement();
                    if (addr.startsWith(inetAddress.getHostAddress())) return true;
                }
            }
            return false;
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static final HostnameVerifier HostnameNonVerifier = new HostnameVerifier() {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    public static final String getHTTPContent(String address, final boolean dontVerify) {
        String content = "";
        OutputStream out = null;
        HttpsURLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(address);
            out = new ByteArrayOutputStream();
            conn = (HttpsURLConnection) url.openConnection();
            if (dontVerify) {
                conn.setHostnameVerifier(HostnameNonVerifier);
            }
            in = conn.getInputStream();
            final byte[] buffer = new byte[1024];
            int numRead;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
            }
            content = out.toString();
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
        return content;
    }
}
