package mou.net2;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;

public class UrlCommunicator {

    private static Pattern paramPattern = Pattern.compile("<ITEM>([^(?:</ITEM>)]*)</ITEM>", Pattern.DOTALL);

    private static Pattern errorPattern = Pattern.compile("<ERROR>([^(?:</ERROR>)]*)</ERROR>", Pattern.DOTALL);

    private String targetPath = "http://localhost";

    private List<String> reply_values = new ArrayList<String>();

    private String errorString = "";

    public UrlCommunicator(String targetPath) {
        this.targetPath = targetPath;
    }

    /**
	 * Sendet Request zu im Konstruktor angegebenen Target-Path mit angegebenen Parametern.
	 * @param parameters Key: Parametername, Value: Parameterwert
	 * @param encodeValues true wenn Parameterwerte URL-Encodiert werden muessen
	 */
    public void sendCommand(Map<String, String> parameters, boolean encodeValues) throws IOException {
        reply_values = new ArrayList<String>();
        URL url = new URL(targetPath);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setUseCaches(false);
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setDoInput(true);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        OutputStreamWriter textout = new OutputStreamWriter(bout, "ASCII");
        boolean first = false;
        for (String key : parameters.keySet()) {
            if (StringUtils.isBlank(key)) continue;
            String val = parameters.get(key);
            if (StringUtils.isBlank(val)) val = "";
            if (encodeValues) val = URLEncoder.encode(val, "UTF-8");
            if (first) {
                textout.append('?');
                first = false;
            } else {
                textout.append('&');
            }
            textout.append(key).append('=').append(val);
        }
        textout.flush();
        byte[] encodedData = bout.toByteArray();
        con.setRequestProperty("Content-Length", Integer.toString(encodedData.length));
        con.setFixedLengthStreamingMode(encodedData.length);
        con.getOutputStream().write(encodedData);
        InputStream in = con.getInputStream();
        readReplyParams(in);
        in.close();
    }

    /**
	 * Parst Antwort-Werte aus der InputStream von Webserver
	 */
    protected void readReplyParams(InputStream in) throws IOException {
        String reply = IOUtils.toString(in);
        reply_values = new ArrayList<String>();
        Matcher matcher = paramPattern.matcher(reply);
        while (matcher.find()) {
            String wert = matcher.group(1);
            if (!StringUtils.isBlank(wert)) {
                reply_values.add(URLDecoder.decode(wert, "UTF-8"));
            }
        }
        matcher = errorPattern.matcher(reply);
        if (matcher.find()) {
            errorString = matcher.group(2);
            matcher.group(1);
        }
    }

    public String getError() {
        return (errorString.trim().length() == 0 ? null : errorString);
    }

    /**
	 * 
	 * @return niemals null
	 */
    public List<String> getReplyValues() {
        if (reply_values == null) reply_values = new ArrayList<String>();
        return reply_values;
    }
}
