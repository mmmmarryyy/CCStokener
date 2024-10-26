package jpastebin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author thotheolh
 */
public class PostData {

    private String paste_code_key = "paste_code";

    private String paste_name_key = "paste_name";

    private String paste_email_key = "paste_email";

    private String paste_subdomain_key = "paste_subdomain";

    private String paste_private_key = "paste_private";

    private String paste_expire_date_key = "paste_expire_date";

    private String paste_format_key = "paste_format";

    private String paste_code_data = null;

    private String paste_name_data = null;

    private String paste_email_data = null;

    private String paste_subdomain_data = null;

    private String paste_private_data = null;

    private String paste_expire_date_data = null;

    private String paste_format_data = null;

    private String paramData = "";

    public PostData() {
    }

    public void loadParams(String key, String data_val) {
        try {
            if (getParamData().length() != 0) {
                setParamData(getParamData() + "&");
            }
            setParamData(getParamData() + URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(data_val, "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(PostData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String uploadPost(String textToUpload, String nameAndTitle, String email, String subdomain, boolean isPrivate, String expirydate, String textFormatType) {
        try {
            this.setPaste_code_data(textToUpload);
            this.setPaste_name_data(nameAndTitle);
            this.setPaste_email_data(email);
            this.setPaste_subdomain_data(subdomain);
            if (isPrivate) {
                this.setPaste_private_data("1");
            } else {
                this.setPaste_private_data("0");
            }
            this.setPaste_expire_date_data(expirydate);
            this.setPaste_format_data(textFormatType);
            loadParams(this.paste_code_key, this.getPaste_code_data());
            if (this.getPaste_name_data() != null) {
                loadParams(this.paste_name_key, this.getPaste_name_data());
            }
            if (this.getPaste_email_data() != null) {
                loadParams(this.paste_email_key, this.getPaste_email_data());
            }
            if (this.getPaste_subdomain_data() != null) {
                loadParams(this.paste_subdomain_key, this.getPaste_subdomain_data());
            }
            loadParams(this.paste_private_key, this.getPaste_private_data());
            if (this.getPaste_expire_date_data() != null) {
                loadParams(this.paste_expire_date_key, this.getPaste_expire_date_data());
            }
            if (this.getPaste_format_data() != null) {
                loadParams(this.paste_format_key, this.getPaste_format_data());
            }
            URL url = new URL("http://pastebin.com/api_public.php");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter writeOut = new OutputStreamWriter(conn.getOutputStream());
            writeOut.write(getParamData());
            writeOut.flush();
            BufferedReader readIn = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = readIn.readLine();
            writeOut.close();
            readIn.close();
            return line;
        } catch (IOException ex) {
            Logger.getLogger(PostData.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * @return the paste_code_data
     */
    public String getPaste_code_data() {
        return paste_code_data;
    }

    /**
     * @param paste_code_data the paste_code_data to set
     */
    public void setPaste_code_data(String paste_code_data) {
        this.paste_code_data = paste_code_data;
    }

    /**
     * @return the paste_name_data
     */
    public String getPaste_name_data() {
        return paste_name_data;
    }

    /**
     * @param paste_name_data the paste_name_data to set
     */
    public void setPaste_name_data(String paste_name_data) {
        this.paste_name_data = paste_name_data;
    }

    /**
     * @return the paste_email_data
     */
    public String getPaste_email_data() {
        return paste_email_data;
    }

    /**
     * @param paste_email_data the paste_email_data to set
     */
    public void setPaste_email_data(String paste_email_data) {
        this.paste_email_data = paste_email_data;
    }

    /**
     * @return the paste_subdomain_data
     */
    public String getPaste_subdomain_data() {
        return paste_subdomain_data;
    }

    /**
     * @param paste_subdomain_data the paste_subdomain_data to set
     */
    public void setPaste_subdomain_data(String paste_subdomain_data) {
        this.paste_subdomain_data = paste_subdomain_data;
    }

    /**
     * @return the paste_private_data
     */
    public String getPaste_private_data() {
        return paste_private_data;
    }

    /**
     * @param paste_private_data the paste_private_data to set
     */
    public void setPaste_private_data(String paste_private_data) {
        this.paste_private_data = paste_private_data;
    }

    /**
     * @return the paste_expire_date_data
     */
    public String getPaste_expire_date_data() {
        return paste_expire_date_data;
    }

    /**
     * @param paste_expire_date_data the paste_expire_date_data to set
     */
    public void setPaste_expire_date_data(String paste_expire_date_data) {
        this.paste_expire_date_data = paste_expire_date_data;
    }

    /**
     * @return the paramData
     */
    public String getParamData() {
        return paramData;
    }

    /**
     * @param paramData the paramData to set
     */
    public void setParamData(String paramData) {
        this.paramData = paramData;
    }

    /**
     * @return the paste_format_data
     */
    public String getPaste_format_data() {
        return paste_format_data;
    }

    /**
     * @param paste_format_data the paste_format_data to set
     */
    public void setPaste_format_data(String paste_format_data) {
        this.paste_format_data = paste_format_data;
    }
}
