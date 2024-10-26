package jm.lib.web.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.Locale;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import jm.lib.util.StrUtil;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Jiming Liu
 */
public class ServletUtil {

    public static final String USER_LOCALE = "_USER_LOCALE";

    public static final String RESOURCE_BUNDLE_NAME = "_RESOURCE_BUNDLE_NAME";

    public static void returnFile(String filename, OutputStream out) throws IOException {
        IOUtils.copy(new FileInputStream(filename), out);
    }

    public static void returnURL(URL url, OutputStream out) throws IOException {
        IOUtils.copy(url.openStream(), out);
    }

    public static void returnURL(URL url, Writer out) throws IOException {
        IOUtils.copy(url.openStream(), out);
    }

    public static Locale getLocale(HttpServletRequest r) {
        Locale locale = null;
        HttpSession s = r.getSession(false);
        if (null == s) {
            return r.getLocale();
        } else if ((locale = (Locale) s.getAttribute(USER_LOCALE)) == null) {
            Cookie cookie = getCookie(r, USER_LOCALE, null);
            if (null == cookie) {
                locale = r.getLocale();
                s.setAttribute(USER_LOCALE, locale);
            } else {
                locale = setLocale(s, cookie.getValue(), r);
            }
        }
        return locale;
    }

    public static Locale setLocale(HttpServletRequest r, String l) {
        HttpSession s = r.getSession(false);
        Locale locale;
        if (null == s) {
            locale = r.getLocale();
        } else {
            locale = setLocale(s, l, r);
        }
        return locale;
    }

    static final Locale setLocale(HttpSession s, String l, HttpServletRequest r) {
        String[] ls = StrUtil.split(l, "_", 2);
        Locale locale;
        int len = ls.length;
        if (1 == len) {
            locale = new Locale(ls[0]);
        } else if (2 == len) {
            locale = new Locale(ls[0], ls[1]);
        } else {
            locale = r.getLocale();
        }
        s.setAttribute(USER_LOCALE, locale);
        return locale;
    }

    public static Cookie getCookie(HttpServletRequest r, String name) {
        return getCookie(r, name, null);
    }

    public static Cookie getCookie(HttpServletRequest r, String name, String path) {
        Cookie[] cookies = r.getCookies();
        for (int i = 0; i < cookies.length; i++) {
            if (name.equalsIgnoreCase(cookies[i].getName())) {
                if (null == path || path.equalsIgnoreCase(cookies[i].getPath())) {
                    return cookies[i];
                }
            }
        }
        return null;
    }
}
