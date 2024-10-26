package dinamica;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.http.Cookie;

/**
 * Base class to create Output classes. This kind of classes
 * represent the "view" part of this framework MVC mechanism.
 * This class consumes recordsets published by transactions, which
 * have been previously executed by the controller.<br>
 * <br>
 * Creation date: 4/10/2003<br>
 * Last Update: 4/10/2003<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 */
public class GenericOutput extends AbstractModule implements IRowEvent {

    private int rowColor = 0;

    /**
	 * Generate text-based output using automatic data binding.<br>
	 * This class will consume Recordsets from the "data" object
	 * passed to this method, which is a Transaction that has been
	 * executed before calling the method.<br>
	 * This methods performs the data binding between the templates
	 * and the recordsets, according to the parameters defined in the
	 * config.xml file - it is a kind of VB DataControl, if you will!
	 * All its functionality relies on the TemplateEngine class.
     * Since this method performs a lot of work, all descendants of this class that
     * want to reimplement this method, should call super.print(TemplateEngine, Transaction) in the first line
     * of the reimplemented method in order to reuse all this functionality.
	 * @param te TemplateEngine containing the text based template ready for data binding
	 * @param data Transaction object that publishes one or more recordsets
	 * @throws Throwable
	 */
    public void print(TemplateEngine te, GenericTransaction data) throws Throwable {
        if (data != null) {
            Recordset rs = _config.getPrintCommands();
            while (rs.next()) {
                String nullExpr = null;
                if (_config.contentType.equals("text/html")) nullExpr = "&nbsp;"; else nullExpr = "";
                String pagesize = getRequest().getParameter("pagesize");
                if (pagesize != null && pagesize.trim().equals("")) pagesize = null;
                if (pagesize == null) pagesize = rs.getString("pagesize");
                String mode = (String) rs.getValue("mode");
                String tag = (String) rs.getValue("tag");
                String control = (String) rs.getValue("control");
                String rsname = (String) rs.getValue("recordset");
                String altColors = (String) rs.getValue("alternate-colors");
                String nullValue = (String) rs.getValue("null-value");
                if (nullValue != null) nullExpr = nullValue;
                if (mode.equals("table")) {
                    Recordset x = data.getRecordset(rsname);
                    if (x.getPageCount() > 0) pagesize = String.valueOf(x.getPageSize());
                    if (pagesize != null) {
                        int page = 0;
                        if (x.getPageCount() == 0) x.setPageSize(Integer.parseInt(pagesize));
                        String pageNumber = getRequest().getParameter("pagenumber");
                        if (pageNumber == null || pageNumber.equals("")) {
                            page = x.getPageNumber();
                            if (page == 0) page = 1;
                        } else {
                            page = Integer.parseInt(pageNumber);
                        }
                        x = x.getPage(page);
                    }
                    if (altColors != null && altColors.equals("true")) te.setRowEventObject(this);
                    te.replace(x, nullExpr, tag);
                } else if (mode.equals("form")) {
                    nullExpr = "";
                    Recordset x = data.getRecordset(rsname);
                    if (x.getRecordCount() > 0 && x.getRecordNumber() < 0) x.first();
                    if (x.getRecordCount() == 0) throw new Throwable("Recordset [" + rsname + "] has no records; can't print (mode=form) using an empty Recordset.");
                    te.replace(x, nullExpr);
                } else if (mode.equals("combo")) {
                    if (control == null) throw new Throwable("'control' attribute cannot be null when print-mode='combo'");
                    Recordset x = data.getRecordset(rsname);
                    if (x.getRecordCount() > 1) te.setComboValue(control, x); else if (x.getRecordCount() > 0 && x.getRecordNumber() < 0) x.first();
                    te.setComboValue(control, String.valueOf(x.getValue(control)));
                } else if (mode.equals("checkbox")) {
                    if (control == null) throw new Throwable("'control' attribute cannot be null when print-mode='checkbox'");
                    Recordset x = data.getRecordset(rsname);
                    te.setCheckbox(control, x);
                } else if (mode.equals("radio")) {
                    if (control == null) throw new Throwable("'control' attribute cannot be null when print-mode='radio'");
                    Recordset x = data.getRecordset(rsname);
                    if (x.getRecordCount() > 0 && x.getRecordNumber() < 0) x.first();
                    te.setRadioButton(control, String.valueOf(x.getValue(control)));
                } else if (mode.equals("clear")) {
                    te.clearFieldMarkers();
                } else {
                    throw new Throwable("Invalid print mode [" + mode + "] attribute in config.xml: " + _config.path);
                }
            }
        }
    }

    /**
	 * This method is called for non text based output (images, binaries, etc.).
	 * Reimplementations of this method MUST write the output
	 * thru the Servlet OutputStream.
	 * @param data Transaction object
	 * @throws Throwable
	 */
    public void print(GenericTransaction data) throws Throwable {
    }

    /**
	 * Implementation of the interface dinamica.IRowEvent.<br> 
	 * This code is used to alternate row colors,
	 * the row template must include the special
	 * field marker ${fld:_rowStyle} which will be replaced
	 * by the style parameters set in web.xml. 	 
	 * @see dinamica.IRowEvent#onNewRow(dinamica.Recordset, int, java.lang.String)
	 */
    public String onNewRow(Recordset rs, String rowTemplate) throws Throwable {
        String style1 = getContext().getInitParameter("def-color1");
        String style2 = getContext().getInitParameter("def-color2");
        String currentStyle = "";
        if (rowColor == 0) {
            rowColor = 1;
            currentStyle = style1;
        } else {
            rowColor = 0;
            currentStyle = style2;
        }
        rowTemplate = StringUtil.replace(rowTemplate, "${fld:_rowStyle}", currentStyle);
        return rowTemplate;
    }

    /**
	 * resets value of private variable rowColor. This
	 * variable control alternate printing of colors
	 * for tables (print mode="table")
	 *
	 */
    protected void resetRowColor() {
        this.rowColor = 0;
    }

    /**
	 * Return byte array with the content of a remote binary resource
	 * accessed via HTTP(S).
	 * @param url URL of the resource
	 * @param sessionID Session ID
	 * @param logStdout If TRUE will print trace log to System.out.
	 * @return Byte array with the content of the file
	 * @throws Throwable In case of any HTTP error of if the data cannot
	 * be read for any reason.
	 */
    protected byte[] getImage(String url, String sessionID, boolean logStdout) throws Throwable {
        HttpURLConnection urlc = null;
        BufferedInputStream bin = null;
        final int bufferSize = 10240;
        byte[] buffer = null;
        URL page = new URL(url);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        if (logStdout) System.err.println("Waiting for reply...:" + url);
        try {
            urlc = (HttpURLConnection) page.openConnection();
            urlc.setUseCaches(false);
            urlc.addRequestProperty("Host", getRequest().getServerName());
            urlc.addRequestProperty("Cookie", "JSESSIONID=" + sessionID);
            urlc.addRequestProperty("Cache-Control", "max-age=0");
            if (logStdout) {
                System.err.println("Content-type = " + urlc.getContentType());
                System.err.println("Content-length = " + urlc.getContentLength());
                System.err.println("Response-code = " + urlc.getResponseCode());
                System.err.println("Response-message = " + urlc.getResponseMessage());
            }
            int retCode = urlc.getResponseCode();
            String retMsg = urlc.getResponseMessage();
            if (retCode >= 400) throw new Throwable("HTTP Error: " + retCode + " - " + retMsg + " - URL:" + url);
            int size = urlc.getContentLength();
            if (size > 0) buffer = new byte[size]; else buffer = new byte[bufferSize];
            bin = new BufferedInputStream(urlc.getInputStream(), buffer.length);
            int bytesRead = 0;
            do {
                bytesRead = bin.read(buffer);
                if (bytesRead > 0) bout.write(buffer, 0, bytesRead);
            } while (bytesRead != -1);
            if (logStdout) {
                System.err.println("Connection closed.");
            }
            return bout.toByteArray();
        } catch (Throwable e) {
            throw e;
        } finally {
            if (bin != null) bin.close();
            if (urlc != null) urlc.disconnect();
        }
    }

    /**
	 * Retrieve the session ID from the request headers
	 * looking for a cookie named JSESSIONID. This method was
	 * implemented because some Servers (WepSphere 5.1) won't
	 * return the real cookie value when the HttpSession.getId()
	 * method is invoked, which causes big trouble when retrieving an
	 * image from an Action using HTTP and the session ID. This problem
	 * was discovered while testing PDF reports with charts on WAS 5.1, it
	 * is specific to WAS 5.1, but this method works well with all tested
	 * servlet engines, including Resin 2.x. 
	 * @return The session ID as stored in the cookie header, or NULL if it can find the cookie.
	 * @throws Throwable
	 */
    protected String getSessionID() {
        String value = null;
        Cookie c[] = getRequest().getCookies();
        for (int i = 0; i < c.length; i++) {
            if (c[i].getName().equals("JSESSIONID")) {
                value = c[i].getValue();
                break;
            }
        }
        return value;
    }

    /**
	 * Return byte array with the content of a remote binary resource
	 * accessed via HTTP(S) - a Cookie header (JSESSIONID) with the current
	 * session ID will be added to the request headers
	 * @param url URL of the resource
	 * @param logStdout If TRUE will print trace log to System.err.
	 * @return Byte array with the content of the file
	 * @throws Throwable In case of any HTTP error of if the data cannot
	 * be read for any reason.
	 */
    protected byte[] getImage(String url, boolean logStdout) throws Throwable {
        String sID = getSessionID();
        return getImage(url, sID, logStdout);
    }

    /**
	 * Invoke local Action (in the same context) via HTTP GET
	 * preserving the same Session ID
	 * @param path Action's path, should start with /action/...
	 * @return Action response as a byte array, can be converted into a String or 
	 * used as is (in case of images or PDFs).
	 * @throws Throwable
	 */
    protected byte[] callLocalAction(String path) throws Throwable {
        return getImage(getServerBaseURL() + path, getSessionID(), false);
    }

    /**
	 * Returns base URL for retrieving images from the same host
	 * where the application is running. The programmer will need to
	 * add the rest of the path, like /action/chart or /images/logo.png, etc.
	 * @return Base URL to retrieve images from current host
	 */
    protected String getServerBaseURL() {
        String server = "http://";
        if (getRequest().isSecure()) server = "https://";
        server = server + getRequest().getServerName() + ":" + getRequest().getServerPort() + getRequest().getContextPath();
        return server;
    }
}
