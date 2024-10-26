package edu.ucsd.ncmir.spl.utilities;

import edu.ucsd.ncmir.spl.core.NamedThread;
import java.io.InputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author spl
 */
public class URLWriter extends NamedThread {

    private String _surl;

    private InputStream _input_stream;

    private Exception _exception = null;

    private String _reply = null;

    public URLWriter(String surl, InputStream input_stream) {
        super(surl);
        this._surl = surl;
        this._input_stream = input_stream;
    }

    @Override
    public void run() {
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            HttpPut put = new HttpPut(this._surl);
            InputStreamWrapper isw = new InputStreamWrapper(this, this._input_stream);
            InputStreamEntity ire = new InputStreamEntity(isw, -1);
            ire.setContentType("text/binary");
            put.setEntity(ire);
            HttpResponse response = client.execute(put);
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) this._reply = EntityUtils.toString(entity);
            }
        } catch (Exception e) {
            this._exception = e;
        }
    }

    public final String getReply() throws Exception {
        this.join();
        if (this._exception != null) throw this._exception;
        return this._reply;
    }

    public void update(long l) {
    }
}
