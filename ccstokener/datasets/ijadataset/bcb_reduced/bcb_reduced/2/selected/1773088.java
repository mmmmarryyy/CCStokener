package net.sf.easygettask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EasyGet extends Task {

    private List<Mirror> mirrors = new LinkedList<Mirror>();

    private List<Get> gets = new LinkedList<Get>();

    private List<Import> imports = new LinkedList<Import>();

    private Path dest;

    private Path cacheDir;

    private DecimalFormat format = new DecimalFormat("#.##");

    public EasyGet() {
        File f = new File(System.getProperty("user.home") + File.separator + ".easygettask");
        cacheDir = new Path(new Project(), f.getAbsolutePath());
    }

    public void addGet(Get get) throws Exception {
        gets.add(get);
    }

    public void addMirror(Mirror rep) {
        mirrors.add(rep);
    }

    public void addImport(Import file) throws Exception {
        imports.add(file);
    }

    private void doImports() throws Exception {
        for (Import file : imports) {
            if (file.getPath() != null) {
            } else if (file.getFile() != null) {
                File f = new File(file.getFile());
                if (f.exists()) {
                    processFile(new FileInputStream(f));
                } else {
                    log("Missing file: " + file.getFile());
                    throw new FileNotFoundException(file.getFile());
                }
            } else if (file.getUrl() != null) {
                URL url = new URL(file.getUrl());
                InputStream is = url.openStream();
                processFile(is);
                is.close();
            } else {
                log("Import needs url or file");
                throw new BuildException("Import needs url or file");
            }
        }
    }

    @Override
    public void execute() throws BuildException {
        try {
            doImports();
        } catch (Exception e) {
            throw new BuildException(e);
        }
        boolean failed = false;
        for (Get get : gets) {
            if (get.getFile() == null || "".equals(get.getFile().trim()) || "null".equals(get.getFile())) {
                log("Missing file field in: " + get);
                throw new BuildException("Missing file field:" + get);
            }
            try {
                if (needsGet(get)) {
                    long start = System.currentTimeMillis();
                    if (getCached(get)) {
                        log("EasyGot cached file " + get.getFile());
                    } else if (get(get)) {
                        long end = System.currentTimeMillis();
                        log("EasyGot file " + get.getFile() + " - " + getRate(get, end - start));
                    } else {
                        log("Failed to get file " + get.getFile());
                        failed = true;
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                failed = true;
            }
        }
        if (failed) throw new BuildException("Some files did not get downloaded");
    }

    private String getPrettySize(long bytes) {
        String ret = "0KB";
        if (bytes < 1024) {
            ret = bytes + "B";
        } else if (bytes < 1024 * 1024) {
            ret = format.format(bytes / (float) 1024) + "KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            ret = format.format(bytes / (float) (1024 * 1024)) + "MB";
        } else {
            ret = format.format(bytes / (float) (1024 * 1024 * 1024)) + "GB";
        }
        return ret;
    }

    private String getRate(Get get, long dur) {
        if (dur == 0) dur = 1;
        StringBuffer sb = new StringBuffer();
        File f = getDestFile(get);
        sb.append(getPrettySize(f.length()));
        sb.append(" @ ");
        sb.append(getPrettySize(f.length() / dur * 1000));
        sb.append("/s in " + dur + "ms");
        return sb.toString();
    }

    private File getCachedFile(Get g) {
        File ret = new File(cacheDir.toString() + File.separator + g.getPath() + File.separator + g.getFile());
        ret.getParentFile().mkdirs();
        return ret;
    }

    private boolean getCached(Get g) throws IOException {
        boolean ret = false;
        File f = getCachedFile(g);
        if (f.exists()) {
            InputStream is = null;
            OutputStream os = null;
            try {
                is = new FileInputStream(f);
                os = new FileOutputStream(getDestFile(g));
                int read;
                byte[] buffer = new byte[4096];
                while ((read = is.read(buffer)) > 0) {
                    os.write(buffer, 0, read);
                }
                ret = true;
            } finally {
                if (is != null) is.close();
                if (os != null) os.close();
                is = null;
                os = null;
            }
        }
        return ret;
    }

    private File getDestFile(Get get) {
        return new File(dest.toString() + File.separator + get.getFile());
    }

    private boolean get(Get g) throws IOException {
        File dst = getDestFile(g);
        if (g.getUrl() != null && !"".equals(g.getUrl())) {
            if (get(g.getUrl() + "/" + g.getFile(), dst, g)) return true;
        }
        for (Mirror mirror : mirrors) {
            String url = mirror.getUrl() + "/" + g.getPath() + "/" + g.getFile();
            if (get(url, dst, g)) {
                return true;
            }
        }
        return false;
    }

    private boolean get(String surl, File dst, Get get) throws IOException {
        boolean ret = false;
        InputStream is = null;
        OutputStream os = null;
        try {
            try {
                if (surl.startsWith("file://")) {
                    is = new FileInputStream(surl.substring(7));
                } else {
                    URL url = new URL(surl);
                    is = url.openStream();
                }
                if (is != null) {
                    os = new FileOutputStream(dst);
                    int read;
                    byte[] buffer = new byte[4096];
                    while ((read = is.read(buffer)) > 0) {
                        os.write(buffer, 0, read);
                    }
                    ret = true;
                }
            } catch (ConnectException ex) {
                log("Connect exception " + ex.getMessage(), ex, 3);
                if (dst.exists()) dst.delete();
            } catch (UnknownHostException ex) {
                log("Unknown host " + ex.getMessage(), ex, 3);
            } catch (FileNotFoundException ex) {
                log("File not found: " + ex.getMessage(), 3);
            }
        } finally {
            if (is != null) is.close();
            if (os != null) os.close();
            is = null;
            os = null;
        }
        if (ret) {
            try {
                is = new FileInputStream(dst);
                os = new FileOutputStream(getCachedFile(get));
                int read;
                byte[] buffer = new byte[4096];
                while ((read = is.read(buffer)) > 0) {
                    os.write(buffer, 0, read);
                }
            } finally {
                if (is != null) is.close();
                if (os != null) os.close();
                is = null;
                os = null;
            }
        }
        return ret;
    }

    public Path getDest() {
        return dest;
    }

    private boolean needsGet(Get g) {
        File f = new File(dest.toString() + File.separator + g.getFile());
        return !f.exists();
    }

    public void processFile(InputStream is) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.parse(is);
        NodeList nl = dom.getElementsByTagName("mirror");
        for (int i = 0; i < nl.getLength(); i++) {
            Mirror m = new Mirror();
            Node n = nl.item(i);
            m.setUrl(n.getAttributes().getNamedItem("url").getNodeValue());
            this.addMirror(m);
        }
        nl = dom.getElementsByTagName("get");
        for (int i = 0; i < nl.getLength(); i++) {
            Get g = new Get();
            Node n = nl.item(i);
            NamedNodeMap nnm = n.getAttributes();
            n = nnm.getNamedItem("url");
            if (n != null) g.setUrl(n.getNodeValue());
            n = nnm.getNamedItem("file");
            if (n != null) g.setFile(n.getNodeValue());
            n = nnm.getNamedItem("path");
            if (n != null) g.setPath(n.getNodeValue());
            this.addGet(g);
        }
    }

    public void setDest(Path dest) {
        this.dest = dest;
    }

    public void setFile(File file) throws Exception {
        processFile(new FileInputStream(file));
    }

    public Path getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(Path cacheDir) {
        this.cacheDir = cacheDir;
    }
}
