package no.ntnu.kpro09.renderer.video;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import no.ntnu.kpro09.renderer.MyInstruction;

/**
 * The video instruction has to download the file to the disk, since JavaFX
 * doesn't recognize the file from YouTube (because of the url format). HTML5
 * would have been able to play it, but JavaFX doesn't have great HTML5 support
 * either:-) It is ugly, it is slow, but it is working, and that's the most
 * important thing for the presentation.
 * 
 * Should be rewritten to something more sensible later!
 * 
 * @author Gaute Nordhaug
 * 
 */
public class VideoInstruction extends MyInstruction implements Runnable {

    private int xpos;

    private int ypos;

    private int offset;

    private String videoUrl;

    private String videoId;

    private boolean fullscreen;

    private Thread thread;

    private boolean ready = false;

    /**
	 * Setting the url of the video, if a file with that name does not exist on
	 * the disk it will get downloaded.
	 * 
	 * @param videoId
	 */
    public void setUrl(String videoId) {
        this.videoId = videoId.trim();
        File file = new File(videoId + ".flv");
        videoUrl = file.getPath();
        videoUrl = videoUrl.replaceAll("\\\\", "/");
        if (!file.exists()) {
            thread = new Thread(this);
            thread.start();
        } else {
            ready = true;
        }
    }

    /**
	 * This function will download a youtube flv file and save it to the disk.
	 */
    public void run() {
        videoId = videoId.trim();
        System.out.println("fetching video");
        String requestUrl = "http://www.youtube.com/get_video_info?&video_id=" + videoId;
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = rd.readLine();
            int from = line.indexOf("&token=") + 7;
            int to = line.indexOf("&thumbnail_url=");
            String id = line.substring(from, to);
            String tmp = "http://www.youtube.com/get_video?video_id=" + videoId + "&t=" + id;
            url = new URL(tmp);
            conn = (HttpURLConnection) url.openConnection();
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            rd.readLine();
            tmp = conn.getURL().toString();
            url = new URL(tmp);
            conn = (HttpURLConnection) url.openConnection();
            InputStream is;
            OutputStream outStream;
            URLConnection uCon;
            byte[] buf;
            int ByteRead, ByteWritten = 0;
            url = new URL(tmp);
            outStream = new BufferedOutputStream(new FileOutputStream(videoId + ".flv"));
            uCon = url.openConnection();
            is = uCon.getInputStream();
            buf = new byte[1024];
            while ((ByteRead = is.read(buf)) != -1) {
                outStream.write(buf, 0, ByteRead);
                ByteWritten += ByteRead;
            }
            is.close();
            outStream.close();
            System.out.println(videoUrl + " is ready");
        } catch (Exception e) {
            System.out.println("Could not find flv-url " + videoId + "! " + e.getMessage());
        } finally {
            ready = true;
        }
    }

    public int getXpos() {
        return xpos;
    }

    public void setXpos(int xpos) {
        this.xpos = xpos;
    }

    public int getYpos() {
        return ypos;
    }

    public void setYpos(int ypos) {
        this.ypos = ypos;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(int fullscreen) {
        if (fullscreen == 1) {
            this.fullscreen = true;
        } else {
            this.fullscreen = false;
        }
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public boolean isReady() {
        return ready;
    }
}
