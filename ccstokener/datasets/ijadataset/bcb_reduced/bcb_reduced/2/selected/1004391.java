package net.sourceforge.seriesdownloader.downloader.newzbinrss;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JOptionPane;
import net.sourceforge.seriesdownloader.Util;
import net.sourceforge.seriesdownloader.controller.options.IOptions;
import net.sourceforge.seriesdownloader.downloader.IDownloader;
import net.sourceforge.seriesdownloader.model.EpisodeNotation;
import net.sourceforge.seriesdownloader.model.Show;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

public class Downloader implements IDownloader {

    private QName idQName = new QName("id", new Namespace("report", "http://www.newzbin.com/DTD/2007/feeds/report/"));

    private List<DownloadItem> items;

    private String username;

    private String password;

    private ArrayList<String> include = new ArrayList<String>();

    private ArrayList<String> exclude = new ArrayList<String>();

    public String getName() {
        return "Newzbin RSS downloader";
    }

    public File download(Show s) {
        String name = s.getLabel().toLowerCase();
        Collection<String> include = Util.toCollection((List<String>) this.include.clone(), Util.nonNullString(s.getInclude()).split(","));
        Collection<String> exclude = Util.toCollection((List<String>) this.exclude.clone(), Util.nonNullString(s.getExclude()).split(","));
        for (DownloadItem item : items) {
            if (item.name.contains(name) && item.name.contains(s.getNextEpisode().toString(EpisodeNotation.nxnn)) && Util.containsAll(item.name, include) && Util.containsNone(item.name, exclude)) {
                try {
                    File downloaded = download(s, item.id);
                    if (downloaded != null) return downloaded;
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    private File download(Show show, String id) throws Exception {
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

    public boolean hasSettings() {
        return true;
    }

    public void showSettings(IOptions options) {
        new NewzBinSettings(options).setVisible(true);
    }

    public void init(IOptions options) {
        username = options.readString(NewzBinSettings.USERNAME, null);
        password = options.readString(NewzBinSettings.PASSWORD, null);
        Util.toCollection(include, options.readString(GLOABAL_INCLUDE_SETTING, "").split(","));
        Util.toCollection(exclude, options.readString(GLOABAL_EXCLUDE_SETTING, "").split(","));
        items = new ArrayList<DownloadItem>();
        String url = options.readString(NewzBinSettings.RSSFEED, null);
        if (Util.isEmpty(url)) {
            JOptionPane.showMessageDialog(null, getName() + " isn't properly configured. Fill in the settings in the options window.");
        }
        String rssResult = NewzBinSettings.rssResult;
        try {
            Document doc = DocumentHelper.parseText(rssResult);
            List<Element> nodes = doc.getRootElement().selectNodes("//item");
            for (Element node : nodes) {
                items.add(new DownloadItem(node.elementText("title").toLowerCase(), node.elementText(idQName)));
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    class DownloadItem {

        String name;

        String id;

        public DownloadItem(String name, String id) {
            this.name = name;
            this.id = id;
        }
    }
}
