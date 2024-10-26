package iws.db.util;

import iws.db.object.Carrello;
import iws.db.object.ProxyConfig;
import iws.db.util.bean.GenericForm;
import iws.db.util.bean.GenericResponse;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

public class PayPalUtil {

    private static HashMap<String, String> getParametriPagamentoPayPal() {
        HashMap<String, String> parametriPagamento = new HashMap<String, String>();
        parametriPagamento.put(CostantiPayPal.PAYPAL_API_username_KEY, "andrea_1252943707_biz_api1.gmail.com");
        parametriPagamento.put(CostantiPayPal.PAYPAL_API_password_KEY, "1252943717");
        parametriPagamento.put(CostantiPayPal.PAYPAL_API_signature_KEY, "ATb.OZn4eHHVmzfBHDot7y2iQdmwAAAn77q8VVeggrntV83NIjZ8vjRB");
        return parametriPagamento;
    }

    private static GenericResponse eseguiFormGenerica(GenericForm form, ProxyConfig proxy) throws IOException {
        URL url;
        URLConnection urlConn;
        DataOutputStream printout;
        DataInputStream input;
        if (proxy != null && proxy.getUseProxy()) {
            Properties sysProps = System.getProperties();
            sysProps.put("proxySet", "true");
            sysProps.put("proxyHost", proxy.getProxyHost());
            sysProps.put("proxyPort", proxy.getProxyPort());
        }
        url = new URL(form.getAction());
        urlConn = url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestProperty("Content-Type", form.getContentType());
        printout = new DataOutputStream(urlConn.getOutputStream());
        StringBuffer buf = new StringBuffer();
        Iterator<Map.Entry<String, String>> iterator = form.getParametriRequest().entrySet().iterator();
        Map.Entry<String, String> ele;
        while (iterator.hasNext()) {
            ele = iterator.next();
            if (buf.length() > 0) buf.append("&");
            buf.append(ele.getKey());
            buf.append("=");
            buf.append(ele.getValue());
        }
        String content = buf.toString();
        printout.writeBytes(content);
        printout.flush();
        printout.close();
        input = new DataInputStream(urlConn.getInputStream());
        String str;
        StringBuffer res = new StringBuffer();
        while (null != ((str = input.readLine()))) {
            res.append(str);
        }
        input.close();
        GenericResponse response = new GenericResponse();
        String[] stra = res.toString().split("&");
        String[] s;
        if (stra != null) {
            for (int i = 0; i < stra.length; i++) {
                s = stra[i].split("=");
                response.getParametri().put(s[0], s[1]);
            }
        }
        return response;
    }

    public static String eseguiExpressCheckoutAuthentication(Carrello carrello, String basePath) throws IOException {
        GenericForm form = new GenericForm();
        String linkPagamentoOK = basePath + CostantiPayPal.PAYPAL_ACTION_PAGAMENTO_OK;
        String linkPagamentoERRORE = basePath + CostantiPayPal.PAYPAL_ACTION_PAGAMENTO_ERRORE;
        HashMap<String, String> parametriPagamento = PayPalUtil.getParametriPagamentoPayPal();
        form.setAction(CostantiPayPal.PAYPAL_URL_AUTHENTICATION);
        form.setContentType(CostantiPayPal.PAYPAL_CONTENT_TYPE_FORM);
        form.setMethod("POST");
        form.getParametriRequest().put("USER", parametriPagamento.get(CostantiPayPal.PAYPAL_API_username_KEY));
        form.getParametriRequest().put("PWD", parametriPagamento.get(CostantiPayPal.PAYPAL_API_password_KEY));
        form.getParametriRequest().put("SIGNATURE", parametriPagamento.get(CostantiPayPal.PAYPAL_API_signature_KEY));
        form.getParametriRequest().put("METHOD", "SetExpressCheckout");
        form.getParametriRequest().put("RETURNURL", linkPagamentoOK);
        form.getParametriRequest().put("CANCELURL", linkPagamentoERRORE);
        form.getParametriRequest().put("AMT", String.valueOf(carrello.getTotale()));
        form.getParametriRequest().put("PAYMENTACTION", "Authorization");
        form.getParametriRequest().put("CURRENCYCODE", CostantiPayPal.PAYPAL_CURRENCYCODE);
        form.getParametriRequest().put("VERSION", CostantiPayPal.PAYPAL_VERSION);
        GenericResponse res = eseguiFormGenerica(form, ProxyConfig.getInstance());
        if ("Success".equals(res.getParametri().get("ACK"))) {
            return CostantiPayPal.PAYPAL_URL_AUTHENTICATION_REDIRECT + res.getParametri().get("TOKEN");
        } else {
            return null;
        }
    }

    public static String eseguiExpressCheckoutGetDetail(HttpSession session, String basePath, String token) throws IOException {
        GenericForm form = new GenericForm();
        HashMap<String, String> parametriPagamento = PayPalUtil.getParametriPagamentoPayPal();
        form.setAction(CostantiPayPal.PAYPAL_URL_PAYPAL_URL_GETDETAIL);
        form.setContentType(CostantiPayPal.PAYPAL_CONTENT_TYPE_FORM);
        form.setMethod("POST");
        form.getParametriRequest().put("USER", parametriPagamento.get(CostantiPayPal.PAYPAL_API_username_KEY));
        form.getParametriRequest().put("PWD", parametriPagamento.get(CostantiPayPal.PAYPAL_API_password_KEY));
        form.getParametriRequest().put("SIGNATURE", parametriPagamento.get(CostantiPayPal.PAYPAL_API_signature_KEY));
        form.getParametriRequest().put("METHOD", "GetExpressCheckoutDetails");
        form.getParametriRequest().put("VERSION", CostantiPayPal.PAYPAL_VERSION);
        form.getParametriRequest().put("TOKEN", token);
        GenericResponse res = eseguiFormGenerica(form, ProxyConfig.getInstance());
        if ("Success".equals(res.getParametri().get("ACK"))) {
            session.setAttribute(CostantiPayPal.ATTRIBUTO_RISPOSTA_DETTAGLIO, res);
            return basePath + "/pagamentoPayPalDetail.jsp";
        } else {
            return null;
        }
    }
}
