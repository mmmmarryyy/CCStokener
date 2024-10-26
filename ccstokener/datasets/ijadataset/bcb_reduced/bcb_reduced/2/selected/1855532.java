package com.astromine.mp3;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * The Translator factory creates a translator object that can 
 * decompose and to compose a media play list of the type specified 
 * @author stephen
 *
 */
public class TranslatorFactory {

    /**
     * 
     */
    public TranslatorFactory() {
        super();
    }

    /**
     * Creates a translator to decompose and compose a media play list 
     * @param content the contents of a play list or redirector file
     * @return An object able to decompose the play list
     */
    public static Translator createTranslator(String content) {
        Translator translator = null;
        if (content == null || content.trim().length() == 0) {
            ;
        } else if (content.toLowerCase().contains("[playlist]")) {
            translator = new PLSTranslator(content);
        } else if (content.toUpperCase().contains("#EXTM3U")) {
            translator = new M3UTranslator(content);
        } else if (content.toUpperCase().contains("<ASX")) {
            translator = new ASXTranslator(content);
        }
        return translator;
    }

    /**
     * Creates a translator to decompose and compose a media play list 
     * @param url the Internet address of the media play list or redirector file
     * @return An object able to decompose the play list
     */
    public static Translator createTranslator(URL url) throws IOException {
        return createTranslator(url.openConnection());
    }

    /**
     * Creates a translator to decompose and compose a media play list 
     * @param connection the Internet connection to the media play list or redirector file
     * @return A Translator able to decompose the play list
     */
    public static Translator createTranslator(URLConnection connection) {
        Translator translator = null;
        Map<String, List<String>> header = null;
        String fileName = null;
        try {
            URL url = connection.getURL();
            fileName = url.getFile();
            connection.setConnectTimeout(15000);
            if (!fileName.toLowerCase().endsWith("asx")) {
                connection.setRequestProperty("user-agent", "Winamp/5.52");
            }
            connection.setRequestProperty("accept", "*/*");
            connection.connect();
            header = connection.getHeaderFields();
            if (header.get("Content-Disposition") != null && header.get("Content-Disposition").size() > 0) {
                String disposition = header.get("Content-Disposition").get(0);
                if (disposition.toLowerCase().contains("filename")) {
                    int start = disposition.indexOf("filename");
                    start = disposition.indexOf("=", start);
                    int end = disposition.indexOf(";", start);
                    if (end > start) {
                        fileName = disposition.substring(start + 1, end).trim();
                    } else {
                        fileName = disposition.substring(start + 1).trim();
                    }
                    if (fileName.startsWith("\"")) {
                        fileName = fileName.substring(1);
                    }
                    if (fileName.endsWith("\"")) {
                        fileName = fileName.substring(0, fileName.length() - 1);
                    }
                }
            }
            if (fileName.toLowerCase().endsWith("m3u") || connection.getContentType().contains("x-mpegurl")) {
                translator = new M3UTranslator(connection.getInputStream());
            } else if (fileName.toLowerCase().endsWith("pls") || connection.getContentType().contains("x-scpls") || connection.getContentType().contains("scpls")) {
                translator = new PLSTranslator(connection.getInputStream());
            } else if (fileName.toLowerCase().endsWith("asx") || connection.getContentType().contains("video/x-ms-asf")) {
                translator = new ASXTranslator(connection.getInputStream());
            } else if (connection.getContentType().contains("audio/mpeg") || connection.getContentType().contains("audio/aacp")) {
                translator = new StreamTranslator(connection.getURL());
            } else if (connection.getContentType().contains("unknown/unknown")) {
                translator = new StreamTranslator(connection.getURL());
            } else {
                System.out.println("Un-recognized file type ");
                System.out.println("Content-Type:       " + connection.getContentType());
                System.out.println("filename:           " + fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return translator;
    }
}
