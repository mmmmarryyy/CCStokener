package com.javacodegeeks.kannel.api;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.Externalizable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SMSManager {

    private static final String WAP_PUSH_RESPONSE_SUCCESS_CODE = "1001";

    private static final String SMS_PUSH_RESPONSE_SUCCESS_CODE = "0";

    public static final String WAP_PUSH_RECEIVER_TYPE_MOBILE = "PLMN";

    public static final String WAP_PUSH_RECEIVER_TYPE_IP = "IPv4";

    public static final Short MESSAGE_PRIORITY_0 = Short.valueOf((short) 0);

    public static final Short MESSAGE_PRIORITY_1 = Short.valueOf((short) 1);

    public static final Short MESSAGE_PRIORITY_2 = Short.valueOf((short) 2);

    public static final Short MESSAGE_PRIORITY_3 = Short.valueOf((short) 3);

    public static final Short MESSAGE_PRIORITY_4 = Short.valueOf((short) 4);

    public static final Short MESSAGE_PRIORITY_5 = Short.valueOf((short) 5);

    public static final Short MESSAGE_PRIORITY_6 = Short.valueOf((short) 6);

    public static final Short MESSAGE_PRIORITY_7 = Short.valueOf((short) 7);

    public static final Short MESSAGE_PRIORITY_8 = Short.valueOf((short) 8);

    public static final Short MESSAGE_PRIORITY_9 = Short.valueOf((short) 9);

    private static SMSManager instance;

    private SMSManagerWorker smsManagerWorker;

    private Map<Short, List<byte[][]>> messagesMap;

    private int messagesPrefetchSize = 10;

    private int messagesSendRate = 50;

    private DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
	 * @return a singleton instance of the SMSManager
	 */
    public static SMSManager getInstance() {
        if (instance == null) {
            instance = new SMSManager();
            instance.initializeSMSMap();
            instance.startSMSManagerWorker();
        }
        return instance;
    }

    /**
	 * 
	 * @return  the number of messages the background worker will fetch for each priority list
	 * 					 to send to Kannel SMS/WAP Gateway at every poll
	 */
    public int getMessagesPrefetchSize() {
        return messagesPrefetchSize;
    }

    /**
	 * 
	 * @param messagesPrefetchSize defines the number of messages the background worker will fetch 
	 * 					 for each priority list to send to Kannel SMS/WAP Gateway at every poll. Default value is 10
	 */
    public void setMessagesPrefetchSize(int messagesPrefetchSize) {
        this.messagesPrefetchSize = messagesPrefetchSize;
    }

    /**
	 * 
	 * @return the rate in Message/Second the background worker will send to Kannel SMS/WAP Gateway
	 */
    public int getMessagesSendRate() {
        return messagesSendRate;
    }

    /**
	 * 
	 * @param messagesSendRate defines the rate in SMS/Second the background worker will send to 
	 * 					 Kannel SMS/WAP Gateway. Default value is 50
	 */
    public void setMessagesSendRate(int messagesSendRate) {
        this.messagesSendRate = messagesSendRate;
    }

    /**
	 * 
	 * Sends an SMS using Kannel SMS/WAP Gateway
	 * 
	 * 	@param host mandatory parameter. The host name of the server where Kannel gateway is installed
	 * 
	 * @param port mandatory parameter. Kannel send SMS port, defaults to 13013
	 * 
	 * @param username mandatory parameter. Kannel send SMS user group, username
	 * 
	 * @param password mandatory parameter. Kannel send SMS user group, password
	 * 
	 * @param from mandatory parameter. Phone number of the sender. It can also 
	 * 					 be an alphanumeric string
	 * 
	 * @param to mandatory parameter. Phone number of the receiver
	 * 
	 * @param text optional contents of the message. The content can be more 
	 * 					 than 160 characters, SMS gateway does the message splitting function 
	 * 					 transparently to the end user
	 * 
	 * @return kannel response (Upon successfully submitted request, the http interface replies with the text string "Sent.")
	 * 
	 * @throws SMSPushRequestException on all errorneous responses from the gateway
	 * 
	 * @throws Exception
	 * 
	 */
    public String sendSMS(String host, String port, String username, String password, String from, String to, String text) throws SMSPushRequestException, Exception {
        return sendSMS(host, port, username, password, from, to, text, null, null, null, null, null, null, null, null, null, null);
    }

    /**
	 * 
	 * Sends an SMS using Kannel SMS/WAP Gateway. This method is asynchronous, meaning that the message
	 * is not sent to Kannel SMS/WAP Gateway in the call, but is stored in memory given the specified priority. An 
	 * background worker thread is polling for pending messages and sends them to Kannel SMS/WAP Gateway
	 * according to their priority and send rate.
	 * 
	 * 	@param host mandatory parameter. The host name of the server where Kannel gateway is installed
	 * 
	 * @param port mandatory parameter. Kannel send SMS port, defaults to 13013
	 * 
	 * @param username mandatory parameter. Kannel send SMS user group, username
	 * 
	 * @param password mandatory parameter. Kannel send SMS user group, password
	 * 
	 * @param from mandatory parameter. Phone number of the sender. It can also 
	 * 					 be an alphanumeric string
	 * 
	 * @param to mandatory parameter. Phone number of the receiver
	 * 
	 * @param text optional contents of the message. The content can be more 
	 * 					 than 160 characters, SMS gateway does the message splitting function 
	 * 					 transparently to the end user
	 * 
	 * @param priority mandatory parameter. Accepts values from 0 through 9, 0 being the highest priority.
	 * 					SMSManager class provides convenient static fields for every available priority
	 * 	
	 * 
	 */
    public void sendSMS(String host, String port, String username, String password, String from, String to, String text, Short priority) throws Exception {
        List<String> recipientsList = new ArrayList<String>();
        recipientsList.add(to);
        SMSMessage smsMessage = new SMSMessage(host, port, username, password, from, recipientsList, text, null, null, null, null, null, null, null, null, null, null);
        addMessageToMap(priority, smsMessage);
    }

    /**
	 * 
	 * Sends an SMS to multiple recipients using Kannel SMS/WAP Gateway
	 * 
	 * 	@param host mandatory parameter. The host name of the server where Kannel gateway is installed
	 * 
	 * @param port mandatory parameter. Kannel send SMS port, defaults to 13013
	 * 
	 * @param username mandatory parameter. Kannel send SMS user group, username
	 * 
	 * @param password mandatory parameter. Kannel send SMS user group, password
	 * 
	 * @param from mandatory parameter. Phone number of the sender. It can also 
	 * 					 be an alphanumeric string
	 * 
	 * @param to mandatory parameter. Phone number(s) of the receiver(s)
	 * 
	 * @param text optional contents of the message. The content can be more 
	 * 					 than 160 characters, SMS gateway does the message splitting function 
	 * 					 transparently to the end user
	 * 
	 * @return kannel response (Upon successfully submitted request, the http interface replies with the text string "Sent.")
	 * 
	 * @throws SMSPushRequestException on all errorneous responses from the gateway
	 * 
	 * @throws Exception
	 * 
	 */
    public String sendSMS(String host, String port, String username, String password, String from, List<String> to, String text) throws SMSPushRequestException, Exception {
        return sendBulkSMS(host, port, username, password, from, to, text, null, null, null, null, null, null, null, null, null, null);
    }

    /**
	 * 
	 * Sends an SMS to multiple recipients using Kannel SMS/WAP Gateway. This method is asynchronous, meaning that messages
	 * are not sent to Kannel SMS/WAP Gateway in the call, but are stored in memory given the specified priority. An 
	 * background worker thread is polling for pending messages and sends them to Kannel SMS/WAP Gateway
	 * according to their priority and send rate.
	 * 
	 * 	@param host mandatory parameter. The host name of the server where Kannel gateway is installed
	 * 
	 * @param port mandatory parameter. Kannel send SMS port, defaults to 13013
	 * 
	 * @param username mandatory parameter. Kannel send SMS user group, username
	 * 
	 * @param password mandatory parameter. Kannel send SMS user group, password
	 * 
	 * @param from mandatory parameter. Phone number of the sender. It can also 
	 * 					 be an alphanumeric string
	 * 
	 * @param to mandatory parameter. Phone number(s) of the receiver(s)
	 * 
	 * @param text optional contents of the message. The content can be more 
	 * 					 than 160 characters, SMS gateway does the message splitting function 
	 * 					 transparently to the end user
	 * 
	 * @param priority mandatory parameter. Accepts values from 0 through 9, 0 being the highest priority.
	 * 					SMSManager class provides convenient static fields for every available priority
	 * 	
	 * 
	 */
    public void sendBulkSMS(String host, String port, String username, String password, String from, List<String> to, String text, Short priority) throws Exception {
        SMSMessage smsMessage = new SMSMessage(host, port, username, password, from, to, text, null, null, null, null, null, null, null, null, null, null);
        addMessageToMap(priority, smsMessage);
    }

    /**
	 * 
	 * Sends an SMS using Kannel SMS/WAP Gateway
	 * 
	 * 	@param host mandatory parameter. The host name of the server where Kannel gateway is installed
	 * 
	 * @param port mandatory parameter. Kannel send SMS port, defaults to 13013
	 * 
	 * @param username mandatory parameter. Kannel send SMS user group, username
	 * 
	 * @param password mandatory parameter. Kannel send SMS user group, password
	 * 
	 * @param from mandatory parameter. Phone number of the sender. It can also 
	 * 					 be an alphanumeric string
	 * 
	 * @param to mandatory parameter. Phone number of the receiver
	 * 
	 * @param text optional contents of the message. The content can be more 
	 * 					 than 160 characters, SMS gateway does the message splitting function 
	 * 					 transparently to the end user
	 * 
	 * @param uhd optional User Data Header (UDH) part of the message
	 * 
	 * @param charset optional. Charset of text message. Used to convert to a 
	 * 					 format suitable for 7 bits or to UCS2. Defaults to GSM default alphabet if 
	 * 					 coding is 7bits and UTF16BE if coding is UCS2
	 * 
	 * @param coding optional. Sets the coding scheme bits in DCS field. Accepts 
	 * 					 values 0 to 2, for 7bit, 8bit or UCS2 respectively. If unset, defaults to 7 
	 * 					 bits unless udh is defined, which sets coding to 8bits
	 * 
	 * @param validity optional. If given, SMS gateway will inform 
	 * 					 SMS Center that it should only try to send the message for this many 
	 * 					 minutes. If the destination mobile is off or in other situation that it 
	 * 					 cannot receive the sms, the SMS Center discards the message
	 * 
	 * @param deferred optional. If given, the SMS Center will 
	 * 					 postpone the message to be delivered at now plus this many minutes
	 * 
	 * @param dlrmask optional. Request for delivery reports with the 
	 * 					 state of the sent message. The value is a bit mask composed of: 1: Delivered 
	 * 					 to phone, 2: Non-Delivered to Phone, 4: Queued on SMSC, 8:  Delivered to 
	 * 					 SMSC, 16: Non-Delivered to SMSC. Usually dlrmask = 3 is used. In this case 
	 * 					 SMS Center sends delivery reports to SMS gateway only when sms is delivered 
	 * 					 to phone or not-delivered to phone (1+2)
	 * 
	 * @param dlrurl optional. If dlrmask is set, this is the url to be 
	 * 					 fetched.E.g. http://someDomain/...?type=%d? , 
	 * 					 type parameter holds the value 
	 * 					 for the status of the SMS
	 * 
	 * @param pid optional. Sets the PID value. PID = 64 is used to mark the SMS 
	 * 					 as silent SMS (SMS0)
	 * 
	 * @param mclass optional. Sets the Message Class in DCS Field. Accepts 
	 * 					 values between 1 and 4. Possible cases per mclass value are: 
	 * 					 1 - sends the message directly to display 
	 * 					 2 - sends to mobile 
	 * 					 3 - sends to SIM 
	 * 					 4 - sends to SIM Toolkit
	 * 
	 * @param mwi optional. Sets Message Waiting Indicator bits in DCS field. If 
	 * 					 given, the message will be encoded as a Message Waiting Indicator. The 
	 * 					 accepted values are 1, 2, 3 and 4 for activating the voice, fax, email and 
	 * 					 other indicator, or 5, 6, 7, 8 for deactivating, respectively
	 * 
	 * @return kannel response (Upon successfully submitted request, the http interface replies with the text string "Sent.")
	 * 
	 * @throws SMSPushRequestException on all errorneous responses from the gateway
	 * 
	 * @throws Exception
	 * 
	 */
    public String sendSMS(String host, String port, String username, String password, String from, String to, String text, String uhd, String charset, String coding, String validity, String deferred, String dlrmask, String dlrurl, String pid, String mclass, String mwi) throws SMSPushRequestException, Exception {
        StringBuffer res = new StringBuffer();
        if (!Utils.checkNonEmptyStringAttribute(coding) || coding.equals("0")) text = Utils.convertTextForGSMEncodingURLEncoded(text); else if (coding.equals("1")) text = Utils.convertTextForUTFEncodingURLEncoded(text, "UTF-8"); else text = Utils.convertTextForUTFEncodingURLEncoded(text, "UCS-2");
        String directives = "username=" + username;
        directives += "&password=" + password;
        directives += "&from=" + URLEncoder.encode(from, "UTF-8");
        directives += "&to=" + to;
        directives += "&text=" + text;
        if (Utils.checkNonEmptyStringAttribute(uhd)) directives += "&uhd=" + uhd;
        if (Utils.checkNonEmptyStringAttribute(charset)) directives += "&charset=" + charset;
        if (Utils.checkNonEmptyStringAttribute(coding)) directives += "&coding=" + coding;
        if (Utils.checkNonEmptyStringAttribute(validity)) directives += "&validity=" + validity;
        if (Utils.checkNonEmptyStringAttribute(deferred)) directives += "&deferred=" + deferred;
        if (Utils.checkNonEmptyStringAttribute(dlrmask)) directives += "&dlrmask=" + dlrmask;
        if (Utils.checkNonEmptyStringAttribute(dlrurl)) directives += "&dlrurl=" + dlrurl;
        if (Utils.checkNonEmptyStringAttribute(pid)) directives += "&pid=" + pid;
        if (Utils.checkNonEmptyStringAttribute(mclass)) directives += "&mclass=" + mclass;
        if (Utils.checkNonEmptyStringAttribute(mwi)) directives += "&mwi=" + mwi;
        URL url = new URL("http://" + host + ":" + port + "/cgi-bin/sendsms?" + directives);
        URLConnection conn = url.openConnection();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String response;
        while ((response = rd.readLine()) != null) res.append(response);
        rd.close();
        String resultCode = res.substring(0, res.indexOf(":"));
        if (!resultCode.equals(SMS_PUSH_RESPONSE_SUCCESS_CODE)) throw new SMSPushRequestException(resultCode);
        return res.toString();
    }

    /**
	 * 
	 * Sends an SMS using Kannel SMS/WAP Gateway. This method is asynchronous, meaning that the message
	 * is not sent to Kannel SMS/WAP Gateway in the call, but is stored in memory given the specified priority. An 
	 * background worker thread is polling for pending messages and sends them to Kannel SMS/WAP Gateway
	 * according to their priority and send rate.
	 * 
	 * 	@param host mandatory parameter. The host name of the server where Kannel gateway is installed
	 * 
	 * @param port mandatory parameter. Kannel send SMS port, defaults to 13013
	 * 
	 * @param username mandatory parameter. Kannel send SMS user group, username
	 * 
	 * @param password mandatory parameter. Kannel send SMS user group, password
	 * 
	 * @param from mandatory parameter. Phone number of the sender. It can also 
	 * 					 be an alphanumeric string
	 * 
	 * @param to mandatory parameter. Phone number of the receiver
	 * 
	 * @param text optional contents of the message. The content can be more 
	 * 					 than 160 characters, SMS gateway does the message splitting function 
	 * 					 transparently to the end user
	 * 
	 * @param uhd optional User Data Header (UDH) part of the message
	 * 
	 * @param charset optional. Charset of text message. Used to convert to a 
	 * 					 format suitable for 7 bits or to UCS2. Defaults to GSM default alphabet if 
	 * 					 coding is 7bits and UTF16BE if coding is UCS2
	 * 
	 * @param coding optional. Sets the coding scheme bits in DCS field. Accepts 
	 * 					 values 0 to 2, for 7bit, 8bit or UCS2 respectively. If unset, defaults to 7 
	 * 					 bits unless udh is defined, which sets coding to 8bits
	 * 
	 * @param validity optional. If given, SMS gateway will inform 
	 * 					 SMS Center that it should only try to send the message for this many 
	 * 					 minutes. If the destination mobile is off or in other situation that it 
	 * 					 cannot receive the sms, the SMS Center discards the message
	 * 
	 * @param deferred optional. If given, the SMS Center will 
	 * 					 postpone the message to be delivered at now plus this many minutes
	 * 
	 * @param dlrmask optional. Request for delivery reports with the 
	 * 					 state of the sent message. The value is a bit mask composed of: 1: Delivered 
	 * 					 to phone, 2: Non-Delivered to Phone, 4: Queued on SMSC, 8:  Delivered to 
	 * 					 SMSC, 16: Non-Delivered to SMSC. Usually dlrmask = 3 is used. In this case 
	 * 					 SMS Center sends delivery reports to SMS gateway only when sms is delivered 
	 * 					 to phone or not-delivered to phone (1+2)
	 * 
	 * @param dlrurl optional. If dlrmask is set, this is the url to be 
	 * 					 fetched.E.g. http://someDomain/...?type=%d? , 
	 * 					 type parameter holds the value 
	 * 					 for the status of the SMS
	 * 
	 * @param pid optional. Sets the PID value. PID = 64 is used to mark the SMS 
	 * 					 as silent SMS (SMS0)
	 * 
	 * @param mclass optional. Sets the Message Class in DCS Field. Accepts 
	 * 					 values between 1 and 4. Possible cases per mclass value are: 
	 * 					 1 - sends the message directly to display 
	 * 					 2 - sends to mobile 
	 * 					 3 - sends to SIM 
	 * 					 4 - sends to SIM Toolkit
	 * 
	 * @param mwi optional. Sets Message Waiting Indicator bits in DCS field. If 
	 * 					 given, the message will be encoded as a Message Waiting Indicator. The 
	 * 					 accepted values are 1, 2, 3 and 4 for activating the voice, fax, email and 
	 * 					 other indicator, or 5, 6, 7, 8 for deactivating, respectively
	 * 
	 * 	@param priority mandatory parameter. Accepts values from 0 through 9, 0 being the highest priority.
	 * 					SMSManager class provides convenient static fields for every available priority
	 * 
	 */
    public void sendSMS(String host, String port, String username, String password, String from, String to, String text, String uhd, String charset, String coding, String validity, String deferred, String dlrmask, String dlrurl, String pid, String mclass, String mwi, Short priority) throws Exception {
        List<String> recipientsList = new ArrayList<String>();
        recipientsList.add(to);
        SMSMessage smsMessage = new SMSMessage(host, port, username, password, from, recipientsList, text, uhd, charset, coding, validity, deferred, dlrmask, dlrurl, pid, mclass, mwi);
        addMessageToMap(priority, smsMessage);
    }

    /**
	 * 
	 * Sends an SMS to multiple recipients using Kannel SMS/WAP Gateway
	 * 
	 * 	@param host mandatory parameter. The host name of the server where Kannel gateway is installed
	 * 
	 * @param port mandatory parameter. Kannel send SMS port, defaults to 13013
	 * 
	 * @param username mandatory parameter. Kannel send SMS user group, username
	 * 
	 * @param password mandatory parameter. Kannel send SMS user group, password
	 * 
	 * @param from mandatory parameter. Phone number of the sender. It can also 
	 * 					 be an alphanumeric string
	 * 
	 * @param to mandatory parameter. Phone number(s) of the receiver(s)
	 * 
	 * @param text optional contents of the message. The content can be more 
	 * 					 than 160 characters, SMS gateway does the message splitting function 
	 * 					 transparently to the end user
	 * 
	 * @param uhd optional User Data Header (UDH) part of the message
	 * 
	 * @param charset optional. Charset of text message. Used to convert to a 
	 * 					 format suitable for 7 bits or to UCS2. Defaults to GSM default alphabet if 
	 * 					 coding is 7bits and UTF16BE if coding is UCS2
	 * 
	 * @param coding optional. Sets the coding scheme bits in DCS field. Accepts 
	 * 					 values 0 to 2, for 7bit, 8bit or UCS2 respectively. If unset, defaults to 7 
	 * 					 bits unless udh is defined, which sets coding to 8bits
	 * 
	 * @param validity optional. If given, SMS gateway will inform 
	 * 					 SMS Center that it should only try to send the message for this many 
	 * 					 minutes. If the destination mobile is off or in other situation that it 
	 * 					 cannot receive the sms, the SMS Center discards the message
	 * 
	 * @param deferred optional. If given, the SMS Center will 
	 * 					 postpone the message to be delivered at now plus this many minutes
	 * 
	 * @param dlrmask optional. Request for delivery reports with the 
	 * 					 state of the sent message. The value is a bit mask composed of: 1: Delivered 
	 * 					 to phone, 2: Non-Delivered to Phone, 4: Queued on SMSC, 8:  Delivered to 
	 * 					 SMSC, 16: Non-Delivered to SMSC. Usually dlrmask = 3 is used. In this case 
	 * 					 SMS Center sends delivery reports to SMS gateway only when sms is delivered 
	 * 					 to phone or not-delivered to phone (1+2)
	 * 
	 * @param dlrurl optional. If dlrmask is set, this is the url to be 
	 * 					 fetched.E.g. http://someDomain/...?type=%d? , 
	 * 					 type parameter holds the value 
	 * 					 for the status of the SMS
	 * 
	 * @param pid optional. Sets the PID value. PID = 64 is used to mark the SMS 
	 * 					 as silent SMS (SMS0)
	 * 
	 * @param mclass optional. Sets the Message Class in DCS Field. Accepts 
	 * 					 values between 1 and 4. Possible cases per mclass value are: 
	 * 					 1 - sends the message directly to display 
	 * 					 2 - sends to mobile 
	 * 					 3 - sends to SIM 
	 * 					 4 - sends to SIM Toolkit
	 * 
	 * @param mwi optional. Sets Message Waiting Indicator bits in DCS field. If 
	 * 					 given, the message will be encoded as a Message Waiting Indicator. The 
	 * 					 accepted values are 1, 2, 3 and 4 for activating the voice, fax, email and 
	 * 					 other indicator, or 5, 6, 7, 8 for deactivating, respectively
	 * 
	 * @return kannel response (Upon successfully submitted request, the http interface replies with the text string "Sent.")
	 * 
	 * @throws SMSPushRequestException on all errorneous responses from the gateway
	 * 
	 * @throws Exception
	 * 
	 */
    public String sendBulkSMS(String host, String port, String username, String password, String from, List<String> to, String text, String uhd, String charset, String coding, String validity, String deferred, String dlrmask, String dlrurl, String pid, String mclass, String mwi) throws SMSPushRequestException, Exception {
        StringBuffer res = new StringBuffer();
        if (!Utils.checkNonEmptyStringAttribute(coding) || coding.equals("0")) text = Utils.convertTextForGSMEncodingURLEncoded(text); else if (coding.equals("1")) text = Utils.convertTextForUTFEncodingURLEncoded(text, "UTF-8"); else text = Utils.convertTextForUTFEncodingURLEncoded(text, "UCS-2");
        String directives = "username=" + username;
        directives += "&password=" + password;
        directives += "&from=" + URLEncoder.encode(from, "UTF-8");
        StringBuffer receivers = new StringBuffer();
        Iterator<String> it = to.iterator();
        while (it.hasNext()) receivers.append(it.next() + "+");
        receivers.deleteCharAt(receivers.length() - 1);
        directives += "&to=" + receivers.toString();
        directives += "&text=" + text;
        if (Utils.checkNonEmptyStringAttribute(uhd)) directives += "&uhd=" + uhd;
        if (Utils.checkNonEmptyStringAttribute(charset)) directives += "&charset=" + charset;
        if (Utils.checkNonEmptyStringAttribute(coding)) directives += "&coding=" + coding;
        if (Utils.checkNonEmptyStringAttribute(validity)) directives += "&validity=" + validity;
        if (Utils.checkNonEmptyStringAttribute(deferred)) directives += "&deferred=" + deferred;
        if (Utils.checkNonEmptyStringAttribute(dlrmask)) directives += "&dlrmask=" + dlrmask;
        if (Utils.checkNonEmptyStringAttribute(dlrurl)) directives += "&dlrurl=" + dlrurl;
        if (Utils.checkNonEmptyStringAttribute(pid)) directives += "&pid=" + pid;
        if (Utils.checkNonEmptyStringAttribute(mclass)) directives += "&mclass=" + mclass;
        if (Utils.checkNonEmptyStringAttribute(mwi)) directives += "&mwi=" + mwi;
        URL url = new URL("http://" + host + ":" + port + "/cgi-bin/sendsms?" + directives);
        URLConnection conn = url.openConnection();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String response;
        while ((response = rd.readLine()) != null) res.append(response);
        rd.close();
        String resultCode = res.substring(0, res.indexOf(":"));
        if (!resultCode.equals(SMS_PUSH_RESPONSE_SUCCESS_CODE)) throw new SMSPushRequestException(resultCode);
        return res.toString();
    }

    /**
	 * 
	 * Sends an SMS to multiple recipients using Kannel SMS/WAP Gateway. This method is asynchronous, meaning that the messages
	 * are not sent to Kannel SMS/WAP Gateway in the call, but are stored in memory given the specified priority. An 
	 * background worker thread is polling for pending messages and sends them to Kannel SMS/WAP Gateway
	 * according to their priority and send rate.
	 * 
	 * 	@param host mandatory parameter. The host name of the server where Kannel gateway is installed
	 * 
	 * @param port mandatory parameter. Kannel send SMS port, defaults to 13013
	 * 
	 * @param username mandatory parameter. Kannel send SMS user group, username
	 * 
	 * @param password mandatory parameter. Kannel send SMS user group, password
	 * 
	 * @param from mandatory parameter. Phone number of the sender. It can also 
	 * 					 be an alphanumeric string
	 * 
	 * @param to mandatory parameter. Phone number(s) of the receiver(s)
	 * 
	 * @param text optional contents of the message. The content can be more 
	 * 					 than 160 characters, SMS gateway does the message splitting function 
	 * 					 transparently to the end user
	 * 
	 * @param uhd optional User Data Header (UDH) part of the message
	 * 
	 * @param charset optional. Charset of text message. Used to convert to a 
	 * 					 format suitable for 7 bits or to UCS2. Defaults to GSM default alphabet if 
	 * 					 coding is 7bits and UTF16BE if coding is UCS2
	 * 
	 * @param coding optional. Sets the coding scheme bits in DCS field. Accepts 
	 * 					 values 0 to 2, for 7bit, 8bit or UCS2 respectively. If unset, defaults to 7 
	 * 					 bits unless udh is defined, which sets coding to 8bits
	 * 
	 * @param validity optional. If given, SMS gateway will inform 
	 * 					 SMS Center that it should only try to send the message for this many 
	 * 					 minutes. If the destination mobile is off or in other situation that it 
	 * 					 cannot receive the sms, the SMS Center discards the message
	 * 
	 * @param deferred optional. If given, the SMS Center will 
	 * 					 postpone the message to be delivered at now plus this many minutes
	 * 
	 * @param dlrmask optional. Request for delivery reports with the 
	 * 					 state of the sent message. The value is a bit mask composed of: 1: Delivered 
	 * 					 to phone, 2: Non-Delivered to Phone, 4: Queued on SMSC, 8:  Delivered to 
	 * 					 SMSC, 16: Non-Delivered to SMSC. Usually dlrmask = 3 is used. In this case 
	 * 					 SMS Center sends delivery reports to SMS gateway only when sms is delivered 
	 * 					 to phone or not-delivered to phone (1+2)
	 * 
	 * @param dlrurl optional. If dlrmask is set, this is the url to be 
	 * 					 fetched.E.g. http://someDomain/...?type=%d? , 
	 * 					 type parameter holds the value 
	 * 					 for the status of the SMS
	 * 
	 * @param pid optional. Sets the PID value. PID = 64 is used to mark the SMS 
	 * 					 as silent SMS (SMS0)
	 * 
	 * @param mclass optional. Sets the Message Class in DCS Field. Accepts 
	 * 					 values between 1 and 4. Possible cases per mclass value are: 
	 * 					 1 - sends the message directly to display 
	 * 					 2 - sends to mobile 
	 * 					 3 - sends to SIM 
	 * 					 4 - sends to SIM Toolkit
	 * 
	 * @param mwi optional. Sets Message Waiting Indicator bits in DCS field. If 
	 * 					 given, the message will be encoded as a Message Waiting Indicator. The 
	 * 					 accepted values are 1, 2, 3 and 4 for activating the voice, fax, email and 
	 * 					 other indicator, or 5, 6, 7, 8 for deactivating, respectively
	 * 
	 * 	@param priority mandatory parameter. Accepts values from 0 through 9, 0 being the highest priority.
	 * 					SMSManager class provides convenient static fields for every available priority
	 * 
	 */
    public void sendBulkSMS(String host, String port, String username, String password, String from, List<String> to, String text, String uhd, String charset, String coding, String validity, String deferred, String dlrmask, String dlrurl, String pid, String mclass, String mwi, Short priority) throws Exception {
        SMSMessage smsMessage = new SMSMessage(host, port, username, password, from, to, text, uhd, charset, coding, validity, deferred, dlrmask, dlrurl, pid, mclass, mwi);
        addMessageToMap(priority, smsMessage);
    }

    /**
	 * 
	 * Sends a single WAP Push request to a receiver
	 * 
	 * 	@param host mandatory parameter. The host name of the server where Kannel PPG gateway is installed
	 * 
	 * @param port mandatory parameter. The port PPG is listening at. Default 8080
	 * 
	 * @param receiver mandatory parameter. Mobile number or IPv4 of the receiver
	 * 
	 * @param type mandatory parameter. PLMN for mobile reveivers, IPv4 for all other. Please use the relevant static
	 * 					attributes provided
	 * 
	 * @param message mandatory parameter. Service indication (SI) message to be displayed on the receiver device
	 * 
	 * @param url mandatory parameter. URL for actual content push
	 * 
	 * @param expirationDays mandatory parameter. Expiration days for the WAP PUSH request
	 * 
	 * @throws WAPPushRequestException on all errorneous responses from the gateway
	 * 
	 * @throws Exception
	 */
    public void sendWAPPush(String host, String port, String receiver, String type, String message, String url, int expirationDays) throws WAPPushRequestException, Exception {
        sendWAPPush(host, port, receiver, type, message, url, expirationDays, null, null);
    }

    /**
	 * 
	 * Sends a single WAP Push request to a receiver
	 * 
	 * 	@param host mandatory parameter. The host name of the server where Kannel PPG gateway is installed
	 * 
	 * @param port mandatory parameter. The port PPG is listening at. Default 8080
	 * 
	 * @param receiver mandatory parameter. Mobile number or IPv4 of the receiver
	 * 
	 * @param type mandatory parameter. PLMN for mobile reveivers, IPv4 for all other. Please use the relevant static
	 * 					attributes provided
	 * 
	 * @param message mandatory parameter. Service indication (SI) message to be displayed on the receiver device
	 * 
	 * @param url mandatory parameter. URL for actual content push
	 * 
	 * @param expirationDays mandatory parameter. Expiration days for the WAP PUSH request
	 * 
	 * 	@param dlrmask optional. Request for delivery reports with the 
	 * 					 state of the sent message. The value is a bit mask composed of: 1: Delivered 
	 * 					 to phone, 2: Non-Delivered to Phone, 4: Queued on SMSC, 8:  Delivered to 
	 * 					 SMSC, 16: Non-Delivered to SMSC. Usually dlrmask = 3 is used. In this case 
	 * 					 SMS Center sends delivery reports to SMS gateway only when sms is delivered 
	 * 					 to phone or not-delivered to phone (1+2)
	 * 
	 * @param dlrurl optional. If dlrmask is set, this is the url to be 
	 * 					 fetched.E.g. http://someDomain/...?type=%d? , 
	 * 					 type parameter holds the value 
	 * 					 for the status of the WAP Push
	 * 
	 * @throws WAPPushRequestException on all errorneous responses from the gateway
	 * 
	 * @throws Exception
	 */
    public void sendWAPPush(String host, String port, String receiver, String type, String message, String url, int expirationDays, String dlrmask, String dlrurl) throws WAPPushRequestException, Exception {
        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) + expirationDays);
        Date expirationDate = new Date(cal.getTimeInMillis());
        String siMessage = createSIMessage(url, df.format(now), df.format(expirationDate), message);
        receiver = ((type.equals(SMSManager.WAP_PUSH_RECEIVER_TYPE_MOBILE) && !receiver.startsWith("+")) ? ("+" + receiver) : receiver);
        String slMessage = createSLMessage("" + System.currentTimeMillis(), df.format(expirationDate), df.format(now), receiver, type);
        URLConnection connection = new URL("http://" + host + ":" + port + "/wappush").openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-type", "multipart/related; boundary=frontier; type=\"application/xml\"");
        connection.setRequestProperty("X-WAP-Application-Id", "http://www.wiral.com:wml.ua");
        connection.setRequestProperty("Authorization", "Basic YTph");
        if (Utils.checkNonEmptyStringAttribute(dlrmask)) connection.setRequestProperty("X-Kannel-DLR-Mask", dlrmask);
        if (Utils.checkNonEmptyStringAttribute(dlrurl)) connection.setRequestProperty("X-Kannel-DLR-URL", dlrurl);
        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        StringBuilder requestBuffer = new StringBuilder();
        requestBuffer.append("--frontier");
        requestBuffer.append("\r\n");
        requestBuffer.append("Content-Type: application/xml" + "\r\n");
        requestBuffer.append("\r\n");
        requestBuffer.append(slMessage);
        requestBuffer.append("\r\n" + "--frontier");
        requestBuffer.append("\r\n");
        requestBuffer.append("Content-Type: text/vnd.wap.si" + "\r\n");
        requestBuffer.append("\r\n");
        requestBuffer.append(siMessage);
        requestBuffer.append("\r\n" + "--frontier--");
        out.writeBytes(requestBuffer.toString());
        out.flush();
        out.close();
        InputStream stream = connection.getInputStream();
        BufferedInputStream in = new BufferedInputStream(stream);
        StringBuilder responseBuffer = new StringBuilder();
        int i = 0;
        while ((i = in.read()) != -1) {
            responseBuffer.append((char) i);
        }
        in.close();
        String responseCode = Utils.parseWAPPushResponseForResultCode(responseBuffer.toString());
        if (responseCode == null || !responseCode.equals(WAP_PUSH_RESPONSE_SUCCESS_CODE)) throw new WAPPushRequestException(responseCode);
    }

    /**
	 * 
	 * Sends a WAP Push request to a single recipient using Kannel SMS/WAP Gateway. This method is asynchronous, meaning that the message
	 * is not sent to Kannel SMS/WAP Gateway in the call, but is stored in memory given the specified priority. An 
	 * background worker thread is polling for pending messages and sends them to Kannel SMS/WAP Gateway
	 * according to their priority and send rate.
	 * 
	 * 	@param host mandatory parameter. The host name of the server where Kannel PPG gateway is installed
	 * 
	 * @param port mandatory parameter. The port PPG is listening at. Default 8080
	 * 
	 * @param receiver mandatory parameter. Mobile number or IPv4 of the receiver
	 * 
	 * @param type mandatory parameter. PLMN for mobile reveivers, IPv4 for all other. Please use the relevant static
	 * 					attributes provided
	 * 
	 * @param message mandatory parameter. Service indication (SI) message to be displayed on the receiver device
	 * 
	 * @param url mandatory parameter. URL for actual content push
	 * 
	 * @param expirationDays mandatory parameter. Expiration days for the WAP PUSH request
	 * 
	 * 	@param priority mandatory parameter. Accepts values from 0 through 9, 0 being the highest priority.
	 * 					SMSManager class provides convenient static fields for every available priority
	 * 
	 * @throws WAPPushRequestException on all errorneous responses from the gateway
	 * 
	 * @throws Exception
	 */
    public void sendWAPPush(String host, String port, String receiver, String type, String message, String url, int expirationDays, Short priority) throws Exception {
        List<String> recipientsList = new ArrayList<String>();
        recipientsList.add(receiver);
        WAPPushMessage wapPushMessage = new WAPPushMessage(host, port, recipientsList, type, message, url, expirationDays, null, null);
        addMessageToMap(priority, wapPushMessage);
    }

    /**
	 * 
	 * Sends a WAP Push request to a single recipient using Kannel SMS/WAP Gateway. This method is asynchronous, meaning that the message
	 * is not sent to Kannel SMS/WAP Gateway in the call, but is stored in memory given the specified priority. An 
	 * background worker thread is polling for pending messages and sends them to Kannel SMS/WAP Gateway
	 * according to their priority and send rate.
	 * 
	 * 	@param host mandatory parameter. The host name of the server where Kannel PPG gateway is installed
	 * 
	 * @param port mandatory parameter. The port PPG is listening at. Default 8080
	 * 
	 * @param receiver mandatory parameter. Mobile number or IPv4 of the receiver
	 * 
	 * @param type mandatory parameter. PLMN for mobile reveivers, IPv4 for all other. Please use the relevant static
	 * 					attributes provided
	 * 
	 * @param message mandatory parameter. Service indication (SI) message to be displayed on the receiver device
	 * 
	 * @param url mandatory parameter. URL for actual content push
	 * 
	 * @param expirationDays mandatory parameter. Expiration days for the WAP PUSH request
	 * 
	 * 	@param dlrmask optional. Request for delivery reports with the 
	 * 					 state of the sent message. The value is a bit mask composed of: 1: Delivered 
	 * 					 to phone, 2: Non-Delivered to Phone, 4: Queued on SMSC, 8:  Delivered to 
	 * 					 SMSC, 16: Non-Delivered to SMSC. Usually dlrmask = 3 is used. In this case 
	 * 					 SMS Center sends delivery reports to SMS gateway only when sms is delivered 
	 * 					 to phone or not-delivered to phone (1+2)
	 * 
	 * @param dlrurl optional. If dlrmask is set, this is the url to be 
	 * 					 fetched.E.g. http://someDomain/...?type=%d? , 
	 * 					 type parameter holds the value 
	 * 					 for the status of the WAP Push
	 * 
	 * 	@param priority mandatory parameter. Accepts values from 0 through 9, 0 being the highest priority.
	 * 					SMSManager class provides convenient static fields for every available priority
	 * 
	 * @throws WAPPushRequestException on all errorneous responses from the gateway
	 * 
	 * @throws Exception
	 */
    public void sendWAPPush(String host, String port, String receiver, String type, String message, String url, int expirationDays, String dlrmask, String dlrurl, Short priority) throws Exception {
        List<String> recipientsList = new ArrayList<String>();
        recipientsList.add(receiver);
        WAPPushMessage wapPushMessage = new WAPPushMessage(host, port, recipientsList, type, message, url, expirationDays, dlrmask, dlrurl);
        addMessageToMap(priority, wapPushMessage);
    }

    /**
	 * 
	 * 	Sends a WAP Push request to multiple recipients using Kannel SMS/WAP Gateway. This method is asynchronous, meaning that the messages
	 * are not sent to Kannel SMS/WAP Gateway in the call, but are stored in memory given the specified priority. An 
	 * background worker thread is polling for pending messages and sends them to Kannel SMS/WAP Gateway
	 * according to their priority and send rate.
	 * 
	 * 	@param host mandatory parameter. The host name of the server where Kannel PPG gateway is installed
	 * 
	 * @param port mandatory parameter. The port PPG is listening at. Default 8080
	 * 
	 * @param receivers mandatory parameter. Mobile number(s) or IPv4 of the receiver(s)
	 * 
	 * @param type mandatory parameter. PLMN for mobile reveivers, IPv4 for all other. Please use the relevant static
	 * 					attributes provided
	 * 
	 * @param message mandatory parameter. Service indication (SI) message to be displayed on the receiver device
	 * 
	 * @param url mandatory parameter. URL for actual content push
	 * 
	 * @param expirationDays mandatory parameter. Expiration days for the WAP PUSH request
	 * 
	 * 	@param priority mandatory parameter. Accepts values from 0 through 9, 0 being the highest priority.
	 * 					SMSManager class provides convenient static fields for every available priority
	 * 
	 * @throws WAPPushRequestException on all errorneous responses from the gateway
	 * 
	 * @throws Exception
	 */
    public void sendBulkWAPPush(String host, String port, List<String> receivers, String type, String message, String url, int expirationDays, Short priority) throws Exception {
        WAPPushMessage wapPushMessage = new WAPPushMessage(host, port, receivers, type, message, url, expirationDays, null, null);
        addMessageToMap(priority, wapPushMessage);
    }

    /**
	 * 
	 * 	Sends a WAP Push request to multiple recipients using Kannel SMS/WAP Gateway. This method is asynchronous, meaning that the messages
	 * are not sent to Kannel SMS/WAP Gateway in the call, but are stored in memory given the specified priority. An 
	 * background worker thread is polling for pending messages and sends them to Kannel SMS/WAP Gateway
	 * according to their priority and send rate.
	 * 
	 * 	@param host mandatory parameter. The host name of the server where Kannel PPG gateway is installed
	 * 
	 * @param port mandatory parameter. The port PPG is listening at. Default 8080
	 * 
	 * @param receivers mandatory parameter. Mobile number(s) or IPv4 of the receiver(s)
	 * 
	 * @param type mandatory parameter. PLMN for mobile reveivers, IPv4 for all other. Please use the relevant static
	 * 					attributes provided
	 * 
	 * @param message mandatory parameter. Service indication (SI) message to be displayed on the receiver device
	 * 
	 * @param url mandatory parameter. URL for actual content push
	 * 
	 * @param expirationDays mandatory parameter. Expiration days for the WAP PUSH request
	 * 
	 * 	@param dlrmask optional. Request for delivery reports with the 
	 * 					 state of the sent message. The value is a bit mask composed of: 1: Delivered 
	 * 					 to phone, 2: Non-Delivered to Phone, 4: Queued on SMSC, 8:  Delivered to 
	 * 					 SMSC, 16: Non-Delivered to SMSC. Usually dlrmask = 3 is used. In this case 
	 * 					 SMS Center sends delivery reports to SMS gateway only when sms is delivered 
	 * 					 to phone or not-delivered to phone (1+2)
	 * 
	 * @param dlrurl optional. If dlrmask is set, this is the url to be 
	 * 					 fetched.E.g. http://someDomain/...?type=%d? , 
	 * 					 type parameter holds the value 
	 * 					 for the status of the WAP Push
	 * 
	 * 	@param priority mandatory parameter. Accepts values from 0 through 9, 0 being the highest priority.
	 * 					SMSManager class provides convenient static fields for every available priority
	 * 
	 * @throws WAPPushRequestException on all errorneous responses from the gateway
	 * 
	 * @throws Exception
	 */
    public void sendBulkWAPPush(String host, String port, List<String> receivers, String type, String message, String url, int expirationDays, String dlrmask, String dlrurl, Short priority) throws Exception {
        WAPPushMessage wapPushMessage = new WAPPushMessage(host, port, receivers, type, message, url, expirationDays, dlrmask, dlrurl);
        addMessageToMap(priority, wapPushMessage);
    }

    private void addMessageToMap(Short priority, Externalizable message) throws Exception {
        List<byte[][]> messagesList = messagesMap.get(priority);
        synchronized (messagesList) {
            messagesList.add(Utils.serializeMessage(message));
        }
    }

    private void initializeSMSMap() {
        messagesMap = new TreeMap<Short, List<byte[][]>>();
        messagesMap.put(MESSAGE_PRIORITY_0, new ArrayList<byte[][]>());
        messagesMap.put(MESSAGE_PRIORITY_1, new ArrayList<byte[][]>());
        messagesMap.put(MESSAGE_PRIORITY_2, new ArrayList<byte[][]>());
        messagesMap.put(MESSAGE_PRIORITY_3, new ArrayList<byte[][]>());
        messagesMap.put(MESSAGE_PRIORITY_4, new ArrayList<byte[][]>());
        messagesMap.put(MESSAGE_PRIORITY_5, new ArrayList<byte[][]>());
        messagesMap.put(MESSAGE_PRIORITY_6, new ArrayList<byte[][]>());
        messagesMap.put(MESSAGE_PRIORITY_7, new ArrayList<byte[][]>());
        messagesMap.put(MESSAGE_PRIORITY_8, new ArrayList<byte[][]>());
        messagesMap.put(MESSAGE_PRIORITY_9, new ArrayList<byte[][]>());
    }

    private String createSIMessage(String url, String creationDate, String expirationDate, String indicationMessage) {
        String siMessage = "<?xml version=\"1.0\"?>" + "<!DOCTYPE si PUBLIC \"-//WAPFORUM//DTD SI 1.0//EN\" \"http://www.wapforum.org/DTD/si.dtd\">" + "<si>" + "<indication " + "href=\"" + url + "\" " + "si-id=\"1@gni.ch\" " + "action=\"signal-high\" " + "created=\"" + creationDate + "\" " + "si-expires=\"" + expirationDate + "\">" + indicationMessage + "</indication>" + "</si>";
        return siMessage;
    }

    private String createSLMessage(String pushId, String deliverBefore, String deliverAfter, String receiver, String type) {
        String slMessage = "<?xml version=\"1.0\"?>" + "<!DOCTYPE pap PUBLIC \"-//WAPFORUM//DTD PAP//EN\" \"http://www.wapforum.org/DTD/pap_1.0.dtd\">" + "<pap>" + "<push-message " + "push-id=\"" + pushId + "\" " + "deliver-before-timestamp=\"" + deliverBefore + "\" " + "deliver-after-timestamp=\"" + deliverAfter + "\" " + "progress-notes-requested=\"false\">" + "<address " + "address-value=\"WAPPUSH=" + receiver + "/TYPE=" + type + "\">" + "</address>" + "<quality-of-service " + "priority=\"low\" " + "delivery-method=\"unconfirmed\">" + "</quality-of-service>" + "</push-message>" + "</pap>";
        return slMessage;
    }

    /**
	 * Starts the background worker thread so as to send asynchronous messages 
	 * to Kannel SMS/WAP Gateway
	 */
    public void startSMSManagerWorker() {
        if (smsManagerWorker == null) {
            smsManagerWorker = new SMSManagerWorker();
            Thread worker = new Thread(smsManagerWorker);
            worker.setDaemon(true);
            worker.start();
        }
    }

    /**
	 * Stops the background worker thread
	 */
    public void stopSMSManagerWorker() {
        smsManagerWorker.stop();
        smsManagerWorker = null;
    }

    private class SMSManagerWorker implements Runnable {

        private boolean stop = false;

        private long lastMessageSentTimeMillis = 0;

        private long sendMessageCount = 0;

        public void run() {
            while (!stop) {
                Iterator<List<byte[][]>> it = messagesMap.values().iterator();
                while (it.hasNext()) {
                    List<byte[][]> availableMessages = it.next();
                    List<byte[][]> messagesToSend = new ArrayList<byte[][]>();
                    int reqsCount = Math.min(availableMessages.size(), messagesPrefetchSize);
                    synchronized (availableMessages) {
                        for (int i = 0; i < reqsCount; i++) messagesToSend.add(availableMessages.remove(0));
                    }
                    Iterator<byte[][]> it1 = messagesToSend.iterator();
                    while (it1.hasNext()) {
                        try {
                            Externalizable message = Utils.deserializeMessage(it1.next());
                            if (message instanceof SMSMessage) sendSMSMessage((SMSMessage) message); else sendWAPPushMessage((WAPPushMessage) message);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                try {
                    Thread.sleep(1);
                } catch (Exception ex) {
                }
            }
        }

        private void sendSMSMessage(SMSMessage smsMessage) throws Exception {
            Iterator<String> it = smsMessage.getBnumbers().iterator();
            while (it.hasNext()) {
                long sendMessageTimeSlot = (System.currentTimeMillis() - lastMessageSentTimeMillis);
                if (sendMessageTimeSlot < 1000 && sendMessageCount >= messagesSendRate) {
                    Thread.sleep((1000 - sendMessageTimeSlot));
                    sendMessageCount = 0;
                }
                try {
                    sendSMS(smsMessage.getHost(), smsMessage.getPort(), smsMessage.getUsername(), smsMessage.getPassword(), smsMessage.getAnumber(), it.next(), smsMessage.getMessage(), smsMessage.getUhd(), smsMessage.getCharset(), smsMessage.getCoding(), smsMessage.getValidity(), smsMessage.getDeferred(), smsMessage.getDlrmask(), smsMessage.getDlrurl(), smsMessage.getPid(), smsMessage.getMclass(), smsMessage.getMwi());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                sendMessageCount++;
                lastMessageSentTimeMillis = System.currentTimeMillis();
            }
        }

        private void sendWAPPushMessage(WAPPushMessage wapPushMessage) throws Exception {
            Iterator<String> it = wapPushMessage.getBnumbers().iterator();
            while (it.hasNext()) {
                long sendMessageTimeSlot = (System.currentTimeMillis() - lastMessageSentTimeMillis);
                if (sendMessageTimeSlot < 1000 && sendMessageCount >= messagesSendRate) {
                    Thread.sleep((1000 - sendMessageTimeSlot));
                    sendMessageCount = 0;
                }
                try {
                    sendWAPPush(wapPushMessage.getHost(), wapPushMessage.getPort(), it.next(), wapPushMessage.getType(), wapPushMessage.getMessage(), wapPushMessage.getContenturl(), wapPushMessage.getExpirationDays(), wapPushMessage.getDlrmask(), wapPushMessage.getDlrurl());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                sendMessageCount++;
                lastMessageSentTimeMillis = System.currentTimeMillis();
            }
        }

        public void stop() {
            this.stop = true;
        }
    }
}
