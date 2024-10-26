package net.sf.jdistunit.platform.action;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import junit.framework.TestCase;
import net.sf.agentopia.util.OS;
import net.sf.jdistunit.platform.action.ITestAction;
import net.sf.jdistunit.platform.action.ITestThread;

/**
 * Test a simple internet test action.
 * 
 * @author <a href="mailto:kain@land-of-kain.de">Kai Ruhl</a>
 * @since 24 Sep 2009
 */
public class TestActionTest extends TestCase {

    /**
     * An example test action, connecting to a web server.
     * <p>
     * Reads the HTML content from the JDistUnit homepage, and optionally makes
     * delays between each line (so as to simulate a very busy server).
     */
    public static class ExampleTestAction implements ITestAction {

        /** The page that has been read. */
        public StringBuilder page = new StringBuilder(65536);

        /** The stream reader. */
        public BufferedReader in = null;

        /** The delay to wait between reading lines. */
        public long lineDelay = 0;

        /**
         * @see net.sf.jdistunit.platform.action.ITestAction#testAction(net.sf.jdistunit.platform.action.ITestThread)
         */
        @Override
        public void testAction(ITestThread testThread) throws Throwable {
            try {
                final InputStream urlIn = new URL("http://jdistunit.sourceforge.net").openStream();
                final int availableBytes = urlIn.available();
                if (0 == availableBytes) {
                    throw new IllegalStateException("Zero bytes on target host.");
                }
                in = new BufferedReader(new InputStreamReader(urlIn));
                String line;
                while (null != (line = in.readLine())) {
                    page.append(line);
                    page.append('\n');
                    if (0 != lineDelay) {
                        OS.sleep(lineDelay);
                    }
                    if (null != testThread && testThread.isActionStopped()) {
                        break;
                    }
                }
            } finally {
                if (null != in) {
                    in.close();
                    in = null;
                }
            }
        }
    }

    /**
     * @throws Throwable If something failed.
     */
    public void testInternetAction() throws Throwable {
        ExampleTestAction action = new ExampleTestAction();
        action.testAction(null);
        String page = action.page.toString();
        assertNotNull(page);
        assertTrue(page.contains("JDistUnit"));
        assertTrue(page.contains("Load testing"));
        assertTrue(page.contains("wikipedia"));
    }
}
