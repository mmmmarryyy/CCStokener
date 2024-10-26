package org.meruvian.midas.android.geneology;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.meruvian.midas.android.geneology.adapter.PersonAdapter;
import org.meruvian.midas.android.geneology.entity.Person;
import org.meruvian.midas.android.geneology.manager.ConnectionManager;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class AddChild extends ListActivity {

    private int selectedItemId;

    private ArrayList<Person> persons;

    private ConnectionManager connectionManager;

    private Menu menu;

    private ProgressDialog myProgressDialog;

    private Bundle bundle;

    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        setContentView(R.layout.list);
        TextView listTitle = (TextView) findViewById(R.id.listTitle);
        listTitle.setText("Add Child");
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.my_title);
        connectionManager = new ConnectionManager();
        bundle = getIntent().getExtras();
        persons = connectionManager.getPersonList(prefs.getString("connection", "") + "parent/" + bundle.getString("id") + "/c.json");
        PersonAdapter personAdapter = new PersonAdapter(this, persons);
        getListView().setAdapter(personAdapter);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        getListView().setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedItemId = getListView().getCheckedItemPosition();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        this.menu = menu;
        addRegularMenuItems(menu);
        return true;
    }

    private void addRegularMenuItems(Menu menu) {
        int base = Menu.FIRST;
        MenuItem adding = menu.add(base, base, base, "Add");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            addChild(getIdList(selectedItemId));
            Intent in = new Intent(AddChild.this, Child.class);
            Person person = new Person();
            person = connectionManager.getPersonalData(prefs.getString("connection", "") + "person/" + bundle.getString("id") + "/.json");
            in.putExtra("myprofile", person);
            startActivity(in);
        }
        return true;
    }

    private void addChild(String id) {
        HttpPost httpPost = new HttpPost(prefs.getString("connection", "") + "parent/" + bundle.getString("id") + "/addC");
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("cid", id));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpClient.execute(httpPost);
            Log.i("Android JSON", response.getStatusLine().toString());
            Log.i("Sending data to", httpPost.getURI().toString());
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                String result = new ConnectionManager().convertStreamToString(instream);
                Log.i("Response", result);
                instream.close();
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] getNameList() {
        String data[] = new String[persons.size()];
        int index = 0;
        for (Person p : persons) {
            data[index] = p.getFirstName();
            index++;
        }
        return data;
    }

    private String getIdList(int selectedIndex) {
        int index = 0;
        String data[] = new String[persons.size()];
        for (Person p : persons) {
            data[index] = p.getId();
            index++;
        }
        return data[selectedIndex];
    }
}
