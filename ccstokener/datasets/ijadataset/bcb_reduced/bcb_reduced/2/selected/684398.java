package org.merlotxml.merlot.plugin.dtd;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.merlotxml.merlot.MerlotDebug;
import org.merlotxml.util.FileUtil;
import org.merlotxml.util.xml.DTDCache;
import org.merlotxml.util.xml.XPathUtil;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Merlot Pluggable DTD Configuration
 * 
 * @author Tim McCune
 */
class DTDConfig {

    protected static final String XPATH_TEXT = "/text()";

    protected static final String XPATH_FILE = "file" + XPATH_TEXT;

    protected static final String XPATH_URL = "url" + XPATH_TEXT;

    protected static final String XPATH_DOCTYPE = "doctype" + XPATH_TEXT;

    protected static final String XPATH_PUBLIC_ID = "publicID" + XPATH_TEXT;

    protected static final String XPATH_SYSTEM_ID = "systemID" + XPATH_TEXT;

    protected String _doctype;

    protected String _publicID;

    protected String _systemID;

    protected DTDPluginConfig _parent = null;

    /**
	 * @exception SAXException Thrown if the XML is malformed
	 * @exception IOException Thrown if there is a problem reading in the DTD
	 * @exception FileNotFoundException Thrown if the specified DTD is not found
	 */
    public void parse(Node node) throws SAXException, IOException, FileNotFoundException {
        File pluginSource = _parent.getSource();
        InputStream dtdSource = null;
        PluginDTDCacheEntry cacheEntry;
        String s;
        ZipEntry dtdEntry;
        ZipFile sourceZip;
        if (((s = XPathUtil.getValue(node, XPATH_FILE)) != null) && (pluginSource != null)) {
            if (pluginSource.isDirectory()) {
                String urlstr = XPathUtil.getValue(node, XPATH_URL);
                File xsdFile = null;
                if (urlstr != null) {
                    try {
                        URL furl = new URL(urlstr);
                        File xsdDir = new File(pluginSource, s);
                        xsdFile = downloadURL(furl, xsdDir.getParentFile(), xsdDir.getName());
                    } catch (Exception e) {
                        xsdFile = new File(pluginSource, s);
                    }
                } else {
                    xsdFile = new File(pluginSource, s);
                }
                dtdSource = new FileInputStream(xsdFile);
            } else {
                try {
                    sourceZip = new ZipFile(pluginSource);
                    dtdEntry = sourceZip.getEntry(s);
                    dtdSource = sourceZip.getInputStream(dtdEntry);
                } catch (ZipException e) {
                }
            }
        }
        _doctype = XPathUtil.getValue(node, XPATH_DOCTYPE);
        _publicID = XPathUtil.getValue(node, XPATH_PUBLIC_ID);
        _systemID = XPathUtil.getValue(node, XPATH_SYSTEM_ID);
        if (dtdSource != null) {
            cacheEntry = new PluginDTDCacheEntry(_publicID, _systemID, this, _parent);
            cacheEntry.setDoctype(_doctype);
            cacheEntry.setFilePath(s);
            DTDCache.getSharedInstance().loadDTDIntoCache(dtdSource, cacheEntry);
        }
    }

    void setParent(DTDPluginConfig parent) {
        _parent = parent;
    }

    public static File downloadContent(URLConnection connection, File cacheFile) {
        try {
            BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
            File outFile = File.createTempFile("tmp", "-t");
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
            int i;
            while ((i = in.read()) != -1) {
                out.write(i);
            }
            out.flush();
            out.close();
            in.close();
            FileUtil.copyFile(outFile, cacheFile);
            outFile.delete();
            return cacheFile;
        } catch (IOException ex) {
            MerlotDebug.exception(ex);
        }
        return null;
    }

    public static File downloadURL(URL url, File dir, String sfilename) {
        MerlotDebug.msg("Downloading URL: " + url);
        String filename = url.getFile();
        if (filename.indexOf('/') >= 0) {
            filename = filename.substring(filename.lastIndexOf('/') + 1);
        }
        File cache = new File(dir, sfilename);
        try {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            URLConnection connection = url.openConnection();
            if (cache.exists() && cache.canRead()) {
                connection.connect();
                long remoteTimestamp = connection.getLastModified();
                if (remoteTimestamp == 0 || remoteTimestamp > cache.lastModified()) {
                    cache = downloadContent(connection, cache);
                } else {
                    MerlotDebug.msg("Using cached version for URL: " + url);
                }
            } else {
                cache = downloadContent(connection, cache);
            }
        } catch (IOException ex) {
            MerlotDebug.exception(ex);
        }
        if (cache != null && cache.exists()) {
            return cache;
        } else {
            return null;
        }
    }

    public String toString() {
        StringBuffer rtn = new StringBuffer();
        rtn.append("doctype: " + _doctype + "\n");
        rtn.append("publicID: " + _publicID + "\n");
        rtn.append("systemID: " + _systemID + "\n");
        return rtn.toString();
    }
}
