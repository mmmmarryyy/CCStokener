package games.strategy.triplea.ui;

import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.ui.ProductionPanel.Rule;
import games.strategy.util.Tuple;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ProductionTabsProperties {

    private static final String PROPERTY_FILE = "production_tabs";

    private static final String USE_DEFAULT_TABS = "production_tabs.use_default_tabs";

    private static final String NUMBER_OF_TABS = "production_tabs.number_of_tabs";

    private static final String TAB_NAME = "production_tabs.tab_name";

    private static final String TAB_UNITS = "production_tabs.tab_units";

    private static final String NUMBER_OF_ROWS = "production_tabs.rows";

    private static final String NUMBER_OF_COLUMNS = "production_tabs.columns";

    private final Properties m_properties = new Properties();

    private final List<Rule> m_rules;

    private List<Tuple<String, List<Rule>>> m_ruleLists;

    protected ProductionTabsProperties(final PlayerID playerId, final List<Rule> mRules, final String mapDir) {
        m_rules = mRules;
        final ResourceLoader loader = ResourceLoader.getMapResourceLoader(mapDir);
        String propertyFile = PROPERTY_FILE + "." + playerId.getName() + ".properties";
        URL url = loader.getResource(propertyFile);
        if (url == null) {
            propertyFile = PROPERTY_FILE + ".properties";
            url = loader.getResource(propertyFile);
            if (url == null) {
            } else {
                try {
                    m_properties.load(url.openStream());
                } catch (final IOException e) {
                    System.out.println("Error reading " + propertyFile + e);
                }
            }
        }
    }

    public static ProductionTabsProperties getInstance(final PlayerID playerId, final List<Rule> mRules, final String mapDir) {
        return new ProductionTabsProperties(playerId, mRules, mapDir);
    }

    public List<Tuple<String, List<Rule>>> getRuleLists() {
        if (m_ruleLists != null) return m_ruleLists;
        m_ruleLists = new ArrayList<Tuple<String, List<Rule>>>();
        final int iTabs = getNumberOfTabs();
        for (int i = 1; i <= iTabs; i++) {
            final String tabName = m_properties.getProperty(TAB_NAME + "." + i);
            final List<String> tabValues = Arrays.asList(m_properties.getProperty(TAB_UNITS + "." + i).split(":"));
            final List<Rule> ruleList = new ArrayList<Rule>();
            for (final Rule rule : m_rules) {
                if (tabValues.contains(rule.getProductionRule().getResults().keySet().iterator().next().getName())) {
                    ruleList.add(rule);
                }
            }
            m_ruleLists.add(new Tuple<String, List<Rule>>(tabName, ruleList));
        }
        return m_ruleLists;
    }

    private int getNumberOfTabs() {
        return Integer.valueOf(m_properties.getProperty(NUMBER_OF_TABS, "0")).intValue();
    }

    public boolean useDefaultTabs() {
        return Boolean.valueOf(m_properties.getProperty(USE_DEFAULT_TABS, "true")).booleanValue();
    }

    public int getRows() {
        return Integer.valueOf(m_properties.getProperty(NUMBER_OF_ROWS, "0")).intValue();
    }

    public int getColumns() {
        return Integer.valueOf(m_properties.getProperty(NUMBER_OF_COLUMNS, "0")).intValue();
    }
}
