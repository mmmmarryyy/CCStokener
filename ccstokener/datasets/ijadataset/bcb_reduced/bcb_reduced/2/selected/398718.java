package muhtar.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class MuhtarClient {

    private final String muhtarUrl;

    private long timeout = 1000L * 60L * 60L;

    private Map<String, ConfigItem> configurationMap;

    public MuhtarClient(String host, int port) {
        if (!host.startsWith("http")) {
            host = "http://" + host;
        }
        muhtarUrl = host + ":" + port;
        configurationMap = new HashMap<String, ConfigItem>();
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    private String httpGet(String endpoint, String requestParameters) throws IOException {
        String urlStr = endpoint;
        if (requestParameters != null && requestParameters.length() > 0) {
            urlStr += "?" + requestParameters;
        }
        URL url = new URL(urlStr);
        URLConnection conn = url.openConnection();
        BufferedReader rd = null;
        StringBuffer sb = new StringBuffer();
        try {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            if (rd != null) {
                rd.close();
            }
        }
        return sb.toString();
    }

    public String get(String key) throws IOException {
        return get(key, false);
    }

    public String get(String key, boolean force) throws IOException {
        if (!key.startsWith("/")) {
            key = "/" + key;
        }
        if (force == false && configurationMap.containsKey(key)) {
            ConfigItem configItem = configurationMap.get(key);
            if (configItem.getTimeout() > System.currentTimeMillis()) {
                return configItem.getValue();
            }
        }
        String requestUrl = muhtarUrl + key;
        String value = httpGet(requestUrl, null);
        value = URLDecoder.decode(value, "UTF-8");
        if (value == null || "".equals(value)) {
            value = null;
            configurationMap.remove(key);
        } else {
            configurationMap.put(key, new ConfigItem(key, value, System.currentTimeMillis() + timeout));
        }
        return value;
    }

    public void set(String key, String value) throws IOException {
        if (!key.startsWith("/")) {
            key = "/" + key;
        }
        String requestUrl = muhtarUrl + key;
        if (value == null) {
            httpGet(requestUrl + "?", value);
            configurationMap.remove(key);
        } else {
            httpGet(requestUrl, URLEncoder.encode(value, "UTF-8"));
            configurationMap.put(key, new ConfigItem(key, value, System.currentTimeMillis() + timeout));
        }
    }
}
