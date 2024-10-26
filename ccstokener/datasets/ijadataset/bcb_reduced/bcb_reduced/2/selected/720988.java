package org.nex.ts.server.backchannel.model;

import org.nex.ts.TopicSpacesException;
import org.apache.commons.codec.binary.Base64;
import org.nex.ts.server.common.model.Environment;
import java.net.*;
import java.io.*;

/**
 * @author park
 *
 */
public class BackchannelHttpClient {

    private Environment environment;

    private String backchannelBaseURL;

    private String userName, password;

    /**
	 * 
	 */
    public BackchannelHttpClient(String baseURL, String user, String pwd) {
        backchannelBaseURL = baseURL;
        userName = user;
        password = pwd;
        environment = Environment.getInstance();
    }

    /**
	 * <p>Send some cargo</p>
	 * </p><code>urx is the REST command line, e.g. <code>new/ibis/</code>
	 * <p>Returns whatever is returned from the opposite end.</p>
	 * @param urx 
	 * @param cargo
	 * @return
	 * @throws TopicSpacesException
	 */
    public String sendCargo(String urx, String cargo) throws TopicSpacesException {
        String result = "";
        OutputStream os;
        InputStream is = null;
        try {
            String encodedData = URLEncoder.encode(cargo, "UTF-8");
            URL url = new URL(backchannelBaseURL + urx);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setUseCaches(false);
            int len = encodedData.length();
            con.setRequestProperty("Content-Length", Integer.toString(len));
            String userPassword = userName + ":" + password;
            byte[] foo = Base64.encodeBase64(userPassword.getBytes());
            String up64 = new String(foo);
            con.setRequestProperty("Authorization", "Basic " + up64);
            os = con.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeBytes(encodedData);
            dos.flush();
            is = con.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
            is.close();
            os.close();
            con.disconnect();
        } catch (Exception e) {
            environment.logError("BackchannelHttpClient.sendCargo error " + e.getMessage());
            throw new TopicSpacesException(e.getMessage(), e);
        }
        environment.logDebug("Backchannel.sendCargo+ " + result);
        return result;
    }
}
