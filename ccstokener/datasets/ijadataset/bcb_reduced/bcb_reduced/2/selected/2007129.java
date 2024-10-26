package com.baldwin.www.datahandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * <p>
 * <b>�ļ���ƣ� </b> <br>
 * URLHandler.java <br>
 * </p>
 * <p>
 * <b>ʵ�ֹ��ܣ� </b> <br>
 * ͨ��URL�����˽������ <br>
 * </p>
 * <p>
 * <b>�޸���ʷ: </b> <br>
 * 2005-02-24 ������ɣ������汾1.0.0 <br>
 * 2005-03-02 �޸��ش�bug��ȡ����ݵ�ѭ�������ж�Ӧ����==-1������==1, �����汾 1.0.1
 * 2005-03-09 ����setContentType����,�������������FormType, �����汾 1.1.0
 * </p>
 * <p>
 * <b>��ǰ�汾��1.1.0 </b> <br>
 * </p>
 * <p>
 * <b>��ˣ� </b> <br>
 * </p>
 */
public final class URLHandler {

    private boolean debug = false;

    private final int MAX_WAIT_TIME = 8;

    private final String TIMEOUT = "TIMEOUT";

    private final String EXCEPTION = "EXCEPTION";

    private final String INTERRUPTED = "INTERRUPTED";

    private String contentType = null;

    /**
	 * @param contentType
	 *            ���ñ?����
	 */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
	 * Ĭ�Ϲ��캯��
	 */
    public URLHandler() {
    }

    /**
	 * �����Ƿ����
	 * 
	 * @param debug
	 *            bool��,���Ϊtrue������ӡ������Ϣ����������
	 */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
	 * ͨ��URLʵ����ݽ���
	 * 
	 * @param url
	 *            �����URL
	 * @param d
	 *            URL��Ҫ�����
	 * @return URL���صĽ��
	 */
    public String doDataExchange(String url, String d) {
        if (debug) System.out.println(url);
        ExchangeData ed = new ExchangeData(d, url);
        ed.start();
        try {
            ed.join(MAX_WAIT_TIME * 1000);
        } catch (InterruptedException e) {
            return INTERRUPTED;
        }
        return ed.getDataout();
    }

    class ExchangeData extends Thread {

        private String dataIn = "";

        private String dataOut = TIMEOUT;

        private String url = null;

        public ExchangeData(String d, String target) {
            dataIn = d;
            url = target;
        }

        public void run() {
            try {
                URL httpUrl = new URL(url);
                HttpURLConnection urlConn = (HttpURLConnection) httpUrl.openConnection();
                urlConn.setDoOutput(true);
                urlConn.setRequestMethod("POST");
                if (contentType != null) urlConn.setRequestProperty("Content-Type", contentType);
                OutputStream os = urlConn.getOutputStream();
                os.write(dataIn.getBytes());
                BufferedReader receiver = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                StringBuffer lastReceived = new StringBuffer();
                char[] rt = new char[512];
                int n = 0;
                while ((n = receiver.read(rt, 0, 512)) != -1) {
                    lastReceived.append(rt, 0, n);
                }
                dataOut = lastReceived.toString().trim();
                if (debug) System.out.println(dataOut);
            } catch (Exception e) {
                if (debug) e.printStackTrace();
                dataOut = EXCEPTION;
            }
        }

        public String getDataout() {
            return dataOut;
        }
    }
}
