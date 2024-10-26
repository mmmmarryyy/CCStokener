package src.eleconics;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
import src.multiplayer.xmlParser.XmlParser;

/**
 * This class stores static functions that are used frequently in a variety of places
 */
public class Utilities {

    /**
	 * Returns the matrix that rotates the vectors INITIAL_HEADING 
	 * and INITIAL_UP to heading and up
	 * 
	 * Let A = matrix of (INITIAL_HEADING, INITIAL_UP, initialCross)
	 * Let B = matrix of (heading, up, currentCross)
	 * Looking for matrix C s.t. C*INITIAL_HEADING = heading
	 * and C*INITIAL_UP = up
	 * and C*initialCross = currentCross
	 * in other words C*A = B
	 * Then C = B*(A^-1)
	 */
    public static Matrix3f getRotationUsingHeadingAndUp(Vector3f initialHeading, Vector3f initialUp, Vector3f heading, Vector3f up) {
        Vector3f initialCross = new Vector3f();
        initialCross.cross(initialHeading, initialUp);
        Vector3f currentCross = new Vector3f();
        currentCross.cross(heading, up);
        Matrix3f A = new Matrix3f();
        Matrix3f B = new Matrix3f();
        Matrix3f C = new Matrix3f();
        A.setColumn(0, initialHeading);
        A.setColumn(1, initialUp);
        A.setColumn(2, initialCross);
        B.setColumn(0, heading);
        B.setColumn(1, up);
        B.setColumn(2, currentCross);
        A.invert();
        C.mul(B, A);
        return C;
    }

    /**
     * Get the angle and axis of rotation necessary to align
     * this Orientation's Ship with the specified heading.
     * @param dir The vector with which the Ship should be
     *    aligned.
     * @return The AxisAngle4f that specifies the correct
     *    rotation to align the Orientation with the specified
     *    vector.
     */
    public static AxisAngle4f findRotation(Vector3f initialHeading, Vector3f initialUp, Vector3f finalDir) {
        finalDir.normalize();
        Vector3f axis = new Vector3f();
        float angle = initialHeading.angle(finalDir);
        axis.cross(initialHeading, finalDir);
        if (axis.x == 0 && axis.y == 0 && axis.z == 0) {
            axis.set(new Vector3f(initialUp));
            angle = 0;
        }
        if (angle > Math.PI) {
            angle = (float) -(2 * Math.PI - angle);
        } else if (angle < -(Math.PI)) {
            angle = (float) (angle + 2 * Math.PI);
        }
        return new AxisAngle4f(axis, angle);
    }

    /**
	 * Pop up a message with the specified notification
	 * @param notification The notification to display to
	 *    the user.
	 */
    public static void popUp(String notification) {
        javax.swing.JOptionPane.showMessageDialog(null, notification, "Eleconics Notification", javax.swing.JOptionPane.ERROR_MESSAGE);
    }

    /**
	 * Pop up a message with the specified notification
	 * @param notification The notification to display to
	 *    the user.
	 */
    public static void notify(String notification) {
        javax.swing.JOptionPane.showMessageDialog(null, notification, "Eleconics Notification", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    /**
	 * Get the external ip address for this client.
	 * @return The external IP address for this client.
	 */
    public static String getExternalIP() {
        String externalIP = null;
        try {
            XmlParser xmlParser;
            URL url;
            HttpURLConnection conn;
            String currentIP;
            url = new URL("http://checkip.dyndns.org/");
            conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            xmlParser = new XmlParser(new InputStreamReader(conn.getInputStream(), "8859_1"));
            xmlParser.openNode("html");
            xmlParser.openNode("head");
            xmlParser.openNode("title");
            xmlParser.match("Current IP Check");
            xmlParser.closeNode("title");
            xmlParser.closeNode("head");
            xmlParser.openNode("body");
            currentIP = xmlParser.readString();
            xmlParser.closeNode("body");
            xmlParser.closeNode("html");
            xmlParser.close();
            externalIP = (currentIP.split(": "))[1];
        } catch (Exception e) {
            StringBuffer error = new StringBuffer("Error obtaining external ip:");
            error.append(e.getMessage());
            Utilities.popUp(error.toString());
            e.printStackTrace();
        }
        return externalIP;
    }

    /**
	 * Generates a random number between min (inclusive) and 
	 * max (exclusive) to pick a sound file to play.
	 * @param min The minimum (inclusive) of the range of random ints.
	 * @param max The maximum (exclusive) of the range of random ints.
	 * @return The random integer between min and max.
	 */
    public static int getRandom(int min, int max) {
        if (min == max) {
            return min;
        }
        Random generator = new Random();
        return generator.nextInt(max - min) + min;
    }
}
