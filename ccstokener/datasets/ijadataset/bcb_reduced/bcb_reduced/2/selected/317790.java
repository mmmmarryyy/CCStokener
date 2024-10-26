package com.c2b2.ipoint.presentation.portlets;

import com.c2b2.ipoint.model.PersistentModelException;
import com.c2b2.ipoint.model.RSSCacheEntry;
import com.c2b2.ipoint.presentation.PortletRenderer;
import com.c2b2.ipoint.presentation.PresentationException;
import com.c2b2.ipoint.presentation.forms.fieldtypes.BooleanField;
import com.c2b2.ipoint.presentation.forms.fieldtypes.NumberField;
import com.c2b2.ipoint.presentation.forms.fieldtypes.StringField;
import com.c2b2.ipoint.model.Property;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndImage;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.SyndFeedOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import javax.servlet.ServletException;

public class RSSNews extends PortletRenderer {

    public RSSNews() {
    }

    public void initialiseNew() throws PresentationException {
        try {
            myPortlet.storeProperty(RSS_URL_PROPERTY, "");
            myPortlet.storeProperty(NUMBER_TO_SHOW_PROPERTY, "10");
            myPortlet.storeProperty(CACHE_TIME_PROPERTY, "60");
            myPortlet.storeProperty(SHOW_BODY_PROPERTY, "True");
        } catch (PersistentModelException e) {
            throw new PresentationException("Unable to set the initial property values", e);
        }
    }

    public void renderContent() throws PresentationException {
        try {
            myPR.includeJSP(myPortlet.getType().getInclude());
        } catch (IOException e) {
            throw new PresentationException("Unable to render the portlet", e);
        } catch (ServletException e) {
            throw new PresentationException("Unable to render the portlet", e);
        }
    }

    public void renderEdit() throws PresentationException {
        renderContent();
    }

    public void renderHelp() throws PresentationException {
    }

    public void renderMinimized() throws PresentationException {
    }

    public List getValidProperties() {
        List result = super.getValidProperties();
        result.add(new StringField(RSS_URL_PROPERTY, "RSS Feed URL", getDefinitiveProperty(RSS_URL_PROPERTY), false, 0));
        result.add(new NumberField(CACHE_TIME_PROPERTY, "Cache Time(s)", getDefinitiveProperty(CACHE_TIME_PROPERTY)));
        result.add(new NumberField(NUMBER_TO_SHOW_PROPERTY, "News Items to Show", getDefinitiveProperty(NUMBER_TO_SHOW_PROPERTY)));
        result.add(new BooleanField(SHOW_BODY_PROPERTY, "Show News Item Body", getDefinitiveProperty(SHOW_BODY_PROPERTY)));
        return result;
    }

    public void preProcess() throws PresentationException {
        super.preProcess();
        myPR.requireStyle("rssportlet.css");
        String nToShow = getDefinitiveProperty(NUMBER_TO_SHOW_PROPERTY);
        if (nToShow != null) {
            myNumberToShow = Integer.parseInt(nToShow);
        }
        String showBody = getDefinitiveProperty(SHOW_BODY_PROPERTY);
        if (showBody != null && showBody.toLowerCase().equals("false")) {
            myShowBody = false;
        }
        String cacheTime = getDefinitiveProperty(CACHE_TIME_PROPERTY);
        if (cacheTime != null) {
            myCacheTime = Integer.parseInt(cacheTime);
        }
        myURL = getDefinitiveProperty(RSS_URL_PROPERTY);
        if (myURL == null) {
            myURL = "";
        }
        try {
            RSSCacheEntry ce = RSSCacheEntry.find(myPortlet);
            if (ce != null) {
                Date saveDate = new Date(ce.getSaveTime());
                long now = System.currentTimeMillis();
                long maxSaveDate = now - myCacheTime * 1000;
                Date maxSave = new Date(maxSaveDate);
                if (ce.getUrl() == null || !ce.getUrl().equals(myURL) || saveDate.before(maxSave)) {
                    ce.delete();
                    ce = null;
                }
            }
            if (ce != null) {
                try {
                    SyndFeedInput input = new SyndFeedInput();
                    myFeed = input.build(new StringReader(ce.getXML()));
                    myLogger.fine("Using cached XML");
                } catch (FeedException e) {
                    myLogger.log(Level.INFO, "Unable to unmarshal from the cached xml", e);
                }
            } else {
                loadFromURL();
                cacheXML();
            }
        } catch (PersistentModelException e) {
            myLogger.log(Level.INFO, "Got persistent model exception", e);
            loadFromURL();
            cacheXML();
        }
    }

    public boolean isChannelImage() {
        boolean result = false;
        if (myFeed.getImage() != null && myFeed.getImage().getUrl() != null) {
            result = true;
        }
        return result;
    }

    public String getImageURL() {
        SyndImage myFeedImage = myFeed.getImage();
        if (myFeedImage != null) {
            return (myFeedImage.getUrl());
        } else {
            return ("No URL available");
        }
    }

    public String getImageTitle() {
        SyndImage myFeedImage = myFeed.getImage();
        if (myFeedImage != null) {
            return (myFeedImage.getTitle());
        } else {
            return ("No Title Available");
        }
    }

    public String getImageHref() {
        SyndImage myFeedImage = myFeed.getImage();
        if (myFeedImage != null) {
            return (myFeedImage.getLink());
        } else {
            return ("No Link Available");
        }
    }

    public String getChannelHref() {
        return myFeed.getLink();
    }

    public String getChannelDate() {
        String result = "";
        if (myFeed.getPublishedDate() != null) {
            result = myFeed.getPublishedDate().toString();
        }
        return result;
    }

    public String getChannelTitle() {
        String result = null;
        myFeed.getTitle();
        return result;
    }

    public List getItems() {
        List result = myFeed.getEntries();
        return result;
    }

    public int getNumberToShow() {
        return myNumberToShow;
    }

    public boolean getShowBody() {
        return myShowBody;
    }

    public SyndFeed getFeed() {
        return myFeed;
    }

    public boolean isValidFeed() {
        return myFeed != null;
    }

    public String getChannelImage() {
        return myFeed.getImage().getDescription();
    }

    private void loadFromURL() {
        if (myURL != null) {
            InputStream is = null;
            try {
                try {
                    Property HTTPProxyHost = Property.getProperty("HTTPProxyHost");
                    if (HTTPProxyHost != null && HTTPProxyHost.getValue() != null && !HTTPProxyHost.getValue().equals("")) {
                        java.util.Properties systemProperties = System.getProperties();
                        systemProperties.put("proxySet", "true");
                        systemProperties.put("proxyHost", HTTPProxyHost.getValue());
                        Property HTTPProxyPort = Property.getProperty("HTTPProxyPort");
                        if (HTTPProxyPort.getValue() != null && !HTTPProxyPort.getValue().equals("")) {
                            systemProperties.put("proxyPort", HTTPProxyPort.getValue());
                        }
                    }
                } catch (PersistentModelException e) {
                    myLogger.log(Level.WARNING, "Unable to find the properties to initialise the HTTP proxy", e);
                }
                myLogger.log(Level.INFO, "Retrieving url: " + myURL);
                URL url = new URL(myURL);
                java.net.URLConnection urlconn = url.openConnection();
                try {
                    Property HTTPProxyUser = Property.getProperty("HTTPProxyUser");
                    if (HTTPProxyUser != null && HTTPProxyUser.getValue() != null && !HTTPProxyUser.getValue().equals("")) {
                        String password = HTTPProxyUser.getValue();
                        Property HTTPProxyPassword = Property.getProperty("HTTPProxyPassword");
                        if (HTTPProxyPassword != null && HTTPProxyPassword.getValue() != null && !HTTPProxyPassword.getValue().equals("")) {
                            password = password + ":" + HTTPProxyPassword.getValue();
                        }
                        sun.misc.BASE64Encoder Base64 = new sun.misc.BASE64Encoder();
                        String encodedPassword = Base64.encode(password.getBytes());
                        urlconn.setRequestProperty("Proxy-Authorization", "Basic " + encodedPassword);
                    }
                } catch (PersistentModelException e) {
                    myLogger.log(Level.WARNING, "Unable to find the properties to initialise the HTTP proxy Authorization", e);
                }
                try {
                    Property HTTPUserAgent = Property.getProperty("HTTPUserAgent");
                    if (HTTPUserAgent != null && HTTPUserAgent.getValue() != null && !HTTPUserAgent.getValue().equals("")) {
                        urlconn.addRequestProperty("User-Agent", HTTPUserAgent.getValue());
                        myLogger.log(Level.INFO, "Setting User-Agent to: " + HTTPUserAgent.getValue());
                    }
                } catch (PersistentModelException e) {
                    myLogger.log(Level.WARNING, "Unable to find the properties to initialise the HTTPUserAgent", e);
                }
                is = urlconn.getInputStream();
                SyndFeedInput sfi = new SyndFeedInput();
                myFeed = sfi.build(new InputStreamReader(is));
            } catch (MalformedURLException e) {
                myLogger.log(Level.INFO, "The provided URL " + myURL + " was malformed ", e);
            } catch (FeedException e) {
                myLogger.log(Level.INFO, "Unable to unmarshal from the URL", e);
            } catch (IOException ioe) {
                myLogger.log(Level.WARNING, "Unable to open the URL", ioe);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    myLogger.log(Level.WARNING, "IOException when closing the Input Stream", e);
                }
            }
        }
    }

    private void cacheXML() {
        if (myFeed != null) {
            try {
                StringWriter sw = new StringWriter();
                SyndFeedOutput sfo = new SyndFeedOutput();
                try {
                    sfo.output(myFeed, sw);
                    RSSCacheEntry.create(sw.getBuffer().toString(), myPortlet, myURL);
                } catch (PersistentModelException e) {
                    myLogger.log(Level.INFO, "Unable to save xml to create a RSSCacheEntry", e);
                }
            } catch (IOException e) {
                myLogger.log(Level.INFO, "Unable to marshal xml to create a RSSCacheEntry", e);
            } catch (FeedException fe) {
                myLogger.log(Level.INFO, "Unable to marshal xml to create a RSSCacheEntry", fe);
            }
        }
    }

    public String getAllHREF() {
        return encodeRequestURL(NUMBER_TO_SHOW_PROPERTY, "10000");
    }

    private SyndFeed myFeed;

    private int myNumberToShow = 10;

    private int myCacheTime = 60;

    private boolean myShowBody = true;

    private String myURL;

    private static final String RSS_URL_PROPERTY = "RSSUrl";

    private static final String NUMBER_TO_SHOW_PROPERTY = "NumberToShow";

    private static final String CACHE_TIME_PROPERTY = "CacheTime";

    private static final String SHOW_BODY_PROPERTY = "ShowBody";
}
