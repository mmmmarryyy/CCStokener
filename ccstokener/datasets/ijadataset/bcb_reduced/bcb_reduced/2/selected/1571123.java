package plugins.series;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import com.umc.beans.persons.Actor;
import com.umc.beans.persons.IPerson;
import com.umc.plugins.series.IPluginSeries;

public class TheTVDB implements IPluginSeries {

    private boolean cacheData = false;

    private static Logger log = Logger.getLogger("com.umc.plugin.thetvdb");

    private static Integer TIMEOUT = 30000;

    private static String API_KEY = "3982248AA7329D06";

    private static String URL_MIRRORS = "http://www.thetvdb.com/api/" + API_KEY + "/mirrors.xml";

    private static String URL_SERVERTIME = "http://www.thetvdb.com/api/Updates.php?type=none";

    private static String URL_SEARCH = "http://www.thetvdb.com/api/GetSeries.php?seriesname=";

    private LinkedList<String> xmlmirrors = new LinkedList<String>();

    private LinkedList<String> bannermirrors = new LinkedList<String>();

    private LinkedList<String> zipmirrors = new LinkedList<String>();

    private Element data_xml = null;

    private Element banners_xml = null;

    private Element actors_xml = null;

    private Boolean dataLoaded = false;

    private Integer servertime = null;

    private LinkedList<String> languages = null;

    private String language = "en";

    private String actSeason = null;

    private String actEpisode = null;

    private Element episode = null;

    private String seriesUUID = null;

    private String KEY_VALUE_ID = null;

    public TheTVDB() {
        getMirrors();
    }

    public void setID(String ID, String seriesUUID) {
        setID(ID);
        this.seriesUUID = seriesUUID;
    }

    public void setID(String ID) {
        this.init();
        KEY_VALUE_ID = ID;
    }

    public void setLanguage(String language) {
        String uuid = this.seriesUUID;
        setID(this.KEY_VALUE_ID);
        this.language = language;
        this.seriesUUID = uuid;
    }

    private void init() {
        data_xml = null;
        banners_xml = null;
        actors_xml = null;
        actSeason = null;
        actEpisode = null;
        episode = null;
        KEY_VALUE_ID = null;
        seriesUUID = null;
        dataLoaded = false;
    }

    private void getMirrors() {
        try {
            URL u = new URL(URL_MIRRORS);
            URLConnection con = u.openConnection();
            con.setConnectTimeout(TIMEOUT);
            Document docOnline = new SAXBuilder().build(u);
            Element mirrors = docOnline.getRootElement();
            Iterator<Element> i = mirrors.getChildren().iterator();
            while (i.hasNext()) {
                Element mirror = i.next();
                int typemask = Integer.valueOf(mirror.getChild("typemask").getValue());
                if ((typemask & 0x1) == 1) xmlmirrors.add(mirror.getChild("mirrorpath").getValue());
                if ((typemask & 0x2) == 2) bannermirrors.add(mirror.getChild("mirrorpath").getValue());
                if ((typemask & 0x4) == 4) zipmirrors.add(mirror.getChild("mirrorpath").getValue());
            }
        } catch (Exception e) {
            log.error("Could not get mirros", e);
        }
    }

    private void getServertime() {
        try {
            URL u = new URL(URL_SERVERTIME);
            URLConnection con = u.openConnection();
            con.setConnectTimeout(TIMEOUT);
            Document docOnline = new SAXBuilder().build(u);
            Element time = docOnline.getRootElement().getChild("Time");
            servertime = Integer.valueOf(time.getValue());
        } catch (Exception e) {
            log.error("Could not get server time", e);
        }
    }

    public String setIDByTitle(String title, String seriesUUID) {
        String result = setIDByTitle(title);
        this.seriesUUID = seriesUUID;
        return result;
    }

    public String setIDByTitle(String title) {
        init();
        try {
            URL u = new URL(URL_SEARCH + title + "&language=" + language);
            URLConnection con = u.openConnection();
            con.setConnectTimeout(TIMEOUT);
            Document docOnline = new SAXBuilder().build(u);
            Element series = docOnline.getRootElement().getChild("Series");
            if (series != null) KEY_VALUE_ID = series.getChildText("seriesid");
        } catch (Exception e) {
            log.error("Could not set id by title", e);
        }
        return KEY_VALUE_ID;
    }

    private String getRandomXmlmirror() {
        Integer range = xmlmirrors.size();
        Integer choose = (int) (Math.random() * range + 1);
        return xmlmirrors.get(choose - 1);
    }

    private String getRandomBannermirror() {
        Integer range = bannermirrors.size();
        Integer choose = (int) (Math.random() * range + 1);
        return bannermirrors.get(choose - 1);
    }

    private String getRandomZipmirror() {
        Integer range = zipmirrors.size();
        Integer choose = (int) (Math.random() * range + 1);
        return zipmirrors.get(choose - 1);
    }

    public LinkedList<String> getLanguages() {
        if (this.languages == null) {
            String mirrorpath = getRandomXmlmirror();
            String url = mirrorpath + "/api/" + API_KEY + "/languages.xml";
            LinkedList<String> languages = new LinkedList<String>();
            try {
                URL u = new URL(url);
                URLConnection con = u.openConnection();
                con.setConnectTimeout(TIMEOUT);
                Document docOnline = new SAXBuilder().build(u);
                Element lang = docOnline.getRootElement();
                if (languages != null) {
                    Iterator<Element> i = lang.getChildren().iterator();
                    while (i.hasNext()) {
                        Element language = i.next();
                        languages.add(language.getChildText("abbreviation"));
                    }
                }
            } catch (Exception e) {
                log.error("Could not get languages", e);
            }
            this.languages = languages;
            return this.languages;
        }
        return this.languages;
    }

    private void getSeriesData() {
        if (KEY_VALUE_ID != null && !dataLoaded) {
            Boolean loadCachedFiles = false;
            if (seriesUUID != null) {
                if (new File("resources/seriesXMLs/" + language + "/" + seriesUUID + "/data.ser").exists() && new File("resources/seriesXMLs/" + language + "/" + seriesUUID + "/banners.ser").exists() && new File("resources/seriesXMLs/" + language + "/" + seriesUUID + "/actors.ser").exists()) {
                    loadCachedFiles = true;
                }
            }
            if (loadCachedFiles) {
                getCachedSeriesData();
            } else {
                getOnlineSeriesData();
            }
            dataLoaded = true;
        }
    }

    private void getCachedSeriesData() {
        File data_xml_file = new File("resources/seriesXMLs/" + language + "/" + seriesUUID + "/data.ser");
        File banners_xml_file = new File("resources/seriesXMLs/" + language + "/" + seriesUUID + "/banners.ser");
        File actors_xml_file = new File("resources/seriesXMLs/" + language + "/" + seriesUUID + "/actors.ser");
        SAXBuilder builder = new SAXBuilder();
        try {
            data_xml = builder.build(data_xml_file).getRootElement();
            banners_xml = builder.build(banners_xml_file).getRootElement();
            actors_xml = builder.build(actors_xml_file).getRootElement();
        } catch (Exception e) {
            log.error(e);
        }
    }

    private void getOnlineSeriesData() {
        String id;
        if (seriesUUID == null) id = String.valueOf(Math.abs(new Random().nextInt())); else id = seriesUUID;
        new File("resources/seriesXMLs/" + language + "/" + id).mkdirs();
        File data_xml_file = new File("resources/seriesXMLs/" + language + "/" + id + "/data.ser");
        File banners_xml_file = new File("resources/seriesXMLs/" + language + "/" + id + "/banners.ser");
        File actors_xml_file = new File("resources/seriesXMLs/" + language + "/" + id + "/actors.ser");
        try {
            SAXBuilder builder = new SAXBuilder();
            String zippath = getRandomZipmirror();
            URL url = new URL(zippath + "/api/" + API_KEY + "/series/" + KEY_VALUE_ID + "/all/" + language + ".zip");
            URLConnection con = url.openConnection();
            con.setReadTimeout(TIMEOUT);
            ZipInputStream zipstream = new ZipInputStream(con.getInputStream());
            ZipEntry entry = null;
            while ((entry = zipstream.getNextEntry()) != null) {
                ByteArrayOutputStream streamBuilder = new ByteArrayOutputStream();
                int bytesRead;
                byte[] tempBuffer = new byte[8192 * 2];
                while ((bytesRead = zipstream.read(tempBuffer)) != -1) {
                    streamBuilder.write(tempBuffer, 0, bytesRead);
                }
                if (entry.getName().equals(language + ".xml")) {
                    FileOutputStream fo_stream = new FileOutputStream(data_xml_file);
                    streamBuilder.writeTo(fo_stream);
                    data_xml = builder.build(data_xml_file).getRootElement();
                    fo_stream.close();
                } else if (entry.getName().equals("banners.xml")) {
                    FileOutputStream fo_stream = new FileOutputStream(banners_xml_file);
                    streamBuilder.writeTo(fo_stream);
                    banners_xml = builder.build(banners_xml_file).getRootElement();
                    fo_stream.close();
                } else if (entry.getName().equals("actors.xml")) {
                    FileOutputStream fo_stream = new FileOutputStream(actors_xml_file);
                    streamBuilder.writeTo(fo_stream);
                    actors_xml = builder.build(actors_xml_file).getRootElement();
                    fo_stream.close();
                }
            }
        } catch (Exception E) {
            log.error("Could not get series data", E);
        } finally {
            if (!cacheData || seriesUUID == null) {
                data_xml_file.delete();
                banners_xml_file.delete();
                actors_xml_file.delete();
                new File("resources/seriesXMLs/" + language + "/" + id).delete();
                new File("resources/seriesXMLs/" + language).delete();
                new File("resources/seriesXMLs").delete();
            }
        }
    }

    public String getSeriesTitle() {
        if (data_xml == null) getSeriesData();
        if (data_xml != null) {
            return data_xml.getChild("Series").getChildText("SeriesName");
        } else return null;
    }

    public String getEpisodeTitle(String season, String episode) {
        Element episodeTree = getEpisode(season, episode);
        if (episodeTree != null) {
            return episodeTree.getChildText("EpisodeName");
        } else return null;
    }

    public String getEpisodePlot(String season, String episode) {
        Element episodeTree = getEpisode(season, episode);
        if (episodeTree != null) {
            return episodeTree.getChildText("Overview");
        } else return null;
    }

    public String getEpisodeScreen(String season, String episode) {
        Element episodeTree = getEpisode(season, episode);
        if (episodeTree != null) {
            String filepath = episodeTree.getChildText("filename");
            if (StringUtils.isNotEmpty(filepath)) return getRandomBannermirror() + "/banners/" + episodeTree.getChildText("filename"); else return null;
        } else return null;
    }

    public String getSeriesPlot() {
        if (data_xml == null) getSeriesData();
        if (data_xml != null) {
            return data_xml.getChild("Series").getChildText("Overview");
        } else return null;
    }

    public LinkedList<String> getBanners() {
        LinkedList<String> banners = new LinkedList<String>();
        if (banners_xml == null) getSeriesData();
        if (banners_xml != null) {
            LinkedList<Element> list = new LinkedList<Element>();
            Iterator<Element> i = banners_xml.getChildren("Banner").iterator();
            while (i.hasNext()) {
                list.add(i.next());
            }
            Element banner;
            while (list.size() > 0) {
                banner = list.removeFirst();
                if (banner.getChildText("BannerType").equals("series")) banners.add(getRandomBannermirror() + "/banners/" + banner.getChildText("BannerPath"));
            }
        }
        return banners;
    }

    public LinkedList<String> getBackdrops() {
        LinkedList<String> backdrops = new LinkedList<String>();
        if (banners_xml == null) getSeriesData();
        if (banners_xml != null) {
            LinkedList<Element> list = new LinkedList<Element>();
            Iterator<Element> i = banners_xml.getChildren("Banner").iterator();
            while (i.hasNext()) {
                list.add(i.next());
            }
            Element backdrop;
            while (list.size() > 0) {
                backdrop = list.removeFirst();
                if (backdrop.getChildText("BannerType").equals("fanart")) backdrops.add(getRandomBannermirror() + "/banners/" + backdrop.getChildText("BannerPath"));
            }
        }
        return backdrops;
    }

    public LinkedList<String> getPosters() {
        LinkedList<String> posters = new LinkedList<String>();
        if (banners_xml == null) getSeriesData();
        if (banners_xml != null) {
            LinkedList<Element> list = new LinkedList<Element>();
            Iterator<Element> i = banners_xml.getChildren("Banner").iterator();
            while (i.hasNext()) {
                list.add(i.next());
            }
            Element backdrop;
            while (list.size() > 0) {
                backdrop = list.removeFirst();
                if (backdrop.getChildText("BannerType").equals("poster")) posters.add(getRandomBannermirror() + "/banners/" + backdrop.getChildText("BannerPath"));
            }
        }
        return posters;
    }

    public LinkedList<IPerson> getPersons() {
        LinkedList<IPerson> persons = new LinkedList<IPerson>();
        if (actors_xml == null) getSeriesData();
        if (actors_xml != null) {
            LinkedList<Element> list = new LinkedList<Element>();
            Iterator<Element> i = actors_xml.getChildren("Actor").iterator();
            while (i.hasNext()) {
                list.add(i.next());
            }
            Element actor;
            while (list.size() > 0) {
                actor = list.removeFirst();
                Actor actBean = new Actor();
                actBean.setName(actor.getChildText("Name"));
                actBean.setPicture(getRandomBannermirror() + "/banners/" + actor.getChildText("Image"));
                actBean.setRole(actor.getChildText("Role").replace("|", ", "));
                persons.add(actBean);
            }
        }
        return persons;
    }

    public LinkedList<String> getGenres() {
        LinkedList<String> genres = new LinkedList<String>();
        if (data_xml == null) getSeriesData();
        if (data_xml != null) {
            String genreList = data_xml.getChild("Series").getChildText("Genre").substring(1);
            String[] genreArray = genreList.split("\\|");
            for (int i = 0; i < genreArray.length; i++) genres.add(genreArray[i]);
        }
        return genres;
    }

    private Element getEpisode(String season, String episode) {
        if (actSeason != null && actEpisode != null && actSeason.equals(season) && actEpisode.equals(episode)) return this.episode; else {
            if (data_xml == null) getSeriesData();
            if (data_xml != null) {
                List<Element> episodes = data_xml.getChildren("Episode");
                boolean found = false;
                Element act_episode = null;
                int i = 0;
                while (!found && episodes.size() > i) {
                    act_episode = episodes.get(i);
                    if (act_episode.getChildText("SeasonNumber").equals(season) && act_episode.getChildText("EpisodeNumber").equals(episode)) found = true;
                    i++;
                }
                if (found) {
                    actSeason = season;
                    actEpisode = episode;
                    this.episode = act_episode;
                    return this.episode;
                } else {
                    actSeason = null;
                    actEpisode = null;
                    this.episode = null;
                    return this.episode;
                }
            } else return null;
        }
    }

    public String getID() {
        return KEY_VALUE_ID;
    }

    public String getIdIMDB() {
        if (data_xml == null) getSeriesData();
        if (data_xml != null) {
            return data_xml.getChild("Series").getChildText("IMDB_ID");
        } else return null;
    }

    public Float getRating() {
        if (data_xml == null) getSeriesData();
        if (data_xml != null) {
            String rating = data_xml.getChild("Series").getChildText("Rating");
            if (StringUtils.isNotEmpty(rating)) return Float.valueOf(rating); else return Float.valueOf(-1);
        } else return Float.valueOf(-1);
    }

    @Override
    public String getFirstAiredDate() {
        if (data_xml == null) getSeriesData();
        if (data_xml != null) {
            return data_xml.getChild("Series").getChildText("FirstAired");
        } else return null;
    }
}
