package com.microfly.job.sitemap;

import com.microfly.core.*;
import com.microfly.util.tree.Node;
import com.microfly.exception.NpsException;
import com.microfly.exception.ErrorHelper;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Iterator;
import java.io.*;

/**
 * GoogleSitemapBuilder SiteMap���񴴽���
 * a new publishing system
 * Copyright (c) 2007
 *
 * @author jialin
 * @version 1.0
 */
public class GoogleSitemapBuilder {

    private NpsContext ctxt = null;

    private Site site = null;

    private GoogleSitemapIndex indexer = null;

    public GoogleSitemapBuilder(NpsContext ctxt) {
        this.ctxt = ctxt;
    }

    public void Generate(Site site) throws Exception {
        this.site = site;
        indexer = new GoogleSitemapIndex(ctxt, site);
        GenerateSitemaps();
        GenerateSitemapIndex();
    }

    private void GenerateSitemaps() throws Exception {
        Iterator toplevel_topics = site.GetTopicTree().GetChilds(null);
        while (toplevel_topics.hasNext()) {
            Node node = (Node) toplevel_topics.next();
            Topic top = (Topic) node.GetValue();
            GoogleSitemap sitemap = new GoogleSitemap(ctxt, top, indexer);
            try {
                sitemap.GenerateSitemap();
            } catch (Exception e) {
                com.microfly.util.DefaultLog.error(e);
            }
        }
    }

    private void GenerateSitemapIndex() throws Exception {
        indexer.Finish();
    }

    public void Submit2Google() throws Exception {
        for (int i = 1; i <= indexer.GetIndex(); i++) {
            String params = URLEncoder.encode(indexer.GetURL(i), "UTF-8");
            URL google_url = new URL("http://www.google.com/webmasters/tools/ping?sitemap=" + params);
            Submit2URL(google_url);
        }
    }

    public void Submit2Yahoo() throws Exception {
        for (int i = 1; i <= indexer.GetIndex(); i++) {
            String params = URLEncoder.encode(indexer.GetURL(i), "UTF-8");
            URL yahoo_url = new URL("http://api.search.yahoo.com/SiteExplorerService/V1/updateNotification?appid=YahooDemo&url=" + params);
            Submit2URL(yahoo_url);
        }
    }

    public void Submit2Live() throws Exception {
        for (int i = 1; i <= indexer.GetIndex(); i++) {
            String params = URLEncoder.encode(indexer.GetURL(i), "UTF-8");
            URL live_url = new URL("http://webmaster.live.com/ping.aspx?siteMap=" + params);
            Submit2URL(live_url);
        }
    }

    public void Submit2Ask() throws Exception {
        for (int i = 1; i <= indexer.GetIndex(); i++) {
            String params = URLEncoder.encode(indexer.GetURL(i), "UTF-8");
            URL live_url = new URL("http://submissions.ask.com/ping?sitemap=" + params);
            Submit2URL(live_url);
        }
    }

    private void Submit2URL(URL url) throws Exception {
        HttpURLConnection urlc = null;
        try {
            urlc = (HttpURLConnection) url.openConnection();
            urlc.setRequestMethod("GET");
            urlc.setDoOutput(true);
            urlc.setDoInput(true);
            urlc.setUseCaches(false);
            urlc.setAllowUserInteraction(false);
            if (urlc.getResponseCode() != 200) {
                InputStream in = null;
                Reader reader = null;
                try {
                    in = urlc.getInputStream();
                    reader = new InputStreamReader(in, "UTF-8");
                    int read = 0;
                    char[] buf = new char[1024];
                    String error = null;
                    while ((read = reader.read(buf)) >= 0) {
                        if (error == null) error = new String(buf, 0, read); else error += new String(buf, 0, read);
                    }
                    throw new NpsException(error, ErrorHelper.SYS_UNKOWN);
                } finally {
                    if (reader != null) try {
                        reader.close();
                    } catch (Exception e1) {
                    }
                    if (in != null) try {
                        in.close();
                    } catch (Exception e1) {
                    }
                }
            }
        } finally {
            if (urlc != null) try {
                urlc.disconnect();
            } catch (Exception e1) {
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        config.init_test();
        User user = User.Login("system", "manager");
        NpsWrapper wrapper = new NpsWrapper(user, "test");
        GoogleSitemapBuilder builderGoogle = new GoogleSitemapBuilder(wrapper.GetContext());
        builderGoogle.Generate(wrapper.GetSite());
        builderGoogle.Submit2Google();
    }
}
