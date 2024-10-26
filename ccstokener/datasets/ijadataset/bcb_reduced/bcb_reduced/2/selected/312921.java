package com.groovytagger.database.discogs.engine;

import com.groovytagger.database.discogs.bean.DiscogsBean;
import com.groovytagger.interfaces.CdDataEngineInterface;
import com.groovytagger.utils.LogManager;
import com.groovytagger.utils.StaticObj;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;

public class DiscogsEngine implements CdDataEngineInterface {

    private static final String[] apiKey = { "b68540c1bb" };

    private static final String disgogsUrl = "www.discogs.com";

    private String engineName = null;

    public DiscogsEngine(String engineName) {
        this.engineName = engineName;
    }

    public void setEngineName(String name) {
        this.engineName = name;
    }

    public String getEngineName() {
        return engineName;
    }

    public DiscogsBean searchByArtist(String artist, String album, String song) throws Exception {
        Document doc = getXML(artist);
        if (doc == null) return null;
        return new DiscogsBean(doc, artist, album, song, "");
    }

    private Document getXML(String artist) throws Exception {
        Document doc = null;
        URL url = new URL("http://" + disgogsUrl + "/artist/" + formatQuery(artist) + "?f=xml&api_key=" + apiKey[0]);
        HttpURLConnection uc = (HttpURLConnection) url.openConnection();
        uc.addRequestProperty("Accept-Encoding", "gzip");
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
        try {
            if (uc.getInputStream() != null) {
                InputStream _is = uc.getInputStream();
                GZIPInputStream _gzipIs = new GZIPInputStream(_is);
                InputStreamReader _isReader = new InputStreamReader(_gzipIs);
                ir = new BufferedReader(_isReader);
                SAXBuilder builder = new SAXBuilder();
                doc = builder.build(ir);
            }
        } catch (Exception e) {
            if (StaticObj.DEBUG) {
                LogManager.getInstance().getLogger().error(e);
                e.printStackTrace();
                System.out.println("No Data found!");
            }
        }
        return doc;
    }

    public String formatQuery(String in) {
        try {
            in = URLEncoder.encode(in.trim(), "UTF-8");
        } catch (Exception ex) {
        }
        return in;
    }

    public Document searchRelease(String id) throws Exception {
        Document doc = null;
        URL url = new URL("http://" + disgogsUrl + "/release/" + id + "?f=xml&api_key=" + apiKey[0]);
        HttpURLConnection uc = (HttpURLConnection) url.openConnection();
        uc.addRequestProperty("Accept-Encoding", "gzip");
        BufferedReader ir = null;
        if (uc.getInputStream() != null) {
            ir = new BufferedReader(new InputStreamReader(new GZIPInputStream(uc.getInputStream()), "ISO8859_1"));
            SAXBuilder builder = new SAXBuilder();
            doc = builder.build(ir);
        }
        return doc;
    }

    public static void main(String[] args) {
        DiscogsEngine engine = new DiscogsEngine("df");
        try {
            System.out.println(engine.formatQuery("   TAM A AA  a "));
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
