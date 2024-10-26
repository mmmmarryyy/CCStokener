package au.csiro.coloradoftp.plugin.geonetwork;

import com.coldcore.coloradoftp.filesystem.FailedActionException;
import com.coldcore.coloradoftp.filesystem.FailedActionReason;
import com.coldcore.coloradoftp.filesystem.FileSystem;
import com.coldcore.coloradoftp.filesystem.ListingFile;
import com.coldcore.coloradoftp.filesystem.impl.ListingFileBean;
import com.coldcore.coloradoftp.session.Session;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jeeves.utils.Xml;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.Namespace;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * @see com.coldcore.coloradoftp.filesystem.FileSystem
 *
 * A filesystem implementation which sits over GeoNetwork catalog.
 */
public class GeoNetworkFileSystem implements FileSystem {

    public String getCurrentDirectory(Session userSession) throws FailedActionException {
        synchronized (GeoNetworkFileSystem.class) {
            log.debug("Called getCurrentDirectory, returning " + currentDir);
            return currentDir;
        }
    }

    public String getParent(String path, Session userSession) throws FailedActionException {
        synchronized (GeoNetworkFileSystem.class) {
            String parent = new File(currentDir).getParent();
            log.debug("Called getParent with " + path + ", returning " + parent);
            return parent;
        }
    }

    public String toAbsolute(String path, Session userSession) throws FailedActionException {
        log.debug("Called getAbsolute with " + path);
        throw new FailedActionException(FailedActionReason.NOT_IMPLEMENTED);
    }

    public ListingFile getPath(String path, Session userSession) throws FailedActionException {
        throw new FailedActionException(FailedActionReason.NOT_IMPLEMENTED);
    }

    public Set<ListingFile> listDirectory(String dir, Session userSession) throws FailedActionException {
        log.debug("Calling listDirectory with " + dir);
        if (dir.equals("/")) {
            return doSearchResults(dir, userSession);
        } else {
            return doUUID(dir, userSession);
        }
    }

    public String changeDirectory(String dir, Session userSession) throws FailedActionException {
        synchronized (GeoNetworkFileSystem.class) {
            if (dir.equals("..")) {
                currentDir = getParent(dir, userSession);
            } else if (dir.equals("/")) {
                currentDir = "/";
            } else {
                String uuid = extractUUIDFromDirPath(dir);
                currentDir = "/" + uuid;
            }
            log.debug("Called changeDirectory with " + dir + ", returning " + currentDir);
            return currentDir;
        }
    }

    public void deletePath(String path, Session userSession) throws FailedActionException {
        throw new FailedActionException(FailedActionReason.NOT_IMPLEMENTED);
    }

    public String createDirectory(String dir, Session userSession) throws FailedActionException {
        throw new FailedActionException(FailedActionReason.NOT_IMPLEMENTED);
    }

    public String renamePath(String from, String to, Session userSession) throws FailedActionException {
        throw new FailedActionException(FailedActionReason.NOT_IMPLEMENTED);
    }

    public ReadableByteChannel readFile(String filename, long position, Session userSession) throws FailedActionException {
        log.debug("trying to get " + filename + " from " + currentDir);
        String uuid = null;
        try {
            uuid = extractUUIDFromDirPath(filename);
        } catch (Exception e) {
            uuid = extractUUIDFromDirPath(currentDir);
        }
        Element md = searchUUIDS.get(uuid);
        Element info = md.getChild(GeoNetworkContext.GEONETINFO, GeoNetworkContext.GEONET_NAMESPACE);
        String id = info.getChildText("id");
        File file = new File(filename);
        String cfilename = getDir(GeoNetworkContext.dataDir, "private", id) + "/" + file.getName();
        if (filename.equals("metadata.xml") || filename.equals("license.html")) {
            Element disclaimer = getXmlFromGeoNetwork(GeoNetworkContext.url + "/" + GeoNetworkContext.disclaimerService + "?uuid=" + uuid + "&access=private", userSession);
            Element license = disclaimer.getChild("license");
            Element metadata = disclaimer.getChild("metadata");
            String output = null;
            if (filename.equals("license.html")) {
                if (license.getContentSize() > 0) {
                    output = Xml.getString((Element) license.getChildren().get(0));
                } else {
                    output = "No license";
                }
            }
            if (filename.equals("metadata.xml")) {
                output = Xml.getString((Element) metadata.getChildren().get(0));
            }
            ByteArrayInputStream input = new ByteArrayInputStream(output.getBytes());
            return Channels.newChannel(input);
        } else {
            try {
                FileInputStream fis = new FileInputStream(new File(cfilename));
                FileChannel fc = fis.getChannel();
                if (position > 0) return fc.position(position); else return fc;
            } catch (Exception e) {
                e.printStackTrace();
                throw new FailedActionException(FailedActionReason.SYSTEM_ERROR);
            }
        }
    }

    public WritableByteChannel saveFile(String filename, boolean append, Session userSession) throws FailedActionException {
        throw new FailedActionException(FailedActionReason.NOT_IMPLEMENTED);
    }

    public String getFileSeparator() {
        return "/";
    }

    private String currentDir = "/";

    private Map<String, Element> searchUUIDS = new HashMap<String, Element>();

    private static Logger log = Logger.getLogger(GeoNetworkFileSystem.class);

    private String extractUUIDFromDirPath(String dir) throws FailedActionException {
        String uuid = null;
        if (dir.contains("::") && dir.startsWith("/")) {
            uuid = dir.substring(1, dir.indexOf("::"));
        } else if (dir.startsWith("/")) {
            uuid = dir.substring(1);
        } else if (dir.endsWith("::")) {
            uuid = dir.substring(0, dir.indexOf("::"));
        } else {
            uuid = dir.trim();
        }
        log.debug("Checking for uuid of " + uuid);
        Element md = searchUUIDS.get(uuid);
        if (md == null) throw new FailedActionException(FailedActionReason.PATH_ERROR);
        return uuid;
    }

    /** Returns download links from the current search results 
		* @dir
		* @userSession
	  */
    private Set<ListingFile> doUUID(String dir, Session userSession) throws FailedActionException {
        String uuid = extractUUIDFromDirPath(dir);
        Element md = searchUUIDS.get(uuid);
        Element info = md.getChild(GeoNetworkContext.GEONETINFO, GeoNetworkContext.GEONET_NAMESPACE);
        Set<ListingFile> results = new HashSet<ListingFile>();
        Element disclaimer = getXmlFromGeoNetwork(GeoNetworkContext.url + "/" + GeoNetworkContext.disclaimerService + "?uuid=" + uuid + "&access=private", userSession);
        Element license = disclaimer.getChild("license");
        if (license != null && license.getContentSize() > 0) {
            ListingFileBean lf = new ListingFileBean();
            lf.setDirectory(false);
            lf.setName("license.html");
            lf.setAbsolutePath("/" + uuid + "/license.html");
            lf.setPermissions("r--r--r--");
            lf.setLastModified(new Date());
            lf.setOwner(info.getChildText("ownername"));
            String htmlStr = Xml.getString(license);
            lf.setSize(htmlStr.length());
            htmlStr = null;
            results.add(lf);
        }
        Element metadata = disclaimer.getChild("metadata");
        if (metadata != null && metadata.getContentSize() > 0) {
            ListingFileBean lf = new ListingFileBean();
            lf.setDirectory(false);
            lf.setName("metadata.xml");
            lf.setAbsolutePath("/" + uuid + "/metadata.xml");
            lf.setPermissions("r--r--r--");
            DateTimeFormatter bdt = ISODateTimeFormat.dateTimeParser();
            DateTime idt = bdt.parseDateTime(info.getChildText("changeDate"));
            lf.setLastModified(new Date(idt.getMillis()));
            lf.setOwner(info.getChildText("ownername"));
            String mdStr = Xml.getString(metadata);
            lf.setSize(mdStr.length());
            mdStr = null;
            results.add(lf);
        }
        Element resourceInfo = getXmlFromGeoNetwork(GeoNetworkContext.url + "/" + GeoNetworkContext.fileInfoService + "?uuid=" + uuid, userSession);
        for (int i = 0; i < resourceInfo.getContentSize(); i++) {
            Object o = resourceInfo.getContent(i);
            if (!(o instanceof Element)) continue;
            Element file = (Element) o;
            if (!file.getName().equals("link")) continue;
            String protocol = file.getAttributeValue("protocol");
            if (protocol.startsWith("WWW:DOWNLOAD") && protocol.contains("http--download")) {
                ListingFileBean lf = new ListingFileBean();
                lf.setDirectory(false);
                String name = file.getAttributeValue("name");
                lf.setName(name);
                lf.setAbsolutePath("/" + uuid + "/" + name);
                lf.setPermissions("r--r--r--");
                DateTimeFormatter bdt = ISODateTimeFormat.dateTimeParser();
                DateTime idt = bdt.parseDateTime(file.getAttributeValue("datemodified"));
                lf.setLastModified(new Date(idt.getMillis()));
                lf.setOwner(info.getChildText("ownername"));
                long size = -1;
                try {
                    size = Integer.parseInt(file.getAttributeValue("size"));
                } catch (Exception e) {
                    log.debug("File size (" + file.getAttributeValue("size") + ") for " + name + " is invalid");
                }
                lf.setSize(size);
                results.add(lf);
            }
        }
        return results;
    }

    /** Returns all records in the catalog 
	  * FIXME: Needs to call a GeoNetwork service that returns the bare 
		* minimum required - basically a set of uuids with title and whether the
		* metadata record has download data attached or not.
		* @dir
		* @userSession
	  */
    private Set<ListingFile> doSearchResults(String dir, Session userSession) throws FailedActionException {
        Element searchResults = getXmlFromGeoNetwork(GeoNetworkContext.url + "/" + GeoNetworkContext.searchService + "?any=*", userSession);
        Set<ListingFile> results = new HashSet<ListingFile>();
        for (int i = 0; i < searchResults.getContentSize(); i++) {
            Element md = (Element) searchResults.getContent(i);
            if (md.getName().equals("summary")) continue;
            ListingFileBean lf = new ListingFileBean();
            lf.setDirectory(true);
            Element info = md.getChild(GeoNetworkContext.GEONETINFO, GeoNetworkContext.GEONET_NAMESPACE);
            lf.setOwner(info.getChildText("ownername"));
            String uuid = info.getChildText("uuid");
            searchUUIDS.put(uuid, md);
            String title = md.getChildText("title").trim();
            title = title.replaceAll(" ++", " ");
            title = title.replaceAll("[\\n\\r]", "");
            title = title.replaceAll("/", "-");
            lf.setName(uuid + "::                  (" + title + ")");
            if (info.getChildText("download").equals("true")) {
                lf.setPermissions("r--r--r--");
            } else {
                lf.setPermissions("---------");
            }
            DateTimeFormatter bdt = ISODateTimeFormat.dateTimeParser();
            DateTime idt = bdt.parseDateTime(info.getChildText("changeDate"));
            lf.setLastModified(new Date(idt.getMillis()));
            lf.setAbsolutePath("/" + uuid);
            results.add((ListingFile) lf);
        }
        return results;
    }

    /** Returns XML from a GeoNetwork service.
		* @urlIn
	  */
    private Element getXmlFromGeoNetwork(String urlIn, Session userSession) throws FailedActionException {
        Element results = null;
        try {
            URL url = new URL(urlIn);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(1000);
            String cookie = (String) userSession.getAttribute("usercookie.object");
            if (cookie != null) conn.setRequestProperty("Cookie", cookie);
            BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
            try {
                results = Xml.loadStream(in);
            } finally {
                in.close();
            }
        } catch (Exception e) {
            throw new FailedActionException(FailedActionReason.SYSTEM_ERROR);
        }
        return results;
    }

    private String getDir(String dataDir, String access, String id) {
        String group = pad(Integer.parseInt(id) / 100, 3);
        String groupDir = group + "00-" + group + "99";
        String subDir = (access != null && access.equals("public")) ? "public" : "private";
        return dataDir + "/" + groupDir + "/" + id + "/" + subDir + "/";
    }

    private String pad(int group, int lenght) {
        String text = Integer.toString(group);
        while (text.length() < lenght) text = "0" + text;
        return text;
    }
}
