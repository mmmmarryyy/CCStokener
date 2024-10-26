package util;

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.swing.JOptionPane;
import language.Language;
import ffmpeg.FFMpegProgressReceiver;
import gui.FFMpegGui;

/**
 * 
 * Objects of Downloader represent a file download to temp/video.flv
 * @author SebastianWe
 *
 */
public class Downloader extends Thread {

    private String file;

    private FFMpegProgressReceiver recv;

    private File f;

    private Language lang;

    /**
	 * Creates a new Downloader
	 * 
	 * @param file
	 * 				Web url
	 * @param recv
	 * 				State change receiver
	 * @param lang
	 * 				Language
	 */
    public Downloader(String file, FFMpegProgressReceiver recv, Language lang) {
        new File(System.getProperty("user.home") + "/.multiconvert/").mkdir();
        f = new File(System.getProperty("user.home") + "/.multiconvert/video.flv");
        this.file = file;
        this.recv = recv;
        this.lang = lang;
    }

    @Override
    public void run() {
        if (download() == null) JOptionPane.showMessageDialog((Component) recv, lang.downloadfailed, lang.error, JOptionPane.ERROR_MESSAGE); else if (recv instanceof FFMpegGui) ((FFMpegGui) recv).getSelInputFile().doClick();
    }

    /**
	 * Starts the download
	 * @return downloaded File, or null if an error occured
	 */
    public File download() {
        try {
            URL url = new URL(file);
            FileOutputStream os = new FileOutputStream(f);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            int responseCode = conn.getResponseCode();
            int i = 0;
            if (responseCode == HttpURLConnection.HTTP_OK) {
                byte tmp_buffer[] = new byte[4096];
                InputStream is = conn.getInputStream();
                int n;
                while ((n = is.read(tmp_buffer)) > 0) {
                    i++;
                    os.write(tmp_buffer, 0, n);
                    os.flush();
                    int val = (int) ((double) (i * 2860) / (double) conn.getContentLength() * 100);
                    if (recv != null && val < 100) recv.setProgress(val);
                }
                if (recv != null) recv.setProgress(104);
            }
            return f;
        } catch (Exception exc) {
            if (Constants.debug) exc.printStackTrace();
            return null;
        }
    }
}
