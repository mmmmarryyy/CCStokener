package be.gervaisb.notification.core.watchers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import be.gervaisb.notification.core.events.RssEvent;

public class RssWatcher extends AbstractWatcher {

    private static final String FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";

    protected static final Logger logger = Logger.getLogger(RssWatcher.class);

    private URL url;

    private RssEvent event;

    private DateFormat dateFormat;

    public RssWatcher(String url, long delay) throws MalformedURLException, IOException {
        super(delay);
        this.url = new URL(url);
        dateFormat = new SimpleDateFormat(FORMAT, Locale.US);
    }

    @Override
    protected void close() {
        url = null;
    }

    @Override
    protected synchronized void watch() {
        try {
            try {
                logger.debug(new StringBuilder("Watching items in ").append(url));
                RssEvent lastEvent = getLastEvent();
                if (event == null || lastEvent != null && lastEvent.getPublication().compareTo(event.getPublication()) > 0) {
                    event = lastEvent;
                    fireEventReceived(event);
                }
            } catch (Exception e) {
                String message = MessageFormat.format("Unable to correctly parse the rss feed in {0}.", url);
                logger.warn(message, e);
                logger.warn("Feed parsing cancelled.");
            }
            wait(delay);
        } catch (InterruptedException e) {
            logger.warn("Thread interrupted", e);
        }
    }

    private RssEvent getLastEvent() throws DocumentException, IOException {
        Document document = new SAXReader().read(url.openStream());
        Element item = document.getRootElement().element("channel").element("item");
        Date date = new Date();
        String dateStr = item.element("pubDate").getStringValue();
        try {
            date = dateFormat.parse(dateStr);
        } catch (ParseException e) {
            String message = MessageFormat.format("Unable to parse string \"{0}\" with pattern \"{1}\".", dateStr, FORMAT);
            logger.warn(message, e);
        }
        RssEvent event = new RssEvent(this, item.element("title").getStringValue(), item.element("link").getStringValue(), item.element("description").getStringValue(), item.element("author").getStringValue(), date);
        return event;
    }
}
