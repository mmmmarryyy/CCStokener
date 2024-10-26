package provider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import multisms.MultiSMS;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

public class FreeSMS12 implements SMSProvider {

    private MultiSMS ref = new MultiSMS();

    private static final String FORM_NUMBER = "sms[handynr]";

    private static final String FORM_TEXT = "sms[nachricht]";

    private static final String FORM_PREFIX = "sms[vorwahl]";

    @Override
    public Result sendSMS(String number, String text, Proxy proxy) {
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            if (proxy != null) {
                HttpHost prox = new HttpHost(proxy.host, proxy.port);
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, prox);
            }
            String target = "http://www.12free-sms.de/";
            HttpPost httppost = new HttpPost(target);
            List<NameValuePair> formparas = new ArrayList<NameValuePair>();
            formparas.add(new BasicNameValuePair("agbok", "Ja"));
            formparas.add(new BasicNameValuePair("smscode", ""));
            formparas.add(new BasicNameValuePair("smsspruch_id", ""));
            formparas.add(new BasicNameValuePair("agbnotok2", "Weiter"));
            formparas.add(new BasicNameValuePair("site", "formular"));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparas, "UTF-8");
            httppost.setEntity(entity);
            HttpResponse response = client.execute(httppost);
            HttpEntity e = response.getEntity();
            Document doc = ref.getDocumentFromInputStream(e.getContent());
            List<Element> forms = ref.selectByXPathOnDocument(doc, "//<ns>FORM", doc.getRootElement().getNamespaceURI());
            if (forms.size() == 0) return new Result(Result.SMS_LIMIT_REACHED);
            Element form = forms.get(0);
            formparas = new ArrayList<NameValuePair>();
            List<Element> inputs = ref.selectByXPathOnElement(form, "//<ns>INPUT|//<ns>TEXTAREA|//<ns>SELECT", form.getNamespaceURI());
            Iterator<Element> it = inputs.iterator();
            while (it.hasNext()) {
                Element input = it.next();
                String type = input.getAttributeValue("type");
                String name = input.getAttributeValue("name");
                String value = input.getAttributeValue("value");
                if (type != null && type.equals("hidden")) {
                    formparas.add(new BasicNameValuePair(name, value));
                } else if (name != null && name.equals(FORM_NUMBER)) {
                    formparas.add(new BasicNameValuePair(name, this.clearLeadingZero(number)));
                } else if (name != null && name.equals(FORM_TEXT)) {
                    formparas.add(new BasicNameValuePair(name, text));
                } else if (name != null && name.equals(FORM_PREFIX)) {
                    formparas.add(new BasicNameValuePair(name, "0049"));
                }
            }
            entity = new UrlEncodedFormEntity(formparas, "UTF-8");
            httppost.setEntity(entity);
            response = client.execute(httppost);
            e = response.getEntity();
            doc = ref.getDocumentFromInputStream(e.getContent());
            List<Element> ps = ref.selectByXPathOnDocument(doc, "//<ns>P|//<ns>DIV", doc.getRootElement().getNamespaceURI());
            Iterator<Element> it2 = ps.iterator();
            while (it2.hasNext()) {
                Element p = it2.next();
                String txt = p.getText();
                if (txt.contains("Du kannst maximal 1 SMS pro Tag versenden!")) {
                    return new Result(Result.SMS_LIMIT_REACHED);
                } else if (txt.contains("SMS wurde erfolgreich versendet.")) {
                    return new Result(Result.SMS_SEND);
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return new Result(Result.UNKNOWN_ERROR);
    }

    private String clearLeadingZero(String number) {
        if (number.charAt(0) == '0') {
            number = number.substring(1);
        }
        return number;
    }

    @Override
    public String getName() {
        return "12-Free-SMS";
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
