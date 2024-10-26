package util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 * @author Dhanika
 */
public class MlpConnector {

    public String getLocationResponse(String phoneNo) {
        String mlpResp = null;
        try {
            URL url = new URL("http://127.0.0.1:8080/simu/thenahari?phoneNo=" + phoneNo);
            URLConnection conn = url.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append("\n" + line);
            }
            rd.close();
            mlpResp = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mlpResp;
    }

    public static void main(String[] args) {
        String phoneNo = "0772445142";
        String mlpResp = new MlpConnector().getLocationResponse(phoneNo);
        System.out.println("MLP response for " + phoneNo + ":\n" + mlpResp);
    }
}
