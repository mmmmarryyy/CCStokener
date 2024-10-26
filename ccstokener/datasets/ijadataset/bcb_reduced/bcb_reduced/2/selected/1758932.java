package cn.com.once.deploytool.unit.transport.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import cn.com.once.deploytool.CommonUtil;
import cn.com.once.deploytool.DeployToolException;
import cn.com.once.deploytool.unit.transport.AbstractTransporter;
import cn.com.once.deploytool.unit.transport.TransporterType;
import cn.com.once.deploytool.unit.transport.UnitTransportException;

/**
 * TODO ���ܺ�����һ��ftp�ͻ��ˣ��п��˼�ǿһ�¡�
 * 
 * @author ����
 * 
 */
public class FtpTransporter extends AbstractTransporter {

    private static Logger logger = Logger.getLogger(FtpTransporter.class);

    public FtpTransporter() {
        logger.info("Create a new [FTP] Transporter.");
        this.type = TransporterType.FTP;
    }

    public String resovleAbsolutionUrl(String baseUrl, String id) {
        if (baseUrl.charAt(baseUrl.length() - 1) != '/') baseUrl = baseUrl + '/';
        logger.info(String.format("The [Ftp] path of unit is [%s]", baseUrl + id));
        return baseUrl + id;
    }

    /**
	 * @param url
	 *            ������Ҫ����ftp�ļ�������·���������û������룬�˿ڵ���Ϣ���磺
	 *            <code>ftp://tcse:tcse@133.133.133.40/incoming/FTP����.txt</code>
	 * @throws UnitTransportException
	 * @throws UnitTransportException
	 */
    public String tranportRemoteUnitToLocalTempFile(String urlStr) throws UnitTransportException {
        URL url = null;
        File tempUnit = null;
        BufferedOutputStream bos = null;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e1) {
            logger.error(String.format("The url [%s] is illegal.", urlStr), e1);
            throw new UnitTransportException(String.format("The url [%s] is illegal.", urlStr), e1);
        }
        URLConnection con = null;
        BufferedInputStream in = null;
        try {
            con = url.openConnection();
            in = new BufferedInputStream(con.getInputStream());
        } catch (IOException e) {
            logger.error(String.format("Can't open url [%s].", urlStr));
            throw new UnitTransportException(String.format("Can't open url [%s].", urlStr), e);
        } catch (Exception e) {
            logger.error(String.format("Unknown error. Maybe miss the username and password in url [%s].", urlStr), e);
            throw new UnitTransportException(String.format("Unknown error. Maybe miss the username and password in url [%s].", urlStr), e);
        }
        String unitName = urlStr.substring(urlStr.lastIndexOf('/') + 1);
        try {
            if (!StringUtils.isEmpty(unitName)) tempUnit = new File(CommonUtil.getTempDir(), unitName); else tempUnit = new File(CommonUtil.createTempFile());
        } catch (DeployToolException e) {
            logger.error(String.format("Can't get temp file [%s].", tempUnit));
            throw new UnitTransportException(String.format("Can't get temp file [%s].", tempUnit), e);
        }
        try {
            bos = new BufferedOutputStream(new FileOutputStream(tempUnit));
            logger.info(String.format("Use [%s] for ftp unit [%s].", tempUnit, urlStr));
        } catch (FileNotFoundException e) {
            logger.error(String.format("File [%s] don't exist.", tempUnit));
            throw new UnitTransportException(String.format("File [%s] don't exist.", tempUnit), e);
        }
        try {
            IOUtils.copy(in, bos);
            bos.flush();
        } catch (IOException e) {
            logger.error(String.format("Error when download [%s] to [%s].", urlStr, tempUnit), e);
            throw new UnitTransportException(String.format("Error when download [%s] to [%s].", urlStr, tempUnit), e);
        } finally {
            IOUtils.closeQuietly(bos);
            IOUtils.closeQuietly(in);
        }
        logger.info(String.format("Download unit to [%s].", tempUnit.getAbsolutePath()));
        return tempUnit.getAbsolutePath();
    }
}
