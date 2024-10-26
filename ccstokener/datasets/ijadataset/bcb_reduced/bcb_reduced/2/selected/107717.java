package com.beust.android.translate;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import org.json.JSONObject;

/**
 * Makes the Google Translate API available to Java applications.
 *
 * @author Richard Midwinter
 * @author Emeric Vernat
 * @author Juan B Cabral
 */
public class Translate {

    private static final String ENCODING = "UTF-8";

    private static final String URL_STRING = "http://ajax.googleapis.com/ajax/services/language/translate?v=1.0&langpair=";

    private static final String TEXT_VAR = "&q=";

    /**
     * Translates text from a given language to another given language using Google Translate
     *
     * @param text The String to translate.
     * @param from The language code to translate from.
     * @param to The language code to translate to.
     * @return The translated String.
     * @throws MalformedURLException
     * @throws IOException
     */
    public static String translate(String text, String from, String to) throws Exception {
        return retrieveTranslation(text, from, to);
    }

    /**
     * Forms an HTTP request and parses the response for a translation.
     *
     * @param text The String to translate.
     * @param from The language code to translate from.
     * @param to The language code to translate to.
     * @return The translated String.
     * @throws Exception
     */
    private static String retrieveTranslation(String text, String from, String to) throws Exception {
        try {
            StringBuilder url = new StringBuilder();
            url.append(URL_STRING).append(from).append("%7C").append(to);
            url.append(TEXT_VAR).append(URLEncoder.encode(text, ENCODING));
            Log.d(TranslateActivity.TAG, "Connecting to " + url.toString());
            HttpURLConnection uc = (HttpURLConnection) new URL(url.toString()).openConnection();
            uc.setDoInput(true);
            uc.setDoOutput(true);
            try {
                Log.d(TranslateActivity.TAG, "getInputStream()");
                InputStream is = uc.getInputStream();
                String result = toString(is);
                JSONObject json = new JSONObject(result);
                return ((JSONObject) json.get("responseData")).getString("translatedText");
            } finally {
                uc.getInputStream().close();
                if (uc.getErrorStream() != null) uc.getErrorStream().close();
            }
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
     * Reads an InputStream and returns its contents as a String. Also effects rate control.
     * @param inputStream The InputStream to read from.
     * @return The contents of the InputStream as a String.
     * @throws Exception
     */
    private static String toString(InputStream inputStream) throws Exception {
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
