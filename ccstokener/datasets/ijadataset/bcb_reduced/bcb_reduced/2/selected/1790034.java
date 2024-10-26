package org.p2s.lib;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.p2s.data.Artist;
import org.p2s.data.Release;
import org.p2s.data.Track;

public class MusicBrainz {

    public static ArrayList searchArtist(String query) throws Exception {
        ArrayList ret = new ArrayList();
        String url = "http://musicbrainz.org/ws/1/artist/?type=xml&name=" + query;
        HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
        XmlNode node = XmlLoader.load(conn.getInputStream());
        node = node.getFirstChild("artist-list");
        if (node != null) {
            XmlNode tracks[] = node.getChild("artist");
            for (int i = 0; i < tracks.length; i++) {
                String id = tracks[i].getAttribute("id");
                String name = tracks[i].getFirstChild("name").getText();
                Artist artist = new Artist();
                artist.id = id;
                artist.name = name;
                ret.add(artist);
            }
        }
        return ret;
    }

    public static Artist artistAux(String id, String q) throws Exception {
        String url = "http://musicbrainz.org/ws/1/artist/" + id + "?type=xml&inc=" + q;
        HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
        XmlNode node = XmlLoader.load(conn.getInputStream());
        node = node.getFirstChild("artist");
        Artist artist = new Artist();
        artist.id = id;
        artist.name = node.getFirstChild("name").getText();
        artist.album = new ArrayList();
        HashSet set = new HashSet();
        try {
            XmlNode releases[] = node.getFirstChild("release-list").getChild("release");
            for (int i = 0; i < releases.length; i++) {
                String rid = releases[i].getAttribute("id");
                String title = releases[i].getFirstChild("title").getText();
                Release release = new Release();
                release.id = rid;
                release.name = title;
                artist.album.add(release);
                set.add(title);
            }
        } catch (NullPointerException e) {
        }
        return artist;
    }

    public static Artist artist(String id) throws Exception {
        Artist artist = artistAux(id, "sa-Album");
        artist.compilation = artistAux(id, "sa-Compilation").album;
        artist.single = artistAux(id, "sa-Single").album;
        artist.ep = artistAux(id, "sa-EP").album;
        return artist;
    }

    static HashMap releases = null;

    public static Release getRelease(String id) throws Exception {
        if (releases == null) releases = new HashMap();
        Release release = (Release) releases.get(id);
        if (release == null) {
            release = releaseNonCache(id);
            releases.put(id, release);
        }
        return release;
    }

    public static Release releaseNonCache(String id) throws Exception {
        String url = "http://musicbrainz.org/ws/1/release/" + id + "?type=xml&inc=tracks";
        HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
        XmlNode node = XmlLoader.load(conn.getInputStream());
        node = node.getFirstChild("release");
        String title = node.getFirstChild("title").getText();
        Release release = new Release();
        XmlNode asin = node.getFirstChild("asin");
        if (asin != null) release.asin = asin.getText();
        release.id = id;
        release.name = title;
        release.tracks = new ArrayList();
        XmlNode releases[] = node.getFirstChild("track-list").getChild("track");
        System.out.println(releases.length);
        for (int i = 0; i < releases.length; i++) {
            String rid = releases[i].getAttribute("id");
            String ttitle = releases[i].getFirstChild("title").getText();
            String tduration = releases[i].getFirstChild("duration").getText();
            Track track = new Track();
            track.id = rid;
            track.name = ttitle;
            if (tduration != null && tduration.length() > 1) track.duration = Integer.parseInt(tduration) / 1000;
            release.tracks.add(track);
        }
        return release;
    }

    public static Track track(String id) throws Exception {
        String url = "http://musicbrainz.org/ws/1/track/" + id + "?type=xml&inc=artist+puids";
        HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
        XmlNode node = XmlLoader.load(conn.getInputStream());
        node = node.getFirstChild("track");
        String title = node.getFirstChild("title").getText();
        Track track = new Track();
        track.id = id;
        track.name = title;
        XmlNode artist = node.getFirstChild("artist");
        track.artist = new Artist();
        track.artist.id = artist.getAttribute("id");
        track.artist.name = artist.getFirstChild("name").getText();
        track.puids = new ArrayList();
        try {
            XmlNode puids[] = node.getFirstChild("puid-list").getChild("puid");
            for (int i = 0; i < puids.length; i++) {
                track.puids.add(puids[i].getAttribute("id"));
            }
        } catch (NullPointerException e) {
        }
        return track;
    }

    public static String puidToMbid(String id) throws Exception {
        String url = "http://musicbrainz.org/ws/1/track/?type=xml&puid=" + id;
        HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
        XmlNode node = XmlLoader.load(conn.getInputStream());
        node = node.getFirstChild("track-list");
        if (node != null) {
            node = node.getFirstChild("track");
            if (node != null) {
                return node.getAttribute("id");
            }
        }
        return "00000000-0000-0000-0000-000000000000";
    }
}
