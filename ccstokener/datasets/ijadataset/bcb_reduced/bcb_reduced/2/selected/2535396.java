package brc;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * Fetches pubmed documents in Medline format taken from Pubfetch.
 */
public class FetchPubmed {

    /**
     * Returns a list of Pubmed ids that succesfully match the
     * query */
    public List fetchID(String query, int max) {
        List pubmed_ids = new ArrayList();
        try {
            String quoted_query = java.net.URLEncoder.encode(query, "UTF-8");
            BufferedReader in;
            String url = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term=" + quoted_query + "&retmax=" + max;
            in = new BufferedReader(new InputStreamReader((new URL(url)).openStream()));
            String inputLine = "";
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith("		<Id>")) {
                    StringTokenizer st = new StringTokenizer(inputLine, "<>/ ");
                    while (st.hasMoreTokens()) {
                        String tok = st.nextToken();
                        if (!(tok.startsWith("I"))) {
                            tok = tok.trim();
                            if (tok.length() > 0 && !pubmed_ids.contains(tok)) {
                                pubmed_ids.add(tok);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return pubmed_ids;
    }

    /** Given a list of ids, returns documents as a list.  Each
     * document is a separate String in medline format.
     * Arguments: ids: list of pubmed ids.
     */
    public List fetchFile(List ids) {
        BufferedReader in;
        List documents = new ArrayList();
        String url = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=" + join(",", ids) + "&retmode=MEDLINE&rettype=MEDLINE";
        StringBuffer buffer = new StringBuffer();
        try {
            in = new BufferedReader(new InputStreamReader((new URL(url)).openStream()));
            String inputLine = "";
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.equals("")) {
                    documents.add(buffer.toString());
                    buffer = new StringBuffer();
                } else {
                    buffer.append(inputLine);
                    buffer.append("\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return documents;
    }

    /**
     * Joins a list of strings together using a delimiter 'delim'.
     */
    public static String join(String delim, java.util.List v) {
        if (v.size() == 0) {
            return "";
        }
        if (v.size() == 1) {
            return "" + v.get(0);
        }
        StringBuffer result = new StringBuffer("" + v.get(0));
        for (int i = 1; i < v.size(); i++) {
            result.append(delim);
            result.append("" + v.get(i));
        }
        return result.toString();
    }

    /** Quicky function to run the pubmed
     */
    public static void main(String[] args) {
        String query = args[0];
        int max = Integer.parseInt(args[1]);
        FetchPubmed fetcher = new FetchPubmed();
        List ids = fetcher.fetchID(query, max);
        List docs = fetcher.fetchFile(ids);
        for (int i = 0; i < docs.size(); i++) {
            System.out.println(docs.get(i));
        }
    }
}
