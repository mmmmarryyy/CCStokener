package com.akop.spark.parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import com.akop.spark.Account;
import com.akop.spark.AuthenticationException;
import com.akop.spark.ParserException;
import com.akop.spark.PsnAccount;
import com.akop.spark.Spark;
import com.akop.spark.XblAccount;
import com.akop.util.SerializableCookie;

public abstract class Parser {

    private static final int TIMEOUT_MS = 10 * 1000;

    protected Context mContext;

    protected DefaultHttpClient mHttpClient;

    protected Parser(Context context) {
        mContext = context;
        mHttpClient = new DefaultHttpClient();
        HttpParams params = mHttpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_MS);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT_MS);
    }

    protected void writeToFile(String filename, String text) {
        java.io.OutputStreamWriter osw = null;
        try {
            java.io.FileOutputStream fOut = mContext.openFileOutput(filename, 0666);
            osw = new java.io.OutputStreamWriter(fOut);
            osw.write(text);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (osw != null) {
                try {
                    osw.flush();
                    osw.close();
                } catch (IOException e) {
                }
            }
        }
    }

    protected String readFromFile(String filename) {
        String text = "";
        java.io.BufferedReader buf = null;
        try {
            java.io.FileInputStream fis = mContext.openFileInput(filename);
            buf = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
            String line;
            while ((line = buf.readLine()) != null) text += line + "\r\n";
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (buf != null) buf.close();
            } catch (IOException e) {
            }
        }
        return text;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        dispose();
    }

    public void dispose() {
        mHttpClient.getConnectionManager().shutdown();
    }

    protected static void setValue(List<NameValuePair> inputs, String name, Object value) {
        for (int i = inputs.size() - 1; i >= 0; i--) if (inputs.get(i).getName().equals(name)) {
            inputs.set(i, new BasicNameValuePair(name, value.toString()));
            break;
        }
    }

    protected static void addValue(List<NameValuePair> inputs, String name, Object value) {
        inputs.add(new BasicNameValuePair(name, value.toString()));
    }

    protected static boolean hasName(List<NameValuePair> inputs, String name) {
        for (int i = inputs.size() - 1; i >= 0; i--) if (inputs.get(i).getName().equals(name)) return true;
        return false;
    }

    protected void submitRequest(HttpUriRequest request) throws IOException {
        synchronized (mHttpClient) {
            mHttpClient.execute(request);
        }
    }

    protected void submitRequest(String url) throws IOException {
        submitRequest(new HttpGet(url));
    }

    protected void submitRequest(String url, List<NameValuePair> inputs) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        HttpEntity entity = new UrlEncodedFormEntity(inputs, HTTP.UTF_8);
        httpPost.setEntity(entity);
        submitRequest(httpPost);
    }

    protected String getResponse(HttpUriRequest request) throws IOException {
        synchronized (mHttpClient) {
            HttpResponse httpResponse = mHttpClient.execute(request);
            HttpEntity entity = httpResponse.getEntity();
            if (entity == null) return null;
            InputStream stream = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream), 10000);
            StringBuilder builder = new StringBuilder(10000);
            String str;
            while ((str = reader.readLine()) != null) builder.append(str);
            stream.close();
            entity.consumeContent();
            return builder.toString();
        }
    }

    protected String getResponse(String url) throws IOException {
        return getResponse(new HttpGet(url));
    }

    protected String getResponse(String url, List<NameValuePair> inputs) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        HttpEntity entity = new UrlEncodedFormEntity(inputs, HTTP.UTF_8);
        httpPost.setEntity(entity);
        return getResponse(httpPost);
    }

    protected static long displayTimeTaken(String description, long started) {
        long now = System.currentTimeMillis();
        Spark.logv("%s: %.02f s", description, (now - started) / 1000.0);
        return now;
    }

    protected static String htmlDecode(String s) {
        return android.text.Html.fromHtml(s).toString().trim();
    }

    protected boolean deleteSession(Account account) {
        return mContext.deleteFile(account.getSessionFile());
    }

    protected boolean saveSession(Account account) {
        ObjectOutputStream objStream = null;
        final List<Cookie> cookies = mHttpClient.getCookieStore().getCookies();
        final List<Cookie> serializableCookies = new ArrayList<Cookie>(cookies.size());
        for (Cookie cookie : cookies) serializableCookies.add(new SerializableCookie(cookie));
        try {
            objStream = new ObjectOutputStream(mContext.openFileOutput(account.getSessionFile(), 0));
            objStream.writeObject(serializableCookies);
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            if (objStream != null) {
                try {
                    objStream.close();
                } catch (IOException e) {
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    protected boolean loadSession(Account account) {
        ObjectInputStream objStream = null;
        final List<Cookie> serializableCookies;
        CookieStore store = mHttpClient.getCookieStore();
        store.clear();
        try {
            objStream = new ObjectInputStream(mContext.openFileInput(account.getSessionFile()));
            serializableCookies = (ArrayList<Cookie>) objStream.readObject();
        } catch (StreamCorruptedException e) {
            return false;
        } catch (IOException e) {
            return false;
        } catch (ClassNotFoundException e) {
            return false;
        } finally {
            if (objStream != null) {
                try {
                    objStream.close();
                } catch (IOException e) {
                }
            }
        }
        for (Cookie cookie : serializableCookies) store.addCookie(cookie);
        return true;
    }

    protected abstract boolean onAuthenticate(Account account) throws IOException;

    protected abstract void onCreateAccount(Account account, ContentValues cv);

    public abstract void deleteAccount(Account account);

    public abstract ContentValues validateAccount(Account account) throws AuthenticationException, IOException, ParserException;

    public static void createAccount(Application app, Account account, ContentValues cv) {
        Parser p = null;
        if (account instanceof XblAccount) p = new XblParser(app); else if (account instanceof PsnAccount) p = new PsnParser(app);
        try {
            p.onCreateAccount(account, cv);
        } finally {
            p.dispose();
        }
    }

    public final boolean authenticate(Account account, boolean useStoredSession) throws IOException {
        long started = System.currentTimeMillis();
        mHttpClient.getCookieStore().clear();
        if (useStoredSession && loadSession(account)) {
            if (Spark.LOGV) Spark.logv("Authenticated with stored session");
        } else {
            if (Spark.LOGV) Spark.logv("Authenticating");
            if (!onAuthenticate(account)) return false;
            if (useStoredSession) saveSession(account);
        }
        if (Spark.LOGV) displayTimeTaken("Authentication completed", started);
        return true;
    }
}
