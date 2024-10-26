package com.vanderbie.reisplanner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
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

public class DepartureTimes extends ListActivity {

    DepartureTimesAdapter adapter;

    final Handler mHandler = new Handler();

    private ArrayList<DepartureTime> mResults;

    private String mLocation;

    ProgressDialog progressDialog;

    TextView emptyMessage;

    final Runnable mUpdateResults = new Runnable() {

        public void run() {
            updateResultsInUi();
        }
    };

    private void updateResultsInUi() {
        doDepartureTimesUpdate(mLocation, getmResults());
        progressDialog.dismiss();
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new DepartureTimesAdapter(this, android.R.layout.simple_list_item_1);
        setListAdapter(adapter);
        emptyMessage = (TextView) findViewById(R.id.emptyMessage);
    }

    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (intent.getExtras().containsKey("com.vanderbie.tjoektjoek.station")) {
            progressDialog = ProgressDialog.show(DepartureTimes.this, "", getString(R.string.loading));
            String location = (String) intent.getExtras().getString("com.vanderbie.tjoektjoek.station");
            mLocation = location;
            Thread t = new Thread() {

                public void run() {
                    setmResults(getDepartureTimesFromRemote(mLocation));
                    mHandler.post(mUpdateResults);
                }
            };
            t.start();
        }
    }

    public void onPause() {
        super.onPause();
    }

    private boolean doDepartureTimesUpdate(String location, ArrayList<DepartureTime> departureTimes) {
        if (!adapter.isEmpty()) {
            adapter.clear();
        }
        if (departureTimes.size() == 0) {
            emptyMessage.setText(this.getString(R.string.no_schedule) + " \"" + location + "\".");
            return false;
        } else {
            setTitle(this.getString(R.string.title) + " / " + location);
            for (DepartureTime departureTime : departureTimes) {
                adapter.add(departureTime);
            }
            return true;
        }
    }

    ArrayList<DepartureTime> getDepartureTimesFromRemote(String stationString) {
        try {
            InputStream is = doPost("http://m.ns.nl/actvertrektijden.action?from=" + URLEncoder.encode(stationString), "");
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            DepartureTimesHandler departureTimesHandler = new DepartureTimesHandler();
            xr.setContentHandler(departureTimesHandler);
            xr.parse(new InputSource(is));
            DepartureTimesDataSet departureTimesDataSet = departureTimesHandler.getParsedData();
            return departureTimesDataSet.getDepartureTimes();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<DepartureTime>();
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
        in = con.getInputStream();
        return in;
    }

    public void setmResults(ArrayList<DepartureTime> mResults) {
        this.mResults = mResults;
    }

    public ArrayList<DepartureTime> getmResults() {
        return mResults;
    }
}
