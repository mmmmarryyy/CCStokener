package net.sf.imca.services;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.apache.commons.lang.math.RandomUtils;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.HashMapFeedInfoCache;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;
import net.sf.imca.model.AdBannerBO;
import net.sf.imca.model.BoatBO;
import net.sf.imca.model.BoatForSaleBO;
import net.sf.imca.model.DesignBO;
import net.sf.imca.model.EquipmentSupplierBO;
import net.sf.imca.model.EventBO;
import net.sf.imca.model.NewsFeedBO;
import net.sf.imca.model.NewsItemBO;
import net.sf.imca.model.PageBO;
import net.sf.imca.model.PersonBO;
import net.sf.imca.model.AssociationBO;
import net.sf.imca.model.entities.AdBannerEntity;
import net.sf.imca.model.entities.AssociationEntity;
import net.sf.imca.model.entities.BoatEntity;
import net.sf.imca.model.entities.BoatForSaleEntity;
import net.sf.imca.model.entities.DesignEntity;
import net.sf.imca.model.entities.EquipmentSupplierEntity;
import net.sf.imca.model.entities.EventEntity;
import net.sf.imca.model.entities.MembershipEntity;
import net.sf.imca.model.entities.NewsFeedEntity;
import net.sf.imca.model.entities.NewsItemEntity;
import net.sf.imca.model.entities.PageEntity;
import net.sf.imca.model.entities.PersonEntity;
import net.sf.imca.model.sort.EventComparator;
import net.sf.imca.model.sort.NewsComparator;
import net.sf.imca.model.sort.NewsFeedBOComparator;
import net.sf.imca.model.sort.NewsFeedComparator;
import net.sf.imca.web.backingbeans.Utils;

/**
 * Service User Interface building.
 *
 * @author dougculnane
 */
public class UiService extends Service {

    private static Date nextIcalUpdate = new Date();

    private static Date nextNewsFeedUpdate = new Date();

    private static Hashtable<String, BlogTableEntry> blogTable = new Hashtable<String, BlogTableEntry>();

    private static List<NewsFeedBO> newsFeeds = new ArrayList<NewsFeedBO>();

    private static boolean updateingBlogs = false;

    /**
     * Default Constructor.
     */
    public UiService() {
        super();
    }

    public AssociationBO[] getAssociationsWithWebsites() {
        this.startTransaction();
        List<AssociationEntity> list = getAssociationsWithWebsites(em);
        AssociationBO[] associations = new AssociationBO[list.size()];
        for (int i = 0; i < associations.length; i++) {
            associations[i] = new AssociationBO(list.get(i));
        }
        this.endTransaction();
        return associations;
    }

    @SuppressWarnings("unchecked")
    private List<AssociationEntity> getAssociationsWithWebsites(EntityManager em) {
        Query query = em.createNamedQuery("findAssociationsWithWebsites");
        return query.getResultList();
    }

    private NewsFeedEntity[] getNewsFeedEntities() {
        this.startTransaction();
        Query query = em.createNamedQuery("findNewsFeeds");
        List<NewsFeedEntity> list = query.getResultList();
        NewsFeedEntity[] feeds = new NewsFeedEntity[list.size()];
        for (int i = 0; i < feeds.length; i++) {
            feeds[i] = list.get(i);
        }
        this.endTransaction();
        return feeds;
    }

    public PersonBO[] getMembersWithWebsites() {
        this.startTransaction();
        List<MembershipEntity> list = findActiveMembersOnDate(em, new Date());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getPerson().getUrl().length() == 0) {
                list.remove(i);
                i--;
            }
        }
        Hashtable<Long, PersonEntity> uniquePersons = new Hashtable<Long, PersonEntity>();
        for (int i = 0; i < list.size(); i++) {
            if (uniquePersons.get(new Long(list.get(i).getPerson().getId())) != null) {
                list.remove(i);
                i--;
            } else {
                uniquePersons.put(new Long(list.get(i).getPerson().getId()), list.get(i).getPerson());
            }
        }
        PersonBO[] members = new PersonBO[list.size()];
        for (int i = 0; i < members.length; i++) {
            members[i] = new PersonBO(list.get(i).getPerson());
        }
        this.endTransaction();
        return members;
    }

    @SuppressWarnings("unchecked")
    private List<MembershipEntity> findActiveMembersOnDate(EntityManager em, Date date) {
        Query query = em.createNamedQuery("findActiveMembersOnDate");
        query.setParameter("date", date);
        List<MembershipEntity> list = query.getResultList();
        return list;
    }

    @SuppressWarnings("unchecked")
    public EventBO[] getEvents(String countryCode) {
        this.startTransaction();
        if (nextIcalUpdate.before(new Date())) {
            nextIcalUpdate.setTime((new Date()).getTime() + (4 * 60 * 60 * 1000));
            updateIcals(em);
        }
        Query query = null;
        if (countryCode == null || countryCode.equals("")) {
            query = em.createNamedQuery("AllNonDublicateEvents");
        } else {
            query = em.createNamedQuery("EventsForCountry");
            query.setParameter("countryCode", countryCode.toUpperCase(Locale.ENGLISH));
        }
        List<EventEntity> list = query.getResultList();
        EventBO[] events = new EventBO[list.size()];
        for (int i = 0; i < events.length; i++) {
            events[i] = new EventBO(list.get(i));
            if (events[i].getEntity().getCoordinates() == null || "".equals(events[i].getEntity().getCoordinates())) {
                try {
                    events[i].updateCoordinnates();
                    em.persist(events[i].getEntity());
                } catch (IOException e) {
                    log.error("Error while updateing Coordinnates: " + e.toString(), e);
                }
            }
        }
        Arrays.sort(events, new EventComparator());
        this.endTransaction();
        return events;
    }

    public NewsItemBO[] getNews(String countryCode) {
        return getNews(countryCode, null);
    }

    @SuppressWarnings("unchecked")
    public NewsItemBO[] getNews(String countryCode, Integer numberOfDaysAgo) {
        this.startTransaction();
        Query query = em.createNamedQuery("findCurrentNews");
        if (numberOfDaysAgo == null) {
            query.setParameter("date", NewsItemBO.getOldestNewsDate());
        } else {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.HOUR, numberOfDaysAgo * -24);
            query.setParameter("date", cal.getTime());
        }
        if (countryCode == null || countryCode.equals("") || countryCode.equals("null")) {
            query.setParameter("countryCode", "%%");
        } else {
            query.setParameter("countryCode", countryCode.toUpperCase(Locale.ENGLISH));
        }
        List<NewsItemEntity> list = query.getResultList();
        NewsItemBO[] news = new NewsItemBO[list.size()];
        for (int i = 0; i < news.length; i++) {
            news[i] = new NewsItemBO(list.get(i));
        }
        Arrays.sort(news, new NewsComparator());
        this.endTransaction();
        return news;
    }

    @SuppressWarnings("unchecked")
    public BoatForSaleBO[] getUsersBoatsForSale() {
        this.startTransaction();
        Query query = null;
        query = em.createNamedQuery("findPersonsBoatsForSale");
        query.setParameter("person", Utils.getWebUser().getPerson().getEntity());
        List<BoatForSaleEntity> list = query.getResultList();
        BoatForSaleBO[] boatsForSale = new BoatForSaleBO[list.size()];
        for (int i = 0; i < boatsForSale.length; i++) {
            boatsForSale[i] = new BoatForSaleBO(list.get(i));
        }
        this.endTransaction();
        return boatsForSale;
    }

    @SuppressWarnings("unchecked")
    public BoatForSaleBO[] getBoatsForSale(String countryCode) {
        this.startTransaction();
        Query query = null;
        query = em.createNamedQuery("findBoatsForSale");
        query.setParameter("date", BoatForSaleBO.getOldestBoatForSaleDate());
        if (countryCode == null || countryCode.equals("") || countryCode.equals("null")) {
            query.setParameter("countryCode", "%%");
        } else {
            query.setParameter("countryCode", countryCode.toUpperCase(Locale.ENGLISH));
        }
        List<BoatForSaleEntity> list = query.getResultList();
        BoatForSaleBO[] boatsForSale = new BoatForSaleBO[list.size()];
        for (int i = 0; i < boatsForSale.length; i++) {
            boatsForSale[i] = new BoatForSaleBO(list.get(i));
        }
        this.endTransaction();
        return boatsForSale;
    }

    @SuppressWarnings("unchecked")
    public DesignBO[] getBoatDesgins() {
        this.startTransaction();
        Query query = null;
        query = em.createNamedQuery("allDesgins");
        List<DesignEntity> list = query.getResultList();
        DesignBO[] designs = new DesignBO[list.size()];
        for (int i = 0; i < designs.length; i++) {
            designs[i] = new DesignBO(list.get(i));
        }
        this.endTransaction();
        return designs;
    }

    @SuppressWarnings("unchecked")
    public EquipmentSupplierBO[] getBoatBuilders() {
        this.startTransaction();
        Query query = null;
        query = em.createNamedQuery("allEquipmentSuppliers");
        List<EquipmentSupplierEntity> list = query.getResultList();
        EquipmentSupplierBO[] builders = new EquipmentSupplierBO[list.size()];
        for (int i = 0; i < builders.length; i++) {
            builders[i] = new EquipmentSupplierBO(list.get(i));
        }
        this.endTransaction();
        return builders;
    }

    public List<NewsFeedBO> getNewsFeeds() {
        return getNewsFeeds(null);
    }

    public List<NewsFeedBO> getNewsFeeds(Integer numberOfDays) {
        if (updateNewsFeedList()) {
            doUpdateOfNewsFeedList();
        }
        if (newsFeeds != null) {
            if (numberOfDays == null) {
                return newsFeeds;
            }
            NewsFeedBO[] ary = newsFeeds.toArray(new NewsFeedBO[0]);
            ArrayList<NewsFeedBO> filteredList = new ArrayList<NewsFeedBO>();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.HOUR, numberOfDays * -24);
            Date filterDate = cal.getTime();
            for (int i = 0; i < ary.length; i++) {
                if (ary[i].getDate().after(filterDate)) {
                    filteredList.add(ary[i]);
                } else {
                    break;
                }
            }
            return filteredList;
        } else {
            return null;
        }
    }

    private void doUpdateOfNewsFeedList() {
        if (UiService.updateingBlogs) {
            return;
        }
        try {
            UiService.updateingBlogs = true;
            log.info("Updateing News Feed List.");
            NewsFeedEntity[] feeds = getNewsFeedEntities();
            FeedFetcherCache feedInfoCache = HashMapFeedInfoCache.getInstance();
            HttpURLFeedFetcher feedFetcher = new HttpURLFeedFetcher(feedInfoCache);
            for (int i = 0; i < feeds.length; i++) {
                String url = feeds[i].getUrl();
                if (url.length() > 0) {
                    try {
                        URL inputUrl = new URL(url);
                        SyndFeed inFeed = feedFetcher.retrieveFeed(inputUrl);
                        List<SyndEntry> list = inFeed.getEntries();
                        for (int j = 0; j < list.size(); j++) {
                            SyndEntry entry = list.get(j);
                            NewsFeedBO feedBO = new NewsFeedBO();
                            feedBO.setSource(feeds[i].getTitle());
                            feedBO.setTitle(entry.getTitle());
                            feedBO.setUrl(entry.getLink());
                            feedBO.setDate(entry.getPublishedDate());
                            if (feedBO.getDate().after(NewsFeedBO.getOldestNewFeedDate()) && !newsFeeds.contains(feedBO) && doesNotContainEntry(newsFeeds, feedBO)) {
                                newsFeeds.add(feedBO);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Error getting News Feed: " + url + " ERROR: " + e.toString());
                    }
                }
            }
            BoatForSaleBO[] boats4Sale = getBoatsForSale("");
            for (int i = 0; i < boats4Sale.length; i++) {
                NewsFeedBO feedBO = new NewsFeedBO();
                feedBO.setSource(boats4Sale[i].getSeller().getName());
                feedBO.setTitle("For Sale: " + boats4Sale[i].getBoat().getTitle());
                feedBO.setUrl(boats4Sale[i].getAbsoluteUrl());
                feedBO.setDate(boats4Sale[i].getEntity().getCreated());
                if (feedBO.getDate() != null && feedBO.getDate().after(NewsFeedBO.getOldestNewFeedDate()) && !newsFeeds.contains(feedBO) && doesNotContainEntry(newsFeeds, feedBO)) {
                    newsFeeds.add(feedBO);
                }
            }
            for (int j = 0; j < newsFeeds.size(); j++) {
                NewsFeedBO entry = newsFeeds.get(j);
                if (entry.getDate().before(NewsFeedBO.getOldestNewFeedDate())) {
                    newsFeeds.remove(entry);
                }
            }
            NewsFeedBO[] ary = newsFeeds.toArray(new NewsFeedBO[0]);
            Arrays.sort(ary, new NewsFeedBOComparator());
            ArrayList<NewsFeedBO> filteredList = new ArrayList<NewsFeedBO>();
            for (int i = 0; i < ary.length; i++) {
                filteredList.add(ary[i]);
            }
            newsFeeds = filteredList;
            log.info("Updated News Feeds List  with " + ary.length + " entries.");
        } catch (Exception ex) {
            UiService.updateingBlogs = false;
            log.error("Error getting Blogs.", ex);
        } finally {
            UiService.updateingBlogs = false;
        }
        nextNewsFeedUpdate.setTime((new Date()).getTime() + (60 * 60 * 1000));
        UiService.updateingBlogs = false;
    }

    public List<SyndEntry> getBlogs(String countryCode) {
        return getBlogs(countryCode, null);
    }

    @SuppressWarnings("unchecked")
    public List<SyndEntry> getBlogs(String countryCode, Integer numberOfDays) {
        if (countryCode == null) {
            countryCode = "";
        }
        if (updateBlogList(countryCode)) {
            doUpdateOfBlogList(countryCode);
        }
        if (blogTable.get(countryCode) != null && blogTable.get(countryCode).blogList != null) {
            if (numberOfDays == null) {
                return blogTable.get(countryCode).blogList;
            }
            List<SyndEntry> list = blogTable.get(countryCode).blogList;
            SyndEntry[] ary = list.toArray(new SyndEntry[0]);
            ArrayList<SyndEntry> filteredList = new ArrayList<SyndEntry>();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.HOUR, numberOfDays * -24);
            Date filterDate = cal.getTime();
            for (int i = 0; i < ary.length; i++) {
                if (ary[i].getPublishedDate().after(filterDate)) {
                    filteredList.add(ary[i]);
                } else {
                    break;
                }
            }
            return filteredList;
        } else {
            return null;
        }
    }

    protected void doUpdateOfBlogList(String countryCode) {
        if (UiService.updateingBlogs) {
            return;
        }
        try {
            UiService.updateingBlogs = true;
            log.info("Updateing Blog List for countryCode: " + countryCode);
            List<SyndEntry> entries;
            if (blogTable.get(countryCode) != null) {
                entries = (blogTable.get(countryCode)).blogList;
            } else {
                entries = new ArrayList<SyndEntry>();
            }
            PersonBO[] persons = getBloggers(countryCode);
            FeedFetcherCache feedInfoCache = HashMapFeedInfoCache.getInstance();
            HttpURLFeedFetcher feedFetcher = new HttpURLFeedFetcher(feedInfoCache);
            for (int i = 0; i < persons.length; i++) {
                String url = persons[i].getEntity().getNewsFeedUrl();
                if (url.length() > 0) {
                    try {
                        URL inputUrl = new URL(url);
                        SyndFeed inFeed = feedFetcher.retrieveFeed(inputUrl);
                        List<SyndEntry> list = inFeed.getEntries();
                        for (int j = 0; j < list.size(); j++) {
                            SyndEntry entry = list.get(j);
                            if (entry.getPublishedDate().after(NewsItemBO.getOldestBlogDate()) && !entries.contains(entry) && doesNotContainEntry(entries, entry)) {
                                entries.add(entry);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Error getting Blog: " + url + ". Error: " + e.toString());
                    }
                }
            }
            for (int j = 0; j < entries.size(); j++) {
                SyndEntry entry = entries.get(j);
                if (entry.getPublishedDate().before(NewsItemBO.getOldestBlogDate())) {
                    entries.remove(entry);
                }
            }
            SyndEntry[] ary = entries.toArray(new SyndEntry[0]);
            Arrays.sort(ary, new NewsFeedComparator());
            ArrayList<SyndEntry> filteredList = new ArrayList<SyndEntry>();
            for (int i = 0; i < ary.length; i++) {
                filteredList.add(ary[i]);
            }
            BlogTableEntry entry = new BlogTableEntry(Calendar.getInstance(), filteredList);
            blogTable.put(countryCode, entry);
            log.info("Updated Blog List for countryCode: " + countryCode + " with " + ary.length + " entries.");
        } catch (Exception ex) {
            UiService.updateingBlogs = false;
            log.error("Error getting Blogs.", ex);
        } finally {
            UiService.updateingBlogs = false;
        }
        UiService.updateingBlogs = false;
    }

    private boolean doesNotContainEntry(List<NewsFeedBO> entries, NewsFeedBO entry) {
        for (Iterator iterator = entries.iterator(); iterator.hasNext(); ) {
            NewsFeedBO syndEntry = (NewsFeedBO) iterator.next();
            if (syndEntry.getTitle().equals(entry.getTitle()) && syndEntry.getSource().equals(entry.getSource()) && syndEntry.getDate().equals(entry.getDate())) {
                return false;
            }
        }
        return true;
    }

    private boolean doesNotContainEntry(List<SyndEntry> entries, SyndEntry entry) {
        for (Iterator iterator = entries.iterator(); iterator.hasNext(); ) {
            SyndEntry syndEntry = (SyndEntry) iterator.next();
            if (syndEntry.getTitle().equals(entry.getTitle()) && syndEntry.getAuthor().equals(entry.getAuthor()) && syndEntry.getPublishedDate().equals(entry.getPublishedDate())) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private PersonBO[] getBloggers(String countryCode) {
        this.startTransaction();
        Query query = null;
        query = em.createNamedQuery("getBloggers");
        if (countryCode == null || countryCode.equals("") || countryCode.equals("null")) {
            query.setParameter("countryCode", "%%");
        } else {
            query.setParameter("countryCode", countryCode.toUpperCase(Locale.ENGLISH));
        }
        List<PersonEntity> list = query.getResultList();
        PersonBO[] persons = new PersonBO[list.size()];
        for (int i = 0; i < persons.length; i++) {
            persons[i] = new PersonBO(list.get(i));
        }
        this.endTransaction();
        return persons;
    }

    private boolean updateNewsFeedList() {
        if (updateingBlogs) {
            return false;
        }
        if (nextNewsFeedUpdate.before(new Date())) {
            return true;
        }
        return false;
    }

    private boolean updateBlogList(String countryCode) {
        if (updateingBlogs) {
            return false;
        }
        if (blogTable.get(countryCode) == null) {
            return true;
        }
        Calendar testDate = Calendar.getInstance();
        testDate.add(Calendar.HOUR, -1);
        if (testDate.after(blogTable.get(countryCode).lastBlogListUpdate)) {
            return true;
        }
        return false;
    }

    public AdBannerBO getNextBanner() {
        AdBannerBO[] ads = getActiveBanners();
        if (ads.length == 0) {
            return new AdBannerBO(null);
        } else if (ads.length == 1) {
            return ads[0];
        } else {
            return ads[RandomUtils.nextInt(ads.length)];
        }
    }

    @SuppressWarnings("unchecked")
    public AdBannerBO[] getActiveBanners() {
        this.startTransaction();
        Query query = null;
        query = em.createNamedQuery("GetActiveAdverts");
        query.setParameter("date", new Date());
        List<AdBannerEntity> list = query.getResultList();
        this.endTransaction();
        AdBannerBO[] ads = new AdBannerBO[list.size()];
        for (int i = 0; i < ads.length; i++) {
            ads[i] = new AdBannerBO(list.get(i));
        }
        return ads;
    }

    public BoatBO[] getAllBoatData() {
        startTransaction();
        Query query = em.createNamedQuery("BoatBasicSearch");
        query.setParameter("search", "%%");
        List<BoatEntity> list = query.getResultList();
        BoatBO[] boats = new BoatBO[list.size()];
        for (int i = 0; i < boats.length; i++) {
            boats[i] = new BoatBO(list.get(i));
            boats[i].getEventEntries(em);
        }
        this.endTransaction();
        return boats;
    }

    private synchronized void updateIcals(EntityManager em) {
        Query query = em.createNamedQuery("EventICals");
        List<AssociationEntity> list = query.getResultList();
        CalendarBuilder builder = new CalendarBuilder();
        String icalURL = "";
        for (int i = 0; i < list.size(); i++) {
            try {
                AssociationEntity entity = list.get(i);
                icalURL = entity.getIcalURL();
                URL url = new URL(icalURL);
                URLConnection con = url.openConnection();
                net.fortuna.ical4j.model.Calendar calendar = builder.build(con.getInputStream());
                List events = calendar.getComponents(Component.VEVENT);
                for (int j = 0; j < events.size(); j++) {
                    EventBO event = new EventBO((VEvent) events.get(j), em, entity.getIsoCountryCode());
                    event.updateCoordinnates();
                    em.persist(event.getEntity());
                    log.info("Updated event from ICal: " + event.getTitle());
                }
            } catch (Exception e) {
                log.error("Error updateIcals: " + e.getMessage() + " iCal URL: " + icalURL, e);
            }
        }
    }

    public PageBO getPage(String parameterId) {
        startTransaction();
        Query query = em.createNamedQuery("GetPage");
        query.setParameter("identifier", parameterId);
        List<PageEntity> list = query.getResultList();
        PageEntity entity;
        if (list.size() > 0) {
            entity = list.get(0);
        } else {
            entity = new PageEntity();
            entity.setTitle("Page not found!");
            entity.setBody("");
        }
        this.endTransaction();
        return new PageBO(entity);
    }

    public List<BoatEntity> getBoatsFromYear(String searchYear) {
        try {
            Integer year = Integer.parseInt(searchYear);
            startTransaction();
            Query query = em.createNamedQuery("GetBoatsFromYear");
            query.setParameter("year", year);
            List<BoatEntity> list = query.getResultList();
            this.endTransaction();
            return list;
        } catch (Exception ex) {
            return null;
        }
    }
}

class BlogTableEntry {

    Calendar lastBlogListUpdate = Calendar.getInstance();

    List<SyndEntry> blogList = null;

    BlogTableEntry(Calendar lastBlogListUpdate, List<SyndEntry> blogList) {
        this.lastBlogListUpdate = lastBlogListUpdate;
        this.blogList = blogList;
    }
}
