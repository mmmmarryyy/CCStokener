package edu.pitt.dbmi.odie.gapp.gwt.server.util.umls;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.SimpleTimeZone;
import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import com.allen_sauer.gwt.log.client.Log;
import edu.pitt.dbmi.odie.gapp.gwt.server.user.ODIE_ServerSideLoginInfo;
import edu.pitt.dbmi.odie.gapp.gwt.server.util.rest.ODIE_NcboRestUtils;

public class ODIE_UmlsService extends DefaultHandler {

    private static final String GET_UMLS_LICENSE_INFO_URL = "http://ncbolabs-nk2.stanford.edu:8080/nlmlicense/license/";

    private static final String COOKIE_NAME = "ncbo_umls";

    private static final String GET_UMLS_SAB_URL = "http://ncbolabs-nk2.stanford.edu:8080/umls";

    private StringBuffer accumulator;

    private String license;

    private String licenseCode;

    private String cookie;

    public static void main(String[] args) {
        ODIE_UmlsService service = new ODIE_UmlsService();
        Log.debug(service.formatTwelveHoursFromNow());
    }

    public ODIE_UmlsService() {
    }

    public ODIE_UmlsService(ODIE_ServerSideLoginInfo serverSideLoginInfo) {
    }

    public String getXmlForSab() {
        String payLoadXml = getRestfulXml(GET_UMLS_SAB_URL, "sab");
        Log.debug(payLoadXml);
        return payLoadXml;
    }

    public String getXmlForSab(String sab) {
        String payLoadXml = ODIE_NcboRestUtils.getEmptyPayload();
        if (ODIE_NcboRestUtils.notNull(sab)) {
            payLoadXml = getRestfulXml(GET_UMLS_SAB_URL + "/" + sab, "cui");
        }
        return payLoadXml;
    }

    public String getXmlForCui(String sab, String cui) {
        String payLoadXml = ODIE_NcboRestUtils.getEmptyPayload();
        if (ODIE_NcboRestUtils.notNull(sab) && ODIE_NcboRestUtils.notNull(cui)) {
            payLoadXml = getRestfulXml(GET_UMLS_SAB_URL + "/" + sab + "/" + cui, "cui");
        }
        Log.debug(payLoadXml);
        return payLoadXml;
    }

    public String getXmlForCuiParents(String sab, String cui) {
        String payLoadXml = ODIE_NcboRestUtils.getEmptyPayload();
        if (ODIE_NcboRestUtils.notNull("sab") && ODIE_NcboRestUtils.notNull("cui")) {
            payLoadXml = getRestfulXml(GET_UMLS_SAB_URL + "/" + sab + "/" + cui + "/" + "parents", "cui");
        }
        Log.debug(payLoadXml);
        return payLoadXml;
    }

    public String getXmlForCuiChildren(String sab, String cui) {
        String payLoadXml = ODIE_NcboRestUtils.getEmptyPayload();
        if (ODIE_NcboRestUtils.notNull("sab") && ODIE_NcboRestUtils.notNull("cui")) {
            payLoadXml = getRestfulXml(GET_UMLS_SAB_URL + "/" + sab + "/" + cui + "/" + "children", "cui");
        }
        Log.debug(payLoadXml);
        return payLoadXml;
    }

    private String getRestfulXml(String url, String validatingElementName) {
        return validateResponseWith(getPayLoadWithCookie(url), validatingElementName);
    }

    private String validateResponseWith(String payLoadXml, String essentialElementName) {
        boolean isValid = payLoadXml != null && payLoadXml.length() > 0;
        isValid = isValid & payLoadXml.indexOf(essentialElementName) != -1;
        return (isValid) ? payLoadXml : ODIE_NcboRestUtils.getEmptyPayload();
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getLicenseCode() {
        return licenseCode;
    }

    public void setLicenseCode(String licenseCode) {
        this.licenseCode = licenseCode;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public void fetchUmlLicense() {
        if (ODIE_NcboRestUtils.isPosLen(this.license)) {
            String fullLicenseUrl = GET_UMLS_LICENSE_INFO_URL + this.license;
            Log.debug("Fetching license ==> " + fullLicenseUrl);
            String contents = ODIE_NcboRestUtils.getPayLoad(fullLicenseUrl);
            parseXmlContents(contents);
            if (this.licenseCode != null) {
                setCookie(COOKIE_NAME + "=" + this.licenseCode + ";path=/umls;expires=" + formatTwelveHoursFromNow());
                Log.debug("Stored cookie ==> " + getCookie());
            }
        }
    }

    private void parseXmlContents(String contents) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(contents.getBytes());
            readXmlStream(bis);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    private String getPayLoadWithCookie(String url) {
        StringBuffer sb = new StringBuffer();
        if (this.cookie != null) {
            try {
                Log.debug("Requesting url ==> " + url);
                URLConnection con = new URL(url).openConnection();
                con.setDoOutput(true);
                con.addRequestProperty("Cookie", this.cookie);
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line = "";
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    private String formatTwelveHoursFromNow() {
        SimpleDateFormat formatter = new SimpleDateFormat("EEEE, dd-MMM-yyyy HH:mm:ss z");
        Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));
        formatter.setCalendar(cal);
        Date timeNow = new Date();
        Date twelveHoursFromNow = new Date(timeNow.getTime() + (12L * 60L * 60L * 1000L));
        String formattedDateTest = formatter.format(twelveHoursFromNow);
        return formattedDateTest;
    }

    private void readXmlStream(InputStream is) throws IOException, SAXException {
        XMLReader parser = new SAXParser();
        parser.setFeature("http://xml.org/sax/features/validation", false);
        parser.setContentHandler(this);
        parser.setErrorHandler(this);
        InputSource input = new InputSource(is);
        input.setEncoding("ISO-8859-1");
        parser.parse(input);
        is.close();
    }

    public void startDocument() {
        accumulator = new StringBuffer();
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public void startElement(String namespaceURL, String localName, String qname, Attributes attributes) {
        accumulator.setLength(0);
    }

    public void endElement(String namespaceURL, String localName, String qname) {
        try {
            if (localName.equals("code")) {
                this.licenseCode = accumulator.toString().trim();
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    public void endDocument() {
    }

    public void warning(SAXParseException exception) {
        Log.warn("WARNING: line " + exception.getLineNumber() + ": " + exception.getMessage());
    }

    public void error(SAXParseException exception) {
        Log.error("ERROR: line " + exception.getLineNumber() + ": " + exception.getMessage());
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        Log.fatal("FATAL: line " + exception.getLineNumber() + ": " + exception.getMessage());
        throw (exception);
    }
}
