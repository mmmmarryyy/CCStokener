package br.com.fc.service.client;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import br.com.fc.service.client.returntype.ErrorReturnType;
import br.com.fc.service.client.returntype.OKReturnType;
import br.com.fc.service.client.returntype.ReturnType;
import br.com.fc.service.client.returntype.ThrowsReturnType;
import br.com.fc.service.io.ServiceObject;

public class ServiceClient {

    private URL url;

    private static final Map<String, ReturnType> returns = new HashMap<String, ReturnType>(3);

    static {
        returns.put(ReturnType.OK, new OKReturnType());
        returns.put(ReturnType.THROWS, new ThrowsReturnType());
        returns.put(ReturnType.ERROR, new ErrorReturnType());
    }

    public ServiceClient(String url) throws ServiceException {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new ServiceException(e);
        }
    }

    private <T> T conectar(String className, String methodName, Class<?>[] parameterTypes, Object[] args, T t) throws Exception {
        try {
            HttpURLConnection conexao = (HttpURLConnection) url.openConnection();
            conexao.setRequestMethod("POST");
            conexao.setDoOutput(true);
            conexao.setRequestProperty("className", className);
            conexao.setRequestProperty("methodName", methodName);
            ServiceObject serviceObject = new ServiceObject();
            serviceObject.write(new Object[] { parameterTypes, args }, conexao.getOutputStream());
            if (conexao.getResponseCode() != 200) {
                throw new ServiceException("Falha grave no servidor com url: " + url);
            }
            String typeResponse = conexao.getHeaderField("Service-Type-Response");
            Object object = serviceObject.read(conexao.getInputStream());
            return returns.get(typeResponse).tratarReturn(object, t);
        } catch (IOException e) {
            throw new ServiceException("Falha ao conectar no servidor com url: " + url, e);
        } catch (ClassNotFoundException e) {
            throw new ServiceException("Falha objeto espera nao e compatavel. Resposta do servidor com url: " + url, e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T call(String className, String methodName, Class<?>[] parameterTypes, Object[] args) throws Exception {
        Type type = Class.forName(className).getMethod(methodName, parameterTypes).getGenericReturnType();
        return (T) conectar(className, methodName, parameterTypes, args, type);
    }
}
