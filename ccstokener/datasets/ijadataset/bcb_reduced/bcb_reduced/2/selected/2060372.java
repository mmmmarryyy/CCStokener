package games.strategy.engine.framework.ui;

import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.GameRunner2;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class NewGameChooserEntry {

    private final URI m_url;

    private GameData m_data;

    private boolean m_gameDataFullyLoaded = false;

    public NewGameChooserEntry(final URI uri) throws IOException, GameParseException, SAXException, EngineVersionException {
        m_url = uri;
        final InputStream input = uri.toURL().openStream();
        final boolean delayParsing = GameRunner2.getDelayedParsing();
        try {
            m_data = new GameParser().parse(input, delayParsing);
            m_gameDataFullyLoaded = !delayParsing;
        } finally {
            try {
                input.close();
            } catch (final IOException e) {
            }
        }
    }

    public void fullyParseGameData() throws GameParseException {
        m_data = null;
        InputStream input;
        String error = null;
        try {
            input = m_url.toURL().openStream();
            try {
                m_data = new GameParser().parse(input, false);
                m_gameDataFullyLoaded = true;
            } catch (final EngineVersionException e) {
                System.out.println(e.getMessage());
                error = e.getMessage();
            } catch (final SAXParseException e) {
                System.err.println("Could not parse:" + m_url + " error at line:" + e.getLineNumber() + " column:" + e.getColumnNumber());
                e.printStackTrace();
                error = e.getMessage();
            } catch (final Exception e) {
                System.err.println("Could not parse:" + m_url);
                e.printStackTrace();
                error = e.getMessage();
            } finally {
                try {
                    input.close();
                } catch (final IOException e) {
                }
            }
        } catch (final MalformedURLException e1) {
            e1.printStackTrace();
            error = e1.getMessage();
        } catch (final IOException e1) {
            e1.printStackTrace();
            error = e1.getMessage();
        }
        if (error != null) throw new GameParseException(error);
    }

    /**
	 * Do not use this if possible. Instead try to remove the bad map from the GameChooserModel.
	 * If that fails, then do a short parse so the user doesn't get a null pointer error.
	 */
    public void delayParseGameData() {
        m_data = null;
        InputStream input;
        try {
            input = m_url.toURL().openStream();
            try {
                m_data = new GameParser().parse(input, true);
                m_gameDataFullyLoaded = false;
            } catch (final EngineVersionException e) {
                System.out.println(e.getMessage());
            } catch (final SAXParseException e) {
                System.err.println("Could not parse:" + m_url + " error at line:" + e.getLineNumber() + " column:" + e.getColumnNumber());
                e.printStackTrace();
            } catch (final Exception e) {
                System.err.println("Could not parse:" + m_url);
                e.printStackTrace();
            } finally {
                try {
                    input.close();
                } catch (final IOException e) {
                }
            }
        } catch (final MalformedURLException e1) {
            e1.printStackTrace();
        } catch (final IOException e1) {
            e1.printStackTrace();
        }
    }

    public boolean isGameDataLoaded() {
        return m_gameDataFullyLoaded;
    }

    @Override
    public String toString() {
        return m_data.getGameName();
    }

    public GameData getGameData() {
        return m_data;
    }

    public URI getURI() {
        return m_url;
    }

    public String getLocation() {
        final String raw = m_url.toString();
        final String base = GameRunner.getRootFolder().toURI().toString() + "maps";
        if (raw.startsWith(base)) {
            return raw.substring(base.length());
        }
        if (raw.startsWith("jar:" + base)) {
            return raw.substring("jar:".length() + base.length());
        }
        return raw;
    }
}
