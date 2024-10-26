package org.fosterapet.test.ui;

import com.google.appengine.repackaged.com.google.common.collect.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import org.fosterapet.ui.ServletReqEnums.EServletReqParam;
import org.greatlogic.gae.*;

public class ClientRequest extends TimerTask {

    private static String _namespace = null;

    private ClientRequestThread _clientRequestThread;

    private long _expectedStartTimeMillis;

    private long _finishTimeMillis;

    private int _httpResponseCode;

    private String _messageBody;

    private Map<EServletReqParam, Object> _paramMap;

    private Map<EServletReqParam, String> _propertyMap;

    private List<String> _responseLineList;

    private Exception _resultException;

    private String _requestId;

    private long _startTimeMillis;

    private ETaskStatus _taskStatus;

    private String _url;

    public enum ETaskStatus {

        CompletedWithErrors, CompletedWithNoErrors, NotStarted, Started
    }

    static void setNamespace(String namespace) {
        _namespace = namespace;
    }

    /**
 * Creates a new client request.
 * @param requestId The request id that can be used by the requester in the callback routine to
 * determine which request has completed.
 * @param urlString The URL that is to be used for the request.
 */
    public ClientRequest(final String requestId, final String urlString) {
        _requestId = requestId;
        _url = urlString;
        _paramMap = Maps.newHashMap();
        _propertyMap = Maps.newHashMap();
        _responseLineList = Lists.newArrayList();
        _taskStatus = ETaskStatus.NotStarted;
        if (_namespace != null) {
            _paramMap.put(EServletReqParam.Context_namespace, _namespace);
        }
    }

    private void appendParamsToURL() {
        int paramCount = 0;
        for (Entry<EServletReqParam, Object> paramMapEntry : _paramMap.entrySet()) {
            String paramName = paramMapEntry.getKey().getParamName();
            if (paramMapEntry.getValue() instanceof String) {
                paramCount = appendParamToURL(paramCount, paramName, (String) paramMapEntry.getValue());
            } else {
                for (String paramValue : (String[]) paramMapEntry.getValue()) {
                    paramCount = appendParamToURL(paramCount, paramName, paramValue);
                }
            }
        }
    }

    private int appendParamToURL(final int paramCount, final String paramName, final String paramValue) {
        _url += (paramCount == 0 ? "?" : "&") + paramName + "=" + paramValue.replaceAll(" ", "+");
        return paramCount + 1;
    }

    public long getExpectedStartTimeMillis() {
        return _expectedStartTimeMillis;
    }

    public long getFinishTimeMillis() {
        return _finishTimeMillis;
    }

    public int getHttpResponseCode() {
        return _httpResponseCode;
    }

    public String getRequestId() {
        return _requestId;
    }

    public List<String> getResponseLineList() {
        return _responseLineList;
    }

    public long getStartTimeMillis() {
        return _startTimeMillis;
    }

    public ETaskStatus getTaskStatus() {
        return _taskStatus;
    }

    public void logResponse() {
        for (String responseLine : _responseLineList) {
            GLLog.info(responseLine);
        }
    }

    @Override
    public void run() {
        _taskStatus = ETaskStatus.Started;
        _startTimeMillis = System.currentTimeMillis();
        try {
            appendParamsToURL();
            HttpURLConnection connection = (HttpURLConnection) new URL(_url).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("FAPUser", "fap_test@gmail.com");
            for (Entry<EServletReqParam, String> propertyMapEntry : _propertyMap.entrySet()) {
                connection.setRequestProperty(propertyMapEntry.getKey().getParamName(), propertyMapEntry.getValue());
            }
            connection.connect();
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(_messageBody == null ? "" : _messageBody);
            writer.close();
            _httpResponseCode = connection.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            do {
                inputLine = reader.readLine();
                if (inputLine != null) {
                    _responseLineList.add(inputLine);
                }
            } while (inputLine != null);
            reader.close();
            connection.disconnect();
        } catch (Exception e) {
            _resultException = e;
        }
        if (_httpResponseCode == HttpURLConnection.HTTP_OK && _resultException == null) {
            _finishTimeMillis = System.currentTimeMillis();
            _taskStatus = ETaskStatus.CompletedWithNoErrors;
        } else {
            GLLog.warning("Request failed for:" + toString() + (_httpResponseCode == HttpURLConnection.HTTP_OK ? " (exception thrown)" : " (response code:" + _httpResponseCode + ")"), _resultException);
            _taskStatus = ETaskStatus.CompletedWithErrors;
        }
        if (_clientRequestThread != null) {
            _clientRequestThread.processRequestCompletion(this);
        }
    }

    public void setClientRequestThread(final ClientRequestThread clientRequestThread) {
        _clientRequestThread = clientRequestThread;
    }

    public void setExpectedStartTimeMillis(final long expectedStartTimeMillis) {
        _expectedStartTimeMillis = expectedStartTimeMillis;
    }

    public void setMessageBody(final String messageBody) {
        _messageBody = messageBody;
    }

    public void setParam(final EServletReqParam param, final long value) {
        setParam(param, String.valueOf(value));
    }

    public void setParam(final EServletReqParam param, final Object value) {
        _paramMap.put(param, value);
    }

    public void setProperty(final EServletReqParam param, final long value) {
        setProperty(param, String.valueOf(value));
    }

    public void setProperty(final EServletReqParam param, final String value) {
        _propertyMap.put(param, value);
    }

    @Override
    public String toString() {
        String params = "";
        for (Entry<EServletReqParam, Object> paramMapEntry : _paramMap.entrySet()) {
            params += (params.isEmpty() ? "" : ";") + paramMapEntry.getKey().getParamName() + ":" + paramMapEntry.getValue();
        }
        return _requestId + " URL:" + _url + " Status:" + _taskStatus + (params.isEmpty() ? "" : " Params:" + params) + (GAEUtil.isEmpty(_messageBody) ? "" : " Body:" + _messageBody) + " Expected start:" + _expectedStartTimeMillis + " Actual start:" + _startTimeMillis + " Difference:" + (_startTimeMillis - _expectedStartTimeMillis) + "ms" + " Execution time:" + (_finishTimeMillis > 0 ? (_finishTimeMillis - _startTimeMillis) + "ms" : "");
    }
}
