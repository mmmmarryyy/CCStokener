package com.tivo.kmttg.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.tivo.kmttg.JSON.JSONArray;
import com.tivo.kmttg.JSON.JSONException;
import com.tivo.kmttg.JSON.JSONObject;
import com.tivo.kmttg.main.config;
import com.tivo.kmttg.rpc.Remote;

public class file {

    public static Boolean isFile(String f) {
        debug.print("f=" + f);
        try {
            return new File(f).isFile();
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static Boolean isDir(String d) {
        debug.print("d=" + d);
        try {
            return new File(d).isDirectory();
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static long size(String f) {
        debug.print("f=" + f);
        try {
            return new File(f).length();
        } catch (NullPointerException e) {
            return 0;
        } catch (SecurityException e) {
            return 0;
        }
    }

    public static long freeSpace(String f) {
        long bad = 0;
        if (!file.isDir(f)) return bad;
        long free;
        Stack<String> command = new Stack<String>();
        if (config.OS.matches("windows")) {
            command.add("cmd");
            command.add("/c");
            command.add("dir");
            command.add(f);
            backgroundProcess process = new backgroundProcess();
            if (process.run(command)) {
                if (process.Wait() == 0) {
                    Stack<String> l = process.getStdout();
                    if (l.size() > 0) {
                        String free_string = l.lastElement();
                        if (free_string.matches(".*bytes\\s+free")) {
                            String[] ll = free_string.split("\\s+");
                            free_string = ll[ll.length - 3].replaceAll(",", "");
                            try {
                                free = Long.parseLong(free_string);
                                return free;
                            } catch (NumberFormatException e) {
                                return bad;
                            }
                        } else {
                            return bad;
                        }
                    }
                }
            }
        } else {
            backgroundProcess process = new backgroundProcess();
            command.add("/bin/df");
            command.add("-k");
            command.add(f);
            if (process.run(command)) {
                if (process.Wait() == 0) {
                    Stack<String> l = process.getStdout();
                    if (l.size() > 0) {
                        String free_string = l.lastElement();
                        String[] ll = free_string.split("\\s+");
                        if (ll.length - 3 >= 0) {
                            free_string = ll[ll.length - 3];
                            try {
                                free = Long.parseLong(free_string);
                                free = (long) (free * Math.pow(2, 10));
                                return free;
                            } catch (NumberFormatException e) {
                                return bad;
                            }
                        }
                    }
                }
            }
        }
        return bad;
    }

    public static Boolean isEmpty(String f) {
        debug.print("f=" + f);
        if (size(f) == 0) {
            return true;
        } else {
            return false;
        }
    }

    public static Boolean delete(String f) {
        debug.print("f=" + f);
        try {
            return new File(f).delete();
        } catch (NullPointerException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    public static Boolean rename(String fold, String fnew) {
        debug.print("fold=" + fold + " fnew=" + fnew);
        try {
            return new File(fold).renameTo(new File(fnew));
        } catch (NullPointerException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    public static Boolean create(String fileName) {
        debug.print("fileName=" + fileName);
        try {
            File f = new File(fileName);
            return f.createNewFile();
        } catch (IOException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    public static String makeTempFile(String prefix) {
        debug.print("prefix=" + prefix);
        try {
            File tmp = File.createTempFile(prefix, ".tmp", new File(config.tmpDir));
            tmp.deleteOnExit();
            return tmp.getPath();
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public static String makeTempFile(String prefix, String suffix) {
        debug.print("prefix=" + prefix);
        try {
            File tmp = File.createTempFile(prefix, suffix, new File(config.tmpDir));
            tmp.deleteOnExit();
            return tmp.getPath();
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public static String unixWhich(String c) {
        if (c != null) {
            Stack<String> command = new Stack<String>();
            command.add("/usr/bin/which");
            command.add(c);
            backgroundProcess process = new backgroundProcess();
            if (process.run(command)) {
                if (process.Wait() == 0) {
                    String result = process.getStdoutLast();
                    if (result.length() > 0 && file.isFile(result)) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    public static void TivoWebPlusDelete(String download_url) {
        if (download_url == null) return;
        int port = 8080;
        Pattern p = Pattern.compile("http://(\\S+):.+&id=(.+)$");
        Matcher m = p.matcher(download_url);
        if (m.matches()) {
            String ip = m.group(1);
            final String id = m.group(2);
            final String urlString = "http://" + ip + ":" + port + "/confirm/del/" + id;
            log.warn(">> Issuing TivoWebPlus show delete request: " + urlString);
            try {
                final URL url = new URL(urlString);
                class AutoThread implements Runnable {

                    AutoThread() {
                    }

                    public void run() {
                        int timeout = 10;
                        try {
                            String data = "u2=bnowshowing";
                            data += "&sub=Delete";
                            data += "&" + URLEncoder.encode("fsida(" + id + ")", "UTF-8") + "=on";
                            data += "&submit=Confirm_Delete";
                            HttpURLConnection c = (HttpURLConnection) url.openConnection();
                            c.setRequestMethod("POST");
                            c.setReadTimeout(timeout * 1000);
                            c.setDoOutput(true);
                            c.connect();
                            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(c.getOutputStream()));
                            bw.write(data);
                            bw.flush();
                            bw.close();
                            String response = c.getResponseMessage();
                            if (response.equals("OK")) {
                                log.print(">> TivoWebPlus delete succeeded.");
                            } else {
                                log.error("TWP Delete: Received unexpected response for: " + urlString);
                                log.error(response);
                            }
                        } catch (Exception e) {
                            log.error("TWP Delete: connection failed: " + urlString);
                            log.error(e.toString());
                        }
                    }
                }
                AutoThread t = new AutoThread();
                Thread thread = new Thread(t);
                thread.start();
            } catch (Exception e) {
                log.error("TWP Delete: connection failed: " + urlString);
                log.error(e.toString());
            }
        }
    }

    public static Boolean iPadDelete(String tivoName, String recordingId) {
        if (recordingId == null) {
            log.error("iPad Delete got null recordingId");
            return false;
        }
        JSONArray a = new JSONArray();
        JSONObject json = new JSONObject();
        a.put(recordingId);
        try {
            json.put("recordingId", a);
            log.warn(">> Attempting iPad delete for id: " + recordingId);
            Remote r = new Remote(tivoName);
            if (r.success) {
                if (r.Command("delete", json) != null) log.warn(">> iPad delete succeeded.");
                r.disconnect();
            }
        } catch (JSONException e) {
            log.error("iPad delete failed - " + e.getMessage());
            return false;
        }
        return true;
    }
}
