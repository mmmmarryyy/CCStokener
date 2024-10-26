package hu.donpedropizza;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class Register extends Activity {

    public static final String REGISTRATION_URL = "http://donpedropizza.hu/html/androidRegister.php";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
    }

    public void back(View view) {
        this.finish();
    }

    public void register(View view) {
        int result = getServerData(REGISTRATION_URL);
        Log.d("m", result + "");
        AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
        if (result == 0) {
            alertbox.setMessage(getString(R.string.registration_succes));
            alertbox.setNeutralButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int arg1) {
                    Register.this.finish();
                }
            });
        } else {
            String errorMessage = getString(R.string.registration_failed);
            if (result == 1) {
                errorMessage = getString(R.string.registration_already_exist);
            }
            if (result == 2) {
                errorMessage = getString(R.string.registration_email_invalid);
            }
            if (result == 3) {
                errorMessage = getString(R.string.registration_email_not_match);
            }
            if (result == 4) {
                errorMessage = getString(R.string.registration_missing_field);
            }
            alertbox.setMessage(errorMessage);
            alertbox.setNeutralButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int arg1) {
                    dialog.cancel();
                }
            });
        }
        alertbox.show();
    }

    private int getServerData(String returnString) {
        InputStream is = null;
        String result = "";
        String email = (((EditText) findViewById(R.id.email)).getText()).toString();
        String remail = (((EditText) findViewById(R.id.remail)).getText()).toString();
        String lastName = (((EditText) findViewById(R.id.last_name)).getText()).toString();
        String firstName = (((EditText) findViewById(R.id.first_name)).getText()).toString();
        String phoneNumber = (((EditText) findViewById(R.id.phonenumber)).getText()).toString();
        String zipCode = (((EditText) findViewById(R.id.zipcode)).getText()).toString();
        String city = (((EditText) findViewById(R.id.city)).getText()).toString();
        String address = (((EditText) findViewById(R.id.address)).getText()).toString();
        if (!(new EmailValidator().validate(email))) {
            return 2;
        }
        if (!(email.equals(remail))) {
            return 3;
        }
        if (email.length() <= 0 || remail.length() <= 0 || lastName.length() <= 0 || firstName.length() <= 0 || phoneNumber.length() <= 0 || zipCode.length() <= 0 || city.length() <= 0 || address.length() <= 0) {
            return 4;
        }
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("email", email));
        nameValuePairs.add(new BasicNameValuePair("lastName", lastName));
        nameValuePairs.add(new BasicNameValuePair("firstName", firstName));
        nameValuePairs.add(new BasicNameValuePair("phoneNumber", phoneNumber));
        nameValuePairs.add(new BasicNameValuePair("zipCode", zipCode));
        nameValuePairs.add(new BasicNameValuePair("city", city));
        nameValuePairs.add(new BasicNameValuePair("address", address));
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(REGISTRATION_URL);
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        } catch (Exception e) {
            Log.e("m", "1: " + e.getMessage());
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            result = sb.toString();
            Log.d("m", result);
        } catch (Exception e) {
            Log.e("m", "2: " + e.getMessage());
        }
        try {
            JSONArray jArray = new JSONArray(result);
            for (int i = 0; i < jArray.length(); i++) {
                JSONObject json_data = jArray.getJSONObject(i);
                if (json_data.getInt("error") == 0) {
                    return 0;
                } else if (json_data.getInt("error") == 1) {
                    return 1;
                }
            }
        } catch (JSONException e) {
            Log.e("m", "3: " + e.getMessage());
        }
        return 100;
    }
}
