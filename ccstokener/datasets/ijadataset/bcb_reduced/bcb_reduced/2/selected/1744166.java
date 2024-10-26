package com.inet.qlcbcc.ws.client.support;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import org.springframework.stereotype.Component;
import org.webos.core.util.IoUtils;
import com.inet.qlcbcc.ws.client.AbstractWebServiceClient;
import com.inet.qlcbcc.ws.client.WebOsFileDownloadWebServiceClient;
import com.inet.qlcbcc.ws.client.WebServiceClientException;

/**
 * WebOsFileDownloadWebServiceClientSupport.
 *
 * @author Duyen Tang
 * @version $Id: WebOsFileDownloadWebServiceClientSupport.java Aug 15, 2011 10:48:06 AM tttduyen $
 *
 * @since 1.0
 */
@Component("webOsFileDownloadWebServiceClientSupport")
public class WebOsFileDownloadWebServiceClientSupport extends AbstractWebServiceClient implements WebOsFileDownloadWebServiceClient {

    public ByteArrayOutputStream download(final String contentUuid) throws WebServiceClientException {
        try {
            URL url = new URL(getPath("/download/" + contentUuid));
            URLConnection connection = url.openConnection();
            InputStream inputStream = connection.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int c;
            while ((c = inputStream.read()) != -1) {
                outputStream.write(c);
            }
            inputStream.close();
            return outputStream;
        } catch (Exception ex) {
            throw new WebServiceClientException("Could not download content from web service.", ex);
        }
    }

    public void download(String contentUuid, File path) throws WebServiceClientException {
        try {
            URL url = new URL(getPath("/download/" + contentUuid));
            URLConnection connection = url.openConnection();
            InputStream inputStream = connection.getInputStream();
            OutputStream output = new FileOutputStream(path);
            IoUtils.copyBytes(inputStream, output);
            IoUtils.close(inputStream);
            IoUtils.close(output);
        } catch (IOException ioex) {
            throw new WebServiceClientException("Could not download or saving content to path [" + path.getAbsolutePath() + "]", ioex);
        } catch (Exception ex) {
            throw new WebServiceClientException("Could not download content from web service.", ex);
        }
    }

    protected String getRelativePath() {
        return "/ws/file";
    }
}
