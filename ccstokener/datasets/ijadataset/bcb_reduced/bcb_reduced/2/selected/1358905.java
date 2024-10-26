package org.lex.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.util.TextureManager;

/**
 * This class helps loading multiple textures with the same properties.
 * This class loads textures from the directory or a zip file configured
 * at initialization time.
 * 
 * @author lex
 *
 */
public class DirectoryTextureLoader {

    private UrlFactory urlFactory;

    private boolean zip;

    private URL url;

    private TextureLoader textureLoader;

    public DirectoryTextureLoader(URL base, boolean zip) {
        this(base, zip, new TextureLoader());
    }

    public DirectoryTextureLoader(URL base, boolean zip, TextureLoader textureLoader) {
        url = base;
        this.zip = zip;
        urlFactory = new UrlFactory(base, zip);
        this.textureLoader = textureLoader;
    }

    public TextureLoader getTextureLoader() {
        return textureLoader;
    }

    /**
	 * Change the texture loader to fine tune texture properties, such
	 * as filtering and imageType.
	 * 
	 * @param textureLoader the textureLoader to use
	 */
    public void setTextureLoader(TextureLoader textureLoader) {
        this.textureLoader = textureLoader;
    }

    public Texture loadTexture(String file) throws IOException {
        URL imageUrl = urlFactory.makeUrl(file);
        Texture cached = textureLoader.getImageFromCache(imageUrl);
        if (cached != null) return cached;
        Image image;
        if (zip) {
            ZipInputStream zis = new ZipInputStream(url.openStream());
            ZipEntry entry;
            boolean found = false;
            while ((entry = zis.getNextEntry()) != null) {
                if (file.equals(entry.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IOException("Cannot find file \"" + file + "\".");
            }
            int extIndex = file.lastIndexOf('.');
            if (extIndex == -1) {
                throw new IOException("Cannot parse file extension.");
            }
            String fileExt = file.substring(extIndex);
            image = TextureManager.loadImage(fileExt, zis, true);
        } else {
            image = TextureManager.loadImage(imageUrl, true);
        }
        return textureLoader.loadTexture(imageUrl, image);
    }

    public URL makeUrl(String file) throws MalformedURLException {
        return urlFactory.makeUrl(file);
    }
}
