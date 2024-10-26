package org.project.trunks.utilities;

import java.io.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.net.*;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;

public class FileUtilities {

    /**
	 * Logger
	 */
    private static org.apache.commons.logging.Log log = LogFactory.getLog(FileUtilities.class);

    public FileUtilities() {
    }

    public static String getValidFileName(String name) {
        String validName = name.replaceAll("/", "_");
        validName = validName.replaceAll(":", "-");
        validName = validName.replaceAll("<", "-");
        validName = validName.replaceAll(">", "-");
        return validName;
    }

    public static void copyFile(String fileURL, String fileOutputPath) throws Exception {
        log.info("FileUtilities.copyFile - fileURL = '" + fileURL + "'");
        URL url = new URL(fileURL);
        URLConnection uc = url.openConnection();
        InputStream is = null;
        try {
            log.info("FileUtilities.copyFile - url.getInputStream()... ");
            is = uc.getInputStream();
        } catch (Throwable e) {
            log.info("FileUtilities.copyFile - EXCEPTION : '" + e.getMessage() + "'");
            log.info("FileUtilities.copyFile - url.getInputStream() using proxy ... ");
            String proxyServer = StringUtilities.getNString(Properties.getProperty("application", "PROXY_SERVER"));
            String proxyPort = StringUtilities.getNString(Properties.getProperty("application", "PROXY_PORT"));
            String proxyAuthorization = StringUtilities.getNString(Properties.getProperty("application", "PROXY_AUTHORIZATION"));
            if (!proxyServer.trim().equals("")) {
                System.getProperties().put("http.proxyHost", proxyServer);
                System.getProperties().put("http.proxyPort", proxyPort);
                if (!proxyAuthorization.trim().equals("")) uc.setRequestProperty("Proxy-Authorization", "Basic " + proxyAuthorization);
                uc.setDoInput(true);
                uc.setDoOutput(true);
            }
            is = uc.getInputStream();
        }
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte b[] = new byte[1024];
        int c = 0;
        while ((c = is.read(b)) > 0) bo.write(b, 0, c);
        bo.flush();
        File f = new File(fileOutputPath);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(bo.toByteArray());
    }

    /**
	 * copyFile
	 * @param is
	 * @param fileOutputPath
	 * @throws java.lang.Exception
	 */
    public static void copyFile(InputStream is, String fileOutputPath) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte b[] = new byte[1024];
        int c = 0;
        while ((c = is.read(b)) > 0) bo.write(b, 0, c);
        bo.flush();
        File f = new File(fileOutputPath);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(bo.toByteArray());
    }

    public static InputStream getInputStream(URL url) throws Exception {
        log.info("<<<  FileUtilities.getInputStream - url = '" + url.toString() + "'");
        URLConnection uc = url.openConnection();
        String proxyServer = StringUtilities.getNString(Properties.getProperty("application", "PROXY_SERVER"));
        String proxyPort = StringUtilities.getNString(Properties.getProperty("application", "PROXY_PORT"));
        String proxyAuthorization = StringUtilities.getNString(Properties.getProperty("application", "PROXY_AUTHORIZATION"));
        if (!proxyServer.trim().equals("")) {
            System.getProperties().put("http.proxyHost", proxyServer);
            System.getProperties().put("http.proxyPort", proxyPort);
            if (!proxyAuthorization.trim().equals("")) uc.setRequestProperty("Proxy-Authorization", "Basic " + proxyAuthorization);
            uc.setDoInput(true);
            uc.setDoOutput(true);
        }
        log.info("<<<  url.openConnection() ");
        return uc.getInputStream();
    }

    /**
   * Replace
   * @param templateFile
   * @param signet
   * @param sToInsert
   * @return
   */
    public static String replace(String templateFile, String signet, String sToInsert) {
        try {
            StringBuffer sFileCompleted = new StringBuffer("");
            BufferedReader bufferedReader = getBufferedReader(templateFile);
            String s2 = null;
            do {
                s2 = bufferedReader.readLine();
                if (s2 != null) {
                    sFileCompleted.append(s2);
                    if (s2.indexOf(signet) != -1) sFileCompleted.append(sToInsert);
                }
            } while (s2 != null);
            bufferedReader.close();
            return sFileCompleted.toString();
        } catch (Exception e) {
            log.error("FileUtilities.replace - EXCEPTION : '" + e.getMessage() + "'");
            return templateFile;
        }
    }

    /**
   * getStringContentFile
   * @param filename
   * @return
   */
    public static String getStringContentFile(String filename) {
        try {
            BufferedReader bufferedReader = getBufferedReader(filename);
            StringBuffer content = new StringBuffer("");
            String s = null;
            do {
                s = bufferedReader.readLine();
                if (s != null) content.append(s);
            } while (s != null);
            bufferedReader.close();
            return content.toString();
        } catch (Exception e) {
            log.error("FileUtilities.getStringContentFile - EXCEPTION : '" + e.getMessage() + "' => return '' ");
            return "";
        }
    }

    /**
	 * getStringContentFile
	 * @param file
	 * @return
	 */
    public static String getStringContentFile(File file) {
        try {
            BufferedReader bais = getFileBufferedReader(file);
            StringBuffer content = new StringBuffer("");
            char b[] = new char[2048];
            int c = 0;
            while ((c = bais.read(b)) > 0) content.append(new String(b, 0, c));
            bais.close();
            return content.toString();
        } catch (Exception e) {
            log.error("FileUtilities.getStringContentFile - EXCEPTION : '" + e.getMessage() + "' => return '' ");
            return "";
        }
    }

    /**
   * getBufferedReader
   * @param filename
   * @return
   */
    protected static BufferedReader getBufferedReader(String templateFile) throws Exception {
        if (templateFile.indexOf("http:") == 0) {
            templateFile = templateFile.replace('\\', '/');
            InputStream is = new URL(templateFile).openStream();
            return new BufferedReader(new InputStreamReader(is));
        } else {
            FileInputStream fis = new FileInputStream(templateFile);
            return new BufferedReader(new InputStreamReader(fis));
        }
    }

    /**
	 * getInputStreamToByteArray
	 * @param inputStream
	 * @return byte[]
	 * @throws java.lang.Exception
	 */
    public static byte[] getInputStreamToByteArray(InputStream inputStream) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte b[] = new byte[1024];
        int c = 0;
        while ((c = inputStream.read(b)) > 0) bo.write(b, 0, c);
        bo.flush();
        return bo.toByteArray();
    }

    /**
   * displayPage
   * @param response
   * @param sPAGE
   * @throws java.lang.Exception
   */
    public static void displayPage(HttpServletResponse response, String sPAGE) throws IOException {
        response.setContentType("text/html");
        ServletOutputStream stream = response.getOutputStream();
        stream.write(sPAGE.getBytes());
        stream.flush();
        stream.close();
    }

    /**
   * displayPDF
   * @param response
   * @param pdfName
   * @throws IOException
   */
    public static void displayPDF(HttpServletResponse response, String pdfName) throws IOException {
        response.setContentType("application/pdf");
        displayFile(response, pdfName);
    }

    /**
	 * displayPDF
	 * @param response
	 * @param pdfName
	 * @throws IOException
	 */
    public static void displayPDF(HttpServletResponse response, byte[] bytes) throws IOException {
        response.setContentType("application/pdf");
        ServletOutputStream stream = response.getOutputStream();
        stream.write(bytes);
        stream.flush();
        stream.close();
    }

    /**
   * displayRTF
   * @param response
   * @param fileName
   * @throws IOException
   */
    public static void displayRTF(HttpServletResponse response, String fileName) throws IOException {
        response.setHeader("Pragma", "no-cache");
        response.setContentType("application/msword");
        FileUtilities.displayFile(response, fileName);
    }

    /**
   * displayRTF
   * @param response
   * @param fileName
   * @throws IOException
   */
    public static void displayRTF(HttpServletResponse response, byte[] bytes) throws IOException {
        response.setHeader("Pragma", "no-cache");
        response.setContentType("application/msword");
        ServletOutputStream stream = response.getOutputStream();
        stream.write(bytes);
        stream.flush();
        stream.close();
    }

    /**
	 * displayEXCEL
	 * @param response
	 * @param fileName
	 * @throws IOException
	 */
    public static void displayEXCEL(HttpServletResponse response, String fileName) throws IOException {
        response.setHeader("Cache-Control", "public");
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-disposition", "inline; filename=" + fileName);
        FileUtilities.displayFile(response, fileName);
    }

    /**
   * displayFile
   * @param response
   * @param sPAGE
   * @throws java.lang.Exception
   */
    public static void displayFile(HttpServletResponse response, String fileName) throws IOException {
        ServletOutputStream stream = response.getOutputStream();
        FileInputStream fis = new FileInputStream(fileName);
        byte[] buffer = new byte[2048];
        int nbBytes = fis.read(buffer);
        while (nbBytes != -1) {
            stream.write(buffer, 0, nbBytes);
            nbBytes = fis.read(buffer);
        }
        stream.flush();
        stream.close();
    }

    /**
   * Physical path to relative path
   * @param filename D:\\Application\\Directory\\Sub-Directory\\theImage.jpg
   * @param baseDir Directory
   * @return /Sub-Directory/theImage.jpg
   */
    public static String physicalPathToRelativePath(String filename, String baseDir) {
        String parts[] = filename.split("\\\\");
        String relPath = "";
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals(baseDir)) {
                for (int k = i + 1; k < parts.length; k++) {
                    try {
                        relPath += "/" + URLEncoder.encode(parts[k], "ISO-8859-1");
                    } catch (Exception e) {
                        log.error("<<< FileUtilities.physicalPathToRelativePath - EXCEPTION : '" + e.getMessage() + "'");
                        relPath += "/" + parts[k];
                    }
                }
                break;
            }
        }
        return relPath;
    }

    /** Convert from a filename to a file URL. */
    private static String fileToUrl(String filename) throws MalformedURLException {
        return toURL(new File(filename)).toString();
    }

    /**
	 * Converts this abstract pathname into a <code>file:</code> URL.  The
	 * exact form of the URL is system-dependent.  If it can be determined that
	 * the file denoted by this abstract pathname is a directory, then the
	 * resulting URL will end with a slash.
	 *
	 * @return a URL object representing the equivalent file URL.
	 * @throws MalformedURLException if the path cannot be parsed as a URL.
	 * @see     java.net.URL
	 * @since   1.2
	 */
    private static URL toURL(File file) throws MalformedURLException {
        String path = file.getAbsolutePath();
        if (File.separatorChar != '/') {
            path = path.replace(File.separatorChar, '/');
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.endsWith("/") && file.isDirectory()) {
            path = path + "/";
        }
        return new URL("file", "", path);
    }

    /**
	 * deleteFiles
	 * @param pathDir Directory path
	 * @param bCreatedDir Create the directory
	 * @param refTime Delete files for which the last modified date is anterior than refTime
	 */
    public static void deleteFiles(String pathDir, boolean bCreatedDir, long refTime) {
        File f = new File(pathDir);
        if (f == null) {
            log.info("<<< FileUtilities.deleteFiles - [" + pathDir + "] returns null");
            if (bCreatedDir) {
                boolean success = (new File(pathDir)).mkdir();
                if (success) log.info("<<< FileUtilities.deleteFiles - Directory [" + pathDir + "] created");
            }
        } else {
            String[] files = f.list();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    String filename = files[i];
                    String fullPath = pathDir + filename;
                    File aFile = new File(fullPath);
                    if (!aFile.isDirectory()) {
                        log.info("<<< FileUtilities.deleteFiles - aFile[" + filename + "].lastModified = " + aFile.lastModified());
                        if (aFile.lastModified() < refTime) if (aFile.delete()) log.info("<<< FileUtilities.deleteFiles - aFile[" + filename + "] was deleted");
                    }
                }
            }
        }
    }

    /**
   * deleteDirectory
   * @param path
   * @return
   */
    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    /**
	 * getInputSourceForFile
	 * @param file
	 * @return InputSource
	 * @throws java.lang.Exception
	 */
    public static InputSource getInputSourceForFile(File file) throws Exception {
        UnicodeReader fr = new UnicodeReader(new FileInputStream(file), "UTF-8");
        return new InputSource(fr);
    }

    public static BufferedReader getFileBufferedReader(File file) throws Exception {
        return new java.io.BufferedReader(getReader(file));
    }

    /**
	 * getReader
	 * @param file File
	 * @return InputStreamReader
	 * @throws java.lang.Exception
	 * @d--eprecate--d This method does not properly manage BOM.
	 */
    public static Reader getReader(File file) throws Exception {
        InputStreamReader fr = null;
        if (System.getProperty("file.encoding").equalsIgnoreCase("Cp1252")) {
            fr = new FileReader(file);
        } else {
            return new java.io.BufferedReader(new UnicodeReader(new FileInputStream(file), "UTF-8"));
        }
        return fr;
    }

    /**
   * @param directory the directory to clean out - the directory will optionally be removed
   * @param removeDir true if the directory must be removed as well, otherwise false
   * @return Returns the number of files removed
   */
    public static int removeFiles(File directory, boolean removeDir) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Expected a directory to clear: " + directory);
        }
        if (!directory.exists()) {
            return 0;
        }
        File[] files = directory.listFiles();
        int count = 0;
        File file = null;
        for (int i = 0; i < files.length; i++) {
            file = files[i];
            if (file.isDirectory()) {
                removeFiles(file, true);
            } else {
                removeFile(file);
            }
        }
        if (removeDir && directory.listFiles().length == 0) {
            try {
                directory.delete();
            } catch (Throwable e) {
                log.info("Failed to remove temp directory: " + directory);
            }
        }
        return count;
    }

    /**
    * removeFile
    * @param file
    */
    public static void removeFile(File file) {
        try {
            file.delete();
        } catch (Throwable e) {
            log.info("Failed to remove temp file: " + file);
        }
    }

    /**
    * createDirectory
    * @param f File
    */
    public static void createDirectory(File f) throws Exception {
        while (!f.exists()) {
            if (f.getParentFile().exists()) {
                createDirectory(f.getParentFile());
            }
            f.mkdirs();
            log.info("FileUtilities.createDirectory [" + f.getPath() + "] is now created");
        }
    }

    public static void autoPipeStream(InputStream i, OutputStream o) throws IOException {
        byte b[] = new byte[1024];
        int c = 0;
        while ((c = i.read(b)) > 0) o.write(b, 0, c);
        o.flush();
    }
}
