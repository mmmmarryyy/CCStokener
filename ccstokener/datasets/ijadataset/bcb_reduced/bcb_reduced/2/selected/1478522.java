package games.strategy.engine.framework;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.data.SerializationTest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import junit.framework.TestCase;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company:
 * </p>
 * 
 * @author unascribed
 * @version 1.0
 */
public class GameDataManagerTest extends TestCase {

    public GameDataManagerTest(final String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        final URL url = SerializationTest.class.getResource("Test.xml");
        final InputStream input = url.openStream();
        (new GameParser()).parse(input, false);
    }

    public void testLoadStoreKeepsGamUUID() throws IOException {
        final GameData data = new GameData();
        final GameDataManager m = new GameDataManager();
        final ByteArrayOutputStream sink = new ByteArrayOutputStream();
        m.saveGame(sink, data);
        final GameData loaded = m.loadGame(new ByteArrayInputStream(sink.toByteArray()));
        assertEquals(loaded.getProperties().get(GameData.GAME_UUID), data.getProperties().get(GameData.GAME_UUID));
    }
}
