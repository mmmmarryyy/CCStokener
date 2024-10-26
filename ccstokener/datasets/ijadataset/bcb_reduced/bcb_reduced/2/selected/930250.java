package com.unmagination.unboten.agent;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * Connection Speed Class
 * 
 * @author Andre Barbosa (andre.barbosa@unmagination.com)
 */
public class ConnectionSpeed implements Runnable {

    private static Logger logger = Logger.getLogger(ConnectionSpeed.class);

    private Thread thread;

    private Boolean running;

    private List<Long> responses;

    /**
   * Class Constructor
   * 
   */
    public ConnectionSpeed() {
        this.running = true;
        this.responses = new LinkedList<Long>();
        this.thread = new Thread(this, "connection-speed");
        this.thread.start();
    }

    private void addResponse() {
        try {
            long before = System.currentTimeMillis();
            URLConnection urlConnection = new URL(ConnectionSpeedProperties.URL).openConnection();
            urlConnection.connect();
            logger.debug(urlConnection.getHeaderFields().get(null).get(0));
            if (this.responses.size() >= ConnectionSpeedProperties.ENTRIES) {
                this.responses.remove(0);
            }
            this.responses.add(System.currentTimeMillis() - before);
        } catch (MalformedURLException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.debug(e.getMessage());
        }
    }

    /**
   * Returns the Connection Speed Thread
   * 
   * @return the Connection Speed Thread
   */
    public Thread getThread() {
        return this.thread;
    }

    /**
   * Returns the Connection Speed Average Response Time
   * 
   * @return the Connection Speed Average Response Time
   */
    public Long getAverageResponseTime() {
        if (this.responses != null && this.responses.size() > 0) {
            long responseTime = 0;
            for (int i = 0; i < this.responses.size(); i++) {
                responseTime = responseTime + this.responses.get(i);
            }
            return ((long) responseTime / this.responses.size());
        }
        return Long.MAX_VALUE;
    }

    /**
   * Checks if the Connection Speed Thread is running
   * 
   * @return true if it is, false if not
   */
    public Boolean isRunning() {
        return this.running;
    }

    /**
   * Runs the Connection Speed Thread
   * 
   */
    public void run() {
        long millis = (long) (ConnectionSpeedProperties.INTERVAL * 1000);
        logger.debug("Connection Speed Thread is running.");
        while (this.running) {
            this.addResponse();
            sleep(millis);
        }
    }

    /**
   * Puts the Connection Speed Thread to sleep for a number of milliseconds
   * 
   * @param millis
   *          the number of milliseconds to sleep
   */
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            logger.debug("Connection Speed Thread interrupted during sleep.");
        }
    }

    /**
   * Stops the Connection Speed Thread
   * 
   */
    public synchronized void stop() {
        this.running = false;
        this.thread.interrupt();
    }
}
