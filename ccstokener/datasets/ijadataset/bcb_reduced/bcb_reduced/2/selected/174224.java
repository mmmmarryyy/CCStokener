package org.jsslutils.sslcontext;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * This class is a wrapper for an X509CRL object that allows it to be re-loaded.
 * The Callable obtained with getReloaderCallable() can be put into a
 * ScheduledThreadPoolExecutor, for example. Note that the constructor does not
 * initially download the CRL, so classes uses this should call the callable at
 * least once priori to use.
 * 
 * @author Bruno Harbulot (Bruno.Harbulot@manchester.ac.uk)
 * 
 */
public class ReloadableX509CRL extends X509CRL {

    private final CertificateFactory certificateFactory;

    private final String crlUrl;

    private final Callable<X509CRL> reloaderCallable;

    private volatile X509CRL crl;

    public ReloadableX509CRL(String crlUrl) {
        this(crlUrl, null);
    }

    public ReloadableX509CRL(String crlUrl, CertificateFactory certificateFactory) {
        this.crlUrl = crlUrl;
        if (certificateFactory == null) {
            try {
                this.certificateFactory = CertificateFactory.getInstance("X.509");
            } catch (CertificateException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.certificateFactory = certificateFactory;
        }
        this.reloaderCallable = new Callable<X509CRL>() {

            public X509CRL call() throws Exception {
                InputStream is = null;
                X509CRL crl = null;
                try {
                    URL url = new URL(ReloadableX509CRL.this.crlUrl);
                    is = url.openStream();
                    crl = (X509CRL) ReloadableX509CRL.this.certificateFactory.generateCRL(is);
                    ReloadableX509CRL.this.crl = crl;
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
                return crl;
            }
        };
    }

    public Callable<X509CRL> getReloaderCallable() {
        return this.reloaderCallable;
    }

    @Override
    public byte[] getEncoded() throws CRLException {
        X509CRL crl = this.crl;
        if (crl != null) {
            return crl.getEncoded();
        } else {
            return new byte[0];
        }
    }

    @Override
    public Principal getIssuerDN() {
        X509CRL crl = this.crl;
        if (crl != null) {
            return crl.getIssuerDN();
        } else {
            return null;
        }
    }

    @Override
    public Date getNextUpdate() {
        X509CRL crl = this.crl;
        if (crl != null) {
            return crl.getNextUpdate();
        } else {
            return null;
        }
    }

    @Override
    public X509CRLEntry getRevokedCertificate(BigInteger serialNumber) {
        X509CRL crl = this.crl;
        if (crl != null) {
            return crl.getRevokedCertificate(serialNumber);
        } else {
            return null;
        }
    }

    @Override
    public Set<? extends X509CRLEntry> getRevokedCertificates() {
        X509CRL crl = this.crl;
        if (crl != null) {
            return crl.getRevokedCertificates();
        } else {
            return null;
        }
    }

    @Override
    public String getSigAlgName() {
        X509CRL crl = this.crl;
        if (crl != null) {
            return crl.getSigAlgName();
        } else {
            return null;
        }
    }

    @Override
    public String getSigAlgOID() {
        X509CRL crl = this.crl;
        if (crl != null) {
            return crl.getSigAlgOID();
        } else {
            return null;
        }
    }

    @Override
    public byte[] getSigAlgParams() {
        X509CRL crl = this.crl;
        if (crl != null) {
            return crl.getSigAlgParams();
        } else {
            return null;
        }
    }

    @Override
    public byte[] getSignature() {
        X509CRL crl = this.crl;
        if (crl != null) {
            return crl.getSignature();
        } else {
            return null;
        }
    }

    @Override
    public byte[] getTBSCertList() throws CRLException {
        X509CRL crl = this.crl;
        if (crl != null) {
            return crl.getTBSCertList();
        } else {
            return null;
        }
    }

    @Override
    public Date getThisUpdate() {
        X509CRL crl = this.crl;
        if (crl != null) {
            return crl.getThisUpdate();
        } else {
            return null;
        }
    }

    @Override
    public int getVersion() {
        X509CRL crl = this.crl;
        if (crl != null) {
            return crl.getVersion();
        } else {
            return -1;
        }
    }

    @Override
    public void verify(PublicKey key, String sigProvider) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        X509CRL crl = this.crl;
        if (crl != null) {
            crl.verify(key, sigProvider);
        } else {
            throw new CRLException("No CRL loaded, nothing to verify.");
        }
    }

    @Override
    public void verify(PublicKey key) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        X509CRL crl = this.crl;
        if (crl != null) {
            crl.verify(key);
        } else {
            throw new CRLException("No CRL loaded, nothing to verify.");
        }
    }

    public Set<String> getCriticalExtensionOIDs() {
        X509CRL crl = this.crl;
        if (crl != null) {
            return crl.getCriticalExtensionOIDs();
        } else {
            return null;
        }
    }

    public byte[] getExtensionValue(String oid) {
        X509CRL crl = this.crl;
        if (crl != null) {
            return crl.getExtensionValue(oid);
        } else {
            return null;
        }
    }

    public Set<String> getNonCriticalExtensionOIDs() {
        X509CRL crl = this.crl;
        if (crl != null) {
            return crl.getNonCriticalExtensionOIDs();
        } else {
            return null;
        }
    }

    public boolean hasUnsupportedCriticalExtension() {
        X509CRL crl = this.crl;
        if (crl != null) {
            return crl.hasUnsupportedCriticalExtension();
        } else {
            return false;
        }
    }

    @Override
    public boolean isRevoked(Certificate cert) {
        X509CRL crl = this.crl;
        if (crl != null) {
            return crl.isRevoked(cert);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        X509CRL crl = this.crl;
        if (crl != null) {
            return this.getClass().getName() + ", wrapped CRL: " + crl.toString();
        } else {
            return this.getClass().getName() + ", no wrapped CRL!";
        }
    }
}
