package org.openscience.nmrshiftdb.tests;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import junit.framework.TestCase;

/**
 *  Description of the Class
 *
 * @cdk.module test
 *
 *@author     chhoppe
 *@cdk.created    2004-11-04
 */
public class ServletTests extends TestCase {

    String server = "nmrshiftdb.ice.mpg.de";

    public ServletTests(String text) {
        super(text);
    }

    public void testRss() throws Exception {
        doTest("rss", "NMRShiftDB is an open-source, open-access");
    }

    public void testTest() throws Exception {
        doTest("test", "success");
    }

    public void testExportinchi() throws Exception {
        doTest("exportcmlbyinchi&inchi=1/C6H12/c1-2-4-6-5-3-1/h1-6H2&spectrumtype=13C", "cyclohexane");
    }

    public void testExprtmdl() throws Exception {
        doTest("exportmdl&moleculeid=234&coordsetid=1", "warburganal");
    }

    public void testExprtSpec() throws Exception {
        doTest("exportspec&spectrumid=10088436&format=cml", "nmrshiftdb10088436");
    }

    public void testExprtMol() throws Exception {
        doTest("exportspec&spectrumid=10088436&format=cml", "nmrshiftdb10027408");
    }

    public void testExprtBoth() throws Exception {
        doTest("exportspec&spectrumid=10088436&format=cmlboth", "nmrshiftdb10088436");
    }

    public void testExportLastInputs() throws Exception {
        doTest("exportlastinputs&username=shk3", "<table>");
    }

    public void doTest(String action, String expectedText) throws Exception {
        URL url = new URL("http://" + server + "/NmrshiftdbServlet?nmrshiftdbaction=" + action);
        Reader is = new InputStreamReader(url.openStream());
        BufferedReader in = new BufferedReader(is);
        for (String s; (s = in.readLine()) != null; ) {
            if (s.contains(expectedText)) {
                in.close();
                return;
            }
        }
        in.close();
        throw new Exception("unexpected text");
    }
}
