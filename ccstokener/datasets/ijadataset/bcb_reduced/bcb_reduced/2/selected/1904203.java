package cn.langhua.opencms.ofbiz.rmi.content.data;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.logging.Log;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import cn.langhua.opencms.ofbiz.rmi.CmsOFBizRemoteClient;
import cn.langhua.opencms.ofbiz.rmi.base.util.UtilProperties;
import cn.langhua.opencms.ofbiz.rmi.content.email.NotificationServices;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.ByteWrapper;
import org.ofbiz.service.GenericServiceException;
import org.opencms.main.CmsLog;
import javolution.util.FastMap;

/**
 * DataResourceWorker Class
 */
public class DataResourceWorker {

    public static final String module = DataResourceWorker.class.getName();

    public static final String err_resource = "ContentErrorUiLabel";

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(DataResourceWorker.class);

    /**
     * Traverses the DataCategory parent/child structure and put it in categoryNode. Returns non-null error string if there is an error.
     * @param depth The place on the categoryTypesIds to start collecting.
     * @param getAll Indicates that all descendants are to be gotten. Used as "true" to populate an
     *     indented select list.
     */
    public static String getDataCategoryMap(CmsOFBizRemoteClient remoteClient, int depth, Map categoryNode, List categoryTypeIds, boolean getAll) throws GenericEntityException {
        String errorMsg = null;
        String parentCategoryId = (String) categoryNode.get("id");
        String currentDataCategoryId = null;
        int sz = categoryTypeIds.size();
        if (depth >= 0 && (sz - depth) > 0) {
            currentDataCategoryId = (String) categoryTypeIds.get(sz - depth - 1);
        }
        String matchValue = null;
        if (parentCategoryId != null) {
            matchValue = parentCategoryId;
        } else {
            matchValue = null;
        }
        List categoryValues = remoteClient.findByAndCache("DataCategory", UtilMisc.toMap("parentCategoryId", matchValue));
        categoryNode.put("count", new Integer(categoryValues.size()));
        List subCategoryIds = new ArrayList();
        for (int i = 0; i < categoryValues.size(); i++) {
            GenericValue category = (GenericValue) categoryValues.get(i);
            String id = (String) category.getAllFields().get("dataCategoryId");
            String categoryName = (String) category.getAllFields().get("categoryName");
            Map newNode = new HashMap();
            newNode.put("id", id);
            newNode.put("name", categoryName);
            errorMsg = getDataCategoryMap(remoteClient, depth + 1, newNode, categoryTypeIds, getAll);
            if (errorMsg != null) break;
            subCategoryIds.add(newNode);
        }
        if (parentCategoryId == null || parentCategoryId.equals("ROOT") || (currentDataCategoryId != null && currentDataCategoryId.equals(parentCategoryId)) || getAll) {
            categoryNode.put("kids", subCategoryIds);
        }
        return errorMsg;
    }

    /**
     * Finds the parents of DataCategory entity and puts them in a list, the start entity at the top.
     */
    public static void getDataCategoryAncestry(CmsOFBizRemoteClient remoteClient, String dataCategoryId, List categoryTypeIds) throws GenericEntityException {
        categoryTypeIds.add(dataCategoryId);
        GenericValue dataCategoryValue = remoteClient.findByPrimaryKey("DataCategory", UtilMisc.toMap("dataCategoryId", dataCategoryId));
        if (dataCategoryValue == null) return;
        String parentCategoryId = (String) dataCategoryValue.getAllFields().get("parentCategoryId");
        if (parentCategoryId != null) {
            getDataCategoryAncestry(remoteClient, parentCategoryId, categoryTypeIds);
        }
    }

    /**
     * Takes a DataCategory structure and builds a list of maps, one value (id) is the dataCategoryId value and the other is an indented string suitable for
     * use in a drop-down pick list.
     */
    public static void buildList(HashMap nd, List lst, int depth) {
        String id = (String) nd.get("id");
        String nm = (String) nd.get("name");
        String spc = "";
        for (int i = 0; i < depth; i++) spc += "&nbsp;&nbsp;";
        HashMap map = new HashMap();
        map.put("dataCategoryId", id);
        map.put("categoryName", spc + nm);
        if (id != null && !id.equals("ROOT") && !id.equals("")) {
            lst.add(map);
        }
        List kids = (List) nd.get("kids");
        int sz = kids.size();
        for (int i = 0; i < sz; i++) {
            HashMap kidNode = (HashMap) kids.get(i);
            buildList(kidNode, lst, depth + 1);
        }
    }

    public static String getMimeTypeFromImageFileName(String imageFileName) {
        String mimeType = null;
        if (UtilValidate.isEmpty(imageFileName)) return mimeType;
        int pos = imageFileName.lastIndexOf(".");
        if (pos < 0) return mimeType;
        String suffix = imageFileName.substring(pos + 1);
        String suffixLC = suffix.toLowerCase();
        if (suffixLC.equals("jpg")) mimeType = "image/jpeg"; else mimeType = "image/" + suffixLC;
        return mimeType;
    }

    /**
     * callDataResourcePermissionCheck Formats data for a call to the checkContentPermission service.
     */
    public static String callDataResourcePermissionCheck(CmsOFBizRemoteClient remoteClient, Map context) {
        Map permResults = callDataResourcePermissionCheckResult(remoteClient, context);
        String permissionStatus = (String) permResults.get("permissionStatus");
        return permissionStatus;
    }

    /**
     * callDataResourcePermissionCheck Formats data for a call to the checkContentPermission service.
     */
    public static Map callDataResourcePermissionCheckResult(CmsOFBizRemoteClient remoteClient, Map context) {
        Map permResults = new HashMap();
        String skipPermissionCheck = (String) context.get("skipPermissionCheck");
        if (LOG.isInfoEnabled()) {
            LOG.info("in callDataResourcePermissionCheckResult, skipPermissionCheck:" + skipPermissionCheck);
        }
        if (skipPermissionCheck == null || skipPermissionCheck.length() == 0 || (!skipPermissionCheck.equalsIgnoreCase("true") && !skipPermissionCheck.equalsIgnoreCase("granted"))) {
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            Map serviceInMap = new HashMap();
            serviceInMap.put("userLogin", userLogin);
            serviceInMap.put("targetOperationList", context.get("targetOperationList"));
            serviceInMap.put("contentPurposeList", context.get("contentPurposeList"));
            serviceInMap.put("entityOperation", context.get("entityOperation"));
            String ownerContentId = (String) context.get("ownerContentId");
            if (ownerContentId != null && ownerContentId.length() > 0) {
                try {
                    GenericValue content = remoteClient.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", ownerContentId));
                    if (content != null) serviceInMap.put("currentContent", content);
                } catch (GenericEntityException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(e);
                    }
                }
            }
            try {
                permResults = remoteClient.runSync("checkContentPermission", serviceInMap);
            } catch (GenericServiceException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Problem checking permissions", e);
                }
            } catch (RemoteException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Problem checking permissions", e);
                }
            }
        } else {
            permResults.put("permissionStatus", "granted");
        }
        return permResults;
    }

    /**
     * Gets image data from ImageDataResource and returns it as a byte array.
     */
    public static byte[] acquireImage(CmsOFBizRemoteClient remoteClient, String dataResourceId) throws GenericEntityException {
        byte[] b = null;
        GenericValue dataResource = remoteClient.findByPrimaryKeyCache("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
        if (dataResource == null) return b;
        b = acquireImage(remoteClient, dataResource);
        return b;
    }

    public static byte[] acquireImage(CmsOFBizRemoteClient remoteClient, GenericValue dataResource) throws GenericEntityException {
        byte[] b = null;
        Map fields = dataResource.getAllFields();
        String dataResourceTypeId = (String) fields.get("dataResourceTypeId");
        String dataResourceId = (String) fields.get("dataResourceId");
        GenericValue imageDataResource = remoteClient.findByPrimaryKey("ImageDataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
        if (imageDataResource != null) {
            b = (byte[]) imageDataResource.getAllFields().get("imageData");
        }
        return b;
    }

    /**
     * Returns the image type.
     */
    public static String getImageType(CmsOFBizRemoteClient remoteClient, String dataResourceId) throws GenericEntityException {
        GenericValue dataResource = remoteClient.findByPrimaryKey("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
        String imageType = getImageType(remoteClient, dataResource);
        return imageType;
    }

    public static String getMimeType(CmsOFBizRemoteClient remoteClient, GenericValue dataResource) {
        String mimeTypeId = null;
        if (dataResource != null) {
            mimeTypeId = (String) dataResource.getAllFields().get("mimeTypeId");
            if (UtilValidate.isEmpty(mimeTypeId)) {
                String fileName = (String) dataResource.getAllFields().get("objectInfo");
                if (fileName != null && fileName.indexOf('.') > -1) {
                    String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1);
                    if (UtilValidate.isNotEmpty(fileExtension)) {
                        GenericValue ext = null;
                        try {
                            ext = remoteClient.findByPrimaryKey(dataResource, "FileExtension", UtilMisc.toMap("fileExtensionId", fileExtension));
                        } catch (GenericEntityException e) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug(e);
                            }
                        }
                        if (ext != null) {
                            Map fields = ext.getAllFields();
                            mimeTypeId = (String) fields.get("mimeTypeId");
                        }
                    }
                }
                if (UtilValidate.isEmpty(mimeTypeId)) {
                    mimeTypeId = "application/octet-stream";
                }
            }
        }
        return mimeTypeId;
    }

    /** @deprecated */
    public static String getImageType(CmsOFBizRemoteClient remoteClient, GenericValue dataResource) {
        String imageType = null;
        if (dataResource != null) {
            imageType = (String) dataResource.getAllFields().get("mimeTypeId");
            if (UtilValidate.isEmpty(imageType)) {
                String imageFileNameExt = null;
                String imageFileName = (String) dataResource.getAllFields().get("objectInfo");
                if (UtilValidate.isNotEmpty(imageFileName)) {
                    int pos = imageFileName.lastIndexOf(".");
                    if (pos >= 0) imageFileNameExt = imageFileName.substring(pos + 1);
                }
                imageType = "image/" + imageFileNameExt;
            }
        }
        return imageType;
    }

    public static String buildRequestPrefix(CmsOFBizRemoteClient remoteClient, Locale locale, String webSiteId, String https) {
        Map prefixValues = FastMap.newInstance();
        String prefix;
        NotificationServices.setBaseUrl(remoteClient, webSiteId, prefixValues);
        if (https != null && https.equalsIgnoreCase("true")) {
            prefix = (String) prefixValues.get("baseSecureUrl");
        } else {
            prefix = (String) prefixValues.get("baseUrl");
        }
        if (UtilValidate.isEmpty(prefix)) {
            if (https != null && https.equalsIgnoreCase("true")) {
                prefix = UtilProperties.getMessage("content", "baseSecureUrl", locale);
            } else {
                prefix = UtilProperties.getMessage("content", "baseUrl", locale);
            }
        }
        return prefix;
    }

    public static File getContentFile(String dataResourceTypeId, String objectInfo, String contextRoot) throws GeneralException, FileNotFoundException {
        File file = null;
        if (dataResourceTypeId.equals("LOCAL_FILE") || dataResourceTypeId.equals("LOCAL_FILE_BIN")) {
            file = new File(objectInfo);
            if (!file.exists()) {
                throw new FileNotFoundException("No file found: " + (objectInfo));
            }
            if (!file.isAbsolute()) {
                throw new GeneralException("File (" + objectInfo + ") is not absolute");
            }
        } else if (dataResourceTypeId.equals("OFBIZ_FILE") || dataResourceTypeId.equals("OFBIZ_FILE_BIN")) {
            String prefix = System.getProperty("ofbiz.home");
            String sep = "";
            if (objectInfo.indexOf("/") != 0 && prefix.lastIndexOf("/") != (prefix.length() - 1)) {
                sep = "/";
            }
            file = new File(prefix + sep + objectInfo);
            if (!file.exists()) {
                throw new FileNotFoundException("No file found: " + (prefix + sep + objectInfo));
            }
        } else if (dataResourceTypeId.equals("CONTEXT_FILE") || dataResourceTypeId.equals("CONTEXT_FILE_BIN")) {
            if (UtilValidate.isEmpty(contextRoot)) {
                throw new GeneralException("Cannot find CONTEXT_FILE with an empty context root!");
            }
            String sep = "";
            if (objectInfo.indexOf("/") != 0 && contextRoot.lastIndexOf("/") != (contextRoot.length() - 1)) {
                sep = "/";
            }
            file = new File(contextRoot + sep + objectInfo);
            if (!file.exists()) {
                throw new FileNotFoundException("No file found: " + (contextRoot + sep + objectInfo));
            }
        }
        return file;
    }

    public static String getDataResourceMimeType(CmsOFBizRemoteClient remoteClient, String dataResourceId, GenericValue view) throws GenericEntityException {
        String mimeType = null;
        Map fields = null;
        if (view != null) {
            fields = view.getAllFields();
            mimeType = (String) fields.get("drMimeTypeId");
        }
        if (UtilValidate.isEmpty(mimeType) && UtilValidate.isNotEmpty(dataResourceId)) {
            GenericValue dataResource = remoteClient.findByPrimaryKeyCache("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
            fields = dataResource.getAllFields();
            mimeType = (String) fields.get("mimeTypeId");
        }
        return mimeType;
    }

    public static String getDataResourceContentUploadPath() {
        String initialPath = UtilProperties.getPropertyValue("content.properties", "content.upload.path.prefix");
        double maxFiles = UtilProperties.getPropertyNumber("content.properties", "content.upload.max.files");
        if (maxFiles < 1) {
            maxFiles = 250;
        }
        String ofbizHome = System.getProperty("ofbiz.home");
        if (!initialPath.startsWith("/")) {
            initialPath = "/" + initialPath;
        }
        Comparator desc = new Comparator() {

            public int compare(Object o1, Object o2) {
                if (((Long) o1).longValue() > ((Long) o2).longValue()) {
                    return -1;
                } else if (((Long) o1).longValue() < ((Long) o2).longValue()) {
                    return 1;
                }
                return 0;
            }
        };
        String parentDir = ofbizHome + initialPath;
        File parent = new File(parentDir);
        TreeMap dirMap = new TreeMap(desc);
        if (parent.exists()) {
            File[] subs = parent.listFiles();
            for (int i = 0; i < subs.length; i++) {
                if (subs[i].isDirectory()) {
                    dirMap.put(new Long(subs[0].lastModified()), subs[i]);
                }
            }
        } else {
            boolean created = parent.mkdir();
            if (!created) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unable to create top level upload directory [" + parentDir + "].");
                }
            }
        }
        File latestDir = null;
        if (dirMap != null && dirMap.size() > 0) {
            latestDir = (File) dirMap.values().iterator().next();
            if (latestDir != null) {
                File[] dirList = latestDir.listFiles();
                if (dirList.length >= maxFiles) {
                    latestDir = makeNewDirectory(parent);
                }
            }
        } else {
            latestDir = makeNewDirectory(parent);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Directory Name : " + latestDir.getName());
        }
        return latestDir.getAbsolutePath().replace('\\', '/');
    }

    private static File makeNewDirectory(File parent) {
        File latestDir = null;
        boolean newDir = false;
        while (!newDir) {
            latestDir = new File(parent, "" + System.currentTimeMillis());
            if (!latestDir.exists()) {
                latestDir.mkdir();
                newDir = true;
            }
        }
        return latestDir;
    }

    public static String renderDataResourceAsText(CmsOFBizRemoteClient remoteClient, String dataResourceId, Map templateContext, Locale locale, String targetMimeTypeId, boolean cache) throws GeneralException, IOException {
        Writer writer = new StringWriter();
        renderDataResourceAsText(remoteClient, dataResourceId, writer, templateContext, locale, targetMimeTypeId, cache);
        return writer.toString();
    }

    public static void renderDataResourceAsText(CmsOFBizRemoteClient remoteClient, String dataResourceId, Writer out, Map templateContext, Locale locale, String targetMimeTypeId, boolean cache) throws GeneralException, IOException {
        if (dataResourceId == null) {
            throw new GeneralException("Cannot lookup data resource with for a null dataResourceId");
        }
        if (templateContext == null) {
            templateContext = FastMap.newInstance();
        }
        if (UtilValidate.isEmpty(targetMimeTypeId)) {
            targetMimeTypeId = "text/html";
        }
        if (locale == null) {
            locale = Locale.getDefault();
        }
        if (!targetMimeTypeId.startsWith("text/")) {
            throw new GeneralException("The desired mime-type is not a text type, cannot render as text: " + targetMimeTypeId);
        }
        GenericValue dataResource = null;
        if (cache) {
            dataResource = remoteClient.findByPrimaryKeyCache("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
        } else {
            dataResource = remoteClient.findByPrimaryKey("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
        }
        if (dataResource == null) {
            throw new GeneralException("No data resource object found for dataResourceId: [" + dataResourceId + "]");
        }
        Map fields = dataResource.getAllFields();
        String dataTemplateTypeId = (String) fields.get("dataTemplateTypeId");
        if (UtilValidate.isEmpty(dataTemplateTypeId) || "NONE".equals(dataTemplateTypeId)) {
            DataResourceWorker.writeDataResourceText(dataResource, targetMimeTypeId, locale, templateContext, remoteClient, out, true);
        } else {
            templateContext.put("mimeTypeId", targetMimeTypeId);
            throw new GeneralException("The dataTemplateTypeId [" + dataTemplateTypeId + "] is not yet supported");
        }
    }

    /** @deprecated */
    public static String renderDataResourceAsText(CmsOFBizRemoteClient remoteClient, String dataResourceId, Map templateContext, GenericValue view, Locale locale, String mimeTypeId) throws GeneralException, IOException {
        return renderDataResourceAsText(remoteClient, dataResourceId, templateContext, locale, mimeTypeId, false);
    }

    /** @deprecated */
    public static void renderDataResourceAsText(CmsOFBizRemoteClient remoteClient, String dataResourceId, Writer out, Map templateContext, GenericValue view, Locale locale, String targetMimeTypeId) throws GeneralException, IOException {
        renderDataResourceAsText(remoteClient, dataResourceId, out, templateContext, locale, targetMimeTypeId, false);
    }

    /** @deprecated */
    public static String renderDataResourceAsTextCache(CmsOFBizRemoteClient remoteClient, String dataResourceId, Map templateContext, GenericValue view, Locale locale, String mimeTypeId) throws GeneralException, IOException {
        return renderDataResourceAsText(remoteClient, dataResourceId, templateContext, locale, mimeTypeId, true);
    }

    /** @deprecated */
    public static void renderDataResourceAsTextCache(CmsOFBizRemoteClient remoteClient, String dataResourceId, Writer out, Map templateContext, GenericValue view, Locale locale, String targetMimeTypeId) throws GeneralException, IOException {
        renderDataResourceAsText(remoteClient, dataResourceId, out, templateContext, locale, targetMimeTypeId, true);
    }

    public static String getDataResourceText(GenericValue dataResource, String mimeTypeId, Locale locale, Map context, CmsOFBizRemoteClient remoteClient, boolean cache) throws IOException, GeneralException {
        Writer out = new StringWriter();
        writeDataResourceText(dataResource, mimeTypeId, locale, context, remoteClient, out, cache);
        return out.toString();
    }

    public static void writeDataResourceText(GenericValue dataResource, String mimeTypeId, Locale locale, Map templateContext, CmsOFBizRemoteClient remoteClient, Writer out, boolean cache) throws IOException, GeneralException {
        Map context = (Map) templateContext.get("context");
        if (context == null) {
            context = FastMap.newInstance();
        }
        String webSiteId = (String) templateContext.get("webSiteId");
        if (UtilValidate.isEmpty(webSiteId)) {
            if (context != null) webSiteId = (String) context.get("webSiteId");
        }
        String https = (String) templateContext.get("https");
        if (UtilValidate.isEmpty(https)) {
            if (context != null) https = (String) context.get("https");
        }
        Map fields = dataResource.getAllFields();
        String dataResourceId = (String) fields.get("dataResourceId");
        String dataResourceTypeId = (String) fields.get("dataResourceTypeId");
        if (UtilValidate.isEmpty(dataResourceTypeId)) {
            dataResourceTypeId = "SHORT_TEXT";
        }
        if ("SHORT_TEXT".equals(dataResourceTypeId) || "LINK".equals(dataResourceTypeId)) {
            String text = (String) fields.get("objectInfo");
            writeText(remoteClient, dataResource, text, templateContext, mimeTypeId, locale, out);
        } else if ("ELECTRONIC_TEXT".equals(dataResourceTypeId)) {
            GenericValue electronicText;
            if (cache) {
                electronicText = remoteClient.findByPrimaryKeyCache("ElectronicText", UtilMisc.toMap("dataResourceId", dataResourceId));
            } else {
                electronicText = remoteClient.findByPrimaryKey("ElectronicText", UtilMisc.toMap("dataResourceId", dataResourceId));
            }
            fields = electronicText.getAllFields();
            String text = (String) fields.get("textData");
            writeText(remoteClient, dataResource, text, templateContext, mimeTypeId, locale, out);
        } else if (dataResourceTypeId.endsWith("_OBJECT")) {
            String text = (String) fields.get("dataResourceId");
            writeText(remoteClient, dataResource, text, templateContext, mimeTypeId, locale, out);
        } else if (dataResourceTypeId.equals("URL_RESOURCE")) {
            String text = null;
            URL url = new URL((String) fields.get("objectInfo"));
            if (url.getHost() != null) {
                InputStream in = url.openStream();
                int c;
                StringWriter sw = new StringWriter();
                while ((c = in.read()) != -1) {
                    sw.write(c);
                }
                sw.close();
                text = sw.toString();
            } else {
                String prefix = DataResourceWorker.buildRequestPrefix(remoteClient, locale, webSiteId, https);
                String sep = "";
                if (url.toString().indexOf("/") != 0 && prefix.lastIndexOf("/") != (prefix.length() - 1)) {
                    sep = "/";
                }
                String fixedUrlStr = prefix + sep + url.toString();
                URL fixedUrl = new URL(fixedUrlStr);
                text = (String) fixedUrl.getContent();
            }
            out.write(text);
        } else if (dataResourceTypeId.endsWith("_FILE_BIN")) {
            writeText(remoteClient, dataResource, dataResourceId, templateContext, mimeTypeId, locale, out);
        } else if (dataResourceTypeId.endsWith("_FILE")) {
            String dataResourceMimeTypeId = (String) fields.get("mimeTypeId");
            String objectInfo = (String) fields.get("objectInfo");
            String rootDir = (String) context.get("rootDir");
            if (dataResourceMimeTypeId == null || dataResourceMimeTypeId.startsWith("text")) {
                renderFile(dataResourceTypeId, objectInfo, rootDir, out);
            } else {
                writeText(remoteClient, dataResource, dataResourceId, templateContext, mimeTypeId, locale, out);
            }
        } else {
            throw new GeneralException("The dataResourceTypeId [" + dataResourceTypeId + "] is not supported in renderDataResourceAsText");
        }
    }

    /** @deprecated */
    public static String getDataResourceTextCache(GenericValue dataResource, String mimeTypeId, Locale locale, Map context, CmsOFBizRemoteClient remoteClient) throws IOException, GeneralException {
        return getDataResourceText(dataResource, mimeTypeId, locale, context, remoteClient, true);
    }

    /** @deprecated */
    public static void writeDataResourceTextCache(GenericValue dataResource, String mimeTypeId, Locale locale, Map context, CmsOFBizRemoteClient remoteClient, Writer outWriter) throws IOException, GeneralException {
        writeDataResourceText(dataResource, mimeTypeId, locale, context, remoteClient, outWriter, true);
    }

    /** @deprecated */
    public static String getDataResourceText(GenericValue dataResource, String mimeTypeId, Locale locale, Map context, CmsOFBizRemoteClient remoteClient) throws IOException, GeneralException {
        return getDataResourceText(dataResource, mimeTypeId, locale, context, remoteClient, false);
    }

    /** @deprecated */
    public static void writeDataResourceText(GenericValue dataResource, String mimeTypeId, Locale locale, Map context, CmsOFBizRemoteClient remoteClient, Writer out) throws IOException, GeneralException {
        writeDataResourceText(dataResource, mimeTypeId, locale, context, remoteClient, out, false);
    }

    public static void writeText(CmsOFBizRemoteClient remoteClient, GenericValue dataResource, String textData, Map context, String targetMimeTypeId, Locale locale, Writer out) throws GeneralException, IOException {
        Map fields = dataResource.getAllFields();
        String dataResourceMimeTypeId = (String) fields.get("mimeTypeId");
        if (UtilValidate.isEmpty(dataResourceMimeTypeId)) {
            dataResourceMimeTypeId = "text/html";
        }
        if (UtilValidate.isEmpty(targetMimeTypeId)) {
            targetMimeTypeId = "text/html";
        }
        if (!targetMimeTypeId.startsWith("text")) {
            throw new GeneralException("Method writeText() only supports rendering text content : " + targetMimeTypeId + " is not supported");
        }
        if ("text/html".equals(targetMimeTypeId)) {
            GenericValue mimeTypeTemplate = remoteClient.findByPrimaryKeyCache("MimeTypeHtmlTemplate", UtilMisc.toMap("mimeTypeId", dataResourceMimeTypeId));
            if (mimeTypeTemplate != null && mimeTypeTemplate.getAllFields().get("templateLocation") != null) {
                Map mimeContext = FastMap.newInstance();
                mimeContext.putAll(context);
                mimeContext.put("dataResource", dataResource);
                mimeContext.put("textData", textData);
                String mimeString = DataResourceWorker.renderMimeTypeTemplate(mimeTypeTemplate, context);
                out.write(mimeString);
            } else {
                out.write(textData);
            }
        } else if ("text/plain".equals(targetMimeTypeId)) {
            out.write(textData);
        }
    }

    public static String renderMimeTypeTemplate(GenericValue mimeTypeTemplate, Map context) throws GeneralException, IOException {
        Map fields = mimeTypeTemplate.getAllFields();
        String location = (String) fields.get("templateLocation");
        StringWriter writer = new StringWriter();
        return writer.toString();
    }

    public static void renderFile(String dataResourceTypeId, String objectInfo, String rootDir, Writer out) throws GeneralException, IOException {
        if (dataResourceTypeId.equals("LOCAL_FILE")) {
            File file = new File(objectInfo);
            if (!file.isAbsolute()) {
                throw new GeneralException("File (" + objectInfo + ") is not absolute");
            }
            int c;
            FileReader in = new FileReader(file);
            while ((c = in.read()) != -1) {
                out.write(c);
            }
        } else if (dataResourceTypeId.equals("OFBIZ_FILE")) {
            String prefix = System.getProperty("ofbiz.home");
            String sep = "";
            if (objectInfo.indexOf("/") != 0 && prefix.lastIndexOf("/") != (prefix.length() - 1)) {
                sep = "/";
            }
            File file = new File(prefix + sep + objectInfo);
            int c;
            FileReader in = new FileReader(file);
            while ((c = in.read()) != -1) out.write(c);
        } else if (dataResourceTypeId.equals("CONTEXT_FILE")) {
            String prefix = rootDir;
            String sep = "";
            if (objectInfo.indexOf("/") != 0 && prefix.lastIndexOf("/") != (prefix.length() - 1)) {
                sep = "/";
            }
            File file = new File(prefix + sep + objectInfo);
            int c;
            FileReader in = null;
            try {
                in = new FileReader(file);
                String enc = in.getEncoding();
                if (LOG.isInfoEnabled()) {
                    LOG.info("in serveImage, encoding:" + enc);
                }
            } catch (FileNotFoundException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(" in renderDataResourceAsHtml(CONTEXT_FILE), in FNFexception:", e);
                }
                throw new GeneralException("Could not find context file to render", e);
            } catch (Exception e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(" in renderDataResourceAsHtml(CONTEXT_FILE), got exception:", e);
                }
            }
            while ((c = in.read()) != -1) {
                out.write(c);
            }
        }
    }

    /**
     * getDataResourceStream - gets an InputStream and Content-Length of a DataResource
     *
     * @param dataResource
     * @param https
     * @param webSiteId
     * @param locale
     * @param contextRoot
     * @return Map containing 'stream': the InputStream and 'length' a Long containing the content-length
     * @throws IOException
     * @throws GeneralException
     */
    public static Map getDataResourceStream(CmsOFBizRemoteClient remoteClient, GenericValue dataResource, String https, String webSiteId, Locale locale, String contextRoot, boolean cache) throws IOException, GeneralException {
        if (dataResource == null) {
            throw new GeneralException("Cannot stream null data resource!");
        }
        Map fields = dataResource.getAllFields();
        String dataResourceTypeId = (String) fields.get("dataResourceTypeId");
        String dataResourceId = (String) fields.get("dataResourceId");
        if (dataResourceTypeId.endsWith("_TEXT") || "LINK".equals(dataResourceTypeId)) {
            String text = "";
            if ("SHORT_TEXT".equals(dataResourceTypeId) || "LINK".equals(dataResourceTypeId)) {
                text = (String) fields.get("objectInfo");
            } else if ("ELECTRONIC_TEXT".equals(dataResourceTypeId)) {
                GenericValue electronicText;
                if (cache) {
                    electronicText = remoteClient.findByPrimaryKeyCache("ElectronicText", UtilMisc.toMap("dataResourceId", dataResourceId));
                } else {
                    electronicText = remoteClient.findByPrimaryKey("ElectronicText", UtilMisc.toMap("dataResourceId", dataResourceId));
                }
                if (electronicText != null) {
                    fields = electronicText.getAllFields();
                    text = (String) fields.get("textData");
                }
            } else {
                throw new GeneralException("Unsupported TEXT type; cannot stream");
            }
            byte[] bytes = text.getBytes();
            return UtilMisc.toMap("stream", new ByteArrayInputStream(bytes), "length", new Integer(bytes.length));
        } else if (dataResourceTypeId.endsWith("_OBJECT")) {
            byte[] bytes = new byte[0];
            GenericValue valObj;
            if ("IMAGE_OBJECT".equals(dataResourceTypeId)) {
                if (cache) {
                    valObj = remoteClient.findByPrimaryKeyCache("ImageDataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
                } else {
                    valObj = remoteClient.findByPrimaryKey("ImageDataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
                }
                if (valObj != null) {
                    bytes = (byte[]) valObj.getAllFields().get("imageData");
                }
            } else if ("VIDEO_OBJECT".equals(dataResourceTypeId)) {
                if (cache) {
                    valObj = remoteClient.findByPrimaryKeyCache("VideoDataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
                } else {
                    valObj = remoteClient.findByPrimaryKey("VideoDataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
                }
                if (valObj != null) {
                    bytes = (byte[]) valObj.getAllFields().get("videoData");
                }
            } else if ("AUDIO_OBJECT".equals(dataResourceTypeId)) {
                if (cache) {
                    valObj = remoteClient.findByPrimaryKeyCache("AudioDataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
                } else {
                    valObj = remoteClient.findByPrimaryKey("AudioDataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
                }
                if (valObj != null) {
                    bytes = (byte[]) valObj.getAllFields().get("audioData");
                }
            } else if ("OTHER_OBJECT".equals(dataResourceTypeId)) {
                if (cache) {
                    valObj = remoteClient.findByPrimaryKeyCache("OtherDataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
                } else {
                    valObj = remoteClient.findByPrimaryKey("OtherDataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
                }
                if (valObj != null) {
                    bytes = (byte[]) valObj.getAllFields().get("dataResourceContent");
                }
            } else {
                throw new GeneralException("Unsupported OBJECT type [" + dataResourceTypeId + "]; cannot stream");
            }
            return UtilMisc.toMap("stream", new ByteArrayInputStream(bytes), "length", new Long(bytes.length));
        } else if (dataResourceTypeId.endsWith("_FILE") || dataResourceTypeId.endsWith("_FILE_BIN")) {
            String objectInfo = (String) fields.get("objectInfo");
            if (UtilValidate.isNotEmpty(objectInfo)) {
                File file = DataResourceWorker.getContentFile(dataResourceTypeId, objectInfo, contextRoot);
                return UtilMisc.toMap("stream", new FileInputStream(file), "length", new Long(file.length()));
            } else {
                throw new GeneralException("No objectInfo found for FILE type [" + dataResourceTypeId + "]; cannot stream");
            }
        } else if ("URL_RESOURCE".equals(dataResourceTypeId)) {
            String objectInfo = (String) fields.get("objectInfo");
            if (UtilValidate.isNotEmpty(objectInfo)) {
                URL url = new URL(objectInfo);
                if (url.getHost() == null) {
                    String newUrl = DataResourceWorker.buildRequestPrefix(remoteClient, locale, webSiteId, https);
                    if (!newUrl.endsWith("/")) {
                        newUrl = newUrl + "/";
                    }
                    newUrl = newUrl + url.toString();
                    url = new URL(newUrl);
                }
                URLConnection con = url.openConnection();
                return UtilMisc.toMap("stream", con.getInputStream(), "length", new Long(con.getContentLength()));
            } else {
                throw new GeneralException("No objectInfo found for URL_RESOURCE type; cannot stream");
            }
        }
        throw new GeneralException("The dataResourceTypeId [" + dataResourceTypeId + "] is not supported in getDataResourceStream");
    }

    public static void streamDataResource(OutputStream os, CmsOFBizRemoteClient remoteClient, String dataResourceId, String https, String webSiteId, Locale locale, String rootDir) throws IOException, GeneralException {
        try {
            GenericValue dataResource = remoteClient.findByPrimaryKeyCache("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
            if (dataResource == null) {
                throw new GeneralException("Error in streamDataResource: DataResource with ID [" + dataResourceId + "] was not found.");
            }
            Map fields = dataResource.getAllFields();
            String dataResourceTypeId = (String) fields.get("dataResourceTypeId");
            if (UtilValidate.isEmpty(dataResourceTypeId)) {
                dataResourceTypeId = "SHORT_TEXT";
            }
            String mimeTypeId = (String) fields.get("mimeTypeId");
            if (UtilValidate.isEmpty(mimeTypeId)) {
                mimeTypeId = "text/html";
            }
            if (dataResourceTypeId.equals("SHORT_TEXT")) {
                String text = (String) fields.get("objectInfo");
                os.write(text.getBytes());
            } else if (dataResourceTypeId.equals("ELECTRONIC_TEXT")) {
                GenericValue electronicText = remoteClient.findByPrimaryKeyCache("ElectronicText", UtilMisc.toMap("dataResourceId", dataResourceId));
                if (electronicText != null) {
                    fields = electronicText.getAllFields();
                    String text = (String) fields.get("textData");
                    if (text != null) os.write(text.getBytes());
                }
            } else if (dataResourceTypeId.equals("IMAGE_OBJECT")) {
                byte[] imageBytes = acquireImage(remoteClient, dataResource);
                if (imageBytes != null) os.write(imageBytes);
            } else if (dataResourceTypeId.equals("LINK")) {
                String text = (String) fields.get("objectInfo");
                os.write(text.getBytes());
            } else if (dataResourceTypeId.equals("URL_RESOURCE")) {
                URL url = new URL((String) fields.get("objectInfo"));
                if (url.getHost() == null) {
                    String prefix = buildRequestPrefix(remoteClient, locale, webSiteId, https);
                    String sep = "";
                    if (url.toString().indexOf("/") != 0 && prefix.lastIndexOf("/") != (prefix.length() - 1)) {
                        sep = "/";
                    }
                    String s2 = prefix + sep + url.toString();
                    url = new URL(s2);
                }
                InputStream in = url.openStream();
                int c;
                while ((c = in.read()) != -1) {
                    os.write(c);
                }
            } else if (dataResourceTypeId.indexOf("_FILE") >= 0) {
                String objectInfo = (String) fields.get("objectInfo");
                File inputFile = getContentFile(dataResourceTypeId, objectInfo, rootDir);
                FileInputStream fis = new FileInputStream(inputFile);
                int c;
                while ((c = fis.read()) != -1) {
                    os.write(c);
                }
            } else {
                throw new GeneralException("The dataResourceTypeId [" + dataResourceTypeId + "] is not supported in streamDataResource");
            }
        } catch (GenericEntityException e) {
            throw new GeneralException("Error in streamDataResource", e);
        }
    }

    public static ByteWrapper getContentAsByteWrapper(CmsOFBizRemoteClient remoteClient, String dataResourceId, String https, String webSiteId, Locale locale, String rootDir) throws IOException, GeneralException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        streamDataResource(baos, remoteClient, dataResourceId, https, webSiteId, locale, rootDir);
        ByteWrapper byteWrapper = new ByteWrapper(baos.toByteArray());
        return byteWrapper;
    }
}
