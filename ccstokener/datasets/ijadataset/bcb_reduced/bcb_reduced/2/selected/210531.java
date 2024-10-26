package com.bulksms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import com.googlecode.sms4j.SmsClient;
import com.googlecode.sms4j.SmsException;

public class BulksmsSmsClient implements SmsClient {

    private static final String BULKSMS_GATEWAY_URL = "http://bulksms.vsms.net:5567/eapi/submission/send_sms/2/2.0";

    private final String username;

    private final String password;

    private final RoutingGroup routingGroup;

    public static enum RoutingGroup {

        ECONOMY(1), STANDARD(2), PREMIUM(3);

        private final int id;

        private RoutingGroup(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    public BulksmsSmsClient(String username, String password, RoutingGroup routingGroup) {
        this.username = username;
        this.password = password;
        this.routingGroup = routingGroup;
    }

    public String send(String srcName, String srcNumber, String destNumber, String text) throws IOException, SmsException {
        final String result = sendImpl(((srcName != null) ? srcName : removeLeadingPlus(srcNumber)), removeLeadingPlus(destNumber), text);
        final String[] resultTokens = result.split("\\|");
        if (resultTokens.length < 2) {
            throw new SmsException("Invalid result: " + result);
        } else {
            final int statusCode = Integer.parseInt(resultTokens[0]);
            final String statusDescription = resultTokens[1];
            if (statusCode == 0) {
                if (resultTokens.length != 3) {
                    throw new SmsException("Invalid result: " + result);
                }
                return resultTokens[2];
            } else {
                throw new BulksmsSmsException(statusCode, statusDescription);
            }
        }
    }

    private String sendImpl(String from, String destNumber, String text) throws IOException {
        final URL url = new URL(BULKSMS_GATEWAY_URL);
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        final StringBuilder data = new StringBuilder();
        data.append("username=").append(URLEncoder.encode(username, "ISO-8859-1"));
        data.append("&password=").append(URLEncoder.encode(password, "ISO-8859-1"));
        data.append("&message=").append(URLEncoder.encode(text, "ISO-8859-1"));
        data.append("&msisdn=").append(destNumber);
        data.append("&sender=").append(URLEncoder.encode(from, "ISO-8859-1"));
        data.append("&routing_group=").append(routingGroup.getId());
        data.append("&repliable=0");
        final String dataStr = data.toString();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", Integer.toString(dataStr.length()));
        conn.setDoOutput(true);
        conn.connect();
        final OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
        try {
            writer.write(dataStr);
        } finally {
            writer.close();
        }
        final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    private static String removeLeadingPlus(String phone) {
        return ((phone.charAt(0) != '+') ? phone : phone.substring(1));
    }
}
