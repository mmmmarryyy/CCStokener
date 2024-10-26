import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.awt.image.*;
import javax.imageio.*;
import java.beans.*;
import javax.swing.table.*;
import com.sun.image.codec.jpeg.*;
import java.net.*;
import javax.imageio.ImageIO;
import java.awt.geom.*;
import java.awt.datatransfer.*;

/** Class defining a library for performing other common Java operations necessary for GUI operations */
public class LibGUI {

    /**
    * Method to load a buffered image
    * @param location     The location of the image
    * @param dimension    The dimension of the image
    * @return             The buffered image
    */
    public static BufferedImage loadImage(String location, Dimension dimension) {
        try {
            Image image = Toolkit.getDefaultToolkit().createImage(location);
            MediaTracker mediaTracker = new MediaTracker(new Container());
            mediaTracker.addImage(image, 0);
            mediaTracker.waitForID(0);
            if (dimension == null) dimension = new Dimension(image.getWidth(null), image.getHeight(null));
            BufferedImage returnImage = new BufferedImage(dimension.width, dimension.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics2D = returnImage.createGraphics();
            graphics2D.drawImage(image, 0, 0, dimension.width, dimension.height, null);
            return returnImage;
        } catch (Exception e) {
            return null;
        }
    }

    /**
    * Method to load a buffered image by location reference
    * @param location      The location of the image
    * @return              The buffered image
    */
    public static BufferedImage loadImage(String location) {
        return loadImage(location, null);
    }

    /**
    * Method to define a URL for given data
    * @param urlString     The URL to create
    * @param getData       The necessary data to get from the URL
    * @param postData      The data to output
    * @return              The URL created
    */
    public static String getURL(String urlString, String getData, String postData) {
        try {
            if (getData != null) if (!getData.equals("")) urlString += "?" + getData;
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            if (!postData.equals("")) {
                connection.setDoOutput(true);
                PrintWriter out = new PrintWriter(connection.getOutputStream());
                out.print(postData);
                out.close();
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            int inputLine;
            String output = "";
            while ((inputLine = in.read()) != -1) output += (char) inputLine;
            in.close();
            return output;
        } catch (Exception e) {
            return null;
        }
    }

    /**
    * Method to define a URL by given string reference
    * @param urlString     The URL to create
    * @return              The URL created
    */
    public static String getURL(String urlString) {
        return getURL(urlString, "", "");
    }

    /**
    * Method to get the current time reference
    * @return              The current time reference
    */
    public static long getTime() {
        Date date = new Date();
        return date.getTime();
    }

    /**
    * Method that saves the state of an object to a file using an output stream.
    * @param file The file to be created.
    * @param object The serializable object to be saved.
    */
    public static void saveStateToFile(File file, Serializable object) {
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
            fos = new FileOutputStream(file);
            out = new ObjectOutputStream(fos);
            out.writeObject(object);
            out.close();
        } catch (IOException ex) {
        }
    }

    /**
    * Method that retrieves the state of an object to a file using an input stream.
    * @param file The file to opened.
    * @return The serializable object restored.
    */
    public static Serializable openStateFromFile(File file) {
        Serializable obj = null;
        FileInputStream fis = null;
        ObjectInputStream in = null;
        try {
            fis = new FileInputStream(file);
            in = new ObjectInputStream(fis);
            obj = (Serializable) in.readObject();
            in.close();
        } catch (IOException ex) {
        } catch (ClassNotFoundException ex) {
        }
        return obj;
    }

    /** If an image is on the system clipboard, this method returns it;
      * otherwise it returns null. */
    public static BufferedImage paste() {
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        try {
            if (t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                BufferedImage text = (BufferedImage) t.getTransferData(DataFlavor.imageFlavor);
                BufferedImage toReturn = new BufferedImage(text.getWidth(), text.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = toReturn.createGraphics();
                g.drawImage(text, 0, 0, null);
                return toReturn;
            }
        } catch (UnsupportedFlavorException e) {
        } catch (IOException e) {
        }
        return null;
    }

    /** This method writes a image to the system clipboard. */
    public static void copy(BufferedImage image) {
        TransferableImage tImage = new TransferableImage(image);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(tImage, null);
    }
}
