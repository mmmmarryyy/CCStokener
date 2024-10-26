package de.guidoludwig.jtrade.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import de.guidoludwig.jtrade.ErrorMessage;

/**
 * Utility for JTrade specific operations
 * 
 * @author <a href="mailto:jtrade@gigabss.de">Guido Ludwig</a>
 * @version $Revision: 1.10 $
 */
public class JTradeUtil {

    /**
     * Loads an Icon with the given name.
     * The name represents an image source located in the
     * resources/images directory.
     * The name must include the image extension, eg. 'image.gif'
     * <br/>
     * If the image is not found, simply a null Icon is returned !
     * <br/>
     * Possible Exceptions thrown are eaten !
     * If an exception occurs, a null Icon is returned ! 
     * @param name
     * @return the loaded icon or null if the icon could not be loaded
     */
    public static ImageIcon loadIcon(String name) {
        URL url = ClassLoader.getSystemResource("resources/images/" + name);
        if (url == null) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).warning("icon " + name + "not found");
            return null;
        }
        return new ImageIcon(url);
    }

    public static String loadResource(String resource) {
        URL url = ClassLoader.getSystemResource("resources/" + resource);
        StringBuffer buffer = new StringBuffer();
        if (url == null) {
            ErrorMessage.handle(new NullPointerException("URL for resources/" + resource + " not found"));
        } else {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return buffer.toString();
    }

    public static String loadFile(String filename) {
        StringBuffer buffer = new StringBuffer();
        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(filename, "r");
        } catch (FileNotFoundException e) {
            ErrorMessage.handle("File not found", e);
            return null;
        }
        String line = null;
        try {
            while ((line = raf.readLine()) != null) {
                buffer.append(line + "\n");
            }
        } catch (IOException e) {
            ErrorMessage.handle(e);
            return null;
        }
        return buffer.toString();
    }
}
