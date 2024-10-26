package org.posper.graphics;

import java.net.URL;
import java.util.List;
import java.util.LinkedList;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class GraphicsResources {

    private static GraphicsResources m_instance = null;

    private List<URL> m_resources;

    protected GraphicsResources() {
        m_resources = new LinkedList<URL>();
    }

    public static GraphicsResources getInstance() {
        if (m_instance == null) {
            m_instance = new GraphicsResources();
        }
        return m_instance;
    }

    public void addURL(String resourceLocation) {
        try {
            addURL(new URL(resourceLocation));
        } catch (MalformedURLException e) {
        }
    }

    public void addURL(URL resourceLocation) {
        if (!m_resources.contains(resourceLocation)) {
            m_resources.add(resourceLocation);
        }
    }

    public List getAvailableKeys() {
        List<String> keys = new LinkedList<String>();
        for (URL res : m_resources) {
            keys.add(res.getPath());
        }
        return keys;
    }

    public URL getURL(String sKey) {
        if (sKey == null) {
            return null;
        } else {
            for (URL r : m_resources) {
                if (r.getPath().equals(sKey)) {
                    return r;
                }
            }
        }
        return null;
    }

    private static byte[] readStream(InputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        byte[] resource = new byte[0];
        int n;
        while ((n = in.read(buffer)) != -1) {
            byte[] b = new byte[resource.length + n];
            System.arraycopy(resource, 0, b, 0, resource.length);
            System.arraycopy(buffer, 0, b, resource.length, n);
            resource = b;
        }
        return resource;
    }

    public static BufferedImage readImage(byte[] b) {
        if (b == null) {
            return null;
        } else {
            try {
                return ImageIO.read(new ByteArrayInputStream(b));
            } catch (IOException e) {
                return null;
            }
        }
    }

    public BufferedImage getBufferedImage(String sKey) throws IOException {
        URL url = getURL(sKey);
        if (url == null) {
            return null;
        }
        InputStream in = null;
        URLConnection urlConnection = url.openConnection();
        in = urlConnection.getInputStream();
        return readImage(readStream(in));
    }

    public byte[] writeImage(BufferedImage img) {
        if (img == null) {
            return null;
        } else {
            try {
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                ImageIO.write(img, "png", b);
                b.flush();
                b.close();
                return b.toByteArray();
            } catch (IOException e) {
                return null;
            }
        }
    }
}
