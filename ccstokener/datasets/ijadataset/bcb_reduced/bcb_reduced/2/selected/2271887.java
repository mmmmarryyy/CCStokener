package com.lts.application;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import com.lts.io.IOUtilities;

/**
 * A ClassLoader that uses an alternative approach to loading resources.
 * 
 * <P>
 * This class basically only cares about the {@link java.lang.ClassLoader#getResource(java.lang.String)}
 * method --- all other methods use the superclass version.
 * 
 * <P>
 * This class tries using the superclass version of 
 * {@link java.lang.ClassLoader#getResource(java.lang.String)} to get resources,
 * but, if that fails, it tries using the class loader that loaded this class to 
 * load the resource.  This is useful in situations where certain system classes
 * return null for {@link java.lang.Class#getClassLoader()}.  
 * 
 * <P>
 * In some situations, system classes will report that they have a null class 
 * loader.  This could be because they are loaded in a debugging environment,
 * but it is difficult to ascertain the exact reason for this.
 * 
 * @author cnh
 */
public class ResourceClassLoader extends ClassLoader {

    public ResourceClassLoader(ClassLoader parent) {
        super(parent);
    }

    public URL getResource(String name) {
        URL url = super.getResource(name);
        if (null == url) url = getClass().getResource(name);
        return url;
    }

    protected void readURL(URL url) {
        InputStream istream = null;
        InputStreamReader isr = null;
        BufferedReader in = null;
        try {
            istream = url.openStream();
            isr = new InputStreamReader(istream);
            in = new BufferedReader(isr);
            String line = in.readLine();
            while (null != line) {
                System.out.println(line);
                line = in.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtilities.close(in);
            IOUtilities.close(isr);
            IOUtilities.close(istream);
        }
    }
}
