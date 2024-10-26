package edu.xtec.jclic.fileSystem;

import edu.xtec.jclic.misc.Utils;
import edu.xtec.util.*;
import edu.xtec.util.Options;
import java.io.*;
import java.net.*;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Component;
import javax.swing.JFileChooser;
import edu.xtec.util.ResourceBridge;
import java.util.Vector;

/** Base class for Clic filesystems.
 * @author Francesc Busquets (fbusquets@xtec.net)
 * @version 0.1
 */
public class FileSystem extends Object {

    public static final String FS = "/", WINFS = "\\";

    public static final char FSCH = '/', WINFSCH = '\\';

    protected static FileChooserForFiles fileChooser;

    private static final String FS_BAK = FS + "..";

    public static java.util.HashMap altFileNames = new java.util.HashMap();

    public String root;

    protected boolean isURL = false;

    protected Boolean ISURL = new Boolean(isURL);

    protected ResourceBridge rb = null;

    public FileSystem(ResourceBridge rb) {
        root = new String("");
        this.rb = rb;
    }

    public FileSystem(String rootPath, ResourceBridge rb) {
        root = stdFn(rootPath);
        this.rb = rb;
        if (root == null) root = new String("");
        if (root.length() > 0) {
            if (isStrUrl(root)) {
                isURL = true;
                ISURL = new Boolean(isURL);
                if (!root.endsWith(FS)) root = root + FS;
                root = getCanonicalNameOf(root);
            } else {
                File f = new File(sysFn(root));
                String saveRoot = new String(root);
                try {
                    root = stdFn(f.getCanonicalPath());
                } catch (Exception e) {
                    root = saveRoot;
                }
                if (!root.endsWith(FS)) root = root + FS;
            }
        }
    }

    public FileSystem duplicate() throws Exception {
        return createFileSystem(root, rb);
    }

    protected void changeBase(String newRoot, String newFileName) throws Exception {
        File f = new File(sysFn(newRoot));
        String saveRoot = new String(root);
        try {
            root = stdFn(f.getCanonicalPath());
        } catch (Exception e) {
            root = saveRoot;
        }
        if (!root.endsWith(FS)) root = root + FS;
    }

    public static String stdFn(String s) {
        return s == null ? s : s.replace(WINFSCH, FSCH);
    }

    public static String sysFn(String s) {
        String result = s;
        if (result != null) {
            result = stdFn(result).replace(FSCH, File.separatorChar);
            if (result.indexOf("%20") >= 0) result = StrUtils.replace(result, "%20", " ");
        }
        return result;
    }

    public static final FileSystem createFileSystem(String rootPath, String fileName, ResourceBridge rb) throws Exception {
        if (fileName == null) return new FileSystem(rootPath, rb); else if (fileName.endsWith(".pcc")) return PCCFileSystem.createPCCFileSystem(rootPath, fileName, rb); else if (fileName.endsWith(".zip")) return ZipFileSystem.createZipFileSystem(rootPath, fileName, rb); else throw new Exception("unknown format " + fileName);
    }

    public static final FileSystem createFileSystem(String fullPath, ResourceBridge rb) throws Exception {
        fullPath = getCanonicalNameOf(fullPath, null);
        String fileName = null;
        String rootPath = getPathPartOf(fullPath);
        if (fullPath.endsWith(".pcc") || fullPath.endsWith(".zip")) {
            fileName = getFileNameOf(fullPath);
        }
        return createFileSystem(rootPath, fileName, rb);
    }

    public String getFullFileNamePath(String fName) {
        if (fName == null || fName.length() == 0) {
            return root.length() > 0 ? root.substring(0, root.length() - 1) : root;
        }
        String result = getCanonicalNameOf(fName);
        if (!isURL) {
            File f = new File(sysFn(result));
            if (!f.isAbsolute()) result = getCanonicalNameOf(root + result);
        } else {
            if (!isStrUrl(result)) result = getCanonicalNameOf(root + result);
        }
        return result;
    }

    public String getRelativeFileNamePath(String fName) {
        String s = stdFn(fName);
        if (s == null || s.length() < root.length() || !s.substring(0, root.length()).equalsIgnoreCase(root)) return s; else return s.substring(root.length());
    }

    public String getFullRoot() {
        return root;
    }

    public boolean isUrlBased() {
        return isURL;
    }

    public static final boolean isStrUrl(String s) {
        return s != null && (s.startsWith("http:") || s.startsWith("ftp:") || s.startsWith("mailto:"));
    }

    public String getUrl(String fileName) {
        String s = stdFn(fileName);
        if (s == null || isStrUrl(s) || s.startsWith("file:")) return s;
        if (!(s.charAt(1) == ':') && !s.startsWith(FS)) s = getFullFileNamePath(s);
        if (isURL) return getCanonicalNameOf(s, ISURL); else return "file://" + sysFn(getCanonicalNameOf(s, ISURL));
    }

    public String getCanonicalNameOf(String fileName) {
        return getCanonicalNameOf(fileName, ISURL);
    }

    public static String getCanonicalNameOf(String fileName, Boolean isUrl) {
        String fn = stdFn(fileName);
        boolean flagUrl = (isUrl != null ? isUrl.booleanValue() : isStrUrl(fn));
        String prefix = "";
        int cut = -1;
        if (fn.startsWith("file:")) {
            fn = fn.substring(5);
        }
        if (isStrUrl(fn)) {
            int k = fn.indexOf('@');
            if (k < 0) k = 7;
            cut = fn.indexOf(FSCH, k);
        } else if (fn.length() > 2 && fn.charAt(1) == ':') {
            cut = (fn.charAt(2) == FSCH ? 2 : 1);
        } else if (fn.startsWith("//")) {
            int i = fn.indexOf(FSCH, 2);
            cut = fn.indexOf(FSCH, i + 1);
        } else if (fn.startsWith(FS)) {
            cut = 0;
        }
        if (cut >= 0) {
            prefix = fn.substring(0, cut + 1);
            fn = fn.substring(cut + 1);
        }
        int r;
        while ((r = fn.indexOf(FS_BAK)) >= 0) {
            int p;
            for (p = r - 1; p >= 0; p--) if (fn.charAt(p) == FSCH) break;
            StringBuffer newfn = new StringBuffer();
            if (p >= 0) newfn.append(fn.substring(0, p + 1));
            if (r + 4 < fn.length()) newfn.append(fn.substring(r + 4));
            fn = newfn.substring(0);
        }
        return prefix + fn;
    }

    public static String getPathPartOf(String fullPath) {
        String s = stdFn(fullPath);
        int i = s.lastIndexOf(FS);
        return i < 0 ? "" : s.substring(0, i + 1);
    }

    public static String getFileNameOf(String fullPath) {
        String s = stdFn(fullPath);
        int i = s.lastIndexOf(FS);
        return i < 0 ? s : s.substring(i + 1);
    }

    public byte[] getBytes(String fileName) throws IOException {
        return StreamIO.readInputStream(getInputStream(fileName));
    }

    public Image getImageFile(String fName) throws Exception {
        return Toolkit.getDefaultToolkit().createImage(getBytes(fName));
    }

    public long getFileLength(String fName) throws IOException {
        long length = 0;
        if (isURL) {
            URL url = new URL(getFullFileNamePath(fName));
            URLConnection c = url.openConnection();
            length = c.getContentLength();
        } else {
            File f = new File(sysFn(getFullFileNamePath(fName)));
            length = f.length();
        }
        return length;
    }

    public boolean fileExists(String fName) {
        boolean result = false;
        try {
            if (isURL) {
                URL url = new URL(getFullFileNamePath(fName));
                URLConnection c = url.openConnection();
                result = (c.getContentLength() > 0);
            } else {
                File f = new File(sysFn(getFullFileNamePath(fName)));
                result = f.exists();
            }
        } catch (Exception ex) {
            result = false;
        }
        return result;
    }

    public InputStream getInputStream(String fName) throws IOException {
        InputStream result = null;
        int length = 0;
        if (isURL) {
            URL url = new URL(getFullFileNamePath(fName));
            URLConnection c = url.openConnection();
            length = c.getContentLength();
            result = c.getInputStream();
        } else {
            File f = new File(sysFn(getFullFileNamePath(fName)));
            if (!f.exists()) {
                String alt = (String) altFileNames.get(fName);
                if (alt != null) f = new File(sysFn(getFullFileNamePath(alt)));
            }
            length = (int) f.length();
            result = new FileInputStream(f);
        }
        if (result != null && rb != null) {
            result = rb.getProgressInputStream(result, length, fName);
        }
        return result;
    }

    public Object getMediaDataSource(String fName) throws Exception {
        if (isURL) return getExtendedByteArrayInputStream(fName);
        return new StringBuffer("file:").append(getFullFileNamePath(fName)).substring(0);
    }

    public edu.xtec.util.ExtendedByteArrayInputStream getExtendedByteArrayInputStream(String fName) throws Exception {
        return new edu.xtec.util.ExtendedByteArrayInputStream(getBytes(fName), fName);
    }

    public static org.jdom.Document getXMLDocument(InputStream is) throws Exception {
        org.jdom.Document doc = JDomUtility.getSAXBuilder().build(is);
        edu.xtec.util.JDomUtility.clearNewLineElements(doc.getRootElement());
        return doc;
    }

    public org.jdom.Document getXMLDocument(String fName) throws Exception {
        org.jdom.Document doc = buildDoc(fName, JDomUtility.getSAXBuilder());
        edu.xtec.util.JDomUtility.clearNewLineElements(doc.getRootElement());
        return doc;
    }

    protected org.jdom.Document buildDoc(String fName, org.jdom.input.SAXBuilder builder) throws Exception {
        return builder.build(getInputStream(fName));
    }

    public void close() {
    }

    protected void open() throws Exception {
    }

    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public static FileChooserForFiles getFileChooser(String root) {
        if (fileChooser == null) {
            fileChooser = new FileChooserForFiles();
            if (root != null) {
                fileChooser.setCurrentDirectory(new File(sysFn(root)));
            }
        }
        return fileChooser;
    }

    public String chooseFile(String defaultValue, boolean save, int[] filters, Options options, String titleKey, Component dlgOwner, boolean proposeMove) {
        String result = null;
        String[] files = chooseFiles(defaultValue, save, filters, options, titleKey, dlgOwner, proposeMove, false);
        if (files != null && files.length > 0) result = files[0];
        return result;
    }

    public String[] chooseFiles(String defaultValue, boolean save, int[] filters, Options options, String titleKey, Component dlgOwner, boolean proposeMove, boolean multiSelection) {
        String[] result = null;
        FileChooserForFiles chooser;
        if (save) multiSelection = false;
        if (options != null) {
            Messages msg = options.getMessages();
            if (isURL) {
                if (save) {
                    msg.showErrorWarning(dlgOwner, "filesystem_saveURLerror", null);
                } else {
                    String s = msg.showInputDlg(dlgOwner, "filesystem_enterURL", "URL", "http://", titleKey != null ? titleKey : "filesystem_openURL", false);
                    if (s != null) result = new String[] { s };
                }
            } else if ((chooser = getFileChooser(root)) != null) {
                chooser.setApproveButtonToolTipText(msg.get(save ? "FILE_SAVE_TOOLTIP" : "FILE_OPEN_TOOLTIP"));
                chooser.setDialogType(save ? JFileChooser.SAVE_DIALOG : JFileChooser.OPEN_DIALOG);
                chooser.setApproveButtonText(msg.get(save ? "SAVE" : "OPEN"));
                chooser.setMultiSelectionEnabled(multiSelection);
                chooser.setDialogTitle(msg.get(titleKey != null ? titleKey : save ? "FILE_SAVE" : "FILE_OPEN"));
                chooser.resetChoosableFileFilters();
                if (filters != null) {
                    chooser.setAcceptAllFileFilterUsed(false);
                    for (int i = 0; i < filters.length; i++) {
                        if (i == filters.length - 1) chooser.setFileFilter(Utils.getFileFilter(filters[i], msg)); else chooser.addChoosableFileFilter(Utils.getFileFilter(filters[i], msg));
                    }
                } else chooser.setAcceptAllFileFilterUsed(true);
                String s = StrUtils.nullableString(defaultValue);
                boolean dummyFile = false;
                if (s == null) {
                    s = ".";
                    dummyFile = true;
                }
                chooser.directSetSelectedFile(new File(sysFn(getFullFileNamePath(s))));
                if (dummyFile) chooser.directSetSelectedFile(null);
                int retVal;
                boolean done = false;
                while (!done) {
                    if (save) retVal = chooser.showSaveDialog(dlgOwner); else retVal = chooser.showOpenDialog(dlgOwner);
                    if (retVal == JFileChooser.APPROVE_OPTION) {
                        File[] files = multiSelection ? files = chooser.getSelectedFiles() : new File[] { chooser.getSelectedFile() };
                        result = new String[files.length];
                        for (int i = 0; i < files.length; i++) {
                            File f = files[i].getAbsoluteFile();
                            result[i] = getRelativeFileNamePath(stdFn(f.getAbsolutePath()));
                            if (save) {
                                javax.swing.filechooser.FileFilter filter = chooser.getFileFilter();
                                if (filter instanceof SimpleFileFilter) {
                                    f = ((SimpleFileFilter) filter).checkFileExtension(f);
                                    result[i] = getRelativeFileNamePath(stdFn(f.getAbsolutePath()));
                                }
                                done = (msg.confirmOverwriteFile(dlgOwner, f, "yn") == Messages.YES);
                            } else {
                                done = msg.confirmReadableFile(dlgOwner, f);
                                if (done && proposeMove && root.length() > 0 && result[i].indexOf(FS) >= 0 && msg.showQuestionDlgObj(dlgOwner, new String[] { msg.get("filesystem_copyToRoot_1") + " " + result[i], msg.get("filesystem_copyToRoot_2"), msg.get("filesystem_copyToRoot_3"), msg.get("filesystem_copyToRoot_4") }, "CONFIRM", "yn") == Messages.YES) {
                                    String name = stdFn(f.getName());
                                    File destFile = new File(sysFn(getFullFileNamePath(name)));
                                    if (msg.confirmOverwriteFile(dlgOwner, destFile, "yn") == Messages.YES) {
                                        try {
                                            OutputStream os = createSecureFileOutputStream(name);
                                            InputStream is = getInputStream(result[i]);
                                            if (StreamIO.writeStreamDlg(is, os, (int) f.length(), msg.get("filesystem_copyFile"), dlgOwner, options)) result[i] = name; else if (destFile.exists()) destFile.delete();
                                        } catch (Exception ex) {
                                            msg.showErrorWarning(dlgOwner, "ERROR", ex);
                                        }
                                    }
                                }
                            }
                            if (!done) {
                                break;
                            }
                        }
                    } else {
                        result = null;
                        done = true;
                    }
                }
            }
        }
        return result;
    }

    class SecureFileOutputStream extends FileOutputStream {

        boolean closed;

        File tempFile;

        File destFile;

        /** Creates new SecureFileOutputStream */
        private SecureFileOutputStream(File tempFile, File destFile) throws FileNotFoundException {
            super(tempFile);
            this.tempFile = tempFile;
            this.destFile = destFile;
            closed = false;
        }

        public void close() throws IOException {
            super.close();
            if (!closed) {
                closed = true;
                if (destFile != null) {
                    boolean isCurrentFs = getFullRoot().equals(stdFn(destFile.getAbsolutePath()));
                    if (isCurrentFs) FileSystem.this.close();
                    if (destFile.exists()) destFile.delete();
                    boolean renamed = tempFile.renameTo(destFile);
                    if (!renamed) {
                        System.err.println("WARNING: Unable to rename " + tempFile + " to " + destFile.getName());
                    }
                    if (isCurrentFs) {
                        try {
                            if (!renamed) {
                                changeBase(tempFile.getParent(), tempFile.getName());
                            }
                            FileSystem.this.open();
                        } catch (Exception ex) {
                            throw new IOException(ex.getMessage());
                        }
                    }
                }
            }
        }
    }

    public FileOutputStream createSecureFileOutputStream(String fileName) throws IOException {
        FileOutputStream result = null;
        File file = new File(sysFn(getFullFileNamePath(fileName)));
        file.getParentFile().mkdirs();
        File tmp = File.createTempFile("tmp", ".tmp", file.getParentFile());
        result = new SecureFileOutputStream(tmp, file);
        return result;
    }

    public static void exploreFiles(String prefix, File f, Vector v, char pathSep, FileFilter filter) {
        File[] files = filter == null ? f.listFiles() : f.listFiles(filter);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < files.length; i++) {
            sb.setLength(0);
            if (files[i].isDirectory()) {
                if (prefix != null) sb.append(prefix);
                sb.append(files[i].getName()).append(pathSep);
                exploreFiles(sb.substring(0), files[i], v, pathSep, filter);
            } else {
                if (prefix != null) sb.append(prefix);
                v.add(sb.append(files[i].getName()).substring(0));
            }
        }
    }

    private static final String validFileChars = "_!~0123456789abcdefghijklmnopqrstuvwxyz";

    private static final String convertibleChars = "áàäâãéèëêíìïîóòöôõúùüûñç€ºªåæøýþÿ";

    private static final String equivalentChars = "aaaaaeeeeiiiiooooouuuunceoaaaoypy";

    public static String getValidFileName(String fn) {
        String result = null;
        if (fn != null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < fn.length(); i++) {
                char ch = Character.toLowerCase(fn.charAt(i));
                if (validFileChars.indexOf(ch) < 0) {
                    int p = convertibleChars.indexOf(ch);
                    if (p >= 0) ch = equivalentChars.charAt(p); else ch = '_';
                }
                sb.append(ch);
            }
            result = sb.substring(0);
        }
        return result;
    }
}
