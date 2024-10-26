package paolomind.test.notunit;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import paolomind.commons.xml.XMLListener;
import paolomind.commons.xml.XMLObjectContainer;

public class StartMe implements XMLListener {

    LinkedList l = new LinkedList();

    PrintStream epr = System.err;

    PrintStream opr = System.out;

    public static void main(String[] args) throws IOException {
        InputStream in = null;
        String filepath;
        if (args.length > 0) {
            filepath = args[0];
        } else {
            filepath = "bean.xml";
        }
        try {
            URL url = new URL(filepath);
            in = url.openConnection().getInputStream();
        } catch (MalformedURLException e) {
            in = StartMe.class.getClassLoader().getResourceAsStream(filepath);
        }
        if (in != null) {
            StartMe s = new StartMe();
            final XMLObjectContainer xml = new XMLObjectContainer(in, s, StartMe.class.getClassLoader());
            in.close();
            xml.equals(null);
            Iterator i;
            System.out.println("~List");
            i = s.l.iterator();
            while (i.hasNext()) {
                System.out.println(i.next());
            }
        }
    }

    public void endreading() {
        synchronized (epr) {
            opr.println("endfile");
        }
    }

    public void objectRegister(String name, Object o) {
        synchronized (epr) {
            opr.print("registered: ");
            opr.print(name);
            opr.print(" = ");
            opr.println(o);
        }
    }

    public void openedStream(URL url) {
        synchronized (epr) {
            opr.print("url open: ");
            opr.println(url);
        }
    }

    public void read(Object o) {
        synchronized (epr) {
            opr.print("object read: ");
            opr.println(o);
        }
        l.add(o);
    }

    public void startreading() {
        synchronized (epr) {
            opr.println("beginfile");
        }
    }

    public void exceptionThrown(Exception e) {
        synchronized (epr) {
            e.printStackTrace(epr);
        }
    }
}
