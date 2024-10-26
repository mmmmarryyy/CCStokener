package org.corrib.s3b.sscf.beans;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;
import org.corrib.s3b.sscf.manage.XfoafSscfResource;
import org.foafrealm.manage.Person;

/**
 * 
 * 
 * @author Sebastian Ryszard Kruk,
 * @created 15.11.2005
 */
public class ResourceWrapper {

    private static final Logger log = Logger.getLogger(ResourceWrapper.class.getName());

    XfoafSscfResource resource = null;

    Object bookRes = null;

    String uri = null;

    String label = null;

    String authors = null;

    String title = null;

    String serviceAddr = null;

    String[] allInOne = null;

    public ResourceWrapper(XfoafSscfResource _resource) {
        this(_resource, null, false);
    }

    /**
	 * 
	 */
    @SuppressWarnings("unchecked")
    public ResourceWrapper(XfoafSscfResource _resource, String _serviceAddr, boolean isBookmark) {
        this.resource = _resource;
        this.uri = resource.getURI().toString();
        this.label = resource.getLabel();
        this.serviceAddr = _serviceAddr;
        if (isBookmark) {
            if (serviceAddr == null) {
                InputStream is = Context.class.getClassLoader().getResourceAsStream("foafrealm_context.properties");
                Properties prop = new Properties();
                try {
                    prop.load(is);
                    is.close();
                } catch (Exception ioex) {
                    log.warning("Context - error loading preferences: foafrealm_context");
                }
                serviceAddr = prop.getProperty("default_server_address");
            }
            String servletUrl = null;
            try {
                servletUrl = uri.substring(0, uri.indexOf("resource/"));
                servletUrl += "servlet/getBook?uri=" + uri;
            } catch (Exception e2) {
                servletUrl = serviceAddr + "servlet/getBook?uri=" + uri;
            }
            try {
                URL url = new URL(servletUrl);
                HttpURLConnection huc = (HttpURLConnection) url.openConnection();
                huc.connect();
                BufferedReader br = new BufferedReader(new InputStreamReader(huc.getInputStream()));
                String getLine = br.readLine();
                if (getLine != null && !"".equals(getLine.trim())) {
                    allInOne = getLine.split(";;");
                    if (allInOne != null && allInOne.length > 1) {
                        authors = allInOne[0];
                        title = allInOne[1];
                    }
                }
                br.close();
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else {
            try {
                Class clBookFactory = Class.forName("org.jeromedl.marcont.book.BookFactory");
                Method mLoadBook = clBookFactory.getMethod("loadBook", new Class[] { String.class });
                Class clBook = Class.forName("org.jeromedl.marcont.book.BookInterface");
                Method mGetPersons = clBook.getMethod("getPersons", new Class[0]);
                Method mGetTitle = clBook.getMethod("getTitle", new Class[0]);
                bookRes = mLoadBook.invoke(null, new Object[] { this.uri });
                authors = getAuthorsAsString((Person[]) mGetPersons.invoke(bookRes, new Object[0]));
                title = (String) mGetTitle.invoke(bookRes, new Object[0]);
            } catch (Exception e) {
            }
        }
    }

    public String getLabel() {
        String _label = null;
        if (bookRes != null) {
            _label = authors + ":" + title;
        } else if (this.label != null && !"".equals(this.label)) {
            _label = this.label;
        } else if (this.uri.indexOf("subject?uri=") > 0) {
            _label = this.uri.substring(this.uri.indexOf("subject?uri=") + 12);
        } else {
            _label = this.uri;
        }
        return _label;
    }

    public String getAuthorsAsString(Person[] persons_) {
        StringBuilder sb = new StringBuilder();
        if (persons_ != null && persons_.length > 0) {
            for (Person _author : persons_) {
                sb.append(_author.getName());
                sb.append(", ");
            }
            sb.delete(sb.length() - 2, sb.length());
        } else {
            sb.append("No author found");
        }
        return sb.toString();
    }

    /**
	 * @return Returns the authors.
	 */
    public String getAuthors() {
        return authors;
    }

    /**
	 * @return Returns the title.
	 */
    public String getTitle() {
        return title;
    }
}
