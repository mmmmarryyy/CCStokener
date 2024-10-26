package uk.ac.ebi.imex.psivalidator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URL;
import java.util.Collection;
import java.util.ArrayList;
import psidev.psi.mi.validator.util.UserPreferences;
import psidev.psi.mi.validator.extensions.mi25.Mi25Validator;
import psidev.psi.mi.validator.framework.ValidatorMessage;
import psidev.psi.mi.validator.framework.ValidatorException;
import psidev.psi.mi.validator.framework.MessageLevel;
import uk.ac.ebi.intact.util.psivalidator.PsiValidatorReport;
import uk.ac.ebi.intact.util.psivalidator.PsiValidator;
import uk.ac.ebi.intact.util.psivalidator.PsiValidatorMessage;

/**
 * This class is the responsible of reading and validating the PSI File and creating a validation report
 * with the information found
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id: PsiReportBuilder.java 6474 2006-10-20 15:24:44Z baranda $
 * @since <pre>12-Jun-2006</pre>
 */
public class PsiReportBuilder {

    /**
     * Logging, logging!
     */
    private static final Log log = LogFactory.getLog(PsiReportBuilder.class);

    /**
     * Name of the PSI gile
     */
    private String name;

    /**
     * If using a URL, the URL
     */
    private URL url;

    /**
     * If using an InputStream, this value won't be null
     */
    private InputStream inputStream;

    /**
     * Handy enumeration to avoid null checks on the previous attributes, when trying to determine
     * whether the info comes from URL or a stream
     */
    private enum SourceType {

        URL, INPUT_STREAM
    }

    /**
     * The current type used
     */
    private SourceType currentSourceType;

    /**
     * Creates a PsiReportBuilder instance using an URL
     * @param name The name of the file, only needed for information purposes
     * @param url The URL with the PSI xml
     */
    public PsiReportBuilder(String name, URL url) {
        this.name = name;
        this.url = url;
        this.currentSourceType = SourceType.URL;
    }

    /**
     * Creates a PsiReportBuilder instance using an InputStream
     * @param name The name of the file, only needed for information purposes
     * @param resettableInputStream The stream with the PSI xml. This InputStream has to be
     * resettable in order to build the report properly. The stream will be reset a few times, so the information
     * is parsed in the different validation phases
     */
    public PsiReportBuilder(String name, InputStream resettableInputStream) {
        this.name = name;
        this.inputStream = resettableInputStream;
        this.currentSourceType = SourceType.INPUT_STREAM;
    }

    /**
     * Creates the PSI report
     * @return the report created, after all the validations
     * @throws IOException thrown if there is something wrong with the I/O stuff
     */
    public PsiReport createPsiReport() throws IOException {
        PsiReport report = new PsiReport(name);
        boolean xmlValid = validateXmlSyntax(report, getInputStream());
        if (xmlValid) {
            createHtmlView(report, getInputStream());
            validatePsiFileSemantics(report, getInputStream());
        } else {
            report.setSemanticsStatus("not checked, XML syntax needs to be valid first");
        }
        return report;
    }

    /**
     * Returns the InputStream, independently of the origin of the information
     * @return the stream with the info
     * @throws IOException throw when there are I/O problems
     */
    private InputStream getInputStream() throws IOException {
        if (currentSourceType == SourceType.URL) {
            return url.openStream();
        } else {
            inputStream.reset();
            return inputStream;
        }
    }

    /**
     * This methods validates the xml syntax of the document
     * @param report An instance of the report being created where the validation information will be set
     * @param is The stream with the PSI xml
     * @return returns true if the validation has been successfull
     * @throws IOException
     */
    private static boolean validateXmlSyntax(PsiReport report, InputStream is) throws IOException {
        PsiValidatorReport validationReport = PsiValidator.validate(new InputSource(is));
        boolean xmlValid = validationReport.isValid();
        StringWriter sw = new StringWriter();
        if (xmlValid) {
            report.setXmlSyntaxStatus("valid");
            report.setXmlSyntaxReport("Document is valid");
        } else {
            report.setXmlSyntaxStatus("invalid");
            report.setXmlSyntaxReport(getOutputFromReport(validationReport));
        }
        return xmlValid;
    }

    private static String getOutputFromReport(PsiValidatorReport report) {
        StringBuffer sb = new StringBuffer(128);
        for (PsiValidatorMessage message : report.getMessages()) {
            sb.append(message.toString() + "\n");
        }
        return sb.toString();
    }

    /**
     * Creates the HTML view using Xstl Transformation, and sets it to the report
     * @param report The report to set the view
     * @param is The input stream with the PSI XML file
     */
    private static void createHtmlView(PsiReport report, InputStream is) {
        String transformedOutput = null;
        try {
            transformedOutput = TransformationUtil.transformToHtml(is).toString();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        report.setHtmlView(transformedOutput);
    }

    /**
     * Validates the PSI Semantics
     * @param report the PsiReport to complete
     * @param is the stream with the psi xml file
     */
    private static void validatePsiFileSemantics(PsiReport report, InputStream is) {
        StringWriter sw = new StringWriter();
        PrintWriter writer = new PrintWriter(sw);
        try {
            String expandedFile = TransformationUtil.transformToExpanded(is).toString();
            InputStream expandedStream = new ByteArrayInputStream(expandedFile.getBytes());
            InputStream configFile = PsiReportBuilder.class.getResourceAsStream("resource/config-mi-validator.xml");
            UserPreferences preferences = new UserPreferences();
            preferences.setKeepDownloadedOntologiesOnDisk(true);
            preferences.setWorkDirectory(new File(System.getProperty("java.io.tmpdir")));
            preferences.setSaxValidationEnabled(false);
            psidev.psi.mi.validator.framework.Validator validator = new Mi25Validator(configFile, preferences);
            Collection<ValidatorMessage> messages = validator.validate(expandedStream);
            report.setValidatorMessages(new ArrayList<ValidatorMessage>(messages));
        } catch (Exception e) {
            e.printStackTrace(writer);
        }
        String output = sw.getBuffer().toString();
        if (!output.equals("")) {
            report.setSemanticsStatus("invalid");
            report.setSemanticsReport(output);
            return;
        }
        String status = "valid";
        report.setSemanticsReport("Document is valid");
        for (ValidatorMessage message : report.getValidatorMessages()) {
            if (message.getLevel() == MessageLevel.WARN) {
                status = "warnings";
                report.setSemanticsReport("Validated with warnings");
            }
            if (message.getLevel().isHigher(MessageLevel.WARN)) {
                status = "invalid";
                report.setSemanticsReport("Validation failed");
                break;
            }
        }
        report.setSemanticsStatus(status);
    }
}
