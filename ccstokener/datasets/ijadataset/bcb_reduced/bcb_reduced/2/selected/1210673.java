package org.formaria.editor.langed;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * <p>Language Resource Translation Utility</p>
 * <p> Copyright (c) Formaria Ltd., 2001-2006, This software is licensed under
 * the GNU Public License (GPL), please see license.txt for more details. If
 * you make commercial use of this software you must purchase a commercial
 * license from Formaria.</p>
 * <p> $Revision: 1.4 $</p>
 */
public class AutoTranslate {

    public static String translate(String txUrl, String directionPrefix, String direction, String srcPrefix, String srcLangString, String startOfResultTag, String endOfResultTag) throws Exception {
        try {
            URLConnection conn;
            URL url = new URL(txUrl);
            String parms = directionPrefix + URLEncoder.encode(direction, "UTF-8");
            parms += srcPrefix + URLEncoder.encode(srcLangString, "UTF-8");
            conn = url.openConnection();
            conn.setDefaultUseCaches(false);
            conn.setDoOutput(true);
            conn.setIfModifiedSince(0);
            OutputStream o = conn.getOutputStream();
            o.write(parms.getBytes());
            o.flush();
            conn.connect();
            InputStream is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            char[] c = new char[4096];
            int i = 1;
            String result = new String("");
            while (i != -1) {
                c[0] = c[1] = '\0';
                i = br.read(c, 0, 4096);
                if (i != -1) result += new String(c, 0, i);
            }
            String search = startOfResultTag;
            String lowerCaseResult = result.toLowerCase();
            int start = lowerCaseResult.indexOf(search.toLowerCase());
            if (start >= 0) {
                int end = lowerCaseResult.indexOf(endOfResultTag.toLowerCase(), start + 1);
                return result.substring(start + search.length(), end).trim();
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception();
        }
    }
}
