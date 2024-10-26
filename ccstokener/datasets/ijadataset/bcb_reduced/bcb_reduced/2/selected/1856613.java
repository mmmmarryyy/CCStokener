package org.one.stone.soup.xapp.wiki.client;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.Vector;
import org.one.stone.soup.file.FileHelper;
import org.one.stone.soup.stringhelper.StringArrayHelper;

public final class Wiki {

    public static final String SANDBOX = "Sandbox";

    public static final String FORM_BOUNDARY = "----puoSenotSenO";

    private static String[] uploadFields = new String[] { "page", "action", "nextpage" };

    private static String[] uploadData = new String[] { SANDBOX, "upload", "Upload.jsp?page=Sandbox" };

    private static String[] saveFields = new String[] { "page", "action", "edittime", "text", "ok" };

    private static String[] saveData = new String[] { SANDBOX, "save", "0", "__No text set__", "Save" };

    private static final String PAGE_INDEX_PATTERN = "<a class=\"wikipage\" href=\"";

    private static final String PAGE_EDIT_TIME_PATTERN = "input name='edittime' type='hidden' value='";

    private static final String PAGE_EDIT_TEXT_PATTERN = "id='editorarea' name='text' rows='25' cols='80'>";

    private String url;

    private class EditTransfer {

        public long edittime;

        public String text;
    }

    private Wiki() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPageURL(String pageName) {
        return getUrl() + "/" + pageName;
    }

    public String getUploadFileURL() {
        return getUrl() + "/Attach";
    }

    public String getUploadPageURL(String pageName) {
        return getUrl() + "/Edit?page=" + pageName;
    }

    public String getPagesIndexUrl() {
        return getPageURL("PagesIndex");
    }

    public Object[] getPages() {
        String urlString = getPagesIndexUrl();
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
            return null;
        }
        try {
            String htmlData = new String(FileHelper.readFile(url.openStream()));
            int nextPageIndex = htmlData.indexOf(PAGE_INDEX_PATTERN);
            Hashtable pages = new Hashtable();
            while (nextPageIndex > -1) {
                htmlData = htmlData.substring(nextPageIndex);
                int endIndex = htmlData.indexOf("\">");
                String pageName = htmlData.substring(PAGE_INDEX_PATTERN.length(), endIndex);
                pages.put(pageName, pageName);
                htmlData = htmlData.substring(PAGE_INDEX_PATTERN.length());
                nextPageIndex = htmlData.indexOf(PAGE_INDEX_PATTERN);
            }
            String[] pagesArray = StringArrayHelper.vectorToStringArray(new Vector(pages.keySet()));
            return StringArrayHelper.sort(pagesArray, StringArrayHelper.ASCENDING);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean uploadFile(String pageName, String fileName, InputStream fileStream) {
        uploadData[0] = pageName;
        String urlString = getUploadFileURL();
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
            return false;
        }
        try {
            URLConnection connection = url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + FORM_BOUNDARY);
            connection.setRequestProperty("User-Agent", "Wiki Poster");
            OutputStream outStream = connection.getOutputStream();
            PrintWriter out = new PrintWriter(outStream);
            for (int loop = 0; loop < uploadFields.length; loop++) {
                out.println("--" + FORM_BOUNDARY);
                out.println("Content-Disposition: form-data; name=\"" + uploadFields[loop] + "\"");
                out.println();
                out.println(uploadData[loop]);
            }
            out.println("--" + FORM_BOUNDARY);
            out.println("Content-Disposition: form-data; name=\"filename\"; filename=\"" + URLEncoder.encode(fileName) + "\"");
            out.println("Content-Type: application/octet-stream");
            out.println();
            out.flush();
            BufferedInputStream fileBuffer = new BufferedInputStream(fileStream);
            byte[] dataBuffer = new byte[1000];
            int size = fileBuffer.read(dataBuffer);
            while (size > 0) {
                outStream.write(dataBuffer, 0, size);
                size = fileBuffer.read(dataBuffer);
                outStream.flush();
            }
            fileStream.close();
            out.print("\r\n--" + FORM_BOUNDARY + "--");
            out.flush();
            String result = new String(FileHelper.readFile(connection.getInputStream()));
            System.out.println("Result: " + result);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private EditTransfer getPageEditTransfer(String pageName) {
        String urlString = getUploadPageURL(pageName);
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
            return null;
        }
        try {
            String htmlData = new String(FileHelper.readFile(url.openStream()));
            int pagePos = htmlData.indexOf(PAGE_EDIT_TIME_PATTERN);
            htmlData = htmlData.substring(pagePos);
            int endIndex = htmlData.indexOf("'/>");
            String editTime = htmlData.substring(PAGE_EDIT_TIME_PATTERN.length(), endIndex);
            htmlData = htmlData.substring(PAGE_EDIT_TIME_PATTERN.length() + endIndex);
            pagePos = htmlData.indexOf(PAGE_EDIT_TEXT_PATTERN);
            htmlData = htmlData.substring(pagePos);
            endIndex = htmlData.indexOf("</textarea>");
            String text = htmlData.substring(PAGE_EDIT_TEXT_PATTERN.length(), endIndex);
            EditTransfer transfer = new Wiki().new EditTransfer();
            transfer.edittime = Long.parseLong(editTime);
            transfer.text = text;
            return transfer;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean uploadPage(String pageName, String pageData) {
        if (pageName == null) {
            pageName = SANDBOX;
        }
        EditTransfer transfer = getPageEditTransfer(pageName);
        if (transfer == null) {
            return false;
        }
        saveData[0] = pageName;
        saveData[2] = "" + transfer.edittime;
        saveData[3] = transfer.text + "\n" + pageData;
        String urlString = getUploadPageURL(pageName);
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
            return false;
        }
        try {
            URLConnection connection = url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("User-Agent", "Wiki Poster");
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            StringBuffer dataString = new StringBuffer();
            for (int loop = 0; loop < saveFields.length; loop++) {
                if (loop > 0) dataString.append('&');
                dataString.append(saveFields[loop] + "=" + URLEncoder.encode(saveData[loop]));
            }
            out.print(dataString.toString());
            out.flush();
            String result = new String(FileHelper.readFile(connection.getInputStream()));
            System.out.println("Result: " + result);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
