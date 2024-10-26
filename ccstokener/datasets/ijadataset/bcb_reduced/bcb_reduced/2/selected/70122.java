package org.imogene.android.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map.Entry;
import java.util.UUID;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.imogene.android.Constants.Paths;
import org.imogene.android.app.WakefulIntentService;
import org.imogene.android.preference.PreferenceHelper;
import org.imogene.android.util.http.multipart.FileInputStreamPart;
import org.imogene.android.util.http.multipart.MultipartEntity;
import org.imogene.android.util.http.multipart.Part;
import org.imogene.android.util.http.ssl.SSLHttpClient;
import org.xmlpull.v1.XmlSerializer;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.Xml;

public class MetadataService extends WakefulIntentService {

    private static final String TAG = MetadataService.class.getName();

    private static final String EXTRA_VALUES = "values-to-send";

    private static final String EXTRA_EXCEPTION = "exception-to-send";

    private static final String EXTRA_TIME = "time-to-send";

    public static final void startMetadataService(Context context, ContentValues values) {
        Intent i = new Intent();
        i.setClass(context, MetadataService.class);
        i.putExtra(EXTRA_VALUES, values);
        sendWakefulWork(context, i);
    }

    public static final void startMetadataService(Context context, Exception e, long time) {
        Intent i = new Intent();
        i.setClass(context, MetadataService.class);
        i.putExtra(EXTRA_TIME, time);
        i.putExtra(EXTRA_EXCEPTION, e);
        sendWakefulWork(context, i);
    }

    private static final String ATTR_TERMINAL = "terminal";

    private static final String ATTR_TIME = "time";

    private static final String ATTR_NAME = "name";

    private static final String ATTR_VALUE = "value";

    private static final String TAG_METADATA = "metadata";

    private static final String TAG_DATA = "data";

    private static final String TAG_EXCEPTION = "exception";

    public MetadataService() {
        super(MetadataService.class.getName());
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        try {
            MetadataClient client = new MetadataClient(PreferenceHelper.getServerUrl(this));
            String hardwareId = PreferenceHelper.getHardwareId(this);
            String realTime = Long.toString(PreferenceHelper.getRealTime(this));
            File file = new File(Paths.PATH_SYNCHRO, System.currentTimeMillis() + ".metadata");
            FileOutputStream fos = new FileOutputStream(file);
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(fos, null);
            serializer.startTag(null, TAG_METADATA);
            serializer.attribute(null, ATTR_TIME, realTime);
            serializer.attribute(null, ATTR_TERMINAL, hardwareId);
            if (intent.hasExtra(EXTRA_VALUES)) {
                ContentValues values = intent.getParcelableExtra(EXTRA_VALUES);
                for (Entry<String, Object> value : values.valueSet()) {
                    if (value.getValue() != null) {
                        serializer.startTag(null, TAG_DATA);
                        serializer.attribute(null, ATTR_NAME, value.getKey());
                        serializer.attribute(null, ATTR_VALUE, value.getValue().toString());
                        serializer.endTag(null, TAG_DATA);
                    }
                }
            } else if (intent.hasExtra(EXTRA_EXCEPTION)) {
                long time = intent.getLongExtra(EXTRA_TIME, 0);
                serializer.startTag(null, TAG_EXCEPTION);
                serializer.attribute(null, ATTR_TIME, Long.toString(time));
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                Exception e = (Exception) intent.getSerializableExtra(EXTRA_EXCEPTION);
                e.printStackTrace(pw);
                pw.flush();
                pw.close();
                serializer.cdsect(sw.toString());
                serializer.endTag(null, TAG_EXCEPTION);
            }
            serializer.endTag(null, TAG_METADATA);
            serializer.flush();
            serializer.endDocument();
            fos.flush();
            fos.close();
            FileInputStream fis = new FileInputStream(file);
            client.sendMetaData(fis);
        } catch (Exception e) {
            Log.e(TAG, "error sending metadata", e);
        }
    }

    public static class MetadataClient {

        private final String mServer;

        public MetadataClient(String server) {
            mServer = server + "sync.html";
        }

        /**
		 * Send meta data to the server
		 * 
		 * @param fis
		 *            the meta data to send
		 * @return 0 if done with success, -1 otherwise
		 * @throws Exception
		 */
        public final int sendMetaData(FileInputStream fis) throws Exception {
            try {
                UUID uuid = UUID.randomUUID();
                HttpClient client = new SSLHttpClient();
                StringBuilder builder = new StringBuilder(mServer).append("?cmd=meta").append("&id=" + uuid);
                HttpPost method = new HttpPost(builder.toString());
                String fileName = uuid + ".metadata";
                FileInputStreamPart part = new FileInputStreamPart("data", fileName, fis);
                MultipartEntity requestContent = new MultipartEntity(new Part[] { part });
                method.setEntity(requestContent);
                HttpResponse response = client.execute(method);
                int code = response.getStatusLine().getStatusCode();
                if (code == HttpStatus.SC_OK) {
                    return 0;
                } else {
                    return -1;
                }
            } catch (Exception e) {
                throw new Exception("send meta data", e);
            }
        }
    }
}
