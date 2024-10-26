package de.fmf.translate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

public class TransAna_Leo {

    public static final String VERB_LINKS = "verblinks";

    public TransAna_Leo(boolean proxy) {
        if (proxy) {
            System.out.println(this.getClass().getName() + "\tINIT TRANSLATOR USING PROXY");
            System.getProperties().put("proxySet", "true");
            System.getProperties().put("proxyHost", TransHandler.IP);
            System.getProperties().put("proxyPort", TransHandler.PORT);
        } else {
            System.out.println(this.getClass().getName() + "\tINIT TRANSLATOR USING NO PROXY");
        }
    }

    public BufferedReader makeLeoSearchCall(String toTranslate) throws IOException {
        String toTransEnc = java.net.URLEncoder.encode(toTranslate, "ISO8859_1");
        URL url = new URL("http://dict.leo.org/esde?search=" + toTransEnc);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), Charset.forName("ISO8859_1")));
        return in;
    }

    public String makeLeoNounCall(String noun) {
        String ret = "";
        StringBuffer buf = new StringBuffer();
        try {
            URL url = new URL("http://dict.leo.org" + noun);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), Charset.forName("ISO8859_1")));
            String inputLine;
            boolean display = false;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains("contentholder")) {
                    display = true;
                }
                if (display) buf.append(inputLine);
            }
            ret = FilterFunctions.findEndTag("<td", buf.toString());
            sleepRandomTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    private void sleepRandomTime() {
        try {
            int sleepTime = 500 + (new Random().nextInt(7000));
            System.out.println("LEO RANDOM SLEEPING FOR(ms) " + sleepTime);
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, String> translate(String toTranslate) {
        HashMap<String, String> results = new HashMap<String, String>();
        try {
            if (toTranslate != null && !toTranslate.equals("")) {
                BufferedReader in = makeLeoSearchCall(toTranslate);
                String inputLine;
                String[] items = null;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.contains("contentholder")) {
                        items = inputLine.split("checkbox");
                    }
                }
                if (items != null && items.length >= 2) {
                    for (int i = 1; i < items.length; i++) {
                        String[] elements = items[i].split("<td");
                        String toTranslatex = elements[1];
                        String translation = elements[3];
                        toTranslatex = toTranslatex.substring(toTranslate.indexOf('>') + 1);
                        translation = translation.substring(translation.indexOf('>') + 1);
                        results.put(FilterFunctions.replaceHtmlTags(toTranslatex), FilterFunctions.replaceHtmlTags(translation));
                    }
                }
            }
            sleepRandomTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    public HashMap<String, ArrayList<String>> translateSecondPass(String toTranslate) {
        HashMap<String, ArrayList<String>> second = new HashMap<String, ArrayList<String>>();
        try {
            if (toTranslate != null && !toTranslate.equals("")) {
                BufferedReader in = makeLeoSearchCall(toTranslate);
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.contains("Orthographisch �hnliche W�rter - Spanisch:")) {
                    }
                    if (inputLine.contains("Orthographisch �hnliche W�rter - Deutsch:")) {
                    }
                    if (inputLine.contains("konjugierte Verben:")) {
                        String[] res = inputLine.split("konjugierte Verben:");
                        String res1 = res[1];
                        if (res1.contains("rterbuch")) {
                            String[] verbTable = res1.split("rterbuch");
                            String new_words = verbTable[0];
                            String verbTab = verbTable[1];
                            ArrayList<String> verbLinks = FilterFunctions.filter_AHREFS(verbTab);
                            second.put(VERB_LINKS, verbLinks);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return second;
    }
}
