package gmusic.mb.services.impl;

import gmusic.mb.bo.ArtistMBBean;
import gmusic.mb.bo.ReleaseMBBean;
import gmusic.mb.bo.TrackMBBean;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

public class MusicBrainzXMLParser {

    private static final Logger log = Logger.getLogger(MusicBrainzXMLParser.class);

    private Document doc = null;

    public MusicBrainzXMLParser(File f) throws DocumentException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        SAXReader reader = new SAXReader();
        doc = reader.read(br);
        br.close();
    }

    public MusicBrainzXMLParser(URL url) throws DocumentException, IOException {
        URLConnection urlConnection = url.openConnection();
        InputStream httpStream = urlConnection.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(httpStream, "ISO-8859-1"));
        SAXReader reader = new SAXReader();
        doc = reader.read(br);
        br.close();
    }

    public MusicBrainzXMLParser(BufferedReader br) throws DocumentException, IOException {
        SAXReader reader = new SAXReader();
        doc = reader.read(br);
        br.close();
    }

    private Element getRootElement() {
        return doc.getRootElement();
    }

    private Element getArtistListElement() {
        Element rootElem = getRootElement();
        Element elem = null;
        for (int i = 0; i < rootElem.nodeCount(); i++) {
            Node node = rootElem.node(i);
            if (node instanceof Element && ((Element) node).getName().equalsIgnoreCase("artist-list")) {
                elem = (Element) node;
            }
        }
        return elem;
    }

    public List<Element> getArtistsElement() {
        List<Element> artists = new ArrayList<Element>();
        Element artistListElem = getArtistListElement();
        for (int i = 0; i < artistListElem.nodeCount(); i++) {
            Node node = artistListElem.node(i);
            if (node instanceof Element && ((Element) node).getName().equalsIgnoreCase("artist")) {
                artists.add((Element) node);
            }
        }
        return artists;
    }

    public Element getArtistElement() {
        Element artist = null;
        Element rootElem = getRootElement();
        for (int i = 0; i < rootElem.nodeCount(); i++) {
            Node node = rootElem.node(i);
            if (node instanceof Element && ((Element) node).getName().equalsIgnoreCase("artist")) {
                artist = (Element) node;
                break;
            }
        }
        return artist;
    }

    private ArtistMBBean getArtistBean(Element e) {
        ArtistMBBean artist = new ArtistMBBean();
        log.debug("mbid = " + e.valueOf("@id"));
        artist.setMbid(e.valueOf("@id"));
        try {
            log.debug("Score = " + e.valueOf("@ext:score") + "%");
            artist.setScore(Integer.parseInt(e.valueOf("@ext:score")));
        } catch (Exception ex) {
        }
        for (int i = 0; i < e.nodeCount(); i++) {
            Node node = e.node(i);
            if (node instanceof Element && ((Element) node).getName().equalsIgnoreCase("name")) {
                artist.setNom((String) ((Element) node).getData());
                log.debug("Name = " + ((Element) node).getData());
            }
        }
        return artist;
    }

    public List<ArtistMBBean> getArtists() {
        List<ArtistMBBean> artists = new ArrayList<ArtistMBBean>();
        List<Element> artistsElem = getArtistsElement();
        for (int i = 0; i < artistsElem.size(); i++) {
            artists.add(getArtistBean(artistsElem.get(i)));
        }
        return artists;
    }

    public ArtistMBBean getArtist() {
        Element artistElem = getArtistElement();
        getArtistBean(artistElem);
        return getArtistBean(artistElem);
    }

    private Element getReleaseListElement() {
        Element rootElem = getRootElement();
        Element elem = null;
        for (int i = 0; i < rootElem.nodeCount(); i++) {
            Node node = rootElem.node(i);
            if (node instanceof Element && ((Element) node).getName().equalsIgnoreCase("release-list")) {
                elem = (Element) node;
            }
        }
        return elem;
    }

    private List<Element> getReleasesElement() {
        List<Element> artists = new ArrayList<Element>();
        Element artistListElem = getReleaseListElement();
        for (int i = 0; i < artistListElem.nodeCount(); i++) {
            Node node = artistListElem.node(i);
            if (node instanceof Element && ((Element) node).getName().equalsIgnoreCase("release")) {
                artists.add((Element) node);
            }
        }
        return artists;
    }

    private Element getReleaseElement() {
        Element artist = null;
        Element rootElem = getRootElement();
        for (int i = 0; i < rootElem.nodeCount(); i++) {
            Node node = rootElem.node(i);
            if (node instanceof Element && ((Element) node).getName().equalsIgnoreCase("release")) {
                artist = (Element) node;
                break;
            }
        }
        return artist;
    }

    private int getAnneeRelease(Element e) {
        int annee = -1;
        for (int i = 0; i < e.nodeCount(); i++) {
            Node node = e.node(i);
            if (node instanceof Element && ((Element) node).getName().equalsIgnoreCase("event")) {
                String dateS = ((Element) node).valueOf("@date");
                try {
                    int date = Integer.parseInt(dateS.substring(0, 4));
                    if (annee == -1 || annee > date) {
                        annee = date;
                    }
                } catch (Exception ex) {
                }
            }
        }
        return annee;
    }

    private ReleaseMBBean getReleaseBean(Element e) {
        ReleaseMBBean release = new ReleaseMBBean();
        log.debug("mbid = " + e.valueOf("@id"));
        release.setMbid(e.valueOf("@id"));
        log.debug("Type = " + e.valueOf("@type") + "%");
        release.setTypeMB((e.valueOf("@type")));
        for (int i = 0; i < e.nodeCount(); i++) {
            Node node = e.node(i);
            if (node instanceof Element) {
                if (((Element) node).getName().equalsIgnoreCase("title")) {
                    release.setNom((String) ((Element) node).getData());
                    log.debug("Title = " + ((Element) node).getData());
                } else if (((Element) node).getName().equalsIgnoreCase("release-event-list")) {
                    release.setAnnee(getAnneeRelease((Element) node));
                }
            }
        }
        return release;
    }

    public List<ReleaseMBBean> getReleases() {
        List<ReleaseMBBean> releases = new ArrayList<ReleaseMBBean>();
        List<Element> releasesElem = getReleasesElement();
        for (int i = 0; i < releasesElem.size(); i++) {
            releases.add(getReleaseBean(releasesElem.get(i)));
        }
        return releases;
    }

    public ReleaseMBBean getRelease() {
        Element releaseElem = getReleaseElement();
        return getReleaseBean(releaseElem);
    }

    private Element getTrackListElement() {
        Element rootElem = getRootElement();
        Element elem = null;
        for (int i = 0; i < rootElem.nodeCount(); i++) {
            Node node = rootElem.node(i);
            if (node instanceof Element && ((Element) node).getName().equalsIgnoreCase("track-list")) {
                elem = (Element) node;
            }
        }
        return elem;
    }

    private List<Element> getTracksElement() {
        List<Element> tracks = new ArrayList<Element>();
        Element releaseListElem = getTrackListElement();
        for (int i = 0; i < releaseListElem.nodeCount(); i++) {
            Node node = releaseListElem.node(i);
            if (node instanceof Element && ((Element) node).getName().equalsIgnoreCase("track")) {
                tracks.add((Element) node);
            }
        }
        return tracks;
    }

    private Element getTrackElement() {
        Element track = null;
        Element rootElem = getRootElement();
        for (int i = 0; i < rootElem.nodeCount(); i++) {
            Node node = rootElem.node(i);
            if (node instanceof Element && ((Element) node).getName().equalsIgnoreCase("track")) {
                track = (Element) node;
                break;
            }
        }
        return track;
    }

    private TrackMBBean getTrackBean(Element e) {
        TrackMBBean track = new TrackMBBean();
        log.debug("mbid = " + e.valueOf("@id"));
        track.setMbid(e.valueOf("@id"));
        for (int i = 0; i < e.nodeCount(); i++) {
            Node node = e.node(i);
            if (node instanceof Element) {
                if (((Element) node).getName().equalsIgnoreCase("title")) {
                    track.setNom((String) ((Element) node).getData());
                    log.debug("Title = " + ((Element) node).getData());
                } else if (((Element) node).getName().equalsIgnoreCase("duration")) {
                    track.setDuree((int) (Integer.parseInt((String) ((Element) node).getData()) / 1000));
                }
            }
        }
        return track;
    }

    public List<TrackMBBean> getTracks() {
        List<TrackMBBean> tracks = new ArrayList<TrackMBBean>();
        List<Element> releasesElem = getTracksElement();
        for (int i = 0; i < releasesElem.size(); i++) {
            tracks.add(getTrackBean(releasesElem.get(i)));
        }
        return tracks;
    }

    public TrackMBBean getTrack() {
        Element releaseElem = getTrackElement();
        return getTrackBean(releaseElem);
    }
}
