package jp.gaomar.onigirisalechecker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class HPCheckTask extends AsyncTask<Void, Void, DOMResult> {

    /** �A�h���X */
    public static final String BASE_ADDRESS = "http://www.sej.co.jp/cmp/";

    public static final String ONIGIRI_ADDRESS = BASE_ADDRESS + "onigiricmp.html";

    /** ��v�p�^�[��*/
    private final Pattern pDate = Pattern.compile("[0-9]{4}�N[0-9]{1,2}��[0-9]{1,2}��");

    private final Pattern pKikan = Pattern.compile("(?<=�y)[0-9]{1,}");

    /** �v���O���X�_�C�A���O*/
    private ProgressDialog progressDialog;

    /** �R���e�L�X�g*/
    private Context mCtx;

    /** �������`�F�b�N�t���O*/
    private boolean mNowFlg;

    /** �ڍ׃{�^��*/
    private Button mButton;

    /**
	 * �R���X�g���N�^
	 * @param ctx
	 */
    public HPCheckTask(Context ctx) {
        super();
        this.mCtx = ctx;
    }

    /**
	 * �R���X�g���N�^
	 * @param ctx
	 * @param flg
	 */
    public HPCheckTask(Context ctx, boolean flg, Button btn) {
        super();
        this.mCtx = ctx;
        this.mNowFlg = flg;
        this.mButton = btn;
    }

    /**
	 * HTTP�N���C�A���g�擾
	 * @return
	 */
    private HttpClient getClient() {
        HttpClient client = new DefaultHttpClient();
        HttpParams httpParams = client.getParams();
        HttpConnectionParams.setSoTimeout(httpParams, 10000);
        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        return client;
    }

    /**
	 * �\�[�X�擾
	 * @param request
	 * @param client
	 * @return
	 */
    private InputSource httpGet(String request, HttpClient client) {
        InputSource source = null;
        try {
            HttpGet httpGet = new HttpGet(request);
            HttpResponse httpResponse = client.execute(httpGet);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                return null;
            }
            if (statusCode > HttpStatus.SC_BAD_REQUEST) {
                return null;
            }
            source = new InputSource(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return source;
    }

    @Override
    protected void onPreExecute() {
        if (mNowFlg) {
            setupDialog(mCtx.getString(R.string.lbl_dialog));
        }
    }

    @Override
    protected DOMResult doInBackground(Void... params) {
        DOMResult result = new DOMResult();
        HttpClient client = getClient();
        InputSource source = httpGet(ONIGIRI_ADDRESS, client);
        try {
            XMLReader reader = new Parser();
            reader.setFeature(Parser.namespacesFeature, false);
            reader.setFeature(Parser.namespacePrefixesFeature, false);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new SAXSource(reader, source), result);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerFactoryConfigurationError e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (SAXNotRecognizedException e) {
            e.printStackTrace();
        } catch (SAXNotSupportedException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    protected void onPostExecute(DOMResult result) {
        dialogClose();
        Document doc = (Document) result.getNode();
        NodeList dlChilds = doc.getElementsByTagName("dl");
        try {
            for (int i = 0; i < dlChilds.getLength(); i++) {
                Element elem = (Element) dlChilds.item(i);
                Matcher m = pDate.matcher(elem.getTextContent());
                if (m.find()) {
                    Matcher mKikan = pKikan.matcher(elem.getTextContent());
                    Calendar now = Calendar.getInstance();
                    Calendar start = Calendar.getInstance();
                    Calendar end = Calendar.getInstance();
                    start.setTimeInMillis(strToDate(m.group(), "yyyy�NMM��dd��").getTime());
                    end.setTimeInMillis(strToDate(m.group(), "yyyy�NMM��dd��").getTime());
                    int kikan = 0;
                    if (mKikan.find()) {
                        kikan = Integer.parseInt(mKikan.group());
                        end.set(Calendar.DATE, start.get(Calendar.DATE) + kikan);
                        if (start.before(now) && end.after(now)) {
                            if (!mNowFlg) {
                                NotificationManager notificationManager = (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
                                Notification notification = new Notification(R.drawable.onigiri, mCtx.getString(R.string.lbl_sale), System.currentTimeMillis());
                                Intent newIntent = new Intent(mCtx, MainActivity.class);
                                newIntent.putExtra("check", true);
                                PendingIntent contentIntent = PendingIntent.getActivity(mCtx, 0, newIntent, 0);
                                notification.setLatestEventInfo(mCtx.getApplicationContext(), mCtx.getString(R.string.app_name), String.format(mCtx.getString(R.string.format_notification), m.group(), mKikan.group()), contentIntent);
                                notificationManager.cancelAll();
                                notificationManager.notify(R.string.app_name, notification);
                            } else {
                                mButton.setVisibility(View.VISIBLE);
                                Toast.makeText(mCtx, mCtx.getString(R.string.lbl_sale), Toast.LENGTH_SHORT).show();
                            }
                        } else if (mNowFlg) {
                            mButton.setVisibility(View.GONE);
                            Toast.makeText(mCtx, mCtx.getString(R.string.lbl_not_sale), Toast.LENGTH_SHORT).show();
                        }
                        break;
                    }
                }
            }
        } catch (NumberFormatException e) {
        } catch (DOMException e) {
        } catch (Exception e) {
        }
    }

    /**
	 * �������t���w��t�H�[�}�b�g��t�^�ϊ�
	 * 
	 * @param strdate
	 *            ��t�ϊ��O������
	 * @param fmt
	 *            �w��t�H�[�}�b�g(��:yyyy/MM/dd)
	 * @return
	 */
    public static Date strToDate(String strdate, String fmt) {
        Date RetDate = null;
        DateFormat parser = new SimpleDateFormat(fmt);
        try {
            RetDate = parser.parse(strdate);
        } catch (ParseException e) {
        }
        return RetDate;
    }

    /**
     * �_�C�A���O�Z�b�g�A�b�v
     */
    private void setupDialog(String message) {
        progressDialog = new ProgressDialog(mCtx);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    /**
     * �_�C�A���O����
     */
    private void dialogClose() {
        if (progressDialog != null) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        }
    }
}
