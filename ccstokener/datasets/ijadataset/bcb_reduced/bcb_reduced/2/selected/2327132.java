package gr.demokritos.iit.PServer;

import java.io.*;
import java.net.*;
import java.util.*;

public class PSClientRequest {

    public StringBuffer response;

    private int respCode;

    private String errMsg;

    private int cols = 0;

    private int rows = 0;

    private Vector ra = null;

    private String userAttributes;

    public PSClientRequest(InetAddress dest, int port, String request, boolean methodPost, int timeout) {
        response = new StringBuffer();
        respCode = responseTomcat(dest, port, request, methodPost, response, timeout);
        if (respCode == 200) parseResponse();
    }

    public void execute(InetAddress dest, int port, String request, boolean methodPost, int timeout) {
        response = new StringBuffer();
        respCode = responseTomcat(dest, port, request, methodPost, response, timeout);
        if (respCode == 200) parseResponse();
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return cols;
    }

    public String getValue(int row, int col) {
        if (ra == null) return null;
        if (row >= rows || col >= cols) return null;
        return ((String) ra.elementAt(row * cols + col));
    }

    public String getResponse() {
        return response.substring(0);
    }

    public boolean isError() {
        if (respCode == 200) return false;
        return true;
    }

    public String getErrorMessage() {
        String msg;
        if (respCode < 0) msg = errMsg; else if (respCode >= 400 && respCode < 500) msg = "Client request error"; else if (respCode >= 500) msg = "Server internal error"; else msg = "";
        return msg;
    }

    public int getResponseCode() {
        return respCode;
    }

    private void parseResponse() {
        int idx;
        int from, to;
        String resp = response.substring(0);
        idx = 0;
        while ((idx = resp.indexOf("<row>", idx)) != -1) {
            idx++;
            rows++;
        }
        if (rows == 0) return;
        from = resp.indexOf("<row>", 0);
        to = resp.indexOf("</row>", 0);
        from += 5;
        String aRow = resp.substring(from, to);
        idx = 0;
        while ((idx = aRow.indexOf("<", idx)) != -1) {
            idx++;
            cols++;
        }
        if (cols % 2 != 0) {
            cols = 0;
            return;
        }
        cols = cols / 2;
        ra = new Vector(rows * cols);
        to = 0;
        for (int i = 0; i < rows; i++) {
            from = resp.indexOf("<row>", to);
            to = resp.indexOf("</row>", from);
            if (from < 0 || to < 0) return;
            from += 5;
            String currRow = resp.substring(from, to);
            StringTokenizer parser = new StringTokenizer(currRow, "<>", true);
            for (int j = 0; j < cols; j++) {
                String value;
                parser.nextToken("<");
                parser.nextToken(">");
                parser.nextToken(">");
                String tmp = parser.nextToken("<");
                value = tmp.equals("<") ? "" : tmp;
                if (!tmp.equals("<")) parser.nextToken("<");
                parser.nextToken(">");
                parser.nextToken(">");
                ra.add(i * cols + j, value);
            }
        }
    }

    int dispatchServer(InetAddress dest, int port, String request, boolean methodPost, StringBuffer response, int timeout) {
        int methodGetMaxSize = 250;
        int methodPostMaxSize = 32000;
        if (request == null || response == null) return -1;
        String fullRequest;
        if (methodPost) {
            String resource;
            String queryStr;
            int qIdx = request.indexOf('?');
            if (qIdx == -1) {
                resource = request;
                queryStr = "";
            } else {
                resource = request.substring(0, qIdx);
                queryStr = request.substring(qIdx + 1);
            }
            fullRequest = "POST " + resource + " HTTP/1.1\nHost: " + dest.getHostName() + ":" + (new Integer(port)).toString() + "\n\n" + queryStr;
        } else {
            fullRequest = "GET " + request + " HTTP/1.1\nHost: " + dest.getHostName() + ":" + (new Integer(port)).toString() + "\n\n";
        }
        if (methodPost && fullRequest.length() > methodPostMaxSize) {
            response.setLength(0);
            response.append("Complete POST request longer than maximum of " + methodPostMaxSize);
            return -5;
        } else if ((!methodPost) && fullRequest.length() > methodGetMaxSize) {
            response.setLength(0);
            response.append("Complete GET request longer than maximum of " + methodGetMaxSize);
            return -6;
        }
        int from = 0;
        while (from < response.length() && !Character.isWhitespace(response.charAt(from))) from += 1;
        while (from < response.length() && Character.isWhitespace(response.charAt(from))) from += 1;
        int to = from;
        while (to < response.length() && !Character.isWhitespace(response.charAt(to))) to += 1;
        String theCode = response.substring(from, to);
        int respCode;
        try {
            respCode = Integer.parseInt(theCode);
        } catch (NumberFormatException e) {
            response.setLength(0);
            response.append(e.toString());
            return -4;
        }
        boolean emptyLine = false;
        boolean justSpaces = false;
        int i = 0;
        while (i < response.length() && !emptyLine) {
            char ch = response.charAt(i);
            if (ch == '\n' && !justSpaces) justSpaces = true; else if (!Character.isWhitespace(ch) && justSpaces) justSpaces = false; else if (ch == '\n' && justSpaces) emptyLine = true;
            i += 1;
        }
        response.delete(0, i);
        return respCode;
    }

    int responseTomcat(InetAddress dest, int port, String request, boolean methodPost, StringBuffer response, int timeout) {
        int methodGetMaxSize = 250;
        int methodPostMaxSize = 32000;
        if (request == null || response == null) return -1;
        String fullRequest;
        if (methodPost) {
            String resource;
            String queryStr;
            int qIdx = request.indexOf('?');
            if (qIdx == -1) {
                resource = request;
                queryStr = "";
            } else {
                resource = request.substring(0, qIdx);
                queryStr = request.substring(qIdx + 1);
            }
            fullRequest = "POST " + resource + " HTTP/1.1\nHost: " + dest.getHostName() + ":" + (new Integer(port)).toString() + "\n\n" + queryStr;
        } else {
            fullRequest = "GET " + request + " HTTP/1.1\nHost: " + dest.getHostName() + ":" + (new Integer(port)).toString() + "\n\n";
        }
        if (methodPost && fullRequest.length() > methodPostMaxSize) {
            response.setLength(0);
            response.append("Complete POST request longer than maximum of " + methodPostMaxSize);
            return -5;
        } else if ((!methodPost) && fullRequest.length() > methodGetMaxSize) {
            response.setLength(0);
            response.append("Complete GET request longer than maximum of " + methodGetMaxSize);
            return -6;
        }
        String inputLine = "";
        request = "http://" + dest.getHostName() + ":" + (new Integer(port).toString()) + request;
        try {
            URL urlAddress = new URL(request);
            URLConnection urlC = urlAddress.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlC.getInputStream()));
            while ((inputLine = in.readLine()) != null) {
                response = response.append(inputLine).append("\n");
            }
        } catch (MalformedURLException e) {
            return -4;
        } catch (IOException e) {
            return -3;
        }
        return 200;
    }
}
