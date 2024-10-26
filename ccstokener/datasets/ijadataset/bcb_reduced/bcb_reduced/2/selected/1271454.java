package com.evaserver.rof.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * TODO implement according to http://www.w3.org/TR/XMLHttpRequest/
 *
 * @author Max Antoni
 * @version $Revision: 130 $
 */
class TXmlHttpRequest extends TObject {

    static final TObject XML_HTTP_REQUEST_PROTOTYPE;

    private static final int READY_STATE_COMPLETE = 4;

    private static final int READY_STATE_UNINITIALIZED = 0;

    static {
        XML_HTTP_REQUEST_PROTOTYPE = new TObject();
        XML_HTTP_REQUEST_PROTOTYPE.putSystemProperty("open", new NativeMethod(2) {

            public TType call(ExecutionContext inContext, TObject inThis, TType[] inArgs) throws ScriptException {
                if (inArgs.length < 2) {
                    throw new ScriptException(ScriptException.Type.ERROR, "Two arguments expected.", inContext.getLineNumber());
                }
                String type = inArgs[0].toJSString(inContext).toString();
                String url = inArgs[1].toJSString(inContext).toString();
                boolean async = false;
                if (inArgs.length >= 3) {
                    async = inArgs[2].toJSBoolean().toBooleanValue();
                }
                ((TXmlHttpRequest) inThis).open(inContext, type, url, async);
                return TUndefined.INSTANCE;
            }
        });
        XML_HTTP_REQUEST_PROTOTYPE.putSystemProperty("setRequestHeader", new NativeMethod(2) {

            public TType call(ExecutionContext inContext, TObject inThis, TType[] inArgs) throws ScriptException {
                if (inArgs.length != 2) {
                    throw new ScriptException(ScriptException.Type.ERROR, "Two arguments expected.", inContext.getLineNumber());
                }
                ((TXmlHttpRequest) inThis).setRequestHeader(inArgs[0].toJSString(inContext).toString(), inArgs[1].toJSString(inContext).toString());
                return TUndefined.INSTANCE;
            }
        });
        XML_HTTP_REQUEST_PROTOTYPE.putSystemProperty("send", new NativeMethod(1) {

            public TType call(ExecutionContext inContext, TObject inThis, TType[] inArgs) throws ScriptException {
                TString data = inArgs.length == 0 ? null : inArgs[0].toJSString(inContext);
                if (data != null) {
                    inThis.putInternal("data", data);
                }
                if (((Boolean) inContext.getWindow().getProperties().get(ScriptProperties.CORE_SEND_XMLHTTPREQUEST)).booleanValue()) {
                    ((TXmlHttpRequest) inThis).send(inContext);
                }
                return TUndefined.INSTANCE;
            }
        });
        XML_HTTP_REQUEST_PROTOTYPE.putSystemProperty("abort", new NativeMethod(1) {

            public TType call(ExecutionContext inContext, TObject inThis, TType[] inArgs) throws ScriptException {
                return TUndefined.INSTANCE;
            }
        });
        XML_HTTP_REQUEST_PROTOTYPE.putSystemProperty("getAllResponseHeaders", new NativeMethod(3) {

            public TType call(ExecutionContext inContext, TObject inThis, TType[] inArgs) throws ScriptException {
                return TString.EMPTY;
            }
        });
        XML_HTTP_REQUEST_PROTOTYPE.putSystemProperty("getResponseHeader", new NativeMethod(3) {

            public TType call(ExecutionContext inContext, TObject inThis, TType[] inArgs) throws ScriptException {
                return TString.EMPTY;
            }
        });
        XML_HTTP_REQUEST_PROTOTYPE.putSystemProperty("sendResponse", new NativeMethod(3) {

            public TType call(ExecutionContext inContext, TObject inThis, TType[] inArgs) throws ScriptException {
                TNumber status;
                if (inArgs.length == 0) {
                    status = new TNumber(200);
                } else {
                    status = inArgs[0].toJSNumber(inContext);
                }
                TString statusText = TString.EMPTY;
                if (inArgs.length >= 2) {
                    statusText = inArgs[1].toJSString(inContext);
                }
                TString responseText = TString.EMPTY;
                if (inArgs.length >= 3) {
                    responseText = inArgs[2].toJSString(inContext);
                }
                ((TXmlHttpRequest) inThis).handleResponse(inContext, status, statusText, responseText);
                return TUndefined.INSTANCE;
            }
        });
        XML_HTTP_REQUEST_PROTOTYPE.putSystemProperty("fire", new NativeMethod(0) {

            public TType call(ExecutionContext inContext, TObject inThis, TType[] inArgs) throws ScriptException {
                ((TXmlHttpRequest) inThis).send(inContext);
                return TUndefined.INSTANCE;
            }
        });
    }

    private TFunction callback;

    private HttpURLConnection connection;

    TXmlHttpRequest() {
        super(XML_HTTP_REQUEST_PROTOTYPE);
    }

    void open(ExecutionContext inContext, String inType, String inUrl, boolean inAsync) throws ScriptException {
        putInternal("type", new TString(inType));
        putInternal("url", new TString(inUrl));
        putInternal("readyState", new TNumber(READY_STATE_UNINITIALIZED));
        putInternal("async", TBoolean.valueOf(inAsync));
        URL url;
        try {
            url = new URL(inUrl);
        } catch (MalformedURLException e) {
            throw new ScriptException(ScriptException.Type.ERROR, e.toString(), inContext.getLineNumber());
        }
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new ScriptException(ScriptException.Type.ERROR, e.toString(), inContext.getLineNumber());
        }
        if ("post".equalsIgnoreCase(inType)) {
            try {
                connection.setRequestMethod("POST");
            } catch (ProtocolException e) {
                throw new ScriptException(ScriptException.Type.ERROR, e.toString(), inContext.getLineNumber());
            }
        }
    }

    String getHeader(String inName) {
        if (connection == null) {
            return null;
        }
        return connection.getHeaderField(inName);
    }

    void send(ExecutionContext inContext) throws ScriptException {
        int status;
        String statusText;
        StringBuffer buffer = new StringBuffer();
        try {
            TType data = get("data");
            if (data != TUndefined.INSTANCE) {
                connection.setDoOutput(true);
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(data.toJSString(inContext).toString().getBytes());
                outputStream.close();
            }
        } catch (IOException e) {
            throw new ScriptException(ScriptException.Type.ERROR, e.toString(), inContext.getLineNumber());
        }
        InputStream stream;
        try {
            connection.connect();
            stream = connection.getInputStream();
        } catch (IOException e) {
            stream = connection.getErrorStream();
        }
        try {
            status = connection.getResponseCode();
            statusText = connection.getResponseMessage();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            reader.close();
        } catch (IOException e) {
            throw new ScriptException(ScriptException.Type.ERROR, e.toString(), inContext.getLineNumber());
        } finally {
            connection.disconnect();
            connection = null;
        }
        handleResponse(inContext, new TNumber(status), new TString(statusText), new TString(buffer.toString()));
    }

    void handleResponse(ExecutionContext inContext, TNumber inStatus, TString inStatusText, TString inResponseText) throws ScriptException {
        if (connection != null) {
            connection = null;
        }
        putInternal("status", inStatus);
        putInternal("statusText", inStatusText);
        putInternal("responseText", inResponseText);
        putInternal("responseXML", TUndefined.INSTANCE);
        putInternal("readyState", new TNumber(READY_STATE_COMPLETE));
        if (get("async").toJSBoolean().toBooleanValue()) {
            callback(inContext);
        }
        inContext.getWindow().removeXHR(this);
    }

    void abort(ExecutionContext inContext) {
        connection = null;
        inContext.getWindow().removeXHR(this);
    }

    void setRequestHeader(String inKey, String inValue) {
        connection.addRequestProperty(inKey, inValue);
    }

    private void callback(ExecutionContext inContext) throws ScriptException {
        if (callback == null) {
            TType handler = get("onreadystatechange");
            if (handler == TUndefined.INSTANCE) {
                return;
            }
            if (handler instanceof TFunction) {
                callback = (TFunction) handler;
            } else {
                throw new ScriptException(ScriptException.Type.TYPE_ERROR, "XMLHttpRequest#onreadystatechange: function expected", Statement.UNKNOWN_LINE);
            }
        }
        callback.call(inContext, this, TType.EMPTY_ARRAY);
    }

    Object createRemote(TWindow inWindow) {
        return new XmlHttpRequest(inWindow, this);
    }
}
