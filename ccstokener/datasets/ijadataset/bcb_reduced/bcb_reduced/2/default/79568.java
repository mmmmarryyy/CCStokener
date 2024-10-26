import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

class WebURL {

    String url;

    private URL uri;

    private URLConnection urlconn;

    WebURL(String url) throws IOException {
        uri = new URL(url);
        urlconn = uri.openConnection();
    }

    public ArrayList<String> getHTML() throws IOException {
        String line = null;
        ArrayList<String> data = new ArrayList<String>();
        urlconn.connect();
        BufferedReader buffreader = new BufferedReader(new InputStreamReader(urlconn.getInputStream()));
        line = buffreader.readLine();
        while (line != null) {
            data.add(line);
            line = buffreader.readLine();
        }
        return data;
    }

    public int getSize() throws IOException {
        urlconn.connect();
        return urlconn.getContentLength();
    }
}

public class Youtube {

    private String videoUrl, videoTitle, fmt, downloadURL;

    private String videoID, videoTID;

    private HashMap<String, String> fmtList;

    private ArrayList<String> data;

    Youtube() {
        initFmt();
    }

    Youtube(String videoUrl, String quality) {
        this.videoUrl = videoUrl;
        initFmt();
        setFmt(quality);
        try {
            data = new WebURL(videoUrl).getHTML();
            setVideoid();
            setTID();
            setVideoTitle();
            makeDownloadUrl();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void makeDownloadUrl() {
        downloadURL = "http://www.youtube.com/get_video?video_id=" + videoID + "&t=" + videoTID + "&fmt=" + fmt;
    }

    private void setVideoid() {
        videoID = videoUrl.split("=")[1].substring(0, 11);
    }

    private void setVideoTitle() {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).contains("document.title")) {
                int start = data.get(i).indexOf("document.title = '") + 18;
                int end = data.get(i).lastIndexOf("';");
                videoTitle = data.get(i).substring(start, end);
                System.out.println(videoTitle);
                String[] badChars = { "\\", "/", "\"", "<", ">", "|", "*", ":" };
                for (int k = 0; k < badChars.length; k++) if (videoTitle.contains(badChars[k])) {
                    videoTitle = videoTitle.replace(badChars[k], "");
                }
                break;
            }
        }
    }

    public String getVideoTitle() throws UnsupportedEncodingException {
        if (videoTitle == null) return videoTitle = "video" + new Random().nextInt(100);
        return new String(videoTitle.getBytes(), "UTF-8");
    }

    private void initFmt() {
        fmtList = new HashMap<String, String>();
        fmtList.put("FLV - 320x240", "5");
        fmtList.put("3GP - 176x144", "17");
        fmtList.put("MP4 - 480x360", "18");
        fmtList.put("MP4 - 1280x720", "22");
        fmtList.put("FLV - 640x480", "34");
        fmtList.put("FLV - 854x640", "35");
        fmtList.put("MP4 - 1920x1080", "37");
    }

    private void setFmt(String quality) {
        fmt = fmtList.get(quality);
    }

    public ArrayList<String> getFmtList() {
        ArrayList<String> fmtVect = new ArrayList<String>();
        Iterator<String> iterator = (fmtList.keySet()).iterator();
        while (iterator.hasNext()) {
            String key = iterator.next().toString();
            fmtVect.add(key);
        }
        return fmtVect;
    }

    public String getExt() {
        if (fmt.equals("5") || fmt.equals("34") || fmt.equals("35")) return ".flv";
        if (fmt.equals("17")) return ".3gp";
        if (fmt.equals("18") || fmt.equals("22") || fmt.equals("37")) return ".mp4";
        return null;
    }

    private void setTID() {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).contains("&t=")) {
                int start = data.get(i).indexOf("&t=") + 3;
                videoTID = data.get(i).substring(start, start + 46);
                break;
            }
        }
    }

    public int getVideoSize() throws IOException {
        return new WebURL(downloadURL).getSize();
    }

    public String getDownloadURL() {
        return downloadURL;
    }
}
