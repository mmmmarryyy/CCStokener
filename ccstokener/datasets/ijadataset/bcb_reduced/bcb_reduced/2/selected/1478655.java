package com.enjoyxstudy.selenium.autoexec.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author onozaty
 */
public class RemoteControlClient {

    /** type text */
    public static final String TYPE_TEXT = "text";

    /** type JSON */
    public static final String TYPE_JSON = "json";

    /** SUCCESS */
    public static final String SUCCESS = "success";

    /** PASSED */
    public static final String PASSED = "passed";

    /** Selenium AutoExec Server command URL */
    private String commandUrl = "http://localhost:4444/selenium-server/autoexec/command/";

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        String url = null;
        boolean isAsync = false;
        for (String arg : args) {
            if (arg.equals("-a")) {
                isAsync = true;
            } else {
                url = arg;
            }
        }
        RemoteControlClient client = new RemoteControlClient(url);
        boolean isSuccess = true;
        if (isAsync) {
            String result = client.runAsyncString();
            isSuccess = RemoteControlClient.isSuccessResult(result);
            System.out.println("run async. " + result);
        } else {
            String result = client.runString();
            isSuccess = RemoteControlClient.isPassedResult(result);
            System.out.println("run. " + result);
        }
        if (!isSuccess) {
            System.exit(1);
        }
    }

    /**
     * constructor.
     */
    public RemoteControlClient() {
    }

    /**
     * @param commandUrl
     */
    public RemoteControlClient(String commandUrl) {
        if (commandUrl != null && !commandUrl.equals("")) {
            this.commandUrl = commandUrl;
        }
    }

    /**
     * call "stop server" command.
     * 
     * @return result
     * @throws IOException
     */
    public boolean stopServer() throws IOException {
        return isSuccessResult(stopServerString());
    }

    /**
    * call "stop server" command.
      * 
     * @return result
     * @throws IOException
     */
    public String stopServerString() throws IOException {
        return stopServerString(TYPE_TEXT);
    }

    /**
    * call "stop server" command.
     * 
     * @param type 
     * @return result
     * @throws IOException
     */
    public String stopServerString(String type) throws IOException {
        return doCommand("server/stop", type);
    }

    /**
     * call "run" command.
     * 
     * @return result
     * @throws IOException
     */
    public boolean run() throws IOException {
        return isPassedResult(runString());
    }

    /**
     * call "run" command.
     * 
     * @return result
     * @throws IOException
     */
    public String runString() throws IOException {
        return runString(TYPE_TEXT);
    }

    /**
     * call "run" command.
     * 
     * @param type 
     * @return result
     * @throws IOException
     */
    public String runString(String type) throws IOException {
        return doCommand("run", type);
    }

    /**
     * check run passed.
     * 
     * @param result
     * @return is passed
     */
    public static boolean isPassedResult(String result) {
        return result.indexOf("result: " + PASSED) == 0;
    }

    /**
     * call "run async" command.
     * 
     * @return result
     * @throws IOException
     */
    public boolean runAsync() throws IOException {
        return isSuccessResult(runAsyncString());
    }

    /**
     * call "run async" command.
     * 
     * @return result
     * @throws IOException
     */
    public String runAsyncString() throws IOException {
        return runAsyncString(TYPE_TEXT);
    }

    /**
     * call "run async" command.
     * 
     * @param type 
     * @return result
     * @throws IOException
     */
    public String runAsyncString(String type) throws IOException {
        return doCommand("run/async", type);
    }

    /**
     * check run async success.
     * 
     * @param result
     * @return is success
     */
    public static boolean isSuccessResult(String result) {
        return result.indexOf("result: " + SUCCESS) == 0;
    }

    /**
     * call remote command.
     * 
     * @param command
     * @param type 
     * @return response
     * @throws IOException
     */
    private String doCommand(String command, String type) throws IOException {
        StringBuilder url = new StringBuilder(commandUrl);
        url.append(command);
        if (type != null) {
            url.append("?type=").append(type);
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(url.toString()).openConnection();
        try {
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_ACCEPTED) {
                throw new IOException("request failed.  responseCode=[" + responseCode + "]");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            try {
                StringWriter writer = new StringWriter();
                char[] buff = new char[128];
                int length;
                while ((length = reader.read(buff)) != -1) {
                    writer.write(buff, 0, length);
                }
                return writer.toString();
            } finally {
                reader.close();
            }
        } finally {
            connection.disconnect();
        }
    }
}
