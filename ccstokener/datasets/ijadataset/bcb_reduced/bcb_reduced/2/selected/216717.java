package hrc.tool.net;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * the tool has sth to do with the web service
 * @author hrc
 *
 */
public class WebUnit {

    /**
	 * get  user name from cookie
	 * @param request
	 * @param key the cookie key which save the name of the user
	 * @return user name
	 */
    public static String getUserNameByCookie(HttpServletRequest request, String key) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (key.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
	 * a easy-webserver funtion which get message from other serivce by url
	 * @param strUrl
	 * @return date from other service
	 */
    public static String getWebService(String strUrl) {
        return webService(strUrl);
    }

    /**
	 * a easy-webserver funtion which get message from other serivce by url
	 * 
	 * @param strUrl
	 *            the url string with "?"
	 * @param values
	 *            the true value of "?"
	 * @return date from other service
	 * @throws Exception
	 */
    public static String getWebService(String strUrl, String[] values) throws Exception {
        return webService(StringParamSwitcher.replaceBrackets(strUrl, values));
    }

    private static String webService(String strUrl) {
        StringBuffer buffer = new StringBuffer();
        try {
            URL url = new URL(strUrl);
            InputStream input = url.openStream();
            String sCurrentLine = "";
            InputStreamReader read = new InputStreamReader(input, "utf-8");
            BufferedReader l_reader = new java.io.BufferedReader(read);
            while ((sCurrentLine = l_reader.readLine()) != null) {
                buffer.append(sCurrentLine);
            }
            return buffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static class StringParamSwitcher {

        public static String replaceBrackets(String str, String[] params) throws Exception {
            StringBuffer buffer = new StringBuffer();
            Pattern pattern = Pattern.compile("\\{\\d\\}");
            Matcher matcher = pattern.matcher(str);
            int i = 0;
            while (matcher.find()) {
                matcher.appendReplacement(buffer, params[i]);
                i++;
            }
            if (params.length != i) {
                throw new Exception("\"{\\d}\" and paramters length is not match");
            }
            matcher.appendTail(buffer);
            return buffer.toString();
        }
    }
}
