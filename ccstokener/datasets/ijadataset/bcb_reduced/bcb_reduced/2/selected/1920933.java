package org.newgenlib.carbon;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Results extends Activity {

    private boolean loadingMore = false;

    private ArrayList alOnlyIds = new ArrayList();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.results);
        Bundle bs = getIntent().getExtras();
        final String text = bs.getString("Text");
        final String index = bs.getString("Index");
        alOnlyIds = new ArrayList();
        final ArrayList al = loadResults(text, index, 0);
        final ArrayAdapter<String> aa = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, al);
        ListView lview = (ListView) findViewById(R.id.searchResultsList);
        lview.setAdapter(aa);
        ListView lv = lview;
        lv.setTextFilterEnabled(true);
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                String[] idSelected = (String[]) alOnlyIds.get(position);
                System.out.println("id is: " + idSelected[0]);
                System.out.println("lib id is: " + idSelected[1]);
            }
        });
        lv.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int lastInScreen = firstVisibleItem + visibleItemCount;
                if ((lastInScreen == totalItemCount) && !(loadingMore)) {
                    ArrayAdapter aa1 = (ArrayAdapter) view.getAdapter();
                    ArrayList al2 = loadResults(text, index, totalItemCount);
                    for (int i = 0; i < al2.size(); i++) {
                        al.add(al2.get(i));
                    }
                    aa1.notifyDataSetChanged();
                }
            }
        });
    }

    private ArrayList loadResults(String text, String index, int from) {
        loadingMore = true;
        JSONObject job = new JSONObject();
        ArrayList al = new ArrayList();
        try {
            String req = job.put("OperationId", "2").toString();
            InputStream is = null;
            String result = "";
            JSONObject jArray = null;
            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost("http://192.168.1.4:8080/newgenlibctxt/CarbonServlet");
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair("OperationId", "2"));
                nameValuePairs.add(new BasicNameValuePair("Text", text));
                nameValuePairs.add(new BasicNameValuePair("Index", index));
                nameValuePairs.add(new BasicNameValuePair("From", from + ""));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();
                is = entity.getContent();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                is.close();
                result = sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                JSONObject jobres = new JSONObject(result);
                JSONArray jarr = jobres.getJSONArray("Records");
                for (int i = 0; i < jarr.length(); i++) {
                    String title = jarr.getJSONObject(i).getString("title");
                    String author = jarr.getJSONObject(i).getString("author");
                    String[] id = new String[2];
                    id[0] = jarr.getJSONObject(i).getString("cataloguerecordid");
                    id[1] = jarr.getJSONObject(i).getString("ownerlibraryid");
                    alOnlyIds.add(id);
                    al.add(Html.fromHtml("<html><body><b>" + title + "</b><br>by " + author + "</body></html>"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        loadingMore = false;
        return al;
    }
}

class BookData {

    private String title;

    private String author;

    private String catId;

    private String libId;

    private String getTitle() {
        return title;
    }

    private void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCatId() {
        return catId;
    }

    public void setCatId(String catId) {
        this.catId = catId;
    }

    public String getLibId() {
        return libId;
    }

    public void setLibId(String libId) {
        this.libId = libId;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
