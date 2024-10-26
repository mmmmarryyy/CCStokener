package ve.usb.buscame;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
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
import android.widget.LinearLayout;
import android.widget.TextView;

public class Login extends Activity {

    boolean activado = false;

    static String usuario = "";

    TextView txt;

    String nombre;

    String clave;

    int control;

    String result = "";

    public static final String ipServidor = "192.168.1.105";

    public static final String auth = "http://" + ipServidor + "/login.php";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        LinearLayout rootLayout = new LinearLayout(getApplicationContext());
        txt = new TextView(getApplicationContext());
        rootLayout.addView(txt);
    }

    private String getServerData(String returnString) {
        InputStream is = null;
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("nombre", nombre));
        nameValuePairs.add(new BasicNameValuePair("clave", clave));
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(auth);
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
            Log.d("Login", "Funciono http connection ");
        } catch (Exception e) {
            Log.e("Login", "Error en conexion http " + e.toString());
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            String line = reader.readLine();
            is.close();
            result = line.trim();
            Log.d("Login", "Longitud line: " + line.trim().length());
        } catch (Exception e) {
            Log.e("Login", "Error convirtiendo resultado: " + e.toString());
        }
        Log.d("Login", "Funciono Json: " + result);
        Log.d("Login", "Longitud: " + result.trim().length());
        returnString += result;
        control = 99;
        if (result.trim().equals("1")) {
            control = 1;
        } else {
            control = 0;
        }
        Log.d("Login", "Control: " + control);
        return returnString;
    }

    protected void onResume() {
        super.onResume();
        final Button boton = (Button) findViewById(R.id.btnEnviar);
        boton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                txt.setText("Conectando...");
                EditText et = (EditText) findViewById(R.id.textoUsuario);
                nombre = et.getText().toString();
                et.setText(nombre);
                EditText password = (EditText) findViewById(R.id.textoContrasena);
                clave = password.getText().toString();
                password.setText(clave);
                txt.setText(getServerData(auth));
                Log.d("Login", "Estoy en el boton.");
                if (control == 1) {
                    Intent i = new Intent(v.getContext(), Principal.class);
                    startActivityForResult(i, 0);
                    Context context = getApplicationContext();
                    usuario = et.getText().toString().trim();
                    CharSequence text = "Bienvenido " + usuario;
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, text, duration);
                    et.setText("");
                    password.setText("");
                } else {
                    Context context = getApplicationContext();
                    usuario = et.getText().toString().trim();
                    CharSequence text = "Datos incorrectos ";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                    et.setText("");
                    password.setText("");
                }
            }
        });
        final Button btnCancel = (Button) findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                EditText et = (EditText) findViewById(R.id.textoUsuario);
                et.setText("");
                EditText password = (EditText) findViewById(R.id.textoContrasena);
                password.setText("");
            }
        });
    }
}
