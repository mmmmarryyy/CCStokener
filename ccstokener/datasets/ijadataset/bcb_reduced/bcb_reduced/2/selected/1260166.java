package com.jframework.module.usps.Services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.jframework.module.usps.beans.USPSBaseResponseObject;
import com.jframework.module.usps.beans.USPSBaseRequestObject;
import com.jframework.module.usps.utils.BeanXMLMapping;

public abstract class USPSBaseService implements USPSService {

    static Log log = LogFactory.getLog(USPSBaseService.class);

    protected BeanXMLMapping xmlMapping = null;

    protected String URL = null;

    protected USPSBaseRequestObject requestObject = null;

    protected String apiName = null;

    public void setAPIName(String apiName) {
        this.apiName = apiName;
    }

    public void initialize(String uspsURL, String dtdResourceURL) {
        if (null != uspsURL) {
            this.URL = uspsURL;
            this.xmlMapping = new BeanXMLMapping();
            URL dtdURL = this.getClass().getResource(dtdResourceURL);
            this.xmlMapping.setDtdURL(dtdURL);
        }
    }

    protected String sendHTTPPost(String urlString, String postData, boolean followRedirect) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setInstanceFollowRedirects(followRedirect);
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setUseCaches(false);
        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
        wr.write(postData);
        wr.flush();
        StringBuffer response = new StringBuffer();
        BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            response.append(line);
        }
        wr.close();
        rd.close();
        String responseString = response.toString();
        if (responseString.equals("") && !followRedirect) {
            responseString = con.getHeaderField("Location");
        }
        if (log.isDebugEnabled()) log.debug("Response string: " + responseString);
        return responseString;
    }

    /**
     * @return the requestObject
     */
    public USPSBaseRequestObject getRequestObject() {
        return requestObject;
    }

    /**
     * @param requestObject the requestObject to set
     */
    public void setRequestObject(USPSBaseRequestObject requestObject) {
        this.requestObject = requestObject;
    }

    /**
     * @return the uRL
     */
    public String getURL() {
        return URL;
    }

    /**
     * @param url the uRL to set
     */
    public void setURL(String url) {
        URL = url;
    }

    protected USPSBaseResponseObject makeUSPSCall(Class responseObjectClass) throws Exception {
        String xmlString = xmlMapping.toXML(this.requestObject);
        String queryString = "API=" + this.apiName + "&XML=" + xmlString;
        if (log.isDebugEnabled()) log.debug("Request string: " + queryString);
        try {
            String data = sendHTTPPost(this.URL, queryString, true);
            USPSBaseResponseObject responseObject = (USPSBaseResponseObject) this.xmlMapping.fromXML(data, responseObjectClass);
            return responseObject;
        } catch (Exception e1) {
            log.error("Some exception in sending HTTP Request to USPS, URL is " + this.URL, e1);
            throw e1;
        } finally {
        }
    }
}
