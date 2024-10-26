package org.ungoverned.osgi.bundle.bundlerepository;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import kxml.sax.KXmlSAXParser;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.ungoverned.osgi.service.bundlerepository.*;
import fr.imag.adele.metadataparser.MultivalueMap;
import fr.imag.adele.metadataparser.XmlCommonHandler;

public class BundleRepositoryServiceImpl implements BundleRepositoryService {

    private BundleContext m_context = null;

    private boolean m_initialized = false;

    private String[] m_urls = null;

    private List m_bundleList = new ArrayList();

    private Map m_exportPackageMap = new HashMap();

    private static final int EXPORT_PACKAGE_IDX = 0;

    private static final int EXPORT_BUNDLE_IDX = 1;

    private int m_hopCount = 1;

    private static final String[] DEFAULT_REPOSITORY_URL = { "http://oscar-osgi.sf.net/repository.xml" };

    public static final String REPOSITORY_URL_PROP = "oscar.repository.url";

    public static final String EXTERN_REPOSITORY_TAG = "extern-repositories";

    public BundleRepositoryServiceImpl(BundleContext context) {
        m_context = context;
        String urlStr = context.getProperty(REPOSITORY_URL_PROP);
        if (urlStr != null) {
            StringTokenizer st = new StringTokenizer(urlStr);
            if (st.countTokens() > 0) {
                m_urls = new String[st.countTokens()];
                for (int i = 0; i < m_urls.length; i++) {
                    m_urls[i] = st.nextToken();
                }
            }
        }
        if (m_urls == null) {
            m_urls = DEFAULT_REPOSITORY_URL;
        }
    }

    public String[] getRepositoryURLs() {
        if (m_urls != null) {
            return (String[]) m_urls.clone();
        }
        return null;
    }

    public synchronized void setRepositoryURLs(String[] urls) {
        if (urls != null) {
            m_urls = urls;
            initialize();
        }
    }

    /**
     * Get the number of bundles available in the repository.
     * @return the number of available bundles.
    **/
    public synchronized int getBundleRecordCount() {
        if (!m_initialized) {
            initialize();
        }
        return m_bundleList.size();
    }

    /**
     * Get the specified bundle record from the repository.
     * @param i the bundle record index to retrieve.
     * @return the associated bundle record or <tt>null</tt>.
    **/
    public synchronized BundleRecord getBundleRecord(int i) {
        if (!m_initialized) {
            initialize();
        }
        if ((i < 0) || (i >= getBundleRecordCount())) {
            return null;
        }
        return (BundleRecord) m_bundleList.get(i);
    }

    /**
     * Get bundle record for the bundle with the specified name
     * and version from the repository.
     * @param name the bundle record name to retrieve.
     * @param version three-interger array of the version associated with
     *        the name to retrieve.
     * @return the associated bundle record or <tt>null</tt>.
    **/
    public synchronized BundleRecord getBundleRecord(String name, int[] version) {
        if (!m_initialized) {
            initialize();
        }
        BundleRecord[] records = getBundleRecords(name);
        if (records.length > 0) {
            for (int i = 0; i < records.length; i++) {
                String targetName = (String) records[i].getAttribute(BundleRecord.BUNDLE_NAME);
                int[] targetVersion = Util.parseVersionString((String) records[i].getAttribute(BundleRecord.BUNDLE_VERSION));
                if ((targetName != null) && targetName.equalsIgnoreCase(name) && (Util.compareVersion(targetVersion, version) == 0)) {
                    return records[i];
                }
            }
        }
        return null;
    }

    /**
     * Get all versions of bundle records for the specified name
     * from the repository.
     * @param name the bundle record name to retrieve.
     * @return an array of all versions of bundle records having the
     *         specified name or <tt>null</tt>.
    **/
    public synchronized BundleRecord[] getBundleRecords(String name) {
        if (!m_initialized) {
            initialize();
        }
        BundleRecord[] records = new BundleRecord[0];
        for (int i = 0; i < m_bundleList.size(); i++) {
            String targetName = (String) getBundleRecord(i).getAttribute(BundleRecord.BUNDLE_NAME);
            if ((targetName != null) && targetName.equalsIgnoreCase(name)) {
                BundleRecord[] newRecords = new BundleRecord[records.length + 1];
                System.arraycopy(records, 0, newRecords, 0, records.length);
                newRecords[records.length] = getBundleRecord(i);
                records = newRecords;
            }
        }
        return records;
    }

    public boolean deployBundle(PrintStream out, PrintStream err, String updateLocation, boolean isResolve, boolean isStart) {
        List startList = null;
        Bundle localBundle = findLocalBundleByUpdate(updateLocation);
        if (localBundle != null) {
            if (!isUpdateAvailable(out, err, localBundle)) {
                out.println("No update available: " + Util.getBundleName(localBundle));
                return false;
            }
        }
        BundleRecord record = findBundleRecordByUpdate(updateLocation);
        if (record != null) {
            List deployList = new ArrayList();
            deployList.add(updateLocation);
            if (isResolve) {
                PackageDeclaration[] imports = (PackageDeclaration[]) record.getAttribute(BundleRecord.IMPORT_PACKAGE);
                try {
                    resolvePackages(imports, deployList);
                } catch (ResolveException ex) {
                    err.println("Resolve error: " + ex.getPackageDeclaration());
                    return false;
                }
            }
            for (int i = deployList.size() - 1; i >= 0; i--) {
                String deployLocation = (String) deployList.get(i);
                localBundle = findLocalBundleByUpdate(deployLocation);
                if (localBundle != null) {
                    if (!isUpdateAvailable(out, err, localBundle)) {
                        continue;
                    }
                    if (!deployLocation.equals(updateLocation)) {
                        out.print("Updating dependency: ");
                    } else {
                        out.print("Updating: ");
                    }
                    out.println(Util.getBundleName(localBundle));
                    try {
                        localBundle.update();
                    } catch (BundleException ex) {
                        err.println("Update error: " + Util.getBundleName(localBundle));
                        ex.printStackTrace(err);
                        return false;
                    }
                } else {
                    if (!deployLocation.equals(updateLocation)) {
                        out.print("Installing dependency: ");
                    } else {
                        out.print("Installing: ");
                    }
                    record = findBundleRecordByUpdate(deployLocation);
                    out.println(record.getAttribute(BundleRecord.BUNDLE_NAME));
                    try {
                        Bundle bundle = m_context.installBundle(deployLocation);
                        if (isStart) {
                            if (startList == null) {
                                startList = new ArrayList();
                            }
                            startList.add(bundle);
                        }
                    } catch (BundleException ex) {
                        err.println("Install error: " + record.getAttribute(BundleRecord.BUNDLE_NAME));
                        ex.printStackTrace(err);
                        return false;
                    }
                }
            }
            if (isStart) {
                for (int i = 0; (startList != null) && (i < startList.size()); i++) {
                    localBundle = (Bundle) startList.get(i);
                    try {
                        localBundle.start();
                    } catch (BundleException ex) {
                        err.println("Update error: " + Util.getBundleName(localBundle));
                        ex.printStackTrace();
                    }
                }
            }
            return true;
        }
        return false;
    }

    public BundleRecord[] resolvePackages(PackageDeclaration[] pkgs) throws ResolveException {
        List deployList = new ArrayList();
        resolvePackages(pkgs, deployList);
        BundleRecord[] records = new BundleRecord[deployList.size()];
        for (int i = 0; i < deployList.size(); i++) {
            String updateLocation = (String) deployList.get(i);
            records[i] = findBundleRecordByUpdate(updateLocation);
        }
        return records;
    }

    public void resolvePackages(PackageDeclaration[] pkgs, List deployList) throws ResolveException {
        for (int pkgIdx = 0; (pkgs != null) && (pkgIdx < pkgs.length); pkgIdx++) {
            if (!isLocallyResolvable(pkgs[pkgIdx])) {
                BundleRecord source = selectResolvingBundle(pkgs[pkgIdx]);
                if (source == null) {
                    throw new ResolveException(pkgs[pkgIdx]);
                }
                String updateLocation = (String) source.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);
                if (!deployList.contains(updateLocation)) {
                    deployList.add(updateLocation);
                    PackageDeclaration[] imports = (PackageDeclaration[]) source.getAttribute(BundleRecord.IMPORT_PACKAGE);
                    resolvePackages(imports, deployList);
                }
            }
        }
    }

    /**
     * Returns a locally installed bundle that has an update location
     * manifest attribute that matches the specified update location
     * value.
     * @param updateLocation the update location attribute for which to search.
     * @return a bundle with a matching update location attribute or
     *         <tt>null</tt> if one could not be found.
    **/
    private Bundle findLocalBundleByUpdate(String updateLocation) {
        Bundle[] locals = m_context.getBundles();
        for (int i = 0; i < locals.length; i++) {
            String localUpdateLocation = (String) locals[i].getHeaders().get(BundleRecord.BUNDLE_UPDATELOCATION);
            if ((localUpdateLocation != null) && localUpdateLocation.equals(updateLocation)) {
                return locals[i];
            }
        }
        return null;
    }

    private boolean isUpdateAvailable(PrintStream out, PrintStream err, Bundle bundle) {
        String updateLocation = (String) bundle.getHeaders().get(Constants.BUNDLE_UPDATELOCATION);
        BundleRecord record = findBundleRecordByUpdate(updateLocation);
        if (record == null) {
            err.println(Util.getBundleName(bundle) + " not in repository.");
            return false;
        }
        int[] bundleVersion = Util.parseVersionString((String) bundle.getHeaders().get(Constants.BUNDLE_VERSION));
        int[] recordVersion = Util.parseVersionString((String) record.getAttribute(BundleRecord.BUNDLE_VERSION));
        if (Util.compareVersion(recordVersion, bundleVersion) > 0) {
            return true;
        }
        return false;
    }

    /**
     * Returns the bundle record corresponding to the specified update
     * location string; update location strings are assumed to be
     * unique.
     * @param updateLocation the update location of the bundle record
     *        to retrieve.
     * @return the corresponding bundle record or <tt>null</tt>.
    **/
    private synchronized BundleRecord findBundleRecordByUpdate(String updateLocation) {
        if (!m_initialized) {
            initialize();
        }
        for (int i = 0; i < m_bundleList.size(); i++) {
            String location = (String) getBundleRecord(i).getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);
            if ((location != null) && location.equalsIgnoreCase(updateLocation)) {
                return getBundleRecord(i);
            }
        }
        return null;
    }

    /**
     * Determines whether a package is resolvable at the local framework.
     * @param target the package declaration to check for availability.
     * @return <tt>true</tt> if the package is available locally,
     *         <tt>false</tt> otherwise.
    **/
    private synchronized boolean isLocallyResolvable(PackageDeclaration target) {
        ServiceReference ref = m_context.getServiceReference("org.osgi.service.packageadmin.PackageAdmin");
        if (ref == null) {
            return false;
        }
        PackageAdmin pa = (PackageAdmin) m_context.getService(ref);
        if (pa == null) {
            return false;
        }
        ExportedPackage[] exports = pa.getExportedPackages((Bundle) null);
        if (exports != null) {
            for (int i = 0; (exports != null) && (i < exports.length); i++) {
                PackageDeclaration source = new PackageDeclaration(exports[i].getName(), exports[i].getSpecificationVersion());
                if (source.doesSatisfy(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Selects a single source bundle record for the target package from
     * the repository. The algorithm tries to select a source bundle record
     * if it is already installed locally in the framework; this approach
     * favors updating already installed bundles rather than installing
     * new ones. If no matching bundles are installed locally, then the
     * first bundle record providing the target package is returned.
     * @param targetPkg the target package for which to select a source
     *        bundle record.
     * @return the selected bundle record or <tt>null</tt> if no sources
     *         could be found.
    **/
    private BundleRecord selectResolvingBundle(PackageDeclaration targetPkg) {
        BundleRecord[] sources = findResolvingBundles(targetPkg);
        if (sources == null) {
            return null;
        }
        for (int i = 0; i < sources.length; i++) {
            String updateLocation = (String) sources[i].getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);
            if (updateLocation != null) {
                Bundle bundle = findLocalBundleByUpdate(updateLocation);
                if (bundle != null) {
                    return sources[i];
                }
            }
        }
        return sources[0];
    }

    /**
     * Returns an array of bundle records that resolve the supplied
     * package declaration.
     * @param target the package declaration to resolve.
     * @return an array of bundle records that resolve the package
     *         declaration or <tt>null</tt> if none are found.
    **/
    private synchronized BundleRecord[] findResolvingBundles(PackageDeclaration targetPkg) {
        ArrayList resolveList = new ArrayList();
        ArrayList exporterList = (ArrayList) m_exportPackageMap.get(targetPkg.getName());
        for (int i = 0; (exporterList != null) && (i < exporterList.size()); i++) {
            Object[] exportInfo = (Object[]) exporterList.get(i);
            PackageDeclaration exportPkg = (PackageDeclaration) exportInfo[EXPORT_PACKAGE_IDX];
            BundleRecord exportBundle = (BundleRecord) exportInfo[EXPORT_BUNDLE_IDX];
            if (exportPkg.doesSatisfy(targetPkg)) {
                resolveList.add(exportBundle);
            }
        }
        if (resolveList.size() == 0) {
            return null;
        }
        return (BundleRecord[]) resolveList.toArray(new BundleRecord[resolveList.size()]);
    }

    private void initialize() {
        m_initialized = true;
        m_bundleList.clear();
        m_exportPackageMap.clear();
        for (int urlIdx = 0; (m_urls != null) && (urlIdx < m_urls.length); urlIdx++) {
            parseRepositoryFile(m_hopCount, m_urls[urlIdx]);
        }
    }

    private String getUserAgentForBundle(Bundle bundle) {
        String uaName = bundle.getSymbolicName();
        String uaVers = (String) bundle.getHeaders().get("Bundle-Version");
        String uaComm = "";
        if (0 == bundle.getBundleId()) {
            uaComm = uaVers;
            uaName = m_context.getProperty(Constants.FRAMEWORK_VENDOR);
            uaVers = m_context.getProperty(Constants.FRAMEWORK_VERSION);
        }
        return uaName + (uaVers != null && uaVers.length() > 0 ? ("/" + uaVers) : "") + (uaComm != null && uaComm.length() > 0 ? (" (" + uaComm + ")") : "");
    }

    private void parseRepositoryFile(int hopCount, String urlStr) {
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            String auth = m_context.getProperty("http.proxyAuth");
            if ((auth != null) && (auth.length() > 0)) {
                if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {
                    String base64 = Util.base64Encode(auth);
                    conn.setRequestProperty("Proxy-Authorization", "Basic " + base64);
                }
            }
            String basicAuth = m_context.getProperty("http.basicAuth");
            if (basicAuth != null && !"".equals(basicAuth)) {
                if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {
                    String base64 = Util.base64Encode(basicAuth);
                    conn.setRequestProperty("Authorization", "Basic " + base64);
                }
            }
            conn.setRequestProperty("User-Agent", getUserAgentForBundle(m_context.getBundle()) + " " + getUserAgentForBundle(m_context.getBundle(0)));
            is = conn.getInputStream();
            XmlCommonHandler handler = new XmlCommonHandler();
            handler.addType("bundles", ArrayList.class);
            handler.addType("repository", HashMap.class);
            handler.addType("extern-repositories", ArrayList.class);
            handler.addType("bundle", MultivalueMap.class);
            handler.addType("import-package", HashMap.class);
            handler.addType("export-package", HashMap.class);
            handler.setDefaultType(String.class);
            br = new BufferedReader(new InputStreamReader(is));
            KXmlSAXParser parser;
            parser = new KXmlSAXParser(br);
            try {
                parser.parseXML(handler);
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
            List root = (List) handler.getRoot();
            for (int bundleIdx = 0; bundleIdx < root.size(); bundleIdx++) {
                Object obj = root.get(bundleIdx);
                if (obj instanceof HashMap) {
                    Map repoMap = new TreeMap(new Comparator() {

                        public int compare(Object o1, Object o2) {
                            return o1.toString().compareToIgnoreCase(o2.toString());
                        }
                    });
                    repoMap.putAll((Map) obj);
                    if (hopCount > 0) {
                        List externList = (List) repoMap.get(EXTERN_REPOSITORY_TAG);
                        for (int i = 0; (externList != null) && (i < externList.size()); i++) {
                            parseRepositoryFile(hopCount - 1, (String) externList.get(i));
                        }
                    }
                } else if (obj instanceof MultivalueMap) {
                    Map bundleMap = new TreeMap(new Comparator() {

                        public int compare(Object o1, Object o2) {
                            return o1.toString().compareToIgnoreCase(o2.toString());
                        }
                    });
                    bundleMap.putAll((Map) obj);
                    Object target = bundleMap.get(BundleRecord.IMPORT_PACKAGE);
                    if (target != null) {
                        bundleMap.put(BundleRecord.IMPORT_PACKAGE, convertPackageDeclarations(target));
                    }
                    target = bundleMap.get(BundleRecord.EXPORT_PACKAGE);
                    if (target != null) {
                        bundleMap.put(BundleRecord.EXPORT_PACKAGE, convertPackageDeclarations(target));
                    }
                    BundleRecord record = new BundleRecord(bundleMap);
                    try {
                        PackageDeclaration[] exportPkgs = (PackageDeclaration[]) record.getAttribute(BundleRecord.EXPORT_PACKAGE);
                        for (int exportIdx = 0; (exportPkgs != null) && (exportIdx < exportPkgs.length); exportIdx++) {
                            ArrayList exporterList = (ArrayList) m_exportPackageMap.get(exportPkgs[exportIdx].getName());
                            if (exporterList == null) {
                                exporterList = new ArrayList();
                            }
                            Object[] exportInfo = new Object[2];
                            exportInfo[EXPORT_PACKAGE_IDX] = exportPkgs[exportIdx];
                            exportInfo[EXPORT_BUNDLE_IDX] = record;
                            exporterList.add(exportInfo);
                            m_exportPackageMap.put(exportPkgs[exportIdx].getName(), exporterList);
                        }
                        m_bundleList.add(record);
                    } catch (IllegalArgumentException ex) {
                    }
                }
            }
        } catch (MalformedURLException ex) {
            System.err.println("Error: " + ex);
        } catch (IOException ex) {
            System.err.println("Error: " + ex);
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException ex) {
            }
        }
        Collections.sort(m_bundleList, new Comparator() {

            public int compare(Object o1, Object o2) {
                BundleRecord r1 = (BundleRecord) o1;
                BundleRecord r2 = (BundleRecord) o2;
                String name1 = (String) r1.getAttribute(BundleRecord.BUNDLE_NAME);
                String name2 = (String) r2.getAttribute(BundleRecord.BUNDLE_NAME);
                return name1.compareToIgnoreCase(name2);
            }
        });
    }

    private PackageDeclaration[] convertPackageDeclarations(Object target) {
        PackageDeclaration[] decls = null;
        if (target instanceof Map) {
            decls = new PackageDeclaration[1];
            decls[0] = convertPackageMap((Map) target);
        } else if (target instanceof List) {
            List pkgList = (List) target;
            decls = new PackageDeclaration[pkgList.size()];
            for (int pkgIdx = 0; pkgIdx < decls.length; pkgIdx++) {
                decls[pkgIdx] = convertPackageMap((Map) pkgList.get(pkgIdx));
            }
        }
        return decls;
    }

    private PackageDeclaration convertPackageMap(Map map) {
        Map pkgMap = new TreeMap(new Comparator() {

            public int compare(Object o1, Object o2) {
                return o1.toString().compareToIgnoreCase(o2.toString());
            }
        });
        pkgMap.putAll((Map) map);
        String name = (String) pkgMap.get(PackageDeclaration.PACKAGE_ATTR);
        String version = (String) pkgMap.get(PackageDeclaration.VERSION_ATTR);
        if (name != null) {
            return new PackageDeclaration(name, version);
        }
        return null;
    }
}
