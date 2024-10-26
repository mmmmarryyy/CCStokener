package org.mbari.aosn.colloport.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.mbari.aosn.colloport.logic.CollOPort;
import org.mbari.aosn.colloport.model.Observatory;
import org.mbari.aosn.colloport.uploader.DocumentUploader;
import org.mbari.aosn.colloport.uploader.NoteUploader;
import org.mbari.aosn.colloport.uploader.ProgressMonitorFileItemFactory;
import org.mbari.aosn.colloport.uploader.Uploader;
import org.mbari.aosn.colloport.util.ServletUtil;

/**
 * Servlet implementation class for Servlet: UploadData
 *
 */
public class UploadData extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    /**
	 * 
	 */
    private static final long serialVersionUID = -7359171172634945004L;

    private static CollOPort colloport = null;

    private static Map<String, Uploader> uploaderMap = null;

    public UploadData() {
        super();
        if (colloport == null) {
            colloport = CollOPort.getInstance();
        }
        if (uploaderMap == null) {
            uploaderMap = new HashMap<String, Uploader>();
            uploaderMap.put("note", new NoteUploader());
            uploaderMap.put("document", new DocumentUploader());
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long current = System.currentTimeMillis();
        long expires = current;
        response.addDateHeader("Expires", expires);
        response.addDateHeader("Last-Modified", current);
        response.setContentType("text/html");
        request.setCharacterEncoding("UTF-8");
        FileItem fileItem = null;
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            ServletFileUpload upload = new ServletFileUpload();
            upload.setFileItemFactory(new ProgressMonitorFileItemFactory(request));
            List<?> fileItems = upload.parseRequest(request);
            for (int i = 0; i < fileItems.size(); i++) {
                FileItem item = (FileItem) fileItems.get(i);
                String fieldName = item.getFieldName();
                if (item.isFormField() == true) {
                    map.put(fieldName, item.getString());
                } else {
                    fileItem = item;
                    map.put(item.getFieldName(), item);
                    request.getSession().setAttribute("FileUpload.Progress." + fieldName, 100);
                }
            }
        } catch (FileUploadException fe) {
            return;
        }
        String observatory_id = getString(map, "observatory_id");
        String topic_id = null;
        Observatory observatory = null;
        if (observatory_id != null) {
            observatory = colloport.getLatestObservatory(observatory_id);
        } else {
            topic_id = getString(map, "topic_id");
            if (topic_id != null) {
                observatory = colloport.getLatestObservatoryFromTopicId(topic_id);
            }
        }
        String upload_url = getString(map, "upload_url");
        if (upload_url != null && upload_url.length() > 0) {
            String filename = upload_url.substring(upload_url.lastIndexOf("/") + 1);
            if (filename.indexOf("?") > 0) {
                filename = filename.substring(0, filename.indexOf("?"));
            }
            URL url = null;
            try {
                url = new URL(response.encodeURL(upload_url));
            } catch (MalformedURLException mue) {
                mue.printStackTrace();
            }
            URLConnection conn = null;
            if (url != null) {
                try {
                    conn = url.openConnection();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            if (conn != null) {
                String contentType = conn.getContentType();
                String contentDisposition;
                if (contentType == null) {
                    contentType = "application/x-java-serialized-object";
                    contentDisposition = "attachment;filename=\"" + filename + "\"";
                } else {
                    contentDisposition = "inline;filename=\"" + filename + "\"";
                }
                response.setHeader("content-disposition", contentDisposition);
                response.setContentType(contentType);
                InputStream inputStream = conn.getInputStream();
                File temporaryDirectory = (File) request.getSession().getServletContext().getAttribute("javax.servlet.context.tempdir");
                String fieldName = "attachment";
                fileItem = new DiskFileItem(fieldName, contentType, false, filename, DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD, temporaryDirectory);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) >= 0) {
                    fileItem.getOutputStream().write(buffer, 0, bytesRead);
                }
                inputStream.close();
                fileItem.getOutputStream().close();
                if (conn instanceof HttpURLConnection) {
                    ((HttpURLConnection) conn).disconnect();
                }
                map.put(fileItem.getFieldName(), fileItem);
                request.getSession().setAttribute("FileUpload.Progress." + fieldName, 100);
            }
        }
        System.out.println("In UploadData#doPost, fileItem=" + fileItem);
        if (observatory == null || !colloport.hasObservatoryPermit(observatory, "write", request)) {
            response.getOutputStream().print("No Permission!");
            return;
        }
        Uploader uploader = (Uploader) uploaderMap.get(getString(map, "type"));
        if (uploader == null) {
            response.getOutputStream().print("No uploader type!");
            return;
        }
        String fullname = CollOPort.getFullName(request);
        PrintStream out = new PrintStream(response.getOutputStream());
        uploader.upload(request, map, response, colloport, fullname, observatory, out);
        ServletUtil.redirectToRoot(request, response, true);
    }

    public static String getString(Map<String, Object> map, String name) {
        Object object = map.get(name);
        if (object != null && object instanceof String) {
            String string = (String) object;
            if (string.length() == 0 || string.equals("null")) {
                return null;
            }
            return string;
        } else if (object instanceof FileItem) {
            return ((FileItem) object).getName();
        }
        return null;
    }

    public static FileItem getFileItem(Map<String, Object> map, String name) {
        Object object = map.get(name);
        if (object instanceof FileItem) {
            return (FileItem) object;
        }
        return null;
    }
}
