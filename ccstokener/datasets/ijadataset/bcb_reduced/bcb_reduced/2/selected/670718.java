package net.sourceforge.seriesdownloader.downloader.newzbinsearch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import net.sourceforge.seriesdownloader.Main;
import net.sourceforge.seriesdownloader.Util;
import net.sourceforge.seriesdownloader.controller.options.IOptions;
import net.sourceforge.seriesdownloader.downloader.IDownloader;
import net.sourceforge.seriesdownloader.model.EpisodeNotation;
import net.sourceforge.seriesdownloader.model.Show;

public class Downloader implements IDownloader {

    private String username;

    private String password;

    private Integer retention;

    public static final int MAX_REQUESTS = 5;

    public static final int REQUEST_TIMEOUT = 60;

    private ArrayList<Date> requests;

    private ArrayList<String> include = new ArrayList<String>();

    private ArrayList<String> exclude = new ArrayList<String>();

    public String getName() {
        return "NewzBin Search Downloader";
    }

    public File download(Show s) throws Exception {
        Collection<String> exclude = Util.toCollection((List<String>) this.exclude.clone(), Util.nonNullString(s.getExclude()).split(","));
        URL url = new URL("http://v3.newzbin.com/search/" + buildQuery(s));
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        while ((line = in.readLine()) != null) {
            if (!Util.containsNone(line, exclude)) continue;
            String id = line.split("\",\"", 3)[1];
            File downloaded = download(s, id);
            if (downloaded != null) return downloaded;
        }
        return null;
    }

    private synchronized void waitForTurn() throws InterruptedException {
        removeClicksOlderThan(REQUEST_TIMEOUT);
        while (requests.size() >= MAX_REQUESTS) {
            for (int i = 0; i <= 60; i++) {
                Main.addStatus(getName() + " waiting " + Math.max(0, 61 - i) + " seconds.", true);
                Thread.sleep(1000);
            }
            removeClicksOlderThan(REQUEST_TIMEOUT);
        }
        requests.add(Calendar.getInstance().getTime());
    }

    private File download(Show show, String id) throws Exception {
        waitForTurn();
        URLConnection urlConn;
        DataOutputStream printout;
        URL url = new URL("http://v3.newzbin.com/api/dnzb/");
        urlConn = url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        printout = new DataOutputStream(urlConn.getOutputStream());
        String content = "username=" + this.username + "&password=" + this.password + "&reportid=" + id;
        printout.writeBytes(content);
        printout.flush();
        printout.close();
        BufferedReader nzbInput = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
        File f = File.createTempFile("show", ".nzb");
        BufferedWriter out = new BufferedWriter(new FileWriter(f));
        String str;
        while (null != ((str = nzbInput.readLine()))) out.write(str);
        nzbInput.close();
        out.close();
        return f;
    }

    private String buildQuery(Show s) {
        Collection<String> includes = Util.toCollection((List<String>) this.include.clone(), Util.nonNullString(s.getInclude()).split(","));
        StringBuffer query = new StringBuffer();
        query.append("?q=").append(s.getLabel() + " " + s.getNextEpisode().toString(EpisodeNotation.nxnn));
        for (String include : includes) query.append(" ").append(include);
        query.append("&searchaction=Search");
        query.append("&fpn=p");
        query.append("&category=8category=11");
        query.append("&area=-1");
        query.append("&u_nfo_posts_only=0");
        query.append("&u_url_posts_only=0");
        query.append("&u_comment_posts_only=0");
        query.append("&u_v3_retention=").append(retention * 60 * 60 * 24);
        query.append("&sort=ps_edit_date");
        query.append("&order=desc");
        query.append("&areadone=-1");
        query.append("&feed=csv");
        return query.toString().replaceAll(" ", "%20");
    }

    private void removeClicksOlderThan(int seconds) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, -seconds);
        for (int i = requests.size() - 1; i >= 0; i--) if (cal.getTime().after(requests.get(i))) requests.remove(i);
    }

    public boolean hasSettings() {
        return true;
    }

    public void init(IOptions options) {
        username = options.readString(NewzBinSettings.USERNAME, null);
        password = options.readString(NewzBinSettings.PASSWORD, null);
        retention = options.readInt(NewzBinSettings.RETENTION, 100);
        requests = options.readObject(NewzBinSettings.REQUESTTIMES, new ArrayList());
        options.setSetting(NewzBinSettings.REQUESTTIMES, requests);
        Authenticator.setDefault(new NewzbinAuthenticator(username, password));
        Util.toCollection(include, options.readString(GLOABAL_INCLUDE_SETTING, "").split(","));
        Util.toCollection(exclude, options.readString(GLOABAL_EXCLUDE_SETTING, "").split(","));
    }

    public void showSettings(IOptions options) {
        new NewzBinSettings(options).setVisible(true);
    }
}

class NewzbinAuthenticator extends Authenticator {

    private String username;

    private String password;

    public NewzbinAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password.toCharArray());
    }
}
