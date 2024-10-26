package ch.blackspirit.graphics.jogl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import com.sun.opengl.util.texture.TextureData;
import com.sun.opengl.util.texture.spi.TextureProvider;

/**
 * TODO add ImageProvider for jogl implementation returning a BufferedImage from file.. -> use apache commons image library
 * Code copied with minimal changes to have it accessible as this originally 
 * was a protected static class: TextureIO.IIOTextureProvider 
 * @author Markus Koller
 */
class ImageIOTextureProvider implements TextureProvider {

    public TextureData newTextureData(File file, int internalFormat, int pixelFormat, boolean mipmap, String fileSuffix) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            return null;
        }
        return new TextureData(internalFormat, pixelFormat, mipmap, img);
    }

    public TextureData newTextureData(InputStream stream, int internalFormat, int pixelFormat, boolean mipmap, String fileSuffix) throws IOException {
        BufferedImage img = ImageIO.read(stream);
        if (img == null) {
            return null;
        }
        return new TextureData(internalFormat, pixelFormat, mipmap, img);
    }

    public TextureData newTextureData(URL url, int internalFormat, int pixelFormat, boolean mipmap, String fileSuffix) throws IOException {
        InputStream stream = url.openStream();
        try {
            return newTextureData(stream, internalFormat, pixelFormat, mipmap, fileSuffix);
        } finally {
            stream.close();
        }
    }
}
