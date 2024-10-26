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
import org.azrul.epice.domain.Item;
import org.azrul.epice.rest.dto.GiveCommentsOnFeedbackRequest;
import org.azrul.epice.rest.dto.GiveCommentsOnFeedbackResponse;
import com.wavemaker.runtime.RuntimeAccess;

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
public class GiveCommentsOnFeedback extends com.wavemaker.runtime.javaservice.JavaServiceSuperClass {

    public GiveCommentsOnFeedback() {
        super(INFO);
    }

    public String sampleJavaOperation() {
        String result = null;
        try {
            log(INFO, "Starting sample operation");
            result = "Hello World";
            log(INFO, "Returning " + result);
        } catch (Exception e) {
            log(ERROR, "The sample java service operation has failed", e);
        }
        return result;
    }

    public Item doGiveCommentsOnFeedback(String itemId, String comments, boolean approved) throws UnsupportedEncodingException, IOException {
        log(INFO, "Give comments on feedback: Item id=" + itemId);
        String sessionId = (String) RuntimeAccess.getInstance().getSession().getAttribute("SESSION_ID");
        DefaultHttpClient httpclient = new DefaultHttpClient();
        GiveCommentsOnFeedbackRequest request = new GiveCommentsOnFeedbackRequest();
        request.setItemID(itemId);
        request.setSessionId(sessionId);
        request.setComments(comments);
        request.setApproved(approved);
        XStream writer = new XStream();
        writer.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        writer.alias("GiveCommentsOnFeedbackRequest", GiveCommentsOnFeedbackRequest.class);
        XStream reader = new XStream();
        reader.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        reader.alias("GiveCommentsOnFeedbackResponse", GiveCommentsOnFeedbackResponse.class);
        String strRequest = URLEncoder.encode(reader.toXML(request), "UTF-8");
        HttpPost httppost = new HttpPost(MewitProperties.getMewitUrl() + "/resources/giveCommentsOnFeedback?REQUEST=" + strRequest);
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            String result = URLDecoder.decode(EntityUtils.toString(entity), "UTF-8");
            GiveCommentsOnFeedbackResponse oResponse = (GiveCommentsOnFeedbackResponse) reader.fromXML(result);
            return oResponse.getItem();
        }
        return null;
    }
}
