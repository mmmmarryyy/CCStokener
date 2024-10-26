package hoge.memoriesalbum.post;

import hoge.memoriesalbum.R;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

public class PostActivity extends Activity implements OnClickListener {

    static final String TAG = "PhotoStore";

    static final int REQUEST_PICTURE = 1;

    private static Activity mActivity;

    static final String POST_URL = "http://photo-storage.appspot.com/upload";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.post);
        mActivity = this;
        intentPicture();
        Button postBtn = (Button) this.findViewById(R.id.post_btn_post);
        postBtn.setOnClickListener(this);
    }

    /**
     * intentを投げて画像選択するActivityに遷移する
     */
    private void intentPicture() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "getpicture"), REQUEST_PICTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICTURE && data != null) {
                ImageView img = (ImageView) findViewById(R.id.post_img);
                img.setImageURI(data.getData());
            }
        }
    }

    @Override
    public void onClick(View v) {
        new PostTask().execute();
    }

    /**
     * GAE側にPOSTする
     */
    private class PostTask extends AsyncTask<Void, Void, Boolean> {

        private ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            showPrrogressDialog();
        }

        @Override
        protected final Boolean doInBackground(Void... v) {
            Bitmap bmp = ((BitmapDrawable) ((ImageView) findViewById(R.id.post_img)).getDrawable()).getBitmap();
            HttpURLConnection con;
            try {
                URL url = new URL(POST_URL);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setRequestProperty("Accept-Language", "multipart/form-data");
                con.setRequestProperty("X-RAW", "true");
            } catch (MalformedURLException e) {
                e.printStackTrace();
                finish();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                finish();
                return false;
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(CompressFormat.JPEG, 100, bos);
            OutputStream os = null;
            try {
                os = con.getOutputStream();
                os.write(bos.toByteArray());
                os.flush();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    os.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return false;
            }
            InputStream is = null;
            BufferedReader reader;
            try {
                is = con.getInputStream();
                reader = new BufferedReader(new InputStreamReader(is));
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    is.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return false;
            }
            String s;
            try {
                while ((s = reader.readLine()) != null) {
                    Log.v(TAG, s);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }

        @Override
        protected final void onPostExecute(Boolean flag) {
            mProgressDialog.dismiss();
            if (flag) {
                showAlertDialog();
            } else {
                finish();
            }
        }

        /**
         * POST中にプログレスダイアログ表示
         */
        private void showPrrogressDialog() {
            String title = getResources().getString(R.string.post_prrogress_dialog_title);
            String mes = getResources().getString(R.string.post_prrogress_dialog_mes);
            mProgressDialog = ProgressDialog.show(mActivity, title, mes, true);
        }

        /**
         * POST後に確認ダイアログ表示
         * @return
         */
        private void showAlertDialog() {
            new AlertDialog.Builder(mActivity).setTitle(R.string.post_alert_dialog_title).setMessage(R.string.post_alert_dialog_mes).setPositiveButton(R.string.post_alert_dialog_btn, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    intentPicture();
                }
            }).create().show();
        }
    }
}
