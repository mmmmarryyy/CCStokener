package edu.calpoly.csc.plantidentification;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import edu.calpoly.csc.plantidentification.objects.Plant;

public class PlantAddActivity extends Activity implements OnClickListener {

    public static final String PLANT_ID_KEY = "PLANT_ID";

    private static int TAKE_PICTURE = 1;

    private static int GET_CHARACTERISTICS = 2;

    public static final String ACTION = "ACTION";

    public static final String ADD = "ADD";

    public static final String EDIT = "EDIT";

    private Plant m_plant;

    private TextView m_tvCommonName;

    private TextView m_tvScientificName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        setContentView(R.layout.add_plant);
        DBAdapter db = new DBAdapter(this);
        db.open();
        m_tvCommonName = (TextView) findViewById(R.id.plantCommonNameTxt);
        m_tvScientificName = (TextView) findViewById(R.id.plantScientificNameTxt);
        if (i.getStringExtra(ACTION).equals(ADD)) {
            m_plant = new Plant((int) i.getLongExtra(PLANT_ID_KEY, -1));
        } else {
            int id = (int) i.getLongExtra(PLANT_ID_KEY, -1);
            if (id != 0 && id != -1) {
                Cursor c = db.getPlant(id);
                c.moveToFirst();
                if (c.getCount() > 0) {
                    m_plant = DBAdapter.getPlantFromCursor(c);
                    setPlant();
                }
            }
        }
        ImageView iv = (ImageView) findViewById(R.id.plantPhoto);
        iv.setOnClickListener(this);
        Button b = (Button) findViewById(R.id.btnSave);
        b.setOnClickListener(this);
        b = (Button) findViewById(R.id.btnGetCharacteristics);
        b.setOnClickListener(this);
    }

    private void setPlant() {
        m_tvCommonName.setText(m_plant.getCommonName());
        m_tvScientificName.setText(m_plant.getScientificName());
        ImageView iv = (ImageView) findViewById(R.id.plantPhoto);
        if (!m_plant.getImageLocation().equals("")) {
            try {
                URL url = new URL(m_plant.getImageLocation());
                URLConnection connection = url.openConnection();
                connection.setUseCaches(true);
                InputStream is = (InputStream) connection.getContent();
                Drawable d = Drawable.createFromStream(is, m_plant.getImageLocation());
                iv.setImageDrawable(d);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.plantPhoto) {
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File file = new File(Environment.getExternalStorageDirectory(), "fieldguideimage" + System.currentTimeMillis() + ".jpg");
            Uri outputFileUri = Uri.fromFile(file);
            m_plant.setImageLocation(outputFileUri.getPath());
            i.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            startActivityForResult(i, TAKE_PICTURE);
        }
        if (view.getId() == R.id.btnSave) {
            DBAdapter db = new DBAdapter(this);
            db.open();
            String commonName = m_tvCommonName.getText().toString();
            String scientificName = (String) m_tvScientificName.getText().toString();
            db.insertPlant(m_plant.getId() + "", commonName, "", scientificName, "", "");
            finish();
        }
        if (view.getId() == R.id.btnGetCharacteristics) {
            Intent intent = new Intent(PlantAddActivity.this, PlantAddCharacteristicsActivity.class);
            startActivityForResult(intent, GET_CHARACTERISTICS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_PICTURE) {
            if (resultCode == Activity.RESULT_CANCELED) {
                m_plant.setImageLocation("");
                ImageView image = (ImageView) findViewById(R.id.plantPhoto);
                image.setVisibility(ImageView.INVISIBLE);
                Toast toast = Toast.makeText(this, R.string.imageCaptureFailed, Toast.LENGTH_SHORT);
                toast.show();
            } else {
                ImageView image = (ImageView) findViewById(R.id.plantPhoto);
                Drawable d = Drawable.createFromPath(m_plant.getImageLocation());
                d.setBounds(0, 25, 0, 25);
                image.setImageDrawable(d);
                image.setVisibility(ImageView.VISIBLE);
            }
        }
    }
}
