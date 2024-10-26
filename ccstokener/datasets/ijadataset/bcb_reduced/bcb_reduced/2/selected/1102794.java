package com.monad.homerun.admin.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.JarURLConnection;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;
import java.util.jar.Manifest;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import com.monad.homerun.admin.impl.Activator;
import com.monad.homerun.config.ConfigService;
import com.monad.homerun.config.LoadService;
import com.monad.homerun.core.GlobalProps;

/**
 * PackageInstaller is a utility for installing collections of resources
 * (packages) into the server. These resources can include: data object
 * load files, java libraries (jar files), help files, icons,
 * configuration files, etc. It reads a standard java archive file,
 * unpacks it, and distributes the contents to the correct locations.
 * It may invoke the ObjectLoader, so necessary data objects can be
 * loaded. It can also just extract a description from the package file,
 * for display to the user to help determine if she wants to install it.
 */
public class PackageInstaller {

    private static final String DESCRIP = "contents.html";

    private static final String DPATH = "PKG-INF";

    private static final String L10N = "L10n.properties";

    private PackageInstaller() {
    }

    public static String getDescription(String pkgURL) {
        String desc = null;
        try {
            URL descURL = new URL("jar:" + pkgURL + "!/" + DPATH + "/" + DESCRIP);
            desc = readJarURL(descURL);
        } catch (IOException ioe) {
            try {
                URL descURL = new URL("jar:" + pkgURL + "!/" + DESCRIP);
                desc = readJarURL(descURL);
            } catch (IOException ioe2) {
                desc = getManifestProperty(pkgURL, "Bundle-Description");
                if (desc == null) {
                    desc = "Not a valid HomeRun archive!";
                }
            }
        }
        if (desc == null) {
            desc = "Package lacks description";
        }
        return "<html><body>" + desc + "</body></html>";
    }

    private static String readJarURL(URL url) throws IOException {
        JarURLConnection juc = (JarURLConnection) url.openConnection();
        InputStream in = juc.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int i = in.read();
        while (i != -1) {
            out.write(i);
            i = in.read();
        }
        return out.toString();
    }

    public static Properties getLocalizerBase(String pkgUrl) {
        Properties props = new Properties();
        try {
            Properties l10nProps = Activator.cfgSvc.getProperties("L10n");
            String chain = l10nProps.getProperty("chain").trim();
            if (!"none".equals(chain)) {
                URL lznURL = new URL("jar:" + pkgUrl + "!/conf/" + L10N);
                JarURLConnection juc = (JarURLConnection) lznURL.openConnection();
                props.load(juc.getInputStream());
            }
        } catch (FileNotFoundException fnfE) {
        } catch (IOException ioE) {
            if (GlobalProps.DEBUG) {
                ioE.printStackTrace();
            }
        }
        return props;
    }

    public static Properties getLocalizerChain(String pkgUrl) {
        Properties props = null;
        try {
            props = getLocalizerBase(pkgUrl);
            FileInputStream in = new FileInputStream(getSiteFile(pkgUrl));
            Properties extProps = new Properties();
            extProps.load(in);
            for (Object key : extProps.keySet()) {
                String skey = (String) key;
                if (props.containsKey(skey)) {
                    props.setProperty(skey, extProps.getProperty(skey));
                }
            }
        } catch (FileNotFoundException fnfE) {
        } catch (MalformedURLException mfuE) {
            if (GlobalProps.DEBUG) {
                mfuE.printStackTrace();
            }
        } catch (IOException ioE) {
            if (GlobalProps.DEBUG) {
                ioE.printStackTrace();
            }
        }
        return props;
    }

    public static void localizeSite(String pkgUrl, Properties localizers) throws IOException {
        FileOutputStream out = new FileOutputStream(getSiteFile(pkgUrl));
        localizers.store(out, "PackageInstaller");
        out.close();
    }

    public static String installPackage(String pkgUrl) {
        return installPackage(pkgUrl, true).error;
    }

    private static AnnBundle installPackage(String pkgUrl, boolean start) {
        if (GlobalProps.DEBUG) {
            System.out.println("InstPkg Url:" + pkgUrl);
        }
        AnnBundle ab = new AnnBundle();
        BundleContext bc = Activator.getBundleContext();
        if (bc != null) {
            String symName = getManifestProperty(pkgUrl, "Bundle-SymbolicName");
            String pkgType = getManifestProperty(pkgUrl, "Homerun-Type");
            if (Activator.isPackageInstalled(symName, null)) {
                ab.status = AnnBundle.INSTALLED;
                ab.error = "package already installed";
                return ab;
            }
            Bundle[] bundles = bc.getBundles();
            String reqStr = getManifestProperty(pkgUrl, "Homerun-Require");
            if (reqStr != null) {
                boolean found = false;
                for (String req : reqStr.split(",")) {
                    for (Bundle bundle : bundles) {
                        if (match(req, bundle, false)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        ab.status = AnnBundle.FAILED;
                        ab.error = "missing requirement '" + req + "'";
                        return ab;
                    }
                    found = false;
                }
            }
            String l10nList = getManifestProperty(pkgUrl, "Homerun-L10n");
            if (l10nList != null) {
                String instPath = "var" + File.separator + "package" + File.separator + "L10n";
                for (String path : l10nList.split(",")) {
                    String jarName = "L10n/" + path;
                    InputStream dataStream = getDataStream(pkgUrl, jarName);
                    if (dataStream != null) {
                        installFile(dataStream, instPath, path, false);
                    }
                }
            }
            String instStr = getManifestProperty(pkgUrl, "Homerun-Install");
            if (instStr != null) {
                boolean found = false;
                List<Bundle> toStart = new ArrayList<Bundle>();
                for (String inst : instStr.split(",")) {
                    for (Bundle bundle : bundles) {
                        if (match(inst, bundle, true)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        String[] bits = inst.split(";");
                        String instUrl = null;
                        if (inst.indexOf("os=") > 0) {
                            int i = bits[2].indexOf("os=");
                            String os = bits[2].substring(i + 3);
                            if (GlobalProps.getOSTag().equals(os)) {
                                instUrl = resolvePackage(bits[0], null, true);
                            }
                        } else {
                            instUrl = resolvePackage(bits[0], null, true);
                        }
                        if (instUrl != null) {
                            String iType = getManifestProperty(instUrl, "Homerun-Type");
                            AnnBundle crb = null;
                            if (iType != null && "proxy".equals(iType)) {
                                crb = installPackage(instUrl, true);
                            } else {
                                crb = installPackage(instUrl, false);
                                if (crb.bundle != null) {
                                    toStart.add(crb.bundle);
                                }
                            }
                            if (crb.status == AnnBundle.FAILED) {
                                ab.status = crb.status;
                                ab.error = crb.error;
                                return ab;
                            }
                        }
                    }
                    found = false;
                }
                if (start && toStart.size() > 0) {
                    try {
                        for (Bundle sbund : toStart) {
                            sbund.start();
                        }
                    } catch (BundleException be) {
                        if (GlobalProps.DEBUG) {
                            System.out.println("Got start Bundle Exception");
                            be.printStackTrace();
                        }
                        ab.status = AnnBundle.FAILED;
                        ab.error = "package start failure";
                        return ab;
                    }
                }
            }
            try {
                ab.bundle = bc.installBundle(pkgUrl);
                if (ab.bundle != null) {
                    if (start && ab.bundle.getHeaders().get("Bundle-Activator") != null) {
                        ab.bundle.start();
                    }
                } else {
                    ab.status = AnnBundle.FAILED;
                    ab.error = "framework installation failure";
                    return ab;
                }
            } catch (BundleException be) {
                if (GlobalProps.DEBUG) {
                    System.out.println("Got install bundle exception");
                    be.printStackTrace();
                }
                ab.status = AnnBundle.FAILED;
                ab.error = "framework installation error";
                return ab;
            }
            String confList = getManifestProperty(pkgUrl, "Homerun-Conf");
            if (confList != null) {
                String instPath = "conf" + File.separator;
                boolean isExport = pkgType != null && "export".equals(pkgType);
                if (!isExport) {
                    instPath += getManifestProperty(pkgUrl, "Bundle-Category") + File.separator;
                }
                for (String name : confList.split(",")) {
                    String jarName = "conf/" + name;
                    InputStream dataStream = getDataStream(pkgUrl, jarName);
                    if (dataStream != null) {
                        String lName = name;
                        StringBuffer pb = new StringBuffer(instPath);
                        if (name.indexOf("/") != -1) {
                            String[] parts = name.split("/");
                            for (int j = 0; j < parts.length - 1; j++) {
                                pb.append(parts[j]);
                                if (j < parts.length - 2) {
                                    pb.append(File.separator);
                                }
                            }
                            lName = parts[parts.length - 1];
                        }
                        installFile(dataStream, pb.toString(), lName, true);
                    }
                }
            }
            String loadList = getManifestProperty(pkgUrl, "Homerun-Load");
            if (loadList != null) {
                LoadService loader = new ObjectLoader();
                for (String name : loadList.split(",")) {
                    String dataName = "data/" + name + ".xml";
                    InputStream dataStream = getDataStream(pkgUrl, dataName);
                    if (dataStream != null) {
                        loader.loadObjects(dataStream, pkgUrl);
                    }
                }
            }
            String coreList = getManifestProperty(pkgUrl, "Homerun-Cores");
            if (coreList != null) {
                LoadService loader = new ObjectLoader();
                for (String name : coreList.split(",")) {
                    String dataName = "data/" + name + ".xml";
                    InputStream dataStream = getDataStream(pkgUrl, dataName);
                    if (dataStream != null) {
                        loader.loadObjects(dataStream, pkgUrl);
                    }
                }
            }
            String iconList = getManifestProperty(pkgUrl, "Homerun-Icons");
            if (iconList != null) {
                String pathBase = "icons" + File.separator;
                for (String iconPath : iconList.split(",")) {
                    String jarName = "icons/" + iconPath;
                    InputStream dataStream = getDataStream(pkgUrl, jarName);
                    if (dataStream != null) {
                        String lName = iconPath;
                        StringBuffer pb = new StringBuffer(pathBase);
                        if (iconPath.indexOf("/") != -1) {
                            String[] parts = iconPath.split("/");
                            for (int j = 0; j < parts.length - 1; j++) {
                                pb.append(parts[j]);
                                if (j < parts.length - 2) {
                                    pb.append(File.separator);
                                }
                            }
                            lName = parts[parts.length - 1];
                        }
                        installFile(dataStream, pb.toString(), lName, true);
                    }
                }
            }
            String imageList = getManifestProperty(pkgUrl, "Homerun-Images");
            if (imageList != null) {
                String pathBase = "var" + File.separator + "images" + File.separator;
                for (String name : imageList.split(",")) {
                    String jarName = "images/" + name;
                    InputStream dataStream = getDataStream(pkgUrl, jarName);
                    if (dataStream != null) {
                        String lName = name;
                        StringBuffer pb = new StringBuffer(pathBase);
                        if (name.indexOf("/") != -1) {
                            String[] parts = name.split("/");
                            for (int j = 0; j < parts.length - 1; j++) {
                                pb.append(parts[j]);
                                if (j < parts.length - 2) {
                                    pb.append(File.separator);
                                }
                            }
                            lName = parts[parts.length - 1];
                        }
                        installFile(dataStream, pb.toString(), lName, true);
                    }
                }
            }
            String cssList = getManifestProperty(pkgUrl, "Homerun-CSS");
            if (cssList != null) {
                String relPath = "var" + File.separator + "web" + File.separator + "styles";
                for (String path : cssList.split(",")) {
                    String jarName = "WEB-INF/styles/" + path;
                    InputStream dataStream = getDataStream(pkgUrl, jarName);
                    if (dataStream != null) {
                        installFile(dataStream, relPath, path, false);
                    }
                }
            }
            String jnlpList = getManifestProperty(pkgUrl, "Homerun-Jnlp");
            if (jnlpList != null) {
                String pathBase = "var" + File.separator + "jnlp";
                for (String path : jnlpList.split(",")) {
                    InputStream dataStream = getDataStream(pkgUrl, path);
                    if (dataStream != null) {
                        installFile(dataStream, pathBase, path, false);
                    }
                }
            }
            String jarList = getManifestProperty(pkgUrl, "Homerun-SwingJars");
            if (jarList != null) {
                String pathBase = "var" + File.separator + "ui" + File.separator + "swing";
                for (String name : jarList.split(",")) {
                    String fullName = name + ".jar";
                    InputStream dataStream = getDataStream(pkgUrl, fullName);
                    if (dataStream != null) {
                        installFile(dataStream, pathBase, fullName, false);
                        ab.error = "Update";
                    }
                }
            }
            String lafList = getManifestProperty(pkgUrl, "Homerun-SwingLaf");
            if (lafList != null) {
                String pathBase = "var" + File.separator + "ui" + File.separator + "swing";
                String propPath = GlobalProps.getHomeDir() + File.separator + pathBase + File.separator + "laf.properties";
                File propFile = new File(propPath);
                Properties lafProps = new Properties();
                try {
                    if (propFile.exists()) {
                        lafProps.load(new FileReader(propFile));
                    }
                    for (String keyVal : lafList.split(",")) {
                        String[] parts = keyVal.split(":");
                        lafProps.setProperty(parts[0], parts[1]);
                    }
                    lafProps.store(new FileWriter(propFile), symName);
                    mergeFile(null, pathBase, "swingui.xml", true);
                    ab.error = "Update";
                } catch (IOException ioe) {
                    if (GlobalProps.DEBUG) {
                        System.out.println("problem reading laf properties");
                    }
                }
            }
            String uiConf = getManifestProperty(pkgUrl, "Homerun-SwingConf");
            if (uiConf != null) {
                String pathBase = "var" + File.separator + "ui" + File.separator + "swing";
                String jarName = "conf/" + uiConf;
                InputStream dataStream = getDataStream(pkgUrl, jarName);
                if (dataStream != null) {
                    String path = GlobalProps.getHomeDir() + File.separator + pathBase + File.separator + uiConf;
                    if (!new File(path).exists()) {
                        installFile(dataStream, pathBase, uiConf, false);
                    } else {
                        mergeFile(dataStream, pathBase, uiConf, false);
                    }
                    ab.error = "Update";
                }
            }
        } else {
            ab.error = "no package installer";
        }
        return ab;
    }

    public static String resolvePackage(String pkgName, String version, boolean logical) {
        return (Activator.repoSvc != null) ? Activator.repoSvc.resolveResource(pkgName, version, logical) : null;
    }

    public static String startPackage(long id) {
        String error = null;
        BundleContext bc = Activator.getBundleContext();
        if (bc != null) {
            try {
                Bundle bundle = bc.getBundle(id);
                if (bundle.getHeaders().get("Bundle-Activator") != null) {
                    bundle.start();
                }
            } catch (BundleException be) {
                if (GlobalProps.DEBUG) {
                    be.printStackTrace();
                }
                error = "IO exception";
            }
        }
        return error;
    }

    private static boolean match(String req, Bundle bundle, boolean exact) {
        String reqName = req;
        String reqVsn = null;
        Dictionary headers = bundle.getHeaders();
        String bdlName = (String) headers.get("Bundle-SymbolicName");
        String bdlVsn = (String) headers.get("Bundle-Version");
        if (bdlName == null) {
            return false;
        }
        if (req.indexOf(";version") > 0) {
            String[] bits = req.split(";");
            reqName = bits[0];
            int start = bits[1].indexOf("\"");
            reqVsn = bits[1].substring(start + 1, bits[1].lastIndexOf("\""));
        }
        if (exact || reqName.indexOf("-") < 0) {
            if (reqName.equals(bdlName)) {
                return reqVsn == null || reqVsn.equals(bdlVsn);
            }
        } else {
            String[] parts = reqName.split("-");
            String reqStem = parts[0];
            String reqSpec = parts[1];
            String bdlStem = bdlName;
            String bdlSpec = null;
            if (bdlName.indexOf("-") > 0) {
                String[] aparts = bdlName.split("-");
                bdlStem = aparts[0];
                bdlSpec = aparts[1];
            }
            if (reqStem.equals(bdlStem)) {
                if (reqVsn != null && (reqSpec.equals("*") || reqSpec.equals(bdlSpec))) {
                    if (reqVsn.endsWith("+")) {
                        reqVsn = reqVsn.substring(0, reqVsn.indexOf("+"));
                        return versionMatch(reqVsn, bdlVsn);
                    } else {
                        return reqVsn.equals(bdlVsn);
                    }
                }
            }
        }
        return false;
    }

    private static boolean versionMatch(String reqVsn, String pkgVsn) {
        int rIdx = reqVsn.indexOf(".");
        int pIdx = reqVsn.indexOf(".");
        int rVsn = -1;
        int pVsn = -1;
        if (rIdx == -1 || pIdx == -1) {
            rVsn = Integer.valueOf(reqVsn);
            pVsn = Integer.valueOf(pkgVsn);
        } else {
            rVsn = Integer.valueOf(reqVsn.substring(0, rIdx));
            pVsn = Integer.valueOf(pkgVsn.substring(0, pIdx));
        }
        if (pVsn > rVsn) {
            return true;
        } else if (pVsn == rVsn) {
            if (rIdx != -1 && pIdx != -1) {
                return versionMatch(reqVsn.substring(rIdx + 1), pkgVsn.substring(pIdx + 1));
            }
            return true;
        }
        return false;
    }

    private static File getSiteFile(String pkgUrl) {
        String propsName = pkgUrl.substring(pkgUrl.lastIndexOf("/") + 1, pkgUrl.indexOf(".jar")) + "-site.properties";
        if (GlobalProps.DEBUG) {
            System.out.println("propsName: " + propsName);
        }
        String lzPath = GlobalProps.getHomeDir() + File.separator + "var" + File.separator + "package" + File.separator + "L10n" + File.separator + propsName;
        return new File(lzPath);
    }

    private static String getManifestProperty(String pkgUrl, String propName) {
        String value = null;
        try {
            URL jarURL = new URL("jar:" + pkgUrl + "!/");
            JarURLConnection juc = (JarURLConnection) jarURL.openConnection();
            Manifest man = juc.getManifest();
            if (man != null) {
                value = man.getMainAttributes().getValue(propName);
            }
        } catch (Exception e) {
            if (GlobalProps.DEBUG) {
                e.printStackTrace();
            }
        }
        return value;
    }

    private static InputStream getDataStream(String pkgUrl, String dataPath) {
        try {
            URL jarURL = new URL("jar:" + pkgUrl + "!/" + dataPath);
            JarURLConnection juc = (JarURLConnection) jarURL.openConnection();
            return juc.getInputStream();
        } catch (Exception e) {
            if (GlobalProps.DEBUG) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static void installFile(InputStream in, String instPath, String fileName, boolean update) {
        instPath = instPath.replaceAll("/", File.separator);
        String path = GlobalProps.getHomeDir() + File.separator + instPath;
        try {
            File instDir = new File(path);
            if (!instDir.isDirectory()) {
                instDir.mkdirs();
            }
            byte[] buf = new byte[1024];
            File instFile = new File(path + File.separator + fileName);
            if (update && instFile.exists()) {
                instFile.delete();
            }
            FileOutputStream fileOut = new FileOutputStream(instFile);
            int read = 0;
            while ((read = in.read(buf)) != -1) {
                fileOut.write(buf, 0, read);
            }
            in.close();
            fileOut.close();
        } catch (IOException ioe) {
            if (GlobalProps.DEBUG) {
                ioe.printStackTrace();
            }
        }
    }

    private static void mergeFile(InputStream in, String instPath, String fileName, boolean incrOnly) {
        ConfigService cfgSvc = Activator.cfgSvc;
        String fullPath = GlobalProps.getHomeDir() + File.separator + instPath + File.separator + fileName;
        try {
            Document oldDoc = cfgSvc.getConfigDocFromPath(fullPath);
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile("//client[@name='Console']");
            Object result = expr.evaluate(oldDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            Node node = nodes.item(0);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) node;
                if (el.hasAttribute("version")) {
                    String oldVerStr = el.getAttribute("version");
                    int ver = Integer.parseInt(oldVerStr) + 1;
                    el.setAttribute("version", String.valueOf(ver));
                } else {
                    if (GlobalProps.DEBUG) {
                        System.out.println("swingui.xml parse error - no version attribute");
                    }
                }
            }
            if (!incrOnly) {
                Document newDoc = cfgSvc.getDocFromStream(in);
                expr = xpath.compile("//menu");
                Object newRes = expr.evaluate(newDoc, XPathConstants.NODESET);
                NodeList mnodes = (NodeList) newRes;
                for (int i = 0; i < mnodes.getLength(); i++) {
                    Node mnode = mnodes.item(i);
                    if (mnode.getNodeType() == Node.ELEMENT_NODE) {
                        Element mel = (Element) mnode;
                        String mname = mel.getAttribute("name");
                        expr = xpath.compile("//menu[@name='" + mname + "']");
                        Object oldRes = expr.evaluate(oldDoc, XPathConstants.NODESET);
                        NodeList omnodes = (NodeList) oldRes;
                        if (omnodes.getLength() == 0) {
                            expr = xpath.compile("//menus");
                            Object oldResadd = expr.evaluate(oldDoc, XPathConstants.NODESET);
                            NodeList oldmnodes = (NodeList) oldResadd;
                            Node menusNode = oldmnodes.item(0);
                            Node newMenu = oldDoc.importNode(mel, true);
                            menusNode.appendChild(newMenu);
                        } else {
                            XPathExpression exp4 = xpath.compile("//menu[@name='" + mname + "']/items");
                            Object oldOne = exp4.evaluate(oldDoc, XPathConstants.NODESET);
                            NodeList oldNodes = (NodeList) oldOne;
                            Node insertNode = oldNodes.item(0);
                            XPathExpression exp5 = xpath.compile("//menu[@name='" + mname + "']/items/*");
                            Object newOne = exp5.evaluate(newDoc, XPathConstants.NODESET);
                            NodeList newNodes = (NodeList) newOne;
                            for (int j = 0; j < newNodes.getLength(); j++) {
                                Node anode = newNodes.item(j);
                                if (anode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element addEl = (Element) anode;
                                    Node newChild = oldDoc.importNode(addEl, true);
                                    insertNode.appendChild(newChild);
                                }
                            }
                        }
                    } else {
                        if (GlobalProps.DEBUG) {
                            System.out.println("parse error - MNode not element");
                        }
                    }
                }
            }
            BufferedWriter out = new BufferedWriter(new FileWriter(fullPath));
            cfgSvc.writeDocToStream(oldDoc, out);
        } catch (Exception e) {
            if (GlobalProps.DEBUG) {
                System.out.println("ouch ouch");
                e.printStackTrace();
            }
        }
    }

    private static class AnnBundle {

        public static final int OK = 0;

        public static final int INSTALLED = 1;

        public static final int FAILED = 2;

        public Bundle bundle = null;

        public int status = OK;

        public String error = null;

        public AnnBundle() {
        }
    }
}
