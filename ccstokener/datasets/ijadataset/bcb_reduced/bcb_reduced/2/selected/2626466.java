package org.xito.launcher.web;

import java.net.*;
import java.io.*;
import org.w3c.dom.*;
import org.w3c.tidy.*;
import org.xito.launcher.*;

/**
 *
 * @author DRICHAN
 */
public class WebDesc extends BaseLaunchDesc {

    private String address;

    private boolean new_browser_flag;

    public WebDesc() {
        super();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String adr) {
        address = adr;
    }

    public boolean useNewBrowser() {
        return new_browser_flag;
    }

    public void setUseNewBrowser(boolean b) {
        new_browser_flag = b;
    }

    public void initializeWebInfo() throws MalformedURLException, IOException, DOMException {
        Tidy tidy = new Tidy();
        URL url = new URL(address);
        InputStream in = url.openConnection().getInputStream();
        Document doc = tidy.parseDOM(in, null);
        Element e = doc.getDocumentElement();
        String title = null;
        if (e != null && e.hasChildNodes()) {
            NodeList children = e.getElementsByTagName("title");
            if (children != null) {
                for (int i = 0; i < children.getLength(); i++) {
                    try {
                        Element childE = (Element) children.item(i);
                        if (childE.getTagName().equals("title")) {
                            NodeList titleChildren = childE.getChildNodes();
                            for (int n = 0; n < titleChildren.getLength(); n++) {
                                if (titleChildren.item(n).getNodeType() == childE.TEXT_NODE) {
                                    title = titleChildren.item(n).getNodeValue();
                                }
                            }
                        }
                    } catch (Exception exp) {
                        exp.printStackTrace();
                    }
                }
            }
        }
        if (title == null || title.equals("")) {
            throw new DOMException(DOMException.NOT_FOUND_ERR, "no title found");
        } else {
            setTitle(title);
        }
    }
}
