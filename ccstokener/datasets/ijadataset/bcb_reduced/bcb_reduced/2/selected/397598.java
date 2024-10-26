package org.gromurph.util.junitextensions;

import java.io.*;
import java.net.*;
import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * The <code>UrlLoadTest</code> is an example
 * load test case which tests a URL response time.
 *
 * @author <a href="mailto:mike@clarkware.com">Mike Clark</a>
 * @author <a href="http://www.clarkware.com">Clarkware Consulting</a>
 *
 * @see LoadTestCase
 */
public class UrlLoadTest extends LoadTestCase {

    private List<URL> _urls;

    /**
	 * Constructs a <code>UrlLoadTest</code>
	 * with the specified name.
	 *
	 * @param name Test name.
	 */
    public UrlLoadTest(String name) {
        super(name);
        setMaxUsers(10);
        setTimer(new ConstantTimer(0));
        setMaxElapsedTime(2000);
        addUrl("http://localhost:8000");
    }

    /**
	 * Sets up the test fixture.
	 */
    protected void setUp() {
        super.setUp();
    }

    /**
	 * Tears down the test fixture.
	 */
    protected void tearDown() {
        super.tearDown();
    }

    /**
	 * Assembles and returns a test suite for all
	 * the test methods of this class.
	 * <p>
	 * All the load-related methods should be
	 * added here.
	 *
	 * @return A non-null test suite.
	 */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(makeLoadTest(new UrlLoadTest("testURL")));
        return suite;
    }

    /**
	 * Example load test.
	 */
    public void xtestURL() {
        try {
            Iterator urlIter = getUrls().iterator();
            while (urlIter.hasNext()) {
                URL url = (URL) urlIter.next();
                URLConnection connection = url.openConnection();
                connection.setUseCaches(false);
                int error = ((HttpURLConnection) connection).getResponseCode();
                assertTrue((error == 200) || (error == 401) || (error == 302));
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = reader.readLine();
                while (line != null) {
                    line = reader.readLine();
                }
                reader.close();
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
	 * Adds the specified URL to the collection of URLs under test.
	 *
	 * @param urlString URL to add.
	 */
    public void addUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            getUrls().add(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Returns the collections of URLs under test.
	 *
	 * @return URLs under test.
	 */
    protected List<URL> getUrls() {
        if (_urls == null) {
            _urls = new ArrayList<URL>();
        }
        return _urls;
    }

    /**
	 * Test main.
	 */
    public static void main(String args[]) {
        instanceMain(UrlLoadTest.class.getName(), args);
    }
}
