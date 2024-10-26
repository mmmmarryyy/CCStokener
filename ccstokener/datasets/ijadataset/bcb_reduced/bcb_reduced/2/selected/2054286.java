package com.submersion.jspshop.adminbrowse;

import java.io.*;
import java.net.*;
import java.util.*;

/** Does the underlying networking flow for the AdminBrowse Applet
 * 
 * @author Jeff Davey (jeffdavey@submersion.com)
 * @changedBy: $Author: jeffdavey $
 * @see com.submersion.jspshop.adminbrowse.AdminBrowse
 * @changed: $Date: 2001/10/03 05:15:18 $
 * @created: September 04, 2001
 * @version $Revision: 1.1.1.1 $
 */
public class NetConnect {

    /** HashMap containing the data returned from the URL.
     */
    HashMap data = new HashMap();

    /** Calls the initialise() function that Connects to the URL passed, and constructs the HashMap for the data.
     * 
     * @param urlString The URL in String format of the Page that gives the 
     *     Data for the AdminBrowse applet.
     */
    public NetConnect(String urlString) {
        initialise(urlString);
    }

    /** Returns the HashMap
     * 
     * @return A HashMap of the data in the URL passed.
     */
    public HashMap getHashData() {
        return data;
    }

    /** This code is called by the constructor and actually does the data connection to the URL. It then calls BuidlData to construct a HashMap of the data.
     * 
     * @param urlString The URL where to connect to, to get the data required.
     */
    private void initialise(String urlString) {
        try {
            URL url = new URL(urlString);
            BufferedInputStream buff = new BufferedInputStream(url.openStream());
            buildData(buff);
        } catch (IOException e) {
            System.err.println("AdminBrowse: Error opening input stream to url: " + urlString);
            e.printStackTrace();
        }
    }

    /** Takes the raw data from initialise, and builds it into a HashMap
     * 
     * @param buff BufferedInputStream containing URL data
     * @exception IOException See the BufferedInputStream IOException
     */
    private void buildData(BufferedInputStream buff) throws IOException {
        int chr;
        StringBuffer nameBuf = new StringBuffer();
        StringBuffer idBuf = new StringBuffer();
        StringBuffer childBuf = new StringBuffer();
        boolean isID = true;
        boolean isName = false;
        while ((chr = buff.read()) != -1) {
            if (!(chr == 10 || chr == 13 || chr == 20)) {
                if (chr != 0) {
                    if (isID) {
                        idBuf.append((char) chr);
                    } else if (isName) {
                        nameBuf.append((char) chr);
                    } else {
                        childBuf.append((char) chr);
                    }
                } else {
                    if (isID) {
                        isID = false;
                        isName = true;
                    } else if (isName) {
                        isName = false;
                    } else {
                        String[] nameData = new String[2];
                        nameData[0] = nameBuf.toString();
                        nameData[1] = childBuf.toString();
                        data.put(idBuf.toString(), nameData);
                        isID = true;
                        idBuf = new StringBuffer();
                        nameBuf = new StringBuffer();
                        childBuf = new StringBuffer();
                    }
                }
            }
        }
    }
}
