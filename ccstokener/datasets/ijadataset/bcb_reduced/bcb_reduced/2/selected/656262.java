package com.yearahead.io;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Associates a URL with a file so that remote files can be loaded
 * and saved to a web server.  This class works with WebFileSystemView,
 * and probably doesn't work standalone.
 */
public class WebFile extends File {

    /** Set to true to turn on debugging. */
    public static final boolean DEBUG = false;

    private URL url;

    private boolean isDir;

    private static File[] FileArrayType = new File[0];

    /**
	 * Create a new named web file.
	 */
    public WebFile(String name) {
        super(name);
    }

    /**
	 * Create a child file - parent is a directory.
	 */
    public WebFile(WebFile f, String name) {
        super(f, name);
        url = f.url;
        isDir = name.endsWith("/");
    }

    /**
	 * Called just once, from WebFileSystem.  This creates the parent
	 * WebFile from which all others are based.
	 */
    public WebFile(String name, URL url) {
        super(name);
        this.url = url;
        this.isDir = true;
    }

    /**
	 * Returns true if file is a directory.
	 */
    public boolean isDirectory() {
        return isDir;
    }

    /**
	 * Returns true if file exists on web server.
	 */
    public boolean exists() {
        dbg("exists: " + getPath());
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(httpPost(url, getPath(), "exists", "1")));
            String result = br2string(r);
            if ("1".equals(result)) {
                return true;
            } else {
                dbg("exists Failed(" + result.length() + "): '" + result + "'");
                return false;
            }
        } catch (IOException x) {
            x.printStackTrace();
            return false;
        }
    }

    /**
	 * Renames the file denoted by this abstract pathname on the
	 * remote web server.
	 */
    public boolean renameTo(File dest) {
        dbg("renameTo url=" + url + " destname=" + dest + " destpath=" + dest.getPath());
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(httpPost(url, getPath(), "rename", dest.getName())));
            String result = br2string(r);
            if ("1".equals(result)) {
                dbg("sucess");
                return true;
            } else {
                dbg("renameTo Failed: " + result);
                return false;
            }
        } catch (IOException x) {
            x.printStackTrace();
            return false;
        }
    }

    /**
	 * Creates a new directory on the remote web server.
	 */
    public boolean mkdir() {
        dbg("mkdir: " + getPath());
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(httpPost(url, getPath(), "mkdir", "1")));
            String result = br2string(r);
            if ("1".equals(result)) {
                return true;
            } else {
                dbg("mkdir Failed: " + result);
                return false;
            }
        } catch (IOException x) {
            x.printStackTrace();
            return false;
        }
    }

    /**
	 * List all the files in this directory.
	 */
    public File[] listFiles() {
        dbg("listFiles: " + getPath());
        ArrayList list = new ArrayList();
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(httpPost(url, getPath(), null, null)));
            String s;
            while ((s = r.readLine()) != null) {
                list.add(new WebFile(this, s));
            }
        } catch (IOException x) {
            x.printStackTrace();
        }
        return (File[]) list.toArray(FileArrayType);
    }

    /**
	 * Opens the given file.  These were made static since Outliner
	 * wants to pass strings rather than a file.
	 */
    public static InputStream open(String host, String path) throws IOException {
        dbg("open: " + path);
        return httpPost(host, path, "open", "1");
    }

    /**
	 * Saves a file to the web server.  These were made static since
	 * Outliner wants to pass strings rather than a file.
	 */
    public static boolean save(String url, String path, byte[] bytes) throws IOException {
        dbg("save, url=" + url + " path=" + path);
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(httpPost(url, path, "save", new String(bytes))));
            String result = br2string(r);
            if ("1".equals(result)) {
                return true;
            } else {
                dbg("save Failed: " + result);
                return false;
            }
        } catch (IOException x) {
            x.printStackTrace();
            return false;
        }
    }

    /**
	 * Util method return buffered reader into string.
	 */
    public static String br2string(BufferedReader r) {
        try {
            StringBuffer sb = new StringBuffer();
            String s;
            while ((s = r.readLine()) != null) {
                sb.append(s);
                sb.append("\n");
            }
            return sb.substring(0, sb.length() - 1);
        } catch (IOException x) {
            x.printStackTrace();
            return null;
        }
    }

    /**
	 * Returns the canonical form of this abstract pathname.
	 */
    public File getCanonicalFile() throws IOException {
        return this;
    }

    /**
	 * Returns the canonical pathname string of this abstract pathname.
	 */
    public String getCanonicalPath() throws IOException {
        return getPath();
    }

    /**
	 * Converts this abstract pathname into a pathname string.
	 */
    public String getPath() {
        File f = this;
        String s = super.getName();
        while ((f = f.getParentFile()) != null) {
            s = f.getName() + "/" + s;
        }
        return s;
    }

    /**
	 * All communication between the webserver and outliner takes
	 * place here.  Performs a HTTP POST to the web server, returning
	 * the results in a BufferedReader.  For the future, this needs
	 * to work with basic http authorization.
	 *
	 * @param url the url to the web server script, something like
	 * http://www.example.com/outliner.php
	 * @param path the relative path (between outliner.php and the files)
	 * @param name optional extra parameter, needed for everything but
	 * listFiles
	 * @param value optional
	 * @return the data returned from the web server, could either
	 * be a boolean type result, like "1" or "0", or the actual data.
	 */
    public static InputStream httpPost(String url, String path, String name, String value) throws IOException {
        return httpPost(new URL(url), path, name, value);
    }

    /**
	 * All communication between the webserver and outliner takes
	 * place here.  Performs a HTTP POST to the web server, returning
	 * the results in a BufferedReader.  For the future, this needs
	 * to work with basic http authorization.
	 *
	 * @param url the url to the web server script, something like
	 * http://www.example.com/outliner.php
	 * @param path the relative path (between outliner.php and the files)
	 * @param name optional extra parameter, needed for everything but
	 * listFiles
	 * @param value optional
	 * @return the data returned from the web server, could either
	 * be a boolean type result, like "1" or "0", or the actual data.
	 */
    public static InputStream httpPost(URL url, String path, String name, String value) throws IOException {
        dbg("httpPost: " + url + " path=" + path + " " + name + "=" + value);
        URLConnection c = url.openConnection();
        c.setDoOutput(true);
        PrintWriter out = new PrintWriter(c.getOutputStream());
        int index = path.indexOf("/");
        if (index != -1) {
            path = path.substring(index);
        } else {
            path = "/.";
        }
        dbg("  path: " + path);
        out.print("path=" + URLEncoder.encode(path, "UTF-8"));
        if (name != null) {
            out.print("&" + name + "=" + URLEncoder.encode(value, "UTF-8"));
        }
        out.close();
        return c.getInputStream();
    }

    /**
	 * Simple debug method that can be turned on and off thru a static
	 * public boolean value.
	 *
	 * @param s the debug output
	 */
    private static void dbg(String s) {
        if (DEBUG) {
            System.out.println("[WebFile] " + s);
        }
    }
}
