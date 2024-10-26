package ve.usb.buscame;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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

public class MisGrupos extends ListActivity implements Runnable {

    static String seleccion;

    static String grupoABorrar = "";

    private ProgressDialog dialog;

    static String[] misGrupos;

    static String[] misGruposPersonales;

    static final String mostrarGrupos = "http://" + Login.ipServidor + "/mostrarGrupos.php";

    static final String eliminarGrupoPhp = "http://" + Login.ipServidor + "/eliminarGrupo.php";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dialog = ProgressDialog.show(MisGrupos.this, "", "Cargando, por favor espere.", true);
        Thread thread = new Thread(this);
        thread.start();
    }

    public void onListItemClick(View v, int position, long id) {
        Toast.makeText(this, "Seleccionaste a:  " + misGrupos[position], Toast.LENGTH_SHORT).show();
        seleccion = misGrupos[position];
        Cliente.userConv = seleccion.trim();
        Log.d("MisGrupos", "seleccion = " + Cliente.userConv + ": " + Cliente.userConv.length());
        if (Cliente.userConv.equals("Administrar-Todos")) {
            startActivityForResult(new Intent(this, AmigosTodos.class), 0);
        } else if (Cliente.userConv.equals("Online")) {
            startActivityForResult(new Intent(this, AmigosOnline.class), 0);
        } else if (Cliente.userConv.equals("Offline")) {
            startActivityForResult(new Intent(this, AmigosOffline.class), 0);
        } else if (Cliente.userConv.equals("GPS")) {
            startActivityForResult(new Intent(this, AmigosGPS.class), 0);
        } else if (Cliente.userConv.equals("GPS demo")) {
            startActivityForResult(new Intent(this, UseGps.class), 0);
        } else {
            startActivity(new Intent(this, AmigosPorGrupo.class));
        }
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        Log.d("TAG", "onLongListItemClick id=" + id);
        grupoABorrar = misGrupos[pos];
        Log.d("MisGrupos", "Grupo a borrar: " + grupoABorrar);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Desea eliminar a " + grupoABorrar + "?").setCancelable(false).setPositiveButton("Si", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                getServerData(eliminarGrupoPhp);
                MisGrupos.this.finish();
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        if (!grupoABorrar.equals("Administrar-Todos") && !grupoABorrar.equals("Online") && !grupoABorrar.equals("Offline") && !grupoABorrar.equals("GPS") && !grupoABorrar.equals("GPS demo")) {
            AlertDialog alert = builder.create();
            alert.show();
        }
        return true;
    }

    private String getServerData(String returnString) {
        InputStream is = null;
        String result = "";
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("usuario", Login.usuario));
        nameValuePairs.add(new BasicNameValuePair("grupoABorrar", grupoABorrar.trim()));
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(returnString);
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        } catch (Exception e) {
            Log.e("MisGrupos", "Error en la conexion http " + e.toString());
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            String line = reader.readLine();
            is.close();
            result = line.trim().toString();
            Log.d("MisGrupos", "Longitud line: " + line.trim().length());
        } catch (Exception e) {
            Log.e("log_tag", "Error converting result " + e.toString());
        }
        Log.d("MisGrupos", "Funciono json: " + result);
        return result;
    }

    private Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            setListAdapter(new ArrayAdapter<String>(MisGrupos.this, R.layout.list_item, misGrupos));
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
        String[] titulo = new String[] { "Administrar-Todos", "Online", "Offline", "GPS", "GPS demo" };
        String stringUsuarios = getServerData(mostrarGrupos).replace("[", "").replace("]", "").replace("\"", "");
        String[] grupos = stringUsuarios.split(",");
        misGruposPersonales = grupos;
        if (!grupos[0].equals("")) {
            misGrupos = concatArrays(titulo, grupos);
        } else {
            misGrupos = titulo;
        }
        handler.sendEmptyMessage(0);
    }
}
