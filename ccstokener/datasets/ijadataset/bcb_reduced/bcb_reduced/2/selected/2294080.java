package com.streamsicle.util;

import java.util.*;
import java.net.*;
import java.io.*;
import org.apache.log4j.Category;

/**
 * A little program that knows how to hammer a Streamsicle server.
 *
 * @author John Watkinson
 */
public class LoadTest implements Runnable {

    /**
    * Indicates that the thread will listen to the stream.
    */
    public static final int THREAD_LISTEN = 1;

    /**
    * Indicates that the thread will control the stream.
    */
    public static final int THREAD_CONTROL = 2;

    /**
    * Port number for listening.
    */
    public static final int LISTEN_PORT = 4711;

    /**
    * Port number for controlling.
    */
    public static final int CONTROL_PORT = 8080;

    /**
    * The buffer size for listening.
    */
    public static final int LISTEN_BUFFER = 1024;

    private static Random random = new Random();

    private int type;

    private long delay;

    private long maxID;

    private String host;

    static Category log = Category.getInstance(LoadTest.class);

    public LoadTest(int type, long delay, long maxID, String host) {
        this.type = type;
        this.delay = delay;
        this.maxID = maxID;
        this.host = host;
    }

    public void error(String text, Exception e) {
        log.error("----- ERROR -----");
        log.error("Thread: " + Thread.currentThread());
        log.error(text);
        e.printStackTrace();
        log.error("-----------------");
    }

    public void event(String text) {
    }

    public void run() {
        if (type == THREAD_LISTEN) {
            listen();
        } else {
            control();
        }
    }

    public void listen() {
        String url = "http://" + host + ":" + LISTEN_PORT;
        HttpURLConnection conn = null;
        while (true) {
            try {
                conn = (HttpURLConnection) (new URL(url).openConnection());
            } catch (Exception e) {
                error("Could not connect to " + url + ".", e);
                return;
            }
            BufferedInputStream in = null;
            try {
                conn.connect();
                in = new BufferedInputStream(conn.getInputStream(), LISTEN_BUFFER);
                event("Connected to stream at " + url + ".");
            } catch (Exception e) {
                error("Could not get stream from " + url + ".", e);
                return;
            }
            try {
                byte[] data = new byte[LISTEN_BUFFER];
                for (int i = 0; i < delay; i++) {
                    in.read(data);
                }
            } catch (Exception e) {
                error("Stream unexpectedly quit from " + url + ".", e);
                return;
            }
        }
    }

    public void control() {
        String urlPrefix = "http://" + host + ":" + CONTROL_PORT + "/servlet/Streamsicle";
        String skipURL = urlPrefix + "?action=skip";
        String addURL = urlPrefix + "?action=add&song=";
        String removeURL = urlPrefix + "?action=action=remove&fileID=";
        String url = null;
        String desc = null;
        while (true) {
            long time = System.currentTimeMillis();
            int action = Math.abs(random.nextInt() % 3);
            long id = 1 + (Math.abs(random.nextLong()) % (maxID - 1));
            switch(action) {
                case 0:
                    {
                        url = skipURL;
                        desc = "Skip song.";
                        break;
                    }
                case 1:
                    {
                        url = addURL + id;
                        desc = "Add song #" + id + ".";
                        break;
                    }
                case 2:
                    {
                        url = removeURL + id;
                        desc = "Remove song #" + id + ".";
                        break;
                    }
            }
            try {
                HttpURLConnection conn = (HttpURLConnection) (new URL(url).openConnection());
                conn.connect();
                String response = "(" + conn.getResponseCode() + ", " + conn.getResponseMessage() + ")";
                event(desc + " Reponse: " + response + ".");
            } catch (Exception e) {
                error("Problem with control action: url.", e);
                return;
            }
            long waitTime = Math.abs(random.nextLong()) % delay;
            long now = System.currentTimeMillis();
            long diff = waitTime - (now - time);
            if (diff > 0) {
                try {
                    Thread.sleep(diff);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public static void showUsage() {
        log.info("Usage:");
        log.info("java com.streamsicle.util.LoadTest host #listeners #controllers packets frequency maxSongID");
        log.info(" where");
        log.info("  'host' is the host running Streamsicle,");
        log.info("  '#listeners' is the number of listening threads,");
        log.info("  '#controllers' is the number of stream-controlling threads,");
        log.info("  'packets' is the number of packets each listening thread will read before disconnecting and reconnecting,");
        log.info("  'frequency' is the average amount of actions performed by a controlling thread per minutea and");
        log.info("  'maxSongID' is the highest song ID the controlling threads should try to request.");
        log.info("\n");
        log.info("PLEASE be sure that maxSongID is a valid ID or else Streamsicle will crash!");
    }

    public static void main(String[] args) {
        if (args.length != 6) {
            showUsage();
            System.exit(0);
        }
        String host = args[0];
        int listeners = 0;
        int controllers = 0;
        int packets = 0;
        int frequency = 0;
        long maxID = 0;
        try {
            listeners = Integer.parseInt(args[1]);
            controllers = Integer.parseInt(args[2]);
            packets = Integer.parseInt(args[3]);
            frequency = Integer.parseInt(args[4]);
            maxID = Long.parseLong(args[5]);
        } catch (NumberFormatException e) {
            log.error("Number required.");
            showUsage();
            System.exit(0);
        }
        long delay = (2 * 60 * 1000) / frequency;
        for (int i = 0; i < listeners; i++) {
            LoadTest test = new LoadTest(THREAD_LISTEN, packets, maxID, host);
            new Thread(test, "Listener #" + i).start();
        }
        for (int i = 0; i < controllers; i++) {
            LoadTest test = new LoadTest(THREAD_CONTROL, delay, maxID, host);
            new Thread(test, "Controller #" + i).start();
        }
    }
}
