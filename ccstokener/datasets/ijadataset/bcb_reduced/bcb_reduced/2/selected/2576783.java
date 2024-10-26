package gov.lanl.xmlsig;

import java.io.File;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.content.RetrievalMethod;
import org.apache.xml.security.samples.utils.resolver.OfflineResolver;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.SignedInfo;
import org.apache.xml.security.utils.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import sun.misc.BASE64Encoder;
import ORG.oclc.oai.util.OAIUtil;

public class VerifyExampleTest {

    String info = null;

    public String getVerificationInfo() {
        return info;
    }

    public String verify(Document doc) {
        boolean isVerified = false;
        long start = System.currentTimeMillis();
        StringBuffer sb = new StringBuffer();
        try {
            org.apache.xml.security.Init.init();
            Constants.setSignatureSpecNSprefix("dsig");
            Element sigElement = null;
            sb.append("<verifyinfo>");
            NodeList nodes = doc.getElementsByTagNameNS(org.apache.xml.security.utils.Constants.SignatureSpecNS, "Signature");
            if (nodes.getLength() != 0) {
                System.out.println("Found " + nodes.getLength() + " Signature  elements.");
                for (int i = 0; i < nodes.getLength(); i++) {
                    sigElement = (Element) nodes.item(i);
                    XMLSignature signature = new XMLSignature(sigElement, "");
                    KeyInfo ki = signature.getKeyInfo();
                    try {
                        SignedInfo xmlsiginfo = signature.getSignedInfo();
                        String ref = OAIUtil.xmlEncode(xmlsiginfo.item(0).getURI());
                        String digest = this.getDigest(xmlsiginfo);
                        sb.append("<sig url=\"" + ref + "\" digest=\"" + digest + "\"");
                        if (ki != null) {
                            if (ki.containsX509Data()) {
                                System.out.println("Could find a X509Data element in the  KeyInfo");
                            }
                            KeyInfo kinfo = signature.getKeyInfo();
                            X509Certificate cert = null;
                            if (kinfo.containsRetrievalMethod()) {
                                RetrievalMethod m = kinfo.itemRetrievalMethod(0);
                                URL url = new URL(m.getURI());
                                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                                cert = (X509Certificate) cf.generateCertificate(url.openStream());
                            } else {
                                cert = signature.getKeyInfo().getX509Certificate();
                            }
                            if (cert != null) {
                                isVerified = signature.checkSignatureValue(cert);
                                System.out.println("The XML signature is " + (isVerified ? "valid (good)" : "invalid !!!!! (bad)"));
                                if (isVerified) {
                                    sb.append(" status=\"0\"> ");
                                } else {
                                    sb.append(" status=\"1\" >");
                                }
                                sb.append(isVerified ? "valid (good)" : "invalid !!!!! (bad)");
                            } else {
                                System.out.println("Did not find a Certificate");
                                PublicKey pk = signature.getKeyInfo().getPublicKey();
                                if (pk != null) {
                                    isVerified = signature.checkSignatureValue(pk);
                                    if (isVerified) {
                                        sb.append(" status=\"0\" >");
                                    } else {
                                        sb.append(" status=\"1\" >");
                                    }
                                    System.out.println("The XML signature is " + (isVerified ? "valid (good)" : "invalid !!!!! (bad)"));
                                    sb.append(isVerified ? "valid (good)" : "invalid !!!!! (bad)");
                                } else {
                                    sb.append(" status=\"2\" >");
                                    sb.append("Did not find a public key");
                                    System.out.println("Did not find a public key, so I can't check the signature");
                                }
                            }
                        } else {
                            sb.append(" status=\"2\" >");
                            System.out.println("Did not find a KeyInfo");
                            sb.append("Did not find a KeyInfo");
                        }
                    } catch (Exception e) {
                        sb.append(" status=\"2\" >");
                        sb.append(OAIUtil.xmlEncode(e.getMessage()));
                    }
                    sb.append("</sig>");
                }
            }
            long end = System.currentTimeMillis();
            double elapsed = end - start;
            System.out.println("verified:" + elapsed);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            sb.append(OAIUtil.xmlEncode(e.getMessage()));
        }
        sb.append("</verifyinfo>");
        this.info = sb.toString();
        System.out.println(sb.toString());
        return sb.toString();
    }

    public static void main(String args[]) {
        org.apache.xml.security.Init.init();
        String signatureFileName = args[0];
        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setAttribute("http://xml.org/sax/features/namespaces", Boolean.TRUE);
        try {
            long start = System.currentTimeMillis();
            org.apache.xml.security.Init.init();
            File f = new File(signatureFileName);
            System.out.println("Verifying " + signatureFileName);
            javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
            org.w3c.dom.Document doc = db.parse(new java.io.FileInputStream(f));
            VerifyExampleTest vf = new VerifyExampleTest();
            vf.verify(doc);
            Constants.setSignatureSpecNSprefix("dsig");
            Element sigElement = null;
            NodeList nodes = doc.getElementsByTagNameNS(org.apache.xml.security.utils.Constants.SignatureSpecNS, "Signature");
            if (nodes.getLength() != 0) {
                System.out.println("Found " + nodes.getLength() + " Signature  elements.");
                for (int i = 0; i < nodes.getLength(); i++) {
                    sigElement = (Element) nodes.item(i);
                    XMLSignature signature = new XMLSignature(sigElement, "");
                    KeyInfo ki = signature.getKeyInfo();
                    signature.addResourceResolver(new OfflineResolver());
                    if (ki != null) {
                        if (ki.containsX509Data()) {
                            System.out.println("Could find a X509Data element in the  KeyInfo");
                        }
                        KeyInfo kinfo = signature.getKeyInfo();
                        X509Certificate cert = null;
                        if (kinfo.containsRetrievalMethod()) {
                            RetrievalMethod m = kinfo.itemRetrievalMethod(0);
                            URL url = new URL(m.getURI());
                            CertificateFactory cf = CertificateFactory.getInstance("X.509");
                            cert = (X509Certificate) cf.generateCertificate(url.openStream());
                        } else {
                            cert = signature.getKeyInfo().getX509Certificate();
                        }
                        if (cert != null) {
                            System.out.println("The XML signature is " + (signature.checkSignatureValue(cert) ? "valid (good)" : "invalid !!!!! (bad)"));
                        } else {
                            System.out.println("Did not find a Certificate");
                            PublicKey pk = signature.getKeyInfo().getPublicKey();
                            if (pk != null) {
                                System.out.println("The XML signatur is " + (signature.checkSignatureValue(pk) ? "valid (good)" : "invalid !!!!! (bad)"));
                            } else {
                                System.out.println("Did not find a public key, so I can't check the signature");
                            }
                        }
                    } else {
                        System.out.println("Did not find a KeyInfo");
                    }
                }
            }
            long end = System.currentTimeMillis();
            double elapsed = end - start;
            System.out.println("verified:" + elapsed);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**                                                                                                                  
     * Get digest value from Signature SignedInfo element                                         
     */
    public String getDigest(SignedInfo info) throws Exception {
        byte[] digest = info.item(0).getDigestValue();
        BASE64Encoder encoder = new BASE64Encoder();
        String coded = new String(encoder.encodeBuffer(digest));
        return coded.trim();
    }
}
