package com.facebook.android;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

public class RequestPoster {

    /** Called when the activity is first created. */
    String domain;

    ArrayList<NameValuePair> library;

    public RequestPoster(String domain, ArrayList<NameValuePair> library) {
        this.domain = domain;
        this.library = library;
    }

    public void execute() {
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(domain);
            httppost.setEntity(new UrlEncodedFormEntity(library));
            httpclient.execute(httppost);
        } catch (Exception e) {
            Log.e("log_tag", "Error in http connection " + e.toString());
        }
    }

    public String[] getFriends() {
        InputStream is = null;
        String[] answer = null;
        String result = "";
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(domain);
            httppost.setEntity(new UrlEncodedFormEntity(library));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        } catch (Exception e) {
            Log.e("log_tag", "Error in http connection " + e.toString());
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + ",");
            }
            is.close();
            result = sb.toString();
            if (result.equals("null,")) {
                answer = new String[1];
                answer[0] = "none";
                return answer;
            }
        } catch (Exception e) {
            Log.e("log_tag", "Error converting result " + e.toString());
        }
        try {
            JSONArray json = new JSONArray(result);
            answer = new String[json.length()];
            for (int i = 0; i < json.length(); i++) {
                JSONObject jsonId = json.getJSONObject(i);
                answer[i] = jsonId.getString("uid");
            }
        } catch (JSONException e) {
            Log.e("log_tag", "Error parsing data " + e.toString());
        }
        return answer;
    }

    public LinkedList<NameValuePair> getScoreboard() {
        InputStream is = null;
        String result = "";
        LinkedList<NameValuePair> scores = new LinkedList<NameValuePair>();
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(domain);
            httppost.setEntity(new UrlEncodedFormEntity(library));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        } catch (Exception e) {
            Log.e("log_tag", "Error in http connection " + e.toString());
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + ",");
            }
            is.close();
            result = sb.toString();
            if (result.equals("null,")) {
                return null;
            }
        } catch (Exception e) {
            Log.e("log_tag", "Error converting result " + e.toString());
        }
        try {
            JSONObject json = new JSONObject(result);
            JSONArray data = json.getJSONArray("data");
            JSONArray me = json.getJSONArray("me");
            for (int i = 0; i < data.length(); i++) {
                JSONObject single = data.getJSONObject(i);
                String uid = single.getString("uid");
                String score = single.getString("score");
                scores.add(new BasicNameValuePair(uid, score));
            }
            for (int i = 0; i < me.length(); i++) {
                JSONObject single = me.getJSONObject(i);
                String uid = single.getString("uid");
                String score = single.getString("score");
                scores.add(new BasicNameValuePair(uid, score));
            }
            System.out.println(json);
        } catch (JSONException e) {
            Log.e("log_tag", "Error parsing data " + e.toString());
        }
        return scores;
    }

    public LinkedList<NameValuePair> getQuestion() {
        InputStream is = null;
        String result = "";
        LinkedList<NameValuePair> question = new LinkedList<NameValuePair>();
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(domain);
            httppost.setEntity(new UrlEncodedFormEntity(library));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        } catch (Exception e) {
            Log.e("log_tag", "Error in http connection " + e.toString());
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            is.close();
            result = sb.toString();
            if (result.equals("null,")) {
                return null;
            }
        } catch (Exception e) {
            Log.e("log_tag", "Error converting result " + e.toString());
        }
        try {
            JSONObject json = new JSONObject(result);
            JSONArray data = json.getJSONArray("data");
            JSONObject quest = data.getJSONObject(0);
            question.add(new BasicNameValuePair("q", quest.getString("q")));
            question.add(new BasicNameValuePair("a", quest.getString("a")));
            question.add(new BasicNameValuePair("b", quest.getString("b")));
            question.add(new BasicNameValuePair("c", quest.getString("c")));
            question.add(new BasicNameValuePair("d", quest.getString("d")));
            question.add(new BasicNameValuePair("correct", quest.getString("correct")));
            return question;
        } catch (JSONException e) {
            Log.e("log_tag", "Error parsing data " + e.toString());
        }
        return null;
    }

    public String getChallengers() {
        InputStream is = null;
        String result = "";
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(domain);
            httppost.setEntity(new UrlEncodedFormEntity(library));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        } catch (Exception e) {
            Log.e("log_tag", "Error in http connection " + e.toString());
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + ",");
            }
            is.close();
            result = sb.toString();
            if (result.equals("null,")) {
                return "none";
            } else return result;
        } catch (Exception e) {
            Log.e("log_tag", "Error converting result " + e.toString());
        }
        return "none";
    }
}
