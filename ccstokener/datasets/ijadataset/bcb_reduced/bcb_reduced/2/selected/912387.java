package net.pyTivo.auto_push.main;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;
import net.pyTivo.auto_push.gui.monitor;

public class auto_push {

    private Stack<Hashtable<String, String>> watchList = null;

    private Hashtable<String, Hashtable<String, String>> watching = new Hashtable<String, Hashtable<String, String>>();

    auto_push(Stack<Hashtable<String, String>> w) {
        this.watchList = w;
    }

    private Boolean isVideo(String testFile) {
        if (file.isDir(testFile)) {
            return false;
        }
        if (testFile.matches("^.+\\.txt$") || file.basename(testFile).equals(config.trackingFile)) {
            return false;
        }
        Stack<String> command = new Stack<String>();
        command.add(config.ffmpeg);
        command.add("-i");
        command.add(testFile);
        backgroundProcess process = new backgroundProcess();
        if (process.run(command)) {
            try {
                process.Wait(config.timeout_ffmpeg * 1000);
                Stack<String> l = process.getStderr();
                if (l.size() > 0) {
                    for (int i = 0; i < l.size(); ++i) {
                        if (l.get(i).matches("^.+\\s+Video:\\s+.+$")) {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Timing out command that was taking too long: " + process.toString());
            }
            return false;
        } else {
            process.printStderr();
        }
        return false;
    }

    private void get_stats(String file) {
        File f = new File(file);
        Hashtable<String, String> h;
        if (watching.containsKey(file)) {
            h = watching.get(file);
        } else {
            h = new Hashtable<String, String>();
        }
        h.put("mtime", Long.toString(f.lastModified()));
        h.put("size", Long.toString(f.length()));
        h.put("status", "watching");
        if (!watching.containsKey(file)) {
            watching.put(file, h);
        }
    }

    private Boolean push(String tivoName, String share, String path, String push_file) {
        if (file.isFile(push_file)) {
            String header = "http://" + config.host + ":" + config.port + "/TiVoConnect?Command=Push&Container=";
            String path_entry;
            if (path.length() == 0) {
                path_entry = "&File=/";
            } else {
                path_entry = "&File=/" + urlEncode(path) + "/";
            }
            String urlString = header + urlEncode(share) + path_entry + urlEncode(file.basename(push_file)) + "&tsn=" + urlEncode(tivoName);
            try {
                URL url = new URL(urlString);
                log.print(url.toString());
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.addRequestProperty("REFERER", "/");
                c.setRequestMethod("GET");
                c.setReadTimeout(config.timeout_http * 1000);
                c.connect();
                String response = c.getResponseMessage();
                if (response.equals("OK")) {
                    return true;
                } else {
                    log.error("Received unexpected response for: " + urlString);
                    log.error(response);
                    return false;
                }
            } catch (Exception e) {
                log.error("Connection failed: " + urlString);
                log.error(e.toString());
            }
        } else {
            log.error("File does not exist - " + push_file);
        }
        return false;
    }

    private void process(String tivoName, String share, String path, String videoFile) {
        if (watching.containsKey(videoFile)) {
            if (watching.get(videoFile).get("status").equals("watching")) {
                log.print("checking file: " + videoFile);
                if (file.isFile(videoFile)) {
                    File f = new File(videoFile);
                    String mtime = Long.toString(f.lastModified());
                    String size = Long.toString(f.length());
                    if (watching.get(videoFile).get("mtime").equals(mtime) && watching.get(videoFile).get("size").equals(size)) {
                        log.print("pushing file: " + videoFile);
                        if (push(tivoName, share, path, videoFile)) {
                            watching.get(videoFile).put("status", "pushed");
                            config.appendTrackingFile(share, path, file.basename(videoFile));
                        }
                    } else {
                        get_stats(videoFile);
                    }
                } else {
                    watching.remove(videoFile);
                }
            } else {
                watching.remove(videoFile);
            }
        } else {
            log.print("watching file: " + videoFile);
            get_stats(videoFile);
        }
    }

    private static String urlEncode(String s) {
        String encoded;
        try {
            encoded = URLEncoder.encode(s, "UTF-8");
            return encoded;
        } catch (Exception e) {
            log.error("Cannot encode url: " + s);
            log.error(e.toString());
            return s;
        }
    }

    public void one_loop() {
        for (int i = 0; i < watchList.size(); ++i) {
            if (config.username == null || config.password == null) {
                log.error("tivo_username and/or tivo_password not set in pyTivo config: " + config.pyTivoConf);
                return;
            }
            if (!watchList.get(i).containsKey("tivo")) continue;
            String tivoName = watchList.get(i).get("tivo");
            String watchDir = watchList.get(i).get("path");
            String share = watchList.get(i).get("share");
            if (file.isDir(watchDir)) {
                List<String> files = file.getAllFiles(watchDir);
                if (files != null) {
                    for (int j = 0; j < files.size(); ++j) {
                        String entry = files.get(j);
                        String path = file.dirname(entry.substring(watchDir.length() + 1, entry.length()));
                        if (config.OS.equals("windows")) {
                            path = path.replaceAll("\\\\", "/");
                        }
                        if (path.endsWith("/")) {
                            path = path.substring(0, path.length() - 1);
                        }
                        if (!config.alreadyPushed(watchDir, path, file.basename(entry)) && isVideo(entry)) {
                            process(tivoName, share, path, entry);
                        }
                    }
                }
            }
        }
        if (config.gui != null) {
            monitor.thread_running = false;
        }
    }

    public void main_loop() {
        if (config.username == null || config.password == null) {
            log.error("tivo_username and/or tivo_password not set in pyTivo config: " + config.pyTivoConf);
            return;
        }
        if (config.numActiveShares() == 0) {
            log.error("There are currently no shares setup for auto push");
            return;
        }
        log.print("AUTO WATCH STARTED FOR SHARES:");
        for (int i = 0; i < watchList.size(); i++) {
            if (watchList.get(i).containsKey("tivo")) {
                log.print("share=" + watchList.get(i).get("share") + " path=" + watchList.get(i).get("path") + " tivo=" + watchList.get(i).get("tivo"));
            }
        }
        while (true) {
            one_loop();
            try {
                Thread.sleep(config.wait * 1000);
            } catch (InterruptedException e) {
                log.error(e.toString());
            }
        }
    }
}
