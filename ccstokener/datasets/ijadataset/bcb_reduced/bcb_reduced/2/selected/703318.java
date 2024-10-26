package au.edu.diasb.annotation.danno.test;

import static au.edu.diasb.annotation.danno.test.CommonTestRecords.ANNOTATION_TEMPLATE;
import static au.edu.diasb.annotation.danno.test.CommonTestRecords.ANNOTATION_WITH_BODY_TEMPLATE;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.xml.datatype.DatatypeConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import au.edu.diasb.annotation.danno.admin.DannoRequestFailureException;
import au.edu.diasb.annotation.danno.protocol.DannoClient;
import au.edu.diasb.annotation.danno.protocol.LoginDannoClientFactory;

/**
 * Tests for Danno OAI functionality.  The tests should touch all of the requests, but are
 * mostly focused on streaming support via resumption tokens.
 * <p>
 * This class is designed to be run as a stand-alone application. 
 * 
 * @author scrawley
 */
public class DannoOAITest extends DannoTestAppBase {

    private DannoClient oaiClient;

    public DannoOAITest() throws DatatypeConfigurationException, ClientProtocolException, IOException {
        super(TestProperties.getProperties());
        oaiClient = new LoginDannoClientFactory(loginUrl, username, password).createClient((HttpServletRequest) null, oaiUrl);
    }

    public void run(String[] args) throws IOException, ProtocolException {
        parseTestArgs(args);
        run(getLoopCount());
    }

    public static void main(String[] args) throws Exception {
        new DannoOAITest().run(args);
    }

    public void run(final long count) throws IOException, ProtocolException {
        doReset();
        report("Loading OAI annotations");
        String annotationURL = null;
        String annotationURL2 = null;
        String bodyURL = null;
        String bodyURL2 = null;
        for (int i = 0; i < count; i++) {
            String tmp = doPostRDF(ANNOTATION_TEMPLATE, "http://serv1.example.com/some/page.html", "http://serv1.example.com/some/annotation-" + i + ".html");
            if (bodyURL == null && tmp != null) {
                annotationURL = extractAnnotationURL(tmp);
                bodyURL = extractBodyURL(tmp);
            }
        }
        for (int i = 0; i < count; i++) {
            doPostRDF(ANNOTATION_TEMPLATE, "http://serv1.example.com/some/page-" + i + ".html", "http://serv1.example.com/some/annotation-" + i + ".html");
        }
        for (int i = 0; i < count; i++) {
            String tmp = doPostRDF(ANNOTATION_WITH_BODY_TEMPLATE, "http://serv1.example.com/some/page-" + i + ".html", "");
            if (bodyURL2 == null && tmp != null) {
                annotationURL2 = extractAnnotationURL(tmp);
                bodyURL2 = extractBodyURL(tmp);
            }
        }
        if (annotationURL == null || annotationURL2 == null) {
            System.err.println("Setup failed: bailing out");
            return;
        }
        report("Starting OAI tests");
        bad();
        identify();
        getRecord(extractRecordId(annotationURL, annoteaUrl), "oai_dc", null);
        getRecord(extractRecordId(annotationURL2, annoteaUrl), "oai_dc", null);
        getRecord(extractRecordId(annotationURL, annoteaUrl), "rdf", null);
        getRecord(extractRecordId(annotationURL2, annoteaUrl), "rdf", null);
        getRecord("fish", "rdf", "idDoesNotExist");
        getRecord("fish", "oai_dc", "idDoesNotExist");
        getRecord(extractRecordId(annotationURL, annoteaUrl), "xml", "cannotDisseminateFormat");
        getRecord(extractRecordId(annotationURL2, annoteaUrl), "xml", "cannotDisseminateFormat");
        listSets();
        long start = System.currentTimeMillis();
        listIdentifiers("oai_dc", count * 3, null);
        report(start, count * 3, "OAI list identifiers");
        start = System.currentTimeMillis();
        listRecords("oai_dc", count * 3, null);
        report(start, count * 3, "OAI list records (oai_dc)");
        start = System.currentTimeMillis();
        listRecords("rdf", count * 3, null);
        report(start, count * 3, "OAI list records (rdf)");
        report("Finished OAI tests");
    }

    private void listIdentifiers(String prefix, long expected, String expectedError) throws IOException, ProtocolException {
        List<String> identifiers = new ArrayList<String>();
        String response = doOAIQuery("?verb=ListIdentifiers&metadataPrefix=" + prefix);
        while (true) {
            if (!checkForOAIPMHError(response, expectedError)) {
                break;
            }
            extractIdentifiers(response, identifiers);
            String resumptionToken = extractResumptionToken(response);
            if (resumptionToken == null || resumptionToken.length() == 0) {
                break;
            }
            response = doOAIQuery("?verb=ListIdentifiers&resumptionToken=" + resumptionToken);
        }
        if (identifiers.size() != expected) {
            System.err.println("ERROR: Got " + identifiers.size() + " identifiers: expected " + expected);
        }
    }

    private void listRecords(String prefix, long expected, String expectedError) throws IOException, ProtocolException {
        List<String> identifiers = new ArrayList<String>();
        String response = doOAIQuery("?verb=ListRecords&metadataPrefix=" + prefix);
        while (true) {
            if (!checkForOAIPMHError(response, expectedError)) {
                break;
            }
            extractIdentifiers(response, identifiers);
            String resumptionToken = extractResumptionToken(response);
            if (resumptionToken == null || resumptionToken.length() == 0) {
                break;
            }
            response = doOAIQuery("?verb=ListRecords&resumptionToken=" + resumptionToken);
        }
        if (identifiers.size() != expected) {
            System.err.println("ERROR: Got " + identifiers.size() + " records: expected " + expected);
        }
    }

    private void listSets() throws IOException, ProtocolException {
        List<String> sets = new ArrayList<String>();
        String response = doOAIQuery("?verb=ListSets");
        while (true) {
            if (!checkForOAIPMHError(response, null)) {
                break;
            }
            extractSets(response, sets);
            String resumptionToken = extractResumptionToken(response);
            if (resumptionToken == null || resumptionToken.length() == 0) {
                break;
            }
            response = doOAIQuery("?verb=ListSets&resumptionToken=" + resumptionToken);
        }
        if (sets.size() != 2) {
            System.err.println("ERROR: Got " + sets.size() + " records: expected 2");
        }
    }

    private void extractIdentifiers(String response, List<String> identifiers) {
        int pos = 0;
        while ((pos = response.indexOf("<identifier>", pos)) >= 0) {
            int pos2 = response.indexOf("</identifier>", pos);
            if (pos2 == -1) {
                System.err.println("ERROR: No matching </identifier>");
            }
            identifiers.add(response.substring(pos, pos2));
            pos = pos2;
        }
    }

    private void extractSets(String response, List<String> sets) {
        int pos = 0;
        while ((pos = response.indexOf("<setSpec>", pos)) >= 0) {
            int pos2 = response.indexOf("</setSpec>", pos);
            if (pos2 == -1) {
                System.err.println("ERROR: No matching </setSpec>");
            }
            sets.add(response.substring(pos, pos2));
            pos = pos2;
        }
    }

    private String extractResumptionToken(String response) {
        return extract(response, "<resumptionToken[^>]*>(.*)</resumptionToken>");
    }

    private String extractAnnotationURL(String rdf) {
        return extract(rdf, "<[a-z0-9.]+:Description [a-z0-9.]+:about=\"([^\"]*)\">");
    }

    private String extractBodyURL(String rdf) {
        return extract(rdf, "<[a-z0-9.]+:body [a-z0-9.]+:resource=\"([^\"]*)\"/>");
    }

    private String extract(String rdf, String regex) {
        Matcher m = Pattern.compile(regex).matcher(rdf);
        if (m.find()) {
            return m.group(1);
        } else {
            if (isDebug()) {
                System.err.println("extract failed: regex = '" + regex + "', rdf = '" + rdf + "'");
            }
            return null;
        }
    }

    private String extractRecordId(String url, String annoteaURL) {
        String tmp = annoteaURL + "/";
        return (url.startsWith(tmp)) ? url.substring(tmp.length()) : null;
    }

    private void getRecord(String recordId, String prefix, String expectedError) throws IOException, ProtocolException {
        String response = doOAIQuery("?verb=GetRecord&metadataPrefix=" + prefix + "&identifier=" + recordId);
        checkForOAIPMHError(response, expectedError);
    }

    private boolean checkForOAIPMHError(String response, String expectedError) {
        String error = extract(response, "<error code=\"(.*?)\">");
        if (error == null) {
            if (expectedError != null) {
                System.err.println("ERROR: Did not get expected error '" + expectedError + "'");
                return false;
            }
            return true;
        } else if (!error.equals(expectedError)) {
            if (expectedError == null) {
                System.err.println("ERROR: Got unexpected error '" + error + "'");
            } else {
                System.err.println("ERROR: Expected error '" + expectedError + "' but got '" + error + "'");
            }
        } else {
            return true;
        }
        System.err.println("Complete response message: ");
        System.err.println(response);
        return false;
    }

    private void bad() throws IOException, ProtocolException {
        String response = doOAIQuery("?verb=identify");
        checkForOAIPMHError(response, "badVerb");
    }

    private void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            System.err.println("ERROR: Test assertion failed");
            System.err.println("Expected: '" + expected);
            System.err.println("Actual:   '" + actual);
            throw new RuntimeException("test failed");
        }
    }

    private void identify() throws IOException, ProtocolException {
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + "<?xml-stylesheet type=\"text/xsl\" href=\"/dannodemo/oaicat.xsl\"?>" + "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" " + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + "xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">" + "<responseDate>XXXX</responseDate>" + "<request verb=\"Identify\">" + oaiUrl + "</request>" + "<Identify>" + "<repositoryName>Danno Annotea Repository</repositoryName>" + "<baseURL>" + oaiUrl + "</baseURL>" + "<protocolVersion>2.0</protocolVersion>" + "<adminEmail>mailto:scrawley@itee.uq.edu.au</adminEmail>" + "<earliestDatestamp>2007-01-01T00:00:00Z</earliestDatestamp>" + "<deletedRecord>no</deletedRecord>" + "<granularity>YYYY-MM-DDThh:mm:ssZ</granularity>" + "<compression>gzip</compression>" + "<compression>deflate</compression>" + "<description>" + "<oai-identifier xmlns=\"http://www.openarchives.org/OAI/2.0/oai-identifier\" " + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + "xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai-identifier http://www.openarchives.org/OAI/2.0/oai-identifier.xsd\">" + "<scheme>oai</scheme>" + "<repositoryIdentifier>example.net</repositoryIdentifier>" + "<delimiter>:</delimiter>" + "<sampleIdentifier>1A2B3C4D1A2B3C4F</sampleIdentifier>" + "</oai-identifier>" + "</description>\n" + "<description>" + "<toolkit xsi:schemaLocation=\"http://oai.dlib.vt.edu/OAI/metadata/toolkit " + "http://alcme.oclc.org/oaicat/toolkit.xsd\" " + "xmlns=\"http://oai.dlib.vt.edu/OAI/metadata/toolkit\">" + "<title>OCLC's OAICat Repository Framework</title>" + "<author>" + "<name>Jeffrey A. Young</name>" + "<email>jyoung@oclc.org</email>" + "<institution>OCLC</institution>" + "</author>" + "<version>1.5.57</version>" + "<toolkitIcon>http://alcme.oclc.org/oaicat/oaicat_icon.gif</toolkitIcon>" + "<URL>http://www.oclc.org/research/software/oai/cat.shtm</URL>" + "</toolkit>" + "</description>" + "</Identify>" + "</OAI-PMH>";
        assertEquals(expected, doOAIQuery("?verb=Identify"));
    }

    private String doPostRDF(String text, String target, String body) throws IOException, ProtocolException {
        String rdf = TestUtils.expand(text, target, body, xmlDatatypeFactory).toString();
        return doPostRDF(annoteaUrl, rdf);
    }

    private String doOAIQuery(String request) throws IOException, ProtocolException {
        DannoClient ac = getClient();
        HttpGet get = new HttpGet(request);
        get.setHeader("Accept", "application/xml");
        HttpResponse response = ac.execute(get);
        if (!ac.isOK()) {
            throw new DannoRequestFailureException("GET", response);
        }
        return massage(new BasicResponseHandler().handleResponse(response));
    }

    /**
     * Massage a response string to get rid of "noise" that would cause unwanted test failures.
     * @param response
     * @return
     */
    private String massage(String response) {
        StringBuffer sb = new StringBuffer(response);
        int pos1 = sb.indexOf("<responseDate>");
        if (pos1 >= 0) {
            int pos2 = sb.indexOf("</responseDate>", pos1);
            if (pos2 >= 0) {
                sb.replace(pos1 + "<responseDate>".length(), pos2, "XXXX");
            }
        }
        return sb.toString();
    }
}
