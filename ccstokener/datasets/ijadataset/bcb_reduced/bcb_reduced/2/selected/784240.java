package com.weespers.download;

import java.io.BufferedInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.swt.browser.Browser;

public class RealVideoExtractor {

    public static void main(String[] args) {
        RealVideoExtractor x = new RealVideoExtractor("KdrVkPBy5nw");
        x.getRealVideos();
    }

    protected String videoId;

    protected ArrayList<RealVideo> realVideos = null;

    protected Browser browser = null;

    public void setBrowser(Browser browser) {
        this.browser = browser;
    }

    public RealVideoExtractor(String videoId) {
        setVideoId(videoId);
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
        realVideos = null;
    }

    public RealVideo getLowQualityRealVideo(int attempts) throws Exception {
        for (int i = 0; i < attempts; i++) {
            RealVideo realVideo = getLowQualityRealVideo();
            if (realVideo != null) return realVideo; else realVideos = null;
        }
        return null;
    }

    public RealVideo getHighQualityRealVideo() {
        for (RealVideo realVideo : getRealVideos()) {
            System.err.println(realVideo.getTypeId());
            if (realVideo.getTypeId() == 18) {
                return realVideo;
            }
        }
        return null;
    }

    public RealVideo getLowQualityRealVideo() {
        for (RealVideo realVideo : getRealVideos()) {
            if (realVideo.getTypeId() == 5) {
                return realVideo;
            }
        }
        return null;
    }

    public List<RealVideo> getRealVideos() {
        if (realVideos == null) {
            String html = getHtml();
            realVideos = new ArrayList<RealVideo>();
            String parameter = "\"url_encoded_fmt_stream_map\": \"";
            Pattern p = Pattern.compile(parameter + ".*?\"");
            Matcher m = p.matcher(html);
            if (m.find()) {
                String fmtUrlMap = m.group();
                fmtUrlMap = fmtUrlMap.substring(parameter.length(), fmtUrlMap.length() - 1);
                String[] parts = fmtUrlMap.split(",");
                for (String part : parts) {
                    try {
                        part = part.substring(4);
                        int end = part.indexOf("quality=");
                        end = end - 6;
                        String url = part.substring(0, end);
                        String rest = part.substring(end);
                        String typeId = rest.substring(rest.lastIndexOf("=") + 1);
                        System.err.println(typeId);
                        RealVideo realVideo = new RealVideo();
                        realVideo.setTypeId(Integer.parseInt(typeId));
                        realVideo.setUrl(URLDecoder.decode(url));
                        realVideos.add(realVideo);
                    } catch (Exception ex) {
                    }
                }
            }
        }
        System.err.println(realVideos.size());
        return realVideos;
    }

    public String cleanUrl(String url) {
        url = url.replaceAll("\\\\", "");
        url = url.replaceAll("u0026", "&");
        url = url.replaceAll("%2C", ",");
        url = url.replaceAll("%3A", ":");
        return url;
    }

    protected String getHtml() {
        try {
            URL url = new URL("http://www.youtube.com/watch?v=" + videoId);
            BufferedInputStream bis = new BufferedInputStream(url.openStream());
            StringBuffer sb = new StringBuffer();
            byte[] buffer = new byte[512];
            int r = bis.read(buffer);
            while (r != -1) {
                sb.append(new String(buffer));
                r = bis.read(buffer);
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
