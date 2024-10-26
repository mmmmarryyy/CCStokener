package com.jPianoBar;

import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;
import com.jPianoBar.blowfish.BlowFish;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlrpc.android.Tag;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

public class XmlRpc extends org.xmlrpc.android.XMLRPCClient {

    public static final String PROTOCOL_VERSION = "31";

    public static final String RPC_URL = "http://www.pandora.com/radio/xmlrpc/v" + PROTOCOL_VERSION + "?";

    public static final String USER_AGENT = "com.jPianoBar";

    public XmlRpc(String url) {
        super(url);
    }

    @SuppressWarnings("unchecked")
    public Object callWithBody(String url, String body) throws XMLRPCException {
        postMethod.setURI(URI.create(url));
        try {
            HttpEntity entity = new StringEntity(body);
            postMethod.setEntity(entity);
            HttpResponse response = client.execute(postMethod);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new XMLRPCException("HTTP status code: " + statusCode + " != " + HttpStatus.SC_OK);
            }
            XmlPullParser pullParser = XmlPullParserFactory.newInstance().newPullParser();
            entity = response.getEntity();
            Reader reader = new InputStreamReader(new BufferedInputStream(entity.getContent()));
            pullParser.setInput(reader);
            pullParser.nextTag();
            pullParser.require(XmlPullParser.START_TAG, null, Tag.METHOD_RESPONSE);
            pullParser.nextTag();
            String tag = pullParser.getName();
            if (tag.equals(Tag.PARAMS)) {
                pullParser.nextTag();
                pullParser.require(XmlPullParser.START_TAG, null, Tag.PARAM);
                pullParser.nextTag();
                Object obj = iXMLRPCSerializer.deserialize(pullParser);
                entity.consumeContent();
                return obj;
            } else if (tag.equals(Tag.FAULT)) {
                pullParser.nextTag();
                Map<String, Object> map = (Map<String, Object>) iXMLRPCSerializer.deserialize(pullParser);
                String faultString = (String) map.get(Tag.FAULT_STRING);
                int faultCode = (Integer) map.get(Tag.FAULT_CODE);
                entity.consumeContent();
                throw new XMLRPCFault(faultString, faultCode);
            } else {
                entity.consumeContent();
                throw new XMLRPCException("Bad tag <" + tag + "> in XMLRPC response - neither <params> nor <fault>");
            }
        } catch (XMLRPCException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new XMLRPCException(e);
        }
    }

    public Object xmlrpcCall(String method, Vector<Object> args, Vector<Object> urlArgs, String rid) throws XMLRPCException {
        if (urlArgs == null) urlArgs = (Vector<Object>) args.clone();
        args.add(0, new Long(System.currentTimeMillis() / 1000L));
        String xml = XmlRpc.makeCall(method, args);
        String data = pandoraEncrypt(xml);
        ArrayList<String> urlArgStrings = new ArrayList<String>();
        if (rid != null) {
            urlArgStrings.add("rid=" + rid);
        }
        method = method.substring(method.lastIndexOf('.') + 1);
        urlArgStrings.add("method=" + method);
        Iterator<Object> urlArgsIter = urlArgs.iterator();
        int count = 1;
        while (urlArgsIter.hasNext()) {
            urlArgStrings.add("arg" + (count++) + "=" + formatUrlArg(urlArgsIter.next()));
        }
        StringBuilder url = new StringBuilder(RPC_URL);
        Iterator<String> argIter = urlArgStrings.iterator();
        while (argIter.hasNext()) {
            url.append(argIter.next());
            if (argIter.hasNext()) url.append("&");
        }
        Object result = callWithBody(url.toString(), data);
        return result;
    }

    public Object xmlrpcCall(String method, Vector<Object> args, Vector<Object> urlArgs, PandoraAccount account) throws XMLRPCException {
        if (urlArgs == null) urlArgs = (Vector<Object>) args.clone();
        args.add(0, new Long(System.currentTimeMillis() / 1000L));
        if (account.getAuthToken() != null) args.add(1, account.getAuthToken());
        String xml = XmlRpc.makeCall(method, args);
        String data = pandoraEncrypt(xml);
        ArrayList<String> urlArgStrings = new ArrayList<String>();
        if (account.getRid() != null) {
            urlArgStrings.add("rid=" + account.getRid());
        }
        if (account.getListenerId() != null) {
            urlArgStrings.add("lid=" + account.getListenerId());
        }
        method = method.substring(method.lastIndexOf('.') + 1);
        urlArgStrings.add("method=" + method);
        Iterator<Object> urlArgsIter = urlArgs.iterator();
        int count = 1;
        while (urlArgsIter.hasNext()) {
            urlArgStrings.add("arg" + (count++) + "=" + formatUrlArg(urlArgsIter.next()));
        }
        StringBuilder url = new StringBuilder(RPC_URL);
        Iterator<String> argIter = urlArgStrings.iterator();
        while (argIter.hasNext()) {
            url.append(argIter.next());
            if (argIter.hasNext()) url.append("&");
        }
        Object result = callWithBody(url.toString(), data);
        return result;
    }

    public void addHeader(String header, String value) {
        postMethod.addHeader(header, value);
    }

    public static String pandoraEncrypt(String s) {
        int length = s.length();
        StringBuilder result = new StringBuilder(length * 2);
        int i8 = 0;
        for (int i = 0; i < length; i += 8) {
            i8 = (i + 8 >= length) ? (length) : (i + 8);
            String substring = s.substring(i, i8);
            String padded = pad(substring, 8);
            long[] blownstring = BlowFish.getBlowFishEncoderInstance().encrypt(padded.toCharArray());
            for (int c = 0; c < blownstring.length; c++) {
                if (blownstring[c] < 0x10) result.append("0");
                result.append(Integer.toHexString((int) blownstring[c]));
            }
        }
        return result.toString();
    }

    public static String pandoraDecrypt(String s) {
        StringBuilder result = new StringBuilder();
        int length = s.length();
        int i16 = 0;
        for (int i = 0; i < length; i += 16) {
            i16 = (i + 16 > length) ? (length - 1) : (i + 16);
            result.append(BlowFish.getBlowFishDecoderInstance().decrypt(pad(fromHex(s.substring(i, i16)), 8).toCharArray()));
        }
        return result.toString().trim();
    }

    private static String fromHex(String hexText) {
        String decodedText = null;
        String chunk = null;
        if (hexText != null && hexText.length() > 0) {
            int numBytes = hexText.length() / 2;
            char[] rawToByte = new char[numBytes];
            int offset = 0;
            for (int i = 0; i < numBytes; i++) {
                chunk = hexText.substring(offset, offset + 2);
                offset += 2;
                rawToByte[i] = (char) (Integer.parseInt(chunk, 16) & 0x000000FF);
            }
            decodedText = new String(rawToByte);
        }
        return decodedText;
    }

    private static String pad(String s, int l) {
        String result = s;
        while (l - s.length() > 0) {
            result += '\0';
            l--;
        }
        return result;
    }

    public static String value(String v) {
        return "<value><string>" + v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</string></value>";
    }

    public static String value(boolean v) {
        return v ? "<value><boolean>1</boolean></value>" : "<value><boolean>0</boolean></value>";
    }

    public static String value(int v) {
        return "<value><int>" + String.valueOf(v) + "</int></value>";
    }

    public static String value(Number v) {
        return value(v.intValue());
    }

    public static String value(String[] list) {
        StringBuilder result = new StringBuilder("<value><array><data>");
        for (int i = 0; i < list.length; i++) {
            result.append(value(list[i]));
        }
        return result.append("</data></array></value>").toString();
    }

    public static String value(int[] list) {
        StringBuilder result = new StringBuilder("<value><array><data>");
        for (int i = 0; i < list.length; i++) {
            result.append(value(list[i]));
        }
        return result.append("</data></array></value>").toString();
    }

    public static String value(AbstractCollection<?> list) {
        StringBuilder result = new StringBuilder("<value><array><data>");
        Iterator<?> listIter = list.iterator();
        while (listIter.hasNext()) {
            result.append(valueGuess(listIter.next()));
        }
        return result.append("</data></array></value>").toString();
    }

    public static String valueGuess(Object v) {
        if (v instanceof Number) return value((Number) v); else if (v instanceof Boolean) return value((Boolean) v); else if (v instanceof String) return value((String) v); else if (v instanceof AbstractCollection<?>) return value((AbstractCollection<?>) v); else return value(v.toString());
    }

    public static String makeCall(String method, Vector<Object> args) {
        StringBuilder argsStr = new StringBuilder();
        Iterator<Object> argsIter = args.iterator();
        while (argsIter.hasNext()) {
            Object item = argsIter.next();
            argsStr.append("<param>").append(valueGuess(item)).append("</param>");
        }
        return "<?xml version=\"1.0\"?><methodCall><methodName>" + method + "</methodName><params>" + argsStr.toString() + "</params></methodCall>";
    }

    private String formatUrlArg(boolean v) {
        return v ? "true" : "false";
    }

    private String formatUrlArg(int v) {
        return String.valueOf(v);
    }

    private String formatUrlArg(long v) {
        return String.valueOf(v);
    }

    private String formatUrlArg(float v) {
        return String.valueOf(v);
    }

    private String formatUrlArg(double v) {
        return String.valueOf(v);
    }

    private String formatUrlArg(char v) {
        return String.valueOf(v);
    }

    private String formatUrlArg(short v) {
        return String.valueOf(v);
    }

    private String formatUrlArg(Object v) {
        return URLEncoder.encode(v.toString());
    }

    private String formatUrlArg(Object[] v) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < v.length; i++) {
            result.append(formatUrlArg(v[i]));
            if (i < v.length - 1) result.append("%2C");
        }
        return result.toString();
    }

    private String formatUrlArg(Iterator<?> v) {
        StringBuilder result = new StringBuilder();
        while (v.hasNext()) {
            result.append(formatUrlArg(v.next()));
            if (v.hasNext()) result.append("%2C");
        }
        return result.toString();
    }

    private String formatUrlArg(Collection<?> v) {
        return formatUrlArg(v.iterator());
    }
}
