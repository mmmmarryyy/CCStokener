package com.adrop.dropbox.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import com.adrop.dropbox.JSONUtilities;

public class DropboxGetOperation {

    private static final Logger logger = Logger.getLogger(DropboxGetOperation.class.getName());

    protected StringBuilder requestUrl = new StringBuilder();

    protected OAuthConsumer oauthConsumer;

    protected URL url;

    protected HttpURLConnection urlConn;

    protected Map<String, String> parameters;

    protected InputStream input;

    private boolean cleaned = false;

    public DropboxGetOperation(String requestUrl, Map<String, String> parameters, OAuthConsumer oauthConsumer) {
        super();
        this.requestUrl.append(requestUrl);
        this.oauthConsumer = oauthConsumer;
        this.parameters = parameters;
    }

    public DropboxGetOperation(String requestUrl, OAuthConsumer oauthConsumer) {
        this(requestUrl, null, oauthConsumer);
    }

    public void execute() throws DropboxException {
        try {
            prepareParameters();
            prepareForConnecting();
            connect();
            processResponse();
            int respCode = urlConn.getResponseCode();
            if (respCode == HttpURLConnection.HTTP_OK) {
            } else if (respCode == -1) {
                throw new DropboxException("Not valid http response.");
            } else {
                StringBuilder buf = new StringBuilder();
                try {
                    buf.append(JSONUtilities.readAsStringBuilder(input));
                } catch (IOException e) {
                }
                throw new DropboxException(respCode, urlConn.getResponseMessage() + "[" + buf.toString() + "]");
            }
        } catch (OAuthMessageSignerException e) {
            throw new DropboxException(e);
        } catch (OAuthExpectationFailedException e) {
            throw new DropboxException(e);
        } catch (OAuthCommunicationException e) {
            throw new DropboxException(e);
        } catch (IOException e) {
            throw new DropboxException(e);
        }
    }

    public JSONObject readResponseAsJSONObject() throws DropboxException {
        try {
            JSONObject jsonResult = JSONUtilities.readAsJSONObject(input);
            return jsonResult;
        } catch (IOException e) {
            throw new DropboxException(e);
        } catch (ParseException e) {
            throw new DropboxException(e);
        } finally {
            cleanup();
        }
    }

    public void cleanup() {
        if (cleaned) return;
        cleaned = true;
        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, null, e);
            }
        }
        if (urlConn != null) {
            urlConn.disconnect();
        }
    }

    private void connect() throws IOException {
        urlConn.connect();
    }

    protected void prepareForConnecting() throws IOException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
        url = new URL(requestUrl.toString());
        urlConn = (HttpURLConnection) url.openConnection();
        oauthConsumer.sign(urlConn);
    }

    /**
	 * 
	 * @param parameters
	 * @throws DropboxException
	 */
    protected void prepareParameters() {
        if (parameters == null || parameters.isEmpty()) return;
        StringBuilder buf = new StringBuilder("?");
        boolean first = true;
        for (Iterator<Map.Entry<String, String>> iter = parameters.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<String, String> entry = iter.next();
            String name = entry.getKey();
            String value = entry.getValue();
            try {
                if (!first) buf.append("&");
                buf.append(URLEncoder.encode(name, "UTF-8")).append("=").append(URLEncoder.encode((String) value, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
            }
            if (first) first = false;
        }
        this.requestUrl.append(buf);
    }

    protected void processResponse() throws IOException {
        input = urlConn.getInputStream();
    }

    private InputStream shiftInputStream(InputStream input) throws DropboxException {
        ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
        int bufLen = 4096;
        byte[] buf = new byte[bufLen];
        int read = 0;
        try {
            while ((read = input.read(buf, 0, bufLen)) != -1) {
                byteArrayOutput.write(buf, 0, read);
            }
        } catch (IOException e) {
            throw new DropboxException(e);
        }
        ByteArrayInputStream byteArrayInput = new ByteArrayInputStream(byteArrayOutput.toByteArray());
        return byteArrayInput;
    }
}
