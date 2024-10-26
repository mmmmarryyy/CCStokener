package neembuuuploader.test.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author dinesh
 */
public class ScribdUploaderPlugin {

    private static URL u;

    private static HttpURLConnection uc;

    private static BufferedReader br;

    private static String doc_id = "";

    private static String session_key = "";

    public static void main(String[] args) throws Exception {
        initialize();
        fileUpload();
    }

    private static void initialize() throws Exception {
        System.out.println("Getting startup cookie from scribd.com");
        u = new URL("http://api.scribd.com/api?method=user.login&username=007007dinesh@gmail.com&password=somepassword&api_key=5t8cd1k0ww3iupw31bb2a&api_sig=sec-b27x1xqgbvhrtccudzeh0s709n");
        uc = (HttpURLConnection) u.openConnection();
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String k = "", tmp = "";
        while ((tmp = br.readLine()) != null) {
            System.out.println(tmp);
            k += tmp;
        }
        if (k.contains("<session_key>")) {
            session_key = parseResponse(k, "<session_key>", "</session_key>");
            System.out.println("session_key  : " + session_key);
        } else {
            System.out.println("scribd login failed :(");
        }
        uc.disconnect();
    }

    private static void fileUpload() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        File f = new File("E:/Projects/NU/NeembuuUploader/test/neembuuuploader/test/plugins/BayFilesUploaderPlugin.java");
        HttpPost httppost = new HttpPost("http://api.scribd.com/api?method=docs.upload&api_key=5t8cd1k0ww3iupw31bb2a&Session_key=" + session_key);
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        FileBody bin = new FileBody(f);
        reqEntity.addPart("file", bin);
        httppost.setEntity(reqEntity);
        System.out.println("Now uploading your file into scribd........ Please wait......................");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        if (resEntity != null) {
            doc_id = EntityUtils.toString(resEntity);
            System.out.println(doc_id);
            if (doc_id.contains("stat=\"ok\"")) {
                doc_id = parseResponse(doc_id, "<doc_id>", "</doc_id>");
                System.out.println("doc id :" + doc_id);
            } else {
                throw new Exception("There might be problem with your internet connection or server error. Please try again some after time :(");
            }
        } else {
            throw new Exception("There might be problem with your internet connection or server error. Please try again some after time :(");
        }
    }

    public static String parseResponse(String response, String stringStart, String stringEnd) {
        response = response.substring(response.indexOf(stringStart));
        response = response.replace(stringStart, "");
        response = response.substring(0, response.indexOf(stringEnd));
        return response;
    }
}
