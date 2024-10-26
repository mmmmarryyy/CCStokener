package com.realtybaron.realanswers;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.json.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * User: marc
 * Date: Aug 19, 2009
 * Time: 5:31:24 AM
 * <p/>
 * THIS SOFTWARE IS COPYRIGHTED.  THE SOFTWARE MAY NOT BE COPIED REPRODUCED, TRANSLATED, OR REDUCED TO ANY ELECTRONIC
 * MEDIUM OR MACHINE READABLE FORM WITHOUT THE PRIOR WRITTEN CONSENT OF SOCO TECHNOLOGIES.
 */
public class WebServiceUtil {

    public static String doGet(String spec) throws IOException {
        URL url = new URL(spec);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setUseCaches(false);
        conn.setRequestMethod("GET");
        InputStream istream = conn.getInputStream();
        try {
            return IOUtils.toString(istream);
        } finally {
            IOUtils.closeQuietly(istream);
        }
    }

    public static String doPost(String spec, String params) throws IOException {
        URL url = new URL(spec);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        OutputStream ostream = conn.getOutputStream();
        try {
            IOUtils.write(params, ostream);
            ostream.flush();
        } finally {
            IOUtils.closeQuietly(ostream);
        }
        InputStream istream = conn.getInputStream();
        try {
            return IOUtils.toString(istream);
        } finally {
            IOUtils.closeQuietly(istream);
        }
    }

    public static List<String> getMessages(JSONObject status) throws JSONException {
        List<String> list = Lists.newArrayList();
        JSONObject messages = status.optJSONObject("messages");
        if (messages != null && !messages.isNull("message")) {
            if (messages.optJSONArray("message") != null) {
                JSONArray array = messages.getJSONArray("message");
                for (int i = 0; i < array.length(); i++) {
                    list.add(array.getString(i));
                }
            } else {
                list.add(messages.getString("message"));
            }
        }
        return list;
    }
}
