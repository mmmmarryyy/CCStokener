package leeon.kaixin.wap.action;

import java.util.ArrayList;
import java.util.List;
import leeon.kaixin.wap.models.Repaste;
import leeon.kaixin.wap.util.HttpUtil;
import leeon.mobile.BBSBrowser.ContentException;
import leeon.mobile.BBSBrowser.NetworkException;
import leeon.mobile.BBSBrowser.actions.HttpConfig;
import leeon.mobile.BBSBrowser.utils.HTMLUtil;
import leeon.mobile.BBSBrowser.utils.HTMLUtil.BaseContentHandler;
import leeon.mobile.BBSBrowser.utils.HTTPUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class RepasteAction {

    public static List<Repaste> listRepaste(String verify, String uid) throws NetworkException {
        return listRepaste(verify, uid, 0);
    }

    public static List<Repaste> listRepaste(String verify, String uid, int start) throws NetworkException {
        HttpClient client = HttpConfig.newInstance();
        String url = HttpUtil.KAIXIN_REPASTES_URL + HttpUtil.KAIXIN_PARAM_UID + uid;
        url += "&" + HttpUtil.KAIXIN_PARAM_VERIFY + verify;
        url += "&" + HttpUtil.KAIXIN_PRRAM_PPAGE + start;
        HttpGet get = new HttpGet(url);
        HttpUtil.setHeader(get);
        try {
            HttpResponse response = client.execute(get);
            String html = HTTPUtil.toString(response.getEntity());
            return parseRepaste(html);
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkException(e);
        }
    }

    public static void detailRepaste(String verify, Repaste r) throws NetworkException {
        detailRepaste(verify, r, 0);
    }

    public static void detailRepaste(String verify, Repaste r, int start) throws NetworkException {
        HttpClient client = HttpConfig.newInstance();
        String url = HttpUtil.KAIXIN_REPASTE_DETAIL_URL + HttpUtil.KAIXIN_PARAM_UID + r.getUid();
        url += "&" + HttpUtil.KAIXIN_PARAM_URPID + r.getUrpid();
        url += "&" + HttpUtil.KAIXIN_PARAM_VERIFY + verify;
        url += "&" + HttpUtil.KAIXIN_PRRAM_PSTART + start;
        HttpGet get = new HttpGet(url);
        HttpUtil.setHeader(get);
        try {
            HttpResponse response = client.execute(get);
            String html = HTTPUtil.toString(response.getEntity());
            if (Repaste.repasteContent(html, r)) {
                detailRepaste(verify, r, start + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkException(e);
        }
    }

    public static String parseHtmlContent(String content, final ImageHandler handler) {
        final StringBuffer html = new StringBuffer();
        HTMLUtil.fromHtml(content, new BaseContentHandler() {

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                html.append(ch, start, length);
            }

            @Override
            public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
                if (name.equalsIgnoreCase("br")) {
                    html.append("\n");
                } else if (name.equalsIgnoreCase("img")) {
                    String url = atts.getValue("src");
                    if (url == null) return;
                    if (url.startsWith("/")) url = HttpUtil.KAIXIN_URL + url;
                    if (handler != null) url = handler.dealImage(url + "&width=1024");
                    html.append("\n").append(url).append("\n");
                }
            }
        });
        return html.toString();
    }

    public static interface ImageHandler {

        public String dealImage(String url);
    }

    private static List<Repaste> parseRepaste(String html) {
        List<Repaste> ret = new ArrayList<Repaste>();
        if (html == null || html.length() == 0) return ret;
        String[] divs = HTMLUtil.dealEnclosingTags(html, "div", "class=\"recm\"", true);
        for (String div : divs) {
            Repaste r = Repaste.newInstance(div);
            ret.add(r);
        }
        return ret;
    }

    /**
	 * @param args
	 * @throws NetworkException 
	 * @throws ContentException 
	 */
    public static void main(String[] args) throws NetworkException {
        List<Repaste> ret = listRepaste("2538938_2538938_1300779905_3029b0a25002d5e2bae7625f22f326cb_kx", "1152298");
        detailRepaste("2538938_2538938_1300779905_3029b0a25002d5e2bae7625f22f326cb_kx", ret.get(3));
        System.out.println(ret);
    }
}
