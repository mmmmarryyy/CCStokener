package br.com.yaw.servlet.filter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;

/**
 * Filter do handle an upload
 * @author Rafael Nunes
 *
 */
public class UploadFilter implements Filter {

    private String urlImageHandle;

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        ImagesService imgService = ImagesServiceFactory.getImagesService();
        InputStream stream = request.getInputStream();
        ArrayList<Byte> bytes = new ArrayList<Byte>();
        int b = 0;
        while ((b = stream.read()) != -1) {
            bytes.add((byte) b);
        }
        byte img[] = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            img[i] = bytes.get(i);
        }
        BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
        String urlBlobstore = blobstoreService.createUploadUrl("/blobstore-servlet?action=upload");
        URL url = new URL("http://localhost:8888" + urlBlobstore);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=29772313");
        OutputStream out = connection.getOutputStream();
        out.write(img);
        out.flush();
        out.close();
        System.out.println(connection.getResponseCode());
        System.out.println(connection.getResponseMessage());
        BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String responseText = "";
        String line;
        while ((line = rd.readLine()) != null) {
            responseText += line;
        }
        out.close();
        rd.close();
        response.sendRedirect("/blobstore-servlet?action=getPhoto&" + responseText);
    }

    @Override
    public void init(FilterConfig fConfig) throws ServletException {
        urlImageHandle = fConfig.getInitParameter("urlImageHandle");
    }
}
