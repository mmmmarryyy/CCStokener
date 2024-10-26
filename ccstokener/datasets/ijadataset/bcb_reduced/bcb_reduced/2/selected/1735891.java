package gov.lanl.TM_tools;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**Displays a text file in a window
  *@author Jim George
  *@version $Revision: 4 $ $Date: 2000-08-12 00:37:42 -0400 (Sat, 12 Aug 2000) $
  */
public class TextWin extends Frame {

    /**The textarea to be created for the window*/
    TextArea thetext;

    /**Constructor for text area window.
    * @param theurl this is a URL where the text is located
    */
    void doSetup() {
        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);
        thetext = new TextArea("", 80, 80, TextArea.SCROLLBARS_VERTICAL_ONLY);
        thetext.setEditable(false);
        thetext.setBackground(Color.lightGray);
        thetext.setForeground(Color.blue);
        thetext.setFont(new Font("TimesRoman", Font.PLAIN, 14));
        TMUtil.constrain(this, thetext, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1.0, 1.0, 2, 2, 2, 2);
        pack();
        validate();
    }

    void addDataFromURL(URL theurl) {
        String line;
        InputStream in = null;
        try {
            in = theurl.openStream();
            BufferedReader data = new BufferedReader(new InputStreamReader(in));
            while ((line = data.readLine()) != null) {
                thetext.append(line + "\n");
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            thetext.append(theurl.toString());
        }
        try {
            in.close();
        } catch (Exception e) {
        }
    }

    public TextWin(URL theurl) {
        super("Info");
        doSetup();
        addDataFromURL(theurl);
        show();
    }

    public TextWin(Vector data, URL theurl) {
        super("Info");
        doSetup();
        if (data != null) {
            String astring;
            Enumeration thestrings = data.elements();
            while (thestrings.hasMoreElements()) {
                astring = (String) thestrings.nextElement();
                thetext.append(astring + "\n");
            }
        }
        addDataFromURL(theurl);
        show();
    }

    /**Set up preferred size for AWT 1.1*/
    public Dimension getPreferredSize() {
        return new Dimension(540, 400);
    }

    /**process window destroyed event*/
    public boolean handleEvent(Event evt) {
        if (evt.id == Event.WINDOW_DESTROY) {
            setVisible(false);
            dispose();
            return (true);
        }
        return false;
    }
}
