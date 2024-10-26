package org.personalsmartspace.lm.locationtagger.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URLConnection;
import java.util.Properties;

class HttpGetter {

    /**
	* Sends an HTTP GET request to a url
	*
	* @param endpoint - The URL of the server. (Example: " http://www.yahoo.com/search")
	* @param requestParameters - all the request parameters (Example: "param1=val1&param2=val2"). Note: This method will add the question mark (?) to the request - DO NOT add it yourself
	* @return - The response from the end point
	*/
    public static String sendGetRequest(String endpoint, String requestParameters) {
        String result = null;
        if (true) {
            try {
                Properties sysProperties = System.getProperties();
                sysProperties.put("proxyHost", Constants.PROXY);
                sysProperties.put("proxyPort", Constants.PROXYPORT);
                sysProperties.put("proxySet", Constants.PROXYENABLED);
                Authenticator.setDefault(new MyAuthenticator());
                String urlStr = endpoint;
                if (requestParameters != null && requestParameters.length() > 0) {
                    urlStr += "?" + requestParameters;
                }
                java.net.URL url = new java.net.URL(urlStr);
                java.lang.System.out.println(urlStr);
                URLConnection conn = url.openConnection();
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()), 8000);
                StringBuffer sb = new StringBuffer();
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
                rd.close();
                result = sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}

class MyAuthenticator extends Authenticator {

    public PasswordAuthentication getPasswordAuthentication() {
        System.err.println("Feeding username and password for " + getRequestingScheme());
        return (new PasswordAuthentication(Constants.USERNAME, Constants.PASSWORD.toCharArray()));
    }
}
