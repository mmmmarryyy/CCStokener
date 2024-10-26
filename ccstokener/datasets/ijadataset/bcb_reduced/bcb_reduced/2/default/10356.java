import java.util.*;
import java.io.*;
import java.net.*;

public class FetchMARC implements Runnable {

    BufferedReader in;

    Vector id = new Vector();

    public void run() {
    }

    public void fetchFile(String ID) {
        String url = "http://www.nal.usda.gov/cgi-bin/agricola-ind?bib=" + ID + "&conf=010000++++++++++++++&screen=MA";
        System.out.println(url);
        try {
            PrintWriter pw = new PrintWriter(new FileWriter("MARC" + ID + ".txt"));
            if (!id.contains("MARC" + ID + ".txt")) {
                id.add("MARC" + ID + ".txt");
            }
            in = new BufferedReader(new InputStreamReader((new URL(url)).openStream()));
            in.readLine();
            String inputLine, stx = "";
            StringBuffer sb = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith("<TR><TD><B>")) {
                    String sts = (inputLine.substring(inputLine.indexOf("B>") + 2, inputLine.indexOf("</")));
                    int i = 0;
                    try {
                        i = Integer.parseInt(sts);
                    } catch (NumberFormatException nfe) {
                    }
                    if (i > 0) {
                        stx = stx + "\n" + sts + " - ";
                    } else {
                        stx += sts;
                    }
                }
                if (!(inputLine.startsWith("<") || inputLine.startsWith(" <") || inputLine.startsWith(">"))) {
                    String tx = inputLine.trim();
                    stx += tx;
                }
            }
            pw.println(stx);
            pw.close();
        } catch (Exception e) {
            System.out.println("Couldn't open stream");
            System.out.println(e);
        }
    }

    public void writeMarc() {
        try {
            PrintWriter pwx = new PrintWriter(new FileWriter("MARCID.txt"));
            for (int i = 0; i < id.size(); i++) {
                System.out.println(id.elementAt(i));
                pwx.println(id.elementAt(i));
            }
            pwx.close();
        } catch (Exception e) {
            System.out.println("Couldn't open stream");
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        FetchMARC fm = new FetchMARC();
        try {
            FileReader fr = new FileReader("Agri.txt");
            BufferedReader buf = new BufferedReader(fr);
            String pmid = "";
            while ((pmid = buf.readLine()) != null) {
                pmid = pmid.trim();
                fm.fetchFile(pmid);
            }
            fm.writeMarc();
        } catch (Exception e) {
            System.out.println("Couldn't open stream");
            System.out.println(e);
        }
    }
}
