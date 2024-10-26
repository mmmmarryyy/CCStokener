package mipt.gui;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import mipt.common.Utils;
import mipt.reflect.MethodCall;
import mipt.reflect.MethodCaller;

/**
 * Creates images from disk or from jar at the classpath. File naming rules are the same as in FileImageIconFactory
 * Works slower than FileImageIconFactory (uses URLs from ClassLoader.getSystemResource())
 * @author Evdokimov
 */
public class SystemImageIconFactory extends ImageIconFactory {

    /**
	 * SystemImageIconFactory constructor for GIF images
	 * @param pathToIcons java.lang.String
	 */
    public SystemImageIconFactory(String pathToIcons) {
        super(pathToIcons);
    }

    /**
	 * pathToIcons - relative path starting with subdirectory name. Use "/" to separate directories
	 * @param pathToIcons java.lang.String - can be null to load images from the same dir as a program
	 * @param extension java.lang.String
	 */
    public SystemImageIconFactory(String pathToIcons, String extension) {
        super(pathToIcons, extension);
    }

    /**
	 * Creates icon with the given name
	 * @return javax.swing.Icon
	 * @param iconName java.lang.String
	 */
    protected Icon newIcon(String iconName) {
        URL url = Utils.getResource(getFullPath(iconName, "/"), getClass());
        if (url == null) {
            if (getParent() != null) return getParent().getIcon(iconName);
            return null;
        }
        try {
            MethodCall getImage = new MethodCaller("org.apache.sanselan.Sanselan", null, "getBufferedImage", new Object[] { InputStream.class }).getMethodCall();
            getImage.setArgumentValue(0, url.openStream());
            return new ImageIcon((BufferedImage) getImage.call());
        } catch (Throwable e) {
            return new ImageIcon(url);
        }
    }
}
