package net.sourceforge.NetProcessor.DataAccess.HTTP;

import java.*;
import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.*;
import javax.activation.MimeType;
import javax.xml.bind.*;
import net.sourceforge.NetProcessor.Core.*;

public class DataAccess implements IDataAccess {

    public DataAccess(String url, String username, String password) {
        mServer = url;
        mUsername = (username != null) ? username : "";
        mPassword = (password != null) ? password : "";
    }

    public static IDataAccess createInstance(String url, String username, String password) {
        return new DataAccess(url, username, password);
    }

    public NodeInfo[] searchNodes(String searchQuery, int loadProperties) {
        NodeInfo[] nodes = null;
        try {
            String query = mServer + "search.php" + ("?query=" + URLEncoder.encode(searchQuery, "UTF-8")) + ("&mask=" + loadProperties);
            URL url = new URL(query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setAllowUserInteraction(false);
            conn.setRequestMethod("GET");
            setCredentials(conn);
            conn.connect();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream stream = conn.getInputStream();
                MimeType contentType = new MimeType(conn.getContentType());
                if (contentType.getBaseType().equals("text/xml")) {
                    try {
                        JAXBContext context = JAXBContext.newInstance(NetProcessorInfo.class);
                        Unmarshaller unm = context.createUnmarshaller();
                        NetProcessorInfo root = (NetProcessorInfo) unm.unmarshal(stream);
                        if (root != null) {
                            nodes = root.getNodes();
                        }
                    } catch (Exception ex) {
                    }
                }
                stream.close();
            }
        } catch (Exception ex) {
        }
        return (nodes != null) ? nodes : new NodeInfo[0];
    }

    public NodeInfo loadNode(int id, int properties) {
        NodeInfo info = null;
        if (((properties & ~NodePropertyFlag.Data) != 0) || (properties == 0)) {
            info = loadNodeMeta(id, properties & ~NodePropertyFlag.Data);
        } else {
            info = new NodeInfo();
            info.setId(id);
        }
        if ((info != null) && ((properties & NodePropertyFlag.Data) != 0)) {
            if (!loadNodeData(info)) {
                info = null;
            }
        }
        return info;
    }

    public boolean saveNode(NodeInfo info, int properties) {
        boolean rCode = false;
        if (info != null) {
            if (saveNodeMeta(info, properties & ~NodePropertyFlag.Data)) {
                rCode = ((properties & NodePropertyFlag.Data) != 0) ? saveNodeData(info) : true;
            }
        }
        return rCode;
    }

    private void setCredentials(HttpURLConnection conn) {
        sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
        String encodedUserPwd = encoder.encode((mUsername + ":" + mPassword).getBytes());
        conn.setRequestProperty("Authorization", "Basic " + encodedUserPwd);
    }

    private NodeInfo loadNodeMeta(int id, int properties) {
        String query = mServer + "load.php" + ("?id=" + id) + ("&mask=" + properties);
        NodeInfo info = null;
        try {
            URL url = new URL(query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setAllowUserInteraction(false);
            conn.setRequestMethod("GET");
            setCredentials(conn);
            conn.connect();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream stream = conn.getInputStream();
                MimeType contentType = new MimeType(conn.getContentType());
                if (contentType.getBaseType().equals("text/xml")) {
                    try {
                        JAXBContext context = JAXBContext.newInstance(NetProcessorInfo.class);
                        Unmarshaller unm = context.createUnmarshaller();
                        NetProcessorInfo root = (NetProcessorInfo) unm.unmarshal(stream);
                        if ((root != null) && (root.getNodes().length == 1)) {
                            info = root.getNodes()[0];
                        }
                    } catch (Exception ex) {
                    }
                }
                stream.close();
            }
        } catch (Exception ex) {
        }
        return info;
    }

    private boolean loadNodeData(NodeInfo info) {
        String query = mServer + "load.php" + ("?id=" + info.getId()) + ("&mask=" + NodePropertyFlag.Data);
        boolean rCode = false;
        try {
            URL url = new URL(query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setAllowUserInteraction(false);
            conn.setRequestMethod("GET");
            setCredentials(conn);
            conn.connect();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream stream = conn.getInputStream();
                byte[] data = new byte[0], temp = new byte[1024];
                boolean eof = false;
                while (!eof) {
                    int read = stream.read(temp);
                    if (read > 0) {
                        byte[] buf = new byte[data.length + read];
                        System.arraycopy(data, 0, buf, 0, data.length);
                        System.arraycopy(temp, 0, buf, data.length, read);
                        data = buf;
                    } else if (read < 0) {
                        eof = true;
                    }
                }
                info.setData(data);
                info.setMIMEType(new MimeType(conn.getContentType()));
                rCode = true;
                stream.close();
            }
        } catch (Exception ex) {
        }
        return rCode;
    }

    private boolean saveNode(NodeInfo info, HttpURLConnection conn) throws Exception {
        boolean rCode = false;
        conn.connect();
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream stream = conn.getInputStream();
            MimeType contentType = new MimeType(conn.getContentType());
            if (contentType.getBaseType().equals("text/xml")) {
                try {
                    JAXBContext context = JAXBContext.newInstance(NetProcessorInfo.class);
                    Unmarshaller unm = context.createUnmarshaller();
                    NetProcessorInfo root = (NetProcessorInfo) unm.unmarshal(stream);
                    if ((root != null) && (root.getNodes().length == 1)) {
                        info.setId(root.getNodes()[0].getId());
                        rCode = true;
                    }
                } catch (Exception ex) {
                }
            }
            stream.close();
        }
        return rCode;
    }

    private boolean saveNodeMeta(NodeInfo info, int properties) {
        boolean rCode = false;
        String query = mServer + "save.php" + ("?id=" + info.getId());
        try {
            URL url = new URL(query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            byte[] body = Helpers.EncodeString(Helpers.ASCII, createURLEncodedPropertyString(info, properties));
            conn.setAllowUserInteraction(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            setCredentials(conn);
            conn.setDoOutput(true);
            conn.getOutputStream().write(body);
            rCode = saveNode(info, conn);
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.toString());
        }
        return rCode;
    }

    private boolean saveNodeData(NodeInfo info) {
        boolean rCode = false;
        String query = mServer + "save.php" + ("?id=" + info.getId());
        try {
            URL url = new URL(query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String contentType = info.getMIMEType().toString();
            byte[] body = info.getData();
            conn.setAllowUserInteraction(false);
            conn.setRequestMethod("PUT");
            if (contentType.equals("")) {
                contentType = "application/octet-stream";
            }
            System.out.println("contentType: " + contentType);
            conn.setRequestProperty("Content-Type", contentType);
            setCredentials(conn);
            conn.setDoOutput(true);
            conn.getOutputStream().write(body);
            rCode = saveNode(info, conn);
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.toString());
        }
        return rCode;
    }

    private String createURLEncodedPropertyString(NodeInfo info, int properties) {
        HashMap<String, byte[]> assignments = new HashMap<String, byte[]>();
        StringBuilder buf = new StringBuilder();
        int i;
        for (i = 0; i < 31; i++) {
            int flag = 1 << i;
            if ((properties & flag) != 0) {
                switch(flag) {
                    case NodePropertyFlag.Title:
                        {
                            assignments.put(Integer.toString(flag), info.getTitleEncoded());
                            break;
                        }
                    case NodePropertyFlag.MIMEType:
                        {
                            assignments.put(Integer.toString(flag), info.getMIMETypeEncoded());
                            break;
                        }
                    case NodePropertyFlag.Data:
                        {
                            break;
                        }
                    case NodePropertyFlag.DataModifiedTime:
                        {
                            break;
                        }
                    case NodePropertyFlag.Preview:
                        {
                            assignments.put(Integer.toString(flag), info.getPreview());
                            break;
                        }
                    case NodePropertyFlag.ViewState:
                        {
                            assignments.put(Integer.toString(flag), info.getViewState());
                            break;
                        }
                    case NodePropertyFlag.Links:
                        {
                            for (LinkInfo link : info.getLinks()) {
                                StringBuilder linkKey = new StringBuilder();
                                linkKey.append(Integer.toString(flag));
                                linkKey.append('_');
                                linkKey.append(Integer.toString(link.getId()));
                                linkKey.append('_');
                                assignments.put(linkKey.toString() + "Present", new byte[0]);
                                if ((properties & NodePropertyFlag.LinkData) != 0) {
                                    for (PropertyInfo prop : link.getProperties()) {
                                        assignments.put(linkKey.toString() + prop.getName(), prop.getValue());
                                    }
                                }
                            }
                            break;
                        }
                    case NodePropertyFlag.LinkData:
                        {
                            break;
                        }
                    default:
                        {
                            break;
                        }
                }
            }
        }
        {
            sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
            for (String key : assignments.keySet()) {
                byte[] value = assignments.get(key);
                if (buf.length() > 0) {
                    buf.append('&');
                }
                buf.append(key);
                buf.append('=');
                buf.append(encoder.encode(value));
            }
        }
        return buf.toString();
    }

    private String mServer;

    private String mUsername;

    private String mPassword;
}
