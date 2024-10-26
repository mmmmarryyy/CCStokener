package org.rt.util;

import java.util.ArrayList;
import java.io.*;
import org.rt.credential.*;
import org.rt.parser.*;
import java.net.*;

public class OnlineImporter {

    private static final String NL = System.getProperty("line.separator");

    public static ArrayList<RoleName> importRoles(String urlString) {
        ArrayList<RoleName> results = new ArrayList<RoleName>();
        try {
            URL url = new URL(urlString);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buff = new StringBuffer();
            String line;
            while ((line = in.readLine()) != null) {
                buff.append(line);
                if (line.equals("</RoleName>")) {
                    RoleName name = ProfileParser.parseRoleName(buff.toString());
                    results.add(name);
                    buff = new StringBuffer();
                } else {
                    buff.append(NL);
                }
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } catch (ParsingException e) {
        }
        return results;
    }

    public static ArrayList<Principal> importPrincipals(String urlString) {
        ArrayList<Principal> results = new ArrayList<Principal>();
        try {
            URL url = new URL(urlString);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buff = new StringBuffer();
            String line;
            while ((line = in.readLine()) != null) {
                buff.append(line);
                if (line.equals("</Principal>")) {
                    Principal principal = ProfileParser.parsePrincipal(buff.toString());
                    results.add(principal);
                    buff = new StringBuffer();
                } else {
                    buff.append(NL);
                }
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } catch (ParsingException e) {
        }
        return results;
    }

    public static ArrayList<Credential> importCredentials(String urlString) {
        ArrayList<Credential> results = new ArrayList<Credential>();
        try {
            URL url = new URL(urlString);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buff = new StringBuffer();
            String line;
            while ((line = in.readLine()) != null) {
                buff.append(line);
                if (line.equals("-----END PGP SIGNATURE-----")) {
                    Credential credential = ProfileParser.parseCredential(buff.toString(), true);
                    results.add(credential);
                    buff = new StringBuffer();
                } else {
                    buff.append(NL);
                }
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } catch (ParsingException e) {
            System.err.println(e);
        }
        return results;
    }
}
