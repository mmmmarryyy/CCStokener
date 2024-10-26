package de.nomule.mediaproviders;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import de.nomule.applogic.MediaProvider;
import de.nomule.applogic.NoMuleRuntime;
import de.nomule.applogic.SearchResult;
import de.nomule.applogic.Settings;
import de.nomule.common.HTTP;

public class YoutubeMediaProvider extends MediaProvider {

    public static boolean TRY_HIGH_QUALITY = true;

    public String getMediaURL(String strLink) {
        try {
            String res = de.nomule.mediaproviders.KeepVid.getAnswer(strLink, "aa");
            if (NoMuleRuntime.DEBUG) System.out.println(res);
            String regexp = "http:\\/\\/[^\"]+\\/get_video[^\"]+";
            Pattern p = Pattern.compile(regexp);
            Matcher m = p.matcher(res);
            m.find();
            String strRetUrl = res.substring(m.start(), m.end());
            strRetUrl = URLDecoder.decode(strRetUrl, "UTF-8");
            if (TRY_HIGH_QUALITY) {
                NoMuleRuntime.showDebug("HIGH_QUALITY");
                strRetUrl += "&fmt=18";
                try {
                    URL url = new URL(strRetUrl);
                    URLConnection conn = url.openConnection();
                    InputStream in = conn.getInputStream();
                    in.close();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    strRetUrl = strRetUrl.substring(0, strRetUrl.length() - 7);
                }
            }
            if (NoMuleRuntime.DEBUG) System.out.println(strRetUrl);
            return strRetUrl;
        } catch (UnsupportedEncodingException e) {
            System.out.println("Error in Youtube Media Provider. Encoding is not supported. (How would that happen?!)");
            e.printStackTrace();
        }
        return "";
    }

    public String getName() {
        return "Youtube";
    }

    public boolean MatchURLPattern(String strLink) {
        return strLink.matches("^http\\:\\/\\/[\\w\\.]*youtube\\.com\\/watch\\?v\\=.*$");
    }

    public boolean providesSearch() {
        return true;
    }

    public LinkedList<SearchResult> search(String strRequest) {
        LinkedList<SearchResult> ret = new LinkedList<SearchResult>();
        try {
            String strUrl = "http://gdata.youtube.com/feeds/api/videos?vq=" + URLEncoder.encode(strRequest, "UTF-8") + "&max-results=10&orderby=viewCount&alt=atom";
            String res = HTTP.get(strUrl);
            String strRegExpTitles = "\\<media\\:title type\\=\'plain\'\\>[^\\<]*\\<";
            String strRegExpUrls = "\\<media\\:player url\\=\'[^\']+\'/>";
            Pattern pTitles = Pattern.compile(strRegExpTitles);
            Matcher mTitles = pTitles.matcher(res);
            Pattern pUrls = Pattern.compile(strRegExpUrls);
            Matcher mUrls = pUrls.matcher(res);
            LinkedList<String> lTitles = new LinkedList<String>();
            LinkedList<String> lUrls = new LinkedList<String>();
            while (mTitles.find()) {
                lTitles.add(res.substring(mTitles.start() + "<media:title type=\"plain\">".length(), mTitles.end() - "<".length()));
            }
            while (mUrls.find()) {
                lUrls.add(res.substring(mUrls.start() + "<media:player url=\"".length(), mUrls.end() - "\"/>".length()));
            }
            if (lTitles.size() == lUrls.size()) {
                for (int i = 0; i < lTitles.size(); i++) {
                    ret.add(new SearchResult(lTitles.get(i) + " at Youtube!", lUrls.get(i)));
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public String[] getProvidedContent() {
        String[] s = { "Music", "General Content" };
        return s;
    }

    @Override
    public String getTestUrl() {
        return "http://www.youtube.com/watch?v=2BESbnMJg9M";
    }
}
