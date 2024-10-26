package de.fmf.translate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;

public class TransAna_Leo_Verb extends TransAna_Leo {

    public static void main(String[] args) {
        new TransAna_Leo_Verb(true);
    }

    public TransAna_Leo_Verb(boolean proxy) {
        super(proxy);
        translate("buscar");
    }

    public BufferedReader makeLeoSearchCall(String toTranslate) throws IOException {
        String toTransEnc = java.net.URLEncoder.encode(toTranslate, "ISO8859_1");
        URL url = new URL("http://dict.leo.org/pages.esde/stemming/verb_ar_averiguar.html?Hilfsverb=haber&stamm=bus&stamm2=c&stamm3=qu");
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), Charset.forName("ISO8859_1")));
        return in;
    }

    public HashMap<String, String> translate(String toTranslate) {
        HashMap<String, String> results = new HashMap<String, String>();
        try {
            if (toTranslate != null && !toTranslate.equals("")) {
                BufferedReader in = makeLeoSearchCall(toTranslate);
                String inputLine;
                String[] items = null;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println(inputLine);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(1250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return results;
    }
}
