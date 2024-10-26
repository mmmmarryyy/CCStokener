package org.one.stone.soup.wiki.processor;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Hashtable;
import org.one.stone.soup.authentication.server.Login;
import org.one.stone.soup.browser.Browser;
import org.one.stone.soup.file.FileHelper;
import org.one.stone.soup.wiki.WikiStream;
import org.one.stone.soup.wiki.file.manager.FileManagerInterface;
import org.one.stone.soup.xml.XmlElement;
import org.one.stone.soup.xml.rpc.XmlRpcCall;
import org.one.stone.soup.xml.rpc.XmlRpcResponse;
import org.one.stone.soup.xml.stream.XmlLoader;

public class ExternalResourceHelper {

    private FileManagerInterface fileManager;

    private Login login;

    private class ExternalStreamHandler implements WikiStream {

        private InputStream iStream;

        public ExternalStreamHandler(InputStream iStream) {
            this.iStream = iStream;
        }

        public long saveTo(OutputStream outStream) {
            return FileHelper.copy(iStream, outStream, null, -1);
        }
    }

    public ExternalResourceHelper(FileManagerInterface fileManager, Login login) {
        Browser.initialize(false, "OpenForum Wiki");
        this.fileManager = fileManager;
        this.login = login;
    }

    public String getData(String url) throws Exception {
        URLConnection connection = getConnection(url);
        String data = new String(FileHelper.readFile(connection.getInputStream()));
        return data;
    }

    public XmlRpcCall createXmlRpcCall(String methodName) {
        return new XmlRpcCall(methodName);
    }

    public XmlRpcResponse processXmlRpc(String url, XmlRpcCall call) throws Exception {
        InputStream iStream = Browser.putData(url, call.toString());
        XmlElement xml = XmlLoader.load(iStream);
        XmlRpcResponse response = new XmlRpcResponse(xml);
        return response;
    }

    public String getData(String url, String method, Hashtable<String, String> properties) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) getConnection(url);
        connection.setRequestMethod(method);
        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            connection.setRequestProperty(key, (String) properties.get(key));
        }
        String data = new String(FileHelper.readFile(connection.getInputStream()));
        return data;
    }

    public String postData(String url, Hashtable<String, String> data) throws Exception {
        InputStream iStream = Browser.postForm(url, data);
        return new String(FileHelper.readFile(iStream));
    }

    public String postFile(String url, String pageName, String fileName) throws Exception {
        InputStream fileStream = fileManager.getAttachmentInputStream(pageName, fileName, login);
        InputStream iStream = Browser.postFile(url, fileStream, fileName);
        return new String(FileHelper.readFile(iStream));
    }

    public String putFile(String url, String pageName, String fileName) throws Exception {
        InputStream fileStream = fileManager.getAttachmentInputStream(pageName, fileName, login);
        InputStream iStream = Browser.putFile(url, fileStream);
        return new String(FileHelper.readFile(iStream));
    }

    public String putData(String url, String data) throws Exception {
        InputStream iStream = Browser.putData(url, data);
        return new String(FileHelper.readFile(iStream));
    }

    public void copyFile(String url, String pageName, String fileName) throws Exception {
        URLConnection connection = getConnection(url);
        InputStream iStream = connection.getInputStream();
        ExternalStreamHandler handler = new ExternalStreamHandler(iStream);
        fileManager.saveWikiStreamAsAttachment(handler, pageName, fileName, login);
        iStream.close();
    }

    public URLConnection getConnection(String urlData) throws Exception {
        URL url = new URL(urlData);
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("User-Agent", "OpenForum Wiki");
        return connection;
    }
}
