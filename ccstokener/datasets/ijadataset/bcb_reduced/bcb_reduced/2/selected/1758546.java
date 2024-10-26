package org.gtugs.codelab.appengine.blog.datastore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.net.Uri;
import android.util.Log;
import android.util.Xml;

public class Post {

    private Long id = null;

    private Date date;

    private String title;

    private String content;

    private static HttpClient httpClient = null;

    private static HttpGet httpGet = null;

    public Post() {
    }

    public Long getId() {
        return id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void save() {
        URL url = null;
        try {
            url = new URL("http://deris0126.appspot.com/admin/post");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HashMap<String, String> req = new HashMap<String, String>();
        if (null != id) {
            req.put("id", "" + this.id);
        }
        req.put("title", this.title);
        req.put("content", this.content);
        Log.d("Blog", "url:" + url.toString());
        httpPost(url, req);
    }

    private void httpPost(URL url, HashMap options) {
        HttpURLConnection conn = null;
        try {
            String postdata = "";
            if (options != null) {
                StringBuilder builder = new StringBuilder();
                Iterator it = options.keySet().iterator();
                boolean firstLine = true;
                while (it.hasNext()) {
                    String key = (String) it.next();
                    String value = (String) options.get(key);
                    String value_encoded = URLEncoder.encode(value, "UTF-8");
                    if (!firstLine) builder.append('&'); else firstLine = false;
                    builder.append(key + "=" + value_encoded);
                }
                postdata = builder.toString();
            }
            Log.d("Blog", "postdata:" + postdata);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10 * 1000);
            conn.setConnectTimeout(15 * 1000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            if (postdata.length() > 0) {
                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
                writer.write(postdata);
                writer.flush();
                writer.close();
            }
            conn.connect();
            InputStreamReader reader = new InputStreamReader(conn.getInputStream(), "UTF-8");
            reader.close();
        } catch (IOException e) {
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public static List<Post> select() {
        URI uri = null;
        try {
            uri = new URI("http://deris0126.appspot.com/list");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        httpGet = new HttpGet();
        httpGet.setURI(uri);
        HttpResponse resp = null;
        httpClient = new DefaultHttpClient();
        try {
            resp = httpClient.execute(httpGet);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (400 <= resp.getStatusLine().getStatusCode()) {
            Log.w("Blog", resp.getStatusLine().toString());
            return null;
        }
        InputStreamReader in = null;
        try {
            in = new InputStreamReader(resp.getEntity().getContent(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Post.parseList(in);
    }

    public static List<Post> parseList(InputStreamReader in) {
        ArrayList<Post> list = new ArrayList<Post>();
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(in);
        } catch (XmlPullParserException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Log.w("okyuu.com", sw.toString());
            return list;
        }
        Post post = null;
        String xmlTag = null;
        String value = null;
        while (true) {
            int type = -1;
            try {
                type = parser.next();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d("Blog", "key:" + xmlTag);
            Log.d("Blog", "value:" + parser.getText());
            if (XmlPullParser.START_TAG == type) {
                xmlTag = parser.getName();
                if (xmlTag.equals("entry")) {
                    Log.d("Blog", "start:" + xmlTag);
                    post = new Post();
                }
            } else if (XmlPullParser.TEXT == type) {
                value = parser.getText();
                Log.d("Blog", "value:" + value);
            } else if (XmlPullParser.END_TAG == type) {
                xmlTag = parser.getName();
                if (null != post && xmlTag.equals("entry")) {
                    Log.d("Blog", xmlTag);
                    list.add(post);
                    post = null;
                }
            }
            if (null != value && null != post && 3 == parser.getDepth()) {
                if (xmlTag.equals("id")) {
                    post.id = Long.valueOf(value);
                } else if (xmlTag.equals("date")) {
                } else if (xmlTag.equals("title")) {
                    post.title = value;
                } else if (xmlTag.equals("content")) {
                    post.content = value;
                }
            } else if (type == XmlPullParser.END_DOCUMENT) {
                break;
            }
        }
        return list;
    }

    public String toString() {
        return this.title;
    }

    public static Post findById(Long id) {
        Post p = new Post();
        p.id = new Long(id);
        p.setDate(new Date());
        p.setTitle("title " + id);
        p.setContent("content " + id);
        return p;
    }
}
