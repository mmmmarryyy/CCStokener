package games.strategy.triplea.ui;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.ResourceLoader;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Properties;

public class TooltipProperties {

    private static final String PROPERTY_FILE = "tooltips.properties";

    private static final String TOOLTIP = "tooltip";

    private static final String UNIT = "unit";

    private static TooltipProperties s_ttp = null;

    private static long s_timestamp = 0;

    private final Properties m_properties = new Properties();

    protected TooltipProperties() {
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

    public static TooltipProperties getInstance() {
        if (s_ttp == null || Calendar.getInstance().getTimeInMillis() > s_timestamp + 5000) {
            s_ttp = new TooltipProperties();
            s_timestamp = Calendar.getInstance().getTimeInMillis();
        }
        return s_ttp;
    }

    public String getToolTip(final UnitType ut, final PlayerID playerId) {
        final String tooltip = m_properties.getProperty(TOOLTIP + "." + UNIT + "." + ut.getName() + "." + playerId.getName(), "");
        if (tooltip == null || tooltip.equals("")) return m_properties.getProperty(TOOLTIP + "." + UNIT + "." + ut.getName(), ""); else return tooltip;
    }
}
