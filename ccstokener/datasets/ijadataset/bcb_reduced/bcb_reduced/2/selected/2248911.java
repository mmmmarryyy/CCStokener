package com.g2d.studio.io.http;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.cell.CIO;
import com.cell.CObject;
import com.cell.CUtil;
import com.cell.io.CFile;
import com.g2d.awt.util.*;
import com.cell.net.http.HttpPost;
import com.cell.net.http.HttpQuery;
import com.cell.net.http.HttpRequester;
import com.cell.net.http.HttpResponse;
import com.g2d.studio.Config;
import com.g2d.studio.io.File;
import com.g2d.studio.io.IO;

public class FileHttp implements IO {

    java.io.File local_root;

    String remote_root;

    public FileHttp(String[] args) {
        String g2d_root = CUtil.replaceString(args[0], "\\", "/");
        g2d_root = g2d_root.substring(0, g2d_root.lastIndexOf("/"));
        this.remote_root = g2d_root;
        this.local_root = new java.io.File(args[1]);
        System.out.println("remote : " + this.remote_root);
        System.out.println("local  : " + this.local_root.getPath());
    }

    public File createFile(String pathname) {
        try {
            pathname = CUtil.replaceString(pathname, "\\", "/");
            return new FileImpl(pathname);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public File createFile(String parent, String child) {
        return createFile(parent + "/" + child);
    }

    public File createFile(File parent, String child) {
        return createFile(parent.getPath() + "/" + child);
    }

    private java.io.File getLocalCookie(String remote_url) {
        return new java.io.File(local_root, remote_url.substring(remote_root.length()));
    }

    private class FileImpl implements File {

        final java.io.File local_file;

        final URL remote_url;

        final String name;

        final String parent;

        private FileImpl(String url) throws Exception {
            this.remote_url = new URL(url);
            this.local_file = getLocalCookie(url);
            String path = remote_url.toExternalForm();
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            this.parent = path.substring(0, path.lastIndexOf("/"));
            this.name = path.substring(path.lastIndexOf("/") + 1);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getParent() {
            return parent;
        }

        @Override
        public File getParentFile() {
            return createFile(getParent());
        }

        @Override
        public File getChildFile(String name) {
            return createFile(this, name);
        }

        @Override
        public String getPath() {
            return remote_url.toExternalForm();
        }

        @Override
        public InputStream getInputStream() {
            return CIO.getInputStream(parent + "/" + name);
        }

        @Override
        public OutputStream getOutputStream() {
            try {
                return new FileOutputStream(local_file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public byte[] readBytes() {
            return CIO.readStream(getInputStream());
        }

        @Override
        public void writeBytes(byte[] data) {
            CFile.writeData(local_file, data);
        }

        @Override
        public String readUTF() {
            return CIO.stringDecode(readBytes(), CIO.ENCODING);
        }

        @Override
        public void writeUTF(String data) {
            writeBytes(CIO.stringEncode(data, CIO.ENCODING));
        }

        @Override
        public boolean exists() {
            if (local_file.exists()) {
                return true;
            } else {
                try {
                    URLConnection c = remote_url.openConnection();
                    try {
                        c.setConnectTimeout(CIO.getLoadingTimeOut());
                        c.connect();
                        return c.getContentLength() > 0;
                    } catch (Exception err) {
                        err.printStackTrace();
                        return false;
                    } finally {
                        if (c instanceof HttpURLConnection) {
                            ((HttpURLConnection) c).disconnect();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

        @Override
        public File[] listFiles() {
            ArrayList<File> files = new ArrayList<File>();
            if (local_file.exists()) {
                for (java.io.File sub : local_file.listFiles()) {
                    files.add(createFile(this, sub.getName()));
                }
            }
            try {
                String html = query(remote_url.toExternalForm());
                int begin = html.indexOf("<ul>");
                int end = html.lastIndexOf("</ul>");
                html = html.substring(begin + 4, end);
                String[] list = html.split("</a></li>");
                for (String s : list) {
                    s = s.trim().replaceAll("<li><a href=\".*\">", "");
                    if (!s.isEmpty() && !s.equals("..")) {
                        File f = createFile(this, s);
                        files.add(f);
                    }
                }
            } catch (Throwable err) {
                System.err.println("list files error : " + getPath());
                err.printStackTrace();
            }
            return files.toArray(new File[files.size()]);
        }

        private String query(String url) throws Exception {
            HttpRequester request = new HttpRequester();
            HttpResponse hr = request.sendGet(url);
            return hr.getContent();
        }

        @Override
        public void delete() {
        }
    }

    public static void main(String[] args) {
    }
}
