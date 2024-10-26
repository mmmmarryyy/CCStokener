package provider;

import java.io.IOException;
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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

public class MobilfunkTalk implements SMSProvider {

    private MultiSMS ref = new MultiSMS();

    private static final String FORM_NUMBER = "nr";

    private static final String FORM_TEXT = "text";

    @Override
    public Result sendSMS(String number, String text, Proxy proxy) {
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            if (proxy != null) {
                HttpHost prox = new HttpHost(proxy.host, proxy.port);
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, prox);
            }
            String target = "http://www.mobilfunk-talk.de/sms/";
            HttpGet get = new HttpGet(target);
            HttpResponse response = client.execute(get);
            HttpEntity e = response.getEntity();
            Document doc = ref.getDocumentFromInputStream(e.getContent());
            List<Element> forms = ref.selectByXPathOnDocument(doc, "//<ns>FORM", doc.getRootElement().getNamespaceURI());
            if (forms.size() == 0) return new Result(Result.SMS_LIMIT_REACHED);
            Element form = forms.get(0);
            List<NameValuePair> formparas = new ArrayList<NameValuePair>();
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
                    formparas.add(new BasicNameValuePair(name, this.getNumberPart(number)));
                } else if (name != null && name.equals(FORM_TEXT)) {
                    formparas.add(new BasicNameValuePair(name, text));
                }
            }
            formparas.add(new BasicNameValuePair("netz", this.getPrefixPart(number)));
            formparas.add(new BasicNameValuePair("counter", "120"));
            formparas.add(new BasicNameValuePair("agbcheck", "on"));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparas, "UTF-8");
            HttpPost post = new HttpPost(target);
            post.setEntity(entity);
            response = client.execute(post);
            e = response.getEntity();
            doc = ref.getDocumentFromInputStream(e.getContent());
            List<Element> fonts = ref.selectByXPathOnDocument(doc, "//<ns>FONT[@color='#ff0000']", doc.getRootElement().getNamespaceURI());
            Iterator<Element> it2 = fonts.iterator();
            while (it2.hasNext()) {
                Element font = it2.next();
                String txt = font.getText();
                if (txt.contains("Bitte warten Sie bis morgen!")) {
                    return new Result(Result.SMS_LIMIT_REACHED);
                } else if (txt.contains("SMS Erfolgreich versendet!")) {
                    return new Result(Result.SMS_SEND);
                } else if (txt.contains("Fehler: Handynummer ung�ltig.")) {
                    return new Result(Result.NUMBER_NOT_VALID);
                }
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (JDOMException e) {
            e.printStackTrace();
        }
        return new Result(Result.UNKNOWN_ERROR);
    }

    private String getNumberPart(String number) {
        if (number.charAt(0) == '0') {
            number = number.substring(4);
        } else {
            number = number.substring(3);
        }
        return number;
    }

    private String getPrefixPart(String number) {
        if (number.charAt(0) == '0') {
            number = "49" + number.substring(1, 4);
        } else {
            number = "49" + number.substring(0, 3);
        }
        return number;
    }

    @Override
    public String getName() {
        return "MobilTalk";
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
