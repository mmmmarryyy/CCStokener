package it.ilz.hostingjava.scheduler;

import com.sun.syndication.feed.synd.SyndEntry;
import it.ilz.hostingjava.listener.Context;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

/**
 *
 * @author luigi
 */
public class FeedReader implements Job {

    public static Log log = LogFactory.getLog(FeedReader.class);

    private static int timer = 1000 * 60 * 60 * 8;

    /** Creates a new instance of FeedReader */
    public FeedReader() {
    }

    private static List entryList = new ArrayList();

    public static final String NAME = "feeds";

    /**
     *la logica di caricamento dei feed &egrave; la seguente:
     *leggo tutti gli url, carico gli ultimi feed per ogni url.
     *l'elenco deve contenere almento 10 indirizzi.
     *Se il numero di url<5 carico 10/num_url entry per ogni blog
     *Se il numero di url>5 carico 2 entry per ogni url
     *Se il numero di url>10 carico 1 entri per ogni url
     *Ordino i feed recuperati per data
     */
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            ServletContext context = (ServletContext) jobExecutionContext.getScheduler().getContext();
            synchronized (context) {
                if (context.getAttribute(NAME) == null) context.setAttribute(NAME, this);
            }
            initFeeds(jobExecutionContext.getJobDetail().getJobDataMap().getString("feedUrls"));
        } catch (SchedulerException ex) {
            LogFactory.getLog(this.getClass()).warn(ex, ex);
            throw new JobExecutionException(ex);
        }
    }

    private void initFeeds(String feedUrls) {
        String[] urls = feedUrls.split(";");
        List entryList = new ArrayList();
        int entry_num = (10 / urls.length) < 0 ? 1 : (10 / urls.length);
        for (int i = 0; i < urls.length; i++) {
            try {
                java.net.URL url = new java.net.URL(urls[i]);
                com.sun.syndication.io.SyndFeedInput input = new com.sun.syndication.io.SyndFeedInput();
                com.sun.syndication.feed.synd.SyndFeed feed = input.build(new java.io.InputStreamReader(url.openStream()));
                int count = (feed.getEntries().size() > entry_num) ? entry_num : feed.getEntries().size();
                for (int j = 0; j < count; j++) {
                    entryList.add(feed.getEntries().get(j));
                }
            } catch (Exception e) {
                log.error("error reading url " + urls[i], e);
            }
        }
        Comparator comp = new Comparator() {

            public int compare(Object o1, Object o2) {
                if (o1 == null || o2 == null) return 0;
                if (((SyndEntry) o1).getPublishedDate() == null || ((SyndEntry) o2).getPublishedDate() == null) return 0;
                return ((SyndEntry) o2).getPublishedDate().compareTo(((SyndEntry) o1).getPublishedDate());
            }

            public boolean equals(Object obj) {
                return false;
            }
        };
        Collections.sort(entryList, comp);
        setEntryList(entryList);
    }

    public List getEntryList() {
        return entryList;
    }

    public synchronized void setEntryList(List entryList) {
        FeedReader.entryList = entryList;
    }
}
