package apollo.external;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import apollo.config.Config;
import apollo.datamodel.GenomicRange;
import apollo.datamodel.SeqFeatureI;
import apollo.gui.event.FeatureSelectionEvent;
import apollo.gui.event.FeatureSelectionListener;
import apollo.gui.menus.LinksMenu;
import apollo.util.HTMLUtil;
import org.apache.log4j.*;

/** Gets selection events from apollo - via controller, set up by CuiCurationState, 
    configured in style - sends selection to igb via http */
public class IgbBridge implements FeatureSelectionListener {

    protected static final Logger logger = LogManager.getLogger(IgbBridge.class);

    private static final int FIRST_PORT_TO_TRY = 7085;

    private static final int NUMBER_OF_PORTS_TO_TRY = 5;

    private String chromosome;

    private String organism;

    private int igbPort = -1;

    public IgbBridge(GenomicRange genRng) {
        setGenomicRange(genRng);
    }

    public void setGenomicRange(GenomicRange genRng) {
        chromosome = genRng.getChromosome();
        organism = genRng.getOrganism();
    }

    public boolean handleFeatureSelectionEvent(FeatureSelectionEvent evt) {
        if (!LinksMenu.igbLinksEnabled()) return false;
        if (apollo.config.Config.DEBUG) {
            if (!igbIsRunning()) {
                logger.debug("can't send off selection to IGB - it's not running");
                return false;
            }
            SeqFeatureI selectedFeat = evt.getFeature();
            if (selectedFeat == null) return false;
            try {
                URL url = makeRegionUrl(selectedFeat);
                logger.debug("Connecting to IGB with URL " + url);
                URLConnection conn = url.openConnection();
                conn.connect();
                conn.getInputStream().close();
            } catch (MalformedURLException me) {
                logger.debug("malformed url", me);
                return false;
            } catch (IOException ie) {
                logger.error("unable to connect to igb", ie);
                return false;
            }
        }
        return true;
    }

    private void connectToUrl(URL url) {
    }

    private boolean igbIsRunning() {
        findIgbLocalhostPort();
        return igbPort != -1;
    }

    /** sets igbPort with port # */
    private void findIgbLocalhostPort() {
        for (int i = 0; i < NUMBER_OF_PORTS_TO_TRY && igbPort == -1; i++) {
            int port = FIRST_PORT_TO_TRY + i;
            try {
                URL url = makePingUrl(port);
                URLConnection conn = url.openConnection();
                conn.connect();
                logger.debug("Found an igb port " + port);
                igbPort = port;
            } catch (MalformedURLException e) {
                logger.error("malformed url");
                return;
            } catch (IOException e) {
                logger.error("No port found at " + port, e);
            }
        }
    }

    private URL makeRegionUrl(SeqFeatureI selectedFeat) throws MalformedURLException {
        int padding = 400;
        int low = selectedFeat.getLow() - padding;
        if (low < 1) low = 1;
        int high = selectedFeat.getHigh() + padding;
        String urlPrefix = makeUrlPrefix();
        String u = urlPrefix + "seqid=chr" + chromosome + "&start=" + low + "&end=" + high + "&version=D_melanogaster_Apr_2004 ";
        URL url = new URL(u);
        return url;
    }

    private URL makePingUrl(int port) throws MalformedURLException {
        String pingString = makeUrlPrefix(port) + "ping";
        logger.debug("pinging " + pingString);
        return new URL(pingString);
    }

    private String makeUrlPrefix() {
        return makeUrlPrefix(igbPort);
    }

    private String makeUrlPrefix(int port) {
        return "http://localhost:" + port + "/UnibrowControl?";
    }
}
