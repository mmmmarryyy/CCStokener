package leeon.kaixin.wap.action;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import leeon.kaixin.wap.models.Picture;
import leeon.kaixin.wap.models.Status;
import leeon.kaixin.wap.models.StatusPic;
import leeon.kaixin.wap.util.HttpUtil;
import leeon.mobile.BBSBrowser.ContentException;
import leeon.mobile.BBSBrowser.NetworkException;
import leeon.mobile.BBSBrowser.actions.HttpConfig;
import leeon.mobile.BBSBrowser.utils.HTMLUtil;
import leeon.mobile.BBSBrowser.utils.HTTPUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

public class PictureAction {

    public static List<Picture> listPicture(String verify, StatusPic s, String viewType) throws NetworkException {
        Picture p = detailPicture(verify, s, viewType);
        return listPictureInternal(p);
    }

    public static List<Picture> listPicture(Picture p) throws NetworkException {
        if (p.getNo() >= p.getCount()) return new ArrayList<Picture>();
        p = detailPictureNext(p);
        return listPictureInternal(p);
    }

    private static List<Picture> listPictureInternal(Picture p) throws NetworkException {
        List<Picture> ret = new ArrayList<Picture>();
        ret.add(p);
        while (p.getNo() < p.getCount() && ret.size() < 5) {
            p = detailPictureNext(p);
            ret.add(p);
        }
        return ret;
    }

    public static Picture detailPicture(String verify, StatusPic s, String viewType) throws NetworkException {
        String url = HttpUtil.KAIXIN_PIC_URL + HttpUtil.KAIXIN_PARAM_VERIFY + verify;
        url += "&" + HttpUtil.KAIXIN_PARAM_UID + s.getUid();
        url += "&" + HttpUtil.KAIXIN_PARAM_PUID + s.getPuid();
        url += "&" + HttpUtil.KAIXIN_PARAM_PID + s.getPid();
        url += "&" + HttpUtil.KAIXIN_PRRAM_VIEWTYPE + viewType;
        Picture r = detailPicture(url);
        if (s.getUrl() == null) s.setUrl(r.getUrl());
        return r;
    }

    public static Picture detailPictureNext(Picture p) throws NetworkException {
        String url = HttpUtil.KAIXIN_URL + p.getNurl();
        return detailPicture(url);
    }

    public static Picture detailPicturePre(Picture p) throws NetworkException {
        String url = HttpUtil.KAIXIN_URL + p.getPurl();
        return detailPicture(url);
    }

    public static Picture detailPicture(String url) throws NetworkException {
        HttpClient client = HttpConfig.newInstance();
        HttpGet get = new HttpGet(url);
        HttpUtil.setHeader(get);
        try {
            HttpResponse response = client.execute(get);
            String html = HTTPUtil.toString(response.getEntity());
            return parsePicture(html);
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkException(e);
        }
    }

    public static Map<String, String> preUploadPicture(String verify) throws NetworkException {
        String url = HttpUtil.KAIXIN_PIC_UP_URL + HttpUtil.KAIXIN_PARAM_VERIFY + verify;
        HttpClient client = HttpConfig.newInstance();
        HttpGet get = new HttpGet(url);
        HttpUtil.setHeader(get);
        try {
            HttpResponse response = client.execute(get);
            String html = HTTPUtil.toString(response.getEntity());
            return dealSelectOptions(html, "name=\"albumid\"");
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkException(e);
        }
    }

    public static void uploadPicture(String verify, String albumid, String title, File file) throws ContentException, NetworkException {
        HttpClient client = HttpConfig.newInstance();
        HttpPost post = new HttpPost(HttpUtil.KAIXIN_PIC_UPLOAD_URL + HttpUtil.KAIXIN_PARAM_VERIFY + verify);
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.STRICT);
        try {
            reqEntity.addPart("albumid", new StringBody(albumid, Charset.forName("UTF-8")));
            reqEntity.addPart("title", new StringBody(title, Charset.forName("UTF-8")));
            reqEntity.addPart("file", new FileBody(file, "image/jpeg"));
            post.setEntity(reqEntity);
            HttpResponse response = client.execute(post);
            HTTPUtil.consume(response.getEntity());
            if (!HTTPUtil.isHttp200(response)) throw new ContentException("upload picture failed");
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            throw new NetworkException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new NetworkException(e);
        }
    }

    private static Picture parsePicture(String html) {
        if (html == null || html.length() == 0) return null;
        String div = HTMLUtil.findStr(html, "<div class=\"picbg\">", "</div>");
        Picture p = Picture.newInstance(div);
        return p;
    }

    /**
	 * 根据html语法抽取select的option值
	 * @param source
	 * @param name select control's name
	 * @return
	 */
    public static Map<String, String> dealSelectOptions(String source, String tag) {
        if (source == null || source.length() == 0) return null;
        if (tag == null || tag.length() == 0) return null;
        source = HTMLUtil.findStrBeforeTag(source, tag, "<select", "</select>");
        if (source == null || source.length() == 0) return null;
        String[] r = HTMLUtil.findStrs(source, "<option ", "</option>");
        HashMap<String, String> ret = new HashMap<String, String>();
        for (String s : r) {
            String v = HTMLUtil.findStr(s, "value=\"", "\"");
            String k = HTMLUtil.findStr(s, ">");
            if (k != null && k.length() != 0) ret.put(k, v);
        }
        return ret;
    }

    /**
	 * @param args
	 * @throws NetworkException 
	 * @throws ContentException 
	 */
    public static void main(String[] args) throws NetworkException {
        List<Status> ret = StatusAction.listStatus("6865223_6865223_1301049274_6ccf08f0d5d6d34812c4f0f4c8c6e5ac_kx");
        List<Picture> l = listPicture("6865223_6865223_1301049274_6ccf08f0d5d6d34812c4f0f4c8c6e5ac_kx", (StatusPic) ret.get(1), "0");
        System.out.println(l);
    }
}
