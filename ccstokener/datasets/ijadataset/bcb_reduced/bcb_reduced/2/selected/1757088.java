package org.seqtagutils.util.web;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.io.IOUtils;
import org.seqtagutils.util.CException;
import org.seqtagutils.util.CFileHelper;
import org.seqtagutils.util.CStringHelper;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

public class CHttpHelper {

    private CHttpHelper() {
    }

    public static String getRequest(String url, Map<String, Object> model) {
        CHttpRequest request = new CHttpRequest();
        return request.getRequest(url, model);
    }

    public static String getRequest(String url, Object... args) {
        Map<String, Object> params = CStringHelper.createMap(args);
        return getRequest(url, params);
    }

    public static String postRequest(String url, Map<String, Object> params) {
        CPostHttpRequest request = new CPostHttpRequest();
        return request.getRequest(url, params);
    }

    public static String postRequest(String url, Object... args) {
        Map<String, Object> params = CStringHelper.createMap(args);
        CPostHttpRequest request = new CPostHttpRequest();
        return request.getRequest(url, params);
    }

    public static BufferedImage postToImageGenerator(String url, Map<String, Object> model) {
        CPostHttpRequest request = new CPostHttpRequest();
        return request.postToImageGenerator(url, model);
    }

    public static String postRedirectRequest(String url, Map<String, Object> model) {
        CPostHttpRequest request = new CPostHttpRequest();
        return request.getRequest(url, model);
    }

    public static String postMultipartRequest(String url, Map<String, Object> model) {
        Map<String, Object> files = Collections.emptyMap();
        return postMultipartRequest(url, model, files);
    }

    public static String postMultipartRequest(String url, Map<String, Object> model, Map<String, Object> files) {
        PostMethod method = null;
        try {
            HttpClient client = new HttpClient();
            method = new PostMethod(url);
            Part[] parts = new Part[model.size() + files.size()];
            int index = 0;
            for (String name : model.keySet()) {
                String value = (String) model.get(name);
                parts[index] = new StringPart(name, value);
                index++;
            }
            for (String name : files.keySet()) {
                String filename = (String) files.get(name);
                File file = new File(filename);
                parts[index] = new FilePart(name, file);
                index++;
            }
            method.setRequestEntity(new MultipartRequestEntity(parts, method.getParams()));
            method.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
            int statusCode = client.executeMethod(method);
            if (statusCode == HttpStatus.SC_OK) {
                return CHttpRequest.getResponseBody(method);
            } else if (statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                Header header = method.getResponseHeader("Location");
                return header.getValue();
            } else {
                CStringHelper.println("Method failed: " + method.getStatusLine());
                return method.getStatusLine().toString();
            }
        } catch (HttpException e) {
            CStringHelper.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
            throw new CException(e);
        } catch (IOException e) {
            CStringHelper.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
            throw new CException(e);
        } finally {
            method.releaseConnection();
        }
    }

    public static class CHttpRequest {

        protected Integer statusCode;

        public String getRequest(String url) {
            return getRequest(url, new HashMap<String, Object>());
        }

        public String getRequest(String url, Map<String, Object> model) {
            HttpClient client = new HttpClient();
            HttpMethod method = createMethod(url, model);
            method.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
            try {
                this.statusCode = client.executeMethod(method);
                if (this.statusCode == HttpStatus.SC_OK) return onOk(method); else if (this.statusCode == HttpStatus.SC_MOVED_TEMPORARILY) return onRedirect(method); else return onOther(method);
            } catch (HttpException e) {
                throw new CException(e);
            } catch (IOException e) {
                throw new CException(e);
            } finally {
                method.releaseConnection();
            }
        }

        public Integer getStatusCode() {
            return this.statusCode;
        }

        public void setStatusCode(Integer statusCode) {
            this.statusCode = statusCode;
        }

        protected HttpMethod createMethod(String url, Map<String, Object> model) {
            char separator = (url.indexOf('?') == -1) ? '?' : '&';
            url = url + separator + CWebHelper.createQueryString(model);
            return new GetMethod(url);
        }

        protected String onOk(HttpMethod method) {
            return getResponseBody(method);
        }

        protected String onRedirect(HttpMethod method) {
            CStringHelper.println("OnRedirect: " + method.getStatusLine());
            Header header = method.getResponseHeader("location");
            if (header == null) throw new CException("the response is invalid and did not provide the new location for the resource");
            return header.getValue();
        }

        protected String onOther(HttpMethod method) {
            String response = getResponseBody(method);
            CStringHelper.println("Method failed: " + method.getStatusLine() + ", " + this.statusCode + ", response=" + response);
            return response;
        }

        protected static String getResponseBody(HttpMethod method) {
            try {
                InputStream input = method.getResponseBodyAsStream();
                return IOUtils.toString(input, CFileHelper.ENCODING.toString());
            } catch (IOException e) {
                throw new CException(e);
            }
        }
    }

    public static class CPostHttpRequest extends CHttpRequest {

        @Override
        protected HttpMethod createMethod(String url, Map<String, Object> model) {
            PostMethod method = new PostMethod(url);
            NameValuePair[] data = getNameValuePairs(model);
            method.setRequestBody(data);
            return method;
        }

        private static NameValuePair[] getNameValuePairs(Map<String, Object> model) {
            if (model == null) return new NameValuePair[0];
            NameValuePair[] data = new NameValuePair[model.size()];
            int index = 0;
            for (String name : model.keySet()) {
                Object value = model.get(name);
                data[index] = new NameValuePair(name, value.toString());
                index++;
            }
            return data;
        }

        public BufferedImage postToImageGenerator(String url, Map<String, Object> model) {
            HttpClient client = new HttpClient();
            HttpMethod method = createMethod(url, model);
            try {
                this.statusCode = client.executeMethod(method);
                if (this.statusCode == HttpStatus.SC_OK) {
                    InputStream stream = method.getResponseBodyAsStream();
                    return ImageIO.read(stream);
                } else throw new CException("returned statusCode: " + this.statusCode);
            } catch (HttpException e) {
                throw new CException(e);
            } catch (IOException e) {
                throw new CException(e);
            } finally {
                method.releaseConnection();
            }
        }
    }

    public static String simplePostRequest(String url) {
        return simplePostRequest(url, new HashMap<String, Object>());
    }

    public static String simplePostRequest(String path, Map<String, Object> model) {
        try {
            URL url = new URL(path);
            URLConnection con = url.openConnection();
            con.setDoOutput(true);
            OutputStream out = con.getOutputStream();
            OutputStream bout = new BufferedOutputStream(out);
            OutputStreamWriter writer = new OutputStreamWriter(bout);
            boolean first = true;
            for (String name : model.keySet()) {
                String value = (String) model.get(name);
                if (!first) {
                    writer.write("&");
                    first = false;
                }
                writer.write(name + "=" + value);
            }
            writer.flush();
            writer.close();
            InputStream stream = new BufferedInputStream(con.getInputStream());
            Reader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder buffer = new StringBuilder();
            for (int c = reader.read(); c != -1; c = reader.read()) {
                buffer.append((char) c);
            }
            return buffer.toString();
        } catch (MalformedURLException e) {
            throw new CException(e);
        } catch (IOException e) {
            throw new CException(e);
        }
    }

    public static void getFtpFiles(String server, String folder, List<String> filenames, String destination) {
        for (String filename : filenames) {
            String url = server + folder + filename;
            String outfile = destination + filename;
            getFtpFile(url, outfile);
        }
    }

    public static void getFtpFile(String url, String outfile) {
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            in = new BufferedInputStream(new URL(url).openStream());
            out = new BufferedOutputStream(new FileOutputStream(outfile), 1024);
            byte[] data = new byte[1024];
            int x = 0;
            while ((x = in.read(data, 0, 1024)) >= 0) {
                out.write(data, 0, x);
            }
        } catch (Exception e) {
            throw new CException(e);
        } finally {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
            } catch (Exception e) {
            }
        }
    }

    public static String getOriginalFilename(HttpServletRequest request, String name) {
        if (!(request instanceof MultipartHttpServletRequest)) throw new CException("request is not an instance of MultipartHttpServletRequest");
        MultipartHttpServletRequest multipart = (MultipartHttpServletRequest) request;
        CommonsMultipartFile file = (CommonsMultipartFile) multipart.getFileMap().get(name);
        return file.getOriginalFilename();
    }
}
