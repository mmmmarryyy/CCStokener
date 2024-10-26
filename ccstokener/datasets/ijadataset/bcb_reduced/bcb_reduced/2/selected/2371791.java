package us.tryclickon;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.TreeMap;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import us.tryclickon.utility.UtilMisc;
import us.tryclickon.vimeoResponse.Frob;
import us.tryclickon.vimeoResponse.Token;
import us.tryclickon.vimeoResponse.UploadStatus;
import us.tryclickon.vimeoResponse.UploadTicket;
import us.tryclickon.vimeoResponse.User;
import com.google.gson.Gson;
import com.myjavatools.web.ClientHttpRequest;

public class VimeoWrapper {

    static Logger log = Logger.getLogger(VimeoWrapper.class.getName());

    private final String GET_FROB_METHOD = "vimeo.auth.getFrob";

    private final String AUTH_LINK_BASE = "http://vimeo.com/services/auth/";

    private String TEMP_AUTH_TOKEN = "temp_auth_toke";

    private String apiKey;

    private String secretKey;

    private String loginLink;

    private String frob;

    public VimeoWrapper(String apiKey, String secretKey) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
    }

    public String generateLoginLink() {
        frob = this.getFrob();
        TreeMap<String, String> params = new TreeMap<String, String>();
        params.put("api_key", apiKey);
        params.put("frob", frob);
        params.put("perms", "delete");
        String signature = this.generateAppSignature(params);
        String link = AUTH_LINK_BASE + "?api_key=" + apiKey + "&perms=delete" + "&frob=" + frob + "&api_sig=" + signature;
        return link;
    }

    public String getFrob() {
        TreeMap<String, String> frobParamList = new TreeMap<String, String>();
        frobParamList.put("method", GET_FROB_METHOD);
        frobParamList.put("api_key", apiKey);
        String jsonText = performRequest(frobParamList, true);
        Gson gson = new Gson();
        Frob frob = gson.fromJson(jsonText, Frob.class);
        return frob.frob;
    }

    public String getToken() {
        TreeMap<String, String> params = new TreeMap<String, String>();
        params.put("api_key", apiKey);
        params.put("frob", frob);
        params.put("method", "vimeo.auth.getToken");
        String jsonResp = this.performRequest(params, true);
        Gson gson = new Gson();
        Token token = gson.fromJson(jsonResp, Token.class);
        String tokenStr = token.getAuth().getToken();
        return tokenStr;
    }

    public String getUploadTicketId() {
        TreeMap<String, String> frobParamList = new TreeMap<String, String>();
        frobParamList.put("method", "vimeo.videos.getUploadTicket");
        frobParamList.put("api_key", apiKey);
        frobParamList.put("auth_token", this.TEMP_AUTH_TOKEN);
        String jsonText = performRequest(frobParamList, true);
        Gson gson = new Gson();
        UploadTicket uploadTicket = gson.fromJson(jsonText, UploadTicket.class);
        return uploadTicket.ticket.id;
    }

    public String uploadVideo(String ticketId, String filePath) {
        TreeMap<String, String> uploadParams = new TreeMap<String, String>();
        String url = "http://www.vimeo.com/services/upload/";
        uploadParams.put("api_key", apiKey);
        uploadParams.put("auth_token", this.TEMP_AUTH_TOKEN);
        uploadParams.put("ticket_id", ticketId);
        uploadParams.put("format", "json");
        String signature = this.generateAppSignature(uploadParams);
        uploadParams.put("api_sig", signature);
        ClientHttpRequest request = null;
        try {
            request = new ClientHttpRequest(new URL(url).openConnection());
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (Entry<String, String> param : uploadParams.entrySet()) {
            try {
                request.setParameter(param.getKey(), param.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        InputStream videoInput = null;
        try {
            videoInput = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            request.setParameter("video", filePath, videoInput);
        } catch (IOException e) {
            e.printStackTrace();
        }
        InputStream response = null;
        try {
            response = request.post();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            InputStreamReader inR = new InputStreamReader(response);
            BufferedReader buf = new BufferedReader(inR);
            String line;
            try {
                while ((line = buf.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "hey";
    }

    public String getUploadStatus(String ticketId) {
        TreeMap<String, String> params = new TreeMap<String, String>();
        params.put("method", "vimeo.videos.checkUploadStatus");
        params.put("api_key", apiKey);
        params.put("ticket_id", ticketId);
        params.put("auth_token", this.TEMP_AUTH_TOKEN);
        String jsonText = performRequest(params, true);
        Gson gson = new Gson();
        UploadStatus uploadStatus = gson.fromJson(jsonText, UploadStatus.class);
        int uploading = uploadStatus.ticket.is_uploading;
        int transcoding = uploadStatus.ticket.is_transcoding;
        return uploadStatus.ticket.video_id;
    }

    public String performRequest(TreeMap<String, String> params, boolean isAuthenticated) {
        params.put("format", "json");
        try {
            URL url = new URL(getApiUrl(params, isAuthenticated));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            Reader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String response = "";
            while (reader.ready()) {
                response += (char) reader.read();
            }
            response = response.replaceFirst("jsonVimeoApi\\(", "");
            response = response.substring(0, response.length() - 2);
            return response;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getApiUrl(TreeMap<String, String> params, boolean isAuthenticated) {
        String url = "http://www.vimeo.com/api/rest?";
        for (Entry<String, String> param : params.entrySet()) {
            url += param.getKey() + "=" + param.getValue() + "&";
        }
        if (isAuthenticated) {
            String signature = this.generateAppSignature(params);
            url += "api_sig=" + signature;
        } else url = url.substring(0, url.lastIndexOf("&"));
        return url;
    }

    /**
	 * 
	 * @param parameterMap
	 * @return
	 */
    public String generateAppSignature(TreeMap<String, String> params) {
        if (params == null) params = new TreeMap<String, String>();
        params.put("format", "json");
        String signature = secretKey;
        for (Entry<String, String> param : params.entrySet()) {
            signature += param.getKey() + param.getValue();
        }
        try {
            return UtilMisc.getMd5(signature);
        } catch (NoSuchAlgorithmException e) {
            log.error("Missing MD5 Algorithm.", e);
        }
        return null;
    }

    public String getLoginLink() {
        if (this.loginLink == null) this.setLoginLink(this.generateLoginLink());
        return loginLink;
    }

    public void setLoginLink(String loginLink) {
        this.loginLink = loginLink;
    }

    public User getUser(String username) {
        URL url;
        try {
            url = new URL("http://vimeo.com/api/" + username + "/info.json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            InputStream in = conn.getInputStream();
            Gson gson = new Gson();
            return gson.fromJson(new BufferedReader(new InputStreamReader(in)), User.class);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
