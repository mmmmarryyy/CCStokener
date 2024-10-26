package info.clockworksapple.android.barsearch.app;

import info.clockworksapple.android.barsearch.R;
import info.clockworksapple.android.barsearch.relax.station.line.LineResponse;
import info.clockworksapple.android.barsearch.relax.station.station.Station;
import info.clockworksapple.android.barsearch.relax.station.station.StationResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.xml.sax.SAXException;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

public class MainMenuActivity extends Activity {

    private Button mSearchStation;

    private Button mSearchLocation;

    private String prefecture = null;

    private String line = null;

    private String station = null;

    private StationResponse stationRes = null;

    private Station selectStation = null;

    private String selectType = "location";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);
        final TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();
        final TabSpec content1 = tabHost.newTabSpec("tab1");
        content1.setIndicator(getString(R.string.tab_menu_01), new BitmapDrawable(BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_mylocation)));
        content1.setContent(R.id.tab1);
        tabHost.addTab(content1);
        final TabSpec content2 = tabHost.newTabSpec("tab2");
        content2.setIndicator(getString(R.string.tab_menu_02), new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.train)));
        content2.setContent(R.id.tab2);
        tabHost.addTab(content2);
        tabHost.setOnTabChangedListener(new OnTabChangeListener() {

            @Override
            public void onTabChanged(String tabId) {
                if (tabId.equals("tab1")) {
                    selectType = "location";
                } else if (tabId.equals("tab2")) {
                    selectType = "station";
                } else {
                    selectType = "location";
                }
            }
        });
        final ArrayAdapter<String> prefAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        final ArrayAdapter<String> lineAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        final ArrayAdapter<String> statAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        final ArrayAdapter<String> typeAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        final ArrayAdapter<String> typeAdapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        prefAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prefAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinPref = (Spinner) findViewById(R.id.prefecture);
        Spinner spinLine = (Spinner) findViewById(R.id.line);
        Spinner spinStat = (Spinner) findViewById(R.id.station);
        Spinner spinType1 = (Spinner) findViewById(R.id.type_01);
        Spinner spinType2 = (Spinner) findViewById(R.id.type_02);
        spinLine.setAdapter(lineAdapter);
        spinStat.setAdapter(statAdapter);
        spinPref.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mSearchStation.setEnabled(false);
                Spinner spinner = (Spinner) arg0;
                prefecture = (String) spinner.getSelectedItem();
                String[] lines = getLine(prefecture);
                if (null == lines || lines.length == 0) {
                    return;
                }
                Spinner spinLine = (Spinner) findViewById(R.id.line);
                @SuppressWarnings("unchecked") ArrayAdapter<String> lineAdapter = (ArrayAdapter<String>) spinLine.getAdapter();
                lineAdapter.clear();
                Spinner spinStat = (Spinner) findViewById(R.id.station);
                @SuppressWarnings("unchecked") ArrayAdapter<String> statAdapter = (ArrayAdapter<String>) spinStat.getAdapter();
                statAdapter.clear();
                for (int j = 0; j < lines.length; j++) {
                    lineAdapter.add(lines[j]);
                }
                if (lines.length == 0) {
                    return;
                }
                getStation(prefecture, lines[0]);
                if (stationRes.getStation().length > 0) {
                    statAdapter.clear();
                }
                for (int k = 0; k < stationRes.sizeStation(); k++) {
                    statAdapter.add(stationRes.getStation(k).getName());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        spinLine.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mSearchStation.setEnabled(false);
                Spinner spinner = (Spinner) arg0;
                line = (String) spinner.getSelectedItem();
                getStation(prefecture, line);
                Spinner spinStat = (Spinner) findViewById(R.id.station);
                @SuppressWarnings("unchecked") ArrayAdapter<String> statAdapter = (ArrayAdapter<String>) spinStat.getAdapter();
                if (stationRes.getStation().length > 0) {
                    statAdapter.clear();
                }
                for (int k = 0; k < stationRes.sizeStation(); k++) {
                    statAdapter.add(stationRes.getStation(k).getName());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                System.out.println("aaaaaaaa");
            }
        });
        spinStat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Spinner spinner = (Spinner) arg0;
                station = (String) spinner.getSelectedItem();
                selectStation = getSelectedStation(station);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        mSearchLocation = (Button) findViewById(R.id.button_search_01);
        mSearchLocation.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                Spinner spinType = (Spinner) findViewById(R.id.type_01);
                String barType = getBarType((String) spinType.getSelectedItem());
                Intent intent = new Intent(getApplicationContext(), BarMapActivity.class);
                intent.putExtra("barType", barType);
                intent.putExtra("selectType", selectType);
                intent.setAction(Intent.ACTION_VIEW);
                startActivity(intent);
            }
        });
        mSearchStation = (Button) findViewById(R.id.button_search_02);
        mSearchStation.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                Spinner spinStatiton = (Spinner) findViewById(R.id.station);
                selectStation = getSelectedStation((String) spinStatiton.getSelectedItem());
                Spinner spinType = (Spinner) findViewById(R.id.type_02);
                String barType = getBarType((String) spinType.getSelectedItem());
                Intent intent = new Intent(getApplicationContext(), BarMapActivity.class);
                intent.putExtra("prefecture", selectStation.getPrefectureAsString());
                intent.putExtra("line", selectStation.getLineAsString());
                intent.putExtra("station", selectStation.getNameAsString());
                intent.putExtra("lng", selectStation.getXAsString());
                intent.putExtra("lat", selectStation.getYAsString());
                intent.putExtra("barType", barType);
                intent.putExtra("selectType", selectType);
                intent.setAction(Intent.ACTION_VIEW);
                startActivity(intent);
            }
        });
        mSearchStation.setEnabled(false);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String defaultSearchType = prefs.getString(getString(R.string.pref_search_type_key), "0");
        String defaultBarType = prefs.getString(getString(R.string.pref_bar_type_key), "0");
        String defaultPrefecture = prefs.getString(getString(R.string.pref_prefecture_key), "0");
        tabHost.setCurrentTab(Integer.parseInt(defaultSearchType));
        spinType1.setSelection(Integer.parseInt(defaultBarType));
        spinType2.setSelection(Integer.parseInt(defaultBarType));
        spinPref.setSelection(Integer.parseInt(defaultPrefecture));
    }

    /**
	 *
	 * @param prefecture
	 * @return
	 */
    public String[] getLine(String prefecture) {
        HttpClient httpclient = null;
        String[] lines = null;
        try {
            httpclient = new DefaultHttpClient();
            List<NameValuePair> qparams = new ArrayList<NameValuePair>();
            qparams.add(new BasicNameValuePair("method", "getLines"));
            qparams.add(new BasicNameValuePair("prefecture", prefecture));
            URI uri = URIUtils.createURI("http", "express.heartrails.com", -1, "/api/xml", URLEncodedUtils.format(qparams, "UTF-8"), null);
            HttpGet httpget = new HttpGet(uri);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            InputStream instream = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(instream, "UTF-8"));
            StringBuffer buf = new StringBuffer();
            String str;
            while ((str = reader.readLine()) != null) {
                buf.append(str);
                buf.append("\n");
            }
            reader.close();
            LineResponse res = new LineResponse(buf.toString());
            lines = res.getLineAsString();
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        } catch (ClientProtocolException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (SAXException ex) {
            ex.printStackTrace();
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
        return lines;
    }

    /**
	 *
	 * @param prefecture
	 * @param line
	 * @return
	 */
    public void getStation(String prefecture, String line) {
        HttpClient httpclient = null;
        try {
            httpclient = new DefaultHttpClient();
            List<NameValuePair> qparams = new ArrayList<NameValuePair>();
            qparams.add(new BasicNameValuePair("method", "getStations"));
            qparams.add(new BasicNameValuePair("prefecture", prefecture));
            qparams.add(new BasicNameValuePair("line", line));
            URI uri = URIUtils.createURI("http", "express.heartrails.com", -1, "/api/xml", URLEncodedUtils.format(qparams, "UTF-8"), null);
            HttpGet httpget = new HttpGet(uri);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            InputStream instream = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(instream, "UTF-8"));
            StringBuffer buf = new StringBuffer();
            String str;
            while ((str = reader.readLine()) != null) {
                buf.append(str);
                buf.append("\n");
            }
            reader.close();
            stationRes = new StationResponse(buf.toString());
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        } catch (ClientProtocolException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (SAXException ex) {
            ex.printStackTrace();
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        } finally {
            mSearchStation.setEnabled(true);
        }
    }

    /**
	 *
	 * @param station
	 * @return
	 */
    private Station getSelectedStation(String station) {
        Station stationObj = null;
        Station[] stations = stationRes.getStation();
        for (int i = 0; i < stations.length; i++) {
            if (stations[i].getName().equals(station)) {
                stationObj = stations[i];
            }
        }
        return stationObj;
    }

    /**
	 *
	 * @param selectedItem
	 * @return
	 */
    private String getBarType(String selectedItem) {
        String barType = "";
        if (selectedItem.equals(getString(R.string.str_bar_type_01))) {
            barType = "1";
        } else if (selectedItem.equals(getString(R.string.str_bar_type_02))) {
            barType = "2";
        } else if (selectedItem.equals(getString(R.string.str_bar_type_03))) {
            barType = "3";
        } else if (selectedItem.equals(getString(R.string.str_bar_type_04))) {
            barType = "4";
        } else {
            barType = "0";
        }
        return barType;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.options_menu_01, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.quit) {
            finish();
        } else if (item.getItemId() == R.id.preference) {
            Intent intent = new Intent();
            intent.setClassName(getString(R.string.package_name), getString(R.string.class_name_preference));
            startActivity(intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
