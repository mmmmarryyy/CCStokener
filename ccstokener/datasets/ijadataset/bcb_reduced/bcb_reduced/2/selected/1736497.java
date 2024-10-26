package org.foafrealm.beans;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashSet;
import org.foafrealm.mfb.NamespaceMap;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * @author skruk
 *
 */
public class RdfHelper {

    public static final RdfHelper instance = new RdfHelper();

    protected RdfHelper() {
    }

    /**
	 * @return
	 * @throws IOException 
	 */
    public byte[] readSource(String source) throws IOException {
        byte[] result = null;
        URL urlsource = new URL(source);
        URLConnection consource = urlsource.openConnection();
        InputStream is = consource.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffor = new byte[102400];
        int length = -1;
        while ((length = is.read(buffor)) > 0) {
            baos.write(buffor, 0, length);
        }
        result = baos.toByteArray();
        baos.close();
        is.close();
        return result;
    }

    /**
   * Generates Jena Model out of output stream
   * @param baos
   * @return
   */
    public Model loadModel(byte[] baos, String type, Model input_model) {
        input_model.read(new ByteArrayInputStream(baos), "", type);
        return input_model;
    }

    /**
   * Lists all properties that can be used to generate this multifaceted browsing UI
   * 
   * 
   * @return
   */
    public Collection<Property> listProperties(Model input_model) {
        Collection<Property> result = new HashSet<Property>();
        StmtIterator stmtit = input_model.listStatements();
        while (stmtit.hasNext()) {
            Statement stmt = stmtit.nextStatement();
            Property p = stmt.getPredicate();
            result.add(p);
        }
        return result;
    }

    public String getUniqueId(String uri) {
        int pos = 0;
        String ns;
        String lname;
        String id;
        if (uri.indexOf("#") > -1) {
            pos = uri.indexOf("#");
        } else {
            pos = uri.lastIndexOf("/");
            if (pos == uri.length() - 1) {
                pos = uri.indexOf("/", 9);
                uri = uri.substring(0, pos + 1) + uri.substring(pos + 1, uri.length() - 1).replace("/", "_");
            }
        }
        ns = uri.substring(0, pos + 1);
        lname = uri.substring(pos + 1);
        id = NamespaceMap.get(ns);
        return id + "_" + lname;
    }

    public String getLabel(Resource resClass) {
        String sLabel = "";
        Statement stmtRdfsLabel = resClass.getProperty(RDFS.label);
        Statement stmtDbLabel = resClass.getProperty(DC.title);
        if (stmtRdfsLabel != null && stmtRdfsLabel.getString() != null && !"".equals(stmtRdfsLabel.getString())) {
            sLabel = stmtRdfsLabel.getString();
        } else if (stmtDbLabel != null && stmtDbLabel.getString() != null && !"".equals(stmtDbLabel.getString())) {
            sLabel = stmtDbLabel.getString();
        } else {
            sLabel = resClass.getLocalName();
        }
        return sLabel;
    }
}
