package com.jon.android.drupaldroid.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.insready.drupalcloud.ServiceNotAvailableException;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Services Server output formats that are currently supported:
 * 
 * @author Jingsheng Wang
 */
public class JSONServerClient implements DrupalService {

    public HttpPost mSERVER;

    public static String mAPI_KEY;

    public static String mDOMAIN;

    public static String mALGORITHM;

    public static Long mSESSION_LIFETIME;

    private HttpClient mClient = new DefaultHttpClient();

    private List<NameValuePair> mPairs = new ArrayList<NameValuePair>(15);

    private Context mCtx;

    private final String mPREFS_AUTH;

    /**
	 * 
	 * @param _ctx
	 *            Context
	 * @param _prefs_auth
	 *            Preference storage
	 * @param _server
	 *            Server address
	 * @param _api_key
	 *            API_Key
	 * @param _domain
	 *            Domain name
	 * @param _algorithm
	 *            Encrypition algorithm
	 * @param _session_lifetime
	 *            Session lifetime
	 */
    public JSONServerClient(Context _ctx, String _prefs_auth, String _server, String _api_key, String _domain, String _algorithm, Long _session_lifetime) {
        mPREFS_AUTH = _prefs_auth;
        mSERVER = new HttpPost(_server);
        mSERVER.setHeader("User-Agent", "DrupalCloud-1.x");
        mAPI_KEY = _api_key;
        mDOMAIN = _domain;
        mALGORITHM = _algorithm;
        mSESSION_LIFETIME = _session_lifetime;
        mCtx = _ctx;
    }

    private String getSessionID() throws ServiceNotAvailableException {
        SharedPreferences auth = mCtx.getSharedPreferences(mPREFS_AUTH, 0);
        Long timestamp = auth.getLong("sessionid_timestamp", 0);
        Long currenttime = new Date().getTime() / 100;
        String sessionid = auth.getString("sessionid", null);
        if (sessionid == null || (currenttime - timestamp) >= mSESSION_LIFETIME) {
            systemConnect();
            sessionid = auth.getString("sessionid", null);
        }
        return sessionid;
    }

    /**
	 * Generic request
	 * 
	 * @param method
	 *            Request name
	 * @param parameters
	 *            Parameters
	 * @return result string
	 */
    public String call(String method, BasicNameValuePair[] parameters) throws ServiceNotAvailableException {
        String sessid = this.getSessionID();
        mPairs.clear();
        String nonce = Integer.toString(new Random().nextInt());
        Mac hmac;
        try {
            hmac = Mac.getInstance(JSONServerClient.mALGORITHM);
            final Long timestamp = new Date().getTime() / 100;
            final String time = timestamp.toString();
            hmac.init(new SecretKeySpec(JSONServerClient.mAPI_KEY.getBytes(), JSONServerClient.mALGORITHM));
            String message = time + ";" + JSONServerClient.mDOMAIN + ";" + nonce + ";" + method;
            hmac.update(message.getBytes());
            String hmac_value = new String(Hex.encodeHex(hmac.doFinal()));
            mPairs.add(new BasicNameValuePair("hash", "\"" + hmac_value + "\""));
            mPairs.add(new BasicNameValuePair("domain_name", "\"" + JSONServerClient.mDOMAIN + "\""));
            mPairs.add(new BasicNameValuePair("domain_time_stamp", "\"" + time + "\""));
            mPairs.add(new BasicNameValuePair("nonce", "\"" + nonce + "\""));
            mPairs.add(new BasicNameValuePair("method", "\"" + method + "\""));
            mPairs.add(new BasicNameValuePair("api_key", "\"" + JSONServerClient.mAPI_KEY + "\""));
            mPairs.add(new BasicNameValuePair("sessid", "\"" + sessid + "\""));
            for (int i = 0; i < parameters.length; i++) {
                mPairs.add(parameters[i]);
            }
            mSERVER.setEntity(new UrlEncodedFormEntity(mPairs));
            HttpResponse response = mClient.execute(mSERVER);
            InputStream is = response.getEntity().getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String result = br.readLine();
            JSONObject jso;
            jso = new JSONObject(result);
            boolean error = jso.getBoolean("#error");
            if (error) {
                String errorMsg = jso.getString("#data");
                throw new ServiceNotAvailableException(errorMsg);
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
            throw new ServiceNotAvailableException("Remote server is not available");
        }
        return null;
    }

    /**
	 * system.connect request for Key Auth
	 */
    private void systemConnect() throws ServiceNotAvailableException {
        mPairs.add(new BasicNameValuePair("method", "\"system.connect\""));
        try {
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(mPairs);
            String entityString = entity.toString();
            mSERVER.setEntity(entity);
            HttpResponse response = mClient.execute(mSERVER);
            InputStream result = response.getEntity().getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(result));
            JSONObject jso = new JSONObject(br.readLine());
            boolean error = jso.getBoolean("#error");
            String data = jso.getString("#data");
            if (error) {
                throw new ServiceNotAvailableException(data);
            }
            jso = new JSONObject(data);
            saveSession(data);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void saveSession(String data) throws JSONException {
        JSONObject jso = new JSONObject(data);
        SharedPreferences auth = mCtx.getSharedPreferences(mPREFS_AUTH, 0);
        SharedPreferences.Editor editor = auth.edit();
        editor.putString("sessionid", jso.getString("sessid"));
        editor.putLong("sessionid_timestamp", new Date().getTime() / 100);
        editor.commit();
    }

    @Override
    public void login(String username, String password) throws ServiceNotAvailableException {
        String response = null;
        try {
            BasicNameValuePair[] parameters = new BasicNameValuePair[2];
            parameters[0] = new BasicNameValuePair("username", "\"" + username + "\"");
            parameters[1] = new BasicNameValuePair("password", "\"" + password + "\"");
            response = call("user.login", parameters);
            JSONObject jso = new JSONObject(response);
            JSONObject data = jso.getJSONObject("#data");
            saveSession(data.toString());
            JSONObject user = data.getJSONObject("user");
            SharedPreferences auth = mCtx.getSharedPreferences(mPREFS_AUTH, 0);
            SharedPreferences.Editor editor = auth.edit();
            editor.putString("uid", user.getString("uid"));
            editor.putString("name", user.getString("name"));
            editor.commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void logout(String sessionID) throws ServiceNotAvailableException {
        if (sessionID == null) {
            systemConnect();
            SharedPreferences auth = mCtx.getSharedPreferences(mPREFS_AUTH, 0);
            sessionID = auth.getString("sessionid", null);
        }
        BasicNameValuePair[] parameters = new BasicNameValuePair[1];
        parameters[0] = new BasicNameValuePair("sessid", "\"" + sessionID + "\"");
        String response = call("user.logout", parameters);
        SharedPreferences auth = mCtx.getSharedPreferences(mPREFS_AUTH, 0);
        SharedPreferences.Editor editor = auth.edit();
        editor.remove("uid");
        editor.remove("name");
        editor.remove("sessionid");
        editor.remove("sessionid_timestamp");
        editor.commit();
    }

    @Override
    public DrupalNode nodeGet(DrupalNode node) throws ServiceNotAvailableException {
        List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
        parameters.add(new BasicNameValuePair("nid", "" + node.getNid() + ""));
        String result = call("node.get", parameters.toArray(new BasicNameValuePair[0]));
        try {
            JSONObject jso = new JSONObject(result);
            node = buildNode(jso.getJSONObject("#data"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return node;
    }

    @Override
    public List<DrupalNode> viewsGet(String view_name, String display_id, String args, int offset, int limit) throws ServiceNotAvailableException {
        List<DrupalNode> nodes = new ArrayList<DrupalNode>();
        List<BasicNameValuePair> paramList = new ArrayList<BasicNameValuePair>();
        paramList.add(new BasicNameValuePair("view_name", "\"" + view_name + "\""));
        if (args != null) {
            paramList.add(new BasicNameValuePair("args", "\"" + args + "\""));
        }
        if (display_id != null) {
            paramList.add(new BasicNameValuePair("display_id", "\"" + display_id + "\""));
        }
        if (offset != -1) {
            paramList.add(new BasicNameValuePair("offset", "\"" + offset + "" + "\""));
        }
        if (limit != -1) {
            paramList.add(new BasicNameValuePair("limit", "\"" + limit + "" + "\""));
        }
        String result = call("views.get", paramList.toArray(new BasicNameValuePair[0]));
        DrupalNode node = new DrupalNode();
        try {
            JSONObject jso = new JSONObject(result);
            JSONArray array = jso.getJSONArray("#data");
            for (int i = 0; i < array.length(); i++) {
                node = buildNode(array.getJSONObject(i));
                nodes.add(node);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return nodes;
    }

    private DrupalNode buildNode(JSONObject obj) throws JSONException {
        DrupalNode node = new DrupalNode();
        node.setBody(obj.getString("body"));
        node.setNid(Long.parseLong(obj.getString("nid")));
        node.setTeaser(obj.getString("teaser"));
        node.setTitle(obj.getString("title"));
        node.setUID(obj.getString("uid"));
        node.setName(obj.getString("name"));
        node.setChanged(obj.getString("changed"));
        return node;
    }

    @Override
    public int commentSave(DrupalNode node) throws ServiceNotAvailableException {
        JSONObject array = new JSONObject();
        try {
            array.put("uid", node.getUID());
            array.put("subject", node.getTitle());
            array.put("comment", node.getBody());
            array.put("name", node.getName());
            array.put("pid", node.getCid());
            array.put("nid", node.getNid());
            BasicNameValuePair[] nvp = { new BasicNameValuePair("comment", array.toString()) };
            String result = call("comment.save", nvp);
            JSONObject jso;
            jso = new JSONObject(result);
            int cid = jso.getInt("#data");
            return cid;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public List<DrupalNode> commentLoadNodeComments(long nid, int count, int start) throws ServiceNotAvailableException {
        Map<String, DrupalNode> nodes = new TreeMap<String, DrupalNode>();
        BasicNameValuePair[] parameters = new BasicNameValuePair[3];
        parameters[0] = new BasicNameValuePair("nid", "\"" + String.valueOf(nid) + "\"");
        parameters[1] = new BasicNameValuePair("count", "\"" + String.valueOf(count) + "\"");
        parameters[2] = new BasicNameValuePair("start", "\"" + String.valueOf(start) + "\"");
        String result = call("comment.loadNodeComments", parameters);
        result = result.replaceAll("(\\\\r\\\\n|\\\\r)", "\\\\n");
        try {
            JSONObject jso = new JSONObject(result);
            JSONArray array = jso.getJSONArray("#data");
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                DrupalNode node = buildComment(obj);
                nodes.put(node.getThread(), node);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new ArrayList<DrupalNode>(nodes.values());
    }

    private DrupalNode buildComment(JSONObject obj) throws JSONException {
        DrupalNode node = new DrupalNode();
        node.setTitle(obj.getString("subject"));
        node.setBody(obj.getString("comment"));
        node.setUID(obj.getString("uid"));
        node.setName(obj.getString("name"));
        node.setChanged(obj.getString("timestamp"));
        node.setNid(obj.getLong("nid"));
        node.setCid(obj.getLong("cid"));
        node.setThread(obj.getString("thread").replace("/", ""));
        return node;
    }

    @Override
    public DrupalNode commentLoad(long cid) throws ServiceNotAvailableException {
        BasicNameValuePair[] parameters = new BasicNameValuePair[1];
        parameters[0] = new BasicNameValuePair("cid", "\"" + String.valueOf(cid) + "\"");
        String result = call("comment.load", parameters);
        DrupalNode node = new DrupalNode();
        try {
            JSONObject jso = new JSONObject(result);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return node;
    }

    @Override
    public void connect() throws Exception {
    }

    @Override
    public void nodeSave(DrupalNode node) throws Exception {
        JSONObject array = new JSONObject();
        array.put("type", node.getType());
        array.put("uid", node.getUID());
        array.put("title", node.getTitle());
        array.put("body", node.getBody());
        array.put("name", node.getName());
        BasicNameValuePair[] nvp = { new BasicNameValuePair("node", array.toString()) };
        String result = call("node.save", nvp);
        try {
            JSONObject jso = new JSONObject(result);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
