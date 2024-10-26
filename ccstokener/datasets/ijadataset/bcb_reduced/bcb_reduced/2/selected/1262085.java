package hoge.memoriesalbum.get;

import hoge.memoriesalbum.R;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class ImageActivity extends Activity {

    private static final String URI_IMAGE = "http://photo-storage.appspot.com/download?key=";

    private String mKey;

    private String mFileName;

    private TextView mTextFileName;

    private ImageView mImageView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.get);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mKey = extras.getString("key");
            mFileName = extras.getString("filename");
        }
        mTextFileName = (TextView) findViewById(R.id.filename);
        mImageView = (ImageView) findViewById(R.id.photo);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        mTextFileName.setText(mFileName);
        mImageView.setImageBitmap(getBitmap(mKey, 0));
        super.onResume();
    }

    public static final Bitmap getBitmap(final String key, int size) {
        Bitmap bmp = null;
        byte[] line = new byte[1024];
        int byteSize = 0;
        String urlStr = URI_IMAGE + key;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.connect();
            InputStream is = con.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while ((byteSize = is.read(line)) > 0) {
                out.write(line, 0, byteSize);
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = size;
            byte[] byteArray = out.toByteArray();
            bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, options);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bmp;
    }
}
