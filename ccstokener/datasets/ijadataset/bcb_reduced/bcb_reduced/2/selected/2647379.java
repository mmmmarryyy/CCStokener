package net.sf.brightside.eterminals.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class Payment {

    private static String servletURL = "http://10.15.4.27:8080/PaymentPlugin/PaymentPluginServlet?";

    private static String encodingType = "UTF-8";

    public Payment() {
        super();
    }

    public int chargeAmount(Long TransactionID, String MSISDN, String refTid, double Amount) {
        int result = -1;
        BufferedReader br = null;
        InputStreamReader isr = null;
        InputStream urlConnInStr = null;
        String strTransactionID = TransactionID.toString();
        int length = strTransactionID.length();
        String baseOfTransactionID = "999";
        for (int i = 0; i < (11 - length); i++) {
            baseOfTransactionID = baseOfTransactionID + "0";
        }
        strTransactionID = baseOfTransactionID + strTransactionID;
        String pRequestType = "RequestType=chargeAmount";
        String pTransactionID = "TransactionId=" + strTransactionID;
        String pRole = "ReqCred.Role=4";
        String pUserID = "ReqCred.UserId=MASTER";
        String pPIN = "ReqCred.PIN=null";
        String pAccessFrontendID = "AccessFrontendId=VAS";
        String pConsumerID = "ConsumerId=" + MSISDN;
        String pAccountID = "ConsumerAccountId=0";
        String pConsumerPIN = "ConsumerPIN=null";
        String pMerchantID = "MerchantId=MASTER";
        String pProductID = "ProductId=MASTER002";
        String pPurpose = "Purpose=Moj_omiljeni_broj";
        String pCurrency = "Money.Currency=DIN";
        long lAmount = (long) (Math.round(Amount * 1000));
        String strAmount = Long.toString(lAmount);
        String pAmount = "Money.Amount=" + strAmount;
        String spec = servletURL + pRequestType + "&" + pTransactionID + "&" + pRole + "&" + pUserID + "&" + pPIN + "&" + pAccessFrontendID + "&" + pConsumerID + "&" + pAccountID + "&" + pConsumerPIN + "&" + pMerchantID + "&" + pProductID + "&" + pPurpose + "&" + pCurrency + "&" + pAmount;
        System.out.println("Poslato: " + spec);
        try {
            URL url = new URL(spec);
            URLConnection urlConn = url.openConnection();
            urlConnInStr = urlConn.getInputStream();
            isr = new InputStreamReader(urlConnInStr);
            br = new BufferedReader(isr);
            String line = br.readLine();
            if (line != null) {
                result = getExecutionStatus(line);
            }
        } catch (MalformedURLException exc1) {
            exc1.printStackTrace();
            System.out.println(exc1.toString() + " Method: ChargeAmount; MalformedURLException");
        } catch (IOException exc2) {
            exc2.printStackTrace();
            System.out.println(exc2.toString() + " Method: ChargeAmount; IOException");
        } finally {
            try {
                if (br != null) br.close();
                if (isr != null) isr.close();
                if (urlConnInStr != null) urlConnInStr.close();
            } catch (IOException exc1) {
                exc1.printStackTrace();
                System.out.println(exc1.toString() + " Method: ChargeAmount; IOException");
            }
            return result;
        }
    }

    public int rechargeAmount(Long TransactionID, String MSISDN, String Purpose, double Amount, long ExpiryDate, String Merchant_id) {
        int result = -1;
        BufferedReader br = null;
        InputStreamReader isr = null;
        InputStream urlConnInStr = null;
        String strTransactionID = TransactionID.toString();
        int length = strTransactionID.length();
        String baseOfTransactionID = "999";
        for (int i = 0; i < (11 - length); i++) {
            baseOfTransactionID = baseOfTransactionID + "0";
        }
        strTransactionID = baseOfTransactionID + strTransactionID;
        String pRequestType = "RequestType=rechargeAmount";
        String pTransactionID = "TransactionId=" + strTransactionID;
        String pRole = "ReqCred.Role=4";
        String pUserID = "ReqCred.UserId=";
        String pPIN = "ReqCred.PIN=null";
        String pAccessFrontendID = "AccessFrontendId=mts";
        String pConsumerID = "ConsumerId=" + MSISDN;
        String pAccountID = "ConsumerAccountId=0";
        String pConsumerPIN = "ConsumerPIN=null";
        String pPurpose = "Purpose";
        try {
            pPurpose = "Purpose=" + URLEncoder.encode(Purpose, encodingType);
            pUserID = "ReqCred.UserId=" + URLEncoder.encode(Merchant_id, encodingType);
        } catch (UnsupportedEncodingException exc1) {
            exc1.printStackTrace();
            return -1;
        }
        String pCurrency = "Money.Currency=DIN";
        long lAmount = (long) (Amount * 1000);
        String strAmount = Long.toString(lAmount);
        String pAmount = "Money.Amount=" + strAmount;
        String pExpiryDate = "ExpiryDate=" + Long.toString(ExpiryDate);
        String spec = servletURL + pRequestType + "&" + pTransactionID + "&" + pRole + "&" + pUserID + "&" + pPIN + "&" + pAccessFrontendID + "&" + pConsumerID + "&" + pAccountID + "&" + pConsumerPIN + "&" + pPurpose + "&" + pCurrency + "&" + pAmount + "&" + pExpiryDate;
        try {
            URL url = new URL(spec);
            URLConnection urlConn = url.openConnection();
            urlConnInStr = urlConn.getInputStream();
            isr = new InputStreamReader(urlConnInStr);
            br = new BufferedReader(isr);
            String line = br.readLine();
            if (line != null) {
                result = getExecutionStatus(line);
            }
        } catch (MalformedURLException exc1) {
            exc1.printStackTrace();
        } catch (IOException exc2) {
            exc2.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
                if (isr != null) isr.close();
                if (urlConnInStr != null) urlConnInStr.close();
            } catch (IOException exc1) {
                exc1.printStackTrace();
            }
            return result;
        }
    }

    private int getExecutionStatus(String line) {
        int result = 1;
        int execPosition = line.indexOf("ExecutionStatus=");
        String strExecutionStatus = line.substring(execPosition + 16);
        result = Integer.parseInt(strExecutionStatus);
        return result;
    }

    public String getConsumerAccountList(Long TransactionID, String msisdns) {
        String result = null;
        BufferedReader br = null;
        InputStreamReader isr = null;
        InputStream urlConnInStr = null;
        String strTransactionID = TransactionID.toString();
        int length = strTransactionID.length();
        String baseOfTransactionID = "999";
        for (int i = 0; i < (11 - length); i++) {
            baseOfTransactionID = baseOfTransactionID + "0";
        }
        strTransactionID = baseOfTransactionID + strTransactionID;
        String pRequestType = "RequestType=getConsumerAccountList";
        String pTransactionID = "TransactionId=" + strTransactionID;
        String pRole = "ReqCred.Role=6";
        String pUserID = "ReqCred.UserId=INQUIRY-C";
        String pPIN = "ReqCred.PIN=";
        String pAccessFrontendID = "AccessFrontendId=HTTP";
        String pUserIdList = "UserIdList=" + msisdns;
        String pAccountID = "AccountIdList=0";
        String pPINList = "PINList=";
        String spec = servletURL + pRequestType + "&" + pTransactionID + "&" + pRole;
        spec += "&" + pUserID + "&" + pPIN + "&" + pAccessFrontendID + "&" + pUserIdList + "&";
        spec += pAccountID + "&" + pPINList;
        System.out.println("Poslato: " + spec);
        try {
            URL url = new URL(spec);
            URLConnection urlConn = url.openConnection();
            urlConnInStr = urlConn.getInputStream();
            isr = new InputStreamReader(urlConnInStr);
            br = new BufferedReader(isr);
            String line = "";
            while (true) {
                String l = br.readLine();
                if (l == null) break;
                line += l + "\r\n";
            }
            if (line != null) {
                result = getBalance(line);
            }
        } catch (MalformedURLException exc1) {
            exc1.printStackTrace();
            System.out.println(exc1.toString() + " Method: GetTAState; MalformedURLException");
        } catch (IOException exc2) {
            exc2.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
                if (isr != null) isr.close();
                if (urlConnInStr != null) urlConnInStr.close();
            } catch (IOException exc1) {
                exc1.printStackTrace();
            }
            return result;
        }
    }

    private String getBalance(String str) {
        String line = str.substring(0, str.indexOf("\r"));
        int execStatus = getExecutionStatus(line);
        if (execStatus == 1) {
            try {
                int execPosition = str.indexOf("Balance.1=");
                String balanceLine = str.substring(execPosition);
                String strBalance = balanceLine.substring("Balance.1=".length(), balanceLine.indexOf("\r") - 1);
                return strBalance;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else if (execStatus == 13 || execStatus == 7) return "ERROR: NOT PREPAID"; else return "ERROR: " + str;
    }
}
