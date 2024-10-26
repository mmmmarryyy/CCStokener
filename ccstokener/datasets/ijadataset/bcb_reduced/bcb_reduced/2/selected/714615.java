package co.edu.unal.ungrid.services.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import co.edu.unal.ungrid.core.DirUtil;
import co.edu.unal.ungrid.core.MemoryHelper;
import co.edu.unal.ungrid.core.Synchronizable;
import co.edu.unal.ungrid.services.client.applet.document.AbstractDocument;
import co.edu.unal.ungrid.services.client.service.ServiceApp;
import co.edu.unal.ungrid.services.client.service.ServiceFactory;
import co.edu.unal.ungrid.services.client.util.BaseDlg;
import co.edu.unal.ungrid.services.client.util.ServiceOptions;

public abstract class ProxyUtil {

    public static void log(final String msg) {
        assert msg != null;
        ServiceFactory.getInstance().log(msg);
    }

    public static String getRemotePath(final String sLocalPath) {
        assert sLocalPath != null;
        String sSvcHome = ServiceFactory.getInstance().getSvcHome();
        String sFileName = sLocalPath;
        int n = sLocalPath.indexOf(sSvcHome);
        if (n > 0) {
            sFileName = sLocalPath.substring(n + sSvcHome.length() + 1);
        } else {
            n = sLocalPath.lastIndexOf(File.separatorChar);
            if (n > 0) {
                sFileName = sLocalPath.substring(n + 1);
            }
        }
        return sFileName;
    }

    private static String trimImgsDir(String sFileName) {
        String sImgsDir = getUserImgsDir();
        int n = sFileName.indexOf('/');
        if (n < 0) {
            n = sFileName.indexOf('\\');
        }
        if (n > 0) {
            String sSubDir = sFileName.substring(0, n);
            int m = sImgsDir.indexOf(sSubDir);
            if (m > 0) {
                sFileName = trimImgsDir(sFileName.substring(n + 1));
            }
        } else {
            int m = sImgsDir.indexOf(sFileName);
            if (m > 0) {
                sFileName = "";
            }
        }
        return sFileName;
    }

    public static String getLocalPath(final String sRemotePath) {
        assert sRemotePath != null;
        String sUserName = ServiceFactory.getInstance().getUserAccount();
        String sFileName = sRemotePath;
        int n = sRemotePath.indexOf(sUserName);
        if (n > 0) {
            sFileName = sRemotePath.substring(n + sUserName.length() + 1);
        } else {
            n = sRemotePath.lastIndexOf(File.separatorChar);
            if (n > 0) {
                sFileName = sRemotePath.substring(n + 1);
            }
        }
        return trimImgsDir(sFileName);
    }

    public static String getDocLocalPath(final String sRemotePath) {
        return (getUserDocsDir() + File.separator + getLocalPath(sRemotePath));
    }

    public static String getImgLocalPath(final String sRemotePath) {
        return (getUserImgsDir() + File.separator + getLocalPath(sRemotePath));
    }

    public static String getRemoteURL(final String sFilePath) {
        assert sFilePath != null;
        final ServiceApp app = ServiceFactory.getInstance();
        String sProtocol = app.getProtocol();
        String sAuthority = app.getAuthority();
        String sPort = app.getPort();
        String sService = app.getDocString().toLowerCase();
        String sFileName = sFilePath;
        int n = sFilePath.indexOf(sService);
        if (n > 0) {
            sFileName = sFilePath.substring(n + sService.length() + 1);
        } else {
            n = sFilePath.lastIndexOf(File.separatorChar);
            if (n > 0) {
                sFileName = sFilePath.substring(n + 1);
            }
        }
        String sRemote = sProtocol + "://" + sAuthority + ":" + sPort + File.separator;
        sRemote += sService + File.separator + USR_SVR_DIR + File.separator;
        sRemote += app.getUserAccount() + File.separator + sFileName;
        return sRemote;
    }

    public static String showImgFileChooserSave(final String sTitle, final String sCurDir, final String sCurName) {
        assert sTitle != null;
        assert sCurDir != null;
        assert sCurName != null;
        String sFileName = null;
        JFileChooser fc = new JFileChooser(sCurDir);
        fc.setSelectedFile(new File(sCurName));
        class Filter extends FileFilter {

            Filter(String sFmt, String sDsc) {
                this.sFmt = sFmt;
                this.sDsc = sDsc;
            }

            public boolean accept(File f) {
                return f.getName().endsWith(sFmt);
            }

            public String getDescription() {
                return sDsc;
            }

            String sFmt;

            String sDsc;
        }
        for (int i = 0; i < saSaveImgFmts.length; i++) {
            fc.addChoosableFileFilter(new Filter(saSaveImgFmts[i], saSaveImgFmtsDesc[i]));
        }
        int r = fc.showSaveDialog(JOptionPane.getRootFrame());
        if (r == JFileChooser.APPROVE_OPTION) {
            sFileName = fc.getSelectedFile().getAbsolutePath();
        }
        return sFileName;
    }

    public static String showSaveDocFileChooser(final String sCurDir, final String sCurName) {
        assert sCurDir != null;
        assert sCurName != null;
        String sFileName = null;
        final ServiceApp app = ServiceFactory.getInstance();
        JFileChooser fc = new JFileChooser(sCurDir);
        fc.setSelectedFile(new File(sCurName));
        fc.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                return (f.isDirectory() || (f.isFile() && f.getName().endsWith(app.getDocSuffix())));
            }

            public String getDescription() {
                return (app.getDocString() + " files (*" + app.getDocSuffix() + ")");
            }
        });
        int r = fc.showSaveDialog(JOptionPane.getRootFrame());
        if (r == JFileChooser.APPROVE_OPTION) {
            sFileName = fc.getSelectedFile().getAbsolutePath();
            if (!sFileName.endsWith(app.getDocSuffix())) {
                sFileName += app.getDocSuffix();
            }
        }
        return sFileName;
    }

    public static String showOpenDocFileChooser(final String sTitle) {
        assert sTitle != null;
        String sFileName = null;
        final ServiceApp app = ServiceFactory.getInstance();
        JFileChooser fc = new JFileChooser(getUserDocsDir());
        fc.setDialogTitle(sTitle);
        fc.addChoosableFileFilter(new FileFilter() {

            public boolean accept(File f) {
                return (f.isDirectory() || (f.isFile() && f.getName().endsWith(app.getDocSuffix())));
            }

            public String getDescription() {
                return (app.getDocString() + " files (*" + app.getDocSuffix() + ")");
            }
        });
        int r = fc.showOpenDialog(JOptionPane.getRootFrame());
        if (r == JFileChooser.APPROVE_OPTION) {
            sFileName = fc.getSelectedFile().getAbsolutePath();
        }
        return sFileName;
    }

    public static File showOpenImageFileChooser(final String sCurDir, final String sTitle) {
        assert sCurDir != null;
        assert sTitle != null;
        File file = null;
        JFileChooser fc = new JFileChooser(sCurDir);
        fc.setDialogTitle(sTitle);
        fc.addChoosableFileFilter(new FileFilter() {

            private boolean isSupported(final String fn) {
                boolean r = false;
                for (String sExt : ProxyUtil.saOpenImgFmts) {
                    if (fn.endsWith(sExt)) {
                        r = true;
                        break;
                    }
                }
                return r;
            }

            public boolean accept(File f) {
                return (f.isDirectory() || isSupported(f.getName().toLowerCase()));
            }

            public String getDescription() {
                return ProxyUtil.saOpenImgFmtsDesc;
            }
        });
        int r = fc.showOpenDialog(JOptionPane.getRootFrame());
        if (r == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
        }
        return file;
    }

    public static String selectImageNameSave(final String sTitle, final String sCurDir, final String sCurFile) {
        assert sTitle != null;
        assert sCurDir != null;
        assert sCurFile != null;
        String sFileName = showImgFileChooserSave(sTitle, sCurDir, sCurFile);
        if (sFileName != null) {
            File f = new File(sFileName);
            if (f.exists()) {
                int r = ServiceFactory.getInstance().readUsrYesNoCancel(sFileName + " already exists.\n\nOverwrite existing file?");
                if (r == BaseDlg.CANCEL) {
                } else if (r == BaseDlg.NO) {
                    sFileName = selectImageNameSave(sTitle, sCurDir, sCurFile);
                } else if (r == BaseDlg.YES) {
                }
            }
        }
        return sFileName;
    }

    public static ServiceOptions loadLocalOptions(final String sFileName) {
        assert sFileName != null;
        ServiceOptions opts = null;
        File f = new File(sFileName);
        try {
            if (f.canRead()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
                opts = (ServiceOptions) ois.readObject();
                ois.close();
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        if (opts == null) {
            log("*** ProxyUtil::loadLocalOptions(): unable to load options from " + sFileName);
        }
        return opts;
    }

    public static void saveLocalOptions(final ServiceOptions opts, final String sFileName) {
        assert opts != null;
        assert sFileName != null;
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(sFileName));
            oos.writeObject(opts);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int saveLocalSyncObj(final SyncResource sync) {
        assert sync != null;
        assert sync.getLocalPath() != null;
        assert sync.getData() != null;
        int r = BaseDlg.OK;
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(sync.getLocalPath()));
            oos.writeObject(sync.getData());
            oos.close();
        } catch (Exception exc) {
            r = BaseDlg.FAILED;
            log("*** ProxyUtil::saveLocalSyncObj(): writing local file '" + sync.getLocalPath() + "' failed: " + exc);
        }
        return r;
    }

    public static Synchronizable loadLocalSyncObj(final String sFilePath) {
        assert sFilePath != null;
        Synchronizable obj = null;
        File f = new File(sFilePath);
        if (f.canRead()) {
            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
                obj = (Synchronizable) ois.readObject();
                ois.close();
            } catch (Exception exc) {
                log("*** ProxyUtil::loadLocalSyncObj(): error reading local file " + sFilePath + ": " + exc);
            }
        } else {
            log("*** ProxyUtil::loadLocalSync(): can't read local file " + sFilePath);
        }
        return obj;
    }

    public static AbstractDocument loadLocalDoc(final String sFilePath) {
        assert sFilePath != null;
        AbstractDocument doc = null;
        Synchronizable obj = loadLocalSyncObj(sFilePath);
        if (obj instanceof AbstractDocument) {
            doc = (AbstractDocument) obj;
            doc.setLocalPath(sFilePath);
            doc.setRemotePath(getRemotePath(sFilePath));
        }
        return doc;
    }

    public static SyncResource loadLocalImg(final String sFilePath) {
        assert sFilePath != null;
        SyncResource img = null;
        File f = new File(sFilePath);
        if (f.canRead()) {
            byte[] ba = MemoryHelper.createByte1D((int) f.length());
            if (ba != null) {
                try {
                    FileInputStream is = new FileInputStream(f);
                    is.read(ba);
                    is.close();
                    img = new SyncResource(sFilePath, getRemotePath(sFilePath), ba);
                } catch (Exception exc) {
                    log("*** ProxyUtil::loadLocalImg(): error reading local image " + sFilePath + ": " + exc);
                }
            }
        }
        return img;
    }

    public static boolean saveLocalImg(final String sFilePath, final byte[] ba) {
        assert sFilePath != null;
        assert ba != null;
        boolean b = false;
        try {
            FileOutputStream os = new FileOutputStream(sFilePath);
            os.write(ba);
            os.close();
            b = true;
        } catch (Exception exc) {
            log("*** ProxyUtil::saveLocalImg(): error writing local image " + sFilePath + ": " + exc);
        }
        return b;
    }

    public static boolean createEmptyFile(final String sPathName) {
        boolean b = true;
        File f = new File(sPathName);
        if (f.exists() == false) {
            try {
                FileOutputStream fos = new FileOutputStream(sPathName);
                fos.close();
            } catch (Exception exc) {
                log("*** ProxyUtil::createEmptyFile(): error creating local " + sPathName + ": " + exc);
                b = false;
            }
        }
        return b;
    }

    public static void replicateDirectory(final String sUsrDir, final File d) {
        String sPathName = d.getAbsolutePath();
        String sUserAcc = ServiceFactory.getInstance().getUserAccount();
        int n = sPathName.indexOf(sUserAcc);
        if (n > 0) {
            n += sUserAcc.length() + 1;
            File f = new File(sUsrDir + File.separator + sPathName.substring(n));
            if (f.exists() == false) {
                if (DirUtil.createDirIfNeeded(f.getAbsolutePath()) == false) {
                    log("*** ProxyUtil::replicateDirectory(): error creating local dir " + f.getAbsolutePath());
                }
            }
        }
    }

    private static boolean isHidden(final File f) {
        assert f != null;
        return f.getName().startsWith(".");
    }

    public static ArrayList<File> scanDir(final String sPath) {
        ArrayList<File> lst = null;
        File fDoc = new File(sPath);
        if (fDoc.isDirectory() && fDoc.canRead()) {
            File[] fa = fDoc.listFiles();
            if (fa != null && fa.length > 0) {
                lst = new ArrayList<File>();
                for (File f : fa) {
                    if (!isHidden(f)) {
                        lst.add(f);
                    }
                }
            }
        }
        return lst;
    }

    public static boolean downloadImage(final String sFileLocal, final String sFileRemote) {
        boolean b = true;
        try {
            URL url = new URL(sFileRemote);
            URLConnection conn = url.openConnection();
            conn.setDoInput(true);
            conn.setUseCaches(false);
            InputStream is = conn.getInputStream();
            FileOutputStream os = new FileOutputStream(sFileLocal);
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
            is.close();
            os.close();
        } catch (Exception exc) {
            exc.printStackTrace();
            b = false;
        }
        return b;
    }

    public static String getUserDocsDir() {
        ServiceOptions so = ServiceFactory.getInstance().getOptions();
        return (so != null ? so.getDocsDir() : ServiceFactory.getInstance().getUserHome());
    }

    public static String getUserImgsDir() {
        ServiceOptions so = ServiceFactory.getInstance().getOptions();
        return (so != null ? so.getImgsDir() : ServiceFactory.getInstance().getUserHome());
    }

    public static String getDateAndTime() {
        Calendar c = new GregorianCalendar();
        return (c.get(Calendar.YEAR) + "-" + c.get(Calendar.MONTH) + "-" + c.get(Calendar.DAY_OF_MONTH) + "-" + c.get(Calendar.HOUR_OF_DAY) + "-" + c.get(Calendar.MINUTE) + "-" + c.get(Calendar.MILLISECOND));
    }

    public static final String[] saOpenImgFmts = { ".bi", ".bmp", ".dcm", ".dic", ".gsi", ".img", ".jpg", ".nii", ".png" };

    public static final String saOpenImgFmtsDesc = "Image files (*.bi, *.bmp, *.dcm, *.dic, *.gsi, *.img, *.jpg, *.nii, *.png)";

    public static final String[] saSaveImgFmts = { "bmp", "gif", "jpg", "jpeg", "png" };

    public static final String saSaveImgFmtsDesc[] = { "Bitmap format (*.bmp)", "GIF format (*.gif)", "JPG format (*.jpg)", "JPG format (*.jpeg)", "PNG format (*.png)" };

    public static final String USR_SVR_DIR = "user";
}
