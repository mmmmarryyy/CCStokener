package com.google.api.translate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.json.JSONObject;
import com.tecnick.htmlutils.htmlentities.HTMLEntities;

/**
 * Makes the Google Translate API available to Java applications.
 * 
 * @author Richard Midwinter
 * @author Emeric Vernat
 * @author Juan B Cabral
 */
public final class Translate {

    private static final String ENCODING = "UTF-8";

    private static final String URL_STRING = "http://ajax.googleapis.com/ajax/services/language/translate?v=1.0&langpair=";

    private static final String TEXT_VAR = "&q=";

    private static String referrer = "http://code.google.com/p/google-api-translate-java/";

    /**
     * Translate. Private constructor as we should not need to initialize it.
     */
    private Translate() {
    }

    /**
     * Sets the HTTP Referrer.
     * @param pReferrer The HTTP referrer parameter.
     */
    public static void setHttpReferrer(final String pReferrer) {
        referrer = pReferrer;
    }

    /**
     * Translates text from a given language to another given language using Google Translate.
     * 
     * @param text The String to translate.
     * @param from The language code to translate from.
     * @param to The language code to translate to.
     * @return The translated String.
     * @throws Exception on error.
     */
    public static String translate(final String text, final Language from, final Language to) throws Exception {
        return retrieveTranslation(text, from, to);
    }

    /**
     * Forms an HTTP request and parses the response for a translation.
     * 
     * @param text The String to translate.
     * @param from The language code to translate from.
     * @param to The language code to translate to.
     * @return The translated String.
     * @throws Exception on error.
     */
    private static String retrieveTranslation(final String text, final Language from, final Language to) throws Exception {
        try {
            final StringBuilder url = new StringBuilder();
            url.append(URL_STRING).append(from).append("%7C").append(to);
            url.append(TEXT_VAR).append(URLEncoder.encode(text, ENCODING));
            final HttpURLConnection uc = (HttpURLConnection) new URL(url.toString()).openConnection();
            uc.setRequestProperty("referer", referrer);
            try {
                String result = toString(uc.getInputStream());
                final JSONObject json = new JSONObject(result);
                final String translatedText = ((JSONObject) json.get("responseData")).getString("translatedText");
                return HTMLEntities.unhtmlentities(translatedText);
            } finally {
                uc.getInputStream().close();
                if (uc.getErrorStream() != null) {
                    uc.getErrorStream().close();
                }
            }
        } catch (Exception ex) {
            throw new Exception("[google-api-translate-java] Error retrieving translation.", ex);
        }
    }

    /**
     * Reads an InputStream and returns its contents as a String.
     * Also effects rate control.
     * @param inputStream The InputStream to read from.
     * @return The contents of the InputStream as a String.
     * @throws Exception on error.
     */
    private static String toString(final InputStream inputStream) throws Exception {
        StringBuilder outputBuilder = new StringBuilder();
        try {
            String string;
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, ENCODING));
                while (null != (string = reader.readLine())) {
                    outputBuilder.append(string).append('\n');
                }
            }
        } catch (Exception ex) {
            throw new Exception("[google-api-translate-java] Error reading translation stream.", ex);
        }
        return outputBuilder.toString();
    }
}
