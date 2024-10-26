package com.gorillalogic.config;

import java.io.File;
import java.io.IOException;
import org.apache.log4j.*;
import com.gorillalogic.util.IOUtil;

/** finds gorilla.home 
 *  if it cannot be found an attempt is made to create a usable one
 *  if no config is done, the most likely place for this to happen is as 
 *  a subdir of the user's home directory
 */
public class GorillaHome {

    private static File _gorillaHome = null;

    static Logger logger = Logger.getLogger(GorillaHome.class);

    public static File initGorillaHome() throws GorillaHomeException {
        _gorillaHome = forceGorillaHome();
        return _gorillaHome;
    }

    private static File forceGorillaHome() throws GorillaHomeException {
        File homeFile = null;
        String homeString = System.getProperty(Preferences.GORILLA_HOME, null);
        if (homeString != null && homeString.length() > 0) {
            homeFile = new File(homeString);
            if (hasGorillaHomeContent(homeFile) || tryExpandGorillaHome(homeFile)) {
                return homeFile;
            }
        }
        String pwd = System.getProperty("user.dir");
        homeFile = findGXEInPath(pwd);
        if (homeFile != null) {
            return homeFile;
        }
        String bootDir = null;
        try {
            bootDir = IOUtil.getClasspathDir(GorillaHome.class).getPath();
        } catch (Exception e) {
            throw new GorillaHomeException("cannot find gorilla.home resources relative to class " + GorillaHome.class.getName());
        }
        homeFile = new File(bootDir + "/resource_defaults/GORILLA_HOME");
        if (hasGorillaHomeContent(homeFile)) {
            return homeFile;
        }
        homeFile = new File(System.getProperty("user.home") + "/gorilla.home");
        if (hasGorillaHomeContent(homeFile) || tryExpandGorillaHome(homeFile)) {
            return homeFile;
        }
        homeFile = new File(System.getProperty("user.dir") + "/.gorilla.home");
        if (hasGorillaHomeContent(homeFile) || tryExpandGorillaHome(homeFile)) {
            return homeFile;
        }
        homeFile = new File(bootDir + "/.gorilla.home");
        if (hasGorillaHomeContent(homeFile) || tryExpandGorillaHome(homeFile)) {
            return homeFile;
        }
        homeFile = new File(System.getProperty("java.io.tmpdir") + "/.gorilla.home");
        if (hasGorillaHomeContent(homeFile) || tryExpandGorillaHome(homeFile)) {
            return homeFile;
        }
        return homeFile;
    }

    private static boolean hasGorillaHomeContent(File f) {
        if (f.exists() && f.isDirectory()) {
            File f2 = new File(f.getPath() + "/gorilla.gxe");
            if (f2.exists()) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryExpandGorillaHome(File f) throws GorillaHomeException {
        if (f.exists()) {
            if (!f.isDirectory() || !f.canWrite()) {
                return false;
            }
        } else {
            boolean dirOK = f.mkdirs();
        }
        if (f.exists() && f.isDirectory() && f.canWrite()) {
            java.net.URL url = GorillaHome.class.getResource("/resource_defaults/GORILLA_HOME");
            if (url == null) {
                throw new GorillaHomeException("cannot find gorilla.home resources relative to class " + GorillaHome.class.getName());
            }
            java.net.URLConnection conn;
            try {
                conn = url.openConnection();
            } catch (IOException e) {
                String msg = "Error opening connection to " + url.toString();
                logger.error(msg, e);
                throw new GorillaHomeException("Error copying " + url.toString(), e);
            }
            if (conn == null) {
                throw new GorillaHomeException("cannot find gorilla.home resources relative to class " + GorillaHome.class.getName());
            }
            if (conn instanceof java.net.JarURLConnection) {
                logger.debug("Expanding gorilla.home from from jar file via url " + url.toString());
                try {
                    IOUtil.expandJar((java.net.JarURLConnection) conn, f);
                    return true;
                } catch (Exception e) {
                    throw new GorillaHomeException("Error expanding gorilla.home" + " from jar file at " + conn.getURL().toString() + ": " + e.getMessage());
                }
            } else {
                try {
                    IOUtil.copyDir(new File(url.getFile()), f);
                    return true;
                } catch (Exception e) {
                    throw new GorillaHomeException("Error expanding gorilla.home" + " from file at " + conn.getURL().toString() + ": " + e.getMessage());
                }
            }
        }
        return false;
    }

    private static File findGXEInPath(String pwd) {
        File file = new File(pwd);
        while (file != null && file.exists()) {
            if (hasGorillaHomeContent(file)) {
                return file;
            }
            file = file.getParentFile();
        }
        return null;
    }
}
