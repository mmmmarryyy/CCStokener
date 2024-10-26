package com.kcode.threads;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

public class ISBNReader implements Runnable {

    private static final int RETRIES = 10;

    private Logger logger = Logger.getLogger(this.getClass());

    private ArrayBlockingQueue<String> outputIsbns;

    private List<String> urls;

    private static final int START = 0;

    private static final int STEP = 100;

    private AtomicBoolean moreInput;

    public ISBNReader(ArrayBlockingQueue<String> outputIsbns, List<String> urls, AtomicBoolean moreInput) {
        this.outputIsbns = outputIsbns;
        this.urls = urls;
        this.moreInput = moreInput;
    }

    @Override
    public void run() {
        moreInput.set(true);
        for (String url : urls) {
            int totalMatches = 0;
            int matches = 0;
            url = initURL(url);
            System.out.println("Using URL: " + url);
            do {
                try {
                    matches = scrapeForIsbns(new URL(url));
                    totalMatches += matches;
                    logger.debug("finished scraping " + url);
                    url = replaceGetVars(url);
                } catch (MalformedURLException e) {
                    logger.fatal(e);
                }
                logger.info("matches/totalMatches: " + matches + "/" + totalMatches);
            } while (matches > 0 && totalMatches < 10000);
        }
        moreInput.set(false);
        System.out.println(this.getClass().getName() + " is done");
        logger.fatal("I'm done!");
    }

    public int scrapeForIsbns(URL url) {
        int matches = 0;
        Pattern p = Pattern.compile("ISBN-10:</strong>\\s*(\\w{10})");
        Matcher m;
        for (int i = 0; i < RETRIES; i++) {
            try {
                InputStream in = url.openStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String str;
                while ((str = br.readLine()) != null) {
                    m = p.matcher(str);
                    while (m.find()) {
                        matches++;
                        outputIsbns.put(m.group(1));
                        logger.debug("Got ISBN: " + m.group(1));
                    }
                }
                break;
            } catch (ConnectException e) {
                logger.warn("Connection attempt " + i + " failed, trying again. Max retries: " + RETRIES);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }
            } catch (IOException e) {
                logger.error("Error reading URL stream", e);
            } catch (InterruptedException e) {
                logger.error("Interrupted while calling put(Object E)", e);
            }
        }
        return matches;
    }

    private String initURL(String url) {
        Pattern p = Pattern.compile("&?(ls=\\d*)&?");
        Matcher m = p.matcher(url);
        String match = "";
        if (m.find()) {
            match = m.group(1);
            url = url.replace(match, "ls=" + START);
        } else {
            url += "&ls=" + START;
        }
        p = Pattern.compile("&?(l=\\d*)&?");
        m = p.matcher(url);
        if (m.find()) {
            match = m.group(1);
            url = url.replace(match, "l=" + STEP);
        } else {
            url += "&l=" + STEP;
        }
        return url;
    }

    private String replaceGetVars(String urlStr) {
        String url = new String(urlStr);
        Pattern p = Pattern.compile("&?(ls=(\\d*))&?");
        Matcher m = p.matcher(url);
        String match = "";
        String bookIndex = "";
        if (m.find()) {
            match = m.group(1);
            bookIndex = m.group(2);
            url = url.replace(match, "ls=" + (Integer.valueOf(bookIndex) + STEP));
        } else {
            throw new RuntimeException("Didn't find 'ls' get var. URL not setup correctly.");
        }
        return url;
    }
}
