package jvc.util;

import java.io.*;
import java.util.*;
import java.net.*;
import jvc.util.io.FileNameFilter;

public class FileUtils {

    /**
	   * ���������ļ��У�
	   * @param in
	   * @param file2��Ŀ���ļ�
	   * @param isReplace�����Ŀ���ļ����ڣ�true��ʾ����Ŀ���ļ������򲻸���
	   * @return�������ɹ�����true,���Ŀ���ļ����ڲ���û�и���Ŀ���ļ�Ҳ����false.
	   * @throws IOException
	   */
    public static String getFileName(String filePathName) {
        String str = filePathName.replaceAll("\\\\", "/");
        str = str.substring(str.lastIndexOf("/") + 1, str.length());
        return str;
    }

    public static String getExtName(String filePathName) {
        String str = filePathName;
        str = str.substring(str.lastIndexOf(".") + 1, str.length());
        return str;
    }

    public static boolean saveFile(InputStream in, File file2, boolean isReplace) throws IOException {
        if (file2.exists()) {
            if (!isReplace) {
                return false;
            }
        } else {
            touch(file2);
        }
        OutputStream out = new FileOutputStream(file2);
        byte[] data = new byte[in.available()];
        in.read(data);
        in.close();
        out.write(data);
        out.flush();
        in.close();
        out.close();
        return true;
    }

    private static Vector expandFileList(String dir) {
        return expandFileList(new String[] { dir }, false, "");
    }

    public static Vector expandFileList(String dir, boolean inclDirs) {
        return expandFileList(new String[] { dir }, inclDirs, "");
    }

    public static Vector expandFileList(String dir, boolean inclDirs, String filter) {
        return expandFileList(new String[] { dir }, inclDirs, filter);
    }

    private static Vector expandFileList(String[] files, boolean inclDirs, String filter) {
        Vector v = new Vector();
        if (files == null) return v;
        for (int i = 0; i < files.length; i++) v.add(new File(files[i]));
        for (int i = 0; i < v.size(); i++) {
            File f = (File) v.get(i);
            if (f.isDirectory()) {
                File[] fs;
                if (filter.equals("")) fs = f.listFiles(); else fs = f.listFiles(new FileNameFilter(filter));
                for (int n = 0; n < fs.length; n++) v.add(fs[n]);
                if (!inclDirs) {
                    v.remove(i);
                    i--;
                }
            }
        }
        return v;
    }

    public static boolean copyDir(String SourcePath, String TargetPath) {
        Vector v = expandFileList(SourcePath);
        try {
            for (int i = 0; i < v.size(); i++) {
                File f_old = (File) v.get(i);
                if (!TargetPath.endsWith(File.separator)) TargetPath += File.separator;
                saveFile(f_old, new File(TargetPath + f_old.getAbsolutePath().substring(new File(SourcePath).getAbsolutePath().length())), true);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean copyFile(File file1, File file2, boolean isReplace) {
        try {
            return saveFile(file1, file2, isReplace);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
	   * �����ļ�������
	   * @param file1��Դ�ļ�
	   * @param file2��Ŀ���ļ�
	   * @param isReplace�����Ŀ���ļ����ڣ�true��ʾ����Ŀ���ļ������򲻸���
	   * @return�������ɹ�����true,���Ŀ���ļ����ڲ���û�и���Ŀ���ļ�Ҳ����false.
	   * @throws IOException
	   */
    public static boolean saveFile(File file1, File file2, boolean isReplace) throws IOException {
        InputStream in = new FileInputStream(file1);
        return saveFile(in, file2, isReplace);
    }

    /**
	   * �ƶ��ļ�������
	   * @param file1��Դ�ļ�
	   * @param file2��Ŀ���ļ�
	   * @param isReplace�����Ŀ���ļ����ڣ�true��ʾ����Ŀ���ļ������򲻸���
	   * @return�������ɹ�����true,���Ŀ���ļ����ڲ���û�и���Ŀ���ļ�Ҳ����false.
	   * @throws IOException
	   */
    public static boolean moveFile(File file1, File file2, boolean isReplace) throws IOException {
        boolean flag = saveFile(file1, file2, isReplace);
        if (!flag) {
            return flag;
        }
        file1.delete();
        return true;
    }

    public static void delete(String fileName) throws IOException {
        delete(new File(fileName));
    }

    /**
	   * ɾ���ļ������Ŀ¼��
	   * @param file���ļ���Ŀ¼
	   * @throws IOException
	   */
    public static void delete(File file) throws IOException {
        if (!file.exists()) return;
        if (file.isFile()) {
            file.delete();
        } else {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                delete(files[i]);
            }
            file.delete();
        }
    }

    /**
	   * ����һ���µĿ��ļ�,��ͬĿ¼�ṹ
	   * @param file
	   * @return ����ɹ����أ�����壬���򷵻أ�����
	   */
    public static boolean touch(File file) throws IOException {
        if (file.exists()) {
            return false;
        } else {
            file.getParentFile().mkdirs();
            file.createNewFile();
            return true;
        }
    }

    /**
	   * ����һ���µĿ��ļ�,��ͬĿ¼�ṹ
	   * @param sfile
	   * @return ����ɹ����أ�����壬���򷵻أ�����
	   */
    public static boolean touch(String sfile) throws IOException {
        File file = new File(sfile);
        if (file.exists()) {
            return false;
        } else {
            file.getParentFile().mkdirs();
            file.createNewFile();
            return true;
        }
    }

    /**
	   * ����Ŀ¼�����԰��¼���Ŀ¼
	   * @param file�����Ŀ¼
	   * @return���ɹ����أ�����壬���򷵻أ�����,�����Ĳ������ļ����򴴽����ļ��ĸ�Ŀ¼
	   * @throws IOException
	   */
    public static boolean mkdir(File file) throws IOException {
        File dir = file;
        if (dir.isFile()) {
            dir = dir.getParentFile();
        }
        return dir.mkdirs();
    }

    public static boolean saveNetFile2(String destUrl, String fileName) {
        try {
            saveNetFile(destUrl, fileName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	   * ��HTTP��Դ���Ϊ�ļ�
	   *
	   * @param destUrl String
	   * @param fileName String
	   * @throws Exception
	   */
    public static void saveNetFile(String destUrl, String fileName) throws IOException {
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        HttpURLConnection httpUrl = null;
        int BUFFER_SIZE = 2048;
        URL url = null;
        byte[] buf = new byte[BUFFER_SIZE];
        int size = 0;
        url = new URL(destUrl);
        httpUrl = (HttpURLConnection) url.openConnection();
        httpUrl.connect();
        bis = new BufferedInputStream(httpUrl.getInputStream());
        java.io.File dest = new java.io.File(fileName).getParentFile();
        if (!dest.exists()) dest.mkdirs();
        fos = new FileOutputStream(fileName);
        while ((size = bis.read(buf)) != -1) fos.write(buf, 0, size);
        fos.close();
        bis.close();
        httpUrl.disconnect();
    }

    public static String convertFileName(String strFileName) {
        String str = strFileName.replaceAll("%apppath%", jvc.util.AppUtils.AppPath);
        str = str.replaceAll("%webpath%", jvc.util.AppUtils.WebPath);
        return str;
    }

    /**  
     *  �ƶ��ļ��е�ָ��Ŀ¼  
     *  @param  oldPath  String  �磺c:/fqf  
     *  @param  newPath  String  �磺d:/fqf  
     */
    public static void moveDir(String oldPath, String newPath) throws IOException {
        if (oldPath.equals(newPath)) return;
        copyDir(oldPath, newPath);
        delete(oldPath);
    }

    public static boolean WriteFile(byte data[], String FileName) {
        try {
            OutputStream out = new FileOutputStream(FileName);
            out.write(data);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void DecodeDir(String sDir, boolean isSubDir) {
        File dir = new File(sDir);
        File[] fs = dir.listFiles();
        for (int i = 0; i < fs.length; i++) {
            if (fs[i].isDirectory()) {
                String newFileName = fs[i].getParent() + "/" + StringUtils.format(fs[i].getName(), "decode");
                fs[i].renameTo(new File(newFileName));
                if (isSubDir) DecodeDir(newFileName, isSubDir);
            }
        }
    }

    public static void ChangeHtmUPloadContent(String RootDir, String FileName) throws IOException {
        InputStream f_data = new FileInputStream(new File(FileName));
        byte[] data = new byte[f_data.available()];
        f_data.read(data);
        f_data.close();
        String Content = new String(data);
        if (Content.indexOf("UploadFile") > 0) {
            Content = Content.replaceAll("\"UploadFile/", "\"/UploadFile/");
            PrintWriter pw = new PrintWriter(new FileOutputStream(FileName, false));
            pw.println(Content);
            pw.close();
        }
    }

    public static void UpdateHtmUpload(String RootDir, String sDir, boolean isSubDir) throws IOException {
        File dir = new File(sDir);
        File[] fs = dir.listFiles();
        for (int i = 0; i < fs.length; i++) {
            if (fs[i].isDirectory()) {
                if (isSubDir) UpdateHtmUpload(RootDir, fs[i].getAbsolutePath(), isSubDir);
            } else if (fs[i].getName().endsWith("htm")) {
                ChangeHtmUPloadContent(RootDir, fs[i].getAbsolutePath());
            }
        }
    }

    public static void main(String[] args) throws IOException {
        DecodeDir("F:\\help\\kmsdata", true);
        UpdateHtmUpload("F:/help/kmsdata/", "F:/help/kmsdata", true);
    }
}
