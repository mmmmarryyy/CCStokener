package rath.jmsn.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import rath.jmsn.MainFrame;

/**
 *
 * @author Jang-Ho Hwang, rath@xrath.com
 * @version 1.0, $Id: Me2DayFeedFetcher.java,v 1.2 2007/06/04 09:27:20 nevard Exp $ since 2007/03/15
 */
public class Me2DayFeedFetcher extends Thread {

    private DocumentBuilder builder = null;

    public boolean isLive = true;

    public Me2DayFeedFetcher() {
        setDaemon(true);
        setName("Me2Day Feed Fetcher");
        setPriority(3);
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doFetch(String userid) {
        if (builder == null) return;
        InputStream in = null;
        try {
            URL url = new URL("http://me2day.net/" + userid + "/rss");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "Java/" + System.getProperty("java.version") + " (JMSN with me2day.net)");
            in = con.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            while (true) {
                int readlen = in.read(buf);
                if (readlen < 1) break;
                bos.write(buf, 0, readlen);
            }
            String str = new String(bos.toByteArray(), "UTF-8");
            Document doc = builder.parse(new InputSource(new StringReader(str)));
            processDocument(doc);
        } catch (Exception e) {
            System.err.println("Error on me2day.net: " + e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
                in = null;
            }
        }
    }

    private String removeTag(String str) {
        StringBuffer sb = new StringBuffer();
        int offset = 0;
        while (true) {
            int i0 = str.indexOf('<', offset);
            if (i0 == -1) break;
            int i1 = str.indexOf('>', i0);
            if (i1 == -1) break;
            sb.append(str.substring(offset, i0));
            offset = i1 + 1;
        }
        sb.append(str.substring(offset));
        return sb.toString();
    }

    protected void processDocument(Document doc) {
        NodeList itemList = doc.getElementsByTagName("item");
        if (itemList.getLength() == 0) return;
        String title = "";
        String link = "";
        String desc = "";
        String author = "";
        Element elemItem = (Element) itemList.item(0);
        NodeList dataList = elemItem.getChildNodes();
        for (int i = 0; i < dataList.getLength(); i++) {
            Node n = dataList.item(i);
            String nv = n.getNodeName();
            if (nv == null) continue;
            if (nv.equals("title")) title = n.getFirstChild().getNodeValue();
            if (nv.equals("link")) link = n.getFirstChild().getNodeValue();
            if (nv.equals("description")) desc = n.getFirstChild().getNodeValue();
            if (nv.equals("author")) author = n.getFirstChild().getNodeValue();
        }
        desc = removeTag(desc);
        String newmsg = "[me2day] " + desc + " by " + author;
        try {
            MainFrame main = MainFrame.INSTANCE;
            main.setPersonalMessage(newmsg);
            main.getMessenger().setPersonalMessage(newmsg);
        } catch (IOException e) {
            System.err.println("Error on MSNMessenger.setPersonalMessage: " + e);
        }
    }

    public void run() {
        while (isLive) {
            if (!MainFrame.INSTANCE.getMessenger().isLoggedIn()) break;
            GlobalProp prop = MainFrame.getGlobalProp();
            String userid = prop.get("me2day.id");
            boolean use = prop.getBoolean("me2day.use", false);
            if (use && userid != null) doFetch(userid);
            try {
                Thread.sleep(1000L * 60L * 5L);
            } catch (InterruptedException e) {
                continue;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Me2DayFeedFetcher me = new Me2DayFeedFetcher();
        me.doFetch("rath");
    }
}
