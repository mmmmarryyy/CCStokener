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
import be.kuleuven.cw.peno3.model.Credential;
import be.kuleuven.cw.peno3.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public class ClientDAOTest {

    private static String hostURL = "http://localhost:9876/";

    public static void main(String[] args) {
        testListUsers();
        testaddUserJson();
    }

    private static void testaddUserJson() {
        try {
            User newUser = new User();
            newUser.setFirstName("Jelle");
            newUser.setLastName("Vloeibergs");
            Credential cred = new Credential();
            cred.setPassword("mysecret");
            cred.setUsername("jel");
            newUser.setCredential(cred);
            String jsonuser = new Gson().toJson(newUser);
            HttpClient client = new HttpClient();
            PostMethod method = new PostMethod(ClientDAOTest.hostURL + "UserHandler/addUser");
            method.addParameter("user", jsonuser);
            int returnCode = client.executeMethod(method);
            System.out.println(method.getResponseBodyAsString());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (HttpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void testListUsers() {
        try {
            String json = stringOfUrl(ClientDAOTest.hostURL + "UserHandler/listUsers");
            User[] obj2 = new Gson().fromJson(json.toString(), User[].class);
            for (User user : obj2) {
                System.out.println(user);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
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
