package com.groovytagger.database.leoslyrics.engine;

import com.groovytagger.database.leoslyrics.bean.LeosLyricsBean;
import com.groovytagger.interfaces.CdDataBeanInterface;
import com.groovytagger.interfaces.CdDataEngineInterface;
import com.groovytagger.utils.StaticObj;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Properties;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class LeosLyrics implements CdDataEngineInterface {

    private String apiUrl = "http://api.leoslyrics.com/api_search.php";

    private String apiAuth = "?auth=tamantit";

    private String apiSong = "&songtitle=";

    private String apiArtist = "&artist=";

    private String apiHihUrl = "http://api.leoslyrics.com/api_lyrics.php";

    private String apiHid = "&hid=";

    private String engineName = null;

    public LeosLyrics(String engineName) {
        this.engineName = engineName;
    }

    public void setEngineName(String name) {
        this.engineName = name;
    }

    public CdDataBeanInterface searchByArtist(String artist, String album, String song) throws Exception {
        String finalUrl = "";
        finalUrl = finalUrl + apiUrl + apiAuth;
        finalUrl = finalUrl + apiArtist + formatQuery(artist.trim());
        finalUrl = finalUrl + apiSong + formatQuery(song.trim());
        Document doc = getXml(finalUrl);
        if (doc == null) {
            return null;
        }
        String hid = parseRelease(doc);
        finalUrl = apiHihUrl + apiAuth;
        finalUrl = finalUrl + apiHid + hid;
        doc = getXml(finalUrl);
        return new LeosLyricsBean(doc);
    }

    public String getEngineName() {
        return engineName;
    }

    public String formatQuery(String in) {
        try {
            in = URLEncoder.encode(in.trim(), "UTF-8");
        } catch (Exception ex) {
        }
        return in;
    }

    private String parseRelease(Document doc) {
        Element root = doc.getRootElement();
        Iterator children = root.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element) children.next();
            if (child.getName().equalsIgnoreCase("response")) {
                if (!child.getValue().equalsIgnoreCase("SUCCESS")) {
                    return null;
                }
            } else if (child.getName().equalsIgnoreCase("searchResults")) {
                Iterator _resIter = child.getChildren().iterator();
                while (_resIter.hasNext()) {
                    Element _result = (Element) _resIter.next();
                    if ((_result.getAttribute("exactMatch").getValue()).equalsIgnoreCase("true")) {
                        return _result.getAttribute("hid").getValue();
                    }
                }
            }
        }
        return null;
    }

    private Document getXml(String urlString) {
        Document doc = null;
        HttpURLConnection uc = null;
        try {
            URL url = new URL(urlString);
            uc = (HttpURLConnection) url.openConnection();
            if (StaticObj.PROXY_ENABLED) {
                Properties systemSettings = System.getProperties();
                systemSettings.put("http.proxyHost", StaticObj.PROXY_URL);
                systemSettings.put("http.proxyPort", StaticObj.PROXY_PORT);
                System.setProperties(systemSettings);
                sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
                String encoded = new String(encoder.encode(new String(StaticObj.PROXY_USERNAME + ":" + StaticObj.PROXY_PASSWORD).getBytes()));
                uc.setRequestProperty("Proxy-Authorization", "Basic " + encoded);
            }
            BufferedReader ir = null;
            if (uc.getInputStream() != null) {
                ir = new BufferedReader(new InputStreamReader(uc.getInputStream(), "ISO8859_1"));
                SAXBuilder builder = new SAXBuilder();
                doc = builder.build(ir);
            }
        } catch (IOException io) {
            System.out.println("No Lyric found!");
        } catch (Exception e) {
            System.out.println("No Lyric found!");
        } finally {
            try {
                uc.disconnect();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return doc;
        }
    }
}
