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
import org.azrul.epice.rest.dto.ModifyPasswordRequest;
import org.azrul.epice.rest.dto.ModifyPasswordResponse;
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
public class ModifyPasswordService extends com.wavemaker.runtime.javaservice.JavaServiceSuperClass {

    public ModifyPasswordService() {
        super(INFO);
    }

    public Boolean doModifyPassword(String oldPassword, String newPassword) throws UnsupportedEncodingException, IOException {
        String sessionId = (String) RuntimeAccess.getInstance().getSession().getAttribute("SESSION_ID");
        DefaultHttpClient httpclient = new DefaultHttpClient();
        ModifyPasswordRequest request = new ModifyPasswordRequest();
        request.setSessionId(sessionId);
        request.setOldPassword(oldPassword);
        request.setNewPassword(newPassword);
        XStream writer = new XStream();
        writer.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        writer.alias("ModifyPasswordRequest", ModifyPasswordRequest.class);
        XStream reader = new XStream();
        reader.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        reader.alias("ModifyPasswordResponse", ModifyPasswordResponse.class);
        String strRequest = URLEncoder.encode(reader.toXML(request), "UTF-8");
        HttpPost httppost = new HttpPost(MewitProperties.getMewitUrl() + "/resources/modifyPassword?REQUEST=" + strRequest);
        HttpResponse httpresponse = httpclient.execute(httppost);
        HttpEntity entity = httpresponse.getEntity();
        if (entity != null) {
            String result = URLDecoder.decode(EntityUtils.toString(entity), "UTF-8");
            ModifyPasswordResponse modifyPasswordResponse = (ModifyPasswordResponse) reader.fromXML(result);
            if (modifyPasswordResponse.getErrors() != null) {
                return Boolean.FALSE;
            } else {
                return Boolean.TRUE;
            }
        }
        return null;
    }
}
