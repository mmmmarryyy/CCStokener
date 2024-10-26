package org.syrup.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.URL;
import java.util.Hashtable;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;

/**
 * Provides BLOB storage functionality by sending requests to the BlobServer.
 * 
 * @author Robbert van Dalen
 */
public class BlobClient implements Serializable, Referenceable {

    static final String COPYRIGHT = "Copyright 2005 Robbert van Dalen." + "At your option, you may copy, distribute, or make derivative works under " + "the terms of The Artistic License. This License may be found at " + "http://www.opensource.org/licenses/artistic-license.php. " + "THERE IS NO WARRANTY; USE THIS PRODUCT AT YOUR OWN RISK.";

    public final String host;

    public final int port;

    public static final long serialVersionUID = 1;

    /**
     */
    public BlobClient(String host_, int port_) {
        host = host_;
        port = port_;
    }

    public BlobClient(String location) throws Exception {
        this(getHost(location), getPort(location));
    }

    private static String getHost(String location) {
        return location.split(":")[0];
    }

    private static int getPort(String location) {
        return Integer.parseInt(location.split(":")[1]);
    }

    public String getLocation() {
        return host + ":" + port;
    }

    public static void main(String[] args) throws Throwable {
        String location = "localhost:6666";
        if (args.length >= 1) {
            if (args.length >= 2) {
                location = args[1];
            }
            BlobClient client = new BlobClient(location);
            if (args[0].equals("read")) {
                client.read(System.in, System.out);
                return;
            } else if (args[0].equals("get")) {
                client.get(System.in, System.out);
                return;
            } else if (args[0].equals("write")) {
                client.write(System.in, System.out);
                return;
            } else if (args[0].equals("post")) {
                client.post(System.in, System.out);
                return;
            }
            throw new Exception("Specify command: read,get or write,post");
        }
    }

    /**
     */
    public void read(InputStream id, OutputStream result) throws Exception {
        Socket s = new Socket(host, port);
        try {
            read(id, result, s);
        } finally {
            result.flush();
            s.close();
        }
    }

    /**
     */
    public void write(InputStream content, OutputStream id) throws Exception {
        Socket s = new Socket(host, port);
        try {
            write(content, id, s);
        } finally {
            id.flush();
            s.close();
        }
    }

    /**
     */
    public void post(InputStream content, OutputStream id) throws Exception {
        Socket s = new Socket(host, port);
        try {
            ByteArrayOutputStream ido = new ByteArrayOutputStream();
            write(content, ido, s);
            if (ido.size() > 0) {
                String url = "http://" + host + ":" + port + "/";
                id.write(url.getBytes());
                id.write(ido.toByteArray());
            }
        } finally {
            id.flush();
            s.close();
        }
    }

    /**
     */
    public void get(InputStream i, OutputStream o) throws Exception {
        byte b[] = new byte[8192];
        int l = read(i, b, 0, 256);
        if (l > 0) {
            String ss = new String(b, 0, l).trim();
            URL url = new URL(ss);
            InputStream ii = null;
            try {
                ii = url.openStream();
                while ((l = ii.read(b)) >= 0) {
                    o.write(b, 0, l);
                }
            } finally {
                if (ii != null) {
                    ii.close();
                }
            }
        }
    }

    /**
     */
    private static void read(InputStream i, OutputStream o, Socket s) throws Exception {
        byte b[] = new byte[80];
        int il = read(i, b, 0, 80);
        if (il >= 80 || il <= 0) {
            throw new Exception("Input has invalid identifier length (must range between 80 characters)");
        }
        OutputStream os = s.getOutputStream();
        InputStream is = s.getInputStream();
        os.write('R');
        os.write(b, 0, il);
        s.shutdownOutput();
        while ((il = is.read(b)) >= 0) {
            o.write(b, 0, il);
        }
    }

    /**
     */
    private static void write(InputStream i, OutputStream o, Socket s) throws Exception {
        byte b[] = new byte[8192];
        int li = 0;
        OutputStream os = s.getOutputStream();
        InputStream is = s.getInputStream();
        os.write('W');
        while ((li = i.read(b)) >= 0) {
            os.write(b, 0, li);
        }
        s.shutdownOutput();
        li = read(is, b, 0, 80);
        if (li <= 80) {
            if (li >= 0) {
                o.write(b, 0, li);
            }
            is.close();
            os.close();
        } else {
            throw new Exception("Server returned invalid identifier length: " + li);
        }
    }

    /**
     */
    private static int read(InputStream i, byte[] b, int s, int l) throws IOException {
        int k = 0;
        int t = 0;
        while ((k = i.read(b, s, l)) >= 0 && s < l) {
            s += k;
            t += k;
            l -= k;
        }
        return t;
    }

    /**
     */
    public Reference getReference() throws NamingException {
        Hashtable env = new Hashtable();
        return new Reference(BlobClient.class.getName(), new StringRefAddr("blobclient", getLocation()), BlobClientFactory.class.getName(), null);
    }
}
