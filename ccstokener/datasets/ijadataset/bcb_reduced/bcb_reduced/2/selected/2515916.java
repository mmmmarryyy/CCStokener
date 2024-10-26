package org.azrul.mewit.client;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.azrul.epice.rest.dto.RetrievePasswordRequest;
import org.azrul.epice.rest.dto.RetrievePasswordResponse;

/**
 * This is a client-facing service class.  All
 * public methods will be exposed to the client.  Their return
 * values and parameters will be passed to the client or taken
 * from the client, respectively.  This will be a singleton
 * instance, shared between all requests. 
 * 
 * To log, call the superclass method log(LOG_LEVEL, String) or log(LOG_LEVEL, String, Exception).
 * LOG_LEVEL is one of FATAL, ERROR, WARN, INFO and DEBUG to modify your log level.
 * For info on these levels, look for tomcat/log4j documentation
 */
public class RetrievePasswordService extends com.wavemaker.runtime.javaservice.JavaServiceSuperClass {

    public RetrievePasswordService() {
        super(INFO);
    }

    public void doRetrievePassword(String email) throws UnsupportedEncodingException, IOException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        RetrievePasswordRequest request = new RetrievePasswordRequest();
        request.setEmail(email);
        XStream writer = new XStream();
        writer.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        writer.alias("RetrievePasswordRequest", RetrievePasswordRequest.class);
        XStream reader = new XStream();
        reader.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        reader.alias("RetrievePasswordResponse", RetrievePasswordResponse.class);
        String strRequest = URLEncoder.encode(reader.toXML(request), "UTF-8");
        log(INFO, strRequest);
        HttpPost httppost = new HttpPost(MewitProperties.getMewitUrl() + "/resources/retrivePassword?REQUEST=" + strRequest);
        HttpResponse httpresponse = httpclient.execute(httppost);
        HttpEntity entity = httpresponse.getEntity();
        if (entity != null) {
            String result = URLDecoder.decode(EntityUtils.toString(entity), "UTF-8");
            log(INFO, result);
            RetrievePasswordResponse oResponse = (RetrievePasswordResponse) reader.fromXML(result);
        }
    }
}
