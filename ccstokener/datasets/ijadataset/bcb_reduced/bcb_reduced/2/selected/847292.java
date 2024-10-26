package it.southdown.avana.predict.netctl;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.*;

/**
 * A NetCTL 1.2 web client
 * 
 */
public class NetctlClient {

    private static final String NETCTL_URL = "http://www.cbs.dtu.dk/cgi-bin/nph-webface";

    private static final String MULTIPART_BOUNDARY = "-----------------------------15327312824085";

    public static final String URL_REDIRECT_REGEX = "location\\.replace\\(\\\"(.*?)\\\"\\)";

    public static Pattern urlRedirectPattern = Pattern.compile(URL_REDIRECT_REGEX);

    private static final int UNKNOWN = -1;

    private static final int SUBMITTED = 1;

    private static final int REJECTED = 2;

    private static final int RESULTS = 3;

    private static final int QUEUED = 4;

    private URL url;

    public NetctlClient() {
        try {
            url = new URL(NETCTL_URL);
        } catch (Exception e) {
            System.err.println("Error with NetCTL URL: " + e);
        }
    }

    public float[] getPeptidePredictions(String supertype, String[] sequences) {
        String predictionPage = null;
        try {
            predictionPage = retrievePredictionPage(supertype, sequences);
        } catch (UnexpectedHttpStatusException e) {
            System.err.println("Skipping request, error requesting NetCTL predictions: " + e);
            return null;
        }
        float[] result = null;
        try {
            HashMap<String, Float> table = parsePredictions(predictionPage);
            result = getPredictionValues(table, sequences);
        } catch (Exception e) {
            System.err.println("Error parsing NetCTL results: " + e);
        }
        return result;
    }

    public String retrievePredictionPage(String supertype, String[] sequences) throws UnexpectedHttpStatusException {
        StringBuffer fsb = new StringBuffer();
        for (int i = 0; i < sequences.length; i++) {
            fsb.append("> S");
            fsb.append(i + 1);
            fsb.append('\n');
            fsb.append(sequences[i]);
            fsb.append('\n');
        }
        String fastaSeq = fsb.toString();
        String resp = submitRequest(supertype, fastaSeq);
        int respType = getResponseType(resp);
        while (respType == REJECTED) {
            System.out.println("Request rejected, waiting before next submission");
            suspendExecution(300000);
            resp = submitRequest(supertype, fastaSeq);
            respType = getResponseType(resp);
        }
        if (getResponseType(resp) != SUBMITTED) {
            System.err.println("Unexpected response: " + resp);
            return null;
        }
        URL redirectUrl = getRedirectUrl(resp);
        URL jobUrl = redirectUrl;
        while (redirectUrl != null) {
            suspendExecution(10000);
            resp = new String(sendGet(jobUrl));
            redirectUrl = getRedirectUrl(resp);
        }
        while (getResponseType(resp) == QUEUED) {
            suspendExecution(10000);
            resp = new String(sendGet(jobUrl));
        }
        if (!resp.contains("prediction results")) {
            System.err.println("Unexpected response: " + resp);
            return null;
        }
        return resp;
    }

    private int getResponseType(String resp) {
        if (resp.contains("Webface Jobsubmission")) {
            return SUBMITTED;
        } else if (resp.contains("Job rejected due to limited number of jobs")) {
            return REJECTED;
        } else if (resp.contains("prediction results")) {
            return RESULTS;
        } else if (resp.contains(" Webservices : Job queue ")) {
            return QUEUED;
        }
        return UNKNOWN;
    }

    private void suspendExecution(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
        }
    }

    private URL getRedirectUrl(String response) {
        String resultUrlStr = findPattern(response, urlRedirectPattern);
        if (resultUrlStr != null) {
            try {
                return new URL(resultUrlStr);
            } catch (Exception e) {
                System.err.println("Error in result URL: " + e);
            }
        }
        return null;
    }

    private HashMap<String, Float> parsePredictions(String resp) throws IOException {
        HashMap<String, Float> table = new HashMap<String, Float>();
        StringReader sr = new StringReader(resp);
        BufferedReader br = new BufferedReader(sr);
        String line = br.readLine();
        while (!line.contains(" predictions using MHC")) {
            line = br.readLine();
        }
        while ((line = getNextValidLine(br)) != null) {
            ArrayList<String> v = new ArrayList<String>();
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) {
                v.add(st.nextToken());
            }
            String peptide = (String) v.get(4);
            String valueStr = (String) v.get(14);
            Float value = Float.valueOf(valueStr);
            table.put(peptide, value);
        }
        return table;
    }

    private float[] getPredictionValues(HashMap<String, Float> valueTable, String[] sequences) {
        float[] result = new float[sequences.length];
        Arrays.fill(result, -1);
        for (int i = 0; i < sequences.length; i++) {
            String s = sequences[i];
            Float value = valueTable.get(s);
            if (value != null) {
                result[i] = value.floatValue();
            }
        }
        return result;
    }

    private String getNextValidLine(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            if (line.startsWith("---------------------")) {
                return null;
            }
            return line;
        }
        return null;
    }

    private String findPattern(String s, Pattern pattern) {
        if (s == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String submitRequest(String supertype, String fastaSeq) throws UnexpectedHttpStatusException {
        StringBuffer sb = new StringBuffer();
        appendMultipartParam(sb, "configfile", "/usr/opt/www/pub/CBS/services/NetCTL-1.2/NetCTL.cf");
        appendMultipartParam(sb, "SEQPASTE", fastaSeq);
        appendMultipartParam(sb, "supertype", supertype);
        appendMultipartParam(sb, "wcle", "0.15");
        appendMultipartParam(sb, "wtap", "0.05");
        appendMultipartParam(sb, "threshold", "0.5");
        appendMultipartParam(sb, "sort", "-1");
        sb.append("--");
        sb.append(MULTIPART_BOUNDARY);
        sb.append("--\r\n");
        String data = sb.toString();
        String resp = new String(sendPostMultipart(url, data));
        return resp;
    }

    private byte[] sendPostMultipart(URL url, String content) throws UnexpectedHttpStatusException {
        byte[] result = null;
        PrintWriter w = null;
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-type", "multipart/form-data, boundary=" + MULTIPART_BOUNDARY);
            conn.setRequestMethod("POST");
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            dos.writeBytes(content);
            dos.flush();
            int statusCode = conn.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                throw new UnexpectedHttpStatusException("Response code from " + url + " was " + statusCode);
            }
            in = conn.getInputStream();
            out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        } catch (UnexpectedHttpStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("POST Error (" + url + "): " + e);
        } finally {
            try {
                w.close();
            } catch (Exception e) {
            }
            try {
                in.close();
            } catch (Exception e) {
            }
            try {
                out.close();
            } catch (Exception e) {
            }
        }
        result = out.toByteArray();
        return result;
    }

    private void appendMultipartParam(StringBuffer sb, String key, String value) {
        sb.append("--");
        sb.append(MULTIPART_BOUNDARY);
        sb.append("\r\nContent-Disposition: form-data; name=\"");
        sb.append(key);
        sb.append("\"\r\n\r\n");
        sb.append(value);
        sb.append("\r\n");
    }

    public byte[] sendGet(URL url) throws UnexpectedHttpStatusException {
        byte[] result = null;
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            int statusCode = conn.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                throw new UnexpectedHttpStatusException("Response code from " + url + " was " + statusCode);
            }
            in = conn.getInputStream();
            out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        } catch (UnexpectedHttpStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("GET Error (" + url + "): " + e);
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
            try {
                out.close();
            } catch (Exception e) {
            }
        }
        result = out.toByteArray();
        return result;
    }

    private class UnexpectedHttpStatusException extends Exception {

        @SuppressWarnings("unused")
        public UnexpectedHttpStatusException() {
        }

        public UnexpectedHttpStatusException(String msg) {
            super(msg);
        }
    }
}
