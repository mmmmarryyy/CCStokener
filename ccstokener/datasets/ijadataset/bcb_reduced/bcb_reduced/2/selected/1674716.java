package com.xmlap.jrp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.xmlap.jrp.transform.JSONTransformer;
import com.xmlap.jrp.transform.XMLRPCTransformer;

public class JRPFilter implements Filter, Constants {

    private String jrpVersion;

    private String jrpClientJS;

    public void init(FilterConfig filterConfig) throws ServletException {
        ClassLoader loader = getClass().getClassLoader();
        URL rsrc_url = loader.getResource("jrp-internal.properties");
        jrpVersion = "unknown";
        if (rsrc_url != null) {
            try {
                Properties props = new Properties();
                props.load(rsrc_url.openStream());
                jrpVersion = props.getProperty("jrp.version");
            } catch (IOException e) {
            }
        }
        URL client_url = loader.getResource("jrp-client.js");
        if (client_url != null) {
            try {
                InputStream is = client_url.openStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int count;
                byte[] buf = new byte[1024];
                while ((count = is.read(buf)) != -1) {
                    if (count == 0) continue;
                    baos.write(buf, 0, count);
                }
                jrpClientJS = baos.toString("UTF-8");
                is.close();
                baos.close();
            } catch (IOException e) {
            }
        }
        if (jrpClientJS == null) {
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest h_req = (HttpServletRequest) request;
        JRPResponseWrapper res_wrapper = new JRPResponseWrapper((HttpServletResponse) response);
        if (h_req.getMethod().equals("GET")) {
            String mode = h_req.getParameter("mode");
            if (mode == null) mode = "index";
            if (mode.equals("form")) {
                String method = h_req.getParameter("method");
                if (method == null) method = "null";
                request.setAttribute(JRP_METHOD, method);
                request.setAttribute(JRP_RUNTIME_MODE, new Integer(MODE_UI_FORM));
                chain.doFilter(request, res_wrapper);
                response.setContentType("text/html");
                PrintWriter writer = response.getWriter();
                Map result = (Map) request.getAttribute(JRP_RESULT);
                if (result.get("description") == null) {
                    result.put("description", "No description available");
                }
                markupForm(writer, result);
            } else if (mode.equals("exec")) {
                String input = h_req.getParameter("input");
                Map jrp_request = null;
                try {
                    jrp_request = JSONTransformer.parseObject(input);
                } catch (ParseException e) {
                    throw new ServletException(e);
                }
                String method = (String) jrp_request.get("method");
                List params = (List) jrp_request.get("params");
                String return_id = (String) jrp_request.get("id");
                request.setAttribute(JRP_METHOD, method);
                request.setAttribute(JRP_PARAMS, params);
                request.setAttribute(JRP_ID, return_id);
                if (method.equals("system.listMethods")) {
                    request.setAttribute(JRP_RUNTIME_MODE, new Integer(MODE_LIST_METHODS));
                } else {
                    request.setAttribute(JRP_RUNTIME_MODE, new Integer(MODE_RPC_EXEC));
                }
                chain.doFilter(request, res_wrapper);
                Map jrp_response = new LinkedHashMap();
                jrp_response.put("result", request.getAttribute(JRP_RESULT));
                jrp_response.put("error", request.getAttribute(JRP_ERROR));
                jrp_response.put("id", return_id);
                String result = JSONTransformer.serialize(jrp_response, 2);
                response.setContentType("text/plain");
                PrintWriter writer = response.getWriter();
                writer.write(result);
                writer.flush();
            } else {
                request.setAttribute(JRP_RUNTIME_MODE, new Integer(MODE_INDEX));
                chain.doFilter(request, res_wrapper);
                response.setContentType("text/html");
                PrintWriter writer = response.getWriter();
                Map result = (Map) request.getAttribute(JRP_RESULT);
                if (result.get("description") == null) {
                    result.put("description", "No description available");
                }
                markupIndex(writer, result);
            }
        } else if (h_req.getMethod().equals("POST")) {
            boolean xml_rpc = false;
            if ("text/xml".equals(h_req.getHeader("Content-Type"))) {
                xml_rpc = true;
            }
            Map jrp_request = parseJRPInput(request, xml_rpc);
            String method = (String) jrp_request.get("method");
            List params = (List) jrp_request.get("params");
            String return_id = (String) jrp_request.get("id");
            request.setAttribute(JRP_METHOD, method);
            request.setAttribute(JRP_PARAMS, params);
            request.setAttribute(JRP_ID, return_id);
            if (method.equals("system.listMethods")) {
                request.setAttribute(JRP_RUNTIME_MODE, new Integer(MODE_LIST_METHODS));
            } else {
                request.setAttribute(JRP_RUNTIME_MODE, new Integer(MODE_RPC_EXEC));
            }
            chain.doFilter(request, res_wrapper);
            Map jrp_response = new LinkedHashMap();
            jrp_response.put("result", request.getAttribute(JRP_RESULT));
            jrp_response.put("error", request.getAttribute(JRP_ERROR));
            jrp_response.put("id", return_id);
            handleJRPOutput(response, jrp_response, xml_rpc);
        } else {
            throw new ServletException("No JRP action defined for method: " + h_req.getMethod());
        }
    }

    public void destroy() {
    }

    private Map parseJRPInput(ServletRequest req, boolean xmlRpc) throws ServletException, IOException {
        Reader reader = req.getReader();
        StringBuffer in_buf = new StringBuffer();
        char[] chars = new char[1024];
        int count;
        while ((count = reader.read(chars)) != -1) {
            if (count == 0) continue;
            in_buf.append(chars, 0, count);
        }
        String input = in_buf.toString();
        Map jrp_request = null;
        try {
            if (xmlRpc) {
                jrp_request = XMLRPCTransformer.parseMethodCall(input);
            } else {
                jrp_request = JSONTransformer.parseObject(input);
            }
        } catch (ParseException e) {
            throw new ServletException(e);
        }
        return jrp_request;
    }

    private void handleJRPOutput(ServletResponse response, Map jrpRes, boolean xmlRpc) throws ServletException, IOException {
        byte[] result;
        if (xmlRpc) {
            result = XMLRPCTransformer.serializeMethodResponse(jrpRes).getBytes();
            response.setContentType("text/xml");
        } else {
            result = JSONTransformer.serialize(jrpRes).getBytes();
            response.setContentType("text/plain");
        }
        response.setContentLength(result.length);
        OutputStream os = response.getOutputStream();
        os.write(result);
        os.flush();
    }

    private void markupIndex(PrintWriter writer, Map spec) throws IOException {
        writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"");
        writer.println("          \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        writer.println("<html>");
        writer.println("<head>");
        writer.println("<title>" + spec.get("name") + "</title>");
        writer.println("<style type='text/css'>");
        writer.println("");
        writer.println("body {");
        writer.println("  margin-left: 15;");
        writer.println("  margin-right: 15;");
        writer.println("  font-family: sans-serif;");
        writer.println("}");
        writer.println("");
        writer.println("h1, h2, h3, h4, h5 {");
        writer.println("  font-family: 'Trebuchet MS' cursive;");
        writer.println("}");
        writer.println("");
        writer.println("</style>");
        writer.println("</head>");
        writer.println("<body>");
        writer.println("");
        writer.println("<h1>" + spec.get("name") + "</h1>");
        writer.println("");
        writer.println("<p>" + spec.get("description") + "</p>");
        writer.println("");
        writer.println("<h2>RPC Methods in service</h2>");
        writer.println("<dl>");
        List methods = (List) spec.get("methods");
        for (int i = 0; i < methods.size(); i++) {
            String[] entry = (String[]) methods.get(i);
            String method = entry[0];
            String args = entry[1];
            String desc = entry[2];
            writer.println("  <dt><a href='" + spec.get("name") + "?mode=form&method=" + URLEncoder.encode(method, "UTF-8") + "'>" + method + args + "</a></dt>");
            writer.println("  <dd>" + desc + "</dd>");
        }
        writer.println("</dl>");
        writer.println("");
        writer.println("<hr />");
        writer.println("<p style='font-size: 80%; text-align: center;'><a href='http://www.xmlap.com/jrp'>JRP</a> version " + jrpVersion + "</p>");
        writer.println("");
        writer.println("</body>");
        writer.println("</html>");
    }

    private void markupForm(PrintWriter writer, Map spec) throws IOException {
        writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"");
        writer.println("          \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        writer.println("<html>");
        writer.println("<head>");
        writer.println("<title>" + spec.get("method") + "</title>");
        writer.println("<style type='text/css'>");
        writer.println("");
        writer.println("body {");
        writer.println("  margin-left: 15;");
        writer.println("  margin-right: 15;");
        writer.println("  font-family: sans-serif;");
        writer.println("}");
        writer.println("");
        writer.println("h1, h2, h3, h4, h5 {");
        writer.println("  font-family: 'Trebuchet MS' cursive;");
        writer.println("}");
        writer.println("");
        writer.println("</style>");
        if (jrpClientJS != null) {
            writer.println("<script type='text/javascript'>");
            writer.print(jrpClientJS);
            writer.println("</script>");
        }
        writer.println("<script type='text/javascript'>");
        writer.println("");
        writer.println("jsonrpc_client = new JSONRpcClient ('" + spec.get("name") + "');");
        writer.println("");
        writer.println("function invoke (method, theForm) {");
        writer.println("");
        writer.println("    var args = new Array (theForm.size.value);");
        writer.println("    for (var i=0; i < theForm.size.value; i++) {");
        writer.println("        args[i] = theForm['param' + i].value;");
        writer.println("    }");
        writer.println("");
        writer.println("    var req = jsonrpc_client.makeRequest (method, args);");
        writer.println("    var result = jsonrpc_client.sendRequest (req);");
        writer.println("    theForm.result.value = toJSON (result);");
        writer.println("}");
        writer.println("</script>");
        writer.println("</head>");
        writer.println("<body>");
        writer.println("<h1>" + spec.get("method") + "</h1>");
        writer.println("<p>" + spec.get("description") + "</p>");
        writer.println("");
        writer.println("<form>");
        writer.println("<table cellpadding='5'>");
        List args = (List) spec.get("args");
        for (int i = 0; i < args.size(); i++) {
            String[] entry = (String[]) args.get(i);
            String arg = entry[0];
            String desc = entry[1];
            writer.println("  <tr>");
            writer.println("    <th>" + arg + "</th>");
            writer.println("    <td><input name='param" + i + "' /></td>");
            writer.println("    <td colspan='2'>" + desc + "</td>");
            writer.println("  </tr>");
        }
        writer.println("  <tr>");
        writer.println("    <td><input type='hidden' name='size' value='" + args.size() + "' /></td>");
        writer.println("    <td colspan='2'><input type='button' value='Execute' onclick=\"invoke ('" + spec.get("method") + "', this.form)\" /></td>");
        writer.println("  </tr>");
        writer.println("  <tr>");
        writer.println("    <th valign='top'>result</th>");
        writer.println("    <td colspan='2'><textarea name='result' rows='10' cols='40'></textarea></td>");
        writer.println("  </tr>");
        writer.println("</table>");
        writer.println("</form>");
        writer.println("<a href='" + spec.get("name") + "'>Methods Index</a>");
        writer.println("");
        writer.println("<hr />");
        writer.println("<p style='font-size: 80%; text-align: center;'><a href='http://www.xmlap.com/jrp'>JRP</a> version " + jrpVersion + "</p>");
        writer.println("");
        writer.println("</body>");
        writer.println("</html>");
    }
}
