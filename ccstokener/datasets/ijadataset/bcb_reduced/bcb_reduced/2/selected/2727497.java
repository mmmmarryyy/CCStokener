package com.leclercb.commons.api.utils;

import java.io.File;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;

public final class FileUtils {

    private FileUtils() {
    }

    public static void copyURLToFile(URL url, File file) throws Exception {
        copyURLToFile(url, file, null, 0, null, null);
    }

    public static void copyURLToFile(URL url, File file, final String proxyHost, final int proxyPort, final String proxyUsername, final String proxyPassword) throws Exception {
        HttpURLConnection connection = null;
        if (proxyHost == null) {
            connection = (HttpURLConnection) url.openConnection();
        } else {
            if (proxyUsername != null && proxyPassword != null) {
                Authenticator.setDefault(new Authenticator() {

                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxyUsername, proxyPassword.toCharArray());
                    }
                });
            }
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            connection = (HttpURLConnection) url.openConnection(proxy);
        }
        InputStream inputStream = connection.getInputStream();
        if (connection.getResponseCode() != 200 || inputStream == null) throw new Exception("HTTP error: " + connection.getResponseCode() + " - " + connection.getResponseMessage());
        org.apache.commons.io.FileUtils.copyInputStreamToFile(inputStream, file);
    }

    public static String getExtention(String fileName) {
        CheckUtils.isNotNull(fileName);
        int lastIndexOfDot = fileName.lastIndexOf('.');
        if (lastIndexOfDot < 0) return "";
        return fileName.substring(lastIndexOfDot + 1, fileName.length());
    }

    public static boolean hasExtention(String fileName, String... extentions) {
        CheckUtils.isNotNull(fileName);
        CheckUtils.isNotNull(extentions);
        String extention = getExtention(fileName);
        for (int i = 0; i < extentions.length; i++) if (extention.equals(extentions[i])) return true;
        return false;
    }

    public static String removeExtention(String fileName) {
        CheckUtils.isNotNull(fileName);
        int lastIndexOfDot = fileName.lastIndexOf('.');
        if (lastIndexOfDot < 0) return fileName;
        return fileName.substring(0, lastIndexOfDot);
    }
}
