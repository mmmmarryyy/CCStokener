package com.tirix.anim8or;

import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import com.sun.j3d.loaders.LoaderBase;
import com.tirix.anim8or.Anim8orLoader.ResourceLoader;

/**
 * A facility for loading resources.
 * This default ResourceLoader implementation simply tries to access the resources as files. 
 * This is sufficient in most cases. 
 * More advanced implementations may include:
 * - looking for images/textures in a special directory, 
 * - getting the resources from a jar or a ZIP file, 
 * - getting the resources from the Applet resources,
 * - etc. 
 *
 * @author Thierry Arnoux
 *
 * @version $Id: DefaultResourceLoader.java,v 1.1 2008/02/02 23:54:25 tirix Exp $</p>
 *
 * <b>Change Log:</b><pre>
 * $Log: DefaultResourceLoader.java,v $
 * Revision 1.1  2008/02/02 23:54:25  tirix
 * Added a 'ResourceLoader' interface: enables to customize eg. texture loading.
 *
 *
 * </pre>
 * <i>Copyright (C) 2008 Thierry Arnoux.</i></p>
 */
public class DefaultResourceLoader implements ResourceLoader {

    LoaderBase loaderBase;

    public DefaultResourceLoader(LoaderBase loaderBase) {
        this.loaderBase = loaderBase;
    }

    public File getFile(String resourceName) throws IOException {
        File f = new File(loaderBase.getBasePath(), resourceName);
        if (!f.exists()) throw new IOException("File does not exist : " + f);
        return f;
    }

    public InputStream openResource(String resourceName) throws IOException {
        return new FileInputStream(getFile(resourceName));
    }

    public Image openImage(String resourceName) throws IOException {
        return ImageIO.read(getFile(resourceName));
    }

    public InputStream openResource(URL url) throws IOException {
        return url.openStream();
    }
}
