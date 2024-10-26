package org.redmine.ta.internal.logging;

import org.junit.Test;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Test case for the {@link LoggerFactory}.
 *
 * @author Matthias Paul Scholz
 */
public class LoggerFactoryTest {

    private static final String PROPERTIES_FILE_NAME = "redmine.log.properties";

    private static final String PROPERTY_KEY_LOGLEVEL = "log.level";

    /**
     * Tests the creation of loggers.
     */
    @Test
    public void testGetLogger() {
        String loggerIdentifier = "Test logger";
        Logger logger1 = LoggerFactory.getLogger(loggerIdentifier);
        Logger logger2 = LoggerFactory.getLogger(loggerIdentifier);
        Logger logger3 = LoggerFactory.getLogger(this.getClass());
        assertNotNull("First logger retrieved from LoggerFactory should not be null", logger1);
        assertNotNull("Second logger retrieved from LoggerFactory should not be null", logger2);
        assertNotNull("Third logger retrieved from LoggerFactory should not be null", logger3);
        assertTrue("Two loggers with same identifier should point to the same instance", logger1 == logger2);
        assertFalse("Expected third logger not to be equal to first logger", logger3.equals(logger1));
    }

    /**
     * Tests the correct configuration of the {@link LoggerFactory}.
     * The test assumes that there is a valid log configuration file in the classpath.
     * @throws IOException thrown in case the configuration file could not be read or is invalid
     * @throws IllegalArgumentException thrown in case the configuration file contains an invalid value
     */
    @Test
    public void testValidLogConfiguration() throws IOException, IllegalArgumentException {
        URL url = ClassLoader.getSystemResource(PROPERTIES_FILE_NAME);
        if (url == null) {
            throw new IOException("Could not find configuration file " + PROPERTIES_FILE_NAME + " in class path");
        }
        Properties properties = new Properties();
        properties.load(url.openStream());
        LogLevel logLevel = LogLevel.valueOf((String) properties.get(PROPERTY_KEY_LOGLEVEL));
        if (logLevel == null) {
            throw new IOException("Invalid configuration file " + PROPERTIES_FILE_NAME + ": no entry for " + PROPERTY_KEY_LOGLEVEL);
        }
        String loggerIdentifier = "Test logger";
        Logger logger = LoggerFactory.getLogger(loggerIdentifier);
        assertEquals("Logger has wrong log level", logLevel, logger.getLogLevel());
    }
}
