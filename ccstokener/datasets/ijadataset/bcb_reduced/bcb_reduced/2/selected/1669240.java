package org.azrul.mewit.client;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.azrul.epice.domain.Person;
import org.azrul.epice.rest.dto.LoginRequest;
import org.azrul.epice.rest.dto.LoginResponse;
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
public class LoginService extends com.wavemaker.runtime.javaservice.JavaServiceSuperClass {

    public LoginService() {
        super(INFO);
    }

    public Person doLogin(String username, String password) throws UnsupportedEncodingException, IOException, ParseException, Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        LoginRequest request = new LoginRequest();
        if (username == null && password == null) {
            request.setLogin(RuntimeAccess.getInstance().getRequest().getParameter("username"));
            request.setPassword(RuntimeAccess.getInstance().getRequest().getParameter("key"));
        } else {
            request.setLogin(username);
            request.setPassword(password);
        }
        XStream xwriter = new XStream();
        xwriter.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        xwriter.alias("LoginRequest", LoginRequest.class);
        XStream xreader = new XStream();
        xreader.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        xreader.alias("LoginResponse", LoginResponse.class);
        String strRequest = URLEncoder.encode(xwriter.toXML(request), "UTF-8");
        HttpGet httpget = new HttpGet(MewitProperties.getMewitUrl() + "/resources/login?REQUEST=" + strRequest);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        LoginResponse loginResponse = null;
        if (entity != null) {
            String result = URLDecoder.decode(EntityUtils.toString(entity), "UTF-8");
            log(INFO, "XML=" + result);
            loginResponse = (LoginResponse) xreader.fromXML(result);
            if (loginResponse.getErrors() != null) {
                throw new Exception();
            } else {
                RuntimeAccess.getInstance().getSession().setAttribute("SESSION_ID", loginResponse.getSessionId());
                RuntimeAccess.getInstance().getSession().setAttribute("USER_ID", loginResponse.getPerson().getUsername());
                RuntimeAccess.getInstance().getSession().setAttribute("PERSON", loginResponse.getPerson());
                return loginResponse.getPerson();
            }
        }
        return null;
    }
}
