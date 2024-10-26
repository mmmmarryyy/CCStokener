package com.trinea.sns.serviceImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.http.HttpParameters;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import com.trinea.sns.entity.CommentInfo;
import com.trinea.sns.entity.StatusInfo;
import com.trinea.sns.entity.UserInfo;
import com.trinea.sns.service.SnsService;
import com.trinea.sns.util.SnsConstant;
import com.trinea.sns.utilImpl.StringUtils;

public abstract class SohuServiceImpl extends SnsService {

    @Override
    public int updateStatus(UserInfo userInfo, String status) throws Exception {
        OAuthConsumer consumer = SnsConstant.getOAuthConsumer(SnsConstant.SOHU);
        consumer.setTokenWithSecret(userInfo.getAccessToken(), userInfo.getAccessSecret());
        try {
            URL url = new URL(SnsConstant.SOHU_UPDATE_STATUS_URL);
            HttpURLConnection request = (HttpURLConnection) url.openConnection();
            request.setDoOutput(true);
            request.setRequestMethod("POST");
            HttpParameters para = new HttpParameters();
            para.put("status", StringUtils.utf8Encode(status).replaceAll("\\+", "%20"));
            consumer.setAdditionalParameters(para);
            consumer.sign(request);
            OutputStream ot = request.getOutputStream();
            ot.write(("status=" + URLEncoder.encode(status, "utf-8")).replaceAll("\\+", "%20").getBytes());
            ot.flush();
            ot.close();
            System.out.println("Sending request...");
            request.connect();
            System.out.println("Response: " + request.getResponseCode() + " " + request.getResponseMessage());
            BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
            String b = null;
            while ((b = reader.readLine()) != null) {
                System.out.println(b);
            }
            return SnsConstant.SOHU_UPDATE_STATUS_SUCC_WHAT;
        } catch (Exception e) {
            SnsConstant.SOHU_OPERATOR_FAIL_REASON = processException(e.getMessage());
            return SnsConstant.SOHU_UPDATE_STATUS_FAIL_WHAT;
        }
    }

    @Override
    public int updateStatus(UserInfo userInfo, String status, String picturePath) {
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(SnsConstant.SOHU_UPDATE_STATUS_URL);
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("parameter1", "parameterValue1"));
        parameters.add(new BasicNameValuePair("parameter2", "parameterValue2"));
        try {
            post.setEntity(new UrlEncodedFormEntity(parameters));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            HttpResponse response = client.execute(post);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public String processException(String exceptionMessage) {
        if (!StringUtils.isEmpty(exceptionMessage)) {
            if (exceptionMessage.contains("Received authentication challenge is null")) {
                return "�Ѻ�΢������ʧ��Ŷ^_^";
            }
        }
        return "";
    }

    @Override
    public List<StatusInfo> getStatusesOfAll(UserInfo userInfo, int page, long lastPageTime) {
        return null;
    }

    @Override
    public int repost(UserInfo userInfo, long statusId, String comments) {
        return 0;
    }

    @Override
    public StatusInfo getStatus(UserInfo userInfo, long statusId) {
        return null;
    }

    @Override
    public List<CommentInfo> getStatusComments(UserInfo userInfo, long statusId, int page) {
        return null;
    }

    @Override
    public List<StatusInfo> getCommentsToMe(UserInfo userInfo, int page, long lastPageTime) {
        return null;
    }

    @Override
    public int repost(UserInfo userInfo, long statusId, String comment, boolean isComment) {
        return 0;
    }

    @Override
    public int commentStatus(UserInfo userInfo, long statusId, String comment) {
        return 0;
    }

    @Override
    public int commentComment(UserInfo userInfo, long statusId, long commentId, String comment) {
        return 0;
    }

    @Override
    public List<StatusInfo> getCommentsByMe(UserInfo userInfo, int page, long lastPageTime, long lastId) {
        return null;
    }

    @Override
    public List<StatusInfo> getCommentsAboutMe(UserInfo userInfo, int page, long lastPageTime, long lastId) {
        return null;
    }
}
