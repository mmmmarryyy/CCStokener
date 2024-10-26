package net.sourceforge.unit;

import java.util.Enumeration;
import java.util.Hashtable;
import java.net.*;
import java.io.*;
import gnu.regexp.*;

class Sample6 {

    public static void main(String[] args) throws ParseException, MalformedURLException, IOException, REException {
        System.out.println("Starting Sample6.");
        Enumeration units = Unit.enumerate();
        while (units.hasMoreElements()) {
            Unit u = (Unit) units.nextElement();
        }
        URL base = new URL("file:package2.html");
        URLConnection connection = base.openConnection();
        InputStreamReader in1 = new InputStreamReader(connection.getInputStream());
        BufferedReader in = new BufferedReader(in1);
        String inputLine;
        int i = 0;
        String one = "<td>\\s*([^<]*\\s*[^ <]+)\\s*</td>";
        RE regex = new RE(one + one + one + one);
        while ((inputLine = in.readLine()) != null) {
            i++;
            REMatch m = regex.getMatch(inputLine);
            if (m != null) {
                lookup(m.toString(1), m.toString(2));
            } else {
                System.out.println(i + ") NOT A MATCH: " + inputLine);
            }
            if (i > 5) return;
        }
        in.close();
    }

    static boolean debug = true;

    static void lookup(String s1, String s2) throws MalformedURLException, IOException, REException {
        s1 = s1.replace(' ', '+');
        s1 = s1.replace('/', '+');
        s2 = s2.replace(' ', '+');
        URL url = new URL("http://www.google.com/search?q=unit+" + s1 + "+" + s2 + "&hl=en");
        System.out.println("Trying to connect to " + url);
        URLConnection connection = url.openConnection();
        InputStreamReader in1 = new InputStreamReader(connection.getInputStream());
        BufferedReader in = new BufferedReader(in1);
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            System.out.println(inputLine);
        }
        in.close();
        System.out.println("\n\n\n");
    }
}
