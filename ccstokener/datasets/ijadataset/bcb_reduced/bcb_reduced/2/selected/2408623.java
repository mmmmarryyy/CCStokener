package org.opensourcephysics.ejs.control;

import org.opensourcephysics.tools.ResourceLoader;

/**
 * Some utility functions
 */
public class Utils {

    private static java.util.Hashtable cacheImages = new java.util.Hashtable();

    public static boolean fileExists(String _codebase, String _filename) {
        if (_filename == null) {
            return false;
        }
        if (cacheImages.get(_filename) != null) {
            return true;
        }
        if (_codebase != null) {
            if (_codebase.startsWith("file:")) {
                _codebase = "file:///" + _codebase.substring(6);
            }
            if (!_codebase.endsWith("/")) {
                _codebase += "/";
            }
        }
        int index = _filename.indexOf('+');
        if (index >= 0) {
            return fileExistsInJar(_codebase, _filename.substring(0, index), _filename.substring(index + 1));
        } else if (_codebase == null) {
            java.io.File file = new java.io.File(_filename);
            return file.exists();
        } else {
            try {
                java.net.URL url = new java.net.URL(_codebase + _filename);
                java.io.InputStream stream = url.openStream();
                stream.close();
                return true;
            } catch (Exception exc) {
                return false;
            }
        }
    }

    public static boolean fileExistsInJar(String _codebase, String _jarFile, String _filename) {
        if (_filename == null || _jarFile == null) {
            return false;
        }
        java.io.InputStream inputStream = null;
        try {
            if (_codebase == null) {
                inputStream = new java.io.FileInputStream(_jarFile);
            } else {
                java.net.URL url = new java.net.URL(_codebase + _jarFile);
                inputStream = url.openStream();
            }
            java.util.jar.JarInputStream jis = new java.util.jar.JarInputStream(inputStream);
            while (true) {
                java.util.jar.JarEntry je = jis.getNextJarEntry();
                if (je == null) {
                    break;
                }
                if (je.isDirectory()) {
                    continue;
                }
                if (je.getName().equals(_filename)) {
                    return true;
                }
            }
        } catch (Exception exc) {
            return false;
        }
        return false;
    }

    public static javax.swing.ImageIcon icon(String _codebase, String _gifFile) {
        return icon(_codebase, _gifFile, true);
    }

    public static javax.swing.ImageIcon icon(String _codebase, String _gifFile, boolean _verbose) {
        if (_gifFile == null) {
            return null;
        }
        javax.swing.ImageIcon icon = (javax.swing.ImageIcon) cacheImages.get(_gifFile);
        if (icon != null) {
            return icon;
        }
        if (_codebase != null) {
            if (_codebase.startsWith("file:")) {
                _codebase = "file:///" + _codebase.substring(6);
            }
            if (!_codebase.endsWith("/")) {
                _codebase += "/";
            }
        }
        int index = _gifFile.indexOf('+');
        if (index >= 0) {
            icon = iconJar(_codebase, _gifFile.substring(0, index), _gifFile.substring(index + 1), _verbose);
        } else if (_codebase == null) {
            java.io.File file = new java.io.File(_gifFile);
            if (file.exists()) {
                icon = new javax.swing.ImageIcon(_gifFile);
            }
            if (icon == null) icon = ResourceLoader.getIcon(_gifFile);
        } else {
            try {
                java.net.URL url = new java.net.URL(_codebase + _gifFile);
                icon = new javax.swing.ImageIcon(url);
            } catch (Exception exc) {
                if (_verbose) {
                    exc.printStackTrace();
                }
                icon = null;
            }
        }
        if (icon == null || icon.getIconHeight() <= 0) {
            if (_verbose) {
                System.out.println("Unable to load image " + _gifFile);
            }
        } else {
            cacheImages.put(_gifFile, icon);
        }
        return icon;
    }

    private static byte[] enormous = new byte[100000];

    public static javax.swing.ImageIcon iconJar(String _codebase, String _jarFile, String _gifFile, boolean _verbose) {
        if (_gifFile == null || _jarFile == null) {
            return null;
        }
        javax.swing.ImageIcon icon = null;
        java.io.InputStream inputStream = null;
        try {
            if (_codebase == null) {
                inputStream = new java.io.FileInputStream(_jarFile);
            } else {
                java.net.URL url = new java.net.URL(_codebase + _jarFile);
                inputStream = url.openStream();
            }
            java.util.jar.JarInputStream jis = new java.util.jar.JarInputStream(inputStream);
            boolean done = false;
            byte[] b = null;
            while (!done) {
                java.util.jar.JarEntry je = jis.getNextJarEntry();
                if (je == null) {
                    break;
                }
                if (je.isDirectory()) {
                    continue;
                }
                if (je.getName().equals(_gifFile)) {
                    long size = (int) je.getSize();
                    int rb = 0;
                    int chunk = 0;
                    while (chunk >= 0) {
                        chunk = jis.read(enormous, rb, 255);
                        if (chunk == -1) {
                            break;
                        }
                        rb += chunk;
                    }
                    size = rb;
                    b = new byte[(int) size];
                    System.arraycopy(enormous, 0, b, 0, (int) size);
                    done = true;
                }
            }
            icon = new javax.swing.ImageIcon(b);
        } catch (Exception exc) {
            if (_verbose) {
                exc.printStackTrace();
            }
            icon = null;
        }
        return icon;
    }
}
