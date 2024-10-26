package issrg.test.pmi;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.naming.directory.Attributes;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import issrg.pba.rbac.CustomisePERMIS;
import issrg.pba.rbac.LDAPDNPrincipal;
import issrg.utils.repository.VirtualRepository;

/**
 * This class is for testing various PMI setups. The PMI is represented by a
 * single XML where all subjects are given tokens by issuers. The repository
 * can return ParsedTokens for individual subjects. PERMIS can use these tokens
 * to arrive at its decisions, without having to configure the LDAP and PKI.
 */
public class PMIXMLRepository extends VirtualRepository {

    public static final String PMI_XML_PROTOCOL = "xml";

    public PMIXMLRepository(String url) throws MalformedURLException, IOException {
        if (url == null || !url.startsWith(PMI_XML_PROTOCOL + ":")) {
            throw new MalformedURLException("The Repository URL should start with " + PMI_XML_PROTOCOL + ", but " + url + " was found");
        }
        url = url.substring(PMI_XML_PROTOCOL.length() + 1);
        try {
            populateRepository(this, new URL(url).openStream(), CustomisePERMIS.getAttributeCertificateAttribute());
        } catch (IOException ioe) {
            throw ioe;
        }
    }

    public Attributes getAllAttributes(java.security.Principal DN) {
        Attributes attrs = super.getAllAttributes(DN);
        return attrs;
    }

    /**
   * This method populates the given VirtualRepository with the contents of the
   * XML in the given InputStream.
   */
    public static void populateRepository(VirtualRepository that, InputStream is, String acAttrName) throws IOException {
        Element repository;
        try {
            repository = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is).getDocumentElement();
        } catch (IOException ioe) {
            throw ioe;
        } catch (Throwable th) {
            throw (IOException) new IOException("Couldn't parse the repository XML").initCause(th);
        }
        NodeList entries = repository.getElementsByTagName("entry");
        for (int i = entries.getLength(); i-- > 0; ) {
            Element entry = (Element) entries.item(i);
            String dn = entry.getAttribute("dn");
            try {
                dn = new LDAPDNPrincipal(dn).getName();
            } catch (Throwable th) {
            }
            NodeList values = entry.getElementsByTagName("attribute");
            for (int j = values.getLength(); j-- > 0; ) {
                Element value = (Element) values.item(j);
                String attrName = value.getAttribute("type");
                if (attrName != null && attrName.equals("attributeCertificateAttribute")) {
                    that.populate(dn, acAttrName, value);
                }
            }
        }
    }
}
