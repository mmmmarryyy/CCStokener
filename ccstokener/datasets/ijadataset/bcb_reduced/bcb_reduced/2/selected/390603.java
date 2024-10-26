package com.netx.ebs;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import com.netx.generics.basic.Checker;
import com.netx.generics.basic.Context;
import com.netx.generics.basic.RUN_MODE;
import com.netx.generics.basic.ErrorListException;
import com.netx.generics.io.Streams;
import com.netx.generics.time.Date;
import com.netx.generics.time.Moment;

class EbsResponseImpl extends HttpServletResponseWrapper implements EbsResponse {

    private final ServletContext _srvCtx;

    private final EbsContext _ebsCtx;

    EbsResponseImpl(HttpServletResponse response, ServletContext srvCtx, EbsContext ebsCtx) {
        super(response);
        _srvCtx = srvCtx;
        _ebsCtx = ebsCtx;
    }

    public ServletContext getServletContext() {
        return _srvCtx;
    }

    public EbsContext getEbsContext() {
        return _ebsCtx;
    }

    public void setContentType(String contentType) {
        Checker.checkEmpty(contentType, "contentType");
        if (contentType.startsWith("text")) {
            if (contentType.indexOf("charset") != -1) {
                super.setContentType(contentType);
            } else {
                super.setContentType(contentType + "; charset=" + _ebsCtx.getCharset());
            }
        }
    }

    public void sendDisableCache() {
        setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
        setDateHeader("Last-Modified", new Moment().getTimeInMilliseconds());
        setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        addHeader("Cache-Control", "post-check=0, pre-check=0");
        setHeader("pragma", "no-cache");
    }

    public void sendEnableCache() {
        if (Context.getRootContext().getRunMode() == RUN_MODE.DEV) {
            sendDisableCache();
        } else {
            Date shift = new Moment().getDate();
            shift.setYear(shift.getYear() + 1);
            Moment future = new Moment(shift);
            setDateHeader("Expires", future.getTimeInMilliseconds());
            setHeader("Last-Modified", "Mon, 26 Jul 1997 05:00:00 GMT");
        }
    }

    public void sendTemplate(String filename, TemplateValues values) throws IOException {
        Checker.checkEmpty(filename, "filename");
        Checker.checkNull(values, "values");
        URL url = _getFile(filename);
        boolean writeSpaces = Context.getRootContext().getRunMode() == RUN_MODE.DEV ? true : false;
        Template t = new Template(url.openStream(), writeSpaces);
        try {
            t.write(getWriter(), values);
        } catch (ErrorListException ele) {
            Context.getRootContext().getLogger().error(ele);
        }
    }

    public void sendTextFile(String filename) throws IOException {
        Checker.checkEmpty(filename, "filename");
        URL url = _getFile(filename);
        PrintWriter out = getWriter();
        Streams.copy(new InputStreamReader(url.openStream()), out);
        out.close();
    }

    public void sendBinaryFile(String filename) throws IOException {
        Checker.checkEmpty(filename, "filename");
        URL url = _getFile(filename);
        OutputStream out = getOutputStream();
        Streams.copy(url.openStream(), out);
        out.close();
    }

    public void logout() throws IOException {
        sendRedirect(_ebsCtx.getAuthenticatorName() + "?action=logout");
    }

    public void addCookie(Cookie cookie) {
        _checkCommitted();
        super.addCookie(cookie);
    }

    public void addDateHeader(String name, long date) {
        _checkCommitted();
        super.addDateHeader(name, date);
    }

    public void addHeader(String name, String value) {
        _checkCommitted();
        super.addHeader(name, value);
    }

    public void addIntHeader(String name, int value) {
        _checkCommitted();
        super.addIntHeader(name, value);
    }

    public void setDateHeader(String name, long date) {
        _checkCommitted();
        super.setDateHeader(name, date);
    }

    public void setHeader(String name, String value) {
        _checkCommitted();
        super.setHeader(name, value);
    }

    public void setIntHeader(String name, int value) {
        _checkCommitted();
        super.setIntHeader(name, value);
    }

    private void _checkCommitted() {
        if (isCommitted()) {
            throw new IllegalStateException();
        }
    }

    private URL _getFile(String filename) throws IOException {
        URL url = getServletContext().getResource("/" + filename);
        if (url == null) {
            throw new IllegalArgumentException(filename + ": file does not exist");
        } else {
            return url;
        }
    }
}
