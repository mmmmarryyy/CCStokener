package facebookchat.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.JOptionPane;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import facebookchat.ui.chat.Chatroom;
import facebookchat.ui.main.Cheyenne;
import facebookchat.ui.main.LoginDialog;

public class Launcher {

    private static HttpClient httpClient;

    public static Cheyenne fbc;

    public static String loginPageUrl = "http://www.facebook.com/login.php";

    public static String homePageUrl = "http://www.facebook.com/home.php";

    public static String uid = null;

    public static String channel = "15";

    public static String post_form_id = null;

    public static long seq = -1;

    public static HashSet<String> msgIDCollection;

    private static Map<String, Chatroom> chatroomCache;

    private String Proxy_Host = "ISASRV";

    private int Proxy_Port = 80;

    private String Proxy_Username = "daizw";

    private String Proxy_Password = "xxxxxx";

    /**
     * The default parameters.
     * Instantiated in {@link #setup setup}.
     */
    private static HttpParams defaultParameters = null;

    /**
     * The scheme registry.
     * Instantiated in {@link #setup setup}.
     */
    private static SchemeRegistry supportedSchemes;

    public static Chatroom getChatroomAnyway(String uid) {
        uid = uid.trim();
        System.out.println("%%%%%%>" + uid + "<%%%%%%");
        if (chatroomCache.containsKey(uid)) {
            System.out.println("%%%%%%contains key:>" + uid + "<%%%%%%");
            return chatroomCache.get(uid);
        } else {
            System.out.println("%%%%%%new chatroom:>" + uid + "<%%%%%%");
            Chatroom chatroom = new Chatroom(uid);
            chatroomCache.put(uid, chatroom);
            System.out.println("registing chatroom...");
            return chatroom;
        }
    }

    public static boolean isChatroomExist(String uid) {
        uid = uid.trim();
        if (chatroomCache.containsKey(uid)) {
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        System.setProperty("java.util.logging.config.file", "logging.properties");
        Launcher laucher = new Launcher();
        laucher.go();
    }

    public Launcher() {
        msgIDCollection = new HashSet<String>();
        msgIDCollection.clear();
        chatroomCache = new Hashtable<String, Chatroom>();
        chatroomCache.clear();
        final HttpHost target = new HttpHost("www.google.com", 80, "http");
        setup();
        httpClient = createHttpClient();
    }

    private final HttpClient createHttpClient() {
        ClientConnectionManager ccm = new ThreadSafeClientConnManager(getParams(), supportedSchemes);
        DefaultHttpClient dhc = new DefaultHttpClient(ccm, getParams());
        dhc.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        return dhc;
    }

    private void setUpProxy(DefaultHttpClient dhc) {
        final HttpHost proxy = new HttpHost(Proxy_Host, Proxy_Port, "http");
        dhc.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        AuthState authState = new AuthState();
        authState.setAuthScope(new AuthScope(proxy.getHostName(), proxy.getPort()));
        AuthScope authScope = authState.getAuthScope();
        Credentials creds = new UsernamePasswordCredentials(Proxy_Username, Proxy_Password);
        dhc.getCredentialsProvider().setCredentials(authScope, creds);
        System.out.println("executing request via " + proxy);
    }

    /**
     * Performs general setup.
     * This should be called only once.
     */
    private static final void setup() {
        supportedSchemes = new SchemeRegistry();
        SocketFactory sf = PlainSocketFactory.getSocketFactory();
        supportedSchemes.register(new Scheme("http", sf, 80));
        sf = SSLSocketFactory.getSocketFactory();
        supportedSchemes.register(new Scheme("https", sf, 80));
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpProtocolParams.setHttpElementCharset(params, "UTF-8");
        HttpProtocolParams.setUserAgent(params, "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9) Gecko/2008052906 Firefox/3.0");
        defaultParameters = params;
    }

    private static final HttpParams getParams() {
        return defaultParameters;
    }

    /**
     * Creates a request to execute in this example.
     *
     * @return  a request without an entity
     */
    private static final HttpRequest createRequest() {
        HttpRequest req = new BasicHttpRequest("GET", "/", HttpVersion.HTTP_1_1);
        return req;
    }

    public void go() {
        LoginDialog login = new LoginDialog();
        String action = (String) login.showDialog();
        if (action.equals(LoginDialog.CANCELCMD)) System.exit(0);
        String email = login.getUsername();
        String pass = new String(login.getPassword());
        System.out.println(email + ":" + pass);
        int loginErrorCode = doLogin(email, pass);
        if (loginErrorCode == ErrorCode.Error_Global_NoError) {
            if (doParseHomePage() == ErrorCode.Error_Global_NoError) {
                getBuddyList();
                Thread msgRequester = new Thread(new Runnable() {

                    public void run() {
                        System.out.println("Keep requesting...");
                        while (true) {
                            try {
                                keepRequesting();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                msgRequester.start();
                Thread buddyListRequester = new Thread(new Runnable() {

                    public void run() {
                        System.out.println("Keep requesting buddylist...");
                        while (true) {
                            try {
                                getBuddyList();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (fbc != null) fbc.updateBuddyListPane();
                            try {
                                Thread.sleep(60 * 1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                buddyListRequester.start();
                System.out.println("Init GUI...");
                fbc = new Cheyenne();
                fbc.setVisible(true);
            }
        } else if (loginErrorCode == ErrorCode.kError_Async_NotLoggedIn) {
            JOptionPane.showMessageDialog(null, "Not logged in, please check your input!", "Not Logged In", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, "Not logged in, please check your internet connection!", "Not Logged In", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int doLogin(String email, String pass) {
        System.out.println("Target URL: " + loginPageUrl);
        try {
            HttpGet loginGet = new HttpGet(loginPageUrl);
            HttpResponse response = httpClient.execute(loginGet);
            HttpEntity entity = response.getEntity();
            System.out.println("Login form get: " + response.getStatusLine());
            if (entity != null) {
                entity.consumeContent();
            }
            System.out.println("Initial set of cookies:");
            List<Cookie> cookies = ((DefaultHttpClient) httpClient).getCookieStore().getCookies();
            if (cookies.isEmpty()) {
                System.out.println("None");
            } else {
                for (int i = 0; i < cookies.size(); i++) {
                    System.out.println("- " + cookies.get(i).toString());
                }
            }
            HttpPost httpost = new HttpPost(loginPageUrl);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("email", email));
            nvps.add(new BasicNameValuePair("pass", pass));
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse responsePost = httpClient.execute(httpost);
            entity = responsePost.getEntity();
            System.out.println("Login form get: " + responsePost.getStatusLine());
            if (entity != null) {
                entity.consumeContent();
            }
            System.out.println("Post logon cookies:");
            cookies = ((DefaultHttpClient) httpClient).getCookieStore().getCookies();
            if (cookies.isEmpty()) {
                System.out.println("None");
            } else {
                for (int i = 0; i < cookies.size(); i++) {
                    System.out.println("- " + cookies.get(i).toString());
                }
            }
        } catch (IOException ioe) {
            System.err.print("IOException");
            System.err.println(ioe.getMessage());
            ioe.printStackTrace();
            return ErrorCode.kError_Global_ValidationError;
        }
        return ErrorCode.Error_Global_NoError;
    }

    private int doParseHomePage() {
        String getMethodResponseBody = facebookGetMethod(homePageUrl);
        System.out.print("=========HomePage: getMethodResponseBody begin=========");
        System.out.print("+++++++++HomePage: getMethodResponseBody end+++++++++");
        System.out.print("The final cookies:");
        List<Cookie> finalCookies = ((DefaultHttpClient) httpClient).getCookieStore().getCookies();
        if (finalCookies.isEmpty()) {
            System.out.print("None");
        } else {
            for (int i = 0; i < finalCookies.size(); i++) {
                System.out.print("- " + finalCookies.get(i).toString());
                if (finalCookies.get(i).getName().equals("c_user")) uid = finalCookies.get(i).getValue();
            }
        }
        if (getMethodResponseBody == null) {
            System.out.print("Can't get the home page! Exit.");
            return ErrorCode.Error_Async_UnexpectedNullResponse;
        }
        if (uid == null) {
            System.out.print("Can't get the user's id! Exit.");
            return ErrorCode.Error_System_UIDNotFound;
        }
        String channelPrefix = " \"channel";
        int channelBeginPos = getMethodResponseBody.indexOf(channelPrefix) + channelPrefix.length();
        if (channelBeginPos < channelPrefix.length()) {
            System.out.println("Error: Can't find channel!");
            return ErrorCode.Error_System_ChannelNotFound;
        } else {
            channel = getMethodResponseBody.substring(channelBeginPos, channelBeginPos + 2);
            System.out.println("Channel: " + channel);
        }
        String postFormIDPrefix = "<input type=\"hidden\" id=\"post_form_id\" name=\"post_form_id\" value=\"";
        int formIdBeginPos = getMethodResponseBody.indexOf(postFormIDPrefix) + postFormIDPrefix.length();
        if (formIdBeginPos < postFormIDPrefix.length()) {
            System.out.println("Error: Can't find post form ID!");
            return ErrorCode.Error_System_PostFormIDNotFound;
        } else {
            post_form_id = getMethodResponseBody.substring(formIdBeginPos, formIdBeginPos + 32);
            System.out.println("post_form_id: " + post_form_id);
        }
        return ErrorCode.Error_Global_NoError;
    }

    public static void PostMessage(String uid, String msg) {
        if (uid.equals(Launcher.uid)) return;
        System.out.println("====== PostMessage begin======");
        System.out.println("to:" + uid);
        System.out.println("msg:" + msg);
        String url = "http://www.facebook.com/ajax/chat/send.php";
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("msg_text", (msg == null) ? "" : msg));
        nvps.add(new BasicNameValuePair("msg_id", new Random().nextInt(999999999) + ""));
        nvps.add(new BasicNameValuePair("client_time", new Date().getTime() + ""));
        nvps.add(new BasicNameValuePair("to", uid));
        nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        System.out.println("executeMethod ing...");
        try {
            String responseStr = facebookPostMethod("http://www.facebook.com", "/ajax/chat/send.php", nvps);
            System.out.println("+++++++++ PostMessage end +++++++++");
            ResponseParser.messagePostingResultParser(uid, msg, responseStr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void keepRequesting() throws Exception {
        seq = getSeq();
        while (true) {
            int currentSeq = getSeq();
            System.out.println("My seq:" + seq + " | Current seq:" + currentSeq + '\n');
            if (seq > currentSeq) seq = currentSeq;
            while (seq <= currentSeq) {
                String msgResponseBody = facebookGetMethod(getMessageRequestingUrl(seq));
                System.out.println("=========msgResponseBody begin=========");
                System.out.println("+++++++++msgResponseBody end+++++++++");
                try {
                    ResponseParser.messageRequestResultParser(msgResponseBody);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                seq++;
            }
        }
    }

    private int getSeq() {
        int tempSeq = -1;
        while (tempSeq == -1) {
            String seqResponseBody;
            try {
                seqResponseBody = facebookGetMethod(getMessageRequestingUrl(-1));
                tempSeq = parseSeq(seqResponseBody);
                System.out.println("getSeq(): SEQ: " + tempSeq);
                if (tempSeq >= 0) {
                    return tempSeq;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                System.out.println("retrying to retrieve the seq code after 1 second...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return tempSeq;
    }

    private int parseSeq(String msgResponseBody) throws JSONException {
        if (msgResponseBody == null) return -1;
        String prefix = "for (;;);";
        if (msgResponseBody.startsWith(prefix)) msgResponseBody = msgResponseBody.substring(prefix.length());
        JSONObject body = new JSONObject(msgResponseBody);
        if (body != null) return body.getInt("seq"); else return -1;
    }

    private String getMessageRequestingUrl(long seq) {
        String url = "http://0.channel" + channel + ".facebook.com/x/0/false/p_" + uid + "=" + seq;
        System.out.println("request:" + url);
        return url;
    }

    /**
	 * fetch user's info<br>
	 * fetch buddy list<br>
	 * store them in the BuddyList object
	 */
    public static void getBuddyList() {
        System.out.println("====== getBuddyList begin======");
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("buddy_list", "1"));
        nvps.add(new BasicNameValuePair("notifications", "1"));
        nvps.add(new BasicNameValuePair("force_render", "true"));
        nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        nvps.add(new BasicNameValuePair("user", uid));
        try {
            String responseStr = facebookPostMethod("http://www.facebook.com", "/ajax/presence/update.php", nvps);
            System.out.println(responseStr);
            System.out.println("+++++++++ getBuddyList end +++++++++");
            ResponseParser.buddylistParser(responseStr);
        } catch (JSONException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Set status message
     * 
     * @param statusMsg status message
     */
    public static void setStatusMessage(String statusMsg) {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        if (statusMsg.length() < 1) nvps.add(new BasicNameValuePair("clear", "1")); else nvps.add(new BasicNameValuePair("status", statusMsg));
        nvps.add(new BasicNameValuePair("profile_id", uid));
        nvps.add(new BasicNameValuePair("home_tab_id", "1"));
        nvps.add(new BasicNameValuePair("test_name", "INLINE_STATUS_EDITOR"));
        nvps.add(new BasicNameValuePair("action", "HOME_UPDATE"));
        nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        System.out.println("@executeMethod setStatusMessage() ing ... : " + statusMsg);
        String respStr = facebookPostMethod("http://www.facebook.com", "/updatestatus.php", nvps);
        System.out.println(respStr);
    }

    public static void shutdown() {
        httpClient.getConnectionManager().shutdown();
        httpClient = null;
    }

    /**
     * The general facebook post method.
     * @param host the host
     * @param urlPostfix the post fix of the URL
     * @param data the parameter
     * @return the response string
     */
    private static String facebookPostMethod(String host, String urlPostfix, List<NameValuePair> nvps) {
        System.out.println("@executing facebookPostMethod():" + host + urlPostfix);
        String responseStr = null;
        try {
            HttpPost httpost = new HttpPost(host + urlPostfix);
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse postResponse = httpClient.execute(httpost);
            HttpEntity entity = postResponse.getEntity();
            System.out.println("facebookPostMethod: " + postResponse.getStatusLine());
            if (entity != null) {
                responseStr = EntityUtils.toString(entity);
                entity.consumeContent();
            }
            System.out.println("Post Method done(" + postResponse.getStatusLine().getStatusCode() + "), response string length: " + (responseStr == null ? 0 : responseStr.length()));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return responseStr;
    }

    /**
     * The general facebook get method.
     * @param url the URL of the page we wanna get
     * @return the response string
     */
    private static String facebookGetMethod(String url) {
        System.out.println("@executing facebookGetMethod():" + url);
        String responseStr = null;
        try {
            HttpGet loginGet = new HttpGet(url);
            HttpResponse response = httpClient.execute(loginGet);
            HttpEntity entity = response.getEntity();
            System.out.println("facebookGetMethod: " + response.getStatusLine());
            if (entity != null) {
                responseStr = EntityUtils.toString(entity);
                entity.consumeContent();
            }
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                System.out.println("Error Occured! Status Code = " + statusCode);
                responseStr = null;
            }
            System.out.println("Get Method done(" + statusCode + "), response string length: " + (responseStr == null ? 0 : responseStr.length()));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return responseStr;
    }
}
