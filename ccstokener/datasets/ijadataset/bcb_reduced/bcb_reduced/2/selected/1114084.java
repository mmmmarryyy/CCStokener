package org.opencms.relations;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.types.CmsResourceTypePointer;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.report.CmsLogReport;
import org.opencms.report.I_CmsReport;
import org.opencms.scheduler.I_CmsScheduledJob;
import org.opencms.util.CmsUriSplitter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Class to validate pointer links.<p>
 * 
 * @author Jan Baudisch 
 * 
 * @version $Revision: 1.8 $ 
 * 
 * @since 6.0.0 
 */
public class CmsExternalLinksValidator implements I_CmsScheduledJob {

    /** The report for the output. */
    private I_CmsReport m_report;

    /**
     * Checks if the given url is valid.<p>
     *
     * @param check the url to check
     * @param cms a OpenCms context object
     * 
     * @return false if the url could not be accessed
     */
    public static boolean checkUrl(CmsObject cms, String check) {
        URI uri = null;
        try {
            uri = new CmsUriSplitter(check, true).toURI();
        } catch (URISyntaxException exc) {
            return false;
        }
        try {
            if (!uri.isAbsolute()) {
                return cms.existsResource(cms.getRequestContext().removeSiteRoot(uri.getPath()));
            } else {
                URL url = uri.toURL();
                if ("http".equals(url.getProtocol())) {
                    HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
                    return (httpcon.getResponseCode() == 200);
                } else {
                    return true;
                }
            }
        } catch (MalformedURLException mue) {
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * This method is called by the cron scheduler.<p>
     * 
     * @param cms a OpenCms context object
     * @param parameters link check parameters
     * @return the String that is written to the OpenCms log
     * @throws CmsException if something goes wrong 
     */
    public String launch(CmsObject cms, Map parameters) throws CmsException {
        if (Boolean.valueOf((String) parameters.get("writeLog")).booleanValue()) {
            m_report = new CmsLogReport(cms.getRequestContext().getLocale(), CmsExternalLinksValidator.class);
        }
        validateLinks(cms);
        return "CmsExternLinkValidator.launch(): Links checked.";
    }

    /** 
     * Sets the report for the output.<p>
     * 
     * @param report the report for the output
     */
    public void setReport(I_CmsReport report) {
        m_report = report;
    }

    /**
     * Validate all links.<p>
     * 
     * @param cms a OpenCms context object
     * 
     * @throws CmsException if something goes wrong 
     */
    public void validateLinks(CmsObject cms) throws CmsException {
        if (m_report == null) {
            m_report = new CmsLogReport(cms.getRequestContext().getLocale(), CmsExternalLinksValidator.class);
        }
        m_report.println(Messages.get().container(Messages.RPT_VALIDATE_EXTERNAL_LINKS_BEGIN_0), I_CmsReport.FORMAT_HEADLINE);
        List links = cms.readResources("/", CmsResourceFilter.ONLY_VISIBLE_NO_DELETED.addRequireType(CmsResourceTypePointer.getStaticTypeId()));
        Iterator iterator = links.iterator();
        Map brokenLinks = new HashMap();
        for (int i = 1; iterator.hasNext(); i++) {
            CmsFile link = cms.readFile(cms.getSitePath((CmsResource) iterator.next()));
            String linkUrl = new String(link.getContents());
            m_report.print(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_SUCCESSION_1, new Integer(i), new Integer(links.size())), I_CmsReport.FORMAT_NOTE);
            m_report.print(Messages.get().container(Messages.RPT_VALIDATE_LINK_0), I_CmsReport.FORMAT_NOTE);
            m_report.print(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_ARGUMENT_1, link.getRootPath()));
            m_report.print(Messages.get().container(Messages.GUI_LINK_POINTING_TO_0), I_CmsReport.FORMAT_NOTE);
            m_report.print(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_ARGUMENT_1, linkUrl));
            m_report.print(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_DOTS_0));
            if (!checkUrl(cms, linkUrl)) {
                brokenLinks.put(link.getRootPath(), linkUrl);
                m_report.println(Messages.get().container(Messages.RPT_BROKEN_0), I_CmsReport.FORMAT_ERROR);
            } else {
                m_report.println(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_OK_0), I_CmsReport.FORMAT_OK);
            }
        }
        m_report.println(Messages.get().container(Messages.RPT_LINK_VALIDATION_STAT_2, new Integer(links.size()), new Integer(brokenLinks.size())), I_CmsReport.FORMAT_HEADLINE);
        m_report.println(Messages.get().container(Messages.RPT_VALIDATE_EXTERNAL_LINKS_END_0), I_CmsReport.FORMAT_HEADLINE);
        OpenCms.getLinkManager().setPointerLinkValidationResult(new CmsExternalLinksValidationResult(brokenLinks));
    }
}
