package games.strategy.triplea.ui;

import games.strategy.triplea.ResourceLoader;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Properties;

public class NotificationMessages {

    private static final String PROPERTY_FILE = "notifications.properties";

    private static NotificationMessages s_nm = null;

    private static long s_timestamp = 0;

    private final Properties m_properties = new Properties();

    protected NotificationMessages() {
        final ResourceLoader loader = ResourceLoader.getMapResourceLoader(UIContext.getMapDir());
        final URL url = loader.getResource(PROPERTY_FILE);
        if (url == null) {
        } else {
            try {
                m_properties.load(url.openStream());
            } catch (final IOException e) {
                System.out.println("Error reading " + PROPERTY_FILE + " : " + e);
            }
        }
    }

    public static NotificationMessages getInstance() {
        if (s_nm == null || Calendar.getInstance().getTimeInMillis() > s_timestamp + 10000) {
            s_nm = new NotificationMessages();
            s_timestamp = Calendar.getInstance().getTimeInMillis();
        }
        return s_nm;
    }

    public String getMessage(final String notificationMessageKey) {
        return m_properties.getProperty(notificationMessageKey, notificationMessageKey);
    }
}
