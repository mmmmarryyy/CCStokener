package com.softwaresmithy.preferences;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.xpath.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class DownloadLibrariesTask extends AsyncTask<Object, Void, Long> {

    private XPathExpression rootXpath;

    private XPathExpression libraries;

    private XPathExpression state;

    private Set<String> distinctStates = new HashSet<String>();

    private Node rootNode;

    private Context context;

    public DownloadLibrariesTask(Context context) {
        super();
        this.context = context;
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            rootXpath = xpath.compile("/xml");
            libraries = xpath.compile("library");
            state = xpath.compile("state");
        } catch (XPathExpressionException e) {
            Log.e(this.getClass().getName(), "xpath error", e);
        }
    }

    @Override
    protected Long doInBackground(Object... params) {
        HttpClient client = (HttpClient) params[0];
        HttpGet getLibs = new HttpGet((String) params[1]);
        Boolean reload = params.length > 2 ? (Boolean) params[2] : false;
        File storageDir = context.getExternalFilesDir(null);
        File libXml = new File(storageDir, "libraries.xml");
        InputSource inputSource = null;
        try {
            if (libXml.exists() && !reload) {
                inputSource = new InputSource(new FileInputStream(libXml));
            } else {
                HttpResponse resp = client.execute(getLibs);
                InputStream is = resp.getEntity().getContent();
                libXml.delete();
                if (libXml.createNewFile()) {
                    FileOutputStream fos = new FileOutputStream(libXml);
                    try {
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = is.read(buf)) > 0) {
                            fos.write(buf, 0, len);
                        }
                    } finally {
                        fos.close();
                        is.close();
                    }
                    inputSource = new InputSource(new FileInputStream(libXml));
                } else {
                    inputSource = new InputSource(is);
                }
            }
            rootNode = (Node) rootXpath.evaluate(inputSource, XPathConstants.NODE);
            NodeList libraryNodes = (NodeList) libraries.evaluate(rootNode, XPathConstants.NODESET);
            for (int i = 0; i < libraryNodes.getLength(); i++) {
                Node libNode = libraryNodes.item(i);
                String stateText = state.evaluate(libNode);
                distinctStates.add(stateText);
            }
        } catch (Exception e) {
            Log.e(this.getClass().getName(), "xpath error creating preferences", e);
        } finally {
            try {
                inputSource.getByteStream().close();
            } catch (Exception e) {
                Log.e(this.getClass().getName(), "Failed to close InputStream", e);
            }
        }
        return libXml.lastModified();
    }

    public Set<String> getDistinctStates() {
        return distinctStates;
    }

    public Node getRootNode() {
        return rootNode;
    }
}
