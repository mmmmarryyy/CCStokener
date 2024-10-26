package com.bccapi.core;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import com.bccapi.api.APIException;
import com.bccapi.api.AccountInfo;
import com.bccapi.api.AccountStatement;
import com.bccapi.api.BitcoinClientAPI;
import com.bccapi.api.Network;
import com.bccapi.api.SendCoinForm;
import com.bccapi.api.Tx;

/**
 * This is an implementation of the BCCAPI
 */
public class BitcoinClientApiImpl implements BitcoinClientAPI {

    private static final String BCCAPI_COM_SSL_THUMBPRINT = "b9:d9:0e:a2:7f:f4:79:3a:2b:54:be:40:ba:cb:56:65:56:13:0c:cc";

    private static final String API_VERSION = "1";

    private URL _url;

    private Network _network;

    public BitcoinClientApiImpl(URL url, Network network) {
        _url = url;
        _network = network;
    }

    @Override
    public Network getNetwork() {
        return _network;
    }

    @Override
    public byte[] getLoginChallenge(byte[] accountPublicKey) throws APIException, IOException {
        try {
            String options = "?key=" + HexUtils.toHex(accountPublicKey);
            HttpURLConnection connection = getHttpConnection("getLoginChallenge", options, null);
            connection.setRequestMethod("GET");
            connection.connect();
            int status = connection.getResponseCode();
            if (status != 200) {
                throw generateException(connection);
            }
            return StreamReader.readAllBytes(connection.getInputStream());
        } catch (IOException e) {
            throw e;
        }
    }

    @Override
    public String login(byte[] accountPublicKey, byte[] challengeResponse) throws APIException, IOException {
        try {
            String responseString = HexUtils.toHex(challengeResponse);
            String options = "?key=" + HexUtils.toHex(accountPublicKey);
            HttpURLConnection connection = getHttpConnection("login", options, null);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("response", responseString);
            connection.connect();
            int status = connection.getResponseCode();
            if (status != 200) {
                throw generateException(connection);
            }
            String sessionId = new String(StreamReader.readAllBytes(connection.getInputStream()));
            return sessionId;
        } catch (IOException e) {
            throw e;
        }
    }

    @Override
    public AccountInfo getAccountInfo(String sessionId) throws APIException, IOException {
        try {
            HttpURLConnection connection = getHttpConnection("getAccountInfo", "", sessionId);
            connection.setRequestMethod("GET");
            connection.connect();
            int status = connection.getResponseCode();
            if (status != 200) {
                throw generateException(connection);
            }
            return AccountInfo.fromStream(new DataInputStream(connection.getInputStream()));
        } catch (IOException e) {
            throw e;
        }
    }

    @Override
    public AccountStatement getAccountStatement(String sessionId, int startIndex, int count) throws IOException, APIException {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("?index=").append(startIndex);
            sb.append("&count=").append(count);
            HttpURLConnection connection = getHttpConnection("getAccountStatement", sb.toString(), sessionId);
            connection.setRequestMethod("GET");
            connection.connect();
            int status = connection.getResponseCode();
            if (status != 200) {
                throw generateException(connection);
            }
            return AccountStatement.fromStream(new DataInputStream(connection.getInputStream()));
        } catch (IOException e) {
            throw e;
        }
    }

    @Override
    public AccountStatement getRecentTransactionSummary(String sessionId, int count) throws IOException, APIException {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("?count=").append(count);
            HttpURLConnection connection = getHttpConnection("getRecentTransactionSummary", sb.toString(), sessionId);
            connection.setRequestMethod("GET");
            connection.connect();
            int status = connection.getResponseCode();
            if (status != 200) {
                throw generateException(connection);
            }
            return AccountStatement.fromStream(new DataInputStream(connection.getInputStream()));
        } catch (IOException e) {
            throw e;
        }
    }

    @Override
    public void addKeyToWallet(String sessionId, byte[] publicKey) throws APIException, IOException {
        try {
            String keyString = HexUtils.toHex(publicKey);
            HttpURLConnection connection = getHttpConnection("addKeyToWallet", "", sessionId);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Length", String.valueOf(keyString.getBytes().length));
            connection.getOutputStream().write(keyString.getBytes());
            int status = connection.getResponseCode();
            if (status != 200) {
                throw generateException(connection);
            }
        } catch (IOException e) {
            throw e;
        }
    }

    @Override
    public SendCoinForm getSendCoinForm(String sessionId, String receivingAddressString, long amount, long fee) throws APIException, IOException {
        try {
            HttpURLConnection connection = getHttpConnection("getSendCoinForm", "", sessionId);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            ByteArrayOutputStream toSend = new ByteArrayOutputStream();
            byte[] addressStringBytes = receivingAddressString.getBytes();
            toSend.write(addressStringBytes.length);
            toSend.write(addressStringBytes);
            BitUtils.uint64ToStream(amount, toSend);
            BitUtils.uint64ToStream(fee, toSend);
            connection.setRequestProperty("Content-Length", String.valueOf(toSend.toByteArray().length));
            connection.getOutputStream().write(toSend.toByteArray());
            int status = connection.getResponseCode();
            if (status != 200) {
                throw generateException(connection);
            }
            return SendCoinForm.fromStream(new DataInputStream(connection.getInputStream()));
        } catch (IOException e) {
            throw e;
        }
    }

    @Override
    public void submitTransaction(String sessionId, Tx tx) throws APIException, IOException {
        try {
            HttpURLConnection connection = getHttpConnection("submitTransaction", "", sessionId);
            connection.setDoOutput(true);
            ByteArrayOutputStream toSend = new ByteArrayOutputStream();
            tx.toStream(toSend);
            connection.setRequestProperty("Content-Length", String.valueOf(toSend.toByteArray().length));
            connection.getOutputStream().write(toSend.toByteArray());
            int status = connection.getResponseCode();
            if (status != 200) {
                throw generateException(connection);
            }
        } catch (IOException e) {
            throw e;
        }
    }

    private HttpURLConnection getHttpConnection(String function, String options, String sessionId) throws IOException {
        StringBuilder sb = new StringBuilder();
        String spec = sb.append('/').append(API_VERSION).append('/').append(function).append(options).toString();
        URL url = new URL(_url, spec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        SslUtils.configureTrustedCertificate(connection, BCCAPI_COM_SSL_THUMBPRINT);
        if (sessionId != null) {
            connection.setRequestProperty("sessionId", sessionId);
        }
        connection.setReadTimeout(60000);
        return connection;
    }

    private APIException generateException(HttpURLConnection connection) throws IOException {
        if (connection.getErrorStream() == null) {
            return new APIException("No server response");
        }
        return new APIException(StreamReader.readFully(connection.getErrorStream()));
    }
}
