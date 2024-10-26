package provider;

import java.awt.HeadlessException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
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
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

public class SMSBox implements SMSProvider {

    private MultiSMS ref = new MultiSMS();

    private static final String FORM_NUMBER = "sms[number]";

    private static final String FORM_TEXT = "sms[text]";

    private static final String FORM_CAPTCHA = "sms[captcha]";

    @Override
    public Result sendSMS(String number, String text, Proxy proxy) {
        try {
            HttpClient client = new DefaultHttpClient();
            if (proxy != null) {
                HttpHost prox = new HttpHost(proxy.host, proxy.port);
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, prox);
            }
            Document doc = ref.getDocumentForURL("http://server.smsform.de/index.php?i=1");
            List<Element> forms = ref.selectByXPathOnDocument(doc, "//<ns>FORM", doc.getRootElement().getNamespaceURI());
            Element form = forms.get(0);
            String target = form.getAttributeValue("action");
            HttpPost httppost = new HttpPost(target);
            List<NameValuePair> formparas = new ArrayList<NameValuePair>();
            List<Element> inputs = ref.selectByXPathOnElement(form, "//<ns>INPUT|//<ns>TEXTAREA", form.getNamespaceURI());
            Iterator<Element> it = inputs.iterator();
            while (it.hasNext()) {
                Element input = it.next();
                String type = input.getAttributeValue("type");
                String name = input.getAttributeValue("name");
                String value = input.getAttributeValue("value");
                if (type != null && type.equals("hidden")) {
                    formparas.add(new BasicNameValuePair(name, value));
                } else if (name != null && name.equals(FORM_NUMBER)) {
                    formparas.add(new BasicNameValuePair(name, number));
                } else if (name != null && name.equals(FORM_TEXT)) {
                    formparas.add(new BasicNameValuePair(name, text));
                }
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparas, "UTF-8");
            httppost.setEntity(entity);
            HttpResponse response = client.execute(httppost);
            HttpEntity e = response.getEntity();
            doc = ref.getDocumentFromInputStream(e.getContent());
            forms = ref.selectByXPathOnDocument(doc, "//<ns>FORM", doc.getRootElement().getNamespaceURI());
            form = forms.get(0);
            target = form.getAttributeValue("action");
            httppost = new HttpPost(target);
            formparas = new ArrayList<NameValuePair>();
            inputs = ref.selectByXPathOnElement(form, "//<ns>INPUT|//<ns>TEXTAREA", form.getNamespaceURI());
            it = inputs.iterator();
            while (it.hasNext()) {
                Element input = it.next();
                String type = input.getAttributeValue("type");
                String name = input.getAttributeValue("name");
                String value = input.getAttributeValue("value");
                if (type != null && type.equals("hidden")) {
                    formparas.add(new BasicNameValuePair(name, value));
                }
            }
            entity = new UrlEncodedFormEntity(formparas, "UTF-8");
            httppost.setEntity(entity);
            response = client.execute(httppost);
            e = response.getEntity();
            doc = ref.getDocumentFromInputStream(e.getContent());
            forms = ref.selectByXPathOnDocument(doc, "//<ns>FORM", doc.getRootElement().getNamespaceURI());
            form = forms.get(0);
            List<Element> captchas = ref.selectByXPathOnDocument(doc, "//<ns>TD[@class='captcha']/<ns>IMG", doc.getRootElement().getNamespaceURI());
            Element captcha = captchas.get(0);
            String imgsrc = captcha.getAttributeValue("src");
            HttpGet imgcall = new HttpGet(imgsrc);
            HttpResponse imgres = client.execute(imgcall);
            HttpEntity imge = imgres.getEntity();
            BufferedImage img = ImageIO.read(imge.getContent());
            imge.getContent().close();
            Icon icon = new ImageIcon(img);
            String result = (String) JOptionPane.showInputDialog(null, "Bitte Captcha eingeben:", "Captcha", JOptionPane.INFORMATION_MESSAGE, icon, null, "");
            httppost = new HttpPost(target);
            formparas = new ArrayList<NameValuePair>();
            inputs = ref.selectByXPathOnElement(form, "//<ns>INPUT|//<ns>TEXTAREA", form.getNamespaceURI());
            it = inputs.iterator();
            while (it.hasNext()) {
                Element input = it.next();
                String type = input.getAttributeValue("type");
                String name = input.getAttributeValue("name");
                String value = input.getAttributeValue("value");
                if (name != null && name.equals(FORM_CAPTCHA)) {
                    formparas.add(new BasicNameValuePair(name, result));
                } else if (type != null && type.equals("hidden")) {
                    formparas.add(new BasicNameValuePair(name, value));
                }
            }
            entity = new UrlEncodedFormEntity(formparas, "UTF-8");
            httppost.setEntity(entity);
            response = client.execute(httppost);
            e = response.getEntity();
            doc = ref.getDocumentFromInputStream(e.getContent());
            forms = ref.selectByXPathOnDocument(doc, "//<ns>FORM", doc.getRootElement().getNamespaceURI());
            form = forms.get(0);
            formparas = new ArrayList<NameValuePair>();
            inputs = ref.selectByXPathOnElement(form, "//<ns>INPUT|//<ns>TEXTAREA", form.getNamespaceURI());
            it = inputs.iterator();
            while (it.hasNext()) {
                Element input = it.next();
                String type = input.getAttributeValue("type");
                String name = input.getAttributeValue("name");
                String value = input.getAttributeValue("value");
                if (type != null && type.equals("hidden")) {
                    formparas.add(new BasicNameValuePair(name, value));
                }
            }
            entity = new UrlEncodedFormEntity(formparas, "UTF-8");
            httppost.setEntity(entity);
            response = client.execute(httppost);
            e = response.getEntity();
            String token = null;
            String session = null;
            doc = ref.getDocumentFromInputStream(e.getContent());
            List<Element> scripts = ref.selectByXPathOnDocument(doc, "//<ns>SCRIPT", "http://www.w3.org/1999/xhtml");
            it = scripts.iterator();
            while (it.hasNext()) {
                Element script = it.next();
                String txt = script.getText();
                if (txt.contains("tok = '")) {
                    int pos1 = txt.indexOf("tok = '");
                    int pos2 = txt.indexOf("'", pos1 + 8);
                    token = txt.substring(pos1 + 7, pos2);
                }
                if (txt.contains("PHPSESSID = '")) {
                    int pos1 = txt.indexOf("PHPSESSID = '");
                    int pos2 = txt.indexOf("'", pos1 + 14);
                    session = txt.substring(pos1 + 13, pos2);
                }
            }
            if (token != null && session != null) {
                target = "http://server.smsform.de/sms.php";
                httppost = new HttpPost(target);
                formparas = new ArrayList<NameValuePair>();
                formparas.add(new BasicNameValuePair("token", token));
                formparas.add(new BasicNameValuePair("PHPSESSID", session));
                entity = new UrlEncodedFormEntity(formparas, "UTF-8");
                httppost.setEntity(entity);
                response = client.execute(httppost);
                e = response.getEntity();
                String res = EntityUtils.toString(e);
                if (res.charAt(0) == '0' && res.contains("zu kurz")) return new Result(Result.SMS_TOO_SHORT, res); else if (res.charAt(0) == '0') return new Result(Result.SMS_LIMIT_REACHED, res); else if (res.charAt(0) == '1') return new Result(Result.SMS_SEND, res);
            }
        } catch (HeadlessException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (JDOMException e) {
            e.printStackTrace();
        }
        return new Result(Result.UNKNOWN_ERROR);
    }

    @Override
    public String getName() {
        return "SMSBox";
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
