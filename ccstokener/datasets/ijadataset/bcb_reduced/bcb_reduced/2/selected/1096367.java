package com.reactiveplot.library.scriptloader;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Opens an InputStream from a URL.
 * 
 * On the first request to get the InputStream, this reads in the entire contents of the URL and saves it to memory.
 * It uses inefficient implementation methods and during reading will require twice the number of bytes as the size of the remote object.
 * This extra memory is freed immediately.
 * 
 * The in-memory URL contents will only be freed when the CachingURLGetInputStream object
 * is destroyed. 
 * 
 * Subsequent calls to get the InputStream will not cause network traffic.
 * @see URLGetInputStream  
 */
public class CachingURLGetInputStream implements ScriptLoader, Serializable {

    private static final long serialVersionUID = 5822364498803811261L;

    URL scriptURL;

    byte byteBuffer[];

    ByteArrayInputStream storage = null;

    @Override
    public InputStream getXMLInputStream() {
        if (storage != null) {
            storage.reset();
            return storage;
        }
        try {
            URLConnection urlConn = scriptURL.openConnection();
            InputStream in = new BufferedInputStream(urlConn.getInputStream());
            ArrayList<Byte> buffer = new ArrayList<Byte>();
            int byteRead;
            while ((byteRead = in.read()) != -1) buffer.add((byte) byteRead);
            in.close();
            byteBuffer = new byte[buffer.size()];
            for (int i = 0; i < buffer.size(); i++) byteBuffer[i] = buffer.get(i);
            storage = new ByteArrayInputStream(byteBuffer);
            return storage;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public CachingURLGetInputStream(URL url) {
        scriptURL = url;
    }
}
