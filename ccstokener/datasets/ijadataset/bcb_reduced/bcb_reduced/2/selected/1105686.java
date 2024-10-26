package edu.upmc.opi.caBIG.caTIES.client.vr.utils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import org.apache.log4j.Logger;
import edu.upmc.opi.caBIG.caTIES.client.vr.desktop.CaTIES_Desktop;
import edu.upmc.opi.caBIG.caTIES.common.CaTIES_Constants;
import edu.upmc.opi.caBIG.caTIES.database.domain.Organization;
import edu.upmc.opi.caBIG.caTIES.middletier.CaTIES_MiddleTierImpl;
import edu.upmc.opi.caBIG.caTIES.middletier.CaTIES_OrderSetImpl;
import edu.upmc.opi.caBIG.caTIES.middletier.CaTIES_UserImpl;
import edu.upmc.opi.caBIG.common.ProxyURLFactory;

public class GeneralUtilities {

    private static Logger logger = Logger.getLogger(GeneralUtilities.class);

    private static final String[] browsers = { "google-chrome", "firefox", "opera", "epiphany", "konqueror", "conkeror", "midori", "kazehakase", "mozilla" };

    private static final String WIN_ID = "Windows";

    private static final String WIN_PATH = "rundll32";

    private static final String WIN_FLAG = "url.dll,FileProtocolHandler";

    private static final String UNIX_PATH = "netscape";

    private static final String UNIX_FLAG = "-remote openURL";

    /**
	 * Change the contents of text file in its entirety, overwriting any
	 * existing text.
	 * 
	 * This style of implementation throws all exceptions to the caller.
	 * 
	 * @param aContents
	 *            String
	 * @param aFile
	 *            is an existing file which can be written to.
	 * 
	 * @throws FileNotFoundException
	 *             if the file does not exist.
	 * @throws IOException
	 *             if problem encountered during write.
	 * @throws IllegalArgumentException
	 *             if param does not comply.
	 */
    public static void setContents(File aFile, String aContents) throws FileNotFoundException, IOException {
        if (aFile == null) {
            throw new IllegalArgumentException("File should not be null.");
        }
        if (!aFile.exists()) {
            throw new FileNotFoundException("File does not exist: " + aFile);
        }
        if (!aFile.isFile()) {
            throw new IllegalArgumentException("Should not be a directory: " + aFile);
        }
        if (!aFile.canWrite()) {
            throw new IllegalArgumentException("File cannot be written: " + aFile);
        }
        Writer output = null;
        try {
            output = new BufferedWriter(new FileWriter(aFile));
            output.write(aContents);
        } finally {
            if (output != null) output.close();
        }
    }

    /**
	 * Method prepareRenderer.
	 * 
	 * @param renderer
	 *            TableCellRenderer
	 * @param vColIndex
	 *            int
	 * @param rowIndex
	 *            int
	 * 
	 * @return Component
	 */
    public static Component prepareRenderer(JTable table, JComponent c, int rowIndex, int vColIndex) {
        if (c instanceof JLabel) ((JLabel) c).setVerticalAlignment(JLabel.TOP);
        c.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 0));
        if (!table.isCellSelected(rowIndex, vColIndex)) {
            c.setForeground(Color.black);
            if (rowIndex % 2 == 0) c.setBackground(new Color(240, 240, 240)); else c.setBackground(table.getBackground());
        }
        return c;
    }

    /**
	 * Display a file in the system browser. If you want to display a file, you
	 * must include the absolute path name.
	 * 
	 * @param url
	 *            the file's url (the url must start with either "http://" or
	 *            "file://").
	 */
    public static void displayURL(String url) {
        boolean windows = isWindowsPlatform();
        String cmd = null;
        try {
            if (windows) {
                cmd = WIN_PATH + " " + WIN_FLAG + " " + url;
                Process p = Runtime.getRuntime().exec(cmd);
            } else {
                try {
                    Class<?> d = Class.forName("java.awt.Desktop");
                    d.getDeclaredMethod("browse", new Class[] { java.net.URI.class }).invoke(d.getDeclaredMethod("getDesktop").invoke(null), new Object[] { java.net.URI.create(url) });
                } catch (Exception ignore) {
                    String browser = null;
                    for (String b : browsers) if (browser == null && Runtime.getRuntime().exec(new String[] { "which", b }).getInputStream().read() != -1) Runtime.getRuntime().exec(new String[] { browser = b, url });
                    if (browser == null) {
                        logger.fatal("Error bringing up browsers, url='" + url + "'");
                    }
                }
            }
        } catch (IOException x) {
            logger.fatal("Could not invoke browser, command=" + cmd);
            logger.fatal("Caught: " + x);
        }
    }

    /**
	 * Try to determine whether this application is running under Windows or
	 * some other platform by examing the "os.name" property.
	 * 
	 * @return true if this application is running under a Windows OS
	 */
    public static boolean isWindowsPlatform() {
        String os = System.getProperty("os.name");
        if (os != null && os.startsWith(WIN_ID)) return true; else return false;
    }

    /**
	 * Returns the disk file name of the class that is executing.
	 * 
	 * @return Name of class that is currently executing
	 */
    public String getClassName() {
        String thisClassName;
        thisClassName = this.getClass().getName();
        thisClassName = thisClassName.substring(thisClassName.lastIndexOf(".") + 1, thisClassName.length());
        thisClassName += ".class";
        return thisClassName;
    }

    /**
	 * Returns the name of the local directory based on the results of a call to
	 * getClassName().
	 * 
	 * @return Name of directory that contains the executing class
	 */
    public String getLocalDirName() {
        String localDirName;
        java.net.URL myURL = this.getClass().getResource(getClassName());
        localDirName = myURL.getPath();
        localDirName = myURL.getPath().replaceAll("%20", " ");
        localDirName = localDirName.substring(0, localDirName.lastIndexOf("/"));
        return localDirName;
    }

    /**
	 * Returns a File reference to the local directory based on the results of a
	 * call to getClassName().
	 * 
	 * @return File object that points to the local directory
	 */
    public java.io.File getLocalDirRef() {
        File myFileObj;
        myFileObj = new File(getLocalDirName());
        return myFileObj;
    }

    /**
	 * Populate combo box model.
	 * 
	 * @param model
	 *            the model
	 * @param elements
	 *            the elements
	 */
    public static void populateComboBoxModel(DefaultComboBoxModel model, Object[] elements) {
        model.removeAllElements();
        for (int i = 0; i < elements.length; i++) {
            model.addElement(elements[i]);
        }
    }

    /**
	 * Method setGlobalWaitCursor.
	 * 
	 * @param component
	 *            Component
	 */
    public static void setGlobalWaitCursor(Component component) {
        setGlobalCursor(Cursor.WAIT_CURSOR, component);
    }

    /**
	 * Method setGlobalNoWaitCursor.
	 * 
	 * @param component
	 *            Component
	 */
    public static void setGlobalNoWaitCursor(Component component) {
        setGlobalCursor(Cursor.DEFAULT_CURSOR, component);
    }

    /**
	 * Method setGlobalCursor.
	 * 
	 * @param cursor
	 *            int
	 * @param component
	 *            Component
	 */
    public static void setGlobalCursor(int cursor, Component component) {
        component.setCursor(Cursor.getPredefinedCursor(cursor));
        Frame frame = getFrame(component);
        if (frame != null) frame.setCursor(Cursor.getPredefinedCursor(cursor));
    }

    /**
	 * Gets the container frame of a component.
	 * 
	 * @param c
	 *            Component
	 * 
	 * @return Frame
	 */
    public static Frame getFrame(Component c) {
        if (c instanceof Frame) return (Frame) c;
        while ((c = c.getParent()) != null) if (c instanceof Frame) return (Frame) c;
        return null;
    }

    public static final void centerFrame(JFrame frame) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        if (frameSize.height > screenSize.height) frameSize.height = screenSize.height;
        if (frameSize.width > screenSize.width) frameSize.width = screenSize.width;
        frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
    }

    /**
	 * Centers a dialog with respect to the parent..
	 * 
	 * @param d
	 *            JDialog
	 */
    public static void centerDialog(JDialog d) {
        Frame parent = (Frame) d.getParent();
        Dimension dim = parent.getSize();
        Point loc = parent.getLocationOnScreen();
        Dimension size = d.getSize();
        loc.x += (dim.width - size.width) / 2;
        loc.y += (dim.height - size.height) / 2;
        if (loc.x < 0) loc.x = 0;
        if (loc.y < 0) loc.y = 0;
        Dimension screen = d.getToolkit().getScreenSize();
        if (size.width > screen.width) size.width = screen.width;
        if (size.height > screen.height) size.height = screen.height;
        if (loc.x + size.width > screen.width) loc.x = screen.width - size.width;
        if (loc.y + size.height > screen.height) loc.y = screen.height - size.height;
        d.setBounds(loc.x, loc.y, size.width, size.height);
    }

    /**
	 * Gets the preferred width.
	 * 
	 * @param c
	 *            the c
	 * 
	 * @return the preferred width
	 */
    public static int getPreferredWidth(JComponent c) {
        if (c instanceof JComboBox) {
            ComboBoxModel m = ((JComboBox) c).getModel();
            FontMetrics fm = c.getFontMetrics(c.getFont());
            int max = 0;
            for (int i = 0; i < m.getSize(); i++) {
                int current = fm.stringWidth(m.getElementAt(i).toString());
                if (current > max) max = current;
            }
            return max;
        } else if (c instanceof JSpinner) {
            SpinnerModel model = ((JSpinner) c).getModel();
            if (model instanceof SpinnerNumberModel) {
                String s = ((SpinnerNumberModel) model).getMaximum().toString();
                int current = getStringWidthForComponent(s, c);
                return current;
            }
        } else if (c instanceof JButton) {
            String s = ((JButton) c).getText();
            int current = getStringWidthForComponent(s, c);
            return current;
        } else if (c instanceof JLabel) {
            String s = ((JLabel) c).getText();
            int current = getStringWidthForComponent(s, c);
            return current;
        }
        return -1;
    }

    public static int getStringWidthForComponent(String s, Component c) {
        FontMetrics fm = c.getFontMetrics(c.getFont());
        return fm.stringWidth(s);
    }

    /**
	 * Gets the adjusted size.
	 * 
	 * @param additionalwidth
	 *            the additionalwidth
	 * @param additionalheight
	 *            the additionalheight
	 * @param c
	 *            the c
	 * 
	 * @return the adjusted size
	 */
    public static Dimension getAdjustedSize(JComponent c, int additionalwidth, int additionalheight) {
        return new Dimension((int) (c.getPreferredSize().getWidth() + additionalwidth), (int) c.getPreferredSize().getHeight() + additionalheight);
    }

    /**
	 * Gets the adjusted size.
	 * 
	 * @param d
	 *            the d
	 * @param additionalwidth
	 *            the additionalwidth
	 * @param additionalheight
	 *            the additionalheight
	 * 
	 * @return the adjusted size
	 */
    public static Dimension getAdjustedSize(Dimension d, int additionalwidth, int additionalheight) {
        return new Dimension((int) (d.getWidth() + additionalwidth), (int) d.getHeight() + additionalheight);
    }

    /**
	 * Tries to set a global cursor in the background.
	 */
    public class SetGlobalCursorThread implements Runnable {

        /**
		 * Field cursor.
		 */
        int cursor;

        /**
		 * Field component.
		 */
        Component component;

        /**
		 * The Constructor.
		 * 
		 * @param cursor
		 *            the cursor
		 * @param component
		 *            the component
		 */
        protected SetGlobalCursorThread(int cursor, Component component) {
            super();
            this.cursor = cursor;
            this.component = component;
        }

        /**
		 * Method run.
		 * 
		 * @see java.lang.Runnable#run()
		 */
        public void run() {
            component.setCursor(Cursor.getPredefinedCursor(cursor));
            Frame frame = getFrame(component);
            if (frame != null) frame.setCursor(Cursor.getPredefinedCursor(cursor));
        }
    }

    /**
	 * Populate list model.
	 * 
	 * @param model
	 *            the model
	 * @param elements
	 *            the elements
	 */
    public static void populateListModel(DefaultListModel model, Object[] elements) {
        model.removeAllElements();
        for (int i = 0; i < elements.length; i++) {
            model.addElement(elements[i]);
        }
    }

    public static boolean existsURL(String urlStr) {
        try {
            URL url = ProxyURLFactory.createHttpUrl(urlStr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.connect();
            int responseCode = con.getResponseCode();
            con.disconnect();
            return !(responseCode == HttpURLConnection.HTTP_NOT_FOUND);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean exists(String filename) {
        if (filename == null) return false;
        if (filename.startsWith("/")) filename = filename.substring(1);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream in = loader.getResourceAsStream(filename);
        return (in != null);
    }

    public static void enableORdisableAllChildren(Component child, boolean enabled) {
        child.setEnabled(enabled);
        if (child instanceof Container) {
            enableORdisableAllChildren(((Container) child).getComponents(), enabled);
        }
    }

    private static void enableORdisableAllChildren(Component[] components, boolean enabled) {
        for (Component child : components) {
            enableORdisableAllChildren(child, enabled);
        }
    }

    public static String getNullSafeToString(Object obj) {
        if (obj == null) return ""; else return obj.toString();
    }

    public static void showMetathesaurusWebpage(String cui) {
        if (cui.startsWith("C9")) cui = cui.replaceFirst("9", "L");
        GeneralUtilities.displayURL("http://ncim.nci.nih.gov/ncimbrowser/pages/" + "concept_details.jsf?dictionary=NCI MetaThesaurus&code=" + cui);
    }

    public static JSplitPane createSplitPane(JComponent leftComponent, JComponent rightComponent, int splitType) {
        JSplitPane split = new JSplitPane(splitType, true);
        split.setLeftComponent(leftComponent);
        split.setRightComponent(rightComponent);
        split.setDividerSize(5);
        split.setBorder(BorderFactory.createEmptyBorder());
        return split;
    }

    public static CaTIES_OrderSetImpl createCaseSetObj() {
        CaTIES_OrderSetImpl order = null;
        CaTIES_UserImpl cu = CaTIES_Desktop.getCurrentUser();
        order = CaTIES_MiddleTierImpl.createOrderSetObj();
        if (cu.isHonestBroker()) order.obj.setStatus(CaTIES_OrderSetImpl.STATUS_PENDING); else order.obj.setStatus(CaTIES_OrderSetImpl.STATUS_NOT_SUBMITTED);
        cu.getCurrentDistributionProtocolAssignment().obj.addOrderSet(order.obj);
        return order;
    }

    public static String getUseridFromDistinguishedName(String distinguishedName) {
        if (distinguishedName == null) return ""; else return distinguishedName.substring(distinguishedName.lastIndexOf("=") + 1);
    }

    public static String getPublicSecureAdminDispatcherHandle(Organization org) {
        String handle = org.getIndexHandle();
        return handle.substring(0, handle.lastIndexOf("/Dispatcher")) + "_admin" + "/Dispatcher";
    }

    public static String getPrivateSecureAdminDispatcherHandle(Organization org) {
        String handle = org.getIndexHandle();
        String publicadminServiceURI = getPublicSecureAdminDispatcherHandle(org);
        String privhandle = org.getPrivateHandle();
        if (privhandle != null && privhandle.trim().length() > 0) {
            return privhandle.substring(0, privhandle.indexOf("/wsrf/")) + publicadminServiceURI.substring(publicadminServiceURI.indexOf("/wsrf/"));
        }
        return null;
    }

    public static String getPublicSecureDispatcherHandle(Organization org) {
        return org.getIndexHandle();
    }

    public static String getPrivateSecureDispatcherHandle(Organization org) {
        String handle = getPublicSecureDispatcherHandle(org);
        String privhandle = org.getPrivateHandle();
        if (privhandle != null) {
            return privhandle.substring(0, privhandle.indexOf("/wsrf/")) + handle.substring(handle.indexOf("/wsrf/"));
        }
        return null;
    }

    /**
	 * @param name
	 * @param i
	 * @return
	 */
    public static String getShortenedString(String str, int length) {
        if (str.length() > length) {
            return str.substring(0, length - 2) + "..";
        }
        return str;
    }
}
