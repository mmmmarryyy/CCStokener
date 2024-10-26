package provider;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
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

public class SMSBilliger implements SMSProvider {

    private MultiSMS ref = new MultiSMS();

    private static final String FORM_NUMBER = "number";

    private static final String FORM_TEXT = "text";

    private static final String FORM_CAPTCHA = "sec_code";

    private static final String FORM_AGB = "conditions";

    @Override
    public Result sendSMS(String number, String text, Proxy proxy) {
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            if (proxy != null) {
                HttpHost prox = new HttpHost(proxy.host, proxy.port);
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, prox);
            }
            String target = "http://www.smsbilliger.de/free-sms.html";
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
                } else if (name != null && name.equals(FORM_AGB)) {
                    formparas.add(new BasicNameValuePair(name, "true"));
                }
            }
            formparas.add(new BasicNameValuePair("dialing_code", this.getPrefixPart(number)));
            formparas.add(new BasicNameValuePair("no_schedule", "yes"));
            List<Element> captchas = ref.selectByXPathOnDocument(doc, "//<ns>IMG[@id='code_img']", doc.getRootElement().getNamespaceURI());
            Element captcha = captchas.get(0);
            String url = "http://www.smsbilliger.de/" + captcha.getAttributeValue("src");
            HttpGet imgcall = new HttpGet(url);
            HttpResponse imgres = client.execute(imgcall);
            HttpEntity imge = imgres.getEntity();
            BufferedImage img = ImageIO.read(imge.getContent());
            imge.getContent().close();
            Icon icon = new ImageIcon(img);
            String result = (String) JOptionPane.showInputDialog(null, "Bitte Captcha eingeben:", "Captcha", JOptionPane.INFORMATION_MESSAGE, icon, null, "");
            formparas.add(new BasicNameValuePair(FORM_CAPTCHA, result));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparas, "UTF-8");
            HttpPost post = new HttpPost(target);
            post.setEntity(entity);
            response = client.execute(post);
            e = response.getEntity();
            doc = ref.getDocumentFromInputStream(e.getContent());
            List<Element> fonts = ref.selectByXPathOnDocument(doc, "//<ns>H3", doc.getRootElement().getNamespaceURI());
            Iterator<Element> it2 = fonts.iterator();
            while (it2.hasNext()) {
                Element font = it2.next();
                String txt = font.getText();
                if (txt.contains("Die SMS wurde erfolgreich versendet.")) {
                    return new Result(Result.SMS_SEND);
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

    @Override
    public String toString() {
        return this.getName();
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
            number = "0049" + number.substring(1, 4);
        } else {
            number = "0049" + number.substring(0, 3);
        }
        return number;
    }

    @Override
    public String getName() {
        return "SMSBilliger";
    }
}
