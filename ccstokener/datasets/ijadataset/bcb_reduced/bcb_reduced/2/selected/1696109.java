package org.signserver.validationservice.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXCertPathChecker;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.ocsp.BasicOCSPResp;
import org.bouncycastle.ocsp.CertificateID;
import org.bouncycastle.ocsp.OCSPException;
import org.bouncycastle.ocsp.OCSPReq;
import org.bouncycastle.ocsp.OCSPReqGenerator;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.ocsp.OCSPRespStatus;
import org.bouncycastle.ocsp.SingleResp;
import org.ejbca.util.CertTools;
import org.signserver.common.CryptoTokenOfflineException;
import org.signserver.common.IllegalRequestException;
import org.signserver.common.SignServerException;
import org.signserver.validationservice.common.Validation;
import org.signserver.validationservice.common.X509Certificate;

/**
 * Stateful OCSP PKIX certificate path checker.
 * It does not support forward checking (reverse is must by default) because we want certificates to be presented from
 * trust anchor (not included) to the target certificate
 * 
 * NOTE : support for forward checking could be enabled by searching issuer certificate of certificate in question and making it stateless.
 * 
 * @author rayback2
 * @version $Id: OCSPPathChecker.java 1825 2011-08-10 10:21:18Z netmackan $
 */
public class OCSPPathChecker extends PKIXCertPathChecker {

    X509Certificate cACert;

    X509Certificate rootCACert;

    Properties props;

    List<X509Certificate> authorizedOCSPResponderCerts;

    protected transient Logger log = Logger.getLogger(this.getClass());

    public OCSPPathChecker(X509Certificate rootCACert, Properties props, List<X509Certificate> authorizedOCSPResponderCerts) {
        this.rootCACert = rootCACert;
        this.props = props;
        this.authorizedOCSPResponderCerts = authorizedOCSPResponderCerts;
    }

    public void init(boolean forward) throws CertPathValidatorException {
        cACert = null;
        if (rootCACert == null) {
            throw new CertPathValidatorException("Root CA Certificate passed in constructor can not be null");
        }
    }

    public void check(Certificate cert, Collection<String> unresolvedCritExts) throws CertPathValidatorException {
        if (!(cert instanceof X509Certificate)) {
            throw new CertPathValidatorException("Certificate passed to check method of OCSPPathChecker is not of type X509Certificate");
        }
        if (cACert == null) {
            cACert = rootCACert;
        }
        X509Certificate x509Cert = (X509Certificate) cert;
        log.debug("check method called with certificate " + x509Cert.getSubject());
        try {
            String oCSPURLString = CertTools.getAuthorityInformationAccessOcspUrl(x509Cert);
            if (oCSPURLString == null || oCSPURLString.length() == 0) {
                throw new SignServerException("OCSP service locator url missing for certificate " + x509Cert.getSubject());
            }
            if (cACert == null) {
                throw new SignServerException("Issuer of certificate : " + x509Cert.getSubject() + " not passed to OCSPPathChecker");
            }
            OCSPReq req = generateOCSPRequest(cACert, x509Cert);
            byte[] derocspresponse = sendOCSPRequest(req, oCSPURLString);
            parseAndVerifyOCSPResponse(x509Cert, derocspresponse);
        } catch (Exception e) {
            log.error("Exception occured on validion of certificate using OCSPPathChecker ", e);
            throw new CertPathValidatorException(e);
        }
        cACert = x509Cert;
    }

    public Set<String> getSupportedExtensions() {
        return null;
    }

    public boolean isForwardCheckingSupported() {
        return false;
    }

    /**
     * Generates basic ocsp request
     * @param issuerCert certificate of the issuer of the certificate to be queried for status
     * @param cert certificate to be queried for status
     * @return basic ocsp request for single certificate
     * @throws OCSPException
     */
    protected OCSPReq generateOCSPRequest(X509Certificate issuerCert, X509Certificate cert) throws OCSPException {
        CertificateID idToCheck = new CertificateID(CertificateID.HASH_SHA1, issuerCert, cert.getSerialNumber());
        OCSPReqGenerator reqgen = new OCSPReqGenerator();
        reqgen.addRequest(idToCheck);
        return reqgen.generate();
    }

    /**
     * Sends passed in ocsp request to ocsp responder at url identified by oCSPURLString
     * 
     * @return der encoded ocsp response
     */
    protected byte[] sendOCSPRequest(OCSPReq ocspRequest, String oCSPURLString) throws IOException, SignServerException {
        byte[] reqarray = ocspRequest.getEncoded();
        URL url = new URL(oCSPURLString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setAllowUserInteraction(false);
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setInstanceFollowRedirects(false);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Length", Integer.toString(reqarray.length));
        con.setRequestProperty("Content-Type", "application/ocsp-request");
        con.connect();
        OutputStream os = con.getOutputStream();
        os.write(reqarray);
        os.close();
        if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new SignServerException("Response code unexpected. Expecting : HTTP_OK(200). Received :  " + con.getResponseCode());
        }
        if ((con.getContentType() == null) || !con.getContentType().equals("application/ocsp-response")) {
            throw new SignServerException("Response type unexpected. Expecting : application/ocsp-response, Received : " + con.getContentType());
        }
        byte[] responsearr = null;
        InputStream reader = con.getInputStream();
        int responselen = con.getContentLength();
        if (responselen != -1) {
            responsearr = new byte[responselen];
            int offset = 0;
            int bread;
            while ((responselen > 0) && (bread = reader.read(responsearr, offset, responselen)) != -1) {
                offset += bread;
                responselen -= bread;
            }
            if (responselen > 0) {
                throw new SignServerException("Unexpected EOF encountered while reading ocsp response from : " + oCSPURLString);
            }
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int b;
            while ((b = reader.read()) != -1) {
                baos.write(b);
            }
            responsearr = baos.toByteArray();
        }
        reader.close();
        con.disconnect();
        return responsearr;
    }

    /**
     * Parses received response bytes to form basic ocsp response object and verifies ocsp response  
     * If returns , ocsp response is successfully verified, otherwise throws exception detailing problem
     * 
     * @param x509Cert - certificate originally passed to validator for validation
     * @param derocspresponse - der formatted ocsp response received from ocsp responder
     * @throws OCSPException 
     * @throws NoSuchProviderException 
     * @throws IOException 
     * @throws CertStoreException 
     * @throws NoSuchAlgorithmException 
     * @throws NoSuchAlgorithmException 
     * @throws SignServerException 
     * @throws CertificateParsingException 
     * @throws CryptoTokenOfflineException 
     * @throws IllegalRequestException 
     */
    protected void parseAndVerifyOCSPResponse(X509Certificate x509Cert, byte[] derocspresponse) throws NoSuchProviderException, OCSPException, NoSuchAlgorithmException, CertStoreException, IOException, SignServerException, CertificateParsingException, IllegalRequestException, CryptoTokenOfflineException {
        OCSPResp ocspresp = new OCSPResp(derocspresponse);
        if (ocspresp.getStatus() != OCSPRespStatus.SUCCESSFUL) {
            throw new SignServerException("Unexpected ocsp response status. Response Status Received : " + ocspresp.getStatus());
        }
        BasicOCSPResp basicOCSPResponse = (BasicOCSPResp) ocspresp.getResponseObject();
        if (basicOCSPResponse == null) {
            throw new SignServerException("Could not construct BasicOCSPResp object from response. Only BasicOCSPResponse as defined in RFC 2560 is supported.");
        }
        X509Certificate ocspRespSignerCertificate = null;
        if (basicOCSPResponse.verify(cACert.getPublicKey(), "BC")) {
            ocspRespSignerCertificate = cACert;
        }
        if (ocspRespSignerCertificate == null) {
            log.debug("OCSP Response is not signed by issuing CA. Looking for authorized responders");
            if (basicOCSPResponse.getCerts("BC") == null) {
                log.debug("OCSP Response does not contain certificate chain, trying to verify response using one of configured authorized ocsp responders");
                ocspRespSignerCertificate = getAuthorizedOCSPRespondersCertificateFromProperties(basicOCSPResponse);
                if (ocspRespSignerCertificate == null) {
                    throw new SignServerException("OCSP Response does not contain certificate chain, and response is not signed by any of the configured Authorized OCSP Responders or CA issuing certificate.");
                }
            } else {
                ocspRespSignerCertificate = getAuthorizedOCSPRespondersCertificateFromOCSPResponse(basicOCSPResponse);
                if (ocspRespSignerCertificate == null) {
                    throw new SignServerException("Certificate signing the ocsp response is not found in ocsp response's certificate chain received and is not signed by CA issuing certificate");
                }
            }
        }
        log.debug("OCSP response signed by :  " + ocspRespSignerCertificate.getSubject());
        if (ocspRespSignerCertificate.getExtensionValue(OCSPObjectIdentifiers.id_pkix_ocsp_nocheck.getId()) != null) {
            try {
                ocspRespSignerCertificate.checkValidity();
            } catch (CertificateExpiredException e) {
                throw new SignServerException("Certificate signing the ocsp response has expired. OCSP Responder Certificate Subject DN : " + ocspRespSignerCertificate.getSubject());
            } catch (CertificateNotYetValidException e) {
                throw new SignServerException("Certificate signing the ocsp response is not yet valid. OCSP Responder Certificate Subject DN : " + ocspRespSignerCertificate.getSubject());
            }
        } else {
            if (CertTools.getCrlDistributionPoint(ocspRespSignerCertificate) == null) {
                throw new SignServerException("CRL Distribution Point extension missing in ocsp signer's certificate.");
            }
            CRLValidator crlValidator = new CRLValidator();
            Validation valresult = crlValidator.validate(ocspRespSignerCertificate, this.props);
            if (valresult.getStatus() != Validation.Status.VALID) {
                throw new SignServerException("Validation of ocsp signer's certificate failed. Status message received : " + valresult.getStatusMessage());
            }
        }
        for (SingleResp singleResponse : basicOCSPResponse.getResponses()) {
            if (singleResponse.getCertID().getSerialNumber().equals(x509Cert.getSerialNumber())) {
                if (singleResponse.getCertStatus() != null) {
                    throw new OCSPStatusNotGoodException("Responce for queried certificate is not good. Certificate status returned : " + singleResponse.getCertStatus(), singleResponse.getCertStatus());
                }
                if (singleResponse.getNextUpdate() != null && (new Date()).compareTo(singleResponse.getNextUpdate()) >= 0) {
                    throw new SignServerException("Unreliable response received. Response reported a nextupdate as : " + singleResponse.getNextUpdate().toString() + " which is earlier than current date.");
                }
                if (singleResponse.getThisUpdate() != null && (new Date()).compareTo(singleResponse.getThisUpdate()) <= 0) {
                    throw new SignServerException("Unreliable response received. Response reported a thisupdate as : " + singleResponse.getThisUpdate().toString() + " which is earlier than current date.");
                }
                break;
            }
        }
    }

    /**
     * 
     * Method that retrieves the Authorized OCSP Responders certificate from basic ocsp response structure
     * the Authorized OCSP responders certificate is identified by OCSPSigner extension
     * Only certificate having this extension and that can verify response's signature is returned 
     * 
     * NOTE : RFC 2560 does not state it should be an end entity certificate ! 
     * 
     * @param basic ocsp response
     * @return Authorized OCSP Responders certificate if found, null if not found
     * @throws OCSPException 
     * @throws NoSuchProviderException 
     * @throws NoSuchAlgorithmException 
     * @throws CertStoreException 
     */
    protected X509Certificate getAuthorizedOCSPRespondersCertificateFromOCSPResponse(BasicOCSPResp basicOCSPResponse) throws NoSuchAlgorithmException, NoSuchProviderException, OCSPException, CertStoreException {
        X509Certificate retCert = null;
        X509Certificate tempCert = null;
        CertStore ocspRespCertStore = basicOCSPResponse.getCertificates("Collection", "BC");
        X509ExtendedKeyUsageExistsCertSelector certSel = new X509ExtendedKeyUsageExistsCertSelector("1.3.6.1.5.5.7.3.9");
        Iterator<?> certsIter = ocspRespCertStore.getCertificates(certSel).iterator();
        while (certsIter.hasNext()) {
            try {
                tempCert = X509Certificate.getInstance((java.security.cert.X509Certificate) certsIter.next());
            } catch (Exception e) {
                continue;
            }
            if (tempCert != null && basicOCSPResponse.verify(tempCert.getPublicKey(), "BC")) {
                retCert = tempCert;
                break;
            }
        }
        return retCert;
    }

    /**
     * Method that traverses all configured AuthorizedOCSPResponderCert properties for the issuer of certficate passed originally to the validators validate() method 
     * and tries to find the one that signed the ocsp response
     * @param basicOCSPResponse - response that is tried to be verified
     * @return - Authorized ocsp responder's certificate, or null if none found that verifies ocsp response received
     * @throws NoSuchProviderException
     * @throws OCSPException
     */
    protected X509Certificate getAuthorizedOCSPRespondersCertificateFromProperties(BasicOCSPResp basicOCSPResponse) throws NoSuchProviderException, OCSPException {
        log.debug("Searching for Authorized OCSP Responder certificate from PROPERTIES");
        if (this.authorizedOCSPResponderCerts == null || this.authorizedOCSPResponderCerts.isEmpty()) {
            return null;
        }
        for (X509Certificate ocspCert : this.authorizedOCSPResponderCerts) {
            if (basicOCSPResponse.verify(ocspCert.getPublicKey(), "BC")) {
                log.debug("Found Authorized OCSP Responder's certificate, signing ocsp response. found cert : " + ocspCert.getSubject());
                return ocspCert;
            }
        }
        log.debug("Authorized OCSP Responder is not found");
        return null;
    }

    /**
     * 
     * Since we are implementing stateful checker we ought to override clone method for proper functionality
     * clone is used by certpath builder to backtrack and try another path when potential certificate path reaches dead end.
     * 
     * @throws SignServerException 
     */
    public Object clone() {
        try {
            OCSPPathChecker clonedOCSPPathChecker = null;
            X509Certificate clonedPrevCert = null;
            if (cACert != null) {
                CertificateFactory certFact = CertificateFactory.getInstance("X509", "BC");
                ByteArrayInputStream bis = new ByteArrayInputStream(cACert.getEncoded());
                clonedPrevCert = (X509Certificate) certFact.generateCertificate(bis);
            }
            clonedOCSPPathChecker = new OCSPPathChecker(rootCACert, this.props, this.authorizedOCSPResponderCerts);
            clonedOCSPPathChecker.cACert = clonedPrevCert;
            return clonedOCSPPathChecker;
        } catch (CertificateException e) {
            log.error("Exception occured on clone of OCSPPathChecker", e);
        } catch (NoSuchProviderException e) {
            log.error("Exception occured on clone of OCSPPathChecker", e);
        }
        return null;
    }
}
