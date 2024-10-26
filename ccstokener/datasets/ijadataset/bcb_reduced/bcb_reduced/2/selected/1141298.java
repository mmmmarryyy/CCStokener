package net.sf.genedator.plugin.utils;

import com.thoughtworks.xstream.XStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.genedator.plugin.PluginInfo;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 *
 * @author sylwester.balcerek
 */
public class RemotePluginsManager {

    public static List<PluginInfo> getPluginInfos(String urlRepo) throws MalformedURLException, IOException {
        XStream xStream = new XStream();
        xStream.alias("plugin", PluginInfo.class);
        xStream.alias("plugins", List.class);
        List<PluginInfo> infos = null;
        URL url;
        BufferedReader in = null;
        StringBuilder buffer = new StringBuilder();
        try {
            url = new URL(urlRepo);
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                buffer.append(inputLine);
            }
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(RemotePluginsManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        infos = (List<PluginInfo>) xStream.fromXML(buffer.toString());
        return infos;
    }

    public static void downloadPlugin(String urlString, String fileName) throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(urlString);
        httpGet.addHeader("User-Agent", "Genedator Plugin Manager");
        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(entity.getContent());
            bos = new BufferedOutputStream(new FileOutputStream("plugins/" + fileName));
            int i;
            while ((i = bis.read()) != -1) {
                bos.write(i);
            }
        } finally {
            if (bis != null) {
                bis.close();
            }
            if (bos != null) {
                bos.close();
            }
        }
    }
}
