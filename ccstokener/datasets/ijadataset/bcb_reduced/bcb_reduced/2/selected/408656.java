package com.softwaresmithy.lib;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.regex.MatchResult;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class CheckAvailability {

    private static final String isbnSearchUrl = "http://libsys.arlingtonva.us/search/?searchtype=i&searcharg=%s&searchscope=1";

    /**
	 * @param args
	 */
    public static void main(String[] args) throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        InputStream is = CheckAvailability.class.getResourceAsStream("/isbns.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String isbn = null;
        HttpGet get = null;
        while ((isbn = br.readLine().split(" ")[0]) != null) {
            System.out.println("Target url: \n\t" + String.format(isbnSearchUrl, isbn));
            get = new HttpGet(String.format(isbnSearchUrl, isbn));
            HttpResponse resp = httpclient.execute(get);
            Scanner s = new Scanner(resp.getEntity().getContent());
            String pattern = s.findWithinHorizon("((\\d*) hold[s]? on first copy returned of (\\d*) )?[cC]opies", 0);
            if (pattern != null) {
                MatchResult match = s.match();
                if (match.groupCount() == 3) {
                    if (match.group(2) == null) {
                        System.out.println(isbn + ": copies available");
                    } else {
                        System.out.println(isbn + ": " + match.group(2) + " holds on " + match.group(3) + " copies");
                    }
                }
            } else {
                System.out.println(isbn + ": no match");
            }
            get.abort();
        }
    }
}
