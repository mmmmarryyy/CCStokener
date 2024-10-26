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
import android.widget.TextView;

public class MaintenanceDetail extends ListActivity {

    MaintenanceDetailAdapter adapter;

    final Handler mHandler = new Handler();

    private ArrayList<MaintenanceDetailItem> mResults;

    private String mLink = "";

    private String cookie = "";

    ProgressDialog progressDialog;

    TextView emptyMessage;

    final Runnable mUpdateResults = new Runnable() {

        public void run() {
            updateResultsInUi();
        }
    };

    private void updateResultsInUi() {
        doMaintenanceDetailUpdate(mLink, getmResults());
        progressDialog.dismiss();
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new MaintenanceDetailAdapter(this, android.R.layout.simple_list_item_1);
        setListAdapter(adapter);
        emptyMessage = (TextView) findViewById(R.id.emptyMaintenanceMessage);
    }

    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (intent.getExtras().containsKey("com.vanderbie.tjoektjoek.maintenance.link") && intent.getExtras().containsKey("com.vanderbie.tjoektjoek.maintenance.cookie")) {
            progressDialog = ProgressDialog.show(MaintenanceDetail.this, "", getString(R.string.loading));
            String link = (String) intent.getExtras().getString("com.vanderbie.tjoektjoek.maintenance.link");
            cookie = (String) intent.getExtras().getString("com.vanderbie.tjoektjoek.maintenance.cookie");
            mLink = link;
            Thread t = new Thread() {

                public void run() {
                    setmResults(getMaintenanceDetailFromRemote(mLink));
                    mHandler.post(mUpdateResults);
                }
            };
            t.start();
        }
    }

    public void onPause() {
        super.onPause();
    }

    private boolean doMaintenanceDetailUpdate(String location, ArrayList<MaintenanceDetailItem> maintenanceDetail) {
        if (!adapter.isEmpty()) {
            adapter.clear();
        }
        if (maintenanceDetail.size() == 0) {
            emptyMessage.setText(this.getString(R.string.no_maintenance));
            return false;
        } else {
            setTitle(this.getString(R.string.title) + " / " + getString(R.string.maintenance) + "(" + maintenanceDetail.size() + ")");
            for (MaintenanceDetailItem maintenanceDetailItem : maintenanceDetail) {
                adapter.add(maintenanceDetailItem);
            }
            return true;
        }
    }

    ArrayList<MaintenanceDetailItem> getMaintenanceDetailFromRemote(String linkString) {
        try {
            InputStream is = doPost(linkString, "");
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            MaintenanceDetailHandler maintenanceDetailHandler = new MaintenanceDetailHandler();
            xr.setContentHandler(maintenanceDetailHandler);
            xr.parse(new InputSource(is));
            MaintenanceDetailDataSet maintenanceDetailDataSet = maintenanceDetailHandler.getParsedData();
            return maintenanceDetailDataSet.getMaintenanceDetailItem();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<MaintenanceDetailItem>();
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
        HttpURLConnection.setFollowRedirects(false);
        con.setRequestProperty("Cookie", "JSESSIONID=36CBFFF9BB5FC07A0D1594149E571B87; rl-sticky-key=7b368df7d0ec06acf66c7767d7b7c1a0; rl-sticky-key-0c=92bdb2f298601672168bbbae09f5a220");
        con.setRequestProperty("id", urlString.substring(urlString.indexOf("?id=") + 4));
        con.connect();
        out = con.getOutputStream();
        buff = content.getBytes("UTF8");
        out.write(buff);
        out.flush();
        out.close();
        in = con.getInputStream();
        return in;
    }

    public void setmResults(ArrayList<MaintenanceDetailItem> mResults) {
        this.mResults = mResults;
    }

    public ArrayList<MaintenanceDetailItem> getmResults() {
        return mResults;
    }
}
