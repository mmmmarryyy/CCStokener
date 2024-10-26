package org.vrspace.util;

import java.net.*;
import java.util.*;
import java.io.*;

/**
This class represents a network resource - a collection of URLs pointing to same file.
*/
public class NetResource implements Serializable {

    public long db_id;

    public Vector urls = new Vector();

    public NetResource() {
    }

    /**
  This constructor creates NetResource with specified urls.
  @param urlSpec url list delimited by space
  */
    public NetResource(String urlSpec) {
        StringTokenizer st = new StringTokenizer(urlSpec, " ");
        while (st.hasMoreTokens()) {
            urls.addElement(st.nextToken());
        }
    }

    /**
  Returns url list delimited by space
  */
    public String toString() {
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < urls.size(); i++) {
            ret.append(urls.elementAt(i).toString());
            ret.append(' ');
        }
        return ret.toString().trim();
    }

    /**
  Returns urls as string array
  */
    public String[] toArray() {
        String[] ret = new String[urls.size()];
        for (int i = 0; i < urls.size(); i++) {
            ret[i] = urls.elementAt(i).toString();
        }
        return ret;
    }

    /**
  Returns string in format [ url1,url2,...urlN ]
  */
    public String toSpecString() {
        StringBuffer ret = new StringBuffer("[ ");
        for (int i = 0; i < urls.size() - 1; i++) {
            ret.append(urls.elementAt(i).toString());
            ret.append(',');
        }
        ret.append(urls.elementAt(urls.size() - 1));
        ret.append(" ]");
        return ret.toString();
    }

    /**
  Returns prefered URL of this resource. Checks whether URL is available by opening a connection.
  @return URL or null if there is no available URLs for this resource
  */
    public URL getURL() {
        URL ret = null;
        for (int i = 0; i < urls.size(); i++) {
            try {
                ret = new URL((String) urls.elementAt(i));
                InputStream stream = ret.openStream();
                stream.close();
                break;
            } catch (MalformedURLException urlE) {
                Logger.logWarning("Malformed url: " + urls.elementAt(i));
            } catch (IOException ioE) {
                Logger.logDebug("URL " + ret + " unavailable");
            }
        }
        return ret;
    }

    /**
  Adds url to the list. Checks whether URL is accessible.
  @throws MalformedURLException if unsupported protocol is specified
  @throws IOException if URL content is unacessible
  */
    public void addURL(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        for (int i = 0; i < urls.size(); i++) {
            if (((URL) urls.elementAt(i)).equals(url)) {
                Logger.logWarning("Attempt to add an URL twice: " + url);
                return;
            }
        }
        InputStream stream = url.openStream();
        stream.close();
        urls.addElement(urlSpec);
        Logger.logDebug("Added " + url);
    }

    /**
  Updates this resource info. Opens connections to each url in list and checks for availability.
  Ensures that all urls are available.
  */
    public void update() {
        Vector invalids = new Vector();
        for (int i = 0; i < urls.size(); i++) {
            URL url = null;
            try {
                url = new URL((String) urls.elementAt(i));
                InputStream stream = url.openStream();
                stream.close();
            } catch (MalformedURLException urlE) {
                Logger.logWarning("Malformed URL: " + urls.elementAt(i));
            } catch (IOException ioE) {
                invalids.addElement(url);
            }
        }
        for (int i = 0; i < invalids.size(); i++) {
            urls.removeElement(invalids.elementAt(i));
            Logger.logInfo("Removed " + invalids.elementAt(i) + " - no longer available");
        }
    }

    public static void main(String[] args) throws Exception {
        new Logger();
        NetResource test = new NetResource();
        test.addURL("http://www.vrspace.org/");
    }

    public static String getHibernateMapping() {
        StringBuffer ret = new StringBuffer();
        ret.append("<?xml version=\"1.0\"?><!DOCTYPE hibernate-mapping PUBLIC \"-//Hibernate/Hibernate Mapping DTD 3.0//EN\" \"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\n");
        ret.append("<hibernate-mapping package=\"org.vrspace.util\">\n");
        ret.append("  <class name=\"NetResource\" table=\"vrs_NetResource\">\n");
        ret.append("    <id name=\"db_id\" column=\"vrs_db_id\" access=\"field\">\n");
        ret.append("      <generator class=\"native\"/>\n");
        ret.append("    </id>\n");
        ret.append("    <property name=\"urls\" column=\"vrs_urls\" access=\"field\"/>\n");
        ret.append("  </class>\n");
        ret.append("</hibernate-mapping>\n");
        return ret.toString();
    }
}
