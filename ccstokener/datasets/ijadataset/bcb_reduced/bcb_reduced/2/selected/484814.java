package com.newbie.iSee;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import android.util.Log;

public class WebInterface {

    private DefaultHttpClient httpClient = null;

    private static final int REGISTRATION_TIMEOUT = 30 * 1000;

    WebInterface() {
        maybeCreateHttpClient();
    }

    public String FTPupload(String filepath) {
        String fileUrl = null;
        Long clicks = System.currentTimeMillis();
        String currentDateTimeString = clicks.toString();
        String[] tmpSplite = filepath.split("/");
        String filename = currentDateTimeString + tmpSplite[tmpSplite.length - 1];
        String host = "140.112.31.165:8080/sound/";
        Log.d("test", "get in");
        FTPClient ftp = new FTPClient();
        Log.d("test", "initial ftp");
        try {
            ftp.connect("140.112.31.165");
            ftp.enterLocalPassiveMode();
            Log.d("test", "we connected");
            if (!ftp.login("tacowu", "4565686")) {
                ftp.logout();
                return fileUrl;
            }
            int replyCode = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                Log.d("test", "get in trouble");
                ftp.disconnect();
                return fileUrl;
            }
            Log.d("test", "we logged in");
            ftp.setFileType(ftp.BINARY_FILE_TYPE);
            ftp.enterLocalPassiveMode();
            File file = new File(filepath);
            if (file == null) Log.d("test", "file open faild"); else Log.d("test", "file open sucess");
            FileInputStream aInputStream = new FileInputStream(file);
            boolean aRtn = ftp.storeFile(filename, aInputStream);
            aInputStream.close();
            ftp.disconnect();
        } catch (Exception ex) {
        }
        fileUrl = host + filename;
        return fileUrl;
    }

    private void maybeCreateHttpClient() {
        if (httpClient == null) {
            httpClient = new DefaultHttpClient();
            HttpParams params = httpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(params, REGISTRATION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, REGISTRATION_TIMEOUT);
            ConnManagerParams.setTimeout(params, REGISTRATION_TIMEOUT);
        }
    }

    public String process(OP request) {
        String errMsg = null;
        String response = null;
        try {
            response = sendToServer(request.toJSON().toString());
            Log.d("test", "response: " + response);
            request.result(response);
        } catch (IOException ex) {
            errMsg = "Connection problem";
            Log.e("test", "IOException", ex);
        } catch (JSONException ex) {
            errMsg = "Malformed response";
            Log.e("test", "Malformed JSON response: " + response, ex);
        }
        return errMsg;
    }

    private String sendToServer(String request) throws IOException {
        Log.d("test", "request body " + request);
        String result = null;
        maybeCreateHttpClient();
        HttpPost post = new HttpPost(Config.APP_BASE_URI);
        post.addHeader("Content-Type", "text/vnd.aexp.json.req");
        post.setEntity(new StringEntity(request));
        HttpResponse resp = httpClient.execute(post);
        int status = resp.getStatusLine().getStatusCode();
        if (status != HttpStatus.SC_OK) throw new IOException("HTTP status: " + Integer.toString(status));
        DataInputStream is = new DataInputStream(resp.getEntity().getContent());
        result = is.readLine();
        return result;
    }

    public void uploadPhoto(photo pho) {
        uploadPhoto UP = new uploadPhoto(pho);
        String errMSG = process(UP);
    }

    public void uploadAlbum(String aid, photo[] phoList) {
        uploadAlbum UPA = new uploadAlbum(aid, phoList);
        String errMSG = process(UPA);
    }

    public void addComment(String uid, String uname, String pid, String msg, String Powner, String PownerName, String cid) {
        postComment postC = new postComment(uid, uname, pid, msg, Powner, PownerName, cid);
        String errMSG = process(postC);
    }

    public photo[] lookAround(String uid, String uname, String[] fidList, int latitude, int longitude, int scope) {
        lookAround Look = new lookAround(uid, uname, fidList, latitude, longitude, scope);
        String errMSG = process(Look);
        return Look.photoList;
    }

    public event[] recentEvent(String uid, String uname, String[] fidList, int beginDay, int untilDay) {
        recentEvent recent = new recentEvent(uid, uname, fidList, beginDay, untilDay);
        String errMSG = process(recent);
        eventComp ec = new eventComp();
        Arrays.sort(recent.eventList, ec);
        return recent.eventList;
    }

    class eventComp implements Comparator<event> {

        public int compare(event eventA, event eventB) {
            return eventB.date.compareTo(eventA.date);
        }
    }

    public void cleanGQLDB() {
        cleanDB CDB = new cleanDB();
        String errMSG = process(CDB);
    }
}

class Config {

    public static final String APP_BASE_URI = "http://iseebackend.appspot.com/";
}

class OP {

    public String uname;

    public String uid;

    public String op;

    public OP(String uid, String uname) {
        this.uid = uid;
        this.uname = uname;
    }

    public void result(String response) {
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        return obj;
    }
}

class cleanDB extends OP {

    cleanDB() {
        super("clean", "clean");
    }

    public void result(String response) {
        Log.d("test", "response: " + response);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("op", "cleanDB");
        ;
        return obj;
    }
}

class photo {

    public String photoUrl;

    public int latitude;

    public int longitude;

    public String uid;

    public String uname;

    public String pid;

    photo(String photoUrl, int latitude, int longitude, String uid, String uname, String pid) {
        this.photoUrl = photoUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.uid = uid;
        this.uname = uname;
        this.pid = pid;
    }
}

class uploadPhoto extends OP {

    public photo pho;

    uploadPhoto(photo pho) {
        super(pho.uid, pho.uname);
        this.pho = pho;
    }

    public void result(String response) {
        Log.d("test", "response: " + response);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("op", "upload_photo");
        obj.put("uid", pho.uid);
        obj.put("uname", pho.uname);
        obj.put("pid", pho.pid);
        obj.put("photoUrl", pho.photoUrl);
        obj.put("latitude", pho.latitude);
        obj.put("longitude", pho.longitude);
        return obj;
    }
}

class uploadAlbum extends OP {

    public String aid;

    public photo[] photoList;

    uploadAlbum(String aid, photo[] photoList) {
        super(photoList[0].uid, photoList[0].uname);
        this.photoList = photoList;
        this.aid = aid;
    }

    public void result(String response) {
        Log.d("test", "response: " + response);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("op", "upload_album");
        obj.put("uid", uid);
        obj.put("uname", uname);
        obj.put("aid", aid);
        JSONArray JpidList = new JSONArray();
        for (int i = 0; i < photoList.length; i++) {
            JSONObject o = new JSONObject();
            o.put("photoUrl", photoList[i].photoUrl);
            o.put("latitude", photoList[i].latitude);
            o.put("longitude", photoList[i].longitude);
            o.put("uid", photoList[i].uid);
            o.put("uname", photoList[i].uname);
            o.put("pid", photoList[i].pid);
            JpidList.put(o);
        }
        obj.put("pidlist", JpidList);
        return obj;
    }
}

class postComment extends OP {

    public String pid;

    public String PownerName;

    public String msg;

    public String Powner;

    public String cid;

    postComment(String uid, String uname, String pid, String msg, String Powner, String PownerName, String cid) {
        super(uid, uname);
        this.pid = pid;
        this.msg = msg;
        this.Powner = Powner;
        this.PownerName = PownerName;
        this.cid = cid;
    }

    public void result(String response) {
        Log.d("test", "response: " + response);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("op", "post_comment");
        obj.put("uid", uid);
        obj.put("uname", uname);
        obj.put("pid", pid);
        obj.put("msg", msg);
        obj.put("Powner", Powner);
        obj.put("PownerName", PownerName);
        obj.put("cid", cid);
        return obj;
    }
}

class event {

    public String event_type;

    public String uid;

    public String uname;

    public String tid;

    public String date;

    public String Powner;

    public String Pownername;

    public String msg;

    public String cid;
}

class recentEvent extends OP {

    public String[] fidList;

    public event[] eventList;

    int beginDay;

    int untilDay;

    recentEvent(String uid, String uname, String[] fidList, int beginDay, int untilDay) {
        super(uid, uname);
        this.fidList = fidList;
        this.beginDay = beginDay;
        this.untilDay = untilDay;
    }

    public void result(String response) {
        eventList = null;
        try {
            Log.d("test", "response: " + response);
            JSONTokener tokener = new JSONTokener(response);
            Object o = tokener.nextValue();
            if (o instanceof JSONArray) {
                JSONArray array = (JSONArray) o;
                eventList = new event[array.length()];
                for (int i = 0; i < array.length(); ++i) {
                    Log.d("test", "vector length:" + String.valueOf(array.length()));
                    JSONObject object = array.getJSONObject(i);
                    event tmpevent = new event();
                    tmpevent.event_type = object.getString("event_type");
                    tmpevent.uid = object.getString("uid");
                    tmpevent.uname = object.getString("uname");
                    tmpevent.tid = object.getString("tid");
                    tmpevent.date = object.getString("date");
                    if (tmpevent.event_type.equals("post_comment")) {
                        tmpevent.Powner = object.getString("Powner");
                        tmpevent.Pownername = object.getString("Pownername");
                        tmpevent.msg = object.getString("msg");
                        tmpevent.cid = object.getString("cid");
                    } else {
                        tmpevent.Powner = null;
                        tmpevent.Pownername = null;
                        tmpevent.msg = null;
                        tmpevent.cid = null;
                    }
                    eventList[i] = tmpevent;
                }
            } else throw new JSONException("Top element is not a JSONArray");
        } catch (JSONException ex) {
            Log.e("test", "Malformed JSON response: " + "Malformed response", ex);
        }
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("op", "recent_event");
        obj.put("uid", uid);
        JSONArray JfidList = new JSONArray();
        for (int i = 0; i < fidList.length; i++) {
            JfidList.put(fidList[i]);
        }
        obj.put("fidList", JfidList);
        obj.put("beginDay", beginDay);
        obj.put("untilDay", untilDay);
        return obj;
    }
}

class lookAround extends OP {

    public int latitude;

    public int longitude;

    public int scope;

    public String[] fidList;

    public photo[] photoList;

    lookAround(String uid, String uname, String[] fidList, int latitude, int longitude, int scope) {
        super(uid, uname);
        this.fidList = fidList;
        this.latitude = latitude;
        this.longitude = longitude;
        this.scope = scope;
    }

    public void result(String response) {
        photoList = null;
        try {
            Log.d("test", "response: " + response);
            JSONTokener tokener = new JSONTokener(response);
            Object o = tokener.nextValue();
            if (o instanceof JSONArray) {
                JSONArray array = (JSONArray) o;
                photoList = new photo[array.length()];
                for (int i = 0; i < array.length(); ++i) {
                    JSONObject object = array.getJSONObject(i);
                    photoList[i] = new photo(object.getString("photoUrl"), object.getInt("latitude"), object.getInt("longitude"), object.getString("uid"), object.getString("uname"), object.getString("pid"));
                }
            } else throw new JSONException("Top element is not a JSONArray");
        } catch (JSONException ex) {
            Log.e("test", "Malformed JSON response: " + "Malformed response", ex);
        }
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("op", "look_around");
        obj.put("uid", uid);
        JSONArray JfidList = new JSONArray();
        for (int i = 0; i < fidList.length; i++) {
            JfidList.put(fidList[i]);
        }
        obj.put("fidList", JfidList);
        obj.put("latitude", latitude);
        obj.put("longitude", longitude);
        obj.put("scope", scope);
        return obj;
    }
}
