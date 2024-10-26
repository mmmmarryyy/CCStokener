package oagrc.portal.view.security;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSetBase;
import org.apache.commons.digester.xmlrules.DigesterLoader;
import org.apache.commons.logging.*;
import org.xml.sax.SAXException;

/**
 * Parser para leer la configuraci�n desde fichero: jsf-security-config.xml
 *
 * @copyright Copyright (c) 2007 OAGRC
 * @author Diego Alcaraz Garc�a
 * @version 1.0, Enero 2007
 */
public class SecurityParser implements Serializable {

    private SecurityApplication securityApplication;

    public SecurityParser() {
        this(null);
    }

    private URL getDigesterSetup(FacesContext fctx) throws IOException {
        URL url = null;
        ExternalContext exctx = fctx != null ? fctx.getExternalContext() : null;
        String _facesSecurityConfig = "security-digester.xml";
        if (fctx.getCurrentInstance() != null) {
            String requestPathTranslated = ((HttpServletRequest) exctx.getRequest()).getPathTranslated();
            String requestPage = ((HttpServletRequest) exctx.getRequest()).getPathInfo();
            String requestPageOsTranslated = requestPage.replace('/', File.separatorChar);
            String public_html_location = requestPathTranslated.substring(0, requestPathTranslated.indexOf(requestPageOsTranslated));
            url = new URL("file:" + public_html_location + File.separator + "WEB-INF" + File.separator + _facesSecurityConfig);
            if (url == null) {
                url = getFacesSecurityConfigFromClasspath(_facesSecurityConfig);
            }
            if (url == null) {
                throw new FileNotFoundException("No encuentro el fichero digester de seguridad: " + _facesSecurityConfig);
            }
        } else {
            url = getFacesSecurityConfigFromClasspath(_facesSecurityConfig);
        }
        return url;
    }

    public SecurityParser(FacesContext fctx) {
        try {
            Digester digester = DigesterLoader.createDigester(getDigesterSetup(fctx));
            securityApplication = parseSecurityXMLFile(digester, fctx);
        } catch (MalformedURLException ue) {
            ue.getMessage();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Lee el fichero XML de configuraci�n de seguridad y
     * devuelve un objeto SecurityApplication
     */
    private SecurityApplication parseSecurityXMLFile(Digester d, FacesContext fctx) throws IOException, SAXException {
        URL url = null;
        ExternalContext exctx = fctx != null ? fctx.getExternalContext() : null;
        String _facesSecurityConfig = "security-config.xml";
        if (fctx.getCurrentInstance() != null) {
            String requestPathTranslated = ((HttpServletRequest) exctx.getRequest()).getPathTranslated();
            String requestPage = ((HttpServletRequest) exctx.getRequest()).getPathInfo();
            String requestPageOsTranslated = requestPage.replace('/', File.separatorChar);
            String public_html_location = requestPathTranslated.substring(0, requestPathTranslated.indexOf(requestPageOsTranslated));
            url = new URL("file:" + public_html_location + File.separator + "WEB-INF" + File.separator + _facesSecurityConfig);
            if (url == null) {
                url = getFacesSecurityConfigFromClasspath(_facesSecurityConfig);
            }
            if (url == null) {
                throw new FileNotFoundException("No encuentro el fichero de configuraci�n de seguridad: " + _facesSecurityConfig);
            }
        } else {
            url = getFacesSecurityConfigFromClasspath(_facesSecurityConfig);
        }
        return (SecurityApplication) d.parse(url.openStream());
    }

    private URL getFacesSecurityConfigFromClasspath(String config) {
        return Thread.currentThread().getContextClassLoader().getResource(config);
    }

    public SecurityApplication getSecurityApplication() {
        return securityApplication;
    }

    /**
     * Clase Interna DefineSecurityRuleSet contiene las DigesterRules  
     * para hacer parse del fichero jsf-security-config
     */
    class DefineSecurityRuleSet extends RuleSetBase {

        public void addRuleInstances(Digester d) {
            d.addObjectCreate("security-application", SecurityApplication.class);
            d.addObjectCreate("securtiy-application/security-setup-info", SecurityConfig.class);
            d.addBeanPropertySetter("security-application/security-setup-info/logon-view-id", "loginViewId");
            d.addBeanPropertySetter("security-application/security-setup-info/logon-cert-view-id", "loginCertViewId");
            d.addSetNext("security-application/security-setup-info", "addSecurityConfig");
            d.addObjectCreate("security-application/security-page", SecurityPage.class);
            d.addBeanPropertySetter("security-application/security-page/page-name", "pageName");
            d.addCallMethod("security-application/security-page", "setRequiresCertificate", 1);
            d.addCallParam("security-application/security-page", 0, "requires-certificate");
            d.addSetNext("security-application/security-page", "addSecurityPage");
        }
    }
}
