package com.gittigidiyor.payment.garanti.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

class PaymentService {

    private static String sendRequest(String requestXml, String hostAddress) throws GarantiApiException {
        String responseXml = null;
        OutputStream os = null;
        BufferedReader reader = null;
        StringBuilder stringBuilder = null;
        try {
            String data = "data=" + requestXml;
            byte[] byteArray = data.getBytes("UTF8");
            URL url = new URL(hostAddress);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", new Integer(byteArray.length).toString());
            conn.setDoOutput(true);
            conn.connect();
            os = conn.getOutputStream();
            os.write(byteArray);
            os.close();
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            stringBuilder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            responseXml = stringBuilder.toString();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new GarantiApiException("Error Occured During Posting XmlData.", e.getCause());
        }
        return responseXml;
    }

    public static GarantiApiResponse processTransaction(GarantiApiRequest garantiApiRequest) throws GarantiApiException {
        GarantiApiResponse garantiApiResponse = null;
        if (garantiApiRequest == null) {
            throw new GarantiApiException("Garanti Request object must be initialized.");
        }
        RequestData requestData = new RequestData();
        requestData.populate(garantiApiRequest);
        String requestXml = XmlProcessor.getRequestXml(requestData);
        String responseXml = sendRequest(requestXml, garantiApiRequest.getHostAddress());
        garantiApiResponse = XmlProcessor.parseResponseXml(responseXml);
        return garantiApiResponse;
    }

    public static GarantiApiThreeDRequestInfo getThreeDRequestInfo(GarantiApiRequest garantiApiRequest) throws GarantiApiException {
        GarantiApiThreeDRequestInfo garantiApiThreeDRequestInfo = null;
        try {
            String amountStr = GarantiApiUtil.getAmountAsString(garantiApiRequest.getAmount());
            String installmentStr = GarantiApiUtil.getInstallmentAsString(garantiApiRequest.getInstallment());
            String secureHash = GarantiApiUtil.calculateHashAsDefaultAndUpperCase(garantiApiRequest.getProvisionPassword() + GarantiApiUtil.getArrangedTerminalId(garantiApiRequest.getTerminalId()));
            String secure3dhash = GarantiApiUtil.calculateHashAsDefaultAndUpperCase(garantiApiRequest.getTerminalId() + garantiApiRequest.getOrderId() + amountStr + garantiApiRequest.getSuccessUrl() + garantiApiRequest.getFailureUrl() + garantiApiRequest.getType() + installmentStr + garantiApiRequest.getStoreKey() + secureHash);
            garantiApiThreeDRequestInfo = new GarantiApiThreeDRequestInfo(secure3dhash, GarantiApiConstants.API_VERSION_DEFAULT, amountStr, installmentStr);
        } catch (Exception e) {
            throw new GarantiApiException("Error Occured During Request Hash Calculation", e.getCause());
        }
        return garantiApiThreeDRequestInfo;
    }
}
