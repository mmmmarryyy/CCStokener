package com.dcivision.dms.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
  HttpSender.java

  This class is to store all error code constants which used in systems.

    @author          Rollo Chan
    @company         DCIVision Limited
    @creation date   24/06/2003
    @version         $Revision: 1.8.26.1 $
    */
public class HttpSender {

    public static final String REVISION = "$Revision: 1.8.26.1 $";

    protected static final String starter = "-----------------------------";

    protected static final String returnChar = "\r\n";

    protected static final String lineEnd = "--";

    private List txtList = new ArrayList();

    private List fileList = new ArrayList();

    private String urlString;

    private String targetFile = null;

    private String actionStatus = null;

    private static final Log log = LogFactory.getLog(HttpSender.class);

    public HttpSender() {
    }

    public void addHtmlFormText(HtmlFormText txt) {
        txtList.add(txt);
    }

    public void addHtmlFormFile(HtmlFormFile file) {
        fileList.add(file);
    }

    public void setSubmissionURL(String urlString) {
        this.urlString = urlString;
    }

    public void setTargetFile(String targetFile) {
        this.targetFile = targetFile;
    }

    public String getTargetFile() {
        return this.targetFile;
    }

    public String getActionStatus() {
        return this.actionStatus;
    }

    public StringBuffer transmit() throws Exception {
        return this.transmit(null);
    }

    public StringBuffer transmit(String input) throws Exception {
        StringBuffer returnMessage = new StringBuffer();
        final String boundary = String.valueOf(System.currentTimeMillis());
        URL url = null;
        URLConnection conn = null;
        BufferedReader br = null;
        DataOutputStream dos = null;
        try {
            url = new URL(urlString);
            conn = url.openConnection();
            ((HttpURLConnection) conn).setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "multipart/form-data, boundary=" + "---------------------------" + boundary);
            if (input != null) {
                String auth = "Basic " + new sun.misc.BASE64Encoder().encode(input.getBytes());
                conn.setRequestProperty("Authorization", auth);
            }
            dos = new DataOutputStream(conn.getOutputStream());
            dos.write((starter + boundary + returnChar).getBytes());
            for (int i = 0; i < txtList.size(); i++) {
                HtmlFormText htmltext = (HtmlFormText) txtList.get(i);
                dos.write(htmltext.getTranslated());
                if (i + 1 < txtList.size()) {
                    dos.write((starter + boundary + returnChar).getBytes());
                } else if (fileList.size() > 0) {
                    dos.write((starter + boundary + returnChar).getBytes());
                }
            }
            for (int i = 0; i < fileList.size(); i++) {
                HtmlFormFile htmlfile = (HtmlFormFile) fileList.get(i);
                dos.write(htmlfile.getTranslated());
                if (i + 1 < fileList.size()) {
                    dos.write((starter + boundary + returnChar).getBytes());
                }
            }
            dos.write((starter + boundary + "--" + returnChar).getBytes());
            dos.flush();
            br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String tempstr;
            int line = 0;
            while (null != ((tempstr = br.readLine()))) {
                if (!tempstr.equals("")) {
                    if (line == 0) {
                        returnMessage.append("\r\n");
                        returnMessage.append("Time At: " + getCurrentTimestamp() + "," + "\n");
                        returnMessage.append("Uploading document " + targetFile + "," + "\n");
                    }
                    if (line == 1 || line == 2 || line == 3) {
                        returnMessage.append(formatLine(tempstr));
                    } else {
                        returnMessage.append(formatLine(tempstr));
                    }
                    returnMessage.append("\n");
                    line++;
                }
            }
            System.out.println("\nProcess " + returnMessage);
            txtList.clear();
            fileList.clear();
        } catch (Exception e) {
            actionStatus = "error";
            log.error(e, e);
        } finally {
            try {
                dos.close();
            } catch (Exception e) {
            } finally {
                dos = null;
            }
            try {
                br.close();
            } catch (Exception e) {
            } finally {
                br = null;
            }
            conn = null;
            url = null;
        }
        return returnMessage;
    }

    private String formatLine(String inputStr) {
        String result = "";
        String messageType = "";
        if (inputStr.indexOf("errorMessage") > 0) {
            messageType = "error";
            actionStatus = "error";
            result = "Status: " + messageType + ",";
        } else if (inputStr.indexOf("systemMessage") > 0) {
            messageType = "system";
            actionStatus = "system";
            result = "Status: " + messageType + ",";
        } else if (inputStr.indexOf(">") > 0) {
            String tmpStr = "";
            tmpStr = inputStr.substring(inputStr.lastIndexOf(">") + 1, inputStr.length());
            if (!"".equals(tmpStr)) {
                result = "Message: " + tmpStr;
            }
        } else if (inputStr.indexOf("</UL>") > 0) {
        } else if (inputStr.indexOf("\n") > 0) {
        } else {
            if (!"".equals(inputStr)) {
                result = inputStr;
            }
        }
        return result;
    }

    /**
   * getCurrentTimestamp
   *
   * Returns current time in Timestamp object.
   *
   * @return  Timestamp object which representing the current time.
   */
    public static java.sql.Timestamp getCurrentTimestamp() {
        java.util.Calendar tmp = java.util.Calendar.getInstance();
        tmp.clear(java.util.Calendar.MILLISECOND);
        return (new java.sql.Timestamp(tmp.getTime().getTime()));
    }
}
