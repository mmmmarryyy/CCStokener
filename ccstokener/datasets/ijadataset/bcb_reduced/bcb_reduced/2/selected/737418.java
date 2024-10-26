package org.xith3d.loaders.texture;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Locates a texture from an URL.
 * 
 * @author Matthias Mann
 */
public class TextureStreamLocatorURL implements TextureStreamLocator {

    protected URL baseUrl;

    public String getBaseDirName() {
        return (new File(baseUrl.getFile()).getAbsolutePath());
    }

    /** Creates a new instance of FileTextureStreamLocator */
    public TextureStreamLocatorURL(URL baseUrl) {
        this.baseUrl = baseUrl;
    }

    public URL getBaseUrl() {
        return (baseUrl);
    }

    public void setBaseUrl(URL baseUrl) {
        this.baseUrl = baseUrl;
    }

    public InputStream openTextureStream(String name) {
        try {
            URL url = new URL(baseUrl, name);
            return (url.openStream());
        } catch (IOException ex) {
            return (null);
        }
    }
}
