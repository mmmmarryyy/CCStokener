package com.cellact;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import com.googlecode.sms4j.SmsClient;
import com.googlecode.sms4j.SmsException;

public class CellactSmsClient implements SmsClient {

    public CellactSmsClient(String company, String username, String password) {
        this.company = company;
        this.username = username;
        this.password = password;
    }

    public String send(String srcName, String srcNumber, String destNumber, String text) throws IOException, SmsException {
        final Result result = sendImpl(text, Collections.singletonList(destNumber), ((srcName != null) ? srcName : srcNumber));
        if (result.code != 0) {
            throw new CellactSmsException(result);
        }
        return result.blmj;
    }

    private Result sendImpl(String text, List<String> destinations, String source) throws IOException {
        final StringBuilder builder = new StringBuilder();
        builder.append("<PALO>");
        builder.append("<HEAD>");
        addTag(builder, "FROM", company);
        builder.append("<APP USER=\"").append(username).append("\" PASSWORD=\"").append(password).append("\"/>");
        addTag(builder, "CMD", "sendtextmt");
        builder.append("</HEAD>");
        builder.append("<BODY>");
        addTag(builder, "CONTENT", text);
        builder.append("<DEST_LIST>");
        for (String destination : destinations) {
            addTag(builder, "TO", destination);
        }
        builder.append("</DEST_LIST>");
        builder.append("</BODY>");
        builder.append("<OPTIONAL>");
        addTag(builder, "CALLBACK", source);
        builder.append("</OPTIONAL>");
        builder.append("</PALO>");
        final String resultXml = post(builder.toString());
        return Result.fromXml(resultXml);
    }

    private static String post(String xmlString) throws IOException {
        final URL url = new URL(CELLACT_GATEWAY_URL);
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        final String encodedMessage = "XMLString=" + xmlString.replace(" ", "%20").replace("+", "%2B");
        final byte[] byteMessage = encodedMessage.getBytes("UTF-8");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Length", Integer.toString(byteMessage.length));
        conn.connect();
        final OutputStream os = conn.getOutputStream();
        try {
            os.write(byteMessage);
        } finally {
            os.close();
        }
        final StringBuilder builder = new StringBuilder();
        final BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
        } finally {
            br.close();
        }
        return builder.toString();
    }

    private static void addTag(StringBuilder builder, String name, String content) {
        builder.append('<').append(name).append('>');
        builder.append(content);
        builder.append("</").append(name).append('>');
    }

    private final String company;

    private final String username;

    private final String password;

    private static final String CELLACT_GATEWAY_URL = "https://cellactpro.net/GlobalSms/ExternalClient/GlobalAPI.asp";
}
