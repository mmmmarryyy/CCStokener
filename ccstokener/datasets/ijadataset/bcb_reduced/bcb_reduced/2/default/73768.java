import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class UploadTest extends Test {

    String page;

    URL url = null;

    URLConnection yc = null;

    BufferedReader in = null;

    UploadTest(String description, String address, int port, int step, String page) {
        super("Upload result:", description, address, port, step);
        this.page = page;
    }

    @Override
    boolean doActivate() {
        setTimeout(15000);
        String report = Test.URLEncodeAll(step - 1);
        String urlstring = getAddress() + "/" + page + "?" + report;
        try {
            url = new URL(urlstring);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
        try {
            yc = url.openConnection();
            in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
        } catch (IOException e) {
            System.err.println("UploadTest.doActivate:" + e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    boolean doReceive() {
        String inputLine;
        if (in == null) {
            return true;
        }
        try {
            while ((inputLine = in.readLine()) != null) if (inputLine.contains("</body>")) {
                this.txtResult.setText("Ok");
                resultColorBg = Color.GREEN;
                return true;
            }
        } catch (IOException e) {
            System.err.println("UploadTest doReceive:" + e.getMessage());
        }
        return false;
    }

    @Override
    void doClose() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
        }
    }
}
