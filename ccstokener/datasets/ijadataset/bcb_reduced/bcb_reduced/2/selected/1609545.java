package org.ffck.weblets;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Hashtable;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import net.java.dev.weblets.Weblet;
import net.java.dev.weblets.WebletConfig;
import net.java.dev.weblets.WebletException;
import net.java.dev.weblets.WebletRequest;
import net.java.dev.weblets.WebletResponse;

public class FFCKeditorWeblet extends Weblet {

    private static final Log log = LogFactory.getLog(FFCKeditorWeblet.class);

    private static final String CONNECTOR_PATTERN = "connectors/weblets/connector";

    private static final String UPLOADER_PATTERN = "filemanager/upload/simpleuploader";

    public static final String FILE_REPOSITORY_PATH_CONTEXT_PARAM = "FILE_REPOSITORY_PATH";

    public static final String FILE_REPOSITORY_PATH_CONTEXT_PARAM_2 = "repository.path";

    public static final String FILE_CONTEXT_PATH_CONTEXT_PARAM = "FILE_CONTEXT_PATH";

    public static final String FILE_CHROOT_PATH_SESSION_PARAM = "FILE_CHROOT_PATH";

    public static final String SIZE_MAX = "org.apache.commons.fileupload.SIZE_MAX";

    public static final String DENIED_EXTENSIONS_PARAMETER = "DeniedExtensions";

    public static final String ALLOWED_EXTENSIONS_IMAGE_PARAMETER = "AllowedExtensionsImage";

    public static final String ALLOWED_EXTENSIONS_FLASH_PARAMETER = "AllowedExtensionsFlash";

    public static final String ALLOWED_EXTENSIONS_MEDIA_PARAMETER = "AllowedExtensionsMedia";

    public static final String FILE_TYPE = "File";

    public static final String IMAGE_TYPE = "Image";

    public static final String FLASH_TYPE = "Flash";

    public static final String MEDIA_TYPE = "Media";

    private Connector connector = null;

    private SimpleUploader uploader = null;

    private long max_upload_size = 0L;

    private String baseDir = null;

    private String realDir = null;

    private ArrayList deniedExtensions = null;

    private Hashtable<String, ArrayList> allowedExtensions = new Hashtable<String, ArrayList>(3);

    public FFCKeditorWeblet() {
        super();
    }

    @Override
    public void destroy() {
        super.destroy();
        connector = null;
        uploader = null;
    }

    @Override
    public void init(WebletConfig conf) {
        super.init(conf);
        deniedExtensions = stringToArrayList(conf.getInitParameter(DENIED_EXTENSIONS_PARAMETER));
        allowedExtensions.put(IMAGE_TYPE, stringToArrayList(conf.getInitParameter(ALLOWED_EXTENSIONS_IMAGE_PARAMETER)));
        allowedExtensions.put(FLASH_TYPE, stringToArrayList(conf.getInitParameter(ALLOWED_EXTENSIONS_FLASH_PARAMETER)));
        allowedExtensions.put(MEDIA_TYPE, stringToArrayList(conf.getInitParameter(ALLOWED_EXTENSIONS_MEDIA_PARAMETER)));
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            ExternalContext ex_context = context.getExternalContext();
            try {
                String size_max = ex_context.getInitParameter(FFCKeditorWeblet.SIZE_MAX);
                setMaxUploadSize(Long.parseLong(size_max));
            } catch (Exception e) {
                log.error("Servelt context init parameter '" + FFCKeditorWeblet.SIZE_MAX + "' error.");
            }
            realDir = ex_context.getInitParameter(FILE_REPOSITORY_PATH_CONTEXT_PARAM_2);
            if (realDir == null) {
                realDir = ex_context.getInitParameter(FILE_REPOSITORY_PATH_CONTEXT_PARAM);
            }
            baseDir = ex_context.getInitParameter(FILE_CONTEXT_PATH_CONTEXT_PARAM);
            if (realDir == null || baseDir == null) {
                log.error("Context parameter FILE_REPOSITORY_PATH and FILE_CONTEXT_PATH must be set.");
                return;
            }
            if (!baseDir.endsWith("/")) {
                baseDir = baseDir + "/";
            }
            if (!baseDir.startsWith("/")) {
                baseDir = ex_context.getRequestContextPath() + "/" + baseDir;
            }
            if (log.isDebugEnabled()) {
                log.debug("baseDir: " + baseDir);
            }
            if (!realDir.endsWith(File.separator)) {
                realDir = realDir + File.separator;
            }
            if (log.isDebugEnabled()) {
                log.debug("realDir: " + realDir);
            }
            try {
                File baseFile = new File(this.realDir);
                if (!baseFile.isAbsolute()) {
                    if (ex_context.getContext() instanceof ServletContext) {
                        this.realDir = ((ServletContext) ex_context.getContext()).getRealPath("/") + this.realDir;
                        if (log.isDebugEnabled()) {
                            log.debug("realDir: " + realDir);
                        }
                        baseFile = new File(this.realDir);
                    }
                }
                if (!baseFile.exists()) {
                    baseFile.mkdir();
                }
            } catch (Exception e) {
                log.error("Error to create base directory: " + this.realDir);
            }
        } catch (Exception e) {
            log.error("Error to get FacesContext.");
        }
    }

    public void service(WebletRequest request, WebletResponse response) throws IOException, WebletException {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext ex_context = context.getExternalContext();
        Object faces_request = ex_context.getRequest();
        if (!(faces_request instanceof HttpServletRequest)) {
            log.error("Unable to get HttpServletRequest.");
            response.setStatus(WebletResponse.SC_NOT_FOUND);
            return;
        }
        HttpServletRequest http_request = (HttpServletRequest) faces_request;
        Object jsf_response = ex_context.getResponse();
        if (!(jsf_response instanceof HttpServletResponse)) {
            log.error("Unable to get HttpServletResponse.");
            response.setStatus(WebletResponse.SC_NOT_FOUND);
            return;
        }
        HttpServletResponse http_response = (HttpServletResponse) jsf_response;
        if (log.isDebugEnabled()) {
            log.debug("ConnectorWeblet.service: getPathInfo = " + request.getPathInfo());
            log.debug("ConnectorWeblet.service: getRequestURL = " + http_request.getRequestURL());
        }
        String path_info = request.getPathInfo();
        if (path_info.contains(CONNECTOR_PATTERN)) {
            try {
                if (connector == null) {
                    connector = new Connector(this);
                }
                if (http_request.getMethod().equalsIgnoreCase("POST")) {
                    connector.doPost(http_request, http_response);
                } else {
                    connector.doGet(http_request, http_response);
                }
            } catch (Exception e) {
                log.error("FCKeditorWeblet.service editor connector error: ", e);
            }
            return;
        } else if (path_info.contains(UPLOADER_PATTERN)) {
            try {
                if (uploader == null) {
                    uploader = new SimpleUploader(this);
                }
                uploader.doPost(http_request, http_response);
            } catch (Exception e) {
                log.error("FCKeditorWeblet.service editor uploader error: ", e);
            }
            return;
        }
        try {
            doGetEditor(path_info, request, response);
        } catch (Exception e) {
            log.error("ConnectorWeblet.service editor resource error: ", e);
        }
    }

    public void doGetEditor(String path, WebletRequest request, WebletResponse response) throws IOException {
        ClassLoader cl = this.getClass().getClassLoader();
        response.setLastModified(System.currentTimeMillis());
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith(".html")) {
            response.setContentType("text/html");
        } else if (path.endsWith(".xml")) {
            response.setContentType("text/xml");
        } else if (path.endsWith(".css")) {
            response.setContentType("text/css");
        } else if (path.endsWith(".js")) {
            response.setContentType("text/javascript");
        } else if (path.endsWith(".gif")) {
            response.setContentType("image/gif");
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            response.setContentType("image/jpeg");
        } else if (path.endsWith(".png")) {
            response.setContentType("image/png");
        }
        URL url = cl.getResource(path);
        if (url == null) {
            if (log.isWarnEnabled()) {
                log.warn("FCKeditorWeblet: resource not found: " + path);
            }
            return;
        }
        URLConnection conn = url.openConnection();
        if (conn == null) {
            if (log.isWarnEnabled()) {
                log.warn("FCKeditorWeblet: resource not found: " + path);
            }
            return;
        }
        response.setLastModified(conn.getLastModified());
        if (request.getIfModifiedSince() < conn.getLastModified()) {
            response.setContentLength(conn.getContentLength());
            InputStream is = conn.getInputStream();
            OutputStream out = response.getOutputStream();
            byte[] buffer = new byte[2048];
            BufferedInputStream bis = new BufferedInputStream(is);
            int read = 0;
            read = bis.read(buffer);
            while (read != -1) {
                out.write(buffer, 0, read);
                read = bis.read(buffer);
            }
            bis.close();
            out.flush();
            out.close();
        } else {
            response.setStatus(WebletResponse.SC_NOT_MODIFIED);
        }
    }

    public long getMaxUploadSize() {
        return max_upload_size;
    }

    public void setMaxUploadSize(long max_upload_size) {
        this.max_upload_size = max_upload_size;
    }

    public String getBaseDir() {
        String chRootPath = getChRootPath();
        if (chRootPath != null) {
            String result = baseDir + chRootPath;
            log.debug("baseDir: " + result);
            return result;
        }
        return baseDir;
    }

    public String getRealDir() {
        String chRootPath = getChRootPath();
        if (chRootPath != null) {
            String result = realDir + chRootPath.replaceAll("/", File.separator);
            log.debug("realDir: " + result);
            try {
                File baseFile = new File(result);
                if (!baseFile.exists()) {
                    baseFile.mkdir();
                }
                return result;
            } catch (Exception e) {
                log.error("Error to create base directory: " + result);
            }
        }
        return realDir;
    }

    public String getChRootPath() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext ex_context = context.getExternalContext();
        Object chRootObj = ex_context.getSessionMap().get(FILE_CHROOT_PATH_SESSION_PARAM);
        String chRootPath = null;
        if (chRootObj != null) {
            chRootPath = chRootObj.toString();
            if (chRootPath != null) {
                while (chRootPath.startsWith("/")) {
                    chRootPath = chRootPath.substring(1);
                }
                if (!chRootPath.endsWith("/")) {
                    chRootPath = chRootPath + "/";
                }
            }
        }
        return chRootPath;
    }

    /**
   * Helper function to verify if a file extension is allowed or not allowed.
   */
    public boolean extIsAllowed(String fileType, String ext) {
        try {
            ext = ext.toLowerCase();
            if (deniedExtensions.contains(ext)) {
                return false;
            }
            if (FILE_TYPE.equalsIgnoreCase(fileType)) {
                return true;
            }
            ArrayList allowList = allowedExtensions.get(fileType);
            if (allowList.contains(ext)) {
                return true;
            }
        } catch (Exception e) {
            log.error("FCKeditorWeblet.extIsAllowed", e);
        }
        return false;
    }

    /**
   * Helper function to convert the configuration string to an ArrayList.
   */
    @SuppressWarnings("unchecked")
    private ArrayList stringToArrayList(String str) {
        if (log.isDebugEnabled()) {
            log.debug(str);
        }
        if (str == null) {
            return new ArrayList();
        }
        String[] strArr = str.split("\\|");
        ArrayList tmp = new ArrayList();
        if (str.length() > 0) {
            for (int i = 0; i < strArr.length; ++i) {
                if (log.isDebugEnabled()) {
                    log.debug(i + " - " + strArr[i]);
                }
                tmp.add(strArr[i].toLowerCase().trim());
            }
        }
        return tmp;
    }
}
