package org.meruvian.midas.android.geneology;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.meruvian.midas.android.geneology.entity.Person;
import org.meruvian.midas.android.geneology.manager.ConnectionManager;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

public class Profile extends Activity {

    EditText firstName, lastName, address, city, state, country, mobile, email;

    RadioButton laki, perem;

    boolean jkValue;

    ConnectionManager connectionManager;

    private SharedPreferences prefs;

    Person person, fromIntent;

    Button button;

    private TextView mDateDisplay;

    private Button mPickDate;

    private int mYear;

    private int mMonth;

    private int mDay;

    static final int DATE_DIALOG_ID = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.updateform);
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        TextView listTitle = (TextView) findViewById(R.id.listTitle);
        listTitle.setText("   Profile");
        ImageView imageView = (ImageView) findViewById(R.id.ImageView01);
        imageView.setBackgroundResource(R.drawable.male);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.my_title);
        Bundle bundle = getIntent().getExtras();
        connectionManager = new ConnectionManager();
        fromIntent = (Person) bundle.getSerializable("myprofile");
        person = connectionManager.getPersonalData(prefs.getString("connection", "") + "person/" + fromIntent.getId() + "/.json");
        firstName = (EditText) findViewById(R.id.fname);
        firstName.setText(person.getFirstName());
        lastName = (EditText) findViewById(R.id.lname);
        lastName.setText(person.getLastName());
        address = (EditText) findViewById(R.id.address);
        address.setText(person.getAddress());
        city = (EditText) findViewById(R.id.city);
        city.setText(person.getCity());
        state = (EditText) findViewById(R.id.state);
        state.setText(person.getState());
        country = (EditText) findViewById(R.id.country);
        country.setText(person.getCountry());
        mobile = (EditText) findViewById(R.id.mobile);
        mobile.setText(person.getMobile());
        email = (EditText) findViewById(R.id.email);
        email.setText(person.getEmail());
        laki = (RadioButton) findViewById(R.id.laki);
        perem = (RadioButton) findViewById(R.id.perem);
        button = (Button) findViewById(R.id.submit);
        laki.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                jkValue = true;
            }
        });
        perem.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                jkValue = false;
            }
        });
        if (person.getGender().equals("true")) {
            laki.setChecked(true);
        } else {
            perem.setChecked(true);
        }
        button.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                HttpPost httpPost = new HttpPost(prefs.getString("connection", "") + "person/" + person.getId() + "/");
                DefaultHttpClient httpClient = new DefaultHttpClient();
                try {
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                    nameValuePairs.add(new BasicNameValuePair("rstatus", "2"));
                    nameValuePairs.add(new BasicNameValuePair("firstName", firstName.getText().toString()));
                    nameValuePairs.add(new BasicNameValuePair("lastName", lastName.getText().toString()));
                    nameValuePairs.add(new BasicNameValuePair("address", address.getText().toString()));
                    nameValuePairs.add(new BasicNameValuePair("city", city.getText().toString()));
                    nameValuePairs.add(new BasicNameValuePair("state", state.getText().toString()));
                    nameValuePairs.add(new BasicNameValuePair("country", country.getText().toString()));
                    nameValuePairs.add(new BasicNameValuePair("mobile", mobile.getText().toString()));
                    nameValuePairs.add(new BasicNameValuePair("email", email.getText().toString()));
                    nameValuePairs.add(new BasicNameValuePair("birthDate", mDateDisplay.getText().toString()));
                    nameValuePairs.add(new BasicNameValuePair("_method", "put"));
                    nameValuePairs.add(new BasicNameValuePair("male", String.valueOf(jkValue)));
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
                    Intent in = new Intent(Profile.this, DashBoard.class);
                    startActivity(in);
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        mDateDisplay = (TextView) findViewById(R.id.date);
        mPickDate = (Button) findViewById(R.id.btnDate);
        mPickDate.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                showDialog(DATE_DIALOG_ID);
            }
        });
        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        updateDisplay();
        findViewById(R.id.homeBtn).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Profile.this, DashBoard.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.preferenceBtn).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Profile.this, Preference.class);
                startActivity(intent);
            }
        });
    }

    private void updateDisplay() {
        DateFormat dateFormat = new SimpleDateFormat("dd/mm/yyyy");
        mDateDisplay.setText(dateFormat.format(person.getBirthDate()));
    }

    private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            mYear = year;
            mMonth = monthOfYear;
            mDay = dayOfMonth;
            mDateDisplay.setText(new StringBuilder().append(mDay).append("-").append(mMonth + 1).append("-").append(mYear).append(" "));
        }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case DATE_DIALOG_ID:
                return new DatePickerDialog(this, mDateSetListener, mYear, mMonth, mDay);
        }
        return null;
    }
}
