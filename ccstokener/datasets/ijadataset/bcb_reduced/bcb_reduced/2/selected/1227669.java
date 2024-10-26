package org.torweg.pulse.component.shop.checkout;

import java.io.IOException;
import java.io.InputStream;
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
import org.torweg.pulse.component.shop.checkout.Order.OrderBuilder;
import org.torweg.pulse.component.shop.checkout.PayPalPaymentControllerConfiguration.PayPalPaymentControllerLocaleConfiguration;
import org.torweg.pulse.component.shop.checkout.PaymentData.PaymentStatus;
import org.torweg.pulse.configuration.Configurable;
import org.torweg.pulse.service.PulseException;
import org.torweg.pulse.service.event.RedirectEvent;
import org.torweg.pulse.service.request.Command;
import org.torweg.pulse.service.request.ServiceRequest;

/**
 * The {@code PayPalPaymentController}.
 * 
 * @author Christian Schatt
 * @version $Revision: 2158 $
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
        if (orderBuilder == null) {
            LOGGER.warn("The OrderBuilder is null.");
            return new PaymentControllerResult(actionName, PaymentControllerStatus.ORDER_BUILDER_NULL);
        }
        if (!PaymentControllerUtils.matchUsers(request, orderBuilder)) {
            LOGGER.warn("The current User's id does not match the Customer's user id.");
            OrderBuilder.removeFromSession(bundle, request.getLocale(), request.getSession());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.USER_INVALID);
        }
        if (orderBuilder.getOrderMetaData().getStatus().equals(OrderStatus.FINISHED)) {
            LOGGER.warn("The OrderStatus '{}' is invalid.", orderBuilder.getOrderMetaData().getStatus());
            OrderBuilder.removeFromSession(bundle, request.getLocale(), request.getSession());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.ORDER_STATUS_INVALID, orderBuilder);
        }
        PaymentData data = orderBuilder.getPaymentData();
        if (data == null) {
            LOGGER.warn("The PaymentData is null.");
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_DATA_NULL, orderBuilder);
        }
        if (!(data instanceof PayPalPaymentData)) {
            LOGGER.warn("The PaymentData's type '{}' is illegal.", data.getClass().getCanonicalName());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_DATA_ILLEGAL_TYPE, orderBuilder);
        }
        if (data.getStatus().equals(PaymentStatus.CAPTURED) || data.getStatus().equals(PaymentStatus.PENDING) || data.getStatus().equals(PaymentStatus.BOOKED_BACK)) {
            LOGGER.warn("The PaymentStatus '{}' is invalid.", data.getStatus());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_STATUS_INVALID, orderBuilder);
        }
        PayPalPaymentData paymentData = (PayPalPaymentData) data;
        if (orderBuilder.getOrderDetails().isNetOrder()) {
            paymentData.setAmount(orderBuilder.getTotalPrice().getNetAmount());
        } else {
            paymentData.setAmount(orderBuilder.getTotalPrice().getGrossAmount());
        }
        paymentData.setCurrency(orderBuilder.getTotalPrice().getCurrency());
        paymentData.setTransactionId(PaymentControllerUtils.createTransactionId(orderBuilder.getCustomer().getUserId()));
        paymentData.setEmailAddress(orderBuilder.getCustomer().getEmailAddress());
        paymentData.setStatus(PaymentStatus.AUTHORIZATION_STARTED);
        boolean success = setExpressCheckout(request, orderBuilder);
        if (!success) {
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
        if (orderBuilder == null) {
            LOGGER.warn("The OrderBuilder is null.");
            return new PaymentControllerResult(actionName, PaymentControllerStatus.ORDER_BUILDER_NULL);
        }
        if (!PaymentControllerUtils.matchUsers(request, orderBuilder)) {
            LOGGER.warn("The current User's id does not match the Customer's user id.");
            OrderBuilder.removeFromSession(bundle, request.getLocale(), request.getSession());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.USER_INVALID);
        }
        if (orderBuilder.getOrderMetaData().getStatus().equals(OrderStatus.FINISHED)) {
            LOGGER.warn("The OrderStatus '{}' is invalid.", orderBuilder.getOrderMetaData().getStatus());
            OrderBuilder.removeFromSession(bundle, request.getLocale(), request.getSession());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.ORDER_STATUS_INVALID, orderBuilder);
        }
        PaymentData data = orderBuilder.getPaymentData();
        if (data == null) {
            LOGGER.warn("The PaymentData is null.");
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_DATA_NULL, orderBuilder);
        }
        if (!(data instanceof PayPalPaymentData)) {
            LOGGER.warn("The PaymentData's type '{}' is illegal.", data.getClass().getCanonicalName());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_DATA_ILLEGAL_TYPE, orderBuilder);
        }
        if (!data.getStatus().equals(PaymentStatus.AUTHORIZATION_STARTED)) {
            LOGGER.warn("The PaymentStatus '{}' is invalid.", data.getStatus());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_STATUS_INVALID, orderBuilder);
        }
        PayPalPaymentData paymentData = (PayPalPaymentData) data;
        boolean success = getExpressCheckoutDetails(request, orderBuilder);
        if (!success) {
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
        if (orderBuilder == null) {
            LOGGER.warn("The OrderBuilder is null.");
            return new PaymentControllerResult(actionName, PaymentControllerStatus.ORDER_BUILDER_NULL);
        }
        if (!PaymentControllerUtils.matchUsers(request, orderBuilder)) {
            LOGGER.warn("The current User's id does not match the Customer's user id.");
            OrderBuilder.removeFromSession(bundle, request.getLocale(), request.getSession());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.USER_INVALID);
        }
        if (orderBuilder.getOrderMetaData().getStatus().equals(OrderStatus.FINISHED)) {
            LOGGER.warn("The OrderStatus '{}' is invalid.", orderBuilder.getOrderMetaData().getStatus());
            OrderBuilder.removeFromSession(bundle, request.getLocale(), request.getSession());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.ORDER_STATUS_INVALID, orderBuilder);
        }
        PaymentData data = orderBuilder.getPaymentData();
        if (data == null) {
            LOGGER.warn("The PaymentData is null.");
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_DATA_NULL, orderBuilder);
        }
        if (!(data instanceof PayPalPaymentData)) {
            LOGGER.warn("The PaymentData's type '{}' is illegal.", data.getClass().getCanonicalName());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_DATA_ILLEGAL_TYPE, orderBuilder);
        }
        if (!data.getStatus().equals(PaymentStatus.AUTHORIZED)) {
            LOGGER.warn("The PaymentStatus '{}' is invalid.", data.getStatus());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_STATUS_INVALID, orderBuilder);
        }
        PayPalPaymentData paymentData = (PayPalPaymentData) data;
        paymentData.setStatus(PaymentStatus.CAPTURE_STARTED);
        boolean success = doExpressCheckoutPayment(request, orderBuilder);
        if (!success) {
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
        if (orderBuilder == null) {
            LOGGER.warn("The OrderBuilder is null.");
            return new PaymentControllerResult(actionName, PaymentControllerStatus.ORDER_BUILDER_NULL);
        }
        if (!PaymentControllerUtils.matchUsers(request, orderBuilder)) {
            LOGGER.warn("The current User's id does not match the Customer's user id.");
            OrderBuilder.removeFromSession(bundle, request.getLocale(), request.getSession());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.USER_INVALID);
        }
        if (orderBuilder.getOrderMetaData().getStatus().equals(OrderStatus.FINISHED)) {
            LOGGER.warn("The OrderStatus '{}' is invalid.", orderBuilder.getOrderMetaData().getStatus());
            OrderBuilder.removeFromSession(bundle, request.getLocale(), request.getSession());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.ORDER_STATUS_INVALID, orderBuilder);
        }
        PaymentData data = orderBuilder.getPaymentData();
        if (data == null) {
            LOGGER.warn("The PaymentData is null.");
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_DATA_NULL, orderBuilder);
        }
        if (!(data instanceof PayPalPaymentData)) {
            LOGGER.warn("The PaymentData's type '{}' is illegal.", data.getClass().getCanonicalName());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_DATA_ILLEGAL_TYPE, orderBuilder);
        }
        if (!data.getStatus().equals(PaymentStatus.PENDING)) {
            LOGGER.warn("The PaymentStatus '{}' is invalid.", data.getStatus());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_STATUS_INVALID, orderBuilder);
        }
        PayPalPaymentData paymentData = (PayPalPaymentData) data;
        paymentData.setPaymentDate(new Date());
        paymentData.setStatus(PaymentStatus.CAPTURED);
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
        if (orderBuilder == null) {
            LOGGER.warn("The OrderBuilder is null.");
            return new PaymentControllerResult(actionName, PaymentControllerStatus.ORDER_BUILDER_NULL);
        }
        if (!PaymentControllerUtils.matchUsers(request, orderBuilder)) {
            LOGGER.warn("The current User's id does not match the Customer's user id.");
            OrderBuilder.removeFromSession(bundle, request.getLocale(), request.getSession());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.USER_INVALID);
        }
        if (orderBuilder.getOrderMetaData().getStatus().equals(OrderStatus.FINISHED)) {
            LOGGER.warn("The OrderStatus '{}' is invalid.", orderBuilder.getOrderMetaData().getStatus());
            OrderBuilder.removeFromSession(bundle, request.getLocale(), request.getSession());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.ORDER_STATUS_INVALID, orderBuilder);
        }
        PaymentData data = orderBuilder.getPaymentData();
        if (data == null) {
            LOGGER.warn("The PaymentData is null.");
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_DATA_NULL, orderBuilder);
        }
        if (!(data instanceof PayPalPaymentData)) {
            LOGGER.warn("The PaymentData's type '{}' is illegal.", data.getClass().getCanonicalName());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_DATA_ILLEGAL_TYPE, orderBuilder);
        }
        if (data.getStatus().equals(PaymentStatus.CAPTURED) || data.getStatus().equals(PaymentStatus.BOOKED_BACK)) {
            LOGGER.warn("The PaymentStatus '{}' is invalid.", data.getStatus());
            return new PaymentControllerResult(actionName, PaymentControllerStatus.PAYMENT_STATUS_INVALID, orderBuilder);
        }
        PayPalPaymentData paymentData = (PayPalPaymentData) data;
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
	 * Starts the PayPal™ Express checkout by calling the "SetExtpressCheckout"
	 * service method.
	 * 
	 * @param request
	 *            the {@code ServiceRequest}.
	 * @param orderBuilder
	 *            the {@code OrderBuilder}.
	 * @return a {@code boolean} determining whether the method call was
	 *         successful.
	 */
    private boolean setExpressCheckout(final ServiceRequest request, final OrderBuilder orderBuilder) {
        PayPalPaymentData paymentData = (PayPalPaymentData) orderBuilder.getPaymentData();
        Map<String, String> params = new HashMap<String, String>();
        params.put("METHOD", "SetExpressCheckout");
        params.put("RETURNURL", createReturnURL(request));
        params.put("CANCELURL", createCancelURL(request));
        params.put("GIROPAYSUCCESSURL", createGiropaySuccessURL(request));
        params.put("GIROPAYCANCELURL", createGiropayCancelURL(request));
        params.put("AMT", paymentData.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        params.put("CURRENCYCODE", paymentData.getCurrency().getCurrencyCode());
        params.put("EMAIL", paymentData.getEmailAddress());
        params.put("LOCALECODE", orderBuilder.getOrderMetaData().getLocale().getCountry());
        params.put("NOSHIPPING", "1");
        Map<String, String> result = execPayPalNVPCall(params, this.configuration.getLocaleConfiguration(request.getLocale()));
        if (result.get("ACK").equalsIgnoreCase("success")) {
            paymentData.setToken(result.get("TOKEN"));
            return true;
        }
        paymentData.setStatus(PaymentStatus.AUTHORIZATION_REJECTED);
        return false;
    }

    /**
	 * Collects the PayPal™ Express checkout data provided by the PayPal™ user
	 * by calling the "GetExtpressCheckoutDetails" service method.
	 * 
	 * @param request
	 *            the {@code ServiceRequest}.
	 * @param orderBuilder
	 *            the {@code OrderBuilder}.
	 * @return a {@code boolean} determining whether the method call was
	 *         successful.
	 */
    private boolean getExpressCheckoutDetails(final ServiceRequest request, final OrderBuilder orderBuilder) {
        PayPalPaymentData paymentData = (PayPalPaymentData) orderBuilder.getPaymentData();
        Map<String, String> params = new HashMap<String, String>();
        params.put("METHOD", "GetExpressCheckoutDetails");
        params.put("TOKEN", paymentData.getToken());
        Map<String, String> result = execPayPalNVPCall(params, this.configuration.getLocaleConfiguration(request.getLocale()));
        if (result.get("ACK").equalsIgnoreCase("success")) {
            paymentData.setToken(result.get("TOKEN"));
            paymentData.setPayerId(result.get("PAYERID"));
            if (result.get("REDIRECTREQUIRED") != null) {
                paymentData.setRedirectRequired(result.get("REDIRECTREQUIRED").equalsIgnoreCase("true"));
            }
            paymentData.setEmailAddress(result.get("EMAIL"));
            paymentData.setPaymentRedirectURL(createPaymentRedirectURL(request.getLocale(), paymentData.getToken()));
            paymentData.setStatus(PaymentStatus.AUTHORIZED);
            return true;
        }
        paymentData.setStatus(PaymentStatus.AUTHORIZATION_REJECTED);
        return false;
    }

    /**
	 * Finalizes the PayPal™ Express checkout by calling the
	 * "DoExpressCheckoutPayment" service method.
	 * 
	 * @param request
	 *            the {@code ServiceRequest}.
	 * @param orderBuilder
	 *            the {@code OrderBuilder}.
	 * @return a {@code boolean} determining whether the method call was
	 *         successful.
	 */
    private boolean doExpressCheckoutPayment(final ServiceRequest request, final OrderBuilder orderBuilder) {
        PayPalPaymentData paymentData = (PayPalPaymentData) orderBuilder.getPaymentData();
        Map<String, String> params = new HashMap<String, String>();
        params.put("METHOD", "DoExpressCheckoutPayment");
        params.put("TOKEN", paymentData.getToken());
        params.put("PAYERID", paymentData.getPayerId());
        params.put("PAYMENTACTION", "Sale");
        params.put("AMT", paymentData.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        params.put("CURRENCYCODE", paymentData.getCurrency().getCurrencyCode());
        params.put("EMAIL", paymentData.getEmailAddress());
        params.put("LOCALECODE", orderBuilder.getOrderMetaData().getLocale().getCountry());
        Map<String, String> result = execPayPalNVPCall(params, this.configuration.getLocaleConfiguration(request.getLocale()));
        if (result.get("ACK").equalsIgnoreCase("success")) {
            paymentData.setProviderTransactionId(result.get("TRANSACTIONID"));
            paymentData.setToken(result.get("TOKEN"));
            paymentData.setPayerId(result.get("PAYERID"));
            if (result.get("REDIRECTREQUIRED") != null) {
                paymentData.setRedirectRequired(result.get("REDIRECTREQUIRED").equalsIgnoreCase("true"));
            }
            if (paymentData.isRedirectRequired()) {
                paymentData.setStatus(PaymentStatus.PENDING);
            } else {
                paymentData.setPaymentDate(new Date(System.currentTimeMillis()));
                paymentData.setStatus(PaymentStatus.CAPTURED);
            }
            return true;
        }
        paymentData.setStatus(PaymentStatus.CAPTURE_REJECTED);
        return false;
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
