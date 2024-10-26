package com.io_software.utils.web;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;

/** Tests the <tt>java.net.Authenticator</tt> class. With JDK 1.2
    there's a bug: FTP URLs don't use the Authenticator.
*/
public class Auth {

    /** loads the URL specified as <tt>args[0]</tt>. If the URL
	  requires authentication, an instance of the inner class
	  <tt>Authenticator</tt> is employed to ask a username and a
	  password, displaying the URL that requested it.
      */
    public static void main(String[] args) {
        try {
            Authenticator a = new StdioAuthenticator();
            Authenticator.setDefault(a);
            URL url = new URL(args[0]);
            URLConnection conn = url.openConnection();
            InputStream is = conn.getInputStream();
            if (is != null) {
                System.out.println("URL opened OK.");
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** inner class doing a stdio-based user authentication */
    public static class StdioAuthenticator extends Authenticator {

        protected PasswordAuthentication getPasswordAuthentication() {
            try {
                System.out.println("Requesting authentication for " + getRequestingProtocol() + "://" + getRequestingSite() + ":" + getRequestingPort() + "/" + getRequestingScheme());
                if (getRequestingPrompt() != null) System.out.println(getRequestingPrompt()); else System.out.print("Username: ");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                String username = br.readLine();
                System.out.print("Password: ");
                String password = br.readLine();
                char[] chars = new char[password.length()];
                password.getChars(0, password.length(), chars, 0);
                return new PasswordAuthentication(username, chars);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
