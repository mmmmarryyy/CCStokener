package org.probatron.officeotron;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.probatron.officeotron.sessionstorage.ValidationSession;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.ValidationDriver;

public class ODFValidationSession extends ValidationSession {

    static Logger logger = Logger.getLogger(ODFValidationSession.class);

    private static byte[] schema10;

    private static byte[] schema11;

    private static byte[] schema12;

    private static byte[] manifest12;

    private boolean forceIs;

    private boolean checkIds;

    private boolean uses12;

    static {
        try {
            schema10 = Utils.derefUrl(new URL("http://www.oasis-open.org/committees/download.php/12571/OpenDocument-schema-v1.0-os.rng"));
            schema11 = Utils.derefUrl(new URL("http://docs.oasis-open.org/office/v1.1/OS/OpenDocument-schema-v1.1.rng"));
            schema12 = Utils.derefUrl(new URL("http://docs.oasis-open.org/office/v1.2/csprd03/OpenDocument-v1.2-csprd03-schema.rng"));
            manifest12 = Utils.derefUrl(new URL("http://docs.oasis-open.org/office/v1.2/csprd03/OpenDocument-v1.2-csprd03-manifest-schema.rng"));
        } catch (MalformedURLException e) {
            logger.fatal(e.getMessage());
        }
    }

    public ODFValidationSession(UUID uuid, OptionMap optionMap, ReportFactory reportFactory) {
        super(uuid, reportFactory);
        this.forceIs = optionMap.getBooleanOption("force-is");
        this.checkIds = optionMap.getBooleanOption("check-ids");
        logger.trace("Creating ODFValidationSession. forceIs=" + this.forceIs + "; checkIds=" + this.checkIds);
    }

    public void validate() {
        ODFPackage mft;
        try {
            mft = parseManifest();
        } catch (SAXException e) {
            getCommentary().addComment("ERROR", "The manifest cannot be parsed");
            return;
        } catch (IOException e) {
            getCommentary().addComment("ERROR", "The manifest cannot be extracted or is corrupt");
            return;
        }
        processManifestDocs(mft);
        if (uses12) {
            validateManifest();
        }
        getCommentary().addComment("Grand total count of validity errors: " + getCommentary().getErrCount());
    }

    private void validateManifest() {
        getCommentary().addComment("Validating manifest");
        getCommentary().incIndent();
        PropertyMapBuilder builder = new PropertyMapBuilder();
        CommentatingErrorHandler h = new CommentatingErrorHandler(getCommentary(), "META-INF/manifest.xml");
        ValidateProperty.ERROR_HANDLER.put(builder, h);
        ValidationDriver driver = new ValidationDriver(builder.toPropertyMap());
        InputStream candidateStream = null;
        try {
            driver.loadSchema(new InputSource(new ByteArrayInputStream(manifest12)));
            URLConnection conn = new URL(getUrlForEntry("META-INF/manifest.xml").toString()).openConnection();
            candidateStream = conn.getInputStream();
            boolean isValid = driver.validate(new InputSource(candidateStream));
            if (isValid) {
                getCommentary().addComment("Manifest is valid");
            } else {
                getCommentary().addComment("ERROR", "Manifest is invalid");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utils.streamClose(candidateStream);
        }
        getCommentary().decIndent();
    }

    private ODFPackage parseManifest() throws SAXException, IOException {
        String manifestUrl = getUrlForEntry("META-INF/manifest.xml").toString();
        ODFPackage mft = new ODFPackage();
        mft.process(manifestUrl);
        return mft;
    }

    private void processManifestDocs(ODFPackage mft) {
        for (int i = 0; i < mft.getItemRefs().size(); i++) {
            String mimeType = mft.getItemTypes().get(i);
            String entry = mft.getItemRefs().get(i);
            if (entry.endsWith("/")) {
                continue;
            }
            if (mimeType.equals("")) {
                getCommentary().addComment("WARN", "Manifest entry for \"" + entry + "\" should have a MIME type, but has an empty string");
            }
            if (mimeType.indexOf("xml") == -1) {
                if (!entry.trim().endsWith(".xml")) {
                    logger.debug("Skipping entry " + entry);
                    continue;
                }
            }
            String entryUrl = getUrlForEntry(entry).toString();
            logger.debug("processing " + entryUrl);
            getCommentary().addComment("Processing manifest entry: " + entry);
            getCommentary().incIndent();
            ODFSniffer sniffer = new ODFSniffer(getCommentary(), checkIds);
            XMLSniffData sd;
            try {
                sd = sniffer.doSniff(entryUrl);
            } catch (Exception e) {
                logger.fatal("Referenced resource in manifest cannot be found/processed");
                getCommentary().addComment("WARN", "Referenced resource in manifest cannot be found/processed");
                getCommentary().decIndent();
                continue;
            }
            if (sd.getRootNs().equals(ODFSniffer.ODF_OFFICE_NS)) {
                processODFDocument(entryUrl, sd);
            }
            logger.trace("Done document processing");
            if (sniffer.getGenerator() != "") {
                getCommentary().addComment("The generator value is: \"<b>" + sniffer.getGenerator().trim() + "</b>\"");
            }
            getCommentary().decIndent();
        }
    }

    private void processODFDocument(String entryUrl, XMLSniffData sd) {
        String ver = Utils.getQAtt(sd.getAtts(), ODFSniffer.ODF_OFFICE_NS, "version");
        logger.debug("version is " + ver);
        getCommentary().addComment("It has root element named &lt;" + sd.getRootElementName() + "> in the namespace <tt>" + sd.getRootNs() + "</tt>");
        logger.trace("beginning validation with force setting of " + this.forceIs);
        if (this.forceIs) {
            getCommentary().addComment("WARN", "Forcing validation against ISO/IEC 26300");
            ver = "1.0";
        } else {
            if (ver != null) {
                getCommentary().addComment("It claims to be ODF version " + ver);
            } else {
                getCommentary().addComment("WARN", "It has no version attribute! (assuming ODF v1.1)");
                ver = "1.1";
            }
        }
        try {
            validateODFDoc(entryUrl, ver, getCommentary());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Validates the given ODF XML document.
     * 
     * @param url
     *            the URL of the candidate
     * @param ver
     *            the version; must be "1.0", "1.1" or "1.2"
     * @param commentary
     *            where to report the validation narrative
     * @throws IOException
     * @throws MalformedURLException
     */
    private void validateODFDoc(String url, String ver, ValidationReport commentary) throws IOException, MalformedURLException {
        logger.debug("Beginning document validation ...");
        synchronized (ODFValidationSession.class) {
            PropertyMapBuilder builder = new PropertyMapBuilder();
            String[] segments = url.split("/");
            CommentatingErrorHandler h = new CommentatingErrorHandler(commentary, segments[segments.length - 1]);
            ValidateProperty.ERROR_HANDLER.put(builder, h);
            ValidationDriver driver = new ValidationDriver(builder.toPropertyMap());
            InputStream candidateStream = null;
            try {
                logger.debug("Loading schema version " + ver);
                byte[] schemaBytes = getSchemaForVersion(ver);
                driver.loadSchema(new InputSource(new ByteArrayInputStream(schemaBytes)));
                URLConnection conn = new URL(url).openConnection();
                candidateStream = conn.getInputStream();
                logger.debug("Calling validate()");
                commentary.incIndent();
                boolean isValid = driver.validate(new InputSource(candidateStream));
                logger.debug("Errors in instance:" + h.getInstanceErrCount());
                if (h.getInstanceErrCount() > CommentatingErrorHandler.THRESHOLD) {
                    commentary.addComment("(<i>" + (h.getInstanceErrCount() - CommentatingErrorHandler.THRESHOLD) + " error(s) omitted for the sake of brevity</i>)");
                }
                commentary.decIndent();
                if (isValid) {
                    commentary.addComment("The document is valid");
                } else {
                    commentary.addComment("ERROR", "The document is invalid");
                }
            } catch (SAXException e) {
                commentary.addComment("FATAL", "The resource is not conformant XML: " + e.getMessage());
                logger.error(e.getMessage());
            } finally {
                Utils.streamClose(candidateStream);
            }
        }
    }

    private byte[] getSchemaForVersion(String ver) {
        byte[] schemaBytes = null;
        if (ver.equals("1.0")) {
            schemaBytes = schema10;
        } else if (ver.equals("1.1")) {
            schemaBytes = schema11;
        } else if (ver.equals("1.2")) {
            schemaBytes = schema12;
            uses12 = true;
        } else {
            logger.fatal("No version found ...");
            return null;
        }
        return schemaBytes;
    }
}
