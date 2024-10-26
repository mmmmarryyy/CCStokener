package com.windsor.node.service.helper.web;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import com.windsor.node.service.helper.RemoteFileResourceHelper;
import com.windsor.node.service.helper.settings.SettingServiceProvider;

public class SimpleRemoteFileResourceHelper implements RemoteFileResourceHelper, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(SimpleRemoteFileResourceHelper.class);

    private SettingServiceProvider settingProvider;

    public void afterPropertiesSet() {
        if (settingProvider == null) {
            throw new RuntimeException("SettingProvider not set");
        }
    }

    /**
     * getBytesFromURL
     */
    public byte[] getBytesFromURL(String address) {
        try {
            URL url = new URL(address);
            URLConnection conn = url.openConnection();
            InputStream fis = conn.getInputStream();
            ByteArrayOutputStream byteArrOut = new ByteArrayOutputStream();
            int ln;
            byte[] buf = new byte[1024 * 12];
            while ((ln = fis.read(buf)) != -1) {
                byteArrOut.write(buf, 0, ln);
            }
            return byteArrOut.toByteArray();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException("Error while getting content from Url: " + address, ex);
        }
    }

    public void setSettingProvider(SettingServiceProvider settingProvider) {
        this.settingProvider = settingProvider;
    }
}
