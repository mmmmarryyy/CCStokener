package org.azrul.mewit.client;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.azrul.epice.domain.Person;
import org.azrul.epice.rest.dto.ModifyProfileRequest;
import org.azrul.epice.rest.dto.ModifyProfileResponse;
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
public class UpdatePersonService extends com.wavemaker.runtime.javaservice.JavaServiceSuperClass {

    public UpdatePersonService() {
        super(INFO);
    }

    public Person doUpdatePerson(Person person) throws UnsupportedEncodingException, IOException {
        String sessionId = (String) RuntimeAccess.getInstance().getSession().getAttribute("SESSION_ID");
        DefaultHttpClient httpclient = new DefaultHttpClient();
        ModifyProfileRequest request = new ModifyProfileRequest();
        request.setPerson(person);
        request.setSessionId(sessionId);
        XStream writer = new XStream();
        writer.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        writer.alias("ModifyProfileRequest", ModifyProfileRequest.class);
        XStream reader = new XStream();
        reader.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        reader.alias("ModifyProfileResponse", ModifyProfileResponse.class);
        String strRequest = URLEncoder.encode(reader.toXML(request), "UTF-8");
        HttpGet httpget = new HttpGet(MewitProperties.getMewitUrl() + "/resources/modifyProfile?REQUEST=" + strRequest);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            String result = URLDecoder.decode(EntityUtils.toString(entity), "UTF-8");
            ModifyProfileResponse modifyProfileResponse = (ModifyProfileResponse) reader.fromXML(result);
            return modifyProfileResponse.getPerson();
        }
        return null;
    }
}
