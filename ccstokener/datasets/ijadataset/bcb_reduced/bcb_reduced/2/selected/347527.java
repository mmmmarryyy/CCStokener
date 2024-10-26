package org.das2.client;

import java.util.logging.Level;
import org.das2.stream.StreamDescriptor;
import org.das2.stream.StreamException;
import org.das2.stream.DasStreamFormatException;
import org.das2.util.URLBuddy;
import org.das2.DasIOException;
import org.das2.system.DasLogger;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;
import org.das2.DasException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author  jbf
 * A DasServer is the object that holds the methods of a remote das server.
 * These include, for example, getLogo() which returns a graphical mnemonic
 * for the server, authenticate() and setPassword().
 */
public class DasServer {

    private String host;

    private String path;

    private int port;

    private HashMap keys;

    private Key key;

    private static final Logger logger = DasLogger.getLogger(DasLogger.DATA_TRANSFER_LOG);

    private static HashMap instanceHashMap = new HashMap();

    public static final DasServer plasmaWaveGroup;

    public static final DasServer sarahandjeremy;

    static {
        try {
            plasmaWaveGroup = DasServer.create(new URL("http://www-pw.physics.uiowa.edu/das/das2Server"));
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        try {
            sarahandjeremy = DasServer.create(new URL("http://www.sarahandjeremy.net/das/dasServer.cgi"));
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Creates a new instance of DasServer */
    private DasServer(String host, String path) {
        String[] s = host.split(":");
        if (s.length > 1) {
            this.port = Integer.parseInt(s[1]);
            host = s[0];
        } else {
            port = -1;
        }
        this.host = host;
        this.path = path;
        this.keys = new HashMap();
    }

    public String getURL() {
        if (port == -1) {
            return "http://" + host + path;
        } else {
            return "http://" + host + ":" + port + path;
        }
    }

    public static DasServer create(URL url) {
        String host = url.getHost();
        int port = url.getPort();
        if (port != -1) {
            host += ":" + port;
        }
        String key = "http://" + host + url.getPath();
        if (instanceHashMap.containsKey(key)) {
            logger.log(Level.INFO, "Using existing DasServer for {0}", url);
            return (DasServer) instanceHashMap.get(key);
        } else {
            String path = url.getPath();
            logger.log(Level.INFO, "Creating DasServer for {0}", url);
            DasServer result = new DasServer(host, path);
            instanceHashMap.put(key, result);
            return result;
        }
    }

    public String getName() {
        String formData = "server=id";
        try {
            URL server = new URL("http", host, port, path + "?" + formData);
            logger.log(Level.INFO, "connecting to {0}", server);
            URLConnection urlConnection = server.openConnection();
            urlConnection.connect();
            InputStream in = urlConnection.getInputStream();
            String result = new String(read(in));
            logger.log(Level.INFO, "response={0}", result);
            return result;
        } catch (IOException e) {
            return "";
        }
    }

    public ImageIcon getLogo() {
        String formData = "server=logo";
        try {
            URL server = new URL("http", host, port, path + "?" + formData);
            logger.log(Level.INFO, "connecting to {0}", server);
            URLConnection urlConnection = server.openConnection();
            urlConnection.connect();
            InputStream in = urlConnection.getInputStream();
            byte[] data = read(in);
            logger.log(Level.INFO, "response={0} bytes", data.length);
            return new ImageIcon(data);
        } catch (IOException e) {
            return new ImageIcon();
        }
    }

    public TreeModel getDataSetListWithDiscovery() throws org.das2.DasException {
        String formData = "server=discovery";
        try {
            URL server = new URL("http", host, port, path + "?" + formData);
            logger.log(Level.INFO, "connecting to {0}", server);
            URLConnection urlConnection = server.openConnection();
            urlConnection.connect();
            InputStream in = urlConnection.getInputStream();
            TreeModel result = createModel(in);
            logger.log(Level.INFO, "response->{0}", result);
            return result;
        } catch (IOException e) {
            throw new DasIOException(e.getMessage());
        }
    }

    public TreeModel getDataSetList() throws org.das2.DasException {
        String formData = "server=list";
        try {
            URL server = new URL("http", host, port, path + "?" + formData);
            logger.log(Level.INFO, "connecting to {0}", server);
            URLConnection urlConnection = server.openConnection();
            urlConnection.connect();
            InputStream in = urlConnection.getInputStream();
            TreeModel result = createModel(in);
            logger.log(Level.INFO, "response->{0}", result);
            return result;
        } catch (IOException e) {
            throw new DasIOException(e.getMessage());
        }
    }

    private TreeModel createModel(InputStream uin) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(uin));
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(getURL(), true);
        DefaultTreeModel model = new DefaultTreeModel(root, true);
        String line = in.readLine();
        while (line != null) {
            DefaultMutableTreeNode current = root;
            StringTokenizer tokenizer = new StringTokenizer(line, "/");
            token: while (tokenizer.hasMoreTokens()) {
                String tok = tokenizer.nextToken();
                for (int index = 0; index < current.getChildCount(); index++) {
                    String str = current.getChildAt(index).toString();
                    if (str.equals(tok)) {
                        current = (DefaultMutableTreeNode) current.getChildAt(index);
                        continue token;
                    }
                }
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(tok, (tokenizer.hasMoreElements() ? true : line.endsWith("/")));
                current.add(node);
                current = node;
            }
            line = in.readLine();
        }
        return model;
    }

    public StandardDataStreamSource getStandardDataStreamSource(URL url) {
        return new WebStandardDataStreamSource(this, url);
    }

    public StreamDescriptor getStreamDescriptor(URL dataSetID) throws DasException {
        try {
            String dsdf = dataSetID.getQuery().split("&")[0];
            URL url = new URL("http", host, port, path + "?server=dsdf&dataset=" + dsdf);
            logger.log(Level.INFO, "connecting to {0}", url);
            URLConnection connection = url.openConnection();
            connection.connect();
            String contentType = connection.getContentType();
            String[] s1 = contentType.split(";");
            contentType = s1[0];
            if (contentType.equalsIgnoreCase("text/plain")) {
                PushbackReader reader = new PushbackReader(new InputStreamReader(connection.getInputStream()), 4);
                char[] four = new char[4];
                reader.read(four);
                if (new String(four).equals("[00]")) {
                    logger.info("response is a das2Stream");
                    reader.skip(6);
                    Document header = StreamDescriptor.parseHeader(reader);
                    Element root = header.getDocumentElement();
                    if (root.getTagName().equals("stream")) {
                        return new StreamDescriptor(root);
                    } else if (root.getTagName().equals("exception")) {
                        logger.info("response is an exception");
                        String type = root.getAttribute("type");
                        StreamException se = new StreamException("stream exception: " + type);
                        throw new DasException("stream exception: " + type, se);
                    } else if (root.getTagName().equals("")) {
                        throw new DasStreamFormatException();
                    } else {
                        throw new DasStreamFormatException();
                    }
                } else {
                    logger.info("response is a legacy descriptor");
                    reader.unread(four);
                    BufferedReader in = new BufferedReader(reader);
                    StreamDescriptor result = StreamDescriptor.createLegacyDescriptor(in);
                    return result;
                }
            } else {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder message = new StringBuilder();
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    message.append(line).append('\n');
                }
                throw new IOException(message.toString());
            }
        } catch (MalformedURLException e) {
            throw new DataSetDescriptorNotAvailableException("malformed URL");
        } catch (FileNotFoundException e) {
            throw new DasServerNotFoundException(e.getMessage());
        } catch (IOException e) {
            throw new DasIOException(e.toString());
        }
    }

    public Key authenticate(String user, String passCrypt) {
        try {
            Key result = null;
            String formData = "server=authenticator";
            formData += "&user=" + URLBuddy.encodeUTF8(user);
            formData += "&passwd=" + URLBuddy.encodeUTF8(passCrypt);
            URL server = new URL("http", host, port, path + "?" + formData);
            logger.log(Level.INFO, "connecting to {0}", server);
            InputStream in = server.openStream();
            BufferedInputStream bin = new BufferedInputStream(in);
            String serverResponse = readServerResponse(bin);
            String errTag = "error";
            String keyTag = "key";
            if (serverResponse.substring(0, keyTag.length() + 2).equals("<" + keyTag + ">")) {
                int index = serverResponse.indexOf("</" + keyTag + ">");
                String keyString = serverResponse.substring(keyTag.length() + 2, index);
                result = new Key(keyString);
            } else {
                result = null;
            }
            return result;
        } catch (UnsupportedEncodingException uee) {
            throw new AssertionError("UTF-8 not supported");
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * returns a List<String> of resource Id's available with this key
     */
    public List groups(Key key) {
        try {
            String formData = "server=groups";
            formData += "&key=" + URLBuddy.encodeUTF8(key.toString());
            URL server = new URL("http", host, port, path + "?" + formData);
            logger.log(Level.INFO, "connecting to {0}", server);
            InputStream in = server.openStream();
            BufferedInputStream bin = new BufferedInputStream(in);
            String serverResponse = readServerResponse(bin);
            String[] groups = serverResponse.split(",");
            ArrayList result = new ArrayList();
            for (int i = 0; i < groups.length; i++) {
                groups[i] = groups[i].trim();
                if (!"".equals(groups[i])) result.add(groups[i]);
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void changePassword(String user, String oldPass, String newPass) throws DasServerException {
        try {
            String formData = "server=changePassword";
            formData += "&user=" + URLBuddy.encodeUTF8(user);
            String cryptPass = org.das2.util.Crypt.crypt(oldPass);
            formData += "&passwd=" + URLBuddy.encodeUTF8(cryptPass);
            String cryptNewPass = org.das2.util.Crypt.crypt(newPass);
            formData += "&newPasswd=" + URLBuddy.encodeUTF8(cryptNewPass);
            URL server = new URL("http", host, port, path + "?" + formData);
            logger.log(Level.INFO, "connecting to {0}", server);
            InputStream in = server.openStream();
            BufferedInputStream bin = new BufferedInputStream(in);
            String serverResponse = readServerResponse(bin);
            String errTag = "error";
            String keyTag = "key";
            if (serverResponse.substring(0, errTag.length() + 2).equals("<" + errTag + ">")) {
                int index = serverResponse.indexOf("</" + errTag + ">");
                String errString = serverResponse.substring(errTag.length() + 2, index);
                if (errString.equals("<badAuthentication/>")) {
                    throw new DasServerException("Bad User/Pass");
                }
            }
        } catch (UnsupportedEncodingException uee) {
            throw new AssertionError("UTF-8 not supported");
        } catch (IOException e) {
            throw new DasServerException("Failed Connection");
        }
    }

    public String readServerResponse(BufferedInputStream in) {
        in.mark(Integer.MAX_VALUE);
        String das2Response;
        byte[] data = new byte[4096];
        int offset = 0;
        try {
            int bytesRead = in.read(data, offset, 4096 - offset);
            String das2ResponseTag = "das2Response";
            if (bytesRead < (das2ResponseTag.length() + 2)) {
                offset += bytesRead;
                bytesRead = in.read(data, offset, 4096 - offset);
            }
            if (new String(data, 0, 14, "UTF-8").equals("<" + das2ResponseTag + ">")) {
                while (new String(data, 0, offset, "UTF-8").indexOf("</" + das2ResponseTag + ">") == -1 && offset < 4096) {
                    offset += bytesRead;
                    bytesRead = in.read(data, offset, 4096 - offset);
                }
                int index = new String(data, 0, offset, "UTF-8").indexOf("</" + das2ResponseTag + ">");
                das2Response = new String(data, 14, index - 14);
                org.das2.util.DasDie.println("das2Response=" + das2Response);
                in.reset();
                long n = das2Response.length() + 2 * das2ResponseTag.length() + 5;
                while (n > 0) {
                    long k = in.skip(n);
                    n -= k;
                }
            } else {
                in.reset();
                das2Response = "";
            }
        } catch (IOException e) {
            das2Response = "";
        }
        logger.log(Level.INFO, "response={0}", das2Response);
        return das2Response;
    }

    private byte[] read(InputStream uin) throws IOException {
        LinkedList<byte[]> list = new LinkedList();
        byte[] data;
        int bytesRead = 0;
        InputStream in = uin;
        data = new byte[4096];
        int lastBytesRead = -1;
        int offset = 0;
        bytesRead = in.read(data, offset, 4096 - offset);
        while (bytesRead != -1) {
            offset += bytesRead;
            lastBytesRead = offset;
            if (offset == 4096) {
                list.addLast(data);
                data = new byte[4096];
                offset = 0;
            }
            bytesRead = in.read(data, offset, 4096 - offset);
        }
        if (lastBytesRead < 4096) {
            list.addLast(data);
        }
        if (list.size() == 0) {
            return new byte[0];
        }
        int dataLength = (list.size() - 1) * 4096 + lastBytesRead;
        data = new byte[dataLength];
        Iterator<byte[]> iterator = list.iterator();
        int i;
        for (i = 0; i < list.size() - 1; i++) {
            System.arraycopy(iterator.next(), 0, data, i * 4096, 4096);
        }
        System.arraycopy(iterator.next(), 0, data, i * 4096, lastBytesRead);
        return data;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public URL getURL(String formData) throws MalformedURLException {
        return new URL("http", host, port, path + "?" + formData);
    }

    public Key getKey(String resource) {
        synchronized (this) {
            if (keys.get(resource) == null) {
                Authenticator authenticator;
                authenticator = new Authenticator(this, resource);
                Key key1 = authenticator.authenticate();
                if (key1 != null) keys.put(resource, key1);
            }
        }
        return (Key) keys.get(resource);
    }

    public void setKey(Key key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return this.getURL();
    }
}
