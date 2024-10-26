package gov.fnal.mcas.portlets;

import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.net.*;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.UnavailableException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class XqueryXsltPortlet extends gov.fnal.mcas.portlets.base.XsltPortlet {

    public String getXsltScript() {
        return "/WEB-INF/lib/null.xsl";
    }

    public Map<String, String> getXsltParams(RenderRequest request) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        String portletTitle = request.getPreferences().getValue("PortletTitle", "Portlet");
        System.out.println(portletTitle);
        params.put("PortletTitle", portletTitle);
        params.put("FunctionName", "draw" + portletTitle);
        String specifiedXQuery = request.getPreferences().getValue("xquery", "/WEB-INF/lib/null.xq");
        params.put("xquery", specifiedXQuery);
        String specifiedXSLT = request.getPreferences().getValue("xslt", "/WEB-INF/lib/null.xsl");
        params.put("xslt", specifiedXSLT);
        String specifiedUsername = request.getPreferences().getValue("username", "");
        params.put("username", specifiedUsername);
        return params;
    }

    public void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException, UnavailableException {
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        try {
            Map<String, String> xsltParams = getXsltParams(request);
            String specifiedXQuery = xsltParams.get("xquery");
            String xqueryPath = getPortletContext().getRealPath(specifiedXQuery);
            File xqueryFile = new File(xqueryPath);
            FileInputStream xqueryStream = new FileInputStream(xqueryFile);
            String sourceUrl = xsltParams.get(ThisDataSourceURL);
            if (sourceUrl == null) {
                sourceUrl = getDataSourceURL(request);
            }
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
            String specifiedXSLT = xsltParams.get("xslt");
            Transformer xsltTransformer = createXsltTransformer(specifiedXSLT);
            xsltTransformer.clearParameters();
            for (String xsltParamName : xsltParams.keySet()) {
                xsltTransformer.setParameter(xsltParamName, xsltParams.get(xsltParamName));
            }
            xsltTransformer.transform(s, new StreamResult(writer));
        } catch (Exception ex) {
            throw new PortletException(ex);
        }
    }
}
