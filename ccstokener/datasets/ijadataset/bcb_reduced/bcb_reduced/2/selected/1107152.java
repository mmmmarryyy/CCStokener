package com.fujitsu.arcon.gateway;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Base64Encoder;
import sun.io.ByteToCharASCII;
import sun.io.ByteToCharConverter;
import sun.io.ByteToCharISO8859_1;
import com.fujitsu.arcon.gateway.logger.Logger;
import com.fujitsu.arcon.gateway.logger.LoggerLevel;
import com.fujitsu.arcon.gateway.logger.LoggerManager;

/**
 * @author Sven van den Berghe, fujitsu
 * 
 * @version $Id: ActiveCRLChecker.java,v 1.7 2006/08/18 13:30:14 tweddell Exp $
 * 
 */
public class ActiveCRLChecker implements CRLChecker, Alarm.Bell {

    private Map crls;

    private Map being_fetched;

    int invalidated;

    private File invalidate_file;

    Logger logger;

    public ActiveCRLChecker() {
        logger = LoggerManager.get(this.getClass().getName());
        crls = Collections.synchronizedMap(new HashMap());
        being_fetched = new HashMap();
        invalidated = 0;
        HostnameVerifier hv = new HostnameVerifier() {

            public boolean verify(String urlHostName, SSLSession session) {
                logger.chat("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
        try {
            SSLContext context = SSLContext.getInstance("SSLv3");
            context.init(null, ListenerManager.getGTM(), new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (Exception ex) {
            logger.severe("Problems initialising SSL for CRL checking", ex);
            TerminationManager.get().abort();
        }
        Alarm.get().set(this, System.currentTimeMillis() + Configuration.getwatchInterval());
        invalidate_file = new File(Configuration.getHomeDir(), "invalidate_crls");
        logger.config("CRL checker watching for <" + invalidate_file + ">");
    }

    /** ****** fetch crlDistributionPoints from user cert //tweddell*************** */
    private Vector getCRLDistPointsFromCert(X509Certificate certificate) {
        Vector retCrlURLs = new Vector();
        Vector crlURLs = null;
        byte[] crlDistributionPoints = certificate.getExtensionValue(X509Extensions.CRLDistributionPoints.getId());
        if (crlDistributionPoints != null) {
            try {
                crlURLs = getOctetValues(crlDistributionPoints);
            } catch (Exception e) {
                logger.chat("Cannot get octet values from crl dist points");
            }
        }
        if (crlURLs == null) {
            return null;
        }
        Iterator crlURLsIterator = crlURLs.iterator();
        while (crlURLsIterator.hasNext()) {
            String crlURLString = new String((byte[]) crlURLsIterator.next());
            retCrlURLs.add(crlURLString);
        }
        return retCrlURLs;
    }

    private DERObject getDERObject(byte[] extensionValue) {
        if (extensionValue == null) return null;
        try {
            ASN1InputStream extIS = new ASN1InputStream(new ByteArrayInputStream(extensionValue));
            byte[] extOctets = ((ASN1OctetString) extIS.readObject()).getOctets();
            extIS = new ASN1InputStream(new ByteArrayInputStream(extOctets));
            return extIS.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    private Vector getOctetValues(byte[] extensionValue) {
        return getOctetValues(getDERObject(extensionValue));
    }

    private Vector getOctetValues(DERObject derObject) {
        if (derObject == null) return null;
        if (derObject instanceof DERSequence) {
            Vector derValues = new Vector();
            Enumeration derObjects = ((DERSequence) derObject).getObjects();
            while (derObjects.hasMoreElements()) {
                DERObject nestedObj = (DERObject) derObjects.nextElement();
                derValues.addAll(getOctetValues(nestedObj));
            }
            return derValues;
        }
        if (derObject instanceof DERTaggedObject) {
            DERTaggedObject derTaggedObject = (DERTaggedObject) derObject;
            if (derTaggedObject.isExplicit() && !derTaggedObject.isEmpty()) {
                DERObject nestedObj = derTaggedObject.getObject();
                Vector derValues = getOctetValues(nestedObj);
                return derValues;
            } else {
                DEROctetString derOctetString = (DEROctetString) derTaggedObject.getObject();
                byte[] octetValue = derOctetString.getOctets();
                Vector derValues = new Vector();
                derValues.add(octetValue);
                return derValues;
            }
        }
        return null;
    }

    /** *************************************************************************** */
    public boolean isRevoked(X509Certificate cert) {
        String cert_name = cert.getSubjectDN().getName();
        String crl_location = null;
        X509CRL crl = null;
        logger.chat("<" + cert_name + "> Extracting CRLDistributionPoints...");
        Vector CRLs = this.getCRLDistPointsFromCert(cert);
        if (CRLs == null) {
            logger.chat("<" + cert_name + "> has no CRLs. Checking for gw.crl_fallback_location...");
            crl_location = Configuration.getFallbackCRLLocation();
            if (crl_location != null) {
                String[] fallbackcrls = crl_location.split(",");
                logger.chat("gw.crl_fallback_location has " + fallbackcrls.length + " CRLs.");
                CRLs = new Vector(fallbackcrls.length);
                for (int i = 0; i < fallbackcrls.length; ++i) {
                    CRLs.add(fallbackcrls[i]);
                }
            } else logger.chat("No fallback locations in gateway.properties!");
        } else {
            logger.chat("<" + cert_name + "> contains " + CRLs.size() + " CRLs.");
        }
        boolean ret = true;
        if (CRLs != null) {
            logger.chat("We have " + CRLs.size() + " CRLs. Lets go...");
            if (logger.CHAT) for (int i = 0; i < CRLs.size(); ++i) {
                logger.chat("  [" + (i + 1) + "]:" + CRLs.elementAt(i));
            }
            crl = null;
            Iterator it = CRLs.iterator();
            int whichCRL = 0;
            while (it.hasNext() && crl == null) {
                ++whichCRL;
                crl_location = (String) it.next();
                crl = getCurrentCRL(crl_location);
            }
            if (crl == null) {
                logger.warning("Cannot get one of the CRLs. Rejecting connection.");
            } else {
                logger.chat("Found CRL at URI [" + whichCRL + "] <" + crl_location + "> Checking...");
                if (crl.isRevoked(cert)) {
                    logger.warning("Certificate <" + cert_name + "> is revoked by CA, rejecting connection.");
                } else {
                    logger.chat("Certificate <" + cert_name + "> is not revoked within CRL: <" + crl_location + "> ");
                    ret = false;
                }
            }
        } else {
            logger.log("CA <" + cert.getIssuerDN().getName() + "> does not supply CRLs." + " I dont have a fallbackCRLLocation. " + " So accepting connection <" + cert_name + ">", LoggerLevel.CHAT);
            ret = false;
        }
        return ret;
    }

    private X509CRL getCurrentCRL(String uri) {
        X509CRL crl = (X509CRL) crls.get(uri);
        if (crl == null) {
            logger.chat("Cannot find a current CRL from <" + uri + "> so fetching one");
            fetchCRL(uri);
            crl = (X509CRL) crls.get(uri);
        }
        return crl;
    }

    /************************************************************************/
    private X509CRL readCRLFromPEM(byte[] crldata, CertificateFactory cf) throws IOException, CRLException {
        logger.chat("Trying to get CRL in PEM format...");
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(crldata)));
        String line = reader.readLine();
        while (!line.startsWith("-----BEGIN X509 CRL")) {
            line = reader.readLine();
        }
        String wot_we_want = "";
        do {
            wot_we_want += line + "\n";
            line = reader.readLine();
        } while (!line.startsWith("-----END X509 CRL"));
        wot_we_want += line + "\n";
        logger.chat("Adding to CRL Map:\n" + wot_we_want);
        return (X509CRL) cf.generateCRL(new ByteArrayInputStream(wot_we_want.getBytes()));
    }

    private X509CRL readCRLFromDER(byte[] crldata, CertificateFactory cf) throws CRLException, IOException {
        logger.chat("Trying to get CRL in DER format...");
        byte[] crlpem = Base64.encode(crldata);
        String lo = new String(crlpem);
        final String crlbeg = "-----BEGIN X509 CRL-----";
        final String crlend = "-----END X509 CRL-----";
        String crlpemstr = crlbeg + "\n";
        for (int i = 0; i < lo.length(); i += 64) crlpemstr += lo.substring(i, (i + 64 > lo.length()) ? lo.length() : i + 64) + "\n";
        crlpemstr += crlend + "\n";
        logger.chat(crlpemstr);
        ;
        return (X509CRL) cf.generateCRL(new ByteArrayInputStream(crlpemstr.getBytes()));
    }

    /************************************************************************/
    public void fetchCRL(String uri) {
        Object fing;
        X509CRL old_crl = null;
        boolean im_fetching = false;
        synchronized (this) {
            fing = being_fetched.get(uri);
            if (fing == null) {
                fing = new Object();
                being_fetched.put(uri, fing);
                old_crl = (X509CRL) crls.put(uri, null);
                im_fetching = true;
            }
        }
        if (im_fetching) {
            logger.chat("Fetching up to date CRL from <" + uri + ">");
            X509CRL new_crl = null;
            boolean ok = false;
            String problem = "none";
            try {
                URL url = new URL(uri);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                logger.chat("<" + url.toString() + "> uses protocol " + url.getProtocol());
                InputStream is = url.openStream();
                DataInputStream dis = new DataInputStream(is);
                BufferedInputStream bis = new BufferedInputStream(dis);
                byte[] readcrldata = new byte[100 * 1024];
                int in;
                int i;
                for (i = 0; (in = bis.read()) != -1; i++) readcrldata[i] = (byte) in;
                bis.close();
                logger.chat("Bytes read: " + i);
                byte[] crldata = new byte[i];
                int j = 0;
                for (j = 0; j < i; j++) {
                    crldata[j] = readcrldata[j];
                }
                String crlpem = new String(crldata);
                if (crlpem.indexOf("-----BEGIN X509 CRL") != -1) {
                    new_crl = readCRLFromPEM(crldata, cf);
                } else {
                    new_crl = readCRLFromDER(crldata, cf);
                }
                if (new_crl == null) {
                    problem = "CRL could not be parsed (probably no DER or PEM format)";
                } else {
                    Enumeration e = ListenerManager.getGKS().aliases();
                    while (e.hasMoreElements()) {
                        Certificate c = ListenerManager.getGKS().getCertificate((String) e.nextElement());
                        if (c != null) {
                            try {
                                new_crl.verify(c.getPublicKey());
                                ok = true;
                                break;
                            } catch (Exception ex) {
                            }
                        }
                    }
                    if (!ok) {
                        problem = "The updated CRL was not created by any of the allowed CAs";
                    }
                }
            } catch (java.net.MalformedURLException mfuex) {
                problem = "Unknown protocol to access CRL";
            } catch (CertificateException cex) {
                problem = "CRLs Could not get the X509 CertificateFactory (check application CLASSPATH): " + cex;
            } catch (CRLException crlex) {
                problem = "The address does not return a recognised CRL: " + crlex;
            } catch (java.io.IOException ioex) {
                problem = "Problems updating crl: " + ioex;
            } catch (java.security.KeyStoreException ksex) {
                problem = "Problems getting public keys for allowed CAs (in CRL fetching): " + ksex;
            } finally {
                if (ok) {
                    crls.put(uri, new_crl);
                    Date now = new Date();
                    Date next_update = new_crl.getNextUpdate();
                    if (next_update == null || next_update.before(now)) {
                        next_update = new Date(now.getTime() + Configuration.defaultCRLValidity());
                    }
                    Alarm.get().set(new Waiter(uri, this), next_update.getTime());
                    logger.info("Updated CRL from <" + uri + "> next update = " + next_update);
                } else {
                    Date update_of_old = null;
                    if (old_crl != null) update_of_old = old_crl.getNextUpdate();
                    if (update_of_old != null && Configuration.getCRLGrace() > 0.0) {
                        long valid_time = update_of_old.getTime() - old_crl.getThisUpdate().getTime();
                        if (valid_time < 0) {
                            valid_time = 1;
                        }
                        long grace_so_far = System.currentTimeMillis() - update_of_old.getTime();
                        if ((((double) grace_so_far) / ((double) valid_time)) < Configuration.getCRLGrace()) {
                            crls.put(uri, old_crl);
                            long next_update = System.currentTimeMillis() + (long) (Configuration.getCRLGrace() * (valid_time / 10.0));
                            Alarm.get().set(new Waiter(uri, this), next_update);
                            logger.warning("Updating CRL from <" + uri + "> failed because: " + problem + "\nWithin CRL grace period so keeping previous until: " + new Date(next_update));
                        } else {
                            logger.severe("Updating CRL from <" + uri + "> failed because: " + problem + "\nOut of grace period so there is no valid CRL for this CA.");
                        }
                    } else {
                        logger.severe("Updating CRL from <" + uri + "> failed because: " + problem + "\nOut of grace period or no previously valid CRL, so there is no valid CRL for this CA.");
                    }
                }
                synchronized (this) {
                    being_fetched.remove(uri);
                }
                synchronized (fing) {
                    fing.notifyAll();
                }
            }
        } else {
            synchronized (fing) {
                try {
                    if (logger.CHAT) logger.log("Waiting for CRL update to complete on <" + uri + ">", LoggerLevel.CHAT);
                    fing.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    public void ring() {
        Alarm.get().set(this, System.currentTimeMillis() + Configuration.getwatchInterval());
        if (invalidate_file.exists()) {
            invalidate_file.delete();
            logger.warning("All current Certificate Revocation lists invalidated");
            crls = Collections.synchronizedMap(new HashMap());
            invalidated++;
        }
    }

    private class Waiter implements Alarm.Bell {

        String uri;

        ActiveCRLChecker cc;

        private int old_invalidated;

        public Waiter(String uri, ActiveCRLChecker cc) {
            this.uri = uri;
            this.cc = cc;
            old_invalidated = cc.invalidated;
        }

        public void ring() {
            if (old_invalidated == cc.invalidated) {
                new Thread(new Runnable() {

                    public void run() {
                        cc.fetchCRL(uri);
                    }
                }).start();
            }
        }
    }

    class GHostnameVerifier implements HostnameVerifier {

        public boolean verify(String arg0, SSLSession arg1) {
            return false;
        }
    }
}
