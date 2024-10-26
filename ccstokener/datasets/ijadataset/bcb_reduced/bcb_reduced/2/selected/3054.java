package org.das2.util.filesystem;

import java.util.logging.Level;
import org.das2.util.monitor.CancelledOperationException;
import org.das2.util.Base64;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author  Jeremy
 */
public class HtmlUtil {

    public static boolean isDirectory(URL url) {
        String file = url.getFile();
        return file.charAt(file.length() - 1) != '/';
    }

    /**
     * Get the listing of the web directory, returning links that are "under" the given URL.
     * Note this does not handle off-line modes where we need to log into
     * a website first, as is often the case for a hotel.
     *
     * This was refactored to support caching of listings by simply writing the content to disk.
     *
     * @param url
     * @return list of URIs referred to in the page.
     * @throws IOException
     * @throws CancelledOperationException
     */
    public static URL[] getDirectoryListing(URL url, InputStream urlStream) throws IOException, CancelledOperationException {
        byte b[] = new byte[10000];
        int numRead = urlStream.read(b);
        StringBuilder contentBuffer = new StringBuilder(10000);
        if (numRead != -1) contentBuffer.append(new String(b, 0, numRead));
        while (numRead != -1) {
            FileSystem.logger.finest("download listing");
            numRead = urlStream.read(b);
            if (numRead != -1) {
                String newContent = new String(b, 0, numRead);
                contentBuffer.append(newContent);
            }
        }
        urlStream.close();
        String content = contentBuffer.toString();
        String hrefRegex = "(?i)href\\s*=\\s*([\"'])(.+?)\\1";
        Pattern hrefPattern = Pattern.compile(hrefRegex);
        Matcher matcher = hrefPattern.matcher(content);
        ArrayList urlList = new ArrayList();
        while (matcher.find()) {
            FileSystem.logger.finest("parse listing");
            String strLink = matcher.group(2);
            URL urlLink = null;
            try {
                urlLink = new URL(url, strLink);
                strLink = urlLink.toString();
            } catch (MalformedURLException e) {
                System.err.println("bad URL: " + url + " " + strLink);
                continue;
            }
            if (urlLink.toString().startsWith(url.toString()) && null == urlLink.getQuery()) {
                urlList.add(urlLink);
            }
        }
        return (URL[]) urlList.toArray(new URL[urlList.size()]);
    }

    /**
     * Get the listing of the web directory, returning links that are "under" the given URL.
     * Note this does not handle off-line modes where we need to log into
     * a website first, as is often the case for a hotel.
     * TODO: check for 302 redirect that Holiday Inn used to get credentials page
     * @param url
     * @return list of URIs referred to in the page.
     * @throws IOException
     * @throws CancelledOperationException
     */
    public static URL[] getDirectoryListing(URL url) throws IOException, CancelledOperationException {
        FileSystem.logger.log(Level.FINER, "listing {0}", url);
        String file = url.getFile();
        if (file.charAt(file.length() - 1) != '/') {
            url = new URL(url.toString() + '/');
        }
        String userInfo = KeyChain.getDefault().getUserInfo(url);
        URLConnection urlConnection = url.openConnection();
        urlConnection.setAllowUserInteraction(false);
        urlConnection.setConnectTimeout(FileSystem.settings().getConnectTimeoutMs());
        if (userInfo != null) {
            String encode = Base64.encodeBytes(userInfo.getBytes());
            urlConnection.setRequestProperty("Authorization", "Basic " + encode);
        }
        InputStream urlStream;
        urlStream = urlConnection.getInputStream();
        return getDirectoryListing(url, urlStream);
    }
}
