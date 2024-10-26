package com.yict.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * <h3>Class name</h3> 文件操作类 <h4>Description</h4>
 * 
 * <h4>Special Notes</h4>
 * 
 * 
 * @ver 0.1
 * @author Jay.Wu 2008-11-21
 * 
 */
public class FileUtil {

    protected static final transient Log log = LogFactory.getLog(FileUtil.class);

    private BufferedWriter out;

    private BufferedReader in;

    private InputStream ins;

    private OutputStream outs;

    private String path;

    private File file;

    private String fileName;

    /**
	 * 缓冲区大小
	 */
    private static int BUFFER_SIZE = 8096;

    public FileUtil() {
    }

    public static String getCurrentPath() {
        String path = FileUtil.class.getClassLoader().getResource("").toString().substring(6);
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (Exception e) {
            log.error("", e);
            path = null;
        }
        return path;
    }

    public FileUtil(File file) {
        this.file = file;
        this.path = file.getAbsolutePath();
        this.fileName = file.getName();
    }

    public FileUtil(String path, String fileName) {
        this.path = path;
        this.fileName = fileName;
        this.file = new File(path + fileName);
    }

    public void createFile() throws Exception {
        file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(path + fileName);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
    }

    /**
	 * 读取流内容
	 * 
	 * @param inputStream
	 * @return
	 * @throws Exception
	 * @author Jay.Wu
	 */
    public String read(InputStream inputStream) {
        try {
            in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuffer result = new StringBuffer("");
            String str;
            while ((str = in.readLine()) != null) {
                result.append(str + "\n");
            }
            return result.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    public void append(String content) throws Exception {
        String oldStr = read();
        close();
        if (out == null) {
            log.debug("init BufferedWriter in path：" + path + fileName);
            file = new File(path + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path + fileName), "UTF-8"));
        }
        out.write(oldStr + content);
    }

    /**
	 * 写入文件
	 * 
	 * @param content
	 * @throws Exception
	 * @author Jay.Wu
	 */
    public void write(String content) throws Exception {
        if (content == null) {
            content = "";
        }
        if (out == null) {
            log.debug("init BufferedWriter in path：" + path + fileName);
            createFile();
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path + fileName), "UTF-8"));
        }
        out.write(content);
    }

    /**
	 * 写文件完成后关闭
	 * 
	 * @author Jay.Wu
	 */
    public void close() {
        log.debug("Close FileUtil");
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                log.error("", e);
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                log.error("", e);
            }
        }
        if (ins != null) {
            try {
                ins.close();
            } catch (IOException e) {
                log.error("", e);
            }
        }
        if (outs != null) {
            try {
                outs.close();
            } catch (IOException e) {
                log.error("", e);
            }
        }
    }

    /**
	 * 将对象序列化到文件
	 * 
	 * @param obj
	 * @throws Exception
	 * @throws IOException
	 * @author Jay.Wu
	 */
    public void wirteObj(Object obj) throws Exception {
        if (out == null) {
            log.debug("init ObjectOutputStream in path：" + path + fileName);
            createFile();
            outs = new ObjectOutputStream(new FileOutputStream(path + fileName));
        }
        ((ObjectOutputStream) outs).writeObject(obj);
    }

    public void readObj(Object[] dataList) throws Exception {
        if (ins == null) {
            log.debug("init ObjectInputStream in path：" + path);
            ins = new ObjectInputStream(new FileInputStream(path + fileName));
        }
        if (dataList != null) {
            for (int i = 0; i < dataList.length; i++) {
                dataList[i] = ((ObjectInputStream) ins).readObject();
            }
        }
    }

    public String read() {
        if (ins == null) {
            log.debug("init InputStream in path：" + path + fileName);
            file = new File(path + fileName);
            if (file.exists()) {
                try {
                    ins = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    log.error(e.getMessage(), e);
                    return "";
                }
            } else {
                return "";
            }
        }
        return read(ins);
    }

    /**
	 * 将HTTP资源另存为文件
	 * 
	 * @param destUrl
	 * @throws IOException
	 * @author Jay.Wu
	 * @throws Exception
	 */
    public void saveToFile(String destUrl) throws Exception {
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        HttpURLConnection httpUrl = null;
        URL url = null;
        byte[] buf = new byte[BUFFER_SIZE];
        int size = 0;
        createFile();
        url = new URL(destUrl);
        httpUrl = (HttpURLConnection) url.openConnection();
        httpUrl.connect();
        bis = new BufferedInputStream(httpUrl.getInputStream());
        fos = new FileOutputStream(path + fileName);
        log.debug("destUrl：[" + destUrl + "]， save to ：[" + path + fileName + "]");
        while ((size = bis.read(buf)) != -1) fos.write(buf, 0, size);
        fos.close();
        bis.close();
        httpUrl.disconnect();
        file = new File(path + fileName);
    }

    /**
	 * 删除文件
	 * 
	 * @author Sagax.Luo
	 * @throws Exception
	 */
    public void delete() throws Exception {
        String fileUrl = path + fileName;
        File dFile = new File(fileUrl);
        try {
            delFile(dFile);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
	 * 删除文件夹或者文件
	 * 
	 * @param file
	 * @author Jay.Wu
	 */
    public void delFile(File file) {
        if (file.isDirectory() && file.listFiles().length > 0) {
            log.debug("directory=" + file.getPath());
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                delFile(files[i]);
            }
        } else {
            log.debug("delete file=" + file.getPath());
            file.delete();
        }
    }

    /**
	 * 复制文件/文件夹
	 * 
	 * @param sourceFilePath
	 * @param aimFilePath
	 * @author Jay.Wu
	 */
    public static void copyFiles(String sourceFilePath, String aimFilePath) {
        copyFiles(new File(sourceFilePath), new File(aimFilePath));
    }

    /**
	 * 复制文件/文件夹
	 * 
	 * @param sourceFile
	 * @param aimFile
	 * @author Jay.Wu
	 */
    public static void copyFiles(File sourceFile, File aimFile) {
        log.debug("copyFiles start: " + sourceFile + " to " + aimFile);
        try {
            if (!aimFile.exists()) {
                aimFile.mkdirs();
                if (!aimFile.isDirectory()) {
                    throw new Exception("拷贝文件时出错，目标文件必须是目录");
                }
            }
            if (sourceFile.isFile()) {
                copyFile(sourceFile, aimFile);
            } else {
                File list[] = sourceFile.listFiles();
                for (int i = 0; i < list.length; i++) {
                    if (list[i].isFile()) {
                        copyFile(list[i], aimFile);
                    } else {
                        copyFiles(list[i], new File(aimFile, list[i].getName()));
                    }
                }
            }
        } catch (Exception e) {
            log.error("", e);
        }
        log.debug("copyFiles end: " + sourceFile + " to " + aimFile);
    }

    /**
	 * 复制文件
	 * 
	 * @param sourceFile
	 * @param aimFile
	 * @return
	 * @author Jay.Wu
	 */
    private static boolean copyFile(File sourceFile, File aimFile) {
        try {
            byte[] buffer = new byte[4096];
            InputStream in = new BufferedInputStream(new FileInputStream(sourceFile));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(aimFile, sourceFile.getName())));
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();
            return true;
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public static int getSize(String filePath) {
        return getSize(new File(filePath));
    }

    public static int getSize(File file) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
            return fis.available();
        } catch (Exception e) {
            return 0;
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            FileInputStream fis = new FileInputStream("C:\\Users\\songlin.li\\Downloads\\初始数据\\truckmanage.txt");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader buf = new BufferedReader(isr);
            String c;
            try {
                int i = 0;
                while ((c = buf.readLine()) != null) {
                    String[] st = c.split("\\|");
                    System.out.println(st[st.length - 1]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public BufferedWriter getOut() {
        return out;
    }

    public void setOut(BufferedWriter out) {
        this.out = out;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        out = null;
        this.path = path;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
