package leeon.kaixin.wap.action;

import java.util.ArrayList;
import java.util.List;
import leeon.kaixin.wap.models.Status;
import leeon.kaixin.wap.util.HttpUtil;
import leeon.mobile.BBSBrowser.ContentException;
import leeon.mobile.BBSBrowser.NetworkException;
import leeon.mobile.BBSBrowser.actions.HttpConfig;
import leeon.mobile.BBSBrowser.utils.HTMLUtil;
import leeon.mobile.BBSBrowser.utils.HTTPUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

public class StatusAction {

    public static List<Status> listStatus(String verify) throws NetworkException {
        return listStatus(verify, 0);
    }

    public static List<Status> listStatus(String verify, int start) throws NetworkException {
        HttpClient client = HttpConfig.newInstance();
        String url = HttpUtil.KAIXIN_STATUS_URL + HttpUtil.KAIXIN_PARAM_UID + LoginAction.uid(verify);
        url += "&" + HttpUtil.KAIXIN_PARAM_VERIFY + verify;
        url += "&" + HttpUtil.KAIXIN_PRRAM_PSTART + start;
        HttpGet get = new HttpGet(url);
        HttpUtil.setHeader(get);
        try {
            HttpResponse response = client.execute(get);
            String html = HTTPUtil.toString(response.getEntity());
            return parseStatus(html);
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkException(e);
        }
    }

    private static List<Status> parseStatus(String html) {
        List<Status> ret = new ArrayList<Status>();
        if (html == null || html.length() == 0) return ret;
        String[] divs = HTMLUtil.dealEnclosingTags(html, "div", "class=\"qmt\"", false);
        for (String div : divs) {
            Status s = Status.newInstance(div);
            ret.add(s);
        }
        return ret;
    }

    /**
	 * @param args
	 * @throws NetworkException 
	 * @throws ContentException 
	 */
    public static void main(String[] args) throws NetworkException {
        List<Status> ret = listStatus("6865223_6865223_1300775380_0dcb8b9046822af91b103b5d1b118d25_kx");
        System.out.println(ret);
    }
}
