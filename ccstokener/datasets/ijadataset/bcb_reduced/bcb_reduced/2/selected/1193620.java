package com.android.lifestyle;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import android.util.Log;

/**
 * reference
 * http://txton.net/hoehoe/2008/080716031353.html
 * http://www.fireproject.jp/feature/xml/programing/java-dom.html
 * @author iizuka
 *
 */
public class YahooFuriganaParser {

    private static final String YAHOO_URL = "http://jlp.yahooapis.jp/MAService/V1/parse";

    private static final String APP_ID = "PnCgp62xg67pFfuS6WLKNxe9pqPxsu3eN1TTlvOdvz0uA1Gi.oBAT.0OrU3ZtA--";

    private static Document doc;

    private static StringBuffer sbFurigana;

    private static final Map<String, String> m;

    static {
        m = new HashMap<String, String>();
        m.put("ょ", "yo");
        m.put("あ", "ah");
        m.put("い", "e");
        m.put("う", "wool");
        m.put("え", "eay");
        m.put("お", "oh");
        m.put("か", "car");
        m.put("き", "key");
        m.put("く", "ku");
        m.put("け", "k");
        m.put("こ", "koh");
        m.put("さ", "sar");
        m.put("し", "c");
        m.put("す", "sue");
        m.put("せ", "say");
        m.put("そ", "so");
        m.put("た", "tar");
        m.put("ち", "tick");
        m.put("つ", "two");
        m.put("て", "tea");
        m.put("と", "toe");
        m.put("な", "na");
        m.put("に", "need");
        m.put("ぬ", "nue");
        m.put("ね", "ney");
        m.put("の", "no");
        m.put("は", "ha");
        m.put("ひ", "he");
        m.put("ふ", "foo");
        m.put("へ", "hey");
        m.put("ほ", "ho");
        m.put("ま", "ma");
        m.put("み", "me");
        m.put("む", "muh");
        m.put("め", "may");
        m.put("も", "mo");
        m.put("や", "yah");
        m.put("ゆ", "you");
        m.put("よ", "yo");
        m.put("ら", "lar");
        m.put("り", "lee");
        m.put("る", "lu");
        m.put("れ", "ray");
        m.put("ろ", "low");
        m.put("わ", "were");
        m.put("ゐ", "e");
        m.put("ゑ", "eay");
        m.put("を", "war");
        m.put("ん", "unn");
        m.put("が", "ga");
        m.put("ぎ", "gee");
        m.put("ぐ", "goo");
        m.put("げ", "gay");
        m.put("ご", "go");
        m.put("ざ", "za");
        m.put("じ", "zee");
        m.put("ず", "zu");
        m.put("ぜ", "zey");
        m.put("ぞ", "zo");
        m.put("だ", "da");
        m.put("ぢ", "zi");
        m.put("づ", "zu");
        m.put("で", "dead");
        m.put("ど", "doh");
        m.put("ば", "ba");
        m.put("び", "be");
        m.put("ぶ", "boo");
        m.put("べ", "bay");
        m.put("ぼ", "bo");
        m.put("ぱ", "pa");
        m.put("ぴ", "pee");
        m.put("ぷ", "pooh");
        m.put("ぺ", "pay");
        m.put("ぽ", "po");
    }

    /**
     * Get Japanese to Roman String
     * @param sentence
     * @return Roman string
     * @throws Exception
     */
    public static String getRoman(String sentence) throws Exception {
        String furiganaStr = getFurigana(sentence);
        StringBuffer dst = new StringBuffer();
        for (int i = 0; i < furiganaStr.length(); i++) {
            char c = furiganaStr.charAt(i);
            if (c < 128) {
                dst.append(c);
            } else {
                String d = m.get("" + c);
                if (d != null) {
                    dst.append(d);
                }
                dst.append(" ");
            }
        }
        return dst.toString();
    }

    /**
     * Get Japanese Frigana
     * @param sentence
     * @return FuriganaStr
     * @throws Exception
     */
    public static String getFurigana(String sentence) throws Exception {
        Log.d("--VOA--", "getFurigana START");
        sbFurigana = new StringBuffer();
        String urlStr = getYahooApiURL();
        urlStr = addSentence(urlStr, sentence);
        URL url = new URL(urlStr);
        URLConnection uc = url.openConnection();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Log.d("--VOA--", uc.getURL().toString());
        InputStream is = uc.getInputStream();
        doc = db.parse(is);
        walkThrough();
        Log.d("--VOA--", "getFurigana END");
        return sbFurigana.toString();
    }

    private static void walkThrough() {
        Node root = doc.getDocumentElement();
        recursiveWalk(root);
    }

    private static void recursiveWalk(Node node) {
        if (node.getNodeType() == Node.TEXT_NODE && node.getNodeValue().trim().length() == 0) {
            return;
        }
        getNodeInfo(node);
        Node child = node.getFirstChild();
        while (child != null) {
            recursiveWalk(child);
            try {
                child = child.getNextSibling();
            } catch (Throwable e) {
                break;
            }
        }
    }

    private static void getNodeInfo(Node node) {
        if (node.getNodeName().equalsIgnoreCase("reading")) {
            Node c = node.getFirstChild();
            sbFurigana.append(c.getNodeValue());
        }
    }

    private static String getYahooApiURL() {
        StringBuffer sb = new StringBuffer();
        sb.append(YAHOO_URL);
        sb.append("?");
        sb.append("appid=" + APP_ID);
        sb.append("&");
        sb.append("results=ma");
        return sb.toString();
    }

    private static String addSentence(String url, String sentence) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(url);
        sb.append("&");
        sb.append("sentence=" + URLEncoder.encode(sentence, "UTF-8"));
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        YahooFuriganaParser yfp = new YahooFuriganaParser();
        System.out.println(yfp.getFurigana("布団が吹っ飛んだ"));
    }
}
