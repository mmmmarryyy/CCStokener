package be.kuleuven.cw.peno3.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public class ContactMomentDAOTest {

    private static String hostURL = "http://localhost:9876/";

    public static void main(String[] args) {
        test();
    }

    private static void test() {
        try {
            System.out.println("ContactMomentDAOTest::test()");
            HttpClient client = new HttpClient();
            PostMethod method = new PostMethod(ContactMomentDAOTest.hostURL + "ContactHandler/geefContactMomenten");
            method.addParameter("vakId", "2");
            int returnCode = client.executeMethod(method);
            System.out.println(method.getResponseBodyAsString());
            System.out.println("tot hier");
        } catch (IllegalArgumentException e) {
            System.out.println("2");
            e.printStackTrace();
        } catch (HttpException e) {
            System.out.println("3");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("4");
            e.printStackTrace();
        }
    }

    public static String stringOfUrl(String addr) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        URL url = new URL(addr);
        IOUtils.copy(url.openStream(), output);
        return output.toString();
    }

    public static String streamToString(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IOUtils.copy(stream, output);
        return output.toString();
    }
}
