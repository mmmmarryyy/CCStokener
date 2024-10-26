package net.sourceforge.blogentis.plugins.trackback.impl;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.transform.TransformerException;
import net.sourceforge.blogentis.om.Blog;
import net.sourceforge.blogentis.om.Post;
import net.sourceforge.blogentis.om.PostPeer;
import net.sourceforge.blogentis.plugins.base.AbstractPostEditExtension;
import net.sourceforge.blogentis.plugins.trackback.ITrackbackConstants;
import net.sourceforge.blogentis.turbine.BlogRunData;
import net.sourceforge.blogentis.utils.HTMLUtils;
import net.sourceforge.blogentis.utils.JTidyService;
import net.sourceforge.blogentis.utils.LinkFactoryService;
import net.sourceforge.blogentis.utils.MappedConfiguration;
import net.sourceforge.blogentis.utils.tools.FragmentTool;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.TorqueException;
import org.apache.turbine.util.RunData;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;

/**
 * @author abas
 */
public class TrackbackPostEditExtension extends AbstractPostEditExtension implements ITrackbackConstants {

    public static final Log log = LogFactory.getLog(TrackbackPostEditExtension.class);

    public static final Pattern rdfPattern = Pattern.compile("<rdf:RDF.*?</rdf:RDF>", Pattern.DOTALL);

    public static final Pattern tbPattern = Pattern.compile("trackback:ping=\"([^\"]+)\"");

    public static final Pattern aboutPattern = Pattern.compile("about=\"([^\"]+)\"");

    public static final Pattern identPattern = Pattern.compile("dc:identifier=\"([^\"]+)\"");

    public String getName() {
        return "Sends trackbacks when post is published.";
    }

    public String buildOptionsHTML(BlogRunData data, Post post, String location) {
        if (post == null || post.isNew() || !LOCATION_END_OF_OPTIONS.equals(location) || blog.getConfiguration().getBoolean(T_BLOG_SEND, true) == false) {
            return "";
        }
        return FragmentTool.findAndInvoke(data, "TrackbackPostEdit", data, post);
    }

    public void postPublicationStatusChanged(BlogRunData data, Post post, int oldState) {
        if (post.getPostType() != PostPeer.PUBLISHED_TYPE) return;
        MappedConfiguration conf = this.blog.getConfiguration();
        MappedConfiguration postConf = post.getProperties();
        if (conf.getBoolean(T_BLOG_SEND, true) && data.getParameters().getString("sendTrackbacks", null) != null) {
            processPost(post);
            postConf.setProperty(T_SENT, Boolean.TRUE);
        }
        if (!StringUtils.isEmpty(conf.getString(T_BLOG_WEBLOGPINGUPDATE_SITES, "")) && data.getParameters().getString("sendWeblogsPing", null) != null) {
            sendWeblogUpdatePing(data, data.getBlog(), post);
        }
    }

    private Node makePostDocument(Post p) {
        String contents = "<title>" + p.getTitle() + "</title>" + p.getShortDescription() + p.getFullText();
        return JTidyService.parseHTMLBody(contents);
    }

    private List getLinks(Node doc) {
        NodeIterator i;
        try {
            i = XPathAPI.selectNodeIterator(doc, "//a/@href");
        } catch (TransformerException e) {
            log.error(e);
            return null;
        }
        ArrayList l = new ArrayList();
        Node n = null;
        while ((n = i.nextNode()) != null) {
            l.add(n.getNodeValue());
        }
        return l;
    }

    private void processPost(Post p) {
        Node doc = makePostDocument(p);
        List links = getLinks(doc);
        Set alreadySent = new HashSet(p.getProperties().getList(T_URIS_SENT, Collections.EMPTY_LIST));
        Set notSent = new HashSet(p.getProperties().getList(T_URIS_NOT_SENT, Collections.EMPTY_LIST));
        for (Iterator i = links.iterator(); i.hasNext(); ) {
            String link = (String) i.next();
            if (!link.startsWith("https://") && !link.startsWith("http://")) {
                log.debug("Skipping " + link);
                continue;
            }
            log.debug("Looking at " + link + " for trackback URLs");
            try {
                if (alreadySent.contains(link) || notSent.contains(link)) continue;
                URLConnection con = openConnection(link);
                if (con == null) continue;
                String c = fetchURLContents(con);
                String trackBackLink = getTrackbackFromHTML(c, link);
                if (trackBackLink != null) {
                    if (sendTrackBackTo(trackBackLink, p)) alreadySent.add(link);
                } else if ((trackBackLink = getPingbackFromHTML(con, c)) != null) {
                    if (sendPingbackTo(trackBackLink, p, link)) alreadySent.add(link);
                }
            } catch (IOException e) {
                log.warn("Could not get contents of " + link, e);
            } catch (Exception e) {
                log.warn("Could not send trackback to " + link, e);
            } finally {
                if (!alreadySent.contains(link)) {
                    notSent.add(link);
                }
            }
        }
        String[] strings = this.blog.getConfiguration().getString(T_BLOG_EXTRA_LINKS, "").split("/n");
        links = new ArrayList();
        for (int i = 0; i < strings.length; i++) {
            String link = strings[i].trim();
            if (link.length() < 5) continue;
            if (alreadySent.contains(link) || notSent.contains(link)) continue;
            if (sendTrackBackTo(link, p)) {
                alreadySent.add(link);
                notSent.remove(link);
            }
        }
        p.getProperties().setList(T_URIS_SENT, new ArrayList(alreadySent));
        p.getProperties().setList(T_URIS_NOT_SENT, new ArrayList(notSent));
    }

    private String getPingbackFromHTML(URLConnection con, String c) {
        String pbLink = con.getHeaderField("X-Pingback");
        if (con != null) return pbLink;
        Document doc = JTidyService.parseHTML(c);
        if (doc == null) return null;
        try {
            Node node = null;
            node = XPathAPI.selectSingleNode(doc, "//link[@rel='pingback']/@href");
            if (node != null) return node.getNodeValue();
        } catch (TransformerException e) {
            log.warn("", e);
        }
        return null;
    }

    private String fetchURLContents(URLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        if ("text/html".equals(con.getContentType())) {
            return JTidyService.cleanupString(is);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int len;
        byte[] byffer = new byte[1024];
        do {
            len = is.read(byffer, 0, 1024);
            if (len > 0) bos.write(byffer, 0, len);
        } while (len > 0);
        return new String(bos.toByteArray(), "UTF-8");
    }

    private URLConnection openConnection(String link) {
        URL url = null;
        try {
            url = new URL(link);
        } catch (MalformedURLException e) {
            log.debug("Incorrect URL " + link, e);
            return null;
        }
        try {
            URLConnection connection = url.openConnection();
            connection.setReadTimeout(10 * 1000);
            return connection;
        } catch (IOException e1) {
            log.debug("Could not fetch " + link, e1);
            return null;
        }
    }

    private String getTrackbackFromHTML(String s, String origUrl) {
        Document doc = JTidyService.parseHTML(s);
        if (doc != null) {
            try {
                URI orig = new URI(origUrl);
                Node node = XPathAPI.selectSingleNode(doc, "//link[@rel='trackback']/@href");
                if (node != null) {
                    return new URI(orig, node.getNodeValue()).toString();
                }
                node = XPathAPI.selectSingleNode(doc, "//a[@rel='trackback']/@href");
                if (node != null) return new URI(orig, node.getNodeValue()).toString();
            } catch (TransformerException e) {
                log.error("Exception in the XPATH API", e);
            } catch (URIException e) {
                log.warn("Not an URI", e);
            }
        }
        Matcher m = rdfPattern.matcher(s);
        while (m.find()) {
            String rdf = m.group();
            Matcher tb = identPattern.matcher(rdf);
            if (!tb.find()) continue;
            if (!origUrl.equals(tb.group(1))) continue;
            tb = tbPattern.matcher(rdf);
            if (tb.find()) return tb.group(1);
            tb = aboutPattern.matcher(rdf);
            if (tb.find()) return tb.group(1);
        }
        return null;
    }

    private boolean sendTrackBackTo(String url, Post p) {
        log.debug("Sending trackback to " + url);
        try {
            URL l = new URL(url);
            HttpURLConnection con = (HttpURLConnection) l.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded" + "; charset=utf-8");
            con.setDoOutput(true);
            OutputStreamWriter bw = new OutputStreamWriter(new BufferedOutputStream(con.getOutputStream()), "utf-8");
            bw.write("title=");
            bw.write(URLEncoder.encode(p.getTitle(), "utf-8"));
            bw.write("&url=");
            bw.write(LinkFactoryService.getInstance().getLink().permaLink(p).toString());
            bw.write("&excerpt=");
            bw.write(URLEncoder.encode(HTMLUtils.removeTags(p.getShortDescription()), "utf-8"));
            bw.write("&blog_name=");
            bw.write(URLEncoder.encode(p.getBlog().getTitle(), "utf-8"));
            bw.flush();
            bw.close();
            fetchURLContents(con);
            return true;
        } catch (MalformedURLException e) {
            log.debug("URL specified in RDF as trackback was invalid.", e);
            return false;
        } catch (IOException e) {
            log.debug("Error sending trackback.", e);
            return false;
        } catch (TorqueException e) {
            log.error("Torque threw an error.", e);
            return false;
        }
    }

    private void sendWeblogUpdatePing(RunData data, Blog blog, Post p) {
        String[] strings = blog.getConfiguration().getString(T_BLOG_WEBLOGPINGUPDATE_SITES, "").split("\n");
        List l = new ArrayList(strings.length);
        String link = LinkFactoryService.getInstance().getLink().permaLink(blog).toString();
        for (int i = 0; i < strings.length; i++) {
            String url = strings[i].trim();
            if (StringUtils.isEmpty(url)) continue;
            try {
                XmlRpcClient xrc = new XmlRpcClient(url);
                Vector params = new Vector(2);
                params.add(blog.getTitle());
                params.add(link);
                xrc.execute("weblogUpdates.ping", params);
                l.add(url);
            } catch (Exception e) {
                log.error(e);
            }
        }
        p.getProperties().setList(T_POST_WEBLOGUPDATEPING_SENT, l);
    }

    private boolean sendPingbackTo(String trackBackLink, Post p, String origLink) {
        String link = LinkFactoryService.getInstance().getLink().permaLink(p).toString();
        try {
            XmlRpcClient xrc = new XmlRpcClient(trackBackLink);
            Vector params = new Vector(2);
            params.add(link);
            params.add(origLink);
            xrc.execute("pingback.ping", params);
            return true;
        } catch (Exception e) {
            log.warn("Could not send pingback to " + trackBackLink, e);
            return false;
        }
    }
}
