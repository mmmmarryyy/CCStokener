package com.xebia.jarep.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.jamonapi.MonitorComposite;
import com.jamonapi.MonitorFactory;
import com.xebia.jarep.persister.Persister;

/**
 * Uses the fetcher to fetch jamon data then uses the persister to store the
 * data in a database. <br>
 * This is the man in the middle that uses a concrete fetcher to retrieve a
 * jamon data message
 * 
 * @author shautvast
 * 
 */
public class JamonDataTransporter implements Runnable {

    private static Log log = LogFactory.getLog(JamonDataTransporter.class);

    /**
   * stores the data.
   */
    private Persister persister;

    /**
   * Contains key:value => prefix:jamonServerURL Needed for remote fetch of
   * jamondata
   */
    private HashMap jamonServers;

    /**
   * a prefix can be inserted here. Viable for some cases
   */
    private String prefix;

    /**
   * The prefix is injected into the remote server at the http request. It must
   * be because it needn't be aware of it.
   */
    public void transportRemoteData() {
        for (Iterator serverEntries = jamonServers.entrySet().iterator(); serverEntries.hasNext(); ) {
            Entry entry = (Entry) serverEntries.next();
            String prefix = (String) entry.getKey();
            String jamonUrlString = (String) entry.getValue();
            try {
                URL jamonUrl = new URL(jamonUrlString + "?prefix=" + prefix + "&reset=true");
                String dataString = fetchRemoteData(jamonUrl);
                persister.store(dataString);
            } catch (MalformedURLException e) {
                log.error("wrong jamon url:" + jamonUrlString);
            } catch (IOException e) {
                log.error("error fetching jamon data", e);
            }
        }
    }

    /**
   * Collect local jamon data directly and store it in a database
   */
    public void transportLocalData() {
        log.info("store data");
        MonitorComposite rootMonitor = MonitorFactory.getRootMonitor();
        if (rootMonitor == null) {
            log.warn("No Jamon data found");
        } else {
            persister.store(prefix, rootMonitor.getData());
            MonitorFactory.reset();
        }
    }

    /**
   * Fetches jamon data from a single URL
   * 
   * @throws IOException
   */
    public String fetchRemoteData(URL url) throws IOException {
        URLConnection jamonCon = url.openConnection();
        BufferedReader jamonReader = new BufferedReader(new InputStreamReader(jamonCon.getInputStream()));
        StringBuffer buffer = new StringBuffer();
        String line = jamonReader.readLine();
        while (line != null) {
            buffer.append(line);
            line = jamonReader.readLine();
        }
        return buffer.toString();
    }

    /**
   * @return
   */
    public HashMap getJamonServers() {
        return jamonServers;
    }

    /**
   * @param jamonServers
   */
    public void setJamonServers(HashMap jamonServers) {
        this.jamonServers = jamonServers;
    }

    /**
   * @return
   */
    public Persister getPersister() {
        return persister;
    }

    /**
   * @param persister
   */
    public void setPersister(Persister persister) {
        this.persister = persister;
    }

    public void run() {
    }

    /**
   * @return the prefix
   */
    public String getPrefix() {
        return prefix;
    }

    /**
   * @param prefix
   *          the prefix to set
   */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
