package org.logitest.testlet;

import java.awt.Component;
import java.net.*;
import java.util.*;
import javax.swing.*;
import org.jdom.*;
import org.logitest.*;

/** A Testlet implementation which verifies all linked resources.

	@author Anthony Eden
*/
public class LinkTestlet extends Testlet {

    /** Default constructor. */
    public LinkTestlet() {
    }

    /** Test the given resource. 
	
		@param resource The resource
	*/
    public boolean test(Resource resource) throws Exception {
        Document document = resource.getDocument();
        URL context = new URL(resource.getURL());
        List passed = new ArrayList();
        List failed = new ArrayList();
        test(document.getRootElement(), context, passed, failed);
        if (failed.size() == 0) {
            setState(Test.PASSED);
            return true;
        } else {
            setState(Test.FAILED);
            return false;
        }
    }

    protected void test(Element element, URL context, List passed, List failed) {
        Iterator children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element) children.next();
            if (child.getName().equals("a")) {
                if (checkLink(context, child)) {
                    passed.add(child);
                } else {
                    failed.add(child);
                }
            }
            test(child, context, passed, failed);
        }
    }

    protected boolean checkLink(URL context, Element element) {
        Attribute att = element.getAttribute("href");
        if (att != null) {
            try {
                URL url = new URL(context, att.getValue());
                URLConnection conn = url.openConnection();
                conn.connect();
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    public Component getEditor() {
        return new JPanel();
    }

    protected Map getProperties() {
        if (properties == null) {
            properties = new HashMap();
            properties.put(DISPLAY_NAME, "Link Testlet");
            properties.put(DESCRIPTION, "Test for link validity.");
            properties.put(AUTHOR, "Anthony Eden");
            properties.put(VERSION, "1.0");
        }
        return properties;
    }

    private Map properties;
}
