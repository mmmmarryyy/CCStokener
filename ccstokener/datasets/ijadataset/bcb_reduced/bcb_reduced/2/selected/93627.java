package org.cydonia.engines;

import iharder.net.base64.Base64;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cydonia.engines.interfaces.Engine;
import org.cydonia.ip.ExternalIp;

/**
 *
 * @author franklin
 */
public class DynDNS implements Engine {

    public String doUpdate(String username, String password, String domainName, String ip) {
        String retorno = "";
        try {
            String endereco = "http://members.dyndns.org/nic/update?hostname=" + domainName + "&myip=" + ip + "&wildcard=NOCHG&mx=NOCHG&backmx=NOCHG";
            String userPass = username + ":" + password;
            String codificado = Base64.encodeBytes(userPass.getBytes());
            URL url = new URL(endereco);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("Authorization", "Basic " + codificado);
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String linha;
            String rtn = "";
            while ((linha = in.readLine()) != null) {
                rtn += linha;
            }
            in.close();
            retorno = filtraRetorno(rtn);
        } catch (IOException ex) {
            Logger.getLogger(ExternalIp.class.getName()).log(Level.SEVERE, null, ex);
            return "ERROR.";
        }
        return retorno;
    }

    private String filtraRetorno(String str) {
        String rtn = "";
        if (str.startsWith("good") || str.startsWith("nochg")) {
            rtn = "IP updated with success!";
        } else if (str.startsWith("nohost")) {
            rtn = "Host not found.";
        } else {
            rtn = "The server return this error: " + str;
        }
        return rtn;
    }
}
