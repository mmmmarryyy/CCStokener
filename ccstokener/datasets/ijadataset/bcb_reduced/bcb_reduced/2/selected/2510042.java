package org.torweg.pulse.component.shop.checkout.paypal;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.torweg.pulse.accesscontrol.Role;
import org.torweg.pulse.accesscontrol.User.Everybody;
import org.torweg.pulse.annotations.Action;
import org.torweg.pulse.annotations.Groups;
import org.torweg.pulse.annotations.Permission;
import org.torweg.pulse.annotations.Action.Security;
import org.torweg.pulse.bundle.Bundle;
import org.torweg.pulse.bundle.Controller;
import org.torweg.pulse.component.shop.checkout.AbstractShipmentMethod;
import org.torweg.pulse.component.shop.checkout.Address;
import org.torweg.pulse.component.shop.checkout.AddressBuilder;
import org.torweg.pulse.component.shop.checkout.BasicPaymentMethodConfiguration;
import org.torweg.pulse.component.shop.checkout.BasicShipmentMethodConfiguration;
import org.torweg.pulse.component.shop.checkout.Order;
import org.torweg.pulse.component.shop.checkout.paypal.PPECheckoutConfiguration.PPECheckoutLocDepConfiguration;
import org.torweg.pulse.component.shop.model.ShopSettings;
import org.torweg.pulse.component.shop.model.ShoppingCart;
import org.torweg.pulse.component.shop.payment.PaymentStatus;
import org.torweg.pulse.configuration.Configurable2;
import org.torweg.pulse.configuration.Configuration;
import org.torweg.pulse.configuration.ConfigurationException;
import org.torweg.pulse.configuration.EmailAddressConfiguration;
import org.torweg.pulse.configuration.EmailConfiguration;
import org.torweg.pulse.configuration.FormConfiguration;
import org.torweg.pulse.configuration.FormConfiguration.FormFieldConfiguration;
import org.torweg.pulse.email.Email;
import org.torweg.pulse.invocation.lifecycle.Lifecycle;
import org.torweg.pulse.result.ExtendedFormResult;
import org.torweg.pulse.result.FormResult.FormFieldResult;
import org.torweg.pulse.result.FormResult.FormFieldStatus;
import org.torweg.pulse.service.PulseException;
import org.torweg.pulse.service.event.RedirectEvent;
import org.torweg.pulse.service.request.Command;
import org.torweg.pulse.service.request.Parameter;
import org.torweg.pulse.service.request.ServiceRequest;
import org.torweg.pulse.service.request.ServiceSession;
import org.torweg.pulse.util.xml.transform.XSLTOutputter;

/**
 * The PayPal™ Express (PPE) checkout controller.
 * 
 * @author Christian Schatt
 * @version $Revision$
 */
public final class PPECheckoutController extends Controller implements Configurable2 {

    /**
	 * The <code>Logger</code>.
	 */
    private static final Logger LOGGER = Logger.getLogger(PPECheckoutController.class);

    /**
	 * The <code>PPECheckoutConfiguration</code>.
	 */
    private PPECheckoutConfiguration configuration = null;

    /**
	 * Starts the PayPal™ Express (PPE) checkout process.
	 * 
	 * @param bundle
	 *            the <code>Bundle</code>
	 * @param request
	 *            the <code>ServiceRequest</code>
	 * @return the <code>PPECheckoutResult</code>
	 */
    @Action(value = "startPPECheckout", generate = true, security = Security.ALWAYS, stripSitemapID = false)
    @Permission("startPPECheckout")
    @Groups(values = { "PPECheckout" })
    public PPECheckoutResult startPPECheckout(final Bundle bundle, final ServiceRequest request) {
        LOGGER.info("start PayPal™ Express checkout");
        PPECheckoutLocDepConfiguration config = getConfiguration().getLocDepConfigurations().get(request.getLocale());
        PPECheckoutResult result = new PPECheckoutResult("startPPECheckout");
        if (config.isLoginMandatory() && (request.getUser() instanceof Everybody)) {
            result.setStatus(PPECheckoutStatus.USER_NOT_LOGGED_IN);
            return result;
        }
        Order order = getNewOrder(bundle, request, config, result);
        if (order == null) {
            return result;
        }
        order.setEmailAddress(request.getUser().getEmail());
        Set<BasicShipmentMethodConfiguration> shipConfs = getAvailableShipMethConfs(request, order, config);
        if (shipConfs.isEmpty()) {
            result.setStatus(PPECheckoutStatus.NO_SHIPMENT_AVAILABLE);
            return result;
        }
        PPEPayment payMeth = getPaymentMethod(request, order, config);
        if (payMeth == null) {
            result.setStatus(PPECheckoutStatus.NO_PAYMENT_AVAILABLE);
            return result;
        }
        order.setPaymentMethod(payMeth);
        PPECheckoutState state = new PPECheckoutState(order, shipConfs, createProcessId());
        setPPECheckoutState(state, bundle, request.getLocale(), request.getSession());
        result.setState(state);
        boolean success = doSetExpressCheckout(request, state, config);
        if (!success) {
            LOGGER.warn("Proceeding the payment process was rejected.");
            removePPECheckoutState(bundle, request.getLocale(), request.getSession());
            result.setStatus(PPECheckoutStatus.PAYMENT_REJECTED);
            return result;
        }
        RedirectEvent redirect = new RedirectEvent(createPaymentRedirectURL(state.getToken()));
        redirect.setTemporaryRedirect(true);
        request.getEventManager().addEvent(redirect);
        return result;
    }

    /**
	 * Cancels the PayPal™ Express checkout process.
	 * 
	 * @param bundle
	 *            the <code>Bundle</code>
	 * @param request
	 *            the <code>ServiceRequest</code>
	 * @return the <code>PPECheckoutResult</code>
	 */
    @Action(value = "cancelPPECheckout", generate = true, security = Security.ALWAYS, stripSitemapID = false)
    @Permission("cancelPPECheckout")
    @Groups(values = { "PPECheckout" })
    public PPECheckoutResult cancelPPECheckout(final Bundle bundle, final ServiceRequest request) {
        LOGGER.info("cancel PayPal™ Express checkout");
        removePPECheckoutState(bundle, request.getLocale(), request.getSession());
        return new PPECheckoutResult("cancelPPECheckout");
    }

    /**
	 * Returns the data collected on the PayPal™ Express website.
	 * 
	 * @param bundle
	 *            the <code>Bundle</code>
	 * @param request
	 *            the <code>ServiceRequest</code>
	 * @return the <code>PPECheckoutResult</code>
	 */
    @Action(value = "getPPECheckoutData", generate = true, security = Security.ALWAYS, stripSitemapID = false)
    @Permission("getPPECheckoutData")
    @Groups(values = { "PPECheckout" })
    public PPECheckoutResult getPPECheckoutData(final Bundle bundle, final ServiceRequest request) {
        LOGGER.info("get PayPal™ Express checkout data");
        PPECheckoutResult result = new PPECheckoutResult("getPPECheckoutData");
        PPECheckoutState state = getPPECheckoutState(bundle, request.getLocale(), request.getSession());
        result.setState(state);
        if (state == null) {
            result.setStatus(PPECheckoutStatus.STATE_IS_NULL);
            return result;
        }
        if (!processIdValid(request.getCommand(), state)) {
            removePPECheckoutState(bundle, request.getLocale(), request.getSession());
            result.setStatus(PPECheckoutStatus.PROCESS_ID_INVALID);
            return result;
        }
        boolean success = doGetExpressCheckoutDetails(state, getConfiguration().getLocDepConfigurations().get(request.getLocale()));
        if (!success) {
            LOGGER.warn("Proceeding the payment process was rejected.");
            removePPECheckoutState(bundle, request.getLocale(), request.getSession());
            result.setStatus(PPECheckoutStatus.PAYMENT_REJECTED);
            return result;
        }
        return result;
    }

    /**
	 * Completes the PayPal™ Express checkout process.
	 * 
	 * @param bundle
	 *            the <code>Bundle</code>
	 * @param request
	 *            the <code>ServiceRequest</code>
	 * @return the <code>PPECheckoutResult</code>
	 */
    @Action(value = "completePPECheckout", generate = true, security = Security.ALWAYS, stripSitemapID = false)
    @Permission("completePPECheckout")
    @Groups(values = { "PPECheckout" })
    public PPECheckoutResult completePPECheckout(final Bundle bundle, final ServiceRequest request) {
        LOGGER.info("complete PayPal™ Express checkout");
        PPECheckoutLocDepConfiguration config = getConfiguration().getLocDepConfigurations().get(request.getLocale());
        PPECheckoutResult result = new PPECheckoutResult("completePPECheckout");
        PPECheckoutState state = getPPECheckoutState(bundle, request.getLocale(), request.getSession());
        result.setState(state);
        if (state == null) {
            result.setStatus(PPECheckoutStatus.STATE_IS_NULL);
            return result;
        }
        if (!processIdValid(request.getCommand(), state)) {
            removePPECheckoutState(bundle, request.getLocale(), request.getSession());
            result.setStatus(PPECheckoutStatus.PROCESS_ID_INVALID);
            return result;
        }
        boolean success = parseFormData(request.getCommand(), state, config, result);
        if (!success) {
            result.setStatus(PPECheckoutStatus.FORM_ERROR);
            return result;
        }
        success = doDoExpressCheckoutPayment(state, config);
        if (!success) {
            LOGGER.warn("Proceeding the payment process was rejected.");
            removePPECheckoutState(bundle, request.getLocale(), request.getSession());
            result.setStatus(PPECheckoutStatus.PAYMENT_REJECTED);
            return result;
        }
        if (state.isRedirectRequired()) {
            RedirectEvent redirect = new RedirectEvent(createGiropayRedirectURL(state.getToken()));
            redirect.setTemporaryRedirect(true);
            request.getEventManager().addEvent(redirect);
            return result;
        }
        completeProcess(bundle, request, state.getOrder(), config);
        return result;
    }

    /**
	 * Completes the PayPal™ Express checkout process after giropay or
	 * electronic funds transfer payment.
	 * 
	 * @param bundle
	 *            the <code>Bundle</code>
	 * @param request
	 *            the <code>ServiceRequest</code>
	 * @return the <code>PPECheckoutResult</code>
	 */
    @Action(value = "completePPECheckoutWithGiropay", generate = true, security = Security.ALWAYS, stripSitemapID = false)
    @Permission("completePPECheckoutWithGiropay")
    @Groups(values = { "PPECheckout" })
    public PPECheckoutResult completePPECheckoutWithGiropay(final Bundle bundle, final ServiceRequest request) {
        LOGGER.info("complete PayPal™ Express checkout with giropay");
        PPECheckoutLocDepConfiguration config = getConfiguration().getLocDepConfigurations().get(request.getLocale());
        PPECheckoutResult result = new PPECheckoutResult("completePPECheckoutWithGiropay");
        PPECheckoutState state = getPPECheckoutState(bundle, request.getLocale(), request.getSession());
        result.setState(state);
        if (state == null) {
            result.setStatus(PPECheckoutStatus.STATE_IS_NULL);
            return result;
        }
        if (!processIdValid(request.getCommand(), state)) {
            removePPECheckoutState(bundle, request.getLocale(), request.getSession());
            result.setStatus(PPECheckoutStatus.PROCESS_ID_INVALID);
            return result;
        }
        completeProcess(bundle, request, state.getOrder(), config);
        return result;
    }

    /**
	 * Initializes the <code>PPECheckoutController</code> with the given
	 * <code>Configuration</code>.
	 * 
	 * @param config
	 *            the <code>Configuration</code>
	 * @throws NullPointerException
	 *             if the given <code>Configuration</code> is <code>null</code>
	 * @throws ConfigurationException
	 *             if the given <code>Configuration</code> is not a
	 *             <code>PPECheckoutConfiguration</code>
	 */
    public void initialize(final Configuration config) {
        if (!(config instanceof PPECheckoutConfiguration)) {
            if (config == null) {
                throw new NullPointerException("The given Configuration is null.");
            }
            throw new ConfigurationException(config, this);
        }
        setConfiguration((PPECheckoutConfiguration) config);
    }

    /**
	 * Returns the <code>PPECheckoutConfiguration</code>.
	 * 
	 * @return the <code>PPECheckoutConfiguration</code>
	 * @throws IllegalStateException
	 *             if the <code>PPECheckoutController</code> has not yet been
	 *             initialized
	 */
    private PPECheckoutConfiguration getConfiguration() {
        if (this.configuration == null) {
            throw new IllegalStateException("The PPECheckoutController has not yet been initialized.");
        }
        return this.configuration;
    }

    /**
	 * Sets the <code>PPECheckoutConfiguration</code>.
	 * 
	 * @param conf
	 *            the <code>PPECheckoutConfiguration</code> to set
	 * @return <code>this</code>
	 * @throws NullPointerException
	 *             if the given <code>PPECheckoutConfiguration</code> is
	 *             <code>null</code>
	 */
    private PPECheckoutController setConfiguration(final PPECheckoutConfiguration conf) {
        if (conf == null) {
            throw new NullPointerException("The given PPECheckoutConfiguration is null.");
        }
        this.configuration = conf;
        return this;
    }

    /**
	 * Returns the <code>PPECheckoutState</code> for the given
	 * <code>Bundle</code> and <code>Locale</code> from the given
	 * <code>ServiceSession</code>.
	 * 
	 * <p>
	 * Returns <code>null</code>, if the given <code>ServiceSession</code> does
	 * not hold a <code>PPECheckoutState</code> for the given
	 * <code>Bundle</code> and <code>Locale</code>.
	 * </p>
	 * 
	 * @param bundle
	 *            the<code>Bundle</code>
	 * @param locale
	 *            the <code>Locale</code>
	 * @param session
	 *            the <code>ServiceSession</code>
	 * @return the <code>PPECheckoutState</code> or <code>null</code>
	 * @throws NullPointerException
	 *             if the given <code>Bundle</code>, <code>Locale</code> or
	 *             <code>ServiceSession</code> is <code>null</code>
	 */
    private static PPECheckoutState getPPECheckoutState(final Bundle bundle, final Locale locale, final ServiceSession session) {
        return (PPECheckoutState) session.getAttribute(getPPECheckoutStateAttributeName(bundle, locale));
    }

    /**
	 * Sets the given <code>PPECheckoutState</code> for the given
	 * <code>Bundle</code> and <code>Locale</code> in the given
	 * <code>ServiceSession</code>.
	 * 
	 * @param state
	 *            the <code>PPECheckoutState</code> to set
	 * @param bundle
	 *            the <code>Bundle</code>
	 * @param locale
	 *            the <code>Locale</code>
	 * @param session
	 *            the <code>ServiceSession</code>
	 * @throws NullPointerException
	 *             if the given <code>PPECheckoutState</code>,
	 *             <code>Bundle</code>, <code>Locale</code> or
	 *             <code>ServiceSession</code> is <code>null</code>
	 */
    private static void setPPECheckoutState(final PPECheckoutState state, final Bundle bundle, final Locale locale, final ServiceSession session) {
        if (state == null) {
            throw new NullPointerException("The given PPECheckoutState is null.");
        }
        session.setAttribute(getPPECheckoutStateAttributeName(bundle, locale), state);
    }

    /**
	 * Removes the <code>PPECheckoutState</code> for the given
	 * <code>Bundle</code> and <code>Locale</code> from the given
	 * <code>ServiceSession</code>.
	 * 
	 * @param bundle
	 *            the <code>Bundle</code>
	 * @param locale
	 *            the <code>Locale</code>
	 * @param session
	 *            the <code>ServiceSession</code>
	 * @throws NullPointerException
	 *             if the given <code>Bundle</code>, <code>Locale</code> or
	 *             <code>ServiceSession</code> is <code>null</code>
	 */
    private static void removePPECheckoutState(final Bundle bundle, final Locale locale, final ServiceSession session) {
        session.removeAttribute(getPPECheckoutStateAttributeName(bundle, locale));
    }

    /**
	 * Returns the <code>ServiceSession</code>-attribute-name of the
	 * <code>PPECheckoutState</code> for the given <code>Bundle</code> and
	 * <code>Locale</code>.
	 * 
	 * @param bundle
	 *            the <code>Bundle</code>
	 * @param locale
	 *            the <code>Locale</code>
	 * @return the <code>ServiceSession</code>-attribute-name
	 * @throws NullPointerException
	 *             if the given <code>Bundle</code> or <code>Locale</code> is
	 *             <code>null</code>
	 */
    private static String getPPECheckoutStateAttributeName(final Bundle bundle, final Locale locale) {
        return bundle.getName() + ":" + locale.toString() + ":" + PPECheckoutController.class.getCanonicalName() + ":" + PPECheckoutState.class.getCanonicalName();
    }

    /**
	 * Returns a new <code>Order</code> created from the
	 * <code>ShoppingCart</code> in the given <code>ServiceRequest</code>.
	 * 
	 * <p>
	 * Returns <code>null</code> if the <code>ShoppingCart</code> is either
	 * <code>null</code> or empty, or if the order total is less than the
	 * configured minimum order amount or greater than the configured maximum
	 * order amount.
	 * </p>
	 * 
	 * @param bundle
	 *            the <code>Bundle</code>
	 * @param request
	 *            the <code>ServiceRequest</code>
	 * @param config
	 *            the <code>PPECheckoutLocDepConfiguration</code>
	 * @param result
	 *            the <code>PPECheckoutResult</code>
	 * @return the new <code>Order</code> or <code>null</code>
	 */
    private static Order getNewOrder(final Bundle bundle, final ServiceRequest request, final PPECheckoutLocDepConfiguration config, final PPECheckoutResult result) {
        ShoppingCart cart = ShoppingCart.getCart(bundle, request);
        if (cart == null) {
            result.setStatus(PPECheckoutStatus.CART_NULL);
            return null;
        }
        if (cart.isEmpty()) {
            result.setStatus(PPECheckoutStatus.CART_EMPTY);
            return null;
        }
        Order order = new Order(cart, ShopSettings.getSettings(bundle, request), request.getUser());
        long orderTotal = getOrderTotal(order);
        if ((config.getMinOrderValue() != 0L) && (config.getMinOrderValue() > orderTotal)) {
            result.setStatus(PPECheckoutStatus.ORDER_TOTAL_TOO_LOW);
            return null;
        }
        if ((config.getMaxOrderValue() != 0L) && (config.getMaxOrderValue() < orderTotal)) {
            result.setStatus(PPECheckoutStatus.ORDER_TOTAL_TOO_HIGH);
            return null;
        }
        return order;
    }

    /**
	 * Returns the order total from the given <code>Order</code>.
	 * 
	 * <p>
	 * If the given <code>Order</code> is net based, the net total is returned.
	 * If the given <code>Order</code> is gross based, the gross total is
	 * returned. Extra charges (e.g. for shipment or payment) are not included
	 * in the order total.
	 * </p>
	 * 
	 * @param order
	 *            the <code>Order</code>
	 * @return the order total
	 */
    private static long getOrderTotal(final Order order) {
        if (order.isNetBased()) {
            return order.getNetTotal();
        }
        return order.getGrossTotal();
    }

    /**
	 * Returns a <code>Set</code> holding the
	 * <code>BasicShipmentMethodConfiguration</code>s configured to be available
	 * for the current <code>User</code> and the given <code>Order</code>.
	 * 
	 * <p>
	 * Returns an empty <code>Set</code>, if no
	 * <code>BasicShipmentMethodConfiguration</code> is configured to be
	 * available.
	 * </p>
	 * 
	 * @param request
	 *            the <code>ServiceRequest</code>
	 * @param order
	 *            the <code>Order</code>
	 * @param config
	 *            the <code>PPECheckoutLocDepConfiguration</code>
	 * @return a <code>Set</code> holding the available
	 *         <code>BasicShipmentMethodConfiguration</code>s
	 */
    private static Set<BasicShipmentMethodConfiguration> getAvailableShipMethConfs(final ServiceRequest request, final Order order, final PPECheckoutLocDepConfiguration config) {
        long orderTotal = getOrderTotal(order);
        Set<BasicShipmentMethodConfiguration> shipConfs = new HashSet<BasicShipmentMethodConfiguration>();
        for (BasicShipmentMethodConfiguration shipConf : config.getShipmentMethodConfs()) {
            if (((shipConf.getMinOrderValue() == 0L) || (shipConf.getMinOrderValue() <= orderTotal)) && ((shipConf.getMaxOrderValue() == 0L) || (shipConf.getMaxOrderValue() >= orderTotal))) {
                Set<String> roleNames = shipConf.getRoleNames();
                if (roleNames.isEmpty()) {
                    shipConfs.add(shipConf);
                } else {
                    for (Role role : request.getUser().getRoles()) {
                        if (roleNames.contains(role.getName())) {
                            shipConfs.add(shipConf);
                            break;
                        }
                    }
                }
            }
        }
        return shipConfs;
    }

    /**
	 * Returns a <code>PPEPayment</code> if configured to be available for the
	 * current <code>User</code> and the given <code>Order</code>.
	 * 
	 * <code>Returns <code>null</code>, if the <code>PPEPayment</code></code> is
	 * not configured to be available.
	 * 
	 * @param request
	 *            the <code>ServiceRequest</code>
	 * @param order
	 *            the <code>Order</code>
	 * @param config
	 *            the <code>PPECheckoutLocDepConfiguration</code>
	 * @return the <code>PPEPayment</code> or <code>null</code>
	 */
    private static PPEPayment getPaymentMethod(final ServiceRequest request, final Order order, final PPECheckoutLocDepConfiguration config) {
        long orderTotal = getOrderTotal(order);
        BasicPaymentMethodConfiguration payConf = config.getPaymentMethodConf();
        if (((payConf.getMinOrderValue() == 0L) || (payConf.getMinOrderValue() <= orderTotal)) && ((payConf.getMaxOrderValue() == 0L) || (payConf.getMaxOrderValue() >= orderTotal))) {
            Set<String> roleNames = payConf.getRoleNames();
            if (roleNames.isEmpty()) {
                return new PPEPayment().init(payConf);
            } else {
                for (Role role : request.getUser().getRoles()) {
                    if (roleNames.contains(role.getName())) {
                        return new PPEPayment().init(payConf);
                    }
                }
            }
        }
        return null;
    }

    /**
	 * Creates a process id used to check whether the checkout process has been
	 * interrupted.
	 * 
	 * @return the process id
	 */
    private static String createProcessId() {
        return new StringBuilder().append(Long.toHexString(Lifecycle.getRandom().nextLong())).append('_').append(Long.toHexString(new Date().getTime())).toString();
    }

    /**
	 * Starts the PayPal™ Express checkout by calling the "SetExtpressCheckout"
	 * service method.
	 * 
	 * @param request
	 *            the <code>ServiceRequest</code>
	 * @param state
	 *            the <code>PPECheckoutState</code>
	 * @param config
	 *            the <code>PPECheckoutLocDepConfiguration</code>
	 * @return a <code>boolean</code> determining whether the method call was
	 *         successful
	 */
    private static boolean doSetExpressCheckout(final ServiceRequest request, final PPECheckoutState state, final PPECheckoutLocDepConfiguration config) {
        Order order = state.getOrder();
        Map<String, String> params = new HashMap<String, String>();
        params.put("METHOD", "SetExpressCheckout");
        params.put("RETURNURL", createReturnURL(request, state.getProcessId()));
        params.put("CANCELURL", createCancelURL(request));
        params.put("GIROPAYSUCCESSURL", createGiropaySuccessURL(request, state.getProcessId()));
        params.put("GIROPAYCANCELURL", createGiropayCancelURL(request));
        params.put("AMT", BigDecimal.valueOf(getOrderTotal(order)).scaleByPowerOfTen(-order.getCurrency().getDefaultFractionDigits()).toPlainString());
        params.put("CURRENCYCODE", order.getCurrency().getCurrencyCode());
        if (order.getEmailAddress() != null) {
            params.put("EMAIL", order.getEmailAddress());
        }
        params.put("LOCALECODE", order.getLocale().getCountry());
        Map<String, String> result = execPayPalNVPCall(params, config);
        if (result.get("ACK").equalsIgnoreCase("success")) {
            state.setToken(result.get("TOKEN"));
            return true;
        }
        return false;
    }

    /**
	 * Returns the "ReturnURL"-<code>String</code>.
	 * 
	 * @param request
	 *            the <code>ServiceRequest</code>
	 * @param pid
	 *            the processId
	 * @return the "ReturnURL"-<code>String</code>
	 */
    private static String createReturnURL(final ServiceRequest request, final String pid) {
        Command command = request.getCommand().createCopy(false);
        command.setAction("getPPECheckoutData");
        command.addHttpParameter("processId", pid);
        command.setSecurity(Security.KEEP);
        return request.getHttpServletResponse().encodeURL(command.toCommandURL(request));
    }

    /**
	 * Returns the "CancelURL"-<code>String</code>.
	 * 
	 * @param request
	 *            the <code>ServiceRequest</code>
	 * @return the "ReturnURL"-<code>String</code>
	 */
    private static String createCancelURL(final ServiceRequest request) {
        Command command = request.getCommand().createCopy(false);
        command.setAction("cancelPPECheckout");
        command.setSecurity(Security.KEEP);
        return request.getHttpServletResponse().encodeURL(command.toCommandURL(request));
    }

    /**
	 * Returns the "GiropaySuccesssURL"-<code>String</code>.
	 * 
	 * @param request
	 *            the <code>ServiceRequest</code>
	 * @param pid
	 *            the processId
	 * @return the "GiropaySuccessURL"-<code>String</code>
	 */
    private static String createGiropaySuccessURL(final ServiceRequest request, final String pid) {
        Command command = request.getCommand().createCopy(false);
        command.setAction("completePPECheckoutWithGiropay");
        command.addHttpParameter("processId", pid);
        command.setSecurity(Security.KEEP);
        return request.getHttpServletResponse().encodeURL(command.toCommandURL(request));
    }

    /**
	 * Returns the "GiropayCancelURL"-<code>String</code>.
	 * 
	 * @param request
	 *            the <code>ServiceRequest</code>
	 * @return the "GiropayCancelURL"-<code>String</code>
	 */
    private static String createGiropayCancelURL(final ServiceRequest request) {
        return createCancelURL(request);
    }

    /**
	 * Executes a call to the PayPal™ NVP service.
	 * 
	 * @param params
	 *            the parameter-map to use when calling the PayPal™ NVP service
	 * @param config
	 *            the <code>PPECheckoutLocDepConfiguration</code>
	 * @return the parameter-map returned by the PayPal™ NVP service
	 * @throws NullPointerException
	 *             if the given parameter-map or the given
	 *             <code>PPECheckoutLocDepConfiguration</code> is
	 *             <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the given parameter-map is empty
	 */
    private static Map<String, String> execPayPalNVPCall(final Map<String, String> params, final PPECheckoutLocDepConfiguration config) {
        if (params == null) {
            throw new NullPointerException("The given parameter-map is null.");
        }
        if (params.isEmpty()) {
            throw new IllegalArgumentException("The given parameter-map is empty.");
        }
        if (config == null) {
            throw new NullPointerException("The given PPECheckoutLocDepConfiguration is null.");
        }
        StringBuilder urlBuilder = new StringBuilder();
        HttpsURLConnection connection;
        try {
            urlBuilder.append(config.getPayPalURL());
            urlBuilder.append("?USER=").append(URLEncoder.encode(config.getUsername(), "UTF-8"));
            urlBuilder.append("&PWD=").append(URLEncoder.encode(config.getPassword(), "UTF-8"));
            urlBuilder.append("&SIGNATURE=").append(URLEncoder.encode(config.getSignature(), "UTF-8"));
            urlBuilder.append("&VERSION=").append(URLEncoder.encode(config.getVersion(), "UTF-8"));
            for (Map.Entry<String, String> entry : params.entrySet()) {
                urlBuilder.append('&').append(URLEncoder.encode(entry.getKey().toUpperCase(), "UTF-8"));
                urlBuilder.append('=').append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            connection = (HttpsURLConnection) new URL(urlBuilder.toString()).openConnection();
            connection.connect();
        } catch (IOException e) {
            throw new PulseException(e.getLocalizedMessage(), e);
        }
        StringBuilder responseBuilder = new StringBuilder();
        InputStream in = null;
        try {
            in = connection.getInputStream();
            while (in.available() > 0) {
                responseBuilder.append(Character.toChars(in.read()));
            }
        } catch (IOException e) {
            throw new PulseException(e.getLocalizedMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOGGER.warn(e.getLocalizedMessage(), e);
                }
            }
            connection.disconnect();
        }
        String response = responseBuilder.toString();
        Map<String, String> result = new HashMap<String, String>();
        for (String param : response.split("&")) {
            String[] tmp = param.split("=");
            try {
                result.put(URLDecoder.decode(tmp[0], "UTF-8"), URLDecoder.decode(tmp[1], "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new PulseException(e.getLocalizedMessage(), e);
            }
        }
        return result;
    }

    /**
	 * Returns the URL-<code>String</code> for the payment redirect.
	 * 
	 * @param token
	 *            the token to be appended as a parameter
	 * @return the URL-<code>String</code> for the redirect
	 * @throws NullPointerException
	 *             if the given token is <code>null</code>
	 */
    private static String createPaymentRedirectURL(final String token) {
        if (token == null) {
            throw new NullPointerException("The given token is null.");
        }
        return new StringBuilder().append("https://www.sandbox.paypal.com/cgi-bin/webscr?").append("cmd=_express-checkout&token=").append(token).toString();
    }

    /**
	 * Determines whether the processId-parameter from the given
	 * <code>Command</code> matches the processId in the given
	 * <code>PPECheckoutState</code>.
	 * 
	 * @param command
	 *            the <code>Command</code>
	 * @param state
	 *            the <code>PPECheckoutState</code>
	 * @return <code>true</code> if the processId-parameter from the given
	 *         <code>Command</code> matches the processId in the given
	 *         <code>PPECheckoutState</code>. Returns <code>false</code>,
	 *         otherwise
	 */
    private static boolean processIdValid(final Command command, final PPECheckoutState state) {
        Parameter processIdParam = command.getParameter("processId");
        if (processIdParam == null) {
            return false;
        }
        String processId = processIdParam.getFirstValue();
        return (processId != null) && processId.equals(state.getProcessId());
    }

    /**
	 * Collects the PayPal™ Express checkout data provided by the PayPal™ user
	 * by calling the "GetExtpressCheckoutDetails" service method.
	 * 
	 * @param state
	 *            the <code>PPECheckoutState</code>
	 * @param config
	 *            the <code>PPECheckoutLocDepConfiguration</code>
	 * @return a <code>boolean</code> determining whether the method call was
	 *         successful
	 */
    private static boolean doGetExpressCheckoutDetails(final PPECheckoutState state, final PPECheckoutLocDepConfiguration config) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("METHOD", "GetExpressCheckoutDetails");
        params.put("TOKEN", state.getToken());
        Map<String, String> result = execPayPalNVPCall(params, config);
        if (result.get("ACK").equalsIgnoreCase("success")) {
            state.setToken(result.get("TOKEN"));
            state.setPayerId(result.get("PAYERID"));
            if (result.get("REDIRECTREQUIRED") != null) {
                state.setRedirectRequired(result.get("REDIRECTREQUIRED").equalsIgnoreCase("true"));
            }
            Order order = state.getOrder();
            if (order.getEmailAddress() == null) {
                order.setEmailAddress(result.get("EMAIL"));
            }
            AddressBuilder builder = new AddressBuilder();
            String name = result.get("SHIPTONAME");
            builder.setFirstName(name.substring(0, name.lastIndexOf(' ')));
            builder.setLastName(name.substring(name.lastIndexOf(' ') + 1));
            builder.setStreet(result.get("SHIPTOSTREET"));
            builder.setStreetExtension(result.get("SHIPTOSTREET2"));
            builder.setPostalCode(result.get("SHIPTOZIP"));
            builder.setCity(result.get("SHIPTOCITY"));
            builder.setCountry(result.get("SHIPTOCOUNTRYCODE"));
            order.setBillingAddress(new Address(builder));
            order.setShippingAddress(new Address(builder));
            return true;
        }
        return false;
    }

    /**
	 * ...
	 * 
	 * @param command
	 *            the <code>Command</code>
	 * @param state
	 *            the <code>PPECheckoutState</code>
	 * @param config
	 *            the <code>PPECheckoutLocDepConfiguration</code>
	 * @param result
	 *            the <code>PPECheckoutResult</code>
	 * @return a <code>boolean</code> determining whether a form error was found
	 */
    private static boolean parseFormData(final Command command, final PPECheckoutState state, final PPECheckoutLocDepConfiguration config, final PPECheckoutResult result) {
        Order order = state.getOrder();
        result.setEmailAddressFormResult(parseForm(command, order, config.getEmailAddressFormConf(), ""));
        result.setShippingAddressFormResult(parseForm(command, order.getShippingAddress(), config.getShippingAddressFormConf(), "shippingAddress_"));
        result.setBillingAddressFormResult(parseForm(command, order.getBillingAddress(), config.getBillingAddressFormConf(), "billingAddress_"));
        result.setShipmentMethodFormResult(setShipmentMethod(command, state));
        if (!result.getEmailAddressFormResult().isSuccess() || !result.getShippingAddressFormResult().isSuccess() || !result.getBillingAddressFormResult().isSuccess() || !result.getShipmentMethodFormResult().isSuccess()) {
            return false;
        }
        return true;
    }

    /**
	 * ...
	 * 
	 * @param command
	 *            the <code>Command</code>
	 * @param obj
	 *            the <code>Object</code>
	 * @param conf
	 *            the <code>FormConfiguration</code>
	 * @param prefix
	 *            the request parameter name prefix
	 * @return the <code>ExtendedFormResult</code>
	 */
    private static ExtendedFormResult parseForm(final Command command, final Object obj, final FormConfiguration conf, final String prefix) {
        Collection<FormFieldResult> ffResults = new ArrayList<FormFieldResult>();
        boolean success = true;
        FormFieldResult ffResult = null;
        for (FormFieldConfiguration ffConf : conf.getFormFieldConfigurations()) {
            ffResult = parseFormField(command, obj, ffConf, prefix);
            ffResults.add(ffResult);
            if (!ffResult.getStatus().equals(FormFieldStatus.OK)) {
                success = false;
            }
        }
        return new ExtendedFormResult(success, ffResults);
    }

    /**
	 * ...
	 * 
	 * @param command
	 *            the <code>Command</code>
	 * @param obj
	 *            the <code>Object</code>
	 * @param conf
	 *            the <code>FormFieldConfiguration</code>
	 * @param prefix
	 *            the request parameter name prefix
	 * @return the <code>FormFieldResult</code>
	 */
    private static FormFieldResult parseFormField(final Command command, final Object obj, final FormFieldConfiguration conf, final String prefix) {
        String prefixString = prefix;
        if (prefixString == null) {
            prefixString = "";
        }
        String oldFieldValue = getFieldValue(obj, conf.getName());
        Parameter param = command.getParameter(prefixString + conf.getName());
        FormFieldResult result = new FormFieldResult();
        if (((param == null) || StringUtils.isBlank(param.getFirstValue())) && conf.isRequired()) {
            result.setName(conf.getName());
            result.setStatus(FormFieldStatus.REQUIRED);
            result.setValue(oldFieldValue);
            result.setInvalidValue(null);
        } else if (((param == null) || StringUtils.isBlank(param.getFirstValue())) && !conf.isRequired()) {
            result.setName(conf.getName());
            result.setStatus(FormFieldStatus.OK);
            result.setValue(null);
            result.setInvalidValue(null);
            setFieldValue(obj, conf.getName(), null);
        } else if ((conf.getRegularExpression() != null) && !param.getFirstValue().matches(conf.getRegularExpression())) {
            result.setName(conf.getName());
            result.setStatus(FormFieldStatus.REGEX_FAILURE);
            result.setValue(oldFieldValue);
            result.setInvalidValue(param.getFirstValue());
        } else {
            result.setName(conf.getName());
            result.setStatus(FormFieldStatus.OK);
            result.setValue(param.getFirstValue());
            result.setInvalidValue(null);
            setFieldValue(obj, conf.getName(), param.getFirstValue());
        }
        return result;
    }

    /**
	 * Returns the value of the declared <code>Field</code> with the given name
	 * from the given <code>Object</code>.
	 * 
	 * @param obj
	 *            the <code>Object</code>
	 * @param name
	 *            the field name
	 * @return the field value
	 * @throws ConfigurationException
	 *             if the given <code>Object</code> does not have a declared
	 *             field with the given name, or if the field is not accessible.
	 */
    private static String getFieldValue(final Object obj, final String name) {
        if (obj == null) {
            return null;
        }
        try {
            Field field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            String value = (String) field.get(obj);
            field.setAccessible(false);
            return value;
        } catch (NoSuchFieldException e) {
            throw new ConfigurationException(e.getLocalizedMessage(), e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException(e.getLocalizedMessage(), e);
        }
    }

    /**
	 * Sets the value of the declared <code>Field</code> with the given name in
	 * the given <code>Object</code> to the given value.
	 * 
	 * @param obj
	 *            the <code>Object</code>
	 * @param name
	 *            the field name
	 * @param value
	 *            the field value to set
	 * @throws ConfigurationException
	 *             if the given <code>Object</code> does not have a declared
	 *             field with the given name, or if the field is not accessible
	 */
    private static void setFieldValue(final Object obj, final String name, final String value) {
        try {
            Field field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(obj, value);
            field.setAccessible(false);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException(e.getLocalizedMessage(), e);
        } catch (NoSuchFieldException e) {
            throw new ConfigurationException(e.getLocalizedMessage(), e);
        }
    }

    /**
	 * ...
	 * 
	 * @param command
	 *            the <code>Command</code>
	 * @param state
	 *            the <code>PPECheckoutState</code>
	 * @return the <code>ExtendedFormResult</code>
	 */
    private static ExtendedFormResult setShipmentMethod(final Command command, final PPECheckoutState state) {
        Order order = state.getOrder();
        AbstractShipmentMethod oldShipMeth = order.getShipmentMethod();
        String oldIdCode = null;
        if (oldShipMeth != null) {
            oldIdCode = oldShipMeth.getIdCode();
        }
        AbstractShipmentMethod newShipMeth = null;
        String newIdCode = null;
        Parameter param = command.getParameter("shipmentMethod");
        if ((param != null) && (param.getFirstValue() != null)) {
            newIdCode = param.getFirstValue();
            for (BasicShipmentMethodConfiguration conf : state.getShipmentMethodConfs()) {
                if (newIdCode.equals(conf.getIdCode())) {
                    try {
                        newShipMeth = (AbstractShipmentMethod) Class.forName(conf.getShipmentMethodClassName()).newInstance();
                    } catch (Exception e) {
                        throw new PulseException(e.getLocalizedMessage(), e);
                    }
                    newShipMeth.init(conf);
                    order.setShipmentMethod(newShipMeth);
                    break;
                }
            }
        }
        Collection<FormFieldResult> ffResults = new ArrayList<FormFieldResult>(1);
        if (newIdCode == null) {
            ffResults.add(new FormFieldResult("shipmentMethod", FormFieldStatus.REQUIRED, oldIdCode, null));
            return new ExtendedFormResult(false, ffResults);
        }
        if (newShipMeth == null) {
            ffResults.add(new FormFieldResult("shipmentMethod", FormFieldStatus.REGEX_FAILURE, oldIdCode, newIdCode));
            return new ExtendedFormResult(false, ffResults);
        }
        ffResults.add(new FormFieldResult("shipmentMethod", FormFieldStatus.OK, newIdCode, null));
        return new ExtendedFormResult(true, ffResults);
    }

    /**
	 * Finalizes the PayPal™ Express checkout by calling the
	 * "DoExtpressCheckoutPayment" service method.
	 * 
	 * @param state
	 *            the <code>PPECheckoutState</code>
	 * @param config
	 *            the <code>PPECheckoutLocDepConfiguration</code>
	 * @return a <code>boolean</code> determining whether the method call was
	 *         successful
	 */
    private static boolean doDoExpressCheckoutPayment(final PPECheckoutState state, final PPECheckoutLocDepConfiguration config) {
        Order order = state.getOrder();
        long total = getTotal(order);
        Map<String, String> params = new HashMap<String, String>();
        params.put("METHOD", "DoExpressCheckoutPayment");
        params.put("TOKEN", state.getToken());
        params.put("PAYERID", state.getPayerId());
        params.put("PAYMENTACTION", "Sale");
        params.put("AMT", BigDecimal.valueOf(total).scaleByPowerOfTen(-order.getCurrency().getDefaultFractionDigits()).toPlainString());
        params.put("CURRENCYCODE", order.getCurrency().getCurrencyCode());
        if (order.getEmailAddress() != null) {
            params.put("EMAIL", order.getEmailAddress());
        }
        params.put("LOCALECODE", order.getLocale().getCountry());
        Address address = order.getShippingAddress();
        params.put("SHIPTONAME", address.getFirstName() + ' ' + address.getLastName());
        params.put("SHIPTOSTREET", address.getStreet());
        if (address.getStreetExtension() != null) {
            params.put("SHIPTOSTREET2", address.getStreetExtension());
        }
        params.put("SHIPTOZIP", address.getPostalCode());
        params.put("SHIPTOCITY", address.getCity());
        params.put("SHIPTOCOUNTRYCODE", address.getCountry());
        Map<String, String> result = execPayPalNVPCall(params, config);
        if (result.get("ACK").equalsIgnoreCase("success")) {
            state.setToken(result.get("TOKEN"));
            state.setPayerId(result.get("PAYERID"));
            if (result.get("REDIRECTREQUIRED") != null) {
                state.setRedirectRequired(result.get("REDIRECTREQUIRED").equalsIgnoreCase("true"));
            }
            ((PPEPayment) order.getPaymentMethod()).setPaymentData(total, order.getCurrency(), PaymentStatus.CAPTURED, result.get("TRANSACTIONID"));
            return true;
        }
        return false;
    }

    /**
	 * Returns the total from the given <code>Order</code>.
	 * 
	 * <p>
	 * If the given <code>Order</code> is net based, the net total is returned.
	 * If the given <code>Order</code> is gross based, the gross total is
	 * returned. Extra charges (e.g. for shipment or payment) are included in
	 * the total.
	 * </p>
	 * 
	 * @param order
	 *            the <code>Order</code>
	 * @return the total
	 */
    private static long getTotal(final Order order) {
        long total = getOrderTotal(order);
        if ((order.getShipmentMethod() != null) && (order.getShipmentMethod().getExtraCharge() != null)) {
            total += order.getShipmentMethod().getExtraCharge().getPrice();
        }
        if ((order.getPaymentMethod() != null) && (order.getPaymentMethod().getExtraCharge() != null)) {
            total += order.getPaymentMethod().getExtraCharge().getPrice();
        }
        return total;
    }

    /**
	 * Returns the URL-<code>String</code> for the giropay redirect.
	 * 
	 * @param token
	 *            the token to be appended as a parameter
	 * @return the URL-<code>String</code> for the redirect
	 * @throws NullPointerException
	 *             if the given token is <code>null</code>
	 */
    private static String createGiropayRedirectURL(final String token) {
        if (token == null) {
            throw new NullPointerException("The given token is null.");
        }
        return new StringBuilder().append("https://www.sandbox.paypal.com/cgi-bin/webscr?").append("cmd=_complete-express-checkout&token=").append(token).toString();
    }

    /**
	 * Saves the <code>Order</code>, sends the order/confirmation e-mails,
	 * clears the <code>ShoppingCart</code>, and removes the
	 * <code>PPECheckoutState</code> from the <code>ServiceSession</code>.
	 * 
	 * @param bundle
	 *            the <code>Bundle</code>
	 * @param request
	 *            the <code>ServiceRequest</code>
	 * @param order
	 *            the <code>Order</code>
	 * @param config
	 *            the <code>PPECheckoutLocDepConfiguration</code>
	 */
    private static void completeProcess(final Bundle bundle, final ServiceRequest request, final Order order, final PPECheckoutLocDepConfiguration config) {
        order.setEndDate();
        saveOrder(order);
        sendEmail(order, false, config.getOrderEmailConf());
        sendEmail(order, true, config.getConfirmationEmailConf());
        ShoppingCart.getCart(bundle, request).clear();
        removePPECheckoutState(bundle, request.getLocale(), request.getSession());
    }

    /**
	 * Saves the given <code>Order</code>.
	 * 
	 * @param order
	 *            the <code>Order</code> to save
	 */
    private static void saveOrder(final Order order) {
        Session sess = Lifecycle.getHibernateDataSource().createNewSession();
        Transaction trans = null;
        try {
            trans = sess.beginTransaction();
            sess.save(order);
            trans.commit();
        } catch (Exception e) {
            if (trans != null) {
                trans.rollback();
            }
            throw new PulseException(e.getLocalizedMessage(), e);
        } finally {
            sess.close();
        }
    }

    /**
	 * Sends an order/confirmation email for the given <code>Order</code>, based
	 * on the given <code>EmailConfiguration</code>.
	 * 
	 * @param order
	 *            the <code>Order</code>
	 * @param flag
	 *            a flag indicating whether the email-address from the
	 *            <code>Order</code> is to be added to the recipients
	 * @param config
	 *            the <code>PPECheckoutLocDepConfiguration</code>
	 */
    private static void sendEmail(final Order order, final boolean flag, final EmailConfiguration config) {
        String text = null;
        if (config.getText() != null) {
            StringWriter writer = new StringWriter();
            XSLTOutputter.marshalAndTransform(order, config.getText().getXSLHandle(), new StreamResult(writer));
            text = writer.toString();
        }
        String html = null;
        if (config.getHtml() != null) {
            StringWriter writer = new StringWriter();
            XSLTOutputter.marshalAndTransform(order, config.getHtml().getXSLHandle(), new StreamResult(writer));
            html = writer.toString();
        }
        ArrayList<InternetAddress> senders = new ArrayList<InternetAddress>();
        for (EmailAddressConfiguration conf : config.getSenders()) {
            senders.add(conf.getInternetAddress());
        }
        ArrayList<InternetAddress> recipients = new ArrayList<InternetAddress>();
        for (EmailAddressConfiguration conf : config.getRecipients()) {
            recipients.add(conf.getInternetAddress());
        }
        if (flag) {
            try {
                recipients.add(0, new InternetAddress(order.getEmailAddress()));
            } catch (AddressException e) {
                throw new PulseException(e.getLocalizedMessage(), e);
            }
        }
        for (InternetAddress recipient : recipients) {
            Lifecycle.getMailQueue().add(buildEmail(recipient, config.getSubject(), text, html, senders));
        }
    }

    /**
	 * Builds an <code>Email</code>.
	 * 
	 * @param recipient
	 *            the recipient
	 * @param subject
	 *            the subject
	 * @param text
	 *            the text-part
	 * @param html
	 *            the HTML-part
	 * @param senders
	 *            the senders
	 * 
	 * @return the <code>Email</code>
	 */
    private static Email buildEmail(final InternetAddress recipient, final String subject, final String text, final String html, final List<InternetAddress> senders) {
        Email email = new Email();
        email.setRecipient(recipient);
        email.setSubject(subject);
        email.setFrom(senders);
        if (text != null) {
            email.setTextContent(text);
        }
        if (html != null) {
            email.setHTMLContent(html);
        }
        return email;
    }
}
