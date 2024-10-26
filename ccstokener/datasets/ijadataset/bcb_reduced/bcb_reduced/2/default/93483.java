import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class NewsRegion {

    private String feedUrl;

    private Color regionColor;

    private float score;

    private float normalizedScore;

    private String name = "";

    public static float NORMALIZE_MAX = 1.0f;

    int sc = 0;

    static HashMap goodWords = mkHt("good_words.txt");

    static HashMap badWords = mkHt("bad_words.txt");

    public int getStoryCount() {
        return sc;
    }

    public String getName() {
        return name;
    }

    public NewsRegion(String name, String feedUrl, Color regionColor) {
        this.name = name;
        this.feedUrl = feedUrl;
        this.regionColor = regionColor;
    }

    private String downloadFeed(String feedUrl2) {
        String data = "";
        System.out.print("\t[" + feedUrl2 + "]");
        try {
            final URL url = new URL(feedUrl2);
            URLConnection conn = url.openConnection();
            InputStream in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            long l = 0;
            long len = 0;
            while ((l = in.read(buffer)) != -1) {
                len += l;
                data += new String(buffer);
            }
            System.out.print("\t[ " + (len / 1024) + " kB ]");
        } catch (Exception e) {
            System.err.println("\n\t" + e.getMessage());
        }
        String dataClean = "";
        final String start = "<description>";
        final String end = "</description>";
        data = data.toLowerCase();
        int startpos = data.indexOf(start);
        int c = 0;
        while (startpos != -1) {
            int endpos = data.indexOf(end, startpos);
            if (endpos == -1) break;
            c++;
            dataClean += data.substring(startpos + start.length(), endpos) + " ";
            startpos = data.indexOf(start, endpos);
        }
        System.out.print("\t[ " + (c) + " stories ]");
        sc = c;
        System.out.println("");
        return dataClean;
    }

    private static HashMap mkHt(String f) {
        BufferedReader input = null;
        HashMap ret = new HashMap();
        try {
            input = new BufferedReader(new FileReader(f));
            String line = null;
            while ((line = input.readLine()) != null) {
                if (line.length() > 2) {
                    ret.put(line.toLowerCase(), null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void generateScore() {
        String data = "";
        System.out.println("aggregating " + name);
        String[] urls = feedUrl.split(" ");
        for (int i = 0; i < urls.length; i++) {
            data += downloadFeed(urls[i]);
        }
        StringTokenizer tok = new StringTokenizer(data, " ,!.?-();:\"");
        float tscore = 0;
        while (tok.hasMoreTokens()) {
            String word = tok.nextToken().toLowerCase().trim();
            if (goodWords.containsKey(word)) tscore += 1.0f; else if (badWords.containsKey(word)) tscore -= 1.0f;
        }
        this.score = tscore / sc;
        this.normalizedScore = 0.0f;
    }

    public void update() {
        generateScore();
    }

    public float getScore() {
        return score;
    }

    public float getNormalizedScore() {
        return this.normalizedScore;
    }

    public Color getRegionColor() {
        return regionColor;
    }

    public void normalize(float min, float max) {
        float t;
        float s = this.score;
        if (s > 0) {
            if (max == 0) t = 0.0f; else t = ((float) s / (float) max) * NORMALIZE_MAX;
        } else {
            if (min == 0) t = 0.0f; else t = -((float) s / (float) min) * NORMALIZE_MAX;
        }
        this.normalizedScore = t;
    }
}
