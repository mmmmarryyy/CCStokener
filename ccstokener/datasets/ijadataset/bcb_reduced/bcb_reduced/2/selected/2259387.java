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
import com.cidero.util.URLUtil;

/**
 * Playlist class. M3U playlists are used by convention to pass playlist
 * information from a server to a renderer in a 'lightweight' manner 
 * (no XML parsing required)
 *
 * M3U playlists can be as simple as a list of URL's, one per line, e.g.:
 *
 *   http://192.168.1.100:41752/media/music/tracks/song1.mpg
 *   http://192.168.1.100:41752/media/music/tracks/song2.mpg
 *
 * To provide support for basic Metadata, the Extended M3U format is
 * used. The syntax of an Extended M3U playlist is pretty basic.
 * The first line of the file contains the string '#EXTM3U', and
 * each playlist item is preceded by a metadata line with syntax:
 *
 *   #EXTINF:<playingTimeSecs>,<artist> - <title>
 * 
 * Note the spaces around the '-' separating the <artist>, <title> elements
 * are significant (required), though this class handles it if they are
 * missing.
 *
 * A sample of an EXTM3U file is show below:
 *  
 *   #EXTM3U
 *   #EXTINF:320,Rolling Stones - Tattoo You
 *   http://192.168.1.100:41752/media/music/tracks/Tattoo%20You.mpg
 *   #EXTINF:400,Rolling Stones - Start me up
 *   http://192.168.1.100:41752/media/music/tracks/Start%20Me%20Up.mpg
 * 
 * Notes:  
 *
 * It's not clear whether the format supports blank lines. This routine
 * handles blank lines just in case.
 *
 * If there is no '-' separator, the entire string after the duration is
 * taken to be the title, and the artist is set to the null string 
 * (Handle Twonkyvision server M3U's)
 *
 * When there is no extended data (MusicMatch UPnP media server as 
 * of 10/10/2004 does not support it), then this class constructs
 * a title from the trailing part of the playlist item pathname, minus
 * the suffix.  For example, if the playlist had a line like:
 *
 *   http://192.168.1.100:41752/media/music/tracks/Tattoo%20You.mpg
 *
 * the title field would be set to 'Tattoo You'.  The artist field 
 * is just set to 'Unknown' in this scenario
 *  
 *  
 */
public class M3UPlaylist {

    private static Logger logger = Logger.getLogger("com.cidero.upnp");

    static int CONNECT_TIMEOUT_MS = 10000;

    boolean hasExtendedInfo = false;

    boolean isAudioBroadcastPlaylist = false;

    CDSObjectList objList = new CDSObjectList();

    public M3UPlaylist() {
    }

    /**
   *  Read an M3U playlist from an input stream
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
    public M3UPlaylist(URL url) throws IOException, PlaylistException {
        set(url);
    }

    public M3UPlaylist(URL url, boolean isAudioBroadcastPlaylist) throws IOException, PlaylistException {
        this.isAudioBroadcastPlaylist = isAudioBroadcastPlaylist;
        set(url);
    }

    /**
   *  Read an M3U playlist from an input stream
   *
   *  @param     inputStream   Input stream 
   *
   *  @exception PlaylistException  
   *               If playlist was badly formatted, this exception is 
   *               returned. The items up to the point of the syntax
   *               error may still be usable 
   */
    public M3UPlaylist(InputStream inputStream) throws IOException, PlaylistException {
        set(inputStream);
    }

    public M3UPlaylist(Reader reader) throws IOException, PlaylistException {
        set(reader);
    }

    public void set(URL url) throws IOException, PlaylistException {
        logger.info("Opening playlist connection to: " + url);
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        logger.info("  Connecting");
        conn.connect();
        logger.info("  Connected!");
        set(conn.getInputStream());
    }

    public void set(InputStream inputStream) throws IOException, PlaylistException {
        set(new InputStreamReader(inputStream));
    }

    public void set(Reader reader) throws IOException, PlaylistException {
        BufferedReader bufReader = new BufferedReader(reader);
        String durationSecs;
        String artist;
        String title;
        String resourceName;
        String line = bufReader.readLine();
        if (line == null) throw new PlaylistException("Empty playlist");
        if (line.startsWith("#EXTM3U")) {
            hasExtendedInfo = true;
            line = getNonBlankLine(bufReader);
        } else {
            hasExtendedInfo = false;
        }
        if (objList == null) objList = new CDSObjectList(); else objList.clear();
        while (line != null) {
            artist = null;
            title = null;
            durationSecs = null;
            resourceName = null;
            if (hasExtendedInfo) {
                if (!line.startsWith("#EXTINF")) throw new PlaylistException("Playlist syntax error - missing #EXTINF line");
                String[] info = line.split("[:,]");
                if (info.length < 3) throw new PlaylistException("Playlist syntax error - corrupt #EXTINF line");
                durationSecs = info[1];
                int index = info[2].indexOf(" - ");
                if (index >= 0) {
                    artist = info[2].substring(0, index).trim();
                    title = info[2].substring(index + 3).trim();
                } else {
                    artist = null;
                    title = info[2].trim();
                }
                resourceName = getNonBlankLine(bufReader);
                if (resourceName == null) throw new PlaylistException("Playlist syntax error - unexpected EOF");
            } else {
                resourceName = line;
                if ((title = URLUtil.getPathTail(line)) == null) title = "Untitled";
            }
            CDSResource resource = new CDSResource();
            resource.setName(resourceName);
            resource.setProtocolInfoFromExtension("http-get:*:audio/mpeg:*");
            if (durationSecs != null) resource.setDurationSecs(Integer.parseInt(durationSecs));
            if (resource.getProtocolInfo().indexOf("audio") > 0) {
                if (isAudioBroadcastPlaylist) {
                    CDSAudioBroadcast audioBroadcast = new CDSAudioBroadcast();
                    audioBroadcast.setCreator(artist);
                    audioBroadcast.setTitle(title);
                    audioBroadcast.addResource(resource);
                    objList.add(audioBroadcast);
                } else {
                    CDSMusicTrack musicTrack = new CDSMusicTrack();
                    musicTrack.setArtist(artist);
                    musicTrack.setTitle(title);
                    musicTrack.addResource(resource);
                    objList.add(musicTrack);
                }
            } else {
                CDSPlaylistItem item = new CDSPlaylistItem();
                item.setArtist(artist);
                item.setTitle(title);
                item.addResource(resource);
                objList.add(item);
            }
            line = getNonBlankLine(bufReader);
        }
    }

    public M3UPlaylist(CDSObjectList objList) {
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
   * Convert to the string representation of the M3U format
   */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("#EXTM3U\n");
        for (int n = 0; n < objList.size(); n++) {
            CDSObject obj = objList.getObject(n);
            if (obj.isContainer()) continue;
            if (obj.getResourceCount() < 1) continue;
            if (obj.getResourceCount() > 1) {
                logger.warning("Resource count > 1 - using resource 0");
            }
            CDSResource res = obj.getResource(0);
            int durationSecs = res.getDurationSecs();
            buf.append("#EXTINF:" + durationSecs + ",");
            if (obj.getCreator() != null) buf.append(obj.getCreator() + " - ");
            buf.append(obj.getTitle() + "\n");
            buf.append(res.getName() + "\n");
        }
        return buf.toString();
    }

    /**
   * Simple test program
   */
    public static void main(String[] args) {
        StringBuffer buf = new StringBuffer();
        buf.append("#EXTM3U\n");
        buf.append("#EXTINF:320,Rolling Stones - Tattoo You\n");
        buf.append("http://192.168.1.100:41752/media/music/Tattoo%20You.mpg\n");
        buf.append("#EXTINF:380,Rolling Stones - Start Me Up\n");
        buf.append("http://192.168.1.100:41752/media/music/Start%20Me%20Up.mpg\n");
        StringReader reader = new StringReader(buf.toString());
        try {
            M3UPlaylist playlist = new M3UPlaylist(reader);
            if (playlist.hasExtendedInfo()) System.out.println("Playlist has extended info");
            CDSObjectList objList = playlist.getObjectList();
            System.out.println("Playlist as DIDL-Lite:\n" + objList.toString());
            System.out.println("\nPlaylist back to string:\n" + playlist.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
