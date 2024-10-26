package com.angis.fx.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import com.angis.fx.activity.Enforcement;
import com.angis.fx.data.ChangsuoInformation;
import com.angis.fx.data.PictureInfo;

public class UploadUtil {

    public static String uploadPic(PictureInfo info) throws ClientProtocolException, IOException {
        HttpClient client = new DefaultHttpClient();
        HttpPost httpPos = new HttpPost(new StringBuilder().append("http://").append(Enforcement.HOST).append("/ZJWHServiceTest/GIS_WHCS.asmx/UploadPictureBase64").toString());
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("pictype", info.getPictype()));
        nameValuePairs.add(new BasicNameValuePair("recordid", info.getRecordid()));
        nameValuePairs.add(new BasicNameValuePair("picrecordid", info.getPicrecordid()));
        nameValuePairs.add(new BasicNameValuePair("filename", info.getFilename()));
        nameValuePairs.add(new BasicNameValuePair("fileExtension", info.getFileExtension()));
        String body = new String(com.angis.fx.util.Base64.encode(info.getFilebody()));
        nameValuePairs.add(new BasicNameValuePair("imgBase64string", body));
        httpPos.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        System.out.println(body);
        HttpResponse httpResponse = client.execute(httpPos);
        body = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
        StringBuffer buffer = new StringBuffer();
        for (String str = reader.readLine(); str != null; str = reader.readLine()) {
            buffer.append(str);
        }
        System.out.println(buffer.toString());
        return buffer.toString();
    }

    public static String uploadCsCoord(ChangsuoInformation info) throws ClientProtocolException, IOException {
        HttpClient client = new DefaultHttpClient();
        HttpPost httpPos = new HttpPost(new StringBuilder().append("http://").append(Enforcement.HOST).append("/ZJWHServiceTest/GIS_WHCS/RecieveWHCSXY").toString());
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        StringBuilder lBuilder = new StringBuilder();
        lBuilder.append(info.getCsId()).append("&").append(info.getXlonGPS()).append("&").append(info.getYlatGPS()).append("#");
        nameValuePairs.add(new BasicNameValuePair("coorInfo", lBuilder.toString()));
        httpPos.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        HttpResponse httpResponse = client.execute(httpPos);
        BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
        StringBuffer buffer = new StringBuffer();
        for (String str = reader.readLine(); str != null; str = reader.readLine()) {
            buffer.append(str);
        }
        return buffer.toString();
    }
}
