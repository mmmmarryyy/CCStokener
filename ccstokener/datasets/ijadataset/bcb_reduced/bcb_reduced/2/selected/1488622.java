package com.monad.homerun.pkg.weather.source;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.monad.homerun.core.GlobalProps;
import com.monad.homerun.pkg.weather.Observation;
import com.monad.homerun.pkg.weather.Source;
import com.monad.homerun.pkg.weather.Report;
import com.monad.homerun.pkg.weather.impl.NWSXmlObservation;

/**
 * WebSource obtains a weather report from a web site, for instance the NOAA
 * (National Weather Service) for the configured area. It is essentially a 
 * screen-scraper for various formats, e.g. METAR observations.
 */
public class WebSource implements Source {

    private int state = UNALLOCATED;

    private Properties webProps = null;

    private int updateMins = 0;

    private Logger logger = null;

    private Observation obs = null;

    public WebSource() {
    }

    public boolean init(Logger logger, Properties srcProps) {
        this.logger = logger;
        if (GlobalProps.DEBUG) {
            logger.log(Level.FINE, "WebSource initializing");
        }
        if (state == DEALLOCATED) {
            return false;
        }
        if (state == ALLOCATED) {
            ;
        }
        state = UNALLOCATED;
        webProps = srcProps;
        if (webProps != null) {
            String updateMinsStr = getProperty("updateMins");
            if (updateMinsStr != null && updateMinsStr.length() > 0) {
                try {
                    updateMins = Integer.parseInt(updateMinsStr);
                    long sleepTime = updateMins * 60 * 1000;
                    new SessionThread(sleepTime).start();
                    state = ALLOCATED;
                    return true;
                } catch (NumberFormatException e) {
                    logger.log(Level.SEVERE, "Update interval: '" + updateMinsStr + "' is not a number.");
                }
            } else {
                logger.log(Level.SEVERE, "No update interval property");
            }
        } else {
            logger.log(Level.SEVERE, "No source properties");
        }
        return false;
    }

    public String getName() {
        return getProperty("tag");
    }

    public int getUpdateMins() {
        return updateMins;
    }

    public Report reportWeather() {
        if (state == ALLOCATED && obs != null) {
            Report report = new Report("Web");
            report.setValues(obs);
            return report;
        }
        return null;
    }

    public void shutdown() {
        state = DEALLOCATED;
    }

    private String getProperty(String name) {
        return webProps.getProperty(name);
    }

    private class SessionThread extends Thread {

        private long sleepTime = 0L;

        private boolean stayAlive = true;

        public SessionThread(long sleep) {
            sleepTime = sleep;
        }

        public void stopThread() {
            this.interrupt();
        }

        public void run() {
            while (stayAlive) {
                retrieveData();
                try {
                    sleep(sleepTime);
                } catch (InterruptedException e) {
                    stayAlive = false;
                }
            }
        }
    }

    private void retrieveData() {
        StringBuffer obsBuf = new StringBuffer();
        try {
            URL url = new URL(getProperty("sourceURL"));
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String lineIn = null;
            while ((lineIn = in.readLine()) != null) {
                if (GlobalProps.DEBUG) {
                    logger.log(Level.FINE, "WebSource retrieveData: " + lineIn);
                }
                obsBuf.append(lineIn);
            }
            String fmt = getProperty("dataFormat");
            if (GlobalProps.DEBUG) {
                logger.log(Level.FINE, "Raw: " + obsBuf.toString());
            }
            if ("NWS XML".equals(fmt)) {
                obs = new NWSXmlObservation(obsBuf.toString());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Can't connect to: " + getProperty("sourceURL"));
            if (GlobalProps.DEBUG) {
                e.printStackTrace();
            }
        }
    }
}
