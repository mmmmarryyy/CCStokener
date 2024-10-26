package net.cytopia.tofu.util;

import java.io.BufferedReader;
import net.cytopia.tofu.http.HttpData;
import net.cytopia.tofu.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 *
 * @author kev
 */
public class HttpUtils {

    private static boolean loggingEnabled = true;

    public static HttpData processMultiPartRequest(HttpServletRequest request, HttpServletResponse response) {
        Hashtable<String, File> files = new Hashtable<String, File>();
        Hashtable<String, String> params = new Hashtable<String, String>();
        HttpData data = new HttpData(request, response);
        String delim = FileUtil.getFilePathDelimeter();
        String tempPath = FileUtil.getWebAppFilePath(request.getContextPath()) + delim + "temp/";
        try {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            List fileList = upload.parseRequest(request);
            InputStream uploadedFileStream = null;
            String uploadedFileName = null;
            for (Iterator i = fileList.iterator(); i.hasNext(); ) {
                FileItem fi = (FileItem) i.next();
                if (fi.isFormField()) {
                    String key = fi.getFieldName();
                    String val = fi.getString();
                    params.put(key, val);
                    log(HttpUtils.class, "Form parameter " + key + "=" + val);
                } else {
                    if (fi.getSize() < 1) {
                        throw new Exception("No file was uplaoded");
                    }
                    uploadedFileName = fi.getName();
                    uploadedFileStream = fi.getInputStream();
                    String filePath = FileUtil.getWebAppFilePath(request.getContextPath()) + delim + fi.getName();
                    FileUtil.writeBytesToFile(fi.get(), filePath);
                    File file = new File(filePath);
                    if (file.exists()) {
                        files.put(fi.getFieldName(), file);
                        log(HttpUtils.class, " Adding file: " + file.getName());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        data.setFiles(files);
        data.setParameters(params);
        return data;
    }

    public static String getAppServerDir(String webAppContext) {
        String delimeter = FileUtil.getFilePathDelimeter();
        String basePath = System.getProperty("user.dir") + delimeter + ".." + delimeter + "webapps" + webAppContext;
        return basePath;
    }

    public static synchronized String getURLContent(URL url) {
        String result = "";
        InputStream is = null;
        if (url == null) {
            return result;
        }
        try {
            log(HttpUtils.class, "Open Connection...");
            URLConnection connection = url.openConnection();
            is = connection.getInputStream();
            Scanner scanner = new Scanner(is);
            scanner.useDelimiter(END_OF_INPUT);
            result = scanner.next();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static void log(Class cls, String msg) {
        if (loggingEnabled) {
            System.out.println("[" + cls.getSimpleName() + "] " + msg);
        }
    }

    public static boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public static void toggleLogging(boolean state) {
        loggingEnabled = state;
    }

    public static boolean isFlagSet(HttpServletRequest request, String flagName) {
        if (request.getParameter(flagName) != null && !request.getParameter(flagName).equals("")) {
            return true;
        }
        return false;
    }

    public static void printRequest(HttpServletRequest request) {
        if (request != null) {
            Enumeration enm = request.getParameterNames();
            while (enm.hasMoreElements()) {
                String key = (String) enm.nextElement();
                log(HttpUtils.class, key + " has value: " + request.getParameter(key));
            }
        }
    }

    public static boolean getBooleanValue(HttpServletRequest request, String name, boolean defaultValue) {
        boolean result = defaultValue;
        if (isFlagSet(request, name)) {
            log(HttpUtils.class, "Flag (" + name + ") is set: " + request.getParameter(name));
            if (request.getParameter(name).toLowerCase().equals("true") || request.getParameter(name).toLowerCase().equals("yes") || request.getParameter(name).toLowerCase().equals("on")) {
                result = true;
            } else {
                result = false;
            }
        } else {
            log(HttpUtils.class, "Flag (" + name + ") is NOT set: ");
        }
        return result;
    }

    public static int getIntValue(HttpServletRequest request, String name) {
        int result = -1;
        if (isFlagSet(request, name)) {
            log(HttpUtils.class, "Flag (" + name + ") is set: " + request.getParameter(name));
            try {
                result = Integer.parseInt(request.getParameter(name).trim());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static String getRequestValue(HttpServletRequest request, String name) {
        if (request.getParameter(name) != null && !request.getParameter(name).equals("")) {
            return request.getParameter(name);
        }
        return "";
    }

    private static final String END_OF_INPUT = "\\Z";
}
