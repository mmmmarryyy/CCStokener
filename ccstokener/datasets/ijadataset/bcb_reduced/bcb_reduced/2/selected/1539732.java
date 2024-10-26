package khall.rpc;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.JLabel;
import khall.utils.OnlineFile;
import khall.utils.xml.XmlDocument;
import khall.utils.xml.XmlDocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class FlickrRpcClient implements Runnable, RpcClient {

    private String server;

    private String method;

    private XmlDocument packet;

    private XmlDocument returnpacket;

    private Vector params;

    private String appId;

    private String groupId;

    private String targetDir;

    private String email;

    private String password;

    private String tags;

    private String rpcType;

    private boolean complete = false;

    private JLabel statusLabel;

    /**
     * Create a flickrRpcClient
     *  
     */
    public FlickrRpcClient() {
    }

    public void run() {
        runFlickrRpc();
    }

    /**
     * Create a FlickrRpcClient with a server url and a method
     * 
     * @param server
     * @param method
     */
    public FlickrRpcClient(String server, String method, Vector params) {
        this.server = server;
        this.method = method;
        this.params = new Vector(params);
    }

    /**
     * @return Returns the appId.
     */
    public String getAppId() {
        return appId;
    }

    /**
     * @param appId The appId to set.
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * @return Returns the email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email The email to set.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return Returns the groupId.
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @param groupId The groupId to set.
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * @return Returns the password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password The password to set.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return Returns the targetDir.
     */
    public String getTargetDir() {
        return targetDir;
    }

    /**
     * @param targetDir The targetDir to set.
     */
    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }

    /**
     * @return Returns the tags.
     */
    public String getTags() {
        return tags;
    }

    /**
     * @param tags The tags to set.
     */
    public void setTags(String tags) {
        this.tags = tags;
    }

    /**
     * @return Returns the rpcType.
     */
    public String getRpcType() {
        return rpcType;
    }

    /**
     * @param rpcType The rpcType to set.
     */
    public void setRpcType(String rpcType) {
        this.rpcType = rpcType;
    }

    /**
     * 
     * @param complete
     */
    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    /**
     * 
     * @return
     */
    public boolean getComplete() {
        return complete;
    }

    /**
     * @return Returns the method.
     */
    public String getMethod() {
        return method;
    }

    /**
     * @param method
     * The method to set.
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * @return Returns the server.�
     */
    public String getServer() {
        return server;
    }

    /**
     * @param server
     * The server to set.
     */
    public void setServer(String server) {
        this.server = server;
    }

    /**
     * @return Returns the params.
     */
    public Vector getParams() {
        return params;
    }

    /**
     * @param params The params to set.
     */
    public void setParams(Vector params) {
        this.params = params;
    }

    /**
     * @return Returns the returnpacket.
     */
    public XmlDocument getReturnpacket() {
        return returnpacket;
    }

    /**
     * @param packet The packet to set.
     */
    public void setPacket(XmlDocument packet) {
        this.packet = packet;
    }

    /**
     * @return Returns the statusLabel.
     */
    public JLabel getStatusLabel() {
        return statusLabel;
    }

    /**
     * @param statusLabel The statusLabel to set.
     */
    public void setStatusLabel(JLabel parent) {
        this.statusLabel = parent;
    }

    /**
     * make the xml-rpc call
     * 
     * @param params
     * @return
     * @throws Exception
     */
    public String call(Vector params) throws Exception {
        if (server == null || method == null) {
            throw new Exception("server and method cannot be null");
        }
        packet = new XmlDocument();
        packet.createRoot("methodCall");
        packet.createNewTextNode(getMethod(), "/methodCall/methodName");
        packet.addElement("params", "/methodCall");
        packet.addElement("param", "/methodCall/params");
        packet.addElement("value", "/methodCall/params/param");
        packet.addElement("struct", "/methodCall/params/param/value");
        Iterator iter = params.iterator();
        int count = 1;
        while (iter.hasNext()) {
            RpcParam param = (RpcParam) iter.next();
            packet.addElement("member", "/methodCall/params/param/value/struct");
            packet.createNewTextNode(param.getName(), "/methodCall/params/param/value/struct/member[" + count + "]/name");
            packet.addElement("value", "/methodCall/params/param/value/struct/member[" + count + "]");
            packet.createNewTextNode(param.getValue(), "/methodCall/params/param/value/struct/member[" + count + "]/value/" + param.getParamtype());
            count++;
        }
        URL url = new URL(getServer());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        System.out.println("xml = " + packet.toString());
        conn.connect();
        OutputStream os = conn.getOutputStream();
        OutputStreamWriter wout = new OutputStreamWriter(os, "UTF-8");
        BufferedReader sr = new BufferedReader(new StringReader(packet.toString()));
        String line = sr.readLine();
        while (line != null) {
            wout.write(line);
            line = sr.readLine();
        }
        wout.flush();
        os.close();
        InputStream in = conn.getInputStream();
        returnpacket = new XmlDocument(in);
        os.close();
        conn.disconnect();
        String retpackage = returnpacket.getNodeValue("/methodResponse/params/param/value/string/text()");
        if (retpackage == null) {
            retpackage = returnpacket.getNodeValue("/methodResponse/fault/value/struct/member[2]/value/string/text()");
        } else {
        }
        return retpackage;
    }

    /**
     * run the flickr client
     * @param appId
     * @param groupId
     * @param targetDir
     * @param email
     * @param password
     */
    public synchronized void runFlickrRpc() {
        try {
            statusLabel.setText("starting");
            this.complete = false;
            Vector v = null;
            System.out.println("rpcType =" + rpcType);
            if (rpcType.equalsIgnoreCase("search")) {
                System.out.println("running search");
                v = createSearchParams("1", appId, tags);
            } else if (rpcType.equalsIgnoreCase("run")) {
                System.out.println("running run");
                v = createParams("1", appId, groupId, email, password);
            } else {
                return;
            }
            XmlDocumentFragment docFrag = new XmlDocumentFragment(new StringReader(call(v)));
            Element node = (Element) docFrag.getNode("/photos");
            String page = node.getAttribute("page");
            String pages = node.getAttribute("pages");
            NodeList nodes = docFrag.getNodeList("//photo");
            int pagetotal = Integer.parseInt(pages);
            int pagecount = 1;
            while (pagecount <= pagetotal) {
                statusLabel.setText("getting page " + pagecount);
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element photo = (Element) nodes.item(i);
                    String id = photo.getAttribute("id");
                    String secret = photo.getAttribute("secret");
                    String server = photo.getAttribute("server");
                    String title = photo.getAttribute("title");
                    StringBuffer titleBuffer = new StringBuffer(id);
                    try {
                        File file = new File(targetDir + File.separator + titleBuffer.toString().trim() + ".jpg");
                        if (!file.exists()) {
                            OnlineFile.getOnlineImage("http://photos" + server + ".flickr.com/" + id + "_" + secret + "_t.jpg", targetDir, titleBuffer.toString().trim() + ".jpg");
                        }
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                }
                pagecount++;
                if (rpcType.equalsIgnoreCase("search")) {
                    v = createSearchParams("" + pagecount, appId, tags);
                } else if (rpcType.equalsIgnoreCase("run")) {
                    v = createParams("" + pagecount, appId, groupId, email, password);
                }
                docFrag = new XmlDocumentFragment(new StringReader(call(v)));
                node = (Element) docFrag.getNode("/photos");
                page = node.getAttribute("page");
                pages = node.getAttribute("pages");
                nodes = docFrag.getNodeList("//photo");
            }
            complete = true;
            statusLabel.setText("complete");
        } catch (Exception e) {
            System.err.println(e.getMessage());
            complete = true;
            statusLabel.setText("errored");
        }
    }

    /**
 * create the parameters required for the flickr rpc call
 * @param pageno
 * @param appId
 * @param groupId
 * @param email
 * @param password
 * @return
 */
    private static Vector createParams(String pageno, String appId, String groupId, String email, String password) {
        Vector v = new Vector();
        RpcParam param1 = new RpcParam();
        param1.setName("email");
        param1.setValue(email);
        param1.setParamtype("string");
        v.add(param1);
        RpcParam param2 = new RpcParam();
        param2.setName("password");
        param2.setValue(password);
        param2.setParamtype("string");
        v.add(param2);
        RpcParam param3 = new RpcParam();
        param3.setName("api_key");
        param3.setValue(appId);
        param3.setParamtype("string");
        v.add(param3);
        RpcParam param4 = new RpcParam();
        param4.setName("group_id");
        param4.setValue(groupId);
        param4.setParamtype("string");
        v.add(param4);
        RpcParam param5 = new RpcParam();
        param5.setName("page");
        param5.setValue(pageno);
        param5.setParamtype("string");
        v.add(param5);
        return v;
    }

    /**
 * create the parameters required for the flickr rpc search
 * @param pageno
 * @param appId
 * @param tags
 * @return
 */
    private static Vector createSearchParams(String pageno, String appId, String tags) {
        Vector v = new Vector();
        RpcParam param1 = new RpcParam();
        param1.setName("api_key");
        param1.setValue(appId);
        param1.setParamtype("string");
        v.add(param1);
        RpcParam param2 = new RpcParam();
        param2.setName("tags");
        param2.setValue(tags);
        param2.setParamtype("string");
        v.add(param2);
        RpcParam param3 = new RpcParam();
        param3.setName("page");
        param3.setValue(pageno);
        param3.setParamtype("string");
        v.add(param3);
        return v;
    }
}
