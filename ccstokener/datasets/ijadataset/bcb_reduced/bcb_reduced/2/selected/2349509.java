package com.sebscape.sebcms.tools;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.Security;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.sun.net.ssl.internal.ssl.Provider;

/**
 * @author Stephen
 *
 */
@SuppressWarnings("unused")
public class HttpUtils {

    private static Log log = LogFactory.getLog(HttpUtils.class);

    /**
	 *  Default Constructor
	 **/
    public HttpUtils() {
    }

    /**
	 * Use this method to post a form to a url. It then looks for a substring in the response page, and returns true or
	 * false to indicate the substring was found. Currently, it is used by the contactForm portlet to
	 * submit an email to the PigeonEmail service, 'true' indicating the submission was successful.
	 * @param urlString url to which the form is posted.
	 * @param formMap Map of field_names/values to be submitted.
	 * @param successString if this is not null, the page returned will be scanned for this substring indicating success.
	 * @return true if the substring was found in the response, false if not.
	 */
    public static boolean postForm(String urlString, Map<String, String> formMap, String successString) throws IOException, MalformedURLException {
        String resultString = postForm(urlString, formMap);
        if (successString == null) {
            return true;
        } else if (resultString == null || "".equals(resultString)) {
            return false;
        } else if (resultString.toLowerCase().indexOf(successString.toLowerCase()) != -1) {
            return true;
        } else {
            return false;
        }
    }

    /**
	 * Method used to post a form to a url.
	 * @param urlString URL to which the form will be submitted.
	 * @param formMap the form field names -> values.
	 * @return the response from the URLConnection after the form is submitted.
	 * @throws IOException
	 * @throws MalformedURLException
	 */
    public static String postForm(String urlString, Map<String, String> formMap) throws IOException, MalformedURLException {
        URL url;
        URLConnection urlConn;
        DataOutputStream printout;
        BufferedReader input;
        StringBuffer urlResponse;
        url = getUrl(urlString);
        if (url == null) {
            return null;
        }
        urlConn = (URLConnection) url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        printout = new DataOutputStream(urlConn.getOutputStream());
        if (formMap != null) {
            StringBuffer formContent = new StringBuffer();
            Iterator<String> iter = formMap.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                formContent.append(URLEncoder.encode(key, "UTF-8"));
                formContent.append('=');
                if (formMap.get(key) != null) formContent.append(URLEncoder.encode((String) formMap.get(key), "UTF-8"));
                if (iter.hasNext()) formContent.append('&');
            }
            printout.writeBytes(formContent.toString());
            printout.flush();
            printout.close();
        }
        urlResponse = new StringBuffer();
        input = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
        String str;
        while (null != ((str = input.readLine()))) {
            log.debug(str);
            urlResponse.append(str + "\n");
        }
        input.close();
        return urlResponse.toString();
    }

    public static URL getUrl(String urlString) throws MalformedURLException {
        URL url = null;
        url = new URL(urlString);
        return url;
    }
}
