package com.columboid.testharness.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import com.columboid.protocol.syncml.helper.JaxbHelper;
import com.columboid.protocol.syncml.representation.SyncML;
import com.columboid.testharness.util.Log4j;
import com.columboid.testharness.util.SyncMLHandler;

public class HttpConnection implements Connection {

    /**
	 * Post Xml request to the server
	 */
    public String postXmlRequest(String url, String data) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(url);
        StringBuffer responseStr = new StringBuffer();
        try {
            System.out.println(data);
            Log4j.logger.info("Request:\n" + data);
            StringEntity reqEntity = new StringEntity(data, "UTF-8");
            reqEntity.setContentType("text/xml");
            httppost.setEntity(reqEntity);
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            this.setPostSatus(response.getStatusLine().getStatusCode());
            BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                responseStr.append(line + "\n");
            }
            if (entity != null) {
                entity.consumeContent();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println(responseStr);
        Log4j.logger.info("Response:\n" + responseStr);
        return responseStr.toString();
    }

    /**
	 * Parse response string to SyncML object.
	 */
    public SyncML getXmlResponse(String data) {
        SyncML sml = null;
        String xsd = JaxbHelper.getSyncMLSchema();
        StringReader xmlReader = new StringReader(data);
        StringReader xsdReader = new StringReader(xsd);
        try {
            sml = SyncMLHandler.CreateSymcMLObject(xmlReader, xsdReader);
        } catch (Exception e) {
            e.printStackTrace();
            Log4j.logger.error(e.toString());
        } finally {
            if (xmlReader != null) {
                xmlReader.close();
                xmlReader = null;
            }
            if (xsdReader != null) {
                xsdReader.close();
                xsdReader = null;
            }
        }
        return sml;
    }

    public int getPostSatus() {
        return this.Status;
    }

    public void setPostSatus(int status) {
        this.Status = status;
    }

    int Status;
}
