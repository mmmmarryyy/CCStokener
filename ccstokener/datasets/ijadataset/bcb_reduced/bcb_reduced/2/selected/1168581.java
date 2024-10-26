package org.fao.waicent.util;

import java.applet.Applet;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Observer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Loader {

    public static boolean netscape = false;

    private Applet applet;

    private Component image_creator;

    private URL base_url;

    private String target = "_this";

    private String base_directory;

    public Loader(String base_url_str, Component image_creator) {
        this.applet = null;
        this.image_creator = image_creator;
        this.base_directory = base_url_str;
        setBaseURL(base_url_str);
    }

    public Loader(Applet applet) {
        this.applet = applet;
        image_creator = this.applet;
        setBaseURL(applet.getCodeBase());
    }

    public void setBaseURL(URL url) {
        base_url = url;
        this.base_directory = url.toString();
    }

    public void setBaseURL(String url_str) {
        try {
            setBaseURL(new URL(url_str));
        } catch (Exception e) {
            System.err.println(e);
            System.err.println("Invalido URL");
            this.base_url = null;
        }
    }

    public URL getBaseURL() {
        return base_url;
    }

    public String getBaseDirectory() {
        return base_directory;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Applet getApplet() {
        return applet;
    }

    public Component getCreator() {
        return image_creator;
    }

    public Image createImage(int width, int height) {
        if (image_creator != null) {
            return image_creator.createImage(width, height);
        } else {
            return null;
        }
    }

    /**     protected Image createImage(int width, int height)     {     //		image = Toolkit.getDefaultToolkit().createImage(new ImageSource(getScaledMap().getBoundingBox().width, getScaledMap().getBoundingBox().height));     //		Toolkit.getDefaultToolkit().prepareImage(image, getScaledMap().getBoundingBox().width, getScaledMap().getBoundingBox().height, this);     {     int w = getScaledMap().getBoundingBox().width;     int h = getScaledMap().getBoundingBox().height;     int pix[] = new int[w * h];     int index = 0;     for (int y = 0; y < h; y++) {     int red = (y * 255) / (h - 1);     for (int x = 0; x < w; x++) {     int blue = (x * 255) / (w - 1);     pix[index++] = (255 << 24) | (red << 16) | blue;     }     }     image = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w, h, pix, 0, w));     Toolkit.getDefaultToolkit().prepareImage(image, -1, -1, this);     }     return null;     }     **/
    public Image getImage(String filename) {
        if (applet != null) {
            Image image = getApplet().getImage(base_url, filename);
            return image;
        } else {
            return null;
        }
    }

    public void showStatus(String text) {
        if (applet != null) {
            applet.showStatus(text);
        }
    }

    public void showDocument(String filename) {
        showDocument(filename, true, null);
    }

    public void showDocument(String filename, boolean request_focus) {
        showDocument(filename, request_focus, null);
    }

    public void showDocument(String filename, boolean request_focus, String where) {
        if (applet != null) {
            if (filename.equals("html/empty.htm")) {
                return;
            }
            String tmp_target = target;
            if (filename.substring(filename.length() - 3, filename.length()).equals("xls")) {
                tmp_target = "_blank";
            }
            if (where != null) {
                tmp_target = where;
            }
            try {
                URL doc = new URL(applet.getCodeBase(), filename);
                applet.getAppletContext().showDocument(doc, tmp_target);
                applet.getAppletContext().showStatus("Loading:" + doc);
                if (!tmp_target.equals("xls")) {
                    if (request_focus) {
                        applet.requestFocus();
                    }
                }
            } catch (MalformedURLException read_URL_failed) {
                System.out.println("Cannot read URL");
            }
        }
    }

    Frame frame = null;

    public Frame getFrame() {
        return this.frame;
    }

    public void setFrame(Frame frame) {
        this.frame = frame;
    }

    public Object readObjectDialogMode(String filename) {
        return readObject(filename).getObject();
    }

    public LoaderThread readObject(String filename) {
        return readObject(filename, null);
    }

    public LoaderThread readObject(String filename, Observer observer) {
        LoaderThread thread = new LoaderThread(this, filename);
        if (observer != null) {
            thread.addObserver(observer);
            thread.start();
        } else {
            thread.readObject();
        }
        return thread;
    }

    public LoaderThread writeObject(Object obj, String filename) {
        return writeObject(obj, filename, null);
    }

    public LoaderThread writeObject(Object obj, String filename, Observer observer) {
        LoaderThread thread = new LoaderThread(this, filename, obj);
        if (observer != null) {
            thread.addObserver(observer);
            thread.start();
        } else {
            thread.writeObject();
        }
        return thread;
    }

    /**** IN ****/
    public ObjectInputStream openIn(String filename) {
        ObjectInputStream in = null;
        try {
            URL url = null;
            try {
                url = new URL(base_url, getCacheFilename(filename));
                ZipInputStream zin = new ZipInputStream(url.openStream());
                System.out.println("Open " + zin.getNextEntry().getName() + " from " + url);
                in = new ObjectInputStream(zin);
            } catch (Exception e) {
                closeIn(in);
                in = null;
            }
        } catch (Exception exception) {
            System.out.println("Cannot open (in) " + filename);
            System.out.println(exception);
            closeIn(in);
            in = null;
        }
        return in;
    }

    public DataInputStream openDataIn(String filename) {
        DataInputStream data_in = null;
        InputStream in = openTextIn(filename);
        if (in != null) {
            data_in = new DataInputStream(in);
        }
        return data_in;
    }

    public StreamTokenizer openTokenIn(String filename) {
        StreamTokenizer tok_in = null;
        InputStream in = openTextIn(filename);
        if (in != null) {
            Reader r = new BufferedReader(new InputStreamReader(in));
            tok_in = new StreamTokenizer(r);
        }
        return tok_in;
    }

    public InputStream openTextIn(String filename) {
        boolean relative = (filename.indexOf(":") == -1);
        InputStream in = null;
        try {
            if (relative) {
                URL url = new URL(base_url, filename);
                in = url.openStream();
            } else {
                in = new FileInputStream(filename);
            }
            System.out.println("Open " + filename);
        } catch (Exception e) {
            System.out.println("Cannot open (in) " + filename);
            System.out.println(e);
            closeIn(in);
            in = null;
        } finally {
        }
        return in;
    }

    public void closeIn(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException close_failed) {
                System.out.println("Cannot close " + in);
            } finally {
                in = null;
            }
        }
    }

    /**** OUT ****/
    public static String makeLocalFilenameFromURL(URL url) {
        String local_filename = url.getFile().replace('|', ':');
        local_filename = local_filename.substring(1, local_filename.length());
        local_filename = local_filename.replace('/', File.separatorChar);
        return local_filename;
    }

    public static String getCacheFilename(String filename) {
        String cache_dir = "cache/";
        if (netscape) {
            cache_dir += "netscape/";
        }
        String zip_filename = cache_dir + filename + ".zip";
        zip_filename = zip_filename.replace(':', '_');
        zip_filename = zip_filename.replace('|', '_');
        return zip_filename;
    }

    public boolean existsCacheFile(String filename) {
        boolean exists = false;
        try {
            URL url = new URL(base_url, getCacheFilename(filename));
            String fullfilename = makeLocalFilenameFromURL(url);
            exists = new File(fullfilename).exists();
        } catch (Exception e) {
        }
        return exists;
    }

    public ObjectOutputStream openOut(String filename) {
        ObjectOutputStream out = null;
        try {
            URL url = new URL(base_url, getCacheFilename(filename));
            ZipOutputStream zout = null;
            String fullfilename = makeLocalFilenameFromURL(url);
            File file = new File(fullfilename);
            new File(file.getParent()).mkdirs();
            zout = new ZipOutputStream(new FileOutputStream(file));
            zout.putNextEntry(new ZipEntry(filename));
            out = new ObjectOutputStream(zout);
        } catch (Exception e) {
            System.out.println("Cannot open (out) " + filename);
            System.out.println(e);
            e.printStackTrace();
            closeOut(out);
            out = null;
        }
        return out;
    }

    public FileOutputStream openFileOut(String filename) {
        FileOutputStream out = null;
        try {
            File file = new File(filename);
            out = new FileOutputStream(file);
        } catch (Exception e) {
            System.out.println("Cannot open (out) " + filename);
            System.out.println(e);
            e.printStackTrace();
            closeOut(out);
            out = null;
        }
        return out;
    }

    public void closeOut(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException close_failed) {
                System.out.println("Cannot close " + out);
            } finally {
                out = null;
            }
        }
    }
}
