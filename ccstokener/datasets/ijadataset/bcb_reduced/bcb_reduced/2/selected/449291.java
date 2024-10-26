package bump3;

import bump3.*;
import java.io.*;
import java.net.*;

/** this class gets the text from a webpage and stores it in a string
 *  this class also has a method to get the filesize of an item on a website
 */
public class GetUrl {

    /** returns the text from a webpage (HTML or whatever)
	 *  @param theURL url of the website to visit, must begin with the protocol (http, ftp, etc)
	 *  @return the url's plain text
	 */
    public static String getURL(String theURL) {
        URL u;
        BufferedReader dis = null;
        String s, result = "";
        try {
            u = new URL(theURL);
            URLConnection uc = u.openConnection();
            uc.setConnectTimeout(Main.CONNECT_TIMEOUT);
            uc.setReadTimeout(Main.READ_TIMEOUT);
            uc.setRequestProperty("User-Agent", Main.USER_AGENT);
            dis = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            while ((s = dis.readLine()) != null) {
                result = result + s + "\n";
            }
        } catch (MalformedURLException mue) {
            Methods.pv("MalformedURLException Occurred for \'" + theURL + "\'");
            return "";
        } catch (SocketTimeoutException ste) {
            Methods.pv("SocketTimeoutException for \'" + theURL + "\'");
        } catch (IOException ioe) {
            Methods.pv("IOException for \'" + theURL + "\'");
            return "";
        } finally {
            try {
                if (dis != null) dis.close();
            } catch (IOException ioe) {
            }
        }
        return result;
    }

    /** returns how large a file on a website is
	 *  useful in checking if a link is valid
	 *  @param theURL url of the file to check
	 *  @return size of the file, -1 if file is not found
	*/
    public static int getFilesize(String theURL) {
        URL url;
        URLConnection conn;
        int size = -1;
        try {
            url = new URL(theURL);
            conn = url.openConnection();
            conn.setRequestProperty("User-Agent", Main.USER_AGENT);
            conn.setConnectTimeout(Main.CONNECT_TIMEOUT * 2);
            conn.setReadTimeout(Main.READ_TIMEOUT * 2);
            size = conn.getContentLength();
            conn.getInputStream().close();
        } catch (FileNotFoundException fnfe) {
            return -2;
        } catch (ConnectException ce) {
            return -3;
        } catch (ProtocolException fpe) {
            return -2;
        } catch (IOException ioe) {
            return -2;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    public static String sendPostData(String url, String postdata) {
        URL u;
        BufferedReader dis = null;
        String s, result = "";
        try {
            u = new URL(url);
            URLConnection uc = u.openConnection();
            uc.setRequestProperty("User-Agent", Main.USER_AGENT);
            uc.setConnectTimeout(Main.CONNECT_TIMEOUT);
            uc.setReadTimeout(Main.READ_TIMEOUT);
            uc.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(uc.getOutputStream());
            wr.write(postdata);
            wr.close();
            dis = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            while ((s = dis.readLine()) != null) {
                result = result + s + "\n";
            }
            dis.close();
        } catch (MalformedURLException mue) {
            System.out.println("MalformedURLException Occurred for \'" + url + "\'");
            return "";
        } catch (SocketTimeoutException ste) {
        } catch (IOException ioe) {
            return "";
        }
        return result;
    }
}
