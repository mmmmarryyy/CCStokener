package brc;

import java.util.*;
import java.io.*;
import java.net.*;

public class FetchAgricolaID {

    BufferedReader in;

    public List fetchID(String query, int max) {
        List results = new ArrayList();
        int i = 0;
        while (i < max) {
            List newIds = fetchIdsOfPage("rice+AND+wheat", i);
            for (int j = 0; j < newIds.size(); j++) {
                results.add(newIds.get(j));
                i++;
                if (i >= max) {
                    break;
                }
            }
        }
        return results;
    }

    private List fetchIdsOfPage(String query, int start) {
        String url = "http://www.nal.usda.gov/cgi-bin/agricola-ind?searchtype=keyword&searcharg=" + query + "&startnum=" + start;
        List agricolaIds = new ArrayList();
        try {
            in = new BufferedReader(new InputStreamReader((new URL(url)).openStream()));
            in.readLine();
            String inputLine = "";
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith("<tr> <td")) {
                    StringTokenizer st = new StringTokenizer(inputLine, "<>");
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        if (token.startsWith("a href")) {
                            String bib = token.substring(token.indexOf("?") + 5, token.indexOf("&"));
                            if (!agricolaIds.contains(bib)) {
                                agricolaIds.add(bib);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return agricolaIds;
    }

    public static void main(String[] args) {
        System.out.println("Start");
        FetchAgricolaID fm = new FetchAgricolaID();
        List ids = fm.fetchID("rice+AND+wheat", 10);
    }
}
