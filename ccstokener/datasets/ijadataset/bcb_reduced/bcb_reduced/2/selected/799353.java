package com.aptana.ide.scripting.io;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.mozilla.javascript.ScriptableObject;
import com.aptana.ide.core.IdeLog;
import com.aptana.ide.core.StringUtils;
import com.aptana.ide.scripting.ScriptingPlugin;

/**
 * @author Paul Colton
 */
public class WebRequest extends ScriptableObject {

    private static final long serialVersionUID = -2151860369251448749L;

    private String _method;

    private String _uri;

    /**
	 * XMLHttpRequest
	 */
    public WebRequest() {
        this._method = null;
        this._uri = null;
    }

    /**
	 * @see org.mozilla.javascript.Scriptable#getClassName()
	 */
    public String getClassName() {
        return "WebRequest";
    }

    /**
	 * jsFunction_open
	 * 
	 * @param method
	 * @param uri
	 */
    public void jsFunction_open(String method, String uri) {
        this._method = method.toLowerCase();
        this._uri = uri;
    }

    /**
	 * jsFunction_send
	 * 
	 * @param postData
	 * @return String
	 */
    public String jsFunction_send(String postData) {
        URL url = null;
        try {
            if (_uri.startsWith("http")) {
                url = new URL(_uri);
            } else {
                url = new URL("file://./" + _uri);
            }
        } catch (MalformedURLException e) {
            IdeLog.logError(ScriptingPlugin.getDefault(), Messages.WebRequest_Error, e);
            return StringUtils.EMPTY;
        }
        try {
            URLConnection conn = url.openConnection();
            OutputStreamWriter wr = null;
            if (this._method.equals("post")) {
                conn.setDoOutput(true);
                wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(postData);
                wr.flush();
            }
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line + "\r\n");
            }
            if (wr != null) {
                wr.close();
            }
            rd.close();
            String result = sb.toString();
            return result;
        } catch (Exception e) {
            IdeLog.logError(ScriptingPlugin.getDefault(), Messages.WebRequest_Error, e);
            return StringUtils.EMPTY;
        }
    }
}
