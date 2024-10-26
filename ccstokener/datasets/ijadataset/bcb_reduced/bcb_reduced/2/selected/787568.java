package net.sf.elm_ve.cn;

import java.io.*;
import java.net.URL;
import java.util.*;

public class CNUtil {

    /**
     * sun.misc.Service.providers()と同じ働きをするメソッド。
     * ジェネリクスにも対応させた。
     */
    public static <T> Iterator<T> providers(Class<T> c) {
        ArrayList<T> objects = new ArrayList<T>();
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        String cn = c.getName();
        Enumeration<URL> urls = null;
        try {
            urls = cl.getResources("META-INF/services/" + cn);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (urls == null) return objects.iterator();
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            try {
                InputStream is = url.openStream();
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader br = new BufferedReader(isr);
                String className = null;
                while ((className = br.readLine()) != null) {
                    if (className.contains("#")) {
                        className = className.substring(0, className.indexOf('#'));
                    }
                    className = className.trim();
                    if (className.equals("")) continue;
                    try {
                        Class<?> theClass = cl.loadClass(className);
                        Class<? extends T> tClass = theClass.asSubclass(c);
                        objects.add(tClass.newInstance());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return objects.iterator();
    }

    /**
     * Serializableをシリアライズします。失敗した時はnullを返します。
     */
    public static byte[] serialize(Serializable s) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(s);
            oos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * バイト列をデシリアライズします。
     * デシリアイズに失敗した時にはnullを返します。
     */
    public static Object deserialize(byte data[]) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * バイト列をデシリアライズします。
     * デシリアイズに失敗した時にはnullを返します。
     */
    public static Object deserialize(byte data[], int offset, int length) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data, offset, length);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getProtocolFromID(String id) {
        return id.substring(0, id.indexOf(':'));
    }

    public static String getInternalIDFromID(String id) {
        return id.substring(id.indexOf(':') + 1);
    }
}
