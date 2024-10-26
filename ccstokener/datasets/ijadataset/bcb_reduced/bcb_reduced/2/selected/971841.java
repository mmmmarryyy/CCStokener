package udf.web;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.pig.EvalFunc;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

public class FetchTagsRanked extends EvalFunc<DataBag> {

    BagFactory mBagFactory = BagFactory.getInstance();

    public static LinkedList<String[]> getSuggestions(HashSet<String> query, Hashtable<String, Float> seeds, int n) throws IOException {
        LinkedList<String[]> list = new LinkedList<String[]>();
        URL url;
        URLConnection urlConnection;
        DataOutputStream outStream;
        String query_text = setToString(query);
        String seeds_text = hashToString(seeds);
        String body = "query=" + URLEncoder.encode(query_text, "UTF-8") + "&seeds=" + URLEncoder.encode(seeds_text, "UTF-8") + "&n=" + n;
        url = new URL("http://130.89.13.204:8080/rankService/RankServlet?" + body);
        urlConnection = url.openConnection();
        ((HttpURLConnection) urlConnection).setRequestMethod("GET");
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        urlConnection.setUseCaches(false);
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        urlConnection.setRequestProperty("Content-Length", "" + body.length());
        outStream = new DataOutputStream(urlConnection.getOutputStream());
        InputStreamReader rea = new InputStreamReader(urlConnection.getInputStream());
        BufferedReader d = new BufferedReader(rea);
        outStream.writeBytes(body);
        outStream.flush();
        outStream.close();
        String buffer;
        while ((buffer = d.readLine()) != null) {
            String temp[] = buffer.split("\t");
            list.add(temp);
        }
        return list;
    }

    public static String setToString(HashSet<String> set) {
        String temp = "";
        Iterator<String> iter = set.iterator();
        while (iter.hasNext()) {
            String tag = iter.next();
            temp = temp + tag + "\t";
        }
        return temp.trim();
    }

    public static String hashToString(Hashtable<String, Float> set) {
        String temp = "";
        Enumeration<String> enu = set.keys();
        while (enu.hasMoreElements()) {
            String tag = enu.nextElement();
            Float freq = set.get(tag);
            temp = temp + tag + " " + freq + "\t";
        }
        return temp.trim();
    }

    public static void main(String args[]) throws IOException {
        URL url;
        URLConnection urlConnection;
        DataOutputStream outStream;
        String body = "query=" + URLEncoder.encode("elmo\ttoys\tgames", "UTF-8") + "&seeds=" + URLEncoder.encode("games 10\ttoys 100\t", "UTF-8") + "&n=100";
        url = new URL("http://130.89.13.204:8080/rankService/RankServlet?" + body);
        urlConnection = url.openConnection();
        ((HttpURLConnection) urlConnection).setRequestMethod("POST");
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        urlConnection.setUseCaches(false);
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        urlConnection.setRequestProperty("Content-Length", "" + body.length());
        outStream = new DataOutputStream(urlConnection.getOutputStream());
        InputStreamReader rea = new InputStreamReader(urlConnection.getInputStream());
        BufferedReader d = new BufferedReader(rea);
        outStream.writeBytes(body);
        outStream.flush();
        outStream.close();
        String buffer;
        while ((buffer = d.readLine()) != null) {
            System.out.println(buffer);
        }
    }

    @Override
    public DataBag exec(Tuple tuple) throws IOException {
        DataBag bag_query = (DataBag) tuple.get(0);
        DataBag bag_seeds = (DataBag) tuple.get(1);
        Hashtable<String, Float> seeds = initSeeds(bag_seeds);
        HashSet<String> query = initQuery(bag_query);
        String n_top_ = (String) tuple.get(2);
        int nn = Integer.valueOf(n_top_);
        LinkedList<String[]> suggestions = getSuggestions(query, seeds, nn);
        DataBag output = mBagFactory.newDefaultBag();
        for (int i = 0; i < suggestions.size(); i++) {
            Tuple t = TupleFactory.getInstance().newTuple();
            String item[] = suggestions.get(i);
            t.append(item[0]);
            t.append(item[1]);
            t.append(i + 1);
            output.add(t);
        }
        suggestions.clear();
        return output;
    }

    private HashSet<String> initQuery(DataBag bag) {
        HashSet<String> set = new HashSet<String>();
        Iterator<Tuple> iterator = bag.iterator();
        while (iterator.hasNext()) {
            Tuple tup = iterator.next();
            try {
                String tag = (String) tup.get(0);
                set.add(tag);
            } catch (ExecException e) {
                e.printStackTrace();
            }
        }
        return set;
    }

    private Hashtable<String, Float> initSeeds(DataBag bagSeeds) {
        Iterator<Tuple> iterator = bagSeeds.iterator();
        Hashtable<String, Float> hash = new Hashtable<String, Float>();
        while (iterator.hasNext()) {
            Tuple tup = iterator.next();
            try {
                String freq_text = (String) tup.get(1);
                String tag = (String) tup.get(0);
                Float f = Float.valueOf(freq_text);
                hash.put(tag, f);
            } catch (ExecException e) {
                e.printStackTrace();
            }
        }
        return hash;
    }
}

class Item {

    String tag = null;

    Float score = null;

    int pos = -1;
}
