package com.example.androidwar.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class Util {

    public static final String BR = System.getProperty("line.separator");

    public static void showOKDialog(final Activity activity, String title, String text) {
        AlertDialog.Builder ad = new AlertDialog.Builder(activity);
        ad.setTitle(title);
        ad.setMessage(text);
        ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                activity.setResult(Activity.RESULT_OK);
            }
        });
        ad.create();
        ad.show();
    }

    public static HashMap<String, Object> generateMoveData(int action, String name, int x, int y) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("action", action);
        data.put("name", name);
        HashMap<String, Object> value = new HashMap<String, Object>();
        value.put("x", x);
        value.put("y", y);
        value.put("power", 0);
        data.put("value", value);
        return data;
    }

    public static HashMap<String, Object> generateAtackData(int action, String name, int power) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("action", action);
        data.put("name", name);
        HashMap<String, Object> value = new HashMap<String, Object>();
        value.put("x", 0);
        value.put("y", 0);
        value.put("power", power);
        data.put("value", value);
        return data;
    }

    @SuppressWarnings("unchecked")
    public static String showMoveData(HashMap<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("name:" + data.get("name"));
        sb.append(" ");
        sb.append("action:" + data.get("action"));
        sb.append(" ");
        HashMap<String, Object> value = (HashMap<String, Object>) data.get("value");
        sb.append("value[" + " x:" + value.get("x") + " y:" + value.get("y") + "]");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static String showAttackData(HashMap<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("name:" + data.get("name"));
        sb.append(" ");
        sb.append("action:" + data.get("action"));
        sb.append(" ");
        HashMap<String, Object> value = (HashMap<String, Object>) data.get("value");
        sb.append("value[" + " power:" + value.get("power") + "]");
        return sb.toString();
    }

    /**
     * データを送る
     * @param name
     * @param action
     * @param x
     * @param y
     * @return
     */
    public static String push(String name, int action, int x, int y, int power) {
        String result = "";
        try {
            String line;
            URL url = new URL("http://hackathon20091219-sensor.appspot.com/recieveEvent");
            HttpURLConnection uc = (HttpURLConnection) url.openConnection();
            uc.setRequestMethod("POST");
            uc.setRequestProperty("Accept-Language", "ja");
            uc.setDoOutput(true);
            PrintWriter writer;
            BufferedReader reader;
            String text = "{\"name\":\"" + name + "\"" + ",\"value\":" + "{\"y\":" + Integer.toString(y) + ",\"power\":" + Integer.toString(power) + ",\"x\":" + Integer.toString(x) + "},\"action\":" + action + "}";
            System.out.println(text);
            writer = new PrintWriter(uc.getOutputStream());
            writer.print("event=" + text);
            writer.close();
            reader = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            while ((line = reader.readLine()) != null) {
                result += line;
            }
            reader.close();
            uc.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
