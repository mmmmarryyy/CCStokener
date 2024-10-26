package net.vermaas.blogger;

import com.google.gdata.data.Entry;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.TextConstruct;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/**
 * Utility class that validates if links in a blog posy or in blog post
 * comments are still valid. If not, the details will be logged to the log file.
 * Using details from the properties file it will attempt to fix broken links.
 *
 * @author Gero Vermaas
 */
public class LinkValidator {

    private boolean fixRelativeLinks;

    private String fixLinksPrefix;

    private String currentBlogPath;

    private static final Logger log = Logger.getLogger(LinkValidator.class);

    public LinkValidator(boolean fixLinks, String prefix, String currentBlogPath) {
        this.fixRelativeLinks = fixLinks;
        this.fixLinksPrefix = prefix;
        this.currentBlogPath = currentBlogPath;
        log.debug("fixRelativeLinks: " + fixRelativeLinks);
        log.debug("fixlinksPrefix: " + fixLinksPrefix);
        log.debug("currentBlogPath: " + currentBlogPath);
    }

    List<String> validateAndFixLinks(BlogPostDetails bpd, Map<String, String> oldNewMap) {
        log.debug("validateLinks for: " + bpd.getOriginalPostId());
        List<String> failedUrls = new ArrayList<String>();
        String fixedContent = validateLinks(bpd.getBlogPost().getPlainTextContent(), failedUrls, oldNewMap);
        bpd.getBlogPost().setContent(new PlainTextConstruct(fixedContent));
        TextConstruct summary = bpd.getBlogPost().getSummary();
        if (!summary.isEmpty()) {
            fixedContent = validateLinks(summary.getPlainText(), failedUrls, oldNewMap);
            bpd.getBlogPost().setSummary(new PlainTextConstruct(fixedContent));
        }
        for (Entry comment : bpd.getComments()) {
            fixedContent = validateLinks(comment.getPlainTextContent(), failedUrls, oldNewMap);
            comment.setContent(new PlainTextConstruct(fixedContent));
        }
        return failedUrls;
    }

    String validateLinks(String str, List<String> failedLinks, Map<String, String> oldNewMap) {
        String result = validateLinks(str, "href=", failedLinks, oldNewMap);
        result = validateLinks(result, "src=", failedLinks, oldNewMap);
        return result;
    }

    String validateLinks(String str, String searchAtt, List<String> failedLinks, Map<String, String> oldNewMap) {
        StringBuilder sb = new StringBuilder(str);
        int searchAttLength = searchAtt.length();
        int iPos = str.indexOf(searchAtt);
        while (iPos != -1) {
            sb = new StringBuilder();
            boolean urlQuoted = str.charAt(iPos + searchAttLength) == '"';
            String delims = "\"";
            int startAt = iPos + searchAttLength + 1;
            if (!urlQuoted) {
                delims = "> ";
                startAt = iPos + searchAttLength;
            }
            sb.append(str.substring(0, startAt));
            StringTokenizer st = new StringTokenizer(str.substring(startAt), delims);
            String link = st.nextToken();
            String orgLink = link;
            link = link.replace('"', ' ').trim();
            boolean linkOK = true;
            if (link.startsWith("/" + currentBlogPath) || link.startsWith(fixLinksPrefix + currentBlogPath)) {
                String key = link;
                if (link.startsWith(fixLinksPrefix)) {
                    key = link.substring(fixLinksPrefix.length() + currentBlogPath.length());
                }
                String newBlogLocation = oldNewMap.get(key);
                if (newBlogLocation == null) {
                    newBlogLocation = oldNewMap.get(fixLinksPrefix + link);
                }
                if (newBlogLocation != null) {
                    link = newBlogLocation;
                }
            } else {
                if (!(link.startsWith("http") || link.startsWith("https"))) {
                    if (fixRelativeLinks) {
                        log.debug("Prepending prefix to: " + link);
                        link = fixLinksPrefix + link;
                    }
                }
                linkOK = validateLink(link);
                if (!linkOK) {
                    failedLinks.add(orgLink.replace('"', ' ').trim());
                }
            }
            if (sb.charAt(sb.length() - 1) != '"') {
                sb.append("\"");
            }
            if (linkOK) {
                sb.append(link);
            } else {
                sb.append(orgLink);
            }
            if (str.charAt(startAt + orgLink.length()) != '"') {
                sb.append("\"");
            }
            sb.append(str.substring(startAt + orgLink.length()));
            str = sb.toString();
            iPos = str.indexOf(searchAtt, startAt + link.length());
        }
        return sb.toString();
    }

    boolean validateLink(String link) {
        try {
            URL url = new URL(link);
            URLConnection conn = (URLConnection) url.openConnection();
            if (conn instanceof HttpURLConnection) {
                HttpURLConnection httpConn = (HttpURLConnection) conn;
                httpConn.connect();
                int respCode = httpConn.getResponseCode();
                if (respCode < 200 || respCode >= 400) {
                    return false;
                }
            }
        } catch (MalformedURLException mue) {
            return false;
        } catch (IOException ioe) {
            return false;
        }
        return true;
    }
}
