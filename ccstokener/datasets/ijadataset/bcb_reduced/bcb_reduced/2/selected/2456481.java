package uk.ac.leeds.amset.sword.service.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 *
 * @author clayton
 */
public class AlfrescoFoundationDepositManagerTest {

    public AlfrescoFoundationDepositManagerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Tests for a 201 Created response header from HTTP post.
     *
     * Full unit test will have to check for the creation of the file
     * in the repository.
     *
     * Requires address of collection and a file to deposit and the path to
     * a file to be deposited.
     */
    @Test
    public void depositByteArrayResponse201Test() throws IOException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://localhost:8080/alfresco/sword/deposit/company_home");
        File file = new File("/Library/Application Support/Apple/iChat Icons/Planets/Mars.gif");
        FileEntity entity = new FileEntity(file, "image/gif");
        entity.setChunked(true);
        httppost.setEntity(entity);
        Date date = new Date();
        Long time = date.getTime();
        httppost.addHeader("content-disposition", "filename=x" + time + "x.gif");
        System.out.println("Executing request...." + httppost.getRequestLine());
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        assertEquals("HTTP/1.1 201 Created", response.getStatusLine().toString());
        if (resEntity != null) {
            InputStream is = resEntity.getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = "";
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) System.out.println(line);
            }
        }
        if (resEntity != null) {
            resEntity.consumeContent();
        }
        httpclient.getConnectionManager().shutdown();
    }

    /**
     * Tests for the correct error response from an unacceptable mime type.
     *
     * Requires address of collection and a file to deposit and the path to
     * a file to be deposited.
     */
    @Test
    public void unacceptableMimeTypeTest() throws IOException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://localhost:8080/alfresco/sword/deposit/company_home");
        File file = new File("/Library/Application Support/Apple/iChat Icons/Planets/Mars.gif");
        FileEntity entity = new FileEntity(file, "text/xml");
        entity.setChunked(true);
        httppost.setEntity(entity);
        Date date = new Date();
        Long time = date.getTime();
        httppost.addHeader("content-disposition", "filename=x" + time + "x.gif");
        System.out.println("Executing request...." + httppost.getRequestLine());
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        if (resEntity != null) {
            InputStream is = resEntity.getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = "";
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) System.out.println(line);
            }
        }
        if (resEntity != null) {
            resEntity.consumeContent();
        }
        httpclient.getConnectionManager().shutdown();
    }
}
