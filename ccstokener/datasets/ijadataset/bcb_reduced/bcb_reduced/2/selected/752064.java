package com.xmlap.jrp.tag;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.JspFragment;
import com.xmlap.jrp.JRPResponseWrapper;
import com.xmlap.jrp.transform.JSONTransformer;

public class CallTag extends JRPTag {

    private String url;

    private String context;

    private String jrp;

    private String method;

    private Object params;

    private String var;

    private int scope = PageContext.PAGE_SCOPE;

    public void setUrl(String url) {
        this.url = url;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setJrp(String jrp) {
        this.jrp = jrp;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setParams(Object params) {
        this.params = params;
    }

    public void setVar(String var) {
        this.var = var;
    }

    public void setScope(String scope) {
        this.scope = getScopeInt(scope);
    }

    protected void doRpcExec() throws JspException, IOException {
        if (jrp != null) {
            if (url != null) {
                throw new JspException("JRP Call tag error: both jrp and url specified.");
            } else {
                handleJrp();
            }
        } else {
            if (url == null) {
                throw new JspException("JRP Call tag error: neither url and jrp specified.");
            } else {
                handleURL();
            }
        }
    }

    private void handleJrp() throws JspException, IOException {
        RequestDispatcher req_dis = prepareRequestDispatcher();
        if (req_dis == null) {
            returnErrorResult("RequestDispatcher doesn't exist for context: " + context + ", jrp: " + jrp);
        }
        List params_list = prepareParams();
        PageContext pc = (PageContext) getJspContext();
        HttpServletRequest req = (HttpServletRequest) pc.getRequest();
        Object old_method = req.getAttribute(JRP_METHOD);
        Object old_params = req.getAttribute(JRP_PARAMS);
        Object old_id = req.getAttribute(JRP_ID);
        Object old_result = req.getAttribute(JRP_RESULT);
        Object old_error = req.getAttribute(JRP_ERROR);
        Object old_mode = req.getAttribute(JRP_RUNTIME_MODE);
        req.setAttribute(JRP_METHOD, method);
        req.setAttribute(JRP_PARAMS, params_list);
        req.setAttribute(JRP_ID, "id");
        req.setAttribute(JRP_RESULT, null);
        req.setAttribute(JRP_ERROR, null);
        req.setAttribute(JRP_RUNTIME_MODE, new Integer(MODE_RPC_EXEC));
        HttpServletResponse res = (HttpServletResponse) pc.getResponse();
        JRPResponseWrapper res_wrapper = new JRPResponseWrapper(res);
        try {
            req_dis.include(req, res_wrapper);
        } catch (ServletException e) {
            throw new IOException(e.getMessage());
        }
        Object result = req.getAttribute(JRP_RESULT);
        Object error = req.getAttribute(JRP_ERROR);
        req.setAttribute(JRP_METHOD, old_method);
        req.setAttribute(JRP_PARAMS, old_params);
        req.setAttribute(JRP_ID, old_id);
        req.setAttribute(JRP_RESULT, old_result);
        req.setAttribute(JRP_ERROR, old_error);
        req.setAttribute(JRP_RUNTIME_MODE, old_mode);
        handleResult(result, error);
    }

    private RequestDispatcher prepareRequestDispatcher() {
        PageContext pc = (PageContext) getJspContext();
        if (context != null) {
            ServletContext ctx = pc.getServletContext();
            ctx = ctx.getContext(context);
            if (ctx == null) return null;
            return ctx.getRequestDispatcher(jrp);
        } else {
            HttpServletRequest req = (HttpServletRequest) pc.getRequest();
            if (jrp.equals("this")) {
                return req.getRequestDispatcher(req.getServletPath());
            } else {
                return req.getRequestDispatcher(jrp);
            }
        }
    }

    private void handleURL() throws JspException, IOException {
        Map in_map = prepareInputMap();
        String in_str = JSONTransformer.serialize(in_map);
        byte[] input = in_str.getBytes("UTF-8");
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.connect();
        OutputStream os = conn.getOutputStream();
        os.write(input);
        os.close();
        InputStream is = conn.getInputStream();
        InputStreamReader reader = new InputStreamReader(is, "UTF-8");
        StringBuffer s_buf = new StringBuffer();
        char[] tmp_buf = new char[1024];
        int count;
        while ((count = reader.read(tmp_buf)) != -1) {
            if (count == 0) continue;
            s_buf.append(tmp_buf, 0, count);
        }
        reader.close();
        Map out_map = null;
        try {
            out_map = JSONTransformer.parseObject(s_buf.toString());
        } catch (ParseException e) {
            returnErrorResult(e.getMessage());
        }
        handleResultMap(out_map);
    }

    private void handleResultMap(Map result) throws IOException, JspException {
        if (result == null) {
            returnErrorResult("no result from Call tag execution");
        }
        handleResult(result.get("result"), result.get("error"));
    }

    private void handleResult(Object result, Object error) throws IOException, JspException {
        if (error != null) {
            returnErrorResult("error returned from call tag: " + error.toString());
        }
        outResult(result);
    }

    private Map prepareInputMap() throws IOException, JspException {
        List param_list = prepareParams();
        LinkedHashMap in_map = new LinkedHashMap();
        in_map.put("method", method);
        in_map.put("params", param_list);
        in_map.put("id", "id");
        return in_map;
    }

    private List prepareParams() throws IOException, JspException {
        List param_list = null;
        if (params != null) {
            if (params instanceof List) {
                param_list = (List) params;
            } else if (params instanceof Object[]) {
                param_list = Arrays.asList((Object[]) params);
            } else {
                param_list = new ArrayList();
                param_list.add(params);
            }
        } else {
            JspFragment body = getJspBody();
            StringWriter s_writer = new StringWriter();
            body.invoke(s_writer);
            try {
                param_list = JSONTransformer.parseArray(s_writer.toString());
            } catch (ParseException e) {
                returnErrorResult(e.getMessage());
            }
        }
        return param_list;
    }

    private void outResult(Object result) throws IOException {
        if (var != null) {
            getJspContext().setAttribute(var, result, scope);
        } else {
            JspWriter out = getJspContext().getOut();
            out.write(JSONTransformer.serialize(result));
        }
    }
}
