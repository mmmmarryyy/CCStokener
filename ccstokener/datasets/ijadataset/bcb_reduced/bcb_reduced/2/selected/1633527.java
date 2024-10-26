package com.google.code.javastorage.dropio;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.code.javastorage.Session;
import com.google.code.javastorage.callback.ConnectionKeyCallback;
import com.google.code.javastorage.callback.SecretFilesKeyCallback;
import com.google.code.javastorage.dropio.rest.Assets;
import com.google.code.javastorage.dropio.rest.Response;
import com.google.code.javastorage.util.URLOpener;
import com.google.code.javastorage.util.XMLUtil;

/**
 * 
 * @author thomas.scheuchzer@gmail.com
 * 
 */
public class DropIoSession implements Session {

    private Logger log = LoggerFactory.getLogger(DropIoSession.class);

    private SecretFilesKeyCallback guestPasswordCallback = new SecretFilesKeyCallback("guest-password");

    private ConnectionKeyCallback apiKeyCallback;

    private DropIoFile root;

    private URLOpener urlOpener = new URLOpener();

    private XMLUtil xmlUtil;

    public DropIoSession(CallbackHandler callbackHandler, ConnectionKeyCallback apiKeyCallback) {
        this.apiKeyCallback = apiKeyCallback;
        this.xmlUtil = new XMLUtil(Response.class, Assets.class);
        this.root = new DropIoFile(this);
        try {
            callbackHandler.handle(new Callback[] { guestPasswordCallback });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DropIoFile getRoot() {
        return root;
    }

    public Assets request(URL url) {
        try {
            log.info("Executing " + url);
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

    public String getApiKey() {
        return apiKeyCallback.getValue();
    }

    public String getGuestPassword() {
        String pw = guestPasswordCallback.getValue();
        return StringUtils.isBlank(pw) ? "" : pw;
    }
}
