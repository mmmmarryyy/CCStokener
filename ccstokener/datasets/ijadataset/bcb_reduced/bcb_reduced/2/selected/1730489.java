package il.co.inforu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import com.googlecode.sms4j.SmsClient;
import com.googlecode.sms4j.SmsException;

public class InforuSmsClient implements SmsClient {

    public InforuSmsClient(String username, String password) {
        this.user = new User(username, password);
    }

    public String send(String srcName, String srcNumber, String destNumber, String text) throws IOException, SmsException {
        if ((srcNumber != null) && (srcNumber.charAt(0) == '+')) {
            srcNumber = srcNumber.substring(1);
        }
        final Result result = sendImpl(new Message(user, new Content(text), new Recipients(destNumber), new Settings(srcNumber, srcName)));
        if ((result.status != Status.OK) || (result.numberOfRecipients != 1)) {
            throw new InforuSmsException(result);
        }
        return "";
    }

    private static Result sendImpl(Message inforuMessage) throws IOException {
        final URL url = new URL(INFORU_GATEWAY_URL);
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        final String encodedMessage = "InforuXML=" + inforuMessage.toXml().replace(' ', '+');
        final byte[] byteMessage = encodedMessage.getBytes("UTF-8");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", Integer.toString(byteMessage.length));
        conn.connect();
        final OutputStream os = conn.getOutputStream();
        try {
            os.write(byteMessage);
        } finally {
            os.close();
        }
        final BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        try {
            return Result.fromXml(br.readLine());
        } finally {
            br.close();
        }
    }

    private final User user;

    private static final String INFORU_GATEWAY_URL = "https://api.inforu.co.il/SendMessageXml.ashx";
}
