package com.jot.admin;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import mojasi.MojasiWriter;
import com.jot.system.utils.EndTimer;
import com.jot.system.utils.HttpUtils;

/**
 * A simple class to read a String from an http server using a GET.
 * 
 * @author alanwootton
 * 
 */
public class GetHtmlFromServer {

    public static String get(String host, int port, String cmd) {
        byte[] b = getbytes(host, port, cmd);
        if (b == null) return "";
        return new String(b);
    }

    public static byte[] getbytes(String host, int port, String cmd) {
        String result = "GetHtmlFromServer no answer";
        String tmp = "";
        result = "";
        try {
            tmp = "http://" + host + ":" + port + "/" + cmd;
            URL url = new URL(tmp);
            if (1 == 2) {
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                String str;
                while ((str = in.readLine()) != null) {
                    result += str;
                }
                in.close();
                return result.getBytes();
            } else {
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(2 * 1000);
                c.setRequestMethod("GET");
                c.connect();
                int amt = c.getContentLength();
                InputStream in = c.getInputStream();
                MojasiWriter writer = new MojasiWriter();
                byte[] buff = new byte[256];
                while (writer.size() < amt) {
                    int got = in.read(buff);
                    if (got < 0) break;
                    writer.pushBytes(buff, got);
                }
                in.close();
                c.disconnect();
                return writer.getBytes();
            }
        } catch (MalformedURLException e) {
            System.err.println(tmp + " " + e);
        } catch (IOException e) {
            ;
        }
        return null;
    }

    public static byte[] XXhttp_raw(String method, String host, int port, String cmd, byte[] data) {
        String result = "GetHtmlFromServer no answer";
        result = "";
        try {
            if (1 == 1) {
                Socket sock = new Socket(host, port);
                byte[] ppst = HttpUtils.makeHTTPraw(method, cmd, data, host);
                OutputStream out = sock.getOutputStream();
                out.write(ppst);
                out.flush();
                InputStream in = sock.getInputStream();
                List<byte[]> list = new ArrayList<byte[]>();
                int size = 0;
                EndTimer timer = new EndTimer(2000);
                boolean done = false;
                while (!done) {
                    byte[] tmp = new byte[64];
                    int got = 0;
                    while (got < tmp.length) {
                        int b = in.read();
                        if (b < 0) {
                            done = true;
                            break;
                        }
                        tmp[got++] = (byte) b;
                    }
                    if (got != tmp.length) {
                        byte[] t = new byte[tmp.length];
                        System.arraycopy(tmp, 0, t, 0, got);
                        tmp = t;
                    }
                    list.add(tmp);
                    size += tmp.length;
                    if (!timer.good()) return null;
                }
                in.close();
                byte[] res = new byte[size];
                int pos = 0;
                for (byte[] a : list) {
                    System.arraycopy(a, 0, res, pos, a.length);
                    pos += a.length;
                }
                return res;
            } else {
                Socket sock = new Socket(host, port);
                String connectString = "POST /" + cmd;
                connectString += " HTTP/1.1\r\n";
                connectString += "Cache-Control: no-cache\r\n";
                connectString += "Host: " + host + "\r\n";
                connectString += "Content-Length: " + data.length + "\r\n";
                connectString += "Content-Type: text/javascript\r\n";
                connectString += "\r\n";
                OutputStream out = sock.getOutputStream();
                out.write(connectString.getBytes());
                out.write(data);
                out.flush();
                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                String str;
                while ((str = in.readLine()) != null) {
                    result += str;
                }
                in.close();
                out.close();
                return result.getBytes();
            }
        } catch (MalformedURLException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.out.println(e);
        }
        return null;
    }

    public static byte[] post(String host, int port, String cmd, byte[] data) {
        String url = "http://" + host + ":" + port + "/" + cmd;
        byte[] got = excutePostRaw(url, data);
        if (got == null) return new byte[0];
        return got;
    }

    public static byte[] excutePostRaw(String targetURL, byte[] postdata) {
        URL url;
        HttpURLConnection connection = null;
        try {
            url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(postdata.length));
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.write(postdata);
            wr.flush();
            wr.close();
            int amt = connection.getContentLength();
            InputStream in = connection.getInputStream();
            MojasiWriter writer = new MojasiWriter();
            byte[] buff = new byte[256];
            while (writer.size() < amt) {
                int got = in.read(buff);
                if (got < 0) break;
                writer.pushBytes(buff, got);
                if (writer.size() > 1) System.out.println();
            }
            in.close();
            connection.disconnect();
            return writer.getBytes();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static void main(String[] args) {
        String str = GetHtmlFromServer.get("google.com", 80, "");
        str = GetHtmlFromServer.get("localhost", 8080, "balli/getplayer?user_id=1234");
        System.out.println(str);
    }
}
