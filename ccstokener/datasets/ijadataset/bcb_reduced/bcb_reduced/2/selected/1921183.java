package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Vector;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import com.loveazure.eo.Cent;
import com.loveazure.eo.Dict;
import com.loveazure.util.HibernateUtil;
import com.loveazure.util.SymbolConvert;

public class XMLGet {

    SAXReader reader = new SAXReader();

    Session session;

    SymbolConvert convert = new SymbolConvert();

    public static void main(String[] args) throws Exception {
        new XMLGet().go();
    }

    private void go() throws Exception {
        SessionFactory factory = HibernateUtil.getSessionFactory();
        session = factory.openSession();
        File file = new File("words", "1275637913656");
        InputStream is = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(is, "utf-8");
        BufferedReader br = new BufferedReader(isr);
        String line;
        int i = 0;
        while ((line = br.readLine()) != null) {
            read("http://dict.cn/ws.php?q=" + line.split("=")[0]);
            System.out.println(i++);
        }
    }

    private void read(String url) {
        session.beginTransaction();
        try {
            Document doc = reader.read(new URL(url).openStream());
            Element root = doc.getRootElement();
            Dict dic = new Dict();
            Vector<Cent> v = new Vector<Cent>();
            for (Object o : root.elements()) {
                Element e = (Element) o;
                if (e.getName().equals("key")) {
                    dic.setName(e.getTextTrim());
                } else if (e.getName().equals("audio")) {
                    dic.setAudio(e.getTextTrim());
                } else if (e.getName().equals("pron")) {
                    dic.setPron(e.getTextTrim());
                } else if (e.getName().equals("def")) {
                    dic.setDef(e.getTextTrim());
                } else if (e.getName().equals("sent")) {
                    Cent cent = new Cent();
                    for (Object subo : e.elements()) {
                        Element sube = (Element) subo;
                        if (sube.getName().equals("orig")) {
                            cent.setOrig(sube.getTextTrim());
                        } else if (sube.getName().equals("trans")) {
                            cent.setTrans(sube.getTextTrim());
                        }
                    }
                    v.add(cent);
                }
            }
            if (dic.getName() == null || "".equals(dic.getName())) {
                session.getTransaction().commit();
                return;
            }
            session.save(dic);
            dic.setCent(new HashSet<Cent>());
            for (Cent c : v) {
                c.setDict(dic);
                dic.getCent().add(c);
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
            session.getTransaction().rollback();
        }
    }
}
