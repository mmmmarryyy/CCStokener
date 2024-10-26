package jp.gaomar.mytem;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * @gabuさん作成GAEのAPIを使ってGAEへの読み書きをするクラスです
 * @author hide
 */
public class MytemGaeController {

    /** Google App Engine のアドレス */
    private static String address = "http://mytemserver.appspot.com/";

    /** 該当JANコードの商品がありません */
    private static final int NOT_FOUND = 404;

    /** すでに該当JANコードの商品があります */
    private static final int DUPLICATE = 400;

    private Context context;

    private static final String TMPFILENAME = "tmp.jpg";

    /**
	 * コンストラクタ
	 * @param context
	 */
    public MytemGaeController(Context context) {
        this.context = context;
    }

    /**
	 * JANコードを引数にGAEから商品マスタを得る
	 * 
	 * @param janCode
	 * @return
	 * @throws GaeException
	 */
    public MytemMaster getMytemMaster(String janCode) throws GaeException {
        HttpClient client = new DefaultHttpClient();
        HttpParams httpParams = client.getParams();
        HttpConnectionParams.setSoTimeout(httpParams, 10000);
        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        BufferedReader reader = null;
        StringBuffer request = new StringBuffer(address);
        request.append("api/mytems/show?jan=");
        request.append(janCode);
        try {
            HttpGet httpGet = new HttpGet(request.toString());
            HttpResponse httpResponse = client.execute(httpGet);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == NOT_FOUND) {
                return null;
            }
            if (statusCode >= 400) {
                throw new GaeException("Status Error = " + Integer.toString(statusCode));
            }
            reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return createMytemMaster(builder.toString());
        } catch (ClientProtocolException e) {
            throw new GaeException(e);
        } catch (SocketTimeoutException e) {
            throw new GaeException(e);
        } catch (IOException exception) {
            throw new GaeException(exception);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
	 * JANコードを引数にGAEから商品履歴を得る
	 * 
	 * @param janCode
	 * @return
	 * @throws GaeException
	 */
    public List<MytemHistory> getMytemHistories(String janCode) throws GaeException {
        HttpClient client = new DefaultHttpClient();
        HttpParams httpParams = client.getParams();
        HttpConnectionParams.setSoTimeout(httpParams, 10000);
        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        BufferedReader reader = null;
        StringBuffer request = new StringBuffer(address);
        request.append("api/mytems/history?jan=");
        request.append(janCode);
        try {
            HttpGet httpGet = new HttpGet(request.toString());
            HttpResponse httpResponse = client.execute(httpGet);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == NOT_FOUND) {
                return null;
            }
            if (statusCode >= 400) {
                throw new GaeException("Status Error = " + Integer.toString(statusCode));
            }
            reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return createMytemHistories(builder.toString());
        } catch (ClientProtocolException e) {
            throw new GaeException(e);
        } catch (SocketTimeoutException e) {
            throw new GaeException(e);
        } catch (IOException exception) {
            throw new GaeException(exception);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
	 * 文字列から商品マスタを生成する
	 * 
	 * @param string
	 * @return
	 */
    private List<MytemHistory> createMytemHistories(String string) {
        try {
            List<MytemHistory> retList = new ArrayList<MytemHistory>();
            JSONObject jsonObject = new JSONObject(string);
            JSONArray jsonArray = jsonObject.getJSONArray("history");
            for (int ii = 0; ii < jsonArray.length(); ii++) {
                JSONObject jsonHistory = jsonArray.getJSONObject(ii);
                String shopname = jsonHistory.getString("shopname");
                int price = jsonHistory.getInt("price");
                Date postdate = null;
                try {
                    postdate = MytemHistory.getSimpleDateFormat().parse(jsonHistory.getString("strPostDate"));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                String note = jsonHistory.getString("note");
                MytemHistory history = new MytemHistory(shopname, price, postdate, note);
                retList.add(history);
            }
            return retList;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * 文字列から商品マスタを生成する
	 * 
	 * @param string
	 * @return
	 */
    private MytemMaster createMytemMaster(String string) {
        try {
            JSONObject jsonObject = new JSONObject(string);
            String name = jsonObject.getString("name");
            String janCode = jsonObject.getString("jan");
            String imageUrl = jsonObject.getString("imageUrl");
            Bitmap image = getBitmap(imageUrl);
            return new MytemMaster(janCode, name, image);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * URLから画像を取得する
	 * 
	 * @param imageUrl
	 * @return
	 * @throws IOException
	 */
    private Bitmap getBitmap(String imageUrl) {
        URL url;
        InputStream input = null;
        try {
            url = new URL(address + imageUrl);
            input = url.openStream();
            return BitmapFactory.decodeStream(input);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
	 * GAEに商品マスタを追加作成します
	 * 
	 * @param noodleMaster
	 * @throws DuplexMytemMasterException
	 * @throws GaeException
	 */
    public void create(MytemMaster mytemMaster) throws DuplexMytemMasterException, GaeException {
        HttpClient client = new DefaultHttpClient();
        HttpParams httpParams = client.getParams();
        HttpConnectionParams.setSoTimeout(httpParams, 10000);
        BufferedReader reader = null;
        InputStream imageInputStream = null;
        HttpPost httpPost = new HttpPost(address + "api/mytems/create");
        try {
            MultipartEntity entity = new MultipartEntity();
            entity.addPart("name", new StringBody(mytemMaster.getName()));
            entity.addPart("jan", new StringBody(mytemMaster.getJanCode()));
            imageInputStream = createImageInputStream(mytemMaster.getImage());
            entity.addPart("image", new InputStreamBody(imageInputStream, "filename"));
            httpPost.setEntity(entity);
            HttpResponse httpResponse;
            httpResponse = client.execute(httpPost);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == DUPLICATE) {
                throw new DuplexMytemMasterException();
            }
            if (statusCode > 400) {
                throw new GaeException("Status Error = " + Integer.toString(statusCode));
            }
            reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch (UnsupportedEncodingException exception) {
            throw new GaeException(exception);
        } catch (ClientProtocolException e) {
            throw new GaeException(e);
        } catch (IOException e) {
            throw new GaeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (imageInputStream != null) {
                try {
                    imageInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            File file = new File(MytemManager.SAVE_IMAGE_DIRECTORY, TMPFILENAME);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
	 * GAEに商品履歴情報を作成します
	 * 
	 * @throws GaeException
	 */
    public void createHistory(String jancode, MytemHistory mytemHistory) throws GaeException {
        HttpClient client = new DefaultHttpClient();
        HttpParams httpParams = client.getParams();
        HttpConnectionParams.setSoTimeout(httpParams, 10000);
        BufferedReader reader = null;
        InputStream imageInputStream = null;
        HttpPost httpPost = new HttpPost(address + "api/mytems/postHistory");
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            MultipartEntity entity = new MultipartEntity();
            entity.addPart("jan", new StringBody(jancode));
            entity.addPart("shopname", new StringBody(mytemHistory.getShopName()));
            entity.addPart("price", new StringBody(Integer.toString(mytemHistory.getPrice())));
            entity.addPart("postdate", new StringBody(sdf.format(mytemHistory.getPostDate())));
            entity.addPart("note", new StringBody(mytemHistory.getNote()));
            httpPost.setEntity(entity);
            HttpResponse httpResponse;
            httpResponse = client.execute(httpPost);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == DUPLICATE) {
                throw new DuplexMytemMasterException();
            }
            if (statusCode > 400) {
                throw new GaeException("Status Error = " + Integer.toString(statusCode));
            }
            reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch (UnsupportedEncodingException exception) {
            throw new GaeException(exception);
        } catch (ClientProtocolException e) {
            throw new GaeException(e);
        } catch (IOException e) {
            throw new GaeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (imageInputStream != null) {
                try {
                    imageInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            File file = new File(MytemManager.SAVE_IMAGE_DIRECTORY, TMPFILENAME);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
	 * Bitmapからファイルを作成しファイルのInputStreamを返す
	 * 
	 * @param bitmap
	 * @return inputstream
	 */
    private InputStream createImageInputStream(Bitmap bitmap) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileOutputStream fileOutputStream = null;
        try {
            bitmap.compress(CompressFormat.JPEG, 100, bos);
            File file = new File(MytemManager.SAVE_IMAGE_DIRECTORY, TMPFILENAME);
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bos.toByteArray());
            fileOutputStream.flush();
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Log.d("err", e.getMessage(), e);
        } catch (IOException e) {
            Log.d("err", e.getMessage(), e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    Log.d("err", e.getMessage(), e);
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
