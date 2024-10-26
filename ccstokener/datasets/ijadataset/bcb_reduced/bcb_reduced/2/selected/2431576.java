package com.android.wheretogo.threads;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.Context;
import android.util.Log;
import com.android.wheretogo.adapter.DbAdapter;
import com.android.wheretogo.constant.Constant;
import com.android.wheretogo.util.JsonContent;
import com.w2g.objects.PictureData;

public class DownloadThread extends Thread {

    String uri;

    String longitude;

    String latitude;

    String picDirectory;

    Context context = null;

    ArrayList<Integer> picIds = new ArrayList<Integer>();

    ArrayList<Integer> notPicIds = new ArrayList<Integer>();

    public DownloadThread(String uri, String longitude, String latitude, String picDirectory, Context context) {
        this.uri = uri;
        this.longitude = longitude;
        this.latitude = latitude;
        this.picDirectory = picDirectory;
        this.context = context;
    }

    public void run() {
        try {
            String url = Constant.urlIMP + "/getPictureByLatitudeAndLongitude.action";
            String json = JsonContent.getContent(new HttpGet(url + "?latitude=" + latitude + "&longitude=" + longitude));
            JSONArray jsonArray = new JSONArray(json);
            Map<String, Map<String, String>> tuples = new HashMap<String, Map<String, String>>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Map<String, String> map = new HashMap<String, String>();
                map.put("tags", obj.getString("tags"));
                map.put("path", obj.getString("path"));
                map.put("latitude", obj.getString("latitude"));
                map.put("longitude", obj.getString("longitude"));
                map.put("source", obj.getString("source"));
                map.put("name", obj.getString("name"));
                map.put("ownerId", obj.getString("ownerId"));
                map.put("uploadDate", obj.getString("uploadDate"));
                map.put("ownerName", obj.getString("ownerName"));
                tuples.put(obj.getString("id"), map);
            }
            List<String> notPicIds = new ArrayList<String>();
            DbAdapter adapter = new DbAdapter(context);
            adapter.open();
            List<String> picIds = new ArrayList<String>(tuples.keySet());
            for (String picId : picIds) {
                int pic = -1;
                try {
                    String picName = picDirectory + picId;
                    pic = adapter.getPictureIdByPicName(picName);
                } catch (Exception e) {
                    Log.d("bad", e.getMessage());
                }
                if (pic == -1) {
                    notPicIds.add(picId);
                }
                Map<String, String> tuple = tuples.get(picId);
                Integer userId = Integer.parseInt(tuple.get("ownerId"));
                if (!adapter.isUserIdExist(userId)) {
                    String userName = tuple.get("ownerName");
                    adapter.insertUser(userId, userName);
                }
            }
            for (String picId : notPicIds) {
                String urlPath = Constant.urlIMP + "/downloadPictureToAndroid.action?picId=" + picId;
                getPicture(urlPath, picId);
                try {
                    Map<String, String> tuple = tuples.get(picId);
                    adapter.insertBasicPicInfo(picDirectory + picId, Integer.parseInt(tuple.get("ownerId")), tuple.get("longitude"), tuple.get("latitude"), tuple.get("uploadDate"));
                    int androidPicId = adapter.getPictureIdByPicName(picDirectory + picId);
                    String strTags = tuple.get("tags");
                    if (strTags != null && !strTags.equals(" ") && !strTags.equals("")) {
                        String[] tags = strTags.split(" ");
                        for (String tag : tags) {
                            int tagId = -1;
                            if (adapter.isTagExist(tag)) {
                                tagId = adapter.getTagIdByTag(tag);
                            } else {
                                tagId = adapter.insertTag(tag);
                            }
                            adapter.insertPicTagById(androidPicId, tagId);
                        }
                    }
                } catch (Exception e) {
                    Log.d("bad", e.getMessage());
                }
            }
            adapter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writePictureDataIntoDatabase(PictureData pictureData) {
        ArrayList<String> basicPicInfo = pictureData.getBasicPicInfo();
        ArrayList<String> tags = pictureData.getTags();
        byte[] picture = pictureData.getPicture();
        String strPicId = basicPicInfo.get(0);
        String name = basicPicInfo.get(1);
        name = picDirectory + strPicId + ".jpg";
        String ownerId = basicPicInfo.get(2);
        String longitude = basicPicInfo.get(3);
        String latitude = basicPicInfo.get(4);
        String created = basicPicInfo.get(5);
        File file = new File(picDirectory + strPicId + ".jpg");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(picture);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            DbAdapter adapter = new DbAdapter(context);
            adapter.open();
            if (tags != null) {
                for (String tag : tags) {
                    int tagId = -1;
                    if (adapter.isTagExist(tag)) {
                        tagId = adapter.getTagIdByTag(tag);
                    } else {
                        tagId = adapter.insertTag(tag);
                    }
                    adapter.insertPicTagById(Integer.parseInt(strPicId), tagId);
                }
            }
            adapter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getPicture(String urlPath, String picId) throws Exception {
        URL url = new URL(urlPath);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        if (conn.getResponseCode() == 200) {
            InputStream inputStream = conn.getInputStream();
            byte[] data = readStream(inputStream);
            File file = new File(picDirectory + picId);
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(data);
            outputStream.close();
        }
        conn.disconnect();
    }

    private byte[] readStream(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }
        outputStream.close();
        inputStream.close();
        return outputStream.toByteArray();
    }
}
