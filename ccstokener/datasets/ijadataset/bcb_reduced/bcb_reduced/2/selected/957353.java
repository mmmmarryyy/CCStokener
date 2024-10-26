package org.pointrel.pointrel20100810.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Remote {

    public static String baseURL = "http://localhost:8000/cgi-bin/simple.py?";

    public static ArrayList<String> remoteCall(Map<String, String> dataDict) {
        ArrayList<String> result = new ArrayList<String>();
        String encodedData = "";
        for (String key : dataDict.keySet()) {
            String encodedSegment = "";
            String value = dataDict.get(key);
            if (value == null) continue;
            try {
                encodedSegment = key + "=" + URLEncoder.encode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (encodedData.length() > 0) {
                encodedData += "&";
            }
            encodedData += encodedSegment;
        }
        try {
            URL url = new URL(baseURL + encodedData);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
                System.out.println("GOT: " + line);
            }
            reader.close();
            result.remove(0);
            if (result.size() != 0) {
                if (!result.get(result.size() - 1).equals("DONE")) {
                    result.clear();
                } else {
                    result.remove(result.size() - 1);
                }
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return result;
    }

    public static Triple query(String uuidToFetch) {
        HashMap<String, String> dict = new HashMap<String, String>();
        dict.put("query", uuidToFetch);
        ArrayList<String> remoteCallResult = remoteCall(dict);
        if (remoteCallResult.size() == 0) return null;
        if (remoteCallResult.get(0).equalsIgnoreCase("EXISTS")) {
            String uuid = remoteCallResult.get(2);
            String previous = remoteCallResult.get(4);
            String merge = remoteCallResult.get(6);
            String a = remoteCallResult.get(8);
            String b = remoteCallResult.get(10);
            String c = "ERROR";
            try {
                c = URLDecoder.decode(remoteCallResult.get(12), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String timestamp = remoteCallResult.get(14);
            String author = remoteCallResult.get(16);
            String signature = remoteCallResult.get(18);
            Triple result = new Triple(uuid, previous, merge, a, b, c, timestamp, author, signature);
            return result;
        }
        return null;
    }

    public static String add(Triple triple) {
        HashMap<String, String> dict = new HashMap<String, String>();
        dict.put("add", triple.uuid);
        dict.put("previous", triple.previous);
        dict.put("merge", triple.merge);
        dict.put("a", triple.a);
        dict.put("b", triple.b);
        dict.put("c", triple.c);
        dict.put("timestamp", triple.timestamp);
        dict.put("author", triple.author);
        dict.put("signature", triple.signature);
        ArrayList<String> remoteCallResult = remoteCall(dict);
        if (remoteCallResult.size() == 0) return null;
        if (remoteCallResult.get(0).equalsIgnoreCase("ERROR")) {
            return null;
        }
        return triple.uuid;
    }

    public static HashMap<String, String> tag(String tagName, String update, String previous) {
        if (tagName == null) tagName = "DEBUG_LIST";
        HashMap<String, String> dict = new HashMap<String, String>();
        dict.put("tag", tagName);
        if (update != null) dict.put("update", update);
        if (previous != null) dict.put("previous", previous);
        ArrayList<String> remoteCallResult = remoteCall(dict);
        if (remoteCallResult.size() == 0) return null;
        HashMap<String, String> result = new HashMap<String, String>();
        if (tagName.equals("DEBUG_LIST")) {
            remoteCallResult.remove(0);
            for (int i = 0; i < remoteCallResult.size() / 2; i++) {
                result.put(remoteCallResult.get(i * 2), remoteCallResult.get(i * 2 + 1));
            }
            return result;
        }
        if (update == null) {
            if (remoteCallResult.get(0).equalsIgnoreCase("EXISTS")) {
                result.put(remoteCallResult.get(1), remoteCallResult.get(2));
                return result;
            } else {
                return null;
            }
        }
        if (remoteCallResult.get(0).equalsIgnoreCase("UPDATED")) {
            result.put(remoteCallResult.get(1), remoteCallResult.get(2));
            return result;
        } else {
            return null;
        }
    }

    public static String tag(String tagName) {
        HashMap<String, String> result = tag(tagName, null, null);
        if (result == null) return null;
        return result.get(tagName);
    }

    public static HashMap<String, String> resource(String resourceUUID, String contents) {
        if (resourceUUID == null) resourceUUID = "DEBUG_LIST";
        HashMap<String, String> dict = new HashMap<String, String>();
        dict.put("resource", resourceUUID);
        if (contents != null) dict.put("contents", contents);
        ArrayList<String> remoteCallResult = remoteCall(dict);
        if (remoteCallResult.size() == 0) return null;
        HashMap<String, String> result = new HashMap<String, String>();
        if (resourceUUID.equals("DEBUG_LIST")) {
            remoteCallResult.remove(0);
            for (int i = 0; i < remoteCallResult.size(); i++) {
                result.put(remoteCallResult.get(i), "");
            }
            return result;
        }
        if (contents == null) {
            if (remoteCallResult.get(0).equalsIgnoreCase("EXISTS")) {
                String decodedContents = "ERROR";
                try {
                    decodedContents = URLDecoder.decode(remoteCallResult.get(2), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                result.put(remoteCallResult.get(1), decodedContents);
                return result;
            } else {
                return null;
            }
        }
        if (remoteCallResult.get(0).equalsIgnoreCase("ADDED")) {
            result.put(remoteCallResult.get(1), "");
            return result;
        } else {
            return null;
        }
    }

    public static String resource(String resourceUUID) {
        HashMap<String, String> result = resource(resourceUUID, null);
        if (result == null) return null;
        return result.get(resourceUUID);
    }
}
