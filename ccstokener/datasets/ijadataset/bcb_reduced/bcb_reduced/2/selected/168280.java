package org.gjt.btools.license;

import java.io.*;
import java.net.URL;
import javax.swing.JTextArea;

/**
 * Provides access to the full descriptions of various licenses.
 */
public class License {

    /**
     * The GNU General Public License.
     */
    public static final int GPL = 1;

    /**
     * The GNU Lesser General Public License.
     */
    public static final int LGPL = 2;

    /**
     * The BSD License.
     */
    public static final int BSD = 3;

    /**
     * The Artistic License.
     */
    public static final int Artistic = 4;

    /**
     * The location of the GNU General Public License.
     */
    private static final String locGPL = "org/gjt/btools/license/gpl.txt";

    /**
     * The location of the GNU Lesser General Public License.
     */
    private static final String locLGPL = "org/gjt/btools/license/lgpl.txt";

    /**
     * The location of the BSD License.
     */
    private static final String locBSD = "org/gjt/btools/license/bsd.txt";

    /**
     * The location of the Artistic License.
     */
    private static final String locArtistic = "org/gjt/btools/license/artistic.txt";

    /**
     * Returns an URL to a resource that contains a full
     * description of the given license.  The resource will be in the
     * form of a plain text file.
     *
     * @param license specifies which license to locate; this must
     * be one of the static license constants defined in this class.
     * @return an URL to the corresponding full description, or
     * <tt>null</tt> if an invalid license was specified.
     */
    public static URL getResource(int license) {
        String location = null;
        switch(license) {
            case GPL:
                location = locGPL;
                break;
            case LGPL:
                location = locLGPL;
                break;
            case BSD:
                location = locBSD;
                break;
            case Artistic:
                location = locArtistic;
                break;
        }
        return License.class.getClassLoader().getResource(location);
    }

    /**
     * Returns an open reader to the full description of the given license.
     * The description will be in the form of a plain text file.
     *
     * @param license specifies which license to read; this must
     * be one of the static license constants defined in this class.
     * @return an open reader to the corresponding full description, or
     * <tt>null</tt> if an invalid license was specified or the
     * description could not be read.
     */
    public static BufferedReader getReader(int license) {
        URL url = getResource(license);
        if (url == null) return null;
        InputStream inStream;
        try {
            inStream = url.openStream();
        } catch (IOException e) {
            return null;
        }
        return new BufferedReader(new InputStreamReader(inStream));
    }

    /**
     * Returns an uneditable text area displaying the full description
     * of the given license.
     *
     * @param license specifies which license to display; this must
     * be one of the static license constants defined in this class.
     * @return a component displaying the corresponding full description,
     * or <tt>null</tt> if an invalid license was specified or the
     * description could not be read.
     */
    public static JTextArea getViewer(int license) {
        BufferedReader reader = getReader(license);
        if (reader == null) return null;
        JTextArea pane = new JTextArea();
        try {
            String line = reader.readLine();
            while (line != null) {
                pane.append(line + '\n');
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            try {
                reader.close();
            } catch (Throwable th) {
            }
            return null;
        }
        pane.setCaretPosition(0);
        pane.setEditable(false);
        return pane;
    }
}
