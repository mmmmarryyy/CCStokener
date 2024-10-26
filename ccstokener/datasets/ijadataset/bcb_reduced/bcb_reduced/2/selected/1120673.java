package org.fit.cssbox.demo;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import org.fit.cssbox.css.CSSNorm;
import org.fit.cssbox.css.DOMAnalyzer;
import org.fit.cssbox.css.NormalOutput;
import org.fit.cssbox.css.Output;
import org.w3c.dom.Document;

/**
 * This example computes the effective style of each element and encodes it into the
 * <code>style</code> attribute of this element. The modified HTML document is then saved
 * to the output file.
 * 
 * @author burgetr
 */
public class ComputeStyles {

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: ComputeStyles <url> <output_file>");
            System.exit(0);
        }
        try {
            URL url = new URL(args[0]);
            URLConnection con = url.openConnection();
            InputStream is = con.getInputStream();
            DOMSource parser = new DOMSource(is);
            Document doc = parser.parse();
            DOMAnalyzer da = new DOMAnalyzer(doc, url);
            da.attributesToStyles();
            da.addStyleSheet(null, CSSNorm.stdStyleSheet());
            da.addStyleSheet(null, CSSNorm.userStyleSheet());
            da.getStyleSheets();
            System.err.println("Computing style...");
            da.stylesToDomInherited();
            PrintStream os = new PrintStream(new FileOutputStream(args[1]));
            Output out = new NormalOutput(doc);
            out.dumpTo(os);
            os.close();
            is.close();
            System.err.println("Done.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
