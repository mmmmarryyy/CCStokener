package org.sensorweb.demo.report;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.net.URL;

public class StopWatch {

    public static final String END = "end";

    public static final String PAUSE = "pause";

    public static final String RESUME = "resume";

    public static final String START = "start";

    public static synchronized HashMap<Integer, HashMap<String, Long>> readFromFile(File file) {
        BufferedReader br = null;
        HashMap<Integer, HashMap<String, Long>> data = new HashMap<Integer, HashMap<String, Long>>();
        try {
            br = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                Integer id = 0;
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].trim().equals("")) continue;
                    if (i == 0) {
                        String p = parts[0];
                        id = new Integer(p.substring(p.indexOf(":") + 1));
                        if (!data.containsKey(id)) {
                            data.put(id, new HashMap<String, Long>());
                        }
                    } else {
                        String str1 = parts[i].substring(0, parts[i].indexOf(":"));
                        String str2 = parts[i].substring(parts[i].indexOf(":") + 1);
                        if (!data.get(id).containsKey(str1)) data.get(id).put(str1, new Long(str2)); else data.get(id).put(str1, data.get(id).get(str1) + new Long(str2));
                    }
                }
            }
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized HashMap<Integer, HashMap<String, Long>> readFromFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) return null;
        return readFromFile(file);
    }

    private Integer id = -1;

    private Queue<Mark> watch = null;

    private String filename = "result.txt";

    public StopWatch(Integer id) {
        watch = new LinkedList<Mark>();
        this.id = id;
    }

    public void start() {
        mark(START);
    }

    public void pause() {
        mark(PAUSE);
    }

    public void resume() {
        mark(RESUME);
    }

    public void stop() {
        mark(END);
    }

    public void mark(String tag) {
        mark(tag, System.currentTimeMillis());
    }

    public void mark(String tag, long time) {
        watch.offer(new Mark(tag, time));
    }

    public long getTotal() {
        return get(START, END);
    }

    public long get(String from, String to) {
        try {
            Queue<Mark> copyQ = new LinkedList<Mark>(watch);
            Iterator<Mark> iter = copyQ.iterator();
            long start_point_time = 0;
            long end_point_time = 0;
            long total_pause_time = 0;
            long last_pause_point_time = 0;
            boolean in = false;
            boolean inPause = false;
            while (iter.hasNext()) {
                Mark mark = iter.next();
                if (mark.tag.equals(from)) {
                    in = true;
                    start_point_time = mark.time;
                    break;
                }
            }
            if (!in) return 0;
            while (iter.hasNext()) {
                Mark mark = iter.next();
                if (inPause) {
                    if (!mark.tag.equals(RESUME)) return 0;
                    total_pause_time += mark.time - last_pause_point_time;
                    inPause = false;
                } else {
                    if (mark.tag.equals(to)) {
                        in = false;
                        end_point_time = mark.time;
                        break;
                    } else if (mark.tag.equals(PAUSE)) {
                        last_pause_point_time = mark.time;
                        inPause = true;
                    }
                }
            }
            if (in) return 0;
            return (end_point_time - start_point_time - total_pause_time);
        } catch (Exception e) {
            return 0;
        }
    }

    public synchronized void writeToFile(String[] tags) {
        Properties props = new Properties();
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource("cache.conf");
            if (url == null) {
                url = new URL("file:///C:/Program Files/Apache Software Foundation/Tomcat 5.5/webapps/wsrf/cache.conf");
            }
            props.load(url.openStream());
            if (props.containsKey("filename")) filename = props.getProperty("filename");
            writeToFile(filename, tags);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void writeToFile(File file, String[] tags) {
        if (tags.length % 2 != 0) return;
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file, true));
            String str = "";
            str += "id:" + id + ";";
            for (int i = 0; i < tags.length; i += 2) {
                str += tags[i] + ":" + get(tags[i], tags[i + 1]) + ";";
            }
            bw.write(str);
            bw.newLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.flush();
                    bw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void writeToFile(String filename, String[] tags) {
        if (tags.length % 2 != 0) return;
        File file = new File(filename);
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        writeToFile(file, tags);
    }

    public static void main(String[] args) {
        StopWatch watch = new StopWatch(0);
        watch.writeToFile(new String[] { "sdf" });
    }
}

class Mark {

    public String tag;

    public Long time;

    public Mark(String tag, Long time) {
        this.tag = tag;
        this.time = time;
    }
}
