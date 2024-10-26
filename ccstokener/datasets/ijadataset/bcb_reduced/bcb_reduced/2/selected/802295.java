package net.dzzd.extension.jogl;

import java.applet.Applet;
import java.applet.AppletStub;
import java.applet.AppletContext;
import java.io.*;
import java.net.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.jar.*;
import java.security.PrivilegedAction;
import java.security.AccessController;
import javax.media.opengl.*;
import net.dzzd.extension.loader.IExtension;
import net.dzzd.extension.loader.IExtensionLoader;
import net.dzzd.access.IProgressListener;

public class JOGLLoader implements IExtension {

    /**
	 * This is a generic method called by an ExtensionLoader using an alternate SecurityManager
	 *   allowing extension requiering pecial right to be load in a "generic" way
	 * <br><br>
	 *  NB: that should be the only one public method in an extension loader.
	 * <br><br>
	 * @param args this extension loader parameter
	 * @param pl listener interface to survey extension loading status  
	 */
    public void load(String baseURL, String localDirectory, IProgressListener pl, IExtensionLoader loader) throws Exception {
        pl.setProgress(0);
        pl.setFinished(false);
        pl.setError(false);
        URL downloadURL = null;
        String installDirectory = null;
        try {
            downloadURL = new URL(baseURL);
            installDirectory = System.getProperty("user.home") + File.separator + localDirectory;
        } catch (Exception e) {
            pl.setProgress(100);
            pl.setFinished(true);
            pl.setError(true);
            throw new Exception("JOGLLoader: Invalid parameter: " + e.toString());
        }
        loader.loadJar(localDirectory, baseURL, "jogl.jar", pl);
        loader.loadJar(localDirectory, baseURL, "gluegen-rt.jar", pl);
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        if (!checkOSAndArch(osName, osArch)) {
            pl.setProgress(100);
            pl.setFinished(true);
            pl.setError(true);
            throw new Exception("Init JOGL failed : Unsupported os / arch ( " + osName + " / " + osArch + " )");
        }
        File installDir = new File(installDirectory);
        if (!installDir.exists()) {
            if (!installDir.mkdirs()) {
                pl.setProgress(100);
                pl.setFinished(true);
                pl.setError(true);
                throw new Exception("Cannot create install directory: " + installDirectory);
            }
        }
        try {
            Class alClass = Class.forName("net.java.games.joal.AL", false, this.getClass().getClassLoader());
            haveJOAL = true;
        } catch (Exception e) {
        }
        String[] nativeJarNames = new String[] { nativeLibInfo.formatNativeJarName("jogl-natives-{0}.jar"), nativeLibInfo.formatNativeJarName("gluegen-rt-natives-{0}.jar"), (haveJOAL ? nativeLibInfo.formatNativeJarName("joal-natives-{0}.jar") : null) };
        for (int n = 0; n < nativeJarNames.length; n++) {
            String nativeJarName = nativeJarNames[n];
            if (nativeJarName == null) continue;
            URL nativeLibURL;
            URLConnection urlConnection;
            String path = downloadURL.toExternalForm() + nativeJarName;
            nativeLibURL = new URL(path);
            urlConnection = nativeLibURL.openConnection();
            File localJarFile = new File(installDir, nativeJarName);
            saveNativesJarLocally(localJarFile, urlConnection);
            JarFile jf = new JarFile(localJarFile);
            if (!findNativeEntries(jf)) {
                pl.setProgress(100);
                pl.setFinished(true);
                pl.setError(true);
                throw new Exception("Native libraries not found in jar file");
            }
            byte[] buf = new byte[8192];
            for (int i = 0; i < nativeLibNames.length; i++) {
                if (!installFile(installDir, jf, nativeLibNames[i], buf)) {
                    pl.setProgress(100);
                    pl.setFinished(true);
                    pl.setError(true);
                    throw new Exception("Cannot install file " + jf.toString() + " in: " + installDir);
                }
            }
            jf.close();
            localJarFile.delete();
        }
        loadNatives(installDir);
        joglLoaded = true;
        pl.setProgress(100);
        pl.setFinished(true);
        pl.setError(false);
    }

    private static class NativeLibInfo {

        private String osName;

        private String osArch;

        private String osNameAndArchPair;

        private String nativePrefix;

        private String nativeSuffix;

        public NativeLibInfo(String osName, String osArch, String osNameAndArchPair, String nativePrefix, String nativeSuffix) {
            this.osName = osName;
            this.osArch = osArch;
            this.osNameAndArchPair = osNameAndArchPair;
            this.nativePrefix = nativePrefix;
            this.nativeSuffix = nativeSuffix;
        }

        public boolean matchesOSAndArch(String osName, String osArch) {
            if (osName.toLowerCase().startsWith(this.osName)) {
                if ((this.osArch == null) || (osArch.toLowerCase().equals(this.osArch))) {
                    return true;
                }
            }
            return false;
        }

        public boolean matchesNativeLib(String fileName) {
            if (fileName.toLowerCase().endsWith(nativeSuffix)) {
                return true;
            }
            return false;
        }

        public String formatNativeJarName(String nativeJarPattern) {
            return MessageFormat.format(nativeJarPattern, new Object[] { osNameAndArchPair });
        }

        public String getNativeLibName(String baseName) {
            return nativePrefix + baseName + nativeSuffix;
        }

        public boolean isMacOS() {
            return (osName.equals("mac"));
        }

        public boolean mayNeedDRIHack() {
            return (!isMacOS() && !osName.equals("win"));
        }
    }

    private static final NativeLibInfo[] allNativeLibInfo = { new NativeLibInfo("win", "x86", "windows-i586", "", ".dll"), new NativeLibInfo("win", "amd64", "windows-amd64", "", ".dll"), new NativeLibInfo("win", "x86_64", "windows-amd64", "", ".dll"), new NativeLibInfo("mac", "ppc", "macosx-ppc", "lib", ".jnilib"), new NativeLibInfo("mac", "i386", "macosx-universal", "lib", ".jnilib"), new NativeLibInfo("linux", "i386", "linux-i586", "lib", ".so"), new NativeLibInfo("linux", "x86", "linux-i586", "lib", ".so"), new NativeLibInfo("linux", "amd64", "linux-amd64", "lib", ".so"), new NativeLibInfo("linux", "x86_64", "linux-amd64", "lib", ".so"), new NativeLibInfo("sunos", "sparc", "solaris-sparc", "lib", ".so"), new NativeLibInfo("sunos", "sparcv9", "solaris-sparcv9", "lib", ".so"), new NativeLibInfo("sunos", "x86", "solaris-i586", "lib", ".so"), new NativeLibInfo("sunos", "amd64", "solaris-amd64", "lib", ".so"), new NativeLibInfo("sunos", "x86_64", "solaris-amd64", "lib", ".so") };

    private NativeLibInfo nativeLibInfo;

    private String[] nativeLibNames;

    /** true if we successfully loaded all dll's */
    private boolean joglLoaded = false;

    /** Indicates whether JOAL is present */
    private boolean haveJOAL = false;

    private boolean checkOSAndArch(String osName, String osArch) {
        for (int i = 0; i < allNativeLibInfo.length; i++) {
            NativeLibInfo info = allNativeLibInfo[i];
            if (info.matchesOSAndArch(osName, osArch)) {
                nativeLibInfo = info;
                return true;
            }
        }
        return false;
    }

    private void saveNativesJarLocally(File localJarFile, URLConnection urlConnection) throws Exception {
        BufferedOutputStream out = null;
        ;
        InputStream in = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(localJarFile));
            int totalLength = urlConnection.getContentLength();
            in = urlConnection.getInputStream();
            byte[] buffer = new byte[1024];
            int len;
            int sum = 0;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
                sum += len;
                int percent = (100 * sum / totalLength);
            }
            out.close();
            in.close();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private boolean findNativeEntries(JarFile jf) {
        List list = new ArrayList();
        Enumeration e = jf.entries();
        while (e.hasMoreElements()) {
            JarEntry entry = (JarEntry) e.nextElement();
            if (nativeLibInfo.matchesNativeLib(entry.getName())) {
                list.add(entry.getName());
            }
        }
        if (list.isEmpty()) {
            return false;
        }
        nativeLibNames = (String[]) list.toArray(new String[0]);
        return true;
    }

    private boolean installFile(File installDir, JarFile jar, String fileName, byte[] buf) {
        try {
            JarEntry entry = jar.getJarEntry(fileName);
            if (entry == null) {
                return false;
            }
            InputStream is = jar.getInputStream(entry);
            int totalLength = (int) entry.getSize();
            BufferedOutputStream out = null;
            File outputFile = new File(installDir, fileName);
            boolean exists = false;
            try {
                exists = outputFile.exists();
                out = new BufferedOutputStream(new FileOutputStream(outputFile));
            } catch (Exception e) {
                if (exists) {
                    return true;
                } else {
                    return false;
                }
            }
            int len;
            try {
                while ((len = is.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                outputFile.delete();
                return false;
            }
            out.flush();
            out.close();
            is.close();
            return true;
        } catch (Exception e2) {
            e2.printStackTrace();
            return false;
        }
    }

    /** last step before launch : System.load() the natives and init()/start() the child applet  */
    private void loadNatives(final File nativeLibDir) {
        com.sun.opengl.impl.NativeLibLoader.disableLoading();
        com.sun.gluegen.runtime.NativeLibLoader.disableLoading();
        loadLibrary(nativeLibDir, "gluegen-rt");
        Class driHackClass = null;
        if (nativeLibInfo.mayNeedDRIHack()) {
            try {
                driHackClass = Class.forName("com.sun.opengl.impl.x11.DRIHack");
                driHackClass.getMethod("begin", new Class[] {}).invoke(null, new Object[] {});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        loadLibrary(nativeLibDir, "jogl");
        if (nativeLibInfo.mayNeedDRIHack()) {
            try {
                driHackClass.getMethod("end", new Class[] {}).invoke(null, new Object[] {});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!nativeLibInfo.isMacOS()) {
            try {
                System.loadLibrary("jawt");
            } catch (UnsatisfiedLinkError ex) {
                if (ex.getMessage().indexOf("already loaded") == -1) {
                    throw ex;
                }
            }
        }
        loadLibrary(nativeLibDir, "jogl_awt");
        if (haveJOAL) {
            try {
                Class c = Class.forName("net.java.games.joal.impl.NativeLibLoader");
                c.getMethod("disableLoading", new Class[] {}).invoke(null, new Object[] {});
            } catch (Exception e) {
                e.printStackTrace();
            }
            String javaLibPath = System.getProperty("java.library.path");
            String absPath = nativeLibDir.getAbsolutePath();
            boolean shouldSet = false;
            if (javaLibPath == null) {
                javaLibPath = absPath;
                shouldSet = true;
            } else if (javaLibPath.indexOf(absPath) < 0) {
                javaLibPath = javaLibPath + File.pathSeparator + absPath;
                shouldSet = true;
            }
            if (shouldSet) {
                System.setProperty("java.library.path", javaLibPath);
            }
            loadLibrary(nativeLibDir, "joal_native");
        }
    }

    private void loadLibrary(File installDir, String libName) {
        String nativeLibName = nativeLibInfo.getNativeLibName(libName);
        try {
            System.load(new File(installDir, nativeLibName).getPath());
        } catch (UnsatisfiedLinkError ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
}
