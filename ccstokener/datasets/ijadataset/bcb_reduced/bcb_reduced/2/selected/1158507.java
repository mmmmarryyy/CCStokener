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
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class AmigosTodos extends ListActivity implements Runnable {

    static String seleccion;

    static String amigoACopiar = "";

    private ProgressDialog dialog;

    static String[] amigosTodos;

    static final String mostrarOnlinePhp = "http://" + Login.ipServidor + "/mostrarOnOffline.php";

    static final String insertarAmigosPorGrupo = "http://" + Login.ipServidor + "/insertarAmigosPorGrupo.php";

    String grupoSeleccionado = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dialog = ProgressDialog.show(AmigosTodos.this, "", "Cargando, por favor espere...", true);
        Thread thread = new Thread(this);
        thread.start();
    }

    public void onListItemClick(View v, int position, long id) {
        seleccion = amigosTodos[position];
        Cliente.userConv = seleccion.trim();
        Log.d("AmigosTodos", "seleccion = " + Cliente.userConv + ": " + Cliente.userConv.length());
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        Log.d("TAG", "onLongListItemClick id=" + id);
        amigoACopiar = amigosTodos[pos];
        Log.d("AmigosTodos", "Amigo a agregar al grupo: " + amigoACopiar);
        final CharSequence[] items = MisGrupos.misGruposPersonales;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Agregar a grupo: ");
        builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int item) {
                CharSequence text = amigoACopiar + " agregado al grupo: " + items[item];
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                grupoSeleccionado = items[item].toString();
                Log.d("TAG", "onClick grupoSeleccionado: " + grupoSeleccionado);
                Log.d("TAG", "checking usuario: " + Login.usuario);
                Log.d("TAG", "checking amigoACopiar: " + amigoACopiar);
                getServerData(insertarAmigosPorGrupo);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
        return true;
    }

    private String getServerData(String returnString) {
        InputStream is = null;
        String result = "";
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("usuario", Login.usuario));
        nameValuePairs.add(new BasicNameValuePair("amigoACopiar", amigoACopiar.trim()));
        nameValuePairs.add(new BasicNameValuePair("grupoSeleccionado", grupoSeleccionado.trim()));
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(returnString);
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        } catch (Exception e) {
            Log.e("AmigosOnline", "Error en la conexion http " + e.toString());
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            String line = reader.readLine();
            is.close();
            result = line.trim().toString();
            Log.d("AmigosTodos", "Longitud line: " + line.trim().length());
        } catch (Exception e) {
            Log.e("log_tag", "Error converting result " + e.toString());
        }
        Log.d("AmigosTodos", "Funciono json" + result);
        return result;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int indiceListaAmigos = 0;
        switch(keyCode) {
            case KeyEvent.KEYCODE_DEL:
                Log.d("AmigosTodos", "Delete key hit " + keyCode);
                indiceListaAmigos = getSelectedItemPosition();
                amigoACopiar = amigosTodos[indiceListaAmigos];
                Log.d("AmigosTodos", "Amigo a borrar: " + amigoACopiar);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Desea eliminar a " + amigoACopiar + "?").setCancelable(false).setPositiveButton("Si", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        AmigosTodos.this.finish();
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void mensaje() {
        AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
        alertbox.setMessage("No tienes amigos todavia. ");
        alertbox.setNeutralButton("Ok", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface arg0, int arg1) {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        alertbox.show();
    }

    private Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            if (amigosTodos.length >= 1) {
                setListAdapter(new ArrayAdapter<String>(AmigosTodos.this, R.layout.list_item, amigosTodos));
            } else {
                mensaje();
            }
            ListView lv = getListView();
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
                    onListItemClick(v, pos, id);
                }
            });
            lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                    return onLongListItemClick(v, pos, id);
                }
            });
            dialog.dismiss();
        }
    };

    @Override
    public void run() {
        String stringUsuarios = getServerData(mostrarOnlinePhp).replace("[", "").replace("]", "").replace("\"", "");
        String[] usuarios = stringUsuarios.split(",");
        amigosTodos = usuarios;
        handler.sendEmptyMessage(0);
    }
}
