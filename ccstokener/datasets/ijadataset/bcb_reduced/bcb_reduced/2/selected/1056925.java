package com.showdown.util;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.showdown.api.IShowInfo;
import com.showdown.api.impl.ShowInfo;

/**
 * Utility class for getting show summary information which parser TV.com for 
 * Show information. Note that use of this is disabled by default in ShowDown and 
 * should only be enabled with appropriate permission.
 * @author Mat DeLong
 */
public final class ShowInfoUtil {

    public static void main(String[] args) {
        System.out.println(getShowSummaryHTML(getShowInfo("1718")));
    }

    /**
    * Returns an {@link IShowInfo} instance for the show with the given tv.com ID
    * @param showID the id for the show on TV.com
    * @return the {@link IShowInfo} instance
    */
    public static IShowInfo getShowInfo(String showID) {
        final String showIdString = showID;
        String pageText = FileUtil.getURLText(getShowPageURL(showIdString));
        String showDescription = null;
        URL image = null;
        List<String> categories = new ArrayList<String>();
        double score = -1;
        if (pageText != null) {
            String imagePath = getShowImageURL(showIdString);
            String categoriesString = getCategories(pageText);
            String showScoreString = getShowScore(pageText);
            showDescription = getShowDescription(showID, pageText);
            String[] tokens = categoriesString.split(",");
            for (String token : tokens) {
                try {
                    categories.add(token.substring(token.indexOf('>') + 1, token.indexOf("</a>")));
                } catch (Exception ex) {
                }
            }
            try {
                image = new URL(imagePath);
                if (!exists(image)) {
                    image = null;
                }
            } catch (MalformedURLException e) {
            }
            try {
                score = Double.parseDouble(showScoreString);
            } catch (Exception ex) {
            }
        }
        IShowInfo info = null;
        if (showDescription != null) {
            info = new ShowInfo(showDescription, image, score);
            info.getCategories().addAll(categories);
        }
        return info;
    }

    /**
    * Returns true if the URL points to an existing file through an HTTP connection
    * @param url The URL to check
    * @return true if it points to a valid file through an HTTP connection
    */
    public static boolean exists(URL url) {
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("HEAD");
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            return false;
        }
    }

    /**
    * Returns the HTML show summary for the show with the given ShowInfo
    * @param info the {@link IShowInfo} for the show
    * @return the HTML or an empty string
    */
    public static String getShowSummaryHTML(IShowInfo info) {
        StringBuffer sb = new StringBuffer();
        if (info != null) {
            if (info.getImageURL() != null) {
                sb.append("<img border=\"1\" width=\"100%\" src=\"");
                sb.append(info.getImageURL().toExternalForm());
                sb.append("\"/><br />\n");
            }
            for (int i = 0; i < info.getCategories().size(); ++i) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(info.getCategories().get(i));
            }
            sb.append("<br />\n");
            if (info.getShowScore() >= 0) {
                sb.append("Rating: ");
                sb.append(Double.toString(info.getShowScore()));
                sb.append("<br /><br />\n");
            }
            if (info.getDescription().length() > 0) {
                sb.append(info.getDescription());
            } else {
                sb.append("<i>No show description available.</i>");
            }
        }
        return sb.toString();
    }

    private static String getCategories(String pageText) {
        String result = "";
        String prefix = "<h4>Genres</h4>";
        String suffix = "</div>";
        int pos = pageText.indexOf(prefix);
        if (pos > -1) {
            int suffixPos = pageText.indexOf(suffix, pos);
            if (suffixPos > pos) {
                result = pageText.substring(pos, suffixPos);
                result = result.replace(prefix, " ").trim().replaceAll("\\s+", "");
            }
        }
        return result;
    }

    private static String getShowScore(String pageText) {
        String score = "";
        String scorePrefix = "<h3>Show Score</h3>";
        String scorePrefix2 = "<span class=\"number\">";
        int pos = pageText.indexOf(scorePrefix);
        if (pos > -1) {
            pos = pageText.indexOf(scorePrefix2, pos);
        }
        if (pos > -1) {
            pos += scorePrefix2.length();
            int suffixPos = pageText.indexOf("</span>", pos);
            if (suffixPos > pos) {
                score = pageText.substring(pos, suffixPos).trim();
            }
        }
        return score;
    }

    private static String getShowDescription(String showID, String pageText) {
        String regex = "<p class=\"show_description MORE_LESS\">(.+?)<span class=\"truncater\">&hellip; <a class=\"show_more POINTER\" href=\"#\">More</a></span><span>(.+?)</span></p>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(pageText);
        String summary = "";
        if (matcher.find()) {
            String firstPart = matcher.group(1).trim();
            String secondPart = matcher.group(2).trim();
            summary = firstPart + " " + secondPart;
        }
        summary += "<br /><br />";
        summary += "<a href=\"" + getShowPageURL(showID) + "\">View on TV.com</a>";
        return summary;
    }

    private static String getShowImageURL(String showID) {
        return "http://image.com.com/tv/images/content_headers/program_new/" + showID + ".jpg";
    }

    public static String getShowPageURL(String showID) {
        return "http://www.tv.com/uselessnode/show/" + showID + "/summary.html";
    }
}
