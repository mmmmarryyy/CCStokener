package de.wadndadn.dailydilbert.data;

import static de.wadndadn.base.net.HttpClientFactory.createHttpClient;
import static de.wadndadn.dailydilbert.DailyDilbertPlugin.STATUS_CODE_ERROR_CONTENT_TYPE;
import static de.wadndadn.dailydilbert.DailyDilbertPlugin.STATUS_CODE_ERROR_DOWNLOAD;
import static de.wadndadn.dailydilbert.DailyDilbertPlugin.STATUS_CODE_ERROR_FEED_READING;
import static de.wadndadn.dailydilbert.DailyDilbertPlugin.STATUS_CODE_ERROR_HTTP_STATUS;
import static de.wadndadn.dailydilbert.DailyDilbertPlugin.STATUS_CODE_ERROR_IMAGE_URL;
import static de.wadndadn.dailydilbert.DailyDilbertPlugin.STATUS_CODE_ERROR_LINK;
import static de.wadndadn.dailydilbert.DailyDilbertPlugin.STATUS_CODE_ERROR_FEED_URL;
import static de.wadndadn.dailydilbert.DailyDilbertPlugin.getDefault;
import static de.wadndadn.dailydilbert.DailyDilbertPlugin.log;
import static de.wadndadn.dailydilbert.Messages.getMessages;
import static de.wadndadn.dailydilbert.Statuses.getStatuses;
import static de.wadndadn.dailydilbert.preference.DailyDilbertPreferenceConstants.PREFERENCE_ENABLE_AUTODOWNLOAD;
import static de.wadndadn.dailydilbert.preference.DailyDilbertPreferenceConstants.PREFERENCE_ENABLE_UPDATE;
import static org.apache.commons.lang.StringUtils.deleteWhitespace;
import static org.apache.commons.lang.StringUtils.split;
import static org.eclipse.core.runtime.IStatus.ERROR;
import static org.eclipse.core.runtime.IStatus.WARNING;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import de.wadndadn.dailydilbert.DailyDilbertPlugin;
import de.wadndadn.dailydilbert.errorhandling.FeedReaderException;

/**
 * TODO Document.
 * 
 * @author SchubertCh
 */
public final class FeedReaderUtil {

    /**
     * TODO Document.
     */
    private static final String EXPECTED_CONTENT_TYPE = "text/html";

    /**
     * TODO Document.
     */
    private static final Pattern DILBERT_IMAGE_URL_PATTERN = Pattern.compile("<imgsrc=\"(.*)border=\"0\"/>.*");

    /**
     * TODO Document.
     */
    private static final int OK_STATUS_CODE = 200;

    /**
     * TODO Document.
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * TODO Document.
     * 
     * @param subProgressMonitor
     *            TODO Document
     * @param ticks
     *            TODO Document
     * 
     * @return TODO Document
     * 
     * @throws FeedReaderException
     *             TODO Document
     */
    public static SyndFeed readFeed(final SubProgressMonitor subProgressMonitor, final int ticks) throws FeedReaderException {
        subProgressMonitor.beginTask(getMessages().bind("syndFeedRead.monitor.name", getDefault().getControl().getCurrentFeedDescription().getTitle(), getDefault().getControl().getCurrentFeedDescription().getLink()), ticks);
        subProgressMonitor.subTask(getMessages().bind("syndFeedRead.subtask.name", getDefault().getControl().getCurrentFeedDescription().getTitle(), getDefault().getControl().getCurrentFeedDescription().getLink()));
        HttpClient httpClient = null;
        try {
            URI feedUri = getDefault().getControl().getCurrentFeedDescription().getLink().toURI();
            httpClient = createHttpClient(feedUri);
            HttpGet feedMethod = new HttpGet(feedUri);
            HttpResponse response = httpClient.execute(feedMethod);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != OK_STATUS_CODE) {
                IStatus status = getStatuses().create(WARNING, STATUS_CODE_ERROR_HTTP_STATUS, null, statusCode, feedUri);
                throw new FeedReaderException(status);
            }
            BufferedInputStream is = null;
            try {
                is = new BufferedInputStream(response.getEntity().getContent());
                SyndFeedInput syndFeedInput = new SyndFeedInput();
                SyndFeed syndFeed = syndFeedInput.build(new XmlReader(is));
                return syndFeed;
            } catch (FeedException fe) {
                IStatus status = getStatuses().create(ERROR, STATUS_CODE_ERROR_FEED_READING, fe, feedUri);
                throw new FeedReaderException(status);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException ioe) {
            IStatus status = getStatuses().create(ERROR, STATUS_CODE_ERROR_FEED_READING, ioe, getDefault().getControl().getCurrentFeedDescription().getLink());
            throw new FeedReaderException(status);
        } catch (URISyntaxException use) {
            IStatus status = getStatuses().create(ERROR, STATUS_CODE_ERROR_FEED_URL, use, getDefault().getControl().getCurrentFeedDescription().getLink());
            throw new FeedReaderException(status);
        } finally {
            if (httpClient != null && httpClient.getConnectionManager() != null) {
                httpClient.getConnectionManager().shutdown();
                subProgressMonitor.worked(ticks);
                subProgressMonitor.done();
            }
        }
    }

    /**
     * TODO Document.
     * 
     * @param syndFeed
     *            TODO Document
     * @param subProgressMonitor
     *            TODO Document
     * @param ticks
     *            TODO Document
     * 
     * @return TODO Document
     */
    public static List<FeedEntry> parseFeed(final SyndFeed syndFeed, final SubProgressMonitor subProgressMonitor, final int ticks) {
        String subProgressMonitorName = getMessages().bind("syndFeedParse.monitor.name", getDefault().getControl().getCurrentFeedDescription().getTitle());
        try {
            List<FeedEntry> entries = null;
            if (syndFeed != null && syndFeed.getEntries() != null && syndFeed.getEntries().size() > 0) {
                int numberOfEntries = syndFeed.getEntries().size();
                subProgressMonitor.beginTask(subProgressMonitorName, numberOfEntries);
                entries = new ArrayList<FeedEntry>(numberOfEntries);
                for (Object syndEntryObject : syndFeed.getEntries()) {
                    SyndEntry syndEntry = (SyndEntry) syndEntryObject;
                    subProgressMonitor.subTask(getMessages().bind("syndFeedParse.subtask.name", syndEntry.getTitle()));
                    FeedEntry entry = parseEntry(syndEntry);
                    if (entry != null) {
                        entries.add(entry);
                    }
                    subProgressMonitor.worked(1);
                }
            } else {
                subProgressMonitor.beginTask(subProgressMonitorName, 1);
                entries = new ArrayList<FeedEntry>(0);
                subProgressMonitor.worked(1);
            }
            return entries;
        } finally {
            subProgressMonitor.done();
        }
    }

    /**
     * TODO Document.
     * 
     * @param imageUrl
     *            TODO Document
     * 
     * @return TODO Document Will be <code>null</code> if image data could not
     *         be downloaded.
     */
    public static byte[] downloadImageData(final URL imageUrl) {
        HttpClient downloadClient = null;
        try {
            URI imageUri = new URI(imageUrl.toExternalForm());
            downloadClient = createHttpClient(imageUri);
            HttpGet downloadMethod = new HttpGet(imageUri);
            HttpResponse response = downloadClient.execute(downloadMethod);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != OK_STATUS_CODE) {
                IStatus status = getStatuses().create(WARNING, STATUS_CODE_ERROR_HTTP_STATUS, null, statusCode, imageUrl);
                log(status);
                return null;
            }
            BufferedInputStream is = null;
            ByteArrayOutputStream os = null;
            try {
                is = new BufferedInputStream(response.getEntity().getContent());
                os = new ByteArrayOutputStream();
                byte[] buffer = new byte[BUFFER_SIZE];
                int rbytes = 0;
                while ((rbytes = is.read(buffer)) != -1) {
                    os.write(buffer, 0, rbytes);
                }
                return os.toByteArray();
            } finally {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            }
        } catch (IOException ioe) {
            IStatus status = getStatuses().create(WARNING, STATUS_CODE_ERROR_DOWNLOAD, ioe, imageUrl);
            log(status);
        } catch (URISyntaxException use) {
            IStatus status = getStatuses().create(WARNING, STATUS_CODE_ERROR_DOWNLOAD, use, imageUrl);
            log(status);
        } finally {
            if (downloadClient != null && downloadClient.getConnectionManager() != null) {
                downloadClient.getConnectionManager().shutdown();
            }
        }
        return null;
    }

    /**
     * TODO Document.
     * 
     * @return TODO Document
     */
    public static boolean isEnableAutodownload() {
        boolean enableAutodownload = getDefault().getPreferenceStore().getBoolean(PREFERENCE_ENABLE_AUTODOWNLOAD);
        return enableAutodownload;
    }

    /**
     * TODO Document.
     * 
     * @return TODO Document
     */
    public static boolean isEnableAutoupdate() {
        boolean enableAutoupdate = getDefault().getPreferenceStore().getBoolean(PREFERENCE_ENABLE_UPDATE);
        return enableAutoupdate;
    }

    /**
     * TODO Document.
     * 
     * @param syndEntry
     *            TODO Document
     * 
     * @return TODO Document Will be <code>null</code> if entry could not be
     *         parsed.
     */
    private static FeedEntry parseEntry(final SyndEntry syndEntry) {
        String title = syndEntry.getTitle();
        URL link = null;
        try {
            link = new URL(syndEntry.getUri());
        } catch (MalformedURLException mue) {
            IStatus status = getStatuses().create(WARNING, STATUS_CODE_ERROR_LINK, mue, syndEntry.getUri());
            DailyDilbertPlugin.log(status);
            return null;
        }
        SyndContent description = syndEntry.getDescription();
        URL imageLink = parseDescription(description);
        if (imageLink == null) {
            return null;
        }
        FeedEntry feedEntry = new FeedEntry(imageLink, link, title, description);
        return feedEntry;
    }

    /**
     * TODO Document.
     * 
     * @param description
     *            TODO Document
     * 
     * @return TODO Document Will be <code>null</code> if no dilbert image url
     *         could be parsed.
     */
    private static URL parseDescription(final SyndContent description) {
        if (!EXPECTED_CONTENT_TYPE.equals(description.getType())) {
            IStatus status = getStatuses().create(WARNING, STATUS_CODE_ERROR_CONTENT_TYPE, null, "description", description.getType());
            log(status);
            return null;
        }
        try {
            String value = description.getValue();
            value = deleteWhitespace(value);
            Matcher matcher = DILBERT_IMAGE_URL_PATTERN.matcher(value);
            if (matcher.matches()) {
                String dilbertImageUrl = matcher.group(1);
                String[] splitted = split(dilbertImageUrl, "\"");
                dilbertImageUrl = splitted[0];
                return new URL(dilbertImageUrl);
            }
        } catch (MalformedURLException mue) {
            IStatus status = getStatuses().create(WARNING, STATUS_CODE_ERROR_IMAGE_URL, mue, description.getValue());
            log(status);
        }
        return null;
    }

    /**
     * Private constructor to avoid instantiation.
     */
    private FeedReaderUtil() {
    }
}
