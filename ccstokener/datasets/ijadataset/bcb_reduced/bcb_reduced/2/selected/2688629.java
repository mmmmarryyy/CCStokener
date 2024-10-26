package gov.fnal.mcas.servlets.base;

import gov.fnal.mcas.util.XsltTransform;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.Map;
import java.net.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

public class XqueryXsltServlet extends HttpServlet {

    public static final String DataSourceURL = "DataSourceURL";

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        try {
            Map<String, String> xsltParams = getXsltParams(request);
            String specifiedXQuery = xsltParams.get("xquery");
            String xqueryPath = getServletContext().getRealPath(specifiedXQuery);
            File xqueryFile = new File(xqueryPath);
            FileInputStream xqueryStream = new FileInputStream(xqueryFile);
            String sourceUrl = xsltParams.get(XsltTransform.ThisDataSourceURL);
            if (sourceUrl == null) sourceUrl = getDataSourceURL(request);
            sourceUrl += "?";
            for (String xsltParamName : xsltParams.keySet()) {
                sourceUrl += xsltParamName + "=" + xsltParams.get(xsltParamName) + "&";
            }
            sourceUrl = sourceUrl.substring(0, sourceUrl.length() - 1);
            URL url = new URL(sourceUrl);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            byte[] readData = new byte[1024];
            int i = xqueryStream.read(readData);
            while (i != -1) {
                wr.write(new String(readData), 0, i);
                i = xqueryStream.read(readData);
            }
            xqueryStream.close();
            wr.flush();
            Source s = new StreamSource(conn.getInputStream());
            String filePath = getServletContext().getRealPath(getXsltScript(request));
            XsltTransform.doTransform(filePath, s, xsltParams, writer);
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    public String getXsltScript(HttpServletRequest request) throws ServletException {
        String xsltName = request.getParameter("xslt");
        if (xsltName == null) {
            String msg = "Must specify name of xsl transform for command: xslt";
            throw new ServletException(msg);
        }
        return xsltName;
    }

    public Map<String, String> getXsltParams(HttpServletRequest request) {
        LinkedHashMap<String, String[]> parameterMap = new LinkedHashMap<String, String[]>(request.getParameterMap());
        LinkedHashMap<String, String> xsltParams = new LinkedHashMap<String, String>();
        for (String xsltParamName : parameterMap.keySet()) {
            xsltParams.put(xsltParamName, parameterMap.get(xsltParamName)[0]);
        }
        return xsltParams;
    }

    public String getDataSourceURL(HttpServletRequest request) throws ServletException {
        String sourceUrl = getParameterValue(request, DataSourceURL, null);
        if (sourceUrl == null) {
            String msg = "Must specify name of data source URL for command: xslt";
            throw new ServletException(msg);
        }
        return sourceUrl;
    }

    public String getParameterValue(HttpServletRequest request, String parameter_name, String default_value) {
        String parameter_value = request.getParameter(parameter_name);
        if (parameter_value == null) parameter_value = default_value;
        return parameter_value;
    }
}
