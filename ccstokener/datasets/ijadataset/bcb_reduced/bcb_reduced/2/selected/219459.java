package com.ssg.net;

import com.ssg.util.SB_XML;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

/**
 * command-line tool to send request(s) to HTTP server(s) and store response(s).
 *
 * @author ssg
 */
public class HttpCaller {

    public static boolean _test = false;

    public static boolean _quiet = false;

    public static void help() {
        System.out.println("USAGE: " + HttpCaller.class.getName() + " options fileName");
        System.out.println("  Sends requests (calls) to URL and gets response. Results are wrapped in XML format.");
        System.out.println("  Options are:");
        System.out.println("    -url=<http url>");
        System.out.println("    -user=<user name>");
        System.out.println("    -password=<password>");
        System.out.println("    -test=<true|false> Show (true) or show and execute (false) requests. Default is " + _test);
        System.out.println("    -quiet=<true|false> Skip (true) or show runtime info messages " + _quiet);
        System.out.println("    -header=<http header=value>");
        System.out.println("    -call=<http body>");
        System.out.println("    -file=<file with calls> or @<file with list of fileswith calls>");
        System.out.println("  Last option value is used. Options '-call' and '-file' are accumulated.");
    }

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            help();
        } else {
            String url = null;
            String user = null;
            String password = null;
            String fileName = null;
            String testS = Boolean.toString(_test);
            String quietS = Boolean.toString(_quiet);
            String commitS = null;
            List<String> headers = new LinkedList<String>();
            List<String> calls = new LinkedList<String>();
            for (int i = 0; i < args.length; i++) {
                String s = args[i];
                String name = null;
                String value = s;
                if (s.indexOf("=") != -1) {
                    int idx = s.indexOf("=");
                    name = s.substring(0, idx);
                    value = s.substring(idx + 1);
                }
                if (name != null) {
                    if (name.equalsIgnoreCase("-url")) {
                        url = value;
                    } else if (name.equalsIgnoreCase("-user")) {
                        user = value;
                    } else if (name.equalsIgnoreCase("-password")) {
                        password = value;
                    } else if (name.equalsIgnoreCase("-test")) {
                        testS = value;
                    } else if (name.equalsIgnoreCase("-quiet")) {
                        quietS = value;
                    } else if (name.equalsIgnoreCase("-commit")) {
                        commitS = value;
                    } else if (name.equalsIgnoreCase("-header")) {
                        headers.add(value);
                    } else if (name.equalsIgnoreCase("-call")) {
                        calls.add(value);
                    } else if (name.equalsIgnoreCase("-file")) {
                        calls.add("@" + value);
                    } else {
                        System.err.println("UNKNOWN option(ignored): " + s);
                    }
                } else {
                    fileName = value;
                }
            }
            boolean test = _test;
            try {
                test = Boolean.parseBoolean(testS);
            } catch (Throwable th) {
            }
            boolean quiet = _quiet;
            try {
                quiet = Boolean.parseBoolean(quietS);
            } catch (Throwable th) {
            }
            List<RequestResponse> requests = new LinkedList<RequestResponse>();
            for (String call : calls) {
                if (call.startsWith("@")) {
                    String fn = call.substring(1);
                    try {
                        boolean fileWithFileNames = fn.startsWith("@");
                        if (fileWithFileNames) {
                            fn = fn = fn.substring(1);
                        }
                        LineNumberReader lnr = new LineNumberReader(new FileReader(fn));
                        StringBuffer sb = new StringBuffer();
                        String s = "";
                        while ((s = lnr.readLine()) != null) {
                            if (fileWithFileNames && s.trim().length() > 0) {
                                String fn2 = s;
                                try {
                                    LineNumberReader lnr2 = new LineNumberReader(new FileReader(fn2));
                                    while ((s = lnr2.readLine()) != null) {
                                        sb.append(s);
                                        sb.append("\n");
                                    }
                                    if (sb.toString().trim().length() > 0) {
                                        if (!quiet) {
                                            System.out.println("Added call from file '" + fn2 + "'");
                                        }
                                        requests.add(new RequestResponse(url, user, password, headers, sb.toString()));
                                    }
                                    sb = new StringBuffer();
                                    lnr2.close();
                                } catch (IOException ioex) {
                                    System.err.println("Skipping file @'" + fn2 + "'. Failed to open/read: " + ioex.getMessage());
                                }
                            } else {
                                sb.append(s);
                                sb.append("\n");
                            }
                        }
                        if (sb.toString().trim().length() > 0) {
                            if (!quiet) {
                                System.out.println("Added call from file '" + fn + "'");
                            }
                            requests.add(new RequestResponse(url, user, password, headers, sb.toString()));
                        }
                        lnr.close();
                    } catch (IOException ioex) {
                        System.err.println("Skipping file '" + fn + "'. Failed to open/read: " + ioex.getMessage());
                    }
                } else {
                    requests.add(new RequestResponse(url, user, password, headers, call));
                }
            }
            for (RequestResponse request : requests) {
                if (!quiet) {
                    System.out.print("URL: " + request._url + ", body: " + request._request + " ...");
                }
                if (!test) {
                    try {
                        request.run();
                        if (!quiet) {
                            System.out.println(request._response);
                        }
                    } finally {
                        if (!quiet) {
                            System.out.println();
                        }
                    }
                }
            }
            SB_XML sb = new SB_XML(new StringBuffer());
            sb.openTag("tests", true);
            for (RequestResponse request : requests) {
                if (!test) {
                    int level = sb.getLevel();
                    try {
                        sb.openTag("test", true);
                        sb.attribute("url", request._url.toString());
                        if (request._start != null) {
                            sb.attribute("start", request._start.toString());
                        }
                        if (request._end != null) {
                            sb.attribute("end", request._end.toString());
                        }
                        if (request._start != null && request._end != null) {
                            sb.attribute("duration", request._end.getTime() - request._start.getTime());
                        }
                        if (request._request != null) {
                            sb.attribute("req-size", request._request.length());
                        }
                        if (request._response != null) {
                            sb.attribute("res-size", request._response.length());
                        }
                        sb.openTag("request", true);
                        sb.cdata(request._request);
                        sb.closeTag();
                        if (request._headers != null && request._headers.size() > 0 || request._response_headers != null && request._response_headers.size() > 0) {
                            sb.openTag("headers", true);
                            if (request._headers != null && request._headers.size() > 0) {
                                sb.openTag("request", true);
                                for (String s : request._headers) {
                                    sb.openTag("header", true);
                                    sb.text(s);
                                    sb.closeTag();
                                }
                                sb.closeTag();
                            }
                            if (request._response_headers != null && request._response_headers.size() > 0) {
                                sb.openTag("response", true);
                                for (String s : request._response_headers) {
                                    sb.openTag("header", true);
                                    sb.text(s);
                                    sb.closeTag();
                                }
                                sb.closeTag();
                            }
                            sb.closeTag();
                        }
                        if (request._response != null) {
                            sb.openTag("response", true);
                            sb.cdata(request._response);
                            sb.closeTag();
                        }
                        if (request._error != null) {
                            sb.openTag("error", true);
                            sb.text(request._error.toString());
                            if (request._error.getCause() != null) {
                                sb.openTag("cause", true);
                                sb.text(request._error.getCause().getMessage());
                                sb.closeTag();
                            }
                            sb.closeTag();
                        }
                        sb.closeTag();
                    } finally {
                        while (sb.getLevel() > level) {
                            sb.closeTag();
                        }
                    }
                }
            }
            sb.closeTag();
            if (fileName == null) {
                System.out.println(sb.toString());
            } else {
                try {
                    Writer writer = new OutputStreamWriter(new FileOutputStream(new File(fileName)), Charset.forName("UTF-8"));
                    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    writer.write(sb.toString());
                    writer.close();
                } catch (IOException ioex) {
                    System.err.println(HttpCaller.class.getSimpleName() + ": " + ioex.getMessage());
                }
            }
        }
    }

    public static class RequestResponse {

        URL _url;

        List<String> _headers;

        String _request;

        String _response;

        List<String> _response_headers;

        Throwable _error;

        Date _start;

        Date _end;

        public RequestResponse(String url, String user, String password, List<String> headers, String request) {
            try {
                _url = new URL(url);
                if (user != null && (_url.getUserInfo() == null || _url.getUserInfo().trim() == "")) {
                }
            } catch (Throwable th) {
            }
            _headers = headers;
            _request = request;
        }

        public void run() {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) _url.openConnection();
                if (_headers != null) {
                    for (String header : _headers) {
                        if (header != null && header.indexOf("=") != -1) {
                            int idx = header.indexOf("=");
                            String name = header.substring(0, idx);
                            String value = header.substring(idx + 1);
                            conn.addRequestProperty(name, value);
                        }
                    }
                }
                conn.addRequestProperty("Content-Length", "" + _request.getBytes().length);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                _start = new Date();
                if (_request != null) {
                    conn.setDoOutput(true);
                    OutputStream os = conn.getOutputStream();
                    os.write(_request.getBytes());
                    os.flush();
                } else {
                    conn.connect();
                }
                BufferedInputStream bis = new BufferedInputStream(conn.getInputStream(), 1024 * 30);
                StringBuffer sb = new StringBuffer();
                for (byte[] b = new byte[1]; bis.read(b) > 0; sb.append((char) b[0])) ;
                bis.close();
                _response = sb.toString();
                if (conn.getHeaderFields() != null) {
                    for (Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
                        StringBuffer e = new StringBuffer();
                        e.append(entry.getKey());
                        e.append("=");
                        for (String v : entry.getValue()) {
                            e.append(v);
                            e.append(";");
                        }
                        if (e.toString().trim().length() > 0) {
                            if (_response_headers == null) {
                                _response_headers = new LinkedList<String>();
                            }
                            _response_headers.add(e.toString());
                        }
                    }
                }
                conn.disconnect();
            } catch (Throwable th) {
                th.printStackTrace();
                _error = th;
                if (conn != null) {
                    try {
                        conn.disconnect();
                    } catch (Throwable dummy) {
                    }
                }
            } finally {
                _end = new Date();
            }
        }
    }
}
