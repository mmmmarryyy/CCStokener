package ca.etsmtl.ihe.xdsitest.docsource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import ca.etsmtl.ihe.xdsitest.dcmtk.DcmtkUtil;
import ca.etsmtl.ihe.xdsitest.fileserver.FileServerPut;
import ca.etsmtl.ihe.xdsitest.profile.AppProfile;
import ca.etsmtl.ihe.xdsitest.registry.RegistryClient;
import ca.etsmtl.ihe.xdsitest.registry.message.RegistryMessage;
import ca.etsmtl.ihe.xdsitest.registry.message.RegistryMessageFactory;
import ca.etsmtl.ihe.xdsitest.util.Configuration;
import ca.etsmtl.ihe.xdsitest.util.FailedException;
import ca.etsmtl.ihe.xdsitest.util.GlobalConfiguration;
import ca.etsmtl.ihe.xdsitest.util.OidFactory;
import ca.etsmtl.ihe.xdsitest.util.ProcessWrapper;
import ca.etsmtl.ihe.xdsitest.util.Utilities;
import ca.etsmtl.ihe.xdsitest.xml.EBXMLConstants;
import ca.etsmtl.ihe.xdsitest.xml.Transform;

public class SimplePublisher {

    protected final Configuration config;

    protected final Properties publicationProperties;

    protected final File SOPInstance;

    protected final File workDirectory;

    protected final File xmlDirectory;

    protected final OidFactory oidFactory;

    protected final FileServerPut fileServerPut;

    protected final RegistryClient registryClient;

    protected final DcmtkUtil dcmtkUtil;

    protected final boolean justGenerate;

    public SimplePublisher(File workDirectory, String justGenerate) throws Exception {
        try {
            InputStream istream;
            this.justGenerate = justGenerate.equalsIgnoreCase("-k");
            this.config = GlobalConfiguration.getInstance();
            this.xmlDirectory = this.config.getFile("xdsitest.xmlDirectory");
            this.oidFactory = OidFactory.getInstance();
            this.fileServerPut = FileServerPut.getInstance();
            this.registryClient = new RegistryClient();
            this.dcmtkUtil = DcmtkUtil.getInstance();
            if (workDirectory.isDirectory()) {
                this.SOPInstance = null;
                this.workDirectory = workDirectory;
                this.publicationProperties = new Properties();
                istream = new BufferedInputStream(new FileInputStream(new File(this.workDirectory, "publication.properties")));
                try {
                    this.publicationProperties.load(istream);
                } finally {
                    istream.close();
                }
            } else {
                this.SOPInstance = workDirectory;
                this.workDirectory = new File(this.SOPInstance.getPath() + ".dir");
                this.workDirectory.mkdir();
                this.publicationProperties = null;
            }
        } catch (Exception e) {
            throw new Exception("Initialization (" + e.getMessage() + ").");
        }
    }

    public void publish(PrintStream msgStream) throws Exception {
        File dicomInstance = new File(workDirectory, "instance.xml");
        File sreportXml = new File(workDirectory, "sreport.xml");
        File sreportDicom = new File(workDirectory, "sreport.dcm");
        File documentEntry = new File(workDirectory, "document_entry.xml");
        File submissionSet = new File(workDirectory, "submission_set.xml");
        File submissionRequest = new File(workDirectory, "submission_request.xml");
        File registryResponse = new File(workDirectory, "registry_response.xml");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String creationTime = dateFormat.format(new Date());
        String oidBase = oidFactory.newOidString(GlobalConfiguration.getInstance().getString("oidroot"));
        if (publicationProperties != null) {
            msg(msgStream, ">> Fetching DICOM SOP instance");
            fetchDicomSopInstance(dicomInstance);
        } else {
            generateDicomInstanceXML(dicomInstance);
        }
        msg(msgStream, ">> Generating DICOM Key Object Selection Document");
        generateSReportXml(dicomInstance, oidBase, creationTime, config.getString("imagectn.aeTitle"), sreportXml);
        generateSReportDicom(sreportXml, sreportDicom);
        msg(msgStream, ">> Uploading DICOM Key Object Selection Document to file server");
        Properties descriptionUploadedSReport = uploadSReport(sreportDicom);
        msg(msgStream, ">> Generating Submission Request");
        generateDocumentEntry(dicomInstance, sreportXml, descriptionUploadedSReport, oidBase, creationTime, documentEntry);
        generateSubmissionSet(documentEntry, oidBase, creationTime, submissionSet);
        generateSubmissionRequest(submissionSet, documentEntry, submissionRequest);
        if (!justGenerate) {
            msg(msgStream, ">> Sending Submission Request to registry");
            String registryResponseStatus = sendSubmissionRequest(submissionRequest, sreportDicom, documentEntry, registryResponse);
            msg(msgStream, "<<<<<<<<");
            msg(msgStream, (new StringBuilder("registry response status: ")).append(registryResponseStatus).toString());
        } else {
            msg(msgStream, "<<<<<<<<");
        }
        msg(msgStream, "The generated metadata can be found in:");
        msg(msgStream, (new StringBuilder("    ")).append(submissionSet).toString());
        msg(msgStream, (new StringBuilder("    ")).append(documentEntry).toString());
    }

    protected void generateDicomInstanceXML(File dicomInstance) throws FailedException {
        String cmdarray[] = { dcmtkUtil.absoluteCommandPath("dcm2xml"), "+Xn", SOPInstance.getPath(), dicomInstance.getPath() };
        ca.etsmtl.ihe.xdsitest.util.ProcessWrapper.Result result = ProcessWrapper.executeCommand(cmdarray, dcmtkUtil.commandEnvironment, null, null, null);
        if (!result.checkExitStatusOk()) {
            throw new FailedException("Running DICOM to XML converter", result.toString());
        } else {
            return;
        }
    }

    protected void fetchDicomSopInstance(File result) throws Exception {
        try {
            URL url = new URL(this.config.getUrl("wadoserver.url").toString() + "?requestType=WADO&contentType=text/xml" + "&studyUID=" + publicationProperties.getProperty("studyUID") + "&seriesUID=" + publicationProperties.getProperty("seriesUID") + "&objectUID=" + publicationProperties.getProperty("objectUID"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            OutputStream ostream;
            InputStream istream;
            connection.setUseCaches(false);
            connection.setRequestProperty("accept", "text/xml");
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new Exception("Error fetching DICOM SOP instance (" + connection.getResponseCode() + " " + connection.getResponseMessage() + ")");
            }
            ostream = new BufferedOutputStream(new FileOutputStream(result));
            try {
                istream = connection.getInputStream();
                Utilities.copyStream(istream, ostream);
            } finally {
                ostream.close();
            }
        } catch (Exception e) {
            throw new Exception("Error fetching DICOM SOP instance.", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected void generateSReportXml(File dicomInstance, String oidBase, String creationTime, String retrieveAeTitle, File result) throws Exception {
        Map params = new HashMap();
        params.put("seriesUid", (new StringBuilder(String.valueOf(oidBase))).append(".0").toString());
        params.put("objectUid", (new StringBuilder(String.valueOf(oidBase))).append(".1").toString());
        params.put("creationDate", creationTime.substring(0, 8));
        params.put("creationTime", creationTime.substring(8));
        params.put("referencedInstanceUri", dicomInstance.toURI().toString());
        params.put("retrieveAeTitle", retrieveAeTitle);
        setParamsAndApplyTransformation(params, "xdsi/docsource/set_params_sreport.xsl", "xdsi/docsource/generate_sreport.xsl", "params_sreport.xml", result);
    }

    protected void generateSReportDicom(File sreportXml, File sreportDicom) throws Exception {
        try {
            File dcmtkInstallDir = config.getFile("dcmtk.installDirectory");
            String command[] = { (new File(dcmtkInstallDir, "bin/xml2dsr")).getPath(), sreportXml.getPath(), sreportDicom.getPath() };
            executeExternalCommand(command);
        } catch (Exception e) {
            throw new Exception("Running XML to DICOM SR converter failed.", e);
        }
    }

    protected Properties uploadSReport(File sreport) throws Exception {
        try {
            InputStream istream = new BufferedInputStream(new FileInputStream(sreport));
            return fileServerPut.put(istream, "application/dicom", "en-us");
        } catch (Exception e) {
            throw new Exception("Uploading KOSD failed.", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected void generateDocumentEntry(File dicomInstance, File sreportXml, Properties descriptionUploadedSReport, String oidBase, String creationTime, File result) throws Exception {
        Map params = new HashMap();
        params.put("dicomInstanceUri", dicomInstance.toURI().toString());
        params.put("sreportUri", sreportXml.toURI().toString());
        params.put("uniqueId", (new StringBuilder(String.valueOf(oidBase))).append(".2").toString());
        params.put("creationTime", creationTime.substring(0, 14));
        setParamsAndApplyTransformation(params, "xdsi/docsource/set_params_document_entry.xsl", "xdsi/docsource/generate_document_entry.xsl", "params_document_entry.xml", result);
    }

    @SuppressWarnings("unchecked")
    protected void generateSubmissionSet(File documentEntry, String oidBase, String creationTime, File result) throws Exception {
        Map params = new HashMap();
        params.put("documentEntryUri", documentEntry.toURI().toString());
        params.put("uniqueId", (new StringBuilder(String.valueOf(oidBase))).append(".3").toString());
        params.put("submissionTime", creationTime.substring(0, 14));
        setParamsAndApplyTransformation(params, "xdsi/docsource/set_params_submission_set.xsl", "xdsi/docsource/generate_submission_set.xsl", "params_submission_set.xml", result);
    }

    @SuppressWarnings("unchecked")
    protected void generateSubmissionRequest(File submissionSet, File documentEntry, File result) throws Exception {
        Map params = new HashMap();
        params.put("submissionSetUri", submissionSet.toURI().toString());
        params.put("documentEntryUri", documentEntry.toURI().toString());
        setParamsAndApplyTransformation(params, "xdsi/docsource/set_params_submission_request.xsl", "xdsi/docsource/generate_submission_request.xsl", "params_submission_request.xml", result);
    }

    protected String sendSubmissionRequest(File submissionRequest, File sreport, File documentEntry, File registryResponse) throws Exception {
        try {
            registryClient.setResponseFile(registryResponse);
            RegistryMessage requestMessage = RegistryMessageFactory.newInstance(AppProfile.getInstance()).createRegistryMessage();
            requestMessage.setBodyContent(submissionRequest);
            OMElement e = requestMessage.getRoot();
            AXIOMXPath xpath = EBXMLConstants.getInstance().newXPath("//ebxml_rim:ExtrinsicObject[@id]");
            List<?> result = xpath.selectNodes(e);
            String attachmentID = ((OMElement) result.get(0)).getAttributeValue(new QName("id"));
            requestMessage.addAttachment(attachmentID, sreport);
            RegistryMessage responseMessage = requestMessage.send(config.getUrl("repository.NIST.provide.url"), registryResponse);
            return responseMessage.getResponseStatus().toString();
        } catch (Exception e) {
            throw new Exception("Sending SubmissionRequest failed.", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected void setParamsAndApplyTransformation(Map params, String setParamsFilename, String generatorFilename, String paramsFilename, File resultFile) throws Exception {
        try {
            File transformationFile = new File(xmlDirectory, setParamsFilename);
            File paramsFile = new File(workDirectory, paramsFilename);
            Transform.transform(transformationFile, (File) null, paramsFile, params);
            transformationFile = new File(xmlDirectory, generatorFilename);
            Transform.transform(transformationFile, paramsFile, resultFile, null);
        } catch (Exception e) {
            throw new Exception("XSL Transformation failed.", e);
        }
    }

    protected void executeExternalCommand(String command[]) throws IOException, Exception {
        Process proc = Runtime.getRuntime().exec(command);
        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            proc.destroy();
            Thread.currentThread().interrupt();
        }
        if (proc.exitValue() != 0) {
            throw new Exception("Execution ended with status failed.");
        } else {
            return;
        }
    }

    protected void msg(PrintStream msgStream, String msg) {
        if (msgStream != null) {
            msgStream.println(msg);
        }
    }

    public static void main(String args[]) {
        try {
            SimplePublisher instance = new SimplePublisher(new File(args[args.length - 1]), args[0]);
            instance.publish(System.out);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            System.err.println(e.getMessage());
            if (cause != null) {
                System.err.println(cause);
            }
        }
    }
}
