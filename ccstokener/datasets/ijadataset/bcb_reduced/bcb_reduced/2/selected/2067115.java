package ru.adv.web.app.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import javax.servlet.http.HttpServletRequest;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.DateUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import ru.adv.http.Environment;
import ru.adv.logger.TLogger;
import ru.adv.mozart.framework.EnvironmentCalculator;
import com.maxmind.geoip.Country;
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import com.maxmind.geoip.regionName;

/**
 * 
 * Calculates remote client location by IP
 *
 *  @see http://www.maxmind.com/app/ip-location
 *  @author vic
 *
 */
public class GeoIpEnvironmentCalculator implements EnvironmentCalculator, InitializingBean, DestroyableService {

    private TLogger LOGGER = new TLogger(GeoIpEnvironmentCalculator.class);

    private static final String GEOIP_CITY = "GEOIP_CITY";

    private static final String GEOIP_REGION = "GEOIP_REGION";

    private static final String GEOIP_COUNTRY_NAME = "GEOIP_COUNTRY_NAME";

    private static final String GEOIP_COUNTRY_CODE = "GEOIP_COUNTRY_CODE";

    private static final String DB_CITY_DOWNLOAD_URL = "http://www.maxmind.com/download/geoip/database/GeoLiteCity.dat.gz";

    private static final String DB_COUTRY_DOWNLOAD_URL = "http://www.maxmind.com/download/geoip/database/GeoLiteCountry/GeoIP.dat.gz";

    private ScheduledExecutorService scheduler;

    private String countryDataFileName = "GeoIP.dat";

    private String cityDataFileName = "GeoLiteCity.dat";

    private String countryDataFileUpdateURL = DB_COUTRY_DOWNLOAD_URL;

    private String cityDataFileUpdateURL = DB_CITY_DOWNLOAD_URL;

    private int checkForUpdatesInHours = 48;

    private String databaseDirectory;

    private LookupService countryLookup;

    private LookupService cityLookup;

    public GeoIpEnvironmentCalculator() {
        super();
        startSchedulerToLoadUpdates();
    }

    public void startSchedulerToLoadUpdates() {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleWithFixedDelay(new Runnable() {

                public void run() {
                    GeoIpEnvironmentCalculator.this.checkForUpdates();
                }
            }, getCheckForUpdatesInHours(), getCheckForUpdatesInHours(), TimeUnit.HOURS);
        }
    }

    public void stopScheduleThreadToLoadUpdates() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    /** 
	 * writable directory to store GeoIp files 
	 * and download then in it
	 * @return
	 */
    public String getDatabaseDirectory() {
        return databaseDirectory;
    }

    public void setDatabaseDirectory(String databaseDirectory) {
        this.databaseDirectory = databaseDirectory;
    }

    public String getCityDataFileName() {
        return cityDataFileName;
    }

    public void setCityDataFileName(String cityDataFileName) {
        this.cityDataFileName = cityDataFileName;
    }

    public String getCountryDataFileName() {
        return countryDataFileName;
    }

    public void setCountryDataFileName(String countryDataFileName) {
        this.countryDataFileName = countryDataFileName;
    }

    /**
	 * Check for new database updates in hours
	 * @return
	 */
    public int getCheckForUpdatesInHours() {
        return checkForUpdatesInHours;
    }

    public void setCheckForUpdatesInHours(int checkForUpdatesInHours) {
        this.checkForUpdatesInHours = checkForUpdatesInHours;
        if (this.scheduler != null) {
            stopScheduleThreadToLoadUpdates();
            startSchedulerToLoadUpdates();
        }
    }

    public String getCountryDataFileUpdateURL() {
        return countryDataFileUpdateURL;
    }

    public void setCountryDataFileUpdateURL(String countryDataFileUpdateURL) {
        this.countryDataFileUpdateURL = countryDataFileUpdateURL;
    }

    public String getCityDataFileUpdateURL() {
        return cityDataFileUpdateURL;
    }

    public void setCityDataFileUpdateURL(String cityDataFileUpdateURL) {
        this.cityDataFileUpdateURL = cityDataFileUpdateURL;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    public String getCountryDbFilePath() {
        return getDatabaseDirectory() + "/" + getCountryDataFileName();
    }

    public String getCityDbFilePath() {
        return getDatabaseDirectory() + "/" + getCityDataFileName();
    }

    public void init() throws Exception {
        Assert.hasText(getDatabaseDirectory(), "Database directory is not set");
        File dir = new File(getDatabaseDirectory());
        Assert.isTrue(dir.isDirectory(), "Database directory is wrong");
        Assert.isTrue(dir.canWrite(), "Database directory is not writable");
        Assert.hasText(getCountryDataFileName(), "Property countryDataFileName is empty");
        Assert.isTrue(new File(getCountryDbFilePath()).exists(), "File " + getCountryDbFilePath() + " not exists!");
        Assert.hasText(getCityDataFileName(), "Property cityDataFile is empty");
        Assert.isTrue(new File(getCityDbFilePath()).exists(), "File " + getCityDbFilePath() + " not exists!");
        initCountryLookupService();
        initCityLookupService();
    }

    private LookupService createLookupService(String filePath) throws IOException {
        return new LookupService(filePath, LookupService.GEOIP_INDEX_CACHE);
    }

    public synchronized void destroy() {
        LOGGER.info("do destroy");
        stopScheduleThreadToLoadUpdates();
        destroyCityLookupService();
        destroyCountryLookupService();
    }

    /**
	 * 
	 * test URL for new files
	 * 
	 */
    public void checkForUpdates() {
        if (needToDownload(getCountryDataFileUpdateURL(), getCountryDbFilePath())) {
            try {
                File tmpFile = loadFile(getCountryDataFileUpdateURL(), new File(getDatabaseDirectory()));
                synchronized (this) {
                    destroyCountryLookupService();
                    new File(getCountryDbFilePath()).delete();
                    tmpFile.renameTo(new File(getCountryDbFilePath()));
                    initCountryLookupService();
                }
            } catch (Throwable t) {
                LOGGER.error("Can't update file " + getCountryDataFileName());
            }
        }
        if (needToDownload(getCityDataFileUpdateURL(), getCityDbFilePath())) {
            try {
                File tmpFile = loadFile(getCityDataFileUpdateURL(), new File(getDatabaseDirectory()));
                synchronized (this) {
                    destroyCityLookupService();
                    new File(getCityDbFilePath()).delete();
                    tmpFile.renameTo(new File(getCityDbFilePath()));
                    initCityLookupService();
                }
            } catch (Throwable t) {
                LOGGER.error("Can't update file " + getCityDataFileName());
            }
        }
    }

    private void initCityLookupService() throws IOException {
        this.cityLookup = createLookupService(getCityDbFilePath());
    }

    private void initCountryLookupService() throws IOException {
        this.countryLookup = createLookupService(getCountryDbFilePath());
    }

    private synchronized void destroyCityLookupService() {
        if (this.cityLookup != null) {
            LOGGER.info("destroy cityLookupService");
            this.cityLookup.close();
            this.cityLookup = null;
        }
    }

    private synchronized void destroyCountryLookupService() {
        if (this.countryLookup != null) {
            LOGGER.info("destroy countryLookupService");
            this.countryLookup.close();
            this.countryLookup = null;
        }
    }

    /** 
	 * Load file form URL
	 * @param url
	 * @param downloadDir
	 * @return tmp file with downloaded data
	 * @throws Exception
	 */
    private synchronized File loadFile(String url, File downloadDir) throws Exception {
        File tmpFile = File.createTempFile(StringUtils.getFilename(url), null, downloadDir);
        LOGGER.info(String.format("Load file %1$s into %2$s", url, tmpFile));
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpRequest = new HttpGet(url);
        try {
            HttpResponse httResponse = httpClient.execute(httpRequest);
            final int statusCode = httResponse.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                FileCopyUtils.copy(new GZIPInputStream(httResponse.getEntity().getContent()), new FileOutputStream(tmpFile));
                return tmpFile;
            } else {
                LOGGER.warning("Bad response code =" + statusCode);
                throw new Exception("Can't load " + url + ". Result status code = " + statusCode);
            }
        } catch (Exception e) {
            LOGGER.error(e);
            throw e;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    private boolean needToDownload(String url, String localFilePath) {
        File f = new File(localFilePath);
        if (!f.exists()) {
            return true;
        }
        Date fileLastModifed = new Date(f.lastModified());
        LOGGER.info("File modified at " + fileLastModifed);
        LOGGER.info("Check URL " + url);
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpRequest = new HttpGet(url);
        try {
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            final int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                Header h = httpResponse.getFirstHeader("Last-Modified");
                Date urlLastModifed = DateUtils.parseDate(h.getValue());
                LOGGER.info("urlLastModifed : " + urlLastModifed);
                if (urlLastModifed.after(fileLastModifed)) {
                    return true;
                }
            } else {
                LOGGER.warning("Bad response code =" + statusCode);
            }
        } catch (Throwable e) {
            LOGGER.error(e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        return false;
    }

    /**
	 * Set remote user locations by IP 
	 * 
	 */
    @Override
    public synchronized void calculateAndSetProperties(Environment environment, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (StringUtils.hasText(ip)) {
            if (this.countryLookup != null) {
                try {
                    Country country = countryLookup.getCountry(ip);
                    if (country != null) {
                        environment.put(GEOIP_COUNTRY_CODE, country.getCode());
                        environment.put(GEOIP_COUNTRY_NAME, country.getName());
                    }
                } catch (Throwable t) {
                    LOGGER.logErrorStackTrace("Error on find a country", t);
                    destroyCountryLookupService();
                    backupDeleteFile(getCountryDbFilePath());
                }
            }
            if (this.cityLookup != null) {
                try {
                    Location loc = cityLookup.getLocation(ip);
                    if ((loc != null)) {
                        environment.put(GEOIP_REGION, regionName.regionNameByCode(loc.countryCode, loc.region));
                        environment.put(GEOIP_CITY, loc.city);
                    }
                } catch (Throwable t) {
                    LOGGER.logErrorStackTrace("Error on find a region and city", t);
                    destroyCityLookupService();
                    backupDeleteFile(getCityDbFilePath());
                }
            }
        }
    }

    private void backupDeleteFile(String dbFilePath) {
        if (new File(dbFilePath).exists()) {
            File backupFile = new File(String.format(dbFilePath + ".%1$tYmdHMS", Calendar.getInstance()));
            LOGGER.info("Backup previous file to " + backupFile);
            new File(dbFilePath).renameTo(backupFile);
        }
    }
}
