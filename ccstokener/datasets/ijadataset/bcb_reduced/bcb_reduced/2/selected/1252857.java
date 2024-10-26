package de.shandschuh.jaolt.core.pictureuploadservice;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import de.shandschuh.jaolt.core.ConfigurationValue;
import de.shandschuh.jaolt.core.Language;
import de.shandschuh.jaolt.core.auction.Picture;
import de.shandschuh.jaolt.core.exception.CommonException;
import de.shandschuh.jaolt.tools.url.URLHelper;

public class FTPPictureUploadService extends BasicPictureUploadService {

    public static final String FTP_USERNAME = "FTP_USERNAME";

    public static final String FTP_PASSWORD = "FTP_PASSWORD";

    public static final String FTP_HOSTNAME = "FTP_HOSTNAME";

    public static final String FTP_PORT = "FTP_PORT";

    public static final String FTP_PICTUREFOLDER = "FTP_PICTUREFOLDER";

    public static final String FTP_HTTPPREFIX = "FTP_HTTPPREFIX";

    @Override
    public ConfigurationValue[] getConfigurationValues() {
        return new ConfigurationValue[] { new ConfigurationValue(ConfigurationValue.TYPE_STRING, FTP_USERNAME), new ConfigurationValue(ConfigurationValue.TYPE_PASSWORD, FTP_PASSWORD), new ConfigurationValue(ConfigurationValue.TYPE_HOSTNAME, FTP_HOSTNAME), new ConfigurationValue(ConfigurationValue.TYPE_INT, FTP_PORT), new ConfigurationValue(ConfigurationValue.TYPE_STRING, FTP_PICTUREFOLDER), new ConfigurationValue(ConfigurationValue.TYPE_URL, FTP_HTTPPREFIX) };
    }

    @Override
    public String getDescription() {
        return Language.translateStatic("PICTURESERVICE_FTP_DESCRIPTION");
    }

    @Override
    public String getName() {
        return "FTP";
    }

    @Override
    public URL uploadPicture(Picture picture, ConfigurationValue[] configurationValues) throws Exception {
        String username = ConfigurationValue.getConfigurationValue(FTP_USERNAME, configurationValues);
        if (username == null || username.length() == 0) {
            throw new CommonException(Language.translateStatic("ERROR_NOFTPUSERNAMESET"));
        }
        String password = ConfigurationValue.getConfigurationValue(FTP_PASSWORD, configurationValues);
        if (password == null || password.length() == 0) {
            throw new CommonException(Language.translateStatic("ERROR_NOFTPPASSOWRDSET"));
        }
        String ftpFolder = ConfigurationValue.getConfigurationValue(FTP_PICTUREFOLDER, configurationValues);
        if (ftpFolder != null) {
            if (!ftpFolder.startsWith("/")) {
                ftpFolder = "/" + ftpFolder;
            }
            if (!ftpFolder.endsWith("/")) {
                ftpFolder = ftpFolder + "/";
            }
        } else {
            ftpFolder = "/";
        }
        String host = ConfigurationValue.getConfigurationValue(FTP_HOSTNAME, configurationValues);
        if (host == null || host.length() == 0) {
            throw new CommonException(Language.translateStatic("ERROR_NOFTPHOSTSET"));
        }
        Integer port = ConfigurationValue.getConfigurationValue(FTP_PORT, configurationValues);
        if (port < 1) {
            port = 21;
        }
        String filename = picture.getFileName();
        URL url = new URL("ftp://" + URLEncoder.encode(username, "utf-8") + ":" + URLEncoder.encode(password, "utf-8") + "@" + host + ":" + port + ftpFolder + URLEncoder.encode(filename, "utf-8"));
        URLConnection connection = url.openConnection();
        OutputStream outputStream = connection.getOutputStream();
        FileInputStream fileInputStream = new FileInputStream(picture.getFile());
        int read = 0;
        byte[] buffer = new byte[1024];
        while (read != -1) {
            outputStream.write(buffer, 0, read);
            read = fileInputStream.read(buffer);
        }
        fileInputStream.close();
        outputStream.close();
        URL pictureURL = ConfigurationValue.getConfigurationValue(FTP_HTTPPREFIX, configurationValues);
        if (pictureURL == null) {
            return new URL(host + ftpFolder + filename);
        } else {
            return new URL(pictureURL + "/" + filename);
        }
    }

    @Override
    public int getMaximumFileSize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public URL getHelpURL() {
        return URLHelper.createURL("http://code.google.com/p/jaolt/wiki/FTPPictureHosting");
    }

    @Override
    public boolean isUsable() {
        return true;
    }

    public static void main(String[] args) {
    }
}
