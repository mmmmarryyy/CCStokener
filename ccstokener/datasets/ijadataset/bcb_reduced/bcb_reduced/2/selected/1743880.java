package entagged.covers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AmazonCoverSource implements CoverSource {

    private static final String SOAP_ACTION = "SearchArtistRequest";

    private static final String SOAP_URL = "http://soap.amazon.com/onca/soap3";

    private static final Pattern SOAP_PATTERN = Pattern.compile(".+?ProductName.+?>(.+?)</ProductName.+?Artist .+?>(.+?)</Artist.+?ImageUrlSmall.+?>(.+?)</ImageUrlSmall.+?ImageUrlMedium.+?>(.+?)</ImageUrlMedium.+?ImageUrlLarge.+?>(.+?)</ImageUrlLarge.*");

    private String getSoapRequest(String artist, String album) {
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<SOAP-ENV:Envelope SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\">");
        sb.append("<SOAP-ENV:Body>");
        sb.append("<ns1:ArtistSearchRequest xmlns:ns1=\"http://soap.amazon.com\" SOAP-ENC:root=\"1\">");
        sb.append("<v1>" + "<sort xsi:type=\"xsd:string\">+pmrank</sort>");
        sb.append("<artist xsi:type=\"xsd:string\">").append(artist).append("</artist>");
        sb.append("<devtag xsi:type=\"xsd:string\">D1R5WXCELBX4NF</devtag>");
        sb.append("<tag xsi:type=\"xsd:string\">accessonline</tag>");
        sb.append("<mode xsi:type=\"xsd:string\">music</mode>");
        sb.append("<keywords xsi:type=\"xsd:string\">").append(album).append("</keywords>");
        sb.append("<type xsi:type=\"xsd:string\">lite</type>");
        sb.append("<page xsi:type=\"xsd:int\">1</page>" + "</v1>");
        sb.append("</ns1:ArtistSearchRequest>" + "</SOAP-ENV:Body>");
        sb.append("</SOAP-ENV:Envelope>");
        return sb.toString();
    }

    private String sendSoapRequest(String request) throws IOException {
        HttpURLConnection httpConn = (HttpURLConnection) new URL(SOAP_URL).openConnection();
        httpConn.setRequestProperty("Content-Length", String.valueOf(request.length()));
        httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        httpConn.setRequestProperty("SOAPAction", SOAP_ACTION);
        httpConn.setRequestMethod("POST");
        httpConn.setDoOutput(true);
        httpConn.setDoInput(true);
        PrintWriter out = new PrintWriter(httpConn.getOutputStream());
        out.println(request);
        out.close();
        BufferedReader in = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
        String inputLine;
        StringBuffer sb = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            sb.append(inputLine);
        }
        in.close();
        return sb.toString();
    }

    private boolean isImageEmpty(String url) {
        try {
            HttpURLConnection httpConn = (HttpURLConnection) new URL(url).openConnection();
            return httpConn.getContentLength() == 807;
        } catch (IOException e) {
            return true;
        }
    }

    public List search(String artist, String album) throws CoverException {
        List list = new LinkedList();
        String nartist = Utils.normalize(artist);
        String nalbum = Utils.normalize(album);
        String answer;
        try {
            answer = sendSoapRequest(getSoapRequest(nartist, nalbum));
        } catch (IOException e) {
            throw new CoverException(e.getMessage());
        }
        answer = answer.replaceAll("\n", "");
        answer = answer.replaceAll("&amp;", "&");
        int nextSubIndex = 0;
        while (answer.length() > nextSubIndex) {
            Matcher matcher = SOAP_PATTERN.matcher(answer.substring(nextSubIndex));
            if (matcher.matches()) {
                String bestUrl = "";
                for (int i = 5; i >= 3; i--) {
                    bestUrl = matcher.group(i);
                    if (!isImageEmpty(bestUrl)) break;
                }
                if (bestUrl.equals("")) break;
                try {
                    list.add(new Cover(matcher.group(2), matcher.group(1), new URL(bestUrl)));
                } catch (IOException e) {
                }
                nextSubIndex += matcher.end(matcher.groupCount());
            } else {
                break;
            }
        }
        return list;
    }
}
