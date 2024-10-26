package com.fanfq;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.content.Context;
import android.util.Log;

public class FileDownloader {

    private Context context;

    private FileService fileService;

    private static final String TAG = "FileDownloader";

    private int downloadSize = 0;

    private int fileSize = 0;

    private DownloadThread[] threads;

    private URL url;

    private File saveFile;

    private File fileSaveDir;

    private File logFile;

    private Map<Integer, Integer> data = new ConcurrentHashMap<Integer, Integer>();

    private int block;

    private String downloadUrl;

    /**
	 * ��ȡ�߳���
	 * @return
	 */
    public int getThreadSize() {
        return threads.length;
    }

    /**
	 * ��ȡ�ļ���С
	 * @return
	 */
    public int getFileSize() {
        return fileSize;
    }

    /**
	 * �ۼ������صĴ�С
	 * @param size
	 */
    protected synchronized void append(int size) {
        downloadSize += size;
    }

    /**
	 * ����ָ���߳�������ص�λ��
	 * @param threadId
	 * @param pos
	 */
    protected void update(int threadId, int pos) {
        this.data.put(threadId, pos);
    }

    /**
	 * �����¼�ļ�
	 */
    protected synchronized void saveLogFile() {
        this.fileService.update(this.downloadUrl, this.data);
    }

    /**
	 * �����ļ�������
	 * @param downloadUrl ����·��
	 * @param fileSaveDir �ļ�����Ŀ¼
	 * @param threadNum �����߳���
	 */
    public FileDownloader(Context context, String downloadUrl, File fileSaveDir, int threadNum) {
        try {
            this.context = context;
            this.downloadUrl = downloadUrl;
            this.fileService = new FileService(context);
            this.url = new URL(downloadUrl);
            this.threads = new DownloadThread[threadNum];
            this.fileSaveDir = fileSaveDir;
            if (!fileSaveDir.exists()) {
                fileSaveDir.mkdir();
            }
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(6 * 1000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
            conn.setRequestProperty("Accept-Language", "zh-CN");
            conn.setRequestProperty("Referer", downloadUrl);
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.connect();
            printResponseHeader(conn);
            if (conn.getResponseCode() == 200) {
                this.fileSize = conn.getContentLength();
                if (this.fileSize <= 0) throw new RuntimeException("�޷���֪�ļ���С");
                String fileName = getFileName(conn);
                this.saveFile = new File(fileSaveDir, fileName);
                Map<Integer, Integer> logdata = fileService.getData(downloadUrl);
                if (logdata.size() > 0) {
                    data.putAll(logdata);
                }
                this.block = this.fileSize / this.threads.length + 1;
                if (this.data.size() == this.threads.length) {
                    for (int i = 0; i < this.threads.length; i++) {
                        this.downloadSize += this.data.get(i + 1) - (this.block * i);
                    }
                    print("�Ѿ����صĳ��ȣ�" + this.downloadSize);
                }
            } else {
                print("��������Ӧ����");
                throw new RuntimeException("��������Ӧ����");
            }
        } catch (Exception e) {
            print("���Ӳ�������·��" + e.toString());
            throw new RuntimeException("���Ӳ�������·��");
        }
    }

    /**
	 * ��ȡ�ļ���
	 * 
	 */
    private String getFileName(HttpURLConnection conn) {
        String fileName = this.url.toString().substring(this.url.toString().lastIndexOf('/') + 1);
        if (fileName == null || "".equals(fileName.trim())) {
            for (int i = 0; ; i++) {
                String mine = conn.getHeaderField(i);
                if (mine == null) break;
                if ("content-disposition".equals(conn.getHeaderFieldKey(i).toLowerCase())) {
                    Matcher m = Pattern.compile(".*filename=(.*)").matcher(mine.toLowerCase());
                    if (m.find()) return m.group(1);
                }
            }
            fileName = UUID.randomUUID() + ".tmp";
        }
        return getFileNumber(fileName);
    }

    /**
	 * ������ļ����Ƿ���ļ��Դ���
	 * @return fileNumber 0 ��ʾ���ļ������ڣ����򷵻ظ��ļ��Ѵ��ڵĸ��� test.xml test(1).xml
	 * @author fangqing.fan@gmail.com
	 * �÷����������ƣ��ļ����������ж��"."�����ļ�û����չ����Ҫ�����ٴ���һ��
	 */
    @SuppressWarnings("finally")
    private String getFileNumber(String fileName) {
        String result = fileName;
        try {
            File file[] = this.fileSaveDir.listFiles();
            String[] resFileName = fileName.split("\\.");
            String fileNameTmp = null;
            int fileNumber = 0;
            for (int i = 0; i < file.length; i++) {
                if (file[i].isFile()) {
                    fileNameTmp = file[i].getAbsolutePath().toString();
                    fileNameTmp = fileNameTmp.substring(fileNameTmp.lastIndexOf('/') + 1);
                    String[] distFileName = fileNameTmp.split("\\.");
                    if (distFileName[0].startsWith(resFileName[0]) && distFileName[1].equals(resFileName[1])) {
                        fileNumber++;
                    }
                }
            }
            if (fileNumber != 0) {
                result = resFileName[0] + "(" + fileNumber + ")." + resFileName[1];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            result = fileName;
            print("���ļ�û����չ��");
        } finally {
            return result;
        }
    }

    /**
	 * ��ʼ�����ļ�
	 * @param listener �������������ı仯�������Ҫ�˽�ʵʱ���ص���������������Ϊnull
	 * @param return �������ļ��Ĵ�С
	 * @throws Exceptioin
	 */
    public int download(DownloadProgressListener listener) throws Exception {
        try {
            if (this.data.size() != this.threads.length) {
                this.data.clear();
                for (int i = 0; i < this.threads.length; i++) {
                    this.data.put(i + 1, this.block * i);
                }
            }
            for (int i = 0; i < this.threads.length; i++) {
                int downLength = this.data.get(i + 1) - (this.block * i);
                if (downLength < this.block && this.data.get(i + 1) < this.fileSize) {
                    RandomAccessFile randOut = new RandomAccessFile(this.saveFile, "rw");
                    if (this.fileSize > 0) {
                        randOut.setLength(this.fileSize);
                    }
                    randOut.seek(this.data.get(i + 1));
                    this.threads[i] = new DownloadThread(this, this.url, randOut, this.block, this.data.get(i + 1), this.data.get(i + 1) + this.block, i + 1);
                    this.threads[i].setPriority(7);
                    this.threads[i].start();
                } else {
                    this.threads[i] = null;
                }
            }
            this.fileService.save(this.downloadUrl, this.data);
            boolean notFinish = true;
            while (notFinish) {
                Thread.sleep(900);
                notFinish = false;
                for (int i = 0; i < this.threads.length; i++) {
                    if (this.threads[i] != null && !this.threads[i].isFinish()) {
                        notFinish = true;
                        if (this.threads[i].getDownLength() == -1) {
                            RandomAccessFile randOut = new RandomAccessFile(this.saveFile, "rw");
                            randOut.seek(this.data.get(i + 1));
                            this.threads[i] = new DownloadThread(this, this.url, randOut, this.block, this.data.get(i + 1), this.data.get(i + 2) - 1, i + 1);
                            this.threads[i].setPriority(7);
                            this.threads[i].start();
                        }
                    }
                }
                if (listener != null) {
                    listener.onDownloadSize(this.downloadSize);
                }
            }
            fileService.delete(this.downloadUrl);
        } catch (Exception e) {
            print("����ʧ��" + e.toString());
            throw new Exception("����ʧ��");
        }
        return this.downloadSize;
    }

    /**
	 * ��ȡHttp��Ӧͷ�ֶ�
	 * @param HttpURLConnection
	 * @return
	 */
    public static Map<String, String> getHttpResponseHeader(HttpURLConnection conn) {
        Map<String, String> header = new LinkedHashMap<String, String>();
        for (int i = 0; ; i++) {
            String mine = conn.getHeaderField(i);
            if (mine == null) break;
            header.put(conn.getHeaderFieldKey(i), mine);
        }
        return header;
    }

    /**
	 * ��ӡhttpͷ
	 * @param HttpURLConnection
	 */
    public static void printResponseHeader(HttpURLConnection conn) {
        Map<String, String> header = getHttpResponseHeader(conn);
        for (Map.Entry<String, String> entry : header.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey() + ":" : "";
            print(key + entry.getValue());
        }
    }

    private static void print(String msg) {
        System.out.println(TAG + " " + msg);
    }
}
