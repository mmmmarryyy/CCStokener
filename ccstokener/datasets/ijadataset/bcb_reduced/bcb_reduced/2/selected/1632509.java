package br.net.woodstock.rockframework.web.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import br.net.woodstock.rockframework.util.Assert;
import br.net.woodstock.rockframework.utils.FileUtils;
import br.net.woodstock.rockframework.utils.IOUtils;

public abstract class ResponseUtils {

    public static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";

    public static final String DOWNLOAD_CONTENT_TYPE = "application/force-download";

    public static final String HTML_CONTENT_TYPE = "text/html";

    public static final String TEXT_CONTENT_TYPE = "text/plain";

    public static final String XML_CONTENT_TYPE = "text/xml";

    public static final String PDF_CONTENT_TYPE = "application/pdf";

    public static final String INLINE_CONTENT_DISPOSITION = "inline";

    public static final String ATTACHMENT_CONTENT_DISPOSITION = "attachment";

    private ResponseUtils() {
    }

    public static void forward(final PageContext context, final String url) throws ServletException, IOException {
        Assert.notNull(context, "context");
        Assert.notEmpty(url, "url");
        RequestDispatcher d = context.getServletContext().getRequestDispatcher(url);
        d.forward(context.getRequest(), context.getResponse());
    }

    public static void include(final PageContext context, final String url) throws ServletException, IOException {
        Assert.notNull(context, "context");
        Assert.notEmpty(url, "url");
        RequestDispatcher d = context.getServletContext().getRequestDispatcher(url);
        d.include(context.getRequest(), context.getResponse());
    }

    public static void downloadFile(final HttpServletResponse response, final String file) throws IOException {
        Assert.notNull(response, "response");
        Assert.notEmpty(file, "file");
        File f = new File(file);
        ResponseUtils.downloadFile(response, f);
    }

    public static void downloadFile(final HttpServletResponse response, final File file) throws IOException {
        Assert.notNull(response, "response");
        Assert.notNull(file, "file");
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        InputStream inputStream = new FileInputStream(file);
        byte[] bytes = IOUtils.toByteArray(inputStream);
        String fileName = FileUtils.getName(file);
        ResponseUtils.downloadFile(response, bytes, fileName);
    }

    public static void downloadFile(final HttpServletResponse response, final URL url) throws IOException {
        Assert.notNull(response, "response");
        Assert.notNull(url, "url");
        InputStream inputStream = url.openStream();
        byte[] bytes = IOUtils.toByteArray(inputStream);
        String fileName = FileUtils.getName(url);
        ResponseUtils.downloadFile(response, bytes, fileName);
    }

    public static void downloadFile(final HttpServletResponse response, final byte[] bytes, final String fileName) throws IOException {
        Assert.notNull(response, "response");
        Assert.notNull(bytes, "bytes");
        Assert.notEmpty(fileName, "fileName");
        response.setContentType(ResponseUtils.DOWNLOAD_CONTENT_TYPE);
        response.setContentLength(bytes.length);
        response.setHeader(ResponseUtils.CONTENT_DISPOSITION_HEADER, ResponseUtils.getAttachmentContentDisposition(fileName));
        OutputStream output = response.getOutputStream();
        output.write(bytes);
    }

    public static String getAttachmentContentDisposition(final String fileName) {
        return "attachment; filename=\"" + fileName + "\"";
    }

    public static String getInLineContentDisposition(final String fileName) {
        return "inline; filename=\"" + fileName + "\"";
    }
}
