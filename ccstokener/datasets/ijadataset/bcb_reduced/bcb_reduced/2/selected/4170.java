package org.opencms.staticexport;

import org.opencms.db.CmsPublishedResource;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.loader.I_CmsResourceLoader;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.report.I_CmsReport;
import org.opencms.util.CmsFileUtil;
import org.opencms.util.CmsRequestUtil;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.CmsUUID;
import org.opencms.workplace.CmsWorkplace;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;

/**
 * Implementation for the <code>{@link I_CmsStaticExportHandler}</code> interface.<p>
 * 
 * This handler exports all changes immediately after something is published.<p>
 * 
 * @author Michael Moossen  
 * 
 * @version $Revision: 1.29 $ 
 * 
 * @since 6.0.0 
 * 
 * @see I_CmsStaticExportHandler
 */
public class CmsAfterPublishStaticExportHandler extends A_CmsStaticExportHandler implements I_CmsStaticExportHandler {

    /** Header field set-cookie constant. */
    private static final String HEADER_FIELD_SET_COOKIE = "Set-Cookie";

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsAfterPublishStaticExportHandler.class);

    /** Request method get constant. */
    private static final String REQUEST_METHOD_GET = "GET";

    /** Request property cookie constant. */
    private static final String REQUEST_PROPERTY_COOKIE = "Cookie";

    /**
     * Does the actual static export.<p>
     *  
     * @param resources a list of CmsPublishedREsources to start the static export with
     * @param report an <code>{@link I_CmsReport}</code> instance to print output message, or <code>null</code> to write messages to the log file
     *       
     * @throws CmsException in case of errors accessing the VFS
     * @throws IOException in case of erros writing to the export output stream
     * @throws ServletException in case of errors accessing the servlet 
     */
    public void doExportAfterPublish(List resources, I_CmsReport report) throws CmsException, IOException, ServletException {
        boolean templatesFound;
        CmsObject cmsExportObject = OpenCms.initCmsObject(OpenCms.getDefaultUsers().getUserExport());
        List resourcesToExport = getRelatedResources(cmsExportObject, resources);
        templatesFound = exportNonTemplateResources(cmsExportObject, resourcesToExport, report);
        if ((templatesFound) || (!OpenCms.getStaticExportManager().getQuickPlainExport())) {
            CmsStaticExportManager manager = OpenCms.getStaticExportManager();
            Set resourceFilter = new HashSet();
            Iterator itExpRes = resourcesToExport.iterator();
            while (itExpRes.hasNext()) {
                CmsPublishedResource pubResource = (CmsPublishedResource) itExpRes.next();
                String rfsName = manager.getRfsName(cmsExportObject, pubResource.getRootPath());
                resourceFilter.add(rfsName.substring(manager.getRfsPrefixForRfsName(rfsName).length()));
            }
            long timestamp = 0;
            List publishedTemplateResources;
            boolean newTemplateLinksFound;
            int linkMode = CmsStaticExportManager.EXPORT_LINK_WITHOUT_PARAMETER;
            do {
                publishedTemplateResources = cmsExportObject.readStaticExportResources(linkMode, timestamp);
                if (publishedTemplateResources == null) {
                    break;
                }
                newTemplateLinksFound = publishedTemplateResources.size() > 0;
                if (newTemplateLinksFound) {
                    if (linkMode == CmsStaticExportManager.EXPORT_LINK_WITHOUT_PARAMETER) {
                        linkMode = CmsStaticExportManager.EXPORT_LINK_WITH_PARAMETER;
                        publishedTemplateResources.retainAll(resourceFilter);
                    } else {
                        timestamp = System.currentTimeMillis();
                        Iterator itPubTemplates = publishedTemplateResources.iterator();
                        while (itPubTemplates.hasNext()) {
                            String rfsName = (String) itPubTemplates.next();
                            if (!resourceFilter.contains(rfsName.substring(0, rfsName.lastIndexOf('_')))) {
                                itPubTemplates.remove();
                            }
                        }
                    }
                    if (publishedTemplateResources.isEmpty()) {
                        break;
                    }
                    exportTemplateResources(cmsExportObject, publishedTemplateResources, report);
                }
            } while (newTemplateLinksFound);
        }
    }

    /**
     * Returns all resources within the current OpenCms site that are not marked as internal.<p>
     * 
     * The result list contains objects of type {@link CmsPublishedResource}.<p>
     * 
     * @param cms the cms context
     * 
     * @return all resources within the current OpenCms site that are not marked as internal
     * 
     * @throws CmsException if something goes wrong
     */
    public List getAllResources(CmsObject cms) throws CmsException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(Messages.get().getBundle().key(Messages.LOG_GET_ALL_RESOURCES_0));
        }
        List vfsResources = cms.readResources("/", CmsResourceFilter.ALL.addExcludeFlags(CmsResource.FLAG_INTERNAL));
        CmsExportFolderMatcher matcher = OpenCms.getStaticExportManager().getExportFolderMatcher();
        List resources = new ArrayList(vfsResources.size());
        Iterator i = vfsResources.iterator();
        while (i.hasNext()) {
            CmsResource resource = (CmsResource) i.next();
            if (!matcher.match(resource.getRootPath())) {
                continue;
            }
            CmsPublishedResource pubRes = new CmsPublishedResource(resource);
            if (LOG.isDebugEnabled()) {
                LOG.debug(Messages.get().getBundle().key(Messages.LOG_PROCESSING_1, resource.getRootPath()));
            }
            resources.add(pubRes);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(Messages.get().getBundle().key(Messages.LOG_NUM_RESOURCES_1, new Integer(resources.size())));
        }
        return resources;
    }

    /**
     * @see org.opencms.staticexport.I_CmsStaticExportHandler#performEventPublishProject(org.opencms.util.CmsUUID, org.opencms.report.I_CmsReport)
     */
    public void performEventPublishProject(CmsUUID publishHistoryId, I_CmsReport report) {
        try {
            m_busy = true;
            exportAfterPublish(publishHistoryId, report);
        } catch (Throwable t) {
            if (LOG.isErrorEnabled()) {
                LOG.error(Messages.get().getBundle().key(Messages.LOG_STATIC_EXPORT_ERROR_0), t);
            }
            if (report != null) {
                report.addError(t);
            }
        } finally {
            m_busy = false;
        }
    }

    /**
     * Starts the static export on publish.<p>
     * 
     * Exports all modified resources after a publish process into the real FS.<p>
     * 
     * @param publishHistoryId the publichHistoryId of the published project
     * @param report an <code>{@link I_CmsReport}</code> instance to print output message, or <code>null</code> to write messages to the log file   
     *  
     * @throws CmsException in case of errors accessing the VFS
     * @throws IOException in case of erros writing to the export output stream
     * @throws ServletException in case of errors accessing the servlet 
     */
    protected void exportAfterPublish(CmsUUID publishHistoryId, I_CmsReport report) throws CmsException, IOException, ServletException {
        String rfsName = CmsFileUtil.normalizePath(OpenCms.getStaticExportManager().getExportPath(OpenCms.getStaticExportManager().getTestResource()) + OpenCms.getStaticExportManager().getTestResource());
        if (LOG.isDebugEnabled()) {
            LOG.debug(Messages.get().getBundle().key(Messages.LOG_CHECKING_TEST_RESOURCE_1, rfsName));
        }
        File file = new File(rfsName);
        if (!file.exists()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(Messages.get().getBundle().key(Messages.LOG_TEST_RESOURCE_NOT_EXISTANT_0));
            }
            OpenCms.getStaticExportManager().exportFullStaticRender(true, report);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug(Messages.get().getBundle().key(Messages.LOG_TEST_RESOURCE_EXISTS_0));
            }
            List publishedResources = scrubExportFolders(publishHistoryId);
            doExportAfterPublish(publishedResources, report);
        }
    }

    /**
     * Exports all non template resources found in a list of published resources.<p>
     * 
     * @param cms the current cms object
     * @param publishedResources the list of published resources
     * @param report an I_CmsReport instance to print output message, or null to write messages to the log file
     * 
     * @return true if some template resources were found while looping the list of published resources
     * 
     * @throws CmsException in case of errors accessing the VFS
     * @throws IOException in case of errors writing to the export output stream
     * @throws ServletException in case of errors accessing the servlet 
     */
    protected boolean exportNonTemplateResources(CmsObject cms, List publishedResources, I_CmsReport report) throws CmsException, IOException, ServletException {
        report.println(Messages.get().container(Messages.RPT_STATICEXPORT_NONTEMPLATE_RESOURCES_BEGIN_0), I_CmsReport.FORMAT_HEADLINE);
        if (LOG.isDebugEnabled()) {
            LOG.debug(Messages.get().getBundle().key(Messages.LOG_EXPORTING_NON_TEMPLATE_1, new Integer(publishedResources.size())));
        }
        CmsStaticExportManager manager = OpenCms.getStaticExportManager();
        List resourcesToExport = new ArrayList();
        boolean templatesFound = readNonTemplateResourcesToExport(cms, publishedResources, resourcesToExport);
        int count = 1;
        int size = resourcesToExport.size();
        if (LOG.isDebugEnabled()) {
            LOG.debug(Messages.get().getBundle().key(Messages.LOG_NUM_EXPORT_1, new Integer(size)));
        }
        Iterator i = resourcesToExport.iterator();
        while (i.hasNext()) {
            CmsStaticExportData exportData = (CmsStaticExportData) i.next();
            if (LOG.isDebugEnabled()) {
                LOG.debug(Messages.get().getBundle().key(Messages.LOG_EXPORT_FILE_2, exportData.getVfsName(), exportData.getRfsName()));
            }
            report.print(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_SUCCESSION_2, new Integer(count++), new Integer(size)), I_CmsReport.FORMAT_NOTE);
            report.print(Messages.get().container(Messages.RPT_EXPORTING_0), I_CmsReport.FORMAT_NOTE);
            report.print(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_ARGUMENT_1, exportData.getVfsName()));
            report.print(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_DOTS_0));
            int status = manager.export(null, null, cms, exportData);
            if (status == HttpServletResponse.SC_OK) {
                report.println(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_OK_0), I_CmsReport.FORMAT_OK);
            } else {
                report.println(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_IGNORED_0), I_CmsReport.FORMAT_NOTE);
            }
            if (LOG.isInfoEnabled()) {
                Object[] arguments = new Object[] { exportData.getVfsName(), exportData.getRfsName(), new Integer(status) };
                LOG.info(Messages.get().getBundle().key(Messages.LOG_EXPORT_FILE_STATUS_3, arguments));
            }
            Thread.yield();
        }
        resourcesToExport = null;
        report.println(Messages.get().container(Messages.RPT_STATICEXPORT_NONTEMPLATE_RESOURCES_END_0), I_CmsReport.FORMAT_HEADLINE);
        return templatesFound;
    }

    /**
     * Exports a single (template) resource specified by its vfsName and rsfName.<p>
     * 
     * @param vfsName the vfsName of the resource
     * @param rfsName the target rfs name
     * @param cookies cookies to keep the session
     * 
     * @return the status of the http request used to perform the export
     * 
     * @throws IOException if the http request fails
     */
    protected int exportTemplateResource(String rfsName, String vfsName, StringBuffer cookies) throws IOException {
        CmsStaticExportManager manager = OpenCms.getStaticExportManager();
        String exportUrlStr = manager.getExportUrl() + manager.getRfsPrefix(vfsName) + rfsName;
        if (LOG.isDebugEnabled()) {
            LOG.debug(Messages.get().getBundle().key(Messages.LOG_SENDING_REQUEST_2, rfsName, exportUrlStr));
        }
        URL exportUrl = new URL(exportUrlStr);
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection urlcon = (HttpURLConnection) exportUrl.openConnection();
        urlcon.setRequestMethod(REQUEST_METHOD_GET);
        urlcon.setRequestProperty(CmsRequestUtil.HEADER_OPENCMS_EXPORT, CmsStringUtil.TRUE);
        if (manager.getAcceptLanguageHeader() != null) {
            urlcon.setRequestProperty(CmsRequestUtil.HEADER_ACCEPT_LANGUAGE, manager.getAcceptLanguageHeader());
        } else {
            urlcon.setRequestProperty(CmsRequestUtil.HEADER_ACCEPT_LANGUAGE, manager.getDefaultAcceptLanguageHeader());
        }
        if (manager.getAcceptCharsetHeader() != null) {
            urlcon.setRequestProperty(CmsRequestUtil.HEADER_ACCEPT_CHARSET, manager.getAcceptCharsetHeader());
        } else {
            urlcon.setRequestProperty(CmsRequestUtil.HEADER_ACCEPT_CHARSET, manager.getDefaultAcceptCharsetHeader());
        }
        String exportFileName = CmsFileUtil.normalizePath(manager.getExportPath(vfsName) + rfsName);
        File exportFile = new File(exportFileName);
        long dateLastModified = exportFile.lastModified();
        if (vfsName.startsWith(CmsWorkplace.VFS_PATH_SYSTEM)) {
            Iterator it = manager.getRfsRules().iterator();
            while (it.hasNext()) {
                CmsStaticExportRfsRule rule = (CmsStaticExportRfsRule) it.next();
                if (rule.match(vfsName)) {
                    exportFileName = CmsFileUtil.normalizePath(rule.getExportPath() + rfsName);
                    exportFile = new File(exportFileName);
                    if (dateLastModified > exportFile.lastModified()) {
                        dateLastModified = exportFile.lastModified();
                    }
                }
            }
        }
        urlcon.setIfModifiedSince(dateLastModified);
        if (LOG.isDebugEnabled()) {
            LOG.debug(Messages.get().getBundle().key(Messages.LOG_IF_MODIFIED_SINCE_SET_2, exportFile.getName(), new Long((dateLastModified / 1000) * 1000)));
        }
        if (cookies.length() > 0) {
            urlcon.setRequestProperty(REQUEST_PROPERTY_COOKIE, cookies.toString());
        }
        urlcon.connect();
        int status = urlcon.getResponseCode();
        if (cookies.length() == 0) {
            cookies.append(urlcon.getHeaderField(HEADER_FIELD_SET_COOKIE));
            if (LOG.isDebugEnabled()) {
                LOG.debug(Messages.get().getBundle().key(Messages.LOG_STATICEXPORT_COOKIES_1, cookies));
            }
        }
        urlcon.disconnect();
        if (LOG.isInfoEnabled()) {
            LOG.info(Messages.get().getBundle().key(Messages.LOG_REQUEST_RESULT_3, rfsName, exportUrlStr, new Integer(status)));
        }
        return status;
    }

    /**
     * Exports all template resources found in a list of published resources.<p>
     * 
     * @param cms the cms context
     * @param publishedTemplateResources list of potential candidates to export
     * @param report an I_CmsReport instance to print output message, or null to write messages to the log file    
     */
    protected void exportTemplateResources(CmsObject cms, List publishedTemplateResources, I_CmsReport report) {
        CmsStaticExportManager manager = OpenCms.getStaticExportManager();
        int size = publishedTemplateResources.size();
        int count = 1;
        if (LOG.isDebugEnabled()) {
            LOG.debug(Messages.get().getBundle().key(Messages.LOG_EXPORT_TEMPLATES_1, new Integer(size)));
        }
        report.println(Messages.get().container(Messages.RPT_STATICEXPORT_TEMPLATE_RESOURCES_BEGIN_0), I_CmsReport.FORMAT_HEADLINE);
        StringBuffer cookies = new StringBuffer();
        Iterator i = publishedTemplateResources.iterator();
        while (i.hasNext()) {
            String rfsName = (String) i.next();
            String vfsName = manager.getVfsNameInternal(cms, rfsName);
            if (vfsName == null) {
                String rfsBaseName = rfsName;
                int pos = rfsName.lastIndexOf('_');
                if (pos >= 0) {
                    rfsBaseName = rfsName.substring(0, pos);
                }
                vfsName = manager.getVfsNameInternal(cms, rfsBaseName);
            }
            if (vfsName != null) {
                report.print(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_SUCCESSION_2, new Integer(count++), new Integer(size)), I_CmsReport.FORMAT_NOTE);
                report.print(Messages.get().container(Messages.RPT_EXPORTING_0), I_CmsReport.FORMAT_NOTE);
                report.print(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_ARGUMENT_1, rfsName));
                report.print(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_DOTS_0));
            } else {
                continue;
            }
            try {
                int status = exportTemplateResource(rfsName, vfsName, cookies);
                if (status == HttpServletResponse.SC_OK) {
                    report.println(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_OK_0), I_CmsReport.FORMAT_OK);
                } else if (status == HttpServletResponse.SC_NOT_MODIFIED) {
                    report.println(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_SKIPPED_0), I_CmsReport.FORMAT_NOTE);
                } else if (status == HttpServletResponse.SC_SEE_OTHER) {
                    report.println(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_IGNORED_0), I_CmsReport.FORMAT_NOTE);
                } else {
                    report.println(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_ARGUMENT_1, new Integer(status)), I_CmsReport.FORMAT_OK);
                }
            } catch (IOException e) {
                report.println(e);
            }
            Thread.yield();
        }
        report.println(Messages.get().container(Messages.RPT_STATICEXPORT_TEMPLATE_RESOURCES_END_0), I_CmsReport.FORMAT_HEADLINE);
    }

    /**
     * @see org.opencms.staticexport.A_CmsStaticExportHandler#getRelatedFilesToPurge(java.lang.String, java.lang.String)
     */
    protected List getRelatedFilesToPurge(String exportFileName, String vfsName) {
        return Collections.EMPTY_LIST;
    }

    /**
     * Creates a list of <code>{@link org.opencms.db.CmsPublishedResource}</code> objects containing all related resources of the VFS tree.<p>
     * 
     * If the static export has been triggered by the OpenCms workplace, publishedResources is null and all resources in the VFS tree are returned.<p>
     * If really an after publish static export is triggered, then only the related resources are returned.<p>
     *
     * @param cms the current cms object
     * @param publishedResources the list of published resources
     * 
     * @return list of CmsPulishedResource objects containing all resources of the VFS tree
     * 
     * @throws CmsException in case of errors accessing the VFS
     */
    protected List getRelatedResources(CmsObject cms, List publishedResources) throws CmsException {
        String storedSiteRoot = cms.getRequestContext().getSiteRoot();
        try {
            cms.getRequestContext().setSiteRoot("/");
            if (publishedResources == null) {
                return getAllResources(cms);
            } else {
                Map resourceMap = new HashMap();
                Iterator itPubRes = publishedResources.iterator();
                while (itPubRes.hasNext()) {
                    CmsPublishedResource pubResource = (CmsPublishedResource) itPubRes.next();
                    if (cms.existsResource(pubResource.getRootPath())) {
                        CmsResource vfsResource = cms.readResource(pubResource.getRootPath());
                        if (!vfsResource.isInternal()) {
                            Iterator itSiblings = getSiblings(cms, pubResource).iterator();
                            while (itSiblings.hasNext()) {
                                CmsPublishedResource sibling = (CmsPublishedResource) itSiblings.next();
                                resourceMap.put(sibling.getRootPath(), sibling);
                            }
                        }
                    } else {
                        resourceMap.put(pubResource.getRootPath(), pubResource);
                    }
                    boolean match = false;
                    Iterator itExportRules = OpenCms.getStaticExportManager().getExportRules().iterator();
                    while (itExportRules.hasNext()) {
                        CmsStaticExportExportRule rule = (CmsStaticExportExportRule) itExportRules.next();
                        Set relatedResources = rule.getRelatedResources(cms, pubResource);
                        if (relatedResources != null) {
                            Iterator itRelatedRes = relatedResources.iterator();
                            while (itRelatedRes.hasNext()) {
                                CmsPublishedResource relatedRes = (CmsPublishedResource) itRelatedRes.next();
                                resourceMap.put(relatedRes.getRootPath(), relatedRes);
                            }
                            match = true;
                        }
                    }
                    if (!match) {
                        return getAllResources(cms);
                    }
                }
                return new ArrayList(resourceMap.values());
            }
        } finally {
            cms.getRequestContext().setSiteRoot(storedSiteRoot);
        }
    }

    /**
     * Returns all siblings of the published resource as list of <code>CmsPublishedResource</code>.<p>
     * 
     * @param cms the cms object
     * @param pubResource the published resource
     * 
     * @return all siblings of the published resource
     * 
     * @throws CmsException if something goes wrong
     */
    protected Set getSiblings(CmsObject cms, CmsPublishedResource pubResource) throws CmsException {
        Set siblings = new HashSet();
        for (Iterator i = getSiblingsList(cms, pubResource.getRootPath()).iterator(); i.hasNext(); ) {
            String sibling = (String) i.next();
            siblings.add(new CmsPublishedResource(cms.readResource(sibling)));
        }
        return siblings;
    }

    /**
     * Returns all non template resources found in a list of published resources.<p>
     * 
     * @param cms the current cms object
     * @param publishedResources the list of published resources
     * @param resourcesToExport the list of non-template resources
     * 
     * @return <code>true</code> if some template resources were found while looping the list of published resources
     * 
     * @throws CmsException in case of errors accessing the VFS
     */
    protected boolean readNonTemplateResourcesToExport(CmsObject cms, List publishedResources, List resourcesToExport) throws CmsException {
        CmsStaticExportManager manager = OpenCms.getStaticExportManager();
        boolean templatesFound = false;
        Iterator i = publishedResources.iterator();
        while (i.hasNext()) {
            CmsPublishedResource pupRes = (CmsPublishedResource) i.next();
            String vfsName = pupRes.getRootPath();
            if (manager.getExportFolderMatcher().match(vfsName)) {
                CmsStaticExportData exportData = manager.getExportData(vfsName, cms);
                if (exportData != null) {
                    CmsResource resource = null;
                    if (pupRes.isFile()) {
                        resource = exportData.getResource();
                    } else {
                        try {
                            String defaultFileName = cms.readPropertyObject(vfsName, CmsPropertyDefinition.PROPERTY_DEFAULT_FILE, false).getValue();
                            if (defaultFileName != null) {
                                resource = cms.readResource(vfsName + defaultFileName);
                            }
                        } catch (CmsException e) {
                            for (int j = 0; j < OpenCms.getDefaultFiles().size(); j++) {
                                String tmpResourceName = vfsName + OpenCms.getDefaultFiles().get(j);
                                try {
                                    resource = cms.readResource(tmpResourceName);
                                    break;
                                } catch (CmsException e1) {
                                }
                            }
                        }
                    }
                    if ((resource != null) && resource.isFile()) {
                        I_CmsResourceLoader loader = OpenCms.getResourceManager().getLoader(resource);
                        if (!loader.isStaticExportProcessable()) {
                            if (!pupRes.getState().isDeleted()) {
                                resourcesToExport.add(exportData);
                            }
                        } else {
                            templatesFound = true;
                            cms.writeStaticExportPublishedResource(exportData.getRfsName(), CmsStaticExportManager.EXPORT_LINK_WITHOUT_PARAMETER, "", System.currentTimeMillis());
                        }
                    }
                }
            }
        }
        return templatesFound;
    }
}
