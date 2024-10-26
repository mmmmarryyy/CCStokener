package jode.bytecode;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import jode.GlobalOptions;

/**
 * This class represents a path of multiple directories and/or zip files,
 * where we can search for file names.
 * 
 * @author Jochen Hoenicke
 */
public class SearchPath {

    /**
     * We need a different pathSeparatorChar, since ':' (used for most
     * UNIX System) is used a protocol separator in URLs.  
     *
     * We currently allow both pathSeparatorChar and
     * altPathSeparatorChar and decide if it is a protocol separator
     * by context.  
     */
    public static final char altPathSeparatorChar = ',';

    URL[] bases;

    byte[][] urlzips;

    File[] dirs;

    ZipFile[] zips;

    String[] zipDirs;

    Hashtable[] zipEntries;

    private static void addEntry(Hashtable entries, String name) {
        String dir = "";
        int pathsep = name.lastIndexOf("/");
        if (pathsep != -1) {
            dir = name.substring(0, pathsep);
            name = name.substring(pathsep + 1);
        }
        Vector dirContent = (Vector) entries.get(dir);
        if (dirContent == null) {
            dirContent = new Vector();
            entries.put(dir, dirContent);
            if (dir != "") addEntry(entries, dir);
        }
        dirContent.addElement(name);
    }

    private void fillZipEntries(int nr) {
        Enumeration zipEnum = zips[nr].entries();
        zipEntries[nr] = new Hashtable();
        while (zipEnum.hasMoreElements()) {
            ZipEntry ze = (ZipEntry) zipEnum.nextElement();
            String name = ze.getName();
            if (zipDirs[nr] != null) {
                if (!name.startsWith(zipDirs[nr])) continue;
                name = name.substring(zipDirs[nr].length());
            }
            if (!ze.isDirectory() && name.endsWith(".class")) addEntry(zipEntries[nr], name);
        }
    }

    private void readURLZip(int nr, URLConnection conn) {
        int length = conn.getContentLength();
        if (length <= 0) length = 10240; else length++;
        urlzips[nr] = new byte[length];
        try {
            InputStream is = conn.getInputStream();
            int pos = 0;
            for (; ; ) {
                int avail = Math.max(is.available(), 1);
                if (pos + is.available() > urlzips[nr].length) {
                    byte[] newarr = new byte[Math.max(2 * urlzips[nr].length, pos + is.available())];
                    System.arraycopy(urlzips[nr], 0, newarr, 0, pos);
                    urlzips[nr] = newarr;
                }
                int count = is.read(urlzips[nr], pos, urlzips[nr].length - pos);
                if (count == -1) break;
                pos += count;
            }
            if (pos < urlzips[nr].length) {
                byte[] newarr = new byte[pos];
                System.arraycopy(urlzips[nr], 0, newarr, 0, pos);
                urlzips[nr] = newarr;
            }
        } catch (IOException ex) {
            GlobalOptions.err.println("IOException while reading " + "remote zip file " + bases[nr]);
            bases[nr] = null;
            urlzips[nr] = null;
            return;
        }
        try {
            ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(urlzips[nr]));
            zipEntries[nr] = new Hashtable();
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                String name = ze.getName();
                if (zipDirs[nr] != null) {
                    if (!name.startsWith(zipDirs[nr])) continue;
                    name = name.substring(zipDirs[nr].length());
                }
                if (!ze.isDirectory() && name.endsWith(".class")) addEntry(zipEntries[nr], name);
                zis.closeEntry();
            }
            zis.close();
        } catch (IOException ex) {
            GlobalOptions.err.println("Remote zip file " + bases[nr] + " is corrupted.");
            bases[nr] = null;
            urlzips[nr] = null;
            zipEntries[nr] = null;
            return;
        }
    }

    /**
     * Creates a new search path for the given path.
     * @param path The path where we should search for files.  They
     * should be separated by the system dependent pathSeparator.  The
     * entries may also be zip or jar files.
     */
    public SearchPath(String path) {
        int length = 1;
        for (int index = path.indexOf(File.pathSeparatorChar); index != -1; length++) index = path.indexOf(File.pathSeparatorChar, index + 1);
        if (File.pathSeparatorChar != altPathSeparatorChar) {
            for (int index = path.indexOf(altPathSeparatorChar); index != -1; length++) index = path.indexOf(altPathSeparatorChar, index + 1);
        }
        bases = new URL[length];
        urlzips = new byte[length][];
        dirs = new File[length];
        zips = new ZipFile[length];
        zipEntries = new Hashtable[length];
        zipDirs = new String[length];
        int i = 0;
        for (int ptr = 0; ptr < path.length(); ptr++, i++) {
            int next = ptr;
            while (next < path.length() && path.charAt(next) != File.pathSeparatorChar && path.charAt(next) != altPathSeparatorChar) next++;
            int index = ptr;
            colon_separator: while (next > ptr && next < path.length() && path.charAt(next) == ':') {
                while (index < next) {
                    char c = path.charAt(index);
                    if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z') && (c < '0' || c > '9') && "+-".indexOf(c) == -1) {
                        break colon_separator;
                    }
                    index++;
                }
                next++;
                index++;
                while (next < path.length() && path.charAt(next) != File.pathSeparatorChar && path.charAt(next) != altPathSeparatorChar) next++;
            }
            String token = path.substring(ptr, next);
            ptr = next;
            boolean mustBeJar = false;
            if (token.startsWith("jar:")) {
                index = 0;
                do {
                    index = token.indexOf('!', index);
                } while (index != -1 && index != token.length() - 1 && token.charAt(index + 1) != '/');
                if (index == -1 || index == token.length() - 1) {
                    GlobalOptions.err.println("Warning: Illegal jar url " + token + ".");
                    continue;
                }
                zipDirs[i] = token.substring(index + 2);
                if (!zipDirs[i].endsWith("/")) zipDirs[i] = zipDirs[i] + "/";
                token = token.substring(4, index);
                mustBeJar = true;
            }
            index = token.indexOf(':');
            if (index != -1 && index < token.length() - 2 && token.charAt(index + 1) == '/' && token.charAt(index + 2) == '/') {
                try {
                    bases[i] = new URL(token);
                    try {
                        URLConnection connection = bases[i].openConnection();
                        if (mustBeJar || token.endsWith(".zip") || token.endsWith(".jar") || connection.getContentType().endsWith("/zip")) {
                            readURLZip(i, connection);
                        }
                    } catch (IOException ex) {
                    } catch (SecurityException ex) {
                        GlobalOptions.err.println("Warning: Security exception while accessing " + bases[i] + ".");
                    }
                } catch (MalformedURLException ex) {
                    bases[i] = null;
                    dirs[i] = null;
                }
            } else {
                try {
                    dirs[i] = new File(token);
                    if (mustBeJar || !dirs[i].isDirectory()) {
                        try {
                            zips[i] = new ZipFile(dirs[i]);
                        } catch (java.io.IOException ex) {
                            dirs[i] = null;
                        }
                    }
                } catch (SecurityException ex) {
                    GlobalOptions.err.println("Warning: SecurityException while accessing " + token + ".");
                    dirs[i] = null;
                }
            }
        }
    }

    public boolean exists(String filename) {
        String localFileName = (java.io.File.separatorChar != '/') ? filename.replace('/', java.io.File.separatorChar) : filename;
        for (int i = 0; i < dirs.length; i++) {
            if (zipEntries[i] != null) {
                if (zipEntries[i].get(filename) != null) return true;
                String dir = "";
                String name = filename;
                int index = filename.lastIndexOf('/');
                if (index >= 0) {
                    dir = filename.substring(0, index);
                    name = filename.substring(index + 1);
                }
                Vector directory = (Vector) zipEntries[i].get(dir);
                if (directory != null && directory.contains(name)) return true;
                continue;
            }
            if (bases[i] != null) {
                try {
                    URL url = new URL(bases[i], filename);
                    URLConnection conn = url.openConnection();
                    conn.connect();
                    conn.getInputStream().close();
                    return true;
                } catch (IOException ex) {
                }
                continue;
            }
            if (dirs[i] == null) continue;
            if (zips[i] != null) {
                String fullname = zipDirs[i] != null ? zipDirs[i] + filename : filename;
                ZipEntry ze = zips[i].getEntry(fullname);
                if (ze != null) return true;
            } else {
                try {
                    File f = new File(dirs[i], localFileName);
                    if (f.exists()) return true;
                } catch (SecurityException ex) {
                }
            }
        }
        return false;
    }

    /**
     * Searches for a file in the search path.
     * @param filename the filename. The path components should be separated
     * by <code>/</code>.
     * @return An InputStream for the file.
     */
    public InputStream getFile(String filename) throws IOException {
        String localFileName = (java.io.File.separatorChar != '/') ? filename.replace('/', java.io.File.separatorChar) : filename;
        for (int i = 0; i < dirs.length; i++) {
            if (urlzips[i] != null) {
                ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(urlzips[i]));
                ZipEntry ze;
                String fullname = zipDirs[i] != null ? zipDirs[i] + filename : filename;
                while ((ze = zis.getNextEntry()) != null) {
                    if (ze.getName().equals(fullname)) {
                        return zis;
                    }
                    zis.closeEntry();
                }
                continue;
            }
            if (bases[i] != null) {
                try {
                    URL url = new URL(bases[i], filename);
                    URLConnection conn = url.openConnection();
                    conn.setAllowUserInteraction(true);
                    return conn.getInputStream();
                } catch (SecurityException ex) {
                    GlobalOptions.err.println("Warning: SecurityException" + " while accessing " + bases[i] + filename);
                    ex.printStackTrace(GlobalOptions.err);
                } catch (FileNotFoundException ex) {
                }
                continue;
            }
            if (dirs[i] == null) continue;
            if (zips[i] != null) {
                String fullname = zipDirs[i] != null ? zipDirs[i] + filename : filename;
                ZipEntry ze = zips[i].getEntry(fullname);
                if (ze != null) return zips[i].getInputStream(ze);
            } else {
                try {
                    File f = new File(dirs[i], localFileName);
                    if (f.exists()) return new FileInputStream(f);
                } catch (SecurityException ex) {
                    GlobalOptions.err.println("Warning: SecurityException" + " while accessing " + dirs[i] + localFileName);
                }
            }
        }
        throw new FileNotFoundException(filename);
    }

    /**
     * Searches for a filename in the search path and tells if it is a
     * directory.
     * @param filename the filename. The path components should be separated
     * by <code>/</code>.
     * @return true, if filename exists and is a directory, false otherwise.
     */
    public boolean isDirectory(String filename) {
        String localFileName = (java.io.File.separatorChar != '/') ? filename.replace('/', java.io.File.separatorChar) : filename;
        for (int i = 0; i < dirs.length; i++) {
            if (dirs[i] == null) continue;
            if (zips[i] != null && zipEntries[i] == null) fillZipEntries(i);
            if (zipEntries[i] != null) {
                if (zipEntries[i].containsKey(filename)) return true;
            } else {
                try {
                    File f = new File(dirs[i], localFileName);
                    if (f.exists()) return f.isDirectory();
                } catch (SecurityException ex) {
                    GlobalOptions.err.println("Warning: SecurityException" + " while accessing " + dirs[i] + localFileName);
                }
            }
        }
        return false;
    }

    /**
     * Searches for all files in the given directory.
     * @param dirName the directory name. The path components should
     * be separated by <code>/</code>.
     * @return An enumeration with all files/directories in the given
     * directory.  */
    public Enumeration listFiles(final String dirName) {
        return new Enumeration() {

            int pathNr;

            Enumeration zipEnum;

            int fileNr;

            String localDirName = (java.io.File.separatorChar != '/') ? dirName.replace('/', java.io.File.separatorChar) : dirName;

            File currentDir;

            String[] files;

            public String findNextFile() {
                while (true) {
                    if (zipEnum != null) {
                        while (zipEnum.hasMoreElements()) {
                            return (String) zipEnum.nextElement();
                        }
                        zipEnum = null;
                    }
                    if (files != null) {
                        while (fileNr < files.length) {
                            String name = files[fileNr++];
                            if (name.endsWith(".class")) {
                                return name;
                            } else if (name.indexOf(".") == -1) {
                                File f = new File(currentDir, name);
                                if (f.exists() && f.isDirectory()) return name;
                            }
                        }
                        files = null;
                    }
                    if (pathNr == dirs.length) return null;
                    if (zips[pathNr] != null && zipEntries[pathNr] == null) fillZipEntries(pathNr);
                    if (zipEntries[pathNr] != null) {
                        Vector entries = (Vector) zipEntries[pathNr].get(dirName);
                        if (entries != null) zipEnum = entries.elements();
                    } else if (dirs[pathNr] != null) {
                        try {
                            File f = new File(dirs[pathNr], localDirName);
                            if (f.exists() && f.isDirectory()) {
                                currentDir = f;
                                files = f.list();
                                fileNr = 0;
                            }
                        } catch (SecurityException ex) {
                            GlobalOptions.err.println("Warning: SecurityException" + " while accessing " + dirs[pathNr] + localDirName);
                        }
                    }
                    pathNr++;
                }
            }

            String nextName;

            public boolean hasMoreElements() {
                return (nextName != null || (nextName = findNextFile()) != null);
            }

            public Object nextElement() {
                if (nextName == null) return findNextFile(); else {
                    String result = nextName;
                    nextName = null;
                    return result;
                }
            }
        };
    }
}
