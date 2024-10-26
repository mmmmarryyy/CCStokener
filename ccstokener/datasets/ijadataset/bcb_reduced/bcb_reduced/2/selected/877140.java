package ve.usb.buscame;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Enviadas extends ListActivity {

    TextView txt;

    String solicita;

    String result = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout rootLayout = new LinearLayout(getApplicationContext());
        txt = new TextView(getApplicationContext());
        rootLayout.addView(txt);
    }

    public static final String auth = "http://" + Login.ipServidor + "/enviadas.php";

    private String getServerData(String returnString) {
        InputStream is = null;
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("solicita", Login.usuario));
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(auth);
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
            Log.d("Enviadas", "Funciono enviadas, http connection ");
        } catch (Exception e) {
            Log.e("Enviadas", "Error en conexion http " + e.toString());
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            String line = reader.readLine();
            is.close();
            result = line.trim().toString();
            Log.d("Enviadas", "Longitud line: " + line.trim().length());
        } catch (Exception e) {
            Log.e("Enviadas", "Error convirtiendo el resultado: " + e.toString());
        }
        Log.d("Enviadas", "Funciono Json: " + result);
        return result;
    }

    protected void onResume() {
        super.onResume();
        txt.setText(getServerData(auth));
        String finale = getServerData(auth).replace("[", "").replace("]", "").replace("\"", "");
        String[] f = finale.split(",");
        if (f.length >= 1) {
            setListAdapter(new ArrayAdapter<String>(this, R.layout.enviadas, f));
        } else {
            mensaje();
        }
    }

    public void mensaje() {
        AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
        alertbox.setMessage("No tiene solicitudes enviadas");
        alertbox.setNeutralButton("Ok", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface arg0, int arg1) {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        alertbox.show();
    }
}
