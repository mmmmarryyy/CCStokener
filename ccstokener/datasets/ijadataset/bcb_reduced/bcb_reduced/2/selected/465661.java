package edu.jlu.fuliang;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadThread extends Thread {

    private String savePath;

    private DownloadInfo downloadInfo;

    private static DownloadTableModel downloadTableModel = DownloadTableModel.getInstance();

    private int index;

    public DownloadThread() {
    }

    public void run() {
        URL downloadUrl = parserRealDownloadUrl();
        doDownload(downloadUrl);
    }

    private void doDownload(URL downloadUrl) {
        downloadTableModel.incRowCount();
        downloadTableModel.setValueAt(downloadInfo.getSongName(), index, 0);
        downloadTableModel.setValueAt(downloadInfo.getAblum(), index, 1);
        downloadTableModel.setValueAt(downloadInfo.getSonger(), index, 2);
        try {
            HttpURLConnection connection = (HttpURLConnection) downloadUrl.openConnection();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedInputStream bis = new BufferedInputStream(connection.getInputStream());
                connection.setReadTimeout(60000);
                long totalSize = connection.getContentLength();
                long downloadedSize = 0;
                int downloaded = 0;
                int speed = 0;
                String url = downloadUrl.toString();
                savePath += url.substring(url.lastIndexOf("/"));
                File file = new File(savePath);
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                byte[] buffer = new byte[2048];
                int len = 0;
                long previousTime, currentTime;
                downloadTableModel.setValueAt((float) totalSize / 1024 + "kb", index, 3);
                while (true) {
                    previousTime = System.nanoTime();
                    if ((len = bis.read(buffer)) == -1) break;
                    bos.write(buffer, 0, len);
                    downloadedSize += len;
                    downloaded = (int) (100 * downloadedSize / totalSize);
                    currentTime = System.nanoTime();
                    speed = (int) (len * 1000 / (currentTime - previousTime));
                    downloadTableModel.setValueAt(downloaded + "%", index, 4);
                    downloadTableModel.setValueAt(speed + "kb/s", index, 5);
                }
                bos.close();
                bis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private URL parserRealDownloadUrl() {
        String link = downloadInfo.getLink();
        link = link.replaceAll(" ", "%20");
        URL parserRealDownloadUrl = null;
        String urlStr = null;
        try {
            URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                byte[] buffer = new byte[2048];
                String content = null;
                int len = 0;
                Pattern purl = Pattern.compile("href=\"(.*)\"");
                while ((len = inputStream.read(buffer)) != -1) {
                    content = new String(buffer, 0, len);
                    Matcher murl = purl.matcher(content);
                    if (murl.find()) {
                        urlStr = murl.group(1);
                        break;
                    }
                }
                inputStream.close();
            }
            parserRealDownloadUrl = new URL(urlStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return parserRealDownloadUrl;
    }

    public void setDownloadInfo(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
