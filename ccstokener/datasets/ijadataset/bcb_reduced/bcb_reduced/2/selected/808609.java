package com.hsbc.hbfr.ccf.at.logreader.model;

import com.hsbc.hbfr.ccf.at.logreader.ui.ProgressIndicatorNeutral;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

public class URLLogReader extends LogReader implements LogProvider {

    private static final Logger logger = Logger.getLogger("INFRA." + URLLogReader.class.getName());

    private final URL url;

    public URLLogReader(LogEventRecognizer recognize, URL anURL) {
        super(recognize);
        url = anURL;
    }

    public URLLogReader(URL anUrl) {
        super();
        url = anUrl;
    }

    @Override
    public List<LogEvent> parse() throws IOException {
        long start = System.currentTimeMillis();
        ProgressIndicatorNeutral myProgressMonitor = getProgressMonitor();
        List<LogEvent> l = super.parse(new InputStreamReader(url.openStream()));
        myProgressMonitor.worked(toBeWorked);
        long end = System.currentTimeMillis();
        if (logger.isDebugEnabled()) {
            logger.debug("parsed [" + url + "] in " + (end - start) + " milliseconds");
        }
        return l;
    }

    public List<LogEvent> getEvents(ProgressIndicatorNeutral monitor, int toBeWorked) throws IOException {
        setProgressMonitor(monitor);
        setToBeWorked(toBeWorked);
        return parse();
    }

    public boolean isTransient() {
        return false;
    }

    @Override
    protected SimpleLogEvent createEvent(String eventString, LogEventRecognizer reco) {
        return new ProvidedEvent(this, reco, eventString);
    }

    @Override
    public String toString() {
        return url.toString();
    }

    public static LogProvider fromProperties(Properties props, String providerPrefix) throws MalformedURLException {
        String providerAsString = props.getProperty(providerPrefix);
        URL url = new URL(providerAsString);
        try {
            LogEventRecognizer recognizer = LogEventRecognizer.Factory.getRecognizerFromProperties(props, providerPrefix + ".recognizer");
            return new URLLogReader(recognizer, url);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
