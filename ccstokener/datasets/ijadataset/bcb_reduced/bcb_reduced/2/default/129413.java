import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SmsLib {

    private String password;

    private String username;

    private String server = "https://www.sms.ethz.ch/send.pl";

    public SmsLib() {
    }

    public SmsLib(String aUsername, String aPassword) {
        username = aUsername;
        password = aPassword;
    }

    public void initiate(String aUsername, String aPassword) {
        username = aUsername;
        password = aPassword;
    }

    public HashMap<String, PhoneBookEntry> getAddresBook() throws SmsLibException {
        LinkedList<String> retParam = httprequest("listaddressbook", new HashMap<String, String>());
        HashMap<String, PhoneBookEntry> book = new HashMap<String, PhoneBookEntry>();
        for (String line : retParam) {
            String[] linearray = line.split(",");
            if (linearray.length > 1) {
                book.put(linearray[1].substring(1, linearray[1].length() - 1), new PhoneBookEntry(linearray[2].substring(1, linearray[2].length() - 1), linearray[0].substring(1, linearray[0].length() - 1)));
            }
        }
        return (book);
    }

    public void sendSms(String[] reciver, String message) throws SmsLibException {
        if (message.length() > 300) {
            throw new SmsLibException(SmsLibException.Code.MESSAGE_TOO_LONG, "The message you would like to send is too long. At most 300 charactzer are allowed!");
        }
        HashMap<String, String> parameter = new HashMap<String, String>();
        String actualReciver;
        for (int i = 0; i < reciver.length; i++) {
            actualReciver = reciver[i].replaceAll(" ", "");
            parameter.put("originator", "auto");
            parameter.put("reciver", actualReciver);
            httprequest("sendsms", parameter);
        }
    }

    public void modifyAddress(String rowid, String number, String name) throws SmsLibException {
        number = number.replaceAll(" ", "");
        rowid = rowid.replaceAll(" ", "");
        HashMap<String, String> hashy = new HashMap<String, String>();
        hashy.put("rowid", rowid);
        hashy.put("name", name);
        hashy.put("number", number);
        httprequest("modifyaddress", hashy);
    }

    public void deleteAddress(String rowid) throws SmsLibException {
        rowid = rowid.replaceAll(" ", "");
        HashMap<String, String> hashy = new HashMap<String, String>();
        hashy.put("rowid", rowid);
        httprequest("deleteaddress", hashy);
    }

    public void addAddress(String name, String number) throws SmsLibException {
        number = number.replaceAll(" ", "");
        name = name.replaceAll(" ", "+");
        HashMap<String, String> hashy = new HashMap<String, String>();
        hashy.put("name", name);
        hashy.put("number", number);
        httprequest("addaddress", hashy);
    }

    private LinkedList<String> httprequest(String action, HashMap<String, String> parameters) throws SmsLibException {
        HttpURLConnection conn = null;
        try {
            String line = server + "?username=" + username + "&password=" + password + "&action=" + action;
            for (String key : parameters.keySet()) {
                line = line + "&" + key + "=" + parameters.get(key);
            }
            URL url = new URL(line);
            conn = (HttpURLConnection) url.openConnection();
        } catch (Exception e) {
            throw new SmsLibException(SmsLibException.Code.COULD_NOT_CONNECT_SERVER_ERROR, "Could not connect to server");
        }
        try {
            conn.connect();
        } catch (IOException e) {
            throw new SmsLibException(SmsLibException.Code.COULD_NOT_CONNECT_REQUEST_ERROR, "Could not connect to server");
        }
        try {
            int ret = conn.getResponseCode();
            if (ret != 200) {
                throw new SmsLibException(ret);
            }
        } catch (IOException e) {
            throw new SmsLibException(SmsLibException.Code.COULD_NOT_READ_STATUS_CODE, "Could not read status code");
        }
        BufferedReader rd;
        try {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } catch (IOException e) {
            throw new SmsLibException(SmsLibException.Code.NO_ANSWER_ERROR, "Did not recive answer");
        }
        LinkedList<String> retParam = new LinkedList<String>();
        try {
            String line;
            while ((line = rd.readLine()) != null) {
                retParam.add(line);
            }
        } catch (IOException e) {
            throw new SmsLibException(SmsLibException.Code.BUFFERD_READ_ERROR, "Could not read from BufferdReader. This should not happen");
        }
        if (retParam.size() == 1) {
            try {
                Integer code = Integer.decode(retParam.get(0).substring(0, 3));
                if (code.intValue() != 200) {
                    throw new SmsLibException(code);
                }
            } catch (NumberFormatException error) {
            }
        }
        return retParam;
    }

    public static class SmsLibException extends Exception {

        private static final long serialVersionUID = 1L;

        enum Code {

            COULD_NOT_CONNECT_SERVER_ERROR, COULD_NOT_CONNECT_REQUEST_ERROR, NO_ANSWER_ERROR, BUFFERD_READ_ERROR, INVALID_STATUS_CODE, SERVER_ERROR, COULD_NOT_READ_STATUS_CODE, PROTOCOL_ERROR, MESSAGE_TOO_LONG, NOT_IMPLEMENTED
        }

        public Code code;

        public String message;

        public SmsLibException(Code aCode, String aMess) {
            code = aCode;
            message = aMess;
        }

        public SmsLibException(int ret) {
            this.code = Code.SERVER_ERROR;
            switch(ret) {
                case 200:
                    this.message = "Called analyseError with 200 return code! This sould not happen";
                    break;
                case 400:
                    this.message = "No valid value for action. This should not happen.";
                    break;
                case 401:
                    this.message = "Username not defined. This should not happen";
                    break;
                case 402:
                    this.message = "Invalid Username";
                    break;
                case 403:
                    this.message = "Invalid Password";
                    break;
                case 404:
                    this.message = "No message defined. This should not happen";
                    break;
                case 405:
                    this.message = "No number defined. This should not happen";
                    break;
                case 406:
                    this.message = "No originator defined. This should not happen";
                    break;
                case 407:
                    this.message = "Invalid originator";
                    break;
                case 408:
                    this.message = "No rowid given. This should not happen";
                    break;
                case 409:
                    this.message = "Invalid ID";
                    break;
                case 410:
                    this.message = "No name given. This should not happen";
                    break;
                case 411:
                    this.message = "There is already a phone book entry with this name";
                    break;
                case 500:
                    this.message = "Unknow server exception";
                    break;
                default:
                    this.message = "Unknow status code " + ret + ". This should not happen.";
            }
        }

        @Override
        public String getMessage() {
            return ("SmsLibException " + code.ordinal() + ": " + message);
        }
    }

    public class PhoneBookEntry {

        String number;

        String rowid;

        public PhoneBookEntry(String aNumber, String aRowid) {
            number = aNumber;
            rowid = aRowid;
        }
    }
}
