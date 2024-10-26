package gov.lanl.repo.oaidb.srv;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class PutPostClient {

    public String execPut(String strurl, String xmlstr) throws Exception {
        String data = "verb=PutRecord&xml=" + URLEncoder.encode(xmlstr, "UTF-8");
        URL url = new URL(strurl);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        StringBuffer sb = new StringBuffer();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        wr.close();
        rd.close();
        return sb.toString();
    }

    public String execDelete(String strurl, String id) throws Exception {
        String data = "verb=DeleteRecord&identifier=" + URLEncoder.encode(id, "UTF-8");
        URL url = new URL(strurl);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        StringBuffer sb = new StringBuffer();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        wr.close();
        rd.close();
        return sb.toString();
    }

    public String execUpdate(String strurl, String id, String xmlstr) throws Exception {
        String data = "verb=UpdateRecord&identifier=" + URLEncoder.encode(id, "UTF-8") + "&xml=" + URLEncoder.encode(xmlstr, "UTF-8");
        URL url = new URL(strurl);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        StringBuffer sb = new StringBuffer();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        wr.close();
        rd.close();
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("usage: PutFromFile [properties file] [file with pmpxml]");
            throw new IllegalArgumentException("Wrong number of arguments");
        }
        Reader is = new FileReader(args[1]);
        char[] b = new char[1024];
        StringBuffer sb = new StringBuffer();
        int n;
        while ((n = is.read(b)) > 0) {
            sb.append(b, 0, n);
        }
        String test = sb.toString();
        System.out.println(test);
        String strurl = args[0];
        String data = "verb=PutRecord&xml=" + URLEncoder.encode(test, "UTF-8");
        URL url = new URL(strurl);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            System.out.println(line);
        }
        wr.close();
        rd.close();
    }
}
