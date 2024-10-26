package multisms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.cyberneko.html.parsers.DOMParser;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.DOMBuilder;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import provider.FreeSMS12;
import provider.FreiSimser;
import provider.MobilfunkTalk;
import provider.Proxy;
import provider.Result;
import provider.SMSBilliger;
import provider.SMSBox;
import provider.SMSProvider;

/**
 * 
 * @author Martin Hoelzel
 */
public class MultiSMS {

    private String dirpath = System.getProperty("user.home") + "\\AppData\\Local\\MultiSMS";

    private String contactsFile = dirpath + "\\contacts.xml";

    private int proxyLimit = 3;

    public List<Proxy> proxyList = Collections.synchronizedList(new ArrayList<Proxy>());

    @SuppressWarnings("unchecked")
    public List<Element> selectByXPathOnDocument(Document doc, String query, String uri) throws JDOMException {
        if (uri.equals("")) {
            query = query.replace("<ns>", "");
            return XPath.selectNodes(doc, query);
        }
        query = query.replace("<ns>", "x:");
        XPath xp = XPath.newInstance(query);
        Namespace ns = Namespace.getNamespace("x", uri);
        xp.addNamespace(ns);
        return xp.selectNodes(doc);
    }

    @SuppressWarnings("unchecked")
    public List<Element> selectByXPathOnElement(Element ele, String query, String uri) throws JDOMException {
        if (uri.equals("")) {
            query = query.replace("<ns>", "");
            return XPath.selectNodes(ele, query);
        }
        query = query.replace("<ns>", "x:");
        XPath xp = XPath.newInstance(query);
        Namespace ns = Namespace.getNamespace("x", uri);
        xp.addNamespace(ns);
        return xp.selectNodes(ele);
    }

    public Document getDocumentForURL(String location) throws MalformedURLException, IOException, SAXException {
        URL url = new URL(location);
        InputStream in = url.openStream();
        return this.getDocumentFromInputStream(in);
    }

    public Document getDocumentFromInputStream(InputStream in) throws SAXException, IOException {
        DOMParser p = new DOMParser();
        p.parse(new InputSource(in));
        DOMBuilder b = new DOMBuilder();
        return b.build(p.getDocument());
    }

    public void outputStream(InputStream in) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(in));
        String line = null;
        while ((line = rd.readLine()) != null) System.out.println(line);
    }

    public List<SMSProvider> getProvider(boolean withProxy) {
        List<SMSProvider> provider = new ArrayList<SMSProvider>();
        if (withProxy) {
            provider.add(new FreeSMS12());
            provider.add(new MobilfunkTalk());
            provider.add(new FreiSimser());
            provider.add(new SMSBilliger());
        } else {
            provider.add(new FreeSMS12());
            provider.add(new MobilfunkTalk());
            provider.add(new FreiSimser());
            provider.add(new SMSBilliger());
            provider.add(new SMSBox());
        }
        return provider;
    }

    public Result sendSMSToAll(UI ref, String number, String text, boolean proxy) {
        List<SMSProvider> provider = this.getProvider(proxy);
        int max = provider.size();
        int a = 0;
        if (!proxy) {
            Iterator<SMSProvider> it = provider.iterator();
            while (it.hasNext()) {
                a++;
                SMSProvider p = it.next();
                ref.setTitle("(" + a + "/" + max + ") " + p.getName());
                Result res = p.sendSMS(number, text, null);
                if (res.statusCode == Result.SMS_SEND || !it.hasNext()) return res;
            }
        } else {
            Result rs = null;
            max = Math.min(proxyLimit, proxyList.size()) * max;
            a = 0;
            for (int i = 0; i < proxyLimit && i < proxyList.size(); i++) {
                Proxy prox = proxyList.get(i);
                Iterator<SMSProvider> it = provider.iterator();
                while (it.hasNext()) {
                    a++;
                    SMSProvider next = it.next();
                    ref.setTitle("(" + a + "/" + max + ") " + next.getName() + " (" + prox + ")");
                    rs = next.sendSMS(number, text, prox);
                    int status = rs.statusCode;
                    if (status == Result.SMS_SEND) return rs;
                }
                if (!(i < proxyLimit - 1 && i < proxyList.size() - 1)) return rs;
            }
        }
        return new Result(Result.UNKNOWN_ERROR);
    }

    public Result sendSMS(UI ref, SMSProvider provider, String number, String text, boolean proxy) {
        if (proxy) {
            int max = Math.min(proxyLimit, proxyList.size());
            int a = 0;
            Result rs = null;
            for (int i = 0; i < proxyLimit && i < proxyList.size(); i++) {
                a++;
                Proxy prox = proxyList.get(i);
                ref.setTitle("(" + a + "/" + max + ") " + provider.getName() + " (" + prox + ")");
                rs = provider.sendSMS(number, text, prox);
                int status = rs.statusCode;
                if (status == Result.SMS_SEND) return rs;
            }
            return rs;
        } else {
            return provider.sendSMS(number, text, null);
        }
    }

    public void generateProxyList() {
        try {
            HttpClient client = new DefaultHttpClient();
            String target = "http://www.proxy-listen.de/Proxy/Proxyliste.html";
            HttpGet get = new HttpGet(target);
            HttpResponse response = client.execute(get);
            HttpEntity e = response.getEntity();
            List<NameValuePair> formparas = new ArrayList<NameValuePair>();
            Document doc = this.getDocumentFromInputStream(e.getContent());
            List<Element> forms = this.selectByXPathOnDocument(doc, "//<ns>FORM[<ns>INPUT[@type='hidden']]", doc.getRootElement().getNamespaceURI());
            Element form = forms.get(0);
            List<Element> hiddens = this.selectByXPathOnElement(form, "//<ns>INPUT[@type='hidden']", form.getNamespaceURI());
            Iterator<Element> it = hiddens.iterator();
            while (it.hasNext()) {
                Element hidden = it.next();
                String name = hidden.getAttributeValue("name");
                String value = hidden.getAttributeValue("value");
                formparas.add(new BasicNameValuePair(name, value));
            }
            formparas.add(new BasicNameValuePair("submit", "Anzeigen"));
            formparas.add(new BasicNameValuePair("type", "http"));
            formparas.add(new BasicNameValuePair("liststyle", "leech"));
            HttpPost post = new HttpPost(target);
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparas, "UTF-8");
            post.setEntity(entity);
            response = client.execute(post);
            e = response.getEntity();
            doc = this.getDocumentFromInputStream(e.getContent());
            List<Element> proxylist = this.selectByXPathOnDocument(doc, "//<ns>A[@class='proxyList']", doc.getRootElement().getNamespaceURI());
            Iterator<Element> it2 = proxylist.iterator();
            while (it2.hasNext()) {
                Element p = it2.next();
                String proxystring = p.getText();
                String[] split = proxystring.split(":");
                if (split.length == 2) {
                    String host = split[0];
                    int port = Integer.valueOf(split[1]);
                    Timer t = new Timer();
                    t.schedule(new CheckTask(this, port, host), 0);
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
    }

    @SuppressWarnings("unchecked")
    public List<Contact> getContacts() {
        List<Contact> contacts = new ArrayList<Contact>();
        try {
            File f = new File(contactsFile);
            File dir = new File(dirpath);
            if (!f.exists()) this.createFileAndDir(f, dir);
            SAXBuilder build = new SAXBuilder();
            Document doc = build.build(f);
            List<Element> cts = doc.getRootElement().getChildren("contact");
            Iterator<Element> it = cts.iterator();
            while (it.hasNext()) {
                Element e = it.next();
                Element name = e.getChild("name");
                Element number = e.getChild("number");
                Contact c = new Contact(name.getText(), number.getText());
                contacts.add(c);
            }
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contacts;
    }

    public void addContact(Contact c) {
        try {
            File dir = new File(dirpath);
            File file = new File(contactsFile);
            if (!file.exists()) {
                this.createFileAndDir(file, dir);
            }
            SAXBuilder build = new SAXBuilder();
            Document doc = build.build(file);
            Element root = doc.getRootElement();
            Element contact = new Element("contact");
            Element name = new Element("name");
            name.setText(c.name);
            contact.addContent(name);
            Element number = new Element("number");
            number.setText(c.number);
            contact.addContent(number);
            root.addContent(contact);
            FileOutputStream fout = new FileOutputStream(file);
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
            out.output(doc, fout);
            fout.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createFileAndDir(File file, File dir) {
        if (!dir.exists()) dir.mkdir();
        try {
            file.createNewFile();
            FileWriter write = new FileWriter(file);
            write.append("<contacts></contacts>");
            write.close();
        } catch (IOException e) {
        }
    }
}
