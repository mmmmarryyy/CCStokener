package org.vrforcad.controller.online.http;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import org.vrforcad.lib.network.beans.POConfirmation;
import org.vrforcad.lib.network.beans.PassingObject;

/**
 * The http abstract connection class.
 *  
 * @version 1.1 
 * @author Daniel Cioi <dan.cioi@vrforcad.org>
 */
public abstract class WebConnection {

    protected String session = null;

    private final int OUT_BEAN = 0;

    private final int OUT_PARAMETERS = 1;

    private final int INPUT_BEAN = 0;

    private final int INPUT_FILE = 1;

    private int output;

    private int input;

    private final String contentTypeStream = "application/octet-stream";

    private final String contentTypeForm = "application/x-www-form-urlencoded";

    /**
	 * Generic POST method.
	 * @param url
	 * @param contentType
	 * @param output
	 * @param po
	 * @param body
	 * @param input
	 * @return
	 */
    private Object postRequest(URL url, String contentType, int output, PassingObject po, String body, int input) {
        try {
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("POST");
            httpConnection.setAllowUserInteraction(false);
            httpConnection.setDoOutput(true);
            httpConnection.setDoInput(true);
            httpConnection.setUseCaches(false);
            httpConnection.setRequestProperty("Content-Type", contentType);
            if (session != null) httpConnection.addRequestProperty("Cookie", session);
            ((HttpURLConnection) httpConnection).setChunkedStreamingMode(1024);
            if (output == OUT_BEAN) outputObject(httpConnection, po); else if (output == OUT_PARAMETERS) ouputParameters(httpConnection, body);
            if (session == null) getSession(httpConnection);
            if (input == INPUT_BEAN) return inputObject(httpConnection); else if (input == INPUT_FILE) return inputFile(httpConnection);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Output a bean.
	 * @param httpConnection
	 * @param po
	 * @throws IOException
	 */
    private void outputObject(HttpURLConnection httpConnection, PassingObject po) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(httpConnection.getOutputStream());
        oos.writeObject(po);
        oos.flush();
        oos.close();
    }

    /**
	 * Output parameters.
	 * @param httpConnection
	 * @param body
	 * @throws IOException
	 */
    private void ouputParameters(HttpURLConnection httpConnection, String body) throws IOException {
        DataOutputStream out = new DataOutputStream(httpConnection.getOutputStream());
        out.writeBytes(body);
        out.flush();
        out.close();
    }

    /**
	 * Receiving a bean.
	 * @param httpConnection
	 * @return
	 * @throws IOException
	 */
    private PassingObject inputObject(HttpURLConnection httpConnection) throws IOException {
        PassingObject received = null;
        InputStream in = httpConnection.getInputStream();
        ObjectInputStream ois = new ObjectInputStream(in);
        try {
            received = (PassingObject) ois.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        in.close();
        return received;
    }

    /**
	 * Receiving a file.
	 * @param httpConnection
	 * @return
	 * @throws IOException
	 */
    private File inputFile(HttpURLConnection httpConnection) throws IOException {
        String filePath = "temp" + File.separator + "model.tmp";
        InputStream in = httpConnection.getInputStream();
        try {
            OutputStream os = new FileOutputStream(filePath);
            int length = -1;
            long read = 0;
            byte[] buf = new byte[1024];
            while ((length = in.read(buf)) != -1) {
                os.write(buf, 0, length);
                read += length;
            }
            in.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new File(filePath);
    }

    /**
	 * This method post a PassingObject and receive a PassingObject.
	 * @param url
	 * @param po
	 * @return
	 */
    public PassingObject sendObject(URL url, PassingObject po) {
        return (PassingObject) postRequest(url, contentTypeStream, OUT_BEAN, po, null, INPUT_BEAN);
    }

    /**
	 * This method post some parameters and receive a PassingObject.
	 * @param url
	 * @param body
	 * @return
	 */
    public PassingObject sendParameters(URL url, String body) {
        return (PassingObject) postRequest(url, contentTypeForm, OUT_PARAMETERS, null, body, INPUT_BEAN);
    }

    /**
	 * Get the cookies.
	 * @param httpConnection
	 */
    private void getSession(HttpURLConnection httpConnection) {
        List<String> cookies = httpConnection.getHeaderFields().get("Set-Cookie");
        if (cookies != null && cookies.size() > 0) session = cookies.get(0).split(";")[0];
    }

    /**
	 * Get (download) the selected file.
	 * @param url
	 * @param fileName
	 * @return
	 */
    public File getCADFile(URL url, PassingObject po) {
        return (File) postRequest(url, contentTypeStream, OUT_BEAN, po, null, INPUT_FILE);
    }

    /**
	 * Method to get the created files or modified file by this user.
	 * @param url
	 * @return
	 */
    @SuppressWarnings("rawtypes")
    public List getGenericList(URL url) {
        List filesList = null;
        try {
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("GET");
            httpConnection.setRequestProperty("Content-Type", "text/xml");
            httpConnection.addRequestProperty("Cookie", session);
            InputStream in = httpConnection.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(in);
            try {
                filesList = (List) ois.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filesList;
    }

    /**
	 * Upload a file (zip) to web server.
	 * @param url
	 * @param tmpName
	 * @param fileName
	 * @param extension
	 * @param description
	 * @throws IOException
	 */
    public boolean uploadFile(URL url, File tmpName, String fileName, String extension, String description) throws IOException {
        String charset = "UTF-8";
        String parameters = extension + "&" + description;
        File zipFile = new File(fileName);
        String boundary = Long.toHexString(System.currentTimeMillis());
        String CRLF = "\r\n";
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.addRequestProperty("Cookie", session);
        PrintWriter writer = null;
        try {
            OutputStream output = connection.getOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"parameters\"; prms=\"" + parameters + "\"").append(CRLF);
            writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
            writer.append(CRLF);
            writer.append(parameters);
            writer.append(CRLF);
            writer.flush();
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"binaryFile\"; filename=\"" + zipFile.getName() + "\"").append(CRLF);
            writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(zipFile.getName())).append(CRLF);
            writer.append("Content-Transfer-Encoding: binary").append(CRLF);
            writer.append(CRLF).flush();
            InputStream input = null;
            try {
                input = new FileInputStream(tmpName);
                byte[] buffer = new byte[1024];
                for (int length = 0; (length = input.read(buffer)) > 0; ) {
                    output.write(buffer, 0, length);
                }
                output.flush();
            } finally {
                if (input != null) try {
                    input.close();
                } catch (IOException logOrIgnore) {
                }
            }
            writer.append(CRLF).flush();
            writer.append("--" + boundary + "--").append(CRLF);
        } finally {
            if (writer != null) writer.close();
        }
        POConfirmation confirm = null;
        confirm = (POConfirmation) inputObject(connection);
        return confirm.isSuccessful();
    }
}
