package com.nhncorp.cubridqa.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;

/**
 * 
 * The utility used to set RSS data .
 * @ClassName: RssClient
 * @deprecated
 * @date 2009-9-1
 * @version V1.0 Copyright (C) www.nhn.com
 */
public class RssClient {

    /**
	 * the utility to transfer string content to url .
	 * 
	 * @deprecated
	 * @param contents
	 * @param urlString
	 * @param urlString2
	 * @param serverIp
	 * @param port
	 */
    public static void contentTrans(String contents, String urlString, String urlString2, String serverIp, int port) {
        try {
            URL url = new URL(urlString);
            url.openStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Socket server = new Socket(InetAddress.getByName(serverIp), port);
            OutputStream outputStream = server.getOutputStream();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            bufferedWriter.write(contents);
            bufferedWriter.flush();
            bufferedWriter.close();
            server.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            URL url2 = new URL(urlString2);
            url2.openStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * 
	 * @param filePath
	 * @param urlString
	 * @param urlString2
	 * @param serverIp
	 * @param port
	 */
    public static void fileTrans(String filePath, String urlString, String urlString2, String serverIp, int port) {
        try {
            URL url = new URL(urlString);
            url.openStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
        File file = new File(filePath);
        try {
            FileInputStream fis = new FileInputStream(file);
            Socket server = new Socket(InetAddress.getByName(serverIp), port);
            OutputStream outputStream = server.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(outputStream));
            byte[] buffer = new byte[2048];
            int num = fis.read(buffer);
            while (num != -1) {
                dataOutputStream.write(buffer, 0, num);
                dataOutputStream.flush();
                num = fis.read(buffer);
            }
            fis.close();
            dataOutputStream.close();
            server.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            URL url2 = new URL(urlString2);
            url2.openStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
