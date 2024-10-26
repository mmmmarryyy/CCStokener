package com.cidero.upnp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.Properties;
import com.cidero.util.URLUtil;

/**
 * Playlist class for .pls playlist format 
 *
 * An example of .pls syntax:   (groovesalad.pls from www.somafm.com)
 *
 * [playlist]
 * numberofentries=3
 * File1=http://64.236.34.67:80/stream/1018
 * Title1=SomaFM Presents: Groove Salad 128k (Feed #1)
 * Length1=-1
 * File2=http://64.236.34.196:80/stream/1018
 * Title2=SomaFM Presents: Groove Salad 128k (Feed #2)
 * Length2=-1
 * File3=http://207.200.96.228:8076
 * Title3=SomaFM Presents: Groove Salad 128k (Feed #3)
 * Length3=-1
 * Version=2
 *
 * Notes:  
 *
 * It's not clear whether the format supports blank lines. This routine
 * handles blank lines just in case.
 *
 * I have seen Internet radio station playlists with 'numberofentries'
 * values greater than the actual number of entries in the file.
 */
public class PLSPlaylist {

    private static Logger logger = Logger.getLogger("com.cidero.upnp");

    boolean hasExtendedInfo = false;

    boolean isAudioBroadcastPlaylist = false;

    CDSObjectList objList = new CDSObjectList();

    public PLSPlaylist() {
    }

    /**
   *  Read an PLS playlist from an input stream
   *
   *  @param     url
   *
   *  @throws    PlaylistException  
   *               If playlist was badly formatted, this exception is 
   *               returned. The items up to the point of the syntax
   *               error may still be usable 
   *
   *  @throws    IOException  
   *               If there was an I/O error reading the playlist
   */
    public PLSPlaylist(URL url) throws IOException, PlaylistException {
        set(url);
    }

    public PLSPlaylist(URL url, boolean isAudioBroadcastPlaylist) throws IOException, PlaylistException {
        this.isAudioBroadcastPlaylist = isAudioBroadcastPlaylist;
        set(url);
    }

    /**
   *  Read an PLS playlist from an input stream
   *
   *  @param     inputStream   Input stream 
   *
   *  @exception PlaylistException  
   *               If playlist was badly formatted, this exception is 
   *               returned. The items up to the point of the syntax
   *               error may still be usable 
   */
    public PLSPlaylist(InputStream inputStream) throws IOException, PlaylistException {
        set(inputStream);
    }

    public PLSPlaylist(Reader reader) throws IOException, PlaylistException {
        set(reader);
    }

    public void set(URL url) throws IOException, PlaylistException {
        logger.fine("Opening playlist connection to: " + url);
        URLConnection conn = url.openConnection();
        logger.fine("  Connecting");
        conn.connect();
        set(conn.getInputStream());
    }

    public void set(InputStream inputStream) throws IOException, PlaylistException {
        set(new InputStreamReader(inputStream));
    }

    public void set(Reader reader) throws IOException, PlaylistException {
        BufferedReader bufReader = new BufferedReader(reader);
        String line = getNonBlankLine(bufReader);
        if (line == null) throw new PlaylistException("Empty playlist");
        if (!line.toLowerCase().startsWith("[playlist]")) throw new PlaylistException("Syntax error (1st line)");
        objList.clear();
        line = getNonBlankLine(bufReader);
        Properties props = new Properties();
        while (line != null) {
            int index = line.indexOf("=");
            if (index >= 0) {
                String name = line.substring(0, index);
                String value = line.substring(index + 1).trim();
                props.setProperty(name.toLowerCase(), value.toLowerCase());
            } else {
                logger.warning("Playlist syntax error - no '=' in line: " + line);
            }
            line = getNonBlankLine(bufReader);
        }
        String propVal = props.getProperty("numberofentries");
        if (propVal == null) throw new PlaylistException("Syntax error (missing 'numberofentries')");
        int nEntries = Integer.parseInt(propVal);
        for (int n = 0; n < nEntries; n++) {
            String resourceName = props.getProperty("file" + (n + 1));
            if (resourceName == null) {
                logger.warning("Syntax error (missing file" + (n + 1) + ")");
                continue;
            }
            String title = props.getProperty("title" + (n + 1));
            if (title == null) {
                logger.warning("Syntax error (missing title" + (n + 1) + ")");
                title = "Playlist Entry #" + (n + 1);
            }
            CDSResource resource = new CDSResource();
            resource.setName(resourceName);
            resource.setProtocolInfoFromExtension("http-get:*:audio/mpeg:*");
            if (resource.getProtocolInfo().indexOf("audio") > 0) {
                if (isAudioBroadcastPlaylist) {
                    CDSAudioBroadcast audioBroadcast = new CDSAudioBroadcast();
                    audioBroadcast.setTitle(title);
                    audioBroadcast.addResource(resource);
                    objList.add(audioBroadcast);
                } else {
                    CDSMusicTrack musicTrack = new CDSMusicTrack();
                    musicTrack.setTitle(title);
                    musicTrack.addResource(resource);
                    objList.add(musicTrack);
                }
            } else {
                CDSPlaylistItem item = new CDSPlaylistItem();
                item.setTitle(title);
                item.addResource(resource);
                objList.add(item);
            }
        }
    }

    public PLSPlaylist(CDSObjectList objList) {
        this.objList = objList;
    }

    public boolean hasExtendedInfo() {
        return hasExtendedInfo;
    }

    public void add(CDSPlaylistItem item) {
        objList.add(item);
    }

    public int size() {
        return objList.size();
    }

    public CDSItem getPlaylistItem(int n) {
        return (CDSItem) objList.get(n);
    }

    public CDSObjectList getObjectList() {
        return objList;
    }

    public String getNonBlankLine(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmedLine = line.trim();
            if (trimmedLine.length() > 0) return trimmedLine;
        }
        return null;
    }

    /**
   * Convert to the string representation of the PLS format
   */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("[playlist]\n");
        buf.append("numberofentries=" + objList.size() + "\n");
        for (int n = 0; n < objList.size(); n++) {
            CDSObject obj = objList.getObject(n);
            if (obj.isContainer()) continue;
            if (obj.getResourceCount() < 1) continue;
            if (obj.getResourceCount() > 1) {
                logger.warning("Resource count > 1 - using resource 0");
            }
            CDSResource res = obj.getResource(0);
            buf.append("File" + (n + 1) + "=" + res.getName() + "\n");
            buf.append("Title" + (n + 1) + "=" + obj.getTitle() + "\n");
            buf.append("Length" + (n + 1) + "=-1" + "\n");
        }
        buf.append("Version=2\n");
        return buf.toString();
    }

    /**
   * Simple test program
   */
    public static void main(String[] args) {
        StringBuffer buf = new StringBuffer();
        buf.append("[playlist]\n");
        buf.append("numberofentries=2\n");
        buf.append("File1=http://192.168.1.100:41752/music/Tattoo%20You.mpg\n");
        buf.append("Title1=Tattoo You\n");
        buf.append("File2=http://192.168.1.100:41752/music/Start%20Me%20Up.mpg\n");
        buf.append("Title2=Start Me Up\n");
        buf.append("Version=2\n");
        StringReader reader = new StringReader(buf.toString());
        try {
            PLSPlaylist playlist = new PLSPlaylist(reader);
            CDSObjectList objList = playlist.getObjectList();
            System.out.println("Playlist as DIDL-Lite:\n" + objList.toString());
            System.out.println("\nPlaylist back to string:\n" + playlist.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
