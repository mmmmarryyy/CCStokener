package com.jme3.system;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class for extracting the natives (dll, so) from the jars.
 * This class should only be used internally.
 */
public final class Natives {

    private static final Logger logger = Logger.getLogger(Natives.class.getName());

    private static final byte[] buf = new byte[1024];

    private static File extractionDirOverride = null;

    private static File extractionDir = null;

    public static void setExtractionDir(String name) {
        extractionDirOverride = new File(name).getAbsoluteFile();
    }

    public static File getExtractionDir() {
        if (extractionDirOverride != null) {
            return extractionDirOverride;
        }
        if (extractionDir == null) {
            File workingFolder = new File("").getAbsoluteFile();
            if (!workingFolder.canWrite()) {
                setStorageExtractionDir();
            } else {
                try {
                    File file = new File(workingFolder.getAbsolutePath() + File.separator + ".jmetestwrite");
                    file.createNewFile();
                    file.delete();
                    extractionDir = workingFolder;
                } catch (Exception e) {
                    setStorageExtractionDir();
                }
            }
        }
        return extractionDir;
    }

    private static void setStorageExtractionDir() {
        logger.log(Level.WARNING, "Working directory is not writable. Using home directory instead.");
        extractionDir = new File(JmeSystem.getStorageFolder(), "natives_" + Integer.toHexString(computeNativesHash()));
        if (!extractionDir.exists()) {
            extractionDir.mkdir();
        }
    }

    private static int computeNativesHash() {
        try {
            String classpath = System.getProperty("java.class.path");
            URL url = Thread.currentThread().getContextClassLoader().getResource("com/jme3/system/Natives.class");
            StringBuilder sb = new StringBuilder(url.toString());
            if (sb.indexOf("jar:") == 0) {
                sb.delete(0, 4);
                sb.delete(sb.indexOf("!"), sb.length());
                sb.delete(sb.lastIndexOf("/") + 1, sb.length());
            }
            try {
                url = new URL(sb.toString());
            } catch (MalformedURLException ex) {
                throw new UnsupportedOperationException(ex);
            }
            URLConnection conn = url.openConnection();
            int hash = classpath.hashCode() ^ (int) conn.getLastModified();
            return hash;
        } catch (IOException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

    public static void extractNativeLib(String sysName, String name) throws IOException {
        extractNativeLib(sysName, name, false, true);
    }

    public static void extractNativeLib(String sysName, String name, boolean load) throws IOException {
        extractNativeLib(sysName, name, load, true);
    }

    public static void extractNativeLib(String sysName, String name, boolean load, boolean warning) throws IOException {
        String fullname = System.mapLibraryName(name);
        String path = "native/" + sysName + "/" + fullname;
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        if (url == null) {
            if (!warning) {
                logger.log(Level.WARNING, "Cannot locate native library: {0}/{1}", new String[] { sysName, fullname });
            }
            return;
        }
        URLConnection conn = url.openConnection();
        InputStream in = conn.getInputStream();
        File targetFile = new File(getExtractionDir(), fullname);
        OutputStream out = null;
        try {
            if (targetFile.exists()) {
                long targetLastModified = targetFile.lastModified();
                long sourceLastModified = conn.getLastModified();
                if (targetLastModified + 1000 > sourceLastModified) {
                    logger.log(Level.FINE, "Not copying library {0}. Latest already extracted.", fullname);
                    return;
                }
            }
            out = new FileOutputStream(targetFile);
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            in = null;
            out.close();
            out = null;
            targetFile.setLastModified(conn.getLastModified());
        } catch (FileNotFoundException ex) {
            if (ex.getMessage().contains("used by another process")) {
                return;
            }
            throw ex;
        } finally {
            if (load) {
                System.load(targetFile.getAbsolutePath());
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
        logger.log(Level.FINE, "Copied {0} to {1}", new Object[] { fullname, targetFile });
    }

    protected static boolean isUsingNativeBullet() {
        try {
            Class clazz = Class.forName("com.jme3.bullet.util.NativeMeshUtil");
            return clazz != null;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    public static void extractNativeLibs(Platform platform, AppSettings settings) throws IOException {
        String renderer = settings.getRenderer();
        String audioRenderer = settings.getAudioRenderer();
        boolean needLWJGL = false;
        boolean needOAL = false;
        boolean needJInput = false;
        boolean needNativeBullet = isUsingNativeBullet();
        if (renderer != null) {
            if (renderer.startsWith("LWJGL")) {
                needLWJGL = true;
            }
        }
        if (audioRenderer != null) {
            if (audioRenderer.equals("LWJGL")) {
                needLWJGL = true;
                needOAL = true;
            }
        }
        needJInput = settings.useJoysticks();
        String libraryPath = getExtractionDir().toString();
        if (needLWJGL) {
            logger.log(Level.INFO, "Extraction Directory: {0}", getExtractionDir().toString());
            System.setProperty("org.lwjgl.librarypath", libraryPath);
        }
        if (needJInput) {
            System.setProperty("net.java.games.input.librarypath", libraryPath);
        }
        switch(platform) {
            case Windows64:
                if (needLWJGL) {
                    extractNativeLib("windows", "lwjgl64");
                }
                if (needOAL) {
                    extractNativeLib("windows", "OpenAL64");
                }
                if (needJInput) {
                    extractNativeLib("windows", "jinput-dx8_64");
                    extractNativeLib("windows", "jinput-raw_64");
                }
                if (needNativeBullet) {
                    extractNativeLib("windows", "bulletjme64", true, false);
                }
                break;
            case Windows32:
                if (needLWJGL) {
                    extractNativeLib("windows", "lwjgl");
                }
                if (needOAL) {
                    extractNativeLib("windows", "OpenAL32");
                }
                if (needJInput) {
                    extractNativeLib("windows", "jinput-dx8");
                    extractNativeLib("windows", "jinput-raw");
                }
                if (needNativeBullet) {
                    extractNativeLib("windows", "bulletjme", true, false);
                }
                break;
            case Linux64:
                if (needLWJGL) {
                    extractNativeLib("linux", "lwjgl64");
                }
                if (needJInput) {
                    extractNativeLib("linux", "jinput-linux64");
                }
                if (needOAL) {
                    extractNativeLib("linux", "openal64");
                }
                if (needNativeBullet) {
                    extractNativeLib("linux", "bulletjme64", true, false);
                }
                break;
            case Linux32:
                if (needLWJGL) {
                    extractNativeLib("linux", "lwjgl");
                }
                if (needJInput) {
                    extractNativeLib("linux", "jinput-linux");
                }
                if (needOAL) {
                    extractNativeLib("linux", "openal");
                }
                if (needNativeBullet) {
                    extractNativeLib("linux", "bulletjme", true, false);
                }
                break;
            case MacOSX_PPC32:
            case MacOSX32:
            case MacOSX_PPC64:
            case MacOSX64:
                if (needLWJGL) {
                    extractNativeLib("macosx", "lwjgl");
                }
                if (needJInput) {
                    extractNativeLib("macosx", "jinput-osx");
                }
                if (needNativeBullet) {
                    extractNativeLib("macosx", "bulletjme", true, false);
                }
                break;
        }
    }
}
