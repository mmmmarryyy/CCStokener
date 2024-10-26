package games.strategy.engine.data;

import games.strategy.triplea.Constants;
import java.io.InputStream;
import java.net.URL;
import junit.framework.TestCase;

/**
 * @author Mukul Agrawal
 * 
 */
public class AllianceTrackerTest extends TestCase {

    private GameData m_data;

    @Override
    public void setUp() throws Exception {
        final URL url = this.getClass().getResource("Test.xml");
        final InputStream input = url.openStream();
        m_data = (new GameParser()).parse(input, false);
    }

    public void testAddAlliance() throws Exception {
        final PlayerID bush = m_data.getPlayerList().getPlayerID("bush");
        final PlayerID castro = m_data.getPlayerList().getPlayerID("castro");
        final AllianceTracker allianceTracker = m_data.getAllianceTracker();
        final RelationshipTracker relationshipTracker = m_data.getRelationshipTracker();
        assertEquals(relationshipTracker.isAllied(bush, castro), false);
        allianceTracker.addToAlliance(bush, "natp");
        relationshipTracker.setRelationship(bush, castro, m_data.getRelationshipTypeList().getRelationshipType(Constants.RELATIONSHIP_TYPE_DEFAULT_ALLIED));
        assertEquals(relationshipTracker.isAllied(bush, castro), true);
    }

    @Override
    public void tearDown() throws Exception {
        m_data = null;
    }
}
