package octlight.image;

import octlight.image.loader.ImageIOImageLoader;
import octlight.image.loader.ImageLoader;
import octlight.image.loader.TargaImageLoader;
import octlight.util.FileUtil;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

/**
 * @author $Author: creator $
 * @version $Revision: 1.2 $
 */
public class ImageManager {

    private static HashMap<String, ImageLoader> loaders = new HashMap<String, ImageLoader>();

    private ImageManager() {
    }

    static {
        loaders.put("tga", TargaImageLoader.INSTANCE);
        loaders.put("jpg", ImageIOImageLoader.INSTANCE);
        loaders.put("gif", ImageIOImageLoader.INSTANCE);
        loaders.put("png", ImageIOImageLoader.INSTANCE);
    }

    private static ImageLoader getLoader(String name) throws IOException {
        ImageLoader loader = loaders.get(FileUtil.getExtension(name));
        if (loader == null) throw new IOException("Can't find loader for '" + name + "'!");
        return loader;
    }

    public static Image loadImage(File file) throws IOException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            return getLoader(file.getName()).loadImage(in);
        } finally {
            in.close();
        }
    }

    public static Image loadImage(URL url) throws IOException {
        BufferedInputStream in = new BufferedInputStream(url.openStream());
        try {
            return getLoader(url.getFile()).loadImage(in);
        } finally {
            in.close();
        }
    }
}
