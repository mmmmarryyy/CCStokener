package juploader.upload.plugin.glowfoto;

import juploader.httpclient.FileUploadRequest;
import juploader.httpclient.HttpClient;
import juploader.httpclient.HttpResponse;
import org.apache.commons.io.IOUtils;
import java.io.File;

/** @author Adam Pawelec */
public class Main {

    public static void main(String[] args) throws Exception {
        HttpClient httpClient = HttpClient.createInstance();
        FileUploadRequest request = httpClient.createFileUploadRequest();
        request.setUrl("http://img5.glowfoto.com/upload2.php");
        request.setFile("image", new File("/home/proktor/a.png"));
        request.addParameter("thumbsize", "200");
        HttpResponse response = httpClient.execute(request);
        System.out.println(IOUtils.toString(response.getResponseBody()));
    }
}
