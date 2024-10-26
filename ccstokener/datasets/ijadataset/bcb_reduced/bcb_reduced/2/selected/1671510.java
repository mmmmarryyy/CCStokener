package cn.edu.thss.iise.beehivez.client.ui.modelio.crawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpFile {

    /**
	 * http connection
	 */
    private HttpURLConnection conn;

    /**
	 * url
	 */
    private java.net.URL url;

    /**
	 * network inputstream
	 */
    private BufferedReader httpReader;

    /**
	 * construction
	 * 
	 * @param address
	 *            url
	 */
    public HttpFile(String address) {
        try {
            url = new URL(address);
        } catch (Exception e) {
            url = null;
            e.printStackTrace();
        }
    }

    /**
	 * ��ȡ��ҳ����ļ�������
	 * 
	 * @return �ļ�����
	 * @throws IOException
	 */
    public String getContent() throws IOException {
        String result = new String();
        if (url == null) return null;
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Internet Explorer");
        conn.setReadTimeout(50000);
        conn.connect();
        httpReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String str = httpReader.readLine();
        while (str != null) {
            result += str;
            str = httpReader.readLine();
        }
        return result;
    }

    /**
	 * ���ļ����ص�ָ���ļ���
	 * 
	 * @param target
	 *            �ļ���
	 */
    public void download(String target) {
        try {
            if (url == null) return;
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Internet Explorer");
            conn.setReadTimeout(10000);
            conn.connect();
            httpReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            java.io.BufferedWriter out = new BufferedWriter(new FileWriter(target, false));
            String str = httpReader.readLine();
            while (str != null) {
                out.write(str);
                str = httpReader.readLine();
            }
            out.close();
            System.out.println("file download successfully: " + url.getHost() + url.getPath());
            System.out.println("saved to: " + target);
        } catch (Exception e) {
            System.out.println("file download failed: " + url.getHost() + url.getPath());
            e.printStackTrace();
        }
    }
}
