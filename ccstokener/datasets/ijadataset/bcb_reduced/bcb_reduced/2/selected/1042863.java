package com.google.code.ihtika.IhtikaClient.Net.Downloads;

import com.google.code.ihtika.IhtikaClient.Vars.Ini;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 *
 * @author Arthur Khusnutdinov
 */
public class DownloadFile {

    public String download(String urlString, JProgressBar progressBar, String outFileName) {
        String fileName = "";
        int count;
        try {
            URL url = new URL(urlString);
            URLConnection conexion = url.openConnection();
            conexion.connect();
            int lenghtOfFile = conexion.getContentLength();
            if (outFileName != null) {
                fileName = outFileName;
            } else {
                fileName = urlString.substring(urlString.lastIndexOf("/") + 1);
            }
            File outputDir = new File("tmp");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            InputStream input = new BufferedInputStream(url.openStream());
            OutputStream output = new FileOutputStream("tmp/" + fileName);
            byte data[] = new byte[1024];
            long total = 0;
            int countProgress = 0;
            while ((count = input.read(data)) != -1) {
                total += count;
                if (progressBar != null) {
                    countProgress = (int) (total * 100 / lenghtOfFile);
                    SwingUtilities.invokeLater(new UpdateProgressBarTask(progressBar, countProgress));
                    progressBar.repaint();
                }
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();
            if (progressBar != null) {
                SwingUtilities.invokeLater(new UpdateProgressBarTask(progressBar, 0));
                progressBar.repaint();
            }
        } catch (Exception ex) {
            Ini.logger.fatal("Error: ", ex);
            return null;
        }
        return fileName;
    }

    private class UpdateProgressBarTask implements Runnable {

        private JProgressBar progressBar;

        private int value;

        public UpdateProgressBarTask(JProgressBar progressBar, int value) {
            this.value = value;
            this.progressBar = progressBar;
        }

        @Override
        public void run() {
            if (this.progressBar != null) {
                this.progressBar.setValue(value);
            }
        }
    }
}
