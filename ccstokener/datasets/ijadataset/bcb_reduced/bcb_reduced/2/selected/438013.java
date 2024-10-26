package util.io;

import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.LinkedList;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.cookie.params.*;

public class URLInput {

    static LinkedList<String> user_agents = null;

    public static String readFromURL(String url_) {
        StringBuffer buffer = new StringBuffer();
        try {
            URL url = new URL(url_);
            System.setProperty("http.agent", "");
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
            connection.setDoInput(true);
            InputStream inStream = connection.getInputStream();
            BufferedReader input = new BufferedReader(new InputStreamReader(inStream, "utf8"));
            String line = "";
            while ((line = input.readLine()) != null) {
                buffer.append(line + "\n");
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return buffer.toString();
    }

    public static void writeURLToFile(String url, String path) throws MalformedURLException, IOException {
        java.io.BufferedInputStream in = new java.io.BufferedInputStream(new java.net.URL(url).openStream());
        java.io.FileOutputStream fos = new java.io.FileOutputStream(path);
        java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
        byte data[] = new byte[1024];
        int count;
        while ((count = in.read(data, 0, 1024)) != -1) {
            ;
            bout.write(data, 0, count);
        }
        bout.close();
        in.close();
    }

    public static String submitURLRequest(String url) throws HttpException, IOException, URISyntaxException {
        HttpClient httpclient = new DefaultHttpClient();
        InputStream stream = null;
        user_agents = new LinkedList<String>();
        user_agents.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
        String response_text = "";
        URI uri = new URI(url);
        HttpGet post = new HttpGet(uri);
        int MAX = user_agents.size() - 1;
        int index = (int) Math.round(((double) Math.random() * (MAX)));
        String agent = user_agents.get(index);
        httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, agent);
        httpclient.getParams().setParameter("User-Agent", agent);
        httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.ACCEPT_NONE);
        HttpResponse response = httpclient.execute(post);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            stream = entity.getContent();
            response_text = convertStreamToString(stream);
        }
        httpclient.getConnectionManager().shutdown();
        if (stream != null) {
            stream.close();
        }
        return response_text;
    }

    public static String submitURLRequest(String url, String path) throws HttpException, IOException, URISyntaxException {
        HttpClient httpclient = new DefaultHttpClient();
        InputStream stream = null;
        if (user_agents == null) {
            user_agents = new LinkedList<String>();
            if (path != null) {
                ReadFileInJAR reader = new ReadFileInJAR();
                LinkedList<String> lines = reader.readFromJARFile(path);
                for (int i = 0; i < lines.size(); i++) {
                    user_agents.add(lines.get(i).trim());
                }
            } else {
                user_agents.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
            }
        }
        String response_text = "";
        URI uri = new URI(url);
        HttpGet post = new HttpGet(uri);
        int MAX = user_agents.size() - 1;
        int index = (int) Math.round(((double) Math.random() * (MAX)));
        String agent = user_agents.get(index);
        httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, agent);
        httpclient.getParams().setParameter("User-Agent", agent);
        httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.ACCEPT_NONE);
        HttpResponse response = httpclient.execute(post);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            stream = entity.getContent();
            response_text = convertStreamToString(stream);
        }
        httpclient.getConnectionManager().shutdown();
        if (stream != null) {
            stream.close();
        }
        return response_text;
    }

    public static String submitURLRequestUDF(String url, String path) throws HttpException, IOException, URISyntaxException {
        HttpClient httpclient = new DefaultHttpClient();
        InputStream stream = null;
        if (user_agents == null) {
            user_agents = new LinkedList<String>();
            if (path != null) {
                ReadFileInJAR reader = new ReadFileInJAR();
                LinkedList<String> lines = reader.readFromJARFile(path);
                for (int i = 0; i < lines.size(); i++) {
                    user_agents.add(lines.get(i).trim());
                }
            } else {
                user_agents.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
            }
        }
        String response_text = "";
        URI uri = new URI(url);
        HttpGet post = new HttpGet(uri);
        int MAX = user_agents.size() - 1;
        int index = (int) Math.round(((double) Math.random() * (MAX)));
        String agent = user_agents.get(index);
        httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, agent);
        httpclient.getParams().setParameter("User-Agent", agent);
        httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.ACCEPT_NONE);
        HttpResponse response = httpclient.execute(post);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            stream = entity.getContent();
            response_text = convertStreamToString(stream);
        }
        httpclient.getConnectionManager().shutdown();
        if (stream != null) {
            stream.close();
        }
        return response_text;
    }

    public static String convertStreamToString(InputStream is) throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

    public static void main(String argsv[]) {
        int MAX = 0;
        int index = (int) Math.round(((double) Math.random() * (MAX)));
        System.out.println(index);
    }
}
