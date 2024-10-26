package org.dvdcatalog.dvdc.ui.wizard.imdb;

import java.net.*;
import java.io.*;

/**
 *  This Class loades a page into a String. Ex; if google.com is called, then
 *  this class will load the html code for google.com and store it in a String.
 *  However, this class will only be used to load imdb links!
 *
 *@author     lars
 *@created    November 21, 2004
 */
public class PageLoader {

    private String inputLine;

    /**
	 *  Constructor for the PageLoader object
	 *
	 *@param  pageAddress    The address it will look up and copy to a String
	 *@exception  Exception  Can be any Exception.
	 */
    public PageLoader(String pageAddress) throws Exception {
        URL url = new URL(pageAddress);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        inputLine = "";
        while (in.ready()) {
            inputLine = inputLine + in.readLine();
        }
        in.close();
    }

    /**
	 *  This method is called when a user want the imdb page (in html code) in
	 *  String format.
	 *
	 *@return    A String that is the page it loaded.
	 */
    public String getString() {
        return inputLine;
    }
}
