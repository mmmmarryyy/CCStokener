package net.esle.sinadura.core.firma.timestamp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import net.esle.sinadura.core.firma.exceptions.SinaduraCoreException;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;

/**
 * Implementación del TimeStampAuthorityClient.
 * 
 * @author zylk.net
 */
public class TSAClientBouncyCastle implements TSAClient {

    protected String tsaURL;

    protected String tsaUsername;

    protected String tsaPassword;

    protected int tokSzEstimate;

    protected Proxy proxy;

    /**
	 * @param url
	 */
    public TSAClientBouncyCastle(String url) {
        this(url, null, null, 4096);
    }

    /**
	 * @param url
	 * @param username
	 * @param password
	 */
    public TSAClientBouncyCastle(String url, String username, String password) {
        this(url, username, password, 4096);
    }

    /**
	 * @param url
	 * @param username
	 * @param password
	 * @param tokSzEstimate
	 */
    public TSAClientBouncyCastle(String url, String username, String password, int tokSzEstimate) {
        this.tsaURL = url;
        this.tsaUsername = username;
        this.tsaPassword = password;
        this.tokSzEstimate = tokSzEstimate;
    }

    public void setProxy(Proxy _proxy) {
        this.proxy = _proxy;
    }

    /**
	 * @return int
	 */
    public int getTokenSizeEstimate() {
        return this.tokSzEstimate;
    }

    public byte[] getTimeStampToken(TsaPdfPKCS7 caller, byte[] imprint) throws SinaduraCoreException {
        return getTimeStampToken(imprint);
    }

    /**
	 * @param imprint
	 * @return
	 * @throws SinaduraCoreException
	 * @throws Exception
	 */
    protected byte[] getTimeStampToken(byte[] imprint) throws SinaduraCoreException {
        byte[] encoded = null;
        try {
            byte[] respBytes = null;
            TimeStampRequestGenerator tsqGenerator = new TimeStampRequestGenerator();
            tsqGenerator.setCertReq(true);
            BigInteger nonce = BigInteger.valueOf(System.currentTimeMillis());
            TimeStampRequest request = tsqGenerator.generate(X509ObjectIdentifiers.id_SHA1.getId(), imprint, nonce);
            byte[] requestBytes = request.getEncoded();
            respBytes = getTSAResponse(requestBytes);
            TimeStampResponse response = new TimeStampResponse(respBytes);
            response.validate(request);
            PKIFailureInfo failure = response.getFailInfo();
            int value = (failure == null) ? 0 : failure.intValue();
            if (value != 0) {
                throw new Exception("Invalid TSA '" + this.tsaURL + "' response, code " + value);
            }
            TimeStampToken tsToken = response.getTimeStampToken();
            if (tsToken == null) {
                throw new Exception("TSA '" + this.tsaURL + "' failed to return time stamp token");
            }
            TimeStampTokenInfo info = tsToken.getTimeStampInfo();
            encoded = tsToken.getEncoded();
            long stop = System.currentTimeMillis();
            this.tokSzEstimate = encoded.length + 32;
        } catch (IOException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (TSPException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (Exception e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        }
        return encoded;
    }

    /**
	 * @return
	 * @throws SinaduraCoreException
	 */
    protected byte[] getTSAResponse(byte[] requestBytes) throws SinaduraCoreException {
        byte[] respBytes = null;
        try {
            URL url = new URL(this.tsaURL);
            URLConnection tsaConnection = null;
            if (this.proxy == null) tsaConnection = url.openConnection(); else tsaConnection = url.openConnection(this.proxy);
            tsaConnection.setDoInput(true);
            tsaConnection.setDoOutput(true);
            tsaConnection.setUseCaches(false);
            tsaConnection.setRequestProperty("Content-Type", "application/timestamp-query");
            tsaConnection.setRequestProperty("Content-Transfer-Encoding", "binary");
            if ((this.tsaUsername != null) && !this.tsaUsername.equals("")) {
                String userPassword = this.tsaUsername + ":" + this.tsaPassword;
                tsaConnection.setRequestProperty("Authorization", "Basic " + new String(new sun.misc.BASE64Encoder().encode(userPassword.getBytes())));
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
            respBytes = baos.toByteArray();
            String encoding = tsaConnection.getContentEncoding();
            if (encoding != null && encoding.equalsIgnoreCase("base64")) {
                sun.misc.BASE64Decoder dec = new sun.misc.BASE64Decoder();
                respBytes = dec.decodeBuffer(new String(respBytes));
            }
        } catch (MalformedURLException e) {
            throw new SinaduraCoreException("URL malformed " + e.getMessage(), e);
        } catch (IOException e) {
            throw new SinaduraCoreException("Connection Error " + e.getMessage(), e);
        }
        return respBytes;
    }
}
