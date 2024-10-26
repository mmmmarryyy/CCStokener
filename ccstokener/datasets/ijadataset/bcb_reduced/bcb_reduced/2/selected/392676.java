package com.volantis.synergetics.reporting.impl;

import com.volantis.synergetics.localization.LocalizationFactory;
import com.volantis.synergetics.log.LogDispatcher;
import com.volantis.synergetics.reporting.Report;
import com.volantis.synergetics.reporting.ReportingException;
import com.volantis.synergetics.reporting.ReportingTransactionFactory;
import com.volantis.synergetics.reporting.DynamicReport;
import com.volantis.synergetics.reporting.config.JibxReportingConfigParser;
import com.volantis.synergetics.reporting.config.ReportingConfig;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * This factory crates a {@link Report} instance using the {@link
 * MetricGroupProxyFactory}.
 *
 * To specify the location of the configuration file a key {@link
 * #LOCATION_KEY} must be put in the configuration and map to either a {@link
 * URL}, a {@link String} representation of a URL or an {@link InputStream}.
 */
public class DefaultReportingTransactionFactory implements ReportingTransactionFactory {

    /**
     * Used for logging.
     */
    private static final LogDispatcher LOGGER = LocalizationFactory.createLogger(DefaultReportingTransactionFactory.class);

    /**
     * The key used to find the location of the configuration file
     */
    public static final String LOCATION_KEY = "reporting.config.location";

    /**
     * The metric group proxy factory.
     */
    private final MetricGroupProxyFactory FACTORY;

    /**
     * Create a new ReportingTransaction instance using the parameters
     * specified in the config.
     *
     * @param config a map of configuration information
     * @throws ReportingException this indicates a fatal error occurred.
     */
    public DefaultReportingTransactionFactory(Map config) throws ReportingException {
        Object location = config.get(LOCATION_KEY);
        InputStream is = null;
        try {
            if (location instanceof String) {
                URL url = new URL((String) location);
                is = url.openStream();
            } else if (location instanceof URL) {
                is = ((URL) location).openStream();
            } else if (location instanceof InputStream) {
                is = (InputStream) location;
            } else {
                LOGGER.warn("invalid-url", location);
                LOGGER.warn("reporting-disabled");
            }
        } catch (MalformedURLException e) {
            LOGGER.warn("invalid-url", location);
            LOGGER.warn("reporting-disabled");
        } catch (IOException e) {
            LOGGER.warn("configuration-file-not-found", location);
            LOGGER.warn("reporting-disabled");
        }
        ReportingConfig reportingConfig = null;
        if (is != null) {
            try {
                JibxReportingConfigParser parser = new JibxReportingConfigParser();
                reportingConfig = parser.parse(is);
            } catch (RuntimeException re) {
                LOGGER.fatal("failed-to-parse-configuration-file", location, re);
                throw new ReportingException("failed-to-parse-configuration-file", location);
            }
        }
        FACTORY = new MetricGroupProxyFactory(reportingConfig);
    }

    public Report createReport(Class clazz) {
        return FACTORY.createReport(clazz);
    }

    public Report createReport(Class clazz, String transactionID) {
        return FACTORY.createReport(clazz, transactionID);
    }

    public DynamicReport createDynamicReport(String binding) {
        return FACTORY.createDynamicReport(binding);
    }

    public DynamicReport createDynamicReport(String binding, String transactionID) {
        return FACTORY.createDynamicReport(binding, transactionID);
    }
}
