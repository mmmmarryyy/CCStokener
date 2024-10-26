package ve.usb.buscame;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
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

public class AmigosOnline extends ListActivity implements Runnable {

    static String seleccion;

    static String amigoABorrar = "";

    private ProgressDialog dialog;

    static String[] amigosOnline;

    static final String mostrarOnlinePhp = "http://" + Login.ipServidor + "/mostrarOnOffline.php";

    static final String eliminarAmigoPhp = "http://" + Login.ipServidor + "/eliminarAmigo.php";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dialog = ProgressDialog.show(AmigosOnline.this, "", "Cargando, por favor espere.", true);
        Thread thread = new Thread(this);
        thread.start();
    }

    public void onListItemClick(View v, int position, long id) {
        Toast.makeText(this, "Seleccionaste a:  " + amigosOnline[position], Toast.LENGTH_SHORT).show();
        seleccion = amigosOnline[position];
        Cliente.userConv = seleccion.trim();
        Log.d("AmigosOnline", "seleccion = " + Cliente.userConv + ": " + Cliente.userConv.length());
        if (seleccion.trim().equals("Enviar mensaje a todos")) {
            startActivity(new Intent(this, Broadcast.class));
        } else {
            startActivity(new Intent(this, Conversacion.class));
        }
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        Log.d("TAG", "onLongListItemClick id=" + id);
        amigoABorrar = amigosOnline[pos];
        Log.d("AmigosOnline", "Amigo a borrar: " + amigoABorrar);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Desea eliminar a " + amigoABorrar + "?").setCancelable(false).setPositiveButton("Si", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                getServerData(eliminarAmigoPhp);
                AmigosOnline.this.finish();
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
            Log.d("AmigosOnline", "Longitud line: " + line.trim().length());
        } catch (Exception e) {
            Log.e("log_tag", "Error converting result " + e.toString());
        }
        Log.d("AmigosOnline", "Funciono json" + result);
        return result;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int indiceListaAmigos = 0;
        switch(keyCode) {
            case KeyEvent.KEYCODE_DEL:
                Log.d("AmigosOnline", "Delete key hit " + keyCode);
                indiceListaAmigos = getSelectedItemPosition();
                amigoABorrar = amigosOnline[indiceListaAmigos];
                Log.d("AmigosOnline", "Amigo a borrar: " + amigoABorrar);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Desea eliminar a " + amigoABorrar + "?").setCancelable(false).setPositiveButton("Si", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        getServerData(eliminarAmigoPhp);
                        AmigosOnline.this.finish();
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
        alertbox.setMessage("No tienes amigos conectados");
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
            if (amigosOnline.length > 1) {
                setListAdapter(new ArrayAdapter<String>(AmigosOnline.this, R.layout.list_item, amigosOnline));
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
        String[] titulo = new String[] { "Enviar mensaje a todos" };
        String stringUsuarios = getServerData(mostrarOnlinePhp).replace("[", "").replace("]", "").replace("\"", "");
        String[] usuarios = stringUsuarios.split(",");
        amigosOnline = new String[titulo.length + usuarios.length];
        System.arraycopy(titulo, 0, amigosOnline, 0, titulo.length);
        System.arraycopy(usuarios, 0, amigosOnline, titulo.length, usuarios.length);
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(Cliente.socket.getOutputStream())), true);
            out.println("AmigosOnline|x");
            List<String> usuarioFinalSerializado = Arrays.asList(amigosOnline);
            out.println(usuarioFinalSerializado.toString());
            Log.d("AmigosOnline", "Enviando para online:  " + usuarioFinalSerializado.toString());
            Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        handler.sendEmptyMessage(0);
    }
}
