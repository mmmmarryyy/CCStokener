package com.atosorigin.nl.saml.cas.view;

import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.util.Loader;
import org.apache.xml.security.signature.XMLSignature;
import org.inspektr.common.ioc.annotation.NotNull;
import org.jasig.cas.authentication.Authentication;
import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.validation.Assertion;
import org.jasig.cas.web.view.AbstractCasView;
import org.opensaml.SAMLAssertion;
import org.opensaml.SAMLAttribute;
import org.opensaml.SAMLAttributeStatement;
import org.opensaml.SAMLAuthenticationStatement;
import org.opensaml.SAMLBinding;
import org.opensaml.SAMLException;
import org.opensaml.SAMLNameIdentifier;
import org.opensaml.SAMLResponse;
import org.opensaml.SAMLSOAPHTTPBinding;
import org.opensaml.SAMLSubject;
import org.opensaml.provider.SOAPHTTPBindingProvider;

/**
 * @author a108600
 * 
 */
public class SamlProxySuccessResponseView extends AbstractCasView {

    private static final String CAS_NAMESPACE = "http://www.jasig.org/cas";

    private static final QName XSD_STRING = new QName("http://www.w3.org/2001/XMLSchema", "string");

    /** The issuer, generally the hostname. */
    @NotNull
    private String issuer;

    @NotNull
    private String issuerKeyProperties;

    /** The amount of time in milliseconds this is valid for. */
    private long issueLength = 30000;

    /**
	 * 
	 */
    public SamlProxySuccessResponseView() {
        super();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void renderMergedOutputModel(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            final Assertion assertion = getAssertionFrom(model);
            final Authentication authentication = assertion.getChainedAuthentications().get(0);
            final Date currentDate = new Date();
            final Service service = assertion.getService();
            final String requestId = (String) model.get("requestId");
            final SAMLResponse samlResponse = new SAMLResponse(requestId, service.getId(), new ArrayList<Object>(), null);
            samlResponse.setIssueInstant(currentDate);
            samlResponse.setMinorVersion(0);
            final SAMLAssertion samlAssertion = new SAMLAssertion();
            samlAssertion.setIssueInstant(currentDate);
            samlAssertion.setIssuer(this.issuer);
            samlAssertion.setNotBefore(currentDate);
            samlAssertion.setNotOnOrAfter(new Date(currentDate.getTime() + this.issueLength));
            final SAMLAuthenticationStatement samlAuthenticationStatement = getSamlAuthenticationStatement(authentication, model);
            samlAssertion.addStatement(samlAuthenticationStatement);
            final SAMLAttributeStatement samlAttributeStatement = getSamlAttributeStatement(model);
            samlAssertion.addStatement(samlAttributeStatement);
            signSamlAssertion(samlAssertion);
            samlResponse.addAssertion(samlAssertion);
            final SAMLSOAPHTTPBinding binding = new SOAPHTTPBindingProvider(SAMLBinding.SOAP, null);
            binding.respond(response, samlResponse, null);
        } catch (final Exception e) {
            log.error(e, e);
            throw e;
        }
    }

    protected void signSamlAssertion(final SAMLAssertion samlAssertion) throws Exception {
        final Crypto issuerCrypto;
        final String issuerKeyPassword;
        final String issuerKeyName;
        Properties properties = new Properties();
        URL url = Loader.getResource(this.issuerKeyProperties);
        properties.load(url.openStream());
        String cryptoProp = properties.getProperty("org.apache.ws.security.saml.issuer.cryptoProp.file");
        issuerCrypto = CryptoFactory.getInstance(cryptoProp);
        issuerKeyName = properties.getProperty("org.apache.ws.security.saml.issuer.key.name");
        issuerKeyPassword = properties.getProperty("org.apache.ws.security.saml.issuer.key.password");
        X509Certificate[] issuerCerts = issuerCrypto.getCertificates(issuerKeyName);
        java.security.Key issuerPK = issuerCrypto.getPrivateKey(issuerKeyName, issuerKeyPassword);
        String sigAlgo = XMLSignature.ALGO_ID_SIGNATURE_RSA;
        String pubKeyAlgo = issuerCerts[0].getPublicKey().getAlgorithm();
        log.debug("automatic sig algo detection: " + pubKeyAlgo);
        if (pubKeyAlgo.equalsIgnoreCase("DSA")) {
            sigAlgo = XMLSignature.ALGO_ID_SIGNATURE_DSA;
        }
        samlAssertion.sign(sigAlgo, issuerPK, Arrays.asList(issuerCerts));
    }

    protected SAMLAuthenticationStatement getSamlAuthenticationStatement(final Authentication authentication, final Map<String, Object> model) throws SAMLException {
        final String authenticationMethod = (String) authentication.getAttributes().get("samlAuthenticationStatement::authMethod");
        final SAMLAuthenticationStatement samlAuthenticationStatement = new SAMLAuthenticationStatement();
        samlAuthenticationStatement.setAuthInstant(authentication.getAuthenticatedDate());
        samlAuthenticationStatement.setAuthMethod(authenticationMethod != null ? authenticationMethod : SAMLAuthenticationStatement.AuthenticationMethod_Unspecified);
        samlAuthenticationStatement.setSubject(getSamlSubject(model));
        return samlAuthenticationStatement;
    }

    protected SAMLAttributeStatement getSamlAttributeStatement(final Map<String, Object> model) throws SAMLException {
        final SAMLAttributeStatement samlAttributeStatement = new SAMLAttributeStatement();
        final String ticket = (String) model.get("ticket");
        final String targetService = (String) model.get("targetService");
        samlAttributeStatement.setSubject(getSamlSubject(model));
        SAMLAttribute ticketAttribute = new SAMLAttribute("proxyTicket", CAS_NAMESPACE, XSD_STRING, 0, Arrays.asList(ticket));
        samlAttributeStatement.addAttribute(ticketAttribute);
        SAMLAttribute serviceAttribute = new SAMLAttribute("targetService", CAS_NAMESPACE, XSD_STRING, 0, Arrays.asList(targetService));
        samlAttributeStatement.addAttribute(serviceAttribute);
        return samlAttributeStatement;
    }

    protected SAMLSubject getSamlSubject(final Map<String, Object> model) throws SAMLException {
        final SAMLSubject samlSubject = new SAMLSubject();
        samlSubject.addConfirmationMethod(SAMLSubject.CONF_SENDER_VOUCHES);
        final SAMLNameIdentifier samlNameIdentifier = new SAMLNameIdentifier();
        samlNameIdentifier.setName((String) model.get("user"));
        samlSubject.setNameIdentifier(samlNameIdentifier);
        return samlSubject;
    }

    /**
	 * @return the issuer
	 */
    public String getIssuer() {
        return issuer;
    }

    /**
	 * @param issuer the issuer to set
	 */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
	 * @return the issueLength
	 */
    public long getIssueLength() {
        return issueLength;
    }

    /**
	 * @param issueLength the issueLength to set
	 */
    public void setIssueLength(long issueLength) {
        this.issueLength = issueLength;
    }

    /**
	 * @return the issuerKeyProperties
	 */
    public String getIssuerKeyProperties() {
        return issuerKeyProperties;
    }

    /**
	 * @param issuerKeyProperties the issuerKeyProperties to set
	 */
    public void setIssuerKeyProperties(String issuerKeyProperties) {
        this.issuerKeyProperties = issuerKeyProperties;
    }
}
