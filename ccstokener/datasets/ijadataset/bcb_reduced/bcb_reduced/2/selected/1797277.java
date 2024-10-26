package org.tridentproject.repository.api;

import org.tridentproject.repository.fedora.mgmt.XSLTransformer;
import org.tridentproject.repository.fedora.mgmt.FedoraAPIException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.net.URL;
import java.net.MalformedURLException;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.DomRepresentation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.dom4j.DocumentException;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.SAXReader;
import org.dom4j.ProcessingInstruction;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Resource that manages a job report
 *
 */
public class JobReportResource extends BaseResource {

    private static Logger log = Logger.getLogger(JobReportResource.class);

    String strJobId;

    public JobReportResource(Context context, Request request, Response response) {
        super(context, request, response);
        this.strJobId = (String) getRequest().getAttributes().get("jobid");
        log.debug("loading job report resource");
        Reference resourceRef = getRequest().getResourceRef();
        String strReqPath = resourceRef.getPath();
        log.debug("path = " + strReqPath);
        if (strReqPath.matches(".*report.html$")) {
            getVariants().add(new Variant(MediaType.TEXT_HTML));
            log.debug("adding html support");
        } else {
            getVariants().add(new Variant(MediaType.TEXT_XML));
            log.debug("adding xml support");
        }
    }

    /**
     * Returns a representation of a job.  GetJob API method.
     */
    @Override
    public Representation getRepresentation(Variant variant) {
        Representation representation = null;
        MediaType requestMediaType = variant.getMediaType();
        if (MediaType.TEXT_XML.equals(requestMediaType)) {
            try {
                representation = new DomRepresentation(MediaType.TEXT_XML);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        String strJobsUrl = ((ResourceApplication) getApplication()).getJobsUrl();
        org.dom4j.Document reportDoc = null;
        try {
            SAXReader reader = new SAXReader();
            reportDoc = reader.read(strJobsUrl + "/" + strJobId + "/report.xml");
        } catch (DocumentException e) {
            String strErrMsg = "Unable to find job, " + strJobId + ": " + e.getMessage();
            log.debug(strErrMsg);
            Representation rep = SetRepositoryMessage(Status.CLIENT_ERROR_NOT_FOUND, null, "JobNotFound", strErrMsg, null);
            return rep;
        }
        if (MediaType.TEXT_XML.equals(requestMediaType)) {
            log.debug("XML Media Type requested");
            try {
                reportDoc.removeProcessingInstruction("xml-stylesheet");
                DOMWriter writer = new DOMWriter();
                Document doc = writer.write(reportDoc);
                doc.normalizeDocument();
                ((DomRepresentation) representation).setDocument(doc);
                return representation;
            } catch (DocumentException e) {
                String strErrMsg = "Error display job's report: " + e.getMessage();
                log.debug(strErrMsg);
                Representation rep = SetRepositoryMessage(Status.CLIENT_ERROR_CONFLICT, null, "ClientConflict", strErrMsg, null);
                return rep;
            }
        } else if (MediaType.TEXT_HTML.equals(requestMediaType)) {
            log.debug("HTML Media Type requested");
            try {
                ProcessingInstruction pi = reportDoc.processingInstruction("xml-stylesheet");
                if (pi != null) {
                    String strXslHref = pi.getValue("href");
                    URL url = new URL(strXslHref);
                    XSLTransformer reportDocTransformer = new XSLTransformer();
                    log.debug("constructed the reportDocTransformer");
                    reportDoc = reportDocTransformer.transform(reportDoc, url.openStream());
                }
            } catch (MalformedURLException e) {
                String strErrMsg = "Error accessing referenced XSL-STYLESHEET: " + e.getMessage();
                log.debug(strErrMsg);
                Representation rep = SetRepositoryMessage(Status.SERVER_ERROR_INTERNAL, null, "InternalError", strErrMsg, null);
                return rep;
            } catch (IOException e) {
                String strErrMsg = "Error accessing referenced XSL-STYLESHEET: " + e.getMessage();
                log.debug(strErrMsg);
                Representation rep = SetRepositoryMessage(Status.SERVER_ERROR_INTERNAL, null, "InternalError", strErrMsg, null);
                return rep;
            } catch (FedoraAPIException e) {
                String strErrMsg = "Error accessing referenced XSL-STYLESHEET: " + e.getMessage();
                log.debug(strErrMsg);
                Representation rep = SetRepositoryMessage(Status.SERVER_ERROR_INTERNAL, null, "InternalError", strErrMsg, null);
                return rep;
            }
            representation = new StringRepresentation(reportDoc.asXML(), MediaType.TEXT_HTML);
            return representation;
        }
        return null;
    }
}
