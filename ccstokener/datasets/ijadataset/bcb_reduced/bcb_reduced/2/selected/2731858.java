package ve.usb.buscame;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

public class AmigosPorGrupo extends ListActivity implements Runnable {

    static String seleccion;

    static String amigoABorrar = "";

    private ProgressDialog dialog;

    static String[] amigosPorGrupo;

    static final String mostrarAmigosPorGrupo = "http://" + Login.ipServidor + "/mostrarAmigosPorGrupo.php";

    static final String eliminarAmigoGrupoPhp = "http://" + Login.ipServidor + "/eliminarAmigoGrupo.php";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dialog = ProgressDialog.show(AmigosPorGrupo.this, "", "Cargando, por favor espere.", true);
        Thread thread = new Thread(this);
        thread.start();
    }

    public void onListItemClick(View v, int position, long id) {
        Toast.makeText(this, "Seleccionaste a:  " + amigosPorGrupo[position], Toast.LENGTH_SHORT).show();
        seleccion = amigosPorGrupo[position];
        Cliente.userConv = seleccion.trim();
        Log.d("AmigosPorGrupo", "seleccion = " + Cliente.userConv + ": " + Cliente.userConv.length());
        if (seleccion.trim().equals("Enviar mensaje a todos")) {
            startActivity(new Intent(this, Broadcast.class));
        } else {
            startActivity(new Intent(this, Conversacion.class));
        }
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        Log.d("AmigosPorGrupo", "onLongListItemClick id=" + id);
        amigoABorrar = amigosPorGrupo[pos];
        Log.d("AmigosPorGrupo", "Amigo a borrar: " + amigoABorrar);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Desea eliminar a '" + amigoABorrar + "' del grupo '" + MisGrupos.seleccion + "'?").setCancelable(false).setPositiveButton("Si", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                getServerData(eliminarAmigoGrupoPhp);
                CharSequence text = amigoABorrar + " fue eliminado del grupo " + MisGrupos.seleccion + "!";
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                AmigosPorGrupo.this.finish();
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
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
        nameValuePairs.add(new BasicNameValuePair("amigoABorrar", amigoABorrar.trim()));
        nameValuePairs.add(new BasicNameValuePair("grupo", MisGrupos.seleccion.trim()));
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(returnString);
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        } catch (Exception e) {
            Log.e("AmigosPorGrupo", "Error en la conexion http " + e.toString());
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            String line = reader.readLine();
            is.close();
            result = line.trim().toString();
            Log.d("AmigosPorGrupo", "Longitud line: " + line.trim().length());
        } catch (Exception e) {
            Log.e("AmigosPorGrupo", "Error converting result " + e.toString());
        }
        Log.d("AmigosPorGrupo", "Funciono json" + result);
        return result;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int indiceListaAmigos = 0;
        switch(keyCode) {
            case KeyEvent.KEYCODE_DEL:
                Log.d("AmigosPorGrupo", "Delete key hit " + keyCode);
                indiceListaAmigos = getSelectedItemPosition();
                amigoABorrar = amigosPorGrupo[indiceListaAmigos];
                Log.d("AmigosPorGrupo", "Amigo a borrar: " + amigoABorrar);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Desea eliminar a " + amigoABorrar + " del grupo " + MisGrupos.seleccion + "?").setCancelable(false).setPositiveButton("Si", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        getServerData(eliminarAmigoGrupoPhp);
                        AmigosPorGrupo.this.finish();
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
        alertbox.setMessage("No tienes amigos en este grupo");
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
            if (amigosPorGrupo.length > 1) {
                setListAdapter(new ArrayAdapter<String>(AmigosPorGrupo.this, R.layout.list_item, amigosPorGrupo));
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

    String[] concatArrays(String[] first, String[] second) {
        List<String> both = new ArrayList<String>(first.length + second.length);
        Collections.addAll(both, first);
        Collections.addAll(both, second);
        return both.toArray(new String[] {});
    }

    @Override
    public void run() {
        String[] titulo = new String[] { "Enviar mensaje a todos" };
        String stringUsuarios = getServerData(mostrarAmigosPorGrupo).replace("[", "").replace("]", "").replace("\"", "");
        String[] usuarios = stringUsuarios.split(",");
        if (usuarios[0] != "") {
            amigosPorGrupo = concatArrays(titulo, usuarios);
        } else {
            amigosPorGrupo = titulo;
        }
        Conversacion.amigosBroadcast = new ArrayList<String>(Arrays.asList(usuarios));
        usuarios = concatArrays(titulo, usuarios);
        handler.sendEmptyMessage(0);
    }
}
