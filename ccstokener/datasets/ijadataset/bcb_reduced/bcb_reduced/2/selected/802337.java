package org.archive.modules.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.logging.Logger;
import org.archive.modules.DefaultProcessorURI;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.state.ExampleStateProvider;
import org.archive.util.Recorder;

/**
 * Unit test for {@link ExtractorSWF}.
 *
 * @author pjack
 * @author nlevitt
 */
public class ExtractorSWFTest extends ContentExtractorTestBase {

    private static Logger logger = Logger.getLogger(ExtractorSWFTest.class.getName());

    @Override
    protected Class<ExtractorSWF> getModuleClass() {
        return ExtractorSWF.class;
    }

    @Override
    protected Object makeModule() {
        return new ExtractorSWF();
    }

    @Override
    protected ExtractorSWF makeExtractor() {
        ExtractorSWF extractor = new ExtractorSWF();
        initExtractor(extractor);
        return extractor;
    }

    protected void initExtractor(Extractor extractor) {
        UriErrorLoggerModule ulm = new UnitTestUriLoggerModule();
        ExampleStateProvider dsp = new ExampleStateProvider();
        dsp.set(extractor, Extractor.URI_ERROR_LOGGER_MODULE, ulm);
        extractor.initialTasks(dsp);
    }

    private DefaultProcessorURI setupURI(String url) throws MalformedURLException, IOException {
        UURI uuri = UURIFactory.getInstance(url);
        DefaultProcessorURI curi = new DefaultProcessorURI(uuri, uuri, LinkContext.NAVLINK_MISC);
        URLConnection conn = new URL(url).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        InputStream in = conn.getInputStream();
        Recorder recorder = Recorder.wrapInputStreamWithHttpRecord(this.getTmpDir(), this.getClass().getName(), in, null);
        logger.info("got recorder for " + url);
        curi.setContentSize(recorder.getRecordedInput().getSize());
        curi.setContentType("application/x-shockwave-flash");
        curi.setFetchStatus(200);
        curi.setRecorder(recorder);
        return curi;
    }

    public void xestHer1509() throws IOException {
        HashMap<String, String> testUrls = new HashMap<String, String>();
        testUrls.put("http://wayback.archive-it.org/779/20080709003013/http://www.dreamingmethods.com/uploads/lastdream/loader.swf", "project.swf");
        testUrls.put("http://wayback.archive-it.org/1094/20080923035716/http://www.dreamingmethods.com/uploads/dm_archive/mainsite/downloads/flash/Dim%20O%20Gauble/loader.swf", "map_3d.swf");
        testUrls.put("http://wayback.archive-it.org/1094/20080923040243/http://www.dreamingmethods.com/uploads/dm_archive/mainsite/downloads/flash/clearance/loader.swf", "clearance_intro.swf");
        for (String url : testUrls.keySet()) {
            logger.info("testing " + url);
            DefaultProcessorURI curi;
            try {
                curi = setupURI(url);
            } catch (IOException e) {
                logger.severe("unable to open url, skipping: " + e);
                continue;
            }
            long startTime = System.currentTimeMillis();
            this.extractor.extract(curi);
            long elapsed = System.currentTimeMillis() - startTime;
            logger.info(this.extractor.getClass().getSimpleName() + " took " + elapsed + "ms to process " + url);
            boolean foundIt = false;
            for (Link link : curi.getOutLinks()) {
                logger.info("found link: " + link);
                foundIt = foundIt || link.getDestination().toString().endsWith(testUrls.get(url));
            }
            assertTrue("failed to extract link \"" + testUrls.get(url) + "\" from " + url, foundIt);
        }
    }

    public void xestNonAsciiLink() throws MalformedURLException, IOException {
        HashMap<String, String> testUrls = new HashMap<String, String>();
        testUrls.put("http://wayback.archive-it.org/1100/20080721212134/http://www.marca.com/futbol/madrid_vs_barca/previa/barca/barcaOK.swf", "barca/delape%C3%B1a.swf");
        for (String url : testUrls.keySet()) {
            logger.info("testing " + url);
            DefaultProcessorURI curi;
            try {
                curi = setupURI(url);
            } catch (IOException e) {
                logger.severe("unable to open url, skipping: " + e);
                continue;
            }
            long startTime = System.currentTimeMillis();
            this.extractor.extract(curi);
            long elapsed = System.currentTimeMillis() - startTime;
            logger.info(this.extractor.getClass().getSimpleName() + " took " + elapsed + "ms to process " + url);
            boolean foundIt = false;
            for (Link link : curi.getOutLinks()) {
                logger.info("found link: " + link);
                foundIt = foundIt || link.getDestination().toString().endsWith(testUrls.get(url));
            }
            if (!foundIt) logger.severe("failed to extract link \"" + testUrls.get(url) + "\" from " + url);
            assertTrue("failed to extract link \"" + testUrls.get(url) + "\" from " + url, foundIt);
        }
    }
}
