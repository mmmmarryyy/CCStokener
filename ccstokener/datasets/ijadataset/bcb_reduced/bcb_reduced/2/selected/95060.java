package DN2;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DNutil {

    public DNutil() {
    }

    /** @param BufferedInputStream bis
	 *  @return String
	 *  @see BufferedInputStream �� �̿� �ܼ��� ������ ���� �� �ְ� ���ֱ� ����
	 *  @see simplicity one line read to used BufferedInputStream
	 */
    public static String readline(BufferedInputStream bis) {
        String line = "";
        char c;
        try {
            while ((c = (char) bis.read()) != '\r') {
                line += c;
            }
            bis.skip(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }

    /**
	 * @see �ʸ� �޾� ��:��:�� ������ ��Ʈ������ �����ϴ� �Լ�
	 * @param long sec
	 * @return String
	 */
    public static String SecToString(long sec) {
        int min = (int) (sec / 60);
        int hour = (min / 60);
        min = (min % 60);
        sec = (sec % 60);
        return hour + ":" + min + ":" + sec;
    }

    /**
	 * @see redirect �Ǵ��� Ȯ�� �ϴ� �Լ�.
	 * @param URL url
	 * @return boolean
	 */
    public static boolean RedirectCheck(URL url) {
        boolean retVal = false;
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setInstanceFollowRedirects(false);
            if (con.getResponseCode() == 302) retVal = true; else retVal = false;
            con.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return retVal;
    }

    /**
	 * @see redirect �ּҸ� �������� �Լ�.
	 * @see get redirect address function
	 * @param URL url
	 * @return String 
	 */
    public static String GetRedirectLocation(URL url) {
        String retVal = "";
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setInstanceFollowRedirects(false);
            retVal = con.getHeaderField("Location");
            con.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return retVal;
    }

    /**
	 * @see code�� Ȯ�� �������ڸ� �������ִ� �Լ�.
	 * @see 0 : NONE, 1 : ENABLE, 4 : ERROR
	 * @param int code
	 * @return String 
	 */
    public static String retString(int code) {
        String retVal = "";
        switch(code) {
            case 0:
                retVal = "���� ����.";
                break;
            case 1:
                retVal = "���� �ٿ�ε� ������.";
                break;
            case 4:
                retVal = "���� �߻� : �ٿ�ε� �Ұ�";
                break;
            default:
                retVal = "�˼� ��� ����!";
        }
        return retVal;
    }
}
