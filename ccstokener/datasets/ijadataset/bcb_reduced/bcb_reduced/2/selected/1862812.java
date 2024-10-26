package br.com.devcase.servlet.autoriza;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

public class autorizacao {

    /**
	 * @param args
	 * @throws IOException 
	 * @throws HttpException 
	 */
    public static void main(String[] args) throws HttpException, IOException {
        String headerName = null;
        String t = "http://localhost:8080/access/content/group/81c8542d-3f58-48cf-ac72-9f482df47ebe/sss/QuestionarioSocioEc.pdf";
        URL url = new URL(t);
        HttpURLConnection srvletConnection = (HttpURLConnection) url.openConnection();
        srvletConnection.setDoOutput(true);
        srvletConnection.setDoInput(true);
        srvletConnection.setRequestMethod("POST");
        srvletConnection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
        String myCookie = "JSESSIONID=acab62f5-bc6a-4886-9719-e040e8af3fc6.localhost";
        srvletConnection.setRequestProperty("Cookie", myCookie);
        srvletConnection.setInstanceFollowRedirects(false);
        srvletConnection.connect();
        System.out.println(srvletConnection.getContent());
        System.out.println(srvletConnection.getContentType());
        System.out.println(srvletConnection.getContent().toString());
        System.out.println(srvletConnection.getContentLength());
        System.out.println(srvletConnection.getContentEncoding());
        DataOutputStream out2 = new DataOutputStream(srvletConnection.getOutputStream());
        out2.flush();
        out2.close();
    }
}
