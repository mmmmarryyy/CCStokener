package com.kenstevens.stratdom.site.httpclient;

import java.util.Observable;
import javax.annotation.PostConstruct;
import javax.xml.xpath.XPathExpressionException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.kenstevens.stratdom.site.Command;
import com.kenstevens.stratdom.site.CommandProcessor;
import com.kenstevens.stratdom.site.LoginFailedException;
import com.kenstevens.stratdom.site.MessageRecipients;
import com.kenstevens.stratdom.site.SiteForm;
import com.kenstevens.stratdom.site.SiteFormFactory;
import com.kenstevens.stratdom.site.SiteResponse;
import com.kenstevens.stratdom.site.StratSite;
import com.kenstevens.stratdom.site.parser.ParseException;

@Component
class StratSiteHttpClient extends Observable implements StratSite {

    @Autowired
    MessageRecipients messageRecipients;

    @Autowired
    CommandProcessor commandProcessor;

    @Autowired
    SiteFormFactory siteFormFactory;

    DefaultHttpClient httpclient = new DefaultHttpClient();

    private SiteForm mainform;

    private SiteForm loginForm;

    private SiteForm sendmailForm;

    private SiteResponse lastResponse;

    @PostConstruct
    public void initialize() {
        httpclient = new DefaultHttpClient();
        mainform = null;
        loginForm = null;
        sendmailForm = null;
    }

    public SiteResponse getCurrentPage() {
        return lastResponse;
    }

    public SiteForm getMainform() throws LoginFailedException {
        if (mainform == null) {
            throw new LoginFailedException();
        }
        return mainform;
    }

    public SiteForm getLoginForm() {
        return loginForm;
    }

    private void setLoginForm(SiteFormHttpClient loginForm) {
        this.loginForm = loginForm;
    }

    public void setMainform(SiteForm mainform) {
        this.mainform = mainform;
    }

    public void setSendmailForm(SiteForm sendmailForm) {
        this.sendmailForm = sendmailForm;
    }

    public SiteForm getSendmailForm() {
        return sendmailForm;
    }

    public void setLoginForm(SiteResponse homePage) throws ParseException {
        try {
            setLoginForm(new SiteFormHttpClient(homePage.getDOM(), "action", "/login.html"));
        } catch (Exception e) {
            throw new ParseException("Unable to find Login Form", e);
        }
    }

    public void setMailFormAndRecipients(SiteResponse sendMessagePage) throws XPathExpressionException {
        SiteForm sendmailform = siteFormFactory.parseSendMailForm(sendMessagePage, "name", "mainform");
        if (sendmailform != null) {
            setSendmailForm(sendmailform);
            messageRecipients.setRecipients(sendmailform.getOptions(Command.FORM_MESSAGE_TARGET), sendmailform.getOptionValues(Command.FORM_MESSAGE_TARGET));
        }
    }

    @Override
    public SiteResponse getResponse(String URL) throws Exception {
        return new SiteResponseHttpClient(httpclient.execute(new HttpGet(URL)));
    }

    @Override
    public void submit(SiteForm form) throws Exception {
        lastResponse = form.submit(this);
    }

    HttpResponse execute(HttpPost httpost) throws Exception {
        return httpclient.execute(httpost);
    }
}
