package flickr.service;

import flickr.response.Contact;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author leon
 */
public class FlickrUtils {

    private FlickrUtils() {
    }

    public static URL getGroupURLFromComment(String comment) {
        try {
            int idx = comment.indexOf("/groups/");
            if (idx == -1) {
                return null;
            }
            int start = idx + "/groups/".length();
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < comment.length(); i++) {
                char ch = comment.charAt(i);
                if (ch == '/' || ch == '\"' || ch == '\'') {
                    break;
                }
                sb.append(ch);
            }
            return new URL("http://www.flickr.com/groups/" + sb);
        } catch (IOException ex) {
            return null;
        }
    }

    public static boolean isGroupURL(URL url) {
        String str = url.toString();
        return str.contains("flickr.com/groups/");
    }

    public static String getGroupId(URL url) {
        String urlS = url.toString();
        int idx = urlS.lastIndexOf("/groups/");
        if (idx == -1) {
            return null;
        }
        String group = urlS.substring(idx + "/groups/".length());
        if (group.indexOf('@') == -1) {
            try {
                return getGroupByParsingHtml(url);
            } catch (IOException ex) {
                return null;
            }
        } else {
            idx = group.indexOf("/");
            if (idx == -1) {
                return group;
            }
            return group.substring(0, idx);
        }
    }

    private static String getGroupByParsingHtml(URL url) throws IOException {
        String html = readURL(url);
        int idx = html.indexOf("groups_members.gne?id=");
        if (idx == -1) {
            return null;
        }
        idx = idx + "groups_members.gne?id=".length();
        int maxIndex = html.length();
        StringBuilder idBuilder = new StringBuilder();
        while (idx < maxIndex && html.charAt(idx) != '&' && html.charAt(idx) != '"' && html.charAt(idx) != '\'') {
            idBuilder.append(html.charAt(idx));
            idx++;
        }
        return idBuilder.toString();
    }

    public static String readURL(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        InputStream is = null;
        try {
            conn.setRequestMethod("GET");
            is = conn.getInputStream();
            String contentEncoding = conn.getContentEncoding();
            if (contentEncoding == null) {
                contentEncoding = "UTF-8";
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is, contentEncoding));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
            return sb.toString();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            if (is != null) {
                is.close();
            }
        }
    }

    public static URL getGroupJoiningURL(String groupId) {
        try {
            return new URL("http://www.flickr.com/groups_join.gne?id=" + groupId);
        } catch (MalformedURLException ex) {
            return getFlickrURL();
        }
    }

    public static URL getUserPhotosURL(String userId) {
        try {
            return new URL("http://www.flickr.com/photos/" + userId);
        } catch (MalformedURLException ex) {
            return getFlickrURL();
        }
    }

    public static URL getUserFavoritesURL(String userId) {
        try {
            return new URL("http://www.flickr.com/" + userId + "/favorites");
        } catch (MalformedURLException ex) {
            return getFlickrURL();
        }
    }

    public static URL getUserContactsURL(String userId) {
        try {
            return new URL("http://www.flickr.com/people/" + userId + "/contacts");
        } catch (MalformedURLException ex) {
            return getFlickrURL();
        }
    }

    public static URL getUserProfileURL(String userId) {
        try {
            return new URL("http://www.flickr.com/people/" + userId);
        } catch (MalformedURLException ex) {
            return getFlickrURL();
        }
    }

    public static URL getUserMapURL(String userId) {
        try {
            return new URL("http://www.flickr.com/" + userId + "/map");
        } catch (MalformedURLException ex) {
            return getFlickrURL();
        }
    }

    public static String getCurrentUserId() {
        return Flickr.get().getAuth().getUser().getId();
    }

    public static URL getScoutURL(String userId) {
        try {
            if (userId != null) {
                return new URL("http://www.bighugelabs.com/flickr/scout.php?username=" + userId + "&combined=1");
            } else {
                return getBigHugeLabsURL();
            }
        } catch (MalformedURLException ex) {
            return getFlickrURL();
        }
    }

    public static URL getBigHugeLabsURL() {
        try {
            return new URL("http://www.bighugelabs.com/flickr/scout.php");
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Should not have happened.", ex);
        }
    }

    public static URL getFlickrURL() {
        try {
            return new URL("http://www.flickr.com");
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Should not have happened.", ex);
        }
    }

    public static URL getGroupDiscussionsURL(String groupId) {
        try {
            return new URL("http://www.flickr.com/groups/" + groupId + "/discuss");
        } catch (MalformedURLException ex) {
            return getFlickrURL();
        }
    }

    public static URL getPhotoURL(String userId, String photoId) {
        try {
            return new URL("http://www.flickr.com/photos/" + userId + "/" + photoId);
        } catch (MalformedURLException ex) {
            return getFlickrURL();
        }
    }

    public static URL getBuddyIconURL() {
        try {
            return new URL("http://www.flickr.com/images/buddyicon.jpg");
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Should not have happened", ex);
        }
    }

    public static URL getRecentActivityURL(String period) {
        try {
            return new URL("http://www.flickr.com/recent_activity.gne?days=" + period);
        } catch (MalformedURLException ex) {
            return getFlickrURL();
        }
    }

    public static URL getBuddyIconURL(Contact contact) {
        try {
            return new URL("http://farm" + contact.getIconFarm() + ".static.flickr.com/" + contact.getIconServer() + "/buddyicons/" + contact.getId() + ".jpg");
        } catch (MalformedURLException ex) {
            return getBuddyIconURL();
        }
    }
}
