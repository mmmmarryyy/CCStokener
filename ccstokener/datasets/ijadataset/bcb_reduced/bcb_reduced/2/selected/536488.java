package sound;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import javax.swing.JButton;
import javax.swing.JLabel;
import util.GetSoundInterface;

/**
 *
 * @author reza
 */
public class GetSound implements GetSoundInterface {

    private String Word;

    private File f;

    private boolean stopped;

    JButton button;

    Thread thisthread;

    private JLabel label;

    /** Creates a new instance of GetSound */
    public GetSound() {
    }

    public void setWord(String Word) {
        this.Word = Word;
    }

    private boolean getWave(String url) {
        BufferedOutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            File FF = new File(f.getParent() + "/" + f.getName() + "pron");
            FF.mkdir();
            URL url2 = new URL(url);
            out = new BufferedOutputStream(new FileOutputStream(f.getParent() + "/" + f.getName() + "pron/" + Word + ".wav"));
            conn = url2.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.flush();
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
        return true;
    }

    public void run() {
        String s, s2;
        s = "";
        s2 = "";
        try {
            URL url = new URL("http://www.m-w.com/dictionary/" + Word);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            while (((str = in.readLine()) != null) && (!stopped)) {
                s = s + str;
            }
            in.close();
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        Pattern pattern = Pattern.compile("popWin\\('/cgi-bin/(.+?)'", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(s);
        if ((!stopped) && (matcher.find())) {
            String newurl = "http://m-w.com/cgi-bin/" + matcher.group(1);
            try {
                URL url2 = new URL(newurl);
                BufferedReader in2 = new BufferedReader(new InputStreamReader(url2.openStream()));
                String str;
                while (((str = in2.readLine()) != null) && (!stopped)) {
                    s2 = s2 + str;
                }
                in2.close();
            } catch (MalformedURLException e) {
            } catch (IOException e) {
            }
            Pattern pattern2 = Pattern.compile("<A HREF=\"http://(.+?)\">Click here to listen with your default audio player", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher matcher2 = pattern2.matcher(s2);
            if ((!stopped) && (matcher2.find())) {
                if (getWave("http://" + matcher2.group(1))) label.setEnabled(true);
            }
        }
        button.setEnabled(true);
    }

    public void stop() {
        stopped = true;
        thisthread = null;
    }

    public void start() {
        thisthread = new Thread(this);
        stopped = false;
        button.setEnabled(false);
        label.setEnabled(false);
        thisthread.start();
    }

    public void init(File f, String word, JButton button, JLabel label) {
        Word = word;
        this.f = f;
        this.button = button;
        this.label = label;
    }

    public String toString() {
        return "Merriam-Webster";
    }
}
