package com.google.code.javastorage.wuala;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.code.javastorage.Session;
import com.google.code.javastorage.callback.NullCallbackHandler;
import com.google.code.javastorage.callback.SecretFilesKeyCallback;
import com.google.code.javastorage.util.URLOpener;
import com.google.code.javastorage.util.XMLUtil;
import com.google.code.javastorage.wuala.file.WualaFile;
import com.google.code.javastorage.wuala.file.rest.Result;

/**
 * 
 * @author thomas.scheuchzer@gmail.com
 * 
 */
public class WualaSession implements Session {

    private Logger log = LoggerFactory.getLogger(WualaSession.class);

    private SecretFilesKeyCallback secretKeyCallback = new SecretFilesKeyCallback("secretKey");

    private WualaFile root;

    private URLOpener urlOpener = new URLOpener();

    private XMLUtil xmlUtil;

    public WualaSession(XMLUtil xmlUtil) {
        this(new NullCallbackHandler(), xmlUtil);
    }

    public WualaSession(CallbackHandler callbackHandler, XMLUtil xmlUtil) {
        this.xmlUtil = xmlUtil;
        this.root = new WualaFile(this);
        try {
            callbackHandler.handle(new Callback[] { secretKeyCallback });
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up session. " + e.getMessage(), e);
        }
    }

    @Override
    public WualaFile getRoot() {
        return root;
    }

    public Result request(URL url) {
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

    public String getSecretFilesKey() {
        return secretKeyCallback.getValue();
    }

    public boolean isSecretFileKey() {
        return StringUtils.isNotBlank(secretKeyCallback.getValue());
    }
}
