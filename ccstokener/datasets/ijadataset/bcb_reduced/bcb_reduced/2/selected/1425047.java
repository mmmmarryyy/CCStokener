package com.submersion.jspshop.adminlist;

import java.io.*;
import java.net.*;
import java.util.*;

/** Does the underlying networking flow for the ListBrowse Applet
 * . Basically connects to a URL, reads the data there, and then constructs a HashMap of that data.
 * 
 * @author Jeff Davey (jeffdavey@submersion.com)
 * @changedBy: $Author: jeffdavey $
 * @see com.submersion.jspshop.adminlist.AdminList
 * @changed: $Date: 2001/10/03 05:15:18 $
 * @created: September 08, 2001
 * @version $Revision: 1.1.1.1 $
 */
public class NetConnect {

    /** A HashMap containing the data created in the constructor.
     */
    HashMap data = new HashMap();

    /** Calls the initialise function which connects to the URL, gets the data, and builds the HashMap
     * 
     * @param urlString The URL to connect to, to get the data required.
     */
    public NetConnect(String urlString) {
        initialise(urlString);
    }

    /** Accessor method to get HashMap of data.
     * 
     * @return Contains the data created by the Constructor method.
     */
    public HashMap getHashData() {
        return data;
    }

    /** Connects to the URL specified and returns a BufferedInputStream that buildData uses to construct the HashMap
     * 
     * @param urlString The URL from which to get Data from
     */
    private void initialise(String urlString) {
        try {
            URL url = new URL(urlString);
            BufferedInputStream buff = new BufferedInputStream(url.openStream());
            buildData(buff);
        } catch (IOException e) {
            System.err.println("AdminList: Error opening input stream to url: " + urlString);
            e.printStackTrace();
        }
    }

    /** Reads the BufferedInputStream passed and creates a HashMap based on the data therein.
     * 
     * @param buff BufferedInputStream passed from initialise method.
     * @exception IOException See the BufferedInputStream IOException
     */
    private void buildData(BufferedInputStream buff) throws IOException {
        int chr;
        StringBuffer nameBuf = new StringBuffer();
        StringBuffer idBuf = new StringBuffer();
        boolean isID = true;
        while ((chr = buff.read()) != -1) {
            if (!(chr == 10 || chr == 13 || chr == 20)) {
                if (chr != 0) {
                    if (isID) {
                        idBuf.append((char) chr);
                    } else {
                        nameBuf.append((char) chr);
                    }
                } else {
                    if (isID) {
                        isID = false;
                    } else {
                        data.put(idBuf.toString(), nameBuf.toString());
                        isID = true;
                        idBuf = new StringBuffer();
                        nameBuf = new StringBuffer();
                    }
                }
            }
        }
    }
}
