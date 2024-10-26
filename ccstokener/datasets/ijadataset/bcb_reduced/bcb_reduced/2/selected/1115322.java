package de.miethxml.toolkit.plugins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author <a href="mailto:simon.mieth@gmx.de">Simon Mieth </a>
 *
 */
public class PluginInstallerImpl implements PluginInstaller {

    private PluginManager manager;

    private String installDir = "";

    private String currentDir = "";

    public void installPlugin(URL url) {
        try {
            URLConnection conn = url.openConnection();
            InputStream in = conn.getInputStream();
            installPlugin(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void installPlugin(InputStream in) {
        try {
            ZipInputStream zip = new ZipInputStream(in);
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                mkDir(installDir, entry.getName());
                if (!entry.isDirectory()) {
                    storeFile(installDir + File.separator + entry.getName(), zip);
                }
                zip.closeEntry();
            }
            zip.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mkDir(String root, String path) {
        File f = new File(root + File.separator + path);
        if (f.isDirectory()) {
            f.mkdirs();
        } else if (f.isFile()) {
            f.getParentFile().mkdirs();
        }
    }

    private void storeFile(String filename, ZipInputStream in) {
        try {
            FileOutputStream out = new FileOutputStream(filename);
            byte[] buffer = new byte[1028];
            int count = 0;
            int length = 0;
            while ((length = in.read(buffer, 0, buffer.length)) > -1) {
                out.write(buffer, 0, length);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void setPluginManager(PluginManager manager) {
        this.manager = manager;
        this.installDir = (new File(manager.getPluginInstallLocation())).getAbsolutePath();
    }
}
