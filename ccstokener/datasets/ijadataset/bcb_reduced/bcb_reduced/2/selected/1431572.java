package ve.usb.buscame;

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
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Pendientes extends Activity {

    TextView txt;

    String solicitado;

    String result = "";

    public static final String auth = "http://" + Login.ipServidor + "/pendientes.php";

    public static final String rechazado = "http://" + Login.ipServidor + "/rechazado.php";

    public static final String agregar = "http://" + Login.ipServidor + "/agregar.php";

    String selec = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recibidas);
        LinearLayout rootLayout = new LinearLayout(getApplicationContext());
        txt = new TextView(getApplicationContext());
        rootLayout.addView(txt);
    }

    private String getServerData(String returnString) {
        InputStream is = null;
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("solicitado", Login.usuario));
        Log.d("Pendientes", "Select: " + selec);
        nameValuePairs.add(new BasicNameValuePair("solicita", selec));
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(returnString);
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
            Log.d("Pendientes", "Funciono enviadas http connection ");
        } catch (Exception e) {
            Log.e("Pendientes", "Error en conexion http " + e.toString());
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            String line = reader.readLine();
            is.close();
            result = line.trim().toString();
            Log.d("Pendientes", "Longitud line: " + line.trim().length());
        } catch (Exception e) {
            Log.e("Pendientes", "Error convirtiendo resultado: " + e.toString());
        }
        Log.d("Pendientes", "Funciono Json: " + result);
        return result;
    }

    protected void onResume() {
        super.onResume();
        txt.setText(getServerData(auth));
        String finale = getServerData(auth).replace("[", "").replace("]", "").replace("\"", "");
        String[] f = finale.split(",");
        final Spinner spinner = (Spinner) findViewById(R.id.spinnerRecibidos);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, f);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);
        final Button boton = (Button) findViewById(R.id.btnAgregar);
        boton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                selec = spinner.getSelectedItem().toString();
                txt.setText(getServerData(agregar));
                Log.d("Pendientes", "Agregar envia Json: " + result);
                Context context = getApplicationContext();
                CharSequence text;
                int duration;
                Toast toast;
                if (!selec.equals("")) {
                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    finish();
                    duration = Toast.LENGTH_SHORT;
                    text = selec + " fue agregado a tu lista de amigos!";
                    toast = Toast.makeText(context, text, duration);
                    toast.show();
                } else {
                    duration = Toast.LENGTH_SHORT;
                    text = "Seleccione una solicitud...";
                    toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
            }
        });
        final Button btnCancel = (Button) findViewById(R.id.btnRechazar);
        btnCancel.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                selec = spinner.getSelectedItem().toString();
                Log.d("Pendientes", "Valor de spinner seleccionado" + selec);
                result = "";
                txt.setText(getServerData(rechazado));
                Log.d("Pendientes", "Rechaza envia JSON ###########" + result);
                Context context = getApplicationContext();
                CharSequence text;
                int duration;
                Toast toast;
                if (!selec.equals("")) {
                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    finish();
                    duration = Toast.LENGTH_SHORT;
                    text = selec + " fue rechazado de su lista de amigos!";
                    toast = Toast.makeText(context, text, duration);
                    toast.show();
                } else {
                    duration = Toast.LENGTH_SHORT;
                    text = "Seleccione la solicitud que desea rechazar...";
                    toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
            }
        });
    }
}
