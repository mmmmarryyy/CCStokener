package com.google.code.maven.plugin.http.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.logging.Log;

/**
 * FormCredential
 * 
 * @author Nadim Benabdenbi
 * @version 1.0
 * @since JDK1.6
 */
public class FormCredentials extends Credentials {

    /**
	 * unique serial version identifier.
	 */
    private static final long serialVersionUID = 1L;

    /**
	 * @parameter
	 * @required
	 */
    private String formUrl;

    /**
	 * @parameter
	 * @required
	 */
    private String loginParameterName;

    /**
	 * @parameter
	 * @required
	 */
    private String passwordParameterName;

    /**
	 * @parameter the additional form parameters
	 */
    private Parameter parameters[];

    /**
	 * formUrl getter.
	 * 
	 * @return the formUrl
	 */
    public String getFormUrl() {
        return formUrl;
    }

    /**
	 * formUrl setter.
	 * 
	 * @param formUrl
	 *            the formUrl to set
	 */
    public void setFormUrl(String formUrl) {
        this.formUrl = formUrl;
    }

    /**
	 * loginParameterName getter.
	 * 
	 * @return the loginParameterName
	 */
    public String getLoginParameterName() {
        return loginParameterName;
    }

    /**
	 * loginParameterName setter.
	 * 
	 * @param loginParameterName
	 *            the loginParameterName to set
	 */
    public void setLoginParameterName(String loginParameterName) {
        this.loginParameterName = loginParameterName;
    }

    /**
	 * passwordParameterName getter.
	 * 
	 * @return the passwordParameterName
	 */
    public String getPasswordParameterName() {
        return passwordParameterName;
    }

    /**
	 * passwordParameterName setter.
	 * 
	 * @param passwordParameterName
	 *            the passwordParameterName to set
	 */
    public void setPasswordParameterName(String passwordParameterName) {
        this.passwordParameterName = passwordParameterName;
    }

    /**
	 * @return the parameters
	 */
    public Parameter[] getParameters() {
        return parameters;
    }

    /**
	 * @param parameters
	 *            the parameters to set
	 */
    public void setParameters(Parameter parameters[]) {
        this.parameters = parameters;
    }

    /**
	 * @param httpclient
	 * @param beanResolver
	 * @return
	 * @throws IOException
	 */
    public void authenticate(DefaultHttpClient httpclient, Log log) throws IOException {
        HttpPost httpPost = new HttpPost(formUrl);
        UsernamePasswordCredentials credentials = toUsernamePasswordCredentials();
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new Parameter(loginParameterName, credentials.getUserName()).toNameValuePair());
        nvps.add(new Parameter(passwordParameterName, credentials.getPassword()).toNameValuePair());
        for (Parameter parameter : parameters) {
            nvps.add(parameter.toNameValuePair());
        }
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        HttpResponse response = httpclient.execute(httpPost);
        EntityUtils.consume(response.getEntity());
        log.info("form authentication submitted to " + formUrl);
    }
}
