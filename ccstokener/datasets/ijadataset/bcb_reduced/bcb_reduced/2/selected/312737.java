package org.qsardb.resolution.parscit;

import java.io.*;
import java.net.*;
import java.util.*;
import org.jbibtex.*;
import org.jbibtex.ParseException;
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.conn.*;
import org.apache.http.entity.mime.*;
import org.apache.http.entity.mime.content.*;
import org.apache.http.impl.client.*;
import org.apache.http.params.*;
import org.apache.http.util.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class Service {

    private Service() {
    }

    public static BibTeXDatabase parse(String citation) throws IOException, ParseException {
        String html = resolve(citation);
        Document document = Jsoup.parse(html);
        Elements bibChildren = document.select("div[id = bib] > pre");
        if (bibChildren.size() != 1) {
            throw new IllegalArgumentException(citation);
        }
        Element bibChild = bibChildren.first();
        StringReader reader = new StringReader(bibChild.text());
        try {
            BibTeXParser parser = new BibTeXParser();
            BibTeXDatabase database = parser.parse(reader);
            Map<Key, BibTeXEntry> entries = database.getEntries();
            if (entries.size() != 1) {
                throw new IllegalArgumentException(citation);
            }
            return database;
        } finally {
            reader.close();
        }
    }

    private static String resolve(String citation) throws IOException {
        URL url = new URL("http://aye.comp.nus.edu.sg/parsCit/parsCit.cgi");
        HttpParams parameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(parameters, 10 * 1000);
        HttpConnectionParams.setSoTimeout(parameters, 10 * 1000);
        HttpClient client = new DefaultHttpClient(parameters);
        try {
            HttpPost request = new HttpPost(url.toURI());
            MultipartEntity requestBody = new MultipartEntity();
            requestBody.addPart("demo", new StringBody("3"));
            requestBody.addPart("textlines", new StringBody(citation));
            requestBody.addPart("bib3", new StringBody("on"));
            request.setEntity(requestBody);
            HttpResponse response = client.execute(request);
            StatusLine status = response.getStatusLine();
            switch(status.getStatusCode()) {
                case HttpStatus.SC_OK:
                    break;
                default:
                    throw new IOException(status.getReasonPhrase());
            }
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                HttpEntity responseBody = response.getEntity();
                try {
                    responseBody.writeTo(os);
                } finally {
                    os.flush();
                }
                String encoding = EntityUtils.getContentCharSet(responseBody);
                if (encoding == null) {
                    encoding = "UTF-8";
                }
                return os.toString(encoding);
            } finally {
                os.close();
            }
        } catch (URISyntaxException use) {
            throw new IOException(use);
        } finally {
            ClientConnectionManager connectionManager = client.getConnectionManager();
            if (connectionManager != null) {
                connectionManager.shutdown();
            }
        }
    }
}
