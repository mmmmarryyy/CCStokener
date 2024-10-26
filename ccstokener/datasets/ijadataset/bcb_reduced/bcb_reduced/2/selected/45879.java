package juploader.upload.plugin.yourfilelink;

import juploader.httpclient.FileUploadRequest;
import juploader.httpclient.HttpClient;
import juploader.httpclient.HttpResponse;
import org.apache.commons.io.IOUtils;
import java.io.File;

/** @author Adam Pawelec */
public class Main {

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.createInstance();
        FileUploadRequest request = client.createFileUploadRequest();
        request.setUrl("http://www.yourfilelink.com/upload.php");
        request.setFile("attached", new File("/home/proktor/a.png"));
        request.addParameter("agree", "yes");
        request.addParameter("terms", "yes");
        request.addParameter("action", "upload_process");
        HttpResponse response = client.execute(request);
        System.out.println(response.getResponseCode());
        System.out.println(IOUtils.toString(response.getResponseBody()));
    }
}
