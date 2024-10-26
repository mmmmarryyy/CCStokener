package nl.utwente.ewi.stream.network.provenance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Logger;
import nl.utwente.ewi.stream.network.AbstractPE;
import nl.utwente.ewi.stream.network.Configuration;
import nl.utwente.ewi.stream.network.TupeloProxy;
import nl.utwente.ewi.stream.network.attributes.Buffer;
import nl.utwente.ewi.stream.network.attributes.Trigger;

/**
 * WikiProvenance provider documents course grained data provenance in a 
 * Mediawiki System with SemanticMediawiki extension and the AddPage extension.
 * Configuration parameters are
 * <ul><li>wiki-url: containing the url to the wiki home page
 * <li> wiki-namespace: containing the namespace name where the provenance information should be added
 * <li> wiki-namespace-number: containing the number associated with the above mentioned namespace name, 
 * where the provenance information is added.
 * </ul>
 * @author wombachera
 *
 */
public class WikiProvenance extends AbstractProvenanceProvider {

    private static final Logger logger = Logger.getLogger(WikiProvenance.class.getName());

    private String namespace;

    private String namespaceNumber;

    private String url;

    private String username;

    private String password;

    private String domain;

    public WikiProvenance(Configuration config) {
        namespace = config.getWikiNamespace();
        namespaceNumber = config.getWikiNamespaceNumber();
        url = config.getWikiUrl();
        username = config.getUsername();
        password = config.getPassword();
    }

    /**
     * Send processing element information to WIKI
     * @throws UnsupportedEncodingException 
     */
    @Override
    public void sendProvenance(TripleCollection tripleCollection) {
    }

    @Override
    public boolean checkConnection() {
        int status = 0;
        try {
            URL url = new URL(TupeloProxy.endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            status = conn.getResponseCode();
        } catch (Exception e) {
            logger.severe("Connection test failed with code:" + status);
            e.printStackTrace();
        }
        if (status < 200 || status >= 400) return false;
        String url = this.url + "?title=Special:UserLogin&action=submitlogin&type=login&returnto=Main_Page&wpDomain=" + domain + "&wpLoginattempt=Log%20in&wpName=" + username + "&wpPassword=" + password;
        return true;
    }

    @Override
    public byte[] getProvenanceGraph(AbstractPE node, Map<String, String> options) {
        return null;
    }

    @Override
    public String getProvenanceRelations(AbstractPE node) {
        return null;
    }

    @Override
    public int getType() {
        return ProvenanceManager.WIKI;
    }
}
