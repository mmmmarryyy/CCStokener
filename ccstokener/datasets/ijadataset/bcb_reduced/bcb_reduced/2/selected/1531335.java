package architetris;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

public class HttpCommunicator {

    public static Vector<String> getData(URL url) {
        Vector<String> answer = new Vector<String>();
        int responseCode = 0;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            responseCode = connection.getResponseCode();
        } catch (Exception ex) {
            return null;
        }
        if (responseCode != 200) {
            return null;
        }
        try {
            String line;
            InputStream input = connection.getInputStream();
            BufferedReader dataInput = new BufferedReader(new InputStreamReader(input));
            while ((line = dataInput.readLine()) != null) {
                answer.add(line);
            }
        } catch (Exception ex) {
            return null;
        }
        return answer;
    }

    public static boolean postData(URL url, String parameters) {
        HttpURLConnection hpcon = null;
        try {
            hpcon = (HttpURLConnection) url.openConnection();
            hpcon.setRequestMethod("POST");
            hpcon.setRequestProperty("Content-Length", "" + Integer.toString(parameters.getBytes().length));
            hpcon.setUseCaches(false);
            hpcon.setDoInput(true);
            hpcon.setDoOutput(true);
            DataOutputStream printout = new DataOutputStream(hpcon.getOutputStream());
            printout.writeBytes(parameters);
            printout.flush();
            printout.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(hpcon.getInputStream()));
            String input;
            boolean success = false;
            while ((input = in.readLine()) != null) {
                if (input.contains("OK")) success = true;
            }
            return success;
        } catch (Exception e) {
            try {
                if (hpcon != null) hpcon.disconnect();
            } catch (Exception e2) {
            }
            return false;
        }
    }
}
