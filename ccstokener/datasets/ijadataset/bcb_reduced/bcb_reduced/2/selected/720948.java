package org.lindenb.tool.oneshot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Vector;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.lindenb.util.Compilation;
import org.lindenb.util.Base64;

public class Connotea2Delicious {

    private Properties prefs = new Properties();

    private boolean debug = false;

    private static class EchoInput extends InputStream {

        private InputStream in;

        EchoInput(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            int c = in.read();
            if (c == -1) return -1;
            System.err.write(c);
            return c;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    private static class Bookmark {

        String title = null;

        String link = null;

        HashSet<String> subject = new HashSet<String>();

        boolean isPrivate = false;
    }

    private void run() throws IOException, XMLStreamException, InterruptedException {
        int num = Integer.parseInt(prefs.getProperty("num", "100"));
        URL url = new URL("http://www.connotea.org/data/user/" + prefs.getProperty("connotea-user") + "?num=" + num);
        URLConnection con = url.openConnection();
        String encoding = Base64.encode((prefs.getProperty("connotea-user") + ":" + prefs.getProperty("connotea-password")).getBytes());
        con.setRequestProperty("Authorization", "Basic " + encoding);
        con.connect();
        InputStream in = con.getInputStream();
        if (debug) in = new EchoInput(in);
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = factory.createXMLEventReader(in);
        XMLEvent evt;
        Vector<Bookmark> bookmarks = new Vector<Bookmark>(num);
        {
            Bookmark bookmark = new Bookmark();
            while (!(evt = reader.nextEvent()).isEndDocument()) {
                if (evt.isStartElement()) {
                    StartElement e = evt.asStartElement();
                    String local = e.getName().getLocalPart();
                    if (local.equals("Post")) {
                        bookmark = new Bookmark();
                    } else if (local.equals("subject")) {
                        bookmark.subject.add(reader.getElementText());
                    } else if (local.equals("title")) {
                        bookmark.title = reader.getElementText();
                    } else if (local.equals("link")) {
                        bookmark.link = reader.getElementText();
                    } else if (local.equals("private")) {
                        bookmark.isPrivate = reader.getElementText().equals("1");
                    }
                } else if (evt.isEndElement()) {
                    EndElement e = evt.asEndElement();
                    String local = e.getName().getLocalPart();
                    if (local.equals("Post")) {
                        if (bookmark.link != null) {
                            if (bookmark.title == null) bookmark.title = bookmark.link;
                            if (bookmark.subject.isEmpty()) bookmark.subject.add("post");
                            bookmarks.addElement(bookmark);
                        }
                        bookmark = new Bookmark();
                    }
                }
            }
            in.close();
        }
        Collections.reverse(bookmarks);
        for (Bookmark bookmark : bookmarks) {
            StringBuilder sb = new StringBuilder("https://api.del.icio.us/v1/posts/add?");
            sb.append("url=" + URLEncoder.encode(bookmark.link, "UTF-8"));
            sb.append("&description=" + URLEncoder.encode(bookmark.title, "UTF-8"));
            sb.append("&tags=");
            for (String tag : bookmark.subject) {
                sb.append(URLEncoder.encode(tag, "UTF-8") + "+");
            }
            sb.append("&replace=yes");
            sb.append(bookmark.isPrivate ? "&shared=no" : "");
            System.out.print(bookmark.title + " " + sb);
            URL url2 = new URL(sb.toString());
            URLConnection con2 = url2.openConnection();
            encoding = Base64.encode((prefs.getProperty("delicious-user") + ":" + prefs.getProperty("delicious-password")).getBytes());
            con2.setRequestProperty("Authorization", "Basic " + encoding);
            con2.connect();
            InputStream in2 = con2.getInputStream();
            if (debug) in2 = new EchoInput(in2);
            XMLEventReader reader2 = factory.createXMLEventReader(in2);
            XMLEvent evt2;
            while (!(evt2 = reader2.nextEvent()).isEndDocument()) {
                if (!evt2.isStartElement()) continue;
                if (!evt2.asStartElement().getName().getLocalPart().equals("result")) continue;
                Attribute att = evt2.asStartElement().getAttributeByName(new QName("code"));
                if (att == null) continue;
                System.out.print("\t" + att.getValue());
                break;
            }
            System.out.println();
            in2.close();
            Thread.sleep(2000);
        }
        System.err.print("Done.");
    }

    public static void main(String[] args) {
        try {
            File prefFile = new File(System.getProperty("user.home"), ".connotea.xml");
            if (!prefFile.exists()) {
                System.err.println("Cannot get " + prefFile + "\nThis file should look like this:\n" + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">" + "<properties>" + "       <entry key=\"num\">200</entry>" + "       <entry key=\"connotea-user\">xxxxx</entry>" + "       <entry key=\"connotea-password\">xxxxxxx</entry>" + "       <entry key=\"delicious-user\">xxxxxxxx</entry>" + "       <entry key=\"delicious-password\">xxxxxxx</entry>" + "</properties>");
                return;
            }
            Connotea2Delicious app = new Connotea2Delicious();
            InputStream in = new FileInputStream(prefFile);
            app.prefs.loadFromXML(in);
            in.close();
            int optind = 0;
            while (optind < args.length) {
                if (args[optind].equals("-h")) {
                    System.err.println(Compilation.getLabel());
                    System.err.println("-h this screen");
                    System.err.println("-d debug");
                    System.err.println("-n (int) optional number of bookmarks");
                    return;
                } else if (args[optind].equals("-n")) {
                    app.prefs.setProperty("num", args[++optind]);
                } else if (args[optind].equals("-d")) {
                    app.debug = true;
                } else if (args[optind].equals("--")) {
                    ++optind;
                    break;
                } else if (args[optind].startsWith("-")) {
                    System.err.println("bad argument " + args[optind]);
                    System.exit(-1);
                } else {
                    break;
                }
                ++optind;
            }
            if (optind != args.length) {
                System.err.println("bad number of args");
                return;
            }
            app.run();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
}
