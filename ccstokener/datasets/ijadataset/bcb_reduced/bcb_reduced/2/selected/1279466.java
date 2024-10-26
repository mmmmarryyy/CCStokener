package com.sts.webmeet.applets;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Component;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ResourceBundle;
import java.util.Vector;
import com.sts.webmeet.common.I18NUtil;
import com.sts.webmeet.common.ImageScalingUtil;
import com.sts.webmeet.common.PackageUtil;
import com.sts.webmeet.content.common.MeetingScript;
import com.sts.webmeet.content.common.ScriptInfoList;
import com.sts.webmeet.upload.applet.FileDropEvent;
import com.sts.webmeet.upload.applet.FileDropListener;
import com.sts.webmeet.upload.common.AbstractUploadResultMessage;
import com.sts.webmeet.upload.common.FileInfo;
import com.sts.webmeet.upload.common.ScriptCreatedMessage;
import com.sts.webmeet.upload.common.ScriptCreationErrorMessage;
import com.sts.webmeet.upload.common.StopUploadMessage;
import com.sts.webmeet.upload.common.UploadInfo;

public class ScriptEditorApplet extends Applet implements ScriptEditContext, FileDropListener {

    public void init() {
        System.out.println("applet init");
        strServlet = getCodeBase() + getParameter("servlet");
        if (null != getParameter("customerID")) {
            customerID = new Integer(getParameter("customerID"));
        } else {
            throw new RuntimeException("error: customerID parameter required");
        }
        buildBundle();
        sui = new ThinletScriptUI();
        setLayout(new BorderLayout());
        ((thinlet.Thinlet) sui).setResourceBundle(bundle);
        sui.setLocale(this.locale);
        ((thinlet.Thinlet) sui).init();
        sui.setScriptEditContext(this);
        add((Component) sui, BorderLayout.CENTER);
        try {
            Class fileDropBeanClass = Class.forName("com.sts.webmeet.upload.applet.FileDropBean");
            Object dndInstance = fileDropBeanClass.newInstance();
            Method m = fileDropBeanClass.getMethod("addFileDropTarget", new Class[] { java.awt.Component.class });
            m.invoke(dndInstance, new Object[] { (Component) sui });
            m = fileDropBeanClass.getMethod("addFileDropListener", new Class[] { com.sts.webmeet.upload.applet.FileDropListener.class });
            m.invoke(dndInstance, new Object[] { this });
            System.out.println("drag and drop enabled.");
        } catch (Exception e) {
            System.out.println("drag and drop not enabled.");
            e.printStackTrace();
        }
        try {
            this.imageUtil = new ImageScalingUtil();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            sui.editScripts();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleUploadDone() {
        this.sui.hideUploadProgressDialog();
        this.sui.refreshScript();
    }

    public void handleUploadStopped() {
        this.bStopUpload = true;
    }

    public void filesDropped(FileDropEvent evt) {
        this.lTotalBytesUploaded = 0L;
        this.lTotalUploadSize = 0L;
        this.bStopUpload = false;
        this.droppedFiles = evt.getFiles();
        sanitizeUpload();
        if (this.sui.getCurrentScript() == null) {
            try {
                this.newScriptId = createNewScript();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("showing progress dialog...");
        this.sui.showUploadProgressDialog(this.droppedFiles.length, this.lTotalUploadSize);
        System.out.println("...done showing progress dialog.");
        Thread uploadThread = new Thread("Upload Thread") {

            public void run() {
                for (int i = 0; i < droppedFiles.length && !bStopUpload; i++) {
                    try {
                        System.out.println("trying upload (" + droppedFiles[i].getAbsolutePath() + ")");
                        doUpload(droppedFiles[i]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                sui.handleUploadDone();
                System.out.println("Upload thread exiting");
            }
        };
        uploadThread.start();
    }

    private Integer createNewScript() throws Exception {
        Integer scriptId = null;
        String strName = "New_Script_" + this.droppedFiles[0].getName() + "_" + System.currentTimeMillis();
        String strDesc = "New_script_description";
        String uploadUrl = this.getUploadUrlString() + "?scriptName=" + strName + "&scriptDescription=" + strDesc;
        URL url = new URL(uploadUrl);
        Object objMessage = this.getObject(url);
        if (objMessage instanceof ScriptCreatedMessage) {
            scriptId = ((ScriptCreatedMessage) objMessage).getScriptId();
        } else if (objMessage instanceof ScriptCreationErrorMessage) {
            System.out.println("problem creating script: " + ((ScriptCreationErrorMessage) objMessage).getMessage());
        }
        return scriptId;
    }

    private void buildBundle() {
        System.out.println("building bundle");
        String strLanguage = getParameter("language");
        String strCountry = getParameter("country");
        locale = I18NUtil.getInstance().getSupportedLocale(strLanguage, strCountry);
        String strBundle = PackageUtil.getParent(getClass().getName()) + ".Resources";
        bundle = ResourceBundle.getBundle(strBundle, locale);
    }

    public ScriptInfoList getScriptList() {
        ScriptInfoList list = null;
        try {
            list = getScriptListFromServer(customerID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public MeetingScript getScript(Integer id) {
        MeetingScript script = null;
        try {
            script = getScriptFromServer(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return script;
    }

    public String getSessionID() {
        return getParameter(SESSION_ID_PARM);
    }

    public void allDone() {
        browseToAccountPage();
    }

    public void scriptDone(MeetingScript script) {
        try {
            postObject(script, strServlet + getSessionIDSuffix());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String[] getItemClassNames() {
        return ITEM_CLASSES;
    }

    public void deleteScript(Integer id) {
        InputStream is = null;
        try {
            URL url = new URL(strServlet + getSessionIDSuffix() + "?deleteScript=" + id);
            System.out.println("requesting: " + url);
            is = url.openStream();
            while (is.read() != -1) ;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
    }

    private void browseToAccountPage() {
        try {
            URL urlBase = getCodeBase();
            URL urlNew = new URL(urlBase, getParameter("done_page") + getSessionIDSuffix());
            getAppletContext().showDocument(urlNew);
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        }
    }

    private void sanitizeUpload() {
        this.lTotalUploadSize = 0L;
        Vector vectFiles = new Vector();
        for (int i = 0; i < this.droppedFiles.length; i++) {
            if (isSupportedFileFormat(this.droppedFiles[i])) {
                vectFiles.add(this.droppedFiles[i]);
                this.lTotalUploadSize += this.droppedFiles[i].length();
            }
        }
        this.droppedFiles = (File[]) vectFiles.toArray(new File[0]);
    }

    private boolean isSupportedFileFormat(File file) {
        if (file.getName().toLowerCase().endsWith(JPG_SUFFIX) || file.getName().toLowerCase().endsWith(GIF_SUFFIX) || file.getName().toLowerCase().endsWith(JPEG_SUFFIX)) {
            return true;
        }
        return false;
    }

    private String getSessionIDSuffix() {
        return ";jsessionid=" + getParameter(SESSION_ID_PARM);
    }

    private MeetingScript getScriptFromServer(Integer scriptID) throws Exception {
        MeetingScript script = (MeetingScript) getObject(strServlet + getSessionIDSuffix() + "?script=" + scriptID);
        return script;
    }

    private ScriptInfoList getScriptListFromServer(Integer customerID) throws Exception {
        ScriptInfoList scripts = (ScriptInfoList) getObject(strServlet + getSessionIDSuffix());
        return scripts;
    }

    private Object getObject(URL url) throws Exception {
        Object objRet = null;
        System.out.println("requesting: " + url);
        URLConnection urlConn = url.openConnection();
        urlConn.setUseCaches(false);
        ObjectInputStream ois = new ObjectInputStream(urlConn.getInputStream());
        objRet = ois.readObject();
        ois.close();
        return objRet;
    }

    private Object getObject(String strURL) throws Exception {
        URL url = new URL(strURL);
        return getObject(url);
    }

    private void postObject(Object obj, String strURL) throws Exception {
        print("entering post object");
        URL url = new URL(strURL);
        URLConnection urlConn = url.openConnection();
        print("HttpNetworkMessageConnection.postObject:returned from url.openConnection()");
        urlConn.setUseCaches(false);
        urlConn.setDoOutput(true);
        ObjectOutputStream oos = new ObjectOutputStream(urlConn.getOutputStream());
        print("HttpNetworkMessageConnection.postObject:returned from urlConn.getOutputStream()");
        oos.writeObject(obj);
        print("HttpNetworkMessageConnection.postObject:returned from writeObject()");
        oos.flush();
        oos.close();
        InputStream is = urlConn.getInputStream();
        print("HttpNetworkMessageConnection.postObject:returned from getInputStream()");
        while (is.read() != -1) {
        }
        is.close();
        print("exiting postObject");
    }

    private void doUpload(File file) throws Exception {
        ObjectInputStream ois = null;
        ObjectOutputStream oos = null;
        System.out.println(getClass().getName() + ".doUpload: " + file.getAbsolutePath());
        UploadInfo uploadInfo = new UploadInfo(file.getName(), null != this.sui.getCurrentScript() ? this.sui.getCurrentScript().getID() : this.newScriptId);
        InputStream is = new FileInputStream(file);
        long length = file.length();
        if (this.imageUtil != null) {
            this.sui.showUploadMessage("Scaling " + file.getName());
            byte[] scaledJpeg = this.imageUtil.scaledJpegForInputStream(is, 640, 480);
            is = new ByteArrayInputStream(scaledJpeg);
            length = scaledJpeg.length;
        }
        uploadInfo.addFileInfo(new FileInfo(file.getName(), length));
        URLConnection urlConnection = this.getUploadConnection();
        oos = new ObjectOutputStream(urlConnection.getOutputStream());
        System.out.println(getClass().getName() + ".doUpload: got upload stream");
        oos.writeObject(uploadInfo);
        System.out.println(getClass().getName() + ".doUpload: wrote upload info");
        byte[] buff = new byte[16 * 1024];
        long lBytesWritten = 0;
        long lFileBytesWritten = 0;
        this.sui.uploadingFile(file.getName(), length);
        while (lBytesWritten < length) {
            if (bStopUpload) {
                oos.writeObject(new StopUploadMessage());
                this.sui.showUploadMessage("Stopping upload...");
                break;
            }
            int iRead = is.read(buff);
            if (iRead != -1) {
                byte[] ba = new byte[iRead];
                System.arraycopy(buff, 0, ba, 0, ba.length);
                oos.writeObject(ba);
                oos.reset();
                System.out.println(getClass().getName() + ".doUpload: wrote " + ba.length + " bytes");
                lBytesWritten += iRead;
                lFileBytesWritten += iRead;
                lTotalBytesUploaded += iRead;
                this.sui.showUploadProgressUpdate(lFileBytesWritten, this.lTotalBytesUploaded);
                Thread.yield();
                System.out.println("returned from updating progress");
            } else {
                break;
            }
        }
        ois = new ObjectInputStream(urlConnection.getInputStream());
        AbstractUploadResultMessage result = (AbstractUploadResultMessage) ois.readObject();
        this.sui.showUploadMessage(result.getMessage());
        oos.close();
        ois.close();
    }

    private URLConnection getUploadConnection() throws Exception {
        URL url = new URL(getUploadUrlString());
        System.out.println(getClass().getName() + ".getUploadStream: opening URL: " + url);
        URLConnection urlConnection = url.openConnection();
        urlConnection.setDoOutput(true);
        urlConnection.setUseCaches(false);
        return urlConnection;
    }

    private String getUploadUrlString() throws Exception {
        return new URL(this.getCodeBase(), UPLOAD_SERVLET + TOMCAT_SESSION_SUFFIX + getParameter(SESSION_ID_PARAM)).toString();
    }

    private static void print(String str) {
        if (bDebug) {
            System.out.println(str);
        }
    }

    private static boolean bDebug = false;

    private static final String[] ITEM_CLASSES = { "com.sts.webmeet.content.server.questions.Server", "com.sts.webmeet.content.server.appshare.Server" };

    private static final String SESSION_ID_PARM = "session_id";

    private java.util.ResourceBundle bundle;

    private ScriptUI sui;

    private String strServlet;

    private Integer customerID;

    private java.util.Locale locale;

    private File[] droppedFiles;

    private long lTotalUploadSize;

    private long lTotalBytesUploaded;

    private boolean bStopUpload;

    private Integer newScriptId;

    private static final String UPLOAD_SERVLET = "bulkUpload";

    private static final String TOMCAT_SESSION_SUFFIX = ";jsessionid=";

    private static final String SESSION_ID_PARAM = "session_id";

    private static final long MEGABYTE = 1000 * 1000;

    private static final long MAX_UPLOAD_SIZE = 10 * MEGABYTE;

    private static final String GIF_SUFFIX = ".gif";

    private static final String JPG_SUFFIX = ".jpg";

    private static final String JPEG_SUFFIX = ".jpeg";

    private ImageScalingUtil imageUtil;
}
