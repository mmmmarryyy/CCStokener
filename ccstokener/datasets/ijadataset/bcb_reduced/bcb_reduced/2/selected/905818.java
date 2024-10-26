package org.sfilo.downloader.http;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.logging.Logger;
import org.sfilo.downloader.FrameFileSpider;
import org.sfilo.downloader.SpiderEvent;

public class HttpFrameFileSpider extends Observable implements FrameFileSpider, Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = -2183761865643390562L;

    transient java.util.logging.Logger logger = Logger.getLogger(HttpFrameFileSpider.class.getName());

    private boolean pauseFlag = false;

    private boolean stopFlag = false;

    private int bufferSize = 1024;

    private String destUrl, destFileName;

    private int startPosition, length;

    Map<String, String> headers;

    private int currPosition = 0;

    private int retryTime = 99;

    private long idleTime = 500;

    public HttpFrameFileSpider(String destUrl, int startPosition, int length, String destFileName, Map<String, String> headers) throws Exception {
        this.destUrl = destUrl;
        this.startPosition = startPosition;
        this.destFileName = destFileName;
        this.length = length;
        this.headers = headers;
    }

    public HttpFrameFileSpider(String destUrl, int startPosition, int length, String destFileName) throws Exception {
        this(destUrl, startPosition, length, destFileName, null);
    }

    private Logger getLogger() {
        if (logger == null) logger = Logger.getLogger(HttpFrameFileSpider.class.getName());
        return logger;
    }

    @Override
    public void pause() throws Exception {
        pauseFlag = true;
        getLogger().info("������ͣ");
        setChanged();
        notifyObservers(SpiderEvent.DOWNLOADPAUSE);
    }

    @Override
    public void scratch() throws Exception {
        getLogger().info("bufferSize=" + bufferSize);
        byte[] temp = new byte[bufferSize];
        URL url = new URL(destUrl);
        HttpURLConnection httpUrl = null;
        BufferedInputStream bis = null;
        RandomAccessFile file = null;
        for (int i = 0; i < retryTime; i++) {
            getLogger().info("���Ե�" + i + "��");
            try {
                httpUrl = (HttpURLConnection) url.openConnection();
                file = new RandomAccessFile(new File(destFileName), "rw");
                MappedByteBuffer buffer = file.getChannel().map(FileChannel.MapMode.READ_WRITE, startPosition, length);
                if (currPosition == 0) httpUrl.setRequestProperty("Range", "bytes=" + startPosition + "-" + (startPosition + length - 1)); else {
                    getLogger().info(String.valueOf("currposition=" + currPosition));
                    httpUrl.setRequestProperty("Range", "bytes=" + currPosition + "-" + (length - currPosition - 1));
                    buffer.position(currPosition);
                }
                if (headers != null) {
                    Iterator<String> itor = headers.keySet().iterator();
                    while (itor.hasNext()) {
                        String key = itor.next();
                        httpUrl.setRequestProperty(key, headers.get(key));
                    }
                }
                bis = new BufferedInputStream(httpUrl.getInputStream());
                String fileSizeStr = httpUrl.getHeaderField("Content-Length");
                getLogger().info("�ļ�����=" + fileSizeStr);
                int count = 0;
                while (!stopFlag) {
                    if (pauseFlag) {
                        continue;
                    }
                    if ((count = bis.read(temp)) != -1) {
                        buffer.put(temp, 0, count);
                        currPosition = currPosition + count;
                        setChanged();
                        notifyObservers(SpiderEvent.DOWNLOADPROGRESS);
                    } else break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                wait(idleTime);
                continue;
            } finally {
                if (httpUrl != null) httpUrl.disconnect();
                if (bis != null) bis.close();
                if (file != null) file.close();
            }
            break;
        }
        getLogger().info("Ƭ���������");
        setChanged();
        notifyObservers(SpiderEvent.DOWNLOADCOMPLETE);
    }

    @Override
    public void stop() throws Exception {
        stopFlag = true;
        setChanged();
        notifyObservers(SpiderEvent.DOWNLOADSTOP);
    }

    @Override
    public void resume() throws Exception {
        pauseFlag = false;
        getLogger().info("���ػָ�");
        setChanged();
        notifyObservers(SpiderEvent.DOWNLOADRESUME);
    }

    @Override
    public void reload() throws Exception {
        stopFlag = false;
        retryTime = 0;
    }
}
