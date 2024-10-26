package com.cronopista.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import com.cronopista.lightpacker.Main;
import com.cronopista.lightpacker.GUI.ProgressMonitor;
import com.cronopista.lightpacker.steps.Installer;

/**
 * @author Eduardo Rodr�guez
 * 
 */
public class FileUtils {

    public static boolean unzip(File from, File to, ProgressMonitor pm) {
        Enumeration entries;
        ZipFile zipFile;
        try {
            zipFile = new ZipFile(from);
            entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                File destFile = new File(to, entry.getName());
                File destinationParent = destFile.getParentFile();
                destinationParent.mkdirs();
                copyInputStream(zipFile.getInputStream(entry), new FileOutputStream(destFile), pm);
            }
            zipFile.close();
        } catch (Exception ioe) {
            Installer.getInstance().getLogger().log(StringUtils.getStackTrace(ioe));
            return false;
        }
        return true;
    }

    public static final void copyInputStream(InputStream in, OutputStream out, ProgressMonitor pm) throws Exception {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
            if (pm != null) pm.addToProgress(len);
        }
        in.close();
        out.close();
    }

    public static int countFiles(File path) {
        int res = 0;
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    res += countFiles(files[i]);
                } else {
                    res++;
                }
            }
        }
        return res;
    }

    public static boolean deleteDirectory(File path, ProgressMonitor pm, int progress) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i], pm, progress);
                } else {
                    if (pm != null) {
                        pm.addMessage(Main.getInstance().translate("deleting.description", "filename", files[i].getAbsolutePath()), true);
                    }
                    files[i].delete();
                    if (pm != null) {
                        pm.addToProgress(progress);
                    }
                }
            }
        }
        return (path.delete());
    }

    public static boolean dumpFile(String from, File to, String lineBreak) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(from)));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(to)));
            String line = null;
            while ((line = in.readLine()) != null) out.write(Main.getInstance().resolve(line) + lineBreak);
            in.close();
            out.close();
        } catch (Exception e) {
            Installer.getInstance().getLogger().log(StringUtils.getStackTrace(e));
            return false;
        }
        return true;
    }

    public static boolean downloadFile(String from, String to, ProgressMonitor pm) {
        try {
            FileOutputStream out = new FileOutputStream(to);
            URL url = new URL(from);
            URLConnection conn = url.openConnection();
            InputStream in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                if (pm != null) pm.addToProgress(read);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            Installer.getInstance().getLogger().log(StringUtils.getStackTrace(e));
            return false;
        }
        return true;
    }

    public static void zipDir(File zipDir, ZipOutputStream zos, String path, ProgressMonitor pm, Map filter) {
        try {
            String[] dirList = zipDir.list();
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    if (filter != null && filter.get(f.getAbsolutePath()) == null) zipDir(f, zos, path + f.getName() + "/", pm, filter);
                } else {
                    FileInputStream fis = new FileInputStream(f);
                    ZipEntry anEntry = new ZipEntry(path + f.getName());
                    zos.putNextEntry(anEntry);
                    while ((bytesIn = fis.read(readBuffer)) != -1) {
                        zos.write(readBuffer, 0, bytesIn);
                        if (pm != null) pm.addToProgress(bytesIn);
                    }
                    fis.close();
                }
            }
        } catch (Exception e) {
            Installer.getInstance().getLogger().log(StringUtils.getStackTrace(e));
        }
    }

    public static File createTempDirectory() throws IOException {
        final File temp;
        temp = File.createTempFile("temp", Long.toString(System.currentTimeMillis()));
        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return (temp);
    }
}
