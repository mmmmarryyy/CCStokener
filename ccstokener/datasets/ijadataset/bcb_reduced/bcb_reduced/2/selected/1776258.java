package games.strategy.triplea.ui;

import games.strategy.triplea.ResourceLoader;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Properties;

/**
 * Returns a bunch of messages from politicstext.properties
 * 
 * @author Edwin van der Wal
 * 
 */
public class PoliticsText {

    private static final String PROPERTY_FILE = "politicstext.properties";

    private static PoliticsText s_pt = null;

    private static long s_timestamp = 0;

    private final Properties m_properties = new Properties();

    private static final String BUTTON = "BUTTON";

    private static final String DESCRIPTION = "DESCRIPTION";

    private static final String NOTIFICATION_SUCCESS = "NOTIFICATION_SUCCESS";

    private static final String OTHER_NOTIFICATION_SUCCESS = "OTHER_NOTIFICATION_SUCCESS";

    private static final String NOTIFICATION_FAILURE = "NOTIFICATION_FAILURE";

    private static final String OTHER_NOTIFICATION_FAILURE = "OTHER_NOTIFICATION_FAILURE";

    private static final String ACCEPT_QUESTION = "ACCEPT_QUESTION";

    protected PoliticsText() {
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

    public static PoliticsText getInstance() {
        if (s_pt == null || Calendar.getInstance().getTimeInMillis() > s_timestamp + 10000) {
            s_pt = new PoliticsText();
            s_timestamp = Calendar.getInstance().getTimeInMillis();
        }
        return s_pt;
    }

    private String getString(final String value) {
        return m_properties.getProperty(value, "NO: " + value + " set.");
    }

    private String getMessage(final String politicsKey, final String messageKey) {
        return getString(politicsKey + "." + messageKey);
    }

    public String getButtonText(final String politicsKey) {
        return getMessage(politicsKey, BUTTON);
    }

    public String getDescription(final String politicsKey) {
        return getMessage(politicsKey, PoliticsText.DESCRIPTION);
    }

    public String getNotificationSucccess(final String politicsKey) {
        return getMessage(politicsKey, PoliticsText.NOTIFICATION_SUCCESS);
    }

    public String getNotificationSuccessOthers(final String politicsKey) {
        return getMessage(politicsKey, PoliticsText.OTHER_NOTIFICATION_SUCCESS);
    }

    public String getNotificationFailure(final String politicsKey) {
        return getMessage(politicsKey, PoliticsText.NOTIFICATION_FAILURE);
    }

    public String getNotificationFailureOthers(final String politicsKey) {
        return getMessage(politicsKey, PoliticsText.OTHER_NOTIFICATION_FAILURE);
    }

    public String getAcceptanceQuestion(final String politicsKey) {
        return getMessage(politicsKey, PoliticsText.ACCEPT_QUESTION);
    }
}
