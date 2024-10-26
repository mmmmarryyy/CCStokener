package com.dotmarketing.cms.creditcard.model.authorize;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;
import javax.servlet.ServletException;
import com.dotmarketing.cms.creditcard.model.CreditCardProcessor;
import com.dotmarketing.cms.creditcard.model.CreditCardProcessorResponse;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

public class AuthorizeCreditCardProcessor extends CreditCardProcessor {

    private final String successPageConfig = "successPageConfig";

    private final String failurePageConfig = "failurePageConfig";

    private final String testMode = "testMode";

    private final String login = "login";

    private final String password = "password";

    private final String tranKey = "tranKey";

    private final String delimChart = "delimChart";

    private final String successCommand = "successCommand";

    private final String failureCommand = "failureCommand";

    private final String companyEmail = "companyEmail";

    private String _SuccessPage;

    private String _FailurePage;

    private boolean _TestMode;

    private String _Login;

    private String _Password;

    private String _TranKey;

    private String _DelimChart;

    private String _SuccessCommand;

    private String _FailureCommand;

    private String _Company_Name;

    private String _CompanyEmail;

    static char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public AuthorizeCreditCardProcessor() {
        _SuccessPage = Config.getStringProperty(successPageConfig);
        _FailurePage = Config.getStringProperty(failurePageConfig);
        _TestMode = Config.getBooleanProperty(testMode);
        _Login = Config.getStringProperty(login);
        _Password = Config.getStringProperty(password);
        _TranKey = Config.getStringProperty(tranKey);
        _DelimChart = Config.getStringProperty(delimChart);
        _SuccessCommand = Config.getStringProperty(successCommand);
        _FailureCommand = Config.getStringProperty(failureCommand);
        _CompanyEmail = Config.getStringProperty(companyEmail);
        _Company_Name = PublicCompanyFactory.getDefaultCompany().getName();
    }

    public AuthorizeCreditCardResponse process() throws AuthorizeCreditCardProcessorException {
        try {
            if (_SuccessPage.endsWith("?") || _SuccessPage.endsWith("&")) _SuccessPage = _SuccessPage.substring(0, _SuccessPage.length() - 1);
            if (_FailurePage.endsWith("?") || _FailurePage.endsWith("&")) _FailurePage = _FailurePage.substring(0, _FailurePage.length() - 1);
            if ((_SuccessPage == null || _FailurePage == null) || (_SuccessPage.equals("") || _FailurePage.equals(""))) {
                throw new ServletException("The success_page and failure_page fields have to be passed to me to work");
            } else {
                StringBuffer sb = new StringBuffer();
                sb.append("x_login=" + _Login + "&");
                sb.append("x_tran_key=" + _TranKey + "&");
                sb.append("x_version=3.1&");
                if (_TestMode) {
                    sb.append("x_test_request=TRUE&");
                }
                sb.append("x_method=CC&");
                sb.append("x_type=AUTH_CAPTURE&");
                sb.append("x_delim_data=TRUE&");
                sb.append("x_delim_char=" + _DelimChart + "&");
                sb.append("x_relay_response=FALSE&");
                sb.append("x_description=" + _Company_Name + "&");
                sb.append("&success_page=" + _SuccessPage);
                sb.append("&success_cmd=" + _SuccessCommand);
                sb.append("&failure_page=" + _FailureCommand);
                sb.append("&failure_cmd=" + _FailureCommand);
                sb.append("&x_merchant_email=" + _CompanyEmail);
                sb.append("&x_email_customer=" + "false");
                sb.append("&x_first_name=" + getBillingFirstName());
                sb.append("&x_last_name=" + getBillingLastName());
                sb.append("&x_email=" + getBillingEmailAdress());
                sb.append("&x_company=" + getBillingCompany());
                sb.append("&x_address=" + getBillingStreet());
                sb.append("&x_city=" + getBillingCity());
                sb.append("&x_state=" + getBillingState());
                sb.append("&x_zip=" + getBillingZip());
                sb.append("&x_country=" + getBillingCountry());
                sb.append("&x_phone=" + getBillingPhone());
                sb.append("&x_ship_to_first_name=" + getShippingFirstName());
                sb.append("&x_ship_to_last_name=" + getShippingLastName());
                sb.append("&x_ship_to_company=" + getShippingCompany());
                sb.append("&x_ship_to_address=" + getShippingStreet());
                sb.append("&x_ship_to_city=" + getShippingCity());
                sb.append("&x_ship_to_state=" + getShippingState());
                sb.append("&x_ship_to_zip=" + getShippingZip());
                sb.append("&x_ship_to_country=" + getShippingCountry());
                sb.append("&x_ship_to_phone=" + getShippingPhone());
                sb.append("&x_card_num=" + getCreditCardNumber());
                float amountFloat = getAmount();
                DecimalFormat df = new DecimalFormat("########.##");
                String amount = df.format(amountFloat);
                sb.append("&x_amount=" + amount);
                Date expDate = getCreditCardExpirationDate();
                GregorianCalendar gc = new GregorianCalendar();
                gc.setTime(expDate);
                int month = gc.get(Calendar.MONTH);
                month++;
                int year = gc.get(Calendar.YEAR);
                String expdDateString = Integer.toString(month) + Integer.toString(year);
                sb.append("&x_exp_date=" + expdDateString);
                sb.append("&x_card_code=" + getCreditCardCVV());
                URL url = new URL("https://secure.authorize.net/gateway/transact.dll");
                if (_TestMode) {
                    url = new URL("https://certification.authorize.net/gateway/transact.dll");
                }
                URLConnection connection = (URLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                out.write(sb.toString().getBytes());
                out.flush();
                out.close();
                Logger.info(this, "\n\n\nSENDING TO=" + url);
                Logger.info(this, "SENDING=" + sb.toString());
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                line = in.readLine();
                in.close();
                Logger.debug(AuthorizeCreditCardProcessor.class, line);
                Vector ccrep = split(_DelimChart, line);
                Logger.info(this, "Response Code: " + ccrep.elementAt(0).toString());
                Logger.info(this, "Human Readable Response Code: " + ccrep.elementAt(3).toString());
                Logger.info(this, "Approval Code: " + ccrep.elementAt(4).toString());
                Logger.info(this, "Trans ID: " + ccrep.elementAt(6).toString());
                Logger.info(this, "MD5 Hash Server: " + ccrep.elementAt(37).toString());
                String message = ccrep.elementAt(3).toString();
                StringBuffer _RedirectTo = new StringBuffer();
                String command = "";
                Logger.info(this, "RESPONSE CODE=" + ccrep.elementAt(0));
                AuthorizeCreditCardResponse accr = new AuthorizeCreditCardResponse();
                if (!ccrep.elementAt(0).equals("1")) {
                    Logger.debug(this, "Response code: " + ccrep.elementAt(0));
                    accr.setCode(CreditCardProcessorResponse.ERROR);
                    _RedirectTo.append(_FailurePage);
                    command = _FailureCommand;
                } else {
                    Logger.info(this, "Redirect to success page: " + _SuccessPage);
                    accr.setCode(CreditCardProcessorResponse.APPROVED);
                    accr.setOrdernum(ccrep.elementAt(6).toString());
                    _RedirectTo.append(_SuccessPage);
                    command = _SuccessCommand;
                }
                _RedirectTo.append("?message=" + message);
                _RedirectTo.append("&cmd=" + command);
                String redirectToURL = _RedirectTo.toString();
                accr.setMessage(redirectToURL);
                return accr;
            }
        } catch (Exception e) {
            Logger.fatal(this, e.toString());
            throw new AuthorizeCreditCardProcessorException(e.getMessage(), e);
        }
    }

    public static Vector split(String pattern, String in) {
        int s1 = 0, s2 = -1;
        Vector out = new Vector(30);
        while (true) {
            s2 = in.indexOf(pattern, s1);
            if (s2 != -1) {
                out.addElement(in.substring(s1, s2));
            } else {
                String _ = in.substring(s1);
                if (_ != null && !_.equals("")) {
                    out.addElement(_);
                }
                break;
            }
            s1 = s2;
            s1 += pattern.length();
        }
        return out;
    }
}
