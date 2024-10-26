package net.esle.sinadura.core.validator;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;
import java.security.cert.*;
import javax.security.auth.x500.X500Principal;
import sun.security.action.GetPropertyAction;
import sun.security.util.*;
import sun.security.x509.*;

/**
 * Class to obtain CRLs via the CRLDistributionPoints extension.
 * Note that the functionality of this class must be explicitly enabled
 * via a system property, see the USE_CRLDP variable below.
 *
 * This class also implements CRL caching. Currently, the cache is shared
 * between all applications in the VM and uses a hardcoded policy.
 * The cache has a maximum size of 185 entries, which are held by 
 * SoftReferences. A request will be satisfied from the cache if we last
 * checked for an update within CHECK_INTERVAL (last 30 seconds). Otherwise,
 * we open an URLConnection to download the CRL using an If-Modified-Since 
 * request (HTTP) if possible. Note that both positive and negative responses
 * are cached, i.e. if we are unable to open the connection or the CRL cannot
 * be parsed, we remember this result and additional calls during the
 * CHECK_INTERVAL period do not try to open another connection.
 *
 * @author Andreas Sterbenz
 * @author Sean Mullan
 * @version 1.10, 05/08/06
 * @since 1.4.2
 */
class DistributionPointFetcher {

    private static final Debug debug = Debug.getInstance("certpath");

    private static final boolean[] ALL_REASONS = { true, true, true, true, true, true, true, true, true };

    /**
     * Flag indicating whether support for the CRL distribution point
     * extension shall be enabled. Currently disabled by default for
     * compatibility and legal reasons.
     */
    private static final boolean USE_CRLDP = getBooleanProperty("com.sun.security.enableCRLDP", false);

    /**
     * Return the value of the boolean System property propName.
     */
    public static boolean getBooleanProperty(String propName, boolean defaultValue) {
        String b = (String) AccessController.doPrivileged(new GetPropertyAction(propName));
        if (b == null) {
            return defaultValue;
        } else if (b.equalsIgnoreCase("false")) {
            return false;
        } else if (b.equalsIgnoreCase("true")) {
            return true;
        } else {
            throw new RuntimeException("Value of " + propName + " must either be 'true' or 'false'");
        }
    }

    private static final DistributionPointFetcher INSTANCE = new DistributionPointFetcher();

    private static final int CHECK_INTERVAL = 30 * 1000;

    private static final int CACHE_SIZE = 185;

    private final CertificateFactory factory;

    /**
     * CRL cache mapping URI -> CacheEntry.
     */
    private final Cache cache;

    /** 
     * Private instantiation only.
     */
    private DistributionPointFetcher() {
        try {
            factory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new RuntimeException();
        }
        cache = Cache.newSoftMemoryCache(CACHE_SIZE);
    }

    /**
     * Return a DistributionPointFetcher instance.
     */
    static DistributionPointFetcher getInstance() {
        return INSTANCE;
    }

    /**
     * Return the X509CRLs matching this selector. The selector must be
     * an X509CRLSelector with certificateChecking set.
     *
     * If CRLDP support is disabled, this method always returns an
     * empty set.
     */
    Collection<X509CRL> getCRLs(CRLSelector selector, PublicKey prevKey, String provider, List<CertStore> certStores, boolean[] reasonsMask) throws CertStoreException {
        if (USE_CRLDP == false) {
            return Collections.emptySet();
        }
        if (selector instanceof X509CRLSelector == false) {
            return Collections.emptySet();
        }
        X509CRLSelector x509Selector = (X509CRLSelector) selector;
        X509Certificate cert = x509Selector.getCertificateChecking();
        if (cert == null) {
            return Collections.emptySet();
        }
        try {
            X509CertImpl certImpl = X509CertImpl.toImpl(cert);
            if (debug != null) {
                debug.println("DistributionPointFetcher.getCRLs: Checking " + "CRLDPs for " + certImpl.getSubjectX500Principal());
            }
            CRLDistributionPointsExtension ext = certImpl.getCRLDistributionPointsExtension();
            if (ext == null) {
                if (debug != null) {
                    debug.println("No CRLDP ext");
                }
                return Collections.emptySet();
            }
            List points = (List) ext.get(CRLDistributionPointsExtension.POINTS);
            Set<X509CRL> results = new HashSet<X509CRL>();
            for (Iterator t = points.iterator(); t.hasNext() && !Arrays.equals(reasonsMask, ALL_REASONS); ) {
                DistributionPoint point = (DistributionPoint) t.next();
                Collection<X509CRL> crls = getCRLs(x509Selector, certImpl, point, reasonsMask, prevKey, provider, certStores);
                results.addAll(crls);
            }
            if (debug != null) {
                debug.println("Returning " + results.size() + " CRLs");
            }
            return results;
        } catch (CertificateException e) {
            return Collections.emptySet();
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    /**
     * Download CRLs from the given distribution point, verify and return them.
     * See the top of the class for current limitations.
     */
    private Collection<X509CRL> getCRLs(X509CRLSelector selector, X509CertImpl certImpl, DistributionPoint point, boolean[] reasonsMask, PublicKey prevKey, String provider, List<CertStore> certStores) {
        GeneralNames fullName = point.getFullName();
        if (fullName == null) {
            return Collections.emptySet();
        }
        Collection<X509CRL> possibleCRLs = new ArrayList<X509CRL>();
        Collection<X509CRL> crls = new ArrayList<X509CRL>(2);
        for (Iterator t = fullName.iterator(); t.hasNext(); ) {
            GeneralName name = (GeneralName) t.next();
            if (name.getType() == GeneralNameInterface.NAME_DIRECTORY) {
                X500Name x500Name = (X500Name) name.getName();
                possibleCRLs.addAll(getCRLs(x500Name, certImpl.getIssuerX500Principal(), certStores));
            } else if (name.getType() == GeneralNameInterface.NAME_URI) {
                URIName uriName = (URIName) name.getName();
                X509CRL crl = getCRL(uriName);
                if (crl != null) {
                    possibleCRLs.add(crl);
                }
            }
        }
        for (X509CRL crl : possibleCRLs) {
            try {
                selector.setIssuerNames(null);
                if (selector.match(crl) && verifyCRL(certImpl, point, crl, reasonsMask, prevKey, provider)) {
                    crls.add(crl);
                }
            } catch (Exception e) {
                if (debug != null) {
                    debug.println("Exception verifying CRL: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return crls;
    }

    /**
     * Download CRL from given URI.
     */
    private X509CRL getCRL(URIName name) {
        URI uri = name.getURI();
        if (debug != null) {
            debug.println("Trying to fetch CRL from DP " + uri);
        }
        if (uri.getScheme().toLowerCase().equals("ldap")) {
            String path = uri.getPath();
            if (debug != null) {
                debug.println("authority:" + uri.getAuthority());
                debug.println("path:" + path);
            }
            if (path.charAt(0) == '/') {
                path = path.substring(1);
            }
            try {
                LDAPCertStore.LDAPCRLSelector sel = new LDAPCertStore.LDAPCRLSelector();
                sel.addIssuerName(path);
                CertStore lcs = LDAPCertStore.getInstance(LDAPCertStore.getParameters(uri));
                Collection crls = lcs.getCRLs(sel);
                if (crls.isEmpty()) {
                    return null;
                } else {
                    return (X509CRL) crls.iterator().next();
                }
            } catch (Exception e) {
                if (debug != null) {
                    debug.println("Exception getting CRL from CertStore: " + e);
                    e.printStackTrace();
                }
            }
            return null;
        }
        CacheEntry entry = (CacheEntry) cache.get(uri);
        if (entry == null) {
            entry = new CacheEntry();
            cache.put(uri, entry);
        }
        return entry.getCRL(factory, uri);
    }

    /**
     * Fetch CRLs from certStores.
     */
    private Collection<X509CRL> getCRLs(X500Name name, X500Principal certIssuer, List<CertStore> certStores) {
        if (debug != null) {
            debug.println("Trying to fetch CRL from DP " + name);
        }
        X509CRLSelector xcs = new X509CRLSelector();
        xcs.addIssuer(name.asX500Principal());
        xcs.addIssuer(certIssuer);
        Collection<X509CRL> crls = new ArrayList<X509CRL>();
        for (CertStore store : certStores) {
            try {
                crls.addAll((Collection<X509CRL>) store.getCRLs(xcs));
            } catch (CertStoreException cse) {
                if (debug != null) {
                    debug.println("Non-fatal exception while retrieving " + "CRLs: " + cse);
                    cse.printStackTrace();
                }
            }
        }
        return crls;
    }

    /**
     * Verifies a CRL for the given certificate's Distribution Point to 
     * ensure it is appropriate for checking the revocation status.
     *
     * @param certImpl the certificate whose revocation status is being checked
     * @param point one of the distribution points of the certificate
     * @param crl the CRL
     * @param reasonsMask the interim reasons mask
     * @param prevKey the public key that verifies the certificate's signature
     * @param provider the Signature provider to use
     * @return true if ok, false if not
     */
    boolean verifyCRL(X509CertImpl certImpl, DistributionPoint point, X509CRL crl, boolean[] reasonsMask, PublicKey prevKey, String provider) throws CRLException, IOException {
        X509CRLImpl crlImpl = X509CRLImpl.toImpl(crl);
        IssuingDistributionPointExtension idpExt = crlImpl.getIssuingDistributionPointExtension();
        X500Name certIssuer = (X500Name) certImpl.getIssuerDN();
        X500Name crlIssuer = (X500Name) crlImpl.getIssuerDN();
        GeneralNames pointCrlIssuer = point.getCRLIssuer();
        if (pointCrlIssuer != null) {
            if (idpExt == null || ((Boolean) idpExt.get(IssuingDistributionPointExtension.INDIRECT_CRL)).equals(Boolean.FALSE)) {
                return false;
            }
            boolean match = false;
            for (Iterator t = pointCrlIssuer.iterator(); !match && t.hasNext(); ) {
                GeneralNameInterface name = ((GeneralName) t.next()).getName();
                if (crlIssuer.equals(name) == true) {
                    match = true;
                }
            }
            if (match == false) {
                return false;
            }
        } else if (crlIssuer.equals(certIssuer) == false) {
            if (debug != null) {
                debug.println("crl issuer does not equal cert issuer");
            }
            return false;
        }
        if (idpExt != null) {
            DistributionPointName idpPoint = (DistributionPointName) idpExt.get(IssuingDistributionPointExtension.POINT);
            if (idpPoint != null) {
                GeneralNames idpNames = idpPoint.getFullName();
                if (idpNames == null) {
                    RDN relativeName = idpPoint.getRelativeName();
                    if (relativeName == null) {
                        if (debug != null) {
                            debug.println("IDP must be relative or full DN");
                        }
                        return false;
                    }
                    if (debug != null) {
                        debug.println("IDP relativeName:" + relativeName);
                    }
                    idpNames = getFullNames(crlIssuer, relativeName);
                }
                if (point.getFullName() != null || point.getRelativeName() != null) {
                    GeneralNames pointNames = point.getFullName();
                    if (pointNames == null) {
                        RDN relativeName = point.getRelativeName();
                        if (relativeName == null) {
                            if (debug != null) {
                                debug.println("DP must be relative or full DN");
                            }
                            return false;
                        }
                        if (debug != null) {
                            debug.println("DP relativeName:" + relativeName);
                        }
                        pointNames = getFullNames(certIssuer, relativeName);
                    }
                    boolean match = false;
                    for (Iterator i = idpNames.iterator(); !match && i.hasNext(); ) {
                        GeneralNameInterface idpName = ((GeneralName) i.next()).getName();
                        if (debug != null) {
                            debug.println("idpName: " + idpName);
                        }
                        for (Iterator p = pointNames.iterator(); !match && p.hasNext(); ) {
                            GeneralNameInterface pointName = ((GeneralName) p.next()).getName();
                            if (debug != null) {
                                debug.println("pointName: " + pointName);
                            }
                            match = idpName.equals(pointName);
                        }
                    }
                    if (!match) {
                        if (debug != null) {
                            debug.println("IDP name does not match DP name");
                        }
                        return false;
                    }
                } else {
                    boolean match = false;
                    for (Iterator t = pointCrlIssuer.iterator(); !match && t.hasNext(); ) {
                        GeneralNameInterface crlIssuerName = ((GeneralName) t.next()).getName();
                        for (Iterator i = idpNames.iterator(); !match && i.hasNext(); ) {
                            GeneralNameInterface idpName = ((GeneralName) i.next()).getName();
                            match = crlIssuerName.equals(idpName);
                        }
                    }
                    if (!match) {
                        return false;
                    }
                }
            }
            Boolean b = (Boolean) idpExt.get(IssuingDistributionPointExtension.ONLY_USER_CERTS);
            if (b.equals(Boolean.TRUE) && certImpl.getBasicConstraints() != -1) {
                if (debug != null) {
                    debug.println("cert must be a EE cert");
                }
                return false;
            }
            b = (Boolean) idpExt.get(IssuingDistributionPointExtension.ONLY_CA_CERTS);
            if (b.equals(Boolean.TRUE) && certImpl.getBasicConstraints() == -1) {
                if (debug != null) {
                    debug.println("cert must be a CA cert");
                }
                return false;
            }
            b = (Boolean) idpExt.get(IssuingDistributionPointExtension.ONLY_ATTRIBUTE_CERTS);
            if (b.equals(Boolean.TRUE)) {
                if (debug != null) {
                    debug.println("cert must not be an AA cert");
                }
                return false;
            }
        }
        boolean[] interimReasonsMask = new boolean[9];
        ReasonFlags reasons = null;
        if (idpExt != null) {
            reasons = (ReasonFlags) idpExt.get(IssuingDistributionPointExtension.REASONS);
        }
        boolean[] pointReasonFlags = point.getReasonFlags();
        if (reasons != null) {
            if (pointReasonFlags != null) {
                boolean[] idpReasonFlags = reasons.getFlags();
                for (int i = 0; i < idpReasonFlags.length; i++) {
                    if (idpReasonFlags[i] && pointReasonFlags[i]) {
                        interimReasonsMask[i] = true;
                    }
                }
            } else {
                interimReasonsMask = (boolean[]) reasons.getFlags().clone();
            }
        } else if (idpExt == null || reasons == null) {
            if (pointReasonFlags != null) {
                interimReasonsMask = (boolean[]) pointReasonFlags.clone();
            } else {
                interimReasonsMask = new boolean[9];
                Arrays.fill(interimReasonsMask, true);
            }
        }
        boolean oneOrMore = false;
        for (int i = 0; i < interimReasonsMask.length && !oneOrMore; i++) {
            if (!reasonsMask[i] && interimReasonsMask[i]) {
                oneOrMore = true;
            }
        }
        if (!oneOrMore) {
            return false;
        }
        try {
            crl.verify(prevKey, provider);
        } catch (Exception e) {
            if (debug != null) {
                debug.println("CRL signature failed to verify");
            }
            return false;
        }
        Set unresCritExts = crl.getCriticalExtensionOIDs();
        if (unresCritExts != null) {
            unresCritExts.remove(PKIXExtensions.IssuingDistributionPoint_Id.toString());
            if (!unresCritExts.isEmpty()) {
                if (debug != null) {
                    debug.println("Unrecognized critical extension(s) in CRL: " + unresCritExts);
                    Iterator i = unresCritExts.iterator();
                    while (i.hasNext()) debug.println((String) i.next());
                }
                return false;
            }
        }
        for (int i = 0; i < interimReasonsMask.length; i++) {
            if (!reasonsMask[i] && interimReasonsMask[i]) {
                reasonsMask[i] = true;
            }
        }
        return true;
    }

    /**
     * Append relative name to the issuer name and return a new
     * GeneralNames object.
     */
    private GeneralNames getFullNames(X500Name issuer, RDN rdn) throws IOException {
        List<RDN> rdns = new ArrayList<RDN>(issuer.rdns());
        rdns.add(rdn);
        X500Name fullName = new X500Name(((RDN[]) rdns.toArray(new RDN[0])));
        GeneralNames fullNames = new GeneralNames();
        fullNames.add(new GeneralName(fullName));
        return fullNames;
    }

    /**
     * Inner class used for cache entries.
     */
    private static class CacheEntry {

        private X509CRL crl;

        private long lastChecked;

        private long lastModified;

        CacheEntry() {
        }

        /**
	 * Return the CRL for this entry. It returns the cached value
	 * if it is still current and fetches the CRL otherwise.
	 * For the caching details, see the top of this class.
	 */
        synchronized X509CRL getCRL(CertificateFactory factory, URI uri) {
            long time = System.currentTimeMillis();
            if (time - lastChecked < CHECK_INTERVAL) {
                if (debug != null) {
                    debug.println("Returning CRL from cache");
                }
                return crl;
            }
            lastChecked = time;
            InputStream in = null;
            try {
                URL url = uri.toURL();
                URLConnection connection = url.openConnection();
                if (lastModified != 0) {
                    connection.setIfModifiedSince(lastModified);
                }
                in = connection.getInputStream();
                long oldLastModified = lastModified;
                lastModified = connection.getLastModified();
                if (oldLastModified != 0) {
                    if (oldLastModified == lastModified) {
                        if (debug != null) {
                            debug.println("Not modified, using cached copy");
                        }
                        return crl;
                    } else if (connection instanceof HttpURLConnection) {
                        HttpURLConnection hconn = (HttpURLConnection) connection;
                        if (hconn.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                            if (debug != null) {
                                debug.println("Not modified, using cached copy");
                            }
                            return crl;
                        }
                    }
                }
                if (debug != null) {
                    debug.println("Downloading new CRL...");
                }
                crl = (X509CRL) factory.generateCRL(in);
                return crl;
            } catch (IOException e) {
                if (debug != null) {
                    debug.println("Exception fetching CRLDP:");
                    e.printStackTrace();
                }
            } catch (CRLException e) {
                if (debug != null) {
                    debug.println("Exception fetching CRLDP:");
                    e.printStackTrace();
                }
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
            }
            lastModified = 0;
            crl = null;
            return null;
        }
    }
}
