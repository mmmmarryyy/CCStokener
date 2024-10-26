package com.hisham.creditcard.skipjack.util;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import com.hisham.creditcard.skipjack.*;

/**
 *
 * <p>Title: SjTransactionResources</p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 *
 * @author Ali Hisham Malik
 * @version 2.0
 */
public class SjTransactionResources implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1334022535387734432L;

    public static final String AUTHORIZE = "authorize";

    public static final String CREDIT = "credit";

    public static final String DELETE = "delete";

    public static final String SETTLE = "settle";

    public static final String SPLITSETTLE = "splitsettle";

    public static final String skipjackResources = "SkipjackResources";

    protected String SjTransactionAuthorizeUrl = "";

    protected String SjTransactionChangeStatusRequestUrl = "";

    protected String htmlSerialNumber;

    protected String developerSerialNumber;

    private static SjTransactionResources sjTransactionResources = null;

    public static SjTransactionResources getInstance() {
        if (sjTransactionResources == null) return sjTransactionResources = new SjTransactionResources();
        return sjTransactionResources;
    }

    private SjTransactionResources() {
        init();
    }

    /**
	 *
	 * @param htmlSerialNumber String
	 */
    public void setHtmlSerialNumber(String htmlSerialNumber) {
        this.htmlSerialNumber = htmlSerialNumber;
    }

    /**
	 *
	 * @return String
	 */
    public String getHtmlSerialNumber() {
        return htmlSerialNumber;
    }

    /**
	 * initialize the provider
	 *
	 */
    public void init() {
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internalwww.protocol");
        java.security.Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        ResourceBundle skipjackBundle = ResourceBundle.getBundle(skipjackResources);
        this.setSjTransactionAuthorizeUrl(skipjackBundle.getString("AuthorizeAPIUrl"));
        this.setSjTransactionChangeStatusRequestUrl(skipjackBundle.getString("ChangeTransactionStatusUrl"));
        this.setHtmlSerialNumber(skipjackBundle.getString("htmlSerialNumber"));
        this.setDeveloperSerialNumber(skipjackBundle.getString("developerSerialNumber"));
    }

    /**
	 * AUTHORIZE transaction change status request
	 * @param orderNumber String
	 * @param orderAmount String
	 * @param transactionId String
	 * @param forceSettlement String
	 * @return boolean
	 */
    public boolean SjAuthorize(String orderNumber, String orderAmount, String transactionId, String forceSettlement) {
        return (this.transactionChangeStatusRequest("AUTHORIZE", orderNumber, orderAmount, transactionId, forceSettlement));
    }

    /**
	 *
	 * @param orderCommand String
	 * @param orderNumber String
	 * @param orderAmount double
	 * @param transactionID String
	 * @param forceSettlement boolean
	 * @return boolean
	 */
    public boolean changeTransactionStatus(String orderCommand, String orderNumber, double orderAmount, String transactionID, boolean forceSettlement) {
        DecimalFormat formatter = new DecimalFormat("#.00");
        String transactionAmount = formatter.format(orderAmount);
        return transactionChangeStatusRequest(orderCommand, orderNumber, transactionAmount, transactionID, forceSettlement ? "1" : "0");
    }

    /**
	 * transaction change status request
	 * @param orderCommand String
	 * @param orderNumber String
	 * @param orderAmount String
	 * @param transactionId String
	 * @param forceSettlement String
	 * @return boolean
	 */
    public boolean transactionChangeStatusRequest(String orderCommand, String orderNumber, String orderAmount, String transactionId, String forceSettlement) {
        try {
            StringBuffer query = new StringBuffer();
            query.append("szSerialNumber=");
            query.append(this.getHtmlSerialNumber());
            query.append("&szDeveloperSerialNumber=");
            query.append(this.getDeveloperSerialNumber());
            query.append("&szOrderNumber=");
            query.append(orderNumber.trim());
            query.append("&szDesiredStatus=");
            query.append(orderCommand.trim());
            query.append("&szAmount=");
            query.append(orderAmount.trim());
            query.append("&szTransactionId=");
            query.append(transactionId.trim());
            query.append("&szForceSettlement=");
            query.append(forceSettlement.trim());
            URL url = new URL("https://" + this.SjTransactionChangeStatusRequestUrl);
            URLConnection connection = url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            DataOutputStream output = new DataOutputStream(connection.getOutputStream());
            output.writeBytes(query.toString());
            output.close();
            DataInputStream in = new DataInputStream(connection.getInputStream());
            for (int c = in.read(); c != -1; c = in.read()) {
                System.out.print((char) c);
            }
            in.close();
            return true;
        } catch (Exception E) {
            System.out.println(E.getMessage());
            return false;
        } catch (Throwable T) {
            System.out.println(T.getMessage());
            return false;
        }
    }

    /**
	 *
	 * @param SjTransactionAuthorizeUrl String
	 */
    public void setSjTransactionAuthorizeUrl(String SjTransactionAuthorizeUrl) {
        this.SjTransactionAuthorizeUrl = SjTransactionAuthorizeUrl;
    }

    /**
	 *
	 * @param SjTransactionChangeStatusRequestUrl String
	 */
    public void setSjTransactionChangeStatusRequestUrl(String SjTransactionChangeStatusRequestUrl) {
        this.SjTransactionChangeStatusRequestUrl = SjTransactionChangeStatusRequestUrl;
    }

    /**
	 *
	 * @return String
	 */
    public String getDeveloperSerialNumber() {
        return developerSerialNumber;
    }

    /**
	 *
	 * @param developerSerialNumber String
	 */
    public void setDeveloperSerialNumber(String developerSerialNumber) {
        this.developerSerialNumber = developerSerialNumber;
    }

    /**
	 *
	 * @param oos ObjectOutputStream
	 * @throws IOException
	 */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }

    /**
	 *
	 * @param ois ObjectInputStream
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
    }

    public String getSjTransactionAuthorizeUrl() {
        return SjTransactionAuthorizeUrl;
    }

    public String getSjTransactionChangeStatusRequestUrl() {
        return SjTransactionChangeStatusRequestUrl;
    }

    /**
	 * constructs the order string for transaction post
	 * @param itemNumber String
	 * @param itemDescription String
	 * @param itemCost String
	 * @param itemQuantity String
	 * @param itemTaxable String
	 * @return String
	 */
    public String getOrderString(String itemNumber, String itemDescription, String itemCost, String itemQuantity, String itemTaxable) {
        StringBuffer sb = new StringBuffer();
        sb.append(itemNumber);
        sb.append("~");
        sb.append(itemDescription);
        sb.append("~");
        sb.append(itemCost);
        sb.append("~");
        sb.append(itemQuantity);
        sb.append("~");
        sb.append(itemTaxable);
        sb.append("~||");
        return (sb.toString());
    }

    /**
	 *
	 * @param sjOrder SjTransactionInfo
	 * @throws UnsupportedEncodingException
	 * @return String
	 */
    private String createTransactionQuery(SjTransactionInfo sjOrder) throws UnsupportedEncodingException {
        String charEncoding = "UTF-8";
        StringBuffer query = new StringBuffer();
        query.append("serialnumber=");
        query.append(this.getHtmlSerialNumber());
        query.append("&orderstring=");
        query.append(URLEncoder.encode(sjOrder.getOrderString().trim(), charEncoding));
        query.append("&ordernumber=");
        query.append(URLEncoder.encode(sjOrder.getOrderNumber().trim(), charEncoding));
        query.append("&sjname=");
        query.append(URLEncoder.encode(sjOrder.getCreditcard().getCcName().trim(), charEncoding));
        query.append("&email=");
        query.append(URLEncoder.encode(sjOrder.getBillingInfo().getEmailAddress().trim(), charEncoding));
        query.append("&streetaddress=");
        query.append(URLEncoder.encode(sjOrder.getBillingInfo().getMailingAddress().getAddressLine1().trim(), charEncoding));
        query.append("&streetaddress2=");
        query.append(URLEncoder.encode(sjOrder.getBillingInfo().getMailingAddress().getAddressLine2().trim(), charEncoding));
        query.append("&city=");
        query.append(URLEncoder.encode(sjOrder.getBillingInfo().getMailingAddress().getCity().trim(), charEncoding));
        query.append("&state=");
        query.append(URLEncoder.encode(sjOrder.getBillingInfo().getMailingAddress().getState().trim(), charEncoding));
        query.append("&zipcode=");
        query.append(URLEncoder.encode(sjOrder.getBillingInfo().getMailingAddress().getZipCode().trim(), charEncoding));
        query.append("&transactionamount=");
        DecimalFormat formatter = new DecimalFormat("#.00");
        String transactionAmount = formatter.format(sjOrder.getTransactionAmount());
        query.append(URLEncoder.encode(transactionAmount, charEncoding));
        query.append("&accountnumber=");
        query.append(URLEncoder.encode(sjOrder.getCreditcard().getCcNumber().trim(), charEncoding));
        query.append("&month=");
        query.append(URLEncoder.encode(String.valueOf(sjOrder.getCreditcard().getCcExpMonth() + 1), charEncoding));
        query.append("&year=");
        query.append(URLEncoder.encode(String.valueOf(sjOrder.getCreditcard().getCcExpYear()), charEncoding));
        query.append("&shiptoname=");
        query.append(URLEncoder.encode(sjOrder.getShippingInfo().getFullName().trim(), charEncoding));
        query.append("&shiptostreetaddress=");
        query.append(URLEncoder.encode(sjOrder.getShippingInfo().getMailingAddress().getAddressLine1().trim(), charEncoding));
        query.append("&shiptostreetaddress2=");
        query.append(URLEncoder.encode(sjOrder.getShippingInfo().getMailingAddress().getAddressLine2().trim(), charEncoding));
        query.append("&shiptocity=");
        query.append(URLEncoder.encode(sjOrder.getShippingInfo().getMailingAddress().getCity().trim(), charEncoding));
        query.append("&shiptostate=");
        query.append(URLEncoder.encode(sjOrder.getShippingInfo().getMailingAddress().getState().trim(), charEncoding));
        query.append("&shiptozipcode=");
        query.append(URLEncoder.encode(sjOrder.getShippingInfo().getMailingAddress().getZipCode().trim(), charEncoding));
        query.append("&shiptophone=");
        query.append(URLEncoder.encode(sjOrder.getShippingInfo().getPhoneNumber().trim(), charEncoding));
        query.append("&comment=");
        query.append(URLEncoder.encode(sjOrder.getAdditionalComments().trim(), charEncoding));
        return query.toString();
    }

    /** authorize credit card
	 *
	 * The method AuthorizeAPI returns two lines of text, as a form post.
	 * The data will be separated by a Carriage return and a line feed.
	 * The field names will be in the first line and their values in the second.
	 * The field names returned are only those provided in the standard Skipjack
	 * response Table.
	 * The field names returned do not include UserDefinedFields.
	 *
	 * @param sjTransactionInfo SjTransactionInfo
	 * @param sjTransactionResponse SjTransactionResponse
	 * @return boolean
	 */
    public boolean approveTransaction(SjTransactionInfo sjTransactionInfo, SjTransactionResponse sjTransactionResponse) {
        try {
            String transactionQuery = createTransactionQuery(sjTransactionInfo);
            URL url = new URL("https://" + this.getSjTransactionAuthorizeUrl());
            URLConnection connection = url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            DataOutputStream output = new DataOutputStream(connection.getOutputStream());
            output.writeBytes(transactionQuery);
            output.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = in.readLine();
            response = response + "#" + in.readLine();
            in.close();
            sjTransactionResponse.parse(response);
            return sjTransactionResponse.getSjIsApproved().equals("1");
        } catch (Exception ex) {
            System.err.println("Exception occured in skipjack webauthorize function");
            ex.printStackTrace(System.err);
            sjTransactionResponse.setDeclineReason("Unable to communicate with Credit Card Processor, try again later");
            return false;
        } catch (Throwable t) {
            System.err.println("Throwing error from skipjack webauthorize function");
            t.printStackTrace(System.err);
            sjTransactionResponse.setDeclineReason("Unable to communicate with Credit Card Processor, try again later");
            return false;
        }
    }
}
