package com.autentia.portlet;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import com.autentia.portlet.i18n.UtilI18N;
import com.liferay.portal.NoSuchImageException;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextFactory;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portlet.imagegallery.DuplicateImageNameException;
import com.liferay.portlet.imagegallery.NoSuchFolderException;
import com.liferay.portlet.imagegallery.model.IGFolder;
import com.liferay.portlet.imagegallery.model.IGImage;
import com.liferay.portlet.imagegallery.service.IGFolderLocalServiceUtil;
import com.liferay.portlet.imagegallery.service.IGFolderServiceUtil;
import com.liferay.portlet.imagegallery.service.IGImageServiceUtil;
import com.liferay.portlet.journal.ArticleTypeException;
import com.liferay.portlet.journal.NoSuchArticleException;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;

public class ImportWebContentPortlet extends GenericPortlet {

    UtilI18N utilI18N = null;

    String locale = null;

    StringBuffer warns = new StringBuffer();

    public void init() throws PortletException {
        editJSP = getInitParameter("edit-jsp");
        helpJSP = getInitParameter("help-jsp");
        viewJSP = getInitParameter("view-jsp");
        noPermisoJSP = getInitParameter("nopermiso-jsp");
        utilI18N = new UtilI18N("com.autentia.portlet.i18n.messages");
    }

    public void doDispatch(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException {
        String jspPage = renderRequest.getParameter("jspPage");
        if (jspPage != null) {
            include(jspPage, renderRequest, renderResponse);
        } else {
            super.doDispatch(renderRequest, renderResponse);
        }
    }

    public void doEdit(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException {
        if (renderRequest.getPreferences() == null) {
            super.doEdit(renderRequest, renderResponse);
        } else {
            include(editJSP, renderRequest, renderResponse);
        }
    }

    public void doHelp(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException {
        include(helpJSP, renderRequest, renderResponse);
    }

    public void doView(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException {
        ServiceContext serviceContext;
        PrintWriter out = null;
        locale = renderRequest.getLocale().toString();
        try {
            out = renderResponse.getWriter();
            serviceContext = ServiceContextFactory.getInstance(getPortletName(), renderRequest);
            User currentUser = UserLocalServiceUtil.getUser(serviceContext.getUserId());
            if (isUserAdministrator(currentUser.getRoles())) {
                include(viewJSP, renderRequest, renderResponse);
            } else {
                include(noPermisoJSP, renderRequest, renderResponse);
            }
        } catch (PortalException e) {
            out.println("<p>" + utilI18N.getMessage("error.1", locale) + ":" + e.getMessage() + "</p>");
        } catch (SystemException e) {
            out.println("<p>" + utilI18N.getMessage("error.2", locale) + ":" + e.getMessage() + "</p>");
        }
    }

    private boolean isUserAdministrator(List<Role> listRoles) {
        boolean is = false;
        for (Role r : listRoles) {
            if ("Administrator".equals(r.getName())) {
                is = true;
                break;
            }
        }
        return is;
    }

    @SuppressWarnings("unchecked")
    public void processAction(ActionRequest actionRequest, ActionResponse actionResponse) throws IOException, PortletException {
        StringBuilder sbErrors = new StringBuilder();
        nFilesConverted = 0;
        nImages = 0;
        try {
            ServiceContext serviceContext = ServiceContextFactory.getInstance(getPortletName(), actionRequest);
            if (actionRequest.getPortletMode() == PortletMode.VIEW) {
                int idParentDirectory = 0;
                try {
                    idParentDirectory = new Integer(actionRequest.getPreferences().getValue("idParentDirectory", ""));
                } catch (NumberFormatException e) {
                    throw new SystemException(utilI18N.getMessage("validate.mustsetpreferences", locale));
                }
                String idUserLiferay = GetterUtil.getString(actionRequest.getParameter("idUserLiferay"), "" + serviceContext.getUserId());
                if ("".equals(idUserLiferay)) {
                    idUserLiferay = "" + serviceContext.getUserId();
                }
                String urlWebContent = actionRequest.getParameter("urlWebContent");
                String extWebContent = FilenameUtils.getExtension(urlWebContent);
                String typeWebContent = GetterUtil.getString(actionRequest.getParameter("type"), "");
                String tagsWebContent = GetterUtil.getString(actionRequest.getParameter("webContentTags"), "");
                String[] tags = null;
                if (!"".equals(tagsWebContent)) {
                    tags = getTags(tagsWebContent);
                }
                boolean isDirectory = extWebContent == "";
                if (isDirectory) {
                    User userOwner = UserLocalServiceUtil.getUser(new Long(idUserLiferay));
                    File dirFiles = new File(urlWebContent);
                    String[] exts = new String[] { "html", "htm", "php" };
                    Collection<File> filesAllowed = FileUtils.listFiles(dirFiles, exts, true);
                    Iterator<File> ite = filesAllowed.iterator();
                    while (ite.hasNext()) {
                        File f = ite.next();
                        URL urlFile = new URL("file://" + f.getAbsolutePath());
                        String convertedContent = replaceHTMLToWebContent(urlFile, idParentDirectory, serviceContext);
                        createWebContent(urlFile, convertedContent, typeWebContent, tags, userOwner, serviceContext);
                        nFilesConverted++;
                    }
                } else {
                    User userOwner = null;
                    try {
                        userOwner = UserLocalServiceUtil.getUser(new Long(idUserLiferay));
                    } catch (NumberFormatException e) {
                        throw new SystemException(utilI18N.getMessage("validate.wrongiduser", locale));
                    } catch (NoSuchUserException e) {
                        throw new SystemException(utilI18N.getMessage("validate.noexistsiduser", locale));
                    }
                    URL urlWC = new URL(urlWebContent);
                    String convertedContent = replaceHTMLToWebContent(urlWC, idParentDirectory, serviceContext);
                    createWebContent(urlWC, convertedContent, typeWebContent, tags, userOwner, serviceContext);
                    nFilesConverted++;
                }
                actionResponse.setRenderParameter("info", utilI18N.getMessage("info.processsucess", locale) + utilI18N.getMessage("info.totalfilesimported", locale) + ": " + nFilesConverted + "." + utilI18N.getMessage("info.totalimagesimported", locale) + ": " + nImages + "<br/>");
            } else {
                String idParentDirectory = actionRequest.getParameter("id_parent_directory");
                actionRequest.getPreferences().setValue("idParentDirectory", idParentDirectory);
                actionRequest.getPreferences().store();
                actionResponse.setRenderParameter("info", utilI18N.getMessage("info.processsucess", locale));
            }
        } catch (ArticleTypeException ate) {
            sbErrors.append(utilI18N.getMessage("error.typecontentnotsuported", locale) + "<br/>");
        } catch (PortalException e) {
            sbErrors.append(e.getMessage() + "<br/>");
        } catch (SystemException e) {
            sbErrors.append(e.getMessage() + "<br/>");
        } finally {
            actionResponse.setRenderParameter("error", sbErrors.toString());
            actionResponse.setRenderParameter("warn", warns.toString());
        }
    }

    private String[] getTags(String tagsWebContent) {
        return tagsWebContent.split(",");
    }

    private String replaceHTMLToWebContent(URL urlWebContent, int idParentDirectory, ServiceContext serviceContext) throws IOException, SystemException {
        StringBuilder sbReplaceImg = null;
        String nameWebContent = FilenameUtils.getName(urlWebContent.getFile());
        String urlPath = urlWebContent.toString();
        String urlWebContents = urlPath.replace(nameWebContent, "");
        URLConnection urlCon = urlWebContent.openConnection();
        InputStream isWebContent = urlCon.getInputStream();
        Source source = new Source(isWebContent);
        OutputDocument outputDocument = new OutputDocument(source);
        List<StartTag> imgStartTags = source.getAllStartTags(HTMLElementName.IMG);
        if ((imgStartTags != null) && (imgStartTags.size() > 0)) {
            IGFolder igFolder = null;
            try {
                igFolder = createOrGetFolder(idParentDirectory, nameWebContent.replaceFirst("." + FilenameUtils.getExtension(nameWebContent), ""), serviceContext);
            } catch (PortalException e) {
                if (e instanceof NoSuchFolderException) {
                    throw new SystemException(utilI18N.getMessage("validate.wrongidparentdir", locale));
                } else {
                    throw new SystemException(utilI18N.getMessage("error.wrongwebcontent", locale));
                }
            }
            int ite = 0;
            for (Iterator<StartTag> i = imgStartTags.iterator(); i.hasNext(); ) {
                ite++;
                StartTag startTag = i.next();
                Attributes attributes = startTag.getAttributes();
                sbReplaceImg = new StringBuilder();
                sbReplaceImg.append("<img ");
                URL urlImage = null;
                for (Iterator<Attribute> j = attributes.iterator(); j.hasNext(); ) {
                    Attribute attr = j.next();
                    if (attr != null) {
                        if ("src".equalsIgnoreCase(attr.getName())) {
                            urlImage = null;
                            String pathImage = attr.getValue();
                            if (pathImage.startsWith("http")) {
                                urlImage = new URL(pathImage);
                            } else {
                                urlImage = new URL(urlWebContents + attr.getValue());
                            }
                            IGImage igImage = null;
                            try {
                                igImage = createImage(igFolder, urlImage, ite, serviceContext);
                            } catch (FileNotFoundException e) {
                                warns.append(utilI18N.getMessage("warn.filenotfound", locale) + ": " + urlImage.toString());
                                ite--;
                                continue;
                            } catch (MalformedURLException e) {
                                warns.append(utilI18N.getMessage("warn.urlimagemalformed", locale) + ": " + urlImage.toString());
                                ite--;
                                continue;
                            }
                            sbReplaceImg.append(" ").append("src=\"/image/image_gallery?uuid=").append(igImage.getUuid()).append("&groupId=").append(igImage.getGroupId()).append("\"");
                        } else if ("style".equalsIgnoreCase(attr.getName())) {
                            sbReplaceImg.append("");
                        } else {
                            sbReplaceImg.append(" ").append(attr);
                        }
                    }
                }
                sbReplaceImg.append(">\n").append("\n</img>");
                outputDocument.replace(startTag, sbReplaceImg.toString());
                nImages++;
            }
        }
        return outputDocument.toString();
    }

    private IGFolder createOrGetFolder(int idParentFolderImage, String nameFolder, ServiceContext serviceContext) throws PortalException, SystemException {
        IGFolder igFolder = null;
        try {
            igFolder = IGFolderLocalServiceUtil.getFolder(serviceContext.getScopeGroupId(), idParentFolderImage, nameFolder);
        } catch (NoSuchFolderException nsfe) {
            igFolder = IGFolderServiceUtil.addFolder(idParentFolderImage, nameFolder, nameFolder, serviceContext);
        }
        return igFolder;
    }

    private IGImage createImage(IGFolder igFolder, URL urlImage, int order, ServiceContext serviceContext) throws FileNotFoundException, IOException, SystemException, MalformedURLException {
        _log.info(utilI18N.getMessage("info.creatingimage", locale) + ": " + urlImage.toString());
        String nameFile = FilenameUtils.getName(urlImage.getFile());
        String extImage = FilenameUtils.getExtension(nameFile).toLowerCase();
        StringBuilder nameImage = new StringBuilder();
        if (!nameFile.contains("?") && (!"".equals(extImage))) {
            try {
                nameImage.append(igFolder.getName());
                nameImage.append("_img");
                nameImage.append(order);
                nameImage.append(".");
                nameImage.append(extImage);
                IGImage igImage = IGImageServiceUtil.getImageByFolderIdAndNameWithExtension(igFolder.getFolderId(), nameImage.toString());
                return igImage;
            } catch (NoSuchImageException e) {
            } catch (PortalException e) {
            }
        }
        InputStream urlStream = urlImage.openStream();
        String extension = "";
        if (nameFile.contains("?") || ("".equals(extImage))) {
            InputStream urlStreamExt = urlImage.openStream();
            extension = getFormatName(urlStreamExt);
        } else {
            extension = extImage;
        }
        extension = extension.toLowerCase();
        if ("jpeg".equals(extension)) {
            extension = "jpg";
        }
        BufferedImage image = ImageIO.read(urlStream);
        nameImage = new StringBuilder();
        nameImage.append(igFolder.getName()).append("_img").append(order).append(".").append(extension);
        File tempFile = new File("temp/temp." + extension);
        ImageIO.write(image, extension, tempFile);
        IGImage igImage = null;
        try {
            igImage = IGImageServiceUtil.addImage(igFolder.getFolderId(), nameImage.toString(), nameImage.toString(), tempFile, extension, serviceContext);
        } catch (DuplicateImageNameException e) {
            try {
                igImage = IGImageServiceUtil.getImageByFolderIdAndNameWithExtension(igFolder.getFolderId(), nameImage.toString());
            } catch (PortalException e1) {
                throw new SystemException(e1.getMessage());
            }
        } catch (PortalException e) {
            throw new SystemException(e.getMessage());
        }
        return igImage;
    }

    private String getFormatName(Object o) {
        try {
            ImageInputStream iis = ImageIO.createImageInputStream(o);
            Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
            if (!iter.hasNext()) {
                return null;
            }
            ImageReader reader = iter.next();
            iis.close();
            return reader.getFormatName();
        } catch (IOException e) {
        }
        return null;
    }

    private JournalArticle createWebContent(URL urlWebContent, String content, String typeWebContent, String[] tags, User userOwner, ServiceContext serviceContext) throws ArticleTypeException, PortalException, SystemException {
        long userId = userOwner.getUserId();
        long groupId = serviceContext.getScopeGroupId();
        String articleId = "";
        boolean autoArticleId = true;
        String title = urlWebContent.toString();
        String description = urlWebContent.toString();
        String webContent = START_CONTENT_WEB + content + END_CONTENT_WEB;
        String type = typeWebContent;
        String structureId = "";
        String templateId = "";
        Calendar today = GregorianCalendar.getInstance();
        int displayDateMonth = today.get(Calendar.MONTH);
        int displayDateDay = today.get(Calendar.DATE);
        int displayDateYear = today.get(Calendar.YEAR);
        int displayDateHour = today.get(Calendar.HOUR - 1);
        int displayDateMinute = today.get(Calendar.MINUTE);
        int expirationDateMonth = today.get(Calendar.MONTH);
        int expirationDateDay = today.get(Calendar.DATE);
        int expirationDateYear = today.get(Calendar.YEAR);
        int expirationDateHour = today.get(Calendar.HOUR);
        int expirationDateMinute = today.get(Calendar.MINUTE);
        boolean neverExpire = true;
        int reviewDateMonth = today.get(Calendar.MONTH);
        int reviewDateDay = today.get(Calendar.DATE);
        int reviewDateYear = today.get(Calendar.YEAR);
        int reviewDateHour = today.get(Calendar.HOUR);
        int reviewDateMinute = today.get(Calendar.MINUTE);
        boolean neverReview = true;
        boolean indexable = true;
        boolean smallImage = false;
        String smallImageURL = "";
        File smallFile = null;
        Map<String, byte[]> images = null;
        String articleURL = urlWebContent.toString();
        if (tags != null) {
            serviceContext.setTagsCategories(tags);
        }
        JournalArticle journalArticle = null;
        try {
            journalArticle = JournalArticleLocalServiceUtil.getArticleByUrlTitle(groupId, articleURL);
            _log.info(utilI18N.getMessage("info.existswebconten", locale) + ": " + title);
        } catch (NoSuchArticleException e) {
            journalArticle = JournalArticleLocalServiceUtil.addArticle(userId, groupId, articleId, autoArticleId, title, description, webContent, type, structureId, templateId, displayDateMonth, displayDateDay, displayDateYear, displayDateHour, displayDateMinute, expirationDateMonth, expirationDateDay, expirationDateYear, expirationDateHour, expirationDateMinute, neverExpire, reviewDateMonth, reviewDateDay, reviewDateYear, reviewDateHour, reviewDateMinute, neverReview, indexable, smallImage, smallImageURL, smallFile, images, articleURL, serviceContext);
            journalArticle.setUrlTitle(articleURL);
            journalArticle.setApproved(true);
            journalArticle.setApprovedByUserId(userId);
            journalArticle.setCreateDate(new GregorianCalendar().getTime());
            JournalArticleLocalServiceUtil.updateJournalArticle(journalArticle, true);
            _log.info(utilI18N.getMessage("info.webcontentcreated", locale) + ": " + title);
        }
        return journalArticle;
    }

    protected void include(String path, RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException {
        PortletRequestDispatcher portletRequestDispatcher = getPortletContext().getRequestDispatcher(path);
        if (portletRequestDispatcher == null) {
            _log.error(utilI18N.getMessage("error.wrongpathinclude", locale) + ": " + path);
        } else {
            portletRequestDispatcher.include(renderRequest, renderResponse);
        }
    }

    protected String editJSP;

    protected String helpJSP;

    protected String viewJSP;

    protected String noPermisoJSP;

    private static Log _log = LogFactoryUtil.getLog(ImportWebContentPortlet.class);

    private final String START_CONTENT_WEB = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root available-locales=\"es_ES\" default-locale=\"es_ES\"><static-content language-id=\"es_ES\"><![CDATA[";

    private final String END_CONTENT_WEB = "]]></static-content></root>";

    private int nFilesConverted = 0;

    private int nImages = 0;
}
