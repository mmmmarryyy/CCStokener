package com.ringord.reader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class NewsReaderActivity extends Activity {

    ListView list;

    ArrayList<Data> news;

    NewsAdapter adapter;

    static boolean title;

    static boolean link;

    static boolean description;

    static final String TAG = "NewsReader";

    DBAdapter db;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        list = (ListView) findViewById(R.id.list);
        db = new DBAdapter(this);
        news = new ArrayList<Data>();
        adapter = new NewsAdapter(news);
        list.setAdapter(adapter);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = null;
        DefaultHandler handler = null;
        try {
            parser = factory.newSAXParser();
            handler = new DefaultHandler() {

                Data newsItem;

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    Log.d(TAG, qName);
                    if (qName.equals("item")) newsItem = new Data();
                    if (qName.equals("title")) title = true;
                    if (qName.equals("link")) link = true;
                    if (qName.equals("description")) description = true;
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    if (qName.equals("item")) news.add(newsItem);
                    if (qName.equals("title")) title = false;
                    if (qName.equals("link")) link = false;
                    if (qName.equals("description")) description = false;
                }

                @Override
                public void characters(char ch[], int start, int length) throws SAXException {
                    if (newsItem == null) {
                        return;
                    }
                    if (title) {
                        newsItem.setTitle(new String(ch, start, length));
                    }
                    if (link) {
                        newsItem.setLink(new String(ch, start, length));
                    }
                    if (description) {
                        newsItem.setDesc(new String(ch, start, length));
                    }
                }
            };
        } catch (ParserConfigurationException e1) {
            e1.printStackTrace();
        } catch (SAXException e1) {
            e1.printStackTrace();
        }
        Intent siteIntent = getIntent();
        String siteurl = siteIntent.getStringExtra("siteurl");
        URLConnection connection = null;
        URL url;
        try {
            url = new URL(siteurl);
            Log.i(TAG, "1");
            connection = url.openConnection();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "2");
        try {
            parser.parse(connection.getInputStream(), handler);
            Log.i(TAG, "3");
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        adapter.notifyDataSetChanged();
        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapt, View view, int position, long id) {
                String link;
                link = news.get(position).getLink();
                Intent intent = new Intent(NewsReaderActivity.this, WebViewActivity.class);
                intent.putExtra("link", link);
                startActivity(intent);
            }
        });
    }

    class NewsAdapter extends BaseAdapter {

        private ArrayList<Data> object;

        private static final String TAG = "NewsAdapter";

        public NewsAdapter(ArrayList<Data> object) {
            super();
            this.object = object;
        }

        @Override
        public int getCount() {
            return object.size();
        }

        @Override
        public Object getItem(int arg0) {
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            return 0;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(NewsReaderActivity.this);
                convertView = inflater.inflate(R.layout.listview_listrow, parent, false);
                holder = new ViewHolder();
                holder.vhTextView = (TextView) convertView.findViewById(R.id.txt_headline);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            String headLine = object.get(pos).getTitle();
            Log.d(TAG, headLine);
            holder.vhTextView.setText(headLine);
            return convertView;
        }
    }

    static class ViewHolder {

        TextView vhTextView;
    }
}
