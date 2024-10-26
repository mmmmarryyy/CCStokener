package com.elibera.ccs.img;

import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessControlException;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import com.elibera.ccs.res.Msg;
import com.elibera.ccs.util.HelperStd;

/**
 * @author meisi
 *
 */
public class HelperOp {

    private static JFileChooser fc;

    private static JFileChooser fcdir;

    /**
	 * öffnet den File Dialog und gibt die Datei zurück
	 * @param filter
	 * @param dialogTitel
	 * @param rootPanel
	 * @return
	 * @throws AccessControlException
	 */
    public static File getFileWithFileChooser(FileFilter filter, String dialogTitel, Component rootPanel) throws AccessControlException {
        if (dialogTitel == null) dialogTitel = Msg.getMsg("FILE_CHOOSER_STD_DIALOG_TITEL");
        if (fc == null) createFileChooser();
        File ret = null;
        if (filter != null) fc.setFileFilter(filter);
        int returnVal = fc.showDialog(rootPanel, dialogTitel);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            ret = fc.getSelectedFile();
        }
        fc.setFileFilter(null);
        System.out.println("getFileWithFileChooser:" + ret);
        return ret;
    }

    public static File getFolderWithFileChooser(String dialogTitel, Component rootPanel) throws AccessControlException {
        if (dialogTitel == null) dialogTitel = Msg.getMsg("FOLDER_CHOOSER_STD_DIALOG_TITEL");
        if (fcdir == null) fcdir = new JFileChooser();
        fcdir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fcdir.setAcceptAllFileFilterUsed(false);
        File ret = null;
        int returnVal = fcdir.showDialog(rootPanel, dialogTitel);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            ret = fcdir.getSelectedFile();
        }
        System.out.println("getFolderWithFileChooser:" + ret);
        return ret;
    }

    public static String removeFileDescitptor(String fileUrl) {
        if (fileUrl.indexOf("file:/") == 0) {
            fileUrl = fileUrl.substring(6);
            while (fileUrl.indexOf("//") == 0) {
                fileUrl = fileUrl.substring(1);
            }
        }
        return fileUrl;
    }

    /**
	 * gibt den absoluten Dateinamen zurück, für das ausgewählte Bild
	 * @param dialogTitel
	 * @param rootPanel
	 * @return
	 * @throws AccessControlException
	 */
    public static String getImageWithFileChoose(String dialogTitel, Component rootPanel) throws AccessControlException {
        if (dialogTitel == null) dialogTitel = Msg.getMsg("DIALOG_IMAGE_UPLOAD_FILE_CHOOSER_TITEL");
        File f = getFileWithFileChooser(new MyFileFilter(new String[] { ".jpeg", ".jpg", ".png", ".tiff" }, "JPEG (*.jpeg), PNG (*.png), TIFF (*.tiff)"), dialogTitel, rootPanel);
        if (f == null) return null;
        return f.getAbsolutePath();
    }

    public static String getAudioWithFileChoose(String dialogTitel, Component rootPanel) throws AccessControlException {
        if (dialogTitel == null) dialogTitel = Msg.getMsg("DIALOG_AUDIO_UPLOAD_FILE_CHOOSER_TITEL");
        File f = getFileWithFileChooser(new MyFileFilter(new String[] { ".wav", ".mp3", ".amr", ".awb", ".wb-amr", ".midi", ".au", ".3gp", ".mp4", ".aac", ".m4a", ".3g2", ".ra", ".rm" }, "WAV (*.wav), MP3 (*.mp3), AMR (*.amr), AWB (*.awb), MIDI (*.midi), AU (*.au), .."), dialogTitel, rootPanel);
        if (f == null) return null;
        return f.getAbsolutePath();
    }

    public static String getVideoWithFileChoose(String dialogTitel, Component rootPanel) throws AccessControlException {
        if (dialogTitel == null) dialogTitel = Msg.getMsg("DIALOG_VIDEO_UPLOAD_FILE_CHOOSER_TITEL");
        File f = getFileWithFileChooser(new MyFileFilter(new String[] { ".mpeg", ".mpg", ".3gp", ".3gpp", ".mp4", ".h263", ".h264", ".3g2", ".ra", ".rm" }, "3GP (*.3gp), MPEG (*.mpeg), MP4 (*.mp4), .."), dialogTitel, rootPanel);
        if (f == null) return null;
        return f.getAbsolutePath();
    }

    public static String getMLOWithFileChoose(String dialogTitel, Component rootPanel) throws AccessControlException {
        if (dialogTitel == null) dialogTitel = Msg.getMsg("DIALOG_LO_FILE_CHOOSER_TITEL");
        File f = getFileWithFileChooser(new MyFileFilter(new String[] { ".zip", ".lo", ".mlo" }, "ZIP (*.zip), LO (*.lo), MLO (*.mlo) .."), dialogTitel, rootPanel);
        if (f == null) return null;
        return f.getAbsolutePath();
    }

    /**
	 * gibt einen FileFilter für Zip-Files zurück
	 * @return
	 */
    public static FileFilter getZipFileFilter() {
        return new FileFilter() {

            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".zip") || f.isDirectory();
            }

            public String getDescription() {
                return "Zip Archiv (*.zip)";
            }
        };
    }

    public static String getHtmlOrTextWithFileChoose(String dialogTitel, Component rootPanel) throws AccessControlException {
        File f = getFileWithFileChooser(new MyFileFilter(new String[] { ".html", ".htm", ".txt" }, "HTML (*.html), TEXT (*.txt)"), dialogTitel, rootPanel);
        if (f == null) return null;
        return f.getAbsolutePath();
    }

    /**
	 * speichert die Datei ab
	 * @param file
	 * @param data
	 * @throws IOException
	 * @throws AccessControlException
	 */
    public static void saveFile(File file, byte[] data) throws IOException, AccessControlException {
        System.out.println("saveFile:" + file.getAbsolutePath());
        if (!file.exists()) file.createNewFile();
        FileOutputStream fout = new FileOutputStream(file);
        fout.write(data);
        fout.close();
    }

    /**
	 * öffnet ein locales Bild und gibt die Byte zurück
	 * @param localFileUrl
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public static byte[] getLocalImageFile(String localFileUrl) throws FileNotFoundException, IOException {
        File fi = HelperOp.openFile(localFileUrl);
        javax.imageio.stream.FileImageInputStream u2 = new javax.imageio.stream.FileImageInputStream(fi);
        byte[] b = new byte[(int) fi.length()];
        u2.readFully(b);
        u2.close();
        System.out.println("getLocalImageFile:" + b.length);
        return b;
    }

    /**
	 * gibt eine lokale Datei zurück
	 * @param localFileUrl
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public static byte[] getLocalFile(String localFileUrl) throws FileNotFoundException, IOException {
        try {
            System.out.println("getLocalFile:" + localFileUrl);
            java.io.File f = new java.io.File(localFileUrl);
            if (f.exists()) {
                FileInputStream fr = new FileInputStream(f);
                byte[] c = new byte[(int) f.length()];
                fr.read(c);
                fr.close();
                return c;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("getLocalFile:" + e.getMessage());
        }
        return null;
    }

    /**
	 * öffnet eine lokale Datei
	 * @param fileEndungen
	 * @param fileEndungenDesc
	 * @param dialogTitel
	 * @param rootPanel
	 * @return
	 * @throws AccessControlException
	 */
    public static File openLocaleFile(String[] fileEndungen, String fileEndungenDesc, String dialogTitel, Component rootPanel) throws AccessControlException {
        try {
            return getFileWithFileChooser(new MyFileFilter(fileEndungen, fileEndungenDesc), dialogTitel, rootPanel);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("openLocaleFile:" + e.getMessage());
        }
        return null;
    }

    public static FileInputStream openLocaleFileInputStream(String[] fileEndungen, String fileEndungenDesc, String dialogTitel, Component rootPanel) throws AccessControlException {
        try {
            java.io.File f = openLocaleFile(fileEndungen, fileEndungenDesc, dialogTitel, rootPanel);
            if (f.exists()) {
                return new FileInputStream(f);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("getLocalFileInputStream:" + e.getMessage());
        }
        return null;
    }

    /**
	 * eigener FileFilter
	 * @author meisi
	 *
	 */
    public static class MyFileFilter extends FileFilter {

        String[] e;

        String d;

        public MyFileFilter(String[] endungen, String desc) {
            e = endungen;
            d = desc;
        }

        public boolean accept(File f) {
            String name = f.getName().toLowerCase();
            for (int i = 0; i < e.length; i++) {
                if (name.endsWith(e[i])) return true;
            }
            return f.isDirectory();
        }

        public String getDescription() {
            return d;
        }
    }

    /**
	 * initiiert den FileChooser
	 * @throws AccessControlException
	 */
    public static void createFileChooser() throws AccessControlException {
        System.out.println("creating FileChooser...");
        fc = new JFileChooser();
        System.out.println("successfully created: FileChooser...");
    }

    /**
	 * öffnet eine lokale Datei
	 * JAR muss signiert sein!!
	 * @param url
	 * @return
	 */
    public static File openFile(String url) {
        try {
            File fi = new File(url);
            return fi;
        } catch (Exception e) {
            System.out.println("openFile Error:" + e.getLocalizedMessage());
        }
        return null;
    }

    /**
	 * lädt eine HTTP Ressource
	 * @param url
	 * @param contentType
	 * @param newUrl
	 * @param encoding
	 * @return
	 * @throws Exception
	 */
    public static byte[] downloadHTTP(String url, StringBuffer contentType, StringBuffer newUrl, StringBuffer encoding) throws Exception {
        if (contentType == null) contentType = new StringBuffer();
        if (newUrl == null) newUrl = new StringBuffer();
        if (encoding == null) encoding = new StringBuffer();
        URL src = new URL(url);
        URLConnection urlConn = src.openConnection();
        urlConn.setReadTimeout(10 * 1000);
        urlConn.setConnectTimeout(10 * 1000);
        urlConn.setDoInput(true);
        urlConn.setDoOutput(false);
        urlConn.setUseCaches(false);
        urlConn.setRequestProperty("User-Agent", "mle|0.8.9|Nokia6680|editor");
        urlConn.setRequestProperty("Accept-Encoding", "UTF-8");
        int len = urlConn.getContentLength();
        BufferedInputStream in = new BufferedInputStream(urlConn.getInputStream());
        contentType.append(urlConn.getContentType());
        String ct = urlConn.getHeaderField("Content-Type");
        newUrl.append(urlConn.getURL().toString());
        if (ct != null && ct.indexOf("charset=") > 0) {
            int a = ct.indexOf("charset=") + "charset=".length();
            encoding.append(ct.substring(a));
        }
        System.out.println("encoding:" + encoding);
        byte[] data = null;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int b = 0;
        while ((b = in.read()) != -1) {
            bout.write(b);
        }
        data = bout.toByteArray();
        in.close();
        if (encoding.length() <= 0) {
            if (HelperStd.checkUTFEncoding(data) > 0) encoding.append("UTF-8"); else encoding.append("ISO-8859-1");
        }
        System.out.println("encoding:" + encoding);
        return data;
    }

    public static File createTempDirectory() throws IOException {
        final File temp;
        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        temp.deleteOnExit();
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                deleteDir(temp);
            }
        });
        return (temp);
    }

    public static boolean deleteDir(File dir) {
        if (dir == null) return false;
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
}
