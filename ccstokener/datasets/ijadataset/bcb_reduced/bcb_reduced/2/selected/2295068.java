package com.trackerdogs.websources.results;

import java.lang.*;
import java.io.*;
import java.util.*;
import java.net.*;
import org.w3c.dom.*;
import org.w3c.tidy.*;

/**********************************************************************
 * A html/xml results page returned by an online resource. It contains
 * the text itself, and the Tidy tag structure.
 *
 * @author Koen Witters
 *
 * @version 0.1
 */
public class ResultsPage {

    /**********************************************************************
     * Constructs a Results Page of a specific url (from a query). It tries
     * to find the results within the page.
     *
     * @param url the url to construct from.
     */
    public ResultsPage(URL url) {
        try {
            Tidy tidy = new Tidy();
            tidy.setShowWarnings(false);
            tidy.setQuiet(true);
            NodeList body = tidy.parseDOM(url.openStream(), null).getElementsByTagName("body");
            NodeList nl = body.item(0).getChildNodes();
            PatternSequence ps = new PatternSequence();
            ps.findSequence(nl);
            Vector train = ps.getTrain();
            NodeList tNl = ps.getTrainNodeList();
            int begin = ((Integer) train.elementAt(0)).intValue();
            int end = ((Integer) train.elementAt(1)).intValue();
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }
}
