package com.googlecode.sqldatagenerator.service.remote;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.springframework.util.Assert;
import com.googlecode.sqldatagenerator.util.Callback;

public abstract class AbstractServletServiceRemote {

    protected void invoke(String path, Object request, Callback<Object> callback) throws IOException, ClassNotFoundException {
        Assert.notNull(callback, "callback cant be null");
        URL url = new URL(path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setDefaultUseCaches(false);
        connection.setRequestMethod("POST");
        connection.connect();
        try {
            ObjectOutputStream output = new ObjectOutputStream(connection.getOutputStream());
            try {
                output.writeObject(request);
                output.flush();
            } finally {
                output.close();
            }
            ObjectInputStream input = new ObjectInputStream(connection.getInputStream());
            try {
                for (; ; ) {
                    Object result = input.readObject();
                    if (result == null) {
                        break;
                    }
                    callback.onSuccess(result);
                }
            } finally {
                input.close();
            }
        } finally {
            connection.disconnect();
        }
    }
}
