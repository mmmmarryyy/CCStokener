package com.google.code.javastorage.s3;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.impl.soap.axis.SoapS3Service;
import org.jets3t.service.security.AWSCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.code.javastorage.Session;
import com.google.code.javastorage.callback.NullCallbackHandler;
import com.google.code.javastorage.callback.StringValueCallback;
import com.google.code.javastorage.util.URLOpener;
import com.google.code.javastorage.util.XMLUtil;

public class S3Session implements Session {

    private Logger log = LoggerFactory.getLogger(S3Session.class);

    private S3Service service;

    private S3File root;

    private URLOpener urlOpener = new URLOpener();

    private XMLUtil xmlUtil;

    private StringValueCallback accessKey = new StringValueCallback("aws_access_key_id");

    private StringValueCallback secretKey = new StringValueCallback("aws_secret_access_key");

    public S3Session(XMLUtil xmlUtil) {
        this(new NullCallbackHandler(), xmlUtil);
    }

    public S3Session(CallbackHandler callbackHandler, XMLUtil xmlUtil) {
        this.xmlUtil = xmlUtil;
        try {
            callbackHandler.handle(new Callback[] { accessKey, secretKey });
            AWSCredentials cred = new AWSCredentials(accessKey.getValue(), secretKey.getValue());
            service = new SoapS3Service(cred);
            this.root = new S3File(this, service);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up session. " + e.getMessage(), e);
        }
    }

    @Override
    public S3File getRoot() {
        return root;
    }

    public Object request(URL url) {
        try {
            return xmlUtil.unmarshall(urlOpener.openStream(url));
        } catch (FileNotFoundException e) {
            log.info("File not found: " + url);
        } catch (IOException e) {
            log.error("Failed to read from url: " + url + ". " + e.getMessage(), e);
        }
        return null;
    }

    public URLOpener getUrlOpener() {
        return urlOpener;
    }

    public void setUrlOpener(URLOpener urlOpener) {
        this.urlOpener = urlOpener;
    }
}
