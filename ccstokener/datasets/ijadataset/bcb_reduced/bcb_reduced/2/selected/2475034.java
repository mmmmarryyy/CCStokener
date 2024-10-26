package com.baldwin.www.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * ����:һ��ͨ�õ��ļ���д��
 *@author seaman 
 *@see  http://blog.csdn.net/coolriver/archive/2004/09/13/102420.aspx Java������
 *@since 2005-05-28
 */
public class CoReader {

    CoReader() {
    }

    /**
	 * ����:���ı��ļ��ж�ȡ�������
	 * by seaman yang
	 * @param pathFile
	 * @return
	 */
    public static String getFromTxtFile(String pathFile) {
        String contents = "";
        try {
            BufferedReader in = new BufferedReader(new FileReader(pathFile));
            String sline;
            while ((sline = in.readLine()) != null) {
                contents += sline + "\r\n";
            }
            in.close();
        } catch (FileNotFoundException e) {
            System.err.println(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contents;
    }

    /**
	 * ����:����ļ�·������ļ���ȡ���ļ�����
	 * 
	 * @param pathName
	 * @param fileName
	 * @return
	 */
    public static String getFromTxtFile(String pathName, String fileName) {
        return getFromTxtFile(pathName + fileName);
    }

    /**
	 * ����:��һ��URL�ļ��ж�ȡ����,����Э�����:HTTP,FTP
	 * by anson lee
	 * @param strURL
	 * @return
	 */
    public static String callURL(String strURL) {
        try {
            URL url = new URL(strURL);
            BufferedReader receiver = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer msg = new StringBuffer();
            char[] data = new char[512];
            int n = 0;
            while ((n = receiver.read(data, 0, 512)) != -1) {
                msg.append(data, 0, n);
            }
            return msg.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
