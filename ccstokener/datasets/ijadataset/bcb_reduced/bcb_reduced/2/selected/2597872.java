package com.itextpdf.text.pdf;

import java.io.*;
import java.math.*;
import java.net.*;
import com.itextpdf.text.error_messages.MessageLocalization;
import org.bouncycastle.asn1.cmp.*;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.tsp.*;
import com.itextpdf.text.pdf.codec.Base64;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;

/**
 * Time Stamp Authority Client interface implementation using Bouncy Castle
 * org.bouncycastle.tsp package.
 * <p>
 * Created by Aiken Sam, 2006-11-15, refactored by Martin Brunecky, 07/15/2007
 * for ease of subclassing.
 * </p>
 * @since	2.1.6
 */
public class TSAClientBouncyCastle implements TSAClient {

    /** URL of the Time Stamp Authority */
    protected String tsaURL;

    /** TSA Username */
    protected String tsaUsername;

    /** TSA password */
    protected String tsaPassword;

    /** Estimate of the received time stamp token */
    protected int tokSzEstimate;

    protected String digestAlgorithm;

    private static final String defaultDigestAlgorithm = "SHA-1";

    /**
     * Creates an instance of a TSAClient that will use BouncyCastle.
     * @param url String - Time Stamp Authority URL (i.e. "http://tsatest1.digistamp.com/TSA")
     */
    public TSAClientBouncyCastle(String url) {
        this(url, null, null, 4096, defaultDigestAlgorithm);
    }

    /**
     * Creates an instance of a TSAClient that will use BouncyCastle.
     * @param url String - Time Stamp Authority URL (i.e. "http://tsatest1.digistamp.com/TSA")
     * @param username String - user(account) name
     * @param password String - password
     */
    public TSAClientBouncyCastle(String url, String username, String password) {
        this(url, username, password, 4096, defaultDigestAlgorithm);
    }

    /**
     * Constructor.
     * Note the token size estimate is updated by each call, as the token
     * size is not likely to change (as long as we call the same TSA using
     * the same imprint length).
     * @param url String - Time Stamp Authority URL (i.e. "http://tsatest1.digistamp.com/TSA")
     * @param username String - user(account) name
     * @param password String - password
     * @param tokSzEstimate int - estimated size of received time stamp token (DER encoded)
     */
    public TSAClientBouncyCastle(String url, String username, String password, int tokSzEstimate, String digestAlgorithm) {
        this.tsaURL = url;
        this.tsaUsername = username;
        this.tsaPassword = password;
        this.tokSzEstimate = tokSzEstimate;
        this.digestAlgorithm = digestAlgorithm;
    }

    /**
     * Get the token size estimate.
     * Returned value reflects the result of the last succesfull call, padded
     * @return an estimate of the token size
     */
    public int getTokenSizeEstimate() {
        return tokSzEstimate;
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    /**
     * Get RFC 3161 timeStampToken.
     * Method may return null indicating that timestamp should be skipped.
     * @param imprint byte[] - data imprint to be time-stamped
     * @return byte[] - encoded, TSA signed data of the timeStampToken
     * @throws Exception - TSA request failed
     */
    public byte[] getTimeStampToken(byte[] imprint) throws Exception {
        byte[] respBytes = null;
        try {
            TimeStampRequestGenerator tsqGenerator = new TimeStampRequestGenerator();
            tsqGenerator.setCertReq(true);
            BigInteger nonce = BigInteger.valueOf(System.currentTimeMillis());
            TimeStampRequest request = tsqGenerator.generate(new ASN1ObjectIdentifier(PdfPKCS7.getAllowedDigests(getDigestAlgorithm())), imprint, nonce);
            byte[] requestBytes = request.getEncoded();
            respBytes = getTSAResponse(requestBytes);
            TimeStampResponse response = new TimeStampResponse(respBytes);
            response.validate(request);
            PKIFailureInfo failure = response.getFailInfo();
            int value = (failure == null) ? 0 : failure.intValue();
            if (value != 0) {
                throw new Exception(MessageLocalization.getComposedMessage("invalid.tsa.1.response.code.2", tsaURL, String.valueOf(value)));
            }
            TimeStampToken tsToken = response.getTimeStampToken();
            if (tsToken == null) {
                throw new Exception(MessageLocalization.getComposedMessage("tsa.1.failed.to.return.time.stamp.token.2", tsaURL, response.getStatusString()));
            }
            TimeStampTokenInfo info = tsToken.getTimeStampInfo();
            byte[] encoded = tsToken.getEncoded();
            long stop = System.currentTimeMillis();
            this.tokSzEstimate = encoded.length + 32;
            return encoded;
        } catch (Exception e) {
            throw e;
        } catch (Throwable t) {
            throw new Exception(MessageLocalization.getComposedMessage("failed.to.get.tsa.response.from.1", tsaURL), t);
        }
    }

    /**
     * Get timestamp token - communications layer
     * @return - byte[] - TSA response, raw bytes (RFC 3161 encoded)
     */
    protected byte[] getTSAResponse(byte[] requestBytes) throws Exception {
        URL url = new URL(tsaURL);
        URLConnection tsaConnection;
        tsaConnection = (URLConnection) url.openConnection();
        tsaConnection.setDoInput(true);
        tsaConnection.setDoOutput(true);
        tsaConnection.setUseCaches(false);
        tsaConnection.setRequestProperty("Content-Type", "application/timestamp-query");
        tsaConnection.setRequestProperty("Content-Transfer-Encoding", "binary");
        if ((tsaUsername != null) && !tsaUsername.equals("")) {
            String userPassword = tsaUsername + ":" + tsaPassword;
            tsaConnection.setRequestProperty("Authorization", "Basic " + Base64.encodeBytes(userPassword.getBytes()));
        }
        OutputStream out = tsaConnection.getOutputStream();
        out.write(requestBytes);
        out.close();
        InputStream inp = tsaConnection.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead = 0;
        while ((bytesRead = inp.read(buffer, 0, buffer.length)) >= 0) {
            baos.write(buffer, 0, bytesRead);
        }
        byte[] respBytes = baos.toByteArray();
        String encoding = tsaConnection.getContentEncoding();
        if (encoding != null && encoding.equalsIgnoreCase("base64")) {
            respBytes = Base64.decode(new String(respBytes));
        }
        return respBytes;
    }
}
