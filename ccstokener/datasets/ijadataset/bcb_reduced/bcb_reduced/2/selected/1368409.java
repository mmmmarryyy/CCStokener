package net.moonbiter.ebs.protocols.httpjava;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import net.moonbiter.OperationFailureException;
import net.moonbiter.ebs.InputDef;
import net.moonbiter.ebs.ServiceDef;
import net.moonbiter.ebs.validation.UnexistingParamValidationException;
import net.moonbiter.ebs.validation.ValidationException;
import net.moonbiter.ebs.validation.WrongTypeValidationParamException;

public class ServiceClient {

    private URL url;

    private static final String HTTP_METHOD_POST = "POST";

    private ServiceDef serviceDef;

    public ServiceClient(ServiceDef serviceDef, String urlString) {
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Unvalid url given (url:" + urlString + ")", e);
        }
        this.serviceDef = serviceDef;
    }

    public Object call(Map<String, Object> inputs) throws ValidationException, OperationFailureException, IOException, ClassNotFoundException, ServerException {
        Map<String, InputDef> inputsDefs = serviceDef.getInputs();
        for (String inputName : inputsDefs.keySet()) {
            InputDef inputDef = inputsDefs.get(inputName);
            if (inputs.containsKey(inputName)) {
                Object inputValue = inputs.get(inputName);
                Class<?> inputClass = inputDef.getType();
                if (!inputClass.isInstance(inputValue)) {
                    throw new WrongTypeValidationParamException(inputName, inputClass.getCanonicalName(), inputValue.getClass().getCanonicalName());
                }
            } else if (!inputDef.isOptional()) {
                throw new UnexistingParamValidationException(inputName);
            }
        }
        Object result = concreteSending(inputs);
        Map<String, Object> resultMap = examineResult(result);
        if (resultMap.containsKey(HttpJavaProtocol.ERROR)) {
            throw new OperationFailureException("service invokation", (Exception) resultMap.get(HttpJavaProtocol.ERROR));
        } else {
            return result;
        }
    }

    public Map<String, Object> examineResult(Object result) {
        if (!(result instanceof Map)) {
            throw new IllegalArgumentException("given result is not a map");
        }
        Map rawMap = (Map) result;
        for (Object aKey : rawMap.keySet()) {
            if (!(aKey instanceof String)) {
                throw new IllegalArgumentException("given result map have keys that are not strings");
            }
        }
        Map<String, Object> map = (Map<String, Object>) rawMap;
        if (map.containsKey("error")) {
            Object errorValue = map.get(HttpJavaProtocol.ERROR);
            if (!(errorValue instanceof Exception)) {
                throw new IllegalArgumentException("given error is not an exception");
            }
            return map;
        } else if (map.containsKey(HttpJavaProtocol.RESULT)) {
            return map;
        } else {
            throw new IllegalArgumentException("given result that is not successful nor reports an error");
        }
    }

    /**
	 * Send the given obj on an HTTP connection and read the result received.
	 * @param obj
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
    private Object concreteSending(Map<String, Object> obj) throws IOException, ClassNotFoundException, ServerException {
        URLConnection connection = url.openConnection();
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpConnection = null;
            try {
                httpConnection = (HttpURLConnection) connection;
                httpConnection.setRequestMethod(HTTP_METHOD_POST);
                httpConnection.setDoOutput(true);
                OutputStream outStream = httpConnection.getOutputStream();
                ObjectOutputStream objOutStream = new ObjectOutputStream(outStream);
                objOutStream.writeObject(obj);
                httpConnection.connect();
                int response = httpConnection.getResponseCode();
                if (HttpURLConnection.HTTP_OK != response) {
                    throw new ServerException("Bad response from server, response was: " + response);
                }
                InputStream is = httpConnection.getInputStream();
                ObjectInputStream objInStream = new ObjectInputStream(is);
                Object result = objInStream.readObject();
                return result;
            } finally {
                if (httpConnection != null) httpConnection.disconnect();
            }
        } else {
            throw new IllegalStateException("An unvalid url was given and a non http connection was opened");
        }
    }
}
