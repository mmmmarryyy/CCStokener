package com.cwxstat.dev;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.InputStream;

public class AuthBot {

    private String remoteurl;

    private WriteData wd;

    private void myinit() {
        wd = new WriteData();
    }

    public AuthBot(String url) {
        this.remoteurl = url;
        myinit();
    }

    public AuthBot() {
        this.remoteurl = "https://23isprime.appspot.com/showsettings/inc";
        myinit();
    }

    public String getKey() {
        return wd.getKey();
    }

    public void setIpaddr(String s) {
        wd.setIpaddr(s);
    }

    public void setHost(String s) {
        wd.setHost(s);
    }

    public void setInstance(String s) {
        wd.setInstance(s);
    }

    public void setCount(String s) {
        wd.setCount(s);
    }

    public boolean getAuth(String content) throws IOException {
        String resp_remote;
        try {
            URL url = new URL(remoteurl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write("md5sum=" + content);
            writer.close();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer();
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\r');
                }
                rd.close();
                resp_remote = response.toString();
                wd.del();
                wd.setKey(resp_remote);
                return true;
            } else {
                return false;
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return false;
    }

    private class WriteData {

        private String ipaddr = "";

        private String host = "";

        private String instance = "";

        private String count = "";

        public void setIpaddr(String s) {
            this.ipaddr = s;
        }

        public void setHost(String s) {
            this.host = s;
        }

        public void setInstance(String s) {
            this.instance = s;
        }

        public void setCount(String s) {
            this.count = s;
        }

        public void setKey(String mkey) {
            Key authlogKey = KeyFactory.createKey("AuthLog", "AuthLog");
            String content = "AuthBotInner";
            Date date = new Date();
            Entity authlog = new Entity("AuthLog", authlogKey);
            authlog.setProperty("key", mkey);
            authlog.setProperty("date", date);
            authlog.setProperty("content", content);
            authlog.setProperty("ipaddr", ipaddr);
            authlog.setProperty("host", host);
            authlog.setProperty("instance", instance);
            authlog.setProperty("count", count);
            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            datastore.put(authlog);
        }

        public String getKey() {
            String key = "null";
            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            Query q = new Query("AuthLog");
            q.addFilter("count", Query.FilterOperator.EQUAL, count);
            q.addFilter("instance", Query.FilterOperator.EQUAL, instance);
            PreparedQuery pq = datastore.prepare(q);
            for (Entity result : pq.asIterable()) {
                key = (String) result.getProperty("key");
            }
            return key;
        }

        public String del() {
            String key = "null";
            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            Query q = new Query("AuthLog");
            q.addFilter("count", Query.FilterOperator.EQUAL, count);
            q.addFilter("instance", Query.FilterOperator.EQUAL, instance);
            PreparedQuery pq = datastore.prepare(q);
            for (Entity result : pq.asIterable()) {
                key = (String) result.getProperty("key");
                datastore.delete(result.getKey());
            }
            return key;
        }
    }
}
