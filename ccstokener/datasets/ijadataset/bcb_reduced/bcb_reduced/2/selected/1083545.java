package games.strategy.kingstable.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import java.io.InputStream;
import java.net.URL;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Lane Schwartz
 * @version $LastChangedDate: 2012-01-25 12:33:43 -0500 (Wed, 25 Jan 2012) $
 */
public class DelegateTest extends TestCase {

    protected GameData m_data;

    protected PlayerID black;

    protected PlayerID white;

    protected Territory[][] territories;

    protected UnitType pawn;

    protected UnitType king;

    /**
	 * Creates new DelegateTest
	 */
    public DelegateTest(final String name) {
        super(name);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite();
        suite.addTestSuite(DelegateTest.class);
        return suite;
    }

    @Override
    public void setUp() throws Exception {
        final URL url = this.getClass().getResource("DelegateTest.xml");
        final InputStream input = url.openStream();
        m_data = (new GameParser()).parse(input, false);
        input.close();
        black = m_data.getPlayerList().getPlayerID("Black");
        white = m_data.getPlayerList().getPlayerID("White");
        territories = new Territory[m_data.getMap().getXDimension()][m_data.getMap().getYDimension()];
        for (int x = 0; x < m_data.getMap().getXDimension(); x++) for (int y = 0; y < m_data.getMap().getYDimension(); y++) territories[x][y] = m_data.getMap().getTerritoryFromCoordinates(x, y);
        pawn = m_data.getUnitTypeList().getUnitType("pawn");
        king = m_data.getUnitTypeList().getUnitType("king");
    }

    public void assertValid(final String string) {
        assertNull(string);
    }

    public void assertError(final String string) {
        assertNotNull(string);
    }

    public void testTest() {
        assertValid(null);
        assertError("Can not do this");
    }
}
