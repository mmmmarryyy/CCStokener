package net.sf.borg.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * Abstracts the I/O subsystem. Also allows applets to perform I/O using our
 * in-memory scheme.
 * 
 * @author Mohan Embar
 */
public class IOHelper {

    public static InputStream fileOpen(String startDirectory, String title) throws Exception {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(startDirectory));
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal != JFileChooser.APPROVE_OPTION) return null;
        String s = chooser.getSelectedFile().getAbsolutePath();
        return new FileInputStream(s);
    }

    public static void fileSave(String startDirectory, InputStream istr, String defaultFilename) throws Exception {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(startDirectory));
        chooser.setDialogTitle(Resource.getPlainResourceString("Save"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal != JFileChooser.APPROVE_OPTION) return;
        String s = chooser.getSelectedFile().getAbsolutePath();
        FileOutputStream ostr = new FileOutputStream(s);
        int b;
        while ((b = istr.read()) != -1) ostr.write(b);
        istr.close();
        ostr.close();
    }

    public static InputStream openStream(String file) throws Exception {
        return new FileInputStream(file);
    }

    public static OutputStream createOutputStream(URL url) throws Exception {
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        return connection.getOutputStream();
    }

    public static OutputStream createOutputStream(String file) throws Exception {
        File fil = new File(file);
        fil.getParentFile().mkdirs();
        return new FileOutputStream(fil);
    }

    private IOHelper() {
    }

    public static boolean checkOverwrite(String fname) {
        File f = new File(fname);
        if (!f.exists()) return true;
        int ret = JOptionPane.showConfirmDialog(null, net.sf.borg.common.Resource.getResourceString("overwrite_warning") + fname + " ?", "confirm_overwrite", JOptionPane.OK_CANCEL_OPTION);
        if (ret != JOptionPane.OK_OPTION) return false;
        return (true);
    }
}
