package securus.services;

import static securus.services.ApplicationContext.getProperty;
import java.io.StringReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.persistence.EntityManager;
import javax.xml.parsers.DocumentBuilderFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;
import org.jboss.seam.web.ServletContexts;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import securus.entity.Subscription;
import securus.entity.User;
import securus.entity.Subscription.Currency;

/**
 * Payment operations
 * 
 * @author m.kanel
 * 
 */
@Name("paymentService")
@Scope(ScopeType.SESSION)
@AutoCreate
public class PaymentService {

    @Logger
    private Log log;

    private Currency currency = Currency.CHF;

    public String REF_NO = "191911";

    @In
    private EntityManager entityManager;

    public enum Mode {

        REGISTER, CHANGE
    }

    ;

    private Mode mode = Mode.REGISTER;

    public void appendParameter(StringBuilder sb, String key, Object value) {
        sb.append(key).append("=").append(value).append('&');
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(String name) {
        return (T) Component.getInstance(name);
    }

    public StringBuilder getFullUrl(String amount) {
        StringBuilder sb = new StringBuilder();
        sb.append(getGatewayUrl()).append('?');
        appendParameter(sb, "testOnly", getTestOnly());
        appendParameter(sb, "merchantId", getMerchantId());
        appendParameter(sb, "amount", amount);
        appendParameter(sb, "currency", currency);
        appendParameter(sb, "refno", REF_NO);
        appendParameter(sb, "conversationId", Conversation.instance().getId());
        appendParameter(sb, "cid", Conversation.instance().getId());
        appendParameter(sb, "useAlias", "yes");
        User user = getComponent("currentUser");
        if (Mode.CHANGE.equals(mode) && user != null && user.getAccount() != null) {
            Subscription subscription = user.getAccount().getCurrentSubscription();
            if (subscription != null && subscription.getAliasCC() != null && subscription.getCurrency().equals(currency)) {
                appendParameter(sb, "aliasCC", subscription.getAliasCC());
                appendParameter(sb, "paymentmethod", subscription.getLastPaymentMethod());
                appendParameter(sb, "expm", subscription.getLastCardExpm());
                appendParameter(sb, "expy", subscription.getLastCardExpy());
            }
        }
        return sb;
    }

    private Subscription getSubscription(Long id) {
        return entityManager.find(Subscription.class, id);
    }

    private String getTextValue(Element ele, String tagName) {
        String textVal = null;
        NodeList nl = ele.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            Element el = (Element) nl.item(0);
            textVal = el.getFirstChild().getNodeValue();
        }
        return textVal;
    }

    private Element getElement(Element document, String name) {
        NodeList list = document.getElementsByTagName(name);
        for (int s = 0; s < list.getLength(); s++) {
            Node n = list.item(s);
            if (Node.ELEMENT_NODE == n.getNodeType()) {
                return (Element) n;
            }
        }
        return null;
    }

    public void processReccurentPaymentResponce(String receivedResponse) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            StringReader reader = new StringReader(receivedResponse);
            InputSource inputSource = new InputSource(reader);
            Document doc = dbf.newDocumentBuilder().parse(inputSource);
            doc.getDocumentElement().normalize();
            Element body = getElement(doc.getDocumentElement(), "body");
            Element transaction = getElement(body, "transaction");
            String refno = transaction.getAttribute("refno");
            if (REF_NO.equals(refno)) {
                Element request = getElement(transaction, "request");
                Element response = getElement(transaction, "response");
                if (request != null && response != null) {
                    processPaymentXMLResponse(request, response);
                }
            }
        } catch (Exception ex) {
            throw new SecurusException("cannot make recurring payment", ex);
        }
    }

    private void processPaymentXMLResponse(Element request, Element response) {
        String responseCode = getTextValue(response, "responseCode");
        String responseMessage = getTextValue(response, "responseMessage");
        if ("Authorized".equals(responseMessage)) {
            String uppTransactionId = getTextValue(response, "uppTransactionId");
            String subscriptionId = getTextValue(request, "subscriptionId");
            Subscription subscription = getSubscription(Long.valueOf(subscriptionId));
            subscription.setLastTransactionId(uppTransactionId);
            Calendar today = new GregorianCalendar();
            subscription.getEffective().setBeginDate(today.getTime());
            today.add(subscription.isBillYearly() ? Calendar.YEAR : Calendar.MONTH, 1);
            subscription.setNextBilling(today.getTime());
            entityManager.persist(subscription);
            Events.instance().raiseEvent("recurringPaymentOk", subscription);
            return;
        }
        throw new SecurusException("Payment not authorized");
    }

    public void doRecurringPayment(Subscription subscription) {
        int amount = Math.round(subscription.getTotalCostWithDiscounts() * 100.0f);
        String currency = subscription.getCurrency();
        String aliasCC = subscription.getAliasCC();
        String expm = subscription.getLastCardExpm();
        String expy = subscription.getLastCardExpy();
        String subscriptionId = String.valueOf(subscription.getSubscriptionId());
        StringBuffer xmlSB = new StringBuffer("");
        xmlSB.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xmlSB.append("<authorizationService version=\"1\">\n");
        xmlSB.append(" <body merchantId=\"" + getMerchantId() + "\" testOnly=\"" + getTestOnly() + "\">\n");
        xmlSB.append("    <transaction refno=\"" + REF_NO + "\">\n");
        xmlSB.append("      <request>\n");
        xmlSB.append("        <amount>" + amount + "</amount>\n");
        xmlSB.append("        <currency>" + currency + "</currency>\n");
        xmlSB.append("        <aliasCC>" + aliasCC + "</aliasCC>\n");
        xmlSB.append("        <expm>" + expm + "</expm>\n");
        xmlSB.append("        <expy>" + expy + "</expy>\n");
        xmlSB.append("        <subscriptionId>" + subscriptionId + "</subscriptionId>\n");
        xmlSB.append("      </request>\n");
        xmlSB.append("    </transaction>\n");
        xmlSB.append(" </body>\n");
        xmlSB.append("</authorizationService>\n");
        String xmlS = xmlSB.toString();
        try {
            java.net.URL murl = new java.net.URL(getRecurringPaymentUrl());
            java.net.HttpURLConnection mcon = (java.net.HttpURLConnection) murl.openConnection();
            mcon.setRequestMethod("POST");
            mcon.setRequestProperty("encoding", "UTF-8");
            mcon.setRequestProperty("Content-Type", "text/xml");
            mcon.setRequestProperty("Content-length", String.valueOf(xmlS.length()));
            mcon.setDoOutput(true);
            java.io.OutputStream outs = mcon.getOutputStream();
            outs.write(xmlS.getBytes("UTF-8"));
            outs.close();
            java.io.BufferedReader inps = new java.io.BufferedReader(new java.io.InputStreamReader(mcon.getInputStream()));
            StringBuffer respSB = new StringBuffer("");
            String s = null;
            while ((s = inps.readLine()) != null) {
                respSB.append(s);
            }
            inps.close();
            String respXML = respSB.toString();
            processReccurentPaymentResponce(respXML);
        } catch (Exception ex) {
            throw new SecurusException(ex);
        }
    }

    public Subscription getCurrentSubscription() {
        return getComponent("selectedSubscription");
    }

    private boolean registering() {
        return Mode.REGISTER.equals(mode);
    }

    private String getRecurringPaymentUrl() {
        return getProperty("payment_gateway_xml_url");
    }

    public String getPaymentFailedReverseUrl() {
        return getServerUrl() + (registering() ? getProperty("payment_failed_reverse_url") : getProperty("payment_update_failed_reverse_url"));
    }

    public String getPaymentCancelReverseUrl() {
        return getServerUrl() + (registering() ? getProperty("payment_cancel_reverse_url") : getProperty("payment_update_cancel_reverse_url"));
    }

    public String getSuccessURL() {
        return getServerUrl() + (registering() ? getProperty("payment_ok_reverse_url") : getProperty("payment_changed_reverse_url"));
    }

    private String getMerchantId() {
        return getProperty("payment_merchant_id");
    }

    private String getTestOnly() {
        return getProperty("payment_test_only");
    }

    private String getGatewayUrl() {
        return getProperty("payment_gateway_url");
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    private String getServerUrl() {
        return ApplicationContext.getServerURLFromRequest(ServletContexts.getInstance().getRequest());
    }
}
