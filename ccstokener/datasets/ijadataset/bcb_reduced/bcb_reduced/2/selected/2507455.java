package de.icehorsetools.dataImport.zrde;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.commons.lang.StringUtils;
import org.ugat.dataAccess.UnitOfWork;
import de.icehorsetools.constants.ConfigCo;
import de.icehorsetools.exeption.IcehorsetoolsException;
import de.icehorsetools.exeption.IcehorsetoolsRuntimeException;
import de.icehorsetools.exeption.NoNumberFormatException;
import de.icehorsetools.iceoffice.service.DefaultSvFactory;
import de.icehorsetools.iceoffice.service.configuration.IConfigurationSv;
import de.icehorsetools.interfaces.IIcehorestoolsDataAccess;
import de.ug2t.kernel.KeEnvironment;

/**
 * @author tkr
 * @version $Id: ProcessZrDE.java 382 2010-05-31 23:13:13Z kruegertom $
 */
public class ProcessZrDE {

    private String configProxyIp;

    private int configProxyPort;

    private URL url;

    private String user;

    private String pw;

    private UnitOfWork uow;

    private IIcehorestoolsDataAccess da;

    public ProcessZrDE() {
        this.uow = UnitOfWork.Factory.createInstance();
        UnitOfWork.Factory.setCurrentUnitOfWork(uow);
        this.da = IIcehorestoolsDataAccess.Factory.getInstance(uow);
        IConfigurationSv cfg = DefaultSvFactory.getInstance().getConfigurationSv();
        try {
            this.configProxyIp = cfg.getValueString(ConfigCo.COMMON_PROXY_IP);
            this.configProxyPort = cfg.getValueInt(ConfigCo.COMMON_PROXY_PORT);
            this.url = new URL(cfg.getValueString(ConfigCo.ZR_DOWNLOAD_URL));
            this.user = cfg.getValueString(ConfigCo.ZR_DOWNLOAD_USER);
            this.pw = cfg.getValueString(ConfigCo.ZR_DOWNLOAD_PW);
        } catch (NoNumberFormatException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void startDownload() {
        try {
            InputStream stream = openAuthorizedStream(this.url, this.user, this.pw);
            File tmpZipFile = File.createTempFile("zr_download", ".zip");
            FileOutputStream fos = new FileOutputStream(tmpZipFile);
            copy(stream, fos);
            ZipFile zipFile = new ZipFile(tmpZipFile);
            getEntry(zipFile, zipFile.getEntry("zrdb.rsd"));
        } catch (IOException e) {
            throw new IcehorsetoolsRuntimeException(e);
        }
    }

    /**
     * @param url
     * @param user
     * @param passwd
     * @return
     * @throws IOException
     */
    private InputStream openAuthorizedStream(URL url, String user, String passwd) throws IOException {
        String s = user + ":" + passwd;
        String base64 = "Basic " + new sun.misc.BASE64Encoder().encode(s.getBytes());
        URLConnection conn;
        if (StringUtils.isBlank(this.configProxyIp)) {
            conn = url.openConnection();
        } else {
            SocketAddress address = new InetSocketAddress(this.configProxyIp, this.configProxyPort);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, address);
            conn = url.openConnection(proxy);
        }
        conn.setRequestProperty("Authorization", base64);
        conn.connect();
        return conn.getInputStream();
    }

    /**
     * @param fis
     * @param fos
     * @throws IOException
     */
    private void copy(InputStream fis, FileOutputStream fos) throws IOException {
        try {
            byte[] buffer = new byte[1024];
            int numRead;
            while ((numRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, numRead);
            }
        } finally {
            if (fis != null) {
                fis.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    private static void getEntry(ZipFile zipFile, ZipEntry target) throws ZipException, IOException {
        final int EOF = -1;
        File file = new File(KeEnvironment.buildPath("data/db/" + target.getName()));
        BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(target));
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        for (int c; (c = bis.read()) != EOF; ) {
            bos.write((byte) c);
        }
        bos.close();
    }
}
