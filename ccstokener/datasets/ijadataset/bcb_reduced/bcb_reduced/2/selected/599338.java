package uk.me.g4dpz.gae.satellitecloud.server.implementation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import org.apache.commons.lang.StringUtils;
import uk.me.g4dpz.gae.satellitecloud.persistence.SatelliteElementSet;
import uk.me.g4dpz.satellite.TLE;
import com.google.appengine.api.mail.MailService;
import com.google.appengine.api.mail.MailServiceFactory;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.MemcacheService;

public class KepsManagerShared {

    private static final String AS_SITE_ID_FOR_DOWLOADING_KEPS = " as site id for dowloading KEPS";

    private static final String BADGERJOHNSON_GMAIL_COM = "badgerjohnson@googlemail.com";

    private static final Logger LOGGER = Logger.getLogger(KepsManagerShared.class.getName());

    private static final String NEW_LINE = "\n";

    private static final int MAX_CACHE_SIZE = 100;

    private MemcacheService memcacheService = null;

    private static SimpleTimeZone TZ = new SimpleTimeZone(0, "UTC");

    public KepsManagerShared() {
    }

    public Boolean loadKeps(final String id, final String fileName) {
        String msgBody = "";
        String kepsFileName = (null != fileName) ? fileName : "amateur.txt";
        boolean success = false;
        MailService.Message mess = new MailService.Message();
        SimpleTimeZone zone = new SimpleTimeZone(0, "UTC");
        int satelliteCount = 0;
        List<SatelliteElementSet> elementSets;
        EntityManager em = null;
        EntityTransaction tx = null;
        try {
            String url = "";
            msgBody = "Using: " + id + "/" + kepsFileName + AS_SITE_ID_FOR_DOWLOADING_KEPS + NEW_LINE;
            if (id.equals(KeplerSource.celestrak.name())) {
                url = "http://www.celestrak.com/NORAD/elements/" + kepsFileName;
            } else if (id.equals(KeplerSource.localhost.name())) {
                url = "http://localhost/~badger/" + kepsFileName;
            } else if (id.equals(KeplerSource.spacelink.name())) {
            } else {
                throw new IllegalArgumentException("Tried to use: " + id + AS_SITE_ID_FOR_DOWLOADING_KEPS);
            }
            if (null == memcacheService) {
                memcacheService = MemcacheServiceFactory.getMemcacheService();
            }
            elementSets = parseTLE(url);
            msgBody += "Elements read: " + elementSets.size() + NEW_LINE;
            em = CoreManager.getEmf().createEntityManager();
            if (null == em) {
                throw new NullPointerException("Could not create EntityManager");
            }
            for (SatelliteElementSet satSet : elementSets) {
                tx = em.getTransaction();
                if (null == tx) {
                    throw new NullPointerException("Could not create Transaction");
                }
                tx.begin();
                SatelliteElementSet cachedSatSet = (SatelliteElementSet) memcacheService.get(satSet.getCatalogNumber());
                if (null == cachedSatSet) {
                    msgBody += ("Satellite: " + satSet.getName() + " was not in cache" + NEW_LINE);
                    SatelliteElementSet satSetDb = em.find(SatelliteElementSet.class, Long.valueOf(satSet.getCatalogNumber()));
                    if (null == satSetDb) {
                        msgBody += ("Satellite: " + satSet.getName() + " was not in database" + NEW_LINE);
                        em.persist(satSet);
                        em.flush();
                        msgBody += ("Added satellite: " + satSet.getName() + NEW_LINE);
                    } else {
                        msgBody += ("Satellite: " + satSet.getName() + " was in database" + NEW_LINE);
                        msgBody += ("Comparing satSet for: " + satSet.getName() + " with tle on SetNum: " + satSetDb.getSetNumber() + ", " + satSet.getSetNumber() + NEW_LINE);
                        if (satSet.getSetNumber().longValue() > satSetDb.getSetNumber().longValue()) {
                            satSetDb.update(satSet, Calendar.getInstance(zone).getTime());
                            em.merge(satSetDb);
                            em.flush();
                            msgBody += ("Updated satellite: " + satSet.getName() + NEW_LINE);
                        }
                    }
                    memcacheService.put(satSet.getCatalogNumber(), satSet);
                } else {
                    msgBody += ("Satellite: " + satSet.getName() + " was in cache" + NEW_LINE);
                    msgBody += ("Comparing satSet for: " + satSet.getName() + " with tle on SetNum: " + cachedSatSet.getSetNumber() + ", " + satSet.getSetNumber() + NEW_LINE);
                    if (satSet.getSetNumber().longValue() > cachedSatSet.getSetNumber().longValue()) {
                        cachedSatSet.update(satSet, Calendar.getInstance(zone).getTime());
                        em.merge(cachedSatSet);
                        em.flush();
                        msgBody += ("Updated satellite: " + satSet.getName() + NEW_LINE);
                        memcacheService.put(satSet.getCatalogNumber(), satSet);
                    }
                }
                tx.commit();
            }
            success = true;
        } catch (IOException e) {
            msgBody = reportError(e, msgBody);
        } catch (NullPointerException e) {
            msgBody = reportError(e, msgBody);
        } finally {
            if (null != tx && tx.isActive()) {
                tx.rollback();
            }
            if (null != em) {
                em.close();
            }
        }
        MailService mailServ = MailServiceFactory.getMailService();
        mess.setSender(BADGERJOHNSON_GMAIL_COM);
        mess.setTo("david.johnson@blackpepper.co.uk");
        mess.setSubject("loadKeps called");
        mess.setTextBody(msgBody);
        try {
            mailServ.send(mess);
        } catch (IOException e) {
            LOGGER.severe(e.getMessage());
        }
        return Boolean.valueOf(success);
    }

    private String reportError(final Exception e, final String msgBody) {
        StringBuilder sb = new StringBuilder(msgBody);
        sb.append(e.getMessage());
        LOGGER.setLevel(Level.SEVERE);
        LOGGER.severe(msgBody);
        return sb.toString();
    }

    /**
     * Parses the TLE String.
     * 
     * @param tleString
     *            The TLE string
	 * @param msgBody 
     * @return the map of name to TLE
     * @throws IOException
     *             problem processing the file
     */
    public List<SatelliteElementSet> parseTLE(String urlString) throws IOException {
        List<SatelliteElementSet> elementSets = new ArrayList<SatelliteElementSet>();
        BufferedReader reader = null;
        try {
            String line = null;
            int i = 0;
            URL url = new URL(urlString);
            String[] lines = new String[3];
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            while ((line = reader.readLine()) != null) {
                i++;
                switch(i) {
                    case 1:
                        {
                            lines[0] = line;
                            break;
                        }
                    case 2:
                        {
                            lines[1] = line;
                            break;
                        }
                    case 3:
                        {
                            lines[2] = line;
                            Long catnum = Long.parseLong(StringUtils.strip(lines[1].substring(2, 7)));
                            long setnum = Long.parseLong(StringUtils.strip(lines[1].substring(64, 68)));
                            elementSets.add(new SatelliteElementSet(catnum, lines, setnum, Calendar.getInstance(TZ).getTime()));
                            i = 0;
                            break;
                        }
                    default:
                        {
                            throw new IOException("TLE string did not contain three elements");
                        }
                }
            }
        } finally {
            if (null != reader) {
                reader.close();
            }
        }
        return elementSets;
    }

    public TLE getTLE(Long catNum) {
        TLE tle = null;
        EntityTransaction tx = null;
        EntityManager em = null;
        try {
            em = CoreManager.getEmf().createEntityManager();
            if (null == em) {
                throw new NullPointerException("Could not create EntityManager");
            }
            tx = em.getTransaction();
            if (null == tx) {
                throw new NullPointerException("Could not create Transaction");
            }
            tx = em.getTransaction();
            if (null == tx) {
                throw new NullPointerException("Could not create Transaction");
            }
            SatelliteElementSet satSet = (SatelliteElementSet) memcacheService.get(catNum);
            if (null == satSet) {
                tx.begin();
                satSet = em.find(SatelliteElementSet.class, catNum);
                if (null == satSet) {
                    throw new NullPointerException("TLE was not found in cache or database");
                } else {
                    memcacheService.put(catNum, satSet);
                }
                tx.commit();
            }
            String[] lines = new String[3];
            lines[0] = satSet.getName();
            lines[1] = satSet.getLine1();
            lines[2] = satSet.getLine2();
            tle = new TLE(lines);
        } catch (NullPointerException e) {
            LOGGER.severe(e.getMessage());
        } finally {
            if (null != tx && tx.isActive()) {
                tx.rollback();
            }
            if (null != em) {
                em.close();
            }
        }
        return tle;
    }
}
