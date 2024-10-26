package net.sf.jdistunit.daemon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import net.sf.agentopia.core.AgentopiaAgentMarker;
import net.sf.agentopia.util.OS;
import net.sf.jdistunit.platform.action.ITestThread;
import net.sf.jdistunit.platform.agent.JDistUnitAgent;

/**
 * Test agent that reads the HTML content from the JDistUnit homepage.
 * <p>
 * If <code>lineDelay</code> is set (to a number of milliseconds), the agent
 * will makes delays between reading each line (to simulate a very busy server).
 */
@AgentopiaAgentMarker(name = "JDistUnit Homepage Test Agent")
public class HomepageTestAgent extends JDistUnitAgent {

    /** Serial version uid. */
    private static final long serialVersionUID = -1115550838130690448L;

    /** The homepage URL. */
    public String testUrl = "http://jdistunit.sourceforge.net";

    /** The delay to wait between reading lines. */
    public long lineDelay = 0;

    /** The JDistUnit page that has been read so far. */
    public StringBuilder page = new StringBuilder(65536);

    /** The stream reader. Not to be transferred over agent network. */
    public transient BufferedReader in = null;

    /**
     * An empty constructor is necessary for (de)serialisation of the agent.
     */
    public HomepageTestAgent() {
    }

    /**
     * A new homepage test agent.
     * 
     * @param testUrl The homepage url, e.g. "http://jdistunit.sourceforge.net".
     */
    public HomepageTestAgent(String testUrl) {
        this.testUrl = testUrl;
    }

    /**
     * The test action, namely reading the homepage line by line.
     * 
     * @see net.sf.jdistunit.platform.action.ITestAction#testAction(net.sf.jdistunit.platform.action.ITestThread)
     */
    public void testAction(ITestThread testThread) throws Throwable {
        try {
            readHomePage(testThread);
        } finally {
            closeReader();
        }
    }

    /**
     * Reads the JDistUnit homepage.
     * 
     * @param testThread The currently executing test thread.
     * @throws IOException If reading failed.
     */
    private void readHomePage(ITestThread testThread) throws IOException {
        if (null == testThread) {
            throw new IllegalArgumentException("Test thread may not be null.");
        }
        final InputStream urlIn = new URL(testUrl).openStream();
        final int availableBytes = urlIn.available();
        if (0 == availableBytes) {
            throw new IllegalStateException("Zero bytes on target host.");
        }
        in = new BufferedReader(new InputStreamReader(urlIn));
        String line;
        while (null != in && null != (line = in.readLine())) {
            page.append(line);
            page.append('\n');
            if (0 != lineDelay) {
                OS.sleep(lineDelay);
            }
            if (testThread.isActionStopped()) {
                break;
            }
        }
    }

    /**
     * Closes the homepage reader.
     * 
     * @throws IOException If closing failed.
     */
    private void closeReader() throws IOException {
        if (null != in) {
            in.close();
            in = null;
        }
    }
}
