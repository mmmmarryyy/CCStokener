package org.scidac.sam.eln.client.applet;

import java.applet.*;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;
import java.net.URL;
import java.net.URI;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import javax.swing.*;
import org.apache.util.HttpURL;
import org.apache.webdav.lib.WebdavResource;
import org.apache.webdav.lib.methods.DepthSupport;
import org.apache.webdav.lib.Property;
import org.apache.webdav.lib.PropertyName;
import org.apache.webdav.lib.ResponseEntity;
import eln.client.event.NObActionListener;
import eln.client.event.NObActionEvent;
import eln.client.event.EditorLaunchListener;
import eln.client.NetNBEPanel;
import eln.server.NotebookServerProxyFactory;
import eln.server.NotebookServerProxy;
import eln.server.NotebookServerException;
import eln.server.GenericConnection;
import eln.nob.NObNode;
import eln.nob.NObListNode;
import eln.nob.NObList;
import eln.nob.NOb;
import eln.util.StatusChangeListener;
import eln.util.StatusEvent;
import eln.editors.WhiteBoardWrapper2;
import org.scidac.sam.eln.nob.WebdavNOb;
import org.scidac.sam.eln.serverproxy.SAMNotebookServerProxy;
import org.scidac.sam.nbservices.exim.NoteAddition;

public class EditorApplet extends Applet implements NObActionListener, WindowListener, StatusChangeListener, EditorLaunchListener {

    /**
     * Symbollic constants
     */
    public static final int kIE = 1;

    public static final int kPlugIn = 2;

    public static final int kNetscape = 3;

    /**
     * Member Variables
     */
    protected String mFileAnnotationRoot;

    protected String mServerUrl;

    protected String mUser;

    protected String mPw;

    protected String mBgColor;

    protected NetNBEPanel mEditorPanel;

    protected NotebookServerProxy mServer;

    protected String mCurrentNodeUrl;

    protected NObListNode mNewNObList;

    protected NoteAddition currentAddition = null;

    protected GenericConnection mBrowserConnection;

    protected String mImageUrl;

    protected byte[] mImageBytes;

    protected boolean mEditMode;

    protected String codebase;

    protected Integer additionToEdit;

    protected String browserCookie;

    /**
     *  Constructor
     */
    public EditorApplet() {
    }

    /**
     *  Gets the appletInfo attribute of the AnnotationTool applet object.
     *
     * @return    The appletInfo value
     */
    public String getAppletInfo() {
        return "SAM Project - Editor applet ";
    }

    /**
     * Initializes the applet
     */
    public void init() {
        mFileAnnotationRoot = getParameter("file_annotation_root");
        mBgColor = getParameter("background_color");
        mServerUrl = getParameter("server_url");
        mCurrentNodeUrl = getParameter("selected_node");
        String edit = getParameter("edit");
        browserCookie = getParameter("browserCookie");
        try {
            codebase = getParameter("eln_codebase");
            DirectCommandProcessor cmndProc = new DirectCommandProcessor(codebase, this, getJVM(), getParameter("loginNames"), getParameter("allowGuests"), getParameter("allowNewUsers"), mFileAnnotationRoot, mServerUrl, getParameter("context"));
            mBrowserConnection = new DirectConnection(cmndProc);
            NotebookServerProxyFactory factory = NotebookServerProxyFactory.getFactory();
            mServer = factory.createServerProxy(mBrowserConnection);
            mEditorPanel = new NetNBEPanel(mServer);
            mEditorPanel.addNObActionListener(this);
            mEditorPanel.addEditorWindowListener(this);
            mEditorPanel.addEditorLaunchListener(this);
            mEditorPanel.addStatusListener(this);
            JPanel panel = new JPanel();
            if (mBgColor != null && !mBgColor.equals("")) {
                System.out.println("Setting background color to: " + mBgColor);
                try {
                    Color color = Color.decode(mBgColor);
                    panel.setBackground(color);
                    setBackground(Color.WHITE);
                } catch (Exception e) {
                    System.err.println("Invalid format of background_color" + " applet parameter. " + "Background not set!");
                }
            }
            panel.add(mEditorPanel);
            add(panel);
            System.out.println("EditorApplet started successfully");
        } catch (Exception e) {
            System.err.println("Initialization error for server URL: " + mServerUrl);
            e.printStackTrace();
        }
        if (edit != null && !edit.equals("")) {
            mEditMode = true;
            additionToEdit = new Integer(edit);
            launchInEditMode();
        } else {
            mEditMode = false;
        }
    }

    /**
     * Method Description:  Called when editor has a status message.
     *
     * @param evt the actual event being submitted
    */
    public void statusChanged(StatusEvent statusEvt) {
        System.out.println(statusEvt.toString());
    }

    /**
     * Method Description:  Called right before editor panel launches a new
     *  editor.  Gives us the chance to launch with a NObNode instead of null.
     *
     * @param className The fully qualified classname of the editor that is
     *                  being launched
     * @return a NObNode if the editor should launch with a context other than
     *         empty.  Null if the editor content should be empty on launch.
     */
    public NObNode editorLaunching(String className) {
        System.out.println("calling editor launching");
        NObNode ret = null;
        if (className.equals("eln.editors.WhiteBoardWrapper2")) {
            NObList nobList = new NObList();
            if (mCurrentNodeUrl == null || mCurrentNodeUrl.equals("")) {
                if (mImageUrl != null) {
                    System.out.println("Trying to load background image");
                    NOb nob = new NOb();
                    byte[] bytes = loadBackgroundImage();
                    nob.put("data", bytes);
                    nob.put("dataType", "image/whiteboard");
                    nob.put("label", "Whiteboard image");
                    nob.put("editor", "eln.editors.WhiteBoardWrapper2");
                    nob.put("node_id", WhiteBoardWrapper2.BACKGROUND_IMAGE);
                    nobList.insert(nob);
                }
            } else {
                System.out.println("looking up content for: " + mCurrentNodeUrl);
            }
            if (ret == null) {
                ret = nobList;
            }
        }
        mEditorPanel.setEnabled(false);
        return ret;
    }

    /**
     * Method Description:  Called when an editor saves a NOb.
     * This method copies properties from NOb to a new NoteAddition and
     *  sends that node addition to the servlet at /sam/echo to be added to
     *  the temp store.
     *
     * @param evt the actual event being submitted
     */
    public void performNObAction(NObActionEvent evt) {
        System.out.println("Entering performNObAction");
        NObNode newNob = evt.getNObToChange();
        NObNode curNob;
        Vector nobs = new Vector();
        if (newNob instanceof NObListNode) {
            Enumeration children = newNob.children();
            while (children.hasMoreElements()) {
                nobs.add(children.nextElement());
            }
        } else {
            nobs.add(newNob);
        }
        int numNobs = nobs.size();
        int index = 0;
        for (int i = 0; i < numNobs; i++) {
            curNob = (NObNode) nobs.elementAt(i);
            index = nobs.indexOf(curNob);
            Enumeration keys = curNob.keys();
            String key = "";
            while (keys.hasMoreElements()) {
                key = (String) keys.nextElement();
            }
            try {
                String label = (String) curNob.get("label");
                NoteAddition input;
                Object newData = curNob.get("data");
                Object dataRef = curNob.get("dataref");
                if ((mEditMode) && (currentAddition != null)) {
                    input = currentAddition;
                } else {
                    input = new NoteAddition(label);
                    input.setProp("id", new Long(new java.util.Date().getTime()));
                }
                if (newData != null) {
                    if (newData.getClass().getName().equals("java.lang.String")) {
                        newData = ((String) newData).getBytes();
                    }
                    input.setProp("data", newData);
                } else if (dataRef != null) {
                    input.setProp("dataref", (String) dataRef);
                    File dataFile = new File((String) dataRef);
                    InputStream dataStream = (InputStream) new FileInputStream(dataFile);
                    newData = new byte[dataStream.available()];
                    int len = dataStream.read((byte[]) newData);
                    input.setProp("data", newData);
                    newData = null;
                }
                try {
                    input.setProp("datatype", (String) curNob.get("datatype"));
                } catch (Exception dt) {
                    System.out.println("Exception occured setting datatype: " + dt);
                }
                try {
                    input.setProp("editor", (String) curNob.get("editor"));
                } catch (Exception ed) {
                    System.out.println("Exception occured setting editor: " + ed);
                }
                try {
                    input.setProp("datalength", new Long(curNob.getDataLength()).toString());
                } catch (Exception dl) {
                    System.out.println("Exception occured setting datalength: " + dl);
                }
                java.net.URL urlServlet = new java.net.URL(mServerUrl + "/echo");
                java.net.URLConnection con = urlServlet.openConnection();
                con.setDoInput(true);
                con.setDoOutput(true);
                con.setUseCaches(false);
                con.setRequestProperty("Content-Type", "application/x-java-serialized-object");
                if (browserCookie != null) con.setRequestProperty("Cookie", getParameter("browserCookie"));
                java.io.OutputStream outstream = con.getOutputStream();
                java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(outstream);
                oos.writeObject(input);
                oos.flush();
                oos.close();
                java.io.InputStream in = con.getInputStream();
                java.io.ObjectInputStream inputFromServlet = new java.io.ObjectInputStream(in);
                try {
                    Object echo = inputFromServlet.readObject();
                    System.out.println("Response:" + echo);
                } catch (Exception e) {
                    System.out.println(e);
                }
                inputFromServlet.close();
                in.close();
                mEditMode = false;
            } catch (java.net.MalformedURLException e) {
                System.out.println("ex" + e);
            } catch (java.io.IOException e2) {
                System.out.println("ex2" + e2);
            }
            mEditorPanel.setEnabled(true);
            mEditMode = false;
        }
        System.out.println("Exiting performNObAction");
    }

    /**
     * Invoked when the Window is set to be the active Window.
     */
    public void windowActivated(WindowEvent e) {
    }

    /**
     * Invoked when a window has been closed as the result of calling dispose
     * on the window.  When the editor window closes, we discard any unsaved
     * changes.
     *
     * TODO: pop up a modal dialog asking if user wants to save
     */
    public void windowClosed(WindowEvent e) {
        mEditorPanel.setEnabled(true);
        mEditMode = false;
    }

    /**
     * Invoked when the user attempts to close the window from the window's
     * system menu.
     */
    public void windowClosing(WindowEvent e) {
        mEditorPanel.setEnabled(true);
        mEditMode = false;
    }

    /**
     * Invoked when a Window is no longer the active Window.
     */
    public void windowDeactivated(WindowEvent e) {
    }

    /**
     * Invoked when a window is changed from a minimized to a normal state.
     */
    public void windowDeiconified(WindowEvent e) {
    }

    /**
     * Invoked when a window is changed from a normal to a minimized state.
     */
    public void windowIconified(WindowEvent e) {
    }

    /**
     * Invoked the first time a window is made visible.
     */
    public void windowOpened(WindowEvent e) {
    }

    protected int getJVM() {
        int ret = 0;
        try {
            float jversion = (Float.valueOf(System.getProperty("java.specification.version"))).floatValue();
            if ((jversion - 1.39) >= 0) {
                ret = kPlugIn;
                System.out.println("Using Java 1.4+ security model");
            } else {
                System.out.println("JVM Version older than 1.4");
                throw new Exception("Old version with weak security settings");
            }
        } catch (Exception e) {
            try {
                if (((String) (netscape.javascript.JSObject.getWindow(this)).eval("navigator.appName")).indexOf("Netscape") != -1) {
                    System.out.println("Using Netscape Security Model");
                    ret = kNetscape;
                }
            } catch (NoClassDefFoundError ne) {
            } catch (Exception npe) {
            }
            if (ret != kNetscape) {
                try {
                    Class.forName("com.ms.security.PolicyEngine");
                    ret = kIE;
                    System.out.println("ELN: Using IE Security Model");
                } catch (Exception cnfe) {
                    ret = kPlugIn;
                    System.out.println("Using Java 1.4+ Security Model " + "by default");
                }
            }
        }
        return ret;
    }

    /**
     * Retrieves the current annotation, extracts its content, and then
     * launches the appropriate editor.
     */
    protected void launchInEditMode() {
        HttpURL url = new HttpURL(mCurrentNodeUrl);
        try {
            java.net.URL urlServlet = new java.net.URL(mServerUrl + "/echo");
            java.net.URLConnection con = urlServlet.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestProperty("Content-Type", "application/x-java-serialized-object");
            if (browserCookie != null) con.setRequestProperty("Cookie", getParameter("browserCookie"));
            java.io.OutputStream outstream = con.getOutputStream();
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(outstream);
            oos.writeObject(additionToEdit);
            oos.flush();
            oos.close();
            java.io.InputStream in = con.getInputStream();
            java.io.ObjectInputStream inputFromServlet = new java.io.ObjectInputStream(in);
            try {
                currentAddition = (NoteAddition) inputFromServlet.readObject();
                System.out.println("Response:" + currentAddition);
            } catch (Exception e) {
                System.out.println(e);
            }
            inputFromServlet.close();
            in.close();
        } catch (java.net.MalformedURLException e) {
            System.out.println("ex" + e);
        } catch (java.io.IOException e2) {
            System.out.println("ex2" + e2);
        }
        try {
            String datatype = (String) currentAddition.getProp("datatype");
            NObNode contentNode = extractContent(currentAddition);
            boolean readonly = false;
            mEditorPanel.launchWithNOb(contentNode, readonly);
            mEditorPanel.setEnabled(false);
        } catch (Exception e) {
            System.err.println("Failed to launch editor on node: " + mCurrentNodeUrl);
            e.printStackTrace();
        }
    }

    /**
       * Extract all the content nodes from the mNewNObList.  This list could
       * contain children that represent other sub-annotations, and we will
       * omit them from the return list.  The return value should only be a
       * single node unless this is a whiteboard object.  In that case it could
       * have multiple nodes, one for the xml content, and other for imported
       * images
       */
    protected NObNode extractContent(NoteAddition annotation) {
        NObNode curNode;
        NObList nobList = new NObList();
        NObNode ret = null;
        String curType;
        try {
            curType = (String) annotation.getProp("datatype");
            System.out.println("datatype: " + curType);
            curNode = convertToNOb(annotation);
            System.out.println("curNode is " + curNode);
            if (("image/whiteboard").equals(curType) || ("application/whiteboard").equals(curType) || ("image/gif").equals(curType)) {
                String editor = (String) curNode.get("editor");
                String user = (String) curNode.get("portal_user");
                ret = curNode;
            } else {
                byte[] d = (byte[]) curNode.get("data");
                Object insertData = new String(d);
                System.out.println("data is " + insertData);
                curNode.put("data", insertData);
                ret = curNode;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
       * Converts a NoteAddition to a NOb.  We need to do this because the
       * NBEditorPanel will cast all NObNodes to NOb, so if we pass the
       * WebdavNOb, we get ClassCastException
       */
    protected NOb convertToNOb(NoteAddition na) {
        NOb ret = new NOb();
        Enumeration keys = na.keys();
        String key;
        while (keys.hasMoreElements()) {
            key = (String) keys.nextElement();
            ret.put(key, na.getProp(key));
        }
        return ret;
    }

    protected byte[] loadBackgroundImage() {
        byte[] ret = null;
        WebdavResource wdResource = null;
        try {
            String password = getParameter("portal_pass");
            HttpURL httpURL = new HttpURL(mImageUrl);
            httpURL.setUserInfo(mUser, password);
            System.out.println("Trying to connect to url: " + mImageUrl);
            wdResource = new WebdavResource(mImageUrl);
            File tmp = File.createTempFile("wbimage", null);
            wdResource.getMethod(tmp);
            ret = WhiteBoardWrapper2.getBytesFromFile(tmp);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (wdResource != null) wdResource.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return ret;
    }
}
