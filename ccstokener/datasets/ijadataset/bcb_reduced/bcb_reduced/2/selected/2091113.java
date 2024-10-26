package hrc.wow_notifier.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public abstract class NotifyService {

    public List<String> loadServerName() {
        return analyseServerName(loadStatusResult());
    }

    public boolean isServerOnline(String serverName) {
        return analyseServerStatus(loadStatusResult(), serverName);
    }

    private String loadStatusResult() {
        try {
            URL url = new URL(getServerUrl());
            InputStream input = url.openStream();
            InputStreamReader is = new InputStreamReader(input, "utf-8");
            BufferedReader reader = new BufferedReader(is);
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            return buffer.toString();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
            return null;
        } catch (IOException e2) {
            e2.printStackTrace();
            return null;
        }
    }

    protected abstract List<String> analyseServerName(String result);

    protected abstract boolean analyseServerStatus(String result, String serverName);

    protected abstract String getServerUrl();
}
