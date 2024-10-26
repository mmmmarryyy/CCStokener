package com.arcucomp.util;

import java.io.*;
import java.net.URL;
import java.util.Vector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

public class FileToStringLoader {

    String strout;

    public FileToStringLoader(String filename) throws IOException {
        try {
            boolean isHttp = false;
            boolean isFttp = false;
            char pathChar = File.separatorChar;
            int httpExp = filename.indexOf("http:///");
            int fileExp = filename.indexOf("file:///");
            int httpExp2 = filename.indexOf("http://");
            int fileExp2 = filename.indexOf("file://");
            int httpExp3 = filename.indexOf("http:/");
            int fileExp3 = filename.indexOf("file:/");
            if (-1 != httpExp || -1 != httpExp2 || -1 != httpExp3) isHttp = true;
            if (-1 != fileExp || -1 != fileExp2 || -1 != fileExp3) isFttp = true;
            if (!isHttp && !isFttp) {
                if (pathChar == '\\') {
                    filename.replace("//", "\\");
                    filename.replace("/", "\\");
                } else if (pathChar == '/') {
                    filename.replace("\\\\", "/");
                    filename.replace("\\", "/");
                }
                strout = this.loadFile(filename);
            } else {
                boolean fixURL = false;
                if (-1 == httpExp && -1 == httpExp2 && -1 != httpExp3) fixURL = true;
                if (-1 == fileExp && -1 == fileExp2 && -1 != fileExp3) fixURL = true;
                if (fixURL) {
                    filename = filename.replaceFirst(":/", "://");
                }
                URL u = new URL(filename);
                if (isHttp) {
                    strout = this.loadURL(u);
                } else {
                    strout = this.loadFile(u.getFile());
                }
            }
        } catch (Exception ex) {
            System.out.println(ex);
            strout = "File load failure     : " + ex.getMessage();
        }
    }

    public String getString() {
        return strout;
    }

    public Vector getFileList(String dir) {
        Vector v = new Vector();
        File f = new File(dir);
        File[] fileList = f.listFiles();
        int i = 0;
        while (i < fileList.length) {
            File file = fileList[i];
            v.addElement(file.getAbsoluteFile());
            i++;
        }
        return v;
    }

    public static void main(String[] args) throws IOException {
        String dir = "";
        if (args.length != 1) {
            dir = ".";
        } else {
            dir = args[0];
        }
        try {
            FileToStringLoader f = new FileToStringLoader(dir);
            Vector v = f.getFileList(".");
            System.out.println(v);
        } catch (IOException e) {
        }
    }

    public String loadURL(URL url) {
        String retVal = "";
        try {
            InputStream inputStream = url.openStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = bufferedReader.readLine();
            retVal += line + "\n";
            while (line != null) {
                System.out.println(line);
                line = bufferedReader.readLine();
                if (line != null) retVal += line + "\n";
            }
            bufferedReader.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            retVal = e.getMessage();
        } catch (IOException e) {
            e.printStackTrace();
            retVal = e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            retVal = e.getMessage();
        }
        return retVal;
    }

    public String loadFile(String fileName) {
        String retVal = "";
        try {
            File f = new File(fileName);
            int size = (int) f.length();
            int bytes_read = 0;
            FileInputStream in = new FileInputStream(f);
            byte[] data = new byte[size];
            while (bytes_read < size) {
                bytes_read += in.read(data, bytes_read, size - bytes_read);
            }
            retVal = new String(data);
        } catch (Exception ex) {
            System.out.println(ex);
            retVal = null;
        }
        return retVal;
    }
}
