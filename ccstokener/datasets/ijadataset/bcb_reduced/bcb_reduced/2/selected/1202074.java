package org.creativecommons.api;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;
import org.jaxen.JaxenException;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 *  A wrapper around Creative Commons REST web services.
 *
 * @author Nathan R. Yergler
 * @author Creative Commons
 * @version 0.0.1
 */
public class CcRest {

    private String rest_root = "http://api.creativecommons.org/rest";

    private Document license_doc = null;

    private SAXBuilder parser = new SAXBuilder();

    private List classes = new Vector();

    private List fields = new Vector();

    /**
	 * Constructs a new instance with the default web services root.
	 *
	 */
    public CcRest() {
        super();
    }

    /**
	 * Constructs a new instance, explicitly specifying the web service root URL.
	 * 
	 * @param rest_root The root URL for the web service wrapper.
	 */
    public CcRest(String rest_root) {
        super();
        this.rest_root = rest_root;
    }

    /**
	 * Returns the id for a particular LicenseClass label.  Returns an
	 * empty string if no match is found.
	 * 
	 * @param class_label The LicenseClass label to find.
	 * @return Returns a String containing the License class ID if the label 
	 * 			is found; if not found, returns an empty string.
	 * 
	 * @see LicenseClass
	 * 
	 */
    public String getLicenseId(String class_label) {
        for (int i = 0; i < this.classes.size(); i++) {
            if (((LicenseClass) this.classes.get(i)).getLabel().equals(class_label)) {
                return ((LicenseClass) this.classes.get(i)).getIdentifier();
            }
        }
        return "";
    }

    /**
	 * Queries the web service for the available license classes.
	 * 
	 * @param language The language to request labels and description strings in.
	 * @return Returns a Collection of LicenseClass objects.
	 * 
	 * @see Collection
	 * @see LicenseClass
	 * 
	 */
    public Collection licenseClasses(String language) {
        JDOMXPath xp_LicenseClasses;
        JDOMXPath xp_LicenseID;
        Document classDoc;
        URL classUrl;
        List results = null;
        try {
            xp_LicenseClasses = new JDOMXPath("//licenses/license");
            xp_LicenseID = new JDOMXPath("@id");
        } catch (JaxenException jaxen_e) {
            return null;
        }
        try {
            classUrl = new URL(this.rest_root + "/classes");
        } catch (Exception e) {
            return null;
        }
        try {
            classDoc = this.parser.build(classUrl);
        } catch (JDOMException jdom_e) {
            return null;
        } catch (IOException io_e) {
            return null;
        }
        try {
            results = xp_LicenseClasses.selectNodes(classDoc);
        } catch (JaxenException jaxen_e) {
            return null;
        }
        classes.clear();
        for (int i = 0; i < results.size(); i++) {
            Element license = (Element) results.get(i);
            try {
                LicenseClass lc = new LicenseClass(((Attribute) xp_LicenseID.selectSingleNode(license)).getValue(), license.getText());
                classes.add(lc);
            } catch (JaxenException jaxen_e) {
                return null;
            }
        }
        return classes;
    }

    /**
	 * Queries the web service for a set of fields for a particular license class.
	 * 
	 * @param license A String specifying the LicenseClass identifier to 
	 * 			retrieve fields for.
	 * @return A Collection of LicenseField objects.
	 * 
	 * @see LicenseClass
	 * @see LicenseField
	 *  
	 */
    public Collection fields(String license) {
        JDOMXPath xp_LicenseField;
        JDOMXPath xp_LicenseID;
        JDOMXPath xp_FieldType;
        JDOMXPath xp_Description;
        JDOMXPath xp_Label;
        JDOMXPath xp_Enum;
        Document fieldDoc;
        URL classUrl;
        List results = null;
        List enumOptions = null;
        try {
            xp_LicenseField = new JDOMXPath("//field");
            xp_LicenseID = new JDOMXPath("@id");
            xp_Description = new JDOMXPath("description");
            xp_Label = new JDOMXPath("label");
            xp_FieldType = new JDOMXPath("type");
            xp_Enum = new JDOMXPath("enum");
        } catch (JaxenException e) {
            return null;
        }
        try {
            classUrl = new URL(this.rest_root + "/license/" + license);
        } catch (Exception err) {
            return null;
        }
        try {
            fieldDoc = this.parser.build(classUrl);
        } catch (JDOMException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        this.fields.clear();
        try {
            results = xp_LicenseField.selectNodes(fieldDoc);
        } catch (JaxenException e) {
            return null;
        }
        for (int i = 0; i < results.size(); i++) {
            Element field = (Element) results.get(i);
            try {
                LicenseField f = new LicenseField(((Attribute) xp_LicenseID.selectSingleNode(field)).getValue(), ((Element) xp_Label.selectSingleNode(field)).getText());
                f.setDescription(((Element) xp_Description.selectSingleNode(field)).getText());
                f.setType(((Element) xp_FieldType.selectSingleNode(field)).getText());
                enumOptions = xp_Enum.selectNodes(field);
                for (int j = 0; j < enumOptions.size(); j++) {
                    String id = ((Attribute) xp_LicenseID.selectSingleNode(enumOptions.get(j))).getValue();
                    String label = ((Element) xp_Label.selectSingleNode(enumOptions.get(j))).getText();
                    f.getEnum().put(id, label);
                }
                this.fields.add(f);
            } catch (JaxenException e) {
                return null;
            }
        }
        return fields;
    }

    /**
	 * Passes a set of "answers" to the web service and retrieves a license.
	 * 
	 * @param licenseId The identifier of the license class being requested.
	 * @param answers A Map containing the answers to the license fields;
	 * 			each key is the identifier of a LicenseField, with the value
	 * 			containing the user-supplied answer.
	 * @param lang The language to request localized elements in.
	 * 
	 * @throws IOException
	 * 
	 * @see LicenseClass
	 * @see Map
	 */
    public void issue(String licenseId, Map answers, String lang) throws IOException {
        String issueUrl = this.rest_root + "/license/" + licenseId + "/issue";
        String answer_doc = "<answers>\n<license-" + licenseId + ">";
        Iterator keys = answers.keySet().iterator();
        try {
            String current = (String) keys.next();
            while (true) {
                answer_doc += "<" + current + ">" + (String) answers.get(current) + "</" + current + ">\n";
                current = (String) keys.next();
            }
        } catch (NoSuchElementException e) {
        }
        answer_doc += "</license-" + licenseId + ">\n</answers>\n";
        String post_data;
        try {
            post_data = URLEncoder.encode("answers", "UTF-8") + "=" + URLEncoder.encode(answer_doc, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return;
        }
        URL post_url;
        try {
            post_url = new URL(issueUrl);
        } catch (MalformedURLException e) {
            return;
        }
        URLConnection conn = post_url.openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(post_data);
        wr.flush();
        try {
            this.license_doc = this.parser.build(conn.getInputStream());
        } catch (JDOMException e) {
            System.out.print("Danger Will Robinson, Danger!");
        }
        return;
    }

    /**
	 * Retrieves the URI for the license issued.
	 * 
	 * @return A String containing the URI for the license issued.
	 */
    public String getLicenseUrl() {
        try {
            JDOMXPath xp_LicenseName = new JDOMXPath("//result/license-uri");
            return ((Element) xp_LicenseName.selectSingleNode(this.license_doc)).getText();
        } catch (JaxenException err) {
            return null;
        }
    }

    /**
	 * Retrieves the human readable name for the license issued.
	 * 
	 * @return A String containing the license name.
	 */
    public String getLicenseName() {
        try {
            JDOMXPath xp_LicenseName = new JDOMXPath("//result/license-name");
            return ((Element) xp_LicenseName.selectSingleNode(this.license_doc)).getText();
        } catch (JaxenException err) {
            return null;
        }
    }

    /**
	 * Retrieves the human readable name for the license issued.
	 * 
	 * @return A String containing the license name.
	 */
    public String getLicenseHTML() {
        try {
            XMLOutputter outputter = new XMLOutputter();
            outputter.setFormat(Format.getPrettyFormat());
            StringWriter w = new StringWriter();
            outputter.output(((Element) new JDOMXPath("//result/html").selectSingleNode(this.license_doc)).getContent(), w);
            return w.toString();
        } catch (Exception err) {
            return null;
        }
    }
}
