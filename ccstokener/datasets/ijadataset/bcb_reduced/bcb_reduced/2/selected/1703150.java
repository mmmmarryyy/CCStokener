package de.icehorsetools.iceoffice.service.url;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Scanner;
import org.apache.commons.lang.StringUtils;
import org.ugat.wiser.language.Lang;
import de.icehorsetools.constants.ConfigCo;
import de.icehorsetools.exeption.IcehorsetoolsRuntimeException;
import de.icehorsetools.exeption.NoNumberFormatException;
import de.icehorsetools.iceoffice.service.DefaultSvFactory;
import de.icehorsetools.iceoffice.service.configuration.IConfigurationSv;
import de.ug2t.kernel.KeEnvironment;

public class UrlSv implements IUrlSv {

    public Scanner getUrlScanner(String strUrl) {
        Scanner scanner;
        URLConnection connection = getConnection(strUrl);
        String charSet = StringUtils.substringAfterLast(connection.getContentType(), "charset=");
        try {
            scanner = new Scanner(connection.getInputStream(), (charSet == StringUtils.EMPTY ? "ISO-8859-1" : charSet));
        } catch (IOException e) {
            throw new IcehorsetoolsRuntimeException(MessageFormat.format(Lang.get(this.getClass(), "IOException"), new Object[] { connection.getURL().toString() }));
        }
        return scanner;
    }

    public BufferedReader getUrlBufferdReader(String strUrl) {
        URLConnection connection = getConnection(strUrl);
        BufferedReader inputReader;
        try {
            inputReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } catch (IOException e) {
            throw new IcehorsetoolsRuntimeException(MessageFormat.format(Lang.get(this.getClass(), "IOException"), new Object[] { connection.getURL().toString() }));
        }
        return inputReader;
    }

    private URLConnection getConnection(String strUrl) {
        URL url = null;
        IConfigurationSv cfg = DefaultSvFactory.getInstance().getConfigurationSv();
        URLConnection connection;
        String configProxyIp = cfg.getValueString(ConfigCo.COMMON_PROXY_IP);
        int configProxyPort = 0;
        try {
            configProxyPort = cfg.getValueInt(ConfigCo.COMMON_PROXY_PORT);
        } catch (NoNumberFormatException e) {
            e.printStackTrace();
        }
        try {
            if (!strUrl.startsWith("http")) {
                strUrl = "http://" + strUrl;
            }
            url = new URL(strUrl);
            if (StringUtils.isBlank(configProxyIp)) {
                connection = url.openConnection();
            } else {
                SocketAddress address = new InetSocketAddress(configProxyIp, configProxyPort);
                Proxy proxy = new Proxy(Proxy.Type.HTTP, address);
                connection = url.openConnection(proxy);
            }
        } catch (MalformedURLException e) {
            throw new IcehorsetoolsRuntimeException(MessageFormat.format(Lang.get(this.getClass(), "MalformedURLException"), new Object[] { url.toString() }));
        } catch (IOException e) {
            throw new IcehorsetoolsRuntimeException(MessageFormat.format(Lang.get(this.getClass(), "IOException"), new Object[] { url.toString() }));
        }
        return connection;
    }

    @Override
    public File getFileFromAuthorizedStream(URL url, String filename, String user, String passwd) throws IOException {
        IConfigurationSv cfg = DefaultSvFactory.getInstance().getConfigurationSv();
        String configProxyIp = cfg.getValueString(ConfigCo.COMMON_PROXY_IP);
        int configProxyPort = 0;
        try {
            configProxyPort = cfg.getValueInt(ConfigCo.COMMON_PROXY_PORT);
        } catch (NoNumberFormatException e) {
            e.printStackTrace();
        }
        String s = user + ":" + passwd;
        String base64 = "Basic " + new sun.misc.BASE64Encoder().encode(s.getBytes());
        URLConnection conn;
        if (StringUtils.isBlank(configProxyIp)) {
            conn = url.openConnection();
        } else {
            SocketAddress address = new InetSocketAddress(configProxyIp, configProxyPort);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, address);
            conn = url.openConnection(proxy);
        }
        conn.setRequestProperty("Authorization", base64);
        conn.connect();
        InputStream fis = conn.getInputStream();
        File down = new File(KeEnvironment.getSessionTmpDir() + "/" + filename);
        FileOutputStream fos = new FileOutputStream(down);
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
        return down;
    }
}
