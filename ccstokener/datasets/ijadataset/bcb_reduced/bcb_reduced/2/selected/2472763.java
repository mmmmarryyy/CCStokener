package com.sametime.twitterclient.twitterapi;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class communicate and parse the data from twitter.com
 * 
 * @since 1.0
 * @author Silvan Imsand
 */
public class TwitterAPI {

    public static final int AUTH_UNKNOWN = 0;

    public static final int AUTH_OK = 1;

    public static final int AUTH_BAD = 2;

    private String username = "";

    private String password = "";

    private String statusMessage = "";

    private int authenthicated = AUTH_UNKNOWN;

    /**
	 * Constructor without name and password. D'ont forget to provide name and
	 * password as we use only twitter-api calls which need authentification
	 */
    public TwitterAPI() {
    }

    /**
	 * Constructor with name and password
	 * 
	 * @param username
	 * @param password
	 */
    public TwitterAPI(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
	 * @return the Username
	 */
    public String getUsername() {
        return username;
    }

    /**
	 * Set a new Username
	 * 
	 * @param username
	 */
    public void setUsername(String username) {
        this.username = username;
        authenthicated = AUTH_UNKNOWN;
    }

    /**
	 * Set a new password
	 * 
	 * @param password
	 */
    public void setPassword(String password) {
        this.password = password;
        authenthicated = AUTH_UNKNOWN;
    }

    /**
	 * Check if the user provided correct login information
	 * 
	 * @return true if username and password are correct
	 */
    public boolean isAuthenthicated() {
        try {
            httpGet("http://twitter.com/account/verify_credentials.xml", false);
        } catch (Exception ex) {
        }
        return authenthicated == AUTH_OK;
    }

    /**
	 * 
	 * @return a base64 encoded Authentication String
	 */
    private String getAuthentificationString() {
        String auth;
        auth = username + ":" + password;
        return Base64.encodeBytes(auth.getBytes());
    }

    /**
	 * Connect to the provided url and returns a String with the answer This
	 * Method is also used to send a POST message with a status-update to
	 * twitter (this should probably be done in a seperate method, but we need
	 * only a limited part of the twitter api, so it isn't worth the effort )
	 * 
	 * @param urlString
	 *            specified API-URL
	 * @param postStatus
	 *            true if we want to include an update with POST
	 * @return answer from the server
	 * @throws Exception
	 */
    private String httpGet(String urlString, boolean postStatus) throws Exception {
        URL url;
        URLConnection conn;
        String answer = "";
        try {
            if (username.equals("") || password.equals("")) throw new AuthNotProvidedException();
            url = new URL(urlString);
            conn = url.openConnection();
            conn.setRequestProperty("Authorization", "Basic " + getAuthentificationString());
            if (postStatus) {
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                DataOutputStream das = new DataOutputStream(conn.getOutputStream());
                String content = "status=" + URLEncoder.encode(statusMessage, "UTF-8") + "&source=" + URLEncoder.encode("sametimetwitterclient", "UTF-8");
                das.writeBytes(content);
                das.flush();
                das.close();
            }
            InputStream is = (InputStream) conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                answer += line + "\n";
            }
            br.close();
        } catch (FileNotFoundException ex) {
            System.out.println(ex.toString());
            throw new RuntimeException("Page not Found. Maybe Twitter-API has changed.");
        } catch (UnknownHostException ex) {
            System.out.println(ex.toString());
            throw new RuntimeException("Network connection problems. Could not find twitter.com");
        } catch (IOException ex) {
            System.out.println("IO-Exception");
            if (ex.getMessage().indexOf("401") > -1) {
                authenthicated = AUTH_BAD;
                throw new AuthNotAcceptedException();
            }
            System.out.println(ex.toString());
        }
        if (checkForError(answer) != null) {
            throw new RuntimeException(checkForError(answer));
        }
        authenthicated = AUTH_OK;
        return answer;
    }

    /**
	 * Sends a new Twitter status for the autheticated user
	 * 
	 * @param statusMessage
	 *            new Message
	 * @return new status Object
	 * @throws Exception
	 */
    public TwitterStatus sendStatusUpdate(String statusMessage) throws Exception {
        String xml;
        ArrayList statusList;
        if (statusMessage.length() >= 160) {
            throw new MessageToLongException();
        }
        this.statusMessage = statusMessage;
        xml = httpGet("http://twitter.com/statuses/update.xml", true);
        statusList = (ArrayList) getStatusList("[" + xml + "]");
        if (statusList.size() != 1) {
            if (checkForError(xml) != null) {
                throw new RuntimeException(checkForError(xml));
            } else {
                throw new RuntimeException("Unkown error while sending the message: " + xml);
            }
        }
        return (TwitterStatus) statusList.get(0);
    }

    /**
	 * Gets the timeline with the messages from friends
	 * 
	 * @return
	 * @throws Exception
	 */
    public List getFriendsTimeline() throws Exception {
        String xml;
        ArrayList statusList;
        xml = httpGet("http://twitter.com/statuses/friends_timeline.xml", false);
        statusList = (ArrayList) getStatusList(xml);
        return statusList;
    }

    /**
	 * Gets the UserTimeline with only messages from the user
	 * 
	 * @return
	 * @throws Exception
	 */
    public List getUserTimeline() throws Exception {
        String xml;
        ArrayList statusList;
        xml = httpGet("http://twitter.com/statuses/user_timeline.xml", false);
        statusList = (ArrayList) getStatusList(xml);
        return statusList;
    }

    /**
	 * Gets the last user Status
	 * 
	 * @return
	 * @throws Exception
	 */
    public TwitterStatus getUserStatus() throws Exception {
        String xml;
        ArrayList statusList;
        xml = httpGet("http://twitter.com/statuses/user_timeline.xml?count=1", false);
        statusList = (ArrayList) getStatusList(xml);
        if (statusList.size() != 1) {
            if (checkForError(xml) != null) {
                throw new RuntimeException(checkForError(xml));
            } else {
                throw new RuntimeException("Error while parsing answer from twitter: " + xml + " " + username + " " + password);
            }
        }
        return (TwitterStatus) statusList.get(0);
    }

    private String checkForError(String message) {
        String errorMessage = null;
        try {
            errorMessage = getMatches(message, "error");
        } catch (Exception e) {
        }
        if (errorMessage == null) {
            if (message.indexOf("Could not authenticate") > -1 && message.indexOf("Could not authenticate") <= 2) errorMessage = message;
        }
        return errorMessage;
    }

    /**
	 * Parses the xml from twitter.com and fill up a List with TwitterStatus
	 * objects
	 * 
	 * @param xml
	 * @return
	 * @throws ParseException 
	 * @throws URISyntaxException 
	 */
    private List getStatusList(String xml) throws ParseException, URISyntaxException {
        List statusList = new ArrayList();
        if (xml.trim().equals("")) {
            return statusList;
        }
        try {
            Pattern p = Pattern.compile("<status>|</status>");
            String[] statusArray;
            statusArray = p.split(xml);
            for (int i = 0; i < statusArray.length; i++) {
                String status = statusArray[i];
                if (status.indexOf("created_at") > -1) {
                    TwitterStatus twitterStatus = new TwitterStatus();
                    String createDate = getMatches(status, "created_at");
                    twitterStatus.setMessageDate(parseDate(createDate));
                    twitterStatus.setMessage(getMatches(status, "text"));
                    twitterStatus.setScreenName(getMatches(status, "screen_name"));
                    String value = getMatches(status, "profile_image_url");
                    if (value != null) {
                        URI uri = parseURL(value);
                        if (uri != null) {
                            twitterStatus.setImagePath(new URI(value));
                        }
                    }
                    statusList.add(twitterStatus);
                }
            }
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }
        return statusList;
    }

    private URI parseURL(String url) {
        URI uri;
        try {
            uri = new URI(url);
            return uri;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
	 * Parse the date and returns 1.1.1970 if parsing failed
	 * @param date
	 * @return
	 */
    private Date parseDate(String date) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.US);
            return formatter.parse(date);
        } catch (Exception e) {
        }
        return new Date(0);
    }

    /**
	 * Filters the XML-message and returns the first found value
	 * 
	 * @param text
	 * @param search
	 * @return
	 */
    private String getMatches(String text, String search) {
        Pattern p;
        Matcher m;
        int start = 0;
        int stop = 0;
        p = Pattern.compile("<" + search + ">");
        m = p.matcher(text);
        if (m.find()) {
            start = m.end();
        } else {
            throw new XMLParsingException("Error while parsing: " + search);
        }
        p = Pattern.compile("</" + search + ">");
        m = p.matcher(text);
        if (m.find()) {
            stop = m.start();
        } else {
            throw new XMLParsingException("Error (1) while parsing: " + search);
        }
        return text.substring(start, stop);
    }
}
