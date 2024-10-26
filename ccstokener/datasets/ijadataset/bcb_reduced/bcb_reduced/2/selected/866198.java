package com.alexmcchesney.twitter;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import javax.xml.bind.*;
import com.alexmcchesney.twitter.jaxb.*;

/**
 * Point of contact with del.icio.us service.
 * @author AMCCHESNEY
 *
 */
public class Service {

    /** 
	 * Time the last call was made.  Used to ensure that we don't do more than one authenticated "get" call per minute,
	 * as this will result in throttling.
	 */
    private static long m_lLastCallTime = 0;

    /** Time to wait between authenticated gets. (One minute) */
    private static int m_iDelayTime = 60000;

    /** Default url of the api */
    public static final String DEFAULT_API_URL = "http://twitter.com/";

    /** Base URL of API */
    private String m_sBaseURL = DEFAULT_API_URL;

    /** Http POST method */
    private static final String POST_METHOD = "POST";

    /** Relative URL to "public timeline" method */
    private static final String PUBLIC_TIMELINE_URL = "statuses/public_timeline.xml";

    /** Relative URL to "friends timeline" method */
    private static final String FRIENDS_TIMELINE_URL = "statuses/friends_timeline.xml";

    /** Relative URL to "show" method.  postnumber.xml must be appended */
    private static final String SHOW_URL = "statuses/show/";

    /** Relative URL to "update" method.  */
    private static final String UPDATE_URL = "statuses/update.xml";

    /** Relative URL to "destroy" method.  postnumber.xml must be appended */
    private static final String DESTROY_URL = "statuses/destroy/";

    /** Relative URL to "replies" method.  */
    private static final String REPLIES_URL = "statuses/replies.xml";

    /** Relative URL to "verify credentials" method */
    private static final String VERIFY_CREDENTIALS_URL = "account/verify_credentials.xml";

    /** Parameter specifying an id to get posts "since" */
    private static final String SINCE_ID_PARAMETER = "since_id";

    /** Parameter specifying a date to get posts "since" */
    private static final String SINCE_PARAMETER = "since";

    /** Generic id parameter */
    private static final String ID_PARAMETER = "id";

    /** Parameter specifying the number of records to return */
    private static final String COUNT_PARAMETER = "count";

    /** Parameter containing the status to set */
    private static final String STATUS_PARAMETER = "status";

    /** Parameter specifying the page of results to get */
    private static final String PAGE_PARAMETER = "page";

    /** User to log into Twitter as */
    private String m_sUserName = null;

    /** Password to log in with */
    private String m_sPassword = null;

    /** Indicates that we are retrying a request after being throttled */
    private boolean m_bRetry = false;

    /** User agent string to send to server. */
    private String m_sUserAgent = "Alex's Twitter library";

    /** Package path to the jaxb context */
    private static final String JAXB_CONTEXT = "com.alexmcchesney.twitter.jaxb";

    /** Error code indicating that we are being throttled */
    private static final int THROTTLED_CODE = 400;

    /** Format of dates passed to/from Twitter server */
    private static final String DATE_FORMAT = "EEE MMM dd HH:mm:ss Z yyyy";

    /** Singleton jaxb context */
    private static JAXBContext m_context;

    /**
	 * Constructor
	 * @param sUserName	User to log into del.icio.us as
	 * @param sPassword	Password for that user
	 */
    public Service(String sUserName, String sPassword) {
        m_sUserName = sUserName;
        m_sPassword = sPassword;
    }

    /**
	 * Sets the value of the user-agent field of http requests.
	 * If not set, a default is used.
	 * @param sAgent	New value.
	 */
    public void setUserAgent(String sAgent) {
        m_sUserAgent = sAgent;
    }

    /**
	 * Sets the base url of the del.icio.us api.  Defaults
	 * to https://api.del.icio.us/v1/ as this is its home at the time
	 * of writing, but if it moves clients can use this method to override it.
	 * @param sURL	New url
	 */
    public void setBaseURL(String sURL) {
        m_sBaseURL = sURL;
    }

    /**
	 * Gets latest public posts
	 * @param sSinceID	If not null, only posts more recent than that with the given
	 * id will be returned.
	 * @return	Array of posts, if any
	 * @throws HttpException Thrown if we cannot make a connection to the del.icio.us server
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws ServerException	Thrown if the server returns an error message
	 * @throws ThrottledException	Thrown if the Twitter server is throttling us
	 */
    public com.alexmcchesney.twitter.StatusPost[] getPublicTimeline(String sSinceID) throws HttpException, InvalidXMLException, ServerException, UnknownMessageException, ThrottledException {
        StringBuilder urlBuilder = new StringBuilder(m_sBaseURL);
        urlBuilder.append(PUBLIC_TIMELINE_URL);
        if (sSinceID != null) {
            appendURLParameter(urlBuilder, SINCE_ID_PARAMETER, sSinceID, true);
        }
        Statuses postsElement = (Statuses) makeCall(urlBuilder.toString(), Statuses.class, false, false);
        return createPostsFromElement(postsElement);
    }

    /**
	 * Gets the "friends" timeline, containing the last 20 posts by the authenticated
	 * user and his/her friends.  Alternatively, can view the friends timeline of
	 * another user, if provided that user's id or screen name.
	 * 
	 * @param sUserID	ID or screen name of the user to get the friends timeline from.  If null, the
	 * authenticated user will be used instead.
	 * @param since	If not null, only posts sent since the given time will be shown.
	 * @return	Array of posts, if any
	 * @throws HttpException Thrown if we cannot make a connection to the del.icio.us server
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws ServerException	Thrown if the server returns an error message
	 * @throws ThrottledException	Thrown if the Twitter server is throttling us
	 */
    public com.alexmcchesney.twitter.StatusPost[] getFriendsTimeline(String sUserID, Date since) throws HttpException, InvalidXMLException, ServerException, UnknownMessageException, ThrottledException {
        StringBuilder urlBuilder = new StringBuilder(m_sBaseURL);
        urlBuilder.append(FRIENDS_TIMELINE_URL);
        if (sUserID != null) {
            appendURLParameter(urlBuilder, ID_PARAMETER, sUserID, true);
        }
        if (since != null) {
            appendURLParameter(urlBuilder, SINCE_PARAMETER, translateDate(since), sUserID == null);
        }
        waitBetweenCalls();
        Statuses postsElement = (Statuses) makeCall(urlBuilder.toString(), Statuses.class, true, false);
        return createPostsFromElement(postsElement);
    }

    /**
	 * Gets the timeline of the currently authenticated user, or another if specified.
	 * 
	 * @param sUserID	ID or screen name of the user to get the timeline from.  If null, the
	 * authenticated user will be used instead.
	 * @param iCount	Maximum number of posts to get.  Maximum of 20.  If this value is greater
	 * than 20 or less than 1, 20 will be returned.
	 * @param since		Optional date to get posts "since"
	 * @return	Array of posts, if any
	 * @throws HttpException Thrown if we cannot make a connection to the Twitter server
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws ServerException	Thrown if the server returns an error message
	 * @throws ThrottledException	Thrown if the Twitter server is throttling us
	 */
    public com.alexmcchesney.twitter.StatusPost[] getUserTimeline(String sUserID, int iCount, Date since) throws HttpException, InvalidXMLException, ServerException, UnknownMessageException, ThrottledException {
        StringBuilder urlBuilder = new StringBuilder(m_sBaseURL);
        urlBuilder.append(FRIENDS_TIMELINE_URL);
        boolean bFirstParam = true;
        if (sUserID != null) {
            appendURLParameter(urlBuilder, ID_PARAMETER, sUserID, bFirstParam);
            bFirstParam = false;
        }
        if (iCount > 0 && iCount < 20) {
            appendURLParameter(urlBuilder, COUNT_PARAMETER, Integer.toString(20), bFirstParam);
            bFirstParam = false;
        }
        if (since != null) {
            appendURLParameter(urlBuilder, SINCE_PARAMETER, translateDate(since), bFirstParam);
        }
        waitBetweenCalls();
        Statuses postsElement = (Statuses) makeCall(urlBuilder.toString(), Statuses.class, true, false);
        return createPostsFromElement(postsElement);
    }

    /**
	 * Gets the 20 most recent "replies" to the authenticated user.
	 * 
	 * @param iPage	The page of results to get.
	 * @return	Array of posts, if any
	 * @throws HttpException Thrown if we cannot make a connection to the Twitter server
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws ServerException	Thrown if the server returns an error message
	 * @throws ThrottledException	Thrown if the Twitter server is throttling us
	 */
    public com.alexmcchesney.twitter.StatusPost[] getReplies(int iPage) throws HttpException, InvalidXMLException, ServerException, UnknownMessageException, ThrottledException {
        StringBuilder urlBuilder = new StringBuilder(m_sBaseURL);
        urlBuilder.append(FRIENDS_TIMELINE_URL);
        if (iPage > 0) {
            appendURLParameter(urlBuilder, PAGE_PARAMETER, Integer.toString(iPage), true);
        }
        waitBetweenCalls();
        Statuses postsElement = (Statuses) makeCall(urlBuilder.toString(), Statuses.class, true, false);
        return createPostsFromElement(postsElement);
    }

    /**
	 * Gets a single status post with the given id.
	 * @param sPostID	The id of the post to get
	 * @return	The post in question.
	 */
    public StatusPost getPost(String sPostID) throws HttpException, InvalidXMLException, ServerException, UnknownMessageException, ThrottledException {
        StringBuilder urlBuilder = new StringBuilder(m_sBaseURL);
        urlBuilder.append(SHOW_URL);
        urlBuilder.append(sPostID);
        urlBuilder.append(".xml");
        waitBetweenCalls();
        Status postElement = (Status) makeCall(urlBuilder.toString(), Status.class, true, false);
        return new StatusPost(postElement);
    }

    /**
	 * Deletes a single status post with the given id.
	 * @param sPostID	The id of the post to delete
	 * @return	The post in question.
	 */
    public StatusPost deletePost(String sPostID) throws HttpException, InvalidXMLException, ServerException, UnknownMessageException, ThrottledException {
        StringBuilder urlBuilder = new StringBuilder(m_sBaseURL);
        urlBuilder.append(DESTROY_URL);
        urlBuilder.append(sPostID);
        urlBuilder.append(".xml");
        waitBetweenCalls();
        Status postElement = (Status) makeCall(urlBuilder.toString(), Status.class, true, false);
        return new StatusPost(postElement);
    }

    /**
	 * Updates the authenticated user's status
	 * @return Post object describing the new status post
	 * @param sStatus
	 * @throws HttpException
	 * @throws InvalidXMLException
	 * @throws ServerException
	 * @throws UnknownMessageException
	 * @throws ThrottledException
	 */
    public StatusPost update(String sStatus) throws HttpException, InvalidXMLException, ServerException, UnknownMessageException, ThrottledException {
        StringBuilder urlBuilder = new StringBuilder(m_sBaseURL);
        urlBuilder.append(UPDATE_URL);
        appendURLParameter(urlBuilder, STATUS_PARAMETER, sStatus, true);
        Status postElement = (Status) makeCall(urlBuilder.toString(), Status.class, true, true);
        return new StatusPost(postElement);
    }

    /**
	 * Quickly validates that the credentials we have are valid
	 * @return
	 */
    public boolean verifyCredentials() throws InvalidXMLException, ServerException, UnknownMessageException, ThrottledException {
        StringBuilder urlBuilder = new StringBuilder(m_sBaseURL);
        urlBuilder.append(VERIFY_CREDENTIALS_URL);
        boolean bValid = false;
        try {
            makeCall(urlBuilder.toString(), Boolean.class, true, false);
            bValid = true;
        } catch (HttpException e) {
        }
        return bValid;
    }

    /**
	 * Appends a parameter to a url in a StringBuilder
	 * @param urlBuilder	StringBuilder to append to
	 * @param sParamName	Name of the parameter to append
	 * @param sParamValue	Value of the parameter
	 * @param bFirstParameter	Flag indicating that this is the first parameter to be added to this url
	 */
    private void appendURLParameter(StringBuilder urlBuilder, String sParamName, String sParamValue, boolean bFirstParameter) throws HttpException {
        if (bFirstParameter) {
            urlBuilder.append("?");
        } else {
            urlBuilder.append("&");
        }
        urlBuilder.append(sParamName);
        urlBuilder.append("=");
        try {
            urlBuilder.append(URLEncoder.encode(sParamValue, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new HttpException(e);
        }
    }

    /**
	 * Calls the Twitter API via http and returns the result.
	 * @param sURL	URL to call
	 * @param expectedResultClass	Once the result is parsed, we expect the root jaxb object
	 * to be of the given class.
	 * @param bAuthenticate	If true, we will pass the username and password with this call.
	 * @param bPost	If true, the request will use POST.  Otherwise, GET.
	 * @return Document document object describing xml structure of the response
	 * @throws HttpException Thrown if we cannot make a connection to the Twitter server
	 * @throws ServerException Thrown if the Twitter server returned an error message
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws  UnknownMessageException	Thrown if the returned message is not what was expected
	 * @throws ThrottledException	Thrown if the Twitter server is throttling us
	 */
    private Object makeCall(String sURL, Class<?> expectedResultClass, boolean bAuthenticate, boolean bPost) throws HttpException, ServerException, InvalidXMLException, UnknownMessageException, ThrottledException {
        URL url = null;
        try {
            url = new URL(sURL);
        } catch (MalformedURLException e) {
            throw new HttpException(e);
        }
        HttpURLConnection con = null;
        InputStream stream = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            if (bPost) {
                con.setRequestMethod(POST_METHOD);
            }
            con.setRequestProperty("user-agent", m_sUserAgent);
            if (bAuthenticate) {
                String auth = "Basic " + (new sun.misc.BASE64Encoder()).encode((m_sUserName + ":" + m_sPassword).getBytes());
                con.setRequestProperty("Authorization", auth);
            }
            con.connect();
            int iResponseCode = con.getResponseCode();
            if (iResponseCode != 200) {
                if (iResponseCode == THROTTLED_CODE && !m_bRetry) {
                    try {
                        Thread.sleep(m_iDelayTime);
                    } catch (InterruptedException e) {
                    }
                    ;
                    m_iDelayTime = m_iDelayTime + 10000;
                    m_bRetry = true;
                    makeCall(sURL, expectedResultClass, bAuthenticate, false);
                } else if (iResponseCode == THROTTLED_CODE) {
                    throw new ThrottledException();
                } else {
                    m_bRetry = false;
                    throw new HttpException(sURL, iResponseCode);
                }
            }
            m_bRetry = false;
            stream = con.getInputStream();
            Object resultObject = null;
            try {
                if (m_context == null) {
                    m_context = JAXBContext.newInstance(JAXB_CONTEXT, this.getClass().getClassLoader());
                }
                Unmarshaller u = m_context.createUnmarshaller();
                resultObject = u.unmarshal(stream);
            } catch (JAXBException jaxbEx) {
                throw new InvalidXMLException(jaxbEx);
            }
            checkResult(resultObject);
            if (!expectedResultClass.isInstance(resultObject)) {
                throw new UnknownMessageException();
            }
            return resultObject;
        } catch (IOException e) {
            throw new HttpException(e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
            if (con != null) {
                con.disconnect();
            }
        }
    }

    /**
	 * Checks the xml result of an operation to validate that an error did not occur.
	 * @param result
	 * @throws UnknownMessageException if the returned content cannot be understood.
	 * @throws ServerException if an error is returned from the del.icio.us server.
	 */
    private void checkResult(Object result) throws UnknownMessageException, ServerException {
        if (result instanceof Hash) {
            String sError = ((Hash) result).getError();
            throw new ServerException(sError);
        }
    }

    /**
	 * Checks if the last call to the server was made less than a minute ago and,
	 * if so, waits for an appropriate time.
	 *
	 */
    private void waitBetweenCalls() {
        java.util.Date now = new java.util.Date();
        long lNow = now.getTime();
        long lDiff = lNow - m_lLastCallTime;
        if (lDiff < m_iDelayTime) {
            try {
                Thread.sleep(m_iDelayTime - lDiff);
            } catch (InterruptedException e) {
            }
        }
        m_lLastCallTime = new java.util.Date().getTime();
    }

    /**
	 * Parses a jaxb StatusesType object representing the "statuses"
	 * element in the returned message, turning it into an array of
	 * StatusPost objects.
	 * 
	 * @param element	The element to parse
	 * @return	Array of StatusPost elements.
	 */
    public StatusPost[] createPostsFromElement(Statuses element) throws UnknownMessageException {
        List<Status> statusList = (List<Status>) element.getStatus();
        int iTotalStatuses = statusList.size();
        StatusPost[] postArray = new StatusPost[iTotalStatuses];
        for (int i = 0; i < iTotalStatuses; i++) {
            postArray[i] = new StatusPost(statusList.get(i));
        }
        return postArray;
    }

    /**
	 * Translates a date string in Twitter format to a Date object
	 * @param sDate	Date string to parse
	 * @return	Date object
	 * @throws UnknownMessageException thrown if the time cannot be parsed
	 */
    static java.util.Date translateDate(String sDate) throws UnknownMessageException {
        DateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        java.util.Date dateValue = null;
        try {
            dateValue = formatter.parse(sDate);
        } catch (ParseException e) {
            throw new UnknownMessageException(e);
        }
        return dateValue;
    }

    static String translateDate(Date date) {
        DateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        return formatter.format(date);
    }
}
