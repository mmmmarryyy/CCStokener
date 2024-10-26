package de.mse.mogwai.impl.swing;

import java.net.URL;
import java.util.HashMap;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import de.mse.mogwai.MOGWAI.*;
import javax.swing.*;
import org.omg.CORBA.ORB;

/**
 * Implementation for the MogwaiRoot.
 *
 * @author  Mirko Sertic
 */
public class MogwaiRootImplementation implements MogwaiRootOperations {

    private ORB m_orb;

    private static MogwaiResourceLoader resourceloader;

    private static HashMap resourcecache = new HashMap();

    public MogwaiRootImplementation(ORB orb) {
        this.m_orb = orb;
    }

    public MogwaiFrame createFrame(String name) {
        return MogwaiComponentFactoryImplementation.getInstance().createFrame(name, this.m_orb);
    }

    public MogwaiDialog createDialog(String name) {
        return MogwaiComponentFactoryImplementation.getInstance().createDialog(name, m_orb, null);
    }

    public void shutdown() {
        this.m_orb.shutdown(false);
    }

    public void notifyUserAboutError(String message) {
        JOptionPane.showMessageDialog(null, message, "Uncaught Exception", JOptionPane.ERROR_MESSAGE);
    }

    public void setResourceLoader(MogwaiResourceLoader loader) {
        resourceloader = loader;
    }

    private static byte[] loadLocalResource(String name) {
        URL url = MogwaiRootImplementation.class.getClassLoader().getResource("resources/" + name);
        if (url != null) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                BufferedInputStream bis = new BufferedInputStream(url.openStream());
                byte[] data = new byte[5000];
                while (bis.available() > 0) {
                    int anz = bis.read(data);
                    if (anz > 0) bos.write(data, 0, anz);
                }
                bis.close();
                return bos.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static byte[] loadResourceAsByteArray(String name) {
        if (!resourcecache.containsKey(name)) {
            byte[] data = loadLocalResource(name);
            if (data == null) data = resourceloader.loadResource(name);
            resourcecache.put(name, data);
            return data;
        } else {
            return (byte[]) resourcecache.get(name);
        }
    }

    public static Icon loadResourceAsIcon(String name) {
        if (!resourcecache.containsKey(name)) {
            byte[] data = loadLocalResource(name);
            if (data == null) data = resourceloader.loadResource(name);
            resourcecache.put(name, data);
            return new ImageIcon(data);
        } else {
            return new ImageIcon((byte[]) resourcecache.get(name));
        }
    }
}
