package com.skruk.elvis.doc;

import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.IterationTag;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.ServletRequest;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.*;
import com.skruk.elvis.db.xml.DbEngine;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;
import org.xmldb.api.*;

/**
 *  Klasa odpowiadająca znacznikowi <code><i>doc</i>:xslt</code> z TagLib <tt>doc</tt> -
 *  odpowiedzialna za przekształcanie XSLT  
 */
public class XsltTag extends BodyTagSupport {

    /** 
		 * Atrybut określający dokument XML
     */
    private String xml;

    /**
		 * Atrybut określający arkusz XSL
     */
    private String xsl;

    /**
		 *	Obiekt odpowiedzialny za tworzenie obiektów biorących udział w przekształceniach
		 */
    private javax.xml.transform.TransformerFactory tFactory = null;

    /**
		 *	Obiekt reprezentujący źródło XML
		 */
    private javax.xml.transform.Source xmlSource = null;

    /**
		 *	Obiekt reprezentujący arkusz XSL
		 */
    private javax.xml.transform.Source xslSource = null;

    /**
		 *	Obiekt odpowiedzialny za przekształcenia XST
		 */
    private javax.xml.transform.Transformer transformer = null;

    /**
		 * Atrybut określający kolekcję w bazie danych XML
     */
    private String xmlcol = null;

    /**
		 * Konstruktor
		 */
    public XsltTag() {
        super();
        try {
            tFactory = javax.xml.transform.TransformerFactory.newInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     *	Metoda odpowiedzialna za generowanie początku znacznika  
     */
    public void otherDoStartTagOperations() {
        try {
            xmlSource = this.getSourceXml();
            xslSource = this.getSourceXsl();
            transformer = tFactory.newTransformer(xslSource);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     *	Określa czy ma zostać wygenerowana zawartość znacznika  
     *  @return Czy ma zostać wygenerowana zawartość znacznika?
     */
    public boolean theBodyShouldBeEvaluated() {
        return true;
    }

    /**
     *	Metoda generuje końcową część znacznika 
     */
    public void otherDoEndTagOperations() {
        try {
            StringWriter sw = new StringWriter();
            javax.xml.transform.stream.StreamResult sr = new javax.xml.transform.stream.StreamResult(sw);
            this.transformer.transform(xmlSource, sr);
            this.pageContext.getOut().write(sw.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     *	Określa czy ma zostać wygenerowana reszta strony po znaczniku  
     *  @return Czy ma zostać wygenerowana reszta strony po znaczniku ?
     */
    public boolean shouldEvaluateRestOfPageAfterEndTag() {
        return true;
    }

    /** .//GEN-BEGIN:doStartTag
     *
     * This method is called when the JSP engine encounters the start tag,
     * after the attributes are processed.
     * Scripting variables (if any) have their values set here.
     * @return EVAL_BODY_INCLUDE if the JSP engine should evaluate the tag body, otherwise return SKIP_BODY.
     * This method is automatically generated. Do not modify this method.
     * Instead, modify the methods that this method calls.
     *
     */
    public int doStartTag() throws JspException, JspException {
        otherDoStartTagOperations();
        if (theBodyShouldBeEvaluated()) {
            return EVAL_BODY_BUFFERED;
        } else {
            return SKIP_BODY;
        }
    }

    /** .//GEN-BEGIN:doEndTag
     *
     *
     * This method is called after the JSP engine finished processing the tag.
     * @return EVAL_PAGE if the JSP engine should continue evaluating the JSP page, otherwise return SKIP_PAGE.
     * This method is automatically generated. Do not modify this method.
     * Instead, modify the methods that this method calls.
     *
     */
    public int doEndTag() throws JspException, JspException {
        otherDoEndTagOperations();
        if (shouldEvaluateRestOfPageAfterEndTag()) {
            return EVAL_PAGE;
        } else {
            return SKIP_PAGE;
        }
    }

    /**
     *  Zwraca plik związany z podanym jako parametr tagu źródłem XML
     */
    protected javax.xml.transform.Source getSourceXml() throws java.net.MalformedURLException, org.jdom.JDOMException, java.io.IOException, org.xmldb.api.base.XMLDBException, com.skruk.elvis.db.DbException {
        java.net.URL url = null;
        javax.xml.transform.Source source = null;
        if (this.getXmlcol() == null) {
            if (this.getXml().startsWith("http")) {
                url = new java.net.URL(this.getXml());
                org.jdom.Document doc = new org.jdom.input.SAXBuilder().build(url);
                source = new javax.xml.transform.dom.DOMSource(new org.jdom.output.DOMOutputter().output(doc));
            } else {
                StringBuffer path = new StringBuffer(this.pageContext.getServletContext().getInitParameter("installDir"));
                path.append("/").append(this.getXml());
                url = new java.io.File(path.toString()).toURL();
                source = new javax.xml.transform.stream.StreamSource(url.openStream());
            }
        } else {
            DbEngine dbe = DbEngine.createInstance(this.pageContext.getServletContext());
            dbe.loadCollection(this.getXmlcol());
            XMLResource doc = dbe.getDocument(this.getXml());
            source = new javax.xml.transform.dom.DOMSource(doc.getContentAsDOM());
        }
        return source;
    }

    /**
     *  Zwraca plik zwi�zany z podanym jako parametr tagu źródłem XML
     */
    protected javax.xml.transform.Source getSourceXsl() throws java.net.MalformedURLException, org.jdom.JDOMException, java.io.IOException, org.xmldb.api.base.XMLDBException, com.skruk.elvis.db.DbException {
        java.net.URL url = null;
        javax.xml.transform.Source source = null;
        if (this.getXml().startsWith("http")) {
            url = new java.net.URL(this.getXsl());
            org.jdom.Document doc = new org.jdom.input.SAXBuilder().build(url);
            source = new javax.xml.transform.dom.DOMSource(new org.jdom.output.DOMOutputter().output(doc));
        } else {
            StringBuffer path = new StringBuffer(this.pageContext.getServletContext().getInitParameter("installDir"));
            path.append("/").append(this.getXsl());
            url = new java.io.File(path.toString()).toURL();
            source = new javax.xml.transform.stream.StreamSource(url.openStream());
        }
        return source;
    }

    /** .
     *	Generuje zawartość znacznika
		 * @param out Obiekt do którego pisana jest zawartość
		 * @param bodyContent Obiekt reprezentujący zawartość znacznika głównego
     */
    public void writeTagBodyContent(JspWriter out, BodyContent bodyContent) throws IOException {
        bodyContent.writeOut(out);
        bodyContent.clearBody();
    }

    /** .
     *
     * Handles exception from processing the body content.
     *
     */
    public void handleBodyContentException(Exception ex) throws JspException {
        throw new JspException("error in XsltTag: " + ex);
    }

    /** .
     *
     *
     * This method is called after the JSP engine processes the body content of the tag.
     * @return EVAL_BODY_AGAIN if the JSP engine should evaluate the tag body again, otherwise return SKIP_BODY.
     * This method is automatically generated. Do not modify this method.
     * Instead, modify the methods that this method calls.
     *
     */
    public int doAfterBody() throws JspException {
        try {
            BodyContent bodyContent = getBodyContent();
            JspWriter out = bodyContent.getEnclosingWriter();
            writeTagBodyContent(out, bodyContent);
        } catch (Exception ex) {
            handleBodyContentException(ex);
        }
        if (theBodyShouldBeEvaluatedAgain()) {
            return EVAL_BODY_AGAIN;
        } else {
            return SKIP_BODY;
        }
    }

    /**
     * Określa czy zawartość znacznika ma zostać wygenerowana jeszcze raz
		 * @return Czy zawartość znacznika ma zostać wygenerowana jeszcze raz ??
     */
    public boolean theBodyShouldBeEvaluatedAgain() {
        return false;
    }

    /** 
			************************* metody get i set dostępu do pół znacznika *************************** 
			*/
    public javax.xml.transform.Transformer getTransformer() {
        return this.transformer;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String value) {
        xml = value;
    }

    public String getXsl() {
        return xsl;
    }

    public void setXsl(String value) {
        xsl = value;
    }

    public String getXmlcol() {
        return xmlcol;
    }

    public void setXmlcol(String value) {
        xmlcol = value;
    }
}
