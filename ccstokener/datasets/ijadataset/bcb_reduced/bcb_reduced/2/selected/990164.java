package eu.funcnet.clients.picr.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

/**
 * HTTP client for PICR, possibly a bit simpler to use than the SOAP one.
 */
public class PicrHttpClient {

    private static final String __urlTempl2 = "http://www.ebi.ac.uk/Tools/picr/rest/getUPIForAccession?accession=%s&database=SWISSPROT&taxid=%d&onlyactive=true";

    private static final String __xpath = "//*[local-name()='identicalCrossReferences']/*[local-name()='accession']";

    private static final int __connTimeout = 5 * 1000;

    private static final int __readTimeout = 30 * 1000;

    /**
	 * As getProteins, but any input proteins that are not mapped successfully
	 * are retained verbatim.
	 * 
	 * @param queries
	 * @param taxon
	 * @return
	 * @throws IOException
	 * @throws XPathExpressionException
	 * @throws ParserConfigurationException
	 */
    public static Multimap<String, String> getProteinsSafely(final Set<String> queries, final int taxon) throws IOException, XPathExpressionException, ParserConfigurationException {
        final Multimap<String, String> mMap = getProteins(queries, taxon);
        final Set<String> mappedOK = new HashSet<String>(mMap.values());
        final SetView<String> missing = Sets.difference(queries, mappedOK);
        for (final String protein : missing) {
            mMap.put(protein, protein);
        }
        return mMap;
    }

    public static Multimap<String, String> getProteins(final Set<String> queries, final int taxon) throws ParserConfigurationException, XPathExpressionException {
        final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final XPathExpression xpe = XPathFactory.newInstance().newXPath().compile(__xpath);
        final Multimap<String, String> proteins = HashMultimap.create();
        for (final String query : queries) {
            HttpURLConnection connection = null;
            try {
                final String encoded = URLEncoder.encode(query.trim(), "UTF-8");
                final URL url = new URL(String.format(__urlTempl2, encoded, taxon));
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(__connTimeout);
                connection.setReadTimeout(__readTimeout);
                connection.setRequestProperty("Connection", "close");
                connection.connect();
                final InputStream stream = connection.getInputStream();
                final Document doc = parser.parse(stream);
                final NodeList nodes = (NodeList) xpe.evaluate(doc, XPathConstants.NODESET);
                if (nodes != null) {
                    final int n = nodes.getLength();
                    for (int i = 0; i < n; i++) {
                        final Node node = nodes.item(i);
                        proteins.put(node.getTextContent().trim(), query.trim());
                    }
                }
            } catch (final Exception ex) {
                continue;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        return proteins;
    }
}
