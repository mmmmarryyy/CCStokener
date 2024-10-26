package cn.com.once.deploytool.unit.transport.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import cn.com.once.deploytool.CommonUtil;
import cn.com.once.deploytool.DeployToolException;
import cn.com.once.deploytool.unit.transport.AbstractTransporter;
import cn.com.once.deploytool.unit.transport.TransporterType;
import cn.com.once.deploytool.unit.transport.UnitTransportException;

public class HttpTransporter extends AbstractTransporter {

    private static Logger logger = Logger.getLogger(HttpTransporter.class);

    public HttpTransporter() {
        logger.info("Create a new [HTTP] Transporter.");
        this.type = TransporterType.HTTP;
    }

    public String resovleAbsolutionUrl(String baseUrl, String id) {
        if (baseUrl.charAt(baseUrl.length() - 1) != '/') baseUrl = baseUrl + '/';
        logger.info(String.format("The Http path of unit is [%s]", baseUrl + id));
        return baseUrl + id;
    }

    public String tranportRemoteUnitToLocalTempFile(String urlStr) throws UnitTransportException {
        InputStream input = null;
        BufferedOutputStream bos = null;
        File tempUnit = null;
        try {
            URL url = null;
            int total = 0;
            try {
                url = new URL(urlStr);
                input = url.openStream();
                URLConnection urlConnection;
                urlConnection = url.openConnection();
                total = urlConnection.getContentLength();
            } catch (IOException e) {
                throw new UnitTransportException(String.format("Can't get remote file [%s].", urlStr), e);
            }
            String unitName = urlStr.substring(urlStr.lastIndexOf('/') + 1);
            tempUnit = null;
            try {
                if (StringUtils.isNotEmpty(unitName)) tempUnit = new File(CommonUtil.getTempDir(), unitName); else tempUnit = File.createTempFile(CommonUtil.getTempDir(), "tempUnit");
                File parent = tempUnit.getParentFile();
                FileUtils.forceMkdir(parent);
                if (!tempUnit.exists()) FileUtils.touch(tempUnit);
                bos = new BufferedOutputStream(new FileOutputStream(tempUnit));
            } catch (FileNotFoundException e) {
                throw new UnitTransportException(String.format("Can't find temp file [%s].", tempUnit.getAbsolutePath()), e);
            } catch (IOException e) {
                throw new UnitTransportException(String.format("Can't create temp file [%s].", tempUnit.getAbsolutePath()), e);
            } catch (DeployToolException e) {
                throw new UnitTransportException(String.format("Error when create temp file [%s].", tempUnit), e);
            }
            logger.info(String.format("Use [%s] for http unit [%s].", tempUnit.getAbsoluteFile(), urlStr));
            int size = -1;
            try {
                size = IOUtils.copy(input, bos);
                bos.flush();
            } catch (IOException e) {
                logger.info(String.format("Error when download [%s] to [%s].", urlStr, tempUnit));
            }
            if (size != total) throw new UnitTransportException(String.format("The file size is not right when download http unit [%s]", urlStr));
        } finally {
            if (input != null) IOUtils.closeQuietly(input);
            if (bos != null) IOUtils.closeQuietly(bos);
        }
        logger.info(String.format("Download unit to [%s].", tempUnit.getAbsolutePath()));
        return tempUnit.getAbsolutePath();
    }
}
