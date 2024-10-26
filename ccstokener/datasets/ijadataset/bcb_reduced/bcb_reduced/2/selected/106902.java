package com.angis.fx.handler.login;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import android.content.ContentValues;
import android.content.Context;
import com.angis.fx.activity.Enforcement;
import com.angis.fx.activity.R;
import com.angis.fx.activity.login.LoginActivity;
import com.angis.fx.activity.sxry.SxryActivity;
import com.angis.fx.activity.sxry.SxryMActivity;
import com.angis.fx.data.CKTypeInfo;
import com.angis.fx.data.ContextInfo;
import com.angis.fx.data.DraftInfo;
import com.angis.fx.data.DutyInfo;
import com.angis.fx.data.GISZFJGInfo;
import com.angis.fx.db.BusinessTypeDBHelper;
import com.angis.fx.db.EcnomicTypeDBHelper;
import com.angis.fx.db.GISZFJGDBHelper;
import com.angis.fx.db.WGTypeDBHelper;
import com.angis.fx.db.ZCTypeDBHelper;
import com.angis.fx.util.ContentValuesUtil;
import com.angis.fx.util.DataParseUtil;
import com.angis.fx.util.SystemUpdateParseUtil;

public class UpdateHandler {

    private static HttpClient mHttpClient = new DefaultHttpClient();

    private static StringBuilder mBuilder = new StringBuilder();

    private static HttpPost mHttpPost;

    /**
	 * 1：更新成功, 0：数据更新错误， -1：服务器连接错误
	 * @param pContext
	 * @return
	 */
    public static int ckTypeUpdate(Context pContext) {
        mHttpClient = new DefaultHttpClient();
        mBuilder = new StringBuilder();
        mHttpPost = new HttpPost(new StringBuilder().append("http://").append(Enforcement.HOST).append("/ZJWHServiceTest/GIS_CheckDaily.asmx/DownLoadJCJGInfo").toString());
        try {
            HttpResponse lHttpResponse = mHttpClient.execute(mHttpPost);
            BufferedReader lReader = new BufferedReader(new InputStreamReader(lHttpResponse.getEntity().getContent()));
            for (String s = lReader.readLine(); s != null; s = lReader.readLine()) {
                mBuilder.append(s);
            }
            String lResponse = DataParseUtil.handleResponse(mBuilder.toString());
            Map<String, List<CKTypeInfo>> lCKTypeInfoMap = SystemUpdateParseUtil.CKResultParse(lResponse);
            if (lCKTypeInfoMap.get("WGTYPE").size() > 0 || lCKTypeInfoMap.get("ZCTYPE").size() > 0) {
                WGTypeDBHelper lWGTypeDBHelper = new WGTypeDBHelper(pContext);
                lWGTypeDBHelper.delAll();
                List<CKTypeInfo> lWGTypeInfoList = lCKTypeInfoMap.get("WGTYPE");
                ContentValues lValues = null;
                for (CKTypeInfo wgTypeInfo : lWGTypeInfoList) {
                    lValues = ContentValuesUtil.convertWGType(wgTypeInfo);
                    lWGTypeDBHelper.insertWGType(lValues);
                }
                lWGTypeDBHelper.closeDB();
                ZCTypeDBHelper lZCTypeDBHelper = new ZCTypeDBHelper(pContext);
                lZCTypeDBHelper.delAll();
                List<CKTypeInfo> lZCTypeInfoList = lCKTypeInfoMap.get("ZCTYPE");
                for (CKTypeInfo zcTypeInfo : lZCTypeInfoList) {
                    lValues = ContentValuesUtil.convertZCType(zcTypeInfo);
                    lZCTypeDBHelper.insertWGType(lValues);
                }
                lZCTypeDBHelper.closeDB();
            } else {
                return 0;
            }
        } catch (Exception e) {
            return -1;
        }
        return 1;
    }

    /**
	 * 1：更新成功, 0：数据更新错误， -1：服务器连接错误
	 * @param pContext
	 * @return
	 */
    public static int ecnomicTypeUpdate(Context pContext) {
        mHttpClient = new DefaultHttpClient();
        mBuilder = new StringBuilder();
        mHttpPost = new HttpPost(new StringBuilder().append("http://").append(Enforcement.HOST).append("/ZJWHServiceTest/GIS_WHCS.asmx/DownLoadEconomyType").toString());
        try {
            HttpResponse lHttpResponse = mHttpClient.execute(mHttpPost);
            BufferedReader lReader = new BufferedReader(new InputStreamReader(lHttpResponse.getEntity().getContent()));
            for (String s = lReader.readLine(); s != null; s = lReader.readLine()) {
                mBuilder.append(s);
            }
            String lResponse = DataParseUtil.handleResponse(mBuilder.toString());
            List<String> lEcnomicTypeList = SystemUpdateParseUtil.ecnomicTypeParse(lResponse);
            if (null != lEcnomicTypeList) {
                if (lEcnomicTypeList.size() > 0) {
                    EcnomicTypeDBHelper lDBHelper = new EcnomicTypeDBHelper(pContext);
                    lDBHelper.deleteAll();
                    for (String str : lEcnomicTypeList) {
                        ContentValues lValues = ContentValuesUtil.convertEcnomicType(str);
                        lDBHelper.insert(lValues);
                    }
                    lDBHelper.closeDB();
                }
            } else {
                return 0;
            }
        } catch (Exception e) {
            return -1;
        }
        return 1;
    }

    /**
	 * 1：更新成功, 0：数据更新错误， -1：服务器连接错误
	 * @param pContext
	 * @return
	 */
    public static int businessTypeUpdate(Context pContext) {
        mHttpClient = new DefaultHttpClient();
        mBuilder = new StringBuilder();
        mHttpPost = new HttpPost(new StringBuilder().append("http://").append(Enforcement.HOST).append("/ZJWHServiceTest/GIS_WHCS.asmx/DownLoadManageType").toString());
        try {
            HttpResponse lHttpReponse = mHttpClient.execute(mHttpPost);
            BufferedReader lReader = new BufferedReader(new InputStreamReader(lHttpReponse.getEntity().getContent()));
            for (String s = lReader.readLine(); null != s; s = lReader.readLine()) {
                mBuilder.append(s);
            }
            String lResponse = DataParseUtil.handleResponse(mBuilder.toString());
            List<String> businessTypeList = SystemUpdateParseUtil.businessTypeParse(lResponse);
            if (null != businessTypeList) {
                if (businessTypeList.size() > 0) {
                    BusinessTypeDBHelper lDBHelper = new BusinessTypeDBHelper(pContext);
                    lDBHelper.deleteAll();
                    for (String str : businessTypeList) {
                        ContentValues lValues = ContentValuesUtil.convertBusinessType(str);
                        lDBHelper.insert(lValues);
                    }
                    lDBHelper.closeDB();
                }
            } else {
                return 0;
            }
        } catch (Exception e) {
            return -1;
        }
        return 1;
    }

    /**
	 * 1：更新成功, 0：数据更新错误， -1：服务器连接错误
	 * @param pContext
	 * @return
	 */
    public static int gisZFJGUpdate(Context pContext) {
        mHttpClient = new DefaultHttpClient();
        mBuilder = new StringBuilder();
        mHttpPost = new HttpPost(new StringBuilder().append("http://").append(Enforcement.HOST).append("/ZJWHServiceTest/GIS_Duty.asmx/DownLoadZFJG").toString());
        try {
            HttpResponse lHttpReponse = mHttpClient.execute(mHttpPost);
            BufferedReader lReader = new BufferedReader(new InputStreamReader(lHttpReponse.getEntity().getContent()));
            for (String s = lReader.readLine(); null != s; s = lReader.readLine()) {
                mBuilder.append(s);
            }
            String lResponse = DataParseUtil.handleResponse(mBuilder.toString());
            List<GISZFJGInfo> gisZFJGList = SystemUpdateParseUtil.gisZFJGParse(lResponse);
            if (null != gisZFJGList) {
                if (gisZFJGList.size() > 0) {
                    GISZFJGDBHelper lDBHelper = new GISZFJGDBHelper(pContext);
                    lDBHelper.deleteAll();
                    for (GISZFJGInfo gisZFJGInfo : gisZFJGList) {
                        ContentValues lValues = ContentValuesUtil.convertGISZFJG(gisZFJGInfo);
                        lDBHelper.insert(lValues);
                    }
                    lDBHelper.closeDB();
                }
            } else {
                return 0;
            }
        } catch (Exception e) {
            return -1;
        }
        return 1;
    }

    public static int checkdutyUpdate(DraftInfo pDraftInfo) {
        mHttpClient = new DefaultHttpClient();
        mBuilder = new StringBuilder();
        mHttpPost = new HttpPost(new StringBuilder().append("http://").append(Enforcement.HOST).append("/ZJWHServiceTest/GIS_CheckDaily.asmx/PDARecieveCheckInfo").toString());
        try {
            String[] jcqks = pDraftInfo.getJcqk().split(";");
            StringBuilder lQtBuilder = new StringBuilder();
            StringBuilder lWgBuilder = new StringBuilder();
            for (String jcqk : jcqks) {
                if (jcqk.contains("#")) {
                    lWgBuilder.append("").append(jcqk).append("&");
                } else {
                    lQtBuilder.append("").append(jcqk).append(";");
                }
            }
            List<NameValuePair> lNameValuePairs = new ArrayList<NameValuePair>(2);
            StringBuffer lSb = new StringBuffer();
            lSb.append("id=").append(pDraftInfo.getId()).append("^");
            lSb.append("dutyno=").append(pDraftInfo.getDutyno()).append("^");
            lSb.append("whcsno=").append(pDraftInfo.getCsid()).append("^");
            lSb.append("reachtime=").append(pDraftInfo.getArrivaltime()).append("^");
            lSb.append("leavetime=").append(pDraftInfo.getLeavetime()).append("^");
            lSb.append("dailycheckno=").append(pDraftInfo.getWhcsdutyid()).append("^");
            lSb.append("jcqk=").append(lQtBuilder.toString()).append("^");
            lSb.append("tjGuid=").append(pDraftInfo.getTjguid()).append("^");
            lSb.append("area=").append(pDraftInfo.getArea()).append("^");
            lSb.append("cdrc=").append(pDraftInfo.getCdrc()).append("^");
            lSb.append("xs=").append(pDraftInfo.getCheckxs()).append("^");
            lSb.append("account=").append(LoginActivity.g_username).append("^");
            lSb.append("together=").append(pDraftInfo.getTogether()).append("^");
            lSb.append("whcsname=").append(pDraftInfo.getContitle()).append("^");
            lSb.append("areatype=").append(pDraftInfo.getAreatype()).append("^");
            lSb.append("address=").append(pDraftInfo.getAddress()).append("^");
            lSb.append("jcjg=").append(pDraftInfo.getJcjg()).append("^");
            lSb.append("jcsj=").append(pDraftInfo.getChecktime()).append("^");
            lSb.append("permitcode=").append(pDraftInfo.getPermitcode()).append("^");
            lSb.append("permitword=").append(pDraftInfo.getPermitword()).append("^");
            lSb.append("manager=").append(pDraftInfo.getManager()).append("^");
            lSb.append("cslb=").append(pDraftInfo.getCslb()).append("^");
            lSb.append("jcjgDetial=").append(lWgBuilder.toString()).append("~");
            lNameValuePairs.add(new BasicNameValuePair("checkinfo", lSb.toString()));
            mHttpPost.setEntity(new UrlEncodedFormEntity(lNameValuePairs));
            HttpResponse lHttpReponse = mHttpClient.execute(mHttpPost);
            BufferedReader lReader = new BufferedReader(new InputStreamReader(lHttpReponse.getEntity().getContent()));
            for (String s = lReader.readLine(); null != s; s = lReader.readLine()) {
                mBuilder.append(s);
            }
            String lResponse = DataParseUtil.handleResponse(mBuilder.toString());
            if (!lResponse.equals("1")) {
                return 0;
            }
        } catch (Exception e) {
            return -1;
        }
        return 1;
    }

    public static int newChangsuo(Context pContext, String pCsInfo) {
        mHttpClient = new DefaultHttpClient();
        mBuilder = new StringBuilder();
        mHttpPost = new HttpPost(new StringBuilder().append("http://").append(Enforcement.HOST).append("/ZJWHServiceTest/GIS_WHCS.asmx/AddNewWHCS").toString());
        try {
            List<NameValuePair> lNameValuePairs = new ArrayList<NameValuePair>(2);
            lNameValuePairs.add(new BasicNameValuePair("csInfo", pCsInfo));
            mHttpPost.setEntity(new UrlEncodedFormEntity(lNameValuePairs));
            HttpResponse lHttpReponse = mHttpClient.execute(mHttpPost);
            BufferedReader lReader = new BufferedReader(new InputStreamReader(lHttpReponse.getEntity().getContent()));
            for (String s = lReader.readLine(); null != s; s = lReader.readLine()) {
                mBuilder.append(s);
            }
            String lResponse = DataParseUtil.handleResponse(mBuilder.toString());
            if (!lResponse.equals("1")) {
                return 0;
            }
        } catch (Exception e) {
            return -1;
        }
        return 1;
    }

    public static int uploadDutyInfo(Context pContext, DutyInfo pDutyInfo) {
        mHttpClient = new DefaultHttpClient();
        mBuilder = new StringBuilder();
        mHttpPost = new HttpPost(new StringBuilder().append("http://").append(Enforcement.HOST).append("/ZJWHServiceTest/GIS_Duty.asmx/DutyStationInfo").toString());
        try {
            List<NameValuePair> lNameValuePairs = new ArrayList<NameValuePair>(2);
            StringBuilder lBuilder = new StringBuilder();
            lBuilder.append(pDutyInfo.getDutyno()).append("~").append(pDutyInfo.getDusername()).append("~").append(SxryMActivity.SXRY_USER_DEPARTMENT).append("~").append(SxryActivity.QTRY_EDIT_MEMBER).append("~").append(pDutyInfo.getStartdutytime()).append("~").append(pDutyInfo.getEnddutytime()).append("~").append(Enforcement.mDeviceId).append("~").append(LoginActivity.mLoginId).append("^");
            lNameValuePairs.add(new BasicNameValuePair("stationinfo", lBuilder.toString()));
            mHttpPost.setEntity(new UrlEncodedFormEntity(lNameValuePairs));
            HttpResponse lHttpReponse = mHttpClient.execute(mHttpPost);
            BufferedReader lReader = new BufferedReader(new InputStreamReader(lHttpReponse.getEntity().getContent()));
            for (String s = lReader.readLine(); null != s; s = lReader.readLine()) {
                mBuilder.append(s);
            }
            String lResponse = DataParseUtil.handleResponse(mBuilder.toString());
            if (!lResponse.equals("1")) {
                return 0;
            }
        } catch (Exception e) {
            return -1;
        }
        return 1;
    }

    public static int uploadLoginInfo(Context pContext, ContextInfo contextInfo) {
        mHttpClient = new DefaultHttpClient();
        mBuilder = new StringBuilder();
        mHttpPost = new HttpPost(new StringBuilder().append("http://").append(Enforcement.HOST).append("/ZJWHServiceTest/GIS_Duty.asmx/PDALoginRecord").toString());
        try {
            List<NameValuePair> lNameValuePairs = new ArrayList<NameValuePair>(2);
            StringBuilder lBuilder = new StringBuilder();
            lBuilder.append(LoginActivity.mLoginId).append("&").append(LoginActivity.g_username).append("&&&").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(contextInfo.getLoginTime())).append("&").append(contextInfo.getDeviceno()).append("&&").append(pContext.getString(R.string.appversion)).append("#");
            lNameValuePairs.add(new BasicNameValuePair("loginInfos", lBuilder.toString()));
            mHttpPost.setEntity(new UrlEncodedFormEntity(lNameValuePairs));
            HttpResponse lHttpReponse = mHttpClient.execute(mHttpPost);
            BufferedReader lReader = new BufferedReader(new InputStreamReader(lHttpReponse.getEntity().getContent()));
            for (String s = lReader.readLine(); null != s; s = lReader.readLine()) {
                mBuilder.append(s);
            }
            String lResponse = DataParseUtil.handleResponse(mBuilder.toString());
            if (!lResponse.equals("1")) {
                return 0;
            }
        } catch (Exception e) {
            return -1;
        }
        return 1;
    }
}
