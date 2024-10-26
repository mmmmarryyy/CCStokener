package net.sf.jfacebookiml;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.http.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.*;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.*;

/**
 * Adapter for the facebook protocol.
 * With it we can login and send/receive facebook chat messages
 * 
 * Primarily borrowed from SIP Communicator.
 * 
 * @author Dai Zhiwei
 * @author Daniel Henninger
 * @author Maxime Chéramy
 *
 */
public class FacebookAdapter {

    private static Logger logger = Logger.getLogger(FacebookAdapter.class);

    public static final String DEFAULT_HOST = "www.facebook.com";

    public static final int DEFAULT_PORT = 80;

    public static final String LOGIN_HOST = "login.facebook.com";

    public static final String APPS_HOST = "apps.facebook.com";

    private String host = DEFAULT_HOST;

    private String proxyHost;

    private int proxyPort;

    private String proxyUsername;

    private String proxyPassword;

    private Boolean useProxy = false;

    /**
	 * The url of the host
	 */
    private String hostUrl;

    /**
     * The url of the login page
     */
    private String loginPageUrl;

    /**
     * The url of the home page
     */
    private String homePageUrl;

    /**
	 * The http client we use to simulate a browser.
	 */
    private FacebookHttpClient facebookHttpClient;

    /**
	 * The UID of this account
	 */
    private String uid = null;

    /**
	 * The channel this account is using
	 */
    private String channel = null;

    /**
	 * The post form id
	 */
    private String post_form_id = null;

    /**
	 * The dtsg value (not sure what this is yet)
	 */
    private String dtsg = null;

    /**
	 * The current seq number
	 */
    private int seq = -1;

    /**
	 * IDs of the messages we receive,<br>
	 * We can know if the incoming message has been handled before via looking up this collection.
	 */
    private HashSet<String> msgIDCollection;

    /**
	 * The buddy list of this account
	 */
    private FacebookBuddyList buddyList;

    /**
	 * true, we keep requesting new message and buddy list from server;
	 * false, we exit the separate thread.
	 */
    private boolean isClientRunning = true;

    /**
	 * The thread which keeps requesting new messages.
	 */
    private Thread msgRequester;

    /**
	 * The thread which requests buddy list every 90 seconds.
	 */
    private Thread buddyListRequester;

    /**
	 * Listeners for facebook events
	 */
    private List<FacebookEventListener> facebookListeners = new CopyOnWriteArrayList<FacebookEventListener>();

    /**
	 * Adapter for each Facebook Chat account.  
	 */
    public FacebookAdapter() {
        init();
    }

    /**
	 * Adapter for each Facebook Chat account.  
	 */
    public FacebookAdapter(String hostname, String port) {
        init();
    }

    private void init() {
        hostUrl = "http://" + host;
        loginPageUrl = "https://login.facebook.com/login.php?login_attempt=1&_fb_noscript=1";
        homePageUrl = hostUrl + "/home.php";
        facebookHttpClient = new FacebookHttpClient(FacebookAdapter.this);
        msgIDCollection = new HashSet<String>();
        msgIDCollection.clear();
        buddyList = new FacebookBuddyList(FacebookAdapter.this);
        isClientRunning = true;
        logger.trace("Facebook: FacebookAdapter() begin");
    }

    /**
	 * Update the buddy list from the given data(JSON Object)
	 * @param buddyListJO the JSON Object that contains the buddy list
	 */
    public void updateBuddyList(JSONObject buddyListJO) {
        try {
            this.buddyList.updateBuddyList(buddyListJO);
        } catch (JSONException e) {
            logger.warn("Facebook: ", e);
        }
    }

    /**
	 * Get the facebook id of this account
	 * @return the facebook id of this account
	 */
    public String getUID() {
        return uid;
    }

    /**
	 * Whether this message id already exists in our collection.
	 * If do, we already handle it, so we just omit it. 
	 * @param msgID the id of current message
	 * @return if this id already exists in our collection
	 */
    public boolean isMessageHandledBefore(String msgID) {
        if (msgIDCollection.contains(msgID)) {
            logger.debug("Facebook: Omitting a already handled message: " + msgIDCollection.contains(msgID));
            return true;
        }
        return false;
    }

    /**
	 * Add the given message id to our collection,
	 * that means this message has been handled.
	 * @param msgID the id of current message
	 */
    public void addMessageToCollection(String msgID) {
        msgIDCollection.add(msgID);
    }

    /**
	 * Initialize the connection,<br>
	 * Initialize the variables, e.g. uid, channel, etc.
	 * 
	 * @param email our facebook "username"
	 * @param pass the password
	 * @return the error code
	 */
    public int initialize(final String email, final String pass) {
        logger.trace("Facebook: initialize() [begin]");
        isClientRunning = true;
        int loginErrorCode = connectAndLogin(email, pass);
        if (loginErrorCode == FacebookErrorCode.Error_Global_NoError) {
            msgRequester = new Thread(new Runnable() {

                public void run() {
                    logger.info("Facebook: Keep requesting...");
                    while (isClientRunning) {
                        try {
                            keepRequesting();
                        } catch (Exception e) {
                            logger.warn("Facebook: Exception while requesting message: ", e);
                        }
                    }
                }
            });
            msgRequester.start();
            buddyListRequester = new Thread(new Runnable() {

                public void run() {
                    logger.info("Facebook: Keep requesting buddylist...");
                    while (isClientRunning) {
                        try {
                            int errorCode = getBuddyList();
                            if (errorCode == FacebookErrorCode.kError_Async_NotLoggedIn) {
                                invokeFacebookDisconnection("Lost login connection.");
                            }
                        } catch (Exception e) {
                            logger.warn("Facebook: Failed to initialize", e);
                        }
                        try {
                            Thread.sleep(60 * 1000);
                        } catch (InterruptedException e) {
                            logger.warn("Facebook: Sleep was interrupted", e);
                        }
                    }
                }
            });
            buddyListRequester.start();
            logger.trace("Facebook: initialize() [END]");
            return FacebookErrorCode.Error_Global_NoError;
        } else if (loginErrorCode == FacebookErrorCode.kError_Login_GenericError) {
            logger.error("Facebook: Not logged in, please check your input or the internet connection!");
        } else {
            logger.error("Facebook: Not logged in, please check your internet connection!");
        }
        logger.trace("Facebook: initialize() [Login Error]");
        return loginErrorCode;
    }

    /**
	 * Connect and login with the email and password
	 * @param email the account email
	 * @param pass the password
	 * @return the error code
	 * @throws URISyntaxException 
	 * @throws UnsupportedEncodingException 
	 */
    private int connectAndLogin(String email, String pass) {
        logger.trace("Facebook: =========connectAndLogin begin===========");
        try {
            HttpPost httpost = new HttpPost(loginPageUrl);
            httpost.addHeader("Cookie", "lsd=abcde; test_cookie=1");
            httpost.addHeader("User-Agent", "Opera/9.50 (Windows NT 5.1; U; en-GB)");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("email", email));
            nvps.add(new BasicNameValuePair("pass", pass));
            nvps.add(new BasicNameValuePair("charset_test", "€,´,€,´,水,Д,Є"));
            nvps.add(new BasicNameValuePair("locale", "en_US"));
            nvps.add(new BasicNameValuePair("pass_placeHolder", "Password"));
            nvps.add(new BasicNameValuePair("persistent", "1"));
            nvps.add(new BasicNameValuePair("lsd", "abcde"));
            nvps.add(new BasicNameValuePair("login", "Login"));
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            logger.info("Facebook: @executing post method to:" + loginPageUrl);
            HttpResponse loginPostResponse = facebookHttpClient.getHttpClient().execute(httpost);
            HttpEntity entity = loginPostResponse.getEntity();
            logger.trace("Facebook: Login form post: " + loginPostResponse.getStatusLine());
            if (entity != null) {
                logger.trace("Facebook: " + EntityUtils.toString(entity));
                entity.consumeContent();
            } else {
                logger.error("Facebook: Error: login post's response entity is null");
                return FacebookErrorCode.kError_Login_GenericError;
            }
            logger.trace("Facebook: Post logon cookies:");
            List<Cookie> cookies = facebookHttpClient.getCookies();
            if (cookies.isEmpty()) {
                logger.trace("Facebook: None");
            } else {
                for (int i = 0; i < cookies.size(); i++) {
                    logger.trace("Facebook: - " + cookies.get(i).toString());
                }
            }
            int statusCode = loginPostResponse.getStatusLine().getStatusCode();
            logger.info("Facebook: Post Method done(" + statusCode + ")");
            switch(statusCode) {
                case 100:
                    break;
                case 301:
                case 302:
                case 303:
                case 307:
                    {
                        Header[] headers = loginPostResponse.getAllHeaders();
                        for (int i = 0; i < headers.length; i++) {
                            logger.trace("Facebook: " + headers[i]);
                        }
                        Header locationHeader = loginPostResponse.getFirstHeader("location");
                        if (locationHeader != null) {
                            homePageUrl = locationHeader.getValue();
                            logger.info("Facebook: Redirect Location: " + homePageUrl);
                            if (homePageUrl == null || !homePageUrl.contains("facebook.com/home.php")) {
                                logger.error("Facebook: Login error! Redirect Location Url not contains \"facebook.com/home.php\"");
                                return FacebookErrorCode.kError_Login_GenericError;
                            }
                        } else {
                            logger.warn("Facebook: Warning: Got no redirect location.");
                        }
                    }
                    break;
                default:
                    ;
            }
            getPostFormIDAndFriends();
        } catch (IOException ioe) {
            logger.error("Facebook: IOException\n" + ioe.getMessage());
            return FacebookErrorCode.kError_Global_ValidationError;
        }
        logger.trace("Facebook: =========connectAndLogin end==========");
        return FacebookErrorCode.Error_Global_NoError;
    }

    /**
	 * Get the home page, and get the information we need,
	 * e.g.:<br>
	 * <ol>
	 * <li>our uid</li>
	 * <li>the channel we're using</li>
	 * <li>the post form id</li>
	 * </ol>
	 * 
	 * @return
	 */
    private int getPostFormIDAndFriends() {
        String getMethodResponseBody = facebookHttpClient.getMethod(hostUrl + "/presence/popout.php");
        if (getMethodResponseBody == null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.trace("Facebook:", e);
            }
            getMethodResponseBody = facebookHttpClient.getMethod(homePageUrl);
        }
        logger.trace("Facebook: =========HomePage: getMethodResponseBody begin=========");
        logger.trace(getMethodResponseBody);
        logger.trace("Facebook: +++++++++HomePage: getMethodResponseBody end+++++++++");
        logger.trace("Facebook: The final cookies:");
        List<Cookie> finalCookies = facebookHttpClient.getCookies();
        if (finalCookies.isEmpty()) {
            logger.trace("Facebook: None");
        } else {
            for (int i = 0; i < finalCookies.size(); i++) {
                logger.trace("Facebook: - " + finalCookies.get(i).toString());
                if (finalCookies.get(i).getName().equals("c_user")) uid = finalCookies.get(i).getValue();
            }
        }
        if (getMethodResponseBody == null) {
            logger.fatal("Facebook: Can't get the home page! Exit.");
            return FacebookErrorCode.Error_Async_UnexpectedNullResponse;
        }
        if (uid == null) {
            logger.fatal("Facebook: Can't get the user's id! Exit.");
            return FacebookErrorCode.Error_System_UIDNotFound;
        }
        String postFormIDPrefix = "<input type=\"hidden\" id=\"post_form_id\" name=\"post_form_id\" value=\"";
        int formIdBeginPos = getMethodResponseBody.indexOf(postFormIDPrefix) + postFormIDPrefix.length();
        if (formIdBeginPos == -1) {
            logger.fatal("Facebook: Error: Can't find post form ID!");
            return FacebookErrorCode.Error_System_PostFormIDNotFound;
        } else {
            post_form_id = getMethodResponseBody.substring(formIdBeginPos, formIdBeginPos + 32);
            logger.info("Facebook: post_form_id: " + post_form_id);
            String dtsgIDPrefix = "fb_dtsg:\"";
            int dtsgBeginPos = getMethodResponseBody.indexOf(dtsgIDPrefix) + dtsgIDPrefix.length();
            if (dtsgBeginPos != -1) {
                String tmp = getMethodResponseBody.substring(dtsgBeginPos);
                dtsg = tmp.substring(0, tmp.indexOf('"'));
                logger.info("Facebook: dtsg: " + dtsg);
            }
            String channelPrefix1 = "js\\\", \\\"channel";
            int channelBeginPos = getMethodResponseBody.indexOf(channelPrefix1);
            if (channelBeginPos != -1) {
                String tmp = getMethodResponseBody.substring(channelBeginPos + 8);
                channel = tmp.substring(0, tmp.indexOf("\\\""));
                logger.info("Facebook: channel: " + channel);
            } else {
                String channelPrefix2 = "js\\\",\\\"channel";
                channelBeginPos = getMethodResponseBody.indexOf(channelPrefix2);
                if (channelBeginPos != -1) {
                    String tmp = getMethodResponseBody.substring(channelBeginPos + 7);
                    channel = tmp.substring(0, tmp.indexOf("\\\""));
                    logger.info("Facebook: channel: " + channel);
                }
            }
            if (channel == null) findChannel();
        }
        return FacebookErrorCode.Error_Global_NoError;
    }

    /**
	 * Post a buddy list request to the facebook server, get and parse the response.
	 * @return the error code
	 */
    private int getBuddyList() {
        logger.trace("Facebook: ====== getBuddyList begin======");
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("buddy_list", "1"));
        nvps.add(new BasicNameValuePair("force_render", "true"));
        nvps.add(new BasicNameValuePair("popped_out", "true"));
        nvps.add(new BasicNameValuePair("post_form_id", post_form_id != null ? post_form_id : "(null)"));
        nvps.add(new BasicNameValuePair("post_form_id_source", "AsyncRequest"));
        nvps.add(new BasicNameValuePair("__a", "1"));
        nvps.add(new BasicNameValuePair("fb_dtsg", dtsg != null ? dtsg : "(null)"));
        nvps.add(new BasicNameValuePair("user", uid));
        try {
            String responseStr = facebookHttpClient.postMethod(hostUrl, "/ajax/chat/buddy_list.php", nvps);
            logger.trace("Facebook: +++++++++ getBuddyList end +++++++++");
            int errorCode = FacebookResponseParser.buddylistParser(FacebookAdapter.this, responseStr);
            return errorCode;
        } catch (JSONException e) {
            logger.warn("Facebook: ", e);
        }
        return FacebookErrorCode.Error_Global_JSONError;
    }

    /**
	 * Post a Facebook Chat message to our contact "to", get and parse the response
	 * @param msg the message to be sent
	 * @param to the buddy to send our message to
	 * @return MessageDeliveryFailedEvent(null if no error)
	 * @throws JSONException json parsing JSONException
	 */
    public void postFacebookChatMessage(String msg, String to) throws JSONException {
        if (to.equals(this.uid)) return;
        logger.trace("Facebook: ====== Post Facebook Chat Message begin======");
        logger.trace("Facebook: PostMessage(): to:" + to);
        logger.trace("Facebook: PostMessage(): msg:" + msg);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("msg_text", (msg == null) ? "" : msg));
        nvps.add(new BasicNameValuePair("msg_id", new Random().nextInt() + ""));
        nvps.add(new BasicNameValuePair("client_time", new Date().getTime() + ""));
        nvps.add(new BasicNameValuePair("to", to));
        nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        logger.info("Facebook: @executeMethod PostMessage() ing... : posting facebook chat message to " + to);
        String responseStr = facebookHttpClient.postMethod(hostUrl, "/ajax/chat/send.php", nvps);
        logger.trace("Facebook: +++++++++ Post Facebook Chat Message end +++++++++");
        FacebookResponseParser.messagePostingResultParser(this, msg, to, responseStr);
    }

    /**
	 * Keep requesting new messages from the server.
	 * If we've got one, parse it, do something to promote the message, and request the next message.
	 * If there's no new message yet, this "thread" just wait for it.
	 * If time out, we try again.
	 * @throws Exception
	 */
    private void keepRequesting() throws Exception {
        seq = getSeq();
        while (isClientRunning) {
            int currentSeq = getSeq();
            logger.trace("Facebook: My seq:" + seq + " | Current seq:" + currentSeq + '\n');
            if (seq > currentSeq) seq = currentSeq;
            while (seq <= currentSeq) {
                String msgResponseBody = facebookHttpClient.getMethod(getMessageRequestingUrl(seq));
                logger.trace("Facebook: =========msgResponseBody begin=========");
                logger.trace(msgResponseBody);
                logger.trace("Facebook: +++++++++msgResponseBody end+++++++++");
                try {
                    FacebookResponseParser.messageRequestResultParser(FacebookAdapter.this, msgResponseBody);
                } catch (JSONException e) {
                    logger.warn("Facebook:", e);
                }
                seq++;
            }
        }
    }

    /**
	 * Get the current seq number from the server via requesting a message with seq=-1<br>
	 * Because -1 is a invalid seq number, the server will return the current seq number.<br>  
	 * @return the current(newest) seq number
	 */
    private int getSeq() {
        int tempSeq = -1;
        while (tempSeq == -1 && isClientRunning) {
            String seqResponseBody;
            try {
                seqResponseBody = facebookHttpClient.getMethod(getMessageRequestingUrl(-1));
                tempSeq = parseSeq(seqResponseBody);
                logger.trace("Facebook: getSeq(): SEQ: " + tempSeq);
                if (tempSeq >= 0) {
                    return tempSeq;
                }
            } catch (JSONException e) {
                logger.warn("Facebook:", e);
            }
            try {
                logger.trace("Facebook: retrying to fetch the seq code after 1 second...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.warn("Facebook: ", e);
            }
        }
        return tempSeq;
    }

    /**
	 * Parse the seq number from the string.
	 * 
	 * @param msgResponseBody the respon we got from the get method
	 * @return if can't parse a seq number successfully, return -1.
	 * @throws JSONException parsing exception
	 */
    private int parseSeq(String msgResponseBody) throws JSONException {
        if (msgResponseBody == null) return -1;
        String prefix = "for (;;);";
        if (msgResponseBody.startsWith(prefix)) msgResponseBody = msgResponseBody.substring(prefix.length());
        if (msgResponseBody == null) return -1;
        JSONObject body = new JSONObject(msgResponseBody);
        if (body != null && body.has("seq")) return body.getInt("seq"); else return -1;
    }

    /**
	 * A util to make a message requesting URL.
	 * @param seq the seq number
	 * @return the message requesting URL
	 */
    private String getMessageRequestingUrl(long seq) {
        String url = "http://0." + channel + ".facebook.com/x/" + new Date().getTime() + "/false/p_" + uid + "=" + seq;
        logger.trace("Facebook: request url:" + url);
        return url;
    }

    /**
	 * We got a message that should be put into the GUI,
	 *  so pass this message to the opration set. 
	 * @param fm facebook message we got
	 */
    public void promoteMessage(FacebookMessage fm) {
        logger.trace("Facebook: in promoteMessage(): Got a message: " + fm.text);
        invokeFacebookMessageEvent(fm);
    }

    /**
	 * Get the buddy who has the given ID from our "buddy cache". 
	 * @param contactID the facebook user ID
	 * @return the buddy, if we can't find him/her, return null.
	 */
    public FacebookUser getBuddyFromCacheByID(String contactID) {
        return buddyList.getBuddyFromCacheByID(contactID);
    }

    /**
     * Get meta info of this account
     * 
     * @return meta info of this account
     */
    public FacebookUser getMyMetaInfo() {
        return buddyList.getMyMetaInfo();
    }

    /**
	 * Set the visibility.
	 * 
	 * @param isVisible true(visible) or false(invisible)
	 */
    public void setVisibility(boolean isVisible) {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("visibility", isVisible + ""));
        nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        logger.info("Facebook: @executeMethod setVisibility() ing ...");
        facebookHttpClient.postMethod(hostUrl, "/ajax/chat/settings.php", nvps);
    }

    /**
	 * Set status message
	 * 
	 * @param statusMsg status message
	 */
    public void setStatusMessage(String statusMsg) {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        if (statusMsg.length() < 1) nvps.add(new BasicNameValuePair("clear", "1")); else nvps.add(new BasicNameValuePair("status", statusMsg));
        nvps.add(new BasicNameValuePair("profile_id", uid));
        nvps.add(new BasicNameValuePair("home_tab_id", "1"));
        nvps.add(new BasicNameValuePair("test_name", "INLINE_STATUS_EDITOR"));
        nvps.add(new BasicNameValuePair("action", "HOME_UPDATE"));
        nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        logger.info("Facebook: @executeMethod setStatusMessage() ing ...");
        facebookHttpClient.postMethod(hostUrl, "/updatestatus.php", nvps);
    }

    /**
	 * Pause the client.<br>
	 * Ensure that initialize() can resume httpclient
	 * 
	 * @fixme logout first
	 */
    public void pause() {
        isClientRunning = false;
        Logout();
    }

    /**
	 * Log out
	 */
    public void Logout() {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("confirm", "1"));
        logger.info("Facebook: @executeMethod Logout() ing ...");
        facebookHttpClient.postMethod(hostUrl, "/logout.php", nvps);
    }

    /**
	 * Shut down the client.
	 */
    public void shutdown() {
        this.isClientRunning = false;
        this.buddyListRequester = null;
        this.msgRequester = null;
        this.facebookHttpClient.getHttpClient().getConnectionManager().shutdown();
        this.facebookHttpClient = null;
        this.msgIDCollection.clear();
        this.buddyList.clear();
    }

    /**
	 * Post typing notification to the given contact.
	 * @param notifiedContact the contact we want to notify
	 * @param typingState our current typing state(SC)
	 * @throws HttpException the http exception
	 * @throws IOException IO exception
	 * @throws JSONException JSON parsing exception
	 * @throws Exception the general exception
	 */
    public void postTypingNotification(String notifiedContact, int typingState) throws HttpException, IOException, JSONException, Exception {
        if (notifiedContact.equals(this.uid)) return;
        int facebookTypingState = 0;
        switch(typingState) {
            case 1:
                facebookTypingState = 1;
                break;
            default:
                facebookTypingState = 0;
                break;
        }
        logger.trace("Facebook: ====== PostTypingNotification begin======");
        logger.trace("Facebook: PostTypingNotification(): to:" + notifiedContact);
        logger.trace("Facebook: PostTypingNotification(): typing state:" + facebookTypingState);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("typ", facebookTypingState + ""));
        nvps.add(new BasicNameValuePair("to", notifiedContact));
        nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        logger.info("Facebook: @executeMethod PostMessage() ing... : posting TypingNotification to " + notifiedContact);
        facebookHttpClient.postMethod(hostUrl, "/ajax/chat/typ.php", nvps);
        logger.trace("Facebook: +++++++++ PostTypingNotification end +++++++++");
    }

    /**
     * Post poke to the given contact.
     * @param notifiedContact the contact we want to poke
     * @throws HttpException the http exception
     * @throws IOException IO exception
     * @throws JSONException JSON parsing exception
     * @throws Exception the general exception
     */
    public void postBuddyPoke(String pokedContact) throws HttpException, IOException, JSONException, Exception {
        logger.trace("Facebook: ====== PostBuddyPoke begin======");
        logger.trace("Facebook: PostBuddyPoke(): to:" + pokedContact);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("pokeback", "0"));
        nvps.add(new BasicNameValuePair("to", pokedContact));
        nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        logger.info("Facebook: @executeMethod PostMessage() ing... : posting Poke to " + pokedContact);
        facebookHttpClient.postMethod(hostUrl, "/ajax/poke.php", nvps);
        logger.trace("Facebook: +++++++++ PostBuddyPoke end +++++++++");
    }

    /**
	 * We got a typing notification from the facebook server,
	 * we pass it to the GUI.
	 * @param fromID where the typing notification from
	 * @param facebookTypingState facebook typing state: 1: typing; 0: stop typing.
	 */
    public void promoteTypingNotification(String fromID, int facebookTypingState) {
        logger.info("Facebook: in promoteTypingNotification(): Got a TypingNotification: " + facebookTypingState);
        switch(facebookTypingState) {
            case 1:
                invokeFacebookTypingEvent(fromID, true);
                break;
            case 0:
                invokeFacebookTypingEvent(fromID, false);
                break;
            default:
                invokeFacebookTypingEvent(fromID, false);
        }
    }

    /**
	 * Get the profile page for parsing. It's invoked when user opens contact info box.
	 * @param contactAddress the contact address
	 * @return profile page string
	 */
    public String getProfilePage(String contactAddress) {
        return facebookHttpClient.getMethod(hostUrl + "/profile.php?id=" + contactAddress + "&v=info");
    }

    /**
      * The method to find and set the channel host.
      */
    private void findChannel() {
        if (post_form_id != null) {
            String url = hostUrl + "/ajax/presence/reconnect.php?reason=7&post_form_id=" + post_form_id + "&__a=1";
            String responseStr = facebookHttpClient.getMethod(url);
            if (responseStr == null) {
                logger.warn("Facebook: Error getting the reconnect page needed to find the channel!");
            } else {
                String hostPrefix = "\"host\":\"";
                int hostBeginPos = responseStr.indexOf(hostPrefix) + hostPrefix.length();
                if (hostBeginPos < hostPrefix.length()) {
                    logger.warn("Facebook: Failed to parse channel");
                } else {
                    logger.debug("Facebook: channel host found.");
                    channel = responseStr.substring(hostBeginPos, responseStr.substring(hostBeginPos).indexOf("\"") + hostBeginPos);
                    logger.debug("Facebook: channel host: " + channel);
                }
            }
        }
    }

    /**
     * Registers a listener with this connection.
     * 
     * @param listener the listener to notify of new messages.
     */
    public void addFacebookListener(FacebookEventListener listener) {
        if (listener == null) {
            return;
        }
        if (!facebookListeners.contains(listener)) {
            facebookListeners.add(listener);
        }
    }

    /**
     * Removes a listener from this connection.
     * 
     * @param listener the listener to remove.
     */
    public void removeFacebookListener(FacebookEventListener listener) {
        if (listener == null) {
            return;
        }
        facebookListeners.remove(listener);
    }

    /**
     * Returns the list of listeners.
     * 
     * @return list of listeners.
     */
    public List<FacebookEventListener> getFacebookListeners() {
        return facebookListeners;
    }

    /**
     * Fires contact status change event to listeners.
     * 
     * @param user Facebook user object including status change.
     */
    public void invokeFacebookUserStatusChangeEvent(FacebookUser user) {
        for (FacebookEventListener listener : facebookListeners) {
            listener.contactChangedStatus(user);
        }
    }

    /**
     * Fires message event to listeners.
     * 
     * @param message Facebook message sent..
     */
    public void invokeFacebookMessageEvent(FacebookMessage message) {
        for (FacebookEventListener listener : facebookListeners) {
            listener.receivedMessageEvent(message);
        }
    }

    /**
     * Fires typing event to listeners.
     * 
     * @param from Who sent the message.
     * @param isTyping whether the person is typing or not.
     */
    public void invokeFacebookTypingEvent(String from, Boolean isTyping) {
        for (FacebookEventListener listener : facebookListeners) {
            listener.receivedTypingEvent(from, isTyping);
        }
    }

    /**
     * Fires error event to listeners.
     * 
     * @param summary Summary of error event.
     * @param description Full description of error event.
     */
    public void invokeFacebookErrorEvent(String summary, String description) {
        for (FacebookEventListener listener : facebookListeners) {
            listener.receivedErrorEvent(summary, description);
        }
    }

    /**
     * Fires a disconnection event to listeners.
     * 
     * @param reason Reason for the disconnection.
     */
    public void invokeFacebookDisconnection(String reason) {
        for (FacebookEventListener listener : facebookListeners) {
            listener.sessionDisconnected(reason);
        }
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setUseProxy(Boolean useProxy) {
        this.useProxy = useProxy;
    }

    public Boolean getUseProxy() {
        return useProxy;
    }

    public void setupProxy(String host, int port, String username, String password) {
        this.proxyUsername = username;
        this.proxyPassword = password;
        this.proxyHost = host;
        this.proxyPort = port;
        this.useProxy = true;
    }
}
