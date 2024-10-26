package com.pkcs11.support.validation;

import com.pkcs11.support.OCSPException;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Vector;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.ocsp.BasicOCSPResp;
import org.bouncycastle.ocsp.CertificateID;
import org.bouncycastle.ocsp.CertificateStatus;
import org.bouncycastle.ocsp.OCSPReq;
import org.bouncycastle.ocsp.OCSPReqGenerator;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.ocsp.RevokedStatus;
import org.bouncycastle.ocsp.SingleResp;
import org.bouncycastle.ocsp.UnknownStatus;

/**
 *
 * @author iozen
 */
public class OCSPRequest {

    private OCSPReq generateOCSPRequest(final X509Certificate issuerCert, final BigInteger serialNumber) throws OCSPException, org.bouncycastle.ocsp.OCSPException {
        final CertificateID id = new CertificateID(CertificateID.HASH_SHA1, issuerCert, serialNumber);
        final OCSPReqGenerator gen = new OCSPReqGenerator();
        gen.addRequest(id);
        final BigInteger nonce = BigInteger.valueOf(System.currentTimeMillis());
        final Vector<DERObjectIdentifier> oids = new Vector<DERObjectIdentifier>();
        final Vector<X509Extension> values = new Vector<X509Extension>();
        oids.add(OCSPObjectIdentifiers.id_pkix_ocsp_nonce);
        values.add(new X509Extension(false, new DEROctetString(nonce.toByteArray())));
        gen.setRequestExtensions(new X509Extensions(oids, values));
        return gen.generate();
    }

    public int makeOCSPRequest(final X509Certificate issuerCert, final X509Certificate userCert, final String serviceAddr) throws OCSPException, org.bouncycastle.ocsp.OCSPException {
        OCSPReq request;
        try {
            request = generateOCSPRequest(issuerCert, userCert.getSerialNumber());
        } catch (final OCSPException e) {
            e.printStackTrace();
            throw new OCSPException("UNABLE TO CREATE REQUEST");
        }
        byte[] array;
        try {
            array = request.getEncoded();
        } catch (final IOException e) {
            e.printStackTrace();
            throw new OCSPException("UNABLE TO PREPARE REQUEST");
        }
        if (serviceAddr == null) {
            throw new OCSPException("NO OCSP ADDRESS");
        }
        if (!serviceAddr.startsWith("http")) {
            throw new OCSPException("UNSUPPORTED PROTOCOL");
        }
        HttpURLConnection con = null;
        URL url;
        try {
            url = new URL(serviceAddr);
        } catch (final MalformedURLException e) {
            e.printStackTrace();
            throw new OCSPException("MALFORMED OCSP ADDRESS");
        }
        try {
            con = (HttpURLConnection) url.openConnection();
        } catch (final IOException e) {
            e.printStackTrace();
            throw new OCSPException("UNABLE TO CONNECT OCSP ADDRESS");
        }
        con.setRequestProperty("Content-Type", "application/ocsp-request");
        con.setRequestProperty("Accept", "application/ocsp-response");
        con.setDoOutput(true);
        OCSPResp ocspResponse;
        try {
            final OutputStream out = con.getOutputStream();
            final DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(out));
            dataOut.write(array);
            dataOut.flush();
            dataOut.close();
            if (con.getResponseCode() / 100 != 2) {
                throw new OCSPException("HTTP STATUS ERROR");
            }
            final InputStream in = (InputStream) con.getContent();
            ocspResponse = new OCSPResp(in);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new OCSPException("UNABLE TO CONNECT OCSP ADDRESS");
        }
        final int status = ocspResponse.getStatus();
        BasicOCSPResp basicResponse;
        try {
            basicResponse = (BasicOCSPResp) ocspResponse.getResponseObject();
        } catch (final org.bouncycastle.ocsp.OCSPException e) {
            e.printStackTrace();
            throw new OCSPException("UNABLE TO PARSE RESPONSE");
        }
        if (basicResponse == null) {
            throw new OCSPException("NO RESPONSE FROM SERVER");
        }
        final SingleResp[] responses = basicResponse.getResponses();
        if (responses.length == 0) {
            throw new OCSPException("NO RESPONSE FROM SERVER");
        }
        if (responses.length != 1) {
            throw new OCSPException("TOO MANY RESPONSES");
        }
        final SingleResp resp = responses[0];
        final Object status2 = resp.getCertStatus();
        if (status2 == CertificateStatus.GOOD) {
            return 0;
        }
        if (status2 instanceof RevokedStatus) {
            return 1;
        }
        if (status2 instanceof UnknownStatus) {
            throw new OCSPException("STATUS UNKNOWN");
        }
        if (status == 0) {
            return 0;
        }
        if (status == 1) {
            return 1;
        }
        if (status == 2) {
            return 2;
        }
        return 2;
    }
}
