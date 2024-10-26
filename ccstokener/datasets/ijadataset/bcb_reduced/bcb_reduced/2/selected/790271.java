package org.sunshine.mamadu;

import org.w3c.dom.Document;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class MamaduCheck extends Thread {

    private static final String DEFAULT_HISTORY_FILE_NAME = System.getProperty("user.home") + File.separator + "mamadu_history.xml";

    private static final String DEFAULT_MAMADU_URL = "http://mamadu.ru/abrd/doskaest.php?n=0232";

    public static interface Listener {

        public void handleNewAds(List<Advertisement> list);

        public void handleInitAds(List<Advertisement> list);
    }

    private String url;

    private boolean running;

    private long sleepTime = 15 * 60 * 1000;

    private String historyFilePath = DEFAULT_HISTORY_FILE_NAME;

    private static final MamaduParser mamaduParser = new MamaduParser();

    private List<Listener> listeners = new ArrayList<Listener>();

    public MamaduCheck(String url) {
        if (url != null && url.length() > 0) {
            this.url = url;
        } else this.url = DEFAULT_MAMADU_URL;
    }

    public MamaduCheck() {
        this(null);
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    public void stopCheking() {
        running = false;
    }

    public void setSleepTime(long sleepTime) {
        this.sleepTime = sleepTime;
    }

    public void run() {
        running = true;
        try {
            List<Advertisement> adHistory = XmlUtils.getAdvertisementsFromFile(historyFilePath);
            fireInitAds(adHistory);
            while (running) {
                LocalLogger.LOGGER.info("Getting ads from mamadu...");
                String content = null;
                try {
                    content = getContent();
                } catch (IOException e) {
                    LocalLogger.LOGGER.severe("Cannot get html from " + url + ". Another attempt in 10 seconds.");
                    Thread.sleep(10 * 1000);
                    continue;
                }
                List<Advertisement> freshList = mamaduParser.parse(content);
                List<Advertisement> newAds = getNewAds(adHistory, freshList);
                if (newAds.size() > 0) {
                    fireNewAds(newAds);
                    saveHistory(adHistory);
                    fireInitAds(adHistory);
                }
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    LocalLogger.LOGGER.log(Level.WARNING, "Exception during chenk sleep.", e);
                }
            }
        } catch (Exception e) {
            LocalLogger.LOGGER.log(Level.SEVERE, "Error during checking.", e);
        }
    }

    private void fireInitAds(List<Advertisement> freshList) throws Exception {
        LocalLogger.LOGGER.info("Initiation ads list...");
        for (Listener l : listeners) {
            l.handleInitAds(freshList);
        }
    }

    private void fireNewAds(List<Advertisement> newAds) {
        LocalLogger.LOGGER.info("WOW! Here is new ads on the site!");
        for (Listener l : listeners) {
            l.handleNewAds(newAds);
        }
    }

    public String getContent() throws IOException {
        URLConnection urlConnection = new URL(url).openConnection();
        InputStream inputStream = urlConnection.getInputStream();
        byte[] bytes = read(inputStream);
        String response = new String(bytes, "windows-1251");
        inputStream.close();
        return response;
    }

    public static byte[] read(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int ch;
        while ((ch = inputStream.read()) >= 0) {
            buf.write(ch);
        }
        if (buf.size() == 0) {
            return null;
        }
        return buf.toByteArray();
    }

    /**
     * Check if there is new advertisemens in the fresh list. If so, add them to history and return.
     */
    private static List<Advertisement> getNewAds(List<Advertisement> adHistory, List<Advertisement> freshList) {
        List<Advertisement> newAds = new ArrayList<Advertisement>();
        for (Advertisement advertisement : freshList) {
            if (adHistory.contains(advertisement)) continue;
            newAds.add(advertisement);
            adHistory.add(advertisement);
        }
        return newAds;
    }

    private void saveHistory(List<Advertisement> freshList) throws Exception {
        LocalLogger.LOGGER.fine("Saving history file.");
        Document document = XmlUtils.buildDOM(freshList);
        XmlUtils.saveDomToFile(document, historyFilePath);
    }
}
