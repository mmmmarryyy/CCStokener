package net.dadajax.downloadmanager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dadajax.download.Cookie;
import net.dadajax.download.RapidshareCookie;
import org.apache.log4j.Logger;

/**
 * @author dadajax
 *
 */
public class RapidshareLinkChecker implements LinkChecker {

    private static final String RAPIDSHARE_LOGIN_PAGE = "https://ssl.rapidshare.com/cgi-bin/premiumzone.cgi";

    private static final String RAPIDSHARE_REGEX = "http://(www)?rapidshare.(com|de)[[^\"<]&&\\S]*";

    private static Cookie cookie;

    public RapidshareLinkChecker() {
        cookie = RapidshareCookie.getInstance();
    }

    @Override
    public LinkCheckerResult getCheckedLinks(String string) {
        LinkCheckerResult result = new LinkCheckerResult();
        List<String> links = getAllLinks(string);
        Set<String> linksSet = new LinkedHashSet<String>();
        for (String s : links) {
            linksSet.add(s);
        }
        for (String strLink : linksSet) {
            if (checkLink(strLink)) {
                result.addValidLink(strLink);
            } else {
                result.addInvalidLink(strLink);
            }
        }
        return result;
    }

    @Override
    public List<String> getAllLinks(String string) {
        Pattern p = Pattern.compile(RAPIDSHARE_REGEX);
        Matcher m = p.matcher(string);
        Set<String> links = new LinkedHashSet<String>();
        while (m.find()) {
            String strLink = string.substring(m.start(), m.end());
            links.add(strLink);
        }
        List<String> linksArray = new ArrayList<String>();
        for (String s : links) {
            linksArray.add(s);
        }
        return linksArray;
    }

    @Override
    public boolean checkLink(String link) {
        boolean result = false;
        URLConnection connection = null;
        URL url = null;
        try {
            url = new URL(link);
            connection = url.openConnection();
            connection.setRequestProperty("Cookie", cookie.getCookie(RAPIDSHARE_LOGIN_PAGE));
            connection.connect();
            if (connection.getContentLength() > 0) {
                if (connection.getContentType().equals("application/octet-stream")) {
                    result = true;
                }
                Logger.getRootLogger().debug(connection.getContentType());
            }
        } catch (MalformedURLException e) {
            Logger.getRootLogger().error(link + " cannot be url", e);
        } catch (IOException e) {
            Logger.getRootLogger().error("connection failed", e);
        }
        if (!result) {
            Logger.getRootLogger().warn(link + " doesn't exist");
        }
        return result;
    }
}
