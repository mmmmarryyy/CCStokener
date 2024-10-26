package org.spartanrobotics.devel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import scouter.game.Competition;
import scouter.game.GameInfo;
import scouter.server.Constants;
import scouter.server.mobile.jsp.MatchEditorViewType;
import scouter.server.mobile.jsp.MatchListView;

/**
 * @author User
 */
public class GetHTML {

    private static final String host = "http://localhost:8888";

    /**
	 * @param args
	 */
    public static void main(String[] args) throws MalformedURLException, IOException {
        HttpURLConnection.setFollowRedirects(false);
        for (MatchListView c : MatchListView.values()) {
            add(c.toString(), "matches-xhr.jsp?view=" + c);
        }
        for (GameInfo g : GameInfo.values()) {
            for (MatchEditorViewType c : MatchEditorViewType.values()) {
                add(c.toString() + "-" + g, "match_editor-xhr.jsp?view=" + c + "&game=" + g);
            }
        }
        add("index", "index-xhr.jsp?");
        StringBuilder b = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?><resources>");
        b.append("<string-array name=\"games\">");
        for (GameInfo c : GameInfo.values()) {
            b.append("<item>" + c.name() + "</item>");
        }
        b.append("</string-array>");
        b.append("<integer name=\"page_size\">" + Constants.pageSize + "</integer>");
        b.append("</resources>");
        Writer out = new OutputStreamWriter(new FileOutputStream(new File(System.getProperty("res-loc") + "/values/generated_text.xml")));
        out.append(b);
        out.close();
        try {
            JSONObject map = new JSONObject();
            for (Competition c : Competition.values()) {
                map.put(c.name(), c.getGame().name());
            }
            out = new OutputStreamWriter(new FileOutputStream(new File(System.getProperty("assets-loc") + "/comp_map.json")));
            out.append(map.toString());
            out.close();
        } catch (JSONException e) {
            Logger.getLogger(GetHTML.class.getName()).log(Level.SEVERE, "Could not write JSON data.", e);
        }
        copy("compass", "core-.*\\.cache\\.css", "core.css");
    }

    private static void copy(String fromdir, final String regex, String to) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(new File(System.getProperty("frc_scouter_war") + "/" + fromdir + "/" + new File(System.getProperty("frc_scouter_war") + "/" + fromdir).list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.matches(regex);
            }
        })[0])));
        StringBuilder b = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            b.append(line);
            b.append("\r\n");
        }
        in.close();
        Writer out = new OutputStreamWriter(new FileOutputStream(new File(System.getProperty("assets-loc") + "/" + to)));
        out.append(b.toString().replace("/mobile", "/android_asset/generated"));
        out.close();
        HttpURLConnection con = getCon("/getAndroidData");
        in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        b = new StringBuilder();
        while ((line = in.readLine()) != null) {
            b.append(line);
            b.append("\r\n");
        }
        in.close();
        out = new OutputStreamWriter(new FileOutputStream(new File(System.getProperty("assets-loc") + "/dataDefault")));
        out.append(b);
        out.close();
    }

    private static void add(String path, String page) throws MalformedURLException, IOException {
        HttpURLConnection con = getCon("/mobile/" + page + "&sendData=false");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder b = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            b.append(line);
            b.append("\r\n");
        }
        in.close();
        Writer out = new OutputStreamWriter(new FileOutputStream(new File(System.getProperty("assets-loc") + "/views/" + path + ".html")));
        out.append(b);
        out.close();
    }

    private static HttpURLConnection getCon(String url) throws MalformedURLException, IOException {
        HttpURLConnection r = (HttpURLConnection) (new URL(host + url).openConnection());
        r.addRequestProperty("Cookie", "dev_appserver_login=get_html@localhost.devel:false:18580476422013912411");
        r.setRequestMethod("GET");
        r.setReadTimeout(15000);
        r.connect();
        return r;
    }
}
