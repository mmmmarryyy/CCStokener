package com.tony.fbqueries;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.example.possessed.PossessedApplication;
import com.vaadin.Application;

public class FBQueries {

    public static FBPhotoAlbum[] getAlbums(Application application) throws IOException {
        ArrayList<FBPhotoAlbum> rtn = new ArrayList<FBPhotoAlbum>();
        try {
            String queryparms = "access_token=" + URLEncoder.encode(((PossessedApplication) application).getAuthCode(), "UTF-8");
            URL url = new URL("https://graph.facebook.com/me/albums?" + queryparms);
            URLConnection conn = url.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            JSONParser p = new JSONParser();
            JSONObject ob = (JSONObject) p.parse(rd);
            if (ob.containsKey("error")) {
                rd.close();
                queryparms = "access_token=" + URLEncoder.encode(((PossessedApplication) application).getAuthCode(true), "UTF-8");
                url = new URL("https://graph.facebook.com/me/albums?" + queryparms);
                conn = url.openConnection();
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                p = new JSONParser();
                ob = (JSONObject) p.parse(rd);
            }
            JSONArray ob2 = (JSONArray) p.parse(ob.get("data").toString());
            for (Object album : ob2.toArray()) {
                JSONObject ob3 = (JSONObject) album;
                FBPhotoAlbum alb = new FBPhotoAlbum();
                alb.setName(ob3.get("name").toString());
                alb.setUrl(ob3.get("link").toString());
                alb.setId(ob3.get("id").toString());
                rtn.add(alb);
            }
            rd.close();
        } catch (IOException err) {
            throw err;
        } catch (Exception err) {
            err.printStackTrace();
        }
        return rtn.toArray(new FBPhotoAlbum[] {});
    }

    public static void getProfilePic(Application application) {
        try {
            String queryparms = "access_token=" + URLEncoder.encode(((PossessedApplication) application).getAuthCode(), "UTF-8");
            URL url = null;
            try {
                url = new URL("https://graph.facebook.com/me/picture?" + queryparms);
            } catch (RuntimeException err) {
                err.printStackTrace();
            }
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            Map hf = conn.getHeaderFields();
            Set keys = hf.keySet();
            for (Object o : keys) {
                System.out.println(o);
            }
            System.out.println(conn.getURL());
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public static HashMap getPhotos(Application application, FBPhotoAlbum album, String next) {
        ArrayList<FBPhoto> rtn = new ArrayList<FBPhoto>();
        JSONObject paging = null;
        try {
            String queryparms = "access_token=" + URLEncoder.encode(((PossessedApplication) application).getAuthCode(), "UTF-8");
            URL url = null;
            try {
                if (next != null) {
                    url = new URL(next);
                } else {
                    url = new URL("https://graph.facebook.com/" + album.getId() + "/photos?" + queryparms);
                }
            } catch (RuntimeException err) {
                err.printStackTrace();
            }
            URLConnection conn = url.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            JSONParser p = new JSONParser();
            JSONObject ob = (JSONObject) p.parse(rd);
            JSONArray ob2 = (JSONArray) p.parse(ob.get("data").toString());
            if (ob.get("paging") != null) {
                paging = (JSONObject) p.parse(ob.get("paging").toString());
            }
            for (Object photos : ob2.toArray()) {
                JSONObject ob3 = (JSONObject) photos;
                FBPhoto photo = new FBPhoto();
                photo.setSmallurl(ob3.get("picture").toString());
                photo.setBigurl(ob3.get("source").toString());
                photo.setUid(ob3.get("id").toString());
                rtn.add(photo);
            }
            rd.close();
        } catch (Exception err) {
            err.printStackTrace();
        }
        HashMap rtn1 = new HashMap();
        rtn1.put("fbphoto", rtn.toArray(new FBPhoto[] {}));
        if (paging != null) rtn1.put("next", paging.get("next"));
        return rtn1;
    }

    ;

    public static FBUserInfo getUserInfo(String token, String user) {
        FBUserInfo info = new FBUserInfo();
        try {
            String queryparms = "access_token=" + URLEncoder.encode(token, "UTF-8");
            URL url = new URL("https://graph.facebook.com/" + user + "?" + queryparms);
            URLConnection conn = url.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            JSONParser p = new JSONParser();
            JSONObject ob = (JSONObject) p.parse(rd);
            info.setName((String) ob.get("name"));
            info.setGender((String) ob.get("gender"));
            rd.close();
        } catch (Exception err) {
            err.printStackTrace();
        }
        return info;
    }

    public static String[] getGetFriendsList(PossessedApplication app) {
        ArrayList<String> rtnarr = new ArrayList<String>();
        try {
            String queryparms = "access_token=" + URLEncoder.encode(app.getAuthCode(), "UTF-8");
            URL url = new URL("https://graph.facebook.com/me/friends?" + queryparms);
            URLConnection conn = url.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            JSONParser p = new JSONParser();
            JSONObject ob = (JSONObject) p.parse(rd);
            JSONArray rtn = (JSONArray) ob.get("data");
            Iterator<JSONObject> elm = rtn.iterator();
            while (elm.hasNext()) {
                JSONObject jsonObject = (JSONObject) elm.next();
                rtnarr.add(jsonObject.get("id").toString());
            }
            rd.close();
        } catch (Exception err) {
            err.printStackTrace();
        }
        return rtnarr.toArray(new String[] {});
    }
}
