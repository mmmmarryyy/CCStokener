package com.filesearch;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import com.filesearch.FileManager;

public class LinkParser {

    private static Logger _log = null;

    private ArrayList<String> excludeTypes = new ArrayList<String>();

    private ArrayList<String> includeTypes = new ArrayList<String>();

    private ArrayList<String> excludeTerms = new ArrayList<String>();

    private ArrayList<String> includeTerms = new ArrayList<String>();

    private long availMem = 0;

    private int resourceLimit = 1;

    public void setAvailMem(long value) {
        availMem = value;
    }

    public void setResourceLimit(int value) {
        resourceLimit = value;
    }

    public void setExcludeTypes(ArrayList<String> vals) {
        excludeTypes = vals;
    }

    public void setLogger(Logger log) {
        _log = log;
    }

    public void setIncludeTypes(ArrayList<String> vals) {
        includeTypes = vals;
    }

    public void setExcludeTerms(ArrayList<String> vals) {
        excludeTerms = vals;
    }

    public void setIncludeTerms(ArrayList<String> vals) {
        includeTerms = vals;
    }

    public static String downloadFile(String fileURL, String localFileName, String compareFolder, int minFileSize) throws Exception {
        OutputStream out = null;
        String status = "";
        Object[] urlBOS = new Object[4];
        ByteArrayOutputStream bos = null;
        try {
            urlBOS = LinkParser.getURLStream(fileURL);
            Integer code = (Integer) urlBOS[1];
            if (code < 400) {
                bos = (ByteArrayOutputStream) urlBOS[2];
                urlBOS = null;
                if (bos.toByteArray().length >= minFileSize) {
                    if (!FileManager.fileIsDuplicate(bos.toByteArray(), localFileName, compareFolder)) {
                        _log.debug("File Size=" + Integer.toString(bos.size()));
                        out = new FileOutputStream(localFileName);
                        out.write(bos.toByteArray());
                        status = "OK";
                    } else {
                        status = "Duplicate file.";
                    }
                } else {
                    status = "File too small. size=" + Integer.toString(bos.toByteArray().length);
                }
            } else {
                if (code != 499) {
                    status = "Bad URL: response=" + code.toString();
                } else {
                    status = "No Connection";
                }
            }
        } catch (Exception e) {
            throw new Exception("Could not download file " + fileURL + " ERROR: " + e.getMessage());
        } finally {
            if (out != null) {
                out.close();
            }
        }
        urlBOS = null;
        bos = null;
        return status;
    }

    public static Object[] getURLStream(String address) throws Exception {
        URLConnection conn = null;
        InputStream in = null;
        Object[] urlBOS = new Object[4];
        Object[] urlResponse = new Object[3];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Integer code = 0;
        String newURL = address;
        String host = "";
        try {
            URL url = new URL(newURL);
            conn = url.openConnection();
            conn.setConnectTimeout(30000);
            urlResponse = getURLResponse(conn);
            newURL = (String) urlResponse[0];
            code = (Integer) urlResponse[1];
            host = (String) urlResponse[2];
            urlResponse = null;
            if (code < 400) {
                in = conn.getInputStream();
                byte[] buffer = new byte[1024];
                int numRead;
                long numWritten = 0;
                while ((numRead = in.read(buffer)) != -1) {
                    numWritten += numRead;
                    if (resourcesFree(numWritten)) {
                        bos.write(buffer, 0, numRead);
                    } else {
                        throw new Exception("Not enough available memory.");
                    }
                }
                buffer = null;
            }
        } catch (Exception e) {
            throw new Exception("Could not download url " + newURL + " ERROR: " + e.getMessage());
        } finally {
            urlBOS[0] = newURL;
            urlBOS[1] = code;
            urlBOS[2] = bos;
            urlBOS[3] = host;
            bos = null;
            in = null;
        }
        return urlBOS;
    }

    public static Object[] getURLResponse(URLConnection conn) throws Exception {
        Object[] urlResponse = new Object[3];
        int response = 0;
        String location = conn.getURL().toString();
        HttpURLConnection.setFollowRedirects(false);
        String host = "";
        try {
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setConnectTimeout(30000);
            httpConn.connect();
            response = httpConn.getResponseCode();
            location = conn.getURL().toString();
            host = httpConn.getRequestProperty("Host");
            if (!host.toLowerCase().startsWith("http://", 0)) {
                host = "http://" + host;
            }
            if (!location.toLowerCase().contains(host.toLowerCase())) {
                response = 301;
            }
            if (response == 301 || response == 302 || response == 303 || response == 307) {
                location = httpConn.getHeaderField("Location");
                if (!location.toLowerCase().startsWith("http://", 0)) {
                    if (!host.endsWith("/")) {
                        host = host + "/";
                    }
                    location = host + location;
                }
            }
        } catch (UnknownHostException e) {
            response = 499;
        } catch (ConnectException e) {
            response = 499;
        } catch (Exception e) {
            throw new Exception("--->LinkParser.getURLResponse: url: " + location + " error: " + e.toString());
        } finally {
            urlResponse[0] = location;
            urlResponse[1] = response;
            urlResponse[2] = host;
            HttpURLConnection.setFollowRedirects(true);
        }
        return urlResponse;
    }

    public String getLinks(String strHTML, String hostURL) throws Exception {
        String links = new String();
        try {
            links = parseLinks(strHTML, "http://", links, hostURL);
            links = parseLinks(strHTML, "href=\"", links, hostURL);
            links = parseLinks(strHTML, "href='", links, hostURL);
        } catch (Exception e) {
            throw new Exception("--->LinkParser.getLinks(): " + e.getMessage());
        }
        links = links.replace("|", "\n");
        return links.replaceFirst("\n", "");
    }

    public String parseLinks(String strHTML, String searchText, String links, String hostURL) throws Exception {
        int intStart = 0;
        int intEnd = 0;
        String tmpLinks = links;
        String strLink = "";
        try {
            intStart = strHTML.indexOf(searchText);
            while (intStart > 0) {
                intEnd = 0;
                int intPos = intStart + searchText.length();
                String chars = "";
                strLink = "";
                while (intEnd == 0 && intPos < strHTML.length()) {
                    chars = strHTML.substring(intPos, intPos + 1);
                    if (chars.equals("\"") || chars.equals("'") || chars.equals(" ") || chars.equals(">") || chars.equals("<")) {
                        intEnd = intStart + strLink.length();
                        break;
                    }
                    strLink += chars;
                    intPos++;
                }
                if (intEnd > 0) {
                    if (!linkIsExcluded(strLink)) {
                        strLink = cleanupURL(strLink);
                        if (searchText.equals("http://")) {
                            strLink = searchText + strLink;
                        }
                        intStart = strLink.indexOf("http://", 7);
                        if (intStart > 0) {
                            strLink = strLink.substring(intStart);
                        } else if (!strLink.startsWith("http://")) {
                            if (hostURL.endsWith("/") && strLink.startsWith("/")) {
                                strLink = cleanupLink(hostURL, strLink);
                            } else if (!hostURL.endsWith("/") && !strLink.startsWith("/")) {
                                strLink = hostURL + "/" + strLink;
                            } else {
                                strLink = hostURL + strLink;
                            }
                        }
                        if (strLink.toLowerCase().startsWith("http://")) {
                            if (!tmpLinks.contains("|" + strLink + "|") && !tmpLinks.endsWith("|" + strLink)) {
                                tmpLinks += "|" + strLink;
                            }
                        }
                    }
                    intStart = strHTML.indexOf(searchText, intEnd);
                    if (intStart == intEnd) {
                        intStart = 0;
                    }
                } else {
                    intStart = 0;
                }
            }
        } catch (Exception e) {
            throw new Exception("--->LinkParser.parse(): " + e.getMessage());
        }
        return tmpLinks;
    }

    public static String cleanupLink(String host, String link) {
        String cleanLink = link;
        for (int i = host.length(); i > 0; i--) {
            if (link.toLowerCase().startsWith(host.substring(i, host.length()))) {
                cleanLink = host + link.replaceFirst(host.substring(i, host.length()), "");
            }
        }
        return cleanLink;
    }

    public static String cleanupURL(String strURL) {
        String strTemp = strURL;
        strTemp = strTemp.replaceAll("%3a", ":");
        strTemp = strTemp.replaceAll("%2f", "/");
        strTemp = strTemp.replaceAll("%3f", "?");
        strTemp = strTemp.replaceAll("%3d", "=");
        return strTemp;
    }

    public boolean linkIsRelevant(String link) {
        boolean ok = false;
        String keyWord = new String();
        for (int i = 0; i < includeTerms.size(); i++) {
            keyWord = (String) includeTerms.get(i);
            if (link.contains(keyWord)) {
                _log.info("Keyword found in link: \"" + link + "\" Keyword=\"" + keyWord + "\"");
                ok = true;
                break;
            }
        }
        return ok;
    }

    public boolean pageIsRelevant(String html) {
        boolean ok = false;
        String title = getTitle(html);
        String keyWord = "";
        for (int i = 0; i < includeTerms.size(); i++) {
            keyWord = (String) includeTerms.get(i);
            if (title.toLowerCase().contains(keyWord.toLowerCase())) {
                _log.info("Keyword found in title: \"" + title + "\" Keyword=\"" + keyWord + "\"");
                ok = true;
                break;
            }
        }
        if (ok) {
            for (int i = 0; i < excludeTerms.size(); i++) {
                keyWord = (String) excludeTerms.get(i);
                if (title.toLowerCase().contains(keyWord.toLowerCase())) {
                    _log.info("Excluded Terms in Title: " + title + ": Keyword=\"" + keyWord + "\"");
                    ok = false;
                    break;
                }
            }
        }
        if (ok) {
            for (int i = 0; i < excludeTypes.size(); i++) {
                keyWord = (String) excludeTypes.get(i);
                if (title.toLowerCase().endsWith(keyWord.toLowerCase())) {
                    _log.info("Excluded Terms in Title: " + title + ": Keyword=\"" + keyWord + "\"");
                    ok = false;
                    break;
                }
            }
        }
        return ok;
    }

    public String getMetaContent(String html) {
        String content = "";
        try {
            String metaTag = html.toLowerCase().substring(html.toLowerCase().indexOf("<meta name=\"keywords\" content="));
            metaTag = metaTag.substring(0, html.indexOf("\">"));
            content = metaTag;
        } catch (Exception e) {
        }
        return content;
    }

    public String getTitle(String html) {
        String content = "";
        try {
            String title = html.substring(html.indexOf("<title>"), html.indexOf("</title>"));
            content = title.replace("<title>", "");
        } catch (Exception e) {
        }
        return content;
    }

    public boolean linkIsExcluded(String link) {
        boolean excluded = false;
        String term = new String();
        for (int i = 0; i < excludeTerms.size(); i++) {
            term = (String) excludeTerms.get(i);
            if (link.toLowerCase().contains(term.toLowerCase())) {
                _log.info("Excluded URL: " + link + ": Keyword=\"" + term + "\"");
                excluded = true;
                break;
            }
        }
        if (!excluded) {
            for (int i = 0; i < excludeTypes.size(); i++) {
                term = (String) excludeTypes.get(i);
                if (link.toLowerCase().endsWith(term.toLowerCase())) {
                    _log.info("Excluded URL: " + link + ": Keyword=\"" + term + "\"");
                    excluded = true;
                    break;
                }
            }
        }
        return excluded;
    }

    public boolean linkIsFileURL(String link) {
        boolean ok = false;
        for (int i = 0; i < includeTypes.size(); i++) {
            if (link.toLowerCase().endsWith((String) includeTypes.get(i).toLowerCase())) {
                ok = true;
                break;
            }
        }
        return ok;
    }

    private static boolean resourcesFree(long length) {
        boolean ok = true;
        long freeMem = (Runtime.getRuntime().freeMemory() + (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory()));
        if (freeMem < length) {
            ok = false;
        }
        return ok;
    }
}
