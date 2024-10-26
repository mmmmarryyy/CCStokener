package com.alexmcchesney.delicious;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import javax.xml.bind.*;
import org.apache.log4j.Logger;
import com.alexmcchesney.delicious.jaxb.*;

/**
 * Point of contact with del.icio.us service.
 * @author AMCCHESNEY
 *
 */
public class Service {

    /** Time the last call was made.  Used to ensure that we don't make more than 1 call per second */
    private static long m_lLastCallTime = 0;

    /** Time to wait between calls.  Starts at one second, but if it looks like we're being throttled we'll increase it */
    private static int m_iDelayTime = 1000;

    /** Default url of the api */
    public static final String DEFAULT_API_URL = "https://api.del.icio.us/v1/";

    /** Base URL of API */
    private String m_sBaseURL = DEFAULT_API_URL;

    /** Relative URL to "update" method */
    private static final String UPDATE_URL = "posts/update";

    /** Relative URL to "tags/get" method */
    private static final String TAGS_GET_URL = "tags/get";

    /** Relative URL to "tags/rename" method */
    private static final String TAGS_RENAME_URL = "tags/rename";

    /** Relative URL to "posts/add" method */
    private static final String POSTS_ADD_URL = "posts/add";

    /** Relative URL to "posts/get" method */
    private static final String POSTS_GET_URL = "posts/get";

    /** Relative URL to "posts/all" method */
    private static final String POSTS_ALL_URL = "posts/all";

    /** Relative URL to "posts/delete" method */
    private static final String POSTS_DELETE_URL = "posts/delete";

    /** Relative URL to "posts/recent" method */
    private static final String POSTS_RECENT_URL = "posts/recent";

    /** Relative URL to "posts/dates" method */
    private static final String POSTS_DATES_URL = "posts/dates";

    /** Relative URL to "bundles/all" method */
    private static final String BUNDLES_ALL_URL = "tags/bundles/all";

    /** Relative URL to "bundles/set" method */
    private static final String BUNDLES_SET_URL = "tags/bundles/set";

    /** Relative URL to "bundles/delete" method */
    private static final String BUNDLES_DELETE_URL = "tags/bundles/delete";

    /** Value of result code for successful operations */
    private static final String SUCCESS_CODE = "done";

    /** Alternative balue of result code for some operations (inconsistancy within del.icio.us api) */
    private static final String ALT_SUCCESS_CODE = "ok";

    /** URL parameter for the old name of a tag */
    private static final String OLD_PARAM = "old";

    /** URL parameter for the new name of a tag */
    private static final String NEW_PARAM = "new";

    /** URL parameter for the url of a post */
    private static final String URL_PARAM = "url";

    /** URL parameter for the description of a post */
    private static final String DESCRIPTION_PARAM = "description";

    /** URL parameter for the extended notes of a post */
    private static final String NOTES_PARAM = "extended";

    /** URL parameter for the tags of a post */
    private static final String TAGS_PARAM = "tags";

    /** URL parameter for a tag */
    private static final String TAG_PARAM = "tag";

    /** URL parameter for a date */
    private static final String DATE_PARAM = "date";

    /** URL parameter for the "replace" flag */
    private static final String REPLACE_PARAM = "replace";

    /** URL parameter for the "shared" flag */
    private static final String SHARED_PARAM = "shared";

    /** URL parameter for the count of posts to get */
    private static final String COUNT_PARAM = "count";

    /** URL parameter for a bundle name */
    private static final String BUNDLE_PARAM = "bundle";

    /** "True" value for flag parameters */
    private static final String PARAM_TRUE = "yes";

    /** "False" value for flag parameters */
    private static final String PARAM_FALSE = "no";

    /** Format of times returned by del.icio.us */
    private static final String TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /** Format of dates returned by del.icio.us */
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    /** User to log into del.icio.us as */
    private String m_sUserName = null;

    /** Password to log in with */
    private String m_sPassword = null;

    /** Indicates that we are retrying a request after being throttled */
    private boolean m_bRetry = false;

    /** User agent string to send to server. */
    private String m_sUserAgent = "Alex's del.icio.us library";

    /** Package path to the jaxb context */
    private static final String JAXB_CONTEXT = "com.alexmcchesney.delicious.jaxb";

    /** Http "ok" response code */
    private static final int OK_CODE = 200;

    /** Http "unauthorized" code */
    private static final int UNAUTHORIZED_CODE = 401;

    /** Http response code indicating that the server is throttling us */
    private static final int THROTTLED_CODE = 503;

    /** Debug log */
    private static final Logger m_log = Logger.getLogger(Service.class);

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
	 * Gets the last time your account was updated (had a post added/
	 * removed/modified)
	 * @return	Time of the last update
	 * @throws HttpException Thrown if we cannot make a connection to the del.icio.us server
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws UnknownMessageException Thrown if the server returned valid xml, but it could not be understood
	 * by this library.
	 * @throws ThrottledException	Thrown if the del.icio.us server is throttling us
	 */
    public java.util.Date getLastUpdate() throws HttpException, InvalidXMLException, UnknownMessageException, ServerException, ThrottledException, UnauthorizedException {
        String sURL = m_sBaseURL + UPDATE_URL;
        Update updateElement = (Update) makeCall(sURL, Update.class);
        String sTime = updateElement.getTime();
        if (sTime == null || sTime.length() == 0) {
            throw new UnknownMessageException();
        }
        return translateTime(sTime);
    }

    /**
	 * Gets details of all the tags in a del.icio.us account.  Only returns tags that contain
	 * public (shared) posts.
	 * 
	 * @return	Array of Tag objects
	 * @throws HttpException Thrown if we cannot make a connection to the del.icio.us server
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws UnknownMessageException Thrown if the server returned valid xml, but it could not be understood
	 * by this library.
	 * @throws ServerException	Thrown if an error message is returned from the server
	 * @throws ThrottledException	Thrown if the del.icio.us server is throttling us
	 */
    public com.alexmcchesney.delicious.Tag[] getTags() throws HttpException, InvalidXMLException, UnknownMessageException, ServerException, ThrottledException, UnauthorizedException {
        String sURL = m_sBaseURL + TAGS_GET_URL;
        Tags tagsElement = (Tags) makeCall(sURL, Tags.class);
        List<com.alexmcchesney.delicious.jaxb.Tag> tagList = tagsElement.getTag();
        com.alexmcchesney.delicious.Tag[] tags;
        if (tagList != null) {
            int iTotalTags = tagList.size();
            tags = new Tag[iTotalTags];
            for (int i = 0; i < iTotalTags; i++) {
                tags[i] = new com.alexmcchesney.delicious.Tag(tagList.get(i));
            }
        } else {
            tags = new com.alexmcchesney.delicious.Tag[0];
        }
        return tags;
    }

    /**
	 * Gets details of all the tags in a del.icio.us account
	 * @return	Array of Tag objects
	 * @throws HttpException Thrown if we cannot make a connection to the del.icio.us server
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws UnknownMessageException Thrown if the server returned valid xml, but it could not be understood
	 * by this library.
	 * @throws ServerException	Thrown if an error was returned from the server
	 * @throws ThrottledException	Thrown if the del.icio.us server is throttling us
	 */
    public void renameTag(String sOldName, String sNewName) throws HttpException, InvalidXMLException, UnknownMessageException, ServerException, ThrottledException, UnauthorizedException {
        String sURL = null;
        try {
            sURL = m_sBaseURL + TAGS_RENAME_URL + "?" + OLD_PARAM + "=" + URLEncoder.encode(sOldName, "UTF-8") + "&" + NEW_PARAM + "=" + URLEncoder.encode(sNewName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new HttpException(e);
        }
        makeCall(sURL, Result.class);
    }

    /**
	 * Sends a new post to the account.
	 * 
	 * @param sPostURL				URL to be bookmarked
	 * @param sDescription		Short description of the bookmark
	 * @param sNotes			Extended description.  Optional.
	 * @param sTags				Array of tags.  Optional.
	 * @param bReplace			If true, an existing post with the same url will be replaced by this one.
	 * @param bShared			If true, the post will be public.  Otherwise, private.
	 * @throws HttpException Thrown if we cannot make a connection to the del.icio.us server
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws UnknownMessageException Thrown if the server returned valid xml, but it could not be understood
	 * by this library.
	 * @throws ServerException	Thrown if an error was returned from the server
	 * @throws ThrottledException	Thrown if the del.icio.us server is throttling us
	 */
    public void newPost(String sPostURL, String sDescription, String sNotes, String[] sTags, boolean bReplace, boolean bShared) throws HttpException, InvalidXMLException, UnknownMessageException, ServerException, ThrottledException, UnauthorizedException {
        StringBuilder tagStringBuilder = new StringBuilder();
        if (sTags != null) {
            int iTotalTags = sTags.length;
            for (int i = 0; i < iTotalTags; i++) {
                tagStringBuilder.append(sTags[i]);
                if (i + 1 < iTotalTags) {
                    tagStringBuilder.append(" ");
                }
            }
        }
        newPost(sPostURL, sDescription, sNotes, tagStringBuilder.toString(), bReplace, bShared);
    }

    /**
	 * Sends a new post to the account.
	 * 
	 * @param sPostURL				URL to be bookmarked
	 * @param sDescription		Short description of the bookmark
	 * @param sNotes			Extended description.  Optional.
	 * @param sTags				Space-delimited tags.  Optional.
	 * @param bReplace			If true, an existing post with the same url will be replaced by this one.
	 * @param bShared			If true, the post will be public.  Otherwise, private.
	 * @throws HttpException Thrown if we cannot make a connection to the del.icio.us server
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws UnknownMessageException Thrown if the server returned valid xml, but it could not be understood
	 * by this library.
	 * @throws ServerException	Thrown if an error was returned from the server
	 * @throws ThrottledException	Thrown if the del.icio.us server is throttling us
	 */
    public void newPost(String sPostURL, String sDescription, String sNotes, String sTags, boolean bReplace, boolean bShared) throws HttpException, InvalidXMLException, UnknownMessageException, ServerException, ThrottledException, UnauthorizedException {
        StringBuilder urlBuilder = new StringBuilder(m_sBaseURL);
        urlBuilder.append(POSTS_ADD_URL);
        appendURLParameter(urlBuilder, URL_PARAM, sPostURL, true);
        appendURLParameter(urlBuilder, DESCRIPTION_PARAM, sDescription, false);
        appendURLParameter(urlBuilder, REPLACE_PARAM, bReplace ? PARAM_TRUE : PARAM_FALSE, false);
        appendURLParameter(urlBuilder, SHARED_PARAM, bShared ? PARAM_TRUE : PARAM_FALSE, false);
        if (sNotes != null && sNotes.length() > 0) {
            appendURLParameter(urlBuilder, NOTES_PARAM, sNotes, false);
        }
        if (sTags != null && sTags.length() > 0) {
            appendURLParameter(urlBuilder, TAGS_PARAM, sTags, false);
        }
        makeCall(urlBuilder.toString(), Result.class);
    }

    /**
	 * Deletes a post from the account.
	 * 
	 * @param sPostURL				URL to be deleted
	 * @throws HttpException Thrown if we cannot make a connection to the del.icio.us server
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws UnknownMessageException Thrown if the server returned valid xml, but it could not be understood
	 * by this library.
	 * @throws ServerException	Thrown if an error was returned from the server
	 * @throws ThrottledException	Thrown if the del.icio.us server is throttling us
	 */
    public void deletePost(String sPostURL) throws HttpException, InvalidXMLException, UnknownMessageException, ServerException, ThrottledException, UnauthorizedException {
        StringBuilder urlBuilder = new StringBuilder(m_sBaseURL);
        urlBuilder.append(POSTS_DELETE_URL);
        appendURLParameter(urlBuilder, URL_PARAM, sPostURL, true);
        makeCall(urlBuilder.toString(), Result.class);
    }

    /**
	 * Gets a mapping of dates on which posts were made, and how many were made on those dates
	 * 
	 * @param sTag	Tag to filter on.  Optional.
	 * @throws HttpException Thrown if we cannot make a connection to the del.icio.us server
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws UnknownMessageException Thrown if the server returned valid xml, but it could not be understood
	 * by this library.
	 * @throws ServerException	Thrown if an error was returned from the server
	 * @throws ThrottledException	Thrown if the del.icio.us server is throttling us
	 */
    public HashMap<java.util.Date, Integer> getDates(String sTag) throws HttpException, InvalidXMLException, UnknownMessageException, ServerException, ThrottledException, UnauthorizedException {
        StringBuilder urlBuilder = new StringBuilder(m_sBaseURL);
        urlBuilder.append(POSTS_DATES_URL);
        if (sTag != null) {
            appendURLParameter(urlBuilder, TAG_PARAM, sTag, true);
        }
        Dates datesElement = (Dates) makeCall(urlBuilder.toString(), Dates.class);
        Iterator<com.alexmcchesney.delicious.jaxb.Date> it = datesElement.getDate().iterator();
        HashMap<java.util.Date, Integer> dateMap = new HashMap<java.util.Date, Integer>();
        while (it.hasNext()) {
            com.alexmcchesney.delicious.jaxb.Date dateElement = it.next();
            dateMap.put(translateDate(dateElement.getDate()), dateElement.getCount().intValue());
        }
        return dateMap;
    }

    /**
	 * Gets posts from the server.	If no filter is provided, the most recent date is used.
	 * @param sTagFilter	Tag to filter by.  Optional.
	 * @param dateFilter	Date to filter by.  Optional.
	 * @param sURLFilter	URL to filter by. Optional.
	 * @return	Array of matching posts, if any
	 * @throws HttpException Thrown if we cannot make a connection to the del.icio.us server
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws ServerException	Thrown if the server returns an error message
	 * @throws ThrottledException	Thrown if the del.icio.us server is throttling us
	 */
    public com.alexmcchesney.delicious.Post[] getPosts(String sTagFilter, java.util.Date dateFilter, String sURLFilter) throws HttpException, InvalidXMLException, ServerException, UnknownMessageException, ThrottledException, UnauthorizedException {
        StringBuilder urlBuilder = new StringBuilder(m_sBaseURL);
        urlBuilder.append(POSTS_GET_URL);
        boolean bFirstParameter = true;
        if (sTagFilter != null && sTagFilter.length() > 0) {
            appendURLParameter(urlBuilder, TAG_PARAM, sTagFilter, bFirstParameter);
            bFirstParameter = false;
        }
        if (dateFilter != null) {
            appendURLParameter(urlBuilder, DATE_PARAM, translateTime(dateFilter), bFirstParameter);
            bFirstParameter = false;
        }
        if (sURLFilter != null) {
            appendURLParameter(urlBuilder, URL_PARAM, sURLFilter, bFirstParameter);
            bFirstParameter = false;
        }
        Posts postsElement = (Posts) makeCall(urlBuilder.toString(), Posts.class);
        return createPostsFromElement(postsElement);
    }

    /**
	 * Gets all posts in the account from the server.
	 * @return	Array of posts, if any
	 * @throws HttpException Thrown if we cannot make a connection to the del.icio.us server
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws ServerException	Thrown if the server returns an error message
	 * @throws ThrottledException	Thrown if the del.icio.us server is throttling us
	 */
    public com.alexmcchesney.delicious.Post[] getAllPosts() throws HttpException, InvalidXMLException, ServerException, UnknownMessageException, ThrottledException, UnauthorizedException {
        StringBuilder urlBuilder = new StringBuilder(m_sBaseURL);
        urlBuilder.append(POSTS_ALL_URL);
        Posts postsElement = (Posts) makeCall(urlBuilder.toString(), Posts.class);
        return createPostsFromElement(postsElement);
    }

    /**
	 * Gets recent posts from the server
	 * @param iMaxPosts		Maximum number of recent posts to return.  Can be no more than 100
	 * @param sTagFilter	Tag to filter results on
	 * @return	Array of matching posts, if any
	 * @throws HttpException Thrown if we cannot make a connection to the del.icio.us server
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws ServerException	Thrown if the server returns an error message
	 * @throws ThrottledException	Thrown if the del.icio.us server is throttling us
	 */
    public com.alexmcchesney.delicious.Post[] getRecentPosts(int iMaxPosts, String sTagFilter) throws HttpException, InvalidXMLException, ServerException, UnknownMessageException, ThrottledException, UnauthorizedException {
        StringBuilder urlBuilder = new StringBuilder(m_sBaseURL);
        urlBuilder.append(POSTS_RECENT_URL);
        appendURLParameter(urlBuilder, COUNT_PARAM, Integer.toString(iMaxPosts), true);
        if (sTagFilter != null && sTagFilter.length() > 0) {
            appendURLParameter(urlBuilder, TAG_PARAM, sTagFilter, false);
        }
        Posts postsElement = (Posts) makeCall(urlBuilder.toString(), Posts.class);
        return createPostsFromElement(postsElement);
    }

    /**
	 * Creates an array of post objects from a PostsType jaxb element
	 * @param postsElement
	 * @return
	 * @throws UnknownMessageException
	 */
    private com.alexmcchesney.delicious.Post[] createPostsFromElement(Posts postsElement) throws UnknownMessageException {
        List<com.alexmcchesney.delicious.jaxb.Post> postList = postsElement.getPost();
        int iTotalPosts = postList.size();
        com.alexmcchesney.delicious.Post[] posts = new Post[iTotalPosts];
        for (int i = 0; i < iTotalPosts; i++) {
            posts[i] = new com.alexmcchesney.delicious.Post(postList.get(i));
        }
        return posts;
    }

    /**
	 * Gets all bundles known to the account
	 * @throws HttpException Thrown if we cannot make a connection to the del.icio.us server
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws ServerException	Thrown if the server returns an error message
	 * @throws ThrottledException	Thrown if the del.icio.us server is throttling us
	 * @return HashMap of bundle name strings to string arrays containing tags included in the bundles
	 */
    public HashMap<String, String[]> getAllBundles() throws HttpException, InvalidXMLException, ServerException, UnknownMessageException, ThrottledException, UnauthorizedException {
        StringBuilder urlBuilder = new StringBuilder(m_sBaseURL);
        urlBuilder.append(BUNDLES_ALL_URL);
        Bundles bundlesElement = (Bundles) makeCall(urlBuilder.toString(), Bundles.class);
        HashMap<String, String[]> bundleMap = new HashMap<String, String[]>();
        Iterator<Bundle> it = bundlesElement.getBundle().iterator();
        while (it.hasNext()) {
            Bundle bundle = it.next();
            bundleMap.put(bundle.getName(), bundle.getTags().split(" "));
        }
        return bundleMap;
    }

    /**
	 * Creates or modifies a tag bundle
	 * @param sName	Name of the bundle
	 * @param sTags	Space-delimited list of tags to add to the bundle
	 * @throws HttpException Thrown if we cannot make a connection to the del.icio.us server
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws ServerException	Thrown if the server returns an error message
	 * @return HashMap of bundle name strings to string arrays containing tags included in the bundles
	 * @throws ThrottledException	Thrown if the del.icio.us server is throttling us
	 */
    public void setBundle(String sName, String sTags) throws HttpException, InvalidXMLException, ServerException, UnknownMessageException, ThrottledException, UnauthorizedException {
        StringBuilder urlBuilder = new StringBuilder(m_sBaseURL);
        urlBuilder.append(BUNDLES_SET_URL);
        appendURLParameter(urlBuilder, BUNDLE_PARAM, sName, true);
        appendURLParameter(urlBuilder, TAGS_PARAM, sTags, false);
        makeCall(urlBuilder.toString(), Result.class);
    }

    /**
	 * Deletes a tag bundle
	 * @param sName	Name of the bundle
	 * @throws HttpException Thrown if we cannot make a connection to the del.icio.us server
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws ServerException	Thrown if the server returns an error message
	 * @throws ThrottledException	Thrown if the del.icio.us server is throttling us
	 * @return HashMap of bundle name strings to string arrays containing tags included in the bundles
	 */
    public void deleteBundle(String sName) throws HttpException, InvalidXMLException, ServerException, UnknownMessageException, ThrottledException, UnauthorizedException {
        StringBuilder urlBuilder = new StringBuilder(m_sBaseURL);
        urlBuilder.append(BUNDLES_DELETE_URL);
        appendURLParameter(urlBuilder, BUNDLE_PARAM, sName, true);
        makeCall(urlBuilder.toString(), Result.class);
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
	 * Calls the del.icio.us API via https and returns the result.
	 * @param sURL	URL to call
	 * @param expectedResultClass	Once the result is parsed, we expect the root jaxb object
	 * to be of the given class.
	 * @return Document document object describing xml structure of the response
	 * @throws HttpException Thrown if we cannot make a connection to the del.icio.us server
	 * @throws ServerException Thrown if the del.icio.us erver returned an error message
	 * @throws InvalidXMLException Thrown if the server does not return an xml message we can parse.
	 * @throws  UnknownMessageException	Thrown if the returned message is not what was expected
	 * @throws ThrottledException	Thrown if the del.icio.us server is throttling us
	 */
    private Object makeCall(String sURL, Class<?> expectedResultClass) throws HttpException, ServerException, InvalidXMLException, UnknownMessageException, ThrottledException, UnauthorizedException {
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
            con.setRequestProperty("user-agent", m_sUserAgent);
            String auth = "Basic " + (new sun.misc.BASE64Encoder()).encode((m_sUserName + ":" + m_sPassword).getBytes());
            con.setRequestProperty("Authorization", auth);
            waitBetweenCalls();
            con.connect();
            m_lLastCallTime = new java.util.Date().getTime();
            int iResponseCode = con.getResponseCode();
            if (iResponseCode != 200) {
                if (iResponseCode == THROTTLED_CODE && !m_bRetry) {
                    try {
                        Thread.sleep(m_iDelayTime);
                    } catch (InterruptedException e) {
                    }
                    ;
                    m_iDelayTime = m_iDelayTime + 500;
                    m_bRetry = true;
                    makeCall(sURL, expectedResultClass);
                } else if (iResponseCode == THROTTLED_CODE) {
                    throw new ThrottledException();
                } else if (iResponseCode == UNAUTHORIZED_CODE) {
                    throw new UnauthorizedException();
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
                u.setEventHandler(new ValidationEventHandler() {

                    public boolean handleEvent(ValidationEvent event) {
                        boolean bContinue;
                        if (event.getSeverity() == ValidationEvent.FATAL_ERROR) {
                            bContinue = false;
                            m_log.warn("Fatal unmarshal error: " + event.getMessage());
                        } else {
                            bContinue = true;
                            m_log.warn("Unmarshal error: " + event.getMessage());
                        }
                        return bContinue;
                    }
                });
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
        if (result instanceof Result) {
            String sResultCode = ((Result) result).getValue();
            if (sResultCode == null || sResultCode.length() == 0) {
                sResultCode = ((Result) result).getCode();
            }
            if (sResultCode == null || sResultCode.length() == 0) {
                throw new UnknownMessageException();
            }
            if (!sResultCode.equals(SUCCESS_CODE) && !sResultCode.equals(ALT_SUCCESS_CODE)) {
                throw new ServerException(sResultCode);
            }
        }
    }

    /**
	 * Checks if the last call to the server was made less than a second ago and,
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
    }

    /**
	 * Translates a time string in del.icio.us format to a Date object
	 * @param sTime	Time string to parse
	 * @return	Date object
	 * @throws UnknownMessageException thrown if the time cannot be parsed
	 */
    static java.util.Date translateTime(String sTime) throws UnknownMessageException {
        DateFormat formatter = new SimpleDateFormat(TIME_FORMAT);
        formatter.setTimeZone(new SimpleTimeZone(0, "GMT"));
        java.util.Date dateValue = null;
        try {
            dateValue = formatter.parse(sTime);
        } catch (ParseException e) {
            throw new UnknownMessageException(e);
        }
        return dateValue;
    }

    /**
	 * Translates a Date object to a string in del.icio.us format
	 * @param time	Date object to parse
	 * @return	String representation of the provided time
	 * @throws UnknownMessageException thrown if the time cannot be parsed
	 */
    static String translateTime(java.util.Date time) {
        DateFormat formatter = new SimpleDateFormat(TIME_FORMAT);
        formatter.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return formatter.format(time);
    }

    /**
	 * Translates a date string in del.icio.us format to a Date object
	 * @param sDate	Date string to parse
	 * @return	Date object
	 * @throws UnknownMessageException thrown if the time cannot be parsed
	 */
    static java.util.Date translateDate(String sDate) throws UnknownMessageException {
        DateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        formatter.setTimeZone(new SimpleTimeZone(0, "GMT"));
        java.util.Date dateValue = null;
        try {
            dateValue = formatter.parse(sDate);
        } catch (ParseException e) {
            throw new UnknownMessageException(e);
        }
        return dateValue;
    }
}
