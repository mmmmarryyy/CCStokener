package ejb.bprocess.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author siddartha
 */
public class SendSMS implements Runnable {

    public static final int SEND_SUCCESSFUL = 1;

    public static final int ERR_INVALID_SUBSCRPTION_DETAILS = 4;

    public static final int ERR_NO_CONNECTIVITY = 5;

    public static final int ERR_SMS_SERVER_ERROR = 2;

    public static final int ERR_SMS_SUBSCRIPTION_EXPIRED = 7;

    private static String rawURL = "";

    private static int staticError = 0;

    private int retint = 0;

    private static boolean isLoaded = false;

    private String mobileNumber = "";

    private String message = "";

    private static void loadValues() {
        if (!isLoaded) {
            System.out.println("**************************************1");
            String mobilealerts = SystemFilesLoader.getInstance().getNewgenlibProperties().getProperty("MOBILE_ALERTS", "");
            System.out.println("**************************************2");
            if (!mobilealerts.equals("") && mobilealerts.trim().equals("YES")) {
                System.out.println("**************************************3");
                String vsplid = SystemFilesLoader.getInstance().getNewgenlibProperties().getProperty("VERUS_SUBSCRIPTION_ID", "");
                String vsplvercode = SystemFilesLoader.getInstance().getNewgenlibProperties().getProperty("VERUS_VERIFICATION_CODE", "");
                System.out.println("**************************************4");
                File SMSURL = new File(NewGenLibRoot.getRoot() + "/SystemFiles/SMS_URL");
                if (SMSURL.exists()) {
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(SMSURL));
                        while (br.ready()) rawURL += br.readLine();
                        br.close();
                    } catch (Exception e) {
                    }
                }
                if (!vsplid.equals("") && !vsplvercode.equals("") && rawURL.equals("")) {
                    try {
                        System.out.println("**************************************5");
                        System.out.println("");
                        URL url = new URL("http://www.verussolutions.biz/mobility.php?Id=" + vsplid + "&verificationCode=" + vsplvercode);
                        URLConnection urconn = url.openConnection();
                        System.out.println("**************************************6");
                        if (ProxySettings.getInstance().isProxyAvailable()) {
                            System.out.println("Proxy settings set................................sdfsdfsd");
                            urconn.setRequestProperty("Proxy-Authorization", "Basic " + ProxySettings.getInstance().getEncodedPassword());
                        }
                        System.out.println("**************************************7");
                        urconn.setDoOutput(true);
                        System.out.println("**************************************8");
                        OutputStream os = urconn.getOutputStream();
                        os.flush();
                        System.out.println("**************************************9");
                        InputStream is = urconn.getInputStream();
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        String response = "";
                        System.out.println("**************************************10");
                        while (br.ready()) {
                            response += br.readLine();
                        }
                        br.close();
                        JSONObject jb = null;
                        try {
                            jb = new JSONObject(response);
                        } catch (Exception expp) {
                            expp.printStackTrace();
                            staticError = ERR_INVALID_SUBSCRPTION_DETAILS;
                        }
                        if (jb != null) {
                            String proceed = jb.getString("PROCEED");
                            if (proceed.equals("YES")) {
                                rawURL = jb.getString("RAWURL");
                            } else {
                                staticError = ERR_SMS_SUBSCRIPTION_EXPIRED;
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        staticError = ERR_NO_CONNECTIVITY;
                    }
                }
            }
            isLoaded = true;
        }
    }

    private SendSMS() {
        loadValues();
    }

    public static SendSMS getInstance() {
        return new SendSMS();
    }

    public static void main(String[] args) {
        SendSMS.getInstance().send("9247660498", "Youhavecheckoutblahblahblah");
    }

    public void send(String mobileNumber, String message) {
        this.mobileNumber = mobileNumber;
        this.message = message;
    }

    @Override
    public void run() {
        if (!rawURL.equals("")) {
            try {
                char[] array = message.toCharArray();
                Vector messages = new Vector(1, 1);
                if (array.length < 165) {
                    messages.addElement(message);
                } else {
                    for (int i = 0; i < array.length; i += 165) {
                        int j = i + 165;
                        if (j >= array.length) {
                            j = array.length - 1;
                        }
                        char[] newarr = Arrays.copyOfRange(array, i, j);
                        messages.addElement(new String(newarr));
                    }
                }
                for (int i = 0; i < messages.size(); i++) {
                    String messageToSend = messages.elementAt(i).toString();
                    String[] str = new String[2];
                    str[0] = mobileNumber;
                    str[1] = URLEncoder.encode(messageToSend);
                    MessageFormat mf = new MessageFormat(rawURL);
                    String finalurl = mf.format(rawURL, str);
                    URL urlU = new URL(finalurl);
                    URLConnection ucon = urlU.openConnection();
                    if (ProxySettings.getInstance().isProxyAvailable()) {
                        System.out.println("Proxy settings set................................");
                        ucon.setRequestProperty("Proxy-Authorization", "Basic " + ProxySettings.getInstance().getEncodedPassword());
                    }
                    ucon.setDoOutput(true);
                    OutputStream os = ucon.getOutputStream();
                    InputStream is = ucon.getInputStream();
                    BufferedReader irs = new BufferedReader(new InputStreamReader(is));
                    while (irs.ready()) {
                        System.out.println("***********************************8" + irs.readLine());
                    }
                    is.close();
                    os.close();
                }
                retint = SEND_SUCCESSFUL;
            } catch (Exception exp) {
                retint = ERR_SMS_SERVER_ERROR;
                exp.printStackTrace();
            }
        }
    }

    /**
     * @return the retint
     */
    public int getRetint() {
        return retint;
    }
}
