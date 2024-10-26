package com.angis.fx.handler.login;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import android.content.Context;
import android.database.Cursor;
import com.angis.fx.activity.login.LoginActivity;
import com.angis.fx.data.ContextInfo;
import com.angis.fx.db.UserInfoDBHelper;
import com.angis.fx.util.DataParseUtil;
import com.angis.fx.util.LoginParseUtil;

public class OnlineLoginHandler {

    public static ContextInfo login(Context pContext, String pUsername, String pPwd, String pDeviceid) {
        HttpClient lClient = new DefaultHttpClient();
        StringBuilder lBuilder = new StringBuilder();
        ContextInfo lContextInfo = null;
        HttpPost lHttpPost = new HttpPost(new StringBuilder().append("http://").append(LoginActivity.mIpAddress.getText().toString()).append("/ZJWHServiceTest/GIS_Duty.asmx/PDALoginCheck").toString());
        List<NameValuePair> lNameValuePairs = new ArrayList<NameValuePair>(2);
        lNameValuePairs.add(new BasicNameValuePair("username", pUsername));
        lNameValuePairs.add(new BasicNameValuePair("password", pPwd));
        lNameValuePairs.add(new BasicNameValuePair("deviceid", pDeviceid));
        try {
            lHttpPost.setEntity(new UrlEncodedFormEntity(lNameValuePairs));
            HttpResponse lResponse = lClient.execute(lHttpPost);
            BufferedReader lHeader = new BufferedReader(new InputStreamReader(lResponse.getEntity().getContent()));
            for (String s = lHeader.readLine(); s != null; s = lHeader.readLine()) {
                lBuilder.append(s);
            }
            String lResult = lBuilder.toString();
            lResult = DataParseUtil.handleResponse(lResult);
            lContextInfo = LoginParseUtil.onlineParse(lResult);
            lContextInfo.setDeviceid(pDeviceid);
            if (0 == lContextInfo.getLoginFlag()) {
                lContextInfo.setLoginFlag(0);
            } else if (1 == lContextInfo.getLoginFlag()) {
                lContextInfo.setLoginFlag(1);
                updateUserInfo(pContext, lContextInfo);
            } else if (2 == lContextInfo.getLoginFlag()) {
                lContextInfo.setLoginFlag(2);
            } else if (3 == lContextInfo.getLoginFlag()) {
                lContextInfo.setLoginFlag(3);
            }
        } catch (Exception e) {
            return lContextInfo;
        }
        return lContextInfo;
    }

    public static void updateUserInfo(Context pContext, ContextInfo pContextInfo) {
        UserInfoDBHelper lUserInfoDBHelper = new UserInfoDBHelper(pContext);
        Cursor lCursor = lUserInfoDBHelper.getSingleUserInfo(LoginActivity.g_username, LoginActivity.g_password);
        if (lCursor.getCount() > 0) {
            lUserInfoDBHelper.updateUserInfo(LoginActivity.g_username, "", LoginActivity.g_password, pContextInfo.getOrganizationInfo().getOid(), "", pContextInfo.getMemberInfos(), pContextInfo.getDeviceno());
        } else {
            lUserInfoDBHelper.insertUserInfo(LoginActivity.g_username, "", LoginActivity.g_password, pContextInfo.getOrganizationInfo().getOid(), "", pContextInfo.getMemberInfos(), pContextInfo.getDeviceno());
        }
        lCursor.close();
        lUserInfoDBHelper.closeDB();
    }
}
