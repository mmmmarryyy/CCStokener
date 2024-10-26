package org.torweg.pulse.component.shop.checkout.payment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.torweg.pulse.annotations.Action;
import org.torweg.pulse.annotations.Action.Security;
import org.torweg.pulse.annotations.Groups;
import org.torweg.pulse.annotations.Permission;
import org.torweg.pulse.bundle.Bundle;
import org.torweg.pulse.bundle.Controller;
import org.torweg.pulse.component.shop.checkout.PaymentControllerResult;
import org.torweg.pulse.component.shop.checkout.PaymentControllerStatus;
import org.torweg.pulse.component.shop.checkout.model.Order;
import org.torweg.pulse.component.shop.checkout.model.Order.OrderBuilder;
import org.torweg.pulse.component.shop.checkout.model.PaymentData.PaymentStatus;
import org.torweg.pulse.component.shop.checkout.payment.PayPalPaymentControllerConfiguration.PayPalPaymentControllerLocaleConfiguration;
import org.torweg.pulse.component.shop.checkout.payment.model.PayPalPaymentData;
import org.torweg.pulse.configuration.Configurable;
import org.torweg.pulse.service.PulseException;
import org.torweg.pulse.service.event.RedirectEvent;
import org.torweg.pulse.service.request.Command;
import org.torweg.pulse.service.request.ServiceRequest;
import org.torweg.pulse.util.entity.IAddress;

/**
 * The {@code PayPalPaymentController}.
 * 
 * @author Christian Schatt
 * @version $Revision: 2463 $
 */
public final class PayPalPaymentController extends Controller implements Configurable<PayPalPaymentControllerConfiguration> {

    /**
	 * The {@code Logger}.
	 */
    private static final Logger LOGGER = LoggerFactory.getLogger(PayPalPaymentController.class);

    /**
	 * The {@code PayPalPaymentControllerConfiguration}.
	 */
    private PayPalPaymentControllerConfiguration configuration;

    /**
	 * Start the PayPal™ payment authorization.
	 * 
	 * @param bundle
	 *            the {@code Bundle}.
	 * @param request
	 *            the {@code ServiceRequest}.
	 * @return the {@code PaymentControllerResult}.
	 */
    @Action(value = "startPayPalPaymentAuthorization", generate = true, security = Security.ALWAYS, stripSitemapID = false)
    @Permission("startPayPalPaymentAuthorization")
    @Groups(values = { "checkout" })
    public PaymentControllerResult startAuthorization(final Bundle bundle, final ServiceRequest request) {
        String actionName = "startAuthorization";
        LOGGER.info("Started '{}'.", actionName);
        OrderBuilder orderBuilder = OrderBuilder.getFromSession(bundle, request.getLocale(), request.getSession());
        PaymentControllerStatus status = PaymentControllerUtils.validateForPaymentAction(bundle, request, PayPalPaymentData.class);
        if (!status.equals(PaymentControllerStatus.OK)) {
            return new PaymentControllerResult(actionName, status, orderBuilder);
        }
        PayPalPaymentData paymentData = (PayPalPaymentData) orderBuilder.getPaymentData();
        if (paymentData.getStatus().equals(PaymentStatus.CAPTURED) || paymentData.getStatus().equals(PaymentStatus.PENDING) || paymentData.getStatus().equals(PaymentStatus.BOOKED_BACK)) {
            LOGGER.warn("The PaymentStatus of the OrderBuilder's PaymentData is invalid.");
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_STATUS_INVALID, orderBuilder);
        }
        paymentData.setAmount(PaymentControllerUtils.getAmountToPay(orderBuilder));
        paymentData.setCurrency(orderBuilder.getTotalPrice().getCurrency());
        paymentData.setTransactionId(generateTransactionId(request.getLocale(), orderBuilder));
        paymentData.setEmailAddress(orderBuilder.getCustomer().getEmailAddress());
        paymentData.setStatus(PaymentStatus.AUTHORIZATION_STARTED);
        setExpressCheckout(request, orderBuilder);
        if (!paymentData.getStatus().equals(PaymentStatus.AUTHORIZATION_STARTED)) {
            PaymentControllerUtils.setRedirectEvent(request, paymentData.getAuthorizationFailedAction());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.FAILED, orderBuilder);
        }
        String redirectURL = createPaymentRedirectURL(request.getLocale(), paymentData.getToken());
        RedirectEvent redirect = new RedirectEvent(redirectURL);
        redirect.setTemporaryRedirect(true);
        request.getEventManager().addEvent(redirect);
        LOGGER.debug("Added a RedirectEvent to '{}' to the EventManager.", redirectURL);
        LOGGER.info("Finished '{}' successfully.", actionName);
        return new PaymentControllerResult(actionName, PaymentControllerStatus.OK, orderBuilder);
    }

    /**
	 * Finish the PayPal™ payment authorization.
	 * 
	 * @param bundle
	 *            the {@code Bundle}.
	 * @param request
	 *            the {@code ServiceRequest}.
	 * @return the {@code PaymentControllerResult}.
	 */
    @Action(value = "finishPayPalPaymentAuthorization", generate = true, security = Security.ALWAYS, stripSitemapID = false)
    @Permission("finishPayPalPaymentAuthorization")
    @Groups(values = { "checkout" })
    public PaymentControllerResult finishAuthorization(final Bundle bundle, final ServiceRequest request) {
        String actionName = "finishAuthorization";
        LOGGER.info("Started '{}'.", actionName);
        OrderBuilder orderBuilder = OrderBuilder.getFromSession(bundle, request.getLocale(), request.getSession());
        PaymentControllerStatus status = PaymentControllerUtils.validateForPaymentAction(bundle, request, PayPalPaymentData.class);
        if (!status.equals(PaymentControllerStatus.OK)) {
            return new PaymentControllerResult(actionName, status, orderBuilder);
        }
        PayPalPaymentData paymentData = (PayPalPaymentData) orderBuilder.getPaymentData();
        if (!paymentData.getStatus().equals(PaymentStatus.AUTHORIZATION_STARTED)) {
            LOGGER.warn("The PaymentStatus of the OrderBuilder's PaymentData is invalid.");
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_STATUS_INVALID, orderBuilder);
        }
        getExpressCheckoutDetails(request, orderBuilder);
        if (!paymentData.getStatus().equals(PaymentStatus.AUTHORIZED)) {
            PaymentControllerUtils.setRedirectEvent(request, paymentData.getAuthorizationFailedAction());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.FAILED, orderBuilder);
        }
        PaymentControllerUtils.setRedirectEvent(request, paymentData.getAuthorizationFinishedAction());
        LOGGER.info("Finished '{}' successfully.", actionName);
        return new PaymentControllerResult(actionName, PaymentControllerStatus.OK, orderBuilder);
    }

    /**
	 * Start the PayPal™ payment capture.
	 * 
	 * @param bundle
	 *            the {@code Bundle}.
	 * @param request
	 *            the {@code ServiceRequest}.
	 * @return the {@code PaymentControllerResult}.
	 */
    @Action(value = "startPayPalPaymentCapture", generate = true, security = Security.ALWAYS, stripSitemapID = false)
    @Permission("startPayPalPaymentCapture")
    @Groups(values = { "checkout" })
    public PaymentControllerResult startCapture(final Bundle bundle, final ServiceRequest request) {
        String actionName = "startCapture";
        LOGGER.info("Started '{}'.", actionName);
        OrderBuilder orderBuilder = OrderBuilder.getFromSession(bundle, request.getLocale(), request.getSession());
        PaymentControllerStatus status = PaymentControllerUtils.validateForPaymentAction(bundle, request, PayPalPaymentData.class);
        if (!status.equals(PaymentControllerStatus.OK)) {
            return new PaymentControllerResult(actionName, status, orderBuilder);
        }
        PayPalPaymentData paymentData = (PayPalPaymentData) orderBuilder.getPaymentData();
        if (!paymentData.getStatus().equals(PaymentStatus.AUTHORIZED)) {
            LOGGER.warn("The PaymentStatus of the OrderBuilder's PaymentData is invalid.");
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_STATUS_INVALID, orderBuilder);
        }
        paymentData.setStatus(PaymentStatus.CAPTURE_STARTED);
        doExpressCheckoutPayment(request, orderBuilder);
        if (!paymentData.getStatus().equals(PaymentStatus.CAPTURED) && !paymentData.getStatus().equals(PaymentStatus.PENDING)) {
            PaymentControllerUtils.setRedirectEvent(request, paymentData.getCaptureFailedAction());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.FAILED, orderBuilder);
        }
        if (paymentData.isRedirectRequired()) {
            String redirectURL = createGiropayRedirectURL(request.getLocale(), paymentData.getToken());
            RedirectEvent redirect = new RedirectEvent(redirectURL);
            redirect.setTemporaryRedirect(true);
            request.getEventManager().addEvent(redirect);
            LOGGER.debug("Added a RedirectEvent to '{}' to the EventManager.", redirectURL);
            LOGGER.info("Finished '{}' successfully.", actionName);
            return new PaymentControllerResult(actionName, PaymentControllerStatus.OK, orderBuilder);
        } else {
            orderBuilder.getOrderMetaData().setOrderDate(new Date());
            Order order = PaymentControllerUtils.saveOrder(orderBuilder.build());
            Order.putIdIntoSession(order, request.getSession());
            OrderBuilder.removeFromSession(bundle, request.getLocale(), request.getSession());
            PaymentControllerUtils.setRedirectEvent(request, paymentData.getCaptureFinishedAction(), "orderId", order.getId().toString());
            LOGGER.info("Finished '{}' successfully.", actionName);
            return new PaymentControllerResult(actionName, PaymentControllerStatus.OK, order);
        }
    }

    /**
	 * Finish the PayPal™ payment capture with giropay.
	 * 
	 * @param bundle
	 *            the {@code Bundle}.
	 * @param request
	 *            the {@code ServiceRequest}.
	 * @return the {@code PaymentControllerResult}.
	 */
    @Action(value = "finishPayPalPaymentCaptureWithGiropay", generate = true, security = Security.ALWAYS, stripSitemapID = false)
    @Permission("finishPayPalPaymentCaptureWithGiropay")
    @Groups(values = { "checkout" })
    public PaymentControllerResult finishCaptureWithGiropay(final Bundle bundle, final ServiceRequest request) {
        String actionName = "finishCaptureWithGiropay";
        LOGGER.info("Started '{}'.", actionName);
        OrderBuilder orderBuilder = OrderBuilder.getFromSession(bundle, request.getLocale(), request.getSession());
        PaymentControllerStatus status = PaymentControllerUtils.validateForPaymentAction(bundle, request, PayPalPaymentData.class);
        if (!status.equals(PaymentControllerStatus.OK)) {
            return new PaymentControllerResult(actionName, status, orderBuilder);
        }
        PayPalPaymentData paymentData = (PayPalPaymentData) orderBuilder.getPaymentData();
        if (!paymentData.getStatus().equals(PaymentStatus.PENDING)) {
            LOGGER.warn("The PaymentStatus of the OrderBuilder's PaymentData is invalid.");
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_STATUS_INVALID, orderBuilder);
        }
        paymentData.setPaymentDate(new Date());
        if (request.getCommand().getParameter("BANKTXNPENDING") == null) {
            paymentData.setStatus(PaymentStatus.CAPTURED);
        }
        orderBuilder.getOrderMetaData().setOrderDate(new Date());
        Order order = PaymentControllerUtils.saveOrder(orderBuilder.build());
        Order.putIdIntoSession(order, request.getSession());
        OrderBuilder.removeFromSession(bundle, request.getLocale(), request.getSession());
        PaymentControllerUtils.setRedirectEvent(request, paymentData.getCaptureFinishedAction(), "orderId", order.getId().toString());
        LOGGER.info("Finished '{}' successfully.", actionName);
        return new PaymentControllerResult(actionName, PaymentControllerStatus.OK, order);
    }

    /**
	 * Cancel the PayPal™ payment.
	 * 
	 * @param bundle
	 *            the {@code Bundle}
	 * @param request
	 *            the {@code ServiceRequest}
	 * @return the {@code PaymentControllerResult}
	 */
    @Action(value = "cancelPayPalPayment", generate = true, security = Security.ALWAYS, stripSitemapID = false)
    @Permission("cancelPayPalPayment")
    @Groups(values = { "checkout" })
    public PaymentControllerResult cancel(final Bundle bundle, final ServiceRequest request) {
        String actionName = "cancel";
        LOGGER.info("Started '{}'.", actionName);
        OrderBuilder orderBuilder = OrderBuilder.getFromSession(bundle, request.getLocale(), request.getSession());
        PaymentControllerStatus status = PaymentControllerUtils.validateForPaymentAction(bundle, request, PayPalPaymentData.class);
        if (!status.equals(PaymentControllerStatus.OK)) {
            return new PaymentControllerResult(actionName, status, orderBuilder);
        }
        PayPalPaymentData paymentData = (PayPalPaymentData) orderBuilder.getPaymentData();
        if (paymentData.getStatus().equals(PaymentStatus.CAPTURED) || paymentData.getStatus().equals(PaymentStatus.BOOKED_BACK)) {
            LOGGER.warn("The PaymentStatus of the OrderBuilder's PaymentData is invalid.");
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_STATUS_INVALID, orderBuilder);
        }
        if (paymentData.getStatus().equals(PaymentStatus.AUTHORIZATION_STARTED) || paymentData.getStatus().equals(PaymentStatus.AUTHORIZATION_REJECTED) || paymentData.getStatus().equals(PaymentStatus.AUTHORIZATION_INTERRUPTED) || paymentData.getStatus().equals(PaymentStatus.AUTHORIZATION_CANCELED) || paymentData.getStatus().equals(PaymentStatus.AUTHORIZED)) {
            paymentData.setStatus(PaymentStatus.AUTHORIZATION_CANCELED);
            PaymentControllerUtils.setRedirectEvent(request, paymentData.getAuthorizationCanceledAction());
        } else if (paymentData.getStatus().equals(PaymentStatus.CAPTURE_STARTED) || paymentData.getStatus().equals(PaymentStatus.CAPTURE_REJECTED) || paymentData.getStatus().equals(PaymentStatus.CAPTURE_INTERRUPTED) || paymentData.getStatus().equals(PaymentStatus.CAPTURE_CANCELED) || paymentData.getStatus().equals(PaymentStatus.PENDING)) {
            paymentData.setStatus(PaymentStatus.CAPTURE_CANCELED);
            PaymentControllerUtils.setRedirectEvent(request, paymentData.getCaptureCanceledAction());
        }
        LOGGER.info("Finished '{}' successfully.", actionName);
        return new PaymentControllerResult(actionName, PaymentControllerStatus.OK, orderBuilder);
    }

    /**
	 * Initializes the {@code PayPalPaymentController} with the given
	 * {@code PayPalPaymentControllerConfiguration}.
	 * 
	 * @param c
	 *            the {@code PayPalPaymentControllerConfiguration}.
	 * @throws IllegalArgumentException
	 *             if the given {@code PayPalPaymentControllerConfiguration} is
	 *             {@code null}.
	 */
    public void initialize(final PayPalPaymentControllerConfiguration c) {
        if (c == null) {
            throw new IllegalArgumentException("The given PayPalPaymentControllerConfiguration is null.");
        }
        this.configuration = c;
    }

    /**
	 * Generates the transaction id for the given {@code Locale} and
	 * {@code OrderBuilder}.
	 * 
	 * @param locale
	 *            the {@code Locale}
	 * @param orderBuilder
	 *            the {@code OrderBuilder}
	 * 
	 * @return the transaction id
	 * 
	 * @throws PulseException
	 *             if an {@code IllegalAccessException}, an
	 *             {@code InstantiationException} or a
	 *             {@code ClassNotFoundException} is thrown within the method
	 */
    private String generateTransactionId(final Locale locale, final OrderBuilder orderBuilder) {
        try {
            return ((ITransactionIdGenerator) Class.forName(this.configuration.getLocaleConfiguration(locale).getTransactionIdGeneratorClass()).newInstance()).generateTransactionId(orderBuilder);
        } catch (IllegalAccessException e) {
            throw new PulseException("Error: " + e.getLocalizedMessage(), e);
        } catch (InstantiationException e) {
            throw new PulseException("Error: " + e.getLocalizedMessage(), e);
        } catch (ClassNotFoundException e) {
            throw new PulseException("Error: " + e.getLocalizedMessage(), e);
        }
    }

    /**
	 * Starts the PayPal™ Express checkout by calling the "SetExtpressCheckout"
	 * service method.
	 * 
	 * @param request
	 *            the {@code ServiceRequest}.
	 * @param orderBuilder
	 *            the {@code OrderBuilder}.
	 */
    private void setExpressCheckout(final ServiceRequest request, final OrderBuilder orderBuilder) {
        PayPalPaymentControllerLocaleConfiguration config = this.configuration.getLocaleConfiguration(request.getLocale());
        PayPalPaymentData paymentData = (PayPalPaymentData) orderBuilder.getPaymentData();
        try {
            Map<String, String> params = new HashMap<String, String>();
            params.put("METHOD", "SetExpressCheckout");
            params.put("RETURNURL", createReturnURL(request));
            params.put("CANCELURL", createCancelURL(request));
            params.put("NOSHIPPING", "1");
            params.put("ALLOWNOTE", "0");
            params.put("LOCALECODE", orderBuilder.getOrderMetaData().getLocale().getCountry());
            if (config.getHdrImg() != null) {
                params.put("HDRIMG", config.getHdrImg());
            }
            if (config.getHdrBorderColor() != null) {
                params.put("HDRBORDERCOLOR", config.getHdrBorderColor());
            }
            if (config.getHdrBackColor() != null) {
                params.put("HDRBACKCOLOR", config.getHdrBackColor());
            }
            if (config.getPayflowColor() != null) {
                params.put("PAYFLOWCOLOR", config.getPayflowColor());
            }
            params.put("EMAIL", paymentData.getEmailAddress());
            params.put("GIROPAYSUCCESSURL", createGiropaySuccessURL(request));
            params.put("GIROPAYCANCELURL", createGiropayCancelURL(request));
            params.put("BANKTXNPENDINGURL", createBanktxnPendingURL(request));
            params.put("PAYMENTREQUEST_0_AMT", paymentData.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
            params.put("PAYMENTREQUEST_0_CURRENCYCODE", paymentData.getCurrency().getCurrencyCode());
            params.put("PAYMENTREQUEST_0_INVNUM", paymentData.getTransactionId());
            Map<String, String> result = execPayPalNVPCall(params, config);
            if (!result.get("ACK").matches("^Success(?:WithWarning)?$")) {
                paymentData.setStatus(PaymentStatus.AUTHORIZATION_REJECTED);
                return;
            }
            paymentData.setToken(result.get("TOKEN"));
        } catch (Exception e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
            paymentData.setStatus(PaymentStatus.AUTHORIZATION_INTERRUPTED);
            PaymentControllerUtils.sendErrorReport(this, e, config.getErrorReportConfiguration());
        }
    }

    /**
	 * Collects the PayPal™ Express checkout data provided by the PayPal™ user
	 * by calling the "GetExtpressCheckoutDetails" service method.
	 * 
	 * @param request
	 *            the {@code ServiceRequest}.
	 * @param orderBuilder
	 *            the {@code OrderBuilder}.
	 */
    private void getExpressCheckoutDetails(final ServiceRequest request, final OrderBuilder orderBuilder) {
        PayPalPaymentControllerLocaleConfiguration config = this.configuration.getLocaleConfiguration(request.getLocale());
        PayPalPaymentData paymentData = (PayPalPaymentData) orderBuilder.getPaymentData();
        try {
            Map<String, String> params = new HashMap<String, String>();
            params.put("METHOD", "GetExpressCheckoutDetails");
            params.put("TOKEN", paymentData.getToken());
            Map<String, String> result = execPayPalNVPCall(params, config);
            if (!result.get("ACK").matches("^Success(?:WithWarning)?$")) {
                paymentData.setStatus(PaymentStatus.AUTHORIZATION_REJECTED);
                return;
            }
            paymentData.setToken(result.get("TOKEN"));
            if (result.get("REDIRECTREQUIRED") != null) {
                paymentData.setRedirectRequired(result.get("REDIRECTREQUIRED").equalsIgnoreCase("true"));
            }
            paymentData.setEmailAddress(result.get("EMAIL"));
            paymentData.setPayerId(result.get("PAYERID"));
            paymentData.setPaymentRedirectURL(createPaymentRedirectURL(request.getLocale(), paymentData.getToken()));
            paymentData.setStatus(PaymentStatus.AUTHORIZED);
        } catch (Exception e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
            paymentData.setStatus(PaymentStatus.AUTHORIZATION_INTERRUPTED);
            PaymentControllerUtils.sendErrorReport(this, e, config.getErrorReportConfiguration());
        }
    }

    /**
	 * Finalizes the PayPal™ Express checkout by calling the
	 * "DoExpressCheckoutPayment" service method.
	 * 
	 * @param request
	 *            the {@code ServiceRequest}.
	 * @param orderBuilder
	 *            the {@code OrderBuilder}.
	 */
    private void doExpressCheckoutPayment(final ServiceRequest request, final OrderBuilder orderBuilder) {
        PayPalPaymentControllerLocaleConfiguration config = this.configuration.getLocaleConfiguration(request.getLocale());
        PayPalPaymentData paymentData = (PayPalPaymentData) orderBuilder.getPaymentData();
        try {
            Map<String, String> params = new HashMap<String, String>();
            params.put("METHOD", "DoExpressCheckoutPayment");
            params.put("TOKEN", paymentData.getToken());
            params.put("PAYERID", paymentData.getPayerId());
            params.put("PAYMENTREQUEST_0_AMT", paymentData.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
            params.put("PAYMENTREQUEST_0_CURRENCYCODE", paymentData.getCurrency().getCurrencyCode());
            params.put("PAYMENTREQUEST_0_INVNUM", paymentData.getTransactionId());
            params.putAll(createDeliveryAddressParams(orderBuilder.getDeliveryAddress()));
            Map<String, String> result = execPayPalNVPCall(params, config);
            if (!result.get("ACK").matches("^Success(?:WithWarning)?$")) {
                paymentData.setStatus(PaymentStatus.CAPTURE_REJECTED);
                return;
            }
            paymentData.setToken(result.get("TOKEN"));
            if (result.get("REDIRECTREQUIRED") != null) {
                paymentData.setRedirectRequired(result.get("REDIRECTREQUIRED").equalsIgnoreCase("true"));
            }
            paymentData.setProviderTransactionId(result.get("PAYMENTREQUEST_0_TRANSACTIONID"));
            if (paymentData.isRedirectRequired()) {
                paymentData.setStatus(PaymentStatus.PENDING);
            } else {
                paymentData.setPaymentDate(new Date(System.currentTimeMillis()));
                paymentData.setStatus(PaymentStatus.CAPTURED);
            }
        } catch (Exception e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
            paymentData.setStatus(PaymentStatus.AUTHORIZATION_INTERRUPTED);
            PaymentControllerUtils.sendErrorReport(this, e, config.getErrorReportConfiguration());
        }
    }

    /**
	 * Creates the delivery address parameters.
	 * 
	 * @param address
	 *            the delivery address
	 * 
	 * @return the parameters
	 */
    private Map<String, String> createDeliveryAddressParams(final IAddress address) {
        Map<String, String> params = new HashMap<String, String>();
        if (address == null) {
            return params;
        }
        StringBuilder builder = new StringBuilder();
        if (address.getFirstName() != null) {
            builder.append(address.getFirstName()).append(' ');
        }
        if (address.getLastName() != null) {
            builder.append(address.getLastName());
        }
        addDeliveryAddressParam(params, "PAYMENTREQUEST_0_SHIPTONAME", builder.toString());
        addDeliveryAddressParam(params, "PAYMENTREQUEST_0_SHIPTOSTREET", address.getStreet());
        addDeliveryAddressParam(params, "PAYMENTREQUEST_0_SHIPTOSTREET2", address.getAdditionalStreetInfo());
        addDeliveryAddressParam(params, "PAYMENTREQUEST_0_SHIPTOZIP", address.getPostalCode());
        addDeliveryAddressParam(params, "PAYMENTREQUEST_0_SHIPTOCITY", address.getCity());
        addDeliveryAddressParam(params, "PAYMENTREQUEST_0_SHIPTOSTATE", address.getRegion());
        addDeliveryAddressParam(params, "PAYMENTREQUEST_0_SHIPTOCOUNTRYCODE", address.getCountry());
        return params;
    }

    /**
	 * Adds the delivery address parameter with the given name and value to the
	 * given {@code Map} holding the parameters.
	 * 
	 * @param params
	 *            the parameters
	 * @param name
	 *            the name of the parameter to add
	 * @param value
	 *            the value of the parameter to add
	 */
    private void addDeliveryAddressParam(final Map<String, String> params, final String name, final String value) {
        if (value == null) {
            params.put(name, "");
        } else {
            params.put(name, value);
        }
    }

    /**
	 * Returns the "ReturnURL"-{@code String}.
	 * 
	 * @param request
	 *            the {@code ServiceRequest}.
	 * @return the "ReturnURL"-{@code String}.
	 */
    private String createReturnURL(final ServiceRequest request) {
        Command command = request.getCommand().createCopy(false);
        command.setAction(this.configuration.getLocaleConfiguration(request.getLocale()).getReturnAction());
        command.setSecurity(Security.ALWAYS);
        return request.getHttpServletResponse().encodeURL(command.toCommandURL(request));
    }

    /**
	 * Returns the "CancelURL"-{@code String}.
	 * 
	 * @param request
	 *            the {@code ServiceRequest}.
	 * @return the "ReturnURL"-{@code String}.
	 */
    private String createCancelURL(final ServiceRequest request) {
        Command command = request.getCommand().createCopy(false);
        command.setAction(this.configuration.getLocaleConfiguration(request.getLocale()).getCancelAction());
        command.setSecurity(Security.ALWAYS);
        return request.getHttpServletResponse().encodeURL(command.toCommandURL(request));
    }

    /**
	 * Returns the "GiropaySuccesssURL"-{@code String}.
	 * 
	 * @param request
	 *            the {@code ServiceRequest}.
	 * @return the "GiropaySuccessURL"-{@code String}.
	 */
    private String createGiropaySuccessURL(final ServiceRequest request) {
        Command command = request.getCommand().createCopy(false);
        command.setAction(this.configuration.getLocaleConfiguration(request.getLocale()).getGiropaySuccessAction());
        command.setSecurity(Security.ALWAYS);
        return request.getHttpServletResponse().encodeURL(command.toCommandURL(request));
    }

    /**
	 * Returns the "GiropayCancelURL"-{@code String}.
	 * 
	 * @param request
	 *            the {@code ServiceRequest}.
	 * @return the "GiropayCancelURL"-{@code String}.
	 */
    private String createGiropayCancelURL(final ServiceRequest request) {
        Command command = request.getCommand().createCopy(false);
        command.setAction(this.configuration.getLocaleConfiguration(request.getLocale()).getGiropayCancelAction());
        command.setSecurity(Security.ALWAYS);
        return request.getHttpServletResponse().encodeURL(command.toCommandURL(request));
    }

    /**
	 * Returns the "BanktxnPendingURL"-{@code String}.
	 * 
	 * @param request
	 *            the {@code ServiceRequest}.
	 * @return the "BanktxnPendingURL"-{@code String}.
	 */
    private String createBanktxnPendingURL(final ServiceRequest request) {
        Command command = request.getCommand().createCopy(false);
        command.setAction(this.configuration.getLocaleConfiguration(request.getLocale()).getBanktxnPendingAction());
        command.setSecurity(Security.ALWAYS);
        command.addParameter("BANKTXNPENDING", "");
        return request.getHttpServletResponse().encodeURL(command.toCommandURL(request));
    }

    /**
	 * Returns the URL-{@code String} for the payment redirect.
	 * 
	 * @param locale
	 *            the {@code Locale}.
	 * @param token
	 *            the token to be appended as a parameter.
	 * @return the URL-{@code String} for the redirect.
	 * @throws IllegalArgumentException
	 *             if the given {@code Locale} or token is {@code null}.
	 */
    private String createPaymentRedirectURL(final Locale locale, final String token) {
        if (locale == null) {
            throw new IllegalArgumentException("The given Locale is null.");
        }
        if (token == null) {
            throw new IllegalArgumentException("The given token is null.");
        }
        return new StringBuilder(this.configuration.getLocaleConfiguration(locale).getPaymentRedirectURL()).append("&token=").append(token).toString();
    }

    /**
	 * Returns the URL-{@code String} for the giropay redirect.
	 * 
	 * @param locale
	 *            the {@code Locale}.
	 * @param token
	 *            the token to be appended as a parameter.
	 * @return the URL-{@code String} for the redirect.
	 * @throws IllegalArgumentException
	 *             if the given {@code Locale} or token is {@code null}.
	 */
    private String createGiropayRedirectURL(final Locale locale, final String token) {
        if (locale == null) {
            throw new IllegalArgumentException("The given Locale is null.");
        }
        if (token == null) {
            throw new IllegalArgumentException("The given token is null.");
        }
        return new StringBuilder(this.configuration.getLocaleConfiguration(locale).getGiropayRedirectURL()).append("&token=").append(token).toString();
    }

    /**
	 * Executes a call to the PayPal™ NVP service.
	 * 
	 * @param params
	 *            the parameter-map to use when calling the PayPal™ NVP service.
	 * @param config
	 *            the {@code PayPalPaymentControllerLocaleConfiguration}.
	 * @return the parameter-map returned by the PayPal™ NVP service.
	 * @throws NullPointerException
	 *             if the given parameter-map or the given
	 *             {@code PayPalPaymentLocaleConfiguration} is {@code null}.
	 * @throws IllegalArgumentException
	 *             if the given parameter-map is empty.
	 */
    private static Map<String, String> execPayPalNVPCall(final Map<String, String> params, final PayPalPaymentControllerLocaleConfiguration config) {
        if (params == null) {
            throw new NullPointerException("The given parameter-map is null.");
        }
        if (params.isEmpty()) {
            throw new IllegalArgumentException("The given parameter-map is empty.");
        }
        if (config == null) {
            throw new NullPointerException("The given PayPalPaymentControllerLocaleConfiguration is null.");
        }
        HttpsURLConnection connection;
        try {
            StringBuilder urlBuilder = new StringBuilder(config.getPayPalURL());
            urlBuilder.append("?USER=").append(URLEncoder.encode(config.getUserName(), "UTF-8"));
            urlBuilder.append("&PWD=").append(URLEncoder.encode(config.getPassword(), "UTF-8"));
            urlBuilder.append("&SIGNATURE=").append(URLEncoder.encode(config.getSignature(), "UTF-8"));
            urlBuilder.append("&VERSION=").append(URLEncoder.encode(config.getVersion(), "UTF-8"));
            for (Map.Entry<String, String> entry : params.entrySet()) {
                urlBuilder.append('&').append(URLEncoder.encode(entry.getKey().toUpperCase(), "UTF-8"));
                urlBuilder.append('=').append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            String urlString = urlBuilder.toString();
            LOGGER.info("Executing a PayPal™ NVP call with the URL '{}'.", urlString);
            connection = (HttpsURLConnection) new URL(urlString).openConnection();
            connection.setConnectTimeout(config.getConnectTimeout());
            connection.setReadTimeout(config.getReadTimeout());
            connection.connect();
        } catch (IOException e) {
            throw new PulseException(e.getLocalizedMessage(), e);
        }
        StringBuilder responseBuilder = new StringBuilder();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String l = in.readLine();
            while (l != null) {
                responseBuilder.append(l);
                l = in.readLine();
                if (l != null) {
                    responseBuilder.append('\n');
                }
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
        LOGGER.info("The answer to the PayPal™ NVP call is '{}'.", response);
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
}
