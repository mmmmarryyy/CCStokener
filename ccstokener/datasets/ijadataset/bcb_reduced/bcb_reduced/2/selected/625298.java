package com.dcivision.workflow.applet.net;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import com.dcivision.dms.client.HtmlFormFile;
import com.dcivision.dms.client.HtmlFormText;
import com.dcivision.workflow.applet.WorkflowEditor;

/**
  HttpSender.java

  This class is use httpConnection communication to paraDM system .

    @author          Beyond
    @company         DCIVision Limited
    @creation date   27/08/2004
    @version         $Revision: 1.15.2.1 $
    */
public class HttpOperation {

    public static final String REVISION = "$Revision: 1.15.2.1 $";

    protected static final String starter = "-----------------------------";

    protected static final String returnChar = "\r\n";

    protected static final String lineEnd = "--";

    private List txtList = new ArrayList();

    private List fileList = new ArrayList();

    private List headList = new ArrayList();

    private String urlString = "";

    private String targetFile = null;

    private String actionStatus = null;

    private WorkflowEditor workflowEditor = null;

    private URL baseURL = null;

    private String projectName = null;

    private boolean needLogin = false;

    public HttpOperation(WorkflowEditor workflowEditor) {
        this.workflowEditor = workflowEditor;
    }

    public HttpOperation() {
    }

    public void addHtmlFormText(HtmlFormText txt) {
        txtList.add(txt);
    }

    public void addHtmlFormFile(HtmlFormFile file) {
        fileList.add(file);
    }

    public void setBaseURL(URL baseURL) {
        this.baseURL = baseURL;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
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

    public void setHeadList(List headList) {
        this.headList = headList;
    }

    public List getHeadList() {
        return this.headList;
    }

    public void setNeedLogin(boolean needLogin) {
        this.needLogin = needLogin;
    }

    public boolean getNeedLogin() {
        return this.needLogin;
    }

    public StringBuffer transmit() throws Exception {
        return this.transmit(null);
    }

    public StringBuffer transmit(String input) throws Exception {
        if (!(headList != null && headList.size() > 0) || "".equals(urlString)) {
            return null;
        }
        StringBuffer returnMessage = new StringBuffer();
        final String boundary = String.valueOf(System.currentTimeMillis());
        URL url = null;
        URLConnection conn = null;
        BufferedReader br = null;
        DataOutputStream dos = null;
        try {
            url = new URL(baseURL, "/" + projectName + urlString);
            conn = url.openConnection();
            ((HttpURLConnection) conn).setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Cookie", (String) headList.get(0));
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
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String tempstr;
            int line = 0;
            while (null != ((tempstr = br.readLine()))) {
                returnMessage.append("\n" + tempstr);
            }
            txtList.clear();
            fileList.clear();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                dos.close();
            } catch (Exception e) {
            }
            try {
                br.close();
            } catch (Exception e) {
            }
        }
        return returnMessage;
    }

    private String formatLine(String inputStr) {
        String result = "";
        String messageType = "";
        if (inputStr.indexOf("errorMessage") > 0) {
            messageType = "error";
            actionStatus = "error";
            result = "Status: " + messageType;
        } else if (inputStr.indexOf("systemMessage") > 0) {
            messageType = "system";
            actionStatus = "system";
            result = "Status: " + messageType;
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

    /**
   * login to System
   *
   * Set logind return headinformation(sessionid).
   *
   *
   * @param strUrl
   * @throws ServletException exception
   * 
   * @return is login sucessful.
   *
   */
    public boolean login(URL strUrl, String loginName, String loginPwd, String sessionID) throws Exception {
        String starter = "-----------------------------";
        String returnChar = "\r\n";
        String lineEnd = "--";
        URL urlString = strUrl;
        String input = null;
        List txtList = new ArrayList();
        List fileList = new ArrayList();
        String targetFile = null;
        String actionStatus = null;
        StringBuffer returnMessage = new StringBuffer();
        List head = new ArrayList();
        final String boundary = String.valueOf(System.currentTimeMillis());
        URL url = null;
        URLConnection conn = null;
        BufferedReader br = null;
        DataOutputStream dos = null;
        boolean isLogin = false;
        txtList.add(new HtmlFormText("loginName", loginName));
        txtList.add(new HtmlFormText("loginPwd", loginPwd));
        txtList.add(new HtmlFormText("navMode", "I"));
        txtList.add(new HtmlFormText("action", "login"));
        try {
            url = new URL(urlString, "/" + projectName + "/Login.do");
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
            dos.write((starter + boundary + "--" + returnChar).getBytes());
            dos.flush();
            br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String key;
            String header;
            int i = 1;
            key = conn.getHeaderFieldKey(i);
            header = conn.getHeaderField(i);
            System.out.println(header);
            if (Utility.isEmpty(header) || header.indexOf("JSESSIONID") < 0) {
                header = "JSESSIONID=" + sessionID + "; Path=/" + projectName;
            }
            while (key != null) {
                head.add(header);
                i++;
                key = conn.getHeaderFieldKey(i);
                header = conn.getHeaderField(i);
            }
            String tempstr;
            int line = 0;
            while (null != ((tempstr = br.readLine()))) {
                if (!tempstr.equals("")) {
                    if ("window.location.replace(\"/eip/Home.do\");".indexOf(returnMessage.append(formatLine(tempstr)).toString()) != -1) {
                        isLogin = true;
                        break;
                    }
                    line++;
                }
            }
            txtList.clear();
            fileList.clear();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                dos.close();
            } catch (Exception e) {
            }
            try {
                br.close();
            } catch (Exception e) {
            }
        }
        this.setHeadList(head);
        return isLogin;
    }

    /**
   * getObjectInputStreamFromServlet
   * 
   * get object inputStream from servlet
   *
   * @param strUrl
   * @throws ServletException exception
   * 
   * @return  ObjectInputStream.
   *  
   */
    public ObjectInputStream getObjectInputStreamFromServlet(String strUrl) throws Exception {
        if (headList.size() == 0) {
            return null;
        }
        String starter = "-----------------------------";
        String returnChar = "\r\n";
        String lineEnd = "--";
        String urlString = strUrl;
        String input = null;
        List txtList = new ArrayList();
        List fileList = new ArrayList();
        String targetFile = null;
        String actionStatus = null;
        StringBuffer returnMessage = new StringBuffer();
        List head = new ArrayList();
        final String boundary = String.valueOf(System.currentTimeMillis());
        URL url = null;
        URLConnection conn = null;
        DataOutputStream dos = null;
        ObjectInputStream inputFromServlet = null;
        try {
            url = new URL(baseURL, "/" + projectName + strUrl);
            conn = url.openConnection();
            ((HttpURLConnection) conn).setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "multipart/form-data, boundary=" + "---------------------------" + boundary);
            conn.setRequestProperty("Cookie", (String) headList.get(0));
            if (input != null) {
                String auth = "Basic " + new sun.misc.BASE64Encoder().encode(input.getBytes());
                conn.setRequestProperty("Authorization", auth);
            }
            dos = new DataOutputStream(conn.getOutputStream());
            dos.flush();
            inputFromServlet = new ObjectInputStream(conn.getInputStream());
            txtList.clear();
            fileList.clear();
        } catch (EOFException e) {
            workflowEditor.getEditor().outputMessage("Session Expired!", false);
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                dos.close();
            } catch (Exception e) {
            }
        }
        return inputFromServlet;
    }

    public void reset() {
        txtList = new ArrayList();
        fileList = new ArrayList();
        setSubmissionURL("");
    }
}
