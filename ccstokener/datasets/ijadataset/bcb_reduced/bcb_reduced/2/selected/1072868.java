package MyCommon;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import sun.misc.Launcher;

/** This class has a variety of useful functions.
 *
 * @author Brandon Drake
 */
public class Utilities {

    /** Converts each byte to a hex string in the array and concatenates all values.
     */
    public static String convertToHex(byte[] l_value) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < l_value.length; i++) {
            String appendText = Integer.toHexString(0xFF & l_value[i]);
            if (appendText.length() < 2) {
                appendText = '0' + appendText;
            }
            hexString.append(appendText);
        }
        return hexString.toString();
    }

    /** Converts every two character hex code to a byte in a byte array.  The result is undefined or may throw an error when the string is not hex.
     */
    public static byte[] convertFromHex(String l_value) {
        int len = l_value.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(l_value.charAt(i), 16) << 4) + Character.digit(l_value.charAt(i + 1), 16));
        }
        return data;
    }

    /** Gets the parent frame of the component.  If the frame cannot be found, this returns null.
     */
    public static Frame getFrame(Component c) {
        if (null == c) {
            return null;
        } else if (c instanceof Frame) {
            return (Frame) c;
        }
        return getFrame(c.getParent());
    }

    /** Gets an array of class names that should be pre-loaded from the package.  Basically this returns all classes listed in a "PreLoadClasses.txt" file.
     */
    public static ArrayList<String> getPackagePreLoadClasses(java.lang.Package l_package) {
        DataInputStream dataIn = null;
        ArrayList<String> result = new ArrayList<String>();
        try {
            String pckgname = l_package.getName();
            String name = new String(pckgname);
            if (!name.startsWith("/")) {
                name = "/" + name;
            }
            name = name.replace('.', '/');
            name = name + "/PreLoadClasses.txt";
            URL url = Launcher.class.getResource(name);
            URLConnection conn = url.openConnection();
            dataIn = new DataInputStream(conn.getInputStream());
            String readLine = dataIn.readLine();
            while (null != readLine) {
                if (false == readLine.startsWith("#") && false == readLine.equals("")) {
                    result.add(readLine);
                }
                readLine = dataIn.readLine();
            }
        } catch (Exception ex) {
            return null;
        } finally {
            try {
                dataIn.close();
            } catch (IOException ex) {
            }
        }
        return result;
    }

    /** This joins all elements in the collection into one string, with the delimiter character in between.
     */
    public static String join(AbstractCollection s, String delimiter) {
        if (s.isEmpty()) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        Iterator iter = s.iterator();
        Object nextValue = iter.next();
        if (null != nextValue) {
            buffer.append(nextValue);
        }
        while (iter.hasNext()) {
            buffer.append(delimiter);
            nextValue = iter.next();
            if (null != nextValue) {
                buffer.append(nextValue);
            }
        }
        return buffer.toString();
    }

    /** This joins all elements in the array into one string, with the delimiter character in between.
     */
    public static String join(Object[] s, String delimiter) {
        if (0 == s.length) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        if (null != s[0]) {
            buffer.append(s[0]);
        }
        int currentIndex = 1;
        while (currentIndex < s.length) {
            buffer.append(delimiter);
            if (null != s[currentIndex]) {
                buffer.append(s[currentIndex]);
            }
            currentIndex++;
        }
        return buffer.toString();
    }

    /** Loads all classes in this package.
     */
    public static void loadPackageClasses(java.lang.Package l_package) {
        ArrayList<String> classesToLoad = getPackagePreLoadClasses(l_package);
        for (String className : classesToLoad) {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException cnfex) {
            }
        }
    }

    /** Opens the url at the specified location.
     */
    public static void openURL(String url) {
        String osName = System.getProperty("os.name");
        try {
            if (osName.startsWith("Mac OS")) {
                Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
                java.lang.reflect.Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });
                openURL.invoke(null, new Object[] { url });
            } else if (osName.startsWith("Windows")) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error attempting to launch web browser\n" + e.toString());
        }
    }

    /** Reads all data from an input stream and gets it as a string.
     */
    public static String readEntireStream(InputStream l_inStream) throws IOException {
        java.lang.String result = "";
        try {
            java.io.InputStreamReader inStream = new java.io.InputStreamReader(l_inStream);
            java.nio.CharBuffer inData;
            inData = java.nio.CharBuffer.allocate(1000);
            int charsRead = inStream.read(inData);
            while (charsRead != -1) {
                inData.position(0);
                result += inData.subSequence(0, charsRead);
                if (inData.capacity() < 10000) {
                    inData = java.nio.CharBuffer.allocate(inData.capacity() * 2);
                }
                charsRead = inStream.read(inData);
            }
        } catch (java.io.IOException ex) {
            throw ex;
        }
        return result;
    }

    /** Sets the control and its children to the enabled status specified.
     */
    public static void setControlsEnabled(java.awt.Component l_component, boolean l_enabled) {
        setControlsEnabled(new java.awt.Component[] { l_component }, l_enabled);
    }

    /** Sets all of the controls and their children to the enabled status specified.
     */
    public static void setControlsEnabled(java.awt.Component[] l_components, boolean l_enabled) {
        for (java.awt.Component comp : l_components) {
            comp.setEnabled(l_enabled);
            if (comp instanceof javax.swing.JComponent) {
                JComponent jComp = (javax.swing.JComponent) comp;
                if (0 < jComp.getComponentCount()) {
                    setControlsEnabled(jComp.getComponents(), l_enabled);
                }
            }
        }
    }

    /** This function can be used like JOptionPane.showConfirmMessage() except that it can display very long messages.
     */
    public static int showLargeConfirmMessage(java.awt.Component l_parent, String l_message, String l_title, int l_options) {
        JTextArea textArea = new JTextArea(l_message);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setFont(textArea.getFont().deriveFont((float) 11.0));
        textArea.setMargin(new java.awt.Insets(5, 5, 5, 5));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(510, 100));
        return JOptionPane.showConfirmDialog(l_parent, scrollPane, l_title, l_options);
    }
}
