package edu.calpoly.csc.plantidentification;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.http.util.ByteArrayBuffer;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import edu.calpoly.csc.plantidentification.objects.Plant;

public class OptionsActivity extends Activity {

    private static final String USERNAME_SAVE = "USERNAME_SAVE";

    private EditText m_username;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.options);
        final Context c = this;
        m_username = (EditText) this.findViewById(R.id.editUsername);
        SharedPreferences sp = OptionsActivity.this.getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        if (sp.contains("Username")) {
            Button btn = (Button) OptionsActivity.this.findViewById(R.id.btnRegister);
            btn.setVisibility(View.INVISIBLE);
            TextView tv = (TextView) OptionsActivity.this.findViewById(R.id.txtRegisterPrompt);
            EditText et = (EditText) OptionsActivity.this.findViewById(R.id.editUsername);
            et.setVisibility(View.INVISIBLE);
            tv.setText(OptionsActivity.this.getResources().getString(R.string.txtRegisterDone) + " " + sp.getString("Username", ""));
        }
        ((Button) this.findViewById(R.id.btnResyncIdentifications)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                DBAdapter db = new DBAdapter(c);
                db.open();
                db.clearAllButIdentifications();
                db.close();
                startService(new Intent(c, QuestionDownloadService.class));
            }
        });
        ((Button) this.findViewById(R.id.btnDownloadPictures)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                downloadPictures();
            }
        });
        ((Button) this.findViewById(R.id.btnRegister)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                UserManager um = new UserManager();
                UserManager.Result result = um.registerUser(m_username.getText().toString());
                if (result == UserManager.Result.BAD_FORMAT) {
                    Toast t = Toast.makeText(OptionsActivity.this, R.string.usernameRestrictions, Toast.LENGTH_SHORT);
                    t.show();
                }
                if (result == UserManager.Result.SERVER_CONNECTION_FAILED) {
                    Toast t = Toast.makeText(OptionsActivity.this, R.string.usernameNoConnect, Toast.LENGTH_SHORT);
                    t.show();
                }
                if (result == UserManager.Result.SERVER_REJECT) {
                    Toast t = Toast.makeText(OptionsActivity.this, R.string.usernameRejected, Toast.LENGTH_SHORT);
                    t.show();
                }
                if (result == UserManager.Result.OK) {
                    Button btn = (Button) OptionsActivity.this.findViewById(R.id.btnRegister);
                    btn.setVisibility(View.INVISIBLE);
                    TextView tv = (TextView) OptionsActivity.this.findViewById(R.id.txtRegisterPrompt);
                    EditText et = (EditText) OptionsActivity.this.findViewById(R.id.editUsername);
                    et.setVisibility(View.INVISIBLE);
                    tv.setText(OptionsActivity.this.getResources().getString(R.string.txtRegisterDone) + " " + m_username.getText().toString());
                    SharedPreferences sp = OptionsActivity.this.getSharedPreferences("Preferences", Context.MODE_PRIVATE);
                    SharedPreferences.Editor ed = sp.edit();
                    ed.putString("Username", m_username.getText().toString());
                    ed.putString("Secret", um.secret);
                    ed.commit();
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(USERNAME_SAVE, m_username.getText().toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        m_username.setText(savedInstanceState.getString(USERNAME_SAVE));
        super.onRestoreInstanceState(savedInstanceState);
    }

    protected void downloadPictures() {
        DBAdapter db = new DBAdapter(this);
        db.open();
        Cursor c = db.getAllPlants();
        while (c.moveToNext()) {
            Plant p = DBAdapter.getPlantFromCursor(c);
            downloadPicture(p.getImageLocation());
        }
        db.close();
    }

    protected void downloadPicture(String urlpath) {
        try {
            URL url = new URL(urlpath);
            File file = new File(ImageManager.getLocalStorageLocation(urlpath));
            if (!file.exists()) file.createNewFile();
            URLConnection ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.outHeight = 800;
            o.outWidth = 800;
            Bitmap bm = BitmapFactory.decodeByteArray(baf.toByteArray(), 0, baf.toByteArray().length);
            FileOutputStream out = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
