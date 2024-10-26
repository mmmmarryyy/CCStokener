package com.itextpdf.text.pdf;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Vector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.ocsp.*;
import com.itextpdf.text.error_messages.MessageLocalization;
import com.itextpdf.text.log.Level;
import com.itextpdf.text.log.Logger;
import com.itextpdf.text.log.LoggerFactory;
import java.security.cert.CertificateEncodingException;
import org.bouncycastle.asn1.ocsp.BasicOCSPResponse;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.operator.OperatorException;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

/**
 * OcspClient implementation using BouncyCastle.
 * @author psoares
 * @since	2.1.6
 */
public class OcspClientBouncyCastle implements OcspClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcspClientBouncyCastle.class);

    /**
     * Generates an OCSP request using BouncyCastle.
     * @param issuerCert	certificate of the issues
     * @param serialNumber	serial number
     * @return	an OCSP request
     * @throws OCSPException
     * @throws IOException
     */
    private static OCSPReq generateOCSPRequest(X509Certificate issuerCert, BigInteger serialNumber) throws OCSPException, IOException, OperatorException, CertificateEncodingException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        CertificateID id = new CertificateID(new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1), new JcaX509CertificateHolder(issuerCert), serialNumber);
        OCSPReqBuilder gen = new OCSPReqBuilder();
        gen.addRequest(id);
        Extension ext = new Extension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce, false, new DEROctetString(new DEROctetString(PdfEncryption.createDocumentId()).getEncoded()));
        gen.setRequestExtensions(new Extensions(new Extension[] { ext }));
        return gen.build();
    }

    /**
	 * Gets an encoded byte array with OCSP validation. The method should not throw an exception.
     * @param checkCert to certificate to check
     * @param rootCert the parent certificate
     * @param the url to get the verification. It it's null it will be taken
     * from the check cert or from other implementation specific source
	 * @return	a byte array with the validation or null if the validation could not be obtained
	 */
    public byte[] getEncoded(X509Certificate checkCert, X509Certificate rootCert, String url) {
        try {
            if (checkCert == null || rootCert == null) return null;
            if (url == null) {
                url = PdfPKCS7.getOCSPURL(checkCert);
            }
            if (url == null) return null;
            OCSPReq request = generateOCSPRequest(rootCert, checkCert.getSerialNumber());
            byte[] array = request.getEncoded();
            URL urlt = new URL(url);
            HttpURLConnection con = (HttpURLConnection) urlt.openConnection();
            con.setRequestProperty("Content-Type", "application/ocsp-request");
            con.setRequestProperty("Accept", "application/ocsp-response");
            con.setDoOutput(true);
            OutputStream out = con.getOutputStream();
            DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(out));
            dataOut.write(array);
            dataOut.flush();
            dataOut.close();
            if (con.getResponseCode() / 100 != 2) {
                throw new IOException(MessageLocalization.getComposedMessage("invalid.http.response.1", con.getResponseCode()));
            }
            InputStream in = (InputStream) con.getContent();
            OCSPResp ocspResponse = new OCSPResp(RandomAccessFileOrArray.InputStreamToArray(in));
            if (ocspResponse.getStatus() != 0) throw new IOException(MessageLocalization.getComposedMessage("invalid.status.1", ocspResponse.getStatus()));
            BasicOCSPResp basicResponse = (BasicOCSPResp) ocspResponse.getResponseObject();
            if (basicResponse != null) {
                SingleResp[] responses = basicResponse.getResponses();
                if (responses.length == 1) {
                    SingleResp resp = responses[0];
                    Object status = resp.getCertStatus();
                    if (status == CertificateStatus.GOOD) {
                        return basicResponse.getEncoded();
                    } else if (status instanceof org.bouncycastle.ocsp.RevokedStatus) {
                        throw new IOException(MessageLocalization.getComposedMessage("ocsp.status.is.revoked"));
                    } else {
                        throw new IOException(MessageLocalization.getComposedMessage("ocsp.status.is.unknown"));
                    }
                }
            }
        } catch (Exception ex) {
            if (LOGGER.isLogging(Level.ERROR)) LOGGER.error("OcspClientBouncyCastle", ex);
        }
        return null;
    }
}
