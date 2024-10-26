package com.vanderbie.reisplanner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class Maintenance extends ListActivity {

    MaintenanceAdapter adapter;

    final Handler mHandler = new Handler();

    private ArrayList<MaintenanceItem> mResults;

    ProgressDialog progressDialog;

    private String cookie = "";

    TextView emptyMessage;

    final Runnable mUpdateResults = new Runnable() {

        public void run() {
            updateResultsInUi();
        }
    };

    private void updateResultsInUi() {
        doDelayUpdate(getmResults());
        progressDialog.dismiss();
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.departuretimemain);
        adapter = new MaintenanceAdapter(this, android.R.layout.simple_list_item_1);
        setListAdapter(adapter);
        getListView().setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                String link = adapter.getItem(arg2).link;
                Intent maintenanceDetail = new Intent(Maintenance.this.getApplicationContext(), MaintenanceDetail.class);
                maintenanceDetail.putExtra("com.vanderbie.tjoektjoek.maintenance.link", link);
                maintenanceDetail.putExtra("com.vanderbie.tjoektjoek.maintenance.cookie", cookie);
                Maintenance.this.startActivity(maintenanceDetail);
            }
        });
        emptyMessage = (TextView) findViewById(R.id.emptyMessage);
    }

    protected void onResume() {
        super.onResume();
        progressDialog = ProgressDialog.show(Maintenance.this, "", getString(R.string.loading));
        Thread t = new Thread() {

            public void run() {
                setmResults(getDelayFromRemote());
                mHandler.post(mUpdateResults);
            }
        };
        t.start();
    }

    public void onPause() {
        super.onPause();
    }

    private boolean doDelayUpdate(ArrayList<MaintenanceItem> maintenance) {
        if (!adapter.isEmpty()) {
            adapter.clear();
        }
        setTitle(this.getString(R.string.title) + " / " + this.getString(R.string.maintenance) + "(" + maintenance.size() + ")");
        if (maintenance.size() == 0) {
            emptyMessage.setText(this.getString(R.string.no_maintenance));
            return false;
        } else {
            for (MaintenanceItem departureTime : maintenance) {
                adapter.add(departureTime);
            }
            return true;
        }
    }

    ArrayList<MaintenanceItem> getDelayFromRemote() {
        try {
            InputStream is = doPost("http://m.ns.nl/werkaanspoor.action", "");
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            MaintenanceHandler maintenanceHandler = new MaintenanceHandler();
            xr.setContentHandler(maintenanceHandler);
            xr.parse(new InputSource(is));
            MaintenanceDataSet maintenanceDataSet = maintenanceHandler.getParsedData();
            return maintenanceDataSet.getDelayItem();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<MaintenanceItem>();
    }

    private InputStream doPost(String urlString, String content) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        InputStream in = null;
        OutputStream out;
        byte[] buff;
        con.setRequestMethod("GET");
        con.setDoOutput(true);
        con.setDoInput(true);
        con.connect();
        out = con.getOutputStream();
        buff = content.getBytes("UTF8");
        out.write(buff);
        out.flush();
        out.close();
        Object[] cookieObject = con.getHeaderFields().get("set-cookie").toArray();
        cookie = (String) cookieObject[0] + " " + (String) cookieObject[1] + " " + (String) cookieObject[2];
        in = con.getInputStream();
        return in;
    }

    public void setmResults(ArrayList<MaintenanceItem> mResults) {
        this.mResults = mResults;
    }

    public ArrayList<MaintenanceItem> getmResults() {
        return mResults;
    }
}
