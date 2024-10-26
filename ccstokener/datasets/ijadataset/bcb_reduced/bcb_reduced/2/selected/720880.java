package com.isfasiel.util.request;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.springframework.stereotype.Component;

/**
 * 
 * @Class Name : RequestUtil.java
 * @Description : ���� �� ����Ʈ ��û�� Ŭ����
 * @Modification Information
 * @  ������      ������              ��������
 * @ ---------   ---------   -------------------------------
 * @ 2010. 12. 30.	�躴��
 * @author �躴��
 */
@Component
public class RequestUtil {

    private String CHAR_SET = "UTF-8";

    public String getCHAR_SET() {
        return CHAR_SET;
    }

    public void setCHAR_SET(String cHARSET) {
        CHAR_SET = cHARSET;
    }

    /**
	 * send get request 
	 * @param targetUrl
	 * @param params
	 * @return
	 */
    public String get(String targetUrl, String params) {
        return excute(targetUrl, params, "GET");
    }

    /**
	 * send post request
	 * @param targetUrl
	 * @param params
	 * @return
	 */
    public String post(String targetUrl, String params) {
        return excute(targetUrl, params, "POST");
    }

    /**
	 * execute request
	 * @param targetUrl
	 * @param params
	 * @param type
	 * @return string of html code
	 */
    public String excute(String targetUrl, String params, String type) {
        URL url;
        HttpURLConnection connection = null;
        try {
            url = new URL(targetUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(type);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(params.getBytes().length));
            connection.setRequestProperty("Content-Language", CHAR_SET);
            connection.setRequestProperty("Connection", "close");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            if (params != null) {
                if (params.length() > 0) {
                    DataOutputStream wr;
                    wr = new DataOutputStream(connection.getOutputStream());
                    wr.writeBytes(params);
                    wr.flush();
                    wr.close();
                }
            }
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, CHAR_SET));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append("\r\n");
            }
            rd.close();
            return response.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
